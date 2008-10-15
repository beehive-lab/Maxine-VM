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

import com.sun.max.vm.compiler.cir.*;

/**
 * A traversal that determines how often a node appears in a graph, without entering blocks.
 *
 * @author Bernd Mathiske
 */
public final class CirCount extends CirTraversal {
    private boolean _enterBlock;
    private int _numberOfOccurrences = 0;

    private CirCount(CirNode graph, boolean enterBlock) {
        super(graph);
        _enterBlock = enterBlock;
    }

    @Override
    public void visitBlock(CirBlock block) {
        if (_enterBlock) {
            super.visitBlock(block);
        }
    }

    private void run(CirNode countedNode) {
        while (!_toDo.isEmpty()) {
            final CirNode node = _toDo.removeFirst();
            if (node == countedNode) {
                _numberOfOccurrences++;
            }
            node.acceptVisitor(this);
        }
    }

    private void run(CirPredicate predicate) {
        while (!_toDo.isEmpty()) {
            final CirNode node = _toDo.removeFirst();
            if (node.acceptPredicate(predicate)) {
                _numberOfOccurrences++;
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
        return count._numberOfOccurrences;
    }

    public static int apply(CirNode graph, CirPredicate predicate) {
        final CirCount count = new CirCount(graph, true);
        count.run(predicate);
        return count._numberOfOccurrences;
    }
}
