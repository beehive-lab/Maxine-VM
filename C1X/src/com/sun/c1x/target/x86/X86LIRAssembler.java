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

import com.sun.c1x.Bailout;
import com.sun.c1x.C1XCompilation;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.asm.*;
import com.sun.c1x.asm.Address.ScaleFactor;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiMethodData;
import com.sun.c1x.ci.CiRuntimeCall;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.CodeStub;
import com.sun.c1x.stub.MonitorAccessStub;
import com.sun.c1x.stub.MonitorExitStub;
import com.sun.c1x.stub.PatchingStub;
import com.sun.c1x.target.Register;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.value.ValueStack;

import java.util.List;

public class X86LIRAssembler extends LIRAssembler {

    private static final long NULLWORD = 0;
    private static final Register ICKlass = X86Register.rax;
    private static final Register SYNCHeader = X86Register.rax;
    private static final Register SHIFTCount = X86Register.rcx;

    private static final int FloatConstantAlignment = 16;
    private static final long FloatSignFlip = 0x8000000080000000L;
    private static final long DoubleSignFlip = 0x8000000000000000L;
    private static final long DoubleSignMask = 0x7FFFFFFFFFFFFFFFL;

    X86MacroAssembler masm;

    final int callStubSize;
    private final int exceptionHandlerSize;
    final int deoptHandlerSize;
    private final int wordSize;
    private final int referenceSize;
    private final Register rscratch1;

    public X86LIRAssembler(C1XCompilation compilation) {
        super(compilation);

        assert (compilation.masm() instanceof X86MacroAssembler);
        masm = (X86MacroAssembler) compilation.masm();

        wordSize = compilation.target.arch.wordSize;
        referenceSize = compilation.target.referenceSize;
        exceptionHandlerSize = 175;
        rscratch1 = X86FrameMap.rscratch1(compilation.target.arch);
        if (compilation.target.arch.is64bit()) {
            callStubSize = 28;
            deoptHandlerSize = 17;
        } else {
            callStubSize = 15;
            deoptHandlerSize = 10;
        }
    }

    private X86MacroAssembler masm() {
        return masm;
    }

    @Override
    protected void set_24bitFPU() {

        masm().fldcw(new RuntimeAddress(CiRuntimeCall.FpuCntrlWrd_24));

    }

    @Override
    protected void resetFPU() {
        masm().fldcw(new RuntimeAddress(CiRuntimeCall.FpuCntrlWrdStd));
    }

    @Override
    protected void fpop() {
        masm().fpop();
    }

    @Override
    protected void fxch(int i) {
        masm().fxch(i);
    }

    @Override
    protected void fld(int i) {
        masm().fldS(i);
    }

    @Override
    protected void ffree(int i) {
        masm().ffree(i);
    }

    @Override
    protected void breakpoint() {
        masm().int3();
    }

    protected static Register asRegister(LIROperand opr) {
        return opr.asRegister();
    }

