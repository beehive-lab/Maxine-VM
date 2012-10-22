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

import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;

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
 *
 * Reference valued objects must not be stored in the log as the flushing code might provoke
 * a GC that could corrupt a value in an in-flight log method, so we use {@link ObjectID} etc.
 */

public class VMAVMLogger {

    static final VMAVMLoggerImpl logger = new VMAVMLoggerImpl();

    public static class VMAVMLoggerImpl extends VMAVMLoggerAuto {

        protected VMAVMLoggerImpl() {
            super("VMAdvice");
        }

        VMAVMLoggerStoreAdapter storeAdaptor;

        @NEVER_INLINE
        private VMAVMLoggerStoreAdapter storeAdaptor(int threadId) {
            return (VMAVMLoggerStoreAdapter) storeAdaptor.getStoreAdaptorForThread(threadId);
        }

// START GENERATED INTERFACE
// EDIT AND RUN VMAVMLoggerGenerator.main() TO MODIFY

        @Override
        protected void traceAdviseBeforeGC(int threadId, long time) {
            storeAdaptor(threadId).adviseBeforeGC(time);
        }

        @Override
        protected void traceAdviseAfterGC(int threadId, long time) {
            storeAdaptor(threadId).adviseAfterGC(time);
        }

        @Override
        protected void traceAdviseBeforeThreadStarting(int threadId, long time) {
            storeAdaptor(threadId).adviseBeforeThreadStarting(time);
        }

        @Override
        protected void traceAdviseBeforeThreadTerminating(int threadId, long time) {
            storeAdaptor(threadId).adviseBeforeThreadTerminating(time);
        }

