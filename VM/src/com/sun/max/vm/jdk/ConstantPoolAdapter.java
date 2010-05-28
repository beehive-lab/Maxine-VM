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
package com.sun.max.vm.jdk;

import java.lang.reflect.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * @author Bernd Mathiske
 */
public final class ConstantPoolAdapter extends sun.reflect.ConstantPool {

    private final com.sun.max.vm.classfile.constant.ConstantPool constantPool;

    ConstantPoolAdapter(com.sun.max.vm.classfile.constant.ConstantPool constantPool) {
        this.constantPool = constantPool;
    }

    @Override
    public int getSize() {
        return constantPool.numberOfConstants();
    }

    @Override
    public Class getClassAt(int index) {
        return constantPool.classAt(index).resolve(constantPool, index).toJava();
    }

    @Override
    public Class getClassAtIfLoaded(int index) {
        final ClassConstant classRef = constantPool.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(constantPool)) {
            return getClassAt(index);
        }
        return null;
    }

    @Override
    public Member getMethodAt(int index) {
        final MethodActor methodActor = constantPool.methodAt(index).resolve(constantPool, index);
        if (methodActor.isInstanceInitializer()) {
            return methodActor.toJavaConstructor();
        }
        return methodActor.toJava();
    }

    @Override
    public Member getMethodAtIfLoaded(int index) {
        if (constantPool.methodAt(index).isResolvableWithoutClassLoading(constantPool)) {
            return getMethodAt(index);
        }
        return null;
    }

    @Override
    public Field getFieldAt(int index) {
        return constantPool.fieldAt(index).resolve(constantPool, index).toJava();
    }

    @Override
    public Field getFieldAtIfLoaded(int index) {
        if (constantPool.fieldAt(index).isResolvableWithoutClassLoading(constantPool)) {
            getFieldAt(index);
        }
        return null;
    }

    @Override
    public String[] getMemberRefInfoAt(int index) {
        final String holder;
        final String name;
        final String descriptor;
        final MemberRefConstant member = constantPool.memberAt(index);
        holder = member.holder(constantPool).toString();
        name = member.name(constantPool).toString();
        descriptor = member.descriptor(constantPool).toString();
        return new String[]{holder, name, descriptor};
    }

    @Override
    public int getIntAt(int index) {
        return constantPool.intAt(index);
    }

    @Override
    public long getLongAt(int index) {
        return constantPool.longAt(index);
    }

    @Override
    public float getFloatAt(int index) {
        return constantPool.floatAt(index);
    }

    @Override
    public double getDoubleAt(int index) {
        return constantPool.doubleAt(index);
    }

    @Override
    public String getStringAt(int index) {
        return constantPool.stringAt(index);
    }

    @Override
    public String getUTF8At(int index) {
        return constantPool.utf8At(index, null).toString();
    }
}
