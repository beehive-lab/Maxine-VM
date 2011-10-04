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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;

/**
 * Support for the JVMTI functions related to {@link Thread}.
 */
class JVMTIThreads {
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
        // TODO Maxine does not keep any state beyond Thread.State so the detailed JVMTI states are unavailable
        VmThread vmThread = VmThread.fromJava(thread);
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
        threadStatePtr.setInt(state);
        return JVMTI_ERROR_NONE;
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
     * Types too complex to do in Java, delegate to native code.
     * @param threadInfoPtr
     * @param name
     * @param priority
     * @param isDaemon
     * @param threadGroup
     * @param contextClassLoader
     */
    @C_FUNCTION
    private static native void setJVMTIThreadInfo(Pointer threadInfoPtr, Pointer name, int priority, boolean isDaemon, JniHandle threadGroup, JniHandle contextClassLoader);

}
