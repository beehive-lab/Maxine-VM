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
package com.sun.max.vm.compiler.cps.b.c;

import com.sun.max.vm.compiler.cps.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * Mapping of abstract stack frame locations to IR variables.
 *
 * @author Bernd Mathiske
 */
final class JavaFrame extends JavaSlots {

    JavaFrame(LocalVariableFactory variableFactory) {
        super(variableFactory);
        variableFactory.copyParametersInto(slots);
    }

    public CirVariable makeVariable(Kind kind, int localIndex) {
        final CirVariable var = variableFactory.makeVariable(kind, localIndex);
        final VariableJavaStackSlot slot = new VariableJavaStackSlot(var);
        slots[localIndex] = slot;
        return var;
    }

    public CirVariable getReferenceOrWordVariable(int localIndex) {
        final JavaStackSlot slot = slots[localIndex];
        assert slot instanceof VariableJavaStackSlot;
        final CirVariable result = ((VariableJavaStackSlot) slot).cirVariable();
        assert result.kind() == Kind.REFERENCE || result.kind() == Kind.WORD;
        return result;
    }

    @Override
    public JavaFrame copy() {
        return (JavaFrame) super.copy();
    }

    @Override
    protected int effectiveLength() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                return i;
            }
        }
        return slots.length;
    }

}
