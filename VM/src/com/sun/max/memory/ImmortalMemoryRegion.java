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
package com.sun.max.memory;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * Immortal memory region can be used for objects, which are not collected by the GC.
 *
 * @author Hannes Payer
 */
public class ImmortalMemoryRegion extends RuntimeMemoryRegion{

    public void initialize(int size) {
        Pointer region = Memory.allocate(size);
        if (region.equals(Pointer.zero())) {
            FatalError.unexpected("Initialization of immortal memory region failed");
        }
        this.size = Size.fromInt(size);
        this.start = region;
        this.mark.set(region);
    }

    public synchronized Object allocate(Class javaClass) {
        if (ImmortalHeap.traceAllocation.getValue()) {
            Log.print("Allocation on immortal heap: ");
        }
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        MakeClassInitialized.makeClassInitialized(classActor);
        if (classActor.isArrayClassActor()) {
            Object object =  createArray(classActor.dynamicHub(), 0);
            if (ImmortalHeap.traceAllocation.getValue()) {
                Heap.traceCreateArray(classActor.dynamicHub(), 0, object);
            }
            return object;
        }
        if (classActor.isTupleClassActor()) {
            Object object = createTuple(classActor.dynamicHub());
            if (ImmortalHeap.traceAllocation.getValue()) {
                Heap.traceCreateTuple(classActor.dynamicHub(), object);
            }
            return object;
        }
        return null;
    }

    private Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
        final Pointer cell = immortalMemoryAllocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    public final Object createTuple(Hub hub) {
        final Pointer cell = immortalMemoryAllocate(hub.tupleSize);
        return Cell.plantTuple(cell, hub);
    }

    private Pointer immortalMemoryAllocate(Size size) {
        final Pointer allocation = mark().asPointer();
        final Pointer newMark = mark().plus(size.toInt());
        if (mark().asPointer().equals(Pointer.zero()) || newMark.greaterThan(start().plus(size().toInt()))) {
            FatalError.unexpected("Out of memory error in immortal memory region");
        }
        mark.set(newMark);
        return allocation;
    }

    /**
     * Visit the cells in all the code regions in this code manager.
     *
     * @param cellVisitor the visitor to call back for each cell in each region
     * @param includeBootCode specifies if the cells in the {@linkplain Code#bootCodeRegion() boot code region} should
     *            also be visited
     */
    public void visitCells(CellVisitor cellVisitor) {
        Pointer firstCell = start().asPointer();
        Pointer cell = firstCell;
        while (cell.lessThan(mark())) {
            cell = DebugHeap.checkDebugCellTag(firstCell, cell);
            cell = cellVisitor.visitCell(cell);
        }
        Log.println();
    }
}
