/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vm.ext.graal;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.meta.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;

/**
 * Likely temporary indirect between a {@link MethodActor} and a {@link ResolvedJavaMethod},
 * since {@code MethodActor} already implements the old {@link RiResolvedMethod} interface.
 */
public class MaxResolvedJavaMethod implements ResolvedJavaMethod {

    private static ConcurrentHashMap<MethodActor, MaxResolvedJavaMethod> map = new ConcurrentHashMap<MethodActor, MaxResolvedJavaMethod>();

    private MethodActor methodActor;

    static ResolvedJavaMethod get(MethodActor methodActor) {
        MaxResolvedJavaMethod result = map.get(methodActor);
        if (result == null) {
            result = new MaxResolvedJavaMethod(methodActor);
            map.put(methodActor, result);
        }
        return result;
    }

    private MaxResolvedJavaMethod(MethodActor methodActor) {
        this.methodActor = methodActor;
    }

    @Override
    public String getName() {
        unimplemented();
        return null;
    }

    @Override
    public Signature getSignature() {
        unimplemented();
        return null;
    }

    @Override
    public byte[] getCode() {
        unimplemented();
        return null;
    }

    @Override
    public int getCodeSize() {
        unimplemented();
        return 0;
    }

    @Override
    public int getCompiledCodeSize() {
        unimplemented();
        return 0;
    }

    @Override
    public int getCompilationComplexity() {
        unimplemented();
        return 0;
    }

    @Override
    public ResolvedJavaType getDeclaringClass() {
        unimplemented();
        return null;
    }

    @Override
    public int getMaxLocals() {
        unimplemented();
        return 0;
    }

    @Override
    public int getMaxStackSize() {
        unimplemented();
        return 0;
    }

    @Override
    public int getModifiers() {
        unimplemented();
        return 0;
    }

    @Override
    public boolean isClassInitializer() {
        unimplemented();
        return false;
    }

    @Override
    public boolean isConstructor() {
        unimplemented();
        return false;
    }

    @Override
    public boolean canBeStaticallyBound() {
        unimplemented();
        return false;
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers() {
        unimplemented();
        return null;
    }

    @Override
    public StackTraceElement asStackTraceElement(int bci) {
        unimplemented();
        return null;
    }

    @Override
    public ProfilingInfo getProfilingInfo() {
        unimplemented();
        return null;
    }

    @Override
    public Map<Object, Object> getCompilerStorage() {
        unimplemented();
        return null;
    }

    @Override
    public ConstantPool getConstantPool() {
        unimplemented();
        return null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        unimplemented();
        return null;
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        unimplemented();
        return null;
    }

    @Override
    public Type[] getGenericParameterTypes() {
        unimplemented();
        return null;
    }

    @Override
    public boolean canBeInlined() {
        unimplemented();
        return false;
    }

    private static void unimplemented() {
        ProgramError.unexpected("unimplemented");
    }

}
