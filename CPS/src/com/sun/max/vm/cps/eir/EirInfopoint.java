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
