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

import static com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.RecordType.*;
import static com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.*;

import com.sun.max.vm.*;

/**
 * Counts the number of records of the given types and outputs a summary at the end.
 * Can be used to quantify the basic overhead and to estimate log sizes.
 */
public class CountingAdviceRecordFlusher extends AdviceRecordFlusherAdapter {

    private static long[] counts = new long[RECORD_TYPE_VALUES.length];

    public CountingAdviceRecordFlusher() {
        super("CountingAdviceRecordFlusher");
    }

    @Override
    public void initialise(MaxineVM.Phase phase, ObjectStateHandler state) {
        super.initialise(phase, state);
        if (phase == MaxineVM.Phase.TERMINATING) {
            // output the data
            for (RecordType rt : RECORD_TYPE_VALUES) {
                System.out.printf("%s: %d%n", rt.name(), counts[rt.ordinal()]);
            }
        }
    }

    @Override
    protected void processRecords(RecordBuffer buffer) {
        for (int i = 0; i < buffer.index; i++) {
            counts[buffer.records[i].getRecordType().ordinal()]++;
        }
    }

}
