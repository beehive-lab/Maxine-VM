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

import com.sun.c1x.*;
import com.sun.c1x.xir.XirTemplate;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * This class implements the X86-specific portion of the macro assembler.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class X86MacroAssembler extends X86Assembler {

    private CiRegister rscratch1;
    private final C1XCompiler compiler;
    public static final int LONG_SIZE = 8;

    public X86MacroAssembler(C1XCompiler compiler, CiTarget target) {
        super(target);
        this.compiler = compiler;
        this.rscratch1 = compiler.target.scratchRegister;
    }

    public final int callGlobalStub(XirTemplate stub, LIRDebugInfo info, CiRegister result, Object... args) {
        assert args.length == stub.parameters.length;
        return callGlobalStubHelper(compiler.lookupGlobalStub(stub), stub.resultOperand.kind, info, result, args);
    }

    public final int callGlobalStub(GlobalStub stub, LIRDebugInfo info, CiRegister result, Object... args) {
        assert args.length == stub.argOffsets.length;
        return callGlobalStubHelper(stub, stub.resultKind, info, result, args);
    }

    private int callGlobalStubHelper(GlobalStub stub, CiKind resultKind, LIRDebugInfo info, CiRegister result, Object... args) {
        for (int i = 0; i < args.length; i++) {
            storeParameter(args[i], stub.argOffsets[i]);
        }

        int pos = directCall(stub.stubObject, info);

        if (result != CiRegister.None) {
            this.loadResult(result, stub.resultOffset, resultKind);
        }

        // Clear out parameters
        if (C1XOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                movptr(new Address(X86.rsp, stub.argOffsets[i]), 0);
            }
        }
        return pos;
    }

    void loadResult(CiRegister r, int offset, CiKind kind) {
        if (kind == CiKind.Int || kind == CiKind.Boolean) {
            movl(r, new Address(X86.rsp, offset));
        } else {
            movq(r, new Address(X86.rsp, offset));
        }
    }

    void storeParameter(Object registerOrConstant, int offset) {
        if (registerOrConstant instanceof CiConstant) {
            CiConstant c = (CiConstant) registerOrConstant;
            if (c.kind == CiKind.Object) {
                movoop(new Address(X86.rsp, offset), c);
            } else {
                movptr(new Address(X86.rsp, offset), c.asInt());
            }
        } else if (registerOrConstant instanceof CiRegister) {
            movptr(new Address(X86.rsp, offset), (CiRegister) registerOrConstant);
        }
    }

    void increment(CiRegister reg, int value) {
        incrementq(reg, value);
    }

    void decrement(CiRegister reg, int value) {
        decrementq(reg, value);
    }

    void cmpoop(Address src1, Object obj) {
        // (tw) Cannot embed oop as literal (only 32-bit relevant)
        throw Util.unimplemented();
        //cmpLiteral32(src1, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
    }

    void cmpoop(CiRegister src1, Object obj) {
        // (tw) Cannot embed oop as literal (only 32-bit relevant)
        throw Util.unimplemented();
        //cmpLiteral32(src1, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
    }

    void extendSign(CiRegister hi, CiRegister lo) {
        // According to Intel Doc. AP-526, "Integer Divide", p.18.
        if (target.isP6() && hi == X86.rdx && lo == X86.rax) {
            cdql();
        } else {
            movl(hi, lo);
            sarl(hi, 31);
        }
    }

    void movoop(CiRegister dst, CiConstant obj) {
        assert obj.kind == CiKind.Object;
        if (obj.isNull()) {
            this.xorq(dst, dst);
        } else {
            this.movq(dst, recordDataReferenceInCode(obj));
        }
    }

    void movoop(Address dst, CiConstant obj) {
        assert obj.kind == CiKind.Object;
        if (obj.isNull()) {
            xorq(rscratch1, rscratch1);
        } else {
            this.movq(rscratch1, recordDataReferenceInCode(obj));
        }
        movq(dst, rscratch1);
    }

    // src should NEVER be a real pointer. Use AddressLiteral for true pointers
    void movptr(Address dst, long src) {
        mov64(rscratch1, src);
        movq(dst, rscratch1);
    }

    void pushptr(Address src) {
        pushq(src);
    }

    void popptr(Address src) {
        popq(src);
    }

    void xorptr(CiRegister dst, CiRegister src) {
        xorq(dst, src);
    }

    void xorptr(CiRegister dst, Address src) {
        xorq(dst, src);
    }

    // 64 bit versions

    int correctedIdivq(CiRegister reg) {
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
        assert reg != X86.rax && reg != X86.rdx : "reg cannot be X86Register.rax or X86Register.rdx register";
        final long minLong = 0x8000000000000000L;
        Label normalCase = new Label();
        Label specialCase = new Label();

        // check for special case
        cmpq(X86.rax, recordDataReferenceInCode(CiConstant.forLong(minLong)));
        jcc(X86Assembler.ConditionFlag.notEqual, normalCase);
        xorl(X86.rdx, X86.rdx); // prepare X86Register.rdx for possible special case (where
        // remainder = 0)
        cmpq(reg, -1);
        jcc(X86Assembler.ConditionFlag.equal, specialCase);

        // handle normal case
        bind(normalCase);
        cdqq();
        int idivqOffset = codeBuffer.position();
        idivq(reg);

        // normal and special case exit
        bind(specialCase);

        return idivqOffset;
    }

    void decrementq(CiRegister reg, int value) {
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
        } else {
            subq(reg, value);
        }
    }

    void incrementq(CiRegister reg, int value) {
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
        } else {
            addq(reg, value);
        }
    }

    // These are mostly for initializing null
    void movptr(Address dst, int src) {
        movslq(dst, src);
    }

    void movptr(CiRegister dst, long src) {
        mov64(dst, src);
    }

    void stop(String msg) {
        if (C1XOptions.GenAssertionCode) {
            // TODO: pass a pointer to the message
            directCall(CiRuntimeCall.Debug, null);
            hlt();
        }
    }

    // Now versions that are common to 32/64 bit

    void addptr(CiRegister dst, int imm32) {
        addq(dst, imm32);
    }

    void addptr(CiRegister dst, CiRegister src) {
        addq(dst, src);
    }

    void addptr(Address dst, CiRegister src) {
        addq(dst, src);
    }

    @Override
    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            nop(modulus - (codeBuffer.position() % modulus));
        }
    }

    void andptr(CiRegister dst, int imm32) {
        andq(dst, imm32);
    }

    void c2bool(CiRegister x) {
        // implements x == 0 ? 0 : 1
        // note: must only look at least-significant byte of x
        // since C-style booleans are stored in one byte
        // only! (was bug)
        andl(x, 0xFF);
        setb(X86Assembler.ConditionFlag.notZero, x);
    }

    public final void cmp32(CiRegister src1, int imm) {
        cmpl(src1, imm);
    }

    public final void cmp32(CiRegister src1, Address src2) {
        cmpl(src1, src2);
    }

    void cmpsd2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
        assert opr1.isXmm() && opr2.isXmm();
        ucomisd(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(X86Assembler.ConditionFlag.parity, l);
            jcc(X86Assembler.ConditionFlag.below, l);
            movl(dst, 0);
            jcc(X86Assembler.ConditionFlag.equal, l);
            increment(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(X86Assembler.ConditionFlag.parity, l);
            jcc(X86Assembler.ConditionFlag.above, l);
            movl(dst, 0);
            jcc(X86Assembler.ConditionFlag.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void cmpss2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
        assert opr1.isXmm();
        assert opr2.isXmm();
        ucomiss(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(X86Assembler.ConditionFlag.parity, l);
            jcc(X86Assembler.ConditionFlag.below, l);
            movl(dst, 0);
            jcc(X86Assembler.ConditionFlag.equal, l);
            increment(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(X86Assembler.ConditionFlag.parity, l);
            jcc(X86Assembler.ConditionFlag.above, l);
            movl(dst, 0);
            jcc(X86Assembler.ConditionFlag.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void cmpptr(CiRegister src1, CiRegister src2) {
        cmpq(src1, src2);
    }

    void cmpptr(CiRegister src1, Address src2) {
        cmpq(src1, src2);
    }

    void cmpptr(CiRegister src1, int src2) {
        cmpq(src1, src2);
    }

    void cmpptr(Address src1, int src2) {
        cmpq(src1, src2);
    }

    void cmpxchgptr(CiRegister reg, Address adr) {
        cmpxchgq(reg, adr);
    }

    void decrementl(CiRegister reg, int value) {
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
        } else {
            subl(reg, value);
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
        } else {
            subl(dst, value);
        }
    }

    void incrementl(CiRegister reg, int value) {
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
        } else {
            addl(reg, value);
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
        } else {
            addl(dst, value);
        }
    }

    int loadSignedByte(CiRegister dst, Address src) {
        int off = codeBuffer.position();
        movsxb(dst, src); // movsxb
        return off;
    }

    // Note: loadSignedShort used to be called loadSignedWord.
    // Although the 'w' in x86 opcodes refers to the term "word" in the assembler
    // manual : which means 16 bits : that usage is found nowhere in HotSpot code.
    // The term "word" in HotSpot means a 32- or 64-bit machine word.
    int loadSignedShort(CiRegister dst, Address src) {
        // This is dubious to me since it seems safe to do a signed 16 => 64 bit
        // version but this is what 64bit has always done. This seems to imply
        // that users are only using 32bits worth.
        int off = codeBuffer.position();
        movswl(dst, src); // movsxw
        return off;
    }

    int loadUnsignedByte(CiRegister dst, Address src) {
        // According to Intel Doc. AP-526 : "Zero-Extension of Short" : p.16 :
        // and "3.9 Partial Register Penalties" : p. 22.
        int off = codeBuffer.position();
        movzxb(dst, src); // movzxb
        return off;
    }

    // Note: loadUnsignedShort used to be called loadUnsignedWord.
    int loadUnsignedShort(CiRegister dst, Address src) {
        // According to Intel Doc. AP-526, "Zero-Extension of Short", p.16,
        // and "3.9 Partial Register Penalties", p. 22).
        int off = codeBuffer.position();
        movzxl(dst, src); // movzxw
        return off;
    }

    void movptr(CiRegister dst, CiRegister src) {
        movq(dst, src);
    }

    void movptr(CiRegister dst, Address src) {
        movq(dst, src);
    }

    void movptr(Address dst, CiRegister src) {
        movq(dst, src);
    }

    // sign extend as need a l to ptr sized element
    void movl2ptr(CiRegister dst, Address src) {
        movslq(dst, src);
    }

    void movl2ptr(CiRegister dst, CiRegister src) {
        movslq(dst, src);
    }

    @Override
    public void nullCheck(CiRegister reg) {
        // provoke OS null exception if reg = null by
        // accessing M[reg] w/o changing any (non-CC) registers
        // NOTE: cmpl is plenty here to provoke a segv
        cmpptr(X86.rax, new Address(reg, 0));
        // Note: should probably use testl(X86Register.rax, new Address(reg, 0));
        // may be shorter code (however, this version of
        // testl needs to be implemented first)
    }

    void getThread(CiRegister javaThread) {
        // Platform-specific! Solaris / Windows / Linux
        Util.nonFatalUnimplemented();
    }

    void shlptr(CiRegister dst, int imm8) {
        shlq(dst, imm8);
    }

    void shrptr(CiRegister dst, int imm8) {
        shrq(dst, imm8);
    }

    void signExtendByte(CiRegister reg) {
        if (reg.isByte()) {
            movsxb(reg, reg); // movsxb
        } else {
            shll(reg, 24);
            sarl(reg, 24);
        }
    }

    void signExtendShort(CiRegister reg) {
        movsxw(reg, reg); // movsxw
    }

    void subptr(CiRegister dst, int imm32) {
        subq(dst, imm32);
    }

    void subptr(CiRegister dst, CiRegister src) {
        subq(dst, src);
    }

    void testptr(CiRegister dst, CiRegister src) {
        testq(dst, src);
    }

    boolean verifyOop(CiRegister reg) {
        if (!C1XOptions.VerifyOops) {
            return true;
        }

        // TODO: Figure out how to verify oops, maybe a runtime call?
        throw Util.unimplemented();
    }

    // Support optimal SSE move instructions.
    void movflt(CiRegister dst, CiRegister src) {
        assert dst.isXmm() && src.isXmm();
        if (C1XOptions.UseXmmRegToRegMoveAll) {
            movaps(dst, src);
        } else {
            movss(dst, src);
        }
    }

    void movflt(CiRegister dst, Address src) {
        assert dst.isXmm();
        movss(dst, src);
    }

    void movflt(Address dst, CiRegister src) {
        assert src.isXmm();
        movss(dst, src);
    }

    void movdbl(CiRegister dst, CiRegister src) {
        assert dst.isXmm() && src.isXmm();
        if (C1XOptions.UseXmmRegToRegMoveAll) {
            movapd(dst, src);
        } else {
            movsd(dst, src);
        }
    }

    void movdbl(CiRegister dst, Address src) {
        assert dst.isXmm();
        if (C1XOptions.UseXmmLoadAndClearUpper) {
            movsd(dst, src);
        } else {
            movlpd(dst, src);
        }
    }

    void movdbl(Address dst, CiRegister src) {
        assert src.isXmm();
        movsd(dst, src);
    }

    void addptr(Address dst, int src) {
        addq(dst, src);
    }

    void addptr(CiRegister dst, Address src) {
        addq(dst, src);
    }

    void andptr(CiRegister src1, CiRegister src2) {
        andq(src1, src2);
    }

    int lockObject(RiRuntime runtime, CiRegister hdr, CiRegister obj, CiRegister dispHdr, CiRegister scratch, Label slowCase) {
        jmp(slowCase);
        return 0;
    }

    public void unlockObject(RiRuntime runtime, CiRegister hdr, CiRegister obj, CiRegister dispHdr, Label slowCase) {
        jmp(slowCase);
    }

    void invalidateRegisters(boolean invRax, boolean invRbx, boolean invRcx, boolean invRdx, boolean invRsi, boolean invRdi) {

        if (C1XOptions.GenAssertionCode) {
            if (invRax) {
                movptr(X86.rax, 0xDEAD);
            }
            if (invRbx) {
                movptr(X86.rbx, 0xDEAD);
            }
            if (invRcx) {
                movptr(X86.rcx, 0xDEAD);
            }
            if (invRdx) {
                movptr(X86.rdx, 0xDEAD);
            }
            if (invRsi) {
                movptr(X86.rsi, 0xDEAD);
            }
            if (invRdi) {
                movptr(X86.rdi, 0xDEAD);
            }
        }
    }

    void verifyNotNullOop(CiRegister r) {
        if (!C1XOptions.VerifyOops) {
            return;
        }
        Label notNull = new Label();
        testptr(r, r);
        jcc(X86Assembler.ConditionFlag.notZero, notNull);
        stop("non-null oop required");
        bind(notNull);
        verifyOop(r);
    }

    void xchgptr(CiRegister src1, CiRegister src2) {
        xchgq(src1, src2);
    }

    void cmov(ConditionFlag cc, CiRegister dst, CiRegister src) {
        cmovq(cc, dst, src);
    }

    void cmovptr(ConditionFlag cc, CiRegister dst, Address src) {
        cmovq(cc, dst, src);
    }

    void cmovptr(ConditionFlag cc, CiRegister dst, CiRegister src) {
        cmovq(cc, dst, src);
    }

    void orptr(CiRegister dst, CiRegister src) {
        orq(dst, src);
    }

    void orptr(CiRegister dst, int src) {
        orq(dst, src);
    }

    void shlptr(CiRegister dst) {
        shlq(dst);
    }

    void shrptr(CiRegister dst) {
        shrq(dst);
    }

    void sarptr(CiRegister dst) {
        sarq(dst);
    }

    void sarptr(CiRegister dst, int src) {
        sarq(dst, src);
    }

    void negptr(CiRegister dst) {
        negq(dst);
    }

    private void bangStackWithOffset(int offset) {
        // stack grows down, caller passes positive offset
        assert offset > 0 :  "must bang with negative offset";
        movq(new Address(X86.rsp, (-offset)), X86.rax);
    }

    @Override
    public void buildFrame(int frameSizeInBytes) {
        decrement(X86.rsp, frameSizeInBytes); // does not emit code for frameSize == 0
        int framePages = frameSizeInBytes / target.pageSize;
        // emit multiple stack bangs for methods with frames larger than a page
        for (int i = 0; i <= framePages; i++) {
            bangStackWithOffset((i + C1XOptions.StackShadowPages) * target.pageSize);
        }
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }

    public void safepoint(LIRDebugInfo info) {
        CiRegister safepointRegister = compiler.target.registerConfig.getSafepointRegister();
        this.recordSafepoint(codeBuffer.position(), info.registerRefMap(), info.stackRefMap(), info.debugInfo());
        movq(safepointRegister, new Address(safepointRegister));
    }

    public void enter(short imm16, byte imm8) {
        emitByte(0xC8);
        // appended:
        emitByte(imm16 & 0xff);
        imm16 >>= 8;
        emitByte(imm16 & 0xff);
        emitByte(imm8);
    }
}
