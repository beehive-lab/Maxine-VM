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
import com.sun.max.vm.runtime.*;

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
        // Need to plant a dead object in the leftover to make the heap parsable (required for sweeping).
        Pointer hardLimit = tlabEnd.plus(tlabHeadroom());
        if (tlabAllocationMark.greaterThan(tlabEnd)) {
            FatalError.check(hardLimit.equals(tlabAllocationMark), "TLAB allocation mark cannot be greater than TLAB End");
            return;
        }
        DarkMatter.format(tlabAllocationMark, hardLimit);
    }

    class TLABFiller extends HeapSchemeWithTLAB.ResetTLAB {
        @Override
        protected void doBeforeReset(Pointer etla, Pointer tlabMark, Pointer tlabTop) {
            if (MaxineVM.isDebug() && logTLABEvents(tlabMark)) {
                TLABLog.doOnRetireTLAB(etla);
            }
            fillTLABWithDeadObject(tlabMark, tlabTop);
        }
    }
    protected final TLABFiller tlabFiller = new TLABFiller();
    public HeapSchemeWithTLABAdaptor() {
        super();
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

    @INLINE
    @Override
    public final boolean supportsTagging() {
        return false;
    }

    @INLINE
    @Override
    public final boolean supportsPadding() {
        return false;
    }
}
