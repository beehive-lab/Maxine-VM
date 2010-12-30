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

import com.sun.max.asm.amd64.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public final class AMD64EirEpilogue extends EirEpilogue<AMD64EirInstructionVisitor, AMD64EirTargetEmitter> implements AMD64EirInstruction {

    public AMD64EirEpilogue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            EirLocation resultLocation) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, resultLocation);
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final int frameSize = eirMethod().frameSize();
            final AMD64Assembler asm = emitter.assembler();
            final AMD64GeneralRegister64 framePointer = emitter.framePointer();
            if (frameSize != 0) {
                asm.addq(framePointer, frameSize);
            }
        }
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
