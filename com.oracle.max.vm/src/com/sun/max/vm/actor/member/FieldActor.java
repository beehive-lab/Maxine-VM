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
package com.sun.max.vm.actor.member;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.type.ClassRegistry.Property.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import sun.reflect.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.hosted.JDKInterceptor.InterceptedField;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representations of fields.
 */
public class FieldActor extends MemberActor implements RiResolvedField {

    /**
     * Flags indicating special annotations applied to methods.
     */
    public enum VmFlag {
        Injected,
        Constant,
        ConstantWhenNotZero,
        Reset;

        public final int mask;

        VmFlag() {
            assert ordinal() < 16 : "Too many VmFlags to fit into 16 bits";
            mask = 1 << (ordinal() + 16);
        }

        @INLINE
        public boolean check(FieldActor fieldActor) {
            return (fieldActor.flags() & mask) != 0;
        }
    }

    public static final FieldActor[] NONE = {};

    public final Kind kind;
    @CONSTANT
    private int offset;

    public FieldActor(Kind kind,
                    Utf8Constant name,
                    TypeDescriptor descriptor,
                    int flags) {
        super(name,
              descriptor,
              flags);
        this.kind = kind;
        assert isInjected() == this instanceof InjectedFieldActor;
    }

    @INLINE
    public final boolean isVolatile() {
        return isVolatile(flags());
    }

    @INLINE
    public final boolean isTransient() {
        return isTransient(flags());
    }

    @INLINE
    public final boolean isInjected() {
        return isInjected(flags());
    }

    @Override
    public final boolean isHiddenToReflection() {
        return isInjected();
    }

    @INLINE
    public final TypeDescriptor descriptor() {
        return (TypeDescriptor) descriptor;
    }

    /**
     * Gets the value specified in a ConstantValue attribute associated with this field.
     *
     * @return null if there is no ConstantValue attribute associated with this field
     */
    public Value constantValue() {
        return holder().classRegistry().get(CONSTANT_VALUE, this);
    }

