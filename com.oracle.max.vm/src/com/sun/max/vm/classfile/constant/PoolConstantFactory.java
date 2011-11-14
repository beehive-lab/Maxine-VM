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
package com.sun.max.vm.classfile.constant;

import static com.sun.max.lang.Classes.*;

import java.lang.reflect.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * All "context free" pool constants are created by this singleton factory. Context free pool constants are those that
 * do not have indexes to other pool constants and so can be shared by many ConstantPool instances.
 */
public final class PoolConstantFactory {

    private PoolConstantFactory() {

    }

    /**
     * Creates a {@code ClassMethodRefConstant} for a Java constructor.
     */
    public static ClassMethodRefConstant createClassMethodConstant(Class clazz, Class... parameterTypes) {
        return createClassMethodConstant(getDeclaredConstructor(clazz, parameterTypes));
    }

    /**
     * Creates a {@code ClassMethodRefConstant} for a normal (i.e. non-constructor) Java method.
     */
    public static ClassMethodRefConstant createClassMethodConstant(Class clazz, Utf8Constant name, Class... parameterTypes) {
        return createClassMethodConstant(getDeclaredMethod(clazz, name.toString(), parameterTypes));
    }

    public static FieldRefConstant createFieldConstant(Class clazz, Utf8Constant name) {
        return createFieldConstant(getDeclaredField(clazz, name.toString()));
    }

    public static ClassMethodRefConstant createClassMethodConstant(ClassActor holder, Utf8Constant name, SignatureDescriptor signature) {
        return new ClassMethodRefConstant.Unresolved(holder, name, signature);
    }

    public static ClassMethodRefConstant createClassMethodConstant(Constructor javaConstructor) {
        return new ClassMethodRefConstant.Resolved(MethodActor.fromJavaConstructor(javaConstructor));
    }

    public static ClassMethodRefConstant createClassMethodConstant(Method javaMethod) {
        if (javaMethod.getDeclaringClass().isInterface()) {
            throw new IllegalArgumentException("expected non-interface method");
        }
        return new ClassMethodRefConstant.Resolved(MethodActor.fromJava(javaMethod));
    }

    public static InterfaceMethodRefConstant createInterfaceMethodConstant(Method javaMethod) {
        if (!javaMethod.getDeclaringClass().isInterface()) {
            throw new IllegalArgumentException("expected interface method");
        }
        return new InterfaceMethodRefConstant.Resolved(MethodActor.fromJava(javaMethod));
    }

    public static InterfaceMethodRefConstant createInterfaceMethodConstant(ClassActor holder, Utf8Constant name, SignatureDescriptor signature) {
        return new InterfaceMethodRefConstant.Unresolved(holder, name, signature);
    }

    public static MethodRefConstant createMethodConstant(Method javaMethod) {
        if (javaMethod.getDeclaringClass().isInterface()) {
            return createInterfaceMethodConstant(javaMethod);
        }
        return createClassMethodConstant(javaMethod);
    }

    public static FieldRefConstant createFieldConstant(Field javaField) {
        return new FieldRefConstant.Resolved(FieldActor.fromJava(javaField));
    }

    public static FieldRefConstant createFieldConstant(ClassActor holder, Utf8Constant name, TypeDescriptor type) {
        return new FieldRefConstant.Unresolved(holder, name, type);
    }

    public static ClassConstant createClassConstant(ClassActor classActor) {
        return new ClassConstant.Resolved(classActor);
    }

    public static ClassConstant createClassConstant(Class javaClass) {
        return new ClassConstant.Resolved(ClassActor.fromJava(javaClass));
    }

    public static ClassConstant createClassConstant(TypeDescriptor javaClass) {
        return new ClassConstant.Unresolved(javaClass);
    }

    public static NameAndTypeConstant createNameAndTypeConstant(Utf8Constant name, Descriptor descriptor) {
        return new NameAndTypeConstant(name, descriptor);
    }

    public static NameAndTypeConstant createNameAndTypeConstant(Utf8Constant name, Utf8Constant descriptor) {
        return new NameAndTypeConstant(name, descriptor);
    }

    public static Utf8Constant makeUtf8Constant(String utf8) {
        return SymbolTable.makeSymbol(utf8);
    }

    public static IntegerConstant createIntegerConstant(int value) {
        return new IntegerConstant(value);
    }

    public static StringConstant createStringConstant(String string) {
        return new StringConstant(string);
    }

    public static ObjectConstant createObjectConstant(Object object) {
        return new ObjectConstant(object);
    }

    public static FloatConstant createFloatConstant(float value) {
        return new FloatConstant(value);
    }

    public static LongConstant createLongConstant(long value) {
        return new LongConstant(value);
    }

    public static DoubleConstant createDoubleConstant(double value) {
        return new DoubleConstant(value);
    }
}
