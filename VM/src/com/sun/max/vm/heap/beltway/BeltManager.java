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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;

/**
 * @author Christos Kotselidis
 */

public final class BeltManager {

    // Initial number of belts, used only for the bootstrap of the heap
    private static final int MAX_INITIAL_BELTS = 5;

    // The List of Belts used
    private static final List<Belt> _belts = new ArrayList<Belt>();

    // The beltSize, currently static, but it will change in Appel style GCs
    private static Size _beltSize;

    private static Belt _applicationHeap = new Belt();

    private static Belt _tempBelt = new Belt();

    public BeltManager() {
    }

    @INLINE
    public void createBelts() {
        for (int i = 0; i < MAX_INITIAL_BELTS; i++) {
            _belts.add(new Belt());
        }
    }

    @INLINE
    public void matchStartAllocationMark(Belt belt) {
        belt.setStart(belt.getAllocationMark());
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

    @INLINE
    private void initializeFirstBelt() {
        final Size size = calculateBeltSize(0);
        _belts.get(0).setStart(BeltwayHeapSchemeConfiguration.getApplicationHeapStartAddress());
        _belts.get(0).setEnd(_belts.get(0).start().plus(size));
        _belts.get(0).setIndex(0);
        _belts.get(0).setFramePercentageOfUsableMemory(BeltwayConfiguration.getPercentOfUsableMemoryPerBelt(0));
        _belts.get(0).resetAllocationMark();
        _tempBelt.resetAllocationMark();
    }


    public void initializeBelts() {
        initializeFirstBelt();
        int i;
        for (i = 1; i < BeltwayConfiguration.getNumberOfBelts() - 1; i++) {
            _belts.get(i).setStart(_belts.get(i - 1).end());
            _belts.get(i).setEnd(_belts.get(i).start().plus(calculateBeltSize(i)));
            _belts.get(i).setIndex(i);
            _belts.get(i).resetAllocationMark();
            _belts.get(i).setFramePercentageOfUsableMemory(BeltwayConfiguration.getPercentOfUsableMemoryPerBelt(i));
        }
        // Initialise the last belt.
        // Because of the alignment the last belt will be a little bit smaller
        // than the previous ones, as it has to stop at the belt's boundaries
        if (i < BeltwayConfiguration.getNumberOfBelts()) {
            _belts.get(i).setStart(_belts.get(i - 1).end());
            _belts.get(i).setEnd(_belts.get(0).start().plus(BeltwayConfiguration.getMaxHeapSize().asAddress()));
            _belts.get(i).setFramePercentageOfUsableMemory(BeltwayConfiguration.getPercentOfUsableMemoryPerBelt(i));
            _belts.get(i).setIndex(i);
            _belts.get(i).resetAllocationMark();
        }
    }

    @INLINE
    private Size calculateBeltSize(int i) {
        final Size heapSize = BeltwayHeapSchemeConfiguration.getMaxHeapSize();
        final Size beltSize = heapSize.times(BeltwayConfiguration.getPercentOfUsableMemoryPerBelt(i)).dividedBy(100);
        return beltSize.roundedUpBy(BeltwayHeapSchemeConfiguration.TLAB_SIZE.toInt()).asSize();
    }

    @INLINE
    public Size getBeltSize() {
        return _beltSize;
    }

    @INLINE
    public Address getEnd() {
        return _belts.get(BeltwayConfiguration.getNumberOfBelts() - 1).end();
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Pointer allocate(Belt belt, Size size) {
        return belt.allocate(size);
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Pointer bumpAllocate(Belt belt, Size size) {
        return belt.bumpAllocate(size);
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
        return _belts.get(index);
    }

    @INLINE
    public Belt getBeltBeforeLastAllocation(Belt from) {
        from.setEnd(from.getAllocationMarkSnapshot().asAddress());
        return from;
    }

    @INLINE
    public Belt getCustomBelt(Address startAddress, Address stopAddress, Address allocationMark) {
        _tempBelt.setStart(startAddress);
        _tempBelt.setEnd(stopAddress);
        _tempBelt.setAllocationMark(allocationMark);
        return _tempBelt;
    }

    @INLINE
    public Belt getRemainingOverlappingBelt(Belt from) {
        _tempBelt.setStart(from.getPrevAllocationMark());
        _tempBelt.setEnd(from.end());
        _tempBelt.setAllocationMark(from.getAllocationMark());
        return _tempBelt;
    }

    @INLINE
    public boolean checkOverlappingBelts(Belt from, Belt to) {
        return (from.getAllocationMark().greaterThan(to.start()) || from.end().greaterThan(to.start())) ? true : false;
    }

    @INLINE
    public static Belt getApplicationHeap() {
        _applicationHeap.setStart(BeltwayConfiguration.getApplicationHeapStartAddress());
        _applicationHeap.setEnd(BeltwayConfiguration.getApplicationHeapEndAddress());
        return _applicationHeap;
    }


    public void printBeltsInfo() {
        for (int i = 0; i < BeltwayConfiguration.getNumberOfBelts(); i++) {
            _belts.get(i).printInfo();
        }
    }

    public Size reportFreeSpace() {
        Size freeSize = Size.zero();
        for (int i = 0; i < BeltwayConfiguration.getNumberOfBelts(); i++) {
            freeSize = freeSize.plus(_belts.get(i).getRemainingMemorySize());
        }
        return freeSize;
    }

    public void printTotalMemory() {
        Size size = Heap.bootHeapRegion().size().plus(Code.bootCodeRegion().size()).plus(Code.getCodeSize());
        for (int i = 0; i < BeltwayConfiguration.getNumberOfBelts(); i++) {
            size = size.plus(_belts.get(i).size());
        }
        Log.print("Total Memory: ");
        Log.println(size.toLong());
    }

}
