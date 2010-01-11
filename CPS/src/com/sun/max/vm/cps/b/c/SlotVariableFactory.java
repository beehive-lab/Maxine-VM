/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
        return kind.isCategory2() ? index < maxSlotCount : index <= maxSlotCount;
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
