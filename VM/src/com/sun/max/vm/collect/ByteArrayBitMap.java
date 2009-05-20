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
package com.sun.max.vm.collect;

import com.sun.max.lang.*;

/**
 * A {@link ByteArrayBitMap} instance is a view on a fixed size bit map encoded in a byte array. The underlying byte
 * array may contain more than one bit map. In this case, each bit map occupies a range of the byte array that is disjoint from
 * every other encoded bit map.
 *
 * @author Doug Simon
 */
public final class ByteArrayBitMap implements Cloneable {

    /**
     * The byte array encoding the bit map.
     */
    private byte[] _bytes;

    /**
     * The offset in {@link _bytes} at which this bit map's bits start.
     */
    private int _offset;

    /**
     * The number of bytes in {@link _bytes} reserved for this bit map's bits start.
     */
    private int _size;

    public ByteArrayBitMap(int numberOfBits) {
        _size = computeBitMapSize(numberOfBits);
        _bytes = new byte[_size];
        _offset = 0;
    }

    public ByteArrayBitMap(byte[] bytes, int offset, int size) {
        _bytes = bytes;
        _offset = offset;
        _size = size;
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
        for (int i = _size + _offset; --i >= 0;) {
            h ^= _bytes[i] * (i + 1);
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
            if (_size == bm._size) {
                for (int i = 0; i < _size; ++i) {
                    if (_bytes[_offset + i] != bm._bytes[bm._offset + i]) {
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
        return _bytes;
    }

    /**
     * Gets the index in the underlying bytes array of the first byte used by this bit map.
     */
    public int offset() {
        return _offset;
    }

    /**
     * Gets the number of bytes used by this bit map.
     */
    public int size() {
        return _size;
    }

    /**
     * Gets the number of bits set in this bit map.
     */
    public int cardinality() {
        int cardinality = 0;
        final int end = _offset + _size;
        for (int i = _offset; i < end; ++i) {
            final byte b = _bytes[i];
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
        return _size * Bytes.WIDTH;
    }

    public void setBytes(byte[] bytes) {
        _bytes = bytes;
    }

    public void setOffset(int offset) {
        assert offset < _bytes.length;
        _offset = offset;
    }

    public void setSize(int size) {
        _size = size;
    }

    /**
     * Adjusts this map to the next logical bit map encoded in the underlying byte array. That is, this bit map's
     * {@linkplain #offset() offset} is updated to {@code this.size() + this.offset()}.
     */
    public void next() {
        final int newOffset = _offset + _size;
        _offset = newOffset;
    }

    /**
     * Sets a bit in this map.
     *
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0 || bitIndex >= this.size() * 8}
     */
    public void set(int bitIndex) throws IndexOutOfBoundsException {
        set(_bytes, _offset, _size, bitIndex);
    }

    public static void set(byte[] bytes, int offset, int size, int bitIndex) throws IndexOutOfBoundsException {
        final int byteIndex = byteIndexFor(offset, size, bitIndex);
        // set the relevant bit
        final byte bit = (byte) (1 << (bitIndex % Bytes.WIDTH));
        bytes[byteIndex] |= bit;

    }

    // calculate the index of the relevant byte in the map
    private int byteIndexFor(int bitIndex) throws IndexOutOfBoundsException {
        return byteIndexFor(_offset, _size, bitIndex);
    }

    // calculate the index of the relevant byte in the map
    private static int byteIndexFor(int offset, int size, int bitIndex) throws IndexOutOfBoundsException {
        final int relativeByteIndex = Unsigned.idiv(bitIndex, Bytes.WIDTH);
        if (relativeByteIndex >= size) {
            throw new IndexOutOfBoundsException();
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
        _bytes[byteIndex] &= ~bit;
    }

    /**
     * Determines if a bit is set in this map.
     *
     * @return true if bit {@code bitIndex} is set, false otherwise
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0 || bitIndex >= this.size() * 8}
     */
    public boolean isSet(int bitIndex) throws IndexOutOfBoundsException {
        return isSet(_bytes, _offset, _size, bitIndex);
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

        int byteIndex = _offset + Unsigned.idiv(fromIndex, Bytes.WIDTH);
        final int end = _offset + _size;
        if (byteIndex >= end) {
            return -1;
        }

        final int fromBitIndex = fromIndex % Bytes.WIDTH;
        byte bite = (byte) (_bytes[byteIndex] & (0xFF << fromBitIndex));

        while (true) {
            if (bite != 0) {
                final int result = ((byteIndex - _offset) * Bytes.WIDTH) + Bytes.numberOfTrailingZeros(bite);
                return result;
            }
            if (++byteIndex == end) {
                return -1;
            }
            bite = _bytes[byteIndex];
        }
    }

    @Override
    public String toString() {
        final int numBits = _size * Bytes.WIDTH;
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
        return Unsigned.idiv(Ints.roundUp(numberOfBits, Bytes.WIDTH), Bytes.WIDTH);
    }
}
