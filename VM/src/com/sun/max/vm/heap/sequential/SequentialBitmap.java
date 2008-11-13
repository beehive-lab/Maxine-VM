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
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package com.sun.max.vm.heap.sequential;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public class SequentialBitmap {

    private Pointer _bitmapStart;
    private Address _start;


    public SequentialBitmap(Address start, Size heapSize) {
        _start = start;
        final Size bitmapSize = heapSize.dividedBy(Word.size());
        _bitmapStart = Memory.allocate(bitmapSize);
    }

    @INLINE
    public final int addressToIndex(Address address) {
        return address.minus(_start).toInt();
    }

    @INLINE
    public final int bitPosition(int bit) {
        return bit & (Word.numberOfBits() - 1);
    }

    //Note: this is not MT-safe. Will only work with a serial collector. Need an atomic flag & a work queue for parallel GC.
    @INLINE
    public final void mark(Address address) {
        final int index = addressToIndex(address);
        int current = _bitmapStart.getInt();
        current |= 1 << bitPosition(index);
        _bitmapStart.setInt(current);
    }

    @INLINE
    public final boolean isMarked(Address address) {
        final int index = addressToIndex(address);
        final int current = _bitmapStart.getInt();
        return (current & (1 << bitPosition(index))) > 0 ? true : false;
    }

}
