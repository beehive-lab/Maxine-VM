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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.cri.bytecode.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirInfopoint extends EirInfopoint<EirInstructionVisitor, AMD64EirTargetEmitter> {

    public AMD64EirInfopoint(EirBlock block, int opcode, EirValue destination) {
        super(block, opcode, destination);
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        emitter.addSafepoint(this);
        if (opcode == Bytecodes.SAFEPOINT) {
            final AMD64EirRegister.General r = (AMD64EirRegister.General) emitter.abi().safepointLatchRegister();
            final AMD64GeneralRegister64 register = r.as64();
            emitter.assembler().mov(register, register.indirect());
        } else if (opcode == Bytecodes.HERE) {
            final Label label = new Label();
            emitter.assembler().bindLabel(label);
            emitter.assembler().rip_lea(operandGeneralRegister().as64(), label);
        }
    }

    public AMD64EirRegister.General operandGeneralRegister() {
        return (AMD64EirRegister.General) operandLocation();
    }

}
