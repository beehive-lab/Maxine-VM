/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Interface that heap resizing policies expect.
 */
public interface ResizableSpace {
    /**
     * Try to grow the resizable space by delta bytes.
     * The method rounds the delta up to any alignment constraints the resizable space may have to enforce.
     * If the requested delta is larger than the reserve of space the resizable space can grow from,  the heap is simply grown to its capacity.
     * @param delta the number of bytes to grow the resizable with
     * @return the effective growth of the space, zero if failed to increase the space.
     */
    Size increaseSize(Size delta);

    /**
     * Try to shrink the resizable space by delta bytes.
      * The method rounds the delta up to any alignment constraints the resizable space may have to enforce.
    * @param delta the number of bytes to shrink the resizable with
     * @return the effective size the space shrunk, zero if failed to decrease the space.
     */
    Size decreaseSize(Size delta);
    /**
     * Amount of memory used by the space. This includes space allocated to live data, dark matter, and space available for allocation.
     * The total space must be less or equal to the capacity of the resizable space.
     * @return size in bytes
     */
    Size totalSpace();
    /**
     * Capacity of the resizable space. This is the upper bound of space the resizable space can grow up to.
     * @return size in bytes
     */
    Size capacity();
}
