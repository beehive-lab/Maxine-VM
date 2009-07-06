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
import com.sun.c1x.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
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

    @Override
    public int alignObjectSize(int size) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int arrayLengthOffsetInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean dtraceMethodProbes() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CiConstantPool getConstantPool(CiMethod method) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CiOsrFrame getOsrFrame(CiMethod method, int bci) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Address getRuntimeEntry(CiRuntimeCall runtimeCall) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int headerSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isMP() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int javaNioBufferLimitOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean jvmtiCanPostExceptions() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int klassJavaMirrorOffsetInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int klassOffsetInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean mustInline(CiMethod method) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean mustNotCompile(CiMethod method) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean mustNotInline(CiMethod method) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needsExplicitNullCheck(int offset) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int threadExceptionOopOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int threadExceptionPcOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int threadObjOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Address throwCountAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int vtableEntryMethodOffsetInBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int vtableEntrySize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int vtableStartOffset() {
        // TODO Auto-generated method stub
        return 0;
    }
}
