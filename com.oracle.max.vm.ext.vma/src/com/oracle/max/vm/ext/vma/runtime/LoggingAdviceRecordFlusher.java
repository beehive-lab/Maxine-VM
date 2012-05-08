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
package com.oracle.max.vm.ext.vma.runtime;

import com.oracle.max.vm.ext.vma.log.VMAdviceHandlerLog.TimeStampGenerator;
import com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link AdviceRecordFlusher} that uses a {@link LoggingVMAdviceHandler} to log the events
 * using a background thread. This produces essentially the same result as {@link SyncLogVMAdviceHandler}, save
 * that the events are batched into per-thread groups. The (latency) overhead on the generating thread is reduced,
 * although it will block when the buffer needs flushing.
 */
public class LoggingAdviceRecordFlusher extends AdviceRecordFlusherAdapter {

    private static class EventThreadNameGenerator extends LoggingVMAdviceHandler.ThreadNameGenerator {
        private VmThread vmThread;

        @INLINE
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

    private LoggingVMAdviceHandler logHandler;
    private EventThreadNameGenerator tng;
    private RecordTimeStampGenerator tsg;

    public LoggingAdviceRecordFlusher() {
        super("LoggingAdviceRecordFlusher");
        logHandler = new LoggingVMAdviceHandler();
        this.tng = new EventThreadNameGenerator();
        logHandler.setThreadNameGenerator(tng);
        this.tsg = new RecordTimeStampGenerator();
    }

    @Override
    public void initialise(MaxineVM.Phase phase, ObjectStateHandler state) {
        super.initialise(phase, state);
        if (phase == MaxineVM.Phase.RUNNING) {
            logHandler.initialise(phase);
            logHandler.getLog().setTimeStampGenerator(tsg);
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            tsg.setEvent(null);
            logHandler.initialise(phase);
        }
    }

