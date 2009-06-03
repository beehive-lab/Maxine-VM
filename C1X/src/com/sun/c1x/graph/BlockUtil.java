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
import com.sun.c1x.ir.Goto;
import com.sun.c1x.value.ValueStack;

import java.util.List;
import java.util.Iterator;

/**
 * The <code>BlockUtil</code> class contains a number of utilities for manipulating a CFG of basic blocks.
 *
 * @author Ben L. Titzer
 */
public class BlockUtil {

    /**
     * Creates and inserts a new block between this block and the specified successor,
     * altering the successor and predecessor lists of involved blocks appropriately.
     * Note that this method only splits the first occurrence of any edges between
     * these two blocks (a block that ends in a switch may have multiple edges between
     * the source and target).
     * @param source the source of the edge
     * @param target the successor before which to insert a block
     * @return the new block inserted
     */
    public BlockBegin splitFirstEdge(BlockBegin source, BlockBegin target) {
        int bci;
        if (target.predecessors().size() == 1) {
            bci = target.bci();
        } else {
            bci = source.end().bci();
        }

        // create new successor and mark it for special block order treatment
        BlockBegin newSucc = new BlockBegin(bci);
        newSucc.setBlockFlag(BlockBegin.BlockFlag.CriticalEdgeSplit);

        // This goto is not a safepoint.
        Goto e = new Goto(target, null, false);
        newSucc.setNext(e, bci);
        newSucc.setEnd(e);
        // setup states
        ValueStack s = source.end().state();
        newSucc.setState(s.copy());
        e.setState(s.copy());
        assert newSucc.state().localsSize() == s.localsSize();
        assert newSucc.state().stackSize() == s.stackSize();
        assert newSucc.state().locksSize() == s.locksSize();
        // link predecessor to new block
        source.end().substituteSuccessor(target, newSucc);

        // The ordering needs to be the same, so remove the link that the
        // set_end call above added and substitute the new_sux for this
        // block.
        target.removePredecessor(newSucc);

        // the successor could be the target of a switch so it might have
        // multiple copies of this predecessor, so substitute the new_sux
        // for the first and delete the rest.
        // XXX: wtf? why delete other occurrences?
        List<BlockBegin> list = target.predecessors();
        int x = list.indexOf(source);
        assert x >= 0;
        list.set(x, newSucc);
        Iterator<BlockBegin> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == source) {
                iterator.remove();
            }
        }
        return newSucc;
    }

    /**
     * Remove an edge between two basic blocks.
     * @param from the origin block of the edge
     * @param to the destination block of the edge
     */
    public static void disconnectEdge(BlockBegin from, BlockBegin to) {
        from.removeSuccessor(to);
        to.removePredecessor(from);
    }

    /**
     * Adds an edge between two basic blocks.
     * @param from the origin of the edge
     * @param to the destination of the edge
     */
    public static void addEdge(BlockBegin from, BlockBegin to) {
        from.addSuccessor(to);
        to.addPredecessor(from);
    }

    /**
     * Disconnects the specified block from all other blocks.
     * @param block the block to remove from the graph
     */
    public static void disconnectFromGraph(BlockBegin block) {
        for (BlockBegin p : block.predecessors()) {
            p.successors().remove(block);
        }
        for (BlockBegin s : block.successors()) {
            s.predecessors().remove(block);
        }
    }
}
