/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.target.amd64;

import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.xir.CiXirAssembler.XirMark;

public enum AMD64CallOp implements StandardOp.CallOpcode<AMD64MacroAssembler, LIRCall> {
    DIRECT_CALL, INDIRECT_CALL, NATIVE_CALL;

    public LIRInstruction create(Object target, CiValue result, List<CiValue> arguments, CiValue targetAddress, LIRDebugInfo info, Map<XirMark, Mark> marks, List<CiValue> pointerSlots) {
        return new LIRCall(this, target, result, arguments, targetAddress, info, marks, false, pointerSlots);
    }

    @Override
    public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRCall op) {
        switch (this) {
            case DIRECT_CALL: {
                callAlignment(tasm);
                if (op.marks != null) {
                    op.marks.put(XirMark.CALLSITE, tasm.recordMark(null, new Mark[0]));
                }
                directCall(tasm, op.target, op.info);
                break;
            }
            case INDIRECT_CALL: {
                callAlignment(tasm);
                if (op.marks != null) {
                    op.marks.put(XirMark.CALLSITE, tasm.recordMark(null, new Mark[0]));
                }
                CiRegister reg = tasm.asRegister(op.targetAddress());
                indirectCall(tasm, reg, op.target, op.info);
                break;
            }
            case NATIVE_CALL: {
                CiRegister reg = tasm.asRegister(op.targetAddress());
                indirectCall(tasm, reg, op.target, op.info);
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }
    }

    public void callAlignment(TargetMethodAssembler<AMD64MacroAssembler> tasm) {
        if (GraalOptions.AlignCallsForPatching) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = tasm.masm.codeBuffer.position();
            offset += tasm.target.arch.machineCodeCallDisplacementOffset;
            while (offset++ % tasm.target.wordSize != 0) {
                tasm.masm.nop();
            }
        }
    }

    public static void callStub(TargetMethodAssembler<AMD64MacroAssembler> tasm, CompilerStub stub, CiKind resultKind, LIRDebugInfo info, CiValue result, CiValue... args) {
        assert args.length == stub.inArgs.length;
        for (int i = 0; i < args.length; i++) {
            assert stub.inArgs[i].inCallerFrame();
            AMD64MoveOp.move(tasm, stub.inArgs[i].asOutArg(), args[i]);
        }

        directCall(tasm, stub.stubObject, info);

        if (result.isLegal()) {
            AMD64MoveOp.move(tasm, result, stub.outResult.asOutArg());
        }

        // Clear out parameters
        if (GraalOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                CiStackSlot inArg = stub.inArgs[i];
                CiStackSlot outArg = inArg.asOutArg();
                CiAddress dst = tasm.asAddress(outArg);
                tasm.masm.movptr(dst, 0);
            }
        }
    }

    public static void directCall(TargetMethodAssembler<AMD64MacroAssembler> tasm, Object target, LIRDebugInfo info) {
        int before = tasm.masm.codeBuffer.position();
        if (target instanceof CiRuntimeCall) {
            long maxOffset = tasm.compilation.compiler.runtime.getMaxCallTargetOffset((CiRuntimeCall) target);
            if (maxOffset != (int) maxOffset) {
                // offset might not fit a 32-bit immediate, generate an
                // indirect call with a 64-bit immediate
                CiRegister scratch = tasm.compilation.registerConfig.getScratchRegister();
                // TODO(cwi): we want to get rid of a generally reserved scratch register.
                tasm.masm.movq(scratch, 0L);
                tasm.masm.call(scratch);
            } else {
                tasm.masm.call();
            }
        } else {
            tasm.masm.call();
        }
        int after = tasm.masm.codeBuffer.position();
        tasm.recordDirectCall(before, after, asCallTarget(tasm, target), info);
        tasm.recordExceptionHandlers(after, info);
        tasm.masm.ensureUniquePC();
    }

    public static void directJmp(TargetMethodAssembler<AMD64MacroAssembler> tasm, Object target) {
        int before = tasm.masm.codeBuffer.position();
        tasm.masm.jmp(0, true);
        int after = tasm.masm.codeBuffer.position();
        tasm.recordDirectCall(before, after, asCallTarget(tasm, target), null);
        tasm.masm.ensureUniquePC();
    }

    public static void indirectCall(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiRegister dst, Object target, LIRDebugInfo info) {
        int before = tasm.masm.codeBuffer.position();
        tasm.masm.call(dst);
        int after = tasm.masm.codeBuffer.position();
        tasm.recordIndirectCall(before, after, asCallTarget(tasm, target), info);
        tasm.recordExceptionHandlers(after, info);
        tasm.masm.ensureUniquePC();
    }

    private static Object asCallTarget(TargetMethodAssembler<AMD64MacroAssembler> tasm, Object o) {
        return tasm.compilation.compiler.runtime.asCallTarget(o);
    }

    public static void shouldNotReachHere(TargetMethodAssembler<AMD64MacroAssembler> tasm) {
        if (GraalOptions.GenAssertionCode) {
            directCall(tasm, CiRuntimeCall.Debug, null);
            tasm.masm.hlt();
        }
    }
}
