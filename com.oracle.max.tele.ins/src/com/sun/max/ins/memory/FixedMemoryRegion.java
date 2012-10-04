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
package com.sun.max.ins.memory;

import com.sun.max.ins.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM.
 */
public class FixedMemoryRegion extends AbstractMemoryRegion {

    private String regionName;

    /**
     * Default start location, as of creation time.
     */
    private final Address start;

    /**
     * Default size, as of creation time.
     */
    private final long nBytes;

    public FixedMemoryRegion(Inspection inspection, String regionName, Address start, long nBytes) {
        super(inspection);
        TeleError.check(nBytes == 0 || start.isNotZero(), "Non-empty memory regions may not start address 0");
        this.start = start;
        this.nBytes = nBytes;
        this.regionName = regionName;
    }

    public FixedMemoryRegion(Inspection inspection, String regionName, Address start, Address end) {
        super(inspection);
        TeleError.check(end.isZero() || start.isNotZero(), "Non-empty memory regions may not start address 0");
        this.start = start;
        this.nBytes = end.minus(start).toLong();
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

}
