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
 * A traversal that determines how often a node appears in a graph, without entering blocks.
 *
 * @author Bernd Mathiske
 */
public final class CirCount extends CirTraversal {
    private boolean enterBlock;
    private int numberOfOccurrences = 0;

    private CirCount(CirNode graph, boolean enterBlock) {
        super(graph);
        this.enterBlock = enterBlock;
    }

    @Override
    public void visitBlock(CirBlock block) {
        if (enterBlock) {
            super.visitBlock(block);
        }
    }

    private void run(CirNode countedNode) {
        while (!toDo.isEmpty()) {
            final CirNode node = toDo.removeFirst();
            if (node == countedNode) {
                numberOfOccurrences++;
            }
            node.acceptVisitor(this);
        }
    }

    private void run(CirPredicate predicate) {
        while (!toDo.isEmpty()) {
            final CirNode node = toDo.removeFirst();
            if (node.acceptPredicate(predicate)) {
                numberOfOccurrences++;
            }
            node.acceptVisitor(this);
        }
    }
    /**
     * Searches for a node in a graph, without entering blocks.
     *
     * @param graph the graph to search in
     * @param countedNode the node to look for
     * @return the number of times the node appears in graph
     */
    public static int apply(CirNode graph, CirNode countedNode) {
        final CirCount count = new CirCount(graph, false);
        count.run(countedNode);
        return count.numberOfOccurrences;
    }

    public static int apply(CirNode graph, CirPredicate predicate) {
        final CirCount count = new CirCount(graph, true);
        count.run(predicate);
        return count.numberOfOccurrences;
    }
}
