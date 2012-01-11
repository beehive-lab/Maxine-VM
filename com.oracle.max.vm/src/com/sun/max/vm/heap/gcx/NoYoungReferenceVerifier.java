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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.CardTable.CardState;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

public final class NoYoungReferenceVerifier extends PointerIndexAndHeaderVisitor implements HeapSpaceRangeVisitor, OverlappingCellVisitor {
    final CardTableRSet cardTableRSet;
    final HeapSpace youngSpace;
    public NoYoungReferenceVerifier(CardTableRSet cardTableRSet, HeapSpace youngSpace) {
        this.cardTableRSet = cardTableRSet;
        this.youngSpace = youngSpace;
    }

    @Override
    public void visitCells(Address start, Address end) {
        cardTableRSet.visitCards(start, end, CardState.DIRTY_CARD, this);
    }

    private Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        checkNotYoung(origin, HUB_WORD_INDEX);
        final Hub hub = getHub(origin);
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
            if (hub.isJLRReference) {
                checkNotYoung(origin, SpecialReferenceManager.REFERENT_WORD_INDEX);
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
        } else if (specificLayout.isReferenceArrayLayout()) {
            final int length = Layout.readArrayLength(origin) + FIRST_ELEMENT_INDEX;
            for (int index = FIRST_ELEMENT_INDEX; index < length; index++) {
                checkNotYoung(origin, index);
            }
        }
        return cell.plus(Layout.size(origin));
    }

    @Override
    public Pointer visitCell(Pointer cell, Address start, Address end) {
        if (start.greaterEqual(cell) && cell.lessEqual(end)) {
            return visitCell(cell);
        }
        final Pointer origin = Layout.cellToOrigin(cell);
        final Pointer hubPointer = origin.plusWords(HUB_WORD_INDEX);

        if (hubPointer.greaterEqual(start) && hubPointer.lessThan(end)) {
            checkNotYoung(origin, HUB_WORD_INDEX);
        }
        final Hub hub = getHub(origin);
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this, start, end);
            if (hub.isJLRReference) {
                final Pointer referentFieldPointer = origin.plusWords(SpecialReferenceManager.REFERENT_WORD_INDEX);
                if (referentFieldPointer.greaterEqual(start) && referentFieldPointer.lessThan(end)) {
                    checkNotYoung(origin, SpecialReferenceManager.REFERENT_WORD_INDEX);
                }
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this, start, end);
        } else if (specificLayout.isReferenceArrayLayout()) {
            int length = Layout.readArrayLength(origin) + FIRST_ELEMENT_INDEX;
            int firstWordIndex = FIRST_ELEMENT_INDEX;
            Pointer p = origin.plusWords(FIRST_ELEMENT_INDEX);
            if (p.lessThan(start)) {
                firstWordIndex += start.minus(p).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
            }
            p = p.plusWords(length);
            if (end.lessThan(p)) {
                length -= p.minus(end).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
            }
            for (int index = firstWordIndex; index < length; index++) {
                checkNotYoung(origin, index);
            }
        }
        return cell.plus(Layout.size(origin));
    }


    void checkNotYoung(Pointer pointer, int wordIndex) {
        final Pointer cell = pointer.getReference(wordIndex).toOrigin();
        if (youngSpace.contains(cell)) {
            Log.print("Reference at ");
            Log.print(pointer.plusWords(wordIndex));
            Log.print(" holds young gen pointer to ");
            Log.println(cell);
            FatalError.breakpoint();
            FatalError.crash("invariant violation");
        }
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        checkNotYoung(pointer, wordIndex);
    }
}
