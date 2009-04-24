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
package com.sun.max.vm.classfile.constant;

import static com.sun.max.lang.Classes.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

import java.lang.reflect.*;

/**
 * All "context free" pool constants are created by this singleton factory. Context free pool constants are those that
 * do not have indexes to other pool constants and so can be shared by many ConstantPool instances.
 *
 * @author Doug Simon
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

    public static MethodRefConstant createMethodConstant(boolean isInterface, ClassActor holder, Utf8Constant name, SignatureDescriptor signature) {
        if (isInterface) {
            return createInterfaceMethodConstant(holder, name, signature);
        }
        return createClassMethodConstant(holder, name, signature);
    }

    public static FieldRefConstant createFieldConstant(Field javaField) {
        return new FieldRefConstant.Resolved(FieldActor.fromJava(javaField));
    }

    public static FieldRefConstant createFieldConstant(ClassActor holder, Utf8Constant name, TypeDescriptor type) {
        return new FieldRefConstant.Unresolved(holder, name, type);
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
