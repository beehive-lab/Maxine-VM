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
import static com.sun.max.vm.ext.jvmti.JVMTIVmThreadLocal.*;
import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.ext.jvmti.JJVMTI.*;
import com.sun.max.vm.ext.jvmti.JVMTIUtil.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.run.java.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Support for the JVMTI functions related to {@link Thread}.
 */
public class JVMTIThreadFunctions {
    private static ClassActor vmThreadClassActor;
    private static ClassActor jniFunctionsClassActor;
    private static ClassActor vmThreadMapClassActor;
    private static ClassActor methodClassActor;
    private static ClassActor jvmtiClassActor;
    private static ClassActor javaRunSchemeClassActor;
    private static MethodActor[] stackBaseMethodActors;
    private static final LinkedList<StackElement> EMPTY_STACK = new LinkedList<StackElement>();

    static ClassActor vmThreadClassActor() {
        if (vmThreadClassActor == null) {
            vmThreadClassActor = ClassActor.fromJava(VmThread.class);
        }
        return vmThreadClassActor;
    }

    private static ClassActor vmThreadMapClassActor() {
        if (vmThreadMapClassActor == null) {
            vmThreadMapClassActor = ClassActor.fromJava(VmThreadMap.class);
        }
        return vmThreadMapClassActor;
    }

    private static ClassActor jniFunctionsClassActor() {
        if (jniFunctionsClassActor == null) {
            jniFunctionsClassActor = ClassActor.fromJava(JniFunctions.class);
        }
        return jniFunctionsClassActor;
    }

    static ClassActor methodClassActor() {
        if (methodClassActor == null) {
            methodClassActor = ClassActor.fromJava(Method.class);
        }
        return methodClassActor;
    }

    private static ClassActor jvmtiClassActor() {
        if (jvmtiClassActor == null) {
            jvmtiClassActor = ClassActor.fromJava(JVMTI.class);
        }
        return jvmtiClassActor;
    }

    private static ClassActor javaRunSchemeClassActor() {
        if (javaRunSchemeClassActor == null) {
            javaRunSchemeClassActor = ClassActor.fromJava(JavaRunScheme.class);
        }
        return javaRunSchemeClassActor;
    }

    static {
        // Should search for @JVMTI_STACKBASE
        stackBaseMethodActors = new MethodActor[1];
        try {
            stackBaseMethodActors[0] = MethodActor.fromJava(JVMTIBreakpoints.class.getDeclaredMethod("event", long.class));
        } catch (NoSuchMethodException ex) {
            ProgramError.unexpected("failed to find method actor for JVMTIBreakpoints.event", ex);
        }
    }

