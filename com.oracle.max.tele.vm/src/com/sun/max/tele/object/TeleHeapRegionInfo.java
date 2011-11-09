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
import com.sun.max.vm.reference.*;

public class TeleHeapRegionInfo extends TeleTupleObject {
    final int regionID;

    protected TeleHeapRegionInfo(TeleVM vm, Reference heapRegionInfoReference) {
        super(vm, heapRegionInfoReference);
        regionID = TeleRegionTable.theTeleRegionTable().toRegionID(heapRegionInfoReference.toOrigin());
    }

    public int regionID() {
        return regionID;
    }

    public Address regionStart() {
        return TeleRegionTable.theTeleRegionTable().regionStart(regionID);
    }

    public Address regionEnd() {
        return TeleRegionTable.theTeleRegionTable().regionEnd(regionID);
    }

    public Address firstFreeChunk() {
        if (vm().fields().HeapRegionInfo_numFreeChunks.readInt(getReference()) > 0) {
            int offsetToFirstFreeChunk = vm().fields().HeapRegionInfo_firstFreeChunkOffset.readInt(getReference());
            return regionStart().plus(offsetToFirstFreeChunk);
        }
        return Address.zero();
    }

    public int flags() {
        if (regionID  >= 0) {
            return vm().fields().HeapRegionInfo_flags.readInt(getReference());
        }
        return 0;
    }
}
