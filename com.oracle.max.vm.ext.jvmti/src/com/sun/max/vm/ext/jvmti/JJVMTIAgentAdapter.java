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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.ext.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link JJVMTI} that reuses as much as possible of the native JVMTI implementation.
 * An actual agent should subclass this class and provide implementations of the {@link JJVMTI.EventCallbacks event callbacks}
 * that it wishes to handle. Subclasses {@link JVMTI.JavaEnv} which implements {@link JJVMTI.EventCallbacks}, defining
 * empty implementations. So an actual agent subclass should override the callback methods it is interested
 * in (potentially) receiving, and call {@link #setEventNotificationMode} to enable delivery.
 *
 * Although this class is not directly referenced by the VM code, we want it to be fully compiled into the boot image
 * so we define every method as a {@link CriticalMethod}.
 *
 * TODO: complete the implementation.
 */
public class JJVMTIAgentAdapter extends JVMTI.JavaEnv implements JJVMTI {

    private static final JJVMTI.Exception notImplemented = new JJVMTI.Exception(JVMTI_ERROR_NOT_AVAILABLE);

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {
        @Override
        public void initializationComplete() {
            for (Method m : JJVMTIAgentAdapter.class.getDeclaredMethods()) {
                new CriticalMethod(ClassMethodActor.fromJava(m), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            }
        }
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    /**
     * Register an agent.
     * @param the agent implementation subclass
     */
    public static void register(JJVMTIAgentAdapter javaEnv) {
        JVMTI.setJVMTIJavaEnv(javaEnv);
    }

    @Override
    public void setEventNotificationMode(int mode, int event, Thread thread) throws Exception {
        JVMTIEvent.setEventNotificationMode(this, mode, event, thread);
    }

    @Override
    public Thread[] getAllThreads() throws Exception {
        return VmThreadMap.getThreads(true);
    }

    @Override
    public void suspendThread(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public void resumeThread(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public void stopThread(Thread thread, Throwable t) throws Exception {
        throw notImplemented;

    }

    @Override
    public void interruptThread(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public void getThreadInfo(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public void getOwnedMonitorInfo(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public Object getCurrentContendedMonitor(Thread thread) throws Exception {
        throw notImplemented;
    }

    @Override
    public void runAgentThread(Runnable runnable, int priority) throws Exception {
        throw notImplemented;

    }

    @Override
    public ThreadGroup[] getTopThreadGroups() throws Exception {
        throw notImplemented;
    }

    @Override
    public void getThreadGroupInfo(ThreadGroup threadGroup) throws Exception {
        throw notImplemented;

    }

    @Override
    public void getThreadGroupChildren(ThreadGroup threadGroup) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getFrameCount(Thread thread) throws Exception {
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            throw new Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        SingleThreadStackTraceVmOperation op = new FindAppFramesStackTraceOperation(vmThread).submitOp();
        return op.stackTraceVisitor.stackElements.size();
    }

    @Override
    public int getThreadState(Thread thread) throws Exception {
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            throw new JJVMTI.Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        return JVMTIThreadFunctions.getThreadState(vmThread);
    }

    @Override
    public Thread getCurrentThread() throws Exception {
        return VmThread.current().javaThread();
    }

    @Override
    public FrameInfo getFrameLocation(Thread thread, int depth) throws Exception {
        return JVMTIThreadFunctions.getFrameLocation(thread, depth);
    }

    @Override
    public void notifyFramePop(Thread thread, int depth) throws Exception {
        throw notImplemented;

    }

    @Override
    public Object getLocalObject(Thread thread, int depth, int slot) throws Exception {
        return JVMTIThreadFunctions.getLocalObject(thread, depth, slot);
    }

    @Override
    public int getLocalInt(Thread thread, int depth, int slot) throws Exception {
        return JVMTIThreadFunctions.getLocalInt(thread, depth, slot);
    }

    @Override
    public long getLocalLong(Thread thread, int depth, int slot) throws Exception {
        return JVMTIThreadFunctions.getLocalLong(thread, depth, slot);
    }

    @Override
    public float getLocalFloat(Thread thread, int depth, int slot) throws Exception {
        return JVMTIThreadFunctions.getLocalFloat(thread, depth, slot);
    }

    @Override
    public double getLocalDouble(Thread thread, int depth, int slot) throws Exception {
        return JVMTIThreadFunctions.getLocalDouble(thread, depth, slot);
    }

    @Override
    public void setLocalObject(Thread thread, int depth, int slot, Object value) throws Exception {
        JVMTIThreadFunctions.setLocalObject(thread, depth, slot, value);
    }

    @Override
    public void setLocalInt(Thread thread, int depth, int slot, int value) throws Exception {
        JVMTIThreadFunctions.setLocalInt(thread, depth, slot, value);
    }

    @Override
    public void setLocalLong(Thread thread, int depth, int slot, long value) throws Exception {
        JVMTIThreadFunctions.setLocalLong(thread, depth, slot, value);
    }

    @Override
    public void setLocalFloat(Thread thread, int depth, int slot, float value) throws Exception {
        JVMTIThreadFunctions.setLocalFloat(thread, depth, slot, value);
    }

    @Override
    public void setLocalDouble(Thread thread, int depth, int slot, double value) throws Exception {
        JVMTIThreadFunctions.setLocalDouble(thread, depth, slot, value);
    }

    @Override
    public RawMonitor createRawMonitor(String name) throws Exception {
        throw notImplemented;

    }

    @Override
    public void destroyRawMonitor(RawMonitor rawMonitor) throws Exception {
        throw notImplemented;

    }

    @Override
    public void rawMonitorEnter(RawMonitor rawMonitor) throws Exception {
        throw notImplemented;

    }

    @Override
    public void rawMonitorExit(RawMonitor rawMonitor) throws Exception {
        throw notImplemented;

    }

    @Override
    public void rawMonitorWait(RawMonitor rawMonitor, long arg2) throws Exception {
        throw notImplemented;

    }

    @Override
    public void rawMonitorNotify(RawMonitor rawMonitor) throws Exception {
        throw notImplemented;

    }

    @Override
    public void rawMonitorNotifyAll(RawMonitor rawMonitor) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setBreakpoint(Method method, long location) throws Exception {
        throw notImplemented;

    }

    @Override
    public void clearBreakpoint(Method method, long location) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setFieldAccessWatch(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public void clearFieldAccessWatch(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setFieldModificationWatch(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public void clearFieldModificationWatch(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isModifiableClass(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getClassSignature(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getClassStatus(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getSourceFileName(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getClassModifiers(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public Method[] getClassMethods(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public Field[] getClassFields(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public Class[] getImplementedInterfaces(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isInterface(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isArrayClass(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public ClassLoader getClassLoader(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getObjectHashCode(Object object) throws Exception {
        throw notImplemented;

    }

    @Override
    public ObjectMonitorUsage getObjectMonitorUsage(Object object) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getFieldName(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getFieldSignature(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public Class getFieldDeclaringClass(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getFieldModifiers(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isFieldSynthetic(Field field) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getMethodName(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getMethodSignature(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public String getMethodGenericSignature(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public Class getMethodDeclaringClass(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getMethodModifiers(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getMaxLocals(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getArgumentsSize(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public LineNumberEntry[] getLineNumberTable(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public MethodLocation getMethodLocation(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public LocalVariableEntry[] getLocalVariableTable(Method method) throws Exception {
        return JVMTIClassFunctions.getLocalVariableTable(ClassMethodActor.fromJava(method));

    }

    @Override
    public void setNativeMethodPrefix(String prefix) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setNativeMethodPrefixes(String[] prefixes) throws Exception {
        throw notImplemented;

    }

    @Override
    public byte[] getBytecodes(Method method, byte[] useThis) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isMethodNative(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isMethodSynthetic(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public Class[] getLoadedClasses() throws Exception {
        return JVMTIClassFunctions.getLoadedClasses();
    }

    @Override
    public Class[] getClassLoaderClasses(ClassLoader loader) throws Exception {
        throw notImplemented;

    }

    @Override
    public void popFrame(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceEarlyReturnObject(Thread thread, Object value) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceEarlyReturnInt(Thread thread, int value) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceEarlyReturnLong(Thread thread, long value) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceEarlyReturnFloat(Thread thread, float value) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceEarlyReturnDouble(Thread thread, double value) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceEarlyReturnVoid(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public void redefineClasses(ClassDefinition[] classDefinitions) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getVersionNumber() throws Exception {
        throw notImplemented;

    }

    @Override
    public long getCapabilities() throws Exception {
        throw notImplemented;

    }

    @Override
    public String getSourceDebugExtension(Class klass) throws Exception {
        throw notImplemented;

    }

    @Override
    public boolean isMethodObsolete(Method method) throws Exception {
        throw notImplemented;

    }

    @Override
    public void suspendThreadList(Thread[] threads) throws Exception {
        throw notImplemented;

    }

    @Override
    public void resumeThreadList(Thread[] threads) throws Exception {
        throw notImplemented;

    }

    @Override
    public StackInfo[] getAllStackTraces(int maxFrameCount) throws Exception {
        throw notImplemented;

    }

    @Override
    public void getThreadListStackTraces(Thread[] threads, int maxFrameCount) throws Exception {
        throw notImplemented;

    }

    private static class ObjectThreadLocal extends ThreadLocal<Object> {
        @Override
        public Object initialValue() {
            return null;
        }
    }

    private static final ObjectThreadLocal otl = new ObjectThreadLocal();

    @Override
    public Object getThreadLocalStorage(Thread thread) throws Exception {
        thread = JVMTIThreadFunctions.checkThread(thread);
        if (thread == null) {
            throw new Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        return otl.get();
    }

    @Override
    public void setThreadLocalStorage(Thread thread, Object data) throws Exception {
        thread = JVMTIThreadFunctions.checkThread(thread);
        if (thread == null) {
            throw new Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        otl.set(data);
    }

    @Override
    public int getStackTrace(Thread thread, int startDepth, int maxFrameCount, FrameInfo[] stackframeInfo) throws Exception {
        throw notImplemented;

    }

    @Override
    public Object getTag(Object object) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setTag(Object object, Object tag) throws Exception {
        throw notImplemented;

    }

    @Override
    public void forceGarbageCollection() throws Exception {
        throw notImplemented;

    }

    @Override
    public void disposeEnvironment() throws Exception {
        throw notImplemented;

    }

    @Override
    public String getErrorName(int error) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getJLocationFormat() throws Exception {
        throw notImplemented;

    }

    @Override
    public Properties getSystemProperties() throws Exception {
        throw notImplemented;

    }

    @Override
    public String getSystemProperty(String key) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setSystemProperty(String key, String value) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getPhase() throws Exception {
        throw notImplemented;

    }

    @Override
    public long getCurrentThreadCpuTime() throws Exception {
        throw notImplemented;

    }

    @Override
    public long getThreadCpuTime(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public long getTime() throws Exception {
        throw notImplemented;

    }

    @Override
    public EnumSet<JVMTICapabilities.E> getPotentialCapabilities() throws Exception {
        throw notImplemented;

    }

    @Override
    public void addCapabilities(EnumSet<JVMTICapabilities.E> caps) throws Exception {
        throw notImplemented;

    }

    @Override
    public void relinquishCapabilities(EnumSet<JVMTICapabilities.E> caps) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getAvailableProcessors() throws Exception {
        throw notImplemented;

    }

    @Override
    public ClassVersionInfo getClassVersionNumbers(Class klasss, ClassVersionInfo classVersionInfo) throws Exception {
        throw notImplemented;

    }

    @Override
    public int getConstantPool(Class klass, byte[] pool) throws Exception {
        throw notImplemented;

    }

    @Override
    public Object getEnvironmentLocalStorage() throws Exception {
        throw notImplemented;

    }

    @Override
    public void setEnvironmentLocalStorage(Object data) throws Exception {
        throw notImplemented;

    }

    @Override
    public void addToBootstrapClassLoaderSearch(String path) throws Exception {
        throw notImplemented;

    }

    @Override
    public void setVerboseFlag(int arg1, boolean arg2) throws Exception {
        throw notImplemented;

    }

    @Override
    public void addToSystemClassLoaderSearch(String path) throws Exception {
        throw notImplemented;

    }

    @Override
    public void retransformClasses(Class[] klasses) throws Exception {
        throw notImplemented;

    }

    @Override
    public MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(Thread thread) throws Exception {
        throw notImplemented;

    }

    @Override
    public long getObjectSize(Object object) throws Exception {
        throw notImplemented;
    }

    @Override
    public void setEventCallbacks(EventCallbacks eventCallbacks) throws Exception {
        throw notImplemented;

    }

}
