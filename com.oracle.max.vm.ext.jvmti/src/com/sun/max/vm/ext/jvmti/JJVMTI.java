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

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

/**
 * A functionally equivalent JVMTI interface but cast in terms of standard Java types that can
 * be called from agents written in Java (for Maxine).
 *
 * Some of the JVMTI functions are redundant in that they essentially replicate existing functionality
 * in the JDK, however, we include them for completeness.
 *
 * A few of the JVMTI functions don't have an Java equivalent, and these are omitted.
 *
 * A Java JVMTI agent that uses this interface is specified to the VM using the {@code -javaagent}
 * mechanism. A consequence of this is that the agent is initialized much later in the VM startup
 * than a native agent. Some facilities, e.g., affecting the configuration of the VM before it
 * completes initialization are therefore unavailable.
 */
public interface JJVMTI {

    /**
     * JVMTI errors.
     * Whereas native JVMTI indicates errors by a return code, Java JVMTI uses
     * an exception with the error code as argument. Since errors are rare
     * this is a {@link RuntimeException}.
     */
    public class Exception extends RuntimeException {
        public final int error;

        public Exception(int error) {
            super();
            this.error = error;
        }
    }

    public static abstract class RawMonitor {

    }

    public static class FrameInfo {
        Method method;
        int location;
    }

    public static class MethodLocation {
        int start;
        int end;
    }

    public static class LineNumberEntry {
        int bci;
        int lineNumber;
    }

    public static class LocalVariableEntry {
        long location;
        int length;
        String name;
        String signature;
        String genericSignature;
        int slot;
    }

    public static class ClassDefinition {
        Class klass;
        byte[] classBytes;
    }

    public static class ClassVersionInfo {
        int major;
        int minor;
    }

    public static class StackInfo {
        Thread thread;
        int state;
        FrameInfo frameInfo;
        int frameCount;
    }

    public static class ObjectMonitorUsage {

    }

    public static class MonitorStackDepthInfo {
        Object monitor;
        int stackDepth;
    }

    /**
     * Event callbacks.
     * Unlike the native JVMTI interface where callbacks are registered individually,
     * Java agents register a single object and use overriding to handle just those
     * events they want.
     */
    public static interface EventCallbacks {
        void agentOnLoad();
        void vmInit();
        void breakpoint(Thread thread, Method method, long location);
        void garbageCollectionStart();
        void garbageCollectionFinish();
        void classLoad(Thread thread, Class klass);
        void classPrepare(Thread thread, Class klass);
        byte[] classFileLoadHook(ClassLoader loader, String name,
                               ProtectionDomain protectionDomain, byte[] classData);
        void threadStart(Thread thread);
        void threadEnd(Thread thread);
        void vmDeath();
    }

