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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Canonical surrogate for a {@link BootHeapRegion} object in the VM,
 * a singleton that describes the memory occupied by the boot heap.
 * <p>
 * Usage defaults to 100% if nothing can be determined about the allocation mark.
 */
public class TeleBootHeapRegion extends TeleLinearAllocationMemoryRegion {

    private static final int TRACE_VALUE = 1;

    /**
     * Location in VM memory (in the boot heap) of the first byte of the boot heap reference map.
     */
    private Address referenceMap = Address.zero();

    private final int mask;


    public TeleBootHeapRegion(TeleVM vm, RemoteReference bootHeapRegionReference) {
        super(vm, bootHeapRegionReference);
        mask = (1 << Word.widthValue().log2numberOfBits) - 1;
    }

    /** {@inheritDoc}
     * <p>
     * Preempt upward the priority for tracing on {@link BootHeapRegion} objects,
     * since they usually represent very significant regions.
     */
    @Override
    protected int getObjectUpdateTraceValue(long epoch) {
        return TRACE_VALUE;
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        if (referenceMap.isZero()) {
            final RemoteReference refMapByteArrayRef = fields().BootHeapRegion_referenceMapBytes.readRemoteReference(reference());
            if (!refMapByteArrayRef.isZero()) {
                final TeleArrayObject refMapByteArray = (TeleArrayObject) objects().makeTeleObject(refMapByteArrayRef);
                referenceMap = refMapByteArrayRef.toOrigin().plus(refMapByteArray.arrayOffsetFromOrigin());
            }
        }
        return true;
    }

    public boolean isRefMapMarked(Address address) {
        if (!referenceMap.isZero() && isAllocated() && contains(address)) {
            final int bitIndex = address.minus(getRegionStart()).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
            final int wordIndex = bitIndex >> Word.widthValue().log2numberOfBits;
            final Address tableWordAddress = referenceMap.plus(wordIndex * Word.size());
            final Address refMapWord = memory().readWord(tableWordAddress).asAddress();
            final int bitWordIndex = bitIndex & mask;
            //final String suffix = refMapWord.isBitSet(bitWordIndex) ? " MARKED" : "";
            //System.out.println("bit=" + bitIndex + " (" + wordIndex + "/" + bitWordIndex + ") word@" + tableWordAddress.to0xHexString() + ", value=" + refMapWord.to0xHexString() + suffix);
            return refMapWord.isBitSet(bitWordIndex);
        }
        return false;
    }


}
