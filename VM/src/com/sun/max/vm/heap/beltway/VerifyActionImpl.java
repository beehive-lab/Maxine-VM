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
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Christos Kotselidis
 */

public class VerifyActionImpl implements Verify {

    public VerifyActionImpl() {

    }

    @Override
    public Grip doAction(Grip grip, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        return verifyGrip(from, grip);
    }

    public void checkCellTag(Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
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
        if (VMConfiguration.hostOrTarget().buildLevel() == BuildLevel.DEBUG) {
            if (!grip.isZero()) {
                checkCellTag(Layout.originToCell(grip.toOrigin()));
            }
        }
    }

    @Override
    public void checkHub(Hub hub) {
        Hub h = hub;
        if (h instanceof StaticHub) {
            final ClassActor classActor = hub.classActor();
            checkClassActor(h.classActor());
            FatalError.check(classActor.staticHub() == h, "lost static hub");
            h = ObjectAccess.readHub(h);
        }
        for (int i = 0; i < 2; i++) {
            h = ObjectAccess.readHub(h);
        }
        FatalError.check(ObjectAccess.readHub(h) == h, "lost hub hub");
    }

    private void checkClassActor(ClassActor classActor) {
    }

    public Grip verifyGrip(RuntimeMemoryRegion from, Grip grip) {
        if (grip.isZero()) {
            return null;
        }

        final Pointer origin = grip.toOrigin();
        checkGripTag(grip);
        if (!(from.contains(origin) || Heap.bootHeapRegion().contains(origin) || Code.contains(origin))) {
            Log.print("Invalid grip: ");
            Log.print(origin);
            FatalError.unexpected("invalid grip");

        }
        return null;

    }

}
