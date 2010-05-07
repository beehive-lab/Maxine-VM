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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */

public class GripVerifierPointerVisitor  extends PointerIndexVisitor {
    HeapBoundChecker heapBoundChecker;

    public void init(HeapBoundChecker heapBoundChecker) {
        this.heapBoundChecker = heapBoundChecker;
    }

    public void checkCellTag(Pointer cell) {
        if (DebugHeap.isTagging()) {
            if (!DebugHeap.isValidCellTag(cell.getWord(-1))) {
                Log.print("Error: ");
                Log.println(cell);
                Log.print("  origin: ");
                Log.println(Layout.cellToOrigin(cell));
                Log.print("cell long: ");
                Log.println(cell);
                FatalError.unexpected("missing object tag");
            }
        }
    }

    public void checkGripTag(Grip grip) {
        if (DebugHeap.isTagging()) {
            if (!grip.isZero()) {
                checkCellTag(Layout.originToCell(grip.toOrigin()));
            }
        }
    }

    public void checkHub(Hub hub) {
        Hub h = hub;
        if (h instanceof StaticHub) {
            final ClassActor classActor = hub.classActor;
            FatalError.check(classActor.staticHub() == h, "lost static hub");
            h = ObjectAccess.readHub(h);
        }
        for (int i = 0; i < 2; i++) {
            h = ObjectAccess.readHub(h);
        }
        FatalError.check(ObjectAccess.readHub(h) == h, "lost hub hub");
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        final Grip grip = pointer.getGrip(wordIndex);
        verifyGrip(grip);
    }

    public void verifyGrip(Grip grip) {
        if (grip.isZero()) {
            return;
        }

        DebugHeap.checkNonNullGripTag(grip);
        //  if (!(from.contains(origin) || Heap.bootHeapRegion.contains(origin) || Code.contains(origin))) {
        if (!heapBoundChecker.contains(grip)) {
            Log.print("Invalid grip: ");
            Log.print(grip.toOrigin());
            FatalError.unexpected("invalid grip");
        }
    }

}
