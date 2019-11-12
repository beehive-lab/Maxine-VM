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
import com.sun.max.unsafe.Word;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.FatalError;

public final class Aarch64LIRAssembler extends LIRAssembler {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final CiRegister SHIFTCount = Aarch64.r1;

    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    final CiTarget target;
    final Aarch64MacroAssembler masm;
    final CiRegister scratchRegister;

    public Aarch64LIRAssembler(C1XCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (Aarch64MacroAssembler) tasm.asm;
        target = compilation.target;
        scratchRegister = compilation.registerConfig.getScratchRegister();
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
                masm.adr(dst.asRegister(), 0);
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
    protected void emitGetTicks(CiValue result) {
        throw FatalError.unimplemented("Aarch64LIRAssembler.emitGetTicks");
    }

    @Override
    protected void emitGetCpuID(CiValue result) {
        throw FatalError.unimplemented("Aarch64LIRAssembler.emitGetCpuID");
    }

    @Override
    protected void emitBreakpoint() {
        masm.brk();
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

    @Override
    public void emitTraps() {
        for (int i = 0; i < C1XOptions.MethodEndBreakpointGuards; ++i) {
            masm.brk();
        }
        masm.nop(8);
    }

    private void const2reg(CiRegister dst, float constant) {
        masm.mov(scratchRegister, Float.floatToRawIntBits(constant));
        masm.fmov(32, dst, scratchRegister);
    }

    private void const2reg(CiRegister dst, double constant) {
        masm.mov(scratchRegister, Double.doubleToRawLongBits(constant));
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
                masm.mov(dest.asRegister(), c.asLong());
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
                masm.movz(64, scratchRegister, constant.asInt() & 0xFF, 0);
                break;
            case Char:
            case Short:
                masm.movz(64, scratchRegister, constant.asInt() & 0xFFFF, 0);
                break;
            case Jsr:
            case Int:
                masm.mov(scratchRegister, constant.asInt());
                break;
            case Float:
                masm.mov(scratchRegister, Float.floatToRawIntBits(constant.asFloat()));
                break;
            case Object:
                movoop(scratchRegister, constant);
                kind = CiKind.Long;
                break;
            case Long:
                masm.mov(scratchRegister, constant.asLong());
                kind = CiKind.Long;
                break;
            case Double:
                masm.mov(scratchRegister, Double.doubleToRawLongBits(constant.asDouble()));
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
        int highKey = op.lowKey + op.targets.length - 1;
        if (op.lowKey != 0) {
            // subtract the low value from the switch value
            masm.sub(64, value, value, (long) op.lowKey);
            masm.cmp(64, value, highKey - op.lowKey);
        } else {
            masm.cmp(64, value, highKey);
        }

        // Jump to default target if index is not within the jump table
        masm.branchConditionally(ConditionFlag.HI, op.defaultTarget.label());
        // Set scratch to address of jump table
        int adrPos = buf.position();
        masm.adr(scratchRegister, 0);

        // Load jump table entry into value and jump to it
        masm.add(64, value, scratchRegister, value, ShiftType.LSL, 2); // Shift left by 2 to make offset in bytes
        masm.jmp(value);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            masm.nop(4 - (buf.position() & 0x3));
        }

        // Patch setUpScratch instructions above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(adrPos);
        masm.adr(scratchRegister, jumpTablePos - adrPos);
        buf.setPosition(jumpTablePos);

        // Emit jump table entries
        for (BlockBegin target : op.targets) {
            Label label = target.label();
            if (label.isBound()) {
                masm.b(label.position() - buf.position());
            } else {
                label.addPatchAt(buf.position());
                buf.emitByte(PatchLabelKind.TABLE_SWITCH.encoding);
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
            ConditionFlag acond;
            if (op.code == LIROpcode.CondFloatBranch) {
                assert op.unorderedBlock() != null : "must have unordered successor";
                masm.branchConditionally(ConditionFlag.VS, op.unorderedBlock().label());

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
                acond = convertCondition(op.cond());
            }
            masm.branchConditionally(acond, op.label());
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
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFFFFFFFFL);
                break;
            case I2B:
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFFL);
                masm.sxt(64, 8, dest.asRegister(), src.asRegister());
                break;
            case I2C:
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFFFFL);
                break;
            case I2S:
                masm.and(64, dest.asRegister(), src.asRegister(), 0xFFFFL);
                masm.sxt(64, 16, dest.asRegister(), src.asRegister());
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
                masm.load(scratchRegister, frameMap.toStackAddress(otherSlot), other.kind);
                masm.cmov(64, result.asRegister(), scratchRegister, result.asRegister(), ncond);
            }
        } else {
            // conditional move not available, use emit a branch and move
            Label skip = new Label();
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
                    masm.load(Aarch64.d30, raddr, kind);
                    rreg = Aarch64.d30;
                }
            } else {
                assert right.isConstant();
                assert kind.isFloat() || kind.isDouble();
                if (kind.isFloat()) {
                    tasm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) right).asFloat()));
                } else {
                    tasm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) right).asDouble()));
                }
                masm.adr(scratchRegister, 0); // this gets patched by Aarch64InstructionDecoder.patchRelativeInstruction
                masm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                rreg = Aarch64.d30;
                masm.load(rreg, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister), kind);
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
            masm.load(scratchRegister, laddr, kind);
            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add:
                        masm.add(32, scratchRegister, scratchRegister, rreg);
                        break;
                    case Sub:
                        masm.sub(32, scratchRegister, scratchRegister, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();

                }
            } else {
                assert right.isConstant();
                int c = ((CiConstant) right).asInt();
                switch (code) {
                    case Add:
                        masm.add(32, scratchRegister, scratchRegister, (long) c);
                        break;
                    case Sub:
                        masm.sub(32, scratchRegister, scratchRegister, (long) c);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            masm.store(scratchRegister, laddr, kind);
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
                masm.and(size, dest, reg, rright);
                break;
            case LogicOr:
                masm.or(size, dest, reg, rright);
                break;
            case LogicXor:
                masm.eor(size, dest, reg, rright);
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
                masm.cmp(size, numerator, scratchRegister);
                masm.branchConditionally(ConditionFlag.NE, normalCase);
                masm.cmp(size, denominator, -1);
                if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                    // prepare scratch for possible special case where remainder = 0
                    masm.mov(quotient, 0);
                }
                masm.branchConditionally(ConditionFlag.EQ, continuation);
                masm.bind(normalCase);
            }
            int offset = masm.insertDivByZeroCheck(size, denominator);
            tasm.recordImplicitException(offset, info);
            if (code == LIROpcode.Irem || code == LIROpcode.Lrem) {
                if (quotient == numerator || quotient == denominator) {
                    quotient = scratchRegister;
                }
                masm.sdiv(size, quotient, numerator, denominator);
                masm.msub(size, result.asRegister(), quotient, denominator, numerator);
            } else {
                assert code == LIROpcode.Idiv || code == LIROpcode.Ldiv;
                masm.sdiv(size, quotient, numerator, denominator);
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

        int offset = masm.insertDivByZeroCheck(size, denominator);
        tasm.recordImplicitException(offset, info);
        if (code == LIROpcode.Iurem || code == LIROpcode.Lurem) {
            if (quotient == numerator || quotient == denominator) {
                quotient = scratchRegister;
            }
            masm.udiv(size, quotient, numerator, denominator);
            masm.msub(size, result.asRegister(), quotient, denominator, numerator);
        } else {
            assert code == LIROpcode.Iudiv || code == LIROpcode.Ludiv;
            masm.udiv(size, quotient, numerator, denominator);
        }
    }

    private ConditionFlag convertCondition(Condition condition) {
        ConditionFlag acond;
        switch (condition) {
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

                        masm.fmov(64, Aarch64.d30, Aarch64.r9);
                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
//                        masm.ldrd(ConditionFlag.Always, Aarch64.r17, Aarch64.r16, 0);
//                        masm.lcmpl(convertCondition(condition), reg1, Aarch64.r17);
                        masm.fmov(64, Aarch64.r9, Aarch64.d30);
                        break;
                    case Object:
                        if (true) {
                            throw Util.unimplemented();
                        }

                        //                        masm.cmpptr(reg1, frameMap.toStackAddress(opr2Slot));
                        break;
                    case Float:
                        masm.load(Aarch64.d30, frameMap.toStackAddress(opr2Slot), opr1.kind);
                        masm.ucomisd(32, reg1, Aarch64.d30, opr1.kind, CiKind.Float);
                        break;
                    case Double:
                        masm.load(Aarch64.d30, frameMap.toStackAddress(opr2Slot), opr1.kind);
                        masm.ucomisd(64, reg1, Aarch64.d30, opr1.kind, CiKind.Double);
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
                        assert false : "not tested";
                        tasm.recordDataReferenceInCode(CiConstant.forFloat(c.asFloat()));
                        masm.adr(scratchRegister, 0); // this gets patched by Aarch64InstructionDecoder.patchRelativeInstruction
                        masm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                        masm.fldr(32, Aarch64.d30, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
                        masm.ucomisd(32, reg1, Aarch64.d30, opr1.kind, CiKind.Float);
                        break;
                    case Double:
                        assert false : "not tested";
                        tasm.recordDataReferenceInCode(CiConstant.forDouble(c.asDouble()));
                        masm.adr(scratchRegister, 0); // this gets patched by Aarch64InstructionDecoder.patchRelativeInstruction
                        masm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                        masm.fldr(64, Aarch64.d15, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
                        masm.ucomisd(64, reg1, Aarch64.d15, opr1.kind, CiKind.Double);
                        break;
                    case Long: {
                        masm.cmp(64, reg1, c.asLong());
                        break;
                    }
                    case Object: {
                        if (c.isNull()) {
                            masm.cmp(64, reg1, 0);
                        } else {
                            movoop(scratchRegister, c);
                            masm.cmp(64, reg1, scratchRegister);
                        }
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
        CiRegister dest = dst.asRegister();
        if (code == LIROpcode.Cmpfd2i || code == LIROpcode.Ucmpfd2i) {
            assert left.kind.isFloat() || left.kind.isDouble();
            CiRegister lreg = left.asRegister();
            CiRegister rreg = right.asRegister();
            masm.fcmp(left.kind.isFloat() ? 32 : 64, lreg, rreg);

            Label l = new Label();
            if (code == LIROpcode.Ucmpfd2i) {
                masm.mov(dest, -1);
                masm.branchConditionally(ConditionFlag.VS, l);
                masm.branchConditionally(ConditionFlag.LO, l);
                masm.mov(dest, 0);
                masm.branchConditionally(ConditionFlag.EQ, l);
                masm.add(64, dest, dest, (long) 1);
            } else { // unordered is greater
                masm.mov(dest, 1);
                masm.branchConditionally(ConditionFlag.VS, l);
                masm.branchConditionally(ConditionFlag.HI, l);
                masm.mov(dest, 0);
                masm.branchConditionally(ConditionFlag.EQ, l);
                masm.sub(64, dest, dest, (long) 1);
            }
            masm.bind(l);

        } else {
            assert code == LIROpcode.Cmpl2i;
            Label high = new Label();
            Label done = new Label();
            Label isEqual = new Label();
            masm.cmp(64, left.asRegister(), right.asRegister());
            masm.branchConditionally(ConditionFlag.EQ, isEqual);
            masm.branchConditionally(ConditionFlag.GT, high);
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
        masm.alignForPatchableDirectCall();
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
        assert count.asRegister() == SHIFTCount : "count must be in r17";
        assert left == dest : "left and dest must be equal";
        assert tmp.isIllegal() : "wasting a register if tmp is allocated";
        assert left.isRegister();
        assert count.asRegister() != Aarch64.r16 : "count register must not be scratch";
        CiRegister register = left.asRegister();
        assert register != SHIFTCount : "left cannot be r17";
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
        masm.not(64, result, Aarch64.zr);
        Label end = new Label();
        masm.cbz(64, src.asRegister(), end);
        // else find the bit
        if (most) {
            masm.clz(64, result, value);
            masm.mov64BitConstant(scratchRegister, 63);
            masm.sub(64, result, scratchRegister, result);
        } else {
            masm.rbit(64, scratchRegister, value);
            masm.clz(64, result, scratchRegister);
        }
        masm.bind(end);
    }

    @Override
    protected void emitAlignment() {
        masm.align(8);
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
                        Label label = labels[((XirLabel) inst.extra).index];
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
                    emitXirCompare(inst, Condition.EQ, ConditionFlag.EQ, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.NE, ConditionFlag.NE, operands, label);
                    break;
                }
                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GT, ConditionFlag.GT, operands, label);
                    break;
                }
                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GE, ConditionFlag.GE, operands, label);
                    break;
                }
                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.AE, ConditionFlag.HS, operands, label);
                    break;
                }
                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LT, ConditionFlag.LT, operands, label);
                    break;
                }
                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LE, ConditionFlag.LE, operands, label);
                    break;
                }
                case Jbset: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    CiValue offset = operands[inst.y().index];
                    CiValue bit = operands[inst.z().index];
                    assert offset.isConstant() && bit.isConstant();
                    assert false;
                    masm.crashme();
                    masm.branchConditionally(ConditionFlag.GE, label);
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

                    masm.push(Aarch64.linkRegister);

                    if (C1XOptions.ZapStackOnMethodEntry) {
                        masm.mov(scratchRegister, 0xC1C1C1C1_C1C1C1C1L);
                        for (int i = 0; i < frameSize / (2 * Word.size()); ++i) {
                            masm.stp(Word.width(), scratchRegister, scratchRegister, Aarch64Address.createPreIndexedImmediateAddress(Aarch64.sp, -2 * Word.size()));
                        }
                    } else {
                        masm.sub(64, Aarch64.sp, Aarch64.sp, frameSize);
                    }

                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        assert frameToCSA >= 0;
                        masm.save(csl, frameToCSA);
                    }

                    if (C1XOptions.DebugMethods) {
                        masm.mov32BitConstant(masm.scratchRegister, methodID);
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
                    masm.add(64, Aarch64.sp, Aarch64.sp, frameSize);
                    break;
                }
                case Push: {
                    CiRegisterValue value = assureInRegister(operands[inst.x().index]);
                    if (value.asRegister().number <= Aarch64.zr.number) {
                        masm.push(value.asRegister());
                    } else {
                        masm.fpush(value.asRegister());
                    }
                    break;
                }
                case Pop: {
                    CiValue result = operands[inst.result.index];
                    if (result.isRegister()) {
                        if (result.asRegister().number <= Aarch64.zr.number) {
                            masm.pop(result.asRegister());
                        } else {
                            masm.fpop(result.asRegister());
                        }
                    } else {
                        masm.pop(scratchRegister);
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

    private void emitXirCompare(XirInstruction inst, Condition condition, ConditionFlag cflag, CiValue[] ops, Label label) {
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];
        emitCompare(condition, x, y, null);
        masm.branchConditionally(cflag, new Label(label.getPatchPositions(), label.positionCopy()));
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
                masm.adr(scratchRegister, 0); // this gets patched by Aarch64InstructionDecoder.patchRelativeInstruction
                masm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                masm.load(dst, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister), obj.kind);
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
            masm.hlt();
        }
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }
}