        @Override
        protected void traceAdviseBeforeReturnByThrow(int threadId, long time, int bci, ObjectID arg1, int arg2) {
            storeAdaptor(threadId).adviseBeforeReturnByThrow(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long time, int bci, long arg1) {
            storeAdaptor(threadId).adviseBeforeConstLoad(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long time, int bci, ObjectID arg1) {
            storeAdaptor(threadId).adviseBeforeConstLoad(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long time, int bci, float arg1) {
            storeAdaptor(threadId).adviseBeforeConstLoad(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeConstLoad(int threadId, long time, int bci, double arg1) {
            storeAdaptor(threadId).adviseBeforeConstLoad(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeLoad(int threadId, long time, int bci, int arg1) {
            storeAdaptor(threadId).adviseBeforeLoad(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeArrayLoad(int threadId, long time, int bci, ObjectID arg1, int arg2) {
            storeAdaptor(threadId).adviseBeforeArrayLoad(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeStore(int threadId, long time, int bci, int arg1, long arg2) {
            storeAdaptor(threadId).adviseBeforeStore(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeStore(int threadId, long time, int bci, int arg1, float arg2) {
            storeAdaptor(threadId).adviseBeforeStore(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeStore(int threadId, long time, int bci, int arg1, double arg2) {
            storeAdaptor(threadId).adviseBeforeStore(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeStore(int threadId, long time, int bci, int arg1, ObjectID arg2) {
            storeAdaptor(threadId).adviseBeforeStore(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long time, int bci, ObjectID arg1, int arg2, float arg3) {
            storeAdaptor(threadId).adviseBeforeArrayStore(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long time, int bci, ObjectID arg1, int arg2, long arg3) {
            storeAdaptor(threadId).adviseBeforeArrayStore(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long time, int bci, ObjectID arg1, int arg2, double arg3) {
            storeAdaptor(threadId).adviseBeforeArrayStore(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeArrayStore(int threadId, long time, int bci, ObjectID arg1, int arg2, ObjectID arg3) {
            storeAdaptor(threadId).adviseBeforeArrayStore(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeStackAdjust(int threadId, long time, int bci, int arg1) {
            storeAdaptor(threadId).adviseBeforeStackAdjust(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeOperation(int threadId, long time, int bci, int arg1, long arg2, long arg3) {
            storeAdaptor(threadId).adviseBeforeOperation(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeOperation(int threadId, long time, int bci, int arg1, float arg2, float arg3) {
            storeAdaptor(threadId).adviseBeforeOperation(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeOperation(int threadId, long time, int bci, int arg1, double arg2, double arg3) {
            storeAdaptor(threadId).adviseBeforeOperation(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeConversion(int threadId, long time, int bci, int arg1, float arg2) {
            storeAdaptor(threadId).adviseBeforeConversion(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeConversion(int threadId, long time, int bci, int arg1, long arg2) {
            storeAdaptor(threadId).adviseBeforeConversion(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeConversion(int threadId, long time, int bci, int arg1, double arg2) {
            storeAdaptor(threadId).adviseBeforeConversion(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeIf(int threadId, long time, int bci, int arg1, int arg2, int arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeIf(time, bci, arg1, arg2, arg3, arg4);
        }

        @Override
        protected void traceAdviseBeforeIf(int threadId, long time, int bci, int arg1, ObjectID arg2, ObjectID arg3, int arg4) {
            storeAdaptor(threadId).adviseBeforeIf(time, bci, arg1, arg2, arg3, arg4);
        }

        @Override
        protected void traceAdviseBeforeGoto(int threadId, long time, int bci, int arg1) {
            storeAdaptor(threadId).adviseBeforeGoto(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeReturn(int threadId, long time, int bci, ObjectID arg1) {
            storeAdaptor(threadId).adviseBeforeReturn(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeReturn(int threadId, long time, int bci, long arg1) {
            storeAdaptor(threadId).adviseBeforeReturn(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeReturn(int threadId, long time, int bci, float arg1) {
            storeAdaptor(threadId).adviseBeforeReturn(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeReturn(int threadId, long time, int bci, double arg1) {
            storeAdaptor(threadId).adviseBeforeReturn(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeReturn(int threadId, long time, int bci) {
            storeAdaptor(threadId).adviseBeforeReturn(time, bci);
        }

        @Override
        protected void traceAdviseBeforeGetStatic(int threadId, long time, int bci, FieldID arg1) {
            storeAdaptor(threadId).adviseBeforeGetStatic(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long time, int bci, FieldID arg1, ObjectID arg2) {
            storeAdaptor(threadId).adviseBeforePutStatic(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long time, int bci, FieldID arg1, double arg2) {
            storeAdaptor(threadId).adviseBeforePutStatic(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long time, int bci, FieldID arg1, long arg2) {
            storeAdaptor(threadId).adviseBeforePutStatic(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforePutStatic(int threadId, long time, int bci, FieldID arg1, float arg2) {
            storeAdaptor(threadId).adviseBeforePutStatic(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeGetField(int threadId, long time, int bci, ObjectID arg1, FieldID arg2) {
            storeAdaptor(threadId).adviseBeforeGetField(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforePutField(int threadId, long time, int bci, ObjectID arg1, FieldID arg2, ObjectID arg3) {
            storeAdaptor(threadId).adviseBeforePutField(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforePutField(int threadId, long time, int bci, ObjectID arg1, FieldID arg2, double arg3) {
            storeAdaptor(threadId).adviseBeforePutField(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforePutField(int threadId, long time, int bci, ObjectID arg1, FieldID arg2, long arg3) {
            storeAdaptor(threadId).adviseBeforePutField(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforePutField(int threadId, long time, int bci, ObjectID arg1, FieldID arg2, float arg3) {
            storeAdaptor(threadId).adviseBeforePutField(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseBeforeInvokeVirtual(int threadId, long time, int bci, ObjectID arg1, MethodID arg2) {
            storeAdaptor(threadId).adviseBeforeInvokeVirtual(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeInvokeSpecial(int threadId, long time, int bci, ObjectID arg1, MethodID arg2) {
            storeAdaptor(threadId).adviseBeforeInvokeSpecial(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeInvokeStatic(int threadId, long time, int bci, ObjectID arg1, MethodID arg2) {
            storeAdaptor(threadId).adviseBeforeInvokeStatic(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeInvokeInterface(int threadId, long time, int bci, ObjectID arg1, MethodID arg2) {
            storeAdaptor(threadId).adviseBeforeInvokeInterface(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeArrayLength(int threadId, long time, int bci, ObjectID arg1, int arg2) {
            storeAdaptor(threadId).adviseBeforeArrayLength(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeThrow(int threadId, long time, int bci, ObjectID arg1) {
            storeAdaptor(threadId).adviseBeforeThrow(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeCheckCast(int threadId, long time, int bci, ObjectID arg1, ClassID arg2) {
            storeAdaptor(threadId).adviseBeforeCheckCast(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeInstanceOf(int threadId, long time, int bci, ObjectID arg1, ClassID arg2) {
            storeAdaptor(threadId).adviseBeforeInstanceOf(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseBeforeMonitorEnter(int threadId, long time, int bci, ObjectID arg1) {
            storeAdaptor(threadId).adviseBeforeMonitorEnter(time, bci, arg1);
        }

        @Override
        protected void traceAdviseBeforeMonitorExit(int threadId, long time, int bci, ObjectID arg1) {
            storeAdaptor(threadId).adviseBeforeMonitorExit(time, bci, arg1);
        }

        @Override
        protected void traceAdviseAfterLoad(int threadId, long time, int bci, int arg1, ObjectID arg2) {
            storeAdaptor(threadId).adviseAfterLoad(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseAfterArrayLoad(int threadId, long time, int bci, ObjectID arg1, int arg2, ObjectID arg3) {
            storeAdaptor(threadId).adviseAfterArrayLoad(time, bci, arg1, arg2, arg3);
        }

        @Override
        protected void traceAdviseAfterNew(int threadId, long time, int bci, ObjectID arg1, ClassID arg2) {
            storeAdaptor(threadId).adviseAfterNew(time, bci, arg1, arg2);
        }

        @Override
        protected void traceAdviseAfterNewArray(int threadId, long time, int bci, ObjectID arg1, ClassID arg2, int length) {
            storeAdaptor(threadId).adviseAfterNewArray(time, bci, arg1, arg2, length);
        }

        @Override
        protected void traceAdviseAfterMethodEntry(int threadId, long time, int bci, ObjectID arg1, MethodID arg2) {
            storeAdaptor(threadId).adviseAfterMethodEntry(time, bci, arg1, arg2);
        }

        @Override
        protected void traceUnseenObject(int threadId, long time, ObjectID arg1, ClassID arg2) {
            storeAdaptor(threadId).unseenObject(time, arg1, arg2);
        }

        @Override
        protected void traceDead(int threadId, long time, ObjectID arg1) {
            storeAdaptor(threadId).dead(time, arg1);
        }

    }

    @HOSTED_ONLY
    @VMLoggerInterface(hidden = true, traceThread = true)
    public interface VMAVMLoggerInterface {
        void adviseBeforeGC(long time);
        void adviseAfterGC(long time);
        void adviseBeforeThreadStarting(long time);
        void adviseBeforeThreadTerminating(long time);
        void adviseBeforeReturnByThrow(long time, int bci, ObjectID arg1, int arg2);
        void adviseBeforeConstLoad(long time, int bci, long arg1);
        void adviseBeforeConstLoad(long time, int bci, ObjectID arg1);
        void adviseBeforeConstLoad(long time, int bci, float arg1);
        void adviseBeforeConstLoad(long time, int bci, double arg1);
        void adviseBeforeLoad(long time, int bci, int arg1);
        void adviseBeforeArrayLoad(long time, int bci, ObjectID arg1, int arg2);
        void adviseBeforeStore(long time, int bci, int arg1, long arg2);
        void adviseBeforeStore(long time, int bci, int arg1, float arg2);
        void adviseBeforeStore(long time, int bci, int arg1, double arg2);
        void adviseBeforeStore(long time, int bci, int arg1, ObjectID arg2);
        void adviseBeforeArrayStore(long time, int bci, ObjectID arg1, int arg2, float arg3);
        void adviseBeforeArrayStore(long time, int bci, ObjectID arg1, int arg2, long arg3);
        void adviseBeforeArrayStore(long time, int bci, ObjectID arg1, int arg2, double arg3);
        void adviseBeforeArrayStore(long time, int bci, ObjectID arg1, int arg2, ObjectID arg3);
        void adviseBeforeStackAdjust(long time, int bci, int arg1);
        void adviseBeforeOperation(long time, int bci, int arg1, long arg2, long arg3);
        void adviseBeforeOperation(long time, int bci, int arg1, float arg2, float arg3);
        void adviseBeforeOperation(long time, int bci, int arg1, double arg2, double arg3);
        void adviseBeforeConversion(long time, int bci, int arg1, float arg2);
        void adviseBeforeConversion(long time, int bci, int arg1, long arg2);
        void adviseBeforeConversion(long time, int bci, int arg1, double arg2);
        void adviseBeforeIf(long time, int bci, int arg1, int arg2, int arg3, int arg4);
        void adviseBeforeIf(long time, int bci, int arg1, ObjectID arg2, ObjectID arg3, int arg4);
        void adviseBeforeGoto(long time, int bci, int arg1);
        void adviseBeforeReturn(long time, int bci, ObjectID arg1);
        void adviseBeforeReturn(long time, int bci, long arg1);
        void adviseBeforeReturn(long time, int bci, float arg1);
        void adviseBeforeReturn(long time, int bci, double arg1);
        void adviseBeforeReturn(long time, int bci);
        void adviseBeforeGetStatic(long time, int bci, FieldID arg1);
        void adviseBeforePutStatic(long time, int bci, FieldID arg1, ObjectID arg2);
        void adviseBeforePutStatic(long time, int bci, FieldID arg1, double arg2);
        void adviseBeforePutStatic(long time, int bci, FieldID arg1, long arg2);
        void adviseBeforePutStatic(long time, int bci, FieldID arg1, float arg2);
        void adviseBeforeGetField(long time, int bci, ObjectID arg1, FieldID arg2);
        void adviseBeforePutField(long time, int bci, ObjectID arg1, FieldID arg2, ObjectID arg3);
        void adviseBeforePutField(long time, int bci, ObjectID arg1, FieldID arg2, double arg3);
        void adviseBeforePutField(long time, int bci, ObjectID arg1, FieldID arg2, long arg3);
        void adviseBeforePutField(long time, int bci, ObjectID arg1, FieldID arg2, float arg3);
        void adviseBeforeInvokeVirtual(long time, int bci, ObjectID arg1, MethodID arg2);
        void adviseBeforeInvokeSpecial(long time, int bci, ObjectID arg1, MethodID arg2);
        void adviseBeforeInvokeStatic(long time, int bci, ObjectID arg1, MethodID arg2);
        void adviseBeforeInvokeInterface(long time, int bci, ObjectID arg1, MethodID arg2);
        void adviseBeforeArrayLength(long time, int bci, ObjectID arg1, int arg2);
        void adviseBeforeThrow(long time, int bci, ObjectID arg1);
        void adviseBeforeCheckCast(long time, int bci, ObjectID arg1, ClassID arg2);
        void adviseBeforeInstanceOf(long time, int bci, ObjectID arg1, ClassID arg2);
        void adviseBeforeMonitorEnter(long time, int bci, ObjectID arg1);
        void adviseBeforeMonitorExit(long time, int bci, ObjectID arg1);
        void adviseAfterLoad(long time, int bci, int arg1, ObjectID arg2);
        void adviseAfterArrayLoad(long time, int bci, ObjectID arg1, int arg2, ObjectID arg3);
        void adviseAfterNew(long time, int bci, ObjectID arg1, ClassID arg2);
        void adviseAfterNewArray(long time, int bci, ObjectID arg1, ClassID arg2, int length);
        void adviseAfterMethodEntry(long time, int bci, ObjectID arg1, MethodID arg2);
        void unseenObject(long time, ObjectID arg1, ClassID arg2);
        void dead(long time, ObjectID arg1);
    }
// END GENERATED INTERFACE

// START GENERATED CODE
    private static abstract class VMAVMLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            AdviseAfterArrayLoad, AdviseAfterGC, AdviseAfterLoad,
            AdviseAfterMethodEntry, AdviseAfterNew, AdviseAfterNewArray, AdviseBeforeArrayLength,
            AdviseBeforeArrayLoad, AdviseBeforeArrayStore, AdviseBeforeArrayStore2, AdviseBeforeArrayStore3,
            AdviseBeforeArrayStore4, AdviseBeforeCheckCast, AdviseBeforeConstLoad, AdviseBeforeConstLoad2,
            AdviseBeforeConstLoad3, AdviseBeforeConstLoad4, AdviseBeforeConversion, AdviseBeforeConversion2,
            AdviseBeforeConversion3, AdviseBeforeGC, AdviseBeforeGetField, AdviseBeforeGetStatic,
            AdviseBeforeGoto, AdviseBeforeIf, AdviseBeforeIf2, AdviseBeforeInstanceOf,
            AdviseBeforeInvokeInterface, AdviseBeforeInvokeSpecial, AdviseBeforeInvokeStatic, AdviseBeforeInvokeVirtual,
            AdviseBeforeLoad, AdviseBeforeMonitorEnter, AdviseBeforeMonitorExit, AdviseBeforeOperation,
            AdviseBeforeOperation2, AdviseBeforeOperation3, AdviseBeforePutField, AdviseBeforePutField2,
            AdviseBeforePutField3, AdviseBeforePutField4, AdviseBeforePutStatic, AdviseBeforePutStatic2,
            AdviseBeforePutStatic3, AdviseBeforePutStatic4, AdviseBeforeReturn, AdviseBeforeReturn2,
            AdviseBeforeReturn3, AdviseBeforeReturn4, AdviseBeforeReturn5, AdviseBeforeReturnByThrow,
            AdviseBeforeStackAdjust, AdviseBeforeStore, AdviseBeforeStore2, AdviseBeforeStore3,
            AdviseBeforeStore4, AdviseBeforeThreadStarting, AdviseBeforeThreadTerminating, AdviseBeforeThrow,
            Dead, UnseenObject;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected VMAVMLoggerAuto(String name) {
            super(name, Operation.VALUES.length, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logAdviseAfterArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5) {
            log(Operation.AdviseAfterArrayLoad.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4), arg5);
        }
        protected abstract void traceAdviseAfterArrayLoad(int threadId, long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5);

        @INLINE
        public final void logAdviseAfterGC(long arg1) {
            log(Operation.AdviseAfterGC.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseAfterGC(int threadId, long arg1);

        @INLINE
        public final void logAdviseAfterLoad(long arg1, int arg2, int arg3, ObjectID arg4) {
            log(Operation.AdviseAfterLoad.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), arg4);
        }
        protected abstract void traceAdviseAfterLoad(int threadId, long arg1, int arg2, int arg3, ObjectID arg4);

        @INLINE
        public final void logAdviseAfterMethodEntry(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
            log(Operation.AdviseAfterMethodEntry.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseAfterMethodEntry(int threadId, long arg1, int arg2, ObjectID arg3, MethodID arg4);

        @INLINE
        public final void logAdviseAfterNew(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
            log(Operation.AdviseAfterNew.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseAfterNew(int threadId, long arg1, int arg2, ObjectID arg3, ClassID arg4);

        @INLINE
        public final void logAdviseAfterNewArray(long arg1, int arg2, ObjectID arg3, ClassID arg4, int arg5) {
            log(Operation.AdviseAfterNewArray.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4, intArg(arg5));
        }
        protected abstract void traceAdviseAfterNewArray(int threadId, long arg1, int arg2, ObjectID arg3, ClassID arg4, int arg5);

        @INLINE
        public final void logAdviseBeforeArrayLength(long arg1, int arg2, ObjectID arg3, int arg4) {
            log(Operation.AdviseBeforeArrayLength.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayLength(int threadId, long arg1, int arg2, ObjectID arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeArrayLoad(long arg1, int arg2, ObjectID arg3, int arg4) {
            log(Operation.AdviseBeforeArrayLoad.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4));
        }
        protected abstract void traceAdviseBeforeArrayLoad(int threadId, long arg1, int arg2, ObjectID arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5) {
            log(Operation.AdviseBeforeArrayStore.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4), arg5);
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, ObjectID arg3, int arg4, ObjectID arg5);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, double arg5) {
            log(Operation.AdviseBeforeArrayStore2.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4), doubleArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, ObjectID arg3, int arg4, double arg5);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, float arg5) {
            log(Operation.AdviseBeforeArrayStore3.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4), floatArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, ObjectID arg3, int arg4, float arg5);

        @INLINE
        public final void logAdviseBeforeArrayStore(long arg1, int arg2, ObjectID arg3, int arg4, long arg5) {
            log(Operation.AdviseBeforeArrayStore4.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4), longArg(arg5));
        }
        protected abstract void traceAdviseBeforeArrayStore(int threadId, long arg1, int arg2, ObjectID arg3, int arg4, long arg5);

        @INLINE
        public final void logAdviseBeforeCheckCast(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
            log(Operation.AdviseBeforeCheckCast.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeCheckCast(int threadId, long arg1, int arg2, ObjectID arg3, ClassID arg4);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, ObjectID arg3) {
            log(Operation.AdviseBeforeConstLoad.ordinal(), longArg(arg1), intArg(arg2), arg3);
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, ObjectID arg3);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeConstLoad2.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeConstLoad(long arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeConstLoad3.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeConstLoad(int threadId, long arg1, int arg2, float arg3);

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
        public final void logAdviseBeforeGetField(long arg1, int arg2, ObjectID arg3, FieldID arg4) {
            log(Operation.AdviseBeforeGetField.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeGetField(int threadId, long arg1, int arg2, ObjectID arg3, FieldID arg4);

        @INLINE
        public final void logAdviseBeforeGetStatic(long arg1, int arg2, FieldID arg3) {
            log(Operation.AdviseBeforeGetStatic.ordinal(), longArg(arg1), intArg(arg2), arg3);
        }
        protected abstract void traceAdviseBeforeGetStatic(int threadId, long arg1, int arg2, FieldID arg3);

        @INLINE
        public final void logAdviseBeforeGoto(long arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeGoto.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeGoto(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeIf(long arg1, int arg2, int arg3, ObjectID arg4, ObjectID arg5,
                int arg6) {
            log(Operation.AdviseBeforeIf.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), arg4, arg5,
                intArg(arg6));
        }
        protected abstract void traceAdviseBeforeIf(int threadId, long arg1, int arg2, int arg3, ObjectID arg4, ObjectID arg5,
                int arg6);

        @INLINE
        public final void logAdviseBeforeIf(long arg1, int arg2, int arg3, int arg4, int arg5,
                int arg6) {
            log(Operation.AdviseBeforeIf2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), intArg(arg4), intArg(arg5),
                intArg(arg6));
        }
        protected abstract void traceAdviseBeforeIf(int threadId, long arg1, int arg2, int arg3, int arg4, int arg5,
                int arg6);

        @INLINE
        public final void logAdviseBeforeInstanceOf(long arg1, int arg2, ObjectID arg3, ClassID arg4) {
            log(Operation.AdviseBeforeInstanceOf.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeInstanceOf(int threadId, long arg1, int arg2, ObjectID arg3, ClassID arg4);

        @INLINE
        public final void logAdviseBeforeInvokeInterface(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
            log(Operation.AdviseBeforeInvokeInterface.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeInvokeInterface(int threadId, long arg1, int arg2, ObjectID arg3, MethodID arg4);

        @INLINE
        public final void logAdviseBeforeInvokeSpecial(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
            log(Operation.AdviseBeforeInvokeSpecial.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeInvokeSpecial(int threadId, long arg1, int arg2, ObjectID arg3, MethodID arg4);

        @INLINE
        public final void logAdviseBeforeInvokeStatic(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
            log(Operation.AdviseBeforeInvokeStatic.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeInvokeStatic(int threadId, long arg1, int arg2, ObjectID arg3, MethodID arg4);

        @INLINE
        public final void logAdviseBeforeInvokeVirtual(long arg1, int arg2, ObjectID arg3, MethodID arg4) {
            log(Operation.AdviseBeforeInvokeVirtual.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforeInvokeVirtual(int threadId, long arg1, int arg2, ObjectID arg3, MethodID arg4);

        @INLINE
        public final void logAdviseBeforeLoad(long arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeLoad.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeLoad(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeMonitorEnter(long arg1, int arg2, ObjectID arg3) {
            log(Operation.AdviseBeforeMonitorEnter.ordinal(), longArg(arg1), intArg(arg2), arg3);
        }
        protected abstract void traceAdviseBeforeMonitorEnter(int threadId, long arg1, int arg2, ObjectID arg3);

        @INLINE
        public final void logAdviseBeforeMonitorExit(long arg1, int arg2, ObjectID arg3) {
            log(Operation.AdviseBeforeMonitorExit.ordinal(), longArg(arg1), intArg(arg2), arg3);
        }
        protected abstract void traceAdviseBeforeMonitorExit(int threadId, long arg1, int arg2, ObjectID arg3);

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
        public final void logAdviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, ObjectID arg5) {
            log(Operation.AdviseBeforePutField.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4, arg5);
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, ObjectID arg3, FieldID arg4, ObjectID arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, double arg5) {
            log(Operation.AdviseBeforePutField2.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4, doubleArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, ObjectID arg3, FieldID arg4, double arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, float arg5) {
            log(Operation.AdviseBeforePutField3.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4, floatArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, ObjectID arg3, FieldID arg4, float arg5);

        @INLINE
        public final void logAdviseBeforePutField(long arg1, int arg2, ObjectID arg3, FieldID arg4, long arg5) {
            log(Operation.AdviseBeforePutField4.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4, longArg(arg5));
        }
        protected abstract void traceAdviseBeforePutField(int threadId, long arg1, int arg2, ObjectID arg3, FieldID arg4, long arg5);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, FieldID arg3, ObjectID arg4) {
            log(Operation.AdviseBeforePutStatic.ordinal(), longArg(arg1), intArg(arg2), arg3, arg4);
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, FieldID arg3, ObjectID arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, FieldID arg3, double arg4) {
            log(Operation.AdviseBeforePutStatic2.ordinal(), longArg(arg1), intArg(arg2), arg3, doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, FieldID arg3, double arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, FieldID arg3, float arg4) {
            log(Operation.AdviseBeforePutStatic3.ordinal(), longArg(arg1), intArg(arg2), arg3, floatArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, FieldID arg3, float arg4);

        @INLINE
        public final void logAdviseBeforePutStatic(long arg1, int arg2, FieldID arg3, long arg4) {
            log(Operation.AdviseBeforePutStatic4.ordinal(), longArg(arg1), intArg(arg2), arg3, longArg(arg4));
        }
        protected abstract void traceAdviseBeforePutStatic(int threadId, long arg1, int arg2, FieldID arg3, long arg4);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2) {
            log(Operation.AdviseBeforeReturn.ordinal(), longArg(arg1), intArg(arg2));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, ObjectID arg3) {
            log(Operation.AdviseBeforeReturn2.ordinal(), longArg(arg1), intArg(arg2), arg3);
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, ObjectID arg3);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, double arg3) {
            log(Operation.AdviseBeforeReturn3.ordinal(), longArg(arg1), intArg(arg2), doubleArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, double arg3);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, float arg3) {
            log(Operation.AdviseBeforeReturn4.ordinal(), longArg(arg1), intArg(arg2), floatArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, float arg3);

        @INLINE
        public final void logAdviseBeforeReturn(long arg1, int arg2, long arg3) {
            log(Operation.AdviseBeforeReturn5.ordinal(), longArg(arg1), intArg(arg2), longArg(arg3));
        }
        protected abstract void traceAdviseBeforeReturn(int threadId, long arg1, int arg2, long arg3);

        @INLINE
        public final void logAdviseBeforeReturnByThrow(long arg1, int arg2, ObjectID arg3, int arg4) {
            log(Operation.AdviseBeforeReturnByThrow.ordinal(), longArg(arg1), intArg(arg2), arg3, intArg(arg4));
        }
        protected abstract void traceAdviseBeforeReturnByThrow(int threadId, long arg1, int arg2, ObjectID arg3, int arg4);

        @INLINE
        public final void logAdviseBeforeStackAdjust(long arg1, int arg2, int arg3) {
            log(Operation.AdviseBeforeStackAdjust.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceAdviseBeforeStackAdjust(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, ObjectID arg4) {
            log(Operation.AdviseBeforeStore.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), arg4);
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, ObjectID arg4);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, double arg4) {
            log(Operation.AdviseBeforeStore2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), doubleArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, double arg4);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, float arg4) {
            log(Operation.AdviseBeforeStore3.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), floatArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, float arg4);

        @INLINE
        public final void logAdviseBeforeStore(long arg1, int arg2, int arg3, long arg4) {
            log(Operation.AdviseBeforeStore4.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), longArg(arg4));
        }
        protected abstract void traceAdviseBeforeStore(int threadId, long arg1, int arg2, int arg3, long arg4);

        @INLINE
        public final void logAdviseBeforeThreadStarting(long arg1) {
            log(Operation.AdviseBeforeThreadStarting.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeThreadStarting(int threadId, long arg1);

        @INLINE
        public final void logAdviseBeforeThreadTerminating(long arg1) {
            log(Operation.AdviseBeforeThreadTerminating.ordinal(), longArg(arg1));
        }
        protected abstract void traceAdviseBeforeThreadTerminating(int threadId, long arg1);

        @INLINE
        public final void logAdviseBeforeThrow(long arg1, int arg2, ObjectID arg3) {
            log(Operation.AdviseBeforeThrow.ordinal(), longArg(arg1), intArg(arg2), arg3);
        }
        protected abstract void traceAdviseBeforeThrow(int threadId, long arg1, int arg2, ObjectID arg3);

        @INLINE
        public final void logDead(long arg1, ObjectID arg2) {
            log(Operation.Dead.ordinal(), longArg(arg1), arg2);
        }
        protected abstract void traceDead(int threadId, long arg1, ObjectID arg2);

        @INLINE
        public final void logUnseenObject(long arg1, ObjectID arg2, ClassID arg3) {
            log(Operation.UnseenObject.ordinal(), longArg(arg1), arg2, arg3);
        }
        protected abstract void traceUnseenObject(int threadId, long arg1, ObjectID arg2, ClassID arg3);

        @Override
        protected void trace(Record r) {
            int threadId = r.getThreadId();
            switch (r.getOperation()) {
                case 0: { //AdviseAfterArrayLoad
                    traceAdviseAfterArrayLoad(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4), toObjectID(r, 5));
                    break;
                }
                case 1: { //AdviseAfterGC
                    traceAdviseAfterGC(threadId, toLong(r, 1));
                    break;
                }
                case 2: { //AdviseAfterLoad
                    traceAdviseAfterLoad(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toObjectID(r, 4));
                    break;
                }
                case 3: { //AdviseAfterMethodEntry
                    traceAdviseAfterMethodEntry(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toMethodID(r, 4));
                    break;
                }
                case 4: { //AdviseAfterNew
                    traceAdviseAfterNew(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toClassID(r, 4));
                    break;
                }
                case 5: { //AdviseAfterNewArray
                    traceAdviseAfterNewArray(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toClassID(r, 4), toInt(r, 5));
                    break;
                }
                case 6: { //AdviseBeforeArrayLength
                    traceAdviseBeforeArrayLength(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4));
                    break;
                }
                case 7: { //AdviseBeforeArrayLoad
                    traceAdviseBeforeArrayLoad(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4));
                    break;
                }
                case 8: { //AdviseBeforeArrayStore
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4), toObjectID(r, 5));
                    break;
                }
                case 9: { //AdviseBeforeArrayStore2
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4), toDouble(r, 5));
                    break;
                }
                case 10: { //AdviseBeforeArrayStore3
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4), toFloat(r, 5));
                    break;
                }
                case 11: { //AdviseBeforeArrayStore4
                    traceAdviseBeforeArrayStore(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4), toLong(r, 5));
                    break;
                }
                case 12: { //AdviseBeforeCheckCast
                    traceAdviseBeforeCheckCast(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toClassID(r, 4));
                    break;
                }
                case 13: { //AdviseBeforeConstLoad
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3));
                    break;
                }
                case 14: { //AdviseBeforeConstLoad2
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 15: { //AdviseBeforeConstLoad3
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 16: { //AdviseBeforeConstLoad4
                    traceAdviseBeforeConstLoad(threadId, toLong(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 17: { //AdviseBeforeConversion
                    traceAdviseBeforeConversion(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 18: { //AdviseBeforeConversion2
                    traceAdviseBeforeConversion(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 19: { //AdviseBeforeConversion3
                    traceAdviseBeforeConversion(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 20: { //AdviseBeforeGC
                    traceAdviseBeforeGC(threadId, toLong(r, 1));
                    break;
                }
                case 21: { //AdviseBeforeGetField
                    traceAdviseBeforeGetField(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toFieldID(r, 4));
                    break;
                }
                case 22: { //AdviseBeforeGetStatic
                    traceAdviseBeforeGetStatic(threadId, toLong(r, 1), toInt(r, 2), toFieldID(r, 3));
                    break;
                }
                case 23: { //AdviseBeforeGoto
                    traceAdviseBeforeGoto(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 24: { //AdviseBeforeIf
                    traceAdviseBeforeIf(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toObjectID(r, 4), toObjectID(r, 5), toInt(r, 6));
                    break;
                }
                case 25: { //AdviseBeforeIf2
                    traceAdviseBeforeIf(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toInt(r, 4), toInt(r, 5), toInt(r, 6));
                    break;
                }
                case 26: { //AdviseBeforeInstanceOf
                    traceAdviseBeforeInstanceOf(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toClassID(r, 4));
                    break;
                }
                case 27: { //AdviseBeforeInvokeInterface
                    traceAdviseBeforeInvokeInterface(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toMethodID(r, 4));
                    break;
                }
                case 28: { //AdviseBeforeInvokeSpecial
                    traceAdviseBeforeInvokeSpecial(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toMethodID(r, 4));
                    break;
                }
                case 29: { //AdviseBeforeInvokeStatic
                    traceAdviseBeforeInvokeStatic(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toMethodID(r, 4));
                    break;
                }
                case 30: { //AdviseBeforeInvokeVirtual
                    traceAdviseBeforeInvokeVirtual(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toMethodID(r, 4));
                    break;
                }
                case 31: { //AdviseBeforeLoad
                    traceAdviseBeforeLoad(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 32: { //AdviseBeforeMonitorEnter
                    traceAdviseBeforeMonitorEnter(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3));
                    break;
                }
                case 33: { //AdviseBeforeMonitorExit
                    traceAdviseBeforeMonitorExit(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3));
                    break;
                }
                case 34: { //AdviseBeforeOperation
                    traceAdviseBeforeOperation(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toDouble(r, 4), toDouble(r, 5));
                    break;
                }
                case 35: { //AdviseBeforeOperation2
                    traceAdviseBeforeOperation(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toFloat(r, 4), toFloat(r, 5));
                    break;
                }
                case 36: { //AdviseBeforeOperation3
                    traceAdviseBeforeOperation(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toLong(r, 4), toLong(r, 5));
                    break;
                }
                case 37: { //AdviseBeforePutField
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toFieldID(r, 4), toObjectID(r, 5));
                    break;
                }
                case 38: { //AdviseBeforePutField2
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toFieldID(r, 4), toDouble(r, 5));
                    break;
                }
                case 39: { //AdviseBeforePutField3
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toFieldID(r, 4), toFloat(r, 5));
                    break;
                }
                case 40: { //AdviseBeforePutField4
                    traceAdviseBeforePutField(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toFieldID(r, 4), toLong(r, 5));
                    break;
                }
                case 41: { //AdviseBeforePutStatic
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toFieldID(r, 3), toObjectID(r, 4));
                    break;
                }
                case 42: { //AdviseBeforePutStatic2
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toFieldID(r, 3), toDouble(r, 4));
                    break;
                }
                case 43: { //AdviseBeforePutStatic3
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toFieldID(r, 3), toFloat(r, 4));
                    break;
                }
                case 44: { //AdviseBeforePutStatic4
                    traceAdviseBeforePutStatic(threadId, toLong(r, 1), toInt(r, 2), toFieldID(r, 3), toLong(r, 4));
                    break;
                }
                case 45: { //AdviseBeforeReturn
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2));
                    break;
                }
                case 46: { //AdviseBeforeReturn2
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3));
                    break;
                }
                case 47: { //AdviseBeforeReturn3
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toDouble(r, 3));
                    break;
                }
                case 48: { //AdviseBeforeReturn4
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toFloat(r, 3));
                    break;
                }
                case 49: { //AdviseBeforeReturn5
                    traceAdviseBeforeReturn(threadId, toLong(r, 1), toInt(r, 2), toLong(r, 3));
                    break;
                }
                case 50: { //AdviseBeforeReturnByThrow
                    traceAdviseBeforeReturnByThrow(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3), toInt(r, 4));
                    break;
                }
                case 51: { //AdviseBeforeStackAdjust
                    traceAdviseBeforeStackAdjust(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 52: { //AdviseBeforeStore
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toObjectID(r, 4));
                    break;
                }
                case 53: { //AdviseBeforeStore2
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toDouble(r, 4));
                    break;
                }
                case 54: { //AdviseBeforeStore3
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toFloat(r, 4));
                    break;
                }
                case 55: { //AdviseBeforeStore4
                    traceAdviseBeforeStore(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toLong(r, 4));
                    break;
                }
                case 56: { //AdviseBeforeThreadStarting
                    traceAdviseBeforeThreadStarting(threadId, toLong(r, 1));
                    break;
                }
                case 57: { //AdviseBeforeThreadTerminating
                    traceAdviseBeforeThreadTerminating(threadId, toLong(r, 1));
                    break;
                }
                case 58: { //AdviseBeforeThrow
                    traceAdviseBeforeThrow(threadId, toLong(r, 1), toInt(r, 2), toObjectID(r, 3));
                    break;
                }
                case 59: { //Dead
                    traceDead(threadId, toLong(r, 1), toObjectID(r, 2));
                    break;
                }
                case 60: { //UnseenObject
                    traceUnseenObject(threadId, toLong(r, 1), toObjectID(r, 2), toClassID(r, 3));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
