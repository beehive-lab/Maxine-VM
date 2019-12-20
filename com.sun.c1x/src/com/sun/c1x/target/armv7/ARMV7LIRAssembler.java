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
package com.sun.c1x.target.armv7;

import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.cri.ci.CiValue.*;
import static com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.oracle.max.asm.target.armv7.ARMV7Label.*;
import com.oracle.max.asm.target.armv7.ARMV7Label.BranchInfo.*;
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
import com.sun.max.vm.runtime.FatalError;

public final class ARMV7LIRAssembler extends LIRAssembler {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final CiRegister SHIFTCount = ARMV7.r1;

    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    final CiTarget target;
    final ARMV7MacroAssembler masm;
    final CiRegister rscratch1;

    public ARMV7LIRAssembler(C1XCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (ARMV7MacroAssembler) tasm.asm;
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
        throw Util.unimplemented();
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
                masm.leaq(dst.asRegister(), CiAddress.Placeholder);
                int afterLea = masm.codeBuffer.position();
                masm.codeBuffer.setPosition(beforeLea);
                masm.leaq(dst.asRegister(), new CiAddress(target.wordKind, ARMV7.rip.asValue(), beforeLea - afterLea));
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
        masm.leaq(dst.asRegister(), new CiAddress(slot.kind, ARMV7.r13.asValue(), slot.index() * target.arch.wordSize));
    }

    @Override
    protected void emitPause() {
        masm.pause();
    }

    @Override
    protected void emitGetTicks(CiValue result) {
        throw FatalError.unimplemented("ARMV7LIRAssembler.emitGetTicks");
    }

    @Override
    protected void emitGetCpuID(CiValue result) {
        throw FatalError.unimplemented("ARMV7LIRAssembler.emitGetCpuID");
    }

    @Override
    protected void emitBreakpoint() {
        masm.int3();
    }

