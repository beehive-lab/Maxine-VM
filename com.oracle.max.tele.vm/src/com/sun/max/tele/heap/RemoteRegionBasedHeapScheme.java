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

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

public abstract class RemoteRegionBasedHeapScheme extends AbstractRemoteHeapScheme {

    protected final TeleRegionTable teleRegionTable;

    public RemoteRegionBasedHeapScheme(TeleVM vm) {
        super(vm);
        teleRegionTable = TeleRegionTable.makeTheTeleRegionTable(vm);
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {

        return new MaxMemoryManagementInfo() {
            final int regionID = teleRegionTable.regionID(address);

            public MaxMemoryStatus status() {
                if (regionID < 0) {
                    final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                    if (heapRegion == null) {
                        // The location is not in any memory region allocated by the heap.
                        return MaxMemoryStatus.UNKNOWN;
                    }
                }

                // Unclear what the semantics of this should be during GC.
                // We should be able to tell past the marking phase if an address point to a live object.
                // But what about during the marking phase ? The only thing that can be told is that
                // what was dead before marking begin should still be dead during marking.

                // TODO (ld) This requires the inspector to know intimately about the heap structures.
                // The current MS scheme  linearly allocate over chunk of free space discovered during the past MS.
                // However, it doesn't maintain these as "linearly allocating memory region". This could be done by formatting
                // all reusable free space as such (instead of the chunk of free list as is done now). in any case.

                return MaxMemoryStatus.LIVE;
            }

            public String terseInfo() {
                return regionID < 0 ? "-" : "region #" + regionID;
            }

            public String shortDescription() {
                // Laurent: more information could be added here, will appear in tooltip
                return vm().heapScheme().name();
            }

            public Address address() {
                return address;
            }

            public TeleObject tele() {
                if (regionID < 0) {
                    return null;
                }
                return objects().makeTeleObject(vm().referenceManager().makeReference(teleRegionTable.regionInfo(regionID).asPointer()));
            }
        };
    }
}
