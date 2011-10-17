/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.graal.compiler.phases;

import java.util.*;

import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.schedule.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.PhiNode.*;
import com.oracle.max.graal.nodes.extended.*;
import com.sun.cri.ci.*;

public class FloatingReadPhase extends Phase {

    public FloatingReadPhase(GraalContext context) {
        super(context);
    }

    private static class MemoryMap {
        private Block block;
        private IdentityHashMap<Object, Node> map;
        private IdentityHashMap<Object, Node> loopEntryMap;
        private int mergeOperationCount;

        public MemoryMap(Block block) {
            this.block = block;
            map = new IdentityHashMap<Object, Node>();
        }

        public MemoryMap(Block block, MemoryMap other) {
            this(block);
            map.putAll(other.map);
        }

        public void mergeLoopEntryWith(MemoryMap otherMemoryMap) {
            for (Object keyInOther : otherMemoryMap.map.keySet()) {
                assert loopEntryMap.containsKey(keyInOther) || map.get(keyInOther) == otherMemoryMap.map.get(keyInOther) : keyInOther + ", " + map.get(keyInOther) + " vs " + otherMemoryMap.map.get(keyInOther);
            }

            for (Map.Entry<Object, Node> entry : loopEntryMap.entrySet()) {
                PhiNode phiNode = (PhiNode) entry.getValue();
                assert phiNode.valueCount() == 1;

                Node other;
                Object key = entry.getKey();
                if (otherMemoryMap.map.containsKey(key)) {
                    other = otherMemoryMap.map.get(key);
                } else {
                    other = otherMemoryMap.map.get(LocationNode.ANY_LOCATION);
                }

                phiNode.addInput((ValueNode) other);
            }
        }

        public void mergeWith(MemoryMap otherMemoryMap, Block b) {
            if (GraalOptions.TraceMemoryMaps) {
                TTY.println("merging block " + otherMemoryMap.block + " into block " + block);
            }
            IdentityHashMap<Object, Node> otherMap = otherMemoryMap.map;

            for (Map.Entry<Object, Node> entry : map.entrySet()) {
                if (otherMap.containsKey(entry.getKey())) {
                    mergeNodes(entry.getKey(), entry.getValue(), otherMap.get(entry.getKey()), b);
                } else {
                    mergeNodes(entry.getKey(), entry.getValue(), otherMap.get(LocationNode.ANY_LOCATION), b);
                }
            }

            Node anyLocationNode = map.get(LocationNode.ANY_LOCATION);
            for (Map.Entry<Object, Node> entry : otherMap.entrySet()) {
                if (!map.containsKey(entry.getKey())) {
                    Node current = anyLocationNode;
                    if (anyLocationNode instanceof PhiNode) {
                        PhiNode phiNode = (PhiNode) anyLocationNode;
                        if (phiNode.merge() == block.firstNode()) {
                            PhiNode phiCopy = (PhiNode) phiNode.copyWithInputs();
                            phiCopy.removeInput(phiCopy.valueCount() - 1);
                            current = phiCopy;
                            map.put(entry.getKey(), current);
                        }
                    }
                    mergeNodes(entry.getKey(), current, entry.getValue(), b);
                }
            }

            mergeOperationCount++;
        }

        private void mergeNodes(Object location, Node original, Node newValue, Block block) {
            if (original == newValue) {
                // Nothing to merge.
                if (GraalOptions.TraceMemoryMaps) {
                    TTY.println("Nothing to merge both nodes are " + original.id());
                }
                return;
            }
            MergeNode m = (MergeNode) block.firstNode();
            if (m.isPhiAtMerge(original)) {
                PhiNode phi = (PhiNode) original;
                phi.addInput((ValueNode) newValue);
                if (GraalOptions.TraceMemoryMaps) {
                    TTY.println("Add new input to phi " + original.id() + ": " + newValue.id());
                }
                assert phi.valueCount() <= phi.merge().endCount() : phi.merge();
            } else {
                assert m != null;
                PhiNode phi = m.graph().unique(new PhiNode(CiKind.Illegal, m, PhiType.Memory));
                for (int i = 0; i < mergeOperationCount + 1; ++i) {
                    phi.addInput((ValueNode) original);
                }
                phi.addInput((ValueNode) newValue);
                if (GraalOptions.TraceMemoryMaps) {
                    TTY.println("Creating new phi " + phi.id() + " merge=" + phi.merge() + ", mergeOperationCount=" + mergeOperationCount + ", newValue=" + newValue.id() + ", location=" + location);
                }
                assert phi.valueCount() <= phi.merge().endCount() + ((phi.merge() instanceof LoopBeginNode) ? 1 : 0) : phi.merge() + "/" + phi.valueCount() + "/" + phi.merge().endCount() + "/" + mergeOperationCount;
                assert m.usages().contains(phi);
                assert phi.merge().usages().contains(phi);
                for (Node input : phi.inputs()) {
                    assert input.usages().contains(phi);
                }
                map.put(location, phi);
            }
        }

        public void processCheckpoint(AbstractMemoryCheckpointNode checkpoint) {
            map.clear();
            map.put(LocationNode.ANY_LOCATION, checkpoint);
        }

