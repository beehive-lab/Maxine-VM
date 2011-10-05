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

import static com.sun.max.vm.jvmti.JVMTIConstants.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
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
        SingleThreadStackTraceVmOperation vmOperation = new SingleThreadStackTraceVmOperation(vmThread, startDepth, maxFrameCount, frameBuffer);
        vmOperation.submit();
        countPtr.setInt(vmOperation.stackTraceVisitor.frameBufferIndex);
        return JVMTI_ERROR_NONE;
    }

    private static class SingleThreadStackTraceVmOperation extends VmOperation {
        StackTraceVisitor stackTraceVisitor;
        CountStackTraceVisitor countStackTraceVisitor;

        SingleThreadStackTraceVmOperation(VmThread vmThread, int startDepth, int maxCount, Pointer frameBuffer) {
            super("JVMTISingleStackTrace", vmThread, Mode.Safepoint);
            if (startDepth < 0) {
                countStackTraceVisitor = new CountStackTraceVisitor();
            }
            stackTraceVisitor = new StackTraceVisitor(vmThread, startDepth, maxCount, frameBuffer);
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            if (countStackTraceVisitor != null) {
                countStackTraceVisitor.walk(null, ip, sp, fp);
                stackTraceVisitor.stackDepth = countStackTraceVisitor.depth;
            }
            stackTraceVisitor.walk(null, ip, sp, fp);
        }
    }

    private static class StackTraceVisitor extends SourceFrameVisitor {
        int startDepth;
        int maxCount;
        int depth;
        int frameBufferIndex;
        Pointer frameBuffer;
        int stackDepth = -1;  // actual depth if startDepth < 0
        VmThread vmThread;

        StackTraceVisitor(VmThread vmThread, int startDepth, int maxCount, Pointer frameBuffer) {
            this.startDepth = startDepth;
            this.maxCount = maxCount;
            this.frameBuffer = frameBuffer;
            this.vmThread = vmThread;
        }

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            ClassMethodActor original = method.original();
            if (original.holder().isReflectionStub()) {
                // ignore invocation stubs
                return true;
            }
            boolean record = startDepth < 0 ? depth >= stackDepth + startDepth : depth >= startDepth;
            if (record) {
                frameBuffer.setWord(frameBufferIndex, MethodID.fromMethodActor(original));
                frameBuffer.setLong(frameBufferIndex, bci);
                frameBufferIndex++;
                if (frameBufferIndex >= maxCount) {
                    return false;
                }
            }
            depth++;
            return true;
        }

    }

    private static class CountStackTraceVisitor  extends SourceFrameVisitor {
        int depth;

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            ClassMethodActor original = method.original();
            if (original.holder().isReflectionStub()) {
                // ignore invocation stubs
                return true;
            }
            depth++;
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
            Log.print("operateOnThread: "); Log.print(vmThread.getName());
            for (int i = 0; i < threads.length; i++) {
                if (VmThread.fromJava(threads[i]) == vmThread) {
                    Log.println(": true");
                    return true;
                }
            }
            Log.println(": false");
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
    private static native void setJVMTIStackInfo(Pointer stackInfo, int index, Word threadHandle,
                    int state, Pointer frameBuffer, int frameount);

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


}
