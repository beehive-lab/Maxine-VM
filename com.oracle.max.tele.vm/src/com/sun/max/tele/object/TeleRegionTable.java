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

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.reference.*;

/**
 * Inspector's surrogate for the heap region table used by region based GC (currently, MSE heap scheme).
 * @see RegionTable
 */
public class TeleRegionTable extends AbstractVmHolder {
    static TeleRegionTable theTeleRegionTable;

    public static synchronized TeleRegionTable makeTheTeleRegionTable(TeleVM vm) {
        if (theTeleRegionTable == null) {
            theTeleRegionTable = new TeleRegionTable(vm);
        }
        return theTeleRegionTable;
    }

    public static TeleRegionTable theTeleRegionTable() {
        assert theTeleRegionTable != null;
        return theTeleRegionTable;
    }

    private boolean initializing;
    private RegionTable regionTable;
    private Address regionTableAddress = Address.zero();

    private TeleRegionTable(TeleVM vm) {
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
        Reference theRegionTableReference =  vm().fields().RegionTable_theRegionTable.readReference(vm());
        if (theRegionTableReference.isZero()) {
            return;
        }
        int numRegions = vm().fields().RegionTable_length.readInt(theRegionTableReference);
        if (numRegions == 0) {
            return;
        }
        // The VM's region table is initialized. We build a local instance of RegionTable based on the values of the VM's instance.
        // The local copy will be identical, except for its address. This allows to use many function of RegionTable directly.
        HeapRegionConstants.initializeWithConstants(vm().fields().HeapRegionConstants_regionSizeInBytes.readInt(vm()));
        Address start = vm().fields().RegionTable_regionPoolStart.readWord(theRegionTableReference).asAddress();
        Address end = vm().fields().RegionTable_regionPoolEnd.readWord(theRegionTableReference).asAddress();
        int infoSize = vm().fields().RegionTable_regionInfoSize.readInt(theRegionTableReference);
        regionTable = new RegionTable(start, end, numRegions, infoSize);
        regionTableAddress = theRegionTableReference.toOrigin();
        initializing = false;
    }

    public boolean isValidRegionID(int regionID) {
        return regionTable.isValidRegionID(regionID);
    }

    public Address regionInfo(int regionID) {
        assert regionTable.isValidRegionID(regionID);
        return regionTableAddress.plus(regionTable.regionInfoOffset(regionID));
    }

    public Address regionStart(int regionID) {
        return regionTable.regionAddress(regionID);
    }
    public Address regionEnd(int regionID) {
        return regionStart(regionID + 1);
    }

    public int toRegionID(Address regionInfo) {
        assert isInitialized();
        if (regionInfo.lessThan(regionTableAddress)) {
            // May reach here when trying to create the nullHeapRegionInfo
            return INVALID_REGION_ID;
        }
        final int offsetFromTableAddress = regionInfo.minus(regionTableAddress).toInt();
        return regionTable.regionIDFromRegionInfoOffset(offsetFromTableAddress);
    }

    /**
     * Return the region ID of the heap region containing the location specified by the address.
     * @param address an address in the heap
     * @return a region ID
     */
    public int regionID(Address address) {
        if (!isInitialized()) {
            initialize();
            if (!isInitialized()) {
                return INVALID_REGION_ID;
            }
        }
        return regionTable.regionID(address);
    }
}
