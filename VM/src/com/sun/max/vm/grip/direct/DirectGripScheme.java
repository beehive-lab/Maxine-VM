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

package com.sun.max.vm.grip.direct;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * Direct grips.  Straight to the object.
 *
 * @author Matthew Seidl
 */
public final class DirectGripScheme extends AbstractVMScheme implements GripScheme {

    private DataModel dataModel;
    @INLINE
    public DataModel dataModel() {
        return dataModel;
    }

    private WordWidth gripWidth;

    @INLINE
    public WordWidth gripWidth() {
        return gripWidth;
    }

    public DirectGripScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        dataModel = vmConfiguration.platform().processorKind.dataModel;
        gripWidth = dataModel.wordWidth;
    }

    @INLINE
    public boolean isConstant() {
        return false;
    }

    @INLINE(override = true)
    public Grip fromJava(Object object) {
        return UnsafeCast.asGrip(object);
    }

    @INLINE(override = true)
    public Object toJava(Grip grip) {
        return grip;
    }

    @INLINE
    public Grip fromReference(Reference reference) {
        return toGrip(reference);
    }

    @UNSAFE_CAST
    private static native Grip toGrip(Reference reference);

    @INLINE
    public Grip fromOrigin(Pointer origin) {
        return toGrip(origin);
    }

    @UNSAFE_CAST
    private static native Grip toGrip(Pointer origin);

    @INLINE
    public Pointer toOrigin(Grip grip) {
        return toWord(grip).asPointer();
    }

    @UNSAFE_CAST
    private static native Word toWord(Grip grip);

    @INLINE
    public Grip updateGrip(Grip grip, Pointer origin) {
        return fromOrigin(origin);
    }

    public byte[] createPrototypeGrip(Address address) {
        return dataModel().toBytes(address);
    }

    public byte[] createPrototypeNullGrip() {
        return dataModel().toBytes(Word.zero());
    }

    @INLINE
    public Grip zero() {
        return fromOrigin(Pointer.zero());
    }

    @INLINE
    public boolean isZero(Grip grip) {
        return toOrigin(grip).isZero();
    }

    @INLINE
    public boolean isAllOnes(Grip grip) {
        return toOrigin(grip).isAllOnes();
    }

    public boolean equals(Grip grip1, Grip grip2) {
        return grip1 == grip2;
    }

    @INLINE
    public boolean isMarked(Grip grip) {
        return toOrigin(grip).isBitSet(0);
    }

    @INLINE
    public Grip marked(Grip grip) {
        final Pointer origin = toOrigin(grip).bitSet(0);
        return Grip.fromOrigin(origin);
    }

    @INLINE
    public Grip unmarked(Grip grip) {
        final Pointer origin = toOrigin(grip).bitClear(0);
        return Grip.fromOrigin(origin);
    }

    @INLINE
    public byte readByte(Grip grip, Offset offset) {
        return toOrigin(grip).readByte(offset);
    }

    @INLINE
    public byte readByte(Grip grip, int offset) {
        return toOrigin(grip).readByte(offset);
    }

    @INLINE
    public byte getByte(Grip grip, int displacement, int index) {
        return toOrigin(grip).getByte(displacement, index);
    }

    @INLINE
    public boolean readBoolean(Grip grip, Offset offset) {
        return toOrigin(grip).readBoolean(offset);
    }

    @INLINE
    public boolean readBoolean(Grip grip, int offset) {
        return toOrigin(grip).readBoolean(offset);
    }

    @INLINE
    public boolean getBoolean(Grip grip, int displacement, int index) {
        return toOrigin(grip).getBoolean(displacement, index);
    }

    @INLINE
    public short readShort(Grip grip, Offset offset) {
        return toOrigin(grip).readShort(offset);
    }

    @INLINE
    public short readShort(Grip grip, int offset) {
        return toOrigin(grip).readShort(offset);
    }

    @INLINE
    public short getShort(Grip grip, int displacement, int index) {
        return toOrigin(grip).getShort(displacement, index);
    }

    @INLINE
    public char readChar(Grip grip, Offset offset) {
        return toOrigin(grip).readChar(offset);
    }

    @INLINE
    public char readChar(Grip grip, int offset) {
        return toOrigin(grip).readChar(offset);
    }

    @INLINE
    public char getChar(Grip grip, int displacement, int index) {
        return toOrigin(grip).getChar(displacement, index);
    }

    @INLINE
    public int readInt(Grip grip, Offset offset) {
        return toOrigin(grip).readInt(offset);
    }

    @INLINE
    public int readInt(Grip grip, int offset) {
        return toOrigin(grip).readInt(offset);
    }

    @INLINE
    public int getInt(Grip grip, int displacement, int index) {
        return toOrigin(grip).getInt(displacement, index);
    }

    @INLINE
    public float readFloat(Grip grip, Offset offset) {
        return toOrigin(grip).readFloat(offset);
    }

    @INLINE
    public float readFloat(Grip grip, int offset) {
        return toOrigin(grip).readFloat(offset);
    }

    @INLINE
    public float getFloat(Grip grip, int displacement, int index) {
        return toOrigin(grip).getFloat(displacement, index);
    }

    @INLINE
    public long readLong(Grip grip, Offset offset) {
        return toOrigin(grip).readLong(offset);
    }

    @INLINE
    public long readLong(Grip grip, int offset) {
        return toOrigin(grip).readLong(offset);
    }

    @INLINE
    public long getLong(Grip grip, int displacement, int index) {
        return toOrigin(grip).getLong(displacement, index);
    }

    @INLINE
    public double readDouble(Grip grip, Offset offset) {
        return toOrigin(grip).readDouble(offset);
    }

    @INLINE
    public double readDouble(Grip grip, int offset) {
        return toOrigin(grip).readDouble(offset);
    }

    @INLINE
    public double getDouble(Grip grip, int displacement, int index) {
        return toOrigin(grip).getDouble(displacement, index);
    }

    @INLINE
    public Word readWord(Grip grip, Offset offset) {
        return toOrigin(grip).readWord(offset);
    }

    @INLINE
    public Word readWord(Grip grip, int offset) {
        return toOrigin(grip).readWord(offset);
    }

    @INLINE
    public Word getWord(Grip grip, int displacement, int index) {
        return toOrigin(grip).getWord(displacement, index);
    }

    @INLINE
    public Grip readGrip(Grip grip, Offset offset) {
        return toOrigin(grip).readReference(offset).toGrip();
    }

    @INLINE
    public Grip readGrip(Grip grip, int offset) {
        return toOrigin(grip).readReference(offset).toGrip();
    }

    @INLINE
    public Grip getGrip(Grip grip, int displacement, int index) {
        return toOrigin(grip).getReference(displacement, index).toGrip();
    }

    @INLINE
    public void writeByte(Grip grip, Offset offset, byte value) {
        toOrigin(grip).writeByte(offset, value);
    }

    @INLINE
    public void writeByte(Grip grip, int offset, byte value) {
        toOrigin(grip).writeByte(offset, value);
    }

    @INLINE
    public void setByte(Grip grip, int displacement, int index, byte value) {
        toOrigin(grip).setByte(displacement, index, value);
    }

    @INLINE
    public void writeBoolean(Grip grip, Offset offset, boolean value) {
        toOrigin(grip).writeBoolean(offset, value);
    }

    @INLINE
    public void writeBoolean(Grip grip, int offset, boolean value) {
        toOrigin(grip).writeBoolean(offset, value);
    }

    @INLINE
    public void setBoolean(Grip grip, int displacement, int index, boolean value) {
        toOrigin(grip).setBoolean(displacement, index, value);
    }

    @INLINE
    public void writeShort(Grip grip, Offset offset, short value) {
        toOrigin(grip).writeShort(offset, value);
    }

    @INLINE
    public void writeShort(Grip grip, int offset, short value) {
        toOrigin(grip).writeShort(offset, value);
    }

    @INLINE
    public void setShort(Grip grip, int displacement, int index, short value) {
        toOrigin(grip).setShort(displacement, index, value);
    }

    @INLINE
    public void writeChar(Grip grip, Offset offset, char value) {
        toOrigin(grip).writeChar(offset, value);
    }

    @INLINE
    public void writeChar(Grip grip, int offset, char value) {
        toOrigin(grip).writeChar(offset, value);
    }

    @INLINE
    public void setChar(Grip grip, int displacement, int index, char value) {
        toOrigin(grip).setChar(displacement, index, value);
    }

    @INLINE
    public void writeInt(Grip grip, Offset offset, int value) {
        toOrigin(grip).writeInt(offset, value);
    }

    @INLINE
    public void writeInt(Grip grip, int offset, int value) {
        toOrigin(grip).writeInt(offset, value);
    }

    @INLINE
    public void setInt(Grip grip, int displacement, int index, int value) {
        toOrigin(grip).setInt(displacement, index, value);
    }

    @INLINE
    public void writeFloat(Grip grip, Offset offset, float value) {
        toOrigin(grip).writeFloat(offset, value);
    }

    @INLINE
    public void writeFloat(Grip grip, int offset, float value) {
        toOrigin(grip).writeFloat(offset, value);
    }

    @INLINE
    public void setFloat(Grip grip, int displacement, int index, float value) {
        toOrigin(grip).setFloat(displacement, index, value);
    }



    @INLINE
    public void writeDouble(Grip grip, Offset offset, double value) {
        toOrigin(grip).writeDouble(offset, value);
    }

    @INLINE
    public void writeDouble(Grip grip, int offset, double value) {
        toOrigin(grip).writeDouble(offset, value);
    }

    @INLINE
    public void setDouble(Grip grip, int displacement, int index, double value) {
        toOrigin(grip).setDouble(displacement, index, value);
    }



    @INLINE
    public int compareAndSwapInt(Grip grip, Offset offset, int suspectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    @INLINE
    public int compareAndSwapInt(Grip grip, int offset, int suspectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    @INLINE
    public void writeGrip(Grip grip, Offset offset, Grip value) {
        toOrigin(grip).writeReference(offset, value.toReference());
    }

    @INLINE
    public Reference compareAndSwapReference(Grip grip, Offset offset, Reference suspectedValue, Reference newValue) {
        return toOrigin(grip).compareAndSwapReference(offset, suspectedValue, newValue);
    }

    @INLINE
    public Reference compareAndSwapReference(Grip grip, int offset, Reference suspectedValue, Reference newValue) {
        return toOrigin(grip).compareAndSwapReference(offset, suspectedValue, newValue);
    }

    @INLINE
    public Word compareAndSwapWord(Grip grip, Offset offset, Word suspectedValue, Word newValue) {
        return toOrigin(grip).compareAndSwapWord(offset, suspectedValue, newValue);
    }

    @INLINE
    public Word compareAndSwapWord(Grip grip, int offset, Word suspectedValue, Word newValue) {
        return toOrigin(grip).compareAndSwapWord(offset, suspectedValue, newValue);
    }

    @INLINE
    public void writeWord(Grip grip, Offset offset, Word value) {
        toOrigin(grip).writeWord(offset, value);
    }

    @INLINE
    public void writeWord(Grip grip, int offset, Word value) {
        toOrigin(grip).writeWord(offset, value);
    }

    @INLINE
    public void setWord(Grip grip, int displacement, int index, Word value) {
        toOrigin(grip).setWord(displacement, index, value);
    }

    @INLINE
    public void writeGrip(Grip grip, int offset, Grip value) {
        toOrigin(grip).writeReference(offset, value.toReference());
    }

    @INLINE
    public void setGrip(Grip grip, int displacement, int index, Grip value) {
        toOrigin(grip).setReference(displacement, index, value.toReference());
    }

    @INLINE
    public void writeLong(Grip grip, Offset offset, long value) {
        toOrigin(grip).writeLong(offset, value);
    }

    @INLINE
    public void writeLong(Grip grip, int offset, long value) {
        toOrigin(grip).writeLong(offset, value);
    }

    @INLINE
    public void setLong(Grip grip, int displacement, int index, long value) {
        toOrigin(grip).setLong(displacement, index, value);
    }

}
