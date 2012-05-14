/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.sequential.gen.semiSpace;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.runtime.*;


public class ContiguousSemiSpace <T extends BaseAtomicBumpPointerAllocator<? extends Refiller>> extends ContiguousAllocatingSpace<T> {
    @INSPECTED
    ContiguousHeapSpace fromSpace;
    ContiguousSemiSpace(T allocator, String name) {
        super(allocator, name + "'s To Space");
        fromSpace = new ContiguousHeapSpace(name + "'s From Space");
    }


    public Address highestAddress() {
        Address fend = fromSpace.end();
        Address tend = space.end();
        return fend.greaterThan(tend) ? fend : tend;
    }

    public Address lowestAddress() {
        Address fstart = fromSpace.start();
        Address tstart = space.start();
        return fstart.greaterThan(tstart) ? fstart : tstart;
    }

    @Override
    public void initialize(Address start, Size semiSpaceMaxSize, Size semiSpaceInitSize) {
        super.initialize(start, semiSpaceMaxSize, semiSpaceInitSize);
        fromSpace.setReserved(start.plus(semiSpaceMaxSize), semiSpaceMaxSize);
        fromSpace.growCommittedSpace(semiSpaceInitSize);
        // Inspector support:
        // Zero-fill  the first word of  the still virgin backing storage of the from space to force the OS to map the first page in virtual memory.
        // This avoids the inspector to get DataIO Error on trying to read the bytes from the first page.
        fromSpace.start().asPointer().setWord(Word.zero());
    }

    void flipSpaces() {
        String fromSpaceName = fromSpace.regionName();
        String toSpaceName = space.regionName();
        // Make allocator parsable before flipping semi-space.
        allocator.doBeforeGC();
        ContiguousHeapSpace toSpace = fromSpace;
        fromSpace = space;
        fromSpace.setRegionName(fromSpaceName);
        space = toSpace;
        space.setRegionName(toSpaceName);
        // Now, refill the allocator with the new to Space.
        allocator.refill(space.start(), space.committedSize());
    }

    @Override
    public Size growAfterGC(Size delta) {
        Size size = super.growAfterGC(delta);
        boolean shrunk = fromSpace.growCommittedSpace(size);
        FatalError.check(shrunk, "request for growing space after GC must always succeed");
        return size;
    }

    @Override
    public Size shrinkAfterGC(Size delta) {
        Size size = super.shrinkAfterGC(delta);
        boolean shrunk = fromSpace.shrinkCommittedSpace(size);
        FatalError.check(shrunk, "request for shrinking space after GC must always succeed");
        return size;
    }
    @Override
    public void doBeforeGC() {
    }

    @Override
    public void doAfterGC() {
    }

}
