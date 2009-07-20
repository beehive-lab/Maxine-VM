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
import com.sun.c1x.util.*;

public abstract class X86Assembler extends AbstractAssembler {

    enum Condition { // The x86 condition codes used for conditional jumps/moves.
        zero(0x4), notZero(0x5), equal(0x4), notEqual(0x5), less(0xc), lessEqual(0xe), greater(0xf), greaterEqual(0xd), below(0x2), belowEqual(0x6), above(0x7), aboveEqual(0x3), overflow(0x0), noOverflow(
                        0x1), carrySet(0x2), carryClear(0x3), negative(0x8), positive(0x9), parity(0xa), noParity(0xb);

        public final int value;

        private Condition(int value) {
            this.value = value;
        }

        public Condition negate() {
            switch (this) {
                // Note some conditions are synonyms for others
                case zero:
                    return notZero;
                case notZero:
                    return zero;
                case less:
                    return greaterEqual;
                case lessEqual:
                    return greater;
                case greater:
                    return lessEqual;
                case greaterEqual:
                    return less;
                case below:
                    return aboveEqual;
                case belowEqual:
                    return above;
                case above:
                    return belowEqual;
                case aboveEqual:
                    return below;
                case overflow:
                    return noOverflow;
                case noOverflow:
                    return overflow;
                case negative:
                    return positive;
                case positive:
                    return negative;
                case parity:
                    return noParity;
                case noParity:
                    return parity;
            }
            throw Util.shouldNotReachHere();
        }
    }

    private static final Prefix[] lookup = new Prefix[256];

    enum Prefix {
        // segment overrides
        CSSegment(0x2e), SSSegment(0x36), DSSegment(0x3e), ESSegment(0x26), FSSegment(0x64), GSSegment(0x65),

        REX(0x40),

        REXB(0x41), REXX(0x42), REXXB(0x43), REXR(0x44), REXRB(0x45), REXRX(0x46), REXRXB(0x47),

        REXW(0x48),

        REXWB(0x49), REXWX(0x4A), REXWXB(0x4B), REXWR(0x4C), REXWRB(0x4D), REXWRX(0x4E), REXWRXB(0x4F);

        public final int value;

        private Prefix(int value) {
            this.value = value;
            assert value < lookup.length : "increase lookup table size!";
            assert lookup[value] == null : "duplicate value!";
            lookup[value] = this;
        }

    }

    public static Prefix prefixFromInt(int i) {
        return lookup[i];
    }

    private void emitData(int disp, RelocationHolder rspec, WhichOperand disp32operand) {
        emitData(disp, rspec, disp32operand.ordinal());

    }

    enum WhichOperand {
        // input to locateOperand, and format code for relocations
        immOperand, // embedded 32-bit|64-bit immediate operand
        disp32operand, // embedded 32-bit displacement or Address
        call32operand, // embedded 32-bit self-relative displacement
        narrowOopOperand, endPcOperand
    }

