/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.atomic.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.type.*;

/**
 * This class implements a thread local allocation buffer.
 *
 * @author Christos Kotselidis
 */
public class BeltTLAB extends LinearAllocationMemoryRegion {

    private Address endAllocationMark;
    private Address previousAllocationMark;
    private Address nextTLAB = Address.zero();
    private Address startScavengeAddress = Address.zero();
    private Address stopScavengeAddress = Address.zero();

    // This variable will be written only during GC allocate TLABS
    // When 0 that TLAB (space) has not been scavenged yet.
    // It is also used for synchronization when accessing the linked list
    private final AtomicWord scavenged = new AtomicWord();

    public BeltTLAB() {
        mark.set(start());
        setRegionName("TLAB");
    }

    @INLINE
    private Pointer scavenged() {
        return scavenged.get().asPointer();
    }

    public void initializeTLAB(Address newAddress) {
        setStart(newAddress);
        setSize(BeltwayHeapSchemeConfiguration.TLAB_SIZE);
        mark.set(newAddress);
        if (MaxineVM.isDebug()) {
            endAllocationMark = end().asPointer().minusWords(4);
        } else {
            endAllocationMark = end().asPointer().minusWords(3);

        }
        scavenged().bitSet(0);
    }

    public void initializeTLAB(Address newAddress, Address allocationMark, Size size) {
        setStart(newAddress);
        // Add one word in case of debugging, because we pass the parameter of the start
        // address without the taking account the debug word
        setSize(size);
        mark.set(allocationMark);
        previousAllocationMark = allocationMark;
        if (MaxineVM.isDebug()) {
            endAllocationMark = end().asPointer().minusWords(4);
        } else {
            endAllocationMark = end().asPointer().minusWords(3);

        }
        scavenged().bitSet(0);
    }

    public void undoLastAllocation() {
        mark.set(previousAllocationMark);
    }

    @INLINE
    public final void resetTLAB() {
        mark.set(Address.zero());
    }

    @INLINE
    public final void setAllocationMark(Address allocationMark) {
        mark.set(allocationMark);
    }

    @INLINE
    public final void setScavengeStart(Address startScavenge) {
        startScavengeAddress = startScavenge;
    }

    @INLINE
    public final void setScavengeStop(Address stopScavenge) {
        stopScavengeAddress = stopScavenge;
    }

    @INLINE
    public final  boolean isSet() {
        return !mark().isZero();
    }

    @INLINE
    public final void unSet() {
        mark.set(Address.zero());
    }

    @INLINE
    public final void setNextTLAB(Address tlab) {
        nextTLAB = tlab;
    }

    @INLINE
    public final Address getNextTLAB() {
        return nextTLAB;
    }

    @INLINE
    public final Address getScavengeStartAddress() {
        return startScavengeAddress;
    }

    @INLINE
    public final Address getScavengeStopAddress() {
        return stopScavengeAddress;
    }

    @INLINE
    public final boolean isScavenged() {
        return scavenged().isBitSet(0);
    }

    @INLINE
    public final void setScavenged() {
        scavenged().bitSet(0);
    }

    @INLINE
    public final Pointer allocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = mark();
        if (MaxineVM.isDebug()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);

        if (end.greaterThan(endAllocationMark)) {
            fillTLAB();
            return Pointer.zero();
        }
        mark.set(end);
        previousAllocationMark = mark();
        return cell;
    }

    @INLINE
    public final Pointer allocate(RuntimeMemoryRegion space, Size size) {
        return null;
    }

    @INLINE
    public final boolean isFull() {
        return getAllocationMark() == end();
    }

    @INLINE
    public final void fillTLAB() {
        final Size fillingSize = endAllocationMark.minus(mark()).asPointer().asSize();
        endAllocationMark = end();
        Cell.plantArray(mark().plusWords(1), ClassRegistry.BYTE_ARRAY.dynamicHub(), fillingSize.toInt());
        mark.set(end());
    }

    public Pointer compareAndSwapScavenge(Pointer expectedValue, Pointer newValue) {
        return scavenged.compareAndSwap(expectedValue, newValue).asPointer();
    }
}
