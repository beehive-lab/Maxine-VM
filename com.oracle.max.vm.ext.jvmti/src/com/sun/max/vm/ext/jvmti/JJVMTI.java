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
 * A functionally equivalent <A href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html">JVMTI</a> interface
 * but cast in terms of standard Java types that can be called from agents written in Java (for Maxine).
 *
 * Some of the JVMTI functions are redundant in that they essentially replicate existing functionality in the JDK,
 * however, we include them for completeness.
 *
 * A few of the JVMTI functions don't have a Java equivalent, and these are omitted.
 *
 */
public interface JJVMTI {

    /**
     * JVMTI errors. Whereas native JVMTI indicates errors by a return code, Java JVMTI uses an exception with the error
     * code as argument. Since errors are rare this is a {@link RuntimeException}.
     */
    public class Exception extends RuntimeException {
        public final int error;

        public Exception(int error) {
            super();
            this.error = error;
        }

        @Override
        public String toString() {
            return getClass().getName() + ": " + Integer.toString(error);
        }
    }

    public static abstract class RawMonitor {
        // TODO
    }

    /*
     * Immutable value classes used to convey results and values to callbacks.
     */

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiFrameInfo">jvmtiFrameInfo</a>.
     */
    public static class FrameInfo {
        public final Method method;
        public final int location;

