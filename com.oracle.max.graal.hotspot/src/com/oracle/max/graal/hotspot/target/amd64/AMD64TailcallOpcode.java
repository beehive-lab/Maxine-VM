/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.hotspot.target.amd64;

import com.oracle.max.asm.target.amd64.AMD64MacroAssembler;
import com.oracle.max.graal.compiler.asm.TargetMethodAssembler;
import com.oracle.max.graal.compiler.lir.LIRInstruction;
import com.oracle.max.graal.compiler.lir.LIROpcode;
import com.oracle.max.graal.compiler.target.amd64.AMD64LIRInstruction;
import com.oracle.max.graal.compiler.util.Util;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiValue;

/**
 * Performs a hard-coded tail call to the specified target, which normally should be an RiCompiledCode instance.
 */
public enum AMD64TailcallOpcode implements LIROpcode {
    TAILCALL;

    public LIRInstruction create(final Object target, CiValue[] parameters, CiValue[] callingConvention) {
        CiValue[] inputs = parameters.clone();
        CiValue[] temps = callingConvention.clone();
        assert inputs.length == temps.length;

        return new AMD64LIRInstruction(this, CiValue.IllegalValue, null, inputs, temps) {
            @Override
            public void emitCode(TargetMethodAssembler tasm, AMD64MacroAssembler masm) {
                emit(tasm, masm, target, inputs, temps);
            }
        };
    }

    private void emit(TargetMethodAssembler tasm, AMD64MacroAssembler masm, Object target, CiValue[] inputs, CiValue[] temps) {
        switch (this) {
            case TAILCALL: {
                // move all parameters to the correct positions, according to the calling convention
                for (int i = 0; i < inputs.length; i++) {
                    assert inputs[i].kind == CiKind.Object || inputs[i].kind == CiKind.Int || inputs[i].kind == CiKind.Long : "only Object, int and long supported for now";
                    assert temps[i].isRegister() : "too many parameters";
                    if (inputs[i].isRegister()) {
                        masm.movq(temps[i].asRegister(), inputs[i].asRegister());
                    } else {
                        masm.movq(temps[i].asRegister(), tasm.asAddress(inputs[i]));
                    }
                }
                // destroy the current frame (now the return address is the top of stack)
                masm.leave();
                // jump to the target method
                int before = masm.codeBuffer.position();
                masm.jmp(0, true);
                int after = masm.codeBuffer.position();
                tasm.recordDirectCall(before, after, target, null);
                masm.ensureUniquePC();
                break;
            }
            default:   throw Util.shouldNotReachHere();
        }
    }
}
