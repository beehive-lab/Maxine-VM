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
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayCellVisitorImpl implements BeltCellVisitor {
    final GripUpdaterPointerVisitor pointerVisitorGripUpdater;

    public BeltwayCellVisitorImpl(GripUpdaterPointerVisitor pointerOffsetGripUpdater) {
        this.pointerVisitorGripUpdater = pointerOffsetGripUpdater;
    }

    public void init(Belt from, Belt to) {
        pointerVisitorGripUpdater.action.init(from, to);
    }

    public static void linearVisitTLAB(BeltTLAB tlab, BeltCellVisitor cellVisitor, Belt from, Belt to) {
        cellVisitor.init(from, to);
        Pointer cell = tlab.start().asPointer();
        final Pointer initialEnd = tlab.end().asPointer();
        while (cell.lessThan(tlab.getAllocationMark()) && cell.lessThan(initialEnd)) {
            cell = DebugHeap.checkDebugCellTag(from.start(), cell);
            cell = cellVisitor.visitCell(cell);
        }
    }

    public static void linearVisitAllCellsTLAB(BeltCellVisitor cellVisitor, Pointer tlabStart, Pointer tlabEnd, Belt from, Belt to) {
        cellVisitor.init(from, to);
        Pointer cell = tlabStart;
        while (cell.lessThan(tlabEnd)) {
            cell = DebugHeap.checkDebugCellTag(from.start(), cell);
            cell = cellVisitor.visitCell(cell);
        }
    }

    @INLINE
    private Action action() {
        return pointerVisitorGripUpdater.action;
    }

    private void scanReferenceArray(Pointer origin) {
        final int length = Layout.readArrayLength(origin);
        for (int index = 0; index < length; index++) {
            final Grip oldGrip = Layout.getGrip(origin, index);
            final Grip newGrip = action().doAction(oldGrip);
            if (newGrip != oldGrip) {
                Layout.setGrip(origin, index, newGrip);
            }
        }
    }

    public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);

        final Grip oldHubGrip = Layout.readHubGrip(origin); // Reads the hub-Grip of the previously retrieved object.

        // Grips are used for GC purpose.
        final Grip newHubGrip = action().doAction(oldHubGrip);

        if (newHubGrip != oldHubGrip) {
            Layout.writeHubGrip(origin, newHubGrip);
        }
        final Hub hub = UnsafeCast.asHub(newHubGrip.toJava());
        final SpecificLayout specificLayout = hub.specificLayout;

        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, pointerVisitorGripUpdater);
            if (hub.isSpecialReference) {
                SpecialReferenceManager.discoverSpecialReference(Grip.fromOrigin(origin));
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, pointerVisitorGripUpdater);
        } else if (specificLayout.isReferenceArrayLayout()) {
            scanReferenceArray(origin);
        }
        return cell.plus(Layout.size(origin));
    }
}
