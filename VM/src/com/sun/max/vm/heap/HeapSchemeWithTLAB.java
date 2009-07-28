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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * An HeapScheme adaptor with support for thread local allocation buffer.
 * The adaptor factors out methods for allocating from a tlab, enabling / disabling allocations,
 * and replacing the tlab when refill is needed. Choosing when to refill a TLAB and how to refill it is up to the
 * HeapScheme concrete implementation.
 *
 * Do not support fancy features to enable adaptive per-thread TLAB sizing and refill policy.
 *
 * @author Laurent Daynes
 */
public abstract class HeapSchemeWithTLAB extends HeapSchemeAdaptor {

    /**
     * A VM option for disabling use of TLABs.
     */
    private static VMBooleanXXOption useTLABOption = register(new VMBooleanXXOption("-XX:+UseTLAB2", // FIXME: this conflict with SemiHeap's space UseTLAB option
        "Use thread-local object allocation."), MaxineVM.Phase.PRISTINE);

    /**
     * A VM option for specifying the size of a TLAB. Default is 64 K.
     */
    private static VMSizeOption tlabSizeOption = register(new VMSizeOption("-XX:TLABSize2=", Size.K.times(64), // FIXME: same as above. See vmoption issue!
        "The size of thread-local allocation buffers."), MaxineVM.Phase.PRISTINE);


    /**
     * The top of the current thread-local allocation buffer. This will remain zero if TLABs are not
     * {@linkplain #useTLABOption enabled}.
     */
    private static VmThreadLocal TLAB_TOP
        = new VmThreadLocal("TLAB_TOP", Kind.WORD, "HeapSchemeWithTLAB: top of current TLAB, zero if not used");

    /**
     * The allocation mark of the current thread-local allocation buffer. This will remain zero if TLABs
     * are not {@linkplain #useTLABOption enabled}.
     */
    private static VmThreadLocal TLAB_MARK
        = new VmThreadLocal("TLAB_MARK", Kind.WORD, "HeapSchemeWithTLAB: allocation mark of current TLAB, zero if not used");

    /**
     * Thread-local used to disable allocation per thread.
     */
    private static VmThreadLocal ALLOCATION_DISABLED
        = new VmThreadLocal("TLAB_DISABLED", Kind.WORD, "HeapSchemeWithTLAB: disables per thread allocation if non-zero");

