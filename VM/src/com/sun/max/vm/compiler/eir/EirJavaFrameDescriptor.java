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
package com.sun.max.vm.compiler.eir;

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.runtime.*;

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

    public EirJavaFrameDescriptor(EirInstruction instruction, EirJavaFrameDescriptor parent, BytecodeLocation bytecodeLocation, EirValue[] locals, EirValue[] stackSlots) {
        super(parent, bytecodeLocation, createOperands(instruction, locals), createOperands(instruction, stackSlots));
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
