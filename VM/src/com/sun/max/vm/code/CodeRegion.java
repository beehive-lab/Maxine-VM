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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.type.*;

/**
 * A code region that encapsulates a contiguous, fixed-sized memory area in the {@link TeleVM}
 * for storing code and data structures relating to code.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CodeRegion extends LinearAllocatorHeapRegion {

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
    @HOSTED_ONLY
    public CodeRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    /**
     * Binds this code region to some allocated memory range.
     *
     * @param start the start address of the range
     * @param size the size of the memory range
     */
    public void bind(Address start, Size size) {
        this.start = start;
        this.size = size;
        this.mark.set(start);
    }

    /**
     * A sorted list of the memory regions allocated within this code region.
     */
    private final SortedMemoryRegionList<RuntimeMemoryRegion> sortedMemoryRegions = new SortedMemoryRegionList<RuntimeMemoryRegion>();

    /**
     * The byte array hub, which is necessary for allocating a byte array instance to encapsulate
     * raw memory allocations.
     */
    private static final DynamicHub byteArrayHub = ClassRegistry.BYTE_ARRAY.dynamicHub();

    /**
     * Accessor for the sorted list of target methods.
     *
     * @return the sorted list of target methods in this code region
     */
    @HOSTED_ONLY
    public Iterable<TargetMethod> targetMethods() {
        final AppendableSequence<TargetMethod> result = new ArrayListSequence<TargetMethod>(sortedMemoryRegions.length());
        for (MemoryRegion memoryRegion : sortedMemoryRegions) {
            if (memoryRegion instanceof TargetMethod) {
                result.append((TargetMethod) memoryRegion);
            }
        }
        return result;
    }

    public void addToSortedMemoryRegions(RuntimeMemoryRegion region) {
        sortedMemoryRegions.add(region);
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

}
