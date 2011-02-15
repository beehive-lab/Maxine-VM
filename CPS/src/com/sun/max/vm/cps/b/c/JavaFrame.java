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

import com.sun.max.vm.cps.cir.variable.*;
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

    public CirVariable getVariable(int localIndex) {
        return ((VariableJavaStackSlot) slots[localIndex]).cirVariable();
    }

    public CirVariable makeVariable(Kind kind, int localIndex) {
        final CirVariable var = variableFactory.makeVariable(kind, localIndex);
        final VariableJavaStackSlot slot = new VariableJavaStackSlot(var);
        slots[localIndex] = slot;
        return var;
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
