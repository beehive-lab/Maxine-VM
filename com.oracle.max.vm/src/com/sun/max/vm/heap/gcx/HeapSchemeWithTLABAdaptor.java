/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.memory.*;
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Adaptor for factoring a number of common boiler plate for HeapScheme implemented with components of the gcx package.
 */
public abstract class HeapSchemeWithTLABAdaptor extends HeapSchemeWithTLAB {
    protected static boolean VerifyAfterGC = false;
    static {
        VMOptions.addFieldOption("-XX:", "VerifyAfterGC", HeapSchemeWithTLABAdaptor.class, "Verify heap after GC", Phase.PRISTINE);
    }

    /**
     * Size to reserve at the end of a TLABs to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @FOLD
    protected static Size tlabHeadroom() {
        return minObjectSize();
    }

    @FOLD
    protected static int tlabHeadroomNumWords() {
        return tlabHeadroom().unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
    }

    protected static void fillTLABWithDeadObject(Pointer tlabAllocationMark, Pointer tlabEnd) {
        // Need to plant a dead object in the leftover to make the heap parseable (required for sweeping).
        Pointer hardLimit = tlabEnd.plus(tlabHeadroom());
        if (tlabAllocationMark.greaterThan(tlabEnd)) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("TLAB_MARK = ");
            Log.print(tlabAllocationMark);
            Log.print(", TLAB end = ");
            Log.println(tlabEnd);
            FatalError.check(hardLimit.equals(tlabAllocationMark), "TLAB allocation mark cannot be greater than TLAB End");
            Log.unlock(lockDisabledSafepoints);
            return;
        }
        fillWithDeadObject(tlabAllocationMark, hardLimit);
    }

    class TLABFiller extends HeapSchemeWithTLAB.ResetTLAB {
        @Override
        protected void doBeforeReset(Pointer etla, Pointer tlabMark, Pointer tlabTop) {
            if (MaxineVM.isDebug() && logTLABEvents(tlabMark)) {
                TLABLog.doOnRetireTLAB(etla);
            }

            if (tlabMark.greaterThan(tlabTop)) {
                // Already filled-up (mark is at the limit).
                return;
            }
            fillTLABWithDeadObject(tlabMark, tlabTop);
        }
    }
    protected final TLABFiller tlabFiller = new TLABFiller();
    protected final AtomicPinCounter pinnedCounter;

    public HeapSchemeWithTLABAdaptor() {
        super();
        pinnedCounter = MaxineVM.isDebug() ? new AtomicPinCounter() : null;
    }

    /**
     * Debugging support for TLAB event logging. Tells whether a condition is met for logging a TLAB event.
     * @param tlabStart start of the TLAB
     * @return true if the event on the specified TLAB should be logged
     */
    protected boolean logTLABEvents(Address tlabStart) {
        return false;
    }

    abstract protected void allocateHeapAndGCStorage();
    abstract protected void reportTotalGCTimes();

    private static final TimerMetric heapStartupTime = new TimerMetric(new SingleUseTimer(Clock.SYSTEM_MILLISECONDS));


    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            heapStartupTime.start();
            allocateHeapAndGCStorage();
            heapStartupTime.stop();
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (Heap.logGCTime()) {
                heapStartupTime.report("allocateHeapAndGCStorage", Log.out);
                reportTotalGCTimes();
                VirtualMemory.reportMetrics();
            }
        }
    }

    @Override
    public int reservedVirtualSpaceKB() {
        // 2^30 Kb = 1 TB of reserved virtual space.
        // This will be truncated as soon as we taxed what we need at initialization time.
        return Size.G.toInt();
    }

    @Override
    protected void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd) {
        fillTLABWithDeadObject(tlabAllocationMark, tlabEnd);
    }

    @Override
    protected void tlabReset(Pointer tla) {
        tlabFiller.run(tla);
    }

    @Override
    protected void releaseUnusedReservedVirtualSpace() {
        // Do nothing. Heap schemes using this package have their own way of doing this.
    }

    @Override
    public boolean isGcThread(Thread thread) {
        return thread instanceof VmOperationThread;
    }

    @INLINE
    @Override
    public boolean supportsTagging() {
        return false;
    }

    @INLINE
    public boolean pin(Object object) {
        // Objects never relocate. So this is always safe.
        if (MaxineVM.isDebug()) {
            pinnedCounter.increment();
        }
        return true;
    }

    @INLINE
    public void unpin(Object object) {
        if (MaxineVM.isDebug()) {
            pinnedCounter.decrement();
        }
    }

    // TODO These are just placeholders that implement the minimum defined functionality
    // required by HeapScheme
    // Located here for inheritance by sub schemes.

    @Override
    public TimeLogger timeLogger() {
        return timeLogger;
    }

    @Override
    public PhaseLogger phaseLogger() {
        return phaseLogger;
    }

    @HOSTED_ONLY
    @VMLoggerInterface(parent = HeapScheme.PhaseLogger.class)
    private interface PhaseLoggerInterface {
        void scanningThreadRoots(@VMLogParam(name = "vmThread") VmThread vmThread);
    }

    static final PhaseLogger phaseLogger = new PhaseLogger();

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

    private static final TimeLogger timeLogger = new TimeLogger();

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
