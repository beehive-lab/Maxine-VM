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

import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.ci.*;

public class CanonicalizerPhase extends Phase {
    private static final int MAX_ITERATION_PER_NODE = 10;

    private boolean newNodes;
    private final CiTarget target;

    private NodeWorkList nodeWorkList;

    public CanonicalizerPhase(GraalContext context, CiTarget target) {
        this(context, target, false);
    }

    public CanonicalizerPhase(GraalContext context, CiTarget target, boolean newNodes) {
        super(context);
        this.newNodes = newNodes;
        this.target = target;
    }

    @Override
    protected void run(Graph graph) {
        nodeWorkList = graph.createNodeWorkList(!newNodes, MAX_ITERATION_PER_NODE);
        if (newNodes) {
            nodeWorkList.addAll(graph.getNewNodes());
        }
        Tool tool = new Tool();
        int canonicalized = 0;
        for (Node node : nodeWorkList) {
            if (node instanceof Canonicalizable) {
                if (GraalOptions.TraceCanonicalizer) {
                    TTY.println("Canonicalizer: work on " + node);
                }
                graph.mark();
                tool.setNode(node);
                Node canonical = ((Canonicalizable) node).canonical(tool);
                if (canonical != node) {
//     cases:                                           original node:
//                                         |Floating|Fixed-unconnected|Fixed-connected|
//                                         --------------------------------------------
//                                     null|   1    |        X        |       2       |
//                                         --------------------------------------------
//                                 Floating|   1    |        X        |       2       |
//       canonical node:                   --------------------------------------------
//                        Fixed-unconnected|   X    |        X        |       2       |
//                                         --------------------------------------------
//                          Fixed-connected|   1    |        X        |       3       |
//                                         --------------------------------------------
//       X: must not happen (checked with assertions)
                    if (node instanceof FloatingNode) {
                        // case 1
                        assert canonical == null || canonical instanceof FloatingNode || (canonical instanceof FixedNode && canonical.predecessor() != null);
                        node.replaceAndDelete(canonical);
                    } else {
                        assert node instanceof FixedNode && node.predecessor() != null : node + " -> " + canonical + " : node should be Fixed & connected";
                        if (canonical instanceof FixedNode && canonical.predecessor() == null) {
                            // case 2
                            node.replaceAndDelete(canonical);
                        } else {
                            // case 3
                            FixedNode nextNode = null;
                            if (node instanceof FixedWithNextNode) {
                                nextNode = ((FixedWithNextNode) node).next();
                            } else if (node instanceof ControlSplitNode) {
                                for (FixedNode sux : ((ControlSplitNode) node).blockSuccessors()) {
                                    if (nextNode == null) {
                                        nextNode = sux;
                                    } else {
                                        assert sux == null;
                                    }
                                }
                            }
                            node.clearSuccessors();
                            node.replaceAtPredecessors(nextNode);
                            node.replaceAndDelete(canonical);
                        }
                    }

                    for (Node newNode : graph.getNewNodes()) {
                        nodeWorkList.add(newNode);
                    }
                    if (canonical != null) {
                        nodeWorkList.replaced(canonical, node, false, EdgeType.USAGES);
                    }
                    canonicalized++;
                }
            }
        }
        if (canonicalized > 0 && GraalOptions.Meter) {
            context.metrics.NodesCanonicalized += canonicalized;
        }
    }

    private final class Tool implements CanonicalizerTool {

        private Node node;

        @Override
        public void deleteBranch(FixedNode branch) {
            node.replaceFirstSuccessor(branch, null);
            killCFG(branch);
        }

        public void killCFG(FixedNode node) {
            for (Node successor : node.successors()) {
                if (successor != null) {
                    node.replaceFirstSuccessor(successor, null);
                    assert !node.isDeleted();
                    killCFG((FixedNode) successor);
                }
            }
            if (node instanceof LoopEndNode) {
                LoopEndNode loopEnd = (LoopEndNode) node;
                LoopBeginNode loopBegin = loopEnd.loopBegin();
                if (loopBegin != null) {
                    assert loopBegin.isAlive();
                    assert loopBegin.endCount() == 1;
                    EndNode predEnd = loopBegin.endAt(0);

                    for (PhiNode phi : loopBegin.phis()) {
                        ValueNode value = phi.valueAt(0);
                        phi.replaceAndDelete(value);
                        nodeWorkList.replaced(value, phi, false, EdgeType.USAGES);
                    }
                    FixedNode next = loopBegin.next();
                    loopEnd.setLoopBegin(null);
                    loopBegin.delete();

                    predEnd.replaceAndDelete(next);
                }
            } else if (node instanceof EndNode) {
                EndNode end = (EndNode) node;
                MergeNode merge = end.merge();
                if (merge instanceof LoopBeginNode) {
                    for (PhiNode phi : merge.phis()) {
                        ValueNode value = phi.valueAt(0);
                        phi.replaceAndDelete(value);
                        nodeWorkList.replaced(value, phi, false, EdgeType.USAGES);
                    }
                    if (((LoopBeginNode) merge).loopEnd() != null) {
                        ((LoopBeginNode) merge).loopEnd().setLoopBegin(null);
                    }
                    killCFG(merge);
                } else {
                    merge.removeEnd(end);
                    if (merge.phiPredecessorCount() == 1) {
                        for (PhiNode phi : merge.phis()) {
                            ValueNode value = phi.valueAt(0);
                            phi.replaceAndDelete(value);
                            nodeWorkList.replaced(value, phi, false, EdgeType.USAGES);
                        }
                        Node replacedSux = merge.phiPredecessorAt(0);
                        Node pred = replacedSux.predecessor();
                        assert replacedSux instanceof EndNode;
                        FixedNode next = merge.next();
                        merge.setNext(null);
                        pred.replaceFirstSuccessor(replacedSux, next);
                        merge.delete();
                        replacedSux.delete();
                    }
                }
            }
            killNonCFG(node, null);
        }

        public void killNonCFG(Node node, Node input) {
            if (node instanceof PhiNode) {
                node.replaceFirstInput(input, null);
            } else {
                for (Node usage : node.usages().snapshot()) {
                    if (usage instanceof FloatingNode && !usage.isDeleted()) {
                        killNonCFG(usage, node);
                    }
                }
                // null out remaining usages
                node.replaceAtUsages(null);
                node.delete();
            }
        }

        public void setNode(Node node) {
            this.node = node;
        }

        @Override
        public CiTarget target() {
            return target;
        }
    }
}
