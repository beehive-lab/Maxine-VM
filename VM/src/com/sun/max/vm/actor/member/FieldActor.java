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
/*VCSID=ce5620b9-2c8c-4755-b64f-980b232d7165*/
package com.sun.max.vm.actor.member;

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.type.ClassRegistry.Property.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representations of fields.
 *
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 * @author Doug Simon
 */
public abstract class FieldActor<Value_Type extends Value<Value_Type>> extends MemberActor {

    private final Kind<Value_Type> _kind;

    protected FieldActor(Kind<Value_Type> kind,
                    Utf8Constant name,
                    TypeDescriptor descriptor,
                    int flags) {
        super(name,
              descriptor,
              flags);
        _kind = kind;
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
    @Override
    public final TypeDescriptor descriptor() {
        return (TypeDescriptor) super.descriptor();
    }

    public Kind<Value_Type> kind() {
        return _kind;
    }

    public int valueSize() {
        return _kind.size();
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
        // Still starting up, do not use full class loader functionality yet:
        return descriptor().toClassActor(holder().classLoader());
    }

    @CONSTANT
    private int _offset;

    public void setOffset(int offset) {
        _offset = offset;
    }

    @INLINE
    public final int offset() {
        return _offset;
    }

    public Value_Type readValue(Reference reference) {
        if (MaxineVM.isPrototyping() && this instanceof InjectedFieldActor) {
            final InjectedFieldActor<Value_Type> injectedFieldActor = StaticLoophole.cast(this);
            return injectedFieldActor.readInjectedValue(reference);
        }

        return _kind.readValue(reference, _offset);
    }

    public void writeValue(Object reference, Value_Type value) {
        _kind.writeValue(reference, _offset, value);
    }

    public void writeErasedValue(Object reference, Value value) {
        _kind.writeErasedValue(reference, _offset, value);
    }

    public static FieldActor fromJava(Field javaField) {
        FieldActor fieldActor = MaxineVM.isPrototyping() ? null : Field_fieldActor.read(javaField);
        if (fieldActor == null) {
            final ClassActor classActor = ClassActor.fromJava(javaField.getDeclaringClass());
            fieldActor = classActor.findFieldActor(SymbolTable.makeSymbol(javaField.getName()), JavaTypeDescriptor.forJavaClass(javaField.getType()));
            if (!MaxineVM.isPrototyping()) {
                Field_fieldActor.writeObject(javaField, fieldActor);
            }
        }
        return fieldActor;
    }

    public static FieldActor fromJava(Class javaClass, Utf8Constant name) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        return classActor.findFieldActor(name);
    }

    public Field toJava() {
        if (MaxineVM.isPrototyping()) {
            return JavaPrototype.javaPrototype().toJava(this);
        }
        final Class javaHolder = holder().toJava();
        final Field javaField = ReflectionFactory.getReflectionFactory().newField(javaHolder, name().toString(), type().toJava(), flags(), memberIndex(), genericSignatureString(), runtimeVisibleAnnotationsBytes());
        Field_fieldActor.writeObject(javaField, this);
        return javaField;
    }

    public Pointer pointer(Object object) {
        if (isStatic()) {
            return Pointer.zero();
        }
        return Reference.fromJava(object).toOrigin().plus(offset());
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return toJava().getAnnotation(annotationClass);
    }

    /**
     * A field is regarded as "constant" if:
     * - it is NOT declared in java.lang.System and
     * - it is final or it has the {@link CONSTANT} annotation.
     *
     * @return whether the addressed field will ever change its value for the given holder throughout this runtime activation
     */
    public final boolean isConstant() {
        final ClassActor holder = holder();
        if (holder.toJava() == System.class) {
            // The fields 'in', 'out', 'err' are "fake final".
            // HotSpot assigns to them via native methods!
            // Therefore we must not treat them as constant.
            // The remaining field in System is not final anyway, so:
            return false;
        }
        if (isFinal() || isConstant(flags())) {
            if (MaxineVM.isPrototyping()) {
                // All classes are fully loaded and initialized before compiling anything:
                return true;
            }
            // We have to be careful not to treat uninitialized fields as "constant":
            return !isStatic() || holder.isInitialized();
        }
        return false;
    }

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

    public static final FieldActor[] NONE = {};

    public void write(DataOutputStream stream) throws IOException {
        FieldID.fromFieldActor(this).write(stream);
    }

    public static FieldActor read(DataInputStream stream) throws IOException {
        return FieldID.toFieldActor(FieldID.fromWord(Word.read(stream)));
    }

    @FOLD
    public static FieldActor findInstance(Class javaClass, String name) {
        return ClassActor.fromJava(javaClass).findLocalInstanceFieldActor(name);
    }

    @FOLD
    public static FieldActor findStatic(Class javaClass, String name) {
        return ClassActor.fromJava(javaClass).findLocalStaticFieldActor(name);
    }
}
