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
import com.sun.max.vm.object.*;
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
public class FieldActor extends MemberActor {

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
        return descriptor().resolve(holder().classLoader);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @INLINE
    public final int offset() {
        return offset;
    }

    public Value readValue(Reference reference) {
        if (MaxineVM.isHosted() && this instanceof InjectedFieldActor) {
            final InjectedFieldActor injectedFieldActor = StaticLoophole.cast(this);
            return injectedFieldActor.readInjectedValue(reference);
        }

        return kind.readValue(reference, offset);
    }

    public void writeValue(Object reference, Value value) {
        kind.writeErasedValue(reference, offset, value);
    }

    public static FieldActor fromJava(Field javaField) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toFieldActor(javaField);
        }
        FieldActor fieldActor = (FieldActor) TupleAccess.readObject(javaField, Field_fieldActor.offset());
        if (fieldActor == null) {
            final ClassActor holder = ClassActor.fromJava(javaField.getDeclaringClass());
            fieldActor = holder.findFieldActor(SymbolTable.makeSymbol(javaField.getName()), JavaTypeDescriptor.forJavaClass(javaField.getType()));
            TupleAccess.writeObject(javaField, Field_fieldActor.offset(), fieldActor);
        }
        return fieldActor;
    }

    public Field toJava() {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toJava(this);
        }
        final Class javaHolder = holder().toJava();
        final Field javaField = ReflectionFactory.getReflectionFactory().newField(javaHolder, name.toString(), type().toJava(), flags(), memberIndex(), genericSignatureString(), runtimeVisibleAnnotationsBytes());
        TupleAccess.writeObject(javaField, Field_fieldActor.offset(), this);
        return javaField;
    }

    @Override
    public String toString() {
        return format("%H.%n");
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
     * Determines if the value read from this field will always be the same from this time on.
     *
     * @return {@code true} if this field will never change its value anytime it is read subsequent to calling this
     *         method; {@code false} otherwise
     */
    public final boolean isConstant() {
        final ClassActor holder = holder();
        if (isFinal()) {
            if (isStatic()) {
                // Static final field:
                if (!holder().hasClassInitializer()) {
                    // The field's value must come from a ConstantValue attribute. If
                    // no such attribute is present for this field, then it will have the
                    // default value for its type.
                    return true;
                }
                if (MaxineVM.isHosted()) {
                    if (MaxineVM.isMaxineClass(holder())) {
                        // The class initializers of all Maxine classes are run while bootstrapping and
                        // the values they assign to static final fields are frozen in the boot image.
                        return true;
                    }
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
            assert MaxineVM.isMaxineClass(holder()) : "@CONSTANT applied to field of non-Maxine class: " + this;
            return MaxineVM.isHosted() || !isStatic() || holder.isInitialized();
        }
        return false;
    }


    /**
     * Determines if the value read from this field will always be the same if it is a non-default value for this
     * field's type.
     */
    public boolean isConstantWhenNotZero() {
        return isConstantWhenNotZero(flags());
    }

    @Override
    public String javaSignature(boolean qualified) {
        return descriptor().toJavaString(qualified) + ' ' + qualified;
    }

    /**
     * Determines if this field is a reference type field that is treated specially by
     * the garbage collector. Typically, the {@code referent} field in {@link java.lang.ref.Reference}
     * used to hold a weak reference will return true for this method.
     */
    @INLINE
    public final boolean isSpecialReference() {
        return isSpecialReference(flags());
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

    @FOLD
    public static FieldActor findInstance(Class javaClass, String name) {
        return ClassActor.fromJava(javaClass).findLocalInstanceFieldActor(name);
    }
}
