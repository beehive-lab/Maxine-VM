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
 */
public class X86MacroAssembler extends X86Assembler {

    private CiRegister rscratch1;
    private final int wordSize;
    private final C1XCompiler compiler;
    public static final int LONG_SIZE = 8;

    public X86MacroAssembler(C1XCompiler compiler, CiTarget target, int frameSize) {
        super(target, frameSize);
        // TODO: make macro assembler compiler independent w.r.t global stubs
        this.compiler = compiler;

        rscratch1 = compiler.target.scratchRegister;
        wordSize = this.target.arch.wordSize;
    }

    public final int callGlobalStub(GlobalStub stub, LIRDebugInfo info, CiRegister result, CiRegister... args) {
        RegisterOrConstant[] rc = new RegisterOrConstant[args.length];
        for (int i = 0; i < args.length; i++) {
            rc[i] = new RegisterOrConstant(args[i]);
        }
        return callGlobalStub(stub, info, result, rc);
    }

    public final int callRuntimeCalleeSaved(CiRuntimeCall stub, LIRDebugInfo info, CiRegister result, CiRegister... args) {
        RegisterOrConstant[] rc = new RegisterOrConstant[args.length];
        for (int i = 0; i < args.length; i++) {
            rc[i] = new RegisterOrConstant(args[i]);
        }
        return callRuntimeCalleeSaved(stub, info, result, rc);
    }

    public final int callGlobalStub(XirTemplate stub, C1XCompilation compilation, LIRDebugInfo info, CiRegister result, RegisterOrConstant...args) {
        assert args.length == stub.parameters.length;
        return callGlobalStubHelper(compiler.lookupGlobalStub(stub), compilation, stub.resultOperand.kind, info, result, args);
    }

    public final int callGlobalStubNoArgs(GlobalStub stub, LIRDebugInfo info, CiRegister result) {
        assert 0 == stub.arguments.length;
        return callGlobalStubHelper(compiler.lookupGlobalStub(stub), null, CiKind.Illegal, info, result);
    }


    public final int callGlobalStub(GlobalStub stub, LIRDebugInfo info, CiRegister result, RegisterOrConstant...args) {
        assert args.length == stub.arguments.length;
        return callGlobalStubHelper(compiler.lookupGlobalStub(stub), null, CiKind.Illegal, info, result, args);
    }

    public final int callRuntimeCalleeSaved(CiRuntimeCall stub, LIRDebugInfo info, CiRegister result, RegisterOrConstant...args) {
        assert args.length == stub.arguments.length;
        return callGlobalStubHelper(compiler.lookupGlobalStub(stub), null, CiKind.Illegal, info, result, args);
    }

    private int callGlobalStubHelper(Object stub, C1XCompilation compilation, CiKind resultKind, LIRDebugInfo info, CiRegister result, RegisterOrConstant... args) {
        int index = 0;
        for (RegisterOrConstant op : args) {
            storeParameter(op, index++);
        }

        emitGlobalStubCall(stub, info);
        int pos = this.codeBuffer.position();

        if (result != CiRegister.None) {
            this.loadResult(result, 0, resultKind);
        }

        // Clear out parameters
        if (C1XOptions.GenAssertionCode) {
            for (index = 0; index < args.length; index++) {
                storeParameter(0, index++);
            }
        }
        return pos;
    }

    private int calcGlobalStubParameterOffset(int index) {
        assert index >= 0 : "invalid offset from rsp";
        return -(index + 2) * target.arch.wordSize;
    }

    void loadResult(CiRegister r, int index, CiKind kind) {
        int offsetFromRspInBytes = calcGlobalStubParameterOffset(index);
        if (kind == CiKind.Int || kind == CiKind.Boolean) {
            movl(r, new Address(X86.rsp, offsetFromRspInBytes));
        } else {
            assert kind == CiKind.Long || kind == CiKind.Object || kind == CiKind.Word || kind == CiKind.Illegal;
            assert target.arch.is64bit();
            movq(r, new Address(X86.rsp, offsetFromRspInBytes));
        }
    }

