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

import static com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandler.*;
import com.oracle.max.vm.ext.vma.log.VMAdviceHandlerLog.TimeStampGenerator;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link AdviceRecordFlusher} that uses a {@link LoggingVMAdviceHandler} to log the events
 * using a background thread. This produces essentially the same result as {@link SyncLogVMAdviceHandler}, save
 * that the events are batched into per-thread groups. The (latency) overhead on the generating thread is reduced,
 * although it will block when the buffer needs flushing.
 */
public class LoggingAdviceRecordFlusher extends Thread implements AdviceRecordFlusher {

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

    public static class RecordTimeStampGenerator implements TimeStampGenerator {
        private AdviceRecord record;

        public long getTimeStamp() {
            if (record == null) {
                return System.nanoTime();
            } else {
                return record.time;
            }
        }

        public void setEvent(AdviceRecord record) {
            this.record = record;
        }
    }

    /**
     * Used to communicate with the logging thread. Single-threaded flushing, <code>buffer != null</code> signifies in
     * use.
     */
    private static class LogBuffer {
        RecordBuffer buffer;
        boolean done; // true when flush is complete
    }

    private LoggingVMAdviceHandler logHandler;
    private EventThreadNameGenerator tng;
    private RecordTimeStampGenerator tsg;
    private LogBuffer logBuffer;

