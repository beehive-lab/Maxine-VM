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

import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.meta.ProfilingInfo.ExceptionSeen;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.member.*;

/**
 * Likely temporary indirect between a {@link MethodActor} and a {@link ResolvedJavaMethod},
 * since {@code MethodActor} already implements the old {@link RiResolvedMethod} interface.
 */
public class MaxResolvedJavaMethod extends MaxJavaMethod implements ResolvedJavaMethod {

    protected MaxResolvedJavaMethod(RiResolvedMethod riResolvedMethod) {
        super(riResolvedMethod);
    }

    private RiResolvedMethod riResolvedMethod() {
        return (RiResolvedMethod) riMethod;
    }

    static MaxResolvedJavaMethod get(RiResolvedMethod riMethod) {
        return (MaxResolvedJavaMethod) MaxJavaMethod.get(riMethod);
    }

    static RiResolvedMethod get(ResolvedJavaMethod resolvedJavaMethod) {
        return (RiResolvedMethod) MaxJavaMethod.get(resolvedJavaMethod);
    }

    @Override
    public byte[] getCode() {
        return riResolvedMethod().code();
    }

    @Override
    public int getCodeSize() {
        return riResolvedMethod().codeSize();
    }

    @Override
    public int getCompiledCodeSize() {
        unimplemented("ResolvedMethod.getCompiledCodeSize");
        return 0;
    }

    @Override
    public int getCompilationComplexity() {
        unimplemented("ResolvedMethod.getCompilationComplexity");
        return 0;
    }

    @Override
    public ResolvedJavaType getDeclaringClass() {
        return MaxResolvedJavaType.get(riResolvedMethod().holder());
    }

    @Override
    public int getMaxLocals() {
        return riResolvedMethod().maxLocals();
    }

    @Override
    public int getMaxStackSize() {
        return riResolvedMethod().maxStackSize();
    }

    @Override
    public int getModifiers() {
        return riResolvedMethod().accessFlags();
    }

    @Override
    public boolean isClassInitializer() {
        return riResolvedMethod().isClassInitializer();
    }

    @Override
    public boolean isConstructor() {
        return riResolvedMethod().isConstructor();
    }

    @Override
    public boolean canBeStaticallyBound() {
        return riResolvedMethod().canBeStaticallyBound();
    }

    @Override
    public ExceptionHandler[] getExceptionHandlers() {
        RiExceptionHandler[] riExHandlers = riResolvedMethod().exceptionHandlers();
        ExceptionHandler[] exHandlers = new ExceptionHandler[riExHandlers.length];
        for (int i = 0; i < riExHandlers.length; i++) {
            RiExceptionHandler riEx = riExHandlers[i];
            exHandlers[i] = new ExceptionHandler(riEx.startBCI(), riEx.endBCI(), riEx.handlerBCI(),
                            riEx.catchTypeCPI(), MaxJavaType.get(riEx.catchType()));
        }
        return exHandlers;
    }

    @Override
    public StackTraceElement asStackTraceElement(int bci) {
        unimplemented("ResolvedMethod.asStackTraceElement");
        return null;
    }

    @Override
    public ProfilingInfo getProfilingInfo() {
        // We do not want to deal with exception handling right now, so just assume nothing throws an exception...
        return DefaultProfilingInfo.get(ExceptionSeen.FALSE);
    }

    private final Map<Object, Object> compilerStorage = new ConcurrentHashMap<Object, Object>();

    @Override
    public Map<Object, Object> getCompilerStorage() {
        return compilerStorage;
    }

    @Override
    public ConstantPool getConstantPool() {
        return MaxConstantPool.get(riResolvedMethod().getConstantPool());
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return riResolvedMethod().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return riResolvedMethod().getParameterAnnotations();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return riResolvedMethod().getGenericParameterTypes();
    }

    @Override
    public boolean canBeInlined() {
        // TODO implement properly. for now we want the invoke
        return false;
    }

}
