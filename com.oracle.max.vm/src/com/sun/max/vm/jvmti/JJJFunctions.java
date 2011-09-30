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
import static com.sun.max.vm.jvmti.JJJConstants.*;
import static com.sun.max.vm.jvmti.JJJEnvImplFields.*;
import static com.sun.max.vm.jni.JniFunctions.JNI_ERR;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;

/**
 * The transformed form of {@link JJJFunctionsSource}.
 * This file is read-only to all but {@link JJJFunctionsGenerator}.
 * Do not add code here, add it to the appropriate implementation class.
 */
public class JJJFunctions  {
 // Checkstyle: stop method name check

// START GENERATED CODE

    private static final boolean INSTRUMENTED = false;

    @VM_ENTRY_POINT
    private static native void reserved1();
        // Source: JJJFunctionsSource.java:39

    @VM_ENTRY_POINT
    private static int SetEventNotificationMode(Pointer env, int mode, int event_type, JniHandle event_thread) {
        // Source: JJJFunctionsSource.java:42
        Pointer anchor = prologue(env, "SetEventNotificationMode");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (event_thread.isZero()) {
                long envMask = EVENTMASK.get(env).asAddress().toLong();
                long maskBit = getEventBitMask(event_type);
                if (maskBit < 0) {
                    return JVMTI_ERROR_INVALID_EVENT_TYPE;
                }
                if (mode == JVMTI_ENABLE) {
                    envMask = envMask | maskBit;
                } else if (mode == JVMTI_DISABLE) {
                    envMask = envMask & ~maskBit;
                } else {
                    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
                }
                EVENTMASK.set(env, Address.fromLong(envMask));
                return JVMTI_ERROR_NONE;
            } else {
                // TODO handle per-thread events
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetEventNotificationMode");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved3();
        // Source: JJJFunctionsSource.java:66

    @VM_ENTRY_POINT
    private static int GetAllThreads(Pointer env, Pointer threads_count_ptr, Pointer threads_ptr) {
        // Source: JJJFunctionsSource.java:69
        Pointer anchor = prologue(env, "GetAllThreads");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetAllThreads");
        }
    }

    @VM_ENTRY_POINT
    private static int SuspendThread(Pointer env, JniHandle thread) {
        // Source: JJJFunctionsSource.java:74
        Pointer anchor = prologue(env, "SuspendThread");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SuspendThread");
        }
    }

