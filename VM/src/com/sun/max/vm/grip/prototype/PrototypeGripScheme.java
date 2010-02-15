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
package com.sun.max.vm.grip.prototype;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A pseudo grip scheme for limited unit testing on the prototype host, without bootstrapping the VM.
 *
 * @author Bernd Mathiske
 */
public final class PrototypeGripScheme extends AbstractVMScheme implements GripScheme {

    private final DataModel dataModel;
    private final WordWidth gripWidth;

    public PrototypeGripScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        dataModel = vmConfiguration.platform().processorKind.dataModel;
        gripWidth = dataModel.wordWidth;
    }

    public DataModel dataModel() {
        return dataModel;
    }

    public WordWidth gripWidth() {
        return dataModel.wordWidth;
    }

    public boolean isConstant() {
        return false;
    }

    public Grip fromJava(Object object) {
        return new PrototypeGrip(object);
    }

    public Object toJava(Grip grip) {
        final PrototypeGrip prototypeGrip = (PrototypeGrip) grip;
        return prototypeGrip.getObject();
    }

    public Grip fromReference(Reference reference) {
        return fromJava(reference.toJava());
    }

    public Grip fromOrigin(Pointer origin) {
        throw ProgramError.unexpected();
    }

    public Grip updateGrip(Grip grip, Pointer origin) {
        throw ProgramError.unexpected();
    }

    @INLINE
    public Grip zero() {
        return null;
    }

    public boolean isZero(Grip grip) {
        return toJava(grip) == null;
    }

    @INLINE
    public boolean isAllOnes(Grip grip) {
        throw ProgramError.unexpected();
    }

    public boolean equals(Grip grip1, Grip grip2) {
        return grip1.equals(grip2);
    }

    public boolean isMarked(Grip grip) {
        throw ProgramError.unexpected();
    }

    public Grip marked(Grip grip) {
        throw ProgramError.unexpected();
    }

    public Grip unmarked(Grip grip) {
        throw ProgramError.unexpected();
    }

    public Pointer toOrigin(Grip grip) {
        throw ProgramError.unexpected();
    }

    private void setValue(Grip grip, int displacement, int index, Object wordOrBoxedJavaValue) {
        final Object object = toJava(grip);
        final PrototypeObjectMirror mirror = new PrototypeObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;
        ProgramError.check(displacement == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid array displacement");
        final Value value = wordOrBoxedJavaValue instanceof Word ? WordValue.from((Word) wordOrBoxedJavaValue) : Value.fromBoxedJavaValue(wordOrBoxedJavaValue);
        mirror.writeElement(value.kind(), index, value);
    }

    private <T> T getValue(Grip grip, Class<T> type, int displacement, int index) {
        final Object object = toJava(grip);
        final PrototypeObjectMirror mirror = new PrototypeObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;
        ProgramError.check(displacement == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid array displacement");
        final Kind kind = Kind.fromJava(type);
        final Class<T> castType = null;
        return StaticLoophole.cast(castType, mirror.readElement(kind, index).asBoxedJavaValue());
    }

    private void writeValue(Grip grip, int offset, Object wordOrBoxedJavaValue) {
        final Object object = toJava(grip);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                WithoutAccessCheck.setStaticField(javaClass, fieldActor.name.toString(), wordOrBoxedJavaValue);
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not write field: " + fieldActor, throwable);
            }
        } else {
            final PrototypeObjectMirror mirror = new PrototypeObjectMirror(object);
            final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;

            final Value value = wordOrBoxedJavaValue instanceof Word ? WordValue.from((Word) wordOrBoxedJavaValue) : Value.fromBoxedJavaValue(wordOrBoxedJavaValue);
            specificLayout.writeValue(value.kind(), mirror, offset, value);
            return;
        }
    }

    private <T> T readValue(Grip grip, Class<T> type, int offset) {
        final Class<T> castType = null;
        final Object object = toJava(grip);

        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            try {
                return StaticLoophole.cast(castType, WithoutAccessCheck.getStaticField(staticTuple.classActor().toJava(), fieldActor.name.toString()));
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not read field: " + fieldActor, throwable);
            }
        }

        final PrototypeObjectMirror mirror = new PrototypeObjectMirror(object);
        final ClassActor classActor = mirror.classActor();
        final SpecificLayout specificLayout = classActor.dynamicHub().specificLayout;

        final Kind kind = Kind.fromJava(type);
        final Value value = specificLayout.readValue(kind, mirror, offset);
        return StaticLoophole.cast(castType, value.asBoxedJavaValue());
    }

    public byte readByte(Grip grip, Offset offset) {
        return readByte(grip, offset.toInt());
    }

    public byte readByte(Grip grip, int offset) {
        return readValue(grip, byte.class, offset).byteValue();
    }

    public byte getByte(Grip grip, int displacement, int index) {
        return getValue(grip, byte.class, displacement, index);
    }

    public boolean readBoolean(Grip grip, Offset offset) {
        return readBoolean(grip, offset.toInt());
    }

    public boolean readBoolean(Grip grip, int offset) {
        return readValue(grip, boolean.class, offset).booleanValue();
    }

    public boolean getBoolean(Grip grip, int displacement, int index) {
        return getValue(grip, boolean.class, displacement, index);
    }

    public short readShort(Grip grip, Offset offset) {
        return readShort(grip, offset.toInt());
    }

    public short readShort(Grip grip, int offset) {
        return readValue(grip, short.class, offset).shortValue();
    }

    public short getShort(Grip grip, int displacement, int index) {
        return getValue(grip, short.class, displacement, index);
    }

    public char readChar(Grip grip, Offset offset) {
        return readChar(grip, offset.toInt());
    }

    public char readChar(Grip grip, int offset) {
        return readValue(grip, char.class, offset).charValue();
    }

    public char getChar(Grip grip, int displacement, int index) {
        return getValue(grip, char.class, displacement, index);
    }

    public int readInt(Grip grip, Offset offset) {
        return readInt(grip, offset.toInt());
    }

    public int readInt(Grip grip, int offset) {
        return readValue(grip, int.class, offset).intValue();
    }

    public int getInt(Grip grip, int displacement, int index) {
        return getValue(grip, int.class, displacement, index);
    }

    public float readFloat(Grip grip, Offset offset) {
        return readFloat(grip, offset.toInt());
    }

    public float readFloat(Grip grip, int offset) {
        return readValue(grip, float.class, offset).floatValue();
    }

    public float getFloat(Grip grip, int displacement, int index) {
        return getValue(grip, float.class, displacement, index);
    }

    public long readLong(Grip grip, Offset offset) {
        return readLong(grip, offset.toInt());
    }

    public long readLong(Grip grip, int offset) {
        return readValue(grip, long.class, offset).longValue();
    }

    public long getLong(Grip grip, int displacement, int index) {
        return getValue(grip, long.class, displacement, index);
    }

    public double readDouble(Grip grip, Offset offset) {
        return readDouble(grip, offset.toInt());
    }

    public double readDouble(Grip grip, int offset) {
        return readValue(grip, double.class, offset).doubleValue();
    }

    public double getDouble(Grip grip, int displacement, int index) {
        return getValue(grip, double.class, displacement, index);
    }

    public Word readWord(Grip grip, Offset offset) {
        return readWord(grip, offset.toInt());
    }

    public Word readWord(Grip grip, int offset) {
        return readValue(grip, Word.class, offset);
    }

    public Word getWord(Grip grip, int displacement, int index) {
        return getValue(grip, Word.class, displacement, index);
    }

    public Grip readGrip(Grip grip, Offset offset) {
        return readGrip(grip, offset.toInt());
    }

    public Grip readGrip(Grip grip, int offset) {
        return fromJava(readValue(grip, Object.class, offset));
    }

    public Grip getGrip(Grip grip, int displacement, int index) {
        return fromJava(getValue(grip, Object.class, displacement, index));
    }

    public void writeByte(Grip grip, Offset offset, byte value) {
        writeByte(grip, offset.toInt(), value);
    }

    public void writeByte(Grip grip, int offset, byte value) {
        writeValue(grip, offset, Byte.valueOf(value));
    }

    public void setByte(Grip grip, int displacement, int index, byte value) {
        setValue(grip, displacement, index, Byte.valueOf(value));
    }

    public void writeBoolean(Grip grip, Offset offset, boolean value) {
        writeBoolean(grip, offset.toInt(), value);
    }

    public void writeBoolean(Grip grip, int offset, boolean value) {
        writeValue(grip, offset, Boolean.valueOf(value));
    }

    public void setBoolean(Grip grip, int displacement, int index, boolean value) {
        setValue(grip, displacement, index, Boolean.valueOf(value));
    }

    public void writeShort(Grip grip, Offset offset, short value) {
        writeShort(grip, offset.toInt(), value);
    }

    public void writeShort(Grip grip, int offset, short value) {
        writeValue(grip, offset, Short.valueOf(value));
    }

    public void setShort(Grip grip, int displacement, int index, short value) {
        setValue(grip, displacement, index, Short.valueOf(value));
    }

    public void writeChar(Grip grip, Offset offset, char value) {
        writeChar(grip, offset.toInt(), value);
    }

    public void writeChar(Grip grip, int offset, char value) {
        writeValue(grip, offset, Character.valueOf(value));
    }

    public void setChar(Grip grip, int displacement, int index, char value) {
        setValue(grip, displacement, index, Character.valueOf(value));
    }

    public void writeInt(Grip grip, Offset offset, int value) {
        writeInt(grip, offset.toInt(), value);
    }

    public void writeInt(Grip grip, int offset, int value) {
        writeValue(grip, offset, Integer.valueOf(value));
    }

    public void setInt(Grip grip, int displacement, int index, int value) {
        setValue(grip, displacement, index, Integer.valueOf(value));
    }

    public void writeFloat(Grip grip, Offset offset, float value) {
        writeFloat(grip, offset.toInt(), value);
    }

    public void writeFloat(Grip grip, int offset, float value) {
        writeValue(grip, offset, Float.valueOf(value));
    }

    public void setFloat(Grip grip, int displacement, int index, float value) {
        setValue(grip, displacement, index, Float.valueOf(value));
    }

    public void writeLong(Grip grip, Offset offset, long value) {
        writeLong(grip, offset.toInt(), value);
    }

    public void writeLong(Grip grip, int offset, long value) {
        writeValue(grip, offset, Long.valueOf(value));
    }

    public void setLong(Grip grip, int displacement, int index, long value) {
        setValue(grip, displacement, index, Long.valueOf(value));
    }

    public void writeDouble(Grip grip, Offset offset, double value) {
        writeDouble(grip, offset.toInt(), value);
    }

    public void writeDouble(Grip grip, int offset, double value) {
        writeValue(grip, offset, Double.valueOf(value));
    }

    public void setDouble(Grip grip, int displacement, int index, double value) {
        setValue(grip, displacement, index, Double.valueOf(value));
    }

    public void writeWord(Grip grip, Offset offset, Word value) {
        writeWord(grip, offset.toInt(), value);
    }

    public void writeWord(Grip grip, int offset, Word value) {
        writeValue(grip, offset, value);
    }

    public void setWord(Grip grip, int displacement, int index, Word value) {
        setValue(grip, displacement, index, new BoxedWord(value));
    }

    public void writeGrip(Grip grip, Offset offset, Grip value) {
        writeGrip(grip, offset.toInt(), value);
    }

    public void writeGrip(Grip grip, int offset, Grip value) {
        writeValue(grip, offset, value.toJava());
    }

    public void setGrip(Grip grip, int displacement, int index, Grip value) {
        setValue(grip, displacement, index, value.toJava());
    }

    public byte[] createPrototypeGrip(Address address) {
        throw ProgramError.unexpected();
    }

    public byte[] createPrototypeNullGrip() {
        throw ProgramError.unexpected();
    }

    public int compareAndSwapInt(Grip grip, Offset offset, int suspectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    public int compareAndSwapInt(Grip grip, int offset, int suspectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, suspectedValue, newValue);
    }

    public Word compareAndSwapWord(Grip grip, Offset offset, Word suspectedValue, Word newValue) {
        throw ProgramError.unexpected();
    }

    public Word compareAndSwapWord(Grip grip, int offset, Word suspectedValue, Word newValue) {
        throw ProgramError.unexpected();
    }

    public Reference compareAndSwapReference(Grip grip, Offset offset, Reference suspectedValue, Reference newValue) {
        throw ProgramError.unexpected();
    }

    public Reference compareAndSwapReference(Grip grip, int offset, Reference suspectedValue, Reference newValue) {
        throw ProgramError.unexpected();
    }

    public void copyElements(Grip src, int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        throw ProgramError.unexpected();
    }
}
