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

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Definition of reference fields injected into JDK classes.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class InjectedReferenceFieldActor<T> extends FieldActor implements InjectedFieldActor<ReferenceValue> {

    public TypeDescriptor holderTypeDescriptor() {
        return holder;
    }

    public ReferenceValue readInjectedValue(Reference reference) {
        throw ProgramError.unexpected(this + " cannot be read while bootstrapping");
    }

    private final TypeDescriptor holder;

    /**
     * Creates an actor for an injected long field.
     *
     * @param holder the class into which the field is injected
     * @param fieldType the type of the field (the name of the field is derived from this value)
     */
    @HOSTED_ONLY
    public InjectedReferenceFieldActor(Class holder, Class<T> fieldType) {
        super(Kind.REFERENCE,
              SymbolTable.makeSymbol("_$injected$" + fieldType.getSimpleName()),
              JavaTypeDescriptor.forJavaClass(fieldType),
              ACC_SYNTHETIC + ACC_PRIVATE + INJECTED);
        this.holder = JavaTypeDescriptor.forJavaClass(holder);
        Static.registerInjectedFieldActor(this);
    }

    /**
     * A field of type {@link ClassActor} injected into {@link Class}.
     */
    public static final InjectedReferenceFieldActor<ClassActor> Class_classActor = new InjectedReferenceFieldActor<ClassActor>(Class.class, ClassActor.class) {
        @Override
        public ReferenceValue readInjectedValue(Reference reference) {
            final Class javaClass = (Class) reference.toJava();
            return ReferenceValue.from(ClassActor.fromJava(javaClass));
        }
    };

    /**
     * A field of type {@link ClassRegistry} injected into {@link ClassLoader}.
     */
    public static final InjectedReferenceFieldActor<ClassRegistry> ClassLoader_classRegistry = new InjectedReferenceFieldActor<ClassRegistry>(ClassLoader.class, ClassRegistry.class) {
        @Override
        public ReferenceValue readInjectedValue(Reference reference) {
            assert reference.toJava() instanceof ClassLoader;
            return ReferenceValue.from(ClassRegistry.vmClassRegistry());
        }
    };

    /**
     * A field of type {@link VmThread} injected into {@link Thread}.
     */
    public static final InjectedReferenceFieldActor<VmThread> Thread_vmThread = new InjectedReferenceFieldActor<VmThread>(Thread.class, VmThread.class) {
        @Override
        public ReferenceValue readInjectedValue(Reference reference) {
            assert reference.toJava() instanceof Thread;
            return ReferenceValue.from(VmThread.MAIN_VM_THREAD);
        }
    };

    /**
     * A field of type {@link FieldActor} injected into {@link Field}.
     */
    public static final InjectedReferenceFieldActor<FieldActor> Field_fieldActor = new InjectedReferenceFieldActor<FieldActor>(Field.class, FieldActor.class) {
        @Override
        public ReferenceValue readInjectedValue(Reference reference) {
            final Object object = reference.toJava();
            assert object instanceof Field;
            return ReferenceValue.from(FieldActor.fromJava((Field) object));
        }
    };

    /**
     * A field of type {@link MethodActor} injected into {@link Method}.
     */
    public static final InjectedReferenceFieldActor<MethodActor> Method_methodActor = new InjectedReferenceFieldActor<MethodActor>(Method.class, MethodActor.class) {
        @Override
        public ReferenceValue readInjectedValue(Reference reference) {
            final Object object = reference.toJava();
            assert object instanceof Method;
            return ReferenceValue.from(MethodActor.fromJava((Method) object));
        }
    };

    /**
     * A field of type {@link MethodActor} injected into {@link Constructor}.
     */
    public static final InjectedReferenceFieldActor<MethodActor> Constructor_methodActor = new InjectedReferenceFieldActor<MethodActor>(Constructor.class, MethodActor.class) {
        @Override
        public ReferenceValue readInjectedValue(Reference reference) {
            final Object object = reference.toJava();
            assert object instanceof Constructor;
            return ReferenceValue.from(MethodActor.fromJavaConstructor((Constructor) object));
        }
    };
}
