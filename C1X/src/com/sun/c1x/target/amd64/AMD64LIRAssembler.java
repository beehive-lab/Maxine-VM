/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.target.amd64;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.ci.CiValue.*;
import static java.lang.Double.*;
import static java.lang.Float.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.FrameMap.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.amd64.AMD64Assembler.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.*;

/**
 * This class implements the x86-specific code generation for LIR.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class AMD64LIRAssembler extends LIRAssembler implements LocalStubVisitor {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final long NULLWORD = 0;
    private static final CiRegister SHIFTCount = AMD64.rcx;

    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    final CiTarget target;
    final AMD64MacroAssembler masm;
    final int wordSize;
    final CiRegister rscratch1;

    public AMD64LIRAssembler(C1XCompilation compilation) {
        super(compilation);

        masm = (AMD64MacroAssembler) compilation.masm();
        target = compilation.target;
        wordSize = target.wordSize;
        rscratch1 = target.scratchRegister;
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
        // Reset the stack pointer
        masm.incrementq(target.stackPointerRegister, initialFrameSizeInBytes());
        // TODO: Add Safepoint polling at return!
        masm.ret(0);
    }

    /**
     * Emits an instruction which assigns the address of the immediately succeeding instruction into {@code dst}.
     * This satisfies the requirements for correctly translating the {@link LoadPC} HIR instruction.
     */
    @Override
    protected void emitReadPC(CiValue dst) {
        masm.leaq(dst.asRegister(), CiAddress.Placeholder);
    }

    @Override
    protected void emitPause() {
        masm.pause();
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, CiValue dst) {
        masm.leaq(dst.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    @Override
    protected void emitSafepoint(CiValue tmp, LIRDebugInfo info) {
        masm.safepoint(info);
    }

    private void moveRegs(CiRegister fromReg, CiRegister toReg) {
        if (fromReg != toReg) {
            masm.mov(toReg, fromReg);
        }
    }

    private void swapReg(CiRegister a, CiRegister b) {
        masm.xchgptr(a, b);
    }

    private void const2reg(CiRegister dst, int constant) {
        // Do not optimize with an XOR as this instruction may be between
        // a CMP and a Jcc in which case the XOR will modify the condition
        // flags and interfere with the Jcc.
        masm.movl(dst, constant);
    }

    private void const2reg(CiRegister dst, long constant) {
        // Do not optimize with an XOR as this instruction may be between
        // a CMP and a Jcc in which case the XOR will modify the condition
        // flags and interfere with the Jcc.
        masm.mov64(dst, constant);
    }

    private void const2reg(CiRegister dst, Object constant) {
        // Do not optimize with an XOR as this instruction may be between
        // a CMP and a Jcc in which case the XOR will modify the condition
        // flags and interfere with the Jcc.
        masm.movq(dst, masm.recordDataReferenceInCode(CiConstant.forObject(constant)));
    }

    private void const2reg(CiRegister dst, float constant) {
        if (constant == 0.0f) {
            masm.xorps(dst, dst);
        } else {
            masm.movflt(dst, masm.recordDataReferenceInCode(CiConstant.forFloat(constant)));
        }
    }

    private void const2reg(CiRegister dst, double constant) {
        if (constant == 0.0f) {
            masm.xorpd(dst, dst);
        } else {
            masm.movdbl(dst, masm.recordDataReferenceInCode(CiConstant.forDouble(constant)));
        }
    }

    @Override
    protected void const2reg(CiValue src, CiValue dest, LIRDebugInfo info) {
        assert src.isConstant();
        assert dest.isRegister();
        CiConstant c = (CiConstant) src;

        switch (c.kind) {
            case Boolean :
            case Byte    :
            case Char    :
            case Short   :
            case Int     : const2reg(dest.asRegister(), c.asInt()); break;
            case Long    : const2reg(dest.asRegister(), c.asLong()); break;
            case Jsr     : const2reg(dest.asRegister(), c.asJsr()); break;
            case Word    : const2reg(dest.asRegister(), c.asLong()); break;
            case Object  : const2reg(dest.asRegister(), c.asObject()); break;
            case Float   : const2reg(asXmmFloatReg(dest), c.asFloat()); break;
            case Double  : const2reg(asXmmDoubleReg(dest), c.asDouble()); break;
            default      : throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2stack(CiValue src, CiValue dst) {
        assert src.isConstant();
        assert dst.isStackSlot();
        CiStackSlot slot = (CiStackSlot) dst;
        CiConstant c = (CiConstant) src;

        switch (c.kind) {
            case Boolean :
            case Byte    :
            case Char    :
            case Short   :
            case Int     : masm.movl(frameMap.toStackAddress(slot), c.asInt()); break;
            case Float   : masm.movl(frameMap.toStackAddress(slot), floatToRawIntBits(c.asFloat())); break;
            case Object  : masm.movoop(frameMap.toStackAddress(slot), CiConstant.forObject(c.asObject())); break;
            case Long    : masm.movptr(frameMap.toStackAddress(slot), c.asLong()); break;
            case Double  : masm.movptr(frameMap.toStackAddress(slot), doubleToRawLongBits(c.asDouble())); break;
            default      : throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2mem(CiValue src, CiValue dst, CiKind kind, LIRDebugInfo info) {
        assert src.isConstant();
        assert dst.isAddress();
        CiConstant constant = (CiConstant) src;
        CiAddress addr = asAddress(dst);

        int nullCheckHere = codePos();
        switch (kind) {
            case Boolean :
            case Byte    : masm.movb(addr, constant.asInt() & 0xFF); break;
            case Char    :
            case Short   : masm.movw(addr, constant.asInt() & 0xFFFF); break;
            case Int     : masm.movl(addr, constant.asInt()); break;
            case Float   : masm.movl(addr, floatToRawIntBits(constant.asFloat())); break;
            case Object  : masm.movoop(addr, CiConstant.forObject(constant.asObject())); break;
            case Long    : masm.mov64(rscratch1, constant.asLong());
                           nullCheckHere = codePos();
                           masm.movq(addr, rscratch1); break;
            case Double  : masm.mov64(rscratch1, doubleToRawLongBits(constant.asDouble()));
                           nullCheckHere = codePos();
                           masm.movq(addr, rscratch1); break;
            default      : throw Util.shouldNotReachHere();
        }

        if (info != null) {
            asm.recordImplicitException(nullCheckHere, info);
        }
    }

    @Override
    protected void reg2reg(CiValue src, CiValue dest) {
        assert src.isRegister();
        assert dest.isRegister();

        if (dest.kind.isFloat()) {
            masm.movflt(asXmmFloatReg(dest), asXmmFloatReg(src));
        } else if (dest.kind.isDouble()) {
            masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(src));
        } else {
            if (src.kind == CiKind.Object) {
                masm.verifyOop(src.asRegister());
            }
            moveRegs(src.asRegister(), dest.asRegister());
        }
    }

    @Override
    protected void reg2stack(CiValue src, CiValue dst, CiKind kind) {
        assert src.isRegister();
        assert dst.isStackSlot();
        CiAddress addr = frameMap.toStackAddress(((CiStackSlot) dst));

        if (src.kind.isObject()) {
            masm.verifyOop(src.asRegister());
        }

        switch (src.kind) {
            case Boolean :
            case Byte    :
            case Char    :
            case Short   :
            case Jsr     :
            case Int     : masm.movl(addr, src.asRegister()); break;
            case Object  :
            case Long    :
            case Word    : masm.movq(addr, src.asRegister()); break;
            case Float   : masm.movflt(addr, asXmmFloatReg(src)); break;
            case Double  : masm.movsd(addr, asXmmDoubleReg(src)); break;
            default      : throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2mem(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        CiAddress toAddr = (CiAddress) dest;

        if (kind == CiKind.Object) {
            masm.verifyOop(src.asRegister());
        }
        if (info != null) {
            asm.recordImplicitException(codePos(), info);
        }

        switch (kind) {
            case Float   : masm.movflt(toAddr, asXmmFloatReg(src)); break;
            case Double  : masm.movsd(toAddr, asXmmDoubleReg(src)); break;
            case Int     : masm.movl(toAddr, src.asRegister()); break;
            case Long    :
            case Word    :
            case Object  : masm.movq(toAddr, src.asRegister()); break;
            case Char    :
            case Short   : masm.movw(toAddr, src.asRegister()); break;
            case Byte    :
            case Boolean : masm.movb(toAddr, src.asRegister()); break;
            default      : throw Util.shouldNotReachHere();
        }
    }

    private static CiRegister asXmmFloatReg(CiValue src) {
        assert src.kind.isFloat() : "must be float";
        CiRegister result = src.asRegister();
        assert result.isFpu() : "must be xmm";
        return result;
    }

    @Override
    protected void stack2reg(CiValue src, CiValue dest, CiKind kind) {
        assert src.isStackSlot();
        assert dest.isRegister();

        if (kind == CiKind.Object) {
            masm.verifyOop(dest.asRegister());
        }

        CiAddress addr = frameMap.toStackAddress(((CiStackSlot) src));

        switch (dest.kind) {
            case Boolean :
            case Byte    :
            case Char    :
            case Short   :
            case Jsr     :
            case Int     : masm.movl(dest.asRegister(), addr); break;
            case Object  :
            case Long    :
            case Word    : masm.movq(dest.asRegister(), addr); break;
            case Float   : masm.movflt(asXmmFloatReg(dest), addr); break;
            case Double  : masm.movdbl(asXmmDoubleReg(dest), addr); break;
            default      : throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void mem2mem(CiValue src, CiValue dest, CiKind kind) {
        if (dest.kind.isInt()) {
            masm.pushl(((CiAddress) src));
            masm.popl(((CiAddress) dest));
        } else {
            masm.pushptr(((CiAddress) src));
            masm.popptr(((CiAddress) dest));
        }
    }

    @Override
    protected void mem2stack(CiValue src, CiValue dest, CiKind kind) {
        if (dest.kind.isInt()) {
            masm.pushl(((CiAddress) src));
            masm.popl(frameMap.toStackAddress(((CiStackSlot) dest)));
        } else {
            masm.pushptr(((CiAddress) src));
            masm.popptr(frameMap.toStackAddress(((CiStackSlot) dest)));
        }
    }

    @Override
    protected void stack2stack(CiValue src, CiValue dest, CiKind kind) {
        if (src.kind.isInt()) {
            masm.pushl(frameMap.toStackAddress(((CiStackSlot) src)));
            masm.popl(frameMap.toStackAddress(((CiStackSlot) dest)));
        } else {
            masm.pushptr(frameMap.toStackAddress(((CiStackSlot) src)));
            masm.popptr(frameMap.toStackAddress(((CiStackSlot) dest)));
        }
    }

    @Override
    protected void mem2reg(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        assert src.isAddress();
        assert dest.isRegister();

        CiAddress addr = (CiAddress) src;
        CiAddress fromAddr = addr;

        if (info != null) {
            asm.recordImplicitException(codePos(), info);
        }

        switch (kind) {
            case Float   : masm.movflt(asXmmFloatReg(dest), fromAddr); break;
            case Double  : masm.movdbl(asXmmDoubleReg(dest), fromAddr); break;
            case Word    :
            case Object  : masm.movq(dest.asRegister(), fromAddr); break;
            case Int     : masm.movslq(dest.asRegister(), fromAddr); break;
            case Long    : masm.movq(dest.asRegister(), addr); break;
            case Boolean :
            case Byte    : masm.movsxb(dest.asRegister(), fromAddr); break;
            case Char    : masm.movzxl(dest.asRegister(), fromAddr); break;
            case Short   : masm.movswl(dest.asRegister(), fromAddr); break;
            default      : throw Util.shouldNotReachHere();
        }

        if (kind == CiKind.Object) {
            masm.verifyOop(dest.asRegister());
        }
    }

    @Override
    protected void emitReadPrefetch(CiValue src) {
        CiAddress addr = (CiAddress) src;
        CiAddress fromAddr = addr;

        switch (C1XOptions.ReadPrefetchInstr) {
            case 0  : masm.prefetchnta(fromAddr); break;
            case 1  : masm.prefetcht0(fromAddr); break;
            case 2  : masm.prefetcht2(fromAddr); break;
            default : throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitOp3(LIROp3 op) {
        switch (op.code) {
            case Idiv  :
            case Irem  : arithmeticIdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info); break;
            case Ldiv  :
            case Lrem  : arithmeticLdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info); break;
            case Wdiv  :
            case Wdivi :
            case Wrem  :
            case Wremi : arithmeticWdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info); break;
            default    : throw Util.shouldNotReachHere();
        }
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

    @Override
    protected void emitBranch(LIRBranch op) {

        assert assertEmitBranch(op);

        if (op.cond() == Condition.TRUE) {
            if (op.info != null) {
                asm.recordImplicitException(codePos(), op.info);
            }
            masm.jmp(op.label());
        } else {
            ConditionFlag acond = ConditionFlag.zero;
            if (op.code == LIROpcode.CondFloatBranch) {
                assert op.unorderedBlock() != null : "must have unordered successor";
                masm.jcc(ConditionFlag.parity, op.unorderedBlock().label());
                switch (op.cond()) {
                    case EQ : acond = ConditionFlag.equal; break;
                    case NE : acond = ConditionFlag.notEqual; break;
                    case LT : acond = ConditionFlag.below; break;
                    case LE : acond = ConditionFlag.belowEqual; break;
                    case GE : acond = ConditionFlag.aboveEqual; break;
                    case GT : acond = ConditionFlag.above; break;
                    default : throw Util.shouldNotReachHere();
                }
            } else {
                switch (op.cond()) {
                    case EQ : acond = ConditionFlag.equal; break;
                    case NE : acond = ConditionFlag.notEqual; break;
                    case LT : acond = ConditionFlag.less; break;
                    case LE : acond = ConditionFlag.lessEqual; break;
                    case GE : acond = ConditionFlag.greaterEqual; break;
                    case GT : acond = ConditionFlag.greater; break;
                    case BE : acond = ConditionFlag.belowEqual; break;
                    case AE : acond = ConditionFlag.aboveEqual; break;
                    default : throw Util.shouldNotReachHere();
                }
            }
            masm.jcc(acond, (op.label()));
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        CiValue src = op.operand();
        CiValue dest = op.result();
        Label endLabel = new Label();
        CiRegister srcRegister = src.asRegister();
        CiRegister rscratch1 = compilation.target.scratchRegister;
        switch (op.bytecode) {
            case I2L:
                masm.movslq(dest.asRegister(), srcRegister);
                break;

            case L2I:
                moveRegs(srcRegister, dest.asRegister());
                break;

            case I2B:
                moveRegs(srcRegister, dest.asRegister());
                masm.signExtendByte(dest.asRegister());
                break;

            case I2C:
                moveRegs(srcRegister, dest.asRegister());
                masm.andl(dest.asRegister(), 0xFFFF);
                break;

            case I2S:
                moveRegs(srcRegister, dest.asRegister());
                masm.signExtendShort(dest.asRegister());
                break;

            case F2D:
                masm.cvtss2sd(asXmmDoubleReg(dest), asXmmFloatReg(src));
                break;

            case D2F:
                masm.cvtsd2ss(asXmmFloatReg(dest), asXmmDoubleReg(src));
                break;

            case I2F:
                masm.cvtsi2ssl(asXmmFloatReg(dest), srcRegister);
                break;
            case I2D:
                masm.cvtsi2sdl(asXmmDoubleReg(dest), srcRegister);
                break;

            case F2I: {
                assert srcRegister.isFpu() && dest.isRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttss2sil(dest.asRegister(), srcRegister);
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case D2I: {
                assert srcRegister.isFpu() && dest.isRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2sil(dest.asRegister(), asXmmDoubleReg(src));
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case L2F:
                masm.cvtsi2ssq(asXmmFloatReg(dest), srcRegister);
                break;

            case L2D:
                masm.cvtsi2sdq(asXmmDoubleReg(dest), srcRegister);
                break;

            case F2L: {
                assert srcRegister.isFpu() && dest.kind.isLong() : "must both be XMM register (no fpu stack)";
                masm.cvttss2siq(dest.asRegister(), asXmmFloatReg(src));
                masm.mov64(rscratch1, java.lang.Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            case D2L: {
                assert srcRegister.isFpu() && dest.kind.isLong() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2siq(dest.asRegister(), asXmmDoubleReg(src));
                masm.mov64(rscratch1, java.lang.Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            case MOV_I2F:
                masm.movdl(asXmmFloatReg(dest), srcRegister);
                break;

            case MOV_L2D:
                masm.movdq(asXmmDoubleReg(dest), srcRegister);
                break;

            case MOV_F2I:
                masm.movdl(dest.asRegister(), asXmmFloatReg(src));
                break;

            case MOV_D2L:
                masm.movdq(dest.asRegister(), asXmmDoubleReg(src));
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        CiAddress address = op.address();
        CiRegister newval = op.newValue().asRegister();
        CiRegister cmpval = op.exepectedValue().asRegister();
        assert cmpval == AMD64.rax : "wrong register";
        assert newval != null : "new val must be register";
        assert cmpval != newval : "cmp and new values must be in different registers";
        assert cmpval != address.base() : "cmp and addr must be in different registers";
        assert newval != address.base() : "new value and addr must be in different registers";
        assert cmpval != address.index() : "cmp and addr must be in different registers";
        assert newval != address.index() : "new value and addr must be in different registers";
        if (compilation.target.isMP) {
            masm.lock();
        }
        if (op.code == LIROpcode.CasInt) {
            masm.cmpxchgl(newval, address);
        } else {
            assert op.code == LIROpcode.CasObj || op.code == LIROpcode.CasLong || op.code == LIROpcode.CasWord;
            masm.cmpxchgptr(newval, address);
        }
    }

    @Override
    protected void emitConditionalMove(Condition condition, CiValue opr1, CiValue opr2, CiValue result) {
        ConditionFlag acond;
        ConditionFlag ncond;
        switch (condition) {
            case EQ:
                acond = ConditionFlag.equal;
                ncond = ConditionFlag.notEqual;
                break;
            case NE:
                acond = ConditionFlag.notEqual;
                ncond = ConditionFlag.equal;
                break;
            case LT:
                acond = ConditionFlag.less;
                ncond = ConditionFlag.greaterEqual;
                break;
            case LE:
                acond = ConditionFlag.lessEqual;
                ncond = ConditionFlag.greater;
                break;
            case GE:
                acond = ConditionFlag.greaterEqual;
                ncond = ConditionFlag.less;
                break;
            case GT:
                acond = ConditionFlag.greater;
                ncond = ConditionFlag.lessEqual;
                break;
            case BE:
                acond = ConditionFlag.belowEqual;
                ncond = ConditionFlag.above;
                break;
            case BT:
                acond = ConditionFlag.below;
                ncond = ConditionFlag.aboveEqual;
                break;
            case AE:
                acond = ConditionFlag.aboveEqual;
                ncond = ConditionFlag.below;
                break;
            case AT:
                acond = ConditionFlag.above;
                ncond = ConditionFlag.belowEqual;
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
                    masm.cmovq(ncond, result.asRegister(), other.asRegister());
                } else {
                    masm.cmovq(ncond, result.asRegister(), other.asRegister());
                }
            } else {
                assert other.isStackSlot();
                CiStackSlot otherSlot = (CiStackSlot) other;
                if (other.kind.isInt()) {
                    masm.cmovl(ncond, result.asRegister(), frameMap.toStackAddress(otherSlot));
                } else {
                    masm.cmovq(ncond, result.asRegister(), frameMap.toStackAddress(otherSlot));
                }
            }

        } else {
            // conditional move not available, use emit a branch and move
            Label skip = new Label();
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
        assert left.kind == right.kind;
        assert left.equals(dest) : "left and dest must be equal";
        CiKind kind = left.kind;

        if (left.isRegister()) {
            CiRegister lreg = left.asRegister();

            if (right.isRegister()) {
                // register - register
                CiRegister rreg = right.asRegister();
                if (kind.isInt()) {
                    switch (code) {
                        case Add : masm.addl(lreg, rreg); break;
                        case Sub : masm.subl(lreg, rreg); break;
                        case Mul : masm.imull(lreg, rreg); break;
                        default  : throw Util.shouldNotReachHere();
                    }
                } else if (kind.isFloat()) {
                    assert rreg.isFpu() : "must be xmm";
                    switch (code) {
                        case Add : masm.addss(lreg, rreg); break;
                        case Sub : masm.subss(lreg, rreg); break;
                        case Mul : masm.mulss(lreg, rreg); break;
                        case Div : masm.divss(lreg, rreg); break;
                        default  : throw Util.shouldNotReachHere();
                    }
                } else if (kind.isDouble()) {
                    assert rreg.isFpu();
                    switch (code) {
                        case Add : masm.addsd(lreg, rreg); break;
                        case Sub : masm.subsd(lreg, rreg); break;
                        case Mul : masm.mulsd(lreg, rreg); break;
                        case Div : masm.divsd(lreg, rreg); break;
                        default  : throw Util.shouldNotReachHere();
                    }
                } else {
                    assert target.sizeInBytes(kind) == 8;
                    switch (code) {
                        case Add : masm.addq(lreg, rreg); break;
                        case Sub : masm.subq(lreg, rreg); break;
                        case Mul : masm.imulq(lreg, rreg);  break;
                        default  : throw Util.shouldNotReachHere();
                    }
                }
            } else {
                if (kind.isInt()) {
                    if (right.isStackSlot()) {
                        // register - stack
                        CiAddress raddr = frameMap.toStackAddress(((CiStackSlot) right));
                        switch (code) {
                            case Add : masm.addl(lreg, raddr); break;
                            case Sub : masm.subl(lreg, raddr); break;
                            default  : throw Util.shouldNotReachHere();
                        }
                    } else if (right.isConstant()) {
                        // register - constant
                        if (kind.isInt()) {
                            int delta = ((CiConstant) right).asInt();
                            switch (code) {
                                case Add : masm.incrementl(lreg, delta); break;
                                case Sub : masm.decrementl(lreg, delta); break;
                                default  : throw Util.shouldNotReachHere();
                            }
                        }
                    }
                } else if (kind.isFloat()) {
                    // register - stack/constant
                    CiAddress raddr;
                    if (right.isStackSlot()) {
                        raddr = frameMap.toStackAddress(((CiStackSlot) right));
                    } else {
                        assert right.isConstant();
                        raddr = masm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) right).asFloat()));
                    }
                    switch (code) {
                        case Add : masm.addss(lreg, raddr); break;
                        case Sub : masm.subss(lreg, raddr); break;
                        case Mul : masm.mulss(lreg, raddr); break;
                        case Div : masm.divss(lreg, raddr); break;
                        default  : throw Util.shouldNotReachHere();
                    }
                } else if (kind.isDouble()) {
                    // register - stack/constant
                    CiAddress raddr;
                    if (right.isStackSlot()) {
                        raddr = frameMap.toStackAddress(((CiStackSlot) right));
                    } else {
                        assert right.isConstant();
                        raddr = masm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) right).asDouble()));
                    }
                    switch (code) {
                        case Add : masm.addsd(lreg, raddr); break;
                        case Sub : masm.subsd(lreg, raddr); break;
                        case Mul : masm.mulsd(lreg, raddr); break;
                        case Div : masm.divsd(lreg, raddr); break;
                        default  : throw Util.shouldNotReachHere();
                    }
                } else {
                    // register - constant
                    assert target.sizeInBytes(kind) == 8;
                    assert right.isConstant();
                    long c = ((CiConstant) right).asLong();
                    masm.mov64(rscratch1, c);
                    switch (code) {
                        case Add : masm.addq(lreg, rscratch1); break;
                        case Sub : masm.subq(lreg, rscratch1); break;
                        default  : throw Util.shouldNotReachHere();
                    }
                }
            }

        } else {
            assert kind.isInt();
            CiAddress laddr = asAddress(left);

            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add : masm.addl(laddr, rreg); break;
                    case Sub : masm.subl(laddr, rreg); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else {
                assert right.isConstant();
                int c = ((CiConstant) right).asInt();
                switch (code) {
                    case Add : masm.incrementl(laddr, c); break;
                    case Sub : masm.decrementl(laddr, c); break;
                    default  : throw Util.shouldNotReachHere();
                }
            }
        }
    }

    @Override
    protected void emitIntrinsicOp(LIROpcode code, CiValue value, CiValue unused, CiValue dest, LIROp2 op) {
        assert value.kind.isDouble();
        switch (code) {
            case Abs:
                if (asXmmDoubleReg(dest) != asXmmDoubleReg(value)) {
                    masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(value));
                }
                masm.andpd(asXmmDoubleReg(dest), masm.recordDataReferenceInCode(CiConstant.forLong(DoubleSignMask)));
                break;

            case Sqrt:
                masm.sqrtsd(asXmmDoubleReg(dest), asXmmDoubleReg(value));
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitLogicOp(LIROpcode code, CiValue left, CiValue right, CiValue dst) {
        assert left.isRegister();
        if (left.kind.isInt()) {
            CiRegister reg = left.asRegister();
            if (right.isConstant()) {
                int val = ((CiConstant) right).asInt();
                switch (code) {
                    case LogicAnd : masm.andl(reg, val); break;
                    case LogicOr  : masm.orl(reg, val); break;
                    case LogicXor : masm.xorl(reg, val); break;
                    default       : throw Util.shouldNotReachHere();
                }
            } else if (right.isStackSlot()) {
                // added support for stack operands
                CiAddress raddr = frameMap.toStackAddress(((CiStackSlot) right));
                switch (code) {
                    case LogicAnd : masm.andl(reg, raddr); break;
                    case LogicOr  : masm.orl(reg, raddr); break;
                    case LogicXor : masm.xorl(reg, raddr); break;
                    default       : throw Util.shouldNotReachHere();
                }
            } else {
                CiRegister rright = right.asRegister();
                switch (code) {
                    case LogicAnd : masm.andq(reg, rright); break;
                    case LogicOr  : masm.orq(reg, rright); break;
                    case LogicXor : masm.xorptr(reg, rright); break;
                    default       : throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister());
        } else {
            assert target.sizeInBytes(left.kind) == 8;
            CiRegister lreg = left.asRegister();
            if (right.isConstant()) {
                CiConstant rightConstant = (CiConstant) right;
                masm.mov64(rscratch1, rightConstant.asLong());
                switch (code) {
                    case LogicAnd : masm.andq(lreg, rscratch1); break;
                    case LogicOr  : masm.orq(lreg, rscratch1); break;
                    case LogicXor : masm.xorq(lreg, rscratch1); break;
                    default       : throw Util.shouldNotReachHere();
                }
            } else {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case LogicAnd : masm.andq(lreg, rreg); break;
                    case LogicOr  : masm.orq(lreg, rreg); break;
                    case LogicXor : masm.xorptr(lreg, rreg); break;
                    default       : throw Util.shouldNotReachHere();
                }
            }

            CiRegister dreg = dst.asRegister();
            moveRegs(lreg, dreg);
        }
    }

    void arithmeticIdiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() || right.isConstant() : "right must be register or constant";
        assert result.isRegister() : "result must be register";

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();

        if (right.isConstant()) {
            int divisor = ((CiConstant) right).asInt();
            assert divisor > 0 && CiUtil.isPowerOf2(divisor) : "divisor must be power of two";
            if (code == LIROpcode.Idiv) {
                assert lreg == AMD64.rax : "dividend must be rax";
                masm.cdql(); // sign extend into rdx:rax
                if (divisor == 2) {
                    masm.subl(lreg, AMD64.rdx);
                } else {
                    masm.andl(AMD64.rdx, divisor - 1);
                    masm.addl(lreg, AMD64.rdx);
                }
                masm.sarl(lreg, CiUtil.log2(divisor));
                moveRegs(lreg, dreg);
            } else {
                assert code == LIROpcode.Irem;
                Label done = new Label();
                masm.mov(dreg, lreg);
                masm.andl(dreg, 0x80000000 | (divisor - 1));
                masm.jcc(ConditionFlag.positive, done);
                masm.decrementl(dreg, 1);
                masm.orl(dreg, ~(divisor - 1));
                masm.incrementl(dreg, 1);
                masm.bind(done);
            }
        } else {
            CiRegister rreg = right.asRegister();
            assert lreg == AMD64.rax : "left register must be rax";
            assert rreg != AMD64.rdx : "right register must not be rdx";

            moveRegs(lreg, AMD64.rax);

            Label continuation = new Label();

            if (C1XOptions.GenSpecialDivChecks) {
                // check for special case of Integer.MIN_VALUE / -1
                Label normalCase = new Label();
                masm.cmpl(AMD64.rax, Integer.MIN_VALUE);
                masm.jcc(ConditionFlag.notEqual, normalCase);
                if (code == LIROpcode.Irem) {
                    // prepare X86Register.rdx for possible special case where remainder = 0
                    masm.xorl(AMD64.rdx, AMD64.rdx);
                }
                masm.cmpl(rreg, -1);
                masm.jcc(ConditionFlag.equal, continuation);

                // handle normal case
                masm.bind(normalCase);
            }
            masm.cdql();
            int offset = masm.codeBuffer.position();
            masm.idivl(rreg);

            // normal and special case exit
            masm.bind(continuation);

            asm.recordImplicitException(offset, info);
            if (code == LIROpcode.Irem) {
                moveRegs(AMD64.rdx, dreg); // result is in rdx
            } else {
                assert code == LIROpcode.Idiv;
                moveRegs(AMD64.rax, dreg);
            }
        }
    }

    void arithmeticLdiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";
        assert result.kind.isLong();

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();
        CiRegister rreg = right.asRegister();
        assert lreg == AMD64.rax : "left register must be rax";
        assert rreg != AMD64.rdx : "right register must not be rdx";

        moveRegs(lreg, AMD64.rax);

        Label continuation = new Label();

        if (C1XOptions.GenSpecialDivChecks) {
            // check for special case of Long.MIN_VALUE / -1
            Label normalCase = new Label();
            masm.mov64(AMD64.rdx, java.lang.Long.MIN_VALUE);
            masm.cmpq(AMD64.rax, AMD64.rdx);
            masm.jcc(ConditionFlag.notEqual, normalCase);
            if (code == LIROpcode.Lrem) {
                // prepare X86Register.rdx for possible special case (where remainder = 0)
                masm.xorq(AMD64.rdx, AMD64.rdx);
            }
            masm.cmpl(rreg, -1);
            masm.jcc(ConditionFlag.equal, continuation);

            // handle normal case
            masm.bind(normalCase);
        }
        masm.cdqq();
        int offset = masm.codeBuffer.position();
        masm.idivq(rreg);

        // normal and special case exit
        masm.bind(continuation);

        asm.recordImplicitException(offset, info);
        if (code == LIROpcode.Lrem) {
            moveRegs(AMD64.rdx, dreg);
        } else {
            assert code == LIROpcode.Ldiv;
            moveRegs(AMD64.rax, dreg);
        }
    }

    void arithmeticWdiv(LIROpcode code, CiValue left, CiValue right, CiValue result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();
        CiRegister rreg = right.asRegister();
        assert lreg == AMD64.rax : "left register must be rax";
        assert rreg != AMD64.rdx : "right register must not be rdx";

        // Must zero the high 64-bit word (in RDX) of the dividend
        masm.xorq(AMD64.rdx, AMD64.rdx);

        if (code == LIROpcode.Wdivi || code == LIROpcode.Wremi) {
            // Zero the high 32 bits of the divisor
            masm.movzxd(rreg, rreg);
        }

        moveRegs(lreg, AMD64.rax);

        int offset = masm.codeBuffer.position();
        masm.divq(rreg);

        asm.recordImplicitException(offset, info);
        if (code == LIROpcode.Wrem || code == LIROpcode.Wremi) {
            moveRegs(AMD64.rdx, dreg);
        } else {
            assert code == LIROpcode.Wdiv || code == LIROpcode.Wdivi;
            moveRegs(AMD64.rax, dreg);
        }
    }

    @Override
    protected void emitCompare(Condition condition, CiValue opr1, CiValue opr2, LIROp2 op) {
        assert opr1.kind.stackKind() == opr2.kind.stackKind();
        if (opr1.isRegister()) {
            CiRegister reg1 = opr1.asRegister();
            if (opr2.isRegister()) {
                // register - register
                switch (opr1.kind) {
                    case Boolean :
                    case Byte    :
                    case Char    :
                    case Short   :
                    case Int     : masm.cmpl(reg1, opr2.asRegister()); break;
                    case Long    :
                    case Word    :
                    case Object  : masm.cmpq(reg1, opr2.asRegister()); break;
                    case Float   : masm.ucomiss(reg1, asXmmFloatReg(opr2)); break;
                    case Double  : masm.ucomisd(reg1, asXmmDoubleReg(opr2)); break;
                    default      : throw Util.shouldNotReachHere();
                }
            } else if (opr2.isStackSlot()) {
                // register - stack
                CiStackSlot opr2Slot = (CiStackSlot) opr2;
                switch (opr1.kind) {
                    case Boolean :
                    case Byte    :
                    case Char    :
                    case Short   :
                    case Int     : masm.cmpl(reg1, frameMap.toStackAddress(opr2Slot)); break;
                    case Long    :
                    case Word    :
                    case Object  : masm.cmpptr(reg1, frameMap.toStackAddress(opr2Slot)); break;
                    case Float   : masm.ucomiss(reg1, frameMap.toStackAddress(opr2Slot)); break;
                    case Double  : masm.ucomisd(reg1, frameMap.toStackAddress(opr2Slot)); break;
                    default      : throw Util.shouldNotReachHere();
                }
            } else if (opr2.isConstant()) {
                // register - constant
                CiConstant c = (CiConstant) opr2;
                switch (opr1.kind) {
                    case Boolean :
                    case Byte    :
                    case Char    :
                    case Short   :
                    case Int     : masm.cmpl(reg1, c.asInt()); break;
                    case Float   : masm.ucomiss(reg1, masm.recordDataReferenceInCode(CiConstant.forFloat(((CiConstant) opr2).asFloat()))); break;
                    case Double  : masm.ucomisd(reg1, masm.recordDataReferenceInCode(CiConstant.forDouble(((CiConstant) opr2).asDouble()))); break;
                    case Long    :
                    case Word    : {
                        if (c.asLong() == 0) {
                            masm.cmpq(reg1, 0);
                        } else {
                            masm.mov64(rscratch1, c.asLong());
                            masm.cmpq(reg1, rscratch1);

                        }
                        break;
                    }
                    case Object  :  {
                        masm.movoop(rscratch1, CiConstant.forObject(c.asObject()));
                        masm.cmpq(reg1, rscratch1);
                        break;
                    }
                    default      : throw Util.shouldNotReachHere();
                }
            } else if (opr2.isAddress()) {
                // register - address
                if (op != null && op.info != null) {
                    asm.recordImplicitException(codePos(), op.info);
                }
                switch (opr1.kind) {
                    case Boolean :
                    case Byte    :
                    case Char    :
                    case Short   :
                    case Int     : masm.cmpl(reg1, asAddress(opr2)); break;
                    default      : throw Util.shouldNotReachHere();
                }
            }
        } else {
            assert opr1.isAddress() && opr2.isConstant();
            CiConstant c = ((CiConstant) opr2);

            if (c.kind == CiKind.Object) {
                assert condition == Condition.EQ || condition == Condition.NE : "need to reverse";
                masm.movoop(rscratch1, CiConstant.forObject(c.asObject()));
            }
            if (op != null && op.info != null) {
                asm.recordImplicitException(codePos(), op.info);
            }
            // special case: address - constant
            CiAddress addr = (CiAddress) opr1;
            if (c.kind == CiKind.Int) {
                masm.cmpl(addr, c.asInt());
            } else {
                assert c.kind == CiKind.Object || c.kind == CiKind.Word;
                // %%% Make this explode if addr isn't reachable until we figure out a
                // better strategy by giving X86.noreg as the temp for asAddress
                masm.cmpptr(rscratch1, addr);
            }
        }
    }

    @Override
    protected void emitCompare2Int(LIROpcode code, CiValue left, CiValue right, CiValue dst, LIROp2 op) {
        if (code == LIROpcode.Cmpfd2i || code == LIROpcode.Ucmpfd2i) {
            if (left.kind.isFloat()) {
                masm.cmpss2int(asXmmFloatReg(left), asXmmFloatReg(right), dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else if (left.kind.isDouble()) {
                masm.cmpsd2int(asXmmDoubleReg(left), asXmmDoubleReg(right), dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else {
                throw Util.unimplemented("no fpu stack");
            }
        } else {
            assert code == LIROpcode.Cmpl2i;
            CiRegister dest = dst.asRegister();
            Label high = new Label();
            Label done = new Label();
            Label isEqual = new Label();
            masm.cmpptr(left.asRegister(), right.asRegister());
            masm.jcc(ConditionFlag.equal, isEqual);
            masm.jcc(ConditionFlag.greater, high);
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
    protected void emitCallAlignment(LIROpcode code) {
        // make sure that the displacement word of the call ends up word aligned
        int offset = masm.codeBuffer.position();
        assert code == LIROpcode.DirectCall;
        offset += compilation.target.arch.machineCodeCallDisplacementOffset;
        while (offset++ % wordSize != 0) {
            masm.nop();
        }
    }

    @Override
    protected void emitIndirectCall(Object target, LIRDebugInfo info, CiValue callAddress) {
        CiRegister reg = compilation.target.scratchRegister;
        if (callAddress.isRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, reg.asValue(callAddress.kind), callAddress.kind, null, false);
        }
        masm.indirectCall(reg, target, info);
    }

    @Override
    protected void emitDirectCall(Object target, LIRDebugInfo info) {
        masm.directCall(target, info);
    }

    @Override
    protected void emitNativeCall(String symbol, LIRDebugInfo info, CiValue callAddress) {
        CiRegister reg = compilation.target.scratchRegister;
        if (callAddress.isRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, reg.asValue(callAddress.kind), callAddress.kind, null, false);
        }
        masm.nativeCall(reg, symbol, info);
    }

    @Override
    protected void emitThrow(CiValue exceptionPC, CiValue exceptionOop, LIRDebugInfo info, boolean unwind) {
       // exception object is not added to oop map by LinearScan
       // (LinearScan assumes that no oops are in fixed registers)
       // info.addRegisterOop(exceptionOop);
        masm.directCall(unwind ? CiRuntimeCall.UnwindException : CiRuntimeCall.HandleException, info);
        // enough room for two byte trap
        masm.nop();
    }

    @Override
    protected void emitShiftOp(LIROpcode code, CiValue left, CiValue count, CiValue dest, CiValue tmp) {
        // optimized version for linear scan:
        // * count must be already in ECX (guaranteed by LinearScan)
        // * left and dest must be equal
        // * tmp must be unused
        assert count.asRegister() == SHIFTCount : "count must be in ECX";
        assert left == dest : "left and dest must be equal";
        assert tmp.isIllegal() : "wasting a register if tmp is allocated";
        assert left.isRegister();

        if (left.kind.isInt()) {
            CiRegister value = left.asRegister();
            assert value != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl  : masm.shll(value); break;
                case Shr  : masm.sarl(value); break;
                case Ushr : masm.shrl(value); break;
                default   : throw Util.shouldNotReachHere();
            }
        } else {
            CiRegister lreg = left.asRegister();
            assert lreg != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl  : masm.shlq(lreg); break;
                case Shr  : masm.sarq(lreg); break;
                case Ushr : masm.shrq(lreg); break;
                default   : throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void emitShiftOp(LIROpcode code, CiValue left, int count, CiValue dest) {
        assert dest.isRegister();
        if (dest.kind.isInt()) {
            // first move left into dest so that left is not destroyed by the shift
            CiRegister value = dest.asRegister();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegister(), value);
            switch (code) {
                case Shl  : masm.shll(value, count); break;
                case Shr  : masm.sarl(value, count); break;
                case Ushr : masm.shrl(value, count); break;
                default   : throw Util.shouldNotReachHere();
            }
        } else {

            // first move left into dest so that left is not destroyed by the shift
            CiRegister value = dest.asRegister();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegister(), value);
            switch (code) {
                case Shl  : masm.shlq(value, count); break;
                case Shr  : masm.sarq(value, count); break;
                case Ushr : masm.shrq(value, count); break;
                default   : throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void emitSignificantBitOp(boolean most, CiValue inOpr1, CiValue dst) {
        assert dst.isRegister();
        CiRegister result = dst.asRegister();
        masm.xorl(result, result);
        masm.notq(result);
        if (inOpr1.isRegister()) {
            CiRegister value = inOpr1.asRegister();
            if (most) {
                masm.bsrl(result, value);
            } else {
                masm.bsfl(result, value);
            }
        } else {
            CiAddress laddr = asAddress(inOpr1);
            if (most) {
                masm.bsrl(result, laddr);
            } else {
                masm.bsfl(result, laddr);
            }
        }
    }

    @Override
    protected void emitAlignment() {
        masm.align(wordSize);
    }

    @Override
    protected void emitNegate(LIROp1 op) {
        CiValue left = op.operand();
        CiValue dest = op.result();
        assert left.isRegister();
        if (left.kind.isInt()) {
            masm.negl(left.asRegister());
            moveRegs(left.asRegister(), dest.asRegister());

        } else if (dest.kind.isFloat()) {
            if (asXmmFloatReg(left) != asXmmFloatReg(dest)) {
                masm.movflt(asXmmFloatReg(dest), asXmmFloatReg(left));
            }
            masm.callGlobalStub(op.globalStub, null, asXmmFloatReg(dest), asXmmFloatReg(dest));

        } else if (dest.kind.isDouble()) {
            if (asXmmDoubleReg(left) != asXmmDoubleReg(dest)) {
                masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(left));
            }

            masm.callGlobalStub(op.globalStub, null, asXmmDoubleReg(dest), asXmmDoubleReg(dest));
        } else {
            CiRegister lreg = left.asRegister();
            CiRegister dreg = dest.asRegister();
            masm.movq(dreg, lreg);
            masm.negq(dreg);
        }
    }

    @Override
    protected void emitLea(CiValue src, CiValue dest) {
        CiRegister reg = dest.asRegister();
        masm.leaq(reg, asAddress(src));
    }

    @Override
    protected void emitVolatileMove(CiValue src, CiValue dest, CiKind kind, LIRDebugInfo info) {
        assert kind == CiKind.Long : "only for volatile long fields";

        if (info != null) {
            asm.recordImplicitException(codePos(), info);
        }

        if (src.kind.isDouble()) {
            if (dest.isRegister()) {
                masm.movdq(dest.asRegister(), asXmmDoubleReg(src));
            } else if (dest.isStackSlot()) {
                masm.movsd(frameMap.toStackAddress(((CiStackSlot) dest)), asXmmDoubleReg(src));
            } else {
                assert dest.isAddress();
                masm.movsd(((CiAddress) dest), asXmmDoubleReg(src));
            }
        } else {
            assert dest.kind.isDouble();
            if (src.isStackSlot()) {
                masm.movdbl(asXmmDoubleReg(dest), frameMap.toStackAddress(((CiStackSlot) src)));
            } else {
                assert src.isAddress();
                masm.movdbl(asXmmDoubleReg(dest), ((CiAddress) src));
            }
        }
    }

    private static CiRegister asXmmDoubleReg(CiValue dest) {
        assert dest.kind.isDouble() : "must be double XMM register";
        CiRegister result = dest.asRegister();
        assert result.isFpu() : "must be XMM register";
        return result;
    }

    @Override
    protected void emitMembar() {
        // QQQ sparc TSO uses this,
        masm.membar(AMD64Assembler.MembarMaskBits.StoreLoad.mask());
    }

    @Override
    protected void emitMembarAcquire() {
        // No x86 machines currently require load fences
        // lir(). loadFence();
    }

    @Override
    protected void emitMembarRelease() {
        // No x86 machines currently require store fences
        // lir(). storeFence();
    }

    @Override
    protected void doPeephole(LIRList list) {
        // Do nothing for now
    }

    @Override
    protected void emitPrologue() {
        compilation.runtime.codePrologue(compilation.method, asm.codeBuffer);
    }

    @Override
    protected void emitCode(LocalStub s) {
        s.accept(this);
    }

    public static Object asRegisterOrConstant(CiValue operand) {
        if (operand.isRegister()) {
            return operand.asRegister();
        } else {
            assert operand.isConstant();
            return operand;
        }
    }

    @Override
    protected void emitXir(LIRXirInstruction instruction) {
        XirSnippet snippet = instruction.snippet;

        Label[] labels = new Label[snippet.template.labels.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }
        emitXirInstructions(instruction, snippet.template.fastPath, labels, instruction.getOperands());
        if (snippet.template.slowPath != null) {
            addSlowPath(new SlowPath(instruction, labels));
        }
    }

    @Override
    protected void emitSlowPath(SlowPath sp) {
        emitXirInstructions(sp.instruction, sp.instruction.snippet.template.slowPath, sp.labels, sp.instruction.getOperands());
        masm.nop();
    }

    public void emitXirInstructions(LIRXirInstruction xir, XirInstruction[] instructions, Label[] labels, CiValue[] operands) {
        LIRDebugInfo info = xir == null ? null : xir.info;

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
                    }
                    emitArithOp(LIROpcode.Div, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], null);
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
                    emitShiftOp(LIROpcode.Shl, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], IllegalValue);
                    break;

                case Shr:
                    emitShiftOp(LIROpcode.Shr, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], IllegalValue);
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
                    if ((Boolean) inst.extra && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiRegisterValue register = assureInRegister(pointer);
                    moveOp(new CiAddress(inst.kind, register, 0), result, inst.kind, null, false);
                    break;
                }

                case PointerStore: {
                    if ((Boolean) inst.extra && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    CiValue value = operands[inst.y().index];
                    CiValue pointer = operands[inst.x().index];
                    assert pointer.isVariableOrRegister();
                    moveOp(value, new CiAddress(inst.kind, pointer, 0), inst.kind, null, false);
                    break;
                }

                case PointerLoadDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;

                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();

                    CiValue src = null;
                    if (index.isConstant() && index.kind == CiKind.Int) {
                        CiConstant constantIndex = (CiConstant) index;
                        src = new CiAddress(inst.kind, pointer, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        src = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }

                    moveOp(src, result, inst.kind, null, false);
                    break;
                }

                case PointerStoreDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;

                    CiValue value = operands[inst.z().index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();

                    CiValue dst;
                    if (index.isConstant() && index.kind == CiKind.Int) {
                        CiConstant constantIndex = (CiConstant) index;
                        dst = new CiAddress(inst.kind, pointer, IllegalValue, scale, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        dst = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }

                    moveOp(value, dst, inst.kind, null, false);
                    break;
                }

                case PointerCAS:
                    break;

                case CallStub:
                    XirTemplate stubId = (XirTemplate) inst.extra;
                    CiRegister result = CiRegister.None;
                    if (inst.result != null) {
                        result = operands[inst.result.index].asRegister();
                    }
                    Object[] args = new Object[inst.arguments.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = asRegisterOrConstant(operands[inst.arguments[i].index]);
                    }
                    masm.callGlobalStub(stubId, info, result, args);
                    break;

                case CallRuntime:
                    CiKind[] signature = new CiKind[inst.arguments.length];
                    for (int i = 0; i < signature.length; i++) {
                        signature[i] = inst.arguments[i].kind;
                    }

                    CiCallingConvention cc = frameMap.runtimeCallingConvention(signature);
                    for (int i = 0; i < inst.arguments.length; i++) {
                        CiValue argumentLocation = cc.locations[i];
                        CiValue argumentSourceLocation = operands[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            moveOp(argumentSourceLocation, argumentLocation, argumentLocation.kind, null, false);
                        }
                    }

                    RiMethod method = (RiMethod) inst.extra;
                    masm.directCall(method, info);

                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        CiRegister returnRegister = compilation.target.registerConfig.getReturnRegister(inst.result.kind);
                        CiValue resultLocation = returnRegister.asValue(inst.result.kind.stackKind());
                        moveOp(resultLocation, operands[inst.result.index], inst.result.kind.stackKind(), null, false);
                    }
                    break;

                case Jmp: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    masm.jmp(label);
                    break;
                }
                case Jeq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.EQ, ConditionFlag.equal, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.NE, ConditionFlag.notEqual, operands, label);
                    break;
                }

                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GT, ConditionFlag.greater, operands, label);
                    break;
                }

                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GE, ConditionFlag.greaterEqual, operands, label);
                    break;
                }

                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.AE, ConditionFlag.aboveEqual, operands, label);
                    break;
                }

                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LT, ConditionFlag.less, operands, label);
                    break;
                }

                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LE, ConditionFlag.lessEqual, operands, label);
                    break;
                }

                case Bind: {
                    XirLabel l = (XirLabel) inst.extra;
                    Label label = labels[l.index];
                    asm.bind(label);
                    break;
                }
                case NullCheck: {
                    asm.recordImplicitException(codePos(), info);
                    CiValue pointer = operands[inst.x().index];
                    asm.nullCheck(pointer.asRegister());
                    break;
                }

                default:
                    throw Util.unimplemented("XIR operation " + inst.op);
            }
        }
    }

    private CiRegisterValue assureInRegister(CiValue pointer) {
        if (pointer.isConstant()) {
            CiRegisterValue register = compilation.target.scratchRegister.asValue(pointer.kind);
            moveOp(pointer, register, pointer.kind, null, false);
            return register;
        }

        assert pointer.isRegister();
        return (CiRegisterValue) pointer;
    }

    private void emitXirCompare(XirInstruction inst, Condition condition, ConditionFlag cflag, CiValue[] ops, Label label) {
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];
        emitCompare(condition, x, y, null);
        masm.jcc(cflag, label);
    }

    public void visitThrowStub(ThrowStub stub) {
        masm.bind(stub.entry);
        CiValue[] operands = stub.operands;
        Object[] params;
        if (operands != null) {
            params = new Object[operands.length];
            for (int i = 0; i < params.length; i++) {
                params[i] = asRegisterOrConstant(stub.operand(i));
            }
        } else {
            params = NO_PARAMS;
        }

        masm.callGlobalStub(stub.globalStub, stub.info, CiRegister.None, params);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenAssertionCode) {
            masm.shouldNotReachHere();
        }
    }
}
