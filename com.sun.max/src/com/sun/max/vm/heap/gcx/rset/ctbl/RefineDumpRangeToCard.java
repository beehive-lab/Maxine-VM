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
package com.sun.max.vm.heap.gcx.rset.ctbl;

import static com.sun.max.vm.heap.gcx.rset.ctbl.CardTableRSet.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.HeapRangeDumper.DumpRangeRefinement;

public class RefineDumpRangeToCard implements DumpRangeRefinement {
    private CardTableRSet cardTableRSet;

    public RefineDumpRangeToCard(CardTableRSet cardTableRSet) {
        this.cardTableRSet = cardTableRSet;
    }

    public void refineRange(HeapRangeDumper heapDumper, Address unparsable) {
        int startCardIndex = cardTableRSet.cardTable.tableEntryIndex(unparsable);
        if (traceCardTableRSet()) {
            Log.print("Refining heap dumping range to card #");
            Log.println(startCardIndex);
        }
        Address start = cardTableRSet.cfoTable.cellStart(startCardIndex).asPointer();
        Address end = cardTableRSet.cardTable.rangeStart(startCardIndex).plus(CARD_SIZE);
        heapDumper.setRange(start, end);
    }
}
