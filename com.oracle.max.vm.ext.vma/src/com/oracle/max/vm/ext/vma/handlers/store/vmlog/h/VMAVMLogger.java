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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.thread.*;

/**
 * Defines a {@link VMLogger} with operations that correspond to the {@link VMAHandler} operations.
 *
 * The requires a two-step auto generation process:
 * <ul>
 * <li>Run {@link VMAVMLoggerGenerator#main} to generate {@link VMAVMLoggerInterface}.</li>
 * <li>Run {@link VMLoggerGenerator#main} to generate {@link VMAVMLoggerAuto}.</li>
 * </ul>
 *
 * This is a special "hidden" logger that has its own {@link VMLog} instance.
 * A {@link VMAdviceHandler}, e.g., {@link VMLogStoreVMAdviceHandler}, implements its
 * advice methods by calling {@code logger.adviceMethod}. When the log overflows
 * the expectation is that the {@code trace} method is called which invokes the
 * correct {@link VMAdviceHandlerTextStoreAdapter} to flush the log records to the store.
 * To avoid unnecessary synchronization, per-thread store adaptors are supported.
 * The {@link VMLoggerInterface#traceThread} option is set to cause the
 * thread id (which is always stored in the log record) to be passed to the
 * {@code traceXXX} methods, allowing the correct adaptor to be quickly found.
 *
 * A "time" argument is prefixed to the normal handler arguments so that a handler may associate an
 * accurate time with each record.
 */

public class VMAVMLogger {

    static final VMAVMLoggerImpl logger = new VMAVMLoggerImpl();

    public static class VMAVMLoggerImpl extends VMAVMLoggerAuto {

        protected VMAVMLoggerImpl() {
            super("VMAdvice");
        }

        VMAdviceHandlerTextStoreAdapter storeAdaptor;

        @NEVER_INLINE
        private VMAdviceHandlerTextStoreAdapter storeAdaptor(int threadId) {
            return storeAdaptor.getStoreAdaptorForThread(threadId);
        }

// START GENERATED INTERFACE
// EDIT AND RUN VMAVMLoggerGenerator.main() TO MODIFY

