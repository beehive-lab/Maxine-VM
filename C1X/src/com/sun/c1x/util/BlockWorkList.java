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
package com.sun.c1x.util;

import com.sun.c1x.ir.*;

/**
 * This class implements a worklist for dealing with blocks. The worklist can
 * operate either as a stack (i.e. first-in / last-out), or as a sorted list,
 * where blocks can be sorted by a supplied number. The latter usage lends itself
 * naturally to iterative dataflow analysis problems.
 *
 * This implementation is not able to tell if a block is in the worklist already.
 * Note that this implementation is slightly less efficient than the dedicated
 * work list in {@link com.sun.c1x.graph.ScopeData}, because this worklist uses
 * an externally supplied number.
 *
 * @author Ben L. Titzer
 */
public class BlockWorkList {
    BlockBegin[] workList;
    int[] workListNumbers;
    int workListIndex;

    /**
     * Adds a block to this list in an unsorted fashion, like a stack.
     * @param block the block to add
     */
    public void add(BlockBegin block) {
        if (workList == null) {
            // worklist not allocated yet
            allocate();
        } else if (workListIndex == workList.length) {
            // need to grow the worklist
            grow();
        }
        // put the block at the end of the array
        workList[workListIndex++] = block;
    }

    /**
     * Adds a block to this list, sorted by the supplied number. The block
     * with the lowest number is returned upon subsequent removes.
     * @param block the block to add
     * @param number the number used to sort the block
     */
    public void addSorted(BlockBegin block, int number) {
        if (workList == null) {
            // worklist not allocated yet
            allocate();
        } else if (workListIndex == workList.length) {
            // need to grow the worklist
            grow();
        }
        // put the block at the end of the array
        workList[workListIndex] = block;
        workListNumbers[workListIndex] = number;
        workListIndex++;
        int i = workListIndex - 2;
        // push block towards the beginning of the array
        for (; i >= 0; i--) {
            int n = workListNumbers[i];
            if (n >= number) {
                break; // already in the right position
            }
            workList[i + 1] = workList[i]; // bubble b down by one
            workList[i] = block;           // and overwrite its place with block
            workListNumbers[i + 1] = n;    // bubble n down by one
            workListNumbers[i] = number;   // and overwrite its place with number
        }
    }

    /**
     * Removes the next block from this work list. If the blocks have been added
     * in a sorted order, then the block with the lowest number is returned. Otherwise,
     * the last block added is returned.
     * @return the next block in the list
     */
    public BlockBegin removeFromWorkList() {
        if (workListIndex != 0) {
            return workList[--workListIndex];
        }
        return null;
    }

    /**
     * Checks whether the list is empty.
     * @return {@code true} if this list is empty
     */
    public boolean isEmpty() {
        return workListIndex == 0;
    }

    private void allocate() {
        workList = new BlockBegin[5];
        workListNumbers = new int[5];
    }

    private void grow() {
        int prevLength = workList.length;
        BlockBegin[] nworkList = new BlockBegin[prevLength * 3];
        System.arraycopy(workList, 0, nworkList, 0, prevLength);
        workList = nworkList;

        int[] nworkListNumbers = new int[prevLength * 3];
        System.arraycopy(workListNumbers, 0, nworkListNumbers, 0, prevLength);
        workListNumbers = nworkListNumbers;
    }
}
