/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * A code manager that allocates virtual memory somewhere in the address space.
 * Since we cannot guarantee that an allocated address of virtual memory is at an address
 * that is a multiple of {@link CodeManager#runtimeCodeRegions} (c.f. {@link FixedAddressCodeManager})
 * we keep the {@link CodeManager#runtimeCodeRegions} array sorted by increasing address.
 *
 * In general we cannot easily guarantee the invariant that the regions managed by this manager
 * are within 32 bits of each other. We assume that {@link VirtualMemory#allocate(Size, com.sun.max.memory.VirtualMemory.Type)}
 * preserves the constraint when asked to allocate {@linkplain VirtualMemory.Type#CODE code}.
 */
public class VariableAddressCodeManager extends CodeManager {

    /**
     * Initialize this code manager.
     */
    @Override
    void initialize() {
        final Size size = runtimeCodeRegionSize.getValue();
        final Address address = allocateCodeRegionMemory(size);
        if (address.isZero() || address.isAllOnes()) {
            throw ProgramError.unexpected("could not allocate runtime code region");
        }
        runtimeCodeRegion.bind(address, size);
    }

    protected Address allocateCodeRegionMemory(Size size) {
        return VirtualMemory.allocate(size, VirtualMemory.Type.CODE);
    }

}
