/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.c1x.target.riscv64;

import com.oracle.max.asm.Buffer;
import com.oracle.max.asm.Label;
import com.oracle.max.asm.NumUtil;
import com.oracle.max.asm.target.riscv64.RISCV64;
import com.oracle.max.asm.target.riscv64.RISCV64Address;
import com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler;
import com.oracle.max.criutils.TTY;
import com.sun.c1x.C1XCompilation;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.asm.TargetMethodAssembler;
import com.sun.c1x.gen.LIRGenerator.DeoptimizationStub;
import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.ir.Condition;
import com.sun.c1x.ir.Infopoint;
import com.sun.c1x.lir.FrameMap.StackBlock;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.CompilerStub;
import com.sun.c1x.util.Util;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.JumpTable;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.xir.CiXirAssembler.*;
import com.sun.cri.xir.XirSnippet;
import com.sun.cri.xir.XirTemplate;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.Word;
import com.sun.max.vm.compiler.CompilationBroker;
import com.sun.max.vm.runtime.FatalError;

import java.util.Map;

import static com.sun.cri.ci.CiCallingConvention.Type.RuntimeCall;
import static com.sun.cri.ci.CiValue.IllegalValue;

public final class RISCV64LIRAssembler extends LIRAssembler {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final CiRegister SHIFTCount = RISCV64.a2;

    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    final CiTarget target;
    final RISCV64MacroAssembler masm;
    final CiRegister scratchRegister;
    final CiRegister scratchRegister1;

    public RISCV64LIRAssembler(C1XCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (RISCV64MacroAssembler) tasm.asm;
        target = compilation.target;
        scratchRegister = compilation.registerConfig.getScratchRegister();
        scratchRegister1 = compilation.registerConfig.getScratchRegister1();
    }

    private CiAddress asAddress(CiValue value) {
        if (value.isAddress()) {
            return (CiAddress) value;
        }
        assert value.isStackSlot();
        return compilation.frameMap().toStackAddress((CiStackSlot) value);
    }

    @Override
    protected void emitOsrEntry() {
        if (true) {
            throw Util.unimplemented();
        }
    }

    @Override
    protected int initialFrameSizeInBytes() {
        return frameMap.frameSize();
    }

    @Override
    protected void emitReturn(CiValue result) {
        // TODO: Consider adding safepoint polling at return!
        masm.ret();
    }

