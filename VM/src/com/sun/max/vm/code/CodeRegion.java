/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.code;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;

/**
 * A code region that encapsulates a contiguous, fixed-sized memory area in the VM
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
     * A sorted list of the target methods allocated within this code region.
     */
    @INSPECTED
    private final SortedMemoryRegionList<TargetMethod> targetMethods = new SortedMemoryRegionList<TargetMethod>();

    /**
     * Accessor for the sorted list of target methods.
     *
     * @return the sorted list of target methods in this code region
     */
    @HOSTED_ONLY
    public Iterable<TargetMethod> targetMethods() {
        return targetMethods;
    }

    /**
     * Accessor for a copy of the sorted list of target methods as an array.
     * @return a copy of all existing target methods at the time of invocation
     */
    public TargetMethod[] currentTargetMethods() {
        TargetMethod[] result = new TargetMethod[targetMethods.size()];
        int index = 0;
        for (TargetMethod tm : targetMethods) {
            result[index++] = tm;
        }
        return result;
    }

    public void add(TargetMethod targetMethod) {
        targetMethods.add(targetMethod);
    }

    /**
     * Looks up the target method containing a particular address (typically using binary search).
     *
     * @param address the address to lookup in this region
     * @return a reference to the target method containing the specified address, if it exists; {@code null} otherwise
     */
    public TargetMethod find(Address address) {
        return targetMethods.find(address);
    }

}
