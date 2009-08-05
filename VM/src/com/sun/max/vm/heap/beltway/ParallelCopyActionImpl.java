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

/**
 * @author Christos Kotselidis
 */
public class ParallelCopyActionImpl extends CopyActionImpl {

    public ParallelCopyActionImpl(Verify verifyAction) {
        super(verifyAction);
    }

    // FIXME: need to devirtualize this for performance reason...
    @Override
    public Grip doAction(Grip origin) {
        final Pointer fromOrigin = origin.toOrigin();
        if (MaxineVM.isDebug()) {
            verify(origin);
        }
        if (from.contains(fromOrigin)) {
            final Grip forwardGrip = Layout.readForwardGrip(fromOrigin);
            if (!forwardGrip.isZero()) {
                return forwardGrip;
            }
            final Pointer fromCell = Layout.originToCell(fromOrigin);
            final Size size = Layout.size(fromOrigin);
            final Pointer toCell = heapScheme.gcTlabAllocate(to, size);
            DebugHeap.writeCellTag(toCell);
            Memory.copyBytes(fromCell, toCell, size);
            final Pointer toOrigin = Layout.cellToOrigin(toCell);
            final Grip toGrip = Grip.fromOrigin(toOrigin);

            final Grip forwardGripValue = Layout.readForwardGripValue(fromOrigin);

            if ((Layout.compareAndSwapForwardGrip(fromOrigin, forwardGripValue, toGrip)) != forwardGripValue) {
                //Debug.println("Conflict in CAS forward Grip");
                // FIXME: BELTWAY.
                //VmThread.current().getTLAB().undoLastAllocation();
                return Layout.readForwardGrip(fromOrigin);
            }
            return toGrip;
        }
        return origin;
    }
}
