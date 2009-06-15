/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.compiler.ir.igv;

import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * Creates a graph for BirMethod, CirMethod, DirMethod, and EirMethod objects.
 * @author Thomas Wuerthinger
 */
class GraphIrMethodVisitor implements IrMethodVisitor {

    private final GraphWriter.Group _group;
    private final Object _context;
    private final String _stateName;

    GraphIrMethodVisitor(GraphWriter.Group group, Object context, String stateName) {
        _group = group;
        _context = context;
        _stateName = stateName;
    }

    /**
     * Creates a control flow graph for BirMethod objects and adds it to the {@link _group} field.
     * @param method the method for which the graph is generated
     */
    public void visit(BirMethod method) {
        final String graphName = method.classMethodActor().format("%H.%n(%p)") + "BIR";
        final GraphWriter.Graph graph = _group.createGraph(graphName);

        for (BirBlock block : method.blocks()) {
            final GraphWriter.Node node = graph.createNode(block.serial());
            node.getProperties().setProperty("name", "Block (" + block.serial() + "-" + block.bytecodeBlock().end() + ")");
            node.getProperties().setProperty("start", Integer.toString(block.bytecodeBlock().start()));
            node.getProperties().setProperty("bci", Integer.toString(block.bytecodeBlock().start()));
            node.getProperties().setProperty("end", Integer.toString(block.bytecodeBlock().end()));
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
        if (_context == null) {
            return;
        }
        assert _context instanceof CirNode;
        final GraphWriter.Group group = _group;
        final GraphWriter.Graph graph = group.createGraph(_stateName);
        final CirNode node = (CirNode) _context;
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
