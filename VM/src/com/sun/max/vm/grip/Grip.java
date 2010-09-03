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

package com.sun.max.vm.grip;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * @author Matthew L. Seidl
 * @author Bernd Mathiske
 */
public class Grip implements Accessor {

    protected Grip() {
    }

    @UNSAFE
    @FOLD
    private static GripScheme gripScheme() {
        return vmConfig().gripScheme();
    }

    @UNSAFE
    @FOLD
    private static ReferenceScheme referenceScheme() {
        return vmConfig().referenceScheme();
    }

    @FOLD
    public static boolean isConstant() {
        return gripScheme().isConstant();
    }

    @INLINE
    public static Grip fromJava(Object object) {
        return gripScheme().fromJava(object);
    }

    @INLINE
    public final Object toJava() {
        return gripScheme().toJava(this);
    }

    @INLINE
    public final Reference toReference() {
        return referenceScheme().fromGrip(this);
    }

    @INLINE
    public static Grip fromOrigin(Pointer origin) {
        return gripScheme().fromOrigin(origin);
    }

    @INLINE
    public final Pointer toOrigin() {
        return gripScheme().toOrigin(this);
    }

    @INLINE
    public final boolean isMarked() {
        return gripScheme().isMarked(this);
    }

    @INLINE
    public final Grip marked() {
        return gripScheme().marked(this);
    }

    @INLINE
    public final Grip unmarked() {
        return gripScheme().unmarked(this);
    }

    @INLINE
    public final boolean isZero() {
        return gripScheme().isZero(this);
    }

    @INLINE
    public final byte readByte(Offset offset) {
        return gripScheme().readByte(this, offset);
    }

    @INLINE
    public final byte readByte(int offset) {
        return gripScheme().readByte(this, offset);
    }

    @INLINE
    public final byte getByte(int displacement, int index) {
        return gripScheme().getByte(this, displacement, index);
    }

    @INLINE
    public final boolean readBoolean(Offset offset) {
        return gripScheme().readBoolean(this, offset);
    }

    @INLINE
    public final boolean readBoolean(int offset) {
        return gripScheme().readBoolean(this, offset);
    }

    @INLINE
    public final boolean getBoolean(int displacement, int index) {
        return gripScheme().getBoolean(this, displacement, index);
    }

    @INLINE
    public final short readShort(Offset offset) {
        return gripScheme().readShort(this, offset);
    }

    @INLINE
    public final short readShort(int offset) {
        return gripScheme().readShort(this, offset);
    }

    @INLINE
    public final short getShort(int displacement, int index) {
        return gripScheme().getShort(this, displacement, index);
    }

    @INLINE
    public final char readChar(Offset offset) {
        return gripScheme().readChar(this, offset);
    }

    @INLINE
    public final char readChar(int offset) {
        return gripScheme().readChar(this, offset);
    }

    @INLINE
    public final char getChar(int displacement, int index) {
        return gripScheme().getChar(this, displacement, index);
    }

    @INLINE
    public final int readInt(Offset offset) {
        return gripScheme().readInt(this, offset);
    }

    @INLINE
    public final int readInt(int offset) {
        return gripScheme().readInt(this, offset);
    }

    @INLINE
    public final int getInt(int displacement, int index) {
        return gripScheme().getInt(this, displacement, index);
    }

    @INLINE
    public final float readFloat(Offset offset) {
        return gripScheme().readFloat(this, offset);
    }

    @INLINE
    public final float readFloat(int offset) {
        return gripScheme().readFloat(this, offset);
    }

    @INLINE
    public final float getFloat(int displacement, int index) {
        return gripScheme().getFloat(this, displacement, index);
    }

    @INLINE
    public final long readLong(Offset offset) {
        return gripScheme().readLong(this, offset);
    }

    @INLINE
    public final long readLong(int offset) {
        return gripScheme().readLong(this, offset);
    }

    @INLINE
    public final long getLong(int displacement, int index) {
        return gripScheme().getLong(this, displacement, index);
    }

    @INLINE
    public final double readDouble(Offset offset) {
        return gripScheme().readDouble(this, offset);
    }

    @INLINE
    public final double readDouble(int offset) {
        return gripScheme().readDouble(this, offset);
    }

    @INLINE
    public final double getDouble(int displacement, int index) {
        return gripScheme().getDouble(this, displacement, index);
    }

    @INLINE
    public final Word readWord(Offset offset) {
        return gripScheme().readWord(this, offset);
    }

    @INLINE
    public final Word readWord(int offset) {
        return gripScheme().readWord(this, offset);
    }

    @INLINE
    public final Word getWord(int displacement, int index) {
        return gripScheme().getWord(this, displacement, index);
    }

    @INLINE
    public final Grip readGrip(Offset offset) {
        return gripScheme().readGrip(this, offset);
    }

    @INLINE
    public final Grip readGrip(int offset) {
        return gripScheme().readGrip(this, offset);
    }

    @INLINE
    public final Grip getGrip(int displacement, int index) {
        return gripScheme().getGrip(this, displacement, index);
    }

    @INLINE
    public final Reference readReference(Offset offset) {
        return gripScheme().readGrip(this, offset).toReference();
    }

    @INLINE
    public final Reference readReference(int offset) {
        return gripScheme().readGrip(this, offset).toReference();
    }

