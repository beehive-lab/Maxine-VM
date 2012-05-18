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

import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * A code manager that reserves and allocates virtual memory immediately after the boot region.
 * Specifically, the code manager allocates two page-aligned contiguous ranges of virtual memory immediately (one for each of the baseline and optimized code regions) after the
 * first virtual memory page next to the boot heap region highest address.
 * It relies on cooperation with the HeapScheme to reserve up to 1 G of space next to the boot heap region.
 * This guarantees that (1) virtual memory can be allocated at that address, and (2) all code allocated from the code manager will be within a 32-bit displacement from
 * any code in the boot code region.
 *
 * See {@link HeapSchemeAdaptor#createCodeManager()}
 */
public class NearBootRegionCodeManager extends CodeManager {
    /**
     * Initialize this code manager. This comprises allocating virtual memory the code manager will allocate code from.
     */
    @Override
    void initialize() {
        final Address baselineAddress = Code.bootCodeRegion().end().alignUp(Platform.platform().pageSize);
        tryAllocate(runtimeBaselineCodeRegionSize, runtimeBaselineCodeRegion, baselineAddress);
        final Address optAddress = runtimeBaselineCodeRegion.end().alignUp(Platform.platform().pageSize);
        tryAllocate(runtimeOptCodeRegionSize, runtimeOptCodeRegion, optAddress);
    }

    private void tryAllocate(VMSizeOption s, CodeRegion cr, Address address) {
        final Size size = s.getValue();
        if (!Heap.AvoidsAnonOperations && !VirtualMemory.allocateAtFixedAddress(address, size, VirtualMemory.Type.CODE)) {
            throw ProgramError.unexpected("could not allocate " + cr.regionName());
        }
        cr.bind(address, size);
    }
}