        public void processWrite(WriteNode writeNode) {
            map.put(writeNode.location().locationIdentity(), writeNode);
        }

        public void processRead(ReadNode readNode) {
            FixedNode next = readNode.next();
            readNode.setNext(null);
            readNode.replaceAtPredecessors(next);

            if (GraalOptions.TraceMemoryMaps) {
                TTY.println("Register read to node " + readNode.id());
            }

            // Create dependency on previous node that creates the memory state for this location.
            readNode.addDependency(getLocationForRead(readNode));
        }

        private Node getLocationForRead(ReadNode readNode) {
            Object locationIdentity = readNode.location().locationIdentity();
            Node result = map.get(locationIdentity);
            if (result == null) {
                result = map.get(LocationNode.ANY_LOCATION);
            }
            return result;
        }

        public void createLoopEntryMemoryMap(Set<Object> modifiedLocations, LoopBeginNode begin) {

            loopEntryMap = new IdentityHashMap<Object, Node>();

            for (Object modifiedLocation : modifiedLocations) {
                Node other;
                if (map.containsKey(modifiedLocation)) {
                    other = map.get(modifiedLocation);
                } else {
                    other = map.get(LocationNode.ANY_LOCATION);
                }
                createLoopEntryPhi(modifiedLocation, other, begin, loopEntryMap);
            }

            if (modifiedLocations.contains(LocationNode.ANY_LOCATION)) {
                for (Map.Entry<Object, Node> entry : map.entrySet()) {
                    if (!modifiedLocations.contains(entry.getKey())) {
                        createLoopEntryPhi(entry.getKey(), entry.getValue(), begin, loopEntryMap);
                    }
                }
            }
        }

        private void createLoopEntryPhi(Object modifiedLocation, Node other, LoopBeginNode begin, IdentityHashMap<Object, Node> loopEntryMap) {
            PhiNode phi = other.graph().unique(new PhiNode(CiKind.Illegal, begin, PhiType.Memory));
            phi.addInput((ValueNode) other);
            map.put(modifiedLocation, phi);
            loopEntryMap.put(modifiedLocation, phi);
        }


        public IdentityHashMap<Object, Node> getLoopEntryMap() {
            return loopEntryMap;
        }
    }

    @Override
    protected void run(Graph graph) {

        // Add start node write checkpoint.
        addStartCheckpoint((CompilerGraph) graph);

        // Create node-to-loop relation.
        NodeMap<LoopBeginNode> nodeToLoop = graph.createNodeMap();
        for (LoopBeginNode begin : graph.getNodes(LoopBeginNode.class)) {
            mark(begin, null, nodeToLoop);
        }


        // Get parent-child relationships between loops.
        NodeMap<List<LoopBeginNode>> loopChildren = graph.createNodeMap();
        NodeMap<Set<Object>> modifiedValues = graph.createNodeMap();
        List<LoopBeginNode> rootLoops = new ArrayList<LoopBeginNode>();
        for (LoopBeginNode begin : graph.getNodes(LoopBeginNode.class)) {
            LoopBeginNode parentLoop = nodeToLoop.get(begin.forwardEdge());
            if (parentLoop != null) {
                if (loopChildren.get(parentLoop) == null) {
                    loopChildren.set(parentLoop, new ArrayList<LoopBeginNode>());
                }
                loopChildren.get(parentLoop).add(begin);
            } else {
                rootLoops.add(begin);
            }

            // Initialize modified values to empty hash set.
            modifiedValues.set(begin, new HashSet<Object>());
        }

        // Get modified values in loops.
        for (Node n : graph.getNodes()) {
            LoopBeginNode loop = nodeToLoop.get(n);
            if (loop != null) {
                if (n instanceof WriteNode) {
                    WriteNode writeNode = (WriteNode) n;
                    traceWrite(loop, writeNode.location().locationIdentity(), modifiedValues);
                } else if (n instanceof AbstractMemoryCheckpointNode) {
                    traceMemoryCheckpoint(loop, modifiedValues);

                }
            }
        }

        // Propagate values to parent loops.
        for (LoopBeginNode begin : rootLoops) {
            propagateFromChildren(begin, modifiedValues, loopChildren);
        }

        if (GraalOptions.TraceMemoryMaps) {
            print(graph, nodeToLoop, modifiedValues);
        }

        // Identify blocks.
        final IdentifyBlocksPhase s = new IdentifyBlocksPhase(context, false);
        s.apply(graph);
        List<Block> blocks = s.getBlocks();

        // Process blocks (predecessors first).
        MemoryMap[] memoryMaps = new MemoryMap[blocks.size()];
        for (final Block b : blocks) {
            processBlock(b, memoryMaps, s.getNodeToBlock(), modifiedValues);
        }
    }

    private void addStartCheckpoint(CompilerGraph graph) {
        EntryPointNode entryPoint = graph.start();
        WriteMemoryCheckpointNode checkpoint = graph.add(new WriteMemoryCheckpointNode());
        FixedNode next = entryPoint.next();
        entryPoint.setNext(checkpoint);
        checkpoint.setNext(next);
    }

