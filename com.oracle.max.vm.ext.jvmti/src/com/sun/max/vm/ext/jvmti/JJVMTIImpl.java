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

import com.sun.max.vm.ext.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.thread.*;

/**
 * Implementation of {@link JJVMTI}.
 * Reuses as much as possible of the native JVMTI implementation.
 */
public class JJVMTIImpl implements JJVMTI {

    public static final JJVMTIImpl instance = new JJVMTIImpl();

    /**
     * Register an agent.
     * @param callbacks the instance to invoke the callbacks on
     */
    public void register(JVMTI.JavaEnv javaEnv) {
        JVMTI.setJVMTIJavaEnv(javaEnv);
    }

    @Override
    public void setEventNotificationMode(JVMTI.JavaEnv env, int mode, int event, Thread thread) throws Exception {
        JVMTIEvent.setEventNotificationMode(env, mode, event, thread);

    }

    @Override
    public Thread[] getAllThreads(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void suspendThread(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void resumeThread(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void stopThread(JVMTI.JavaEnv env, Thread thread, Throwable t) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void interruptThread(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void getThreadInfo(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void getOwnedMonitorInfo(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getCurrentContendedMonitor(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void runAgentThread(JVMTI.JavaEnv env, Runnable runnable, int priority) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public ThreadGroup[] getTopThreadGroups(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void getThreadGroupInfo(JVMTI.JavaEnv env, ThreadGroup threadGroup) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void getThreadGroupChildren(JVMTI.JavaEnv env, ThreadGroup threadGroup) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public int getFrameCount(JVMTI.JavaEnv env, Thread thread) throws Exception {
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            throw new Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        SingleThreadStackTraceVmOperation op = new FindAppFramesStackTraceOperation(vmThread).submitOp();
        return op.stackTraceVisitor.stackElements.size();
    }

    @Override
    public int getThreadState(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void getCurrentThread(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public FrameInfo getFrameLocation(JVMTI.JavaEnv env, Thread thread, int depth, FrameInfo methodInfo) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void notifyFramePop(JVMTI.JavaEnv env, Thread thread, int depth) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getLocalObject(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocalInt(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLocalLong(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getLocalFloat(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getLocalDouble(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLocalObject(JVMTI.JavaEnv env, Thread thread, int depth, int slot, Object value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLocalInt(JVMTI.JavaEnv env, Thread thread, int depth, int slot, int value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLocalLong(JVMTI.JavaEnv env, Thread thread, int depth, int slot, long value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLocalFloat(JVMTI.JavaEnv env, Thread thread, int depth, int slot, float value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLocalDouble(JVMTI.JavaEnv env, Thread thread, int arg2, int arg3, double value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public RawMonitor createRawMonitor(JVMTI.JavaEnv env, String name) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void destroyRawMonitor(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void rawMonitorEnter(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void rawMonitorExit(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void rawMonitorWait(JVMTI.JavaEnv env, RawMonitor rawMonitor, long arg2) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void rawMonitorNotify(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void rawMonitorNotifyAll(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBreakpoint(JVMTI.JavaEnv env, Method method, long location) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearBreakpoint(JVMTI.JavaEnv env, Method method, long location) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFieldAccessWatch(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearFieldAccessWatch(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFieldModificationWatch(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearFieldModificationWatch(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isModifiableClass(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getClassSignature(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getClassStatus(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getSourceFileName(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getClassModifiers(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Method[] getClassMethods(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Field[] getClassFields(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class[] getImplementedInterfaces(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isInterface(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isArrayClass(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ClassLoader getClassLoader(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getObjectHashCode(JVMTI.JavaEnv env, Object object) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ObjectMonitorUsage getObjectMonitorUsage(JVMTI.JavaEnv env, Object object) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFieldName(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFieldSignature(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class getFieldDeclaringClass(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFieldModifiers(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isFieldSynthetic(JVMTI.JavaEnv env, Field field) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getMethodName(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMethodSignature(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMethodGenericSignature(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class getMethodDeclaringClass(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getMethodModifiers(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxLocals(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getArgumentsSize(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public LineNumberEntry[] getLineNumberTable(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MethodLocation getMethodLocation(JVMTI.JavaEnv env, Method method, MethodLocation methodLocation) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LocalVariableEntry[] getLocalVariableTable(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setNativeMethodPrefix(JVMTI.JavaEnv env, String prefix) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNativeMethodPrefixes(JVMTI.JavaEnv env, String[] prefixes) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] getBytecodes(JVMTI.JavaEnv env, Method method, byte[] useThis) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isMethodNative(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isMethodSynthetic(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Class[] getLoadedClasses(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class[] getClassLoaderClasses(JVMTI.JavaEnv env, ClassLoader loader) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void popFrame(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceEarlyReturnObject(JVMTI.JavaEnv env, Thread thread, Object value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceEarlyReturnInt(JVMTI.JavaEnv env, Thread thread, int value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceEarlyReturnLong(JVMTI.JavaEnv env, Thread thread, long value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceEarlyReturnFloat(JVMTI.JavaEnv env, Thread thread, float value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceEarlyReturnDouble(JVMTI.JavaEnv env, Thread thread, double value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceEarlyReturnVoid(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void redefineClasses(JVMTI.JavaEnv env, ClassDefinition[] classDefinitions) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public int getVersionNumber(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getCapabilities(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getSourceDebugExtension(JVMTI.JavaEnv env, Class klass) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isMethodObsolete(JVMTI.JavaEnv env, Method method) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void suspendThreadList(JVMTI.JavaEnv env, Thread[] threads) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void resumeThreadList(JVMTI.JavaEnv env, Thread[] threads) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public StackInfo[] getAllStackTraces(JVMTI.JavaEnv env, int maxFrameCount) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void getThreadListStackTraces(JVMTI.JavaEnv env, Thread[] threads, int maxFrameCount) throws Exception {
        // TODO Auto-generated method stub

    }

    private static class ObjectThreadLocal extends ThreadLocal<Object> {
        @Override
        public Object initialValue() {
            return null;
        }
    }

    private static final ObjectThreadLocal otl = new ObjectThreadLocal();

    @Override
    public Object getThreadLocalStorage(JVMTI.JavaEnv env, Thread thread) throws Exception {
        thread = JVMTIThreadFunctions.checkThread(thread);
        if (thread == null) {
            throw new Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        return otl.get();
    }

    @Override
    public void setThreadLocalStorage(JVMTI.JavaEnv env, Thread thread, Object data) throws Exception {
        thread = JVMTIThreadFunctions.checkThread(thread);
        if (thread == null) {
            throw new Exception(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        otl.set(data);
    }

    @Override
    public int getStackTrace(JVMTI.JavaEnv env, Thread thread, int startDepth, int maxFrameCount, FrameInfo[] stackframeInfo) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getTag(JVMTI.JavaEnv env, Object object) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTag(JVMTI.JavaEnv env, Object object, Object tag) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void forceGarbageCollection(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void disposeEnvironment(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public String getErrorName(JVMTI.JavaEnv env, int error) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getJLocationFormat(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Properties getSystemProperties(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSystemProperty(JVMTI.JavaEnv env, String key) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSystemProperty(JVMTI.JavaEnv env, String key, String value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public int getPhase(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getCurrentThreadCpuTime(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getThreadCpuTime(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTime(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public EnumSet<JVMTICapabilities.E> getPotentialCapabilities(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addCapabilities(JVMTI.JavaEnv env, EnumSet<JVMTICapabilities.E> caps) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void relinquishCapabilities(JVMTI.JavaEnv env, EnumSet<JVMTICapabilities.E> caps) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public int getAvailableProcessors(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ClassVersionInfo getClassVersionNumbers(JVMTI.JavaEnv env, Class klasss, ClassVersionInfo classVersionInfo) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getConstantPool(JVMTI.JavaEnv env, Class klass, byte[] pool) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getEnvironmentLocalStorage(JVMTI.JavaEnv env) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setEnvironmentLocalStorage(JVMTI.JavaEnv env, Object data) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToBootstrapClassLoaderSearch(JVMTI.JavaEnv env, String path) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVerboseFlag(JVMTI.JavaEnv env, int arg1, boolean arg2) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToSystemClassLoaderSearch(JVMTI.JavaEnv env, String path) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void retransformClasses(JVMTI.JavaEnv env, Class[] klasses) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(JVMTI.JavaEnv env, Thread thread) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getObjectSize(JVMTI.JavaEnv env, Object object) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setEventCallbacks(JVMTI.JavaEnv env, EventCallbacks eventCallbacks) throws Exception {
        // TODO Auto-generated method stub

    }

}
