/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.heap.*;

/**
 * Heap Space interface. A heap may be made of one or more heap spaces.
 * Space may be made of one or multiple contiguous ranges of virtual memory.
 * This interface is an attempt to front various space implementation with a common interface to
 * ease composition of heap management components.
 * WORK IN PROGRESS.
 */
public interface HeapSpace extends ResizableSpace, EvacuatingSpace {
    /**
     * Allocate a cell of exactly the specified size.
     * @param size size in bytes
     * @return a pointer to raw heap space of exactly the requested size
     */
    Pointer allocate(Size size);

    /**
     * Allocate a cell for a TLAB refill.
     * @param size
     * @return a pointer to a cell formatted as a {@link HeapFreeChunk}
     */
    Pointer allocateTLAB(Size size);

    /**
     * Retire space from a previously allocated TLAB.
     * @param start pointer to the first word of free space in the retired TLAB
     * @param size  free space left over in the retired TLAB.
     */
    void retireTLAB(Pointer start, Size size);

    /**
     * Amount of space available for allocation.
     * @return a size in bytes
     */
    Size freeSpace();
    /**
     * Amount of space occupied by allocated cells.
     * @return  a size in bytes
     */
    Size usedSpace();

    /**
     * Visit all the ranges of contiguous virtual memory of the heap space that may comprise allocated objects.
     * The ranges must be iterable, i.e., formatted as a sequence of cells whose size can be queried by visitors and such that
     * the address of the cell plus its size gives the address of the next cell.
     * Allocation to the heap space is prohibited during visiting.
     *
     * @param visitor a visitor that can iterate over iterable ranges of contiguous heap space.
     */
    void visit(CellRangeVisitor visitor);
}