    public X86Assembler(C1XCompilation compilation, CodeBuffer code) {
        super(compilation, code);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected int codeFillByte() {
        return 0xF4;
    }

    // make this go away someday
    void emitData(int data, RelocInfo.Type rtype, int format) {
        if (rtype == RelocInfo.Type.none) {
            emitLong(data);
        } else {
            emitData(data, Relocation.specSimple(rtype), format);
        }
    }

    void emitData(int data, RelocationHolder rspec, int format) {
        assert WhichOperand.immOperand.ordinal() == 0 : "default format must be immediate in this file";
        assert instMark() != null : "must be inside InstructionMark";
        if (rspec.type() != RelocInfo.Type.none) {
            assert checkRelocation(rspec, format);
            // Do not use AbstractAssembler.relocate, which is not intended for
            // embedded words. Instead, relocate to the enclosing instruction.

            // hack. call32 is too wide for mask so use disp32
            if (format == WhichOperand.call32operand.ordinal()) {
                codeSection().relocate(instMark(), rspec, WhichOperand.disp32operand.ordinal());
            } else {
                codeSection().relocate(instMark(), rspec, format);
            }
        }
        emitLong(data);
    }

    static int encode(X86Register r) {
        int enc = r.encoding();
        if (enc >= 8) {
            enc -= 8;
        }
        return enc;
    }

    void emitArithB(int op1, int op2, X86Register dst, int imm8) {
        assert dst.hasByteRegister() : "must have byte register";
        assert isByte(op1) && isByte(op2) : "wrong opcode";
        assert isByte(imm8) : "not a byte";
        assert (op1 & 0x01) == 0 : "should be 8bit operation";
        emitByte(op1);
        emitByte(op2 | encode(dst));
        emitByte(imm8);
    }

    void emitArith(int op1, int op2, X86Register dst, int imm32) {
        assert isByte(op1) && isByte(op2) : "wrong opcode";
        assert (op1 & 0x01) == 1 : "should be 32bit operation";
        assert (op1 & 0x02) == 0 : "sign-extension bit should not be set";
        if (is8bit(imm32)) {
            emitByte(op1 | 0x02); // set sign bit
            emitByte(op2 | encode(dst));
            emitByte(imm32 & 0xFF);
        } else {
            emitByte(op1);
            emitByte(op2 | encode(dst));
            emitLong(imm32);
        }
    }

    // immediate-to-memory forms
    void emitArithOperand(int op1, X86Register rm, Address adr, int imm32) {
        assert (op1 & 0x01) == 1 : "should be 32bit operation";
        assert (op1 & 0x02) == 0 : "sign-extension bit should not be set";
        if (is8bit(imm32)) {
            emitByte(op1 | 0x02); // set sign bit
            emitOperand(rm, adr, 1);
            emitByte(imm32 & 0xFF);
        } else {
            emitByte(op1);
            emitOperand(rm, adr, 4);
            emitLong(imm32);
        }
    }

    void emitArith(int op1, int op2, X86Register dst, Object obj) {

        if (compilation.target.arch.is64bit()) {
            throw Util.shouldNotReachHere();
        }
        assert isByte(op1) && isByte(op2) : "wrong opcode";
        assert (op1 & 0x01) == 1 : "should be 32bit operation";
        assert (op1 & 0x02) == 0 : "sign-extension bit should not be set";
        try {
            setInstMark();
            emitByte(op1);
            emitByte(op2 | encode(dst));
            if (compilation.target.arch.is32bit()) {
                int objPointer = Util.convertToPointer32(obj);
                emitData(objPointer, RelocInfo.Type.oopType, 0);
            } else {
                long objPointer = Util.convertToPointer64(obj);
                emitData64(objPointer, RelocInfo.Type.oopType, 0);
            }
        } finally {
            clearInstMark();
        }
    }

    void testptr(X86Register src, int imm32) {
        if (compilation.target.arch.is64bit()) {
            testq(src, imm32);
        } else {
            testl(src, imm32);
        }
    }

    void emitArith(int op1, int op2, X86Register dst, X86Register src) {
        assert isByte(op1) && isByte(op2) : "wrong opcode";
        emitByte(op1);
        emitByte(op2 | encode(dst) << 3 | encode(src));
    }

    void emitOperand(X86Register reg, X86Register base, X86Register index, Address.ScaleFactor scale, int disp, RelocationHolder rspec, int ripRelativeCorrection) {
        RelocInfo.Type rtype = rspec.type();

        // Encode the registers as needed in the fields they are used in

        int regenc = encode(reg) << 3;
        int indexenc = index.isValid() ? encode(index) << 3 : 0;
        int baseenc = base.isValid() ? encode(base) : 0;

        if (base.isValid()) {
            if (index.isValid()) {
                assert scale != Address.ScaleFactor.noScale : "inconsistent Address";
                // [base + indexscale + disp]
                if (disp == 0 && rtype == RelocInfo.Type.none && base != X86Register.rbp && (!compilation.target.arch.is64bit() || base != X86Register.r13)) {
                    // [base + indexscale]
                    // [00 reg 100][ss index base]
                    assert index != X86Register.rsp : "illegal addressing mode";
                    emitByte(0x04 | regenc);
                    emitByte(scale.value << 6 | indexenc | baseenc);
                } else if (is8bit(disp) && rtype == RelocInfo.Type.none) {
                    // [base + indexscale + imm8]
                    // [01 reg 100][ss index base] imm8
                    assert index != X86Register.rsp : "illegal addressing mode";
                    emitByte(0x44 | regenc);
                    emitByte(scale.value << 6 | indexenc | baseenc);
                    emitByte(disp & 0xFF);
                } else {
                    // [base + indexscale + disp32]
                    // [10 reg 100][ss index base] disp32
                    assert index != X86Register.rsp : "illegal addressing mode";
                    emitByte(0x84 | regenc);
                    emitByte(scale.value << 6 | indexenc | baseenc);
                    emitData(disp, rspec, WhichOperand.disp32operand.ordinal());
                }
            } else if (base == X86Register.rsp || (compilation.target.arch.is64bit() && base == X86Register.r12)) {
                // [rsp + disp]
                if (disp == 0 && rtype == RelocInfo.Type.none) {
                    // [rsp]
                    // [00 reg 100][00 100 100]
                    emitByte(0x04 | regenc);
                    emitByte(0x24);
                } else if (is8bit(disp) && rtype == RelocInfo.Type.none) {
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
                    emitData(disp, rspec, WhichOperand.disp32operand.ordinal());
                }
            } else {
                // [base + disp]
                assert base != X86Register.rsp && (!compilation.target.arch.is64bit() || base != X86Register.r12) : "illegal addressing mode";
                if (disp == 0 && rtype == RelocInfo.Type.none && base != X86Register.rbp && (!compilation.target.arch.is64bit() || base != X86Register.r13)) {
                    // [base]
                    // [00 reg base]
                    emitByte(0x00 | regenc | baseenc);
                } else if (is8bit(disp) && rtype == RelocInfo.Type.none) {
                    // [base + disp8]
                    // [01 reg base] disp8
                    emitByte(0x40 | regenc | baseenc);
                    emitByte(disp & 0xFF);
                } else {
                    // [base + disp32]
                    // [10 reg base] disp32
                    emitByte(0x80 | regenc | baseenc);
                    emitData(disp, rspec, WhichOperand.disp32operand);
                }
            }
        } else {
            if (index.isValid()) {
                assert scale != Address.ScaleFactor.noScale : "inconsistent Address";
                // [indexscale + disp]
                // [00 reg 100][ss index 101] disp32
                assert index != X86Register.rsp : "illegal addressing mode";
                emitByte(0x04 | regenc);
                emitByte(scale.value << 6 | indexenc | 0x05);
                emitData(disp, rspec, WhichOperand.disp32operand);
            } else if (rtype != RelocInfo.Type.none) {
                // [disp] (64bit) RIP-RELATIVE (32bit) abs
                // [00 000 101] disp32

                emitByte(0x05 | regenc);
                // Note that the RIP-rel. correction applies to the generated
                // disp field, but not_ to the target Address in the rspec.

                // disp was created by converting the target Address minus the pc
                // at the start of the instruction. That needs more correction here.
                // intptrT disp = target - nextIp;
                assert instMark() != null : "must be inside InstructionMark";
                long nextIp = pc().value + Util.sizeofInt() + ripRelativeCorrection;
                long adjusted = disp;
                // Do rip-rel adjustment for 64bit
                if (compilation.target.arch.is64bit()) {
                    adjusted -= (nextIp - instMark().value);
                }
                assert isSimm32(adjusted) : "must be 32bit offset (RIP relative Address)";
                emitData((int) adjusted, rspec, WhichOperand.disp32operand);

            } else {
                // 32bit never did this, did everything as the rip-rel/disp code above
                // [disp] ABSOLUTE
                // [00 reg 100][00 100 101] disp32
                emitByte(0x04 | regenc);
                emitByte(0x25);
                emitData(disp, rspec, WhichOperand.disp32operand);
            }
        }
    }

    static boolean isSimm32(long adjusted) {
        return adjusted == (int) adjusted;
    }

    Pointer locateOperand(Pointer inst, WhichOperand which) {

        /*
         * // Decode the given instruction, and return the Address of // an embedded 32-bit operand word.
         *
         * // If "which" is WhichOperand.disp32operand, selects the displacement portion // of an effective Address
         * specifier. // If "which" is imm64Operand, selects the trailing immediate constant. // If "which" is
         * WhichOperand.call32operand, selects the displacement of a call or jump. // Caller is responsible for ensuring
         * that there is such an operand, // and that it is 32/64 bits wide.
         *
         * // If "which" is endPcOperand, find the end of the instruction.
         *
         * Address ip = inst; boolean is64bit = false;
         *
         * boolean hasDisp32 = false;
         *
         *
         * int tailSize = 0; // other random bytes (#32, #16, etc.) at end of insn
         *
         *
         * boolean continueAfterPrefix = true;
         *
         * while (continueAfterPrefix) { continueAfterPrefix = false; Prefix nextIp = prefixFromInt(0xFF &
         * ip.accessInt()); ip.inc();
         *
         * switch (nextIp) {
         *
         *
         * case CSSegment: case SSSegment: case DSSegment: case ESSegment: case FSSegment: case GSSegment: // Seems
         * dubious assert !compilation.target.arch.is64bit() : "shouldn't have that prefix"; assert ip == inst+1 :
         * "only one prefix allowed"; continueAfterPrefix = true; break;
         *
         * case 0x67: case REX: case REXB: case REXX: case REXXB: case REXR: case REXRB: case REXRX: case REXRXB: assert
         * compilation.target.arch.is64bit() : "64bit prefixes"; continueAfterPrefix = true; break;
         *
         * case REXW: case REXWB: case REXWX: case REXWXB: case REXWR: case REXWRB: case REXWRX: case REXWRXB: assert
         * compilation.target.arch.is64bit() : "64bit prefixes"; is64bit = true; continueAfterPrefix = true; break;
         *
         * case 0xFF: // pushq a; decl a; incl a; call a; jmp a case 0x88: // movb a, r case 0x89: // movl a, r case
         * 0x8A: // movb r, a case 0x8B: // movl r, a case 0x8F: // popl a hasDisp32 = true; break;
         *
         * case 0x68: // pushq #32 if (which == WhichOperand.endPcOperand) { return ip + 4; } assert which ==
         * WhichOperand.immOperand && !is64bit : "pushl has no disp32 or 64bit immediate"; return ip; // not produced by
         * emitOperand
         *
         * case 0x66: // movw ... (size prefix) boolean continueAfterPrefix2 = true; while (continueAfterPrefix2) {
         * continueAfterPrefix2 = false; switch (0xFF & *ip++) { case REX: case REXB: case REXX: case REXXB: case REXR:
         * case REXRB: case REXRX: case REXRXB: case REXW: case REXWB: case REXWX: case REXWXB: case REXWR: case REXWRB:
         * case REXWRX: case REXWRXB: assert compilation.target.arch.is64bit() : "64bit prefix found";
         * continueAfterPrefix2 = true; case 0x8B: // movw r, a case 0x89: // movw a, r hasDisp32 = true; break; case
         * 0xC7: // movw a, #16 debugOnly(hasDisp32 = true); tailSize = 2; // the imm16 break; case 0x0F: // several
         * SSE/SSE2 variants ip--; // reparse the 0x0F goto againAfterPrefix; default: Util.shouldNotReachHere(); } }
         * break;
         *
         * case 0xB8: // movl/q r, #32/#64(oop?) case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBE: case
         * 0xBF:
         *
         * if (which == endPcOperand) return ip + (is64bit ? 8 : 4); // these assert are somewhat nonsensical assert
         * compilation.target.arch.is64bit() || which == WhichOperand.immOperand || which == WhichOperand.disp32operand;
         *
         * assert !compilation.target.arch.is64bit() || ((which == WhichOperand.call32operand || which ==
         * WhichOperand.immOperand) && is64bit || which == narrowOopOperand && !is64bit); return ip;
         *
         * case 0x69: // imul r : a : #32 case 0xC7: // movl a : #32(oop?) tailSize = 4; hasDisp32 = true; // has both
         * kinds of operands! break;
         *
         * case 0x0F: // movx... : etc. switch (0xFF & *ip++) { case 0x12: // movlps case 0x28: // movaps case 0x2E: //
         * ucomiss case 0x2F: // comiss case 0x54: // andps case 0x55: // andnps case 0x56: // orps case 0x57: // xorps
         * case 0x6E: // movd case 0x7E: // movd case 0xAE: // ldmxcsr a // 64bit side says it these have both operands
         * but that doesn't // appear to be true hasDisp32 = true; break;
         *
         * case 0xAD: // shrd r : a : %cl case 0xAF: // imul r : a case 0xBE: // movsbl r : a (movsxb) case 0xBF: //
         * movswl r : a (movsxw) case 0xB6: // movzbl r : a (movzxb) case 0xB7: // movzwl r : a (movzxw) case
         * REP16(0x40): // cmovl cc : r : a case 0xB0: // cmpxchgb case 0xB1: // cmpxchg case 0xC1: // xaddl case 0xC7:
         * // cmpxchg8 case REP16(0x90): // setcc a hasDisp32 = true; // fall out of the switch to decode the Address
         * break;
         *
         * case 0xAC: // shrd r : a : #8 hasDisp32 = true; tailSize = 1; // the imm8 break;
         *
         * case 0x80: // jcc rdisp32 case 0x81: case 0x82: case 0x83: case 0x84: case 0x85: case 0x86: case 0x87: case
         * 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8E: case 0x8F: if (which == endPcOperand)
         * return ip + 4; assert which == WhichOperand.call32operand : "jcc has no disp32 or imm"; return ip; default:
         * Util.shouldNotReachHere(); } break;
         *
         * case 0x81: // addl a : #32; addl r : #32 // also: orl : adcl : sbbl : andl : subl : xorl : cmpl // on 32bit
         * in the case of cmpl : the imm might be an oop tailSize = 4; debugOnly(hasDisp32 = true); // has both kinds of
         * operands! break;
         *
         * case 0x83: // addl a : #8; addl r : #8 // also: orl : adcl : sbbl : andl : subl : xorl : cmpl
         * debugOnly(hasDisp32 = true); // has both kinds of operands! tailSize = 1; break;
         *
         * case 0x9B: switch (0xFF & *ip++) { case 0xD9: // fnstcw a debugOnly(hasDisp32 = true); break; default:
         * Util.shouldNotReachHere(); } break;
         *
         * case REP4(0x00): // addb a : r; addl a : r; addb r : a; addl r : a case REP4(0x10): // adc... case
         * REP4(0x20): // and... case REP4(0x30): // xor... case REP4(0x08): // or... case REP4(0x18): // sbb... case
         * REP4(0x28): // sub... case 0xF7: // mull a case 0x8D: // lea r : a case 0x87: // xchg r : a case REP4(0x38):
         * // cmp... case 0x85: // test r : a debugOnly(hasDisp32 = true); // has both kinds of operands! break;
         *
         * case 0xC1: // sal a : #8; sar a : #8; shl a : #8; shr a : #8 case 0xC6: // movb a : #8 case 0x80: // cmpb a :
         * #8 case 0x6B: // imul r : a : #8 debugOnly(hasDisp32 = true); // has both kinds of operands! tailSize = 1; //
         * the imm8 break;
         *
         * case 0xE8: // call rdisp32 case 0xE9: // jmp rdisp32 if (which == endPcOperand) return ip + 4; assert(which
         * == WhichOperand.call32operand, "call has no disp32 or imm"); return ip;
         *
         * case 0xD1: // sal a : 1; sar a : 1; shl a : 1; shr a : 1 case 0xD3: // sal a : %cl; sar a : %cl; shl a : %cl;
         * shr a : %cl case 0xD9: // fldS a; fstS a; fstpS a; fldcw a case 0xDD: // fldD a; fstD a; fstpD a case 0xDB:
         * // fildS a; fistpS a; fldX a; fstpX a case 0xDF: // fildD a; fistpD a case 0xD8: // faddS a; fsubrS a; fmulS
         * a; fdivrS a; fcompS a case 0xDC: // faddD a; fsubrD a; fmulD a; fdivrD a; fcompD a case 0xDE: // faddpD a;
         * fsubrpD a; fmulpD a; fdivrpD a; fcomppD a hasDisp32 = true; break;
         *
         * case 0xF0: // Lock assert compilation.runtime.isMP() : "only on MP"; goto againAfterPrefix;
         *
         * case 0xF3: // For SSE case 0xF2: // For SSE2 switch (0xFF & *ip++) { case REX: case REXB: case REXX: case
         * REXXB: case REXR: case REXRB: case REXRX: case REXRXB: case REXW: case REXWB: case REXWX: case REXWXB: case
         * REXWR: case REXWRB: case REXWRX: case REXWRXB: assert compilation.target.arch.is64bit() :
         * "found 64bit prefix"; ip++; default: ip++; } hasDisp32 = true; // has both kinds of operands! break;
         *
         * default: Util.shouldNotReachHere(); }
         *
         * assert(which != WhichOperand.call32operand, "instruction is not a call, jmp, or jcc");
         *
         * assert !compilation.target.arch.is64bit() || which != WhichOperand.immOperand :
         * "instruction is not a movq reg, imm64"; assert compilation.target.arch.is64bit() || (which !=
         * WhichOperand.immOperand || hasDisp32) : "instruction has no imm32 field"; assert (which !=
         * WhichOperand.disp32operand || hasDisp32) : "instruction has no disp32 field";
         *
         * // parse the output of emitOperand int op2 = 0xFF & *ip++; int base = op2 & 0x07; int op3 = -1; int b100 = 4;
         * int b101 = 5; if (base == b100 && (op2 >> 6) != 3) { op3 = 0xFF & *ip++; base = op3 & 0x07; // refetch the
         * base } // now ip points at the disp (if any)
         *
         * switch (op2 >> 6) { case 0: // [00 reg 100][ss index base] // [00 reg 100][00 100 esp] // [00 reg base] //
         * [00 reg 100][ss index 101][disp32] // [00 reg 101] [disp32]
         *
         * if (base == b101) { if (which == WhichOperand.disp32operand) return ip; // caller wants the disp32 ip += 4;
         * // skip the disp32 } break;
         *
         * case 1: // [01 reg 100][ss index base][disp8] // [01 reg 100][00 100 esp][disp8] // [01 reg base] [disp8] ip
         * += 1; // skip the disp8 break;
         *
         * case 2: // [10 reg 100][ss index base][disp32] // [10 reg 100][00 100 esp][disp32] // [10 reg base] [disp32]
         * if (which == WhichOperand.disp32operand) return ip; // caller wants the disp32 ip += 4; // skip the disp32
         * break;
         *
         * case 3: // [11 reg base] (not a memory addressing mode) break; }
         *
         * if (which == endPcOperand) { return ip + tailSize; }
         *
         * assert !compilation.target.arch.is64bit() || (which == narrowOopOperand && !is64bit) :
         * "instruction is not a movl adr, imm32"; assert compilation.target.arch.is64bit() || (which ==
         * WhichOperand.immOperand) : "instruction has only an imm field"; return ip;
         */

        throw Util.unimplemented();
    }

    Pointer locateNextInstruction(Pointer inst) {
        // Secretly share code with locateOperand:
        return locateOperand(inst, WhichOperand.endPcOperand);
    }

    boolean checkRelocation(RelocationHolder rspec, int format) {
        Pointer inst = instMark();
        assert inst != null && inst.smallerThan(pc()) : "must point to beginning of instruction";
        Pointer opnd;

        Relocation r = rspec.reloc();
        if (r.type() == RelocInfo.Type.none) {
            return true;
        } else if (r.isCall() || format == WhichOperand.call32operand.ordinal()) {
            opnd = locateOperand(inst, WhichOperand.call32operand);
        } else if (r.isData()) {
            assert format == WhichOperand.immOperand.ordinal() || format == WhichOperand.disp32operand.ordinal() ||
                            (!compilation.target.arch.is64bit() || format == WhichOperand.narrowOopOperand.ordinal()) : "format ok";
            opnd = locateOperand(inst, WhichOperand.values()[format]);
        } else {
            assert format == WhichOperand.immOperand.ordinal() : "cannot specify a format";
            return true;
        }
        assert opnd.value == pc().value : "must put operand where relocs can find it";
        return true;
    }

    void emitOperand32(X86Register reg, Address adr) {
        assert reg.encoding() < 8 : "no extended registers";
        assert !adr.baseNeedsRex() && !adr.indexNeedsRex() : "no extended registers";
        emitOperand(reg, adr.base, adr.index, adr.scale, adr.disp, adr.rspec, 0);
    }

    void emitOperand(X86Register reg, Address adr) {
        emitOperand(reg, adr, 0);
    }

    void emitOperand(X86Register reg, Address adr, int ripRelativeCorrection) {
        emitOperand(reg, adr.base, adr.index, adr.scale, adr.disp, adr.rspec, ripRelativeCorrection);
    }

    void emitOperand(Address adr, X86Register reg) {
        assert reg.isXMM();
        assert !adr.baseNeedsRex() && !adr.indexNeedsRex() : "no extended registers";
        emitOperand(reg, adr.base, adr.index, adr.scale, adr.disp, adr.rspec, 0);
    }

    void emitFarith(int b1, int b2, int i) {
        assert isByte(b1) && isByte(b2) : "wrong opcode";
        assert 0 <= i && i < 8 : "illegal stack offset";
        emitByte(b1);
        emitByte(b2 + i);
    }

    // Now the Assembler instruction (identical for 32/64 bits)

    void adcl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xD0, dst, imm32);
    }

    void adcl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x13);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void adcl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x13, 0xC0, dst, src);
    }

    void addl(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefix(dst);
            emitArithOperand(0x81, X86Register.rax, dst, imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void addl(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefix(dst, src);
            emitByte(0x01);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void addl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xC0, dst, imm32);
    }

    void addl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x03);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void addl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x03, 0xC0, dst, src);
    }

    void addrNop4() {
        // 4 bytes: NOP DWORD PTR [EAX+0]
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x40); // emitRm(cbuf, 0x1, EAXEnc, EAXEnc);
        emitByte(0); // 8-bits offset (1 byte)
    }

    void addrNop5() {
        // 5 bytes: NOP DWORD PTR [EAX+EAX*0+0] 8-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x44); // emitRm(cbuf, 0x1, EAXEnc, 0x4);
        emitByte(0x00); // emitRm(cbuf, 0x0, EAXEnc, EAXEnc);
        emitByte(0); // 8-bits offset (1 byte)
    }

    void addrNop7() {
        // 7 bytes: NOP DWORD PTR [EAX+0] 32-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x80); // emitRm(cbuf, 0x2, EAXEnc, EAXEnc);
        emitLong(0); // 32-bits offset (4 bytes)
    }

    void addrNop8() {
        // 8 bytes: NOP DWORD PTR [EAX+EAX*0+0] 32-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x84); // emitRm(cbuf, 0x2, EAXEnc, 0x4);
        emitByte(0x00); // emitRm(cbuf, 0x0, EAXEnc, EAXEnc);
        emitLong(0); // 32-bits offset (4 bytes)
    }

    void addsd(X86Register dst, X86Register src) {
        assert dst.isXMM() && src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x58);
        emitByte(0xC0 | encode);
    }

    void addsd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x58);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void addss(X86Register dst, X86Register src) {
        assert dst.isXMM() && src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x58);
        emitByte(0xC0 | encode);
    }

    void addss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x58);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void andl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xE0, dst, imm32);
    }

    void andl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x23);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void andl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x23, 0xC0, dst, src);
    }

    void andpd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x54);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void bsfl(X86Register dst, X86Register src) {
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBC);
        emitByte(0xC0 | encode);
    }

    void bsrl(X86Register dst, X86Register src) {
        assert !compilation.target.supportsLzcnt() : "encoding is treated as LZCNT";
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    void bswapl(X86Register reg) { // bswap
        int encode = prefixAndEncode(reg.encoding());
        emitByte(0x0F);
        emitByte(0xC8 | encode);
    }

    void call(Label l, RelocInfo.Type rtype) {
        // suspect disp32 is always good
        int operand = WhichOperand.disp32operand.ordinal();
        if (!compilation.target.arch.is64bit()) {
            operand = WhichOperand.immOperand.ordinal();
        }

        if (l.isBound()) {
            int longSize = 5;
            int offs = Util.safeToInt(target(l).value - pc().value);
            assert offs <= 0 : "assembler error";
            this.setInstMark();
            try {
                // 1110 1000 #32-bit disp
                emitByte(0xE8);
                emitData(offs - longSize, rtype, operand);
            } finally {
                this.clearInstMark();
            }
        } else {
            this.setInstMark();
            try {
                // 1110 1000 #32-bit disp
                l.addPatchAt(code(), locator());

                emitByte(0xE8);
                emitData(0, rtype, operand);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void call(X86Register dst) {
        // this may be true but dbx disassembles it as if it
        // were 32bits...
        // int encode = prefixAndEncode(dst.encoding());
        // if (offset() != x) assert dst.encoding() >= 8 : "what?";
        int encode = prefixqAndEncode(dst.encoding());

        emitByte(0xFF);
        emitByte(0xD0 | encode);
    }

    void call(Address adr) {
        this.setInstMark();
        try {
            prefix(adr);
            emitByte(0xFF);
            emitOperand(X86Register.rdx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void callLiteral(Pointer entry, RelocationHolder rspec) {
        assert entry != null : "call most probably wrong";
        this.setInstMark();
        try {
            emitByte(0xE8);
            long disp = entry.value - (codePos.value + Util.sizeofInt());
            assert isSimm32(disp) : "must be 32bit offset (call2)";
            // Technically, should use WhichOperand.call32operand, but this format is
            // implied by the fact that we're emitting a call instruction.

            WhichOperand operand = WhichOperand.disp32operand;
            if (!compilation.target.arch.is64bit()) {
                operand = WhichOperand.call32operand;
            }
            emitData((int) disp, rspec, operand);
        } finally {
            this.clearInstMark();
        }
    }

    void cdql() {
        emitByte(0x99);
    }

    void cmovl(Condition cc, X86Register dst, X86Register src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsCmov() : "illegal instruction";
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitByte(0xC0 | encode);
    }

    void cmovl(Condition cc, X86Register dst, Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsCmov() : "illegal instruction";
        prefix(src, dst);
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitOperand(dst, src);
    }

    void cmpb(Address dst, int imm8) {
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0x80);
            emitOperand(X86Register.rdi, dst, 1);
            emitByte(imm8);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpl(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0x81);
            emitOperand(X86Register.rdi, dst, 4);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xF8, dst, imm32);
    }

    void cmpl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x3B, 0xC0, dst, src);
    }

    void cmpl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x3B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpw(Address dst, int imm16) {
        this.setInstMark();
        try {
            assert !dst.baseNeedsRex() && !dst.indexNeedsRex() : "no extended registers";
            emitByte(0x66);
            emitByte(0x81);
            emitOperand(X86Register.rdi, dst, 2);
            emitWord(imm16);
        } finally {
            this.clearInstMark();
        }
    }

    // The 32-bit cmpxchg compares the value at adr with the contents of X86Register.rax,
    // and stores reg into adr if so; otherwise, the value at adr is loaded into X86Register.rax,.
    // The ZF is set if the compared values were equal, and cleared otherwise.
    void cmpxchgl(X86Register reg, Address adr) { // cmpxchg
        if ((C1XOptions.Atomics & 2) != 0) {
            // caveat: no instructionmark, so this isn't relocatable.
            // Emit a synthetic, non-atomic, CAS equivalent.
            // Beware. The synthetic form sets all ICCs, not just ZF.
            // cmpxchg r,[m] is equivalent to X86Register.rax, = CAS (m, X86Register.rax, r)
            cmpl(X86Register.rax, adr);
            movl(X86Register.rax, adr);
            if (reg != X86Register.rax) {
                Label l = new Label();
                jcc(Condition.notEqual, l);
                movl(adr, reg);
                bind(l);
            }
        } else {
            this.setInstMark();
            try {
                prefix(adr, reg);
                emitByte(0x0F);
                emitByte(0xB1);
                emitOperand(reg, adr);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void comisd(X86Register dst, Address src) {
        assert dst.isXMM();
        // NOTE: dbx seems to decode this as comiss even though the
        // 0x66 is there. Strangly ucomisd comes out correct
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0x66);
        comiss(dst, src);
    }

    void comiss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();

        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x2F);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void cvtdq2pd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xE6);
        emitByte(0xC0 | encode);
    }

    void cvtdq2ps(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5B);
        emitByte(0xC0 | encode);
    }

    void cvtsd2ss(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5A);
        emitByte(0xC0 | encode);
    }

    void cvtsi2sdl(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    void cvtsi2ssl(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    void cvtss2sd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5A);
        emitByte(0xC0 | encode);
    }

    void cvttsd2sil(X86Register dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    void cvttss2sil(X86Register dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    void decl(Address dst) {
        // Don't use it directly. Use Macrodecrement() instead.
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0xFF);
            emitOperand(X86Register.rcx, dst);
            this.setInstMark();
        } finally {
            this.clearInstMark();
        }
    }

    void divsd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x5E);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void divsd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5E);
        emitByte(0xC0 | encode);
    }

    void divss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x5E);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void divss(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5E);
        emitByte(0xC0 | encode);
    }

    void emms() {
        assert compilation.target.supportsMmx();
        emitByte(0x0F);
        emitByte(0x77);
    }

    void hlt() {
        emitByte(0xF4);
    }

    void idivl(X86Register src) {
        int encode = prefixAndEncode(src.encoding());
        emitByte(0xF7);
        emitByte(0xF8 | encode);
    }

    void imull(X86Register dst, X86Register src) {
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xAF);
        emitByte(0xC0 | encode);
    }

    void imull(X86Register dst, X86Register src, int value) {
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        if (is8bit(value)) {
            emitByte(0x6B);
            emitByte(0xC0 | encode);
            emitByte(value);
        } else {
            emitByte(0x69);
            emitByte(0xC0 | encode);
            emitLong(value);
        }
    }

    void incl(Address dst) {
        // Don't use it directly. Use Macroincrement() instead.
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0xFF);
            emitOperand(X86Register.rax, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void jcc(Condition cc, Label l) {
        jcc(cc, l, RelocInfo.Type.none);
    }

    void jcc(Condition cc, Label l, RelocInfo.Type rtype) {
        this.setInstMark();
        try {
            relocate(rtype);
            assert (0 <= cc.value) && (cc.value < 16) : "illegal cc";
            if (l.isBound()) {
                Pointer dst = target(l);
                assert dst != null : "jcc most probably wrong";

                int shortSize = 2;
                int longSize = 6;
                long offs = dst.value - codePos.value;
                if (rtype == RelocInfo.Type.none && Util.is8bit(offs - shortSize)) {
                    // 0111 tttn #8-bit disp
                    emitByte(0x70 | cc.value);
                    emitByte((int) ((offs - shortSize) & 0xFF));
                } else {
                    // 0000 1111 1000 tttn #32-bit disp
                    assert isSimm32(offs - longSize) : "must be 32bit offset (call4)";
                    emitByte(0x0F);
                    emitByte(0x80 | cc.value);
                    emitLong(offs - longSize);
                }
            } else {
                // Note: could eliminate cond. jumps to this jump if condition
                // is the same however, seems to be rather unlikely case.
                // Note: use jccb() if label to be bound is very close to get
                // an 8-bit displacement
                l.addPatchAt(code(), locator());
                emitByte(0x0F);
                emitByte(0x80 | cc.value);
                emitLong(0);
            }
        } finally {
            this.clearInstMark();
        }
    }

    void jccb(Condition cc, Label l) {
        if (l.isBound()) {
            int shortSize = 2;
            Pointer entry = target(l);
            assert Util.is8bit(entry.value - (codePos.value + shortSize)) : "Dispacement too large for a short jmp";
            long offs = entry.value - codePos.value;
            // 0111 tttn #8-bit disp
            emitByte(0x70 | cc.value);
            emitByte((int) ((offs - shortSize) & 0xFF));
        } else {
            this.setInstMark();
            try {
                l.addPatchAt(code(), locator());
                emitByte(0x70 | cc.value);
                emitByte(0);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void jmp(Address adr) {
        this.setInstMark();
        try {
            prefix(adr);
            emitByte(0xFF);
            emitOperand(X86Register.rsp, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void jmp(Label l, RelocInfo.Type rtype) {
        if (l.isBound()) {
            Pointer entry = target(l);
            assert entry != null : "jmp most probably wrong";
            this.setInstMark();
            try {
                int shortSize = 2;
                int longSize = 5;
                long offs = entry.value - codePos.value;
                if (rtype == RelocInfo.Type.none && Util.is8bit(offs - shortSize)) {
                    emitByte(0xEB);
                    emitByte((offs - shortSize) & 0xFF);
                } else {
                    emitByte(0xE9);
                    emitLong(offs - longSize);
                }
            } finally {
                this.clearInstMark();
            }
        } else {
            // By default, forward jumps are always 32-bit displacements, since
            // we can't yet know where the label will be bound. If you're sure that
            // the forward jump will not run beyond 256 bytes, use jmpb to
            // force an 8-bit displacement.
            this.setInstMark();
            try {
                relocate(rtype);
                l.addPatchAt(code(), locator());
                emitByte(0xE9);
                emitLong(0);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void jmp(X86Register entry) {
        int encode = prefixAndEncode(entry.encoding());
        emitByte(0xFF);
        emitByte(0xE0 | encode);
    }

    void jmpLiteral(Pointer dest, RelocationHolder rspec) {
        this.setInstMark();
        try {
            emitByte(0xE9);
            assert dest != null : "must have a target";
            long disp = dest.value - (codePos.value + Util.sizeofInt());
            assert isSimm32(disp) : "must be 32bit offset (jmp)";
            emitData((int) disp, rspec.reloc().type(), WhichOperand.call32operand.ordinal());
        } finally {
            this.clearInstMark();
        }
    }

    void jmpb(Label l) {
        if (l.isBound()) {
            int shortSize = 2;
            Pointer entry = target(l);
            assert entry != null : "jmp most probably wrong";
            assert Util.is8bit((entry.value - codePos.value) + shortSize) : "Dispacement too large for a short jmp";
            long offs = entry.value - codePos.value;
            emitByte(0xEB);
            emitByte((offs - shortSize) & 0xFF);
        } else {
            this.setInstMark();
            try {
                l.addPatchAt(code(), locator());
                emitByte(0xEB);
                emitByte(0);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void ldmxcsr(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            prefix(src);
            emitByte(0x0F);
            emitByte(0xAE);
            emitOperand(X86Register.fromEncoding(2), src);
        } finally {
            this.clearInstMark();
        }
    }

    void leal(X86Register dst, Address src) {
        this.setInstMark();
        try {
            if (compilation.target.arch.is64bit()) {
                emitByte(0x67); // addr32
                prefix(src, dst);
            }
            emitByte(0x8D);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void lock() {
        if ((C1XOptions.Atomics & 1) != 0) {
            // Emit either nothing, a NOP, or a NOP: prefix
            emitByte(0x90);
        } else {
            emitByte(0xF0);
        }
    }

    void lzcntl(X86Register dst, X86Register src) {
        assert compilation.target.supportsLzcnt() : "encoding is treated as BSR";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    // Emit mfence instruction
    void mfence() {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        emitByte(0x0F);
        emitByte(0xAE);
        emitByte(0xF0);
    }

    void mov(X86Register dst, X86Register src) {
        if (compilation.target.arch.is64bit()) {
            movq(dst, src);
        } else {
            movl(dst, src);
        }
    }

    void movapd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        int dstenc = dst.encoding();
        int srcenc = src.encoding();
        emitByte(0x66);
        if (dstenc < 8) {
            if (srcenc >= 8) {
                prefix(Prefix.REXB);
                srcenc -= 8;
            }
        } else {
            if (srcenc < 8) {
                prefix(Prefix.REXR);
            } else {
                prefix(Prefix.REXRB);
                srcenc -= 8;
            }
            dstenc -= 8;
        }
        emitByte(0x0F);
        emitByte(0x28);
        emitByte(0xC0 | dstenc << 3 | srcenc);
    }

    void movaps(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE() : "unsupported";
        int dstenc = dst.encoding();
        int srcenc = src.encoding();
        if (dstenc < 8) {
            if (srcenc >= 8) {
                prefix(Prefix.REXB);
                srcenc -= 8;
            }
        } else {
            if (srcenc < 8) {
                prefix(Prefix.REXR);
            } else {
                prefix(Prefix.REXRB);
                srcenc -= 8;
            }
            dstenc -= 8;
        }
        emitByte(0x0F);
        emitByte(0x28);
        emitByte(0xC0 | dstenc << 3 | srcenc);
    }

    void movb(X86Register dst, Address src) {
        assert compilation.target.arch.is64bit() || dst.hasByteRegister() : "must have byte register";

        this.setInstMark();
        try {
            prefix(src, dst); // , true)
            emitByte(0x8A);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movb(Address dst, int imm8) {
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0xC6);
            emitOperand(X86Register.rax, dst, 1);
            emitByte(imm8);
        } finally {
            this.clearInstMark();
        }
    }

    void movb(Address dst, X86Register src) {
        assert src.hasByteRegister() : "must have byte register";
        this.setInstMark();
        try {
            prefix(dst, src); // , true)
            emitByte(0x88);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void movdl(X86Register dst, X86Register src) {
        if (dst.isXMM()) {
            assert dst.isXMM();
            assert !src.isXMM() : "does this hold?";
            assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
            emitByte(0x66);
            int encode = prefixAndEncode(dst.encoding(), src.encoding());
            emitByte(0x0F);
            emitByte(0x6E);
            emitByte(0xC0 | encode);
        } else if (src.isXMM()) {
            assert src.isXMM();
            assert !dst.isXMM();
            assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
            emitByte(0x66);
            // swap src/dst to get correct prefix
            int encode = prefixAndEncode(src.encoding(), dst.encoding());
            emitByte(0x0F);
            emitByte(0x7E);
            emitByte(0xC0 | encode);
        }
    }

    void movdqa(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x6F);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movdqa(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        emitByte(0x66);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x6F);
        emitByte(0xC0 | encode);
    }

    void movdqa(Address dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        this.setInstMark();
        try {

            emitByte(0x66);
            prefix(dst, src);
            emitByte(0x0F);
            emitByte(0x7F);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void movdqu(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x6F);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }

    }

    void movdqu(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        this.setInstMark();
        try {

            emitByte(0xF3);
            int encode = prefixqAndEncode(dst.encoding(), src.encoding());
            emitByte(0x0F);
            emitByte(0x6F);
            emitByte(0xC0 | encode);
        } finally {
            this.clearInstMark();
        }

    }

    void movdqu(Address dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(dst, src);
            emitByte(0x0F);
            emitByte(0x7F);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }

    }

    void movl(X86Register dst, int imm32) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xB8 | encode);
        emitLong(imm32);
    }

    void movl(X86Register dst, X86Register src) {
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x8B);
        emitByte(0xC0 | encode);
    }

    void movl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x8B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }

    }

    void movl(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0xC7);
            emitOperand(X86Register.rax, dst, 4);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }

    }

    void movl(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefix(dst, src);
            emitByte(0x89);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    // New cpus require to use movsd and movss to avoid partial register stall
    // when loading from memory. But for old Opteron use movlpd instead of movsd.
    // The selection is done in Macromovdbl() and movflt().
    void movlpd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x12);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }

    }

    void movq(X86Register dst, Address src) {
        if (dst.isMMX()) {
            assert dst.isMMX();
            assert compilation.target.supportsMMX() : "unsupported";

            emitByte(0x0F);
            emitByte(0x6F);
            emitOperand(dst, src);
        } else if (dst.isXMM()) {
            assert dst.isXMM();
            assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

            this.setInstMark();
            try {
                emitByte(0xF3);
                prefix(src, dst);
                emitByte(0x0F);
                emitByte(0x7E);
                emitOperand(dst, src);
            } finally {
                this.clearInstMark();
            }
        } else {
            this.setInstMark();
            try {
                prefixq(src, dst);
                emitByte(0x8B);
                emitOperand(dst, src);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void movq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x8B);
        emitByte(0xC0 | encode);
    }

    void movq(Address dst, X86Register src) {
        if (src.isMMX()) {
            assert src.isMMX();
            assert compilation.target.supportsMMX() : "unsupported";
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
            assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

            this.setInstMark();
            try {
                emitByte(0x66);
                prefix(dst, src);
                emitByte(0x0F);
                emitByte(0xD6);
                emitOperand(src, dst);
            } finally {
                this.clearInstMark();
            }
        } else {

            this.setInstMark();
            try {
                prefixq(dst, src);
                emitByte(0x89);
                emitOperand(src, dst);
            } finally {
                this.clearInstMark();
            }
        }
    }

    void movsbl(X86Register dst, Address src) { // movsxb
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0xBE);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movsbl(X86Register dst, X86Register src) { // movsxb
        assert compilation.target.arch.is64bit() || src.hasByteRegister() : "must have byte register";
        int encode = prefixAndEncode(dst.encoding(), src.encoding(), true);
        emitByte(0x0F);
        emitByte(0xBE);
        emitByte(0xC0 | encode);
    }

    void movsd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x10);
        emitByte(0xC0 | encode);
    }

    void movsd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x10);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movsd(Address dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(dst, src);
            emitByte(0x0F);
            emitByte(0x11);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void movss(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE() : "unsupported";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x10);
        emitByte(0xC0 | encode);
    }

    void movss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE() : "unsupported";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x10);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movss(Address dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE() : "unsupported";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(dst, src);
            emitByte(0x0F);
            emitByte(0x11);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void movswl(X86Register dst, Address src) { // movsxw
        assert dst.isXMM();
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0xBF);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movswl(X86Register dst, X86Register src) { // movsxw
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBF);
        emitByte(0xC0 | encode);
    }

    void movw(Address dst, int imm16) {
        this.setInstMark();
        try {

            emitByte(0x66); // switch to 16-bit mode
            prefix(dst);
            emitByte(0xC7);
            emitOperand(X86Register.rax, dst, 2);
            emitWord(imm16);
        } finally {
            this.clearInstMark();
        }
    }

    void movw(X86Register dst, Address src) {
        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x8B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movw(Address dst, X86Register src) {
        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(dst, src);
            emitByte(0x89);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void movzbl(X86Register dst, Address src) { // movzxb
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0xB6);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movzbl(X86Register dst, X86Register src) { // movzxb
        assert compilation.target.arch.is64bit() || src.hasByteRegister() : "must have byte register";
        int encode = prefixAndEncode(dst.encoding(), src.encoding(), true);
        emitByte(0x0F);
        emitByte(0xB6);
        emitByte(0xC0 | encode);
    }

    void movzwl(X86Register dst, Address src) { // movzxw
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0xB7);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movzwl(X86Register dst, X86Register src) { // movzxw
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xB7);
        emitByte(0xC0 | encode);
    }

    void mull(Address src) {
        this.setInstMark();
        try {
            prefix(src);
            emitByte(0xF7);
            emitOperand(X86Register.rsp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void mull(X86Register src) {
        int encode = prefixAndEncode(src.encoding());
        emitByte(0xF7);
        emitByte(0xE0 | encode);
    }

    void mulsd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x59);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void mulsd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() : "unsupported";

        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x59);
        emitByte(0xC0 | encode);
    }

    void mulss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE() : "unsupported";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x59);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void mulss(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE() : "unsupported";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x59);
        emitByte(0xC0 | encode);
    }

    void negl(X86Register dst) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xF7);
        emitByte(0xD8 | encode);
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

        if (C1XOptions.UseAddressNop && compilation.target.isIntel()) {
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
        if (C1XOptions.UseAddressNop && compilation.target.isAmd()) {
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

    void notl(X86Register dst) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xF7);
        emitByte(0xD0 | encode);
    }

    void orl(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0x81);
            emitOperand(X86Register.rcx, dst, 4);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void orl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xC8, dst, imm32);
    }

    void orl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void orl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x0B, 0xC0, dst, src);
    }

    void pcmpestri(X86Register dst, Address src, int imm8) {
        assert dst.isXMM();
        assert compilation.target.supportsSse42() : "";

        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x3A);
            emitByte(0x61);
            emitOperand(dst, src);
            emitByte(imm8);
        } finally {
            this.clearInstMark();
        }
    }

    void pcmpestri(X86Register dst, X86Register src, int imm8) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.supportsSse42() : "";

        emitByte(0x66);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x3A);
        emitByte(0x61);
        emitByte(0xC0 | encode);
        emitByte(imm8);
    }

    // generic
    void pop(X86Register dst) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0x58 | encode);
    }

    void popcntl(X86Register dst, Address src) {
        assert compilation.target.supportsPopcnt() : "must support";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0xB8);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void popcntl(X86Register dst, X86Register src) {
        assert compilation.target.supportsPopcnt() : "must support";
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xB8);
        emitByte(0xC0 | encode);
    }

    void popf() {
        emitByte(0x9D);
    }

    void popl(Address dst) {
        // NOTE: this will adjust stack by 8byte on 64bits
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0x8F);
            emitOperand(X86Register.rax, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void prefetchPrefix(Address src) {
        prefix(src);
        emitByte(0x0F);
    }

    void prefetchnta(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            prefetchPrefix(src);
            emitByte(0x18);
            emitOperand(X86Register.rax, src); // 0, src
        } finally {
            this.clearInstMark();
        }
    }

    void prefetchr(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supports3DNOW();
        this.setInstMark();
        try {
            prefetchPrefix(src);
            emitByte(0x0D);
            emitOperand(X86Register.rax, src); // 0, src
        } finally {
            this.clearInstMark();
        }
    }

    void prefetcht0(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            prefetchPrefix(src);
            emitByte(0x18);
            emitOperand(X86Register.rcx, src); // 1, src
        } finally {
            this.clearInstMark();
        }
    }

    void prefetcht1(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            prefetchPrefix(src);
            emitByte(0x18);
            emitOperand(X86Register.rdx, src); // 2, src
        } finally {
            this.clearInstMark();
        }
    }

    void prefetcht2(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            prefetchPrefix(src);
            emitByte(0x18);
            emitOperand(X86Register.rbx, src); // 3, src
        } finally {
            this.clearInstMark();
        }
    }

    void prefetchw(Address src) {
        assert compilation.target.arch.is64bit() || compilation.target.supports3DNOW();
        this.setInstMark();
        try {
            prefetchPrefix(src);
            emitByte(0x0D);
            emitOperand(X86Register.rcx, src); // 1, src
        } finally {
            this.clearInstMark();
        }
    }

    void prefix(Prefix p) {
        aByte(p.value);
    }

    void pshufd(X86Register dst, X86Register src, int mode) {
        assert dst.isXMM();
        assert src.isXMM();
        assert isByte(mode) : "invalid value";
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();

        emitByte(0x66);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x70);
        emitByte(0xC0 | encode);
        emitByte(mode & 0xFF);

    }

    void pshufd(X86Register dst, Address src, int mode) {
        assert dst.isXMM();
        assert isByte(mode) : "invalid value";
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();

        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x70);
            emitOperand(dst, src);
            emitByte(mode & 0xFF);
        } finally {
            this.clearInstMark();
        }
    }

    void pshuflw(X86Register dst, X86Register src, int mode) {
        assert dst.isXMM();
        assert src.isXMM();
        assert isByte(mode) : "invalid value";
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();

        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x70);
        emitByte(0xC0 | encode);
        emitByte(mode & 0xFF);
    }

    void pshuflw(X86Register dst, Address src, int mode) {
        assert dst.isXMM();
        assert isByte(mode) : "invalid value";
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();

        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst); // QQ new
            emitByte(0x0F);
            emitByte(0x70);
            emitOperand(dst, src);
            emitByte(mode & 0xFF);
        } finally {
            this.clearInstMark();
        }
    }

    void psrlq(X86Register dst, int shift) {
        assert dst.isXMM();
        // HMM Table D-1 says sse2 or mmx
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();

        int encode = prefixqAndEncode(X86Register.xmm2.encoding(), dst.encoding());
        emitByte(0x66);
        emitByte(0x0F);
        emitByte(0x73);
        emitByte(0xC0 | encode);
        emitByte(shift);
    }

    void ptest(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.supportsSse41() : "";

        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x38);
            emitByte(0x17);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void ptest(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.supportsSse41() : "";

        emitByte(0x66);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x38);
        emitByte(0x17);
        emitByte(0xC0 | encode);
    }

    void punpcklbw(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0x66);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x60);
        emitByte(0xC0 | encode);
    }

    void push(int imm32) {
        // in 64bits we push 64bits onto the stack but only
        // take a 32bit immediate
        emitByte(0x68);
        emitLong(imm32);
    }

    void push(X86Register src) {
        int encode = prefixAndEncode(src.encoding());
        emitByte(0x50 | encode);
    }

    void pushf() {
        emitByte(0x9C);
    }

    void pushl(Address src) {
        // Note this will push 64bit on 64bit
        this.setInstMark();
        try {
            prefix(src);
            emitByte(0xFF);
            emitOperand(X86Register.rsi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void pxor(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0xEF);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void pxor(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0x66);
            int encode = prefixAndEncode(dst.encoding(), src.encoding());
            emitByte(0x0F);
            emitByte(0xEF);
            emitByte(0xC0 | encode);
        } finally {
            this.clearInstMark();
        }
    }

    void rcll(X86Register dst, int imm8) {
        assert isShiftCount(imm8) : "illegal shift count";
        int encode = prefixAndEncode(dst.encoding());
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xD0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xD0 | encode);
            emitByte(imm8);
        }
    }

    // copies data from [esi] to [edi] using X86Register.rcx pointer sized words
    // generic
    void repMov() {
        emitByte(0xF3);
        // MOVSQ
        if (compilation.target.arch.is64bit()) {
            prefix(Prefix.REXW);
        }
        emitByte(0xA5);
    }

    // sets X86Register.rcx pointer sized words with X86Register.rax, value at [edi]
    // generic
    void repSet() { // repSet
        emitByte(0xF3);
        // STOSQ
        if (compilation.target.arch.is64bit()) {
            prefix(Prefix.REXW);
        }
        emitByte(0xAB);
    }

    // scans X86Register.rcx pointer sized words at [edi] for occurance of X86Register.rax,
    // generic
    void repneScan() { // repneScan
        emitByte(0xF2);
        // SCASQ
        if (compilation.target.arch.is64bit()) {
            prefix(Prefix.REXW);
        }
        emitByte(0xAF);
    }

    // scans X86Register.rcx 4 byte words at [edi] for occurance of X86Register.rax,
    // generic
    void repneScanl() { // repneScan
        assert compilation.target.arch.is64bit();
        emitByte(0xF2);
        // SCASL
        emitByte(0xAF);
    }

    void ret(int imm16) {
        if (imm16 == 0) {
            emitByte(0xC3);
        } else {
            emitByte(0xC2);
            emitWord(imm16);
        }
    }

    void sahf() {
        // Not supported in 64bit mode
        if (compilation.target.arch.is64bit()) {
            Util.shouldNotReachHere();
        }
        emitByte(0x9E);
    }

    void sarl(X86Register dst, int imm8) {
        int encode = prefixAndEncode(dst.encoding());
        assert isShiftCount(imm8) : "illegal shift count";
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xF8 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xF8 | encode);
            emitByte(imm8);
        }
    }

    void sarl(X86Register dst) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xD3);
        emitByte(0xF8 | encode);
    }

    void sbbl(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefix(dst);
            emitArithOperand(0x81, X86Register.rbx, dst, imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void sbbl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xD8, dst, imm32);
    }

    void sbbl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x1B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void sbbl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x1B, 0xC0, dst, src);
    }

    void setb(Condition cc, X86Register dst) {
        assert 0 <= cc.value && cc.value < 16 : "illegal cc";
        int encode = prefixAndEncode(dst.encoding(), true);
        emitByte(0x0F);
        emitByte(0x90 | cc.value);
        emitByte(0xC0 | encode);
    }

    void shll(X86Register dst, int imm8) {
        assert isShiftCount(imm8) : "illegal shift count";
        int encode = prefixAndEncode(dst.encoding());
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xE0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xE0 | encode);
            emitByte(imm8);
        }
    }

    void shll(X86Register dst) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xD3);
        emitByte(0xE0 | encode);
    }

    void shrl(X86Register dst, int imm8) {
        assert isShiftCount(imm8) : "illegal shift count";
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xC1);
        emitByte(0xE8 | encode);
        emitByte(imm8);
    }

    void shrl(X86Register dst) {
        int encode = prefixAndEncode(dst.encoding());
        emitByte(0xD3);
        emitByte(0xE8 | encode);
    }

    // copies a single word from [esi] to [edi]
    void smovl() {
        emitByte(0xA5);
    }

    void sqrtsd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        // HMM Table D-1 says sse2
        // assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x51);
        emitByte(0xC0 | encode);
    }

    void stmxcsr(Address dst) {
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0x0F);
            emitByte(0xAE);
            emitOperand(X86Register.fromEncoding(3), dst);
        } finally {
            this.clearInstMark();
        }
    }

    void subl(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefix(dst);
            if (is8bit(imm32)) {
                emitByte(0x83);
                emitOperand(X86Register.rbp, dst, 1);
                emitByte(imm32 & 0xFF);
            } else {
                emitByte(0x81);
                emitOperand(X86Register.rbp, dst, 4);
                emitLong(imm32);
            }
        } finally {
            this.clearInstMark();
        }
    }

    void subl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xE8, dst, imm32);
    }

    void subl(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefix(dst, src);
            emitByte(0x29);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void subl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x2B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void subl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x2B, 0xC0, dst, src);
    }

    void subsd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5C);
        emitByte(0xC0 | encode);
    }

    void subsd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x5C);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void subss(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x5C);
        emitByte(0xC0 | encode);
    }

    void subss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x5C);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void testb(X86Register dst, int imm8) {
        assert compilation.target.arch.is64bit() || dst.hasByteRegister() : "must have byte register";
        prefixAndEncode(dst.encoding(), true);
        emitArithB(0xF6, 0xC0, dst, imm8);
    }

    void testl(X86Register dst, int imm32) {
        // not using emitArith because test
        // doesn't support sign-extension of
        // 8bit operands
        int encode = dst.encoding();
        if (encode == 0) {
            emitByte(0xA9);
        } else {
            encode = prefixAndEncode(encode);
            emitByte(0xF7);
            emitByte(0xC0 | encode);
        }
        emitLong(imm32);
    }

    void testl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x85, 0xC0, dst, src);
    }

    void testl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x85);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void ucomisd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0x66);
        ucomiss(dst, src);
    }

    void ucomisd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0x66);
        ucomiss(dst, src);
    }

    void ucomiss(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();

        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x2E);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void ucomiss(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2E);
        emitByte(0xC0 | encode);
    }

    void xaddl(Address dst, X86Register src) {
        assert src.isXMM();
        this.setInstMark();
        try {
            prefix(dst, src);
            emitByte(0x0F);
            emitByte(0xC1);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void xchgl(X86Register dst, Address src) { // xchg
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x87);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void xchgl(X86Register dst, X86Register src) {
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x87);
        emitByte(0xc0 | encode);
    }

    void xorl(X86Register dst, int imm32) {
        prefix(dst);
        emitArith(0x81, 0xF0, dst, imm32);
    }

    void xorl(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x33);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void xorl(X86Register dst, X86Register src) {
        prefixAndEncode(dst.encoding(), src.encoding());
        emitArith(0x33, 0xC0, dst, src);
    }

    void xorpd(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0x66);
        xorps(dst, src);
    }

    void xorpd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0x66);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x57);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void xorps(X86Register dst, X86Register src) {

        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        int encode = prefixAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x57);
        emitByte(0xC0 | encode);
    }

    void xorps(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        this.setInstMark();
        try {
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x57);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    // 32bit only pieces of the assembler

    void cmpLiteral32(X86Register src1, int imm32, RelocationHolder rspec) {
        assert compilation.target.arch.is32bit();
        // NO PREFIX AS NEVER 64BIT
        this.setInstMark();
        try {
            emitByte(0x81);
            emitByte(0xF8 | src1.encoding());
            emitData(imm32, rspec, 0);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpLiteral32(Address src1, int imm32, RelocationHolder rspec) {
        assert compilation.target.arch.is32bit();
        // NO PREFIX AS NEVER 64BIT (not even 32bit versions of 64bit regs
        this.setInstMark();
        try {
            emitByte(0x81);
            emitOperand(X86Register.rdi, src1);
            emitData(imm32, rspec, 0);
        } finally {
            this.clearInstMark();
        }
    }

    // The 64-bit (32bit platform) cmpxchg compares the value at adr with the contents of
    // X86Register.rdx:X86Register.rax,
    // and stores X86Register.rcx:X86Register.rbx into adr if so; otherwise, the value at adr is loaded
    // into X86Register.rdx:X86Register.rax. The ZF is set if the compared values were equal, and cleared otherwise.
    void cmpxchg8(Address adr) {
        assert compilation.target.arch.is32bit();
        this.setInstMark();
        try {
            emitByte(0x0F);
            emitByte(0xc7);
            emitOperand(X86Register.rcx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void decl(X86Register dst) {
        if (compilation.target.arch.is32bit()) {
            assert compilation.target.arch.is32bit();
            // Don't use it directly. Use Macrodecrementl() instead.
            emitByte(0x48 | dst.encoding());
        } else if (compilation.target.arch.is64bit()) {

            // Don't use it directly. Use Macrodecrementl() instead.
            // Use two-byte form (one-byte form is a REX prefix in 64-bit mode)
            int encode = prefixAndEncode(dst.encoding());
            emitByte(0xFF);
            emitByte(0xC8 | encode);
        } else {
            Util.shouldNotReachHere();
        }
    }

    // 64bit typically doesn't use the x87 but needs to for the trig funcs

    void fabs() {
        emitByte(0xD9);
        emitByte(0xE1);
    }

    void fadd(int i) {
        emitFarith(0xD8, 0xC0, i);
    }

    void faddD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rax, src);
        } finally {
            this.clearInstMark();
        }
    }

    void faddS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rax, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fadda(int i) {
        emitFarith(0xDC, 0xC0, i);
    }

    void faddp(int i) {
        emitFarith(0xDE, 0xC0, i);
    }

    void fchs() {
        emitByte(0xD9);
        emitByte(0xE0);
    }

    void fcom(int i) {
        emitFarith(0xD8, 0xD0, i);
    }

    void fcomp(int i) {
        emitFarith(0xD8, 0xD8, i);
    }

    void fcompD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rbx, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fcompS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rbx, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fcompp() {
        emitByte(0xDE);
        emitByte(0xD9);
    }

    void fcos() {
        emitByte(0xD9);
        emitByte(0xFF);
    }

    void fdecstp() {
        emitByte(0xD9);
        emitByte(0xF6);
    }

    void fdiv(int i) {
        emitFarith(0xD8, 0xF0, i);
    }

    void fdivD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rsi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fdivS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rsi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fdiva(int i) {
        emitFarith(0xDC, 0xF8, i);
    }

    // Note: The Intel manual (Pentium Processor User's Manual, Vol.3, 1994)
// is erroneous for some of the floating-point instructions below.

    void fdivp(int i) {
        emitFarith(0xDE, 0xF8, i); // ST(0) <- ST(0) / ST(1) and pop (Intel manual wrong)
    }

    void fdivr(int i) {
        emitFarith(0xD8, 0xF8, i);
    }

    void fdivrD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rdi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fdivrS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rdi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fdivra(int i) {
        emitFarith(0xDC, 0xF0, i);
    }

    void fdivrp(int i) {
        emitFarith(0xDE, 0xF0, i); // ST(0) <- ST(1) / ST(0) and pop (Intel manual wrong)
    }

    void ffree(int i) {
        emitFarith(0xDD, 0xC0, i);
    }

    void fildD(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDF);
            emitOperand32(X86Register.rbp, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fildS(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDB);
            emitOperand32(X86Register.rax, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fincstp() {
        emitByte(0xD9);
        emitByte(0xF7);
    }

    void finit() {
        emitByte(0x9B);
        emitByte(0xDB);
        emitByte(0xE3);
    }

    void fistS(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDB);
            emitOperand32(X86Register.rdx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fistpD(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDF);
            emitOperand32(X86Register.rdi, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fistpS(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDB);
            emitOperand32(X86Register.rbx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fld1() {
        emitByte(0xD9);
        emitByte(0xE8);
    }

    void fldD(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDD);
            emitOperand32(X86Register.rax, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fldS(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xD9);
            emitOperand32(X86Register.rax, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fldS(int index) {
        emitFarith(0xD9, 0xC0, index);
    }

    void fldX(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDB);
            emitOperand32(X86Register.rbp, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fldcw(Address src) {
        this.setInstMark();
        try {
            emitByte(0xd9);
            emitOperand32(X86Register.rbp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fldenv(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD9);
            emitOperand32(X86Register.rsp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fldlg2() {
        emitByte(0xD9);
        emitByte(0xEC);
    }

    void fldln2() {
        emitByte(0xD9);
        emitByte(0xED);
    }

    void fldz() {
        emitByte(0xD9);
        emitByte(0xEE);
    }

    void flog() {
        fldln2();
        fxch(1);
        fyl2x();
    }

    void flog10() {
        fldlg2();
        fxch(1);
        fyl2x();
    }

    void fmul(int i) {
        emitFarith(0xD8, 0xC8, i);
    }

    void fmulD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rcx, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fmulS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rcx, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fmula(int i) {
        emitFarith(0xDC, 0xC8, i);
    }

    void fmulp(int i) {
        emitFarith(0xDE, 0xC8, i);
    }

    void fnsave(Address dst) {
        this.setInstMark();
        try {
            emitByte(0xDD);
            emitOperand32(X86Register.rsi, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void fnstcw(Address src) {
        this.setInstMark();
        try {
            emitByte(0x9B);
            emitByte(0xD9);
            emitOperand32(X86Register.rdi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fnstswAx() {
        emitByte(0xdF);
        emitByte(0xE0);
    }

    void fprem() {
        emitByte(0xD9);
        emitByte(0xF8);
    }

    void fprem1() {
        emitByte(0xD9);
        emitByte(0xF5);
    }

    void frstor(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDD);
            emitOperand32(X86Register.rsp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fsin() {
        emitByte(0xD9);
        emitByte(0xFE);
    }

    void fsqrt() {
        emitByte(0xD9);
        emitByte(0xFA);
    }

    void fstD(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDD);
            emitOperand32(X86Register.rdx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fstS(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xD9);
            emitOperand32(X86Register.rdx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fstpD(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDD);
            emitOperand32(X86Register.rbx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fstpD(int index) {
        emitFarith(0xDD, 0xD8, index);
    }

    void fstpS(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xD9);
            emitOperand32(X86Register.rbx, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fstpX(Address adr) {
        this.setInstMark();
        try {
            emitByte(0xDB);
            emitOperand32(X86Register.rdi, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void fsub(int i) {
        emitFarith(0xD8, 0xE0, i);
    }

    void fsubD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rsp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fsubS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rsp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fsuba(int i) {
        emitFarith(0xDC, 0xE8, i);
    }

    void fsubp(int i) {
        emitFarith(0xDE, 0xE8, i); // ST(0) <- ST(0) - ST(1) and pop (Intel manual wrong)
    }

    void fsubr(int i) {
        emitFarith(0xD8, 0xE8, i);
    }

    void fsubrD(Address src) {
        this.setInstMark();
        try {
            emitByte(0xDC);
            emitOperand32(X86Register.rbp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fsubrS(Address src) {
        this.setInstMark();
        try {
            emitByte(0xD8);
            emitOperand32(X86Register.rbp, src);
        } finally {
            this.clearInstMark();
        }
    }

    void fsubra(int i) {
        emitFarith(0xDC, 0xE0, i);
    }

    void fsubrp(int i) {
        emitFarith(0xDE, 0xE0, i); // ST(0) <- ST(1) - ST(0) and pop (Intel manual wrong)
    }

    void ftan() {
        emitByte(0xD9);
        emitByte(0xF2);
        emitByte(0xDD);
        emitByte(0xD8);
    }

    void ftst() {
        emitByte(0xD9);
        emitByte(0xE4);
    }

    void fucomi(int i) {
        // make sure the instruction is supported (introduced for P6, together with cmov)
        Util.guarantee(compilation.target.supportsCmov(), "illegal instruction");
        emitFarith(0xDB, 0xE8, i);
    }

    void fucomip(int i) {
        // make sure the instruction is supported (introduced for P6, together with cmov)
        Util.guarantee(compilation.target.supportsCmov(), "illegal instruction");
        emitFarith(0xDF, 0xE8, i);
    }

    void fwait() {
        emitByte(0x9B);
    }

    void fxch(int i) {
        emitFarith(0xD9, 0xC8, i);
    }

    void fyl2x() {
        emitByte(0xD9);
        emitByte(0xF1);
    }

    void incl(X86Register dst) {
        if (compilation.target.arch.is32bit()) {
            assert compilation.target.arch.is32bit();
            // Don't use it directly. Use Macroincrementl() instead.
            emitByte(0x40 | dst.encoding());
        } else {
            // Don't use it directly. Use Macroincrementl() instead.
            // Use two-byte form (one-byte from is a REX prefix in 64-bit mode)
            int encode = prefixAndEncode(dst.encoding());
            emitByte(0xFF);
            emitByte(0xC0 | encode);
        }
    }

    void lea(X86Register dst, Address src) {
        if (compilation.target.arch.is32bit()) {
            assert compilation.target.arch.is32bit();
            leal(dst, src);
        } else {
            leaq(dst, src);
        }
    }

    void movLiteral32(Address dst, int imm32, RelocationHolder rspec) {
        assert compilation.target.arch.is32bit();
        this.setInstMark();
        try {
            emitByte(0xC7);
            emitOperand(X86Register.rax, dst);
            emitData(imm32, rspec, 0);
        } finally {
            this.clearInstMark();
        }
    }

    void movLiteral32(X86Register dst, int imm32, RelocationHolder rspec) {
        assert compilation.target.arch.is32bit();
        this.setInstMark();
        try {
            int encode = prefixAndEncode(dst.encoding());
            emitByte(0xB8 | encode);
            emitData(imm32, rspec, 0);
        } finally {
            this.clearInstMark();
        }
    }

    void popa() {

        if (compilation.target.arch.is64bit()) {
            // 32bit
            assert compilation.target.arch.is32bit();
            emitByte(0x61);
        } else {
            final int wordSize = compilation.target.arch.wordSize;
            // 64bit
            movq(X86Register.r15, new Address(X86Register.rsp, 0));
            movq(X86Register.r14, new Address(X86Register.rsp, wordSize));
            movq(X86Register.r13, new Address(X86Register.rsp, 2 * wordSize));
            movq(X86Register.r12, new Address(X86Register.rsp, 3 * wordSize));
            movq(X86Register.r11, new Address(X86Register.rsp, 4 * wordSize));
            movq(X86Register.r10, new Address(X86Register.rsp, 5 * wordSize));
            movq(X86Register.r9, new Address(X86Register.rsp, 6 * wordSize));
            movq(X86Register.r8, new Address(X86Register.rsp, 7 * wordSize));
            movq(X86Register.rdi, new Address(X86Register.rsp, 8 * wordSize));
            movq(X86Register.rsi, new Address(X86Register.rsp, 9 * wordSize));
            movq(X86Register.rbp, new Address(X86Register.rsp, 10 * wordSize));
            // skip rsp
            movq(X86Register.rbx, new Address(X86Register.rsp, 12 * wordSize));
            movq(X86Register.rdx, new Address(X86Register.rsp, 13 * wordSize));
            movq(X86Register.rcx, new Address(X86Register.rsp, 14 * wordSize));
            movq(X86Register.rax, new Address(X86Register.rsp, 15 * wordSize));

            addq(X86Register.rsp, 16 * wordSize);
        }
    }

    void pushLiteral32(int imm32, RelocationHolder rspec) {
        assert compilation.target.arch.is32bit();
        this.setInstMark();
        try {
            emitByte(0x68);
            emitData(imm32, rspec, 0);
        } finally {
            this.clearInstMark();
        }
    }

    void pusha() {

        if (compilation.target.arch.is32bit()) {
            // 32bit
            assert compilation.target.arch.is32bit();
            emitByte(0x60);
        } else {

            final int wordSize = compilation.target.arch.wordSize;

            // we have to store original rsp. ABI says that 128 bytes
            // below rsp are local scratch.
            movq(new Address(X86Register.rsp, -5 * wordSize), X86Register.rsp);

            subq(X86Register.rsp, 16 * wordSize);

            movq(new Address(X86Register.rsp, 15 * wordSize), X86Register.rax);
            movq(new Address(X86Register.rsp, 14 * wordSize), X86Register.rcx);
            movq(new Address(X86Register.rsp, 13 * wordSize), X86Register.rdx);
            movq(new Address(X86Register.rsp, 12 * wordSize), X86Register.rbx);
            // skip rsp
            movq(new Address(X86Register.rsp, 10 * wordSize), X86Register.rbp);
            movq(new Address(X86Register.rsp, 9 * wordSize), X86Register.rsi);
            movq(new Address(X86Register.rsp, 8 * wordSize), X86Register.rdi);
            movq(new Address(X86Register.rsp, 7 * wordSize), X86Register.r8);
            movq(new Address(X86Register.rsp, 6 * wordSize), X86Register.r9);
            movq(new Address(X86Register.rsp, 5 * wordSize), X86Register.r10);
            movq(new Address(X86Register.rsp, 4 * wordSize), X86Register.r11);
            movq(new Address(X86Register.rsp, 3 * wordSize), X86Register.r12);
            movq(new Address(X86Register.rsp, 2 * wordSize), X86Register.r13);
            movq(new Address(X86Register.rsp, wordSize), X86Register.r14);
            movq(new Address(X86Register.rsp, 0), X86Register.r15);
        }
    }

    void setByteIfNotZero(X86Register dst) {
        assert compilation.target.arch.is32bit();
        emitByte(0x0F);
        emitByte(0x95);
        emitByte(0xE0 | dst.encoding());
    }

    void shldl(X86Register dst, X86Register src) {
        assert compilation.target.arch.is32bit();
        emitByte(0x0F);
        emitByte(0xA5);
        emitByte(0xC0 | src.encoding() << 3 | dst.encoding());
    }

    void shrdl(X86Register dst, X86Register src) {
        assert compilation.target.arch.is32bit();
        emitByte(0x0F);
        emitByte(0xAD);
        emitByte(0xC0 | src.encoding() << 3 | dst.encoding());
    }

    // 64-Bit part of the Assembler

    // 64bit only pieces of the assembler
    // This should only be used by 64bit instructions that can use rip-relative
    // it cannot be used by instructions that want an immediate value.

    boolean reachable(AddressLiteral adr) {
        long disp;
        // None will force a 64bit literal to the code stream. Likely a placeholder
        // for something that will be patched later and we need to certain it will
        // always be reachable.
        if (adr.reloc() == RelocInfo.Type.none) {
            return false;
        }
        if (adr.reloc() == RelocInfo.Type.internalWordType) {
            // This should be rip relative and easily reachable.
            return true;
        }
        if (adr.reloc() == RelocInfo.Type.virtualCallType || adr.reloc() == RelocInfo.Type.optVirtualCallType || adr.reloc() == RelocInfo.Type.staticCallType ||
                        adr.reloc() == RelocInfo.Type.staticStubType) {
            // This should be rip relative within the code cache and easily
            // reachable until we get huge code caches. (At which point
            // ic code is going to have issues).
            return true;
        }
        if (adr.reloc() != RelocInfo.Type.externalWordType && adr.reloc() != RelocInfo.Type.pollReturnType && // these
                        // are
                        // really
                        // externalWord
                        // but
                        // need
                        // special
                        adr.reloc() != RelocInfo.Type.pollType && // relocs to identify them
                        adr.reloc() != RelocInfo.Type.runtimeCallType) {
            return false;
        }

        // Stress the correction code
        if (C1XOptions.ForceUnreachable) {
            // Must be runtimecall reloc, see if it is in the codecache
            // Flipping stuff in the codecache to be unreachable causes issues
            // with things like inline caches where the additional instructions
            // are not handled.
            if (CodeCache.findBlob(adr.target) == null) {
                return false;
            }
        }
        // For externalWordType/runtimeCallType if it is reachable from where we
        // are now (possibly a temp buffer) and where we might end up
        // anywhere in the codeCache then we are always reachable.
        // This would have to change if we ever save/restore shared code
        // to be more pessimistic.

        disp = adr.target.value - (CodeCache.lowBound() + Util.sizeofInt());
        if (!isSimm32(disp)) {
            return false;
        }
        disp = adr.target.value - (CodeCache.highBound() + Util.sizeofInt());
        if (!isSimm32(disp)) {
            return false;
        }

        disp = adr.target.value - (codePos.value + Util.sizeofInt());

        // Because rip relative is a disp + addressOfNextInstruction and we
        // don't know the value of addressOfNextInstruction we apply a fudge factor
        // to make sure we will be ok no matter the size of the instruction we get placed into.
        // We don't have to fudge the checks above here because they are already worst case.

        // 12 == override/rex byte, opcode byte, rm byte, sib byte, a 4-byte disp , 4-byte literal
        // + 4 because better safe than sorry.
        int fudge = 12 + 4;
        if (disp < 0) {
            disp -= fudge;
        } else {
            disp += fudge;
        }
        return isSimm32(disp);
    }

    void emitData64(long data, RelocInfo.Type rtype, int format) {
        if (rtype != RelocInfo.Type.none) {
            emitLong64(data);
            emitData64(data, Relocation.specSimple(rtype), format);
        }
    }

    void emitData64(long data, RelocationHolder rspec, int format) {
        assert WhichOperand.immOperand.ordinal() == 0 : "default format must be immediate in this file";
        assert WhichOperand.immOperand.ordinal() == format : "must be immediate";
        assert instMark() != null : "must be inside InstructionMark";
        // Do not use Abstractrelocate, which is not intended for
        // embedded words. Instead, relocate to the enclosing instruction.
        codeSection().relocate(instMark(), rspec, format);
        assert checkRelocation(rspec, format);
        emitLong64(data);
    }

    int prefixAndEncode(int regEnc) {
        return prefixAndEncode(regEnc, false);
    }

    int prefixAndEncode(int regEnc, boolean byteinst) {
        if (regEnc >= 8) {
            prefix(Prefix.REXB);
            regEnc -= 8;
        } else if (byteinst && regEnc >= 4) {
            prefix(Prefix.REX);
        }
        return regEnc;
    }

    int prefixqAndEncode(int regEnc) {
        if (regEnc < 8) {
            prefix(Prefix.REXW);
        } else {
            prefix(Prefix.REXWB);
            regEnc -= 8;
        }
        return regEnc;
    }

    int prefixAndEncode(int dstEnc, int srcEnc) {
        return prefixAndEncode(dstEnc, srcEnc, false);
    }

    int prefixAndEncode(int dstEnc, int srcEnc, boolean byteinst) {
        if (dstEnc < 8) {
            if (srcEnc >= 8) {
                prefix(Prefix.REXB);
                srcEnc -= 8;
            } else if (byteinst && srcEnc >= 4) {
                prefix(Prefix.REX);
            }
        } else {
            if (srcEnc < 8) {
                prefix(Prefix.REXR);
            } else {
                prefix(Prefix.REXRB);
                srcEnc -= 8;
            }
            dstEnc -= 8;
        }
        return dstEnc << 3 | srcEnc;
    }

    int prefixqAndEncode(int dstEnc, int srcEnc) {
        if (dstEnc < 8) {
            if (srcEnc < 8) {
                prefix(Prefix.REXW);
            } else {
                prefix(Prefix.REXWB);
                srcEnc -= 8;
            }
        } else {
            if (srcEnc < 8) {
                prefix(Prefix.REXWR);
            } else {
                prefix(Prefix.REXWRB);
                srcEnc -= 8;
            }
            dstEnc -= 8;
        }
        return dstEnc << 3 | srcEnc;
    }

    void prefix(X86Register reg) {
        if (reg.encoding() >= 8) {
            prefix(Prefix.REXB);
        }
    }

    void prefix(Address adr) {
        if (adr.baseNeedsRex()) {
            if (adr.indexNeedsRex()) {
                prefix(Prefix.REXXB);
            } else {
                prefix(Prefix.REXB);
            }
        } else {
            if (adr.indexNeedsRex()) {
                prefix(Prefix.REXX);
            }
        }
    }

    void prefixq(Address adr) {
        if (adr.baseNeedsRex()) {
            if (adr.indexNeedsRex()) {
                prefix(Prefix.REXWXB);
            } else {
                prefix(Prefix.REXWB);
            }
        } else {
            if (adr.indexNeedsRex()) {
                prefix(Prefix.REXWX);
            } else {
                prefix(Prefix.REXW);
            }
        }
    }

    void prefix(Address adr, X86Register reg) {
        if (reg.encoding() < 8) {
            if (adr.baseNeedsRex()) {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXXB);
                } else {
                    prefix(Prefix.REXB);
                }
            } else {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXX);
                } else if (reg.encoding() >= 4) {
                    prefix(Prefix.REX);
                }
            }
        } else {
            if (adr.baseNeedsRex()) {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXRXB);
                } else {
                    prefix(Prefix.REXRB);
                }
            } else {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXRX);
                } else {
                    prefix(Prefix.REXR);
                }
            }
        }
    }

    void prefixq(Address adr, X86Register src) {
        if (src.encoding() < 8) {
            if (adr.baseNeedsRex()) {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXWXB);
                } else {
                    prefix(Prefix.REXWB);
                }
            } else {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXWX);
                } else {
                    prefix(Prefix.REXW);
                }
            }
        } else {
            if (adr.baseNeedsRex()) {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXWRXB);
                } else {
                    prefix(Prefix.REXWRB);
                }
            } else {
                if (adr.indexNeedsRex()) {
                    prefix(Prefix.REXWRX);
                } else {
                    prefix(Prefix.REXWR);
                }
            }
        }
    }

    void adcq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xD0, dst, imm32);
    }

    void adcq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x13);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void adcq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x13, 0xC0, dst, src);
    }

    void addq(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefixq(dst);
            emitArithOperand(0x81, X86Register.rax, dst, imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void addq(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefixq(dst, src);
            emitByte(0x01);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void addq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xC0, dst, imm32);
    }

    void addq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x03);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void addq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x03, 0xC0, dst, src);
    }

    void andq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xE0, dst, imm32);
    }

    void andq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x23);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void andq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x23, 0xC0, dst, src);
    }

    void bsfq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBC);
        emitByte(0xC0 | encode);
    }

    void bsrq(X86Register dst, X86Register src) {
        assert !compilation.target.supportsLzcnt() : "encoding is treated as LZCNT";
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    void bswapq(X86Register reg) {
        int encode = prefixqAndEncode(reg.encoding());
        emitByte(0x0F);
        emitByte(0xC8 | encode);
    }

    void cdqq() {
        prefix(Prefix.REXW);
        emitByte(0x99);
    }

    void clflush(Address adr) {
        prefix(adr);
        emitByte(0x0F);
        emitByte(0xAE);
        emitOperand(X86Register.rdi, adr);
    }

    void cmovq(Condition cc, X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x40 | cc.value);
        emitByte(0xC0 | encode);
    }

    void cmovq(Condition cc, X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0x40 | cc.value);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpq(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefixq(dst);
            emitByte(0x81);
            emitOperand(X86Register.rdi, dst, 4);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xF8, dst, imm32);
    }

    void cmpq(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefixq(dst, src);
            emitByte(0x3B);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x3B, 0xC0, dst, src);
    }

    void cmpq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x3B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpxchgq(X86Register reg, Address adr) {
        this.setInstMark();
        try {
            prefixq(adr, reg);
            emitByte(0x0F);
            emitByte(0xB1);
            emitOperand(reg, adr);
        } finally {
            this.clearInstMark();
        }
    }

    void cvtsi2sdq(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    void cvtsi2ssq(X86Register dst, X86Register src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2A);
        emitByte(0xC0 | encode);
    }

    void cvttsd2siq(X86Register dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        emitByte(0xF2);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    void cvttss2siq(X86Register dst, X86Register src) {
        assert src.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE();
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0x2C);
        emitByte(0xC0 | encode);
    }

    void decq(X86Register dst) {
        // Don't use it directly. Use Macrodecrementq() instead.
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode)
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xFF);
        emitByte(0xC8 | encode);
    }

    void decq(Address dst) {
        // Don't use it directly. Use Macrodecrementq() instead.
        this.setInstMark();
        try {
            prefixq(dst);
            emitByte(0xFF);
            emitOperand(X86Register.rcx, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void fxrstor(Address src) {
        prefixq(src);
        emitByte(0x0F);
        emitByte(0xAE);
        emitOperand(X86Register.fromEncoding(1), src);
    }

    void fxsave(Address dst) {
        prefixq(dst);
        emitByte(0x0F);
        emitByte(0xAE);
        emitOperand(X86Register.fromEncoding(0), dst);
    }

    void idivq(X86Register src) {
        int encode = prefixqAndEncode(src.encoding());
        emitByte(0xF7);
        emitByte(0xF8 | encode);
    }

    void imulq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xAF);
        emitByte(0xC0 | encode);
    }

    void imulq(X86Register dst, X86Register src, int value) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        if (is8bit(value)) {
            emitByte(0x6B);
            emitByte(0xC0 | encode);
            emitByte(value);
        } else {
            emitByte(0x69);
            emitByte(0xC0 | encode);
            emitLong(value);
        }
    }

    void incq(X86Register dst) {
        // Don't use it directly. Use Macroincrementq() instead.
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode)
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xFF);
        emitByte(0xC0 | encode);
    }

    void incq(Address dst) {
        // Don't use it directly. Use Macroincrementq() instead.
        this.setInstMark();
        try {
            prefixq(dst);
            emitByte(0xFF);
            emitOperand(X86Register.rax, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void leaq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x8D);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void mov64(X86Register dst, long imm64) {
        this.setInstMark();
        try {
            int encode = prefixqAndEncode(dst.encoding());
            emitByte(0xB8 | encode);
            emitLong64(imm64);
        } finally {
            this.clearInstMark();
        }
    }

    void movLiteral64(X86Register dst, long imm64, RelocationHolder rspec) {
        this.setInstMark();
        try {
            int encode = prefixqAndEncode(dst.encoding());
            emitByte(0xB8 | encode);
            emitData64(imm64, rspec);
        } finally {
            this.clearInstMark();
        }
    }

    private void emitData64(long imm64, RelocationHolder rspec) {
        emitData64(imm64, rspec, 0);

    }

    void movNarrowOop(X86Register dst, int imm32, RelocationHolder rspec) {
        this.setInstMark();
        try {
            int encode = prefixAndEncode(dst.encoding());
            emitByte(0xB8 | encode);
            emitData(imm32, rspec, WhichOperand.narrowOopOperand);
        } finally {
            this.clearInstMark();
        }
    }

    void movNarrowOop(Address dst, int imm32, RelocationHolder rspec) {
        this.setInstMark();
        try {
            prefix(dst);
            emitByte(0xC7);
            emitOperand(X86Register.rax, dst, 4);
            emitData(imm32, rspec, WhichOperand.narrowOopOperand);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpNarrowOop(X86Register src1, int imm32, RelocationHolder rspec) {
        this.setInstMark();
        try {
            int encode = prefixAndEncode(src1.encoding());
            emitByte(0x81);
            emitByte(0xF8 | encode);
            emitData(imm32, rspec, WhichOperand.narrowOopOperand);
        } finally {
            this.clearInstMark();
        }
    }

    void cmpNarrowOop(Address src1, int imm32, RelocationHolder rspec) {
        this.setInstMark();
        try {
            prefix(src1);
            emitByte(0x81);
            emitOperand(X86Register.rax, src1, 4);
            emitData(imm32, rspec, WhichOperand.narrowOopOperand);
        } finally {
            this.clearInstMark();
        }
    }

    void lzcntq(X86Register dst, X86Register src) {
        assert compilation.target.supportsLzcnt() : "encoding is treated as BSR";
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | encode);
    }

    void movdq(X86Register dst, X86Register src) {

        // table D-1 says MMX/SSE2
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2() || compilation.target.supportsMMX();
        emitByte(0x66);

        if (dst.isXMM()) {
            assert dst.isXMM();
            int encode = prefixqAndEncode(dst.encoding(), src.encoding());
            emitByte(0x0F);
            emitByte(0x6E);
            emitByte(0xC0 | encode);
        } else if (src.isXMM()) {

            // swap src/dst to get correct prefix
            int encode = prefixqAndEncode(src.encoding(), dst.encoding());
            emitByte(0x0F);
            emitByte(0x7E);
            emitByte(0xC0 | encode);
        } else {
            Util.shouldNotReachHere();
        }
    }

    void movsbq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0xBE);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movsbq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBE);
        emitByte(0xC0 | encode);
    }

    void movslq(X86Register dst, int imm32) {
        // dbx shows movslq(X86Register.rcx, 3) as movq $0x0000000049000000,(%X86Register.rbx)
        // and movslq(X86Register.r8, 3); as movl $0x0000000048000000,(%X86Register.rbx)
        // as a result we shouldn't use until tested at runtime...
        Util.shouldNotReachHere();
        this.setInstMark();
        try {
            int encode = prefixqAndEncode(dst.encoding());
            emitByte(0xC7 | encode);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }
    }

    public static boolean isSimm32(int imm32) {
        return true;
    }

    void movslq(Address dst, int imm32) {
        assert isSimm32(imm32) : "lost bits";
        this.setInstMark();
        try {
            prefixq(dst);
            emitByte(0xC7);
            emitOperand(X86Register.rax, dst, 4);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void movslq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x63);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movslq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x63);
        emitByte(0xC0 | encode);
    }

    void movswq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0xBF);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movswq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xBF);
        emitByte(0xC0 | encode);
    }

    void movzbq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0xB6);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movzbq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xB6);
        emitByte(0xC0 | encode);
    }

    void movzwq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0xB7);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void movzwq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xB7);
        emitByte(0xC0 | encode);
    }

    void negq(X86Register dst) {
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xF7);
        emitByte(0xD8 | encode);
    }

    void notq(X86Register dst) {
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xF7);
        emitByte(0xD0 | encode);
    }

    void orq(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefixq(dst);
            emitByte(0x81);
            emitOperand(X86Register.rcx, dst, 4);
            emitLong(imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void orq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xC8, dst, imm32);
    }

    void orq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x0B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void orq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x0B, 0xC0, dst, src);
    }

    void popcntq(X86Register dst, Address src) {
        assert compilation.target.supportsPopcnt() : "must support";
        this.setInstMark();
        try {
            emitByte(0xF3);
            prefixq(src, dst);
            emitByte(0x0F);
            emitByte(0xB8);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void popcntq(X86Register dst, X86Register src) {
        assert compilation.target.supportsPopcnt() : "must support";
        emitByte(0xF3);
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x0F);
        emitByte(0xB8);
        emitByte(0xC0 | encode);
    }

    void popq(Address dst) {
        this.setInstMark();
        try {
            prefixq(dst);
            emitByte(0x8F);
            emitOperand(X86Register.rax, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void pushq(Address src) {
        this.setInstMark();
        try {
            prefixq(src);
            emitByte(0xFF);
            emitOperand(X86Register.rsi, src);
        } finally {
            this.clearInstMark();
        }
    }

    void rclq(X86Register dst, int imm8) {
        assert isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding());
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xD0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xD0 | encode);
            emitByte(imm8);
        }
    }

    void sarq(X86Register dst, int imm8) {
        assert isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding());
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xF8 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xF8 | encode);
            emitByte(imm8);
        }
    }

    void sarq(X86Register dst) {
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xD3);
        emitByte(0xF8 | encode);
    }

    void sbbq(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefixq(dst);
            emitArithOperand(0x81, X86Register.rbx, dst, imm32);
        } finally {
            this.clearInstMark();
        }
    }

    void sbbq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xD8, dst, imm32);
    }

    void sbbq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x1B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void sbbq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x1B, 0xC0, dst, src);
    }

    void shlq(X86Register dst, int imm8) {
        assert isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding());
        if (imm8 == 1) {
            emitByte(0xD1);
            emitByte(0xE0 | encode);
        } else {
            emitByte(0xC1);
            emitByte(0xE0 | encode);
            emitByte(imm8);
        }
    }

    void shlq(X86Register dst) {
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xD3);
        emitByte(0xE0 | encode);
    }

    void shrq(X86Register dst, int imm8) {
        assert isShiftCount(imm8 >> 1) : "illegal shift count";
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xC1);
        emitByte(0xE8 | encode);
        emitByte(imm8);
    }

    void shrq(X86Register dst) {
        int encode = prefixqAndEncode(dst.encoding());
        emitByte(0xD3);
        emitByte(0xE8 | encode);
    }

    void sqrtsd(X86Register dst, Address src) {
        assert dst.isXMM();
        assert compilation.target.arch.is64bit() || compilation.target.supportsSSE2();
        this.setInstMark();
        try {
            emitByte(0xF2);
            prefix(src, dst);
            emitByte(0x0F);
            emitByte(0x51);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void subq(Address dst, int imm32) {
        this.setInstMark();
        try {
            prefixq(dst);
            if (is8bit(imm32)) {
                emitByte(0x83);
                emitOperand(X86Register.rbp, dst, 1);
                emitByte(imm32 & 0xFF);
            } else {
                emitByte(0x81);
                emitOperand(X86Register.rbp, dst, 4);
                emitLong(imm32);
            }
        } finally {
            this.clearInstMark();
        }
    }

    void subq(X86Register dst, int imm32) {
        prefixqAndEncode(dst.encoding());
        emitArith(0x81, 0xE8, dst, imm32);
    }

    void subq(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefixq(dst, src);
            emitByte(0x29);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void subq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x2B);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void subq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x2B, 0xC0, dst, src);
    }

    void testq(X86Register dst, int imm32) {
        // not using emitArith because test
        // doesn't support sign-extension of
        // 8bit operands
        int encode = dst.encoding();
        if (encode == 0) {
            prefix(Prefix.REXW);
            emitByte(0xA9);
        } else {
            encode = prefixqAndEncode(encode);
            emitByte(0xF7);
            emitByte(0xC0 | encode);
        }
        emitLong(imm32);
    }

    void testq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x85, 0xC0, dst, src);
    }

    void xaddq(Address dst, X86Register src) {
        this.setInstMark();
        try {
            prefixq(dst, src);
            emitByte(0x0F);
            emitByte(0xC1);
            emitOperand(src, dst);
        } finally {
            this.clearInstMark();
        }
    }

    void xchgq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x87);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    void xchgq(X86Register dst, X86Register src) {
        int encode = prefixqAndEncode(dst.encoding(), src.encoding());
        emitByte(0x87);
        emitByte(0xc0 | encode);
    }

    void xorq(X86Register dst, X86Register src) {
        prefixqAndEncode(dst.encoding(), src.encoding());
        emitArith(0x33, 0xC0, dst, src);
    }

    void xorq(X86Register dst, Address src) {
        this.setInstMark();
        try {
            prefixq(src, dst);
            emitByte(0x33);
            emitOperand(dst, src);
        } finally {
            this.clearInstMark();
        }
    }

    enum MembarMaskBits {

        LoadLoad, StoreLoad, LoadStore, StoreStore;

        public int mask() {
            return 1 << this.ordinal();
        }
    }

    // Serializes memory and blows flags
    void membar(int orderConstraint) {
        if (compilation.runtime.isMP()) {
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
                addl(new Address(X86Register.rsp, 0), 0); // Assert the lock# signal here
            }
        }
    }
}
