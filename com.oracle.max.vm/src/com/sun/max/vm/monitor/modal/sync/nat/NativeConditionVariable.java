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
 * Java wrapper for a native condition variable.
 *
 * TODO: Decide how to keep the NativeReference instances reachable and how to manage disposal.
 */
public final class NativeConditionVariable extends ConditionVariable {

    static class NativeReference extends WeakReference<NativeConditionVariable> {

        @CONSTANT_WHEN_NOT_ZERO
        private Word condition = Word.zero();

        NativeReference(NativeConditionVariable c) {
            super(c, refQueue);
        }

        private void disposeNative() {
            Memory.deallocate(condition.asPointer());
        }
    }

    private static ReferenceQueue<NativeConditionVariable> refQueue = new ReferenceQueue<NativeConditionVariable>();
    private NativeReference nativeRef;

    static void initialize() {
        assert vm().phase == MaxineVM.Phase.PRIMORDIAL;
    }

    NativeConditionVariable() {
        nativeRef = new NativeReference(this);
    }

    /**
     * Performs native allocation for this {@code ConditionVariable}.
     */
    @Override
    public ConditionVariable init() {
        if (nativeRef.condition.isZero()) {
            nativeRef.condition = OSMonitor.newCondition();
        }
        return this;
    }

    public boolean requiresAllocation() {
        return nativeRef.condition.isZero();
    }

    @Override
    public boolean threadWait(Mutex mutex, long timeoutMilliSeconds) {
        return OSMonitor.nativeConditionWait(((NativeMutex) mutex).asPointer(), nativeRef.condition, timeoutMilliSeconds);
    }

    @Override
    public boolean threadNotify(boolean all) {
        return OSMonitor.nativeConditionNotify(nativeRef.condition, all);
    }

    /**
     * Returns a pointer to this {@code ConditionVariable}'s native data structure.
     * @return
     */
    @INLINE
    Pointer asPointer() {
        return nativeRef.condition.asPointer();
    }

    @Override
    public long logId() {
        return nativeRef.condition.asAddress().toLong();
    }

}
