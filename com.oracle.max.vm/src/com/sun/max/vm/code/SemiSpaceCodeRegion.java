/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.runtime.*;

/**
 * A code region with semi-space characteristics.
 */
public final class SemiSpaceCodeRegion extends CodeRegion {

    public SemiSpaceCodeRegion(String description) {
        super(description);
        fromTargetMethods = new TargetMethod[DEFAULT_CAPACITY];
        fromLength = 0;
        fromFindIndex = new int[DEFAULT_CAPACITY];
    }

    @INSPECTED
    protected Address toSpace;

    @INSPECTED
    protected Address fromSpace;

    @INSPECTED
    protected Size spaceSize;

    @INSPECTED
    protected Address topOfSpace;

    /**
     * Stores the target methods in from-space.
     * The {@link #targetMethods} array stores target methods in to-space.
     */
    @INSPECTED
    protected TargetMethod[] fromTargetMethods;

    /**
     * A {@link #findIndex} for from-space.
     */
    private int[] fromFindIndex;

    /**
     * Amount of entries in the from-space target methods array.
     * The {@link #length} field represents the amount of entries in to-space.
     */
    @INSPECTED
    protected int fromLength;

    /**
     * Binds this code region to some allocated memory range and sets the semi-space addresses.
     *
     * @param start the start address of the range
     * @param size the size of the memory range
     */
    @Override
    public void bind(Address start, Size size) {
        this.start = start;
        this.size = size;
        this.toSpace = start;
        this.spaceSize = size.dividedBy(2);
        this.fromSpace = toSpace.plus(spaceSize);
        this.topOfSpace = toSpace.plus(spaceSize);
        this.mark.set(toSpace);
    }

    /**
     * Gets the GC start.
     * For semi-space code regions, the GC start is to-space start.
     */
    @Override
    public Address gcstart() {
        return toSpace;
    }

    /**
     * Flip to-space and from-space, set topOfSpace accordingly.
     */
    public void flip() {
        Address tmpSpace = toSpace;
        toSpace = fromSpace;
        fromSpace = tmpSpace;
        topOfSpace = toSpace.plus(spaceSize);
        mark.set(toSpace);
        TargetMethod[] tmpMethods = targetMethods;
        targetMethods = fromTargetMethods;
        fromTargetMethods = tmpMethods;
        int tmpLength = length;
        length = fromLength;
        fromLength = tmpLength;
        int[] tmpIndex = findIndex;
        findIndex = fromFindIndex;
        fromFindIndex = tmpIndex;
    }

    /**
     * Allocates some memory from this region. See {@linkplain LinearAllocatorRegion} for details.
     */
    @Override
    public Pointer allocate(Size size, boolean adjustForDebugTag) {
        if (!size.isWordAligned()) {
            FatalError.unexpected("Allocation size must be word aligned");
        }

        Pointer oldAllocationMark = mark();
        Pointer cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
        Address end = cell.plus(size);
        if (end.greaterThan(topOfSpace)) {
            if (MaxineVM.isHosted()) {
                ProgramError.unexpected("out of space in linear allocator region");
            }
            return Pointer.zero();
        }
        setMark(end);
        return cell;
    }

    /**
     * Looks up the target method containing a particular address <i>in from-space</i>.
     */
    public TargetMethod findInFromSpace(Address cp) {
        if (!isInFromSpace(cp)) {
            return null;
        }
        return find0(cp, fromSpace, fromFindIndex, fromTargetMethods);
    }

    @Override
    protected boolean validMethodStart(TargetMethod tm, Address address) {
        return isInFromSpace(address) ? tm.oldStart().lessEqual(address) : super.validMethodStart(tm, address);
    }

    @Override
    protected boolean methodFound(TargetMethod tm, Address address) {
        if (isInFromSpace(address)) {
            final Address start = tm.oldStart();
            return start.lessEqual(address) && start.plus(tm.size()).greaterThan(address);
        } else {
            return super.methodFound(tm, address);
        }
    }

    /**
     * Controls whether {@code #find()} will also take from-space into account.
     * This is to be used with extreme care. It is intended to be used during code eviction only.
     */
    public boolean allowFromSpaceLookup = false;

    /**
     * Looks up the target method containing a particular address (using binary search).
     * This specialised version for a semi-space code region checks whether the passed address is in from-space.
     * If so, it looks for the address in from-space first, but only if the {@code allowFromSpaceLookup} field is set to {@code true}.
     * This must be the case only during code eviction.
     *
     * @param cp the address to lookup in this region
     * @return a reference to the target method containing the specified address, if it exists; {@code null} otherwise
     */
    @Override
    public TargetMethod find(Address cp) {
        final boolean addressInFromSpace = isInFromSpace(cp);
        if (allowFromSpaceLookup) {
            final TargetMethod fromSpaceLookup = findInFromSpace(cp);
            if (fromSpaceLookup != null) {
                return fromSpaceLookup;
            }
            return super.find(cp);
        } else {
            if (addressInFromSpace) {
                return null;
            } else {
                return super.find(cp);
            }
        }
    }

    public boolean isInFromSpace(Address a) {
        return fromSpace.lessEqual(a) && fromSpace.plus(spaceSize).greaterThan(a);
    }

    public boolean isInToSpace(Address a) {
        return toSpace.lessEqual(a) && topOfSpace.greaterThan(a);
    }

    /**
     * Adds a method to this code region.
     * This specialises the implementation in {@code CodeRegion} in that the {@link #fromTargetMethods} array
     * grows along with the {@link #targetMethods} array.
     */
    @Override
    public void add(TargetMethod tm) {
        final TargetMethod[] tms = targetMethods;
        final int[] index = findIndex;
        super.add(tm);
        if (tms != targetMethods) {
            fromTargetMethods = Arrays.copyOf(fromTargetMethods, targetMethods.length);
        }
        if (index != findIndex) {
            fromFindIndex = Arrays.copyOf(fromFindIndex, findIndex.length);
        }
    }

    /**
     * Reset the from-space target methods array.
     */
    public void resetFromSpace() {
        Arrays.fill(fromTargetMethods, null);
        fromLength = 0;
        Arrays.fill(fromFindIndex, 0);
    }

    /**
     * Process each target method in this region's to-space with a given closure.
     */
    public boolean doNewTargetMethods(TargetMethod.Closure c) {
        TargetMethod[] targetMethods = this.targetMethods;
        assert length <= targetMethods.length;
        for (int i = 0; i < length; i++) {
            TargetMethod targetMethod = targetMethods[i];
            if (targetMethod != null && isInToSpace(targetMethod.codeStart().toAddress()) && !c.doTargetMethod(targetMethod)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Process each target method in this region's from-space with a given closure.
     */
    public boolean doOldTargetMethods(TargetMethod.Closure c) {
        TargetMethod[] tms = this.fromTargetMethods;
        assert fromLength <= tms.length;
        for (int i = 0; i < fromLength; i++) {
            TargetMethod targetMethod = tms[i];
            if (targetMethod != null && isInFromSpace(targetMethod.codeStart().toAddress()) && !c.doTargetMethod(targetMethod)) {
                return false;
            }
        }
        return true;
    }

}
