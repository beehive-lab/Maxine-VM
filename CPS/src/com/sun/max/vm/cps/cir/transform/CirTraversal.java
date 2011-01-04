/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.transform;

import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Traverses a CIR graph,
 * visiting each node exactly once.
 *
 * @author Bernd Mathiske
 */
public abstract class CirTraversal extends CirVisitor {

    protected final LinkedList<CirNode> toDo = new LinkedList<CirNode>() {
        @Override
        public boolean add(CirNode e) {
            assert e != null;
            return super.add(e);
        }
    };

    protected CirTraversal(CirNode graph) {
        toDo.add(graph);
    }

    private void addValues(CirValue[] values) {
        for (CirValue value : values) {
            if (value != null) {
                toDo.add(value);
            }
        }
    }

    protected void visitJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        CirJavaFrameDescriptor j = javaFrameDescriptor;
        while (j != null) {
            addValues(j.locals);
            addValues(j.stackSlots);
            j = j.parent();
        }
    }

    @Override
    public void visitCall(CirCall call) {
        toDo.add(call.procedure());
        addValues(call.arguments());
        visitJavaFrameDescriptor(call.javaFrameDescriptor());
    }

    @Override
    public void visitClosure(CirClosure closure) {
        for (CirVariable parameter : closure.parameters()) {
            toDo.add(parameter);
        }
        toDo.add(closure.body());
    }

    protected final IdentityHashSet<CirBlock> visitedBlocks = new IdentityHashSet<CirBlock>();

    @Override
    public void visitBlock(CirBlock block) {
        if (!visitedBlocks.contains(block)) {
            visitedBlocks.add(block);
            toDo.add(block.closure());
        }
    }

    public void run() {
        while (!toDo.isEmpty()) {
            final CirNode node = toDo.removeFirst();
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
