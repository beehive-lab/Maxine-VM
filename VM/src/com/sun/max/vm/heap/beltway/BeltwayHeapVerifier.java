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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * A utility class that sequentially verifies the Heap.
 *
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */
public class BeltwayHeapVerifier {

    private final GripVerifierPointerVisitor cellVerifier = new GripVerifierPointerVisitor();

    private final SequentialHeapRootsScanner stackAndMonitorVerifier;

    public BeltwayHeapVerifier() {
        stackAndMonitorVerifier = new SequentialHeapRootsScanner(cellVerifier);
    }

    public void verifyCodeRegions(HeapBoundChecker heapBoundChecker) {
        cellVerifier.init(heapBoundChecker);
        Code.visitReferences(cellVerifier);
    }

    public void verifyThreadAndStack(HeapBoundChecker heapBoundChecker) {
        cellVerifier.init(heapBoundChecker);
        stackAndMonitorVerifier.run();
    }

    /**
     * Verifies that references within the specified region are within bounds and refers to properly formated objects.
     * @param start start of the region to be verified
     * @param end end of the region to be verified
     * @param heapBoundChecker checker that specifies the bounds for valid references.
     */
    public void verifyHeapRange(Address start, Address end, HeapBoundChecker heapBoundChecker) {
        Pointer cell = start.asPointer();
        while (cell.lessThan(end)) {
            cell = DebugHeap.checkDebugCellTag(start, cell);
            final Pointer origin = Layout.cellToOrigin(cell);
            final Grip hubGrip = Layout.readHubGrip(origin);
            FatalError.check(!hubGrip.isZero(), "null hub");
            cellVerifier.verifyGrip(hubGrip);
            final Hub hub = UnsafeLoophole.cast(hubGrip.toJava());
            cellVerifier.checkHub(hub);
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, cellVerifier);
                cell = cell.plus(hub.tupleSize);
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitReferences(hub, origin, cellVerifier);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        cellVerifier.verifyGrip(Layout.getGrip(origin, index));
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }

    }
}
