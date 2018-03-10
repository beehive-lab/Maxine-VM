/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.compiler.target.aarch64;

import com.oracle.max.asm.target.aarch64.*;
import com.sun.max.annotate.C_FUNCTION;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

public final class Aarch64TargetMethodUtil {

    public static final int RIP_CALL_INSTRUCTION_SIZE = 4;

    /**
     * Extract an instruction from the code array which starts at index=idx.
     * @param code
     * @param idx
     * @return
     */
    private static int extractInstruction(byte [] code, int idx) {
        assert code.length >= idx + 4 : "Insufficient space in code buffer";
        int instruction = 0;
        instruction = ((code[idx + 3] & 0xFF) << 24) | ((code[idx + 2] & 0xFF) << 16) | ((code[idx + 1] & 0xFF) << 8) | (code[idx + 0] & 0xFF);
        return instruction;
    }

    @C_FUNCTION
    public static native void maxine_cache_flush(Pointer start, int length);

    /**
     * Thread safe patching of the displacement field in a direct call.
     *
     * @return the target of the call prior to patching
     */
    public static CodePointer mtSafePatchCallDisplacement(TargetMethod tm, CodePointer callSite, CodePointer target) {
        throw FatalError.unimplemented();
    }

    /**
     * Fixup the target displacement (28bit) in a branch immediate instruction.
     * Returns the old displacement.
     *
     * @param code - array containing the instruction
     * @param callOffset - offset of the call in code
     * @param displacement - the new displacement.
     * @return the previous displacement
     */
    public static int fixupCall28Site(byte [] code, int callOffset, int displacement) {
        int instruction = extractInstruction(code, callOffset);
        int oldDisplacement = Aarch64Assembler.bImmExtractDisplacement(instruction);
        instruction = Aarch64Assembler.bImmPatch(instruction, displacement);
        code[callOffset + 0] = (byte) (instruction       & 0xFF);
        code[callOffset + 1] = (byte) (instruction >> 8  & 0xFF);
        code[callOffset + 2] = (byte) (instruction >> 16 & 0xFF);
        code[callOffset + 3] = (byte) (instruction >> 24 & 0xFF);
        return oldDisplacement;
    }

    /**
     * Fix up the target displacement in a branch immediate instruction.
     * Returns the old displacement.
     *
     * @param tm - the method containing the call
     * @param callOffset - the offset of the call in the methods code
     * @param target - the new target
     * @return the previous displacement
     */
    public static CodePointer fixupCall32Site(TargetMethod tm, int callOffset, CodePointer target) {
        CodePointer callSite = tm.codeAt(callOffset);

        long disp64 = target.toLong() - callSite.plus(RIP_CALL_INSTRUCTION_SIZE).toLong();
        int disp32 = (int) disp64;
        int oldDisplacement = 0;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        if (MaxineVM.isHosted()) {
            byte [] code = tm.code();
            oldDisplacement = fixupCall28Site(code, callOffset, disp32);
        } else {
            final Pointer callSitePointer = callSite.toPointer();
            int instruction = callSitePointer.readInt(0);
            oldDisplacement = Aarch64Assembler.bImmExtractDisplacement(instruction);
            callSitePointer.writeInt(0, Aarch64Assembler.bImmPatch(instruction, disp32));
        }
        return callSite.plus(RIP_CALL_INSTRUCTION_SIZE).plus(oldDisplacement);
    }

    public static boolean isPatchableCallSite(CodePointer callSite) {
        final Address callSiteAddress = callSite.toAddress();
        return callSiteAddress.isWordAligned();
    }
}
