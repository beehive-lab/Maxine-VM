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
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * A closure for evacuating an object to a belt.
 * The closure assume single-threaded evacuation and perform a non-synchronized bump allocation
 * to allocate space in the evacuation belt.
 *
 * @author Christos Kotselidis
 */

public class CopyActionImpl implements Action {

    private final Verify verifyAction;

    private BeltwayHeapScheme heapScheme;

    public CopyActionImpl(Verify verifyAction) {
        this.verifyAction = verifyAction;
    }

    /**
     * Initialize the heap scheme this closure is being used by.
     * This needs to be decoupled from the constructor because the closure is typically created at prototyping time
     * before the heap scheme might be created.
     */
    public void initialize(BeltwayHeapScheme heapScheme) {
        this.heapScheme = heapScheme;
    }

    public Grip doAction(Grip origin, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final Pointer fromOrigin = origin.toOrigin();
        if (MaxineVM.isDebug()) {
            if (!heapScheme.contains(fromOrigin)) {
                Log.print("invalid grip: ");
                Log.println(origin.toOrigin().asAddress());
                FatalError.unexpected("invalid grip");
            }

            verifyAction.checkGripTag(origin);
        }

        if (from.contains(fromOrigin)) {
            final Grip forwardGrip = Layout.readForwardGrip(fromOrigin);
            if (!forwardGrip.isZero()) {
                return forwardGrip;
            }
            final Pointer fromCell = Layout.originToCell(fromOrigin);
            final Size size = Layout.size(fromOrigin);
            final Belt evacuationBelt = (Belt) to;
            final Pointer toCell = evacuationBelt.bumpAllocate(size);
            if (toCell.isZero()) {
                throw BeltwayHeapScheme.outOfMemoryError;
            }
            DebugHeap.writeCellTag(toCell);
            Memory.copyBytes(fromCell, toCell, size);
            final Pointer toOrigin = Layout.cellToOrigin(toCell);
            final Grip toGrip = Grip.fromOrigin(toOrigin);
            Layout.writeForwardGrip(fromOrigin, toGrip);
            return toGrip;
        }

        return origin;
    }
}
