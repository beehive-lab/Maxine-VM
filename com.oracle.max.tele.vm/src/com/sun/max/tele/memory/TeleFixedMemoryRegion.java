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
package com.sun.max.tele.memory;

import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM where the description is immutable.
 */
public class TeleFixedMemoryRegion extends VmMemoryRegion {

    private final String regionName;
    private final Address start;
    private final long nBytes;

    public TeleFixedMemoryRegion(MaxVM vm, String regionName, Address start, long nBytes) {
        super(vm);
        TeleError.check(start.isNotZero() || nBytes == 0, "Non-empty memory regions may not start at address 0");
        this.start = start;
        this.nBytes = nBytes;
        this.regionName = regionName;
    }

    public final String regionName() {
        return regionName;
    }

    public final Address start() {
        return start;
    }

    public final long nBytes() {
        return nBytes;
    }

    @Override
    public boolean isAllocated() {
        // Don't need to check for 0 start, per checked invariant
        return nBytes > 0;
    }

    public Address mark() {
        return null;
    }

}
