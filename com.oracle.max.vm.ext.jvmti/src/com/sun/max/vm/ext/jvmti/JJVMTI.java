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

import java.security.*;
import java.util.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
/**
 * A functionally equivalent <A href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html">JVMTI</a> interface
 * but cast in terms of Maxine Java types that can be called from agents written in Java (for Maxine).
 *
 * Some of the JVMTI functions are redundant in that they essentially replicate existing functionality in the JDK,
 * however, we include them for completeness.
 *
 * A few of the JVMTI functions don't have a Java equivalent, and these are omitted. Raw monitors are
 * unnecessary as agents can use standard Java synchronization mechanisms.
 *
 * Whereas native JVMTI returns errors as the function result, {@link JJVMTI} throws a {@link JJVMTIException}.
 *
 * Classes, fields and methods are denoted using the Maxine {@link Actor} classes. The other choice would have been to use
 * the types defined in the standard reflection package. The native JVMTI API uses JNI handles for Class instances and
 * scalar values for field and method instances, the latter corresponding to Maxine's {@link MemberID} class.
 * One reason for not using the standard reflection types is that a {@link Method} instance cannot represent a, {@code <init>}
 * method, which is represented with {@link Constructor}, whereas Maxine's {@link MethodActor} can represent all method instances
 * uniformly. In contrast, the API uses {@link Thread} over Maxine's {@link VmThread}, as there is no compelling reason to choose
 * the latter. Conversion between the Maxine types and platform types is, if necessary, straightforward using the {@code fromJava}
 * and {@code toJava} methods.
 */
public interface JJVMTI {
    /**
     * JVMTI errors. Whereas native JVMTI indicates errors by a return code, Java JVMTI uses an exception with the error
     * code as argument. Since errors are rare this is a {@link RuntimeException}.
     */
    public static class JJVMTIException extends RuntimeException {
        public final int error;

        public JJVMTIException(int error) {
            super();
            this.error = error;
        }

