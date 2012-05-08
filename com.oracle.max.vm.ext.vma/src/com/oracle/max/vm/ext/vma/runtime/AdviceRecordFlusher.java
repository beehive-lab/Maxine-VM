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
package com.oracle.max.vm.ext.vma.runtime;

import com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * The interface through which {@link TransientVMAdviceHandler} flushes the per-thread event buffer.
 */

public interface AdviceRecordFlusher {

    /**
     * Thread-specific buffer of events.
     */
    public static class RecordBuffer {
        VmThread vmThread;
        AdviceRecord[] records;
        /**
         * Valid records are {@code x >= 0 && x < index}.
         */
        int index;

        public RecordBuffer(AdviceRecord[] records) {
            this.records = records;
            this.vmThread = VmThread.current();
        }
    }

    /**
     * Flush the buffer.
     * @param buffer
     */
    void flushBuffer(RecordBuffer buffer);

    void initialise(MaxineVM.Phase phase, ObjectStateHandler state);

}
