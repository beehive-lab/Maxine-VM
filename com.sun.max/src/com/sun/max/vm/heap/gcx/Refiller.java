/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;

/**
 * Basic refiller for bump pointer allocators.
 */
public abstract class Refiller {

    /**
     * Dispose of the contiguous space left in the allocator and return a new chunk of memory to refill it, or refill directly.
     * If the refill is done directly, a zero address is returned, otherwise, an address to a cell formated as a {@linkplain HeapFreeChunk} is
     * returned.
     * @param requestedSize Requested amount of bytes that caused the refill
     * @param startOfSpaceLeft address of the first byte of the space left at the end of the linear space allocator being asking for refill.
     * @param spaceLeft size, in bytes, of the space left
     *
     * @return a zero Address if the managed allocator was refilled, the address to a {@linkplain HeapFreeChunk} otherwise.
     */
    public abstract Address allocateRefill(Size requestedSize, Pointer startOfSpaceLeft, Size spaceLeft);

    /**
     * Make a non-empty region of the allocator indicated by the start and end pointers iterable
     * by an object iterator.
     * @param start start of the region
     * @param end end of the region, must be greater than start
     */
    void makeParsable(Pointer start, Pointer end) {
        DarkMatter.format(start, end);
    }

    /**
     * Prepare for GC.
     */
    protected abstract void doBeforeGC();

    /**
     * Called directly by the allocator if the requested size is larger than its maximum size limit.
     * The allocated chunk is left raw. Clearing (i.e., zero-fill) is the responsibility of the caller.
     *
     * @param size number of bytes requested
     * @return the address to a contiguous region of the requested size
     */
    public abstract Address allocateLargeRaw(Size size);

}