    @Override
    protected void emitInfopoint(CiValue dst, LIRDebugInfo info, Infopoint.Op op) {
        switch (op) {
            case HERE:
                tasm.recordSafepoint(codePos(), info);
                masm.auipc(dst.asRegister(), 0);
                break;
            case UNCOMMON_TRAP:
                directCall(CiRuntimeCall.Deoptimize, info);
                break;
            case INFO:
                tasm.recordSafepoint(codePos(), info);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitMonitorAddress(int monitor, CiValue dst) {
        CiStackSlot slot = frameMap.toMonitorBaseStackAddress(monitor);
        masm.leaq(dst.asRegister(), new CiAddress(slot.kind, RISCV64.sp.asValue(), slot.index() * target.arch.wordSize));
    }

    @Override
    protected void emitPause() {
        masm.pause();
    }

    @Override
    public void emitGetTicks(CiValue result) {
        throw FatalError.unimplemented("RISCV64LIRGenerator.emitGetTicks");
    }

    @Override
    public void emitGetCpuID(CiValue result) {
        throw FatalError.unimplemented("RISCV64LIRGenerator.emitGetCpuID");
    }

    @Override
    protected void emitBreakpoint() {
        masm.ebreak();
    }

    @Override
    protected void emitIfBit(CiValue address, CiValue bitNo) {
        assert false : "emitIfBit RISCV64LIRAssembler";
        masm.crashme();
        masm.insertForeverLoop();
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, CiValue dst) {
        masm.leaq(dst.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    private void moveRegs(CiRegister fromReg, CiRegister toReg) {
        if (fromReg != toReg) {
            masm.mov(toReg, fromReg);
        }
    }

    @Override
    public void emitTraps() {
        for (int i = 0; i < C1XOptions.MethodEndBreakpointGuards; ++i) {
            masm.ebreak();
        }
        masm.nop(8);
    }

    private void const2reg(CiRegister dst, float constant) {
        masm.mov64BitConstant(scratchRegister, Float.floatToRawIntBits(constant));
        masm.fmov(32, dst, scratchRegister);
    }

    private void const2reg(CiRegister dst, double constant) {
        masm.mov64BitConstant(scratchRegister, Double.doubleToRawLongBits(constant));
        masm.fmov(64, dst, scratchRegister);
    }

    @Override
    protected void const2reg(CiValue src, CiValue dest, LIRDebugInfo info) {
        assert src.isConstant();
        assert dest.isRegister();
        CiConstant c = (CiConstant) src;

        // Checkstyle: off
        switch (c.kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Int:
                masm.mov(dest.asRegister(), c.asInt());
                break;
            case Long:
                masm.mov64BitConstant(dest.asRegister(), c.asLong());
                break;
            case Object:
                movoop(dest.asRegister(), c);
                break;
            case Float:
                const2reg(dest.asRegister(), c.asFloat());
                break;
            case Double:
                const2reg(dest.asRegister(), c.asDouble());
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
    }

    private CiKind setScratchRegister(CiConstant constant) {
        CiKind kind = CiKind.Int;
        switch (constant.kind) {
            case Boolean:
            case Byte:
                masm.movByte(scratchRegister, constant.asInt() & 0xFF);
                break;
            case Char:
            case Short:
                masm.movShort(scratchRegister, constant.asInt() & 0xFFFF);
                break;
            case Jsr:
            case Int:
                masm.mov(scratchRegister, constant.asInt());
                break;
            case Float:
                masm.mov64BitConstant(scratchRegister, Float.floatToRawIntBits(constant.asFloat()));
                break;
            case Object:
                movoop(scratchRegister, constant);
                kind = CiKind.Long;
                break;
            case Long:
                masm.mov64BitConstant(scratchRegister, constant.asLong());
                kind = CiKind.Long;
                break;
            case Double:
                masm.mov64BitConstant(scratchRegister, Double.doubleToRawLongBits(constant.asDouble()));
                kind = CiKind.Long;
                break;
            default:
                throw Util.shouldNotReachHere("Unknown constant kind for const2stack: " + constant.kind);
        }
        return kind;
    }

    @Override
    protected void const2stack(CiValue src, CiValue dst) {
        assert src.isConstant();
        assert dst.isStackSlot();
        CiStackSlot slot = (CiStackSlot) dst;
        CiConstant c = (CiConstant) src;

        CiAddress address = frameMap.toStackAddress(slot);
        CiKind kind = setScratchRegister(c);
        masm.store(scratchRegister, address, kind);
    }

    @Override
    protected void const2mem(CiValue src, CiValue dst, CiKind kind, LIRDebugInfo info) {
        assert src.isConstant();
        assert dst.isAddress();
        CiConstant constant = (CiConstant) src;
        CiAddress addr = asAddress(dst);
        CiKind storeKind = setScratchRegister(constant);
        masm.store(scratchRegister, addr, storeKind);
        if (info != null) {
            tasm.recordImplicitException(codePos() - 4, info);
        }
    }

    @Override
    protected void reg2reg(CiValue src, CiValue dest) {
        assert src.isRegister();
        assert dest.isRegister();
        if (dest.kind.isFloat()) {
            masm.fmov(32, dest.asRegister(), src.asRegister());
        } else if (dest.kind.isDouble()) {
            masm.fmov(64, dest.asRegister(), src.asRegister());
        } else {
            moveRegs(src.asRegister(), dest.asRegister());
        }
    }

    @Override
    protected void reg2stack(CiValue src, CiValue dst, CiKind kind) {
        assert src.isRegister();
        assert dst.isStackSlot();
        CiAddress addr = frameMap.toStackAddress((CiStackSlot) dst);
        masm.store(src.asRegister(), addr, src.kind.stackKind());
    }

    @Override
    protected void reg2mem(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        CiAddress destAddress = (CiAddress) dest;
        masm.store(src.asRegister(), destAddress, kind);
        if (info != null) {
            tasm.recordImplicitException(codePos() - 4, info);
        }
    }

    @Override
    protected void stack2reg(CiValue src, CiValue dest, CiKind kind) {
        assert src.isStackSlot();
        assert dest.isRegister();
        CiAddress addr = frameMap.toStackAddress((CiStackSlot) src);
        masm.load(dest.asRegister(), addr, dest.kind.stackKind());
    }

    @Override
    protected void mem2mem(CiValue src, CiValue dest, CiKind kind) {
        masm.load(masm.scratchRegister, (CiAddress) src, kind);
        masm.store(masm.scratchRegister, (CiAddress) dest, kind);
    }

    @Override
    protected void mem2stack(CiValue src, CiValue dest, CiKind kind) {
        assert false : "mem2stack not implemented";
    }

    @Override
    protected void stack2stack(CiValue src, CiValue dest, CiKind kind) {
        masm.load(scratchRegister, frameMap.toStackAddress((CiStackSlot) src), src.kind);
        masm.store(scratchRegister, frameMap.toStackAddress((CiStackSlot) dest), src.kind);
    }

    @Override
    protected void mem2reg(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        assert src.isAddress();
        assert dest.isRegister() : "dest=" + dest;
        CiAddress addr = (CiAddress) src;
        masm.load(dest.asRegister(), addr, kind);
        if (info != null) {
            tasm.recordImplicitException(codePos() - 4, info);
        }
    }

    @Override
    protected void emitReadPrefetch(CiValue src) {
        assert false : "emitReadPrefetch unimplemented!";
    }

    @Override
    protected void emitOp3(LIROp3 op) {
        // Checkstyle: off
        switch (op.code) {
            case Idiv:
            case Irem:
                arithmeticDiv(32, op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Iudiv:
            case Iurem:
                arithmeticUdiv(32, op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Ldiv:
            case Lrem:
                arithmeticDiv(64, op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Ludiv:
            case Lurem:
                arithmeticUdiv(64, op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
    }

    private boolean assertEmitBranch(LIRBranch op) {
        assert op.block() == null || op.block().label() == op.label() : "wrong label";
        if (op.block() != null) {
            branchTargetBlocks.add(op.block());
        }
        if (op.unorderedBlock() != null) {
            branchTargetBlocks.add(op.unorderedBlock());
        }
        return true;
    }

    private boolean assertEmitTableSwitch(LIRTableSwitch op) {
        assert op.defaultTarget != null;
        branchTargetBlocks.add(op.defaultTarget);
        for (BlockBegin target : op.targets) {
            assert target != null;
            branchTargetBlocks.add(target);
        }
        return true;
    }

    @Override
    protected void emitTableSwitch(LIRTableSwitch op) {
        assert assertEmitTableSwitch(op);
        CiRegister value = op.value().asRegister();
        final Buffer buf = masm.codeBuffer;

        // Compare index against jump table bounds
        masm.mov64BitConstant(scratchRegister, op.lowKey);

        // Check if index is lower than lowMatch and branch
        masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.LT, value, scratchRegister, op.defaultTarget.label());

        int highKey = op.lowKey + op.targets.length - 1;

        // index = index - lowMatch
        masm.sub(value, value, scratchRegister);

        masm.mov64BitConstant(scratchRegister, highKey - op.lowKey);

        // Check if index is higher than highMatch and branch
        masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.GT, value, scratchRegister, op.defaultTarget.label());

        // Set scratch to address of jump table
        int adrPos = buf.position();
        masm.auipc(scratchRegister, 0);
        masm.add(64, scratchRegister, scratchRegister, 0);

        // Load jump table entry into value and jump to it
        masm.slli(value, value, 2); // Shift left by 2 to make offset in bytes
        masm.add(value, scratchRegister, value);
        masm.jalr(RISCV64.zero, value, 0);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            masm.nop(4 - (buf.position() & 0x3));
        }

        int jumpTablePos = buf.position();
        buf.setPosition(adrPos + 4);
        masm.add(64, scratchRegister, scratchRegister, jumpTablePos - adrPos);
        buf.setPosition(jumpTablePos);

        // Emit jump table entries
        for (BlockBegin target : op.targets) {
            Label label = target.label();
            if (label.isBound()) {
                masm.jal(RISCV64.zero, label.position() - jumpTablePos);
            } else {
                label.addPatchAt(buf.position());
                buf.emitByte(RISCV64MacroAssembler.PatchLabelKind.TABLE_SWITCH.encoding);
                buf.emitByte(0);
                buf.emitShort(0);
            }
        }
        JumpTable jt = new JumpTable(jumpTablePos, op.lowKey, highKey, 4);
        tasm.targetMethod.addAnnotation(jt);
    }

    @Override
    protected void emitBranch(LIRBranch op) {
        assert assertEmitBranch(op);
        if (op.cond() == Condition.TRUE) {
            masm.b(op.label());
            if (op.info != null) {
                tasm.recordImplicitException(codePos() - 4, op.info); // ADDED EXCEPTION
            }
        } else {
            RISCV64MacroAssembler.ConditionFlag acond;
            if (op.code == LIROpcode.CondFloatBranch) {
                assert op.unorderedBlock() != null : "must have unordered successor";
                // Check if NV flag in fflags is set to 1. If it is, then one of the operands from emitCompare was NaN.
                // Should go to the 'false' branch in this case
                masm.csrrci(scratchRegister, 0x001, 0b10000); // Read and clear NV flag
                masm.andi(scratchRegister, scratchRegister, 0b10000);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.NE, scratchRegister, RISCV64.zero, op.unorderedBlock().label());

                switch (op.cond()) {
                    case EQ:
                        acond = RISCV64MacroAssembler.ConditionFlag.EQ;
                        break;
                    case NE:
                        acond = RISCV64MacroAssembler.ConditionFlag.NE;
                        break;
                    case LT:
                        acond = RISCV64MacroAssembler.ConditionFlag.LT;
                        break;
                    case LE:
                        acond = RISCV64MacroAssembler.ConditionFlag.LE;
                        break;
                    case GE:
                        acond = RISCV64MacroAssembler.ConditionFlag.GE;
                        break;
                    case GT:
                        acond = RISCV64MacroAssembler.ConditionFlag.GT;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                acond = convertConditionEmitBranch(op.cond());
            }
            masm.branchConditionally(acond, RISCV64.x31, RISCV64.zero, op.label());
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        CiValue src = op.operand();
        CiValue dest = op.result();
        switch (op.opcode) {
            case I2L:
                masm.addiw(dest.asRegister(), src.asRegister(), 0);
                break;
            case L2I:
                masm.mov64BitConstant(scratchRegister, 0xFFFFFFFFL);
                masm.and(dest.asRegister(), src.asRegister(), scratchRegister);
                break;
            case I2B:
                masm.andi(dest.asRegister(), src.asRegister(), 0x0FF);
                masm.slli(dest.asRegister(), dest.asRegister(), 24);
                masm.addiw(dest.asRegister(), dest.asRegister(), 0);
                masm.srli(dest.asRegister(), dest.asRegister(), 24);
                masm.addiw(dest.asRegister(), dest.asRegister(), 0);
                break;
            case I2C:
                masm.mov32BitConstant(scratchRegister, 0xFFFF);
                masm.and(dest.asRegister(), src.asRegister(), scratchRegister);
                break;
            case I2S:
                masm.mov32BitConstant(scratchRegister, 0xFFFF);
                masm.and(dest.asRegister(), src.asRegister(), scratchRegister);
                masm.slli(dest.asRegister(), dest.asRegister(), 16);
                masm.addiw(dest.asRegister(), dest.asRegister(), 0);
                masm.srli(dest.asRegister(), dest.asRegister(), 16);
                masm.addiw(dest.asRegister(), dest.asRegister(), 0);
                break;
            case F2D:
                masm.fcvtds(dest.asRegister(), src.asRegister());
                break;
            case D2F:
                masm.fcvtsd(dest.asRegister(), src.asRegister());
                break;
            case I2F:
                masm.fcvtsw(dest.asRegister(), src.asRegister());
                break;
            case I2D:
                masm.fcvtdw(dest.asRegister(), src.asRegister());
                break;
            case F2I: {
                // Check if src is Float.NaN. If so, then move 0 to dest
                masm.mov32BitConstant(RISCV64.x30, Float.floatToRawIntBits(Float.NaN));
                masm.fmvxw(scratchRegister1, src.asRegister());
                masm.mov(dest.asRegister(), RISCV64.zero);
                masm.beq(RISCV64.x30, scratchRegister1, 2 * RISCV64MacroAssembler.INSTRUCTION_SIZE);
                masm.fcvtwsRTZ(dest.asRegister(), src.asRegister());
                break;
            }
            case D2I: {
                // Check if src is Double.NaN. If so, then move 0 to dest
                masm.mov64BitConstant(RISCV64.x30, Double.doubleToRawLongBits(Double.NaN));
                masm.fmvxd(scratchRegister1, src.asRegister());
                masm.mov(dest.asRegister(), RISCV64.zero);
                masm.beq(RISCV64.x30, scratchRegister1, 2 * RISCV64MacroAssembler.INSTRUCTION_SIZE);
                masm.fcvtwdRTZ(dest.asRegister(), src.asRegister());
                break;
            }
            case L2F:
                masm.fcvtsl(dest.asRegister(), src.asRegister());
                break;
            case L2D:
                masm.fcvtdl(dest.asRegister(), src.asRegister());
                break;
            case F2L: {
                // Check if src is Float.NaN. If so, then move 0 to dest
                masm.mov32BitConstant(RISCV64.x30, Float.floatToRawIntBits(Float.NaN));
                masm.fmvxw(scratchRegister1, src.asRegister());
                masm.mov(dest.asRegister(), RISCV64.zero);
                masm.beq(RISCV64.x30, scratchRegister1, 2 * RISCV64MacroAssembler.INSTRUCTION_SIZE);
                masm.fcvtlsRTZ(dest.asRegister(), src.asRegister());
                break;
            }
            case D2L: {
                // Check if src is Double.NaN. If so, then move 0 to dest
                masm.mov64BitConstant(RISCV64.x30, Double.doubleToRawLongBits(Double.NaN));
                masm.fmvxd(scratchRegister1, src.asRegister());
                masm.mov(dest.asRegister(), RISCV64.zero);
                masm.beq(RISCV64.x30, scratchRegister1, 2 * RISCV64MacroAssembler.INSTRUCTION_SIZE);
                masm.fcvtldRTZ(dest.asRegister(), src.asRegister());
                break;
            }
            case MOV_I2F:
                masm.fmvwx(dest.asRegister(), src.asRegister());
                break;
            case MOV_L2D:
                masm.fmvdx(dest.asRegister(), src.asRegister());
                break;
            case MOV_F2I:
                masm.fmvxw(dest.asRegister(), src.asRegister());
                break;
            case MOV_D2L:
                masm.fmvxd(dest.asRegister(), src.asRegister());
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        RISCV64Address address = RISCV64Address.createBaseRegisterOnlyAddress(op.address().asRegister());
        CiRegister newval = op.newValue().asRegister();
        CiRegister cmpval = op.expectedValue().asRegister();
        assert newval != null : "new val must be register";
        assert cmpval != newval : "cmp and new values must be in different registers";
        assert cmpval != address.base() : "cmp and addr must be in different registers";
        assert newval != address.base() : "new value and addr must be in different registers";
        assert cmpval != address.index() : "cmp and addr must be in different registers";
        assert newval != address.index() : "new value and addr must be in different registers";

        if (op.code == LIROpcode.CasInt) {
            masm.cas(32, newval, cmpval, address);
        } else {
            assert op.code == LIROpcode.CasLong || op.code == LIROpcode.CasObj;
            masm.cas(64, newval, cmpval, address);
        }
    }

    @Override
    protected void emitConditionalMove(Condition condition, CiValue opr1, CiValue opr2, CiValue result) {
        RISCV64MacroAssembler.ConditionFlag acond = convertConditionEmitBranch(condition);
        RISCV64MacroAssembler.ConditionFlag ncond = acond.negate();

        CiValue def = opr1; // assume left operand as default
        CiValue other = opr2;

        if (opr2.isRegister() && opr2.asRegister() == result.asRegister()) {
            // if the right operand is already in the result register, then use it as the default
            def = opr2;
            other = opr1;
            // and flip the condition
            RISCV64MacroAssembler.ConditionFlag tcond = acond;
            acond = ncond;
            ncond = tcond;
        }

        if (def.isRegister()) {
            reg2reg(def, result);
        } else if (def.isStackSlot()) {
            stack2reg(def, result, result.kind);
        } else {
            assert def.isConstant();
            const2reg(def, result, null);
        }

        if (!other.isConstant()) {
            // optimized version that does not require a branch
            if (other.isRegister()) {
                assert other.asRegister() != result.asRegister() : "other already overwritten by previous move";
                if (other.kind.isInt()) {
                    masm.cmov(32, result.asRegister(), other.asRegister(), result.asRegister(), ncond);
                } else {
                    masm.cmov(64, result.asRegister(), other.asRegister(), result.asRegister(), ncond);
                }
            } else {
                assert other.isStackSlot();
                CiStackSlot otherSlot = (CiStackSlot) other;
                masm.load(scratchRegister, frameMap.toStackAddress(otherSlot), other.kind);
                masm.cmov(64, result.asRegister(), scratchRegister, result.asRegister(), ncond);
            }
        } else {
            // conditional move not available, use emit a branch and move
            Label skip = new Label();
            masm.branchConditionally(acond, RISCV64.x31, RISCV64.zero, skip);
            if (other.isRegister()) {
                reg2reg(other, result);
            } else if (other.isStackSlot()) {
                stack2reg(other, result, result.kind);
            } else {
                assert other.isConstant();
                const2reg(other, result, null);
            }
            masm.bind(skip);
        }
    }

    @Override
    protected void emitArithOp(LIROpcode code, CiValue left, CiValue right, CiValue dest, LIRDebugInfo info) {
        assert info == null : "should never be used :  idiv/irem and ldiv/lrem not handled by this method";
        assert Util.archKindsEqual(left.kind, right.kind) || (left.kind == CiKind.Long && right.kind == CiKind.Int)
                : code.toString() + " left arch is " + left.kind + " and right arch is " + right.kind;
        assert left.equals(dest) : "left and dest must be equal";
        CiKind kind = left.kind;

        if (left.isRegister()) {
            final int size = kind.isInt() || kind.isFloat() ? 32 : 64;
            CiRegister lreg = left.asRegister();
            CiRegister rreg;
            if (right.isConstant() && (kind.isInt() || kind.isLong())) {
                assert kind.isInt() || kind.isLong();
                final long delta = ((CiConstant) right).asLong();
                switch (code) {
                    case Add:
                        masm.add(size, dest.asRegister(), lreg, delta);
                        break;
                    case Sub:
                        masm.sub(size, dest.asRegister(), lreg, delta);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
                return;
            }
            if (right.isRegister()) {
                rreg = right.asRegister();
            } else if (right.isStackSlot()) {
                CiAddress raddr = frameMap.toStackAddress((CiStackSlot) right);
                if (kind.isInt() || kind.isLong()) {
                    masm.load(scratchRegister, raddr, kind);
                    rreg = scratchRegister;
                } else {
                    assert kind.isFloat() || kind.isDouble();
                    masm.load(RISCV64.f30, raddr, kind);
                    rreg = RISCV64.f30;
                }
            } else {
                assert right.isConstant();
                assert kind.isFloat() || kind.isDouble();
                if (kind.isFloat()) {
                    tasm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) right).asFloat()));
                } else {
                    tasm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) right).asDouble()));
                }
                masm.auipc(scratchRegister, 0); // this gets patched by RISCV64InstructionDecoder.patchRelativeInstruction
                masm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                rreg = RISCV64.f30;
                masm.load(rreg, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister), kind);
            }
            if (kind.isInt() || kind.isLong()) {
                switch (code) {
                    case Add:
                        masm.add(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Sub:
                        masm.sub(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Mul:
                        masm.mul(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Rem:
                        masm.rem(size, dest.asRegister(), lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                assert kind.isFloat() || kind.isDouble();
                assert rreg.isFpu() : "must be floating point register";
                switch (code) {
                    case Add:
                        masm.fadd(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Sub:
                        masm.fsub(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Mul:
                        masm.fmul(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Div:
                        masm.fdiv(size, dest.asRegister(), lreg, rreg);
                        break;
                    case Rem:
                        masm.frem(size, dest.asRegister(), lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
        } else {
            assert kind.isInt();
            CiAddress laddr = asAddress(left);
            masm.load(scratchRegister1, laddr, kind);
            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add:
                        masm.add(32, scratchRegister1, scratchRegister1, rreg);
                        break;
                    case Sub:
                        masm.sub(32, scratchRegister1, scratchRegister1, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();

                }
            } else {
                assert right.isConstant();
                masm.mov(scratchRegister, ((CiConstant) right).asInt());
                switch (code) {
                    case Add:
                        masm.add(32, scratchRegister1, scratchRegister, scratchRegister1);
                        break;
                    case Sub:
                        masm.sub(32, scratchRegister1, scratchRegister, scratchRegister1);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            masm.store(scratchRegister1, laddr, kind);
        }
    }

    @Override
    protected void emitIntrinsicOp(LIROpcode code, CiValue value, CiValue unused, CiValue dest, LIROp2 op) {
        assert value.kind.isDouble();
        switch (code) {
            case Abs:
                masm.fabs(64, dest.asRegister(), value.asRegister());
                break;
            case Sqrt:
                masm.fsqrt(64, dest.asRegister(), value.asRegister());
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    // TODO (fz): Optimize using logical immediates where possible
    @Override
    protected void emitLogicOp(LIROpcode code, CiValue left, CiValue right, CiValue dst) {
        assert left.isRegister();
        assert dst.isRegister();
        int size = left.kind.isInt() ? 32 : 64;
        CiRegister reg = left.asRegister();
        CiRegister dest = dst.asRegister();
        CiRegister rright;
        if (right.isStackSlot()) {
            // added support for stack operands
            CiAddress raddr = frameMap.toStackAddress((CiStackSlot) right);
            masm.load(scratchRegister, raddr, right.kind);
            rright = scratchRegister;
        } else if (right.isConstant()) {
            if (left.kind.isInt()) {
                int val = ((CiConstant) right).asInt();
                masm.mov32BitConstant(scratchRegister, val);
            } else {
                long val = ((CiConstant) right).asLong();
                masm.mov64BitConstant(scratchRegister, val);
            }
            rright = scratchRegister;
        } else {
            rright = right.asRegister();
        }
        switch (code) {
            case LogicAnd:
                masm.and(dest, reg, rright);
                break;
            case LogicOr:
                masm.or(dest, reg, rright);
                break;
            case LogicXor:
                masm.xor(dest, reg, rright);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    void arithmeticDiv(int size, LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() || right.isConstant() : "right must be register or constant";
        assert result.isRegister() : "result must be register";
        assert size == 32 || size == 64 : "size must be 32 or 64";

        CiRegister numerator = left.asRegister();
        CiRegister quotient = result.asRegister();

        if (right.isConstant()) {
            Util.shouldNotReachHere("cwi: I assume this is dead code, notify me if I'm wrong...");
        } else {
            CiRegister denominator = right.asRegister();
            Label continuation = new Label();
            if (C1XOptions.GenSpecialDivChecks) {
                // check for special case of MIN_VALUE / -1
                Label normalCase = new Label();
                masm.mov(scratchRegister, size == 32 ? Integer.MIN_VALUE : Long.MIN_VALUE);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.NE, numerator, scratchRegister, normalCase);
                if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                    // prepare scratch for possible special case where remainder = 0
                    masm.mov(quotient, 0);
                }
                masm.mov(scratchRegister, -1);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, denominator, scratchRegister, continuation);
                masm.bind(normalCase);
            }
            int offset = masm.insertDivByZeroCheck(denominator);
            tasm.recordImplicitException(offset, info);
            if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                if (quotient == numerator || quotient == denominator) {
                    quotient = scratchRegister;
                }
                masm.div(size, quotient, numerator, denominator);
                masm.mul(size, quotient, quotient, denominator);
                masm.sub(size, result.asRegister(), numerator, quotient);
            } else {
                assert code == LIROpcode.Idiv || code == LIROpcode.Ldiv;
                masm.div(size, quotient, numerator, denominator);
            }
            masm.bind(continuation);
        }
    }

    void arithmeticUdiv(int size, LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";
        assert size == 32 || size == 64 : "size must be 32 or 64";
        CiRegister numerator   = left.asRegister();
        CiRegister quotient    = result.asRegister();
        CiRegister denominator = right.asRegister();

        int offset = masm.insertDivByZeroCheck(denominator);
        tasm.recordImplicitException(offset, info);
        if (code == LIROpcode.Iurem || code == LIROpcode.Lurem) {
            if (quotient == numerator || quotient == denominator) {
                quotient = scratchRegister;
            }
            masm.divu(size, quotient, numerator, denominator);
            masm.mul(size, quotient, quotient, denominator);
            masm.sub(size, result.asRegister(), numerator, quotient);
        } else {
            assert code == LIROpcode.Iudiv || code == LIROpcode.Ludiv;
            masm.divu(size, quotient, numerator, denominator);
        }
    }

    private RISCV64MacroAssembler.ConditionFlag convertCondition(Condition condition) {
        RISCV64MacroAssembler.ConditionFlag acond;
        switch (condition) {
            case EQ:
                acond = RISCV64MacroAssembler.ConditionFlag.EQ;
                break;
            case NE:
                acond = RISCV64MacroAssembler.ConditionFlag.NE;
                break;
            case LT:
                acond = RISCV64MacroAssembler.ConditionFlag.LT;
                break;
            case LE:
                acond = RISCV64MacroAssembler.ConditionFlag.LE;
                break;
            case GE:
                acond = RISCV64MacroAssembler.ConditionFlag.GE;
                break;
            case GT:
                acond = RISCV64MacroAssembler.ConditionFlag.GT;
                break;
            case BE:
                acond = RISCV64MacroAssembler.ConditionFlag.LEU;
                break;
            case AE:
                acond = RISCV64MacroAssembler.ConditionFlag.GEU;
                break;
            case BT:
                acond = RISCV64MacroAssembler.ConditionFlag.LTU;
                break;
            case AT:
                acond = RISCV64MacroAssembler.ConditionFlag.GTU;
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        return acond;
    }

    private RISCV64MacroAssembler.ConditionFlag convertConditionEmitBranch(Condition condition) {
        RISCV64MacroAssembler.ConditionFlag acond;
        switch (condition) {
            case EQ:
                acond = RISCV64MacroAssembler.ConditionFlag.EQ;
                break;
            case NE:
                acond = RISCV64MacroAssembler.ConditionFlag.NE;
                break;
            case BT:
            case LT:
                acond = RISCV64MacroAssembler.ConditionFlag.LT;
                break;
            case BE:
            case LE:
                acond = RISCV64MacroAssembler.ConditionFlag.LE;
                break;
            case AE:
            case GE:
                acond = RISCV64MacroAssembler.ConditionFlag.GE;
                break;
            case AT:
            case GT:
                acond = RISCV64MacroAssembler.ConditionFlag.GT;
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        return acond;
    }

    @Override
    @SuppressWarnings("fallthrough")
    protected void emitCompare(Condition condition, CiValue opr1, CiValue opr2, LIROp2 op) {
        // Checkstyle: off
        assert Util.archKindsEqual(opr1.kind.stackKind(), opr2.kind.stackKind()) || (opr1.kind == CiKind.Long && opr2.kind == CiKind.Int) : "nonmatching stack kinds (" + condition + "): " +
                opr1.kind.stackKind() + "==" + opr2.kind.stackKind();
        CiValue oldOpr1 = opr1;
        if (opr1.isConstant()) {
            CiValue newOpr1 = compilation.registerConfig.getScratchRegister().asValue(opr1.kind);
            const2reg(opr1, newOpr1, null);
            opr1 = newOpr1;
            assert (opr1.kind != CiKind.Float);
            assert (opr1.kind != CiKind.Long);
            assert (opr1.kind != CiKind.Double);
        }

        RISCV64MacroAssembler.ConditionFlag cond = convertCondition(condition);
        if (opr1.isRegister()) {
            CiRegister reg1 = opr1.asRegister();
            if (opr2.isRegister()) {
                // register - register
                switch (opr1.kind) {
                    case Boolean:
                    case Byte:
                    case Char:
                    case Short:
                    case Int:
                    case Object:
                    case Long:
                    case Float:
                    case Double: {
                        doComparison(scratchRegister, reg1, opr2.asRegister(), opr1.kind, cond);
                        break;
                    }
                    default:
                        throw Util.shouldNotReachHere(opr1.kind.toString());
                }
            } else if (opr2.isStackSlot()) {
                // register - stack
                CiStackSlot opr2Slot = (CiStackSlot) opr2;
                switch (opr1.kind) {
                    case Boolean:
                    case Byte:
                    case Char:
                    case Short:
                    case Int:
                    case Long:
                    case Object: {
                        masm.load(scratchRegister1, frameMap.toStackAddress(opr2Slot), opr1.kind);
                        doComparison(scratchRegister, reg1, scratchRegister1, opr1.kind, cond);
                        break;
                    }
                    case Float:
                    case Double: {
                        masm.load(RISCV64.f31, frameMap.toStackAddress(opr2Slot), opr1.kind);
                        doComparison(scratchRegister, reg1, RISCV64.f31, opr1.kind, cond);
                        break;
                    }
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (opr2.isConstant()) {
                // register - constant
                CiConstant c = (CiConstant) opr2;
                switch (opr1.kind) {
                    case Boolean:
                    case Byte:
                    case Char:
                    case Short:
                    case Int:
                    case Long: {
                        boolean isLong = opr1.kind == CiKind.Long;
                        if (isLong) {
                            masm.mov64BitConstant(scratchRegister, c.asLong());
                        } else {
                            masm.mov32BitConstant(scratchRegister, c.asInt());
                        }
                        doComparison(scratchRegister1, reg1, scratchRegister, opr1.kind, cond);
                        break;
                    }
                    case Object: {
                        if (c.isNull()) {
                            masm.and(RISCV64.x31, reg1, reg1);
                        } else {
                            movoop(scratchRegister1, c);
                            doComparison(scratchRegister, reg1, scratchRegister1, opr1.kind, cond);
                        }
                        break;
                    }
                    case Float: {
                        masm.mov64BitConstant(scratchRegister, Float.floatToRawIntBits(c.asFloat()));
                        masm.fmvwx(RISCV64.f31, scratchRegister);
                        doComparison(scratchRegister, reg1, RISCV64.f31, opr1.kind, cond);
                        break;
                    }
                    case Double: {
                        masm.mov64BitConstant(scratchRegister, Double.doubleToRawLongBits(c.asDouble()));
                        masm.fmvdx(RISCV64.f31, scratchRegister);
                        doComparison(scratchRegister, reg1, RISCV64.f31, opr1.kind, cond);
                        break;
                    }
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                throw Util.shouldNotReachHere();
            }
        } else if (opr1.isStackSlot()) {
            if (opr2.isConstant()) {
                CiConstant right = (CiConstant) opr2;
                assert false : "stack constant ";
                switch (opr1.kind) {
                    case Boolean:
                    case Byte:
                    case Char:
                    case Short:
                    case Int:
                    case Long:
                        assert NumUtil.isInt(right.asLong());
                    case Object:
                        assert right.isNull();
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere(opr1.toString() + " opr2 = " + opr2);
        }
        // Checkstyle: on
    }

    private void doComparison(CiRegister regWithOne, CiRegister leftComparand, CiRegister rightComparand, CiKind comparandKind, RISCV64MacroAssembler.ConditionFlag setLessThanConditionFlag) {
        assert regWithOne != leftComparand && regWithOne != rightComparand : "regWithOne should be different from the comparands";
        Label continueLabel = new Label();
        Label lessThanLabel = new Label();

        if (comparandKind == CiKind.Float || comparandKind == CiKind.Double) {
             // Clear NV flag from fflags.
             // Will get set to 1 by masm.fltd/s if one of the operands is NaN.
            masm.csrrci(RISCV64.x0, 0x001, 0b10000);
        }

        // a > b
        masm.mov32BitConstant(regWithOne, 1);
        if (comparandKind == CiKind.Float || comparandKind == CiKind.Double) {
            boolean isDouble = comparandKind == CiKind.Double;
            masm.setLessThanFloatingPoint(RISCV64.x31, rightComparand, leftComparand, isDouble);
        } else {
            masm.setLessThan(RISCV64.x31, rightComparand, leftComparand, setLessThanConditionFlag.isUnsigned());
        }
        masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, RISCV64.x31, regWithOne, continueLabel);
        // a == b
        if (comparandKind == CiKind.Float || comparandKind == CiKind.Double) {
            boolean isDouble = comparandKind == CiKind.Double;
            masm.setLessThanFloatingPoint(RISCV64.x31, leftComparand, rightComparand, isDouble);
        } else {
            masm.setLessThan(RISCV64.x31, leftComparand, rightComparand, setLessThanConditionFlag.isUnsigned());
        }
        masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, RISCV64.x31, regWithOne, lessThanLabel);
        masm.mov32BitConstant(RISCV64.x31, 0);
        masm.b(continueLabel);
        // a < b
        masm.bind(lessThanLabel);
        masm.mov32BitConstant(RISCV64.x31, -1);
        masm.bind(continueLabel);
    }

    @Override
    protected void emitCompare2Int(LIROpcode code, CiValue left, CiValue right, CiValue dst, LIROp2 op) {
        CiRegister dest = dst.asRegister();
        if (code == LIROpcode.Cmpfd2i || code == LIROpcode.Ucmpfd2i) {
            assert left.kind.isFloat() || left.kind.isDouble();

            Label l = new Label();
            if (code == LIROpcode.Ucmpfd2i) {
                // Clear NV flag from fflags.
                // Will get set to 1 by masm.fltd/s if one of the operands is NaN.
                masm.csrrci(RISCV64.x0, 0x001, 0b10000);

                // less than unsigned case
                masm.mov(dest, -1);
                if (left.kind.isFloat()) {
                    masm.flts(scratchRegister1, left.asRegister(), right.asRegister());
                } else {
                    masm.fltd(scratchRegister1, left.asRegister(), right.asRegister());
                }

                // Check if NV flag in fflags is set to 1.
                masm.csrrci(scratchRegister, 0x001, 0b10000); // Read and clear NV flag
                masm.andi(scratchRegister, scratchRegister, 0b10000);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.NE, scratchRegister, RISCV64.zero, l);

                masm.mov32BitConstant(scratchRegister, 1);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, scratchRegister1, scratchRegister, l);

                // equal case
                masm.mov(dest, 0);
                if (left.kind.isFloat()) {
                    masm.fles(scratchRegister1, left.asRegister(), right.asRegister());
                } else {
                    masm.fled(scratchRegister1, left.asRegister(), right.asRegister());
                }
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, scratchRegister1, scratchRegister, l);
                //higher case
                masm.mov(dest, 1);
            } else { // unordered is greater

                // Clear NV flag from fflags.
                // Will get set to 1 by masm.fltd/s if one of the operands is NaN.
                masm.csrrci(RISCV64.x0, 0x001, 0b10000);

                // higher than case
                masm.mov(dest, 1);
                if (left.kind.isFloat()) {
                    masm.flts(scratchRegister1, right.asRegister(), left.asRegister());
                } else {
                    masm.fltd(scratchRegister1, right.asRegister(), left.asRegister());
                }

                // Check if NV flag in fflags is set to 1.
                masm.csrrci(scratchRegister, 0x001, 0b10000); // Read and clear NV flag
                masm.andi(scratchRegister, scratchRegister, 0b10000);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.NE, scratchRegister, RISCV64.zero, l);

                masm.mov32BitConstant(scratchRegister, 1);
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, scratchRegister1, scratchRegister, l);

                // equal case
                masm.mov(dest, 0);
                if (left.kind.isFloat()) {
                    masm.fles(scratchRegister1, right.asRegister(), left.asRegister());
                } else {
                    masm.fled(scratchRegister1, right.asRegister(), left.asRegister());
                }
                masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, scratchRegister1, scratchRegister, l);
                masm.subi(dest, dest, 1);
            }
            masm.bind(l);

        } else {
            assert code == LIROpcode.Cmpl2i;
            Label high = new Label();
            Label done = new Label();
            Label isEqual = new Label();
            masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.EQ, left.asRegister(), right.asRegister(), isEqual);
            masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.GT, left.asRegister(), right.asRegister(), high);
            masm.mov(dest, -1);
            masm.b(done);
            masm.bind(high);
            masm.mov(dest, 1);
            masm.b(done);
            masm.bind(isEqual);
            masm.mov(dest, 0);
            masm.bind(done);
        }
    }

    @Override
    protected void emitDirectCallAlignment() {
        masm.alignForPatchableDirectCall(masm.codeBuffer.position());
    }

    @Override
    protected void emitIndirectCall(Object target, LIRDebugInfo info, CiValue callAddress) {
        CiRegister reg = scratchRegister;
        if (callAddress.isRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, reg.asValue(callAddress.kind), callAddress.kind, null, false);
        }
        indirectCall(reg, target, info);
    }

    @Override
    protected void emitDirectCall(Object target, LIRDebugInfo info) {
        directCall(target, info);
    }

    @Override
    protected void emitNativeCall(String symbol, LIRDebugInfo info, CiValue callAddress) {
        CiRegister reg = scratchRegister;
        if (callAddress.isRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, reg.asValue(callAddress.kind), callAddress.kind, null, false);
        }
        indirectCall(reg, symbol, info);
    }

    @Override
    protected void emitThrow(CiValue exceptionPC, CiValue exceptionOop, LIRDebugInfo info, boolean unwind) {
        // exception object is not added to oop map by LinearScan
        // (LinearScan assumes that no oops are in fixed registers)
        // info.addRegisterOop(exceptionOop);
        directCall(unwind ? CiRuntimeCall.UnwindException : CiRuntimeCall.HandleException, info);
        // enough room for two byte trap
        if (!C1XOptions.EmitNopAfterCall) {
            masm.nop();
        }
    }

    private void emitXIRShiftOp(LIROpcode code, CiValue left, CiValue count, CiValue dest) {
        if (count.isConstant()) {
            emitShiftOp(code, left, ((CiConstant) count).asInt(), dest);
        } else {
            emitShiftOp(code, left, count, dest, IllegalValue);
        }
    }

    @Override
    protected void emitShiftOp(LIROpcode code, CiValue left, CiValue count, CiValue dest, CiValue tmp) {
        assert count.asRegister() == SHIFTCount : "count must be in s11/x27";
        assert left == dest : "left and dest must be equal";
        assert tmp.isIllegal() : "wasting a register if tmp is allocated";
        assert left.isRegister();
        assert count.asRegister() != RISCV64.x28 : "count register must not be scratch";
        CiRegister register = left.asRegister();
        assert register != SHIFTCount : "left cannot be s11/x27";
        if (left.kind.isInt()) {
            masm.mov64BitConstant(scratchRegister, 0xFFFFFFFFL);
        } else {
            masm.mov64BitConstant(scratchRegister, 0xFFFFFFFFFFFFFFFFL);
        }
        switch (code) {
            case Shl: {
                masm.and(scratchRegister1, count.asRegister(), scratchRegister);
                if (left.kind.isInt()) {
                    masm.sllw(register, register, scratchRegister1);
                } else {
                    masm.sll(register, register, scratchRegister1);
                }
            }
            break;
            case Shr: {
                if (left.kind.isInt()) {
                    masm.sraw(register, register, count.asRegister());
                } else {
                    masm.sra(register, register, count.asRegister());
                }
            }
            break;
            case Ushr: {
                masm.and(scratchRegister1, count.asRegister(), scratchRegister);
                if (left.kind.isInt()) {
                    masm.srlw(register, register, scratchRegister1);
                } else {
                    masm.srl(register, register, scratchRegister1);
                }
            }
            break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitShiftOp(LIROpcode code, CiValue left, int count, CiValue dest) {
        assert left == dest : "left and dest must be equal";
        assert left.isRegister();
        CiRegister register = left.asRegister();
        switch (code) {
            case Shl: {
                if (left.kind.isInt()) {
                    masm.slliw(register, register, count);
                } else {
                    masm.slli(register, register, count);
                }
                break;
            }
            case Shr: {
                if (left.kind.isInt()) {
                    masm.sraiw(register, register, count);
                } else {
                    masm.srai(register, register, count);
                }
                break;
            }
            case Ushr: {
                if (left.kind.isInt()) {
                    masm.srliw(register, register, count);
                } else {
                    masm.srli(register, register, count);
                }
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitSignificantBitOp(boolean most, CiValue src, CiValue dst) {
        assert dst.isRegister();
        CiRegister result = dst.asRegister();
        CiRegister value;
        if (src.isRegister()) {
            value = src.asRegister();
        } else {
            CiAddress laddr = asAddress(src);
            masm.setUpScratch(laddr);
            value = scratchRegister;
        }
        assert value != result;
        // if zero return -1
        masm.xori(result, RISCV64.zero, -1); // result = ~RISCV64.zero
        Label end = new Label();
        masm.cbz(src.asRegister(), end);
        // else find the bit
        if (most) {
            masm.clz(64, result, value);
            masm.mov64BitConstant(scratchRegister, 63);
            masm.sub(64, result, scratchRegister, result);
        } else {
            masm.ctz(64, result, value);
        }
        masm.bind(end);
    }

    @Override
    protected void emitAlignment() {
        masm.align(16);
    }

    @Override
    protected void emitNegate(LIRNegate op) {
        CiValue left = op.operand();
        CiValue dest = op.result();
        assert left.isRegister();
        if (left.kind.isInt()) {
            masm.neg(32, dest.asRegister(), left.asRegister());
        } else if (dest.kind.isFloat()) {
            masm.fneg(32, dest.asRegister(), left.asRegister());
        } else if (dest.kind.isDouble()) {
            masm.fneg(64, dest.asRegister(), left.asRegister());
        } else {
            masm.neg(64, dest.asRegister(), left.asRegister());
        }
    }

    @Override
    protected void emitLea(CiValue src, CiValue dest) {
        CiRegister reg = dest.asRegister();
        masm.leaq(reg, asAddress(src));
    }

    @Override
    protected void emitNullCheck(CiValue src, LIRDebugInfo info) {
        assert src.isRegister();
        if (C1XOptions.NullCheckUniquePc) {
            masm.nop();
        }
        masm.nullCheck(src.asRegister());
        tasm.recordImplicitException(codePos() - 4, info);
    }

    @Override
    protected void emitVolatileMove(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info) {
        assert kind == CiKind.Long : "only for volatile long fields";
        if (info != null) {
            tasm.recordImplicitException(codePos(), info);
        }
        assert false : "emitVolatileMove RSICV64LIRAssembler";

        if (src.kind.isDouble()) {
            assert dest.isAddress() || dest.isRegister() || dest.isStackSlot();
        } else {
            assert dest.kind.isDouble();
            if (src.isStackSlot()) {
                masm.load(dest.asRegister(), frameMap.toStackAddress((CiStackSlot) src), CiKind.Double);
            } else {
                assert src.isAddress();
                masm.load(dest.asRegister(), (CiAddress) src, CiKind.Double);
            }
        }
    }

    @Override
    protected void emitMemoryBarriers(int barriers) {
        masm.membar();
    }

    @Override
    protected void emitDebugID(String methodName, String inlinedMethodName) {
        debugMethodWriter.append(inlinedMethodName + " " + Integer.toHexString(masm.codeBuffer.position()) + " " + masm.codeBuffer.position() + " (inlined)", methodID);
    }

    @Override
    protected void doPeephole(LIRList list) {
        // Do nothing for now
    }

    @Override
    protected void emitXir(LIRXirInstruction instruction) {
        XirSnippet snippet = instruction.snippet;
        Label[] labels = new Label[snippet.template.labels.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }
        emitXirInstructions(instruction, snippet.template.fastPath, labels, instruction.getOperands(), snippet.marks);
        if (snippet.template.slowPath != null) {
            addSlowPath(new SlowPath(instruction, labels, snippet.marks));
        }
    }

    @Override
    protected void emitSlowPath(SlowPath sp) {
        int start = -1;
        if (C1XOptions.TraceAssembler) {
            TTY.println("Emitting slow path for XIR instruction " + sp.instruction.snippet.template.name);
            start = masm.codeBuffer.position();
        }
        emitXirInstructions(sp.instruction, sp.instruction.snippet.template.slowPath, sp.labels, sp.instruction.getOperands(), sp.marks);
        masm.nop();
        if (C1XOptions.TraceAssembler) {
            TTY.println("From " + start + " to " + masm.codeBuffer.position());
        }
    }

    public void emitXirInstructions(LIRXirInstruction xir, XirInstruction[] instructions, Label[] labels, CiValue[] operands, Map<XirMark, Mark> marks) {
        LIRDebugInfo info = xir == null ? null : xir.info;
        LIRDebugInfo infoAfter = xir == null ? null : xir.infoAfter;
        for (XirInstruction inst : instructions) {
            switch (inst.op) {
                case Add:
                    emitArithOp(LIROpcode.Add, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    break;
                case Sub:
                    emitArithOp(LIROpcode.Sub, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    break;
                case Div:
                    if (inst.kind == CiKind.Int) {
                        arithmeticDiv(32, LIROpcode.Idiv, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    } else {
                        emitArithOp(LIROpcode.Div, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    }
                    break;
                case Mul:
                    emitArithOp(LIROpcode.Mul, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    break;
                case Mod:
                    if (inst.kind == CiKind.Int) {
                        arithmeticDiv(32, LIROpcode.Irem, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    } else {
                        emitArithOp(LIROpcode.Rem, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    }
                    break;
                case Shl:
                    emitXIRShiftOp(LIROpcode.Shl, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;
                case Sar:
                    emitXIRShiftOp(LIROpcode.Shr, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;
                case Shr:
                    emitXIRShiftOp(LIROpcode.Ushr, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;
                case And:
                    emitLogicOp(LIROpcode.LogicAnd, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;
                case Or:
                    emitLogicOp(LIROpcode.LogicOr, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;
                case Xor:
                    emitLogicOp(LIROpcode.LogicXor, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;
                case Mov: {
                    CiValue result = operands[inst.result.index];
                    CiValue source = operands[inst.x().index];
                    moveOp(source, result, result.kind, null, false);
                    break;
                }
                case PointerLoad: {
                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiRegisterValue register = assureInRegister(pointer);
                    moveOp(new CiAddress(inst.kind, register, 0), result, inst.kind, null, false);
                    if ((Boolean) inst.extra && info != null) {
                        tasm.recordImplicitException(codePos() - 4, info);
                    }
                    break;
                }
                case PointerStore: {
                    CiValue value = operands[inst.y().index];
                    CiValue pointer = operands[inst.x().index];
                    assert pointer.isVariableOrRegister();
                    moveOp(value, new CiAddress(inst.kind, pointer, 0), inst.kind, null, false);
                    if ((Boolean) inst.extra && info != null) {
                        tasm.recordImplicitException(codePos() - 4, info);
                    }
                    break;
                }
                case PointerLoadDisp: {
                    AddressAccessInformation addressInformation = (AddressAccessInformation) inst.extra;
                    boolean canTrap = addressInformation.canTrap;
                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;
                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];
                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();
                    CiValue src = null;
                    if (index.isConstant()) {
                        assert index.kind == CiKind.Int;
                        CiConstant constantIndex = (CiConstant) index;
                        src = new CiAddress(inst.kind, pointer, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        src = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }
                    moveOp(src, result, inst.kind, canTrap ? info : null, false);
                    break;
                }
                case Here: {
                    CiValue result = operands[inst.result.index];
                    CiRegister dst = result.asRegister();
                    int beforeLea = masm.codeBuffer.position();
                    masm.auipc(dst, 0);
                    masm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                    int afterLea = masm.codeBuffer.position();
                    masm.codeBuffer.setPosition(beforeLea);
                    masm.auipc(dst, 0);
                    masm.mov32BitConstant(scratchRegister, beforeLea - afterLea);
                    masm.add(dst, dst, scratchRegister);
                    masm.codeBuffer.setPosition(afterLea);
                    break;
                }
                case LoadEffectiveAddress: {
                    AddressAccessInformation addressInformation = (AddressAccessInformation) inst.extra;
                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;
                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];
                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();
                    CiValue src = new CiAddress(CiKind.Illegal, pointer, index, scale, displacement);
                    emitLea(src, result);
                    break;
                }
                case PointerStoreDisp: {
                    AddressAccessInformation addressInformation = (AddressAccessInformation) inst.extra;
                    boolean canTrap = addressInformation.canTrap;
                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;
                    CiValue value = operands[inst.z().index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];
                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();
                    CiValue dst;
                    if (index.isConstant()) {
                        assert index.kind == CiKind.Int;
                        CiConstant constantIndex = (CiConstant) index;
                        dst = new CiAddress(inst.kind, pointer, IllegalValue, scale, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        dst = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }
                    moveOp(value, dst, inst.kind, canTrap ? info : null, false);
                    break;
                }
                case RepeatMoveBytes:
                    assert false : "RepeatMoveBytes in RISCV64LIRAssembler unimplemented";
                    masm.crashme();
                    break;
                case RepeatMoveWords:
                    assert false : "RepeatMoveWords in RISCV64LIRAssembler unimplemented";
                    masm.crashme();
                    break;
                case PointerCAS: {
                    assert false : "PointerCAS in RISCV64LIRAssembler unimplemented";
                    masm.crashme();
                    if ((Boolean) inst.extra && info != null) {
                        tasm.recordImplicitException(codePos(), info);
                    }
                    break;
                }
                case CallStub: {
                    XirTemplate stubId = (XirTemplate) inst.extra;
                    CiRegister result = CiRegister.None;
                    if (inst.result != null) {
                        result = operands[inst.result.index].asRegister();
                    }
                    CiValue[] args = new CiValue[inst.arguments.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = operands[inst.arguments[i].index];
                    }
                    callStub(stubId, info, result, args);
                    break;
                }
                case CallRuntime: {
                    CiKind[] signature = new CiKind[inst.arguments.length];
                    for (int i = 0; i < signature.length; i++) {
                        signature[i] = inst.arguments[i].kind;
                    }
                    CiCallingConvention cc = frameMap.getCallingConvention(signature, RuntimeCall);
                    for (int i = 0; i < inst.arguments.length; i++) {
                        CiValue argumentLocation = cc.locations[i];
                        CiValue argumentSourceLocation = operands[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            moveOp(argumentSourceLocation, argumentLocation, argumentLocation.kind, null, false);
                        }
                    }
                    RuntimeCallInformation runtimeCallInformation = (RuntimeCallInformation) inst.extra;
                    directCall(runtimeCallInformation.target, (runtimeCallInformation.useInfoAfter) ? infoAfter : info);
                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        CiRegister returnRegister = compilation.registerConfig.getReturnRegister(inst.result.kind);
                        CiValue resultLocation = returnRegister.asValue(inst.result.kind.stackKind());
                        moveOp(resultLocation, operands[inst.result.index], inst.result.kind.stackKind(), null, false);
                    }
                    break;
                }
                case Jmp: {
                    if (inst.extra instanceof XirLabel) {
                        Label label = labels[((XirLabel) inst.extra).index];
                        masm.b(label);
                    } else {
                        directJmp(inst.extra);
                    }
                    break;
                }
                case DecAndJumpNotZero: {
                    assert false : "DecAndJumpNotZero in RISCV64LIRAssembler unimplemented";
                    masm.crashme();
                    CiValue value = operands[inst.x().index];
                    if (value.kind == CiKind.Long) {
                        masm.sub(64, value.asRegister(), value.asRegister(), value.asRegister());
                    } else {
                        assert value.kind == CiKind.Int;
                    }
                    break;
                }
                case Jeq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.EQ, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.NE, operands, label);
                    break;
                }
                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GT, operands, label);
                    break;
                }
                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GE, operands, label);
                    break;
                }
                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.AE, operands, label);
                    break;
                }
                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LT, operands, label);
                    break;
                }
                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LE, operands, label);
                    break;
                }
                case Jbset: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    CiValue offset = operands[inst.y().index];
                    CiValue bit = operands[inst.z().index];
                    assert offset.isConstant() && bit.isConstant();
                    assert false;
                    masm.crashme();
                    masm.branchConditionally(RISCV64MacroAssembler.ConditionFlag.GE, RISCV64.x31, RISCV64.zero, label);
                    break;
                }
                case Bind: {
                    XirLabel l = (XirLabel) inst.extra;
                    Label label = labels[l.index];
                    asm.bind(label);
                    break;
                }
                case Safepoint: {
                    assert info != null : "Must have debug info in order to create a safepoint.";
                    int offset = (Integer) inst.extra;
                    assert offset == 0 || Platform.target().arch.is32bit();
                    tasm.recordSafepoint(codePos() + offset, info);
                    break;
                }
                case NullCheck: {
                    CiValue pointer = operands[inst.x().index];
                    masm.nullCheck(pointer.asRegister());
                    tasm.recordImplicitException(codePos() - 4, info);
                    break;
                }
                case Align: {
                    masm.align((Integer) inst.extra);
                    break;
                }
                case StackOverflowCheck: {
                    int frameSize = initialFrameSizeInBytes();
                    int lastFramePage = frameSize / target.pageSize;
                    // emit multiple stack bangs for methods with frames larger than a page
                    for (int i = 0; i <= lastFramePage; i++) {
                        int offset = (i + C1XOptions.StackShadowPages) * target.pageSize;
                        // Deduct 'frameSize' to handle frames larger than the shadow
                        masm.bangStackWithOffset(offset - frameSize);
                    }
                    break;
                }
                case PushFrame: {
                    int frameSize = initialFrameSizeInBytes();
                    if (CompilationBroker.singleton.simulateAdapter()) {
                        masm.nop(4);
                    }

                    masm.push(64, RISCV64.ra);

                    if (C1XOptions.ZapStackOnMethodEntry) {
                        masm.mov(scratchRegister, 0xC1C1C1C1_C1C1C1C1L);
                        for (int i = 0; i < frameSize / (2 * Word.size()); ++i) {
                            masm.str(Word.width(), scratchRegister, RISCV64Address.createPreIndexedImmediateAddress(RISCV64.sp, -Word.size()));
                            masm.str(Word.width(), scratchRegister, RISCV64Address.createPreIndexedImmediateAddress(RISCV64.sp, -Word.size()));
                        }
                    } else {
                        masm.mov64BitConstant(scratchRegister, frameSize);
                        masm.sub(64, RISCV64.sp, RISCV64.sp, scratchRegister);
                    }

                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        assert frameToCSA >= 0;
                        masm.save(csl, frameToCSA);
                    }

                    if (C1XOptions.DebugMethods) {
                        masm.mov64BitConstant(masm.scratchRegister, methodID);
                        debugMethodWriter.append(compilation.method.holder() + "." + compilation.method.name() + ";" + compilation.method.signature(), methodID);
                    }
                    break;
                }
                case PopFrame: {
                    int frameSize = initialFrameSizeInBytes();

                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        registerRestoreEpilogueOffset = masm.codeBuffer.position();
                        // saved all registers, restore all registers
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        masm.restore(csl, frameToCSA);
                    }
                    masm.mov64BitConstant(scratchRegister, frameSize);
                    masm.add(64, RISCV64.sp, RISCV64.sp, scratchRegister);
                    break;
                }
                case Push: {
                    CiRegisterValue value = assureInRegister(operands[inst.x().index]);
                    if (value.asRegister().isCpu()) {
                        masm.push(64, value.asRegister());
                    } else {
                        masm.fpush(64, value.asRegister());
                    }
                    break;
                }
                case Pop: {
                    CiValue result = operands[inst.result.index];
                    if (result.isRegister()) {
                        if (result.asRegister().isCpu()) {
                            masm.pop(64, result.asRegister(), true);
                        } else {
                            masm.fpop(64, result.asRegister());
                        }
                    } else {
                        masm.pop(64, scratchRegister, true);
                        moveOp(scratchRegister.asValue(), result, result.kind, null, true);
                    }
                    break;
                }
                case Mark: {
                    XirMark xmark = (XirMark) inst.extra;
                    Mark[] references = new Mark[xmark.references.length];
                    for (int i = 0; i < references.length; i++) {
                        references[i] = marks.get(xmark.references[i]);
                        assert references[i] != null;
                    }
                    Mark mark = tasm.recordMark(xmark.id, references);
                    marks.put(xmark, mark);
                    break;
                }
                case Nop: {
                    for (int i = 0; i < (Integer) inst.extra; i++) {
                        masm.nop();
                    }
                    break;
                }
                case RawBytes: {
                    for (byte b : (byte[]) inst.extra) {
                        masm.codeBuffer.emitByte(b & 0xff);
                    }
                    break;
                }
                case ShouldNotReachHere: {
                    if (inst.extra == null) {
                        stop("should not reach here");
                    } else {
                        stop("should not reach here: " + inst.extra);
                    }
                    break;
                }
                default:
                    throw Util.unimplemented("XIR operation " + inst.op);
            }
        }
    }

    private CiRegisterValue assureInRegister(CiValue pointer) {
        if (pointer.isConstant()) {
            CiRegisterValue register = scratchRegister.asValue(pointer.kind);
            moveOp(pointer, register, pointer.kind, null, false);
            return register;
        }
        assert pointer.isRegister() : "should be register, but is: " + pointer;
        return (CiRegisterValue) pointer;
    }

    private void emitXirCompare(XirInstruction inst, Condition condition, CiValue[] ops, Label label) {
        RISCV64MacroAssembler.ConditionFlag cflag = convertConditionEmitBranch(condition);
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];
        emitCompare(condition, x, y, null);
        masm.branchConditionally(cflag, RISCV64.x31, RISCV64.zero, label);
        masm.nop(3);
    }

    @Override
    public void emitDeoptizationStub(DeoptimizationStub stub) {
        masm.bind(stub.label);
        directCall(CiRuntimeCall.Deoptimize, stub.info);
        shouldNotReachHere();
    }

    public CompilerStub lookupStub(XirTemplate template) {
        return compilation.compiler.lookupStub(template);
    }

    public void callStub(XirTemplate stub, LIRDebugInfo info, CiRegister result, CiValue... args) {
        callStubHelper(lookupStub(stub), stub.resultOperand.kind, info, result, args);
    }

    public void callStub(CompilerStub stub, LIRDebugInfo info, CiRegister result, CiValue... args) {
        callStubHelper(stub, stub.resultKind, info, result, args);
    }

    private void callStubHelper(CompilerStub stub, CiKind resultKind, LIRDebugInfo info, CiRegister result, CiValue... args) {
        assert args.length == stub.inArgs.length;
        for (int i = 0; i < args.length; i++) {
            CiStackSlot inArg = stub.inArgs[i];
            assert inArg.inCallerFrame();
            CiStackSlot outArg = inArg.asOutArg();
            storeParameter(args[i], outArg);
        }

        directCall(stub.stubObject, info);

        if (result != CiRegister.None) {
            final CiAddress src = compilation.frameMap().toStackAddress(stub.outResult.asOutArg());
            loadResult(result, src);
        }

        // Clear out parameters
        if (C1XOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                CiStackSlot inArg = stub.inArgs[i];
                CiStackSlot outArg = inArg.asOutArg();
                CiAddress dst = compilation.frameMap().toStackAddress(outArg);
                masm.mov32BitConstant(scratchRegister, 0);
                masm.store(scratchRegister, dst, CiKind.Long);
            }
        }
    }

    private void loadResult(CiRegister dst, CiAddress src) {
        masm.load(dst, src, src.kind);
    }

    private void storeParameter(CiValue registerOrConstant, CiStackSlot outArg) {
        CiAddress dst = compilation.frameMap().toStackAddress(outArg);
        CiKind k = registerOrConstant.kind;
        if (registerOrConstant.isConstant()) {
            CiConstant c = (CiConstant) registerOrConstant;
            if (c.kind == CiKind.Object) {
                movoop(scratchRegister, c);
            } else {
                masm.mov(scratchRegister, c.asInt());
            }
            masm.store(scratchRegister, dst, c.kind);
        } else if (registerOrConstant.isRegister()) {
            masm.store(registerOrConstant.asRegister(), dst, k);
        } else {
            throw new InternalError("should not reach here");
        }
    }

    public void movoop(CiRegister dst, CiConstant obj) {
        assert obj.kind == CiKind.Object;
        if (obj.isNull()) {
            masm.mov(dst, 0);
        } else {
            if (target.inlineObjects) {
                assert false : "Object inlining not supported";
            } else {
                tasm.recordDataReferenceInCode(obj);
                masm.auipc(scratchRegister, 0);
                masm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS); // this gets patched by RISCV64InstructionDecoder.patchRelativeInstruction
                masm.load(dst, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister), obj.kind);
            }
        }
    }

    public void directCall(Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
        masm.call();
        int after = masm.codeBuffer.position();
        if (C1XOptions.EmitNopAfterCall) {
            masm.nop();
        }
        tasm.recordDirectCall(before, after - before, asCallTarget(target), info);
        tasm.recordExceptionHandlers(after, info);
    }

    public void directJmp(Object target) {
        throw Util.unimplemented();
    }

    public void indirectCall(CiRegister src, Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
        masm.call(src);
        int after = masm.codeBuffer.position();
        if (C1XOptions.EmitNopAfterCall) {
            masm.nop();
        }
        tasm.recordIndirectCall(before, after - before, asCallTarget(target), info);
        tasm.recordExceptionHandlers(after, info);
    }

    protected void stop(String msg) {
        if (C1XOptions.GenAssertionCode) {
            directCall(CiRuntimeCall.Debug, null);
            masm.ebreak();
        }
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }
}
