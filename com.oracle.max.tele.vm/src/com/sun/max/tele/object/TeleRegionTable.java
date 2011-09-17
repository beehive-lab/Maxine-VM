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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.reference.*;
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

/**
 *
 */
public class TeleRegionTable extends AbstractTeleVMHolder {
    private boolean initializing;
    private RegionTable regionTable;
    private Address regionTableAddress = Address.zero();

    public TeleRegionTable(TeleVM vm) {
        super(vm);
    }

    private boolean isInitialized() {
        return regionTable != null;
    }

    private void initialize() {
        if (isInitialized()) {
            return;
        }
        initializing();
    }

    private synchronized void initializing() {
        if (initializing) {
            return;
        }
        initializing = true;
        Reference theRegionTableReference =  vm().teleFields().RegionTable_theRegionTable.readReference(vm());
        if (theRegionTableReference.isZero()) {
            return;
        }
        int numRegions = vm().teleFields().RegionTable_length.readInt(theRegionTableReference);
        if (numRegions == 0) {
            return;
        }
        HeapRegionConstants.initializeWithConstants(vm().teleFields().HeapRegionConstants_regionSizeInBytes.readInt(vm()));
        Address start = vm().teleFields().RegionTable_regionPoolStart.readWord(theRegionTableReference).asAddress();
        Address end = vm().teleFields().RegionTable_regionPoolEnd.readWord(theRegionTableReference).asAddress();
        int infoSize = vm().teleFields().RegionTable_regionInfoSize.readInt(theRegionTableReference);
        regionTable = new RegionTable(start, end, numRegions, infoSize);
        regionTableAddress = theRegionTableReference.toOrigin();
        initializing = false;
    }

    public Address regionInfo(int regionID) {
        assert regionID > INVALID_REGION_ID;
        return regionTableAddress.plus(regionTable.regionInfoOffset(regionID));
    }

    public int regionID(Address address) {
        initialize();
        if (isInitialized()) {
            return regionTable.regionID(address);
        }
        return INVALID_REGION_ID;
    }
}
