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

import com.sun.max.vm.cps.cir.*;

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
        while (!toDo.isEmpty()) {
            final CirNode node = toDo.removeFirst();
            if (node.acceptPredicate(predicate)) {
                return node;
            }
            node.acceptVisitor(this);
        }
        return null;
    }

    protected boolean runNode(CirNode node) {
        while (!toDo.isEmpty()) {
            final CirNode current = toDo.removeFirst();
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
