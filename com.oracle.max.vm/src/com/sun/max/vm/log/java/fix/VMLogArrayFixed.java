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
package com.sun.max.vm.log.java.fix;

import com.sun.max.vm.*;
import com.sun.max.vm.log.java.*;

/**
 * Simple space inefficient implementation.
 * Allocates {@link Record records} large enough to hold the maximum number of arguments.
 * All records are considered in use, i.e., not FREE, even if they are not currently filled in (early startup).
 */
public class VMLogArrayFixed extends VMLogArray {

    private int myIdAtLastFlush;

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = new Record8();
            }
        }
    }

    @Override
    protected Record getRecord(int argCount) {
        int myId = getUniqueId();
        if (myId - myIdAtLastFlush >= logEntries) {
            // buffer is about to overflow
            flush(myId);
        }
        Record r = buffer[myId % logEntries];
        return r;
    }

    private synchronized void flush(int myId) {
        try {
            flusher.start();
            for (int id = myIdAtLastFlush; id < myId; id++) {
                flusher.flushRecord(buffer[id % logEntries]);
            }
            myIdAtLastFlush = myId;
        } finally {
            flusher.end();
        }
    }

    @Override
    public void flushLog() {
        // The assumption is that we can safely read "nextId" without
        // interference from concurrent activity.
        flush(nextId);
    }

}
