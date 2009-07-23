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

import com.sun.c1x.C1XCompilation;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.CiRuntimeCall;
import com.sun.c1x.target.Register;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;

/**
 * @author Thomas Wuerthinger
 *
 */
public class X86MacroAssembler extends X86Assembler {

    private final int fpuStateSizeInWords;
    private Register rscratch1;
    private final int wordSize;
    private int rspOffset;

    final Register cRarg0;
    final Register cRarg1;
    final Register cRarg2;
    final Register cRarg3;
    final Register cRarg4;

    final Register jRarg0;
    final Register jRarg1;
    final Register jRarg2;
    final Register jRarg3;
    final Register jRarg4;
    private static final long NULLWORD = 0;

    public X86MacroAssembler(C1XCompilation compilation) {
        super(compilation);
        // TODO Auto-generated constructor stub

        rscratch1 = X86FrameMap.rscratch1(compilation.target.arch);
        wordSize = compilation.target.arch.wordSize;

        if (compilation.target.arch.is64bit()) {
            fpuStateSizeInWords = 512 / wordSize;
        } else {
            fpuStateSizeInWords = 27;
        }

        cRarg0 = compilation.runtime.getCRarg(0);
        cRarg1 = compilation.runtime.getCRarg(1);
        cRarg2 = compilation.runtime.getCRarg(2);
        cRarg3 = compilation.runtime.getCRarg(3);
        cRarg4 = compilation.runtime.getCRarg(4);

        jRarg0 = compilation.runtime.getJRarg(0);
        jRarg1 = compilation.runtime.getJRarg(1);
        jRarg2 = compilation.runtime.getJRarg(2);
        jRarg3 = compilation.runtime.getJRarg(3);
        jRarg4 = compilation.runtime.getJRarg(4);
    }