        @Override
        public String toString() {
            return getClass().getName() + ": " + Integer.toString(error);
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiFrameInfo">jvmtiFrameInfo</a>.
     */
    public static class FrameInfo {
        public final int location;
        public final MethodActor method;

        public FrameInfo(MethodActor method, int location) {
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
        public final ClassActor klass;
        public final byte[] classBytes;

        public ClassDefinition(ClassActor klass, byte[] classBytes) {
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
     * There is no separate {@code VM_START} event as Maxine cannot usefully distinguish it from {@code VM_INIT}.
     * There is no separate {@code CLASS_PREPARE} event as Maxine cannot usefully distinguish it from {@code CLASS_LOAD}.
     *
     * TODO: Not all events are supported yet.
     */
    public interface EventCallbacks {
        /**
         * This is not really an event, but is essentially equivalent to
         * <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#startup">startup</a>,
         * and it is more more convenient to define it here. <b>N.B.</b>This will only be called for agents
         * that are built into the boot image and in {code PRIMORDIAL} mode, whereVM functionality is
         * very limited. Dynamically loaded agents have their {@code onLoad} method called instead.
         */
        void onBoot();
        void breakpoint(Thread thread, MethodActor method, long location);
        byte[] classFileLoadHook(ClassLoader loader, String name,
                        ProtectionDomain protectionDomain, byte[] classData);
        void classLoad(Thread thread, ClassActor klass);
        void fieldAccess(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field);
        void fieldModification(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field, Object newValue);
        void garbageCollectionStart();
        void garbageCollectionFinish();
        void methodEntry(Thread thread, MethodActor method);
        void methodExit(Thread thread, MethodActor method, boolean exeception, Object returnValue);
        void threadStart(Thread thread);
        void threadEnd(Thread thread);
        void vmDeath();
        /**
         * This callback is the recommended place to do the majority of agent setup as it is called for both
         * dynamically loaded and boot image agents.
         */
        void vmInit();
    }

    /**
     * Heap callbacks.
     * TODO: complete
     */

    public interface HeapCallbacks {
        /**
         * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiHeapIterationCallback">heap iteration callback</a>.
         * @return visit control flags
         */
        int heapIteration(Object classTag, long size, Object objectTag, int length, Object userData);
    }

    /*
     * The following methods are independent of Class, Field and Method types.
     */

    void setEventNotificationMode(int mode, int event, Thread thread) throws JJVMTIException;
    Thread[] getAllThreads() throws JJVMTIException;
    void suspendThread(Thread thread) throws JJVMTIException;
    void resumeThread(Thread thread) throws JJVMTIException;
    void stopThread(Thread thread, Throwable t) throws JJVMTIException;
    void interruptThread(Thread thread) throws JJVMTIException;
    void getThreadInfo(Thread thread) throws JJVMTIException;
    void getOwnedMonitorInfo(Thread thread) throws JJVMTIException;
    Object getCurrentContendedMonitor(Thread thread) throws JJVMTIException;
    void runAgentThread(Runnable runnable, int priority) throws JJVMTIException;
    ThreadGroup[] getTopThreadGroups() throws JJVMTIException;
    void getThreadGroupInfo(ThreadGroup threadGroup) throws JJVMTIException;
    void getThreadGroupChildren(ThreadGroup threadGroup) throws JJVMTIException;
    int getFrameCount(Thread thread) throws JJVMTIException;
    int getThreadState(Thread thread) throws JJVMTIException;
    Thread getCurrentThread() throws JJVMTIException;
    FrameInfo getFrameLocation(Thread thread, int depth) throws JJVMTIException;
    void notifyFramePop(Thread thread, int depth) throws JJVMTIException;
    Object getLocalObject(Thread thread, int depth, int slot) throws JJVMTIException;
    int getLocalInt(Thread thread, int depth, int slot) throws JJVMTIException;
    long getLocalLong(Thread thread, int depth, int slot) throws JJVMTIException;
    float getLocalFloat(Thread thread, int depth, int slot) throws JJVMTIException;
    double getLocalDouble(Thread thread, int depth, int slot) throws JJVMTIException;
    void setLocalObject(Thread thread, int depth, int slot, Object value) throws JJVMTIException;
    void setLocalInt(Thread thread, int depth, int slot, int value) throws JJVMTIException;
    void setLocalLong(Thread thread, int depth, int slot, long value) throws JJVMTIException;
    void setLocalFloat(Thread thread, int depth, int slot, float value) throws JJVMTIException;
    void setLocalDouble(Thread thread, int arg2, int arg3, double value) throws JJVMTIException;

    int getObjectHashCode(Object object) throws JJVMTIException;
    ObjectMonitorUsage getObjectMonitorUsage(Object object) throws JJVMTIException;

    void setNativeMethodPrefix(String prefix) throws JJVMTIException;
    void setNativeMethodPrefixes(String[] prefixes) throws JJVMTIException;

    void popFrame(Thread thread) throws JJVMTIException;
    void forceEarlyReturnObject(Thread thread, Object value) throws JJVMTIException;
    void forceEarlyReturnInt(Thread thread, int value) throws JJVMTIException;
    void forceEarlyReturnLong(Thread thread, long value) throws JJVMTIException;
    void forceEarlyReturnFloat(Thread thread, float value) throws JJVMTIException;
    void forceEarlyReturnDouble(Thread thread, double value) throws JJVMTIException;
    void forceEarlyReturnVoid(Thread thread) throws JJVMTIException;

    int getVersionNumber() throws JJVMTIException;
    long getCapabilities() throws JJVMTIException;

    void suspendThreadList(Thread[] threads) throws JJVMTIException;
    void resumeThreadList(Thread[] threads) throws JJVMTIException;
    void getThreadListStackTraces(Thread[] threads, int maxFrameCount) throws JJVMTIException;
    Object getThreadLocalStorage(Thread thread) throws JJVMTIException;
    void setThreadLocalStorage(Thread thread, Object data) throws JJVMTIException;
    StackInfo[] getAllStackTraces(int maxFrameCount) throws JJVMTIException;
    int getStackTrace(Thread thread, int startDepth, int maxFrameCount, FrameInfo[] stackframeInfo) throws JJVMTIException;

    Object getTag(Object object) throws JJVMTIException;
    void setTag(Object object, Object tag) throws JJVMTIException;
    void forceGarbageCollection() throws JJVMTIException;

    /*
    void iterateOverObjectsReachableFromObject(JniHandle arg1, Address arg2, Pointer arg3) throws Exception;
    void iterateOverReachableObjects(Address arg1, Address arg2, Address arg3, Pointer arg4) throws Exception;
    void iterateOverHeap(int arg1, Address arg2, Pointer arg3) throws Exception;
    void iterateOverInstancesOfClass(JniHandle arg1, int arg2, Address arg3, Pointer arg4) throws Exception;
    void getObjectsWithTags(int arg1, Pointer arg2, Pointer arg3, Pointer arg4, Pointer arg5) throws Exception;
    void followReferences(int arg1, JniHandle arg2, JniHandle arg3, Pointer arg4, Pointer arg5) throws Exception;

    void generateEvents(int arg1) throws Exception;
    void getExtensionFunctions(Pointer arg1, Pointer arg2) throws Exception;
    void getExtensionEvents(Pointer arg1, Pointer arg2) throws Exception;
    void setExtensionEventCallback(int arg1, Address arg2) throws Exception;
    */

    void iterateThroughHeap(int filter, ClassActor classActor, HeapCallbacks heapCallbacks, Object userData) throws JJVMTIException;


    void disposeEnvironment() throws JJVMTIException;
    String getErrorName(int error) throws JJVMTIException;
    int getJLocationFormat() throws JJVMTIException;
    Properties getSystemProperties() throws JJVMTIException;
    String getSystemProperty(String key) throws JJVMTIException;
    void setSystemProperty(String key, String value) throws JJVMTIException;
    int getPhase() throws JJVMTIException;
//    void getCurrentThreadCpuTimerInfo(Pointer arg1) throws Exception;
    long getCurrentThreadCpuTime() throws JJVMTIException;
//    void getThreadCpuTimerInfo(Pointer arg1) throws Exception;
    long getThreadCpuTime(Thread thread) throws JJVMTIException;
//    void getTimerInfo(Pointer arg1) throws Exception;
    long getTime() throws JJVMTIException;
    EnumSet<JVMTICapabilities.E> getPotentialCapabilities() throws JJVMTIException;
    void addCapabilities(EnumSet<JVMTICapabilities.E> caps) throws JJVMTIException;
    void relinquishCapabilities(EnumSet<JVMTICapabilities.E> caps) throws JJVMTIException;
    int getAvailableProcessors() throws JJVMTIException;

    Object getEnvironmentLocalStorage() throws JJVMTIException;
    void setEnvironmentLocalStorage(Object data) throws JJVMTIException;
    void addToBootstrapClassLoaderSearch(String path) throws JJVMTIException;
    void setVerboseFlag(int arg1, boolean arg2) throws JJVMTIException;
    void addToSystemClassLoaderSearch(String path) throws JJVMTIException;

    MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(Thread thread) throws JJVMTIException;
    long getObjectSize(Object object) throws JJVMTIException;
//  void getLocalInstance(JniHandle arg1, int arg2, Pointer arg3) throws Exception;

    /*
     * These methods refer to methods, fields and classes. in native JVMTI these values are represented
     * by scalar values that correspond to Maxine's MemberID, ClassID. Here we choose to use the Maxine
     * Actor types.
     */

    void setBreakpoint(MethodActor method, long location) throws JJVMTIException;
    void clearBreakpoint(MethodActor method, long location) throws JJVMTIException;
    void setFieldAccessWatch(FieldActor field) throws JJVMTIException;
    void clearFieldAccessWatch(FieldActor field) throws JJVMTIException;
    void setFieldModificationWatch(FieldActor field) throws JJVMTIException;
    void clearFieldModificationWatch(FieldActor field) throws JJVMTIException;
    boolean isModifiableClass(ClassActor klass) throws JJVMTIException;
    String getClassSignature(ClassActor klass) throws JJVMTIException;
    int getClassStatus(ClassActor klass) throws JJVMTIException;
    String getSourceFileName(ClassActor klass) throws JJVMTIException;
    int getClassModifiers(ClassActor klass) throws JJVMTIException;
    MethodActor[] getClassMethods(ClassActor klass) throws JJVMTIException;
    FieldActor[] getClassFields(ClassActor klass) throws JJVMTIException;
    ClassActor[] getImplementedInterfaces(ClassActor klass) throws JJVMTIException;
    boolean isInterface(ClassActor klass) throws JJVMTIException;
    boolean isArrayClass(ClassActor klass) throws JJVMTIException;
    ClassLoader getClassLoader(ClassActor klass) throws JJVMTIException;
    String getFieldName(FieldActor field) throws JJVMTIException;
    String getFieldSignature(FieldActor field) throws JJVMTIException;
    ClassActor getFieldDeclaringClass(FieldActor field) throws JJVMTIException;
    int getFieldModifiers(FieldActor field) throws JJVMTIException;
    boolean isFieldSynthetic(FieldActor field) throws JJVMTIException;
    String getMethodName(MethodActor method) throws JJVMTIException;
    String getMethodSignature(MethodActor method) throws JJVMTIException;
    String getMethodGenericSignature(MethodActor method) throws JJVMTIException;
    ClassActor getMethodDeclaringClass(MethodActor method) throws JJVMTIException;
    int getMethodModifiers(MethodActor method) throws JJVMTIException;
    int getMaxLocals(MethodActor method) throws JJVMTIException;
    int getArgumentsSize(MethodActor method) throws JJVMTIException;
    LineNumberEntry[] getLineNumberTable(MethodActor method) throws JJVMTIException;
    MethodLocation getMethodLocation(MethodActor method) throws JJVMTIException;
    LocalVariableEntry[] getLocalVariableTable(MethodActor member) throws JJVMTIException;
    byte[] getBytecodes(MethodActor method, byte[] useThis) throws JJVMTIException;
    boolean isMethodNative(MethodActor method) throws JJVMTIException;
    boolean isMethodSynthetic(MethodActor method) throws JJVMTIException;
    ClassActor[] getLoadedClasses() throws JJVMTIException;
    ClassActor[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException;
    void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException;
    String getSourceDebugExtension(ClassActor klass) throws JJVMTIException;
    boolean isMethodObsolete(MethodActor method) throws JJVMTIException;
    ClassVersionInfo getClassVersionNumbers(ClassActor klasss, ClassVersionInfo classVersionInfo) throws JJVMTIException;
    int getConstantPool(ClassActor klass, byte[] pool) throws JJVMTIException;
    void retransformClasses(ClassActor[] klasses) throws JJVMTIException;

}
