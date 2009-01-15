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
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.heap.*;
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
     * Specifies if the belt is expandable or not (i.e. whether it can overlap with a contiguous memory segment).
     */
    private boolean _expandable = false;

    /**
     * The percent of usable memory frames the belt occupies. This corresponds to the "X" described in the Beltway paper.
     */
    private int _framePercentageOfUsableMemory;

    private Pointer _markPointer;

    private static final FieldActor _markActor = ClassActor.fromJava(Belt.class).findFieldActor(SymbolTable.makeSymbol("_mark"));

    // The address and the pointer to the address
    // of the previous allocation mark
    private volatile Address _prevAllocationMark = Address.zero();

    private Address _usableMemoryEnd;
    private Address _usableMemoryStart;

    private Size _usableMemory;

    public Belt(int index, Address beltStartAddress, Size beltSize, int framePercentageOfUsableMemory) {
        super(beltStartAddress, beltSize);
        _index = index;
        _usableMemoryStart = beltStartAddress;
        _framePercentageOfUsableMemory = framePercentageOfUsableMemory;
        _mark = beltStartAddress;
        _markPointer = _markActor.pointer(this);

    }

    public Belt() {
    }

    public final Address getPrevAllocationMark() {
        return _prevAllocationMark;
    }

    public final Size getUsedMemorySize() {
        return getAllocationMark().minus(start()).asSize();
    }

    public final Size getRemainingMemorySize() {
        return end().asSize().minus(getAllocationMark());
    }

    public final void resetAllocationMark() {
        _mark = start();
        _markPointer = _markActor.pointer(this);
    }

    @Override
    public void setStart(Address beltStartAddress) {
        super.setStart(beltStartAddress);
        _mark = beltStartAddress;
    }

    public void setIndex(int index) {
        _index = index;
    }

    public int getIndex() {
        return _index;
    }

    public int getFramePercentageOfUsableMemory() {
        return _framePercentageOfUsableMemory;
    }

    public void setFramePercentageOfUsableMemory(int framePercentageOfUsableMemory) {
        _framePercentageOfUsableMemory = framePercentageOfUsableMemory;
    }

    public void setExpandable(boolean expandable) {
        _expandable = expandable;
    }

    @Override
    public Pointer allocate(RuntimeMemoryRegion from, Size size) {
        return null;
    }

    @NO_SAFEPOINTS("TODO")
    @Override
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

        if (!checkObjectInBelt(size)) {
            if (!_expandable) {
                return Pointer.zero();
            }
        }
        if (_markPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark) {
            if (Heap.verbose()) {
                Log.println("Conflict! retry-allocate");
            }
            return retryAllocate(size);
        }
        return cell;
    }

    @NO_SAFEPOINTS("TODO")
    @INLINE
    public final Pointer bumpAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _mark.asPointer();

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
        _mark = end;
        return cell;
    }

    @NEVER_INLINE
    private Pointer retryAllocate(Size size) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = _mark.asPointer();
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = oldAllocationMark.plusWords(1);
            } else {
                cell = oldAllocationMark;
            }
            end = cell.plus(size);
            if (checkNotExceedUsable(end.asSize())) {
                return Pointer.zero();
            }
            oldAllocationMark = _mark.asPointer();
        } while (_markPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark);
        return cell;
    }

    @INLINE
    public final Pointer gcAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _mark.asPointer();
        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }

        final Pointer end = cell.plus(size);

        if (!end.lessThan(end())) {
            if (!_expandable) {
                if (Heap.verbose()) {
                    Log.println(" Trying to Overwrite contiguous spaces");
                }
                // Allocation Overflow, throw outOfMemory exception
                return Pointer.zero();
            }
            if (!end.lessThan(BeltManager.getApplicationHeap().end())) {
                return Pointer.zero();
            }
            if (!(_markPointer.compareAndSwapWord(oldAllocationMark, end) == oldAllocationMark)) {
                if (Heap.verbose()) {
                    Log.println("Conflict - Retry allocate");
                }
                return retryGCAllocate(size);
            }

        } else {
            if (!(_markPointer.compareAndSwapWord(oldAllocationMark, end) == oldAllocationMark)) {
                if (Heap.verbose()) {
                    Log.println("Conflict - Retry allocate");
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
            oldAllocationMark = _mark.asPointer();
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = oldAllocationMark.plusWords(1);
            } else {
                cell = oldAllocationMark;
            }
            end = cell.plus(size);
            oldAllocationMark = _mark.asPointer();
        } while (_markPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark);

        return cell;
    }

    @INLINE
    public final Pointer gcBumpAllocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _mark.asPointer();

        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }

        final Pointer end = cell.plus(size);

        if (!end.lessThan(end())) {
            if (!_expandable) {
                FatalError.check(_mark.lessThan(end()), "GC allocation overflow");
            }
            if (Heap.verbose()) {
                Log.println(" Trying to Overwrite contiguous spaces");
            }
            _mark = end;

        } else {
            _mark = end;
        }
        return cell;
    }

    @INLINE
    private boolean checkObjectInBelt(Size size) {
        final Pointer oldAllocationMark = _mark.asPointer();
        if (oldAllocationMark.plus(size).greaterThan(end().asPointer())) {
            if (Heap.verbose()) {
                Log.println("Allocation did not fit, check whether a new frame can be fit in the belt");
                Log.println(" Check whether a new frame can be fit into the belt ");
                Log.print(" Allocation Size:");
                Log.println(size);
                Log.print(" Allocation Mark: ");
                Log.println(oldAllocationMark);
            }
            return false;
        }
        return true;

    }

    public void setAllocationMarkSnapshot() {
        _prevAllocationMark = _mark;
    }

    public void setAllocationMark(Address address) {
        _mark = address;
    }

    public Pointer getAllocationMarkSnapshot() {
        return _prevAllocationMark.asPointer();
    }

    public final boolean checkNotExceedUsable(Size size) {
        final Size usedMemory = calculateUsedMemory();
        if (usedMemory.plus(size).greaterThan(BeltwayConfiguration.getUsableMemory())) {
            if (Heap.verbose()) {
                Log.println("Allocation is trying to exceed usable memory");
                Log.print(" Usable memory Size: ");
                Log.println(BeltwayConfiguration.getUsableMemory().toLong());
                Log.print(" Used Memory: ");
                Log.println(usedMemory.toLong());
                Log.print(" Allocation Size: ");
                Log.println(size.toLong());
                Log.print(" BeltwayConfiguration.getUsableMemory() Index: ");
                Log.println(BeltwayConfiguration.getUsableMemory().toLong());
            }
            return false;
        }
        return true;

    }

    public Size calculateUsedMemory() {
        Size usableMemory = Size.zero();
        if (_mark.lessThan(_usableMemoryStart)) {
            usableMemory = usableMemory.plus(_usableMemoryStart.plus(end())).asSize();
            usableMemory = usableMemory.plus(start().plus(_mark)).asSize();
        } else {
            usableMemory = _mark.minus(_usableMemoryStart).asSize();
        }
        return usableMemory;
    }

    public void printInfo() {
        Log.println();
        Log.print("Belt index: ");
        Log.println(_index);
        Log.print("Belt start address: ");
        Log.println(start());
        Log.print("Belt stop address:  ");
        Log.println(end());
        Log.print("Belt alloc address:  ");
        Log.println(getAllocationMark());
        Log.print("Belt size: ");
        Log.println(size().toLong());
        Log.print("Frame percentage of usable memory: ");
        Log.println(_framePercentageOfUsableMemory);
        if (start().isAligned(BeltwayHeapSchemeConfiguration.ALLIGNMENT)) {
            Log.println("true");
        }

    }

    @INLINE
    public final void visitCells(Visitor cellVisitor, Action action, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        BeltwayCellVisitorImpl.linearVisitAllCells((BeltWayCellVisitor) cellVisitor, action, this, from, to);
    }
}
