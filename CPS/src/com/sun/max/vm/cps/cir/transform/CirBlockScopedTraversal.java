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