    static int getAllThreads(Pointer threadsCountPtr, Pointer threadsPtr) {
        final Thread[] threads = VmThreadMap.getThreads(true);
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
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        threadStatePtr.setInt(getThreadState(vmThread));
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
            if (VmOperation.isSuspendRequest(vmThread.tla())) {
                state |= JVMTI_THREAD_STATE_SUSPENDED;
            }
        }
        return state;
    }

    static ThreadInfo getThreadInfo(Thread thread) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            throw new JJVMTI.JJVMTIException(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        thread = vmThread.javaThread();
        return new ThreadInfo(thread.getName(), thread.getPriority(), thread.isDaemon(), thread.getThreadGroup(), thread.getContextClassLoader());
    }

    static int getThreadInfo(Thread thread, Pointer threadInfoPtr) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
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

    static FrameInfo[] getStackTrace(Thread thread, int startDepth, int maxFrameCount) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            throw new JJVMTI.JJVMTIException(JVMTI_ERROR_THREAD_NOT_ALIVE);
        }
        FrameInfo[] frameInfo = new FrameInfo[maxFrameCount];
        for (int i = 0; i < frameInfo.length; i++) {
            frameInfo[i] = new FrameInfo();
        }
        JavaThreadListStackTraceVisitor stackTraceVisitor = new JavaThreadListStackTraceVisitor(vmThread, startDepth, maxFrameCount, frameInfo);
        new SingleThreadStackTraceVmOperation(vmThread, stackTraceVisitor).submit();
        if (stackTraceVisitor.frameBufferIndex < frameInfo.length) {
            FrameInfo[] newFrameInfo = new FrameInfo[stackTraceVisitor.frameBufferIndex];
            System.arraycopy(frameInfo, 0, newFrameInfo, 0, stackTraceVisitor.frameBufferIndex);
            frameInfo = newFrameInfo;
        }
        return frameInfo;
    }

    static int getStackTrace(Thread thread, int startDepth, int maxFrameCount, Pointer frameBuffer, Pointer countPtr) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        NativeThreadListStackTraceVisitor stackTraceVisitor = new NativeThreadListStackTraceVisitor(vmThread, startDepth, maxFrameCount, frameBuffer);
        new SingleThreadStackTraceVmOperation(vmThread, stackTraceVisitor).submit();
        countPtr.setInt(stackTraceVisitor.frameBufferIndex);
        return JVMTI_ERROR_NONE;
    }

    private static class FrameAccessWithIP extends FrameAccess {
        CodePointer ip;

        @NEVER_INLINE
        //TODO remove
        void setCallerInfo(StackFrameCursor callerCursor) {
            this.setCallerInfo(callerCursor.sp(), callerCursor.fp());
        }

        FrameAccessWithIP(StackFrameCursor currentCursor) {
            super(currentCursor.csl(), currentCursor.csa(), currentCursor.sp(), currentCursor.fp(),
                            Pointer.zero(), Pointer.zero());
            if (currentCursor.ip.targetMethod() != null) {
                this.ip = currentCursor.vmIP();
            }

        }
    }

    static class StackElement {
        ClassMethodActor classMethodActor;
        int bci;
        FrameAccessWithIP frameAccess;

        StackElement(ClassMethodActor classMethodActor, int bci, FrameAccessWithIP frameAccess) {
            this.classMethodActor = classMethodActor;
            this.frameAccess = frameAccess;
            if (bci < 0) {
                // outside the actual bytecode area, e.g. in method entry count overflow code
                // make it look like a call from first instruction
                bci = 0;
            }
            this.bci = bci;
        }

    }

    /**
     * A stack visitor that analyses the stack, potentially removing
     * frames that are VM related. If {@link JVMTI#JVMTI_VM} is {@code true}
     * then all frames are gathered, otherwise we ignore the frames that are on the stack
     * because of the way {@link VMOperation} brings a thread to a safepoint, and
     * "implementation" frames, i.e., VM frames, reflection stubs.
     *
     * Since the decision to include VM frames is optional, we abstract this into the
     * notion of a "visible" frame.
     *
     * The nature of the mechanisms for freezing threads in {@link VMOperation}
     * and entering native code, means that there are always VM frames on the
     * stack that we do not want to include. In addition because JVMTI is
     * implemented in Java, any calls from agents in response to JVMTI events
     * and callbacks will also have stacks containing VM frames. Plus the
     * base of every thread stack has VM frames from the thread startup.
     *
     * The stack walker visits the stack top down, but an accurate picture
     * requires a bottom up analysis. So in a first top down pass we build a list
     * of {@link StackElement} which is an approximation, then re-analyse it
     * bottom up. The resulting list is then easy to use to answer all the
     * JVMTI query variants. The process isn't allocation free.
     *
     * In the initial scan downwards all VM frames are dropped until a non-VM frame
     * is seen, then all frames are kept (except reflection stubs).
     */
    static class FindAppFramesStackTraceVisitor extends SourceFrameVisitor {
        boolean seenVisibleFrame = JVMTI.JVMTI_VM;
        LinkedList<StackElement> stackElements = new LinkedList<StackElement>();
        StackElement[] stackElementsArray;    // strictly for ease of debugging in the Inspector
        boolean raw;

        @Override
        public boolean visitSourceFrame(ClassMethodActor methodActor, int bci, boolean trapped, long frameId) {
            // "trapped" indicates the frame in the safepoint trap handler.
            // In other stack visitors in the VM this causes a reset but,
            // in this context, it is subsumed by the check for VM frames.
            ClassMethodActor classMethodActor = methodActor.original();
            // check for first visible frame
            if (seenVisibleFrame) {
                add(classMethodActor, bci);
            } else {
                if (JVMTIClassFunctions.isVisibleClass(classMethodActor.holder())) {
                    seenVisibleFrame = true;
                    add(classMethodActor, bci);
                }
            }
            return true;
        }

        StackElement getStackElement(int depth) {
            assert depth < stackElements.size();
            return stackElements.get((stackElements.size() - 1) - depth);
        }

        private void add(ClassMethodActor classMethodActor, int bci) {
            if (JVMTI.JVMTI_VM || raw || !classMethodActor.holder().isReflectionStub()) {
                stackElements.addFirst(new StackElement(classMethodActor, bci, getFrameAccessWithIP()));
            }
        }

        protected FrameAccessWithIP getFrameAccessWithIP() {
            return null;
        }

        /**
         * Resets state if the visitor is being reused.
         */
        void reset() {
            seenVisibleFrame = JVMTI.JVMTI_VM;
            if (stackElements.size() > 0) {
                stackElements = new LinkedList<StackElement>();
            }
        }

        /**
         * Standard walk removes VM frames.
         */
        @Override
        public void walk(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp) {
            walkVariant(walker, ip, sp, fp, false);
        }

        /**
         * Raw walk that does not remove VM frames.
         */
        void walkRaw(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp) {
            walkVariant(walker, ip, sp, fp, true);
        }

        void walkVariant(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp, boolean raw) {
            this.raw = raw;
            super.walk(walker, ip, sp, fp);
            createDebugArray();
            if (!raw) {
                removeVMFrames();
            }
        }

        /**
         * Remove VM frames from the stack trace in {@link #stackElements}.
         * Analysing the stack to remove the VM frames depends on knowledge
         * of the way that threads start up. The normal case is {@link VmThread#run}
         * calls {@link VmThread#executeRunnable} which calls {@link Thread#run}.
         * The unusual case is an attached native thread which starts with
         * {@link JNIFunctions#CallStaticVoidMethodA}. A special case is the
         * main thread which has a {@link Method#invoke}, since it is is called from
         * {@link JavaRunScheme}, unless it has returned but the VM hasn't terminated, in which
         * case it is in {@link VmThreadMap#joinAllNonDaemons}. This analysis gets the
         * correct base frame. We then scan upwards. If we hit another VM frame we throw everything
         * away after that. Reason being that the initial down scan may have hit a platform
         * class method used by the VM, but couldn't know there might be another VM class below,
         * so decided it was an app frame.
         *
         * TODO handle user-defined classloader callbacks, which have VM frames sandwiched
         * between app frames.
         *
         * N.B. if we are including VM frames then the processing is different.
         */

        void removeVMFrames() {
            if (stackElements.size() == 0) {
                // some threads, e.g., SignalDispatcher, have only VM frames, so (usually) nothing to analyse.
                return;
            }

            if (JVMTI.JVMTI_VM) {
                // The usual dance to handle VM frame removal at the base is not appropriate.
                // However, some of the frames at the top of the stack should be removed,
                // e.g., those delivering a breakpoint event.
                stripJVMTIFrames();
                return;
            }

            int startIndex = -1;
            StackElement base = stackElements.getFirst();
            ClassActor classActor = base.classMethodActor.holder();
            if (classActor == vmThreadClassActor()) {
                base = stackElements.get(1);
                classActor = base.classMethodActor.holder();
                if (classActor == vmThreadClassActor) {
                    if (stackElements.get(2).classMethodActor.holder() == javaRunSchemeClassActor()) {
                        startIndex = 5;
                    } else {
                        startIndex = 2;
                    }
                } else if (classActor == vmThreadMapClassActor()) {
                    // main returned
                    stackElements = EMPTY_STACK;
                    return;
                } else if (classActor == methodClassActor()) {
                    startIndex = 3;
                } else if (classActor == jvmtiClassActor()) {
                    startIndex = 2;
                }
            } else if (classActor == jniFunctionsClassActor()) {
                startIndex = 3;
            }

            if (startIndex < 0) {
                // unexpected stack layout, log it and return empty
                Log.println("JVMTI: unexpected thread stack layout");
                stackElements = EMPTY_STACK;
                return;
            }

            // discard VM frames below first app frame
            for (int i = 0; i < startIndex; i++) {
                stackElements.remove();
            }
            // now 0 is the first app frame
            ListIterator<StackElement> iter = stackElements.listIterator();
            while (iter.hasNext()) {
                StackElement e = iter.next();
                classActor = e.classMethodActor.holder();
                if (JVMTIClassFunctions.isVMClass(classActor)) {
                    iter.remove();
                }
            }
        }

        /**
         * It is infinitely more convenient to see the stack elements in an array in the Inspector.
         */
        private void createDebugArray() {
            stackElementsArray = new StackElement[stackElements.size()];
            stackElements.toArray(stackElementsArray);
        }

        /**
         * When {@link JVMTI#JVMTI_VM} is {@code true} we strip out
         * frames that are part of the JVMTI implementation of, e.g., breakpoints.
         */
        private void stripJVMTIFrames() {
            // index 0 is base of stack
            int index = 0;
            for (int i = 0; i < stackElements.size(); i++) {
                MethodActor methodActor = stackElements.get(i).classMethodActor;
                for (MethodActor stackBaseMethodActor : stackBaseMethodActors) {
                    if (methodActor == stackBaseMethodActor) {
                        index = i;
                        break;
                    }
                }
            }
            if (index > 0) {
                removeElements(index);
            }
        }

        private void removeElements(int index) {
            int lastIndex = stackElements.size();
            for (int i = index; i < lastIndex; i++) {
                // this reduces the index of everything after removed item
                // so we always remove the same index.
                stackElements.remove(index);
            }
        }

    }

    /**
     * Visitor for copying portions of a stack to an agent provided buffer.
     * We do this as a visitor because it is used in single/multiple thread variants.
     */
    private static abstract class ThreadListStackTraceVisitor extends FindAppFramesStackTraceVisitor {
        int startDepth;       // first frame to record; > 0 => from top, < 0 from bottom
        int maxCount;         // max number of frames to record
        int frameBufferIndex; // in range 0 .. maxCount - 1
        VmThread vmThread;    // thread associated with this stack

        ThreadListStackTraceVisitor(VmThread vmThread, int startDepth, int maxCount) {
            this.startDepth = startDepth;
            this.maxCount = maxCount;
            this.vmThread = vmThread;
        }

        @Override
        public void walk(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp) {
            super.walk(walker, ip, sp, fp);
            if (startDepth < 0) {
                startDepth = stackElements.size() + startDepth;
            }
            for (int i = 0; i < stackElements.size(); i++) {
                if (i >= startDepth) {
                    if (store(getStackElement(i))) {
                        break;
                    }
                }
            }
        }

        /**
         * Subclass-specific mechanism for storing information.
         * @param se
         */
        protected abstract void subStore(StackElement se);

        protected boolean store(StackElement se) {
            if (frameBufferIndex >= maxCount) {
                return true;
            } else {
                subStore(se);
                frameBufferIndex++;
                return false;
            }
        }
    }

    private static class NativeThreadListStackTraceVisitor extends ThreadListStackTraceVisitor {
        Pointer frameBuffer;  // C struct for storing info

        NativeThreadListStackTraceVisitor(VmThread vmThread, int startDepth, int maxCount, Pointer frameBuffer) {
            super(vmThread, startDepth, maxCount);
            this.frameBuffer = frameBuffer;
        }

        @Override
        protected void subStore(StackElement se) {
            setJVMTIFrameInfo(frameBuffer, frameBufferIndex, MethodID.fromMethodActor(se.classMethodActor), se.bci);
        }
    }

    private static class JavaThreadListStackTraceVisitor extends ThreadListStackTraceVisitor {
        FrameInfo[] frameInfo;

        JavaThreadListStackTraceVisitor(VmThread vmThread, int startDepth, int maxCount, FrameInfo[] frameInfo) {
            super(vmThread, startDepth, maxCount);
            this.frameInfo = frameInfo;
        }

        @Override
        protected void subStore(StackElement se) {
            FrameInfo fi = frameInfo[frameBufferIndex];
            fi.method = se.classMethodActor;
            fi.location = se.bci;
        }
    }

    /**
     * Invokes a {@link FindAppFramesStackTraceVisitor} on a single thread in a {@link VmOperation}.
     * N.B. To workaround a limitation in {@link VmOperation} where a GC cannot be run during the
     * {@link VmOperation} unless all threads are stopped, we run this as a multi-thread operation
     * even though that is not strictly necessary. Typically everything is stopped anyway.
     *
     * The case where the current thread is making the request is handled specially as no
     * {@link VmOperation}is necessary, just the stack walk.
     */
    static class SingleThreadStackTraceVmOperation extends VmOperation {
        FindAppFramesStackTraceVisitor stackTraceVisitor;
        VmThread vmThread;

        /**
         * Create a {@link VmOperation} that runs the given {@link BaseStackTraceVisitor} on the given thread.
         * @param vmThread
         * @param stackTraceVisitor
         */
        private SingleThreadStackTraceVmOperation(VmThread vmThread, FindAppFramesStackTraceVisitor stackTraceVisitor) {
            super("JVMTISingleStackTrace", null, Mode.Safepoint);
            this.vmThread = vmThread;
            this.stackTraceVisitor = stackTraceVisitor;
        }

        @Override
        protected boolean operateOnThread(VmThread vmThread) {
            return vmThread == this.vmThread;
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            stackTraceVisitor.walk(new VmStackFrameWalker(vmThread.tla()), ip, sp, fp);
        }

        /**
         * The preferred way to invoke the operation, that encapsulates the special case of the current thread.
         * @param vmThread
         * @param stackTraceVisitor
         * @return {@code stackTraceVisitor} for convenience
         */
        static FindAppFramesStackTraceVisitor invoke(VmThread vmThread, FindAppFramesStackTraceVisitor stackTraceVisitor) {
            if (vmThread == VmThread.current()) {
                stackTraceVisitor.walk(new VmStackFrameWalker(vmThread.tla()), Address.fromLong(here()).asPointer(),
                                getAbiStackPointer(), getCpuFramePointer());
            } else {
                new SingleThreadStackTraceVmOperation(vmThread, stackTraceVisitor).submit();
            }
            return stackTraceVisitor;
        }

        static FindAppFramesStackTraceVisitor invoke(VmThread vmThread) {
            return SingleThreadStackTraceVmOperation.invoke(vmThread, new FindAppFramesStackTraceVisitor());
        }
    }

    // Stack traces for all threads

    private static class StackTraceUnion extends ModeUnion {
        // native
        Pointer stackInfoArrayPtr;
        // java
        StackInfo[] stackInfoArray;
        StackTraceUnion(boolean isNative) {
            super(isNative);
        }
    }

    static StackInfo[] getAllStackTraces(int maxFrameCount) {
        return getThreadListStackTraces(VmThreadMap.getThreads(false), maxFrameCount);
    }

    static StackInfo[] getThreadListStackTraces(Thread[] threads, int maxFrameCount) {
        StackTraceUnion stu = new StackTraceUnion(ModeUnion.JAVA);
        int error = getThreadListStackTraces(threads, maxFrameCount, stu);
        if (error != JVMTI_ERROR_NONE) {
            throw new JJVMTI.JJVMTIException(error);
        }
        return stu.stackInfoArray;
    }

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
        StackTraceUnion stu = new StackTraceUnion(ModeUnion.NATIVE);
        int error = getThreadListStackTraces(threads, maxFrameCount, stu);
        if (error == JVMTI_ERROR_NONE) {
            stackInfoPtrPtr.setWord(stu.stackInfoArrayPtr);
            if (!threadCountPtr.isZero()) {
                threadCountPtr.setInt(threads.length);
            }
        }
        return error;
    }

    private static int getThreadListStackTraces(Thread[] threads, int maxFrameCount, StackTraceUnion stu) {
        int threadCount = threads.length;
        Pointer frameBuffersBasePtr = Pointer.zero();

        if (stu.isNative) {
            // Have to preallocate all the memory in one contiguous chunk
            stu.stackInfoArrayPtr = Memory.allocate(Size.fromInt(threadCount * (stackInfoSize() + maxFrameCount * FRAME_INFO_STRUCT_SIZE)));
            if (stu.stackInfoArrayPtr.isZero()) {
                return JVMTI_ERROR_OUT_OF_MEMORY;
            }
            frameBuffersBasePtr = stu.stackInfoArrayPtr.plus(threadCount * stackInfoSize());
        } else {
            stu.stackInfoArray = new StackInfo[threadCount];
            for (int i = 0; i < stu.stackInfoArray.length; i++) {
                stu.stackInfoArray[i] = new StackInfo();
                FrameInfo[] frameInfo = new FrameInfo[maxFrameCount];
                for (int j = 0; j < maxFrameCount; j++) {
                    frameInfo[j] = new FrameInfo();
                }
                stu.stackInfoArray[i].frameInfo = frameInfo;
            }
        }
        ThreadListStackTraceVisitor[] stackTraceVisitors = null;
        if (stu.isNative) {
            stackTraceVisitors = new NativeThreadListStackTraceVisitor[threadCount];
            for (int i = 0; i < threadCount; i++) {
                stackTraceVisitors[i] = new NativeThreadListStackTraceVisitor(VmThread.fromJava(threads[i]), 0, maxFrameCount,
                                frameBuffersBasePtr.plus(i * maxFrameCount * FRAME_INFO_STRUCT_SIZE));
            }
        } else {
            stackTraceVisitors = new JavaThreadListStackTraceVisitor[threadCount];
            for (int i = 0; i < threadCount; i++) {
                stackTraceVisitors[i] = new JavaThreadListStackTraceVisitor(VmThread.fromJava(threads[i]), 0, maxFrameCount,
                                stu.stackInfoArray[i].frameInfo);
            }
        }

        new MultipleThreadStackTraceVmOperation(threads, stackTraceVisitors).submit();

        for (int i = 0; i < threadCount; i++) {
            ThreadListStackTraceVisitor sv = stackTraceVisitors[i];
            Thread thread = sv.vmThread.javaThread();
            int state = getThreadState(sv.vmThread);
            if (stu.isNative) {
                NativeThreadListStackTraceVisitor nsv = (NativeThreadListStackTraceVisitor) sv;
                setJVMTIStackInfo(stu.stackInfoArrayPtr, i, JniHandles.createLocalHandle(thread),
                            state, nsv.frameBuffer, nsv.frameBufferIndex);
            } else {
                StackInfo si = stu.stackInfoArray[i];
                si.thread = thread;
                si.state = state;
                si.frameCount = sv.frameBufferIndex;
            }
        }
        return JVMTI_ERROR_NONE;
    }

    /**
     * Invokes a {@link FindAppFramesStackTraceVisitor} on multiple threads in a {@link VmOperation}.
     */
    static class MultipleThreadStackTraceVmOperation extends VmOperation {
        FindAppFramesStackTraceVisitor[] stackTraceVisitors;
        Thread[] threads;

        MultipleThreadStackTraceVmOperation(Thread[] threads, FindAppFramesStackTraceVisitor[] stackTraceVisitors) {
            super("JVMTIMultipleStackTrace", null, Mode.Safepoint);
            this.threads = threads;
            this.stackTraceVisitors = stackTraceVisitors;
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

        private FindAppFramesStackTraceVisitor getStackTraceVisitor(VmThread vmThread) {
            for (int i = 0; i < threads.length; i++) {
                if (VmThread.fromJava(threads[i]) == vmThread) {
                    return stackTraceVisitors[i];
                }
            }
            assert false;
            return null;
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            getStackTraceVisitor(vmThread).walk(new VmStackFrameWalker(vmThread.tla()), ip, sp, fp);
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

    /**
     * Checks for current thread request ({@code thread == null}, and live state.
     * @return {@code null} if should return error, the {@link VmThread} otherwise.
     */
    static VmThread checkVmThread(Thread thread) {
        if (thread == null) {
            return VmThread.current();
        }
        VmThread vmThread = VmThread.fromJava(thread);
        if (vmThread == null || vmThread.state() == Thread.State.TERMINATED) {
            return null;
        }
        return vmThread;
    }

    /**
     * Checks for {@code null} which means current thread (which is alive).
     * else checks if alive.
     * @param thread
     * @return {@code null} if the thread is not alive, {@code thread} otherwise
     */
    static Thread checkThread(Thread thread) {
        if (thread == null) {
            return VmThread.current().javaThread();
        }
        if (!thread.isAlive()) {
            return null;
        }
        return thread;
    }

    static int getFrameCount(Thread thread, Pointer countPtr) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        FindAppFramesStackTraceVisitor stackTraceVisitor = SingleThreadStackTraceVmOperation.invoke(vmThread);
        countPtr.setInt(stackTraceVisitor.stackElements.size());
        return JVMTI_ERROR_NONE;
    }

    private static class FrameLocationUnion {
        boolean isNative;

    }

    private static class MethodActorLocation {
        MethodActor methodActor;
        int location;
    }

    static JJVMTI.FrameInfo getFrameLocation(Thread thread, int depth) throws JJVMTI.JJVMTIException {
        MethodActorLocation methodActorLocation = new MethodActorLocation();
        int error = getFrameLocation(methodActorLocation, thread, depth);
        if (error == JVMTI_ERROR_NONE) {
            return new FrameInfo(methodActorLocation.methodActor, methodActorLocation.location);
        } else {
            throw new JJVMTI.JJVMTIException(error);
        }
    }

    static int getFrameLocation(Thread thread, int depth, Pointer methodPtr, Pointer locationPtr) {
        MethodActorLocation methodActorLocation = new MethodActorLocation();
        int error = getFrameLocation(methodActorLocation, thread, depth);
        if (error == JVMTI_ERROR_NONE) {
            methodPtr.setWord(MethodID.fromMethodActor(methodActorLocation.methodActor));
            locationPtr.setLong(methodActorLocation.location);
        }
        return error;
    }

    static int getFrameLocation(MethodActorLocation methodActorLocation, Thread thread, int depth) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        if (depth < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        FindAppFramesStackTraceVisitor stackTraceVisitor = SingleThreadStackTraceVmOperation.invoke(vmThread);
        if (depth < stackTraceVisitor.stackElements.size()) {
            methodActorLocation.methodActor = stackTraceVisitor.getStackElement(depth).classMethodActor;
            methodActorLocation.location = stackTraceVisitor.getStackElement(depth).bci;
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_NO_MORE_FRAMES;
        }
    }

    /**
     * Used to carry the frame pop data from here through the event dispatch machinery.
     */
    static class FramePopEventData {
        MethodID methodID;
        boolean wasPoppedByException;
        Object value;
    }

    static class FramePopEventDataThreadLocal extends ThreadLocal<FramePopEventData> {
        @Override
        public FramePopEventData initialValue() {
            return new FramePopEventData();
        }
    }

    private static FramePopEventDataThreadLocal framePopEventDataTL = new FramePopEventDataThreadLocal();

    @NEVER_INLINE
    public static void framePopEvent(boolean wasPoppedByException, long value) {
        framePopEvent(wasPoppedByException, new Long(value));
    }
    @NEVER_INLINE
    public static void framePopEvent(boolean wasPoppedByException, float value) {
        framePopEvent(wasPoppedByException, new Float(value));
    }
    @NEVER_INLINE
    public static void framePopEvent(boolean wasPoppedByException, double value) {
        framePopEvent(wasPoppedByException, new Double(value));
    }
    @NEVER_INLINE
    public static void framePopEvent(boolean wasPoppedByException) {
        framePopEvent(wasPoppedByException, null);
    }

    /**
     * Invoked from compiled code before a frame is being popped, e.g. a return.
     * @param wasPoppedByException
     */
    @NEVER_INLINE
    public static void framePopEvent(boolean wasPoppedByException, Object value) {
        VmThread vmThread = VmThread.current();
        Pointer tla = vmThread.tla();
        if (JVMTIVmThreadLocal.bitIsSet(tla, JVMTI_FRAME_POP) || JVMTIEvent.isEventSet(JVMTIEvent.METHOD_EXIT)) {
            FindAppFramesStackTraceVisitor stackTraceVisitor = SingleThreadStackTraceVmOperation.invoke(vmThread);
            // if we are single stepping, we may need to deopt the method we are returning to
            if (stackTraceVisitor.stackElements.size() > 1) {
                long codeEventSettings = JVMTIEvent.codeEventSettings(null, vmThread);
                if ((codeEventSettings & JVMTIEvent.bitSetting(JVMTI_EVENT_SINGLE_STEP)) != 0) {
                    JVMTICode.checkDeOptForMethod(stackTraceVisitor.getStackElement(1).classMethodActor, codeEventSettings);
                }
            }
            // METHOD_EXIT events can cause frame pops from within VM code compiled at run time,
            // which results in an empty stack
            if (stackTraceVisitor.stackElements.size() > 0) {
                FramePopEventData framePopEventData = getFramePopEventData(
                                MethodID.fromMethodActor(stackTraceVisitor.getStackElement(0).classMethodActor),
                                wasPoppedByException,
                                value);
                if (JVMTIVmThreadLocal.bitIsSet(tla, JVMTI_FRAME_POP)) {
                    JVMTI.event(JVMTI_EVENT_FRAME_POP, framePopEventData);
                }

                if (JVMTIEvent.isEventSet(JVMTIEvent.METHOD_EXIT)) {
                    JVMTI.event(JVMTI_EVENT_METHOD_EXIT, framePopEventData);
                }
            }
        }
    }

    static FramePopEventData getFramePopEventData(MethodID methodID, boolean wasPoppedByException, Object value) {
        FramePopEventData framePopEventData = framePopEventDataTL.get();
        framePopEventData.methodID = methodID;
        framePopEventData.wasPoppedByException = wasPoppedByException;
        framePopEventData.value = value;
        return framePopEventData;
    }

    static int notifyFramePop(Thread thread, int depth) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        if (depth < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        if (depth > 0) {
            // need deopt for frames below us, else we won't get the frame pop events
            assert false;
        }
        FindAppFramesStackTraceVisitor stackTraceVisitor = SingleThreadStackTraceVmOperation.invoke(vmThread);
        if (depth < stackTraceVisitor.stackElements.size()) {
            Pointer tla = vmThread.tla();
            JVMTIVmThreadLocal.setDepth(tla, depth);
            JVMTIVmThreadLocal.setBit(tla, JVMTI_FRAME_POP);
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_NO_MORE_FRAMES;
        }
    }

    static int interruptThread(Thread thread) {
        if (!thread.isAlive()) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        VmThread.fromJava(thread).interrupt0();
        return JVMTI_ERROR_NONE;
    }

    /**
     * Visitor for getting/setting local variables in stack frames.
     */
    private static class GetSetStackTraceVisitor extends FindAppFramesStackTraceVisitor {
        int depth;
        int slot;
        TypedData typedData;
        boolean isSet;
        int returnCode = JVMTI_ERROR_NONE;
        FrameAccessWithIP calleeFrameAccess;

        GetSetStackTraceVisitor(int depth, int slot, TypedData typedData, boolean isSet) {
            this.depth = depth;
            this.slot = slot;
            this.typedData = typedData;
            this.isSet = isSet;
        }

        @Override
        protected FrameAccessWithIP getFrameAccessWithIP() {
            // this only changes for physical frames
            return calleeFrameAccess;
        }

        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            // This is the physical frame visit
            // since we walk down, we don't know the caller yet, but update the callee caller info to this frame.
            if (calleeFrameAccess != null) {
                calleeFrameAccess.setCallerInfo(current);
            }
            calleeFrameAccess = new FrameAccessWithIP(current);
            return super.visitFrame(current, callee);
        }

        @Override
        public void walk(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp) {
            super.walk(walker, ip, sp, fp);
            if (depth < stackElements.size()) {
                StackElement stackElement = getStackElement(depth);
                ClassMethodActor classMethodActor = stackElement.classMethodActor;
                // the stack elements are logical but there may be inlining, so we must
                // get the TargetMethod from the physical frame info.
                FrameAccessWithIP frameAccess = stackElement.frameAccess;
                TargetMethod targetMethod = frameAccess.ip.toTargetMethod();
                if (!targetMethod.isBaseline() && isSet) {
                    returnCode = JVMTI_ERROR_OPAQUE_FRAME; // TODO need deopt
                    return;
                }
                targetMethod.finalizeReferenceMaps();
                int spi = targetMethod.findSafepointIndex(frameAccess.ip);
                assert spi >= 0;
                CiFrame ciFrame = targetMethod.debugInfoAt(spi, isSet ? null : frameAccess).frame();
                if (slot >= ciFrame.numLocals) {
                    returnCode = JVMTI_ERROR_INVALID_SLOT;
                    return;
                }
                if (!typeCheck(classMethodActor)) {
                    returnCode = JVMTI_ERROR_TYPE_MISMATCH;
                    return;
                }

                CiConstant ciConstant = null;
                CiAddress ciAddress = null;
                if (isSet) {
                    ciAddress = (CiAddress)  ciFrame.getLocalValue(slot);
                } else {
                    ciConstant = (CiConstant) ciFrame.getLocalValue(slot);
                }
                // The type of ciConstant almost certainly will not be accurate,
                // for example, T1X only distinguishes reference (object) types; everything else is a long

                // Checkstyle: stop
                switch (typedData.tag) {
                    case 'L':
                        if (ciConstant.kind != CiKind.Object) {
                            returnCode = JVMTI_ERROR_TYPE_MISMATCH;
                            return;
                        }
                        if (isSet) {
                        } else {
                            typedData.objectValue = ciConstant.asObject();
                        }
                        break;

                    case 'F':
                        if (isSet) {
                        } else {
                            typedData.floatValue = Float.intBitsToFloat((int) ciConstant.asPrimitive());
                        }
                        break;

                    case 'D':
                        if (isSet) {
                        } else {
                            typedData.doubleValue = Double.longBitsToDouble(ciConstant.asPrimitive());
                        }
                        break;

                    case 'I':
                        if (isSet) {
                            Pointer varPtr = frameSlotAddress(ciAddress, frameAccess);
                            varPtr.setInt(typedData.intValue);
                        } else {
                            typedData.intValue = (int) ciConstant.asPrimitive();
                        }
                        break;

                    case 'J':
                        if (isSet) {
                        } else {
                            typedData.longValue = ciConstant.asLong();
                        }
                }
                // Checkstyle: resume
            } else {
                returnCode = JVMTI_ERROR_NO_MORE_FRAMES;
            }
        }

        @NEVER_INLINE
        private static Pointer frameSlotAddress(CiAddress address, FrameAccess frameAccess) {
            Pointer fpVal = frameAccess.fp;
            return fpVal.plus(address.displacement);
        }

        private boolean typeCheck(ClassMethodActor classMethodActor) {
            LocalVariableTable.Entry[] entries = classMethodActor.codeAttribute().localVariableTable().entries();
            if (entries.length > 0) {
                for (int i = 0; i < entries.length; i++) {
                    LocalVariableTable.Entry entry = entries[i];
                    if (entry.slot() == slot) {
                        String slotType = classMethodActor.holder().constantPool().utf8At(entry.descriptorIndex(), "local variable type").toString();
                        return typeMatch(slotType.charAt(0));
                    }
                }
                return false;
            } else {
                // no info so assume ok
                return true;
            }
        }

        private boolean typeMatch(int type) {
            if (typedData.tag == 'L') {
                return type == 'L' || type == '[';
            } else {
                if (typedData.tag == 'I') {
                    return type == 'Z' || type == 'B' || type == 'C' || type == 'S' || type == 'I';
                } else {
                    return type == typedData.tag;
                }
            }
        }
    }

    /**
     * {@code typedData} carries the type and the input value when {@code isSet}, and carries the output
     * value when not {@code isSet}.
     */
    private static int getOrSetLocalValue(Thread thread, int depth, int slot, TypedData typedData, boolean isSet) {
        VmThread vmThread = checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        GetSetStackTraceVisitor stackTraceVisitor = new GetSetStackTraceVisitor(depth, slot, typedData, isSet);
        SingleThreadStackTraceVmOperation.invoke(vmThread, stackTraceVisitor);
        return stackTraceVisitor.returnCode;
    }

    /**
     * Native variant. Places value in {@code valuePtr}.
     */
    static int getLocalValue(Thread thread, int depth, int slot, Pointer valuePtr, char type) {
        TypedData typedData = new TypedData(type);
        int error = getOrSetLocalValue(thread, depth, slot, typedData, false);
        if (error == JVMTI_ERROR_NONE) {
            switch (typedData.tag) {
                case 'L':
                    valuePtr.setWord(JniHandles.createLocalHandle(typedData.objectValue));
                    break;
                case 'D':
                    valuePtr.setDouble(typedData.doubleValue);
                    break;
                case 'F':
                    valuePtr.setFloat(typedData.floatValue);
                    break;
                case 'J':
                    valuePtr.setLong(typedData.longValue);
                    break;
                case 'I':
                    valuePtr.setInt(typedData.intValue);
                    break;
            }
        }
        return error;
    }

    static int setLocalValue(Thread thread, int depth, int slot, TypedData typedData) {
        return getOrSetLocalValue(thread, depth, slot, typedData, true);
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

    // JJVMTI get variants


    static TypedData getLocalValue(Thread thread, int depth, int slot, int typeTag) {
        TypedData typedData = new TypedData(typeTag);
        int error = getOrSetLocalValue(thread, depth, slot, typedData, false);
        if (error == JVMTI_ERROR_NONE) {
            return typedData;
        } else {
            throw new JJVMTI.JJVMTIException(error);
        }
    }

    static int getLocalInt(Thread thread, int depth, int slot) {
        return getLocalValue(thread, depth, slot, TypedData.DATA_INT).intValue;
    }

    static long getLocalLong(Thread thread, int depth, int slot) {
        return getLocalValue(thread, depth, slot, TypedData.DATA_LONG).longValue;
    }

    static float getLocalFloat(Thread thread, int depth, int slot) {
        return getLocalValue(thread, depth, slot, TypedData.DATA_FLOAT).floatValue;
    }

    static double getLocalDouble(Thread thread, int depth, int slot) {
        return getLocalValue(thread, depth, slot, TypedData.DATA_DOUBLE).doubleValue;
    }

    static Object getLocalObject(Thread thread, int depth, int slot) {
        return getLocalValue(thread, depth, slot, TypedData.DATA_OBJECT).objectValue;
    }

    // Thread suspend/resume/stop/interrupt

    static int suspendThread(JVMTI.Env jvmtiEnv, Thread thread) {
        int[] error = suspendThreadList(jvmtiEnv, new Thread[] {thread});
        return error[0];
    }

    static int[] suspendThreadList(JVMTI.Env jvmtiEnv, Thread[] threads) {
        Set<VmThread> threadSet = new HashSet<VmThread>();
        int[] errors = new int[threads.length];
        for (int i = 0; i < threads.length; i++) {
            VmThread vmThread = checkVmThread(threads[i]);
            if (vmThread == null) {
                errors[i] = JVMTI_ERROR_THREAD_NOT_ALIVE;
            } else if (VmOperation.isSuspendRequest(vmThread.tla())) {
                errors[i] = JVMTI_ERROR_THREAD_SUSPENDED;
            } else {
                threadSet.add(vmThread);
            }
        }
        suspendOrResumeThreadList(jvmtiEnv, threadSet, true);
        return errors;
    }

    private static int suspendOrResumeThreadList(JVMTI.Env jvmtiEnv, int requestCount, Pointer requestList, Pointer results, boolean isSuspend) {
        if (requestCount < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        Set<VmThread> threadSet = new HashSet<VmThread>();
        for (int i = 0; i < requestCount; i++) {
            try {
                Thread thread = (Thread) requestList.getWord(i).asJniHandle().unhand();
                VmThread vmThread = checkVmThread(thread);
                if (!thread.isAlive()) {
                    results.setInt(i, JVMTI_ERROR_THREAD_NOT_ALIVE);
                } else if (VmOperation.isSuspendRequest(vmThread.tla())) {
                    results.setInt(i, JVMTI_ERROR_THREAD_SUSPENDED);
                } else {
                    threadSet.add(VmThread.fromJava(thread));
                    results.setInt(i, JVMTI_ERROR_NONE);
                }
            } catch (ClassCastException ex) {
                results.setInt(i, JVMTI_ERROR_INVALID_THREAD);
            }
        }
        return suspendOrResumeThreadList(jvmtiEnv, threadSet, true);
    }

    private static int suspendOrResumeThreadList(JVMTI.Env jvmtiEnv, Set<VmThread> threadSet, boolean isSuspend) {
        if (isSuspend) {
            new VmOperation.SuspendThreadSet(threadSet).submit();
            JVMTICode.suspendThreadListNotify(jvmtiEnv, threadSet);
        } else {
            JVMTICode.resumeThreadListNotify(jvmtiEnv, threadSet);
            new VmOperation.ResumeThreadSet(threadSet).submit();
        }
        return JVMTI_ERROR_NONE;

    }

    static int suspendThreadList(JVMTI.Env jvmtiEnv, int requestCount, Pointer requestList, Pointer results) {
        return suspendOrResumeThreadList(jvmtiEnv, requestCount, requestList, results, true);
    }

    static int resumeThread(JVMTI.Env jvmtiEnv, Thread thread) {
        int[] error = resumeThreadList(jvmtiEnv, new Thread[] {thread});
        return error[0];
    }

    static int[] resumeThreadList(JVMTI.Env jvmtiEnv, Thread[] threads) {
        Set<VmThread> threadSet = new HashSet<VmThread>();
        int[] errors = new int[threads.length];
        for (int i = 0; i < threads.length; i++) {
            VmThread vmThread = checkVmThread(threads[i]);
            if (vmThread == null) {
                errors[i] = JVMTI_ERROR_THREAD_NOT_ALIVE;
            } else if (!VmOperation.isSuspendRequest(vmThread.tla())) {
                errors[i] = JVMTI_ERROR_THREAD_NOT_SUSPENDED;
            } else {
                threadSet.add(vmThread);
            }
        }
        suspendOrResumeThreadList(jvmtiEnv, threadSet, false);
        return errors;
    }

    static int resumeThreadList(JVMTI.Env jvmtiEnv, int requestCount, Pointer requestList, Pointer results) {
        return suspendOrResumeThreadList(jvmtiEnv, requestCount, requestList, results, false);
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


    private static class ThreadGroupChildrenUnion extends ModeUnion {
        // native
        Pointer threadGroupsPtr;
        Pointer threadsPtr;
        int nGroups;
        int nThreads;
        // Java
        ThreadGroupChildrenInfo threadGroupChildrenInfo;

        ThreadGroupChildrenUnion(boolean isNative) {
            super(isNative);
        }
    }

    static JJVMTI.ThreadGroupChildrenInfo getThreadGroupChildren(ThreadGroup threadGroup) {
        ThreadGroupChildrenUnion tgu = new ThreadGroupChildrenUnion(false);
        getThreadGroupChildren(threadGroup, tgu);
        return tgu.threadGroupChildrenInfo;
    }

    static int getThreadGroupChildren(ThreadGroup threadGroup, Pointer threadCountPtr, Pointer threadsPtrPtr, Pointer groupCountPtr, Pointer groupsPtrPtr) {
        ThreadGroupChildrenUnion tgu = new ThreadGroupChildrenUnion(true);
        int error = getThreadGroupChildren(threadGroup, tgu);
        if (error == JVMTI_ERROR_NONE) {
            threadCountPtr.setInt(tgu.nThreads);
            groupCountPtr.setInt(tgu.nGroups);
            threadsPtrPtr.setWord(tgu.threadsPtr);
            groupsPtrPtr.setWord(tgu.threadGroupsPtr);

        }
        return error;
    }

    private static int getThreadGroupChildren(ThreadGroup threadGroup, ThreadGroupChildrenUnion tgu) {
        // We reach directly into the ThreadGroup class state to avoid security checks and clumsy iterators.
        ThreadGroupProxy proxy = ThreadGroupProxy.asThreadGroupProxy(threadGroup);
        Thread[] threads = proxy.threads;
        ThreadGroup[] threadGroups = proxy.groups;
        ArrayList<ThreadGroup> activeGroups = null;
        ArrayList<Thread> liveThreads = null;
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

            if (tgu.isNative) {
                tgu.threadsPtr = Memory.allocate(Size.fromInt(nThreads * Word.size()));
                if (tgu.threadsPtr.isZero()) {
                    return JVMTI_ERROR_OUT_OF_MEMORY;
                }
                tgu.threadGroupsPtr = Memory.allocate(Size.fromInt(nGroups * Word.size()));
                if (tgu.threadGroupsPtr.isZero()) {
                    Memory.deallocate(tgu.threadsPtr);
                    return JVMTI_ERROR_OUT_OF_MEMORY;
                }
            } else {
                activeGroups = new ArrayList<ThreadGroup>();
                liveThreads = new ArrayList<Thread>();
            }
            // we recompute the live thread count
            int liveThreadCount = 0;
            for (int i = 0; i < nThreads; i++) {
                if (threads[i].isAlive()) {
                    if (tgu.isNative) {
                        tgu.threadsPtr.setWord(i, JniHandles.createLocalHandle(threads[i]));
                    } else {
                        liveThreads.add(threads[i]);
                    }
                    liveThreadCount++;
                }
            }
            int activeGroupCount = 0;
            for (int i = 0; i < nGroups; i++) {
                if (!threadGroups[i].isDestroyed()) {
                    if (tgu.isNative) {
                        tgu.threadGroupsPtr.setWord(i, JniHandles.createLocalHandle(threadGroups[i]));
                        activeGroupCount++;
                    } else {
                        activeGroups.add(threadGroups[i]);
                    }
                }
            }
            if (!tgu.isNative) {
                Thread[] threadArray = new Thread[liveThreadCount];
                ThreadGroup[] threadGroupArray = new ThreadGroup[activeGroupCount];
                liveThreads.toArray(threadArray);
                activeGroups.toArray(threadGroupArray);
                tgu.threadGroupChildrenInfo = new JJVMTI.ThreadGroupChildrenInfo(threadArray, threadGroupArray);
            } else {
                tgu.nThreads = liveThreadCount;
                tgu.nGroups = activeGroupCount;
            }
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
