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

import static com.sun.max.vm.ext.jvmti.JVMTICapabilities.E.*;

import java.security.*;
import java.util.*;

import com.sun.max.unsafe.*;
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
 * unnecessary as agents can use standard Java synchronization mechanisms. The functions for environment
 * local storage and thread local storage are omitted as these can easily be handled directly by the agent.
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
        public int location;
        public MethodActor method;

        public FrameInfo() {
        }

        public FrameInfo(MethodActor method, int location) {
            this.method = method;
            this.location = location;
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiStackInfo">jvmtiStackInfo</a>.
     */
    public static class StackInfo {
        public Thread thread;
        public int state;
        public FrameInfo[] frameInfo;
        public int frameCount;

        public StackInfo() {
        }

        public StackInfo(Thread thread, int state, FrameInfo[] frameInfo, int frameCount) {
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

    public static class AddrLocation {
        Address startAddress;
        long location;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiThreadInfo">jvmtiThreadInfo</a>
     * Somewhat redundant but for completeness cf native JVMTI.
     */
    public static class ThreadInfo {
        public final String name;
        public final int priority;
        public final boolean isDaemon;
        public final ThreadGroup threadGroup;
        public final ClassLoader contextClassLoader;

        ThreadInfo(String name, int priority, boolean isDaemon, ThreadGroup threadGroup, ClassLoader contextClassLoader) {
            this.name = name;
            this.priority = priority;
            this.isDaemon = isDaemon;
            this.threadGroup = threadGroup;
            this.contextClassLoader = contextClassLoader;
        }
    }

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiThreadGroupInfo">jvmtiThreadGroupInfo</a>
     * Somewhat redundant but for completeness cf native JVMTI.
     */
    public static class ThreadGroupInfo {
        public final ThreadGroup parent;
        public final String name;
        public final int maxPriority;
        public final boolean isDaemon;

        ThreadGroupInfo(ThreadGroup parent, String name, int maxPriority, boolean isDaemon) {
            this.parent = parent;
            this.name = name;
            this.maxPriority = maxPriority;
            this.isDaemon = isDaemon;
        }
    }

    public static class ThreadGroupChildrenInfo {
        public final Thread[] threads;
        public final ThreadGroup[] groups;

        ThreadGroupChildrenInfo(Thread[] threads, ThreadGroup[] groups) {
            this.threads = threads;
            this.groups = groups;
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
     * N.B: Not all events are supported by the VM at this point. Those marked // TODO are not supported.
     */
    public interface EventCallbacks {

        /**
         * This is not really an event, but is essentially equivalent to <a
         * href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#startup">startup</a>, and it is more
         * more convenient to define it as an event in {@code JJVMTI}. <b>N.B.</b>This will only be called for agents
         * that are built into the boot image and it is called in {code PRIMORDIAL} mode, where VM functionality is very
         * limited. Dynamically loaded agents have their {@code onLoad} method called instead. Both variants will
         * receive the {@code vmInit} event, which is the best place to do general agent setup.
         */
        void onBoot();
        /*
         * The standard events. The method names match the JVMTI event names, modulo case and '_' differences.
         */
        void breakpoint(Thread thread, MethodActor method, long location);
        byte[] classFileLoadHook(ClassLoader loader, String name,
                        ProtectionDomain protectionDomain, byte[] classData);
        void classLoad(Thread thread, ClassActor klass);
        void compiledMethodLoad(MethodActor method, int codeSize, Address codeAddr, AddrLocation[] map, Object compileInfo);
        void compiledMethodUnload(MethodActor method, Address codeAddr);
        void dataDumpRequest(); // TODO
        void dynamicCodeGenerated(String name, Address codeAddr, int length); // TODO
        void exception(Thread thread, MethodActor method, long location, Object exception, MethodActor catchMethod, long catchLocation);
        void exceptionCatch(Thread thread, MethodActor method, long location, Object exception); // TODO
        void fieldAccess(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field);
        void fieldModification(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field, Object newValue);
        void framePop(Thread thread, MethodActor method, boolean wasPoppedByException);
        void garbageCollectionStart();
        void garbageCollectionFinish();
        void methodEntry(Thread thread, MethodActor method);
        void methodExit(Thread thread, MethodActor method, boolean exeception, Object returnValue);
        void monitorContendedEnter(Thread thread, Object object); // TODO
        void monitorContendedEntered(Thread thread, Object object); // TODO
        void monitorWait(Thread thread, Object object, long timeout); // TODO
        void monitorWaited(Thread thread, Object object, long timeout); // TODO
        void objectFree(Object tag); // TODO
        void resourceExhausted(int flags, String description); // TODO
        void singleStep(Thread thread, MethodActor method, long location);
        void threadStart(Thread thread);
        void threadEnd(Thread thread);
        void vmDeath();
        void vmInit();
        void vmObjectAlloc(Thread thread, Object object, ClassActor classActor, int size); // TODO
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

        /**
         * A Maxine-specific variant that just passes the object, which is much simpler and more
         * efficient for the client.
         */
        int heapIterationMax(Object object, Object userData);
    }

    /*
     * The following methods are independent of Class, Field and Method types.
     */

    void setEventNotificationMode(int mode, JVMTIEvents.E event, Thread thread) throws JJVMTIException;

    Thread[] getAllThreads() throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SUSPEND)
    void suspendThread(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SUSPEND)
    void resumeThread(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SUSPEND)
    int[] suspendThreadList(Thread[] threads) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SUSPEND)
    int[] resumeThreadList(Thread[] threads) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SIGNAL_THREAD)
    void interruptThread(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SIGNAL_THREAD)
    void stopThread(Thread thread, Throwable t) throws JJVMTIException;

    ThreadInfo getThreadInfo(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_OWNED_MONITOR_INFO)
    void getOwnedMonitorInfo(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_OWNED_MONITOR_STACK_DEPTH_INFO)
    MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_CURRENT_CONTENDED_MONITOR)
    Object getCurrentContendedMonitor(Thread thread) throws JJVMTIException;

    void runAgentThread(Thread thread, int priority) throws JJVMTIException;

    ThreadGroup[] getTopThreadGroups() throws JJVMTIException;

    ThreadGroupInfo getThreadGroupInfo(ThreadGroup threadGroup) throws JJVMTIException;

    ThreadGroupChildrenInfo getThreadGroupChildren(ThreadGroup threadGroup) throws JJVMTIException;

    int getFrameCount(Thread thread) throws JJVMTIException;

    int getThreadState(Thread thread) throws JJVMTIException;

    Thread getCurrentThread() throws JJVMTIException;

    FrameInfo getFrameLocation(Thread thread, int depth) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_FRAME_POP_EVENTS)
    void notifyFramePop(Thread thread, int depth) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    Object getLocalObject(Thread thread, int depth, int slot) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    int getLocalInt(Thread thread, int depth, int slot) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    long getLocalLong(Thread thread, int depth, int slot) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    float getLocalFloat(Thread thread, int depth, int slot) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    double getLocalDouble(Thread thread, int depth, int slot) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    void setLocalObject(Thread thread, int depth, int slot, Object value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    void setLocalInt(Thread thread, int depth, int slot, int value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    void setLocalLong(Thread thread, int depth, int slot, long value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    void setLocalFloat(Thread thread, int depth, int slot, float value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    void setLocalDouble(Thread thread, int arg2, int arg3, double value) throws JJVMTIException;

    int getObjectHashCode(Object object) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_MONITOR_INFO)
    ObjectMonitorUsage getObjectMonitorUsage(Object object) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SET_NATIVE_METHOD_PREFIX)
    void setNativeMethodPrefix(String prefix) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_SET_NATIVE_METHOD_PREFIX)
    void setNativeMethodPrefixes(String[] prefixes) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_POP_FRAME)
    void popFrame(Thread thread) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_FORCE_EARLY_RETURN)
    void forceEarlyReturnObject(Thread thread, Object value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_FORCE_EARLY_RETURN)
    void forceEarlyReturnInt(Thread thread, int value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_FORCE_EARLY_RETURN)
    void forceEarlyReturnLong(Thread thread, long value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_FORCE_EARLY_RETURN)
    void forceEarlyReturnFloat(Thread thread, float value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_FORCE_EARLY_RETURN)
    void forceEarlyReturnDouble(Thread thread, double value) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_FORCE_EARLY_RETURN)
    void forceEarlyReturnVoid(Thread thread) throws JJVMTIException;

    int getVersionNumber() throws JJVMTIException;

    EnumSet<JVMTICapabilities.E> getCapabilities() throws JJVMTIException;

    StackInfo[] getThreadListStackTraces(Thread[] threads, int maxFrameCount) throws JJVMTIException;

    StackInfo[] getAllStackTraces(int maxFrameCount) throws JJVMTIException;

    FrameInfo[] getStackTrace(Thread thread, int startDepth, int maxFrameCount) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_TAG_OBJECTS)
    Object getTag(Object object) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_TAG_OBJECTS)
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

    @JJVMTI_FUNCTION(cap = CAN_TAG_OBJECTS)
    void iterateThroughHeap(int filter, ClassActor classActor, HeapCallbacks heapCallbacks, Object userData) throws JJVMTIException;

    /**
     * Maxine-specific version invokes {@link HeapCallbacks#heapIteration(Object, Object)}.
     */
    void iterateThroughHeapMax(int filter, ClassActor classActor, HeapCallbacks heapCallbacks, Object userData) throws JJVMTIException;

    void disposeEnvironment() throws JJVMTIException;

    String getErrorName(int error) throws JJVMTIException;

    int getJLocationFormat() throws JJVMTIException;

    String[] getSystemProperties() throws JJVMTIException;

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

    void addToBootstrapClassLoaderSearch(String path) throws JJVMTIException;

    void setVerboseFlag(int flag, boolean value) throws JJVMTIException;

    void addToSystemClassLoaderSearch(String path) throws JJVMTIException;


    long getObjectSize(Object object) throws JJVMTIException;