    public LoggingAdviceRecordFlusher() {
        logHandler = new LoggingVMAdviceHandler();
        this.tng = new EventThreadNameGenerator();
        logHandler.setThreadNameGenerator(tng);
        this.tsg = new RecordTimeStampGenerator();
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
            RecordBuffer buffer;
            synchronized (logBuffer) {
                while (logBuffer.buffer == null) {
                    try {
                        logBuffer.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                buffer = logBuffer.buffer;
                logRecords(buffer);
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

    private void logRecords(RecordBuffer buffer) {
        assert buffer.index > 0;
        tng.setThread(buffer.vmThread);
        for (int i = 0; i < buffer.index; i++) {
            final AdviceRecord thisRecord = buffer.records[i];
            tsg.setEvent(thisRecord);
            if (i == 0) {
                // force time reset to this batch
                logHandler.getLog().resetTime();
            }
            RecordType rt = getRecordType(thisRecord);
            switch (rt) {
                // BEGIN GENERATED CODE

                // GENERATED -- EDIT AND RUN LoggingAdviceRecordFlusherGenerator.main() TO MODIFY
                case Unseen: {
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.unseenObject(record.value);
                    break;
                }
                case Removal: {
                    logHandler.removal(getPackedValue(thisRecord));
                    break;
                }
                case GC: {
                    assert getAdviceMode(thisRecord) == 1;
                    logHandler.adviseAfterGC();
                    break;
                }
                case ThreadStarting: {
                    assert getAdviceMode(thisRecord) == 0;
                    break;
                }
                case ThreadTerminating: {
                    assert getAdviceMode(thisRecord) == 0;
                    break;
                }
                case ConstLoadLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case ConstLoadObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case ConstLoadFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case ConstLoadDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case IPush: {
                    assert getAdviceMode(thisRecord) == 0;
                    logHandler.adviseBeforeIPush(getPackedValue(thisRecord));
                    break;
                }
                case Load: {
                    assert getAdviceMode(thisRecord) == 0;
                    logHandler.adviseBeforeLoad(getPackedValue(thisRecord));
                    break;
                }
                case ArrayLoad: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayLoad(record.value, getArrayIndex(record));
                    break;
                }
                case StoreLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(getPackedValue(record), record.value);
                    break;
                }
                case StoreFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(getPackedValue(record), record.value);
                    break;
                }
                case StoreDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(getPackedValue(record), record.value);
                    break;
                }
                case StoreObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(getPackedValue(record), record.value);
                    break;
                }
                case ArrayStoreFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectFloatAdviceRecord record = (ObjectFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, getArrayIndex(record), record.value2);
                    break;
                }
                case ArrayStoreLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectLongAdviceRecord record = (ObjectLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, getArrayIndex(record), record.value2);
                    break;
                }
                case ArrayStoreDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectDoubleAdviceRecord record = (ObjectDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, getArrayIndex(record), record.value2);
                    break;
                }
                case ArrayStoreObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, getArrayIndex(record), record.value2);
                    break;
                }
                case StackAdjust: {
                    assert getAdviceMode(thisRecord) == 0;
                    logHandler.adviseBeforeStackAdjust(getPackedValue(thisRecord));
                    break;
                }
                case OperationLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongLongAdviceRecord record = (LongLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeOperation(getPackedValue(record), record.value, record.value2);
                    break;
                }
                case OperationFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    FloatFloatAdviceRecord record = (FloatFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeOperation(getPackedValue(record), record.value, record.value2);
                    break;
                }
                case OperationDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    DoubleDoubleAdviceRecord record = (DoubleDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeOperation(getPackedValue(record), record.value, record.value2);
                    break;
                }
                case IInc: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongLongAdviceRecord record = (LongLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeIInc(getPackedValue(record), (int) record.value, (int) record.value2);
                    break;
                }
                case ConversionLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConversion(getPackedValue(record), record.value);
                    break;
                }
                case ConversionFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConversion(getPackedValue(record), record.value);
                    break;
                }
                case ConversionDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConversion(getPackedValue(record), record.value);
                    break;
                }
                case IfInt: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongLongAdviceRecord record = (LongLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeIf(getPackedValue(record), (int) record.value, (int) record.value2);
                    break;
                }
                case IfObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeIf(getPackedValue(record), record.value, record.value2);
                    break;
                }
                case ReturnObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case ReturnLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case ReturnFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case ReturnDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case Return: {
                    assert getAdviceMode(thisRecord) == 0;
                    logHandler.adviseBeforeReturn(getPackedValue(thisRecord));
                    break;
                }
                case GetStatic: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeGetStatic(record.value, getPackedValue(record));
                    break;
                }
                case PutStaticDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectDoubleAdviceRecord record = (ObjectDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case PutStaticLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectLongAdviceRecord record = (ObjectLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case PutStaticFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectFloatAdviceRecord record = (ObjectFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case PutStaticObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case GetField: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeGetField(record.value, getPackedValue(record));
                    break;
                }
                case PutFieldDouble: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectDoubleAdviceRecord record = (ObjectDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case PutFieldLong: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectLongAdviceRecord record = (ObjectLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case PutFieldFloat: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectFloatAdviceRecord record = (ObjectFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case PutFieldObject: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, getPackedValue(record), record.value2);
                    break;
                }
                case InvokeVirtual: {
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    if (getAdviceMode(thisRecord) == 0) {
                        logHandler.adviseBeforeInvokeVirtual(record.value, getPackedValue(record));
                    } else {
                        logHandler.adviseAfterInvokeVirtual(record.value, getPackedValue(record));
                    }
                    break;
                }
                case InvokeSpecial: {
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    if (getAdviceMode(thisRecord) == 0) {
                        logHandler.adviseBeforeInvokeSpecial(record.value, getPackedValue(record));
                    } else {
                        logHandler.adviseAfterInvokeSpecial(record.value, getPackedValue(record));
                    }
                    break;
                }
                case InvokeStatic: {
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    if (getAdviceMode(thisRecord) == 0) {
                        logHandler.adviseBeforeInvokeStatic(record.value, getPackedValue(record));
                    } else {
                        logHandler.adviseAfterInvokeStatic(record.value, getPackedValue(record));
                    }
                    break;
                }
                case InvokeInterface: {
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    if (getAdviceMode(thisRecord) == 0) {
                        logHandler.adviseBeforeInvokeInterface(record.value, getPackedValue(record));
                    } else {
                        logHandler.adviseAfterInvokeInterface(record.value, getPackedValue(record));
                    }
                    break;
                }
                case ArrayLength: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayLength(record.value, getArrayIndex(record));
                    break;
                }
                case Throw: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeThrow(record.value);
                    break;
                }
                case CheckCast: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeCheckCast(record.value, record.value2);
                    break;
                }
                case InstanceOf: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeInstanceOf(record.value, record.value2);
                    break;
                }
                case MonitorEnter: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeMonitorEnter(record.value);
                    break;
                }
                case MonitorExit: {
                    assert getAdviceMode(thisRecord) == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeMonitorExit(record.value);
                    break;
                }
                case Bytecode: {
                    assert getAdviceMode(thisRecord) == 0;
                    logHandler.adviseBeforeBytecode(getPackedValue(thisRecord));
                    break;
                }
                case New: {
                    assert getAdviceMode(thisRecord) == 1;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseAfterNew(record.value);
                    break;
                }
                case NewArray: {
                    assert getAdviceMode(thisRecord) == 1;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseAfterNewArray(record.value, getPackedValue(record));
                    break;
                }
                case MultiNewArray: {
                    assert false;
                    break;
                }

                // END GENERATED CODE
                default:
                    assert false : "unhandled event type: " + rt;
                    break;

            }
            // it would be better to cache this with the thread
            thisRecord.owner = null;
        }
    }
}

