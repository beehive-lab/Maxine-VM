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
 *
 * The {@link VMLogger} tracing support is used to handle log flushing by explicitly calling the {@link VMLogger#trace}
 * method.
 *
 * A "time" argument is prefixed to the handler arguments so that a handler may log an accurate time with each record.
 */

public class VMAVMLogger {

    static final VMAVMLoggerImpl logger = new VMAVMLoggerImpl();

    static VMAdviceHandlerTextStoreAdapter storeAdaptor;

    public static class VMAVMLoggerImpl extends VMAVMLoggerAuto {

        protected VMAVMLoggerImpl() {
            super("VMAdvice");
        }

        public static void setStoreAdaptor(VMAdviceHandlerTextStoreAdapter h) {
            storeAdaptor = h;
        }

// START GENERATED INTERFACE
// EDIT AND RUN VMAVMLoggerGenerator.main() TO MODIFY

        @Override
        protected void traceAdviseBeforeGC(long arg1) {
            storeAdaptor.adviseBeforeGC(arg1);
        }
        @Override
        protected void traceAdviseAfterGC(long arg1) {
            storeAdaptor.adviseAfterGC(arg1);
        }
        @Override
        protected void traceAdviseBeforeThreadStarting(long arg1, VmThread arg2) {
            storeAdaptor.adviseBeforeThreadStarting(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeThreadTerminating(long arg1, VmThread arg2) {
            storeAdaptor.adviseBeforeThreadTerminating(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeReturnByThrow(long arg1, Throwable arg2, int arg3) {
            storeAdaptor.adviseBeforeReturnByThrow(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(long arg1, long arg2) {
            storeAdaptor.adviseBeforeConstLoad(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(long arg1, Object arg2) {
            storeAdaptor.adviseBeforeConstLoad(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(long arg1, float arg2) {
            storeAdaptor.adviseBeforeConstLoad(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(long arg1, double arg2) {
            storeAdaptor.adviseBeforeConstLoad(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeLoad(long arg1, int arg2) {
            storeAdaptor.adviseBeforeLoad(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeArrayLoad(long arg1, Object arg2, int arg3) {
            storeAdaptor.adviseBeforeArrayLoad(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeStore(long arg1, int arg2, long arg3) {
            storeAdaptor.adviseBeforeStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeStore(long arg1, int arg2, float arg3) {
            storeAdaptor.adviseBeforeStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeStore(long arg1, int arg2, double arg3) {
            storeAdaptor.adviseBeforeStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeStore(long arg1, int arg2, Object arg3) {
            storeAdaptor.adviseBeforeStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, float arg4) {
            storeAdaptor.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, long arg4) {
            storeAdaptor.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, double arg4) {
            storeAdaptor.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, Object arg4) {
            storeAdaptor.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeStackAdjust(long arg1, int arg2) {
            storeAdaptor.adviseBeforeStackAdjust(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeOperation(long arg1, int arg2, long arg3, long arg4) {
            storeAdaptor.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeOperation(long arg1, int arg2, float arg3, float arg4) {
            storeAdaptor.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeOperation(long arg1, int arg2, double arg3, double arg4) {
            storeAdaptor.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeConversion(long arg1, int arg2, float arg3) {
            storeAdaptor.adviseBeforeConversion(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConversion(long arg1, int arg2, long arg3) {
            storeAdaptor.adviseBeforeConversion(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConversion(long arg1, int arg2, double arg3) {
            storeAdaptor.adviseBeforeConversion(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeIf(long arg1, int arg2, int arg3, int arg4) {
            storeAdaptor.adviseBeforeIf(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeIf(long arg1, int arg2, Object arg3, Object arg4) {
            storeAdaptor.adviseBeforeIf(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeBytecode(long arg1, int arg2) {
            storeAdaptor.adviseBeforeBytecode(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeReturn(long arg1) {
            storeAdaptor.adviseBeforeReturn(arg1);
        }
        @Override
        protected void traceAdviseBeforeReturn(long arg1, long arg2) {
            storeAdaptor.adviseBeforeReturn(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeReturn(long arg1, float arg2) {
            storeAdaptor.adviseBeforeReturn(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeReturn(long arg1, double arg2) {
            storeAdaptor.adviseBeforeReturn(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeReturn(long arg1, Object arg2) {
            storeAdaptor.adviseBeforeReturn(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeGetStatic(long arg1, Object arg2, int arg3) {
            storeAdaptor.adviseBeforeGetStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, Object arg4) {
            storeAdaptor.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, float arg4) {
            storeAdaptor.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, double arg4) {
            storeAdaptor.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, long arg4) {
            storeAdaptor.adviseBeforePutStatic(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeGetField(long arg1, Object arg2, int arg3) {
            storeAdaptor.adviseBeforeGetField(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, Object arg4) {
            storeAdaptor.adviseBeforePutField(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, float arg4) {
            storeAdaptor.adviseBeforePutField(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, double arg4) {
            storeAdaptor.adviseBeforePutField(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, long arg4) {
            storeAdaptor.adviseBeforePutField(arg1, arg2, arg3, arg4);
        }
        @Override
        protected void traceAdviseBeforeInvokeVirtual(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeInvokeSpecial(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeInvokeStatic(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseBeforeInvokeStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeInvokeInterface(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseBeforeInvokeInterface(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeArrayLength(long arg1, Object arg2, int arg3) {
            storeAdaptor.adviseBeforeArrayLength(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeThrow(long arg1, Object arg2) {
            storeAdaptor.adviseBeforeThrow(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeCheckCast(long arg1, Object arg2, Object arg3) {
            storeAdaptor.adviseBeforeCheckCast(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeInstanceOf(long arg1, Object arg2, Object arg3) {
            storeAdaptor.adviseBeforeInstanceOf(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeMonitorEnter(long arg1, Object arg2) {
            storeAdaptor.adviseBeforeMonitorEnter(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeMonitorExit(long arg1, Object arg2) {
            storeAdaptor.adviseBeforeMonitorExit(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterInvokeVirtual(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseAfterInvokeVirtual(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterInvokeSpecial(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseAfterInvokeSpecial(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterInvokeStatic(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseAfterInvokeStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterInvokeInterface(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseAfterInvokeInterface(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterNew(long arg1, Object arg2) {
            storeAdaptor.adviseAfterNew(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterNewArray(long arg1, Object arg2, int arg3) {
            storeAdaptor.adviseAfterNewArray(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterMultiNewArray(long arg1, Object arg2, int[] arg3) {
            storeAdaptor.adviseAfterMultiNewArray(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseAfterMethodEntry(long arg1, Object arg2, MethodActor arg3) {
            storeAdaptor.adviseAfterMethodEntry(arg1, arg2, arg3);
        }
        @Override
        protected void traceUnseenObject(long arg1, Object arg2) {
            storeAdaptor.unseenObject(arg1, arg2);
        }
    }

    @HOSTED_ONLY
    @VMLoggerInterface(hidden = true)
    private interface VMAVMLoggerInterface {
        void adviseBeforeGC(long arg1);

        void adviseAfterGC(long arg1);

        void adviseBeforeThreadStarting(long arg1, VmThread arg2);

        void adviseBeforeThreadTerminating(long arg1, VmThread arg2);

        void adviseBeforeReturnByThrow(long arg1, Throwable arg2, int arg3);

        void adviseBeforeConstLoad(long arg1, long arg2);

        void adviseBeforeConstLoad(long arg1, Object arg2);

        void adviseBeforeConstLoad(long arg1, float arg2);

        void adviseBeforeConstLoad(long arg1, double arg2);

        void adviseBeforeLoad(long arg1, int arg2);

        void adviseBeforeArrayLoad(long arg1, Object arg2, int arg3);

        void adviseBeforeStore(long arg1, int arg2, long arg3);

        void adviseBeforeStore(long arg1, int arg2, float arg3);

        void adviseBeforeStore(long arg1, int arg2, double arg3);

        void adviseBeforeStore(long arg1, int arg2, Object arg3);

        void adviseBeforeArrayStore(long arg1, Object arg2, int arg3, float arg4);

        void adviseBeforeArrayStore(long arg1, Object arg2, int arg3, long arg4);

        void adviseBeforeArrayStore(long arg1, Object arg2, int arg3, double arg4);

        void adviseBeforeArrayStore(long arg1, Object arg2, int arg3, Object arg4);

        void adviseBeforeStackAdjust(long arg1, int arg2);

        void adviseBeforeOperation(long arg1, int arg2, long arg3, long arg4);

        void adviseBeforeOperation(long arg1, int arg2, float arg3, float arg4);

        void adviseBeforeOperation(long arg1, int arg2, double arg3, double arg4);

        void adviseBeforeConversion(long arg1, int arg2, float arg3);

        void adviseBeforeConversion(long arg1, int arg2, long arg3);

        void adviseBeforeConversion(long arg1, int arg2, double arg3);

        void adviseBeforeIf(long arg1, int arg2, int arg3, int arg4);

        void adviseBeforeIf(long arg1, int arg2, Object arg3, Object arg4);

        void adviseBeforeBytecode(long arg1, int arg2);

        void adviseBeforeReturn(long arg1);

        void adviseBeforeReturn(long arg1, long arg2);

        void adviseBeforeReturn(long arg1, float arg2);

        void adviseBeforeReturn(long arg1, double arg2);

        void adviseBeforeReturn(long arg1, Object arg2);

        void adviseBeforeGetStatic(long arg1, Object arg2, int arg3);

        void adviseBeforePutStatic(long arg1, Object arg2, int arg3, Object arg4);

        void adviseBeforePutStatic(long arg1, Object arg2, int arg3, float arg4);

        void adviseBeforePutStatic(long arg1, Object arg2, int arg3, double arg4);

        void adviseBeforePutStatic(long arg1, Object arg2, int arg3, long arg4);

        void adviseBeforeGetField(long arg1, Object arg2, int arg3);

        void adviseBeforePutField(long arg1, Object arg2, int arg3, Object arg4);

        void adviseBeforePutField(long arg1, Object arg2, int arg3, float arg4);

        void adviseBeforePutField(long arg1, Object arg2, int arg3, double arg4);

        void adviseBeforePutField(long arg1, Object arg2, int arg3, long arg4);

        void adviseBeforeInvokeVirtual(long arg1, Object arg2, MethodActor arg3);

        void adviseBeforeInvokeSpecial(long arg1, Object arg2, MethodActor arg3);

        void adviseBeforeInvokeStatic(long arg1, Object arg2, MethodActor arg3);

        void adviseBeforeInvokeInterface(long arg1, Object arg2, MethodActor arg3);

        void adviseBeforeArrayLength(long arg1, Object arg2, int arg3);

        void adviseBeforeThrow(long arg1, Object arg2);

        void adviseBeforeCheckCast(long arg1, Object arg2, Object arg3);

        void adviseBeforeInstanceOf(long arg1, Object arg2, Object arg3);

        void adviseBeforeMonitorEnter(long arg1, Object arg2);

        void adviseBeforeMonitorExit(long arg1, Object arg2);

        void adviseAfterInvokeVirtual(long arg1, Object arg2, MethodActor arg3);

        void adviseAfterInvokeSpecial(long arg1, Object arg2, MethodActor arg3);

        void adviseAfterInvokeStatic(long arg1, Object arg2, MethodActor arg3);

        void adviseAfterInvokeInterface(long arg1, Object arg2, MethodActor arg3);

        void adviseAfterNew(long arg1, Object arg2);

        void adviseAfterNewArray(long arg1, Object arg2, int arg3);

        void adviseAfterMultiNewArray(long arg1, Object arg2, int[] arg3);

        void adviseAfterMethodEntry(long arg1, Object arg2, MethodActor arg3);

        void unseenObject(long arg1, Object arg2);

    }
// END GENERATED INTERFACE

// START GENERATED CODE
    private static abstract class VMAVMLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            AdviseAfterGC, AdviseAfterInvokeInterface, AdviseAfterInvokeSpecial,
            AdviseAfterInvokeStatic, AdviseAfterInvokeVirtual, AdviseAfterMethodEntry, AdviseAfterMultiNewArray,
            AdviseAfterNew, AdviseAfterNewArray, AdviseBeforeArrayLength, AdviseBeforeArrayLoad,
            AdviseBeforeArrayStore, AdviseBeforeArrayStore2, AdviseBeforeArrayStore3, AdviseBeforeArrayStore4,
            AdviseBeforeBytecode, AdviseBeforeCheckCast, AdviseBeforeConstLoad, AdviseBeforeConstLoad2,
            AdviseBeforeConstLoad3, AdviseBeforeConstLoad4, AdviseBeforeConversion, AdviseBeforeConversion2,
            AdviseBeforeConversion3, AdviseBeforeGC, AdviseBeforeGetField, AdviseBeforeGetStatic,
            AdviseBeforeIf, AdviseBeforeIf2, AdviseBeforeInstanceOf, AdviseBeforeInvokeInterface,
            AdviseBeforeInvokeSpecial, AdviseBeforeInvokeStatic, AdviseBeforeInvokeVirtual, AdviseBeforeLoad,
            AdviseBeforeMonitorEnter, AdviseBeforeMonitorExit, AdviseBeforeOperation, AdviseBeforeOperation2,
            AdviseBeforeOperation3, AdviseBeforePutField, AdviseBeforePutField2, AdviseBeforePutField3,
            AdviseBeforePutField4, AdviseBeforePutStatic, AdviseBeforePutStatic2, AdviseBeforePutStatic3,
            AdviseBeforePutStatic4, AdviseBeforeReturn, AdviseBeforeReturn2, AdviseBeforeReturn3,
            AdviseBeforeReturn4, AdviseBeforeReturn5, AdviseBeforeReturnByThrow, AdviseBeforeStackAdjust,
            AdviseBeforeStore, AdviseBeforeStore2, AdviseBeforeStore3, AdviseBeforeStore4,
            AdviseBeforeThreadStarting, AdviseBeforeThreadTerminating, AdviseBeforeThrow, UnseenObject;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x0, 0x2, 0x2, 0x2, 0x2, 0x2, 0x6, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0xa,
            0x2, 0x0, 0x6, 0x0, 0x0, 0x2, 0x0, 0x0, 0x0, 0x0, 0x0, 0x2, 0x2, 0x0,
            0xc, 0x6, 0x2, 0x2, 0x2, 0x2, 0x0, 0x2, 0x2, 0x0, 0x0, 0x0, 0x2, 0x2,
            0xa, 0x2, 0x2, 0x2, 0xa, 0x2, 0x0, 0x0, 0x0, 0x2, 0x0, 0x2, 0x0,
            0x0, 0x0, 0x4, 0x0, 0x0, 0x0, 0x2, 0x2};

        protected VMAVMLoggerAuto(String name) {
            super(name, Operation.VALUES.length, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logAdviseAfterGC(long arg1) {
            log(Operation.AdviseAfterGC.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseAfterGC(long arg1);

        @INLINE
        public final void logAdviseAfterInvokeInterface(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseAfterInvokeInterface.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseAfterInvokeInterface(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseAfterInvokeSpecial(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseAfterInvokeSpecial.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseAfterInvokeSpecial(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseAfterInvokeStatic(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseAfterInvokeStatic.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseAfterInvokeStatic(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseAfterInvokeVirtual(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseAfterInvokeVirtual.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseAfterInvokeVirtual(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseAfterMethodEntry(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseAfterMethodEntry.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseAfterMethodEntry(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseAfterMultiNewArray(long arg1, Object arg2, int[] arg3) {
            log(Operation.AdviseAfterMultiNewArray.ordinal(), longArg(arg1), objectArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseAfterMultiNewArray(long arg1, Object arg2, int[] arg3);

        @INLINE
        public final void logAdviseAfterNew(long arg1, Object arg2) {
            log(Operation.AdviseAfterNew.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseAfterNew(long arg1, Object arg2);

        @INLINE
        public final void logAdviseAfterNewArray(long arg1, Object arg2, int arg3) {
            log(Operation.AdviseAfterNewArray.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseAfterNewArray(long arg1, Object arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeArrayLength(long arg1, Object arg2, int arg3) {
            log(Operation.AdviseBeforeArrayLength.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeArrayLength(long arg1, Object arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeArrayLoad(long arg1, Object arg2, int arg3) {
            log(Operation.AdviseBeforeArrayLoad.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeArrayLoad(long arg1, Object arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, double arg4) {
            log(Operation.AdviseBeforeArrayStore.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, double arg4);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, float arg4) {
            log(Operation.AdviseBeforeArrayStore2.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, float arg4);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, Object arg4) {
            log(Operation.AdviseBeforeArrayStore3.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, long arg4) {
            log(Operation.AdviseBeforeArrayStore4.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayStore(long arg1, Object arg2, int arg3, long arg4);

        @INLINE
        public final void logAdviseBeforeBytecode(long arg1, int arg2) {
            log(Operation.AdviseBeforeBytecode.ordinal(), longArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeBytecode(long arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeCheckCast(long arg1, Object arg2, Object arg3) {
            log(Operation.AdviseBeforeCheckCast.ordinal(), longArg(arg1), objectArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeCheckCast(long arg1, Object arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, double arg2) {
            log(Operation.AdviseBeforeConstLoad.ordinal(), longArg(arg1), doubleArg(arg2));
        }
        protected abstract void traceAdviseBeforeConstLoad(long arg1, double arg2);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, float arg2) {
            log(Operation.AdviseBeforeConstLoad2.ordinal(), longArg(arg1), floatArg(arg2));
        }
        protected abstract void traceAdviseBeforeConstLoad(long arg1, float arg2);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, Object arg2) {
            log(Operation.AdviseBeforeConstLoad3.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeConstLoad(long arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, long arg2) {
            log(Operation.AdviseBeforeConstLoad4.ordinal(), longArg(arg1), longArg(arg2));
        }
        protected abstract void traceAdviseBeforeConstLoad(long arg1, long arg2);

        @INLINE
        public final void logAdviseBeforeConversion(long arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeConversion.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeConversion(long arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeConversion(long arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeConversion2.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeConversion(long arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeConversion(long arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforeConversion3.ordinal(), longArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeConversion(long arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeGC(long arg1) {
            log(Operation.AdviseBeforeGC.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeGC(long arg1);

        @INLINE
        public final void logAdviseBeforeGetField(long arg1, Object arg2, int arg3) {
            log(Operation.AdviseBeforeGetField.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeGetField(long arg1, Object arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeGetStatic(long arg1, Object arg2, int arg3) {
            log(Operation.AdviseBeforeGetStatic.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeGetStatic(long arg1, Object arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeIf(long arg1, int arg2, int arg3, int arg4) {
            log(Operation.AdviseBeforeIf.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), intArg(arg4));
        }
        protected abstract void traceAdviseBeforeIf(long arg1, int arg2, int arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeIf(long arg1, int arg2, Object arg3, Object arg4) {
            log(Operation.AdviseBeforeIf2.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforeIf(long arg1, int arg2, Object arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforeInstanceOf(long arg1, Object arg2, Object arg3) {
            log(Operation.AdviseBeforeInstanceOf.ordinal(), longArg(arg1), objectArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeInstanceOf(long arg1, Object arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeInvokeInterface(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseBeforeInvokeInterface.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseBeforeInvokeInterface(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseBeforeInvokeSpecial(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseBeforeInvokeSpecial.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseBeforeInvokeSpecial(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseBeforeInvokeStatic(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseBeforeInvokeStatic.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseBeforeInvokeStatic(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseBeforeInvokeVirtual(long arg1, Object arg2, MethodActor arg3) {
            log(Operation.AdviseBeforeInvokeVirtual.ordinal(), longArg(arg1), objectArg(arg2), methodActorArg(arg3));
        }
        protected abstract void traceAdviseBeforeInvokeVirtual(long arg1, Object arg2, MethodActor arg3);

        @INLINE
        public final void logAdviseBeforeLoad(long arg1, int arg2) {
            log(Operation.AdviseBeforeLoad.ordinal(), longArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeLoad(long arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeMonitorEnter(long arg1, Object arg2) {
            log(Operation.AdviseBeforeMonitorEnter.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeMonitorEnter(long arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeMonitorExit(long arg1, Object arg2) {
            log(Operation.AdviseBeforeMonitorExit.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeMonitorExit(long arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeOperation(long arg1, int arg2, double arg3, double arg4) {
            log(Operation.AdviseBeforeOperation.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforeOperation(long arg1, int arg2, double arg3, double arg4);

        @INLINE
        public final void logAdviseBeforeOperation(long arg1, int arg2, float arg3, float arg4) {
            log(Operation.AdviseBeforeOperation2.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforeOperation(long arg1, int arg2, float arg3, float arg4);

        @INLINE
        public final void logAdviseBeforeOperation(long arg1, int arg2, long arg3, long arg4) {
            log(Operation.AdviseBeforeOperation3.ordinal(), longArg(arg1), intArg(arg2), longArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforeOperation(long arg1, int arg2, long arg3, long arg4);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, Object arg2, int arg3, double arg4) {
            log(Operation.AdviseBeforePutField.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, double arg4);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, Object arg2, int arg3, float arg4) {
            log(Operation.AdviseBeforePutField2.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, float arg4);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, Object arg2, int arg3, Object arg4) {
            log(Operation.AdviseBeforePutField3.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, Object arg2, int arg3, long arg4) {
            log(Operation.AdviseBeforePutField4.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforePutField(long arg1, Object arg2, int arg3, long arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, Object arg2, int arg3, double arg4) {
            log(Operation.AdviseBeforePutStatic.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, double arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, Object arg2, int arg3, float arg4) {
            log(Operation.AdviseBeforePutStatic2.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, float arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, Object arg2, int arg3, Object arg4) {
            log(Operation.AdviseBeforePutStatic3.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), objectArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, Object arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, Object arg2, int arg3, long arg4) {
            log(Operation.AdviseBeforePutStatic4.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(long arg1, Object arg2, int arg3, long arg4);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1) {
            log(Operation.AdviseBeforeReturn.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeReturn(long arg1);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, double arg2) {
            log(Operation.AdviseBeforeReturn2.ordinal(), longArg(arg1), doubleArg(arg2));
        }
        protected abstract void traceAdviseBeforeReturn(long arg1, double arg2);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, float arg2) {
            log(Operation.AdviseBeforeReturn3.ordinal(), longArg(arg1), floatArg(arg2));
        }
        protected abstract void traceAdviseBeforeReturn(long arg1, float arg2);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, Object arg2) {
            log(Operation.AdviseBeforeReturn4.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeReturn(long arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, long arg2) {
            log(Operation.AdviseBeforeReturn5.ordinal(), longArg(arg1), longArg(arg2));
        }
        protected abstract void traceAdviseBeforeReturn(long arg1, long arg2);

        @INLINE
        public final void logAdviseBeforeReturnByThrow(long arg1, Throwable arg2, int arg3) {
            log(Operation.AdviseBeforeReturnByThrow.ordinal(), longArg(arg1), objectArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturnByThrow(long arg1, Throwable arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeStackAdjust(long arg1, int arg2) {
            log(Operation.AdviseBeforeStackAdjust.ordinal(), longArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeStackAdjust(long arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeStore.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeStore(long arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeStore2.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeStore(long arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeStore3.ordinal(), longArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeStore(long arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforeStore4.ordinal(), longArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeStore(long arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeThreadStarting(long arg1, VmThread arg2) {
            log(Operation.AdviseBeforeThreadStarting.ordinal(), longArg(arg1), vmThreadArg(arg2));
        }
        protected abstract void traceAdviseBeforeThreadStarting(long arg1, VmThread arg2);

        @INLINE
        public final void logAdviseBeforeThreadTerminating(long arg1, VmThread arg2) {
            log(Operation.AdviseBeforeThreadTerminating.ordinal(), longArg(arg1), vmThreadArg(arg2));
        }
        protected abstract void traceAdviseBeforeThreadTerminating(long arg1, VmThread arg2);

        @INLINE
        public final void logAdviseBeforeThrow(long arg1, Object arg2) {
            log(Operation.AdviseBeforeThrow.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeThrow(long arg1, Object arg2);

        @INLINE
        public final void logUnseenObject(long arg1, Object arg2) {
            log(Operation.UnseenObject.ordinal(), longArg(arg1), objectArg(arg2));
        }
        protected abstract void traceUnseenObject(long arg1, Object arg2);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //AdviseAfterGC
                    traceAdviseAfterGC(toLong(r, 1));
                    break;
                }
                case 1: { //AdviseAfterInvokeInterface
                    traceAdviseAfterInvokeInterface(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 2: { //AdviseAfterInvokeSpecial
                    traceAdviseAfterInvokeSpecial(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 3: { //AdviseAfterInvokeStatic
                    traceAdviseAfterInvokeStatic(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 4: { //AdviseAfterInvokeVirtual
                    traceAdviseAfterInvokeVirtual(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 5: { //AdviseAfterMethodEntry
                    traceAdviseAfterMethodEntry(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 6: { //AdviseAfterMultiNewArray
                    traceAdviseAfterMultiNewArray(toLong(r, 1), toObject(r, 2), toIntArray(r, 3));
                    break;
                }
                case 7: { //AdviseAfterNew
                    traceAdviseAfterNew(toLong(r, 1), toObject(r, 2));
                    break;
                }
                case 8: { //AdviseAfterNewArray
                    traceAdviseAfterNewArray(toLong(r, 1), toObject(r, 2), toInt(r, 3));
                    break;
                }
                case 9: { //AdviseBeforeArrayLength
                    traceAdviseBeforeArrayLength(toLong(r, 1), toObject(r, 2), toInt(r, 3));
                    break;
                }
                case 10: { //AdviseBeforeArrayLoad
                    traceAdviseBeforeArrayLoad(toLong(r, 1), toObject(r, 2), toInt(r, 3));
                    break;
                }
                case 11: { //AdviseBeforeArrayStore
                    traceAdviseBeforeArrayStore(toLong(r, 1), toObject(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 12: { //AdviseBeforeArrayStore2
                    traceAdviseBeforeArrayStore(toLong(r, 1), toObject(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 13: { //AdviseBeforeArrayStore3
                    traceAdviseBeforeArrayStore(toLong(r, 1), toObject(r, 2), toInt(r, 3), toObject(r, 4));
                    break;
                }
                case 14: { //AdviseBeforeArrayStore4
                    traceAdviseBeforeArrayStore(toLong(r, 1), toObject(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 15: { //AdviseBeforeBytecode
                    traceAdviseBeforeBytecode(toLong(r, 1), toInt(r, 2));
                    break;
                }
                case 16: { //AdviseBeforeCheckCast
                    traceAdviseBeforeCheckCast(toLong(r, 1), toObject(r, 2), toObject(r, 3));
                    break;
                }
                case 17: { //AdviseBeforeConstLoad
                    traceAdviseBeforeConstLoad(toLong(r, 1), toDouble(r, 2));
                    break;
                }
                case 18: { //AdviseBeforeConstLoad2
                    traceAdviseBeforeConstLoad(toLong(r, 1), toFloat(r, 2));
                    break;
                }
                case 19: { //AdviseBeforeConstLoad3
                    traceAdviseBeforeConstLoad(toLong(r, 1), toObject(r, 2));
                    break;
                }
                case 20: { //AdviseBeforeConstLoad4
                    traceAdviseBeforeConstLoad(toLong(r, 1), toLong(r, 2));
                    break;
                }
                case 21: { //AdviseBeforeConversion
                    traceAdviseBeforeConversion(toLong(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 22: { //AdviseBeforeConversion2
                    traceAdviseBeforeConversion(toLong(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 23: { //AdviseBeforeConversion3
                    traceAdviseBeforeConversion(toLong(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 24: { //AdviseBeforeGC
                    traceAdviseBeforeGC(toLong(r, 1));
                    break;
                }
                case 25: { //AdviseBeforeGetField
                    traceAdviseBeforeGetField(toLong(r, 1), toObject(r, 2), toInt(r, 3));
                    break;
                }
                case 26: { //AdviseBeforeGetStatic
                    traceAdviseBeforeGetStatic(toLong(r, 1), toObject(r, 2), toInt(r, 3));
                    break;
                }
                case 27: { //AdviseBeforeIf
                    traceAdviseBeforeIf(toLong(r, 1), toInt(r, 2), toInt(r, 3), toInt(r, 4));
                    break;
                }
                case 28: { //AdviseBeforeIf2
                    traceAdviseBeforeIf(toLong(r, 1), toInt(r, 2), toObject(r, 3), toObject(r, 4));
                    break;
                }
                case 29: { //AdviseBeforeInstanceOf
                    traceAdviseBeforeInstanceOf(toLong(r, 1), toObject(r, 2), toObject(r, 3));
                    break;
                }
                case 30: { //AdviseBeforeInvokeInterface
                    traceAdviseBeforeInvokeInterface(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 31: { //AdviseBeforeInvokeSpecial
                    traceAdviseBeforeInvokeSpecial(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 32: { //AdviseBeforeInvokeStatic
                    traceAdviseBeforeInvokeStatic(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 33: { //AdviseBeforeInvokeVirtual
                    traceAdviseBeforeInvokeVirtual(toLong(r, 1), toObject(r, 2), toMethodActor(r, 3));
                    break;
                }
                case 34: { //AdviseBeforeLoad
                    traceAdviseBeforeLoad(toLong(r, 1), toInt(r, 2));
                    break;
                }
                case 35: { //AdviseBeforeMonitorEnter
                    traceAdviseBeforeMonitorEnter(toLong(r, 1), toObject(r, 2));
                    break;
                }
                case 36: { //AdviseBeforeMonitorExit
                    traceAdviseBeforeMonitorExit(toLong(r, 1), toObject(r, 2));
                    break;
                }
                case 37: { //AdviseBeforeOperation
                    traceAdviseBeforeOperation(toLong(r, 1), toInt(r, 2), toDouble(r, 3), toDouble(r, 4));
                    break;
                }
                case 38: { //AdviseBeforeOperation2
                    traceAdviseBeforeOperation(toLong(r, 1), toInt(r, 2), toFloat(r, 3), toFloat(r, 4));
                    break;
                }
                case 39: { //AdviseBeforeOperation3
                    traceAdviseBeforeOperation(toLong(r, 1), toInt(r, 2), toLong(r, 3), toLong(r, 4));
                    break;
                }
                case 40: { //AdviseBeforePutField
                    traceAdviseBeforePutField(toLong(r, 1), toObject(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 41: { //AdviseBeforePutField2
                    traceAdviseBeforePutField(toLong(r, 1), toObject(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 42: { //AdviseBeforePutField3
                    traceAdviseBeforePutField(toLong(r, 1), toObject(r, 2), toInt(r, 3), toObject(r, 4));
                    break;
                }
                case 43: { //AdviseBeforePutField4
                    traceAdviseBeforePutField(toLong(r, 1), toObject(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 44: { //AdviseBeforePutStatic
                    traceAdviseBeforePutStatic(toLong(r, 1), toObject(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 45: { //AdviseBeforePutStatic2
                    traceAdviseBeforePutStatic(toLong(r, 1), toObject(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 46: { //AdviseBeforePutStatic3
                    traceAdviseBeforePutStatic(toLong(r, 1), toObject(r, 2), toInt(r, 3), toObject(r, 4));
                    break;
                }
                case 47: { //AdviseBeforePutStatic4
                    traceAdviseBeforePutStatic(toLong(r, 1), toObject(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 48: { //AdviseBeforeReturn
                    traceAdviseBeforeReturn(toLong(r, 1));
                    break;
                }
                case 49: { //AdviseBeforeReturn2
                    traceAdviseBeforeReturn(toLong(r, 1), toDouble(r, 2));
                    break;
                }
                case 50: { //AdviseBeforeReturn3
                    traceAdviseBeforeReturn(toLong(r, 1), toFloat(r, 2));
                    break;
                }
                case 51: { //AdviseBeforeReturn4
                    traceAdviseBeforeReturn(toLong(r, 1), toObject(r, 2));
                    break;
                }
                case 52: { //AdviseBeforeReturn5
                    traceAdviseBeforeReturn(toLong(r, 1), toLong(r, 2));
                    break;
                }
                case 53: { //AdviseBeforeReturnByThrow
                    traceAdviseBeforeReturnByThrow(toLong(r, 1), toThrowable(r, 2), toInt(r, 3));
                    break;
                }
                case 54: { //AdviseBeforeStackAdjust
                    traceAdviseBeforeStackAdjust(toLong(r, 1), toInt(r, 2));
                    break;
                }
                case 55: { //AdviseBeforeStore
                    traceAdviseBeforeStore(toLong(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 56: { //AdviseBeforeStore2
                    traceAdviseBeforeStore(toLong(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 57: { //AdviseBeforeStore3
                    traceAdviseBeforeStore(toLong(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 58: { //AdviseBeforeStore4
                    traceAdviseBeforeStore(toLong(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 59: { //AdviseBeforeThreadStarting
                    traceAdviseBeforeThreadStarting(toLong(r, 1), toVmThread(r, 2));
                    break;
                }
                case 60: { //AdviseBeforeThreadTerminating
                    traceAdviseBeforeThreadTerminating(toLong(r, 1), toVmThread(r, 2));
                    break;
                }
                case 61: { //AdviseBeforeThrow
                    traceAdviseBeforeThrow(toLong(r, 1), toObject(r, 2));
                    break;
                }
                case 62: { //UnseenObject
                    traceUnseenObject(toLong(r, 1), toObject(r, 2));
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
