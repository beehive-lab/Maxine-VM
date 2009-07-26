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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

public final class Layout {

    private Layout() {
    }

    @UNSAFE
    @FOLD
    public static LayoutScheme layoutScheme() {
        return VMConfiguration.hostOrTarget().layoutScheme();
    }

    @UNSAFE
    @FOLD
    public static GeneralLayout generalLayout() {
        return layoutScheme().generalLayout;
    }

    public enum HeaderField {
        HUB, MISC, LENGTH;
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

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer cellToOrigin(Pointer cell) {
        return generalLayout().cellToOrigin(cell);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer tupleCellToOrigin(Pointer cell) {
        return tupleLayout().cellToOrigin(cell);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer hybridCellToOrigin(Pointer cell) {
        return hybridLayout().cellToOrigin(cell);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer arrayCellToOrigin(Pointer cell) {
        return arrayHeaderLayout().cellToOrigin(cell);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer originToCell(Pointer origin) {
        return generalLayout().originToCell(origin);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer tupleOriginToCell(Pointer origin) {
        return tupleLayout().originToCell(origin);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer hybridOriginToCell(Pointer origin) {
        return hybridLayout().originToCell(origin);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Pointer arrayOriginToCell(Pointer origin) {
        return arrayHeaderLayout().originToCell(origin);
    }

    @UNSAFE
    @FOLD
    private static ReferenceScheme referenceScheme() {
        return VMConfiguration.hostOrTarget().referenceScheme();
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Category category(Reference reference) {
        return generalLayout().category(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Category category(Grip grip) {
        return generalLayout().category(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Category category(Pointer origin) {
        return generalLayout().category(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean isArray(Reference reference) {
        return generalLayout().isArray(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static boolean isArray(Grip grip) {
        return generalLayout().isArray(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static boolean isArray(Pointer origin) {
        return generalLayout().isArray(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean isTuple(Reference reference) {
        return generalLayout().isTuple(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static boolean isTuple(Grip grip) {
        return generalLayout().isTuple(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static boolean isTuple(Pointer origin) {
        return generalLayout().isTuple(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean isHybrid(Reference reference) {
        return generalLayout().isHybrid(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static boolean isHybrid(Grip grip) {
        return generalLayout().isHybrid(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static boolean isHybrid(Pointer origin) {
        return generalLayout().isHybrid(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static SpecificLayout specificLayout(Reference reference) {
        return generalLayout().specificLayout(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static SpecificLayout specificLayout(Grip grip) {
        return generalLayout().specificLayout(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static SpecificLayout specificLayout(Pointer origin) {
        return generalLayout().specificLayout(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Size size(Reference reference) {
        return generalLayout().size(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Size size(Grip grip) {
        return generalLayout().size(grip);
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

    @ACCESSOR(Grip.class)
    @INLINE
    public static Reference readHubReference(Grip grip) {
        return generalLayout().readHubReference(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Reference readHubReference(Pointer origin) {
        return generalLayout().readHubReference(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Grip readHubGrip(Reference reference) {
        return readHubReference(reference).toGrip();
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Grip readHubGrip(Grip grip) {
        return readHubReference(grip).toGrip();
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Grip readHubGrip(Pointer origin) {
        return readHubReference(origin).toGrip();
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeHubReference(Reference reference, Reference hubReference) {
        generalLayout().writeHubReference(reference, hubReference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void writeHubReference(Grip grip, Reference hubReference) {
        generalLayout().writeHubReference(grip, hubReference);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeHubReference(Pointer origin, Reference hubReference) {
        generalLayout().writeHubReference(origin, hubReference);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeHubGrip(Reference reference, Grip hubGrip) {
        writeHubReference(reference, hubGrip.toReference());
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void writeHubGrip(Grip grip, Grip hubGrip) {
        writeHubReference(grip, hubGrip.toReference());
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeHubGrip(Pointer origin, Grip hubGrip) {
        writeHubReference(origin, hubGrip.toReference());
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Word readMisc(Reference reference) {
        return generalLayout().readMisc(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Word readMisc(Grip grip) {
        return generalLayout().readMisc(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Word readMisc(Pointer pointer) {
        return generalLayout().readMisc(pointer);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeMisc(Reference reference, Word value) {
        generalLayout().writeMisc(reference, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void writeMisc(Grip grip, Word value) {
        generalLayout().writeMisc(grip, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeMisc(Pointer origin, Word value) {
        generalLayout().writeMisc(origin, value);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Word compareAndSwapMisc(Reference reference, Word suspectedValue, Word newValue) {
        return generalLayout().compareAndSwapMisc(reference, suspectedValue, newValue);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Word compareAndSwapMisc(Grip grip, Word suspectedValue, Word newValue) {
        return generalLayout().compareAndSwapMisc(grip, suspectedValue, newValue);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Word compareAndSwapMisc(Pointer origin, Word suspectedValue, Word newValue) {
        return generalLayout().compareAndSwapMisc(origin, suspectedValue, newValue);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static int readArrayLength(Reference reference) {
        return arrayHeaderLayout().readLength(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static int readArrayLength(Grip grip) {
        return arrayHeaderLayout().readLength(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static int readArrayLength(Pointer pointer) {
        return arrayHeaderLayout().readLength(pointer);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeArrayLength(Reference reference, int hashCode) {
        arrayHeaderLayout().writeLength(reference, hashCode);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void writeArrayLength(Grip grip, int hashCode) {
        arrayHeaderLayout().writeLength(grip, hashCode);
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

    @ACCESSOR(Grip.class)
    @INLINE
    public static Grip forwarded(Grip grip) {
        return generalLayout().forwarded(grip);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Grip readForwardGrip(Reference reference) {
        return generalLayout().readForwardGrip(reference);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Grip readForwardGrip(Grip grip) {
        return generalLayout().readForwardGrip(grip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Grip readForwardGrip(Pointer origin) {
        return generalLayout().readForwardGrip(origin);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Grip readForwardGripValue(Pointer origin) {
        return generalLayout().readForwardGripValue(origin);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void writeForwardGrip(Reference reference, Grip forwardGrip) {
        generalLayout().writeForwardGrip(reference, forwardGrip);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void writeForwardGrip(Grip grip, Grip forwardGrip) {
        generalLayout().writeForwardGrip(grip, forwardGrip);
    }

    @INLINE
    public static Grip compareAndSwapForwardGrip(Grip grip, Grip suspectedGrip, Grip forwardGrip) {
        return  generalLayout().compareAndSwapForwardGrip(grip, suspectedGrip, forwardGrip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void writeForwardGrip(Pointer origin, Grip forwardGrip) {
        generalLayout().writeForwardGrip(origin, forwardGrip);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Grip compareAndSwapForwardGrip(Pointer grip, Grip suspectedGrip, Grip forwardGrip) {
        return  generalLayout().compareAndSwapForwardGrip(grip, suspectedGrip, forwardGrip);
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

    @ACCESSOR(Grip.class)
    @INLINE
    public static byte getByte(Grip array, int index) {
        return byteArrayLayout().getByte(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static byte getByte(Pointer array, int index) {
        return byteArrayLayout().getByte(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setByte(Reference array, int index, byte value) {
        byteArrayLayout().setByte(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setByte(Grip array, int index, byte value) {
        byteArrayLayout().setByte(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setByte(Pointer array, int index, byte value) {
        byteArrayLayout().setByte(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static BooleanArrayLayout booleanArrayLayout() {
        return layoutScheme().booleanArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static boolean getBoolean(Reference array, int index) {
        return booleanArrayLayout().getBoolean(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static boolean getBoolean(Grip array, int index) {
        return booleanArrayLayout().getBoolean(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static boolean getBoolean(Pointer array, int index) {
        return booleanArrayLayout().getBoolean(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setBoolean(Reference array, int index, boolean value) {
        booleanArrayLayout().setBoolean(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setBoolean(Grip array, int index, boolean value) {
        booleanArrayLayout().setBoolean(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setBoolean(Pointer array, int index, boolean value) {
        booleanArrayLayout().setBoolean(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static ShortArrayLayout shortArrayLayout() {
        return layoutScheme().shortArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static short getShort(Reference array, int index) {
        return shortArrayLayout().getShort(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static short getShort(Grip array, int index) {
        return shortArrayLayout().getShort(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static short getShort(Pointer array, int index) {
        return shortArrayLayout().getShort(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setShort(Reference array, int index, short value) {
        shortArrayLayout().setShort(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setShort(Grip array, int index, short value) {
        shortArrayLayout().setShort(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setShort(Pointer array, int index, short value) {
        shortArrayLayout().setShort(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static CharArrayLayout charArrayLayout() {
        return layoutScheme().charArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static char getChar(Reference array, int index) {
        return charArrayLayout().getChar(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static char getChar(Grip array, int index) {
        return charArrayLayout().getChar(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static char getChar(Pointer array, int index) {
        return charArrayLayout().getChar(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setChar(Reference array, int index, char value) {
        charArrayLayout().setChar(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setChar(Grip array, int index, char value) {
        charArrayLayout().setChar(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setChar(Pointer array, int index, char value) {
        charArrayLayout().setChar(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static IntArrayLayout intArrayLayout() {
        return layoutScheme().intArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static int getInt(Reference array, int index) {
        return intArrayLayout().getInt(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static int getInt(Grip array, int index) {
        return intArrayLayout().getInt(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static int getInt(Pointer array, int index) {
        return intArrayLayout().getInt(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setInt(Reference array, int index, int value) {
        intArrayLayout().setInt(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setInt(Grip array, int index, int value) {
        intArrayLayout().setInt(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setInt(Pointer array, int index, int value) {
        intArrayLayout().setInt(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static FloatArrayLayout floatArrayLayout() {
        return layoutScheme().floatArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static float getFloat(Reference array, int index) {
        return floatArrayLayout().getFloat(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static float getFloat(Grip array, int index) {
        return floatArrayLayout().getFloat(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static float getFloat(Pointer array, int index) {
        return floatArrayLayout().getFloat(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setFloat(Reference array, int index, float value) {
        floatArrayLayout().setFloat(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setFloat(Grip array, int index, float value) {
        floatArrayLayout().setFloat(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setFloat(Pointer array, int index, float value) {
        floatArrayLayout().setFloat(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static LongArrayLayout longArrayLayout() {
        return layoutScheme().longArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static long getLong(Reference array, int index) {
        return longArrayLayout().getLong(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static long getLong(Grip array, int index) {
        return longArrayLayout().getLong(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static long getLong(Pointer array, int index) {
        return longArrayLayout().getLong(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setLong(Reference array, int index, long value) {
        longArrayLayout().setLong(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setLong(Grip array, int index, long value) {
        longArrayLayout().setLong(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setLong(Pointer array, int index, long value) {
        longArrayLayout().setLong(array, index, value);
    }

    @UNSAFE
    @FOLD
    private static DoubleArrayLayout doubleArrayLayout() {
        return layoutScheme().doubleArrayLayout;
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static double getDouble(Reference array, int index) {
        return doubleArrayLayout().getDouble(array, index);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static double getDouble(Grip array, int index) {
        return doubleArrayLayout().getDouble(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static double getDouble(Pointer array, int index) {
        return doubleArrayLayout().getDouble(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setDouble(Reference array, int index, double value) {
        doubleArrayLayout().setDouble(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setDouble(Grip array, int index, double value) {
        doubleArrayLayout().setDouble(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setDouble(Pointer array, int index, double value) {
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

    @ACCESSOR(Grip.class)
    @INLINE
    public static Word getWord(Grip array, int index) {
        return wordArrayLayout().getWord(array, index);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Word getWord(Pointer array, int index) {
        return wordArrayLayout().getWord(array, index);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setWord(Reference array, int index, Word value) {
        wordArrayLayout().setWord(array, index, value);
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setWord(Grip array, int index, Word value) {
        wordArrayLayout().setWord(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setWord(Pointer array, int index, Word value) {
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

    @ACCESSOR(Grip.class)
    @INLINE
    public static Reference getReference(Grip array, int index) {
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

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setReference(Grip array, int index, Reference value) {
        referenceArrayLayout().setReference(array, index, value);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setReference(Pointer array, int index, Reference value) {
        referenceArrayLayout().setReference(array, index, value);
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static Grip getGrip(Reference array, int index) {
        return getReference(array, index).toGrip();
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static Grip getGrip(Grip array, int index) {
        return getReference(array, index).toGrip();
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static Grip getGrip(Pointer array, int index) {
        return getReference(array, index).toGrip();
    }

    @ACCESSOR(Reference.class)
    @INLINE
    public static void setGrip(Reference array, int index, Grip value) {
        referenceArrayLayout().setReference(array, index, value.toReference());
    }

    @ACCESSOR(Grip.class)
    @INLINE
    public static void setGrip(Grip array, int index, Grip value) {
        referenceArrayLayout().setReference(array, index, value.toReference());
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public static void setGrip(Pointer array, int index, Grip value) {
        referenceArrayLayout().setReference(array, index, value.toReference());
    }

}
