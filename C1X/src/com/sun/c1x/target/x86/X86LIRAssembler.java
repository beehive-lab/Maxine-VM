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
package com.sun.c1x.target.x86;

import static com.sun.c1x.lir.LIROperand.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.FrameMap.*;
import com.sun.c1x.lir.LIRAddress.*;
import com.sun.c1x.lir.LIRCall.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.x86.X86Assembler.*;
import com.sun.c1x.util.*;
import com.sun.c1x.xir.*;
import com.sun.c1x.xir.CiXirAssembler.*;

/**
 * This class implements the x86-specific code generation for LIR.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class X86LIRAssembler extends LIRAssembler implements LocalStubVisitor {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final long NULLWORD = 0;
    private static final CiRegister SHIFTCount = X86.rcx;

    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    final CiTarget target;
    final X86MacroAssembler masm;
    final int wordSize;
    final CiRegister rscratch1;

    public X86LIRAssembler(C1XCompilation compilation) {
        super(compilation);

        masm = (X86MacroAssembler) compilation.masm();
        target = compilation.target;
        wordSize = target.arch.wordSize;
        rscratch1 = target.scratchRegister;

        assert !compilation.runtime.needsExplicitNullCheck(compilation.runtime.hubOffset());
    }

    private boolean isLiteralAddress(LIRAddress addr) {
        return isIllegal(addr.base) && isIllegal(addr.index);
    }

    private Address asAddress(LIRAddress addr) {
        assert isLegal(addr.base);
        CiRegister base = addr.base.asPointerRegister(compilation.target.arch);

        if (isIllegal(addr.index)) {
            return new Address(base, addr.displacement);
        } else {
            if (addr.index.isVariableOrRegister()) {
                CiRegister index = addr.index.asPointerRegister(compilation.target.arch);
                return new Address(base, index, Address.ScaleFactor.fromLog(addr.scale.ordinal()), addr.displacement);
            } else {
                throw Util.shouldNotReachHere();
            }
        }
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
    protected void emitReturn(LIROperand result) {
        // Add again to the stack pointer
        masm.increment(target.stackPointerRegister, initialFrameSizeInBytes());
        // TODO: Add Safepoint polling at return!
        masm.ret(0);
    }

    /**
     * Emits an instruction which assigns the address of the immediately succeeding instruction into {@code resultOpr}.
     * This satisfies the requirements for correctly translating the {@link LoadPC} HIR instruction.
     */
    @Override
    protected void emitReadPC(LIROperand resultOpr) {
        masm.lea(resultOpr.asRegister(), Address.InternalRelocation);
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, LIROperand resultOpr) {
        masm.lea(resultOpr.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    @Override
    protected void emitSafepoint(LIROperand tmp, LIRDebugInfo info) {
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

    private CiKind realKind(CiKind kind) {
        if (kind.isWord()) {
            return CiKind.Long;
        }
        return kind;
    }

    @Override
    protected void const2reg(LIROperand src, LIROperand dest, LIRDebugInfo info) {
        assert isConstant(src);
        assert dest.isVariableOrRegister();
        LIRConstant c = (LIRConstant) src;

        switch (realKind(c.kind)) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Int: {
                masm.movl(dest.asRegister(), c.asInt());
                break;
            }

            case Long: {
                if (c.asLong() == 0L) {
                    masm.xorptr(dest.asRegister(), dest.asRegister());
                } else {
                    masm.movptr(dest.asRegister(), c.asLong());
                }
                break;
            }

            case Object: {
                masm.movoop(dest.asRegister(), CiConstant.forObject(c.asObject()));
                break;
            }

            case Float: {
                if (dest.kind.isFloat()) {
                    if (c.asFloat() == 0.0f) {
                        masm.xorps(asXmmFloatReg(dest), asXmmFloatReg(dest));
                    } else {
                        masm.movflt(asXmmFloatReg(dest), masm.recordDataReferenceInCode(CiConstant.forFloat(c.asFloat())));
                    }
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            case Double: {
                if (dest.kind.isDouble()) {
                    if (c.asDouble() == 0.0d) {
                        masm.xorpd(asXmmDoubleReg(dest), asXmmDoubleReg(dest));
                    } else {
                        masm.movdbl(asXmmDoubleReg(dest), masm.recordDataReferenceInCode(CiConstant.forDouble(c.asDouble())));
                    }
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2stack(LIROperand src, LIROperand dest) {
        assert isConstant(src);
        assert dest.isStack();
        LIRConstant c = (LIRConstant) src;

        switch (c.kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int: // fall through
            case Float:
                masm.movl(frameMap.toStackAddress(dest, 0), c.asIntBits());
                break;

            case Object:
                masm.movoop(frameMap.toStackAddress(dest, 0), CiConstant.forObject(c.asObject()));
                break;

            case Long: // fall through
            case Double:
                masm.movptr(frameMap.toStackAddress(dest, 0), c.asLongBits());
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2mem(LIROperand src, LIROperand dest, CiKind kind, LIRDebugInfo info) {
        assert isConstant(src);
        assert isAddress(dest);
        LIRConstant c = (LIRConstant) src;
        LIRAddress addr = (LIRAddress) dest;

        int nullCheckHere = codePos();
        switch (realKind(kind)) {
            case Boolean: // fall through
            case Byte:
                masm.movb(asAddress(addr), c.asInt() & 0xFF);
                break;

            case Char: // fall through
            case Short:
                masm.movw(asAddress(addr), c.asInt() & 0xFFFF);
                break;

            case Int: // fall through
            case Float:
                masm.movl(asAddress(addr), c.asIntBits());
                break;

            case Object: // fall through
                if (c.asObject() == null) {
                    masm.movptr(asAddress(addr), NULLWORD);
                } else {
                    if (isLiteralAddress(addr)) {
                        masm.movoop(asAddress(addr), CiConstant.forObject(c.asObject()));
                        throw Util.shouldNotReachHere();
                    } else {
                        masm.movoop(asAddress(addr), CiConstant.forObject(c.asObject()));
                    }
                }
                break;

            case Long: // fall through
            case Double:
                if (isLiteralAddress(addr)) {
                    //masm().movptr(asAddress(addr, X86FrameMap.r15thread), c.asLongBits());
                    throw Util.shouldNotReachHere();
                } else {
                    masm.movptr(rscratch1, c.asLongBits());
                    nullCheckHere = codePos();
                    masm.movptr(asAddressLo(addr), rscratch1);
                }
                break;

            default:
                throw Util.shouldNotReachHere();
        }

        if (info != null) {
            //NullPointerExceptionStub stub = new NullPointerExceptionStub(pcOffset, cinfo);
            //emitCodeStub(stub);
            asm.recordImplicitException(nullCheckHere, info);
        }
    }

    Address asAddressHi(LIRAddress addr) {
        Address base = asAddress(addr);
        return new Address(base.base, base.index, base.scale, base.disp + wordSize);
    }

    Address asAddressLo(LIRAddress addr) {
        return asAddress(addr);
    }

    @Override
    protected void reg2reg(LIROperand src, LIROperand dest) {
        assert src.isVariableOrRegister();
        assert dest.isRegister();

        // move between cpu-registers
        if (dest.kind.isInt()) {
            if (src.kind == CiKind.Long) {
                moveRegs(src.asRegister(), dest.asRegister());
                return;
            }
            assert src.isRegister() : "must match";
            if (src.kind == CiKind.Object) {
                masm.verifyOop(src.asRegister());
            }
            moveRegs(src.asRegister(), dest.asRegister());

        } else if (dest.kind.isLong()) {
            if (src.kind == CiKind.Object) {
                masm.verifyOop(src.asRegister());
                moveRegs(src.asRegister(), dest.asRegister());
                return;
            }
            assert src.kind.isLong() && src.isRegister() : "must match";
            CiRegister sreg = src.asRegister();
            CiRegister dreg = dest.asRegister();
            moveRegs(sreg, dreg);
            // move between xmm-registers
        } else if (dest.kind.isFloat()) {
            masm.movflt(asXmmFloatReg(dest), asXmmFloatReg(src));
        } else if (dest.kind.isDouble()) {
            masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(src));
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2stack(LIROperand src, LIROperand dest, CiKind kind) {
        assert src.isRegister();
        assert dest.isStack();

        if (src.kind.isInt()) {
            Address dst = frameMap.toStackAddress(dest, 0);
            if (kind == CiKind.Object || kind == CiKind.Word) {
                if (kind == CiKind.Object) {
                    masm.verifyOop(src.asRegister());
                }
                masm.movptr(dst, src.asRegister());
            } else {
                masm.movl(dst, src.asRegister());
            }

        } else if (src.kind.isLong()) {
            masm.movptr(frameMap.toStackAddress(dest, 0), src.asRegister());

        } else if (src.kind.isFloat()) {
            Address dstAddr = frameMap.toStackAddress(dest, 0);
            masm.movflt(dstAddr, asXmmFloatReg(src));

        } else if (src.kind.isDouble()) {
            Address dstAddr = frameMap.toStackAddress(dest, 0);
            masm.movdbl(dstAddr, asXmmDoubleReg(src));

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2mem(LIROperand src, LIROperand dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        LIRAddress toAddr = (LIRAddress) dest;

        if (kind == CiKind.Object) {
            masm.verifyOop(src.asRegister());
        }
        if (info != null) {
            //NullPointerExceptionStub stub = new NullPointerExceptionStub(pcOffset, cinfo);
            //emitCodeStub(stub);
            asm.recordImplicitException(codePos(), info);
        }

        switch (realKind(kind)) {
            case Float: {
                if (src.kind.isFloat()) {
                    masm.movflt(asAddress(toAddr), asXmmFloatReg(src));
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            case Double: {
                if (src.kind.isDouble()) {
                    masm.movdbl(asAddress(toAddr), asXmmDoubleReg(src));
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            case Object:
                masm.movptr(asAddress(toAddr), src.asRegister());
                break;
            case Int:
                masm.movl(asAddress(toAddr), src.asRegister());
                break;

            case Long: {
                CiRegister fromreg = src.asRegister();
                masm.movptr(asAddressLo(toAddr), fromreg);
                break;
            }

            case Byte: // fall through
            case Boolean: {
                CiRegister srcReg = src.asRegister();
                Address dstAddr = asAddress(toAddr);
                assert compilation.target.isP6() || srcReg.isByte() : "must use byte registers if not P6";
                masm.movb(dstAddr, srcReg);
                break;
            }

            case Char: // fall through
            case Short:
                masm.movw(asAddress(toAddr), src.asRegister());
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    private static CiRegister asXmmFloatReg(LIROperand src) {
        assert src.kind.isFloat() : "must be single xmm";
        CiRegister result = src.asRegister();
        assert result.isXmm() : "must be xmm";
        return result;
    }

    @Override
    protected void stack2reg(LIROperand src, LIROperand dest, CiKind kind) {
        assert src.isStack();
        assert dest.isRegister();

        if (dest.kind.isInt()) {
            if (kind == CiKind.Object || kind == CiKind.Word) {
                masm.movptr(dest.asRegister(), frameMap.toStackAddress(src, 0));
                if (kind == CiKind.Object) {
                    masm.verifyOop(dest.asRegister());
                }
            } else {
                masm.movl(dest.asRegister(), frameMap.toStackAddress(src, 0));
            }

        } else if (dest.kind.isLong()) {
            Address srcAddrLO = frameMap.toStackAddress(src, 0);
            masm.movptr(dest.asRegister(), srcAddrLO);
        } else if (dest.kind.isFloat()) {
            Address srcAddr = frameMap.toStackAddress(src, 0);
            masm.movflt(asXmmFloatReg(dest), srcAddr);

        } else if (dest.kind.isDouble()) {
            Address srcAddr = frameMap.toStackAddress(src, 0);
            masm.movdbl(asXmmDoubleReg(dest), srcAddr);

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void mem2mem(LIROperand src, LIROperand dest, CiKind kind) {
        if (dest.kind.isInt()) {
            masm.pushl(asAddress((LIRAddress) src));
            masm.popl(asAddress((LIRAddress) dest));
        } else {
            masm.pushptr(asAddress((LIRAddress) src));
            masm.popptr(asAddress((LIRAddress) dest));
        }
    }

    @Override
    protected void mem2stack(LIROperand src, LIROperand dest, CiKind kind) {
        if (dest.kind.isInt()) {
            masm.pushl(asAddress((LIRAddress) src));
            masm.popl(frameMap.toStackAddress(dest, 0));
        } else {
            masm.pushptr(asAddress((LIRAddress) src));
            masm.popptr(frameMap.toStackAddress(dest, 0));
        }
    }

    @Override
    protected void stack2stack(LIROperand src, LIROperand dest, CiKind kind) {
        if (src.kind.isInt()) {
            masm.pushl(frameMap.toStackAddress(src, 0));
            masm.popl(frameMap.toStackAddress(dest, 0));
        } else {
            masm.pushptr(frameMap.toStackAddress(src, 0));
            masm.popptr(frameMap.toStackAddress(dest, 0));
        }
    }

    @Override
    protected void mem2reg(LIROperand src, LIROperand dest, CiKind kind, LIRDebugInfo info, boolean unaligned) {
        assert isAddress(src);
        assert dest.isVariableOrRegister();

        LIRAddress addr = (LIRAddress) src;
        Address fromAddr = asAddress(addr);

        switch (kind) {
            case Boolean: // fall through
            case Byte: // fall through
            case Char: // fall through
            case Short:
                if (!compilation.target.isP6() && !fromAddr.uses(dest.asRegister())) {
                    // on pre P6 processors we may get partial register stalls
                    // so blow away the value of toRinfo before loading a
                    // partial word into it. Do it here so that it precedes
                    // the potential patch point below.
                    masm.xorptr(dest.asRegister(), dest.asRegister());
                }
                break;
        }

        if (info != null) {
            asm.recordImplicitException(codePos(), info);
        }

        switch (kind) {
            case Float: {
                assert dest.kind.isFloat();
                masm.movflt(asXmmFloatReg(dest), fromAddr);
                break;
            }

            case Double: {
                assert dest.kind.isDouble();
                masm.movdbl(asXmmDoubleReg(dest), fromAddr);
                break;
            }

            case Word:
            case Object:
                masm.movptr(dest.asRegister(), fromAddr);
                break;
            case Int:
                // %%% could this be a movl? this is a safer but longer instruction
                masm.movl2ptr(dest.asRegister(), fromAddr);
                break;

            case Long: {
                CiRegister dreg = dest.asRegister();
                masm.movptr(dreg, asAddressLo(addr));
                break;
            }

            case Boolean:
            case Byte: {
                CiRegister destReg = dest.asRegister();
                assert compilation.target.isP6() || destReg.isByte() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm.movsxb(destReg, fromAddr);
                } else {
                    masm.movb(destReg, fromAddr);
                    masm.shll(destReg, 24);
                    masm.sarl(destReg, 24);
                }
                // These are unsigned so the zero extension on 64bit is just what we need
                break;
            }

            case Char: {
                CiRegister destReg = dest.asRegister();
                assert compilation.target.isP6() || destReg.isByte() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm.movzxl(destReg, fromAddr);
                } else {
                    masm.movw(destReg, fromAddr);
                }
                // This is unsigned so the zero extension on 64bit is just what we need
                // lir(). movl2ptr(destReg, destReg);
                break;
            }

            case Short: {
                CiRegister destReg = dest.asRegister();
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm.movswl(destReg, fromAddr);
                } else {
                    masm.movw(destReg, fromAddr);
                    masm.shll(destReg, 16);
                    masm.sarl(destReg, 16);
                }
                // Might not be needed in 64bit but certainly doesn't hurt (except for code size)
                masm.movl2ptr(destReg, destReg);
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }

        if (kind == CiKind.Object) {
            masm.verifyOop(dest.asRegister());
        }
    }

    @Override
    protected void emitReadPrefetch(LIROperand src) {
        LIRAddress addr = (LIRAddress) src;
        Address fromAddr = asAddress(addr);

        if (compilation.target.supportsSSE()) {
            switch (C1XOptions.ReadPrefetchInstr) {
                case 0:
                    masm.prefetchnta(fromAddr);
                    break;
                case 1:
                    masm.prefetcht0(fromAddr);
                    break;
                case 2:
                    masm.prefetcht2(fromAddr);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (compilation.target.supports3DNOW()) {
            masm.prefetchr(fromAddr);
        }
    }

    @Override
    protected void emitOp3(LIROp3 op) {
        switch (op.code) {
            case Idiv:
            case Irem:
                arithmeticIdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Ldiv:
            case Lrem:
                arithmeticLdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            case Wdiv:
            case Wdivi:
            case Wrem:
            case Wremi:
                arithmeticWdiv(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    private boolean assertEmitBranch(LIRBranch op) {
        assert op.block() == null || op.block().label() == op.label() : "wrong label";
        if (op.block() != null) {
            branchTargetBlocks.add(op.block());
        }
        if (op.ublock() != null) {
            branchTargetBlocks.add(op.ublock());
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
                assert op.ublock() != null : "must have unordered successor";
                masm.jcc(ConditionFlag.parity, op.ublock().label());
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
                    default           : throw Util.shouldNotReachHere();
                }
            }
            masm.jcc(acond, (op.label()));
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        LIROperand src = op.operand();
        LIROperand dest = op.result();
        Label endLabel = new Label();
        CiRegister srcRegister = src.asRegister();
        CiRegister rscratch1 = compilation.target.scratchRegister;
        switch (op.bytecode) {
            case Bytecodes.I2L:
                masm.movl2ptr(dest.asRegister(), srcRegister);
                break;

            case Bytecodes.L2I:
                moveRegs(srcRegister, dest.asRegister());
                break;

            case Bytecodes.I2B:
                moveRegs(srcRegister, dest.asRegister());
                masm.signExtendByte(dest.asRegister());
                break;

            case Bytecodes.I2C:
                moveRegs(srcRegister, dest.asRegister());
                masm.andl(dest.asRegister(), 0xFFFF);
                break;

            case Bytecodes.I2S:
                moveRegs(srcRegister, dest.asRegister());
                masm.signExtendShort(dest.asRegister());
                break;

            case Bytecodes.F2D:
                masm.cvtss2sd(asXmmDoubleReg(dest), asXmmFloatReg(src));
                break;

            case Bytecodes.D2F:
                masm.cvtsd2ss(asXmmFloatReg(dest), asXmmDoubleReg(src));
                break;

            case Bytecodes.I2F:
                masm.cvtsi2ssl(asXmmFloatReg(dest), srcRegister);
                break;
            case Bytecodes.I2D:
                masm.cvtsi2sdl(asXmmDoubleReg(dest), srcRegister);
                break;

            case Bytecodes.F2I: {
                assert srcRegister.isXmm() && dest.isRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttss2sil(dest.asRegister(), srcRegister);
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case Bytecodes.D2I: {
                assert srcRegister.isXmm() && dest.isRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2sil(dest.asRegister(), asXmmDoubleReg(src));
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case Bytecodes.L2F:
                masm.cvtsi2ssq(asXmmFloatReg(dest), srcRegister);
                break;

            case Bytecodes.L2D:
                masm.cvtsi2sdq(asXmmDoubleReg(dest), srcRegister);
                break;

            case Bytecodes.F2L: {
                assert srcRegister.isXmm() && dest.kind.isLong() : "must both be XMM register (no fpu stack)";
                masm.cvttss2siq(dest.asRegister(), asXmmFloatReg(src));
                masm.mov64(rscratch1, Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            case Bytecodes.D2L: {
                assert srcRegister.isXmm() && dest.kind.isLong() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2siq(dest.asRegister(), asXmmDoubleReg(src));
                masm.mov64(rscratch1, Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(ConditionFlag.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            case Bytecodes.MOV_I2F:
                masm.movdl(asXmmFloatReg(dest), srcRegister);
                break;

            case Bytecodes.MOV_L2D:
                masm.movdq(asXmmDoubleReg(dest), srcRegister);
                break;

            case Bytecodes.MOV_F2I:
                masm.movdl(dest.asRegister(), asXmmFloatReg(src));
                break;

            case Bytecodes.MOV_D2L:
                masm.movdq(dest.asRegister(), asXmmDoubleReg(src));
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        CiRegister addr = op.address().asRegister();
        CiRegister newval = op.newValue().asRegister();
        CiRegister cmpval = op.cmpValue().asRegister();
        assert cmpval == X86.rax : "wrong register";
        assert newval != null : "new val must be register";
        assert cmpval != newval : "cmp and new values must be in different registers";
        assert cmpval != addr : "cmp and addr must be in different registers";
        assert newval != addr : "new value and addr must be in different registers";
        if (compilation.runtime.isMP()) {
            masm.lock();
        }
        if (op.code == LIROpcode.CasInt) {
            masm.cmpxchgl(newval, new Address(addr, 0));
        } else {
            assert op.code == LIROpcode.CasObj || op.code == LIROpcode.CasLong || op.code == LIROpcode.CasWord;
            masm.cmpxchgptr(newval, new Address(addr, 0));
        }
    }

    @Override
    protected void emitConditionalMove(Condition condition, LIROperand opr1, LIROperand opr2, LIROperand result) {
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
            case AE:
                acond = ConditionFlag.aboveEqual;
                ncond = ConditionFlag.below;
                break;
            default:
                throw Util.shouldNotReachHere();
        }

        LIROperand def = opr1; // assume left operand as default
        LIROperand other = opr2;

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
        } else if (def.isStack()) {
            stack2reg(def, result, result.kind);
        } else if (isConstant(def)) {
            const2reg(def, result, null);
        } else {
            throw Util.shouldNotReachHere();
        }

        if (compilation.target.supportsCmov() && !isConstant(other)) {
            // optimized version that does not require a branch
            if (other.isRegister()) {
                assert other.asRegister() != result.asRegister() : "other already overwritten by previous move";
                if (other.kind.isInt()) {
                    masm.cmov(ncond, result.asRegister(), other.asRegister());
                } else {
                    masm.cmovptr(ncond, result.asRegister(), other.asRegister());
                }
            } else if (other.isStack()) {
                if (other.kind.isInt()) {
                    masm.cmovl(ncond, result.asRegister(), frameMap.toStackAddress(other, 0));
                } else {
                    masm.cmovptr(ncond, result.asRegister(), frameMap.toStackAddress(other, 0));
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            // conditional move not available, use emit a branch and move
            Label skip = new Label();
            masm.jcc(acond, skip);
            if (other.isVariableOrRegister()) {
                reg2reg(other, result);
            } else if (other.isStack()) {
                stack2reg(other, result, result.kind);
            } else if (isConstant(other)) {
                const2reg(other, result, null);
            } else {
                throw Util.shouldNotReachHere();
            }
            masm.bind(skip);
        }
    }

    @Override
    protected void emitArithOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dest, LIRDebugInfo info) {
        assert info == null : "should never be used :  idiv/irem and ldiv/lrem not handled by this method";
        assert left.kind == right.kind;
        assert left.equals(dest) : "left and dest must be equal";

        if (left.isRegister() && left.kind.isInt()) {
            CiRegister lreg = left.asRegister();

            if (right.isRegister()) {
                // cpu register - cpu register
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add : masm.addl(lreg, rreg); break;
                    case Sub : masm.subl(lreg, rreg); break;
                    case Mul : masm.imull(lreg, rreg); break;
                    default  : throw Util.shouldNotReachHere();
                }

            } else if (right.isStack()) {
                // cpu register - stack
                Address raddr = frameMap.toStackAddress(right, 0);
                switch (code) {
                    case Add : masm.addl(lreg, raddr); break;
                    case Sub : masm.subl(lreg, raddr); break;
                    default  : throw Util.shouldNotReachHere();
                }

            } else if (isConstant(right)) {
                // cpu register - constant
                int c = ((LIRConstant) right).asInt();
                switch (code) {
                    case Add : masm.increment(lreg, c); break;
                    case Sub : masm.decrement(lreg, c); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (left.kind.isLong() && left.isRegister()) {
            CiRegister lreg = left.asRegister();

            if (right.isRegister()) {
                // cpu register - cpu register
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add : masm.addptr(lreg, rreg); break;
                    case Sub : masm.subptr(lreg, rreg); break;
                    case Mul : masm.imulq(lreg, rreg);  break;
                    default  : throw Util.shouldNotReachHere();
                }

            } else if (isConstant(right)) {
                // cpu register - constant
                long c = ((LIRConstant) right).asLongBits();
                masm.movptr(rscratch1, c);
                switch (code) {
                    case Add : masm.addptr(lreg, rscratch1); break;
                    case Sub : masm.subptr(lreg, rscratch1); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (left.kind.isFloat() && left.isRegister()) {
            CiRegister lreg = left.asRegister();
            assert lreg.isXmm() : "must be xmm";

            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                assert rreg.isXmm() : "must be xmm";
                switch (code) {
                    case Add : masm.addss(lreg, rreg); break;
                    case Sub : masm.subss(lreg, rreg); break;
                    case Mul : masm.mulss(lreg, rreg); break;
                    case Div : masm.divss(lreg, rreg); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isStack()) {
                    raddr = frameMap.toStackAddress(right, 0);
                } else if (isConstant(right)) {
                    raddr = masm.recordDataReferenceInCode(CiConstant.forFloat(((LIRConstant) right).asFloat()));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add : masm.addss(lreg, raddr); break;
                    case Sub : masm.subss(lreg, raddr); break;
                    case Mul : masm.mulss(lreg, raddr); break;
                    case Div : masm.divss(lreg, raddr); break;
                    default  : throw Util.shouldNotReachHere();
                }
            }

        } else if (left.kind.isDouble() && left.isRegister()) {

            CiRegister lreg = left.asRegister();
            assert lreg.isXmm();
            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                assert rreg.isXmm();
                switch (code) {
                    case Add : masm.addsd(lreg, rreg); break;
                    case Sub : masm.subsd(lreg, rreg); break;
                    case Mul : masm.mulsd(lreg, rreg); break;
                    case Div : masm.divsd(lreg, rreg); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isStack()) {
                    raddr = frameMap.toStackAddress(right, 0);
                } else if (isConstant(right)) {
                    raddr = masm.recordDataReferenceInCode(CiConstant.forDouble(((LIRConstant) right).asDouble()));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add : masm.addsd(lreg, raddr); break;
                    case Sub : masm.subsd(lreg, raddr); break;
                    case Mul : masm.mulsd(lreg, raddr); break;
                    case Div : masm.divsd(lreg, raddr); break;
                    default  : throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isStack() || isAddress(left)) {

            Address laddr;
            if (left.isStack()) {
                laddr = frameMap.toStackAddress(left, 0);
            } else if (isAddress(left)) {
                laddr = asAddress((LIRAddress) left);
            } else {
                throw Util.shouldNotReachHere();
            }

            if (right.isRegister()) {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add : masm.addl(laddr, rreg); break;
                    case Sub : masm.subl(laddr, rreg); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else if (isConstant(right)) {
                int c = ((LIRConstant) right).asInt();
                switch (code) {
                    case Add : masm.incrementl(laddr, c); break;
                    case Sub : masm.decrementl(laddr, c); break;
                    default  : throw Util.shouldNotReachHere();
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitIntrinsicOp(LIROpcode code, LIROperand value, LIROperand unused, LIROperand dest, LIROp2 op) {
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
    protected void emitLogicOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst) {
        assert left.isRegister();
        if (left.kind.isInt()) {
            CiRegister reg = left.asRegister();
            if (isConstant(right)) {
                int val = ((LIRConstant) right).asInt();
                switch (code) {
                    case LogicAnd : masm.andl(reg, val); break;
                    case LogicOr  : masm.orl(reg, val); break;
                    case LogicXor : masm.xorl(reg, val); break;
                    default       : throw Util.shouldNotReachHere();
                }
            } else if (right.isStack()) {
                // added support for stack operands
                Address raddr = frameMap.toStackAddress(right, 0);
                switch (code) {
                    case LogicAnd : masm.andl(reg, raddr); break;
                    case LogicOr  : masm.orl(reg, raddr); break;
                    case LogicXor : masm.xorl(reg, raddr); break;
                    default       : throw Util.shouldNotReachHere();
                }
            } else {
                CiRegister rright = right.asRegister();
                switch (code) {
                    case LogicAnd : masm.andptr(reg, rright); break;
                    case LogicOr  : masm.orptr(reg, rright); break;
                    case LogicXor : masm.xorptr(reg, rright); break;
                    default       : throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister());
        } else {
            CiRegister lreg = left.asRegister();
            if (isConstant(right)) {
                LIRConstant rightConstant = (LIRConstant) right;
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
                    case LogicAnd : masm.andptr(lreg, rreg); break;
                    case LogicOr  : masm.orptr(lreg, rreg); break;
                    case LogicXor : masm.xorptr(lreg, rreg); break;
                    default       : throw Util.shouldNotReachHere();
                }
            }

            CiRegister dreg = dst.asRegister();
            moveRegs(lreg, dreg);
        }
    }

    void arithmeticIdiv(LIROpcode code, LIROperand left, LIROperand right, LIROperand result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() || isConstant(right) : "right must be register or constant";
        assert result.isRegister() : "result must be register";

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();

        if (isConstant(right)) {
            int divisor = ((LIRConstant) right).asInt();
            assert divisor > 0 && Util.isPowerOf2(divisor) : "divisor must be power of two";
            if (code == LIROpcode.Idiv) {
                assert lreg == X86.rax : "dividend must be rax";
                masm.cdql(); // sign extend into rdx:rax
                if (divisor == 2) {
                    masm.subl(lreg, X86.rdx);
                } else {
                    masm.andl(X86.rdx, divisor - 1);
                    masm.addl(lreg, X86.rdx);
                }
                masm.sarl(lreg, Util.log2(divisor));
                moveRegs(lreg, dreg);
            } else if (code == LIROpcode.Irem) {
                Label done = new Label();
                masm.mov(dreg, lreg);
                masm.andl(dreg, 0x80000000 | (divisor - 1));
                masm.jcc(ConditionFlag.positive, done);
                masm.decrement(dreg, 1);
                masm.orl(dreg, ~(divisor - 1));
                masm.increment(dreg, 1);
                masm.bind(done);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            CiRegister rreg = right.asRegister();
            assert lreg == X86.rax : "left register must be rax";
            assert rreg != X86.rdx : "right register must not be rdx";

            moveRegs(lreg, X86.rax);

            Label continuation = new Label();

            if (C1XOptions.GenSpecialDivChecks) {
                // check for special case of Integer.MIN_VALUE / -1
                Label normalCase = new Label();
                masm.cmpl(X86.rax, Integer.MIN_VALUE);
                masm.jcc(ConditionFlag.notEqual, normalCase);
                if (code == LIROpcode.Irem) {
                    // prepare X86Register.rdx for possible special case where remainder = 0
                    masm.xorl(X86.rdx, X86.rdx);
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
                moveRegs(X86.rdx, dreg); // result is in rdx
            } else if (code == LIROpcode.Idiv) {
                moveRegs(X86.rax, dreg);
            } else {
                throw Util.shouldNotReachHere();
            }
        }
    }

    void arithmeticLdiv(LIROpcode code, LIROperand left, LIROperand right, LIROperand result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";
        assert result.kind.isLong();

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();
        CiRegister rreg = right.asRegister();
        assert lreg == X86.rax : "left register must be rax";
        assert rreg != X86.rdx : "right register must not be rdx";

        moveRegs(lreg, X86.rax);

        Label continuation = new Label();

        if (C1XOptions.GenSpecialDivChecks) {
            // check for special case of Long.MIN_VALUE / -1
            Label normalCase = new Label();
            masm.mov64(X86.rdx, Long.MIN_VALUE);
            masm.cmpq(X86.rax, X86.rdx);
            masm.jcc(ConditionFlag.notEqual, normalCase);
            if (code == LIROpcode.Lrem) {
                // prepare X86Register.rdx for possible special case (where remainder = 0)
                masm.xorq(X86.rdx, X86.rdx);
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
            moveRegs(X86.rdx, dreg);
        } else if (code == LIROpcode.Ldiv) {
            moveRegs(X86.rax, dreg);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    void arithmeticWdiv(LIROpcode code, LIROperand left, LIROperand right, LIROperand result, LIRDebugInfo info) {
        assert left.isRegister() : "left must be register";
        assert right.isRegister() : "right must be register";
        assert result.isRegister() : "result must be register";

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();
        CiRegister rreg = right.asRegister();
        assert lreg == X86.rax : "left register must be rax";
        assert rreg != X86.rdx : "right register must not be rdx";

        if (code == LIROpcode.Wdivi || code == LIROpcode.Wremi) {
            // Zero the high 32 bits of the divisor
            masm.movzxd(rreg, rreg);
        }

        moveRegs(lreg, X86.rax);

        int offset = masm.codeBuffer.position();
        masm.divq(rreg);

        asm.recordImplicitException(offset, info);
        if (code == LIROpcode.Wrem || code == LIROpcode.Wremi) {
            moveRegs(X86.rdx, dreg);
        } else if (code == LIROpcode.Wdiv || code == LIROpcode.Wdivi) {
            moveRegs(X86.rax, dreg);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompare(Condition condition, LIROperand opr1, LIROperand opr2, LIROp2 op) {
        if (opr1.isRegister()) {
            if (opr1.kind.isInt()) {
                CiRegister reg1 = opr1.asRegister();
                if (opr2.isRegister()) {
                    // cpu register - cpu register
                    if (opr1.kind == CiKind.Object || opr1.kind == CiKind.Word) {
                        masm.cmpptr(reg1, opr2.asRegister());
                    } else {
                        assert opr2.kind != CiKind.Object && opr2.kind != CiKind.Word : "cmp int : oop?";
                        masm.cmpl(reg1, opr2.asRegister());
                    }
                } else if (opr2.isStack()) {
                    // cpu register - stack
                    if (opr1.kind == CiKind.Object || opr1.kind == CiKind.Word) {
                        masm.cmpptr(reg1, frameMap.toStackAddress(opr2, 0));
                    } else {
                        masm.cmpl(reg1, frameMap.toStackAddress(opr2, 0));
                    }
                } else if (isConstant(opr2)) {
                    // cpu register - constant
                    LIRConstant c = (LIRConstant) opr2;
                    if (c.kind == CiKind.Int) {
                        masm.cmpl(reg1, c.asInt());
                    } else if (c.kind == CiKind.Object) {
                        // In 64bit oops are single register
                        Object o = c.asObject();
                        if (o == null) {
                            masm.cmpptr(reg1, (int) NULLWORD);
                        } else {
                            masm.movoop(rscratch1, CiConstant.forObject(o));
                            masm.cmpptr(reg1, rscratch1);
                        }
                    } else {
                        throw Util.shouldNotReachHere();
                    }
                    // cpu register - address
                } else if (isAddress(opr2)) {
                    if (op != null && op.info != null) {
                        asm.recordImplicitException(codePos(), op.info);
                    }
                    masm.cmpl(reg1, asAddress((LIRAddress) opr2));
                } else {
                    throw Util.shouldNotReachHere();
                }
            } else if (opr1.kind.isFloat()) {
                CiRegister reg1 = asXmmFloatReg(opr1);
                assert reg1.isXmm();
                if (opr2.isRegister()) {
                    // xmm register - xmm register
                    masm.ucomiss(reg1, asXmmFloatReg(opr2));
                } else if (opr2.isStack()) {
                    // xmm register - stack
                    masm.ucomiss(reg1, frameMap.toStackAddress(opr2, 0));
                } else if (isConstant(opr2)) {
                    // xmm register - constant
                    masm.ucomiss(reg1, masm.recordDataReferenceInCode(CiConstant.forFloat(((LIRConstant) opr2).asFloat())));
                } else if (isAddress(opr2)) {
                    // xmm register - address
                    if (op != null && op.info != null) {
                        asm.recordImplicitException(codePos(), op.info);
                    }
                    masm.ucomiss(reg1, asAddress((LIRAddress) opr2));
                } else {
                    throw Util.shouldNotReachHere();
                }

            } else if (opr1.kind.isDouble()) {
                CiRegister reg1 = asXmmDoubleReg(opr1);
                assert reg1.isXmm();
                if (opr2.isRegister()) {
                    // xmm register - xmm register
                    masm.ucomisd(reg1, asXmmDoubleReg(opr2));
                } else if (opr2.isStack()) {
                    // xmm register - stack
                    masm.ucomisd(reg1, frameMap.toStackAddress(opr2, 0));
                } else if (isConstant(opr2)) {
                    // xmm register - constant
                    masm.ucomisd(reg1, masm.recordDataReferenceInCode(CiConstant.forDouble(((LIRConstant) opr2).asDouble())));
                } else if (isAddress(opr2)) {
                    // xmm register - address
                    if (op != null && op.info != null) {
                        asm.recordImplicitException(codePos(), op.info);
                    }
                    masm.ucomisd(reg1, asAddress((LIRAddress) opr2));
                } else {
                    throw Util.shouldNotReachHere();
                }

            } else {
                CiRegister xreg = opr1.asRegister();
                if (opr2.isRegister()) {
                    masm.cmpptr(xreg, opr2.asRegister());
                } else if (isConstant(opr2)) {
                    // cpu register - constant 0
                    LIRConstant constantOpr2 = (LIRConstant) opr2;
                    assert constantOpr2.asLong() == 0 : "only handles zero";
                    masm.cmpptr(xreg, (int) constantOpr2.asLong());
                } else {
                    throw Util.shouldNotReachHere();
                }

            }
        } else if (isAddress(opr1) && isConstant(opr2)) {
            LIRConstant c = ((LIRConstant) opr2);

            if (c.kind == CiKind.Object) {
                assert condition == Condition.EQ || condition == Condition.NE : "need to reverse";
                masm.movoop(rscratch1, CiConstant.forObject(c.asObject()));
            }
            if (op != null && op.info != null) {
                //NullPointerExceptionStub stub = new NullPointerExceptionStub(pcOffset, cinfo);
                //emitCodeStub(stub);
                asm.recordImplicitException(codePos(), op.info);
            }
            // special case: address - constant
            LIRAddress addr = (LIRAddress) opr1;
            if (c.kind == CiKind.Int) {
                masm.cmpl(asAddress(addr), c.asInt());
            } else if (c.kind == CiKind.Object || c.kind == CiKind.Word) {
                // %%% Make this explode if addr isn't reachable until we figure out a
                // better strategy by giving X86.noreg as the temp for asAddress
                masm.cmpptr(rscratch1, asAddress(addr));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareFloatInt(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst, LIROp2 op) {
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
            masm.decrement(dest, 1);
            masm.jmp(done);
            masm.bind(high);
            masm.xorptr(dest, dest);
            masm.increment(dest, 1);
            masm.jmp(done);
            masm.bind(isEqual);
            masm.xorptr(dest, dest);

            masm.bind(done);
        }
    }

    @Override
    protected void emitCallAlignment(LIROpcode code) {
        if (compilation.runtime.isMP()) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = masm.codeBuffer.position();
            switch (code) {
                case DirectCall:
                    offset += compilation.target.arch.machineCodeCallDisplacementOffset;
                    break;
                case VirtualCall:
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
            while (offset++ % wordSize != 0) {
                masm.nop();
            }
        }
    }

    @Override
    protected void emitIndirectCall(Object target, LIRDebugInfo info, LIROperand callAddress) {
        CiRegister reg = compilation.target.scratchRegister;
        if (callAddress.isVariableOrRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, forRegister(callAddress.kind, reg), callAddress.kind, null, false);
        }
        masm.indirectCall(reg, target, info);
    }

    @Override
    protected void emitDirectCall(Object target, LIRDebugInfo info) {
        masm.directCall(target, info);
    }

    @Override
    protected void emitNativeCall(NativeFunction nativeFunction, LIRDebugInfo info) {
        CiRegister reg = compilation.target.scratchRegister;
        LIROperand callAddress = nativeFunction.address;
        if (callAddress.isVariableOrRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, forRegister(callAddress.kind, reg), callAddress.kind, null, false);
        }
        masm.nativeCall(reg, nativeFunction.symbol, info);
    }

    @Override
    protected void emitVirtualCall(RiMethod method, LIROperand receiver, LIRDebugInfo info) {
        assert method.isLoaded() : "method is not resolved";
        assert receiver != null && receiver.isVariableOrRegister() : "Receiver must be in a register";

        int vtableOffset = compilation.runtime.vtableEntryMethodOffsetInBytes() + compilation.runtime.vtableStartOffset() + method.vtableIndex() * compilation.runtime.vtableEntrySize();

        asm.recordImplicitException(codePos(), info); // record deopt info for next instruction (possible NPE)
        masm.movq(rscratch1, new Address(receiver.asRegister(), compilation.runtime.hubOffset())); // load hub
        Address callAddress = new Address(rscratch1, vtableOffset);
        masm.indirectCall(callAddress, method, info); // perform indirect call
    }

    @Override
    protected void emitInterfaceCall(RiMethod method, LIROperand receiver, LIRDebugInfo info, GlobalStub globalStub) {
        assert method.isLoaded() : "method is not resolved";
        assert receiver != null && receiver.isVariableOrRegister() : "Receiver must be in a register";

        // TODO: emit interface ID calculation inline
        masm.movl(rscratch1, method.interfaceID());
        // asm.recordExceptionHandlers(codePos(), info);
        masm.callGlobalStub(globalStub, info, rscratch1, receiver.asRegister(), rscratch1);
        masm.addq(rscratch1, method.indexInInterface() * wordSize);

        masm.addq(rscratch1, new Address(receiver.asRegister(), compilation.runtime.hubOffset()));
        masm.indirectCall(new Address(rscratch1, 0), method, info);
    }

    @Override
    protected void emitThrow(LIROperand exceptionPC, LIROperand exceptionOop, LIRDebugInfo info, boolean unwind) {
       // exception object is not added to oop map by LinearScan
       // (LinearScan assumes that no oops are in fixed registers)
       // info.addRegisterOop(exceptionOop);
        masm.directCall(unwind ? CiRuntimeCall.UnwindException : CiRuntimeCall.HandleException, info);
        // enough room for two byte trap
        masm.nop();
    }

    @Override
    protected void emitShiftOp(LIROpcode code, LIROperand left, LIROperand count, LIROperand dest, LIROperand tmp) {
        // optimized version for linear scan:
        // * count must be already in ECX (guaranteed by LinearScan)
        // * left and dest must be equal
        // * tmp must be unused
        assert count.asRegister() == SHIFTCount : "count must be in ECX";
        assert left == dest : "left and dest must be equal";
        assert isIllegal(tmp) : "wasting a register if tmp is allocated";
        assert left.isRegister();

        if (left.kind.isInt()) {
            CiRegister value = left.asRegister();
            assert value != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl  : masm.shll(value); break;
                case Shr  : masm.sarl(value); break;
                case Ushr : masm.shrl(value); break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            CiRegister lreg = left.asRegister();
            assert lreg != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl  : masm.shlptr(lreg); break;
                case Shr  : masm.sarptr(lreg); break;
                case Ushr : masm.shrptr(lreg); break;
                default   : throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void emitShiftOp(LIROpcode code, LIROperand left, int count, LIROperand dest) {
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
                case Shl  : masm.shlptr(value, count); break;
                case Shr  : masm.sarptr(value, count); break;
                case Ushr : masm.shrptr(value, count); break;
                default   : throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void emitAlignment() {
        masm.align(wordSize);
    }

    @Override
    protected void emitNegate(LIROp1 op) {
        LIROperand left = op.operand();
        LIROperand dest = op.result();
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
            masm.movptr(dreg, lreg);
            masm.negptr(dreg);
        }
    }

    @Override
    protected void emitLeal(LIRAddress addr, LIRLocation dest) {
        CiRegister reg = dest.asPointerRegister(compilation.target.arch);
        masm.lea(reg, asAddress(addr));
    }

    @Override
    protected void emitRuntimeCall(CiRuntimeCall dest, LIRDebugInfo info) {
        masm.directCall(dest, info);
    }

    @Override
    protected void emitVolatileMove(LIROperand src, LIROperand dest, CiKind kind, LIRDebugInfo info) {
        assert kind == CiKind.Long : "only for volatile long fields";

        if (info != null) {
            asm.recordImplicitException(codePos(), info);
        }

        if (src.kind.isDouble()) {
            if (dest.isRegister()) {
                masm.movdq(dest.asRegister(), asXmmDoubleReg(src));
            } else if (dest.isStack()) {
                masm.movdbl(frameMap.toStackAddress(dest, 0), asXmmDoubleReg(src));
            } else if (isAddress(dest)) {
                masm.movdbl(asAddress((LIRAddress) dest), asXmmDoubleReg(src));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.kind.isDouble()) {
            if (src.isStack()) {
                masm.movdbl(asXmmDoubleReg(dest), frameMap.toStackAddress(src, 0));
            } else if (isAddress(src)) {
                masm.movdbl(asXmmDoubleReg(dest), asAddress((LIRAddress) src));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private static CiRegister asXmmDoubleReg(LIROperand dest) {
        assert dest.kind.isDouble() : "must be double XMM register";
        CiRegister result = dest.asRegister();
        assert result.isXmm() : "must be XMM register";
        return result;
    }

    @Override
    protected void emitMembar() {
        // QQQ sparc TSO uses this,
        masm.membar(X86Assembler.MembarMaskBits.StoreLoad.mask());
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
    protected void emitLIROp2(LIROp2 op) {
        switch (op.code) {
            case Cmp:
                if (op.info != null) {
                    assert isAddress(op.opr1()) || isAddress(op.opr2()) : "shouldn't be codeemitinfo for non-Pointer operands";
                    //NullPointerExceptionStub stub = new NullPointerExceptionStub(pcOffset, cinfo);
                    //emitCodeStub(stub);
                    asm.recordImplicitException(codePos(), op.info);
                }
                emitCompare(op.condition(), op.opr1(), op.opr2(), op);
                break;

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                emitCompareFloatInt(op.code, op.opr1(), op.opr2(), op.result(), op);
                break;

            case Cmove:
                emitConditionalMove(op.condition(), op.opr1(), op.opr2(), op.result());
                break;

            case Shl:
            case Shr:
            case Ushr:
                if (isConstant(op.opr2())) {
                    emitShiftOp(op.code, op.opr1(), ((LIRConstant) op.opr2()).asInt(), op.result());
                } else {
                    emitShiftOp(op.code, op.opr1(), op.opr2(), op.result(), op.tmp());
                }
                break;

            case Add:
            case Sub:
            case Mul:
            case Div:
            case Rem:
                emitArithOp(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                emitIntrinsicOp(op.code, op.opr1(), op.opr2(), op.result(), op);
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                emitLogicOp(op.code, op.opr1(), op.opr2(), op.result());
                break;

            case Throw:
            case Unwind:
                emitThrow(op.opr1(), op.opr2(), op.info, op.code == LIROpcode.Unwind);
                break;

            default:
                Util.unimplemented();
                break;
        }
    }

    @Override
    protected void emitPrologue() {
        compilation.runtime.codePrologue(compilation.method, asm.codeBuffer);
    }

    @Override
    protected void emitCode(LocalStub s) {
        s.accept(this);
    }

    public static Object asRegisterOrConstant(LIROperand operand) {
        if (operand.isVariableOrRegister()) {
            return operand.asRegister();
        } else if (isConstant(operand)) {
            return ((LIRConstant) operand).value;
        } else {
            throw Util.shouldNotReachHere();
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

    public void emitXirInstructions(LIRXirInstruction xir, XirInstruction[] instructions, Label[] labels, LIROperand[] operands) {
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
                    emitShiftOp(LIROpcode.Shl, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], IllegalLocation);
                    break;

                case Shr:
                    emitShiftOp(LIROpcode.Shr, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index], IllegalLocation);
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
                    LIROperand result = operands[inst.result.index];
                    LIROperand source = operands[inst.x().index];
                    moveOp(source, result, result.kind, null, false);
                    break;
                }

                case PointerLoad: {
                    if ((Boolean) inst.extra && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIROperand result = operands[inst.result.index];
                    LIROperand pointer = operands[inst.x().index];
                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();
                    moveOp(new LIRAddress((LIRLocation) pointer, 0, inst.kind), result, inst.kind, null, false);
                    break;
                }

                case PointerStore: {
                    if ((Boolean) inst.extra && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIROperand value = operands[inst.y().index];
                    LIROperand pointer = operands[inst.x().index];
                    assert pointer.isVariableOrRegister();
                    moveOp(value, new LIRAddress((LIRLocation) pointer, 0, inst.kind), inst.kind, null, false);
                    break;
                }

                case PointerLoadDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIRAddress.Scale scale = (addressInformation.scaling == null) ? Scale.Times1 : Scale.fromLog2(((LIRConstant) operands[addressInformation.scaling.getIndex()]).asInt());
                    int displacement = (addressInformation.offset == null) ? 0 : ((LIRConstant) operands[addressInformation.offset.getIndex()]).asInt();

                    LIROperand result = operands[inst.result.index];
                    LIROperand pointer = operands[inst.x().index];
                    LIROperand index = operands[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();

                    LIROperand src = null;
                    if (isConstant(index) && index.kind == CiKind.Int) {
                        LIRConstant constantDisplacement = (LIRConstant) index;
                        src = new LIRAddress((LIRLocation) pointer, IllegalLocation, scale, constantDisplacement.asInt() << scale.toInt() + displacement, inst.kind);
                    } else {
                        src = new LIRAddress((LIRLocation) pointer, (LIRLocation) index, scale, displacement, inst.kind);
                    }

                    moveOp(src, result, inst.kind, null, false);
                    break;
                }

                case PointerStoreDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIRAddress.Scale scale = (addressInformation.scaling == null) ? Scale.Times1 : Scale.fromLog2(((LIRConstant) operands[addressInformation.scaling.getIndex()]).asInt());
                    int displacement = (addressInformation.offset == null) ? 0 : ((LIRConstant) operands[addressInformation.offset.getIndex()]).asInt();

                    LIROperand value = operands[inst.z().index];
                    LIROperand pointer = operands[inst.x().index];
                    LIROperand index = operands[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();

                    LIROperand dst;
                    if (isConstant(index) && index.kind == CiKind.Int) {
                        LIRConstant constantDisplacement = (LIRConstant) index;
                        dst = new LIRAddress((LIRLocation) pointer, IllegalLocation, scale, constantDisplacement.asInt() << scale.toInt() + displacement, inst.kind);
                    } else {
                        dst = new LIRAddress((LIRLocation) pointer, (LIRLocation) index, scale, displacement, inst.kind);
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

                    CallingConvention cc = frameMap.runtimeCallingConvention(signature);
                    for (int i = 0; i < inst.arguments.length; i++) {
                        LIROperand argumentLocation = cc.operands[i];
                        LIROperand argumentSourceLocation = operands[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            moveOp(argumentSourceLocation, argumentLocation, argumentLocation.kind, null, false);
                        }
                    }

                    RiMethod method = (RiMethod) inst.extra;
                    masm.directCall(method, info);

                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        CiRegister[] returnRegisters = compilation.target.registerConfig.getReturnRegisters(inst.result.kind);
                        assert returnRegisters.length == 1;
                        LIROperand resultLocation = forRegister(inst.result.kind, returnRegisters[0]);
                        moveOp(resultLocation, operands[inst.result.index], inst.result.kind, null, false);
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

                case Bind:
                    XirLabel l = (XirLabel) inst.extra;
                    Label label = labels[l.index];
                    asm.bind(label);
                    break;
            }
        }
    }

    private LIROperand assureInRegister(LIROperand pointer) {
        if (isConstant(pointer)) {
            LIROperand newPointerOperand = forScratch(pointer.kind, compilation.target);
            moveOp(pointer, newPointerOperand, pointer.kind, null, false);
            return newPointerOperand;
        }

        assert pointer.isVariableOrRegister();
        return pointer;
    }

    private void emitXirCompare(XirInstruction inst, Condition condition, ConditionFlag cflag, LIROperand[] ops, Label label) {
        LIROperand x = ops[inst.x().index];
        LIROperand y = ops[inst.y().index];
        emitCompare(condition, x, y, null);
        masm.jcc(cflag, label);
    }

    public void visitThrowStub(ThrowStub stub) {
        masm.bind(stub.entry);
        LIROperand[] operands = stub.operands;
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
