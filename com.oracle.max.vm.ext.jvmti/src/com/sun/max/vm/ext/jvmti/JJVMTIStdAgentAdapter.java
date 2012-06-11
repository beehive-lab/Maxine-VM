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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * An implementation of {@link JJVMTIStd} that reuses as much as possible of the native JVMTI implementation.
 * An actual agent should subclass this class and provide implementations of the callbacks that it wishes to handle.
 * See {@link JJVMTICommon.EventCallbacks} and {@link JJVMTIStd.EventCallbacksStd}.
 *
 * Although this class is not directly referenced by the VM code, we want it to be fully compiled into the boot image
 * so we define every method as a {@link CriticalMethod}.
 *
 * TODO: complete the implementation.
 */
public class JJVMTIStdAgentAdapter extends JJVMTICommonAgentAdapter implements JJVMTIStd, JJVMTIStd.EventCallbacksStd {

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {
        @Override
        public void initializationComplete() {
            registerCriticalMethods(JJVMTIStdAgentAdapter.class);
        }
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    /**
     * Register an agent.
     * @param the agent implementation subclass
     */
    public static void register(JJVMTIStdAgentAdapter agent) {
        agent.registerEnv(new JVMTI.JavaEnvStd(agent));
    }

    @Override
    public void setBreakpoint(Member method, long location) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void clearBreakpoint(Member method, long location) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void setFieldAccessWatch(Field field) throws JJVMTIException {
        JVMTIFieldWatch.setWatch(FieldActor.fromJava(field), JVMTIFieldWatch.ACCESS_STATE);
    }

    @Override
    public void clearFieldAccessWatch(Field field) throws JJVMTIException {
        JVMTIFieldWatch.clearWatch(FieldActor.fromJava(field), JVMTIFieldWatch.ACCESS_STATE);
    }

    @Override
    public void setFieldModificationWatch(Field field) throws JJVMTIException {
        JVMTIFieldWatch.setWatch(FieldActor.fromJava(field), JVMTIFieldWatch.MODIFICATION_STATE);
    }

    @Override
    public void clearFieldModificationWatch(Field field) throws JJVMTIException {
        JVMTIFieldWatch.clearWatch(FieldActor.fromJava(field), JVMTIFieldWatch.MODIFICATION_STATE);
    }

    @Override
    public boolean isModifiableClass(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void iterateThroughHeap(int filter, Class<?> klass, HeapCallbacks heapCallbacks, Object userData) throws JJVMTIException {
        JVMTIHeapFunctions.iterateThroughHeap(env, filter, klass, heapCallbacks, userData);
    }

    @Override
    public String getClassSignature(Class<?> klass) throws JJVMTIException {
        return ClassActor.fromJava(klass).typeDescriptor.string;
    }

    @Override
    public int getClassStatus(Class<?> klass) throws JJVMTIException {
        return JVMTIClassFunctions.getClassStatus(klass);
    }

    @Override
    public String getSourceFileName(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getClassModifiers(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public Method[] getClassMethods(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public Field[] getClassFields(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public Class[] getImplementedInterfaces(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isInterface(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isArrayClass(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public ClassLoader getClassLoader(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getFieldName(Field field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getFieldSignature(Field field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public Class getFieldDeclaringClass(Field field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getFieldModifiers(Field field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isFieldSynthetic(Field field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getMethodName(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getMethodSignature(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getMethodGenericSignature(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public Class getMethodDeclaringClass(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getMethodModifiers(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getMaxLocals(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getArgumentsSize(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public LineNumberEntry[] getLineNumberTable(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public MethodLocation getMethodLocation(Member method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public LocalVariableEntry[] getLocalVariableTable(Member method) throws JJVMTIException {
        ClassMethodActor classMethodActor = checkMember(method);
        return JVMTIClassFunctions.getLocalVariableTable(classMethodActor);
    }

    @Override
    public byte[] getBytecodes(Member method, byte[] useThis) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isMethodNative(Method method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isMethodSynthetic(Method method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public Class<?>[] getLoadedClasses() throws JJVMTIException {
        return JVMTIClassFunctions.getLoadedClasses();
    }

    @Override
    public Class<?>[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getSourceDebugExtension(Class<?> klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isMethodObsolete(Method method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public ClassVersionInfo getClassVersionNumbers(Class<?> klasss, ClassVersionInfo classVersionInfo) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getConstantPool(Class<?> klass, byte[] pool) throws JJVMTIException {
        throw notImplemented;

    }


    @Override
    public void retransformClasses(Class<?>[] klasses) throws JJVMTIException {
        throw notImplemented;

    }

    private static ClassMethodActor checkMember(Member member) {
        if (member instanceof Method) {
            return ClassMethodActor.fromJava((Method) member);
        } else if (member instanceof Constructor) {
            return (ClassMethodActor) ClassMethodActor.fromJavaConstructor((Constructor) member);
        } else {
            throw new JJVMTIException(JVMTI_ERROR_INVALID_METHODID);
        }

    }

    // default empty implementations of the event callbacks

    @Override
    public void breakpoint(Thread thread, Member method, long location) {
    }

    @Override
    public void classLoad(Thread thread, Class<?> klass) {
    }

    @Override
    public void methodEntry(Thread thread, Member method) {
    }

    @Override
    public void methodExit(Thread thread, Member method, boolean exeception, Object returnValue) {
    }

    @Override
    public void fieldAccess(Thread thread, Method method, long location, Class<?> klass, Object object, Field field) {
    }

    @Override
    public void fieldModification(Thread thread, Method method, long location, Class<?> klass, Object object, Field field, Object newValue) {
    }

}
