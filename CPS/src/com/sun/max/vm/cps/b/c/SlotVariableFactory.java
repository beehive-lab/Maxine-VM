/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * Factory for variables that correspond to Java stack locations.
 *
 * @author Bernd Mathiske
 */
abstract class SlotVariableFactory {

    protected final CirVariableFactory cirVariableFactory;
    protected final Map<SlotPosition, CirVariable> positionToCirVariable;
    private final int maxSlotCount;

    protected SlotVariableFactory(CirVariableFactory cirVariableFactory, int maxSlotCount) {
        this.cirVariableFactory = cirVariableFactory;
        this.positionToCirVariable = new Hashtable<SlotPosition, CirVariable>();
        this.maxSlotCount = maxSlotCount;
    }

    public int getMaxSlotCount() {
        return maxSlotCount;
    }

    private boolean isValidIndex(Kind kind, int index) {
        return (!kind.isCategory1) ? index < maxSlotCount : index <= maxSlotCount;
    }

    void setVariable(int index, CirVariable cirVariable) {
        assert isValidIndex(cirVariable.kind(), index);
        final SlotPosition position = new SlotPosition(cirVariable.kind(), index);
        positionToCirVariable.put(position, cirVariable);
    }

    protected abstract CirVariable createSlotVariable(Kind kind, int slotIndex);

    public CirVariable makeVariable(Kind kind, int index) {
        assert isValidIndex(kind, index);
        final SlotPosition position = new SlotPosition(kind, index);
        CirVariable cirVariable = positionToCirVariable.get(position);
        if (cirVariable == null) {
            cirVariable = createSlotVariable(position.getKind(), index);
            positionToCirVariable.put(position, cirVariable);
        }
        return cirVariable;
    }
}
