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

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * An implementation of {@link JJVMTIMax} that reuses as much as possible of the native JVMTI implementation.
 * An actual agent should subclass this class and provide implementations of the callbacks that it wishes to handle.
 * See {@link JJVMTICommon.EventCallbacks} and {@link JJVMTIMax.EventCallbacksMax}.
 *
 * Although this class is not directly referenced by the VM code, we want it to be fully compiled into the boot image
 * so we define every method as a {@link CriticalMethod}.
 *
 * TODO: complete the implementation.
 */
public class JJVMTIMaxAgentAdapter extends JJVMTICommonAgentAdapter implements JJVMTIMax, JJVMTIMax.EventCallbacksMax {

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
    public static void register(JJVMTIMaxAgentAdapter agent) {
        agent.registerEnv(new JVMTI.JavaEnvMax(agent));
    }

    @Override
    public void setBreakpoint(MethodActor method, long location) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void clearBreakpoint(MethodActor method, long location) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void setFieldAccessWatch(FieldActor field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void clearFieldAccessWatch(FieldActor field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void setFieldModificationWatch(FieldActor field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void clearFieldModificationWatch(FieldActor field) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isModifiableClassActor(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getClassActorSignature(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public int getClassActorStatus(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getSourceFileName(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public int getClassActorModifiers(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public MethodActor[] getClassActorMethodActors(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public FieldActor[] getClassActorFieldActors(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public ClassActor[] getImplementedInterfaces(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public boolean isInterface(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public boolean isArrayClassActor(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public ClassLoader getClassLoader(ClassActor klass) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getFieldActorName(FieldActor field) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getFieldActorSignature(FieldActor field) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public ClassActor getFieldActorDeclaringClassActor(FieldActor field) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public int getFieldActorModifiers(FieldActor field) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public boolean isFieldActorSynthetic(FieldActor field) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getMethodActorName(MethodActor method) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getMethodActorSignature(MethodActor method) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public String getMethodActorGenericSignature(MethodActor method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public ClassActor getMethodActorDeclaringClassActor(MethodActor method) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public int getMethodActorModifiers(MethodActor method) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public int getMaxLocals(MethodActor method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getArgumentsSize(MethodActor method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public LineNumberEntry[] getLineNumberTable(MethodActor method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public MethodLocation getMethodActorLocation(MethodActor method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public LocalVariableEntry[] getLocalVariableTable(MethodActor methodActor) throws JJVMTIException {
        return JVMTIClassFunctions.getLocalVariableTable((ClassMethodActor) methodActor);
    }

    @Override
    public byte[] getBytecodes(MethodActor method, byte[] useThis) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isMethodActorNative(MethodActor method) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isMethodActorSynthetic(MethodActor method) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public ClassActor[] getLoadedClasses() throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public ClassActor[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public String getSourceDebugExtension(ClassActor klass) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public boolean isMethodActorObsolete(MethodActor method) throws JJVMTIException {
        throw notImplemented;
    }

    @Override
    public ClassVersionInfo getClassVersionNumbers(ClassActor klasss, ClassVersionInfo classVersionInfo) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public int getConstantPool(ClassActor klass, byte[] pool) throws JJVMTIException {
        throw notImplemented;

    }

    @Override
    public void retransformClasses(ClassActor[] klasses) throws JJVMTIException {
        throw notImplemented;

    }

    // default empty implementations of the event callbacks

    @Override
    public void breakpoint(Thread thread, MethodActor method, long location) {
    }

    @Override
    public void classLoad(Thread thread, ClassActor klass) {
    }

    @Override
    public void methodEntry(Thread thread, MethodActor method) {
    }

    @Override
    public void methodExit(Thread thread, MethodActor method, boolean exeception, Object returnValue) {
    }


}
