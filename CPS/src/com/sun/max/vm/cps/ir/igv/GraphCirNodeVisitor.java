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

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Creates nodes and edges for CirNode objects.
 *
 * @author Thomas Wuerthinger
 */
class GraphCirNodeVisitor extends CirTraversal {

    private GraphWriter.Graph graph;

    GraphCirNodeVisitor(GraphWriter.Graph graph, CirNode node) {
        super(node);
        this.graph = graph;
    }

    @Override
    public void visitNode(CirNode n) {
        if (graph.getNode(n.id()) == null) {
            final GraphWriter.Node inputNode = graph.createNode(n.id());
            inputNode.getProperties().setProperty("name", n.toString());
            inputNode.getProperties().setProperty("class", n.getClass().getName());
            inputNode.getProperties().setProperty("dump_spec", n.toString());
            inputNode.getProperties().setProperty("short_name", n.toString());
        }

        assert graph.getNode(n.id()) != null;
        graph.getNode(n.id()).getProperties().setProperty("type", "Node");
    }

    @Override
    public void visitBlock(CirBlock block) {
        super.visitBlock(block);
        visitNode(block);
        final CirClosure closure = block.closure();
        graph.createEdge(block.id(), closure.id());
        graph.getNode(block.id()).getProperties().setProperty("type", "Block");
    }

    @Override
    public void visitCall(CirCall call) {
        super.visitCall(call);
        visitNode(call);
        graph.createEdge(call.id(), call.procedure().id());
        int z = 1;
        for (CirValue v : call.arguments()) {
            graph.createEdge(call.id(), z, v.id(), 0);
            z++;
        }
        graph.getNode(call.id()).getProperties().setProperty("name", "call");
        graph.getNode(call.id()).getProperties().setProperty("type", "Call");
    }

    @Override
    public void visitClosure(CirClosure closure) {
        super.visitClosure(closure);
        visitNode(closure);

        int z = 0;
        for (CirVariable v : closure.parameters()) {
            graph.createEdge(closure.id(), z, v.id(), 0);
            z++;
        }

        graph.createEdge(closure.id(), z, closure.body().id(), 0);
        graph.getNode(closure.id()).getProperties().setProperty("name", "proc");
        graph.getNode(closure.id()).getProperties().setProperty("type", "Closure");
    }

    @Override
    public void visitContinuation(CirContinuation continuation) {
        super.visitContinuation(continuation);
        graph.getNode(continuation.id()).getProperties().setProperty("name", "cont");
        graph.getNode(continuation.id()).getProperties().setProperty("type", "Continuation");
    }

    @Override
    public void visitLocalVariable(CirLocalVariable variable) {
        super.visitLocalVariable(variable);
        graph.getNode(variable.id()).getProperties().setProperty("type", "LocalVariable");
    }

    @Override
    public void visitMethod(CirMethod method) {
        super.visitMethod(method);
        graph.getNode(method.id()).getProperties().setProperty("type", "Method");
    }

    @Override
    public void visitMethodParameter(CirMethodParameter parameter) {
        super.visitMethodParameter(parameter);
        graph.getNode(parameter.id()).getProperties().setProperty("type", "Parameter");
    }
}
