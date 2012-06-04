/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import com.sun.max.unsafe.*;

/**
 * Thread local storage for agents.
 *
 */
public class JVMTIThreadLocalStorage {

    private static class NativeData {
        Pointer data;
    }

    private static class NativeDataThreadLocal extends ThreadLocal<NativeData> {
        @Override
        public NativeData initialValue() {
            return new NativeData();
        }
    }

    private static final NativeDataThreadLocal ndtl = new NativeDataThreadLocal();

    static int setThreadLocalStorage(Thread thread, Pointer data) {
        thread = JVMTIThreadFunctions.checkThread(thread);
        if (thread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        NativeData nd = ndtl.get();
        nd.data = data;
        return JVMTI_ERROR_NONE;
    }

    static int getThreadLocalStorage(Thread thread, Pointer dataPtr) {
        thread = JVMTIThreadFunctions.checkThread(thread);
        if (thread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        NativeData nd = ndtl.get();
        dataPtr.setWord(nd.data);
        return JVMTI_ERROR_NONE;
    }


}
