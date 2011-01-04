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
