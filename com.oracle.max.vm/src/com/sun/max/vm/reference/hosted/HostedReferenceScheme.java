/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.reference.hosted;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A reference scheme for use when executing in {@linkplain MaxineVM#isHosted() hosted} mode.
 */
public final class HostedReferenceScheme extends AbstractVMScheme implements ReferenceScheme {

    public boolean isConstant() {
        return false;
    }

    public Reference fromJava(Object object) {
        return new HostedReference(object);
    }

    public Object toJava(Reference ref) {
        final HostedReference prototypeReference = (HostedReference) ref;
        return prototypeReference.getObject();
    }

    public Reference fromReference(Reference reference) {
        return fromJava(reference.toJava());
    }

    public Reference fromOrigin(Pointer origin) {
        throw FatalError.unimplemented();
    }

    public Reference updateReference(Reference ref, Pointer origin) {
        throw FatalError.unimplemented();
    }

    @INLINE
    public Reference zero() {
        return null;
    }

    public boolean isZero(Reference ref) {
        return toJava(ref) == null;
    }

    @INLINE
    public boolean isAllOnes(Reference ref) {
        throw FatalError.unimplemented();
    }

    public boolean equals(Reference ref1, Reference ref2) {
        return ref1.equals(ref2);
    }

    public boolean isMarked(Reference ref) {
        throw FatalError.unimplemented();
    }

    public Reference marked(Reference ref) {
        throw FatalError.unimplemented();
    }

    public Reference unmarked(Reference ref) {
        throw FatalError.unimplemented();
    }

    public Pointer toOrigin(Reference ref) {
        throw FatalError.unimplemented();
    }

    private void setValue(Reference ref, int displacement, int index, Object wordOrBoxedJavaValue) {
        final Object object = toJava(ref);
        final HostedObjectMirror mirror = new HostedObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;
        ProgramError.check(displacement == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid array displacement");
        final Value value = wordOrBoxedJavaValue instanceof Word ? WordValue.from((Word) wordOrBoxedJavaValue) : Value.fromBoxedJavaValue(wordOrBoxedJavaValue);
        mirror.writeElement(value.kind(), index, value);
    }

    private <T> T getValue(Reference ref, Class<T> type, int displacement, int index) {
        final Object object = toJava(ref);
        final HostedObjectMirror mirror = new HostedObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;
        ProgramError.check(displacement == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid array displacement");
        final Kind kind = Kind.fromJava(type);
        final Class<T> castType = null;
        return Utils.cast(castType, mirror.readElement(kind, index).asBoxedJavaValue());
    }

    private void writeValue(Reference ref, int offset, Object wordOrBoxedJavaValue) {
        final Object object = toJava(ref);
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
            final HostedObjectMirror mirror = new HostedObjectMirror(object);
            final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;

            final Value value = wordOrBoxedJavaValue instanceof Word ? WordValue.from((Word) wordOrBoxedJavaValue) : Value.fromBoxedJavaValue(wordOrBoxedJavaValue);
            specificLayout.writeValue(value.kind(), mirror, offset, value);
            return;
        }
    }

    private <T> T readValue(Reference ref, Class<T> type, int offset) {
        final Class<T> castType = null;
        final Object object = toJava(ref);

        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            try {
                return Utils.cast(castType, WithoutAccessCheck.getStaticField(staticTuple.classActor().toJava(), fieldActor.name.toString()));
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not read field: " + fieldActor, throwable);
            }
        }

        final HostedObjectMirror mirror = new HostedObjectMirror(object);
        final ClassActor classActor = mirror.classActor();
        final SpecificLayout specificLayout = classActor.dynamicHub().specificLayout;

        final Kind kind = Kind.fromJava(type);
        final Value value = specificLayout.readValue(kind, mirror, offset);
        return Utils.cast(castType, value.asBoxedJavaValue());
    }

    public byte readByte(Reference ref, Offset offset) {
        return readByte(ref, offset.toInt());
    }

    public byte readByte(Reference ref, int offset) {
        return readValue(ref, byte.class, offset).byteValue();
    }

    public byte getByte(Reference ref, int displacement, int index) {
        return getValue(ref, byte.class, displacement, index);
    }

    public boolean readBoolean(Reference ref, Offset offset) {
        return readBoolean(ref, offset.toInt());
    }

    public boolean readBoolean(Reference ref, int offset) {
        return readValue(ref, boolean.class, offset).booleanValue();
    }

    public boolean getBoolean(Reference ref, int displacement, int index) {
        return getValue(ref, boolean.class, displacement, index);
    }

    public short readShort(Reference ref, Offset offset) {
        return readShort(ref, offset.toInt());
    }

    public short readShort(Reference ref, int offset) {
        return readValue(ref, short.class, offset).shortValue();
    }

    public short getShort(Reference ref, int displacement, int index) {
        return getValue(ref, short.class, displacement, index);
    }

    public char readChar(Reference ref, Offset offset) {
        return readChar(ref, offset.toInt());
    }

    public char readChar(Reference ref, int offset) {
        return readValue(ref, char.class, offset).charValue();
    }

    public char getChar(Reference ref, int displacement, int index) {
        return getValue(ref, char.class, displacement, index);
    }

    public int readInt(Reference ref, Offset offset) {
        return readInt(ref, offset.toInt());
    }

    public int readInt(Reference ref, int offset) {
        return readValue(ref, int.class, offset).intValue();
    }

    public int getInt(Reference ref, int displacement, int index) {
        return getValue(ref, int.class, displacement, index);
    }

    public float readFloat(Reference ref, Offset offset) {
        return readFloat(ref, offset.toInt());
    }

    public float readFloat(Reference ref, int offset) {
        return readValue(ref, float.class, offset).floatValue();
    }

    public float getFloat(Reference ref, int displacement, int index) {
        return getValue(ref, float.class, displacement, index);
    }

    public long readLong(Reference ref, Offset offset) {
        return readLong(ref, offset.toInt());
    }

    public long readLong(Reference ref, int offset) {
        return readValue(ref, long.class, offset).longValue();
    }

    public long getLong(Reference ref, int displacement, int index) {
        return getValue(ref, long.class, displacement, index);
    }

    public double readDouble(Reference ref, Offset offset) {
        return readDouble(ref, offset.toInt());
    }

    public double readDouble(Reference ref, int offset) {
        return readValue(ref, double.class, offset).doubleValue();
    }

    public double getDouble(Reference ref, int displacement, int index) {
        return getValue(ref, double.class, displacement, index);
    }

    public Word readWord(Reference ref, Offset offset) {
        return readWord(ref, offset.toInt());
    }

    public Word readWord(Reference ref, int offset) {
        return readValue(ref, Word.class, offset);
    }

    public Word getWord(Reference ref, int displacement, int index) {
        return getValue(ref, Word.class, displacement, index);
    }

    public Reference readReference(Reference ref, Offset offset) {
        return readReference(ref, offset.toInt());
    }

    public Reference readReference(Reference ref, int offset) {
        return fromJava(readValue(ref, Object.class, offset));
    }

    public Reference getReference(Reference ref, int displacement, int index) {
        return fromJava(getValue(ref, Object.class, displacement, index));
    }

    public void writeByte(Reference ref, Offset offset, byte value) {
        writeByte(ref, offset.toInt(), value);
    }

    public void writeByte(Reference ref, int offset, byte value) {
        writeValue(ref, offset, Byte.valueOf(value));
    }

    public void setByte(Reference ref, int displacement, int index, byte value) {
        setValue(ref, displacement, index, Byte.valueOf(value));
    }

    public void writeBoolean(Reference ref, Offset offset, boolean value) {
        writeBoolean(ref, offset.toInt(), value);
    }

    public void writeBoolean(Reference ref, int offset, boolean value) {
        writeValue(ref, offset, Boolean.valueOf(value));
    }

    public void setBoolean(Reference ref, int displacement, int index, boolean value) {
        setValue(ref, displacement, index, Boolean.valueOf(value));
    }

    public void writeShort(Reference ref, Offset offset, short value) {
        writeShort(ref, offset.toInt(), value);
    }

    public void writeShort(Reference ref, int offset, short value) {
        writeValue(ref, offset, Short.valueOf(value));
    }

    public void setShort(Reference ref, int displacement, int index, short value) {
        setValue(ref, displacement, index, Short.valueOf(value));
    }

    public void writeChar(Reference ref, Offset offset, char value) {
        writeChar(ref, offset.toInt(), value);
    }

    public void writeChar(Reference ref, int offset, char value) {
        writeValue(ref, offset, Character.valueOf(value));
    }

    public void setChar(Reference ref, int displacement, int index, char value) {
        setValue(ref, displacement, index, Character.valueOf(value));
    }

    public void writeInt(Reference ref, Offset offset, int value) {
        writeInt(ref, offset.toInt(), value);
    }

    public void writeInt(Reference ref, int offset, int value) {
        writeValue(ref, offset, Integer.valueOf(value));
    }

    public void setInt(Reference ref, int displacement, int index, int value) {
        setValue(ref, displacement, index, Integer.valueOf(value));
    }

    public void writeFloat(Reference ref, Offset offset, float value) {
        writeFloat(ref, offset.toInt(), value);
    }

    public void writeFloat(Reference ref, int offset, float value) {
        writeValue(ref, offset, Float.valueOf(value));
    }

    public void setFloat(Reference ref, int displacement, int index, float value) {
        setValue(ref, displacement, index, Float.valueOf(value));
    }

    public void writeLong(Reference ref, Offset offset, long value) {
        writeLong(ref, offset.toInt(), value);
    }

    public void writeLong(Reference ref, int offset, long value) {
        writeValue(ref, offset, Long.valueOf(value));
    }

    public void setLong(Reference ref, int displacement, int index, long value) {
        setValue(ref, displacement, index, Long.valueOf(value));
    }

    public void writeDouble(Reference ref, Offset offset, double value) {
        writeDouble(ref, offset.toInt(), value);
    }

    public void writeDouble(Reference ref, int offset, double value) {
        writeValue(ref, offset, Double.valueOf(value));
    }

    public void setDouble(Reference ref, int displacement, int index, double value) {
        setValue(ref, displacement, index, Double.valueOf(value));
    }

    public void writeWord(Reference ref, Offset offset, Word value) {
        writeWord(ref, offset.toInt(), value);
    }

    public void writeWord(Reference ref, int offset, Word value) {
        writeValue(ref, offset, value);
    }

    public void setWord(Reference ref, int displacement, int index, Word value) {
        setValue(ref, displacement, index, new BoxedWord(value));
    }

    public void writeReference(Reference ref, Offset offset, Reference value) {
        writeReference(ref, offset.toInt(), value);
    }

    public void writeReference(Reference ref, int offset, Reference value) {
        writeValue(ref, offset, value.toJava());
    }

    public void setReference(Reference ref, int displacement, int index, Reference value) {
        setValue(ref, displacement, index, value.toJava());
    }

    public int compareAndSwapInt(Reference ref, Offset offset, int expectedValue, int newValue) {
        return toOrigin(ref).compareAndSwapInt(offset, expectedValue, newValue);
    }

    public int compareAndSwapInt(Reference ref, int offset, int expectedValue, int newValue) {
        return toOrigin(ref).compareAndSwapInt(offset, expectedValue, newValue);
    }

    public Word compareAndSwapWord(Reference ref, Offset offset, Word expectedValue, Word newValue) {
        throw FatalError.unimplemented();
    }

    public Word compareAndSwapWord(Reference ref, int offset, Word expectedValue, Word newValue) {
        throw FatalError.unimplemented();
    }

    public Reference compareAndSwapReference(Reference ref, Offset offset, Reference expectedValue, Reference newValue) {
        throw FatalError.unimplemented();
    }

    public Reference compareAndSwapReference(Reference ref, int offset, Reference expectedValue, Reference newValue) {
        throw FatalError.unimplemented();
    }

    public void copyElements(int displacement, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        throw FatalError.unimplemented();
    }

    @Override
    public byte[] asBytes(Pointer origin) {
        throw FatalError.unimplemented();
    }

    @Override
    public byte[] nullAsBytes() {
        throw FatalError.unimplemented();
    }
}
