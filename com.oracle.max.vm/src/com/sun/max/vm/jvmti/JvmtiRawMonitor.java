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

import static com.sun.max.vm.jvmti.JvmtiConstants.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;

/**
 * Simple implementation of the JVMTI Raw Monitor functionality.
 * For now just use native monitors and condition variables.
 */
public class JvmtiRawMonitor {

    private static final long RM_MAGIC = ('T' << 24) + ('I' << 16) + ('R' << 8) + 'M';

    enum Struct {
        MAGIC(0),
        NAME(8),
        MUTEX(16),
        CONDITION(24);

        private int offset;
        private static final Size SIZE = Size.fromInt(32);

        Struct(int offset) {
            this.offset = offset;
        }

        void set(Word base, Word value) {
            base.asPointer().writeWord(offset, value);
        }

        Word get(Word base) {
            return base.asPointer().readWord(offset);
        }

        static boolean validate(Word rawMonitor) {
            return Struct.MAGIC.get(rawMonitor).asAddress().toLong() == RM_MAGIC;
        }
    }

    static Size mutexSize;
    static Size conditionSize;

    static {
        new CriticalMethod(JvmtiRawMonitor.class, "create", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JvmtiRawMonitor.class, "destroy", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JvmtiRawMonitor.class, "enter", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JvmtiRawMonitor.class, "exit", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JvmtiRawMonitor.class, "wait", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JvmtiRawMonitor.class, "notify", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JvmtiRawMonitor.class, "notifyAll", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    static void initialize() {
        mutexSize = Size.fromInt(nativeMutexSize());
        conditionSize = Size.fromInt(nativeConditionSize());
    }

    static int create(Pointer name, Pointer rawMonitorPtr) {
        Pointer rawMonitor = Memory.allocate(Struct.SIZE);
        Pointer nativeMutex = Memory.allocate(mutexSize);
        Pointer nativeCondition = Memory.allocate(conditionSize);

        if (rawMonitor.isZero() || nativeCondition.isZero() || nativeMutex.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        nativeMutexInitialize(nativeMutex);
        nativeConditionInitialize(nativeCondition);
        Struct.CONDITION.set(rawMonitor, nativeCondition);
        Struct.MUTEX.set(rawMonitor, nativeMutex);
        Struct.MAGIC.set(rawMonitor, Address.fromLong(RM_MAGIC));

        rawMonitorPtr.setWord(rawMonitor);
        return JVMTI_ERROR_NONE;
    }

    static int destroy(Word rawMonitor) {
        // TODO check ownership
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        Memory.deallocate(Struct.CONDITION.get(rawMonitor).asPointer());
        Memory.deallocate(Struct.MUTEX.get(rawMonitor).asPointer());
        Memory.deallocate(rawMonitor.asPointer());
        return JVMTI_ERROR_NONE;
    }

    static int enter(Word rawMonitor) {
        // TODO check ownership
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (nativeMutexLock(Struct.MUTEX.get(rawMonitor))) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    static int exit(Word rawMonitor) {
        // TODO check ownership
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (nativeMutexUnlock(Struct.MUTEX.get(rawMonitor))) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }

    }

    static int wait(Word rawMonitor, long millis) {
        // TODO check ownership
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (nativeConditionWait(Struct.MUTEX.get(rawMonitor), Struct.CONDITION.get(rawMonitor), millis)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    static int notify(Word rawMonitor) {
        // TODO check ownership
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (nativeConditionNotify(Struct.CONDITION.get(rawMonitor), false)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    static int notifyAll(Word rawMonitor) {
        // TODO check ownership
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (nativeConditionNotify(Struct.CONDITION.get(rawMonitor), true)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    /*
     * These are already defined as private in com.sun.max.vm.monitor.modal.sync.nat.NativeMutex/ConditionVariable.
     * At this point we don't want to establish a dependency to that code.
     */

    static {
        new CriticalNativeMethod(JvmtiRawMonitor.class, "nativeMutexUnlock");
        new CriticalNativeMethod(JvmtiRawMonitor.class, "nativeConditionWait");
    }

    @C_FUNCTION
    private static native int nativeMutexSize();

    @C_FUNCTION
    private static native void nativeMutexInitialize(Word mutex);

    @C_FUNCTION
    private static native boolean nativeMutexUnlock(Word mutex);

    private static native boolean nativeMutexLock(Word mutex);

    @C_FUNCTION
    private static native int nativeConditionSize();

    @C_FUNCTION
    private static native void nativeConditionInitialize(Word condition);

    @C_FUNCTION
    private static native boolean nativeConditionNotify(Word condition, boolean all);

    private static native boolean nativeConditionWait(Word mutex, Word condition, long timeoutMilliSeconds);

}
