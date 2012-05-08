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

import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.vm.*;

/**
 * Handles the basic communication between the {@link TransientVMAdviceHandler} and the {@link AdviceRecordFlusher}.
 * It uses a single thread to handle the flushing and takes care of marking the {@link AdviceRecord} as free
 * once the buffer is processed. By default the thread is started by the {@link #initialise(MaxineVM.Phase, ObjectStateHandler)} method.
 */
public abstract class AdviceRecordFlusherAdapter extends Thread implements AdviceRecordFlusher {

    /**
     * Used to communicate with the logging thread. Single-threaded flushing, <code>buffer != null</code> signifies
     * that the buffer is in use.
     */
    private static class LogBuffer {
        RecordBuffer buffer;
    }

    private LogBuffer logBuffer;

    protected AdviceRecordFlusherAdapter(String name) {
        logBuffer = new LogBuffer();
        setName(name);
        setDaemon(true);
    }

    /**
     * Concrete subclass must implement this method.
     * @param buffer
     */
    protected abstract void processRecords(RecordBuffer buffer);

    @Override
    public void initialise(MaxineVM.Phase phase, ObjectStateHandler objectStateHandler) {
        if (phase == MaxineVM.Phase.RUNNING) {
            start();
        }
    }

    @Override
    public void flushBuffer(RecordBuffer buffer) {
        if (buffer.index == 0) {
            return;
        }
        synchronized (logBuffer) {
            // wait for any existing flush to complete
            while (logBuffer.buffer != null) {
                try {
                    logBuffer.wait();
                } catch (InterruptedException ex) {
                }
            }
            // grab the buffer and wake up logger (may also wake up other callers of this method)
            assert buffer.index > 0;
            logBuffer.buffer = buffer;
            logBuffer.notifyAll();
        }
        synchronized (buffer) {
            // wait for flush to complete
            while (buffer.index > 0) {
                try {
                    buffer.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    @Override
    public void run() {
        VMAJavaRunScheme.disableAdvising();
        while (true) {
            RecordBuffer buffer;
            synchronized (logBuffer) {
                while (logBuffer.buffer == null) {
                    try {
                        logBuffer.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                buffer = logBuffer.buffer;
                processRecords(buffer);
                // free up the records
                for (int i = 0; i < buffer.index; i++) {
                    // it would be better to cache this with the thread
                    buffer.records[i].thread = null;
                }
                // free up the log buffer, notify any waiters
                logBuffer.buffer = null;
                logBuffer.notifyAll();
            }
            synchronized (buffer) {
                // signify completion
                buffer.index = 0;
                buffer.notify(); // only one possible waiter
            }
        }
    }


}
