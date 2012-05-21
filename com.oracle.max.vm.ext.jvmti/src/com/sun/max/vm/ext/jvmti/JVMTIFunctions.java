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

import static com.sun.max.vm.ext.jvmti.JVMTI.*;
import static com.sun.max.vm.ext.jvmti.JVMTICapabilities.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIEnvNativeStruct.*;
import static com.sun.max.vm.jni.JniFunctions.epilogue;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The transformed form of {@link JVMTIFunctionsSource}.
 * This file is read-only to all but {@link JVMTIFunctionsGenerator}.
 * Do not add code here, add it to the appropriate implementation class.
 */
public class JVMTIFunctions  {
    /* The standard JNI entry prologue uses the fact that the jni env value is a slot in the
     * thread local storage area in order to reset the safepoint latch register
     * on an upcall, by indexing back to the base of the storage area.
     *
     * A jvmti env value is agent-specific and can be used across threads.
     * Therefore it cannot have a stored jni env value since that is thread-specific.
     * So we load the current value of the TLA from the native thread control control block.
     * and we use a special variant of C_FUNCTION that does not do anything with the
     * safepoint latch register, since it isn't valid at this point.
     *
     * One possible problem: if the TLA has been set to triggered or disabled this will be wrong.
     * I believe this could only happen in the case of a callback from such a state which is unlikely.
     * However, the callback typically passes the jni env as well as the jvmti env so,if this is an issue,
     * there should be some way to cache the jni env value on the way down and use it on any nested
     * upcalls.

     * TODO handle the (error) case of an upcall from an unattached thread, which will not
     * have a valid TLA in its native thread control control block.
     */

    @C_FUNCTION
    private static native Pointer currentJniEnv();

    public static final ClassMethodActor currentJniEnv;

    static {
        CriticalNativeMethod cnm = new CriticalNativeMethod(JVMTIFunctions.class, "currentJniEnv");
        currentJniEnv = cnm.classMethodActor;
    }

    @INLINE
    static Pointer prologue(Pointer env) {
        return JniFunctions.prologue(currentJniEnv());
    }

    private static class JVMTIFunctionsLogger extends VMLogger {
        private static final LogOperations[] logOperations = LogOperations.values();

        private JVMTIFunctionsLogger() {
            super("JVMTICalls", logOperations.length, null);
        }

        @Override
        public String operationName(int op) {
            return logOperations[op].name();
        }
    }

    private static JVMTIFunctionsLogger logger = new JVMTIFunctionsLogger();

 // Checkstyle: stop method name check

// START GENERATED CODE

    private static final boolean INSTRUMENTED = false;

    @VM_ENTRY_POINT
    private static native void reserved1();
        // Source: JVMTIFunctionsSource.java:177

