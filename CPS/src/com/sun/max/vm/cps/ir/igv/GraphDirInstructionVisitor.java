/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product that is
 * described in this document. In particular, and without limitation, these intellectual property rights may include one
 * or more of the U.S. patents listed at http://www.sun.com/patents and one or more additional patents or pending patent
 * applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks are used under license and
 * are trademarks or registered trademarks of SPARC International, Inc. in the U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open Company, Ltd.
 */
package com.sun.max.vm.cps.ir.igv;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.transform.*;

/**
 * Creates graph nodes and edges from DirInstruction objects.
 * @author Thomas Wuerthinger
 */
class GraphDirInstructionVisitor extends DirAdapter {

    // Used to create unique identifiers for DirValue objects.
    private static final int DIRVALUE_OFFSET = 100000000;

    private final GraphWriter.Graph graph;
    private final Mapping<DirValue, Integer> mapping = new IdentityHashMapping<DirValue, Integer>();
    private GraphWriter.Block valueBlock;

    GraphDirInstructionVisitor(GraphWriter.Graph graph) {
        this.graph = graph;
    }

    private int visitValue(DirValue value) {
        if (!mapping.containsKey(value)) {
            final int id = DIRVALUE_OFFSET + mapping.keys().size();
            mapping.put(value, id);
            final GraphWriter.Node node = graph.createNode(id);
            node.getProperties().setProperty("name", value.toString());
            node.getProperties().setProperty("short_name", value.toString());
            node.getProperties().setProperty("class", value.getClass().toString());
            if (valueBlock == null) {
                valueBlock = graph.createBlock("Values");
                for (GraphWriter.Block block : graph.getBlocks()) {
                    if (block.getPredecessors().size() == 0) {
                        valueBlock.addSuccessor(block);
                    }
                }

            }
            valueBlock.addNode(node);
        }
        return mapping.get(value);
    }

    @Override
    public void visitInstruction(DirInstruction dirInstruction) {
        super.visitInstruction(dirInstruction);
        final GraphWriter.Node node = graph.createNode(dirInstruction.serial());
        node.getProperties().setProperty("name", dirInstruction.toString());
        node.getProperties().setProperty("class", dirInstruction.getClass().toString());
    }

    @Override
    public void visitAssign(DirAssign dirMove) {
        super.visitAssign(dirMove);
        final GraphWriter.Node node = graph.createNode(dirMove.serial());

        final int destinationId = visitValue(dirMove.destination());
        graph.createEdge(1, 0, node.getId(), destinationId);

        final int sourceId = visitValue(dirMove.source());
        graph.createEdge(0, 1, sourceId, node.getId());
    }

    @Override
    public void visitSwitch(DirSwitch dirSwitch) {
        super.visitSwitch(dirSwitch);

        int z = 1;
        for (DirValue value : dirSwitch.matches()) {
            final int valueId = visitValue(value);
            graph.createEdge(0, z, valueId, dirSwitch.serial());
            z++;
        }

        z = 0;
        if (dirSwitch.defaultTargetBlock() != null) {
            graph.createEdge(z, 0, dirSwitch.serial(), dirSwitch.defaultTargetBlock().instructions().get(0).serial());
            z++;
        }

        for (int i = 0; i < dirSwitch.targetBlocks().length; i++) {
            graph.createEdge(z, 0, dirSwitch.serial(), dirSwitch.targetBlocks()[i].instructions().get(0).serial());
            z++;
        }
    }

    @Override
    public void visitGoto(DirGoto dirGoto) {
        super.visitGoto(dirGoto);
        graph.createEdge(0, 0, dirGoto.serial(), dirGoto.targetBlock().instructions().get(0).serial());
    }

    @Override
    public void visitReturn(DirReturn dirReturn) {
        super.visitReturn(dirReturn);
        final DirValue value = dirReturn.returnValue();
        final int valueId = visitValue(value);
        graph.createEdge(0, 1, valueId, dirReturn.serial());
    }

    @Override
    public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
        super.visitBuiltinCall(dirBuiltinCall);
        final GraphWriter.Node node = graph.getNode(dirBuiltinCall.serial());
        node.getProperties().setProperty("name", dirBuiltinCall.builtin().name);
    }

    @Override
    public void visitMethodCall(DirMethodCall dirMethodCall) {
        super.visitMethodCall(dirMethodCall);
        final GraphWriter.Node node = graph.getNode(dirMethodCall.serial());
        node.getProperties().setProperty("name", dirMethodCall.method().toString());
    }

    @Override
    public void visitCall(DirCall dirCall) {
        super.visitCall(dirCall);
        final GraphWriter.Node node = graph.getNode(dirCall.serial());
        node.getProperties().setProperty("argumentCount", Integer.toString(dirCall.arguments().length));

        int z = 1;
        for (DirValue dirValue : dirCall.arguments()) {
            final int valueId = visitValue(dirValue);
            graph.createEdge(0, z, valueId, node.getId());
            z++;
        }

        if (dirCall.catchBlock() != null) {
            node.getProperties().setProperty("catchBlock", Integer.toString(dirCall.catchBlock().id()));
        }

        if (dirCall.result() != null) {
            final int resultId = visitValue(dirCall.result());
            graph.createEdge(1, 0, node.getId(), resultId);
            node.getProperties().setProperty("result", dirCall.result().toString());
        }
    }

    @Override
    public void visitThrow(DirThrow dirThrow) {
        super.visitThrow(dirThrow);
        final GraphWriter.Node node = graph.getNode(dirThrow.serial());
        if (dirThrow.catchBlock() != null) {
            node.getProperties().setProperty("catchBlock", Integer.toString(dirThrow.catchBlock().id()));
            graph.createEdge(1, 0, dirThrow.serial(), dirThrow.catchBlock().instructions().get(0).serial());
        }
        node.getProperties().setProperty("throwable", dirThrow.throwable().toString());
    }

    /**
     * Sets the node properties for a DirSafepoint object. The bci (byte code index) property is used by the visualization tool to relate this node to a certain bytecode.
     * @param dirSafepoint the safepoint node for which the properties are set
     */
    @Override
    public void visitInfopoint(DirInfopoint dirSafepoint) {
        super.visitInfopoint(dirSafepoint);
        final GraphWriter.Node node = graph.getNode(dirSafepoint.serial());
        node.getProperties().setProperty("name", "safepoint");
        node.getProperties().setProperty("dump_spec", dirSafepoint.toString());
        node.getProperties().setProperty("bci", Integer.toString(dirSafepoint.javaFrameDescriptor().bci));
    }
}
