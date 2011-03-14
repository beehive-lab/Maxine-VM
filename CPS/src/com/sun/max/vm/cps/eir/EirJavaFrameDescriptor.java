/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.*;

/**
 * ATTENTION: unlike other Java frame descriptors, EIR Java frame descriptors must NOT share
 * partial structures, because their operands get updated by the register allocator.
 *
 * @author Bernd Mathiske
 */
public class EirJavaFrameDescriptor extends JavaFrameDescriptor<EirOperand> {

    private static EirOperand[] createOperands(EirInstruction instruction, EirValue[] values) {
        final EirOperand[] operands = new EirOperand[values.length];
        for (int i = 0; i < values.length; i++) {
            operands[i] = new EirOperand(instruction, EirOperand.Effect.USE, EirLocationCategory.all());
            operands[i].setEirValue(values[i]);
        }
        return operands;
    }

    public EirJavaFrameDescriptor(EirInstruction instruction, EirJavaFrameDescriptor parent, ClassMethodActor classMethodActor, int bytecodePosition, EirValue[] locals, EirValue[] stackSlots) {
        super(parent, classMethodActor, bytecodePosition, createOperands(instruction, locals), createOperands(instruction, stackSlots));
    }

    @Override
    public EirJavaFrameDescriptor parent() {
        return (EirJavaFrameDescriptor) super.parent();
    }

    @Override
    protected boolean slotsEqual(EirOperand[] operands1, EirOperand[] operands2) {
        if (operands1.length != operands2.length) {
            return false;
        }
        for (int i = 0; i < operands1.length; i++) {
            if (!operands1[i].location().equals(operands2[i].location())) {
                return false;
            }
        }
        return true;
    }
}
