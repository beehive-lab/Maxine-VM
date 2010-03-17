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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A three color mark bitmap. It encodes three colors (white, grey, black) using two consecutive bits. Every bit maps to
 * a fixed chunk of heap such that every object's first words coincide with one fixed chunk. Almost all objects have a
 * size larger than the size of a single chunk covered by one bit. Those that don't (called tiny objects) are segregated
 * or padded (i.e., the heap allocate them the required space cover 2 bits of the mark bitmap). Maxine currently aligns
 * objects on a 8-byte word boundary, uses 8-bytes words, and uses a two-words header.
 *
 * The following choices are considered: - Each bit correspond to 1 Word of the heap; Every object is thus guaranteed
 * two-bits;
 *
 * - Each bit correspond to 2 Words of the heap; Every object larger that 3 words occupies 2 chunks. With this design,
 * the smallest objects can only be allocated 1 bit. Since such objects are generally rare they can be treated
 * specially: e.g., padded to be associated with two bits, or segregated to be allocated in an area covered by a one bit
 * bitmap. Padding is simpler as it allows a unified bitmaps. Maxine's current 8-byte alignment raises another problem
 * with this approach: a chunk can shared by two objects. This complicates finding the head of an object. The solution
 * is to require objects to be chunk-aligned (i.e., 16-byte aligned) potentially wasting heap space.
 *
 * Which solution is best depends on the amount of space wasted by the 2-words alignment requirement, compared to bitmap
 * space saved. A larger grain also means less time to scan the bitmap. We leave this choice to the heap implementation
 * / collector, which is responsible for aligning object and dealing with small object. For simplicity, we begin here
 * with the first alternative (1 bit per word).
 *
 * Finally, note that in both cases, the first bit of a color may be either an odd or an even bit, and a color may span
 * two bitmaps words. This complicates color search/update operation. A heap allocator may arrange for guaranteeing that
 * an object marks never span a bitmap word by padding a dead object before (Dead objects are special instance of object
 * whose size is strictly 2 words, regardless of other rules for dealing with tiny objects).
 *
 * This class enables both designs, and provides generic bitmap manipulation that understands color coding and
 * color-oriented operations (i.e., searching grey or black mark, etc.). It provides fast and slow variant of
 * operations, wherein the fast variant assumes that a color never span a bitmap word. The GC is responsible for
 * guaranteeing this property if it uses the fast variant.
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

    // The encoding below follows the following convention:
    // Heap position bitmap word bit index in bitmap word.
    // Word 0 0 0
    // Word 1 0 1
    // Word 31 0 31
    // Word 32 1 0
    // Word 33 1 1
    // ...
    // This means that the leading bit of a color is always at a lower position in a bitmap word.
    // Iterating over a word of the bitmap goes from low order bit to high order bit.
    // A mark in the color map is always identified by the bit index of the leading bit.

    /**
     * 2-bit mark for white object (00).
     */
    static final long WHITE = 0L;
    /**
     * 2-bit mark for black object (01).
     */
    static final long BLACK = 1L;
    /**
     * 2-bit mark for grey objects (11).
     */
    static final long GREY = 3L;
    /**
     * Invalid 2-bit mark pattern (10).
     */
    static final long INVALID = 2L;

    static final long COLOR_MASK = 3L;

    static final int bitIndexInWordMask = ~(Word.widthValue().numberOfBits - 1);

    /**
     * Return the index within a word of the bit index.
     *
     * @param bitIndex
     * @return
     */
    static final int bitIndexInWord(int bitIndex) {
        return bitIndex & bitIndexInWordMask;
    }

    /**
     * Number of bytes covered by each bit of the bitmaps. Must be a power of 2 of a number of words.
     */
    final int wordsCoveredPerBit;

    /**
     * Log 2 of the number of bytes covered by a bit. Used to compute bit index in the color map from address.
     */
    final int log2BytesCoveredPerBit;
    /**
     * Log 2 to get bitmap word index from an address.
     */
    final int log2BitmapWord;

    /**
     * Heap area covered by the mark bitmap.
     */
    RuntimeMemoryRegion coveredArea;

    /**
     * Base of the bitmap biased with offset to the first word's bit. For fast-computing of heap word's color index in
     * the color map.
     */
    Address biasedBitmapBase;

    /**
     * Bias to the first word of the heap.
     */
    int baseBias;
    /**
     * Memory where the color map is stored.
     */
    final RuntimeMemoryRegion colorMap;

    /**
     * Shortcut to colorMap.start() for fast bitmap operation.
     */
    private Address base;

    /**
     * Return the size in bytes required for a ThreeColorMarkBitmap to cover a contiguous heap area of the specified
     * size.
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
    public ThreeColorMarkBitmap(int log2WordsCoveredPerBit) {
        wordsCoveredPerBit = 1 << log2WordsCoveredPerBit;
        log2BytesCoveredPerBit = Word.widthValue().log2numberOfBytes + log2WordsCoveredPerBit;
        log2BitmapWord = log2BytesCoveredPerBit + Word.widthValue().log2numberOfBits;
        colorMap = new RuntimeMemoryRegion("Mark Bitmap");
    }

    /**
     * Initialize a three color mark bitmap for the covered area. The mark-bitmap is generated at VM startup.
     *
     * @param bitmapStorage
     * @param coveredArea
     */
    public void initialize(RuntimeMemoryRegion coveredArea, Address bitmapStorage, Size bitmapSize) {
        if (MaxineVM.isDebug()) {
            FatalError.check(bitmapSize.toLong() >= bitmapSize(coveredArea.size()).toLong(), "Mark bitmap too small to cover heap");
        }
        this.coveredArea = coveredArea;
        this.colorMap.setStart(bitmapStorage);
        this.colorMap.setSize(bitmapSize);
        this.baseBias = coveredArea.start().unsignedShiftedRight(log2BitmapWord).toInt();
        biasedBitmapBase = colorMap.start().minus(baseBias);
        clear();
    }

    // Address to bitmap word / bit index operations.

    // Pointer to the word in the bitmap containing the bit for the specified address.
    @INLINE
    final Pointer bitmapWordPointerOf(Address addr) {
        return biasedBitmapBase.asPointer().plus(addr.unsignedShiftedRight(log2BitmapWord));
    }

    @INLINE
    final Word bitmapWordOf(Address address) {
        return bitmapWordPointerOf(address).getWord(0);
    }

    // Pointer to bitmap word containing bit at specified bit index.
    @INLINE
    final Word bitmapWordAt(int bitIndex) {
        return base.asPointer().getWord(bitIndex >> Word.widthValue().log2numberOfBits);
    }

    @INLINE
    final Pointer bitmapWordPointerAt(int bitIndex) {
        return base.asPointer().plus(bitIndex >> Word.widthValue().log2numberOfBits);
    }

    // Bit index in the bitmap for the address.
    @INLINE
    final int bitIndexOf(Address address) {
        return address.unsignedShiftedRight(log2BytesCoveredPerBit).minus(baseBias).toInt();
    }

    @INLINE
    final Address addressOf(int bitIndex) {
        return coveredArea.start().plus(bitIndex << log2BytesCoveredPerBit);
    }


    @INLINE
    final void markGrey_(int bitIndex) {
        FatalError.check(bitIndexInWord(bitIndex) != Word.width() - 1, "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        pointer.setLong(pointer.getLong() | (GREY << bitIndexInWord(bitIndex)));
    }


    @INLINE
    final void markBlackFromGrey_(int bitIndex) {
        FatalError.check(bitIndexInWord(bitIndex) != Word.width() - 1, "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        // Clear the second bit of the color.
        pointer.setLong(pointer.getLong() & (1 << bitIndexInWord(bitIndex + 1)));
    }
    @INLINE
    final void markBlackFromWhite_(int bitIndex) {
        FatalError.check(bitIndexInWord(bitIndex) != Word.width() - 1, "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        pointer.setLong(pointer.getLong() | (BLACK << bitIndexInWord(bitIndex)));
    }
    /**
     * Set a black mark not assuming previous mark in place.
     * @param bitIndex the index of the color in the color map.
     */
    @INLINE
    final void markBlack_(int bitIndex) {
        FatalError.check(bitIndexInWord(bitIndex) != Word.width() - 1, "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        long bitmapWord = pointer.getLong();
        final int bitIndexInWord = bitIndexInWord(bitIndex);
        // Clear grey bit and set black one.
        final long blackBit = 1 << bitIndexInWord;
        bitmapWord |= blackBit;
        bitmapWord &= ~(blackBit << 1);
        pointer.setLong(bitmapWord);
    }

    @INLINE
    final boolean isColor_(int bitIndex, long color) {
        FatalError.check(bitIndexInWord(bitIndex) != Word.width() - 1, "Color must not cross word boundary.");
        final Word bitmapWord = bitmapWordAt(bitIndex);
        return ((bitmapWord.asPointer().toLong() >> bitIndexInWord(bitIndex)) & COLOR_MASK) == color;
    }

    @INLINE
    final boolean isBlack_(int bitIndex) {
        return isColor_(bitIndex, BLACK);
    }

    @INLINE
    final boolean isGrey_(int bitIndex) {
        return isColor_(bitIndex, GREY);
    }

    @INLINE
    final boolean isBlack_(Object o) {
        return isBlack_(bitIndexOf(Reference.fromJava(o).toOrigin()));
    }

    @INLINE
    final boolean isGrey_(Object o) {
        return isGrey_(bitIndexOf(Reference.fromJava(o).toOrigin()));
    }

    /**
     * Clear the color map, i.e., turn all bit to white.
     */
    void clear() {

    }

}
