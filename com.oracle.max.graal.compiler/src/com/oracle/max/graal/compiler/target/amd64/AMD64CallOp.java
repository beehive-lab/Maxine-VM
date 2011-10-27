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

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.xir.CiXirAssembler.XirMark;

public enum AMD64CallOp implements StandardOp.CallOpcode<AMD64LIRAssembler, LIRCall> {
    DIRECT_CALL, INDIRECT_CALL, NATIVE_CALL;

    public LIRInstruction create(Object target, CiValue result, List<CiValue> arguments, CiValue targetAddress, LIRDebugInfo info, Map<XirMark, Mark> marks, List<CiValue> pointerSlots) {
        return new LIRCall(this, target, result, arguments, targetAddress, info, marks, false, pointerSlots);
    }

    @Override
    public void emitCode(AMD64LIRAssembler lasm, LIRCall op) {
        switch (this) {
            case DIRECT_CALL: {
                callAlignment(lasm);
                if (op.marks != null) {
                    op.marks.put(XirMark.CALLSITE, lasm.tasm.recordMark(null, new Mark[0]));
                }
                directCall(lasm, op.target, op.info);
                break;
            }
            case INDIRECT_CALL: {
                callAlignment(lasm);
                if (op.marks != null) {
                    op.marks.put(XirMark.CALLSITE, lasm.tasm.recordMark(null, new Mark[0]));
                }
                CiRegister reg = lasm.asRegister(op.targetAddress());
                indirectCall(lasm, reg, op.target, op.info);
                break;
            }
            case NATIVE_CALL: {
                CiRegister reg = lasm.asRegister(op.targetAddress());
                indirectCall(lasm, reg, op.target, op.info);
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }
    }

    public void callAlignment(AMD64LIRAssembler lasm) {
        if (GraalOptions.AlignCallsForPatching) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = lasm.masm.codeBuffer.position();
            offset += lasm.target.arch.machineCodeCallDisplacementOffset;
            while (offset++ % lasm.target.wordSize != 0) {
                lasm.masm.nop();
            }
        }
    }

    public static void callStub(AMD64LIRAssembler lasm, CompilerStub stub, CiKind resultKind, LIRDebugInfo info, CiValue result, CiValue... args) {
        assert args.length == stub.inArgs.length;
        for (int i = 0; i < args.length; i++) {
            assert stub.inArgs[i].inCallerFrame();
            AMD64MoveOp.move(lasm, stub.inArgs[i].asOutArg(), args[i]);
        }

        directCall(lasm, stub.stubObject, info);

        if (result.isLegal()) {
            AMD64MoveOp.move(lasm, result, stub.outResult.asOutArg());
        }

        // Clear out parameters
        if (GraalOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                CiStackSlot inArg = stub.inArgs[i];
                CiStackSlot outArg = inArg.asOutArg();
                CiAddress dst = lasm.asAddress(outArg);
                lasm.masm.movptr(dst, 0);
            }
        }
    }

    public static void directCall(AMD64LIRAssembler lasm, Object target, LIRDebugInfo info) {
        int before = lasm.masm.codeBuffer.position();
        if (target instanceof CiRuntimeCall) {
            long maxOffset = lasm.compilation.compiler.runtime.getMaxCallTargetOffset((CiRuntimeCall) target);
            if (maxOffset != (int) maxOffset) {
                // offset might not fit a 32-bit immediate, generate an
                // indirect call with a 64-bit immediate
                CiRegister scratch = lasm.compilation.registerConfig.getScratchRegister();
                // TODO(cwi): we want to get rid of a generally reserved scratch register.
                lasm.masm.movq(scratch, 0L);
                lasm.masm.call(scratch);
            } else {
                lasm.masm.call();
            }
        } else {
            lasm.masm.call();
        }
        int after = lasm.masm.codeBuffer.position();
        lasm.tasm.recordDirectCall(before, after, asCallTarget(lasm, target), info);
        lasm.tasm.recordExceptionHandlers(after, info);
        lasm.masm.ensureUniquePC();
    }

    public static void directJmp(AMD64LIRAssembler lasm, Object target) {
        int before = lasm.masm.codeBuffer.position();
        lasm.masm.jmp(0, true);
        int after = lasm.masm.codeBuffer.position();
        lasm.tasm.recordDirectCall(before, after, asCallTarget(lasm, target), null);
        lasm.masm.ensureUniquePC();
    }

    public static void indirectCall(AMD64LIRAssembler lasm, CiRegister dst, Object target, LIRDebugInfo info) {
        int before = lasm.masm.codeBuffer.position();
        lasm.masm.call(dst);
        int after = lasm.masm.codeBuffer.position();
        lasm.tasm.recordIndirectCall(before, after, asCallTarget(lasm, target), info);
        lasm.tasm.recordExceptionHandlers(after, info);
        lasm.masm.ensureUniquePC();
    }

    private static Object asCallTarget(AMD64LIRAssembler lasm, Object o) {
        return lasm.compilation.compiler.runtime.asCallTarget(o);
    }

    public static void shouldNotReachHere(AMD64LIRAssembler lasm) {
        if (GraalOptions.GenAssertionCode) {
            directCall(lasm, CiRuntimeCall.Debug, null);
            lasm.masm.hlt();
        }
    }
}
