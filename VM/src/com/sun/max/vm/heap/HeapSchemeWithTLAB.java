/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap;

import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A HeapScheme adaptor with support for thread local allocation buffers (TLABs). The adaptor factors out methods for
 * allocating from a TLAB, enabling / disabling allocations, and replacing the TLAB when refill is needed. Choosing when
 * to refill a TLAB, how to pad the end of the TLAB on refill, and how to refill is delegated to the HeapScheme concrete
 * implementation which also associates a TLAB Refill policy to each thread. The TLAB refill policy is currently required
 * if TLAB is used as it is also used to save/restore TLAB top on enabling/disabling of allocation.
 *
 * @author Laurent Daynes
 */
public abstract class HeapSchemeWithTLAB extends HeapSchemeAdaptor {

    public static final String TLAB_TOP_THREAD_LOCAL_NAME = "TLAB_TOP";
    public static final String TLAB_MARK_THREAD_LOCAL_NAME = "TLAB_MARK";
    public static final String TLAB_DISABLED_THREAD_LOCAL_NAME = "TLAB_DISABLED";

    /**
     * A VM option for disabling use of TLABs.
     */
    protected static final VMBooleanXXOption useTLABOption = register(new VMBooleanXXOption("-XX:+UseTLAB",
        "Use thread-local object allocation."), MaxineVM.Phase.PRISTINE);

    /**
     * A VM option for specifying the size of a TLAB. Default is 64 K.
     */
    private static final VMSizeOption tlabSizeOption = register(new VMSizeOption("-XX:TLABSize=", Size.K.times(64),
        "The size of thread-local allocation buffers."), MaxineVM.Phase.PRISTINE);

    /**
     * The top of the current thread-local allocation buffer. This will remain zero if TLABs are not
     * {@linkplain #useTLABOption enabled}.
     */
    private static final VmThreadLocal TLAB_TOP
        = new VmThreadLocal(TLAB_TOP_THREAD_LOCAL_NAME, false, "HeapSchemeWithTLAB: top of current TLAB, zero if not used");

    /**
     * The allocation mark of the current thread-local allocation buffer. This will remain zero if TLABs
     * are not {@linkplain #useTLABOption enabled}.
     */
    private static final VmThreadLocal TLAB_MARK
        = new VmThreadLocal(TLAB_MARK_THREAD_LOCAL_NAME, false, "HeapSchemeWithTLAB: allocation mark of current TLAB, zero if not used");

    /**
     * The temporary top of the current thread-local allocation buffer. This will remain zero if TLABs are not
     * {@linkplain #useTLABOption enabled}. Used when thread is allocating on the global immortal heap.
     */
    private static final VmThreadLocal TLAB_TOP_TMP
        = new VmThreadLocal("TLAB_TOP_TMP", false, "HeapSchemeWithTLAB: temporary top of current TLAB, zero if not used");

    /**
     * The temporary allocation mark of the current thread-local allocation buffer. This will remain zero if TLABs
     * are not {@linkplain #useTLABOption enabled}. Used when thread is allocating on the global immortal heap.
     */
    private static final VmThreadLocal TLAB_MARK_TMP
        = new VmThreadLocal("TLAB_MARK_TMP", false, "HeapSchemeWithTLAB: temporary allocation mark of current TLAB, zero if not used");

    /**
     * Thread-local used to disable allocation per thread.
     */
    private static final VmThreadLocal ALLOCATION_DISABLED
        = new VmThreadLocal(TLAB_DISABLED_THREAD_LOCAL_NAME, false, "HeapSchemeWithTLAB: disables per thread allocation if non-zero");

    /**
     * A procedure for resetting the TLAB of a thread.
     */
    protected static class ResetTLAB implements Pointer.Procedure {

        protected void doBeforeReset(Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabTop) {
            // Default is nothing.
        }

        public void run(Pointer vmThreadLocals) {
            final Pointer enabledVmThreadLocals = vmThreadLocals.getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            final Pointer tlabMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
            Pointer tlabTop = enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer();
            if (Heap.traceAllocation()) {
                final VmThread vmThread = UnsafeCast.asVmThread(enabledVmThreadLocals.getReference(VM_THREAD.index).toJava());
                final boolean lockDisabledSafepoints = Log.lock();
                Log.printThread(vmThread, false);
                Log.print(": Resetting TLAB [TOP=");
                Log.print(tlabTop);
                Log.print(", MARK=");
                Log.print(tlabMark);
                Log.println("]");
                Log.unlock(lockDisabledSafepoints);
            }
            if (tlabTop.equals(Address.zero())) {
                // TLAB's top can be null in only two cases:
                // (1) it has never been filled, in which case it's allocation mark is null too
                // (2) allocation has been disabled for the thread.
                if (tlabMark.equals(Address.zero()))  {
                    // No TLABs, so nothing to reset.
                    return;
                }
                FatalError.check(!ALLOCATION_DISABLED.getConstantWord().isZero(), "inconsistent TLAB state");
                TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(enabledVmThreadLocals);
                if (refillPolicy != null) {
                    // Go fetch the actual TLAB top in case the heap scheme needs it for its doBeforeReset handler.
                    tlabTop = refillPolicy.getSavedTlabTop().asPointer();
                    // Zap the TLAB top saved in the refill policy. Don't want it to be restored after GC.
                    refillPolicy.saveTlabTop(Address.zero());
                }
            }
            doBeforeReset(enabledVmThreadLocals, tlabMark, tlabTop);
            enabledVmThreadLocals.setWord(TLAB_TOP.index, Address.zero());
            enabledVmThreadLocals.setWord(TLAB_MARK.index, Address.zero());
        }
    }

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

