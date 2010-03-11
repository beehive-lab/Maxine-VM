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
package com.sun.max.vm.heap.gcx;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * A three color mark bitmap. It encodes three colors (white, grey, black) using two consecutive bits.
 * Every bit maps to a fixed chunk of heap such that every object's first words coincide with one fixed chunk.
 * Most objects have a size larger than the size of a single chunk covered by one bit. Those that don't (i.e., very small objects) are
 * segregated. Maxine currently aligns objects on a 8-byte word boundary, uses 8-bytes words, and have a two words header.
 *
 * The following choices are considered:
 * - Each bit correspond to 1 Word of the heap; Every object is thus guaranteed two-bits; objects only need to be 8-byte aligned in the heap.
 * - Each bit correspond to 2 Words of the heap; Every object larger that 3 words occupies 2 chunks. In order to prevent chunk to be shared by two objects,
 *   these needs to be 2 words aligned, resulting in potential wastage of heap space. Further, the smallest object (i.e., 1 chunk object) can only be allocated 1 bit.
 *   Since such objects are generally rare they can be segregated and associated with an area covered by a two-color bitmap (black / white).
 *   Since these objects don't have reference (except their hub), they can be marked black immediately.
 *
 *   Which solution is best depends on the amount of space wasted by the 2-words alignment requirement, compared to bitmap space saved. A larger grain also
 *   means less time to scan the bitmap. We leave this choice to the heap implementation / collector, which is responsible for aligning object and dealing with small object.
 *   For simplicity, we begin here with the first alternative (1 bit per word).
 *
 *
 * @author Laurent Daynes
 */
public class ThreeColorMarkBitmap {
    // The color encoding is chosen to optimize the mark bitmaps.
    // The tracing algorithm primarily search for grey objects, 64-bits at a time.
    // By encoding black as 10 and grey as 11, a fast and common way to
    // determine if a 64-bit words holds any grey objects is to compare w & (w <<1).
    // A bit pattern with only black and white objects will result in a 0, whereas a
    // bit pattern with at least one grey object will yield a non zero result, with the
    // leading bit indicating the position of a first grey object.

    /**
     * 2-bit mark for white object.
     */
    static final byte WHITE = 0;
    /**
     * 2-bit mark for black object.
     */
    static final byte BLACK = 1;
    /**
     * 2-bit mark for grey objects.
     */
    static final byte  GREY =  3;
    /**
     * Invalid 2-bit mark pattern.
     */
    static final byte INVALID = 2;

    /**
     * Number of bytes covered by each bit of the bitmaps.
     * Must be a power of 2 of a number of words.
     */
    final int wordsCoveredPerBit;

    RuntimeMemoryRegion coveredArea;

    final RuntimeMemoryRegion colorMap;

    static final  int LOG2_BYTES_PER_BITS = Word.widthValue().log2numberOfBits;

    /**
     * Return the size in bytes required for a ThreeColorMarkBitmap to cover a contiguous heap area of the specified size.
     *
     * @param coveredAreaSize
     * @return the size a three color mark bitmaps should have to cover the specified area size.
     */
    Size bitmapSize(Size coveredAreaSize) {
        return coveredAreaSize.dividedBy(wordsCoveredPerBit * Word.widthValue().numberOfBits);
    }

    /**
     *
     */
    public ThreeColorMarkBitmap(int wordsCoveredPerBit) {
        this.wordsCoveredPerBit = wordsCoveredPerBit;
        colorMap = new RuntimeMemoryRegion("Mark Bitmap");
    }

    /**
     * Initialize a three color mark bitmap for the covered area.
     * The mark-bitmap is generated at VM startup.
     *
     * @param bitmapStorage
     * @param coveredArea
     */
    public void initialize(RuntimeMemoryRegion coveredArea, Address bitmapStorage, Size bitmapSize) {
        if (MaxineVM.isDebug()) {
            FatalError.check(bitmapSize.toLong() >=  bitmapSize(coveredArea.size()).toLong(), "Mark bitmap too small to cover heap");
        }
        this.coveredArea = coveredArea;
        this.colorMap.setStart(bitmapStorage);
        this.colorMap.setSize(bitmapSize);
    }
}
