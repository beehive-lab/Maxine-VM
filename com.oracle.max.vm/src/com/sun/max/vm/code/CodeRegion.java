/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;

/**
 * A code region that encapsulates a contiguous, fixed-sized memory area in the VM
 * for storing code and data structures relating to code.
 */
public final class CodeRegion extends LinearAllocatorHeapRegion {

    public static final int DEFAULT_CAPACITY = 10;

    /**
     * Creates a code region that is not yet bound to any memory.
     *
     * @param description a description of this code region. This value may be used by a debugger.
     */
    public CodeRegion(String description) {
        super(description);
        targetMethods = new TargetMethod[DEFAULT_CAPACITY];
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
        targetMethods = new TargetMethod[DEFAULT_CAPACITY];
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
    private TargetMethod[] targetMethods;

    /**
     * Length of valid data in {@link #targetMethods}.
     */
    @INSPECTED
    private int length;

    /**
     * Gets a copy of the sorted target method list.
     */
    public TargetMethod[] copyOfTargetMethods() {
        int length = this.length;
        TargetMethod[] result = new TargetMethod[length];
        System.arraycopy(targetMethods, 0, result, 0, length);
        return result;
    }

    /**
     * Adds a target method to this sorted list of target methods.
     */
    public void add(TargetMethod targetMethod) {
        int index = Arrays.binarySearch(targetMethods, 0, length, targetMethod, COMPARATOR);
        if (index < 0) {
            int insertionPoint = -(index + 1);
            if (length == targetMethods.length) {
                int newCapacity = (targetMethods.length * 3) / 2 + 1;
                targetMethods = Arrays.copyOf(targetMethods, newCapacity);
            }
            System.arraycopy(targetMethods, insertionPoint, targetMethods, insertionPoint + 1, length - insertionPoint);
            targetMethods[insertionPoint] = targetMethod;
            length++;
        }
    }

    /**
     * Looks up the target method containing a particular address (using binary search).
     *
     * @param address the address to lookup in this region
     * @return a reference to the target method containing the specified address, if it exists; {@code null} otherwise
     */
    public TargetMethod find(Address address) {
        int left = 0;
        int right = length;
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            final TargetMethod method = targetMethods[middle];
            if (method.start().greaterThan(address)) {
                right = middle;
            } else if (method.start().plus(method.size()).greaterThan(address)) {
                return method;
            } else {
                left = middle + 1;
            }
        }
        return null;
    }

    /**
     * Process each target method in this region with a given closure. This iteration operates on a snapshot of the
     * methods in this region.
     *
     * @return {@code false} if {@code c} returned {@code false} when processing a target method (and thus aborted
     *         processing of any subsequent methods in the snapshot)
     */
    public boolean doAllTargetMethods(TargetMethod.Closure c) {
        TargetMethod[] targetMethods = this.targetMethods;
        for (int i = 0; i < length && i < targetMethods.length; i++) {
            TargetMethod targetMethod = targetMethods[i];
            if (targetMethod != null && !c.doTargetMethod(targetMethod)) {
                return false;
            }
        }
        return true;
    }

    public static final Comparator<TargetMethod> COMPARATOR = new Comparator<TargetMethod>() {
        @Override
        public int compare(TargetMethod o1, TargetMethod o2) {
            Address o1Start = o1.start();
            Address o2Start = o2.start();
            if (o1Start.lessThan(o2Start)) {
                assert o1.end().lessEqual(o2Start) : "intersecting regions";
                return -1;
            }
            if (o1Start.equals(o2Start)) {
                assert o1.end().equals(o2.end()) : "intersecting regions";
                return 0;
            }
            assert o2.end().lessEqual(o1Start) : "intersecting regions";
            return 1;
        }
    };
}
