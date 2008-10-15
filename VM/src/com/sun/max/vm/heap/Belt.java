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
/*VCSID=a7c7f6a8-204c-4fa9-b6a9-324ec2e5c0bc*/
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Christos Kotselidis
 */
public class Belt extends RuntimeMemoryRegion implements Allocator, Visitor {

    /**
     * The relative order of a belt. Always starts from Zero (0). The lower the value, the older
     * the generation and vice versa.
     */
    private int _index;

    /**
     * The start address of the belt.
     */
    private Address _beltStartAddress;

    /**
     * The stop address of the belt.
     */
    private Address _beltStopAddress;

    /**
     * The size of the belt.
     */
    private Size _beltSize;

    /**
     * Specifies if the belt is expandable or not (i.e. whether it can overlap with a contiguous memory segment).
     */
    private boolean _expandable = false;

    /**
     * The percent of usable memory frames the belt occupies. This corresponds to the "X" described in the Beltway paper.
     */
    private int _framePercentageOfUsableMemory;

    /**
     * The top of allocated memory in this belt.
     */
    private volatile Address _allocationMark;

    private Pointer _allocationMarkPointer;

    // The address and the pointer to the address
    // of the previous allocation mark
    private volatile Address _prevAllocationMark = Address.zero();

    private Address _usableMemoryEnd;
    private Address _usableMemoryStart;

    private Size _usableMemory;

    public Belt(int index, Address beltStartAddress, Size beltSize, int framePercentageOfUsableMemory) {
        _index = index;
        _beltStartAddress = beltStartAddress;
        _beltStopAddress = beltStartAddress.plus(beltSize);
        _usableMemoryStart = beltStartAddress;
        _beltSize = beltSize;
        _framePercentageOfUsableMemory = framePercentageOfUsableMemory;
        _allocationMark = beltStartAddress;
        _allocationMarkPointer = ClassActor.fromJava(Belt.class).findLocalInstanceFieldActor("_allocationMark").pointer(this);

    }

    public Belt() {
    }

    @Override
    @INLINE
    public Address getAllocationMark() {
        return _allocationMark;
    }

    @INLINE
    public Address getPrevAllocationMark() {
        return _prevAllocationMark;
    }

    @INLINE
    public Size getUsedMemorySize() {
        return getAllocationMark().minus(start()).asSize();
    }

    @INLINE
    public Size getRemainingMemorySize() {
        return end().asSize().minus(getAllocationMark());
    }

    @INLINE
    public void resetAllocationMark() {
        _allocationMark = _beltStartAddress;
        _allocationMarkPointer = ClassActor.fromJava(Belt.class).findLocalInstanceFieldActor("_allocationMark").pointer(this);
    }

    @Override
    @INLINE
    public Address start() {
        return _beltStartAddress;
    }

    @Override
    @INLINE
    public Address end() {
        return _beltStopAddress;
    }

    @INLINE
    public void setStartAddress(Address beltStartAddress) {
        _beltStartAddress = beltStartAddress;
        _allocationMark = beltStartAddress;
    }

    @INLINE
    public void setStopAddress(Address beltStopAddress) {
        _beltStopAddress = beltStopAddress;
    }

    @INLINE
    public void setIndex(int index) {
        _index = index;
    }

    @INLINE
    public int getIndex() {
        return _index;
    }

    @INLINE
    public void setBeltSize(Size beltSize) {
        _beltSize = beltSize;
    }

    @INLINE
    public int getFramePercentageOfUsableMemory() {
        return _framePercentageOfUsableMemory;
    }

    @INLINE
    public void setFramePercentageOfUsableMemory(int framePercentageOfUsableMemory) {
        _framePercentageOfUsableMemory = framePercentageOfUsableMemory;
    }

    public void setExpandable(boolean expandable) {
        _expandable = expandable;
    }

    @Override
    public Size size() {
        return end().minus(start()).asSize();
    }

    @Override
    public Pointer allocate(RuntimeMemoryRegion from, Size size) {
        return null;
    }

