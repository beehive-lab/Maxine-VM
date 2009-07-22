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
import com.sun.max.vm.runtime.*;

/**
 * A utility class that sequentially verifies the Heap.
 *
 * @author Christos Kotselidis
 */
public class BeltwayHeapVerifier {

    private static Belt belt;

    private final BeltwaySequentialHeapRootsScanner heapRootsVerifier;
    private static Verify cellVerifier = new VerifyActionImpl();

    public BeltwayHeapVerifier() {
        heapRootsVerifier = new BeltwaySequentialHeapRootsScanner();
    }

    public void initialize(BeltwayHeapScheme heapScheme) {
        heapRootsVerifier.setHeapScheme(heapScheme);
        heapRootsVerifier.setBeltwayPointerIndexVisitor(((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).pointerIndexGripVerifier());
    }

    public BeltwaySequentialHeapRootsScanner getRootsVerifier() {
        return heapRootsVerifier;
    }

    public Verify getCellVerificator() {
        return cellVerifier;
    }

    private final PointerOffsetVisitor pointerOffsetVisitor = new PointerOffsetVisitor() {
        public void visitPointerOffset(Pointer pointer, int offset) {
            ((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).pointerOffsetGripVerifier().visitPointerOffset(pointer, offset, belt, belt);
        }
    };

    public void verifyHeap(Address regionStartAddress, Address allocationMark, Belt belt) {
        BeltwayHeapVerifier.belt = belt;
        heapRootsVerifier.run();
        if (Heap.verbose()) {
            Log.println("Finished Roots Verification");
        }
        Pointer cell = regionStartAddress.asPointer();
        while (cell.lessThan(allocationMark)) {
            cell = DebugHeap.checkDebugCellTag(Address.zero(), cell);
            final Pointer origin = Layout.cellToOrigin(cell);
            final Grip hubGrip = Layout.readHubGrip(origin);
            FatalError.check(!hubGrip.isZero(), "null hub");
            cellVerifier.verifyGrip(belt, hubGrip);
            final Hub hub = UnsafeLoophole.cast(hubGrip.toJava());
            cellVerifier.checkHub(hub);
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetVisitor);
                cell = cell.plus(hub.tupleSize);
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetVisitor);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        cellVerifier.verifyGrip(belt, Layout.getGrip(origin, index));
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }

    }
}
