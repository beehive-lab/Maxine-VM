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

import java.util.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.runtime.*;

/**
 * Emit the prologue for a method.
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public final class AMD64EirPrologue extends EirPrologue<AMD64EirInstructionVisitor, AMD64EirTargetEmitter> implements AMD64EirInstruction {

    public AMD64EirPrologue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            BitSet isCalleeSavedParameter,
                            EirValue[] parameters, EirLocation[] parameterLocations) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, isCalleeSavedParameter, parameters, parameterLocations);
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final AMD64Assembler asm = emitter.assembler();
            final AMD64GeneralRegister64 framePointer = emitter.framePointer();
            // emit a regular prologue
            final int frameSize = eirMethod().frameSize();
            if (frameSize != 0) {
                asm.subq(framePointer, frameSize);
            }
            if (Trap.STACK_BANGING && !eirMethod().classMethodActor().isVmEntryPoint()) {
                // emit a read of the stack stackGuardSize bytes down to trigger a stack overflow earlier than would otherwise occur.
                asm.mov(emitter.scratchRegister(), -Trap.stackGuardSize, emitter.stackPointer().indirect());
            }
        }
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
