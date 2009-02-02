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
package com.sun.max.vm.compiler.b.c;

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * An abstract operand stack for abstract byte code interpretation.
 *
 * @author Bernd Mathiske
 */
final class JavaStack extends JavaSlots {

    JavaStack(StackVariableFactory variableFactory) {
        super(variableFactory);
    }

    private int _stackPointer = 0;

    public CirVariable push(Kind kind, BytecodeLocation location) {
        final CirVariable variable = _variableFactory.makeVariable(kind, _stackPointer, location);
        final VariableJavaStackSlot slot = new VariableJavaStackSlot(variable);
        _slots[_stackPointer] = slot;
        _stackPointer++;
        if (kind == Kind.LONG || kind == Kind.DOUBLE) {
            _slots[_stackPointer] = new FillerJavaStackSlot();
            _stackPointer++;
        }
        return variable;
    }

    CirVariable get(Kind kind, int nSlotsDown, BytecodeLocation location) {
        final int slotIndex = _stackPointer - nSlotsDown;
        final JavaStackSlot slot = _slots[slotIndex];
        assert slot instanceof VariableJavaStackSlot;
        final CirVariable variable = ((VariableJavaStackSlot) slot).cirVariable();
        assert variable == _variableFactory.makeVariable(kind, slotIndex, location);
        return variable;
    }

    public CirVariable getTop() {
        final JavaStackSlot top = _slots[_stackPointer - 1];
        if (top instanceof FillerJavaStackSlot) {
            final JavaStackSlot slot = _slots[_stackPointer - 2];
            assert slot instanceof VariableJavaStackSlot;
            return ((VariableJavaStackSlot) slot).cirVariable();
        }
        assert top instanceof VariableJavaStackSlot;
        return ((VariableJavaStackSlot) top).cirVariable();
    }

    public CirVariable pop() {
        --_stackPointer;
        final JavaStackSlot slot = _slots[_stackPointer];
        if (slot instanceof FillerJavaStackSlot) {
            --_stackPointer;
            final JavaStackSlot slot2 = _slots[_stackPointer];
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
        int result = _stackPointer;
        while (result > 0 && _slots[_stackPointer - 1] instanceof FillerJavaStackSlot) {
            result--;
        }
        return result;
    }

}
