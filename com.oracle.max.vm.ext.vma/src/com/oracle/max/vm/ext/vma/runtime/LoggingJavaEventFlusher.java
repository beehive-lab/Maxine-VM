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

import static com.oracle.max.vm.ext.vma.runtime.JavaEventVMAdviceHandler.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.log.VMAdviceHandlerLog.TimeStampGenerator;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.oracle.max.vm.ext.vma.runtime.JavaVMAdviceHandlerEvents.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link JavaEventFlusher} that uses a {@link LoggingVMAdviceHandler} to log the events
 * using a background thread. This produces essentially the same result as {@link SyncLogVMAdviceHandler}, save
 * that the events are batched into per-thread groups. The (latency) overhead on the generating thread is reduced,
 * although it will block when the buffer needs flushing.
 */
public class LoggingJavaEventFlusher extends Thread implements JavaEventFlusher {

    private static class EventThreadNameGenerator extends LoggingVMAdviceHandler.ThreadNameGenerator {
        private VmThread vmThread;

        @INLINE(override = true)
        @Override
        String getThreadName() {
            return vmThread.getName();
        }

        void setThread(VmThread vmThread) {
            this.vmThread = vmThread;
        }
    }

    public static class EventTimeStampGenerator implements TimeStampGenerator {
        private Event event;

        public long getTimeStamp() {
            if (event == null) {
                return System.nanoTime();
            } else {
                return event.time;
            }
        }

        public void setEvent(Event event) {
            this.event = event;
        }
    }

    /**
     * Used to communicate with the logging thread. Single-threaded flushing, <code>buffer != null</code> signifies in
     * use.
     */
    private static class LogBuffer {
        EventBuffer buffer;
        boolean done; // true when flush is complete
    }

    private LoggingVMAdviceHandler logHandler;
    private EventThreadNameGenerator tng;
    private EventTimeStampGenerator tsg;
    private LogBuffer logBuffer;

    public LoggingJavaEventFlusher() {
        logHandler = new LoggingVMAdviceHandler();
        this.tng = new EventThreadNameGenerator();
        logHandler.setThreadNameGenerator(tng);
        this.tsg = new EventTimeStampGenerator();
        setName("LoggingJavaEventFlusher");
        setDaemon(true);
        logBuffer = new LogBuffer();
        start();
    }

    public void initialise(ObjectStateHandler state) {
        logHandler.initialise(state);
        logHandler.getLog().setTimeStampGenerator(tsg);
    }

    public void finalise() {
        tsg.setEvent(null);
        logHandler.finalise();
    }

    public void flushBuffer(EventBuffer buffer) {
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
            logBuffer.done = false;
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
            EventBuffer buffer;
            synchronized (logBuffer) {
                while (logBuffer.buffer == null) {
                    try {
                        logBuffer.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                buffer = logBuffer.buffer;
                logEvents(buffer);
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

    private void logEvents(EventBuffer buffer) {
        assert buffer.index > 0;
        tng.setThread(buffer.vmThread);
        for (int i = 0; i < buffer.index; i++) {
            final Event thisEvent = buffer.events[i];
            tsg.setEvent(thisEvent);
            if (i == 0) {
                // force time reset to this batch
                logHandler.getLog().resetTime();
            }
            EventType evt = getEventType(thisEvent);
            switch (evt) {
                // BEGIN GENERATED CODE
                // GENERATED -- EDIT AND RUN LoggingJavaEventFlusherGenerator.main() TO MODIFY
                case NewArray: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.adviseAfterNewArray(event.obj, getArrayIndex(event));
                    break;
                }
                case NewObject: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.adviseAfterNew(event.obj);
                    break;
                }
                case InvokeSpecial: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.adviseAfterInvokeSpecial(event.obj);
                    break;
                }
                case GetStatic: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.adviseBeforeGetStatic(event.obj, getPackedValue(event), 0);
                    break;
                }
                case PutStaticObject: {
                    ObjectObjectValueEvent event = (ObjectObjectValueEvent) thisEvent;
                    logHandler.adviseBeforePutStatic(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case PutStaticLong: {
                    ObjectLongValueEvent event = (ObjectLongValueEvent) thisEvent;
                    logHandler.adviseBeforePutStatic(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case PutStaticFloat: {
                    ObjectFloatValueEvent event = (ObjectFloatValueEvent) thisEvent;
                    logHandler.adviseBeforePutStatic(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case PutStaticDouble: {
                    ObjectDoubleValueEvent event = (ObjectDoubleValueEvent) thisEvent;
                    logHandler.adviseBeforePutStatic(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case GetField: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.adviseBeforeGetField(event.obj, getPackedValue(event), 0);
                    break;
                }
                case PutFieldObject: {
                    ObjectObjectValueEvent event = (ObjectObjectValueEvent) thisEvent;
                    logHandler.adviseBeforePutField(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case PutFieldLong: {
                    ObjectLongValueEvent event = (ObjectLongValueEvent) thisEvent;
                    logHandler.adviseBeforePutField(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case PutFieldFloat: {
                    ObjectFloatValueEvent event = (ObjectFloatValueEvent) thisEvent;
                    logHandler.adviseBeforePutField(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case PutFieldDouble: {
                    ObjectDoubleValueEvent event = (ObjectDoubleValueEvent) thisEvent;
                    logHandler.adviseBeforePutField(event.obj, getPackedValue(event), event.value);
                    break;
                }
                case ArrayLoad: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.adviseBeforeArrayLoad(event.obj, getArrayIndex(event), 0);
                    break;
                }
                case ArrayStoreObject: {
                    ArrayObjectValueEvent event = (ArrayObjectValueEvent) thisEvent;
                    logHandler.adviseBeforeArrayStore(event.obj, getArrayIndex(event), event.value);
                    break;
                }
                case ArrayStoreLong: {
                    ArrayLongValueEvent event = (ArrayLongValueEvent) thisEvent;
                    logHandler.adviseBeforeArrayStore(event.obj, getArrayIndex(event), event.value);
                    break;
                }
                case ArrayStoreFloat: {
                    ArrayFloatValueEvent event = (ArrayFloatValueEvent) thisEvent;
                    logHandler.adviseBeforeArrayStore(event.obj, getArrayIndex(event), event.value);
                    break;
                }
                case ArrayStoreDouble: {
                    ArrayDoubleValueEvent event = (ArrayDoubleValueEvent) thisEvent;
                    logHandler.adviseBeforeArrayStore(event.obj, getArrayIndex(event), event.value);
                    break;
                }
                case UnseenObject: {
                    ObjectEvent event = (ObjectEvent) thisEvent;
                    logHandler.unseenObject(event.obj);
                    break;
                }
                case GC: {
                    logHandler.adviseGC(AdviceMode.AFTER);
                    break;
                }
                case Removal: {
                    logHandler.removal(getPackedValue(thisEvent));
                    break;
                }
                // END GENERATED CODE
                default:
                    assert false : "unhandled event type: " + evt;
                    break;

            }
            // it would be better to cache this with the thread
            thisEvent.owner = null;
        }
    }
}

