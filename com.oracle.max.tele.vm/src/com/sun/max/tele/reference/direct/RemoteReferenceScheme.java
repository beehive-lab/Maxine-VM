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
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.reference.LocalObjectRemoteReferenceManager.LocalObjectRemoteReference;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.reference.direct.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A specific implementation of the {@link ReferenceScheme} interface for remote access to objects in the VM. It
 * presumes that the VM is built with the {@link DirectReferenceScheme}.
 * <p>
 * This implementation is designed to work in the Inspection environment where all of the references are instances of
 * {@link RemoteReference}, an extension of {@link Reference} that in most cases encapsulates the current address of an
 * object origin in VM memory. There are exceptions:
 * <ul>
 * <li>{@code null} references are implemented as instances of {@link NullReference} that hold {@link Address#zero()}
 * and may in some situations hold extra information useful for debugging the Inspector.</li>
 * <li>Instances of {@link LocalObjectRemoteReference}, which allows a local object in the Inspector to masquerade as a
 * remote object, used mainly in the Inspector's {@link TeleInterpreter}.</li>
 * </ul>
 * <p>
 * Substituting this implementation of {@link ReferenceScheme} in the Inspector's emulation of the VM's static environment
 * allows reuse of considerable VM code, notably in the {@link Layout} and {@link Reference} classes.  Whenever methods in
 * those (and related) classes are used, for example {@link Reference#readReference(int)},
 * operations concerning {@link Reference}s are routed through this scheme implementation.
 * <p>
 * <strong>Important:</strong> the default behavior of most methods routed through this scheme implementation, in particular those
 * with return type {@link Reference} is to:
 * <ul>
 * <li>return only instances of {@link RemoteReference}; and</li>
 * <li>return a {@code null} reference ({@code isZero() == true}) if no {@link ObjectStatus#LIVE} object can be detected
 * at the encapsulated VM memory address.</li>
 * </ul>
 *
 * <p>
 * TODO (mlvdv) This should be generalized to work with other {@link ReferenceScheme} implementations.
 *
 * @see RemoteReference
 * @see VmReferenceManager
 */
public final class RemoteReferenceScheme extends AbstractVMScheme implements ReferenceScheme {

    // TODO (mlvdv) generalize to support ReferenceScheme implementations other than DirectRefer
    /**
     * The default implementation of {@link Reference#zero()}, used as a {@code null} remote reference. It always holds
     * the {@linkplain Address#zero() null address} and always has status {@linkplain ObjectStatus#DEAD DEAD}.
     */
    private static class NullReference extends ConstantRemoteReference {

        NullReference(TeleVM vm) {
            super(vm, Address.zero());
        }

        @Override
        public final ObjectStatus status() {
            return ObjectStatus.DEAD;
        }

        @Override
        public ObjectStatus priorStatus() {
            return null;
        }

        @Override
        public String toString() {
            return "null Remote Reference";
        }
    }

    /**
     * A specialized implementation of {@link Reference#zero()}, used as a {@code null} remote reference. It carries
     * with it the history of why it was created: a textual explanation, along with the location in memory where the
     * creation of a remote reference was attempted and whose failure resulted in creation of this null reference.
     */
    private static final class AnnotatedNullReference extends NullReference {

        private final String description;
        private final Address failedOrigin;

        AnnotatedNullReference(TeleVM vm, String description, Address failedOrigin) {
            super(vm);
            this.description = description;
            this.failedOrigin = failedOrigin;
        }

        @Override
        public String toString() {
            return "ZeroRef: " + description + failedOrigin.to0xHexString();
        }
    }

    private TeleVM vm;
    private DataAccess dataAccess;
    private RemoteReference zero;

    protected LocalObjectRemoteReferenceManager localTeleReferenceManager;

    // TODO (mlvdv) (pass in data access specifically)
    public void setContext(TeleVM vm) {
        this.vm = vm;
        this.localTeleReferenceManager = new LocalObjectRemoteReferenceManager(vm);
        this.dataAccess = vm.memoryIO().access();
        this.zero = new NullReference(vm);
        assert dataAccess != null;
    }

    /**
     * Creates a null reference (implementation of {@link Reference#zero()} that carries with it the history
     * of a failed attempt to create a reference.  An override of {@link Object#toString()} prints out a message
     * containing this information.
     *
     * @param description a description of what part of the system attempted the reference creation, and why it failed
     * @param failedOrigin the location in VM memory at which the reference creation was attempted
     * @return a null reference annotated with historical information
     */
    public RemoteReference makeZeroReference(String description, Address failedOrigin) {
        return new AnnotatedNullReference(vm, description, failedOrigin);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gets the origin of a {@link RemoteReference}.
     * <p>
     * @return the origin of an object referred to; {@link Pointer#zero()} if the
     * reference is {@linkplain ObjectStatus#DEAD DEAD}.
     */
    public Pointer toOrigin(Reference ref) {
        final RemoteReference remoteRef = (RemoteReference) ref;
        if (isZero(remoteRef)) {
            // A {@link RemoteReference} with zero raw value is by definition DEAD.
            return Pointer.zero();
        }
        if (remoteRef instanceof LocalObjectRemoteReference) {
            throw new UnsupportedOperationException();
        }
        return remoteRef.origin().asPointer();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Create a {@link RemoteReference} that refers to a {@linkplain ObjectStatus#LIVE live} object in VM memory at a
     * specified origin.
     * <p>
     * If there is no {@linkplain ObjectStatus#LIVE live} object at the origin, then the result is {@link #zero()}.
     * <p>
     *
     * @param origin address in VM memory
     * @return a reference to the object identified by the specified origin in VM memory.
     */
    public RemoteReference fromOrigin(Pointer origin) {
        return vm.referenceManager().makeReference(origin);
    }

    public RemoteReference fromJava(Object object) {
        return localTeleReferenceManager.make(object);
    }

    public Object toJava(Reference ref) {
        if (ref instanceof LocalObjectRemoteReference) {
            final LocalObjectRemoteReference inspectorLocalTeleRef = (LocalObjectRemoteReference) ref;
            return inspectorLocalTeleRef.object();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Note:</strong> Unlike the VM implementation of {@link Reference}, there is no
     * <em>canonical</em> {@code null} reference. This is done to permit subclasses of the
     * null reference to be created for debugging.
     */
    public Reference zero() {
        return zero;
    }

    /**
     * {@inheritDoc}
     * <p>
     * We admit to multiple implementations of the zero reference
     * for debugging purposes (some may be annotated).  The
     * test for a null/zero reference is defined in terms of the
     * actually origin being stored.  No legitimate reference may
     * have this location.
     */
    public boolean isZero(Reference ref) {
        final RemoteReference remoteRef = (RemoteReference) ref;
        return remoteRef.origin().isZero();
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

    /**
     * {@inheritDoc}
     * <p>
     * Is the reference a forwarding address?
     *
     * Uses the same marking information forwarding that
     * the VM does in normal operation.
     *
     * @see DirectReferenceScheme#isMarked
     */
    public boolean isMarked(Reference ref) {
        return toOrigin(ref).isBitSet(0);
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
            writeField(ref, offset, value);
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

    /**
     * Reads the {@link Hub} word from an object's header field in VM memory.
     * Return's {@link Word#zero()} if the reference is zero..
     *
     * @param remoteRef reference to an object in VM memory
     * @return contents of the object's hub field
     */
    public Word readHubAsWord(RemoteReference remoteRef) {
        if (remoteRef instanceof LocalObjectRemoteReference) {
            throw new UnsupportedOperationException();
        }
        if (remoteRef.isZero()) {
            return Word.zero();
        }
        return Layout.readHubReferenceAsWord(remoteRef);
    }

    /**
     * Reads the {@link Hub} word from an object's header field in VM memory, determines if the word's value
     * does in fact point at a live object (possibly via a forwarder), and if so create a new
     * {@link RemoteReference} for the live hub. Return's {@link Reference#zero()} if the word cannot be read or if the
     * word's value does not point to an object.
     *
     * @param remoteRef reference to an object in VM memory
     * @return a reference to the object's hub, traversing a forwarder if needed.
     */
    public RemoteReference readHubAsRemoteReference(RemoteReference remoteRef) {
        if (remoteRef.isZero()) {
            return zero;
        }
        if (remoteRef instanceof LocalObjectRemoteReference) {
            throw new UnsupportedOperationException();
        }
        final Address hubOrigin = Layout.readHubReferenceAsWord(remoteRef).asAddress();
        if (hubOrigin.isZero()) {
            return zero;
        }
        final ObjectStatus objectStatus = vm.objects().objectStatusAt(hubOrigin);
        switch(objectStatus) {
            case LIVE:
                return fromOrigin(hubOrigin.asPointer());
            case FORWARDER:
                final RemoteReference forwarderReference = vm.referenceManager().makeQuasiReference(hubOrigin);
                return fromOrigin(forwarderReference.forwardedTo().asPointer());
        }
        return makeZeroReference("RemoteReferenceScheme.readRemoteReferenceHub() unsupported object status @", hubOrigin);
    }

    /**
     * Reads a word from an object field in VM memory that is presumed to hold a reference, determines if the word's
     * value does in fact point at a live object (possibly via a forwarder), and if so create a new
     * {@link RemoteReference} for the live object. Return's {@link Reference#zero()} if the word cannot be read or if
     * the word's value does not point to an object.
     *
     * @param remoteRef reference to an object in VM memory
     * @param fieldActor descriptor of the field from which to read
     * @return a reference to the object pointed to by the word's value, traversing a forwarder if needed.
     */
    public RemoteReference readFieldAsRemoteReference(RemoteReference remoteRef, FieldActor fieldActor) {
        if (remoteRef.isZero()) {
            return zero;
        }
        if (remoteRef instanceof LocalObjectRemoteReference) {
            return fromJava(readField(remoteRef, fieldActor.offset()));
        }
        final Address fieldValueOrigin = readWord(remoteRef, fieldActor.offset()).asAddress();
        if (fieldValueOrigin.isZero()) {
            return zero;
        }
        final ObjectStatus objectStatus = vm.objects().objectStatusAt(fieldValueOrigin);
        switch(objectStatus) {
            case LIVE:
                return fromOrigin(fieldValueOrigin.asPointer());
            case FORWARDER:
                final RemoteReference forwarderReference = vm.referenceManager().makeQuasiReference(fieldValueOrigin);
                return fromOrigin(forwarderReference.forwardedTo().asPointer());
        }
        return makeZeroReference("RemoteReferenceScheme.readRemoteReferenceField() unsupported object status @", fieldValueOrigin);
    }

    /**
     * Reads a word from what is presumed to be an array element in VM memory that holds a reference, determines if the
     * word's value does in fact point at a live object (possibly via a forwarder), and if so create a new
     * {@link RemoteReference} for the live object. Return's {@link Reference#zero()} if the word cannot be read or if
     * the word's value does not point to an object.
     *
     * @param remoteRef reference to an array in VM memory
     * @param index the array element presumed to hold a reference
     * @return a reference to the object pointed to by the word's value, traversing a forwarder if needed.
     */
    public RemoteReference readArrayAsRemoteReference(RemoteReference remoteRef, int index) {
        if (remoteRef.isZero()) {
            return zero;
        }
        if (remoteRef instanceof LocalObjectRemoteReference) {
            final Object[] array = (Object[]) toJava(remoteRef);
            return fromJava(array[index]);
        }
        final Address elementValueOrigin = Layout.getWord(remoteRef, index).asAddress();
        if (elementValueOrigin.isZero()) {
            return zero;
        }
        final ObjectStatus objectStatus = vm.objects().objectStatusAt(elementValueOrigin);
        switch(objectStatus) {
            case LIVE:
                return fromOrigin(elementValueOrigin.asPointer());
            case FORWARDER:
                final RemoteReference forwarderReference = vm.referenceManager().makeQuasiReference(elementValueOrigin);
                return fromOrigin(forwarderReference.forwardedTo().asPointer());
        }
        return makeZeroReference("RemoteReferenceScheme.readRemoteReference() unsupported object status @", elementValueOrigin);
    }

    public Value readArrayAsValue(Kind kind, RemoteReference remoteRef, int index) {
        switch (kind.asEnum) {
            case BYTE:
                return ByteValue.from(Layout.getByte(remoteRef, index));
            case BOOLEAN:
                return BooleanValue.from(Layout.getBoolean(remoteRef, index));
            case SHORT:
                return ShortValue.from(Layout.getShort(remoteRef, index));
            case CHAR:
                return CharValue.from(Layout.getChar(remoteRef, index));
            case INT:
                return IntValue.from(Layout.getInt(remoteRef, index));
            case FLOAT:
                return FloatValue.from(Layout.getFloat(remoteRef, index));
            case LONG:
                return LongValue.from(Layout.getLong(remoteRef, index));
            case DOUBLE:
                return DoubleValue.from(Layout.getDouble(remoteRef, index));
            case WORD:
                return new WordValue(Layout.getWord(remoteRef, index));
            case REFERENCE:
                try {
                    return TeleReferenceValue.from(vm, readArrayAsRemoteReference(remoteRef, index));
                } catch (DataIOError err) {
                    final Address elementEntryAddress = remoteRef.toOrigin().plus(Layout.referenceArrayLayout().getElementOffsetInCell(index));
                    TeleWarning.message("RemoteReferenceScheme: Can't access reference array element at " + elementEntryAddress.to0xHexString());
                    return TeleReferenceValue.zero(vm);
                }
            default:
                throw TeleError.unknownCase("unknown array kind");
        }

    }

}