    @Override
    protected void processRecords(RecordBuffer buffer) {
        assert buffer.index > 0;
        tng.setThread(buffer.vmThread);
        for (int i = 0; i < buffer.index; i++) {
            final AdviceRecord thisRecord = buffer.records[i];
            tsg.setEvent(thisRecord);
            if (i == 0) {
                // force time reset to this batch
                logHandler.getLog().resetTime();
            }
            RecordType rt = thisRecord.getRecordType();
            switch (rt) {
// START GENERATED CODE
// EDIT AND RUN LoggingAdviceRecordFlusherGenerator.main() TO MODIFY

                case Unseen: {
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.unseenObject(record.value);
                    break;
                }
                case Removal: {
                    logHandler.removal(thisRecord.getPackedValue());
                    break;
                }
                case GC: {
                    assert thisRecord.getAdviceMode() == 1;
                    logHandler.adviseAfterGC();
                    break;
                }
                case ThreadStarting: {
                    assert thisRecord.getAdviceMode() == 0;
                    break;
                }
                case ThreadTerminating: {
                    assert thisRecord.getAdviceMode() == 0;
                    break;
                }
                case New: {
                    assert thisRecord.getAdviceMode() == 1;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseAfterNew(record.value);
                    break;
                }
                case NewArray: {
                    assert thisRecord.getAdviceMode() == 1;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseAfterNewArray(record.value, record.getPackedValue());
                    break;
                }
                case MultiNewArray: {
                    assert false;
                    break;
                }
                case ConstLoadDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case ConstLoadObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case ConstLoadLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case ConstLoadFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConstLoad(record.value);
                    break;
                }
                case Load: {
                    assert thisRecord.getAdviceMode() == 0;
                    logHandler.adviseBeforeLoad(thisRecord.getPackedValue());
                    break;
                }
                case ArrayLoad: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayLoad(record.value, record.getArrayIndex());
                    break;
                }
                case StoreObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(record.getPackedValue(), record.value);
                    break;
                }
                case StoreFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(record.getPackedValue(), record.value);
                    break;
                }
                case StoreDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(record.getPackedValue(), record.value);
                    break;
                }
                case StoreLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeStore(record.getPackedValue(), record.value);
                    break;
                }
                case ArrayStoreObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, record.getArrayIndex(), record.value2);
                    break;
                }
                case ArrayStoreFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectFloatAdviceRecord record = (ObjectFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, record.getArrayIndex(), record.value2);
                    break;
                }
                case ArrayStoreLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectLongAdviceRecord record = (ObjectLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, record.getArrayIndex(), record.value2);
                    break;
                }
                case ArrayStoreDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectDoubleAdviceRecord record = (ObjectDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayStore(record.value, record.getArrayIndex(), record.value2);
                    break;
                }
                case StackAdjust: {
                    assert thisRecord.getAdviceMode() == 0;
                    logHandler.adviseBeforeStackAdjust(thisRecord.getPackedValue());
                    break;
                }
                case OperationDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    DoubleDoubleAdviceRecord record = (DoubleDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeOperation(record.getPackedValue(), record.value, record.value2);
                    break;
                }
                case OperationLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    LongLongAdviceRecord record = (LongLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeOperation(record.getPackedValue(), record.value, record.value2);
                    break;
                }
                case OperationFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    FloatFloatAdviceRecord record = (FloatFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeOperation(record.getPackedValue(), record.value, record.value2);
                    break;
                }
                case ConversionLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConversion(record.getPackedValue(), record.value);
                    break;
                }
                case ConversionFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConversion(record.getPackedValue(), record.value);
                    break;
                }
                case ConversionDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeConversion(record.getPackedValue(), record.value);
                    break;
                }
                case IfInt: {
                    assert thisRecord.getAdviceMode() == 0;
                    LongLongAdviceRecord record = (LongLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeIf(record.getPackedValue(), (int) record.value, (int) record.value2);
                    break;
                }
                case IfObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeIf(record.getPackedValue(), record.value, record.value2);
                    break;
                }
                case Bytecode: {
                    assert thisRecord.getAdviceMode() == 0;
                    logHandler.adviseBeforeBytecode(thisRecord.getPackedValue());
                    break;
                }
                case Return: {
                    assert thisRecord.getAdviceMode() == 0;
                    logHandler.adviseBeforeReturn(thisRecord.getPackedValue());
                    break;
                }
                case ReturnLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    LongAdviceRecord record = (LongAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case ReturnFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    FloatAdviceRecord record = (FloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case ReturnDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    DoubleAdviceRecord record = (DoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case ReturnObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeReturn(record.value);
                    break;
                }
                case GetStatic: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeGetStatic(record.value, record.getPackedValue());
                    break;
                }
                case PutStaticFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectFloatAdviceRecord record = (ObjectFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case PutStaticLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectLongAdviceRecord record = (ObjectLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case PutStaticDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectDoubleAdviceRecord record = (ObjectDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case PutStaticObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutStatic(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case GetField: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeGetField(record.value, record.getPackedValue());
                    break;
                }
                case PutFieldFloat: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectFloatAdviceRecord record = (ObjectFloatAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case PutFieldDouble: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectDoubleAdviceRecord record = (ObjectDoubleAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case PutFieldObject: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case PutFieldLong: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectLongAdviceRecord record = (ObjectLongAdviceRecord) thisRecord;
                    logHandler.adviseBeforePutField(record.value, record.getPackedValue(), record.value2);
                    break;
                }
                case InvokeVirtual: {
                    ObjectMethodAdviceRecord record = (ObjectMethodAdviceRecord) thisRecord;
                    if (thisRecord.getAdviceMode() == 0) {
                        logHandler.adviseBeforeInvokeVirtual(record.value, (MethodActor) record.value2);
                    } else {
                        logHandler.adviseAfterInvokeVirtual(record.value, (MethodActor) record.value2);
                    }
                    break;
                }
                case InvokeSpecial: {
                    ObjectMethodAdviceRecord record = (ObjectMethodAdviceRecord) thisRecord;
                    if (thisRecord.getAdviceMode() == 0) {
                        logHandler.adviseBeforeInvokeSpecial(record.value, (MethodActor) record.value2);
                    } else {
                        logHandler.adviseAfterInvokeSpecial(record.value, (MethodActor) record.value2);
                    }
                    break;
                }
                case InvokeStatic: {
                    ObjectMethodAdviceRecord record = (ObjectMethodAdviceRecord) thisRecord;
                    if (thisRecord.getAdviceMode() == 0) {
                        logHandler.adviseBeforeInvokeStatic(record.value, (MethodActor) record.value2);
                    } else {
                        logHandler.adviseAfterInvokeStatic(record.value, (MethodActor) record.value2);
                    }
                    break;
                }
                case InvokeInterface: {
                    ObjectMethodAdviceRecord record = (ObjectMethodAdviceRecord) thisRecord;
                    if (thisRecord.getAdviceMode() == 0) {
                        logHandler.adviseBeforeInvokeInterface(record.value, (MethodActor) record.value2);
                    } else {
                        logHandler.adviseAfterInvokeInterface(record.value, (MethodActor) record.value2);
                    }
                    break;
                }
                case ArrayLength: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeArrayLength(record.value, record.getArrayIndex());
                    break;
                }
                case Throw: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeThrow(record.value);
                    break;
                }
                case CheckCast: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeCheckCast(record.value, record.value2);
                    break;
                }
                case InstanceOf: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectObjectAdviceRecord record = (ObjectObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeInstanceOf(record.value, record.value2);
                    break;
                }
                case MonitorEnter: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeMonitorEnter(record.value);
                    break;
                }
                case MonitorExit: {
                    assert thisRecord.getAdviceMode() == 0;
                    ObjectAdviceRecord record = (ObjectAdviceRecord) thisRecord;
                    logHandler.adviseBeforeMonitorExit(record.value);
                    break;
                }
                case MethodEntry: {
                    assert thisRecord.getAdviceMode() == 1;
                    ObjectMethodAdviceRecord record = (ObjectMethodAdviceRecord) thisRecord;
                    logHandler.adviseAfterMethodEntry(record.value, (MethodActor) record.value2);
                    break;
                }
// END GENERATED CODE
                default:
                    assert false : "unhandled event type: " + rt;
                    break;

            }
        }
    }
}

