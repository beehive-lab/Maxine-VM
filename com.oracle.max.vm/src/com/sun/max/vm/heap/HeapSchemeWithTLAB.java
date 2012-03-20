/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;

/**
 * A HeapScheme adaptor with support for thread local allocation buffers (TLABs). The adaptor factors out methods for
 * allocating from a TLAB, enabling / disabling allocations, and replacing the TLAB when refill is needed. Choosing when
 * to refill a TLAB, how to pad the end of the TLAB on refill, and how to refill is delegated to the HeapScheme concrete
 * implementation which also associates a TLAB Refill policy to each thread. The TLAB refill policy is currently required
 * if TLAB is used as it is also used to save/restore TLAB top on enabling/disabling of allocation.
 */
public abstract class HeapSchemeWithTLAB extends HeapSchemeAdaptor {

    public static final String TLAB_TOP_THREAD_LOCAL_NAME = "TLAB_TOP";
    public static final String TLAB_MARK_THREAD_LOCAL_NAME = "TLAB_MARK";
    public static final String TLAB_DISABLED_THREAD_LOCAL_NAME = "TLAB_DISABLED";

    // TODO: clean this up. Used just for testing with and without inlined XIR tlab allocation.
    public static boolean GenInlinedTLABAlloc = true;

    /**
     * Determines if TLABs should be traced.
     *
     * @returns {@code false} if the VM build level is not {@link BuildLevel#DEBUG}.
     */
    @INLINE
    public static boolean traceTLAB() {
        return logger.enabled();
    }

    public static void setTraceTLAB(boolean b) {
        logger.enableTrace(b);
    }

    private static boolean PrintTLABStats;

    static {
        VMOptions.addFieldOption("-XX:", "PrintTLABStats", Classes.getDeclaredField(HeapSchemeWithTLAB.class, "PrintTLABStats"),
                        "Print TLAB statistics at end of program.", MaxineVM.Phase.PRISTINE);

        // TODO: clean this up. Used just for testing with and without inlined XIR tlab allocation.
        VMOptions.addFieldOption("-XX:", "InlineTLAB", Classes.getDeclaredField(HeapSchemeWithTLAB.class, "GenInlinedTLABAlloc"),
                        "XIR generate inlined TLAB allocations.", MaxineVM.Phase.PRISTINE);
    }

    /**
     * A VM option for disabling use of TLABs.
     */
    public static boolean UseTLAB = true;
    static {
        VMOptions.addFieldOption("-XX:", "UseTLAB", HeapSchemeWithTLAB.class, "Use thread-local object allocation", MaxineVM.Phase.PRISTINE);
    }

    /**
     * A VM option for specifying the size of a TLAB. Default is 64 K.
     */
    private static final VMSizeOption tlabSizeOption = register(new VMSizeOption("-XX:TLABSize=", Size.K.times(64),
        "The size of thread-local allocation buffers."), MaxineVM.Phase.PRISTINE);

    /**
     * The top of the current thread-local allocation buffer. This will remain zero if TLABs are not
     * {@linkplain #useTLABOption enabled}.
     */
    public static final VmThreadLocal TLAB_TOP
        = new VmThreadLocal(TLAB_TOP_THREAD_LOCAL_NAME, false, "HeapSchemeWithTLAB: top of current TLAB, zero if not used", Nature.Single);

    /**
     * The allocation mark of the current thread-local allocation buffer. This will remain zero if TLABs
     * are not {@linkplain #useTLABOption enabled}.
     */
    public static final VmThreadLocal TLAB_MARK
        = new VmThreadLocal(TLAB_MARK_THREAD_LOCAL_NAME, false, "HeapSchemeWithTLAB: allocation mark of current TLAB, zero if not used", Nature.Single);

    /**
     * The temporary top of the current thread-local allocation buffer. This will remain zero if TLABs are not
     * {@linkplain #useTLABOption enabled}. Used when thread is allocating on the global immortal heap.
     */
    private static final VmThreadLocal TLAB_TOP_TMP
        = new VmThreadLocal("TLAB_TOP_TMP", false, "HeapSchemeWithTLAB: temporary top of current TLAB, zero if not used", Nature.Single);

