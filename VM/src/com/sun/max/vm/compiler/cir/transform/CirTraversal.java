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
package com.sun.max.vm.compiler.cir.transform;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Traverses a CIR graph,
 * visiting each node exactly once.
 *
 * @author Bernd Mathiske
 */
public abstract class CirTraversal extends CirVisitor {

    protected final LinkedList<CirNode> _toDo = new LinkedList<CirNode>() {
        @Override
        public boolean add(CirNode e) {
            assert e != null;
            return super.add(e);
        }
    };

    protected CirTraversal(CirNode graph) {
        _toDo.add(graph);
    }

    private void addValues(CirValue[] values) {
        for (CirValue value : values) {
            if (value != null) {
                _toDo.add(value);
            }
        }
    }

    protected void visitJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        CirJavaFrameDescriptor j = javaFrameDescriptor;
        while (j != null) {
            addValues(j.locals());
            addValues(j.stackSlots());
            j = j.parent();
        }
    }

    @Override
    public void visitCall(CirCall call) {
        _toDo.add(call.procedure());
        addValues(call.arguments());
        visitJavaFrameDescriptor(call.javaFrameDescriptor());
    }

    @Override
    public void visitClosure(CirClosure closure) {
        for (CirVariable parameter : closure.parameters()) {
            _toDo.add(parameter);
        }
        _toDo.add(closure.body());
    }

    protected final IdentityHashSet<CirBlock> _visitedBlocks = new IdentityHashSet<CirBlock>();

    @Override
    public void visitBlock(CirBlock block) {
        if (!_visitedBlocks.contains(block)) {
            _visitedBlocks.add(block);
            _toDo.add(block.closure());
        }
    }

    public void run() {
        while (!_toDo.isEmpty()) {
            final CirNode node = _toDo.removeFirst();
            node.acceptVisitor(this);
        }
    }

    /**
     * Traverses only outside of blocks, i.e. does not look into block nodes.
     */
    public abstract static class OutsideBlocks extends CirTraversal {
        protected OutsideBlocks(CirNode graph) {
            super(graph);
        }

        @Override
        public void visitBlock(CirBlock block) {
        }
    }
}