    void storeParameter(CiRegister r, int index) {
        int offsetFromRspInBytes = calcGlobalStubParameterOffset(index);
        movptr(new Address(X86.rsp, offsetFromRspInBytes), r);
    }

    void storeParameter(int c, int index) {
        int offsetFromRspInBytes = calcGlobalStubParameterOffset(index);
        movptr(new Address(X86.rsp, offsetFromRspInBytes), c);
    }

    void storeParameter(RegisterOrConstant rc, int offsetFromRspInWords) {
        if (rc.isConstant()) {
            storeParameter(rc.asConstant(), offsetFromRspInWords);
        } else if (rc.isOopConstant()) {
            storeParameter(CiConstant.forObject(rc.asOop()), offsetFromRspInWords);
        } else {
            assert rc.isRegister();
            storeParameter(rc.asRegister(), offsetFromRspInWords);
        }
    }

    void storeParameter(CiConstant o, int index) {
        assert o.basicType == CiKind.Object;
        int offsetFromRspInBytes = calcGlobalStubParameterOffset(index);
        movoop(new Address(X86.rsp, offsetFromRspInBytes), o);
    }

    void increment(CiRegister reg, int value /* = 1 */) {
        if (target.arch.is64bit()) {
            incrementq(reg, value);
        } else {
            incrementl(reg, value);
        }
    }

