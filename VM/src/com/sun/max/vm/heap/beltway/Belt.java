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
package com.sun.max.vm.heap.beltway;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Christos Kotselidis
 */
public class Belt extends LinearAllocationMemoryRegion {
    private static final int NO_BELT_INDEX = -1;
    /**
     * The relative order of a belt. Always starts from Zero (0). The lower the value, the older
     * the generation and vice versa.
     */
    private int index;

    /**
     * Specifies if the belt is expandable or not (i.e. whether it can overlap with a contiguous memory segment).
     */
    private boolean expandable = false;

    // The address and the pointer to the address
    // of the previous allocation mark
    private volatile Address prevAllocationMark = Address.zero();

    public Belt(int index, String description) {
        super(description);
        this.index = index;
    }

    public Belt() {
        this.index = NO_BELT_INDEX;
    }

    public final Address getPrevAllocationMark() {
        return prevAllocationMark;
    }

    public final Size getUsedMemorySize() {
        return getAllocationMark().minus(start()).asSize();
    }

    public final Size getRemainingMemorySize() {
        return end().minus(getAllocationMark()).asSize();
    }

    public final void resetAllocationMark() {
        mark.set(start());
    }

    @Override
    public void setStart(Address beltStartAddress) {
        super.setStart(beltStartAddress);
        mark.set(beltStartAddress);
    }

    public void initialize(Address start, Size size) {

    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public Pointer allocate(RuntimeMemoryRegion from, Size size) {
        return null;
    }

    /**
     * Allocates a cell from the belt, using compare-and-swap to resolve any thread race condition.
     * This routine reserves an extra word before the cell for a debug tag word if debugging is on.
     *
     * If allocation fails in this routine, the belt delegates handling of the failure to the heap scheme.
     * If this one cannot  free up enough space (either via garbage collection or by extending the belt)
     * a {@link OutOfMemoryError} is thrown.
     *
     * @param size the requested cell size to be allocated
     * @return a pointer to the allocated cell
     * @throws OutOfMemoryError if the heap is full.
     */
    @NO_SAFEPOINTS("Slow path for allocation in a belt")
    @INLINE
    public final Pointer allocate(Size size) {
        do {
            final Pointer oldAllocationMark = mark();
            final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
            final Pointer newMark = cell.plus(size);

            if (newMark.greaterThan(end().asPointer())) {
                heapScheme().handleBeltAllocationFailure(size, this, true);
                // If here, we've managed to make some room (otherwise we'd had a oom). So try again.
                continue;
            }
            if (mark.compareAndSwap(oldAllocationMark, newMark) != oldAllocationMark) {
                continue;
            }
            return cell;
        } while(true);

    }

    /**
     * TLAB allocation. Same as allocation, but doesn't skew the allocation with debugging information
     * (complicate TLAB refill when debugging is on as it needs to adjust back
     *  the pointer, and the size is of the TLAB is skewed by one word).
     *
     * @param size Size of the TLAB
     * @return a pointer to a raw chunk of memory from this belt.
     */
    @NO_SAFEPOINTS("TLAB allocation")
    @INLINE
    public final Pointer allocateTLAB(Size size) {
        do {
            final Pointer oldAllocationMark = mark();
            final Pointer newMark = oldAllocationMark.plus(size);

            if (newMark.greaterThan(end().asPointer())) {
                // If here, we've managed to make some room (otherwise we'd had a oom). So try again.
                heapScheme().handleBeltAllocationFailure(size, this, false);
                // Try again.
                continue;
            }
            if (mark.compareAndSwap(oldAllocationMark, newMark) != oldAllocationMark) {
                continue;
            }
            return oldAllocationMark;
        } while(true);
    }

    /**
     * Direct allocation to a belt, unsynchronized.
     * This is used only by single-threaded GC.
     * @param size
     * @return
     */
    @NO_SAFEPOINTS("Direct, unsynchronized allocation.")
    @INLINE
    public final Pointer bumpAllocate(Size size) {
        final Pointer oldAllocationMark = mark();
        final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);

        if (!checkObjectInBelt(size)) {
            if (!expandable) {
                return Pointer.zero();
            }
        }
        mark.set(end);
        return cell;
    }

    @INLINE
    private BeltwayHeapScheme heapScheme() {
        return (BeltwayHeapScheme) VMConfiguration.target().heapScheme();
    }

    @INLINE
    public final Pointer gcAllocate(Size size) {
        final Pointer oldAllocationMark = mark();
        final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);

