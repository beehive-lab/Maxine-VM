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
package com.sun.max.vm.layout;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

public final class Layout {

    private Layout() {
    }

    @UNSAFE
    @FOLD
    public static LayoutScheme layoutScheme() {
        return vmConfig().layoutScheme();
    }

    @UNSAFE
    @FOLD
    public static GeneralLayout generalLayout() {
        return layoutScheme().generalLayout;
    }

    /**
     * A descriptor for a word-sized slot in an object's header. All that the descriptor
     * encapsulates is the name of a slot in a header. The offset of the
     * slots is {@linkplain GeneralLayout#getOffsetFromOrigin(HeaderField) determined}
     * by the configured layout.
     *
     * @author Doug Simon
     */
    public static class HeaderField {

        /**
         * The header word from which the hub of an object can be found.
         */
        public static final HeaderField HUB = new HeaderField("HUB");

        /**
         * The header word in which the monitor and hash code details of an object are encoded.
         */
        public static final HeaderField MISC = new HeaderField("MISC");

        /**
         * The header word in which the length of an array object is encoded.
         */
        public static final HeaderField LENGTH = new HeaderField("LENGTH");

        /**
         * The name of this header field.
         */
        public final String name;

        public HeaderField(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Category {
        TUPLE, ARRAY, HYBRID;
    }

    @UNSAFE
    @FOLD
    public static TupleLayout tupleLayout() {
        return layoutScheme().tupleLayout;
    }

    @UNSAFE
    @FOLD
    public static HybridLayout hybridLayout() {
        return layoutScheme().hybridLayout;
    }

    @UNSAFE
    @FOLD
    public static ArrayHeaderLayout arrayHeaderLayout() {
        return layoutScheme().arrayHeaderLayout;
    }

    @INLINE
    public static Pointer cellToOrigin(Pointer cell) {
        return generalLayout().cellToOrigin(cell);
    }

    @INLINE
    public static Pointer tupleCellToOrigin(Pointer cell) {
        return tupleLayout().cellToOrigin(cell);
    }

    @INLINE
    public static Pointer hybridCellToOrigin(Pointer cell) {
        return hybridLayout().cellToOrigin(cell);
    }

    @INLINE
    public static Pointer arrayCellToOrigin(Pointer cell) {
        return arrayHeaderLayout().cellToOrigin(cell);
    }

    @INLINE
    public static Pointer originToCell(Pointer origin) {
        return generalLayout().originToCell(origin);
    }

    @INLINE
    public static Pointer tupleOriginToCell(Pointer origin) {
        return tupleLayout().originToCell(origin);
    }

    @INLINE
    public static Pointer hybridOriginToCell(Pointer origin) {
        return hybridLayout().originToCell(origin);
    }

    @INLINE
    public static Pointer arrayOriginToCell(Pointer origin) {
        return arrayHeaderLayout().originToCell(origin);
    }

    @UNSAFE
    @FOLD
    private static ReferenceScheme referenceScheme() {
        return vmConfig().referenceScheme();
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean isArray(Reference reference) {
        return generalLayout().isArray(reference);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Size size(Reference reference) {
        return generalLayout().size(reference);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Size size(Pointer origin) {
        return generalLayout().size(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Reference readHubReference(Reference reference) {
        return generalLayout().readHubReference(reference);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Reference readHubReference(Pointer origin) {
        return generalLayout().readHubReference(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeHubReference(Reference reference, Reference hubReference) {
        generalLayout().writeHubReference(reference, hubReference);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeHubReference(Pointer origin, Reference hubReference) {
        generalLayout().writeHubReference(origin, hubReference);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Word readMisc(Reference reference) {
        return generalLayout().readMisc(reference);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeMisc(Reference reference, Word value) {
        generalLayout().writeMisc(reference, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeMisc(Pointer origin, Word value) {
        generalLayout().writeMisc(origin, value);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Word compareAndSwapMisc(Reference reference, Word expectedValue, Word newValue) {
        return generalLayout().compareAndSwapMisc(reference, expectedValue, newValue);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static int readArrayLength(Reference reference) {
        return arrayHeaderLayout().readLength(reference);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static int readArrayLength(Pointer pointer) {
        return arrayHeaderLayout().readLength(pointer);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeArrayLength(Pointer origin, int hashCode) {
        arrayHeaderLayout().writeLength(origin, hashCode);
    }

    /**
     * @see ArrayHeaderLayout#getArraySize(Kind, int)
     */
    @INLINE
    public static Size getArraySize(Kind kind, int length) {
        return arrayHeaderLayout().getArraySize(kind, length);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Reference readForwardRef(Pointer origin) {
        return generalLayout().readForwardRef(origin);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeForwardRef(Pointer origin, Reference forwardRef) {
        generalLayout().writeForwardRef(origin, forwardRef);
    }

    @UNSAFE
    @FOLD
    public static ByteArrayLayout byteArrayLayout() {
        return layoutScheme().byteArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static byte getByte(Reference array, int index) {
        return byteArrayLayout().getByte(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setByte(Reference array, int index, byte value) {
        byteArrayLayout().setByte(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static BooleanArrayLayout booleanArrayLayout() {
        return layoutScheme().booleanArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean getBoolean(Reference array, int index) {
        return booleanArrayLayout().getBoolean(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setBoolean(Reference array, int index, boolean value) {
        booleanArrayLayout().setBoolean(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static ShortArrayLayout shortArrayLayout() {
        return layoutScheme().shortArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static short getShort(Reference array, int index) {
        return shortArrayLayout().getShort(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setShort(Reference array, int index, short value) {
        shortArrayLayout().setShort(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static CharArrayLayout charArrayLayout() {
        return layoutScheme().charArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static char getChar(Reference array, int index) {
        return charArrayLayout().getChar(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setChar(Reference array, int index, char value) {
        charArrayLayout().setChar(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static IntArrayLayout intArrayLayout() {
        return layoutScheme().intArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static int getInt(Reference array, int index) {
        return intArrayLayout().getInt(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setInt(Reference array, int index, int value) {
        intArrayLayout().setInt(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static FloatArrayLayout floatArrayLayout() {
        return layoutScheme().floatArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static float getFloat(Reference array, int index) {
        return floatArrayLayout().getFloat(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setFloat(Reference array, int index, float value) {
        floatArrayLayout().setFloat(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static LongArrayLayout longArrayLayout() {
        return layoutScheme().longArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static long getLong(Reference array, int index) {
        return longArrayLayout().getLong(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setLong(Reference array, int index, long value) {
        longArrayLayout().setLong(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static DoubleArrayLayout doubleArrayLayout() {
        return layoutScheme().doubleArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static double getDouble(Reference array, int index) {
        return doubleArrayLayout().getDouble(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setDouble(Reference array, int index, double value) {
        doubleArrayLayout().setDouble(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static WordArrayLayout wordArrayLayout() {
        return layoutScheme().wordArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Word getWord(Reference array, int index) {
        return wordArrayLayout().getWord(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setWord(Reference array, int index, Word value) {
        wordArrayLayout().setWord(array, index, value);
    }

    @UNSAFE
    @FOLD
    public static ReferenceArrayLayout referenceArrayLayout() {
        return layoutScheme().referenceArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Reference getReference(Reference array, int index) {
        return referenceArrayLayout().getReference(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Reference getReference(Pointer array, int index) {
        return referenceArrayLayout().getReference(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setReference(Reference array, int index, Reference value) {
        referenceArrayLayout().setReference(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setReference(Pointer array, int index, Reference value) {
        referenceArrayLayout().setReference(array, index, value);
    }
}
