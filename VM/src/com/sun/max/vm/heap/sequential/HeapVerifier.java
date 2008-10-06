/*VCSID=74bc9ce4-7cbc-4a63-bba2-68d2405820ed
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
package com.sun.max.vm.heap.sequential;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * A utility class that sequentially verifies the Heap.
 *
 * @author Christos Kotselidis
 */

public class HeapVerifier {

    private static HeapScheme _heapScheme;

    private static Belt _belt;

    private static SequentialHeapRootsScanner _heapRootsVerifier = new SequentialHeapRootsScanner();
    private static Verify _cellVerifier = new VerifyActionImpl();

    public HeapVerifier() {

    }

    public void initialize(HeapScheme heapScheme) {
        _heapScheme = heapScheme;
        _heapRootsVerifier.setHeapScheme(_heapScheme);
        _heapRootsVerifier.setPointerIndexVisitor(VMConfiguration.hostOrTarget().heapScheme().getPointerIndexGripVerifier());
    }

    public SequentialHeapRootsScanner getRootsVerifier() {
        return _heapRootsVerifier;
    }

    public Verify getCellVerificator() {
        return _cellVerifier;
    }

    public void verifyHeap(Address regionStartAddress, Address allocationMark, Belt belt) {
        _belt = belt;
        _heapRootsVerifier.run();
        if (Heap.verbose()) {
            Debug.println("Finished Roots Verification");
        }
        Pointer cell = regionStartAddress.asPointer();
        while (cell.lessThan(allocationMark)) {

            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = cell.plusWords(1);
                _cellVerifier.checkCellTag(cell);
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Grip hubGrip = Layout.readHubGrip(origin);
            FatalError.check(!hubGrip.isZero(), "null hub");
            _cellVerifier.verifyGrip(belt, hubGrip);
            final Hub hub = UnsafeLoophole.cast(hubGrip.toJava());
            _cellVerifier.checkHub(hub);
            final SpecificLayout specificLayout = hub.specificLayout();
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitOriginOffsets(hub, origin, VMConfiguration.hostOrTarget().heapScheme().getPointerOffsetGripVerifier(), _belt, _belt);
                cell = cell.plus(hub.tupleSize());
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitOriginOffsets(hub, origin, VMConfiguration.hostOrTarget().heapScheme().getPointerOffsetGripVerifier(), _belt, _belt);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        _cellVerifier.verifyGrip(belt, Layout.getGrip(origin, index));
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }

    }

}