    /**
     * The temporary allocation mark of the current thread-local allocation buffer. This will remain zero if TLABs
     * are not {@linkplain #useTLABOption enabled}. Used when thread is allocating on the global immortal heap.
     */
    private static final VmThreadLocal TLAB_MARK_TMP
        = new VmThreadLocal("TLAB_MARK_TMP", false, "HeapSchemeWithTLAB: temporary allocation mark of current TLAB, zero if not used", Nature.Single);

    /**
     * Thread-local used to disable allocation per thread.
     */
    private static final VmThreadLocal ALLOCATION_DISABLED
        = new VmThreadLocal(TLAB_DISABLED_THREAD_LOCAL_NAME, false, "HeapSchemeWithTLAB: disables per thread allocation if non-zero");

    /**
     * A procedure for resetting the TLAB of a thread.
     */
    public static class ResetTLAB implements Pointer.Procedure {

        protected void doBeforeReset(Pointer etla, Pointer tlabMark, Pointer tlabTop) {
            // Default is nothing.
        }

        public void run(Pointer tla) {
            final Pointer etla = VmThreadLocal.ETLA.load(tla);
            final Pointer tlabMark = TLAB_MARK.load(etla);
            Pointer tlabTop = TLAB_TOP.load(etla);
            if (traceTLAB()) {
                logger.logReset(UnsafeCast.asVmThread(VM_THREAD.loadRef(etla).toJava()), tlabTop, tlabMark);
            }
            if (tlabTop.equals(Address.zero())) {
                // TLAB's top can be null in only two cases:
                // (1) it has never been filled, in which case it's allocation mark is null too
                if (tlabMark.equals(Address.zero()))  {
                    // No TLABs, so nothing to reset.
                    return;
                }
                // (2) allocation has been disabled for the thread.
                FatalError.check(!ALLOCATION_DISABLED.load(currentTLA()).isZero(), "inconsistent TLAB state");
                TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(etla);
                if (refillPolicy != null) {
                    // Go fetch the actual TLAB top in case the heap scheme needs it for its doBeforeReset handler.
                    tlabTop = refillPolicy.getSavedTlabTop().asPointer();
                    // Zap the TLAB top saved in the refill policy. Don't want it to be restored after GC.
                    refillPolicy.saveTlabTop(Address.zero());
                }
            }
            doBeforeReset(etla, tlabMark, tlabTop);
            TLAB_TOP.store(etla, Address.zero());
            TLAB_MARK.store(etla, Address.zero());
        }
    }

    protected abstract void tlabReset(Pointer tla);

    /**
     * A TLAB policy that never refills. Just a convenience to disable TLAB use.
     */
    protected static final TLABRefillPolicy NEVER_REFILL_TLAB = new TLABRefillPolicy() {
        @Override
        public boolean shouldRefill(Size size, Pointer allocationMark) {
            return false;
        }

        @Override
        public Size nextTlabSize() {
            return Size.zero();
        }
    };

    /**
     * Flags if TLABs are being used for allocation.
     */
    private boolean useTLAB;

    /**
     * Initial size of a TLABs.
     */
    private Size initialTlabSize;

    /*
     * TLAB statistics. For now, something simple shared by all threads without synchronization.
     * Will need to get per-thread, with statistics gathered globally at safepoint,
     * and with more elaborated statistics (especially wrt to TLAB refills).
     */
    static class TLABStats {
        /**
         * Count of calls to slow path from inlined tlab allocation request.
         */
        volatile long inlinedSlowPathAllocateCount = 0L;
        /**
         * Count of all calls to slow path (inlined and runtime).
         */
        volatile long runtimeSlowPathAllocateCount = 0L;

        /**
         * Count TLAB overflows.
         */
        volatile long tlabOverflowCount = 0L;

        /**
         * Leftover after refill.
         */
        volatile long leftover = 0L;

