/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.JVMTIUtil.TypedData;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.VmOperation;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Support for the JVMTI functions related to {@link Thread}.
 */
class JVMTIThreadFunctions {
    static int getAllThreads(Pointer threadsCountPtr, Pointer threadsPtr) {
        final Thread[] threads = VmThreadMap.getThreads(false);
        threadsCountPtr.setInt(threads.length);
        Pointer threadCArray = Memory.allocate(Size.fromInt(threads.length * Word.size()));
        if (threadCArray.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        threadsPtr.setWord(threadCArray);
        for (int i = 0; i < threads.length; i++) {
            Thread thread = threads[i];
            threadCArray.setWord(i, JniHandles.createLocalHandle(thread));
        }
        return JVMTI_ERROR_NONE;
    }

    static int getThreadState(Thread thread, Pointer threadStatePtr) {
        threadStatePtr.setInt(getThreadState(VmThread.fromJava(thread)));
        return JVMTI_ERROR_NONE;
    }

    static int getThreadState(VmThread vmThread) {
        // TODO Maxine does not keep any state beyond Thread.State so the detailed JVMTI states are unavailable
        int state = 0;
        if (vmThread != null) {
            switch (vmThread.state()) {
                case TERMINATED:
                    state = JVMTI_THREAD_STATE_TERMINATED;
                    break;
                case NEW:
                    state = JVMTI_THREAD_STATE_ALIVE;
                    break;
                case RUNNABLE:
                    state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_RUNNABLE;
                    break;
                case BLOCKED:
                    state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;
                    break;
                case WAITING:
                    state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY;
                    break;
                case TIMED_WAITING:
                    state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT;
            }
        }
        return state;
    }

    static int getThreadInfo(Thread thread, Pointer threadInfoPtr) {
        if (thread == null) {
            thread = VmThread.current().javaThread();
        }
        setJVMTIThreadInfo(threadInfoPtr, CString.utf8FromJava(thread.getName()), thread.getPriority(), thread.isDaemon(),
                        JniHandles.createLocalHandle(thread.getThreadGroup()),
                        JniHandles.createLocalHandle(thread.getContextClassLoader()));
        return JVMTI_ERROR_NONE;
    }

    /**
     * Type too complex to handle in Java, delegate to native code.
     */
    @C_FUNCTION
    private static native void setJVMTIThreadInfo(Pointer threadInfoPtr, Pointer name, int priority, boolean isDaemon, JniHandle threadGroup, JniHandle contextClassLoader);

    // Stack trace handling

    // Single thread stack trace

    static int getStackTrace(Thread thread, int startDepth, int maxFrameCount, Pointer frameBuffer, Pointer countPtr) {
        VmThread vmThread = thread == null ? VmThread.current() : VmThread.fromJava(thread);
        StackTraceVisitor stackTraceVisitor = new StackTraceVisitor(vmThread, startDepth, maxFrameCount, frameBuffer);
        SingleThreadStackTraceVmOperation vmOperation = new SingleThreadStackTraceVmOperation(vmThread, stackTraceVisitor);
        vmOperation.submit();
        countPtr.setInt(stackTraceVisitor.frameBufferIndex);
        return JVMTI_ERROR_NONE;
    }

    /**
     * Base class for stack visiting that carries the current depth of the walk and
     * (optionally) the computed depth of the stack.
     */
    private static abstract class BaseStackTraceVisitor extends SourceFrameVisitor {
        int stackDepth;       // actual pre-computed logical depth
        int trapDepth;        // depth at which logical stack begins
        int depth;            // current depth of visitor
        ClassMethodActor original;

        /**
         * Checks for reflection stubs and if {@link ClassMethodActor#original()} is different.
         * @return null if it is a stub, otherwise {@link ClassMethodActor#original()}
         */
        protected boolean stubCheck(ClassMethodActor methodActor) {
            original = methodActor.original();
            if (original.holder().isReflectionStub()) {
                // ignore invocation stubs
                return true;
            }
            return false;
        }
    }

    /**
     * Supports a variety of single-thread stack walks.
     * {@link BaseStackTraceVisitor}
     */
    private static class SingleThreadStackTraceVmOperation extends VmOperation {
        /**
         * A stack visitor that counts the logical stack depth, by which we mean
         * not counting all the frame that are on the stack because of the
         * way {@link VMOperation} brings a thread to a safepoint.
         */
        private static class CountStackTraceVisitor extends BaseStackTraceVisitor {

            @Override
            public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
                if (trapped) {
                    trapDepth = depth;
                    stackDepth = 0;
                }
                if (!stubCheck(method)) {
                    stackDepth++;
                    depth++;
                }
                return true;
            }
        }

        BaseStackTraceVisitor stackTraceVisitor;
        CountStackTraceVisitor countStackTraceVisitor;

        /**
         * Create a {@link VmOperation} that runs the given {@link BaseStackTraceVisitor} on the given thread.
         * @param vmThread
         * @param stackTraceVisitor
         */
        SingleThreadStackTraceVmOperation(VmThread vmThread, BaseStackTraceVisitor stackTraceVisitor) {
            super("JVMTISingleStackTrace", vmThread, Mode.Safepoint);
            countStackTraceVisitor = new CountStackTraceVisitor();
            this.stackTraceVisitor = stackTraceVisitor;
        }

        /**
         * Degenerate variant that simply counts the stack depth.
         * @param vmThread
         */
        SingleThreadStackTraceVmOperation(VmThread vmThread) {
            this(vmThread, null);
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            countStackTraceVisitor.walk(null, ip, sp, fp);
            if (stackTraceVisitor != null) {
                stackTraceVisitor.stackDepth = countStackTraceVisitor.stackDepth;
                stackTraceVisitor.trapDepth = countStackTraceVisitor.trapDepth;
            }
            if (stackTraceVisitor != null) {
                stackTraceVisitor.walk(null, ip, sp, fp);
            }
        }
    }

