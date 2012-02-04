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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;

/**
 * A {@link VmOperation} that forces all Java threads to a safepoint, and marks them
 * for suspend. When they continue from the safepoint, the {@link VmThreadLocal#SUSPEND} flag
 * will be checked in the native code epilogue and they will actually suspend.
 */
public class SuspendVmOperation extends VmOperation {
    public SuspendVmOperation(VmThread singleThread) {
        super("SuspendThreads", singleThread, Mode.Safepoint);
    }

    @Override
    protected void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        //HACK ALERT should be done elsewhere
        vmThread.suspendMonitor.init();
        // There is no race condition associated with this flag since
        // it cannot be read until all the threads have been thawed.
        SUSPEND.store(vmThread.tla(), Address.fromInt(1));
    }
}
