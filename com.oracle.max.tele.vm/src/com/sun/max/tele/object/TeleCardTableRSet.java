/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.data.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;

/**
 * Local mirror of a card table in VM memory.
 *
 * @see CardTableRSet
 */
public final class TeleCardTableRSet extends TeleTupleObject {
    /**
     * Local shallow copy of the card table. Only has the bounds of the table and covered address.
     * Allow to re-use card table code for various card computations (e.g., card index to card table or heap address, heap address to card index,etc.)
     * These can then be used to fetch data off the real card table in the inspected VM.
     */
    final CardTable cardTable = new CardTable();

    /**
     * VM memory region backing the card table remembered set.
     */
    public final VmHeapRegion vmHeapRegion;

    private final Object localStatsPrinter = new Object() {

        @Override
        public String toString() {
            return "card table @" + cardTable.tableAddress() + "= [" + cardTable.coveredAreaStart().to0xHexString() + ", " + cardTable.coveredAreaEnd().to0xHexString() + "]";
        }
    };

    public TeleCardTableRSet(TeleVM vm, RemoteReference cardTableRSetReference) {
        super(vm, cardTableRSetReference);
        final TeleMemoryRegion rsetMemory = (TeleMemoryRegion) objects().makeTeleObject(fields().CardTableRSet_cardTableMemory.readReference(reference()));
        vmHeapRegion = new VmHeapRegion(vm, rsetMemory);
    }

    public int cardIndex(Address address) {
        return cardTable.tableEntryIndex(address);
    }


    private boolean updateCardTableCache() {
        try {
            RemoteReference cardTableReference = fields().CardTableRSet_cardTable.readReference(reference());
            Address tableAddress  = fields().Log2RegionToByteMapTable_tableAddress.readWord(cardTableReference).asAddress();
            final Address oldTableAddress = cardTable.tableAddress();
            if (tableAddress.equals(oldTableAddress)) {
                return true;
            }
            Address coveredAreaStart = fields().Log2RegionToByteMapTable_coveredAreaStart.readWord(cardTableReference).asAddress();
            Address coveredAreaEnd = fields().Log2RegionToByteMapTable_coveredAreaEnd.readWord(cardTableReference).asAddress();
            cardTable.initialize(coveredAreaStart, coveredAreaEnd, tableAddress);
        } catch (DataIOError dataIOError) {
            // No update; data read failed for some reason other than VM availability
            TeleWarning.message("TeleContiguousHeapSpace: ", dataIOError);
            dataIOError.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        statsPrinter.addStat(localStatsPrinter);
        return updateCardTableCache();
    }

    public TeleMemoryRegion getTeleMemoryRegion() {
        return (TeleMemoryRegion) vmHeapRegion.representation();
    }
}
