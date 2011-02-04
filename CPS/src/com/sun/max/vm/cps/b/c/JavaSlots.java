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

//import com.sun.max.lang.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;

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
            result.slots = Arrays.copyOf(slots, slots.length);
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
