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
package com.oracle.max.vm.ext.vma.handlers.store.vmlog.h;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.handlers.*;
import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.oracle.max.vm.ext.vma.handlers.store.sync.h.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation equivalent to {@link SyncStoreVMAdviceHandler} but which uses a secondary
 * per-thread {@link VMLog} for storage and per-thread {@link VMAStoreAdapter store adaptors}.
 *
 * Can be built into the boot image or dynamically loaded. However, the {@code VMLog}
 * must be built into the boot image because it uses additional {@link VMThreadLocal}
 * slots that cannot be added dynamically. This is taken care of by {@link VMAJavaRunScheme}
 * using the {@value VMAJavaRunScheme#VMA_LOG_PROPERTY} system property, which must
 * be set on the image build.
 *
 * By default the current system time is logged for each piece of advice, but this
 * can be suppressed by setting {@value TIME_PROPERTY} to {@code false}.
 *
 * The expectation is that a per-thread {@link VMLog}, e.g. {@link VMLogNativeThreadVariableVMA}
 * is used to avoid synchronization. Consistent with that, per-thread instances of the
 * {@link VMAVMLoggerStoreAdapter} are also created so that logs can be flushed
 * all the way to the store without synchronization.
 *
 * Note that object (reference) types cannot be stored in the log owing to potential for
 * the flushing code to provoke a GC. If a log call was on the stack at that time any object types
 * would not be be handled properly as the {@link VMLogger} log methods use {@link Word} types.
 */
public class VMLogStoreVMAdviceHandler extends ObjectStateHandlerAdaptor {

    private static final String ADAPTER_PROPERTY = "max.vma.vmloghandler.maxid";
    private static final Class DEFAULT_ADAPTER = VMAVMLoggerTextStoreAdapter.class;
    private static final Class MAXIDS_ADAPTER = VMAVMLoggerMaxIdTextStoreAdapter.class;

    /**
     * The custom {@link VMLog} used to store VMA advice records.
     * This is expected to be a per-thread log.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private VMLog vmaVMLog;

    private VMAVMLoggerStoreAdapter storeAdaptor;
    private VMAOptions.TimeMode timeMode;

    /**
     * Per-thread {@link VMLog} flusher. No locking necessary as every thread has its own adaptor/store.
     */
    private static class PerThreadVMLogFlusher extends VMLog.Flusher {

        @Override
        public void flushRecord(VmThread vmThread, Record r, int uuid) {
            VMAVMLogger.logger.trace(r);
        }
    }

    private class RemovalTracker implements ObjectStateHandler.DeadObjectHandler {

        @Override
        public void dead(ObjectID id) {
            VMAVMLogger.logger.logDead(getTime(), id);
        }

    }

    private static VMLog.Flusher createFlusher() {
        return new PerThreadVMLogFlusher();
    }

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new VMLogStoreVMAdviceHandler());
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            vmaVMLog = VMAJavaRunScheme.vmaVMLog();
            vmaVMLog.registerCustom(VMAVMLogger.logger, createFlusher());
        } else if (phase == MaxineVM.Phase.RUNNING) {
            if (vmaVMLog == null) {
                // dynamically loaded
                vmaVMLog = VMAJavaRunScheme.vmaVMLog();
                vmaVMLog.registerCustom(VMAVMLogger.logger, createFlusher());
            }
            try {
                Class<?> adapterClass = DEFAULT_ADAPTER;
                String adapterProperty = System.getProperty(ADAPTER_PROPERTY);
                if (adapterProperty != null) {
                    adapterClass = MAXIDS_ADAPTER;
                }
                Constructor<?> cons = adapterClass.getConstructor(ObjectStateHandler.class);
                storeAdaptor = (VMAVMLoggerStoreAdapter) cons.newInstance(state);
                storeAdaptor.initialise(phase);
            } catch (Exception ex) {
                Log.println("failed to load adapter class: " + ex);
                MaxineVM.native_exit(1);
            }
            super.setDeadObjectHandler(new RemovalTracker());
            VMAVMLogger.logger.storeAdaptor = storeAdaptor;
            timeMode = VMAOptions.getTimeMode();
            VMAVMLogger.logger.enable(true);
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            // the VMA log for the main thread is flushed by adviseBeforeThreadTerminating
            // so we just need to flush the external log
            storeAdaptor.initialise(phase);
        }

    }

    private long getTime() {
        return timeMode.getTime();
    }

    @Override
    protected void unseenObject(Object obj) {
        ClassActor ca = ObjectAccess.readClassActor(obj);
        VMAVMLogger.logger.logUnseenObject(getTime(), state.readId(obj), createClassID(ca));
    }

    @Override
    public void adviseBeforeGC() {
        VMAVMLogger.logger.logAdviseBeforeGC(getTime());
    }

    @Override
    public void adviseAfterGC() {
        // We log the GC first, then super will deliver any dead object events
        VMAVMLogger.logger.logAdviseAfterGC(getTime());
        super.adviseAfterGC();
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
        // Need to inform the adapter, no need to log
        // In case of per-thread case, possible create a per-thread adaptor
        VMAVMLoggerStoreAdapter tsa = (VMAVMLoggerStoreAdapter) storeAdaptor.newThread(vmThread);
        tsa.adviseBeforeThreadStarting(getTime());
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
        vmaVMLog.flush(VMLog.FLUSHMODE_FULL, vmThread);
        VMAVMLoggerStoreAdapter threadStoreAdaptor = (VMAVMLoggerStoreAdapter) storeAdaptor.getStoreAdaptorForThread(vmThread.uuid);
        if (threadStoreAdaptor == null) {
            // this is a thread which we didn't see the start of
            threadStoreAdaptor = (VMAVMLoggerStoreAdapter) storeAdaptor.newThread(vmThread);
        }
        threadStoreAdaptor.adviseBeforeThreadTerminating(getTime());
    }

    private ClassID createClassID(ClassActor ca) {
        storeAdaptor.checkDefineClass(ca);
        return ClassID.fromClassActor(ca);
    }

    private FieldID createFieldID(FieldActor fa) {
        storeAdaptor.checkDefineField(fa);
        return FieldID.fromFieldActor(fa);
    }

    private MethodID createMethodID(MethodActor ma) {
        storeAdaptor.checkDefineMethod(ma);
        return MethodID.fromMethodActor(ma);
    }

