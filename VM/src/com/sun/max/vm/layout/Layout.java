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
package com.sun.max.vm.layout;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Static convenience methods for access to object layout information in the
 * context of the current {@linkplain VMConfiguration VM configuration}.
 */
public final class Layout {

    private Layout() {
    }

    /**
     * Returns the layout scheme being used in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @return  the layout scheme in use
     */
    @UNSAFE
    @FOLD
    public static LayoutScheme layoutScheme() {
        return vmConfig().layoutScheme();
    }

    /**
     * Returns the general layout scheme being used in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @return the general layout scheme in use
     */
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
        public static final HeaderField HUB = new HeaderField("HUB", "Reference to layout description (\"Hub\") for the object, based on its type");

        /**
         * The header word in which the monitor and hash code details of an object are encoded.
         */
        public static final HeaderField MISC = new HeaderField("MISC", "Encoded monitor and hash code details for the object");

        /**
         * The header word in which the length of an array object is encoded.
         */
        public static final HeaderField LENGTH = new HeaderField("LENGTH", "The number of elements in the array");

        /**
         * The name of this header field.
         */
        public final String name;

        /**
         * A short, human-readable description of the field and the role it plays, suitable for inspection.
         */
        public final String description;

        public HeaderField(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Category {
        TUPLE, ARRAY, HYBRID;
    }

    /**
     * Access to <strong>tuple object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static TupleLayout tupleLayout() {
        return layoutScheme().tupleLayout;
    }

    /**
     * Access to <strong>hybrid object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static HybridLayout hybridLayout() {
        return layoutScheme().hybridLayout;
    }

    /**
     * Access to <strong>array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout arrayLayout() {
        return layoutScheme().arrayLayout;
    }

    /**
     * Computes the <strong>origin</strong> of an object, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param cell starting location of an object in memory
     * @return the location of the object's origin
     */
    @INLINE
    public static Pointer cellToOrigin(Pointer cell) {
        return generalLayout().cellToOrigin(cell);
    }

    /**
     * Computes the <strong>origin</strong> of a tuple object, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param cell starting location of a tuple object in memory
     * @return the location of the object's origin
     */
    @INLINE
    public static Pointer tupleCellToOrigin(Pointer cell) {
        return tupleLayout().cellToOrigin(cell);
    }

    /**
     * Computes the <strong>origin</strong> of a hybrid object, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param cell starting location of a hybrid object in memory
     * @return the location of the object's origin
     */
    @INLINE
    public static Pointer hybridCellToOrigin(Pointer cell) {
        return hybridLayout().cellToOrigin(cell);
    }

    /**
     * Computes the <strong>origin</strong> of an array object, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param cell starting location of an array object in memory
     * @return the location of the object's origin
     */
    @INLINE
    public static Pointer arrayCellToOrigin(Pointer cell) {
        return arrayLayout().cellToOrigin(cell);
    }

    /**
     * Computes the starting location of an object in VM memory, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of the object's origin
     * @return starting location of the object in memory
     */
    @INLINE
    public static Pointer originToCell(Pointer origin) {
        return generalLayout().originToCell(origin);
    }

    /**
     * Computes the starting location of a tuple object in VM memory, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of the tuple object's origin
     * @return starting location of the object in memory
     */
    @INLINE
    public static Pointer tupleOriginToCell(Pointer origin) {
        return tupleLayout().originToCell(origin);
    }

    /**
     * Computes the starting location of a hybrid object in VM memory, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of the hybrid object's origin
     * @return starting location of the object in memory
     */
    @INLINE
    public static Pointer hybridOriginToCell(Pointer origin) {
        return hybridLayout().originToCell(origin);
    }

    /**
     * Computes the starting location of an array object in VM memory, using layout information in
     * the context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of the array object's origin
     * @return starting location of the object in memory
     */
    @INLINE
    public static Pointer arrayOriginToCell(Pointer origin) {
        return arrayLayout().originToCell(origin);
    }

    @UNSAFE
    @FOLD
    private static ReferenceScheme referenceScheme() {
        return vmConfig().referenceScheme();
    }

    /**
     * Determines whether an object is an array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @return true iff the object is an array
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean isArray(Reference reference) {
        return generalLayout().isArray(reference);
    }

    /**
     * Gets the size an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @return the size of the object
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static Size size(Reference reference) {
        return generalLayout().size(reference);
    }

    /**
     * Gets the size an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of an object
     * @return the size of the object
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static Size size(Pointer origin) {
        return generalLayout().size(origin);
    }

    /**
     * Reads the hub reference word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @return the value of the object's hub reference
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static Reference readHubReference(Reference reference) {
        return generalLayout().readHubReference(reference);
    }

    /**
     * Reads the hub reference word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of an object
     * @return the value of the object's hub reference
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static Reference readHubReference(Pointer origin) {
        return generalLayout().readHubReference(origin);
    }

    /**
     * Reads the hub reference word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @return the value of the object's hub reference as a word
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static Word readHubReferenceAsWord(Reference reference) {
        return generalLayout().readHubReferenceAsWord(reference);
    }

    /**
     * Reads the hub reference word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of an object
     * @return the value of the object's hub reference as a word
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static Word readHubReferenceAsWord(Pointer origin) {
        return generalLayout().readHubReferenceAsWord(origin);
    }

    /**
     * Writes the hub reference word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @param hubReference the new value of the object's hub reference
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeHubReference(Reference reference, Reference hubReference) {
        generalLayout().writeHubReference(reference, hubReference);
    }

    /**
     * Writes the hub reference word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of an object
     * @param hubReference the new value of the object's hub reference
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeHubReference(Pointer origin, Reference hubReference) {
        generalLayout().writeHubReference(origin, hubReference);
    }

    /**
     * Reads the "misc" word from an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @return the object's "misc" value
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static Word readMisc(Reference reference) {
        return generalLayout().readMisc(reference);
    }

    /**
     * Writes the "misc" word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an object
     * @param value the new "misc" value
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeMisc(Reference reference, Word value) {
        generalLayout().writeMisc(reference, value);
    }

    /**
     * Writes the "misc" word in an object, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param origin location of an object
     * @param value the new "misc" value
     */
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

    /**
     * Reads the length word from an array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param reference an array object
     * @return array length
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static int readArrayLength(Reference reference) {
        return arrayLayout().readLength(reference);
    }

    /**
     * Reads the length word from an array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param pointer location of an array object
     * @return array length
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static int readArrayLength(Pointer pointer) {
        return arrayLayout().readLength(pointer);
    }

    /**
     * Writes the length word in an array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param pointer location of an array object
     * @param length the new value of the array's length word
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeArrayLength(Pointer origin, int length) {
        arrayLayout().writeLength(origin, length);
    }

    /**
     * @see ArrayLayout#getArraySize(Kind, int)
     */
    @INLINE
    public static Size getArraySize(Kind kind, int length) {
        return arrayLayout().getArraySize(kind, length);
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

    /**
     * Access to <strong>byte array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout byteArrayLayout() {
        return layoutScheme().byteArrayLayout;
    }

    /**
     * Reads an element from a byte array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a byte array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static byte getByte(Reference array, int index) {
        return byteArrayLayout().getByte(array, index);
    }

    /**
     * Writes into an element in a byte array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a byte array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setByte(Reference array, int index, byte value) {
        byteArrayLayout().setByte(array, index, value);
    }

    /**
     * Access to <strong>boolean array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout booleanArrayLayout() {
        return layoutScheme().booleanArrayLayout;
    }

    /**
     * Reads an element from a boolean array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a boolean array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean getBoolean(Reference array, int index) {
        return booleanArrayLayout().getBoolean(array, index);
    }

    /**
     * Writes into an element in a boolean array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a boolean array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setBoolean(Reference array, int index, boolean value) {
        booleanArrayLayout().setBoolean(array, index, value);
    }

    /**
     * Access to <strong>short array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout shortArrayLayout() {
        return layoutScheme().shortArrayLayout;
    }

    /**
     * Reads an element from a short array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a short array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static short getShort(Reference array, int index) {
        return shortArrayLayout().getShort(array, index);
    }

    /**
     * Writes into an element in a short array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a short array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setShort(Reference array, int index, short value) {
        shortArrayLayout().setShort(array, index, value);
    }

    /**
     * Access to <strong>char array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout charArrayLayout() {
        return layoutScheme().charArrayLayout;
    }

    /**
     * Reads an element from a char array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a char array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static char getChar(Reference array, int index) {
        return charArrayLayout().getChar(array, index);
    }

    /**
     * Writes into an element in a char array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a char array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setChar(Reference array, int index, char value) {
        charArrayLayout().setChar(array, index, value);
    }

    /**
     * Access to <strong>int array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout intArrayLayout() {
        return layoutScheme().intArrayLayout;
    }

    /**
     * Reads an element from a int array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a int array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static int getInt(Reference array, int index) {
        return intArrayLayout().getInt(array, index);
    }

    /**
     * Writes into an element in a int array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a int array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setInt(Reference array, int index, int value) {
        intArrayLayout().setInt(array, index, value);
    }

    /**
     * Access to <strong>float array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout floatArrayLayout() {
        return layoutScheme().floatArrayLayout;
    }

    /**
     * Reads an element from a float array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a float array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static float getFloat(Reference array, int index) {
        return floatArrayLayout().getFloat(array, index);
    }

    /**
     * Writes into an element in a float array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a float array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setFloat(Reference array, int index, float value) {
        floatArrayLayout().setFloat(array, index, value);
    }

    /**
     * Access to <strong>long array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout longArrayLayout() {
        return layoutScheme().longArrayLayout;
    }

    /**
     * Reads an element from a long array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a long array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static long getLong(Reference array, int index) {
        return longArrayLayout().getLong(array, index);
    }

    /**
     * Writes into an element in a long array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a long array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setLong(Reference array, int index, long value) {
        longArrayLayout().setLong(array, index, value);
    }

    /**
     * Access to <strong>double array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout doubleArrayLayout() {
        return layoutScheme().doubleArrayLayout;
    }

    /**
     * Reads an element from a double array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a double array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static double getDouble(Reference array, int index) {
        return doubleArrayLayout().getDouble(array, index);
    }

    /**
     * Writes into an element in a double array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a double array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setDouble(Reference array, int index, double value) {
        doubleArrayLayout().setDouble(array, index, value);
    }

    /**
     * Access to <strong>word array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout wordArrayLayout() {
        return layoutScheme().wordArrayLayout;
    }

    /**
     * Reads an element from a word array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a word array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static Word getWord(Reference array, int index) {
        return wordArrayLayout().getWord(array, index);
    }

    /**
     * Writes into an element in a word array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a word array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setWord(Reference array, int index, Word value) {
        wordArrayLayout().setWord(array, index, value);
    }

    /**
     * Access to <strong>reference array object</strong> layout information in the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     */
    @UNSAFE
    @FOLD
    public static ArrayLayout referenceArrayLayout() {
        return layoutScheme().referenceArrayLayout;
    }

    /**
     * Reads an element from a reference array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a reference array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static Reference getReference(Reference array, int index) {
        return referenceArrayLayout().getReference(array, index);
    }

    /**
     * Reads an element from a reference array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array location of a reference array
     * @param index the element to read
     * @return the value of the indexed element
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static Reference getReference(Pointer array, int index) {
        return referenceArrayLayout().getReference(array, index);
    }

    /**
     * Writes into an element in a reference array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array a reference array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Reference.class)
    @INLINE
    public static void setReference(Reference array, int index, Reference value) {
        referenceArrayLayout().setReference(array, index, value);
    }

    /**
     * Writes into an element in a reference array, using layout information from the
     * context of the current {@linkplain VMConfiguration VM configuration}.
     *
     * @param array location of a reference array
     * @param index the element into which to write
     * @param value the new value of the indexed element
     */
    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setReference(Pointer array, int index, Reference value) {
        referenceArrayLayout().setReference(array, index, value);
    }
}
