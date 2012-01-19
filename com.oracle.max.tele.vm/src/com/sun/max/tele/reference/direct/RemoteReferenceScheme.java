/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.reference.direct;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.reference.LocalObjectRemoteReferenceManager.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;

/**
 * A specific implementation of the {@link ReferenceScheme} interface for remote
 * access.
 */
public final class RemoteReferenceScheme extends AbstractVMScheme implements ReferenceScheme {

    // TODO (mlvdv) Consider replacing all uses of "instanceof" here with a cast to {@link TeleReference},
    // followed by a call to {@link TeleReference#isLocal}.  Although cleaner, there may be some performance
    // issues; I'm not sure.

    private TeleVM vm;
    private DataAccess dataAccess;
    protected LocalObjectRemoteReferenceManager localTeleReferenceManager;

    // TODO (mlvdv) (pass in data access specifically)
    public void setContext(TeleVM vm) {
        this.vm = vm;
        this.localTeleReferenceManager = new LocalObjectRemoteReferenceManager(vm);
        this.dataAccess = vm.memoryIO().access();
        assert dataAccess != null;
    }

    public Pointer toOrigin(Reference ref) {
        if (ref.isZero()) {
            return Pointer.zero();
        }
        if (ref instanceof LocalObjectRemoteReference) {
            throw new UnsupportedOperationException();
        }
        final RemoteReference remoteRef = (RemoteReference) ref;
        return remoteRef.raw().asPointer();
    }

    public Reference fromOrigin(Pointer origin) {
        return vm.referenceManager().makeReference(origin);
    }

    public Reference fromJava(Object object) {
        return localTeleReferenceManager.make(object);
    }

    public Object toJava(Reference ref) {
        if (ref instanceof LocalObjectRemoteReference) {
            final LocalObjectRemoteReference inspectorLocalTeleRef = (LocalObjectRemoteReference) ref;
            return inspectorLocalTeleRef.object();
        }
        throw new UnsupportedOperationException();
    }

    public Reference zero() {
        return vm.referenceManager().zeroReference();
    }

    public boolean isZero(Reference ref) {
        return ref == vm.referenceManager().zeroReference();
    }

    @INLINE
    public boolean isAllOnes(Reference ref) {
        if (ref.isZero()) {
            return false;
        } else if (ref instanceof LocalObjectRemoteReference) {
            TeleError.unexpected();
        }
        return toOrigin(ref).isAllOnes();
    }

    public boolean equals(Reference ref1, Reference ref2) {
        return ref1.equals(ref2);
    }

    public boolean isMarked(Reference ref) {
        throw new UnsupportedOperationException();
    }

    public boolean isTagged(Reference ref) {
        throw new UnsupportedOperationException();
    }

    public Reference marked(Reference ref) {
        throw new UnsupportedOperationException();
    }

    public Reference unmarked(Reference ref) {
        throw new UnsupportedOperationException();
    }

