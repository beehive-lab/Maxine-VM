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
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.ext.jvmti.JVMTIEvents.E;
import com.sun.max.vm.ext.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The core implementation of {@link JJVMTI}. The standard idiom is to subclass {@link NullJJVMTICallbacks},
 * which extends this class, overriding those {@link JJVMTI.EventCallbacks} that the agent wants to handle.
 * Checking that the agent has the {@link JVMTICapabilities.E capability} to invoke a given method is made
 * by calling {@code super.method(...)} which access the automatically generated {@link JJVMTIAgentAdapterChecker}.
 * The call is inlined away in the generated code of the boot image.
 */
public class JJVMTIAgentAdapter extends JJVMTIAgentAdapterChecker implements JJVMTI {

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    protected static void registerCriticalMethods(Class<?> klass) {
        for (Method m : klass.getDeclaredMethods()) {
            new CriticalMethod(ClassMethodActor.fromJava(m), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        }
    }

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {
        @Override
        public void initializationComplete() {
            registerCriticalMethods(JJVMTIAgentAdapter.class);
        }
    }

    protected static final JJVMTIException notImplemented = new JJVMTIException(JVMTI_ERROR_NOT_AVAILABLE);

    /**
     * Register an agent.
     * @param the agent implementation subclass
     */
    public static JJVMTIAgentAdapter register(JJVMTIAgentAdapter agent) {
        agent.registerEnv(new JVMTI.JavaEnv((JJVMTI.EventCallbacks) agent));
        return agent;
    }

    protected void registerEnv(JVMTI.JavaEnv env) {
        this.env = env;
        JVMTI.setJVMTIJavaEnv(env);
    }

    @Override
    public void setEventNotificationMode(int mode, E event, Thread thread) throws JJVMTIException {
        super.setEventNotificationMode(mode, event, thread);
        int error = JVMTIEvents.setEventNotificationMode(env, mode, event, thread);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTIException(error);
        }
    }

    @Override
    public Thread[] getAllThreads() throws JJVMTIException {
        super.getAllThreads();
        return VmThreadMap.getThreads(true);
    }

