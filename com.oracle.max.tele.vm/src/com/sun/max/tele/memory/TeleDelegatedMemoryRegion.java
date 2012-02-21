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
package com.sun.max.tele.memory;

import java.lang.management.*;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM where the description of the memory
 * is held by a object in the VM and might change.
 */
public abstract class TeleDelegatedMemoryRegion extends VmMemoryRegion {

    private final TeleRuntimeMemoryRegion teleRuntimeMemoryRegion;

    protected TeleDelegatedMemoryRegion(MaxVM vm, TeleRuntimeMemoryRegion teleRuntimeMemoryRegion) {
        super(vm);
        this.teleRuntimeMemoryRegion = teleRuntimeMemoryRegion;
    }

    public final String regionName() {
        return teleRuntimeMemoryRegion.getRegionName();
    }

    public final Address start() {
        return teleRuntimeMemoryRegion.getRegionStart();
    }

    public final long nBytes() {
        return teleRuntimeMemoryRegion.getRegionNBytes();
    }

    @Override
    public final MemoryUsage getUsage() {
        return teleRuntimeMemoryRegion.getUsage();
    }

    public Address mark() {
        return teleRuntimeMemoryRegion.mark();
    }

    @Override
    public boolean containsInAllocated(Address address) {
        return teleRuntimeMemoryRegion.containsInAllocated(address);
    }

    @Override
    public boolean isAllocated() {
        return teleRuntimeMemoryRegion.isAllocated();
    }

}
