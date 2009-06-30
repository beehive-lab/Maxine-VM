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

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.thread.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayCellVisitorImpl implements BeltWayCellVisitor {

    public BeltwayCellVisitorImpl() {

    }

    public static void linearVisitAllCells(BeltWayCellVisitor cellVisitor, Action action, RuntimeMemoryRegion source, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        Pointer cell = source.start().asPointer();
        while (cell.lessThan(source.getAllocationMark())) {
            cell = DebugHeap.checkDebugCellTag(from, cell);
            cell = cellVisitor.visitCell(cell, action, from, to);
        }
    }

    public static void linearVisitTLAB(TLAB tlab, BeltWayCellVisitor cellVisitor, Action action, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        Pointer cell = tlab.start().asPointer();
        final Pointer initialEnd = tlab.end().asPointer();
        while (cell.lessThan(tlab.getAllocationMark()) && cell.lessThan(initialEnd)) {
            cell = DebugHeap.checkDebugCellTag(from, cell);
            cell = cellVisitor.visitCell(cell, action, from, to);
        }
    }

    public static void linearVisitAllCellsTLAB(BeltWayCellVisitor cellVisitor, Action action, Pointer tlabStart, Pointer tlabEnd, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        Pointer cell = tlabStart;
        while (cell.lessThan(tlabEnd)) {
            cell = DebugHeap.checkDebugCellTag(from, cell);
            cell = cellVisitor.visitCell(cell, action, from, to);
        }
    }

    public static void linearVisitAllCellsBelt(BeltWayCellVisitor cellVisitor, Action action, Belt source, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        Pointer cell = source.start().asPointer();
        VmThread thread = VmThread.current();
        TLAB currentTLAB = thread.getTLAB();
        while (cell.lessThan(currentTLAB.getAllocationMark())) {
            cell = DebugHeap.checkDebugCellTag(from, cell);
            cell = cellVisitor.visitCell(cell, action, from, to);
            thread = VmThread.current();
            currentTLAB = thread.getTLAB();
        }
    }

    public static void visitOriginOffsets(Hub hub, Pointer origin, BeltWayPointerOffsetVisitor offsetVisitor, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final int n = hub.referenceMapStartIndex() + hub.referenceMapLength();
        for (int i = hub.referenceMapStartIndex(); i < n; i++) {
            final int offset = hub.getInt(i);
            offsetVisitor.visitPointerOffset(origin, offset, from, to);
        }
    }

    public Pointer visitCell(Pointer cell, Action action, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final Pointer origin = Layout.cellToOrigin(cell);

        final Grip oldHubGrip = Layout.readHubGrip(origin); // Reads the hub-Grip of the previously retrieved object.

        // Grips are used for GC purpose.
        final Grip newHubGrip = action.doAction(oldHubGrip, from, to);

        if (newHubGrip != oldHubGrip) {
            Layout.writeHubGrip(origin, newHubGrip);
        }
        final Hub hub = UnsafeLoophole.cast(newHubGrip.toJava());
        final SpecificLayout specificLayout = hub.specificLayout();

        if (specificLayout.isTupleLayout()) {
            visitOriginOffsets(hub, origin, ((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).pointerOffsetGripUpdater(), from, to);
            return cell.plus(hub.tupleSize());
        }
        if (specificLayout.isHybridLayout()) {
            visitOriginOffsets(hub, origin, ((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).pointerOffsetGripUpdater(), from, to);
        } else if (specificLayout.isReferenceArrayLayout()) {
            scanReferenceArray(action, origin, from, to);
        }
        return cell.plus(Layout.size(origin));
    }

    private static void scanReferenceArray(Action action, Pointer origin, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final int length = Layout.readArrayLength(origin);
        for (int index = 0; index < length; index++) {
            final Grip oldGrip = Layout.getGrip(origin, index);
            final Grip newGrip = action.doAction(oldGrip, from, to);
            if (newGrip != oldGrip) {
                Layout.setGrip(origin, index, newGrip);
            }
        }
    }

}
