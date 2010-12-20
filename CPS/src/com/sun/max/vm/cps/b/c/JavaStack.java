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
