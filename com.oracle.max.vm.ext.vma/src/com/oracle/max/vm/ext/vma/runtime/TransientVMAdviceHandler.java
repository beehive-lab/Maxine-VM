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

import static com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.*;
import static com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.RecordType.*;
import static com.oracle.max.vm.ext.vma.runtime.AdviceRecordFlusher.*;

import com.oracle.max.vm.ext.vma.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link VMAdviceHandler} that stores the advice data as (Java) objects in a per-thread
 * buffer, and delegates to a separate handler to process the data when the buffer is full.
 *
 * This implementation does no allocation in the user heap. New threads are allocated a record buffer from the immortal
 * heap. When advice is handled, an advice record of the required type is taken from a preallocated list (created at
 * image build time) and added to that thread's buffer. If the list is empty, the buffer is flushed
 * and the records reclaimed.
 *
 * Buffer flushing is currently single-threaded through the {@link #LogBuffer} class. Any thread that needs to flush its
 * buffer will block until the logger thread is done.
 */

public class TransientVMAdviceHandler extends ObjectStateHandlerAdaptor {
    private static final String RECORD_BUF_SIZE_PROPERTY = "max.vma.recordbuf.size";
    private static final int DEFAULT_RECORD_BUF_SIZE = 16 * 1024;
    private static final int DEFAULT_RECORD_QUOTA_SIZE = 1024;


    /**
     * Pre-allocated in boot image memory.
     */
    private static AdviceRecord[][] recordLists;

    static {
        recordLists = new AdviceRecord[RECORD_TYPE_VALUES.length][];
        for (RecordType rt : RECORD_TYPE_VALUES) {
            AdviceRecord[] recordList = new AdviceRecord[DEFAULT_RECORD_QUOTA_SIZE];
            recordLists[rt.ordinal()] = recordList;
            for (int i = 0; i < DEFAULT_RECORD_QUOTA_SIZE; i++) {
                recordList[i] = rt.newAdviceRecord();
            }
        }
    }

    private ThreadRecordBuffer tb;
    private AdviceRecordFlusher recordFlusher;
    private volatile boolean finalising;

    static class ASyncRemovalTracker extends ObjectStateHandler.RemovalTracker {
        TransientVMAdviceHandler lta;

        ASyncRemovalTracker(TransientVMAdviceHandler lta) {
            this.lta = lta;
        }

        @Override
        public void removed(long id) {
            LongAdviceRecord record = (LongAdviceRecord) lta.getCheckFlush(Removal, 1);
            if (record != null) {
                record.value = id;
            }
        }
    }

    /**
     * A {@link ThreadLocal} that holds the record buffer for a thread.
     */
    private static class ThreadRecordBuffer extends ThreadLocal<RecordBuffer> {
        @Override
        public RecordBuffer initialValue() {
            RecordBuffer result = null;
            try {
                Heap.enableImmortalMemoryAllocation();
                result = new RecordBuffer(new AdviceRecord[DEFAULT_RECORD_BUF_SIZE]);
                assert result != null;
            } finally {
                Heap.disableImmortalMemoryAllocation();
            }
            return result;
        }
    }

    private AdviceRecord getRecord(RecordType et) {
        AdviceRecord[] list = recordLists[et.ordinal()];
        int count = 0;
        while (count < 10) {
            synchronized (list) {
                for (AdviceRecord record : list) {
                    if (record.thread == null) {
                        record.thread = VmThread.current();
                        return record;
                    }
                }
            }
            // Out of records of requested type, flush and try again
            recordFlusher.flushBuffer(tb.get());
            count++;
        }
        assert false;
        return null;
    }

    @Override
    public void initialise(ObjectStateHandler state) {
        super.initialise(state);
        try {
            // TODO is this really necessary?
            Heap.enableImmortalMemoryAllocation();
            tb = new ThreadRecordBuffer();
            super.setRemovalTracker(new ASyncRemovalTracker(this));
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
        recordFlusher = AdviceRecordFlusherFactory.create();
        recordFlusher.initialise(state);
    }

    @Override
    public void finalise() {
        finalising = true; // this will prevent daemon threads from logging any more records.
        // This is called in the main thread which, clearly, has not yet called {@link #threadTerminating}.
        adviseBeforeThreadTerminating(VmThread.current());
        recordFlusher.finalise();
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
        final RecordBuffer buffer = tb.get();
        if (buffer != null) {
            recordFlusher.flushBuffer(buffer);
        }
    }

    private AdviceRecord getCheckFlush(RecordType rt, int adviceMode) {
        if (finalising) {
            return null;
        }
        final RecordBuffer buffer = tb.get();
        if (buffer.index >= buffer.records.length) {
            recordFlusher.flushBuffer(buffer);
        }
        final AdviceRecord record = getRecord(rt);
        record.time = System.nanoTime();
        record.setCodeAndMode(rt, adviceMode);
        buffer.records[buffer.index++] = record;
        return record;
    }

    private AdviceRecord storeRecord(RecordType rt, int adviceMode) {
        return getCheckFlush(rt, adviceMode);
    }

    private ObjectAdviceRecord storeRecord(RecordType rt, int adviceMode, Object obj) {
        ObjectAdviceRecord record = (ObjectAdviceRecord) getCheckFlush(rt, adviceMode);
        if (record != null) {
            record.value = obj;
        }
        return record;
    }

    private ObjectAdviceRecord storeRecord(RecordType rt, int adviceMode, Object obj, int arg) {
        ObjectAdviceRecord record = (ObjectAdviceRecord) getCheckFlush(rt, adviceMode);
        if (record != null) {
            record.setValue(arg);
            record.value = obj;
        }
        return record;
    }

    private AdviceRecord storeRecord(RecordType rt, int adviceMode, int arg) {
        AdviceRecord record = getCheckFlush(rt, adviceMode);
        if (record != null) {
            record.setValue(arg);
        }
        return record;
    }

    private AdviceRecord storeRecord(RecordType rt, int adviceMode, long arg) {
        AdviceRecord record = getCheckFlush(rt, adviceMode);
        if (record != null) {
            record.setValue((int) arg);
        }
        return record;
    }

    @Override
    public void adviseBeforeGC() {
    }

    @Override
    public void adviseAfterGC() {
        storeRecord(GC, AdviceMode.AFTER.ordinal());
        super.adviseAfterGC();
    }

    @Override
    protected void handleUnseen(Object obj) {
        storeRecord(Unseen, 0, obj);
    }

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        adviseAfterNewArray(arg1, arg2[0]);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(double arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeRecord(ConstLoadDouble, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(long arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeRecord(ConstLoadLong, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(float arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeRecord(ConstLoadFloat, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(Object arg1) {
        super.adviseBeforeConstLoad(arg1);
        storeRecord(ConstLoadObject, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIPush(int arg1) {
        super.adviseBeforeIPush(arg1);
        storeRecord(IPush, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeLoad(int arg1) {
        super.adviseBeforeLoad(arg1);
        storeRecord(Load, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2) {
        super.adviseBeforeArrayLoad(arg1, arg2);
        storeRecord(ArrayLoad, 0, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(int arg1, float arg2) {
        super.adviseBeforeStore(arg1, arg2);
        FloatAdviceRecord r = (FloatAdviceRecord) storeRecord(StoreFloat, 0, arg1);
        if (r != null) {
            r.value = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(int arg1, double arg2) {
        super.adviseBeforeStore(arg1, arg2);
        DoubleAdviceRecord r = (DoubleAdviceRecord) storeRecord(StoreDouble, 0, arg1);
        if (r != null) {
            r.value = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(int arg1, Object arg2) {
        super.adviseBeforeStore(arg1, arg2);
        ObjectAdviceRecord r = (ObjectAdviceRecord) storeRecord(StoreObject, 0, arg1);
        if (r != null) {
            r.value = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(int arg1, long arg2) {
        super.adviseBeforeStore(arg1, arg2);
        LongAdviceRecord r = (LongAdviceRecord) storeRecord(StoreLong, 0, arg1);
        if (r != null) {
            r.value = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ObjectFloatAdviceRecord r = (ObjectFloatAdviceRecord) storeRecord(ArrayStoreFloat, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ObjectDoubleAdviceRecord r = (ObjectDoubleAdviceRecord) storeRecord(ArrayStoreDouble, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ObjectObjectAdviceRecord r = (ObjectObjectAdviceRecord) storeRecord(ArrayStoreObject, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ObjectLongAdviceRecord r = (ObjectLongAdviceRecord) storeRecord(ArrayStoreLong, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStackAdjust(int arg1) {
        super.adviseBeforeStackAdjust(arg1);
        storeRecord(StackAdjust, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        FloatFloatAdviceRecord r = (FloatFloatAdviceRecord) storeRecord(OperationFloat, 0, arg1);
        if (r != null) {
            r.value = arg2;
            r.value2 = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        DoubleDoubleAdviceRecord r = (DoubleDoubleAdviceRecord) storeRecord(OperationDouble, 0, arg1);
        if (r != null) {
            r.value = arg2;
            r.value2 = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        LongLongAdviceRecord r = (LongLongAdviceRecord) storeRecord(OperationLong, 0, arg1);
        if (r != null) {
            r.value = arg2;
            r.value2 = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIInc(int arg1, int arg2, int arg3) {
        super.adviseBeforeIInc(arg1, arg2, arg3);
        LongLongAdviceRecord r = (LongLongAdviceRecord) storeRecord(IInc, 0, arg1);
        if (r != null) {
            r.value = arg2;
            r.value2 = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(int arg1, long arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        storeRecord(ConversionLong, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(int arg1, float arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        storeRecord(ConversionFloat, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(int arg1, double arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        storeRecord(ConversionDouble, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        LongLongAdviceRecord r = (LongLongAdviceRecord) storeRecord(IfInt, 0, arg1);
        if (r != null) {
            r.value = arg2;
            r.value2 = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        ObjectObjectAdviceRecord r = (ObjectObjectAdviceRecord) storeRecord(IfObject, 0, arg1);
        if (r != null) {
            r.value = arg2;
            r.value2 = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(Object arg1) {
        super.adviseBeforeReturn(arg1);
        storeRecord(ReturnObject, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(long arg1) {
        super.adviseBeforeReturn(arg1);
        storeRecord(ReturnLong, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(float arg1) {
        super.adviseBeforeReturn(arg1);
        storeRecord(ReturnFloat, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(double arg1) {
        super.adviseBeforeReturn(arg1);
        storeRecord(ReturnDouble, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn() {
        super.adviseBeforeReturn();
        storeRecord(Return, 0);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2) {
        super.adviseBeforeGetStatic(arg1, arg2);
        storeRecord(GetStatic, 0, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectLongAdviceRecord r = (ObjectLongAdviceRecord) storeRecord(PutStaticLong, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectFloatAdviceRecord r = (ObjectFloatAdviceRecord) storeRecord(PutStaticFloat, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectObjectAdviceRecord r = (ObjectObjectAdviceRecord) storeRecord(PutStaticObject, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectDoubleAdviceRecord r = (ObjectDoubleAdviceRecord) storeRecord(PutStaticDouble, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2) {
        super.adviseBeforeGetField(arg1, arg2);
        storeRecord(GetField, 0, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectLongAdviceRecord r = (ObjectLongAdviceRecord) storeRecord(PutFieldLong, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectFloatAdviceRecord r = (ObjectFloatAdviceRecord) storeRecord(PutFieldFloat, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectObjectAdviceRecord r = (ObjectObjectAdviceRecord) storeRecord(PutFieldObject, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectDoubleAdviceRecord r = (ObjectDoubleAdviceRecord) storeRecord(PutFieldDouble, 0, arg1, arg2);
        if (r != null) {
            r.value2 = arg3;
        }

    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeVirtual(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeVirtual, 0, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeSpecial(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeSpecial, 0, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeStatic(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeStatic, 0, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeInterface(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeInterface, 0, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLength(Object arg1, int arg2) {
        super.adviseBeforeArrayLength(arg1, arg2);
        storeRecord(ArrayLength, 0, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThrow(Object arg1) {
        super.adviseBeforeThrow(arg1);
        storeRecord(Throw, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeCheckCast(Object arg1, Object arg2) {
        super.adviseBeforeCheckCast(arg1, arg2);
        ObjectObjectAdviceRecord r = (ObjectObjectAdviceRecord) storeRecord(CheckCast, 0, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        super.adviseBeforeInstanceOf(arg1, arg2);
        ObjectObjectAdviceRecord r = (ObjectObjectAdviceRecord) storeRecord(InstanceOf, 0, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorEnter(Object arg1) {
        super.adviseBeforeMonitorEnter(arg1);
        storeRecord(MonitorEnter, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorExit(Object arg1) {
        super.adviseBeforeMonitorExit(arg1);
        storeRecord(MonitorExit, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeBytecode(int arg1) {
        super.adviseBeforeBytecode(arg1);
        storeRecord(Bytecode, 0, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeVirtual(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeVirtual, 1, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeSpecial(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeSpecial, 1, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeStatic(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeStatic, 1, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeInterface(arg1, arg2);
        ObjectMethodAdviceRecord r = (ObjectMethodAdviceRecord) storeRecord(InvokeInterface, 1, arg1);
        if (r != null) {
            r.value2 = arg2;
        }
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(Object arg1) {
        super.adviseAfterNew(arg1);
        storeRecord(New, 1, arg1);
    }

    // GENERATED -- EDIT AND RUN TransientVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        super.adviseAfterNewArray(arg1, arg2);
        storeRecord(NewArray, 1, arg1, arg2);
    }


}