    void setEventNotificationMode(JVMTI.JavaEnv env, int mode, int event, Thread thread) throws Exception;
    Thread[] getAllThreads(JVMTI.JavaEnv env) throws Exception;
    void suspendThread(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void resumeThread(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void stopThread(JVMTI.JavaEnv env, Thread thread, Throwable t) throws Exception;
    void interruptThread(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void getThreadInfo(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void getOwnedMonitorInfo(JVMTI.JavaEnv env, Thread thread) throws Exception;
    Object getCurrentContendedMonitor(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void runAgentThread(JVMTI.JavaEnv env, Runnable runnable, int priority) throws Exception;
    ThreadGroup[] getTopThreadGroups(JVMTI.JavaEnv env) throws Exception;
    void getThreadGroupInfo(JVMTI.JavaEnv env, ThreadGroup threadGroup) throws Exception;
    void getThreadGroupChildren(JVMTI.JavaEnv env, ThreadGroup threadGroup) throws Exception;
    int getFrameCount(JVMTI.JavaEnv env, Thread thread) throws Exception;
    int getThreadState(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void getCurrentThread(JVMTI.JavaEnv env) throws Exception;
    FrameInfo getFrameLocation(JVMTI.JavaEnv env, Thread thread, int depth, FrameInfo methodInfo) throws Exception;
    void notifyFramePop(JVMTI.JavaEnv env, Thread thread, int depth) throws Exception;
    Object getLocalObject(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception;
    int getLocalInt(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception;
    long getLocalLong(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception;
    float getLocalFloat(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception;
    double getLocalDouble(JVMTI.JavaEnv env, Thread thread, int depth, int slot) throws Exception;
    void setLocalObject(JVMTI.JavaEnv env, Thread thread, int depth, int slot, Object value) throws Exception;
    void setLocalInt(JVMTI.JavaEnv env, Thread thread, int depth, int slot, int value) throws Exception;
    void setLocalLong(JVMTI.JavaEnv env, Thread thread, int depth, int slot, long value) throws Exception;
    void setLocalFloat(JVMTI.JavaEnv env, Thread thread, int depth, int slot, float value) throws Exception;
    void setLocalDouble(JVMTI.JavaEnv env, Thread thread, int arg2, int arg3, double value) throws Exception;
    RawMonitor createRawMonitor(JVMTI.JavaEnv env, String name) throws Exception;
    void destroyRawMonitor(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception;
    void rawMonitorEnter(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception;
    void rawMonitorExit(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception;
    void rawMonitorWait(JVMTI.JavaEnv env, RawMonitor rawMonitor, long arg2) throws Exception;
    void rawMonitorNotify(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception;
    void rawMonitorNotifyAll(JVMTI.JavaEnv env, RawMonitor rawMonitor) throws Exception;
    void setBreakpoint(JVMTI.JavaEnv env, Method method, long location) throws Exception;
    void clearBreakpoint(JVMTI.JavaEnv env, Method method, long location) throws Exception;
    void setFieldAccessWatch(JVMTI.JavaEnv env, Field field) throws Exception;
    void clearFieldAccessWatch(JVMTI.JavaEnv env, Field field) throws Exception;
    void setFieldModificationWatch(JVMTI.JavaEnv env, Field field) throws Exception;
    void clearFieldModificationWatch(JVMTI.JavaEnv env, Field field) throws Exception;
    boolean isModifiableClass(JVMTI.JavaEnv env, Class klass) throws Exception;
    String getClassSignature(JVMTI.JavaEnv env, Class klass) throws Exception;
    int getClassStatus(JVMTI.JavaEnv env, Class klass) throws Exception;
    String getSourceFileName(JVMTI.JavaEnv env, Class klass) throws Exception;
    int getClassModifiers(JVMTI.JavaEnv env, Class klass) throws Exception;
    Method[] getClassMethods(JVMTI.JavaEnv env, Class klass) throws Exception;
    Field[] getClassFields(JVMTI.JavaEnv env, Class klass) throws Exception;
    Class[] getImplementedInterfaces(JVMTI.JavaEnv env, Class klass) throws Exception;
    boolean isInterface(JVMTI.JavaEnv env, Class klass) throws Exception;
    boolean isArrayClass(JVMTI.JavaEnv env, Class klass) throws Exception;
    ClassLoader getClassLoader(JVMTI.JavaEnv env, Class klass) throws Exception;
    int getObjectHashCode(JVMTI.JavaEnv env, Object object) throws Exception;
    ObjectMonitorUsage getObjectMonitorUsage(JVMTI.JavaEnv env, Object object) throws Exception;
    String getFieldName(JVMTI.JavaEnv env, Field field) throws Exception;
    String getFieldSignature(JVMTI.JavaEnv env, Field field) throws Exception;
    Class getFieldDeclaringClass(JVMTI.JavaEnv env, Field field) throws Exception;
    int getFieldModifiers(JVMTI.JavaEnv env, Field field) throws Exception;
    boolean isFieldSynthetic(JVMTI.JavaEnv env, Field field) throws Exception;
    String getMethodName(JVMTI.JavaEnv env, Method method) throws Exception;
    String getMethodSignature(JVMTI.JavaEnv env, Method method) throws Exception;
    String getMethodGenericSignature(JVMTI.JavaEnv env, Method method) throws Exception;
    Class getMethodDeclaringClass(JVMTI.JavaEnv env, Method method) throws Exception;
    int getMethodModifiers(JVMTI.JavaEnv env, Method method) throws Exception;
    int getMaxLocals(JVMTI.JavaEnv env, Method method) throws Exception;
    int getArgumentsSize(JVMTI.JavaEnv env, Method method) throws Exception;
    LineNumberEntry[] getLineNumberTable(JVMTI.JavaEnv env, Method method) throws Exception;
    MethodLocation getMethodLocation(JVMTI.JavaEnv env, Method method, MethodLocation methodLocation) throws Exception;
    LocalVariableEntry[] getLocalVariableTable(JVMTI.JavaEnv env, Method method) throws Exception;
    void setNativeMethodPrefix(JVMTI.JavaEnv env, String prefix) throws Exception;
    void setNativeMethodPrefixes(JVMTI.JavaEnv env, String[] prefixes) throws Exception;
    byte[] getBytecodes(JVMTI.JavaEnv env, Method method, byte[] useThis) throws Exception;
    boolean isMethodNative(JVMTI.JavaEnv env, Method method) throws Exception;
    boolean isMethodSynthetic(JVMTI.JavaEnv env, Method method) throws Exception;
    Class[] getLoadedClasses(JVMTI.JavaEnv env) throws Exception;
    Class[] getClassLoaderClasses(JVMTI.JavaEnv env, ClassLoader loader) throws Exception;
    void popFrame(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void forceEarlyReturnObject(JVMTI.JavaEnv env, Thread thread, Object value) throws Exception;
    void forceEarlyReturnInt(JVMTI.JavaEnv env, Thread thread, int value) throws Exception;
    void forceEarlyReturnLong(JVMTI.JavaEnv env, Thread thread, long value) throws Exception;
    void forceEarlyReturnFloat(JVMTI.JavaEnv env, Thread thread, float value) throws Exception;
    void forceEarlyReturnDouble(JVMTI.JavaEnv env, Thread thread, double value) throws Exception;
    void forceEarlyReturnVoid(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void redefineClasses(JVMTI.JavaEnv env, ClassDefinition[] classDefinitions) throws Exception;
    int getVersionNumber(JVMTI.JavaEnv env) throws Exception;
    long getCapabilities(JVMTI.JavaEnv env) throws Exception;
    String getSourceDebugExtension(JVMTI.JavaEnv env, Class klass) throws Exception;
    boolean isMethodObsolete(JVMTI.JavaEnv env, Method method) throws Exception;
    void suspendThreadList(JVMTI.JavaEnv env, Thread[] threads) throws Exception;
    void resumeThreadList(JVMTI.JavaEnv env, Thread[] threads) throws Exception;
    StackInfo[] getAllStackTraces(JVMTI.JavaEnv env, int maxFrameCount) throws Exception;
    void getThreadListStackTraces(JVMTI.JavaEnv env, Thread[] threads, int maxFrameCount) throws Exception;
    Object getThreadLocalStorage(JVMTI.JavaEnv env, Thread thread) throws Exception;
    void setThreadLocalStorage(JVMTI.JavaEnv env, Thread thread, Object data) throws Exception;
    int getStackTrace(JVMTI.JavaEnv env, Thread thread, int startDepth, int maxFrameCount, FrameInfo[] stackframeInfo) throws Exception;
    Object getTag(JVMTI.JavaEnv env, Object object) throws Exception;
    void setTag(JVMTI.JavaEnv env, Object object, Object tag) throws Exception;
    void forceGarbageCollection(JVMTI.JavaEnv env) throws Exception;
    /*
    void iterateOverObjectsReachableFromObject(JVMTI.JavaEnv env, JniHandle arg1, Address arg2, Pointer arg3) throws Exception;
    void iterateOverReachableObjects(JVMTI.JavaEnv env, Address arg1, Address arg2, Address arg3, Pointer arg4) throws Exception;
    void iterateOverHeap(JVMTI.JavaEnv env, int arg1, Address arg2, Pointer arg3) throws Exception;
    void iterateOverInstancesOfClass(JVMTI.JavaEnv env, JniHandle arg1, int arg2, Address arg3, Pointer arg4) throws Exception;
    void getObjectsWithTags(JVMTI.JavaEnv env, int arg1, Pointer arg2, Pointer arg3, Pointer arg4, Pointer arg5) throws Exception;
    void followReferences(JVMTI.JavaEnv env, int arg1, JniHandle arg2, JniHandle arg3, Pointer arg4, Pointer arg5) throws Exception;
    void iterateThroughHeap(JVMTI.JavaEnv env, int arg1, JniHandle arg2, Pointer arg3, Pointer arg4) throws Exception;
    */
    void setEventCallbacks(JVMTI.JavaEnv env, EventCallbacks eventCallbacks) throws Exception;
    /*
    void generateEvents(JVMTI.JavaEnv env, int arg1) throws Exception;
    void getExtensionFunctions(JVMTI.JavaEnv env, Pointer arg1, Pointer arg2) throws Exception;
    void getExtensionEvents(JVMTI.JavaEnv env, Pointer arg1, Pointer arg2) throws Exception;
    void setExtensionEventCallback(JVMTI.JavaEnv env, int arg1, Address arg2) throws Exception;
    */
    void disposeEnvironment(JVMTI.JavaEnv env) throws Exception;
    String getErrorName(JVMTI.JavaEnv env, int error) throws Exception;
    int getJLocationFormat(JVMTI.JavaEnv env) throws Exception;
    Properties getSystemProperties(JVMTI.JavaEnv env) throws Exception;
    String getSystemProperty(JVMTI.JavaEnv env, String key) throws Exception;
    void setSystemProperty(JVMTI.JavaEnv env, String key, String value) throws Exception;
    int getPhase(JVMTI.JavaEnv env) throws Exception;
//    void getCurrentThreadCpuTimerInfo(JVMTI.JavaEnv env, Pointer arg1) throws Exception;
    long getCurrentThreadCpuTime(JVMTI.JavaEnv env) throws Exception;
//    void getThreadCpuTimerInfo(JVMTI.JavaEnv env, Pointer arg1) throws Exception;
    long getThreadCpuTime(JVMTI.JavaEnv env, Thread thread) throws Exception;
//    void getTimerInfo(JVMTI.JavaEnv env, Pointer arg1) throws Exception;
    long getTime(JVMTI.JavaEnv env) throws Exception;
    EnumSet<JVMTICapabilities.E> getPotentialCapabilities(JVMTI.JavaEnv env) throws Exception;
    void addCapabilities(JVMTI.JavaEnv env, EnumSet<JVMTICapabilities.E> caps) throws Exception;
    void relinquishCapabilities(JVMTI.JavaEnv env, EnumSet<JVMTICapabilities.E> caps) throws Exception;
    int getAvailableProcessors(JVMTI.JavaEnv env) throws Exception;
    ClassVersionInfo getClassVersionNumbers(JVMTI.JavaEnv env, Class klasss, ClassVersionInfo classVersionInfo) throws Exception;
    int getConstantPool(JVMTI.JavaEnv env, Class klass, byte[] pool) throws Exception;
    Object getEnvironmentLocalStorage(JVMTI.JavaEnv env) throws Exception;
    void setEnvironmentLocalStorage(JVMTI.JavaEnv env, Object data) throws Exception;
    void addToBootstrapClassLoaderSearch(JVMTI.JavaEnv env, String path) throws Exception;
    void setVerboseFlag(JVMTI.JavaEnv env, int arg1, boolean arg2) throws Exception;
    void addToSystemClassLoaderSearch(JVMTI.JavaEnv env, String path) throws Exception;
    void retransformClasses(JVMTI.JavaEnv env, Class[] klasses) throws Exception;
    MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(JVMTI.JavaEnv env, Thread thread) throws Exception;
    long getObjectSize(JVMTI.JavaEnv env, Object object) throws Exception;
//    void getLocalInstance(JVMTI.JavaEnv env, JniHandle arg1, int arg2, Pointer arg3) throws Exception;
}
