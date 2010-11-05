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
/**
 * @author Hannes Payer
 */
package com.sun.max.vm.heap;

import java.lang.management.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.runtime.*;

/**
 * Immortal Heap management.
 *
 * @author Hannes Payer
 */
public final class ImmortalHeap {

    private ImmortalHeap() {
    }

    @INSPECTED
    private static final ImmortalMemoryRegion immortalHeap = new ImmortalMemoryRegion("Heap-Immortal");

    /**
     * VM option to trace immortal heap allocations.
     */
    public static boolean TraceImmortal;
    static {
        VMOptions.addFieldOption("-XX:", "TraceImmortal", ImmortalHeap.class,
            "Trace allocation from the immortal heap.", MaxineVM.Phase.PRISTINE);
    }

    /**
     * VM option to set the size of the immortal heap. Maxine currently only supports a non-growable
     * immortal heap and so the greater of this option and the {@link #MaxPermSize} option is allocated.
     */
    private static Size PermSize = Size.M.times(1);
    static {
        // FIXME: immortal heap is initialized at PRIMORDIAL time, so the value specified on the command line will always be ignored!
        // Beside, PermSize shouldn't really be used as it clash with what it really means in the HotSpot VM
        VMOptions.addFieldOption("-XX:", "PermSize", ImmortalHeap.class, "Size of immortal heap.", MaxineVM.Phase.PRISTINE);
    }

    /**
     * VM option to set the size of the immortal heap. Maxine currently only supports a non-growable
     * immortal heap and so the greater of this option and the {@link #PermSize} option is allocated.
     */
    private static Size MaxPermSize = Size.M.times(1);
    static {
        VMOptions.addFieldOption("-XX:", "MaxPermSize", ImmortalHeap.class, "Size of immortal heap.", MaxineVM.Phase.PRISTINE);
    }

    /**
     * Returns the immortal heap memory.
     * @return immortal heap
     */
    public static ImmortalMemoryRegion getImmortalHeap() {
        return immortalHeap;
    }

    /**
     * This method is called by the allocator when immortal allocation is turned on.
     *
     * NOTE: The caller must ensure that this allocation and the subsequent planting
     * of object header in the allocated cell is atomic.
     *
     * @param size
     * @param adjustForDebugTag
     * @return pointer to allocated object
     */
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public static Pointer allocate(Size size, boolean adjustForDebugTag) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        size = size.wordAligned();
        do {
            oldAllocationMark = immortalHeap.mark().asPointer();
            cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
            end = cell.plus(size);

            if (end.greaterThan(immortalHeap.end())) {
                FatalError.unexpected("Out of memory error in immortal memory region");
            }
        } while (immortalHeap.mark.compareAndSwap(oldAllocationMark, end) != oldAllocationMark);

        // Zero the allocated chunk
        Memory.clearWords(cell, size.dividedBy(Word.size()).toInt());

        if (TraceImmortal) {
            traceAllocation(size, cell);
        }

        return cell;
    }

    private static void traceAllocation(Size size, Pointer cell) {
        if (!cell.isZero()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printCurrentThread(false);
            Log.print(": Allocated chunk in immortal memory at ");
            Log.print(cell);
            Log.print(" [size ");
            Log.print(size.wordAligned().toInt());
            Log.print(", end=");
            Log.print(cell.plus(size.wordAligned()));
            Log.println(']');
            Log.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Initialize the immortal heap memory.
     */
    public static void initialize() {
        immortalHeap.initialize(Size.fromLong(Math.max(MaxPermSize.toLong(), PermSize.toLong())));
    }

    public static void initialize(MemoryRegion memoryRegion) {
        immortalHeap.initialize(memoryRegion);
    }

    /**
     * Visit the cells in the immortal heap.
     *
     * @param cellVisitor the visitor to call back for each cell in each region
     */
    public static void visitCells(CellVisitor cellVisitor) {
        Pointer firstCell = immortalHeap.start().asPointer();
        final Pointer lastCell = immortalHeap.mark();
        Pointer cell = firstCell;
        while (cell.lessThan(lastCell)) {
            cell = DebugHeap.checkDebugCellTag(firstCell, cell);
            cell = cellVisitor.visitCell(cell);
        }
    }

    public static MemoryManagerMXBean getMemoryManagerMXBean() {
        return new ImmortalHeapMemoryManagerMXBean("Immortal");
    }

    private static class ImmortalHeapMemoryManagerMXBean extends MemoryManagerMXBeanAdaptor {
        ImmortalHeapMemoryManagerMXBean(String name) {
            super(name);
            add(new ImmortalMemoryPoolMXBean(immortalHeap, this));
        }
    }

    private static class ImmortalMemoryPoolMXBean extends MemoryPoolMXBeanAdaptor {
        ImmortalMemoryPoolMXBean(MemoryRegion region, MemoryManagerMXBean manager) {
            super(MemoryType.HEAP, region, manager);
        }
    }
}