    @Override
    protected void emitIfBit(CiValue address, CiValue bitNo) {
        assert false : "emitIfBit ARMV7IRAssembler";
        masm.crashme();
        masm.insertForeverLoop();
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, CiValue dst) {
        masm.leaq(dst.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    private void moveRegs(CiRegister fromReg, CiRegister toReg) {
        if (fromReg != toReg) {
            masm.mov(ConditionFlag.Always, false, toReg, fromReg);
        }
    }

    private void moveRegs(CiRegister src, CiRegister dest, CiKind srcKind, CiKind destKind) {
        if (src != dest) {
            if (srcKind == CiKind.Long && destKind == CiKind.Long) {
                masm.mov(ConditionFlag.Always, false, dest, src);
                masm.mov(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[src.number + 1]);
            } else if (srcKind == CiKind.Int && destKind == CiKind.Long) {
                assert srcKind != CiKind.Float;
                assert srcKind != CiKind.Double;
                masm.mov(ConditionFlag.Always, false, dest, src);
                masm.asr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], dest, 31);
            } else {
                assert srcKind != CiKind.Float;
                assert srcKind != CiKind.Double;
                masm.mov(ConditionFlag.Always, false, dest, src);
            }
        } else if (srcKind == CiKind.Int && destKind == CiKind.Long) {
            assert src == dest;
            masm.asr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], dest, 31);
        }
    }

    private void swapReg(CiRegister a, CiRegister b) {
        masm.xchgptr(a, b);
    }

    private void const2reg(CiRegister dst, int constant) {
        masm.movImmediate(ConditionFlag.Always, dst, constant);
    }

    private void const2reg(CiRegister dst, long constant, CiKind dstKind) {
        masm.movlong(dst, constant, dstKind);
    }

    private void const2reg(CiRegister dst, CiConstant constant) {
        assert constant.kind == CiKind.Object;
        if (constant.isNull()) {
            masm.movImmediate(ConditionFlag.Always, dst, 0x0);
        } else if (target.inlineObjects) {
            tasm.recordDataReferenceInCode(constant);
            masm.movImmediate(ConditionFlag.Always, dst, 0xDEADDEAD);
        } else {
            masm.setUpScratch(tasm.recordDataReferenceInCode(constant));
            masm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
            masm.ldr(ConditionFlag.Always, dst, ARMV7.r12, 0);
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
        masm.movImmediate(ConditionFlag.Always, rscratch1, Float.floatToRawIntBits(constant));
        masm.vmov(ConditionFlag.Always, dst, rscratch1, null, dstKind, CiKind.Int);
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
                masm.movImmediate(ConditionFlag.Always, ARMV7.r8, c.asInt());
                masm.store(ARMV7.r8, address, CiKind.Int);
                break;
            case Float:
                masm.movImmediate(ConditionFlag.Always, ARMV7.r8, Float.floatToRawIntBits(c.asFloat()));
                masm.store(ARMV7.r8, address, CiKind.Int);
                break;
            case Object:
                movoop(frameMap.toStackAddress(slot), c);
                break;
            case Long:
                masm.saveInFP(9);
                masm.movlong(ARMV7.r8, c.asLong(), CiKind.Long);
                masm.store(ARMV7.r8, address, CiKind.Long);
                masm.restoreFromFP(9);
                break;
            case Double:
                masm.saveInFP(9);
                masm.movlong(ARMV7.r8, Double.doubleToRawLongBits(c.asDouble()), CiKind.Long);
                masm.store(ARMV7.r8, address, CiKind.Long);
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
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, constant.asInt() & 0xFF);
                } else if (kind == CiKind.Char || kind == CiKind.Short) {
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, constant.asInt() & 0xFFFF);
                } else {
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, constant.asInt());
                }
                masm.store(ARMV7.r8, addr, CiKind.Int);
                nullCheckHere = codePos() - 4;
                break;
            case Float:
                masm.movImmediate(ConditionFlag.Always, ARMV7.r8, Float.floatToRawIntBits(constant.asFloat()));
                masm.store(ARMV7.r8, addr, CiKind.Int);
                nullCheckHere = codePos() - 4;
                break;
            case Object:
                movoop(addr, constant);
                break;
            case Long:
                masm.saveInFP(9);
                masm.movlong(ARMV7.r8, constant.asLong(), CiKind.Long);
                masm.store(ARMV7.r8, addr, CiKind.Long);
                nullCheckHere = codePos() - 4;
                masm.restoreFromFP(9);
                break;
            case Double:
                masm.saveInFP(9);
                masm.movlong(ARMV7.r8, Double.doubleToRawLongBits(constant.asDouble()), CiKind.Long);
                masm.store(ARMV7.r8, addr, CiKind.Long);
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
            masm.movflt(dest.asRegister(), src.asRegister(), dest.kind, src.kind);
        } else if (dest.kind.isDouble()) {
            masm.movdbl(dest.asRegister(), src.asRegister(), dest.kind, src.kind);
        } else {
            moveRegs(src.asRegister(), dest.asRegister(), src.kind, dest.kind);
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
        // Checkstyle: off
        CiRegister tmpRegister = destAddress.base();
        if (tmpRegister == CiRegister.Frame) {
            tmpRegister = masm.frameRegister;
        }
        switch (kind) {
            case Float:
                masm.store(src.asRegister(), destAddress, CiKind.Float);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Double:
                masm.store(src.asRegister(), destAddress, CiKind.Double);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Jsr:
            case Object:
            case Int:
                masm.store(src.asRegister(), destAddress, CiKind.Int);
                tasm.recordImplicitException(codePos() - 4, info);
                break;
            case Long:
                masm.store(src.asRegister(), destAddress, CiKind.Long);
                if (info != null) {
                    tasm.recordImplicitException(codePos() - 4, info);
                }
                break;
            case Char:
            case Short:
                masm.setUpScratch(destAddress);
                if (info != null) {
                    tasm.recordImplicitException(codePos(), info);
                }
                masm.strHImmediate(ConditionFlag.Always, 1, 0, 0, src.asRegister(), ARMV7.r12, 0);
                break;
            case Byte:
            case Boolean:
                masm.setUpScratch(destAddress);
                if (info != null) {
                    tasm.recordImplicitException(codePos(), info);
                }
                masm.strbImmediate(ConditionFlag.Always, 1, 0, 0, src.asRegister(), ARMV7.r12, 0);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
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
            masm.load(ARMV7.r12, (CiAddress) src, CiKind.Int);
            masm.store(ARMV7.r12, (CiAddress) dest, CiKind.Int);
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
            masm.load(ARMV7.r8, frameMap.toStackAddress((CiStackSlot) src), CiKind.Long);
            masm.store(ARMV7.r8, frameMap.toStackAddress((CiStackSlot) dest), CiKind.Long);
            masm.restoreFromFP(9);
        } else {
            masm.load(ARMV7.r8, frameMap.toStackAddress((CiStackSlot) src), CiKind.Int);
            masm.store(ARMV7.r8, frameMap.toStackAddress((CiStackSlot) dest), CiKind.Int);
        }
    }

    @Override
    protected void mem2reg(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        assert src.isAddress();
        assert dest.isRegister() : "dest=" + dest;
        CiAddress addr = (CiAddress) src;
        boolean safepoint = addr.base().isValid() && addr.displacement == 0 && addr.base().compareTo(ARMV7.LATCH_REGISTER) == 0;
        assert !(!safepoint && dest.asRegister().equals(ARMV7.LATCH_REGISTER));
        if (addr.base().compareTo(ARMV7.LATCH_REGISTER) == 0 && addr.displacement == 0) {
            assert dest.asRegister().equals(ARMV7.LATCH_REGISTER) : dest.asRegister().number;
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
                    masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, dest.asRegister(), ARMV7.LATCH_REGISTER, 0);
                } else {
                    masm.setUpScratch(addr);
                    masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, dest.asRegister(), safepoint ? ARMV7.LATCH_REGISTER : ARMV7.r12, 0);
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
                arithmeticIdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Iudiv:
            case Iurem:
                arithmeticIudiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Ldiv:
            case Lrem:
                arithmeticLdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Ludiv:
            case Lurem:
                arithmeticLudiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case LDivExceptionCheck:
                arithmeticLDivExceptionCheck(op.opr1(), op.opr2(), op.result(), op.info);
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
            masm.subq(value, op.lowKey);
            masm.cmpl(value, highKey - op.lowKey);
        } else {
            masm.cmpl(value, highKey);
        }

        // Jump to default target if index is not within the jump table
        masm.jcc(ConditionFlag.UnsignedHigher, new ARMV7Label(op.defaultTarget.label()));
        // Set scratch to address of jump table
        int leaPos = buf.position();
        masm.leaq(rscratch1, CiAddress.Placeholder);
        int afterLea = buf.position();

        // Load jump table entry into scratch and jump to it
        masm.setUpRegister(value, new CiAddress(CiKind.Int, rscratch1.asValue(), value.asValue(), CiAddress.Scale.Times4, 0));
        masm.add12BitImmediate(ConditionFlag.Always, false, rscratch1, value, 0);
        masm.jmp(rscratch1);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            masm.nop(4 - (buf.position() & 0x3));
        }

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos);
        masm.leaq(rscratch1, new CiAddress(target.wordKind, ARMV7.r15.asValue(), jumpTablePos - afterLea));
        buf.setPosition(jumpTablePos);

        // Emit jump table entries
        for (BlockBegin target : op.targets) {
            ARMV7Label label = new ARMV7Label(target.label());
            int offsetToJumpTableBase = buf.position() - jumpTablePos;
            if (label.isBound()) {
                masm.b(ConditionFlag.Always, label.position() - jumpTablePos);
            } else {
                BranchInfo info = new BranchInfo(BranchType.TABLESWITCH, ConditionFlag.Always);
                label.addPatchAt(buf.position(), info);
                buf.emitInt((ConditionFlag.Always.value() << 28) | (offsetToJumpTableBase << 12) | 0x0d0);
            }
        }
        JumpTable jt = new JumpTable(jumpTablePos, op.lowKey, highKey, 4);
        tasm.targetMethod.addAnnotation(jt);
    }

    @Override
    protected void emitBranch(LIRBranch op) {
        assert assertEmitBranch(op);
        if (op.cond() == Condition.TRUE) {
            masm.jmp(new ARMV7Label(op.label()));
            if (op.info != null) {
                tasm.recordImplicitException(codePos() - 4, op.info); // ADDED EXCEPTION
            }
        } else {
            ConditionFlag acond = ConditionFlag.Always;
            if (op.code == LIROpcode.CondFloatBranch) {
                assert op.unorderedBlock() != null : "must have unordered successor";
                masm.jcc(ConditionFlag.SignedOverflow, new ARMV7Label(op.unorderedBlock().label()));

                // Checkstyle: off
                switch (op.cond()) {
                    case EQ:
                        acond = ConditionFlag.Equal;
                        break;
                    case NE:
                        acond = ConditionFlag.NotEqual;
                        break;
                    case LT:
                        acond = ConditionFlag.CarryClearUnsignedLower;
                        break;
                    case LE:
                        acond = ConditionFlag.UnsignedLowerOrEqual;
                        break;
                    case GE:
                        acond = ConditionFlag.SignedGreaterOrEqual;
                        break;
                    case GT:
                        acond = ConditionFlag.SignedGreater;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                switch (op.cond()) {
                    case EQ:
                        acond = ConditionFlag.Equal;
                        break;
                    case NE:
                        acond = ConditionFlag.NotEqual;
                        break;
                    case LT:
                        acond = ConditionFlag.SignedLesser;
                        break;
                    case LE:
                        acond = ConditionFlag.SignedLowerOrEqual;
                        break;
                    case GE:
                        acond = ConditionFlag.SignedGreaterOrEqual;
                        break;
                    case GT:
                        acond = ConditionFlag.SignedGreater;
                        break;
                    case BE:
                        acond = ConditionFlag.UnsignedLowerOrEqual;
                        break;
                    case AE:
                        acond = ConditionFlag.CarrySetUnsignedHigherEqual;
                        break;
                    case BT:
                        acond = ConditionFlag.CarryClearUnsignedLower;
                        break;
                    case AT:
                        acond = ConditionFlag.UnsignedHigher;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
                // Checkstyle: on
            }
            masm.jcc(acond, new ARMV7Label(op.label()));
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        CiValue src = op.operand();
        CiValue dest = op.result();
        CiRegister srcRegister = src.asRegister();
        switch (op.opcode) {
            case I2L:
                moveRegs(srcRegister, dest.asRegister(), src.kind, dest.kind);
                break;
            case L2I:
                moveRegs(srcRegister, dest.asRegister(), src.kind, dest.kind);
                break;
            case I2B:
                moveRegs(srcRegister, dest.asRegister());
                masm.sxtb(ConditionFlag.Always, dest.asRegister(), srcRegister);
                break;
            case I2C:
                masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 0xFFFF);
                masm.and(ConditionFlag.Always, true, dest.asRegister(), ARMV7.r12, src.asRegister(), 0, 0);
                break;
            case I2S:
                moveRegs(srcRegister, dest.asRegister());
                masm.sxth(ConditionFlag.Always, dest.asRegister(), srcRegister);
                break;
            case F2D:
                masm.vcvt(ConditionFlag.Always, dest.asRegister(), false, false, src.asRegister(), dest.kind, src.kind);
                break;
            case D2F:
                masm.vcvt(ConditionFlag.Always, dest.asRegister(), false, false, src.asRegister(), CiKind.Float, CiKind.Double);
                break;
            case I2F:
                masm.vmov(ConditionFlag.Always, ARMV7.s30, srcRegister, null, CiKind.Float, src.kind);
                masm.vcvt(ConditionFlag.Always, dest.asRegister(), false, true, ARMV7.s30, CiKind.Float, CiKind.Int);
                break;
            case I2D:
                masm.vmov(ConditionFlag.Always, ARMV7.s30, srcRegister, null, CiKind.Float, src.kind);
                masm.vcvt(ConditionFlag.Always, dest.asRegister(), false, true, ARMV7.s30, CiKind.Double, CiKind.Int);
                break;
            case F2I: {
                assert srcRegister.isFpu() && dest.isRegister() : "must both be S-register (no fpu stack)";
                masm.vcvt(ConditionFlag.Always, ARMV7.s30, true, true, src.asRegister(), CiKind.Float, src.kind);
                masm.vmov(ConditionFlag.Always, dest.asRegister(), ARMV7.s30, null, dest.kind, CiKind.Float);
                break;
            }
            case D2I: {
                assert srcRegister.isFpu() && dest.isRegister() : "must both be S-register (no fpu stack)";
                if (dest.asRegister().isFpu()) {
                    masm.vcvt(ConditionFlag.Always, dest.asRegister(), true, true, src.asRegister(), dest.kind, src.kind);
                } else {
                    masm.vcvt(ConditionFlag.Always, ARMV7.s30, true, true, src.asRegister(), dest.kind, src.kind);
                    masm.vmov(ConditionFlag.Always, dest.asRegister(), ARMV7.s30, null, dest.kind, CiKind.Float);
                }
                break;
            }
            case L2F:
                assert false : "L2F is implemented as runtime call!";
                break;
            case L2D:
                assert false : "L2D is implemented as runtime call!";
                break;
            case F2L:
                assert false : "F2L is implemented as runtime call!";
                break;
            case D2L:
                assert false : "D2L is implemented as runtime call!";
                break;
            case MOV_I2F:
                masm.vmov(ConditionFlag.Always, dest.asRegister(), srcRegister, null, CiKind.Float, CiKind.Int);
                break;
            case MOV_L2D:
                masm.vmov(ConditionFlag.Always, dest.asRegister(), srcRegister, ARMV7.cpuRegisters[srcRegister.getEncoding() + 1], CiKind.Double, CiKind.Long);
                break;
            case MOV_F2I:
                masm.vmov(ConditionFlag.Always, dest.asRegister(), srcRegister, null, CiKind.Int, CiKind.Float);
                break;
            case MOV_D2L:
                masm.vmov(ConditionFlag.Always, dest.asRegister(), srcRegister, null, CiKind.Long, CiKind.Double);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        CiAddress address = new CiAddress(CiKind.Object, op.address(), 0);
        CiRegister newval = op.newValue().asRegister();
        CiRegister cmpval = op.expectedValue().asRegister();
        assert newval != null : "new val must be register";
        assert cmpval != newval : "cmp and new values must be in different registers";
        assert cmpval != address.base() : "cmp and addr must be in different registers";
        assert newval != address.base() : "new value and addr must be in different registers";
        assert cmpval != address.index() : "cmp and addr must be in different registers";
        assert newval != address.index() : "new value and addr must be in different registers";
        if (compilation.target.isMP) {
            masm.membar(-1);
        }
        if (op.code == LIROpcode.CasInt || op.code == LIROpcode.CasObj) {
            masm.casInt(newval, cmpval, address);
        } else {
            assert op.code == LIROpcode.CasLong;
            masm.casLong(newval, cmpval, address);
        }
        if (compilation.target.isMP) {

            masm.membar(-1);
        }
    }

    @Override
    protected void emitConditionalMove(Condition condition, CiValue opr1, CiValue opr2, CiValue result) {
        ConditionFlag acond;
        ConditionFlag ncond;
        switch (condition) {
            case EQ:
                acond = ConditionFlag.Equal;
                ncond = ConditionFlag.NotEqual;
                break;
            case NE:
                ncond = ConditionFlag.Equal;
                acond = ConditionFlag.NotEqual;
                break;
            case LT:
                acond = ConditionFlag.SignedLesser;
                ncond = ConditionFlag.SignedGreaterOrEqual;
                break;
            case LE:
                acond = ConditionFlag.SignedLowerOrEqual;
                ncond = ConditionFlag.SignedGreater;
                break;
            case GE:
                acond = ConditionFlag.SignedGreaterOrEqual;
                ncond = ConditionFlag.SignedLesser;
                break;
            case GT:
                acond = ConditionFlag.SignedGreater;
                ncond = ConditionFlag.SignedLowerOrEqual;
                break;
            case BE:
                acond = ConditionFlag.UnsignedLowerOrEqual;
                ncond = ConditionFlag.UnsignedHigher;
                break;
            case BT:
                acond = ConditionFlag.CarryClearUnsignedLower;
                ncond = ConditionFlag.CarrySetUnsignedHigherEqual;
                break;
            case AE:
                acond = ConditionFlag.CarrySetUnsignedHigherEqual;
                ncond = ConditionFlag.CarryClearUnsignedLower;
                break;
            case AT:
                acond = ConditionFlag.UnsignedHigher;
                ncond = ConditionFlag.UnsignedLowerOrEqual;
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
                    masm.mov(ncond, false, result.asRegister(), other.asRegister());
                } else {
                    masm.mov(ncond, false, result.asRegister(), other.asRegister());
                }
            } else {
                assert other.isStackSlot();
                CiStackSlot otherSlot = (CiStackSlot) other;
                masm.setUpScratch(frameMap.toStackAddress(otherSlot));
                masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, ARMV7.r12, ARMV7.r12, 0);
                masm.mov(ncond, false, result.asRegister(), ARMV7.r12);
            }
        } else {
            // conditional move not available, use emit a branch and move
            ARMV7Label skip = new ARMV7Label();
            masm.jcc(acond, skip);
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
        assert Util.archKindsEqual(left.kind, right.kind) || (left.kind == CiKind.Long && right.kind == CiKind.Int) : code.toString() + " left arch is " + left.kind + " and right arch is " +
                        right.kind;
        assert left.equals(dest) : "left and dest must be equal";
        CiKind kind = left.kind;

        // Checkstyle: off
        if (left.isRegister()) {
            CiRegister lreg = left.asRegister();
            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                if (kind.isInt()) {
                    switch (code) {
                        case Add:
                            masm.iadd(dest.asRegister(), lreg, rreg);
                            break;
                        case Sub:
                            masm.isub(dest.asRegister(), lreg, rreg);
                            break;
                        case Mul:
                            masm.imul(dest.asRegister(), lreg, rreg);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else if (kind.isFloat()) {
                    assert rreg.isFpu() : "must be xmm";
                    switch (code) {
                        case Add:
                            masm.vadd(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Float);
                            break;
                        case Sub:
                            masm.vsub(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Float);
                            break;
                        case Mul:
                            masm.vmul(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Float);
                            break;
                        case Div:
                            masm.vdiv(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Float);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else if (kind.isDouble()) {
                    assert rreg.isFpu();
                    switch (code) {
                        case Add:
                            masm.vadd(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Double);
                            break;
                        case Sub:
                            masm.vsub(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Double);
                            break;
                        case Mul:
                            masm.vmul(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Double);
                            break;
                        case Div:
                            masm.vdiv(ConditionFlag.Always, lreg, lreg, rreg, CiKind.Double);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    assert kind.isLong();
                    switch (code) {
                        case Add:
                            masm.addLong(dest.asRegister(), lreg, rreg);
                            break;
                        case Sub:
                            masm.subLong(dest.asRegister(), lreg, rreg);
                            break;
                        case Mul:
                            masm.mulLong(dest.asRegister(), lreg, rreg);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }
            } else {
                if (kind.isInt()) {
                    if (right.isStackSlot()) {
                        // register - stack
                        assert false : "FZ: Assuming this is dead code on ARMv7";
                        CiAddress raddr = frameMap.toStackAddress(((CiStackSlot) right));
                        switch (code) {
                            case Add:
                                masm.iadd(dest.asRegister(), lreg, raddr);
                                break;
                            case Sub:
                                masm.isub(dest.asRegister(), lreg, raddr);
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
                                masm.incrementl(lreg, delta);
                                break;
                            case Sub:
                                masm.decrementl(lreg, delta);
                                break;
                            default:
                                throw Util.shouldNotReachHere();
                        }
                    }
                } else if (kind.isFloat()) {
                    // register - stack/constant
                    CiAddress raddr;
                    if (right.isStackSlot()) {
                        raddr = frameMap.toStackAddress(((CiStackSlot) right));
                        masm.load( ARMV7.s30, raddr, CiKind.Float);
                    } else {
                        assert right.isConstant();
                        raddr = tasm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) right).asFloat()));
                        masm.setUpScratch(raddr);
                        masm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                        masm.vldr(ConditionFlag.Always, ARMV7.s30, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
                    }
                    switch (code) {
                        case Add:
                            masm.vadd(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Float);
                            break;
                        case Sub:
                            masm.vsub(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Float);
                            break;
                        case Mul:
                            masm.vmul(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Float);
                            break;
                        case Div:
                            masm.vdiv(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Float);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else if (kind.isDouble()) {
                    // register - stack/constant
                    CiAddress raddr;
                    if (right.isStackSlot()) {
                        raddr = frameMap.toStackAddress(((CiStackSlot) right));
                        masm.load( ARMV7.s30, raddr, CiKind.Double);
                    } else {
                        assert right.isConstant();
                        raddr = tasm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) right).asDouble()));
                        masm.setUpScratch(raddr);
                        masm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                        masm.vldr(ConditionFlag.Always, ARMV7.s30, ARMV7.r12, 0, CiKind.Double, CiKind.Int);
                    }
                    switch (code) {
                        case Add:
                            masm.vadd(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Double);
                            break;
                        case Sub:
                            masm.vsub(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Double);
                            break;
                        case Mul:
                            masm.vmul(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Double);
                            break;
                        case Div:
                            masm.vdiv(ConditionFlag.Always, lreg, lreg, ARMV7.s30, CiKind.Double);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    assert target.sizeInBytes(kind) == 8;
                    if (right.isStackSlot()) {
                        // register - stack
                        assert (right.kind == CiKind.Long);
                        CiAddress raddr = frameMap.toStackAddress(((CiStackSlot) right));
                        masm.saveInFP(9);
                        masm.load(ARMV7.r8, raddr, CiKind.Long);
                        switch (code) {
                            case Add:
                                masm.addLong(lreg, lreg, ARMV7.r8);
                                break;
                            case Sub:
                                masm.subLong(lreg, lreg, ARMV7.r8);
                                break;
                            default:
                                throw Util.shouldNotReachHere();
                        }
                        //masm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s30, null, CiKind.Int, CiKind.Float);
                        masm.restoreFromFP(9);
                    } else {
                        // register - constant
                        assert right.isConstant();
                        long c = ((CiConstant) right).asLong();
                        masm.saveInFP(9);
                        masm.movlong(ARMV7.r8, c, CiKind.Long);
                        switch (code) {
                            case Add:
                                masm.addLong(lreg, lreg, ARMV7.r8);
                                break;
                            case Sub:
                                masm.subLong(lreg, lreg, ARMV7.r8);
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
                masm.load(ARMV7.r8, laddr, CiKind.Int);
                switch (code) {
                    case Add:
                        masm.addRegisters(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, rreg, 0, 0);
                        break;
                    case Sub:
                        masm.sub(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, rreg, 0, 0);
                        break;
                    default:
                        throw Util.shouldNotReachHere();

                }
                masm.store(ARMV7.r8, laddr, CiKind.Int);
            } else {
                assert right.isConstant();
                int c = ((CiConstant) right).asInt();
                switch (code) {
                    case Add:
                        masm.incrementl(laddr, c);
                        break;
                    case Sub:
                        masm.decrementl(laddr, c);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
        }
        // Checkstyle: on
    }

    @Override
    protected void emitIntrinsicOp(LIROpcode code, CiValue value, CiValue unused, CiValue dest, LIROp2 op) {
        assert value.kind.isDouble();
        switch (code) {
            case Abs:
                if (dest.asRegister() != value.asRegister()) {
                    masm.vmov(ConditionFlag.Always, dest.asRegister(), value.asRegister(), null, CiKind.Double, CiKind.Double);
                }
                masm.vmov(ConditionFlag.Always, ARMV7.s30, ARMV7.r9, null, CiKind.Float, CiKind.Int);
                masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 0x7fffffff);
                masm.vmov(ConditionFlag.Always, ARMV7.r8, dest.asRegister(), null, CiKind.Long, CiKind.Double);
                masm.and(ConditionFlag.Always, false, ARMV7.r9, ARMV7.r9, ARMV7.r12, 0, 0);
                masm.vmov(ConditionFlag.Always, dest.asRegister(), ARMV7.r8, ARMV7.r9, CiKind.Double, CiKind.Long);
                masm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s30, null, CiKind.Int, CiKind.Float);
                break;
            case Sqrt:
                masm.vsqrt(ConditionFlag.Always, dest.asRegister(), value.asRegister(), dest.kind);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitLogicOp(LIROpcode code, CiValue left, CiValue right, CiValue dst) {
        assert left.isRegister();
        assert dst.isRegister();
        // Checkstyle: off
        if (left.kind.isInt()) {
            CiRegister reg = left.asRegister();
            if (right.isConstant()) {
                int val = ((CiConstant) right).asInt();
                masm.movImmediate(ConditionFlag.Always, ARMV7.r12, val);
                switch (code) {
                    case LogicAnd:
                        masm.iand(reg, reg, ARMV7.r12);
                        break;
                    case LogicOr:
                        masm.ior(reg, reg, ARMV7.r12);
                        break;
                    case LogicXor:
                        masm.ixor(reg, reg, ARMV7.r12);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isStackSlot()) {
                // added support for stack operands
                CiAddress raddr = frameMap.toStackAddress(((CiStackSlot) right));
                masm.setUpScratch(raddr);
                masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, ARMV7.r8, ARMV7.r12, 0);
                assert (reg != ARMV7.r12);
                switch (code) {
                    case LogicAnd:
                        masm.iand(reg, reg, ARMV7.r8);
                        break;
                    case LogicOr:
                        masm.ior(reg, reg, ARMV7.r8);
                        break;
                    case LogicXor:
                        masm.ixor(reg, reg, ARMV7.r8);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                CiRegister rright = right.asRegister();
                switch (code) {
                    case LogicAnd:
                        masm.iand(reg, reg, rright);
                        break;
                    case LogicOr:
                        masm.ior(reg, reg, rright);
                        break;
                    case LogicXor:
                        masm.ixor(reg, reg, rright);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister(), left.kind, dst.kind);
        } else {
            CiRegister lreg = left.asRegister();
            if (right.isConstant()) {
                CiConstant rightConstant = (CiConstant) right;
                masm.saveInFP(9);
                masm.movlong(ARMV7.r8, rightConstant.asLong(), CiKind.Long);
                switch (code) {
                    case LogicAnd:
                        masm.land(lreg, lreg, ARMV7.r8);
                        break;
                    case LogicOr:
                        masm.lor(lreg, lreg, ARMV7.r8);
                        break;
                    case LogicXor:
                        masm.lxor(lreg, lreg, ARMV7.r8);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
                masm.restoreFromFP(9);
            } else {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case LogicAnd:
                        masm.land(lreg, lreg, rreg);
                        break;
                    case LogicOr:
                        masm.lor(lreg, lreg, rreg);
                        break;
                    case LogicXor:
                        masm.lxor(lreg, lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            CiRegister dreg = dst.asRegister();
            moveRegs(lreg, dreg, left.kind, dst.kind);
        }
        // Checkstyle: on
    }

    void arithmeticIdiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() || right.isConstant() : "right must be register or constant";
        assert result.isRegister() : "result must be register";
        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();

        if (right.isConstant()) {
            Util.shouldNotReachHere("cwi: I assume this is dead code, notify me if I'm wrong...");
        } else {
            CiRegister rreg = right.asRegister();
            ARMV7Label continuation = new ARMV7Label();
            if (C1XOptions.GenSpecialDivChecks) {
                ARMV7Label normalCase = new ARMV7Label();
                masm.movImmediate(ConditionFlag.Always, ARMV7.r12, Integer.MIN_VALUE);
                masm.cmp(ConditionFlag.Always, lreg, ARMV7.r12, 0, 0);
                masm.jcc(ConditionFlag.NotEqual, normalCase);
                if (code == LIROpcode.Irem) {
                    masm.eor(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, ARMV7.r8, 0, 0);
                }
                masm.movImmediate(ConditionFlag.Always, ARMV7.r12, -1);
                masm.cmp(ConditionFlag.Always, rreg, ARMV7.r12, 0, 0);
                masm.jcc(ConditionFlag.Equal, continuation);
                masm.bind(normalCase);
            }
            masm.mov(ConditionFlag.Always, false, ARMV7.r8, lreg);
            masm.eor(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
            masm.cmp(ConditionFlag.Always, ARMV7.r12, rreg, 0, 0);
            masm.insertDivZeroCheck();
            int offset = masm.codeBuffer.position();
            masm.vldr(ConditionFlag.Equal, ARMV7.s30, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
            masm.sdiv(ConditionFlag.Always, dreg, lreg, rreg);
            masm.bind(continuation);
            tasm.recordImplicitException(offset, info);
            if (code == LIROpcode.Irem) {
                masm.mul(ConditionFlag.Always, false, lreg, dreg, rreg);
                masm.sub(ConditionFlag.Always, false, dreg, ARMV7.r8, lreg, 0, 0);
            } else {
                assert code == LIROpcode.Idiv;
            }
        }
    }

    void arithmeticLDivExceptionCheck(CiValue dividend, CiValue divisor, CiValue result, LIRDebugInfo info) {
        assert divisor.isRegister() : "the divisor needs to be a register LDIVE exception checks";
        CiRegister denominator = divisor.asRegister();
        masm.eor(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
        masm.cmp(ConditionFlag.Always, ARMV7.r12, denominator, 0, 0);
        masm.cmp(ConditionFlag.Equal, ARMV7.r12, ARMV7.cpuRegisters[denominator.getEncoding() + 1], 0, 0);
        masm.insertDivZeroCheck();
        int offset = masm.codeBuffer.position();
        masm.vldr(ConditionFlag.Equal, ARMV7.s30, ARMV7.r12, 0, CiKind.Float, CiKind.Int); // fault if EQUAL
        tasm.recordImplicitException(offset, info);
    }

    void arithmeticIudiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";
        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();
        CiRegister rreg = right.asRegister();

        masm.mov(ConditionFlag.Always, false, ARMV7.r8, lreg);
        masm.eor(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
        masm.cmp(ConditionFlag.Always, ARMV7.r12, rreg, 0, 0);
        masm.insertDivZeroCheck();
        int offset = masm.codeBuffer.position();
        masm.vldr(ConditionFlag.Equal, ARMV7.s30, ARMV7.r12, 0, CiKind.Float, CiKind.Int); // fault if EQUAL
        masm.udiv(ConditionFlag.Always, dreg, lreg, rreg);
        tasm.recordImplicitException(offset, info);

        if (code == LIROpcode.Iurem) {
            masm.mul(ConditionFlag.Always, false, lreg, dreg, rreg);
            masm.sub(ConditionFlag.Always, false, dreg, ARMV7.r8, lreg, 0, 0);
        } else {
            assert code == LIROpcode.Iudiv;
        }
    }

    void arithmeticLdiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";
        assert result.kind.isLong();
        CiRegister rreg = right.asRegister();
        ARMV7Label continuation = new ARMV7Label();
        if (C1XOptions.GenSpecialDivChecks) {
            // check for special case of Long.MIN_VALUE / -1
            ARMV7Label normalCase = new ARMV7Label();
            masm.movlong(rreg, java.lang.Long.MIN_VALUE, CiKind.Long);
            masm.jcc(ConditionFlag.NotEqual, normalCase);
            if (code == LIROpcode.Lrem) {
                masm.eor(ConditionFlag.Always, false, rreg, rreg, rreg, 0, 0);
                masm.eor(ConditionFlag.Always, false, ARMV7.cpuRegisters[rreg.getEncoding() + 1], ARMV7.cpuRegisters[rreg.getEncoding() + 1], ARMV7.cpuRegisters[rreg.getEncoding() + 1], 0, 0);
            }
            masm.cmpl(rreg, -1);
            masm.jcc(ConditionFlag.Equal, continuation);
            // handle normal case
            masm.bind(normalCase);
        }
        int offset = masm.codeBuffer.position();
        // normal and special case exit
        masm.bind(continuation);
        tasm.recordImplicitException(offset, info);
        assert code == LIROpcode.Ldiv || code == LIROpcode.Lrem;
        masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 4);
        masm.blx(ARMV7.r12);
    }

    void arithmeticLudiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";
        assert false : "ArithmeticLudiv not implemented";
        int offset = masm.codeBuffer.position();
        tasm.recordImplicitException(offset, info);
        assert code == LIROpcode.Ludiv || code == LIROpcode.Lurem;
        masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 12);
        masm.blx(ARMV7.r12);
    }

    private ConditionFlag convertCondition(Condition condition) {
        ConditionFlag acond = ConditionFlag.NeverUse;
        switch (condition) {
            case EQ:
                acond = ConditionFlag.Equal;
                break;
            case NE:
                acond = ConditionFlag.NotEqual;
                break;
            case LT:
                acond = ConditionFlag.SignedLesser;
                break;
            case LE:
                acond = ConditionFlag.SignedLowerOrEqual;
                break;
            case GE:
                acond = ConditionFlag.SignedGreaterOrEqual;
                break;
            case GT:
                acond = ConditionFlag.SignedGreater;
                break;
            case BE:
                acond = ConditionFlag.UnsignedLowerOrEqual;
                break;
            case AE:
                acond = ConditionFlag.CarrySetUnsignedHigherEqual;
                break;
            case BT:
                acond = ConditionFlag.CarryClearUnsignedLower;
                break;
            case AT:
                acond = ConditionFlag.UnsignedHigher;
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
                        masm.cmpl(reg1, opr2.asRegister());
                        break;
                    case Object:
                        masm.cmpq(reg1, opr2.asRegister());
                        break;
                    case Long:
                        assert (reg1 != ARMV7.r12);
                        masm.lcmpl(convertCondition(condition), reg1, opr2.asRegister());
                        break;
                    case Float:
                        masm.ucomisd(opr1.asRegister(), opr2.asRegister(), opr1.kind, opr2.kind);
                        break;
                    case Double:
                        masm.ucomisd(reg1, opr2.asRegister(), opr1.kind, opr2.kind);
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
                        masm.cmpl(reg1, frameMap.toStackAddress(opr2Slot));
                        break;
                    case Long:
                        masm.vmov(ConditionFlag.Always, ARMV7.s30, ARMV7.r9, null, CiKind.Float, CiKind.Int);
                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
                        masm.ldrd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
                        masm.lcmpl(convertCondition(condition), reg1, ARMV7.r8);
                        masm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s30, null, CiKind.Int, CiKind.Float);
                        break;
                    case Object:
                        masm.cmpptr(reg1, frameMap.toStackAddress(opr2Slot));
                        break;
                    case Float:
                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
                        masm.vldr(ConditionFlag.Always, ARMV7.s30, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
                        masm.ucomisd(reg1, ARMV7.s30, opr1.kind, CiKind.Float);
                        break;
                    case Double:
                        masm.setUpScratch(frameMap.toStackAddress(opr2Slot));
                        masm.vldr(ConditionFlag.Always, ARMV7.s16, ARMV7.r12, 0, CiKind.Double, CiKind.Int);
                        masm.ucomisd(reg1, ARMV7.s16, opr1.kind, CiKind.Double);
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
                        masm.cmpl(reg1, c.asInt());
                        break;
                    case Float:
                        masm.setUpScratch(tasm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) opr2).asFloat())));
                        masm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                        masm.vldr(ConditionFlag.Always, ARMV7.s30, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
                        masm.ucomisd(reg1, ARMV7.s30, opr1.kind, CiKind.Float);
                        break;
                    case Double:
                        masm.setUpScratch(tasm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) opr2).asDouble())));
                        masm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                        masm.vldr(ConditionFlag.Always, ARMV7.s16, ARMV7.r12, 0, CiKind.Double, CiKind.Int);
                        masm.ucomisd(reg1, ARMV7.s16, opr1.kind, CiKind.Double);
                        break;
                    case Long: {
                        masm.saveInFP(9);
                        if (c.asLong() == 0) {
                            masm.movlong(ARMV7.r8, 0, CiKind.Long);
                        } else {
                            masm.movlong(ARMV7.r8, c.asLong(), CiKind.Long);
                        }
                        masm.lcmpl(convertCondition(condition), reg1, ARMV7.r8);
                        masm.restoreFromFP(9);
                        break;
                    }
                    case Object: {
                        movoop(ARMV7.r8, c);
                        if (oldOpr1.isConstant()) {
                            CiValue newOpr1 = compilation.registerConfig.getScratchRegister().asValue(oldOpr1.kind);
                            const2reg(oldOpr1, newOpr1, null);
                            opr1 = newOpr1;
                            reg1 = opr1.asRegister();
                        }
                        masm.cmpq(reg1, ARMV7.r8);
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
        if (code == LIROpcode.Cmpfd2i || code == LIROpcode.Ucmpfd2i) {
            if (left.kind.isFloat()) {
                masm.cmpss2int(left.asRegister(), right.asRegister(), dst.asRegister(), code == LIROpcode.Ucmpfd2i, left.kind, right.kind);
            } else if (left.kind.isDouble()) {
                masm.cmpsd2int(left.asRegister(), right.asRegister(), dst.asRegister(), code == LIROpcode.Ucmpfd2i, left.kind, right.kind);
            } else {
                throw Util.unimplemented("no fpu stack");
            }
        } else {
            assert code == LIROpcode.Cmpl2i;
            CiRegister dest = dst.asRegister();
            ARMV7Label high = new ARMV7Label();
            ARMV7Label done = new ARMV7Label();
            ARMV7Label isEqual = new ARMV7Label();
            masm.lcmpl(ConditionFlag.Equal, left.asRegister(), right.asRegister());
            masm.jcc(ConditionFlag.Equal, isEqual);
            masm.lcmpl(ConditionFlag.SignedGreater, left.asRegister(), right.asRegister());
            masm.jcc(ConditionFlag.SignedGreater, high);
            masm.xorptr(dest, dest);
            masm.decrementl(dest, 1);
            masm.jmp(done);
            masm.bind(high);
            masm.xorptr(dest, dest);
            masm.incrementl(dest, 1);
            masm.jmp(done);
            masm.bind(isEqual);
            masm.xorptr(dest, dest);
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
        assert count.asRegister() != ARMV7.r12 : "count register must not be scratch";
        if (left.kind.isInt()) {
            CiRegister value = left.asRegister();
            assert value != SHIFTCount : "left cannot be r8";
            masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 0x1f);
            masm.and(ConditionFlag.Always, false, ARMV7.r12, count.asRegister(), ARMV7.r12, 0, 0);
            // Checkstyle: off
            switch (code) {
                case Shl:
                    masm.ishl(dest.asRegister(), value, ARMV7.r12);
                    break;
                case Shr:
                    masm.ishr(dest.asRegister(), value, ARMV7.r12);
                    break;
                case Ushr:
                    masm.iushr(dest.asRegister(), value, ARMV7.r12);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            CiRegister lreg = left.asRegister();
            assert lreg != SHIFTCount : "left cannot be r8";
            switch (code) {
                case Shl:
                    masm.lshl(dest.asRegister(), lreg, count.asRegister());
                    break;
                case Shr:
                    masm.lshr(dest.asRegister(), lreg, count.asRegister());
                    break;
                case Ushr:
                    masm.lushr(dest.asRegister(), lreg, count.asRegister());
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
            // Checkstyle: on
        }
    }

    @Override
    protected void emitShiftOp(LIROpcode code, CiValue left, int count, CiValue dest) {
        assert dest.isRegister();
        if (dest.kind.isInt()) {
            CiRegister value = dest.asRegister();
            count = count & 0x1F;
            masm.mov(ConditionFlag.Always, false, dest.asRegister(), left.asRegister());
            // Checkstyle: off
            masm.movImmediate(ConditionFlag.Always, ARMV7.r12, count);
            switch (code) {
                case Shl:
                    masm.ishl(dest.asRegister(), value, ARMV7.r12);
                    break;
                case Shr:
                    masm.ishr(dest.asRegister(), value, ARMV7.r12);
                    break;
                case Ushr:
                    masm.iushr(dest.asRegister(), value, ARMV7.r12);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            CiRegister value = dest.asRegister();
            count = count & 0x3F;
            moveRegs(left.asRegister(), value, left.kind, dest.kind);
            masm.movImmediate(ConditionFlag.Always, ARMV7.r12, count);
            switch (code) {
                case Shl:
                    masm.lshl(value, left.asRegister(), ARMV7.r12);
                    break;
                case Shr:
                    masm.lshr(value, left.asRegister(), ARMV7.r12);
                    break;
                case Ushr:
                    masm.lushr(value, left.asRegister(), ARMV7.r12);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
            // Checkstyle: on
        }
    }

    @Override
    protected void emitSignificantBitOp(boolean most, CiValue src, CiValue dst) {
        assert dst.isRegister();
        CiRegister result = dst.asRegister();
        assert src.asRegister().isCpu();
        if (src.isRegister()) {
            CiRegister value = src.asRegister();
            assert value != ARMV7.r8;
            CiRegister value1 = ARMV7.cpuRegisters[value.getEncoding() + 1];
            assert value != result;
            if (most) {
                if (src.kind.isLong()) {
                    assert !Platform.target().arch.is32bit();
                    ARMV7Label normal = new ARMV7Label();
                    ARMV7Label normal2 = new ARMV7Label();
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, value1, 0);
                    masm.jcc(ConditionFlag.NotEqual, normal);
                    masm.cmpImmediate(ConditionFlag.Equal, value, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.bind(normal);
                    masm.clz(ConditionFlag.Always, ARMV7.r12, value1);
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r12, 32);
                    masm.jcc(ConditionFlag.Equal, normal2);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, 63);
                    masm.sub(ConditionFlag.Always, false, result, ARMV7.r8, ARMV7.r12, 0, 0);
                    masm.jcc(ConditionFlag.Always, exit);
                    masm.bind(normal2);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, 31);
                    masm.sub(ConditionFlag.Always, false, result, ARMV7.r8, value1, 0, 0);
                    masm.bind(exit);
                } else {
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, value, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.clz(ConditionFlag.Always, ARMV7.r12, value);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, 31);
                    masm.sub(ConditionFlag.Always, false, result, ARMV7.r8, ARMV7.r12, 0, 0);
                    masm.bind(exit);
                }
            } else {
                if (src.kind.isLong()) {
                    assert !Platform.target().arch.is32bit();
                    ARMV7Label normal = new ARMV7Label();
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, value1, 0);
                    masm.jcc(ConditionFlag.NotEqual, normal);
                    masm.cmpImmediate(ConditionFlag.Always, value, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.bind(normal);
                    masm.rbit(ConditionFlag.Always, ARMV7.r12, value);
                    masm.rbit(ConditionFlag.Always, value, value1);
                    masm.mov(ConditionFlag.Always, false, value1, ARMV7.r12);
                    masm.clz(ConditionFlag.Always, ARMV7.r12, value1);
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r12, 32);
                    masm.mov(ConditionFlag.NotEqual, false, result, ARMV7.r12);
                    masm.jcc(ConditionFlag.NotEqual, exit);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 32);
                    masm.clz(ConditionFlag.Always, value1, value);
                    masm.addRegisters(ConditionFlag.Always, false, result, ARMV7.r12, value1, 0, 0);
                    masm.bind(exit);
                } else {
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, value, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.rbit(ConditionFlag.Always, ARMV7.r8, value);
                    masm.clz(ConditionFlag.Always, result, ARMV7.r8);
                    masm.bind(exit);
                }
            }
        } else {
            CiAddress laddr = asAddress(src);
            masm.setUpScratch(laddr);
            if (most) {
                if (src.kind.isLong()) {
                    assert !Platform.target().arch.is32bit();
                    masm.ldrd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
                    ARMV7Label normal = new ARMV7Label();
                    ARMV7Label normal2 = new ARMV7Label();
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r9, 0);
                    masm.jcc(ConditionFlag.NotEqual, normal);
                    masm.cmpImmediate(ConditionFlag.Equal, ARMV7.r9, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.bind(normal);
                    masm.clz(ConditionFlag.Always, ARMV7.r12, ARMV7.r9);
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r12, 32);
                    masm.jcc(ConditionFlag.Equal, normal2);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, 63);
                    masm.sub(ConditionFlag.Always, false, result, ARMV7.r8, ARMV7.r12, 0, 0);
                    masm.jcc(ConditionFlag.Always, exit);
                    masm.bind(normal2);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r8, 31);
                    masm.sub(ConditionFlag.Always, false, result, ARMV7.r8, ARMV7.r9, 0, 0);
                    masm.bind(exit);
                } else {
                    masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, ARMV7.r12, ARMV7.r12, 0);
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r12, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.clz(ConditionFlag.Always, ARMV7.r8, ARMV7.r12);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 31);
                    masm.sub(ConditionFlag.Always, false, result, ARMV7.r12, ARMV7.r8, 0, 0);
                    masm.bind(exit);
                }
            } else {
                if (src.kind.isLong()) {
                    assert !Platform.target().arch.is32bit();
                    masm.ldrd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
                    ARMV7Label normal = new ARMV7Label();
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r9, 0);
                    masm.jcc(ConditionFlag.NotEqual, normal);
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r8, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.bind(normal);
                    masm.rbit(ConditionFlag.Always, ARMV7.r12, ARMV7.r8);
                    masm.rbit(ConditionFlag.Always, ARMV7.r8, ARMV7.r9);
                    masm.mov(ConditionFlag.Always, false, ARMV7.r9, ARMV7.r12);
                    masm.clz(ConditionFlag.Always, ARMV7.r12, ARMV7.r9);
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r12, 32);
                    masm.mov(ConditionFlag.NotEqual, false, result, ARMV7.r12);
                    masm.jcc(ConditionFlag.NotEqual, exit);
                    masm.clz(ConditionFlag.Always, ARMV7.r9, ARMV7.r8);
                    masm.add12BitImmediate(ConditionFlag.Always, false, result, ARMV7.r9, 32);
                    masm.bind(exit);
                } else {
                    masm.ldrImmediate(ConditionFlag.Always, 1, 1, 0, ARMV7.r12, ARMV7.r12, 0);
                    ARMV7Label exit = new ARMV7Label();
                    masm.cmpImmediate(ConditionFlag.Always, ARMV7.r12, 0);
                    masm.movImmediate(ConditionFlag.Equal, result, -1);
                    masm.jcc(ConditionFlag.Equal, exit);
                    masm.rbit(ConditionFlag.Always, ARMV7.r12, ARMV7.r12);
                    masm.clz(ConditionFlag.Always, result, ARMV7.r12);
                    masm.bind(exit);
                }
            }
        }
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
            masm.ineg(dest.asRegister(), left.asRegister());
        } else if (dest.kind.isFloat()) {
            masm.vneg(ConditionFlag.Always, dest.asRegister(), left.asRegister(), CiKind.Float);
        } else if (dest.kind.isDouble()) {
            masm.vneg(ConditionFlag.Always, dest.asRegister(), left.asRegister(), CiKind.Double);
        } else {
            masm.lneg(dest.asRegister(), left.asRegister());
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
        assert false : "emitVolatileMove ARMV7IRAssembler";

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
        debugMethodWriter.append(inlinedMethodName + " " + Integer.toHexString(masm.codeBuffer.position()) + " " + masm.codeBuffer.position(), methodID);
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
                        arithmeticIdiv(LIROpcode.Idiv, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    } else {
                        emitArithOp(LIROpcode.Div, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    }
                    break;
                case Mul:
                    emitArithOp(LIROpcode.Mul, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
                    break;
                case Mod:
                    if (inst.kind == CiKind.Int) {
                        arithmeticIdiv(LIROpcode.Irem, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
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
                    masm.leaq(dst, CiAddress.Placeholder);
                    int afterLea = masm.codeBuffer.position();
                    masm.codeBuffer.setPosition(beforeLea);
                    masm.leaq(dst, new CiAddress(target.wordKind, ARMV7.rip.asValue(), beforeLea - afterLea));
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
                    assert false : "RepeatMoveBytes in ARMV7LIRAssembler unimplemented";
                    masm.crashme();
                    break;
                case RepeatMoveWords:
                    assert false : "RepeatMoveWords in ARMV7LIRAssembler unimplemented";
                    masm.crashme();
                    break;
                case PointerCAS: {
                    assert false : "PointerCAS in ARMV7LIRAssembler unimplemented";
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
                        ARMV7Label label = new ARMV7Label(labels[((XirLabel) inst.extra).index]);
                        masm.jmp(label);
                    } else {
                        directJmp(inst.extra);
                    }
                    break;
                }
                case DecAndJumpNotZero: {
                    assert false : "DecAndJumpNotZero in ARMV7LIRAssembler unimplemented";
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
                    emitXirCompare(inst, Condition.EQ, ConditionFlag.Equal, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.NE, ConditionFlag.NotEqual, operands, label);
                    break;
                }
                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GT, ConditionFlag.SignedGreater, operands, label);
                    break;
                }
                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GE, ConditionFlag.SignedGreaterOrEqual, operands, label);
                    break;
                }
                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.AE, ConditionFlag.CarrySetUnsignedHigherEqual, operands, label);
                    break;
                }
                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LT, ConditionFlag.SignedLesser, operands, label);
                    break;
                }
                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LE, ConditionFlag.SignedLowerOrEqual, operands, label);
                    break;
                }
                case Jbset: {
                    ARMV7Label label = new ARMV7Label(labels[((XirLabel) inst.extra).index]);
                    CiValue offset = operands[inst.y().index];
                    CiValue bit = operands[inst.z().index];
                    assert offset.isConstant() && bit.isConstant();
                    assert false;
                    masm.crashme();
                    masm.jcc(ConditionFlag.SignedGreaterOrEqual, label);
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
                        bangStackWithOffset(offset - frameSize);
                    }
                    break;
                }
                case PushFrame: {
                    int frameSize = initialFrameSizeInBytes();
                    if (CompilationBroker.singleton.simulateAdapter()) {
                        masm.nop(OPTIMIZED_ENTRY_POINT.offset() / 4);
                    }
                    masm.push(ConditionFlag.Always, 1 << 14);
                    masm.movImmediate(ConditionFlag.Always, ARMV7.r12, frameSize);
                    masm.sub(ConditionFlag.Always, false, ARMV7.r13, ARMV7.r13, ARMV7.r12, 0, 0);
                    if (C1XOptions.ZapStackOnMethodEntry) {
                        final int intSize = 4;
                        for (int i = 0; i < frameSize / intSize; ++i) {
                            masm.setUpScratch(new CiAddress(CiKind.Int, ARMV7.r13.asValue(), i * intSize));
                            masm.movImmediate(ConditionFlag.Always, ARMV7.r8, 0xC1C1C1C1);
                            masm.str(ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, ARMV7.r0, 0, 0);
                        }
                    }

                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        assert frameToCSA >= 0;
                        masm.save(csl, frameToCSA);
                    }

                    if (C1XOptions.DebugMethods) {
                        masm.movImmediate(ConditionFlag.Always, ARMV7.r12, methodID);
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
                    masm.incrementq(ARMV7.r13, frameSize);
                    break;
                }
                case Push: {
                    CiRegisterValue value = assureInRegister(operands[inst.x().index]);
                    if (value.asRegister().number < 16) {
                        masm.push(ConditionFlag.Always, 1 << value.asRegister().number, true);
                    } else {
                        masm.vpush(ConditionFlag.Always, value.asRegister(), value.asRegister(), value.kind, value.kind);
                    }
                    break;
                }
                case Pop: {
                    CiValue result = operands[inst.result.index];
                    if (result.isRegister()) {
                        if (result.asRegister().getEncoding() < 16) {
                            masm.pop(ConditionFlag.Always, 1 << result.asRegister().getEncoding(), true);
                        } else {
                            masm.vpop(ConditionFlag.Always, result.asRegister(), result.asRegister(), result.kind, result.kind);
                        }
                    } else {
                        masm.pop(ConditionFlag.Always, 1 << 12, true);
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
        masm.setUpScratch(new CiAddress(target.wordKind, ARMV7.RSP, -offset));
        masm.strImmediate(ConditionFlag.Always, 1, 1, 0, ARMV7.r0, ARMV7.r12, 0);
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
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];
        emitCompare(condition, x, y, null);
        masm.jcc(cflag, new ARMV7Label(label));
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
                masm.movptr(dst, 0);
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
                masm.movptr(dst, c.asInt());
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
                masm.movImmediate(ConditionFlag.Always, ARMV7.r12, 0xdeaddead); // patched?
                masm.mov(ConditionFlag.Always, false, dst, ARMV7.r12);
            } else {
                masm.setUpScratch(tasm.recordDataReferenceInCode(obj));
                masm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                masm.ldr(ConditionFlag.Always, dst, ARMV7.r12, 0);
            }
        }
    }

    public void movoop(CiAddress dst, CiConstant obj) {
        movoop(ARMV7.r8, obj);
        masm.store(ARMV7.r8, dst, CiKind.Int);
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
        int before = masm.codeBuffer.position();
        masm.jmp(0, true);
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