//  void getLocalInstance(JniHandle arg1, int arg2, Pointer arg3) throws Exception;

    /*
     * These methods refer to methods, fields and classes. in native JVMTI these values are represented
     * by scalar values that correspond to Maxine's MemberID, ClassID. Here we choose to use the Maxine
     * Actor types.
     */

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_BREAKPOINT_EVENTS)
    void setBreakpoint(ClassMethodActor method, long location) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_BREAKPOINT_EVENTS)
    void clearBreakpoint(ClassMethodActor method, long location) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_FIELD_ACCESS_EVENTS)
    void setFieldAccessWatch(FieldActor field) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_FIELD_ACCESS_EVENTS)
    void clearFieldAccessWatch(FieldActor field) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_FIELD_MODIFICATION_EVENTS)
    void setFieldModificationWatch(FieldActor field) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GENERATE_FIELD_MODIFICATION_EVENTS)
    void clearFieldModificationWatch(FieldActor field) throws JJVMTIException;

    boolean isModifiableClass(ClassActor klass) throws JJVMTIException;

    String getClassSignature(ClassActor klass) throws JJVMTIException;

    int getClassStatus(ClassActor klass) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_SOURCE_FILE_NAME)
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

    @JJVMTI_FUNCTION(cap = CAN_GET_SYNTHETIC_ATTRIBUTE)
    boolean isFieldSynthetic(FieldActor field) throws JJVMTIException;

    String getMethodName(MethodActor method) throws JJVMTIException;

    String getMethodSignature(MethodActor method) throws JJVMTIException;

    String getMethodGenericSignature(MethodActor method) throws JJVMTIException;

    ClassActor getMethodDeclaringClass(MethodActor method) throws JJVMTIException;

    int getMethodModifiers(MethodActor method) throws JJVMTIException;

    int getMaxLocals(ClassMethodActor method) throws JJVMTIException;

    int getArgumentsSize(ClassMethodActor method) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_LINE_NUMBERS)
    LineNumberEntry[] getLineNumberTable(ClassMethodActor method) throws JJVMTIException;

    MethodLocation getMethodLocation(ClassMethodActor method) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_ACCESS_LOCAL_VARIABLES)
    LocalVariableEntry[] getLocalVariableTable(ClassMethodActor member) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_BYTECODES)
    byte[] getBytecodes(ClassMethodActor method) throws JJVMTIException;

    boolean isMethodNative(MethodActor method) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_SYNTHETIC_ATTRIBUTE)
    boolean isMethodSynthetic(MethodActor method) throws JJVMTIException;

    ClassActor[] getLoadedClasses() throws JJVMTIException;

    ClassActor[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_REDEFINE_CLASSES)
    void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException;

    String getSourceDebugExtension(ClassActor klass) throws JJVMTIException;

    boolean isMethodObsolete(MethodActor method) throws JJVMTIException;

    ClassVersionInfo getClassVersionNumbers(ClassActor klass) throws JJVMTIException;

    @JJVMTI_FUNCTION(cap = CAN_GET_CONSTANT_POOL)
    byte[] getConstantPool(ClassActor klass) throws JJVMTIException;

    void retransformClasses(ClassActor[] klasses) throws JJVMTIException;

    /**
     * Maxine-specific call to include VM classes in the analysis, which are usually suppressed.
     */
    void includeMaxVMClasses();

}