    @INLINE
    public final Reference getReference(int displacement, int index) {
        return gripScheme().getGrip(this, displacement, index).toReference();
    }

    @INLINE
    public final void writeByte(Offset offset, byte value) {
        gripScheme().writeByte(this, offset, value);
    }

    @INLINE
    public final void writeByte(int offset, byte value) {
        gripScheme().writeByte(this, offset, value);
    }

    @INLINE
    public final void setByte(int displacement, int index, byte value) {
        gripScheme().setByte(this, displacement, index, value);
    }

    @INLINE
    public final void writeBoolean(Offset offset, boolean value) {
        gripScheme().writeBoolean(this, offset, value);
    }

    @INLINE
    public final void writeBoolean(int offset, boolean value) {
        gripScheme().writeBoolean(this, offset, value);
    }

    @INLINE
    public final void setBoolean(int displacement, int index, boolean value) {
        gripScheme().setBoolean(this, displacement, index, value);
    }

    @INLINE
    public final void writeShort(Offset offset, short value) {
        gripScheme().writeShort(this, offset, value);
    }

    @INLINE
    public final void writeShort(int offset, short value) {
        gripScheme().writeShort(this, offset, value);
    }

    @INLINE
    public final void setShort(int displacement, int index, short value) {
        gripScheme().setShort(this, displacement, index, value);
    }

    @INLINE
    public final void writeChar(Offset offset, char value) {
        gripScheme().writeChar(this, offset, value);
    }

    @INLINE
    public final void writeChar(int offset, char value) {
        gripScheme().writeChar(this, offset, value);
    }

    @INLINE
    public final void setChar(int displacement, int index, char value) {
        gripScheme().setChar(this, displacement, index, value);
    }

    @INLINE
    public final void writeInt(Offset offset, int value) {
        gripScheme().writeInt(this, offset, value);
    }

    @INLINE
    public final void writeInt(int offset, int value) {
        gripScheme().writeInt(this, offset, value);
    }

    @INLINE
    public final void setInt(int displacement, int index, int value) {
        gripScheme().setInt(this, displacement, index, value);
    }

    @INLINE
    public final void writeFloat(Offset offset, float value) {
        gripScheme().writeFloat(this, offset, value);
    }

    @INLINE
    public final void writeFloat(int offset, float value) {
        gripScheme().writeFloat(this, offset, value);
    }

    @INLINE
    public final void setFloat(int displacement, int index, float value) {
        gripScheme().setFloat(this, displacement, index, value);
    }

    @INLINE
    public final void writeLong(Offset offset, long value) {
        gripScheme().writeLong(this, offset, value);
    }

    @INLINE
    public final void writeLong(int offset, long value) {
        gripScheme().writeLong(this, offset, value);
    }

    @INLINE
    public final void setLong(int displacement, int index, long value) {
        gripScheme().setLong(this, displacement, index, value);
    }

    @INLINE
    public final void writeDouble(Offset offset, double value) {
        gripScheme().writeDouble(this, offset, value);
    }

    @INLINE
    public final void writeDouble(int offset, double value) {
        gripScheme().writeDouble(this, offset, value);
    }

    @INLINE
    public final void setDouble(int displacement, int index, double value) {
        gripScheme().setDouble(this, displacement, index, value);
    }

    @INLINE
    public final void writeWord(Offset offset, Word value) {
        gripScheme().writeWord(this, offset, value);
    }

    @INLINE
    public final void writeWord(int offset, Word value) {
        gripScheme().writeWord(this, offset, value);
    }

    @INLINE
    public final void setWord(int displacement, int index, Word value) {
        gripScheme().setWord(this, displacement, index, value);
    }

    @INLINE
    public final void writeGrip(Offset offset, Grip value) {
        gripScheme().writeGrip(this, offset, value);
    }

    @INLINE
    public final void writeGrip(int offset, Grip value) {
        gripScheme().writeGrip(this, offset, value);
    }

    @INLINE
    public final void setGrip(int displacement, int index, Grip value) {
        gripScheme().setGrip(this, displacement, index, value);
    }

    @INLINE
    public final void writeReference(Offset offset, Reference value) {
        gripScheme().writeGrip(this, offset, value.toGrip());
    }

    @INLINE
    public final void writeReference(int offset, Reference value) {
        gripScheme().writeGrip(this, offset, value.toGrip());
    }

    @INLINE
    public final void setReference(int displacement, int index, Reference value) {
        gripScheme().setGrip(this, displacement, index, value.toGrip());
    }

    @INLINE
    public final int compareAndSwapInt(Offset offset, int expectedValue, int newValue) {
        return gripScheme().compareAndSwapInt(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final int compareAndSwapInt(int offset, int expectedValue, int newValue) {
        return gripScheme().compareAndSwapInt(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final Word compareAndSwapWord(Offset offset, Word expectedValue, Word newValue) {
        return gripScheme().compareAndSwapWord(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final Word compareAndSwapWord(int offset, Word expectedValue, Word newValue) {
        return gripScheme().compareAndSwapWord(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final Reference compareAndSwapReference(Offset offset, Reference expectedValue, Reference newValue) {
        return gripScheme().compareAndSwapReference(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final Reference compareAndSwapReference(int offset, Reference expectedValue, Reference newValue) {
        return gripScheme().compareAndSwapReference(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final void copyElements(int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        gripScheme().copyElements(this, displacement, srcIndex, dst, dstIndex, length);
    }
}
