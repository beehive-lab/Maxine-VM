/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM where the description is immutable.
 *
 * @author Michael Van De Vanter
 */
public class TeleFixedMemoryRegion extends TeleMemoryRegion {

    private final String regionName;
    private final Address start;
    private final Size size;

    public TeleFixedMemoryRegion(TeleVM teleVM, String regionName, Address start, Size size) {
        super(teleVM);
        this.start = start;
        this.size = size;
        this.regionName = regionName;
    }

    public TeleFixedMemoryRegion(TeleVM teleVM, String regionName, TeleMemoryRegion memoryRegion) {
        this(teleVM, regionName, memoryRegion.start(), memoryRegion.size());
    }

    public final String regionName() {
        return regionName;
    }

    // TODO (mlvdv)  make final when all sorted out concerning code regions
    public Address start() {
        return start;
    }

    // TODO (mlvdv) make final when all sorted out
    public Size size() {
        return size;
    }

}
