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

import static com.sun.max.vm.jvmti.JVMTI.*;
import static com.sun.max.vm.jni.JniFunctions.epilogue;
import static com.sun.max.vm.jvmti.JVMTICapabilities.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.jvmti.JVMTIEnvNativeStruct.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;

/**
 * The transformed form of {@link JVMTIFunctionsSource}.
 * This file is read-only to all but {@link JVMTIFunctionsGenerator}.
 * Do not add code here, add it to the appropriate implementation class.
 */
public class JVMTIFunctions  {
 // Checkstyle: stop method name check

// START GENERATED CODE

    private static final boolean INSTRUMENTED = false;

    @VM_ENTRY_POINT
    private static native void reserved1();
        // Source: JVMTIFunctionsSource.java:85

    @VM_ENTRY_POINT
    private static int SetEventNotificationMode(Pointer env, int mode, int event_type, JniHandle event_thread) {
        // Source: JVMTIFunctionsSource.java:88
        Pointer anchor = prologue(env, "SetEventNotificationMode");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            return JVMTIEvent.setEventNotificationMode(env, mode, event_type, event_thread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetEventNotificationMode");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved3();
        // Source: JVMTIFunctionsSource.java:94

    @VM_ENTRY_POINT
    private static int GetAllThreads(Pointer env, Pointer threads_count_ptr, Pointer threads_ptr) {
        // Source: JVMTIFunctionsSource.java:97
        Pointer anchor = prologue(env, "GetAllThreads");
        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (threads_count_ptr.isZero() || threads_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIThreadFunctions.getAllThreads(threads_count_ptr, threads_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetAllThreads");
        }
    }

    @VM_ENTRY_POINT
    private static int SuspendThread(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:104
        Pointer anchor = prologue(env, "SuspendThread");
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
            return JVMTIThreadFunctions.suspendThread(handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SuspendThread");
        }
    }

    @VM_ENTRY_POINT
    private static int ResumeThread(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:112
        Pointer anchor = prologue(env, "ResumeThread");
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
            return JVMTIThreadFunctions.resumeThread(handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ResumeThread");
        }
    }

    @VM_ENTRY_POINT
    private static int StopThread(Pointer env, JniHandle thread, JniHandle exception) {
        // Source: JVMTIFunctionsSource.java:120
        Pointer anchor = prologue(env, "StopThread");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "StopThread");
        }
    }

    @VM_ENTRY_POINT
    private static int InterruptThread(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:125
        Pointer anchor = prologue(env, "InterruptThread");
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
            return JVMTIThreadFunctions.interruptThread(handleAsThread);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "InterruptThread");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle thread, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:133
        Pointer anchor = prologue(env, "GetThreadInfo");
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
            return JVMTIThreadFunctions.getThreadInfo(handleAsThread, info_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorInfo(Pointer env, JniHandle thread, Pointer owned_monitor_count_ptr, Pointer owned_monitors_ptr) {
        // Source: JVMTIFunctionsSource.java:141
        Pointer anchor = prologue(env, "GetOwnedMonitorInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetOwnedMonitorInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentContendedMonitor(Pointer env, JniHandle thread, Pointer monitor_ptr) {
        // Source: JVMTIFunctionsSource.java:146
        Pointer anchor = prologue(env, "GetCurrentContendedMonitor");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetCurrentContendedMonitor");
        }
    }

    @VM_ENTRY_POINT
    private static int RunAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        // Source: JVMTIFunctionsSource.java:151
        Pointer anchor = prologue(env, "RunAgentThread");
        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (proc.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTI.runAgentThread(env, jthread, proc, arg, priority);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RunAgentThread");
        }
    }

    @VM_ENTRY_POINT
    private static int GetTopThreadGroups(Pointer env, Pointer group_count_ptr, Pointer groups_ptr) {
        // Source: JVMTIFunctionsSource.java:158
        Pointer anchor = prologue(env, "GetTopThreadGroups");
        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (group_count_ptr.isZero() || groups_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIThreadFunctions.getTopThreadGroups(group_count_ptr, groups_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetTopThreadGroups");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupInfo(Pointer env, JniHandle group, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:165
        Pointer anchor = prologue(env, "GetThreadGroupInfo");
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
            return JVMTIThreadFunctions.getThreadGroupInfo(handleAsThreadGroup, info_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadGroupInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupChildren(Pointer env, JniHandle group, Pointer thread_count_ptr, Pointer threads_ptr, Pointer group_count_ptr, Pointer groups_ptr) {
        // Source: JVMTIFunctionsSource.java:173
        Pointer anchor = prologue(env, "GetThreadGroupChildren");
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
            return JVMTIThreadFunctions.getThreadGroupChildren(handleAsThreadGroup, thread_count_ptr,
                            threads_ptr, group_count_ptr,  groups_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadGroupChildren");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFrameCount(Pointer env, JniHandle thread, Pointer count_ptr) {
        // Source: JVMTIFunctionsSource.java:182
        Pointer anchor = prologue(env, "GetFrameCount");
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
            return JVMTIThreadFunctions.getFrameCount(handleAsThread, count_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetFrameCount");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadState(Pointer env, JniHandle thread, Pointer thread_state_ptr) {
        // Source: JVMTIFunctionsSource.java:190
        Pointer anchor = prologue(env, "GetThreadState");
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
            return JVMTIThreadFunctions.getThreadState(handleAsThread, thread_state_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadState");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThread(Pointer env, Pointer thread_ptr) {
        // Source: JVMTIFunctionsSource.java:198
        Pointer anchor = prologue(env, "GetCurrentThread");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (thread_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            thread_ptr.setWord(JniHandles.createLocalHandle(VmThread.current().javaThread()));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetCurrentThread");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFrameLocation(Pointer env, JniHandle thread, int depth, Pointer method_ptr, Pointer location_ptr) {
        // Source: JVMTIFunctionsSource.java:206
        Pointer anchor = prologue(env, "GetFrameLocation");
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
            return JVMTIThreadFunctions.getFrameLocation(handleAsThread, depth, method_ptr, location_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetFrameLocation");
        }
    }

    @VM_ENTRY_POINT
    private static int NotifyFramePop(Pointer env, JniHandle thread, int depth) {
        // Source: JVMTIFunctionsSource.java:214
        Pointer anchor = prologue(env, "NotifyFramePop");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "NotifyFramePop");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalObject(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:219
        Pointer anchor = prologue(env, "GetLocalObject");
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
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'L');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLocalObject");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalInt(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:228
        Pointer anchor = prologue(env, "GetLocalInt");
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
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'I');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLocalInt");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalLong(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:237
        Pointer anchor = prologue(env, "GetLocalLong");
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
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'J');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLocalLong");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:246
        Pointer anchor = prologue(env, "GetLocalFloat");
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
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'F');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLocalFloat");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:255
        Pointer anchor = prologue(env, "GetLocalDouble");
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
            return JVMTIThreadFunctions.getLocalValue(handleAsThread, depth, slot, value_ptr, 'D');
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLocalDouble");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalObject(Pointer env, JniHandle thread, int depth, int slot, JniHandle value) {
        // Source: JVMTIFunctionsSource.java:264
        Pointer anchor = prologue(env, "SetLocalObject");
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
            return JVMTIThreadFunctions.setLocalObject(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetLocalObject");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalInt(Pointer env, JniHandle thread, int depth, int slot, int value) {
        // Source: JVMTIFunctionsSource.java:272
        Pointer anchor = prologue(env, "SetLocalInt");
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
            return JVMTIThreadFunctions.setLocalInt(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetLocalInt");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalLong(Pointer env, JniHandle thread, int depth, int slot, long value) {
        // Source: JVMTIFunctionsSource.java:280
        Pointer anchor = prologue(env, "SetLocalLong");
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
            return JVMTIThreadFunctions.setLocalLong(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetLocalLong");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, float value) {
        // Source: JVMTIFunctionsSource.java:288
        Pointer anchor = prologue(env, "SetLocalFloat");
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
            return JVMTIThreadFunctions.setLocalFloat(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetLocalFloat");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, double value) {
        // Source: JVMTIFunctionsSource.java:296
        Pointer anchor = prologue(env, "SetLocalDouble");
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
            return JVMTIThreadFunctions.setLocalDouble(handleAsThread, depth, slot, value);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetLocalDouble");
        }
    }

    @VM_ENTRY_POINT
    private static int CreateRawMonitor(Pointer env, Pointer name, Pointer monitor_ptr) {
        // Source: JVMTIFunctionsSource.java:304
        Pointer anchor = prologue(env, "CreateRawMonitor");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (name.isZero() || monitor_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIRawMonitor.create(name, monitor_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "CreateRawMonitor");
        }
    }

    @VM_ENTRY_POINT
    private static int DestroyRawMonitor(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:311
        Pointer anchor = prologue(env, "DestroyRawMonitor");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            return JVMTIRawMonitor.destroy(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "DestroyRawMonitor");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorEnter(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:317
        Pointer anchor = prologue(env, "RawMonitorEnter");
        try {
            // PHASES: ANY
            return JVMTIRawMonitor.enter(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RawMonitorEnter");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorExit(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:323
        Pointer anchor = prologue(env, "RawMonitorExit");
        try {
            // PHASES: ANY
            return JVMTIRawMonitor.exit(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RawMonitorExit");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorWait(Pointer env, Word rawMonitor, long millis) {
        // Source: JVMTIFunctionsSource.java:329
        Pointer anchor = prologue(env, "RawMonitorWait");
        try {
            // PHASES: ANY
            return JVMTIRawMonitor.wait(rawMonitor, millis);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RawMonitorWait");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotify(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:335
        Pointer anchor = prologue(env, "RawMonitorNotify");
        try {
            // PHASES: ANY
            return JVMTIRawMonitor.notify(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RawMonitorNotify");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotifyAll(Pointer env, Word rawMonitor) {
        // Source: JVMTIFunctionsSource.java:341
        Pointer anchor = prologue(env, "RawMonitorNotifyAll");
        try {
            // PHASES: ANY
            return JVMTIRawMonitor.notifyAll(rawMonitor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RawMonitorNotifyAll");
        }
    }

    @VM_ENTRY_POINT
    private static int SetBreakpoint(Pointer env, MethodID method, long location) {
        // Source: JVMTIFunctionsSource.java:347
        Pointer anchor = prologue(env, "SetBreakpoint");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetBreakpoint");
        }
    }

    @VM_ENTRY_POINT
    private static int ClearBreakpoint(Pointer env, MethodID method, long location) {
        // Source: JVMTIFunctionsSource.java:352
        Pointer anchor = prologue(env, "ClearBreakpoint");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ClearBreakpoint");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved40();
        // Source: JVMTIFunctionsSource.java:357

    @VM_ENTRY_POINT
    private static int SetFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:360
        Pointer anchor = prologue(env, "SetFieldAccessWatch");
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
            return JVMTIFieldWatch.setAccessWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetFieldAccessWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int ClearFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:369
        Pointer anchor = prologue(env, "ClearFieldAccessWatch");
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
            return JVMTIFieldWatch.clearAccessWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ClearFieldAccessWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int SetFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:378
        Pointer anchor = prologue(env, "SetFieldModificationWatch");
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
            return JVMTIFieldWatch.setModificationWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetFieldModificationWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int ClearFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JVMTIFunctionsSource.java:387
        Pointer anchor = prologue(env, "ClearFieldModificationWatch");
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
            return JVMTIFieldWatch.clearModificationWatch(handleAsClass, fieldActor);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ClearFieldModificationWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int IsModifiableClass(Pointer env, JniHandle klass, Pointer is_modifiable_class_ptr) {
        // Source: JVMTIFunctionsSource.java:396
        Pointer anchor = prologue(env, "IsModifiableClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IsModifiableClass");
        }
    }

    @VM_ENTRY_POINT
    private static int Allocate(Pointer env, long size, Pointer mem_ptr) {
        // Source: JVMTIFunctionsSource.java:401
        Pointer anchor = prologue(env, "Allocate");
        try {
            // PHASES: ANY
            if (mem_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
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
            epilogue(anchor, "Allocate");
        }
    }

    @VM_ENTRY_POINT
    private static int Deallocate(Pointer env, Pointer mem) {
        // Source: JVMTIFunctionsSource.java:420
        Pointer anchor = prologue(env, "Deallocate");
        try {
            // PHASES: ANY
            Memory.deallocate(mem);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "Deallocate");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassSignature(Pointer env, JniHandle klass, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JVMTIFunctionsSource.java:427
        Pointer anchor = prologue(env, "GetClassSignature");
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
            if (signature_ptr.isZero() || generic_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIClassFunctions.getClassSignature(handleAsClass, signature_ptr, generic_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassSignature");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassStatus(Pointer env, JniHandle klass, Pointer status_ptr) {
        // Source: JVMTIFunctionsSource.java:435
        Pointer anchor = prologue(env, "GetClassStatus");
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
            return JVMTIClassFunctions.getClassStatus(handleAsClass, status_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassStatus");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSourceFileName(Pointer env, JniHandle klass, Pointer source_name_ptr) {
        // Source: JVMTIFunctionsSource.java:443
        Pointer anchor = prologue(env, "GetSourceFileName");
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
            return JVMTIClassFunctions.getSourceFileName(handleAsClass, source_name_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetSourceFileName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassModifiers(Pointer env, JniHandle klass, Pointer modifiers_ptr) {
        // Source: JVMTIFunctionsSource.java:452
        Pointer anchor = prologue(env, "GetClassModifiers");
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
            modifiers_ptr.setInt(ClassActor.fromJava(handleAsClass).accessFlags());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassModifiers");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassMethods(Pointer env, JniHandle klass, Pointer method_count_ptr, Pointer methods_ptr) {
        // Source: JVMTIFunctionsSource.java:461
        Pointer anchor = prologue(env, "GetClassMethods");
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
            return JVMTIClassFunctions.getClassMethods(handleAsClass, method_count_ptr, methods_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassMethods");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassFields(Pointer env, JniHandle klass, Pointer field_count_ptr, Pointer fields_ptr) {
        // Source: JVMTIFunctionsSource.java:469
        Pointer anchor = prologue(env, "GetClassFields");
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
            return JVMTIClassFunctions.getClassFields(handleAsClass, field_count_ptr, fields_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassFields");
        }
    }

    @VM_ENTRY_POINT
    private static int GetImplementedInterfaces(Pointer env, JniHandle klass, Pointer interface_count_ptr, Pointer interfaces_ptr) {
        // Source: JVMTIFunctionsSource.java:477
        Pointer anchor = prologue(env, "GetImplementedInterfaces");
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
            return JVMTIClassFunctions.getImplementedInterfaces(handleAsClass, interface_count_ptr, interfaces_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetImplementedInterfaces");
        }
    }

    @VM_ENTRY_POINT
    private static int IsInterface(Pointer env, JniHandle klass, Pointer is_interface_ptr) {
        // Source: JVMTIFunctionsSource.java:485
        Pointer anchor = prologue(env, "IsInterface");
        try {
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
            epilogue(anchor, "IsInterface");
        }
    }

    @VM_ENTRY_POINT
    private static int IsArrayClass(Pointer env, JniHandle klass, Pointer is_array_class_ptr) {
        // Source: JVMTIFunctionsSource.java:495
        Pointer anchor = prologue(env, "IsArrayClass");
        try {
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
            epilogue(anchor, "IsArrayClass");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassLoader(Pointer env, JniHandle klass, Pointer classloader_ptr) {
        // Source: JVMTIFunctionsSource.java:505
        Pointer anchor = prologue(env, "GetClassLoader");
        try {
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
            epilogue(anchor, "GetClassLoader");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectHashCode(Pointer env, JniHandle object, Pointer hash_code_ptr) {
        // Source: JVMTIFunctionsSource.java:514
        Pointer anchor = prologue(env, "GetObjectHashCode");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetObjectHashCode");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectMonitorUsage(Pointer env, JniHandle object, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:519
        Pointer anchor = prologue(env, "GetObjectMonitorUsage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetObjectMonitorUsage");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldName(Pointer env, JniHandle klass, FieldID field, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JVMTIFunctionsSource.java:524
        Pointer anchor = prologue(env, "GetFieldName");
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
            return JVMTIClassFunctions.getFieldName(fieldActor, name_ptr, signature_ptr, generic_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetFieldName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldDeclaringClass(Pointer env, JniHandle klass, FieldID field, Pointer declaring_class_ptr) {
        // Source: JVMTIFunctionsSource.java:532
        Pointer anchor = prologue(env, "GetFieldDeclaringClass");
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
            return JVMTIClassFunctions.getFieldDeclaringClass(fieldActor, declaring_class_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetFieldDeclaringClass");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldModifiers(Pointer env, JniHandle klass, FieldID field, Pointer modifiers_ptr) {
        // Source: JVMTIFunctionsSource.java:540
        Pointer anchor = prologue(env, "GetFieldModifiers");
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
            modifiers_ptr.setInt(fieldActor.flags());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetFieldModifiers");
        }
    }

    @VM_ENTRY_POINT
    private static int IsFieldSynthetic(Pointer env, JniHandle klass, FieldID field, Pointer is_synthetic_ptr) {
        // Source: JVMTIFunctionsSource.java:549
        Pointer anchor = prologue(env, "IsFieldSynthetic");
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
            boolean result = (fieldActor.flags() & Actor.ACC_SYNTHETIC) != 0;
            is_synthetic_ptr.setBoolean(result);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IsFieldSynthetic");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodName(Pointer env, MethodID method, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JVMTIFunctionsSource.java:560
        Pointer anchor = prologue(env, "GetMethodName");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            return JVMTIClassFunctions.getMethodName(methodActor, name_ptr, signature_ptr, generic_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetMethodName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodDeclaringClass(Pointer env, MethodID method, Pointer declaring_class_ptr) {
        // Source: JVMTIFunctionsSource.java:567
        Pointer anchor = prologue(env, "GetMethodDeclaringClass");
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
            return JVMTIClassFunctions.getMethodDeclaringClass(methodActor, declaring_class_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetMethodDeclaringClass");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodModifiers(Pointer env, MethodID method, Pointer modifiers_ptr) {
        // Source: JVMTIFunctionsSource.java:575
        Pointer anchor = prologue(env, "GetMethodModifiers");
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
            modifiers_ptr.setInt(methodActor.flags());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetMethodModifiers");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved67();
        // Source: JVMTIFunctionsSource.java:584

    @VM_ENTRY_POINT
    private static int GetMaxLocals(Pointer env, MethodID method, Pointer max_ptr) {
        // Source: JVMTIFunctionsSource.java:587
        Pointer anchor = prologue(env, "GetMaxLocals");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (max_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            return JVMTIClassFunctions.getMaxLocals(methodActor, max_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetMaxLocals");
        }
    }

    @VM_ENTRY_POINT
    private static int GetArgumentsSize(Pointer env, MethodID method, Pointer size_ptr) {
        // Source: JVMTIFunctionsSource.java:595
        Pointer anchor = prologue(env, "GetArgumentsSize");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (size_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            return JVMTIClassFunctions.getArgumentsSize(methodActor, size_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetArgumentsSize");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLineNumberTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // Source: JVMTIFunctionsSource.java:603
        Pointer anchor = prologue(env, "GetLineNumberTable");
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
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            return JVMTIClassFunctions.getLineNumberTable(methodActor, entry_count_ptr, table_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLineNumberTable");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodLocation(Pointer env, MethodID method, Pointer start_location_ptr, Pointer end_location_ptr) {
        // Source: JVMTIFunctionsSource.java:612
        Pointer anchor = prologue(env, "GetMethodLocation");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (start_location_ptr.isZero() || end_location_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            return JVMTIClassFunctions.getMethodLocation(methodActor, start_location_ptr, end_location_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetMethodLocation");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalVariableTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // Source: JVMTIFunctionsSource.java:620
        Pointer anchor = prologue(env, "GetLocalVariableTable");
        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (entry_count_ptr.isZero() ||  table_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            MethodActor methodActor = MethodID.toMethodActor(method);
            if (methodActor == null) {
                return JVMTI_ERROR_INVALID_METHODID;
            }
            return JVMTIClassFunctions.getLocalVariableTable(methodActor, entry_count_ptr, table_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLocalVariableTable");
        }
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefix(Pointer env, Pointer prefix) {
        // Source: JVMTIFunctionsSource.java:628
        Pointer anchor = prologue(env, "SetNativeMethodPrefix");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetNativeMethodPrefix");
        }
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefixes(Pointer env, int prefix_count, Pointer prefixes) {
        // Source: JVMTIFunctionsSource.java:633
        Pointer anchor = prologue(env, "SetNativeMethodPrefixes");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetNativeMethodPrefixes");
        }
    }

    @VM_ENTRY_POINT
    private static int GetBytecodes(Pointer env, MethodID method, Pointer bytecode_count_ptr, Pointer bytecodes_ptr) {
        // Source: JVMTIFunctionsSource.java:638
        Pointer anchor = prologue(env, "GetBytecodes");
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
            return JVMTIClassFunctions.getByteCodes(method, bytecode_count_ptr, bytecodes_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetBytecodes");
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodNative(Pointer env, MethodID method, Pointer is_native_ptr) {
        // Source: JVMTIFunctionsSource.java:646
        Pointer anchor = prologue(env, "IsMethodNative");
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
            is_native_ptr.setBoolean(methodActor.isNative());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IsMethodNative");
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodSynthetic(Pointer env, MethodID method, Pointer is_synthetic_ptr) {
        // Source: JVMTIFunctionsSource.java:655
        Pointer anchor = prologue(env, "IsMethodSynthetic");
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
            boolean result = (methodActor.flags() & Actor.ACC_SYNTHETIC) != 0;
            is_synthetic_ptr.setBoolean(result);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IsMethodSynthetic");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLoadedClasses(Pointer env, Pointer class_count_ptr, Pointer classes_ptr) {
        // Source: JVMTIFunctionsSource.java:666
        Pointer anchor = prologue(env, "GetLoadedClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetLoadedClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassLoaderClasses(Pointer env, JniHandle initiatingLoader, Pointer class_count_ptr, Pointer classes_ptr) {
        // Source: JVMTIFunctionsSource.java:671
        Pointer anchor = prologue(env, "GetClassLoaderClasses");
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
            return JVMTIClassFunctions.getClassLoaderClasses(handleAsClassLoader, class_count_ptr, classes_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassLoaderClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int PopFrame(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:679
        Pointer anchor = prologue(env, "PopFrame");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "PopFrame");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnObject(Pointer env, JniHandle thread, JniHandle value) {
        // Source: JVMTIFunctionsSource.java:684
        Pointer anchor = prologue(env, "ForceEarlyReturnObject");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceEarlyReturnObject");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnInt(Pointer env, JniHandle thread, int value) {
        // Source: JVMTIFunctionsSource.java:689
        Pointer anchor = prologue(env, "ForceEarlyReturnInt");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceEarlyReturnInt");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnLong(Pointer env, JniHandle thread, long value) {
        // Source: JVMTIFunctionsSource.java:694
        Pointer anchor = prologue(env, "ForceEarlyReturnLong");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceEarlyReturnLong");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnFloat(Pointer env, JniHandle thread, float value) {
        // Source: JVMTIFunctionsSource.java:699
        Pointer anchor = prologue(env, "ForceEarlyReturnFloat");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceEarlyReturnFloat");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnDouble(Pointer env, JniHandle thread, double value) {
        // Source: JVMTIFunctionsSource.java:704
        Pointer anchor = prologue(env, "ForceEarlyReturnDouble");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceEarlyReturnDouble");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnVoid(Pointer env, JniHandle thread) {
        // Source: JVMTIFunctionsSource.java:709
        Pointer anchor = prologue(env, "ForceEarlyReturnVoid");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceEarlyReturnVoid");
        }
    }

    @VM_ENTRY_POINT
    private static int RedefineClasses(Pointer env, int class_count, Pointer class_definitions) {
        // Source: JVMTIFunctionsSource.java:714
        Pointer anchor = prologue(env, "RedefineClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RedefineClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int GetVersionNumber(Pointer env, Pointer version_ptr) {
        // Source: JVMTIFunctionsSource.java:719
        Pointer anchor = prologue(env, "GetVersionNumber");
        try {
            // PHASES: ANY
            if (version_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            version_ptr.setInt(JVMTI_VERSION);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetVersionNumber");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:727
        Pointer anchor = prologue(env, "GetCapabilities");
        try {
            // PHASES: ANY
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            capabilities_ptr.setLong(0, CAPABILITIES.getPtr(env).readLong(0));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSourceDebugExtension(Pointer env, JniHandle klass, Pointer source_debug_extension_ptr) {
        // Source: JVMTIFunctionsSource.java:735
        Pointer anchor = prologue(env, "GetSourceDebugExtension");
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
            return JVMTIClassFunctions.getSourceDebugExtension(handleAsClass, source_debug_extension_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetSourceDebugExtension");
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodObsolete(Pointer env, MethodID method, Pointer is_obsolete_ptr) {
        // Source: JVMTIFunctionsSource.java:744
        Pointer anchor = prologue(env, "IsMethodObsolete");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IsMethodObsolete");
        }
    }

    @VM_ENTRY_POINT
    private static int SuspendThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // Source: JVMTIFunctionsSource.java:749
        Pointer anchor = prologue(env, "SuspendThreadList");
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
            if (request_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.suspendThreadList(request_count, request_list, results);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SuspendThreadList");
        }
    }

    @VM_ENTRY_POINT
    private static int ResumeThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // Source: JVMTIFunctionsSource.java:760
        Pointer anchor = prologue(env, "ResumeThreadList");
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
            if (request_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.resumeThreadList(request_count, request_list, results);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ResumeThreadList");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved94();
        // Source: JVMTIFunctionsSource.java:771

    @VM_ENTRY_POINT
    private static native void reserved95();
        // Source: JVMTIFunctionsSource.java:774

    @VM_ENTRY_POINT
    private static native void reserved96();
        // Source: JVMTIFunctionsSource.java:777

    @VM_ENTRY_POINT
    private static native void reserved97();
        // Source: JVMTIFunctionsSource.java:780

    @VM_ENTRY_POINT
    private static native void reserved98();
        // Source: JVMTIFunctionsSource.java:783

    @VM_ENTRY_POINT
    private static native void reserved99();
        // Source: JVMTIFunctionsSource.java:786

    @VM_ENTRY_POINT
    private static int GetAllStackTraces(Pointer env, int max_frame_count, Pointer stack_info_ptr, Pointer thread_count_ptr) {
        // Source: JVMTIFunctionsSource.java:789
        Pointer anchor = prologue(env, "GetAllStackTraces");
        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (stack_info_ptr.isZero() || thread_count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            if (max_frame_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.getAllStackTraces(max_frame_count, stack_info_ptr, thread_count_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetAllStackTraces");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadListStackTraces(Pointer env, int thread_count, Pointer thread_list, int max_frame_count, Pointer stack_info_ptr) {
        // Source: JVMTIFunctionsSource.java:799
        Pointer anchor = prologue(env, "GetThreadListStackTraces");
        try {
            if (!(phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (thread_list.isZero() || stack_info_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            if (thread_count < 0 || max_frame_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.getThreadListStackTraces(thread_count, thread_list, max_frame_count, stack_info_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadListStackTraces");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data_ptr) {
        // Source: JVMTIFunctionsSource.java:809
        Pointer anchor = prologue(env, "GetThreadLocalStorage");
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
            return JVMTIThreadLocalStorage.getThreadLocalStorage(handleAsThread, data_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int SetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data) {
        // Source: JVMTIFunctionsSource.java:817
        Pointer anchor = prologue(env, "SetThreadLocalStorage");
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
            return JVMTIThreadLocalStorage.setThreadLocalStorage(handleAsThread, data);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetThreadLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int GetStackTrace(Pointer env, JniHandle thread, int start_depth, int max_frame_count, Pointer frame_buffer, Pointer count_ptr) {
        // Source: JVMTIFunctionsSource.java:824
        Pointer anchor = prologue(env, "GetStackTrace");
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
            if (max_frame_count < 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
            return JVMTIThreadFunctions.getStackTrace(handleAsThread, start_depth, max_frame_count, frame_buffer, count_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetStackTrace");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved105();
        // Source: JVMTIFunctionsSource.java:835

    @VM_ENTRY_POINT
    private static int GetTag(Pointer env, JniHandle object, Pointer tag_ptr) {
        // Source: JVMTIFunctionsSource.java:838
        Pointer anchor = prologue(env, "GetTag");
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
            epilogue(anchor, "GetTag");
        }
    }

    @VM_ENTRY_POINT
    private static int SetTag(Pointer env, JniHandle object, long tag) {
        // Source: JVMTIFunctionsSource.java:849
        Pointer anchor = prologue(env, "SetTag");
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
            epilogue(anchor, "SetTag");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceGarbageCollection(Pointer env) {
        // Source: JVMTIFunctionsSource.java:859
        Pointer anchor = prologue(env, "ForceGarbageCollection");
        try {
            System.gc();
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "ForceGarbageCollection");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverObjectsReachableFromObject(Pointer env, JniHandle object, Address object_reference_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:865
        Pointer anchor = prologue(env, "IterateOverObjectsReachableFromObject");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IterateOverObjectsReachableFromObject");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverReachableObjects(Pointer env, Address heap_root_callback, Address stack_ref_callback, Address object_ref_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:870
        Pointer anchor = prologue(env, "IterateOverReachableObjects");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IterateOverReachableObjects");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverHeap(Pointer env, int object_filter, Address heap_object_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:875
        Pointer anchor = prologue(env, "IterateOverHeap");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IterateOverHeap");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverInstancesOfClass(Pointer env, JniHandle klass, int object_filter, Address heap_object_callback, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:880
        Pointer anchor = prologue(env, "IterateOverInstancesOfClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IterateOverInstancesOfClass");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved113();
        // Source: JVMTIFunctionsSource.java:885

    @VM_ENTRY_POINT
    private static int GetObjectsWithTags(Pointer env, int tag_count, Pointer tags, Pointer count_ptr, Pointer object_result_ptr, Pointer tag_result_ptr) {
        // Source: JVMTIFunctionsSource.java:888
        Pointer anchor = prologue(env, "GetObjectsWithTags");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetObjectsWithTags");
        }
    }

    @VM_ENTRY_POINT
    private static int FollowReferences(Pointer env, int heap_filter, JniHandle klass, JniHandle initial_object, Pointer callbacks, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:893
        Pointer anchor = prologue(env, "FollowReferences");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "FollowReferences");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateThroughHeap(Pointer env, int heap_filter, JniHandle klass, Pointer callbacks, Pointer user_data) {
        // Source: JVMTIFunctionsSource.java:898
        Pointer anchor = prologue(env, "IterateThroughHeap");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "IterateThroughHeap");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved117();
        // Source: JVMTIFunctionsSource.java:903

    @VM_ENTRY_POINT
    private static native void reserved118();
        // Source: JVMTIFunctionsSource.java:906

    @VM_ENTRY_POINT
    private static native void reserved119();
        // Source: JVMTIFunctionsSource.java:909

    @VM_ENTRY_POINT
    private static int SetJNIFunctionTable(Pointer env, Pointer function_table) {
        // Source: JVMTIFunctionsSource.java:912
        Pointer anchor = prologue(env, "SetJNIFunctionTable");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetJNIFunctionTable");
        }
    }

    @VM_ENTRY_POINT
    private static int GetJNIFunctionTable(Pointer env, Pointer function_table) {
        // Source: JVMTIFunctionsSource.java:917
        Pointer anchor = prologue(env, "GetJNIFunctionTable");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetJNIFunctionTable");
        }
    }

    @VM_ENTRY_POINT
    private static int SetEventCallbacks(Pointer env, Pointer callbacks, int size_of_callbacks) {
        // Source: JVMTIFunctionsSource.java:922
        Pointer anchor = prologue(env, "SetEventCallbacks");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Pointer envCallbacks = CALLBACKS.get(env).asPointer();
            Memory.copyBytes(callbacks, envCallbacks, Size.fromInt(size_of_callbacks));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetEventCallbacks");
        }
    }

    @VM_ENTRY_POINT
    private static int GenerateEvents(Pointer env, int event_type) {
        // Source: JVMTIFunctionsSource.java:930
        Pointer anchor = prologue(env, "GenerateEvents");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GenerateEvents");
        }
    }

    @VM_ENTRY_POINT
    private static int GetExtensionFunctions(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        // Source: JVMTIFunctionsSource.java:935
        Pointer anchor = prologue(env, "GetExtensionFunctions");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetExtensionFunctions");
        }
    }

    @VM_ENTRY_POINT
    private static int GetExtensionEvents(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        // Source: JVMTIFunctionsSource.java:940
        Pointer anchor = prologue(env, "GetExtensionEvents");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetExtensionEvents");
        }
    }

    @VM_ENTRY_POINT
    private static int SetExtensionEventCallback(Pointer env, int extension_event_index, Address callback) {
        // Source: JVMTIFunctionsSource.java:945
        Pointer anchor = prologue(env, "SetExtensionEventCallback");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetExtensionEventCallback");
        }
    }

    @VM_ENTRY_POINT
    private static int DisposeEnvironment(Pointer env) {
        // Source: JVMTIFunctionsSource.java:950
        Pointer anchor = prologue(env, "DisposeEnvironment");
        try {
            // PHASES: ANY
            return JVMTI.disposeEnv(env);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "DisposeEnvironment");
        }
    }

    @VM_ENTRY_POINT
    private static int GetErrorName(Pointer env, int error, Pointer name_ptr) {
        // Source: JVMTIFunctionsSource.java:956
        Pointer anchor = prologue(env, "GetErrorName");
        try {
            // PHASES: ANY
            if (name_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
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
            epilogue(anchor, "GetErrorName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetlongFormat(Pointer env, Pointer format_ptr) {
        // Source: JVMTIFunctionsSource.java:970
        Pointer anchor = prologue(env, "GetlongFormat");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetlongFormat");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperties(Pointer env, Pointer count_ptr, Pointer property_ptr) {
        // Source: JVMTIFunctionsSource.java:975
        Pointer anchor = prologue(env, "GetSystemProperties");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetSystemProperties");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperty(Pointer env, Pointer property, Pointer value_ptr) {
        // Source: JVMTIFunctionsSource.java:980
        Pointer anchor = prologue(env, "GetSystemProperty");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (property.isZero() || value_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTI.getSystemProperty(env, property, value_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetSystemProperty");
        }
    }

    @VM_ENTRY_POINT
    private static int SetSystemProperty(Pointer env, Pointer property, Pointer value) {
        // Source: JVMTIFunctionsSource.java:987
        Pointer anchor = prologue(env, "SetSystemProperty");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetSystemProperty");
        }
    }

    @VM_ENTRY_POINT
    private static int GetPhase(Pointer env, Pointer phase_ptr) {
        // Source: JVMTIFunctionsSource.java:992
        Pointer anchor = prologue(env, "GetPhase");
        try {
            // PHASES: ANY
            if (phase_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTI.getPhase(phase_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetPhase");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:999
        Pointer anchor = prologue(env, "GetCurrentThreadCpuTimerInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetCurrentThreadCpuTimerInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTime(Pointer env, Pointer nanos_ptr) {
        // Source: JVMTIFunctionsSource.java:1004
        Pointer anchor = prologue(env, "GetCurrentThreadCpuTime");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetCurrentThreadCpuTime");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:1009
        Pointer anchor = prologue(env, "GetThreadCpuTimerInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadCpuTimerInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTime(Pointer env, JniHandle thread, Pointer nanos_ptr) {
        // Source: JVMTIFunctionsSource.java:1014
        Pointer anchor = prologue(env, "GetThreadCpuTime");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetThreadCpuTime");
        }
    }

    @VM_ENTRY_POINT
    private static int GetTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JVMTIFunctionsSource.java:1019
        Pointer anchor = prologue(env, "GetTimerInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetTimerInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetTime(Pointer env, Pointer nanos_ptr) {
        // Source: JVMTIFunctionsSource.java:1024
        Pointer anchor = prologue(env, "GetTime");
        try {
            // PHASES: ANY
            if (nanos_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            nanos_ptr.setLong(System.nanoTime());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetTime");
        }
    }

    @VM_ENTRY_POINT
    private static int GetPotentialCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:1032
        Pointer anchor = prologue(env, "GetPotentialCapabilities");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            // Currently we don't have any phase-limited or ownership limitations
            JVMTICapabilities.setAll(capabilities_ptr);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetPotentialCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved141();
        // Source: JVMTIFunctionsSource.java:1041

    @VM_ENTRY_POINT
    private static int AddCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:1044
        Pointer anchor = prologue(env, "AddCapabilities");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Pointer envCaps = CAPABILITIES.getPtr(env);
            for (int i = 0; i < JVMTICapabilities.values.length; i++) {
                JVMTICapabilities cap = JVMTICapabilities.values[i];
                if (cap.get(capabilities_ptr)) {
                    if (cap.can) {
                        cap.set(envCaps, true);
                    } else {
                        JVMTI.debug(cap);
                        return JVMTI_ERROR_NOT_AVAILABLE; // TODO
                    }
                }
            }
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "AddCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static int RelinquishCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JVMTIFunctionsSource.java:1063
        Pointer anchor = prologue(env, "RelinquishCapabilities");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
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
            epilogue(anchor, "RelinquishCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static int GetAvailableProcessors(Pointer env, Pointer processor_count_ptr) {
        // Source: JVMTIFunctionsSource.java:1077
        Pointer anchor = prologue(env, "GetAvailableProcessors");
        try {
            // PHASES: ANY
            if (processor_count_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            processor_count_ptr.setInt(Runtime.getRuntime().availableProcessors());
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetAvailableProcessors");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassVersionNumbers(Pointer env, JniHandle klass, Pointer minor_version_ptr, Pointer major_version_ptr) {
        // Source: JVMTIFunctionsSource.java:1085
        Pointer anchor = prologue(env, "GetClassVersionNumbers");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (minor_version_ptr.isZero() ||  minor_version_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetClassVersionNumbers");
        }
    }

    @VM_ENTRY_POINT
    private static int GetConstantPool(Pointer env, JniHandle klass, Pointer constant_pool_count_ptr, Pointer constant_pool_byte_count_ptr, Pointer constant_pool_bytes_ptr) {
        // Source: JVMTIFunctionsSource.java:1092
        Pointer anchor = prologue(env, "GetConstantPool");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetConstantPool");
        }
    }

    @VM_ENTRY_POINT
    private static int GetEnvironmentLocalStorage(Pointer env, Pointer data_ptr) {
        // Source: JVMTIFunctionsSource.java:1097
        Pointer anchor = prologue(env, "GetEnvironmentLocalStorage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetEnvironmentLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int SetEnvironmentLocalStorage(Pointer env, Pointer data) {
        // Source: JVMTIFunctionsSource.java:1102
        Pointer anchor = prologue(env, "SetEnvironmentLocalStorage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetEnvironmentLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int AddToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // Source: JVMTIFunctionsSource.java:1107
        Pointer anchor = prologue(env, "AddToBootstrapClassLoaderSearch");
        try {
            // PHASES ONLOAD,LIVE
            if (segment.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIClassFunctions.addToBootstrapClassLoaderSearch(env, segment);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "AddToBootstrapClassLoaderSearch");
        }
    }

    @VM_ENTRY_POINT
    private static int SetVerboseFlag(Pointer env, int flag, boolean value) {
        // Source: JVMTIFunctionsSource.java:1114
        Pointer anchor = prologue(env, "SetVerboseFlag");
        try {
            // PHASES: ANY
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
            epilogue(anchor, "SetVerboseFlag");
        }
    }

    @VM_ENTRY_POINT
    private static int AddToSystemClassLoaderSearch(Pointer env, Pointer segment) {
        // Source: JVMTIFunctionsSource.java:1136
        Pointer anchor = prologue(env, "AddToSystemClassLoaderSearch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "AddToSystemClassLoaderSearch");
        }
    }

    @VM_ENTRY_POINT
    private static int RetransformClasses(Pointer env, int class_count, Pointer classes) {
        // Source: JVMTIFunctionsSource.java:1141
        Pointer anchor = prologue(env, "RetransformClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "RetransformClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorStackDepthInfo(Pointer env, JniHandle thread, Pointer monitor_info_count_ptr, Pointer monitor_info_ptr) {
        // Source: JVMTIFunctionsSource.java:1146
        Pointer anchor = prologue(env, "GetOwnedMonitorStackDepthInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE; // TODO
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetOwnedMonitorStackDepthInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectSize(Pointer env, JniHandle object, Pointer size_ptr) {
        // Source: JVMTIFunctionsSource.java:1151
        Pointer anchor = prologue(env, "GetObjectSize");
        try {
            if (!(phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (size_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JVMTIClassFunctions.getObjectSize(object.unhand(), size_ptr);
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "GetObjectSize");
        }
    }

    /**
     * This function is an extension and appears in the extended JVMTI interface table,
     * as that is a convenient way to invoke it from native code. It's purpose is
     * simply to record the value of the C struct that denotes the JVMTI environment.
     */
    @VM_ENTRY_POINT
    private static int SetJVMTIEnv(Pointer env) {
        // Source: JVMTIFunctionsSource.java:1163
        Pointer anchor = prologue(env, "SetJVMTIEnv");
        try {
            JVMTI.setJVMTIEnv(env);
            return 0;
        } catch (Throwable t) {
            return JVMTI_ERROR_INTERNAL;
        } finally {
            epilogue(anchor, "SetJVMTIEnv");
        }
    }

// END GENERATED CODE
}
