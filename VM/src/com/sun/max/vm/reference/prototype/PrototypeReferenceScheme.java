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
package com.sun.max.vm.reference.prototype;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * A pseudo reference scheme for limited unit testing on the prototype host, without bootstrapping the VM.
 *
 * @author Bernd Mathiske
 */
public class PrototypeReferenceScheme extends AbstractVMScheme implements ReferenceScheme {

    private final GripScheme gripScheme;

    @FOLD
    public GripScheme gripScheme() {
        return gripScheme;
    }

    public PrototypeReferenceScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        gripScheme = vmConfiguration.gripScheme();
    }

    public Reference fromJava(Object object) {
        return new PrototypeReference(gripScheme.fromJava(object));
    }

    public Object toJava(Reference reference) {
        final PrototypeReference prototypeReference = (PrototypeReference) reference;
        return gripScheme.toJava(prototypeReference.grip());
    }

    public Reference fromGrip(Grip grip) {
        return new PrototypeReference(grip);
    }

    public byte readByte(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readByte(offset);
    }

    public byte readByte(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readByte(offset);
    }

    public byte getByte(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getByte(displacement, index);
    }

    public boolean readBoolean(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readBoolean(offset);
    }

    public boolean readBoolean(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readBoolean(offset);
    }

    public boolean getBoolean(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getBoolean(displacement, index);
    }

    public short readShort(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readShort(offset);
    }

    public short readShort(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readShort(offset);
    }

    public short getShort(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getShort(displacement, index);
    }

    public char readChar(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readChar(offset);
    }

    public char readChar(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readChar(offset);
    }

    public char getChar(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getChar(displacement, index);
    }

    public int readInt(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readInt(offset);
    }

    public int readInt(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readInt(offset);
    }

    public int getInt(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getInt(displacement, index);
    }

    public float readFloat(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readFloat(offset);
    }

    public float readFloat(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readFloat(offset);
    }

    public float getFloat(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getFloat(displacement, index);
    }

    public long readLong(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readLong(offset);
    }

    public long readLong(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readLong(offset);
    }

    public long getLong(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getLong(displacement, index);
    }

    public double readDouble(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readDouble(offset);
    }

    public double readDouble(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readDouble(offset);
    }

    public double getDouble(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getDouble(displacement, index);
    }

    public Word readWord(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readWord(offset);
    }

    public Word readWord(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readWord(offset);
    }

    public Word getWord(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getWord(displacement, index);
    }

    public Reference readReference(Reference reference, Offset offset) {
        return gripScheme().fromReference(reference).readGrip(offset).toReference();
    }

    public Reference readReference(Reference reference, int offset) {
        return gripScheme().fromReference(reference).readGrip(offset).toReference();
    }

    public Reference getReference(Reference reference, int displacement, int index) {
        return gripScheme().fromReference(reference).getGrip(displacement, index).toReference();
    }

    public void writeByte(Reference reference, Offset offset, byte value) {
        gripScheme().fromReference(reference).writeByte(offset, value);
    }

    public void writeByte(Reference reference, int offset, byte value) {
        gripScheme().fromReference(reference).writeByte(offset, value);
    }

    public void setByte(Reference reference, int displacement, int index, byte value) {
        gripScheme().fromReference(reference).setByte(displacement, index, value);
    }

    public void writeBoolean(Reference reference, Offset offset, boolean value) {
        gripScheme().fromReference(reference).writeBoolean(offset, value);
    }

    public void writeBoolean(Reference reference, int offset, boolean value) {
        gripScheme().fromReference(reference).writeBoolean(offset, value);
    }

    public void setBoolean(Reference reference, int displacement, int index, boolean value) {
        gripScheme().fromReference(reference).setBoolean(displacement, index, value);
    }

    public void writeShort(Reference reference, Offset offset, short value) {
        gripScheme().fromReference(reference).writeShort(offset, value);
    }

    public void writeShort(Reference reference, int offset, short value) {
        gripScheme().fromReference(reference).writeShort(offset, value);
    }

    public void setShort(Reference reference, int displacement, int index, short value) {
        gripScheme().fromReference(reference).setShort(displacement, index, value);
    }

    public void writeChar(Reference reference, Offset offset, char value) {
        gripScheme().fromReference(reference).writeChar(offset, value);
    }

    public void writeChar(Reference reference, int offset, char value) {
        gripScheme().fromReference(reference).writeChar(offset, value);
    }

    public void setChar(Reference reference, int displacement, int index, char value) {
        gripScheme().fromReference(reference).setChar(displacement, index, value);
    }

    public void writeInt(Reference reference, Offset offset, int value) {
        gripScheme().fromReference(reference).writeInt(offset, value);
    }

    public void writeInt(Reference reference, int offset, int value) {
        gripScheme().fromReference(reference).writeInt(offset, value);
    }

    public void setInt(Reference reference, int displacement, int index, int value) {
        gripScheme().fromReference(reference).setInt(displacement, index, value);
    }

    public void writeFloat(Reference reference, Offset offset, float value) {
        gripScheme().fromReference(reference).writeFloat(offset, value);
    }

    public void writeFloat(Reference reference, int offset, float value) {
        gripScheme().fromReference(reference).writeFloat(offset, value);
    }

    public void setFloat(Reference reference, int displacement, int index, float value) {
        gripScheme().fromReference(reference).setFloat(displacement, index, value);
    }

    public void writeLong(Reference reference, Offset offset, long value) {
        gripScheme().fromReference(reference).writeLong(offset, value);
    }

    public void writeLong(Reference reference, int offset, long value) {
        gripScheme().fromReference(reference).writeLong(offset, value);
    }

    public void setLong(Reference reference, int displacement, int index, long value) {
        gripScheme().fromReference(reference).setLong(displacement, index, value);
    }

    public void writeDouble(Reference reference, Offset offset, double value) {
        gripScheme().fromReference(reference).writeDouble(offset, value);
    }

    public void writeDouble(Reference reference, int offset, double value) {
        gripScheme().fromReference(reference).writeDouble(offset, value);
    }

    public void setDouble(Reference reference, int displacement, int index, double value) {
        gripScheme().fromReference(reference).setDouble(displacement, index, value);
    }

    public void writeWord(Reference reference, Offset offset, Word value) {
        gripScheme().fromReference(reference).writeWord(offset, value);
    }

    public void writeWord(Reference reference, int offset, Word value) {
        gripScheme().fromReference(reference).writeWord(offset, value);
    }

    public void setWord(Reference reference, int displacement, int index, Word value) {
        gripScheme().fromReference(reference).setWord(displacement, index, value);
    }

    public void writeReference(Reference reference, Offset offset, Reference value) {
        gripScheme().fromReference(reference).writeGrip(offset, value.toGrip());
    }

    public void writeReference(Reference reference, int offset, Reference value) {
        gripScheme().fromReference(reference).writeGrip(offset, value.toGrip());
    }

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

    public void copyElements(int displacement, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        gripScheme().fromReference(src).copyElements(displacement, srcIndex, dst, dstIndex, length);
    }
}