    @Override
    protected void push(LIROperand opr) {
        if (opr.isSingleCpu()) {
            masm().pushReg(asRegister(opr));
        } else if (opr.isDoubleCpu()) {
            if (!compilation.target.arch.is64bit()) {
                masm().pushReg(opr.asRegisterHi());
            }
            masm().pushReg(opr.asRegisterLo());
        } else if (opr.isStack()) {
            masm().pushAddr(frameMap().addressForSlot(opr.singleStackIx()));
        } else if (opr.isConstant()) {
            LIRConstant constOpr = opr.asConstantPtr();
            if (constOpr.type() == BasicType.Object) {
                masm().pushOop(constOpr.asJobject());
            } else if (constOpr.type() == BasicType.Int) {
                masm().pushJint(constOpr.asInt());
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void pop(LIROperand opr) {
        if (opr.isSingleCpu()) {
            masm().popReg(opr.asRegister());
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private boolean isLiteralAddress(LIRAddress addr) {
        return addr.base().isIllegal() && addr.index().isIllegal();
    }

    private Address asAddress(LIRAddress addr) {
        return asAddress(addr, X86FrameMap.rscratch1(compilation.target.arch));
    }

    private Address asAddress(LIRAddress addr, Register tmp) {
        if (addr.base().isIllegal()) {
            assert addr.index().isIllegal() : "must be illegal too";
            AddressLiteral laddr = new AddressLiteral(addr.displacement(), RelocInfo.Type.none);
            if (!masm().reachable(laddr)) {
                masm().movptr(tmp, laddr.addr());
                Address res = new Address(tmp, 0);
                return res;
            } else {
                return masm().asAddress(laddr);
            }
        }

        Register base = addr.base().asPointerRegister(compilation.target.arch);

        if (addr.index().isIllegal()) {
            return new Address(base, addr.displacement());
        } else if (addr.index().isCpuRegister()) {
            Register index = addr.index().asPointerRegister(compilation.target.arch);
            return new Address(base, index, Address.ScaleFactor.fromInt(addr.scale().ordinal()), addr.displacement());
        } else if (addr.index().isConstant()) {
            long addrOffset = (addr.index().asConstantPtr().asInt() << addr.scale().ordinal()) + addr.displacement();
            assert X86Assembler.isSimm32(addrOffset) : "must be";

            return new Address(base, addrOffset);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    LIROperand receiverOpr() {
        return X86FrameMap.asPointerOpr(compilation.runtime.javaCallingConventionReceiverRegister(), compilation.target.arch);
    }

    LIROperand incomingReceiverOpr() {
        return receiverOpr();
    }

    private LIROperand osrBufferPointer() {
        return X86FrameMap.asPointerOpr(receiverOpr().asRegister(), compilation.target.arch);
    }

    @Override
    protected void emitLabel(LIRLabel op) {
        masm.bind(op.label());
    }

    @Override
    protected void osrEntry() {
        offsets().setValue(CodeOffsets.Entries.OSREntry, codeOffset());
        BlockBegin osrEntry = compilation.osrEntry();
        ValueStack entryState = osrEntry.state();
        int numberOfLocks = entryState.locksSize();

        // we jump here if osr happens with the interpreter
        // state set up to continue at the beginning of the
        // loop that triggered osr - in particular, we have
        // the following registers setup:
        //
        // rcx: osr buffer
        //

        // build frame
        masm().buildFrame(initialFrameSizeInBytes());

        // OSR buffer is
        //
        // locals[nlocals-1..0]
        // monitors[0..numberOfLocks]
        //
        // locals is a direct copy of the interpreter frame so in the osr buffer
        // so first slot in the local array is the last local from the interpreter
        // and last slot is local[0] (receiver) from the interpreter
        //
        // Similarly with locks. The first lock slot in the osr buffer is the nth lock
        // from the interpreter frame, the nth lock slot in the osr buffer is 0th lock
        // in the interpreter frame (the method lock if a sync method)

        // Initialize monitors in the compiled activation.
        // rcx: pointer to osr buffer
        //
        // All other registers are dead at this point and the locals will be
        // copied into place by code emitted in the IR.

        Register osrBuf = osrBufferPointer().asPointerRegister(compilation.target.arch);

        assert compilation.runtime.interpreterFrameMonitorSize() == compilation.runtime.basicObjectLockSize() : "adjust code below";
        int monitorOffset = Util.safeToInt(compilation.target.arch.wordSize * method().maxLocals() + (compilation.runtime.basicObjectLockSize() * compilation.target.arch.wordSize) *
                        (numberOfLocks - 1));
        for (int i = 0; i < numberOfLocks; i++) {
            int slotOffset = Util.safeToInt(monitorOffset - ((i * compilation.runtime.basicObjectLockSize()) * compilation.target.arch.wordSize));

            if (C1XOptions.GenerateAssertionCode) {
                Label l = new Label();
                masm().cmpptr(new Address(osrBuf, slotOffset + compilation.runtime.basicObjectLockOffsetInBytes()), (int) NULLWORD);
                masm().jcc(X86Assembler.Condition.notZero, l);
                masm().stop("locked object is null");
                masm().bind(l);
            }
            masm().movptr(X86Register.rbx, new Address(osrBuf, slotOffset + compilation.runtime.basicObjectLockOffsetInBytes()));
            masm().movptr(frameMap().addressForMonitorLock(i), X86Register.rbx);
            masm().movptr(X86Register.rbx, new Address(osrBuf, slotOffset + compilation.runtime.basicObjectObjOffsetInBytes()));
            masm().movptr(frameMap().addressForMonitorObject(i), X86Register.rbx);
        }
    }

    @Override
    protected int checkIcache() {
        Register receiver = this.receiverOpr().asRegister();
        int icCmpSize = 9;
        if (compilation.target.arch.is64bit()) {
            icCmpSize = 10;
        }

        // TODO: Check why icCmpSize is 9 !!
        icCmpSize = 9;

        if (!C1XOptions.VerifyOops) {
            // insert some nops so that the verified entry point is aligned on CodeEntryAlignment
            while ((masm().offset() + icCmpSize) % compilation.target.codeAlignment != 0) {
                masm().nop();
            }
        }
        int offset = masm().offset();
        masm().inlineCacheCheck(receiver, ICKlass);
        assert masm().offset() % compilation.target.codeAlignment == 0 || C1XOptions.VerifyOops : "alignment must be correct";
        if (C1XOptions.VerifyOops) {
            // force alignment after the cache check.
            // It's been verified to be aligned if !VerifyOops
            masm().align(compilation.target.codeAlignment);
        }
        return offset;
    }

    private void monitorexit(LIROperand objOpr, LIROperand lockOpr, Register newHdr, int monitorNo, Register exception) {
        if (exception.isValid()) {
            // preserve exception
            // note: the monitorExit runtime call is a leaf routine
            // and cannot block => no GC can happen
            // The slow case (MonitorAccessStub) uses the first two stack slots
            // ([esp+0] and [esp+4]), therefore we store the exception at [esp+8]
            masm().movptr(new Address(X86Register.rsp, 2 * compilation.target.arch.wordSize), exception);
        }

        Register objReg = objOpr.asRegister();
        Register lockReg = lockOpr.asRegister();

        // setup registers (lockReg must be rax, for lockObject)
        assert objReg != SYNCHeader && lockReg != SYNCHeader : "rax :  must be available here";
        Register hdr = lockReg;
        assert newHdr == SYNCHeader : "wrong register";
        lockReg = newHdr;
        // compute pointer to BasicLock
        Address lockAddr = frameMap().addressForMonitorLock(monitorNo);
        masm().lea(lockReg, lockAddr);
        // unlock object
        MonitorAccessStub slowCase = new MonitorExitStub(lockOpr, true, monitorNo);
        // slowCaseStubs.append(slowCase);
        // temporary fix: must be created after exceptionhandler, therefore as call stub
        slowCaseStubs.add(slowCase);
        if (C1XOptions.UseFastLocking) {
            // try inlined fast unlocking first, revert to slow locking if it fails
            // note: lockReg points to the displaced header since the displaced header offset is 0!
            assert compilation.runtime.basicLockDisplacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            masm().unlockObject(hdr, objReg, lockReg, slowCase.entry);
        } else {
            // always do slow unlocking
            // note: the slow unlocking code could be inlined here, however if we use
            // slow unlocking, speed doesn't matter anyway and this solution is
            // simpler and requires less duplicated code - additionally, the
            // slow unlocking code is the same in either case which simplifies
            // debugging
            masm().jmp(slowCase.entry);
        }
        // done
        masm().bind(slowCase.continuation);

        if (exception.isValid()) {
            // restore exception
            masm().movptr(exception, new Address(X86Register.rsp, 2 * compilation.target.arch.wordSize));
        }
    }

    @Override
    protected int initialFrameSizeInBytes() {
        // if rounding, must let FrameMap know!

        // The frameMap records size in slots (32bit word)

        // subtract two words to account for return address and link
        return (frameMap().framesize() - (2 * (wordSize / FrameMap.spillSlotSizeInBytes))) * FrameMap.spillSlotSizeInBytes;
    }

    @Override
    public void emitExceptionHandler() {
        // if the last instruction is a call (typically to do a throw which
        // is coming at the end after block reordering) the return address
        // must still point into the code area in order to avoid assert on
        // failures when searching for the corresponding bci => add a nop
        // (was bug 5/14/1999 - gri)

        masm().nop();

        // generate code for exception handler
        // TODO: Check with what to replace this!
// Pointer handlerBase = masm().startAStub(exceptionHandlerSize);
// if (handlerBase == null) {
// // not enough space left for the handler
// throw new Bailout("exception handler overflow");
// }

        int offset = codeOffset();

        compilation.offsets().setValue(CodeOffsets.Entries.Exceptions, codeOffset());

        // if the method does not have an exception handler : then there is
        // no reason to search for one
        if (compilation.hasExceptionHandlers() || compilation.runtime.jvmtiCanPostExceptions()) {
            // the exception oop and pc are in rax : and rdx
            // no other registers need to be preserved : so invalidate them
            masm().invalidateRegisters(false, true, true, false, true, true);

            // check that there is really an exception
            masm().verifyNotNullOop(X86Register.rax);

            // search an exception handler (rax: exception oop, rdx: throwing pc)
            masm().call(new RuntimeAddress(CiRuntimeCall.HandleExceptionNofpu));

            // if the call returns here : then the exception handler for particular
            // exception doesn't exist . unwind activation and forward exception to caller
        }

        // the exception oop is in rax :
        // no other registers need to be preserved : so invalidate them
        masm().invalidateRegisters(false, true, true, true, true, true);

        // check that there is really an exception
        masm().verifyNotNullOop(X86Register.rax);

        // unlock the receiver/klass if necessary
        // rax : : exception
        CiMethod method = compilation.method();
        if (method.isSynchronized() && C1XOptions.GenerateSynchronizationCode) {
            monitorexit(X86FrameMap.rbxOopOpr, X86FrameMap.rcxOpr, SYNCHeader, 0, X86Register.rax);
        }

        // unwind activation and forward exception to caller
        // rax : : exception
        masm().jump(new RuntimeAddress(CiRuntimeCall.UnwindException));

        assert codeOffset() - offset <= exceptionHandlerSize : "overflow";

        // masm().endAStub();
    }

    // TODO: Check if emit_string_compare is used somewhere?

    @Override
    protected void returnOp(LIROperand result) {

        assert result.isIllegal() || !result.isSingleCpu() || result.asRegister() == X86Register.rax : "word returns are in rax : ";
        if (!result.isIllegal() && result.isFloatKind() && !result.isXmmRegister()) {
            assert result.asRegisterLo() == X86Register.fpu0 : "result must already be on TOS";
        }

        // Pop the stack before the safepoint code
        masm().leave();

        // TODO: Add Safepoint polling at return!
        // Note: we do not need to round double result; float result has the right precision
        // the poll sets the condition code, but no data registers
        // AddressLiteral pollingPage = new AddressLiteral(compilation.runtime.getPollingPage() +
        // (C1XOptions.SafepointPollOffset % compilation.runtime.vmPageSize()), RelocInfo.Type.pollReturnType);

        // NOTE: the requires that the polling page be reachable else the reloc
        // goes to the movq that loads the address and not the faulting instruction
        // which breaks the signal handler code

        // lir().test32(X86Register.rax, pollingPage);

        masm().ret(0);
    }

    @Override
    protected void safepointPoll(LIROperand tmp, CodeEmitInfo info) {
        // TODO: Add safepoint polling
// AddressLiteral pollingPage = new ExternalAddress(compilation.runtime.getPollingPage() +
        // (C1XOptions.SafepointPollOffset % compilation.runtime.vmPageSize()), RelocInfo.Type.pollType);
//
// if (info != null) {
// addDebugInfoForBranch(info);
// } else {
// throw Util.shouldNotReachHere();
// }
//
// int offset = masm().offset();
//
// // NOTE: the requires that the polling page be reachable else the reloc
// // goes to the movq that loads the address and not the faulting instruction
// // which breaks the signal handler code
//
// masm().test32(X86Register.rax, pollingPage);
    }

    private void moveRegs(Register fromReg, Register toReg) {
        if (fromReg != toReg) {
            masm().mov(toReg, fromReg);
        }
    }

    private void swapReg(Register a, Register b) {
        masm().xchgptr(a, b);
    }

    private void jobject2regWithPatching(Register reg, CodeEmitInfo info) {
        Object o = null;
        PatchingStub patch = new PatchingStub(masm, PatchingStub.PatchID.LoadKlassId);
        masm().movoop(reg, o);
        patchingEpilog(patch, LIRPatchCode.PatchNormal, reg, info);
    }

    @Override
    protected void const2reg(LIROperand src, LIROperand dest, LIRPatchCode patchCode, CodeEmitInfo info) {
        assert src.isConstant() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";
        LIRConstant c = src.asConstantPtr();

        switch (c.type()) {
            case Int: {
                assert patchCode == LIRPatchCode.PatchNone : "no patching handled here";
                masm().movl(dest.asRegister(), c.asInt());
                break;
            }

            case Long: {
                assert patchCode == LIRPatchCode.PatchNone : "no patching handled here";
                if (compilation.target.arch.is64bit()) {
                    masm().movptr(dest.asRegisterLo(), c.asLong());
                } else {

                    masm().movptr(dest.asRegisterLo(), c.asIntLo());
                    masm().movptr(dest.asRegisterHi(), c.asIntHi());
                }
                break;
            }

            case Object: {
                if (patchCode != LIRPatchCode.PatchNone) {
                    jobject2regWithPatching(dest.asRegister(), info);
                } else {
                    masm().movoop(dest.asRegister(), c.asJobject());
                }
                break;
            }

            case Float: {
                if (dest.isSingleXmm()) {
                    if (c.isZeroFloat()) {
                        masm().xorps(asXmmFloatReg(dest), asXmmFloatReg(dest));
                    } else {
                        masm().movflt(asXmmFloatReg(dest), new InternalAddress(masm().floatConstant(c.asJfloat())));
                    }
                } else {
                    assert dest.isSingleFpu() : "must be";
                    assert dest.fpuRegnr() == 0 : "dest must be TOS";
                    if (c.isZeroFloat()) {
                        masm().fldz();
                    } else if (c.isOneFloat()) {
                        masm().fld1();
                    } else {
                        masm().fldS(new InternalAddress(masm().floatConstant(c.asJfloat())));
                    }
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    if (c.isZeroDouble()) {
                        masm().xorpd(asXmmDoubleReg(dest), asXmmDoubleReg(dest));
                    } else {
                        masm().movdbl(asXmmDoubleReg(dest), new InternalAddress(masm().doubleConstant(c.asJdouble())));
                    }
                } else {
                    assert dest.isDoubleFpu() : "must be";
                    assert dest.asRegisterLo() == X86Register.fpu0 : "dest must be TOS";
                    if (c.isZeroDouble()) {
                        masm().fldz();
                    } else if (c.isOneDouble()) {
                        masm().fld1();
                    } else {
                        masm().fldD(new InternalAddress(masm().doubleConstant(c.asJdouble())));
                    }
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
        LIRConstant c = src.asConstantPtr();

        switch (c.type()) {
            case Int: // fall through
            case Float:
                masm().movl(frameMap().addressForSlot(dest.singleStackIx()), c.asIntBits());
                break;

            case Object:
                masm().movoop(frameMap().addressForSlot(dest.singleStackIx()), c.asJobject());
                break;

            case Long: // fall through
            case Double:

                if (compilation.target.arch.is64bit()) {
                    masm().movptr(frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes), c.asLongBits());
                } else {
                    masm().movptr(frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes), c.asIntLoBits());
                    masm().movptr(frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes), c.asIntHiBits());
                }
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void const2mem(LIROperand src, LIROperand dest, BasicType type, CodeEmitInfo info) {
        assert src.isConstant() : "should not call otherwise";
        assert dest.isAddress() : "should not call otherwise";
        LIRConstant c = src.asConstantPtr();
        LIRAddress addr = dest.asAddressPtr();

        int nullCheckHere = codeOffset();
        switch (type) {
            case Int: // fall through
            case Float:
                masm().movl(asAddress(addr), c.asIntBits());
                break;

            case Object: // fall through
                if (c.asJobject() == null) {
                    masm().movptr(asAddress(addr), NULLWORD);
                } else {
                    if (isLiteralAddress(addr)) {
                        masm().movoop(asAddress(addr, Register.noreg), c.asJobject());
                        throw Util.shouldNotReachHere();
                    } else {
                        masm().movoop(asAddress(addr), c.asJobject());
                    }
                }
                break;

            case Long: // fall through
            case Double:

                if (compilation.target.arch.is64bit()) {
                    if (isLiteralAddress(addr)) {
                        masm().movptr(asAddress(addr, X86FrameMap.r15thread), c.asLongBits());
                        throw Util.shouldNotReachHere();
                    } else {
                        masm().movptr(X86Register.r10, c.asLongBits());
                        nullCheckHere = codeOffset();
                        masm().movptr(asAddressLo(addr), X86Register.r10);
                    }
                } else {
                    // Always reachable in 32bit so this doesn't produce useless move literal
                    masm().movptr(asAddressHi(addr), c.asIntHiBits());
                    masm().movptr(asAddressLo(addr), c.asIntLoBits());
                }
                break;

            case Boolean: // fall through
            case Byte:
                masm().movb(asAddress(addr), c.asInt() & 0xFF);
                break;

            case Char: // fall through
            case Short:
                masm().movw(asAddress(addr), c.asInt() & 0xFFFF);
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
                if (src.type() == BasicType.Long) {
                    // Can do LONG . OBJECT
                    moveRegs(src.asRegisterLo(), dest.asRegister());
                    return;
                }
            }
            assert src.isSingleCpu() : "must match";
            if (src.type() == BasicType.Object) {
                masm().verifyOop(src.asRegister());
            }
            moveRegs(src.asRegister(), dest.asRegister());

        } else if (dest.isDoubleCpu()) {
            if (compilation.target.arch.is64bit()) {
                if (src.type() == BasicType.Object) {
                    // Surprising to me but we can see move of a long to tObject
                    masm().verifyOop(src.asRegister());
                    moveRegs(src.asRegister(), dest.asRegisterLo());
                    return;
                }
            }
            assert src.isDoubleCpu() : "must match";
            Register fLo = src.asRegisterLo();
            Register fHi = src.asRegisterHi();
            Register tLo = dest.asRegisterLo();
            Register tHi = dest.asRegisterHi();
            if (compilation.target.arch.is64bit()) {
                assert fHi == fLo : "must be same";
                assert tHi == tLo : "must be same";
                moveRegs(fLo, tLo);
            } else {
                assert fLo != fHi && tLo != tHi : "invalid register allocation";

                if (fLo == tHi && fHi == tLo) {
                    swapReg(fLo, fHi);
                } else if (fHi == tLo) {
                    assert fLo != tHi : "overwriting register";
                    moveRegs(fHi, tHi);
                    moveRegs(fLo, tLo);
                } else {
                    assert fHi != tLo : "overwriting register";
                    moveRegs(fLo, tLo);
                    moveRegs(fHi, tHi);
                }
            }

            // special moves from fpu-register to xmm-register
            // necessary for method results
        } else if (src.isSingleXmm() && !dest.isSingleXmm()) {
            masm().movflt(new Address(X86Register.rsp, 0), asXmmFloatReg(src));
            masm().fldS(new Address(X86Register.rsp, 0));
        } else if (src.isDoubleXmm() && !dest.isDoubleXmm()) {
            masm().movdbl(new Address(X86Register.rsp, 0), asXmmDoubleReg(src));
            masm().fldD(new Address(X86Register.rsp, 0));
        } else if (dest.isSingleXmm() && !src.isSingleXmm()) {
            masm().fstpS(new Address(X86Register.rsp, 0));
            masm().movflt(asXmmFloatReg(dest), new Address(X86Register.rsp, 0));
        } else if (dest.isDoubleXmm() && !src.isDoubleXmm()) {
            masm().fstpD(new Address(X86Register.rsp, 0));
            masm().movdbl(asXmmDoubleReg(dest), new Address(X86Register.rsp, 0));

            // move between xmm-registers
        } else if (dest.isSingleXmm()) {
            assert src.isSingleXmm() : "must match";
            masm().movflt(asXmmFloatReg(dest), asXmmFloatReg(src));
        } else if (dest.isDoubleXmm()) {
            assert src.isDoubleXmm() : "must match";
            masm().movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(src));

            // move between fpu-registers (no instruction necessary because of fpu-stack)
        } else if (dest.isSingleFpu() || dest.isDoubleFpu()) {
            assert src.isSingleFpu() || src.isDoubleFpu() : "must match";
            assert src.equals(dest) : "currently should be nothing to do";
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2stack(LIROperand src, LIROperand dest, BasicType type, boolean popFpuStack) {
        assert src.isRegister() : "should not call otherwise";
        assert dest.isStack() : "should not call otherwise";

        if (src.isSingleCpu()) {
            Address dst = frameMap().addressForSlot(dest.singleStackIx());
            if (type == BasicType.Object) {
                masm().verifyOop(src.asRegister());
                masm().movptr(dst, src.asRegister());
            } else {
                masm().movl(dst, src.asRegister());
            }

        } else if (src.isDoubleCpu()) {
            if (!compilation.target.arch.is64bit()) {
                Address dstLO = frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes);
                masm().movptr(dstLO, src.asRegisterLo());
                Address dstHI = frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes);
                masm().movptr(dstHI, src.asRegisterHi());
            } else {
                Address dstLO = frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes);
                masm().movptr(dstLO, src.asRegisterLo());

            }

        } else if (src.isSingleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.singleStackIx());
            masm().movflt(dstAddr, asXmmFloatReg(src));

        } else if (src.isDoubleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.doubleStackIx());
            masm().movdbl(dstAddr, asXmmDoubleReg(src));

        } else if (src.isSingleFpu()) {
            assert src.fpuRegnr() == 0 : "argument must be on TOS";
            Address dstAddr = frameMap().addressForSlot(dest.singleStackIx());
            if (popFpuStack) {
                masm().fstpS(dstAddr);
            } else {
                masm().fstS(dstAddr);
            }

        } else if (src.isDoubleFpu()) {
            assert src.asRegisterLo() == X86Register.fpu0 : "argument must be on TOS";
            Address dstAddr = frameMap().addressForSlot(dest.doubleStackIx());
            if (popFpuStack) {
                masm().fstpD(dstAddr);
            } else {
                masm().fstD(dstAddr);
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2mem(LIROperand src, LIROperand dest, BasicType type, LIRPatchCode patchCode, CodeEmitInfo info, boolean popFpuStack, boolean unaligned) {
        LIRAddress toAddr = dest.asAddressPtr();
        PatchingStub patch = null;

        if (type == BasicType.Object) {
            masm().verifyOop(src.asRegister());
        }
        if (patchCode != LIRPatchCode.PatchNone) {
            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
            Address toa = asAddress(toAddr);
            assert toa.disp != 0 : "must have";
        }
        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        switch (type) {
            case Float: {
                if (src.isSingleXmm()) {
                    masm().movflt(asAddress(toAddr), asXmmFloatReg(src));
                } else {
                    assert src.isSingleFpu() : "must be";
                    assert src.fpuRegnr() == 0 : "argument must be on TOS";
                    if (popFpuStack) {
                        masm().fstpS(asAddress(toAddr));
                    } else {
                        masm().fstS(asAddress(toAddr));
                    }
                }
                break;
            }

            case Double: {
                if (src.isDoubleXmm()) {
                    masm().movdbl(asAddress(toAddr), asXmmDoubleReg(src));
                } else {
                    assert src.isDoubleFpu() : "must be";
                    assert src.asRegisterLo() == X86Register.fpu0 : "argument must be on TOS";
                    if (popFpuStack) {
                        masm().fstpD(asAddress(toAddr));
                    } else {
                        masm().fstD(asAddress(toAddr));
                    }
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
                if (compilation.target.arch.is64bit()) {
                    masm().movptr(asAddress(toAddr), src.asRegister());
                } else {
                    masm().movl(asAddress(toAddr), src.asRegister());

                }
                break;
            case Int:
                masm().movl(asAddress(toAddr), src.asRegister());
                break;

            case Long: {
                Register fromLo = src.asRegisterLo();
                Register fromHi = src.asRegisterHi();
                if (compilation.target.arch.is64bit()) {
                    masm().movptr(asAddressLo(toAddr), fromLo);
                } else {
                    Register base = toAddr.base().asRegister();
                    Register index = Register.noreg;
                    if (toAddr.index().isRegister()) {
                        index = toAddr.index().asRegister();
                    }
                    if (base == fromLo || index == fromLo) {
                        assert base != fromHi : "can't be";
                        assert index == Register.noreg || (index != base && index != fromHi) : "can't handle this";
                        masm().movl(asAddressHi(toAddr), fromHi);
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchHigh, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchLow;
                        }
                        masm().movl(asAddressLo(toAddr), fromLo);
                    } else {
                        assert index == Register.noreg || (index != base && index != fromLo) : "can't handle this";
                        masm().movl(asAddressLo(toAddr), fromLo);
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchLow, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchHigh;
                        }
                        masm().movl(asAddressHi(toAddr), fromHi);
                    }
                }
                break;
            }

            case Byte: // fall through
            case Boolean: {
                Register srcReg = src.asRegister();
                Address dstAddr = asAddress(toAddr);
                assert compilation.target.isP6() || srcReg.isByte() : "must use byte registers if not P6";
                masm().movb(dstAddr, srcReg);
                break;
            }

            case Char: // fall through
            case Short:
                masm().movw(asAddress(toAddr), src.asRegister());
                break;

            default:
                throw Util.shouldNotReachHere();
        }

        if (patchCode != LIRPatchCode.PatchNone) {
            patchingEpilog(patch, patchCode, toAddr.base().asRegister(), info);
        }
    }

    private static Register asXmmFloatReg(LIROperand src) {
        assert src.isXmmRegister();
        return src.asRegister();
    }

    @Override
    protected void stack2reg(LIROperand src, LIROperand dest, BasicType type) {
        assert src.isStack() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";

        if (dest.isSingleCpu()) {
            if (type == BasicType.Object) {
                masm().movptr(dest.asRegister(), frameMap().addressForSlot(src.singleStackIx()));
                masm().verifyOop(dest.asRegister());
            } else {
                masm().movl(dest.asRegister(), frameMap().addressForSlot(src.singleStackIx()));
            }

        } else if (dest.isDoubleCpu()) {
            Address srcAddrLO = frameMap().addressForSlot(src.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes);
            Address srcAddrHI = frameMap().addressForSlot(src.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes);
            masm().movptr(dest.asRegisterLo(), srcAddrLO);
            if (!compilation.target.arch.is64bit()) {
                masm().movptr(dest.asRegisterHi(), srcAddrHI);
            }

        } else if (dest.isSingleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.singleStackIx());
            masm().movflt(asXmmFloatReg(dest), srcAddr);

        } else if (dest.isDoubleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIx());
            masm().movdbl(asXmmDoubleReg(dest), srcAddr);

        } else if (dest.isSingleFpu()) {
            assert dest.fpuRegnr() == 0 : "dest must be TOS";
            Address srcAddr = frameMap().addressForSlot(src.singleStackIx());
            masm().fldS(srcAddr);

        } else if (dest.isDoubleFpu()) {
            assert dest.asRegisterLo() == X86Register.fpu0 : "dest must be TOS";
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIx());
            masm().fldD(srcAddr);

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void stack2stack(LIROperand src, LIROperand dest, BasicType type) {
        if (src.isSingleStack()) {
            if (type == BasicType.Object) {
                masm().pushptr(frameMap().addressForSlot(src.singleStackIx()));
                masm().popptr(frameMap().addressForSlot(dest.singleStackIx()));
            } else {
                masm().pushl(frameMap().addressForSlot(src.singleStackIx()));
                masm().popl(frameMap().addressForSlot(dest.singleStackIx()));
            }

        } else if (src.isDoubleStack()) {
            if (compilation.target.arch.is64bit()) {
                masm().pushptr(frameMap().addressForSlot(src.doubleStackIx()));
                masm().popptr(frameMap().addressForSlot(dest.doubleStackIx()));
            } else {
                masm().pushl(frameMap().addressForSlot(src.doubleStackIx(), 0));
                // push and pop the part at src + wordSize, adding wordSize for the previous push
                masm().pushl(frameMap().addressForSlot(src.doubleStackIx(), 2 * compilation.target.arch.wordSize));
                masm().popl(frameMap().addressForSlot(dest.doubleStackIx(), 2 * compilation.target.arch.wordSize));
                masm().popl(frameMap().addressForSlot(dest.doubleStackIx(), 0));
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected boolean assertFrameSize() {
        assert !compilation.target.arch.isX86() || masm.rspOffset() == 0 : "frame size should be fixed";
        return true;
    }

    @Override
    protected void mem2reg(LIROperand src, LIROperand dest, BasicType type, LIRPatchCode patchCode, CodeEmitInfo info, boolean unaligned) {
        assert src.isAddress() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";

        LIRAddress addr = src.asAddressPtr();
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
                    masm().xorptr(dest.asRegister(), dest.asRegister());
                }
                break;
        }

        PatchingStub patch = null;
        if (patchCode != LIRPatchCode.PatchNone) {
            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
            assert fromAddr.disp != 0 : "must have";
        }
        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        switch (type) {
            case Float: {
                if (dest.isSingleXmm()) {
                    masm().movflt(asXmmFloatReg(dest), fromAddr);
                } else {
                    assert dest.isSingleFpu() : "must be";
                    assert dest.fpuRegnr() == 0 : "dest must be TOS";
                    masm().fldS(fromAddr);
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    masm().movdbl(asXmmDoubleReg(dest), fromAddr);
                } else {
                    assert dest.isDoubleFpu() : "must be";
                    assert dest.asRegisterLo() == X86Register.fpu0 : "dest must be TOS";
                    masm().fldD(fromAddr);
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
                if (compilation.target.arch.is64bit()) {
                    masm().movptr(dest.asRegister(), fromAddr);
                } else {
                    masm().movl2ptr(dest.asRegister(), fromAddr);

                }
                break;
            case Int:
                // %%% could this be a movl? this is safer but longer instruction
                masm().movl2ptr(dest.asRegister(), fromAddr);
                break;

            case Long: {
                Register toLo = dest.asRegisterLo();
                Register toHi = dest.asRegisterHi();

                if (compilation.target.arch.is64bit()) {
                    masm().movptr(toLo, asAddressLo(addr));
                } else {
                    Register base = addr.base().asRegister();
                    Register index = Register.noreg;
                    if (addr.index().isRegister()) {
                        index = addr.index().asRegister();
                    }
                    if ((base == toLo && index == toHi) || (base == toHi && index == toLo)) {
                        // addresses with 2 registers are only formed as a result of
                        // array access so this code will never have to deal with
                        // patches or null checks.
                        assert info == null && patch == null : "must be";
                        masm().lea(toHi, asAddress(addr));
                        masm().movl(toLo, new Address(toHi, 0));
                        masm().movl(toHi, new Address(toHi, wordSize));
                    } else if (base == toLo || index == toLo) {
                        assert base != toHi : "can't be";
                        assert index == Register.noreg || (index != base && index != toHi) : "can't handle this";
                        masm().movl(toHi, asAddressHi(addr));
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchHigh, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchLow;
                        }
                        masm().movl(toLo, asAddressLo(addr));
                    } else {
                        assert index == Register.noreg || (index != base && index != toLo) : "can't handle this";
                        masm().movl(toLo, asAddressLo(addr));
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchLow, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchHigh;
                        }
                        masm().movl(toHi, asAddressHi(addr));
                    }
                }
                break;
            }

            case Boolean: // fall through
            case Byte: {
                Register destReg = dest.asRegister();
                assert compilation.target.isP6() || destReg.isByte() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm().movsbl(destReg, fromAddr);
                } else {
                    masm().movb(destReg, fromAddr);
                    masm().shll(destReg, 24);
                    masm().sarl(destReg, 24);
                }
                // These are unsigned so the zero extension on 64bit is just what we need
                break;
            }

            case Char: {
                Register destReg = dest.asRegister();
                assert compilation.target.isP6() || destReg.isByte() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm().movzwl(destReg, fromAddr);
                } else {
                    masm().movw(destReg, fromAddr);
                }
                // This is unsigned so the zero extension on 64bit is just what we need
                // lir(). movl2ptr(destReg, destReg);
                break;
            }

            case Short: {
                Register destReg = dest.asRegister();
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    masm().movswl(destReg, fromAddr);
                } else {
                    masm().movw(destReg, fromAddr);
                    masm().shll(destReg, 16);
                    masm().sarl(destReg, 16);
                }
                // Might not be needed in 64bit but certainly doesn't hurt (except for code size)
                masm().movl2ptr(destReg, destReg);
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }

        if (patch != null) {
            patchingEpilog(patch, patchCode, addr.base().asRegister(), info);
        }

        if (type == BasicType.Object) {
            masm().verifyOop(dest.asRegister());
        }
    }

    @Override
    protected void prefetchr(LIROperand src) {
        LIRAddress addr = src.asAddressPtr();
        Address fromAddr = asAddress(addr);

        if (compilation.target.supportsSSE()) {
            switch (C1XOptions.ReadPrefetchInstr) {
                case 0:
                    masm().prefetchnta(fromAddr);
                    break;
                case 1:
                    masm().prefetcht0(fromAddr);
                    break;
                case 2:
                    masm().prefetcht2(fromAddr);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (compilation.target.supports3DNOW()) {
            masm().prefetchr(fromAddr);
        }
    }

    // TODO: Who uses this?
    public void prefetchw(LIROperand src) {
        LIRAddress addr = src.asAddressPtr();
        Address fromAddr = asAddress(addr);

        if (compilation.target.supportsSSE()) {
            switch (C1XOptions.AllocatePrefetchInstr) {
                case 0:
                    masm().prefetchnta(fromAddr);
                    break;
                case 1:
                    masm().prefetcht0(fromAddr);
                    break;
                case 2:
                    masm().prefetcht2(fromAddr);
                    break;
                case 3:
                    masm().prefetchw(fromAddr);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (compilation.target.supports3DNOW()) {
            masm().prefetchw(fromAddr);
        }
    }

    @Override
    protected void emitOp3(LIROp3 op) {
        switch (op.code()) {
            case Idiv:
            case Irem:
                arithmeticIdiv(op.code(), op.opr1(), op.opr2(), op.opr3(), op.result(), op.info());
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
            if (op.info() != null) {
                addDebugInfoForBranch(op.info());
            }
            masm().jmp(op.label());
        } else {
            X86Assembler.Condition acond = X86Assembler.Condition.zero;
            if (op.code() == LIROpcode.CondFloatBranch) {
                assert op.ublock() != null : "must have unordered successor";
                masm().jcc(X86Assembler.Condition.parity, op.ublock().label());
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
            masm().jcc(acond, (op.label()));
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        LIROperand src = op.inOpr();
        LIROperand dest = op.resultOpr();

        switch (op.bytecode()) {
            case Bytecodes.I2L:
                if (compilation.target.arch.is64bit()) {
                    masm().movl2ptr(dest.asRegisterLo(), src.asRegister());
                } else {
                    moveRegs(src.asRegister(), dest.asRegisterLo());
                    moveRegs(src.asRegister(), dest.asRegisterHi());
                    masm().sarl(dest.asRegisterHi(), 31);
                }
                break;

            case Bytecodes.L2I:
                moveRegs(src.asRegisterLo(), dest.asRegister());
                break;

            case Bytecodes.I2B:
                moveRegs(src.asRegister(), dest.asRegister());
                masm().signExtendByte(dest.asRegister());
                break;

            case Bytecodes.I2C:
                moveRegs(src.asRegister(), dest.asRegister());
                masm().andl(dest.asRegister(), 0xFFFF);
                break;

            case Bytecodes.I2S:
                moveRegs(src.asRegister(), dest.asRegister());
                masm().signExtendShort(dest.asRegister());
                break;

            case Bytecodes.F2D:
            case Bytecodes.D2F:
                if (dest.isSingleXmm()) {
                    masm().cvtsd2ss(asXmmFloatReg(dest), asXmmDoubleReg(src));
                } else if (dest.isDoubleXmm()) {
                    masm().cvtss2sd(asXmmDoubleReg(dest), asXmmFloatReg(src));
                } else {
                    assert src.equals(dest) : "register must be equal";
                    // do nothing (float result is rounded later through spilling)
                }
                break;

            case Bytecodes.I2F:
            case Bytecodes.I2D:
                if (dest.isSingleXmm()) {
                    masm().cvtsi2ssl(asXmmFloatReg(dest), src.asRegister());
                } else if (dest.isDoubleXmm()) {
                    masm().cvtsi2sdl(asXmmDoubleReg(dest), src.asRegister());
                } else {
                    assert dest.asRegisterLo() == X86Register.fpu0 : "result must be on TOS";
                    masm().movl(new Address(X86Register.rsp, 0), src.asRegister());
                    masm().fildS(new Address(X86Register.rsp, 0));
                }
                break;

            case Bytecodes.F2I:
            case Bytecodes.D2I:
                if (src.isSingleXmm()) {
                    masm().cvttss2sil(dest.asRegister(), asXmmFloatReg(src));
                } else if (src.isDoubleXmm()) {
                    masm().cvttsd2sil(dest.asRegister(), asXmmDoubleReg(src));
                } else {
                    assert src.asRegisterLo() == X86Register.fpu0 : "input must be on TOS";
                    masm().fldcw(new RuntimeAddress(CiRuntimeCall.FpuCntrlWrdTrunc));
                    masm().fistS(new Address(X86Register.rsp, 0));
                    masm().movl(dest.asRegister(), new Address(X86Register.rsp, 0));
                    masm().fldcw(new RuntimeAddress(CiRuntimeCall.FpuCntrlWrdStd));
                }

                // IA32 conversion instructions do not match JLS for overflow, underflow and NaN . fixup in stub
                assert op.stub() != null : "stub required";
                masm().cmpl(dest.asRegister(), 0x80000000);
                masm().jcc(X86Assembler.Condition.equal, op.stub().entry);
                masm().bind(op.stub().continuation);
                break;

            case Bytecodes.L2F:
            case Bytecodes.L2D:
                assert !dest.isXmmRegister() : "result in xmm register not supported (no SSE instruction present)";
                assert dest.asRegisterLo() == X86Register.fpu0 : "result must be on TOS";

                masm().movptr(new Address(X86Register.rsp, 0), src.asRegisterLo());
                if (!compilation.target.arch.is64bit()) {
                    masm().movl(new Address(X86Register.rsp, wordSize), src.asRegisterHi());
                }
                masm().fildD(new Address(X86Register.rsp, 0));
                // float result is rounded later through spilling
                break;

            case Bytecodes.F2L:
            case Bytecodes.D2L:
                assert !src.isXmmRegister() : "input in xmm register not supported (no SSE instruction present)";
                assert src.asRegisterLo() == X86Register.fpu0 : "input must be on TOS";
                assert dest == X86FrameMap.long0Opr(compilation.target.arch) : "runtime stub places result in these registers";

                // instruction sequence too long to inline it here
                masm().call(new RuntimeAddress(CiRuntimeCall.Fpu2longStub));
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitAllocObj(LIRAllocObj op) {
        if (op.isInitCheck()) {
            masm().cmpl(new Address(op.klass().asRegister(), compilation.runtime.initStateOffsetInBytes()), compilation.runtime.instanceKlassFullyInitialized());
            addDebugInfoForNullCheckHere(op.stub().info);
            masm().jcc(X86Assembler.Condition.notEqual, op.stub().entry);
        }
        masm().allocateObject(op.obj().asRegister(), op.tmp1().asRegister(), op.tmp2().asRegister(), op.headerSize(), op.obectSize(), op.klass().asRegister(), op.stub().entry);
        masm().bind(op.stub().continuation);
    }

    @Override
    protected void emitAllocArray(LIRAllocArray op) {
        if (C1XOptions.UseSlowPath || (!C1XOptions.UseFastNewObjectArray && op.type() == BasicType.Object) || (!C1XOptions.UseFastNewTypeArray && op.type() != BasicType.Object)) {
            masm().jmp(op.stub().entry);
        } else {
            Register len = op.length().asRegister();
            Register tmp1 = op.tmp1().asRegister();
            Register tmp2 = op.tmp2().asRegister();
            Register tmp3 = op.tmp3().asRegister();
            if (len == tmp1) {
                tmp1 = tmp3;
            } else if (len == tmp2) {
                tmp2 = tmp3;
            } else if (len == tmp3) {
                // everything is ok
            } else {
                masm().mov(tmp3, len);
            }
            masm().allocateArray(op.obj().asRegister(), len, tmp1, tmp2, compilation.runtime.arrayOopDescHeaderSize(op.type()),
                            Address.ScaleFactor.fromInt(compilation.runtime.arrayElementSize(op.type())), op.klass().asRegister(), op.stub().entry);
        }
        masm().bind(op.stub().continuation);
    }

    static void selectDifferentRegisters(Register preserve, Register extra, Register[] tmp1, Register[] tmp2) {
        if (tmp1[0] == preserve) {
            assert Register.assertDifferentRegisters(tmp1[0], tmp2[0], extra);
            tmp1[0] = extra;
        } else if (tmp2[0] == preserve) {
            Register.assertDifferentRegisters(tmp1[0], tmp2[0], extra);
            tmp2[0] = extra;
        }
        Register.assertDifferentRegisters(preserve, tmp1[0], tmp2[0]);
    }

    static void selectDifferentRegisters(Register preserve, Register extra, Register[] tmp1, Register[] tmp2, Register[] tmp3) {
        if (tmp1[0] == preserve) {
            Register.assertDifferentRegisters(tmp1[0], tmp2[0], tmp3[0], extra);
            tmp1[0] = extra;
        } else if (tmp2[0] == preserve) {
            Register.assertDifferentRegisters(tmp1[0], tmp2[0], tmp3[0], extra);
            tmp2[0] = extra;
        } else if (tmp3[0] == preserve) {
            Register.assertDifferentRegisters(tmp1[0], tmp2[0], tmp3[0], extra);
            tmp3[0] = extra;
        }
        Register.assertDifferentRegisters(preserve, tmp1[0], tmp2[0], tmp3[0]);
    }

    @Override
    protected void emitTypeCheck(LIRTypeCheck op) {

        // TODO: Make this work with Maxine while preserving the general semantics
        //if (C1XOptions.AvoidUnsupported) {
        //    throw new Bailout("Currently cannot emit a type check!");
        //}


        LIROpcode code = op.code();
        if (code == LIROpcode.StoreCheck) {
            Register value = op.object().asRegister();
            Register array = op.array().asRegister();
            Register kRInfo = op.tmp1().asRegister();
            Register klassRInfo = op.tmp2().asRegister();
            //Register rtmp1 = op.tmp3().asRegister();

            CodeStub stub = op.stub();
            Label done = new Label();
            masm().cmpptr(value, (int) NULLWORD);
            masm().jcc(X86Assembler.Condition.equal, done);
            addDebugInfoForNullCheckHere(op.infoForException());
            masm().movptr(kRInfo, new Address(array, compilation.runtime.klassOffsetInBytes()));
            masm().movptr(klassRInfo, new Address(value, compilation.runtime.klassOffsetInBytes()));

            // get instance klass
            masm().movptr(kRInfo, new Address(kRInfo, compilation.runtime.elementKlassOffsetInBytes()));

            // TODO: enable the fast path!
            // perform the fast part of the checking logic
            //masm().checkKlassSubtypeFastPath(klassRInfo, kRInfo, rtmp1, done, stub.entry, null, new RegisterOrConstant(-1));

            // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
            masm().push(klassRInfo);
            masm().push(kRInfo);
            masm().call(new RuntimeAddress(CiRuntimeCall.SlowSubtypeCheck));
            masm().pop(klassRInfo);
            masm().pop(kRInfo);
            // result is a boolean
            masm().cmpl(kRInfo, 0);
            masm().jcc(X86Assembler.Condition.equal, stub.entry);
            masm().bind(done);
        } else if (op.code() == LIROpcode.CheckCast) {
            // we always need a stub for the failure case.
            CodeStub stub = op.stub();
            Register obj = op.object().asRegister();
            Register kRInfo = op.tmp1().asRegister();
            Register klassRInfo = op.tmp2().asRegister();
            Register dst = op.result().asRegister();
            CiType k = op.klass();
            Register rtmp1 = Register.noreg;

            Label done = new Label();
            if (obj == kRInfo) {
                kRInfo = dst;
            } else if (obj == klassRInfo) {
                klassRInfo = dst;
            }
            if (k.isLoaded()) {
                // TODO: out params?!
                Register[] tmp1 = new Register[] {kRInfo};
                Register[] tmp2 = new Register[] {klassRInfo};
                selectDifferentRegisters(obj, dst, tmp1, tmp2);
                kRInfo = tmp1[0];
                klassRInfo = tmp2[0];
            } else {
                rtmp1 = op.tmp3().asRegister();
                Register[] tmp1 = new Register[] {kRInfo};
                Register[] tmp2 = new Register[] {klassRInfo};
                Register[] tmp3 = new Register[] {rtmp1};
                selectDifferentRegisters(obj, dst, tmp1, tmp2, tmp3);
                kRInfo = tmp1[0];
                klassRInfo = tmp2[0];
                rtmp1 = tmp3[0];
            }

            assert Register.assertDifferentRegisters(obj, kRInfo, klassRInfo);
            if (!k.isLoaded()) {
                jobject2regWithPatching(kRInfo, op.infoForPatch());
            } else {

                if (compilation.target.arch.is64bit()) {
                    masm().movoop(kRInfo, k);
                } else {
                    kRInfo = Register.noreg;
                }
            }
            assert obj != kRInfo : "must be different";
            masm().cmpptr(obj, (int) NULLWORD);
            if (op.profiledMethod() != null) {
                CiMethod method = op.profiledMethod();
                int bci = op.profiledBci();

                Label profileDone = new Label();
                masm().jcc(X86Assembler.Condition.notEqual, profileDone);
                // Object is null; update methodDataOop
                CiMethodData md = method.methodData();
                if (md == null) {
                    throw new Bailout("out of memory building methodDataOop");
                }
                // ciProfileData data = md.bciToData(bci);
                // assert data != null : "need data for checkcast";
                // assert data.isBitData() : "need BitData for checkcast";
                Register mdo = klassRInfo;
                masm().movoop(mdo, md);
                Address dataAddr = new Address(mdo, md.headerOffset(bci));
                int headerBits = compilation.runtime.methodDataNullSeenByteConstant(); // TODO: Check what this really
                // means!
                // DataLayout.flagMaskToHeaderMask(BitData.nullSeenByteConstant());
                masm().orl(dataAddr, headerBits);
                masm().jmp(done);
                masm().bind(profileDone);
            } else {
                masm().jcc(X86Assembler.Condition.equal, done);
            }
            masm().verifyOop(obj);

            if (op.isFastCheck()) {
                // get object classo
                // not a safepoint as obj null check happens earlier
                if (k.isLoaded()) {

                    if (compilation.target.arch.is64bit()) {
                        masm().cmpptr(kRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));
                    } else {
                        masm().cmpoop(new Address(obj, compilation.runtime.klassOffsetInBytes()), k);
                    }
                } else {
                    masm().cmpptr(kRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));

                }
                masm().jcc(X86Assembler.Condition.notEqual, stub.entry);
                masm().bind(done);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                masm().movptr(klassRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));
                if (k.isLoaded()) {
                    // See if we get an immediate positive hit
                    if (compilation.target.arch.is64bit()) {
                        masm().cmpptr(kRInfo, new Address(klassRInfo, k.superCheckOffset()));
                    } else {
                        masm().cmpoop(new Address(klassRInfo, k.superCheckOffset()), k);
                    }
                    if (Util.sizeofOopDesc() + compilation.runtime.secondarySuperCacheOffsetInBytes() != k.superCheckOffset()) {
                        masm().jcc(X86Assembler.Condition.notEqual, stub.entry);
                    } else {
                        // See if we get an immediate positive hit
                        masm().jcc(X86Assembler.Condition.equal, done);
                        // check for self
                        if (compilation.target.arch.is64bit()) {
                            masm().cmpptr(klassRInfo, kRInfo);
                        } else {
                            masm().cmpoop(klassRInfo, k);
                        }
                        masm().jcc(X86Assembler.Condition.equal, done);

                        masm().push(klassRInfo);
                        if (compilation.target.arch.is64bit()) {
                            masm().push(kRInfo);
                        } else {
                            masm().pushoop(k);
                        }
                        masm().call(new RuntimeAddress(CiRuntimeCall.SlowSubtypeCheck));
                        masm().pop(klassRInfo);
                        masm().pop(klassRInfo);
                        // result is a boolean
                        masm().cmpl(klassRInfo, 0);
                        masm().jcc(X86Assembler.Condition.equal, stub.entry);
                    }
                    masm().bind(done);
                } else {
                    // perform the fast part of the checking logic
                    masm().checkKlassSubtypeFastPath(klassRInfo, kRInfo, rtmp1, done, stub.entry, null, new RegisterOrConstant(-1));
                    // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                    masm().push(klassRInfo);
                    masm().push(kRInfo);
                    masm().call(new RuntimeAddress(CiRuntimeCall.SlowSubtypeCheck));
                    masm().pop(klassRInfo);
                    masm().pop(kRInfo);
                    // result is a boolean
                    masm().cmpl(kRInfo, 0);
                    masm().jcc(X86Assembler.Condition.equal, stub.entry);
                    masm().bind(done);
                }

            }
            if (dst != obj) {
                masm().mov(dst, obj);
            }
        } else if (code == LIROpcode.InstanceOf) {
            Register obj = op.object().asRegister();
            Register kRInfo = op.tmp1().asRegister();
            Register klassRInfo = op.tmp2().asRegister();
            Register dst = op.result().asRegister();
            CiType k = op.klass();

            Label done = new Label();
            Label zero = new Label();
            Label one = new Label();
            if (obj == kRInfo) {
                kRInfo = klassRInfo;
                klassRInfo = obj;
            }
            // patching may screw with our temporaries on sparc :
            // so let's do it before loading the class
            if (!k.isLoaded()) {
                jobject2regWithPatching(kRInfo, op.infoForPatch());
            } else {
                if (compilation.target.arch.is64bit()) {
                    masm().movoop(kRInfo, k);
                }
            }
            assert obj != kRInfo : "must be different";

            masm().verifyOop(obj);
            if (op.isFastCheck()) {
                masm().cmpptr(obj, (int) NULLWORD);
                masm().jcc(X86Assembler.Condition.equal, zero);
                // get object class
                // not a safepoint as obj null check happens earlier
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
                    masm().cmpoop(new Address(obj, compilation.runtime.klassOffsetInBytes()), k);
                    kRInfo = Register.noreg;
                } else {
                    masm().cmpptr(kRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));

                }
                masm().jcc(X86Assembler.Condition.equal, one);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                masm().cmpptr(obj, (int) NULLWORD);
                masm().jcc(X86Assembler.Condition.equal, zero);
                masm().movptr(klassRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
                    // See if we get an immediate positive hit
                    masm().cmpoop(new Address(klassRInfo, k.superCheckOffset()), k);
                    masm().jcc(X86Assembler.Condition.equal, one);
                    if (Util.sizeofOopDesc() + compilation.runtime.secondarySuperCacheOffsetInBytes() == k.superCheckOffset()) {
                        // check for self
                        masm().cmpoop(klassRInfo, k);
                        masm().jcc(X86Assembler.Condition.equal, one);
                        masm().push(klassRInfo);
                        masm().pushoop(k);
                        masm().call(new RuntimeAddress(CiRuntimeCall.SlowSubtypeCheck));
                        masm().pop(klassRInfo);
                        masm().pop(dst);
                        masm().jmp(done);
                    }
                } else {
                    // next block is unconditional if LP64:
                    assert dst != klassRInfo && dst != kRInfo : "need 3 registers";

                    // perform the fast part of the checking logic
                    masm().checkKlassSubtypeFastPath(klassRInfo, kRInfo, dst, one, zero, null, new RegisterOrConstant(-1));
                    // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                    masm().push(klassRInfo);
                    masm().push(kRInfo);
                    masm().call(new RuntimeAddress(CiRuntimeCall.SlowSubtypeCheck));
                    masm().pop(klassRInfo);
                    masm().pop(dst);
                    masm().jmp(done);
                }
            }
            masm().bind(zero);
            masm().xorptr(dst, dst);
            masm().jmp(done);
            masm().bind(one);
            masm().movptr(dst, 1);
            masm().bind(done);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        if (!compilation.target.arch.is64bit() && op.code() == LIROpcode.CasLong && compilation.target.supportsCx8()) {
            assert op.cmpValue().asRegisterLo() == X86Register.rax : "wrong register";
            assert op.cmpValue().asRegisterHi() == X86Register.rdx : "wrong register";
            assert op.newValue().asRegisterLo() == X86Register.rbx : "wrong register";
            assert op.newValue().asRegisterHi() == X86Register.rcx : "wrong register";
            Register addr = op.address().asRegister();
            if (compilation.runtime.isMP()) {
                masm().lock();
            }
            masm().cmpxchg8(new Address(addr, 0));

        } else if (op.code() == LIROpcode.CasInt || op.code() == LIROpcode.CasObj) {
            assert compilation.target.arch.is64bit() || op.address().isSingleCpu() : "must be single";
            Register addr = ((op.address().isSingleCpu() ? op.address().asRegister() : op.address().asRegisterLo()));
            Register newval = op.newValue().asRegister();
            Register cmpval = op.cmpValue().asRegister();
            assert cmpval == X86Register.rax : "wrong register";
            assert newval != null : "new val must be register";
            assert cmpval != newval : "cmp and new values must be in different registers";
            assert cmpval != addr : "cmp and addr must be in different registers";
            assert newval != addr : "new value and addr must be in different registers";
            if (compilation.runtime.isMP()) {
                masm().lock();
            }
            if (op.code() == LIROpcode.CasObj) {
                masm().cmpxchgptr(newval, new Address(addr, 0));
            } else if (op.code() == LIROpcode.CasInt) {
                masm().cmpxchgl(newval, new Address(addr, 0));
            } else if (compilation.target.arch.is64bit()) {
                masm().cmpxchgq(newval, new Address(addr, 0));
            }
        } else if (compilation.target.arch.is64bit() && op.code() == LIROpcode.CasLong) {
            Register addr = (op.address().isSingleCpu() ? op.address().asRegister() : op.address().asRegisterLo());
            Register newval = op.newValue().asRegisterLo();
            Register cmpval = op.cmpValue().asRegisterLo();
            assert cmpval == X86Register.rax : "wrong register";
            assert newval != null : "new val must be register";
            assert cmpval != newval : "cmp and new values must be in different registers";
            assert cmpval != addr : "cmp and addr must be in different registers";
            assert newval != addr : "new value and addr must be in different registers";
            if (compilation.runtime.isMP()) {
                masm().lock();
            }
            masm().cmpxchgq(newval, new Address(addr, 0));
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void cmove(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROperand result) {
        X86Assembler.Condition acond;
        X86Assembler.Condition ncond;
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

        if (opr1.isCpuRegister()) {
            reg2reg(opr1, result);
        } else if (opr1.isStack()) {
            stack2reg(opr1, result, result.type());
        } else if (opr1.isConstant()) {
            const2reg(opr1, result, LIRPatchCode.PatchNone, null);
        } else {
            throw Util.shouldNotReachHere();
        }

        if (compilation.target.supportsCmov() && !opr2.isConstant()) {
            // optimized version that does not require a branch
            if (opr2.isSingleCpu()) {
                assert opr2.asRegister() != result.asRegister() : "opr2 already overwritten by previous move";
                masm().cmov(ncond, result.asRegister(), opr2.asRegister());
            } else if (opr2.isDoubleCpu()) {
                assert opr2.cpuRegnrLo() != result.cpuRegnrLo() && opr2.cpuRegnrLo() != result.cpuRegnrHi() : "opr2 already overwritten by previous move";
                assert opr2.cpuRegnrHi() != result.cpuRegnrLo() && opr2.cpuRegnrHi() != result.cpuRegnrHi() : "opr2 already overwritten by previous move";
                masm().cmovptr(ncond, result.asRegisterLo(), opr2.asRegisterLo());
                if (!compilation.target.arch.is64bit()) {
                    masm().cmovptr(ncond, result.asRegisterHi(), opr2.asRegisterHi());
                }
            } else if (opr2.isSingleStack()) {
                masm().cmovl(ncond, result.asRegister(), frameMap().addressForSlot(opr2.singleStackIx()));
            } else if (opr2.isDoubleStack()) {
                masm().cmovptr(ncond, result.asRegisterLo(), frameMap().addressForSlot(opr2.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes));
                if (!compilation.target.arch.is64bit()) {
                    masm().cmovptr(ncond, result.asRegisterHi(), frameMap().addressForSlot(opr2.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes));
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            Label skip = new Label();
            masm().jcc(acond, skip);
            if (opr2.isCpuRegister()) {
                reg2reg(opr2, result);
            } else if (opr2.isStack()) {
                stack2reg(opr2, result, result.type());
            } else if (opr2.isConstant()) {
                const2reg(opr2, result, LIRPatchCode.PatchNone, null);
            } else {
                throw Util.shouldNotReachHere();
            }
            masm().bind(skip);
        }
    }

    @Override
    protected void arithOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dest, CodeEmitInfo info, boolean popFpuStack) {
        assert info == null : "should never be used :  idiv/irem and ldiv/lrem not handled by this method";

        if (left.isSingleCpu()) {
            assert left.equals(dest) : "left and dest must be equal";
            Register lreg = left.asRegister();

            if (right.isSingleCpu()) {
                // cpu register - cpu register
                Register rreg = right.asRegister();
                switch (code) {
                    case Add:
                        masm().addl(lreg, rreg);
                        break;
                    case Sub:
                        masm().subl(lreg, rreg);
                        break;
                    case Mul:
                        masm().imull(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isStack()) {
                // cpu register - stack
                Address raddr = frameMap().addressForSlot(right.singleStackIx());
                switch (code) {
                    case Add:
                        masm().addl(lreg, raddr);
                        break;
                    case Sub:
                        masm().subl(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isConstant()) {
                // cpu register - constant
                int c = right.asConstantPtr().asInt();
                switch (code) {
                    case Add: {
                        masm().increment(lreg, c);
                        break;
                    }
                    case Sub: {
                        masm().decrement(lreg, c);
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
            Register lregLo = left.asRegisterLo();
            Register lregHi = left.asRegisterHi();

            if (right.isDoubleCpu()) {
                // cpu register - cpu register
                Register rregLo = right.asRegisterLo();
                Register rregHi = right.asRegisterHi();
                assert compilation.target.arch.is64bit() || Register.assertDifferentRegisters(lregLo, lregHi, rregLo, rregHi);
                assert !compilation.target.arch.is64bit() || Register.assertDifferentRegisters(lregLo, rregLo);
                switch (code) {
                    case Add:
                        masm().addptr(lregLo, rregLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm().adcl(lregHi, rregHi);
                        }
                        break;
                    case Sub:
                        masm().subptr(lregLo, rregLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm().sbbl(lregHi, rregHi);
                        }
                        break;
                    case Mul:
                        if (compilation.target.arch.is64bit()) {
                            masm().imulq(lregLo, rregLo);
                        } else {
                            assert lregLo == X86Register.rax && lregHi == X86Register.rdx : "must be";
                            masm().imull(lregHi, rregLo);
                            masm().imull(rregHi, lregLo);
                            masm().addl(rregHi, lregHi);
                            masm().mull(rregLo);
                            masm().addl(lregHi, rregHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isConstant()) {
                // cpu register - constant
                if (compilation.target.arch.is64bit()) {
                    long c = right.asConstantPtr().asLongBits();
                    masm().movptr(X86Register.r10, c);
                    switch (code) {
                        case Add:
                            masm().addptr(lregLo, X86Register.r10);
                            break;
                        case Sub:
                            masm().subptr(lregLo, X86Register.r10);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    int cLo = right.asConstantPtr().asIntLo();
                    int cHi = right.asConstantPtr().asIntHi();
                    switch (code) {
                        case Add:
                            masm().addptr(lregLo, cLo);
                            masm().adcl(lregHi, cHi);
                            break;
                        case Sub:
                            masm().subptr(lregLo, cLo);
                            masm().sbbl(lregHi, cHi);
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
            Register lreg = asXmmFloatReg(left);
            assert lreg.isXMM();

            if (right.isSingleXmm()) {
                Register rreg = asXmmFloatReg(right);
                assert rreg.isXMM();
                switch (code) {
                    case Add:
                        masm().addss(lreg, rreg);
                        break;
                    case Sub:
                        masm().subss(lreg, rreg);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        masm().mulss(lreg, rreg);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        masm().divss(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isSingleStack()) {
                    raddr = frameMap().addressForSlot(right.singleStackIx());
                } else if (right.isConstant()) {
                    // hack for now
                    raddr = masm().asAddress(new InternalAddress(masm().floatConstant(right.asJfloat())));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add:
                        masm().addss(lreg, raddr);
                        break;
                    case Sub:
                        masm().subss(lreg, raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        masm().mulss(lreg, raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        masm().divss(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleXmm()) {
            assert left.equals(dest) : "left and dest must be equal";

            Register lreg = asXmmDoubleReg(left);
            assert lreg.isXMM();
            if (right.isDoubleXmm()) {
                Register rreg = asXmmDoubleReg(right);
                assert rreg.isXMM();
                switch (code) {
                    case Add:
                        masm().addsd(lreg, rreg);
                        break;
                    case Sub:
                        masm().subsd(lreg, rreg);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        masm().mulsd(lreg, rreg);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        masm().divsd(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isDoubleStack()) {
                    raddr = frameMap().addressForSlot(right.doubleStackIx());
                } else if (right.isConstant()) {
                    // hack for now
                    raddr = masm().asAddress(new InternalAddress(masm().doubleConstant(right.asJdouble())));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add:
                        masm().addsd(lreg, raddr);
                        break;
                    case Sub:
                        masm().subsd(lreg, raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        masm().mulsd(lreg, raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        masm().divsd(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isSingleFpu()) {
            assert dest.isSingleFpu() : "fpu stack allocation required";

            if (right.isSingleFpu()) {
                arithFpuImplementation(code, left.fpuRegnr() - X86Register.fpu0.number, right.fpuRegnr() - X86Register.fpu0.number, dest.fpuRegnr() - X86Register.fpu0.number, popFpuStack);

            } else {
                assert left.fpuRegnr() == 0 : "left must be on TOS";
                assert dest.fpuRegnr() == 0 : "dest must be on TOS";

                Address raddr;
                if (right.isSingleStack()) {
                    raddr = frameMap().addressForSlot(right.singleStackIx());
                } else if (right.isConstant()) {
                    int constAddr = masm().floatConstant(right.asJfloat());
                    // hack for now
                    raddr = masm().asAddress(new InternalAddress(constAddr));
                } else {
                    throw Util.shouldNotReachHere();
                }

                switch (code) {
                    case Add:
                        masm().faddS(raddr);
                        break;
                    case Sub:
                        masm().fsubS(raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        masm().fmulS(raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        masm().fdivS(raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleFpu()) {
            assert dest.isDoubleFpu() : "fpu stack allocation required";

            if (code == LIROpcode.MulStrictFp || code == LIROpcode.DivStrictFp) {
                // Double values require special handling for strictfp mul/div on x86
                masm().fldX(new RuntimeAddress(CiRuntimeCall.AddrFpuSubnormalBias1));
                masm().fmulp(left.fpuRegnrLo() + 1 - X86Register.fpu0.number);
            }

            if (right.isDoubleFpu()) {
                arithFpuImplementation(code, left.fpuRegnrLo() - X86Register.fpu0.number, right.fpuRegnrLo() - X86Register.fpu0.number, dest.fpuRegnrLo() - X86Register.fpu0.number, popFpuStack);

            } else {
                assert left.asRegisterLo() == X86Register.fpu0 : "left must be on TOS";
                assert dest.asRegisterLo() == X86Register.fpu0 : "dest must be on TOS";

                Address raddr;
                if (right.isDoubleStack()) {
                    raddr = frameMap().addressForSlot(right.doubleStackIx());
                } else if (right.isConstant()) {
                    // hack for now
                    raddr = masm().asAddress(new InternalAddress(masm().doubleConstant(right.asJdouble())));
                } else {
                    throw Util.shouldNotReachHere();
                }

                switch (code) {
                    case Add:
                        masm().faddD(raddr);
                        break;
                    case Sub:
                        masm().fsubD(raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        masm().fmulD(raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        masm().fdivD(raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

            if (code == LIROpcode.MulStrictFp || code == LIROpcode.DivStrictFp) {
                // Double values require special handling for strictfp mul/div on x86
                masm().fldX(new RuntimeAddress(CiRuntimeCall.AddrFpuSubnormalBias2));
                masm().fmulp(dest.fpuRegnrLo() + 1);
            }

        } else if (left.isSingleStack() || left.isAddress()) {
            assert left.equals(dest) : "left and dest must be equal";

            Address laddr;
            if (left.isSingleStack()) {
                laddr = frameMap().addressForSlot(left.singleStackIx());
            } else if (left.isAddress()) {
                laddr = asAddress(left.asAddressPtr());
            } else {
                throw Util.shouldNotReachHere();
            }

            if (right.isSingleCpu()) {
                Register rreg = right.asRegister();
                switch (code) {
                    case Add:
                        masm().addl(laddr, rreg);
                        break;
                    case Sub:
                        masm().subl(laddr, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isConstant()) {
                int c = right.asConstantPtr().asInt();
                switch (code) {
                    case Add: {
                        masm().incrementl(laddr, c);
                        break;
                    }
                    case Sub: {
                        masm().decrementl(laddr, c);
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

    private void arithFpuImplementation(LIROpcode code, int leftIndex, int rightIndex, int destIndex, boolean popFpuStack) {
        assert popFpuStack || (leftIndex == destIndex || rightIndex == destIndex) : "invalid LIR";
        assert !popFpuStack || (leftIndex - 1 == destIndex || rightIndex - 1 == destIndex) : "invalid LIR";
        assert leftIndex == 0 || rightIndex == 0 : "either must be on top of stack";

        boolean leftIsTos = (leftIndex == 0);
        boolean destIsTos = (destIndex == 0);
        int nonTosIndex = (leftIsTos ? rightIndex : leftIndex);

        switch (code) {
            case Add:
                if (popFpuStack) {
                    masm().faddp(nonTosIndex);
                } else if (destIsTos) {
                    masm().fadd(nonTosIndex);
                } else {
                    masm().fadda(nonTosIndex);
                }
                break;

            case Sub:
                if (leftIsTos) {
                    if (popFpuStack) {
                        masm().fsubrp(nonTosIndex);
                    } else if (destIsTos) {
                        masm().fsub(nonTosIndex);
                    } else {
                        masm().fsubra(nonTosIndex);
                    }
                } else {
                    if (popFpuStack) {
                        masm().fsubp(nonTosIndex);
                    } else if (destIsTos) {
                        masm().fsubr(nonTosIndex);
                    } else {
                        masm().fsuba(nonTosIndex);
                    }
                }
                break;

            case MulStrictFp: // fall through
            case Mul:
                if (popFpuStack) {
                    masm().fmulp(nonTosIndex);
                } else if (destIsTos) {
                    masm().fmul(nonTosIndex);
                } else {
                    masm().fmula(nonTosIndex);
                }
                break;

            case DivStrictFp: // fall through
            case Div:
                if (leftIsTos) {
                    if (popFpuStack) {
                        masm().fdivrp(nonTosIndex);
                    } else if (destIsTos) {
                        masm().fdiv(nonTosIndex);
                    } else {
                        masm().fdivra(nonTosIndex);
                    }
                } else {
                    if (popFpuStack) {
                        masm().fdivp(nonTosIndex);
                    } else if (destIsTos) {
                        masm().fdivr(nonTosIndex);
                    } else {
                        masm().fdiva(nonTosIndex);
                    }
                }
                break;

            case Rem:
                assert leftIsTos && destIsTos && rightIndex == 1 : "must be guaranteed by FPU stack allocation";
                masm().fremr(Register.noreg);
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void intrinsicOp(LIROpcode code, LIROperand value, LIROperand unused, LIROperand dest, LIROp2 op) {

        if (value.isDoubleXmm()) {
            switch (code) {
                case Abs:
                    if (asXmmDoubleReg(dest) != asXmmDoubleReg(value)) {
                        masm().movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(value));
                    }
                    masm().andpd(asXmmDoubleReg(dest), new InternalAddress(masm().longConstant(DoubleSignMask, FloatConstantAlignment)));
                    break;

                case Sqrt:
                    masm().sqrtsd(asXmmDoubleReg(dest), asXmmDoubleReg(value));
                    break;
                // all other intrinsics are not available in the SSE instruction set, so FPU is used
                default:
                    throw Util.shouldNotReachHere();
            }

        } else if (value.isDoubleFpu()) {
            assert value.asRegisterLo() == X86Register.fpu0 && dest.asRegisterLo() == X86Register.fpu0 : "both must be on TOS";
            switch (code) {
                case Log:
                    masm().flog();
                    break;
                case Log10:
                    masm().flog10();
                    break;
                case Abs:
                    masm().fabs();
                    break;
                case Sqrt:
                    masm().fsqrt();
                    break;
                case Sin:
                    // Should consider not saving rbx, if not necessary
                    masm().trigfunc('s', op.fpuStackSize());
                    break;
                case Cos:
                    // Should consider not saving rbx, if not necessary
                    assert op.fpuStackSize() <= 6 : "sin and cos need two free stack slots";
                    masm().trigfunc('c', op.fpuStackSize());
                    break;
                case Tan:
                    // Should consider not saving rbx, if not necessary
                    masm().trigfunc('t', op.fpuStackSize());
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void logicOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst) {

        // assert left.destroysRegister() : "check";
        if (left.isSingleCpu()) {
            Register reg = left.asRegister();
            if (right.isConstant()) {
                int val = right.asConstantPtr().asInt();
                switch (code) {
                    case LogicAnd:
                        masm().andl(reg, val);
                        break;
                    case LogicOr:
                        masm().orl(reg, val);
                        break;
                    case LogicXor:
                        masm().xorl(reg, val);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isStack()) {
                // added support for stack operands
                Address raddr = frameMap().addressForSlot(right.singleStackIx());
                switch (code) {
                    case LogicAnd:
                        masm().andl(reg, raddr);
                        break;
                    case LogicOr:
                        masm().orl(reg, raddr);
                        break;
                    case LogicXor:
                        masm().xorl(reg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                Register rright = right.asRegister();
                switch (code) {
                    case LogicAnd:
                        masm().andptr(reg, rright);
                        break;
                    case LogicOr:
                        masm().orptr(reg, rright);
                        break;
                    case LogicXor:
                        masm().xorptr(reg, rright);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister());
        } else {
            Register lLo = left.asRegisterLo();
            Register lHi = left.asRegisterHi();
            if (right.isConstant()) {
                if (compilation.target.arch.is64bit()) {
                    masm().mov64(rscratch1, right.asConstantPtr().asLong());
                    switch (code) {
                        case LogicAnd:
                            masm().andq(lLo, rscratch1);
                            break;
                        case LogicOr:
                            masm().orq(lLo, rscratch1);
                            break;
                        case LogicXor:
                            masm().xorq(lLo, rscratch1);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    int rLo = right.asConstantPtr().asIntLo();
                    int rHi = right.asConstantPtr().asIntHi();
                    switch (code) {
                        case LogicAnd:
                            masm().andl(lLo, rLo);
                            masm().andl(lHi, rHi);
                            break;
                        case LogicOr:
                            masm().orl(lLo, rLo);
                            masm().orl(lHi, rHi);
                            break;
                        case LogicXor:
                            masm().xorl(lLo, rLo);
                            masm().xorl(lHi, rHi);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }
            } else {
                Register rLo = right.asRegisterLo();
                Register rHi = right.asRegisterHi();
                assert lLo != rHi : "overwriting registers";
                switch (code) {
                    case LogicAnd:
                        masm().andptr(lLo, rLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm().andptr(lHi, rHi);
                        }
                        break;
                    case LogicOr:
                        masm().orptr(lLo, rLo);
                        if (!compilation.target.arch.is64bit()) {
                            masm().orptr(lHi, rHi);
                        }
                        break;
                    case LogicXor:
                        masm().xorptr(lLo, rLo);

                        if (!compilation.target.arch.is64bit()) {
                            masm().xorptr(lHi, rHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

            Register dstLo = dst.asRegisterLo();
            Register dstHi = dst.asRegisterHi();

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
    void arithmeticIdiv(LIROpcode code, LIROperand left, LIROperand right, LIROperand temp, LIROperand result, CodeEmitInfo info) {

        assert left.isSingleCpu() : "left must be register";
        assert right.isSingleCpu() || right.isConstant() : "right must be register or constant";
        assert result.isSingleCpu() : "result must be register";

        // assert left.destroysRegister() : "check";
        // assert right.destroysRegister() : "check";

        Register lreg = left.asRegister();
        Register dreg = result.asRegister();

        if (right.isConstant()) {
            int divisor = right.asConstantPtr().asInt();
            assert divisor > 0 && Util.isPowerOf2(divisor) : "must be";
            if (code == LIROpcode.Div) {
                assert lreg == X86Register.rax : "must be rax : ";
                assert temp.asRegister() == X86Register.rdx : "tmp register must be rdx";
                masm().cdql(); // sign extend into rdx:rax
                if (divisor == 2) {
                    masm().subl(lreg, X86Register.rdx);
                } else {
                    masm().andl(X86Register.rdx, divisor - 1);
                    masm().addl(lreg, X86Register.rdx);
                }
                masm().sarl(lreg, Util.log2(divisor));
                moveRegs(lreg, dreg);
            } else if (code == LIROpcode.Rem) {
                Label done = new Label();
                masm().mov(dreg, lreg);
                masm().andl(dreg, 0x80000000 | (divisor - 1));
                masm().jcc(X86Assembler.Condition.positive, done);
                masm().decrement(dreg, 1);
                masm().orl(dreg, ~(divisor - 1));
                masm().increment(dreg, 1);
                masm().bind(done);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            Register rreg = right.asRegister();
            assert lreg == X86Register.rax : "left register must be rax : ";
            assert rreg != X86Register.rdx : "right register must not be rdx";
            assert temp.asRegister() == X86Register.rdx : "tmp register must be rdx";

            moveRegs(lreg, X86Register.rax);

            int idivlOffset = masm().correctedIdivl(rreg);
            addDebugInfoForDiv0(idivlOffset, info);
            if (code == LIROpcode.Rem) {
                moveRegs(X86Register.rdx, dreg); // result is in rdx
            } else {
                moveRegs(X86Register.rax, dreg);
            }
        }
    }

    @Override
    protected void compOp(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROp2 op) {
        if (opr1.isSingleCpu()) {
            Register reg1 = opr1.asRegister();
            if (opr2.isSingleCpu()) {
                // cpu register - cpu register
                if (opr1.type() == BasicType.Object) {
                    masm().cmpptr(reg1, opr2.asRegister());
                } else {
                    assert opr2.type() != BasicType.Object : "cmp int :  oop?";
                    masm().cmpl(reg1, opr2.asRegister());
                }
            } else if (opr2.isStack()) {
                // cpu register - stack
                if (opr1.type() == BasicType.Object) {
                    masm().cmpptr(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
                } else {
                    masm().cmpl(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
                }
            } else if (opr2.isConstant()) {
                // cpu register - constant
                LIRConstant c = opr2.asConstantPtr();
                if (c.type() == BasicType.Int) {
                    masm().cmpl(reg1, c.asInt());
                } else if (c.type() == BasicType.Object) {
                    // In 64bit oops are single register
                    Object o = c.asJobject();
                    if (o == null) {
                        masm().cmpptr(reg1, (int) NULLWORD);
                    } else {
                        if (compilation.target.arch.is64bit()) {
                            masm().movoop(rscratch1, o);
                            masm().cmpptr(reg1, rscratch1);
                        } else {
                            masm().cmpoop(reg1, c.asJobject());
                        }
                    }
                } else {
                    throw Util.shouldNotReachHere();
                }
                // cpu register - address
            } else if (opr2.isAddress()) {
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                masm().cmpl(reg1, asAddress(opr2.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleCpu()) {
            Register xlo = opr1.asRegisterLo();
            Register xhi = opr1.asRegisterHi();
            if (opr2.isDoubleCpu()) {
                if (compilation.target.arch.is64bit()) {
                    masm().cmpptr(xlo, opr2.asRegisterLo());
                } else {
                    // cpu register - cpu register
                    Register ylo = opr2.asRegisterLo();
                    Register yhi = opr2.asRegisterHi();
                    masm().subl(xlo, ylo);
                    masm().sbbl(xhi, yhi);
                    if (condition == LIRCondition.Equal || condition == LIRCondition.NotEqual) {
                        masm().orl(xhi, xlo);
                    }
                }
            } else if (opr2.isConstant()) {
                // cpu register - constant 0
                assert opr2.asLong() == 0 : "only handles zero";
                if (compilation.target.arch.is64bit()) {
                    masm().cmpptr(xlo, (int) opr2.asLong());
                } else {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "only handles equals case";
                    masm().orl(xhi, xlo);
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleXmm()) {
            Register reg1 = asXmmFloatReg(opr1);
            assert reg1.isXMM();
            if (opr2.isSingleXmm()) {
                // xmm register - xmm register
                masm().ucomiss(reg1, asXmmFloatReg(opr2));
            } else if (opr2.isStack()) {
                // xmm register - stack
                masm().ucomiss(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                masm().ucomiss(reg1, new InternalAddress(masm().floatConstant(opr2.asJfloat())));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                masm().ucomiss(reg1, asAddress(opr2.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleXmm()) {
            Register reg1 = asXmmDoubleReg(opr1);
            assert reg1.isXMM();
            if (opr2.isDoubleXmm()) {
                // xmm register - xmm register
                masm().ucomisd(reg1, asXmmDoubleReg(opr2));
            } else if (opr2.isStack()) {
                // xmm register - stack
                masm().ucomisd(reg1, frameMap().addressForSlot(opr2.doubleStackIx()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                masm().ucomisd(reg1, new InternalAddress(masm().doubleConstant(opr2.asJdouble())));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                masm().ucomisd(reg1, asAddress(opr2.asAddress()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleFpu() || opr1.isDoubleFpu()) {
            assert opr1.isFpuRegister() && opr1.asRegisterLo() == X86Register.fpu0 : "currently left-hand side must be on TOS (relax this restriction)";
            assert opr2.isFpuRegister() : "both must be registers";
            masm().fcmp(Register.noreg, opr2.asRegisterLo().number - X86Register.fpu0.number, op.fpuPopCount() > 0, op.fpuPopCount() > 1);

        } else if (opr1.isAddress() && opr2.isConstant()) {
            LIRConstant c = opr2.asConstantPtr();

            if (compilation.target.arch.is64bit()) {
                if (c.type() == BasicType.Object) {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "need to reverse";
                    masm().movoop(rscratch1, c.asJobject());
                }
            }
            if (op.info() != null) {
                addDebugInfoForNullCheckHere(op.info());
            }
            // special case: address - constant
            LIRAddress addr = opr1.asAddressPtr();
            if (c.type() == BasicType.Int) {
                masm().cmpl(asAddress(addr), c.asInt());
            } else if (c.type() == BasicType.Object) {
                if (compilation.target.arch.is64bit()) {
                    // %%% Make this explode if addr isn't reachable until we figure out a
                    // better strategy by giving X86Register.noreg as the temp for asAddress
                    masm().cmpptr(rscratch1, asAddress(addr, Register.noreg));
                } else {
                    masm().cmpoop(asAddress(addr), c.asJobject());
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
                assert right.isSingleXmm() : "must match";
                masm().cmpss2int(asXmmFloatReg(left), asXmmFloatReg(right), dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else if (left.isDoubleXmm()) {
                assert right.isDoubleXmm() : "must match";
                masm().cmpsd2int(asXmmDoubleReg(left), asXmmDoubleReg(right), dst.asRegister(), code == LIROpcode.Ucmpfd2i);

            } else {
                assert left.isSingleFpu() || left.isDoubleFpu() : "must be";
                assert right.isSingleFpu() || right.isDoubleFpu() : "must match";

                assert left.asRegisterLo() == X86Register.fpu0 : "left must be on TOS";
                masm().fcmp2int(dst.asRegister(), code == LIROpcode.Ucmpfd2i, right.asRegisterLo().number - X86Register.fpu0.number, op.fpuPopCount() > 0, op.fpuPopCount() > 1);
            }
        } else {
            assert code == LIROpcode.Cmpl2i;
            if (compilation.target.arch.is64bit()) {
                Register dest = dst.asRegister();
                masm().xorptr(dest, dest);
                Label high = new Label();
                Label done = new Label();
                masm().cmpptr(left.asRegisterLo(), right.asRegisterLo());
                masm().jcc(X86Assembler.Condition.equal, done);
                masm().jcc(X86Assembler.Condition.greater, high);
                masm().decrement(dest, 1);
                masm().jmp(done);
                masm().bind(high);
                masm().increment(dest, 1);

                masm().bind(done);

            } else {
                masm().lcmp2int(left.asRegisterHi(), left.asRegisterLo(), right.asRegisterHi(), right.asRegisterLo());
                moveRegs(left.asRegisterHi(), dst.asRegister());
            }
        }
    }

    @Override
    protected void alignCall(LIROpcode code) {
        if (compilation.runtime.isMP()) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = masm().offset();
            switch (code) {
                case StaticCall:
                case OptVirtualCall:
                    offset += compilation.target.arch.nativeCallDisplacementOffset;
                    break;
                case IcVirtualCall:
                    offset += compilation.target.arch.nativeCallDisplacementOffset + compilation.target.arch.nativeMoveConstInstructionSize;
                    break;
                case VirtualCall:
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
            while (offset++ % wordSize != 0) {
                masm().nop();
            }
        }
    }

    @Override
    protected void call(CiMethod method, CiRuntimeCall entry, CodeEmitInfo info) {
        assert !compilation.runtime.isMP() || (masm().offset() + compilation.target.arch.nativeCallDisplacementOffset) % wordSize == 0 : "must be aligned";
        masm().call(new RuntimeAddress(entry, method));
        addCallInfo(codeOffset(), info);
    }

    @Override
    protected void icCall(CiMethod method, CiRuntimeCall entry, CodeEmitInfo info) {
        masm().movoop(ICKlass, compilation.runtime.universeNonOopWord());
        assert !compilation.runtime.isMP() || (masm().offset() + compilation.target.arch.nativeCallDisplacementOffset) % wordSize == 0 : "must be aligned";
        masm().call(new RuntimeAddress(entry, method));
        addCallInfo(codeOffset(), info);
    }

    /**
     * (tw) Tentative implementation of a vtable call (C1 does always do a resolving runtime call).
     */
    @Override
    protected void vtableCall(CiMethod method, LIROperand receiver, long vtableOffset, CodeEmitInfo info) {
        assert receiver != null && vtableOffset >= 0 : "Invalid receiver or vtable offset!";
        assert receiver.isRegister() : "Receiver must be in a register";
        masm.call(new Address(receiver.asRegister(), Util.safeToInt(vtableOffset * compilation.runtime.vtableEntrySize() + compilation.runtime.vtableStartOffset())));
    }

    @Override
    protected void emitRTCall(LIRRTCall op) {
        rtCall(op.result(), op.address(), op.arguments(), op.tmp(), op.info());
    }

    @Override
    protected void emitStaticCallStub() {
        // TODO: Check with what to replace this!
        // Pointer callPc = masm().pc();

// Pointer stub = masm().startAStub(callStubSize);
// if (stub == null) {
// throw new Bailout("static call stub overflow");
// }
//
// int start = masm().offset();
// if (compilation.runtime.isMP()) {
// // make sure that the displacement word of the call ends up word aligned
// int offset = masm().offset() + compilation.runtime.nativeMovConstRegInstructionSize() +
        // compilation.runtime.nativeCallDisplacementOffset();
// while (offset++ % wordSize != 0) {
// masm().nop();
// }
// }
// masm().relocate(Relocation.staticStubRelocationSpec(callPc));
// masm().movoop(X86Register.rbx, null);
// // must be set to -1 at code generation time
// assert !compilation.runtime.isMP() || ((masm().offset() + 1) % wordSize) == 0 : "must be aligned on MP";
// // On 64bit this will die since it will take a movq & jmp, must be only a jmp
// masm().jump(new RuntimeAddress(masm().pc().value));
//
// assert masm().offset() - start <= callStubSize : "stub too big";
        // masm().endAStub();
    }

    @Override
    protected void throwOp(LIROperand exceptionPC, LIROperand exceptionOop, CodeEmitInfo info, boolean unwind) {
        assert exceptionOop.asRegister() == X86Register.rax : "must match";
        assert unwind || exceptionPC.asRegister() == X86Register.rdx : "must match";

        // exception object is not added to oop map by LinearScan
        // (LinearScan assumes that no oops are in fixed registers)
        info.addRegisterOop(exceptionOop);
        CiRuntimeCall unwindId;

        if (!unwind) {
            // get current pc information
            // pc is only needed if the method has an exception handler, the unwind code does not need it.
            int pcForAthrowOffset = masm().offset();
            InternalAddress pcForAthrow = new InternalAddress(masm().pc());
            masm().lea(exceptionPC.asRegister(), pcForAthrow);
            addCallInfo(pcForAthrowOffset, info); // for exception handler

            masm().verifyNotNullOop(X86Register.rax);
            // search an exception handler (rax: exception oop, rdx: throwing pc)
            if (compilation.hasFpuCode()) {
                unwindId = CiRuntimeCall.HandleException;
            } else {
                unwindId = CiRuntimeCall.HandleExceptionNofpu;
            }
        } else {
            unwindId = CiRuntimeCall.UnwindException;
        }
        masm().call(new RuntimeAddress(unwindId));

        // enough room for two byte trap
        masm().nop();
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
            Register value = left.asRegister();
            assert value != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl:
                    masm().shll(value);
                    break;
                case Shr:
                    masm().sarl(value);
                    break;
                case Ushr:
                    masm().shrl(value);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (left.isDoubleCpu()) {
            Register lo = left.asRegisterLo();
            Register hi = left.asRegisterHi();
            assert lo != SHIFTCount && hi != SHIFTCount : "left cannot be ECX";

            if (compilation.target.arch.is64bit()) {
                switch (code) {
                    case Shl:
                        masm().shlptr(lo);
                        break;
                    case Shr:
                        masm().sarptr(lo);
                        break;
                    case Ushr:
                        masm().shrptr(lo);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {

                switch (code) {
                    case Shl:
                        masm().lshl(hi, lo);
                        break;
                    case Shr:
                        masm().lshr(hi, lo, true);
                        break;
                    case Ushr:
                        masm().lshr(hi, lo, false);
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
            Register value = dest.asRegister();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegister(), value);
            switch (code) {
                case Shl:
                    masm().shll(value, count);
                    break;
                case Shr:
                    masm().sarl(value, count);
                    break;
                case Ushr:
                    masm().shrl(value, count);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (dest.isDoubleCpu()) {

            if (!compilation.target.arch.is64bit()) {
                throw Util.shouldNotReachHere();
            }

            // first move left into dest so that left is not destroyed by the shift
            Register value = dest.asRegisterLo();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegisterLo(), value);
            switch (code) {
                case Shl:
                    masm().shlptr(value, count);
                    break;
                case Shr:
                    masm().sarptr(value, count);
                    break;
                case Ushr:
                    masm().shrptr(value, count);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private void storeParameter(Register r, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        masm().movptr(new Address(X86Register.rsp, offsetFromRspInBytes), r);
    }

    void storeParameter(int c, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        masm().movptr(new Address(X86Register.rsp, offsetFromRspInBytes), c);
    }

    void storeParameter(Object o, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        masm().movoop(new Address(X86Register.rsp, offsetFromRspInBytes), o);
    }

    @Override
    protected void emitArrayCopy(LIRArrayCopy op) {
        CiType defaultType = op.expectedType();
        Register src = op.src().asRegister();
        Register dst = op.dst().asRegister();
        Register srcPos = op.srcPos().asRegister();
        Register dstPos = op.dstPos().asRegister();
        Register length = op.length().asRegister();
        Register tmp = op.tmp().asRegister();

        CodeStub stub = op.stub();
        int flags = op.flags();
        BasicType basicType = defaultType != null ? defaultType.elementType().basicType() : BasicType.Illegal;

        // if we don't know anything or it's an object array, just go through the generic arraycopy
        if (defaultType == null) {
            // save outgoing arguments on stack in case call to System.arraycopy is needed
            // HACK ALERT. This code used to push the parameters in a hardwired fashion
            // for interpreter calling conventions. Now we have to do it in new style conventions.
            // For the moment until C1 gets the new register allocator I just force all the
            // args to the right place (except the register args) and then on the back side
            // reload the register args properly if we go slow path. Yuck

            // These are proper for the calling convention

            storeParameter(length, 2);
            storeParameter(dstPos, 1);
            storeParameter(dst, 0);

            // these are just temporary placements until we need to reload
            storeParameter(srcPos, 3);
            storeParameter(src, 4);
            assert compilation.target.arch.is64bit() || (src == X86Register.rcx && srcPos == X86Register.rdx) : "mismatch in calling convention";

            CiRuntimeCall entry = CiRuntimeCall.ArrayCopy;

            // pass arguments: may push as this is not a safepoint; SP must be fix at each safepoint
            if (compilation.target.arch.is64bit()) {
                // The arguments are in java calling convention so we can trivially shift them to C
                // convention
                assert Register.assertDifferentRegisters(masm.cRarg0, masm.jRarg1, masm.jRarg2, masm.jRarg3, masm.jRarg4);
                masm().mov(masm.cRarg0, masm.jRarg0);
                assert Register.assertDifferentRegisters(masm.cRarg1, masm.jRarg2, masm.jRarg3, masm.jRarg4);
                masm().mov(masm.cRarg1, masm.jRarg1);
                assert Register.assertDifferentRegisters(masm.cRarg2, masm.jRarg3, masm.jRarg4);
                masm().mov(masm.cRarg2, masm.jRarg2);
                assert Register.assertDifferentRegisters(masm.cRarg3, masm.jRarg4);
                masm().mov(masm.cRarg3, masm.jRarg3);
                if (compilation.target.isWindows()) {
                    // Allocate abi space for args but be sure to keep stack aligned
                    masm().subptr(X86Register.rsp, 6 * compilation.target.arch.wordSize);
                    storeParameter(masm.jRarg4, 4);
                    masm().call(new RuntimeAddress(entry));
                    masm().addptr(X86Register.rsp, 6 * compilation.target.arch.wordSize);
                } else {
                    masm().mov(masm.cRarg4, masm.jRarg4);
                    masm().call(new RuntimeAddress(entry));
                }
            } else {
                masm().push(length);
                masm().push(dstPos);
                masm().push(dst);
                masm().push(srcPos);
                masm().push(src);
                masm().callVMLeaf(entry, 5); // removes pushed parameter from the stack

            }

            masm().cmpl(X86Register.rax, 0);
            masm().jcc(X86Assembler.Condition.equal, stub.continuation);

            // Reload values from the stack so they are where the stub
            // expects them.
            masm().movptr(dst, new Address(X86Register.rsp, 0 * wordSize));
            masm().movptr(dstPos, new Address(X86Register.rsp, 1 * wordSize));
            masm().movptr(length, new Address(X86Register.rsp, 2 * wordSize));
            masm().movptr(srcPos, new Address(X86Register.rsp, 3 * wordSize));
            masm().movptr(src, new Address(X86Register.rsp, 4 * wordSize));
            masm().jmp(stub.entry);

            masm().bind(stub.continuation);
            return;
        }

        assert defaultType.isArrayKlass() && defaultType.isLoaded() : "must be true at this point";

        int elemSize = basicType.elementSizeInBytes(referenceSize, wordSize);
        int shiftAmount;
        Address.ScaleFactor scale;

        switch (elemSize) {
            case 1:
                shiftAmount = 0;
                scale = Address.ScaleFactor.times1;
                break;
            case 2:
                shiftAmount = 1;
                scale = Address.ScaleFactor.times2;
                break;
            case 4:
                shiftAmount = 2;
                scale = Address.ScaleFactor.times4;
                break;
            case 8:
                shiftAmount = 3;
                scale = Address.ScaleFactor.times8;
                break;
            default:
                throw Util.shouldNotReachHere();
        }

        Address srcLengthAddr = new Address(src, compilation.runtime.arrayLengthOffsetInBytes());
        Address dstLengthAddr = new Address(dst, compilation.runtime.arrayLengthOffsetInBytes());
        Address srcKlassAddr = new Address(src, compilation.runtime.klassOffsetInBytes());
        Address dstKlassAddr = new Address(dst, compilation.runtime.klassOffsetInBytes());

        // length and pos's are all sign extended at this point on 64bit

        // test for null
        if ((flags & LIRArrayCopy.Flags.SrcNullCheck.mask()) != 0) {
            masm().testptr(src, src);
            masm().jcc(X86Assembler.Condition.zero, stub.entry);
        }
        if ((flags & LIRArrayCopy.Flags.DstNullCheck.mask()) != 0) {
            masm().testptr(dst, dst);
            masm().jcc(X86Assembler.Condition.zero, stub.entry);
        }

        // check if negative
        if ((flags & LIRArrayCopy.Flags.SrcPosPositiveCheck.mask()) != 0) {
            masm().testl(srcPos, srcPos);
            masm().jcc(X86Assembler.Condition.less, stub.entry);
        }
        if ((flags & LIRArrayCopy.Flags.DstPosPositiveCheck.mask()) != 0) {
            masm().testl(dstPos, dstPos);
            masm().jcc(X86Assembler.Condition.less, stub.entry);
        }
        if ((flags & LIRArrayCopy.Flags.LengthPositiveCheck.mask()) != 0) {
            masm().testl(length, length);
            masm().jcc(X86Assembler.Condition.less, stub.entry);
        }

        if ((flags & LIRArrayCopy.Flags.SrcRangeCheck.mask()) != 0) {
            masm().lea(tmp, new Address(srcPos, length, ScaleFactor.times1, 0));
            masm().cmpl(tmp, srcLengthAddr);
            masm().jcc(X86Assembler.Condition.above, stub.entry);
        }
        if ((flags & LIRArrayCopy.Flags.DstRangeCheck.mask()) != 0) {
            masm().lea(tmp, new Address(dstPos, length, ScaleFactor.times1, 0));
            masm().cmpl(tmp, dstLengthAddr);
            masm().jcc(X86Assembler.Condition.above, stub.entry);
        }

        if ((flags & LIRArrayCopy.Flags.TypeCheck.mask()) != 0) {
            masm().movptr(tmp, srcKlassAddr);
            masm().cmpptr(tmp, dstKlassAddr);
            masm().jcc(X86Assembler.Condition.notEqual, stub.entry);
        }

        if (C1XOptions.GenerateAssertionCode) {
            if (basicType != BasicType.Object || (flags & LIRArrayCopy.Flags.TypeCheck.mask()) == 0) {
                // Sanity check the known type with the incoming class. For the
                // primitive case the types must match exactly with src.klass and
                // dst.klass each exactly matching the default type. For the
                // object array case : if no type check is needed then either the
                // dst type is exactly the expected type and the src type is a
                // subtype which we can't check or src is the same array as dst
                // but not necessarily exactly of type defaultType.
                Label knownOk = new Label();
                Label halt = new Label();
                masm().movoop(tmp, defaultType.encoding());
                if (basicType != BasicType.Object) {
                    masm().cmpptr(tmp, dstKlassAddr);
                    masm().jcc(X86Assembler.Condition.notEqual, halt);
                    masm().cmpptr(tmp, srcKlassAddr);
                    masm().jcc(X86Assembler.Condition.equal, knownOk);
                } else {
                    masm().cmpptr(tmp, dstKlassAddr);
                    masm().jcc(X86Assembler.Condition.equal, knownOk);
                    masm().cmpptr(src, dst);
                    masm().jcc(X86Assembler.Condition.equal, knownOk);
                }
                masm().bind(halt);
                masm().stop("incorrect type information in arraycopy");
                masm().bind(knownOk);
            }
        }

        if (shiftAmount > 0 && basicType != BasicType.Object) {
            masm().shlptr(length, shiftAmount);
        }

        if (compilation.target.arch.is64bit()) {
            assert Register.assertDifferentRegisters(masm.cRarg0, dst, dstPos, length);
            masm().lea(masm.cRarg0, new Address(src, srcPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            assert Register.assertDifferentRegisters(masm.cRarg1, length);
            masm().lea(masm.cRarg1, new Address(dst, dstPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            masm().mov(masm.cRarg2, length);

        } else {
            masm().lea(tmp, new Address(src, srcPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            storeParameter(tmp, 0);
            masm().lea(tmp, new Address(dst, dstPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            storeParameter(tmp, 1);
            storeParameter(length, 2);
        }
        if (basicType == BasicType.Object) {
            masm().callVMLeaf(CiRuntimeCall.OopArrayCopy, 0);
        } else {
            masm().callVMLeaf(CiRuntimeCall.PrimitiveArrayCopy, 0);
        }

        masm().bind(stub.continuation);
    }

    @Override
    protected void emitLock(LIRLock op) {
        Register obj = op.objOpr().asRegister(); // may not be an oop
        Register hdr = op.hdrOpr().asRegister();
        Register lock = op.lockOpr().asRegister();
        if (!C1XOptions.UseFastLocking) {
            masm().jmp(op.stub().entry);
        } else if (op.code() == LIROpcode.Lock) {
            Register scratch = Register.noreg;
            if (C1XOptions.UseBiasedLocking) {
                scratch = op.scratchOpr().asRegister();
            }
            assert compilation.runtime.basicLockDisplacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            // add debug info for NullPointerException only if one is possible
            int nullCheckOffset = masm().lockObject(hdr, obj, lock, scratch, op.stub().entry);
            if (op.info() != null) {
                addDebugInfoForNullCheck(nullCheckOffset, op.info());
            }
            // done
        } else if (op.code() == LIROpcode.Unlock) {
            assert compilation.runtime.basicLockDisplacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            masm().unlockObject(hdr, obj, lock, op.stub().entry);
        } else {
            throw Util.shouldNotReachHere();
        }
        masm().bind(op.stub().continuation);
    }

    @Override
    protected void emitProfileCall(LIRProfileCall op) {
        CiMethod method = op.profiledMethod();
        int bci = op.profiledBci();

        // Update counter for all call types
        CiMethodData md = method.methodData();
        if (md == null) {
            throw new Bailout("out of memory building methodDataOop");
        }
        assert op.mdo().isSingleCpu() : "mdo must be allocated";
        Register mdo = op.mdo().asRegister();
        masm().movoop(mdo, md.encoding());
        Address counterAddr = new Address(mdo, md.countOffset(bci));
        masm().addl(counterAddr, 1);
        int bc = method.javaCodeAtBci(bci);
        // Perform additional virtual call profiling for invokevirtual and
        // invokeinterface bytecodes
        if ((bc == Bytecodes.INVOKEVIRTUAL || bc == Bytecodes.INVOKEINTERFACE) && C1XOptions.ProfileVirtualCalls) {
            assert op.recv().isSingleCpu() : "recv must be allocated";
            Register recv = op.recv().asRegister();
            assert Register.assertDifferentRegisters(mdo, recv);
            CiType knownKlass = op.knownHolder();
            if (C1XOptions.OptimizeVirtualCallProfiling && knownKlass != null) {
                // We know the type that will be seen at this call site; we can
                // statically update the methodDataOop rather than needing to do
                // dynamic tests on the receiver type

                // NOTE: we should probably put a lock around this search to
                // avoid collisions by concurrent compilations
                for (int i = 0; i < C1XOptions.ProfileTypeWidth; i++) {
                    CiType receiver = md.receiver(bci, i);
                    if (knownKlass.equals(receiver)) {
                        Address dataAddr = new Address(mdo, md.receiverCountOffset(bci, i));
                        masm().addl(dataAddr, 1);
                        return;
                    }
                }

                // Receiver type not found in profile data; select an empty slot

                // Note that this is less efficient than it should be because it
                // always does a write to the receiver part of the
                // VirtualCallData rather than just the first time
                for (int i = 0; i < C1XOptions.ProfileTypeWidth; i++) {
                    CiType receiver = md.receiver(bci, i);
                    if (receiver == null) {
                        Address recvAddr = new Address(mdo, md.receiverOffset(bci, i));
                        masm().movoop(recvAddr, knownKlass.encoding());
                        Address dataAddr = new Address(mdo, md.receiverCountOffset(bci, i));
                        masm().addl(dataAddr, 1);
                        return;
                    }
                }
            } else {
                masm().movptr(recv, new Address(recv, compilation.runtime.klassOffsetInBytes()));
                Label updateDone = new Label();
                for (int i = 0; i < C1XOptions.ProfileTypeWidth; i++) {
                    Label nextTest = new Label();
                    // See if the receiver is receiver[n].
                    masm().cmpptr(recv, new Address(mdo, md.receiverOffset(bci, i)));
                    masm().jcc(X86Assembler.Condition.notEqual, nextTest);
                    Address dataAddr = new Address(mdo, md.receiverCountOffset(bci, i));
                    masm().addl(dataAddr, 1);
                    masm().jmp(updateDone);
                    masm().bind(nextTest);
                }

                // Didn't find receiver; find next empty slot and fill it in
                for (int i = 0; i < C1XOptions.ProfileTypeWidth; i++) {
                    Label nextTest = new Label();
                    Address recvAddr = new Address(mdo, md.receiverOffset(bci, i));
                    masm().cmpptr(recvAddr, (int) NULLWORD);
                    masm().jcc(X86Assembler.Condition.notEqual, nextTest);
                    masm().movptr(recvAddr, recv);
                    masm().movl(new Address(mdo, md.receiverCountOffset(bci, i)), 1);
                    if (i < (C1XOptions.ProfileTypeWidth - 1)) {
                        masm().jmp(updateDone);
                    }
                    masm().bind(nextTest);
                }

                masm().bind(updateDone);
            }
        }
    }

    @Override
    protected void emitDelay(LIRDelay lirDelay) {
        throw Util.shouldNotReachHere();
    }

    @Override
    protected void monitorAddress(int monitorNo, LIROperand dst) {
        masm().lea(dst.asRegister(), frameMap().addressForMonitorLock(monitorNo));
    }

    @Override
    protected void alignBackwardBranchTarget() {
        masm().align(compilation.target.arch.wordSize);
    }

    @Override
    protected void negate(LIROperand left, LIROperand dest) {
        if (left.isSingleCpu()) {
            masm().negl(left.asRegister());
            moveRegs(left.asRegister(), dest.asRegister());

        } else if (left.isDoubleCpu()) {
            Register lo = left.asRegisterLo();
            if (compilation.target.arch.is64bit()) {
                Register dst = dest.asRegisterLo();
                masm().movptr(dst, lo);
                masm().negptr(dst);
            } else {
                Register hi = left.asRegisterHi();
                masm().lneg(hi, lo);
                if (dest.asRegisterLo() == hi) {
                    assert dest.asRegisterHi() != lo : "destroying register";
                    moveRegs(hi, dest.asRegisterHi());
                    moveRegs(lo, dest.asRegisterLo());
                } else {
                    moveRegs(lo, dest.asRegisterLo());
                    moveRegs(hi, dest.asRegisterHi());
                }
            }

        } else if (dest.isSingleXmm()) {
            if (asXmmFloatReg(left) != asXmmFloatReg(dest)) {
                masm().movflt(asXmmFloatReg(dest), asXmmFloatReg(left));
            }
            masm().xorps(asXmmFloatReg(dest), new InternalAddress(masm.longConstant(FloatSignFlip, FloatConstantAlignment)));

        } else if (dest.isDoubleXmm()) {
            if (asXmmDoubleReg(left) != asXmmDoubleReg(dest)) {
                masm().movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(left));
            }
            masm().xorpd(asXmmDoubleReg(dest), new InternalAddress(masm.longConstant(DoubleSignFlip, FloatConstantAlignment)));

        } else if (left.isSingleFpu() || left.isDoubleFpu()) {
            assert left.asRegisterLo() == X86Register.fpu0 : "arg must be on TOS";
            assert dest.asRegisterLo() == X86Register.fpu0 : "dest must be TOS";
            masm().fchs();

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void leal(LIROperand addr, LIROperand dest) {
        assert addr.isAddress() && dest.isRegister() : "check";
        Register reg = dest.asPointerRegister(compilation.target.arch);
        masm().lea(reg, asAddress(addr.asAddressPtr()));
    }

    @Override
    protected void rtCall(LIROperand result, CiRuntimeCall dest, List<LIROperand> args, LIROperand tmp, CodeEmitInfo info) {
        assert !tmp.isValid() : "don't need temporary";
        masm().call(new RuntimeAddress(dest));
        if (info != null) {
            addCallInfoHere(info);
        }
    }

    @Override
    protected void volatileMoveOp(LIROperand src, LIROperand dest, BasicType type, CodeEmitInfo info) {
        assert type == BasicType.Long : "only for volatile long fields";

        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        if (src.isDoubleXmm()) {
            if (dest.isDoubleCpu()) {
                if (compilation.target.arch.is64bit()) {
                    masm().movdq(dest.asRegisterLo(), asXmmDoubleReg(src));
                } else {
                    masm().movdl(dest.asRegisterLo(), asXmmDoubleReg(src));
                    masm().psrlq(asXmmDoubleReg(src), 32);
                    masm().movdl(dest.asRegisterHi(), asXmmDoubleReg(src));
                }
            } else if (dest.isDoubleStack()) {
                masm().movdbl(frameMap().addressForSlot(dest.doubleStackIx()), asXmmDoubleReg(src));
            } else if (dest.isAddress()) {
                masm().movdbl(asAddress(dest.asAddressPtr()), asXmmDoubleReg(src));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleXmm()) {
            if (src.isDoubleStack()) {
                masm().movdbl(asXmmDoubleReg(dest), frameMap().addressForSlot(src.doubleStackIx()));
            } else if (src.isAddress()) {
                masm().movdbl(asXmmDoubleReg(dest), asAddress(src.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isDoubleFpu()) {
            assert src.asRegisterLo() == X86Register.fpu0 : "must be TOS";
            if (dest.isDoubleStack()) {
                masm().fistpD(frameMap().addressForSlot(dest.doubleStackIx()));
            } else if (dest.isAddress()) {
                masm().fistpD(asAddress(dest.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleFpu()) {
            assert dest.asRegisterLo() == X86Register.fpu0 : "must be TOS";
            if (src.isDoubleStack()) {
                masm().fildD(frameMap().addressForSlot(src.doubleStackIx()));
            } else if (src.isAddress()) {
                masm().fildD(asAddress(src.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private Register asXmmDoubleReg(LIROperand dest) {
        assert dest.isXmmRegister();
        assert dest.isDoubleXmm();
        return dest.asRegister();
    }

    @Override
    protected void membar() {
        // QQQ sparc TSO uses this,
        masm().membar(X86Assembler.MembarMaskBits.StoreLoad.mask());

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
            masm().mov(resultReg.asRegister(), X86FrameMap.r15thread);
        } else {
            masm().getThread(resultReg.asRegister());
        }
    }

    @Override
    protected void peephole(LIRList list) {
        // Do nothing for now
    }

    @Override
    protected void emitLIROp2(LIROp2 op) {
        switch (op.code()) {
            case Cmp:
                if (op.info() != null) {
                    assert op.inOpr1().isAddress() || op.inOpr2().isAddress() : "shouldn't be codeemitinfo for non-Pointer operands";
                    addDebugInfoForNullCheckHere(op.info()); // exception possible
                }
                compOp(op.condition(), op.inOpr1(), op.inOpr2(), op);
                break;

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                compFl2i(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op);
                break;

            case Cmove:
                cmove(op.condition(), op.inOpr1(), op.inOpr2(), op.resultOpr());
                break;

            case Shl:
            case Shr:
            case Ushr:
                if (op.inOpr2().isConstant()) {
                    shiftOp(op.code(), op.inOpr1(), op.inOpr2().asConstantPtr().asInt(), op.resultOpr());
                } else {
                    shiftOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op.tmpOpr());
                }
                break;

            case Add:
            case Sub:
            case Mul:
            case MulStrictFp:
            case Div:
            case DivStrictFp:
            case Rem:
                assert op.fpuPopCount() < 2 : "";
                arithOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op.info(), op.fpuPopCount() == 1);
                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                intrinsicOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op);
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                logicOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr());
                break;

            case Throw:
            case Unwind:
                throwOp(op.inOpr1(), op.inOpr2(), op.info(), op.code() == LIROpcode.Unwind);
                break;

            default:
                Util.unimplemented();
                break;
        }
    }

    @Override
    protected void emitCode(CodeStub s) {
        s.accept(new X86CodeStubVisitor(this));
    }
}
