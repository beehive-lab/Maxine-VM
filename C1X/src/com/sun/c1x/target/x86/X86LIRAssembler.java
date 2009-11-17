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

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.LIRAddress.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.x86.X86Assembler.Condition;
import com.sun.c1x.util.*;
import com.sun.c1x.xir.*;
import com.sun.c1x.xir.CiXirAssembler.*;

public class X86LIRAssembler extends LIRAssembler {

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
    }

    private boolean isLiteralAddress(LIRAddress addr) {
        return addr.base().isIllegal() && addr.index().isIllegal();
    }

    private Address asAddress(LIRAddress addr) {
        assert !addr.base.isIllegal();
        CiRegister base = addr.base().asPointerRegister(compilation.target.arch);

        if (addr.index().isIllegal()) {
            return new Address(base, addr.displacement());
        } else if (addr.index().isRegister()) {
            CiRegister index = addr.index().asPointerRegister(compilation.target.arch);
            return new Address(base, index, Address.ScaleFactor.fromLog(addr.scale().ordinal()), addr.displacement());
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void osrEntry() {
        throw Util.unimplemented();
    }

    @Override
    protected int initialFrameSizeInBytes() {
        return frameMap().frameSize();
    }

    @Override
    protected void returnOp(LIROperand result) {
        // Add again to the stack pointer
        masm.increment(target.stackPointerRegister, initialFrameSizeInBytes());
        // TODO: Add Safepoint polling at return!
        masm.ret(0);
    }

    @Override
    protected void safepointPoll(LIROperand tmp, LIRDebugInfo info) {
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
        assert src.isConstant() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";
        LIRConstant c = (LIRConstant) src;

        switch (c.kind) {
            case Boolean:
            case Byte:
            case Char:
            case Jsr:
            case Int: {
                masm.movl(dest.asRegister(), c.value.asInt());
                break;
            }

            case Word:
            case Long: {
                if (compilation.target.arch.is64bit()) {
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
        assert src.isConstant() : "should not call otherwise";
        assert dest.isStack() : "should not call otherwise";
        LIRConstant c = (LIRConstant) src;

        switch (c.kind) {
            case Int: // fall through
            case Float:
                masm.movl(frameMap().addressForSlot(dest.singleStackIndex()), c.asIntBits());
                break;

            case Object:
                masm.movoop(frameMap().addressForSlot(dest.singleStackIndex()), CiConstant.forObject(c.asObject()));
                break;

            case Long: // fall through
            case Double:

                if (compilation.target.arch.is64bit()) {
                    masm.movptr(frameMap().addressForSlot(dest.doubleStackIndex(), compilation.target.arch.lowWordOffset), c.asLongBits());
                } else {
                    masm.movptr(frameMap().addressForSlot(dest.doubleStackIndex(), compilation.target.arch.lowWordOffset), c.asIntLoBits());
                    masm.movptr(frameMap().addressForSlot(dest.doubleStackIndex(), compilation.target.arch.highWordOffset), c.asIntHiBits());
                }
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2mem(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info) {
        assert src.isConstant() : "should not call otherwise";
        assert dest.isAddress() : "should not call otherwise";
        LIRConstant c = (LIRConstant) src;
        LIRAddress addr = (LIRAddress) dest;

        int nullCheckHere = codeOffset();
        switch (type) {
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
                if (compilation.target.arch.is64bit()) {
                    if (isLiteralAddress(addr)) {
                        //masm().movptr(asAddress(addr, X86FrameMap.r15thread), c.asLongBits());
                        throw Util.shouldNotReachHere();
                    } else {
                        masm.movptr(rscratch1, c.asLongBits());
                        nullCheckHere = codeOffset();
                        masm.movptr(asAddressLo(addr), rscratch1);
                    }
                } else {
                    // Always reachable in 32bit so this doesn't produce useless move literal
                    masm.movptr(asAddressHi(addr), c.asIntHiBits());
                    masm.movptr(asAddressLo(addr), c.asIntLoBits());
                }
                break;

            case Boolean: // fall through
            case Byte:
                masm.movb(asAddress(addr), c.asInt() & 0xFF);
                break;

            case Char: // fall through
            case Short:
                masm.movw(asAddress(addr), c.asInt() & 0xFFFF);
                break;

            default:
                throw Util.shouldNotReachHere();
        }

        if (info != null) {
            addDebugInfoForNullCheck(nullCheckHere, info);
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
        assert src.isRegister() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";

        // move between cpu-registers
        if (dest.isSingleCpu()) {

            if (compilation.target.arch.is64bit()) {
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
            if (compilation.target.arch.is64bit()) {
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
            if (compilation.target.arch.is64bit()) {
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
        assert src.isRegister() : "should not call otherwise";
        assert dest.isStack() : "should not call otherwise";

        if (src.isSingleCpu()) {
            Address dst = frameMap().addressForSlot(dest.singleStackIndex());
            if (type == CiKind.Object || type == CiKind.Word) {
                if (type == CiKind.Object) {
                    masm.verifyOop(src.asRegister());
                }
                masm.movptr(dst, src.asRegister());
            } else {
                masm.movl(dst, src.asRegister());
            }

        } else if (src.isDoubleCpu()) {
            if (!compilation.target.arch.is64bit()) {
                Address dstLO = frameMap().addressForSlot(dest.doubleStackIndex(), compilation.target.arch.lowWordOffset);
                masm.movptr(dstLO, src.asRegisterLow());
                Address dstHI = frameMap().addressForSlot(dest.doubleStackIndex(), compilation.target.arch.highWordOffset);
                masm.movptr(dstHI, src.asRegisterHigh());
            } else {
                Address dstLO = frameMap().addressForSlot(dest.doubleStackIndex(), compilation.target.arch.lowWordOffset);
                masm.movptr(dstLO, src.asRegisterLow());

            }

        } else if (src.isSingleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.singleStackIndex());
            masm.movflt(dstAddr, asXmmFloatReg(src));

        } else if (src.isDoubleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.doubleStackIndex());
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
            addDebugInfoForNullCheckHere(info);
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
                if (compilation.target.arch.is64bit()) {
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
                if (compilation.target.arch.is64bit()) {
                    masm.movptr(asAddressLo(toAddr), fromLo);
                } else {
                    CiRegister base = toAddr.base().asRegister();
                    CiRegister index = CiRegister.None;
                    if (toAddr.index().isRegister()) {
                        index = toAddr.index().asRegister();
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
        assert dest.isRegister() : "should not call otherwise";

        if (dest.isSingleCpu()) {
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.movptr(dest.asRegister(), frameMap().addressForSlot(src.singleStackIndex()));
                if (type == CiKind.Object) {
                    masm.verifyOop(dest.asRegister());
                }
            } else {
                masm.movl(dest.asRegister(), frameMap().addressForSlot(src.singleStackIndex()));
            }

        } else if (dest.isDoubleCpu()) {
            Address srcAddrLO = frameMap().addressForSlot(src.doubleStackIndex(), compilation.target.arch.lowWordOffset);
            Address srcAddrHI = frameMap().addressForSlot(src.doubleStackIndex(), compilation.target.arch.highWordOffset);
            masm.movptr(dest.asRegisterLow(), srcAddrLO);
            if (!compilation.target.arch.is64bit()) {
                masm.movptr(dest.asRegisterHigh(), srcAddrHI);
            }

        } else if (dest.isSingleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.singleStackIndex());
            masm.movflt(asXmmFloatReg(dest), srcAddr);

        } else if (dest.isDoubleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIndex());
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
            if (compilation.target.arch.is64bit()) {
                masm.pushptr(asAddress((LIRAddress) src));
                masm.popptr(asAddress((LIRAddress) dest));
            } else {
                throw Util.unimplemented("32-bit not supported");
            }
        }
    }

    @Override
    protected void mem2stack(LIROperand src, LIROperand dest, CiKind type) {
        if (dest.isSingleStack()) {
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.pushptr(asAddress((LIRAddress) src));
                masm.popptr(frameMap().addressForSlot(dest.singleStackIndex()));
            } else {
                masm.pushl(asAddress((LIRAddress) src));
                masm.popl(frameMap().addressForSlot(dest.singleStackIndex()));
            }

        } else if (dest.isDoubleStack()) {
            if (compilation.target.arch.is64bit()) {
                masm.pushptr(asAddress((LIRAddress) src));
                masm.popptr(frameMap().addressForSlot(dest.doubleStackIndex()));
            } else {
                throw Util.unimplemented("32-bit not supported");
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void stack2stack(LIROperand src, LIROperand dest, CiKind type) {
        if (src.isSingleStack()) {
            if (type == CiKind.Object || type == CiKind.Word) {
                masm.pushptr(frameMap().addressForSlot(src.singleStackIndex()));
                masm.popptr(frameMap().addressForSlot(dest.singleStackIndex()));
            } else {
                masm.pushl(frameMap().addressForSlot(src.singleStackIndex()));
                masm.popl(frameMap().addressForSlot(dest.singleStackIndex()));
            }

        } else if (src.isDoubleStack()) {
            if (compilation.target.arch.is64bit()) {
                masm.pushptr(frameMap().addressForSlot(src.doubleStackIndex()));
                masm.popptr(frameMap().addressForSlot(dest.doubleStackIndex()));
            } else {
                masm.pushl(frameMap().addressForSlot(src.doubleStackIndex(), 0));
                // push and pop the part at src + wordSize, adding wordSize for the previous push
                masm.pushl(frameMap().addressForSlot(src.doubleStackIndex(), 2 * compilation.target.arch.wordSize));
                masm.popl(frameMap().addressForSlot(dest.doubleStackIndex(), 2 * compilation.target.arch.wordSize));
                masm.popl(frameMap().addressForSlot(dest.doubleStackIndex(), 0));
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void mem2reg(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info, boolean unaligned) {
        assert src.isAddress() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";

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
            addDebugInfoForNullCheckHere(info);
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
                if (compilation.target.arch.is64bit()) {
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

                if (compilation.target.arch.is64bit()) {
                    masm.movptr(toLo, asAddressLo(addr));
                } else {
                    CiRegister base = addr.base().asRegister();
                    CiRegister index = CiRegister.None;
                    if (addr.index().isRegister()) {
                        index = addr.index().asRegister();
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
    protected void prefetchr(LIROperand src) {
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
                addDebugInfoForBranch(op.info);
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
                if (compilation.target.arch.is64bit()) {
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
                assert src.isSingleXmm() && dest.isRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttss2sil(dest.asRegister(), srcRegister);
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(Condition.notEqual, endLabel);
                GlobalStub globalStub = compilation.compiler.lookupGlobalStub(GlobalStub.Id.f2i);
                masm.callGlobalStub(globalStub, null, dest.asRegister(), srcRegister);
                // cannot cause an exception
                masm.bind(endLabel);
                break;
            }
            case Bytecodes.D2I: {
                assert src.isDoubleXmm() && dest.isRegister() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2sil(dest.asRegister(), asXmmDoubleReg(src));
                masm.cmp32(dest.asRegister(), Integer.MIN_VALUE);
                masm.jcc(Condition.notEqual, endLabel);
                GlobalStub globalStub = compilation.compiler.lookupGlobalStub(GlobalStub.Id.d2i);
                masm.callGlobalStub(globalStub, null, dest.asRegister(), srcRegister);
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
                GlobalStub globalStub = compilation.compiler.lookupGlobalStub(GlobalStub.Id.f2l);
                masm.callGlobalStub(globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            case Bytecodes.D2L: {
                assert src.isDoubleXmm() && dest.isDoubleCpu() : "must both be XMM register (no fpu stack)";
                masm.cvttsd2siq(dest.asRegister(), asXmmDoubleReg(src));
                masm.mov64(rscratch1, Long.MIN_VALUE);
                masm.cmpq(dest.asRegister(), rscratch1);
                masm.jcc(Condition.notEqual, endLabel);
                GlobalStub globalStub = compilation.compiler.lookupGlobalStub(GlobalStub.Id.d2l);
                masm.callGlobalStub(globalStub, null, dest.asRegister(), srcRegister);
                masm.bind(endLabel);
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitAllocObj(LIRAllocObj op) {
        if (op.isInitCheck()) {
            throw Util.unimplemented("check for class init status");
        }
        masm.allocateObject(compilation.runtime, op.obj().asRegister(), op.tmp1().asRegister(), op.tmp2().asRegister(), op.headerSize(), op.obectSize(), op.klass().asRegister(), op.stub().entry);
        masm.bind(op.stub().continuation);
    }

    @Override
    protected void emitAllocArray(LIRAllocArray op) {
        if (C1XOptions.UseSlowPath || (!C1XOptions.UseFastNewObjectArray && op.type() == CiKind.Object) || (!C1XOptions.UseFastNewTypeArray && op.type() != CiKind.Object)) {
            masm.jmp(op.stub().entry);
        } else {
            CiRegister len = op.length().asRegister();
            CiRegister tmp1 = op.tmp1().asRegister();
            CiRegister tmp2 = op.tmp2().asRegister();
            CiRegister tmp3 = op.tmp3().asRegister();
            if (len == tmp1) {
                tmp1 = tmp3;
            } else if (len == tmp2) {
                tmp2 = tmp3;
            } else if (len == tmp3) {
                // everything is ok
            } else {
                masm.mov(tmp3, len);
            }
            int elemSize = compilation.target.sizeInBytes(op.type());
            masm.allocateArray(compilation.runtime, op.obj().asRegister(), len, tmp1, tmp2, compilation.runtime.arrayHeaderSize(op.type()),
                            Address.ScaleFactor.fromInt(elemSize), op.klass().asRegister(), op.stub().entry);
        }
        masm.bind(op.stub().continuation);
    }

    static void selectDifferentRegisters(CiRegister preserve, CiRegister extra, CiRegister[] tmp1, CiRegister[] tmp2) {
        if (tmp1[0] == preserve) {
            assert CiRegister.assertDifferentRegisters(tmp1[0], tmp2[0], extra);
            tmp1[0] = extra;
        } else if (tmp2[0] == preserve) {
            CiRegister.assertDifferentRegisters(tmp1[0], tmp2[0], extra);
            tmp2[0] = extra;
        }
        CiRegister.assertDifferentRegisters(preserve, tmp1[0], tmp2[0]);
    }

    static void selectDifferentRegisters(CiRegister preserve, CiRegister extra, CiRegister[] tmp1, CiRegister[] tmp2, CiRegister[] tmp3) {
        if (tmp1[0] == preserve) {
            CiRegister.assertDifferentRegisters(tmp1[0], tmp2[0], tmp3[0], extra);
            tmp1[0] = extra;
        } else if (tmp2[0] == preserve) {
            CiRegister.assertDifferentRegisters(tmp1[0], tmp2[0], tmp3[0], extra);
            tmp2[0] = extra;
        } else if (tmp3[0] == preserve) {
            CiRegister.assertDifferentRegisters(tmp1[0], tmp2[0], tmp3[0], extra);
            tmp3[0] = extra;
        }
        CiRegister.assertDifferentRegisters(preserve, tmp1[0], tmp2[0], tmp3[0]);
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

            CodeStub stub = op.stub();
            Label done = new Label();
            masm.cmpptr(value, (int) NULLWORD);
            masm.jcc(X86Assembler.Condition.equal, done);
            addDebugInfoForNullCheckHere(op.info);

            masm.movptr(kRInfo, new Address(array, compilation.runtime.hubOffset()));
            masm.movptr(klassRInfo, new Address(value, compilation.runtime.hubOffset()));

            // get instance klass
            masm.movptr(kRInfo, new Address(kRInfo, compilation.runtime.elementHubOffset()));
            masm.callRuntimeCalleeSaved(CiRuntimeCall.SlowSubtypeCheck, op.info, rtmp1, kRInfo, klassRInfo);

            // result is a boolean
            masm.cmpl(rtmp1, 0);
            masm.jcc(X86Assembler.Condition.equal, stub.entry);
            masm.bind(done);
        } else if (op.code == LIROpcode.CheckCast) {
            // we always need a stub for the failure case.
            CodeStub stub = op.stub();
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
            if (op.profiledMethod() != null) {

                Util.unimplemented();
            } else {
                masm.jcc(X86Assembler.Condition.equal, done);
            }
            masm.verifyOop(obj);

            if (op.isFastCheck()) {
                // get object class
                // not a safepoint as obj null check happens earlier
                if (k.isLoaded()) {
                    if (compilation.target.arch.is64bit()) {
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
                masm.callRuntimeCalleeSaved(CiRuntimeCall.SlowCheckCast, op.info, dst, obj, expectedHub);
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
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
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
                // next block is unconditional if LP64:

                // TODO: Check if this is really necessary
                //assert dst != klassRInfo && dst != kRInfo : "need 3 registers";

                // perform the fast part of the checking logic
                //masm().checkKlassSubtypeFastPath(klassRInfo, kRInfo, dst, one, zero, null, new RegisterOrConstant(-1));
                // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                masm.callRuntimeCalleeSaved(CiRuntimeCall.SlowSubtypeCheck, op.info, dst, kRInfo, klassRInfo);
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
        if (!compilation.target.arch.is64bit() && op.code == LIROpcode.CasLong && compilation.target.supportsCx8()) {
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
            assert compilation.target.arch.is64bit() || op.address().isSingleCpu() : "must be single";
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
            } else if (compilation.target.arch.is64bit()) {
                masm.cmpxchgq(newval, new Address(addr, 0));
            }
        } else if (compilation.target.arch.is64bit() && op.code == LIROpcode.CasLong) {
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
    protected void cmove(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROperand result) {
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

        if (opr1.isRegister()) {
            reg2reg(opr1, result);
        } else if (opr1.isStack()) {
            stack2reg(opr1, result, result.kind);
        } else if (opr1.isConstant()) {
            const2reg(opr1, result, null);
        } else {
            throw Util.shouldNotReachHere();
        }

        if (compilation.target.supportsCmov() && !opr2.isConstant()) {
            // optimized version that does not require a branch
            if (opr2.isSingleCpu()) {
                assert opr2.asRegister() != result.asRegister() : "opr2 already overwritten by previous move";
                masm.cmov(ncond, result.asRegister(), opr2.asRegister());
            } else if (opr2.isDoubleCpu()) {
                assert opr2.cpuRegNumberLow() != result.cpuRegNumberLow() && opr2.cpuRegNumberLow() != result.cpuRegNumberHigh() : "opr2 already overwritten by previous move";
                assert opr2.cpuRegNumberHigh() != result.cpuRegNumberLow() && opr2.cpuRegNumberHigh() != result.cpuRegNumberHigh() : "opr2 already overwritten by previous move";
                masm.cmovptr(ncond, result.asRegisterLow(), opr2.asRegisterLow());
                if (!compilation.target.arch.is64bit()) {
                    masm.cmovptr(ncond, result.asRegisterHigh(), opr2.asRegisterHigh());
                }
            } else if (opr2.isSingleStack()) {
                masm.cmovl(ncond, result.asRegister(), frameMap().addressForSlot(opr2.singleStackIndex()));
            } else if (opr2.isDoubleStack()) {
                masm.cmovptr(ncond, result.asRegisterLow(), frameMap().addressForSlot(opr2.doubleStackIndex(), compilation.target.arch.lowWordOffset));
                if (!compilation.target.arch.is64bit()) {
                    masm.cmovptr(ncond, result.asRegisterHigh(), frameMap().addressForSlot(opr2.doubleStackIndex(), compilation.target.arch.highWordOffset));
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            Label skip = new Label();
            masm.jcc(acond, skip);
            if (opr2.isRegister()) {
                reg2reg(opr2, result);
            } else if (opr2.isStack()) {
                stack2reg(opr2, result, result.kind);
            } else if (opr2.isConstant()) {
                const2reg(opr2, result, null);
            } else {
                throw Util.shouldNotReachHere();
            }
            masm.bind(skip);
        }
    }

    @Override
    protected void arithOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dest, LIRDebugInfo info) {
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
                Address raddr = frameMap().addressForSlot(right.singleStackIndex());
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

            } else if (right.isConstant()) {
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
                assert compilation.target.arch.is64bit() || CiRegister.assertDifferentRegisters(lregLo, lregHi, rregLo, rregHi);
                assert !compilation.target.arch.is64bit() || CiRegister.assertDifferentRegisters(lregLo, rregLo);
                switch (code) {
                    case Add:
                        masm.addptr(lregLo, rregLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm.adcl(lregHi, rregHi);
                        }
                        break;
                    case Sub:
                        masm.subptr(lregLo, rregLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm.sbbl(lregHi, rregHi);
                        }
                        break;
                    case Mul:
                        if (compilation.target.arch.is64bit()) {
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

            } else if (right.isConstant()) {
                // cpu register - constant
                if (compilation.target.arch.is64bit()) {
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
                    raddr = frameMap().addressForSlot(right.singleStackIndex());
                } else if (right.isConstant()) {
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
                    raddr = frameMap().addressForSlot(right.doubleStackIndex());
                } else if (right.isConstant()) {
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

        } else if (left.isSingleStack() || left.isAddress()) {
            assert left.equals(dest) : "left and dest must be equal";

            Address laddr;
            if (left.isSingleStack()) {
                laddr = frameMap().addressForSlot(left.singleStackIndex());
            } else if (left.isAddress()) {
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
            } else if (right.isConstant()) {
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
    protected void intrinsicOp(LIROpcode code, LIROperand value, LIROperand unused, LIROperand dest, LIROp2 op) {

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
    protected void logicOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst) {
        if (left.isSingleCpu()) {
            CiRegister reg = left.asRegister();
            if (right.isConstant()) {
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
                Address raddr = frameMap().addressForSlot(right.singleStackIndex());
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
            if (right.isConstant()) {
                if (compilation.target.arch.is64bit()) {
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
                        if (!compilation.target.arch.is64bit()) {
                            masm.andptr(lHi, rHi);
                        }
                        break;
                    case LogicOr:
                        masm.orptr(lLo, rLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm.orptr(lHi, rHi);
                        }
                        break;
                    case LogicXor:
                        masm.xorptr(lLo, rLo);

                        if (!compilation.target.arch.is64bit()) {
                            masm.xorptr(lHi, rHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

            CiRegister dstLo = dst.asRegisterLow();
            CiRegister dstHi = dst.asRegisterHigh();

            if (compilation.target.arch.is64bit()) {
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

    // we assume that rax, and rdx can be overwritten
    void arithmeticIdiv(LIROpcode code, LIROperand left, LIROperand right, LIROperand result, LIRDebugInfo info) {
        assert left.isSingleCpu() : "left must be register";
        assert right.isSingleCpu() || right.isConstant() : "right must be register or constant";
        assert result.isSingleCpu() : "result must be register";

        // assert left.destroysRegister() : "check";
        // assert right.destroysRegister() : "check";

        CiRegister lreg = left.asRegister();
        CiRegister dreg = result.asRegister();

        if (right.isConstant()) {
            int divisor = ((LIRConstant) right).asInt();
            assert divisor > 0 && Util.isPowerOf2(divisor) : "must be";
            if (code == LIROpcode.Idiv) {
                assert lreg == X86.rax : "must be rax : ";
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
            assert lreg == X86.rax : "left register must be rax : ";
            assert rreg != X86.rdx : "right register must not be rdx";

            moveRegs(lreg, X86.rax);

            int idivlOffset = masm.correctedIdivl(rreg);
            addDebugInfoForDiv0(idivlOffset, info);
            if (code == LIROpcode.Irem) {
                moveRegs(X86.rdx, dreg); // result is in rdx
            } else if (code == LIROpcode.Idiv) {
                moveRegs(X86.rax, dreg);
            } else {
                throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void compOp(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROp2 op) {
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
                    masm.cmpptr(reg1, frameMap().addressForSlot(opr2.singleStackIndex()));
                } else {
                    masm.cmpl(reg1, frameMap().addressForSlot(opr2.singleStackIndex()));
                }
            } else if (opr2.isConstant()) {
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
                        if (compilation.target.arch.is64bit()) {
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
            } else if (opr2.isAddress()) {
                if (op != null && op.info != null) {
                    addDebugInfoForNullCheckHere(op.info);
                }
                masm.cmpl(reg1, asAddress((LIRAddress) opr2));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleCpu()) {
            CiRegister xlo = opr1.asRegisterLow();
            CiRegister xhi = opr1.asRegisterHigh();
            if (opr2.isDoubleCpu()) {
                if (compilation.target.arch.is64bit()) {
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
            } else if (opr2.isConstant()) {
                // cpu register - constant 0
                LIRConstant constantOpr2 = (LIRConstant) opr2;
                assert constantOpr2.asLong() == 0 : "only handles zero";
                if (compilation.target.arch.is64bit()) {
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
                masm.ucomiss(reg1, frameMap().addressForSlot(opr2.singleStackIndex()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                masm.ucomiss(reg1, masm.recordDataReferenceInCode(CiConstant.forFloat(((LIRConstant) opr2).asFloat())));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op != null && op.info != null) {
                    addDebugInfoForNullCheckHere(op.info);
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
                masm.ucomisd(reg1, frameMap().addressForSlot(opr2.doubleStackIndex()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                masm.ucomisd(reg1, masm.recordDataReferenceInCode(CiConstant.forDouble(((LIRConstant) opr2).asDouble())));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op != null && op.info != null) {
                    addDebugInfoForNullCheckHere(op.info);
                }
                masm.ucomisd(reg1, asAddress((LIRAddress) opr2));
            } else {
                throw Util.shouldNotReachHere();
            }
        } else if (opr1.isAddress() && opr2.isConstant()) {
            LIRConstant c = ((LIRConstant) opr2);

            if (compilation.target.arch.is64bit()) {
                if (c.kind == CiKind.Object) {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "need to reverse";
                    masm.movoop(rscratch1, CiConstant.forObject(c.asObject()));
                }
            }
            if (op != null && op.info != null) {
                addDebugInfoForNullCheckHere(op.info);
            }
            // special case: address - constant
            LIRAddress addr = (LIRAddress) opr1;
            if (c.kind == CiKind.Int) {
                masm.cmpl(asAddress(addr), c.asInt());
            } else if (c.kind == CiKind.Object || c.kind == CiKind.Word) {
                if (compilation.target.arch.is64bit()) {
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
    protected void compFl2i(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst, LIROp2 op) {
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
            if (compilation.target.arch.is64bit()) {
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
    protected void alignCall(LIROpcode code) {
        if (compilation.runtime.isMP()) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = masm.codeBuffer.position();
            switch (code) {
                case StaticCall:
                case OptVirtualCall:
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
    protected void xirIndirectCall(RiMethod method, LIRDebugInfo info, LIROperand callAddress) {

        CiRegister reg = compilation.target.scratchRegister;
        if (callAddress.isRegister()) {
            reg = callAddress.asRegister();
        } else {
            moveOp(callAddress, LIROperandFactory.singleLocation(callAddress.kind, reg), callAddress.kind, null, false);
        }
        masm.call(reg, method, info.oopMap.stackMap());
        addCallInfoHere(info);
    }

    @Override
    protected void xirDirectCall(RiMethod method, LIRDebugInfo info) {
        masm.call(method, info.oopMap.stackMap());
        addCallInfoHere(info);
    }

    @Override
    protected void directCall(RiMethod method, CiRuntimeCall entry, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        if (method.isLoaded()) {
            masm.call(method, info.oopMap.stackMap());
        } else {
            assert entry != null;
            masm.callRuntimeCalleeSaved(entry, info, rscratch1, new RegisterOrConstant(cpi), new RegisterOrConstant(constantPool.encoding().asObject()));
            masm.call(rscratch1, method, info.oopMap.stackMap());
        }
        addCallInfoHere(info);
    }

    @Override
    protected void virtualCall(RiMethod method, LIROperand receiver, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        Address callAddress;
        if (method.vtableIndex() >= 0) {
            int vtableOffset = compilation.runtime.vtableEntryMethodOffsetInBytes() + compilation.runtime.vtableStartOffset() + method.vtableIndex() * compilation.runtime.vtableEntrySize();
            assert receiver != null && vtableOffset >= 0 : "Invalid receiver or vtable offset!";
            assert receiver.isRegister() : "Receiver must be in a register";
            addCallInfoHere(info);
            assert !compilation.runtime.needsExplicitNullCheck(compilation.runtime.hubOffset());
            callAddress = new Address(rscratch1, Util.safeToInt(vtableOffset));
            masm.movq(rscratch1, new Address(receiver.asRegister(), compilation.runtime.hubOffset()));
        } else {
            assert method.vtableIndex() == -1 && !method.isLoaded();
            this.masm.callRuntimeCalleeSaved(CiRuntimeCall.ResolveVTableIndex, info, rscratch1, new RegisterOrConstant(cpi), new RegisterOrConstant(constantPool.encoding().asObject()));
            addCallInfoHere(info);
            int vtableEntrySize = compilation.runtime.vtableEntrySize();
            assert Util.isPowerOf2(vtableEntrySize);
            masm.shlq(rscratch1, Util.log2(vtableEntrySize));
            masm.addq(rscratch1, compilation.runtime.vtableEntryMethodOffsetInBytes() + compilation.runtime.vtableStartOffset());
            addCallInfoHere(info);
            masm.addq(rscratch1, new Address(receiver.asRegister(), compilation.runtime.hubOffset()));
            callAddress = new Address(rscratch1);
        }

        masm.call(callAddress, method, info.oopMap.stackMap());
        addCallInfoHere(info);
    }

    /**
     * (tw) Tentative implementation of an interface call.
     */
    @Override
    protected void interfaceCall(RiMethod method, LIROperand receiver, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        assert receiver != null : "Invalid receiver!";
        assert receiver.isRegister() : "Receiver must be in a register";

        if (method.vtableIndex() == -1) {
            // Unresolved method
            this.masm.callRuntimeCalleeSaved(CiRuntimeCall.ResolveInterfaceIndex, info, rscratch1, new RegisterOrConstant(receiver.asRegister()), new RegisterOrConstant(cpi), new RegisterOrConstant(constantPool.encoding().asObject()));
        } else {
            // TODO: emit interface ID calculation inline
            masm.movl(rscratch1, method.interfaceID());
            masm.callRuntimeCalleeSaved(CiRuntimeCall.RetrieveInterfaceIndex, info, rscratch1, receiver.asRegister(), rscratch1);
            masm.addq(rscratch1, method.indexInInterface() * compilation.target.arch.wordSize);
        }

        addCallInfoHere(info);
        masm.addq(rscratch1, new Address(receiver.asRegister(), compilation.runtime.hubOffset()));
        masm.call(new Address(rscratch1, 0), method, info.oopMap.stackMap());
        addCallInfoHere(info);
    }

    @Override
    protected void emitRuntimeCall(LIRRuntimeCall op) {
        rtCall(op.result(), op.runtimeEntry, op.arguments(), op.info, op.calleeSaved);
    }

    @Override
    protected void throwOp(LIROperand exceptionPC, LIROperand exceptionOop, LIRDebugInfo info, boolean unwind) {
       // exception object is not added to oop map by LinearScan
       // (LinearScan assumes that no oops are in fixed registers)
       // info.addRegisterOop(exceptionOop);
        CiRuntimeCall unwindId;

        if (unwind) {
            unwindId = CiRuntimeCall.UnwindException;
        } else {
            unwindId = CiRuntimeCall.HandleException;
        }
        masm.callRuntime(unwindId);

        if (!unwind) {
            addCallInfoHere(info);
        }

        // enough room for two byte trap
        masm.nop();
    }

    @Override
    protected void shiftOp(LIROpcode code, LIROperand left, LIROperand count, LIROperand dest, LIROperand tmp) {
        // optimized version for linear scan:
        // * count must be already in ECX (guaranteed by LinearScan)
        // * left and dest must be equal
        // * tmp must be unused
        assert count.asRegister() == SHIFTCount : "count must be in ECX";
        assert left == dest : "left and dest must be equal";
        assert tmp.isIllegal() : "wasting a register if tmp is allocated";

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

            if (compilation.target.arch.is64bit()) {
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
    protected void shiftOp(LIROpcode code, LIROperand left, int count, LIROperand dest) {
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

            if (!compilation.target.arch.is64bit()) {
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
    protected void emitLock(LIRLock op) {
        CiRegister obj = op.objOpr().asRegister(); // may not be an oop
        CiRegister hdr = op.hdrOpr().asRegister();
        CiRegister lock = op.lockOpr().asRegister();
        if (!C1XOptions.UseFastLocking) {
            masm.jmp(op.stub().entry);
        } else if (op.code == LIROpcode.Monitorenter) {
            CiRegister scratch = CiRegister.None;
            if (C1XOptions.UseBiasedLocking) {
                scratch = op.scratchOpr().asRegister();
            }
            // add debug info for NullPointerException only if one is possible
            int nullCheckOffset = masm.lockObject(compilation.runtime, hdr, obj, lock, scratch, op.stub().entry);
            if (op.info != null) {
                addDebugInfoForNullCheck(nullCheckOffset, op.info);
            }
            // done
        } else if (op.code == LIROpcode.Monitorexit) {
            masm.unlockObject(compilation.runtime, hdr, obj, lock, op.stub().entry);
        } else {
            throw Util.shouldNotReachHere();
        }
        masm.bind(op.stub().continuation);
    }

    @Override
    protected void monitorAddress(int monitorNo, LIROperand dst) {
        masm.movl(dst.asRegister(), frameMap().addressForMonitorLock(monitorNo));
    }

    @Override
    protected void alignBackwardBranchTarget() {
        masm.align(compilation.target.arch.wordSize);
    }

    @Override
    protected void negate(LIROperand left, LIROperand dest) {
        if (left.isSingleCpu()) {
            masm.negl(left.asRegister());
            moveRegs(left.asRegister(), dest.asRegister());

        } else if (left.isDoubleCpu()) {
            CiRegister lo = left.asRegisterLow();
            if (compilation.target.arch.is64bit()) {
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
            GlobalStub globalStub = compilation.compiler.lookupGlobalStub(GlobalStub.Id.fneg);
            masm.callGlobalStub(globalStub, null, asXmmFloatReg(dest), asXmmFloatReg(dest));

        } else if (dest.isDoubleXmm()) {
            if (asXmmDoubleReg(left) != asXmmDoubleReg(dest)) {
                masm.movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(left));
            }

            GlobalStub globalStub = compilation.compiler.lookupGlobalStub(GlobalStub.Id.dneg);
            masm.callGlobalStub(globalStub, null, asXmmDoubleReg(dest), asXmmDoubleReg(dest));
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void leal(LIRAddress addr, LIRLocation dest) {
        CiRegister reg = dest.asPointerRegister(compilation.target.arch);
        masm.lea(reg, asAddress(addr));
    }

    @Override
    protected void rtCall(LIROperand result, CiRuntimeCall dest, List<LIROperand> args, LIRDebugInfo info, boolean calleeSaved) {

        if (calleeSaved) {
            // Call through global stub
            assert result.isRegister();

            final List<RegisterOrConstant> arguments = new ArrayList<RegisterOrConstant>();
            for (LIROperand op : args) {
                if (op.isConstant()) {
                    LIRConstant constantOp = (LIRConstant) op;
                    assert op.kind == CiKind.Int || op.kind == CiKind.Object;
                    if (op.kind == CiKind.Int) {
                        arguments.add(new RegisterOrConstant(constantOp.asInt()));
                    } else if (op.kind == CiKind.Object) {
                        arguments.add(new RegisterOrConstant(((LIRConstant) op).asObject()));
                    }
                } else {
                    assert op.isRegister();
                    arguments.add(new RegisterOrConstant(op.asRegister()));
                }
            }
            masm.callRuntimeCalleeSaved(dest, info, result.asRegister(), arguments.toArray(new RegisterOrConstant[arguments.size()]));
        } else {
            // Call direct
            masm.callRuntime(dest);
            if (info != null) {
                addCallInfoHere(info);
            }
        }
    }

    @Override
    protected void volatileMoveOp(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info) {
        assert type == CiKind.Long : "only for volatile long fields";

        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        if (src.isDoubleXmm()) {
            if (dest.isDoubleCpu()) {
                if (compilation.target.arch.is64bit()) {
                    masm.movdq(dest.asRegisterLow(), asXmmDoubleReg(src));
                } else {
                    masm.movdl(dest.asRegisterLow(), asXmmDoubleReg(src));
                    masm.psrlq(asXmmDoubleReg(src), 32);
                    masm.movdl(dest.asRegisterHigh(), asXmmDoubleReg(src));
                }
            } else if (dest.isDoubleStack()) {
                masm.movdbl(frameMap().addressForSlot(dest.doubleStackIndex()), asXmmDoubleReg(src));
            } else if (dest.isAddress()) {
                masm.movdbl(asAddress((LIRAddress) dest), asXmmDoubleReg(src));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleXmm()) {
            if (src.isDoubleStack()) {
                masm.movdbl(asXmmDoubleReg(dest), frameMap().addressForSlot(src.doubleStackIndex()));
            } else if (src.isAddress()) {
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
    protected void membar() {
        // QQQ sparc TSO uses this,
        masm.membar(X86Assembler.MembarMaskBits.StoreLoad.mask());

    }

    @Override
    protected void membarAcquire() {
        // No x86 machines currently require load fences
        // lir(). loadFence();
    }

    @Override
    protected void membarRelease() {
        // No x86 machines currently require store fences
        // lir(). storeFence();
    }

    @Override
    protected void getThread(LIROperand resultReg) {
        assert resultReg.isRegister() : "check";
        if (compilation.target.arch.is64bit()) {
            // lir(). getThread(resultReg.asRegisterLo());
            masm.mov(resultReg.asRegister(), compilation.target.config.getThreadRegister());
        } else {
            masm.getThread(resultReg.asRegister());
        }
    }

    @Override
    protected void peephole(LIRList list) {
        // Do nothing for now
    }

    @Override
    protected void emitLIROp2(LIROp2 op) {
        switch (op.code) {
            case Cmp:
                if (op.info != null) {
                    assert op.opr1().isAddress() || op.opr2().isAddress() : "shouldn't be codeemitinfo for non-Pointer operands";
                    addDebugInfoForNullCheckHere(op.info); // exception possible
                }
                compOp(op.condition(), op.opr1(), op.opr2(), op);
                break;

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                compFl2i(op.code, op.opr1(), op.opr2(), op.result(), op);
                break;

            case Cmove:
                cmove(op.condition(), op.opr1(), op.opr2(), op.result());
                break;

            case Shl:
            case Shr:
            case Ushr:
                if (op.opr2().isConstant()) {
                    shiftOp(op.code, op.opr1(), ((LIRConstant) op.opr2()).asInt(), op.result());
                } else {
                    shiftOp(op.code, op.opr1(), op.opr2(), op.result(), op.tmp());
                }
                break;

            case Add:
            case Sub:
            case Mul:
            case Div:
            case Rem:
                arithOp(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                intrinsicOp(op.code, op.opr1(), op.opr2(), op.result(), op);
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                logicOp(op.code, op.opr1(), op.opr2(), op.result());
                break;

            case Throw:
            case Unwind:
                throwOp(op.opr1(), op.opr2(), op.info, op.code == LIROpcode.Unwind);
                break;

            default:
                Util.unimplemented();
                break;
        }
    }

    @Override
    protected void emitPrologue() {

        int entryCodeOffset = compilation.runtime.codeOffset();
        if (entryCodeOffset > 0) {
            JITAdapterFrameStub stub = new JITAdapterFrameStub();
            adapterFrameStub = stub;

            masm.jmp(stub.entry);
            assert asm.codeBuffer.position() <= entryCodeOffset;
            while (asm.codeBuffer.position() < entryCodeOffset) {
                int oldVal = asm.codeBuffer.position();
                masm.nop();
                assert oldVal != asm.codeBuffer.position();
            }
            masm.bind(stub.continuation);
        }
    }

    @Override
    protected void emitCode(CodeStub s) {
        s.accept(new X86CodeStubVisitor(this));
    }

    private static RegisterOrConstant asRegisterOrConstant(LIROperand operand) {
        if (operand.isRegister()) {
            return new RegisterOrConstant(operand.asRegister());
        } else if (operand.isConstant()) {
            final LIRConstant c = (LIRConstant) operand;
            if (c.value.kind == CiKind.Int) {
                return new RegisterOrConstant(c.value.asInt());
            } else if (c.value.kind == CiKind.Object) {
                return new RegisterOrConstant(c.value.asObject());
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void resolve(CiRuntimeCall stub, LIRDebugInfo info, LIROperand dest, LIROperand index, LIROperand cp) {
        masm.callRuntimeCalleeSaved(stub, info, dest.asRegister(), asRegisterOrConstant(index), asRegisterOrConstant(cp));
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
        for (XirInstruction inst : instructions) {
            switch (inst.op) {
                case Add:
                    arithOp(LIROpcode.Add, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Sub:
                    arithOp(LIROpcode.Sub, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Div:
                    if (inst.kind == CiKind.Int) {
                        arithmeticIdiv(LIROpcode.Idiv, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    }
                    arithOp(LIROpcode.Div, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Mul:
                    arithOp(LIROpcode.Mul, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    break;

                case Mod:
                    if (inst.kind == CiKind.Int) {
                        arithmeticIdiv(LIROpcode.Irem, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    } else {
                        arithOp(LIROpcode.Rem, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], null);
                    }
                    break;

                case Shl:
                    shiftOp(LIROpcode.Shl, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], LIROperandFactory.IllegalLocation);
                    break;

                case Shr:
                    shiftOp(LIROpcode.Shr, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index], LIROperandFactory.IllegalLocation);
                    break;

                case And:
                    logicOp(LIROpcode.LogicAnd, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index]);
                    break;

                case Or:
                    logicOp(LIROpcode.LogicOr, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index]);
                    break;

                case Xor:
                    logicOp(LIROpcode.LogicXor, ops[inst.x().index], ops[inst.y().index], ops[inst.result.index]);
                    break;

                case Mov: {
                    LIROperand result = ops[inst.result.index];
                    LIROperand source = ops[inst.x().index];
                    moveOp(source, result, result.kind, null, false);
                    break;
                }

                case PointerLoad: {
                    if ((Boolean) inst.extra && xir.info != null) {
                        addCallInfoHere(xir.info);
                    }

                    LIROperand result = ops[inst.result.index];
                    LIROperand pointer = ops[inst.x().index];
                    pointer = assureInRegister(pointer);
                    assert pointer.isRegister();
                    moveOp(new LIRAddress((LIRLocation) pointer, 0, inst.kind), result, inst.kind, null, false);
                    break;
                }

                case PointerStore: {
                    if ((Boolean) inst.extra && xir.info != null) {
                        addCallInfoHere(xir.info);
                    }

                    LIROperand value = ops[inst.y().index];
                    LIROperand pointer = ops[inst.x().index];
                    assert pointer.isRegister();
                    moveOp(value, new LIRAddress((LIRLocation) pointer, 0, inst.kind), inst.kind, null, false);
                    break;
                }

                case PointerLoadDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && xir.info != null) {
                        addCallInfoHere(xir.info);
                    }

                    LIRAddress.Scale scale = (addressInformation.scaling == null) ? Scale.Times1 : Scale.fromInt(((LIRConstant) ops[addressInformation.scaling.getIndex()]).asInt());
                    int displacement = (addressInformation.offset == null) ? 0 : ((LIRConstant) ops[addressInformation.offset.getIndex()]).asInt();

                    LIROperand result = ops[inst.result.index];
                    LIROperand pointer = ops[inst.x().index];
                    LIROperand index = ops[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isRegister();

                    LIROperand src = null;
                    if (index.isConstant() && index.kind == CiKind.Int) {
                        LIRConstant constantDisplacement = (LIRConstant) index;
                        src = new LIRAddress((LIRLocation) pointer, LIROperandFactory.IllegalLocation, scale, constantDisplacement.asInt() << scale.toInt() + displacement, inst.kind);
                    } else {
                        src = new LIRAddress((LIRLocation) pointer, (LIRLocation) index, scale, displacement, inst.kind);
                    }

                    moveOp(src, result, inst.kind, null, false);
                    break;
                }

                case PointerStoreDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    if (addressInformation.canTrap && xir.info != null) {
                        addCallInfoHere(xir.info);
                    }

                    LIRAddress.Scale scale = (addressInformation.scaling == null) ? Scale.Times1 : Scale.fromInt(((LIRConstant) ops[addressInformation.scaling.getIndex()]).asInt());
                    int displacement = (addressInformation.offset == null) ? 0 : ((LIRConstant) ops[addressInformation.offset.getIndex()]).asInt();

                    LIROperand value = ops[inst.z().index];
                    LIROperand pointer = ops[inst.x().index];
                    LIROperand index = ops[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isRegister();

                    LIROperand dst;
                    if (index.isConstant() && index.kind == CiKind.Int) {
                        LIRConstant constantDisplacement = (LIRConstant) index;
                        dst = new LIRAddress((LIRLocation) pointer, LIROperandFactory.IllegalLocation, scale, constantDisplacement.asInt() << scale.toInt() + displacement, inst.kind);
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
                    RegisterOrConstant[] args = new RegisterOrConstant[inst.arguments.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = asRegisterOrConstant(ops[inst.arguments[i].index]);
                    }
                    int infoPos = masm.callGlobalStub(stubId, this.compilation, xir.info, result, args);
                    compilation.addCallInfo(infoPos, xir.info);
                    break;

                case CallRuntime:
                    CiKind[] signature = new CiKind[inst.arguments.length];
                    for (int i = 0; i < signature.length; i++) {
                        signature[i] = inst.arguments[i].kind;
                    }

                    CallingConvention cc = this.frameMap().runtimeCallingConvention(signature);
                    for (int i = 0; i < inst.arguments.length; i++) {
                        LIROperand argumentLocation = cc.operands[i];
                        LIROperand argumentSourceLocation = ops[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            moveOp(argumentSourceLocation, argumentLocation, argumentLocation.kind, null, false);
                        }
                    }

                    RiMethod method = (RiMethod) inst.extra;
                    masm.call(method, new boolean[this.frameMap().frameSize() / compilation.target.arch.wordSize]);

                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        // (tw) remove this hack!
                        CiKind kind = CiKind.Long;
                        CiRegister register = this.compilation.target.config.getReturnRegister(inst.result.kind);
                        LIROperand resultLocation = LIROperandFactory.doubleLocation(kind, register, register);
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
        if (pointer.isConstant()) {
            LIROperand newPointerOperand = LIROperandFactory.scratch(pointer.kind, compilation.target);
            moveOp(pointer, newPointerOperand, pointer.kind, null, false);
            return newPointerOperand;
        }

        assert pointer.isRegister();
        return pointer;
    }

    private void emitXirCompare(XirInstruction inst, LIRCondition lirCondition, Condition condition, LIROperand[] ops, Label label) {
        LIROperand x = ops[inst.x().index];
        LIROperand y = ops[inst.y().index];
        compOp(lirCondition, x, y, null);
        masm.jcc(condition, label);
    }
}