    void decrement(CiRegister reg, int value /* = 1 */) {
        if (target.arch.is64bit()) {
            decrementq(reg, value);
        } else {
            decrementl(reg, value);
        }
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

    // Note: yLo will be destroyed
    void lcmp2int(CiRegister xHi, CiRegister xLo, CiRegister yHi, CiRegister yLo) {
        if (target.arch.is64bit()) {
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
        Address xHi = new Address(X86.rsp, xRspOffset + target.arch.wordSize);
        Address xLo = new Address(X86.rsp, xRspOffset);
        Address yHi = new Address(X86.rsp, yRspOffset + target.arch.wordSize);
        Address yLo = new Address(X86.rsp, yRspOffset);
        Label quick = new Label();
        // load xHi, yHi and check if quick
        // multiplication is possible
        movl(X86.rbx, xHi);
        movl(X86.rcx, yHi);
        movl(X86.rax, X86.rbx);
        orl(X86.rbx, X86.rcx); // X86Register.rbx, = 0 <=> xHi = 0 and yHi = 0
        jcc(X86Assembler.Condition.zero, quick); // if X86Register.rbx, = 0 do quick multiply
        // do full multiplication
        // 1st step
        mull(yLo); // xHi * yLo
        movl(X86.rbx, X86.rax); // save lo(xHi * yLo) in X86Register.rbx,
        // 2nd step
        movl(X86.rax, xLo);
        mull(X86.rcx); // xLo * yHi
        addl(X86.rbx, X86.rax); // add lo(xLo * yHi) to X86Register.rbx,
        // 3rd step
        bind(quick); // note: X86Register.rbx, = 0 if quick multiply!
        movl(X86.rax, xLo);
        mull(yLo); // xLo * yLo
        addl(X86.rdx, X86.rbx); // correct hi(xLo * yLo)
    }

    void lneg(CiRegister hi, CiRegister lo) {

        if (target.arch.is64bit()) {
            Util.shouldNotReachHere(); // 64bit doesn't use two regs
        }
        negl(lo);
        adcl(hi, 0);
        negl(hi);
    }

    void lshl(CiRegister hi, CiRegister lo) {
        // Java shift left long support (semantics as described in JVM spec., p.305)
        // (basic idea for shift counts s >= n: x << s == (x << n) << (s - n))
        // shift value is in X86Register.rcx !
        assert hi != X86.rcx : "must not use X86Register.rcx";
        assert lo != X86.rcx : "must not use X86Register.rcx";
        CiRegister s = X86.rcx; // shift count
        int n = target.arch.wordSize * Byte.SIZE;
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

    void lshr(CiRegister hi, CiRegister lo, boolean signExtension) {
        // Java shift right long support (semantics as described in JVM spec., p.306 & p.310)
        // (basic idea for shift counts s >= n: x >> s == (x >> n) >> (s - n))
        assert hi != X86.rcx : "must not use X86Register.rcx";
        assert lo != X86.rcx : "must not use X86Register.rcx";
        CiRegister s = X86.rcx; // shift count
        int n = target.arch.wordSize * Byte.SIZE;
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

    void movoop(CiRegister dst, CiConstant obj) {
        assert obj.basicType == CiKind.Object;
        if (target.arch.is32bit()) {
            // (tw) Cannot embed oop as immediate!
            throw Util.unimplemented();
        } else if (target.arch.is64bit()) {
            if (obj.asObject() == null) {
                this.xorq(dst, dst);
            } else {
                this.movq(dst, recordDataReferenceInCode(obj));
            }
        } else {
            Util.shouldNotReachHere();
        }
    }


    void movoop(Address dst, CiConstant obj) {
        assert obj.basicType == CiKind.Object;

        if (target.arch.is32bit()) {
            // (tw) Cannot embed oop as immediate!
            throw Util.unimplemented();
            //movLiteral32(dst, compilation.runtime.convertToPointer32(obj), Relocation.specForImmediate());
        } else if (target.arch.is64bit()) {
            if (obj.asObject() == null) {
                xorq(rscratch1, rscratch1);
            } else {
                this.movq(rscratch1, recordDataReferenceInCode(obj));
            }
            movq(dst, rscratch1);
        } else {
            Util.shouldNotReachHere();
        }
    }

    // src should NEVER be a real pointer. Use AddressLiteral for true pointers
    void movptr(Address dst, long src) {
        if (target.arch.is32bit()) {
            movl(dst, Util.safeToInt(src));
        } else if (target.arch.is64bit()) {
            mov64(rscratch1, src);
            movq(dst, rscratch1);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void pushptr(Address src) {
        if (target.arch.is64bit()) {
            pushq(src);
        } else {
            pushl(src);
        }
    }

    void popptr(Address src) {
        if (target.arch.is64bit()) {
            popq(src);
        } else {
            popl(src);
        }
    }

    void xorptr(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            xorq(dst, src);
        } else {
            xorl(dst, src);
        }
    }

    void xorptr(CiRegister dst, Address src) {
        if (target.arch.is64bit()) {
            xorq(dst, src);
        } else {
            xorl(dst, src);
        }
    }

    // 64 bit versions

    int correctedIdivq(CiRegister reg) {
        assert target.arch.is64bit();
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
        jcc(X86Assembler.Condition.notEqual, normalCase);
        xorl(X86.rdx, X86.rdx); // prepare X86Register.rdx for possible special case (where
        // remainder = 0)
        cmpq(reg, -1);
        jcc(X86Assembler.Condition.equal, specialCase);

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
        assert target.arch.is64bit();
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
        assert target.arch.is64bit();
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
        assert target.arch.is64bit();
        movslq(dst, src);
    }

    void movptr(CiRegister dst, long src) {
        assert target.arch.is64bit();
        mov64(dst, src);
    }

    void stop(String msg) {

        if (target.arch.is64bit()) {
            // TODO: Add debug infos / message as paramters to Debug
            callRuntime(CiRuntimeCall.Debug);
            hlt();
        } else {
            throw Util.unimplemented();
        }
    }

    // Now versions that are common to 32/64 bit

    void addptr(CiRegister dst, int imm32) {

        if (target.arch.is64bit()) {
            addq(dst, imm32);
        } else {
            addl(dst, imm32);
        }
    }

    void addptr(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    void addptr(Address dst, CiRegister src) {
        if (target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    @Override
    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            nop(modulus - (codeBuffer.position() % modulus));
        }
    }

    void andptr(CiRegister dst, int imm32) {
        if (target.arch.is64bit()) {
            andq(dst, imm32);
        } else {
            andl(dst, imm32);
        }
    }

    void c2bool(CiRegister x) {
        // implements x == 0 ? 0 : 1
        // note: must only look at least-significant byte of x
        // since C-style booleans are stored in one byte
        // only! (was bug)
        andl(x, 0xFF);
        setb(X86Assembler.Condition.notZero, x);
    }

    public final void cmp32(CiRegister src1, int imm) {
        cmpl(src1, imm);
    }

    public final void cmp32(CiRegister src1, Address src2) {
        cmpl(src1, src2);
    }

    void cmpsd2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
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

    void cmpss2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
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

    void cmpptr(CiRegister src1, CiRegister src2) {
        if (target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(CiRegister src1, Address src2) {
        if (target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(CiRegister src1, int src2) {
        if (target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpptr(Address src1, int src2) {
        if (target.arch.is64bit()) {
            cmpq(src1, src2);
        } else {
            cmpl(src1, src2);
        }
    }

    void cmpxchgptr(CiRegister reg, Address adr) {
        if (target.arch.is64bit()) {
            cmpxchgq(reg, adr);
        } else {
            cmpxchgl(reg, adr);
        }

    }

    int correctedIdivl(CiRegister reg) {
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
        assert reg != X86.rax && reg != X86.rdx : "reg cannot be X86Register.rax, or X86Register.rdx register";
        int minInt = 0x80000000;
        Label normalCase = new Label();
        Label specialCase = new Label();

        // check for special case
        cmpl(X86.rax, minInt);
        jcc(Condition.notEqual, normalCase);
        xorl(X86.rdx, X86.rdx); // prepare X86Register.rdx for possible special case (where remainder =
        // 0)
        cmpl(reg, -1);
        jcc(Condition.equal, specialCase);

        // handle normal case
        bind(normalCase);
        cdql();
        int idivlOffset = codeBuffer.position();
        idivl(reg);

        // normal and special case exit
        bind(specialCase);

        return idivlOffset;
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

    // Defines obj : preserves varSizeInBytes
    void edenAllocate(CiRegister obj, CiRegister varSizeInBytes, int conSizeInBytes, CiRegister t1, Label slowCase) {
        throw Util.unimplemented();
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
        int off;
        if (target.arch.is64bit() || target.isP6()) {
            off = codeBuffer.position();
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
    int loadSignedShort(CiRegister dst, Address src) {
        int off;
        if (target.arch.is64bit() || target.isP6()) {
            // This is dubious to me since it seems safe to do a signed 16 => 64 bit
            // version but this is what 64bit has always done. This seems to imply
            // that users are only using 32bits worth.
            off = codeBuffer.position();
            movswl(dst, src); // movsxw
        } else {
            off = loadUnsignedShort(dst, src);
            shll(dst, 16);
            sarl(dst, 16);
        }
        return off;
    }

    int loadUnsignedByte(CiRegister dst, Address src) {
        // According to Intel Doc. AP-526 : "Zero-Extension of Short" : p.16 :
        // and "3.9 Partial Register Penalties" : p. 22.
        int off;
        if (target.arch.is64bit() || target.isP6() || src.uses(dst)) {
            off = codeBuffer.position();
            movzbl(dst, src); // movzxb
        } else {
            xorl(dst, dst);
            off = codeBuffer.position();
            movb(dst, src);
        }
        return off;
    }

    // Note: loadUnsignedShort used to be called loadUnsignedWord.
    int loadUnsignedShort(CiRegister dst, Address src) {
        // According to Intel Doc. AP-526, "Zero-Extension of Short", p.16,
        // and "3.9 Partial Register Penalties", p. 22).
        int off;
        if (target.arch.is64bit() || target.isP6() || src.uses(dst)) {
            off = codeBuffer.position();
            movzwl(dst, src); // movzxw
        } else {
            xorl(dst, dst);
            off = codeBuffer.position();
            movw(dst, src);
        }
        return off;
    }

    void movptr(CiRegister dst, CiRegister src) {

        if (target.arch.is64bit()) {

            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movptr(CiRegister dst, Address src) {

        if (target.arch.is64bit()) {
            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movptr(Address dst, CiRegister src) {
        if (target.arch.is64bit()) {
            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    // sign extend as need a l to ptr sized element
    void movl2ptr(CiRegister dst, Address src) {
        if (target.arch.is64bit()) {
            movslq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movl2ptr(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            movslq(dst, src);
        } else if (dst != src) {
            movl(dst, src);
        }
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
        if (target.arch.is64bit()) {
            shlq(dst, imm8);
        } else {
            shll(dst, imm8);
        }
    }

    void shrptr(CiRegister dst, int imm8) {
        if (target.arch.is64bit()) {
            shrq(dst, imm8);
        } else {
            shrl(dst, imm8);
        }
    }

    void signExtendByte(CiRegister reg) {
        if (target.arch.is64bit() || target.isP6() && reg.isByte()) {
            movsbl(reg, reg); // movsxb
        } else {
            shll(reg, 24);
            sarl(reg, 24);
        }
    }

    void signExtendShort(CiRegister reg) {
        if (target.arch.is64bit() || target.isP6()) {
            movswl(reg, reg); // movsxw
        } else {
            shll(reg, 16);
            sarl(reg, 16);
        }
    }

    void subptr(CiRegister dst, int imm32) {
        if (target.arch.is64bit()) {
            subq(dst, imm32);
        } else {
            subl(dst, imm32);
        }
    }

    void subptr(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            subq(dst, src);
        } else {
            subl(dst, src);
        }
    }

    void testptr(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            testq(dst, src);
        } else {
            testl(dst, src);
        }
    }

    // Defines obj : preserves varSizeInBytes : okay for t2 == varSizeInBytes.
    void tlabAllocate(RiRuntime runtime, CiRegister obj, CiRegister varSizeInBytes, int conSizeInBytes, CiRegister t1, CiRegister t2, Label slowCase) {
        assert CiRegister.assertDifferentRegisters(obj, t1, t2);
        assert CiRegister.assertDifferentRegisters(obj, varSizeInBytes, t1);
        Util.unimplemented();
//        CiRegister end = t2;
//        CiRegister thread = t1;
//        if (target.arch.is64bit()) {
//            thread = runtime.threadRegister();
//        }
//
//        verifyTlab(runtime);
//
//        if (!target.arch.is64bit()) {
//            getThread(thread);
//        }
//
//        movptr(obj, new Address(thread, runtime.threadTlabTopOffset()));
//        if (varSizeInBytes == CiRegister.None) {
//            lea(end, new Address(obj, conSizeInBytes));
//        } else {
//            lea(end, new Address(obj, varSizeInBytes, Address.ScaleFactor.times1));
//        }
//        cmpptr(end, new Address(thread, runtime.threadTlabEndOffset()));
//        jcc(X86Assembler.Condition.above, slowCase);
//
//        // update the tlab top pointer
//        movptr(new Address(thread, runtime.threadTlabTopOffset()), end);
//
//        // recover varSizeInBytes if necessary
//        if (varSizeInBytes == end) {
//            subptr(varSizeInBytes, obj);
//        }
//        verifyTlab(runtime);
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
        assert dst.isXMM() && src.isXMM();
        if (C1XOptions.UseXmmRegToRegMoveAll) {
            movaps(dst, src);
        } else {
            movss(dst, src);
        }
    }

    void movflt(CiRegister dst, Address src) {
        assert dst.isXMM();
        movss(dst, src);
    }

    void movflt(Address dst, CiRegister src) {
        assert src.isXMM();
        movss(dst, src);
    }

    void movdbl(CiRegister dst, CiRegister src) {
        assert dst.isXMM() && src.isXMM();
        if (C1XOptions.UseXmmRegToRegMoveAll) {
            movapd(dst, src);
        } else {
            movsd(dst, src);
        }
    }

    void movdbl(CiRegister dst, Address src) {
        assert dst.isXMM();
        if (C1XOptions.UseXmmLoadAndClearUpper) {
            movsd(dst, src);
        } else {
            movlpd(dst, src);
        }
    }

    void movdbl(Address dst, CiRegister src) {
        assert src.isXMM();
        movsd(dst, src);
    }

    void addptr(Address dst, int src) {
        if (target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    void addptr(CiRegister dst, Address src) {
        if (target.arch.is64bit()) {
            addq(dst, src);
        } else {
            addl(dst, src);
        }
    }

    void andptr(CiRegister src1, CiRegister src2) {
        if (target.arch.is64bit()) {
            andq(src1, src2);
        } else {
            andl(src1, src2);
        }
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
        jcc(X86Assembler.Condition.notZero, notNull);
        stop("non-null oop required");
        bind(notNull);
        verifyOop(r);
    }

    void xchgptr(CiRegister src1, CiRegister src2) {
        if (target.arch.is64bit()) {
            xchgq(src1, src2);
        } else {
            xchgl(src1, src2);
        }
    }

    void allocateArray(RiRuntime runtime, CiRegister obj, CiRegister len, CiRegister t1, CiRegister t2, int headerSize, Address.ScaleFactor scaleFactor, CiRegister klass, Label slowCase) {
        assert obj == X86.rax : "obj must be in X86Register.rax :  for cmpxchg";
        assert CiRegister.assertDifferentRegisters(obj, len, t1, t2, klass);

        // determine alignment mask
        assert (wordSize & 1) == 0 : "must be a multiple of 2 for masking code to work";

        // check for negative or excessive length
        cmpptr(len, runtime.maximumArrayLength());
        jcc(X86Assembler.Condition.above, slowCase);

        CiRegister arrSize = t2; // okay to be the same

        int getMinObjAlignmentInBytesMask = 0xbadbabe; // runtime.getMinObjAlignmentInBytesMask()
        // align object end
        movptr(arrSize, headerSize * wordSize + getMinObjAlignmentInBytesMask);
        lea(arrSize, new Address(arrSize, len, scaleFactor));
        andptr(arrSize, ~getMinObjAlignmentInBytesMask);

        tryAllocate(runtime, obj, arrSize, 0, t1, t2, slowCase);

        initializeHeader(runtime, obj, klass, len, t1, t2);

        // clear rest of allocated space
        initializeBody(obj, arrSize, headerSize * wordSize, len);

        verifyOop(obj);
    }

    // Defines obj, preserves varSizeInBytes
    void tryAllocate(RiRuntime runtime, CiRegister obj, CiRegister varSizeInBytes, int conSizeInBytes, CiRegister t1, CiRegister t2, Label slowCase) {
        if (C1XOptions.UseTLAB) {
            tlabAllocate(runtime, obj, varSizeInBytes, conSizeInBytes, t1, t2, slowCase);
        } else {
            edenAllocate(obj, varSizeInBytes, conSizeInBytes, t1, slowCase);
        }
    }

    void initializeHeader(RiRuntime runtime, CiRegister obj, CiRegister klass, CiRegister len, CiRegister t1, CiRegister t2) {
        Util.unimplemented();
    }

    // preserves obj, destroys lenInBytes
    void initializeBody(CiRegister obj, CiRegister lenInBytes, int hdrSizeInBytes, CiRegister t1) {
        Label done = new Label();
        assert obj != lenInBytes && obj != t1 && t1 != lenInBytes : "registers must be different";
        assert (hdrSizeInBytes & (wordSize - 1)) == 0 : "header size is not a multiple of BytesPerWord";
        CiRegister index = lenInBytes;
        // index is positive and ptr sized
        subptr(index, hdrSizeInBytes);
        jcc(X86Assembler.Condition.zero, done);
        // initialize topmost word, divide index by 2, check if odd and test if zero
        // note: for the remaining code to work, index must be a multiple of BytesPerWord

        if (C1XOptions.GenAssertionCode) {
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

        if (target.arch.is32bit()) {
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
        if (target.arch.is32bit()) {
            movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - 2 * wordSize), t1);
        }
        decrement(index, 1);
        jcc(X86Assembler.Condition.notZero, loop);

        // done
        bind(done);
    }

    void allocateObject(RiRuntime runtime, CiRegister obj, CiRegister t1, CiRegister t2, int headerSize, int objectSize, CiRegister klass, Label slowCase) {
        assert obj == X86.rax : "obj must be in X86Register.rax :  for cmpxchg";
        assert obj != t1 && obj != t2 && t1 != t2 : "registers must be different"; // XXX really?
        assert headerSize >= 0 && objectSize >= headerSize : "illegal sizes";

        tryAllocate(runtime, obj, CiRegister.None, objectSize * wordSize, t1, t2, slowCase);

        initializeObject(runtime, obj, klass, CiRegister.None, objectSize * wordSize, t1, t2);
    }

    void initializeObject(RiRuntime runtime, CiRegister obj, CiRegister klass, CiRegister varSizeInBytes, int conSizeInBytes, CiRegister t1, CiRegister t2) {
        //assert (conSizeInBytes & runtime.getMinObjAlignmentInBytesMask()) == 0 : "conSizeInBytes is not multiple of alignment";

        Util.unimplemented();
        int hdrSizeInBytes = 0xbadbabe; // runtime.instanceOopDescBaseOffsetInBytes();

        initializeHeader(runtime, obj, klass, CiRegister.None, t1, t2);

        // clear rest of allocated space
        CiRegister t1Zero = t1;
        CiRegister index = t2;
        int threshold = 6 * wordSize; // approximate break even point for code size (see comments below)
        if (varSizeInBytes != CiRegister.None) {
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
            if (!target.arch.is64bit()) {
                movptr(new Address(obj, index, Address.ScaleFactor.times8, hdrSizeInBytes - (2 * wordSize)), t1Zero);
            }
            decrement(index, 1);
            jcc(X86Assembler.Condition.notZero, loop);

        }

        verifyOop(obj);
    }

    void cmov(Condition cc, CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            cmovq(cc, dst, src);
        } else {
            cmovl(cc, dst, src);
        }
    }

    void cmovptr(Condition cc, CiRegister dst, Address src) {
        if (target.arch.is64bit()) {
            cmovq(cc, dst, src);
        } else {
            cmovl(cc, dst, src);
        }
    }

    void cmovptr(Condition cc, CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            cmovq(cc, dst, src);
        } else {
            cmovl(cc, dst, src);
        }
    }

    void orptr(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            orq(dst, src);
        } else {
            orl(dst, src);
        }
    }

    void orptr(CiRegister dst, int src) {
        if (target.arch.is64bit()) {
            orq(dst, src);
        } else {
            orl(dst, src);
        }
    }

    void shlptr(CiRegister dst) {
        if (target.arch.is64bit()) {
            shlq(dst);
        } else {
            shll(dst);
        }
    }

    void shrptr(CiRegister dst) {
        if (target.arch.is64bit()) {
            shrq(dst);
        } else {
            shrl(dst);
        }
    }

    void sarptr(CiRegister dst) {
        if (target.arch.is64bit()) {
            sarq(dst);
        } else {
            sarl(dst);
        }
    }

    void sarptr(CiRegister dst, int src) {
        if (target.arch.is64bit()) {
            sarq(dst, src);
        } else {
            sarl(dst, src);
        }
    }

    void negptr(CiRegister dst) {
        if (target.arch.is64bit()) {
            negq(dst);
        } else {
            negl(dst);
        }
    }

    private void bangStackWithOffset(int offset) {
        // stack grows down, caller passes positive offset
        assert offset > 0 :  "must bang with negative offset";
        if (target.arch.is64bit()) {
            movq(new Address(X86.rsp, (-offset)), X86.rax);
        } else {
            movl(new Address(X86.rsp, (-offset)), X86.rax);
        }
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
        CiRegister safepointRegister = compiler.target.config.getSafepointRegister();
        this.recordSafepoint(codeBuffer.position(), info.oopMap.registerMap(), info.oopMap.stackMap());
        movq(safepointRegister, new Address(safepointRegister));
    }

}
