/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.graph;

import com.sun.c1x.ir.BlockBegin;

/**
 * The <code>BlockUtil</code> class contains a number of utilities for manipulating a CFG of basic blocks.
 *
 * @author Ben L. Titzer
 */
public class BlockUtil {

    /**
     * Remove an edge between two basic blocks.
     * @param from the origin block of the edge
     * @param to the destination block of the edge
     */
    public static void disconnectEdge(BlockBegin from, BlockBegin to) {
        from.end().successors().remove(to);
        to.removePredecessor(from);
    }

    /**
     * Adds an edge between two basic blocks.
     * @param from the origin of the edge
     * @param to the destination of the edge
     */
    public static void addEdge(BlockBegin from, BlockBegin to) {
        from.end().successors().add(to);
        to.addPredecessor(from);
    }

    /**
     * Disconnects the specified block from all other blocks.
     * @param block the block to remove from the graph
     */
    public static void disconnectFromGraph(BlockBegin block) {
        for (BlockBegin p : block.predecessors()) {
            p.end().successors().remove(block);
        }
        for (BlockBegin s : block.end().successors()) {
            s.predecessors().remove(block);
        }
    }
}
