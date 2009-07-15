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
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.x86.Address.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

public class X86LIRAssembler extends LIRAssembler {

    private static final long NULLWORD = 0;
    private static final X86Register ICKlass = X86Register.rax;
    private static final X86Register SYNCHeader = X86Register.rax;
    private static final X86Register SHIFTCount = X86Register.rcx;
    private X86MacroAssembler masm;

    private final int callStubSize;
    private final int exceptionHandlerSize;
    final int deoptHandlerSize;
    private final int wordSize;
    private final int referenceSize;
    private final X86Register rscratch1;


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

    private X86MacroAssembler lir() {
        return masm;
    }

    @Override
    protected void set_24bitFPU() {

        lir().fldcw(new ExternalAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.FpuCntrlWrd_24)));

    }

    @Override
    protected void resetFPU() {
        lir().fldcw(new ExternalAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.FpuCntrlWrdStd)));
    }

    @Override
    protected void fpop() {
        lir().fpop();
    }

    @Override
    protected void fxch(int i) {
        lir().fxch(i);
    }

    @Override
    protected void fld(int i) {
        lir().fldS(i);
    }

    @Override
    protected void ffree(int i) {
        lir().ffree(i);
    }

    @Override
    protected void breakpoint() {
        lir().int3();
    }

    protected static X86Register asRegister(LIROperand opr) {
        return (X86Register) opr.asRegister();
    }

    @Override
    protected void push(LIROperand opr) {
        if (opr.isSingleCpu()) {
            lir().pushReg(asRegister(opr));
        } else if (opr.isDoubleCpu()) {
            if (!compilation.target.arch.is64bit()) {
                lir().pushReg((X86Register) opr.asRegisterHi());
            }
            lir().pushReg((X86Register) opr.asRegisterLo());
        } else if (opr.isStack()) {
            lir().pushAddr(frameMap().addressForSlot(opr.singleStackIx()));
        } else if (opr.isConstant()) {
            LIRConstant constOpr = opr.asConstantPtr();
            if (constOpr.type() == BasicType.Object) {
                lir().pushOop(constOpr.asJobject());
            } else if (constOpr.type() == BasicType.Int) {
                lir().pushJint(constOpr.asJint());
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
            lir().popReg((X86Register) opr.asRegister());
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

    private Address asAddress(LIRAddress addr, X86Register tmp) {
        if (addr.base().isIllegal()) {
            assert addr.index().isIllegal() : "must be illegal too";
            AddressLiteral laddr = new AddressLiteral(new Address(addr.displacement()), RelocInfo.Type.none);
            if (!lir().reachable(laddr)) {
                lir().movptr(tmp, laddr.addr());
                Address res = new Address(tmp, 0);
                return res;
            } else {
                return lir().asAddress(laddr);
            }
        }

        Register base = addr.base().asPointerRegister(compilation.target.arch);

        if (addr.index().isIllegal()) {
            return new Address(base, addr.displacement());
        } else if (addr.index().isCpuRegister()) {
            Register index = addr.index().asPointerRegister(compilation.target.arch);
            return new Address(base, index, addr.scale(), addr.displacement());
        } else if (addr.index().isConstant()) {
            long addrOffset = (addr.index().asConstantPtr().asJint() << addr.scale().ordinal()) + addr.displacement();
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
        BlockBegin osrEntry = compilation().osrEntry();
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
        lir().buildFrame(initialFrameSizeInBytes());

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
                lir().cmpptr(new Address(osrBuf, slotOffset + compilation.runtime.basicObjectLockOffsetInBytes()), (int) NULLWORD);
                lir().jcc(X86Assembler.Condition.notZero, l);
                lir().stop("locked object is null");
                lir().bind(l);
            }
            lir().movptr(X86Register.rbx, new Address(osrBuf, slotOffset + compilation.runtime.basicObjectLockOffsetInBytes()));
            lir().movptr(frameMap().addressForMonitorLock(i), X86Register.rbx);
            lir().movptr(X86Register.rbx, new Address(osrBuf, slotOffset + compilation.runtime.basicObjectObjOffsetInBytes()));
            lir().movptr(frameMap().addressForMonitorObject(i), X86Register.rbx);
        }
    }

    @Override
    protected int checkIcache() {
        X86Register receiver = (X86Register) this.receiverOpr().asRegister();
        int icCmpSize = 9;
        if (compilation.target.arch.is64bit()) {
            icCmpSize = 10;
        }

        if (!C1XOptions.VerifyOops) {
            // insert some nops so that the verified entry point is aligned on CodeEntryAlignment
            while ((lir().offset() + icCmpSize) % C1XOptions.CodeEntryAlignment != 0) {
                lir().nop();
            }
        }
        int offset = lir().offset();
        lir().inlineCacheCheck(receiver, ICKlass);
        assert lir().offset() % C1XOptions.CodeEntryAlignment == 0 || C1XOptions.VerifyOops : "alignment must be correct";
        if (C1XOptions.VerifyOops) {
            // force alignment after the cache check.
            // It's been verified to be aligned if !VerifyOops
            lir().align(C1XOptions.CodeEntryAlignment);
        }
        return offset;
    }

    private void monitorexit(LIROperand objOpr, LIROperand lockOpr, X86Register newHdr, int monitorNo, X86Register exception) {
        if (exception.isValid()) {
            // preserve exception
            // note: the monitorExit runtime call is a leaf routine
            // and cannot block => no GC can happen
            // The slow case (MonitorAccessStub) uses the first two stack slots
            // ([esp+0] and [esp+4]), therefore we store the exception at [esp+8]
            lir().movptr(new Address(X86Register.rsp, 2 * compilation.target.arch.wordSize), exception);
        }

        X86Register objReg = (X86Register) objOpr.asRegister();
        X86Register lockReg = (X86Register) lockOpr.asRegister();

        // setup registers (lockReg must be rax, for lockObject)
        assert objReg != SYNCHeader && lockReg != SYNCHeader : "rax :  must be available here";
        X86Register hdr = lockReg;
        assert newHdr == SYNCHeader : "wrong register";
        lockReg = newHdr;
        // compute pointer to BasicLock
        Address lockAddr = frameMap().addressForMonitorLock(monitorNo);
        lir().lea(lockReg, lockAddr);
        // unlock object
        MonitorAccessStub slowCase = new MonitorExitStub(lockOpr, true, monitorNo);
        // slowCaseStubs.append(slowCase);
        // temporary fix: must be created after exceptionhandler, therefore as call stub
        slowCaseStubs.add(slowCase);
        if (C1XOptions.UseFastLocking) {
            // try inlined fast unlocking first, revert to slow locking if it fails
            // note: lockReg points to the displaced header since the displaced header offset is 0!
            assert compilation.runtime.basicLockDisplacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            lir().unlockObject(hdr, objReg, lockReg, slowCase.entry());
        } else {
            // always do slow unlocking
            // note: the slow unlocking code could be inlined here, however if we use
            // slow unlocking, speed doesn't matter anyway and this solution is
            // simpler and requires less duplicated code - additionally, the
            // slow unlocking code is the same in either case which simplifies
            // debugging
            lir().jmp(slowCase.entry());
        }
        // done
        lir().bind(slowCase.continuation());

        if (exception.isValid()) {
            // restore exception
            lir().movptr(exception, new Address(X86Register.rsp, 2 * compilation.target.arch.wordSize));
        }
    }

    @Override
    protected int initialFrameSizeInBytes() {
        // if rounding, must let FrameMap know!

        // The frameMap records size in slots (32bit word)

        // subtract two words to account for return address and link
        return (frameMap().framesize() - (2 * VMRegImpl.slotsPerWord(wordSize))) * VMRegImpl.stackSlotSize;
    }

    @Override
    public void emitExceptionHandler() {
        // if the last instruction is a call (typically to do a throw which
        // is coming at the end after block reordering) the return address
        // must still point into the code area in order to avoid assert on
        // failures when searching for the corresponding bci => add a nop
        // (was bug 5/14/1999 - gri)

        lir().nop();

        // generate code for exception handler
        Pointer handlerBase = lir().startAStub(exceptionHandlerSize);
        if (handlerBase == null) {
            // not enough space left for the handler
            throw new Bailout("exception handler overflow");
        }

        int offset = codeOffset();

        compilation().offsets().setValue(CodeOffsets.Entries.Exceptions, codeOffset());

        // if the method does not have an exception handler : then there is
        // no reason to search for one
        if (compilation().hasExceptionHandlers() || compilation.runtime.jvmtiCanPostExceptions()) {
            // the exception oop and pc are in rax : and rdx
            // no other registers need to be preserved : so invalidate them
            lir().invalidateRegisters(false, true, true, false, true, true);

            // check that there is really an exception
            lir().verifyNotNullOop(X86Register.rax);

            // search an exception handler (rax: exception oop, rdx: throwing pc)
            lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.HandleExceptionNofpu)));

            // if the call returns here : then the exception handler for particular
            // exception doesn't exist . unwind activation and forward exception to caller
        }

        // the exception oop is in rax :
        // no other registers need to be preserved : so invalidate them
        lir().invalidateRegisters(false, true, true, true, true, true);

        // check that there is really an exception
        lir().verifyNotNullOop(X86Register.rax);

        // unlock the receiver/klass if necessary
        // rax : : exception
        CiMethod method = compilation().method();
        if (method.isSynchronized() && C1XOptions.GenerateSynchronizationCode) {
            monitorexit(X86FrameMap.rbxOopOpr, X86FrameMap.rcxOpr, SYNCHeader, 0, X86Register.rax);
        }

        // unwind activation and forward exception to caller
        // rax : : exception
        lir().jump(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.UnwindException)));

        assert codeOffset() - offset <= exceptionHandlerSize : "overflow";

        lir().endAStub();
    }

    // TODO: Check if emit_string_compare is used somewhere?

    @Override
    protected void returnOp(LIROperand result) {

        assert result.isIllegal() || !result.isSingleCpu() || result.asRegister() == X86Register.rax : "word returns are in rax : ";
        if (!result.isIllegal() && result.isFloatKind() && !result.isXmmRegister()) {
            assert result.fpu() == 0 : "result must already be on TOS";
        }

        // Pop the stack before the safepoint code
        lir().leave();

        // Note: we do not need to round double result; float result has the right precision
        // the poll sets the condition code, but no data registers
        AddressLiteral pollingPage = new AddressLiteral(compilation.runtime.getPollingPage() + (C1XOptions.SafepointPollOffset % compilation.runtime.vmPageSize()), RelocInfo.Type.pollReturnType);

        // NOTE: the requires that the polling page be reachable else the reloc
        // goes to the movq that loads the address and not the faulting instruction
        // which breaks the signal handler code

        lir().test32(X86Register.rax, pollingPage);

        lir().ret(0);
    }

    // TODO: Check why return type is int?
    @Override
    protected int safepointPoll(LIROperand tmp, CodeEmitInfo info) {
        AddressLiteral pollingPage = new AddressLiteral(compilation.runtime.getPollingPage() + (C1XOptions.SafepointPollOffset % compilation.runtime.vmPageSize()), RelocInfo.Type.pollType);

        if (info != null) {
            addDebugInfoForBranch(info);
        } else {
            throw Util.shouldNotReachHere();
        }

        int offset = lir().offset();

        // NOTE: the requires that the polling page be reachable else the reloc
        // goes to the movq that loads the address and not the faulting instruction
        // which breaks the signal handler code

        lir().test32(X86Register.rax, pollingPage);
        return offset;
    }

    private void moveRegs(X86Register fromReg, X86Register toReg) {
        if (fromReg != toReg) {
            lir().mov(toReg, fromReg);
        }
    }

    private void swapReg(X86Register a, X86Register b) {
        lir().xchgptr(a, b);
    }

    private void jobject2regWithPatching(X86Register reg, CodeEmitInfo info) {
        Object o = null;
        PatchingStub patch = new PatchingStub(masm, PatchingStub.PatchID.LoadKlassId);
        lir().movoop(reg, o);
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
                lir().movl((X86Register) dest.asRegister(), c.asJint());
                break;
            }

            case Long: {
                assert patchCode == LIRPatchCode.PatchNone : "no patching handled here";
                if (compilation.target.arch.is64bit()) {
                    lir().movptr((X86Register) dest.asRegisterLo(), c.asLong());
                } else {

                    lir().movptr((X86Register) dest.asRegisterLo(), c.asIntLo());
                    lir().movptr((X86Register) dest.asRegisterHi(), c.asIntHi());
                }
                break;
            }

            case Object: {
                if (patchCode != LIRPatchCode.PatchNone) {
                    jobject2regWithPatching((X86Register) dest.asRegister(), info);
                } else {
                    lir().movoop((X86Register) dest.asRegister(), c.asJobject());
                }
                break;
            }

            case Float: {
                if (dest.isSingleXmm()) {
                    if (c.isZeroFloat()) {
                        lir().xorps((X86Register) dest.asXmmFloatReg(), (X86Register) dest.asXmmFloatReg());
                    } else {
                        lir().movflt((X86Register) dest.asXmmFloatReg(), new InternalAddress(floatConstant(c.asJfloat()).value));
                    }
                } else {
                    assert dest.isSingleFpu() : "must be";
                    assert dest.fpuRegnr() == 0 : "dest must be TOS";
                    if (c.isZeroFloat()) {
                        lir().fldz();
                    } else if (c.isOneFloat()) {
                        lir().fld1();
                    } else {
                        lir().fldS(new InternalAddress(floatConstant(c.asJfloat()).value));
                    }
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    if (c.isZeroDouble()) {
                        lir().xorpd((X86Register) dest.asXmmDoubleReg(), (X86Register) dest.asXmmDoubleReg());
                    } else {
                        lir().movdbl((X86Register) dest.asXmmDoubleReg(), new InternalAddress(doubleConstant(c.asJdouble()).value));
                    }
                } else {
                    assert dest.isDoubleFpu() : "must be";
                    assert dest.fpuRegnrLo() == 0 : "dest must be TOS";
                    if (c.isZeroDouble()) {
                        lir().fldz();
                    } else if (c.isOneDouble()) {
                        lir().fld1();
                    } else {
                        lir().fldD(new InternalAddress(doubleConstant(c.asJdouble()).value));
                    }
                }
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }
    }

    Pointer floatConstant(float f) {
        Pointer constAddr = lir().floatConstant(f);
        if (constAddr == null) {
            throw new Bailout(" section overflow");
        } else {
            return constAddr;
        }
    }

    Pointer doubleConstant(double d) {
        Pointer constAddr = lir().doubleConstant(d);
        if (constAddr == null) {
            throw new Bailout(" section overflow");
        } else {
            return constAddr;
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
                lir().movl(frameMap().addressForSlot(dest.singleStackIx()), c.asIntBits());
                break;

            case Object:
                lir().movoop(frameMap().addressForSlot(dest.singleStackIx()), c.asJobject());
                break;

            case Long: // fall through
            case Double:

                if (compilation.target.arch.is64bit()) {
                    lir().movptr(frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes), c.asLongBits());
                } else {
                    lir().movptr(frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes), c.asIntLoBits());
                    lir().movptr(frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes), c.asIntHiBits());
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
                lir().movl(asAddress(addr), c.asIntBits());
                break;

            case Object: // fall through
                if (c.asJobject() == null) {
                    lir().movptr(asAddress(addr), NULLWORD);
                } else {
                    if (isLiteralAddress(addr)) {
                        lir().movoop(asAddress(addr, X86Register.noreg), c.asJobject());
                        throw Util.shouldNotReachHere();
                    } else {
                        lir().movoop(asAddress(addr), c.asJobject());
                    }
                }
                break;

            case Long: // fall through
            case Double:

                if (compilation.target.arch.is64bit()) {
                    if (isLiteralAddress(addr)) {
                        lir().movptr(asAddress(addr, X86FrameMap.r15thread), c.asLongBits());
                        throw Util.shouldNotReachHere();
                    } else {
                        lir().movptr(X86Register.r10, c.asLongBits());
                        nullCheckHere = codeOffset();
                        lir().movptr(asAddressLo(addr), X86Register.r10);
                    }
                } else {
                    // Always reachable in 32bit so this doesn't produce useless move literal
                    lir().movptr(asAddressHi(addr), c.asIntHiBits());
                    lir().movptr(asAddressLo(addr), c.asIntLoBits());
                }
                break;

            case Boolean: // fall through
            case Byte:
                lir().movb(asAddress(addr), c.asJint() & 0xFF);
                break;

            case Char: // fall through
            case Short:
                lir().movw(asAddress(addr), c.asJint() & 0xFFFF);
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
                    moveRegs((X86Register) src.asRegisterLo(), (X86Register) dest.asRegister());
                    return;
                }
            }
            assert src.isSingleCpu() : "must match";
            if (src.type() == BasicType.Object) {
                lir().verifyOop((X86Register) src.asRegister());
            }
            moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegister());

        } else if (dest.isDoubleCpu()) {
            if (compilation.target.arch.is64bit()) {
                if (src.type() == BasicType.Object) {
                    // Surprising to me but we can see move of a long to tObject
                    lir().verifyOop((X86Register) src.asRegister());
                    moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegisterLo());
                    return;
                }
            }
            assert src.isDoubleCpu() : "must match";
            X86Register fLo = (X86Register) src.asRegisterLo();
            X86Register fHi = (X86Register) src.asRegisterHi();
            X86Register tLo = (X86Register) dest.asRegisterLo();
            X86Register tHi = (X86Register) dest.asRegisterHi();
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
            lir().movflt(new Address(X86Register.rsp, 0), (X86Register) src.asXmmFloatReg());
            lir().fldS(new Address(X86Register.rsp, 0));
        } else if (src.isDoubleXmm() && !dest.isDoubleXmm()) {
            lir().movdbl(new Address(X86Register.rsp, 0), (X86Register) src.asXmmDoubleReg());
            lir().fldD(new Address(X86Register.rsp, 0));
        } else if (dest.isSingleXmm() && !src.isSingleXmm()) {
            lir().fstpS(new Address(X86Register.rsp, 0));
            lir().movflt((X86Register) dest.asXmmFloatReg(), new Address(X86Register.rsp, 0));
        } else if (dest.isDoubleXmm() && !src.isDoubleXmm()) {
            lir().fstpD(new Address(X86Register.rsp, 0));
            lir().movdbl((X86Register) dest.asXmmDoubleReg(), new Address(X86Register.rsp, 0));

            // move between xmm-registers
        } else if (dest.isSingleXmm()) {
            assert src.isSingleXmm() : "must match";
            lir().movflt((X86Register) dest.asXmmFloatReg(), (X86Register) src.asXmmFloatReg());
        } else if (dest.isDoubleXmm()) {
            assert src.isDoubleXmm() : "must match";
            lir().movdbl((X86Register) dest.asXmmDoubleReg(), (X86Register) src.asXmmDoubleReg());

            // move between fpu-registers (no instruction necessary because of fpu-stack)
        } else if (dest.isSingleFpu() || dest.isDoubleFpu()) {
            assert src.isSingleFpu() || src.isDoubleFpu() : "must match";
            assert src.fpu() == dest.fpu() : "currently should be nothing to do";
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
                lir().verifyOop((X86Register) src.asRegister());
                lir().movptr(dst, (X86Register) src.asRegister());
            } else {
                lir().movl(dst, (X86Register) src.asRegister());
            }

        } else if (src.isDoubleCpu()) {
            Address dstLO = frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes);
            Address dstHI = frameMap().addressForSlot(dest.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes);
            lir().movptr(dstLO, (X86Register) src.asRegisterLo());
            if (!compilation.target.arch.is64bit()) {
                lir().movptr(dstHI, (X86Register) src.asRegisterHi());
            }

        } else if (src.isSingleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.singleStackIx());
            lir().movflt(dstAddr, (X86Register) src.asXmmFloatReg());

        } else if (src.isDoubleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.doubleStackIx());
            lir().movdbl(dstAddr, (X86Register) src.asXmmDoubleReg());

        } else if (src.isSingleFpu()) {
            assert src.fpuRegnr() == 0 : "argument must be on TOS";
            Address dstAddr = frameMap().addressForSlot(dest.singleStackIx());
            if (popFpuStack) {
                lir().fstpS(dstAddr);
            } else {
                lir().fstS(dstAddr);
            }

        } else if (src.isDoubleFpu()) {
            assert src.fpuRegnrLo() == 0 : "argument must be on TOS";
            Address dstAddr = frameMap().addressForSlot(dest.doubleStackIx());
            if (popFpuStack) {
                lir().fstpD(dstAddr);
            } else {
                lir().fstD(dstAddr);
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
            lir().verifyOop((X86Register) src.asRegister());
        }
        if (patchCode != LIRPatchCode.PatchNone) {
            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
            Address toa = asAddress(toAddr);
            assert toa.disp() != 0 : "must have";
        }
        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        switch (type) {
            case Float: {
                if (src.isSingleXmm()) {
                    lir().movflt(asAddress(toAddr), asXmmFloatReg(src));
                } else {
                    assert src.isSingleFpu() : "must be";
                    assert src.fpuRegnr() == 0 : "argument must be on TOS";
                    if (popFpuStack) {
                        lir().fstpS(asAddress(toAddr));
                    } else {
                        lir().fstS(asAddress(toAddr));
                    }
                }
                break;
            }

            case Double: {
                if (src.isDoubleXmm()) {
                    lir().movdbl(asAddress(toAddr), asXmmDoubleReg(src));
                } else {
                    assert src.isDoubleFpu() : "must be";
                    assert src.fpuRegnrLo() == 0 : "argument must be on TOS";
                    if (popFpuStack) {
                        lir().fstpD(asAddress(toAddr));
                    } else {
                        lir().fstD(asAddress(toAddr));
                    }
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
                if (compilation.target.arch.is64bit()) {
                    lir().movptr(asAddress(toAddr), (X86Register) src.asRegister());
                } else {
                    lir().movl(asAddress(toAddr), (X86Register) src.asRegister());

                }
                break;
            case Int:
                lir().movl(asAddress(toAddr), (X86Register) src.asRegister());
                break;

            case Long: {
                X86Register fromLo = (X86Register) src.asRegisterLo();
                X86Register fromHi = (X86Register) src.asRegisterHi();
                if (compilation.target.arch.is64bit()) {
                    lir().movptr(asAddressLo(toAddr), fromLo);
                } else {
                    Register base = toAddr.base().asRegister();
                    Register index = X86Register.noreg;
                    if (toAddr.index().isRegister()) {
                        index = toAddr.index().asRegister();
                    }
                    if (base == fromLo || index == fromLo) {
                        assert base != fromHi : "can't be";
                        assert index == X86Register.noreg || (index != base && index != fromHi) : "can't handle this";
                        lir().movl(asAddressHi(toAddr), fromHi);
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchHigh, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchLow;
                        }
                        lir().movl(asAddressLo(toAddr), fromLo);
                    } else {
                        assert index == X86Register.noreg || (index != base && index != fromLo) : "can't handle this";
                        lir().movl(asAddressLo(toAddr), fromLo);
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchLow, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchHigh;
                        }
                        lir().movl(asAddressHi(toAddr), fromHi);
                    }
                }
                break;
            }

            case Byte: // fall through
            case Boolean: {
                X86Register srcReg = (X86Register) src.asRegister();
                Address dstAddr = asAddress(toAddr);
                assert compilation.target.isP6() || srcReg.hasByteRegister() : "must use byte registers if not P6";
                lir().movb(dstAddr, srcReg);
                break;
            }

            case Char: // fall through
            case Short:
                lir().movw(asAddress(toAddr), (X86Register) src.asRegister());
                break;

            default:
                throw Util.shouldNotReachHere();
        }

        if (patchCode != LIRPatchCode.PatchNone) {
            patchingEpilog(patch, patchCode, toAddr.base().asRegister(), info);
        }
    }

    private static X86Register asXmmFloatReg(LIROperand src) {
        return (X86Register) src.asXmmFloatReg();
    }

    @Override
    protected void stack2reg(LIROperand src, LIROperand dest, BasicType type) {
        assert src.isStack() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";

        if (dest.isSingleCpu()) {
            if (type == BasicType.Object) {
                lir().movptr((X86Register) dest.asRegister(), frameMap().addressForSlot(src.singleStackIx()));
                lir().verifyOop((X86Register) dest.asRegister());
            } else {
                lir().movl((X86Register) dest.asRegister(), frameMap().addressForSlot(src.singleStackIx()));
            }

        } else if (dest.isDoubleCpu()) {
            Address srcAddrLO = frameMap().addressForSlot(src.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes);
            Address srcAddrHI = frameMap().addressForSlot(src.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes);
            lir().movptr((X86Register) dest.asRegisterLo(), srcAddrLO);
            if (!compilation.target.arch.is64bit()) {
                lir().movptr((X86Register) dest.asRegisterHi(), srcAddrHI);
            }

        } else if (dest.isSingleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.singleStackIx());
            lir().movflt(asXmmFloatReg(dest), srcAddr);

        } else if (dest.isDoubleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIx());
            lir().movdbl(asXmmDoubleReg(dest), srcAddr);

        } else if (dest.isSingleFpu()) {
            assert dest.fpuRegnr() == 0 : "dest must be TOS";
            Address srcAddr = frameMap().addressForSlot(src.singleStackIx());
            lir().fldS(srcAddr);

        } else if (dest.isDoubleFpu()) {
            assert dest.fpuRegnrLo() == 0 : "dest must be TOS";
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIx());
            lir().fldD(srcAddr);

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void stack2stack(LIROperand src, LIROperand dest, BasicType type) {
        if (src.isSingleStack()) {
            if (type == BasicType.Object) {
                lir().pushptr(frameMap().addressForSlot(src.singleStackIx()));
                lir().popptr(frameMap().addressForSlot(dest.singleStackIx()));
            } else {
                lir().pushl(frameMap().addressForSlot(src.singleStackIx()));
                lir().popl(frameMap().addressForSlot(dest.singleStackIx()));
            }

        } else if (src.isDoubleStack()) {
            if (compilation.target.arch.is64bit()) {
                lir().pushptr(frameMap().addressForSlot(src.doubleStackIx()));
                lir().popptr(frameMap().addressForSlot(dest.doubleStackIx()));
            } else {
                lir().pushl(frameMap().addressForSlot(src.doubleStackIx(), 0));
                // push and pop the part at src + wordSize, adding wordSize for the previous push
                lir().pushl(frameMap().addressForSlot(src.doubleStackIx(), 2 * compilation.target.arch.wordSize));
                lir().popl(frameMap().addressForSlot(dest.doubleStackIx(), 2 * compilation.target.arch.wordSize));
                lir().popl(frameMap().addressForSlot(dest.doubleStackIx(), 0));
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
                    lir().xorptr((X86Register) dest.asRegister(), (X86Register) dest.asRegister());
                }
                break;
        }

        PatchingStub patch = null;
        if (patchCode != LIRPatchCode.PatchNone) {
            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
            assert fromAddr.disp() != 0 : "must have";
        }
        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        switch (type) {
            case Float: {
                if (dest.isSingleXmm()) {
                    lir().movflt(asXmmFloatReg(dest), fromAddr);
                } else {
                    assert dest.isSingleFpu() : "must be";
                    assert dest.fpuRegnr() == 0 : "dest must be TOS";
                    lir().fldS(fromAddr);
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    lir().movdbl(asXmmDoubleReg(dest), fromAddr);
                } else {
                    assert dest.isDoubleFpu() : "must be";
                    assert dest.fpuRegnrLo() == 0 : "dest must be TOS";
                    lir().fldD(fromAddr);
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
                if (compilation.target.arch.is64bit()) {
                    lir().movptr((X86Register) dest.asRegister(), fromAddr);
                } else {
                    lir().movl2ptr((X86Register) dest.asRegister(), fromAddr);

                }
                break;
            case Int:
                // %%% could this be a movl? this is safer but longer instruction
                lir().movl2ptr((X86Register) dest.asRegister(), fromAddr);
                break;

            case Long: {
                X86Register toLo = (X86Register) dest.asRegisterLo();
                X86Register toHi = (X86Register) dest.asRegisterHi();

                if (compilation.target.arch.is64bit()) {
                    lir().movptr(toLo, asAddressLo(addr));
                } else {
                    X86Register base = (X86Register) addr.base().asRegister();
                    X86Register index = X86Register.noreg;
                    if (addr.index().isRegister()) {
                        index = (X86Register) addr.index().asRegister();
                    }
                    if ((base == toLo && index == toHi) || (base == toHi && index == toLo)) {
                        // addresses with 2 registers are only formed as a result of
                        // array access so this code will never have to deal with
                        // patches or null checks.
                        assert info == null && patch == null : "must be";
                        lir().lea(toHi, asAddress(addr));
                        lir().movl(toLo, new Address(toHi, 0));
                        lir().movl(toHi, new Address(toHi, wordSize));
                    } else if (base == toLo || index == toLo) {
                        assert base != toHi : "can't be";
                        assert index == X86Register.noreg || (index != base && index != toHi) : "can't handle this";
                        lir().movl(toHi, asAddressHi(addr));
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchHigh, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchLow;
                        }
                        lir().movl(toLo, asAddressLo(addr));
                    } else {
                        assert index == X86Register.noreg || (index != base && index != toLo) : "can't handle this";
                        lir().movl(toLo, asAddressLo(addr));
                        if (patch != null) {
                            patchingEpilog(patch, LIRPatchCode.PatchLow, base, info);
                            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
                            patchCode = LIRPatchCode.PatchHigh;
                        }
                        lir().movl(toHi, asAddressHi(addr));
                    }
                }
                break;
            }

            case Boolean: // fall through
            case Byte: {
                X86Register destReg = (X86Register) dest.asRegister();
                assert compilation.target.isP6() || destReg.hasByteRegister() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    lir().movsbl(destReg, fromAddr);
                } else {
                    lir().movb(destReg, fromAddr);
                    lir().shll(destReg, 24);
                    lir().sarl(destReg, 24);
                }
                // These are unsigned so the zero extension on 64bit is just what we need
                break;
            }

            case Char: {
                X86Register destReg = (X86Register) dest.asRegister();
                assert compilation.target.isP6() || destReg.hasByteRegister() : "must use byte registers if not P6";
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    lir().movzwl(destReg, fromAddr);
                } else {
                    lir().movw(destReg, fromAddr);
                }
                // This is unsigned so the zero extension on 64bit is just what we need
                // lir(). movl2ptr(destReg, destReg);
                break;
            }

            case Short: {
                X86Register destReg = (X86Register) dest.asRegister();
                if (compilation.target.isP6() || fromAddr.uses(destReg)) {
                    lir().movswl(destReg, fromAddr);
                } else {
                    lir().movw(destReg, fromAddr);
                    lir().shll(destReg, 16);
                    lir().sarl(destReg, 16);
                }
                // Might not be needed in 64bit but certainly doesn't hurt (except for code size)
                lir().movl2ptr(destReg, destReg);
                break;
            }

            default:
                throw Util.shouldNotReachHere();
        }

        if (patch != null) {
            patchingEpilog(patch, patchCode, addr.base().asRegister(), info);
        }

        if (type == BasicType.Object) {
            lir().verifyOop((X86Register) dest.asRegister());
        }
    }

    @Override
    protected void prefetchr(LIROperand src) {
        LIRAddress addr = src.asAddressPtr();
        Address fromAddr = asAddress(addr);

        if (compilation.target.supportsSSE()) {
            switch (C1XOptions.ReadPrefetchInstr) {
                case 0:
                    lir().prefetchnta(fromAddr);
                    break;
                case 1:
                    lir().prefetcht0(fromAddr);
                    break;
                case 2:
                    lir().prefetcht2(fromAddr);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (compilation.target.supports3DNOW()) {
            lir().prefetchr(fromAddr);
        }
    }

    // TODO: Who uses this?
    public void prefetchw(LIROperand src) {
        LIRAddress addr = src.asAddressPtr();
        Address fromAddr = asAddress(addr);

        if (compilation.target.supportsSSE()) {
            switch (C1XOptions.AllocatePrefetchInstr) {
                case 0:
                    lir().prefetchnta(fromAddr);
                    break;
                case 1:
                    lir().prefetcht0(fromAddr);
                    break;
                case 2:
                    lir().prefetcht2(fromAddr);
                    break;
                case 3:
                    lir().prefetchw(fromAddr);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (compilation.target.supports3DNOW()) {
            lir().prefetchw(fromAddr);
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
            lir().jmp(op.label());
        } else {
            X86Assembler.Condition acond = X86Assembler.Condition.zero;
            if (op.code() == LIROpcode.CondFloatBranch) {
                assert op.ublock() != null : "must have unordered successor";
                lir().jcc(X86Assembler.Condition.parity, op.ublock().label());
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
            lir().jcc(acond, (op.label()));
        }
    }

    @Override
    protected void emitConvert(LIRConvert op) {
        LIROperand src = op.inOpr();
        LIROperand dest = op.resultOpr();

        switch (op.bytecode()) {
            case Bytecodes.I2L:
                if (compilation.target.arch.is64bit()) {
                    lir().movl2ptr((X86Register) dest.asRegisterLo(), (X86Register) src.asRegister());
                } else {
                    moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegisterLo());
                    moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegisterHi());
                    lir().sarl((X86Register) dest.asRegisterHi(), 31);
                }
                break;

            case Bytecodes.L2I:
                moveRegs((X86Register) src.asRegisterLo(), (X86Register) dest.asRegister());
                break;

            case Bytecodes.I2B:
                moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegister());
                lir().signExtendByte((X86Register) dest.asRegister());
                break;

            case Bytecodes.I2C:
                moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegister());
                lir().andl((X86Register) dest.asRegister(), 0xFFFF);
                break;

            case Bytecodes.I2S:
                moveRegs((X86Register) src.asRegister(), (X86Register) dest.asRegister());
                lir().signExtendShort((X86Register) dest.asRegister());
                break;

            case Bytecodes.F2D:
            case Bytecodes.D2F:
                if (dest.isSingleXmm()) {
                    lir().cvtsd2ss(asXmmFloatReg(dest), asXmmDoubleReg(src));
                } else if (dest.isDoubleXmm()) {
                    lir().cvtss2sd(asXmmDoubleReg(dest), asXmmFloatReg(src));
                } else {
                    assert src.fpu() == dest.fpu() : "register must be equal";
                    // do nothing (float result is rounded later through spilling)
                }
                break;

            case Bytecodes.I2F:
            case Bytecodes.I2D:
                if (dest.isSingleXmm()) {
                    lir().cvtsi2ssl((X86Register) dest.asXmmFloatReg(), (X86Register) src.asRegister());
                } else if (dest.isDoubleXmm()) {
                    lir().cvtsi2sdl((X86Register) dest.asXmmDoubleReg(), (X86Register) src.asRegister());
                } else {
                    assert dest.fpu() == 0 : "result must be on TOS";
                    lir().movl(new Address(X86Register.rsp, 0), (X86Register) src.asRegister());
                    lir().fildS(new Address(X86Register.rsp, 0));
                }
                break;

            case Bytecodes.F2I:
            case Bytecodes.D2I:
                if (src.isSingleXmm()) {
                    lir().cvttss2sil((X86Register) dest.asRegister(), asXmmFloatReg(src));
                } else if (src.isDoubleXmm()) {
                    lir().cvttsd2sil((X86Register) dest.asRegister(), asXmmDoubleReg(src));
                } else {
                    assert src.fpu() == 0 : "input must be on TOS";
                    lir().fldcw(new ExternalAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.FpuCntrlWrdTrunc)));
                    lir().fistS(new Address(X86Register.rsp, 0));
                    lir().movl((X86Register) dest.asRegister(), new Address(X86Register.rsp, 0));
                    lir().fldcw(new ExternalAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.FpuCntrlWrdStd)));
                }

                // IA32 conversion instructions do not match JLS for overflow, underflow and NaN . fixup in stub
                assert op.stub() != null : "stub required";
                lir().cmpl((X86Register) dest.asRegister(), 0x80000000);
                lir().jcc(X86Assembler.Condition.equal, op.stub().entry());
                lir().bind(op.stub().continuation());
                break;

            case Bytecodes.L2F:
            case Bytecodes.L2D:
                assert !dest.isXmmRegister() : "result in xmm register not supported (no SSE instruction present)";
                assert dest.fpu() == 0 : "result must be on TOS";

                lir().movptr(new Address(X86Register.rsp, 0), (X86Register) src.asRegisterLo());
                if (!compilation.target.arch.is64bit()) {
                    lir().movl(new Address(X86Register.rsp, wordSize), (X86Register) src.asRegisterHi());
                }
                lir().fildD(new Address(X86Register.rsp, 0));
                // float result is rounded later through spilling
                break;

            case Bytecodes.F2L:
            case Bytecodes.D2L:
                assert !src.isXmmRegister() : "input in xmm register not supported (no SSE instruction present)";
                assert src.fpu() == 0 : "input must be on TOS";
                assert dest == X86FrameMap.long0Opr(compilation.target.arch) : "runtime stub places result in these registers";

                // instruction sequence too long to inline it here
                lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.Fpu2longStub)));
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitAllocObj(LIRAllocObj op) {
        if (op.isInitCheck()) {
            lir().cmpl(new Address(op.klass().asRegister(), compilation.runtime.initStateOffsetInBytes()), compilation.runtime.instanceKlassFullyInitialized());
            addDebugInfoForNullCheckHere(op.stub().info());
            lir().jcc(X86Assembler.Condition.notEqual, op.stub().entry());
        }
        lir().allocateObject((X86Register) op.obj().asRegister(), (X86Register) op.tmp1().asRegister(), (X86Register) op.tmp2().asRegister(), op.headerSize(), op.obectSize(),
                        (X86Register) op.klass().asRegister(), op.stub().entry());
        lir().bind(op.stub().continuation());
    }

    @Override
    protected void emitAllocArray(LIRAllocArray op) {
        if (C1XOptions.UseSlowPath || (!C1XOptions.UseFastNewObjectArray && op.type() == BasicType.Object) || (!C1XOptions.UseFastNewTypeArray && op.type() != BasicType.Object)) {
            lir().jmp(op.stub().entry());
        } else {
            X86Register len = (X86Register) op.length().asRegister();
            X86Register tmp1 = (X86Register) op.tmp1().asRegister();
            X86Register tmp2 = (X86Register) op.tmp2().asRegister();
            X86Register tmp3 = (X86Register) op.tmp3().asRegister();
            if (len == tmp1) {
                tmp1 = tmp3;
            } else if (len == tmp2) {
                tmp2 = tmp3;
            } else if (len == tmp3) {
                // everything is ok
            } else {
                lir().mov(tmp3, len);
            }
            lir().allocateArray((X86Register) op.obj().asRegister(), len, tmp1, tmp2, compilation.runtime.arrayOopDescHeaderSize(op.type()), compilation.runtime.arrayElementSize(op.type()),
                            (X86Register) op.klass().asRegister(), op.stub().entry());
        }
        lir().bind(op.stub().continuation());
    }

    static void selectDifferentRegisters(X86Register preserve, X86Register extra, X86Register[] tmp1, X86Register[] tmp2) {
        if (tmp1[0] == preserve) {
            assert Register.assertDifferentRegisters(tmp1[0], tmp2[0], extra);
            tmp1[0] = extra;
        } else if (tmp2[0] == preserve) {
            Register.assertDifferentRegisters(tmp1[0], tmp2[0], extra);
            tmp2[0] = extra;
        }
        Register.assertDifferentRegisters(preserve, tmp1[0], tmp2[0]);
    }

    static void selectDifferentRegisters(X86Register preserve, X86Register extra, X86Register[] tmp1, X86Register[] tmp2, X86Register[] tmp3) {
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
        LIROpcode code = op.code();
        if (code == LIROpcode.StoreCheck) {
            X86Register value = (X86Register) op.object().asRegister();
            X86Register array = (X86Register) op.array().asRegister();
            X86Register kRInfo = (X86Register) op.tmp1().asRegister();
            X86Register klassRInfo = (X86Register) op.tmp2().asRegister();
            X86Register rtmp1 = (X86Register) op.tmp3().asRegister();

            CodeStub stub = op.stub();
            Label done = new Label();
            lir().cmpptr(value, (int) NULLWORD);
            lir().jcc(X86Assembler.Condition.equal, done);
            addDebugInfoForNullCheckHere(op.infoForException());
            lir().movptr(kRInfo, new Address(array, compilation.runtime.klassOffsetInBytes()));
            lir().movptr(klassRInfo, new Address(value, compilation.runtime.klassOffsetInBytes()));

            // get instance klass
            lir().movptr(kRInfo, new Address(kRInfo, compilation.runtime.elementKlassOffsetInBytes()));
            // perform the fast part of the checking logic
            lir().checkKlassSubtypeFastPath(klassRInfo, kRInfo, rtmp1, done, stub.entry(), null, new RegisterOrConstant(-1));
            // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
            lir().push(klassRInfo);
            lir().push(kRInfo);
            lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.SlowSubtypeCheck)));
            lir().pop(klassRInfo);
            lir().pop(kRInfo);
            // result is a boolean
            lir().cmpl(kRInfo, 0);
            lir().jcc(X86Assembler.Condition.equal, stub.entry());
            lir().bind(done);
        } else if (op.code() == LIROpcode.CheckCast) {
            // we always need a stub for the failure case.
            CodeStub stub = op.stub();
            X86Register obj = (X86Register) op.object().asRegister();
            X86Register kRInfo = (X86Register) op.tmp1().asRegister();
            X86Register klassRInfo = (X86Register) op.tmp2().asRegister();
            X86Register dst = (X86Register) op.result().asRegister();
            CiType k = op.klass();
            X86Register rtmp1 = X86Register.noreg;

            Label done = new Label();
            if (obj == kRInfo) {
                kRInfo = dst;
            } else if (obj == klassRInfo) {
                klassRInfo = dst;
            }
            if (k.isLoaded()) {
                // TODO: out params?!
                X86Register[] tmp1 = new X86Register[] {kRInfo};
                X86Register[] tmp2 = new X86Register[] {klassRInfo};
                selectDifferentRegisters(obj, dst, tmp1, tmp2);
                kRInfo = tmp1[0];
                klassRInfo = tmp2[0];
            } else {
                rtmp1 = (X86Register) op.tmp3().asRegister();
                X86Register[] tmp1 = new X86Register[] {kRInfo};
                X86Register[] tmp2 = new X86Register[] {klassRInfo};
                X86Register[] tmp3 = new X86Register[] {rtmp1};
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
                    lir().movoop(kRInfo, k);
                } else {
                    kRInfo = X86Register.noreg;
                }
            }
            assert obj != kRInfo : "must be different";
            lir().cmpptr(obj, (int) NULLWORD);
            if (op.profiledMethod() != null) {
                CiMethod method = op.profiledMethod();
                int bci = op.profiledBci();

                Label profileDone = new Label();
                lir().jcc(X86Assembler.Condition.notEqual, profileDone);
                // Object is null; update methodDataOop
                CiMethodData md = method.methodData();
                if (md == null) {
                    throw new Bailout("out of memory building methodDataOop");
                }
                // ciProfileData data = md.bciToData(bci);
                // assert data != null : "need data for checkcast";
                // assert data.isBitData() : "need BitData for checkcast";
                X86Register mdo = klassRInfo;
                lir().movoop(mdo, md);
                Address dataAddr = new Address(mdo, md.headerOffset(bci));
                int headerBits = compilation.runtime.methodDataNullSeenByteConstant(); // TODO: Check what this really
                // means!
                // DataLayout.flagMaskToHeaderMask(BitData.nullSeenByteConstant());
                lir().orl(dataAddr, headerBits);
                lir().jmp(done);
                lir().bind(profileDone);
            } else {
                lir().jcc(X86Assembler.Condition.equal, done);
            }
            lir().verifyOop(obj);

            if (op.isFastCheck()) {
                // get object classo
                // not a safepoint as obj null check happens earlier
                if (k.isLoaded()) {

                    if (compilation.target.arch.is64bit()) {
                        lir().cmpptr(kRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));
                    } else {
                        lir().cmpoop(new Address(obj, compilation.runtime.klassOffsetInBytes()), k);
                    }
                } else {
                    lir().cmpptr(kRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));

                }
                lir().jcc(X86Assembler.Condition.notEqual, stub.entry());
                lir().bind(done);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                lir().movptr(klassRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));
                if (k.isLoaded()) {
                    // See if we get an immediate positive hit
                    if (compilation.target.arch.is64bit()) {
                        lir().cmpptr(kRInfo, new Address(klassRInfo, k.superCheckOffset()));
                    } else {
                        lir().cmpoop(new Address(klassRInfo, k.superCheckOffset()), k);
                    }
                    if (Util.sizeofOopDesc() + compilation.runtime.secondarySuperCacheOffsetInBytes() != k.superCheckOffset()) {
                        lir().jcc(X86Assembler.Condition.notEqual, stub.entry());
                    } else {
                        // See if we get an immediate positive hit
                        lir().jcc(X86Assembler.Condition.equal, done);
                        // check for self
                        if (compilation.target.arch.is64bit()) {
                            lir().cmpptr(klassRInfo, kRInfo);
                        } else {
                            lir().cmpoop(klassRInfo, k);
                        }
                        lir().jcc(X86Assembler.Condition.equal, done);

                        lir().push(klassRInfo);
                        if (compilation.target.arch.is64bit()) {
                            lir().push(kRInfo);
                        } else {
                            lir().pushoop(k);
                        }
                        lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.SlowSubtypeCheck)));
                        lir().pop(klassRInfo);
                        lir().pop(klassRInfo);
                        // result is a boolean
                        lir().cmpl(klassRInfo, 0);
                        lir().jcc(X86Assembler.Condition.equal, stub.entry());
                    }
                    lir().bind(done);
                } else {
                    // perform the fast part of the checking logic
                    lir().checkKlassSubtypeFastPath(klassRInfo, kRInfo, rtmp1, done, stub.entry(), null, new RegisterOrConstant(-1));
                    // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                    lir().push(klassRInfo);
                    lir().push(kRInfo);
                    lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.SlowSubtypeCheck)));
                    lir().pop(klassRInfo);
                    lir().pop(kRInfo);
                    // result is a boolean
                    lir().cmpl(kRInfo, 0);
                    lir().jcc(X86Assembler.Condition.equal, stub.entry());
                    lir().bind(done);
                }

            }
            if (dst != obj) {
                lir().mov(dst, obj);
            }
        } else if (code == LIROpcode.InstanceOf) {
            X86Register obj = (X86Register) op.object().asRegister();
            X86Register kRInfo = (X86Register) op.tmp1().asRegister();
            X86Register klassRInfo = (X86Register) op.tmp2().asRegister();
            X86Register dst = (X86Register) op.result().asRegister();
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
                    lir().movoop(kRInfo, k);
                }
            }
            assert obj != kRInfo : "must be different";

            lir().verifyOop(obj);
            if (op.isFastCheck()) {
                lir().cmpptr(obj, (int) NULLWORD);
                lir().jcc(X86Assembler.Condition.equal, zero);
                // get object class
                // not a safepoint as obj null check happens earlier
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
                    lir().cmpoop(new Address(obj, compilation.runtime.klassOffsetInBytes()), k);
                    kRInfo = X86Register.noreg;
                } else {
                    lir().cmpptr(kRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));

                }
                lir().jcc(X86Assembler.Condition.equal, one);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                lir().cmpptr(obj, (int) NULLWORD);
                lir().jcc(X86Assembler.Condition.equal, zero);
                lir().movptr(klassRInfo, new Address(obj, compilation.runtime.klassOffsetInBytes()));
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
                    // See if we get an immediate positive hit
                    lir().cmpoop(new Address(klassRInfo, k.superCheckOffset()), k);
                    lir().jcc(X86Assembler.Condition.equal, one);
                    if (Util.sizeofOopDesc() + compilation.runtime.secondarySuperCacheOffsetInBytes() == k.superCheckOffset()) {
                        // check for self
                        lir().cmpoop(klassRInfo, k);
                        lir().jcc(X86Assembler.Condition.equal, one);
                        lir().push(klassRInfo);
                        lir().pushoop(k);
                        lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.SlowSubtypeCheck)));
                        lir().pop(klassRInfo);
                        lir().pop(dst);
                        lir().jmp(done);
                    }
                } else {
                    // next block is unconditional if LP64:
                    assert dst != klassRInfo && dst != kRInfo : "need 3 registers";

                    // perform the fast part of the checking logic
                    lir().checkKlassSubtypeFastPath(klassRInfo, kRInfo, dst, one, zero, null, new RegisterOrConstant(-1));
                    // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                    lir().push(klassRInfo);
                    lir().push(kRInfo);
                    lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.SlowSubtypeCheck)));
                    lir().pop(klassRInfo);
                    lir().pop(dst);
                    lir().jmp(done);
                }
            }
            lir().bind(zero);
            lir().xorptr(dst, dst);
            lir().jmp(done);
            lir().bind(one);
            lir().movptr(dst, 1);
            lir().bind(done);
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
            X86Register addr = (X86Register) op.address().asRegister();
            if (compilation.runtime.isMP()) {
                lir().lock();
            }
            lir().cmpxchg8(new Address(addr, 0));

        } else if (op.code() == LIROpcode.CasInt || op.code() == LIROpcode.CasObj) {
            assert compilation.target.arch.is64bit() || op.address().isSingleCpu() : "must be single";
            X86Register addr = (X86Register) ((op.address().isSingleCpu() ? op.address().asRegister() : op.address().asRegisterLo()));
            X86Register newval = (X86Register) op.newValue().asRegister();
            X86Register cmpval = (X86Register) op.cmpValue().asRegister();
            assert cmpval == X86Register.rax : "wrong register";
            assert newval != null : "new val must be register";
            assert cmpval != newval : "cmp and new values must be in different registers";
            assert cmpval != addr : "cmp and addr must be in different registers";
            assert newval != addr : "new value and addr must be in different registers";
            if (compilation.runtime.isMP()) {
                lir().lock();
            }
            if (op.code() == LIROpcode.CasObj) {
                lir().cmpxchgptr(newval, new Address(addr, 0));
            } else if (op.code() == LIROpcode.CasInt) {
                lir().cmpxchgl(newval, new Address(addr, 0));
            } else if (compilation.target.arch.is64bit()) {
                lir().cmpxchgq(newval, new Address(addr, 0));
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
                lir().lock();
            }
            lir().cmpxchgq((X86Register) newval, new Address(addr, 0));
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
                assert opr2.cpuRegnr() != result.cpuRegnr() : "opr2 already overwritten by previous move";
                lir().cmov(ncond, (X86Register) result.asRegister(), (X86Register) opr2.asRegister());
            } else if (opr2.isDoubleCpu()) {
                assert opr2.cpuRegnrLo() != result.cpuRegnrLo() && opr2.cpuRegnrLo() != result.cpuRegnrHi() : "opr2 already overwritten by previous move";
                assert opr2.cpuRegnrHi() != result.cpuRegnrLo() && opr2.cpuRegnrHi() != result.cpuRegnrHi() : "opr2 already overwritten by previous move";
                lir().cmovptr(ncond, (X86Register) result.asRegisterLo(), (X86Register) opr2.asRegisterLo());
                if (!compilation.target.arch.is64bit()) {
                    lir().cmovptr(ncond, (X86Register) result.asRegisterHi(), (X86Register) opr2.asRegisterHi());
                }
            } else if (opr2.isSingleStack()) {
                lir().cmovl(ncond, (X86Register) result.asRegister(), frameMap().addressForSlot(opr2.singleStackIx()));
            } else if (opr2.isDoubleStack()) {
                lir().cmovptr(ncond, (X86Register) result.asRegisterLo(), frameMap().addressForSlot(opr2.doubleStackIx(), compilation.target.arch.loWordOffsetInBytes));
                if (!compilation.target.arch.is64bit()) {
                    lir().cmovptr(ncond, (X86Register) result.asRegisterHi(), frameMap().addressForSlot(opr2.doubleStackIx(), compilation.target.arch.hiWordOffsetInBytes));
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else {
            Label skip = new Label();
            lir().jcc(acond, skip);
            if (opr2.isCpuRegister()) {
                reg2reg(opr2, result);
            } else if (opr2.isStack()) {
                stack2reg(opr2, result, result.type());
            } else if (opr2.isConstant()) {
                const2reg(opr2, result, LIRPatchCode.PatchNone, null);
            } else {
                throw Util.shouldNotReachHere();
            }
            lir().bind(skip);
        }
    }

    @Override
    protected void arithOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dest, CodeEmitInfo info, boolean popFpuStack) {
        assert info == null : "should never be used :  idiv/irem and ldiv/lrem not handled by this method";

        if (left.isSingleCpu()) {
            assert left == dest : "left and dest must be equal";
            X86Register lreg = (X86Register) left.asRegister();

            if (right.isSingleCpu()) {
                // cpu register - cpu register
                X86Register rreg = (X86Register) right.asRegister();
                switch (code) {
                    case Add:
                        lir().addl(lreg, rreg);
                        break;
                    case Sub:
                        lir().subl(lreg, rreg);
                        break;
                    case Mul:
                        lir().imull(lreg, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isStack()) {
                // cpu register - stack
                Address raddr = frameMap().addressForSlot(right.singleStackIx());
                switch (code) {
                    case Add:
                        lir().addl(lreg, raddr);
                        break;
                    case Sub:
                        lir().subl(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isConstant()) {
                // cpu register - constant
                int c = right.asConstantPtr().asJint();
                switch (code) {
                    case Add: {
                        lir().increment(lreg, c);
                        break;
                    }
                    case Sub: {
                        lir().decrement(lreg, c);
                        break;
                    }
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (left.isDoubleCpu()) {
            assert left == dest : "left and dest must be equal";
            X86Register lregLo = (X86Register) left.asRegisterLo();
            X86Register lregHi = (X86Register) left.asRegisterHi();

            if (right.isDoubleCpu()) {
                // cpu register - cpu register
                X86Register rregLo = (X86Register) right.asRegisterLo();
                X86Register rregHi = (X86Register) right.asRegisterHi();
                assert compilation.target.arch.is64bit() || Register.assertDifferentRegisters(lregLo, lregHi, rregLo, rregHi);
                assert !compilation.target.arch.is64bit() || Register.assertDifferentRegisters(lregLo, rregLo);
                switch (code) {
                    case Add:
                        lir().addptr(lregLo, rregLo);
                        if (!compilation.target.arch.is64bit()) {
                            lir().adcl(lregHi, rregHi);
                        }
                        break;
                    case Sub:
                        lir().subptr(lregLo, rregLo);
                        if (!compilation.target.arch.is64bit()) {
                            lir().sbbl(lregHi, rregHi);
                        }
                        break;
                    case Mul:
                        if (compilation.target.arch.is64bit()) {
                            lir().imulq(lregLo, rregLo);
                        } else {
                            assert lregLo == X86Register.rax && lregHi == X86Register.rdx : "must be";
                            lir().imull(lregHi, rregLo);
                            lir().imull(rregHi, lregLo);
                            lir().addl(rregHi, lregHi);
                            lir().mull(rregLo);
                            lir().addl(lregHi, rregHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }

            } else if (right.isConstant()) {
                // cpu register - constant
                if (compilation.target.arch.is64bit()) {
                    long c = right.asConstantPtr().asLongBits();
                    lir().movptr(X86Register.r10, c);
                    switch (code) {
                        case Add:
                            lir().addptr(lregLo, X86Register.r10);
                            break;
                        case Sub:
                            lir().subptr(lregLo, X86Register.r10);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    int cLo = right.asConstantPtr().asIntLo();
                    int cHi = right.asConstantPtr().asIntHi();
                    switch (code) {
                        case Add:
                            lir().addptr(lregLo, cLo);
                            lir().adcl(lregHi, cHi);
                            break;
                        case Sub:
                            lir().subptr(lregLo, cLo);
                            lir().sbbl(lregHi, cHi);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }

            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (left.isSingleXmm()) {
            assert left == dest : "left and dest must be equal";
            X86Register lreg = asXmmFloatReg(left);
            assert lreg.isXMM();

            if (right.isSingleXmm()) {
                X86Register rreg = asXmmFloatReg(right);
                assert rreg.isXMM();
                switch (code) {
                    case Add:
                        lir().addss(lreg, rreg);
                        break;
                    case Sub:
                        lir().subss(lreg, rreg);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        lir().mulss(lreg, rreg);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        lir().divss(lreg, rreg);
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
                    raddr = lir().asAddress(new InternalAddress(floatConstant(right.asJfloat()).value));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add:
                        lir().addss(lreg, raddr);
                        break;
                    case Sub:
                        lir().subss(lreg, raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        lir().mulss(lreg, raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        lir().divss(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleXmm()) {
            assert left == dest : "left and dest must be equal";

            X86Register lreg = asXmmDoubleReg(left);
            assert lreg.isXMM();
            if (right.isDoubleXmm()) {
                X86Register rreg = asXmmDoubleReg(right);
                assert rreg.isXMM();
                switch (code) {
                    case Add:
                        lir().addsd(lreg, rreg);
                        break;
                    case Sub:
                        lir().subsd(lreg, rreg);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        lir().mulsd(lreg, rreg);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        lir().divsd(lreg, rreg);
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
                    raddr = lir().asAddress(new InternalAddress(doubleConstant(right.asJdouble()).value));
                } else {
                    throw Util.shouldNotReachHere();
                }
                switch (code) {
                    case Add:
                        lir().addsd(lreg, raddr);
                        break;
                    case Sub:
                        lir().subsd(lreg, raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        lir().mulsd(lreg, raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        lir().divsd(lreg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isSingleFpu()) {
            assert dest.isSingleFpu() : "fpu stack allocation required";

            if (right.isSingleFpu()) {
                arithFpuImplementation(code, left.fpuRegnr(), right.fpuRegnr(), dest.fpuRegnr(), popFpuStack);

            } else {
                assert left.fpuRegnr() == 0 : "left must be on TOS";
                assert dest.fpuRegnr() == 0 : "dest must be on TOS";

                Address raddr;
                if (right.isSingleStack()) {
                    raddr = frameMap().addressForSlot(right.singleStackIx());
                } else if (right.isConstant()) {
                    Pointer constAddr = floatConstant(right.asJfloat());
                    assert constAddr != null : "incorrect float/double constant maintainance";
                    // hack for now
                    raddr = lir().asAddress(new InternalAddress(constAddr.value));
                } else {
                    throw Util.shouldNotReachHere();
                }

                switch (code) {
                    case Add:
                        lir().faddS(raddr);
                        break;
                    case Sub:
                        lir().fsubS(raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        lir().fmulS(raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        lir().fdivS(raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleFpu()) {
            assert dest.isDoubleFpu() : "fpu stack allocation required";

            if (code == LIROpcode.MulStrictFp || code == LIROpcode.DivStrictFp) {
                // Double values require special handling for strictfp mul/div on x86
                lir().fldX(new ExternalAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.AddrFpuSubnormalBias1)));
                lir().fmulp(left.fpuRegnrLo() + 1);
            }

            if (right.isDoubleFpu()) {
                arithFpuImplementation(code, left.fpuRegnrLo(), right.fpuRegnrLo(), dest.fpuRegnrLo(), popFpuStack);

            } else {
                assert left.fpuRegnrLo() == 0 : "left must be on TOS";
                assert dest.fpuRegnrLo() == 0 : "dest must be on TOS";

                Address raddr;
                if (right.isDoubleStack()) {
                    raddr = frameMap().addressForSlot(right.doubleStackIx());
                } else if (right.isConstant()) {
                    // hack for now
                    raddr = lir().asAddress(new InternalAddress(doubleConstant(right.asJdouble()).value));
                } else {
                    throw Util.shouldNotReachHere();
                }

                switch (code) {
                    case Add:
                        lir().faddD(raddr);
                        break;
                    case Sub:
                        lir().fsubD(raddr);
                        break;
                    case MulStrictFp: // fall through
                    case Mul:
                        lir().fmulD(raddr);
                        break;
                    case DivStrictFp: // fall through
                    case Div:
                        lir().fdivD(raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

            if (code == LIROpcode.MulStrictFp || code == LIROpcode.DivStrictFp) {
                // Double values require special handling for strictfp mul/div on x86
                lir().fldX(new ExternalAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.AddrFpuSubnormalBias2)));
                lir().fmulp(dest.fpuRegnrLo() + 1);
            }

        } else if (left.isSingleStack() || left.isAddress()) {
            assert left == dest : "left and dest must be equal";

            Address laddr;
            if (left.isSingleStack()) {
                laddr = frameMap().addressForSlot(left.singleStackIx());
            } else if (left.isAddress()) {
                laddr = asAddress(left.asAddressPtr());
            } else {
                throw Util.shouldNotReachHere();
            }

            if (right.isSingleCpu()) {
                X86Register rreg = (X86Register) right.asRegister();
                switch (code) {
                    case Add:
                        lir().addl(laddr, rreg);
                        break;
                    case Sub:
                        lir().subl(laddr, rreg);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isConstant()) {
                int c = right.asConstantPtr().asJint();
                switch (code) {
                    case Add: {
                        lir().incrementl(laddr, c);
                        break;
                    }
                    case Sub: {
                        lir().decrementl(laddr, c);
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
                    lir().faddp(nonTosIndex);
                } else if (destIsTos) {
                    lir().fadd(nonTosIndex);
                } else {
                    lir().fadda(nonTosIndex);
                }
                break;

            case Sub:
                if (leftIsTos) {
                    if (popFpuStack) {
                        lir().fsubrp(nonTosIndex);
                    } else if (destIsTos) {
                        lir().fsub(nonTosIndex);
                    } else {
                        lir().fsubra(nonTosIndex);
                    }
                } else {
                    if (popFpuStack) {
                        lir().fsubp(nonTosIndex);
                    } else if (destIsTos) {
                        lir().fsubr(nonTosIndex);
                    } else {
                        lir().fsuba(nonTosIndex);
                    }
                }
                break;

            case MulStrictFp: // fall through
            case Mul:
                if (popFpuStack) {
                    lir().fmulp(nonTosIndex);
                } else if (destIsTos) {
                    lir().fmul(nonTosIndex);
                } else {
                    lir().fmula(nonTosIndex);
                }
                break;

            case DivStrictFp: // fall through
            case Div:
                if (leftIsTos) {
                    if (popFpuStack) {
                        lir().fdivrp(nonTosIndex);
                    } else if (destIsTos) {
                        lir().fdiv(nonTosIndex);
                    } else {
                        lir().fdivra(nonTosIndex);
                    }
                } else {
                    if (popFpuStack) {
                        lir().fdivp(nonTosIndex);
                    } else if (destIsTos) {
                        lir().fdivr(nonTosIndex);
                    } else {
                        lir().fdiva(nonTosIndex);
                    }
                }
                break;

            case Rem:
                assert leftIsTos && destIsTos && rightIndex == 1 : "must be guaranteed by FPU stack allocation";
                lir().fremr(X86Register.noreg);
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
                    if (dest.asXmmDoubleReg() != value.asXmmDoubleReg()) {
                        lir().movdbl((X86Register) dest.asXmmDoubleReg(), (X86Register) value.asXmmDoubleReg());
                    }
                    lir().andpd((X86Register) dest.asXmmDoubleReg(), new ExternalAddress(compilation.runtime.doubleSignmaskPoolAddress()));
                    break;

                case Sqrt:
                    lir().sqrtsd((X86Register) dest.asXmmDoubleReg(), (X86Register) value.asXmmDoubleReg());
                    break;
                // all other intrinsics are not available in the SSE instruction set, so FPU is used
                default:
                    throw Util.shouldNotReachHere();
            }

        } else if (value.isDoubleFpu()) {
            assert value.fpuRegnrLo() == 0 && dest.fpuRegnrLo() == 0 : "both must be on TOS";
            switch (code) {
                case Log:
                    lir().flog();
                    break;
                case Log10:
                    lir().flog10();
                    break;
                case Abs:
                    lir().fabs();
                    break;
                case Sqrt:
                    lir().fsqrt();
                    break;
                case Sin:
                    // Should consider not saving rbx, if not necessary
                    lir().trigfunc('s', op.fpuStackSize());
                    break;
                case Cos:
                    // Should consider not saving rbx, if not necessary
                    assert op.fpuStackSize() <= 6 : "sin and cos need two free stack slots";
                    lir().trigfunc('c', op.fpuStackSize());
                    break;
                case Tan:
                    // Should consider not saving rbx, if not necessary
                    lir().trigfunc('t', op.fpuStackSize());
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
            X86Register reg = (X86Register) left.asRegister();
            if (right.isConstant()) {
                int val = right.asConstantPtr().asJint();
                switch (code) {
                    case LogicAnd:
                        lir().andl(reg, val);
                        break;
                    case LogicOr:
                        lir().orl(reg, val);
                        break;
                    case LogicXor:
                        lir().xorl(reg, val);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else if (right.isStack()) {
                // added support for stack operands
                Address raddr = frameMap().addressForSlot(right.singleStackIx());
                switch (code) {
                    case LogicAnd:
                        lir().andl(reg, raddr);
                        break;
                    case LogicOr:
                        lir().orl(reg, raddr);
                        break;
                    case LogicXor:
                        lir().xorl(reg, raddr);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {
                X86Register rright = (X86Register) right.asRegister();
                switch (code) {
                    case LogicAnd:
                        lir().andptr(reg, rright);
                        break;
                    case LogicOr:
                        lir().orptr(reg, rright);
                        break;
                    case LogicXor:
                        lir().xorptr(reg, rright);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, (X86Register) dst.asRegister());
        } else {
            X86Register lLo = (X86Register) left.asRegisterLo();
            X86Register lHi = (X86Register) left.asRegisterHi();
            if (right.isConstant()) {
                if (compilation.target.arch.is64bit()) {
                    lir().mov64(rscratch1, right.asConstantPtr().asLong());
                    switch (code) {
                        case LogicAnd:
                            lir().andq(lLo, rscratch1);
                            break;
                        case LogicOr:
                            lir().orq(lLo, rscratch1);
                            break;
                        case LogicXor:
                            lir().xorq(lLo, rscratch1);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                } else {
                    int rLo = right.asConstantPtr().asIntLo();
                    int rHi = right.asConstantPtr().asIntHi();
                    switch (code) {
                        case LogicAnd:
                            lir().andl(lLo, rLo);
                            lir().andl(lHi, rHi);
                            break;
                        case LogicOr:
                            lir().orl(lLo, rLo);
                            lir().orl(lHi, rHi);
                            break;
                        case LogicXor:
                            lir().xorl(lLo, rLo);
                            lir().xorl(lHi, rHi);
                            break;
                        default:
                            throw Util.shouldNotReachHere();
                    }
                }
            } else {
                X86Register rLo = (X86Register) right.asRegisterLo();
                X86Register rHi = (X86Register) right.asRegisterHi();
                assert lLo != rHi : "overwriting registers";
                switch (code) {
                    case LogicAnd:
                        lir().andptr(lLo, rLo);
                        if (!compilation.target.arch.is64bit()) {
                            lir().andptr(lHi, rHi);
                        }
                        break;
                    case LogicOr:
                        lir().orptr(lLo, rLo);
                        if (!compilation.target.arch.is64bit()) {
                            lir().orptr(lHi, rHi);
                        }
                        break;
                    case LogicXor:
                        lir().xorptr(lLo, rLo);

                        if (!compilation.target.arch.is64bit()) {
                            lir().xorptr(lHi, rHi);
                        }
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            }

            X86Register dstLo = (X86Register) dst.asRegisterLo();
            X86Register dstHi = (X86Register) dst.asRegisterHi();

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

        X86Register lreg = (X86Register) left.asRegister();
        X86Register dreg = (X86Register) result.asRegister();

        if (right.isConstant()) {
            int divisor = right.asConstantPtr().asJint();
            assert divisor > 0 && Util.isPowerOf2(divisor) : "must be";
            if (code == LIROpcode.Div) {
                assert lreg == X86Register.rax : "must be rax : ";
                assert temp.asRegister() == X86Register.rdx : "tmp register must be rdx";
                lir().cdql(); // sign extend into rdx:rax
                if (divisor == 2) {
                    lir().subl(lreg, X86Register.rdx);
                } else {
                    lir().andl(X86Register.rdx, divisor - 1);
                    lir().addl(lreg, X86Register.rdx);
                }
                lir().sarl(lreg, Util.log2(divisor));
                moveRegs(lreg, dreg);
            } else if (code == LIROpcode.Rem) {
                Label done = new Label();
                lir().mov(dreg, lreg);
                lir().andl(dreg, 0x80000000 | (divisor - 1));
                lir().jcc(X86Assembler.Condition.positive, done);
                lir().decrement(dreg, 1);
                lir().orl(dreg, ~(divisor - 1));
                lir().increment(dreg, 1);
                lir().bind(done);
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            X86Register rreg = (X86Register) right.asRegister();
            assert lreg == X86Register.rax : "left register must be rax : ";
            assert rreg != X86Register.rdx : "right register must not be rdx";
            assert temp.asRegister() == X86Register.rdx : "tmp register must be rdx";

            moveRegs(lreg, X86Register.rax);

            int idivlOffset = lir().correctedIdivl(rreg);
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
            X86Register reg1 = (X86Register) opr1.asRegister();
            if (opr2.isSingleCpu()) {
                // cpu register - cpu register
                if (opr1.type() == BasicType.Object) {
                    lir().cmpptr(reg1, (X86Register) opr2.asRegister());
                } else {
                    assert opr2.type() != BasicType.Object : "cmp int :  oop?";
                    lir().cmpl(reg1, (X86Register) opr2.asRegister());
                }
            } else if (opr2.isStack()) {
                // cpu register - stack
                if (opr1.type() == BasicType.Object) {
                    lir().cmpptr(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
                } else {
                    lir().cmpl(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
                }
            } else if (opr2.isConstant()) {
                // cpu register - constant
                LIRConstant c = opr2.asConstantPtr();
                if (c.type() == BasicType.Int) {
                    lir().cmpl(reg1, c.asJint());
                } else if (c.type() == BasicType.Object) {
                    // In 64bit oops are single register
                    Object o = c.asJobject();
                    if (o == null) {
                        lir().cmpptr(reg1, (int) NULLWORD);
                    } else {
                        if (compilation.target.arch.is64bit()) {
                            lir().movoop(rscratch1, o);
                            lir().cmpptr(reg1, rscratch1);
                        } else {
                            lir().cmpoop(reg1, c.asJobject());
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
                lir().cmpl(reg1, asAddress(opr2.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleCpu()) {
            X86Register xlo = (X86Register) opr1.asRegisterLo();
            X86Register xhi = (X86Register) opr1.asRegisterHi();
            if (opr2.isDoubleCpu()) {
                if (compilation.target.arch.is64bit()) {
                    lir().cmpptr(xlo, (X86Register) opr2.asRegisterLo());
                } else {
                    // cpu register - cpu register
                    X86Register ylo = (X86Register) opr2.asRegisterLo();
                    X86Register yhi = (X86Register) opr2.asRegisterHi();
                    lir().subl(xlo, ylo);
                    lir().sbbl(xhi, yhi);
                    if (condition == LIRCondition.Equal || condition == LIRCondition.NotEqual) {
                        lir().orl(xhi, xlo);
                    }
                }
            } else if (opr2.isConstant()) {
                // cpu register - constant 0
                assert opr2.asLong() == 0 : "only handles zero";
                if (compilation.target.arch.is64bit()) {
                    lir().cmpptr(xlo, (int) opr2.asLong());
                } else {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "only handles equals case";
                    lir().orl(xhi, xlo);
                }
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleXmm()) {
            X86Register reg1 = asXmmFloatReg(opr1);
            assert reg1.isXMM();
            if (opr2.isSingleXmm()) {
                // xmm register - xmm register
                lir().ucomiss(reg1, asXmmFloatReg(opr2));
            } else if (opr2.isStack()) {
                // xmm register - stack
                lir().ucomiss(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                lir().ucomiss(reg1, new InternalAddress(floatConstant(opr2.asJfloat()).value));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                lir().ucomiss(reg1, asAddress(opr2.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleXmm()) {
            X86Register reg1 = asXmmDoubleReg(opr1);
            assert reg1.isXMM();
            if (opr2.isDoubleXmm()) {
                // xmm register - xmm register
                lir().ucomisd(reg1, asXmmDoubleReg(opr2));
            } else if (opr2.isStack()) {
                // xmm register - stack
                lir().ucomisd(reg1, frameMap().addressForSlot(opr2.doubleStackIx()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                lir().ucomisd(reg1, new InternalAddress(doubleConstant(opr2.asJdouble()).value));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                lir().ucomisd(reg1, asAddress(opr2.asAddress()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleFpu() || opr1.isDoubleFpu()) {
            assert opr1.isFpuRegister() && opr1.fpu() == 0 : "currently left-hand side must be on TOS (relax this restriction)";
            assert opr2.isFpuRegister() : "both must be registers";
            lir().fcmp(X86Register.noreg, opr2.fpu(), op.fpuPopCount() > 0, op.fpuPopCount() > 1);

        } else if (opr1.isAddress() && opr2.isConstant()) {
            LIRConstant c = opr2.asConstantPtr();

            if (compilation.target.arch.is64bit()) {
                if (c.type() == BasicType.Object) {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "need to reverse";
                    lir().movoop(rscratch1, c.asJobject());
                }
            }
            if (op.info() != null) {
                addDebugInfoForNullCheckHere(op.info());
            }
            // special case: address - constant
            LIRAddress addr = opr1.asAddressPtr();
            if (c.type() == BasicType.Int) {
                lir().cmpl(asAddress(addr), c.asJint());
            } else if (c.type() == BasicType.Object) {
                if (compilation.target.arch.is64bit()) {
                    // %%% Make this explode if addr isn't reachable until we figure out a
                    // better strategy by giving X86Register.noreg as the temp for asAddress
                    lir().cmpptr(rscratch1, asAddress(addr, X86Register.noreg));
                } else {
                    lir().cmpoop(asAddress(addr), c.asJobject());
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
                lir().cmpss2int(asXmmFloatReg(left), asXmmFloatReg(right), (X86Register) dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else if (left.isDoubleXmm()) {
                assert right.isDoubleXmm() : "must match";
                lir().cmpsd2int(asXmmDoubleReg(left), asXmmDoubleReg(right), (X86Register) dst.asRegister(), code == LIROpcode.Ucmpfd2i);

            } else {
                assert left.isSingleFpu() || left.isDoubleFpu() : "must be";
                assert right.isSingleFpu() || right.isDoubleFpu() : "must match";

                assert left.fpu() == 0 : "left must be on TOS";
                lir().fcmp2int((X86Register) dst.asRegister(), code == LIROpcode.Ucmpfd2i, right.fpu(), op.fpuPopCount() > 0, op.fpuPopCount() > 1);
            }
        } else {
            assert code == LIROpcode.Cmpl2i;
            if (compilation.target.arch.is64bit()) {
                X86Register dest = (X86Register) dst.asRegister();
                lir().xorptr(dest, dest);
                Label high = new Label();
                Label done = new Label();
                lir().cmpptr((X86Register) left.asRegisterLo(), (X86Register) right.asRegisterLo());
                lir().jcc(X86Assembler.Condition.equal, done);
                lir().jcc(X86Assembler.Condition.greater, high);
                lir().decrement(dest, 1);
                lir().jmp(done);
                lir().bind(high);
                lir().increment(dest, 1);

                lir().bind(done);

            } else {
                lir().lcmp2int((X86Register) left.asRegisterHi(), (X86Register) left.asRegisterLo(), (X86Register) right.asRegisterHi(), (X86Register) right.asRegisterLo());
                moveRegs((X86Register) left.asRegisterHi(), (X86Register) dst.asRegister());
            }
        }
    }

    @Override
    protected void alignCall(LIROpcode code) {
        if (compilation.runtime.isMP()) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = lir().offset();
            switch (code) {
                case StaticCall:
                case OptVirtualCall:
                    offset += compilation.runtime.nativeCallDisplacementOffset();
                    break;
                case IcVirtualCall:
                    offset += compilation.runtime.nativeCallDisplacementOffset() + compilation.runtime.nativeCallInstructionSize();
                    break;
                case VirtualCall: // currently, sparc-specific for niagara
                default:
                    throw Util.shouldNotReachHere();
            }
            while (offset++ % wordSize != 0) {
                lir().nop();
            }
        }
    }

    @Override
    protected void call(long entry, RelocInfo.Type rtype, CodeEmitInfo info) {
        assert !compilation.runtime.isMP() || (lir().offset() + compilation.runtime.nativeCallDisplacementOffset()) % wordSize == 0 : "must be aligned";
        lir().call(new AddressLiteral(entry, rtype));
        addCallInfo(codeOffset(), info);
    }

    @Override
    protected void icCall(long entry, CodeEmitInfo info) {
        RelocationHolder rh = RelocationHolder.virtualCallRelocationSpec(pc());
        lir().movoop(ICKlass, compilation.runtime.universeNonOopWord());
        assert !compilation.runtime.isMP() || (lir().offset() + compilation.runtime.nativeCallDisplacementOffset()) % wordSize == 0 : "must be aligned";
        lir().call(new AddressLiteral(entry, rh));
        addCallInfo(codeOffset(), info);
    }

    @Override
    protected void vtableCall(long vtableOffset, CodeEmitInfo info) {
        throw Util.shouldNotReachHere();
    }

    @Override
    protected void emitRTCall(LIRRTCall op) {
      rtCall(op.result(), op.address(), op.arguments(), op.tmp(), op.info());
    }

    @Override
    protected void emitStaticCallStub() {
        Pointer callPc = lir().pc();
        Pointer stub = lir().startAStub(callStubSize);
        if (stub == null) {
            throw new Bailout("static call stub overflow");
        }

        int start = lir().offset();
        if (compilation.runtime.isMP()) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = lir().offset() + compilation.runtime.nativeMovConstRegInstructionSize() + compilation.runtime.nativeCallDisplacementOffset();
            while (offset++ % wordSize != 0) {
                lir().nop();
            }
        }
        lir().relocate(RelocationHolder.staticStubRelocationSpec(callPc));
        lir().movoop(X86Register.rbx, null);
        // must be set to -1 at code generation time
        assert !compilation.runtime.isMP() || ((lir().offset() + 1) % wordSize) == 0 : "must be aligned on MP";
        // On 64bit this will die since it will take a movq & jmp, must be only a jmp
        lir().jump(new RuntimeAddress(lir().pc().value));

        assert lir().offset() - start <= callStubSize : "stub too big";
        lir().endAStub();
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
            int pcForAthrowOffset = lir().offset();
            InternalAddress pcForAthrow = new InternalAddress(lir().pc().value);
            lir().lea((X86Register) exceptionPC.asRegister(), pcForAthrow);
            addCallInfo(pcForAthrowOffset, info); // for exception handler

            lir().verifyNotNullOop(X86Register.rax);
            // search an exception handler (rax: exception oop, rdx: throwing pc)
            if (compilation().hasFpuCode()) {
                unwindId = CiRuntimeCall.HandleException;
            } else {
                unwindId = CiRuntimeCall.HandleExceptionNofpu;
            }
        } else {
            unwindId = CiRuntimeCall.UnwindException;
        }
        lir().call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(unwindId)));

        // enough room for two byte trap
        lir().nop();
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
            X86Register value = (X86Register) left.asRegister();
            assert value != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case Shl:
                    lir().shll(value);
                    break;
                case Shr:
                    lir().sarl(value);
                    break;
                case Ushr:
                    lir().shrl(value);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (left.isDoubleCpu()) {
            X86Register lo = (X86Register) left.asRegisterLo();
            X86Register hi = (X86Register) left.asRegisterHi();
            assert lo != SHIFTCount && hi != SHIFTCount : "left cannot be ECX";

            if (compilation.target.arch.is64bit()) {
                switch (code) {
                    case Shl:
                        lir().shlptr(lo);
                        break;
                    case Shr:
                        lir().sarptr(lo);
                        break;
                    case Ushr:
                        lir().shrptr(lo);
                        break;
                    default:
                        throw Util.shouldNotReachHere();
                }
            } else {

                switch (code) {
                    case Shl:
                        lir().lshl(hi, lo);
                        break;
                    case Shr:
                        lir().lshr(hi, lo, true);
                        break;
                    case Ushr:
                        lir().lshr(hi, lo, false);
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
            X86Register value = (X86Register) dest.asRegister();
            count = count & 0x1F; // Java spec

            moveRegs((X86Register) left.asRegister(), value);
            switch (code) {
                case Shl:
                    lir().shll(value, count);
                    break;
                case Shr:
                    lir().sarl(value, count);
                    break;
                case Ushr:
                    lir().shrl(value, count);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (dest.isDoubleCpu()) {

            if (!compilation.target.arch.is64bit()) {
                throw Util.shouldNotReachHere();
            }

            // first move left into dest so that left is not destroyed by the shift
            X86Register value = (X86Register) dest.asRegisterLo();
            count = count & 0x1F; // Java spec

            moveRegs((X86Register) left.asRegisterLo(), value);
            switch (code) {
                case Shl:
                    lir().shlptr(value, count);
                    break;
                case Shr:
                    lir().sarptr(value, count);
                    break;
                case Ushr:
                    lir().shrptr(value, count);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private void storeParameter(X86Register r, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        lir().movptr(new Address(X86Register.rsp, offsetFromRspInBytes), r);
    }

    void storeParameter(int c, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        lir().movptr(new Address(X86Register.rsp, offsetFromRspInBytes), c);
    }

    void storeParameter(Object o, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        lir().movoop(new Address(X86Register.rsp, offsetFromRspInBytes), o);
    }

    @Override
    protected void emitArrayCopy(LIRArrayCopy op) {
        CiType defaultType = op.expectedType();
        X86Register src = (X86Register) op.src().asRegister();
        X86Register dst = (X86Register) op.dst().asRegister();
        X86Register srcPos = (X86Register) op.srcPos().asRegister();
        X86Register dstPos = (X86Register) op.dstPos().asRegister();
        X86Register length = (X86Register) op.length().asRegister();
        X86Register tmp = (X86Register) op.tmp().asRegister();

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

            long entry = compilation.runtime.getRuntimeEntry(CiRuntimeCall.ArrayCopy);

            // pass arguments: may push as this is not a safepoint; SP must be fix at each safepoint
            if (compilation.target.arch.is64bit()) {
                // The arguments are in java calling convention so we can trivially shift them to C
                // convention
                assert Register.assertDifferentRegisters(masm.cRarg0, masm.jRarg1, masm.jRarg2, masm.jRarg3, masm.jRarg4);
                lir().mov(masm.cRarg0, masm.jRarg0);
                assert Register.assertDifferentRegisters(masm.cRarg1, masm.jRarg2, masm.jRarg3, masm.jRarg4);
                lir().mov(masm.cRarg1, masm.jRarg1);
                assert Register.assertDifferentRegisters(masm.cRarg2, masm.jRarg3, masm.jRarg4);
                lir().mov(masm.cRarg2, masm.jRarg2);
                assert Register.assertDifferentRegisters(masm.cRarg3, masm.jRarg4);
                lir().mov(masm.cRarg3, masm.jRarg3);
                if (compilation.target.isWindows()) {
                    // Allocate abi space for args but be sure to keep stack aligned
                    lir().subptr(X86Register.rsp, 6 * compilation.target.arch.wordSize);
                    storeParameter(masm.jRarg4, 4);
                    lir().call(new RuntimeAddress(entry));
                    lir().addptr(X86Register.rsp, 6 * compilation.target.arch.wordSize);
                } else {
                    lir().mov(masm.cRarg4, masm.jRarg4);
                    lir().call(new RuntimeAddress(entry));
                }
            } else {
                lir().push(length);
                lir().push(dstPos);
                lir().push(dst);
                lir().push(srcPos);
                lir().push(src);
                lir().callVMLeaf(entry, 5); // removes pushed parameter from the stack

            }

            lir().cmpl(X86Register.rax, 0);
            lir().jcc(X86Assembler.Condition.equal, stub.continuation());

            // Reload values from the stack so they are where the stub
            // expects them.
            lir().movptr(dst, new Address(X86Register.rsp, 0 * wordSize));
            lir().movptr(dstPos, new Address(X86Register.rsp, 1 * wordSize));
            lir().movptr(length, new Address(X86Register.rsp, 2 * wordSize));
            lir().movptr(srcPos, new Address(X86Register.rsp, 3 * wordSize));
            lir().movptr(src, new Address(X86Register.rsp, 4 * wordSize));
            lir().jmp(stub.entry());

            lir().bind(stub.continuation());
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
            lir().testptr(src, src);
            lir().jcc(X86Assembler.Condition.zero, stub.entry());
        }
        if ((flags & LIRArrayCopy.Flags.DstNullCheck.mask()) != 0) {
            lir().testptr(dst, dst);
            lir().jcc(X86Assembler.Condition.zero, stub.entry());
        }

        // check if negative
        if ((flags & LIRArrayCopy.Flags.SrcPosPositiveCheck.mask()) != 0) {
            lir().testl(srcPos, srcPos);
            lir().jcc(X86Assembler.Condition.less, stub.entry());
        }
        if ((flags & LIRArrayCopy.Flags.DstPosPositiveCheck.mask()) != 0) {
            lir().testl(dstPos, dstPos);
            lir().jcc(X86Assembler.Condition.less, stub.entry());
        }
        if ((flags & LIRArrayCopy.Flags.LengthPositiveCheck.mask()) != 0) {
            lir().testl(length, length);
            lir().jcc(X86Assembler.Condition.less, stub.entry());
        }

        if ((flags & LIRArrayCopy.Flags.SrcRangeCheck.mask()) != 0) {
            lir().lea(tmp, new Address(srcPos, length, ScaleFactor.times1, 0));
            lir().cmpl(tmp, srcLengthAddr);
            lir().jcc(X86Assembler.Condition.above, stub.entry());
        }
        if ((flags & LIRArrayCopy.Flags.DstRangeCheck.mask()) != 0) {
            lir().lea(tmp, new Address(dstPos, length, ScaleFactor.times1, 0));
            lir().cmpl(tmp, dstLengthAddr);
            lir().jcc(X86Assembler.Condition.above, stub.entry());
        }

        if ((flags & LIRArrayCopy.Flags.TypeCheck.mask()) != 0) {
            lir().movptr(tmp, srcKlassAddr);
            lir().cmpptr(tmp, dstKlassAddr);
            lir().jcc(X86Assembler.Condition.notEqual, stub.entry());
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
                lir().movoop(tmp, defaultType.encoding());
                if (basicType != BasicType.Object) {
                    lir().cmpptr(tmp, dstKlassAddr);
                    lir().jcc(X86Assembler.Condition.notEqual, halt);
                    lir().cmpptr(tmp, srcKlassAddr);
                    lir().jcc(X86Assembler.Condition.equal, knownOk);
                } else {
                    lir().cmpptr(tmp, dstKlassAddr);
                    lir().jcc(X86Assembler.Condition.equal, knownOk);
                    lir().cmpptr(src, dst);
                    lir().jcc(X86Assembler.Condition.equal, knownOk);
                }
                lir().bind(halt);
                lir().stop("incorrect type information in arraycopy");
                lir().bind(knownOk);
            }
        }

        if (shiftAmount > 0 && basicType != BasicType.Object) {
            lir().shlptr(length, shiftAmount);
        }

        if (compilation.target.arch.is64bit()) {
            assert Register.assertDifferentRegisters(masm.cRarg0, dst, dstPos, length);
            lir().lea(masm.cRarg0, new Address(src, srcPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            assert Register.assertDifferentRegisters(masm.cRarg1, length);
            lir().lea(masm.cRarg1, new Address(dst, dstPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            lir().mov(masm.cRarg2, length);

        } else {
            lir().lea(tmp, new Address(src, srcPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            storeParameter(tmp, 0);
            lir().lea(tmp, new Address(dst, dstPos, scale, compilation.runtime.arrayBaseOffsetInBytes(basicType)));
            storeParameter(tmp, 1);
            storeParameter(length, 2);
        }
        if (basicType == BasicType.Object) {
            lir().callVMLeaf(compilation.runtime.getRuntimeEntry(CiRuntimeCall.OopArrayCopy), 0);
        } else {
            lir().callVMLeaf(compilation.runtime.getRuntimeEntry(CiRuntimeCall.PrimitiveArrayCopy), 0);
        }

        lir().bind(stub.continuation());
    }

    @Override
    protected void emitLock(LIRLock op) {
        X86Register obj = (X86Register) op.objOpr().asRegister(); // may not be an oop
        X86Register hdr = (X86Register) op.hdrOpr().asRegister();
        X86Register lock = (X86Register) op.lockOpr().asRegister();
        if (!C1XOptions.UseFastLocking) {
            lir().jmp(op.stub().entry());
        } else if (op.code() == LIROpcode.Lock) {
            X86Register scratch = X86Register.noreg;
            if (C1XOptions.UseBiasedLocking) {
                scratch = (X86Register) op.scratchOpr().asRegister();
            }
            assert compilation.runtime.basicLockDisplacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            // add debug info for NullPointerException only if one is possible
            int nullCheckOffset = lir().lockObject(hdr, obj, lock, scratch, op.stub().entry());
            if (op.info() != null) {
                addDebugInfoForNullCheck(nullCheckOffset, op.info());
            }
            // done
        } else if (op.code() == LIROpcode.Unlock) {
            assert compilation.runtime.basicLockDisplacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            lir().unlockObject(hdr, obj, lock, op.stub().entry());
        } else {
            throw Util.shouldNotReachHere();
        }
        lir().bind(op.stub().continuation());
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
        X86Register mdo = (X86Register) op.mdo().asRegister();
        lir().movoop(mdo, md.encoding());
        Address counterAddr = new Address(mdo, md.countOffset(bci));
        lir().addl(counterAddr, 1);
        int bc = method.javaCodeAtBci(bci);
        // Perform additional virtual call profiling for invokevirtual and
        // invokeinterface bytecodes
        if ((bc == Bytecodes.INVOKEVIRTUAL || bc == Bytecodes.INVOKEINTERFACE) && C1XOptions.Tier1ProfileVirtualCalls) {
            assert op.recv().isSingleCpu() : "recv must be allocated";
            X86Register recv = (X86Register) op.recv().asRegister();
            assert Register.assertDifferentRegisters(mdo, recv);
            CiType knownKlass = op.knownHolder();
            if (C1XOptions.Tier1OptimizeVirtualCallProfiling && knownKlass != null) {
                // We know the type that will be seen at this call site; we can
                // statically update the methodDataOop rather than needing to do
                // dynamic tests on the receiver type

                // NOTE: we should probably put a lock around this search to
                // avoid collisions by concurrent compilations
                for (int i = 0; i < C1XOptions.TypeProfileWidth; i++) {
                    CiType receiver = md.receiver(bci, i);
                    if (knownKlass.equals(receiver)) {
                        Address dataAddr = new Address(mdo, md.receiverCountOffset(bci, i));
                        lir().addl(dataAddr, 1);
                        return;
                    }
                }

                // Receiver type not found in profile data; select an empty slot

                // Note that this is less efficient than it should be because it
                // always does a write to the receiver part of the
                // VirtualCallData rather than just the first time
                for (int i = 0; i < C1XOptions.TypeProfileWidth; i++) {
                    CiType receiver = md.receiver(bci, i);
                    if (receiver == null) {
                        Address recvAddr = new Address(mdo, md.receiverOffset(bci, i));
                        lir().movoop(recvAddr, knownKlass.encoding());
                        Address dataAddr = new Address(mdo, md.receiverCountOffset(bci, i));
                        lir().addl(dataAddr, 1);
                        return;
                    }
                }
            } else {
                lir().movptr(recv, new Address(recv, compilation.runtime.klassOffsetInBytes()));
                Label updateDone = new Label();
                for (int i = 0; i < C1XOptions.TypeProfileWidth; i++) {
                    Label nextTest = new Label();
                    // See if the receiver is receiver[n].
                    lir().cmpptr(recv, new Address(mdo, md.receiverOffset(bci, i)));
                    lir().jcc(X86Assembler.Condition.notEqual, nextTest);
                    Address dataAddr = new Address(mdo, md.receiverCountOffset(bci, i));
                    lir().addl(dataAddr, 1);
                    lir().jmp(updateDone);
                    lir().bind(nextTest);
                }

                // Didn't find receiver; find next empty slot and fill it in
                for (int i = 0; i < C1XOptions.TypeProfileWidth; i++) {
                    Label nextTest = new Label();
                    Address recvAddr = new Address(mdo, md.receiverOffset(bci, i));
                    lir().cmpptr(recvAddr, (int) NULLWORD);
                    lir().jcc(X86Assembler.Condition.notEqual, nextTest);
                    lir().movptr(recvAddr, recv);
                    lir().movl(new Address(mdo, md.receiverCountOffset(bci, i)), 1);
                    if (i < (C1XOptions.TypeProfileWidth - 1)) {
                        lir().jmp(updateDone);
                    }
                    lir().bind(nextTest);
                }

                lir().bind(updateDone);
            }
        }
    }

    @Override
    protected void emitDelay(LIRDelay lirDelay) {
        throw Util.shouldNotReachHere();
    }

    @Override
    protected void monitorAddress(int monitorNo, LIROperand dst) {
        lir().lea((X86Register) dst.asRegister(), frameMap().addressForMonitorLock(monitorNo));
    }

    @Override
    protected void alignBackwardBranchTarget() {
        lir().align(compilation.target.arch.wordSize);
    }

    @Override
    protected void negate(LIROperand left, LIROperand dest) {
        if (left.isSingleCpu()) {
            lir().negl((X86Register) left.asRegister());
            moveRegs((X86Register) left.asRegister(), (X86Register) dest.asRegister());

        } else if (left.isDoubleCpu()) {
            X86Register lo = (X86Register) left.asRegisterLo();
            if (compilation.target.arch.is64bit()) {
                X86Register dst = (X86Register) dest.asRegisterLo();
                lir().movptr(dst, lo);
                lir().negptr(dst);
            } else {
                X86Register hi = (X86Register) left.asRegisterHi();
                lir().lneg(hi, lo);
                if (dest.asRegisterLo() == hi) {
                    assert dest.asRegisterHi() != lo : "destroying register";
                    moveRegs(hi, (X86Register) dest.asRegisterHi());
                    moveRegs(lo, (X86Register) dest.asRegisterLo());
                } else {
                    moveRegs(lo, (X86Register) dest.asRegisterLo());
                    moveRegs(hi, (X86Register) dest.asRegisterHi());
                }
            }

        } else if (dest.isSingleXmm()) {
            if (left.asXmmFloatReg() != dest.asXmmFloatReg()) {
                lir().movflt(asXmmFloatReg(dest), asXmmFloatReg(left));
            }
            lir().xorps(asXmmFloatReg(dest), new ExternalAddress(compilation.runtime.floatSignflipPoolAddress()));

        } else if (dest.isDoubleXmm()) {
            if (left.asXmmDoubleReg() != dest.asXmmDoubleReg()) {
                lir().movdbl(asXmmDoubleReg(dest), asXmmDoubleReg(left));
            }
            lir().xorpd(asXmmDoubleReg(dest), new ExternalAddress(compilation.runtime.doubleSignflipPoolAddress()));

        } else if (left.isSingleFpu() || left.isDoubleFpu()) {
            assert left.fpu() == 0 : "arg must be on TOS";
            assert dest.fpu() == 0 : "dest must be TOS";
            lir().fchs();

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void leal(LIROperand addr, LIROperand dest) {
        assert addr.isAddress() && dest.isRegister() : "check";
        X86Register reg = (X86Register) dest.asPointerRegister(compilation.target.arch);
        lir().lea(reg, asAddress(addr.asAddressPtr()));
    }

    @Override
    protected void rtCall(LIROperand result, long dest, List<LIROperand> args, LIROperand tmp, CodeEmitInfo info) {
        assert !tmp.isValid() : "don't need temporary";
        lir().call(new RuntimeAddress(dest));
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
                    lir().movdq((X86Register) dest.asRegisterLo(), asXmmDoubleReg(src));
                } else {
                    lir().movdl((X86Register) dest.asRegisterLo(), asXmmDoubleReg(src));
                    lir().psrlq(asXmmDoubleReg(src), 32);
                    lir().movdl((X86Register) dest.asRegisterHi(), asXmmDoubleReg(src));
                }
            } else if (dest.isDoubleStack()) {
                lir().movdbl(frameMap().addressForSlot(dest.doubleStackIx()), asXmmDoubleReg(src));
            } else if (dest.isAddress()) {
                lir().movdbl(asAddress(dest.asAddressPtr()), asXmmDoubleReg(src));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleXmm()) {
            if (src.isDoubleStack()) {
                lir().movdbl(asXmmDoubleReg(dest), frameMap().addressForSlot(src.doubleStackIx()));
            } else if (src.isAddress()) {
                lir().movdbl(asXmmDoubleReg(dest), asAddress(src.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isDoubleFpu()) {
            assert src.fpuRegnrLo() == 0 : "must be TOS";
            if (dest.isDoubleStack()) {
                lir().fistpD(frameMap().addressForSlot(dest.doubleStackIx()));
            } else if (dest.isAddress()) {
                lir().fistpD(asAddress(dest.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleFpu()) {
            assert dest.fpuRegnrLo() == 0 : "must be TOS";
            if (src.isDoubleStack()) {
                lir().fildD(frameMap().addressForSlot(src.doubleStackIx()));
            } else if (src.isAddress()) {
                lir().fildD(asAddress(src.asAddressPtr()));
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private X86Register asXmmDoubleReg(LIROperand dest) {
        return (X86Register) dest.asXmmDoubleReg();
    }

    @Override
    protected void membar() {
        // QQQ sparc TSO uses this,
        lir().membar(X86Assembler.MembarMaskBits.StoreLoad.mask());

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
            lir().mov((X86Register) resultReg.asRegister(), X86FrameMap.r15thread);
        } else {
            lir().getThread((X86Register) resultReg.asRegister());
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
                    shiftOp(op.code(), op.inOpr1(), op.inOpr2().asConstantPtr().asJint(), op.resultOpr());
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
}
