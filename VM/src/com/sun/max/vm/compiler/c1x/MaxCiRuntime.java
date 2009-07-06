/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The <code>MaxCiRuntime</code> class implements the runtime interface needed by C1X.
 * This includes access to runtime features such as class and method representations,
 * constant pools, as well as some compiler tuning.
 *
 * @author Ben L. Titzer
 */
public class MaxCiRuntime implements CiRuntime {

    public static final MaxCiRuntime globalRuntime = new MaxCiRuntime();

    final MaxCiConstantPool globalConstantPool = new MaxCiConstantPool(this, null);

    final WeakHashMap<MaxCiField, MaxCiField> fields = new WeakHashMap<MaxCiField, MaxCiField>();
    final WeakHashMap<MaxCiMethod, MaxCiMethod> methods = new WeakHashMap<MaxCiMethod, MaxCiMethod>();
    final WeakHashMap<MaxCiType, MaxCiType> types = new WeakHashMap<MaxCiType, MaxCiType>();
    final WeakHashMap<ConstantPool, MaxCiConstantPool> constantPools = new WeakHashMap<ConstantPool, MaxCiConstantPool>();

    /**
     * Gets the constant pool for a specified method.
     * @param method the compiler interface method
     * @return the compiler interface constant pool for the specified method
     */
    public CiConstantPool getConstantPool(CiMethod method) {
        final ClassMethodActor classMethodActor = this.asClassMethodActor(method, "getConstantPool()");
        final ConstantPool cp = classMethodActor.rawCodeAttribute().constantPool();
        synchronized (this) {
            MaxCiConstantPool constantPool = constantPools.get(cp);
            if (constantPool == null) {
                constantPool = new MaxCiConstantPool(this, cp);
                constantPools.put(cp, constantPool);
            }
            return constantPool;
        }
    }

    /**
     * Gets the OSR frame for a particular method at a particular bytecode index.
     * @param method the compiler interface method
     * @param bci the bytecode index
     * @return the OSR frame
     */
    public CiOsrFrame getOsrFrame(CiMethod method, int bci) {
        throw FatalError.unimplemented();
    }

    /**
     * Checks whether the runtime requires inlining of the specified method.
     * @param method the method to inline
     * @return <code>true</code> if the method must be inlined; <code>false</code>
     * to allow the compiler to use its own heuristics
     */
    public boolean mustInline(CiMethod method) {
        return asClassMethodActor(method, "mustInline()").isInline();
    }

    /**
     * Checks whether the runtime forbids inlining of the specified method.
     * @param method the method to inline
     * @return <code>true</code> if the runtime forbids inlining of the specified method;
     * <code>false</code> to allow the compiler to use its own heuristics
     */
    public boolean mustNotInline(CiMethod method) {
        return asClassMethodActor(method, "mustNotInline()").isNeverInline();
    }

    /**
     * Checks whether the runtime forbids compilation of the specified method.
     * @param method the method to compile
     * @return <code>true</code> if the runtime forbids compilation of the specified method;
     * <code>false</code> to allow the compiler to compile the method
     */
    public boolean mustNotCompile(CiMethod method) {
        return false;
    }

    /**
     * Performs an instanceof test on an object and a compiler interface type.
     * @param object the object to test
     * @param type the compiler interface type to test
     * @return <code>true</code> if the object is an instance of the specified class
     */
    public boolean instanceOf(Object object, CiType type) {
        return asClassActor(type, "instanceOf").isInstance(object);
    }

    /**
     * Performs a checkcast operation on an object and a compiler interface type.
     * @param object the object to cast
     * @param type the compiler interface type to cast to
     * @return the object if the cast fails
     * @throws ClassCastException if the cast fails
     */
    public Object checkCast(Object object, CiType type) {
        if (object == null || asClassActor(type, "instanceOf").isInstance(object)) {
            return object;
        }
        throw new ClassCastException();
    }

    /**
     * Allocates an object of the specified compiler interface type.
     * @param type the type
     * @return a non-initialized instance of the specified compiler interface type
     */
    public Object allocateObject(CiType type) {
        throw FatalError.unimplemented();
    }

    /**
     * Allocates an array with the specified element type.
     * @param type the element type
     * @param length the length of the array
     * @return an initialized array with the specified length and element type
     */
    public Object allocateArray(CiType type, int length) {
        throw FatalError.unimplemented();
    }

    /**
     * Resolves a compiler interface type by its name. Note that this
     * method should only be called for globally available classes (e.g. java.lang.*),
     * since it does not supply a constant pool.
     * @param name the name of the class
     * @return the compiler interface type for the class
     */
    public CiType resolveType(String name) {
        final ClassActor classActor = ClassRegistry.get((ClassLoader) null, JavaTypeDescriptor.getDescriptorForJavaString(name));
        if (classActor != null) {
            return globalConstantPool.canonicalCiType(classActor);
        }
        return null;
    }

    /**
     * Gets the compiler interface type for the specified Java class.
     * @param javaClass the java class object
     * @return the compiler interface type for the specified class
     */
    public CiType getType(Class<?> javaClass) {
        return globalConstantPool.canonicalCiType(ClassActor.fromJava(javaClass));
    }

    /**
     * Gets the <code>CiMethod</code> for a given method actor.
     * @param methodActor the method actor
     * @return the canonical compiler interface method for the method actor
     */
    public CiMethod getCiMethod(MethodActor methodActor) {
        return globalConstantPool.canonicalCiMethod(methodActor);
    }

    ClassMethodActor asClassMethodActor(CiMethod method, String operation) {
        if (method instanceof MaxCiMethod) {
            return ((MaxCiMethod) method).asClassMethodActor(operation);
        }
        throw new MaxCiUnresolved("invalid CiMethod instance: " + method.getClass());
    }

    ClassActor asClassActor(CiType type, String operation) {
        if (type instanceof MaxCiType) {
            return ((MaxCiType) type).asClassActor(operation);
        }
        throw new MaxCiUnresolved("invalid CiType instance: " + type.getClass());
    }
}
