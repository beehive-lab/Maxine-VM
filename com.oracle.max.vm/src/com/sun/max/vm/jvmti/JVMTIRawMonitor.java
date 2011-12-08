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

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;

/**
 * Simple implementation of the JVMTI Raw Monitor functionality.
 * For now just use native monitors and condition variables.
 */
public class JVMTIRawMonitor {

    private static final long RM_MAGIC = ('T' << 24) + ('I' << 16) + ('R' << 8) + 'M';

    public enum Struct {
        MAGIC(0),
        NAME(8),
        MUTEX(16),
        CONDITION(24);

        public final int offset;
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

    static {
        new CriticalMethod(JVMTIRawMonitor.class, "create", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "destroy", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "enter", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "exit", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "wait", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "notify", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "notifyAll", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    static int create(Pointer namePtr, Pointer rawMonitorPtr) {
        Pointer rawMonitor = Memory.allocate(Struct.SIZE);
        Word nativeMutex = OSMonitor.newMutex(false);
        Word nativeCondition = OSMonitor.newCondition(false);
        Word namePtrCopy = CString.copy(namePtr);

        if (rawMonitor.isZero() || nativeCondition.isZero() || nativeMutex.isZero() || namePtrCopy.isZero()) {
            // Checkstyle: stop
            if (!rawMonitor.isZero()) { Memory.deallocate(rawMonitor); }
            if (!nativeMutex.isZero()) { Memory.deallocate(nativeMutex.asPointer()); }
            if (!nativeCondition.isZero()) { Memory.deallocate(nativeCondition.asPointer()); }
            if (!namePtrCopy.isZero()) { Memory.deallocate(namePtrCopy.asPointer()); }
            // Checkstyle: resume
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        Struct.NAME.set(rawMonitor, namePtrCopy);
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
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (OSMonitor.nativeMutexLock(Struct.MUTEX.get(rawMonitor))) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    static int exit(Word rawMonitor) {
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        Word mutex = checkOwnerShip(rawMonitor);
        if (mutex.isZero()) {
            return JVMTI_ERROR_NOT_MONITOR_OWNER;
        }
        if (OSMonitor.nativeMutexUnlock(mutex)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }

    }

    static int wait(Word rawMonitor, long millis) {
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        Word mutex = checkOwnerShip(rawMonitor);
        if (mutex.isZero()) {
            return JVMTI_ERROR_NOT_MONITOR_OWNER;
        }
        if (millis == -1) {
            // jdwp seems to use -1 when it means 0
            millis = 0;
        }
        if (OSMonitor.nativeConditionWait(mutex, Struct.CONDITION.get(rawMonitor), millis)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    static int notify(Word rawMonitor) {
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (checkOwnerShip(rawMonitor).isZero()) {
            return JVMTI_ERROR_NOT_MONITOR_OWNER;
        }
        if (OSMonitor.nativeConditionNotify(Struct.CONDITION.get(rawMonitor), false)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    static int notifyAll(Word rawMonitor) {
        if (!Struct.validate(rawMonitor)) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        if (checkOwnerShip(rawMonitor).isZero()) {
            return JVMTI_ERROR_NOT_MONITOR_OWNER;
        }
        if (OSMonitor.nativeConditionNotify(Struct.CONDITION.get(rawMonitor), true)) {
            return JVMTI_ERROR_NONE;
        } else {
            return JVMTI_ERROR_INTERNAL; // TODO be more specific
        }
    }

    /**
     * Check that this thread owns the monitor.
     * @param rawMonitor
     * @return the mutex component if owner, {@code Pointer.zero()} if not.
     */
    private static Word checkOwnerShip(Word rawMonitor) {
        // since native mutexes support recursion, we can
        // check for ownership just by trying to get the lock again.
        // If this fails, we don't own it, otherwise we just release it,
        // thereby decrementing the recursion count back to where we came in.
        Word mutex = Struct.MUTEX.get(rawMonitor);
        if (OSMonitor.nativeMutexTryLock(mutex)) {
            OSMonitor.nativeMutexUnlock(mutex);
            return mutex;
        } else {
            return Pointer.zero();
        }
    }

}
