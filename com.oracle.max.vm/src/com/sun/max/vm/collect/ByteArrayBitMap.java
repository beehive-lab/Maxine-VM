/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.collect;

import com.sun.max.lang.*;

/**
 * A {@link ByteArrayBitMap} instance is a view on a fixed size bit map encoded in a byte array. The underlying byte
 * array may contain more than one bit map. In this case, each bit map occupies a range of the byte array that is disjoint from
 * every other encoded bit map.
 */
public final class ByteArrayBitMap implements Cloneable {

    /**
     * The byte array encoding the bit map.
     */
    private byte[] bytes;

    /**
     * The offset in {@link #bytes} at which this bit map's bits start.
     */
    private int offset;

    /**
     * The number of bytes in {@link #bytes} reserved for this bit map's bits.
     */
    private int size;

    public ByteArrayBitMap(int numberOfBits) {
        size = computeBitMapSize(numberOfBits);
        bytes = new byte[size];
        offset = 0;
    }

    public ByteArrayBitMap(byte[] bytes, int offset, int size) {
        this.bytes = bytes;
        this.offset = offset;
        this.size = size;
    }

    public ByteArrayBitMap(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Returns a hash code value for this bit set.
     *
     * Note that the hash code values change if the set of bits is altered.
     *
     * @return a hash code value for this bit set.
     */
    @Override
    public int hashCode() {
        long h = 1234;
        for (int i = size + offset; --i >= 0;) {
            h ^= bytes[i] * (i + 1);
        }
        return (int) ((h >> 32) ^ h);
    }

    /**
     * Compares this object against a given object. The result is {@code true} if and only if {@code other} is not
     * {@code null} and is a {@code ByteArrayBitMap} object that has exactly the same set of bits set as this bit set.
     *
     * @param other the object to compare with
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ByteArrayBitMap) {
            final ByteArrayBitMap bm = (ByteArrayBitMap) other;
            if (size == bm.size) {
                for (int i = 0; i < size; ++i) {
                    if (bytes[offset + i] != bm.bytes[bm.offset + i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the underlying bytes array storing the bits of this bit map.
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * Gets the index in the underlying bytes array of the first byte used by this bit map.
     */
    public int offset() {
        return offset;
    }

    /**
     * Gets the number of bytes used by this bit map.
     */
    public int size() {
        return size;
    }

    /**
     * Gets the number of bits set in this bit map.
     */
    public int cardinality() {
        int cardinality = 0;
        final int end = offset + size;
        for (int i = offset; i < end; ++i) {
            final byte b = bytes[i];
            if (b != 0) {
                cardinality += Integer.bitCount(b & 0xff);
            }
        }
        return cardinality;
    }

    /**
     * Gets the number of bits that can be encoded in this bit map.
     */
    public int width() {
        return size * Bytes.WIDTH;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setOffset(int offset) {
        assert offset < bytes.length;
        this.offset = offset;
    }

    public void setIndex(int index) {
        setOffset(index * size);
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Adjusts this map to the next logical bit map encoded in the underlying byte array. That is, this bit map's
     * {@linkplain #offset() offset} is updated to {@code this.size() + this.offset()}.
     */
    public void next() {
        offset = offset + size;
    }

    /**
     * Sets a bit in this map.
     *
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0 || bitIndex >= this.size() * 8}
     */
    public void set(int bitIndex) throws IndexOutOfBoundsException {
        set(bytes, offset, size, bitIndex);
    }

    public static void set(byte[] bytes, int offset, int size, int bitIndex) throws IndexOutOfBoundsException {
        final int byteIndex = byteIndexFor(offset, size, bitIndex);
        // set the relevant bit
        final byte bit = (byte) (1 << (bitIndex % Bytes.WIDTH));
        bytes[byteIndex] |= bit;

    }

    // calculate the index of the relevant byte in the map
    private int byteIndexFor(int bitIndex) throws IndexOutOfBoundsException {
        return byteIndexFor(offset, size, bitIndex);
    }

    // calculate the index of the relevant byte in the map
    private static int byteIndexFor(int offset, int size, int bitIndex) throws IndexOutOfBoundsException {
        final int relativeByteIndex = Unsigned.idiv(bitIndex, Bytes.WIDTH);
        if (relativeByteIndex >= size) {
            throw new IndexOutOfBoundsException("Bit index: " + bitIndex + ", Width: " + (size * Bytes.WIDTH));
        }
        return offset + relativeByteIndex;
    }

    /**
     * Clears a bit in this map.
     *
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0 || bitIndex >= this.size() * 8}
     */
    public void clear(int bitIndex) throws IndexOutOfBoundsException {
        final int byteIndex = byteIndexFor(bitIndex);
        // clear the relevant bit
        final byte bit = (byte) (1 << (bitIndex % Bytes.WIDTH));
        bytes[byteIndex] &= ~bit;
    }

    /**
     * Determines if a bit is set in this map.
     *
     * @return true if bit {@code bitIndex} is set, false otherwise
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0 || bitIndex >= this.size() * 8}
     */
    public boolean isSet(int bitIndex) throws IndexOutOfBoundsException {
        return isSet(bytes, offset, size, bitIndex);
    }

    /**
     * Determines if a bit is set in a given bit map.
     *
     * @param bytes a byte array encoding a bit map
     * @param offset the index in {@code bytes} of the first byte used by the bit map
     * @param size the number of bytes used by the bit map
     * @return true if bit {@code bitIndex} is set, false otherwise
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0 || bitIndex >= this.size() * 8}
     */
    public static boolean isSet(byte[] bytes, int offset, int size, int bitIndex) throws IndexOutOfBoundsException {
        final int byteIndex = byteIndexFor(offset, size, bitIndex);
        final byte bit = (byte) (1 << (bitIndex % Bytes.WIDTH));
        return (bytes[byteIndex] & bit) != 0;
    }

    /**
     * Returns the index of the first set bit that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned.
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
     * @return the index of the next set bit
     * @throws IndexOutOfBoundsException if the specified index is negative.
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        int byteIndex = offset + Unsigned.idiv(fromIndex, Bytes.WIDTH);
        final int end = offset + size;
        if (byteIndex >= end) {
            return -1;
        }

        final int fromBitIndex = fromIndex % Bytes.WIDTH;
        byte bite = (byte) (bytes[byteIndex] & (0xFF << fromBitIndex));

        while (true) {
            if (bite != 0) {
                final int result = ((byteIndex - offset) * Bytes.WIDTH) + Bytes.numberOfTrailingZeros(bite);
                return result;
            }
            if (++byteIndex == end) {
                return -1;
            }
            bite = bytes[byteIndex];
        }
    }

    @Override
    public String toString() {
        final int numBits = size * Bytes.WIDTH;
        final StringBuilder buffer = new StringBuilder(8 * numBits + 2);
        String separator = "";
        buffer.append('[');

        for (int i = 0; i < numBits; i++) {
            if (isSet(i)) {
                buffer.append(separator);
                separator = ", ";
                buffer.append(i);
            }
        }

        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Computes the minimum number of bytes required to encode a bit map with a given number of bits.
     */
    public static int computeBitMapSize(int numberOfBits) {
        return Unsigned.idiv(Ints.roundUnsignedUpByPowerOfTwo(numberOfBits, Bytes.WIDTH), Bytes.WIDTH);
    }
}