    private Object readField(Reference ref, int offset) {
        final Object object = toJava(ref);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                return WithoutAccessCheck.getStaticField(javaClass, fieldActor.name.toString());
            } catch (Throwable throwable) {
                TeleError.unexpected("could not access static field: " + fieldActor.name, throwable);
            }
        }
        final Class javaClass = object.getClass();
        final ClassActor classActor = ClassActor.fromJava(javaClass);

        if (classActor.isArrayClass()) {
            return Array.getLength(object);
        }

        final FieldActor fieldActor = classActor.findInstanceFieldActor(offset);
        try {
            return WithoutAccessCheck.getInstanceField(object, fieldActor.name.toString());
        } catch (Throwable throwable) {
            throw TeleError.unexpected("could not access field: " + fieldActor.name, throwable);
        }
    }

    public byte readByte(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readByte(ref, offset.toInt());
        }

        return dataAccess.readByte(toOrigin(ref), offset);
    }

    public byte readByte(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Byte result = (Byte) readField(ref, offset);
            return result.byteValue();
        }

        return dataAccess.readByte(toOrigin(ref), offset);
    }

    public byte getByte(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final byte[] array = (byte[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getByte(toOrigin(ref), displacement, index);
    }

    public boolean readBoolean(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readBoolean(ref, offset.toInt());
        }

        return dataAccess.readBoolean(toOrigin(ref), offset);
    }

    public boolean readBoolean(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Boolean result = (Boolean) readField(ref, offset);
            return result.booleanValue();
        }

        return dataAccess.readBoolean(toOrigin(ref), offset);
    }

    public boolean getBoolean(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final boolean[] array = (boolean[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getBoolean(toOrigin(ref), displacement, index);
    }

    public short readShort(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readShort(ref, offset.toInt());
        }

        return dataAccess.readShort(toOrigin(ref), offset);
    }

    public short readShort(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Short result = (Short) readField(ref, offset);
            return result.shortValue();
        }

        return dataAccess.readShort(toOrigin(ref), offset);
    }

    public short getShort(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final short[] array = (short[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getShort(toOrigin(ref), displacement, index);
    }

    public char readChar(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readChar(ref, offset.toInt());
        }

        return dataAccess.readChar(toOrigin(ref), offset);
    }

    public char readChar(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Character result = (Character) readField(ref, offset);
            return result.charValue();
        }

        return dataAccess.readChar(toOrigin(ref), offset);
    }

    public char getChar(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final char[] array = (char[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getChar(toOrigin(ref), displacement, index);
    }

    public int readInt(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readInt(ref, offset.toInt());
        }

        return dataAccess.readInt(toOrigin(ref), offset);
    }

    public int readInt(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Integer result = (Integer) readField(ref, offset);
            return result.intValue();
        }

        return dataAccess.readInt(toOrigin(ref), offset);
    }

    public int getInt(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final int[] array = (int[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getInt(toOrigin(ref), displacement, index);
    }

    public float readFloat(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readFloat(ref, offset.toInt());
        }

        return dataAccess.readFloat(toOrigin(ref), offset);
    }

    public float readFloat(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Float result = (Float) readField(ref, offset);
            return result.floatValue();
        }

        return dataAccess.readFloat(toOrigin(ref), offset);
    }

    public float getFloat(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final float[] array = (float[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getFloat(toOrigin(ref), displacement, index);
    }

    public long readLong(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readLong(ref, offset.toInt());
        }

        return dataAccess.readLong(toOrigin(ref), offset);
    }

    public long readLong(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Long result = (Long) readField(ref, offset);
            return result.longValue();
        }

        return dataAccess.readLong(toOrigin(ref), offset);
    }

    public long getLong(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final long[] array = (long[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getLong(toOrigin(ref), displacement, index);
    }

    public double readDouble(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readDouble(ref, offset.toInt());
        }

        return dataAccess.readDouble(toOrigin(ref), offset);
    }

    public double readDouble(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Double result = (Double) readField(ref, offset);
            return result.doubleValue();
        }

        return dataAccess.readDouble(toOrigin(ref), offset);
    }

    public double getDouble(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final double[] array = (double[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getDouble(toOrigin(ref), displacement, index);
    }

    public Word readWord(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readWord(ref, offset.toInt());
        }

        return dataAccess.readWord(toOrigin(ref), offset);
    }

    public Word readWord(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return (Word) readField(ref, offset);
        }

        return dataAccess.readWord(toOrigin(ref), offset);
    }

    public Word getWord(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Word[] array = (Word[]) ref.toJava();
            return array[index];
        }

        return dataAccess.getWord(toOrigin(ref), displacement, index);
    }

    public Reference readReference(Reference ref, Offset offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return readReference(ref, offset.toInt());
        }

        return fromOrigin(readWord(ref, offset).asPointer());
    }

    public Reference readReference(Reference ref, int offset) {
        if (ref instanceof LocalObjectRemoteReference) {
            return fromJava(readField(ref, offset));
        }

        return fromOrigin(readWord(ref, offset).asPointer());
    }

    public Reference getReference(Reference ref, int displacement, int index) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Object[] array = (Object[]) toJava(ref);
            return fromJava(array[index]);
        }

        return fromOrigin(getWord(ref, displacement, index).asPointer());
    }

    private void writeField(Reference ref, int offset, Object value) {
        final Object object = toJava(ref);
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Class javaClass = staticTuple.classActor().toJava();
            try {
                WithoutAccessCheck.setStaticField(javaClass, fieldActor.name.toString(), value);
            } catch (Throwable throwable) {
                TeleError.unexpected("could not access static field: " + fieldActor.name, throwable);
            }
        } else {
            final Class javaClass = object.getClass();
            final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(javaClass);
            final FieldActor fieldActor = tupleClassActor.findInstanceFieldActor(offset);
            WithoutAccessCheck.setInstanceField(object, fieldActor.name.toString(), value);
        }
    }

    public void writeByte(Reference ref, Offset offset, byte value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeByte(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeByte(toOrigin(ref), offset, value);
    }

    public void writeByte(Reference ref, int offset, byte value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Byte(value));
            return;
        }

        dataAccess.writeByte(toOrigin(ref), offset, value);
    }

    public void setByte(Reference ref, int displacement, int index, byte value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final byte[] array = (byte[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setByte(toOrigin(ref), displacement, index, value);
    }

    public void writeBoolean(Reference ref, Offset offset, boolean value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeBoolean(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeBoolean(toOrigin(ref), offset, value);
    }

    public void writeBoolean(Reference ref, int offset, boolean value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Boolean(value));
            return;
        }

        dataAccess.writeBoolean(toOrigin(ref), offset, value);
    }

    public void setBoolean(Reference ref, int displacement, int index, boolean value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final boolean[] array = (boolean[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setBoolean(toOrigin(ref), displacement, index, value);
    }

    public void writeShort(Reference ref, Offset offset, short value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeShort(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeShort(toOrigin(ref), offset, value);
    }

    public void writeShort(Reference ref, int offset, short value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Short(value));
            return;
        }

        dataAccess.writeShort(toOrigin(ref), offset, value);
    }

    public void setShort(Reference ref, int displacement, int index, short value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final short[] array = (short[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setShort(toOrigin(ref), displacement, index, value);
    }

    public void writeChar(Reference ref, Offset offset, char value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeChar(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeChar(toOrigin(ref), offset, value);
    }

    public void writeChar(Reference ref, int offset, char value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Character(value));
            return;
        }

        dataAccess.writeChar(toOrigin(ref), offset, value);
    }

    public void setChar(Reference ref, int displacement, int index, char value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final char[] array = (char[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setChar(toOrigin(ref), displacement, index, value);
    }

    public void writeInt(Reference ref, Offset offset, int value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeInt(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeInt(toOrigin(ref), offset, value);
    }

    public void writeInt(Reference ref, int offset, int value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Integer(value));
            return;
        }

        dataAccess.writeInt(toOrigin(ref), offset, value);
    }

    public void setInt(Reference ref, int displacement, int index, int value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final int[] array = (int[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setInt(toOrigin(ref), displacement, index, value);
    }

    public void writeFloat(Reference ref, Offset offset, float value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeFloat(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeFloat(toOrigin(ref), offset, value);
    }

    public void writeFloat(Reference ref, int offset, float value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Float(value));
            return;
        }

        dataAccess.writeFloat(toOrigin(ref), offset, value);
    }

    public void setFloat(Reference ref, int displacement, int index, float value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final float[] array = (float[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setFloat(toOrigin(ref), displacement, index, value);
    }

    public void writeLong(Reference ref, Offset offset, long value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeLong(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeLong(toOrigin(ref), offset, value);
    }

    public void writeLong(Reference ref, int offset, long value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Long(value));
            return;
        }

        dataAccess.writeLong(toOrigin(ref), offset, value);
    }

    public void setLong(Reference ref, int displacement, int index, long value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final long[] array = (long[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setLong(toOrigin(ref), displacement, index, value);
    }

    public void writeDouble(Reference ref, Offset offset, double value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeDouble(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeDouble(toOrigin(ref), offset, value);
    }

    public void writeDouble(Reference ref, int offset, double value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, new Double(value));
            return;
        }

        dataAccess.writeDouble(toOrigin(ref), offset, value);
    }

    public void setDouble(Reference ref, int displacement, int index, double value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final double[] array = (double[]) ref.toJava();
            array[index] = value;
            return;
        }

        dataAccess.setDouble(toOrigin(ref), displacement, index, value);
    }

    public void writeWord(Reference ref, Offset offset, Word value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeWord(ref, offset.toInt(), value);
            return;
        }

        dataAccess.writeWord(toOrigin(ref), offset, value);
    }

    public void writeWord(Reference ref, int offset, Word value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final BoxedWord boxedWord = new BoxedWord(value); // avoiding word/ref kind mismatch
            writeField(ref, offset, boxedWord);
            return;
        }

        dataAccess.writeWord(toOrigin(ref), offset, value);
    }

    public void setWord(Reference ref, int displacement, int index, Word value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Word[] array = (Word[]) ref.toJava();
            WordArray.set(array, index, value);
            return;
        }

        dataAccess.setWord(toOrigin(ref), displacement, index, value);
    }

    public void writeReference(Reference ref, Offset offset, Reference value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeReference(ref, offset.toInt(), value);
            return;
        }

        writeWord(ref, offset, value.toOrigin());
    }

    public void writeReference(Reference ref, int offset, Reference value) {
        if (ref instanceof LocalObjectRemoteReference) {
            writeField(ref, offset, value.toJava());
            return;
        }

        writeWord(ref, offset, value.toOrigin());
    }

    public void setReference(Reference ref, int displacement, int index, Reference value) {
        if (ref instanceof LocalObjectRemoteReference) {
            final Object[] array = (Object[]) toJava(ref);
            array[index] = value.toJava();
            return;
        }

        setWord(ref, displacement, index, value.toOrigin());
    }

    public int compareAndSwapInt(Reference ref, Offset offset, int expectedValue, int newValue) {
        return toOrigin(ref).compareAndSwapInt(offset, expectedValue, newValue);
    }

    public int compareAndSwapInt(Reference ref, int offset, int expectedValue, int newValue) {
        return toOrigin(ref).compareAndSwapInt(offset, expectedValue, newValue);
    }

    public Word compareAndSwapWord(Reference ref, Offset offset, Word expectedValue, Word newValue) {
        TeleError.unimplemented();
        return Word.zero();
    }

    public Word compareAndSwapWord(Reference ref, int offset, Word expectedValue, Word newValue) {
        TeleError.unimplemented();
        return Word.zero();
    }

    public Reference compareAndSwapReference(Reference ref, Offset offset, Reference expectedValue, Reference newValue) {
        TeleError.unimplemented();
        return null;
    }

    public Reference compareAndSwapReference(Reference ref, int offset, Reference expectedValue, Reference newValue) {
        TeleError.unimplemented();
        return null;
    }

    public void copyElements(int displacement, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        if (src instanceof LocalObjectRemoteReference) {
            System.arraycopy(toJava(src), srcIndex, dst, dstIndex, length);
        } else {
            dataAccess.copyElements(toOrigin(src), displacement, srcIndex, dst, dstIndex, length);
        }
    }

    @Override
    public byte[] asBytes(Pointer origin) {
        throw TeleError.unimplemented();
    }

    @Override
    public byte[] nullAsBytes() {
        throw TeleError.unimplemented();
    }

}
