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

import java.util.*;

/**
 * The <code>BitMap</code> class implements a bitmap that stores a single bit for
 * a range of integers (0-n).
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class BitMap {

    private int length;
    private int low;
    private int[] extra;

    /**
     * Construct a new bit map with the specified length.
     * @param length the length of the bitmap
     */
    public BitMap(int length) {
        this.length = length;
        if (length > 32) {
            extra = new int[length >> 5];
        }
    }

    /**
     * Set the bit at the specified index.
     * @param i the index of the bit to set
     */
    public void set(int i) {
        if (checkIndex(i) < 32) {
            low |= 1 << i;
        } else {
            int pos = wordIndex(i);
            int index = bitInWord(i);
            extra[pos] |= 1 << index;
        }
    }

    /**
     * Grows this bitmap to a new length, appending necessary zero bits.
     * @param newLength the new length of the bitmap
     */
    public void grow(int newLength) {
        if (newLength > length) {
            // grow this bitmap to the new length
            int newSize = newLength >> 5;
            if (newLength > 0) {
                if (extra == null) {
                    // extra just needs to be allocated now
                    extra = new int[newSize];
                } else {
                    if (extra.length < newSize) {
                        // extra needs to be copied
                        int[] newExtra = new int[newSize];
                        for (int i = 0; i < extra.length; i++) {
                            newExtra[i] = extra[i];
                        }
                        extra = newExtra;
                    } else {
                        // nothing to do, extra is already the right size
                    }
                }
            }
            length = newLength;
        }
    }

    public int length() {
        return length;
    }

    private int bitInWord(int i) {
        return i & (32 - 1);
    }

    private int wordIndex(int i) {
        return (i >> 5) - 1;
    }

    /**
     * Clears the bit at the specified index.
     * @param i the index of the bit to clear
     */
    public void clear(int i) {
        if (checkIndex(i) < 32) {
            low &= ~(1 << i);
        } else {
            int pos = wordIndex(i);
            int index = bitInWord(i);
            extra[pos] &= ~(1 << index);
        }
    }

    /**
     * Sets all the bits in this bitmap.
     */
    public void setAll() {
        low = -1;
        if (extra != null) {
            for (int i = 0; i < extra.length; i++) {
                extra[i] = -1;
            }
        }
    }

    /**
     * Clears all the bits in this bitmap.
     */
    public void clearAll() {
        low = 0;
        if (extra != null) {
            for (int i = 0; i < extra.length; i++) {
                extra[i] = 0;
            }
        }
    }

    /**
     * Gets the value of the bit at the specified index.
     *
     * @param i
     *            the index of the bit to get
     * @return <code>true</code> if the bit at the specified position is <code>1</code>
     */
    public boolean get(int i) {
        if (checkIndex(i) < 32) {
            return ((low >> i) & 1) != 0;
        }
        int pos = wordIndex(i);
        int index = bitInWord(i);
        int bits = extra[pos];
        return ((bits >> index) & 1) != 0;
    }

    /**
     * Gets the value of the bit at the specified index, returning {@code false} if the
     * bitmap does not cover the specified index.
     *
     * @param i the index of the bit to get
     * @return <code>true</code> if the bit at the specified position is <code>1</code>
     */
    public boolean getDefault(int i) {
        if (i < 0 || i >= length) {
            return false;
        }
        if (i < 32) {
            return ((low >> i) & 1) != 0;
        }
        int pos = wordIndex(i);
        int index = bitInWord(i);
        int bits = extra[pos];
        return ((bits >> index) & 1) != 0;
    }

    /**
     * Performs the union operation on this bitmap with the specified bitmap. That is, all bits set in either of the two
     * bitmaps will be set in this bitmap following this operation.
     *
     * @param other
     *            the other bitmap for the union operation
     */
    public void setUnion(BitMap other) {
        low |= other.low;
        if (extra != null && other.extra != null) {
            for (int i = 0; i < extra.length && i < other.extra.length; i++) {
                extra[i] |= other.extra[i];
            }
        }
    }

    /**
     * Performs the union operation on this bitmap with the specified bitmap. That is, a bit is set in this
     * bitmap if and only if it is set in both this bitmap and the specified bitmap.
     *
     * @param other the other bitmap for this operation
     * @return {@code true} if any bits were cleared as a result of this operation
     */
    public boolean setIntersect(BitMap other) {
        boolean same = true;
        int intx = low & other.low;
        if (low != intx) {
            same = false;
            low = intx;
        }
        int[] oxtra = other.extra;
        if (extra != null && oxtra != null) {
            for (int i = 0; i < extra.length; i++) {
                int a = extra[i];
                if (i < oxtra.length) {
                    // zero bits out of this map
                    int ax = a & oxtra[i];
                    if (a != ax) {
                        same = false;
                        extra[i] = ax;
                    }
                } else {
                    // this bitmap is larger than the specified bitmap; zero remaining bits
                    if (a != 0) {
                        same = false;
                        extra[i] = 0;
                    }
                }
            }
        }
        return !same;
    }

    /**
     * Gets the size of this bitmap in bits.
     *
     * @return the size of this bitmap
     */
    public int size() {
        return length;
    }

    private int checkIndex(int i) {
        if (i < 0 || i >= length) {
            throw new IndexOutOfBoundsException();
        }
        return i;
    }

    public void setFrom(BitMap other) {
        assert this.length == other.length : "must have same size";

        low = other.low;
        if (extra != null) {
            for (int i = 0; i < extra.length; i++) {
                extra[i] = other.extra[i];
            }
        }
    }

    /**
     * Sets the bits for a given range [start, end] on this bitmap.
     *
     * @param start
     *            the first bit of the range
     * @param end
     *            the last bit of the range
     */
    public void setRange(int start, int end) {
        while (start <= end) {
            set(start++);
        }
    }

    public void setDifference(BitMap other) {
        assert this.length == other.length : "must have same size";

        low &= ~other.low;
        if (extra != null) {
            for (int i = 0; i < extra.length; i++) {
                extra[i] &= ~other.extra[i];
            }
        }
    }

    public boolean isSame(BitMap other) {
        if (this.length != other.length || this.low != other.low) {
            return false;
        }

        if (extra != null) {
            for (int i = 0; i < extra.length; i++) {
                if (extra[i] != other.extra[i]) {
                    return false;
                }
            }
        }

        return true;
    }

    public int getNextOneOffset(int lOffset, int rOffset) {
        assert lOffset <= size() : "BitMap index out of bounds";
        assert rOffset <= size() : "BitMap index out of bounds";
        assert lOffset <= rOffset : "lOffset > rOffset ?";

        if (lOffset == rOffset) {
            return lOffset;
        }
        int index = wordIndex(lOffset);
        int rIndex = wordIndex(rOffset - 1) + 1;
        int resOffset = lOffset;

        // check bits including and to the left_ of offset's position
        int pos = bitInWord(resOffset);
        int res = map(index) >> pos;
        if (res != 0) {
            // find the position of the 1-bit
            for (; (res & 1) == 0; resOffset++) {
                res = res >> 1;
            }
            assert resOffset >= lOffset && resOffset < rOffset : "just checking";
            return Math.min(resOffset, rOffset);
        }
        // skip over all word length 0-bit runs
        for (index++; index < rIndex; index++) {
            res = map(index);
            if (res != 0) {
                // found a 1, return the offset
                for (resOffset = bitIndex(index); (res & 1) == 0; resOffset++) {
                    res = res >> 1;
                }
                assert (res & 1) == 1 : "tautology; see loop condition";
                assert resOffset >= lOffset : "just checking";
                return Math.min(resOffset, rOffset);
            }
        }
        return rOffset;
    }

    private int bitIndex(int index) {
        return (index + 1) << 5;
    }

    private int map(int index) {
        if (index == -1) {
            return low;
        }
        return extra[index];
    }

    @Override
    public String toString() {
        StringBuffer res = new StringBuffer();
        res.append("[");
        for (int i = 0; i < this.length; i++) {
            if (this.get(i)) {
                res.append(i).append(" ");
            }
        }
        res.append("]");
        return res.toString();
    }

    public BitMap copy() {
        BitMap n = new BitMap(32);
        n.low = low;
        if (extra != null) {
            n.extra = Arrays.copyOf(extra, extra.length);
        }
        n.length = length;
        return n;
    }
}
