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
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;

/**
 * @author Christos Kotselidis
 * @author Laurent Daynes
 */

public final class BeltManager {
    private final BeltwayHeapScheme heapScheme;
    private final Belt [] belts;

    private Belt applicationHeap;

    private Belt tempBelt;

    public BeltManager(BeltwayHeapScheme heapScheme) {
        this.heapScheme = heapScheme;
        applicationHeap = new Belt();
        tempBelt = new Belt();
        final String [] beltDescriptions = heapScheme.beltDescriptions();
        belts = new Belt[beltDescriptions.length];
        for (int i = 0; i < belts.length; i++) {
            belts[i] = new Belt(i, beltDescriptions[i]);
        }
    }

    public void swapBelts(Belt from, Belt to) {
        final Address oldFromSpaceStart = from.start();
        final Address oldFromSpaceStop = from.end();
        final Address oldAllocationMark = from.getAllocationMark();
        from.setStart(to.start());
        from.setEnd(to.end());
        from.setAllocationMark(to.getAllocationMark());
        to.setStart(oldFromSpaceStart);
        to.setEnd(oldFromSpaceStop);
        to.setAllocationMark(oldAllocationMark);

    }

    public int numberOfBelts() {
        return belts.length;
    }

    public void initializeBelts() {
        final Size heapSize = heapScheme.getMaxHeapSize();
        Address nextBeltStart = heapScheme.getHeapStart();
        int [] percentagesOfHeapMemoryPerBelt = heapScheme.beltHeapPercentage();
        tempBelt.resetAllocationMark();
        int beltIndex = 0;
        Belt belt = null;
        while (beltIndex < numberOfBelts() - 1) {
            belt = belts[beltIndex];
            belt.setStart(nextBeltStart);
            final Size beltSize =  heapSize.times(percentagesOfHeapMemoryPerBelt[beltIndex]).dividedBy(100).roundedUpBy(BeltwayHeapScheme.BELT_ALIGNMENT).asSize();
            belt.setSize(beltSize);
            nextBeltStart = nextBeltStart.plus(beltSize);
            belt.resetAllocationMark();
            beltIndex++;
        }
        // Initialize the last belt.
        // Because of the alignment the last belt will be a little bit smaller
        // than the previous ones, as it has to stop at the belt's boundaries
        belt = belts[beltIndex];
        belt.setStart(nextBeltStart);
        belt.setEnd(heapScheme.getHeapStart().plus(heapScheme.getMaxHeapSize()));
        belt.resetAllocationMark();

    }

    @INLINE
    public Address getEnd() {
        return belts[numberOfBelts() - 1].end();
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Pointer gcAllocate(Belt belt, Size size) {
        return belt.gcAllocate(size);
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Pointer gcBumpAllocate(Belt belt, Size size) {
        return belt.gcBumpAllocate(size);
    }

    @INLINE
    public Belt getBelt(int index) {
        return belts[index];
    }

    @INLINE
    public Belt getBeltBeforeLastAllocation(Belt from) {
        from.setEnd(from.getAllocationMarkSnapshot().asAddress());
        return from;
    }

    @INLINE
    public Belt getRemainingOverlappingBelt(Belt from) {
        tempBelt.setStart(from.getPrevAllocationMark());
        tempBelt.setEnd(from.end());
        tempBelt.setAllocationMark(from.getAllocationMark());
        return tempBelt;
    }

    @INLINE
    public boolean checkOverlappingBelts(Belt from, Belt to) {
        return (from.getAllocationMark().greaterThan(to.start()) || from.end().greaterThan(to.start())) ? true : false;
    }

    @INLINE
    public Belt getApplicationHeap() {
        applicationHeap.setStart(heapScheme.getHeapStart());
        applicationHeap.setSize(heapScheme.getMaxHeapSize());
        return applicationHeap;
    }


    public void printBeltsInfo() {
        for (int i = 0; i < belts.length; i++) {
            belts[i].printInfo();
        }
    }

    public Size reportFreeSpace() {
        Size freeSize = Size.zero();
        for (int i = 0; i <  belts.length; i++) {
            freeSize = freeSize.plus(belts[i].getRemainingMemorySize());
        }
        return freeSize;
    }

    public Size reportUsedSpace() {
        Size usedSize = Size.zero();
        for (int i = 0; i <  belts.length; i++) {
            usedSize = usedSize.plus(belts[i].getUsedMemorySize());
        }
        return usedSize;
    }

    public void printTotalMemory() {
        Size size = Heap.bootHeapRegion.size().plus(Code.bootCodeRegion.size()).plus(Code.getCodeSize());
        for (int i = 0; i <  belts.length; i++) {
            size = size.plus(belts[i].size());
        }
        Log.print("Total Memory: ");
        Log.println(size.toLong());
    }

}
