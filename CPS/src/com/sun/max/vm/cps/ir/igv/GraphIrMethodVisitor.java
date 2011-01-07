/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.igv;

import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.eir.*;

/**
 * Creates a graph for BirMethod, CirMethod, DirMethod, and EirMethod objects.
 * @author Thomas Wuerthinger
 */
class GraphIrMethodVisitor implements IrMethodVisitor {

    private final GraphWriter.Group group;
    private final Object context;
    private final String stateName;

    GraphIrMethodVisitor(GraphWriter.Group group, Object context, String stateName) {
        this.group = group;
        this.context = context;
        this.stateName = stateName;
    }

    /**
     * Creates a control flow graph for BirMethod objects and adds it to the {@link _group} field.
     * @param method the method for which the graph is generated
     */
    public void visit(BirMethod method) {
        final String graphName = method.classMethodActor().format("%H.%n(%p)") + "BIR";
        final GraphWriter.Graph graph = group.createGraph(graphName);

        for (BirBlock block : method.blocks()) {
            final GraphWriter.Node node = graph.createNode(block.serial());
            node.getProperties().setProperty("name", "Block (" + block.serial() + "-" + block.bytecodeBlock().end + ")");
            node.getProperties().setProperty("start", Integer.toString(block.bytecodeBlock().start));
            node.getProperties().setProperty("bci", Integer.toString(block.bytecodeBlock().start));
            node.getProperties().setProperty("end", Integer.toString(block.bytecodeBlock().end));
            node.getProperties().setProperty("hasSafePoint", Boolean.toString(block.hasSafepoint()));
            node.getProperties().setProperty("role", block.role().toString());
            node.getProperties().setProperty("size", Integer.toString(block.bytecodeBlock().size()));
            for (BirBlock succ : block.successors()) {
                graph.createEdge(block.serial(), succ.serial());
            }
        }
    }

    /**
     * Creates a graph for CirMethod objects and adds it to the {@link _group} field.
     * @param method the method for which the graph is generated
     */
    public void visit(CirMethod method) {
        if (context == null) {
            return;
        }
        assert context instanceof CirNode;
        final GraphWriter.Graph graph = this.group.createGraph(stateName);
        final CirNode node = (CirNode) context;
        final CirTraversal traversal = new GraphCirNodeVisitor(graph, node);
        traversal.run();
    }

    /**
     * Creates a graph for DirMethod objects and adds it to the {@link _group) field.
     * @param method the method for which the graph is generated
     */
    public void visit(DirMethod method) {
        // TODO: Support DirMethod output.
    }

    /**
     * Creates a graph for an EirMethod object and adds it to the {@link _group} field.
     * @param method the method for which the graph is generated
     */
    public void visit(EirMethod method) {
        // TODO: Support EirMethod output.
    }
}
