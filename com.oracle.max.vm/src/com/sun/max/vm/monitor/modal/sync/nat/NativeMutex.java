/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.sync.nat;

import static com.sun.max.vm.MaxineVM.*;

import java.lang.ref.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.runtime.*;

/**
 * Java wrapper for a native mutex.
 *
 * TODO: Decide how to keep the NativeReference instances reachable and how to manage disposal.
 */
public final class NativeMutex extends Mutex {

    static class NativeReference extends WeakReference<NativeMutex> {

        @CONSTANT_WHEN_NOT_ZERO
        private Word mutex = Word.zero();

        NativeReference(NativeMutex m) {
            super(m, refQueue);
        }

        void disposeNative() {
            Memory.deallocate(mutex.asPointer());
        }
    }

    private static ReferenceQueue<NativeMutex> refQueue = new ReferenceQueue<NativeMutex>();

    private NativeReference nativeRef;

    static void initialize() {
        assert vm().phase == MaxineVM.Phase.PRIMORDIAL;
    }

    NativeMutex() {
        nativeRef = new NativeReference(this);
    }

    /**
     * Performs native allocation for this {@code Mutex}.
     */
    @Override
    public Mutex init() {
        if (nativeRef.mutex.isZero()) {
            nativeRef.mutex =  OSMonitor.newMutex();
        }
        return this;
    }

    @Override
    public void cleanup() {
        nativeRef.disposeNative();
    }

    @Override
    public boolean lock() {
        return OSMonitor.nativeMutexLock(nativeRef.mutex);
    }

    /**
     * Causes the current thread to perform an unlock on the mutex.
     *
     * The current thread must own the given mutex when calling this method, otherwise the
     * results are undefined.
     *
     * @return true if an error occurred in native code; false otherwise
     * @see NativeMutex#lock()
     */
    @Override
    public boolean unlock() {
        return OSMonitor.nativeMutexUnlock(nativeRef.mutex);
    }

    /**
     * Returns a pointer to this {@code NativeMutex}'s native data structure.
     * @return
     */
    @INLINE
    public Pointer asPointer() {
        return nativeRef.mutex.asPointer();
    }

    @Override
    public long logId() {
        return nativeRef.mutex.asAddress().toLong();
    }

}
