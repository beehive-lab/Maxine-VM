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

    public static class Color {
        public final int id;
        public final String name;
        public Color(int id, String name) {
            this.id = id;
            this.name = name;
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
     * @param heapAddress an address in the heap area covered by the mark bitmap
     * @return a bit index
     */
    int getBitIndexOf(Address heapAddress);

    /**
     * Index to the word holding the first bit in the mark bitmap encoding the color corresponding to the specified heap address.
     * @param heapAddress an address in the heap area covered by the mark bitmap
     * @return an address to a word of the mark bitmap
     */
    int getBitmapWordIndex(Address heapAddress);

    /**
     * Address of the word holding the first bit in the mark bitmap encoding the color corresponding to the specified heap address.
     * @param heapAddress an address in the heap area covered by the mark bitmap
     * @return an address to a word of the mark bitmap
     */
    Address bitmapWord(Address heapAddress);

    /**
     * Address in the heap corresponding to a bit index of the mark bitmap.
     * @param bitIndex a bit index
     * @return an address in the heap area covered by the mark bitmap
     */
    Address heapAddress(int bitIndex);

    /**
     * Address of the word containing the first bit of the mark at the specified bit index.
     * @param bitIndex a bit index
     * @return address to bitmap word.
     */
    Address bitmapWord(int bitIndex);

    /**
     * Color of the mark at the specified bit index.
     * @param bitIndex a bit index
     * @return color
     */
    Color getColor(int bitIndex);

    /**
     * Color of the mark corresponding to the specified the heap address.
     * @param heapAddress
     */
    Color getColor(Address heapAddress);

    /**
     * Colors that the mark bitmap can encode. Typical implementation only encode 2 colors (white and black). Exotic
     * implementation may implement three or four color per objects. This let the heap scheme implementation specify what
     * color it supports.
     * @return an array enumerating all the color supported by the heap scheme.
     */
    Color [] colors();
}
