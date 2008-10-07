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
/*VCSID=eeeb6318-1573-4028-83bf-5e49aad0f4ea*/
package com.sun.max.vm.grip.brooks;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * Brooks grip scheme.
 *
 * @author Bernd Mathiske
 */
public class BrooksGripScheme extends AbstractVMScheme implements GripScheme {

    private final DataModel _dataModel;
    private final WordWidth _gripWidth;

    protected BrooksGripScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _dataModel = vmConfiguration.platform().processorKind().dataModel();
        _gripWidth = _dataModel.wordWidth();
    }

    public boolean isConstant() {
        return false;
    }

    public Grip fromOrigin(Pointer origin) {
        origin.writeWord(0, origin);
        return UnsafeLoophole.wordToGrip(origin);
    }

    public Grip updateGrip(Grip grip, Pointer origin) {
        origin.writeWord(0, origin);
        return UnsafeLoophole.wordToGrip(origin);
    }

    public Pointer toOrigin(Grip grip) {
        final Pointer forwardingHandle = UnsafeLoophole.gripToWord(grip).asPointer();
        return forwardingHandle.readWord(0).asPointer();
    }

    public final byte[] createPrototypeGrip(Address address) {
        return dataModel().toBytes(address);
    }

    public final byte[] createPrototypeNullGrip() {
        return dataModel().toBytes(Word.zero());
    }

    @INLINE
    public final DataModel dataModel() {
        return _dataModel;
    }

    @INLINE
    public final WordWidth gripWidth() {
        return _gripWidth;
    }

    @INLINE
    public final Grip fromJava(Object object) {
        return UnsafeLoophole.cast(object);
    }

    @INLINE
    public final Object toJava(Grip grip) {
        return grip;
    }

    @INLINE
    public final Grip fromReference(Reference reference) {
        return UnsafeLoophole.referenceToGrip(reference);
    }

    @INLINE
    public final Grip fromWord(Word word) {
        return UnsafeLoophole.wordToGrip(word);
    }

    public Word toWord(Grip grip) {
        return UnsafeLoophole.gripToWord(grip);
    }

    @INLINE
    public Grip zero() {
        return fromOrigin(Pointer.zero());
    }

    @INLINE
    public final boolean isZero(Grip grip) {
        return UnsafeLoophole.gripToWord(grip).isZero();
    }

    @INLINE
    public final boolean isAllOnes(Grip grip) {
        return UnsafeLoophole.gripToWord(grip).isAllOnes();
    }

    public boolean equals(Grip grip1, Grip grip2) {
        return toOrigin(grip1) == toOrigin(grip2);
    }

    @INLINE
    public final boolean isMarked(Grip grip) {
        return UnsafeLoophole.gripToWord(grip).asPointer().isBitSet(0);
    }

    @INLINE
    public final Grip marked(Grip grip) {
        Pointer pointer = UnsafeLoophole.gripToWord(grip).asPointer();
        pointer = pointer.bitSet(0);
        return UnsafeLoophole.wordToGrip(pointer);
    }

    @INLINE
    public final Grip unmarked(Grip grip) {
        final Pointer pointer = UnsafeLoophole.gripToWord(grip).asPointer();
        pointer.bitCleared(0);
        return UnsafeLoophole.wordToGrip(pointer);
    }

    @INLINE
    public final byte readByte(Grip grip, Offset offset) {
        return toOrigin(grip).readByte(offset);
    }

    @INLINE
    public final byte readByte(Grip grip, int offset) {
        return toOrigin(grip).readByte(offset);
    }

    @INLINE
    public final byte getByte(Grip grip, int displacement, int index) {
        return toOrigin(grip).getByte(displacement, index);
    }

    @INLINE
    public final boolean readBoolean(Grip grip, Offset offset) {
        return toOrigin(grip).readBoolean(offset);
    }

    @INLINE
    public final boolean readBoolean(Grip grip, int offset) {
        return toOrigin(grip).readBoolean(offset);
    }

    @INLINE
    public final boolean getBoolean(Grip grip, int displacement, int index) {
        return toOrigin(grip).getBoolean(displacement, index);
    }

    @INLINE
    public final short readShort(Grip grip, Offset offset) {
        return toOrigin(grip).readShort(offset);
    }

    @INLINE
    public final short readShort(Grip grip, int offset) {
        return toOrigin(grip).readShort(offset);
    }

    @INLINE
    public final short getShort(Grip grip, int displacement, int index) {
        return toOrigin(grip).getShort(displacement, index);
    }

    @INLINE
    public final char readChar(Grip grip, Offset offset) {
        return toOrigin(grip).readChar(offset);
    }

    @INLINE
    public final char readChar(Grip grip, int offset) {
        return toOrigin(grip).readChar(offset);
    }

    @INLINE
    public final char getChar(Grip grip, int displacement, int index) {
        return toOrigin(grip).getChar(displacement, index);
    }

    @INLINE
    public final int readInt(Grip grip, Offset offset) {
        return toOrigin(grip).readInt(offset);
    }

    @INLINE
    public final int readInt(Grip grip, int offset) {
        return toOrigin(grip).readInt(offset);
    }

    @INLINE
    public final int getInt(Grip grip, int displacement, int index) {
        return toOrigin(grip).getInt(displacement, index);
    }

    @INLINE
    public final float readFloat(Grip grip, Offset offset) {
        return toOrigin(grip).readFloat(offset);
    }

    @INLINE
    public final float readFloat(Grip grip, int offset) {
        return toOrigin(grip).readFloat(offset);
    }

    @INLINE
    public final float getFloat(Grip grip, int displacement, int index) {
        return toOrigin(grip).getFloat(displacement, index);
    }

    @INLINE
    public final long readLong(Grip grip, Offset offset) {
        return toOrigin(grip).readLong(offset);
    }

    @INLINE
    public final long readLong(Grip grip, int offset) {
        return toOrigin(grip).readLong(offset);
    }

    @INLINE
    public final long getLong(Grip grip, int displacement, int index) {
        return toOrigin(grip).getLong(displacement, index);
    }

    @INLINE
    public final double readDouble(Grip grip, Offset offset) {
        return toOrigin(grip).readDouble(offset);
    }

    @INLINE
    public final double readDouble(Grip grip, int offset) {
        return toOrigin(grip).readDouble(offset);
    }

    @INLINE
    public final double getDouble(Grip grip, int displacement, int index) {
        return toOrigin(grip).getDouble(displacement, index);
    }

    @INLINE
    public final Word readWord(Grip grip, Offset offset) {
        return toOrigin(grip).readWord(offset);
    }

    @INLINE
    public final Word readWord(Grip grip, int offset) {
        return toOrigin(grip).readWord(offset);
    }

    @INLINE
    public final Word getWord(Grip grip, int displacement, int index) {
        return toOrigin(grip).getWord(displacement, index);
    }

    @INLINE
    public final Grip readGrip(Grip grip, Offset offset) {
        return toOrigin(grip).readGrip(offset);
    }

    @INLINE
    public final Grip readGrip(Grip grip, int offset) {
        return toOrigin(grip).readGrip(offset);
    }

    @INLINE
    public final Grip getGrip(Grip grip, int displacement, int index) {
        return toOrigin(grip).getGrip(displacement, index);
    }

    @INLINE
    public final void writeByte(Grip grip, Offset offset, byte value) {
        toOrigin(grip).writeByte(offset, value);
    }

    @INLINE
    public final void writeByte(Grip grip, int offset, byte value) {
        toOrigin(grip).writeByte(offset, value);
    }

    @INLINE
    public final void setByte(Grip grip, int displacement, int index, byte value) {
        toOrigin(grip).setByte(displacement, index, value);
    }

    @INLINE
    public final void writeBoolean(Grip grip, Offset offset, boolean value) {
        toOrigin(grip).writeBoolean(offset, value);
    }

    @INLINE
    public final void writeBoolean(Grip grip, int offset, boolean value) {
        toOrigin(grip).writeBoolean(offset, value);
    }

    @INLINE
    public final void setBoolean(Grip grip, int displacement, int index, boolean value) {
        toOrigin(grip).setBoolean(displacement, index, value);
    }

    @INLINE
    public final void writeShort(Grip grip, Offset offset, short value) {
        toOrigin(grip).writeShort(offset, value);
    }

    @INLINE
    public final void writeShort(Grip grip, int offset, short value) {
        toOrigin(grip).writeShort(offset, value);
    }

    @INLINE
    public final void setShort(Grip grip, int displacement, int index, short value) {
        toOrigin(grip).setShort(displacement, index, value);
    }

    @INLINE
    public final void writeChar(Grip grip, Offset offset, char value) {
        toOrigin(grip).writeChar(offset, value);
    }

    @INLINE
    public final void writeChar(Grip grip, int offset, char value) {
        toOrigin(grip).writeChar(offset, value);
    }

    @INLINE
    public final void setChar(Grip grip, int displacement, int index, char value) {
        toOrigin(grip).setChar(displacement, index, value);
    }

    @INLINE
    public final void writeInt(Grip grip, Offset offset, int value) {
        toOrigin(grip).writeInt(offset, value);
    }

    @INLINE
    public final void writeInt(Grip grip, int offset, int value) {
        toOrigin(grip).writeInt(offset, value);
    }

    @INLINE
    public final void setInt(Grip grip, int displacement, int index, int value) {
        toOrigin(grip).setInt(displacement, index, value);
    }

    @INLINE
    public final void writeFloat(Grip grip, Offset offset, float value) {
        toOrigin(grip).writeFloat(offset, value);
    }

    @INLINE
    public final void writeFloat(Grip grip, int offset, float value) {
        toOrigin(grip).writeFloat(offset, value);
    }

    @INLINE
    public final void setFloat(Grip grip, int displacement, int index, float value) {
        toOrigin(grip).setFloat(displacement, index, value);
    }

    @INLINE
    public final void writeLong(Grip grip, Offset offset, long value) {
        toOrigin(grip).writeLong(offset, value);
    }

    @INLINE
    public final void writeLong(Grip grip, int offset, long value) {
        toOrigin(grip).writeLong(offset, value);
    }

    @INLINE
    public final void setLong(Grip grip, int displacement, int index, long value) {
        toOrigin(grip).setLong(displacement, index, value);
    }

    @INLINE
    public final void writeDouble(Grip grip, Offset offset, double value) {
        toOrigin(grip).writeDouble(offset, value);
    }

    @INLINE
    public final void writeDouble(Grip grip, int offset, double value) {
        toOrigin(grip).writeDouble(offset, value);
    }

    @INLINE
    public final void setDouble(Grip grip, int displacement, int index, double value) {
        toOrigin(grip).setDouble(displacement, index, value);
    }

    @INLINE
    public final void writeWord(Grip grip, Offset offset, Word value) {
        toOrigin(grip).writeWord(offset, value);
    }

    @INLINE
    public final void writeWord(Grip grip, int offset, Word value) {
        toOrigin(grip).writeWord(offset, value);
    }

    @INLINE
    public final void setWord(Grip grip, int displacement, int index, Word value) {
        toOrigin(grip).setWord(displacement, index, value);
    }

    @INLINE
    public final void writeGrip(Grip grip, Offset offset, Grip value) {
        toOrigin(grip).writeGrip(offset, value);
    }

    @INLINE
    public final void writeGrip(Grip grip, int offset, Grip value) {
        toOrigin(grip).writeGrip(offset, value);
    }

    @INLINE
    public final void setGrip(Grip grip, int displacement, int index, Grip value) {
        toOrigin(grip).setGrip(displacement, index, value);
    }

    @INLINE
    public final int compareAndSwapInt(Grip grip, Offset offset, int suspectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    @INLINE
    public final int compareAndSwapInt(Grip grip, int offset, int suspectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    @INLINE
    public final Word compareAndSwapWord(Grip grip, Offset offset, Word suspectedValue, Word newValue) {
        return toOrigin(grip).compareAndSwapWord(offset, suspectedValue, newValue);
    }

    @INLINE
    public final Word compareAndSwapWord(Grip grip, int offset, Word suspectedValue, Word newValue) {
        return toOrigin(grip).compareAndSwapWord(offset, suspectedValue, newValue);
    }

    @INLINE
    public final Reference compareAndSwapReference(Grip grip, Offset offset, Reference suspectedValue, Reference newValue) {
        return toOrigin(grip).compareAndSwapReference(offset, suspectedValue, newValue);
    }

    @INLINE
    public final Reference compareAndSwapReference(Grip grip, int offset, Reference suspectedValue, Reference newValue) {
        return toOrigin(grip).compareAndSwapReference(offset, suspectedValue, newValue);
    }

}