    /**
     * Gets the actor for this field's type.
     */
    public ClassActor type() {
        if (isInjected(flags())) {
            // special direct search for VM class defining injected field.
            return ClassRegistry.get(VMClassLoader.VM_CLASS_LOADER, descriptor(), false);
        }
        return descriptor().resolve(holder().classLoader);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @INLINE
    public final int offset() {
        return offset;
    }

    /**
     * Generates an error that the field could not be accessed.
     */
    @HOSTED_ONLY
    private InternalError accessError(Throwable cause) {
        throw (InternalError) new InternalError("Unexpected error accessing field: " + this).initCause(cause);
    }

    /**
     * Checks an access of this field.
     *
     * @param obj the object on which the field is being accessed. This is ignored if the field is static.
     * @param kind the kind of the access which must match the kind of this field. This is ignored if it is {@code null}.
     * @return the reference to the instance or static tuple on which the access should be performed. This will be
     *         {@code null} when {@linkplain MaxineVM#isHosted() hosted}.
     */
    private Reference access(Object obj, Kind kind) {
        if (kind != this.kind && kind != null) {
            throw new IllegalArgumentException("Cannot access " + this + " as an " + kind.javaClass.getName());
        }
        if (MaxineVM.isHosted()) {
            Field field = toJava();
            field.setAccessible(true);
            return null;
        }
        if (isStatic()) {
            return Reference.fromJava(holder().staticTuple());
        }
        if (!holder().isInstance(obj)) {
            throw new IllegalArgumentException("Cannot access " + this + " on an instance of " + obj.getClass().getName());
        }
        return Reference.fromJava(obj);
    }

    /**
     * Gets the value of this {@code byte} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code byte} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public byte getByte(Object obj) {
        Reference ref = access(obj, Kind.BYTE);
        if (isHosted()) {
            return getValue(obj).asByte();
        }
        return ref.readByte(offset);
    }

    /**
     * Gets the value of this {@code boolean} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code boolean} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public boolean getBoolean(Object obj) {
        Reference ref = access(obj, Kind.BOOLEAN);
        if (isHosted()) {
            return getValue(obj).asBoolean();
        }
        return ref.readBoolean(offset);
    }

    /**
     * Gets the value of this {@code short} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code short} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public short getShort(Object obj) {
        Reference ref = access(obj, Kind.SHORT);
        if (isHosted()) {
            return getValue(obj).asShort();
        }
        return ref.readShort(offset);
    }

    /**
     * Gets the value of this {@code char} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code char} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public char getChar(Object obj) {
        Reference ref = access(obj, Kind.CHAR);
        if (isHosted()) {
            return getValue(obj).asChar();
        }
        return ref.readChar(offset);
    }

    /**
     * Gets the value of this {@code int} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code int} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public int getInt(Object obj) {
        Reference ref = access(obj, Kind.INT);
        if (isHosted()) {
            return getValue(obj).asInt();
        }
        return ref.readInt(offset);
    }

    /**
     * Gets the value of this {@code float} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code float} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public float getFloat(Object obj) {
        Reference ref = access(obj, Kind.FLOAT);
        if (isHosted()) {
            return getValue(obj).asFloat();
        }
        return ref.readFloat(offset);
    }

    /**
     * Gets the value of this {@code long} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code long} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public long getLong(Object obj) {
        Reference ref = access(obj, Kind.LONG);
        if (isHosted()) {
            return getValue(obj).asLong();
        }
        return ref.readLong(offset);
    }

    /**
     * Gets the value of this {@code double} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code double} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public double getDouble(Object obj) {
        Reference ref = access(obj, Kind.DOUBLE);
        if (isHosted()) {
            return getValue(obj).asDouble();
        }
        return ref.readDouble(offset);
    }

    /**
     * Gets the value of this {@code object} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code object} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public Object getObject(Object obj) {
        Reference ref = access(obj, Kind.REFERENCE);
        if (isHosted()) {
            return getValue(obj).asObject();
        }
        return ref.readReference(offset).toJava();
    }

    /**
     * Gets the value of this {@code Word} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code Word} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public Word getWord(Object obj) {
        Reference ref = access(obj, Kind.WORD);
        if (isHosted()) {
            return getValue(obj).asWord();
        }
        return ref.readWord(offset);
    }

    /**
     * Analogous to {@link Field#get(Object)} but without the access checks that may raise {@link IllegalAccessException}.
     */
    public Object get(Object obj) {
        if (isHosted()) {
            return getValue(obj).asBoxedJavaValue();
        }
        // Avoid boxing Object values
        if (kind.isReference) {
            return getObject(obj);
        }

        return kind.readValue(access(obj, Kind.REFERENCE), offset).asBoxedJavaValue();
    }

    /**
     * Reads the value of this field in a given object.
     */
    public Value getValue(Object obj) {
        if (MaxineVM.isHosted()) {
            if (isInjected()) {
                final InjectedFieldActor injectedFieldActor = (InjectedFieldActor) this;
                return injectedFieldActor.readInjectedValue(obj);
            }
            // is this an intercepted field?
            final InterceptedField interceptedField = JDKInterceptor.getInterceptedField(this);
            if (interceptedField != null) {
                return interceptedField.getValue(obj, this);
            }
            // does the field have a constant value?
            final Value constantValue = constantValue();
            if (constantValue != null) {
                return constantValue;
            }
            // is the field annotated with @RESET?
            if (this.getAnnotation(RESET.class) != null) {
                return kind.zeroValue();
            }
            // try to read the field's value via reflection
            try {
                final Field field = this.toJava();
                field.setAccessible(true);
                Object boxedJavaValue = field.get(obj);
                if (this.kind.isReference) {
                    boxedJavaValue = JavaPrototype.hostToTarget(boxedJavaValue);
                }
                return this.kind.asValue(boxedJavaValue);
            } catch (IllegalAccessException e) {
                throw accessError(e);
            }
        }

        return readValue(access(obj, null));
    }

    /**
     * Gets value of this field from a given tuple.
     *
     * @param tuple the instance or static tuple from which the field will be read. This method does not check if
     *            {@code tuple} is of the right type.
     *
     * @return the value of this field's {@linkplain #kind kind} at this field's {@linkplain #offset() offset} in
     *         {@code tuple}
     */
    public Value readValue(Reference tuple) {
        return kind.readValue(tuple, offset);
    }

    /**
     * Sets the value of this {@code byte} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code byte} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setByte(Object obj, byte value) {
        Reference ref = access(obj, Kind.BYTE);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeByte(offset, value);
        }
    }

    /**
     * Sets the value of this {@code boolean} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code boolean} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setBoolean(Object obj, boolean value) {
        Reference ref = access(obj, Kind.BOOLEAN);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeBoolean(offset, value);
        }
    }

    /**
     * Sets the value of this {@code char} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code char} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setChar(Object obj, char value) {
        Reference ref = access(obj, Kind.CHAR);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeChar(offset, value);
        }
    }

    /**
     * Sets the value of this {@code short} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code short} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setShort(Object obj, short value) {
        Reference ref = access(obj, Kind.SHORT);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeShort(offset, value);
        }
    }

    /**
     * Sets the value of this {@code int} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code int} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setInt(Object obj, int value) {
        Reference ref = access(obj, Kind.INT);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeInt(offset, value);
        }
    }

    /**
     * Sets the value of this {@code float} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code float} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setFloat(Object obj, float value) {
        Reference ref = access(obj, Kind.FLOAT);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeFloat(offset, value);
        }
    }

    /**
     * Sets the value of this {@code long} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code long} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setLong(Object obj, long value) {
        Reference ref = access(obj, Kind.LONG);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeLong(offset, value);
        }
    }

    /**
     * Sets the value of this {@code double} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code double} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setDouble(Object obj, double value) {
        Reference ref = access(obj, Kind.DOUBLE);
        if (isHosted()) {
            set(obj, value);
        } else {
            ref.writeDouble(offset, value);
        }
    }

    /**
     * Sets the value of this {@code Object} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code Object} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setObject(Object obj, Object value) {
        Reference ref = access(obj, Kind.REFERENCE);
        if (isHosted()) {
            set(obj, value);
        } else {
            if (value != null && !type().isInstance(value)) {
                throw new IllegalArgumentException("Cannot set value of type " + value.getClass().getName() + " to " + this);
            }
            ref.writeReference(offset, Reference.fromJava(value));
        }
    }

    /**
     * Sets the value of this {@code Word} field.
     *
     * @throws IllegalArgumentException if this field is not of type {@code Word} OR it is not static and {@code obj} is
     *             not an instance of this field's holder type
     * @throws NullPointerException if this field is not static and {@code obj == null}
     */
    public void setWord(Object obj, Word value) {
        Reference ref = access(obj, Kind.WORD);
        if (isHosted()) {
            setWordHosted(obj, value);
        } else {
            ref.writeWord(offset, value);
        }
    }

    @HOSTED_ONLY
    private void setWordHosted(Object obj, Word value) {
        set(obj, value);
    }

    /**
     * Analogous to {@link Field#set(Object, Object)} but without the access checks that may raise {@link IllegalAccessException}.
     */
    public void set(Object obj, Object value) {
        Reference ref = access(obj, null);
        if (isHosted()) {
            try {
                toJava().set(obj, value);
            } catch (IllegalAccessException e) {
                throw accessError(e);
            }
        } else {
            if (value != null && kind.isReference && !type().isInstance(value)) {
                throw new IllegalArgumentException("Cannot set value of type " + value.getClass().getName() + " to " + this);
            }
            writeValue(ref, kind.asValue(value));
        }
    }

    public void setValue(Object obj, Value value) {
        set(obj, value.asBoxedJavaValue());
    }

    /**
     * Updates the value of this field in a given object. This method assumes
     * that {@code reference} is a static tuple if this is a static field.
     *
     * @param reference the object or static tuple containing this field
     * @param value the value to which this field in {@code reference} will be updated
     */
    public void writeValue(Object reference, Value value) {
        kind.writeErasedValue(reference, offset, value);
    }

    public static FieldActor fromJava(Field javaField) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toFieldActor(javaField);
        }
        FieldActor injectedField = Utils.cast(Field_fieldActor);
        FieldActor fieldActor = (FieldActor) injectedField.getObject(javaField);
        if (fieldActor == null) {
            final ClassActor holder = ClassActor.fromJava(javaField.getDeclaringClass());
            fieldActor = holder.findFieldActor(SymbolTable.makeSymbol(javaField.getName()), JavaTypeDescriptor.forJavaClass(javaField.getType()));
            injectedField.setObject(javaField, fieldActor);
        }
        return fieldActor;
    }

    public Field toJava() {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toJava(this);
        }
        final Class javaHolder = holder().toJava();
        final Field javaField = ReflectionFactory.getReflectionFactory().newField(javaHolder, name.toString(), type().toJava(), flags(), memberIndex(), genericSignatureString(), runtimeVisibleAnnotationsBytes());
        FieldActor injectedField = Utils.cast(Field_fieldActor);
        injectedField.setObject(javaField, this);
        return javaField;
    }

    @Override
    public String toString() {
        return format("%H.%n");
    }

    /**
     * Gets the address of this field in a given object.
     *
     * @param object the object for which the field address is requested. This is ignored if this is a static field.
     * @return the address of this field in {@code object} or the relevant static tuple if this is a static field
     */
    public Pointer addressOf(Object object) {
        if (isStatic()) {
            return Reference.fromJava(holder().staticTuple()).toOrigin().plus(offset());
        }
        return Reference.fromJava(object).toOrigin().plus(offset());
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        try {
            return toJava().getAnnotation(annotationClass);
        } catch (NoSuchFieldError e) {
            if (MaxineVM.isHosted()) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Determines if the value read from this field will always be the same from this time on.
     *
     * @return {@code true} if this field will never change its value anytime it is read subsequent to calling this
     *         method; {@code false} otherwise
     */
    public final boolean isConstant() {
        final ClassActor holder = holder();
        if (isFinal()) {
            if (isStatic()) {
                if (this == ClassRegistry.SYSTEM_IN || this == ClassRegistry.SYSTEM_OUT || this == ClassRegistry.SYSTEM_ERR) {
                    // A *special* static final field whose value can change.
                    return false;
                }

                // Static final field:
                if (!holder().hasClassInitializer()) {
                    // The field's value must come from a ConstantValue attribute. If
                    // no such attribute is present for this field, then it will have the
                    // default value for its type.
                    return true;
                }
                if (MaxineVM.isHosted()) {
                    if (JDKInterceptor.hasMutabilityOverride(this)) {
                        return false;
                    }
                }

                // This is now a field in a class with a class initializer:
                // before the class initializer is executed, the field's value is not guaranteed to be immutable.
                return holder().isInitialized();
            }

            if (MaxineVM.isHosted()) {
                if (JDKInterceptor.hasMutabilityOverride(this)) {
                    return false;
                }
            }

            // Non-static final field:
            return true;
        }

        if (isConstant(flags())) {
            return MaxineVM.isHosted() || !isStatic() || holder.isInitialized();
        }
        return false;
    }


    /**
     * Determines if the value read from this field will always be the same if
     * it is a non-default value for this field's type.
     */
    public boolean isConstantWhenNotZero() {
        return isConstantWhenNotZero(flags());
    }

    @Override
    public String javaSignature(boolean qualified) {
        return descriptor().toJavaString(qualified) + ' ' + qualified;
    }

    public String jniSignature() {
        return descriptor().toString();
    }

    public void write(DataOutputStream stream) throws IOException {
        FieldID.fromFieldActor(this).write(stream);
    }

    public static FieldActor read(DataInputStream stream) throws IOException {
        return FieldID.toFieldActor(FieldID.fromWord(Word.read(stream)));
    }

    @Fold
    public static FieldActor findInstance(Class javaClass, String name) {
        return ClassActor.fromJava(javaClass).findLocalInstanceFieldActor(name);
    }

    public final int accessFlags() {
        return flags() & JAVA_FIELD_FLAGS;
    }

    public final CiConstant constantValue(CiConstant receiver) {
        if (isConstant() || isConstantWhenNotZero()) {
            Value v;
            if (isStatic()) {
                v = constantValue();
                if (v != null) {
                    return v.asCiConstant();
                }
            }
            v = getValue((receiver == null) ? null : receiver.asObject());
            if (!isConstantWhenNotZero() || !v.isZero()) {
                return v.asCiConstant();
            }
        }
        return null;
    }

    public final boolean isResolved() {
        return true;
    }

    @Override
    public final CiKind kind(boolean architecture) {
        return WordUtil.ciKind(kind, architecture);
    }

    public final String name() {
        return name.string;
    }
}