    private void processBlock(Block b, MemoryMap[] memoryMaps, NodeMap<Block> nodeToBlock, NodeMap<Set<Object>> modifiedValues) {

        // Visit every block at most once.
        if (memoryMaps[b.blockID()] != null) {
            return;
        }

        // Process predecessors before this block.
        for (Block pred : b.getPredecessors()) {
            processBlock(pred, memoryMaps, nodeToBlock, modifiedValues);
        }


        // Create initial memory map for the block.
        MemoryMap map = null;
        if (b.getPredecessors().size() == 0) {
            map = new MemoryMap(b);
        } else {
            map = new MemoryMap(b, memoryMaps[b.getPredecessors().get(0).blockID()]);
            if (b.isLoopHeader()) {
                assert b.getPredecessors().size() == 1;
                LoopBeginNode begin = (LoopBeginNode) b.firstNode();
                map.createLoopEntryMemoryMap(modifiedValues.get(begin), begin);
            } else {
                for (int i = 1; i < b.getPredecessors().size(); ++i) {
                    assert b.firstNode() instanceof MergeNode : b.firstNode();
                    Block block = b.getPredecessors().get(i);
                    map.mergeWith(memoryMaps[block.blockID()], b);
                }
            }
        }
        memoryMaps[b.blockID()] = map;

        // Process instructions of this block.
        for (Node n : b.getInstructions()) {
            if (n instanceof ReadNode) {
                ReadNode readNode = (ReadNode) n;
                map.processRead(readNode);
            } else if (n instanceof WriteNode) {
                WriteNode writeNode = (WriteNode) n;
                map.processWrite(writeNode);
            } else if (n instanceof AbstractMemoryCheckpointNode) {
                AbstractMemoryCheckpointNode checkpoint = (AbstractMemoryCheckpointNode) n;
                map.processCheckpoint(checkpoint);
            }
        }


        if (b.lastNode() instanceof LoopEndNode) {
            LoopEndNode end = (LoopEndNode) b.lastNode();
            LoopBeginNode begin = end.loopBegin();
            Block beginBlock = nodeToBlock.get(begin);
            MemoryMap memoryMap = memoryMaps[beginBlock.blockID()];
            assert memoryMap != null : beginBlock.name();
            assert memoryMap.getLoopEntryMap() != null;
            memoryMap.mergeLoopEntryWith(map);
        }
    }

    private void traceMemoryCheckpoint(LoopBeginNode loop, NodeMap<Set<Object>> modifiedValues) {
        modifiedValues.get(loop).add(LocationNode.ANY_LOCATION);
    }

    private void propagateFromChildren(LoopBeginNode begin, NodeMap<Set<Object>> modifiedValues, NodeMap<List<LoopBeginNode>> loopChildren) {
        if (loopChildren.get(begin) != null) {
            for (LoopBeginNode child : loopChildren.get(begin)) {
                propagateFromChildren(child, modifiedValues, loopChildren);
                modifiedValues.get(begin).addAll(modifiedValues.get(child));
            }
        }
    }

    private void traceWrite(LoopBeginNode loop, Object locationIdentity, NodeMap<Set<Object>> modifiedValues) {
        modifiedValues.get(loop).add(locationIdentity);
    }

    private void print(Graph graph, NodeMap<LoopBeginNode> nodeToLoop, NodeMap<Set<Object>> modifiedValues) {

        TTY.println();
        TTY.println("Loops:");
        for (LoopBeginNode begin : graph.getNodes(LoopBeginNode.class)) {
            TTY.print("Loop " + begin.id() + " modified values: " + modifiedValues.get(begin));
            TTY.println();
        }
//
//        TTY.println();
//        TTY.println("nodeToLoop structure:");
//        for (Node n : graph.getNodes()) {
//            if (nodeToLoop.get(n) != null) {
//                TTY.println("Loop " + nodeToLoop.get(n) + " contains " + n);
//            }
//        }
    }

    private void mark(LoopBeginNode begin, LoopBeginNode outer, NodeMap<LoopBeginNode> nodeToLoop) {

        if (nodeToLoop.get(begin) != null) {
            // Loop already processed.
            return;
        }
        nodeToLoop.set(begin, begin);

        NodeFlood workCFG = begin.graph().createNodeFlood();
        workCFG.add(begin.loopEnd());
        for (Node n : workCFG) {
            if (n == begin) {
                // Stop at loop begin.
                continue;
            }
            markNode(n, begin, outer, nodeToLoop);

            for (Node pred : n.cfgPredecessors()) {
                workCFG.add(pred);
            }
        }
    }

    private void markNode(Node n, LoopBeginNode begin, LoopBeginNode outer, NodeMap<LoopBeginNode> nodeToLoop) {
        LoopBeginNode oldMark = nodeToLoop.get(n);
        if (oldMark == null || oldMark == outer) {

            // We have an inner loop, start marking it.
            if (n instanceof LoopBeginNode) {
                mark((LoopBeginNode) n, begin, nodeToLoop);
            }

            nodeToLoop.set(n, begin);
        }
    }
}
