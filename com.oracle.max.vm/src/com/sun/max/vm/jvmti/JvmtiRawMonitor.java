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
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.runtime.*;

/**
 * Simple implementation of the JVMTI Raw Monitor functionality.
 * For now just use native monitors.
 */
public class JvmtiRawMonitor {

    static Size size;

    static void initialize() {
        size = Size.fromInt(nativeMutexSize());
    }

    static int create(Pointer name, Pointer rawMonitorPtr) {
        Pointer rawMonitor = Memory.allocate(size);
        if (rawMonitor.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        nativeMutexInitialize(rawMonitor);
        rawMonitorPtr.setWord(rawMonitor);
        return JVMTI_ERROR_NONE;
    }

    static int destroy(MonitorID rawMonitor) {
        // TODO check validity, ownership
        Memory.deallocate(rawMonitor.asPointer());
        return JVMTI_ERROR_NONE;
    }

    static int enter(MonitorID rawMonitor) {
        // TODO check validity, ownership, lock return value
        nativeMutexLock(rawMonitor);
        return JVMTI_ERROR_NONE;
    }

    static int exit(MonitorID rawMonitor) {
        // TODO check validity, ownership, unlock return value
        nativeMutexUnlock(rawMonitor);
        return JVMTI_ERROR_NONE;

    }

    static int wait(MonitorID rawMonitor, long millis) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    static int notify(MonitorID rawMonitor) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    static int notifyAll(MonitorID rawMonitor) {
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    /*
     * These are already defined as private in com.sun.max.vm.monitor.modal.sync.nat.NativeMutex.
     * At this point we don't want to establish a dependency to that code.
     */

    @C_FUNCTION
    private static native int nativeMutexSize();

    @C_FUNCTION
    private static native void nativeMutexInitialize(Word mutex);

    @C_FUNCTION
    private static native boolean nativeMutexUnlock(Word mutex);

    static {
        new CriticalNativeMethod(JvmtiRawMonitor.class, "nativeMutexUnlock");
    }

    private static native boolean nativeMutexLock(Word mutex);

}
