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
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * A pseudo grip scheme for limited unit testing on the prototype host, without bootstrapping the VM.
 *
 * @author Bernd Mathiske
 */
public final class PrototypeGripScheme extends AbstractVMScheme implements GripScheme {

    private final DataModel _dataModel;
    private final WordWidth _gripWidth;

    public PrototypeGripScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _dataModel = vmConfiguration.platform().processorKind().dataModel();
        _gripWidth = _dataModel.wordWidth();
    }

    public DataModel dataModel() {
        return _dataModel;
    }

    public WordWidth gripWidth() {
        return _dataModel.wordWidth();
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

    @INLINE
    public Grip fromWord(Word word) {
        throw new UnsupportedOperationException();
    }

    public Word toWord(Grip grip) {
        throw new UnsupportedOperationException();
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
        return UnsafeLoophole.gripToWord(grip).asPointer();
    }

    private Object readField(Grip grip, int offset) {
        final Object object = toJava(grip);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                return WithoutAccessCheck.getStaticField(javaClass, fieldActor.name().toString());
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not access static field: " + fieldActor.name(), throwable);
            }
        }
        final Class javaClass = object.getClass();
        final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(javaClass);
        final FieldActor fieldActor = tupleClassActor.findInstanceFieldActor(offset);
        try {
            return WithoutAccessCheck.getInstanceField(object, fieldActor.name().toString());
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not access field: " + fieldActor.name(), throwable);
        }
    }

    public byte readByte(Grip grip, Offset offset) {
        return readByte(grip, offset.toInt());
    }

    public byte readByte(Grip grip, int offset) {
        final Byte result = (Byte) readField(grip, offset);
        return result.byteValue();
    }

    public byte getByte(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final byte[] array = (byte[]) grip.toJava();
        return array[index];
    }

    public boolean readBoolean(Grip grip, Offset offset) {
        return readBoolean(grip, offset.toInt());
    }

    public boolean readBoolean(Grip grip, int offset) {
        final Boolean result = (Boolean) readField(grip, offset);
        return result.booleanValue();
    }

    public boolean getBoolean(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final boolean[] array = (boolean[]) grip.toJava();
        return array[index];
    }

    public short readShort(Grip grip, Offset offset) {
        return readShort(grip, offset.toInt());
    }

    public short readShort(Grip grip, int offset) {
        final Short result = (Short) readField(grip, offset);
        return result.shortValue();
    }

    public short getShort(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final short[] array = (short[]) grip.toJava();
        return array[index];
    }

    public char readChar(Grip grip, Offset offset) {
        return readChar(grip, offset.toInt());
    }

    public char readChar(Grip grip, int offset) {
        final Character result = (Character) readField(grip, offset);
        return result.charValue();
    }

    public char getChar(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final char[] array = (char[]) grip.toJava();
        return array[index];
    }

    public int readInt(Grip grip, Offset offset) {
        return readInt(grip, offset.toInt());
    }

    public int readInt(Grip grip, int offset) {
        final Integer result = (Integer) readField(grip, offset);
        return result.intValue();
    }

    public int getInt(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final int[] array = (int[]) grip.toJava();
        return array[index];
    }

    public float readFloat(Grip grip, Offset offset) {
        return readFloat(grip, offset.toInt());
    }

    public float readFloat(Grip grip, int offset) {
        final Float result = (Float) readField(grip, offset);
        return result.floatValue();
    }

    public float getFloat(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final float[] array = (float[]) grip.toJava();
        return array[index];
    }

    public long readLong(Grip grip, Offset offset) {
        return readLong(grip, offset.toInt());
    }

    public long readLong(Grip grip, int offset) {
        final Long result = (Long) readField(grip, offset);
        return result.longValue();
    }

    public long getLong(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final long[] array = (long[]) grip.toJava();
        return array[index];
    }

    public double readDouble(Grip grip, Offset offset) {
        return readDouble(grip, offset.toInt());
    }

    public double readDouble(Grip grip, int offset) {
        final Double result = (Double) readField(grip, offset);
        return result.doubleValue();
    }

    public double getDouble(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final double[] array = (double[]) grip.toJava();
        return array[index];
    }

    public Word readWord(Grip grip, Offset offset) {
        return readWord(grip, offset.toInt());
    }

    public Word readWord(Grip grip, int offset) {
        return (Word) readField(grip, offset);
    }

    public Word getWord(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final Word[] array = (Word[]) grip.toJava();
        return array[index];
    }

    public Grip readGrip(Grip grip, Offset offset) {
        return readGrip(grip, offset.toInt());
    }

    public Grip readGrip(Grip grip, int offset) {
        return fromJava(readField(grip, offset));
    }

    public Grip getGrip(Grip grip, int displacement, int index) {
        assert displacement == 0;
        final Object[] array = (Object[]) toJava(grip);
        return fromJava(array[index]);
    }

    private void writeField(Grip grip, int offset, Object value) {
        final Object object = toJava(grip);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                WithoutAccessCheck.setStaticField(javaClass, fieldActor.name().toString(), value);
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not access static field: " + fieldActor.name(), throwable);
            }
        } else {
            final Class javaClass = object.getClass();
            final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(javaClass);
            final FieldActor fieldActor = tupleClassActor.findInstanceFieldActor(offset);
            WithoutAccessCheck.setInstanceField(object, fieldActor.name().toString(), value);
        }
    }

    public void writeByte(Grip grip, Offset offset, byte value) {
        writeByte(grip, offset.toInt(), value);
    }

    public void writeByte(Grip grip, int offset, byte value) {
        writeField(grip, offset, new Byte(value));
    }

    public void setByte(Grip grip, int displacement, int index, byte value) {
        assert displacement == 0;
        final byte[] array = (byte[]) grip.toJava();
        array[index] = value;
    }

    public void writeBoolean(Grip grip, Offset offset, boolean value) {
        writeBoolean(grip, offset.toInt(), value);
    }

    public void writeBoolean(Grip grip, int offset, boolean value) {
        writeField(grip, offset, new Boolean(value));
    }

    public void setBoolean(Grip grip, int displacement, int index, boolean value) {
        assert displacement == 0;
        final boolean[] array = (boolean[]) grip.toJava();
        array[index] = value;
    }

    public void writeShort(Grip grip, Offset offset, short value) {
        writeShort(grip, offset.toInt(), value);
    }

    public void writeShort(Grip grip, int offset, short value) {
        writeField(grip, offset, new Short(value));
    }

    public void setShort(Grip grip, int displacement, int index, short value) {
        assert displacement == 0;
        final short[] array = (short[]) grip.toJava();
        array[index] = value;
    }

    public void writeChar(Grip grip, Offset offset, char value) {
        writeChar(grip, offset.toInt(), value);
    }

    public void writeChar(Grip grip, int offset, char value) {
        writeField(grip, offset, new Character(value));
    }

    public void setChar(Grip grip, int displacement, int index, char value) {
        assert displacement == 0;
        final char[] array = (char[]) grip.toJava();
        array[index] = value;
    }

    public void writeInt(Grip grip, Offset offset, int value) {
        writeInt(grip, offset.toInt(), value);
    }

    public void writeInt(Grip grip, int offset, int value) {
        writeField(grip, offset, new Integer(value));
    }

    public void setInt(Grip grip, int displacement, int index, int value) {
        assert displacement == 0;
        final int[] array = (int[]) grip.toJava();
        array[index] = value;
    }

    public void writeFloat(Grip grip, Offset offset, float value) {
        writeFloat(grip, offset.toInt(), value);
    }

    public void writeFloat(Grip grip, int offset, float value) {
        writeField(grip, offset, new Float(value));
    }

    public void setFloat(Grip grip, int displacement, int index, float value) {
        assert displacement == 0;
        final float[] array = (float[]) grip.toJava();
        array[index] = value;
    }

    public void writeLong(Grip grip, Offset offset, long value) {
        writeLong(grip, offset.toInt(), value);
    }

    public void writeLong(Grip grip, int offset, long value) {
        writeField(grip, offset, new Long(value));
    }

    public void setLong(Grip grip, int displacement, int index, long value) {
        assert displacement == 0;
        final long[] array = (long[]) grip.toJava();
        array[index] = value;
    }

    public void writeDouble(Grip grip, Offset offset, double value) {
        writeDouble(grip, offset.toInt(), value);
    }

    public void writeDouble(Grip grip, int offset, double value) {
        writeField(grip, offset, new Double(value));
    }

    public void setDouble(Grip grip, int displacement, int index, double value) {
        assert displacement == 0;
        final double[] array = (double[]) grip.toJava();
        array[index] = value;
    }

    public void writeWord(Grip grip, Offset offset, Word value) {
        writeWord(grip, offset.toInt(), value);
    }

    public void writeWord(Grip grip, int offset, Word value) {
        final BoxedWord boxedWord = new BoxedWord(value); // avoiding word/grip kind mismatch
        writeField(grip, offset, boxedWord);
    }

    public void setWord(Grip grip, int displacement, int index, Word value) {
        assert displacement == 0;
        final Word[] array = (Word[]) grip.toJava();
        WordArray.set(array, index, value);
    }

    public void writeGrip(Grip grip, Offset offset, Grip value) {
        writeGrip(grip, offset.toInt(), value);
    }

    public void writeGrip(Grip grip, int offset, Grip value) {
        writeField(grip, offset, value.toJava());
    }

    public void setGrip(Grip grip, int displacement, int index, Grip value) {
        assert displacement == 0;
        final Object[] array = (Object[]) toJava(grip);
        array[index] = value.toJava();
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
}
