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
 * Traverses a CIR graph <i>iteratively</i> in a depth first manner, calling a delegate visitor for each node in the
 * graph. The delegate visitor is signaled for <i>each edge to a node</i>. As implied by <i>depth first</i>, the
 * delegate visitor is called for a node once all of it's children have been traversed.
 * The delegate visitor must not alter the structure of the graph.
 * 
 * @author Doug Simon
 */
public final class CirDepthFirstTraversal extends CirVisitor {

    private TraversedNode _currentNode;
    private final Stack<TraversedNode> _toDo = new Stack<TraversedNode>();

    public static class TraversedNode {

        private final CirNode _node;

        private final int _depth;

        /**
         * Set to true once all the children of the node have been traversed.
         */
        boolean _done;

        TraversedNode(CirNode node, int depth) {
            _node = node;
            _depth = depth;
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
            return _depth;
        }

        TraversedNode asSecondaryTraversedNode() {
            return new TraversedNode(_node, _depth) {
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
        _blocks = blocks;
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

    private final BlockSet _blocks;

    @Override
    public void visitBlock(CirBlock block) {
        if (!_blocks.containsBlock(block)) {
            _blocks.recordBlock(block);
            _toDo.push(new TraversedNode(block.closure(), _currentNode._depth + 1));
        } else {
            assert _currentNode == _toDo.peek();
            _currentNode = _currentNode.asSecondaryTraversedNode();
            _toDo.set(_toDo.size() - 1, _currentNode);
        }
    }

    private void traverseValues(CirValue[] values, int childDepth) {
        for (int i = values.length - 1; i >= 0; --i) {
            _toDo.push(new TraversedNode(values[i], childDepth));
        }
    }

    @Override
    public void visitCall(CirCall call) {
        final int childDepth = _currentNode._depth + 1;

        traverseValues(call.arguments(), childDepth);

        CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
        while (javaFrameDescriptor != null) {
            traverseValues(javaFrameDescriptor.locals(), childDepth);
            traverseValues(javaFrameDescriptor.stackSlots(), childDepth);
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }

        _toDo.push(new TraversedNode(call.procedure(), childDepth));
    }

    @Override
    public void visitClosure(CirClosure closure) {
        final CirVariable[] parameters = closure.parameters();
        final int childDepth = _currentNode._depth + 1;
        for (int i = parameters.length - 1; i >= 0; --i) {
            _toDo.push(new TraversedNode(parameters[i], childDepth));
        }
        _toDo.push(new TraversedNode(closure.body(), childDepth));
    }

    /**
     * Gets the traversal info of the node currently being visited by the delegate passed to {@link #run(CirVisitor)}.
     */
    public TraversedNode currentNode() {
        return _currentNode;
    }

    /**
     * @param root
     *                the root of the CIR graph to traverse
     * @param delegate
     */
    public void run(CirNode root, CirVisitor delegate) {
        _toDo.push(new TraversedNode(root, 0));
        while (!_toDo.isEmpty()) {
            _currentNode = _toDo.peek();
            if (_currentNode._done) {
                _toDo.pop();
                _currentNode._node.acceptVisitor(delegate);
            } else {
                _currentNode._node.acceptVisitor(this);
                _currentNode._done = true;
            }
        }
    }

}
