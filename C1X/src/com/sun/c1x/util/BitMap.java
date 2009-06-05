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

/**
 * The <code>BitMap</code> class implements a bitmap that stores a single bit for
 * a range of integers (0-n).
 *
 * @author Ben L. Titzer
 */
public class BitMap {

    private final int _length;
    private int _low32;
    private int[] _extra32;

    /**
     * Construct a new bit map with the specified length
     * @param length the length of the bitmap
     */
    public BitMap(int length) {
        this._length = length;
        if (length > 32) {
            _extra32 = new int[length >> 5];
        }
    }

    /**
     * Set the bit at the specified index.
     * @param i the index of the bit to set
     */
    public void set(int i) {
        if (checkIndex(i) < 32) {
            _low32 |= 1 << i;
        } else {
            int pos = i >> 5;
            int index = i & 31;
            _extra32[pos] |= 1 << index;
        }
    }

    /**
     * Clears the bit at the specified index.
     * @param i the index of the bit to clear
     */
    public void clear(int i) {
        if (checkIndex(i) < 32) {
            _low32 &= ~(1 << i);
        } else {
            int pos = i >> 5;
            int index = i & 31;
            _extra32[pos] &= ~(1 << index);
        }
    }

    /**
     * Sets all the bits in this bitmap.
     */
    public void setAll() {
        _low32 = -1;
        if (_extra32 != null) {
            for (int i = 0; i < _extra32.length; i++) {
                _extra32[i] = -1;
            }
        }
    }

    /**
     * Clears all the bits in this bitmap.
     */
    public void clearAll() {
        _low32 = 0;
        if (_extra32 != null) {
            for (int i = 0; i < _extra32.length; i++) {
                _extra32[i] = 0;
            }
        }
    }

    /**
     * Gets the value of the bit at the specified index.
     * @param i the index of the bit to get
     * @return <code>true</code> if the bit at the specified position is <code>1</code>
     */
    public boolean get(int i) {
        if (checkIndex(i) < 32) {
            return ((_low32 >> i) & 1) != 0;
        }
        int pos = i >> 5;
        int index = i & 31;
        int bits = _extra32[pos];
        return ((bits >> index) & 1) != 0;
    }

    /**
     * Performs the union operation on this bitmap with the specified bitmap.
     * That is, all bits set in either of the two bitmaps will be set in
     * this bitmap following this operation.
     * @param other the other bitmap for the union operation
     */
    public void setUnion(BitMap other) {
        _low32 |= other._low32;
        if (_extra32 != null) {
            for (int i = 0; i < _extra32.length; i++) {
                _extra32[i] |= other._extra32[i];
            }
        }
    }

    /**
     * Gets the size of this bitmap in bits.
     * @return the size of this bitmap
     */
    public int size() {
        return _length;
    }

    private int checkIndex(int i) {
        if (i < 0 || i >= _length) {
            throw new IndexOutOfBoundsException();
        }
        return i;
    }
}
