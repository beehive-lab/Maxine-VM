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
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public abstract class X86Assembler extends AbstractAssembler {

    /**
     * The x86 condition codes used for conditional jumps/moves.
     */
    public enum Condition {
        zero(0x4),
        notZero(0x5),
        equal(0x4),
        notEqual(0x5),
        less(0xc),
        lessEqual(0xe),
        greater(0xf), greaterEqual(0xd), below(0x2), belowEqual(0x6), above(0x7), aboveEqual(0x3), overflow(0x0), noOverflow(
                        0x1), carrySet(0x2), carryClear(0x3), negative(0x8), positive(0x9), parity(0xa), noParity(0xb);

        public final int value;

        private Condition(int value) {
            this.value = value;
        }
    }

    /**
     * Constants for X86 prefix bytes.
     */
    private class Prefix {
        private static final int REX = 0x40;
        private static final int REXB = 0x41;
        private static final int REXX = 0x42;
        private static final int REXXB = 0x43;
        private static final int REXR = 0x44;
        private static final int REXRB = 0x45;
        private static final int REXRX = 0x46;
        private static final int REXRXB = 0x47;
        private static final int REXW = 0x48;
        private static final int REXWB = 0x49;
        private static final int REXWX = 0x4A;
        private static final int REXWXB = 0x4B;
        private static final int REXWR = 0x4C;
        private static final int REXWRB = 0x4D;
        private static final int REXWRX = 0x4E;
        private static final int REXWRXB = 0x4F;
    }

    public X86Assembler(CiTarget target, int frameSize) {
        super(target, frameSize);
    }

    private static int encode(CiRegister r) {
        assert r.encoding < 16;
        assert r.encoding >= 0;
        return r.encoding & 0x7;
    }

    void emitArithB(int op1, int op2, CiRegister dst, int imm8) {
        assert dst.isByte() : "must have byte register";
        assert Util.isByte(op1) && Util.isByte(op2) : "wrong opcode";
        assert Util.isByte(imm8) : "not a byte";
        assert (op1 & 0x01) == 0 : "should be 8bit operation";
        emitByte(op1);
        emitByte(op2 | encode(dst));
        emitByte(imm8);
    }

    void emitArith(int op1, int op2, CiRegister dst, int imm32) {
        assert Util.isByte(op1) && Util.isByte(op2) : "wrong opcode";
        assert (op1 & 0x01) == 1 : "should be 32bit operation";
        assert (op1 & 0x02) == 0 : "sign-extension bit should not be set";
        if (Util.is8bit(imm32)) {
            emitByte(op1 | 0x02); // set sign bit
            emitByte(op2 | encode(dst));
            emitByte(imm32 & 0xFF);
        } else {
            emitByte(op1);
            emitByte(op2 | encode(dst));
            emitInt(imm32);
        }
    }

    // immediate-to-memory forms
    void emitArithOperand(int op1, CiRegister rm, Address adr, int imm32) {
        assert (op1 & 0x01) == 1 : "should be 32bit operation";
        assert (op1 & 0x02) == 0 : "sign-extension bit should not be set";
        if (Util.is8bit(imm32)) {
            emitByte(op1 | 0x02); // set sign bit
            emitOperand(rm, adr, 1);
            emitByte(imm32 & 0xFF);
        } else {
            emitByte(op1);
            emitOperand(rm, adr, 4);
            emitInt(imm32);
        }
    }

    public void testptr(CiRegister src, int imm32) {
        if (target.arch.is64bit()) {
            testq(src, imm32);
        } else {
            testl(src, imm32);
        }
    }

    void emitArith(int op1, int op2, CiRegister dst, CiRegister src) {
        assert Util.isByte(op1) && Util.isByte(op2) : "wrong opcode";
        emitByte(op1);
        emitByte(op2 | encode(dst) << 3 | encode(src));
    }

    void emitOperandHelper(CiRegister reg, Address addr) {

        CiRegister base = addr.base;


        CiRegister index = addr.index;
        Address.ScaleFactor scale = addr.scale;
        int disp = addr.disp;

        if (base == CiRegister.Stack) {
            base = this.target.stackRegister;
        } else if (base == CiRegister.CallerStack) {
            base = this.target.stackRegister;
            disp += targetMethod.frameSize() + target.arch.wordSize;
        }


        // Encode the registers as needed in the fields they are used in

        assert reg != CiRegister.None;

        int regenc = encode(reg) << 3;
        int indexenc = index.isValid() ? encode(index) << 3 : 0;
        int baseenc = base.isValid() ? encode(base) : 0;

        if (base.isValid()) {
            if (index.isValid()) {
                assert scale != Address.ScaleFactor.noScale : "inconsistent Address";
                // [base + indexscale + disp]
                if (disp == 0 && base != X86.rbp && (!target.arch.is64bit() || base != X86.r13)) {
                    // [base + indexscale]
                    // [00 reg 100][ss index base]
                    assert index != target.stackRegister : "illegal addressing mode";
                    emitByte(0x04 | regenc);
                    emitByte(scale.value << 6 | indexenc | baseenc);
                } else if (Util.is8bit(disp)) {
                    // [base + indexscale + imm8]
                    // [01 reg 100][ss index base] imm8
                    assert index != target.stackRegister : "illegal addressing mode";
                    emitByte(0x44 | regenc);
                    emitByte(scale.value << 6 | indexenc | baseenc);
                    emitByte(disp & 0xFF);
                } else {
                    // [base + indexscale + disp32]
                    // [10 reg 100][ss index base] disp32
                    assert index != target.stackRegister : "illegal addressing mode";
                    emitByte(0x84 | regenc);
                    emitByte(scale.value << 6 | indexenc | baseenc);
                    emitInt(disp);
                }
            } else if (base == target.stackRegister || (target.arch.is64bit() && base == X86.r12)) {
                // [rsp + disp]
                if (disp == 0) {
                    // [rsp]
                    // [00 reg 100][00 100 100]
                    emitByte(0x04 | regenc);
                    emitByte(0x24);
                } else if (Util.is8bit(disp)) {
                    // [rsp + imm8]
                    // [01 reg 100][00 100 100] disp8
                    emitByte(0x44 | regenc);
                    emitByte(0x24);
                    emitByte(disp & 0xFF);
                } else {
                    // [rsp + imm32]
                    // [10 reg 100][00 100 100] disp32
                    emitByte(0x84 | regenc);
                    emitByte(0x24);
                    emitInt(disp);
                }
            } else {
                // [base + disp]
                assert base != target.stackRegister && (!target.arch.is64bit() || base != X86.r12) : "illegal addressing mode";
                if (disp == 0 && base != X86.rbp && (!target.arch.is64bit() || base != X86.r13)) {
                    // [base]
                    // [00 reg base]
                    emitByte(0x00 | regenc | baseenc);
                } else if (Util.is8bit(disp)) {
                    // [base + disp8]
                    // [01 reg base] disp8
                    emitByte(0x40 | regenc | baseenc);
                    emitByte(disp & 0xFF);
                } else {
                    // [base + disp32]
                    // [10 reg base] disp32
                    emitByte(0x80 | regenc | baseenc);
                    emitInt(disp);
                }
            }
        } else {
            if (index.isValid()) {
                assert scale != Address.ScaleFactor.noScale : "inconsistent Address";
                // [indexscale + disp]
                // [00 reg 100][ss index 101] disp32
                assert index != target.stackRegister : "illegal addressing mode";
                emitByte(0x04 | regenc);
                emitByte(scale.value << 6 | indexenc | 0x05);
                emitInt(disp);
            } else if (addr == Address.InternalRelocation) {
                // [00 000 101] disp32
                // (tw) The relocation was recorded before, now just emit 0
                emitByte(0x05 | regenc);
                emitInt(0);
            } else {
                // 32bit never did this, did everything as the rip-rel/disp code above
                // [disp] ABSOLUTE
                // [00 reg 100][00 100 101] disp32
                emitByte(0x04 | regenc);
                emitByte(0x25);
                emitInt(disp);
            }
        }
    }

    static boolean isSimm32(long adjusted) {
        return adjusted == (int) adjusted;
    }

    void emitOperand32(CiRegister reg, Address adr) {
        assert reg.encoding < 8 : "no extended registers";
        assert !(adr.base.encoding >= MinEncodingNeedsRex) && !(adr.index.encoding >= MinEncodingNeedsRex) : "no extended registers";
        emitOperandHelper(reg, adr);
    }

    void emitOperand(CiRegister reg, Address adr, int ripRelativeCorrection) {
        emitOperand(reg, adr);
    }

    void emitOperand(CiRegister reg, Address adr) {
        emitOperandHelper(reg, adr);
    }

    void emitOperand(Address adr, CiRegister reg) {
        assert reg.isXMM();
        assert !(adr.base.encoding >= MinEncodingNeedsRex) && !(adr.index.encoding >= MinEncodingNeedsRex) : "no extended registers";
        emitOperandHelper(reg, adr);
    }

    void emitFarith(int b1, int b2, int i) {
        assert Util.isByte(b1) && Util.isByte(b2) : "wrong opcode";
        assert 0 <= i && i < 8 : "illegal stack offset";
        emitByte(b1);
        emitByte(b2 + i);
    }

    // Now the Assembler instruction (identical for 32/64 bits)

    public final void adcl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xD0, dst, imm32);
    }

    public final void adcl(CiRegister dst, Address src) {
        prefix(src, dst);
        emitByte(0x13);
        emitOperand(dst, src);
    }

    public final void adcl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x13, 0xC0, dst, src);
    }

    public final void addl(Address dst, int imm32) {
        prefix(dst);
        emitArithOperand(0x81, X86.rax, dst, imm32);
    }

    public final void addl(Address dst, CiRegister src) {
        prefix(dst, src);
        emitByte(0x01);
        emitOperand(src, dst);
    }

    public final void addl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xC0, dst, imm32);
    }

    public final void addl(CiRegister dst, Address src) {
        prefix(src, dst);
        emitByte(0x03);
        emitOperand(dst, src);
    }

    public final void addl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x03, 0xC0, dst, src);
    }

    public final void addrNop4() {
        // 4 bytes: NOP DWORD PTR [EAX+0]
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x40); // emitRm(cbuf, 0x1, EAXEnc, EAXEnc);
        emitByte(0); // 8-bits offset (1 byte)
    }

    public final void addrNop5() {
        // 5 bytes: NOP DWORD PTR [EAX+EAX*0+0] 8-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x44); // emitRm(cbuf, 0x1, EAXEnc, 0x4);
        emitByte(0x00); // emitRm(cbuf, 0x0, EAXEnc, EAXEnc);
        emitByte(0); // 8-bits offset (1 byte)
    }

    public final void addrNop7() {
        // 7 bytes: NOP DWORD PTR [EAX+0] 32-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x80); // emitRm(cbuf, 0x2, EAXEnc, EAXEnc);
        emitInt(0); // 32-bits offset (4 bytes)
    }

    public final void addrNop8() {
        // 8 bytes: NOP DWORD PTR [EAX+EAX*0+0] 32-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x84); // emitRm(cbuf, 0x2, EAXEnc, 0x4);
        emitByte(0x00); // emitRm(cbuf, 0x0, EAXEnc, EAXEnc);
        emitInt(0); // 32-bits offset (4 bytes)
    }

    public final void addsd(CiRegister dst, CiRegister src) {
        assert dst.isXMM() && src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x58);
        emitByte(0xC0 | encode);
    }

    public final void addsd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x58);
        emitOperand(dst, src);
    }

    public final void addss(CiRegister dst, CiRegister src) {
        assert dst.isXMM() && src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x58);
        emitByte(0xC0 | encode);
    }

    public final void addss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x58);
        emitOperand(dst, src);
    }

    public final void andl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xE0, dst, imm32);
    }

    public final void andl(CiRegister dst, Address src) {
        prefix(src, dst);
        emitByte(0x23);
        emitOperand(dst, src);
    }

    public final void andl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x23, 0xC0, dst, src);
    }

    public final void andpd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x54);
        emitOperand(dst, src);
    }

    public final void bsfl(CiRegister dst, CiRegister src) {
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBC);
        emitByte(0xC0 | encode);
    }

    public final void bsrl(CiRegister dst, CiRegister src) {
        assert !target.supportsLzcnt() : "encoding is treated as LZCNT";
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    public final void bswapl(CiRegister reg) { // bswap
        int encode = prefixAndEncode(reg.encoding);
        emitByte(0x0F);
        emitByte(0xC8 | encode);
    }

    public final void call(Label l) {
        if (l.isBound()) {
            int longSize = 5;
            int offs = Util.safeToInt(target(l) - codeBuffer.position());
            assert offs <= 0 : "assembler error";

            // 1110 1000 #32-bit disp
            emitByte(0xE8);
            emitInt((offs - longSize));
        } else {

            // 1110 1000 #32-bit disp
            l.addPatchAt(codeBuffer.position());
            emitByte(0xE8);
            emitInt(0);
        }
    }

    public final void call(CiRegister dst, RiMethod method, boolean[] stackReferenceMap) {

        recordIndirectCall(codeBuffer.position(), method, stackReferenceMap);
        // this may be true but dbx disassembles it as if it
        // were 32bits...
        // int encode = prefixAndEncode(dst.encoding());
        // if (offset() != x) assert dst.encoding() >= 8 : "what?";
        int encode = prefixqAndEncode(dst.encoding);

        emitByte(0xFF);
        emitByte(0xD0 | encode);
    }

    public final void call(RiMethod method, boolean[] stackReferenceMap) {
        recordDirectCall(codeBuffer.position(), method, stackReferenceMap);
        emitByte(0xE8);
        emitInt(0);

    }
    public final void call(Address adr, RiMethod method, boolean[] stackReferenceMap) {

        recordIndirectCall(codeBuffer.position(), method, stackReferenceMap);
        prefix(adr);
        emitByte(0xFF);
        emitOperand(X86.rdx, adr);

    }

    protected final void emitGlobalStubCall(Object globalStubID, CodeEmitInfo info) {
        assert !(globalStubID instanceof GlobalStub);

        int position = codeBuffer.position();
        if (info == null) {
            recordGlobalStubCall(position, globalStubID, null, null);
        } else {
            recordGlobalStubCall(position, globalStubID, info.oopMap.registerMap(), info.oopMap.stackMap());
        }

        emitByte(0xE8);
        emitInt(0);
    }

    public final void callRuntime(CiRuntimeCall runtimeCall) {
        callRuntime(runtimeCall, null);
    }

    /**
     * Emits a call to the runtime. It generates the bytes for the call, fills in 0 for the destination address and
     * records the position as a relocation to the runtime.
     *
     * @param runtimeCall
     *            the destination of the call
     */
    public void callRuntime(CiRuntimeCall runtimeCall, RiMethod method) {
        // TODO: Fill in reference map correctly!
        recordRuntimeCall(codeBuffer.position(), runtimeCall, new boolean[targetMethod.frameSize() / target.arch.wordSize]);
        emitByte(0xE8);
        emitInt(0);
    }

    /**
     * Emits a direct call to a method. It generates the bytes for the call, fills in 0 for the destination address and
     * records the position as a relocation to the runtime.
     *
     * @param method the method to call
     * @param stackRefMap the reference map at the call
     */
    public void callMethodDirect(RiMethod method, boolean[] stackRefMap) {
        recordDirectCall(codeBuffer.position(), method, stackRefMap);
        emitByte(0xE8);
        emitInt(0);
    }

    public final void cdql() {
        emitByte(0x99);
    }

    public final void cmovl(Condition cc, CiRegister dst, CiRegister src) {
        assert target.arch.is64bit() || target.supportsCmov() : "illegal instruction";
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitByte(0xC0 | encode);
    }

    public final void cmovl(Condition cc, CiRegister dst, Address src) {
        assert target.arch.is64bit() || target.supportsCmov() : "illegal instruction";
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitOperand(dst, src);
    }

    public final void cmpb(Address dst, int imm8) {
        prefix(dst);
        emitByte(0x80);
        emitOperand(X86.rdi, dst, 1);
        emitByte(imm8);
    }

    public final void cmpl(Address dst, int imm32) {
        prefix(dst);
        emitByte(0x81);
        emitOperand(X86.rdi, dst, 4);
        emitInt(imm32);
    }

    public final void cmpl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xF8, dst, imm32);
    }

    public final void cmpl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x3B, 0xC0, dst, src);
    }

    public final void cmpl(CiRegister dst, Address src) {
        prefix(src, dst);
        emitByte(0x3B);
        emitOperand(dst, src);
    }

    private static final int MinEncodingNeedsRex = 8;

    public final void cmpw(Address dst, int imm16) {
        assert !(dst.base.encoding >= MinEncodingNeedsRex) && !(dst.index.encoding >= MinEncodingNeedsRex) : "no extended registers";
        emitByte(0x66);
        emitByte(0x81);
        emitOperand(X86.rdi, dst, 2);
        emitShort(imm16);
    }

    // The 32-bit cmpxchg compares the value at adr with the contents of X86.rax,
    // and stores reg into adr if so; otherwise, the value at adr is loaded into X86.rax,.
    // The ZF is set if the compared values were equal, and cleared otherwise.
    public final void cmpxchgl(CiRegister reg, Address adr) { // cmpxchg
        if ((C1XOptions.Atomics & 2) != 0) {
            // caveat: no instructionmark, so this isn't relocatable.
            // Emit a synthetic, non-atomic, CAS equivalent.
            // Beware. The synthetic form sets all ICCs, not just ZF.
            // cmpxchg r,[m] is equivalent to X86.rax, = CAS (m, X86.rax, r)
            cmpl(X86.rax, adr);
            movl(X86.rax, adr);
            if (reg != X86.rax) {
                Label l = new Label();
                jcc(Condition.notEqual, l);
                movl(adr, reg);
                bind(l);
            }
        } else {

            prefix(adr, reg);
            emitByte(0x0F);
            emitByte(0xB1);
            emitOperand(reg, adr);
        }
    }

    public final void comisd(CiRegister dst, Address src) {
        assert dst.isXMM();
        // NOTE: dbx seems to decode this as comiss even though the
        // 0x66 is there. Strangly ucomisd comes out correct
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0x66);
        comiss(dst, src);
    }

    public final void comiss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();

        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x2F);
        emitOperand(dst, src);
    }

    public final void cvtdq2pd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xE6);
        emitByte(0xC0 | encode);
    }

    public final void cvtdq2ps(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5B);
        emitByte(0xC0 | encode);
    }

    public final void cvtsd2ss(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5A);
        emitByte(0xC0 | encode);
    }

    public final void cvtsi2sdl(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    public final void cvtsi2ssl(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    public final void cvtss2sd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5A);
        emitByte(0xC0 | encode);
    }

    public final void cvttsd2sil(CiRegister dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    public final void cvttss2sil(CiRegister dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    public final void decl(Address dst) {
        // Don't use it directly. Use Macrodecrement() instead.
        prefix(dst);
        emitByte(0xFF);
        emitOperand(X86.rcx, dst);
    }

    public final void divsd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x5E);
        emitOperand(dst, src);
    }

    public final void divsd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5E);
        emitByte(0xC0 | encode);
    }

    public final void divss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x5E);
        emitOperand(dst, src);
    }

    public final void divss(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5E);
        emitByte(0xC0 | encode);
    }

    public final void emms() {
        assert target.supportsMmx();
        emitByte(0x0F);
        emitByte(0x77);
    }

    public final void hlt() {
        emitByte(0xF4);
    }

    public final void idivl(CiRegister src) {
        int encode = prefixAndEncode(src.encoding);
        emitByte(0xF7);
        emitByte(0xF8 | encode);
    }

    public final void imull(CiRegister dst, CiRegister src) {
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xAF);
        emitByte(0xC0 | encode);
    }

    public final void imull(CiRegister dst, CiRegister src, int value) {
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        if (Util.is8bit(value)) {
            emitByte(0x6B);
            emitByte(0xC0 | encode);
            emitByte(value);
        } else {
            emitByte(0x69);
            emitByte(0xC0 | encode);
            emitInt(value);
        }
    }

    public final void incl(Address dst) {
        // Don't use it directly. Use Macroincrement() instead.
        prefix(dst);
        emitByte(0xFF);
        emitOperand(X86.rax, dst);
    }

    public final void jcc(Condition cc, Label l) {

        assert (0 <= cc.value) && (cc.value < 16) : "illegal cc";
        if (l.isBound()) {
            int dst = target(l);

            int shortSize = 2;
            int longSize = 6;
            long offs = dst - codeBuffer.position();
            if (Util.is8bit(offs - shortSize)) {
                // 0111 tttn #8-bit disp
                emitByte(0x70 | cc.value);
                emitByte((int) ((offs - shortSize) & 0xFF));
            } else {
                // 0000 1111 1000 tttn #32-bit disp
                assert isSimm32(offs - longSize) : "must be 32bit offset (call4)";
                emitByte(0x0F);
                emitByte(0x80 | cc.value);
                emitInt((int) (offs - longSize));
            }
        } else {
            // Note: could eliminate cond. jumps to this jump if condition
            // is the same however, seems to be rather unlikely case.
            // Note: use jccb() if label to be bound is very close to get
            // an 8-bit displacement
            l.addPatchAt(codeBuffer.position());
            emitByte(0x0F);
            emitByte(0x80 | cc.value);
            emitInt(0);
        }

    }

    public final void jccb(Condition cc, Label l) {
        if (l.isBound()) {
            int shortSize = 2;
            int entry = target(l);
            assert Util.is8bit(entry - (codeBuffer.position() + shortSize)) : "Dispacement too large for a short jmp";
            long offs = entry - codeBuffer.position();
            // 0111 tttn #8-bit disp
            emitByte(0x70 | cc.value);
            emitByte((int) ((offs - shortSize) & 0xFF));
        } else {

            l.addPatchAt(codeBuffer.position());
            emitByte(0x70 | cc.value);
            emitByte(0);
        }
    }

    public final void jmp(Address adr) {
        prefix(adr);
        emitByte(0xFF);
        emitOperand(X86.rsp, adr);
    }

    public final void jmp(Label l) {
        if (l.isBound()) {
            int entry = target(l);

            int shortSize = 2;
            int longSize = 5;
            long offs = entry - codeBuffer.position();
            if (Util.is8bit(offs - shortSize)) {
                emitByte(0xEB);
                emitByte((int) ((offs - shortSize) & 0xFF));
            } else {
                emitByte(0xE9);
                emitInt((int) (offs - longSize));
            }
        } else {
            // By default, forward jumps are always 32-bit displacements, since
            // we can't yet know where the label will be bound. If you're sure that
            // the forward jump will not run beyond 256 bytes, use jmpb to
            // force an 8-bit displacement.

            l.addPatchAt(codeBuffer.position());
            emitByte(0xE9);
            emitInt(0);
        }
    }

    public final void jmp(CiRegister entry) {
        int encode = prefixAndEncode(entry.encoding);
        emitByte(0xFF);
        emitByte(0xE0 | encode);
    }

    public final void jmpb(Label l) {
        if (l.isBound()) {
            int shortSize = 2;
            int entry = target(l);
            assert Util.is8bit((entry - codeBuffer.position()) + shortSize) : "Dispacement too large for a short jmp";
            long offs = entry - codeBuffer.position();
            emitByte(0xEB);
            emitByte((int) ((offs - shortSize) & 0xFF));
        } else {

            l.addPatchAt(codeBuffer.position());
            emitByte(0xEB);
            emitByte(0);
        }
    }

    public final void leal(CiRegister dst, Address src) {
        if (target.arch.is64bit()) {
            emitByte(0x67); // addr32
            prefix(src, dst);
        }
        emitByte(0x8D);
        emitOperand(dst, src);
    }

    public final void lock() {
        if ((C1XOptions.Atomics & 1) != 0) {
            // Emit either nothing, a NOP, or a NOP: prefix
            emitByte(0x90);
        } else {
            emitByte(0xF0);
        }
    }

    public final void lzcntl(CiRegister dst, CiRegister src) {
        assert target.supportsLzcnt() : "encoding is treated as BSR";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    // Emit mfence instruction
    public final void mfence() {
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0x0F);
        emitByte(0xAE);
        emitByte(0xF0);
    }

    public final void mov(CiRegister dst, CiRegister src) {
        if (target.arch.is64bit()) {
            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    public final void movapd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        int dstenc = dst.encoding;
        int srcenc = src.encoding;
        emitByte(0x66);
        if (dstenc < 8) {
            if (srcenc >= 8) {
                emitByte(Prefix.REXB);
                srcenc -= 8;
            }
        } else {
            if (srcenc < 8) {
                emitByte(Prefix.REXR);
            } else {
                emitByte(Prefix.REXRB);
                srcenc -= 8;
            }
            dstenc -= 8;
        }
        emitByte(0x0F);
        emitByte(0x28);
        emitByte(0xC0 | dstenc << 3 | srcenc);
    }

    public final void movaps(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE() : "unsupported";
        int dstenc = dst.encoding;
        int srcenc = src.encoding;
        if (dstenc < 8) {
            if (srcenc >= 8) {
                emitByte(Prefix.REXB);
                srcenc -= 8;
            }
        } else {
            if (srcenc < 8) {
                emitByte(Prefix.REXR);
            } else {
                emitByte(Prefix.REXRB);
                srcenc -= 8;
            }
            dstenc -= 8;
        }
        emitByte(0x0F);
        emitByte(0x28);
        emitByte(0xC0 | dstenc << 3 | srcenc);
    }

    public final void movb(CiRegister dst, Address src) {
        assert target.arch.is64bit() || dst.isByte() : "must have byte register";
        prefix(src, dst); // , true)
        emitByte(0x8A);
        emitOperand(dst, src);
    }

    public final void movb(Address dst, int imm8) {
        prefix(dst);
        emitByte(0xC6);
        emitOperand(X86.rax, dst, 1);
        emitByte(imm8);
    }

    public final void movb(Address dst, CiRegister src) {
        assert src.isByte() : "must have byte register";
        prefix(dst, src); // , true)
        emitByte(0x88);
        emitOperand(src, dst);
    }

    public final void movdl(CiRegister dst, CiRegister src) {
        if (dst.isXMM()) {
            assert dst.isXMM();
            assert !src.isXMM() : "does this hold?";
            assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
            emitByte(0x66);
            int encode = prefixAndEncode(dst.encoding, src.encoding);
            emitByte(0x0F);
            emitByte(0x6E);
            emitByte(0xC0 | encode);
        } else if (src.isXMM()) {
            assert src.isXMM();
            assert !dst.isXMM();
            assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
            emitByte(0x66);
            // swap src/dst to get correct prefix
            int encode = prefixAndEncode(src.encoding, dst.encoding);
            emitByte(0x0F);
            emitByte(0x7E);
            emitByte(0xC0 | encode);
        }
    }

    public final void movdqa(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x6F);
        emitOperand(dst, src);
    }

    public final void movdqa(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0x66);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x6F);
        emitByte(0xC0 | encode);
    }

    public final void movdqa(Address dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0x66);
        prefix(dst, src);
        emitByte(0x0F);
        emitByte(0x7F);
        emitOperand(src, dst);
    }

    public final void movdqu(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x6F);
        emitOperand(dst, src);
    }

    public final void movdqu(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";

        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x6F);
        emitByte(0xC0 | encode);
    }

    public final void movdqu(Address dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";

        emitByte(0xF3);
        prefix(dst, src);
        emitByte(0x0F);
        emitByte(0x7F);
        emitOperand(src, dst);
    }

    public final void movl(CiRegister dst, int imm32) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xB8 | encode);
        emitInt(imm32);
    }

    public final void movl(CiRegister dst, CiRegister src) {
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x8B);
        emitByte(0xC0 | encode);
    }

    public final void movl(CiRegister dst, Address src) {
        prefix(src, dst);
        emitByte(0x8B);
        emitOperand(dst, src);
    }

    public final void movl(Address dst, int imm32) {
        prefix(dst);
        emitByte(0xC7);
        emitOperand(X86.rax, dst, 4);
        emitInt(imm32);
    }

    public final void movl(Address dst, CiRegister src) {
        prefix(dst, src);
        emitByte(0x89);
        emitOperand(src, dst);
    }

    // New cpus require to use movsd and movss to avoid partial register stall
    // when loading from memory. But for old Opteron use movlpd instead of movsd.
    // The selection is done in Macromovdbl() and movflt().
    public final void movlpd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";

        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x12);
        emitOperand(dst, src);

    }

    public final void movq(CiRegister dst, Address src) {
        if (dst.isMMX()) {
            assert dst.isMMX();
            assert target.supportsMMX() : "unsupported";
            emitByte(0x0F);
            emitByte(0x6F);
            emitOperand(dst, src);
        } else if (dst.isXMM()) {
            assert dst.isXMM();
            assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
            emitByte(0xF3);
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0x7E);
            emitOperand(dst, src);
        } else {
            prefixq(src, dst);
            emitByte(0x8B);
            emitOperand(dst, src);
        }
    }

    public final void movq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x8B);
        emitByte(0xC0 | encode);
    }

    public final void movq(Address dst, CiRegister src) {
        if (src.isMMX()) {
            assert src.isMMX();
            assert target.supportsMMX() : "unsupported";
            emitByte(0x0F);
            emitByte(0x7F);
            // workaround gcc (3.2.1-7a) bug
            // In that version of gcc with only an emitOperand(MMX, Address)
            // gcc will tail jump and try and reverse the parameters completely
            // obliterating dst in the process. By having a version available
            // that doesn't need to swap the args at the tail jump the bug is
            // avoided.
            emitOperand(dst, src);
        } else if (src.isXMM()) {
            assert src.isXMM();
            assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";

            emitByte(0x66);
            prefixq(dst, src);
            emitByte(0x0F);
            emitByte(0xD6);
            emitOperand(src, dst);
        } else {

            prefixq(dst, src);
            emitByte(0x89);
            emitOperand(src, dst);
        }
    }

    public final void movsbl(CiRegister dst, Address src) { // movsxb
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0xBE);
        emitOperand(dst, src);
    }

    public final void movsbl(CiRegister dst, CiRegister src) { // movsxb
        assert target.arch.is64bit() || src.isByte() : "must have byte register";
        int encode = prefixAndEncode(dst.encoding, src.encoding, true);
        emitByte(0x0F);
        emitByte(0xBE);
        emitByte(0xC0 | encode);
    }

    public final void movsd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x10);
        emitByte(0xC0 | encode);
    }

    public final void movsd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0xF2);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x10);
        emitOperand(dst, src);
    }

    public final void movsd(Address dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0xF2);
        prefix(dst, src);
        emitByte(0x0F);
        emitByte(0x11);
        emitOperand(src, dst);
    }

    public final void movss(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE() : "unsupported";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x10);
        emitByte(0xC0 | encode);
    }

    public final void movss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE() : "unsupported";
        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x10);
        emitOperand(dst, src);
    }

    public final void movss(Address dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE() : "unsupported";
        emitByte(0xF3);
        prefix(dst, src);
        emitByte(0x0F);
        emitByte(0x11);
        emitOperand(src, dst);
    }

    public final void movswl(CiRegister dst, Address src) {
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0xBF);
        emitOperand(dst, src);
    }

    public final void movswl(CiRegister dst, CiRegister src) { // movsxw
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBF);
        emitByte(0xC0 | encode);
    }

    public final void movw(Address dst, int imm16) {
        emitByte(0x66); // switch to 16-bit mode
        prefix(dst);
        emitByte(0xC7);
        emitOperand(X86.rax, dst, 2);
        emitShort(imm16);
    }

    public final void movw(CiRegister dst, Address src) {
        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x8B);
        emitOperand(dst, src);
    }

    public final void movw(Address dst, CiRegister src) {
        emitByte(0x66);
        prefix(dst, src);
        emitByte(0x89);
        emitOperand(src, dst);
    }

    public final void movzbl(CiRegister dst, Address src) { // movzxb
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0xB6);
        emitOperand(dst, src);
    }

    public final void movzbl(CiRegister dst, CiRegister src) { // movzxb
        assert target.arch.is64bit() || src.isByte() : "must have byte register";
        int encode = prefixAndEncode(dst.encoding, src.encoding, true);
        emitByte(0x0F);
        emitByte(0xB6);
        emitByte(0xC0 | encode);
    }

    public final void movzwl(CiRegister dst, Address src) { // movzxw
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0xB7);
        emitOperand(dst, src);
    }

    public final void movzwl(CiRegister dst, CiRegister src) { // movzxw
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xB7);
        emitByte(0xC0 | encode);
    }

    public final void mull(Address src) {
        prefix(src);
        emitByte(0xF7);
        emitOperand(X86.rsp, src);
    }

    public final void mull(CiRegister src) {
        int encode = prefixAndEncode(src.encoding);
        emitByte(0xF7);
        emitByte(0xE0 | encode);
    }

    public final void mulsd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";
        emitByte(0xF2);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x59);
        emitOperand(dst, src);
    }

    public final void mulsd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2() : "unsupported";

        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x59);
        emitByte(0xC0 | encode);
    }

    public final void mulss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE() : "unsupported";

        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x59);
        emitOperand(dst, src);
    }

    public final void mulss(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE() : "unsupported";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x59);
        emitByte(0xC0 | encode);
    }

    public final void negl(CiRegister dst) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xF7);
        emitByte(0xD8 | encode);
    }

    @Override
    public final void nop() {
        nop(1);
    }

    void nop(int i) {

        if (C1XOptions.UseNormalNop) {
            assert i > 0 : " ";
            // The fancy nops aren't currently recognized by debuggers making it a
            // pain to disassemble code while debugging. If assert are on clearly
            // speed is not an issue so simply use the single byte traditional nop
            // to do alignment.

            for (; i > 0; i--) {
                emitByte(0x90);
            }
            return;
        }

        if (C1XOptions.UseAddressNop && target.isIntel()) {
            //
            // Using multi-bytes nops "0x0F 0x1F [Address]" for Intel
            // 1: 0x90
            // 2: 0x66 0x90
            // 3: 0x66 0x66 0x90 (don't use "0x0F 0x1F 0x00" - need patching safe padding)
            // 4: 0x0F 0x1F 0x40 0x00
            // 5: 0x0F 0x1F 0x44 0x00 0x00
            // 6: 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 7: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 8: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 9: 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 10: 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 11: 0x66 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00

            // The rest coding is Intel specific - don't use consecutive Address nops

            // 12: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x66 0x66 0x66 0x90
            // 13: 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x66 0x66 0x66 0x90
            // 14: 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x66 0x66 0x66 0x90
            // 15: 0x66 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x66 0x66 0x66 0x90

            while (i >= 15) {
                // For Intel don't generate consecutive addess nops (mix with regular nops)
                i -= 15;
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                addrNop8();
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                emitByte(0x90); // nop
            }
            switch (i) {
                case 14:
                    emitByte(0x66); // size prefix
                    // fall through
                case 13:
                    emitByte(0x66); // size prefix
                    // fall through
                case 12:
                    addrNop8();
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x90); // nop
                    break;
                case 11:
                    emitByte(0x66); // size prefix
                    // fall through
                case 10:
                    emitByte(0x66); // size prefix
                    // fall through
                case 9:
                    emitByte(0x66); // size prefix
                    // fall through
                case 8:
                    addrNop8();
                    break;
                case 7:
                    addrNop7();
                    break;
                case 6:
                    emitByte(0x66); // size prefix
                    // fall through
                case 5:
                    addrNop5();
                    break;
                case 4:
                    addrNop4();
                    break;
                case 3:
                    // Don't use "0x0F 0x1F 0x00" - need patching safe padding
                    emitByte(0x66); // size prefix
                    // fall through
                case 2:
                    emitByte(0x66); // size prefix
                    // fall through
                case 1:
                    emitByte(0x90); // nop
                    break;
                default:
                    assert i == 0;
            }
            return;
        }
        if (C1XOptions.UseAddressNop && target.isAmd()) {
            //
            // Using multi-bytes nops "0x0F 0x1F [Address]" for AMD.
            // 1: 0x90
            // 2: 0x66 0x90
            // 3: 0x66 0x66 0x90 (don't use "0x0F 0x1F 0x00" - need patching safe padding)
            // 4: 0x0F 0x1F 0x40 0x00
            // 5: 0x0F 0x1F 0x44 0x00 0x00
            // 6: 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 7: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 8: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 9: 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 10: 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 11: 0x66 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00

            // The rest coding is AMD specific - use consecutive Address nops

            // 12: 0x66 0x0F 0x1F 0x44 0x00 0x00 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 13: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 14: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 15: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 16: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // Size prefixes (0x66) are added for larger sizes

            while (i >= 22) {
                i -= 11;
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                addrNop8();
            }
            // Generate first nop for size between 21-12
            switch (i) {
                case 21:
                    i -= 1;
                    emitByte(0x66); // size prefix
                    // fall through
                case 20:
                    // fall through
                case 19:
                    i -= 1;
                    emitByte(0x66); // size prefix
                    // fall through
                case 18:
                    // fall through
                case 17:
                    i -= 1;
                    emitByte(0x66); // size prefix
                    // fall through
                case 16:
                    // fall through
                case 15:
                    i -= 8;
                    addrNop8();
                    break;
                case 14:
                case 13:
                    i -= 7;
                    addrNop7();
                    break;
                case 12:
                    i -= 6;
                    emitByte(0x66); // size prefix
                    addrNop5();
                    break;
                default:
                    assert i < 12;
            }

            // Generate second nop for size between 11-1
            switch (i) {
                case 11:
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 10:
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 9:
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 8:
                    addrNop8();
                    break;
                case 7:
                    addrNop7();
                    break;
                case 6:
                    emitByte(0x66); // size prefix
                    addrNop5();
                    break;
                case 5:
                    addrNop5();
                    break;
                case 4:
                    addrNop4();
                    break;
                case 3:
                    // Don't use "0x0F 0x1F 0x00" - need patching safe padding
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x90); // nop
                    break;
                case 2:
                    emitByte(0x66); // size prefix
                    emitByte(0x90); // nop
                    break;
                case 1:
                    emitByte(0x90); // nop
                    break;
                default:
                    assert i == 0;
            }
            return;
        }

        // Using nops with size prefixes "0x66 0x90".
        // From AMD Optimization Guide:
        // 1: 0x90
        // 2: 0x66 0x90
        // 3: 0x66 0x66 0x90
        // 4: 0x66 0x66 0x66 0x90
        // 5: 0x66 0x66 0x90 0x66 0x90
        // 6: 0x66 0x66 0x90 0x66 0x66 0x90
        // 7: 0x66 0x66 0x66 0x90 0x66 0x66 0x90
        // 8: 0x66 0x66 0x66 0x90 0x66 0x66 0x66 0x90
        // 9: 0x66 0x66 0x90 0x66 0x66 0x90 0x66 0x66 0x90
        // 10: 0x66 0x66 0x66 0x90 0x66 0x66 0x90 0x66 0x66 0x90
        //
        while (i > 12) {
            i -= 4;
            emitByte(0x66); // size prefix
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90); // nop
        }
        // 1 - 12 nops
        if (i > 8) {
            if (i > 9) {
                i -= 1;
                emitByte(0x66);
            }
            i -= 3;
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90);
        }
        // 1 - 8 nops
        if (i > 4) {
            if (i > 6) {
                i -= 1;
                emitByte(0x66);
            }
            i -= 3;
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90);
        }
        switch (i) {
            case 4:
                emitByte(0x66);
                emitByte(0x66);
                emitByte(0x66);
                emitByte(0x90);
                break;
            case 3:
                emitByte(0x66);
                emitByte(0x66);
                emitByte(0x90);
                break;
            case 2:
                emitByte(0x66);
                emitByte(0x90);
                break;
            case 1:
                emitByte(0x90);
                break;
            default:
                assert i == 0;
        }
    }

    public final void notl(CiRegister dst) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xF7);
        emitByte(0xD0 | encode);
    }

    public final void orl(Address dst, int imm32) {

        prefix(dst);
        emitByte(0x81);
        emitOperand(X86.rcx, dst, 4);
        emitInt(imm32);

    }

    public final void orl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xC8, dst, imm32);
    }

    public final void orl(CiRegister dst, Address src) {

        prefix(src, dst);
        emitByte(0x0B);
        emitOperand(dst, src);

    }

    public final void orl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x0B, 0xC0, dst, src);
    }

    public final void pcmpestri(CiRegister dst, Address src, int imm8) {
        assert dst.isXMM();
        assert target.supportsSse42() : "";

        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x3A);
        emitByte(0x61);
        emitOperand(dst, src);
        emitByte(imm8);

    }

    public final void pcmpestri(CiRegister dst, CiRegister src, int imm8) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.supportsSse42() : "";

        emitByte(0x66);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x3A);
        emitByte(0x61);
        emitByte(0xC0 | encode);
        emitByte(imm8);
    }

    // generic
    public final void pop(CiRegister dst) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0x58 | encode);
    }

    public final void popcntl(CiRegister dst, Address src) {
        assert target.supportsPopcnt() : "must support";
        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0xB8);
        emitOperand(dst, src);
    }

    public final void popcntl(CiRegister dst, CiRegister src) {
        assert target.supportsPopcnt() : "must support";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xB8);
        emitByte(0xC0 | encode);
    }

    public final void popf() {
        emitByte(0x9D);
    }

    public final void popl(Address dst) {
        // NOTE: this will adjust stack by 8byte on 64bits
        prefix(dst);
        emitByte(0x8F);
        emitOperand(X86.rax, dst);
    }

    public final void prefetchPrefix(Address src) {
        prefix(src);
        emitByte(0x0F);
    }

    public final void prefetchnta(Address src) {
        assert target.arch.is64bit() || target.supportsSSE2();
        prefetchPrefix(src);
        emitByte(0x18);
        emitOperand(X86.rax, src); // 0, src
    }

    public final void prefetchr(Address src) {
        assert target.arch.is64bit() || target.supports3DNOW();
        prefetchPrefix(src);
        emitByte(0x0D);
        emitOperand(X86.rax, src); // 0, src
    }

    public final void prefetcht0(Address src) {
        assert target.arch.is64bit() || target.supportsSSE();
        prefetchPrefix(src);
        emitByte(0x18);
        emitOperand(X86.rcx, src); // 1, src

    }

    public final void prefetcht1(Address src) {
        assert target.arch.is64bit() || target.supportsSSE();
        prefetchPrefix(src);
        emitByte(0x18);
        emitOperand(X86.rdx, src); // 2, src
    }

    public final void prefetcht2(Address src) {
        assert target.arch.is64bit() || target.supportsSSE();
        prefetchPrefix(src);
        emitByte(0x18);
        emitOperand(X86.rbx, src); // 3, src
    }

    public final void prefetchw(Address src) {
        assert target.arch.is64bit() || target.supports3DNOW();
        prefetchPrefix(src);
        emitByte(0x0D);
        emitOperand(X86.rcx, src); // 1, src
    }

    public final void pshufd(CiRegister dst, CiRegister src, int mode) {
        assert dst.isXMM();
        assert src.isXMM();
        assert Util.isByte(mode) : "invalid value";
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0x66);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x70);
        emitByte(0xC0 | encode);
        emitByte(mode & 0xFF);
    }

    public final void pshufd(CiRegister dst, Address src, int mode) {
        assert dst.isXMM();
        assert Util.isByte(mode) : "invalid value";
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x70);
        emitOperand(dst, src);
        emitByte(mode & 0xFF);

    }

    public final void pshuflw(CiRegister dst, CiRegister src, int mode) {
        assert dst.isXMM();
        assert src.isXMM();
        assert Util.isByte(mode) : "invalid value";
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x70);
        emitByte(0xC0 | encode);
        emitByte(mode & 0xFF);
    }

    public final void pshuflw(CiRegister dst, Address src, int mode) {
        assert dst.isXMM();
        assert Util.isByte(mode) : "invalid value";
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0xF2);
        prefix(src, dst); // QQ new
        emitByte(0x0F);
        emitByte(0x70);
        emitOperand(dst, src);
        emitByte(mode & 0xFF);
    }

    public final void psrlq(CiRegister dst, int shift) {
        assert dst.isXMM();
        // HMM Table D-1 says sse2 or mmx
        assert target.arch.is64bit() || target.supportsSSE();

        int encode = prefixqAndEncode(X86.xmm2.encoding, dst.encoding);
        emitByte(0x66);
        emitByte(0x0F);
        emitByte(0x73);
        emitByte(0xC0 | encode);
        emitByte(shift);
    }

    public final void ptest(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.supportsSse41() : "";

        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x38);
        emitByte(0x17);
        emitOperand(dst, src);

    }

    public final void ptest(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.supportsSse41() : "";

        emitByte(0x66);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x38);
        emitByte(0x17);
        emitByte(0xC0 | encode);
    }

    public final void punpcklbw(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0x66);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x60);
        emitByte(0xC0 | encode);
    }

    public final void push(int imm32) {
        // in 64bits we push 64bits onto the stack but only
        // take a 32bit immediate
        emitByte(0x68);
        emitInt(imm32);
    }

    public final void push(CiRegister src) {
        int encode = prefixAndEncode(src.encoding);
        emitByte(0x50 | encode);
    }

    public final void pushf() {
        emitByte(0x9C);
    }

    public final void pushl(Address src) {
        // Note this will push 64bit on 64bit

        prefix(src);
        emitByte(0xFF);
        emitOperand(X86.rsi, src);

    }

    public final void pxor(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0xEF);
        emitOperand(dst, src);

    }

    public final void pxor(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0x66);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xEF);
        emitByte(0xC0 | encode);

    }

    public final void rcll(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8) : "illegal shift count";
        int encode = prefixAndEncode(dst.encoding);
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xD0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xD0 | encode);
            emitByte(imm8);
        }
    }

    // copies data from [esi] to [edi] using X86.rcx pointer sized words
    // generic
    public final void repMov() {
        emitByte(0xF3);
        // MOVSQ
        if (target.arch.is64bit()) {
            emitByte(Prefix.REXW);
        }
        emitByte(0xA5);
    }

    // sets X86.rcx pointer sized words with X86.rax, value at [edi]
    // generic
    public final void repSet() { // repSet
        emitByte(0xF3);
        // STOSQ
        if (target.arch.is64bit()) {
            emitByte(Prefix.REXW);
        }
        emitByte(0xAB);
    }

    // scans X86.rcx pointer sized words at [edi] for occurance of X86.rax,
    // generic
    public final void repneScan() { // repneScan
        emitByte(0xF2);
        // SCASQ
        if (target.arch.is64bit()) {
            emitByte(Prefix.REXW);
        }
        emitByte(0xAF);
    }

    // scans X86.rcx 4 byte words at [edi] for occurance of X86.rax,
    // generic
    public final void repneScanl() { // repneScan
        assert target.arch.is64bit();
        emitByte(0xF2);
        // SCASL
        emitByte(0xAF);
    }

    public final void ret(int imm16) {
        if (imm16 == 0) {
            emitByte(0xC3);
        } else {
            emitByte(0xC2);
            emitShort(imm16);
        }
    }

    public final void sahf() {
        // Not supported in 64bit mode
        if (target.arch.is64bit()) {
            Util.shouldNotReachHere();
        }
        emitByte(0x9E);
    }

    public final void sarl(CiRegister dst, int imm8) {
        int encode = prefixAndEncode(dst.encoding);
        assert Util.isShiftCount(imm8) : "illegal shift count";
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xF8 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xF8 | encode);
            emitByte(imm8);
        }
    }

    public final void sarl(CiRegister dst) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xD3);
        emitByte(0xF8 | encode);
    }

    public final void sbbl(Address dst, int imm32) {

        prefix(dst);
        emitArithOperand(0x81, X86.rbx, dst, imm32);

    }

    public final void sbbl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xD8, dst, imm32);
    }

    public final void sbbl(CiRegister dst, Address src) {

        prefix(src, dst);
        emitByte(0x1B);
        emitOperand(dst, src);

    }

    public final void sbbl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x1B, 0xC0, dst, src);
    }

    public final void setb(Condition cc, CiRegister dst) {
        assert 0 <= cc.value && cc.value < 16 : "illegal cc";
        int encode = prefixAndEncode(dst.encoding, true);
        emitByte(0x0F);
        emitByte(0x90 | cc.value);
        emitByte(0xC0 | encode);
    }

    public final void shll(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8) : "illegal shift count";
        int encode = prefixAndEncode(dst.encoding);
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xE0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xE0 | encode);
            emitByte(imm8);
        }
    }

    public final void shll(CiRegister dst) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xD3);
        emitByte(0xE0 | encode);
    }

    public final void shrl(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8) : "illegal shift count";
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xC1);
        emitByte(0xE8 | encode);
        emitByte(imm8);
    }

    public final void shrl(CiRegister dst) {
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xD3);
        emitByte(0xE8 | encode);
    }

    // copies a single word from [esi] to [edi]
    public final void smovl() {
        emitByte(0xA5);
    }

    public final void sqrtsd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        // HMM Table D-1 says sse2
        // assert target.arch.is64bit() || target.supportsSSE();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x51);
        emitByte(0xC0 | encode);
    }

    public final void subl(Address dst, int imm32) {

        prefix(dst);
        if (Util.is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(X86.rbp, dst, 1);
            emitByte(imm32 & 0xFF);
        } else {
            emitByte(0x81);
            emitOperand(X86.rbp, dst, 4);
            emitInt(imm32);
        }

    }

    public final void subl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xE8, dst, imm32);
    }

    public final void subl(Address dst, CiRegister src) {

        prefix(dst, src);
        emitByte(0x29);
        emitOperand(src, dst);

    }

    public final void subl(CiRegister dst, Address src) {

        prefix(src, dst);
        emitByte(0x2B);
        emitOperand(dst, src);

    }

    public final void subl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x2B, 0xC0, dst, src);
    }

    public final void subsd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5C);
        emitByte(0xC0 | encode);
    }

    public final void subsd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0xF2);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x5C);
        emitOperand(dst, src);

    }

    public final void subss(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x5C);
        emitByte(0xC0 | encode);
    }

    public final void subss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();

        emitByte(0xF3);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x5C);
        emitOperand(dst, src);

    }

    public final void testb(CiRegister dst, int imm8) {
        assert target.arch.is64bit() || dst.isByte() : "must have byte register";
        prefixAndEncode(dst.encoding, true);
        emitArithB(0xF6, 0xC0, dst, imm8);
    }

    public final void testl(CiRegister dst, int imm32) {
        // not using emitArith because test
        // doesn't support sign-extension of
        // 8bit operands
        int encode = dst.encoding;
        if (encode == 0) {
            emitByte(0xA9);
        } else {
            encode = prefixAndEncode(encode);
            emitByte(0xF7);
            emitByte(0xC0 | encode);
        }
        emitInt(imm32);
    }

    public final void testl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x85, 0xC0, dst, src);
    }

    public final void testl(CiRegister dst, Address src) {

        prefix(src, dst);
        emitByte(0x85);
        emitOperand(dst, src);

    }

    public final void ucomisd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0x66);
        ucomiss(dst, src);
    }

    public final void ucomisd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0x66);
        ucomiss(dst, src);
    }

    public final void ucomiss(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();

        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x2E);
        emitOperand(dst, src);

    }

    public final void ucomiss(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2E);
        emitByte(0xC0 | encode);
    }

    public final void xaddl(Address dst, CiRegister src) {
        assert src.isXMM();

        prefix(dst, src);
        emitByte(0x0F);
        emitByte(0xC1);
        emitOperand(src, dst);

    }

    public final void xchgl(CiRegister dst, Address src) { // xchg

        prefix(src, dst);
        emitByte(0x87);
        emitOperand(dst, src);

    }

    public final void xchgl(CiRegister dst, CiRegister src) {
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x87);
        emitByte(0xc0 | encode);
    }

    public final void xorl(CiRegister dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xF0, dst, imm32);
    }

    public final void xorl(CiRegister dst, Address src) {

        prefix(src, dst);
        emitByte(0x33);
        emitOperand(dst, src);

    }

    public final void xorl(CiRegister dst, CiRegister src) {
        prefixAndEncode(dst.encoding, src.encoding);
        emitArith(0x33, 0xC0, dst, src);
    }

    public final void xorpd(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0x66);
        xorps(dst, src);
    }

    public final void xorpd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0x66);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x57);
        emitOperand(dst, src);

    }

    public final void xorps(CiRegister dst, CiRegister src) {

        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        int encode = prefixAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x57);
        emitByte(0xC0 | encode);
    }

    public final void xorps(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();

        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x57);
        emitOperand(dst, src);

    }

    // 32bit only pieces of the assembler

    // The 64-bit (32bit platform) cmpxchg compares the value at adr with the contents of
    // X86.rdx:X86.rax,
    // and stores X86.rcx:X86.rbx into adr if so; otherwise, the value at adr is loaded
    // into X86.rdx:X86.rax. The ZF is set if the compared values were equal, and cleared otherwise.
    public final void cmpxchg8(Address adr) {
        assert target.arch.is32bit();

        emitByte(0x0F);
        emitByte(0xc7);
        emitOperand(X86.rcx, adr);

    }

    public final void decl(CiRegister dst) {
        if (target.arch.is32bit()) {
            assert target.arch.is32bit();
            // Don't use it directly. Use Macrodecrementl() instead.
            emitByte(0x48 | dst.encoding);
        } else if (target.arch.is64bit()) {

            // Don't use it directly. Use Macrodecrementl() instead.
            // Use two-byte form (one-byte form is a REX prefix in 64-bit mode)
            int encode = prefixAndEncode(dst.encoding);
            emitByte(0xFF);
            emitByte(0xC8 | encode);
        } else {
            Util.shouldNotReachHere();
        }
    }

    public final void incl(CiRegister dst) {
        if (target.arch.is32bit()) {
            assert target.arch.is32bit();
            // Don't use it directly. Use Macroincrementl() instead.
            emitByte(0x40 | dst.encoding);
        } else {
            // Don't use it directly. Use Macroincrementl() instead.
            // Use two-byte form (one-byte from is a REX prefix in 64-bit mode)
            int encode = prefixAndEncode(dst.encoding);
            emitByte(0xFF);
            emitByte(0xC0 | encode);
        }
    }

    public final void lea(CiRegister dst, Address src) {
        if (target.arch.is32bit()) {
            assert target.arch.is32bit();
            leal(dst, src);
        } else {
            leaq(dst, src);
        }
    }

    // TODO: remove?
    public final void popa() {

        if (target.arch.is64bit()) {
            // 32bit
            assert target.arch.is32bit();
            emitByte(0x61);
        } else {
            final int wordSize = target.arch.wordSize;
            // 64bit
            movq(X86.r15, new Address(X86.rsp, 0));
            movq(X86.r14, new Address(X86.rsp, wordSize));
            movq(X86.r13, new Address(X86.rsp, 2 * wordSize));
            movq(X86.r12, new Address(X86.rsp, 3 * wordSize));
            movq(X86.r11, new Address(X86.rsp, 4 * wordSize));
            movq(X86.r10, new Address(X86.rsp, 5 * wordSize));
            movq(X86.r9, new Address(X86.rsp, 6 * wordSize));
            movq(X86.r8, new Address(X86.rsp, 7 * wordSize));
            movq(X86.rdi, new Address(X86.rsp, 8 * wordSize));
            movq(X86.rsi, new Address(X86.rsp, 9 * wordSize));
            movq(X86.rbp, new Address(X86.rsp, 10 * wordSize));
            // skip rsp
            movq(X86.rbx, new Address(X86.rsp, 12 * wordSize));
            movq(X86.rdx, new Address(X86.rsp, 13 * wordSize));
            movq(X86.rcx, new Address(X86.rsp, 14 * wordSize));
            movq(X86.rax, new Address(X86.rsp, 15 * wordSize));

            addq(X86.rsp, 16 * wordSize);
        }
    }

    // TODO: remove?
    public final void pusha() {

        if (target.arch.is32bit()) {
            // 32bit
            assert target.arch.is32bit();
            emitByte(0x60);
        } else {

            final int wordSize = target.arch.wordSize;

            // we have to store original rsp. ABI says that 128 bytes
            // below rsp are local scratch.
            movq(new Address(X86.rsp, -5 * wordSize), X86.rsp);

            subq(X86.rsp, 16 * wordSize);

            movq(new Address(X86.rsp, 15 * wordSize), X86.rax);
            movq(new Address(X86.rsp, 14 * wordSize), X86.rcx);
            movq(new Address(X86.rsp, 13 * wordSize), X86.rdx);
            movq(new Address(X86.rsp, 12 * wordSize), X86.rbx);
            // skip rsp
            movq(new Address(X86.rsp, 10 * wordSize), X86.rbp);
            movq(new Address(X86.rsp, 9 * wordSize), X86.rsi);
            movq(new Address(X86.rsp, 8 * wordSize), X86.rdi);
            movq(new Address(X86.rsp, 7 * wordSize), X86.r8);
            movq(new Address(X86.rsp, 6 * wordSize), X86.r9);
            movq(new Address(X86.rsp, 5 * wordSize), X86.r10);
            movq(new Address(X86.rsp, 4 * wordSize), X86.r11);
            movq(new Address(X86.rsp, 3 * wordSize), X86.r12);
            movq(new Address(X86.rsp, 2 * wordSize), X86.r13);
            movq(new Address(X86.rsp, wordSize), X86.r14);
            movq(new Address(X86.rsp, 0), X86.r15);
        }
    }

    public final void setByteIfNotZero(CiRegister dst) {
        assert target.arch.is32bit();
        emitByte(0x0F);
        emitByte(0x95);
        emitByte(0xE0 | dst.encoding);
    }

    public final void shldl(CiRegister dst, CiRegister src) {
        assert target.arch.is32bit();
        emitByte(0x0F);
        emitByte(0xA5);
        emitByte(0xC0 | src.encoding << 3 | dst.encoding);
    }

    public final void shrdl(CiRegister dst, CiRegister src) {
        assert target.arch.is32bit();
        emitByte(0x0F);
        emitByte(0xAD);
        emitByte(0xC0 | src.encoding << 3 | dst.encoding);
    }

    int prefixAndEncode(int regEnc) {
        if (target.arch.is64bit()) {
            return prefixAndEncode(regEnc, false);
        } else {
            return regEnc;
        }
    }

    int prefixAndEncode(int regEnc, boolean byteinst) {
      //  assert target.arch.is32bit();
        if (regEnc >= 8) {
            emitByte(Prefix.REXB);
            regEnc -= 8;
        } else if (byteinst && regEnc >= 4) {
            emitByte(Prefix.REX);
        }
        return regEnc;
    }

    int prefixqAndEncode(int regEnc) {
        if (regEnc < 8) {
            emitByte(Prefix.REXW);
        } else {
            emitByte(Prefix.REXWB);
            regEnc -= 8;
        }
        return regEnc;
    }

    int prefixAndEncode(int dstEnc, int srcEnc) {
        return prefixAndEncode(dstEnc, srcEnc, false);
    }

    int prefixAndEncode(int dstEnc, int srcEnc, boolean byteinst) {
//        assert target.arch.is32bit();
        if (dstEnc < 8) {
            if (srcEnc >= 8) {
                emitByte(Prefix.REXB);
                srcEnc -= 8;
            } else if (byteinst && srcEnc >= 4) {
                emitByte(Prefix.REX);
            }
        } else {
            if (srcEnc < 8) {
                emitByte(Prefix.REXR);
            } else {
                emitByte(Prefix.REXRB);
                srcEnc -= 8;
            }
            dstEnc -= 8;
        }
        return dstEnc << 3 | srcEnc;
    }

    /**
     * Creates prefix and the encoding of the lower 6 bits of the ModRM-Byte. It emits an operand prefix. If the given
     * operands exceed 3 bits, the 4th bit is encoded in the prefix.
     *
     * @param regEnc
     *            the encoding of the register part of the ModRM-Byte
     * @param rmEnc
     *            the encoding of the r/m part of the ModRM-Byte
     * @return the lower 6 bits of the ModRM-Byte that should be emitted
     */
    int prefixqAndEncode(int regEnc, int rmEnc) {
        if (regEnc < 8) {
            if (rmEnc < 8) {
                emitByte(Prefix.REXW);
            } else {
                emitByte(Prefix.REXWB);
                rmEnc -= 8;
            }
        } else {
            if (rmEnc < 8) {
                emitByte(Prefix.REXWR);
            } else {
                emitByte(Prefix.REXWRB);
                rmEnc -= 8;
            }
            regEnc -= 8;
        }
        return regEnc << 3 | rmEnc;
    }

    private void prefix(CiRegister reg) {
        if (reg.encoding >= 8) {
            emitByte(Prefix.REXB);
        }
    }

    void prefix(Address adr) {
        if (adr.base.encoding >= MinEncodingNeedsRex) {
            if (adr.index.encoding >= MinEncodingNeedsRex) {
                emitByte(Prefix.REXXB);
            } else {
                emitByte(Prefix.REXB);
            }
        } else {
            if (adr.index.encoding >= MinEncodingNeedsRex) {
                emitByte(Prefix.REXX);
            }
        }
    }

    void prefixq(Address adr) {
        if (adr.base.encoding >= MinEncodingNeedsRex) {
            if (adr.index.encoding >= MinEncodingNeedsRex) {
                emitByte(Prefix.REXWXB);
            } else {
                emitByte(Prefix.REXWB);
            }
        } else {
            if (adr.index.encoding >= MinEncodingNeedsRex) {
                emitByte(Prefix.REXWX);
            } else {
                emitByte(Prefix.REXW);
            }
        }
    }

    private void prefix(Address adr, CiRegister reg) {

        if (reg.encoding < 8) {
            if (adr.base.encoding >= MinEncodingNeedsRex) {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXXB);
                } else {
                    emitByte(Prefix.REXB);
                }
            } else {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXX);
                } else if (reg.encoding >= 4) {
                    emitByte(Prefix.REX);
                }
            }
        } else {
            if (adr.base.encoding >= MinEncodingNeedsRex) {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXRXB);
                } else {
                    emitByte(Prefix.REXRB);
                }
            } else {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXRX);
                } else {
                    emitByte(Prefix.REXR);
                }
            }
        }
    }

    void prefixq(Address adr, CiRegister src) {
        if (src.encoding < 8) {
            if (adr.base.encoding >= MinEncodingNeedsRex) {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXWXB);
                } else {
                    emitByte(Prefix.REXWB);
                }
            } else {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXWX);
                } else {
                    emitByte(Prefix.REXW);
                }
            }
        } else {
            if (adr.base.encoding >= MinEncodingNeedsRex) {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXWRXB);
                } else {
                    emitByte(Prefix.REXWRB);
                }
            } else {
                if (adr.index.encoding >= MinEncodingNeedsRex) {
                    emitByte(Prefix.REXWRX);
                } else {
                    emitByte(Prefix.REXWR);
                }
            }
        }
    }

    public final void adcq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xD0, dst, imm32);
    }

    public final void adcq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x13);
        emitOperand(dst, src);
    }

    public final void adcq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x13, 0xC0, dst, src);
    }

    public final void addq(Address dst, int imm32) {
        prefixq(dst);
        emitArithOperand(0x81, X86.rax, dst, imm32);
    }

    public final void addq(Address dst, CiRegister src) {
        prefixq(dst, src);
        emitByte(0x01);
        emitOperand(src, dst);
    }

    public final void addq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xC0, dst, imm32);
    }

    public final void addq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x03);
        emitOperand(dst, src);
    }

    public final void addq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x03, 0xC0, dst, src);
    }

    public final void andq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xE0, dst, imm32);
    }

    public final void andq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x23);
        emitOperand(dst, src);
    }

    public final void andq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x23, 0xC0, dst, src);
    }

    public final void bsfq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBC);
        emitByte(0xC0 | encode);
    }

    public final void bsrq(CiRegister dst, CiRegister src) {
        assert !target.supportsLzcnt() : "encoding is treated as LZCNT";
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    public final void bswapq(CiRegister reg) {
        int encode = prefixqAndEncode(reg.encoding);
        emitByte(0x0F);
        emitByte(0xC8 | encode);
    }

    public final void cdqq() {
        emitByte(Prefix.REXW);
        emitByte(0x99);
    }

    public final void clflush(Address adr) {
        prefix(adr);
        emitByte(0x0F);
        emitByte(0xAE);
        emitOperand(X86.rdi, adr);
    }

    public final void cmovq(Condition cc, CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitByte(0xC0 | encode);
    }

    public final void cmovq(Condition cc, CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitOperand(dst, src);
    }

    public final void cmpq(Address dst, int imm32) {
        prefixq(dst);
        emitByte(0x81);
        emitOperand(X86.rdi, dst, 4);
        emitInt(imm32);
    }

    public final void cmpq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xF8, dst, imm32);
    }

    public final void cmpq(Address dst, CiRegister src) {
        prefixq(dst, src);
        emitByte(0x3B);
        emitOperand(src, dst);
    }

    public final void cmpq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x3B, 0xC0, dst, src);
    }

    public final void cmpq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x3B);
        emitOperand(dst, src);
    }

    public final void cmpxchgq(CiRegister reg, Address adr) {
        prefixq(adr, reg);
        emitByte(0x0F);
        emitByte(0xB1);
        emitOperand(reg, adr);
    }

    public final void cvtsi2sdq(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    public final void cvtsi2ssq(CiRegister dst, CiRegister src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    public final void cvttsd2siq(CiRegister dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    public final void cvttss2siq(CiRegister dst, CiRegister src) {
        assert src.isXMM();
        assert target.arch.is64bit() || target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    public final void decq(CiRegister dst) {
        // Don't use it directly. Use Macrodecrementq() instead.
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode)
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xFF);
        emitByte(0xC8 | encode);
    }

    public final void decq(Address dst) {
        // Don't use it directly. Use Macrodecrementq() instead.
        prefixq(dst);
        emitByte(0xFF);
        emitOperand(X86.rcx, dst);
    }

    public final void idivq(CiRegister src) {
        int encode = prefixqAndEncode(src.encoding);
        emitByte(0xF7);
        emitByte(0xF8 | encode);
    }

    public final void imulq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xAF);
        emitByte(0xC0 | encode);
    }

    public final void imulq(CiRegister dst, CiRegister src, int value) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        if (Util.is8bit(value)) {
            emitByte(0x6B);
            emitByte(0xC0 | encode);
            emitByte(value);
        } else {
            emitByte(0x69);
            emitByte(0xC0 | encode);
            emitInt(value);
        }
    }

    public final void incq(CiRegister dst) {
        // Don't use it directly. Use Macroincrementq() instead.
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode)
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xFF);
        emitByte(0xC0 | encode);
    }

    public final void incq(Address dst) {
        // Don't use it directly. Use Macroincrementq() instead.
        prefixq(dst);
        emitByte(0xFF);
        emitOperand(X86.rax, dst);
    }

    public final void leaq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x8D);
        emitOperand(dst, src);
    }

    public final void mov64(CiRegister dst, long imm64) {
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xB8 | encode);
        emitLong(imm64);
    }

    public final void lzcntq(CiRegister dst, CiRegister src) {
        assert target.supportsLzcnt() : "encoding is treated as BSR";
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    public final void movdq(CiRegister dst, CiRegister src) {

        // table D-1 says MMX/SSE2
        assert target.arch.is64bit() || target.supportsSSE2() || target.supportsMMX();
        emitByte(0x66);

        if (dst.isXMM()) {
            assert dst.isXMM();
            int encode = prefixqAndEncode(dst.encoding, src.encoding);
            emitByte(0x0F);
            emitByte(0x6E);
            emitByte(0xC0 | encode);
        } else if (src.isXMM()) {

            // swap src/dst to get correct prefix
            int encode = prefixqAndEncode(src.encoding, dst.encoding);
            emitByte(0x0F);
            emitByte(0x7E);
            emitByte(0xC0 | encode);
        } else {
            Util.shouldNotReachHere();
        }
    }

    public final void movsbq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x0F);
        emitByte(0xBE);
        emitOperand(dst, src);
    }

    public final void movsbq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBE);
        emitByte(0xC0 | encode);
    }

    public final void movslq(CiRegister dst, int imm32) {
        // dbx shows movslq(X86.rcx, 3) as movq $0x0000000049000000,(%X86.rbx)
        // and movslq(X86.r8, 3); as movl $0x0000000048000000,(%X86.rbx)
        // as a result we shouldn't use until tested at runtime...
        Util.shouldNotReachHere();

        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xC7 | encode);
        emitInt(imm32);
    }

    public static boolean isSimm32(int imm32) {
        return true;
    }

    public final void movslq(Address dst, int imm32) {
        assert isSimm32(imm32) : "lost bits";
        prefixq(dst);
        emitByte(0xC7);
        emitOperand(X86.rax, dst, 4);
        emitInt(imm32);
    }

    public final void movslq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x63);
        emitOperand(dst, src);
    }

    public final void movslq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x63);
        emitByte(0xC0 | encode);
    }

    public final void movswq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x0F);
        emitByte(0xBF);
        emitOperand(dst, src);
    }

    public final void movswq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xBF);
        emitByte(0xC0 | encode);
    }

    public final void movzbq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x0F);
        emitByte(0xB6);
        emitOperand(dst, src);
    }

    public final void movzbq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xB6);
        emitByte(0xC0 | encode);
    }

    public final void movzwq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x0F);
        emitByte(0xB7);
        emitOperand(dst, src);
    }

    public final void movzwq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xB7);
        emitByte(0xC0 | encode);
    }

    public final void negq(CiRegister dst) {
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xF7);
        emitByte(0xD8 | encode);
    }

    public final void notq(CiRegister dst) {
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xF7);
        emitByte(0xD0 | encode);
    }

    public final void orq(Address dst, int imm32) {
        prefixq(dst);
        emitByte(0x81);
        emitOperand(X86.rcx, dst, 4);
        emitInt(imm32);
    }

    public final void orq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xC8, dst, imm32);
    }

    public final void orq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x0B);
        emitOperand(dst, src);
    }

    public final void orq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x0B, 0xC0, dst, src);
    }

    public final void popcntq(CiRegister dst, Address src) {
        assert target.supportsPopcnt() : "must support";
        emitByte(0xF3);
        prefixq(src, dst);
        emitByte(0x0F);
        emitByte(0xB8);
        emitOperand(dst, src);
    }

    public final void popcntq(CiRegister dst, CiRegister src) {
        assert target.supportsPopcnt() : "must support";
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x0F);
        emitByte(0xB8);
        emitByte(0xC0 | encode);
    }

    public final void popq(Address dst) {
        prefixq(dst);
        emitByte(0x8F);
        emitOperand(X86.rax, dst);
    }

    public final void pushq(Address src) {
        prefixq(src);
        emitByte(0xFF);
        emitOperand(X86.rsi, src);
    }

    public final void rclq(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding);
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xD0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xD0 | encode);
            emitByte(imm8);
        }
    }

    public final void sarq(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding);
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xF8 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xF8 | encode);
            emitByte(imm8);
        }
    }

    public final void sarq(CiRegister dst) {
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xD3);
        emitByte(0xF8 | encode);
    }

    public final void sbbq(Address dst, int imm32) {
        prefixq(dst);
        emitArithOperand(0x81, X86.rbx, dst, imm32);
    }

    public final void sbbq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xD8, dst, imm32);
    }

    public final void sbbq(CiRegister dst, Address src) {

        prefixq(src, dst);
        emitByte(0x1B);
        emitOperand(dst, src);

    }

    public final void sbbq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x1B, 0xC0, dst, src);
    }

    public final void shlq(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding);
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xE0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xE0 | encode);
            emitByte(imm8);
        }
    }

    public final void shlq(CiRegister dst) {
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xD3);
        emitByte(0xE0 | encode);
    }

    public final void shrq(CiRegister dst, int imm8) {
        assert Util.isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xC1);
        emitByte(0xE8 | encode);
        emitByte(imm8);
    }

    public final void shrq(CiRegister dst) {
        int encode = prefixqAndEncode(dst.encoding);
        emitByte(0xD3);
        emitByte(0xE8 | encode);
    }

    public final void sqrtsd(CiRegister dst, Address src) {
        assert dst.isXMM();
        assert target.arch.is64bit() || target.supportsSSE2();

        emitByte(0xF2);
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x51);
        emitOperand(dst, src);
    }

    public final void subq(Address dst, int imm32) {
        prefixq(dst);
        if (Util.is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(X86.rbp, dst, 1);
            emitByte(imm32 & 0xFF);
        } else {
            emitByte(0x81);
            emitOperand(X86.rbp, dst, 4);
            emitInt(imm32);
        }
    }

    public final void subq(CiRegister dst, int imm32) {
        prefixqAndEncode(dst.encoding);
        emitArith(0x81, 0xE8, dst, imm32);
    }

    public final void subq(Address dst, CiRegister src) {
        prefixq(dst, src);
        emitByte(0x29);
        emitOperand(src, dst);
    }

    public final void subq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x2B);
        emitOperand(dst, src);
    }

    public final void subq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x2B, 0xC0, dst, src);
    }

    public final void testq(CiRegister dst, int imm32) {
        // not using emitArith because test
        // doesn't support sign-extension of
        // 8bit operands
        int encode = dst.encoding;
        if (encode == 0) {
            emitByte(Prefix.REXW);
            emitByte(0xA9);
        } else {
            encode = prefixqAndEncode(encode);
            emitByte(0xF7);
            emitByte(0xC0 | encode);
        }
        emitInt(imm32);
    }

    public final void testq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x85, 0xC0, dst, src);
    }

    public final void xaddq(Address dst, CiRegister src) {
        prefixq(dst, src);
        emitByte(0x0F);
        emitByte(0xC1);
        emitOperand(src, dst);
    }

    public final void xchgq(CiRegister dst, Address src) {
        prefixq(src, dst);
        emitByte(0x87);
        emitOperand(dst, src);
    }

    public final void xchgq(CiRegister dst, CiRegister src) {
        int encode = prefixqAndEncode(dst.encoding, src.encoding);
        emitByte(0x87);
        emitByte(0xc0 | encode);
    }

    public final void xorq(CiRegister dst, CiRegister src) {
        prefixqAndEncode(dst.encoding, src.encoding);
        emitArith(0x33, 0xC0, dst, src);
    }

    public final void xorq(CiRegister dst, Address src) {

        prefixq(src, dst);
        emitByte(0x33);
        emitOperand(dst, src);

    }

    enum MembarMaskBits {

        LoadLoad, StoreLoad, LoadStore, StoreStore;

        public int mask() {
            return 1 << this.ordinal();
        }
    }

    // Serializes memory and blows flags
    public final void membar(int orderConstraint) {
        if (target.isMP) {
            // We only have to handle StoreLoad
            if ((orderConstraint & MembarMaskBits.StoreLoad.mask()) != 0) {
                // All usable chips support "locked" instructions which suffice
                // as barriers, and are much faster than the alternative of
                // using cpuid instruction. We use here a locked add [esp],0.
                // This is conveniently otherwise a no-op except for blowing
                // flags.
                // Any change to this code may need to revisit other places in
                // the code where this idiom is used, in particular the
                // orderAccess code.
                lock();
                addl(new Address(X86.rsp, 0), 0); // Assert the lock# signal here
            }
        }
    }

    @Override
    public final void patchJumpTarget(int branch, int branchTarget) {
        int op = codeBuffer.getByte(branch);
        assert op == 0xE8 // call
                        ||
                        op == 0xE9 // jmp
                        || op == 0xEB // short jmp
                        || (op & 0xF0) == 0x70 // short jcc
                        || op == 0x0F && (codeBuffer.getByte(branch + 1) & 0xF0) == 0x80 // jcc
        : "Invalid opcode at patch point";

        if (op == 0xEB || (op & 0xF0) == 0x70) {

            // short offset operators (jmp and jcc)
            int imm8 = branchTarget - (branch + 2);
            assert Util.is8bit(imm8) : "Short forward jump exceeds 8-bit offset";
            codeBuffer.emitByte(imm8, branch + 1);

        } else {

            int off = 1;
            if (op == 0x0F) {
                off = 2;
            }

            int imm32 = branchTarget - (branch + 4 + off);
            codeBuffer.emitInt(imm32, branch + off);
        }
    }
}
