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
package com.sun.max.vm.code;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A code region that encapsulates a contiguous, fixed-sized memory area in the {@link TeleVM}
 * for storing code and data structures relating to code.
 */
public class CodeRegion extends LinearAllocatorHeapRegion {

    public CodeRegion(String description) {
        super(description);
    }

    /**
     * Constructs a new code region that begins at the specified address and has the specified fixed size.
     *
     * @param start the starting memory address
     * @param size the size of the code region in bytes
     */
    public CodeRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    /**
     * A sorted list of the memory regions allocated within this code region.
     */
    private final SortedMemoryRegionList<RuntimeMemoryRegion> sortedMemoryRegions = new SortedMemoryRegionList<RuntimeMemoryRegion>();

    /**
     * The byte array hub, which is necessary for allocating a byte array instance to encapsulate
     * raw memory allocations.
     */
    private static final DynamicHub byteArrayHub = ClassActor.fromJava(byte[].class).dynamicHub();

    /**
     * Accessor for the sorted list of target methods.
     *
     * @return the sorted list of target methods in this code region
     */
    @PROTOTYPE_ONLY
    public final Iterable<TargetMethod> targetMethods() {
        final AppendableSequence<TargetMethod> result = new ArrayListSequence<TargetMethod>(sortedMemoryRegions.length());
        for (MemoryRegion memoryRegion : sortedMemoryRegions) {
            if (memoryRegion instanceof TargetMethod) {
                result.append((TargetMethod) memoryRegion);
            }
        }
        return result;
    }

    /**
     * Allocate space for a target method's code and data structures in this code region.
     *
     * @param targetMethod the target method to allocate in this region
     * @param size the size of the block to allocate
     * @return the allocated space or zero if allocation was not successful
     */
    public Pointer allocateTargetMethod(TargetMethod targetMethod, Size size) {
        final Pointer start = allocateSpace(size);
        if (!start.isZero()) {
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.printVmThread(VmThread.current(), false);
                Log.print(": Allocated target code bundle for ");
                Log.printMethodActor(targetMethod.classMethodActor(), false);
                Log.print(" at ");
                Log.print(start);
                Log.print(" [size=");
                Log.print(size.toInt());
                Log.print(", end=");
                Log.print(start.plus(size));
                Log.println("]");
                Log.unlock(lockDisabledSafepoints);
            }
            targetMethod.setStart(start);
            targetMethod.setSize(size);
            sortedMemoryRegions.add(targetMethod);
        }
        return start;
    }

    /**
     * Allocates a raw piece of memory in this code region for storing a runtime stub. This
     * method will allocate a byte array internally to preserve the invariant that all cells in a code region contain
     * objects. This method returns a pointer to the first byte of the internally allocated array.
     *
     * @param stub an object describing the size of the runtime stub code (i.e. the size in bytes to allocate). If
     *            allocation is successful, the address of the memory chunk allocated (i.e. the address of the first
     *            element of the internally allocated byte array) will be accessible through the
     *            {@link MemoryRegion#start()} method of this object.
     * @return true if space was successfully allocated for the safepoint stub
     */
    public boolean allocateRuntimeStub(RuntimeStub stub) {
        final ByteArrayLayout byteArrayLayout = Layout.byteArrayLayout();
        final int size = stub.size().toInt();
        final Size allocationSize = byteArrayLayout.getArraySize(size);
        final Pointer cell = allocateCell(allocationSize);
        if (cell.isZero()) {
            // allocation failed.
            return false;
        }
        if (Heap.traceGC()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Allocated runtime stub named \"");
            Log.print(stub.name());
            Log.print("\" at ");
            Log.print(cell);
            Log.print(" [size=");
            Log.print(allocationSize.toInt());
            Log.print(", end=");
            Log.print(cell.plus(allocationSize));
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
        DebugHeap.writeCellTag(cell);
        Cell.plantArray(cell, byteArrayHub, size);
        stub.setStart(cell.plus(byteArrayLayout.getElementOffsetInCell(0)));
        sortedMemoryRegions.add(stub);
        return true;
    }

    /**
     * Looks up the target method containing a particular address (typically using binary search).
     *
     * @param address the address to lookup in this region
     * @return a reference to the target method containing the specified address, if it exists; {@code null} otherwise
     */
    public TargetMethod findTargetMethod(Address address) {
        final MemoryRegion memoryRegion = sortedMemoryRegions.find(address);
        if (memoryRegion instanceof TargetMethod) {
            return (TargetMethod) memoryRegion;
        }
        return null;
    }

    /**
     * Looks up the runtime stub containing a particular address (typically using binary search).
     *
     * @param address the address to lookup in this region
     * @return a reference to the runtime stub containing the specified address, if it exists; {@code null}
     *         otherwise
     */
    public RuntimeStub findRuntimeStub(Address address) {
        final MemoryRegion memoryRegion = sortedMemoryRegions.find(address);
        if (memoryRegion instanceof RuntimeStub) {
            return (RuntimeStub) memoryRegion;
        }
        return null;
    }
}
