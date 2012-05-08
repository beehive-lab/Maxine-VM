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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
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
 * The {@link VMLogger} tracing support is used to handle log flushing.
 */
public class VMAVMLogger {

    public static final VMAVMLoggerImpl logger = new VMAVMLoggerImpl();

    private static LoggingVMAdviceHandler handler;

    public static class VMAVMLoggerImpl extends VMAVMLoggerAuto {

        protected VMAVMLoggerImpl() {
            super("VMAdvice");
        }

        public static void setLogHandler(LoggingVMAdviceHandler h) {
            handler = h;
        }

// START GENERATED INTERFACE
// EDIT AND RUN VMAVMLoggerGenerator.main() TO MODIFY

        @Override
        protected void traceAdviseBeforeGC() {
            handler.adviseBeforeGC();
        }
        @Override
        protected void traceAdviseAfterGC() {
            handler.adviseAfterGC();
        }
        @Override
        protected void traceAdviseBeforeThreadStarting(VmThread arg1) {
            handler.adviseBeforeThreadStarting(arg1);
        }
        @Override
        protected void traceAdviseBeforeThreadTerminating(VmThread arg1) {
            handler.adviseBeforeThreadTerminating(arg1);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(long arg1) {
            handler.adviseBeforeConstLoad(arg1);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(Object arg1) {
            handler.adviseBeforeConstLoad(arg1);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(float arg1) {
            handler.adviseBeforeConstLoad(arg1);
        }
        @Override
        protected void traceAdviseBeforeConstLoad(double arg1) {
            handler.adviseBeforeConstLoad(arg1);
        }
        @Override
        protected void traceAdviseBeforeLoad(int arg1) {
            handler.adviseBeforeLoad(arg1);
        }
        @Override
        protected void traceAdviseBeforeArrayLoad(Object arg1, int arg2) {
            handler.adviseBeforeArrayLoad(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeStore(int arg1, long arg2) {
            handler.adviseBeforeStore(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeStore(int arg1, float arg2) {
            handler.adviseBeforeStore(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeStore(int arg1, double arg2) {
            handler.adviseBeforeStore(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeStore(int arg1, Object arg2) {
            handler.adviseBeforeStore(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
            handler.adviseBeforeArrayStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
            handler.adviseBeforeArrayStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
            handler.adviseBeforeArrayStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
            handler.adviseBeforeArrayStore(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeStackAdjust(int arg1) {
            handler.adviseBeforeStackAdjust(arg1);
        }
        @Override
        protected void traceAdviseBeforeOperation(int arg1, long arg2, long arg3) {
            handler.adviseBeforeOperation(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeOperation(int arg1, float arg2, float arg3) {
            handler.adviseBeforeOperation(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeOperation(int arg1, double arg2, double arg3) {
            handler.adviseBeforeOperation(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeConversion(int arg1, float arg2) {
            handler.adviseBeforeConversion(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeConversion(int arg1, long arg2) {
            handler.adviseBeforeConversion(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeConversion(int arg1, double arg2) {
            handler.adviseBeforeConversion(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeIf(int arg1, int arg2, int arg3) {
            handler.adviseBeforeIf(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeIf(int arg1, Object arg2, Object arg3) {
            handler.adviseBeforeIf(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeBytecode(int arg1) {
            handler.adviseBeforeBytecode(arg1);
        }
        @Override
        protected void traceAdviseBeforeReturn() {
            handler.adviseBeforeReturn();
        }
        @Override
        protected void traceAdviseBeforeReturn(long arg1) {
            handler.adviseBeforeReturn(arg1);
        }
        @Override
        protected void traceAdviseBeforeReturn(float arg1) {
            handler.adviseBeforeReturn(arg1);
        }
        @Override
        protected void traceAdviseBeforeReturn(double arg1) {
            handler.adviseBeforeReturn(arg1);
        }
        @Override
        protected void traceAdviseBeforeReturn(Object arg1) {
            handler.adviseBeforeReturn(arg1);
        }
        @Override
        protected void traceAdviseBeforeGetStatic(Object arg1, int arg2) {
            handler.adviseBeforeGetStatic(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
            handler.adviseBeforePutStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutStatic(Object arg1, int arg2, float arg3) {
            handler.adviseBeforePutStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutStatic(Object arg1, int arg2, double arg3) {
            handler.adviseBeforePutStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutStatic(Object arg1, int arg2, long arg3) {
            handler.adviseBeforePutStatic(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeGetField(Object arg1, int arg2) {
            handler.adviseBeforeGetField(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforePutField(Object arg1, int arg2, Object arg3) {
            handler.adviseBeforePutField(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutField(Object arg1, int arg2, float arg3) {
            handler.adviseBeforePutField(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutField(Object arg1, int arg2, double arg3) {
            handler.adviseBeforePutField(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforePutField(Object arg1, int arg2, long arg3) {
            handler.adviseBeforePutField(arg1, arg2, arg3);
        }
        @Override
        protected void traceAdviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
            handler.adviseBeforeInvokeVirtual(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
            handler.adviseBeforeInvokeSpecial(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
            handler.adviseBeforeInvokeStatic(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
            handler.adviseBeforeInvokeInterface(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeArrayLength(Object arg1, int arg2) {
            handler.adviseBeforeArrayLength(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeThrow(Object arg1) {
            handler.adviseBeforeThrow(arg1);
        }
        @Override
        protected void traceAdviseBeforeCheckCast(Object arg1, Object arg2) {
            handler.adviseBeforeCheckCast(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeInstanceOf(Object arg1, Object arg2) {
            handler.adviseBeforeInstanceOf(arg1, arg2);
        }
        @Override
        protected void traceAdviseBeforeMonitorEnter(Object arg1) {
            handler.adviseBeforeMonitorEnter(arg1);
        }
        @Override
        protected void traceAdviseBeforeMonitorExit(Object arg1) {
            handler.adviseBeforeMonitorExit(arg1);
        }
        @Override
        protected void traceAdviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
            handler.adviseAfterInvokeVirtual(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
            handler.adviseAfterInvokeSpecial(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
            handler.adviseAfterInvokeStatic(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
            handler.adviseAfterInvokeInterface(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterNew(Object arg1) {
            handler.adviseAfterNew(arg1);
        }
        @Override
        protected void traceAdviseAfterNewArray(Object arg1, int arg2) {
            handler.adviseAfterNewArray(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterMultiNewArray(Object arg1, int[] arg2) {
            handler.adviseAfterMultiNewArray(arg1, arg2);
        }
        @Override
        protected void traceAdviseAfterMethodEntry(Object arg1, MethodActor arg2) {
            handler.adviseAfterMethodEntry(arg1, arg2);
        }
        @Override
        protected void traceUnseenObject(Object arg1) {
            handler.unseenObject(arg1);
        }
    }

    @HOSTED_ONLY
    @VMLoggerInterface(hidden = true)
    private interface VMAVMLoggerInterface {
        void adviseBeforeGC();

        void adviseAfterGC();

        void adviseBeforeThreadStarting(VmThread arg1);

        void adviseBeforeThreadTerminating(VmThread arg1);

        void adviseBeforeConstLoad(long arg1);

        void adviseBeforeConstLoad(Object arg1);

        void adviseBeforeConstLoad(float arg1);

        void adviseBeforeConstLoad(double arg1);

        void adviseBeforeLoad(int arg1);

        void adviseBeforeArrayLoad(Object arg1, int arg2);

        void adviseBeforeStore(int arg1, long arg2);

        void adviseBeforeStore(int arg1, float arg2);

        void adviseBeforeStore(int arg1, double arg2);

        void adviseBeforeStore(int arg1, Object arg2);

        void adviseBeforeArrayStore(Object arg1, int arg2, float arg3);

        void adviseBeforeArrayStore(Object arg1, int arg2, long arg3);

        void adviseBeforeArrayStore(Object arg1, int arg2, double arg3);

        void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3);

        void adviseBeforeStackAdjust(int arg1);

        void adviseBeforeOperation(int arg1, long arg2, long arg3);

        void adviseBeforeOperation(int arg1, float arg2, float arg3);

        void adviseBeforeOperation(int arg1, double arg2, double arg3);

        void adviseBeforeConversion(int arg1, float arg2);

        void adviseBeforeConversion(int arg1, long arg2);

        void adviseBeforeConversion(int arg1, double arg2);

        void adviseBeforeIf(int arg1, int arg2, int arg3);

        void adviseBeforeIf(int arg1, Object arg2, Object arg3);

        void adviseBeforeBytecode(int arg1);

        void adviseBeforeReturn();

        void adviseBeforeReturn(long arg1);

        void adviseBeforeReturn(float arg1);

        void adviseBeforeReturn(double arg1);

        void adviseBeforeReturn(Object arg1);

        void adviseBeforeGetStatic(Object arg1, int arg2);

        void adviseBeforePutStatic(Object arg1, int arg2, Object arg3);

        void adviseBeforePutStatic(Object arg1, int arg2, float arg3);

        void adviseBeforePutStatic(Object arg1, int arg2, double arg3);

        void adviseBeforePutStatic(Object arg1, int arg2, long arg3);

        void adviseBeforeGetField(Object arg1, int arg2);

        void adviseBeforePutField(Object arg1, int arg2, Object arg3);

        void adviseBeforePutField(Object arg1, int arg2, float arg3);

        void adviseBeforePutField(Object arg1, int arg2, double arg3);

        void adviseBeforePutField(Object arg1, int arg2, long arg3);

        void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2);

        void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2);

        void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2);

        void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2);

        void adviseBeforeArrayLength(Object arg1, int arg2);

        void adviseBeforeThrow(Object arg1);

        void adviseBeforeCheckCast(Object arg1, Object arg2);

        void adviseBeforeInstanceOf(Object arg1, Object arg2);

        void adviseBeforeMonitorEnter(Object arg1);

        void adviseBeforeMonitorExit(Object arg1);

        void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2);

        void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2);

        void adviseAfterInvokeStatic(Object arg1, MethodActor arg2);

        void adviseAfterInvokeInterface(Object arg1, MethodActor arg2);

        void adviseAfterNew(Object arg1);

        void adviseAfterNewArray(Object arg1, int arg2);

        void adviseAfterMultiNewArray(Object arg1, int[] arg2);

        void adviseAfterMethodEntry(Object arg1, MethodActor arg2);

        void unseenObject(Object arg1);

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
            AdviseBeforeReturn4, AdviseBeforeReturn5, AdviseBeforeStackAdjust, AdviseBeforeStore,
            AdviseBeforeStore2, AdviseBeforeStore3, AdviseBeforeStore4, AdviseBeforeThreadStarting,
            AdviseBeforeThreadTerminating, AdviseBeforeThrow, UnseenObject;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x0, 0x1, 0x1, 0x1, 0x1, 0x1, 0x3, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x5,
            0x1, 0x0, 0x3, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x1, 0x1, 0x0,
            0x6, 0x3, 0x1, 0x1, 0x1, 0x1, 0x0, 0x1, 0x1, 0x0, 0x0, 0x0, 0x1, 0x1,
            0x5, 0x1, 0x1, 0x1, 0x5, 0x1, 0x0, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0,
            0x0, 0x2, 0x0, 0x0, 0x0, 0x1, 0x1};

        protected VMAVMLoggerAuto(String name) {
            super(name, Operation.VALUES.length, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logAdviseAfterGC() {
            log(Operation.AdviseAfterGC.ordinal());
        }
        protected abstract void traceAdviseAfterGC();

        @INLINE
        public final void logAdviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
            log(Operation.AdviseAfterInvokeInterface.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseAfterInvokeInterface(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
            log(Operation.AdviseAfterInvokeSpecial.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseAfterInvokeSpecial(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
            log(Operation.AdviseAfterInvokeStatic.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseAfterInvokeStatic(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
            log(Operation.AdviseAfterInvokeVirtual.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseAfterInvokeVirtual(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseAfterMethodEntry(Object arg1, MethodActor arg2) {
            log(Operation.AdviseAfterMethodEntry.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseAfterMethodEntry(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseAfterMultiNewArray(Object arg1, int[] arg2) {
            log(Operation.AdviseAfterMultiNewArray.ordinal(), objectArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseAfterMultiNewArray(Object arg1, int[] arg2);

        @INLINE
        public final void logAdviseAfterNew(Object arg1) {
            log(Operation.AdviseAfterNew.ordinal(), objectArg(arg1));
        }
        protected abstract void traceAdviseAfterNew(Object arg1);

        @INLINE
        public final void logAdviseAfterNewArray(Object arg1, int arg2) {
            log(Operation.AdviseAfterNewArray.ordinal(), objectArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseAfterNewArray(Object arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeArrayLength(Object arg1, int arg2) {
            log(Operation.AdviseBeforeArrayLength.ordinal(), objectArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeArrayLength(Object arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeArrayLoad(Object arg1, int arg2) {
            log(Operation.AdviseBeforeArrayLoad.ordinal(), objectArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeArrayLoad(Object arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeArrayStore.ordinal(), objectArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeArrayStore(Object arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeArrayStore2.ordinal(), objectArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeArrayStore(Object arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforeArrayStore3.ordinal(), objectArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeArrayStore(Object arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforeArrayStore4.ordinal(), objectArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeArrayStore(Object arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeBytecode(int arg1) {
            log(Operation.AdviseBeforeBytecode.ordinal(), intArg(arg1));
        }
        protected abstract void traceAdviseBeforeBytecode(int arg1);

        @INLINE
        public final void logAdviseBeforeCheckCast(Object arg1, Object arg2) {
            log(Operation.AdviseBeforeCheckCast.ordinal(), objectArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeCheckCast(Object arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeConstLoad(double arg1) {
            log(Operation.AdviseBeforeConstLoad.ordinal(), doubleArg(arg1));
        }
        protected abstract void traceAdviseBeforeConstLoad(double arg1);

        @INLINE
        public final void logAdviseBeforeConstLoad(float arg1) {
            log(Operation.AdviseBeforeConstLoad2.ordinal(), floatArg(arg1));
        }
        protected abstract void traceAdviseBeforeConstLoad(float arg1);

        @INLINE
        public final void logAdviseBeforeConstLoad(Object arg1) {
            log(Operation.AdviseBeforeConstLoad3.ordinal(), objectArg(arg1));
        }
        protected abstract void traceAdviseBeforeConstLoad(Object arg1);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1) {
            log(Operation.AdviseBeforeConstLoad4.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeConstLoad(long arg1);

        @INLINE
        public final void logAdviseBeforeConversion(int arg1, double arg2) {
            log(Operation.AdviseBeforeConversion.ordinal(), intArg(arg1), doubleArg(arg2));
        }
        protected abstract void traceAdviseBeforeConversion(int arg1, double arg2);

        @INLINE
        public final void logAdviseBeforeConversion(int arg1, float arg2) {
            log(Operation.AdviseBeforeConversion2.ordinal(), intArg(arg1), floatArg(arg2));
        }
        protected abstract void traceAdviseBeforeConversion(int arg1, float arg2);

        @INLINE
        public final void logAdviseBeforeConversion(int arg1, long arg2) {
            log(Operation.AdviseBeforeConversion3.ordinal(), intArg(arg1), longArg(arg2));
        }
        protected abstract void traceAdviseBeforeConversion(int arg1, long arg2);

        @INLINE
        public final void logAdviseBeforeGC() {
            log(Operation.AdviseBeforeGC.ordinal());
        }
        protected abstract void traceAdviseBeforeGC();

        @INLINE
        public final void logAdviseBeforeGetField(Object arg1, int arg2) {
            log(Operation.AdviseBeforeGetField.ordinal(), objectArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeGetField(Object arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeGetStatic(Object arg1, int arg2) {
            log(Operation.AdviseBeforeGetStatic.ordinal(), objectArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeGetStatic(Object arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeIf(int arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeIf.ordinal(), intArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeIf(int arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeIf(int arg1, Object arg2, Object arg3) {
            log(Operation.AdviseBeforeIf2.ordinal(), intArg(arg1), objectArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforeIf(int arg1, Object arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforeInstanceOf(Object arg1, Object arg2) {
            log(Operation.AdviseBeforeInstanceOf.ordinal(), objectArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeInstanceOf(Object arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
            log(Operation.AdviseBeforeInvokeInterface.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseBeforeInvokeInterface(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
            log(Operation.AdviseBeforeInvokeSpecial.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseBeforeInvokeSpecial(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
            log(Operation.AdviseBeforeInvokeStatic.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseBeforeInvokeStatic(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
            log(Operation.AdviseBeforeInvokeVirtual.ordinal(), objectArg(arg1), methodActorArg(arg2));
        }
        protected abstract void traceAdviseBeforeInvokeVirtual(Object arg1, MethodActor arg2);

        @INLINE
        public final void logAdviseBeforeLoad(int arg1) {
            log(Operation.AdviseBeforeLoad.ordinal(), intArg(arg1));
        }
        protected abstract void traceAdviseBeforeLoad(int arg1);

        @INLINE
        public final void logAdviseBeforeMonitorEnter(Object arg1) {
            log(Operation.AdviseBeforeMonitorEnter.ordinal(), objectArg(arg1));
        }
        protected abstract void traceAdviseBeforeMonitorEnter(Object arg1);

        @INLINE
        public final void logAdviseBeforeMonitorExit(Object arg1) {
            log(Operation.AdviseBeforeMonitorExit.ordinal(), objectArg(arg1));
        }
        protected abstract void traceAdviseBeforeMonitorExit(Object arg1);

        @INLINE
        public final void logAdviseBeforeOperation(int arg1, double arg2, double arg3) {
            log(Operation.AdviseBeforeOperation.ordinal(), intArg(arg1), doubleArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeOperation(int arg1, double arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeOperation(int arg1, float arg2, float arg3) {
            log(Operation.AdviseBeforeOperation2.ordinal(), intArg(arg1), floatArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeOperation(int arg1, float arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeOperation(int arg1, long arg2, long arg3) {
            log(Operation.AdviseBeforeOperation3.ordinal(), intArg(arg1), longArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeOperation(int arg1, long arg2, long arg3);

        @INLINE
        public final void logAdviseBeforePutField(Object arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforePutField.ordinal(), objectArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforePutField(Object arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforePutField(Object arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforePutField2.ordinal(), objectArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforePutField(Object arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforePutField(Object arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforePutField3.ordinal(), objectArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforePutField(Object arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforePutField(Object arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforePutField4.ordinal(), objectArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforePutField(Object arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforePutStatic(Object arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforePutStatic.ordinal(), objectArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforePutStatic(Object arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforePutStatic(Object arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforePutStatic2.ordinal(), objectArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforePutStatic(Object arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
            log(Operation.AdviseBeforePutStatic3.ordinal(), objectArg(arg1), intArg(arg2), objectArg(arg3));
        }
        protected abstract void traceAdviseBeforePutStatic(Object arg1, int arg2, Object arg3);

        @INLINE
        public final void logAdviseBeforePutStatic(Object arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforePutStatic4.ordinal(), objectArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforePutStatic(Object arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeReturn() {
            log(Operation.AdviseBeforeReturn.ordinal());
        }
        protected abstract void traceAdviseBeforeReturn();

        @INLINE
        public final void logAdviseBeforeReturn(double arg1) {
            log(Operation.AdviseBeforeReturn2.ordinal(), doubleArg(arg1));
        }
        protected abstract void traceAdviseBeforeReturn(double arg1);

        @INLINE
        public final void logAdviseBeforeReturn(float arg1) {
            log(Operation.AdviseBeforeReturn3.ordinal(), floatArg(arg1));
        }
        protected abstract void traceAdviseBeforeReturn(float arg1);

        @INLINE
        public final void logAdviseBeforeReturn(Object arg1) {
            log(Operation.AdviseBeforeReturn4.ordinal(), objectArg(arg1));
        }
        protected abstract void traceAdviseBeforeReturn(Object arg1);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1) {
            log(Operation.AdviseBeforeReturn5.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeReturn(long arg1);

        @INLINE
        public final void logAdviseBeforeStackAdjust(int arg1) {
            log(Operation.AdviseBeforeStackAdjust.ordinal(), intArg(arg1));
        }
        protected abstract void traceAdviseBeforeStackAdjust(int arg1);

        @INLINE
        public final void logAdviseBeforeStore(int arg1, double arg2) {
            log(Operation.AdviseBeforeStore.ordinal(), intArg(arg1), doubleArg(arg2));
        }
        protected abstract void traceAdviseBeforeStore(int arg1, double arg2);

        @INLINE
        public final void logAdviseBeforeStore(int arg1, float arg2) {
            log(Operation.AdviseBeforeStore2.ordinal(), intArg(arg1), floatArg(arg2));
        }
        protected abstract void traceAdviseBeforeStore(int arg1, float arg2);

        @INLINE
        public final void logAdviseBeforeStore(int arg1, Object arg2) {
            log(Operation.AdviseBeforeStore3.ordinal(), intArg(arg1), objectArg(arg2));
        }
        protected abstract void traceAdviseBeforeStore(int arg1, Object arg2);

        @INLINE
        public final void logAdviseBeforeStore(int arg1, long arg2) {
            log(Operation.AdviseBeforeStore4.ordinal(), intArg(arg1), longArg(arg2));
        }
        protected abstract void traceAdviseBeforeStore(int arg1, long arg2);

        @INLINE
        public final void logAdviseBeforeThreadStarting(VmThread arg1) {
            log(Operation.AdviseBeforeThreadStarting.ordinal(), vmThreadArg(arg1));
        }
        protected abstract void traceAdviseBeforeThreadStarting(VmThread arg1);

        @INLINE
        public final void logAdviseBeforeThreadTerminating(VmThread arg1) {
            log(Operation.AdviseBeforeThreadTerminating.ordinal(), vmThreadArg(arg1));
        }
        protected abstract void traceAdviseBeforeThreadTerminating(VmThread arg1);

        @INLINE
        public final void logAdviseBeforeThrow(Object arg1) {
            log(Operation.AdviseBeforeThrow.ordinal(), objectArg(arg1));
        }
        protected abstract void traceAdviseBeforeThrow(Object arg1);

        @INLINE
        public final void logUnseenObject(Object arg1) {
            log(Operation.UnseenObject.ordinal(), objectArg(arg1));
        }
        protected abstract void traceUnseenObject(Object arg1);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //AdviseAfterGC
                    traceAdviseAfterGC();
                    break;
                }
                case 1: { //AdviseAfterInvokeInterface
                    traceAdviseAfterInvokeInterface(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 2: { //AdviseAfterInvokeSpecial
                    traceAdviseAfterInvokeSpecial(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 3: { //AdviseAfterInvokeStatic
                    traceAdviseAfterInvokeStatic(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 4: { //AdviseAfterInvokeVirtual
                    traceAdviseAfterInvokeVirtual(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 5: { //AdviseAfterMethodEntry
                    traceAdviseAfterMethodEntry(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 6: { //AdviseAfterMultiNewArray
                    traceAdviseAfterMultiNewArray(toObject(r, 1), toIntArray(r, 2));
                    break;
                }
                case 7: { //AdviseAfterNew
                    traceAdviseAfterNew(toObject(r, 1));
                    break;
                }
                case 8: { //AdviseAfterNewArray
                    traceAdviseAfterNewArray(toObject(r, 1), toInt(r, 2));
                    break;
                }
                case 9: { //AdviseBeforeArrayLength
                    traceAdviseBeforeArrayLength(toObject(r, 1), toInt(r, 2));
                    break;
                }
                case 10: { //AdviseBeforeArrayLoad
                    traceAdviseBeforeArrayLoad(toObject(r, 1), toInt(r, 2));
                    break;
                }
                case 11: { //AdviseBeforeArrayStore
                    traceAdviseBeforeArrayStore(toObject(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 12: { //AdviseBeforeArrayStore2
                    traceAdviseBeforeArrayStore(toObject(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 13: { //AdviseBeforeArrayStore3
                    traceAdviseBeforeArrayStore(toObject(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 14: { //AdviseBeforeArrayStore4
                    traceAdviseBeforeArrayStore(toObject(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 15: { //AdviseBeforeBytecode
                    traceAdviseBeforeBytecode(toInt(r, 1));
                    break;
                }
                case 16: { //AdviseBeforeCheckCast
                    traceAdviseBeforeCheckCast(toObject(r, 1), toObject(r, 2));
                    break;
                }
                case 17: { //AdviseBeforeConstLoad
                    traceAdviseBeforeConstLoad(toDouble(r, 1));
                    break;
                }
                case 18: { //AdviseBeforeConstLoad2
                    traceAdviseBeforeConstLoad(toFloat(r, 1));
                    break;
                }
                case 19: { //AdviseBeforeConstLoad3
                    traceAdviseBeforeConstLoad(toObject(r, 1));
                    break;
                }
                case 20: { //AdviseBeforeConstLoad4
                    traceAdviseBeforeConstLoad(toLong(r, 1));
                    break;
                }
                case 21: { //AdviseBeforeConversion
                    traceAdviseBeforeConversion(toInt(r, 1), toDouble(r, 2));
                    break;
                }
                case 22: { //AdviseBeforeConversion2
                    traceAdviseBeforeConversion(toInt(r, 1), toFloat(r, 2));
                    break;
                }
                case 23: { //AdviseBeforeConversion3
                    traceAdviseBeforeConversion(toInt(r, 1), toLong(r, 2));
                    break;
                }
                case 24: { //AdviseBeforeGC
                    traceAdviseBeforeGC();
                    break;
                }
                case 25: { //AdviseBeforeGetField
                    traceAdviseBeforeGetField(toObject(r, 1), toInt(r, 2));
                    break;
                }
                case 26: { //AdviseBeforeGetStatic
                    traceAdviseBeforeGetStatic(toObject(r, 1), toInt(r, 2));
                    break;
                }
                case 27: { //AdviseBeforeIf
                    traceAdviseBeforeIf(toInt(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 28: { //AdviseBeforeIf2
                    traceAdviseBeforeIf(toInt(r, 1), toObject(r, 2), toObject(r, 3));
                    break;
                }
                case 29: { //AdviseBeforeInstanceOf
                    traceAdviseBeforeInstanceOf(toObject(r, 1), toObject(r, 2));
                    break;
                }
                case 30: { //AdviseBeforeInvokeInterface
                    traceAdviseBeforeInvokeInterface(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 31: { //AdviseBeforeInvokeSpecial
                    traceAdviseBeforeInvokeSpecial(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 32: { //AdviseBeforeInvokeStatic
                    traceAdviseBeforeInvokeStatic(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 33: { //AdviseBeforeInvokeVirtual
                    traceAdviseBeforeInvokeVirtual(toObject(r, 1), toMethodActor(r, 2));
                    break;
                }
                case 34: { //AdviseBeforeLoad
                    traceAdviseBeforeLoad(toInt(r, 1));
                    break;
                }
                case 35: { //AdviseBeforeMonitorEnter
                    traceAdviseBeforeMonitorEnter(toObject(r, 1));
                    break;
                }
                case 36: { //AdviseBeforeMonitorExit
                    traceAdviseBeforeMonitorExit(toObject(r, 1));
                    break;
                }
                case 37: { //AdviseBeforeOperation
                    traceAdviseBeforeOperation(toInt(r, 1), toDouble(r, 2), toDouble(r, 3));
                    break;
                }
                case 38: { //AdviseBeforeOperation2
                    traceAdviseBeforeOperation(toInt(r, 1), toFloat(r, 2), toFloat(r, 3));
                    break;
                }
                case 39: { //AdviseBeforeOperation3
                    traceAdviseBeforeOperation(toInt(r, 1), toLong(r, 2), toLong(r, 3));
                    break;
                }
                case 40: { //AdviseBeforePutField
                    traceAdviseBeforePutField(toObject(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 41: { //AdviseBeforePutField2
                    traceAdviseBeforePutField(toObject(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 42: { //AdviseBeforePutField3
                    traceAdviseBeforePutField(toObject(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 43: { //AdviseBeforePutField4
                    traceAdviseBeforePutField(toObject(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 44: { //AdviseBeforePutStatic
                    traceAdviseBeforePutStatic(toObject(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 45: { //AdviseBeforePutStatic2
                    traceAdviseBeforePutStatic(toObject(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 46: { //AdviseBeforePutStatic3
                    traceAdviseBeforePutStatic(toObject(r, 1), toInt(r, 2), toObject(r, 3));
                    break;
                }
                case 47: { //AdviseBeforePutStatic4
                    traceAdviseBeforePutStatic(toObject(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 48: { //AdviseBeforeReturn
                    traceAdviseBeforeReturn();
                    break;
                }
                case 49: { //AdviseBeforeReturn2
                    traceAdviseBeforeReturn(toDouble(r, 1));
                    break;
                }
                case 50: { //AdviseBeforeReturn3
                    traceAdviseBeforeReturn(toFloat(r, 1));
                    break;
                }
                case 51: { //AdviseBeforeReturn4
                    traceAdviseBeforeReturn(toObject(r, 1));
                    break;
                }
                case 52: { //AdviseBeforeReturn5
                    traceAdviseBeforeReturn(toLong(r, 1));
                    break;
                }
                case 53: { //AdviseBeforeStackAdjust
                    traceAdviseBeforeStackAdjust(toInt(r, 1));
                    break;
                }
                case 54: { //AdviseBeforeStore
                    traceAdviseBeforeStore(toInt(r, 1), toDouble(r, 2));
                    break;
                }
                case 55: { //AdviseBeforeStore2
                    traceAdviseBeforeStore(toInt(r, 1), toFloat(r, 2));
                    break;
                }
                case 56: { //AdviseBeforeStore3
                    traceAdviseBeforeStore(toInt(r, 1), toObject(r, 2));
                    break;
                }
                case 57: { //AdviseBeforeStore4
                    traceAdviseBeforeStore(toInt(r, 1), toLong(r, 2));
                    break;
                }
                case 58: { //AdviseBeforeThreadStarting
                    traceAdviseBeforeThreadStarting(toVmThread(r, 1));
                    break;
                }
                case 59: { //AdviseBeforeThreadTerminating
                    traceAdviseBeforeThreadTerminating(toVmThread(r, 1));
                    break;
                }
                case 60: { //AdviseBeforeThrow
                    traceAdviseBeforeThrow(toObject(r, 1));
                    break;
                }
                case 61: { //UnseenObject
                    traceUnseenObject(toObject(r, 1));
                    break;
                }
            }
        }
        static int[] toIntArray(Record r, int argNum) {
            return asIntArray(toObject(r, argNum));
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native int[] asIntArray(Object arg);
    }

// END GENERATED CODE

}
