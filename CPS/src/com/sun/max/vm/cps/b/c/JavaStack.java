/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * An operand stack for abstract byte code interpretation.
 *
 * @author Bernd Mathiske
 */
final class JavaStack extends JavaSlots {

    private int stackIndex = 0;
    private static final FillerJavaStackSlot FILLER = new FillerJavaStackSlot();

    JavaStack(StackVariableFactory variableFactory) {
        super(variableFactory);
    }


    public CirVariable push(Kind kind) {
        final CirVariable variable = variableFactory.makeVariable(kind, stackIndex);
        final VariableJavaStackSlot slot = new VariableJavaStackSlot(variable);
        slots[stackIndex] = slot;
        stackIndex++;
        if (kind == Kind.LONG || kind == Kind.DOUBLE) {
            slots[stackIndex] = FILLER;
            stackIndex++;
        }
        return variable;
    }

    CirVariable get(Kind kind, int nSlotsDown) {
        final int slotIndex = stackIndex - nSlotsDown;
        final JavaStackSlot slot = slots[slotIndex];
        assert slot instanceof VariableJavaStackSlot;
        final CirVariable variable = ((VariableJavaStackSlot) slot).cirVariable();
        assert variable == variableFactory.makeVariable(kind, slotIndex);
        return variable;
    }

    public CirVariable getTop() {
        final JavaStackSlot top = slots[stackIndex - 1];
        if (top instanceof FillerJavaStackSlot) {
            final JavaStackSlot slot = slots[stackIndex - 2];
            assert slot instanceof VariableJavaStackSlot;
            return ((VariableJavaStackSlot) slot).cirVariable();
        }
        assert top instanceof VariableJavaStackSlot;
        return ((VariableJavaStackSlot) top).cirVariable();
    }

    public CirVariable pop() {
        --stackIndex;
        final JavaStackSlot slot = slots[stackIndex];
        if (slot instanceof FillerJavaStackSlot) {
            --stackIndex;
            final JavaStackSlot slot2 = slots[stackIndex];
            assert slot2 instanceof VariableJavaStackSlot;
            return ((VariableJavaStackSlot) slot2).cirVariable();
        }
        assert slot instanceof VariableJavaStackSlot;
        return ((VariableJavaStackSlot) slot).cirVariable();
    }

    @Override
    public JavaStack copy() {
        return (JavaStack) super.copy();
    }

    @Override
    protected int effectiveLength() {
        int result = stackIndex;
        while (result > 0 && slots[stackIndex - 1] instanceof FillerJavaStackSlot) {
            result--;
        }
        return result;
    }

}
