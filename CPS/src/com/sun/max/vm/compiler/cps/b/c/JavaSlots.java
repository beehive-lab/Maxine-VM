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

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.variable.*;

/**
 * Aggregation of Java stack locations and a variable factory.
 *
 * @see JavaFrame
 * @see JavaStack
 *
 * @author Bernd Mathiske
 */
abstract class JavaSlots implements Cloneable {

    protected final SlotVariableFactory variableFactory;
    protected JavaStackSlot[] slots;

    public abstract static class JavaStackSlot {
    }

    public static class FillerJavaStackSlot extends JavaStackSlot {
    }

    public static class VariableJavaStackSlot extends JavaStackSlot {
        private final CirVariable cirVariable;
        public VariableJavaStackSlot(CirVariable variable) {
            cirVariable = variable;
        }
        public CirVariable cirVariable() {
            return cirVariable;
        }
    }

    protected JavaSlots(SlotVariableFactory variableFactory) {
        this.variableFactory = variableFactory;
        this.slots = new JavaStackSlot[variableFactory.getMaxSlotCount()];
    }

    public JavaSlots copy() {
        try {
            final JavaSlots result = (JavaSlots) clone();
            result.slots = Arrays.copy(slots, new JavaStackSlot[slots.length]);
            return result;
        } catch (CloneNotSupportedException e) {
            throw ProgramError.unexpected(e);
        }
    }

    protected abstract int effectiveLength();

    public final CirValue[] makeDescriptor() {
        final CirValue[] descriptor = CirCall.newArguments(effectiveLength());
        for (int i = 0; i < descriptor.length; i++) {
            if (slots[i] == null || slots[i] instanceof FillerJavaStackSlot) {
                descriptor[i] = CirValue.UNDEFINED;
            } else {
                assert slots[i] instanceof VariableJavaStackSlot;
                descriptor[i] = ((VariableJavaStackSlot) (slots[i])).cirVariable();
            }
        }
        return descriptor;
    }

}
