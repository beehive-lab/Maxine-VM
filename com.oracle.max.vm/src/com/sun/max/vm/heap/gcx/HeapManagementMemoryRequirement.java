/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * Interface implemented by components of a heap management that requires a fix amount of contiguous memory to cover
 * a contiguous range of virtual memory. This interface concerns components that have space requirements dependent on the
 * actual amount of contiguous space that is under control.
 * Example of components include tracing algorithm that use an external mark bitmap, card tables, etc.
 *
 */
public interface HeapManagementMemoryRequirement {
    /**
     * Return the space required to cover a contiguous range of virtual memory.
     * @param maxCoveredAreaSize the size, in bytes, of the covered contiguous range of virtual memory.
     * @return
     */

    Size memoryRequirement(Size maxCoveredAreaSize);
    /**
     * Contiguous region of memory allocated to the component.
     * @return a non-null {@link MemoryRegion}
     */
    MemoryRegion memory();
}
