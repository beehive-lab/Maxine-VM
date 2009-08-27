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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.runtime.*;

/**
 * Immortal Heap management.
 *
 * @author Hannes Payer
 */
public final class ImmortalHeap {

    /**
     * VM option to trace immortal heap allocations.
     */
    public static final VMBooleanXXOption traceAllocation
        = register(new VMBooleanXXOption("-XX:-TraceImmortal", "Trace allocation from the immortal heap."), MaxineVM.Phase.STARTING);

    private ImmortalHeap() {
    }

    /**
     * VM option to set the size of the immortal heap (MaxPermSize called in Hotspot).
     */
    public static final VMSizeOption maxPermSize =
        register(new VMSizeOption("-XX:MaxPermSize=", Size.M.times(32),
            "Size of immortal heap."), MaxineVM.Phase.PRISTINE);

    private static final ImmortalMemoryRegion immortalHeap = new ImmortalMemoryRegion();

    /**
     * Is immortal heap tracing turned on?
     * @return
     */
    public static boolean traceAllocation() {
        return traceAllocation.getValue();
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
     * @param size
     * @param adjustForDebugTag
     * @return pointer to allocated object
     */
    public static Pointer allocate(Size size, boolean adjustForDebugTag) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = immortalHeap.mark().asPointer();
            cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
            end = cell.plus(size);

            if (end.greaterThan(immortalHeap.end())) {
                FatalError.unexpected("Out of memory error in immortal memory region");
            }
        } while (immortalHeap.mark.compareAndSwap(oldAllocationMark, end) != oldAllocationMark);

        return oldAllocationMark;
    }

    /**
     * This method should be called to allocate an object on the immortal heap.
     * !!! Attention !!!
     * This method is like a plain allocation method for a class. Just memory is allocated for a given class,
     * but no constructor is called.
     * This method is probably removed in the future if it is not used.
     * @param javaClass
     * @return allocated object
     */
    public static Object allocate(Class javaClass) {
        Heap.enableImmortalMemoryAllocation();
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        MakeClassInitialized.makeClassInitialized(classActor);
        Object object;
        if (classActor.isArrayClassActor()) {
            object = Heap.createArray(classActor.dynamicHub(), 0);
        } else if (classActor.isTupleClassActor()) {
            object = Heap.createTuple(classActor.dynamicHub());
        } else {
            object = null;
        }
        Heap.disableImmortalMemoryAllocation();
        return object;
    }

    /**
     * Initialize the immortal heap memory.
     */
    public static void initialize() {
        immortalHeap.initialize(maxPermSize.getValue());
    }

    /**
     * Visit the cells in the immortal heap.
     *
     * @param cellVisitor the visitor to call back for each cell in each region
     */
    public static void visitCells(CellVisitor cellVisitor) {
        Pointer firstCell = immortalHeap.start().asPointer();
        Pointer cell = firstCell;
        while (cell.lessThan(immortalHeap.mark())) {
            cell = DebugHeap.checkDebugCellTag(firstCell, cell);
            cell = cellVisitor.visitCell(cell);
        }
    }
}
