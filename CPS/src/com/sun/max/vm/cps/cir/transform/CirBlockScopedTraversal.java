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
 * visiting each node exactly once,
 * keeping track of which block the currently visited node is in.
 *
 * @author Bernd Mathiske
 */
public class CirBlockScopedTraversal extends CirBlockScopedVisitor {

    private static final class Item {
        private CirNode node;
        private CirBlock scope;

        Item(CirNode node, CirBlock scope) {
            this.node = node;
            this.scope = scope;
        }
    }

    protected final LinkedList<Item> toDo = new LinkedList<Item>();

    private void add(CirNode node, CirBlock scope) {
        toDo.add(new Item(node, scope));
    }

    protected CirBlockScopedTraversal(CirNode graph) {
        super();
        toDo.add(new Item(graph, null));
    }

    private void addValues(CirValue[] values, CirBlock scope) {
        for (CirValue value : values) {
            if (value != null) {
                add(value, scope);
            }
        }
    }

    @Override
    public void visitCall(CirCall call, CirBlock scope) {
        add(call.procedure(), scope);
        addValues(call.arguments(), scope);
        CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
        while (javaFrameDescriptor != null) {
            addValues(javaFrameDescriptor.locals, scope);
            addValues(javaFrameDescriptor.stackSlots, scope);
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }
    }

    @Override
    public void visitClosure(CirClosure closure, CirBlock scope) {
        for (CirVariable parameter : closure.parameters()) {
            add(parameter, scope);
        }
        add(closure.body(), scope);
    }

    protected final IdentityHashSet<CirBlock> visitedBlocks = new IdentityHashSet<CirBlock>();

    @Override
    public void visitBlock(CirBlock block, CirBlock scope) {
        if (!visitedBlocks.contains(block)) {
            visitedBlocks.add(block);
            add(block.closure(), block);
        }
    }

    public void run() {
        while (!toDo.isEmpty()) {
            final Item item = toDo.removeFirst();
            item.node.acceptBlockScopedVisitor(this, item.scope);
        }
    }

}
