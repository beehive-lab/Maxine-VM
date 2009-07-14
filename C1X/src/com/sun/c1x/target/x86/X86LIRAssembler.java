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

/*import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.asm.RelocInfo.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

public abstract class X86LIRAssembler extends LIRAssembler {

    private X86MacroAssembler masm;

    public X86LIRAssembler(C1XCompilation compilation) {
        super(compilation);

        assert compilation.masm() instanceof X86MacroAssembler;
        masm = (X86MacroAssembler) compilation.masm();
    }

    private X86MacroAssembler lir() {
        return masm;
    }

    @Override
    protected void set_24bitFPU() {

        lir().fldcw(ExternalAddress(StubRoutines.addrFpuCntrlWrd_24()));

    }

    @Override
    protected void resetFPU() {
        lir().fldcw(ExternalAddress(StubRoutines.addrFpuCntrlWrdStd()));
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

    @Override
    protected void push(LIROperand opr) {
        if (opr.isSingleCpu()) {
            lir().pushReg(opr.asRegister());
        } else if (opr.isDoubleCpu()) {
            if (!compilation.target.arch.is64bit()) {
                lir().pushReg(opr.asRegisterHi());
            }
            lir().pushReg(opr.asRegisterLo());
        } else if (opr.isStack()) {
            lir().pushAddr(frameMap().addressForSlot(opr.singleStackIx()));
        } else if (opr.isConstant()) {
            LIRConstant constOpr = opr.asConstantPtr();
            if (constOpr.type() == BasicType.Object) {
                lir().pushOop(constOpr.asJobject());
            } else if (constOpr.type() == BasicType.Int) {
                lir().pushJint(constOpr.asJint());
            } else {
                Util.shouldNotReachHere();
            }

        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void pop(LIROperand opr) {
        if (opr.isSingleCpu()) {
            lir().popReg(opr.asRegister());
        } else {
            Util.shouldNotReachHere();
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
            Register index = addr.index().asPointerRegister();
            return new Address(base, index, addr.scale(), addr.displacement());
        } else if (addr.index().isConstant()) {
            int addrOffset = (addr.index().asConstantPtr().asJint() << addr.scale().ordinal()) + addr.disp();
            assert Assembler.isSimm32(addrOffset) : "must be";

            return new Address(base, addrOffset);
        } else {
            throw Util.shouldNotReachHere();
        }
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
        CiMethod m = compilation().method();
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

        Register OSRBuf = osrBufferPointer().asPointerRegister();

        assert frame.interpreterFrameMonitorSize() == BasicObjectLock.size() : "adjust code below";
        int monitorOffset = compilation.target.arch.wordSize * method().maxLocals() + (BasicObjectLock.size() * compilation.target.arch.wordSize) * (numberOfLocks - 1);
        for (int i = 0; i < numberOfLocks; i++) {
            int slotOffset = monitorOffset - ((i * BasicObjectLock.size()) * compilation.target.arch.wordSize);

            boolean assertEnabled = false;
            assert assertEnabled = true;
            if (assertEnabled) {
                Label L;
                lir().cmpptr(Address(OSRBuf, slotOffset + BasicObjectLock.objOffsetInBytes()), (int32t) NULLWORD);
                lir().jcc(Assembler.notZero, L);
                lir().stop("locked object is null");
                lir().bind(L);
            }
            lir().movptr(X86Register.rbx, Address(OSRBuf, slotOffset + BasicObjectLock.lockOffsetInBytes()));
            lir().movptr(frameMap().addressForMonitorLock(i), X86Register.rbx);
            lir().movptr(X86Register.rbx, Address(OSRBuf, slotOffset + BasicObjectLock.objOffsetInBytes()));
            lir().movptr(frameMap().addressForMonitorObject(i), X86Register.rbx);
        }

    }

    @Override
    protected int checkIcache() {
        Register receiver = FrameMap.receiverOpr.asRegister();
        Register icKlass = ICKlass;
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

    private void monitorexit(LIROperand objOpr, LIROperand lockOpr, Register newHdr, int monitorNo, Register exception) {
        if (exception.isValid()) {
            // preserve exception
            // note: the monitorExit runtime call is a leaf routine
            // and cannot block => no GC can happen
            // The slow case (MonitorAccessStub) uses the first two stack slots
            // ([esp+0] and [esp+4]), therefore we store the exception at [esp+8]
            lir().movptr(new Address(X86Register.rsp, 2 * compilation.target.arch.wordSize), exception);
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
        lir().lea(lockReg, lockAddr);
        // unlock object
        MonitorAccessStub slowCase = new MonitorExitStub(lockOpr, true, monitorNo);
        // slowCaseStubs.append(slowCase);
        // temporary fix: must be created after exceptionhandler, therefore as call stub
        slowCaseStubs.append(slowCase);
        if (C1XOptions.UseFastLocking) {
            // try inlined fast unlocking first, revert to slow locking if it fails
            // note: lockReg points to the displaced header since the displaced header offset is 0!
            assert BasicLock.displacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
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
        return (frameMap().framesize() - (2 * VMRegImpl.slotsPerWord)) * VMRegImpl.stackSlotSize;
    }

    @Override
    public void emitExceptionHandler() {
      // if the last instruction is a call (typically to do a throw which
      // is coming at the end after block reordering) the return address
      // must still point into the code area in order to avoid assert on
      // failures when searching for the corresponding bci => add a nop
      // (was bug 5/14/1999 - gri)

      lir(). nop();

      // generate code for exception handler
      Address handlerBase = lir(). startAStub(exceptionHandlerSize);
      if (handlerBase == null) {
        // not enough space left for the handler
        throw new Bailout("exception handler overflow");
      }

      int offset = codeOffset();

      compilation().offsets().setValue(CodeOffsets.Entries.Exceptions, codeOffset());

      // if the method does not have an exception handler :  then there is
      // no reason to search for one
      if (compilation().hasExceptionHandlers() || compilation().env().jvmtiCanPostExceptions()) {
        // the exception oop and pc are in rax :  and rdx
        // no other registers need to be preserved :  so invalidate them
        lir(). invalidateRegisters(false, true, true, false, true, true);

        // check that there is really an exception
        lir(). verifyNotNullOop(X86Register.rax);

        // search an exception handler (rax: exception oop, rdx: throwing pc)
        lir(). call(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.HandleExceptionNofpu)));

        // if the call returns here :  then the exception handler for particular
        // exception doesn't exist . unwind activation and forward exception to caller
      }

      // the exception oop is in rax :
      // no other registers need to be preserved :  so invalidate them
      lir(). invalidateRegisters(false, true, true, true, true, true);

      // check that there is really an exception
      lir(). verifyNotNullOop(X86Register.rax);

      // unlock the receiver/klass if necessary
      // rax : : exception
      CiMethod method = compilation().method();
      if (method.isSynchronized() && C1XOptions.GenerateSynchronizationCode) {
        monitorexit(X86FrameMap.rbxOopOpr, X86FrameMap.rcxOpr, SYNCHeader, 0, X86Register.rax);
      }

      // unwind activation and forward exception to caller
      // rax : : exception
      lir(). jump(new RuntimeAddress(compilation.runtime.getRuntimeEntry(CiRuntimeCall.UnwindException)));

      assert(codeOffset() - offset <= exceptionHandlerSize, "overflow");

      lir(). endAStub();
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

        boolean resultIsOop = result.isValid() ? result.isOop() : false;

        // Note: we do not need to round double result; float result has the right precision
        // the poll sets the condition code, but no data registers
        AddressLiteral pollingPage = new AddressLiteral(os.getPollingPage() + (C1XOptions.SafepointPollOffset % os.vmPageSize()), RelocInfo.Type.pollReturnType);

        // NOTE: the requires that the polling page be reachable else the reloc
        // goes to the movq that loads the address and not the faulting instruction
        // which breaks the signal handler code

        lir().test32(X86Register.rax, pollingPage);

        lir().ret(0);
    }

    // TODO: Check why return type is int?
    @Override
    protected int safepointPoll(LIROperand tmp, CodeEmitInfo info) {
        AddressLiteral pollingPage = new AddressLiteral(os.getPollingPage() + (C1XOptions.SafepointPollOffset % os.vmPageSize()), RelocInfo.Type.pollType);

        if (info != null) {
            addDebugInfoForBranch(info);
        } else {
            Util.shouldNotReachHere();
        }

        int offset = lir().offset();

// NOTE: the requires that the polling page be reachable else the reloc
// goes to the movq that loads the address and not the faulting instruction
// which breaks the signal handler code

        lir().test32(X86Register.rax, pollingPage);
        return offset;
    }

    private void moveRegs(Register fromReg, Register toReg) {
        if (fromReg != toReg) {
            lir().mov(toReg, fromReg);
        }
    }

    private void swapReg(Register a, Register b) {
        lir().xchgptr(a, b);
    }

    private void jobject2regWithPatching(Register reg, CodeEmitInfo info) {
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
                lir().movl(dest.asRegister(), c.asJint());
                break;
            }

            case Long: {
                assert patchCode == LIRPatchCode.PatchNone : "no patching handled here";
                if (compilation.target.arch.is64bit()) {
                    lir().movptr(dest.asRegisterLo(), c.asLong());
                } else {

                    lir().movptr(dest.asRegisterLo(), c.asIntLo());
                    lir().movptr(dest.asRegisterHi(), c.asIntHi());
                }
                break;
            }

            case Object: {
                if (patchCode != LIRPatchCode.PatchNone) {
                    jobject2regWithPatching(dest.asRegister(), info);
                } else {
                    lir().movoop(dest.asRegister(), c.asJobject());
                }
                break;
            }

            case Float: {
                if (dest.isSingleXmm()) {
                    if (c.isZeroFloat()) {
                        lir().xorps(dest.asXmmFloatReg(), dest.asXmmFloatReg());
                    } else {
                        lir().movflt(dest.asXmmFloatReg(), InternalAddress(floatConstant(c.asJfloat())));
                    }
                } else {
                    assert dest.isSingleFpu() : "must be";
                    assert dest.fpuRegnr() == 0 : "dest must be TOS";
                    if (c.isZeroFloat()) {
                        lir().fldz();
                    } else if (c.isOneFloat()) {
                        lir().fld1();
                    } else {
                        lir().fldS(InternalAddress(floatConstant(c.asJfloat())));
                    }
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    if (c.isZeroDouble()) {
                        lir().xorpd(dest.asXmmDoubleReg(), dest.asXmmDoubleReg());
                    } else {
                        lir().movdbl(dest.asXmmDoubleReg(), InternalAddress(doubleConstant(c.asJdouble())));
                    }
                } else {
                    assert dest.isDoubleFpu() : "must be";
                    assert dest.fpuRegnrLo() == 0 : "dest must be TOS";
                    if (c.isZeroDouble()) {
                        lir().fldz();
                    } else if (c.isOneDouble()) {
                        lir().fld1();
                    } else {
                        lir().fldD(InternalAddress(doubleConstant(c.asJdouble())));
                    }
                }
                break;
            }

            default:
                Util.shouldNotReachHere();
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
                    lir().movptr(frameMap().addressForSlot(dest.doubleStackIx(), loWordOffsetInBytes), (intptrT) c.asJlongBits());
                } else {
                    lir().movptr(frameMap().addressForSlot(dest.doubleStackIx(), loWordOffsetInBytes), c.asJintLoBits());
                    lir().movptr(frameMap().addressForSlot(dest.doubleStackIx(), hiWordOffsetInBytes), c.asJintHiBits());
                }
                break;

            default:
                Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
                        lir().movoop(asAddress(addr, noreg), c.asJobject());
                    } else {
                        lir().movoop(asAddress(addr), c.asJobject());
                    }
                }
                break;

            case Long: // fall through
            case Double:

                if (compilation.target.arch.is64bit()) {
                    if (isLiteralAddress(addr)) {
                        Util.shouldNotReachHere();
                        lir().movptr(asAddress(addr, r15thread), (intptrT) c.asJlongBits());
                    } else {
                        lir().movptr(r10, (intptrT) c.asJlongBits());
                        nullCheckHere = codeOffset();
                        lir().movptr(asAddressLo(addr), r10);
                    }
                } else {
                    // Always reachable in 32bit so this doesn't produce useless move literal
                    lir().movptr(asAddressHi(addr), c.asJintHiBits());
                    lir().movptr(asAddressLo(addr), c.asJintLoBits());
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
                Util.shouldNotReachHere();
        }
        ;

        if (info != null) {
            addDebugInfoForNullCheck(nullCheckHere, info);
        }
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
                lir().verifyOop(src.asRegister());
            }
            moveRegs(src.asRegister(), dest.asRegister());

        } else if (dest.isDoubleCpu()) {
            if (compilation.target.arch.is64bit()) {
                if (src.type() == BasicType.Object) {
                    // Surprising to me but we can see move of a long to tObject
                    lir().verifyOop(src.asRegister());
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
            lir().movflt(new Address(X86Register.rsp, 0), src.asXmmFloatReg());
            lir().fldS(Address(rsp, 0));
        } else if (src.isDoubleXmm() && !dest.isDoubleXmm()) {
            lir().movdbl(new Address(X86Register.rsp, 0), src.asXmmDoubleReg());
            lir().fldD(Address(rsp, 0));
        } else if (dest.isSingleXmm() && !src.isSingleXmm()) {
            lir().fstpS(new Address(X86Register.rsp, 0));
            lir().movflt(dest.asXmmFloatReg(), Address(rsp, 0));
        } else if (dest.isDoubleXmm() && !src.isDoubleXmm()) {
            lir().fstpD(new Address(X86Register.rsp, 0));
            lir().movdbl(dest.asXmmDoubleReg(), Address(rsp, 0));

            // move between xmm-registers
        } else if (dest.isSingleXmm()) {
            assert src.isSingleXmm() : "must match";
            lir().movflt(dest.asXmmFloatReg(), src.asXmmFloatReg());
        } else if (dest.isDoubleXmm()) {
            assert src.isDoubleXmm() : "must match";
            lir().movdbl(dest.asXmmDoubleReg(), src.asXmmDoubleReg());

            // move between fpu-registers (no instruction necessary because of fpu-stack)
        } else if (dest.isSingleFpu() || dest.isDoubleFpu()) {
            assert src.isSingleFpu() || src.isDoubleFpu() : "must match";
            assert src.fpu() == dest.fpu() : "currently should be nothing to do";
        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2stack(LIROperand src, LIROperand dest, BasicType type, boolean popFpuStack) {
        assert src.isRegister() : "should not call otherwise";
        assert dest.isStack() : "should not call otherwise";

        if (src.isSingleCpu()) {
            Address dst = frameMap().addressForSlot(dest.singleStackIx());
            if (type == BasicType.Object) {
                lir().verifyOop(src.asRegister());
                lir().movptr(dst, src.asRegister());
            } else {
                lir().movl(dst, src.asRegister());
            }

        } else if (src.isDoubleCpu()) {
            Address dstLO = frameMap().addressForSlot(dest.doubleStackIx(), loWordOffsetInBytes);
            Address dstHI = frameMap().addressForSlot(dest.doubleStackIx(), hiWordOffsetInBytes);
            lir().movptr(dstLO, src.asRegisterLo());
            NOTLP64(lir().movptr(dstHI, src.asRegisterHi()));

        } else if (src.isSingleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.singleStackIx());
            lir().movflt(dstAddr, src.asXmmFloatReg());

        } else if (src.isDoubleXmm()) {
            Address dstAddr = frameMap().addressForSlot(dest.doubleStackIx());
            lir().movdbl(dstAddr, src.asXmmDoubleReg());

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
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void reg2mem(LIROperand src, LIROperand dest, BasicType type, LIRPatchCode patchCode, CodeEmitInfo info, boolean popFpuStack, boolean unaligned) {
        LIRAddress toAddr = dest.asAddressPtr();
        PatchingStub patch = null;

        if (type == BasicType.Object) {
            lir().verifyOop(src.asRegister());
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
                    lir().movflt(asAddress(toAddr), src.asXmmFloatReg());
                } else {
                    assert src.isSingleFpu() : "must be";
                    assert src.fpuRegnr() == 0 : "argument must be on TOS";
                    if (popFpuStack)
                        lir().fstpS(asAddress(toAddr));
                    else
                        lir().fstS(asAddress(toAddr));
                }
                break;
            }

            case Double: {
                if (src.isDoubleXmm()) {
                    lir().movdbl(asAddress(toAddr), src.asXmmDoubleReg());
                } else {
                    assert src.isDoubleFpu() : "must be";
                    assert src.fpuRegnrLo() == 0 : "argument must be on TOS";
                    if (popFpuStack)
                        lir().fstpD(asAddress(toAddr));
                    else
                        lir().fstD(asAddress(toAddr));
                }
                break;
            }

            case Jsr: // fall through
            case Object: // fall through
                if (compilation.target.arch.is64bit()) {
                    lir().movptr(asAddress(toAddr), src.asRegister());
                } else {
                    lir().movl(asAddress(toAddr), src.asRegister());

                }
                break;
            case Int:
                lir().movl(asAddress(toAddr), src.asRegister());
                break;

            case Long: {
                Register fromLo = src.asRegisterLo();
                Register fromHi = src.asRegisterHi();
                if (compilation.target.arch.is64bit()) {
                    lir().movptr(asAddressLo(toAddr), fromLo);
                } else {
                    Register base = toAddr.base().asRegister();
                    Register index = noreg;
                    if (toAddr.index().isRegister()) {
                        index = toAddr.index().asRegister();
                    }
                    if (base == fromLo || index == fromLo) {
                        assert base != fromHi : "can't be";
                        assert index == noreg || (index != base && index != fromHi) : "can't handle this";
                        lir().movl(asAddressHi(toAddr), fromHi);
                        if (patch != null) {
                            patchingEpilog(patch, lirPatchHigh, base, info);
                            patch = new PatchingStub(masm, PatchingStub.accessFieldId);
                            patchCode = lirPatchLow;
                        }
                        lir().movl(asAddressLo(toAddr), fromLo);
                    } else {
                        assert index == noreg || (index != base && index != fromLo) : "can't handle this";
                        lir().movl(asAddressLo(toAddr), fromLo);
                        if (patch != null) {
                            patchingEpilog(patch, lirPatchLow, base, info);
                            patch = new PatchingStub(masm, PatchingStub.accessFieldId);
                            patchCode = lirPatchHigh;
                        }
                        lir().movl(asAddressHi(toAddr), fromHi);
                    }
                }
                break;
            }

            case BasicType.Byte: // fall through
            case BasicType.Boolean: {
                Register srcReg = src.asRegister();
                Address dstAddr = asAddress(toAddr);
                assert VMVersion.isP6() || srcReg.hasByteRegister() : "must use byte registers if not P6";
                lir().movb(dstAddr, srcReg);
                break;
            }

            case BasicType.Char: // fall through
            case BasicType.Short:
                lir().movw(asAddress(toAddr), src.asRegister());
                break;

            default:
                Util.shouldNotReachHere();
        }

        if (patchCode != lirPatchNone) {
            patchingEpilog(patch, patchCode, toAddr.base().asRegister(), info);
        }
    }

    @Override
    protected void stack2reg(LIROperand src, LIROperand dest, BasicType type) {
        assert src.isStack() : "should not call otherwise";
        assert dest.isRegister() : "should not call otherwise";

        if (dest.isSingleCpu()) {
            if (type == BasicType.Object) {
                lir().movptr(dest.asRegister(), frameMap().addressForSlot(src.singleStackIx()));
                lir().verifyOop(dest.asRegister());
            } else {
                lir().movl(dest.asRegister(), frameMap().addressForSlot(src.singleStackIx()));
            }

        } else if (dest.isDoubleCpu()) {
            Address srcAddrLO = frameMap().addressForSlot(src.doubleStackIx(), loWordOffsetInBytes);
            Address srcAddrHI = frameMap().addressForSlot(src.doubleStackIx(), hiWordOffsetInBytes);
            lir().movptr(dest.asRegisterLo(), srcAddrLO);
            NOTLP64(lir().movptr(dest.asRegisterHi(), srcAddrHI));

        } else if (dest.isSingleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.singleStackIx());
            lir().movflt(dest.asXmmFloatReg(), srcAddr);

        } else if (dest.isDoubleXmm()) {
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIx());
            lir().movdbl(dest.asXmmDoubleReg(), srcAddr);

        } else if (dest.isSingleFpu()) {
            assert dest.fpuRegnr() == 0 : "dest must be TOS";
            Address srcAddr = frameMap().addressForSlot(src.singleStackIx());
            lir().fldS(srcAddr);

        } else if (dest.isDoubleFpu()) {
            assert dest.fpuRegnrLo() == 0 : "dest must be TOS";
            Address srcAddr = frameMap().addressForSlot(src.doubleStackIx());
            lir().fldD(srcAddr);

        } else {
            Util.shouldNotReachHere();
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
            Util.shouldNotReachHere();
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
                if (!VMVersion.isP6() && !fromAddr.uses(dest.asRegister())) {
                    // on pre P6 processors we may get partial register stalls
                    // so blow away the value of toRinfo before loading a
                    // partial word into it. Do it here so that it precedes
                    // the potential patch point below.
                    lir().xorptr(dest.asRegister(), dest.asRegister());
                }
                break;
        }

        PatchingStub patch = null;
        if (patchCode != lirPatchNone) {
            patch = new PatchingStub(masm, PatchingStub.PatchID.AccessFieldId);
            assert fromAddr.disp() != 0 : "must have";
        }
        if (info != null) {
            addDebugInfoForNullCheckHere(info);
        }

        switch (type) {
            case Float: {
                if (dest.isSingleXmm()) {
                    lir().movflt(dest.asXmmFloatReg(), fromAddr);
                } else {
                    assert dest.isSingleFpu() : "must be";
                    assert dest.fpuRegnr() == 0 : "dest must be TOS";
                    lir().fldS(fromAddr);
                }
                break;
            }

            case Double: {
                if (dest.isDoubleXmm()) {
                    lir().movdbl(dest.asXmmDoubleReg(), fromAddr);
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
                    lir().movptr(dest.asRegister(), fromAddr);
                } else {
                    lir().movl2ptr(dest.asRegister(), fromAddr);

                }
                break;
            case BasicType.Int:
                // %%% could this be a movl? this is safer but longer instruction
                lir().movl2ptr(dest.asRegister(), fromAddr);
                break;

            case BasicType.Long: {
                Register toLo = dest.asRegisterLo();
                Register toHi = dest.asRegisterHi();

                if (compilation.target.arch.is64bit()) {
                    lir().movptr(toLo, asAddressLo(addr));
                } else {
                    Register base = addr.base().asRegister();
                    Register index = noreg;
                    if (addr.index().isRegister()) {
                        index = addr.index().asRegister();
                    }
                    if ((base == toLo && index == toHi) || (base == toHi && index == toLo)) {
                        // addresses with 2 registers are only formed as a result of
                        // array access so this code will never have to deal with
                        // patches or null checks.
                        assert info == null && patch == null : "must be";
                        lir().lea(toHi, asAddress(addr));
                        lir().movl(toLo, Address(toHi, 0));
                        lir().movl(toHi, Address(toHi, BytesPerWord));
                    } else if (base == toLo || index == toLo) {
                        assert base != toHi : "can't be";
                        assert index == noreg || (index != base && index != toHi) : "can't handle this";
                        lir().movl(toHi, asAddressHi(addr));
                        if (patch != null) {
                            patchingEpilog(patch, lirPatchHigh, base, info);
                            patch = new PatchingStub(masm, PatchingStub.accessFieldId);
                            patchCode = lirPatchLow;
                        }
                        lir().movl(toLo, asAddressLo(addr));
                    } else {
                        assert index == noreg || (index != base && index != toLo) : "can't handle this";
                        lir().movl(toLo, asAddressLo(addr));
                        if (patch != null) {
                            patchingEpilog(patch, lirPatchLow, base, info);
                            patch = new PatchingStub(masm, PatchingStub.accessFieldId);
                            patchCode = lirPatchHigh;
                        }
                        lir().movl(toHi, asAddressHi(addr));
                    }
                }
                break;
            }

            case Boolean: // fall through
            case Byte: {
                Register destReg = dest.asRegister();
                assert VMVersion.isP6() || destReg.hasByteRegister() : "must use byte registers if not P6";
                if (VMVersion.isP6() || fromAddr.uses(destReg)) {
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
                Register destReg = dest.asRegister();
                assert VMVersion.isP6() || destReg.hasByteRegister() : "must use byte registers if not P6";
                if (VMVersion.isP6() || fromAddr.uses(destReg)) {
                    lir().movzwl(destReg, fromAddr);
                } else {
                    lir().movw(destReg, fromAddr);
                }
                // This is unsigned so the zero extension on 64bit is just what we need
                // lir(). movl2ptr(destReg, destReg);
                break;
            }

            case Short: {
                Register destReg = dest.asRegister();
                if (VMVersion.isP6() || fromAddr.uses(destReg)) {
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
                Util.shouldNotReachHere();
        }

        if (patch != null) {
            patchingEpilog(patch, patchCode, addr.base().asRegister(), info);
        }

        if (type == BasicType.Object) {
            lir().verifyOop(dest.asRegister());
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
                    Util.shouldNotReachHere();
                    break;
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
                    Util.shouldNotReachHere();
                    break;
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
                Util.shouldNotReachHere();
                break;
        }
    }

    private boolean assertEmitBranch(LIRBranch op) {
        assert op.block() == null || op.block().label() == op.label() : "wrong label";
        if (op.block() != null)
            branchTargetBlocks.add(op.block());
        if (op.ublock() != null)
            branchTargetBlocks.add(op.ublock());
        return true;
    }

    @Override
    protected void emitBranch(LIRBranch op) {

        assert assertEmitBranch(op);

        if (op.cond() == LIRCondition.Always) {
            if (op.info() != null)
                addDebugInfoForBranch(op.info());
            lir().jmp((op.label()));
        } else {
            Assembler.Condition acond = Assembler.zero;
            if (op.code() == LIRCondition.FloatBranch) {
                assert op.ublock() != null : "must have unordered successor";
                lir().jcc(Assembler.parity, p.ublock().label());
                switch (op.cond()) {
                    case LIRCondition.Equal:
                        acond = Assembler.equal;
                        break;
                    case LIRCondition.NotEqual:
                        acond = Assembler.notEqual;
                        break;
                    case LIRCondition.Less:
                        acond = Assembler.below;
                        break;
                    case LIRCondition.LessEqual:
                        acond = Assembler.belowEqual;
                        break;
                    case LIRCondition.GreaterEqual:
                        acond = Assembler.aboveEqual;
                        break;
                    case LIRCondition.Greater:
                        acond = Assembler.above;
                        break;
                    default:
                        Util.shouldNotReachHere();
                }
            } else {
                switch (op.cond()) {
                    case LIRCondition.Equal:
                        acond = Assembler.equal;
                        break;
                    case LIRCondition.NotEqual:
                        acond = Assembler.notEqual;
                        break;
                    case LIRCondition.Less:
                        acond = Assembler.less;
                        break;
                    case LIRCondition.LessEqual:
                        acond = Assembler.lessEqual;
                        break;
                    case LIRCondition.GreaterEqual:
                        acond = Assembler.greaterEqual;
                        break;
                    case LIRCondition.Greater:
                        acond = Assembler.greater;
                        break;
                    case LIRCondition.BelowEqual:
                        acond = Assembler.belowEqual;
                        break;
                    case LIRCondition.AboveEqual:
                        acond = Assembler.aboveEqual;
                        break;
                    default:
                        Util.shouldNotReachHere();
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
                    lir().movl2ptr(dest.asRegisterLo(), src.asRegister());
                } else {
                    moveRegs(src.asRegister(), dest.asRegisterLo());
                    moveRegs(src.asRegister(), dest.asRegisterHi());
                    lir().sarl(dest.asRegisterHi(), 31);
                }
                break;

            case Bytecodes.L2I:
                moveRegs(src.asRegisterLo(), dest.asRegister());
                break;

            case Bytecodes.I2B:
                moveRegs(src.asRegister(), dest.asRegister());
                lir().signExtendByte(dest.asRegister());
                break;

            case Bytecodes.I2C:
                moveRegs(src.asRegister(), dest.asRegister());
                lir().andl(dest.asRegister(), 0xFFFF);
                break;

            case Bytecodes.I2S:
                moveRegs(src.asRegister(), dest.asRegister());
                lir().signExtendShort(dest.asRegister());
                break;

            case Bytecodes.F2D:
            case Bytecodes.D2F:
                if (dest.isSingleXmm()) {
                    lir().cvtsd2ss(dest.asXmmFloatReg(), src.asXmmDoubleReg());
                } else if (dest.isDoubleXmm()) {
                    lir().cvtss2sd(dest.asXmmDoubleReg(), src.asXmmFloatReg());
                } else {
                    assert src.fpu() == dest.fpu() : "register must be equal";
                    // do nothing (float result is rounded later through spilling)
                }
                break;

            case Bytecodes.I2F:
            case Bytecodes.I2D:
                if (dest.isSingleXmm()) {
                    lir().cvtsi2ssl(dest.asXmmFloatReg(), src.asRegister());
                } else if (dest.isDoubleXmm()) {
                    lir().cvtsi2sdl(dest.asXmmDoubleReg(), src.asRegister());
                } else {
                    assert dest.fpu() == 0 : "result must be on TOS";
                    lir().movl(new Address(X86Register.rsp, 0), src.asRegister());
                    lir().fildS(new Address(X86Register.rsp, 0));
                }
                break;

            case Bytecodes.F2I:
            case Bytecodes.D2I:
                if (src.isSingleXmm()) {
                    lir().cvttss2sil(dest.asRegister(), src.asXmmFloatReg());
                } else if (src.isDoubleXmm()) {
                    lir().cvttsd2sil(dest.asRegister(), src.asXmmDoubleReg());
                } else {
                    assert src.fpu() == 0 : "input must be on TOS";
                    lir().fldcw(ExternalAddress(StubRoutines.addrFpuCntrlWrdTrunc()));
                    lir().fistS(Address(rsp, 0));
                    lir().movl(dest.asRegister(), Address(rsp, 0));
                    lir().fldcw(ExternalAddress(StubRoutines.addrFpuCntrlWrdStd()));
                }

                // IA32 conversion instructions do not match JLS for overflow, underflow and NaN . fixup in stub
                assert op.stub() != null : "stub required";
                lir().cmpl(dest.asRegister(), 0x80000000);
                lir().jcc(Assembler.equal, op.stub().entry());
                lir().bind(op.stub().continuation());
                break;

            case Bytecodes.L2F:
            case Bytecodes.L2D:
                assert !dest.isXmmRegister() : "result in xmm register not supported (no SSE instruction present)";
                assert dest.fpu() == 0 : "result must be on TOS";

                lir().movptr(Address(rsp, 0), src.asRegisterLo());
                NOTLP64(lir().movl(Address(rsp, BytesPerWord), src.asRegisterHi()));
                lir().fildD(Address(rsp, 0));
                // float result is rounded later through spilling
                break;

            case Bytecodes.F2L:
            case Bytecodes.D2L:
                assert !src.isXmmRegister() : "input in xmm register not supported (no SSE instruction present)";
                assert src.fpu() == 0 : "input must be on TOS";
                assert dest == FrameMap.long0opr : "runtime stub places result in these registers";

                // instruction sequence too long to inline it here
                {
                    lir().call(RuntimeAddress(Runtime1.entryFor(Runtime1.fpu2longStubId)));
                }
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitAllocObj(LIRAllocObj op) {
        if (op.isInitCheck()) {
            lir().cmpl(Address(op.klass().asRegister(), instanceKlass.initStateOffsetInBytes() + sizeof(oopDesc)), instanceKlass.fullyInitialized);
            addDebugInfoForNullCheckHere(op.stub().info());
            lir().jcc(Assembler.notEqual, op.stub().entry());
        }
        lir().allocateObject(op.obj().asRegister(), op.tmp1().asRegister(), op.tmp2().asRegister(), op.headerSize(), op.objectSize(), op.klass().asRegister(), op.stub().entry());
        lir().bind(op.stub().continuation());
    }

    @Override
    protected void emitAllocArray(LIRAllocArray op) {
        if (C1XOptions.UseSlowPath || (!C1XOptions.UseFastNewObjectArray && op.type() == BasicType.Object) || (!C1XOptions.UseFastNewTypeArray && op.type() != BasicType.Object)) {
            lir().jmp(op.stub().entry());
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
                lir().mov(tmp3, len);
            }
            lir().allocateArray(op.obj().asRegister(), len, tmp1, tmp2, compilation.runtime.arrayOopDescHeaderSize(op.type()), compilation.runtime.arrayElementSize(op.type()),
                            op.klass().asRegister(), op.stub().entry());
        }
        lir().bind(op.stub().continuation());
    }

    @Override
    protected void emitTypeCheck(LIRTypeCheck op) {
        LIROpcode code = op.code();
        if (code == LIROpcode.StoreCheck) {
            Register value = op.object().asRegister();
            Register array = op.array().asRegister();
            Register kRInfo = op.tmp1().asRegister();
            Register klassRInfo = op.tmp2().asRegister();
            Register Rtmp1 = op.tmp3().asRegister();

            CodeStub stub = op.stub();
            Label done = new Label();
            lir().cmpptr(value, (int32t) NULLWORD);
            lir().jcc(Assembler.equal, done);
            addDebugInfoForNullCheckHere(op.infoForException());
            lir().movptr(kRInfo, Address(array, oopDesc.klassOffsetInBytes()));
            lir().movptr(klassRInfo, Address(value, oopDesc.klassOffsetInBytes()));

            // get instance klass
            lir().movptr(kRInfo, Address(kRInfo, objArrayKlass.elementKlassOffsetInBytes() + sizeof(oopDesc)));
            // perform the fast part of the checking logic
            lir().checkKlassSubtypeFastPath(klassRInfo, kRInfo, Rtmp1, done, stub.entry(), null);
            // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
            lir().push(klassRInfo);
            lir().push(kRInfo);
            lir().call(RuntimeAddress(Runtime1.entryFor(Runtime1.slowSubtypeCheckId)));
            lir().pop(klassRInfo);
            lir().pop(kRInfo);
            // result is a boolean
            lir().cmpl(kRInfo, 0);
            lir().jcc(Assembler.equal, stub.entry());
            lir().bind(done);
        } else if (op.code() == lirCheckcast) {
            // we always need a stub for the failure case.
            CodeStub stub = op.stub();
            Register obj = op.object().asRegister();
            Register kRInfo = op.tmp1().asRegister();
            Register klassRInfo = op.tmp2().asRegister();
            Register dst = op.resultOpr().asRegister();
            ciKlass k = op.klass();
            Register Rtmp1 = noreg;

            Label done;
            if (obj == kRInfo) {
                kRInfo = dst;
            } else if (obj == klassRInfo) {
                klassRInfo = dst;
            }
            if (k.isLoaded()) {
                selectDifferentRegisters(obj, dst, kRInfo, klassRInfo);
            } else {
                Rtmp1 = op.tmp3().asRegister();
                selectDifferentRegisters(obj, dst, kRInfo, klassRInfo, Rtmp1);
            }

            assert ifferentRegisters(obj, kRInfo, klassRInfo);
            if (!k.isLoaded()) {
                jobject2regWithPatching(kRInfo, op.infoForPatch());
            } else {

                if (compilation.target.arch.is64bit()) {
                    lir().movoop(kRInfo, k.encoding());
                } else {
                    kRInfo = noreg;
                }
            }
            assert obj != kRInfo : "must be different";
            lir().cmpptr(obj, (int32t) NULLWORD);
            if (op.profiledMethod() != null) {
                ciMethod method = op.profiledMethod();
                int bci = op.profiledBci();

                Label profileDone;
                lir().jcc(Assembler.notEqual, profileDone);
                // Object is null; update methodDataOop
                ciMethodData md = method.methodData();
                if (md == null) {
                    bailout("out of memory building methodDataOop");
                    return;
                }
                ciProfileData data = md.bciToData(bci);
                assert data != null : "need data for checkcast";
                assert data.isBitData() : "need BitData for checkcast";
                Register mdo = klassRInfo;
                lir().movoop(mdo, md.encoding());
                Address dataAddr = new Address(mdo, md.byteOffsetOfSlot(data, DataLayout.headerOffset()));
                int headerBits = DataLayout.flagMaskToHeaderMask(BitData.nullSeenByteConstant());
                lir().orl(dataAddr, headerBits);
                lir().jmp(done);
                lir().bind(profileDone);
            } else {
                lir().jcc(Assembler.equal, done);
            }
            lir().verifyOop(obj);

            if (op.fastCheck()) {
                // get object classo
                // not a safepoint as obj null check happens earlier
                if (k.isLoaded()) {

                    if (compilation.target.arch.is64bit()) {
                        lir().cmpptr(kRInfo, Address(obj, oopDesc.klassOffsetInBytes()));
                    } else {
                        lir().cmpoop(Address(obj, oopDesc.klassOffsetInBytes()), k.encoding());
                    }
                } else {
                    lir().cmpptr(kRInfo, Address(obj, oopDesc.klassOffsetInBytes()));

                }
                lir().jcc(Assembler.notEqual, stub.entry());
                lir().bind(done);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                lir().movptr(klassRInfo, Address(obj, oopDesc.klassOffsetInBytes()));
                if (k.isLoaded()) {
                    // See if we get an immediate positive hit
                    if (compilation.target.arch.is64bit()) {
                        lir().cmpptr(kRInfo, Address(klassRInfo, k.superCheckOffset()));
                    } else {
                        lir().cmpoop(Address(klassRInfo, k.superCheckOffset()), k.encoding());
                    }
                    if (sizeof(oopDesc) + Klass.secondarySuperCacheOffsetInBytes() != k.superCheckOffset()) {
                        lir().jcc(Assembler.notEqual, stub.entry());
                    } else {
                        // See if we get an immediate positive hit
                        lir().jcc(Assembler.equal, done);
                        // check for self
                        if (compilation.target.arch.is64bit()) {
                            lir().cmpptr(klassRInfo, kRInfo);
                        } else {
                            lir().cmpoop(klassRInfo, k.encoding());
                        }
                        lir().jcc(Assembler.equal, done);

                        lir().push(klassRInfo);
                        if (compilation.target.arch.is64bit()) {
                            lir().push(kRInfo);
                        } else {
                            lir().pushoop(k.encoding());
                        }
                        lir().call(RuntimeAddress(Runtime1.entryFor(Runtime1.slowSubtypeCheckId)));
                        lir().pop(klassRInfo);
                        lir().pop(klassRInfo);
                        // result is a boolean
                        lir().cmpl(klassRInfo, 0);
                        lir().jcc(Assembler.equal, stub.entry());
                    }
                    lir().bind(done);
                } else {
                    // perform the fast part of the checking logic
                    lir().checkKlassSubtypeFastPath(klassRInfo, kRInfo, Rtmp1, done, stub.entry(), null);
                    // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                    lir().push(klassRInfo);
                    lir().push(kRInfo);
                    lir().call(RuntimeAddress(Runtime1.entryFor(Runtime1.slowSubtypeCheckId)));
                    lir().pop(klassRInfo);
                    lir().pop(kRInfo);
                    // result is a boolean
                    lir().cmpl(kRInfo, 0);
                    lir().jcc(Assembler.equal, stub.entry());
                    lir().bind(done);
                }

            }
            if (dst != obj) {
                lir().mov(dst, obj);
            }
        } else if (code == lirInstanceof) {
            Register obj = op.object().asRegister();
            Register kRInfo = op.tmp1().asRegister();
            Register klassRInfo = op.tmp2().asRegister();
            Register dst = op.resultOpr().asRegister();
            ciKlass k = op.klass();

            Label done;
            Label zero;
            Label one;
            if (obj == kRInfo) {
                kRInfo = klassRInfo;
                klassRInfo = obj;
            }
            // patching may screw with our temporaries on sparc :
            // so let's do it before loading the class
            if (!k.isLoaded()) {
                jobject2regWithPatching(kRInfo, op.infoForPatch());
            } else {
                LP64ONLY(lir().movoop(kRInfo, k.encoding()));
            }
            assert obj != kRInfo : "must be different";

            lir().verifyOop(obj);
            if (op.fastCheck()) {
                lir().cmpptr(obj, (int32t) NULLWORD);
                lir().jcc(Assembler.equal, zero);
                // get object class
                // not a safepoint as obj null check happens earlier
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
                    lir().cmpoop(Address(obj, oopDesc.klassOffsetInBytes()), k.encoding());
                    kRInfo = noreg;
                } else {
                    lir().cmpptr(kRInfo, Address(obj, oopDesc.klassOffsetInBytes()));

                }
                lir().jcc(Assembler.equal, one);
            } else {
                // get object class
                // not a safepoint as obj null check happens earlier
                lir().cmpptr(obj, (int32t) NULLWORD);
                lir().jcc(Assembler.equal, zero);
                lir().movptr(klassRInfo, Address(obj, oopDesc.klassOffsetInBytes()));
                if (!compilation.target.arch.is64bit() && k.isLoaded()) {
                    // See if we get an immediate positive hit
                    lir().cmpoop(Address(klassRInfo, k.superCheckOffset()), k.encoding());
                    lir().jcc(Assembler.equal, one);
                    if (sizeof(oopDesc) + Klass.secondarySuperCacheOffsetInBytes() == k.superCheckOffset()) {
                        // check for self
                        lir().cmpoop(klassRInfo, k.encoding());
                        lir().jcc(Assembler.equal, one);
                        lir().push(klassRInfo);
                        lir().pushoop(k.encoding());
                        lir().call(RuntimeAddress(Runtime1.entryFor(Runtime1.slowSubtypeCheckId)));
                        lir().pop(klassRInfo);
                        lir().pop(dst);
                        lir().jmp(done);
                    }
                } else // next block is unconditional if LP64:
                {
                    assert dst != klassRInfo && dst != kRInfo : "need 3 registers";

                    // perform the fast part of the checking logic
                    lir().checkKlassSubtypeFastPath(klassRInfo, kRInfo, dst, one, zero, null);
                    // call out-of-line instance of lir(). checkKlassSubtypeSlowPath(...):
                    lir().push(klassRInfo);
                    lir().push(kRInfo);
                    lir().call(RuntimeAddress(Runtime1.entryFor(Runtime1.slowSubtypeCheckId)));
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
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompareAndSwap(LIRCompareAndSwap op) {
        if (!compilation.target.arch.is64bit() && op.code() == LIROpcode.CasLong && VMVersion.supportsCx8()) {
            assert op.cmpValue().asRegisterLo() == X86Register.rax : "wrong register";
            assert op.cmpValue().asRegisterHi() == rdx : "wrong register";
            assert op.newValue().asRegisterLo() == rbx : "wrong register";
            assert op.newValue().asRegisterHi() == rcx : "wrong register";
            Register addr = op.addr().asRegister();
            if (os.isMP()) {
                lir().lock();
            }
            lir().cmpxchg8(Address(addr, 0));

        } else if (op.code() == lirCasInt || op.code() == lirCasObj) {
            assert compilation.target.arch.is64bit() || op.addr().isSingleCpu() : "must be single";
            Register addr = (op.addr().isSingleCpu() ? op.addr().asRegister() : op.addr().asRegisterLo());
            Register newval = op.newValue().asRegister();
            Register cmpval = op.cmpValue().asRegister();
            assert cmpval == X86Register.rax : "wrong register";
            assert newval != null : "new val must be register";
            assert cmpval != newval : "cmp and new values must be in different registers";
            assert cmpval != addr : "cmp and addr must be in different registers";
            assert newval != addr : "new value and addr must be in different registers";
            if (os.isMP()) {
                lir().lock();
            }
            if (op.code() == lirCasObj) {
                lir().cmpxchgptr(newval, Address(addr, 0));
            } else if (op.code() == lirCasInt) {
                lir().cmpxchgl(newval, Address(addr, 0));
            } else if (compilation.target.arch.is64bit()) {
                lir().cmpxchgq(newval, Address(addr, 0));
            }
        } else if (compilation.target.arch.is64bit() && sop.code() == lirCasLong) {
            Register addr = (op.addr().isSingleCpu() ? op.addr().asRegister() : op.addr().asRegisterLo());
            Register newval = op.newValue().asRegisterLo();
            Register cmpval = op.cmpValue().asRegisterLo();
            assert cmpval == X86Register.rax : "wrong register";
            assert newval != null : "new val must be register";
            assert cmpval != newval : "cmp and new values must be in different registers";
            assert cmpval != addr : "cmp and addr must be in different registers";
            assert newval != addr : "new value and addr must be in different registers";
            if (os.isMP()) {
                lir().lock();
            }
            lir().cmpxchgq(newval, Address(addr, 0));
        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void cmove(LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROperand resultOpr) {
        Assembler.Condition acond, ncond;
        switch (condition) {
            case LIRCondition.Equal:
                acond = Assembler.equal;
                ncond = Assembler.notEqual;
                break;
            case LIRCondition.NotEqual:
                acond = Assembler.notEqual;
                ncond = Assembler.equal;
                break;
            case LIRCondition.Less:
                acond = Assembler.less;
                ncond = Assembler.greaterEqual;
                break;
            case LIRCondition.LessEqual:
                acond = Assembler.lessEqual;
                ncond = Assembler.greater;
                break;
            case LIRCondition.GreaterEqual:
                acond = Assembler.greaterEqual;
                ncond = Assembler.less;
                break;
            case LIRCondition.Greater:
                acond = Assembler.greater;
                ncond = Assembler.lessEqual;
                break;
            case LIRCondition.BelowEqual:
                acond = Assembler.belowEqual;
                ncond = Assembler.above;
                break;
            case LIRCondition.AboveEqual:
                acond = Assembler.aboveEqual;
                ncond = Assembler.below;
                break;
            default:
                Util.shouldNotReachHere();
        }

        if (opr1.isCpuRegister()) {
            reg2reg(opr1, result);
        } else if (opr1.isStack()) {
            stack2reg(opr1, result, result.type());
        } else if (opr1.isConstant()) {
            const2reg(opr1, result, lirPatchNone, null);
        } else {
            Util.shouldNotReachHere();
        }

        if (VMVersion.supportsCmov() && !opr2.isConstant()) {
            // optimized version that does not require a branch
            if (opr2.isSingleCpu()) {
                assert opr2.cpuRegnr() != result.cpuRegnr() : "opr2 already overwritten by previous move";
                lir().cmov(ncond, result.asRegister(), opr2.asRegister());
            } else if (opr2.isDoubleCpu()) {
                assert opr2.cpuRegnrLo() != result.cpuRegnrLo() && opr2.cpuRegnrLo() != result.cpuRegnrHi() : "opr2 already overwritten by previous move";
                assert opr2.cpuRegnrHi() != result.cpuRegnrLo() && opr2.cpuRegnrHi() != result.cpuRegnrHi() : "opr2 already overwritten by previous move";
                lir().cmovptr(ncond, result.asRegisterLo(), opr2.asRegisterLo());
                if (!compilation.target.arch.is64bit()) {
                    lir().cmovptr(ncond, result.asRegisterHi(), opr2.asRegisterHi());
                }
            } else if (opr2.isSingleStack()) {
                lir().cmovl(ncond, result.asRegister(), frameMap().addressForSlot(opr2.singleStackIx()));
            } else if (opr2.isDoubleStack()) {
                lir().cmovptr(ncond, result.asRegisterLo(), frameMap().addressForSlot(opr2.doubleStackIx(), loWordOffsetInBytes));
                if (!compilation.target.arch.is64bit()) {
                    lir().cmovptr(ncond, result.asRegisterHi(), frameMap().addressForSlot(opr2.doubleStackIx(), hiWordOffsetInBytes));
                }
            } else {
                Util.shouldNotReachHere();
            }

        } else {
            Label skip = new Label();
            lir().jcc(acond, skip);
            if (opr2.isCpuRegister()) {
                reg2reg(opr2, result);
            } else if (opr2.isStack()) {
                stack2reg(opr2, result, result.type());
            } else if (opr2.isConstant()) {
                const2reg(opr2, result, lirPatchNone, null);
            } else {
                Util.shouldNotReachHere();
            }
            lir().bind(skip);
        }
    }

    @Override
    protected void arithOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dest, CodeEmitInfo info, boolean popFpuStack) {
        assert info == null : "should never be used :  idiv/irem and ldiv/lrem not handled by this method";

        if (left.isSingleCpu()) {
            assert left == dest : "left and dest must be equal";
            Register lreg = left.asRegister();

            if (right.isSingleCpu()) {
                // cpu register - cpu register
                Register rreg = right.asRegister();
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
                        Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
                }

            } else if (right.isConstant()) {
                // cpu register - constant
                jint c = right.asConstantPtr().asJint();
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
                        Util.shouldNotReachHere();
                }

            } else {
                Util.shouldNotReachHere();
            }

        } else if (left.isDoubleCpu()) {
            assert left == dest : "left and dest must be equal";
            Register lregLo = left.asRegisterLo();
            Register lregHi = left.asRegisterHi();

            if (right.isDoubleCpu()) {
                // cpu register - cpu register
                Register rregLo = right.asRegisterLo();
                Register rregHi = right.asRegisterHi();
                assert compilation.target.arch.is64bit() || differentRegisters(lregLo, lregHi, rregLo, rregHi);
                assert !compilation.target.arch.is64bit() || differentRegisters(lregLo, rregLo);
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
                            assert lregLo == X86Register.rax && lregHi == rdx : "must be";
                            lir().imull(lregHi, rregLo);
                            lir().imull(rregHi, lregLo);
                            lir().addl(rregHi, lregHi);
                            lir().mull(rregLo);
                            lir().addl(lregHi, rregHi);
                        }
                        break;
                    default:
                        Util.shouldNotReachHere();
                }

            } else if (right.isConstant()) {
                // cpu register - constant
                if (compilation.target.arch.is64bit()) {
                    jlong c = right.asConstantPtr().asJlongBits();
                    lir().movptr(r10, (intptrT) c);
                    switch (code) {
                        case Add:
                            lir().addptr(lregLo, r10);
                            break;
                        case Sub:
                            lir().subptr(lregLo, r10);
                            break;
                        default:
                            Util.shouldNotReachHere();
                    }
                } else {
                    jint cLo = right.asConstantPtr().asJintLo();
                    jint cHi = right.asConstantPtr().asJintHi();
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
                            Util.shouldNotReachHere();
                    }
                }

            } else {
                Util.shouldNotReachHere();
            }

        } else if (left.isSingleXmm()) {
            assert left == dest : "left and dest must be equal";
            XMMRegister lreg = left.asXmmFloatReg();

            if (right.isSingleXmm()) {
                XMMRegister rreg = right.asXmmFloatReg();
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
                        Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isSingleStack()) {
                    raddr = frameMap().addressForSlot(right.singleStackIx());
                } else if (right.isConstant()) {
                    // hack for now
                    raddr = lir().asAddress(InternalAddress(floatConstant(right.asJfloat())));
                } else {
                    Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleXmm()) {
            assert left == dest : "left and dest must be equal";

            XMMRegister lreg = left.asXmmDoubleReg();
            if (right.isDoubleXmm()) {
                XMMRegister rreg = right.asXmmDoubleReg();
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
                        Util.shouldNotReachHere();
                }
            } else {
                Address raddr;
                if (right.isDoubleStack()) {
                    raddr = frameMap().addressForSlot(right.doubleStackIx());
                } else if (right.isConstant()) {
                    // hack for now
                    raddr = lir().asAddress(InternalAddress(doubleConstant(right.asJdouble())));
                } else {
                    Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
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
                    address constAddr = floatConstant(right.asJfloat());
                    assert constAddr != null : "incorrect float/double constant maintainance";
                    // hack for now
                    raddr = lir().asAddress(InternalAddress(constAddr));
                } else {
                    Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
                }
            }

        } else if (left.isDoubleFpu()) {
            assert dest.isDoubleFpu() : "fpu stack allocation required";

            if (code == lirMulStrictfp || code == lirDivStrictfp) {
                // Double values require special handling for strictfp mul/div on x86
                lir().fldX(ExternalAddress(StubRoutines.addrFpuSubnormalBias1()));
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
                    raddr = lir().asAddress(InternalAddress(doubleConstant(right.asJdouble())));
                } else {
                    Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
                }
            }

            if (code == lirMulStrictfp || code == lirDivStrictfp) {
                // Double values require special handling for strictfp mul/div on x86
                lir().fldX(ExternalAddress(StubRoutines.addrFpuSubnormalBias2()));
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
                Util.shouldNotReachHere();
            }

            if (right.isSingleCpu()) {
                Register rreg = right.asRegister();
                switch (code) {
                    case Add:
                        lir().addl(laddr, rreg);
                        break;
                    case Sub:
                        lir().subl(laddr, rreg);
                        break;
                    default:
                        Util.shouldNotReachHere();
                }
            } else if (right.isConstant()) {
                jint c = right.asConstantPtr().asJint();
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
                        Util.shouldNotReachHere();
                }
            } else {
                Util.shouldNotReachHere();
            }

        } else {
            Util.shouldNotReachHere();
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
                if (popFpuStack)
                    lir().faddp(nonTosIndex);
                else if (destIsTos)
                    lir().fadd(nonTosIndex);
                else
                    lir().fadda(nonTosIndex);
                break;

            case Sub:
                if (leftIsTos) {
                    if (popFpuStack)
                        lir().fsubrp(nonTosIndex);
                    else if (destIsTos)
                        lir().fsub(nonTosIndex);
                    else
                        lir().fsubra(nonTosIndex);
                } else {
                    if (popFpuStack)
                        lir().fsubp(nonTosIndex);
                    else if (destIsTos)
                        lir().fsubr(nonTosIndex);
                    else
                        lir().fsuba(nonTosIndex);
                }
                break;

            case MulStrictFp: // fall through
            case Mul:
                if (popFpuStack)
                    lir().fmulp(nonTosIndex);
                else if (destIsTos)
                    lir().fmul(nonTosIndex);
                else
                    lir().fmula(nonTosIndex);
                break;

            case DivStrictFp: // fall through
            case Div:
                if (leftIsTos) {
                    if (popFpuStack)
                        lir().fdivrp(nonTosIndex);
                    else if (destIsTos)
                        lir().fdiv(nonTosIndex);
                    else
                        lir().fdivra(nonTosIndex);
                } else {
                    if (popFpuStack)
                        lir().fdivp(nonTosIndex);
                    else if (destIsTos)
                        lir().fdivr(nonTosIndex);
                    else
                        lir().fdiva(nonTosIndex);
                }
                break;

            case Rem:
                assert leftIsTos && destIsTos && rightIndex == 1 : "must be guaranteed by FPU stack allocation";
                lir().fremr(noreg);
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    @Override
    protected void intrinsicOp(LIROpcode code, LIROperand value, LIROperand unused, LIROperand dest, LIROp2 op) {

        if (value.isDoubleXmm()) {
            switch (code) {
                case Abs: {
                    if (dest.asXmmDoubleReg() != value.asXmmDoubleReg()) {
                        lir().movdbl(dest.asXmmDoubleReg(), value.asXmmDoubleReg());
                    }
                    lir().andpd(dest.asXmmDoubleReg(), ExternalAddress((address) doubleSignmaskPool));
                }
                    break;

                case Sqrt:
                    lir().sqrtsd(dest.asXmmDoubleReg(), value.asXmmDoubleReg());
                    break;
                // all other intrinsics are not available in the SSE instruction set, so FPU is used
                default:
                    Util.shouldNotReachHere();
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
                    lir().trigfunc('s', op.asOp2().fpuStackSize());
                    break;
                case Cos:
                    // Should consider not saving rbx, if not necessary
                    assert op.asOp2().fpuStackSize() <= 6 : "sin and cos need two free stack slots";
                    lir().trigfunc('c', op.asOp2().fpuStackSize());
                    break;
                case Tan:
                    // Should consider not saving rbx, if not necessary
                    lir().trigfunc('t', op.asOp2().fpuStackSize());
                    break;
                default:
                    Util.shouldNotReachHere();
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void logicOp(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst) {

        // assert left.destroysRegister() : "check";
        if (left.isSingleCpu()) {
            Register reg = left.asRegister();
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
                        Util.shouldNotReachHere();
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
                        Util.shouldNotReachHere();
                }
            } else {
                Register rright = right.asRegister();
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
                        Util.shouldNotReachHere();
                }
            }
            moveRegs(reg, dst.asRegister());
        } else {
            Register lLo = left.asRegisterLo();
            Register lHi = left.asRegisterHi();
            if (right.isConstant()) {
                if (compilation.target.arch.is64bit()) {
                    lir().mov64(rscratch1, right.asConstantPtr().asJlong());
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
                            Util.shouldNotReachHere();
                    }
                } else {
                    int rLo = right.asConstantPtr().asJintLo();
                    int rHi = right.asConstantPtr().asJintHi();
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
                            Util.shouldNotReachHere();
                    }
                }
            } else {
                Register rLo = right.asRegisterLo();
                Register rHi = right.asRegisterHi();
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
                        Util.shouldNotReachHere();
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
                lir().sarl(lreg, log2intptr(divisor));
                moveRegs(lreg, dreg);
            } else if (code == LIROpcode.Rem) {
                Label done;
                lir().mov(dreg, lreg);
                lir().andl(dreg, 0x80000000 | (divisor - 1));
                lir().jcc(Assembler.positive, done);
                lir().decrement(dreg);
                lir().orl(dreg, ~(divisor - 1));
                lir().increment(dreg);
                lir().bind(done);
            } else {
                Util.shouldNotReachHere();
            }
        } else {
            Register rreg = right.asRegister();
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
            Register reg1 = opr1.asRegister();
            if (opr2.isSingleCpu()) {
                // cpu register - cpu register
                if (opr1.type() == BasicType.Object) {
                    lir().cmpptr(reg1, opr2.asRegister());
                } else {
                    assert opr2.type() != BasicType.Object && opr2.type() != BasicType.Array : "cmp int :  oop?";
                    lir().cmpl(reg1, opr2.asRegister());
                }
            } else if (opr2.isStack()) {
                // cpu register - stack
                if (opr1.type() == BasicType.Object || opr1.type() == BasicType.Array) {
                    lir().cmpptr(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
                } else {
                    lir().cmpl(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
                }
            } else if (opr2.isConstant()) {
                // cpu register - constant
                LIRConst c = opr2.asConstantPtr();
                if (c.type() == BasicType.Int) {
                    lir().cmpl(reg1, c.asJint());
                } else if (c.type() == BasicType.Object || c.type() == BasicType.Array) {
                    // In 64bit oops are single register
                    jobject o = c.asJobject();
                    if (o == null) {
                        lir().cmpptr(reg1, (int32t) NULLWORD);
                    } else {
                        if (compilation.target.arch.is64bit()) {
                            lir().movoop(rscratch1, o);
                            lir().cmpptr(reg1, rscratch1);
                        } else {
                            lir().cmpoop(reg1, c.asJobject());
                        }
                    }
                } else {
                    Util.shouldNotReachHere();
                }
                // cpu register - address
            } else if (opr2.isAddress()) {
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                lir().cmpl(reg1, asAddress(opr2.asAddressPtr()));
            } else {
                Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleCpu()) {
            Register xlo = opr1.asRegisterLo();
            Register xhi = opr1.asRegisterHi();
            if (opr2.isDoubleCpu()) {
                if (compilation.target.arch.is64bit()) {
                    lir().cmpptr(xlo, opr2.asRegisterLo());
                } else {
                    // cpu register - cpu register
                    Register ylo = opr2.asRegisterLo();
                    Register yhi = opr2.asRegisterHi();
                    lir().subl(xlo, ylo);
                    lir().sbbl(xhi, yhi);
                    if (condition == LIRCondition.Equal || condition == LIRCondition.NotEqual) {
                        lir().orl(xhi, xlo);
                    }
                }
            } else if (opr2.isConstant()) {
                // cpu register - constant 0
                assert opr2.asJlong() == (jlong) 0 : "only handles zero";
                if (compilation.target.arch.is64bit()) {
                    lir().cmpptr(xlo, (int32t) opr2.asJlong());
                } else {
                    assert condition == LIRCondition.Equal || condition == LIRCondition.NotEqual : "only handles equals case";
                    lir().orl(xhi, xlo);
                }
            } else {
                Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleXmm()) {
            XMMRegister reg1 = opr1.asXmmFloatReg();
            if (opr2.isSingleXmm()) {
                // xmm register - xmm register
                lir().ucomiss(reg1, opr2.asXmmFloatReg());
            } else if (opr2.isStack()) {
                // xmm register - stack
                lir().ucomiss(reg1, frameMap().addressForSlot(opr2.singleStackIx()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                lir().ucomiss(reg1, InternalAddress(floatConstant(opr2.asJfloat())));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                lir().ucomiss(reg1, asAddress(opr2.asAddressPtr()));
            } else {
                Util.shouldNotReachHere();
            }

        } else if (opr1.isDoubleXmm()) {
            XMMRegister reg1 = opr1.asXmmDoubleReg();
            if (opr2.isDoubleXmm()) {
                // xmm register - xmm register
                lir().ucomisd(reg1, opr2.asXmmDoubleReg());
            } else if (opr2.isStack()) {
                // xmm register - stack
                lir().ucomisd(reg1, frameMap().addressForSlot(opr2.doubleStackIx()));
            } else if (opr2.isConstant()) {
                // xmm register - constant
                lir().ucomisd(reg1, InternalAddress(doubleConstant(opr2.asJdouble())));
            } else if (opr2.isAddress()) {
                // xmm register - address
                if (op.info() != null) {
                    addDebugInfoForNullCheckHere(op.info());
                }
                lir().ucomisd(reg1, asAddress(opr2.pointer().asAddress()));
            } else {
                Util.shouldNotReachHere();
            }

        } else if (opr1.isSingleFpu() || opr1.isDoubleFpu()) {
            assert opr1.isFpuRegister() && opr1.fpu() == 0 : "currently left-hand side must be on TOS (relax this restriction)";
            assert opr2.isFpuRegister() : "both must be registers";
            lir().fcmp(noreg, opr2.fpu(), op.fpuPopCount() > 0, op.fpuPopCount() > 1);

        } else if (opr1.isAddress() && opr2.isConstant()) {
            LIRConst c = opr2.asConstantPtr();

            if (compilation.target.arch.is64bit()) {
                if (c.type() == BasicType.Object || c.type() == BasicType.Array) {
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
                    // better strategy by giving noreg as the temp for asAddress
                    lir().cmpptr(rscratch1, asAddress(addr, noreg));
                } else {
                    lir().cmpoop(asAddress(addr), c.asJobject());
                }
            } else {
                Util.shouldNotReachHere();
            }

        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void compFl2i(LIROpcode code, LIROperand left, LIROperand right, LIROperand dst, LIROp2 op) {
        if (code == LIROpcode.Cmpfd2i || code == LIROpcode.Ucmpfd2i) {
            if (left.isSingleXmm()) {
                assert right.isSingleXmm() : "must match";
                lir().cmpss2int(left.asXmmFloatReg(), right.asXmmFloatReg(), dst.asRegister(), code == LIROpcode.Ucmpfd2i);
            } else if (left.isDoubleXmm()) {
                assert right.isDoubleXmm() : "must match";
                lir().cmpsd2int(left.asXmmDoubleReg(), right.asXmmDoubleReg(), dst.asRegister(), code == LIROpcode.Ucmpfd2i);

            } else {
                assert left.isSingleFpu() || left.isDoubleFpu() : "must be";
                assert right.isSingleFpu() || right.isDoubleFpu() : "must match";

                assert left.fpu() == 0 : "left must be on TOS";
                lir().fcmp2int(dst.asRegister(), code == lirUcmpFd2i, right.fpu(), op.fpuPopCount() > 0, op.fpuPopCount() > 1);
            }
        } else {
            assert code == lirCmpL2i : "check";
            if (compilation.target.arch.is64bit()) {
                Register dest = dst.asRegister();
                lir().xorptr(dest, dest);
                Label high, done;
                lir().cmpptr(left.asRegisterLo(), right.asRegisterLo());
                lir().jcc(Assembler.equal, done);
                lir().jcc(Assembler.greater, high);
                lir().decrement(dest);
                lir().jmp(done);
                lir().bind(high);
                lir().increment(dest);

                lir().bind(done);

            } else {
                lir().lcmp2int(left.asRegisterHi(), left.asRegisterLo(), right.asRegisterHi(), right.asRegisterLo());
                moveRegs(left.asRegisterHi(), dst.asRegister());
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
                    offset += NativeCall.displacementOffset;
                    break;
                case IcVirtualCall:
                    offset += NativeCall.displacementOffset + NativeMovConstReg.instructionSize;
                    break;
                case VirtualCall: // currently, sparc-specific for niagara
                default:
                    Util.shouldNotReachHere();
            }
            while (offset++ % BytesPerWord != 0) {
                lir().nop();
            }
        }
    }

    @Override
    protected void call(Address addr, RelocInfo.Type rtype, CodeEmitInfo info) {
        assert !compilation.runtime.isMP() || (lir().offset() + NativeCall.displacementOffset) % BytesPerWord == 0 : "must be aligned";
        lir().call(AddressLiteral(entry, rtype));
        addCallInfo(codeOffset(), info);
    }

    @Override
    protected void icCall(Address entry, CodeEmitInfo info) {
        RelocationHolder rh = virtualCallRelocation.spec(pc());
        lir().movoop(ICKlass, (jobject) Universe.nonOopWord());
        assert !compilation.runtime.isMP() || (lir().offset() + NativeCall.displacementOffset) % BytesPerWord == 0 : "must be aligned";
        lir().call(AddressLiteral(entry, rh));
        addCallInfo(codeOffset(), info);
    }

    @Override
    protected void vtableCall(Address vtableOffset, CodeEmitInfo info) {
        Util.shouldNotReachHere();
    }

    @Override
    protected void emitStaticCallStub() {
        Address callPc = lir().pc();
        Address stub = lir().startAStub(callStubSize);
        if (stub == null) {
            bailout("static call stub overflow");
            return;
        }

        int start = lir().offset();
        if (os.isMP()) {
            // make sure that the displacement word of the call ends up word aligned
            int offset = lir().offset() + NativeMovConstReg.instructionSize + NativeCall.displacementOffset;
            while (offset++ % BytesPerWord != 0) {
                lir().nop();
            }
        }
        lir().relocate(staticStubRelocation.spec(callPc));
        lir().movoop(rbx, (jobject) null);
        // must be set to -1 at code generation time
        assert !os.isMP() || ((lir().offset() + 1) % BytesPerWord) == 0 : "must be aligned on MP";
        // On 64bit this will die since it will take a movq & jmp, must be only a jmp
        lir().jump(RuntimeAddress(lir().pc()));

        assert lir().offset() - start <= callStubSize : "stub too big";
        lir().endAStub();
    }

    @Override
    protected void throwOp(LIROperand exceptionPC, LIROperand exceptionOop, CodeEmitInfo info, boolean unwind) {
        assert exceptionOop.asRegister() == rax : "must match";
        assert unwind || exceptionPC.asRegister() == rdx : "must match";

        // exception object is not added to oop map by LinearScan
        // (LinearScan assumes that no oops are in fixed registers)
        info.addRegisterOop(exceptionOop);
        Runtime1.StubID unwindId;

        if (!unwind) {
            // get current pc information
            // pc is only needed if the method has an exception handler, the unwind code does not need it.
            int pcForAthrowOffset = lir().offset();
            InternalAddress pcForAthrow = new InternalAddress(lir().pc());
            lir().lea(exceptionPC.asRegister(), pcForAthrow);
            addCallInfo(pcForAthrowOffset, info); // for exception handler

            lir().verifyNotNullOop(rax);
            // search an exception handler (rax: exception oop, rdx: throwing pc)
            if (compilation().hasFpuCode()) {
                unwindId = Runtime1.handleExceptionId;
            } else {
                unwindId = Runtime1.handleExceptionNofpuId;
            }
        } else {
            unwindId = Runtime1.unwindExceptionId;
        }
        lir().call(RuntimeAddress(Runtime1.entryFor(unwindId)));

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
            Register value = left.asRegister();
            assert value != SHIFTCount : "left cannot be ECX";

            switch (code) {
                case lirShl:
                    lir().shll(value);
                    break;
                case lirShr:
                    lir().sarl(value);
                    break;
                case lirUshr:
                    lir().shrl(value);
                    break;
                default:
                    Util.shouldNotReachHere();
            }
        } else if (left.isDoubleCpu()) {
            Register lo = left.asRegisterLo();
            Register hi = left.asRegisterHi();
            assert lo != SHIFTCount && hi != SHIFTCount : "left cannot be ECX";

            if (compilation.target.arch.is64bit()) {
                switch (code) {
                    case lirShl:
                        lir().shlptr(lo);
                        break;
                    case lirShr:
                        lir().sarptr(lo);
                        break;
                    case lirUshr:
                        lir().shrptr(lo);
                        break;
                    default:
                        Util.shouldNotReachHere();
                }
            } else {

                switch (code) {
                    case lirShl:
                        lir().lshl(hi, lo);
                        break;
                    case lirShr:
                        lir().lshr(hi, lo, true);
                        break;
                    case lirUshr:
                        lir().lshr(hi, lo, false);
                        break;
                    default:
                        Util.shouldNotReachHere();
                }
            }
        } else {
            Util.shouldNotReachHere();
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
                    lir().shll(value, count);
                    break;
                case Shr:
                    lir().sarl(value, count);
                    break;
                case Ushr:
                    lir().shrl(value, count);
                    break;
                default:
                    Util.shouldNotReachHere();
            }
        } else if (dest.isDoubleCpu()) {

            if (!compilation.target.arch.is64bit()) {
                Util.shouldNotReachHere();
            }

            // first move left into dest so that left is not destroyed by the shift
            Register value = dest.asRegisterLo();
            count = count & 0x1F; // Java spec

            moveRegs(left.asRegisterLo(), value);
            switch (code) {
                case lirShl:
                    lir().shlptr(value, count);
                    break;
                case lirShr:
                    lir().sarptr(value, count);
                    break;
                case lirUshr:
                    lir().shrptr(value, count);
                    break;
                default:
                    Util.shouldNotReachHere();
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    private void storeParameter(Register r, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        lir().movptr(new Address(X86Register.rsp, offsetFromRspInBytes), r);
    }

    private void storeParameter(int c, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        lir().movptr(new Address(X86Register.rsp, offsetFromRspInBytes), c);
    }

    private void storeParameter(Object o, int offsetFromRspInWords) {
        assert offsetFromRspInWords >= 0 : "invalid offset from rsp";
        int offsetFromRspInBytes = offsetFromRspInWords * compilation.target.arch.wordSize;
        assert offsetFromRspInBytes < frameMap().reservedArgumentAreaSize() : "invalid offset";
        lir().movoop(new Address(X86Register.rsp, offsetFromRspInBytes), o);
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
        BasicType basicType = defaultType != null ? defaultType.elementType().basicType() : TILLEGAL;
        if (basicType == BasicType.Array)
            basicType = BasicType.Object;

        // if we don't know anything or it's an object array, just go through the generic arraycopy
        if (defaultType == null) {
            Label done;
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
            assert compilation.target.arch.is64bit() || (src == rcx && srcPos == rdx) : "mismatch in calling convention";

            Address entry = CASTFROMFNPTR(Address, Runtime1.arraycopy);

            // pass arguments: may push as this is not a safepoint; SP must be fix at each safepoint
            if (compilation.target.arch.is64bit()) {
                // The arguments are in java calling convention so we can trivially shift them to C
                // convention
                assert ifferentRegisters(cRarg0, jRarg1, jRarg2, jRarg3, jRarg4);
                lir().mov(cRarg0, jRarg0);
                assertDifferentRegisters(cRarg1, jRarg2, jRarg3, jRarg4);
                lir().mov(cRarg1, jRarg1);
                assertDifferentRegisters(cRarg2, jRarg3, jRarg4);
                lir().mov(cRarg2, jRarg2);
                assertDifferentRegisters(cRarg3, jRarg4);
                lir().mov(cRarg3, jRarg3);
                if (compilation.target.arch.isWindows()) {
                    // Allocate abi space for args but be sure to keep stack aligned
                    lir().subptr(rsp, 6 * compilation.target.arch.wordSize);
                    storeParameter(jRarg4, 4);
                    lir().call(RuntimeAddress(entry));
                    lir().addptr(rsp, 6 * compilation.target.arch.wordSize);
                } else {
                    lir().mov(cRarg4, jRarg4);
                    lir().call(RuntimeAddress(entry));
                }
            } else {
                lir().push(length);
                lir().push(dstPos);
                lir().push(dst);
                lir().push(srcPos);
                lir().push(src);
                lir().callVMLeaf(entry, 5); // removes pushed parameter from the stack

            }

            lir().cmpl(rax, 0);
            lir().jcc(Assembler.equal, stub.continuation());

            // Reload values from the stack so they are where the stub
            // expects them.
            lir().movptr(dst, Address(rsp, 0 * BytesPerWord));
            lir().movptr(dstPos, Address(rsp, 1 * BytesPerWord));
            lir().movptr(length, Address(rsp, 2 * BytesPerWord));
            lir().movptr(srcPos, Address(rsp, 3 * BytesPerWord));
            lir().movptr(src, Address(rsp, 4 * BytesPerWord));
            lir().jmp(stub.entry());

            lir().bind(stub.continuation());
            return;
        }

        assert defaultType != null && defaultType.isArrayKlass() && defaultType.isLoaded() : "must be true at this point";

        int elemSize = type2aelembytes(basicType);
        int shiftAmount;
        Address.ScaleFactor scale;

        switch (elemSize) {
            case 1:
                shiftAmount = 0;
                scale = Address.times_1;
                break;
            case 2:
                shiftAmount = 1;
                scale = Address.times_2;
                break;
            case 4:
                shiftAmount = 2;
                scale = Address.times_4;
                break;
            case 8:
                shiftAmount = 3;
                scale = Address.times_8;
                break;
            default:
                Util.shouldNotReachHere();
        }

        Address srcLengthAddr = Address(src, arrayOopDesc.lengthOffsetInBytes());
        Address dstLengthAddr = Address(dst, arrayOopDesc.lengthOffsetInBytes());
        Address srcKlassAddr = Address(src, oopDesc.klassOffsetInBytes());
        Address dstKlassAddr = Address(dst, oopDesc.klassOffsetInBytes());

        // length and pos's are all sign extended at this point on 64bit

        // test for null
        if (flags & LIROpArrayCopy.srcNullCheck) {
            lir().testptr(src, src);
            lir().jcc(Assembler.zero, stub.entry());
        }
        if (flags & LIROpArrayCopy.dstNullCheck) {
            lir().testptr(dst, dst);
            lir().jcc(Assembler.zero, stub.entry());
        }

        // check if negative
        if (flags & LIROpArrayCopy.srcPosPositiveCheck) {
            lir().testl(srcPos, srcPos);
            lir().jcc(Assembler.less, stub.entry());
        }
        if (flags & LIROpArrayCopy.dstPosPositiveCheck) {
            lir().testl(dstPos, dstPos);
            lir().jcc(Assembler.less, stub.entry());
        }
        if (flags & LIROpArrayCopy.lengthPositiveCheck) {
            lir().testl(length, length);
            lir().jcc(Assembler.less, stub.entry());
        }

        if (flags & LIROpArrayCopy.srcRangeCheck) {
            lir().lea(tmp, Address(srcPos, length, Address.times_1, 0));
            lir().cmpl(tmp, srcLengthAddr);
            lir().jcc(Assembler.above, stub.entry());
        }
        if (flags & LIROpArrayCopy.dstRangeCheck) {
            lir().lea(tmp, Address(dstPos, length, Address.times_1, 0));
            lir().cmpl(tmp, dstLengthAddr);
            lir().jcc(Assembler.above, stub.entry());
        }

        if (flags & LIROpArrayCopy.typeCheck) {
            lir().movptr(tmp, srcKlassAddr);
            lir().cmpptr(tmp, dstKlassAddr);
            lir().jcc(Assembler.notEqual, stub.entry());
        }

        boolean assertDefined = false;
        assert assertDefined = true;
        if (assertDefined) {
            if (basicType != BasicType.Object || !(flags & LIROpArrayCopy.typeCheck)) {
                // Sanity check the known type with the incoming class. For the
                // primitive case the types must match exactly with src.klass and
                // dst.klass each exactly matching the default type. For the
                // object array case : if no type check is needed then either the
                // dst type is exactly the expected type and the src type is a
                // subtype which we can't check or src is the same array as dst
                // but not necessarily exactly of type defaultType.
                Label knownOk;
                Label halt;
                lir().movoop(tmp, defaultType.encoding());
                if (basicType != BasicType.Object) {
                    lir().cmpptr(tmp, dstKlassAddr);
                    lir().jcc(Assembler.notEqual, halt);
                    lir().cmpptr(tmp, srcKlassAddr);
                    lir().jcc(Assembler.equal, knownOk);
                } else {
                    lir().cmpptr(tmp, dstKlassAddr);
                    lir().jcc(Assembler.equal, knownOk);
                    lir().cmpptr(src, dst);
                    lir().jcc(Assembler.equal, knownOk);
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
            assertDifferentRegisters(cRarg0, dst, dstPos, length);
            lir().lea(cRarg0, Address(src, srcPos, scale, arrayOopDesc.baseOffsetInBytes(basicType)));
            assertDifferentRegisters(cRarg1, length);
            lir().lea(cRarg1, Address(dst, dstPos, scale, arrayOopDesc.baseOffsetInBytes(basicType)));
            lir().mov(cRarg2, length);

        } else {
            lir().lea(tmp, Address(src, srcPos, scale, arrayOopDesc.baseOffsetInBytes(basicType)));
            storeParameter(tmp, 0);
            lir().lea(tmp, Address(dst, dstPos, scale, arrayOopDesc.baseOffsetInBytes(basicType)));
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
        Register obj = op.objOpr().asRegister(); // may not be an oop
        Register hdr = op.hdrOpr().asRegister();
        Register lock = op.lockOpr().asRegister();
        if (!C1XOptions.UseFastLocking) {
            lir().jmp(op.stub().entry());
        } else if (op.code() == LIROpcode.Lock) {
            Register scratch = X86Register.noreg;
            if (C1XOptions.UseBiasedLocking) {
                scratch = op.scratchOpr().asRegister();
            }
            assert BasicLock.displacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            // add debug info for NullPointerException only if one is possible
            int nullCheckOffset = lir().lockObject(hdr, obj, lock, scratch, op.stub().entry());
            if (op.info() != null) {
                addDebugInfoForNullCheck(nullCheckOffset, op.info());
            }
            // done
        } else if (op.code() == LIROpcode.Unlock) {
            assert BasicLock.displacedHeaderOffsetInBytes() == 0 : "lockReg must point to the displaced header";
            lir().unlockObject(hdr, obj, lock, op.stub().entry());
        } else {
            Util.shouldNotReachHere();
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
        CiProfileData data = md.bciToData(bci);
        assert data.isCounterData() : "need CounterData for calls";
        assert op.mdo().isSingleCpu() : "mdo must be allocated";
        Register mdo = op.mdo().asRegister();
        lir().movoop(mdo, md.encoding());
        Address counterAddr = new Address(mdo, md.byteOffsetOfSlot(data, CounterData.countOffset()));
        lir().addl(counterAddr, DataLayout.counterIncrement);
        Bytecodes.Code bc = method.javaCodeAtBci(bci);
        // Perform additional virtual call profiling for invokevirtual and
        // invokeinterface bytecodes
        if ((bc == Bytecodes.invokevirtual || bc == Bytecodes.invokeinterface) && Tier1ProfileVirtualCalls) {
            assert op.recv().isSingleCpu() : "recv must be allocated";
            Register recv = op.recv().asRegister();
            assert ifferentRegisters(mdo, recv);
            assert data.isVirtualCallData() : "need VirtualCallData for virtual calls";
            ciKlass knownKlass = op.knownHolder();
            if (Tier1OptimizeVirtualCallProfiling && knownKlass != null) {
                // We know the type that will be seen at this call site; we can
                // statically update the methodDataOop rather than needing to do
                // dynamic tests on the receiver type

                // NOTE: we should probably put a lock around this search to
                // avoid collisions by concurrent compilations
                ciVirtualCallData vcData = (ciVirtualCallData) data;
                uint i;
                for (i = 0; i < VirtualCallData.rowLimit(); i++) {
                    ciKlass receiver = vcData.receiver(i);
                    if (knownKlass.equals(receiver)) {
                        Address dataAddr = new Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverCountOffset(i)));
                        lir().addl(dataAddr, DataLayout.counterIncrement);
                        return;
                    }
                }

                // Receiver type not found in profile data; select an empty slot

                // Note that this is less efficient than it should be because it
                // always does a write to the receiver part of the
                // VirtualCallData rather than just the first time
                for (i = 0; i < VirtualCallData.rowLimit(); i++) {
                    ciKlass receiver = vcData.receiver(i);
                    if (receiver == null) {
                        Address recvAddr = new Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverOffset(i)));
                        lir().movoop(recvAddr, knownKlass.encoding());
                        Address dataAddr = new Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverCountOffset(i)));
                        lir().addl(dataAddr, DataLayout.counterIncrement);
                        return;
                    }
                }
            } else {
                lir().movptr(recv, Address(recv, oopDesc.klassOffsetInBytes()));
                Label updateDone;
                uint i;
                for (i = 0; i < VirtualCallData.rowLimit(); i++) {
                    Label nextTest;
                    // See if the receiver is receiver[n].
                    lir().cmpptr(recv, Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverOffset(i))));
                    lir().jcc(Assembler.notEqual, nextTest);
                    Address dataAddr = new Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverCountOffset(i)));
                    lir().addl(dataAddr, DataLayout.counterIncrement);
                    lir().jmp(updateDone);
                    lir().bind(nextTest);
                }

                // Didn't find receiver; find next empty slot and fill it in
                for (i = 0; i < VirtualCallData.rowLimit(); i++) {
                    Label nextTest;
                    Address recvAddr = new Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverOffset(i)));
                    lir().cmpptr(recvAddr, (int32t) NULLWORD);
                    lir().jcc(Assembler.notEqual, nextTest);
                    lir().movptr(recvAddr, recv);
                    lir().movl(Address(mdo, md.byteOffsetOfSlot(data, VirtualCallData.receiverCountOffset(i))), DataLayout.counterIncrement);
                    if (i < (VirtualCallData.rowLimit() - 1)) {
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
        Util.shouldNotReachHere();
    }

    @Override
    protected void monitorAddress(int monitorNo, LIROperand dst) {
        lir().lea(dst.asRegister(), frameMap().addressForMonitorLock(monitorNo));
    }

    @Override
    protected void alignBackwardBranchTarget() {
        lir().align(compilation.target.arch.wordSize);
    }

    @Override
    protected void negate(LIROperand left, LIROperand dest) {
        if (left.isSingleCpu()) {
            lir().negl(left.asRegister());
            moveRegs(left.asRegister(), dest.asRegister());

        } else if (left.isDoubleCpu()) {
            Register lo = left.asRegisterLo();
            if (compilation.target.arch.is64bit()) {
                Register dst = dest.asRegisterLo();
                lir().movptr(dst, lo);
                lir().negptr(dst);
            } else {
                Register hi = left.asRegisterHi();
                lir().lneg(hi, lo);
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
            if (left.asXmmFloatReg() != dest.asXmmFloatReg()) {
                lir().movflt(dest.asXmmFloatReg(), left.asXmmFloatReg());
            }
            lir().xorps(dest.asXmmFloatReg(), ExternalAddress((Address) floatSignflipPool));

        } else if (dest.isDoubleXmm()) {
            if (left.asXmmDoubleReg() != dest.asXmmDoubleReg()) {
                lir().movdbl(dest.asXmmDoubleReg(), left.asXmmDoubleReg());
            }
            lir().xorpd(dest.asXmmDoubleReg(), ExternalAddress((Address) doubleSignflipPool));

        } else if (left.isSingleFpu() || left.isDoubleFpu()) {
            assert left.fpu() == 0 : "arg must be on TOS";
            assert dest.fpu() == 0 : "dest must be TOS";
            lir().fchs();

        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void leal(LIROperand addr, LIROperand dest) {
        assert addr.isAddress() && dest.isRegister() : "check";
        Register reg = dest.asPointerRegister(compilation.target.arch);
        lir().lea(reg, asAddress(addr.asAddressPtr()));
    }

    @Override
    protected void rtCall(LIROperand result, Address dest, List<LIROperand> args, LIROperand tmp, CodeEmitInfo info) {
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
                    lir().movdq(dest.asRegisterLo(), src.asXmmDoubleReg());
                } else {
                    lir().movdl(dest.asRegisterLo(), src.asXmmDoubleReg());
                    lir().psrlq(src.asXmmDoubleReg(), 32);
                    lir().movdl(dest.asRegisterHi(), src.asXmmDoubleReg());
                }
            } else if (dest.isDoubleStack()) {
                lir().movdbl(frameMap().addressForSlot(dest.doubleStackIx()), src.asXmmDoubleReg());
            } else if (dest.isAddress()) {
                lir().movdbl(asAddress(dest.asAddressPtr()), src.asXmmDoubleReg());
            } else {
                Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleXmm()) {
            if (src.isDoubleStack()) {
                lir().movdbl(dest.asXmmDoubleReg(), frameMap().addressForSlot(src.doubleStackIx()));
            } else if (src.isAddress()) {
                lir().movdbl(dest.asXmmDoubleReg(), asAddress(src.asAddressPtr()));
            } else {
                Util.shouldNotReachHere();
            }

        } else if (src.isDoubleFpu()) {
            assert src.fpuRegnrLo() == 0 : "must be TOS";
            if (dest.isDoubleStack()) {
                lir().fistpD(frameMap().addressForSlot(dest.doubleStackIx()));
            } else if (dest.isAddress()) {
                lir().fistpD(asAddress(dest.asAddressPtr()));
            } else {
                Util.shouldNotReachHere();
            }

        } else if (dest.isDoubleFpu()) {
            assert dest.fpuRegnrLo() == 0 : "must be TOS";
            if (src.isDoubleStack()) {
                lir().fildD(frameMap().addressForSlot(src.doubleStackIx()));
            } else if (src.isAddress()) {
                lir().fildD(asAddress(src.asAddressPtr()));
            } else {
                Util.shouldNotReachHere();
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    protected void membar() {
        // QQQ sparc TSO uses this,
        lir().membar(Assembler.MembarMaskBits(Assembler.StoreLoad));

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
            lir().mov(resultReg.asRegister(), X86FrameMap.r15thread);
        } else {
            lir().getThread(resultReg.asRegister());
        }
    }

    @Override
    protected void peephole(LIRList list) {
        // Do nothing for now
    }
}*/
