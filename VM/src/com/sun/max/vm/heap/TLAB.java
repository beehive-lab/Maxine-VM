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
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.beltway.*;

/**
 * This class implements a thread local allocation buffer.
 *
 * @author Christos Kotselidis
 */
public class TLAB extends RuntimeMemoryRegion implements Allocator {

    private Address _endAllocationMark;
    private Address _previousAllocationMark;
    private Address _nextTLAB = Address.zero();
    private Address _startScavengeAddress = Address.zero();
    private Address _stopScavengeAddress = Address.zero();

    // This variable will be written only during GC allocate TLABS
    // When 0 that TLAB (space) has not been scavenged yet.
    // It is also used for synchronization when accessing the linked list
    private Address _scavenged;
    private Pointer _scavengedPointer;

    public TLAB() {
        super();
        _mark = start();
    }

    public TLAB(Address start, Size size) {
        super(start, size);
        _mark = start();

    }

    public TLAB(Size size) {
        super(size);
        _mark = start();
        setDescription("TLAB");
    }

    public void initializeTLAB(Address newAddress) {
        setStart(newAddress);
        setSize(BeltwayHeapSchemeConfiguration.TLAB_SIZE);
        _mark = newAddress;
        if (VMConfiguration.hostOrTarget().debugging()) {
            _endAllocationMark = end().asPointer().minusWords(4);
        } else {
            _endAllocationMark = end().asPointer().minusWords(3);

        }
        _scavenged.asPointer().bitSet(0);
        _scavengedPointer = ClassActor.fromJava(TLAB.class).findLocalInstanceFieldActor("_scavenged").pointer(this);

    }

    public void initializeTLAB(Address newAddress, Address allocationMark, Size size) {
        setStart(newAddress);
        // Add one word in case of debugging, because we pass the parameter of the start
        // address without the taking account the debug word
        setSize(size);
        _mark = allocationMark;
        _previousAllocationMark = allocationMark;
        if (VMConfiguration.hostOrTarget().debugging()) {
            _endAllocationMark = end().asPointer().minusWords(4);
        } else {
            _endAllocationMark = end().asPointer().minusWords(3);

        }
        _scavenged.asPointer().bitSet(0);
        _scavengedPointer = ClassActor.fromJava(TLAB.class).findLocalInstanceFieldActor("_scavenged").pointer(this);

    }

    public void undoLastAllocation() {
        _mark = _previousAllocationMark;
    }

    @INLINE
    public final void resetTLAB() {
        _mark = Address.zero();
    }

    @INLINE
    public final void setAllocationMark(Address allocationMark) {
        _mark = allocationMark;
    }

    @INLINE
    public final void setScavengeStart(Address startScavenge) {
        _startScavengeAddress = startScavenge;
    }

    @INLINE
    public final void setScavengeStop(Address stopScavenge) {
        _stopScavengeAddress = stopScavenge;
    }

    @INLINE
    public final  boolean isSet() {
        return !_mark.isZero();
    }

    @INLINE
    public final void unSet() {
        _mark = Address.zero();
    }

    @INLINE
    public final void setNextTLAB(Address tlab) {
        _nextTLAB = tlab;
    }

    @INLINE
    public final Address getNextTLAB() {
        return _nextTLAB;
    }

    @INLINE
    public final Address getScavengeStartAddress() {
        return _startScavengeAddress;
    }

    @INLINE
    public final Address getScavengeStopAddress() {
        return _stopScavengeAddress;
    }

    @INLINE
    public final boolean isScavenged() {
        return _scavengedPointer.isBitSet(0);
    }

    @INLINE
    public final void setScavenged() {
        _scavengedPointer.setBit(0);
    }

    @INLINE
    public  final Pointer getScavengedPointer() {
        return _scavengedPointer;
    }

    @INLINE
    public final Address getScavengedAddress() {
        return _scavenged;
    }

    @INLINE
    public final void setScavengedAddress(Address scavenged) {
        _scavenged = scavenged;
    }

    @INLINE
    public final Pointer allocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _mark.asPointer();
        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);

        if (end.greaterThan(_endAllocationMark)) {
            fillTLAB();
            return Pointer.zero();
        }
        _mark = end;
        _previousAllocationMark = _mark;
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
        final Size fillingSize = _endAllocationMark.minus(_mark).asPointer().asSize();
        _endAllocationMark = end();
        Cell.plantArray(_mark.asPointer().plusWords(1), PrimitiveClassActor.BYTE_ARRAY_CLASS_ACTOR.dynamicHub(), fillingSize.toInt());
        _mark = end();
    }

    public Pointer compareAndSwapScavengePointer(Pointer suspectedValue, Pointer newValue) {
        return _scavengedPointer.compareAndSwapWord(suspectedValue, newValue).asPointer();
    }
}
