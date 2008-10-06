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
/*VCSID=92e9cf76-94a2-43cd-b2c7-25ad521de0c9*/
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.vm.compiler.cir.*;

/**
 * A traversal that returns a boolean that indicates
 * whether a node has been found during the search.
 * Aborts searching as soon as that node has been detected.
 *
 * @author Bernd Mathiske
 */
public class CirSearch extends CirTraversal {

    protected CirSearch(CirNode graph) {
        super(graph);
    }

    private CirNode runPredicate(CirPredicate predicate) {
        while (!_toDo.isEmpty()) {
            final CirNode node = _toDo.removeFirst();
            if (node.acceptPredicate(predicate)) {
                return node;
            }
            node.acceptVisitor(this);
        }
        return null;
    }

    protected boolean runNode(CirNode node) {
        while (!_toDo.isEmpty()) {
            final CirNode current = _toDo.removeFirst();
            if (current == node) {
                return true;
            }
            current.acceptVisitor(this);
        }
        return false;
    }

    public static CirNode byPredicate(CirNode graph, CirPredicate predicate) {
        final CirSearch search = new CirSearch(graph);
        return search.runPredicate(predicate);
    }

    /**
     * Searches only outside of blocks, i.e. does not look into block nodes.
     */
    public static class OutsideBlocks extends CirSearch {
        protected OutsideBlocks(CirNode graph) {
            super(graph);
        }

        @Override
        public void visitBlock(CirBlock block) {
        }

        public static boolean contains(CirNode graph, CirNode node) {
            final OutsideBlocks search = new OutsideBlocks(graph);
            return search.runNode(node);
        }
    }

    /**
     * Searches only outside of blocks, i.e. does not look into block nodes AND does not look into JavaFrameDescriptors either.
     */
    public static final class OutsideBlocksAndJavaFrameDescriptors extends OutsideBlocks {
        private OutsideBlocksAndJavaFrameDescriptors(CirNode graph) {
            super(graph);
        }

        @Override
        protected void visitJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        }

        public static boolean contains(CirNode graph, CirNode node) {
            final OutsideBlocksAndJavaFrameDescriptors search = new OutsideBlocksAndJavaFrameDescriptors(graph);
            return search.runNode(node);
        }
    }
}
