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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.CardTable.*;

public final class NoDirtyCardsVerifier implements HeapSpaceRangeVisitor, OverlappingCellVisitor {
    final CardTableRSet cardTableRSet;

    public NoDirtyCardsVerifier(CardTableRSet cardTableRSet) {
        this.cardTableRSet = cardTableRSet;
    }

    @Override
    public void visitCells(Address start, Address end) {
        cardTableRSet.visitCards(start, end, CardState.DIRTY_CARD, this);
    }

    @Override
    public Pointer visitCell(Pointer cell, Address start, Address end) {
        // Must never be called if there aren't any dirty card.
        Log.print("Card is Dirty for range [");
        Log.print(start);
        Log.print(", ]; ");
        Log.print(end);
        Log.println(", ]; ");
        return end.asPointer();
    }
}