        void printTLABStats() {
            Log.println("\n\n Summary TLAB stats");
            Log.print("   inlined allocation slow-path count: ");
            Log.println(inlinedSlowPathAllocateCount);
            Log.print("   runtime allocation slow-path count: ");
            Log.println(runtimeSlowPathAllocateCount);
            Log.print("   tlab overflow count               :");
            Log.println(tlabOverflowCount);
            Log.print("   leftover at TLAB refill           :");
            if (leftover > Size.K.toLong()) {
                Log.print(Size.K.plus(leftover).unsignedShiftedRight(10).toLong());
                Log.println(" K");
            } else {
                Log.print(leftover);
                Log.println(" bytes");
            }
        }
    }

    final TLABStats globalTlabStats = new TLABStats();

    @HOSTED_ONLY
    public HeapSchemeWithTLAB() {
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            useTLAB = UseTLAB;
            initialTlabSize = tlabSizeOption.getValue();
            if (initialTlabSize.lessThan(0)) {
                FatalError.unexpected("Specified TLAB size is too small");
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (PrintTLABStats) {
                globalTlabStats.printTLABStats();
            }
        }
    }

    @Override
    public void disableAllocationForCurrentThread() {
        final Pointer etla = ETLA.load(currentTLA());
        final Address value = ALLOCATION_DISABLED.load(etla).asAddress();
        if (value.isZero()) {
            //Log.println("disabling heap allocation");
            // Saves TLAB's top and set it to null to force TLAB allocation to route to slow path and check if allocation is enabled.
            final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(etla);
            final Address tlabTop = TLAB_TOP.load(etla);
            if (refillPolicy == null) {
                // TLAB was never refill. So TLAB's top must be null, and we'll take the slow path anyway.
                FatalError.check(tlabTop.isZero(), "cannot have null refill policy with non-null TLAB top");
            } else {
                refillPolicy.saveTlabTop(tlabTop);
                TLAB_TOP.store(etla, Address.zero());
            }
        }
        ALLOCATION_DISABLED.store3(etla, value.plus(1));
    }

    @Override
    public void enableAllocationForCurrentThread() {
        final Pointer etla = ETLA.load(currentTLA());
        final Address value = ALLOCATION_DISABLED.load(etla).asAddress();
        if (value.isZero()) {
            FatalError.unexpected("Unbalanced calls to disable/enable allocation for current thread");
        }
        if (value.minus(1).isZero()) {
            //Log.println("enabling heap allocation");
            // Restore TLAB's top if needed.
            final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(etla);
            if (refillPolicy != null) {
                final Address tlabTop = TLABRefillPolicy.getForCurrentThread(etla).getSavedTlabTop();
                TLAB_TOP.store(etla, tlabTop);
            }
        }
        ALLOCATION_DISABLED.store3(etla, value.minus(1));
    }

    @Override
    public final boolean isAllocationDisabledForCurrentThread() {
        return !ALLOCATION_DISABLED.load(currentTLA()).isZero();
    }

    @INLINE
    @Override
    public final boolean usesTLAB() {
        return useTLAB;
    }

    public Size initialTlabSize() {
        return initialTlabSize;
    }

    public void refillTLAB(Pointer tlab, Size size) {
        final Pointer etla = ETLA.load(currentTLA());
        refillTLAB(etla, tlab, size);
    }

