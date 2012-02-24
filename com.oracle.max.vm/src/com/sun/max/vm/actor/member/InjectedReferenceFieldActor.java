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

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Definition of reference fields injected into JDK classes.
 */
public class InjectedReferenceFieldActor<T> extends FieldActor implements InjectedFieldActor<ReferenceValue> {

    public TypeDescriptor holderTypeDescriptor() {
        return holder;
    }

    @HOSTED_ONLY
    public ReferenceValue readInjectedValue(Object object) {
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
        @HOSTED_ONLY
        @Override
        public ReferenceValue readInjectedValue(Object object) {
            final Class javaClass = (Class) object;
            return ReferenceValue.from(ClassActor.fromJava(javaClass));
        }
    };

    /**
     * A field of type {@link ClassRegistry} injected into {@link ClassLoader}.
     */
    public static final InjectedReferenceFieldActor<ClassRegistry> ClassLoader_classRegistry = new InjectedReferenceFieldActor<ClassRegistry>(ClassLoader.class, ClassRegistry.class) {
        @HOSTED_ONLY
        @Override
        public ReferenceValue readInjectedValue(Object object) {
            assert object instanceof ClassLoader;
            return ReferenceValue.from(object == VMClassLoader.VM_CLASS_LOADER ? ClassRegistry.VM_CLASS_REGISTRY : ClassRegistry.BOOT_CLASS_REGISTRY);
        }
    };

    /**
     * A field of type {@link VmThread} injected into {@link Thread}.
     */
    public static final InjectedReferenceFieldActor<VmThread> Thread_vmThread = new InjectedReferenceFieldActor<VmThread>(Thread.class, VmThread.class) {
        @HOSTED_ONLY
        @Override
        public ReferenceValue readInjectedValue(Object object) {
            assert object instanceof Thread;
            return ReferenceValue.from(VmThread.mainThread);
        }
    };

    /**
     * A field of type {@link FieldActor} injected into {@link Field}.
     */
    public static final InjectedReferenceFieldActor<FieldActor> Field_fieldActor = new InjectedReferenceFieldActor<FieldActor>(Field.class, FieldActor.class) {
        @HOSTED_ONLY
        @Override
        public ReferenceValue readInjectedValue(Object object) {
            assert object instanceof Field;
            return ReferenceValue.from(FieldActor.fromJava((Field) object));
        }
    };

    /**
     * A field of type {@link MethodActor} injected into {@link Method}.
     */
    public static final InjectedReferenceFieldActor<MethodActor> Method_methodActor = new InjectedReferenceFieldActor<MethodActor>(Method.class, MethodActor.class) {
        @HOSTED_ONLY
        @Override
        public ReferenceValue readInjectedValue(Object object) {
            assert object instanceof Method;
            return ReferenceValue.from(MethodActor.fromJava((Method) object));
        }
    };

    /**
     * A field of type {@link MethodActor} injected into {@link Constructor}.
     */
    public static final InjectedReferenceFieldActor<MethodActor> Constructor_methodActor = new InjectedReferenceFieldActor<MethodActor>(Constructor.class, MethodActor.class) {
        @HOSTED_ONLY
        @Override
        public ReferenceValue readInjectedValue(Object object) {
            assert object instanceof Constructor;
            return ReferenceValue.from(MethodActor.fromJavaConstructor((Constructor) object));
        }
    };
}
