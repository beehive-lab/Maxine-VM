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

import static com.sun.cri.ci.CiRegister.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.ConditionFlag;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.gen.LIRGenerator.DeoptimizationStub;
import com.oracle.max.graal.compiler.lir.FrameMap.StackBlock;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.Scale;
import com.sun.cri.ci.CiTargetMethod.JumpTable;
import com.sun.cri.xir.*;

/**
 * This class implements the x86-specific code generation for LIR.
 */
public final class AMD64LIRAssembler extends LIRAssembler {

    private static final Object[] NO_PARAMS = new Object[0];

    final CiTarget target;
    final AMD64MacroAssembler masm;
    final CiRegister rscratch1;

    public AMD64LIRAssembler(GraalCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (AMD64MacroAssembler) asm;
        target = compilation.compiler.target;
        rscratch1 = compilation.registerConfig.getScratchRegister();
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
    protected int initialFrameSizeInBytes() {
        return frameMap.frameSize();
    }

    @Override
    protected void emitMonitorAddress(int monitor, CiValue dst) {
        CiStackSlot slot = frameMap.toMonitorBaseStackAddress(monitor);
        masm.leaq(dst.asRegister(), new CiAddress(slot.kind, AMD64.rsp.asValue(), slot.index() * target.arch.wordSize));
    }

    @Override
    protected void emitBreakpoint() {
        masm.int3();
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, CiValue dst) {
        masm.leaq(dst.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    private void swapReg(CiRegister a, CiRegister b) {
        masm.xchgptr(a, b);
    }

    @Override
    public void emitTraps() {
        for (int i = 0; i < GraalOptions.MethodEndBreakpointGuards; ++i) {
            masm.int3();
        }
    }

    @Override
    protected void emitReadPrefetch(CiValue src) {
        CiAddress addr = (CiAddress) src;
        // Checkstyle: off
        switch (GraalOptions.ReadPrefetchInstr) {
            case 0  : masm.prefetchnta(addr); break;
            case 1  : masm.prefetcht0(addr); break;
            case 2  : masm.prefetcht2(addr); break;
            default : throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
    }


    @Override
    protected void emitTableSwitch(LIRTableSwitch op) {
        CiRegister value = op.operand(0).asRegister();
        final Buffer buf = masm.codeBuffer;

        // Compare index against jump table bounds
        int highKey = op.lowKey + op.targets.length - 1;
        if (op.lowKey != 0) {
            // subtract the low value from the switch value
            masm.subl(value, op.lowKey);
            masm.cmpl(value, highKey - op.lowKey);
        } else {
            masm.cmpl(value, highKey);
        }

        // Jump to default target if index is not within the jump table
        masm.jcc(ConditionFlag.above, op.defaultTarget.label());

        // Set scratch to address of jump table
        int leaPos = buf.position();
        masm.leaq(rscratch1, new CiAddress(target.wordKind, InstructionRelative.asValue(), 0));
        int afterLea = buf.position();

        // Load jump table entry into scratch and jump to it
        masm.movslq(value, new CiAddress(CiKind.Int, rscratch1.asValue(), value.asValue(), Scale.Times4, 0));
        masm.addq(rscratch1, value);
        masm.jmp(rscratch1);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            masm.nop(4 - (buf.position() & 0x3));
        }

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos);
        masm.leaq(rscratch1, new CiAddress(target.wordKind, InstructionRelative.asValue(), jumpTablePos - afterLea));
        buf.setPosition(jumpTablePos);

        // Emit jump table entries
        for (LIRBlock target : op.targets) {
            Label label = target.label();
            int offsetToJumpTableBase = buf.position() - jumpTablePos;
            if (label.isBound()) {
                int imm32 = label.position() - jumpTablePos;
                buf.emitInt(imm32);
            } else {
                label.addPatchAt(buf.position());

                buf.emitByte(0); // psuedo-opcode for jump table entry
                buf.emitShort(offsetToJumpTableBase);
                buf.emitByte(0); // padding to make jump table entry 4 bytes wide
            }
        }

        JumpTable jt = new JumpTable(jumpTablePos, op.lowKey, highKey, 4);
        tasm.targetMethod.addAnnotation(jt);
    }

    @Override
    protected void emitCompareAndSwap(LIRInstruction op) {
        CiAddress address = new CiAddress(CiKind.Object, op.operand(0), 0);
        CiRegister newval = op.operand(2).asRegister();
        CiRegister cmpval = op.operand(1).asRegister();
        assert cmpval == AMD64.rax : "wrong register";
        assert newval != null : "new val must be register";
        assert cmpval != newval : "cmp and new values must be in different registers";
        assert cmpval != address.base() : "cmp and addr must be in different registers";
        assert newval != address.base() : "new value and addr must be in different registers";
        assert cmpval != address.index() : "cmp and addr must be in different registers";
        assert newval != address.index() : "new value and addr must be in different registers";
        if (target.isMP) {
            masm.lock();
        }
        switch (op.operand(1).kind) {
            case Int:
                masm.cmpxchgl(newval, address);
                break;
            case Long:
            case Object:
                masm.cmpxchgq(newval, address);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitSignificantBitOp(LegacyOpcode code, CiValue src, CiValue dst) {
        assert dst.isRegister();
        CiRegister result = dst.asRegister();
        masm.xorq(result, result);
        masm.notq(result);
        if (src.isRegister()) {
            CiRegister value = src.asRegister();
            assert value != result;
            switch (code) {
                case Msb: masm.bsrq(result, value); break;
                case Lsb: masm.bsfq(result, value); break;
                default: throw Util.shouldNotReachHere();
            }
        } else {
            CiAddress laddr = asAddress(src);
            switch (code) {
                case Msb: masm.bsrq(result, laddr); break;
                case Lsb: masm.bsfq(result, laddr); break;
                default: throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void emitAlignment() {
        masm.align(target.wordSize);
    }

    @Override
    protected void emitLea(CiValue src, CiValue dest) {
        CiRegister reg = dest.asRegister();
        masm.leaq(reg, asAddress(src));
    }

    @Override
    protected void emitNullCheck(CiValue src, LIRDebugInfo info) {
        assert src.isRegister();
        tasm.recordImplicitException(codePos(), info);
        masm.nullCheck(src.asRegister());
    }

    @Override
    protected void emitMemoryBarriers(int barriers) {
        masm.membar(barriers);
    }

    public static ArrayList<Object> keepAlive = new ArrayList<Object>();

    @Override
    public void emitDeoptizationStub(DeoptimizationStub stub) {
        masm.bind(stub.label);
        if (GraalOptions.CreateDeoptInfo && stub.deoptInfo != null) {
            masm.nop();
            keepAlive.add(stub.deoptInfo);
            AMD64MoveOp.move(this, rscratch1.asValue(), CiConstant.forObject(stub.deoptInfo));
            // TODO Why use rsratch1 here? Is it an implicit calling convention that the runtime function reads this register?
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
        masm.movq(rscratch1, code);
        // TODO Why use rsratch1 here? Is it an implicit calling convention that the runtime function reads this register?
        directCall(CiRuntimeCall.Deoptimize, stub.info);
        shouldNotReachHere();
    }


    public void callStub(XirTemplate stub, LIRDebugInfo info, CiValue result, CiValue... args) {
        callStubHelper(compilation.compiler.lookupStub(stub), stub.resultOperand.kind, info, result, args);
    }

    public void callStub(CompilerStub stub, LIRDebugInfo info, CiValue result, CiValue... args) {
        callStubHelper(stub, stub.resultKind, info, result, args);
    }

    private void callStubHelper(CompilerStub stub, CiKind resultKind, LIRDebugInfo info, CiValue result, CiValue... args) {
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
                masm.movq(rscratch1, 0L);
                masm.call(rscratch1);
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