    /**
     * Refill the TLAB with a chunk of space allocated from the heap.
     * This basically sets the TLAB's {@link #TLAB_MARK} and {@link #TLAB_TOP} thread local variables to the
     * bounds of the allocated space.
     * Heap schemes may overrides a {@link #doBeforeTLABRefill(Pointer, Pointer)}
     * The size may be different from the initial tlab size.
     * @param tlab
     * @param size
     */
    @NO_SAFEPOINT_POLLS("heap up to allocation mark must be verifiable if debug tagging")
    public void refillTLAB(Pointer etla, Pointer tlab, Size size) {
        final Pointer tlabTop = tlab.plus(size); // top of the new TLAB
        final Pointer allocationMark = TLAB_MARK.load(etla);
        if (!allocationMark.isZero()) {
            final Pointer oldTop = TLAB_TOP.load(etla);
            globalTlabStats.leftover += oldTop.minus(allocationMark).toLong();
            // It is a refill, not an initial fill. So invoke handler.
            doBeforeTLABRefill(allocationMark, oldTop);
        } else {
            ProgramError.check(CUSTOM_ALLOCATION_ENABLED.load(etla).isZero(),
                "Must not refill TLAB when in custom allocator is set");
        }

        TLAB_TOP.store(etla, tlabTop);
        TLAB_MARK.store(etla, tlab);
        if (traceTLAB()) {
            VmThread vmThread = UnsafeCast.asVmThread(VM_THREAD.loadRef(etla).toJava());
            logger.logRefill(vmThread, tlabTop, tlabTop, tlab.plus(initialTlabSize), initialTlabSize.toInt());
        }
    }

    @INLINE
    protected final void fastRefillTLAB(Pointer etla, Pointer tlab, Size size) {
        TLAB_TOP.store(etla, tlab.plus(size));
        TLAB_MARK.store(etla, tlab);
    }

    /**
     * Handles TLAB allocation failure.
     * The handler is specified the size of the failed allocation and the allocation mark of the TLAB and must return
     * a pointer to a cell of the specified cell. The handling of the TLAB allocation failure may result in refilling the TLAB.
     *
     * @param size the failed allocation size
     * @param etla
     * @param tlabMark allocation mark of the TLAB
     * @param tlabEnd soft limit in the TLAB to trigger overflow (may equal the actual end of the TLAB, depending on implementation)
     * @return a pointer to a new allocated cell of size {@code size}
     * @throws OutOfMemoryError if the allocation request cannot be satisfied.
     */
    protected abstract Pointer handleTLABOverflow(Size size, Pointer etla, Pointer tlabMark, Pointer tlabEnd);

