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
 *
 * Class that implements a table that maps fixed size regions from a contiguous range of virtual memory to fixed element in a table.
 * Regions have a size that is a power of two and are aligned on that power of 2.
 * This class provides basic services to such table.
 */
public class Power2MapTable {
    final Address coveredAreaStart;
    final Address coveredAreaEnd;
    final int log2RegionSize;

    Power2MapTable(int log2RegionSize, Address coveredAreaStart, Size coveredAreaSize) {
        this.log2RegionSize = log2RegionSize;
        this.coveredAreaStart = coveredAreaStart;
        this.coveredAreaEnd = coveredAreaStart.plus(coveredAreaSize);
    }
}
