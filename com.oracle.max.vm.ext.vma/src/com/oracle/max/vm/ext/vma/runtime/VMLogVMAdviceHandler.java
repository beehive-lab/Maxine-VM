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

import java.util.*;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.log.java.fix.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation that uses a secondary {@link VMLog} for storage.
 */
public class VMLogVMAdviceHandler extends ObjectStateHandlerAdaptor {

    private VMLog vmaVMLog;
    private LoggingVMAdviceHandler logHandler;

    private static class VMLogFlusher extends VMLog.Flusher {

        @Override
        public void flushRecord(Record r) {
            VMAVMLogger.logger.trace(r);
        }
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        super.initialise(phase);
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
//            VMLog vmaVMLog = new VMLogNativeThreadVariableVMA();
            vmaVMLog = new VMLogArrayFixed();
            vmaVMLog.initialize(phase);
            ArrayList<VMLogger> list = new ArrayList<VMLogger>();
            list.add(VMAVMLogger.logger);
            vmaVMLog.initialize(list, new VMLogFlusher());
        } else if (phase == MaxineVM.Phase.RUNNING) {
            logHandler = new LoggingVMAdviceHandler();
            logHandler.initialise(phase);
            super.setRemovalTracker(logHandler.getRemovalTracker(state));
            VMAVMLogger.VMAVMLoggerImpl.setLogHandler(logHandler);
            VMAVMLogger.logger.enable(true);
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            vmaVMLog.flushLog();
            logHandler.initialise(phase);
        }

    }

    @Override
    protected void unseenObject(Object obj) {
        VMAVMLogger.logger.logUnseenObject(obj);
    }

    @Override
    public void adviseBeforeGC() {
        VMAVMLogger.logger.logAdviseBeforeGC();
    }

    @Override
    public void adviseAfterGC() {
        // We log the GC first
        VMAVMLogger.logger.logAdviseAfterGC();
        super.adviseAfterGC();
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread vmThread) {
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread vmThread) {
    }

// START GENERATED CODE
// EDIT AND RUN VMLogVMAdviceHandlerGenerator.main() TO MODIFY

    @Override
    public void adviseBeforeConstLoad(long arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(arg1);
    }

    @Override
    public void adviseBeforeConstLoad(Object arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(arg1);
    }

    @Override
    public void adviseBeforeConstLoad(float arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(arg1);
    }

    @Override
    public void adviseBeforeConstLoad(double arg1) {
        super.adviseBeforeConstLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeConstLoad(arg1);
    }

    @Override
    public void adviseBeforeLoad(int arg1) {
        super.adviseBeforeLoad(arg1);
        VMAVMLogger.logger.logAdviseBeforeLoad(arg1);
    }

    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2) {
        super.adviseBeforeArrayLoad(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeArrayLoad(arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, long arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, float arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, double arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(arg1, arg2);
    }

    @Override
    public void adviseBeforeStore(int arg1, Object arg2) {
        super.adviseBeforeStore(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeStore(arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeArrayStore(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1) {
        super.adviseBeforeStackAdjust(arg1);
        VMAVMLogger.logger.logAdviseBeforeStackAdjust(arg1);
    }

    @Override
    public void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeOperation(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeOperation(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        super.adviseBeforeOperation(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeOperation(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeConversion(int arg1, float arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConversion(arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, long arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConversion(arg1, arg2);
    }

    @Override
    public void adviseBeforeConversion(int arg1, double arg2) {
        super.adviseBeforeConversion(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeConversion(arg1, arg2);
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeIf(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        super.adviseBeforeIf(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforeIf(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeBytecode(int arg1) {
        super.adviseBeforeBytecode(arg1);
        VMAVMLogger.logger.logAdviseBeforeBytecode(arg1);
    }

    @Override
    public void adviseBeforeReturn() {
        super.adviseBeforeReturn();
        VMAVMLogger.logger.logAdviseBeforeReturn();
    }

    @Override
    public void adviseBeforeReturn(long arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(arg1);
    }

    @Override
    public void adviseBeforeReturn(float arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(arg1);
    }

    @Override
    public void adviseBeforeReturn(double arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(arg1);
    }

    @Override
    public void adviseBeforeReturn(Object arg1) {
        super.adviseBeforeReturn(arg1);
        VMAVMLogger.logger.logAdviseBeforeReturn(arg1);
    }

    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2) {
        super.adviseBeforeGetStatic(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeGetStatic(arg1, arg2);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutStatic(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeGetField(Object arg1, int arg2) {
        super.adviseBeforeGetField(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeGetField(arg1, arg2);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        VMAVMLogger.logger.logAdviseBeforePutField(arg1, arg2, arg3);
    }

    @Override
    public void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeVirtual(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeVirtual(arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeSpecial(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeSpecial(arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeStatic(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeStatic(arg1, arg2);
    }

    @Override
    public void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseBeforeInvokeInterface(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInvokeInterface(arg1, arg2);
    }

    @Override
    public void adviseBeforeArrayLength(Object arg1, int arg2) {
        super.adviseBeforeArrayLength(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeArrayLength(arg1, arg2);
    }

    @Override
    public void adviseBeforeThrow(Object arg1) {
        super.adviseBeforeThrow(arg1);
        VMAVMLogger.logger.logAdviseBeforeThrow(arg1);
    }

    @Override
    public void adviseBeforeCheckCast(Object arg1, Object arg2) {
        super.adviseBeforeCheckCast(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeCheckCast(arg1, arg2);
    }

    @Override
    public void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        super.adviseBeforeInstanceOf(arg1, arg2);
        VMAVMLogger.logger.logAdviseBeforeInstanceOf(arg1, arg2);
    }

    @Override
    public void adviseBeforeMonitorEnter(Object arg1) {
        super.adviseBeforeMonitorEnter(arg1);
        VMAVMLogger.logger.logAdviseBeforeMonitorEnter(arg1);
    }

    @Override
    public void adviseBeforeMonitorExit(Object arg1) {
        super.adviseBeforeMonitorExit(arg1);
        VMAVMLogger.logger.logAdviseBeforeMonitorExit(arg1);
    }

    @Override
    public void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeVirtual(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeVirtual(arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeSpecial(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeSpecial(arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeStatic(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeStatic(arg1, arg2);
    }

    @Override
    public void adviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
        super.adviseAfterInvokeInterface(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterInvokeInterface(arg1, arg2);
    }

    @Override
    public void adviseAfterNew(Object arg1) {
        super.adviseAfterNew(arg1);
        VMAVMLogger.logger.logAdviseAfterNew(arg1);
    }

    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        super.adviseAfterNewArray(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterNewArray(arg1, arg2);
        MultiNewArrayHelper.handleMultiArray(this, arg1);
    }

    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        adviseAfterNewArray(arg1, arg2[0]);
    }

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        super.adviseAfterMethodEntry(arg1, arg2);
        VMAVMLogger.logger.logAdviseAfterMethodEntry(arg1, arg2);
    }

}
// END GENERATED CODE