        public FrameInfo(Method method, int location) {
            this.method = method;
            this.location = location;
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiStackInfo">jvmtiStackInfo</a>.
     */
    public static class StackInfo {
        public final Thread thread;
        public final int state;
        public final FrameInfo frameInfo;
        public final int frameCount;

        public StackInfo(Thread thread, int state, FrameInfo frameInfo, int frameCount) {
            this.thread = thread;
            this.state = state;
            this.frameInfo = frameInfo;
            this.frameCount = frameCount;
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#GetMethodLocation">GetMethodLocation</a>.
     */
    public static class MethodLocation {
        public final int start;
        public final int end;

        public MethodLocation(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiLineNumberEntry">jvmtiLineNumberEntry</a>.
     */
    public static class LineNumberEntry {
        public final int bci;
        public final int lineNumber;

        public LineNumberEntry(int bci, int lineNumber) {
            this.bci = bci;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiLocalVariableEntry">jvmtiLocalVariableEntry</a>.
     */
    public static class LocalVariableEntry {
        public final long location;
        public final int length;
        public final String name;
        public final String signature;
        public final String genericSignature;
        public final int slot;

        public LocalVariableEntry(long location, int length, String name, String signature, String genericSignature, int slot) {
            this.location = location;
            this.length = length;
            this.name = name;
            this.signature = signature;
            this.genericSignature = genericSignature;
            this.slot = slot;
        }
    }

    public static class ClassDefinition {
        public final Class klass;
        public final byte[] classBytes;

        public ClassDefinition(Class klass, byte[] classBytes) {
            this.klass = klass;
            this.classBytes = classBytes;
        }
    }

    public static class ClassVersionInfo {
        public final int major;
        public final int minor;

        public ClassVersionInfo(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }
    }

    public static class ObjectMonitorUsage {
        // TODO
    }

    public static class MonitorStackDepthInfo {
        public final Object monitor;
        public final int stackDepth;

        MonitorStackDepthInfo(Object monitor, int stackDepth) {
            this.monitor = monitor;
            this.stackDepth = stackDepth;
        }
    }

    /**
     * Event callbacks to a JJVMTI agent. Unlike the native JVMTI interface where callbacks are registered individually,
     * Java agents register a single object and use overriding to handle just those events they want. The delivery of
     * events is still also controlled by {@link #setEventNotificationMode} as per the JVMTI spec.
     *
     * These are defined in a separate interface partly to call them out and partly to permit some implementation
     * flexibility.
     *
     * TODO: Not all events are supported yet.
     */
    public interface EventCallbacks {
        /**
         * Not really an event callback, equivalent to
         * <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#startup">startup</a>,
         * and more convenient to put it here.
         */
        void agentStartup();
        void breakpoint(Thread thread, Method method, long location);
        /**
         * There is no CLASS_PREPARE event as Maxine cannot usefully distinguish it from CLASS_LOAD.
          */
        void classLoad(Thread thread, Class klass);
        byte[] classFileLoadHook(ClassLoader loader, String name,
                               ProtectionDomain protectionDomain, byte[] classData);
        /**
         * There is no VM_START event as Maxine cannot usefully distinguish it from VM_INIT.
         */
        void garbageCollectionStart();
        void garbageCollectionFinish();
        void methodEntry(Thread thread, Method method);
        void methodExit(Thread thread, Method method);
        void threadStart(Thread thread);
        void threadEnd(Thread thread);
        void vmDeath();
        void vmInit();
    }

    /*
     * Methods that can be called from the JJVMTI agent. They correspond essentially 1-1 to the JVMTI native
     * interface functions.
     */

    void setEventNotificationMode(int mode, int event, Thread thread) throws Exception;
    Thread[] getAllThreads() throws Exception;
    void suspendThread(Thread thread) throws Exception;
    void resumeThread(Thread thread) throws Exception;
    void stopThread(Thread thread, Throwable t) throws Exception;
    void interruptThread(Thread thread) throws Exception;
    void getThreadInfo(Thread thread) throws Exception;
    void getOwnedMonitorInfo(Thread thread) throws Exception;
    Object getCurrentContendedMonitor(Thread thread) throws Exception;
    void runAgentThread(Runnable runnable, int priority) throws Exception;
    ThreadGroup[] getTopThreadGroups() throws Exception;
    void getThreadGroupInfo(ThreadGroup threadGroup) throws Exception;
    void getThreadGroupChildren(ThreadGroup threadGroup) throws Exception;
    int getFrameCount(Thread thread) throws Exception;
    int getThreadState(Thread thread) throws Exception;
    Thread getCurrentThread() throws Exception;
    FrameInfo getFrameLocation(Thread thread, int depth) throws Exception;
    void notifyFramePop(Thread thread, int depth) throws Exception;
    Object getLocalObject(Thread thread, int depth, int slot) throws Exception;
    int getLocalInt(Thread thread, int depth, int slot) throws Exception;
    long getLocalLong(Thread thread, int depth, int slot) throws Exception;
    float getLocalFloat(Thread thread, int depth, int slot) throws Exception;
    double getLocalDouble(Thread thread, int depth, int slot) throws Exception;
    void setLocalObject(Thread thread, int depth, int slot, Object value) throws Exception;
    void setLocalInt(Thread thread, int depth, int slot, int value) throws Exception;
    void setLocalLong(Thread thread, int depth, int slot, long value) throws Exception;
    void setLocalFloat(Thread thread, int depth, int slot, float value) throws Exception;
    void setLocalDouble(Thread thread, int arg2, int arg3, double value) throws Exception;
    RawMonitor createRawMonitor(String name) throws Exception;
    void destroyRawMonitor(RawMonitor rawMonitor) throws Exception;
    void rawMonitorEnter(RawMonitor rawMonitor) throws Exception;
    void rawMonitorExit(RawMonitor rawMonitor) throws Exception;
    void rawMonitorWait(RawMonitor rawMonitor, long arg2) throws Exception;
    void rawMonitorNotify(RawMonitor rawMonitor) throws Exception;
    void rawMonitorNotifyAll(RawMonitor rawMonitor) throws Exception;
    void setBreakpoint(Method method, long location) throws Exception;
    void clearBreakpoint(Method method, long location) throws Exception;
    void setFieldAccessWatch(Field field) throws Exception;
    void clearFieldAccessWatch(Field field) throws Exception;
    void setFieldModificationWatch(Field field) throws Exception;
    void clearFieldModificationWatch(Field field) throws Exception;
    boolean isModifiableClass(Class klass) throws Exception;
    String getClassSignature(Class klass) throws Exception;
    int getClassStatus(Class klass) throws Exception;
    String getSourceFileName(Class klass) throws Exception;
    int getClassModifiers(Class klass) throws Exception;
    Method[] getClassMethods(Class klass) throws Exception;
    Field[] getClassFields(Class klass) throws Exception;
    Class[] getImplementedInterfaces(Class klass) throws Exception;
    boolean isInterface(Class klass) throws Exception;
    boolean isArrayClass(Class klass) throws Exception;
    ClassLoader getClassLoader(Class klass) throws Exception;
    int getObjectHashCode(Object object) throws Exception;
    ObjectMonitorUsage getObjectMonitorUsage(Object object) throws Exception;
    String getFieldName(Field field) throws Exception;
    String getFieldSignature(Field field) throws Exception;
    Class getFieldDeclaringClass(Field field) throws Exception;
    int getFieldModifiers(Field field) throws Exception;
    boolean isFieldSynthetic(Field field) throws Exception;
    String getMethodName(Method method) throws Exception;
    String getMethodSignature(Method method) throws Exception;
    String getMethodGenericSignature(Method method) throws Exception;
    Class getMethodDeclaringClass(Method method) throws Exception;
    int getMethodModifiers(Method method) throws Exception;
    int getMaxLocals(Method method) throws Exception;
    int getArgumentsSize(Method method) throws Exception;
    LineNumberEntry[] getLineNumberTable(Method method) throws Exception;
    MethodLocation getMethodLocation(Method method) throws Exception;
    LocalVariableEntry[] getLocalVariableTable(Method method) throws Exception;
    void setNativeMethodPrefix(String prefix) throws Exception;
    void setNativeMethodPrefixes(String[] prefixes) throws Exception;
    byte[] getBytecodes(Method method, byte[] useThis) throws Exception;
    boolean isMethodNative(Method method) throws Exception;
    boolean isMethodSynthetic(Method method) throws Exception;
    Class[] getLoadedClasses() throws Exception;
    Class[] getClassLoaderClasses(ClassLoader loader) throws Exception;
    void popFrame(Thread thread) throws Exception;
    void forceEarlyReturnObject(Thread thread, Object value) throws Exception;
    void forceEarlyReturnInt(Thread thread, int value) throws Exception;
    void forceEarlyReturnLong(Thread thread, long value) throws Exception;
    void forceEarlyReturnFloat(Thread thread, float value) throws Exception;
    void forceEarlyReturnDouble(Thread thread, double value) throws Exception;
    void forceEarlyReturnVoid(Thread thread) throws Exception;
    void redefineClasses(ClassDefinition[] classDefinitions) throws Exception;
    int getVersionNumber() throws Exception;
    long getCapabilities() throws Exception;
    String getSourceDebugExtension(Class klass) throws Exception;
    boolean isMethodObsolete(Method method) throws Exception;
    void suspendThreadList(Thread[] threads) throws Exception;
    void resumeThreadList(Thread[] threads) throws Exception;
    StackInfo[] getAllStackTraces(int maxFrameCount) throws Exception;
    void getThreadListStackTraces(Thread[] threads, int maxFrameCount) throws Exception;
    Object getThreadLocalStorage(Thread thread) throws Exception;
    void setThreadLocalStorage(Thread thread, Object data) throws Exception;
    int getStackTrace(Thread thread, int startDepth, int maxFrameCount, FrameInfo[] stackframeInfo) throws Exception;
    Object getTag(Object object) throws Exception;
    void setTag(Object object, Object tag) throws Exception;
    void forceGarbageCollection() throws Exception;
    /*
    void iterateOverObjectsReachableFromObject(JniHandle arg1, Address arg2, Pointer arg3) throws Exception;
    void iterateOverReachableObjects(Address arg1, Address arg2, Address arg3, Pointer arg4) throws Exception;
    void iterateOverHeap(int arg1, Address arg2, Pointer arg3) throws Exception;
    void iterateOverInstancesOfClass(JniHandle arg1, int arg2, Address arg3, Pointer arg4) throws Exception;
    void getObjectsWithTags(int arg1, Pointer arg2, Pointer arg3, Pointer arg4, Pointer arg5) throws Exception;
    void followReferences(int arg1, JniHandle arg2, JniHandle arg3, Pointer arg4, Pointer arg5) throws Exception;
    void iterateThroughHeap(int arg1, JniHandle arg2, Pointer arg3, Pointer arg4) throws Exception;
    */
    void setEventCallbacks(EventCallbacks eventCallbacks) throws Exception;
    /*
    void generateEvents(int arg1) throws Exception;
    void getExtensionFunctions(Pointer arg1, Pointer arg2) throws Exception;
    void getExtensionEvents(Pointer arg1, Pointer arg2) throws Exception;
    void setExtensionEventCallback(int arg1, Address arg2) throws Exception;
    */
    void disposeEnvironment() throws Exception;
    String getErrorName(int error) throws Exception;
    int getJLocationFormat() throws Exception;
    Properties getSystemProperties() throws Exception;
    String getSystemProperty(String key) throws Exception;
    void setSystemProperty(String key, String value) throws Exception;
    int getPhase() throws Exception;
//    void getCurrentThreadCpuTimerInfo(Pointer arg1) throws Exception;
    long getCurrentThreadCpuTime() throws Exception;
//    void getThreadCpuTimerInfo(Pointer arg1) throws Exception;
    long getThreadCpuTime(Thread thread) throws Exception;
//    void getTimerInfo(Pointer arg1) throws Exception;
    long getTime() throws Exception;
    EnumSet<JVMTICapabilities.E> getPotentialCapabilities() throws Exception;
    void addCapabilities(EnumSet<JVMTICapabilities.E> caps) throws Exception;
    void relinquishCapabilities(EnumSet<JVMTICapabilities.E> caps) throws Exception;
    int getAvailableProcessors() throws Exception;
    ClassVersionInfo getClassVersionNumbers(Class klasss, ClassVersionInfo classVersionInfo) throws Exception;
    int getConstantPool(Class klass, byte[] pool) throws Exception;
    Object getEnvironmentLocalStorage() throws Exception;
    void setEnvironmentLocalStorage(Object data) throws Exception;
    void addToBootstrapClassLoaderSearch(String path) throws Exception;
    void setVerboseFlag(int arg1, boolean arg2) throws Exception;
    void addToSystemClassLoaderSearch(String path) throws Exception;
    void retransformClasses(Class[] klasses) throws Exception;
    MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(Thread thread) throws Exception;
    long getObjectSize(Object object) throws Exception;
//    void getLocalInstance(JniHandle arg1, int arg2, Pointer arg3) throws Exception;
}