    public HeapSchemeWithTLAB(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            useTLAB = useTLABOption.getValue();
            initialTlabSize = tlabSizeOption.getValue();
            if (initialTlabSize.lessThan(0)) {
                FatalError.unexpected("Specified TLAB size is too small");
            }
        }
    }

    @Override
    public void disableAllocationForCurrentThread() {
        final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
        final Address value = ALLOCATION_DISABLED.getConstantWord(vmThreadLocals).asAddress();
        if (value.isZero()) {
            //Log.println("disabling heap allocation");
            // Saves TLAB's top and set it to null to force TLAB allocation to route to slow path and check if allocation is enabled.
            final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(vmThreadLocals);
            final Address tlabTop = TLAB_TOP.getVariableWord(vmThreadLocals).asAddress();
            if (refillPolicy == null) {
                // TLAB was never refill. So TLAB's top must be null, and we'll take the slow path anyway.
                FatalError.check(tlabTop.isZero(), "cannot have null refill policy with non-null TLAB top");
            } else {
                refillPolicy.saveTlabTop(tlabTop);
                TLAB_TOP.setVariableWord(vmThreadLocals, Address.zero());
            }
        }
        ALLOCATION_DISABLED.setConstantWord(vmThreadLocals, value.plus(1));
    }

    @Override
    public void enableAllocationForCurrentThread() {
        final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
        final Address value = ALLOCATION_DISABLED.getConstantWord(vmThreadLocals).asAddress();
        if (value.isZero()) {
            FatalError.unexpected("Unbalanced calls to disable/enable allocation for current thread");
        }
        if (value.minus(1).isZero()) {
            //Log.println("enabling heap allocation");
            // Restore TLAB's top if needed.
            final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(vmThreadLocals);
            if (refillPolicy != null) {
                final Address tlabTop = TLABRefillPolicy.getForCurrentThread(vmThreadLocals).getSavedTlabTop();
                TLAB_TOP.setVariableWord(vmThreadLocals, tlabTop);
            }
        }
        ALLOCATION_DISABLED.setConstantWord(vmThreadLocals, value.minus(1));
    }

    @Override
    public final boolean isAllocationDisabledForCurrentThread() {
        return !ALLOCATION_DISABLED.getConstantWord().isZero();
    }

    @INLINE(override = true)
    @Override
    public boolean usesTLAB() {
        return useTLAB;
    }

    public Size initialTlabSize() {
        return initialTlabSize;
    }

    public void refillTLAB(Pointer tlab, Size size) {
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        refillTLAB(enabledVmThreadLocals, tlab, size);
    }

    /**
     * Refill the TLAB with a chunk of space allocated from the heap.
     * The size may be different from the initial tlab size.
     * @param tlab
     * @param size
     */
    public void refillTLAB(Pointer enabledVmThreadLocals, Pointer tlab, Size size) {
        final Pointer tlabTop = tlab.plus(size); // top of the new TLAB
        final Pointer allocationMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
        if (!allocationMark.isZero()) {
            // It is a refill, not an initial fill. So invoke handler.
            doBeforeTLABRefill(allocationMark, enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer());
        } else {
            ProgramError.check(enabledVmThreadLocals.getWord(IMMORTAL_ALLOCATION_ENABLED.index).isZero(),
                "Must not refill TLAB when in Immortal allocation");
        }

        enabledVmThreadLocals.setWord(TLAB_TOP.index, tlabTop);
        enabledVmThreadLocals.setWord(TLAB_MARK.index, tlab);
        if (Heap.traceAllocation() || Heap.traceGC()) {
            final boolean lockDisabledSafepoints = Log.lock();
            final VmThread vmThread = UnsafeCast.asVmThread(enabledVmThreadLocals.getReference(VM_THREAD.index).toJava());
            Log.printThread(vmThread, false);
            Log.print(": Refill TLAB with ");
            Log.print(tlab);
            Log.print(" [TOP=");
            Log.print(tlabTop);
            Log.print(", end=");
            Log.print(tlab.plus(initialTlabSize));
            Log.print(", size=");
            Log.print(initialTlabSize.toInt());
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    @INLINE
    protected final void fastRefillTLAB(Pointer enabledVmThreadLocals, Pointer tlab, Size size) {
        enabledVmThreadLocals.setWord(TLAB_TOP.index, tlab.plus(size));
        enabledVmThreadLocals.setWord(TLAB_MARK.index, tlab);
    }

    /**
     * Handles TLAB allocation failure.
     * The handler is specified the size of the failed allocation and the allocation mark of the TLAB and must return
     * a pointer to a cell of the specified cell. The handling of the TLAB allocation failure may result in refilling the TLAB.
     *
     * @param size the failed allocation size
     * @param enabledVmThreadLocals
     * @param tlabMark allocation mark of the TLAB
     * @param tlabEnd soft limit in the TLAB to trigger overflow (may equal the actual end of the TLAB, depending on implementation)
     * @return a pointer to a new allocated cell of size {@code size}
     * @throws OutOfMemoryError if the allocation request cannot be satisfied.
     */
    protected abstract Pointer handleTLABOverflow(Size size, Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabEnd);

    /**
     * Action to perform on a TLAB before its refill with another chunk of heap.
     * @param enabledVmThreadLocals
     */
    protected abstract void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd);

    /**
     * The fast, inline path for allocation.
     *
     * @param size the size of memory chunk to be allocated
     * @return an allocated and zeroed chunk of memory {@code size} bytes in size
     * @throws OutOfMemoryError if the allocation request cannot be satisfied
     */
    @INLINE
    protected final Pointer tlabAllocate(Size size) {
        if (MaxineVM.isDebug() && !size.isWordAligned()) {
            FatalError.unexpected("size is not word aligned in heap allocation request");
        }
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        final Pointer oldAllocationMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
        final Pointer tlabEnd = enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer();
        final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);
        if (end.greaterThan(tlabEnd)) {
            return slowPathAllocate(size, enabledVmThreadLocals, oldAllocationMark, tlabEnd);
        }
        enabledVmThreadLocals.setWord(TLAB_MARK.index, end);
        return cell;
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    @NEVER_INLINE
    private Pointer slowPathAllocate(Size size, final Pointer enabledVmThreadLocals, final Pointer oldAllocationMark, final Pointer tlabEnd) {
        // Slow path may be taken because of a genuine refill request, because allocation was disabled,
        // or because allocation in immortal heap was requested.
        // Check for the second here.
        checkAllocationEnabled(size);
        // Check for Immortal memory allocation.
        final Pointer immortalAllocation = enabledVmThreadLocals.getWord(IMMORTAL_ALLOCATION_ENABLED.index).asPointer();
        if (!immortalAllocation.isZero()) {
            return ImmortalHeap.allocate(size, true);
        }
        // This path will always be taken if TLAB allocation is not enabled.
        return handleTLABOverflow(size, enabledVmThreadLocals, oldAllocationMark, tlabEnd);
    }

    @NEVER_INLINE
    private void checkAllocationEnabled(Size size) {
        if (!ALLOCATION_DISABLED.getConstantWord().isZero()) {
            Log.print("Trying to allocate ");
            Log.print(size.toLong());
            Log.print(" bytes on thread ");
            Log.printCurrentThread(false);
            Log.println(" while allocation is disabled");
            FatalError.unexpected("Trying to allocate while allocation is disabled");
        }
    }

    protected final void setTlabAllocationMark(Pointer enabledVmThreadLocals, Pointer newAllocationMark) {
        enabledVmThreadLocals.setWord(TLAB_MARK.index, newAllocationMark);
    }

    @INLINE
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
        final Pointer cell = tlabAllocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    @INLINE
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object createTuple(Hub hub) {
        final Pointer cell = tlabAllocate(hub.tupleSize);
        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize;
        final Pointer cell = tlabAllocate(size);
        return Cell.plantHybrid(cell, size, hub);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Size size = Layout.hybridLayout().getArraySize(length);
        final Pointer cell = tlabAllocate(size);
        return Cell.plantExpandedHybrid(cell, size, hybrid, length);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object clone(Object object) {
        final Size size = Layout.size(Reference.fromJava(object));
        final Pointer cell = tlabAllocate(size);
        return Cell.plantClone(cell, size, object);
    }

    @Override
    public void enableImmortalMemoryAllocation() {
        super.enableImmortalMemoryAllocation();
        if (usesTLAB()) {
            final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            final Pointer allocationMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
            final Pointer tlabTop = enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer();

            enabledVmThreadLocals.setWord(TLAB_MARK_TMP.index, allocationMark);
            enabledVmThreadLocals.setWord(TLAB_TOP_TMP.index, tlabTop);
            enabledVmThreadLocals.setWord(TLAB_MARK.index, Word.zero());
            enabledVmThreadLocals.setWord(TLAB_TOP.index, Word.zero());
        }
    }

    @Override
    public void disableImmortalMemoryAllocation() {
        super.disableImmortalMemoryAllocation();
        if (usesTLAB()) {
            final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            final Pointer allocationMarkTmp = enabledVmThreadLocals.getWord(TLAB_MARK_TMP.index).asPointer();
            final Pointer tlabTopTmp = enabledVmThreadLocals.getWord(TLAB_TOP_TMP.index).asPointer();

            enabledVmThreadLocals.setWord(TLAB_MARK.index, allocationMarkTmp);
            enabledVmThreadLocals.setWord(TLAB_TOP.index, tlabTopTmp);
            enabledVmThreadLocals.setWord(TLAB_MARK_TMP.index, Word.zero());
            enabledVmThreadLocals.setWord(TLAB_TOP_TMP.index, Word.zero());
        }
    }
}