    @Override
    public void suspendThread(Thread thread) throws JJVMTIException {
        super.suspendThread(thread);
        int error = JVMTIThreadFunctions.suspendThread(env, thread);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTIException(error);
        }
    }

    @Override
    public void resumeThread(Thread thread) throws JJVMTIException {
        super.resumeThread(thread);
        int error = JVMTIThreadFunctions.resumeThread(env, thread);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTIException(error);
        }
    }

    @Override
    public void stopThread(Thread thread, Throwable t) throws JJVMTIException {
        super.stopThread(thread, t);
        throw notImplemented; // TODO

    }

    @Override
    public void interruptThread(Thread thread) throws JJVMTIException {
        super.interruptThread(thread);
        JVMTIThreadFunctions.interruptThread(thread);
    }

    @Override
    public ThreadInfo getThreadInfo(Thread thread) throws JJVMTIException {
        super.getThreadInfo(thread);
        return JVMTIThreadFunctions.getThreadInfo(thread);
    }

    @Override
    public void getOwnedMonitorInfo(Thread thread) throws JJVMTIException {
        super.getOwnedMonitorInfo(thread);
        throw notImplemented; // TODO

    }

    @Override
    public Object getCurrentContendedMonitor(Thread thread) throws JJVMTIException {
        super.getCurrentContendedMonitor(thread);
        throw notImplemented; // TODO
    }

    @Override
    public void runAgentThread(Thread thread, int priority) throws JJVMTIException {
        super.runAgentThread(thread, priority);
        JVMTI.runAgentThread(thread, priority);
    }

    @Override
    public ThreadGroup[] getTopThreadGroups() throws JJVMTIException {
        super.getTopThreadGroups();
        ThreadGroup[] result = new ThreadGroup[1];
        result[0] = VmThread.systemThreadGroup;
        return result;
    }

    @Override
    public ThreadGroupInfo getThreadGroupInfo(ThreadGroup tg) throws JJVMTIException {
        super.getThreadGroupInfo(tg);
        return new ThreadGroupInfo(tg.getParent(), tg.getName(), tg.getMaxPriority(), tg.isDaemon());
    }

    @Override
    public ThreadGroupChildrenInfo getThreadGroupChildren(ThreadGroup threadGroup) throws JJVMTIException {
        super.getThreadGroupChildren(threadGroup);
        return JVMTIThreadFunctions.getThreadGroupChildren(threadGroup);
    }

    @Override
    public int getFrameCount(Thread thread) throws JJVMTIException {
        super.getFrameCount(thread);
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            throw new JJVMTIException(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        FindAppFramesStackTraceVisitor stackTraceVisitor = SingleThreadStackTraceVmOperation.invoke(vmThread);
        return stackTraceVisitor.stackElements.size();
    }

    @Override
    public int getThreadState(Thread thread) throws JJVMTIException {
        super.getThreadState(thread);
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            throw new JJVMTIException(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        return JVMTIThreadFunctions.getThreadState(vmThread);
    }

    @Override
    public Thread getCurrentThread() throws JJVMTIException {
        super.getCurrentThread();
        return VmThread.current().javaThread();
    }

    @Override
    public FrameInfo getFrameLocation(Thread thread, int depth) throws JJVMTIException {
        super.getFrameLocation(thread, depth);
        return JVMTIThreadFunctions.getFrameLocation(thread, depth);
    }

    @Override
    public void notifyFramePop(Thread thread, int depth) throws JJVMTIException {
        super.notifyFramePop(thread, depth);
        throw notImplemented; // TODO

    }

    @Override
    public Object getLocalObject(Thread thread, int depth, int slot) throws JJVMTIException {
        super.getLocalObject(thread, depth, slot);
        return JVMTIThreadFunctions.getLocalObject(thread, depth, slot);
    }

    @Override
    public int getLocalInt(Thread thread, int depth, int slot) throws JJVMTIException {
        super.getLocalInt(thread, depth, slot);
        return JVMTIThreadFunctions.getLocalInt(thread, depth, slot);
    }

    @Override
    public long getLocalLong(Thread thread, int depth, int slot) throws JJVMTIException {
        super.getLocalLong(thread, depth, slot);
        return JVMTIThreadFunctions.getLocalLong(thread, depth, slot);
    }

    @Override
    public float getLocalFloat(Thread thread, int depth, int slot) throws JJVMTIException {
        super.getLocalFloat(thread, depth, slot);
        return JVMTIThreadFunctions.getLocalFloat(thread, depth, slot);
    }

    @Override
    public double getLocalDouble(Thread thread, int depth, int slot) throws JJVMTIException {
        super.getLocalDouble(thread, depth, slot);
        return JVMTIThreadFunctions.getLocalDouble(thread, depth, slot);
    }

    @Override
    public void setLocalObject(Thread thread, int depth, int slot, Object value) throws JJVMTIException {
        super.setLocalObject(thread, depth, slot, value);
        JVMTIThreadFunctions.setLocalObject(thread, depth, slot, value);
    }

    @Override
    public void setLocalInt(Thread thread, int depth, int slot, int value) throws JJVMTIException {
        super.setLocalInt(thread, depth, slot, value);
        JVMTIThreadFunctions.setLocalInt(thread, depth, slot, value);
    }

    @Override
    public void setLocalLong(Thread thread, int depth, int slot, long value) throws JJVMTIException {
        super.setLocalLong(thread, depth, slot, value);
        JVMTIThreadFunctions.setLocalLong(thread, depth, slot, value);
    }

    @Override
    public void setLocalFloat(Thread thread, int depth, int slot, float value) throws JJVMTIException {
        super.setLocalFloat(thread, depth, slot, value);
        JVMTIThreadFunctions.setLocalFloat(thread, depth, slot, value);
    }

    @Override
    public void setLocalDouble(Thread thread, int depth, int slot, double value) throws JJVMTIException {
        super.setLocalDouble(thread, depth, slot, value);
        JVMTIThreadFunctions.setLocalDouble(thread, depth, slot, value);
    }

    @Override
    public int getObjectHashCode(Object object) throws JJVMTIException {
        super.getObjectHashCode(object);
        return System.identityHashCode(object);
    }

    @Override
    public ObjectMonitorUsage getObjectMonitorUsage(Object object) throws JJVMTIException {
        super.getObjectMonitorUsage(object);
        throw notImplemented; // TODO

    }

    @Override
    public void setNativeMethodPrefix(String prefix) throws JJVMTIException {
        super.setNativeMethodPrefix(prefix);
        throw notImplemented; // TODO

    }

    @Override
    public void setNativeMethodPrefixes(String[] prefixes) throws JJVMTIException {
        super.setNativeMethodPrefixes(prefixes);
        throw notImplemented; // TODO

    }

    @Override
    public void popFrame(Thread thread) throws JJVMTIException {
        super.popFrame(thread);
        throw notImplemented; // TODO

    }

    @Override
    public void forceEarlyReturnObject(Thread thread, Object value) throws JJVMTIException {
        super.forceEarlyReturnObject(thread, value);
        throw notImplemented; // TODO

    }

    @Override
    public void forceEarlyReturnInt(Thread thread, int value) throws JJVMTIException {
        super.forceEarlyReturnInt(thread, value);
        throw notImplemented; // TODO

    }

    @Override
    public void forceEarlyReturnLong(Thread thread, long value) throws JJVMTIException {
        super.forceEarlyReturnLong(thread, value);
        throw notImplemented; // TODO

    }

    @Override
    public void forceEarlyReturnFloat(Thread thread, float value) throws JJVMTIException {
        super.forceEarlyReturnFloat(thread, value);
        throw notImplemented; // TODO

    }

    @Override
    public void forceEarlyReturnDouble(Thread thread, double value) throws JJVMTIException {
        super.forceEarlyReturnDouble(thread, value);
        throw notImplemented; // TODO

    }

    @Override
    public void forceEarlyReturnVoid(Thread thread) throws JJVMTIException {
        super.forceEarlyReturnVoid(thread);
        throw notImplemented; // TODO

    }

    @Override
    public int getVersionNumber() throws JJVMTIException {
        super.getVersionNumber();
        return JVMTI_VERSION;
    }

    @Override
    public EnumSet<JVMTICapabilities.E> getCapabilities() throws JJVMTIException {
        super.getCapabilities();
        return env.capabilities;
    }

    @Override
    public int[] suspendThreadList(Thread[] threads) throws JJVMTIException {
        super.suspendThreadList(threads);
        return JVMTIThreadFunctions.suspendThreadList(env, threads);
    }

    @Override
    public int[] resumeThreadList(Thread[] threads) throws JJVMTIException {
        super.resumeThreadList(threads);
        return JVMTIThreadFunctions.resumeThreadList(env, threads);
    }

    @Override
    public StackInfo[] getAllStackTraces(int maxFrameCount) throws JJVMTIException {
        super.getAllStackTraces(maxFrameCount);
        return JVMTIThreadFunctions.getAllStackTraces(maxFrameCount);
    }

    @Override
    public StackInfo[] getThreadListStackTraces(Thread[] threads, int maxFrameCount) throws JJVMTIException {
        super.getThreadListStackTraces(threads, maxFrameCount);
        return JVMTIThreadFunctions.getThreadListStackTraces(threads, maxFrameCount);
    }

    private static class ObjectThreadLocal extends ThreadLocal<Object> {
        @Override
        public Object initialValue() {
            return null;
        }
    }

    @Override
    public FrameInfo[] getStackTrace(Thread thread, int startDepth, int maxFrameCount) throws JJVMTIException {
        super.getStackTrace(thread, startDepth, maxFrameCount);
        return JVMTIThreadFunctions.getStackTrace(thread, startDepth, maxFrameCount);
    }

    @Override
    public Object getTag(Object object) throws JJVMTIException {
        super.getTag(object);
        return env.tags.getTag(object);
    }

    @Override
    public void setTag(Object object, Object tag) throws JJVMTIException {
        super.setTag(object, tag);
        env.tags.setTag(object, tag);
    }

    @Override
    public void forceGarbageCollection() throws JJVMTIException {
        super.forceGarbageCollection();
        System.gc();
    }

    @Override
    public void disposeEnvironment() throws JJVMTIException {
        super.disposeEnvironment();
        JVMTI.disposeJVMTIJavaEnv(env);
    }

    @Override
    public String getErrorName(int error) throws JJVMTIException {
        super.getErrorName(error);
        return JVMTIError.getName(error);
    }

    @Override
    public int getJLocationFormat() throws JJVMTIException {
        super.getJLocationFormat();
        return JVMTIConstants.JVMTI_JLOCATION_JVMBCI;
    }

    @Override
    public String[] getSystemProperties() throws JJVMTIException {
        super.getSystemProperties();
        return JVMTISystem.getSystemProperties();
    }

    @Override
    public String getSystemProperty(String key) throws JJVMTIException {
        super.getSystemProperty(key);
        return JVMTISystem.getSystemProperty(key);
    }

    @Override
    public void setSystemProperty(String key, String value) throws JJVMTIException {
        super.setSystemProperty(key, value);
        throw new JJVMTIException(JVMTI_ERROR_WRONG_PHASE);

    }

    @Override
    public int getPhase() throws JJVMTIException {
        super.getPhase();
        return JVMTI.getPhase();
    }

    @Override
    public long getCurrentThreadCpuTime() throws JJVMTIException {
        super.getCurrentThreadCpuTime();
        throw notImplemented; // TODO

    }

    @Override
    public long getThreadCpuTime(Thread thread) throws JJVMTIException {
        super.getThreadCpuTime(thread);
        throw notImplemented; // TODO

    }

    @Override
    public long getTime() throws JJVMTIException {
        super.getTime();
        return System.nanoTime();

    }

    @Override
    public EnumSet<JVMTICapabilities.E> getPotentialCapabilities() throws JJVMTIException {
        super.getPotentialCapabilities();
        return JVMTICapabilities.getPotentialCapabilities(env);
    }

    @Override
    public void addCapabilities(EnumSet<JVMTICapabilities.E> caps) throws JJVMTIException {
        super.addCapabilities(caps);
        JVMTICapabilities.addCapabilities(env, caps);
    }

    @Override
    public void relinquishCapabilities(EnumSet<JVMTICapabilities.E> caps) throws JJVMTIException {
        super.relinquishCapabilities(caps);
        JVMTICapabilities.relinquishCapabilities(env, caps);
    }

    @Override
    public int getAvailableProcessors() throws JJVMTIException {
        super.getAvailableProcessors();
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public void addToBootstrapClassLoaderSearch(String path) throws JJVMTIException {
        super.addToBootstrapClassLoaderSearch(path);
        throw notImplemented; // TODO

    }

    @Override
    public void setVerboseFlag(int flag, boolean value) throws JJVMTIException {
        super.setVerboseFlag(flag, value);
        int error = JVMTISystem.setVerboseFlag(flag, value);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTIException(error);
        }
    }

    @Override
    public void addToSystemClassLoaderSearch(String path) throws JJVMTIException {
        super.addToSystemClassLoaderSearch(path);
        throw notImplemented; // TODO

    }

    @Override
    public MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(Thread thread) throws JJVMTIException {
        super.getOwnedMonitorStackDepthInfo(thread);
        throw notImplemented; // TODO

    }

    @Override
    public long getObjectSize(Object object) throws JJVMTIException {
        super.getObjectSize(object);
        return Layout.size(Reference.fromJava(object)).toInt();
    }

    @Override
    public void setBreakpoint(ClassMethodActor method, long location) throws JJVMTIException {
        super.setBreakpoint(method, location);
        JVMTIBreakpoints.setBreakpoint(method, location);
    }

    @Override
    public void clearBreakpoint(ClassMethodActor method, long location) throws JJVMTIException {
        super.clearBreakpoint(method, location);
        JVMTIBreakpoints.clearBreakpoint(method, location);
    }

    @Override
    public void setFieldAccessWatch(FieldActor field) throws JJVMTIException {
        super.setFieldAccessWatch(field);
        JVMTIFieldWatch.setWatch(field, JVMTIFieldWatch.ACCESS_STATE);
    }

    @Override
    public void clearFieldAccessWatch(FieldActor field) throws JJVMTIException {
        super.clearFieldAccessWatch(field);
        JVMTIFieldWatch.clearWatch(field, JVMTIFieldWatch.ACCESS_STATE);
    }

    @Override
    public void setFieldModificationWatch(FieldActor field) throws JJVMTIException {
        super.setFieldModificationWatch(field);
        JVMTIFieldWatch.setWatch(field, JVMTIFieldWatch.MODIFICATION_STATE);
    }

    @Override
    public void clearFieldModificationWatch(FieldActor field) throws JJVMTIException {
        super.clearFieldModificationWatch(field);
        JVMTIFieldWatch.clearWatch(field, JVMTIFieldWatch.MODIFICATION_STATE);
    }

    @Override
    public boolean isModifiableClass(ClassActor klass) throws JJVMTIException {
        super.isModifiableClass(klass);
        throw notImplemented; // TODO
    }

    @Override
    public String getClassSignature(ClassActor klass) throws JJVMTIException {
        super.getClassSignature(klass);
        return klass.typeDescriptor.string;
    }

    @Override
    public int getClassStatus(ClassActor klass) throws JJVMTIException {
        super.getClassStatus(klass);
        return JVMTIClassFunctions.getClassStatus(klass);
    }

    @Override
    public String getSourceFileName(ClassActor klass) throws JJVMTIException {
        super.getSourceFileName(klass);
        return  klass.sourceFileName;
    }

    @Override
    public int getClassModifiers(ClassActor klass) throws JJVMTIException {
        super.getClassModifiers(klass);
        return klass.accessFlags();
    }

    @Override
    public MethodActor[] getClassMethods(ClassActor klass) throws JJVMTIException {
        super.getClassMethods(klass);
        return JVMTIClassFunctions.getClassMethods(klass);
    }

    @Override
    public FieldActor[] getClassFields(ClassActor klass) throws JJVMTIException {
        super.getClassFields(klass);
        List<FieldActor> list = klass.getLocalFieldActors();
        FieldActor[] result = new FieldActor[list.size()];
        list.toArray(result);
        return result;
    }

    @Override
    public ClassActor[] getImplementedInterfaces(ClassActor klass) throws JJVMTIException {
        super.getImplementedInterfaces(klass);
        List<InterfaceActor> interfaceActors = klass.getLocalInterfaceActors();
        ClassActor[] result = new ClassActor[interfaceActors.size()];
        interfaceActors.toArray(result);
        return result;
    }

    @Override
    public boolean isInterface(ClassActor klass) throws JJVMTIException {
        super.isInterface(klass);
        return ClassActor.isInterface(klass.flags());
    }

    @Override
    public boolean isArrayClass(ClassActor klass) throws JJVMTIException {
        super.isArrayClass(klass);
        return klass.isArrayClass();
    }

    @Override
    public ClassLoader getClassLoader(ClassActor klass) throws JJVMTIException {
        super.getClassLoader(klass);
        return klass.classLoader;
    }

    @Override
    public String getFieldName(FieldActor field) throws JJVMTIException {
        super.getFieldName(field);
        return field.name();
    }

    @Override
    public String getFieldSignature(FieldActor field) throws JJVMTIException {
        super.getFieldSignature(field);
        return field.descriptor().string;
    }

    @Override
    public ClassActor getFieldDeclaringClass(FieldActor field) throws JJVMTIException {
        super.getFieldDeclaringClass(field);
        return field.holder();
    }

    @Override
    public int getFieldModifiers(FieldActor field) throws JJVMTIException {
        super.getFieldModifiers(field);
        return field.accessFlags();
    }

    @Override
    public boolean isFieldSynthetic(FieldActor field) throws JJVMTIException {
        super.isFieldSynthetic(field);
        return (field.flags() & Actor.ACC_SYNTHETIC) != 0;
    }

    @Override
    public String getMethodName(MethodActor method) throws JJVMTIException {
        super.getMethodName(method);
        return method.name();
    }

    @Override
    public String getMethodSignature(MethodActor method) throws JJVMTIException {
        super.getMethodSignature(method);
        return method.descriptor().string;
    }

    @Override
    public String getMethodGenericSignature(MethodActor method) throws JJVMTIException {
        super.getMethodGenericSignature(method);
        return method.genericSignatureString();

    }

    @Override
    public ClassActor getMethodDeclaringClass(MethodActor method) throws JJVMTIException {
        super.getMethodDeclaringClass(method);
        return method.holder();
    }

    @Override
    public int getMethodModifiers(MethodActor method) throws JJVMTIException {
        super.getMethodModifiers(method);
        return method.accessFlags();
    }

    @Override
    public int getMaxLocals(ClassMethodActor method) throws JJVMTIException {
        super.getMaxLocals(method);
        return method.codeAttribute().maxLocals;
    }

    @Override
    public int getArgumentsSize(ClassMethodActor method) throws JJVMTIException {
        super.getArgumentsSize(method);
        return method.numberOfParameterSlots();
    }

    @Override
    public LineNumberEntry[] getLineNumberTable(ClassMethodActor method) throws JJVMTIException {
        super.getLineNumberTable(method);
        return JVMTIClassFunctions.getLineNumberTable(method);
    }

    @Override
    public MethodLocation getMethodLocation(ClassMethodActor method) throws JJVMTIException {
        super.getMethodLocation(method);
        byte[] code = method.codeAttribute().code();
        return new MethodLocation(0, code.length - 1);
    }

    @Override
    public LocalVariableEntry[] getLocalVariableTable(ClassMethodActor method) throws JJVMTIException {
        super.getLocalVariableTable(method);
        return JVMTIClassFunctions.getLocalVariableTable(method);
    }

    @Override
    public byte[] getBytecodes(ClassMethodActor method) throws JJVMTIException {
        super.getBytecodes(method);
        return method.code();
    }

    @Override
    public boolean isMethodNative(MethodActor method) throws JJVMTIException {
        super.isMethodNative(method);
        return method.isNative();
    }

    @Override
    public boolean isMethodSynthetic(MethodActor method) throws JJVMTIException {
        super.isMethodSynthetic(method);
        return (method.flags() & Actor.ACC_SYNTHETIC) != 0;
    }

    @Override
    public ClassActor[] getLoadedClasses() throws JJVMTIException {
        super.getLoadedClasses();
        return JVMTIClassFunctions.getLoadedClassActors();
    }

    @Override
    public ClassActor[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException {
        super.getClassLoaderClasses(loader);
        return JVMTIClassFunctions.getClassLoaderClasses(loader);
    }

    @Override
    public void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException {
        super.redefineClasses(classDefinitions);
        throw notImplemented; // TODO

    }

    @Override
    public String getSourceDebugExtension(ClassActor klass) throws JJVMTIException {
        super.getSourceDebugExtension(klass);
        throw new JJVMTI.JJVMTIException(JVMTI_ERROR_ABSENT_INFORMATION);
    }

    @Override
    public boolean isMethodObsolete(MethodActor method) throws JJVMTIException {
        super.isMethodObsolete(method);
        throw notImplemented; // TODO
    }

    @Override
    public ClassVersionInfo getClassVersionNumbers(ClassActor klass) throws JJVMTIException {
        super.getClassVersionNumbers(klass);
        return new ClassVersionInfo(klass.majorVersion, klass.minorVersion);

    }

    @Override
    public byte[] getConstantPool(ClassActor klass) throws JJVMTIException {
        super.getConstantPool(klass);
        throw notImplemented; // TODO

    }

    @Override
    public void retransformClasses(ClassActor[] klasses) throws JJVMTIException {
        super.retransformClasses(klasses);
        throw notImplemented; // TODO
    }

    @Override
    public void iterateThroughHeap(int filter, ClassActor classActor, HeapCallbacks heapCallbacks, Object userData) throws JJVMTIException {
        super.iterateThroughHeap(filter, classActor, heapCallbacks, userData);
        JVMTIHeapFunctions.iterateThroughHeap(env, filter, classActor, heapCallbacks, userData);
    }


}
