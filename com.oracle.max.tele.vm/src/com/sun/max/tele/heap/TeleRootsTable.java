/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.heap;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


/**
 * Access to the VM root table, used by the GC to track object locations managed by the semi-space GC.
 */
public class TeleRootsTable extends AbstractVmHolder implements MaxRootsTable {

    private static final class RootsTableMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxRootsTable> {

        private final TeleRootsTable owner;

        protected RootsTableMemoryRegion(MaxVM vm, TeleRootsTable owner, TeleRuntimeMemoryRegion teleRuntimeMemoryRegion) {
            super(vm, teleRuntimeMemoryRegion);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return null;
        }

        public MaxRootsTable owner() {
            return owner;
        }

    }

    private final RootsTableMemoryRegion rootsTableMemoryRegion;

    public TeleRootsTable(TeleVM vm, TeleRuntimeMemoryRegion teleRuntimeMemoryRegion) {
        super(vm);
        rootsTableMemoryRegion = new RootsTableMemoryRegion(vm, this, teleRuntimeMemoryRegion);
    }

    public String entityName() {
        return "Root Table";
    }

    public String entityDescription() {
        return "The VM root table used by the semi-space collector to record object moves";
    }

    public MaxEntityMemoryRegion<MaxRootsTable> memoryRegion() {
        return rootsTableMemoryRegion;
    }

    public boolean contains(Address address) {
        return rootsTableMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        return null;
    }

}