    @NO_SAFEPOINTS("TODO")
    @Override
    @INLINE
    public Pointer allocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _allocationMark.asPointer();

        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);

        if (!checkObjectInBelt(size)) {
            if (!_expandable) {
                return Pointer.zero();
            }
        }
        if (_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark) {
            if (Heap.verbose()) {
                Debug.println("Conflict! retry-allocate");
            }
            return retryAllocate(size);
        }
        return cell;
    }

    @NO_SAFEPOINTS("TODO")
    @INLINE
    public Pointer bumpAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _allocationMark.asPointer();

        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);

        if (!checkObjectInBelt(size)) {
            if (!_expandable) {
                return Pointer.zero();
            }
        }
        _allocationMark = end;
        return cell;
    }

    @NEVER_INLINE
    private Pointer retryAllocate(Size size) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = _allocationMark.asPointer();
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = oldAllocationMark.plusWords(1);
            } else {
                cell = oldAllocationMark;
            }
            end = cell.plus(size);
            if (checkNotExceedUsable(end.asSize())) {
                return Pointer.zero();
            }
            oldAllocationMark = _allocationMark.asPointer();
        } while (_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark);
        return cell;
    }

    @INLINE
    public Pointer gcAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _allocationMark.asPointer();
        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }

        final Pointer end = cell.plus(size);

        if (!end.lessThan(end())) {
            if (!_expandable) {
                if (Heap.verbose()) {
                    Debug.println(" Trying to Overwrite contiguous spaces");
                }
                // Allocation Overflow, throw outOfMemory exception
                return Pointer.zero();
            }
            if (!end.lessThan(BeltManager.getApplicationHeap().end())) {
                return Pointer.zero();
            }
            if (!(_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) == oldAllocationMark)) {
                if (Heap.verbose()) {
                    Debug.println("Conflict - Retry allocate");
                }
                return retryGCAllocate(size);
            }

        } else {
            if (!(_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) == oldAllocationMark)) {
                if (Heap.verbose()) {
                    Debug.println("Conflict - Retry allocate");
                }
                return retryGCAllocate(size);
            }
        }
        return cell;
    }

    @NEVER_INLINE
    private Pointer retryGCAllocate(Size size) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = _allocationMark.asPointer();
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = oldAllocationMark.plusWords(1);
            } else {
                cell = oldAllocationMark;
            }
            end = cell.plus(size);
            oldAllocationMark = _allocationMark.asPointer();
        } while (_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark);

        return cell;
    }

    @INLINE
    public Pointer gcBumpAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _allocationMark.asPointer();

        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }

        final Pointer end = cell.plus(size);

        if (!end.lessThan(end())) {
            if (!_expandable) {
                FatalError.check(_allocationMark.lessThan(end()), "GC allocation overflow");
            }
            if (Heap.verbose()) {
                Debug.println(" Trying to Overwrite contiguous spaces");
            }
            _allocationMark = end;

        } else {
            _allocationMark = end;
        }
        return cell;
    }

    @INLINE
    private boolean checkObjectInBelt(Size size) {
        final Pointer oldAllocationMark = _allocationMark.asPointer();
        if (oldAllocationMark.plus(size).greaterThan(end().asPointer())) {
            if (Heap.verbose()) {
                Debug.println("Allocation did not fit, check whether a new frame can be fit in the belt");
                Debug.println(" Check whether a new frame can be fit into the belt ");
                Debug.print(" Allocation Size:");
                Debug.println(size);
                Debug.print(" Allocation Mark: ");
                Debug.println(oldAllocationMark);
            }
            return false;
        }
        return true;

    }

    @INLINE
    public void setAllocationMarkSnapshot() {
        _prevAllocationMark = _allocationMark;
    }

    @INLINE
    public void setAllocationMark(Address address) {
        _allocationMark = address;
    }

    @INLINE
    public Pointer getAllocationMarkSnapshot() {
        return _prevAllocationMark.asPointer();
    }

    @INLINE
    public boolean checkNotExceedUsable(Size size) {
        final Size usedMemory = calculateUsedMemory();
        if (usedMemory.plus(size).greaterThan(BeltwayConfiguration.getUsableMemory())) {
            if (Heap.verbose()) {
                Debug.println("Allocation is trying to exceed usable memory");
                Debug.print(" Usable memory Size: ");
                Debug.println(BeltwayConfiguration.getUsableMemory().toLong());
                Debug.print(" Used Memory: ");
                Debug.println(usedMemory.toLong());
                Debug.print(" Allocation Size: ");
                Debug.println(size.toLong());
                Debug.print(" BeltwayConfiguration.getUsableMemory() Index: ");
                Debug.println(BeltwayConfiguration.getUsableMemory().toLong());
            }
            return false;
        }
        return true;

    }

    @INLINE
    public Size calculateUsedMemory() {
        Size usableMemory = Size.zero();
        if (_allocationMark.lessThan(_usableMemoryStart)) {
            usableMemory = usableMemory.plus(_usableMemoryStart.plus(end())).asSize();
            usableMemory = usableMemory.plus(start().plus(_allocationMark)).asSize();
        } else {
            usableMemory = _allocationMark.minus(_usableMemoryStart).asSize();
        }
        return usableMemory;
    }

    @Override
    @INLINE
    public boolean contains(Address address) {
        return address.greaterEqual(start()) && address.lessThan(end());
    }

    public void printInfo() {
        Debug.println();
        Debug.print("Belt index: ");
        Debug.println(_index);
        Debug.print("Belt start address: ");
        Debug.println(_beltStartAddress);
        Debug.print("Belt stop address:  ");
        Debug.println(_beltStopAddress);
        Debug.print("Belt alloc address:  ");
        Debug.println(getAllocationMark());
        Debug.print("Belt size: ");
        Debug.println(size().toLong());
        Debug.print("Frame percentage of usable memory: ");
        Debug.println(_framePercentageOfUsableMemory);
        if (start().isAligned(HeapSchemeConfiguration.ALLIGNMENT)) {
            Debug.println("true");
        }

    }

    @INLINE
    public void visitCells(Visitor cellVisitor, Action action, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        CellVisitorImpl.linearVisitAllCells((BeltWayCellVisitor) cellVisitor, action, this, from, to);
    }

}
