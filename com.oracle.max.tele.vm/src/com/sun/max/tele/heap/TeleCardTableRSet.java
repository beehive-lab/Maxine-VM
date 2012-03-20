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
package com.sun.max.tele.heap;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.reference.*;

public class TeleCardTableRSet extends TeleTupleObject {
    /**
     * Local shallow copy of the card table. Only has the bounds of the table and covered address.
     * Allow to re-use card table code for various card computations (e.g., card index to card table or heap address, heap address to card index,etc.)
     * These can then be used to fetch data off the real card table in the inspected VM.
     *
     */
    final CardTable cardTable;

    protected TeleCardTableRSet(TeleVM vm, Reference cardTableRSetReference) {
        super(vm, cardTableRSetReference);
        Reference cardTableReference = vm().fields().CardTableRSet_cardTable.readReference(cardTableRSetReference);
        Address coveredAreaStart = vm().fields().Log2RegionToByteMapTable_coveredAreaStart.readWord(cardTableReference).asAddress();
        Address coveredAreaEnd = vm().fields().Log2RegionToByteMapTable_coveredAreaEnd.readWord(cardTableReference).asAddress();
        Address tableAddress  = vm().fields().Log2RegionToByteMapTable_tableAddress.readWord(cardTableReference).asAddress();
        cardTable = new CardTable();
        cardTable.initialize(coveredAreaStart, coveredAreaEnd, tableAddress);
    }

    public int cardIndex(Address address) {
        return cardTable.tableEntryIndex(address);
    }

}
