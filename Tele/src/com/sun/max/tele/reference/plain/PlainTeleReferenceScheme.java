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
package com.sun.max.tele.reference.plain;

import com.sun.max.annotate.*;
import com.sun.max.tele.grip.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * Plain pass-through to grips without any barriers.
 *
 * @author Bernd Mathiske
 */
public final class PlainTeleReferenceScheme extends TeleReferenceScheme {

    private final GripScheme gripScheme;

    @FOLD
    public GripScheme gripScheme() {
        return gripScheme;
    }

    public PlainTeleReferenceScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        gripScheme = vmConfiguration.gripScheme();
    }

    @Override
    public Reference createReference(TeleGrip grip) {
        return new PlainTeleReference(grip);
    }

    @INLINE(override = true)
    public Reference fromGrip(Grip grip) {
        final TeleGrip teleGrip = (TeleGrip) grip;
        return teleGrip.makeReference(this);
    }

    public Reference fromJava(Object object) {
        final TeleGrip teleGrip = (TeleGrip) gripScheme().fromJava(object);
        return teleGrip.makeReference(this);
    }

    public Object toJava(Reference reference) {
        final PlainTeleReference inspectorPlainReference = (PlainTeleReference) reference;
        return inspectorPlainReference.grip().toJava();
    }

    @INLINE
    public byte readByte(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readByte(offset);
    }

    @INLINE
    public byte readByte(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readByte(offset);
    }

    @INLINE
    public byte getByte(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getByte(displacement, index);
    }

    @INLINE
    public boolean readBoolean(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readBoolean(offset);
    }

    @INLINE
    public boolean readBoolean(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readBoolean(offset);
    }

    @INLINE
    public boolean getBoolean(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getBoolean(displacement, index);
    }

    @INLINE
    public short readShort(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readShort(offset);
    }

    @INLINE
    public short readShort(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readShort(offset);
    }

    @INLINE
    public short getShort(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getShort(displacement, index);
    }

    @INLINE
    public char readChar(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readChar(offset);
    }

    @INLINE
    public char readChar(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readChar(offset);
    }

    @INLINE
    public char getChar(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getChar(displacement, index);
    }

    @INLINE
    public int readInt(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readInt(offset);
    }

    @INLINE
    public int readInt(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readInt(offset);
    }

    @INLINE
    public int getInt(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getInt(displacement, index);
    }

    @INLINE
    public float readFloat(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readFloat(offset);
    }

    @INLINE
    public float readFloat(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readFloat(offset);
    }

    @INLINE
    public float getFloat(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getFloat(displacement, index);
    }

    @INLINE
    public long readLong(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readLong(offset);
    }

    @INLINE
    public long readLong(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readLong(offset);
    }

    @INLINE
    public long getLong(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getLong(displacement, index);
    }

    @INLINE
    public double readDouble(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readDouble(offset);
    }

    @INLINE
    public double readDouble(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readDouble(offset);
    }

    @INLINE
    public double getDouble(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getDouble(displacement, index);
    }

    @INLINE
    public Word readWord(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readWord(offset);
    }

    @INLINE
    public Word readWord(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readWord(offset);
    }

    @INLINE
    public Word getWord(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getWord(displacement, index);
    }

    @INLINE
    public Reference readReference(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readGrip(offset).toReference();
    }

    @INLINE
    public Reference readReference(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readGrip(offset).toReference();
    }

    @INLINE
    public Reference getReference(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getGrip(displacement, index).toReference();
    }

    @INLINE
    public void writeByte(Reference reference, Offset offset, byte value) {
        gripScheme().fromReference(reference).writeByte(offset, value);
    }

    @INLINE
    public void writeByte(Reference reference, int offset, byte value) {
        gripScheme().fromReference(reference).writeByte(offset, value);
    }

    @INLINE
    public void setByte(Reference reference, int displacement, int index, byte value) {
        gripScheme().fromReference(reference).setByte(displacement, index, value);
    }

    @INLINE
    public void writeBoolean(Reference reference, Offset offset, boolean value) {
        gripScheme().fromReference(reference).writeBoolean(offset, value);
    }

    @INLINE
    public void writeBoolean(Reference reference, int offset, boolean value) {
        gripScheme().fromReference(reference).writeBoolean(offset, value);
    }

    @INLINE
    public void setBoolean(Reference reference, int displacement, int index, boolean value) {
        gripScheme().fromReference(reference).setBoolean(displacement, index, value);
    }

    @INLINE
    public void writeShort(Reference reference, Offset offset, short value) {
        gripScheme().fromReference(reference).writeShort(offset, value);
    }

    @INLINE
    public void writeShort(Reference reference, int offset, short value) {
        gripScheme().fromReference(reference).writeShort(offset, value);
    }

    @INLINE
    public void setShort(Reference reference, int displacement, int index, short value) {
        gripScheme().fromReference(reference).setShort(displacement, index, value);
    }

    @INLINE
    public void writeChar(Reference reference, Offset offset, char value) {
        gripScheme().fromReference(reference).writeChar(offset, value);
    }

    @INLINE
    public void writeChar(Reference reference, int offset, char value) {
        gripScheme().fromReference(reference).writeChar(offset, value);
    }

    @INLINE
    public void setChar(Reference reference, int displacement, int index, char value) {
        gripScheme().fromReference(reference).setChar(displacement, index, value);
    }

    @INLINE
    public void writeInt(Reference reference, Offset offset, int value) {
        gripScheme().fromReference(reference).writeInt(offset, value);
    }

    @INLINE
    public void writeInt(Reference reference, int offset, int value) {
        gripScheme().fromReference(reference).writeInt(offset, value);
    }

    @INLINE
    public void setInt(Reference reference, int displacement, int index, int value) {
        gripScheme().fromReference(reference).setInt(displacement, index, value);
    }

    @INLINE
    public void writeFloat(Reference reference, Offset offset, float value) {
        gripScheme().fromReference(reference).writeFloat(offset, value);
    }

    @INLINE
    public void writeFloat(Reference reference, int offset, float value) {
        gripScheme().fromReference(reference).writeFloat(offset, value);
    }

    @INLINE
    public void setFloat(Reference reference, int displacement, int index, float value) {
        gripScheme().fromReference(reference).setFloat(displacement, index, value);
    }

    @INLINE
    public void writeLong(Reference reference, Offset offset, long value) {
        gripScheme().fromReference(reference).writeLong(offset, value);
    }

    @INLINE
    public void writeLong(Reference reference, int offset, long value) {
        gripScheme().fromReference(reference).writeLong(offset, value);
    }

    @INLINE
    public void setLong(Reference reference, int displacement, int index, long value) {
        gripScheme().fromReference(reference).setLong(displacement, index, value);
    }

    @INLINE
    public void writeDouble(Reference reference, Offset offset, double value) {
        gripScheme().fromReference(reference).writeDouble(offset, value);
    }

    @INLINE
    public void writeDouble(Reference reference, int offset, double value) {
        gripScheme().fromReference(reference).writeDouble(offset, value);
    }

    @INLINE
    public void setDouble(Reference reference, int displacement, int index, double value) {
        gripScheme().fromReference(reference).setDouble(displacement, index, value);
    }

    @INLINE
    public void writeWord(Reference reference, Offset offset, Word value) {
        gripScheme().fromReference(reference).writeWord(offset, value);
    }

    @INLINE
    public void writeWord(Reference reference, int offset, Word value) {
        gripScheme().fromReference(reference).writeWord(offset, value);
    }

    @INLINE
    public void setWord(Reference reference, int displacement, int index, Word value) {
        gripScheme().fromReference(reference).setWord(displacement, index, value);
    }

    @INLINE
    public void writeReference(Reference reference, Offset offset, Reference value) {
        gripScheme().fromReference(reference).writeGrip(offset, value.toGrip());
    }

    @INLINE
    public void writeReference(Reference reference, int offset, Reference value) {
        gripScheme().fromReference(reference).writeGrip(offset, value.toGrip());
    }

    @INLINE
    public void setReference(Reference reference, int displacement, int index, Reference value) {
        gripScheme().fromReference(reference).setGrip(displacement, index, value.toGrip());
    }

    @INLINE
    public int compareAndSwapInt(Reference reference, Offset offset, int suspectedValue, int newValue) {
        return gripScheme().fromReference(reference).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    @INLINE
    public int compareAndSwapInt(Reference reference, int offset, int suspectedValue, int newValue) {
        return gripScheme().fromReference(reference).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    @INLINE
    public Word compareAndSwapWord(Reference reference, Offset offset, Word suspectedValue, Word newValue) {
        return gripScheme().fromReference(reference).compareAndSwapWord(offset, suspectedValue, newValue);
    }

    @INLINE
    public Word compareAndSwapWord(Reference reference, int offset, Word suspectedValue, Word newValue) {
        return gripScheme().fromReference(reference).compareAndSwapWord(offset, suspectedValue, newValue);
    }

    @INLINE
    public Reference compareAndSwapReference(Reference reference, Offset offset, Reference suspectedValue, Reference newValue) {
        return gripScheme().fromReference(reference).compareAndSwapReference(offset, suspectedValue, newValue);
    }

    @INLINE
    public Reference compareAndSwapReference(Reference reference, int offset, Reference suspectedValue, Reference newValue) {
        return gripScheme().fromReference(reference).compareAndSwapReference(offset, suspectedValue, newValue);
    }

    public void performWriteBarrier(Reference reference) {
    }
}
