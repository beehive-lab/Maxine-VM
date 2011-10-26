/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.gen.LIRGenerator.DeoptimizationStub;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

/**
 * This class implements the x86-specific code generation for LIR.
 */
public final class AMD64LIRAssembler extends LIRAssembler {

    final CiTarget target;
    final AMD64MacroAssembler masm;

    public AMD64LIRAssembler(GraalCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (AMD64MacroAssembler) asm;
        target = compilation.compiler.target;
    }


    protected CiRegister asIntReg(CiValue value) {
        assert value.kind == CiKind.Int || value.kind == CiKind.Jsr;
        return asRegister(value);
    }

    protected CiRegister asLongReg(CiValue value) {
        assert value.kind == CiKind.Long;
        return asRegister(value);
    }

    protected CiRegister asObjectReg(CiValue value) {
        assert value.kind == CiKind.Object;
        return asRegister(value);
    }

    protected CiRegister asFloatReg(CiValue value) {
        assert value.kind == CiKind.Float;
        return asRegister(value);
    }

    protected CiRegister asDoubleReg(CiValue value) {
        assert value.kind == CiKind.Double;
        return asRegister(value);
    }

    protected CiRegister asRegister(CiValue value) {
        return value.asRegister();
    }

    protected int asIntConst(CiValue value) {
        assert value.kind.stackKind() == CiKind.Int && value.isConstant();
        return ((CiConstant) value).asInt();
    }

    /**
     * Most 64-bit instructions can only have 32-bit immediate operands, therefore this
     * method has the return type int and not long.
     */
    protected int asLongConst(CiValue value) {
        assert value.kind == CiKind.Long && value.isConstant();
        long c = ((CiConstant) value).asLong();
        if (!(NumUtil.isInt(c))) {
            throw Util.shouldNotReachHere();
        }
        return (int) c;
    }

    /**
     * Only null can be inlined in 64-bit instructions, therefore this
     * method has the return type int.
     */
    protected int asObjectConst(CiValue value) {
        assert value.kind == CiKind.Object && value.isConstant();
        if (value != CiConstant.NULL_OBJECT) {
            throw Util.shouldNotReachHere();
        }
        return 0;
    }

    /**
     * Floating point constants are embedded as data references into the code, and the
     * address of the constant is returned.
     */
    protected CiAddress asFloatConst(CiValue value) {
        assert value.kind == CiKind.Float && value.isConstant();
        return tasm.recordDataReferenceInCode((CiConstant) value);
    }

    /**
     * Floating point constants are embedded as data references into the code, and the
     * address of the constant is returned.
     */
    protected CiAddress asDoubleConst(CiValue value) {
        assert value.kind == CiKind.Double && value.isConstant();
        return tasm.recordDataReferenceInCode((CiConstant) value);
    }

    protected CiAddress asAddress(CiValue value) {
        if (value.isStackSlot()) {
            return compilation.frameMap().toStackAddress((CiStackSlot) value);
        }
        return (CiAddress) value;
    }


    @Override
    public void emitTraps() {
        for (int i = 0; i < GraalOptions.MethodEndBreakpointGuards; ++i) {
            masm.int3();
        }
    }

    @Override
    protected void emitAlignment() {
        masm.align(target.wordSize);
    }

    public static ArrayList<Object> keepAlive = new ArrayList<Object>();

    @Override
    public void emitDeoptizationStub(DeoptimizationStub stub) {
        // TODO(cwi): we want to get rid of a generally reserved scratch register.
        CiRegister scratch = compilation.registerConfig.getScratchRegister();

        masm.bind(stub.label);
        if (GraalOptions.CreateDeoptInfo && stub.deoptInfo != null) {
            masm.nop();
            keepAlive.add(stub.deoptInfo);
            AMD64MoveOp.move(this, scratch.asValue(), CiConstant.forObject(stub.deoptInfo));
            // TODO Why use scratch register here? Is it an implicit calling convention that the runtime function reads this register?
            directCall(CiRuntimeCall.SetDeoptInfo, stub.info);
        }
        int code;
        switch(stub.action) {
            case None:
                code = 0;
                break;
            case Recompile:
                code = 1;
                break;
            case InvalidateReprofile:
                code = 2;
                break;
            case InvalidateRecompile:
                code = 3;
                break;
            case InvalidateStopCompiling:
                code = 4;
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        if (code == 0) {
            // TODO Why throw an exception here for a value that was set explicitly some lines above?
            throw new RuntimeException();
        }
        masm.movq(scratch, code);
        // TODO Why use scratch register here? Is it an implicit calling convention that the runtime function reads this register?
        directCall(CiRuntimeCall.Deoptimize, stub.info);
        shouldNotReachHere();
    }


    public void callStub(CompilerStub stub, CiKind resultKind, LIRDebugInfo info, CiValue result, CiValue... args) {
        assert args.length == stub.inArgs.length;
        for (int i = 0; i < args.length; i++) {
            assert stub.inArgs[i].inCallerFrame();
            AMD64MoveOp.move(this, stub.inArgs[i].asOutArg(), args[i]);
        }

        directCall(stub.stubObject, info);

        if (result.isLegal()) {
            AMD64MoveOp.move(this, result, stub.outResult.asOutArg());
        }

        // Clear out parameters
        if (GraalOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                CiStackSlot inArg = stub.inArgs[i];
                CiStackSlot outArg = inArg.asOutArg();
                CiAddress dst = compilation.frameMap().toStackAddress(outArg);
                masm.movptr(dst, 0);
            }
        }
    }

    public void directCall(Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
        if (target instanceof CiRuntimeCall) {
            long maxOffset = compilation.compiler.runtime.getMaxCallTargetOffset((CiRuntimeCall) target);
            if (maxOffset != (int) maxOffset) {
                // offset might not fit a 32-bit immediate, generate an
                // indirect call with a 64-bit immediate
                CiRegister scratch = compilation.registerConfig.getScratchRegister();
                // TODO(cwi): we want to get rid of a generally reserved scratch register.
                masm.movq(scratch, 0L);
                masm.call(scratch);
            } else {
                masm.call();
            }
        } else {
            masm.call();
        }
        int after = masm.codeBuffer.position();
        tasm.recordDirectCall(before, after, asCallTarget(target), info);
        tasm.recordExceptionHandlers(after, info);
        masm.ensureUniquePC();
    }

    public void directJmp(Object target) {
        int before = masm.codeBuffer.position();
        masm.jmp(0, true);
        int after = masm.codeBuffer.position();
        tasm.recordDirectCall(before, after, asCallTarget(target), null);
        masm.ensureUniquePC();
    }

    public void indirectCall(CiRegister dst, Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
        masm.call(dst);
        int after = masm.codeBuffer.position();
        tasm.recordIndirectCall(before, after, asCallTarget(target), info);
        tasm.recordExceptionHandlers(after, info);
        masm.ensureUniquePC();
    }

    private Object asCallTarget(Object o) {
        return compilation.compiler.runtime.asCallTarget(o);
    }

    protected void stop(String msg) {
        if (GraalOptions.GenAssertionCode) {
            // TODO: pass a pointer to the message
            directCall(CiRuntimeCall.Debug, null);
            masm.hlt();
        }
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }
}