    /**
     * Multi-purpose visitor for the different variants of the stack trace functions.
     */
    private static class StackTraceVisitor extends BaseStackTraceVisitor {
        int startDepth;       // first frame to record; > 0 => from top, < 0 from bottom
        int maxCount;         // max number of frames to record
        int frameBufferIndex; // in range 0 .. maxCount - 1
        Pointer frameBuffer;  // C struct for storing info
        VmThread vmThread;    // thread associated with this stack

        StackTraceVisitor(VmThread vmThread, int startDepth, int maxCount, Pointer frameBuffer) {
            this.startDepth = startDepth;
            this.maxCount = maxCount;
            this.frameBuffer = frameBuffer;
            this.vmThread = vmThread;
        }

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (!stubCheck(method)) {
                boolean record = startDepth < 0 ? depth >= trapDepth + stackDepth + startDepth : depth >= trapDepth + startDepth;
                if (record) {
                    setJVMTIFrameInfo(frameBuffer, frameBufferIndex, MethodID.fromMethodActor(original), bci);
                    frameBufferIndex++;
                    if (frameBufferIndex >= maxCount) {
                        return false;
                    }
                }
                depth++;
            }
            return true;
        }
    }

    // Stack traces for all threads

    static int getAllStackTraces(int maxFrameCount, Pointer stackInfoPtrPtr, Pointer threadCountPtr) {
        return getThreadListStackTraces(VmThreadMap.getThreads(false), maxFrameCount, stackInfoPtrPtr, threadCountPtr);
    }

    static int getThreadListStackTraces(int threadCount, Pointer threadList, int maxFrameCount, Pointer stackInfoPtrPtr) {
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            try {
                Thread thread = (Thread) threadList.getWord(i).asJniHandle().unhand();
                threads[i] = thread;
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
        }
        return getThreadListStackTraces(threads, maxFrameCount, stackInfoPtrPtr, Pointer.zero());
    }

    private static int getThreadListStackTraces(Thread[] threads, int maxFrameCount, Pointer stackInfoPtrPtr, Pointer threadCountPtr) {
        int threadCount = threads.length;
        // Have to preallocate all the memory in one contiguous chunk
        Pointer stackInfoArrayPtr = Memory.allocate(Size.fromInt(threadCount * (stackInfoSize() + maxFrameCount * FRAME_INFO_STRUCT_SIZE)));
        if (stackInfoArrayPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }

        MultipleThreadStackTraceVmOperation vmOperation = new MultipleThreadStackTraceVmOperation(maxFrameCount, threads, stackInfoArrayPtr);
        vmOperation.submit();
        for (int i = 0; i < threadCount; i++) {
            StackTraceVisitor sv = vmOperation.stackTraceList.get(i);
            setJVMTIStackInfo(stackInfoArrayPtr, i, JniHandles.createLocalHandle(sv.vmThread.javaThread()),
                            getThreadState(sv.vmThread), sv.frameBuffer, sv.frameBufferIndex);
        }
        stackInfoPtrPtr.setWord(stackInfoArrayPtr);
        if (!threadCountPtr.isZero()) {
            threadCountPtr.setInt(threadCount);
        }
        return JVMTI_ERROR_NONE;
    }

    private static class MultipleThreadStackTraceVmOperation extends VmOperation {
        private int maxFrameCount;
        ArrayList<StackTraceVisitor> stackTraceList = new ArrayList<StackTraceVisitor>();
        Thread[] threads;
        Pointer frameBuffersBasePtr;
        int count;

        MultipleThreadStackTraceVmOperation(int maxFrameCount, Thread[] threads, Pointer stackInfoArrayPtr) {
            super("JVMTIMultipleStackTrace", null, Mode.Safepoint);
            this.maxFrameCount = maxFrameCount;
            this.threads = threads;
            this.frameBuffersBasePtr = stackInfoArrayPtr.plus(threads.length * stackInfoSize());
        }

        @Override
        protected boolean operateOnThread(VmThread vmThread) {
            for (int i = 0; i < threads.length; i++) {
                if (VmThread.fromJava(threads[i]) == vmThread) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            Pointer frameBuffer = frameBuffersBasePtr.plus(count * FRAME_INFO_STRUCT_SIZE);
            StackTraceVisitor stackTraceVisitor = new StackTraceVisitor(vmThread, 0, maxFrameCount, frameBuffer);
            stackTraceList.add(stackTraceVisitor);
            stackTraceVisitor.walk(null, ip, sp, fp);
            count++;
        }

    }

    private static final int FRAME_INFO_STRUCT_SIZE = Word.size() * 2;

    private static int stackInfoSize;

    private static int stackInfoSize() {
        if (stackInfoSize == 0) {
            stackInfoSize = getJVMTIStackInfoSize();
        }
        return stackInfoSize;
    }

    @C_FUNCTION
    private static native int getJVMTIStackInfoSize();

    @C_FUNCTION
    private static native void setJVMTIFrameInfo(Pointer frameInfo, int index, Word methodID, long location);

    @C_FUNCTION
    private static native void setJVMTIStackInfo(Pointer stackInfo, int index, Word threadHandle,
                    int state, Pointer frameBuffer, int frameount);

    // Frame operations

    static int getFrameCount(Thread thread, Pointer countPtr) {
        VmThread vmThread = thread == null ? VmThread.current() : VmThread.fromJava(thread);
        if (vmThread == null || vmThread.state() == Thread.State.TERMINATED) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        SingleThreadStackTraceVmOperation op = new SingleThreadStackTraceVmOperation(vmThread);
        op.submit();
        countPtr.setInt(op.countStackTraceVisitor.stackDepth);
        return JVMTI_ERROR_NONE;
    }

    private static class FrameLocationStackTraceVisitor extends BaseStackTraceVisitor {
        int targetDepth;
        ClassMethodActor targetMethod;
        int targetBCI;

        FrameLocationStackTraceVisitor(int targetDepth) {
            this.targetDepth = targetDepth;
        }

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (!stubCheck(method)) {
                if (depth == targetDepth + trapDepth) {
                    targetMethod = original;
                    targetBCI = bci;
                    return false;
                }
                depth++;
            }
            return true;
        }

    }

    static int getFrameLocation(Thread thread, int depth, Pointer methodPtr, Pointer locationPtr) {
        VmThread vmThread = thread == null ? VmThread.current() : VmThread.fromJava(thread);
        if (vmThread == null || vmThread.state() == Thread.State.TERMINATED) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        if (depth < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        FrameLocationStackTraceVisitor stackVisitor = new FrameLocationStackTraceVisitor(depth);
        SingleThreadStackTraceVmOperation op = new SingleThreadStackTraceVmOperation(vmThread, stackVisitor);
        op.submit();
        if (stackVisitor.targetMethod == null) {
            return JVMTI_ERROR_NO_MORE_FRAMES;
        }
        methodPtr.setWord(MethodID.fromMethodActor(stackVisitor.targetMethod));
        locationPtr.setLong(stackVisitor.targetBCI);
        return JVMTI_ERROR_NONE;
    }

    private static class GetPutValueStackFrameVisitor extends BaseStackTraceVisitor {
        int targetDepth;
        int slot;
        boolean isSet;
        TypedData typedData;
        int jvmtiError = JVMTI_ERROR_NO_MORE_FRAMES;

        GetPutValueStackFrameVisitor(boolean isSet, int targetDepth, int slot, TypedData typedData) {
            this.isSet = isSet;
            this.targetDepth = targetDepth;
            this.slot = slot;
            this.typedData = typedData;
        }

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (!stubCheck(method)) {
                if (depth == targetDepth + trapDepth) {
                    Log.println("depth match");
                    LocalVariableTable.Entry[] entries = original.codeAttribute().localVariableTable().entries();
                    if (entries.length == 0) {
                        jvmtiError = JVMTI_ERROR_INVALID_SLOT;
                    } else {
                        for (int i = 0; i < entries.length; i++) {
                            LocalVariableTable.Entry entry = entries[i];
                            if (entry.slot() == slot) {
                                String slotType = original.holder().constantPool().utf8At(entry.descriptorIndex(), "local variable type").toString();
                                if (slotType.charAt(0) == typedData.tag) {
                                    TargetMethod targetMethod = original.currentTargetMethod();
                                    if (!targetMethod.isBaseline()) {
                                        jvmtiError = JVMTI_ERROR_INVALID_SLOT;
                                    }
                                    int offset = targetMethod.frameLayout().localVariableOffset(slot);
                                    Pointer varPtr = this.currentCursor.fp();

                                    switch (typedData.tag) {
                                        case 'L':
                                            if (isSet) {
                                                varPtr.writeReference(offset, Reference.fromJava(typedData.objectValue));
                                            } else {
                                                typedData.objectValue = varPtr.readReference(offset).toJava();
                                            }
                                            break;

                                        case 'F':
                                            if (isSet) {
                                                varPtr.writeFloat(offset, typedData.floatValue);
                                            } else {
                                                typedData.floatValue = varPtr.readFloat(offset);
                                            }
                                            break;

                                        case 'D':
                                            if (isSet) {
                                                varPtr.writeDouble(offset, typedData.doubleValue);
                                            } else {
                                                typedData.doubleValue = varPtr.readDouble(offset);
                                            }
                                            break;

                                        default:
                                            if (isSet) {
                                                varPtr.writeWord(offset, typedData.wordValue);
                                            } else {
                                                typedData.wordValue = varPtr.readWord(offset);
                                            }
                                    }
                                    jvmtiError = JVMTI_ERROR_NONE;
                                    return false;
                                }
                            }
                        }
                    }
                }
                depth++;
            }
            return true;
        }
    }

    @NEVER_INLINE
    private static void debug(Object obj) {

    }

    static int getLocalValue(Thread thread, int depth, int slot, Pointer valuePtr, char type) {
        if (thread == null) {
            thread = VmThread.current().javaThread();
        }
        TypedData typedData = new TypedData(type);
        GetPutValueStackFrameVisitor getValueStackFrameVisitor = new GetPutValueStackFrameVisitor(false, depth, slot, typedData);
        SingleThreadStackTraceVmOperation op = new SingleThreadStackTraceVmOperation(VmThread.fromJava(thread), getValueStackFrameVisitor);
        op.submit();
        if (getValueStackFrameVisitor.jvmtiError == JVMTI_ERROR_NONE) {
            if (type == 'L') {
                valuePtr.setWord(JniHandles.createLocalHandle(getValueStackFrameVisitor.typedData.objectValue));
            } else if (type == 'F') {
                valuePtr.setFloat(getValueStackFrameVisitor.typedData.floatValue);
            } else if (type == 'D') {
                valuePtr.setDouble(getValueStackFrameVisitor.typedData.doubleValue);
            } else {
                valuePtr.setWord(getValueStackFrameVisitor.typedData.wordValue);
            }
        }
        return getValueStackFrameVisitor.jvmtiError;
    }

    static int setLocalValue(Thread thread, int depth, int slot, TypedData typedData) {
        if (thread == null) {
            thread = VmThread.current().javaThread();
        }
        GetPutValueStackFrameVisitor putValueStackFrameVisitor = new GetPutValueStackFrameVisitor(true, depth, slot, typedData);
        SingleThreadStackTraceVmOperation op = new SingleThreadStackTraceVmOperation(VmThread.fromJava(thread), putValueStackFrameVisitor);
        op.submit();
        return putValueStackFrameVisitor.jvmtiError;
    }

    static int setLocalInt(Thread thread, int depth, int slot, int value) {
        return setLocalValue(thread, depth, slot, new TypedData(TypedData.DATA_INT, value));
    }

    static int setLocalLong(Thread thread, int depth, int slot, long value) {
        return setLocalValue(thread, depth, slot, new TypedData(TypedData.DATA_LONG, value));
    }

    static int setLocalFloat(Thread thread, int depth, int slot, float value) {
        return setLocalValue(thread, depth, slot, new TypedData(TypedData.DATA_FLOAT, value));
    }

    static int setLocalDouble(Thread thread, int depth, int slot, double value) {
        return setLocalValue(thread, depth, slot, new TypedData(TypedData.DATA_DOUBLE, value));
    }

    static int setLocalObject(Thread thread, int depth, int slot, Object value) {
        return setLocalValue(thread, depth, slot, new TypedData(TypedData.DATA_OBJECT, value));
    }

    // Thread suspend/resume

    static int suspendThread(Thread thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    static int suspendThreadList(int requestCount, Pointer requestList, Pointer results) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    static int resumeThread(Thread thread) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    static int resumeThreadList(int requestCount, Pointer requestList, Pointer results) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    // ThreadGroup functions

    static int getTopThreadGroups(Pointer countPtr, Pointer groupsPtrPtr) {
        countPtr.setInt(1);
        Pointer groupsPtr = Memory.allocate(Size.fromInt(Word.size()));
        groupsPtr.setWord(JniHandles.createLocalHandle(VmThread.systemThreadGroup));
        groupsPtrPtr.setWord(groupsPtr);
        return JVMTI_ERROR_NONE;
    }

    static int getThreadGroupInfo(ThreadGroup threadGroup, Pointer infoPtr) {
        setThreadGroupInfo(infoPtr, JniHandles.createLocalHandle(threadGroup.getParent()),
                        CString.utf8FromJava(threadGroup.getName()), threadGroup.getMaxPriority(),
                        threadGroup.isDaemon());
        return JVMTI_ERROR_NONE;
    }

    @C_FUNCTION
    private static native void setThreadGroupInfo(Pointer threadGroupInfoPtr, Word parent,
                    Pointer name, int maxPriority, boolean isDaemon);


    static int getThreadGroupChildren(ThreadGroup threadGroup, Pointer threadCountPtr, Pointer threadsPtrPtr, Pointer groupCountPtr, Pointer groupsPtrPtr) {
        // We reach directly into the ThreadGroup class state to avoid security checks and clumsy iterators.
        ThreadGroupProxy proxy = ThreadGroupProxy.asThreadGroupProxy(threadGroup);
        Thread[] threads = proxy.threads;
        ThreadGroup[] threadGroups = proxy.groups;
        // Holding the lock means no changes to the ThreadGroup, however threads may die at any time.
        synchronized (threadGroup) {
            int nGroups = 0;
            int nThreads = 0;
            for (int i = 0; i < proxy.ngroups; i++) {
                if (!threadGroups[i].isDestroyed()) {
                    nGroups++;
                }
            }
            for (int i = 0; i < proxy.nthreads; i++) {
                if (threads[i].isAlive()) {
                    nThreads++;
                }
            }
            Pointer threadsPtr = Memory.allocate(Size.fromInt(nThreads * Word.size()));
            if (threadsPtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
            Pointer threadGroupsPtr = Memory.allocate(Size.fromInt(nGroups * Word.size()));
            if (threadGroupsPtr.isZero()) {
                Memory.deallocate(threadsPtr);
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
            // we recompute the live thread count
            int liveThreads = 0;
            for (int i = 0; i < nThreads; i++) {
                if (threads[i].isAlive()) {
                    threadsPtr.setWord(i, JniHandles.createLocalHandle(threads[i]));
                    liveThreads++;
                }
            }
            for (int i = 0; i < nGroups; i++) {
                if (!threadGroups[i].isDestroyed()) {
                    threadGroupsPtr.setWord(i, JniHandles.createLocalHandle(threadGroups[i]));
                }
            }
            threadCountPtr.setInt(liveThreads);
            groupCountPtr.setInt(nGroups);
            threadsPtrPtr.setWord(threadsPtr);
            groupsPtrPtr.setWord(threadGroupsPtr);

            return JVMTI_ERROR_NONE;
        }
    }

    static class ThreadGroupProxy {
        @INTRINSIC(UNSAFE_CAST) public static native ThreadGroupProxy asThreadGroupProxy(Object object);

        @ALIAS(declaringClass = ThreadGroup.class)
        private Thread[] threads;
        @ALIAS(declaringClass = ThreadGroup.class)
        private ThreadGroup[] groups;
        @ALIAS(declaringClass = ThreadGroup.class)
        private int nthreads;
        @ALIAS(declaringClass = ThreadGroup.class)
        private int ngroups;

    }

}
