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