    @VM_ENTRY_POINT
    private static int SetEventNotificationMode(Pointer env, int mode, int event_type, JniHandle event_thread) {
        // Source: JVMTIFunctionsSource.java:180
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetEventNotificationMode.ordinal(), env, Address.fromInt(mode), Address.fromInt(event_type), event_thread);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) event_thread.unhand();
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIEvent.setEventNotificationMode(jvmtiEnv, mode, event_type, handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved3();
        // Source: JVMTIFunctionsSource.java:188

    @VM_ENTRY_POINT
    private static int GetAllThreads(Pointer env, Pointer threads_count_ptr, Pointer threads_ptr) {
        // Source: JVMTIFunctionsSource.java:191
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetAllThreads.ordinal(), env, threads_count_ptr, threads_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (threads_count_ptr.isZero() || threads_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getAllThreads(threads_count_ptr, threads_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SuspendThread(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:198
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SuspendThread.ordinal(), env, thread);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_SUSPEND.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.suspendThread(jvmtiEnv, handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ResumeThread(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:206
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ResumeThread.ordinal(), env, thread);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_SUSPEND.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.resumeThread(jvmtiEnv, handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int StopThread(Pointer env, JniHandle thread, JniHandle exception) {
        // Source: JVMTIFunctionsSource.java:214
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.StopThread.ordinal(), env, thread, exception);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int InterruptThread(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:219
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.InterruptThread.ordinal(), env, thread);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_SIGNAL_THREAD.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.interruptThread(handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle thread, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:227
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadInfo.ordinal(), env, thread, info_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (info_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getThreadInfo(handleAsThread, info_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorInfo(Pointer env, JniHandle thread, Pointer owned_monitor_count_ptr, Pointer owned_monitors_ptr) {
        // Source: JVMTIFunctionsSource.java:235
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetOwnedMonitorInfo.ordinal(), env, thread, owned_monitor_count_ptr, owned_monitors_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentContendedMonitor(Pointer env, JniHandle thread, Pointer monitor_ptr) {
        // Source: JVMTIFunctionsSource.java:240
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCurrentContendedMonitor.ordinal(), env, thread, monitor_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RunAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        // Source: JVMTIFunctionsSource.java:245
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RunAgentThread.ordinal(), env, jthread, proc, arg, Address.fromInt(priority));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (proc.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI.runAgentThread(env, jthread, proc, arg, priority);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetTopThreadGroups(Pointer env, Pointer group_count_ptr, Pointer groups_ptr) {
        // Source: JVMTIFunctionsSource.java:252
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetTopThreadGroups.ordinal(), env, group_count_ptr, groups_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (group_count_ptr.isZero() || groups_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getTopThreadGroups(group_count_ptr, groups_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupInfo(Pointer env, JniHandle group, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:259
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadGroupInfo.ordinal(), env, group, info_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            ThreadGroup handleAsThreadGroup;
            try {
                handleAsThreadGroup = (ThreadGroup) group.unhand();
                if (handleAsThreadGroup == null) {
                    return JVMTI_ERROR_INVALID_THREAD_GROUP;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD_GROUP;
            }
            if (info_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getThreadGroupInfo(handleAsThreadGroup, info_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupChildren(Pointer env, JniHandle group, Pointer thread_count_ptr, Pointer threads_ptr, Pointer group_count_ptr, Pointer groups_ptr) {
        // Source: JVMTIFunctionsSource.java:267
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadGroupChildren.ordinal(), env, group, thread_count_ptr, threads_ptr, group_count_ptr, groups_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (thread_count_ptr.isZero() || thread_count_ptr.isZero() || group_count_ptr.isZero() || groups_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ThreadGroup handleAsThreadGroup;
            try {
                handleAsThreadGroup = (ThreadGroup) group.unhand();
                if (handleAsThreadGroup == null) {
                    return JVMTI_ERROR_INVALID_THREAD_GROUP;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD_GROUP;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getThreadGroupChildren(handleAsThreadGroup, thread_count_ptr,
                            threads_ptr, group_count_ptr,  groups_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetFrameCount(Pointer env, JniHandle thread, Pointer count_ptr) {
        // Source: JVMTIFunctionsSource.java:276
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFrameCount.ordinal(), env, thread, count_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getFrameCount(handleAsThread, count_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadState(Pointer env, JniHandle thread, Pointer thread_state_ptr) {
        // Source: JVMTIFunctionsSource.java:284
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadState.ordinal(), env, thread, thread_state_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (thread_state_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getThreadState(handleAsThread, thread_state_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThread(Pointer env, Pointer thread_ptr) {
        // Source: JVMTIFunctionsSource.java:292
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCurrentThread.ordinal(), env, thread_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (thread_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            thread_ptr.setWord(JniHandles.createLocalHandle(VmThread.current().javaThread()));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetFrameLocation(Pointer env, JniHandle thread, int depth, Pointer method_ptr, Pointer location_ptr) {
        // Source: JVMTIFunctionsSource.java:300
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFrameLocation.ordinal(), env, thread, Address.fromInt(depth), method_ptr, location_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (method_ptr.isZero() ||  location_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getFrameLocation(handleAsThread, depth, method_ptr, location_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int NotifyFramePop(Pointer env, JniHandle thread, int depth) {
        // Source: JVMTIFunctionsSource.java:308
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.NotifyFramePop.ordinal(), env, Address.fromInt(depth));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.notifyFramePop(handleAsThread, depth);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalObject(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:316
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalObject.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'L');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalInt(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:325
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalInt.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'I');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalLong(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:334
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalLong.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'J');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:343
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalFloat.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'F');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:352
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalDouble.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'D');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalObject(Pointer env, JniHandle thread, int depth, int slot, JniHandle value) {
        // Source: JVMTIFunctionsSource.java:361
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLocalObject.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), value);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.setLocalObject(handleAsThread, depth, slot, value.unhand());
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalInt(Pointer env, JniHandle thread, int depth, int slot, int value) {
        // Source: JVMTIFunctionsSource.java:369
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLocalInt.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), Address.fromInt(value));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.setLocalInt(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalLong(Pointer env, JniHandle thread, int depth, int slot, long value) {
        // Source: JVMTIFunctionsSource.java:377
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLocalLong.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), Address.fromLong(value));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.setLocalLong(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, float value) {
        // Source: JVMTIFunctionsSource.java:385
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLocalFloat.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), Address.fromInt(Float.floatToRawIntBits(value)));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.setLocalFloat(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, double value) {
        // Source: JVMTIFunctionsSource.java:393
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetLocalDouble.ordinal(), env, thread, Address.fromInt(depth), Address.fromInt(slot), Address.fromLong(Double.doubleToRawLongBits(value)));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_ACCESS_LOCAL_VARIABLES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadFunctions.setLocalDouble(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int CreateRawMonitor(Pointer env, Pointer name, Pointer monitor_ptr) {
        // Source: JVMTIFunctionsSource.java:401
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.CreateRawMonitor.ordinal(), env, name, monitor_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (name.isZero() || monitor_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.create(name, monitor_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int DestroyRawMonitor(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:408
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.DestroyRawMonitor.ordinal(), env, rawMonitor);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.destroy(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorEnter(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:414
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RawMonitorEnter.ordinal(), env, rawMonitor);        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.enter(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorExit(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:420
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RawMonitorExit.ordinal(), env, rawMonitor);        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.exit(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorWait(Pointer env, Word rawMonitor, long millis) {
        // Source: JVMTIFunctionsSource.java:426
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RawMonitorWait.ordinal(), env, rawMonitor, Address.fromLong(millis));        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.wait(rawMonitor, millis);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotify(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:432
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RawMonitorNotify.ordinal(), env, rawMonitor);        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.notify(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotifyAll(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:438
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RawMonitorNotifyAll.ordinal(), env, rawMonitor);        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIRawMonitor.notifyAll(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetBreakpoint(Pointer env, MethodID method, long location) {
        // Source: JVMTIFunctionsSource.java:444
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetBreakpoint.ordinal(), env, method, Address.fromLong(location));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIBreakpoints.setBreakpoint(classMethodActor, method, location);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ClearBreakpoint(Pointer env, MethodID method, long location) {
        // Source: JVMTIFunctionsSource.java:451
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ClearBreakpoint.ordinal(), env, method, Address.fromLong(location));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIBreakpoints.clearBreakpoint(classMethodActor, method, location);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved40();
        // Source: JVMTIFunctionsSource.java:458

    @VM_ENTRY_POINT
    private static int SetFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:461
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetFieldAccessWatch.ordinal(), env, klass, field);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            if (!(CAN_GENERATE_FIELD_ACCESS_EVENTS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIFieldWatch.setAccessWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ClearFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:470
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ClearFieldAccessWatch.ordinal(), env, klass, field);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            if (!(CAN_GENERATE_FIELD_ACCESS_EVENTS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIFieldWatch.clearAccessWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:479
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetFieldModificationWatch.ordinal(), env, klass, field);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            if (!(CAN_GENERATE_FIELD_MODIFICATION_EVENTS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIFieldWatch.setModificationWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ClearFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:488
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ClearFieldModificationWatch.ordinal(), env, klass, field);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            if (!(CAN_GENERATE_FIELD_MODIFICATION_EVENTS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIFieldWatch.clearModificationWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsModifiableClass(Pointer env, JniHandle klass, Pointer is_modifiable_class_ptr) {
        // Source: JVMTIFunctionsSource.java:497
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsModifiableClass.ordinal(), env, klass, is_modifiable_class_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int Allocate(Pointer env, long size, Pointer mem_ptr) {
        // Source: JVMTIFunctionsSource.java:502
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.Allocate.ordinal(), env, Address.fromLong(size), mem_ptr);        }

        try {
    
            if (mem_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (size < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            if (size == 0) {
                mem_ptr.setWord(Word.zero());
            } else {
                Pointer mem = Memory.allocate(Size.fromLong(size));
                if (mem.isZero()) {
                    return JVMTI_ERROR_OUT_OF_MEMORY;
                }
                mem_ptr.setWord(mem);
            }
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int Deallocate(Pointer env, Pointer mem) {
        // Source: JVMTIFunctionsSource.java:521
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.Deallocate.ordinal(), env, mem);        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            Memory.deallocate(mem);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassSignature(Pointer env, JniHandle klass, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JVMTIFunctionsSource.java:528
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassSignature.ordinal(), env, klass, signature_ptr, generic_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getClassSignature(handleAsClass, signature_ptr, generic_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassStatus(Pointer env, JniHandle klass, Pointer status_ptr) {
        // Source: JVMTIFunctionsSource.java:535
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassStatus.ordinal(), env, klass, status_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            if (status_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getClassStatus(handleAsClass, status_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetSourceFileName(Pointer env, JniHandle klass, Pointer source_name_ptr) {
        // Source: JVMTIFunctionsSource.java:543
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetSourceFileName.ordinal(), env, klass, source_name_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_GET_SOURCE_FILE_NAME.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (source_name_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getSourceFileName(handleAsClass, source_name_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassModifiers(Pointer env, JniHandle klass, Pointer modifiers_ptr) {
        // Source: JVMTIFunctionsSource.java:552
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassModifiers.ordinal(), env, klass, modifiers_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (modifiers_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            modifiers_ptr.setInt(ClassActor.fromJava(handleAsClass).accessFlags());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassMethods(Pointer env, JniHandle klass, Pointer method_count_ptr, Pointer methods_ptr) {
        // Source: JVMTIFunctionsSource.java:561
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassMethods.ordinal(), env, klass, method_count_ptr, methods_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (method_count_ptr.isZero() || methods_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getClassMethods(handleAsClass, method_count_ptr, methods_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassFields(Pointer env, JniHandle klass, Pointer field_count_ptr, Pointer fields_ptr) {
        // Source: JVMTIFunctionsSource.java:569
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassFields.ordinal(), env, klass, field_count_ptr, fields_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (field_count_ptr.isZero() || fields_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getClassFields(handleAsClass, field_count_ptr, fields_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetImplementedInterfaces(Pointer env, JniHandle klass, Pointer interface_count_ptr, Pointer interfaces_ptr) {
        // Source: JVMTIFunctionsSource.java:577
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetImplementedInterfaces.ordinal(), env, klass, interface_count_ptr, interfaces_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (interface_count_ptr.isZero() || interfaces_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getImplementedInterfaces(handleAsClass, interface_count_ptr, interfaces_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsInterface(Pointer env, JniHandle klass, Pointer is_interface_ptr) {
        // Source: JVMTIFunctionsSource.java:585
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsInterface.ordinal(), env, klass, is_interface_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            // PHASES LIVE
            if (is_interface_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            boolean is = ClassActor.isInterface(ClassActor.fromJava(handleAsClass).flags());
            is_interface_ptr.setBoolean(is);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsArrayClass(Pointer env, JniHandle klass, Pointer is_array_class_ptr) {
        // Source: JVMTIFunctionsSource.java:595
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsArrayClass.ordinal(), env, klass, is_array_class_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            // PHASES LIVE
            if (is_array_class_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            boolean is = ClassActor.fromJava(handleAsClass).isArrayClass();
            is_array_class_ptr.setBoolean(is);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassLoader(Pointer env, JniHandle klass, Pointer classloader_ptr) {
        // Source: JVMTIFunctionsSource.java:605
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassLoader.ordinal(), env, klass, classloader_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            // PHASES START,LIVE
            if (classloader_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            classloader_ptr.setWord(JniHandles.createLocalHandle(handleAsClass.getClassLoader()));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectHashCode(Pointer env, JniHandle handle, Pointer hash_code_ptr) {
        // Source: JVMTIFunctionsSource.java:614
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectHashCode.ordinal(), env, handle, hash_code_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (hash_code_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            Object object = handle.unhand();
            if (object == null) {
                return JVMTI_ERROR_INVALID_OBJECT;
            }
            hash_code_ptr.setInt(System.identityHashCode(object));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectMonitorUsage(Pointer env, JniHandle object, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:626
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectMonitorUsage.ordinal(), env, object, info_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldName(Pointer env, JniHandle klass, FieldID field, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JVMTIFunctionsSource.java:631
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFieldName.ordinal(), env, klass, field, name_ptr, signature_ptr, generic_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getFieldName(fieldActor, name_ptr, signature_ptr, generic_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldDeclaringClass(Pointer env, JniHandle klass, FieldID field, Pointer declaring_class_ptr) {
        // Source: JVMTIFunctionsSource.java:639
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFieldDeclaringClass.ordinal(), env, klass, field, declaring_class_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (declaring_class_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getFieldDeclaringClass(fieldActor, declaring_class_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldModifiers(Pointer env, JniHandle klass, FieldID field, Pointer modifiers_ptr) {
        // Source: JVMTIFunctionsSource.java:647
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetFieldModifiers.ordinal(), env, klass, field, modifiers_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (modifiers_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            modifiers_ptr.setInt(fieldActor.flags());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsFieldSynthetic(Pointer env, JniHandle klass, FieldID field, Pointer is_synthetic_ptr) {
        // Source: JVMTIFunctionsSource.java:656
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsFieldSynthetic.ordinal(), env, klass, field, is_synthetic_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_GET_SYNTHETIC_ATTRIBUTE.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (is_synthetic_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            FieldActor fieldActor = FieldID.toFieldActor(field);
            if (fieldActor == null) {
                return JVMTI_ERROR_INVALID_FIELDID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            boolean result = (fieldActor.flags() & Actor.ACC_SYNTHETIC) != 0;
            is_synthetic_ptr.setBoolean(result);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodName(Pointer env, MethodID method, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JVMTIFunctionsSource.java:667
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetMethodName.ordinal(), env, method, name_ptr, signature_ptr, generic_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getMethodName(methodActor, name_ptr, signature_ptr, generic_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodDeclaringClass(Pointer env, MethodID method, Pointer declaring_class_ptr) {
        // Source: JVMTIFunctionsSource.java:674
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetMethodDeclaringClass.ordinal(), env, method, declaring_class_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (declaring_class_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getMethodDeclaringClass(methodActor, declaring_class_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodModifiers(Pointer env, MethodID method, Pointer modifiers_ptr) {
        // Source: JVMTIFunctionsSource.java:682
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetMethodModifiers.ordinal(), env, method, modifiers_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (modifiers_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            modifiers_ptr.setInt(methodActor.flags());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved67();
        // Source: JVMTIFunctionsSource.java:691

    @VM_ENTRY_POINT
    private static int GetMaxLocals(Pointer env, MethodID method, Pointer max_ptr) {
        // Source: JVMTIFunctionsSource.java:694
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetMaxLocals.ordinal(), env, method, max_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (max_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getMaxLocals(classMethodActor, max_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetArgumentsSize(Pointer env, MethodID method, Pointer size_ptr) {
        // Source: JVMTIFunctionsSource.java:702
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetArgumentsSize.ordinal(), env, method, size_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (size_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getArgumentsSize(classMethodActor, size_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLineNumberTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // Source: JVMTIFunctionsSource.java:710
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLineNumberTable.ordinal(), env, method, entry_count_ptr, table_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_GET_LINE_NUMBERS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (entry_count_ptr.isZero() || table_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getLineNumberTable(classMethodActor, entry_count_ptr, table_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodLocation(Pointer env, MethodID method, Pointer start_location_ptr, Pointer end_location_ptr) {
        // Source: JVMTIFunctionsSource.java:719
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetMethodLocation.ordinal(), env, method, start_location_ptr, end_location_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (start_location_ptr.isZero() || end_location_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getMethodLocation(classMethodActor, start_location_ptr, end_location_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalVariableTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // Source: JVMTIFunctionsSource.java:727
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalVariableTable.ordinal(), env, method, entry_count_ptr, table_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (entry_count_ptr.isZero() ||  table_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getLocalVariableTable(classMethodActor, entry_count_ptr, table_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefix(Pointer env, Pointer prefix) {
        // Source: JVMTIFunctionsSource.java:735
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetNativeMethodPrefix.ordinal(), env, prefix);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefixes(Pointer env, int prefix_count, Pointer prefixes) {
        // Source: JVMTIFunctionsSource.java:740
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetNativeMethodPrefixes.ordinal(), env, Address.fromInt(prefix_count), prefixes);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetBytecodes(Pointer env, MethodID method, Pointer bytecode_count_ptr, Pointer bytecodes_ptr) {
        // Source: JVMTIFunctionsSource.java:745
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetBytecodes.ordinal(), env, method, bytecode_count_ptr, bytecodes_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_GET_BYTECODES.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (bytecode_count_ptr.isZero() || bytecodes_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getByteCodes(classMethodActor, bytecode_count_ptr, bytecodes_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodNative(Pointer env, MethodID method, Pointer is_native_ptr) {
        // Source: JVMTIFunctionsSource.java:754
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsMethodNative.ordinal(), env, method, is_native_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (is_native_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            is_native_ptr.setBoolean(methodActor.isNative());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodSynthetic(Pointer env, MethodID method, Pointer is_synthetic_ptr) {
        // Source: JVMTIFunctionsSource.java:763
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsMethodSynthetic.ordinal(), env, method, is_synthetic_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_GET_SYNTHETIC_ATTRIBUTE.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (is_synthetic_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            boolean result = (methodActor.flags() & Actor.ACC_SYNTHETIC) != 0;
            is_synthetic_ptr.setBoolean(result);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLoadedClasses(Pointer env, Pointer class_count_ptr, Pointer classes_ptr) {
        // Source: JVMTIFunctionsSource.java:774
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLoadedClasses.ordinal(), env, class_count_ptr, classes_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (class_count_ptr.isZero() || classes_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getLoadedClasses(class_count_ptr, classes_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassLoaderClasses(Pointer env, JniHandle initiatingLoader, Pointer class_count_ptr, Pointer classes_ptr) {
        // Source: JVMTIFunctionsSource.java:781
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassLoaderClasses.ordinal(), env, initiatingLoader, class_count_ptr, classes_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (class_count_ptr.isZero() || classes_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassLoader handleAsClassLoader;
            try {
                handleAsClassLoader = (ClassLoader) initiatingLoader.unhand();
                if (handleAsClassLoader == null) {
                    return JVMTI_ERROR_INVALID_OBJECT;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_OBJECT;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getClassLoaderClasses(handleAsClassLoader, class_count_ptr, classes_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int PopFrame(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:789
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.PopFrame.ordinal(), env, thread);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnObject(Pointer env, JniHandle thread, JniHandle value) {
        // Source: JVMTIFunctionsSource.java:794
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceEarlyReturnObject.ordinal(), env, thread, value);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnInt(Pointer env, JniHandle thread, int value) {
        // Source: JVMTIFunctionsSource.java:799
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceEarlyReturnInt.ordinal(), env, thread, Address.fromInt(value));        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnLong(Pointer env, JniHandle thread, long value) {
        // Source: JVMTIFunctionsSource.java:804
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceEarlyReturnLong.ordinal(), env, thread, Address.fromLong(value));        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnFloat(Pointer env, JniHandle thread, float value) {
        // Source: JVMTIFunctionsSource.java:809
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceEarlyReturnFloat.ordinal(), env, thread, Address.fromInt(Float.floatToRawIntBits(value)));        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnDouble(Pointer env, JniHandle thread, double value) {
        // Source: JVMTIFunctionsSource.java:814
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceEarlyReturnDouble.ordinal(), env, thread, Address.fromLong(Double.doubleToRawLongBits(value)));        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnVoid(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:819
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceEarlyReturnVoid.ordinal(), env, thread);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RedefineClasses(Pointer env, int class_count, Pointer class_definitions) {
        // Source: JVMTIFunctionsSource.java:824
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RedefineClasses.ordinal(), env, Address.fromInt(class_count), class_definitions);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetVersionNumber(Pointer env, Pointer version_ptr) {
        // Source: JVMTIFunctionsSource.java:829
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetVersionNumber.ordinal(), env, version_ptr);        }

        try {
    
            if (version_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            version_ptr.setInt(JVMTI_VERSION);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:837
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCapabilities.ordinal(), env, capabilities_ptr);        }

        try {
    
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            capabilities_ptr.setLong(0, CAPABILITIES.getPtr(env).readLong(0));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetSourceDebugExtension(Pointer env, JniHandle klass, Pointer source_debug_extension_ptr) {
        // Source: JVMTIFunctionsSource.java:845
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetSourceDebugExtension.ordinal(), env, klass, source_debug_extension_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_GET_SOURCE_DEBUG_EXTENSION.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (source_debug_extension_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
                if (handleAsClass == null) {
                    return JVMTI_ERROR_INVALID_CLASS;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getSourceDebugExtension(handleAsClass, source_debug_extension_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodObsolete(Pointer env, MethodID method, Pointer is_obsolete_ptr) {
        // Source: JVMTIFunctionsSource.java:854
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IsMethodObsolete.ordinal(), env, method, is_obsolete_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (is_obsolete_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            ClassMethodActor classMethodActor = JVMTIUtil.toClassMethodActor(method);
            if (classMethodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.isMethodObsolete(classMethodActor, is_obsolete_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SuspendThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // Source: JVMTIFunctionsSource.java:862
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SuspendThreadList.ordinal(), env, Address.fromInt(request_count));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_SUSPEND.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (request_list.isZero() || results.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (request_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.suspendThreadList(jvmtiEnv, request_count, request_list, results);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ResumeThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // Source: JVMTIFunctionsSource.java:874
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ResumeThreadList.ordinal(), env, Address.fromInt(request_count));        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_SUSPEND.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (request_list.isZero() || results.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (request_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.resumeThreadList(jvmtiEnv, request_count, request_list, results);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved94();
        // Source: JVMTIFunctionsSource.java:886

    @VM_ENTRY_POINT
    private static native void reserved95();
        // Source: JVMTIFunctionsSource.java:889

    @VM_ENTRY_POINT
    private static native void reserved96();
        // Source: JVMTIFunctionsSource.java:892

    @VM_ENTRY_POINT
    private static native void reserved97();
        // Source: JVMTIFunctionsSource.java:895

    @VM_ENTRY_POINT
    private static native void reserved98();
        // Source: JVMTIFunctionsSource.java:898

    @VM_ENTRY_POINT
    private static native void reserved99();
        // Source: JVMTIFunctionsSource.java:901

    @VM_ENTRY_POINT
    private static int GetAllStackTraces(Pointer env, int max_frame_count, Pointer stack_info_ptr, Pointer thread_count_ptr) {
        // Source: JVMTIFunctionsSource.java:904
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetAllStackTraces.ordinal(), env, Address.fromInt(max_frame_count), stack_info_ptr, thread_count_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (stack_info_ptr.isZero() || thread_count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (max_frame_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.getAllStackTraces(max_frame_count, stack_info_ptr, thread_count_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadListStackTraces(Pointer env, int thread_count, Pointer thread_list, int max_frame_count, Pointer stack_info_ptr) {
        // Source: JVMTIFunctionsSource.java:914
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadListStackTraces.ordinal(), env, Address.fromInt(thread_count), thread_list, Address.fromInt(max_frame_count), stack_info_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (thread_list.isZero() || stack_info_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (thread_count < 0 || max_frame_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.getThreadListStackTraces(thread_count, thread_list, max_frame_count, stack_info_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data_ptr) {
        // Source: JVMTIFunctionsSource.java:924
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadLocalStorage.ordinal(), env, thread, data_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            if (data_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadLocalStorage.getThreadLocalStorage(handleAsThread, data_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data) {
        // Source: JVMTIFunctionsSource.java:932
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetThreadLocalStorage.ordinal(), env, thread, data);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIThreadLocalStorage.setThreadLocalStorage(handleAsThread, data);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetStackTrace(Pointer env, JniHandle thread, int start_depth, int max_frame_count, Pointer frame_buffer, Pointer count_ptr) {
        // Source: JVMTIFunctionsSource.java:939
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetStackTrace.ordinal(), env, thread, Address.fromInt(start_depth), Address.fromInt(max_frame_count), frame_buffer, count_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (frame_buffer.isZero() || count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Thread handleAsThread;
            try {
                handleAsThread = (Thread) thread.unhand();
                if (handleAsThread == null) {
                    return JVMTI_ERROR_INVALID_THREAD;
                }
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_THREAD;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (max_frame_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.getStackTrace(handleAsThread, start_depth, max_frame_count, frame_buffer, count_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved105();
        // Source: JVMTIFunctionsSource.java:950

    @VM_ENTRY_POINT
    private static int GetTag(Pointer env, JniHandle object, Pointer tag_ptr) {
        // Source: JVMTIFunctionsSource.java:953
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetTag.ordinal(), env, object, tag_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (tag_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return jvmtiEnv.tags.getTag(object.unhand(), tag_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetTag(Pointer env, JniHandle object, long tag) {
        // Source: JVMTIFunctionsSource.java:960
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetTag.ordinal(), env, object, Address.fromLong(tag));        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return jvmtiEnv.tags.setTag(object.unhand(), tag);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int ForceGarbageCollection(Pointer env) {
        // Source: JVMTIFunctionsSource.java:966
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.ForceGarbageCollection.ordinal(), env);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            System.gc();
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverObjectsReachableFromObject(Pointer env, JniHandle object, Address object_reference_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:972
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IterateOverObjectsReachableFromObject.ordinal(), env, object, object_reference_callback, user_data);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverReachableObjects(Pointer env, Address heap_root_callback, Address stack_ref_callback, Address object_ref_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:977
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IterateOverReachableObjects.ordinal(), env, heap_root_callback, stack_ref_callback, object_ref_callback, user_data);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverHeap(Pointer env, int object_filter, Address heap_object_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:982
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IterateOverHeap.ordinal(), env, Address.fromInt(object_filter), heap_object_callback, user_data);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverInstancesOfClass(Pointer env, JniHandle klass, int object_filter, Address heap_object_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:987
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IterateOverInstancesOfClass.ordinal(), env, klass, Address.fromInt(object_filter), heap_object_callback, user_data);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved113();
        // Source: JVMTIFunctionsSource.java:992

    @VM_ENTRY_POINT
    private static int GetObjectsWithTags(Pointer env, int tag_count, Pointer tags, Pointer count_ptr, Pointer object_result_ptr, Pointer tag_result_ptr) {
        // Source: JVMTIFunctionsSource.java:995
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectsWithTags.ordinal(), env, Address.fromInt(tag_count), tags, count_ptr, object_result_ptr, tag_result_ptr);        }

        try {
            if (!(CAN_TAG_OBJECTS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (tags.isZero() || count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI.getEnv(env).tags.getObjectsWithTags(tag_count, tags, count_ptr, object_result_ptr, tag_result_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int FollowReferences(Pointer env, int heap_filter, JniHandle klass, JniHandle initial_object, Pointer callbacks, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:1002
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.FollowReferences.ordinal(), env, Address.fromInt(heap_filter), klass, initial_object, callbacks, user_data);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int IterateThroughHeap(Pointer env, int heap_filter, JniHandle klass, Pointer callbacks, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:1007
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.IterateThroughHeap.ordinal(), env, Address.fromInt(heap_filter), klass, callbacks, user_data);        }

        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (!(CAN_TAG_OBJECTS.get(CAPABILITIES.getPtr(env)))) {
                return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
            }
            if (callbacks.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Class handleAsClass;
            try {
                handleAsClass = (Class) klass.unhand();
            } catch (ClassCastException ex) {
                return JVMTI_ERROR_INVALID_CLASS;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIHeapFunctions.iterateThroughHeap(jvmtiEnv, heap_filter, handleAsClass, callbacks, user_data);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved117();
        // Source: JVMTIFunctionsSource.java:1016

    @VM_ENTRY_POINT
    private static native void reserved118();
        // Source: JVMTIFunctionsSource.java:1019

    @VM_ENTRY_POINT
    private static native void reserved119();
        // Source: JVMTIFunctionsSource.java:1022

    @VM_ENTRY_POINT
    private static int SetJNIFunctionTable(Pointer env, Pointer function_table) {
        // Source: JVMTIFunctionsSource.java:1025
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetJNIFunctionTable.ordinal(), env, function_table);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetJNIFunctionTable(Pointer env, Pointer function_table) {
        // Source: JVMTIFunctionsSource.java:1030
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetJNIFunctionTable.ordinal(), env, function_table);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetEventCallbacks(Pointer env, Pointer callbacks, int size_of_callbacks) {
        // Source: JVMTIFunctionsSource.java:1035
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetEventCallbacks.ordinal(), env, callbacks, Address.fromInt(size_of_callbacks));        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            Pointer envCallbacks = CALLBACKS.get(env).asPointer();
            Memory.copyBytes(callbacks, envCallbacks, Size.fromInt(size_of_callbacks));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GenerateEvents(Pointer env, int event_type) {
        // Source: JVMTIFunctionsSource.java:1043
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GenerateEvents.ordinal(), env, Address.fromInt(event_type));        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetExtensionFunctions(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        // Source: JVMTIFunctionsSource.java:1048
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetExtensionFunctions.ordinal(), env, extension_count_ptr, extensions);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetExtensionEvents(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        // Source: JVMTIFunctionsSource.java:1053
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetExtensionEvents.ordinal(), env, extension_count_ptr, extensions);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetExtensionEventCallback(Pointer env, int extension_event_index, Address callback) {
        // Source: JVMTIFunctionsSource.java:1058
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetExtensionEventCallback.ordinal(), env, Address.fromInt(extension_event_index), callback);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int DisposeEnvironment(Pointer env) {
        // Source: JVMTIFunctionsSource.java:1063
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.DisposeEnvironment.ordinal(), env);        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI.disposeEnv(env);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetErrorName(Pointer env, int error, Pointer name_ptr) {
        // Source: JVMTIFunctionsSource.java:1069
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetErrorName.ordinal(), env, Address.fromInt(error), name_ptr);        }

        try {
    
            if (name_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            if (error < 0 || error > JVMTI_ERROR_MAX) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            byte[] nameBytes = JVMTIError.nameBytes[error];
            Pointer cstring = Memory.allocate(Size.fromInt(nameBytes.length + 1));
            CString.writeBytes(nameBytes, 0, nameBytes.length, cstring, nameBytes.length + 1);
            name_ptr.setWord(0, cstring);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetJLocationFormat(Pointer env, Pointer format_ptr) {
        // Source: JVMTIFunctionsSource.java:1083
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetJLocationFormat.ordinal(), env, format_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperties(Pointer env, Pointer count_ptr, Pointer property_ptr) {
        // Source: JVMTIFunctionsSource.java:1088
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetSystemProperties.ordinal(), env, count_ptr, property_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperty(Pointer env, Pointer property, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:1093
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetSystemProperty.ordinal(), env, property, value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (property.isZero() || value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI.getSystemProperty(env, property, value_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetSystemProperty(Pointer env, Pointer property, Pointer value) {
        // Source: JVMTIFunctionsSource.java:1100
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetSystemProperty.ordinal(), env, property, value);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetPhase(Pointer env, Pointer phase_ptr) {
        // Source: JVMTIFunctionsSource.java:1105
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetPhase.ordinal(), env, phase_ptr);        }

        try {
    
            if (phase_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI.getPhase(phase_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:1112
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCurrentThreadCpuTimerInfo.ordinal(), env, info_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTime(Pointer env, Pointer nanos_ptr) {
        // Source: JVMTIFunctionsSource.java:1117
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetCurrentThreadCpuTime.ordinal(), env, nanos_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:1122
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadCpuTimerInfo.ordinal(), env, info_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTime(Pointer env, JniHandle thread, Pointer nanos_ptr) {
        // Source: JVMTIFunctionsSource.java:1127
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetThreadCpuTime.ordinal(), env, thread, nanos_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:1132
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetTimerInfo.ordinal(), env, info_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetTime(Pointer env, Pointer nanos_ptr) {
        // Source: JVMTIFunctionsSource.java:1137
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetTime.ordinal(), env, nanos_ptr);        }

        try {
    
            if (nanos_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            nanos_ptr.setLong(System.nanoTime());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetPotentialCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:1145
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetPotentialCapabilities.ordinal(), env, capabilities_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            // Currently we don't have any phase-limited or ownership limitations
            JVMTICapabilities.setAll(capabilities_ptr);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved141();
        // Source: JVMTIFunctionsSource.java:1154

    @VM_ENTRY_POINT
    private static int AddCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:1157
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.AddCapabilities.ordinal(), env, capabilities_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTICapabilities.addCapabilities(env, capabilities_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RelinquishCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:1164
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RelinquishCapabilities.ordinal(), env, capabilities_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            Pointer envCaps = CAPABILITIES.getPtr(env);
            for (int i = 0; i < JVMTICapabilities.values.length; i++) {
                JVMTICapabilities cap = JVMTICapabilities.values[i];
                if (cap.get(capabilities_ptr)) {
                   cap.set(envCaps, false);
                }
            }
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetAvailableProcessors(Pointer env, Pointer processor_count_ptr) {
        // Source: JVMTIFunctionsSource.java:1178
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetAvailableProcessors.ordinal(), env, processor_count_ptr);        }

        try {
    
            if (processor_count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            processor_count_ptr.setInt(Runtime.getRuntime().availableProcessors());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassVersionNumbers(Pointer env, JniHandle klass, Pointer minor_version_ptr, Pointer major_version_ptr) {
        // Source: JVMTIFunctionsSource.java:1186
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetClassVersionNumbers.ordinal(), env, klass, minor_version_ptr, major_version_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (minor_version_ptr.isZero() ||  minor_version_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetConstantPool(Pointer env, JniHandle klass, Pointer constant_pool_count_ptr, Pointer constant_pool_byte_count_ptr, Pointer constant_pool_bytes_ptr) {
        // Source: JVMTIFunctionsSource.java:1193
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetConstantPool.ordinal(), env, klass, constant_pool_count_ptr, constant_pool_byte_count_ptr, constant_pool_bytes_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetEnvironmentLocalStorage(Pointer env, Pointer data_ptr) {
        // Source: JVMTIFunctionsSource.java:1198
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetEnvironmentLocalStorage.ordinal(), env, data_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetEnvironmentLocalStorage(Pointer env, Pointer data) {
        // Source: JVMTIFunctionsSource.java:1203
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetEnvironmentLocalStorage.ordinal(), env, data);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int AddToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // Source: JVMTIFunctionsSource.java:1208
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.AddToBootstrapClassLoaderSearch.ordinal(), env, segment);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            // PHASES ONLOAD,LIVE
            if (segment.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIClassFunctions.addToBootstrapClassLoaderSearch(env, segment);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int SetVerboseFlag(Pointer env, int flag, boolean value) {
        // Source: JVMTIFunctionsSource.java:1215
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.SetVerboseFlag.ordinal(), env, Address.fromInt(flag), Address.fromInt(value ? 1 : 0));        }

        try {
    
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            switch (flag) {
                case JVMTI_VERBOSE_GC:
                    VMOptions.verboseOption.verboseGC = value;
                    break;
                case JVMTI_VERBOSE_CLASS:
                    VMOptions.verboseOption.verboseClass = value;
                    break;
                case JVMTI_VERBOSE_JNI:
                    VMOptions.verboseOption.verboseJNI = value;
                    break;
                case JVMTI_VERBOSE_OTHER:
                    VMOptions.verboseOption.verboseCompilation = value;
                    break;
                default:
                    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int AddToSystemClassLoaderSearch(Pointer env, Pointer segment) {
        // Source: JVMTIFunctionsSource.java:1237
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.AddToSystemClassLoaderSearch.ordinal(), env, segment);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int RetransformClasses(Pointer env, int class_count, Pointer classes) {
        // Source: JVMTIFunctionsSource.java:1242
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.RetransformClasses.ordinal(), env, Address.fromInt(class_count), classes);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorStackDepthInfo(Pointer env, JniHandle thread, Pointer monitor_info_count_ptr, Pointer monitor_info_ptr) {
        // Source: JVMTIFunctionsSource.java:1247
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetOwnedMonitorStackDepthInfo.ordinal(), env, thread, monitor_info_count_ptr, monitor_info_ptr);        }

        try {
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectSize(Pointer env, JniHandle object, Pointer size_ptr) {
        // Source: JVMTIFunctionsSource.java:1252
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetObjectSize.ordinal(), env, object, size_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (size_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTIClassFunctions.getObjectSize(object.unhand(), size_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalInstance(Pointer env, JniHandle thread, int depth, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:1259
        Pointer anchor = prologue(env);
        if (logger.enabled()) {
            logger.log(LogOperations.GetLocalInstance.ordinal(), env, thread, Address.fromInt(depth), value_ptr);        }

        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Env jvmtiEnv = JVMTI.getEnv(env);
            if (jvmtiEnv == null) {
                return JVMTI_ERROR_INVALID_ENVIRONMENT;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor);
            // currrently no return logging
        }
    }
    public static enum LogOperations {
        /* 0 */ SetEventNotificationMode,
        /* 1 */ GetAllThreads,
        /* 2 */ SuspendThread,
        /* 3 */ ResumeThread,
        /* 4 */ StopThread,
        /* 5 */ InterruptThread,
        /* 6 */ GetThreadInfo,
        /* 7 */ GetOwnedMonitorInfo,
        /* 8 */ GetCurrentContendedMonitor,
        /* 9 */ RunAgentThread,
        /* 10 */ GetTopThreadGroups,
        /* 11 */ GetThreadGroupInfo,
        /* 12 */ GetThreadGroupChildren,
        /* 13 */ GetFrameCount,
        /* 14 */ GetThreadState,
        /* 15 */ GetCurrentThread,
        /* 16 */ GetFrameLocation,
        /* 17 */ NotifyFramePop,
        /* 18 */ GetLocalObject,
        /* 19 */ GetLocalInt,
        /* 20 */ GetLocalLong,
        /* 21 */ GetLocalFloat,
        /* 22 */ GetLocalDouble,
        /* 23 */ SetLocalObject,
        /* 24 */ SetLocalInt,
        /* 25 */ SetLocalLong,
        /* 26 */ SetLocalFloat,
        /* 27 */ SetLocalDouble,
        /* 28 */ CreateRawMonitor,
        /* 29 */ DestroyRawMonitor,
        /* 30 */ RawMonitorEnter,
        /* 31 */ RawMonitorExit,
        /* 32 */ RawMonitorWait,
        /* 33 */ RawMonitorNotify,
        /* 34 */ RawMonitorNotifyAll,
        /* 35 */ SetBreakpoint,
        /* 36 */ ClearBreakpoint,
        /* 37 */ SetFieldAccessWatch,
        /* 38 */ ClearFieldAccessWatch,
        /* 39 */ SetFieldModificationWatch,
        /* 40 */ ClearFieldModificationWatch,
        /* 41 */ IsModifiableClass,
        /* 42 */ Allocate,
        /* 43 */ Deallocate,
        /* 44 */ GetClassSignature,
        /* 45 */ GetClassStatus,
        /* 46 */ GetSourceFileName,
        /* 47 */ GetClassModifiers,
        /* 48 */ GetClassMethods,
        /* 49 */ GetClassFields,
        /* 50 */ GetImplementedInterfaces,
        /* 51 */ IsInterface,
        /* 52 */ IsArrayClass,
        /* 53 */ GetClassLoader,
        /* 54 */ GetObjectHashCode,
        /* 55 */ GetObjectMonitorUsage,
        /* 56 */ GetFieldName,
        /* 57 */ GetFieldDeclaringClass,
        /* 58 */ GetFieldModifiers,
        /* 59 */ IsFieldSynthetic,
        /* 60 */ GetMethodName,
        /* 61 */ GetMethodDeclaringClass,
        /* 62 */ GetMethodModifiers,
        /* 63 */ GetMaxLocals,
        /* 64 */ GetArgumentsSize,
        /* 65 */ GetLineNumberTable,
        /* 66 */ GetMethodLocation,
        /* 67 */ GetLocalVariableTable,
        /* 68 */ SetNativeMethodPrefix,
        /* 69 */ SetNativeMethodPrefixes,
        /* 70 */ GetBytecodes,
        /* 71 */ IsMethodNative,
        /* 72 */ IsMethodSynthetic,
        /* 73 */ GetLoadedClasses,
        /* 74 */ GetClassLoaderClasses,
        /* 75 */ PopFrame,
        /* 76 */ ForceEarlyReturnObject,
        /* 77 */ ForceEarlyReturnInt,
        /* 78 */ ForceEarlyReturnLong,
        /* 79 */ ForceEarlyReturnFloat,
        /* 80 */ ForceEarlyReturnDouble,
        /* 81 */ ForceEarlyReturnVoid,
        /* 82 */ RedefineClasses,
        /* 83 */ GetVersionNumber,
        /* 84 */ GetCapabilities,
        /* 85 */ GetSourceDebugExtension,
        /* 86 */ IsMethodObsolete,
        /* 87 */ SuspendThreadList,
        /* 88 */ ResumeThreadList,
        /* 89 */ GetAllStackTraces,
        /* 90 */ GetThreadListStackTraces,
        /* 91 */ GetThreadLocalStorage,
        /* 92 */ SetThreadLocalStorage,
        /* 93 */ GetStackTrace,
        /* 94 */ GetTag,
        /* 95 */ SetTag,
        /* 96 */ ForceGarbageCollection,
        /* 97 */ IterateOverObjectsReachableFromObject,
        /* 98 */ IterateOverReachableObjects,
        /* 99 */ IterateOverHeap,
        /* 100 */ IterateOverInstancesOfClass,
        /* 101 */ GetObjectsWithTags,
        /* 102 */ FollowReferences,
        /* 103 */ IterateThroughHeap,
        /* 104 */ SetJNIFunctionTable,
        /* 105 */ GetJNIFunctionTable,
        /* 106 */ SetEventCallbacks,
        /* 107 */ GenerateEvents,
        /* 108 */ GetExtensionFunctions,
        /* 109 */ GetExtensionEvents,
        /* 110 */ SetExtensionEventCallback,
        /* 111 */ DisposeEnvironment,
        /* 112 */ GetErrorName,
        /* 113 */ GetJLocationFormat,
        /* 114 */ GetSystemProperties,
        /* 115 */ GetSystemProperty,
        /* 116 */ SetSystemProperty,
        /* 117 */ GetPhase,
        /* 118 */ GetCurrentThreadCpuTimerInfo,
        /* 119 */ GetCurrentThreadCpuTime,
        /* 120 */ GetThreadCpuTimerInfo,
        /* 121 */ GetThreadCpuTime,
        /* 122 */ GetTimerInfo,
        /* 123 */ GetTime,
        /* 124 */ GetPotentialCapabilities,
        /* 125 */ AddCapabilities,
        /* 126 */ RelinquishCapabilities,
        /* 127 */ GetAvailableProcessors,
        /* 128 */ GetClassVersionNumbers,
        /* 129 */ GetConstantPool,
        /* 130 */ GetEnvironmentLocalStorage,
        /* 131 */ SetEnvironmentLocalStorage,
        /* 132 */ AddToBootstrapClassLoaderSearch,
        /* 133 */ SetVerboseFlag,
        /* 134 */ AddToSystemClassLoaderSearch,
        /* 135 */ RetransformClasses,
        /* 136 */ GetOwnedMonitorStackDepthInfo,
        /* 137 */ GetObjectSize,
        /* 138 */ GetLocalInstance;

    }
// END GENERATED CODE
}