// START GENERATED CODE
// EDIT AND RUN VMLogStoreVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(int arg1, Throwable arg2, int arg3) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeReturnByThrow(getTime(), arg1, state.readId(arg2), arg3);
    }

    @Override
    public void adviseAfterNew(int arg1, Object arg2) {
        super.adviseAfterNew(arg1, arg2);
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        VMAVMLogger.logger.logAdviseAfterNew(getTime(), arg1, state.readId(arg2), createClassID(ca));
    }

    @Override
    public void adviseAfterNewArray(int arg1, Object arg2, int arg3) {
        super.adviseAfterNewArray(arg1, arg2, arg3);
        ClassActor ca = ObjectAccess.readClassActor(arg2);
        VMAVMLogger.logger.logAdviseAfterNewArray(getTime(), arg1, state.readId(arg2), createClassID(ca), arg3);
        MultiNewArrayHelper.handleMultiArray(this, arg1, arg2);
    }

    @Override
    public void adviseBeforeCheckCast(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3);
        ClassActor ca = UnsafeCast.asClassActor(arg3);
        VMAVMLogger.logger.logAdviseBeforeCheckCast(getTime(), arg1, state.readId(arg2), createClassID(ca));
    }

    @Override
    public void adviseBeforeInstanceOf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3);
        ClassActor ca = UnsafeCast.asClassActor(arg3);
        VMAVMLogger.logger.logAdviseBeforeInstanceOf(getTime(), arg1, state.readId(arg2), createClassID(ca));
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, double arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, Object arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1, state.readId(arg2));
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, long arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(int arg1, float arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeLoad(int arg1, int arg2) {
        super.adviseBeforeLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeLoad(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayLoad(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayLoad(getTime(), arg1, state.readId(arg2), arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, double arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, Object arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2, state.readId(arg3));
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, long arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStore(int arg1, int arg2, float arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, float arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, state.readId(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, state.readId(arg2), arg3, state.readId(arg4));
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, double arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, state.readId(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayStore(int arg1, Object arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, state.readId(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1, int arg2) {
        super.adviseBeforeStackAdjust(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStackAdjust(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, float arg3, float arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeOperation(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, double arg3, double arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeOperation(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(int arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforeOperation(getTime(), arg1, arg2, arg3, arg4);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, float arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeConversion(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, double arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeConversion(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, int arg2, long arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeConversion(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, Object arg3, Object arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        VMAVMLogger.logger.logAdviseBeforeIf(getTime(), arg1, arg2, state.readId(arg3), state.readId(arg4), arg5);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3, int arg4, int arg5) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5);
        VMAVMLogger.logger.logAdviseBeforeIf(getTime(), arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeGoto(int arg1, int arg2) {
        super.adviseBeforeGoto(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeGoto(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1, Object arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1, state.readId(arg2));
    }

    @Override
    public void adviseBeforeReturn(int arg1, long arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1, float arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1, double arg2) {
        super.adviseBeforeReturn(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeReturn(int arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeGetStatic(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeGetStatic(getTime(), arg1, createFieldID(arg3));
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, createFieldID(arg3), state.readId(arg4));
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, createFieldID(arg3), arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, createFieldID(arg3), arg4);
    }

    @Override
    public void adviseBeforePutStatic(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, createFieldID(arg3), arg4);
    }

    @Override
    public void adviseBeforeGetField(int arg1, Object arg2, FieldActor arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeGetField(getTime(), arg1, state.readId(arg2), createFieldID(arg3));
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, float arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, state.readId(arg2), createFieldID(arg3), arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, Object arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, state.readId(arg2), createFieldID(arg3), state.readId(arg4));
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, double arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, state.readId(arg2), createFieldID(arg3), arg4);
    }

    @Override
    public void adviseBeforePutField(int arg1, Object arg2, FieldActor arg3, long arg4) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, state.readId(arg2), createFieldID(arg3), arg4);
    }

    @Override
    public void adviseBeforeInvokeVirtual(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeInvokeVirtual(getTime(), arg1, state.readId(arg2), createMethodID(arg3));
    }

    @Override
    public void adviseBeforeInvokeSpecial(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeInvokeSpecial(getTime(), arg1, state.readId(arg2), createMethodID(arg3));
    }

    @Override
    public void adviseBeforeInvokeStatic(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeInvokeStatic(getTime(), arg1, state.readId(arg2), createMethodID(arg3));
    }

    @Override
    public void adviseBeforeInvokeInterface(int arg1, Object arg2, MethodActor arg3) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeInvokeInterface(getTime(), arg1, state.readId(arg2), createMethodID(arg3));
    }

    @Override
    public void adviseBeforeArrayLength(int arg1, Object arg2, int arg3) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayLength(getTime(), arg1, state.readId(arg2), arg3);
    }

    @Override
    public void adviseBeforeThrow(int arg1, Object arg2) {
        super.adviseBeforeThrow(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeThrow(getTime(), arg1, state.readId(arg2));
    }

    @Override
    public void adviseBeforeMonitorEnter(int arg1, Object arg2) {
        super.adviseBeforeMonitorEnter(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeMonitorEnter(getTime(), arg1, state.readId(arg2));
    }

    @Override
    public void adviseBeforeMonitorExit(int arg1, Object arg2) {
        super.adviseBeforeMonitorExit(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeMonitorExit(getTime(), arg1, state.readId(arg2));
    }

    @Override
    public void adviseAfterLoad(int arg1, int arg2, Object arg3) {
        super.adviseAfterLoad(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseAfterLoad(getTime(), arg1, arg2, state.readId(arg3));
    }

    @Override
    public void adviseAfterArrayLoad(int arg1, Object arg2, int arg3, Object arg4) {
        super.adviseAfterArrayLoad(arg1, arg2, arg3, arg4);
        VMAVMLogger.logger.logAdviseAfterArrayLoad(getTime(), arg1, state.readId(arg2), arg3, state.readId(arg4));
    }

    @Override
    public void adviseAfterMultiNewArray(int arg1, Object arg2, int[] arg3) {
        adviseAfterNewArray(arg1, arg2, arg3[0]);
    }

    @Override
    public void adviseAfterMethodEntry(int arg1, Object arg2, MethodActor arg3) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseAfterMethodEntry(getTime(), arg1, state.readId(arg2), createMethodID(arg3));
    }

}
// END GENERATED CODE

