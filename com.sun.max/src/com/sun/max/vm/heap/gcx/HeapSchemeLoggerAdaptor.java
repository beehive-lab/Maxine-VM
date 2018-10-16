/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.annotate.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.thread.*;


public class HeapSchemeLoggerAdaptor {
    public static final TimeLogger timeLogger = new TimeLogger();
    public static final PhaseLogger phaseLogger = new PhaseLogger();

    @HOSTED_ONLY
    @VMLoggerInterface(parent = HeapScheme.PhaseLogger.class)
    private interface PhaseLoggerInterface {
        void scanningThreadRoots(@VMLogParam(name = "vmThread") VmThread vmThread);
    }

    public static final class PhaseLogger extends PhaseLoggerAuto {

        PhaseLogger() {
            super(null, null);
        }

        @Override
        protected void traceScanningThreadRoots(VmThread vmThread) {
            Log.print("Scanning thread local and stack roots for thread ");
            Log.printThread(vmThread, true);
        }
    }

    @HOSTED_ONLY
    @VMLoggerInterface(parent = HeapScheme.TimeLogger.class)
    private interface TimeLoggerInterface {
        void stackReferenceMapPreparationTime(
            @VMLogParam(name = "stackReferenceMapPreparationTime") long stackReferenceMapPreparationTime);
    }

    public static final class TimeLogger extends TimeLoggerAuto {
        private static final String HZ_SUFFIX = TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK);
        private static final String TIMINGS_LEAD = "Timings (" + HZ_SUFFIX + ") for ";

        TimeLogger() {
            super(null, null);
        }

        @Override
        protected void traceStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime) {
            Log.print("Stack reference map preparation time: ");
            Log.print(stackReferenceMapPreparationTime);
            Log.println(HZ_SUFFIX);
        }
    }

// START GENERATED CODE
    private static abstract class PhaseLoggerAuto extends com.sun.max.vm.heap.HeapScheme.PhaseLogger {
        public enum Operation {
            ScanningThreadRoots;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected PhaseLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @Override
        @INLINE
        public final void logScanningThreadRoots(VmThread vmThread) {
            log(Operation.ScanningThreadRoots.ordinal(), vmThreadArg(vmThread));
        }
        protected abstract void traceScanningThreadRoots(VmThread vmThread);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //ScanningThreadRoots
                    traceScanningThreadRoots(toVmThread(r, 1));
                    break;
                }
            }
        }
    }

    private static abstract class TimeLoggerAuto extends com.sun.max.vm.heap.HeapScheme.TimeLogger {
        public enum Operation {
            StackReferenceMapPreparationTime;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected TimeLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @Override
        @INLINE
        public final void logStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime) {
            log(Operation.StackReferenceMapPreparationTime.ordinal(), longArg(stackReferenceMapPreparationTime));
        }
        protected abstract void traceStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //StackReferenceMapPreparationTime
                    traceStackReferenceMapPreparationTime(toLong(r, 1));
                    break;
                }
            }
        }
    }

// END GENERATED CODE
}
