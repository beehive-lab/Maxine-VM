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
package com.oracle.max.vm.ext.vma.handlers.log.vmlog.h;

import java.util.concurrent.locks.*;

import com.oracle.max.vm.ext.vma.handlers.*;
import com.oracle.max.vm.ext.vma.handlers.log.*;
import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation that uses a secondary {@link VMLog} for storage.
 *
 * Can be built into the boot image or dynamically loaded. However, the {@code VMLog}
 * must be built into the boot image because it uses additional {@link VMThreadLocal}
 * slots that cannot be added dynamically. This is taken care of by {@link VMAJavaRunScheme}
 * using the {@value VMAJavaRunScheme#VMA_LOG_PROPERTY} system property, which must
 * be set on the image build.
 */
public class VMLogVMAdviceHandler extends ObjectStateHandlerAdaptor {

    private static final String TIME_PROPERTY = "max.vma.logtime";

    /**
     * The custom {@link VMLog} used to store VMA advice records.
     * This is a per-thread log.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private VMLog vmaVMLog;

    private static boolean logTime = true;

    private static class VMLogFlusher extends VMLog.Flusher {
        private Lock lock = new ReentrantLock();
        boolean firstRecord;

        @Override
        public void start(VmThread vmThread) {
            lock.lock();
            firstRecord = true;
        }

        @Override
        public void flushRecord(VmThread vmThread, Record r, int uuid) {
            if (firstRecord) {
                // Indicate the start of a new batch of records for the current thread
                VMAVMLogger.logger.timeStamp = r.getLongArg(1);
                VMAVMLogger.handler.getLog().resetTime();
                firstRecord = false;
            }
            VMAVMLogger.logger.trace(r);
        }

        @Override
        public void end(VmThread vmThread) {
            lock.unlock();
        }
    }

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new VMLogVMAdviceHandler());
        ObjectStateHandlerAdaptor.forceCompile();
    }


    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            vmaVMLog = VMAJavaRunScheme.vmaVMLog();
            vmaVMLog.registerCustom(VMAVMLogger.logger, new VMLogFlusher());
        } else if (phase == MaxineVM.Phase.RUNNING) {
            if (vmaVMLog == null) {
                // dynamically loaded
                vmaVMLog = VMAJavaRunScheme.vmaVMLog();
                vmaVMLog.registerCustom(VMAVMLogger.logger, new VMLogFlusher());
            }
            VMAdviceHandlerLogAdapter handler = new VMAdviceHandlerLogAdapter();
            handler.initialise(phase);
            super.setRemovalTracker(handler.getRemovalTracker(state));
            VMAVMLogger.VMAVMLoggerImpl.setLogHandler(handler);
            handler.getLog().setTimeStampGenerator(VMAVMLogger.logger);
            VMAVMLogger.logger.enable(true);
            VMAVMLogger.logger.timeStamp = getTime();
            String ltp = System.getProperty(TIME_PROPERTY);
            if (ltp != null) {
                logTime = ltp.equalsIgnoreCase("true");
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            // the VMA log for the main thread is flushed by adviseBeforeThreadTerminating
            // so we just need to flush the external log
            VMAVMLogger.handler.initialise(phase);
        }

    }

    private static long getTime() {
        if (logTime) {
            return System.nanoTime();
        }
        return 0;
    }

    @Override
    protected void unseenObject(Object obj) {
        VMAVMLogger.logger.logUnseenObject(getTime(), obj);
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
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
        vmaVMLog.flush(VMLog.FLUSHMODE_FULL, vmThread);
    }

// START GENERATED CODE
// EDIT AND RUN VMLogVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeReturnByThrow(Throwable arg1, int arg2) {
        super.adviseBeforeReturnByThrow(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeReturnByThrow(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(Object arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(float arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeConstLoad(double arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeLoad(int arg1) {
        super.adviseBeforeLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeLoad(getTime(), arg1);
    }

    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2) {
        super.adviseBeforeArrayLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeArrayLoad(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, long arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, float arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, double arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, Object arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1) {
        super.adviseBeforeStackAdjust(arg1);
        VMAVMLogger.logger.logAdviseBeforeStackAdjust(getTime(), arg1);
    }

    @Override
    public void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeOperation(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeOperation(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeOperation(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, float arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConversion(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, long arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConversion(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, double arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConversion(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeIf(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeIf(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeBytecode(int arg1) {
        super.adviseBeforeBytecode(arg1);
        VMAVMLogger.logger.logAdviseBeforeBytecode(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn() {
        super.adviseBeforeReturn();
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime());
    }

    @Override
    public void adviseBeforeReturn(long arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn(float arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn(double arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeReturn(Object arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(getTime(), arg1);
    }

    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2) {
        super.adviseBeforeGetStatic(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeGetStatic(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeGetField(Object arg1, int arg2) {
        super.adviseBeforeGetField(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeGetField(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(getTime(), arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeVirtual(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeVirtual(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeSpecial(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeSpecial(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeStatic(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeStatic(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeInterface(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeInterface(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayLength(Object arg1, int arg2) {
        super.adviseBeforeArrayLength(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeArrayLength(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeThrow(Object arg1) {
        super.adviseBeforeThrow(arg1);
        VMAVMLogger.logger.logAdviseBeforeThrow(getTime(), arg1);
    }

    @Override
    public void adviseBeforeCheckCast(Object arg1, Object arg2) {
        super.adviseBeforeCheckCast(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeCheckCast(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        super.adviseBeforeInstanceOf(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInstanceOf(getTime(), arg1, arg2);
    }

    @Override
    public void adviseBeforeMonitorEnter(Object arg1) {
        super.adviseBeforeMonitorEnter(arg1);
        VMAVMLogger.logger.logAdviseBeforeMonitorEnter(getTime(), arg1);
    }

    @Override
    public void adviseBeforeMonitorExit(Object arg1) {
        super.adviseBeforeMonitorExit(arg1);
        VMAVMLogger.logger.logAdviseBeforeMonitorExit(getTime(), arg1);
    }

    @Override
    public void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeVirtual(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeVirtual(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeSpecial(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeSpecial(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeStatic(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeStatic(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeInterface(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeInterface(getTime(), arg1, arg2);
    }

    @Override
    public void adviseAfterNew(Object arg1) {
        super.adviseAfterNew(arg1);
        VMAVMLogger.logger.logAdviseAfterNew(getTime(), arg1);
    }

    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        super.adviseAfterNewArray(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterNewArray(getTime(), arg1, arg2);
        MultiNewArrayHelper.handleMultiArray(this, arg1);
    }

    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        adviseAfterNewArray(arg1, arg2[0]);
    }

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        super.adviseAfterMethodEntry(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterMethodEntry(getTime(), arg1, arg2);
    }

}
// END GENERATED CODE

