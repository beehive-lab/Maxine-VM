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
package com.sun.max.vm.cps.cir.transform;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Traverses a CIR graph <i>iteratively</i> in a depth first manner, calling a delegate visitor for each node in the
 * graph. The delegate visitor is signaled for <i>each edge to a node</i>. As implied by <i>depth first</i>, the
 * delegate visitor is called for a node once all of it's children have been traversed.
 * The delegate visitor must not alter the structure of the graph.
 *
 * @author Doug Simon
 */
public final class CirDepthFirstTraversal extends CirVisitor {

    private TraversedNode currentNode;
    private final Stack<TraversedNode> toDo = new Stack<TraversedNode>();

    public static class TraversedNode {

        private final CirNode node;

        private final int depth;

        /**
         * Set to true once all the children of the node have been traversed.
         */
        boolean done;

        TraversedNode(CirNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }

        /**
         * Determines if this is the first time the node was encountered during the traversal.
         */
        public boolean isFirstTraversal() {
            return true;
        }

        /**
         * Gets the number of ancestors between the node and the root of the graph being traversed.
         */
        public int depth() {
            return depth;
        }

        TraversedNode asSecondaryTraversedNode() {
            return new TraversedNode(node, depth) {
                @Override
                public boolean isFirstTraversal() {
                    return false;
                }
            };
        }
    }

    /**
     * Creates a traversing object for a CIR graph rooted by a given node.
     */
    public CirDepthFirstTraversal() {
        this(new DefaultBlockSet());
    }

    /**
     * Creates a traversing object for a CIR graph rooted by a given node.
     *
     * @param blocks
     *                a remembered set to prevent traversing the children of a CirBlock more than once
     */
    public CirDepthFirstTraversal(BlockSet blocks) {
        this.blocks = blocks;
    }

    /**
     * Defines a set used to remember the blocks that have been visited during a depth first traversal.
     */
    public static interface BlockSet {
        boolean containsBlock(CirBlock block);
        void recordBlock(CirBlock block);
    }

    public static class DefaultBlockSet extends HashSet<CirBlock> implements BlockSet {
        public boolean containsBlock(CirBlock block) {
            return contains(block);
        }

        public void recordBlock(CirBlock block) {
            super.add(block);
        }
    }

    public abstract static class AbstractBlockSetMap<Value_Type> extends ChainedHashMapping<CirBlock, Value_Type> implements BlockSet {
        public boolean containsBlock(CirBlock block) {
            return containsKey(block);
        }
    }

    private final BlockSet blocks;

    @Override
    public void visitBlock(CirBlock block) {
        if (!blocks.containsBlock(block)) {
            blocks.recordBlock(block);
            toDo.push(new TraversedNode(block.closure(), currentNode.depth + 1));
        } else {
            assert currentNode == toDo.peek();
            currentNode = currentNode.asSecondaryTraversedNode();
            toDo.set(toDo.size() - 1, currentNode);
        }
    }

    private void traverseValues(CirValue[] values, int childDepth) {
        for (int i = values.length - 1; i >= 0; --i) {
            toDo.push(new TraversedNode(values[i], childDepth));
        }
    }

    @Override
    public void visitCall(CirCall call) {
        final int childDepth = currentNode.depth + 1;

        traverseValues(call.arguments(), childDepth);

        CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
        while (javaFrameDescriptor != null) {
            traverseValues(javaFrameDescriptor.locals, childDepth);
            traverseValues(javaFrameDescriptor.stackSlots, childDepth);
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }

        toDo.push(new TraversedNode(call.procedure(), childDepth));
    }

    @Override
    public void visitClosure(CirClosure closure) {
        final CirVariable[] parameters = closure.parameters();
        final int childDepth = currentNode.depth + 1;
        for (int i = parameters.length - 1; i >= 0; --i) {
            toDo.push(new TraversedNode(parameters[i], childDepth));
        }
        toDo.push(new TraversedNode(closure.body(), childDepth));
    }

    /**
     * Gets the traversal info of the node currently being visited by the delegate passed to {@link #run(CirVisitor)}.
     */
    public TraversedNode currentNode() {
        return currentNode;
    }

    /**
     * @param root
     *                the root of the CIR graph to traverse
     * @param delegate
     */
    public void run(CirNode root, CirVisitor delegate) {
        toDo.push(new TraversedNode(root, 0));
        while (!toDo.isEmpty()) {
            currentNode = toDo.peek();
            if (currentNode.done) {
                toDo.pop();
                currentNode.node.acceptVisitor(delegate);
            } else {
                currentNode.node.acceptVisitor(this);
                currentNode.done = true;
            }
        }
    }

}