    /**
     * Local copy of Dynamic Hub for java.lang.Object to speed up filling cell with dead object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static DynamicHub OBJECT_HUB;

    /**
     * Local copy of Dynamic Hub for byte [] to speed up filling cell with dead object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static DynamicHub BYTE_ARRAY_HUB;

    /**
     * Size of an java.lang.Object instance, presumably the minimum object size.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static Size MIN_OBJECT_SIZE;
    /**
     * Size of byte array header.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static Size BYTE_ARRAY_HEADER_SIZE;

    /**
     * Plant a dead instance of java.lang.Object at the specified pointer.
     */
    private static void plantDeadObject(Pointer cell) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.tupleCellToOrigin(cell);
        Layout.writeHubReference(origin, Reference.fromJava(OBJECT_HUB));
    }


    /**
     * Plant a dead byte array at the specified cell.
     */
    private static void plantDeadByteArray(Pointer cell, Size size) {
        DebugHeap.writeCellTag(cell);
        final int length = size.minus(BYTE_ARRAY_HEADER_SIZE).toInt();
        final Pointer origin = Layout.arrayCellToOrigin(cell);
        Layout.writeArrayLength(origin, length);
        Layout.writeHubReference(origin, Reference.fromJava(BYTE_ARRAY_HUB));
    }

    /**
     * Helper function to fill an area with a dead object.
     * Used to make a dead area in the heap parseable by GCs.
     *
     * @param start start of the dead heap area
     * @param end end of the dead heap area
     */
    public static void fillWithDeadObject(Pointer start, Pointer end) {
        Pointer cell = DebugHeap.adjustForDebugTag(start);
        Size deadObjectSize = end.minus(cell).asSize();
        if (deadObjectSize.greaterThan(MIN_OBJECT_SIZE)) {
            plantDeadByteArray(cell, deadObjectSize);
        } else if (deadObjectSize.equals(MIN_OBJECT_SIZE)) {
            plantDeadObject(cell);
        } else {
            FatalError.unexpected("Not enough space to fit a dead object");
        }
    }

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
        if (phase == MaxineVM.Phase.PROTOTYPING) {
            OBJECT_HUB = TupleClassActor.fromJava(Object.class).dynamicHub();
            BYTE_ARRAY_HUB = PrimitiveClassActor.BYTE_ARRAY_CLASS_ACTOR.dynamicHub();
            MIN_OBJECT_SIZE = OBJECT_HUB.tupleSize;
            BYTE_ARRAY_HEADER_SIZE = Layout.byteArrayLayout().getArraySize(Kind.BYTE, 0);
        } else if (phase == MaxineVM.Phase.PRISTINE) {
            useTLAB = useTLABOption.getValue();
            initialTlabSize = tlabSizeOption.getValue();
            if (initialTlabSize.lessThan(0)) {
                FatalError.unexpected("Specified TLAB size is too small");
            }
        }
    }

    @Override
    public void enableAllocationForCurrentThread() {
        final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
        final Address value = ALLOCATION_DISABLED.getConstantWord(vmThreadLocals).asAddress();
        if (value.isZero()) {
            FatalError.unexpected("Unbalanced calls to disable/enable allocation for current thread");
        }
        ALLOCATION_DISABLED.setConstantWord(vmThreadLocals, value.minus(1));
    }

    @Override
    public void disableAllocationForCurrentThread() {
        final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
        final Address value = ALLOCATION_DISABLED.getConstantWord(vmThreadLocals).asAddress();
        ALLOCATION_DISABLED.setConstantWord(vmThreadLocals, value.plus(1));
    }

    public final boolean allocationDisabledForCurrentThread() {
        return !ALLOCATION_DISABLED.getConstantWord().isZero();
    }

    public boolean useTLAB() {
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
        final Pointer tlabTop = tlab.plus(size);

        doBeforeTLABRefill(enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer(), enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer());

        final Pointer cell = DebugHeap.adjustForDebugTag(tlab);
        enabledVmThreadLocals.setWord(TLAB_TOP.index, tlabTop);
        enabledVmThreadLocals.setWord(TLAB_MARK.index, cell);
        if (Heap.traceAllocation() || Heap.traceGC()) {
            final boolean lockDisabledSafepoints = Log.lock();
            final VmThread vmThread = UnsafeLoophole.cast(enabledVmThreadLocals.getReference(VM_THREAD.index).toJava());
            Log.printVmThread(vmThread, false);
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

    public Pointer tlabAllocationMark(Pointer oldMark) {
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        return enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
    }

    /**
     * Method that extension of {@link HeapSchemeWithTLAB} must implement to handle TLAB allocation failure.
     * The handler is specified the size of the failed allocation and the allocation mark of the TLAB and must return
     * a pointer to a cell of the specified cell. The handling of the TLAB allocation failure may result in refilling the TLAB.
     *
     * @param size
     * @param allocationMark
     * @return a pointer to a cell resulting from a successful allocation of space of the specified size.
     * @throws OutOfMemoryError if the allocation request cannot be satisfied.
     */
    protected abstract Pointer handleTLABOverflow(Size size, Pointer allocationMark);

    /**
     * Action to perform on a TLAB before its refill with another chunk of heap.
     * @param enabledVmThreadLocals
     */
    protected abstract void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd);

    /**
     * The fast, inline path for allocation.
     *
     * @param size the size of memory chunk to be allocated
     * @return an allocated chunk of memory {@code size} bytes in size
     * @throws OutOfMemoryError if the allocation request cannot be satisfied
     */
    @INLINE
    protected final Pointer tlabAllocate(Size size) {
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        final Pointer oldAllocationMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
        final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);
        if (end.greaterThan(enabledVmThreadLocals.getWord(TLAB_TOP.index).asAddress())) {
            // This path will always be taken if TLAB allocation is not enabled
            return handleTLABOverflow(size, oldAllocationMark);
        }
        enabledVmThreadLocals.setWord(TLAB_MARK.index, end);
        return cell;
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
        final Size newSize = Layout.hybridLayout().getArraySize(length);
        final Pointer newCell = tlabAllocate(newSize);
        return Cell.plantExpandedHybrid(newCell, newSize, hybrid, length);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object clone(Object object) {
        final Size size = Layout.size(Reference.fromJava(object));
        final Pointer cell = tlabAllocate(size);
        return Cell.plantClone(cell, size, object);
    }

}

