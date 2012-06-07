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

/**
 * Types and methods that are common to {@link JJVMTIStd} and {@link JJVMTIMax}.
 */
public interface JJVMTICommon {
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
     * This class is incomplete as the type of the method object is left to the subclass.
     */
    public static abstract class FrameInfo {
        // method of frame specified by subclass
        public final int location;

        public FrameInfo(int location) {
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
        byte[] classFileLoadHook(ClassLoader loader, String name,
                        ProtectionDomain protectionDomain, byte[] classData);
        void garbageCollectionStart();
        void garbageCollectionFinish();
        void threadStart(Thread thread);
        void threadEnd(Thread thread);
        void vmDeath();
        /**
         * There is no VM_START event as Maxine cannot usefully distinguish it from VM_INIT.
         */
        void vmInit();
    }

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
    void iterateThroughHeap(int arg1, JniHandle arg2, Pointer arg3, Pointer arg4) throws Exception;

    void generateEvents(int arg1) throws Exception;
    void getExtensionFunctions(Pointer arg1, Pointer arg2) throws Exception;
    void getExtensionEvents(Pointer arg1, Pointer arg2) throws Exception;
    void setExtensionEventCallback(int arg1, Address arg2) throws Exception;
    */

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

}