    @VM_ENTRY_POINT
    private static int ResumeThread(Pointer env, JniHandle thread) {
        // Source: JJJFunctionsSource.java:79
        Pointer anchor = prologue(env, "ResumeThread");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ResumeThread");
        }
    }

    @VM_ENTRY_POINT
    private static int StopThread(Pointer env, JniHandle thread, JniHandle exception) {
        // Source: JJJFunctionsSource.java:84
        Pointer anchor = prologue(env, "StopThread");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "StopThread");
        }
    }

    @VM_ENTRY_POINT
    private static int InterruptThread(Pointer env, JniHandle thread) {
        // Source: JJJFunctionsSource.java:89
        Pointer anchor = prologue(env, "InterruptThread");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "InterruptThread");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadInfo(Pointer env, JniHandle thread, Pointer info_ptr) {
        // Source: JJJFunctionsSource.java:94
        Pointer anchor = prologue(env, "GetThreadInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorInfo(Pointer env, JniHandle thread, Pointer owned_monitor_count_ptr, Pointer owned_monitors_ptr) {
        // Source: JJJFunctionsSource.java:99
        Pointer anchor = prologue(env, "GetOwnedMonitorInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetOwnedMonitorInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentContendedMonitor(Pointer env, JniHandle thread, Pointer monitor_ptr) {
        // Source: JJJFunctionsSource.java:104
        Pointer anchor = prologue(env, "GetCurrentContendedMonitor");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetCurrentContendedMonitor");
        }
    }

    @VM_ENTRY_POINT
    private static int RunAgentThread(Pointer env, JniHandle jthread, Address proc, Pointer arg, int priority) {
        // Source: JJJFunctionsSource.java:109
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
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RunAgentThread");
        }
    }

    @VM_ENTRY_POINT
    private static int GetTopThreadGroups(Pointer env, Pointer group_count_ptr, Pointer groups_ptr) {
        // Source: JJJFunctionsSource.java:116
        Pointer anchor = prologue(env, "GetTopThreadGroups");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetTopThreadGroups");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupInfo(Pointer env, JniHandle group, Pointer info_ptr) {
        // Source: JJJFunctionsSource.java:121
        Pointer anchor = prologue(env, "GetThreadGroupInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadGroupInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadGroupChildren(Pointer env, JniHandle group, Pointer thread_count_ptr, Pointer threads_ptr, Pointer group_count_ptr, Pointer groups_ptr) {
        // Source: JJJFunctionsSource.java:126
        Pointer anchor = prologue(env, "GetThreadGroupChildren");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadGroupChildren");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFrameCount(Pointer env, JniHandle thread, Pointer count_ptr) {
        // Source: JJJFunctionsSource.java:131
        Pointer anchor = prologue(env, "GetFrameCount");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetFrameCount");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadState(Pointer env, JniHandle thread, Pointer thread_state_ptr) {
        // Source: JJJFunctionsSource.java:136
        Pointer anchor = prologue(env, "GetThreadState");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadState");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThread(Pointer env, Pointer thread_ptr) {
        // Source: JJJFunctionsSource.java:141
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
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetCurrentThread");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFrameLocation(Pointer env, JniHandle thread, int depth, Pointer method_ptr, Pointer location_ptr) {
        // Source: JJJFunctionsSource.java:149
        Pointer anchor = prologue(env, "GetFrameLocation");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetFrameLocation");
        }
    }

    @VM_ENTRY_POINT
    private static int NotifyFramePop(Pointer env, JniHandle thread, int depth) {
        // Source: JJJFunctionsSource.java:154
        Pointer anchor = prologue(env, "NotifyFramePop");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "NotifyFramePop");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalObject(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JJJFunctionsSource.java:159
        Pointer anchor = prologue(env, "GetLocalObject");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLocalObject");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalInt(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JJJFunctionsSource.java:164
        Pointer anchor = prologue(env, "GetLocalInt");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLocalInt");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalLong(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JJJFunctionsSource.java:169
        Pointer anchor = prologue(env, "GetLocalLong");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLocalLong");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JJJFunctionsSource.java:174
        Pointer anchor = prologue(env, "GetLocalFloat");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLocalFloat");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, Pointer value_ptr) {
        // Source: JJJFunctionsSource.java:179
        Pointer anchor = prologue(env, "GetLocalDouble");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLocalDouble");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalObject(Pointer env, JniHandle thread, int depth, int slot, JniHandle value) {
        // Source: JJJFunctionsSource.java:184
        Pointer anchor = prologue(env, "SetLocalObject");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetLocalObject");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalInt(Pointer env, JniHandle thread, int depth, int slot, int value) {
        // Source: JJJFunctionsSource.java:189
        Pointer anchor = prologue(env, "SetLocalInt");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetLocalInt");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalLong(Pointer env, JniHandle thread, int depth, int slot, long value) {
        // Source: JJJFunctionsSource.java:194
        Pointer anchor = prologue(env, "SetLocalLong");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetLocalLong");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalFloat(Pointer env, JniHandle thread, int depth, int slot, float value) {
        // Source: JJJFunctionsSource.java:199
        Pointer anchor = prologue(env, "SetLocalFloat");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetLocalFloat");
        }
    }

    @VM_ENTRY_POINT
    private static int SetLocalDouble(Pointer env, JniHandle thread, int depth, int slot, double value) {
        // Source: JJJFunctionsSource.java:204
        Pointer anchor = prologue(env, "SetLocalDouble");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetLocalDouble");
        }
    }

    @VM_ENTRY_POINT
    private static int CreateRawMonitor(Pointer env, Pointer name, Pointer monitor_ptr) {
        // Source: JJJFunctionsSource.java:209
        Pointer anchor = prologue(env, "CreateRawMonitor");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (name.isZero() || monitor_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            return JJJRawMonitor.create(name, monitor_ptr);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "CreateRawMonitor");
        }
    }

    @VM_ENTRY_POINT
    private static int DestroyRawMonitor(Pointer env, MonitorID rawMonitor) {
        // Source: JJJFunctionsSource.java:216
        Pointer anchor = prologue(env, "DestroyRawMonitor");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            return JJJRawMonitor.destroy(rawMonitor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "DestroyRawMonitor");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorEnter(Pointer env, MonitorID rawMonitor) {
        // Source: JJJFunctionsSource.java:222
        Pointer anchor = prologue(env, "RawMonitorEnter");
        try {
            // PHASES: ANY
            return JJJRawMonitor.enter(rawMonitor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RawMonitorEnter");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorExit(Pointer env, MonitorID rawMonitor) {
        // Source: JJJFunctionsSource.java:228
        Pointer anchor = prologue(env, "RawMonitorExit");
        try {
            // PHASES: ANY
            return JJJRawMonitor.exit(rawMonitor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RawMonitorExit");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorWait(Pointer env, MonitorID rawMonitor, long millis) {
        // Source: JJJFunctionsSource.java:234
        Pointer anchor = prologue(env, "RawMonitorWait");
        try {
            // PHASES: ANY
            return JJJRawMonitor.wait(rawMonitor, millis);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RawMonitorWait");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotify(Pointer env, MonitorID rawMonitor) {
        // Source: JJJFunctionsSource.java:240
        Pointer anchor = prologue(env, "RawMonitorNotify");
        try {
            // PHASES: ANY
            return JJJRawMonitor.notify(rawMonitor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RawMonitorNotify");
        }
    }

    @VM_ENTRY_POINT
    private static int RawMonitorNotifyAll(Pointer env, MonitorID rawMonitor) {
        // Source: JJJFunctionsSource.java:246
        Pointer anchor = prologue(env, "RawMonitorNotifyAll");
        try {
            // PHASES: ANY
            return JJJRawMonitor.notifyAll(rawMonitor);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RawMonitorNotifyAll");
        }
    }

    @VM_ENTRY_POINT
    private static int SetBreakpoint(Pointer env, MethodID method, long location) {
        // Source: JJJFunctionsSource.java:252
        Pointer anchor = prologue(env, "SetBreakpoint");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetBreakpoint");
        }
    }

    @VM_ENTRY_POINT
    private static int ClearBreakpoint(Pointer env, MethodID method, long location) {
        // Source: JJJFunctionsSource.java:257
        Pointer anchor = prologue(env, "ClearBreakpoint");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ClearBreakpoint");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved40();
        // Source: JJJFunctionsSource.java:262

    @VM_ENTRY_POINT
    private static int SetFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JJJFunctionsSource.java:265
        Pointer anchor = prologue(env, "SetFieldAccessWatch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetFieldAccessWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int ClearFieldAccessWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JJJFunctionsSource.java:270
        Pointer anchor = prologue(env, "ClearFieldAccessWatch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ClearFieldAccessWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int SetFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JJJFunctionsSource.java:275
        Pointer anchor = prologue(env, "SetFieldModificationWatch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetFieldModificationWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int ClearFieldModificationWatch(Pointer env, JniHandle klass, FieldID field) {
        // Source: JJJFunctionsSource.java:280
        Pointer anchor = prologue(env, "ClearFieldModificationWatch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ClearFieldModificationWatch");
        }
    }

    @VM_ENTRY_POINT
    private static int IsModifiableClass(Pointer env, JniHandle klass, Pointer is_modifiable_class_ptr) {
        // Source: JJJFunctionsSource.java:285
        Pointer anchor = prologue(env, "IsModifiableClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsModifiableClass");
        }
    }

    @VM_ENTRY_POINT
    private static int Allocate(Pointer env, long size, Pointer mem_ptr) {
        // Source: JJJFunctionsSource.java:290
        Pointer anchor = prologue(env, "Allocate");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "Allocate");
        }
    }

    @VM_ENTRY_POINT
    private static int Deallocate(Pointer env, Pointer mem) {
        // Source: JJJFunctionsSource.java:295
        Pointer anchor = prologue(env, "Deallocate");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "Deallocate");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassSignature(Pointer env, JniHandle klass, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JJJFunctionsSource.java:300
        Pointer anchor = prologue(env, "GetClassSignature");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassSignature");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassStatus(Pointer env, JniHandle klass, Pointer status_ptr) {
        // Source: JJJFunctionsSource.java:305
        Pointer anchor = prologue(env, "GetClassStatus");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassStatus");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSourceFileName(Pointer env, JniHandle klass, Pointer source_name_ptr) {
        // Source: JJJFunctionsSource.java:310
        Pointer anchor = prologue(env, "GetSourceFileName");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetSourceFileName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassModifiers(Pointer env, JniHandle klass, Pointer modifiers_ptr) {
        // Source: JJJFunctionsSource.java:315
        Pointer anchor = prologue(env, "GetClassModifiers");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassModifiers");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassMethods(Pointer env, JniHandle klass, Pointer method_count_ptr, Pointer methods_ptr) {
        // Source: JJJFunctionsSource.java:320
        Pointer anchor = prologue(env, "GetClassMethods");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassMethods");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassFields(Pointer env, JniHandle klass, Pointer field_count_ptr, Pointer fields_ptr) {
        // Source: JJJFunctionsSource.java:325
        Pointer anchor = prologue(env, "GetClassFields");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassFields");
        }
    }

    @VM_ENTRY_POINT
    private static int GetImplementedInterfaces(Pointer env, JniHandle klass, Pointer interface_count_ptr, Pointer interfaces_ptr) {
        // Source: JJJFunctionsSource.java:330
        Pointer anchor = prologue(env, "GetImplementedInterfaces");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetImplementedInterfaces");
        }
    }

    @VM_ENTRY_POINT
    private static int IsInterface(Pointer env, JniHandle klass, Pointer is_interface_ptr) {
        // Source: JJJFunctionsSource.java:335
        Pointer anchor = prologue(env, "IsInterface");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsInterface");
        }
    }

    @VM_ENTRY_POINT
    private static int IsArrayClass(Pointer env, JniHandle klass, Pointer is_array_class_ptr) {
        // Source: JJJFunctionsSource.java:340
        Pointer anchor = prologue(env, "IsArrayClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsArrayClass");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassLoader(Pointer env, JniHandle klass, Pointer classloader_ptr) {
        // Source: JJJFunctionsSource.java:345
        Pointer anchor = prologue(env, "GetClassLoader");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassLoader");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectHashCode(Pointer env, JniHandle object, Pointer hash_code_ptr) {
        // Source: JJJFunctionsSource.java:350
        Pointer anchor = prologue(env, "GetObjectHashCode");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetObjectHashCode");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectMonitorUsage(Pointer env, JniHandle object, Pointer info_ptr) {
        // Source: JJJFunctionsSource.java:355
        Pointer anchor = prologue(env, "GetObjectMonitorUsage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetObjectMonitorUsage");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldName(Pointer env, JniHandle klass, FieldID field, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JJJFunctionsSource.java:360
        Pointer anchor = prologue(env, "GetFieldName");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetFieldName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldDeclaringClass(Pointer env, JniHandle klass, FieldID field, Pointer declaring_class_ptr) {
        // Source: JJJFunctionsSource.java:365
        Pointer anchor = prologue(env, "GetFieldDeclaringClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetFieldDeclaringClass");
        }
    }

    @VM_ENTRY_POINT
    private static int GetFieldModifiers(Pointer env, JniHandle klass, FieldID field, Pointer modifiers_ptr) {
        // Source: JJJFunctionsSource.java:370
        Pointer anchor = prologue(env, "GetFieldModifiers");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetFieldModifiers");
        }
    }

    @VM_ENTRY_POINT
    private static int IsFieldSynthetic(Pointer env, JniHandle klass, FieldID field, Pointer is_synthetic_ptr) {
        // Source: JJJFunctionsSource.java:375
        Pointer anchor = prologue(env, "IsFieldSynthetic");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsFieldSynthetic");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodName(Pointer env, MethodID method, Pointer name_ptr, Pointer signature_ptr, Pointer generic_ptr) {
        // Source: JJJFunctionsSource.java:380
        Pointer anchor = prologue(env, "GetMethodName");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetMethodName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodDeclaringClass(Pointer env, MethodID method, Pointer declaring_class_ptr) {
        // Source: JJJFunctionsSource.java:385
        Pointer anchor = prologue(env, "GetMethodDeclaringClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetMethodDeclaringClass");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodModifiers(Pointer env, MethodID method, Pointer modifiers_ptr) {
        // Source: JJJFunctionsSource.java:390
        Pointer anchor = prologue(env, "GetMethodModifiers");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetMethodModifiers");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved67();
        // Source: JJJFunctionsSource.java:395

    @VM_ENTRY_POINT
    private static int GetMaxLocals(Pointer env, MethodID method, Pointer max_ptr) {
        // Source: JJJFunctionsSource.java:398
        Pointer anchor = prologue(env, "GetMaxLocals");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetMaxLocals");
        }
    }

    @VM_ENTRY_POINT
    private static int GetArgumentsSize(Pointer env, MethodID method, Pointer size_ptr) {
        // Source: JJJFunctionsSource.java:403
        Pointer anchor = prologue(env, "GetArgumentsSize");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetArgumentsSize");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLineNumberTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // Source: JJJFunctionsSource.java:408
        Pointer anchor = prologue(env, "GetLineNumberTable");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLineNumberTable");
        }
    }

    @VM_ENTRY_POINT
    private static int GetMethodLocation(Pointer env, MethodID method, Pointer start_location_ptr, Pointer end_location_ptr) {
        // Source: JJJFunctionsSource.java:413
        Pointer anchor = prologue(env, "GetMethodLocation");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetMethodLocation");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLocalVariableTable(Pointer env, MethodID method, Pointer entry_count_ptr, Pointer table_ptr) {
        // Source: JJJFunctionsSource.java:418
        Pointer anchor = prologue(env, "GetLocalVariableTable");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLocalVariableTable");
        }
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefix(Pointer env, Pointer prefix) {
        // Source: JJJFunctionsSource.java:423
        Pointer anchor = prologue(env, "SetNativeMethodPrefix");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetNativeMethodPrefix");
        }
    }

    @VM_ENTRY_POINT
    private static int SetNativeMethodPrefixes(Pointer env, int prefix_count, Pointer prefixes) {
        // Source: JJJFunctionsSource.java:428
        Pointer anchor = prologue(env, "SetNativeMethodPrefixes");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetNativeMethodPrefixes");
        }
    }

    @VM_ENTRY_POINT
    private static int GetBytecodes(Pointer env, MethodID method, Pointer bytecode_count_ptr, Pointer bytecodes_ptr) {
        // Source: JJJFunctionsSource.java:433
        Pointer anchor = prologue(env, "GetBytecodes");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetBytecodes");
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodNative(Pointer env, MethodID method, Pointer is_native_ptr) {
        // Source: JJJFunctionsSource.java:438
        Pointer anchor = prologue(env, "IsMethodNative");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsMethodNative");
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodSynthetic(Pointer env, MethodID method, Pointer is_synthetic_ptr) {
        // Source: JJJFunctionsSource.java:443
        Pointer anchor = prologue(env, "IsMethodSynthetic");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsMethodSynthetic");
        }
    }

    @VM_ENTRY_POINT
    private static int GetLoadedClasses(Pointer env, Pointer class_count_ptr, Pointer classes_ptr) {
        // Source: JJJFunctionsSource.java:448
        Pointer anchor = prologue(env, "GetLoadedClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetLoadedClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassLoaderClasses(Pointer env, JniHandle initiating_loader, Pointer class_count_ptr, Pointer classes_ptr) {
        // Source: JJJFunctionsSource.java:453
        Pointer anchor = prologue(env, "GetClassLoaderClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassLoaderClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int PopFrame(Pointer env, JniHandle thread) {
        // Source: JJJFunctionsSource.java:458
        Pointer anchor = prologue(env, "PopFrame");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "PopFrame");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnObject(Pointer env, JniHandle thread, JniHandle value) {
        // Source: JJJFunctionsSource.java:463
        Pointer anchor = prologue(env, "ForceEarlyReturnObject");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceEarlyReturnObject");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnInt(Pointer env, JniHandle thread, int value) {
        // Source: JJJFunctionsSource.java:468
        Pointer anchor = prologue(env, "ForceEarlyReturnInt");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceEarlyReturnInt");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnLong(Pointer env, JniHandle thread, long value) {
        // Source: JJJFunctionsSource.java:473
        Pointer anchor = prologue(env, "ForceEarlyReturnLong");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceEarlyReturnLong");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnFloat(Pointer env, JniHandle thread, float value) {
        // Source: JJJFunctionsSource.java:478
        Pointer anchor = prologue(env, "ForceEarlyReturnFloat");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceEarlyReturnFloat");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnDouble(Pointer env, JniHandle thread, double value) {
        // Source: JJJFunctionsSource.java:483
        Pointer anchor = prologue(env, "ForceEarlyReturnDouble");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceEarlyReturnDouble");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceEarlyReturnVoid(Pointer env, JniHandle thread) {
        // Source: JJJFunctionsSource.java:488
        Pointer anchor = prologue(env, "ForceEarlyReturnVoid");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceEarlyReturnVoid");
        }
    }

    @VM_ENTRY_POINT
    private static int RedefineClasses(Pointer env, int class_count, Pointer class_definitions) {
        // Source: JJJFunctionsSource.java:493
        Pointer anchor = prologue(env, "RedefineClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RedefineClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int GetVersionNumber(Pointer env, Pointer version_ptr) {
        // Source: JJJFunctionsSource.java:498
        Pointer anchor = prologue(env, "GetVersionNumber");
        try {
            // PHASES: ANY
            if (version_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            version_ptr.setInt(JVMTI_VERSION);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetVersionNumber");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JJJFunctionsSource.java:506
        Pointer anchor = prologue(env, "GetCapabilities");
        try {
            // PHASES: ANY
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            capabilities_ptr.setLong(0, CAPABILITIES.getPtr(env).readLong(0));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSourceDebugExtension(Pointer env, JniHandle klass, Pointer source_debug_extension_ptr) {
        // Source: JJJFunctionsSource.java:514
        Pointer anchor = prologue(env, "GetSourceDebugExtension");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetSourceDebugExtension");
        }
    }

    @VM_ENTRY_POINT
    private static int IsMethodObsolete(Pointer env, MethodID method, Pointer is_obsolete_ptr) {
        // Source: JJJFunctionsSource.java:519
        Pointer anchor = prologue(env, "IsMethodObsolete");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IsMethodObsolete");
        }
    }

    @VM_ENTRY_POINT
    private static int SuspendThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // Source: JJJFunctionsSource.java:524
        Pointer anchor = prologue(env, "SuspendThreadList");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SuspendThreadList");
        }
    }

    @VM_ENTRY_POINT
    private static int ResumeThreadList(Pointer env, int request_count, Pointer request_list, Pointer results) {
        // Source: JJJFunctionsSource.java:529
        Pointer anchor = prologue(env, "ResumeThreadList");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ResumeThreadList");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved94();
        // Source: JJJFunctionsSource.java:534

    @VM_ENTRY_POINT
    private static native void reserved95();
        // Source: JJJFunctionsSource.java:537

    @VM_ENTRY_POINT
    private static native void reserved96();
        // Source: JJJFunctionsSource.java:540

    @VM_ENTRY_POINT
    private static native void reserved97();
        // Source: JJJFunctionsSource.java:543

    @VM_ENTRY_POINT
    private static native void reserved98();
        // Source: JJJFunctionsSource.java:546

    @VM_ENTRY_POINT
    private static native void reserved99();
        // Source: JJJFunctionsSource.java:549

    @VM_ENTRY_POINT
    private static int GetAllStackTraces(Pointer env, int max_frame_count, Pointer stack_info_ptr, Pointer thread_count_ptr) {
        // Source: JJJFunctionsSource.java:552
        Pointer anchor = prologue(env, "GetAllStackTraces");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetAllStackTraces");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadListStackTraces(Pointer env, int thread_count, Pointer thread_list, int max_frame_count, Pointer stack_info_ptr) {
        // Source: JJJFunctionsSource.java:557
        Pointer anchor = prologue(env, "GetThreadListStackTraces");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadListStackTraces");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data_ptr) {
        // Source: JJJFunctionsSource.java:562
        Pointer anchor = prologue(env, "GetThreadLocalStorage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int SetThreadLocalStorage(Pointer env, JniHandle thread, Pointer data) {
        // Source: JJJFunctionsSource.java:567
        Pointer anchor = prologue(env, "SetThreadLocalStorage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetThreadLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int GetStackTrace(Pointer env, JniHandle thread, int start_depth, int max_frame_count, Pointer frame_buffer, Pointer count_ptr) {
        // Source: JJJFunctionsSource.java:572
        Pointer anchor = prologue(env, "GetStackTrace");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetStackTrace");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved105();
        // Source: JJJFunctionsSource.java:577

    @VM_ENTRY_POINT
    private static int GetTag(Pointer env, JniHandle object, Pointer tag_ptr) {
        // Source: JJJFunctionsSource.java:580
        Pointer anchor = prologue(env, "GetTag");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetTag");
        }
    }

    @VM_ENTRY_POINT
    private static int SetTag(Pointer env, JniHandle object, long tag) {
        // Source: JJJFunctionsSource.java:585
        Pointer anchor = prologue(env, "SetTag");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetTag");
        }
    }

    @VM_ENTRY_POINT
    private static int ForceGarbageCollection(Pointer env) {
        // Source: JJJFunctionsSource.java:590
        Pointer anchor = prologue(env, "ForceGarbageCollection");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "ForceGarbageCollection");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverObjectsReachableFromObject(Pointer env, JniHandle object, Address object_reference_callback, Pointer user_data) {
        // Source: JJJFunctionsSource.java:595
        Pointer anchor = prologue(env, "IterateOverObjectsReachableFromObject");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IterateOverObjectsReachableFromObject");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverReachableObjects(Pointer env, Address heap_root_callback, Address stack_ref_callback, Address object_ref_callback, Pointer user_data) {
        // Source: JJJFunctionsSource.java:600
        Pointer anchor = prologue(env, "IterateOverReachableObjects");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IterateOverReachableObjects");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverHeap(Pointer env, int object_filter, Address heap_object_callback, Pointer user_data) {
        // Source: JJJFunctionsSource.java:605
        Pointer anchor = prologue(env, "IterateOverHeap");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IterateOverHeap");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateOverInstancesOfClass(Pointer env, JniHandle klass, int object_filter, Address heap_object_callback, Pointer user_data) {
        // Source: JJJFunctionsSource.java:610
        Pointer anchor = prologue(env, "IterateOverInstancesOfClass");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IterateOverInstancesOfClass");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved113();
        // Source: JJJFunctionsSource.java:615

    @VM_ENTRY_POINT
    private static int GetObjectsWithTags(Pointer env, int tag_count, Pointer tags, Pointer count_ptr, Pointer object_result_ptr, Pointer tag_result_ptr) {
        // Source: JJJFunctionsSource.java:618
        Pointer anchor = prologue(env, "GetObjectsWithTags");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetObjectsWithTags");
        }
    }

    @VM_ENTRY_POINT
    private static int FollowReferences(Pointer env, int heap_filter, JniHandle klass, JniHandle initial_object, Pointer callbacks, Pointer user_data) {
        // Source: JJJFunctionsSource.java:623
        Pointer anchor = prologue(env, "FollowReferences");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "FollowReferences");
        }
    }

    @VM_ENTRY_POINT
    private static int IterateThroughHeap(Pointer env, int heap_filter, JniHandle klass, Pointer callbacks, Pointer user_data) {
        // Source: JJJFunctionsSource.java:628
        Pointer anchor = prologue(env, "IterateThroughHeap");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "IterateThroughHeap");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved117();
        // Source: JJJFunctionsSource.java:633

    @VM_ENTRY_POINT
    private static native void reserved118();
        // Source: JJJFunctionsSource.java:636

    @VM_ENTRY_POINT
    private static native void reserved119();
        // Source: JJJFunctionsSource.java:639

    @VM_ENTRY_POINT
    private static int SetJNIFunctionTable(Pointer env, Pointer function_table) {
        // Source: JJJFunctionsSource.java:642
        Pointer anchor = prologue(env, "SetJNIFunctionTable");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetJNIFunctionTable");
        }
    }

    @VM_ENTRY_POINT
    private static int GetJNIFunctionTable(Pointer env, Pointer function_table) {
        // Source: JJJFunctionsSource.java:647
        Pointer anchor = prologue(env, "GetJNIFunctionTable");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetJNIFunctionTable");
        }
    }

    @VM_ENTRY_POINT
    private static int SetEventCallbacks(Pointer env, Pointer callbacks, int size_of_callbacks) {
        // Source: JJJFunctionsSource.java:652
        Pointer anchor = prologue(env, "SetEventCallbacks");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            Pointer envCallbacks = CALLBACKS.get(env).asPointer();
            Memory.copyBytes(callbacks, envCallbacks, Size.fromInt(size_of_callbacks));
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetEventCallbacks");
        }
    }

    @VM_ENTRY_POINT
    private static int GenerateEvents(Pointer env, int event_type) {
        // Source: JJJFunctionsSource.java:660
        Pointer anchor = prologue(env, "GenerateEvents");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GenerateEvents");
        }
    }

    @VM_ENTRY_POINT
    private static int GetExtensionFunctions(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        // Source: JJJFunctionsSource.java:665
        Pointer anchor = prologue(env, "GetExtensionFunctions");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetExtensionFunctions");
        }
    }

    @VM_ENTRY_POINT
    private static int GetExtensionEvents(Pointer env, Pointer extension_count_ptr, Pointer extensions) {
        // Source: JJJFunctionsSource.java:670
        Pointer anchor = prologue(env, "GetExtensionEvents");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetExtensionEvents");
        }
    }

    @VM_ENTRY_POINT
    private static int SetExtensionEventCallback(Pointer env, int extension_event_index, Address callback) {
        // Source: JJJFunctionsSource.java:675
        Pointer anchor = prologue(env, "SetExtensionEventCallback");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetExtensionEventCallback");
        }
    }

    @VM_ENTRY_POINT
    private static int DisposeEnvironment(Pointer env) {
        // Source: JJJFunctionsSource.java:680
        Pointer anchor = prologue(env, "DisposeEnvironment");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "DisposeEnvironment");
        }
    }

    @VM_ENTRY_POINT
    private static int GetErrorName(Pointer env, int error, Pointer name_ptr) {
        // Source: JJJFunctionsSource.java:685
        Pointer anchor = prologue(env, "GetErrorName");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetErrorName");
        }
    }

    @VM_ENTRY_POINT
    private static int GetlongFormat(Pointer env, Pointer format_ptr) {
        // Source: JJJFunctionsSource.java:690
        Pointer anchor = prologue(env, "GetlongFormat");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetlongFormat");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperties(Pointer env, Pointer count_ptr, Pointer property_ptr) {
        // Source: JJJFunctionsSource.java:695
        Pointer anchor = prologue(env, "GetSystemProperties");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetSystemProperties");
        }
    }

    @VM_ENTRY_POINT
    private static int GetSystemProperty(Pointer env, Pointer property, Pointer value_ptr) {
        // Source: JJJFunctionsSource.java:700
        Pointer anchor = prologue(env, "GetSystemProperty");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetSystemProperty");
        }
    }

    @VM_ENTRY_POINT
    private static int SetSystemProperty(Pointer env, Pointer property, Pointer value) {
        // Source: JJJFunctionsSource.java:705
        Pointer anchor = prologue(env, "SetSystemProperty");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetSystemProperty");
        }
    }

    @VM_ENTRY_POINT
    private static int GetPhase(Pointer env, Pointer phase_ptr) {
        // Source: JJJFunctionsSource.java:710
        Pointer anchor = prologue(env, "GetPhase");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetPhase");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JJJFunctionsSource.java:715
        Pointer anchor = prologue(env, "GetCurrentThreadCpuTimerInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetCurrentThreadCpuTimerInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetCurrentThreadCpuTime(Pointer env, Pointer nanos_ptr) {
        // Source: JJJFunctionsSource.java:720
        Pointer anchor = prologue(env, "GetCurrentThreadCpuTime");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetCurrentThreadCpuTime");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JJJFunctionsSource.java:725
        Pointer anchor = prologue(env, "GetThreadCpuTimerInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadCpuTimerInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetThreadCpuTime(Pointer env, JniHandle thread, Pointer nanos_ptr) {
        // Source: JJJFunctionsSource.java:730
        Pointer anchor = prologue(env, "GetThreadCpuTime");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetThreadCpuTime");
        }
    }

    @VM_ENTRY_POINT
    private static int GetTimerInfo(Pointer env, Pointer info_ptr) {
        // Source: JJJFunctionsSource.java:735
        Pointer anchor = prologue(env, "GetTimerInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetTimerInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetTime(Pointer env, Pointer nanos_ptr) {
        // Source: JJJFunctionsSource.java:740
        Pointer anchor = prologue(env, "GetTime");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetTime");
        }
    }

    @VM_ENTRY_POINT
    private static int GetPotentialCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JJJFunctionsSource.java:745
        Pointer anchor = prologue(env, "GetPotentialCapabilities");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            // Currently we don't have any phase-limited or ownership limitations
            JJJCapabilities.setAll(capabilities_ptr);
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetPotentialCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static native void reserved141();
        // Source: JJJFunctionsSource.java:754

    @VM_ENTRY_POINT
    private static int AddCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JJJFunctionsSource.java:757
        Pointer anchor = prologue(env, "AddCapabilities");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Pointer envCaps = CAPABILITIES.getPtr(env);
            for (int i = 0; i < JJJCapabilities.values.length; i++) {
                JJJCapabilities cap = JJJCapabilities.values[i];
                if (cap.get(capabilities_ptr)) {
                    if (cap.can) {
                        cap.set(envCaps, true);
                    } else {
                        return JVMTI_ERROR_NOT_AVAILABLE;
                    }
                }
            }
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "AddCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static int RelinquishCapabilities(Pointer env, Pointer capabilities_ptr) {
        // Source: JJJFunctionsSource.java:775
        Pointer anchor = prologue(env, "RelinquishCapabilities");
        try {
            if (!(phase == JVMTI_PHASE_ONLOAD || phase == JVMTI_PHASE_LIVE)) {
                return JVMTI_ERROR_WRONG_PHASE;
            }
            if (capabilities_ptr.isZero()) {
                return JVMTI_ERROR_NULL_POINTER;
            }
            Pointer envCaps = CAPABILITIES.getPtr(env);
            for (int i = 0; i < JJJCapabilities.values.length; i++) {
                JJJCapabilities cap = JJJCapabilities.values[i];
                if (cap.get(capabilities_ptr)) {
                   cap.set(envCaps, false);
                }
            }
            return JVMTI_ERROR_NONE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RelinquishCapabilities");
        }
    }

    @VM_ENTRY_POINT
    private static int GetAvailableProcessors(Pointer env, Pointer processor_count_ptr) {
        // Source: JJJFunctionsSource.java:789
        Pointer anchor = prologue(env, "GetAvailableProcessors");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetAvailableProcessors");
        }
    }

    @VM_ENTRY_POINT
    private static int GetClassVersionNumbers(Pointer env, JniHandle klass, Pointer minor_version_ptr, Pointer major_version_ptr) {
        // Source: JJJFunctionsSource.java:794
        Pointer anchor = prologue(env, "GetClassVersionNumbers");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetClassVersionNumbers");
        }
    }

    @VM_ENTRY_POINT
    private static int GetConstantPool(Pointer env, JniHandle klass, Pointer constant_pool_count_ptr, Pointer constant_pool_byte_count_ptr, Pointer constant_pool_bytes_ptr) {
        // Source: JJJFunctionsSource.java:799
        Pointer anchor = prologue(env, "GetConstantPool");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetConstantPool");
        }
    }

    @VM_ENTRY_POINT
    private static int GetEnvironmentLocalStorage(Pointer env, Pointer data_ptr) {
        // Source: JJJFunctionsSource.java:804
        Pointer anchor = prologue(env, "GetEnvironmentLocalStorage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetEnvironmentLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int SetEnvironmentLocalStorage(Pointer env, Pointer data) {
        // Source: JJJFunctionsSource.java:809
        Pointer anchor = prologue(env, "SetEnvironmentLocalStorage");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetEnvironmentLocalStorage");
        }
    }

    @VM_ENTRY_POINT
    private static int AddToBootstrapClassLoaderSearch(Pointer env, Pointer segment) {
        // Source: JJJFunctionsSource.java:814
        Pointer anchor = prologue(env, "AddToBootstrapClassLoaderSearch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "AddToBootstrapClassLoaderSearch");
        }
    }

    @VM_ENTRY_POINT
    private static int SetVerboseFlag(Pointer env, int flag, boolean value) {
        // Source: JJJFunctionsSource.java:819
        Pointer anchor = prologue(env, "SetVerboseFlag");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "SetVerboseFlag");
        }
    }

    @VM_ENTRY_POINT
    private static int AddToSystemClassLoaderSearch(Pointer env, Pointer segment) {
        // Source: JJJFunctionsSource.java:824
        Pointer anchor = prologue(env, "AddToSystemClassLoaderSearch");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "AddToSystemClassLoaderSearch");
        }
    }

    @VM_ENTRY_POINT
    private static int RetransformClasses(Pointer env, int class_count, Pointer classes) {
        // Source: JJJFunctionsSource.java:829
        Pointer anchor = prologue(env, "RetransformClasses");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "RetransformClasses");
        }
    }

    @VM_ENTRY_POINT
    private static int GetOwnedMonitorStackDepthInfo(Pointer env, JniHandle thread, Pointer monitor_info_count_ptr, Pointer monitor_info_ptr) {
        // Source: JJJFunctionsSource.java:834
        Pointer anchor = prologue(env, "GetOwnedMonitorStackDepthInfo");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetOwnedMonitorStackDepthInfo");
        }
    }

    @VM_ENTRY_POINT
    private static int GetObjectSize(Pointer env, JniHandle object, Pointer size_ptr) {
        // Source: JJJFunctionsSource.java:839
        Pointer anchor = prologue(env, "GetObjectSize");
        try {
            return JVMTI_ERROR_NOT_AVAILABLE;
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
            return JNI_ERR;
        } finally {
            epilogue(anchor, "GetObjectSize");
        }
    }

    @VM_ENTRY_POINT
    private static void SetJVMTIEnv(Pointer env) {
        // Source: JJJFunctionsSource.java:844
        Pointer anchor = prologue(env, "SetJVMTIEnv");
        try {
            JVMTI.setJVMTIEnv(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setJniException(t);
        } finally {
            epilogue(anchor, "SetJVMTIEnv");
        }
    }

// END GENERATED CODE
}
