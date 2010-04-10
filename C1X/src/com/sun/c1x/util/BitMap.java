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
 * Implements a bitmap that stores a single bit for a range of integers (0-n).
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class BitMap {

    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    public static final int DEFAULT_LENGTH = BITS_PER_WORD;

    public static int roundUpLength(int length) {
        return ((length + (BITS_PER_WORD - 1)) >> ADDRESS_BITS_PER_WORD) << ADDRESS_BITS_PER_WORD;
    }

    private int length;
    private long low;
    private long[] extra;

    /**
     * Constructs a new bit map with the {@linkplain #DEFAULT_LENGTH default length}.
     */
    public BitMap() {
        this(DEFAULT_LENGTH);
    }

    /**
     * Construct a new bit map with the specified length.
     * @param length the length of the bitmap
     */
    public BitMap(int length) {
        assert length >= 0;
        this.length = length;
        if (length > BITS_PER_WORD) {
            extra = new long[length >> ADDRESS_BITS_PER_WORD];
        }
    }

    /**
     * Set the bit at the specified index.
     * @param i the index of the bit to set
     */
    public void set(int i) {
        if (checkIndex(i) < BITS_PER_WORD) {
            low |= 1L << i;
        } else {
            int pos = wordIndex(i);
            int index = bitInWord(i);
            extra[pos] |= 1L << index;
        }
    }

    /**
     * Grows this bitmap to a new length, appending necessary zero bits.
     * @param newLength the new length of the bitmap
     */
    public void grow(int newLength) {
        if (newLength > length) {
            // grow this bitmap to the new length
            int newSize = newLength >> ADDRESS_BITS_PER_WORD;
            if (newLength > 0) {
                if (extra == null) {
                    // extra just needs to be allocated now
                    extra = new long[newSize];
                } else {
                    if (extra.length < newSize) {
                        // extra needs to be copied
                        long[] newExtra = new long[newSize];
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
        return i & BIT_INDEX_MASK;
    }

    private int wordIndex(int i) {
        return (i >> ADDRESS_BITS_PER_WORD) - 1;
    }

    /**
     * Clears the bit at the specified index.
     * @param i the index of the bit to clear
     */
    public void clear(int i) {
        if (checkIndex(i) < BITS_PER_WORD) {
            low &= ~(1L << i);
        } else {
            int pos = wordIndex(i);
            int index = bitInWord(i);
            extra[pos] &= ~(1L << index);
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
     * @param i the index of the bit to get
     * @return {@code true} if the bit at the specified position is {@code 1}
     */
    public boolean get(int i) {
        if (checkIndex(i) < BITS_PER_WORD) {
            return ((low >> i) & 1) != 0;
        }
        int pos = wordIndex(i);
        int index = bitInWord(i);
        long bits = extra[pos];
        return ((bits >> index) & 1) != 0;
    }

    /**
     * Gets the value of the bit at the specified index, returning {@code false} if the
     * bitmap does not cover the specified index.
     *
     * @param i the index of the bit to get
     * @return {@code true} if the bit at the specified position is {@code 1}
     */
    public boolean getDefault(int i) {
        if (i < 0 || i >= length) {
            return false;
        }
        if (i < BITS_PER_WORD) {
            return ((low >> i) & 1) != 0;
        }
        int pos = wordIndex(i);
        int index = bitInWord(i);
        long bits = extra[pos];
        return ((bits >> index) & 1) != 0;
    }

    /**
     * Performs the union operation on this bitmap with the specified bitmap. That is, all bits set in either of the two
     * bitmaps will be set in this bitmap following this operation.
     *
     * @param other the other bitmap for the union operation
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
        long intx = low & other.low;
        if (low != intx) {
            same = false;
            low = intx;
        }
        long[] oxtra = other.extra;
        if (extra != null && oxtra != null) {
            for (int i = 0; i < extra.length; i++) {
                long a = extra[i];
                if (i < oxtra.length) {
                    // zero bits out of this map
                    long ax = a & oxtra[i];
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

    /**
     * Returns the index of the first set bit that occurs on or after a specified start index.
     * If no such bit exists then -1 is returned.
     * <p>
     * To iterate over the set bits in a {@code BitMap}, use the following loop:
     *
     * <pre>
     * for (int i = bitMap.nextSetBit(0); i &gt;= 0; i = bitMap.nextSetBit(i + 1)) {
     *     // operate on index i here
     * }
     * </pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the lowest set bit between {@code [fromIndex .. size())} or -1 if there is no set bit in this range
     * @throws IndexOutOfBoundsException if the specified index is negative.
     */
    public int nextSetBit(int fromIndex) {
        return nextSetBit(fromIndex, size());
    }

    /**
     * Returns the index of the first set bit that occurs on or after a specified start index
     * and before a specified end index. If no such bit exists then -1 is returned.
     * <p>
     * To iterate over the set bits in a {@code BitMap}, use the following loop:
     *
     * <pre>
     * for (int i = bitMap.nextSetBit(0, bitMap.size()); i &gt;= 0; i = bitMap.nextSetBit(i + 1, bitMap.size())) {
     *     // operate on index i here
     * }
     * </pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @param toIndex the index at which to stop checking (exclusive)
     * @return the index of the lowest set bit between {@code [fromIndex .. toIndex)} or -1 if there is no set bit in this range
     * @throws IndexOutOfBoundsException if the specified index is negative.
     */
    public int nextSetBit(int fromIndex, int toIndex) {
        assert fromIndex <= size() : "BitMap index out of bounds";
        assert toIndex <= size() : "BitMap index out of bounds";
        assert fromIndex <= toIndex : "fromIndex > toIndex";

        if (fromIndex == toIndex) {
            return -1;
        }
        int fromWordIndex = wordIndex(fromIndex);
        int toWordIndex = wordIndex(toIndex - 1) + 1;
        int resultIndex = fromIndex;

        // check bits including and to the left_ of offset's position
        int pos = bitInWord(resultIndex);
        long res = map(fromWordIndex) >> pos;
        if (res != 0) {
            resultIndex += Long.numberOfTrailingZeros(res);
            assert resultIndex >= fromIndex && resultIndex < toIndex : "just checking";
            if (resultIndex < toIndex) {
                return resultIndex;
            }
            return -1;
        }
        // skip over all word length 0-bit runs
        for (fromWordIndex++; fromWordIndex < toWordIndex; fromWordIndex++) {
            res = map(fromWordIndex);
            if (res != 0) {
                // found a 1, return the offset
                resultIndex = bitIndex(fromWordIndex) + Long.numberOfTrailingZeros(res);
                assert resultIndex >= fromIndex : "just checking";
                if (resultIndex < toIndex) {
                    return resultIndex;
                }
                return -1;
            }
        }
        return -1;
    }

    private int bitIndex(int index) {
        return (index + 1) << ADDRESS_BITS_PER_WORD;
    }

    private long map(int index) {
        if (index == -1) {
            return low;
        }
        return extra[index];
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("[");
        for (int i = 0; i < this.length; i++) {
            if (this.get(i)) {
                if (res.length() != 1) {
                    res.append(' ');
                }
                res.append(i);
            }
        }
        res.append("]");
        return res.toString();
    }

    public BitMap copy() {
        BitMap n = new BitMap(BITS_PER_WORD);
        n.low = low;
        if (extra != null) {
            n.extra = Arrays.copyOf(extra, extra.length);
        }
        n.length = length;
        return n;
    }

    public boolean[] toArray() {
        final boolean[] result = new boolean[this.length];
        for (int i = 0; i < length; i++) {
            result[i] = get(i);
        }
        return result;
    }
}
