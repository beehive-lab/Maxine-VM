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
/*VCSID=877defcc-491c-4d46-bae5-cb5997d96f0b*/
package com.sun.max.vm.jdk;

import java.lang.reflect.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * @author Bernd Mathiske
 */
public final class ConstantPoolAdapter extends sun.reflect.ConstantPool {

    private final com.sun.max.vm.classfile.constant.ConstantPool _constantPool;

    ConstantPoolAdapter(com.sun.max.vm.classfile.constant.ConstantPool constantPool) {
        _constantPool = constantPool;
    }

    @Override
    public int getSize() {
        return _constantPool.numberOfConstants();
    }

    @Override
    public Class getClassAt(int index) {
        return _constantPool.classAt(index).resolve(_constantPool, index).toJava();
    }

    @Override
    public Class getClassAtIfLoaded(int index) {
        final ClassConstant classRef = _constantPool.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(_constantPool)) {
            return getClassAt(index);
        }
        return null;
    }

    @Override
    public Member getMethodAt(int index) {
        final MethodActor methodActor = _constantPool.methodAt(index).resolve(_constantPool, index);
        if (methodActor.isInstanceInitializer()) {
            return methodActor.toJavaConstructor();
        }
        return methodActor.toJava();
    }

    @Override
    public Member getMethodAtIfLoaded(int index) {
        if (_constantPool.methodAt(index).isResolvableWithoutClassLoading(_constantPool)) {
            return getMethodAt(index);
        }
        return null;
    }

    @Override
    public Field getFieldAt(int index) {
        return _constantPool.fieldAt(index).resolve(_constantPool, index).toJava();
    }

    @Override
    public Field getFieldAtIfLoaded(int index) {
        if (_constantPool.fieldAt(index).isResolvableWithoutClassLoading(_constantPool)) {
            getFieldAt(index);
        }
        return null;
    }

    @Override
    public String[] getMemberRefInfoAt(int index) {
        final String holder;
        final String name;
        final String descriptor;
        final MemberRefConstant member = _constantPool.memberAt(index);
        holder = member.holder(_constantPool).toString();
        name = member.name(_constantPool).toString();
        descriptor = member.descriptor(_constantPool).toString();
        return new String[]{holder, name, descriptor};
    }

    @Override
    public int getIntAt(int index) {
        return _constantPool.intAt(index);
    }

    @Override
    public long getLongAt(int index) {
        return _constantPool.longAt(index);
    }

    @Override
    public float getFloatAt(int index) {
        return _constantPool.floatAt(index);
    }

    @Override
    public double getDoubleAt(int index) {
        return _constantPool.doubleAt(index);
    }

    @Override
    public String getStringAt(int index) {
        return _constantPool.stringAt(index);
    }

    @Override
    public String getUTF8At(int index) {
        return _constantPool.utf8At(index, null).toString();
    }
}
