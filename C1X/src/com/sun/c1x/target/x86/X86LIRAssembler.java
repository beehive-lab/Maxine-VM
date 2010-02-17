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
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.LIRAddress.*;
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

    @Override
    protected void const2reg(LIROperand src, LIROperand dest, LIRDebugInfo info) {
        assert isConstant(src) : "should not call otherwise";
        assert dest.isVariableOrRegister() : "should not call otherwise";
        LIRConstant c = (LIRConstant) src;

        switch (c.kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Jsr:
            case Int: {
                masm.movl(dest.asRegister(), c.value.asInt());
                break;
            }

            case Word:
            case Long: {
                if (is64) {
                    masm.movptr(dest.asRegisterLow(), c.asLong());
                } else {
                    masm.movptr(dest.asRegisterLow(), c.asIntLo());
                    masm.movptr(dest.asRegisterHigh(), c.asIntHi());
                }
                break;
            }

            case Object: {
                masm.movoop(dest.asRegister(), CiConstant.forObject(c.asObject()));
                break;
            }

            case Float: {
                if (dest.isSingleXmm()) {
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
                if (dest.isDoubleXmm()) {
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
        assert isConstant(src) : "should not call otherwise";
        assert dest.isStack() : "should not call otherwise";
        LIRConstant c = (LIRConstant) src;

        switch (c.kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int: // fall through
            case Float:
                masm.movl(frameMap.toAddress(dest, 0), c.asIntBits());
                break;

            case Object:
                masm.movoop(frameMap.toAddress(dest, 0), CiConstant.forObject(c.asObject()));
                break;

            case Long: // fall through
            case Double:
                if (is64) {
                    masm.movptr(frameMap.toAddress(dest, 0), c.asLongBits());
                } else {
                    masm.movptr(frameMap.toAddress(dest, compilation.target.arch.lowWordOffset), c.asIntLoBits());
                    masm.movptr(frameMap.toAddress(dest, compilation.target.arch.highWordOffset), c.asIntHiBits());
                }
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2mem(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info) {
        assert isConstant(src) : "should not call otherwise";
        assert isAddress(dest) : "should not call otherwise";
        LIRConstant c = (LIRConstant) src;
        LIRAddress addr = (LIRAddress) dest;

        int nullCheckHere = codePos();
        switch (type) {
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
                if (is64) {
                    if (isLiteralAddress(addr)) {
                        //masm().movptr(asAddress(addr, X86FrameMap.r15thread), c.asLongBits());
                        throw Util.shouldNotReachHere();
                    } else {
                        masm.movptr(rscratch1, c.asLongBits());
                        nullCheckHere = codePos();
                        masm.movptr(asAddressLo(addr), rscratch1);
                    }
                } else {
                    // Always reachable in 32bit so this doesn't produce useless move literal
                    masm.movptr(asAddressHi(addr), c.asIntHiBits());
                    masm.movptr(asAddressLo(addr), c.asIntLoBits());
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
        assert src.isVariableOrRegister() : "should not call otherwise";
        assert dest.isVariableOrRegister() : "should not call otherwise";

        // move between cpu-registers
        if (dest.isSingleCpu()) {
            if (is64) {
                if (src.kind == CiKind.Long) {
                    // Can do LONG . OBJECT
                    moveRegs(src.asRegisterLow(), dest.asRegister());
                    return;
                }
            }
            assert src.isSingleCpu() : "must match";
            if (src.kind == CiKind.Object) {
                masm.verifyOop(src.asRegister());
            }
            moveRegs(src.asRegister(), dest.asRegister());

        } else if (dest.isDoubleCpu()) {
            if (is64) {
                if (src.kind == CiKind.Object) {
                    // Surprising to me but we can see move of a long to tObject
                    masm.verifyOop(src.asRegister());
                    moveRegs(src.asRegister(), dest.asRegisterLow());
                    return;
                }
            }
            assert src.isDoubleCpu() : "must match";
            CiRegister fLo = src.asRegisterLow();
            CiRegister fHi = src.asRegisterHigh();
            CiRegister tLo = dest.asRegisterLow();
            CiRegister tHi = dest.asRegisterHigh();
            if (is64) {
                assert fHi == fLo : "must be same";
                assert tHi == tLo : "must be same";
                moveRegs(fLo, tLo);
            } else {
                assert fLo != fHi && tLo != tHi : "invalid register allocation";

                if (fLo == tHi && fHi == tLo) {
                    swapReg(fLo, fHi);
                } else if (fHi == tLo) {
                    moveRegs(fHi, tHi);
                    moveRegs(fLo, tLo);
                } else {
                    moveRegs(fLo, tLo);
                    moveRegs(fHi, tHi);
                }
            }
            // move between xmm-registers
        } else if (dest.isSingleXmm()) {
            masm.movflt(asXmmFloatReg(dest), asXmmFloatReg(src));
        } else if (dest.isDoubleXmm()) {
            masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(src));
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2stack(LIROperand src, LIROperand dest, CiKind type) {
        assert src.isVariableOrRegister() : "should not call otherwise";
        assert dest.isStack() : "should not call otherwise";

        if (src.isSingleCpu()) {
            Address dst = frameMap.toAddress(dest, 0);
            if (type == CiKind.Object || type == CiKind.Word) {
                if (type == CiKind.Object) {
                    masm.verifyOop(src.asRegister());
                }
                masm.movptr(dst, src.asRegister());
            } else {
                masm.movl(dst, src.asRegister());
            }

        } else if (src.isDoubleCpu()) {
            if (is64) {
                Address dstLO = frameMap.toAddress(dest, 0);
                masm.movptr(dstLO, src.asRegisterLow());
            } else {
                Address dstLO = frameMap.toAddress(dest, compilation.target.arch.lowWordOffset);
                masm.movptr(dstLO, src.asRegisterLow());
                Address dstHI = frameMap.toAddress(dest, compilation.target.arch.highWordOffset);
                masm.movptr(dstHI, src.asRegisterHigh());
            }

        } else if (src.isSingleXmm()) {
            Address dstAddr = frameMap.toAddress(dest, 0);
            masm.movflt(dstAddr, asXmmFloatReg(src));

        } else if (src.isDoubleXmm()) {
            Address dstAddr = frameMap.toAddress(dest, 0);
            masm.movdbl(dstAddr, asXmmDoubleReg(src));

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2mem(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info, boolean unaligned) {
        LIRAddress toAddr = (LIRAddress) dest;

        if (type == CiKind.Object) {
            masm.verifyOop(src.asRegister());
        }
        if (info != null) {
            //NullPointerExceptionStub stub = new NullPointerExceptionStub(pcOffset, cinfo);
            //emitCodeStub(stub);
            asm.recordImplicitException(codePos(), info);
        }

        switch (type) {
            case Float: {
                if (src.isSingleXmm()) {
                    masm.movflt(asAddress(toAddr), asXmmFloatReg(src));
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            case Double: {
                if (src.isDoubleXmm()) {
                    masm.movdbl(asAddress(toAddr), asXmmDoubleReg(src));
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
            case Word:
                if (is64) {
                    masm.movptr(asAddress(toAddr), src.asRegister());
                } else {
                    masm.movl(asAddress(toAddr), src.asRegister());
                }
                break;
            case Int:
                masm.movl(asAddress(toAddr), src.asRegister());
                break;

            case Long: {
                CiRegister fromLo = src.asRegisterLow();
                CiRegister fromHi = src.asRegisterHigh();
                if (is64) {
                    masm.movptr(asAddressLo(toAddr), fromLo);
                } else {
                    CiRegister base = toAddr.base.asRegister();
                    CiRegister index = CiRegister.None;
                    if (toAddr.index.isVariableOrRegister()) {
                        index = toAddr.index.asRegister();
                    }
                    if (base == fromLo || index == fromLo) {
                        assert base != fromHi : "can't be";
                        assert index == CiRegister.None || (index != base && index != fromHi) : "can't handle this";
                        masm.movl(asAddressHi(toAddr), fromHi);
                        masm.movl(asAddressLo(toAddr), fromLo);
                    } else {
                        assert index == CiRegister.None || (index != base && index != fromLo) : "can't handle this";
                        masm.movl(asAddressLo(toAddr), fromLo);
                        masm.movl(asAddressHi(toAddr), fromHi);
                    }
                }
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
        CiRegister result = src.asRegister();
        assert src.isSingleXmm() : "must be single xmm";
        assert result.isXmm() : "must be xmm";
        return result;
    }

    @Override
    protected void stack2reg(LIROperand src, LIROperand dest, CiKind type) {
        assert src.isStack() : "should not call otherwise";
        assert dest.isVariableOrRegister() : "should not call otherwise";

        if (dest.isSingleCpu()) {
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.movptr(dest.asRegister(), frameMap.toAddress(src, 0));
                if (type == CiKind.Object) {
                    masm.verifyOop(dest.asRegister());
                }
            } else {
                masm.movl(dest.asRegister(), frameMap.toAddress(src, 0));
            }

        } else if (dest.isDoubleCpu()) {
            if (is64) {
                Address srcAddrLO = frameMap.toAddress(src, 0);
                masm.movptr(dest.asRegisterLow(), srcAddrLO);
            } else {
                Address srcAddrLO = frameMap.toAddress(src, compilation.target.arch.lowWordOffset);
                Address srcAddrHI = frameMap.toAddress(src, compilation.target.arch.highWordOffset);
                masm.movptr(dest.asRegisterLow(), srcAddrLO);
                masm.movptr(dest.asRegisterHigh(), srcAddrHI);
            }

        } else if (dest.isSingleXmm()) {
            Address srcAddr = frameMap.toAddress(src, 0);
            masm.movflt(asXmmFloatReg(dest), srcAddr);

        } else if (dest.isDoubleXmm()) {
            Address srcAddr = frameMap.toAddress(src, 0);
            masm.movdbl(asXmmDoubleReg(dest), srcAddr);

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void mem2mem(LIROperand src, LIROperand dest, CiKind type) {
        if (dest.kind.isSingleWord()) {
            assert src.kind.isSingleWord();
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.pushptr(asAddress((LIRAddress) src));
                masm.popptr(asAddress((LIRAddress) dest));
            } else {
                masm.pushl(asAddress((LIRAddress) src));
                masm.popl(asAddress((LIRAddress) dest));
            }
        } else {
            assert !src.kind.isSingleWord();
            assert is64;
            masm.pushptr(asAddress((LIRAddress) src));
            masm.popptr(asAddress((LIRAddress) dest));
        }
    }

    @Override
    protected void mem2stack(LIROperand src, LIROperand dest, CiKind type) {
        if (dest.isSingleStack()) {
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.pushptr(asAddress((LIRAddress) src));
                masm.popptr(frameMap.toAddress(dest, 0));
            } else {
                masm.pushl(asAddress((LIRAddress) src));
                masm.popl(frameMap.toAddress(dest, 0));
            }
        } else if (dest.isDoubleStack()) {
            assert is64;
            masm.pushptr(asAddress((LIRAddress) src));
            masm.popptr(frameMap.toAddress(dest, 0));
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void stack2stack(LIROperand src, LIROperand dest, CiKind type) {
        if (src.isSingleStack()) {
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.pushptr(frameMap.toAddress(src, 0));
                masm.popptr(frameMap.toAddress(dest, 0));
            } else {
                masm.pushl(frameMap.toAddress(src, 0));
                masm.popl(frameMap.toAddress(dest, 0));
            }

        } else if (src.isDoubleStack()) {
            if (is64) {
                masm.pushptr(frameMap.toAddress(src, 0));
                masm.popptr(frameMap.toAddress(dest, 0));
            } else {
                masm.pushl(frameMap.toAddress(src, 0));
                // push and pop the part at src + wordSize, adding wordSize for the previous push
                masm.pushl(frameMap.toAddress(src, 2 * wordSize));
                masm.popl(frameMap.toAddress(dest, 2 * wordSize));
                masm.popl(frameMap.toAddress(dest, 0));
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void mem2reg(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info, boolean unaligned) {
        assert isAddress(src) : "should not call otherwise";
        assert dest.isVariableOrRegister() : "should not call otherwise";

        LIRAddress addr = (LIRAddress) src;
        Address fromAddr = asAddress(addr);

        switch (type) {
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

        switch (type) {
            case Float: {
                if (dest.isSingleXmm()) {
                    masm.movflt(asXmmFloatReg(dest), fromAddr);
                } else {
                    Util.shouldNotReachHere();
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    masm.movdbl(asXmmDoubleReg(dest), fromAddr);
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
                if (is64) {
                    masm.movptr(dest.asRegister(), fromAddr);
                } else {
                    masm.movl2ptr(dest.asRegister(), fromAddr);
                }
                break;
            case Int:
                // %%% could this be a movl? this is safer but longer instruction
                masm.movl2ptr(dest.asRegister(), fromAddr);
                break;

            case Word:
            case Long: {
                CiRegister toLo = dest.asRegisterLow();
                CiRegister toHi = dest.asRegisterHigh();

                if (is64) {
                    masm.movptr(toLo, asAddressLo(addr));
                } else {
                    CiRegister base = addr.base.asRegister();
                    CiRegister index = CiRegister.None;
                    if (addr.index.isVariableOrRegister()) {
                        index = addr.index.asRegister();
                    }
                    if ((base == toLo && index == toHi) || (base == toHi && index == toLo)) {
                        // addresses with 2 registers are only formed as a result of
                        // array access so this code will never have to deal with
                        // patches or null checks.
                        assert info == null : "must be";
                        masm.lea(toHi, asAddress(addr));
                        masm.movl(toLo, new Address(toHi, 0));
                        masm.movl(toHi, new Address(toHi, wordSize));
                    } else if (base == toLo || index == toLo) {
                        assert base != toHi : "can't be";
                        assert index == CiRegister.None || (index != base && index != toHi) : "can't handle this";
                        masm.movl(toHi, asAddressHi(addr));
                        masm.movl(toLo, asAddressLo(addr));
                    } else {
                        assert index == CiRegister.None || (index != base && index != toLo) : "can't handle this";
                        masm.movl(toLo, asAddressLo(addr));
                        masm.movl(toHi, asAddressHi(addr));
                    }
                }
                break;
            }

            case Boolean: // fall through
            case Byte: {
                CiRegister destReg = dest.asRegister();
                assert compilation.target.isP6() || destReg.isByte() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm.movsbl(destReg, fromAddr);
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
                    masm.movzwl(destReg, fromAddr);
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

        if (type == CiKind.Object) {
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

        if (op.cond() == LIRCondition.Always) {
            if (op.info != null) {
                asm.recordImplicitException(codePos(), op.info);
            }
            masm.jmp(op.label());
        } else {
            Condition acond = X86Assembler.Condition.zero;
            if (op.code == LIROpcode.CondFloatBranch) {
                assert op.ublock() != null : "must have unordered successor";
                masm.jcc(X86Assembler.Condition.parity, op.ublock().label());
                switch (op.cond()) {
                    case Equal:
                        acond = X86Assembler.Condition.equal;
                        break;
                    case NotEqual:
                        acond = X86Assembler.Condition.notEqual;
                        break;
                    case Less:
                        acond = X86Assembler.Condition.below;
                        break;
                    case LessEqual:
                        acond = X86Assembler.Condition.belowEqual;
                        break;
                    case GreaterEqual:
                        acond = X86Assembler.Condition.aboveEqual;
                        break;
                    case Greater:
                        acond = X86Assembler.Condition.above;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                switch (op.cond()) {
                    case Equal:
                        acond = X86Assembler.Condition.equal;
                        break;
                    case NotEqual:
                        acond = X86Assembler.Condition.notEqual;
                        break;
                    case Less:
                        acond = X86Assembler.Condition.less;
                        break;
                    case LessEqual:
                        acond = X86Assembler.Condition.lessEqual;
                        break;
                    case GreaterEqual:
                        acond = X86Assembler.Condition.greaterEqual;
                        break;
                    case Greater:
                        acond = X86Assembler.Condition.greater;
                        break;
                    case BelowEqual:
                        acond = X86Assembler.Condition.belowEqual;
                        break;
                    case AboveEqual:
                        acond = X86Assembler.Condition.aboveEqual;
                        break;
                    default:
                        throw Util.shouldNotReachHere();
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
                if (is64) {
                    masm.movl2ptr(dest.asRegisterLow(), src.asRegister());
                } else {
                    moveRegs(src.asRegister(), dest.asRegisterLow());
                    moveRegs(src.asRegister(), dest.asRegisterHigh());
                    masm.sarl(dest.asRegisterHigh(), 31);
                }
                break;

            case Bytecodes.L2I:
                moveRegs(src.asRegisterLow(), dest.asRegister());
                break;

            case Bytecodes.I2B:
                moveRegs(src.asRegister(), dest.asRegister());
                masm.signExtendByte(dest.asRegister());
                break;

            case Bytecodes.I2C:
                moveRegs(src.asRegister(), dest.asRegister());
                masm.andl(dest.asRegister(), 0xFFFF);
                break;

            case Bytecodes.I2S:
                moveRegs(src.asRegister(), dest.asRegister());
                masm.signExtendShort(dest.asRegister());
                break;

            case Bytecodes.F2D:
            case Bytecodes.D2F:
                if (dest.isSingleXmm()) {
                    masm.cvtsd2ss(asXmmFloatReg(dest), asXmmDoubleReg(src));
                } else if (dest.isDoubleXmm()) {
                    masm.cvtss2sd(asXmmDoubleReg(dest), asXmmFloatReg(src));
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;

            case Bytecodes.I2F:
            case Bytecodes.I2D:
                if (dest.isSingleXmm()) {
                    masm.cvtsi2ssl(asXmmFloatReg(dest), src.asRegister());
                } else if (dest.isDoubleXmm()) {
                    masm.cvtsi2sdl(asXmmDoubleReg(dest), src.asRegister());
                } else {
                    throw Util.unimplemented("no fpu stack");
                }
                break;

            case Bytecodes.F2I: {
                assert src.isSingleXmm() && dest.isVariableOrRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttss2sil(dest.asRegister(), srcRegister);
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(Condition.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case Bytecodes.D2I: {
                assert src.isDoubleXmm() && dest.isVariableOrRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2sil(dest.asRegister(), asXmmDoubleReg(src));
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(Condition.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case Bytecodes.L2F:
                masm.cvtsi2ssq(asXmmFloatReg(dest), src.asRegister());
                break;

            case Bytecodes.L2D:
                masm.cvtsi2sdq(asXmmDoubleReg(dest), src.asRegister());
                break;

            case Bytecodes.F2L: {
                assert src.isSingleXmm() && dest.isDoubleCpu() : "must both be XMM register (no fpu stack)";
                masm.cvttss2siq(dest.asRegister(), asXmmFloatReg(src));
                masm.mov64(rscratch1, Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(Condition.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            case Bytecodes.D2L: {
                assert src.isDoubleXmm() && dest.isDoubleCpu() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2siq(dest.asRegister(), asXmmDoubleReg(src));
                masm.mov64(rscratch1, Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(Condition.notEqual, endLabel);
                masm.callGlobalStub(op.globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitTypeCheck(LIRTypeCheck op) {
        LIROpcode code = op.code;
        if (code == LIROpcode.StoreCheck) {
            CiRegister value = op.object().asRegister();
            CiRegister array = op.array().asRegister();
            CiRegister kRInfo = op.tmp1().asRegister();
            CiRegister klassRInfo = op.tmp2().asRegister();
            CiRegister rtmp1 = op.tmp3().asRegister();

            Label done = new Label();
            masm.cmpptr(value, (int) NULLWORD);
            masm.jcc(X86Assembler.Condition.equal, done);
            // TODO: possible NPE on the next instruction if there is no range check!
            // asm.recordExceptionHandlers(codePos(), op.info);

            masm.movptr(kRInfo, new Address(array, compilation.runtime.hubOffset()));
            masm.movptr(klassRInfo, new Address(value, compilation.runtime.hubOffset()));

            // get instance klass
            masm.movptr(kRInfo, new Address(kRInfo, compilation.runtime.elementHubOffset()));
            masm.callGlobalStub(op.globalStub, op.info, rtmp1, kRInfo, klassRInfo);
            masm.bind(done);
        } else if (op.code == LIROpcode.CheckCast) {
            // we always need a stub for the failure case.
            LocalStub stub = op.stub;
            CiRegister obj = op.object().asRegister();
            CiRegister expectedHub = op.tmp1().asRegister();
            CiRegister dst = op.result().asRegister();
            RiType k = op.klass();

            Label done = new Label();
            if (obj == expectedHub) {
                expectedHub = dst;
            }

            assert obj != expectedHub : "must be different";
            masm.cmpptr(obj, (int) NULLWORD);
            masm.jcc(Condition.equal, done);
            masm.verifyOop(obj);

            if (op.isFastCheck()) {
                // get object class
                // not a safepoint as obj null check happens earlier
                if (k.isLoaded()) {
                    if (is64) {
                        masm.cmpptr(expectedHub, new Address(obj, compilation.runtime.hubOffset()));
                    } else {
                        masm.cmpoop(new Address(obj, compilation.runtime.hubOffset()), k);
                    }
                } else {
                    masm.cmpptr(expectedHub, new Address(obj, compilation.runtime.hubOffset()));

                }
                masm.jcc(X86Assembler.Condition.notEqual, stub.entry);
                masm.bind(done);
            } else {
                // perform a runtime call to cast the object
                masm.callGlobalStub(op.globalStub, op.info, dst, obj, expectedHub);
                masm.bind(done);
            }
            if (dst != obj) {
                masm.mov(dst, obj);
            }
        } else if (code == LIROpcode.InstanceOf) {
            CiRegister obj = op.object().asRegister();
            CiRegister kRInfo = op.tmp1().asRegister();
            CiRegister klassRInfo = op.tmp2().asRegister();
            CiRegister dst = op.result().asRegister();
            RiType k = op.klass();

            Label done = new Label();
            Label zero = new Label();
            Label one = new Label();

            if (klassRInfo == kRInfo || klassRInfo == obj) {
                klassRInfo = dst;
            }
            assert obj != kRInfo : "must be different";
            assert obj != klassRInfo : "must be different";

            assert CiRegister.assertDifferentRegisters(obj, kRInfo, klassRInfo);

            masm.verifyOop(obj);
            if (op.isFastCheck()) {
                masm.cmpptr(obj, (int) NULLWORD);
                masm.jcc(X86Assembler.Condition.equal, zero);
                // get object class
                // not a safepoint as obj null check happens earlier
                if (!is64 && k.isLoaded()) {
                    masm.cmpoop(new Address(obj, compilation.runtime.hubOffset()), k);
                    kRInfo = CiRegister.None;
                } else {
                    masm.cmpptr(kRInfo, new Address(obj, compilation.runtime.hubOffset()));

                }
                masm.jcc(X86Assembler.Condition.equal, one);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                masm.cmpptr(obj, (int) NULLWORD);
                masm.jcc(X86Assembler.Condition.equal, zero);
                masm.movptr(klassRInfo, new Address(obj, compilation.runtime.hubOffset()));
                masm.callGlobalStub(op.globalStub, op.info, dst, kRInfo, klassRInfo);
                masm.jmp(done);
            }
            masm.bind(zero);
            masm.xorptr(dst, dst);
            masm.jmp(done);
            masm.bind(one);
            masm.movptr(dst, 1);
            masm.bind(done);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        if (!is64 && op.code == LIROpcode.CasLong && compilation.target.supportsCx8()) {
            assert op.cmpValue().asRegisterLow() == X86.rax : "wrong register";
            assert op.cmpValue().asRegisterHigh() == X86.rdx : "wrong register";
            assert op.newValue().asRegisterLow() == X86.rbx : "wrong register";
            assert op.newValue().asRegisterHigh() == X86.rcx : "wrong register";
            CiRegister addr = op.address().asRegister();
            if (compilation.runtime.isMP()) {
                masm.lock();
            }
            masm.cmpxchg8(new Address(addr, 0));

        } else if (op.code == LIROpcode.CasInt || op.code == LIROpcode.CasObj) {
            assert is64 || op.address().isSingleCpu() : "must be single";
            CiRegister addr = ((op.address().isSingleCpu() ? op.address().asRegister() : op.address().asRegisterLow()));
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
            if (op.code == LIROpcode.CasObj) {
                masm.cmpxchgptr(newval, new Address(addr, 0));
            } else if (op.code == LIROpcode.CasInt) {
                masm.cmpxchgl(newval, new Address(addr, 0));
            } else if (is64) {
                masm.cmpxchgq(newval, new Address(addr, 0));
            }
        } else if (is64 && op.code == LIROpcode.CasLong) {
            CiRegister addr = (op.address().isSingleCpu() ? op.address().asRegister() : op.address().asRegisterLow());
            CiRegister newval = op.newValue().asRegisterLow();
            CiRegister cmpval = op.cmpValue().asRegisterLow();
            assert cmpval == X86.rax : "wrong register";
            assert newval != null : "new val must be register";
            assert cmpval != newval : "cmp and new values must be in different registers";
            assert cmpval != addr : "cmp and addr must be in different registers";
            assert newval != addr : "new value and addr must be in different registers";
            if (compilation.runtime.isMP()) {
                masm.lock();
            }
            masm.cmpxchgq(newval, new Address(addr, 0));
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitConditionalMove(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROperand result) {
        Condition acond;
        Condition ncond;
        switch (condition) {
            case Equal:
                acond = X86Assembler.Condition.equal;
                ncond = X86Assembler.Condition.notEqual;
                break;
            case NotEqual:
                acond = X86Assembler.Condition.notEqual;
                ncond = X86Assembler.Condition.equal;
                break;
            case Less:
                acond = X86Assembler.Condition.less;
                ncond = X86Assembler.Condition.greaterEqual;
                break;
            case LessEqual:
                acond = X86Assembler.Condition.lessEqual;
                ncond = X86Assembler.Condition.greater;
                break;
            case GreaterEqual:
                acond = X86Assembler.Condition.greaterEqual;
                ncond = X86Assembler.Condition.less;
                break;
            case Greater:
                acond = X86Assembler.Condition.greater;
                ncond = X86Assembler.Condition.lessEqual;
                break;
            case BelowEqual:
                acond = X86Assembler.Condition.belowEqual;
                ncond = X86Assembler.Condition.above;
                break;
            case AboveEqual:
                acond = X86Assembler.Condition.aboveEqual;
                ncond = X86Assembler.Condition.below;
                break;
            default:
                throw Util.shouldNotReachHere();
        }

        LIROperand def = opr1; // assume left operand as default
        LIROperand other = opr2;

        if (opr2.isSingleCpu() && opr2.asRegister() == result.asRegister()) {
            // if the right operand is already in the result register, then use it as the default
            def = opr2;
            other = opr1;
            // and flip the condition
            Condition tcond = acond;
            acond = ncond;
            ncond = tcond;
        }

        if (def.isVariableOrRegister()) {
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
            if (other.isSingleCpu()) {
                assert other.asRegister() != result.asRegister() : "other already overwritten by previous move";
                masm.cmov(ncond, result.asRegister(), other.asRegister());
            } else if (other.isDoubleCpu()) {
                assert other.cpuRegNumberLow() != result.cpuRegNumberLow() && other.cpuRegNumberLow() != result.cpuRegNumberHigh() : "other already overwritten by previous move";
                assert other.cpuRegNumberHigh() != result.cpuRegNumberLow() && other.cpuRegNumberHigh() != result.cpuRegNumberHigh() : "other already overwritten by previous move";
                masm.cmovptr(ncond, result.asRegisterLow(), other.asRegisterLow());
                if (!is64) {
                    masm.cmovptr(ncond, result.asRegisterHigh(), other.asRegisterHigh());
                }
            } else if (other.isSingleStack()) {
                masm.cmovl(ncond, result.asRegister(), frameMap.toAddress(other, 0));
            } else if (other.isDoubleStack()) {
                masm.cmovptr(ncond, result.asRegisterLow(), frameMap.toAddress(other, compilation.target.arch.lowWordOffset));
                if (!is64) {
                    masm.cmovptr(ncond, result.asRegisterHigh(), frameMap.toAddress(other, compilation.target.arch.highWordOffset));
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

        if (left.isSingleCpu()) {
            assert left.equals(dest) : "left and dest must be equal";
            CiRegister lreg = left.asRegister();

            if (right.isSingleCpu()) {
                // cpu register - cpu register
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add:
                        masm.addl(lreg, rreg);
                        break;
                    case Sub:
                        masm.subl(lreg, rreg);
                        break;
                    case Mul:
                        masm.imull(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isStack()) {
                // cpu register - stack
                Address raddr = frameMap.toAddress(right, 0);
                switch (code) {
                    case Add:
                        masm.addl(lreg, raddr);
                        break;
                    case Sub:
                        masm.subl(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (isConstant(right)) {
                // cpu register - constant
                int c = ((LIRConstant) right).asInt();
                switch (code) {
                    case Add: {
                        masm.increment(lreg, c);
                        break;
                    }
                    case Sub: {
                        masm.decrement(lreg, c);
                        break;
                    }
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (left.isDoubleCpu()) {
            assert left.equals(dest) : "left and dest must be equal";
            CiRegister lregLo = left.asRegisterLow();
            CiRegister lregHi = left.asRegisterHigh();

            if (right.isDoubleCpu()) {
                // cpu register - cpu register
                CiRegister rregLo = right.asRegisterLow();
                CiRegister rregHi = right.asRegisterHigh();
                assert is64 || CiRegister.assertDifferentRegisters(lregLo, lregHi, rregLo, rregHi);
                assert !is64 || CiRegister.assertDifferentRegisters(lregLo, rregLo);
                switch (code) {
                    case Add:
                        masm.addptr(lregLo, rregLo);
                        if (!is64) {
                            masm.adcl(lregHi, rregHi);
                        }
                        break;
                    case Sub:
                        masm.subptr(lregLo, rregLo);
                        if (!is64) {
                            masm.sbbl(lregHi, rregHi);
                        }
                        break;
                    case Mul:
                        if (is64) {
                            masm.imulq(lregLo, rregLo);
                        } else {
                            assert lregLo == X86.rax && lregHi == X86.rdx : "must be";
                            masm.imull(lregHi, rregLo);
                            masm.imull(rregHi, lregLo);
                            masm.addl(rregHi, lregHi);
                            masm.mull(rregLo);
                            masm.addl(lregHi, rregHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (isConstant(right)) {
                // cpu register - constant
                if (is64) {
                    long c = ((LIRConstant) right).asLongBits();
                    masm.movptr(rscratch1, c);
                    switch (code) {
                        case Add:
                            masm.addptr(lregLo, rscratch1);
                            break;
                        case Sub:
                            masm.subptr(lregLo, rscratch1);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    int cLo = ((LIRConstant) right).asIntLo();
                    int cHi = ((LIRConstant) right).asIntHi();
                    switch (code) {
                        case Add:
                            masm.addptr(lregLo, cLo);
                            masm.adcl(lregHi, cHi);
                            break;
                        case Sub:
                            masm.subptr(lregLo, cLo);
                            masm.sbbl(lregHi, cHi);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }

            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (left.isSingleXmm()) {
            assert left.equals(dest) : "left and dest must be equal";
            CiRegister lreg = asXmmFloatReg(left);
            assert lreg.isXmm();

            if (right.isSingleXmm()) {
                CiRegister rreg = asXmmFloatReg(right);
                assert rreg.isXmm();
                switch (code) {
                    case Add:
                        masm.addss(lreg, rreg);
                        break;
                    case Sub:
                        masm.subss(lreg, rreg);
                        break;
                    case Mul:
                        masm.mulss(lreg, rreg);
                        break;
                    case Div:
                        masm.divss(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isSingleStack()) {
                    raddr = frameMap.toAddress(right, 0);
                } else if (isConstant(right)) {
                    raddr = masm.recordDataReferenceInCode(CiConstant.forFloat(((LIRConstant) right).asFloat()));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add:
                        masm.addss(lreg, raddr);
                        break;
                    case Sub:
                        masm.subss(lreg, raddr);
                        break;
                    case Mul:
                        masm.mulss(lreg, raddr);
                        break;
                    case Div:
                        masm.divss(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleXmm()) {
            assert left.equals(dest) : "left and dest must be equal";

            CiRegister lreg = asXmmDoubleReg(left);
            assert lreg.isXmm();
            if (right.isDoubleXmm()) {
                CiRegister rreg = asXmmDoubleReg(right);
                assert rreg.isXmm();
                switch (code) {
                    case Add:
                        masm.addsd(lreg, rreg);
                        break;
                    case Sub:
                        masm.subsd(lreg, rreg);
                        break;
                    case Mul:
                        masm.mulsd(lreg, rreg);
                        break;
                    case Div:
                        masm.divsd(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isDoubleStack()) {
                    raddr = frameMap.toAddress(right, 0);
                } else if (isConstant(right)) {
                    // hack for now
                    raddr = masm.recordDataReferenceInCode(CiConstant.forDouble(((LIRConstant) right).asDouble()));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add:
                        masm.addsd(lreg, raddr);
                        break;
                    case Sub:
                        masm.subsd(lreg, raddr);
                        break;
                    case Mul:
                        masm.mulsd(lreg, raddr);
                        break;
                    case Div:
                        masm.divsd(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isSingleStack() || isAddress(left)) {
            assert left.equals(dest) : "left and dest must be equal";

            Address laddr;
            if (left.isSingleStack()) {
                laddr = frameMap.toAddress(left, 0);
            } else if (isAddress(left)) {
                laddr = asAddress((LIRAddress) left);
            } else {
                throw Util.shouldNotReachHere();
            }

            if (right.isSingleCpu()) {
                CiRegister rreg = right.asRegister();
                switch (code) {
                    case Add:
                        masm.addl(laddr, rreg);
                        break;
                    case Sub:
                        masm.subl(laddr, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (isConstant(right)) {
                int c = ((LIRConstant) right).asInt();
                switch (code) {
                    case Add: {
                        masm.incrementl(laddr, c);
                        break;
                    }
                    case Sub: {
                        masm.decrementl(laddr, c);
                        break;
                    }
                    default:
                        throw Util.shouldNotReachHere();
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
        if (value.isDoubleXmm()) {
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

                // all other intrinsics are not available in the SSE instruction set, so FPU is used
                default:
                    throw Util.shouldNotReachHere();
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitLogicOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst) {
        if (left.isSingleCpu()) {
            CiRegister reg = left.asRegister();
            if (isConstant(right)) {
                int val = ((LIRConstant) right).asInt();
                switch (code) {
                    case LogicAnd:
                        masm.andl(reg, val);
                        break;
                    case LogicOr:
                        masm.orl(reg, val);
                        break;
                    case LogicXor:
                        masm.xorl(reg, val);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isStack()) {
                // added support for stack operands
                Address raddr = frameMap.toAddress(right, 0);
                switch (code) {
                    case LogicAnd:
                        masm.andl(reg, raddr);
                        break;
                    case LogicOr:
                        masm.orl(reg, raddr);
                        break;
                    case LogicXor:
                        masm.xorl(reg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                CiRegister rright = right.asRegister();
                switch (code) {
                    case LogicAnd:
                        masm.andptr(reg, rright);
                        break;
                    case LogicOr:
                        masm.orptr(reg, rright);
                        break;
                    case LogicXor:
                        masm.xorptr(reg, rright);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister());
        } else {
            CiRegister lLo = left.asRegisterLow();
            CiRegister lHi = left.asRegisterHigh();
            if (isConstant(right)) {
                if (is64) {
                    LIRConstant rightConstant = (LIRConstant) right;
                    masm.mov64(rscratch1, rightConstant.asLong());
                    switch (code) {
                        case LogicAnd:
                            masm.andq(lLo, rscratch1);
                            break;
                        case LogicOr:
                            masm.orq(lLo, rscratch1);
                            break;
                        case LogicXor:
                            masm.xorq(lLo, rscratch1);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    LIRConstant rightConstant = (LIRConstant) right;
                    int rLo = rightConstant.asIntLo();
                    int rHi = rightConstant.asIntHi();
                    switch (code) {
                        case LogicAnd:
                            masm.andl(lLo, rLo);
                            masm.andl(lHi, rHi);
                            break;
                        case LogicOr:
                            masm.orl(lLo, rLo);
                            masm.orl(lHi, rHi);
                            break;
                        case LogicXor:
                            masm.xorl(lLo, rLo);
                            masm.xorl(lHi, rHi);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }
            } else {
                CiRegister rLo = right.asRegisterLow();
                CiRegister rHi = right.asRegisterHigh();
                assert lLo != rHi : "overwriting registers";
                switch (code) {
                    case LogicAnd:
                        masm.andptr(lLo, rLo);
                        if (!is64) {
                            masm.andptr(lHi, rHi);
                        }
                        break;
                    case LogicOr:
                        masm.orptr(lLo, rLo);
                        if (!is64) {
                            masm.orptr(lHi, rHi);
                        }
                        break;
                    case LogicXor:
                        masm.xorptr(lLo, rLo);

                        if (!is64) {
                            masm.xorptr(lHi, rHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

            CiRegister dstLo = dst.asRegisterLow();
            CiRegister dstHi = dst.asRegisterHigh();

            if (is64) {
                moveRegs(lLo, dstLo);
            } else {
                if (dstLo == lHi) {
                    assert dstHi != lLo : "overwriting registers";
                    moveRegs(lHi, dstHi);
                    moveRegs(lLo, dstLo);
                } else {
                    assert dstLo != lHi : "overwriting registers";
                    moveRegs(lLo, dstLo);
                    moveRegs(lHi, dstHi);
                }
            }
        }
    }

    void arithmeticIdiv(LIROpcode code, LIROperand left, LIROperand right, LIROperand result, LIRDebugInfo info) {
        assert left.isSingleCpu() : "left must be register";
        assert right.isSingleCpu() || isConstant(right) : "right must be register or constant";
        assert result.isSingleCpu() : "result must be register";

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
                masm.jcc(X86Assembler.Condition.positive, done);
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
                masm.jcc(Condition.notEqual, normalCase);
                if (code == LIROpcode.Irem) {
                    // prepare X86Register.rdx for possible special case where remainder = 0
                    masm.xorl(X86.rdx, X86.rdx);
                }
                masm.cmpl(rreg, -1);
                masm.jcc(Condition.equal, continuation);

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
        assert left.isDoubleCpu() : "left must be register";
        assert right.isDoubleCpu() : "right must be register";
        assert result.isDoubleCpu() : "result must be register";

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
            masm.jcc(Condition.notEqual, normalCase);
            if (code == LIROpcode.Lrem) {
                // prepare X86Register.rdx for possible special case (where remainder = 0)
                masm.xorq(X86.rdx, X86.rdx);
            }
            masm.cmpl(rreg, -1);
            masm.jcc(Condition.equal, continuation);

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

    @Override
    protected void emitCompare(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROp2 op) {
        if (opr1.isSingleCpu()) {
            CiRegister reg1 = opr1.asRegister();
            if (opr2.isSingleCpu()) {
                // cpu register - cpu register
                if (opr1.kind == CiKind.Object || opr1.kind == CiKind.Word) {
                    masm.cmpptr(reg1, opr2.asRegister());
                } else {
                    assert opr2.kind != CiKind.Object && opr2.kind != CiKind.Word : "cmp int :  oop?";
                    masm.cmpl(reg1, opr2.asRegister());
                }
            } else if (opr2.isStack()) {
                // cpu register - stack
                if (opr1.kind == CiKind.Object || opr1.kind == CiKind.Word) {
                    masm.cmpptr(reg1, frameMap.toAddress(opr2, 0));
                } else {
                    masm.cmpl(reg1, frameMap.toAddress(opr2, 0));
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
                        if (is64) {
                            masm.movoop(rscratch1, CiConstant.forObject(o));
                            masm.cmpptr(reg1, rscratch1);
                        } else {
                            masm.cmpoop(reg1, c.asObject());
                        }
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

        } else if (opr1.isDoubleCpu()) {
            CiRegister xlo = opr1.asRegisterLow();
            CiRegister xhi = opr1.asRegisterHigh();
            if (opr2.isDoubleCpu()) {
                if (is64) {
                    masm.cmpptr(xlo, opr2.asRegisterLow());
                } else {
                    // cpu register - cpu register
                    CiRegister ylo = opr2.asRegisterLow();
                    CiRegister yhi = opr2.asRegisterHigh();
                    masm.subl(xlo, ylo);
                    masm.sbbl(xhi, yhi);
                    if (condition == LIRCondition.Equal || condition == LIRCondition.NotEqual) {
                        masm.orl(xhi, xlo);
                    }
                }
            } else if (isConstant(opr2)) {
                // cpu register - constant 0
                LIRConstant constantOpr2 = (LIRConstant) opr2;
                assert constantOpr2.asLong() == 0 : "only handles zero";
                if (is64) {
                    masm.cmpptr(xlo, (int) constantOpr2.asLong());
                } else {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "only handles equals case";
                    masm.orl(xhi, xlo);
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleXmm()) {
            CiRegister reg1 = asXmmFloatReg(opr1);
            assert reg1.isXmm();
            if (opr2.isSingleXmm()) {
                // xmm register - xmm register
                masm.ucomiss(reg1, asXmmFloatReg(opr2));
            } else if (opr2.isStack()) {
                // xmm register - stack
                masm.ucomiss(reg1, frameMap.toAddress(opr2, 0));
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

        } else if (opr1.isDoubleXmm()) {
            CiRegister reg1 = asXmmDoubleReg(opr1);
            assert reg1.isXmm();
            if (opr2.isDoubleXmm()) {
                // xmm register - xmm register
                masm.ucomisd(reg1, asXmmDoubleReg(opr2));
            } else if (opr2.isStack()) {
                // xmm register - stack
                masm.ucomisd(reg1, frameMap.toAddress(opr2, 0));
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
        } else if (isAddress(opr1) && isConstant(opr2)) {
            LIRConstant c = ((LIRConstant) opr2);

            if (is64) {
                if (c.kind == CiKind.Object) {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "need to reverse";
                    masm.movoop(rscratch1, CiConstant.forObject(c.asObject()));
                }
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
                if (is64) {
                    // %%% Make this explode if addr isn't reachable until we figure out a
                    // better strategy by giving X86.noreg as the temp for asAddress
                    masm.cmpptr(rscratch1, asAddress(addr));
                } else {
                    masm.cmpoop(asAddress(addr), c.asObject());
                }
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
            if (left.isSingleXmm()) {
                masm.cmpss2int(asXmmFloatReg(left), asXmmFloatReg(right), dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else if (left.isDoubleXmm()) {
                masm.cmpsd2int(asXmmDoubleReg(left), asXmmDoubleReg(right), dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else {
                throw Util.unimplemented("no fpu stack");
            }
        } else {
            assert code == LIROpcode.Cmpl2i;
            if (is64) {
                CiRegister dest = dst.asRegister();
                Label high = new Label();
                Label done = new Label();
                Label isEqual = new Label();
                masm.cmpptr(left.asRegisterLow(), right.asRegisterLow());
                masm.jcc(X86Assembler.Condition.equal, isEqual);
                masm.jcc(X86Assembler.Condition.greater, high);
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
            } else {
                masm.lcmp2int(left.asRegisterHigh(), left.asRegisterLow(), right.asRegisterHigh(), right.asRegisterLow());
                moveRegs(left.asRegisterHigh(), dst.asRegister());
            }
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
    protected void emitDirectCall(RiMethod method, LIRDebugInfo info) {
        assert method.isLoaded() : "method is not resolved";
        masm.directCall(method, info);
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

        if (left.isSingleCpu()) {
            CiRegister value = left.asRegister();
            assert value != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl:
                    masm.shll(value);
                    break;
                case Shr:
                    masm.sarl(value);
                    break;
                case Ushr:
                    masm.shrl(value);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (left.isDoubleCpu()) {
            CiRegister lo = left.asRegisterLow();
            CiRegister hi = left.asRegisterHigh();
            assert lo != SHIFTCount && hi != SHIFTCount : "left cannot be ECX";

            if (is64) {
                switch (code) {
                    case Shl:
                        masm.shlptr(lo);
                        break;
                    case Shr:
                        masm.sarptr(lo);
                        break;
                    case Ushr:
                        masm.shrptr(lo);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {

                switch (code) {
                    case Shl:
                        masm.lshl(hi, lo);
                        break;
                    case Shr:
                        masm.lshr(hi, lo, true);
                        break;
                    case Ushr:
                        masm.lshr(hi, lo, false);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitShiftOp(LIROpcode code, LIROperand left, int count, LIROperand dest) {
        if (dest.isSingleCpu()) {
            // first move left into dest so that left is not destroyed by the shift
            CiRegister value = dest.asRegister();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegister(), value);
            switch (code) {
                case Shl:
                    masm.shll(value, count);
                    break;
                case Shr:
                    masm.sarl(value, count);
                    break;
                case Ushr:
                    masm.shrl(value, count);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (dest.isDoubleCpu()) {

            if (!is64) {
                throw Util.shouldNotReachHere();
            }

            // first move left into dest so that left is not destroyed by the shift
            CiRegister value = dest.asRegisterLow();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegisterLow(), value);
            switch (code) {
                case Shl:
                    masm.shlptr(value, count);
                    break;
                case Shr:
                    masm.sarptr(value, count);
                    break;
                case Ushr:
                    masm.shrptr(value, count);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
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
        if (left.isSingleCpu()) {
            masm.negl(left.asRegister());
            moveRegs(left.asRegister(), dest.asRegister());

        } else if (left.isDoubleCpu()) {
            CiRegister lo = left.asRegisterLow();
            if (is64) {
                CiRegister dst = dest.asRegisterLow();
                masm.movptr(dst, lo);
                masm.negptr(dst);
            } else {
                CiRegister hi = left.asRegisterHigh();
                masm.lneg(hi, lo);
                if (dest.asRegisterLow() == hi) {
                    assert dest.asRegisterHigh() != lo : "destroying register";
                    moveRegs(hi, dest.asRegisterHigh());
                    moveRegs(lo, dest.asRegisterLow());
                } else {
                    moveRegs(lo, dest.asRegisterLow());
                    moveRegs(hi, dest.asRegisterHigh());
                }
            }

        } else if (dest.isSingleXmm()) {
            if (asXmmFloatReg(left) != asXmmFloatReg(dest)) {
                masm.movflt(asXmmFloatReg(dest), asXmmFloatReg(left));
            }
            masm.callGlobalStub(op.globalStub, null, asXmmFloatReg(dest), asXmmFloatReg(dest));

        } else if (dest.isDoubleXmm()) {
            if (asXmmDoubleReg(left) != asXmmDoubleReg(dest)) {
                masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(left));
            }

            masm.callGlobalStub(op.globalStub, null, asXmmDoubleReg(dest), asXmmDoubleReg(dest));
        } else {
            throw Util.shouldNotReachHere();
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
    protected void emitVolatileMove(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info) {
        assert type == CiKind.Long : "only for volatile long fields";

        if (info != null) {
            asm.recordImplicitException(codePos(), info);
        }

        if (src.isDoubleXmm()) {
            if (dest.isDoubleCpu()) {
                if (is64) {
                    masm.movdq(dest.asRegisterLow(), asXmmDoubleReg(src));
                } else {
                    masm.movdl(dest.asRegisterLow(), asXmmDoubleReg(src));
                    masm.psrlq(asXmmDoubleReg(src), 32);
                    masm.movdl(dest.asRegisterHigh(), asXmmDoubleReg(src));
                }
            } else if (dest.isDoubleStack()) {
                masm.movdbl(frameMap.toAddress(dest, 0), asXmmDoubleReg(src));
            } else if (isAddress(dest)) {
                masm.movdbl(asAddress((LIRAddress) dest), asXmmDoubleReg(src));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleXmm()) {
            if (src.isDoubleStack()) {
                masm.movdbl(asXmmDoubleReg(dest), frameMap.toAddress(src, 0));
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
        CiRegister result = dest.asRegister();
        assert dest.isDoubleXmm() : "must be double XMM register";
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

    public void emitXirInstructions(LIRXirInstruction xir, XirInstruction[] instructions, Label[] labels, LIROperand[] ops) {
        LIRDebugInfo info = xir == null ? null : xir.info;

        for (XirInstruction inst : instructions) {
            switch (inst.op) {
                case Add:
                    emitArithOp(LIROpcode.Add, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Sub:
                    emitArithOp(LIROpcode.Sub, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Div:
                    if (inst.kind == CiKind.Int) {
                        arithmeticIdiv(LIROpcode.Idiv, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    }
                    emitArithOp(LIROpcode.Div, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Mul:
                    emitArithOp(LIROpcode.Mul, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Mod:
                    if (inst.kind == CiKind.Int) {
                        arithmeticIdiv(LIROpcode.Irem, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    } else {
                        emitArithOp(LIROpcode.Rem, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    }
                    break;

                case Shl:
                    emitShiftOp(LIROpcode.Shl, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], IllegalLocation);
                    break;

                case Shr:
                    emitShiftOp(LIROpcode.Shr, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], IllegalLocation);
                    break;

                case And:
                    emitLogicOp(LIROpcode.LogicAnd, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index]);
                    break;

                case Or:
                    emitLogicOp(LIROpcode.LogicOr, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index]);
                    break;

                case Xor:
                    emitLogicOp(LIROpcode.LogicXor, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index]);
                    break;

                case Mov: {
                    LIROperand result = ops[inst.result.index];
                    LIROperand source = ops[inst.x().index];
                    moveOp(source, result, result.kind, null, false);
                    break;
                }

                case PointerLoad: {
                    if ((Boolean) inst.extra && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIROperand result = ops[inst.result.index];
                    LIROperand pointer = ops[inst.x().index];
                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();
                    moveOp(new LIRAddress((LIRLocation) pointer, 0, inst.kind), result, inst.kind, null, false);
                    break;
                }

                case PointerStore: {
                    if ((Boolean) inst.extra && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIROperand value = ops[inst.y().index];
                    LIROperand pointer = ops[inst.x().index];
                    assert pointer.isVariableOrRegister();
                    moveOp(value, new LIRAddress((LIRLocation) pointer, 0, inst.kind), inst.kind, null, false);
                    break;
                }

                case PointerLoadDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && info != null) {
                        asm.recordImplicitException(codePos(), info);
                    }

                    LIRAddress.Scale scale = (addressInformation.scaling == null) ? Scale.Times1 : Scale.fromInt(((LIRConstant) ops[addressInformation.scaling.getIndex()]).asInt());
                    int displacement = (addressInformation.offset == null) ? 0 : ((LIRConstant) ops[addressInformation.offset.getIndex()]).asInt();

                    LIROperand result = ops[inst.result.index];
                    LIROperand pointer = ops[inst.x().index];
                    LIROperand index = ops[inst.y().index];

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

                    LIRAddress.Scale scale = (addressInformation.scaling == null) ? Scale.Times1 : Scale.fromInt(((LIRConstant) ops[addressInformation.scaling.getIndex()]).asInt());
                    int displacement = (addressInformation.offset == null) ? 0 : ((LIRConstant) ops[addressInformation.offset.getIndex()]).asInt();

                    LIROperand value = ops[inst.z().index];
                    LIROperand pointer = ops[inst.x().index];
                    LIROperand index = ops[inst.y().index];

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
                        result = ops[inst.result.index].asRegister();
                    }
                    Object[] args = new Object[inst.arguments.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = asRegisterOrConstant(ops[inst.arguments[i].index]);
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
                        LIROperand argumentSourceLocation = ops[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            moveOp(argumentSourceLocation, argumentLocation, argumentLocation.kind, null, false);
                        }
                    }

                    RiMethod method = (RiMethod) inst.extra;
                    masm.directCall(method, info);

                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        // (tw) remove this hack!
                        CiKind kind = CiKind.Long;
                        CiRegister register = this.compilation.target.registerConfig.getReturnRegister(inst.result.kind);
                        LIROperand resultLocation = forRegisters(kind, register, register);
                        moveOp(resultLocation, ops[inst.result.index], kind, null, false);
                    }
                    break;

                case Jmp: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    masm.jmp(label);
                    break;
                }
                case Jeq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.Equal, Condition.equal, ops, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.NotEqual, Condition.notEqual, ops, label);
                    break;
                }

                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.Greater, Condition.greater, ops, label);
                    break;
                }

                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.GreaterEqual, Condition.greaterEqual, ops, label);
                    break;
                }

                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.AboveEqual, Condition.aboveEqual, ops, label);
                    break;
                }

                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.Less, Condition.less, ops, label);
                    break;
                }

                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, LIRCondition.LessEqual, Condition.lessEqual, ops, label);
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

    private void emitXirCompare(XirInstruction inst, LIRCondition lirCondition, Condition condition, LIROperand[] ops, Label label) {
        LIROperand x = ops[inst.x().index];
        LIROperand y = ops[inst.y().index];
        emitCompare(lirCondition, x, y, null);
        masm.jcc(condition, label);
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