        if (!end.lessThan(end())) {
            if (!expandable) {
                if (Heap.verbose()) {
                    Log.println(" Trying to Overwrite contiguous spaces");
                }
                // Allocation Overflow, throw outOfMemory exception
                return Pointer.zero();
            }

            if (!end.lessThan(heapScheme().getHeapEnd())) {
                return Pointer.zero();
            }
            if (!(mark.compareAndSwap(oldAllocationMark, end) == oldAllocationMark)) {
                if (Heap.verbose()) {
                    Log.println("Conflict - Retry allocate");
                }
                return retryGCAllocate(size);
            }

        } else {
            if (!(mark.compareAndSwap(oldAllocationMark, end) == oldAllocationMark)) {
                if (Heap.verbose()) {
                    Log.println("Conflict - Retry allocate");
                }
                return retryGCAllocate(size);
            }
        }
        return cell;
    }

    @NEVER_INLINE
    private Pointer retryGCAllocate(Size size) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = mark();
            if (MaxineVM.isDebug()) {
                cell = oldAllocationMark.plusWords(1);
            } else {
                cell = oldAllocationMark;
            }
            end = cell.plus(size);
            oldAllocationMark = mark();
        } while (mark.compareAndSwap(oldAllocationMark, end) != oldAllocationMark);

        return cell;
    }

    @INLINE
    public final Pointer gcBumpAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = mark();

        if (MaxineVM.isDebug()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }

        final Pointer end = cell.plus(size);

        if (!end.lessThan(end())) {
            if (!expandable) {
                FatalError.check(mark().lessThan(end()), "GC allocation overflow");
            }
            if (Heap.verbose()) {
                Log.println(" Trying to Overwrite contiguous spaces");
            }
            mark.set(end);

        } else {
            mark.set(end);
        }
        return cell;
    }

    @INLINE
    private boolean checkObjectInBelt(Size size) {
        final Pointer oldAllocationMark = mark();
        if (oldAllocationMark.plus(size).greaterThan(end().asPointer())) {
            if (Heap.verbose()) {
                Log.println("Allocation did not fit, check whether a new frame can be fit in the belt");
                Log.println(" Check whether a new frame can be fit into the belt ");
                Log.print(" Allocation Size:");
                Log.println(size);
                Log.print(" Allocation Mark: ");
                Log.println(oldAllocationMark);
            }
            return false;
        }
        return true;

    }

    public void setAllocationMarkSnapshot() {
        prevAllocationMark = mark();
    }

    public void setAllocationMark(Address address) {
        mark.set(address);
    }

    public Pointer getAllocationMarkSnapshot() {
        return prevAllocationMark.asPointer();
    }
/*
    public final boolean checkNotExceedUsable(Size size) {
        final Size usedMemory = calculateUsedMemory();
        if (usedMemory.plus(size).greaterThan(BeltwayConfiguration.getUsableMemory())) {
            if (Heap.verbose()) {
                Log.println("Allocation is trying to exceed usable memory");
                Log.print(" Usable memory Size: ");
                Log.println(BeltwayConfiguration.getUsableMemory().toLong());
                Log.print(" Used Memory: ");
                Log.println(usedMemory.toLong());
                Log.print(" Allocation Size: ");
                Log.println(size.toLong());
                Log.print(" BeltwayConfiguration.getUsableMemory() Index: ");
                Log.println(BeltwayConfiguration.getUsableMemory().toLong());
            }
            return false;
        }
        return true;

    }

    private Size calculateUsedMemory() {
        Size usableMemory = Size.zero();
        if (mark().lessThan(start)) {
            usableMemory = usableMemory.plus(this.usableMemoryStart.plus(end())).asSize();
            usableMemory = usableMemory.plus(start().plus(mark())).asSize();
        } else {
            usableMemory = mark().minus(start).asSize();
        }
        return usableMemory;
    }*/

    public void printInfo() {
        Log.println();
        Log.print("Belt index: ");
        Log.println(index);
        Log.print("Belt start address: ");
        Log.println(start());
        Log.print("Belt stop address:  ");
        Log.println(end());
        Log.print("Belt alloc address:  ");
        Log.println(getAllocationMark());
        Log.print("Belt size: ");
        Log.println(size().toLong());
        Log.print("percentage of heap memory: ");
        Log.println(heapScheme().beltHeapPercentage()[index]);
        if (start.isAligned(BeltwayHeapScheme.BELT_ALIGNMENT)) {
            Log.println("is aligned");
        }

    }

    /**
     * Evacuate objects of the specified belt reachable from objects of this belt into this belt using the specified cell visitor.
     * This method is not multi-thread safe and cannot be used for parallel evacuation.
     *
     * @param cellVisitor visits objects of this belt to find references to evacuation candidates
     * @param from the belt that contains the objects that will be evacuated into this belt.
     */
    public final void evacuate(BeltCellVisitor cellVisitor, Belt from) {
        cellVisitor.init(from, this);
        Pointer cell = start.asPointer();
        while (cell.lessThan(getAllocationMark())) {
            cell = DebugHeap.checkDebugCellTag(start, cell);
            cell = cellVisitor.visitCell(cell);
        }
    }
}