        @Override
        protected void traceAdviseBeforeGC(int threadId, long arg1) {
            storeAdaptor(threadId).adviseBeforeGC(arg1);
        }
        @Override
        protected void traceAdviseAfterGC(int threadId, long arg1) {
            storeAdaptor(threadId).adviseAfterGC(arg1);
        }
        @Override
        protected void traceAdviseBeforeThreadStarting(int threadId, long arg1, VmThread arg2) {
            storeAdaptor(threadId).adviseBeforeThreadStarting(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeThreadTerminating(int threadId, long arg1, VmThread arg2) {
            storeAdaptor(threadId).adviseBeforeThreadTerminating(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeReturnByThrow(int threadId, long arg1, int arg2, Throwable arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeReturnByThrow(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, long arg3) {
            storeAdaptor(threadId).adviseBeforeConstLoad(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, Object arg3) {
            storeAdaptor(threadId).adviseBeforeConstLoad(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, float arg3) {
            storeAdaptor(threadId).adviseBeforeConstLoad(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, double arg3) {
            storeAdaptor(threadId).adviseBeforeConstLoad(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeLoad(int threadId, long arg1, int arg2, int arg3) {
            storeAdaptor(threadId).adviseBeforeLoad(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeArrayLoad(int threadId, long arg1, int arg2, Object arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeArrayLoad(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, long arg4) {
            storeAdaptor(threadId).adviseBeforeStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, float arg4) {
            storeAdaptor(threadId).adviseBeforeStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, double arg4) {
            storeAdaptor(threadId).adviseBeforeStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, Object arg4) {
            storeAdaptor(threadId).adviseBeforeStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, float arg5) {
            storeAdaptor(threadId).adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, long arg5) {
            storeAdaptor(threadId).adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, double arg5) {
            storeAdaptor(threadId).adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            storeAdaptor(threadId).adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeStackAdjust(int threadId, long arg1, int arg2, int arg3) {
            storeAdaptor(threadId).adviseBeforeStackAdjust(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeOperation(int threadId, long arg1, int arg2, int arg3, long arg4, long arg5) {
            storeAdaptor(threadId).adviseBeforeOperation(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeOperation(int threadId, long arg1, int arg2, int arg3, float arg4, float arg5) {
            storeAdaptor(threadId).adviseBeforeOperation(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeOperation(int threadId, long arg1, int arg2, int arg3, double arg4, double arg5) {
            storeAdaptor(threadId).adviseBeforeOperation(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeConversion(int threadId, long arg1, int arg2, int arg3, float arg4) {
            storeAdaptor(threadId).adviseBeforeConversion(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeConversion(int threadId, long arg1, int arg2, int arg3, long arg4) {
            storeAdaptor(threadId).adviseBeforeConversion(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeConversion(int threadId, long arg1, int arg2, int arg3, double arg4) {
            storeAdaptor(threadId).adviseBeforeConversion(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeIf(int threadId, long arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
            storeAdaptor(threadId).adviseBeforeIf(arg1, arg2, arg3, arg4, arg5, arg6);
        }
        @Override
        protected void traceAdviseBeforeIf(int threadId, long arg1, int arg2, int arg3, Object arg4, Object arg5, int arg6) {
            storeAdaptor(threadId).adviseBeforeIf(arg1, arg2, arg3, arg4, arg5, arg6);
        }
        @Override
        protected void traceAdviseBeforeGoto(int threadId, long arg1, int arg2, int arg3) {
            storeAdaptor(threadId).adviseBeforeGoto(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, Object arg3) {
            storeAdaptor(threadId).adviseBeforeReturn(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, long arg3) {
            storeAdaptor(threadId).adviseBeforeReturn(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, float arg3) {
            storeAdaptor(threadId).adviseBeforeReturn(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, double arg3) {
            storeAdaptor(threadId).adviseBeforeReturn(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeReturn(int threadId, long arg1, int arg2) {
            storeAdaptor(threadId).adviseBeforeReturn(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeGetStatic(int threadId, long arg1, int arg2, Object arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeGetStatic(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            storeAdaptor(threadId).adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, double arg5) {
            storeAdaptor(threadId).adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, long arg5) {
            storeAdaptor(threadId).adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, float arg5) {
            storeAdaptor(threadId).adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeGetField(int threadId, long arg1, int arg2, Object arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeGetField(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            storeAdaptor(threadId).adviseBeforePutField(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, double arg5) {
            storeAdaptor(threadId).adviseBeforePutField(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, long arg5) {
            storeAdaptor(threadId).adviseBeforePutField(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, float arg5) {
            storeAdaptor(threadId).adviseBeforePutField(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseBeforeInvokeVirtual(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4) {
            storeAdaptor(threadId).adviseBeforeInvokeVirtual(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeInvokeSpecial(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4) {
            storeAdaptor(threadId).adviseBeforeInvokeSpecial(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeInvokeStatic(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4) {
            storeAdaptor(threadId).adviseBeforeInvokeStatic(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeInvokeInterface(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4) {
            storeAdaptor(threadId).adviseBeforeInvokeInterface(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeArrayLength(int threadId, long arg1, int arg2, Object arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeArrayLength(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeThrow(int threadId, long arg1, int arg2, Object arg3) {
            storeAdaptor(threadId).adviseBeforeThrow(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeCheckCast(int threadId, long arg1, int arg2, Object arg3, Object arg4) {
            storeAdaptor(threadId).adviseBeforeCheckCast(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeInstanceOf(int threadId, long arg1, int arg2, Object arg3, Object arg4) {
            storeAdaptor(threadId).adviseBeforeInstanceOf(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeMonitorEnter(int threadId, long arg1, int arg2, Object arg3) {
            storeAdaptor(threadId).adviseBeforeMonitorEnter(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeMonitorExit(int threadId, long arg1, int arg2, Object arg3) {
            storeAdaptor(threadId).adviseBeforeMonitorExit(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterLoad(int threadId, long arg1, int arg2, int arg3, Object arg4) {
            storeAdaptor(threadId).adviseAfterLoad(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseAfterArrayLoad(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            storeAdaptor(threadId).adviseAfterArrayLoad(arg1, arg2, arg3, arg4, arg5);
        }
        @Override
        protected void traceAdviseAfterNew(int threadId, long arg1, int arg2, Object arg3) {
            storeAdaptor(threadId).adviseAfterNew(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterNewArray(int threadId, long arg1, int arg2, Object arg3, int arg4) {
            storeAdaptor(threadId).adviseAfterNewArray(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseAfterMultiNewArray(int threadId, long arg1, int arg2, Object arg3, int[] arg4) {
            storeAdaptor(threadId).adviseAfterMultiNewArray(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseAfterMethodEntry(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4) {
            storeAdaptor(threadId).adviseAfterMethodEntry(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceUnseenObject(int threadId, long arg1, Object arg2) {
            storeAdaptor(threadId).unseenObject(arg1, arg2);
        }
        @Override
        protected void traceDead(int threadId, long arg1, long arg2) {
            storeAdaptor(threadId).dead(arg1, arg2);
        }
    }

    @HOSTED_ONLY
    @VMLoggerInterface(hidden = true, traceThread = true)
    private interface VMAVMLoggerInterface {
        void adviseBeforeGC(long arg1);

        void adviseAfterGC(long arg1);

        void adviseBeforeThreadStarting(long arg1, VmThread arg2);

        void adviseBeforeThreadTerminating(long arg1, VmThread arg2);

        void adviseBeforeReturnByThrow(long arg1, int arg2, Throwable arg3, int arg4);

        void adviseBeforeConstLoad(long arg1, int arg2, long arg3);

        void adviseBeforeConstLoad(long arg1, int arg2, Object arg3);

        void adviseBeforeConstLoad(long arg1, int arg2, float arg3);

        void adviseBeforeConstLoad(long arg1, int arg2, double arg3);

        void adviseBeforeLoad(long arg1, int arg2, int arg3);

        void adviseBeforeArrayLoad(long arg1, int arg2, Object arg3, int arg4);

        void adviseBeforeStore(long arg1, int arg2, int arg3, long arg4);

        void adviseBeforeStore(long arg1, int arg2, int arg3, float arg4);

        void adviseBeforeStore(long arg1, int arg2, int arg3, double arg4);

        void adviseBeforeStore(long arg1, int arg2, int arg3, Object arg4);

        void adviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, float arg5);

        void adviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, long arg5);

        void adviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, double arg5);

        void adviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, Object arg5);

        void adviseBeforeStackAdjust(long arg1, int arg2, int arg3);

        void adviseBeforeOperation(long arg1, int arg2, int arg3, long arg4, long arg5);

        void adviseBeforeOperation(long arg1, int arg2, int arg3, float arg4, float arg5);

        void adviseBeforeOperation(long arg1, int arg2, int arg3, double arg4, double arg5);

        void adviseBeforeConversion(long arg1, int arg2, int arg3, float arg4);

        void adviseBeforeConversion(long arg1, int arg2, int arg3, long arg4);

        void adviseBeforeConversion(long arg1, int arg2, int arg3, double arg4);

        void adviseBeforeIf(long arg1, int arg2, int arg3, int arg4, int arg5, int arg6);

        void adviseBeforeIf(long arg1, int arg2, int arg3, Object arg4, Object arg5, int arg6);

        void adviseBeforeGoto(long arg1, int arg2, int arg3);

        void adviseBeforeReturn(long arg1, int arg2, Object arg3);

        void adviseBeforeReturn(long arg1, int arg2, long arg3);

        void adviseBeforeReturn(long arg1, int arg2, float arg3);

        void adviseBeforeReturn(long arg1, int arg2, double arg3);

        void adviseBeforeReturn(long arg1, int arg2);

        void adviseBeforeGetStatic(long arg1, int arg2, Object arg3, int arg4);

        void adviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, Object arg5);

        void adviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, double arg5);

        void adviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, long arg5);

        void adviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, float arg5);

        void adviseBeforeGetField(long arg1, int arg2, Object arg3, int arg4);

        void adviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, Object arg5);

        void adviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, double arg5);

        void adviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, long arg5);

        void adviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, float arg5);

        void adviseBeforeInvokeVirtual(long arg1, int arg2, Object arg3, MethodActor arg4);

        void adviseBeforeInvokeSpecial(long arg1, int arg2, Object arg3, MethodActor arg4);

        void adviseBeforeInvokeStatic(long arg1, int arg2, Object arg3, MethodActor arg4);

        void adviseBeforeInvokeInterface(long arg1, int arg2, Object arg3, MethodActor arg4);

        void adviseBeforeArrayLength(long arg1, int arg2, Object arg3, int arg4);

        void adviseBeforeThrow(long arg1, int arg2, Object arg3);

        void adviseBeforeCheckCast(long arg1, int arg2, Object arg3, Object arg4);

        void adviseBeforeInstanceOf(long arg1, int arg2, Object arg3, Object arg4);

        void adviseBeforeMonitorEnter(long arg1, int arg2, Object arg3);

        void adviseBeforeMonitorExit(long arg1, int arg2, Object arg3);

        void adviseAfterLoad(long arg1, int arg2, int arg3, Object arg4);

        void adviseAfterArrayLoad(long arg1, int arg2, Object arg3, int arg4, Object arg5);

        void adviseAfterNew(long arg1, int arg2, Object arg3);

        void adviseAfterNewArray(long arg1, int arg2, Object arg3, int arg4);

        void adviseAfterMultiNewArray(long arg1, int arg2, Object arg3, int[] arg4);

        void adviseAfterMethodEntry(long arg1, int arg2, Object arg3, MethodActor arg4);

        void unseenObject(long arg1, Object arg2);

        void dead(long arg1, long arg2);

    }
// END GENERATED INTERFACE

// START GENERATED CODE
    private static abstract class VMAVMLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            AdviseAfterArrayLoad, AdviseAfterGC, AdviseAfterLoad,
            AdviseAfterMethodEntry, AdviseAfterMultiNewArray, AdviseAfterNew, AdviseAfterNewArray,
            AdviseBeforeArrayLength, AdviseBeforeArrayLoad, AdviseBeforeArrayStore, AdviseBeforeArrayStore2,
            AdviseBeforeArrayStore3, AdviseBeforeArrayStore4, AdviseBeforeCheckCast, AdviseBeforeConstLoad,
            AdviseBeforeConstLoad2, AdviseBeforeConstLoad3, AdviseBeforeConstLoad4, AdviseBeforeConversion,
            AdviseBeforeConversion2, AdviseBeforeConversion3, AdviseBeforeGC, AdviseBeforeGetField,
            AdviseBeforeGetStatic, AdviseBeforeGoto, AdviseBeforeIf, AdviseBeforeIf2,
            AdviseBeforeInstanceOf, AdviseBeforeInvokeInterface, AdviseBeforeInvokeSpecial, AdviseBeforeInvokeStatic,
            AdviseBeforeInvokeVirtual, AdviseBeforeLoad, AdviseBeforeMonitorEnter, AdviseBeforeMonitorExit,
            AdviseBeforeOperation, AdviseBeforeOperation2, AdviseBeforeOperation3, AdviseBeforePutField,
            AdviseBeforePutField2, AdviseBeforePutField3, AdviseBeforePutField4, AdviseBeforePutStatic,
            AdviseBeforePutStatic2, AdviseBeforePutStatic3, AdviseBeforePutStatic4, AdviseBeforeReturn,
            AdviseBeforeReturn2, AdviseBeforeReturn3, AdviseBeforeReturn4, AdviseBeforeReturn5,
            AdviseBeforeReturnByThrow, AdviseBeforeStackAdjust, AdviseBeforeStore, AdviseBeforeStore2,
            AdviseBeforeStore3, AdviseBeforeStore4, AdviseBeforeThreadStarting, AdviseBeforeThreadTerminating,
            AdviseBeforeThrow, Dead, UnseenObject;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x14, 0x0, 0x8, 0x4, 0xc, 0x4, 0x4, 0x4, 0x4, 0x4, 0x4, 0x14, 0x4, 0xc,
            0x0, 0x0, 0x4, 0x0, 0x0, 0x0, 0x0, 0x0, 0x4, 0x4, 0x0, 0x0, 0x18,
            0xc, 0x4, 0x4, 0x4, 0x4, 0x0, 0x4, 0x4, 0x0, 0x0, 0x0, 0x4, 0x4, 0x14,
            0x4, 0x4, 0x4, 0x14, 0x4, 0x0, 0x0, 0x0, 0x4, 0x0, 0x4, 0x0, 0x0,
            0x0, 0x8, 0x0, 0x0, 0x0, 0x4, 0x0, 0x2};

        protected VMAVMLoggerAuto(String name) {
            super(name, Operation.VALUES.length, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logAdviseAfterArrayLoad(long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            log(Operation.AdviseAfterArrayLoad.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), objectArg(arg5));
        }
        protected abstract void traceAdviseAfterArrayLoad(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5);

        @INLINE
        public final void logAdviseAfterGC(long arg1) {
            log(Operation.AdviseAfterGC.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseAfterGC(int threadId, long arg1);

        @INLINE
        public final void logAdviseAfterLoad(long arg1, int arg2, int arg3, Object arg4) {
            log(Operation.AdviseAfterLoad.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseAfterLoad(int threadId, long arg1, int arg2, int arg3, Object arg4);

        @INLINE
        public final void logAdviseAfterMethodEntry(long arg1, int arg2, Object arg3, MethodActor arg4) {
            log(Operation.AdviseAfterMethodEntry.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), methodActorArg(arg4));
        }
        protected abstract void traceAdviseAfterMethodEntry(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4);

        @INLINE
        public final void logAdviseAfterMultiNewArray(long arg1, int arg2, Object arg3, int[] arg4) {
            log(Operation.AdviseAfterMultiNewArray.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseAfterMultiNewArray(int threadId, long arg1, int arg2, Object arg3, int[] arg4);

        @INLINE
        public final void logAdviseAfterNew(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseAfterNew.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseAfterNew(int threadId, long arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseAfterNewArray(long arg1, int arg2, Object arg3, int arg4) {
            log(Operation.AdviseAfterNewArray.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseAfterNewArray(int threadId, long arg1, int arg2, Object arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeArrayLength(long arg1, int arg2, Object arg3, int arg4) {
            log(Operation.AdviseBeforeArrayLength.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayLength(int threadId, long arg1, int arg2, Object arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeArrayLoad(long arg1, int arg2, Object arg3, int arg4) {
            log(Operation.AdviseBeforeArrayLoad.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayLoad(int threadId, long arg1, int arg2, Object arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, double arg5) {
            log(Operation.AdviseBeforeArrayStore.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), doubleArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, double arg5);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, float arg5) {
            log(Operation.AdviseBeforeArrayStore2.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), floatArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, float arg5);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            log(Operation.AdviseBeforeArrayStore3.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), objectArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, Object arg3, int arg4, long arg5) {
            log(Operation.AdviseBeforeArrayStore4.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), longArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, Object arg3, int arg4, long arg5);

        @INLINE
        public final void logAdviseBeforeCheckCast(long arg1, int arg2, Object arg3, Object arg4) {
            log(Operation.AdviseBeforeCheckCast.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforeCheckCast(int threadId, long arg1, int arg2, Object arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeConstLoad.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeConstLoad2.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeConstLoad3.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforeConstLoad4.ordinal(), longArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeConversion(long arg1, int arg2, int arg3, double arg4) {
            log(Operation.AdviseBeforeConversion.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforeConversion(int threadId, long arg1, int arg2, int arg3, double arg4);

        @INLINE
        public final void logAdviseBeforeConversion(long arg1, int arg2, int arg3, float arg4) {
            log(Operation.AdviseBeforeConversion2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforeConversion(int threadId, long arg1, int arg2, int arg3, float arg4);

        @INLINE
        public final void logAdviseBeforeConversion(long arg1, int arg2, int arg3, long arg4) {
            log(Operation.AdviseBeforeConversion3.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforeConversion(int threadId, long arg1, int arg2, int arg3, long arg4);

        @INLINE
        public final void logAdviseBeforeGC(long arg1) {
            log(Operation.AdviseBeforeGC.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeGC(int threadId, long arg1);

        @INLINE
        public final void logAdviseBeforeGetField(long arg1, int arg2, Object arg3, int arg4) {
            log(Operation.AdviseBeforeGetField.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseBeforeGetField(int threadId, long arg1, int arg2, Object arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeGetStatic(long arg1, int arg2, Object arg3, int arg4) {
            log(Operation.AdviseBeforeGetStatic.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseBeforeGetStatic(int threadId, long arg1, int arg2, Object arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeGoto(long arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeGoto.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeGoto(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeIf(long arg1, int arg2, int arg3, int arg4, int arg5,
                int arg6) {
            log(Operation.AdviseBeforeIf.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), intArg(arg4), intArg(arg5),
                intArg(arg6));
        }
        protected abstract void traceAdviseBeforeIf(int threadId, long arg1, int arg2, int arg3, int arg4, int arg5,
                int arg6);

        @INLINE
        public final void logAdviseBeforeIf(long arg1, int arg2, int arg3, Object arg4, Object arg5,
                int arg6) {
            log(Operation.AdviseBeforeIf2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), objectArg(arg4), objectArg(arg5),
                intArg(arg6));
        }
        protected abstract void traceAdviseBeforeIf(int threadId, long arg1, int arg2, int arg3, Object arg4, Object arg5,
                int arg6);

        @INLINE
        public final void logAdviseBeforeInstanceOf(long arg1, int arg2, Object arg3, Object arg4) {
            log(Operation.AdviseBeforeInstanceOf.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforeInstanceOf(int threadId, long arg1, int arg2, Object arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforeInvokeInterface(long arg1, int arg2, Object arg3, MethodActor arg4) {
            log(Operation.AdviseBeforeInvokeInterface.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), methodActorArg(arg4));
        }
        protected abstract void traceAdviseBeforeInvokeInterface(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4);

        @INLINE
        public final void logAdviseBeforeInvokeSpecial(long arg1, int arg2, Object arg3, MethodActor arg4) {
            log(Operation.AdviseBeforeInvokeSpecial.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), methodActorArg(arg4));
        }
        protected abstract void traceAdviseBeforeInvokeSpecial(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4);

        @INLINE
        public final void logAdviseBeforeInvokeStatic(long arg1, int arg2, Object arg3, MethodActor arg4) {
            log(Operation.AdviseBeforeInvokeStatic.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), methodActorArg(arg4));
        }
        protected abstract void traceAdviseBeforeInvokeStatic(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4);

        @INLINE
        public final void logAdviseBeforeInvokeVirtual(long arg1, int arg2, Object arg3, MethodActor arg4) {
            log(Operation.AdviseBeforeInvokeVirtual.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), methodActorArg(arg4));
        }
        protected abstract void traceAdviseBeforeInvokeVirtual(int threadId, long arg1, int arg2, Object arg3, MethodActor arg4);

        @INLINE
        public final void logAdviseBeforeLoad(long arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeLoad.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeLoad(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeMonitorEnter(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeMonitorEnter.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeMonitorEnter(int threadId, long arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeMonitorExit(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeMonitorExit.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeMonitorExit(int threadId, long arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeOperation(long arg1, int arg2, int arg3, double arg4, double arg5) {
            log(Operation.AdviseBeforeOperation.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), doubleArg(arg4), doubleArg(arg5));
        }
        protected abstract void traceAdviseBeforeOperation(int threadId, long arg1, int arg2, int arg3, double arg4, double arg5);

        @INLINE
        public final void logAdviseBeforeOperation(long arg1, int arg2, int arg3, float arg4, float arg5) {
            log(Operation.AdviseBeforeOperation2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), floatArg(arg4), floatArg(arg5));
        }
        protected abstract void traceAdviseBeforeOperation(int threadId, long arg1, int arg2, int arg3, float arg4, float arg5);

        @INLINE
        public final void logAdviseBeforeOperation(long arg1, int arg2, int arg3, long arg4, long arg5) {
            log(Operation.AdviseBeforeOperation3.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), longArg(arg4), longArg(arg5));
        }
        protected abstract void traceAdviseBeforeOperation(int threadId, long arg1, int arg2, int arg3, long arg4, long arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, double arg5) {
            log(Operation.AdviseBeforePutField.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), doubleArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, double arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, float arg5) {
            log(Operation.AdviseBeforePutField2.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), floatArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, float arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            log(Operation.AdviseBeforePutField3.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), objectArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, Object arg3, int arg4, long arg5) {
            log(Operation.AdviseBeforePutField4.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), longArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, Object arg3, int arg4, long arg5);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, double arg5) {
            log(Operation.AdviseBeforePutStatic.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), doubleArg(arg5));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, double arg5);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, float arg5) {
            log(Operation.AdviseBeforePutStatic2.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), floatArg(arg5));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, float arg5);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, Object arg5) {
            log(Operation.AdviseBeforePutStatic3.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), objectArg(arg5));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, Object arg5);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, Object arg3, int arg4, long arg5) {
            log(Operation.AdviseBeforePutStatic4.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4), longArg(arg5));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, Object arg3, int arg4, long arg5);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2) {
            log(Operation.AdviseBeforeReturn.ordinal(), longArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeReturn2.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeReturn3.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeReturn4.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforeReturn5.ordinal(), longArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeReturnByThrow(long arg1, int arg2, Throwable arg3, int arg4) {
            log(Operation.AdviseBeforeReturnByThrow.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseBeforeReturnByThrow(int threadId, long arg1, int arg2, Throwable arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeStackAdjust(long arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeStackAdjust.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeStackAdjust(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, double arg4) {
            log(Operation.AdviseBeforeStore.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, double arg4);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, float arg4) {
            log(Operation.AdviseBeforeStore2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, float arg4);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, Object arg4) {
            log(Operation.AdviseBeforeStore3.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, long arg4) {
            log(Operation.AdviseBeforeStore4.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, long arg4);

        @INLINE
        public final void logAdviseBeforeThreadStarting(long arg1, VmThread arg2) {
            log(Operation.AdviseBeforeThreadStarting.ordinal(), longArg(arg1), vmThreadArg(arg2));
        }
        protected abstract void traceAdviseBeforeThreadStarting(int threadId, long arg1, VmThread arg2);

        @INLINE
        public final void logAdviseBeforeThreadTerminating(long arg1, VmThread arg2) {
            log(Operation.AdviseBeforeThreadTerminating.ordinal(), longArg(arg1), vmThreadArg(arg2));
        }
        protected abstract void traceAdviseBeforeThreadTerminating(int threadId, long arg1, VmThread arg2);

        @INLINE
        public final void logAdviseBeforeThrow(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeThrow.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeThrow(int threadId, long arg1, int arg2, Object arg3);

        @INLINE
        public final void logDead(long arg1, long arg2) {
            log(Operation.Dead.ordinal(), longArg(arg1), longArg(arg2));
        }
        protected abstract void traceDead(int threadId, long arg1, long arg2);

        @INLINE
        public final void logUnseenObject(long arg1, Object arg2) {
            log(Operation.UnseenObject.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceUnseenObject(int threadId, long arg1, Object arg2);

        @Override
        protected void trace(Record r) {
            int threadId = r.getThreadId();
            switch (r.getOperation()) {
                case 0: { //AdviseAfterArrayLoad
                    traceAdviseAfterArrayLoad(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toObject(r, 5));
                    break;
                }
                case 1: { //AdviseAfterGC
                    traceAdviseAfterGC(threadId, toLong(r, 1));
                    break;
                }
                case 2: { //AdviseAfterLoad
                    traceAdviseAfterLoad(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toObject(r, 4));
                    break;
                }
                case 3: { //AdviseAfterMethodEntry
                    traceAdviseAfterMethodEntry(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toMethodActor(r, 4));
                    break;
                }
                case 4: { //AdviseAfterMultiNewArray
                    traceAdviseAfterMultiNewArray(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toIntArray(r, 4));
                    break;
                }
                case 5: { //AdviseAfterNew
                    traceAdviseAfterNew(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 6: { //AdviseAfterNewArray
                    traceAdviseAfterNewArray(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4));
                    break;
                }
                case 7: { //AdviseBeforeArrayLength
                    traceAdviseBeforeArrayLength(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4));
                    break;
                }
                case 8: { //AdviseBeforeArrayLoad
                    traceAdviseBeforeArrayLoad(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4));
                    break;
                }
                case 9: { //AdviseBeforeArrayStore
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toDouble(r, 5));
                    break;
                }
                case 10: { //AdviseBeforeArrayStore2
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toFloat(r, 5));
                    break;
                }
                case 11: { //AdviseBeforeArrayStore3
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toObject(r, 5));
                    break;
                }
                case 12: { //AdviseBeforeArrayStore4
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toLong(r, 5));
                    break;
                }
                case 13: { //AdviseBeforeCheckCast
                    traceAdviseBeforeCheckCast(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toObject(r, 4));
                    break;
                }
                case 14: { //AdviseBeforeConstLoad
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 15: { //AdviseBeforeConstLoad2
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 16: { //AdviseBeforeConstLoad3
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 17: { //AdviseBeforeConstLoad4
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 18: { //AdviseBeforeConversion
                    traceAdviseBeforeConversion(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 19: { //AdviseBeforeConversion2
                    traceAdviseBeforeConversion(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 20: { //AdviseBeforeConversion3
                    traceAdviseBeforeConversion(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 21: { //AdviseBeforeGC
                    traceAdviseBeforeGC(threadId, toLong(r, 1));
                    break;
                }
                case 22: { //AdviseBeforeGetField
                    traceAdviseBeforeGetField(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4));
                    break;
                }
                case 23: { //AdviseBeforeGetStatic
                    traceAdviseBeforeGetStatic(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4));
                    break;
                }
                case 24: { //AdviseBeforeGoto
                    traceAdviseBeforeGoto(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 25: { //AdviseBeforeIf
                    traceAdviseBeforeIf(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toInt(r, 4), toInt(r, 5), toInt(r, 6));
                    break;
                }
                case 26: { //AdviseBeforeIf2
                    traceAdviseBeforeIf(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toObject(r, 4), toObject(r, 5), toInt(r, 6));
                    break;
                }
                case 27: { //AdviseBeforeInstanceOf
                    traceAdviseBeforeInstanceOf(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toObject(r, 4));
                    break;
                }
                case 28: { //AdviseBeforeInvokeInterface
                    traceAdviseBeforeInvokeInterface(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toMethodActor(r, 4));
                    break;
                }
                case 29: { //AdviseBeforeInvokeSpecial
                    traceAdviseBeforeInvokeSpecial(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toMethodActor(r, 4));
                    break;
                }
                case 30: { //AdviseBeforeInvokeStatic
                    traceAdviseBeforeInvokeStatic(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toMethodActor(r, 4));
                    break;
                }
                case 31: { //AdviseBeforeInvokeVirtual
                    traceAdviseBeforeInvokeVirtual(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toMethodActor(r, 4));
                    break;
                }
                case 32: { //AdviseBeforeLoad
                    traceAdviseBeforeLoad(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 33: { //AdviseBeforeMonitorEnter
                    traceAdviseBeforeMonitorEnter(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 34: { //AdviseBeforeMonitorExit
                    traceAdviseBeforeMonitorExit(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 35: { //AdviseBeforeOperation
                    traceAdviseBeforeOperation(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toDouble(r, 4), toDouble(r, 5));
                    break;
                }
                case 36: { //AdviseBeforeOperation2
                    traceAdviseBeforeOperation(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toFloat(r, 4), toFloat(r, 5));
                    break;
                }
                case 37: { //AdviseBeforeOperation3
                    traceAdviseBeforeOperation(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toLong(r, 4), toLong(r, 5));
                    break;
                }
                case 38: { //AdviseBeforePutField
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toDouble(r, 5));
                    break;
                }
                case 39: { //AdviseBeforePutField2
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toFloat(r, 5));
                    break;
                }
                case 40: { //AdviseBeforePutField3
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toObject(r, 5));
                    break;
                }
                case 41: { //AdviseBeforePutField4
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toLong(r, 5));
                    break;
                }
                case 42: { //AdviseBeforePutStatic
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toDouble(r, 5));
                    break;
                }
                case 43: { //AdviseBeforePutStatic2
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toFloat(r, 5));
                    break;
                }
                case 44: { //AdviseBeforePutStatic3
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toObject(r, 5));
                    break;
                }
                case 45: { //AdviseBeforePutStatic4
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3), toInt(r, 4), toLong(r, 5));
                    break;
                }
                case 46: { //AdviseBeforeReturn
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2));
                    break;
                }
                case 47: { //AdviseBeforeReturn2
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 48: { //AdviseBeforeReturn3
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 49: { //AdviseBeforeReturn4
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 50: { //AdviseBeforeReturn5
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 51: { //AdviseBeforeReturnByThrow
                    traceAdviseBeforeReturnByThrow(threadId, toLong(r, 1), toInt(r, 2), toThrowable(r, 3), toInt(r, 4));
                    break;
                }
                case 52: { //AdviseBeforeStackAdjust
                    traceAdviseBeforeStackAdjust(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 53: { //AdviseBeforeStore
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 54: { //AdviseBeforeStore2
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 55: { //AdviseBeforeStore3
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toObject(r, 4));
                    break;
                }
                case 56: { //AdviseBeforeStore4
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 57: { //AdviseBeforeThreadStarting
                    traceAdviseBeforeThreadStarting(threadId, toLong(r, 1), toVmThread(r, 2));
                    break;
                }
                case 58: { //AdviseBeforeThreadTerminating
                    traceAdviseBeforeThreadTerminating(threadId, toLong(r, 1), toVmThread(r, 2));
                    break;
                }
                case 59: { //AdviseBeforeThrow
                    traceAdviseBeforeThrow(threadId, toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 60: { //Dead
                    traceDead(threadId, toLong(r, 1), toLong(r, 2));
                    break;
                }
                case 61: { //UnseenObject
                    traceUnseenObject(threadId, toLong(r, 1), toObject(r, 2));
                    break;
                }
            }
        }
        static int[] toIntArray(Record r, int argNum) {
            if (MaxineVM.isHosted()) {
                return (int[]) ObjectArg.getArg(r, argNum);
            } else {
                return asIntArray(toObject(r, argNum));
            }
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native int[] asIntArray(Object arg);

    }

// END GENERATED CODE

}
