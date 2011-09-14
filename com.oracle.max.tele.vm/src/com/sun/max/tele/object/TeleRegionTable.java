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


public class TeleRegionTable extends TeleTupleObject {
    static private TeleRegionTable teleRegionTable = null;

    static private synchronized void initialize(TeleVM vm) {
        if (teleRegionTable != null) {
            return;
        }
        Reference theRegionTableReference = vm.teleFields().RegionTable_theRegionTable.staticTupleReference(vm);
        teleRegionTable = new TeleRegionTable(vm, theRegionTableReference);
    }

    public static TeleRegionTable theTeleRegionTable(TeleVM vm) {
        if (teleRegionTable == null) {
            initialize(vm);
        }
        return teleRegionTable;
    }

    final  RegionTable regionTable;

    private TeleRegionTable(TeleVM vm, Reference reference) {
        super(vm, reference);
        Reference theRegionTable = vm().teleFields().RegionTable_theRegionTable.staticTupleReference(vm);
        Address start = vm().teleFields().RegionTable_regionPoolStart.readWord(theRegionTable).asAddress();
        Address end = vm().teleFields().RegionTable_regionPoolEnd.readWord(theRegionTable).asAddress();
        int numRegions = vm().teleFields().RegionTable_length.readInt(theRegionTable);
        int infoSize = vm().teleFields().RegionTable_regionInfoSize.readInt(theRegionTable);
        regionTable = new RegionTable(start, end, numRegions, infoSize);
    }

}
