/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import com.sun.max.unsafe.*;

/**
 * An interface for Heap Scheme implementations that uses a mark bitmap and can provide the inspector with information for
 * debugging mark bitmap state.
 */
public interface MaxMarkBitmap extends MaxEntity<MaxMarkBitmap> {

    public enum MarkColor {
        MARK_WHITE(0, "White"),
        MARK_BLACK(1, "Black"),
        MARK_GRAY(2, "Gray"),
        MARK_INVALID(3, "Invalid"),
        MARK_UNAVAILABLE(4, "<?>");

        public final int id;
        public final String name;
        private MarkColor(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Describes the memory region covered by the bitmap.
     */
    MaxMemoryRegion coveredMemoryRegion();

    /**
     * Indicate whether the heap address is covered by the mark bitmap (i.e., if a mark in the mark bitmap is associated with this address).
     * @param heapAddress
     * @return true if the address is covered
     */
    boolean isCovered(Address heapAddress);

    /**
     * Index to the first bit in the mark bitmap encoding the color corresponding to the specified heap address.
     *
     * @param heapAddress an address in the heap area covered by the mark bitmap
     * @return a bit index in the map, {@code -1} if the address is not covered by the map.
     */
    int getBitIndexOf(Address heapAddress);

    /**
     * Address in the heap corresponding to a bit index of the mark bitmap.
     * @param bitIndex a bit index
     * @return an address in the heap area covered by the mark bitmap
     */
    Address heapAddress(int bitIndex);

    /**
     * Gets the (word-based) index of the bitmap word that contains a specific bit index.
     *
     * @param bitIndex index of a bit in the map
     * @return index of a word in the map
     */
    int bitmapWordIndex(int bitIndex);

    /**
     * Address of the word containing the first bit of the mark at the specified bit index.
     * @param bitIndex a bit index
     * @return address to bitmap word.
     */
    Address bitmapWordAddress(int bitIndex);

    /**
     * Reads from VM memory the word in the map that includes the specified bit index.
     *
     * @param bitIndex a bit index
     * @return current contents of the bitmap word for the index
     */
    long readBitmapWord(int bitIndex);

    /**
     * The position of the bit identified by a bit index within the bitmap word holding that bit.
     *
     * @param bitIndex
     * @return a bit position within a word.
     */
    int getBitIndexInWord(int bitIndex);

    /**
     * Return a boolean indicating whether a bit is set in the color map (regardless of color logic).
     * @param bitIndex a bit index.
     * @return a boolean indicating whether the bit at the specified index is set.
     */
    boolean isBitSet(int bitIndex);

    /**
     * Sets a bit in the color map (regardless of color logic).
     * @param bitIndex a bit index.
     */
    void setBit(int bitIndex);

    /**
     * Gets the color of the marking at the covered address, if there is an object at the address covered by the
     * specified bit in the map; {@code null} if there is no object at the covered address.
     *
     * @param bitIndex a bit index
     * @return color color of the mark covering an address; {@code null} if no object at address
     */
    MarkColor getMarkColor(int bitIndex);

    /**
     * Gets the color of the marking at the address, if there is an object at the address covered by the specified bit
     * in the map; {@code null} if there is no object at the address.
     *
     * @param an address, presumed to be in the heap covered by the bitmap
     * @return color color of the mark covering the address; {@code null} if no object at address
     */
    MarkColor getMarkColor(Address heapAddress);

    /**
     * Scans forward in the bitmap, locating the closest bit <em>after</em> a specified starting location that is set.
     *
     * @param startBitIndex Where the scan should start
     * @return the index of the closest set bit after the starting index, -1 if none.
     */
    int nextSetBitAfter(int startBitIndex);

    /**
     * Scans backward in the bitmap, locating the closest bit <em>before</em> a specified starting location that is set.
     *
     * @param startBitIndex Where the scan should start
     * @return the index of the closest set bit before the starting index, -1 if none.
     */
    int previousSetBitBefore(int startBitIndex);

    /**
     * Scans forward in the bitmap, locating the closest bit <em>before</em> a specified starting location that begins a
     * mark of the specified color at an object location.
     *
     * @param startBitIndex Where the scan should start
     * @return the index of the closest mark before the starting index, -1 if none.
     */
    int nextMarkAfter(int startBitIndex, MarkColor color);

    /**
     * Scans forward in the bitmap, locating the closest bit <em>after</em> a specified starting location that begins a
     * mark of the specified color at an object location.
     *
     * @param startBitIndex Where the scan should start
     * @return the index of the closest mark after the starting index, -1 if none.
     */
    int previousMarkBefore(int startBitIndex, MarkColor color);

}
