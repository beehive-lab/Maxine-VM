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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;


public class TeleTricolorHeapMarker extends TeleTupleObject {

    private TeleMemoryRegion colorMapDataRegion;

    public TeleTricolorHeapMarker(TeleVM vm, RemoteReference heapMarkerReference) {
        super(vm, heapMarkerReference);
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        if (colorMapDataRegion == null) {
            final RemoteReference colorMapRef = fields().TricolorHeapMarker_colorMap.readRemoteReference(reference());
            if (!colorMapRef.isZero()) {
                colorMapDataRegion = (TeleMemoryRegion) objects().makeTeleObject(colorMapRef);
            }
        }
        return true;
    }

    public boolean isAllocated() {
        if (colorMapDataRegion == null) {
            return false;
        }
        colorMapDataRegion.updateCacheIfNeeded();
        return colorMapDataRegion.isAllocated();
    }

    /**
     * Gets the VM object (allocated in the boot heap) that describes the memory region, allocated dynamically
     * from the OS, in which the heap marker stores the bitmap data.  The memory region exists, but is
     * <em>unallocated</em> until the VM's heap marker is initialized and allocates the memory.
     *
     * @return the VM memory region object that describes the bitmap data allocation
     */
    public TeleMemoryRegion colorMapDataRegion() {
        return colorMapDataRegion;
    }

    /**
     * Number of bytes covered by each bit of the bitmaps. Must be a power of 2 of a number of words.
     *
     * @see TricolorHeapMarker#wordsCoveredPerBit
     */
    public int wordsCoveredPerBit() {
        return isAllocated() ? fields().TricolorHeapMarker_wordsCoveredPerBit.readInt(reference()) : 0;
    }

    /**
     * Start of the contiguous range of addresses covered by the mark bitmap.
     *
     * @see TricolorHeapMarker#coveredAreaStart
     */
    public Address coveredAreaStart() {
        return isAllocated() ? fields().TricolorHeapMarker_coveredAreaStart.readWord(reference()).asAddress() : Address.zero();
    }

    /**
     * End of the contiguous range of addresses  covered by the mark bitmap.
     *
     * @see TricolorHeapMark#coveredAreaEnd
     */
    public Address coveredAreaEnd() {
        return isAllocated() ? fields().TricolorHeapMarker_coveredAreaEnd.readWord(reference()).asAddress() : Address.zero();
    }

    public Address bitmapStorage() {
        return isAllocated() ? colorMapDataRegion.getRegionStart() : Address.zero();
    }

    public long bitmapSize() {
        return isAllocated() ? colorMapDataRegion.getRegionNBytes() : 0L;
    }


}