    int biasedLockingEnter(Register lockReg, Register objReg, Register swapReg, Register tmpReg, boolean swapRegContainsMark, Label done, Label slowCase, BiasedLockingCounters counters) {
        if (compilation.target.arch.is64bit()) {
            return biasedLockingEnter64(lockReg, objReg, swapReg, tmpReg, swapRegContainsMark, done, slowCase, counters);
        } else if (compilation.target.arch.is32bit()) {
            return biasedLockingEnter64(lockReg, objReg, swapReg, tmpReg, swapRegContainsMark, done, slowCase, counters);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    int biasedLockingEnter32(Register lockReg, Register objReg, Register swapReg, Register tmpReg, boolean swapRegContainsMark, Label done, Label slowCase, BiasedLockingCounters counters) {

        // TODO: Check what to do with biased locking!
        throw Util.unimplemented();

//
// assert C1XOptions.UseBiasedLocking : "why call this otherwise?";
// assert swapReg == X86Register.rax : "swapReg must be X86Register.rax : for cmpxchg";
// assert Register.assertDifferentRegisters(lockReg, objReg, swapReg);
//
// if (C1XOptions.PrintBiasedLockingStatistics && counters == null)
// counters = BiasedLocking.counters();
//
// boolean needTmpReg = false;
// if (tmpReg == X86Register.noreg) {
// needTmpReg = true;
// tmpReg = lockReg;
// } else {
// assert Register.assertDifferentRegisters(lockReg, objReg, swapReg, tmpReg);
// }
// assert markOopDesc.ageShift == markOopDesc.lockBits + markOopDesc.biasedLockBits :
        // "biased locking makes assumptions about bit layout";
// Address markAddr = new Address(objReg, oopDesc.markOffsetInBytes());
// Address klassAddr = new Address (objReg, oopDesc.klassOffsetInBytes());
// Address savedMarkAddr = new Address(lockReg, 0);
//
// // Biased locking
// // See whether the lock is currently biased toward our thread and
// // whether the epoch is still valid
// // Note that the runtime guarantees sufficient alignment of JavaThread
// // pointers to allow age to be placed into low bits
// // First check to see whether biasing is even enabled for this object
// Label casLabel = new Label();
// int nullCheckOffset = -1;
// if (!swapRegContainsMark) {
// nullCheckOffset = offset();
// movl(swapReg, markAddr);
// }
// if (needTmpReg) {
// push(tmpReg);
// }
// movl(tmpReg, swapReg);
// andl(tmpReg, markOopDesc.biasedLockMaskInPlace);
// cmpl(tmpReg, markOopDesc.biasedLockPattern);
// if (needTmpReg) {
// pop(tmpReg);
// }
// jcc(notEqual, casLabel);
// // The bias pattern is present in the object's header. Need to check
// // whether the bias owner and the epoch are both still current.
// // Note that because there is no current thread register on x86 we
// // need to store off the mark word we read out of the object to
// // avoid reloading it and needing to recheck invariants below. This
// // store is unfortunate but it makes the overall code shorter and
// // simpler.
// movl(savedMarkAddr, swapReg);
// if (needTmpReg) {
// push(tmpReg);
// }
// getThread(tmpReg);
// xorl(swapReg, tmpReg);
// if (swapRegContainsMark) {
// nullCheckOffset = offset();
// }
// movl(tmpReg, klassAddr);
// xorl(swapReg, new Address(tmpReg, Klass.prototypeHeaderOffsetInBytes() + klassOopDesc.klassPartOffsetInBytes()));
// andl(swapReg, ~((int) markOopDesc.ageMaskInPlace));
// if (needTmpReg) {
// pop(tmpReg);
// }
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address)counters.biasedLockEntryCountAddr()));
// }
// jcc(equal, done);
//
// Label tryRevokeBias = new Label();
// Label tryRebias = new Label();
//
// // At this point we know that the header has the bias pattern and
// // that we are not the bias owner in the current epoch. We need to
// // figure out more details about the state of the header in order to
// // know what operations can be legally performed on the object's
// // header.
//
// // If the low three bits in the xor result aren't clear : that means
// // the prototype header is no longer biased and we have to revoke
// // the bias on this object.
// testl(swapReg, markOopDesc.biasedLockMaskInPlace);
// jcc(notZero, tryRevokeBias);
//
// // Biasing is still enabled for this data type. See whether the
// // epoch of the current bias is still valid : meaning that the epoch
// // bits of the mark word are equal to the epoch bits of the
// // prototype header. (Note that the prototype header's epoch bits
// // only change at a safepoint.) If not : attempt to rebias the object
// // toward the current thread. Note that we must be absolutely sure
// // that the current epoch is invalid in order to do this because
// // otherwise the manipulations it performs on the mark word are
// // illegal.
// testl(swapReg, markOopDesc.epochMaskInPlace);
// jcc(notZero, tryRebias);
//
// // The epoch of the current bias is still valid but we know nothing
// // about the owner; it might be set or it might be clear. Try to
// // acquire the bias of the object using an atomic operation. If this
// // fails we will go in to the runtime to revoke the object's bias.
// // Note that we first construct the presumed unbiased header so we
// // don't accidentally blow away another thread's valid bias.
// movl(swapReg, savedMarkAddr);
// andl(swapReg,
// markOopDesc.biasedLockMaskInPlace | markOopDesc.ageMaskInPlace | markOopDesc.epochMaskInPlace);
// if (needTmpReg) {
// push(tmpReg);
// }
// getThread(tmpReg);
// orl(tmpReg, swapReg);
// if (compilation.runtime.isMP()) {
// lock();
// }
// cmpxchgptr(tmpReg, new Address(objReg, 0));
// if (needTmpReg) {
// pop(tmpReg);
// }
// // If the biasing toward our thread failed : this means that
// // another thread succeeded in biasing it toward itself and we
// // need to revoke that bias. The revocation will occur in the
// // interpreter runtime in the slow case.
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address)counters.anonymouslyBiasedLockEntryCountAddr()));
// }
// if (slowCase != null) {
// jcc(notZero, slowCase);
// }
// jmp(done);
//
// bind(tryRebias);
// // At this point we know the epoch has expired : meaning that the
// // current "bias owner" : if any : is actually invalid. Under these
// // circumstances only_ : we are allowed to use the current header's
// // value as the comparison value when doing the cas to acquire the
// // bias in the current epoch. In other words : we allow transfer of
// // the bias from one thread to another directly in this situation.
// //
// // FIXME: due to a lack of registers we currently blow away the age
// // bits in this situation. Should attempt to preserve them.
// if (needTmpReg) {
// push(tmpReg);
// }
// getThread(tmpReg);
// movl(swapReg, klassAddr);
// orl(tmpReg, new Address(swapReg, Klass.prototypeHeaderOffsetInBytes() + klassOopDesc.klassPartOffsetInBytes()));
// movl(swapReg, savedMarkAddr);
// if (compilation.runtime.isMP()) {
// lock();
// }
// cmpxchgptr(tmpReg, new Address(objReg, 0));
// if (needTmpReg) {
// pop(tmpReg);
// }
// // If the biasing toward our thread failed : then another thread
// // succeeded in biasing it toward itself and we need to revoke that
// // bias. The revocation will occur in the runtime in the slow case.
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address)counters.rebiasedLockEntryCountAddr()));
// }
// if (slowCase != null) {
// jcc(notZero, slowCase);
// }
// jmp(done);
//
// bind(tryRevokeBias);
// // The prototype mark in the klass doesn't have the bias bit set any
// // more : indicating that objects of this data type are not supposed
// // to be biased any more. We are going to try to reset the mark of
// // this object to the prototype value and fall through to the
// // CAS-based locking scheme. Note that if our CAS fails : it means
// // that another thread raced us for the privilege of revoking the
// // bias of this particular object : so it's okay to continue in the
// // normal locking code.
// //
// // FIXME: due to a lack of registers we currently blow away the age
// // bits in this situation. Should attempt to preserve them.
// movl(swapReg, savedMarkAddr);
// if (needTmpReg) {
// push(tmpReg);
// }
// movl(tmpReg, klassAddr);
// movl(tmpReg, new Address(tmpReg, Klass.prototypeHeaderOffsetInBytes() + klassOopDesc.klassPartOffsetInBytes()));
// if (compilation.runtime.isMP()) {
// lock();
// }
// cmpxchgptr(tmpReg, new Address(objReg, 0));
// if (needTmpReg) {
// pop(tmpReg);
// }
// // Fall through to the normal CAS-based lock : because no matter what
// // the result of the above CAS : some thread must have succeeded in
// // removing the bias bit from the object's header.
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address)counters.revokedLockEntryCountAddr()));
// }
//
// bind(casLabel);
//
// return nullCheckOffset;
    }

    void inlineCacheCheck(Register receiver, Register iCache) {
        assert verifyOop(receiver);
        // explicit null check not needed since load from [klassOffset] causes a trap
        // check against inline cache
        assert !compilation.runtime.needsExplicitNullCheck(compilation.runtime.klassOffsetInBytes()) : "must add explicit null check";
        //int startOffset = offset();
        cmpptr(iCache, new Address(receiver, compilation.runtime.klassOffsetInBytes()));
        // if icache check fails, then jump to runtime routine
        // Note: RECEIVER must still contain the receiver!
        jumpCc(X86Assembler.Condition.notEqual, new RuntimeAddress(CiRuntimeCall.IcMiss));

        // TODO: Check why the size is 9
//        int icCmpSize = 10;
//        if (compilation.target.arch.is32bit()) {
//            icCmpSize = 9;
//        }
//        assert offset() - startOffset == icCmpSize : "check alignment in emitMethodEntry";
    }

    void increment(Register reg, int value /* = 1 */) {
        if (compilation.target.arch.is64bit()) {
            incrementq(reg, value);
        } else {
            incrementl(reg, value);
        }
    }

    void decrement(Register reg, int value /* = 1 */) {
        if (compilation.target.arch.is64bit()) {
            decrementq(reg, value);
        } else {
            decrementl(reg, value);
        }
    }

    void callVMLeafBase(CiRuntimeCall entryPoint, int numberOfArguments) {
        if (compilation.target.arch.is32bit()) {
            call(new RuntimeAddress(entryPoint));
            increment(X86Register.rsp, numberOfArguments * compilation.target.arch.wordSize);
        } else if (compilation.target.arch.is64bit()) {

            Label l = new Label();
            Label e = new Label();

            if (compilation.target.isWin64()) {
                // Windows always allocates space for it's register args
                assert numberOfArguments <= 4 : "only register arguments supported";
                subq(X86Register.rsp, compilation.runtime.argRegSaveAreaBytes());
            }

            // Align stack if necessary
            testl(X86Register.rsp, 15);
            jcc(X86Assembler.Condition.zero, l);

            subq(X86Register.rsp, 8);
            call(new RuntimeAddress(entryPoint));
            addq(X86Register.rsp, 8);
            jmp(e);

            bind(l);
            call(new RuntimeAddress(entryPoint));
            bind(e);

            if (compilation.target.isWin64()) {
                // restore stack pointer
                addq(X86Register.rsp, compilation.runtime.argRegSaveAreaBytes());
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    void cmpoop(Address src1, Object obj) {
        cmpLiteral32(src1, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
    }

    void cmpoop(Register src1, Object obj) {
        cmpLiteral32(src1, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
    }

    void extendSign(Register hi, Register lo) {
        // According to Intel Doc. AP-526, "Integer Divide", p.18.
        if (compilation.target.isP6() && hi == X86Register.rdx && lo == X86Register.rax) {
            cdql();
        } else {
            movl(hi, lo);
            sarl(hi, 31);
        }
    }

    void fatNop() {
        if (compilation.target.arch.is32bit()) {
            // A 5 byte nop that is safe for patching (see patchVerifiedEntry)
            emitByte(0x26); // es:
            emitByte(0x2e); // cs:
            emitByte(0x64); // fs:
            emitByte(0x65); // gs:
            emitByte(0x90);
        } else if (compilation.target.arch.is64bit()) {

            // A 5 byte nop that is safe for patching (see patchVerifiedEntry)
            // Recommened sequence from 'Software Optimization Guide for the AMD
            // Hammer Processor'
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90);
            emitByte(0x66);
            emitByte(0x90);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void jC2(Register tmp, Label l) {
        // set parity bit if FPU flag C2 is set (via X86Register.rax)
        saveRax(tmp);
        fwait();
        fnstswAx();
        sahf();
        restoreRax(tmp);
        // branch
        jcc(Condition.parity, l);
    }

    void jnC2(Register tmp, Label l) {
        // set parity bit if FPU flag C2 is set (via X86Register.rax)
        saveRax(tmp);
        fwait();
        fnstswAx();
        sahf();
        restoreRax(tmp);
        // branch
        jcc(Condition.noParity, l);
    }

    // 32bit can do a case table jump in one instruction but we no longer allow the base
    // to be installed in the Address class
    void jump(ArrayAddress entry) {
        if (compilation.target.arch.is32bit()) {
            jmp(asAddress(entry));
        } else if (compilation.target.arch.is64bit()) {
            lea(rscratch1, entry.base());
            Address dispatch = entry.index();
            assert dispatch.base == Register.noreg : "must be";
            assert dispatch.rspec == null : "otherwise the copy is not made correctly!";
            dispatch = new Address(rscratch1, dispatch.index, dispatch.scale, dispatch.disp);
            jmp(dispatch);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    // Note: yLo will be destroyed
    void lcmp2int(Register xHi, Register xLo, Register yHi, Register yLo) {
        if (compilation.target.arch.is64bit()) {
            // 64 Bit does not use this!
            Util.shouldNotReachHere();
        }

        // Long compare for Java (semantics as described in JVM spec.)
        Label high = new Label();
        Label low = new Label();
        Label done = new Label();

        cmpl(xHi, yHi);
        jcc(Condition.less, low);
        jcc(Condition.greater, high);
        // xHi is the return register
        xorl(xHi, xHi);
        cmpl(xLo, yLo);
        jcc(Condition.below, low);
        jcc(Condition.equal, done);

        bind(high);
        xorl(xHi, xHi);
        increment(xHi, 1);
        jmp(done);

        bind(low);
        xorl(xHi, xHi);
        decrementl(xHi, 1);

        bind(done);
    }

    void lea(Register dst, AddressLiteral src) {
        if (compilation.target.arch.is32bit()) {
            movLiteral32(dst, Util.safeToInt(src.target()), src.rspec());
        } else if (compilation.target.arch.is64bit()) {
            movLiteral64(dst, src.target(), src.rspec());
        } else {
            Util.shouldNotReachHere();
        }
    }

    void lea(Address dst, AddressLiteral adr) {
        if (compilation.target.arch.is32bit()) {
            // leal(dst, asAddress(adr));
            // see note in movl as to why we must use a move
            movLiteral32(dst, Util.safeToInt(adr.target()), adr.rspec());
        } else if (compilation.target.arch.is64bit()) {

            movLiteral64(rscratch1, adr.target(), adr.rspec());
            movptr(dst, rscratch1);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void leave() {
        if (compilation.target.arch.is32bit()) {
            mov(X86Register.rsp, X86Register.rbp);
            pop(X86Register.rbp);

        } else if (compilation.target.arch.is64bit()) {

            // %%% is this really better? Why not on 32bit too?
            emitByte(0xC9); // LEAVE
        } else {
            Util.shouldNotReachHere();
        }
    }

    void lmul(int xRspOffset, int yRspOffset) {
        // Multiplication of two Java long values stored on the stack
        // as illustrated below. Result is in X86Register.rdx:X86Register.rax.
        //
        // X86Register.rsp --. [ ?? ] \ \
        // .... | yRspOffset |
        // [ yLo ] / (in bytes) | xRspOffset
        // [ yHi ] | (in bytes)
        // .... |
        // [ xLo ] /
        // [ xHi ]
        // ....
        //
        // Basic idea: lo(result) = lo(xLo * yLo)
        // hi(result) = hi(xLo * yLo) + lo(xHi * yLo) + lo(xLo * yHi)
        Address xHi = new Address(X86Register.rsp, xRspOffset + compilation.target.arch.wordSize);
        Address xLo = new Address(X86Register.rsp, xRspOffset);
        Address yHi = new Address(X86Register.rsp, yRspOffset + compilation.target.arch.wordSize);
        Address yLo = new Address(X86Register.rsp, yRspOffset);
        Label quick = new Label();
        // load xHi, yHi and check if quick
        // multiplication is possible
        movl(X86Register.rbx, xHi);
        movl(X86Register.rcx, yHi);
        movl(X86Register.rax, X86Register.rbx);
        orl(X86Register.rbx, X86Register.rcx); // X86Register.rbx, = 0 <=> xHi = 0 and yHi = 0
        jcc(X86Assembler.Condition.zero, quick); // if X86Register.rbx, = 0 do quick multiply
        // do full multiplication
        // 1st step
        mull(yLo); // xHi * yLo
        movl(X86Register.rbx, X86Register.rax); // save lo(xHi * yLo) in X86Register.rbx,
        // 2nd step
        movl(X86Register.rax, xLo);
        mull(X86Register.rcx); // xLo * yHi
        addl(X86Register.rbx, X86Register.rax); // add lo(xLo * yHi) to X86Register.rbx,
        // 3rd step
        bind(quick); // note: X86Register.rbx, = 0 if quick multiply!
        movl(X86Register.rax, xLo);
        mull(yLo); // xLo * yLo
        addl(X86Register.rdx, X86Register.rbx); // correct hi(xLo * yLo)
    }

    void lneg(Register hi, Register lo) {

        if (compilation.target.arch.is64bit()) {
            Util.shouldNotReachHere(); // 64bit doesn't use two regs
        }
        negl(lo);
        adcl(hi, 0);
        negl(hi);
    }

    void lshl(Register hi, Register lo) {
        // Java shift left long support (semantics as described in JVM spec., p.305)
        // (basic idea for shift counts s >= n: x << s == (x << n) << (s - n))
        // shift value is in X86Register.rcx !
        assert hi != X86Register.rcx : "must not use X86Register.rcx";
        assert lo != X86Register.rcx : "must not use X86Register.rcx";
        Register s = X86Register.rcx; // shift count
        int n = compilation.target.arch.bitsPerWord;
        Label l = new Label();
        andl(s, 0x3f); // s := s & 0x3f (s < 0x40)
        cmpl(s, n); // if (s < n)
        jcc(X86Assembler.Condition.less, l); // else (s >= n)
        movl(hi, lo); // x := x << n
        xorl(lo, lo);
        // Note: subl(s, n) is not needed since the Intel shift instructions work X86Register.rcx mod n!
        bind(l); // s (mod n) < n
        shldl(hi, lo); // x := x << s
        shll(lo);
    }

    void lshr(Register hi, Register lo, boolean signExtension) {
        // Java shift right long support (semantics as described in JVM spec., p.306 & p.310)
        // (basic idea for shift counts s >= n: x >> s == (x >> n) >> (s - n))
        assert hi != X86Register.rcx : "must not use X86Register.rcx";
        assert lo != X86Register.rcx : "must not use X86Register.rcx";
        Register s = X86Register.rcx; // shift count
        int n = compilation.target.arch.bitsPerWord;
        Label l = new Label();
        andl(s, 0x3f); // s := s & 0x3f (s < 0x40)
        cmpl(s, n); // if (s < n)
        jcc(X86Assembler.Condition.less, l); // else (s >= n)
        movl(lo, hi); // x := x >> n
        if (signExtension) {
            sarl(hi, 31);
        } else {
            xorl(hi, hi);
        }
        // Note: subl(s, n) is not needed since the Intel shift instructions work X86Register.rcx mod n!
        bind(l); // s (mod n) < n
        shrdl(lo, hi); // x := x >> s
        if (signExtension) {
            sarl(hi);
        } else {
            shrl(hi);
        }
    }

    void movoop(Register dst, Object obj) {
        if (compilation.target.arch.is32bit()) {
            movLiteral32(dst, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
        } else if (compilation.target.arch.is64bit()) {

            movLiteral64(dst, compilation.runtime.convertToPointer64(obj), Relocation.specForImmediate());
        } else {
            Util.shouldNotReachHere();
        }
    }

    void movoop(Address dst, Object obj) {

        if (compilation.target.arch.is32bit()) {
            movLiteral32(dst, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
        } else if (compilation.target.arch.is64bit()) {
            movLiteral64(rscratch1, compilation.runtime.convertToPointer64(obj), Relocation.specForImmediate());
            movq(dst, rscratch1);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void movptr(Register dst, AddressLiteral src) {

        if (compilation.target.arch.is32bit()) {
            if (src.isLval()) {
                movLiteral32(dst, Util.safeToInt(src.target()), src.rspec());
            } else {
                movl(dst, asAddress(src));
            }
        } else if (compilation.target.arch.is64bit()) {

            if (src.isLval()) {
                movLiteral64(dst, src.target(), src.rspec());
            } else {
                if (reachable(src)) {
                    movq(dst, asAddress(src));
                } else {
                    lea(rscratch1, src);
                    movq(dst, new Address(rscratch1, 0));
                }
            }
        } else {
            Util.shouldNotReachHere();
        }

    }

    void movptr(ArrayAddress dst, Register src) {
        if (compilation.target.arch.is32bit()) {
            movl(asAddress(dst), src);
        } else if (compilation.target.arch.is64bit()) {
            movq(asAddress(dst), src);

        } else {
            Util.shouldNotReachHere();
        }
    }

    void movptr(Register dst, ArrayAddress src) {
        if (compilation.target.arch.is32bit()) {
            movl(dst, asAddress(src));
        } else if (compilation.target.arch.is64bit()) {

            movq(dst, asAddress(src));
        } else {
            Util.shouldNotReachHere();
        }
    }

    // src should NEVER be a real pointer. Use AddressLiteral for true pointers
    void movptr(Address dst, long src) {
        if (compilation.target.arch.is32bit()) {
            movl(dst, Util.safeToInt(src));
        } else if (compilation.target.arch.is64bit()) {
            mov64(rscratch1, src);
            movq(dst, rscratch1);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void movsd(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        movsd(dst, asAddress(src));
    }

    void popCalleeSavedRegisters() {
        pop(X86Register.rcx);
        pop(X86Register.rdx);
        pop(X86Register.rdi);
        pop(X86Register.rsi);
    }

    void popFTOS() {
        fldD(new Address(X86Register.rsp, 0));
        addl(X86Register.rsp, 2 * wordSize);
    }

    void pushCalleeSavedRegisters() {
        push(X86Register.rsi);
        push(X86Register.rdi);
        push(X86Register.rdx);
        push(X86Register.rcx);
    }

    void pushFTOS() {
        subl(X86Register.rsp, 2 * compilation.target.arch.wordSize);
        fstpD(new Address(X86Register.rsp, 0));
    }

    void pushoop(Object obj) {

        if (compilation.target.arch.is32bit()) {
            pushLiteral32(compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
        } else if (compilation.target.arch.is64bit()) {
            movoop(rscratch1, obj);
            push(rscratch1);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void pushptr(AddressLiteral src) {

        if (compilation.target.arch.is32bit()) {
            if (src.isLval()) {
                pushLiteral32(Util.safeToInt(src.target()), src.rspec());
            } else {
                pushl(asAddress(src));
            }
        } else if (compilation.target.arch.is64bit()) {
            lea(rscratch1, src);
            if (src.isLval()) {
                push(rscratch1);
            } else {
                pushq(new Address(rscratch1, 0));
            }

        } else {
            Util.shouldNotReachHere();
        }
    }

    void pushptr(Address src) {
        if (compilation.target.arch.is64bit()) {
            pushq(src);
        } else {
            pushl(src);
        }
    }

    void popptr(Address src) {
        if (compilation.target.arch.is64bit()) {
            popq(src);
        } else {
            popl(src);
        }
    }

    void xorptr(Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            xorq(dst, src);
        } else {
            xorl(dst, src);
        }
    }

    void xorptr(Register dst, Address src) {
        if (compilation.target.arch.is64bit()) {
            xorq(dst, src);
        } else {
            xorl(dst, src);
        }
    }

    void setWordIfNotZero(Register dst) {
        xorl(dst, dst);
        setByteIfNotZero(dst);
    }

    void passArg0(X86MacroAssembler masm, Register arg) {
        if (compilation.target.arch.is32bit()) {
            masm.push(arg);
        } else if (compilation.target.arch.is64bit()) {
            if (cRarg0 != arg) {
                masm.mov(cRarg0, arg);
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    void passArg1(X86MacroAssembler masm, Register arg) {
        if (compilation.target.arch.is32bit()) {
            masm.push(arg);
        } else if (compilation.target.arch.is64bit()) {
            if (cRarg1 != arg) {
                masm.mov(cRarg1, arg);
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    void passArg2(X86MacroAssembler masm, Register arg) {
        if (compilation.target.arch.is32bit()) {
            masm.push(arg);
        } else if (compilation.target.arch.is64bit()) {
            if (cRarg2 != arg) {
                masm.mov(cRarg2, arg);
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    void passArg3(X86MacroAssembler masm, Register arg) {
        if (compilation.target.arch.is32bit()) {
            masm.push(arg);
        } else if (compilation.target.arch.is64bit()) {
            if (cRarg3 != arg) {
                masm.mov(cRarg3, arg);
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    // 64 bit versions

    Address asAddress(AddressLiteral adr) {
        assert compilation.target.arch.is64bit();
        // amd64 always does this as a pc-rel
        // we can be absolute or disp based on the instruction type
        // jmp/call are displacements others are absolute
        assert !adr.isLval() : "must be rval";
        assert reachable(adr) : "must be";
        return new Address(Util.safeToInt(adr.target() - pc()), adr.target(), adr.reloc());

    }

    Address asAddress(ArrayAddress adr) {
        assert compilation.target.arch.is64bit();
        AddressLiteral base = adr.base();
        lea(rscratch1, base);
        Address index = adr.index();
        assert index.disp == 0 : "must not have disp"; // maybe it can?
        Address array = new Address(rscratch1, index.index, index.scale, index.disp);
        return array;
    }

    int biasedLockingEnter64(Register lockReg, Register objReg, Register swapReg, Register tmpReg, boolean swapRegContainsMark, Label done, Label slowCase, BiasedLockingCounters counters) {

        assert compilation.target.arch.is64bit();
        // TODO: Check what to do with biased locking!
        throw Util.unimplemented();
//
// assert UseBiasedLocking : "why call this otherwise?";
// assert swapReg == X86Register.rax : "swapReg must be X86Register.rax for cmpxchgq";
// assert tmpReg != X86Register.noreg : "tmpReg must be supplied";
// assert ifferentRegisters(lockReg, objReg, swapReg, tmpReg);
// assert markOopDesc.ageShift == markOopDesc.lockBits + markOopDesc.biasedLockBits :
        // "biased locking makes assumptions about bit layout";
// Address markAddr = new Address(objReg, oopDesc.markOffsetInBytes());
// Address savedMarkAddr= new Address(lockReg, 0);
//
// if (PrintBiasedLockingStatistics && counters == null)
// counters = BiasedLocking.counters();
//
// // Biased locking
// // See whether the lock is currently biased toward our thread and
// // whether the epoch is still valid
// // Note that the runtime guarantees sufficient alignment of JavaThread
// // pointers to allow age to be placed into low bits
// // First check to see whether biasing is even enabled for this object
// Label casLabel = new Label();
// int nullCheckOffset = -1;
// if (!swapRegContainsMark) {
// nullCheckOffset = offset();
// movq(swapReg, markAddr);
// }
// movq(tmpReg, swapReg);
// andq(tmpReg, markOopDesc.biasedLockMaskInPlace);
// cmpq(tmpReg, markOopDesc.biasedLockPattern);
// jcc(notEqual, casLabel);
// // The bias pattern is present in the object's header. Need to check
// // whether the bias owner and the epoch are both still current.
// loadPrototypeHeader(tmpReg, objReg);
// orq(tmpReg, r15Thread);
// xorq(tmpReg, swapReg);
// andq(tmpReg, ~((int) markOopDesc.ageMaskInPlace));
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address) counters.anonymouslyBiasedLockEntryCountAddr()));
// }
// jcc(equal, done);
//
// Label tryRevokeBias = new Label();
// Label tryRebias = new Label();
//
// // At this point we know that the header has the bias pattern and
// // that we are not the bias owner in the current epoch. We need to
// // figure out more details about the state of the header in order to
// // know what operations can be legally performed on the object's
// // header.
//
// // If the low three bits in the xor result aren't clear : that means
// // the prototype header is no longer biased and we have to revoke
// // the bias on this object.
// testq(tmpReg, markOopDesc.biasedLockMaskInPlace);
// jcc(notZero, tryRevokeBias);
//
// // Biasing is still enabled for this data type. See whether the
// // epoch of the current bias is still valid : meaning that the epoch
// // bits of the mark word are equal to the epoch bits of the
// // prototype header. (Note that the prototype header's epoch bits
// // only change at a safepoint.) If not : attempt to rebias the object
// // toward the current thread. Note that we must be absolutely sure
// // that the current epoch is invalid in order to do this because
// // otherwise the manipulations it performs on the mark word are
// // illegal.
// testq(tmpReg, markOopDesc.epochMaskInPlace);
// jcc(notZero, tryRebias);
//
// // The epoch of the current bias is still valid but we know nothing
// // about the owner; it might be set or it might be clear. Try to
// // acquire the bias of the object using an atomic operation. If this
// // fails we will go in to the runtime to revoke the object's bias.
// // Note that we first construct the presumed unbiased header so we
// // don't accidentally blow away another thread's valid bias.
// andq(swapReg,
// markOopDesc.biasedLockMaskInPlace | markOopDesc.ageMaskInPlace | markOopDesc.epochMaskInPlace);
// movq(tmpReg, swapReg);
// orq(tmpReg, r15Thread);
// if (compilation.runtime.isMP()) {
// lock();
// }
// cmpxchgq(tmpReg, new Address(objReg, 0));
// // If the biasing toward our thread failed : this means that
// // another thread succeeded in biasing it toward itself and we
// // need to revoke that bias. The revocation will occur in the
// // interpreter runtime in the slow case.
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address) counters.anonymouslyBiasedLockEntryCountAddr()));
// }
// if (slowCase != null) {
// jcc(notZero, *slowCase);
// }
// jmp(done);
//
// bind(tryRebias);
// // At this point we know the epoch has expired : meaning that the
// // current "bias owner" : if any : is actually invalid. Under these
// // circumstances only_ : we are allowed to use the current header's
// // value as the comparison value when doing the cas to acquire the
// // bias in the current epoch. In other words : we allow transfer of
// // the bias from one thread to another directly in this situation.
// //
// // FIXME: due to a lack of registers we currently blow away the age
// // bits in this situation. Should attempt to preserve them.
// loadPrototypeHeader(tmpReg, objReg);
// orq(tmpReg, r15Thread);
// if (compilation.runtime.isMP()) {
// lock();
// }
// cmpxchgq(tmpReg, new Address(objReg, 0));
// // If the biasing toward our thread failed : then another thread
// // succeeded in biasing it toward itself and we need to revoke that
// // bias. The revocation will occur in the runtime in the slow case.
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address) counters.rebiasedLockEntryCountAddr()));
// }
// if (slowCase != null) {
// jcc(notZero, slowCase);
// }
// jmp(done);
//
// bind(tryRevokeBias);
// // The prototype mark in the klass doesn't have the bias bit set any
// // more : indicating that objects of this data type are not supposed
// // to be biased any more. We are going to try to reset the mark of
// // this object to the prototype value and fall through to the
// // CAS-based locking scheme. Note that if our CAS fails : it means
// // that another thread raced us for the privilege of revoking the
// // bias of this particular object : so it's okay to continue in the
// // normal locking code.
// //
// // FIXME: due to a lack of registers we currently blow away the age
// // bits in this situation. Should attempt to preserve them.
// loadPrototypeHeader(tmpReg, objReg);
// if (compilation.runtime.isMP()) {
// lock();
// }
// cmpxchgq(tmpReg, new Address(objReg, 0));
// // Fall through to the normal CAS-based lock : because no matter what
// // the result of the above CAS : some thread must have succeeded in
// // removing the bias bit from the object's header.
// if (counters != null) {
// condInc32(zero,
// new ExternalAddress((Address) counters.revokedLockEntryCountAddr()));
// }
//
// bind(casLabel);
//
// return nullCheckOffset;
    }

    void cmp64(Register src1, AddressLiteral src2) {
        assert compilation.target.arch.is64bit();
        assert !src2.isLval() : "should use cmpptr";

        if (reachable(src2)) {
            cmpq(src1, asAddress(src2));
        } else {
            lea(rscratch1, src2);
            cmpq(src1, new Address(rscratch1, 0));
        }
    }

    int correctedIdivq(Register reg) {
        assert compilation.target.arch.is64bit();
        // Full implementation of Java ldiv and lrem; checks for special
        // case as described in JVM spec. : p.243 & p.271. The function
        // returns the (pc) offset of the idivl instruction - may be needed
        // for implicit exceptions.
        //
        // normal case special case
        //
        // input : X86Register.rax: dividend minLong
        // reg: divisor (may not be eax/edx) -1
        //
        // output: X86Register.rax: quotient (= X86Register.rax idiv reg) minLong
        // X86Register.rdx: remainder (= X86Register.rax irem reg) 0
        assert reg != X86Register.rax && reg != X86Register.rdx : "reg cannot be X86Register.rax or X86Register.rdx register";
        final long minLong = 0x8000000000000000L;
        Label normalCase = new Label();
        Label specialCase = new Label();

        // check for special case
        cmp64(X86Register.rax, new ExternalAddress(minLong));
        jcc(X86Assembler.Condition.notEqual, normalCase);
        xorl(X86Register.rdx, X86Register.rdx); // prepare X86Register.rdx for possible special case (where
        // remainder = 0)
        cmpq(reg, -1);
        jcc(X86Assembler.Condition.equal, specialCase);

        // handle normal case
        bind(normalCase);
        cdqq();
        int idivqOffset = offset();
        idivq(reg);

        // normal and special case exit
        bind(specialCase);

        return idivqOffset;
    }

    void decrementq(Register reg, int value) {
        assert compilation.target.arch.is64bit();
        if (value == Integer.MIN_VALUE) {
            subq(reg, value);
            return;
        }
        if (value < 0) {
            incrementq(reg, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            decq(reg);
            return;
        } else {
            subq(reg, value);
            return;
        }
    }

    void decrementq(Address dst, int value) {
        assert compilation.target.arch.is64bit();
        if (value == Integer.MIN_VALUE) {
            subq(dst, value);
            return;
        }
        if (value < 0) {
            incrementq(dst, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            decq(dst);
            return;
        } else {
            subq(dst, value);
            return;
        }
    }

    void incrementq(Register reg, int value) {
        assert compilation.target.arch.is64bit();
        if (value == Integer.MIN_VALUE) {
            addq(reg, value);
            return;
        }
        if (value < 0) {
            decrementq(reg, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            incq(reg);
            return;
        } else {
            addq(reg, value);
            return;
        }
    }

    void incrementq(Address dst, int value) {
        assert compilation.target.arch.is64bit();
        if (value == Integer.MIN_VALUE) {
            addq(dst, value);
            return;
        }
        if (value < 0) {
            decrementq(dst, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            incq(dst);
            return;
        } else {
            addq(dst, value);
            return;
        }
    }

    // These are mostly for initializing null
    void movptr(Address dst, int src) {
        assert compilation.target.arch.is64bit();
        movslq(dst, src);
    }

    void movptr(Register dst, long src) {
        assert compilation.target.arch.is64bit();
        mov64(dst, src);
    }

    void resetLastJavaFrame(boolean clearFp, boolean clearPc) {
        assert compilation.target.arch.is64bit();

        // TODO: Reset last Java frame!
        throw Util.unimplemented();
// // we must set sp to zero to clear frame
// movptr(new Address(X86FrameMap.r15thread, JavaThread.lastJavaSpOffset()), NULLWORD);
// // must clear fp : so that compiled frames are not confused; it is
// // possible that we need it only for debugging
// if (clearFp) {
// movptr(new Address(X86FrameMap.r15thread, JavaThread.lastJavaFpOffset()), NULLWORD);
// }
//
// if (clearPc) {
// movptr(new Address(X86FrameMap.r15thread, JavaThread.lastJavaPcOffset()), NULLWORD);
// }
    }

    void setLastJavaFrame(Register lastJavaSp, Register lastJavaFp, Address lastJavaPc) {
        assert compilation.target.arch.is64bit();

        // TODO: Set last Java frame!
        throw Util.unimplemented();
// // determine lastJavaSp register
// if (!lastJavaSp.isValid()) {
// lastJavaSp = X86Register.rsp;
// }
//
// // lastJavaFp is optional
// if (lastJavaFp.isValid()) {
// movptr(new Address(X86FrameMap.r15thread, JavaThread.lastJavaFpOffset()), lastJavaFp);
// }
//
// // lastJavaPc is optional
// if (lastJavaPc != null) {
// Address javaPc = new Address(X86FrameMap.r15thread, JavaThread.frameAnchorOffset() +
        // JavaFrameAnchor.lastJavaPcOffset());
// lea(rscratch1, new InternalAddress(lastJavaPc));
// movptr(javaPc, rscratch1);
// }
//
// movptr(Address = new Address(X86FrameMap.r15thread, JavaThread.lastJavaSpOffset()), lastJavaSp);
    }

    void stop(String msg) {

        if (compilation.target.arch.is64bit()) {
            int rip = pc();
            pusha(); // get regs on stack
            lea(cRarg0, new ExternalAddress(Util.stringToAddress(msg)));
            lea(cRarg1, new InternalAddress(rip));
            movq(cRarg2, X86Register.rsp); // pass pointer to regs array
            andq(X86Register.rsp, -16); // align stack as required by ABI
            call(new RuntimeAddress(CiRuntimeCall.Debug));
            hlt();
        } else {
            throw Util.unimplemented();
        }
    }

    void warn(String msg) {
        if (compilation.target.arch.is64bit()) {
            push(X86Register.r12);
            movq(X86Register.r12, X86Register.rsp);
            andq(X86Register.rsp, -16); // align stack as required by pushCPUState and call

            pushCPUState(); // keeps alignment at 16 bytes
            lea(cRarg0, new ExternalAddress(Util.stringToAddress(msg)));
            callVMLeaf(CiRuntimeCall.Warning, 0);
            popCPUState();

            movq(X86Register.rsp, X86Register.r12);
            pop(X86Register.r12);
        } else {
            throw Util.unimplemented();
        }
    }

    // Now versions that are common to 32/64 bit

    void addptr(Register dst, int imm32) {

        if (compilation.target.arch.is64bit()) {
            addq(dst, imm32);
        } else {
            addl(dst, imm32);
        }
    }

    void addptr(Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    void addptr(Address dst, Register src) {

        if (compilation.target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    @Override
    public void align(int modulus) {
        if (offset() % modulus != 0) {
            nop(modulus - (offset() % modulus));
        }
    }

    @Override
    public void makeOffset(int length) {
        nop(length);
    }

    void andpd(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        andpd(dst, asAddress(src));
    }

    void andptr(Register dst, int imm32) {
        if (compilation.target.arch.is64bit()) {
            andq(dst, imm32);
        } else {
            andl(dst, imm32);
        }
    }

    void atomicIncl(AddressLiteral counterAddr) {
        pushf();
        if (compilation.runtime.isMP()) {
            lock();
        }
        incrementl(counterAddr);
        popf();
    }

    // Writes to stack successive pages until offset reached to check for
    // stack overflow + shadow pages. This clobbers tmp.
    void bangStackSize(Register size, Register tmp) {
        movptr(tmp, X86Register.rsp);
        // Bang stack for total size given plus shadow page size.
        // Bang one page at a time because large size can bang beyond yellow and
        // red zones.
        Label loop = new Label();
        bind(loop);
        movl(new Address(tmp, (-compilation.runtime.vmPageSize())), size);
        subptr(tmp, compilation.runtime.vmPageSize());
        subl(size, compilation.runtime.vmPageSize());
        jcc(X86Assembler.Condition.greater, loop);

        // Bang down shadow pages too.
        // The -1 because we already subtracted 1 page.
        for (int i = 0; i < C1XOptions.StackShadowPages - 1; i++) {
            // this could be any sized move but this is can be a debugging crumb
            // so the bigger the better.
            movptr(new Address(tmp, (-i * compilation.runtime.vmPageSize())), size);
        }
    }

    void biasedLockingExit(Register objReg, Register tempReg, Label done) {
        assert C1XOptions.UseBiasedLocking;

        // Check for biased locking unlock case : which is a no-op
        // Note: we do not have to check the thread ID for two reasons.
        // First : the interpreter checks for IllegalMonitorStateException at
        // a higher level. Second : if the bias was revoked while we held the
        // lock : the object could not be rebiased toward another thread : so
        // the bias bit would be clear.
        movptr(tempReg, new Address(objReg, compilation.runtime.markOffsetInBytes()));
        andptr(tempReg, compilation.runtime.biasedLockMaskInPlace());
        cmpptr(tempReg, compilation.runtime.biasedLockPattern());
        jcc(X86Assembler.Condition.equal, done);
    }

    void c2bool(Register x) {
        // implements x == 0 ? 0 : 1
        // note: must only look at least-significant byte of x
        // since C-style booleans are stored in one byte
        // only! (was bug)
        andl(x, 0xFF);
        setb(X86Assembler.Condition.notZero, x);
    }

    void call(AddressLiteral entry) {
        if (reachable(entry)) {
            callLiteral(entry.target(), entry.rspec());
        } else {
            lea(rscratch1, entry);
            call(rscratch1);
        }
    }

    // Implementation of callVM versions

    void callVM(Register oopResult, CiRuntimeCall entryPoint, boolean checkExceptions) {
        Label c = new Label();
        Label e = new Label();
        call(c);
        jmp(e);

        bind(c);
        callVMHelper(oopResult, entryPoint, 0, checkExceptions);
        ret(0);

        bind(e);
    }

    void callVM(Register oopResult, CiRuntimeCall entryPoint, Register arg1, boolean checkExceptions) {
        Label c = new Label();
        Label e = new Label();
        call(c);
        jmp(e);

        bind(c);
        passArg1(this, arg1);
        callVMHelper(oopResult, entryPoint, 1, checkExceptions);
        ret(0);

        bind(e);
    }

    void callVM(Register oopResult, CiRuntimeCall entryPoint, Register arg1, Register arg2, boolean checkExceptions) {
        Label c = new Label();
        Label e = new Label();
        call(c);
        jmp(e);

        bind(c);

        assert !compilation.target.arch.is64bit() || arg1 != cRarg2 : "smashed arg";

        passArg2(this, arg2);
        passArg1(this, arg1);
        callVMHelper(oopResult, entryPoint, 2, checkExceptions);
        ret(0);

        bind(e);
    }

    void callVM(Register oopResult, CiRuntimeCall entryPoint, Register arg1, Register arg2, Register arg3, boolean checkExceptions) {
        Label c = new Label();
        Label e = new Label();
        call(c);
        jmp(e);

        bind(c);

        assert !compilation.target.arch.is64bit() || arg1 != cRarg3 : "smashed arg";
        assert !compilation.target.arch.is64bit() || arg2 != cRarg3 : "smashed arg";
        passArg3(this, arg3);

        assert !compilation.target.arch.is64bit() || arg1 != cRarg2 : "smashed arg";
        passArg2(this, arg2);

        passArg1(this, arg1);
        callVMHelper(oopResult, entryPoint, 3, checkExceptions);
        ret(0);

        bind(e);
    }

    void callVM(Register oopResult, Register lastJavaSp, CiRuntimeCall entryPoint, int numberOfArguments, boolean checkExceptions) {
        Register thread = (compilation.target.arch.is64bit()) ? X86FrameMap.r15thread : Register.noreg;
        callVMBase(oopResult, thread, lastJavaSp, entryPoint, numberOfArguments, checkExceptions);
    }

    void callVM(Register oopResult, Register lastJavaSp, CiRuntimeCall entryPoint, Register arg1, boolean checkExceptions) {
        passArg1(this, arg1);
        callVM(oopResult, lastJavaSp, entryPoint, 1, checkExceptions);
    }

    void callVM(Register oopResult, Register lastJavaSp, CiRuntimeCall entryPoint, Register arg1, Register arg2, boolean checkExceptions) {

        assert !compilation.target.arch.is64bit() || arg1 != cRarg2 : "smashed arg";
        passArg2(this, arg2);
        passArg1(this, arg1);
        callVM(oopResult, lastJavaSp, entryPoint, 2, checkExceptions);
    }

    void callVM(Register oopResult, Register lastJavaSp, CiRuntimeCall entryPoint, Register arg1, Register arg2, Register arg3, boolean checkExceptions) {
        assert !compilation.target.arch.is64bit() || arg1 != cRarg3 : "smashed arg";
        assert !compilation.target.arch.is64bit() || arg2 != cRarg3 : "smashed arg";
        passArg3(this, arg3);
        assert !compilation.target.arch.is64bit() || arg1 != cRarg2 : "smashed arg";
        passArg2(this, arg2);
        passArg1(this, arg1);
        callVM(oopResult, lastJavaSp, entryPoint, 3, checkExceptions);
    }

    void callVMBase(Register oopResult, Register javaThread, Register lastJavaSp, CiRuntimeCall entryPoint, int numberOfArguments, boolean checkExceptions) {
        // determine javaThread register
        if (!javaThread.isValid()) {

            if (compilation.target.arch.is64bit()) {
                javaThread = X86FrameMap.r15thread;
            } else {
                javaThread = X86Register.rdi;
                getThread(javaThread);
            }
        }
        // determine lastJavaSp register
        if (!lastJavaSp.isValid()) {
            lastJavaSp = X86Register.rsp;
        }
        // debugging support
        assert numberOfArguments >= 0 : "cannot have negative number of arguments";
        assert !compilation.target.arch.is64bit() || javaThread == X86FrameMap.r15thread : "unexpected register";
        assert javaThread != oopResult : "cannot use the same register for javaThread & oopResult";
        assert javaThread != lastJavaSp : "cannot use the same register for javaThread & lastJavaSp";

        // push java thread (becomes first argument of C function)

        if (compilation.target.arch.is64bit()) {
            mov(cRarg0, X86FrameMap.r15thread);
        } else {
            push(javaThread);
            numberOfArguments++;
        }
        // set last Java frame before call
        assert lastJavaSp != X86Register.rbp : "can't use ebp/X86Register.rbp";

        // Only interpreter should have to set fp
        setLastJavaFrame(javaThread, lastJavaSp, X86Register.rbp, null);

        // do the call : remove parameters
        callVMLeafBase(entryPoint, numberOfArguments);

        // restore the thread (cannot use the pushed argument since arguments
        // may be overwritten by C code generated by an optimizing compiler);
        // however can use the register value directly if it is callee saved.
        if (compilation.target.arch.is64bit() || javaThread == X86Register.rdi || javaThread == X86Register.rsi) {
            // X86Register.rdi & X86Register.rsi (also r15) are callee saved . nothing to do

            if (C1XOptions.GenerateAssertionCode) {
                Util.guarantee(javaThread != X86Register.rax, "change this code");
                push(X86Register.rax);
                Label l = new Label();
                getThread(X86Register.rax);
                cmpptr(javaThread, X86Register.rax);
                jcc(X86Assembler.Condition.equal, l);
                stop("callVMBase: X86Register.rdi not callee saved?");
                bind(l);
                pop(X86Register.rax);
            }
        } else {
            getThread(javaThread);
        }
        // reset last Java frame
        // Only interpreter should have to clear fp
        resetLastJavaFrame(javaThread, true, false);

        // #ifndef CCINTERP
        // C++ interp handles this in the interpreter
        checkAndHandlePopframe(javaThread);
        checkAndHandleEarlyret(javaThread);

        if (checkExceptions) {
            // check for pending exceptions (javaThread is set upon return)
            cmpptr(new Address(javaThread, compilation.runtime.threadPendingExceptionOffset()), (int) NULLWORD);
            if (!compilation.target.arch.is64bit()) {
                jumpCc(X86Assembler.Condition.notEqual, new RuntimeAddress(CiRuntimeCall.ForwardException));
            } else {
                // This used to conditionally jump to forwardException however it is
                // possible if we relocate that the branch will not reach. So we must jump
                // around so we can always reach

                Label ok = new Label();
                jcc(X86Assembler.Condition.equal, ok);
                jump(new RuntimeAddress(CiRuntimeCall.ForwardException));
                bind(ok);
            }
        }

        // get oop result if there is one and reset the value in the thread
        if (oopResult.isValid()) {
            movptr(oopResult, new Address(javaThread, compilation.runtime.threadVmResultOffset()));
            movptr(new Address(javaThread, compilation.runtime.threadVmResultOffset()), NULLWORD);
            verifyOop(oopResult, "broken oop in callVMBase");
        }
    }

    void callVMHelper(Register oopResult, CiRuntimeCall entryPoint, int numberOfArguments, boolean checkExceptions) {

        // Calculate the value for lastJavaSp
        // somewhat subtle. callVM does an intermediate call
        // which places a return Address on the stack just under the
        // stack pointer as the user finsihed with it. This allows
        // use to retrieve lastJavaPc from lastJavaSp[-1].
        // On 32bit we then have to push additional args on the stack to accomplish
        // the actual requested call. On 64bit callVM only can use register args
        // so the only extra space is the return Address that callVM created.
        // This hopefully explains the calculations here.

        if (compilation.target.arch.is64bit()) {
            // We've pushed one Address : correct lastJavaSp
            lea(X86Register.rax, new Address(X86Register.rsp, compilation.target.arch.wordSize));
        } else {
            lea(X86Register.rax, new Address(X86Register.rsp, (1 + numberOfArguments) * wordSize));
        }

        callVMBase(oopResult, Register.noreg, X86Register.rax, entryPoint, numberOfArguments, checkExceptions);

    }

    void callVMLeaf(CiRuntimeCall entryPoint, int numberOfArguments) {
        callVMLeafBase(entryPoint, numberOfArguments);
    }

    void callVMLeaf(CiRuntimeCall entryPoint, Register arg0) {
        passArg0(this, arg0);
        callVMLeaf(entryPoint, 1);
    }

    void callVMLeaf(CiRuntimeCall entryPoint, Register arg0, Register arg1) {

        assert !compilation.target.arch.is64bit() || arg0 != cRarg1 : "smashed arg";
        passArg1(this, arg1);
        passArg0(this, arg0);
        callVMLeaf(entryPoint, 2);
    }

    void callVMLeaf(CiRuntimeCall entryPoint, Register arg0, Register arg1, Register arg2) {
        assert !compilation.target.arch.is64bit() || arg0 != cRarg2 : "smashed arg";
        assert !compilation.target.arch.is64bit() || arg1 != cRarg2 : "smashed arg";
        passArg2(this, arg2);
        assert !compilation.target.arch.is64bit() || arg0 != cRarg1 : "smashed arg";
        passArg1(this, arg1);
        passArg0(this, arg0);
        callVMLeaf(entryPoint, 3);
    }

    void checkAndHandleEarlyret(Register javaThread) {
    }

    void checkAndHandlePopframe(Register javaThread) {
    }

    void cmp32(AddressLiteral src1, int imm) {
        if (reachable(src1)) {
            cmpl(asAddress(src1), imm);
        } else {
            lea(rscratch1, src1);
            cmpl(new Address(rscratch1, 0), imm);
        }
    }

    void cmp32(Register src1, AddressLiteral src2) {
        assert !src2.isLval() : "use cmpptr";
        if (reachable(src2)) {
            cmpl(src1, asAddress(src2));
        } else {
            lea(rscratch1, src2);
            cmpl(src1, new Address(rscratch1, 0));
        }
    }

    void cmp32(Register src1, int imm) {
        cmpl(src1, imm);
    }

    void cmp32(Register src1, Address src2) {
        cmpl(src1, src2);
    }

    void cmpsd2int(Register opr1, Register opr2, Register dst, boolean unorderedIsLess) {
        assert opr1.isXMM() && opr2.isXMM();
        ucomisd(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(X86Assembler.Condition.parity, l);
            jcc(X86Assembler.Condition.below, l);
            movl(dst, 0);
            jcc(X86Assembler.Condition.equal, l);
            increment(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(X86Assembler.Condition.parity, l);
            jcc(X86Assembler.Condition.above, l);
            movl(dst, 0);
            jcc(X86Assembler.Condition.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void cmpss2int(Register opr1, Register opr2, Register dst, boolean unorderedIsLess) {
        assert opr1.isXMM();
        assert opr2.isXMM();
        ucomiss(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(X86Assembler.Condition.parity, l);
            jcc(X86Assembler.Condition.below, l);
            movl(dst, 0);
            jcc(X86Assembler.Condition.equal, l);
            increment(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(X86Assembler.Condition.parity, l);
            jcc(X86Assembler.Condition.above, l);
            movl(dst, 0);
            jcc(X86Assembler.Condition.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void cmp8(AddressLiteral src1, int imm) {
        if (reachable(src1)) {
            cmpb(asAddress(src1), imm);
        } else {
            lea(rscratch1, src1);
            cmpb(new Address(rscratch1, 0), imm);
        }
    }

    void cmpptr(Register src1, AddressLiteral src2) {

        if (compilation.target.arch.is64bit()) {
            if (src2.isLval()) {
                movptr(rscratch1, src2);
                cmpq(src1, rscratch1);
            } else if (reachable(src2)) {
                cmpq(src1, asAddress(src2));
            } else {
                lea(rscratch1, src2);
                cmpq(src1, new Address(rscratch1, 0));
            }
        } else {
            if (src2.isLval()) {
                cmpLiteral32(src1, Util.safeToInt(src2.target()), src2.rspec());
            } else {
                cmpl(src1, asAddress(src2));
            }
        }
    }

    void cmpptr(Register src1, Register src2) {
        if (compilation.target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(Register src1, Address src2) {
        if (compilation.target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(Register src1, int src2) {
        if (compilation.target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(Address src1, int src2) {
        if (compilation.target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(Address src1, AddressLiteral src2) {
        assert src2.isLval() : "not a mem-mem compare";
        if (compilation.target.arch.is64bit()) {
            // moves src2's literal Address
            movptr(rscratch1, src2);
            cmpq(src1, rscratch1);
        } else {
            cmpLiteral32(src1, Util.safeToInt(src2.target()), src2.rspec());
        }
    }

    void lockedCmpxchgptr(Register reg, AddressLiteral adr) {
        if (reachable(adr)) {
            if (compilation.runtime.isMP()) {
                lock();
            }
            cmpxchgptr(reg, asAddress(adr));
        } else {
            lea(X86FrameMap.rscratch1(compilation.target.arch), adr);
            if (compilation.runtime.isMP()) {
                lock();
            }
            cmpxchgptr(reg, new Address(rscratch1, 0));
        }
    }

    void cmpxchgptr(Register reg, Address adr) {
        if (compilation.target.arch.is64bit()) {
            cmpxchgq(reg, adr);
        } else {
            cmpxchgl(reg, adr);
        }

    }

    void comisd(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        comisd(dst, asAddress(src));
    }

    void comiss(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        comiss(dst, asAddress(src));
    }

    void condInc32(Condition cond, AddressLiteral counterAddr) {
        Condition negatedCond = cond.negate();
        Label l = new Label();
        jcc(negatedCond, l);
        atomicIncl(counterAddr);
        bind(l);
    }

    int correctedIdivl(Register reg) {
        // Full implementation of Java idiv and irem; checks for
        // special case as described in JVM spec. : p.243 & p.271.
        // The function returns the (pc) offset of the idivl
        // instruction - may be needed for implicit exceptions.
        //
        // normal case special case
        //
        // input : X86Register.rax : : dividend minInt
        // reg: divisor (may not be X86Register.rax,/X86Register.rdx) -1
        //
        // output: X86Register.rax : : quotient (= X86Register.rax, idiv reg) minInt
        // X86Register.rdx: remainder (= X86Register.rax, irem reg) 0
        assert reg != X86Register.rax && reg != X86Register.rdx : "reg cannot be X86Register.rax, or X86Register.rdx register";
        int minInt = 0x80000000;
        Label normalCase = new Label();
        Label specialCase = new Label();

        // check for special case
        cmpl(X86Register.rax, minInt);
        jcc(Condition.notEqual, normalCase);
        xorl(X86Register.rdx, X86Register.rdx); // prepare X86Register.rdx for possible special case (where remainder =
        // 0)
        cmpl(reg, -1);
        jcc(Condition.equal, specialCase);

        // handle normal case
        bind(normalCase);
        cdql();
        int idivlOffset = offset();
        idivl(reg);

        // normal and special case exit
        bind(specialCase);

        return idivlOffset;
    }

    void decrementl(Register reg, int value) {
        if (value == Integer.MIN_VALUE) {
            subl(reg, value);
            return;
        }
        if (value < 0) {
            incrementl(reg, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            decl(reg);
            return;
        } else {
            subl(reg, value);
            return;
        }
    }

    void decrementl(Address dst, int value) {
        if (value == Integer.MIN_VALUE) {
            subl(dst, value);
            return;
        }
        if (value < 0) {
            incrementl(dst, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            decl(dst);
            return;
        } else {
            subl(dst, value);
            return;
        }
    }

    void divisionWithShift(Register reg, int shiftValue) {
        assert shiftValue > 0 : "illegal shift value";
        Label isPositive = new Label();
        testl(reg, reg);
        jcc(Condition.positive, isPositive);
        int offset = (1 << shiftValue) - 1;

        if (offset == 1) {
            incrementl(reg, 1);
        } else {
            addl(reg, offset);
        }

        bind(isPositive);
        sarl(reg, shiftValue);
    }

    void emptyFPUStack() {
        if (compilation.target.supportsMmx()) {
            emms();
        } else {
            for (int i = 8; i-- > 0;) {
                ffree(i);
            }
        }
    }

    // Defines obj : preserves varSizeInBytes
    void edenAllocate(Register obj, Register varSizeInBytes, int conSizeInBytes, Register t1, Label slowCase) {

        // TODO: Implement eden allocate!
        throw Util.unimplemented();
// assert obj == X86Register.rax : "obj must be in X86Register.rax, for cmpxchg";
// assert Register.assertDifferentRegisters(obj, varSizeInBytes, t1);
// if (C1XOptions.CMSIncrementalMode || !compilation.runtime.universeSupportsInlineContigAlloc()) {
// jmp(slowCase);
// } else {
// Register end = t1;
// Label retry = new Label();
// bind(retry);
// ExternalAddress heapTop = new ExternalAddress((Address) Universe.heap().topAddr());
// movptr(obj, heapTop);
// if (varSizeInBytes == X86Register.noreg) {
// lea(end, new Address(obj, conSizeInBytes));
// } else {
// lea(end, new Address(obj, varSizeInBytes, Address.times1));
// }
// // if end < obj then we wrapped around => object too long => slow case
// cmpptr(end, obj);
// jcc(below, slowCase);
// cmpptr(end, new ExternalAddress((Address) Universe.heap().endAddr()));
// jcc(above, slowCase);
// // Compare obj with the top addr : and if still equal : store the new top addr in
// // end at the Address of the top addr pointer. Sets ZF if was equal : and clears
// // it otherwise. Use lock prefix for atomicity on MPs.
// lockedCmpxchgptr(end, heapTop);
// jcc(notEqual, retry);
// }
    }

    void enter() {
        push(X86Register.rbp);
        mov(X86Register.rbp, X86Register.rsp);
    }

    void fcmp(Register tmp) {
        fcmp(tmp, 1, true, true);
    }

    void fcmp(Register tmp, int index, boolean popLeft, boolean popRight) {
        assert !popRight || popLeft : "usage error";
        if (compilation.target.supportsCmov()) {
            assert tmp == Register.noreg : "unneeded temp";
            if (popLeft) {
                fucomip(index);
            } else {
                fucomi(index);
            }
            if (popRight) {
                fpop();
            }
        } else {
            assert tmp != Register.noreg : "need temp";
            if (popLeft) {
                if (popRight) {
                    fcompp();
                } else {
                    fcomp(index);
                }
            } else {
                fcom(index);
            }
            // convert FPU condition into eflags condition via X86Register.rax :
            saveRax(tmp);
            fwait();
            fnstswAx();
            sahf();
            restoreRax(tmp);
        }
        // condition codes set as follows:
        //
        // CF (corresponds to C0) if x < y
        // PF (corresponds to C2) if unordered
        // ZF (corresponds to C3) if x = y
    }

    void fcmp2int(Register dst, boolean unorderedIsLess) {
        fcmp2int(dst, unorderedIsLess, 1, true, true);
    }

    void fcmp2int(Register dst, boolean unorderedIsLess, int index, boolean popLeft, boolean popRight) {
        fcmp(compilation.target.supportsCmov() ? Register.noreg : dst, index, popLeft, popRight);
        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(Condition.parity, l);
            jcc(Condition.below, l);
            movl(dst, 0);
            jcc(Condition.equal, l);
            increment(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(Condition.parity, l);
            jcc(Condition.above, l);
            movl(dst, 0);
            jcc(Condition.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void fldD(AddressLiteral src) {
        fldD(asAddress(src));
    }

    void fldS(AddressLiteral src) {
        fldS(asAddress(src));
    }

    void fldX(AddressLiteral src) {
        fldX(asAddress(src));
    }

    void fldcw(AddressLiteral src) {
        fldcw(asAddress(src));
    }

    void fpop() {
        ffree(0);
        fincstp();
    }

    void fremr(Register tmp) {
        saveRax(tmp);
        Label l = new Label();
        bind(l);
        fprem();
        fwait();
        fnstswAx();
        if (compilation.target.arch.is64bit()) {
            testl(X86Register.rax, 0x400);
            jcc(Condition.notEqual, l);
        } else {
            sahf();
            jcc(Condition.parity, l);
        }
        restoreRax(tmp);
        // Result is in ST0.
        // Note: fxch & fpop to get rid of ST1
        // (otherwise FPU stack could overflow eventually)
        fxch(1);
        fpop();
    }

    void incrementl(AddressLiteral dst) {
        if (reachable(dst)) {
            incrementl(asAddress(dst), 1);
        } else {
            lea(rscratch1, dst);
            incrementl(new Address(rscratch1, 0), 1);
        }
    }

    void incrementl(ArrayAddress dst) {
        incrementl(asAddress(dst), 1);
    }

    void incrementl(Register reg, int value) {
        if (value == Integer.MIN_VALUE) {
            addl(reg, value);
            return;
        }
        if (value < 0) {
            decrementl(reg, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            incl(reg);
            return;
        } else {
            addl(reg, value);
            return;
        }
    }

    void incrementl(Address dst, int value) {
        if (value == Integer.MIN_VALUE) {
            addl(dst, value);
            return;
        }
        if (value < 0) {
            decrementl(dst, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && C1XOptions.UseIncDec) {
            incl(dst);
            return;
        } else {
            addl(dst, value);
            return;
        }
    }

    void jump(AddressLiteral dst) {
        if (reachable(dst)) {
            jmpLiteral(dst.target(), dst.rspec());
        } else {
            lea(rscratch1, dst);
            jmp(rscratch1);
        }
    }

    void jumpCc(Condition cc, AddressLiteral dst) {
        if (reachable(dst)) {
            this.setInstMark();
            try {
                relocate(dst.rspec);
                int shortSize = 2;
                int longSize = 6;
                int offs = Util.safeToInt(dst.target() - codeBuffer.position());
                if (dst.reloc() == RelocInfo.Type.none && is8bit(offs - shortSize)) {
                    // 0111 tttn #8-bit disp
                    emitByte(0x70 | cc.value);
                    emitByte((offs - shortSize) & 0xFF);
                } else {
                    // 0000 1111 1000 tttn #32-bit disp
                    emitByte(0x0F);
                    emitByte(0x80 | cc.value);
                    emitInt(offs - longSize);
                }
            } finally {
                this.clearInstMark();
            }
        } else {
            Util.warning("reversing conditional branch");
            Label skip = new Label();
            jccb(cc.negate(), skip);
            lea(rscratch1, dst);
            jmp(rscratch1);
            bind(skip);
        }
    }

    void ldmxcsr(AddressLiteral src) {
        if (reachable(src)) {
            ldmxcsr(asAddress(src));
        } else {
            lea(rscratch1, src);
            ldmxcsr(new Address(rscratch1, 0));
        }
    }

    int loadSignedByte(Register dst, Address src) {
        int off;
        if (compilation.target.arch.is64bit() || compilation.target.isP6()) {
            off = offset();
            movsbl(dst, src); // movsxb
        } else {
            off = loadUnsignedByte(dst, src);
            shll(dst, 24);
            sarl(dst, 24);
        }
        return off;
    }

    // Note: loadSignedShort used to be called loadSignedWord.
    // Although the 'w' in x86 opcodes refers to the term "word" in the assembler
    // manual : which means 16 bits : that usage is found nowhere in HotSpot code.
    // The term "word" in HotSpot means a 32- or 64-bit machine word.
    int loadSignedShort(Register dst, Address src) {
        int off;
        if (compilation.target.arch.is64bit() || compilation.target.isP6()) {
            // This is dubious to me since it seems safe to do a signed 16 => 64 bit
            // version but this is what 64bit has always done. This seems to imply
            // that users are only using 32bits worth.
            off = offset();
            movswl(dst, src); // movsxw
        } else {
            off = loadUnsignedShort(dst, src);
            shll(dst, 16);
            sarl(dst, 16);
        }
        return off;
    }

    int loadUnsignedByte(Register dst, Address src) {
        // According to Intel Doc. AP-526 : "Zero-Extension of Short" : p.16 :
        // and "3.9 Partial Register Penalties" : p. 22.
        int off;
        if (compilation.target.arch.is64bit() || compilation.target.isP6() || src.uses(dst)) {
            off = offset();
            movzbl(dst, src); // movzxb
        } else {
            xorl(dst, dst);
            off = offset();
            movb(dst, src);
        }
        return off;
    }

    // Note: loadUnsignedShort used to be called loadUnsignedWord.
    int loadUnsignedShort(Register dst, Address src) {
        // According to Intel Doc. AP-526, "Zero-Extension of Short", p.16,
        // and "3.9 Partial Register Penalties", p. 22).
        int off;
        if (compilation.target.arch.is64bit() || compilation.target.isP6() || src.uses(dst)) {
            off = offset();
            movzwl(dst, src); // movzxw
        } else {
            xorl(dst, dst);
            off = offset();
            movw(dst, src);
        }
        return off;
    }

    void loadSizedValue(Register dst, Address src, int sizeInBytes, boolean isSigned) {

        int value = sizeInBytes ^ (isSigned ? -1 : 0);

        switch (value) {
            // For case 8, caller is responsible for manually loading
            // the second word into another X86Register.
            case ~8: // fall through:
            case 8:
                if (compilation.target.arch.is64bit()) {
                    movq(dst, src);
                } else {
                    movl(dst, src);
                }
                break;
            case ~4: // fall through:
            case 4:
                movl(dst, src);
                break;
            case ~2:
                loadSignedShort(dst, src);
                break;
            case 2:
                loadUnsignedShort(dst, src);
                break;
            case ~1:
                loadSignedByte(dst, src);
                break;
            case 1:
                loadUnsignedByte(dst, src);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    void mov32(AddressLiteral dst, Register src) {
        if (reachable(dst)) {
            movl(asAddress(dst), src);
        } else {
            lea(rscratch1, dst);
            movl(new Address(rscratch1, 0), src);
        }
    }

    void mov32(Register dst, AddressLiteral src) {
        if (reachable(src)) {
            movl(dst, asAddress(src));
        } else {
            lea(rscratch1, src);
            movl(dst, new Address(rscratch1, 0));
        }
    }

    // C++ boolean manipulation

    void movbool(Register dst, Address src) {
        if (Util.sizeofBoolean() == 1) {
            movb(dst, src);
        } else if (Util.sizeofBoolean() == 2) {
            movw(dst, src);
        } else if (Util.sizeofBoolean() == 4) {
            movl(dst, src);
        } else {
            // unsupported
            Util.shouldNotReachHere();
        }
    }

    void movbool(Address dst, boolean boolconst) {
        if (Util.sizeofBoolean() == 1) {
            movb(dst, Util.toInt(boolconst));
        } else if (Util.sizeofBoolean() == 2) {
            movw(dst, Util.toInt(boolconst));
        } else if (Util.sizeofBoolean() == 4) {
            movl(dst, Util.toInt(boolconst));
        } else {
            // unsupported
            Util.shouldNotReachHere();
        }
    }

    void movbool(Address dst, Register src) {
        if (Util.sizeofBoolean() == 1) {
            movb(dst, src);
        } else if (Util.sizeofBoolean() == 2) {
            movw(dst, src);
        } else if (Util.sizeofBoolean() == 4) {
            movl(dst, src);
        } else {
            // unsupported
            Util.shouldNotReachHere();
        }
    }

    void movbyte(ArrayAddress dst, int src) {
        movb(asAddress(dst), src);
    }

    void movdbl(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        if (reachable(src)) {
            if (C1XOptions.UseXmmLoadAndClearUpper) {
                movsd(dst, asAddress(src));
            } else {
                movlpd(dst, asAddress(src));
            }
        } else {
            lea(rscratch1, src);
            if (C1XOptions.UseXmmLoadAndClearUpper) {
                movsd(dst, new Address(rscratch1, 0));
            } else {
                movlpd(dst, new Address(rscratch1, 0));
            }
        }
    }

    void movflt(Register dst, AddressLiteral src) {

        assert dst.isXMM();
        if (reachable(src)) {
            movss(dst, asAddress(src));
        } else {
            lea(rscratch1, src);
            movss(dst, new Address(rscratch1, 0));
        }
    }

    void movptr(Register dst, Register src) {

        if (compilation.target.arch.is64bit()) {

            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movptr(Register dst, Address src) {

        if (compilation.target.arch.is64bit()) {
            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movptr(Address dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    // sign extend as need a l to ptr sized element
    void movl2ptr(Register dst, Address src) {
        if (compilation.target.arch.is64bit()) {
            movslq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movl2ptr(Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            movslq(dst, src);
        } else if (dst != src) {
            movl(dst, src);
        }
    }

    void movss(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        if (reachable(src)) {
            movss(dst, asAddress(src));
        } else {
            lea(rscratch1, src);
            movss(dst, new Address(rscratch1, 0));
        }
    }

    void nullCheck(Register reg, int offset) {
        if (compilation.runtime.needsExplicitNullCheck(offset)) {
            // provoke OS null exception if reg = null by
            // accessing M[reg] w/o changing any (non-CC) registers
            // NOTE: cmpl is plenty here to provoke a segv
            cmpptr(X86Register.rax, new Address(reg, 0));
            // Note: should probably use testl(X86Register.rax, new Address(reg, 0));
            // may be shorter code (however, this version of
            // testl needs to be implemented first)
        } else {
            // nothing to do, (later) access of M[reg + offset]
            // will provoke OS null exception if reg = null
        }
    }

    void osBreakpoint() {
        // instead of directly emitting a breakpoint, call os:breakpoint for better debugability
        // (e.g., MSVC can't call ps() otherwise)
        call(new RuntimeAddress(CiRuntimeCall.Breakpoint));
    }

    void popCPUState() {
        popFPUState();
        popIUState();
    }

    void popFPUState() {
        if (compilation.target.arch.is64bit()) {
            fxrstor(new Address(X86Register.rsp, 0));
        } else {
            frstor(new Address(X86Register.rsp, 0));
        }
        addptr(X86Register.rsp, fpuStateSizeInWords * wordSize);
    }

    void popIUState() {
        popa();
        if (compilation.target.arch.is64bit()) {
            addq(X86Register.rsp, 8);
        }
        popf();
    }

    // Save Integer and Float state
    // Warning: Stack must be 16 byte aligned (64bit)
    void pushCPUState() {
        pushIUState();
        pushFPUState();
    }

    void pushFPUState() {
        subptr(X86Register.rsp, fpuStateSizeInWords * wordSize);

        if (compilation.target.arch.is64bit()) {

            fxsave(new Address(X86Register.rsp, 0));
        } else {
            fnsave(new Address(X86Register.rsp, 0));
            fwait();
        }
    }

    void pushIUState() {
        // Push flags first because pusha kills them
        pushf();
        // Make sure X86Register.rsp stays 16-byte aligned
        if (compilation.target.arch.is64bit()) {
            subq(X86Register.rsp, 8);
        }
        pusha();
    }

    void resetLastJavaFrame(Register javaThread, boolean clearFp, boolean clearPc) {
        // TODO: Reset last Java frame!
        throw Util.unimplemented();

// // determine javaThread register
// if (!javaThread.isValid()) {
// javaThread = X86Register.rdi;
// getThread(javaThread);
// }
// // we must set sp to zero to clear frame
// movptr(new Address(javaThread, JavaThread.lastJavaSpOffset()), NULLWORD);
// if (clearFp) {
// movptr(new Address(javaThread, JavaThread.lastJavaFpOffset()), NULLWORD);
// }
//
// if (clearPc)
// movptr(new Address(javaThread, JavaThread.lastJavaPcOffset()), NULLWORD);

    }

    void restoreRax(Register tmp) {
        if (tmp == Register.noreg) {
            pop(X86Register.rax);
        } else if (tmp != X86Register.rax) {
            mov(X86Register.rax, tmp);
        }
    }

    void roundTo(Register reg, int modulus) {
        addptr(reg, modulus - 1);
        andptr(reg, -modulus);
    }

    void saveRax(Register tmp) {
        if (tmp == Register.noreg) {
            push(X86Register.rax);
        } else if (tmp != X86Register.rax) {
            mov(tmp, X86Register.rax);
        }
    }


    // Calls to C land
    //
    // When entering C land, the X86Register.rbp, & X86Register.rsp of the last Java frame have to be recorded
    // in the (thread-local) JavaThread object. When leaving C land, the last Java fp
    // has to be reset to 0. This is required to allow proper stack traversal.
    void setLastJavaFrame(Register javaThread, Register lastJavaSp, Register lastJavaFp, Address lastJavaPc) {

        // TODO: Set last Java frame!
        throw Util.unimplemented();
//
// // determine javaThread register
// if (!javaThread.isValid()) {
// javaThread = X86Register.rdi;
// getThread(javaThread);
// }
// // determine lastJavaSp register
// if (!lastJavaSp.isValid()) {
// lastJavaSp = X86Register.rsp;
// }
//
// // lastJavaFp is optional
//
// if (lastJavaFp.isValid()) {
// movptr(new Address(javaThread, JavaThread.lastJavaFpOffset()), lastJavaFp);
// }
//
// // lastJavaPc is optional
//
// if (lastJavaPc != null) {
// lea(new Address(javaThread, JavaThread.frameAnchorOffset() + JavaFrameAnchor.lastJavaPcOffset()),
        // InternalAddress(lastJavaPc));
//
// }
// movptr(new Address(javaThread, JavaThread.lastJavaSpOffset()), lastJavaSp);
    }

    void getThread(Register javaThread) {
        // TODO Auto-generated method stub
        // Platform-specific! Solaris / Windows / Linux
    }

    void shlptr(Register dst, int imm8) {
        if (compilation.target.arch.is64bit()) {
            shlq(dst, imm8);
        } else {
            shll(dst, imm8);
        }
    }

    void shrptr(Register dst, int imm8) {
        if (compilation.target.arch.is64bit()) {
            shrq(dst, imm8);
        } else {
            shrl(dst, imm8);
        }
    }

    void signExtendByte(Register reg) {
        if (compilation.target.arch.is64bit() || compilation.target.isP6() && reg.isByte()) {
            movsbl(reg, reg); // movsxb
        } else {
            shll(reg, 24);
            sarl(reg, 24);
        }
    }

    void signExtendShort(Register reg) {
        if (compilation.target.arch.is64bit() || compilation.target.isP6()) {
            movswl(reg, reg); // movsxw
        } else {
            shll(reg, 16);
            sarl(reg, 16);
        }
    }

    void storeCheck(Register obj) {
        // Does a store check for the oop in register obj. The content of
        // register obj is destroyed afterwards.
        storeCheckPart1(obj);
        storeCheckPart2(obj);
    }

    void storeCheck(Register obj, Address dst) {
        storeCheck(obj);
    }

    // split the store check operation so that other instructions can be scheduled inbetween
    void storeCheckPart1(Register obj) {
        // TODO: Store check
        throw Util.unimplemented();
//
// BarrierSet bs = Universe.heap().barrierSet();
// assert bs.kind() == BarrierSet.CardTableModRef : "Wrong barrier set kind";
// shrptr(obj, CardTableModRefBS.cardShift);
    }

    void storeCheckPart2(Register obj) {
        // TODO: Store check
//
// BarrierSet bs = Universe.heap().barrierSet();
// assert bs.kind() == BarrierSet.CardTableModRef : "Wrong barrier set kind";
// CardTableModRefBS ct = (CardTableModRefBS) bs;
// assert sizeof(ct.byteMapBase) == sizeof(jbyte) : "adjust this code";
//
// // The calculation for byteMapBase is as follows:
// // byteMapBase = byteMap - (uintptrT(lowBound) >> cardShift);
// // So this essentially converts an Address to a displacement and
// // it will never need to be relocated. On 64bit however the value may be too
// // large for a 32bit displacement
//
// intptrT disp = (intptrT) ct.byteMapBase;
// if (isSimm32(disp)) {
// Address cardtable = new Address(X86Register.noreg, obj, Address.times1, disp);
// movb(cardtable, 0);
// } else {
// // By doing it as an ExternalAddress disp could be converted to a rip-relative
// // displacement and done in a single instruction given favorable mapping and
// // a smarter version of asAddress. Worst case it is two instructions which
// // is no worse off then loading disp into a register and doing as a simple
// // Address() as above.
// // We can't do as ExternalAddress as the only style since if disp == 0 we'll
// // assert since null isn't acceptable in a reloci (see 6644928). In any case
// // in some cases we'll get a single instruction version.
//
// ExternalAddress cardtable = new ExternalAddress((Address) disp);
// Address index = new Address(X86Register.noreg, obj, Address.times1);
// movb(asAddress(new ArrayAddress(cardtable, index)), 0);
// }
    }

    void subptr(Register dst, int imm32) {
        if (compilation.target.arch.is64bit()) {
            subq(dst, imm32);
        } else {
            subl(dst, imm32);
        }
    }

    void subptr(Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            subq(dst, src);
        } else {
            subl(dst, src);
        }
    }

    void test32(Register src1, AddressLiteral src2) {
        // src2 must be rval

        if (reachable(src2)) {
            testl(src1, asAddress(src2));
        } else {
            lea(rscratch1, src2);
            testl(src1, new Address(rscratch1, 0));
        }
    }

    // C++ boolean manipulation
    void testbool(Register dst) {
        if (Util.sizeofBoolean() == 1) {
            testb(dst, 0xff);
        } else if (Util.sizeofBoolean() == 2) {
            // testw implementation needed for two byte bools
            Util.shouldNotReachHere();
        } else if (Util.sizeofBoolean() == 4) {
            testl(dst, dst);
        } else {
            // unsupported
            Util.shouldNotReachHere();
        }
    }

    void testptr(Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            testq(dst, src);
        } else {
            testl(dst, src);
        }
    }

    // Defines obj : preserves varSizeInBytes : okay for t2 == varSizeInBytes.
    void tlabAllocate(Register obj, Register varSizeInBytes, int conSizeInBytes, Register t1, Register t2, Label slowCase) {
        assert Register.assertDifferentRegisters(obj, t1, t2);
        assert Register.assertDifferentRegisters(obj, varSizeInBytes, t1);
        Register end = t2;
        Register thread = t1;
        if (compilation.target.arch.is64bit()) {
            thread = X86FrameMap.r15thread;
        }

        verifyTlab();

        if (!compilation.target.arch.is64bit()) {
            getThread(thread);
        }

        movptr(obj, new Address(thread, compilation.runtime.threadTlabTopOffset()));
        if (varSizeInBytes == Register.noreg) {
            lea(end, new Address(obj, conSizeInBytes));
        } else {
            lea(end, new Address(obj, varSizeInBytes, Address.ScaleFactor.times1));
        }
        cmpptr(end, new Address(thread, compilation.runtime.threadTlabEndOffset()));
        jcc(X86Assembler.Condition.above, slowCase);

        // update the tlab top pointer
        movptr(new Address(thread, compilation.runtime.threadTlabTopOffset()), end);

        // recover varSizeInBytes if necessary
        if (varSizeInBytes == end) {
            subptr(varSizeInBytes, obj);
        }
        verifyTlab();
    }

    // Preserves X86Register.rbx : and X86Register.rdx.
    void tlabRefill(Label retry, Label tryEden, Label slowCase) {

        // TODO: Tlab refill
        throw Util.unimplemented();
//
// Register top = X86Register.rax;
// Register t1 = X86Register.rcx;
// Register t2 = X86Register.rsi;
// Register threadReg = (compilation.target.arch.is64bit()) ? X86FrameMap.r15thread : X86Register.rdi;
// assert Register.assertDifferentRegisters(top, threadReg, t1, t2, /* preserve: */X86Register.rbx, X86Register.rdx);
// Label doRefill = new Label();
// Label discardTlab = new Label();
//
// if (C1XOptions.CMSIncrementalMode || !compilation.runtime.universeSupportsInlineContigAlloc()) {
// // No allocation in the shared eden.
// jmp(slowCase);
// }
//
// if (!compilation.target.arch.is64bit()) {
// getThread(threadReg);
// }
//
// movptr(top, new Address(threadReg, compilation.runtime.threadTlabTopOffset()));
// movptr(t1, new Address(threadReg, compilation.runtime.threadTlabEndOffset()));
//
// // calculate amount of free space
// subptr(t1, top);
// shrptr(t1, Util.log2(wordSize));
//
// // Retain tlab and allocate object in shared space if
// // the amount free in the tlab is too large to discard.
// cmpptr(t1, new Address(threadReg, inBytes(JavaThread.tlabRefillWasteLimitOffset())));
// jcc(X86Assembler.Condition.lessEqual, discardTlab);
//
// // Retain
// // %%% yuck as movptr...
// movptr(t2, (int) ThreadLocalAllocBuffer.refillWasteLimitIncrement());
// addptr(new Address(threadReg, inBytes(JavaThread.tlabRefillWasteLimitOffset())), t2);
// if (C1XOptions.TLABStats) {
// // increment number of slowAllocations
// addl(new Address(threadReg, inBytes(JavaThread.tlabSlowAllocationsOffset())), 1);
// }
// jmp(tryEden);
//
// bind(discardTlab);
// if (C1XOptions.TLABStats) {
// // increment number of refills
// addl(new Address(threadReg, inBytes(JavaThread.tlabNumberOfRefillsOffset())), 1);
// // accumulate wastage -- t1 is amount free in tlab
// addl(new Address(threadReg, inBytes(JavaThread.tlabFastRefillWasteOffset())), t1);
// }
//
// // if tlab is currently allocated (top or end != null) then
// // fill [top : end + alignmentReserve with array object
// testptr(top, top);
// jcc(X86Assembler.Condition.zero, doRefill);
//
// // set up the mark word
// movptr(new Address(top, oopDesc.markOffsetInBytes()), (intptrT) markOopDesc.prototype().copySetHash(0x2));
// // set the length to the remaining space
// subptr(t1, typeArrayOopDesc.headerSize(BasicType.Int));
// addptr(t1, (int) ThreadLocalAllocBuffer.alignmentReserve());
// shlptr(t1, log2Intptr(wordSize / Util.sizeofInt()));
// movptr(new Address(top, compilation.runtime.arrayLengthOffsetInBytes()), t1);
// // set klass to intArrayKlass
// // dubious reloc why not an oop reloc?
// movptr(t1, new ExternalAddress((Address) Universe.intArrayKlassObjAddr()));
// // store klass last. concurrent gcs assumes klass length is valid if
// // klass field is not null.
// storeKlass(top, t1);
//
// // refill the tlab with an eden allocation
// bind(doRefill);
// movptr(t1, new Address(threadReg, compilation.runtime.threadTlabSizeOffset()));
// shlptr(t1, Util.log2(wordSize));
// // add objectSize ??
// edenAllocate(top, t1, 0, t2, slowCase);
//
// // Check that t1 was preserved in edenAllocate.
// boolean assertEnabled = false;
// assert assertEnabled = true;
// if (assertEnabled && C1XOptions.UseTLAB) {
// Label ok = new Label();
// Register tsize = X86Register.rsi;
// assert Register.assertDifferentRegisters(tsize, threadReg, t1);
// push(tsize);
// movptr(tsize, new Address(threadReg, compilation.runtime.threadTlabSizeOffset()));
// shlptr(tsize, Util.log2(wordSize));
// cmpptr(t1, tsize);
// jcc(X86Assembler.Condition.equal, ok);
// stop("assert(t1 != tlab size)");
// Util.shouldNotReachHere();
//
// bind(ok);
// pop(tsize);
// }
// movptr(new Address(threadReg, compilation.runtime.threadTlabStartOffset()), top);
// movptr(new Address(threadReg, compilation.runtime.threadTlabTopOffset()), top);
// addptr(top, t1);
// subptr(top, (int) ThreadLocalAllocBuffer.alignmentReserveInBytes();
// movptr(new Address(threadReg, compilation.runtime.threadTlabEndOffset()), top);
// verifyTlab();
// jmp(retry);
    }

    static double pi4 = 0.7853981633974483;

    void trigfunc(char trig, int numFpuRegsInUse) {
        // A hand-coded argument reduction for values in fabs(pi/4, pi/2)
        // was attempted in this code; unfortunately it appears that the
        // switch to 80-bit precision and back causes this to be
        // unprofitable compared with simply performing a runtime call if
        // the argument is out of the (-pi/4, pi/4) range.

        Register tmp = Register.noreg;
        if (!compilation.target.supportsCmov()) {
            // fcmp needs a temporary so preserve X86Register.rbx :
            tmp = X86Register.rbx;
            push(tmp);
        }

        Label slowCase = new Label();
        Label done = new Label();

        ExternalAddress pi4Adr = null; // TODO: Replace this with address of pi (Address)&pi4;
        if (reachable(pi4Adr)) {
            // x ?<= pi/4
            fldD(pi4Adr);
            fldS(1); // Stack: X PI/4 X
            fabs(); // Stack: |X| PI/4 X
            fcmp(tmp);
            jcc(X86Assembler.Condition.above, slowCase);

            // fastest case: -pi/4 <= x <= pi/4
            switch (trig) {
                case 's':
                    fsin();
                    break;
                case 'c':
                    fcos();
                    break;
                case 't':
                    ftan();
                    break;
                default:
                    Util.shouldNotReachHere();
                    break;
            }
            jmp(done);
        }

        // slow case: runtime call
        bind(slowCase);
        // Preserve registers across runtime call
        pusha();
        int incomingArgumentAndReturnValueOffset = -1;
        if (numFpuRegsInUse > 1) {
            // Must preserve all other FPU regs (could alternatively convert
            // SharedRuntime.dsin and dcos into assembly routines known not to trash
            // FPU state, but can not trust C compiler)
            Util.needsCleanUp();
            // NOTE that in this case we also push the incoming argument to
            // the stack and restore it later; we also use this stack slot to
            // hold the return value from dsin or dcos.
            for (int i = 0; i < numFpuRegsInUse; i++) {
                subptr(X86Register.rsp, Util.sizeofJdouble());
                fstpD(new Address(X86Register.rsp, 0));
            }
            incomingArgumentAndReturnValueOffset = Util.sizeofJdouble() * (numFpuRegsInUse - 1);
            fldD(new Address(X86Register.rsp, incomingArgumentAndReturnValueOffset));
        }
        subptr(X86Register.rsp, Util.sizeofJdouble());
        fstpD(new Address(X86Register.rsp, 0));
        if (compilation.target.arch.is64bit()) {
            movdbl(X86Register.xmm0, new Address(X86Register.rsp, 0));
        }

        // NOTE: we must not use callVMLeaf here because that requires a
        // complete interpreter frame in debug mode -- same bug as 4387334
        // callVMLeafBase is perfectly safe and will
        // do proper 64bit abi

        Util.needsCleanUp();
        // Need to add stack banging before this runtime call if it needs to
        // be taken; however : there is no generic stack banging routine at
        // the MacroAssembler level
        switch (trig) {
            case 's':
                callVMLeafBase(CiRuntimeCall.dsin, 0);
                break;
            case 'c':
                callVMLeafBase(CiRuntimeCall.dcos, 0);
                break;
            case 't':
                callVMLeafBase(CiRuntimeCall.dtan, 0);
                break;
            default:
                Util.shouldNotReachHere();
                break;
        }
        if (compilation.target.arch.is64bit()) {
            movsd(new Address(X86Register.rsp, 0), X86Register.xmm0);
            fldD(new Address(X86Register.rsp, 0));
        }
        addptr(X86Register.rsp, Util.sizeofJdouble());
        if (numFpuRegsInUse > 1) {
            // Must save return value to stack and then restore entire FPU stack
            fstpD(new Address(X86Register.rsp, incomingArgumentAndReturnValueOffset));
            for (int i = 0; i < numFpuRegsInUse; i++) {
                fldD(new Address(X86Register.rsp, 0));
                addptr(X86Register.rsp, Util.sizeofJdouble());
            }
        }
        popa();

        // Come here with result in F-TOS
        bind(done);

        if (tmp != Register.noreg) {
            pop(tmp);
        }
    }

    // Look up the method for a megamorphic invokeinterface call.
    // The target method is determined by <intfKlass : itableIndex>.
    // The receiver klass is in recvKlass.
    // On success : the result will be in methodResult : and execution falls through.
    // On failure : execution transfers to the given label.
    void lookupInterfaceMethod(Register recvKlass, Register intfKlass, RegisterOrConstant itableIndex, Register methodResult, Register scanTemp, Label lNoSuchInterface) {
        assert Register.assertDifferentRegisters(recvKlass, intfKlass, methodResult, scanTemp);
        assert itableIndex.isConstant() || itableIndex.asRegister() == methodResult : "caller must use same register for non-constant itable index as for method";

        // Compute start of first itableOffsetEntry (which is at the end of the vtable)
        int vtableBase = compilation.runtime.vtableStartOffset() * wordSize;
        int itentryOff = compilation.runtime.itableMethodEntryMethodOffset();
        int scanStep = compilation.runtime.itableOffsetEntrySize() * wordSize;
        int vteSize = compilation.runtime.vtableEntrySize() * wordSize;
        Address.ScaleFactor timesVteScale = Address.ScaleFactor.timesPtr(compilation.target.arch);
        assert vteSize == wordSize : "else adjust timesVteScale";

        movl(scanTemp, new Address(recvKlass, compilation.runtime.vtableLengthOffset() * wordSize));

        // %%% Could store the aligned : prescaled offset in the klassoop.
        lea(scanTemp, new Address(recvKlass, scanTemp, timesVteScale, vtableBase));
        if (Util.heapWordsPerLong() > 1) {
            // Round up to alignObjectOffset boundary
            // see code for instanceKlass.startOfItable!
            roundTo(scanTemp, Util.bytesPerLong());
        }

        // Adjust recvKlass by scaled itableIndex : so we can free itableIndex.
        assert compilation.runtime.itableOffsetEntrySize() * wordSize == wordSize : "adjust the scaling in the code below";
        lea(recvKlass, new Address(recvKlass, itableIndex, Address.ScaleFactor.timesPtr(compilation.target.arch), itentryOff));

        // for (scan = klass.itable(); scan.interface() != null; scan += scanStep) {
        // if (scan.interface() == intf) {
        // result = (klass + scan.offset() + itableIndex);
        // }
        // }
        Label search = new Label();
        Label foundMethod = new Label();

        for (int peel = 1; peel >= 0; peel--) {
            movptr(methodResult, new Address(scanTemp, compilation.runtime.itableInterfaceOffsetInBytes()));
            cmpptr(intfKlass, methodResult);

            if (peel != 0) {
                jccb(X86Assembler.Condition.equal, foundMethod);
            } else {
                jccb(X86Assembler.Condition.notEqual, search);
                // (invert the test to fall through to foundMethod...)
            }

            if (peel == 0) {
                break;
            }

            bind(search);

            // Check that the previous entry is non-null. A null entry means that
            // the receiver class doesn't implement the interface : and wasn't the
            // same as when the caller was compiled.
            testptr(methodResult, methodResult);
            jcc(X86Assembler.Condition.zero, lNoSuchInterface);
            addptr(scanTemp, scanStep);
        }

        bind(foundMethod);

        // Got a hit.
        movl(scanTemp, new Address(scanTemp, compilation.runtime.itableOffsetOffsetInBytes()));
        movptr(methodResult, new Address(recvKlass, scanTemp, Address.ScaleFactor.times1));
    }

    void checkKlassSubtype(Register subKlass, Register superKlass, Register tempReg, Label lSuccess) {
        // TODO: Also use the fast path!
        //Label lFailure = new Label();
        //checkKlassSubtypeFastPath(subKlass, superKlass, tempReg, lSuccess, lFailure, null, new RegisterOrConstant(-1));
        checkKlassSubtypeSlowPath(subKlass, superKlass, tempReg, Register.noreg, lSuccess, null, false);
        //bind(lFailure);
    }

    void checkKlassSubtypeFastPath(Register subKlass, Register superKlass, Register tempReg, Label lSuccess, Label lFailure, Label lSlowPath, RegisterOrConstant superCheckOffset) {
        // TODO: Model the fast path!
        if (true) {
            throw Util.unimplemented();
        }

        assert Register.assertDifferentRegisters(subKlass, superKlass, tempReg);
        boolean mustLoadSco = (superCheckOffset.constantOrZero() == -1);
        if (superCheckOffset.isRegister()) {
            assert Register.assertDifferentRegisters(subKlass, superKlass, superCheckOffset.asRegister());
        } else if (mustLoadSco) {
            assert tempReg != Register.noreg : "supply either a temp or a register offset";
        }

        Label lFallthrough = new Label();
        int labelNulls = 0;
        if (lSuccess == null) {
            lSuccess = lFallthrough;
            labelNulls++;
        }
        if (lFailure == null) {
            lFailure = lFallthrough;
            labelNulls++;
        }
        if (lSlowPath == null) {
            lSlowPath = lFallthrough;
            labelNulls++;
        }
        assert labelNulls <= 1 : "at most one null in the batch";

        int scOffset = (compilation.runtime.headerSize() * wordSize + compilation.runtime.secondarySuperCacheOffsetInBytes());
        int scoOffset = (compilation.runtime.headerSize() * wordSize + compilation.runtime.superCheckOffsetOffsetInBytes());
        Address superCheckOffsetAddr = new Address(superKlass, scoOffset);

        // If the pointers are equal : we are done (e.g., String[] elements).
        // This self-check enables sharing of secondary supertype arrays among
        // non-primary types such as array-of-interface. Otherwise : each such
        // type would need its own customized SSA.
        // We move this check to the front of the fast path because many
        // type checks are in fact trivially successful in this manner :
        // so we get a nicely predicted branch right at the start of the check.
        cmpptr(subKlass, superKlass);

        if (lSuccess == lFallthrough) {
            jccb(Condition.equal, lSuccess);
        } else {
            jcc(Condition.equal, lSuccess);
        }

        // Check the supertype display:
        if (mustLoadSco) {
            // Positive movl does right thing on LP64.
            movl(tempReg, superCheckOffsetAddr);
            superCheckOffset = new RegisterOrConstant(tempReg);
        }
        Address superCheckAddr = new Address(subKlass, superCheckOffset, Address.ScaleFactor.times1, 0);
        cmpptr(superKlass, superCheckAddr); // load displayed supertype

        // This check has worked decisively for primary supers.
        // Secondary supers are sought in the superCache ('superCacheAddr').
        // (Secondary supers are interfaces and very deeply nested subtypes.)
        // This works in the same check above because of a tricky aliasing
        // between the superCache and the primary super display elements.
        // (The 'superCheckAddr' can Address either, as the case requires.)
        // Note that the cache is updated below if it does not help us find
        // what we need immediately.
        // So if it was a primary super : we can just fail immediately.
        // Otherwise : it's the slow path for us (no success at this point).

        if (superCheckOffset.isRegister()) {

            // local jcc
            if (lSuccess.equals(lFallthrough)) {
                jccb(X86Assembler.Condition.equal, lSuccess);
            } else {
                jcc(X86Assembler.Condition.equal, lSuccess);
            }

            cmpl(superCheckOffset.asRegister(), scOffset);
            if (lFailure == lFallthrough) {

                // local jcc
                if (lSlowPath.equals(lFallthrough)) {
                    jccb(X86Assembler.Condition.equal, lSlowPath);
                } else {
                    jcc(X86Assembler.Condition.equal, lSlowPath);
                }
            } else {
                // local jcc
                if (lFailure.equals(lFallthrough)) {
                    jccb(X86Assembler.Condition.notEqual, lFailure);
                } else {
                    jcc(X86Assembler.Condition.notEqual, lFailure);
                }
                if (!lSlowPath.equals(lFallthrough)) {
                    jmp(lSlowPath);
                }
            }
        } else if (superCheckOffset.asConstant() == scOffset) {
            // Need a slow path; fast failure is impossible.
            if (lSlowPath.equals(lFallthrough)) {

                // local jcc
                if (lSuccess.equals(lFallthrough)) {
                    jccb(X86Assembler.Condition.equal, lSuccess);
                } else {
                    jcc(X86Assembler.Condition.equal, lSuccess);
                }
            } else {
                // local jcc
                if (lSlowPath.equals(lFallthrough)) {
                    jccb(X86Assembler.Condition.notEqual, lSlowPath);
                } else {
                    jcc(X86Assembler.Condition.notEqual, lSlowPath);
                }
                if (!lSuccess.equals(lFallthrough)) {
                    jmp(lSuccess);
                }
            }
        } else {
            // No slow path; it's a fast decision.
            if (lFailure.equals(lFallthrough)) {

                // local jcc
                if (lSuccess.equals(lFallthrough)) {
                    jccb(X86Assembler.Condition.equal, lSuccess);
                } else {
                    jcc(X86Assembler.Condition.equal, lSuccess);
                }

            } else {
                // local jcc
                if (lFailure.equals(lFallthrough)) {
                    jccb(X86Assembler.Condition.equal, lFailure);
                } else {
                    jcc(X86Assembler.Condition.notEqual, lFailure);
                }
                if (!lSuccess.equals(lFallthrough)) {
                    jmp(lSuccess);
                }
            }
        }

        bind(lFallthrough);
    }

    void checkKlassSubtypeSlowPath(Register subKlass, Register superKlass, Register tempReg, Register temp2Reg, Label lSuccess, Label lFailure, boolean setCondCodes) {
        assert Register.assertDifferentRegisters(subKlass, superKlass, tempReg);
        if (temp2Reg != Register.noreg) {
            assert Register.assertDifferentRegisters(subKlass, superKlass, tempReg, temp2Reg);
        }

        Label lFallthrough = new Label();
        int labelNulls = 0;
        if (lSuccess == null) {
            lSuccess = lFallthrough;
            labelNulls++;
        }
        if (lFailure == null) {
            lFailure = lFallthrough;
            labelNulls++;
        }
        assert labelNulls <= 1 : "at most one null in the batch";

        // a couple of useful fields in subKlass:
        int ssOffset = (compilation.runtime.headerSize() * wordSize + compilation.runtime.secondarySupersOffsetInBytes());
        int scOffset = (compilation.runtime.headerSize() * wordSize + compilation.runtime.secondarySuperCacheOffsetInBytes());
        Address secondarySupersAddr = new Address(subKlass, ssOffset);
        Address superCacheAddr = new Address(subKlass, scOffset);

        // Do a linear scan of the secondary super-klass chain.
        // This code is rarely used : so simplicity is a virtue here.
        // The repneScan instruction uses fixed registers : which we must spill.
        // Don't worry too much about pre-existing connections with the input regs.

        assert subKlass != X86Register.rax : "killed reg"; // killed by mov(X86Register.rax, super)
        assert subKlass != X86Register.rcx : "killed reg"; // killed by lea(X86Register.rcx, &pstCounter)

        // Get superKlass value into X86Register.rax (even if it was in X86Register.rdi or X86Register.rcx).
        boolean pushedRax = false;
        boolean pushedRcx = false;
        boolean pushedRdi = false;
        if (superKlass != X86Register.rax) {
            if (X86Register.rax != tempReg && X86Register.rax != temp2Reg) {
                push(X86Register.rax);
                pushedRax = true;
            }
            mov(X86Register.rax, superKlass);
        }
        if (X86Register.rcx != tempReg && X86Register.rcx != temp2Reg) {
            push(X86Register.rcx);
            pushedRcx = true;
        }
        if (X86Register.rdi != tempReg && X86Register.rdi != temp2Reg) {
            push(X86Register.rdi);
            pushedRdi = true;
        }

        // TODO: Check what to do with this!
// #ifndef PRODUCT
// int pstCounter = &SharedRuntime.partialSubtypeCtr;
// ExternalAddress pstCounterAddr((Address) pstCounter);
// NOTLP64( incrementl(pstCounterAddr) );
// LP64ONLY( lea(X86Register.rcx, pstCounterAddr) );
// LP64ONLY( incrementl(new Address(X86Register.rcx, 0)) );
// #endif //PRODUCT

        // We will consult the secondary-super array.
        movptr(X86Register.rdi, secondarySupersAddr);
        // Load the array length. (Positive movl does right thing on LP64.)
        movl(X86Register.rcx, new Address(X86Register.rdi, compilation.runtime.arrayLengthOffsetInBytes()));
        // Skip to start of data.
        addptr(X86Register.rdi, compilation.runtime.arrayBaseOffsetInBytes(BasicType.Object));

        // Scan RCX words at [RDI] for an occurrence of RAX.
        // Set NZ/Z based on last compare.
        // This part is tricky : as values in supers array could be 32 or 64 bit wide
        // and we store values in objArrays always encoded : thus we need to encode
        // the value of X86Register.rax before repne. Note that X86Register.rax is dead after the repne.

        repneScan();

        // Unspill the temp. registers:
        if (pushedRdi) {
            pop(X86Register.rdi);
        }
        if (pushedRcx) {
            pop(X86Register.rcx);
        }
        if (pushedRax) {
            pop(X86Register.rax);
        }

        if (setCondCodes) {
            // Special hack for the AD files: X86Register.rdi is guaranteed non-zero.
            assert !pushedRdi : "X86Register.rdi must be left non-null";
            // Also : the condition codes are properly set Z/NZ on succeed/failure.
        }

        if (lFailure == lFallthrough) {
            jccb(X86Assembler.Condition.notEqual, lFailure);
        } else {
            jcc(X86Assembler.Condition.notEqual, lFailure);
        }

        // Success. Cache the super we found and proceed in triumph.
        movptr(superCacheAddr, superKlass);

        if (lSuccess.equals(lFallthrough)) {
            jmp(lSuccess);
        }

        bind(lFallthrough);
    }

    void ucomisd(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        ucomisd(dst, asAddress(src));
    }

    void ucomiss(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        ucomiss(dst, asAddress(src));
    }

    void xorpd(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        if (reachable(src)) {
            xorpd(dst, asAddress(src));
        } else {
            lea(rscratch1, src);
            xorpd(dst, new Address(rscratch1, 0));
        }
    }

    void xorps(Register dst, AddressLiteral src) {
        assert dst.isXMM();
        if (reachable(src)) {
            xorps(dst, asAddress(src));
        } else {
            lea(rscratch1, src);
            xorps(dst, new Address(rscratch1, 0));
        }
    }

    boolean verifyOop(Register reg) {
        return verifyOop(reg, "");

    }

    boolean verifyOop(Register reg, String message) {
        if (!C1XOptions.VerifyOops) {
            return true;
        }

        // Pass register number to verifyOopSubroutine
        String b = String.format("verifyOop: %s: %s", reg.toString(), message);
        push(X86Register.rax); // save X86Register.rax :
        push(reg); // pass register argument
        ExternalAddress buffer = new ExternalAddress(Util.stringToAddress(b));
        // avoid using pushptr : as it modifies scratch registers
        // and our contract is not to modify anything
        movptr(X86Register.rax, buffer.addr());
        push(X86Register.rax);
        // call indirectly to solve generation ordering problem
        movptr(X86Register.rax, new RuntimeAddress(CiRuntimeCall.VerifyOopSubroutine));
        call(X86Register.rax);
        return true;
    }

    // registers on entry:
    // - X86Register.rax ('check' register): required MethodType
    // - X86Register.rcx: method handle
    // - X86Register.rdx : X86Register.rsi : or ?: killable temp
    void checkMethodHandleType(Register mtypeReg, Register mhReg, Register tempReg, Label wrongMethodType) {
        // TODO: What to do with method handles?
        throw Util.unimplemented();
// // compare method type against that of the receiver
// cmpptr(mtypeReg, new Address(mhReg, delayedValue(javaDynMethodHandle.typeOffsetInBytes, tempReg)));
// jcc(X86Assembler.Condition.notEqual, wrongMethodType);
    }

    // A method handle has a "vmslots" field which gives the size of its
    // argument list in JVM stack slots. This field is either located directly
    // in every method handle : or else is indirectly accessed through the
    // method handle's MethodType. This macro hides the distinction.
    void loadMethodHandleVmslots(Register vmslotsReg, Register mhReg, Register tempReg) {
        // TODO: What to do with method handles?
        throw Util.unimplemented();
// // load mh.type.form.vmslots
// if (javaDynMethodHandle.vmslotsOffsetInBytes() != 0) {
// // hoist vmslots into every mh to avoid dependent load chain
// movl(vmslotsReg, new Address(mhReg, delayedValue(javaDynMethodHandle.vmslotsOffsetInBytes, tempReg)));
// } else {
// Register temp2Reg = vmslotsReg;
// movptr(temp2Reg, new Address(mhReg, delayedValue(javaDynMethodHandle.typeOffsetInBytes, tempReg)));
// movptr(temp2Reg, new Address(temp2Reg, delayedValue(javaDynMethodType.formOffsetInBytes, tempReg)));
// movl(vmslotsReg, new Address(temp2Reg, delayedValue(javaDynMethodTypeForm.vmslotsOffsetInBytes, tempReg)));
// }
    }

    // registers on entry:
    // - X86Register.rcx: method handle
    // - X86Register.rdx: killable temp (interpreted only)
    // - X86Register.rax: killable temp (compiled only)
    void jumpToMethodHandleEntry(Register mhReg, Register tempReg) {
        // TODO: What to do with method handles?
        throw Util.unimplemented();
// assert mhReg == X86Register.rcx : "caller must put MH object in X86Register.rcx";
// assert Register.assertDifferentRegisters(mhReg, tempReg);
//
// // pick out the interpreted side of the handler
// movptr(tempReg, new Address(mhReg, delayedValue(javaDynMethodHandle.vmentryOffsetInBytes, tempReg)));
//
// // off we go...
// jmp(new Address(tempReg, MethodHandleEntry.fromInterpretedEntryOffsetInBytes()));
//
// // for the various stubs which take control at this point :
// // see MethodHandles.generateMethodHandleStub
    }

    void verifyOopAddr(Address addr, String s) {
        if (!C1XOptions.VerifyOops) {
            return;
        }

        // Address adjust(addr.base(), addr.index(), addr.scale(), addr.disp() + BytesPerWord);
        // Pass register number to verifyOopSubroutine
        String b = String.format("verifyOopAddr: %s", s);

        push(X86Register.rax); // save X86Register.rax :
        // addr may contain X86Register.rsp so we will have to adjust it based on the push
        // we just did
        // NOTE: 64bit seemed to have had a bug in that it did movq(addr, X86Register.rax); which
        // stores X86Register.rax into addr which is backwards of what was intended.
        if (addr.uses(X86Register.rsp)) {
            lea(X86Register.rax, addr);
            pushptr(new Address(X86Register.rax, wordSize));
        } else {
            pushptr(addr);
        }

        ExternalAddress buffer = new ExternalAddress(Util.stringToAddress(b));
        // pass msg argument
        // avoid using pushptr : as it modifies scratch registers
        // and our contract is not to modify anything
        movptr(X86Register.rax, buffer.addr());
        push(X86Register.rax);

        // call indirectly to solve generation ordering problem
        movptr(X86Register.rax, new RuntimeAddress(CiRuntimeCall.VerifyOopSubroutine));
        call(X86Register.rax);
        // Caller pops the arguments and restores X86Register.rax : from the stack
    }

    boolean verifyTlab() {
        if (C1XOptions.UseTLAB && C1XOptions.VerifyOops) {
            Label next = new Label();
            Label ok = new Label();
            Register t1 = X86Register.rsi;
            Register threadReg = (compilation.target.arch.is64bit()) ? X86FrameMap.r15thread : X86Register.rbx;

            push(t1);

            if (!compilation.target.arch.is64bit()) {
                push(threadReg);
                getThread(threadReg);
            }

            movptr(t1, new Address(threadReg, compilation.runtime.threadTlabTopOffset()));
            cmpptr(t1, new Address(threadReg, compilation.runtime.threadTlabStartOffset()));
            jcc(X86Assembler.Condition.aboveEqual, next);
            stop("assert(top >= start)");
            Util.shouldNotReachHere();

            bind(next);
            movptr(t1, new Address(threadReg, compilation.runtime.threadTlabEndOffset()));
            cmpptr(t1, new Address(threadReg, compilation.runtime.threadTlabTopOffset()));
            jcc(X86Assembler.Condition.aboveEqual, ok);
            stop("assert(top <= end)");
            Util.shouldNotReachHere();

            bind(ok);
            if (!compilation.target.arch.is64bit()) {
                pop(threadReg);
            }
            pop(t1);
        }
        return true;
    }

    int rspOffset() {
        return rspOffset;
    }

    void setRspOffset(int n) {
        rspOffset = n;
    }

    // Note: NEVER push values directly, but only through following pushXxx functions;
    // This helps us to track the X86Register.rsp changes compared to the entry X86Register.rsp (.rspOffset)

    void pushJint(int i) {
        rspOffset++;
        push(i);
    }

    void pushOop(Object o) {
        rspOffset++;
        pushoop(o);
    }

    // Seems to always be in wordSize
    void pushAddr(Address a) {
        rspOffset++;
        pushptr(a);
    }

    void pushReg(Register r) {
        rspOffset++;
        push(r);
    }

    void popReg(Register r) {
        rspOffset--;
        pop(r);
        assert rspOffset >= 0 : "stack offset underflow";
    }

    void decStack(int nofWords) {
        rspOffset -= nofWords;
        assert rspOffset >= 0 : "stack offset underflow";
        addptr(X86Register.rsp, wordSize * nofWords);
    }

    void decStackAfterCall(int nofWords) {
        rspOffset -= nofWords;
        assert rspOffset >= 0 : "stack offset underflow";
    }

    public void int3() {

        if (compilation.target.isSolaris()) {
            push(X86Register.rax);
            push(X86Register.rdx);
            push(X86Register.rcx);
            call(new RuntimeAddress(CiRuntimeCall.Breakpoint));
            pop(X86Register.rcx);
            pop(X86Register.rdx);
            pop(X86Register.rax);

        } else {
            Util.shouldNotReachHere();
        }
    }

    void jmp(Label label) {
        jmp(label, Relocation.none);
    }

    // Support optimal SSE move instructions.
    void movflt(Register dst, Register src) {
        assert dst.isXMM() && src.isXMM();
        if (C1XOptions.UseXmmRegToRegMoveAll) {
            movaps(dst, src);
            return;
        } else {
            movss(dst, src);
            return;
        }
    }

    void movflt(Register dst, Address src) {
        assert dst.isXMM();
        movss(dst, src);
    }

    void movflt(Address dst, Register src) {
        assert src.isXMM();
        movss(dst, src);
    }

    void movdbl(Register dst, Register src) {
        assert dst.isXMM() && src.isXMM();
        if (C1XOptions.UseXmmRegToRegMoveAll) {
            movapd(dst, src);
            return;
        } else {
            movsd(dst, src);
            return;
        }
    }

    void movdbl(Register dst, Address src) {
        assert dst.isXMM();
        if (C1XOptions.UseXmmLoadAndClearUpper) {
            movsd(dst, src);
            return;
        } else {
            movlpd(dst, src);
            return;
        }
    }

    void movdbl(Address dst, Register src) {
        assert src.isXMM();
        movsd(dst, src);
    }

    void addptr(Address dst, int src) {
        if (compilation.target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    void addptr(Register dst, Address src) {
        if (compilation.target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    void andptr(Register src1, Register src2) {
        if (compilation.target.arch.is64bit()) {
            andq(src1, src2);
        } else {
            andl(src1, src2);
        }
    }

    int lockObject(Register hdr, Register obj, Register dispHdr, Register scratch, Label slowCase) {
        int alignedMask = wordSize - 1;
        int hdrOffset = compilation.runtime.markOffsetInBytes();
        assert hdr == X86Register.rax : "hdr must be X86Register.rax :  for the cmpxchg instruction";
        assert hdr != obj && hdr != dispHdr && obj != dispHdr : "registers must be different";
        Label done = new Label();
        int nullCheckOffset = -1;

        verifyOop(obj);

        // save object being locked into the BasicObjectLock
        movptr(new Address(dispHdr, compilation.runtime.basicObjectLockOffsetInBytes()), obj);

        if (C1XOptions.UseBiasedLocking) {
            assert scratch != Register.noreg : "should have scratch register at this point";
            nullCheckOffset = biasedLockingEnter(dispHdr, obj, hdr, scratch, false, done, slowCase, null);
        } else {
            nullCheckOffset = offset();
        }

        // Load object header
        movptr(hdr, new Address(obj, hdrOffset));
        // and mark it as unlocked
        orptr(hdr, compilation.runtime.unlockedValue());
        // save unlocked object header into the displaced header location on the stack
        movptr(new Address(dispHdr, 0), hdr);
        // test if object header is still the same (i.e. unlocked), and if so, store the
        // displaced header Pointer in the object header - if it is not the same, get the
        // object header instead
        if (compilation.runtime.isMP()) {
            lock(); // must be immediately before cmpxchg!
        }
        cmpxchgptr(dispHdr, new Address(obj, hdrOffset));
        // if the object header was the same, we're done
        if (C1XOptions.PrintBiasedLockingStatistics) {
            condInc32(X86Assembler.Condition.equal, new ExternalAddress(compilation.runtime.biasedLockingFastPathEntryCountAddr()));
        }
        jcc(X86Assembler.Condition.equal, done);
        // if the object header was not the same, it is now in the hdr register
        // => test if it is a stack pointer into the same stack (recursive locking), i.e.:
        //
        // 1) (hdr & alignedMask) == 0
        // 2) X86Register.rsp <= hdr
        // 3) hdr <= X86Register.rsp + pageSize
        //
        // these 3 tests can be done by evaluating the following expression:
        //
        // (hdr - X86Register.rsp) & (alignedMask - pageSize)
        //
        // assuming both the stack pointer and pageSize have their least
        // significant 2 bits cleared and pageSize is a power of 2
        subptr(hdr, X86Register.rsp);
        andptr(hdr, alignedMask - compilation.runtime.vmPageSize());
        // for recursive locking, the result is zero => save it in the displaced header
        // location (null in the displaced hdr location indicates recursive locking)
        movptr(new Address(dispHdr, 0), hdr);
        // otherwise we don't care about the result and handle locking via runtime call
        jcc(X86Assembler.Condition.notZero, slowCase);
        // done
        bind(done);
        return nullCheckOffset;
    }

    public void unlockObject(Register hdr, Register obj, Register dispHdr, Label slowCase) {
        int hdrOffset = compilation.runtime.markOffsetInBytes();
        assert dispHdr == X86Register.rax : "dispHdr must be X86Register.rax :  for the cmpxchg instruction";
        assert hdr != obj && hdr != dispHdr && obj != dispHdr : "registers must be different";
        Label done = new Label();

        if (C1XOptions.UseBiasedLocking) {
            // load object
            movptr(obj, new Address(dispHdr, compilation.runtime.basicObjectObjOffsetInBytes()));
            biasedLockingExit(obj, hdr, done);
        }

        // load displaced header
        movptr(hdr, new Address(dispHdr, 0));
        // if the loaded hdr is null we had recursive locking
        testptr(hdr, hdr);
        // if we had recursive locking, we are done
        jcc(X86Assembler.Condition.zero, done);
        if (!C1XOptions.UseBiasedLocking) {
            // load object
            movptr(obj, new Address(dispHdr, compilation.runtime.basicObjectObjOffsetInBytes()));
        }
        verifyOop(obj);
        // test if object header is pointing to the displaced header, and if so, restore
        // the displaced header in the object - if the object header is not pointing to
        // the displaced header, get the object header instead
        if (compilation.runtime.isMP()) {
            lock(); // must be immediately before cmpxchg!
        }
        cmpxchgptr(hdr, new Address(obj, hdrOffset));
        // if the object header was not pointing to the displaced header,
        // we do unlocking via runtime call
        jcc(X86Assembler.Condition.notEqual, slowCase);
        // done
        bind(done);
    }

    void invalidateRegisters(boolean invRax, boolean invRbx, boolean invRcx, boolean invRdx, boolean invRsi, boolean invRdi) {

        if (C1XOptions.GenerateAssertionCode) {
            if (invRax) {
                movptr(X86Register.rax, 0xDEAD);
            }
            if (invRbx) {
                movptr(X86Register.rbx, 0xDEAD);
            }
            if (invRcx) {
                movptr(X86Register.rcx, 0xDEAD);
            }
            if (invRdx) {
                movptr(X86Register.rdx, 0xDEAD);
            }
            if (invRsi) {
                movptr(X86Register.rsi, 0xDEAD);
            }
            if (invRdi) {
                movptr(X86Register.rdi, 0xDEAD);
            }
        }
    }

    void verifyStackOop(int stackOffset) {
        if (!C1XOptions.VerifyOops) {
            return;
        }
        verifyOopAddr(new Address(X86Register.rsp, stackOffset), "verfyStackOop");
    }

    void verifyNotNullOop(Register r) {
        if (!C1XOptions.VerifyOops) {
            return;
        }
        Label notNull = new Label();
        testptr(r, r);
        jcc(X86Assembler.Condition.notZero, notNull);
        stop("non-null oop required");
        bind(notNull);
        verifyOop(r);
    }

    void xchgptr(Register src1, Register src2) {
        if (compilation.target.arch.is64bit()) {
            xchgq(src1, src2);
        } else {
            xchgl(src1, src2);
        }
    }

    void xchgptr(Register src1, Address src2) {
        if (compilation.target.arch.is64bit()) {
            xchgq(src1, src2);
        } else {
            xchgl(src1, src2);
        }
    }

    void allocateArray(Register obj, Register len, Register t1, Register t2, int headerSize, Address.ScaleFactor scaleFactor, Register klass, Label slowCase) {
        assert obj == X86Register.rax : "obj must be in X86Register.rax :  for cmpxchg";
        assert Register.assertDifferentRegisters(obj, len, t1, t2, klass);

        // determine alignment mask
        assert (wordSize & 1) == 0 : "must be a multiple of 2 for masking code to work";

        // check for negative or excessive length
        cmpptr(len, compilation.runtime.maxArrayAllocationLength());
        jcc(X86Assembler.Condition.above, slowCase);

        Register arrSize = t2; // okay to be the same
        // align object end
        movptr(arrSize, headerSize * wordSize + compilation.runtime.getMinObjAlignmentInBytesMask());
        lea(arrSize, new Address(arrSize, len, scaleFactor));
        andptr(arrSize, ~compilation.runtime.getMinObjAlignmentInBytesMask());

        tryAllocate(obj, arrSize, 0, t1, t2, slowCase);

        initializeHeader(obj, klass, len, t1, t2);

        // clear rest of allocated space
        Register lenZero = len;
        initializeBody(obj, arrSize, headerSize * wordSize, lenZero);

        if (compilation.runtime.dtraceAllocProbes()) {
            assert obj == X86Register.rax : "must be";
            call(new RuntimeAddress(CiRuntimeCall.DtraceObjectAlloc));
        }

        verifyOop(obj);
    }

    // Defines obj, preserves varSizeInBytes
    void tryAllocate(Register obj, Register varSizeInBytes, int conSizeInBytes, Register t1, Register t2, Label slowCase) {
        if (C1XOptions.UseTLAB) {
            tlabAllocate(obj, varSizeInBytes, conSizeInBytes, t1, t2, slowCase);
        } else {
            edenAllocate(obj, varSizeInBytes, conSizeInBytes, t1, slowCase);
        }
    }

    void initializeHeader(Register obj, Register klass, Register len, Register t1, Register t2) {
        assert Register.assertDifferentRegisters(obj, klass, len);
        if (C1XOptions.UseBiasedLocking && !len.isValid()) {
            assert Register.assertDifferentRegisters(obj, klass, len, t1, t2);
            movptr(t1, new Address(klass, compilation.runtime.prototypeHeaderOffsetInBytes() + compilation.runtime.klassPartOffsetInBytes()));
            movptr(new Address(obj, compilation.runtime.markOffsetInBytes()), t1);
        } else {
            // This assumes that all prototype bits fit in an int
            movptr(new Address(obj, compilation.runtime.markOffsetInBytes()), compilation.runtime.markOopDescPrototype());
        }

        movptr(new Address(obj, compilation.runtime.klassOffsetInBytes()), klass);
        if (len.isValid()) {
            movl(new Address(obj, compilation.runtime.arrayLengthOffsetInBytes()), len);
        }
    }

    // preserves obj, destroys lenInBytes
    void initializeBody(Register obj, Register lenInBytes, int hdrSizeInBytes, Register t1) {
        Label done = new Label();
        assert obj != lenInBytes && obj != t1 && t1 != lenInBytes : "registers must be different";
        assert (hdrSizeInBytes & (wordSize - 1)) == 0 : "header size is not a multiple of BytesPerWord";
        Register index = lenInBytes;
        // index is positive and ptr sized
        subptr(index, hdrSizeInBytes);
        jcc(X86Assembler.Condition.zero, done);
        // initialize topmost word, divide index by 2, check if odd and test if zero
        // note: for the remaining code to work, index must be a multiple of BytesPerWord

        if (C1XOptions.GenerateAssertionCode) {
            Label l = new Label();
            testptr(index, wordSize - 1);
            jcc(X86Assembler.Condition.zero, l);
            stop("index is not a multiple of BytesPerWord");
            bind(l);
        }
        xorptr(t1, t1); // use zero reg to clear memory (shorter code)
        if (C1XOptions.UseIncDec) {
            shrptr(index, 3); // divide by 8/16 and set carry flag if bit 2 was set
        } else {
            shrptr(index, 2); // use 2 instructions to avoid partial flag stall
            shrptr(index, 1);
        }

        if (compilation.target.arch.is32bit()) {
            // index could have been not a multiple of 8 (i.e., bit 2 was set)
            Label even = new Label();
            // note: if index was a multiple of 8, than it cannot
            // be 0 now otherwise it must have been 0 before
            // => if it is even, we don't need to check for 0 again
            jcc(X86Assembler.Condition.carryClear, even);
            // clear topmost word (no jump needed if conditional assignment would work here)
            movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - 0 * wordSize), t1);
            // index could be 0 now, need to check again
            jcc(X86Assembler.Condition.zero, done);
            bind(even);
        }

        // initialize remaining object fields: X86Register.rdx is a multiple of 2 now
        Label loop = new Label();
        bind(loop);
        movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - 1 * wordSize), t1);
        if (compilation.target.arch.is32bit()) {
            movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - 2 * wordSize), t1);
        }
        decrement(index, 1);
        jcc(X86Assembler.Condition.notZero, loop);

        // done
        bind(done);
    }

    void allocateObject(Register obj, Register t1, Register t2, int headerSize, int objectSize, Register klass, Label slowCase) {
        assert obj == X86Register.rax : "obj must be in X86Register.rax :  for cmpxchg";
        assert obj != t1 && obj != t2 && t1 != t2 : "registers must be different"; // XXX really?
        assert headerSize >= 0 && objectSize >= headerSize : "illegal sizes";

        tryAllocate(obj, Register.noreg, objectSize * wordSize, t1, t2, slowCase);

        initializeObject(obj, klass, Register.noreg, objectSize * wordSize, t1, t2);
    }

    void initializeObject(Register obj, Register klass, Register varSizeInBytes, int conSizeInBytes, Register t1, Register t2) {
        assert (conSizeInBytes & compilation.runtime.getMinObjAlignmentInBytesMask()) == 0 : "conSizeInBytes is not multiple of alignment";
        int hdrSizeInBytes = compilation.runtime.instanceOopDescBaseOffsetInBytes();

        initializeHeader(obj, klass, Register.noreg, t1, t2);

        // clear rest of allocated space
        Register t1Zero = t1;
        Register index = t2;
        int threshold = 6 * wordSize; // approximate break even point for code size (see comments below)
        if (varSizeInBytes != Register.noreg) {
            mov(index, varSizeInBytes);
            initializeBody(obj, index, hdrSizeInBytes, t1Zero);
        } else if (conSizeInBytes <= threshold) {
            // use explicit null stores
            // code size = 2 + 3*n bytes (n = number of fields to clear)
            xorptr(t1Zero, t1Zero); // use t1Zero reg to clear memory (shorter code)
            for (int i = hdrSizeInBytes; i < conSizeInBytes; i += wordSize) {
                movptr(new Address(obj, i), t1Zero);
            }
        } else if (conSizeInBytes > hdrSizeInBytes) {
            // use loop to null out the fields
            // code size = 16 bytes for even n (n = number of fields to clear)
            // initialize last object field first if odd number of fields
            xorptr(t1Zero, t1Zero); // use t1Zero reg to clear memory (shorter code)
            movptr(index, (conSizeInBytes - hdrSizeInBytes) >> 3);
            // initialize last object field if constant size is odd
            if (((conSizeInBytes - hdrSizeInBytes) & 4) != 0) {
                movptr(new Address(obj, conSizeInBytes - (1 * wordSize)), t1Zero);
            }
            // initialize remaining object fields: X86Register.rdx is a multiple of 2
            Label loop = new Label();
            bind(loop);
            movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - (1 * wordSize)), t1Zero);
            if (!compilation.target.arch.is64bit()) {
                movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - (2 * wordSize)), t1Zero);
            }
            decrement(index, 1);
            jcc(X86Assembler.Condition.notZero, loop);

        }

        if (compilation.runtime.dtraceAllocProbes()) {
            assert obj == X86Register.rax : "must be";
            call(new RuntimeAddress(CiRuntimeCall.DtraceObjectAlloc));
        }

        verifyOop(obj);
    }

    void cmov(Condition cc, Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            cmovq(cc, dst, src);
        } else {
            cmovl(cc, dst, src);
        }
    }

    void cmovptr(Condition cc, Register dst, Address src) {
        if (compilation.target.arch.is64bit()) {
            cmovq(cc, dst, src);
        } else {
            cmovl(cc, dst, src);
        }
    }

    void cmovptr(Condition cc, Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            cmovq(cc, dst, src);
        } else {
            cmovl(cc, dst, src);
        }
    }

    void orptr(Register dst, Address src) {
        if (compilation.target.arch.is64bit()) {
            orq(dst, src);
        } else {
            orl(dst, src);
        }
    }

    void orptr(Register dst, Register src) {
        if (compilation.target.arch.is64bit()) {
            orq(dst, src);
        } else {
            orl(dst, src);
        }
    }

    void orptr(Register dst, int src) {
        if (compilation.target.arch.is64bit()) {
            orq(dst, src);
        } else {
            orl(dst, src);
        }
    }

    void shlptr(Register dst) {
        if (compilation.target.arch.is64bit()) {
            shlq(dst);
        } else {
            shll(dst);
        }
    }

    void shrptr(Register dst) {
        if (compilation.target.arch.is64bit()) {
            shrq(dst);
        } else {
            shrl(dst);
        }
    }

    void sarptr(Register dst) {
        if (compilation.target.arch.is64bit()) {
            sarq(dst);
        } else {
            sarl(dst);
        }
    }

    void sarptr(Register dst, int src) {
        if (compilation.target.arch.is64bit()) {
            sarq(dst, src);
        } else {
            sarl(dst, src);
        }
    }

    void negptr(Register dst) {
        if (compilation.target.arch.is64bit()) {
            negq(dst);
        } else {
            negl(dst);
        }
    }

    void notptr(Register dst) {
        if (compilation.target.arch.is64bit()) {
            notq(dst);
        } else {
            notl(dst);
        }
    }

    @Override
    protected void bangStackWithOffset(int offset) {
        // stack grows down, caller passes positive offset
        assert offset > 0 :  "must bang with negative offset";
        movl(new Address(X86Register.rsp, (-offset)), X86Register.rax);
    }

    @Override
    public void nullCheck(Register reg) {
        nullCheck(reg, -1);
    }

    @Override
    protected boolean pdCheckInstructionMark() {
        return true;
    }

    @Override
    public void buildFrame(int frameSizeInBytes) {
     // Make sure there is enough stack space for this method's activation.
        // Note that we do this before doing an enter(). This matches the
        // ordering of C2's stack overflow check / X86Register.rsp decrement and allows
        // the SharedRuntime stack overflow handling to be consistent
        // between the two compilers.
        generateStackOverflowCheck(frameSizeInBytes);

        enter();

        // c2 leaves fpu stack dirty. Clean it on entry
        if (C1XOptions.SSEVersion < 2) {
          emptyFPUStack();
        }
        decrement(X86Register.rsp, frameSizeInBytes); // does not emit code for frameSize == 0
    }

    public int longConstant(long l) {
        return dataBuffer.emitLong(l);
    }

    public int longConstant(long l, int alignment) {
        dataBuffer.align(alignment);
        return dataBuffer.emitLong(l);
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }
}
