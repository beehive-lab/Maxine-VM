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
package com.sun.max.vm.jit;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 * A buffer that can be addressed in a random access way,
 * reading or writing at positions that have not been touched before.
 * It returns zeros when reading from unwritten positions
 * and it extends the internal backing storage as needed
 * when writing beyond the previous upper addressing bound.
 *
 *
 * @author Bernd Mathiske
 */
public class RandomAccessWordBuffer {
    RandomAccessWordBuffer() {
    }

    private Word[] words = new Word[32];

    private int size;

    public int size() {
        return size;
    }

    private void grow() {
        final Word[] newWords = new Word[size * 2];
        WordArray.copyAll(words, newWords);
        words = newWords;
    }

    public void extend(int size) {
        if (size > this.size) {
            this.size = size;
            if (words.length * Word.size() < this.size) {
                grow();
            }
        }
    }

    private int position;

    public void setPosition(int position) {
        assert position % Word.size() == 0;
        this.position = position;
    }

    public void writeWord(Word value) {
        extend(position + Word.size());
        final int index = Unsigned.idiv(position, Word.size());
        WordArray.set(words, index, value);
    }

    public void copyToMemory(Pointer destination) {
        for (int i = 0; i < size; i++) {
            destination.setWord(i, WordArray.get(words, i));
        }
    }
}
