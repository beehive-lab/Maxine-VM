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
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A code region that encapsulates a contiguous, fixed-sized memory area in the {@link TeleVM}
 * for storing code and data structures relating to code.
 *
 * A code region includes a {@linkplain #setReferenceMapBits(Address, int) reference map} describing
 * where all the references are in the region. The reference map itself exists in the code region
 * and is created when the code region is {@linkplain #bind(Address, Size) bound} to some real memory.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CodeRegion extends LinearAllocatorHeapRegion {

    /**
     * The reference map allocated *in* this code region describing where the
     * references are in the rest of the region.
     *
     * Invariant: the reference map does not have any bits set for header words. That is, all objects in
     * a code region are assumed to be of a type known at boot image time (i.e. the hub word in the
     * header is immutable) and the configured {@link MonitorScheme} explicitly manages the binding of objects to
     * heap allocated monitors via the "misc" word in an object's header.
     */
    private Pointer referenceMap;

    /**
     * The index one past the highest set bit in the {@linkplain #referenceMap reference map}.
     */
    private int referenceMapEndBitIndex;

    /**
     * The address in this code region corresponding to the first bit in the {@linkplain #referenceMap reference map}.
     */
    private Address referenceMapDomainStart;

    /**
     * Creates a code region that is not yet bound to any memory.
     *
     * @param description a description of this code region. This value may be used by a debugger.
     */
    public CodeRegion(String description) {
        super(description);
    }

    /**
     * Constructs a new code region that begins at the specified address and has the specified fixed size.
     *
     * This constructor is only used for creating the {@linkplain Code#bootCodeRegion boot} code region.
     *
     * @param start the starting memory address
     * @param size the size of the code region in bytes
     */
    @PROTOTYPE_ONLY
    public CodeRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    /**
     * Binds this code region to some allocated memory range. This method also initializes the
     * {@linkplain #setReferenceMapBits(Address, int) reference map} at the start of the code
     * region.
     *
     * @param start the start address of the range
     * @param size the size of the memory range
     */
    public void bind(Address start, Size size) {
        this.start = start;
        this.size = size;
        this.mark.set(start);

        final int referenceMapBitCount = size.plus(Word.size() - 1).dividedBy(Word.size()).toInt();
        Size referenceMapSize = Size.fromInt(ByteArrayBitMap.computeBitMapSize(referenceMapBitCount)).wordAligned();
        referenceMap = allocateSpace(Code.traceAllocation.getValue() ? "code region refmap" : null, referenceMapSize);
        FatalError.check(referenceMap.equals(start), "Code region reference map must be at start of region");
        referenceMapDomainStart = referenceMap.plus(referenceMapSize);
    }

    /**
     * Sets the bits in this code region's reference map corresponding to a range of
     * address in this region which contain references.
     *
     * @param start the address of a reference in this code region
     * @param count the number of references in the sequence of references starting at {@code start}
     */
    public void setReferenceMapBits(Address start, int count) {
        if (referenceMap.isZero()) {
            FatalError.unexpected("Cannot write to uninitialized reference map");
        }
        FatalError.check(start.isWordAligned(), "Unaligned reference address");
        final int startBitIndex = start.minus(this.start).dividedBy(Word.size()).toInt();
        final Address referenceMapDomainEnd = end();
        final Address end = start.plus(count * Word.size());
        if (start.lessThan(referenceMapDomainStart) || end.greaterThan(referenceMapDomainEnd)) {
            Log.print("Reference address range ");
            Log.print(start);
            Log.print(" .. ");
            Log.print(end);
            Log.print(" is not completely within domain of reference map [");
            Log.print(referenceMapDomainStart);
            Log.print(" .. ");
            Log.println(referenceMapDomainEnd);
            FatalError.unexpected("Reference address range is not completely within domain of reference map");
        }
        final int endBitIndex = startBitIndex + count;
        for (int bitIndex = startBitIndex; bitIndex < endBitIndex; ++bitIndex) {
            referenceMap.setBit(bitIndex);
        }
        if (endBitIndex > referenceMapEndBitIndex) {
            referenceMapEndBitIndex = endBitIndex;
        }
    }

    @Override
    public void visitReferences(PointerIndexVisitor pointerIndexVisitor) {

        if (referenceMap.isZero()) {
            if (!start.isZero()) {
                FatalError.unexpected("Cannot have null reference map in code region that is bound to memory");
            }
            return;
        }

        final Pointer refMap = referenceMap;
        final int referenceMapWords = Unsigned.idiv(referenceMapEndBitIndex + (Word.width() - 1), Word.width());
        if (Heap.traceRootScanning()) {
            Log.print("Scanning references in code region ");
            Log.print(description());
            Log.print(": start=");
            Log.print(start());
            Log.print(", end=");
            Log.print(end());
            Log.print(", references end=");
            Log.println(start().plus(referenceMapWords * Word.size()));
        }
        if (Heap.traceRootScanning()) {
            scanReferenceMap(pointerIndexVisitor, refMap, referenceMapWords, true);
        } else {
            scanReferenceMap(pointerIndexVisitor, refMap, referenceMapWords, false);
        }
    }

    /**
     * A sorted list of the memory regions allocated within this code region.
     */
    private final SortedMemoryRegionList<RuntimeMemoryRegion> sortedMemoryRegions = new SortedMemoryRegionList<RuntimeMemoryRegion>();

    /**
     * The byte array hub, which is necessary for allocating a byte array instance to encapsulate
     * raw memory allocations.
     */
    private static final DynamicHub byteArrayHub = PrimitiveClassActor.BYTE_ARRAY_CLASS_ACTOR.dynamicHub();

    /**
     * Accessor for the sorted list of target methods.
     *
     * @return the sorted list of target methods in this code region
     */
    @PROTOTYPE_ONLY
    public Iterable<TargetMethod> targetMethods() {
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
        final Pointer start = allocateSpace(null, size);
        if (!start.isZero()) {
            if (Code.traceAllocation.getValue()) {
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
     * Allocates a raw piece of memory in this code region for storing a runtime stub.
     *
     * @param stub an object describing the size of the runtime stub code (i.e. the size in bytes to allocate). If
     *            allocation is successful, the address of the memory chunk allocated will be accessible through the
     *            {@link MemoryRegion#start()} method of this object.
     * @return true if space was successfully allocated for the stub
     */
    public boolean allocateRuntimeStub(RuntimeStub stub) {
        final Pointer cell = allocateSpace(Code.traceAllocation.getValue() ? stub.name() : null, stub.size());
        if (cell.isZero()) {
            return false;
        }
        stub.setStart(cell);
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