    /**
     * Action to perform on a TLAB before its refill with another chunk of heap.
     * Typically used for statistics gathering or formatting the leftover of the TLAB to enable heap walk.
     *
     * @param tlabAllocationMark allocation mark of the TLAB before the refill
     * @param tlabEnd end of the TLAB before the refill
     */
    protected abstract void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd);

    @NEVER_INLINE
    private static void reportAllocatedCell(Pointer cell, Size size) {
        Log.print(" tlabAllocate("); Log.print(size.toLong()); Log.print(") = "); Log.println(cell);
    }
    /**
     * The fast, inline path for allocation.
     *
     * @param size the size of memory chunk to be allocated
     * @return an allocated and zeroed chunk of memory {@code size} bytes in size
     * @throws OutOfMemoryError if the allocation request cannot be satisfied
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    protected final Pointer tlabAllocate(Size size) {
        if (MaxineVM.isDebug() && !size.isWordAligned()) {
            FatalError.unexpected("size is not word aligned in heap allocation request");
        }
        final Pointer etla = ETLA.load(currentTLA());
        final Pointer oldAllocationMark = TLAB_MARK.load(etla);
        final Pointer tlabEnd = TLAB_TOP.load(etla);
        final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);
        if (end.greaterThan(tlabEnd)) {
            return slowPathAllocate(size, etla, oldAllocationMark, tlabEnd);
        }
        TLAB_MARK.store(etla, end);

        if (MaxineVM.isDebug()) {
            TLABLog.record(etla, Pointer.fromLong(Infopoints.here()), cell, size);
        }
        return cell;
    }

    public final Pointer slowPathAllocate(Size size, Pointer etla) {
        globalTlabStats.inlinedSlowPathAllocateCount++;
        return slowPathAllocate(size, etla, TLAB_MARK.load(etla), TLAB_TOP.load(etla));
    }

    /**
     * Handling of custom allocation by sub-classes.
     * The normal allocation path. may be escaped by temporarily enabling use of a custom allocator identified with an opaque identifier.
     * The default code path retrieve this custom allocator identifier and pass it to the customAllocate method, along with the requested size.
     *
     * @param customAllocator identifier of the enabled custom allocator
     * @param size number of bytes requested to the custom allocator
     * @return pointer a the custom allocated space of the requested size
     */
    protected abstract Pointer customAllocate(Pointer customAllocator, Size size);

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    private Pointer slowPathAllocate(Size size, final Pointer etla, final Pointer oldAllocationMark, final Pointer tlabEnd) {
        globalTlabStats.runtimeSlowPathAllocateCount++;
        // Slow path may be taken because of a genuine refill request, because allocation was disabled,
        // or because allocation in immortal heap was requested.
        // Check for the second here.
        checkAllocationEnabled(size);
        // Check for custom allocation

        final Pointer customAllocator = CUSTOM_ALLOCATION_ENABLED.load(etla);
        if (!customAllocator.isZero()) {
            return customAllocate(customAllocator, size);
        }
        globalTlabStats.tlabOverflowCount++;
        // This path will always be taken if TLAB allocation is not enabled.
        return handleTLABOverflow(size, etla, oldAllocationMark, tlabEnd);
    }

    @NEVER_INLINE
    private void checkAllocationEnabled(Size size) {
        if (!ALLOCATION_DISABLED.load(currentTLA()).isZero()) {
            Log.print("Trying to allocate ");
            Log.print(size.toLong());
            Log.print(" bytes on thread ");
            Log.printCurrentThread(false);
            Log.println(" while allocation is disabled");
            FatalError.unexpected("Trying to allocate while allocation is disabled");
        }
    }

    @INLINE
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public final Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
        final Pointer cell = tlabAllocate(size);

        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    @INLINE
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public final Object createTuple(Hub hub) {
        Pointer cell = tlabAllocate(hub.tupleSize);

        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public final Object createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize;
        final Pointer cell = tlabAllocate(size);

        return Cell.plantHybrid(cell, size, hub);
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public final Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Size size = Layout.hybridLayout().getArraySize(length);
        final Pointer cell = tlabAllocate(size);
        return Cell.plantExpandedHybrid(cell, size, hybrid, length);
    }

    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public final Object clone(Object object) {
        final Size size = Layout.size(Reference.fromJava(object));
        final Pointer cell = tlabAllocate(size);
        return Cell.plantClone(cell, size, object);
    }

    @Override
    public void enableCustomAllocation(Address customAllocator) {
        super.enableCustomAllocation(customAllocator);
        if (usesTLAB()) {
            final Pointer etla = ETLA.load(currentTLA());
            final Pointer allocationMark = TLAB_MARK.load(etla);
            final Pointer tlabTop = TLAB_TOP.load(etla);

            TLAB_MARK_TMP.store(etla, allocationMark);
            TLAB_TOP_TMP.store(etla, tlabTop);
            TLAB_MARK.store(etla, Word.zero());
            TLAB_TOP.store(etla, Word.zero());
        }
    }

    @Override
    public void disableCustomAllocation() {
        super.disableCustomAllocation();
        if (usesTLAB()) {
            final Pointer etla = ETLA.load(currentTLA());
            final Pointer allocationMarkTmp = TLAB_MARK_TMP.load(etla);
            final Pointer tlabTopTmp = TLAB_TOP_TMP.load(etla);

            TLAB_MARK.store(etla, allocationMarkTmp);
            TLAB_TOP.store(etla, tlabTopTmp);
            TLAB_MARK_TMP.store(etla, Word.zero());
            TLAB_TOP_TMP.store(etla, Word.zero());
        }
    }

    @Override
    public void notifyCurrentThreadDetach() {
        tlabReset(currentTLA());
    }

    public static final TLABLogger logger = /*MaxineVM.isDebug()*/ true  ? new TLABLogger(true) : new TLABLogger();

    @HOSTED_ONLY
    @VMLoggerInterface(defaultConstructor = true)
    private interface TLabLoggerInterface  {
        void reset(
            @VMLogParam(name = "vmThread") VmThread vmThread,
            @VMLogParam(name = "tlabTop") Pointer tlabTop,
            @VMLogParam(name = "tlabMark") Pointer tlabMark);

        void refill(
            @VMLogParam(name = "vmThread") VmThread vmThread,
            @VMLogParam(name = "tlab") Pointer tlab,
            @VMLogParam(name = "tlabTop") Pointer tlabTop,
            @VMLogParam(name = "tlabEnd") Pointer tlabEnd,
            @VMLogParam(name = "initialTlabSize") int initialTlabSize);

        void pad(
            @VMLogParam(name = "vmThread") VmThread vmThread,
            @VMLogParam(name = "tlabMark") Pointer tlabMark,
            @VMLogParam(name = "padWords") int padWords);
    }

    public static final class TLABLogger extends TLabLoggerAuto {
        TLABLogger(boolean active) {
            super("TLAB", null);
        }

        TLABLogger() {
        }

        @Override
        public void checkOptions() {
            super.checkOptions();
            checkDominantLoggerOptions(Heap.gcAllLogger);
        }

        @Override
        protected void traceReset(VmThread vmThread, Pointer tlabTop, Pointer tlabMark) {
            Log.printThread(vmThread, false);
            Log.print(": Resetting TLAB [TOP=");
            Log.print(tlabTop);
            Log.print(", MARK=");
            Log.print(tlabMark);
            Log.println("]");
        }

        @Override
        protected void tracePad(VmThread vmThread, Pointer tlabMark, int padWords) {
            Log.printThread(vmThread, false);
            Log.print(": Placed TLAB padding at ");
            Log.print(tlabMark);
            Log.print(" [words=");
            Log.print(padWords);
            Log.println("]");
        }

        @Override
        protected void traceRefill(VmThread vmThread, Pointer tlab, Pointer tlabTop, Pointer tlabEnd, int initialTlabSize) {
            Log.printThread(vmThread, false);
            Log.print(": Refill TLAB with [MARK = ");
            Log.print(tlab);
            Log.print(", TOP=");
            Log.print(tlabTop);
            Log.print(", end=");
            Log.print(tlabEnd);
            Log.print(", size=");
            Log.print(initialTlabSize);
            Log.println("]");
        }

    }

