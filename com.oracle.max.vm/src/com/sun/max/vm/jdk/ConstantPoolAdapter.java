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
package com.sun.max.vm.jdk;

import java.lang.reflect.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
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
