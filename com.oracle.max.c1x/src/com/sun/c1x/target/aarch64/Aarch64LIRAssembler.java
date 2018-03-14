/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
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
package com.sun.c1x.target.aarch64;

import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.cri.ci.CiValue.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.aarch64.Aarch64Assembler.*;
import com.oracle.max.criutils.*;
import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.gen.LIRGenerator.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.FrameMap.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.*;
import com.sun.max.platform.*;
import com.sun.max.vm.compiler.*;

public final class Aarch64LIRAssembler extends LIRAssembler {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final CiRegister SHIFTCount = Aarch64.r1;

    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    final CiTarget target;
    final Aarch64MacroAssembler masm;
    final CiRegister rscratch1;

    public Aarch64LIRAssembler(C1XCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (Aarch64MacroAssembler) tasm.asm;
        target = compilation.target;
        rscratch1 = compilation.registerConfig.getScratchRegister();
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
        masm.ret(0);
    }

    @Override
    protected void emitInfopoint(CiValue dst, LIRDebugInfo info, Infopoint.Op op) {
        switch (op) {
            case HERE:
                tasm.recordSafepoint(codePos(), info);
                int beforeLea = masm.codeBuffer.position();
                masm.adr(dst.asRegister(), 0);
                int afterLea = masm.codeBuffer.position();
                masm.adr(dst.asRegister(), beforeLea - afterLea, beforeLea);
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
        masm.leaq(dst.asRegister(), new CiAddress(slot.kind, Aarch64.sp.asValue(), slot.index() * target.arch.wordSize));
    }

    @Override
    protected void emitPause() {
        masm.pause();
    }

    @Override
    protected void emitBreakpoint() {
        masm.int3();
    }

    @Override
    protected void emitIfBit(CiValue address, CiValue bitNo) {
        assert false : "emitIfBit Aarch64IRAssembler";
        masm.crashme();
        masm.insertForeverLoop();
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, CiValue dst) {
        masm.leaq(dst.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    private void moveRegs(CiRegister fromReg, CiRegister toReg) {
        if (fromReg != toReg) {
            masm.mov(64, toReg, fromReg);
        }
    }

    private void swapReg(CiRegister a, CiRegister b) {
        masm.xchgptr(a, b);
    }

    private void const2reg(CiRegister dst, int constant) {
        masm.mov64BitConstant(dst, constant);
    }

    private void const2reg(CiRegister dst, long constant, CiKind dstKind) {
        masm.movlong(dst, constant, dstKind);
    }

    private void const2reg(CiRegister dst, CiConstant constant) {
        if (true) {
            throw Util.unimplemented();
        }

        assert constant.kind == CiKind.Object;
        if (constant.isNull()) {
            masm.mov64BitConstant(dst, 0x0);
        } else if (target.inlineObjects) {
            tasm.recordDataReferenceInCode(constant);
            masm.mov64BitConstant(dst, 0xDEADDEAD);
        } else {
            masm.setUpScratch(tasm.recordDataReferenceInCode(constant));
//            masm.addRegisters(ConditionFlag.Always, false, Aarch64.r16, Aarch64.r16, Aarch64.r15, 0, 0);
//            masm.ldr(ConditionFlag.Always, dst, Aarch64.r16, 0);
        }
    }

    @Override
    public void emitTraps() {
        for (int i = 0; i < C1XOptions.MethodEndBreakpointGuards; ++i) {
            masm.int3();
        }
        masm.nop(8);
    }

    private void const2reg(CiRegister dst, float constant, CiKind dstKind) {
        masm.mov32BitConstant(rscratch1, Float.floatToRawIntBits(constant));
        masm.fmov(32, dst, rscratch1);
    }

    private void const2reg(CiRegister dst, double constant, CiKind dstKind) {
        masm.movlong(dst, Double.doubleToRawLongBits(constant), dstKind);
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
                const2reg(dest.asRegister(), c.asInt());
                break;
            case Long:
                const2reg(dest.asRegister(), c.asLong(), dest.kind);
                break;
            case Object:
                const2reg(dest.asRegister(), c);
                break;
            case Float:
                const2reg(dest.asRegister(), c.asFloat(), dest.kind);
                break;
            case Double:
                const2reg(dest.asRegister(), c.asDouble(), dest.kind);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
    }

    @Override
    protected void const2stack(CiValue src, CiValue dst) {
        assert src.isConstant();
        assert dst.isStackSlot();
        CiStackSlot slot = (CiStackSlot) dst;
        CiConstant c = (CiConstant) src;

        // Checkstyle: off
        CiAddress address = frameMap.toStackAddress(slot);
        switch (c.kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Int:
                masm.mov64BitConstant(Aarch64.r8, c.asInt());
                masm.store(Aarch64.r8, address, CiKind.Int);
                break;
            case Float:
                masm.mov64BitConstant(Aarch64.r8, Float.floatToRawIntBits(c.asFloat()));
                masm.store(Aarch64.r8, address, CiKind.Int);
                break;
            case Object:
                movoop(frameMap.toStackAddress(slot), c);
                break;
            case Long:
                masm.saveInFP(9);
                masm.movlong(Aarch64.r8, c.asLong(), CiKind.Long);
                masm.store(Aarch64.r8, address, CiKind.Long);
                masm.restoreFromFP(9);
                break;
            case Double:
                masm.saveInFP(9);
                masm.movlong(Aarch64.r8, Double.doubleToRawLongBits(c.asDouble()), CiKind.Long);
                masm.store(Aarch64.r8, address, CiKind.Long);
                masm.restoreFromFP(9);
                break;
            default:
                throw Util.shouldNotReachHere("Unknown constant kind for const2stack: " + c.kind);
        }
        // Checkstyle: on
    }

    @Override
    protected void const2mem(CiValue src, CiValue dst, CiKind kind, LIRDebugInfo info) {
        assert src.isConstant();
        assert dst.isAddress();
        CiConstant constant = (CiConstant) src;
        CiAddress addr = asAddress(dst);
        int nullCheckHere = codePos();

        // Checkstyle: off
        switch (kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Int:
                if (kind == CiKind.Boolean || kind == CiKind.Byte) {
                    masm.mov64BitConstant(Aarch64.r8, constant.asInt() & 0xFF);
                } else if (kind == CiKind.Char || kind == CiKind.Short) {
                    masm.mov64BitConstant(Aarch64.r8, constant.asInt() & 0xFFFF);
                } else {
                    masm.mov64BitConstant(Aarch64.r8, constant.asInt());
                }
                masm.store(Aarch64.r8, addr, CiKind.Int);
                nullCheckHere = codePos() - 4;
                break;
            case Float:
                masm.mov64BitConstant(Aarch64.r8, Float.floatToRawIntBits(constant.asFloat()));
                masm.store(Aarch64.r8, addr, CiKind.Int);
                nullCheckHere = codePos() - 4;
                break;
            case Object:
                movoop(addr, constant);
                break;
            case Long:
                masm.saveInFP(9);
                masm.movlong(Aarch64.r8, constant.asLong(), CiKind.Long);
                masm.store(Aarch64.r8, addr, CiKind.Long);
                nullCheckHere = codePos() - 4;
                masm.restoreFromFP(9);
                break;
            case Double:
                masm.saveInFP(9);
                masm.movlong(Aarch64.r8, Double.doubleToRawLongBits(constant.asDouble()), CiKind.Long);
                masm.store(Aarch64.r8, addr, CiKind.Long);
                masm.restoreFromFP(9);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on

        if (info != null) {
            tasm.recordImplicitException(nullCheckHere, info);
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
        // Checkstyle: off
        switch (src.kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Object:
            case Int:
                masm.store(src.asRegister(), addr, CiKind.Int);
                break;
            case Long:
                masm.store(src.asRegister(), addr, CiKind.Long);
                break;
            case Float:
                masm.store(src.asRegister(), addr, CiKind.Float);
                break;
            case Double:
                masm.store(src.asRegister(), addr, CiKind.Double);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
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
        // Checkstyle: off
        switch (dest.kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Int:
            case Object:
                masm.load(dest.asRegister(), addr, CiKind.Int);
                break;
            case Long:
                masm.load(dest.asRegister(), addr, CiKind.Long);
                break;
            case Float:
                masm.load(dest.asRegister(), addr, CiKind.Float);
                break;
            case Double:
                masm.load(dest.asRegister(), addr, CiKind.Double);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
    }

    @Override
    protected void mem2mem(CiValue src, CiValue dest, CiKind kind) {
        if (dest.kind.isInt()) {
            masm.load(Aarch64.r16, (CiAddress) src, CiKind.Int);
            masm.store(Aarch64.r16, (CiAddress) dest, CiKind.Int);
        } else {
            assert false : "Not implemented yet";
        }
    }

    @Override
    protected void mem2stack(CiValue src, CiValue dest, CiKind kind) {
        assert false : "mem2stack not implemented";
    }

    @Override
    protected void stack2stack(CiValue src, CiValue dest, CiKind kind) {
        if (src.kind == CiKind.Long || src.kind == CiKind.Double) {
            masm.saveInFP(9);
            masm.load(Aarch64.r8, frameMap.toStackAddress((CiStackSlot) src), CiKind.Long);
            masm.store(Aarch64.r8, frameMap.toStackAddress((CiStackSlot) dest), CiKind.Long);
            masm.restoreFromFP(9);
        } else {
            masm.load(Aarch64.r8, frameMap.toStackAddress((CiStackSlot) src), CiKind.Int);
            masm.store(Aarch64.r8, frameMap.toStackAddress((CiStackSlot) dest), CiKind.Int);
        }
    }

    @Override
    protected void mem2reg(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        assert src.isAddress();
        assert dest.isRegister() : "dest=" + dest;
        CiAddress addr = (CiAddress) src;
        boolean safepoint = (addr.base().isValid() && addr.displacement == 0 && addr.base().compareTo(Aarch64.LATCH_REGISTER) == 0) ? true : false;
        assert !(!safepoint && dest.asRegister().equals(Aarch64.LATCH_REGISTER));
        if (addr.base().compareTo(Aarch64.LATCH_REGISTER) == 0 && addr.displacement == 0) {
            assert dest.asRegister().equals(Aarch64.LATCH_REGISTER) : dest.asRegister().number;
        }
        // Checkstyle: off
        switch (kind) {
            case Float:
            case Double:
                masm.load(dest.asRegister(), addr, kind);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Object:
            case Int:
                if (safepoint) {
                    if (info != null) {
                        tasm.recordImplicitException(codePos(), info);
                    }
//                    masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, dest.asRegister(), Aarch64.LATCH_REGISTER, 0);
                } else {
                    masm.setUpScratch(addr);
//                    masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, dest.asRegister(), safepoint ? Aarch64.LATCH_REGISTER : Aarch64.r16, 0);
                    if (info != null) {
                        tasm.recordImplicitException(codePos() - 4, info);
                    }
                }
                break;
            case Long:
                masm.load(dest.asRegister(), addr, kind);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Boolean:
            case Byte:
                masm.load(dest.asRegister(), addr, kind);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Char:
                masm.load(dest.asRegister(), addr, kind);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Short:
                masm.load(dest.asRegister(), addr, kind);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
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
        if (true) {
            throw Util.unimplemented();
        }

        assert assertEmitTableSwitch(op);
        CiRegister value = op.value().asRegister();
        final Buffer buf = masm.codeBuffer;

        // Compare index against jump table bounds
        int highKey = op.lowKey + op.targets.length - 1;
        if (op.lowKey != 0) {
            // subtract the low value from the switch value
            masm.subq(value, op.lowKey);
//            masm.cmpl(value, highKey - op.lowKey);
        } else {
//            masm.cmpl(value, highKey);
        }

        // Jump to default target if index is not within the jump table
//        masm.jcc(ConditionFlag.UnsignedHigher, new Aarch64Label(op.defaultTarget.label()));
        // Set scratch to address of jump table
        int leaPos = buf.position();
        masm.leaq(rscratch1, CiAddress.Placeholder);
        int afterLea = buf.position();

        // Load jump table entry into scratch and jump to it
//        masm.setUpRegister(value, new CiAddress(CiKind.Int, rscratch1.asValue(), value.asValue(), CiAddress.Scale.Times4, 0));
//        masm.add12BitImmediate(ConditionFlag.Always, false, rscratch1, value, 0);
        masm.jmp(rscratch1);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            masm.nop(4 - (buf.position() & 0x3));
        }

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos);
        masm.leaq(rscratch1, new CiAddress(target.wordKind, Aarch64.r15.asValue(), jumpTablePos - afterLea));
        buf.setPosition(jumpTablePos);

        // Emit jump table entries
        for (BlockBegin target : op.targets) {
            Aarch64Label label = new Aarch64Label(target.label());
            int offsetToJumpTableBase = buf.position() - jumpTablePos;
            if (label.isBound()) {
                int imm32 = label.position() - jumpTablePos;
                buf.emitInt(imm32);
            } else {
//                BranchInfo info = new BranchInfo(BranchType.TABLESWITCH, ConditionFlag.Always);
//                label.addPatchAt(buf.position(), info);
//                buf.emitInt((ConditionFlag.Always.value() << 28) | (offsetToJumpTableBase << 12) | 0x0d0);
            }
        }
        JumpTable jt = new JumpTable(jumpTablePos, op.lowKey, highKey, 4);
        tasm.targetMethod.addAnnotation(jt);
    }

    @Override
    protected void emitBranch(LIRBranch op) {
        assert assertEmitBranch(op);
        if (op.cond() == Condition.TRUE) {
            masm.b(new Aarch64Label(op.label()));
            if (op.info != null) {
                tasm.recordImplicitException(codePos() - 4, op.info); // ADDED EXCEPTION
            }
        } else {
            ConditionFlag acond = ConditionFlag.AL;
            if (op.code == LIROpcode.CondFloatBranch) {
                assert op.unorderedBlock() != null : "must have unordered successor";
                masm.branchConditionally(ConditionFlag.VS, new Aarch64Label(op.unorderedBlock().label()));

                // Checkstyle: off
                switch (op.cond()) {
                    case EQ:
                        acond = ConditionFlag.EQ;
                        break;
                    case NE:
                        acond = ConditionFlag.NE;
                        break;
                    case LT:
                        acond = ConditionFlag.LO;
                        break;
                    case LE:
                        acond = ConditionFlag.LS;
                        break;
                    case GE:
                        acond = ConditionFlag.GE;
                        break;
                    case GT:
                        acond = ConditionFlag.GT;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                switch (op.cond()) {
                    case EQ:
                        acond = ConditionFlag.EQ;
                        break;
                    case NE:
                        acond = ConditionFlag.NE;
                        break;
                    case LT:
                        acond = ConditionFlag.LT;
                        break;
                    case LE:
                        acond = ConditionFlag.LE;
                        break;
                    case GE:
                        acond = ConditionFlag.GE;
                        break;
                    case GT:
                        acond = ConditionFlag.GT;
                        break;
                    case BE:
                        acond = ConditionFlag.LS;
                        break;
                    case AE:
                        acond = ConditionFlag.HS;
                        break;
                    case BT:
                        acond = ConditionFlag.LO;
                        break;
                    case AT:
                        acond = ConditionFlag.HI;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
                // Checkstyle: on
            }
            masm.branchConditionally(acond, new Aarch64Label(op.label()));
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        CiValue src = op.operand();
        CiValue dest = op.result();
        switch (op.opcode) {
            case I2L:
                masm.sxt(64, 32, dest.asRegister(), src.asRegister());
                break;
            case L2I:
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFFFFFFFF);
                break;
            case I2B:
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFF);
                break;
            case I2C:
            case I2S:
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFFFF);
                break;
            case F2D:
                masm.fcvt(32, dest.asRegister(), src.asRegister());
                break;
            case D2F:
                masm.fcvt(64, dest.asRegister(), src.asRegister());
                break;
            case I2F:
                masm.scvtf(32, 32, dest.asRegister(), src.asRegister());
                break;
            case I2D:
                masm.scvtf(64, 32, dest.asRegister(), src.asRegister());
                break;
            case F2I:
                masm.fcvtzs(32, 32, dest.asRegister(), src.asRegister());
                break;
            case D2I:
                masm.fcvtzs(32, 64, dest.asRegister(), src.asRegister());
                break;
            case L2F:
                masm.scvtf(32, 64, dest.asRegister(), src.asRegister());
                break;
            case L2D:
                masm.scvtf(64, 64, dest.asRegister(), src.asRegister());
                break;
            case F2L:
                masm.fcvtzs(64, 32, dest.asRegister(), src.asRegister());
                break;
            case D2L:
                masm.fcvtzs(64, 64, dest.asRegister(), src.asRegister());
                break;
            case MOV_I2F:
                masm.fmovCpu2Fpu(32, dest.asRegister(), src.asRegister());
                break;
            case MOV_L2D:
                masm.fmovCpu2Fpu(64, dest.asRegister(), src.asRegister());
                break;
            case MOV_F2I:
                masm.fmovFpu2Cpu(32, dest.asRegister(), src.asRegister());
                break;
            case MOV_D2L:
                masm.fmovFpu2Cpu(64, dest.asRegister(), src.asRegister());
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        Aarch64Address address = Aarch64Address.createBaseRegisterOnlyAddress(op.address().asRegister());
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
        ConditionFlag acond;
        ConditionFlag ncond;
        switch (condition) {
            case EQ:
                acond = ConditionFlag.EQ;
                ncond = ConditionFlag.NE;
                break;
            case NE:
                ncond = ConditionFlag.EQ;
                acond = ConditionFlag.NE;
                break;
            case LT:
                acond = ConditionFlag.LT;
                ncond = ConditionFlag.GE;
                break;
            case LE:
                acond = ConditionFlag.LE;
                ncond = ConditionFlag.GT;
                break;
            case GE:
                acond = ConditionFlag.GE;
                ncond = ConditionFlag.LT;
                break;
            case GT:
                acond = ConditionFlag.GT;
                ncond = ConditionFlag.LE;
                break;
            case BE:
                acond = ConditionFlag.LS;
                ncond = ConditionFlag.HI;
                break;
            case BT:
                acond = ConditionFlag.LO;
                ncond = ConditionFlag.HS;
                break;
            case AE:
                acond = ConditionFlag.HS;
                ncond = ConditionFlag.LO;
                break;
            case AT:
                acond = ConditionFlag.HI;
                ncond = ConditionFlag.LS;
                break;
            default:
                throw Util.shouldNotReachHere();
        }

        CiValue def = opr1; // assume left operand as default
        CiValue other = opr2;

        if (opr2.isRegister() && opr2.asRegister() == result.asRegister()) {
            // if the right operand is already in the result register, then use it as the default
            def = opr2;
            other = opr1;
            // and flip the condition
            ConditionFlag tcond = acond;
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
                masm.setUpScratch(frameMap.toStackAddress(otherSlot));
                masm.ldr(64, rscratch1, Aarch64Address.createBaseRegisterOnlyAddress(rscratch1));
                masm.cmov(64, result.asRegister(), rscratch1, result.asRegister(), ncond);
            }
        } else {
            // conditional move not available, use emit a branch and move
            Aarch64Label skip = new Aarch64Label();
            masm.branchConditionally(acond, skip);
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
            CiRegister lreg = left.asRegister();
            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                if (kind.isInt()) {
                    switch (code) {
                        case Add:
                            masm.add(32, dest.asRegister(), lreg, rreg);
                            break;
                        case Sub:
                            masm.sub(32, dest.asRegister(), lreg, rreg);
                            break;
                        case Mul:
                            masm.mul(32, dest.asRegister(), lreg, rreg);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else if (kind.isFloat()) {
                    assert rreg.isFpu() : "must be xmm";
                    switch (code) {
                        case Add:
                            masm.fadd(32, dest.asRegister(), lreg, rreg);
                            break;
                        case Sub:
                            masm.fsub(32, dest.asRegister(), lreg, rreg);
                            break;
                        case Mul:
                            masm.fmul(32, dest.asRegister(), lreg, rreg);
                            break;
                        case Div:
                            masm.fdiv(32, dest.asRegister(), lreg, rreg);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else if (kind.isDouble()) {
                    assert rreg.isFpu();
                    switch (code) {
                        case Add:
                            masm.fadd(64, dest.asRegister(), lreg, rreg);
                            break;
                        case Sub:
                            masm.fsub(64, dest.asRegister(), lreg, rreg);
                            break;
                        case Mul:
                            masm.fmul(64, dest.asRegister(), lreg, rreg);
                            break;
                        case Div:
                            masm.fdiv(64, dest.asRegister(), lreg, rreg);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    assert kind.isLong();
                    switch (code) {
                        case Add:
                            masm.add(64, dest.asRegister(), lreg, rreg);
                            break;
                        case Sub:
                            masm.sub(64, dest.asRegister(), lreg, rreg);
                            break;
                        case Mul:
                            masm.mul(64, dest.asRegister(), lreg, rreg);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }
            } else {
                if (true) {
                    throw Util.unimplemented();
                }
                if (kind.isInt()) {
                    if (right.isStackSlot()) {
                        // register - stack
                        CiAddress raddr = frameMap.toStackAddress((CiStackSlot) right);
                        switch (code) {
                            case Add:
//                                masm.iadd(dest.asRegister(), lreg, raddr);
                                break;
                            case Sub:
//                                masm.isub(dest.asRegister(), lreg, raddr);
                                break;
                            default:
                                throw Util.shouldNotReachHere();
                        }
                    } else if (right.isConstant()) {
                        // register - constant
                        assert kind.isInt();
                        int delta = ((CiConstant) right).asInt();
                        switch (code) {
                            case Add:
//                                masm.incrementl(lreg, delta);
                                break;
                            case Sub:
//                                masm.decrementl(lreg, delta);
                                break;
                            default:
                                throw Util.shouldNotReachHere();
                        }
                    }
                } else if (kind.isFloat()) {
                    // register - stack/constant
                    CiAddress raddr;
                    if (right.isStackSlot()) {
                        raddr = frameMap.toStackAddress((CiStackSlot) right);
                        masm.load(Aarch64.d30, raddr, CiKind.Float);
                    } else {
                        assert right.isConstant();
                        raddr = tasm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) right).asFloat()));
                        masm.setUpScratch(raddr);
//                        masm.addRegisters(ConditionFlag.Always, false, Aarch64.r16, Aarch64.r16, Aarch64.r15, 0, 0);
//                        masm.vldr(ConditionFlag.Always, Aarch64.d30, Aarch64.r16, 0, CiKind.Float, CiKind.Int);
                    }
                    switch (code) {
                        case Add:
//                            masm.vadd(ConditionFlag.Always, lreg, lreg, Aarch64.d30, CiKind.Float);
                            break;
                        case Sub:
//                            masm.vsub(ConditionFlag.Always, lreg, lreg, Aarch64.d30, CiKind.Float);
                            break;
                        case Mul:
//                            masm.vmul(ConditionFlag.Always, lreg, lreg, Aarch64.d30, CiKind.Float);
                            break;
                        case Div:
//                            masm.vdiv(ConditionFlag.Always, lreg, lreg, Aarch64.d30, CiKind.Float);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else if (kind.isDouble()) {
                    // register - stack/constant
                    CiAddress raddr;
                    if (right.isStackSlot()) {
                        raddr = frameMap.toStackAddress((CiStackSlot) right);
                        masm.load(Aarch64.d30, raddr, CiKind.Double);
                    } else {
                        assert right.isConstant();
                        raddr = tasm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) right).asDouble()));
                        masm.setUpScratch(raddr);
                        masm.add(64, Aarch64.r16, Aarch64.r16, Aarch64.r15);
//                        masm.vldr(ConditionFlag.Always, Aarch64.d30, Aarch64.r16, 0, CiKind.Double, CiKind.Int);
                    }
                    switch (code) {
                        case Add:
                            masm.fadd(64, dest.asRegister(), lreg, Aarch64.d30);
                            break;
                        case Sub:
                            masm.fsub(64, dest.asRegister(), lreg, Aarch64.d30);
                            break;
                        case Mul:
                            masm.fmul(64, dest.asRegister(), lreg, Aarch64.d30);
                            break;
                        case Div:
                            masm.fdiv(64, dest.asRegister(), lreg, Aarch64.d30);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    assert target.sizeInBytes(kind) == 8;
                    if (right.isStackSlot()) {
                        // register - stack
                        assert right.kind == CiKind.Long;
                        CiAddress raddr = frameMap.toStackAddress((CiStackSlot) right);
                        masm.saveInFP(9);
                        masm.load(Aarch64.r8, raddr, CiKind.Long);
                        switch (code) {
                            case Add:
//                                masm.addLong(lreg, lreg, Aarch64.r8);
                                break;
                            case Sub:
//                                masm.subLong(lreg, lreg, Aarch64.r8);
                                break;
                            default:
                                throw Util.shouldNotReachHere();
                        }
                        //masm.vmov(ConditionFlag.Always, Aarch64.r9, Aarch64.d30, null, CiKind.Int, CiKind.Float);
                        masm.restoreFromFP(9);
                    } else {
                        // register - constant
                        assert right.isConstant();
                        long c = ((CiConstant) right).asLong();
                        masm.saveInFP(9);
                        masm.movlong(Aarch64.r8, c, CiKind.Long);
                        switch (code) {
                            case Add:
//                                masm.addLong(lreg, lreg, Aarch64.r8);
                                break;
                            case Sub:
//                                masm.subLong(lreg, lreg, Aarch64.r8);
                                break;
                            default:
                                throw Util.shouldNotReachHere();
                        }
                        masm.restoreFromFP(9);
                    }
                }
            }
        } else {
            assert kind.isInt();
            CiAddress laddr = asAddress(left);
            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                masm.load(Aarch64.r8, laddr, CiKind.Int);
                switch (code) {
                    case Add:
                        masm.add(32, Aarch64.r8, Aarch64.r8, rreg);
                        break;
                    case Sub:
                        masm.sub(32, Aarch64.r8, Aarch64.r8, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();

                }
                masm.store(Aarch64.r8, laddr, CiKind.Int);
            } else {
                if (true) {
                    throw Util.unimplemented();
                }
                assert right.isConstant();
                int c = ((CiConstant) right).asInt();
                switch (code) {
                    case Add:
//                        masm.incrementl(laddr, c);
                        break;
                    case Sub:
//                        masm.decrementl(laddr, c);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
        }
    }

    @Override
    protected void emitIntrinsicOp(LIROpcode code, CiValue value, CiValue unused, CiValue dest, LIROp2 op) {
        if (true) {
            throw Util.unimplemented();
        }

        assert value.kind.isDouble();
        switch (code) {
            case Abs:
                if (dest.asRegister() != value.asRegister()) {
//                    masm.vmov(ConditionFlag.Always, dest.asRegister(), value.asRegister(), null, CiKind.Double, CiKind.Double);
                }
//                masm.vmov(ConditionFlag.Always, Aarch64.d30, Aarch64.r9, null, CiKind.Float, CiKind.Int);
                masm.mov64BitConstant(Aarch64.r16, 0x7fffffff);
//                masm.vmov(ConditionFlag.Always, Aarch64.r8, dest.asRegister(), null, CiKind.Long, CiKind.Double);
//                masm.and(ConditionFlag.Always, false, Aarch64.r9, Aarch64.r9, Aarch64.r16, 0, 0);
//                masm.vmov(ConditionFlag.Always, dest.asRegister(), Aarch64.r8, Aarch64.r9, CiKind.Double, CiKind.Long);
//                masm.vmov(ConditionFlag.Always, Aarch64.r9, Aarch64.d30, null, CiKind.Int, CiKind.Float);
                break;
            case Sqrt:
//                masm.vsqrt(ConditionFlag.Always, dest.asRegister(), value.asRegister(), dest.kind);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitLogicOp(LIROpcode code, CiValue left, CiValue right, CiValue dst) {
        assert left.isRegister();
        assert dst.isRegister();
        if (left.kind.isInt()) {
            CiRegister reg = left.asRegister();
            if (right.isConstant()) {
                int val = ((CiConstant) right).asInt();
                switch (code) {
                    case LogicAnd:
                        masm.and(32, dst.asRegister(), reg, val);
                        break;
                    case LogicOr:
                        masm.or(32, dst.asRegister(), reg, val);
                        break;
                    case LogicXor:
                        masm.eor(32, dst.asRegister(), reg, val);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isStackSlot()) {
                if (true) {
                    throw Util.unimplemented();
                }
                // added support for stack operands
                CiAddress raddr = frameMap.toStackAddress((CiStackSlot) right);
                masm.setUpScratch(raddr);
//                masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, Aarch64.r8, Aarch64.r16, 0);
                assert reg != Aarch64.r16;
                switch (code) {
                    case LogicAnd:
//                        masm.iand(reg, reg, Aarch64.r8);
                        break;
                    case LogicOr:
//                        masm.ior(reg, reg, Aarch64.r8);
                        break;
                    case LogicXor:
//                        masm.ixor(reg, reg, Aarch64.r8);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                CiRegister rright = right.asRegister();
                switch (code) {
                    case LogicAnd:
                        masm.and(32, dst.asRegister(), reg, rright);
                        break;
                    case LogicOr:
                        masm.or(32, dst.asRegister(), reg, rright);
                        break;
                    case LogicXor:
                        masm.eor(32, dst.asRegister(), reg, rright);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister());
        } else {
            CiRegister lreg = left.asRegister();
            if (right.isConstant()) {
                long val = ((CiConstant) right).asLong();
                switch (code) {
                    case LogicAnd:
                        masm.and(64, dst.asRegister(), lreg, val);
                        break;
                    case LogicOr:
                        masm.or(64, dst.asRegister(), lreg, val);
                        break;
                    case LogicXor:
                        masm.eor(64, dst.asRegister(), lreg, val);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
                masm.restoreFromFP(9);
            } else {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case LogicAnd:
                        masm.and(64, dst.asRegister(), lreg, rreg);
                        break;
                    case LogicOr:
                        masm.or(64, dst.asRegister(), lreg, rreg);
                        break;
                    case LogicXor:
                        masm.eor(64, dst.asRegister(), lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            CiRegister dreg = dst.asRegister();
            moveRegs(lreg, dreg);
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
            Aarch64Label continuation = new Aarch64Label();
            if (C1XOptions.GenSpecialDivChecks) {
                // check for special case of MIN_VALUE / -1
                Aarch64Label normalCase = new Aarch64Label();
                masm.mov64BitConstant(rscratch1, size == 32 ? Integer.MIN_VALUE : Long.MIN_VALUE);
                masm.cmp(size, numerator, rscratch1);
                masm.branchConditionally(ConditionFlag.NE, normalCase);
                masm.cmp(size, denominator, -1);
                if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                    // prepare scratch for possible special case where remainder = 0
                    masm.mov(size, rscratch1, Aarch64.zr);
                }
                masm.branchConditionally(ConditionFlag.EQ, continuation);
                masm.bind(normalCase);
            }
            int offset = masm.codeBuffer.position();
            masm.sdiv(size, quotient, numerator, denominator);
            if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                masm.msub(size, rscratch1, quotient, denominator, numerator);
            }
            masm.bind(continuation);
            tasm.recordImplicitException(offset, info);
            if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                masm.mov(size, quotient, rscratch1);
            } else {
                assert code == LIROpcode.Idiv || code == LIROpcode.Ldiv;
            }
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

        int offset = masm.codeBuffer.position();
        tasm.recordImplicitException(offset, info);
        masm.udiv(size, quotient, numerator, denominator);
        if (code == LIROpcode.Iurem || code == LIROpcode.Lurem) {
            masm.msub(size, quotient, quotient, denominator, numerator);
        } else {
            assert code == LIROpcode.Iudiv || code == LIROpcode.Ludiv;
        }
    }

    private ConditionFlag convertCondition(Condition condition) {
        if (true) {
            throw Util.unimplemented();
        }

//        ConditionFlag acond = ConditionFlag.NeverUse;
        switch (condition) {
            case EQ:
//                acond = ConditionFlag.Equal;
                break;
            case NE:
//                acond = ConditionFlag.NotEqual;
                break;
            case LT:
//                acond = ConditionFlag.SignedLesser;
                break;
            case LE:
//                acond = ConditionFlag.SignedLowerOrEqual;
                break;
            case GE:
//                acond = ConditionFlag.SignedGreaterOrEqual;
                break;
            case GT:
//                acond = ConditionFlag.SignedGreater;
                break;
            case BE:
//                acond = ConditionFlag.UnsignedLowerOrEqual;
                break;
            case AE:
//                acond = ConditionFlag.CarrySetUnsignedHigherEqual;
                break;
            case BT:
//                acond = ConditionFlag.CarryClearUnsignedLower;
                break;
            case AT:
//                acond = ConditionFlag.UnsignedHigher;
                break;
            default:
                throw Util.shouldNotReachHere();
        }
//        return acond;
        return null; // TODO: remove
    }

    @Override
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
                        masm.cmp(32, reg1, opr2.asRegister());
                        break;
                    case Object:
                        masm.cmp(32, reg1, opr2.asRegister());
                        break;
                    case Long:
                        assert (reg1 != Aarch64.r16);
                        masm.cmp(64, reg1, opr2.asRegister());
                        break;
                    case Float:
                        masm.ucomisd(32, reg1, opr2.asRegister(), opr1.kind, opr2.kind);
                        break;
                    case Double:
                        masm.ucomisd(64, reg1, opr2.asRegister(), opr1.kind, opr2.kind);
                        break;
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
                        if (true) {
                            throw Util.unimplemented();
                        }

                        //                        masm.cmpl(reg1, frameMap.toStackAddress(opr2Slot));
                        break;
                    case Long:
                        if (true) {
                            throw Util.unimplemented();
                        }

                        //                        masm.vmov(ConditionFlag.Always, Aarch64.d30, Aarch64.r9, null, CiKind.Float, CiKind.Int);
                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
//                        masm.ldrd(ConditionFlag.Always, Aarch64.r8, Aarch64.r16, 0);
//                        masm.lcmpl(convertCondition(condition), reg1, Aarch64.r8);
//                        masm.vmov(ConditionFlag.Always, Aarch64.r9, Aarch64.d30, null, CiKind.Int, CiKind.Float);
                        break;
                    case Object:
                        if (true) {
                            throw Util.unimplemented();
                        }

                        //                        masm.cmpptr(reg1, frameMap.toStackAddress(opr2Slot));
                        break;
                    case Float:
                        if (true) {
                            throw Util.unimplemented();
                        }

                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
//                        masm.vldr(ConditionFlag.Always, Aarch64.d30, Aarch64.r16, 0, CiKind.Float, CiKind.Int);
//                        masm.ucomisd(reg1, Aarch64.d30, opr1.kind, CiKind.Float);
                        break;
                    case Double:
                        if (true) {
                            throw Util.unimplemented();
                        }

                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
//                        masm.vldr(ConditionFlag.Always, Aarch64.d15, Aarch64.r16, 0, CiKind.Double, CiKind.Int);
//                        masm.ucomisd(reg1, Aarch64.d15, opr1.kind, CiKind.Double);
                        break;
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
                        masm.cmp(32, reg1, c.asInt());
                        break;
                    case Float:
                        masm.setUpScratch(tasm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) opr2).asFloat())));
                        masm.add(32, Aarch64.r16, Aarch64.r16, Aarch64.r15);
                        masm.fldr(32, Aarch64.d30, Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.r16));
                        masm.ucomisd(32, reg1, Aarch64.d30, opr1.kind, CiKind.Float);
                        break;
                    case Double:
                        if (true) {
                            throw Util.unimplemented();
                        }

                        masm.setUpScratch(tasm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) opr2).asDouble())));
//                        masm.addRegisters(ConditionFlag.Always, false, Aarch64.r16, Aarch64.r16, Aarch64.r15, 0, 0);
//                        masm.vldr(ConditionFlag.Always, Aarch64.d15, Aarch64.r16, 0, CiKind.Double, CiKind.Int);
//                        masm.ucomisd(reg1, Aarch64.d15, opr1.kind, CiKind.Double);
                        break;
                    case Long: {
                        if (true) {
                            throw Util.unimplemented();
                        }

                        masm.saveInFP(9);
                        if (c.asLong() == 0) {
                            masm.movlong(Aarch64.r8, 0, CiKind.Long);
                        } else {
                            masm.movlong(Aarch64.r8, c.asLong(), CiKind.Long);
                        }
//                        masm.lcmpl(convertCondition(condition), reg1, Aarch64.r8);
                        masm.restoreFromFP(9);
                        break;
                    }
                    case Object: {
                        if (true) {
                            throw Util.unimplemented();
                        }

                        movoop(Aarch64.r8, c);
                        if (oldOpr1.isConstant()) {
                            CiValue newOpr1 = compilation.registerConfig.getScratchRegister().asValue(oldOpr1.kind);
                            const2reg(oldOpr1, newOpr1, null);
                            opr1 = newOpr1;
                            reg1 = opr1.asRegister();
                        }
//                        masm.cmpq(reg1, Aarch64.r8);
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

    @Override
    protected void emitCompare2Int(LIROpcode code, CiValue left, CiValue right, CiValue dst, LIROp2 op) {
        if (true) {
            throw Util.unimplemented();
        }

        if (code == LIROpcode.Cmpfd2i || code == LIROpcode.Ucmpfd2i) {
            if (left.kind.isFloat()) {
//                masm.cmpss2int(left.asRegister(), right.asRegister(), dst.asRegister(), code == LIROpcode.Ucmpfd2i, left.kind, right.kind);
            } else if (left.kind.isDouble()) {
//                masm.cmpsd2int(left.asRegister(), right.asRegister(), dst.asRegister(), code == LIROpcode.Ucmpfd2i, left.kind, right.kind);
            } else {
                throw Util.unimplemented("no fpu stack");
            }
        } else {
            assert code == LIROpcode.Cmpl2i;
            CiRegister dest = dst.asRegister();
            Aarch64Label high = new Aarch64Label();
            Aarch64Label done = new Aarch64Label();
            Aarch64Label isEqual = new Aarch64Label();
//            masm.lcmpl(ConditionFlag.Equal, left.asRegister(), right.asRegister());
//            masm.jcc(ConditionFlag.Equal, isEqual);
//            masm.lcmpl(ConditionFlag.SignedGreater, left.asRegister(), right.asRegister());
//            masm.jcc(ConditionFlag.SignedGreater, high);
//            masm.xorptr(dest, dest);
//            masm.decrementl(dest, 1);
//            masm.jmp(done);
            masm.bind(high);
//            masm.xorptr(dest, dest);
//            masm.incrementl(dest, 1);
//            masm.jmp(done);
            masm.bind(isEqual);
//            masm.xorptr(dest, dest);
            masm.bind(done);
        }
    }

    @Override
    protected void emitDirectCallAlignment() {
        masm.alignForPatchableDirectCall();
    }

    @Override
    protected void emitIndirectCall(Object target, LIRDebugInfo info, CiValue callAddress) {
        CiRegister reg = rscratch1;
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
        CiRegister reg = rscratch1;
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
        assert count.asRegister() == SHIFTCount : "count must be in r8";
        assert left == dest : "left and dest must be equal";
        assert tmp.isIllegal() : "wasting a register if tmp is allocated";
        assert left.isRegister();
        assert count.asRegister() != Aarch64.r16 : "count register must not be scratch";
        CiRegister register = left.asRegister();
        assert register != SHIFTCount : "left cannot be r8";
        int size = left.kind.isInt() ? 32 : 64;
        switch (code) {
            case Shl:
                masm.shl(size, register, register, count.asRegister());
                break;
            case Shr:
                masm.ashr(size, register, register, count.asRegister());
                break;
            case Ushr:
                masm.lshr(size, register, register, count.asRegister());
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
        int size = left.kind.isInt() ? 32 : 64;
        switch (code) {
            case Shl:
                masm.shl(size, register, register, count);
                break;
            case Shr:
                masm.ashr(size, register, register, count);
                break;
            case Ushr:
                masm.lshr(size, register, register, count);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitSignificantBitOp(boolean most, CiValue src, CiValue dst) {
        assert dst.isRegister();
        CiRegister result = dst.asRegister();
        masm.eor(64, result, result, result);
        masm.not(64, result, result);
        CiRegister value;
        if (src.isRegister()) {
            value = src.asRegister();
        } else {
            CiAddress laddr = asAddress(src);
            masm.setUpScratch(laddr);
            value = rscratch1;
        }
        assert value != result;
        if (most) {
            masm.clz(64, result, value);
            masm.mov64BitConstant(rscratch1, 63);
            masm.sub(64, result, rscratch1, result);
        } else {
            masm.rbit(64, rscratch1, value);
            masm.clz(64, result, rscratch1);
        }
    }

    @Override
    protected void emitAlignment() {
        if (true) {
            throw Util.unimplemented();
        }

//        masm.align(8);
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
        if (true) {
            throw Util.unimplemented();
        }

        assert src.isRegister();
        if (C1XOptions.NullCheckUniquePc) {
            masm.nop();
        }
//        masm.nullCheck(src.asRegister());
        tasm.recordImplicitException(codePos() - 4, info);
    }

    @Override
    protected void emitVolatileMove(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info) {
        assert kind == CiKind.Long : "only for volatile long fields";
        if (info != null) {
            tasm.recordImplicitException(codePos(), info);
        }
        assert false : "emitVolatileMove Aarch64IRAssembler";

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
        masm.membar(barriers);
    }

    @Override
    protected void emitDebugID(String methodName, String inlinedMethodName) {
        assert C1XOptions.DebugMethods;
        debugMethodWriter.appendDebugMethod(inlinedMethodName + " " + Integer.toHexString(masm.codeBuffer.position()) + " " + masm.codeBuffer.position(), methodID);
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
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;
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
                    masm.adr(dst, 0);
                    int afterLea = masm.codeBuffer.position();
                    masm.adr(dst, beforeLea - afterLea, beforeLea);
                    break;
                }
                case LoadEffectiveAddress: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;
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
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;
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
                    assert false : "RepeatMoveBytes in Aarch64LIRAssembler unimplemented";
                    masm.crashme();
                    break;
                case RepeatMoveWords:
                    assert false : "RepeatMoveWords in Aarch64LIRAssembler unimplemented";
                    masm.crashme();
                    break;
                case PointerCAS: {
                    assert false : "PointerCAS in Aarch64LIRAssembler unimplemented";
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
                        Aarch64Label label = new Aarch64Label(labels[((XirLabel) inst.extra).index]);
                        masm.b(label);
                    } else {
                        directJmp(inst.extra);
                    }
                    break;
                }
                case DecAndJumpNotZero: {
                    assert false : "DecAndJumpNotZero in Aarch64LIRAssembler unimplemented";
                    masm.crashme();
                    CiValue value = operands[inst.x().index];
                    if (value.kind == CiKind.Long) {
                        masm.decq(value.asRegister());
                    } else {
                        assert value.kind == CiKind.Int;
                    }
                    break;
                }
                case Jeq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
                    //                    emitXirCompare(inst, Condition.EQ, ConditionFlag.Equal, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    emitXirCompare(inst, Condition.NE, ConditionFlag.NotEqual, operands, label);
                    break;
                }
                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    emitXirCompare(inst, Condition.GT, ConditionFlag.SignedGreater, operands, label);
                    break;
                }
                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    emitXirCompare(inst, Condition.GE, ConditionFlag.SignedGreaterOrEqual, operands, label);
                    break;
                }
                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    emitXirCompare(inst, Condition.AE, ConditionFlag.CarrySetUnsignedHigherEqual, operands, label);
                    break;
                }
                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    emitXirCompare(inst, Condition.LT, ConditionFlag.SignedLesser, operands, label);
                    break;
                }
                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    emitXirCompare(inst, Condition.LE, ConditionFlag.SignedLowerOrEqual, operands, label);
                    break;
                }
                case Jbset: {
                    Aarch64Label label = new Aarch64Label(labels[((XirLabel) inst.extra).index]);
                    CiValue offset = operands[inst.y().index];
                    CiValue bit = operands[inst.z().index];
                    assert offset.isConstant() && bit.isConstant();
                    assert false;
                    masm.crashme();
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    masm.jcc(ConditionFlag.SignedGreaterOrEqual, label);
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
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    masm.nullCheck(pointer.asRegister());
                    tasm.recordImplicitException(codePos() - 4, info);
                    break;
                }
                case Align: {
                    if (true) {
                        throw Util.unimplemented();
                    }
//                    masm.align((Integer) inst.extra);
                    break;
                }
                case StackOverflowCheck: {
                    int frameSize = initialFrameSizeInBytes();
                    int lastFramePage = frameSize / target.pageSize;
                    // emit multiple stack bangs for methods with frames larger than a page
                    for (int i = 0; i <= lastFramePage; i++) {
                        int offset = (i + C1XOptions.StackShadowPages) * target.pageSize;
                        // Deduct 'frameSize' to handle frames larger than the shadow
                        bangStackWithOffset(offset - frameSize);
                    }
                    break;
                }
                case PushFrame: {
                    int frameSize = initialFrameSizeInBytes();
                    if (CompilationBroker.singleton.simulateAdapter()) {
                        masm.nop(4);
                    }

                    masm.push(Aarch64.linkRegister);

                    masm.sub(64, Aarch64.sp, Aarch64.sp, frameSize);
                    if (C1XOptions.ZapStackOnMethodEntry) {
                        final int intSize = 4;
                        for (int i = 0; i < frameSize / intSize; ++i) {
                            masm.setUpScratch(new CiAddress(CiKind.Int, Aarch64.sp.asValue(), i * intSize));
                            masm.mov64BitConstant(Aarch64.r8, 0xC1C1C1C1);
                            masm.str(64, Aarch64.r8, Aarch64Address.createRegisterOffsetAddress(Aarch64.r16, Aarch64.r0, false));
                        }
                    }

                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        assert frameToCSA >= 0;
                        masm.save(csl, frameToCSA);
                    }