// START GENERATED CODE
    private static abstract class TLabLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Pad, Refill, Reset;

            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected TLabLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        protected TLabLoggerAuto() {
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logPad(VmThread vmThread, Pointer tlabMark, int padWords) {
            log(Operation.Pad.ordinal(), vmThreadArg(vmThread), tlabMark, intArg(padWords));
        }
        protected abstract void tracePad(VmThread vmThread, Pointer tlabMark, int padWords);

        @INLINE
        public final void logRefill(VmThread vmThread, Pointer tlab, Pointer tlabTop, Pointer tlabEnd, int initialTlabSize) {
            log(Operation.Refill.ordinal(), vmThreadArg(vmThread), tlab, tlabTop, tlabEnd, intArg(initialTlabSize));
        }
        protected abstract void traceRefill(VmThread vmThread, Pointer tlab, Pointer tlabTop, Pointer tlabEnd, int initialTlabSize);

        @INLINE
        public final void logReset(VmThread vmThread, Pointer tlabTop, Pointer tlabMark) {
            log(Operation.Reset.ordinal(), vmThreadArg(vmThread), tlabTop, tlabMark);
        }
        protected abstract void traceReset(VmThread vmThread, Pointer tlabTop, Pointer tlabMark);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Pad
                    tracePad(toVmThread(r, 1), toPointer(r, 2), toInt(r, 3));
                    break;
                }
                case 1: { //Refill
                    traceRefill(toVmThread(r, 1), toPointer(r, 2), toPointer(r, 3), toPointer(r, 4), toInt(r, 5));
                    break;
                }
                case 2: { //Reset
                    traceReset(toVmThread(r, 1), toPointer(r, 2), toPointer(r, 3));
                    break;
                }
            }
        }
    }

// END GENERATED CODE


}

