/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.field.*;
import com.sun.max.tele.heap.region.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.gen.mse.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.reference.*;


public class TeleGenMSEHeapScheme extends TeleRegionBasedHeapScheme {
    private TeleCardTableRSet teleCardTableRSet;
    private Address nurseryEnd = Address.zero();
    private Address nurseryStart = Address.zero();
    private Address nurseryTop = Address.zero();
    private Reference nurseryAllocator = Reference.zero();
    private long lastUpdateEpoch = -1L;

    TeleGenMSEHeapScheme(TeleVM vm) {
        super(vm);
    }

    boolean isInNursery(Address address) {
        return address.greaterEqual(nurseryStart) && address.lessThan(nurseryEnd);
    }

    boolean isLiveInNursery(Address address) {
        return address.greaterEqual(nurseryStart) && address.lessThan(nurseryTop);

    }

    private void initializeNursery() {
        if (nurseryAllocator.isZero()) {
            nurseryAllocator =   TeleInstanceReferenceFieldAccess.readPath(toReference(),
                            vm().fields().GenMSEHeapScheme_youngSpace, vm().fields().NoAgingNursery_allocator);
        }
        nurseryStart = vm().fields().BaseAtomicBumpPointerAllocator_start.readWord(nurseryAllocator).asAddress();
        nurseryEnd = vm().fields().BaseAtomicBumpPointerAllocator_end.readWord(nurseryAllocator).asAddress();
    }

    private void updateNurseryTop() {
        // TODO: add invariant check that the new nursery top must be greater or equal to the old one unless
        // the GC count is different or we're not in mutating phase.
        nurseryTop = vm().fields().BaseAtomicBumpPointerAllocator_top.readWord(nurseryAllocator).asAddress();
    }


    // TODO (mlvdv) instantiate through ordinary TeleObjectFactory
    TeleCardTableRSet teleCardTableRSet() {
        if (teleCardTableRSet == null) {
            Reference cardTableRSetReference = vm().fields().GenMSEHeapScheme_cardTableRSet.readReference(toReference());
            teleCardTableRSet = new TeleCardTableRSet(vm(), cardTableRSetReference);
        }
        return teleCardTableRSet;
    }

    @Override
    public Class heapSchemeClass() {
        return GenMSEHeapScheme.class;
    }

    public int gcForwardingPointerOffset() {
        return Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();
    }

    public boolean isObjectForwarded(Pointer origin) {
        if (origin.isNotZero()) {
            Pointer possibleForwardingPointer = memory().readWord(origin.plus(gcForwardingPointerOffset())).asPointer();
            if (isForwardingPointer(possibleForwardingPointer)) {
                return true;
            }
        }
        return false;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        // FIXME (ld): need to check if the region the origin points to is in an evacuated area, and if so, check if it is forwarded.
        return (pointer.isNotZero()) && pointer.and(1).toLong() == 1;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return isForwardingPointer(pointer) ? pointer.minus(1) : pointer;
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        // FIXME (ld): need to check if the region the origin points to is in an evacuated area, and if so, get the forwarded address if any.
        if (origin.isNotZero()) {
            Pointer possibleForwardingPointer = memory().readWord(origin.plus(gcForwardingPointerOffset())).asPointer();
            if (isForwardingPointer(possibleForwardingPointer)) {
                final Pointer newCell = getTrueLocationFromPointer(possibleForwardingPointer);
                if (newCell.isNotZero()) {
                    return Layout.generalLayout().cellToOrigin(newCell);
                }
            }
        }
        return origin;
    }



    class GenMSERegionInfo extends GCXHeapRegionInfo {
        final int cardIndex;

        GenMSERegionInfo(Address address) {
            super(address);
            cardIndex = teleCardTableRSet().cardIndex(address);
        }

        @Override
        public String terseInfo() {
            return (regionID < 0 ? "-" : "region #" + regionID) + (cardIndex < 0 ? " -" : " card# " + cardIndex);
        }

        @Override
        public MaxMemoryStatus status() {
            if (regionID < 0) {
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryStatus.UNKNOWN;
                }
                // in doubt...
                return MaxMemoryStatus.LIVE;
            }
            switch(vm().heap().lastUpdateHeapPhase()) {
                case MUTATING:
                    if (isInNursery(address)) {
                        return address.lessThan(nurseryTop) ? MaxMemoryStatus.LIVE : MaxMemoryStatus.FREE;
                    }
                    return MaxMemoryStatus.LIVE;
                case RECLAIMING:
                    return MaxMemoryStatus.LIVE;
                case ANALYZING:
                    return MaxMemoryStatus.LIVE;
            }
            return MaxMemoryStatus.UNKNOWN;
        }
    }

    @Override
    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new GenMSERegionInfo(address);
    }

    public List<MaxCodeLocation> inspectableMethods() {
        // TODO (ld)
        return EMPTY_METHOD_LIST;
    }

    public MaxMarkBitsInfo markBitInfo() {
        // TODO (ld)
        return null;
    }

    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            if (nurseryStart.isZero()) {
                initializeNursery();
                if (nurseryStart.isZero()) {
                    lastUpdateEpoch = epoch;
                    return;
                }
            }
            // refresh nursery allocator's allocation hand.
            updateNurseryTop();
            lastUpdateEpoch = epoch;
        }
    }

    @Override
    public String tagName(int tag) {
        final Enum [] tagValues = GenMSEHeapScheme.GenMSEHeapRegionTag.values();
        if (tag >= 0 && tag < tagValues.length) {
            return tagValues[tag].name();
        }
        return "";
    }
}