                    if (C1XOptions.DebugMethods) {
                        masm.mov64BitConstant(Aarch64.r16, methodID);
                        debugMethodWriter.appendDebugMethod(compilation.method.holder() + "." + compilation.method.name() + ";" + compilation.method.signature(), methodID);
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
                    masm.add(64, Aarch64.sp, Aarch64.sp, frameSize);
                    break;
                }
                case Push: {
                    CiRegisterValue value = assureInRegister(operands[inst.x().index]);
                    if (value.asRegister().number <= Aarch64.zr.number) {
                        masm.push(value.asRegister());
                    } else {
                        if (true) {
                            throw Util.unimplemented();
                        }

//                        masm.vpush(ConditionFlag.Always, value.asRegister(), value.asRegister(), value.kind, value.kind);
                    }
                    break;
                }
                case Pop: {
                    CiValue result = operands[inst.result.index];
                    if (result.isRegister()) {
                        if (result.asRegister().number <= Aarch64.zr.number) {
                            masm.pop(result.asRegister());
                        } else {
                            if (true) {
                                throw Util.unimplemented();
                            }
                            //                            masm.vpop(ConditionFlag.Always, result.asRegister(), result.asRegister(), result.kind, result.kind);
                        }
                    } else {
                        masm.pop(rscratch1);
                        moveOp(rscratch1.asValue(), result, result.kind, null, true);
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

    /**
     * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
     *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack. For very large
     *            frames, this means that the offset may actually be negative (i.e. denoting a slot "up" the stack above
     *            RSP).
     */
    private void bangStackWithOffset(int offset) {
        masm.mov32BitConstant(rscratch1, offset);
        masm.sub(64, rscratch1, Aarch64.sp, rscratch1, ExtendType.UXTX, 0);
        masm.str(64, Aarch64.r0, Aarch64Address.createBaseRegisterOnlyAddress(rscratch1));
    }

    private CiRegisterValue assureInRegister(CiValue pointer) {
        if (pointer.isConstant()) {
            CiRegisterValue register = rscratch1.asValue(pointer.kind);
            moveOp(pointer, register, pointer.kind, null, false);
            return register;
        }
        assert pointer.isRegister() : "should be register, but is: " + pointer;
        return (CiRegisterValue) pointer;
    }

    private void emitXirCompare(XirInstruction inst, Condition condition, ConditionFlag cflag, CiValue[] ops, Label label) {
        if (true) {
            throw Util.unimplemented();
        }
//
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];
        emitCompare(condition, x, y, null);
//        masm.jcc(cflag, new Aarch64Label(label));
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
                if (true) {
                    throw Util.unimplemented();
                }
                //                masm.movptr(dst, 0);
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
                movoop(dst, c);
            } else {
                if (true) {
                    throw Util.unimplemented();
                }
//                masm.movptr(dst, c.asInt());
            }
        } else if (registerOrConstant.isRegister()) {
            masm.store(registerOrConstant.asRegister(), dst, k);
        } else {
            throw new InternalError("should not reach here");
        }
    }

    public void movoop(CiRegister dst, CiConstant obj) {
        assert obj.kind == CiKind.Object;
        if (obj.isNull()) {
            masm.xorq(dst, dst);
        } else {
            if (target.inlineObjects) {
                assert false : "Not implemented yet!";
                masm.setUpScratch(tasm.recordDataReferenceInCode(obj));
                masm.mov64BitConstant(Aarch64.r16, 0xdeaddead); // patched?
                masm.mov(64, dst, Aarch64.r16);
            } else {
                masm.setUpScratch(tasm.recordDataReferenceInCode(obj));
                if (true) {
                    throw Util.unimplemented();
                }
                // masm.addRegisters(ConditionFlag.Always, false, Aarch64.r16, Aarch64.r16, Aarch64.r15, 0, 0);
                // masm.ldr(ConditionFlag.Always, dst, Aarch64.r16, 0);
            }
        }
    }

    public void movoop(CiAddress dst, CiConstant obj) {
        movoop(Aarch64.r8, obj);
        masm.store(Aarch64.r8, dst, CiKind.Int);
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
        masm.nop(4);
    }

    public void directJmp(Object target) {
        if (true) {
            throw Util.unimplemented();
        }

        int before = masm.codeBuffer.position();
//        masm.jmp(0, true);
        int after = masm.codeBuffer.position();
        if (C1XOptions.EmitNopAfterCall) {
            masm.nop();
        }
        tasm.recordDirectCall(before, after - before, asCallTarget(target), null);
    }

    public void indirectCall(CiRegister src, Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
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
            masm.hlt();
        }
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }
}
