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
package com.sun.max.tele.grip.direct;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.grip.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class DirectInspectorGripScheme extends TeleGripScheme {

    private DataModel dataModel;

    public DataModel dataModel() {
        return dataModel;
    }

    private WordWidth gripWidth;

    public WordWidth gripWidth() {
        return gripWidth;
    }

    public DirectInspectorGripScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        dataModel = vmConfiguration.platform().processorKind.dataModel;
        gripWidth = dataModel.wordWidth;
    }

    public boolean isConstant() {
        return false;
    }

    public Pointer toOrigin(Grip grip) {
        if (grip.isZero()) {
            return Pointer.zero();
        }
        if (grip instanceof LocalTeleGrip) {
            throw new UnsupportedOperationException();
        }
        final RemoteTeleGrip remoteTeleGrip = (RemoteTeleGrip) grip;
        return remoteTeleGrip.raw().asPointer();
    }

    public Grip fromOrigin(Pointer origin) {
        return makeTeleGrip(origin);
    }

    @Override
    public RemoteTeleGrip temporaryRemoteTeleGripFromOrigin(Word origin) {
        return createTemporaryRemoteTeleGrip(origin.asAddress());
    }

    public Grip fromReference(Reference reference) {
        final TeleReference teleReference = (TeleReference) reference;
        return teleReference.grip();
    }

    public Grip fromJava(Object object) {
        return makeLocalGrip(object);
    }

    public Object toJava(Grip grip) {
        if (grip instanceof LocalTeleGrip) {
            final LocalTeleGrip inspectorLocalTeleGrip = (LocalTeleGrip) grip;
            return inspectorLocalTeleGrip.object();
        }
        throw new UnsupportedOperationException();
    }

    public Grip updateGrip(Grip grip, Pointer origin) {
        return fromOrigin(origin);
    }

    public byte[] createPrototypeGrip(Address address) {
        throw new UnsupportedOperationException();
    }

    public byte[] createPrototypeNullGrip() {
        throw new UnsupportedOperationException();
    }

    public Grip zero() {
        return TeleGrip.ZERO;
    }

    public boolean isZero(Grip grip) {
        return grip == TeleGrip.ZERO;
    }

    @INLINE
    public boolean isAllOnes(Grip grip) {
        if (grip.isZero()) {
            return false;
        } else if (grip instanceof LocalTeleGrip) {
            ProgramError.unexpected();
        }
        return toOrigin(grip).isAllOnes();
    }

    public boolean equals(Grip grip1, Grip grip2) {
        return grip1.equals(grip2);
    }

    public boolean isMarked(Grip grip) {
        throw new UnsupportedOperationException();
    }

    public Grip marked(Grip grip) {
        throw new UnsupportedOperationException();
    }

    public Grip unmarked(Grip grip) {
        throw new UnsupportedOperationException();
    }

    private Object readField(Grip grip, int offset) {
        final Object object = toJava(grip);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                return WithoutAccessCheck.getStaticField(javaClass, fieldActor.name.toString());
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not access static field: " + fieldActor.name, throwable);
            }
        }
        final Class javaClass = object.getClass();
        final ClassActor classActor = ClassActor.fromJava(javaClass);

        if (classActor.isArrayClassActor()) {
            return Array.getLength(object);
        }

        final FieldActor fieldActor = classActor.findInstanceFieldActor(offset);
        try {
            return WithoutAccessCheck.getInstanceField(object, fieldActor.name.toString());
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not access field: " + fieldActor.name, throwable);
        }
    }

    public byte readByte(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readByte(grip, offset.toInt());
        }

        return teleVM().dataAccess().readByte(toOrigin(grip), offset);
    }

    public byte readByte(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Byte result = (Byte) readField(grip, offset);
            return result.byteValue();
        }

        return teleVM().dataAccess().readByte(toOrigin(grip), offset);
    }

    public byte getByte(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final byte[] array = (byte[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getByte(toOrigin(grip), displacement, index);
    }

    public boolean readBoolean(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readBoolean(grip, offset.toInt());
        }

        return teleVM().dataAccess().readBoolean(toOrigin(grip), offset);
    }

    public boolean readBoolean(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Boolean result = (Boolean) readField(grip, offset);
            return result.booleanValue();
        }

        return teleVM().dataAccess().readBoolean(toOrigin(grip), offset);
    }

    public boolean getBoolean(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final boolean[] array = (boolean[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getBoolean(toOrigin(grip), displacement, index);
    }

    public short readShort(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readShort(grip, offset.toInt());
        }

        return teleVM().dataAccess().readShort(toOrigin(grip), offset);
    }

    public short readShort(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Short result = (Short) readField(grip, offset);
            return result.shortValue();
        }

        return teleVM().dataAccess().readShort(toOrigin(grip), offset);
    }

    public short getShort(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final short[] array = (short[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getShort(toOrigin(grip), displacement, index);
    }

    public char readChar(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readChar(grip, offset.toInt());
        }

        return teleVM().dataAccess().readChar(toOrigin(grip), offset);
    }

    public char readChar(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Character result = (Character) readField(grip, offset);
            return result.charValue();
        }

        return teleVM().dataAccess().readChar(toOrigin(grip), offset);
    }

    public char getChar(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final char[] array = (char[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getChar(toOrigin(grip), displacement, index);
    }

    public int readInt(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readInt(grip, offset.toInt());
        }

        return teleVM().dataAccess().readInt(toOrigin(grip), offset);
    }

    public int readInt(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Integer result = (Integer) readField(grip, offset);
            return result.intValue();
        }

        return teleVM().dataAccess().readInt(toOrigin(grip), offset);
    }

    public int getInt(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final int[] array = (int[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getInt(toOrigin(grip), displacement, index);
    }

    public float readFloat(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readFloat(grip, offset.toInt());
        }

        return teleVM().dataAccess().readFloat(toOrigin(grip), offset);
    }

    public float readFloat(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Float result = (Float) readField(grip, offset);
            return result.floatValue();
        }

        return teleVM().dataAccess().readFloat(toOrigin(grip), offset);
    }

    public float getFloat(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final float[] array = (float[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getFloat(toOrigin(grip), displacement, index);
    }

    public long readLong(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readLong(grip, offset.toInt());
        }

        return teleVM().dataAccess().readLong(toOrigin(grip), offset);
    }

    public long readLong(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Long result = (Long) readField(grip, offset);
            return result.longValue();
        }

        return teleVM().dataAccess().readLong(toOrigin(grip), offset);
    }

    public long getLong(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final long[] array = (long[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getLong(toOrigin(grip), displacement, index);
    }

    public double readDouble(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readDouble(grip, offset.toInt());
        }

        return teleVM().dataAccess().readDouble(toOrigin(grip), offset);
    }

    public double readDouble(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            final Double result = (Double) readField(grip, offset);
            return result.doubleValue();
        }

        return teleVM().dataAccess().readDouble(toOrigin(grip), offset);
    }

    public double getDouble(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final double[] array = (double[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getDouble(toOrigin(grip), displacement, index);
    }

    public Word readWord(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readWord(grip, offset.toInt());
        }

        return teleVM().dataAccess().readWord(toOrigin(grip), offset);
    }

    public Word readWord(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            return (Word) readField(grip, offset);
        }

        return teleVM().dataAccess().readWord(toOrigin(grip), offset);
    }

    public Word getWord(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final Word[] array = (Word[]) grip.toJava();
            return array[index];
        }

        return teleVM().dataAccess().getWord(toOrigin(grip), displacement, index);
    }

    public Grip readGrip(Grip grip, Offset offset) {
        if (grip instanceof LocalTeleGrip) {
            return readGrip(grip, offset.toInt());
        }

        return fromOrigin(readWord(grip, offset).asPointer());
    }

    public Grip readGrip(Grip grip, int offset) {
        if (grip instanceof LocalTeleGrip) {
            return fromJava(readField(grip, offset));
        }

        return fromOrigin(readWord(grip, offset).asPointer());
    }

    public Grip getGrip(Grip grip, int displacement, int index) {
        if (grip instanceof LocalTeleGrip) {
            final Object[] array = (Object[]) toJava(grip);
            return fromJava(array[index]);
        }

        return fromOrigin(getWord(grip, displacement, index).asPointer());
    }

    private void writeField(Grip grip, int offset, Object value) {
        final Object object = toJava(grip);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                WithoutAccessCheck.setStaticField(javaClass, fieldActor.name.toString(), value);
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not access static field: " + fieldActor.name, throwable);
            }
        } else {
            final Class javaClass = object.getClass();
            final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(javaClass);
            final FieldActor fieldActor = tupleClassActor.findInstanceFieldActor(offset);
            WithoutAccessCheck.setInstanceField(object, fieldActor.name.toString(), value);
        }
    }

    public void writeByte(Grip grip, Offset offset, byte value) {
        if (grip instanceof LocalTeleGrip) {
            writeByte(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeByte(toOrigin(grip), offset, value);
    }

    public void writeByte(Grip grip, int offset, byte value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Byte(value));
            return;
        }

        teleVM().dataAccess().writeByte(toOrigin(grip), offset, value);
    }

    public void setByte(Grip grip, int displacement, int index, byte value) {
        if (grip instanceof LocalTeleGrip) {
            final byte[] array = (byte[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setByte(toOrigin(grip), displacement, index, value);
    }

    public void writeBoolean(Grip grip, Offset offset, boolean value) {
        if (grip instanceof LocalTeleGrip) {
            writeBoolean(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeBoolean(toOrigin(grip), offset, value);
    }

    public void writeBoolean(Grip grip, int offset, boolean value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Boolean(value));
            return;
        }

        teleVM().dataAccess().writeBoolean(toOrigin(grip), offset, value);
    }

    public void setBoolean(Grip grip, int displacement, int index, boolean value) {
        if (grip instanceof LocalTeleGrip) {
            final boolean[] array = (boolean[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setBoolean(toOrigin(grip), displacement, index, value);
    }

    public void writeShort(Grip grip, Offset offset, short value) {
        if (grip instanceof LocalTeleGrip) {
            writeShort(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeShort(toOrigin(grip), offset, value);
    }

    public void writeShort(Grip grip, int offset, short value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Short(value));
            return;
        }

        teleVM().dataAccess().writeShort(toOrigin(grip), offset, value);
    }

    public void setShort(Grip grip, int displacement, int index, short value) {
        if (grip instanceof LocalTeleGrip) {
            final short[] array = (short[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setShort(toOrigin(grip), displacement, index, value);
    }

    public void writeChar(Grip grip, Offset offset, char value) {
        if (grip instanceof LocalTeleGrip) {
            writeChar(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeChar(toOrigin(grip), offset, value);
    }

    public void writeChar(Grip grip, int offset, char value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Character(value));
            return;
        }

        teleVM().dataAccess().writeChar(toOrigin(grip), offset, value);
    }

    public void setChar(Grip grip, int displacement, int index, char value) {
        if (grip instanceof LocalTeleGrip) {
            final char[] array = (char[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setChar(toOrigin(grip), displacement, index, value);
    }

    public void writeInt(Grip grip, Offset offset, int value) {
        if (grip instanceof LocalTeleGrip) {
            writeInt(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeInt(toOrigin(grip), offset, value);
    }

    public void writeInt(Grip grip, int offset, int value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Integer(value));
            return;
        }

        teleVM().dataAccess().writeInt(toOrigin(grip), offset, value);
    }

    public void setInt(Grip grip, int displacement, int index, int value) {
        if (grip instanceof LocalTeleGrip) {
            final int[] array = (int[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setInt(toOrigin(grip), displacement, index, value);
    }

    public void writeFloat(Grip grip, Offset offset, float value) {
        if (grip instanceof LocalTeleGrip) {
            writeFloat(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeFloat(toOrigin(grip), offset, value);
    }

    public void writeFloat(Grip grip, int offset, float value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Float(value));
            return;
        }

        teleVM().dataAccess().writeFloat(toOrigin(grip), offset, value);
    }

    public void setFloat(Grip grip, int displacement, int index, float value) {
        if (grip instanceof LocalTeleGrip) {
            final float[] array = (float[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setFloat(toOrigin(grip), displacement, index, value);
    }

    public void writeLong(Grip grip, Offset offset, long value) {
        if (grip instanceof LocalTeleGrip) {
            writeLong(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeLong(toOrigin(grip), offset, value);
    }

    public void writeLong(Grip grip, int offset, long value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Long(value));
            return;
        }

        teleVM().dataAccess().writeLong(toOrigin(grip), offset, value);
    }

    public void setLong(Grip grip, int displacement, int index, long value) {
        if (grip instanceof LocalTeleGrip) {
            final long[] array = (long[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setLong(toOrigin(grip), displacement, index, value);
    }

    public void writeDouble(Grip grip, Offset offset, double value) {
        if (grip instanceof LocalTeleGrip) {
            writeDouble(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeDouble(toOrigin(grip), offset, value);
    }

    public void writeDouble(Grip grip, int offset, double value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, new Double(value));
            return;
        }

        teleVM().dataAccess().writeDouble(toOrigin(grip), offset, value);
    }

    public void setDouble(Grip grip, int displacement, int index, double value) {
        if (grip instanceof LocalTeleGrip) {
            final double[] array = (double[]) grip.toJava();
            array[index] = value;
            return;
        }

        teleVM().dataAccess().setDouble(toOrigin(grip), displacement, index, value);
    }

    public void writeWord(Grip grip, Offset offset, Word value) {
        if (grip instanceof LocalTeleGrip) {
            writeWord(grip, offset.toInt(), value);
            return;
        }

        teleVM().dataAccess().writeWord(toOrigin(grip), offset, value);
    }

    public void writeWord(Grip grip, int offset, Word value) {
        if (grip instanceof LocalTeleGrip) {
            final BoxedWord boxedWord = new BoxedWord(value); // avoiding word/grip kind mismatch
            writeField(grip, offset, boxedWord);
            return;
        }

        teleVM().dataAccess().writeWord(toOrigin(grip), offset, value);
    }

    public void setWord(Grip grip, int displacement, int index, Word value) {
        if (grip instanceof LocalTeleGrip) {
            final Word[] array = (Word[]) grip.toJava();
            WordArray.set(array, index, value);
            return;
        }

        teleVM().dataAccess().setWord(toOrigin(grip), displacement, index, value);
    }

    public void writeGrip(Grip grip, Offset offset, Grip value) {
        if (grip instanceof LocalTeleGrip) {
            writeGrip(grip, offset.toInt(), value);
            return;
        }

        writeWord(grip, offset, value.toOrigin());
    }

    public void writeGrip(Grip grip, int offset, Grip value) {
        if (grip instanceof LocalTeleGrip) {
            writeField(grip, offset, value.toJava());
            return;
        }

        writeWord(grip, offset, value.toOrigin());
    }

    public void setGrip(Grip grip, int displacement, int index, Grip value) {
        if (grip instanceof LocalTeleGrip) {
            final Object[] array = (Object[]) toJava(grip);
            array[index] = value.toJava();
            return;
        }

        setWord(grip, displacement, index, value.toOrigin());
    }

    public int compareAndSwapInt(Grip grip, Offset offset, int expectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, expectedValue, newValue);
    }

    public int compareAndSwapInt(Grip grip, int offset, int expectedValue, int newValue) {
        return toOrigin(grip).compareAndSwapInt(offset, expectedValue, newValue);
    }

    public Word compareAndSwapWord(Grip grip, Offset offset, Word expectedValue, Word newValue) {
        FatalError.unimplemented();
        return Word.zero();
    }

    public Word compareAndSwapWord(Grip grip, int offset, Word expectedValue, Word newValue) {
        FatalError.unimplemented();
        return Word.zero();
    }

    public Reference compareAndSwapReference(Grip grip, Offset offset, Reference expectedValue, Reference newValue) {
        FatalError.unimplemented();
        return null;
    }

    public Reference compareAndSwapReference(Grip grip, int offset, Reference expectedValue, Reference newValue) {
        FatalError.unimplemented();
        return null;
    }

    public void copyElements(Grip src, int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        if (src instanceof LocalTeleGrip) {
            System.arraycopy(toJava(src), srcIndex, dst, dstIndex, length);
        } else {
            teleVM().dataAccess().copyElements(toOrigin(src), displacement, srcIndex, dst, dstIndex, length);
        }
    }
}
