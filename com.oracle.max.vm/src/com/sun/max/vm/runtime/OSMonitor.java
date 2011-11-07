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
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * Direct access to OS (native) monitors for low-level use.
 */
public class OSMonitor {
    static {
        new CriticalNativeMethod(OSMonitor.class, "nativeMutexSize");
        new CriticalNativeMethod(OSMonitor.class, "nativeMutexInitialize");
        new CriticalNativeMethod(OSMonitor.class, "nativeConditionSize");
        new CriticalNativeMethod(OSMonitor.class, "nativeConditionInitialize");
        new CriticalNativeMethod(OSMonitor.class, "nativeMutexUnlock");
        new CriticalNativeMethod(OSMonitor.class, "nativeMutexLock");
        new CriticalNativeMethod(OSMonitor.class, "nativeConditionNotify");
        new CriticalNativeMethod(OSMonitor.class, "nativeConditionWait");
        new CriticalNativeMethod(OSMonitor.class, "nativeTakeLockAndNotify");
        new CriticalNativeMethod(OSMonitor.class, "nativeTakeLockAndWait");
    }

    static int mutexSize;
    static int conditionSize;

    static int getMutexSize() {
        if (mutexSize == 0) {
            mutexSize = nativeMutexSize();
        }
        return mutexSize;
    }

    static int getConditionSize() {
        if (conditionSize == 0) {
            conditionSize = nativeConditionSize();
        }
        return conditionSize;
    }

    public static Word newMutex() {
        return newMutex(true);
    }

    public static Word newMutex(boolean mustAllocate) {
        Size size = Size.fromInt(getMutexSize());
        Word mutex;
        if (mustAllocate) {
            mutex = Memory.mustAllocate(size);
        } else {
            mutex = Memory.allocate(size);
        }
        if (!mutex.isZero()) {
            nativeMutexInitialize(mutex);
        }
        return mutex;
    }

    public static Word newCondition() {
        return newCondition(true);
    }

    public static Word newCondition(boolean mustAllocate) {
        Size size = Size.fromInt(getConditionSize());
        Word condition;
        if (mustAllocate) {
            condition = Memory.mustAllocate(size);
        } else {
            condition = Memory.allocate(size);
        }
        if (!condition.isZero()) {
            nativeConditionInitialize(condition);
        }
        return condition;
    }

    /**
     * A per-thread monitor used for suspend and resume.
     */
    public static class SuspendMonitor {
        private Word condition;
        private Word mutex;

        public void init() {
            if (mutex.isZero()) {
                mutex = OSMonitor.newMutex();
                condition = OSMonitor.newCondition();
            }
        }

        public void destroy() {
            if (!mutex.isZero()) {
                Memory.deallocate(mutex.asAddress());
            }
            if (!condition.isZero()) {
                Memory.deallocate(condition.asAddress());
            }
        }

        @INLINE
        public void suspend() {
            nativeTakeLockAndWait(mutex, condition);
        }

        @INLINE
        /**
         * Resume the suspended thread.
         * If the lock associated with the monitor cannot be acquired return false.
         */
        public boolean resume() {
            return nativeTakeLockAndNotify(mutex, condition);
        }

    }

    @C_FUNCTION
    public static native int nativeMutexSize();

    @C_FUNCTION
    public static native void nativeMutexInitialize(Word mutex);

    @C_FUNCTION
    public static native int nativeConditionSize();

    @C_FUNCTION
    public static native void nativeConditionInitialize(Word condition);

    @C_FUNCTION
    public static native boolean nativeMutexUnlock(Word mutex);

    // May block so JNI
    public static native boolean nativeMutexLock(Word mutex);

    @C_FUNCTION
    public static native boolean nativeConditionNotify(Word condition, boolean all);

    // May block so JNI
    public static native boolean nativeConditionWait(Word mutex, Word condition, long timeoutMilliSeconds);

    // These methods combine lock/wait lock/notify for thread suspend
    // Only the owning thread ever calls nativeTakeLockAndWait.
    // Only the VmOperation thread ever calls nativeTakeLockAndNotify

    @C_FUNCTION
    /**
     * Check mutex, if locked return false, else take it and notify condition.
     * I.e., this function does not block.
     */
    public static native boolean nativeTakeLockAndNotify(Word mutex, Word condition);

    /**
     * Take the mutex and wait on the condition.
     * This is JNI so that a suspended thread is THREAD_IN_NATIVE for VmOperation.
     */
    public static native boolean nativeTakeLockAndWait(Word mutex, Word condition);

}
