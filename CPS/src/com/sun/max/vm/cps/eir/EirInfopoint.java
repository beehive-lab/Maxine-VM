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
package com.sun.max.vm.cps.eir;

import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import java.lang.reflect.*;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.collect.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirInfopoint<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter>
                      extends EirStop<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    public final int opcode;

    public final EirOperand operand;

    public EirInfopoint(EirBlock block, int opcode, EirValue destination) {
        super(block);
        this.opcode = opcode;
        if (opcode == Bytecodes.HERE) {
            assert destination != null;
            this.operand = new EirOperand(this, EirOperand.Effect.DEFINITION, G);
            this.operand.setEirValue(destination);
        } else {
            assert destination == null;
            this.operand = null;
        }
    }

    public void addRegisterReferenceMap(ByteArrayBitMap map) {
        for (EirVariable variable : liveVariables()) {
            if (variable.location().category() == EirLocationCategory.INTEGER_REGISTER) {
                final EirRegister register = (EirRegister) variable.location();
                if (variable.kind().isReference) {
                    map.set(register.ordinal);
                }
            }
        }
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }
    public EirOperand operand() {
        return operand;
    }

    public EirLocation operandLocation() {
        return operand.location();
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        if (operand != null) {
            visitor.run(operand);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "-" + Bytecodes.nameOf(opcode) + " " + (operand == null ? "" : operand + " ") + javaFrameDescriptor();
    }
}
