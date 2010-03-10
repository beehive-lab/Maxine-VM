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
package com.sun.c1x.ir;

import java.util.Iterator;
import java.util.Arrays;

/**
 * The {@code BlockList} class implements a specialized list data structure for representing
 * the predecessor and successor lists of basic blocks.
 *
 * @author Ben L. Titzer
 */
public class BlockList implements Iterable<BlockBegin> {

    private BlockBegin[] array;
    private int cursor;

    BlockList(int sizeHint) {
        if (sizeHint > 0) {
            array = new BlockBegin[sizeHint];
        } else {
            array = new BlockBegin[2];
        }
    }

    public void remove(int index) {
        if (index < 0 || index >= cursor) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = index; i < cursor; i++) {
            array[i] = array[i + 1];
        }
        cursor--;
    }

    public void remove(BlockBegin block) {
        int j = 0;
        for (int i = 0; i < cursor; i++) {
            if (i != j) {
                array[j] = array[i];
            }
            if (array[i] != block) {
                j++;
            }
        }
        cursor = j;
    }

    public void exchange(int index1, int index2) {
        if (index1 < 0 || index1 >= cursor) {
            throw new IndexOutOfBoundsException();
        }
        if (index2 < 0 || index2 >= cursor) {
            throw new IndexOutOfBoundsException();
        }
        BlockBegin t = array[index2];
        array[index2] = array[index1];
        array[index1] = t;
    }

    public void insert(int index, BlockBegin block) {
        if (index < 0 || index >= cursor) {
            throw new IndexOutOfBoundsException();
        }
        growOne();
        for (int i = cursor; i > index; i--) {
            array[i] = array[i - 1];
        }
        array[cursor++] = block;
    }

    public void append(BlockBegin block) {
        growOne();
        array[cursor++] = block;
    }

    public BlockBegin get(int index) {
        if (index < 0 || index >= cursor) {
            throw new IndexOutOfBoundsException();
        }
        return array[index];
    }

    public void replace(BlockBegin oldBlock, BlockBegin newBlock) {
        for (int i = 0; i < cursor; i++) {
            if (array[i] == oldBlock) {
                array[i] = newBlock;
            }
        }
    }

    public boolean checkForSameBlock() {
        if (cursor == 0) {
            return true;
        }
        BlockBegin b = array[0];
        for (int i = 1; i < cursor; i++) {
            if (array[i] != b) {
                return false;
            }
        }
        return true;
    }

    public Iterator<BlockBegin> iterator() {
        return new Iter();
    }

    private void growOne() {
        if (cursor == array.length) {
            array = Arrays.copyOf(array, array.length * 3);
        }
    }

    private class Iter implements Iterator<BlockBegin> {
        private int pos;

        public boolean hasNext() {
            return pos < cursor;
        }

        public BlockBegin next() {
            return array[pos++];
        }

        public void remove() {
            BlockList.this.remove(pos);
        }
    }
}
