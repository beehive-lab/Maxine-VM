/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES
 * OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for
 * more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need
 * additional information or have any questions.
 */
package com.oracle.max.asm.target.armv7;

import com.oracle.max.asm.AsmOptions;
import com.oracle.max.asm.Label;
import com.sun.cri.ci.*;
import com.sun.cri.ri.RiRegisterConfig;

import static com.oracle.max.asm.target.armv7.ARMV7.r12;

public class ARMV7MacroAssembler extends ARMV7Assembler {

    public ARMV7MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public void pushptr(CiAddress src) {
        //pushq(src);
    }

    public void popptr(CiAddress src) {
        //popq(src);
    }

    public final void casInt(CiRegister newValue, CiRegister cmpValue, CiAddress address) {
        assert (ARMV7.r9 != cmpValue);
        assert (ARMV7.r9 != newValue);
        assert (ARMV7.r8 != cmpValue);
        assert (ARMV7.r8 != newValue);
        assert (ARMV7.r0 == cmpValue);
        Label atomicFail = new Label();
        bind(atomicFail);
        membar(1);
        setUpScratch(address);
        mov32BitConstant(ARMV7.r9, 2);// put we.re not equal in
        ldrex(ConditionFlag.Always, ARMV7.r8, scratchRegister);
        cmp(ConditionFlag.Always, cmpValue, ARMV7.r8, 0, 0);
        // Keep r0 in sync with code at ARMV7LirGenerator.visitCompareAndSwap
        strex(ConditionFlag.Equal, ARMV7.r9, newValue, scratchRegister);
        mov32BitConstant(ARMV7.r12, 1);
        cmp(ConditionFlag.Always, ARMV7.r9, ARMV7.r12, 0, 0);
        jcc(ConditionFlag.Equal, atomicFail);
        mov32BitConstant(ARMV7.r12, 2);
        cmp(ConditionFlag.Always, ARMV7.r9, ARMV7.r12, 0, 0);
        mov(ConditionFlag.Equal, false, cmpValue, ARMV7.r8); // return newValue as we were successful
    }

    public final void casLong(CiRegister newValue, CiRegister cmpValue, CiAddress address) {
        assert (newValue != ARMV7.r8);
        assert (cmpValue != ARMV7.r8);
        assert (newValue != ARMV7.r9);
        assert (cmpValue != ARMV7.r9);

        Label atomicFail = new Label();
        bind(atomicFail);
        membar(1);

        setUpScratch(address);
        ldrexd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12);
        lcmpl(ConditionFlag.Equal, cmpValue, ARMV7.r8);
        // Keep r0 in sync with code at ARMV7LirGenerator.visitCompareAndSwap
        strexd(ConditionFlag.Equal, ARMV7.r8, newValue, ARMV7.r12);
        mov(ConditionFlag.NotEqual, false, cmpValue, ARMV7.r8);
        mov(ConditionFlag.NotEqual, false, ARMV7.cpuRegisters[cmpValue.number + 1], ARMV7.r9);
        mov32BitConstant(ARMV7.r12, 1);
        cmp(ConditionFlag.Equal, ARMV7.r8, ARMV7.r12, 0, 0); // equal to 1 then we failed MP so loop
        jcc(ConditionFlag.Equal, atomicFail);
        // FLAGs might be wrong for subsequent compares?

    }

    public void xorptr(CiRegister dst, CiRegister src) {
        xorq(dst, src);
    }

    public void xorptr(CiRegister dst, CiAddress src) {
        // APN I have assumed we do not need to load the CiAddress?
        // is this incorrect?
        xorq(dst, src);
    }

    // 64 bit versions

    public void decrementq(CiRegister reg, int value) {
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
        if (value == 1 && AsmOptions.UseIncDec) {
            decq(reg);
        } else {
            subq(reg, value);
        }
    }

    public void incrementq(CiRegister reg, int value) {
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
        if (value == 1 && AsmOptions.UseIncDec) {
            incq(reg);
        } else {
            addq(reg, value);
        }
    }

    // These are mostly for initializing null
    public void movptr(CiAddress dst, int src) {
        movslq(dst, src);
    }

    public final void cmp32(CiRegister src1, int imm) {
        cmpl(src1, imm);
    }

    public final void cmp32(CiRegister src1, CiAddress src2) {
        cmpl(src1, src2);
    }

    public void cmpsd2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess, CiKind opr1Kind, CiKind opr2Kind) {
        assert opr1.isFpu() && opr2.isFpu();
        ucomisd(opr1, opr2, opr1Kind, opr2Kind);
        // assert !opr1.isFpu(); //force crash as not implemented yet.
        // get condition codes. don't set
        // FPSCR register flags
        // [31] N Set to 1 if a comparison operation produces a less than result.
        // 30] Z Set to 1 if a comparison operation produces an equal result.
        // [29] C Set to 1 if a comparison operation produces an equal, greater than, or unordered result.
        // [28] V Set to 1 if a comparison operation produces an unordered result. SAME as parity flag?
        Label l = new Label();
        if (unorderedIsLess) {
            mov32BitConstant(dst, -1);
            jcc(ConditionFlag.SignedOverflow, l);
            mov32BitConstant(dst, 0);
            incrementl(dst, 1);
        } else { // unordered is greater
            mov32BitConstant(dst, 1);
            jcc(ConditionFlag.SignedOverflow, l);
            mov32BitConstant(dst, 0);
            decrementl(dst, 1);
        }
        // don't need it in ARM
        bind(l);
    }

    public void cmpss2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess, CiKind opr1Kind, CiKind opr2Kind) {
        assert opr1.isFpu();
        assert opr2.isFpu();
        ucomisd(opr1, opr2, opr1Kind, opr2Kind);
        Label l = new Label();
        if (unorderedIsLess) {
            mov32BitConstant(dst, -1);
            jcc(ConditionFlag.SignedOverflow, l);
            mov32BitConstant(dst, 0);
            incrementl(dst, 1);
        } else { // unordered is greater
            mov32BitConstant(dst, 1);
            jcc(ConditionFlag.SignedOverflow, l);
            mov32BitConstant(dst, 0);
            decrementl(dst, 1);
        }
        bind(l);
    }

    public void cmpq(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public void cmpptr(CiRegister src1, CiRegister src2) {
        // cmpq(src1, src2);
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public void cmpptr(CiRegister src1, CiAddress src2) {
        setUpScratch(src2);
        assert (ARMV7.r12.number != src1.number);
        ldr(ConditionFlag.Always, ARMV7.r12, ARMV7.r12, 0); // TODO is this necessary or is the address the pointer?
        cmp(ConditionFlag.Always, src1, ARMV7.r12, 0, 0);
        // cmpq(src1, src2);
    }

    public void cmpptr(CiRegister src1, int src2) {
        // cmpq(src1, src2);
        assert (ARMV7.r12.number != src1.number);
        mov32BitConstant(ARMV7.r12, src2);
        cmp(ConditionFlag.Always, src1, ARMV7.r12, 0, 0);
    }

    public void cmpptr(CiAddress src1, int src2) {
        setUpScratch(src1);
        mov32BitConstant(ARMV7.r8, src2);
        cmp(ConditionFlag.Always, ARMV7.r12, ARMV7.r8, 0, 0);
        // cmpq(src1, src2);
    }

    public void decrementl(CiRegister reg, int value) {
        CiRegister tmp;
        if (reg == ARMV7.r12) {
            tmp = ARMV7.r8;
        } else {
            tmp = ARMV7.r9;
        }
        mov32BitConstant(tmp, value);
        // sub(ConditionFlag.Always,false,reg,tmp,0,0);
        sub(ConditionFlag.Always, false, reg, reg, tmp, 0, 0);
        /*
         * if (value == Integer.MIN_VALUE) { subl(reg, value); return; } if (value < 0) { incrementl(reg, -value);
         * return; } if (value == 0) { return; } if (value == 1 && AsmOptions.UseIncDec) { decl(reg); } else { subl(reg,
         * value); }
         */
    }

    public void decrementl(CiAddress dst, int value) {
        if (value == 0) {
            return;
        }
        setUpScratch(dst);
        ldr(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
        mov32BitConstant(ARMV7.r9, value);
        // sub(ConditionFlag.Always,false,ARMV7.r8,ARMV7.r9,0,0);
        sub(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, ARMV7.r9, 0, 0);
        strImmediate(ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, 0);

        /*
         * if (value == Integer.MIN_VALUE) { subl(dst, value); return; } if (value < 0) { incrementl(dst, -value);
         * return; } if (value == 0) { return; } if (value == 1 && AsmOptions.UseIncDec) { decl(dst); } else { subl(dst,
         * value); }
         */
    }

    public void incrementl(CiRegister reg, int value) {
        if (value == 0) {
            return;
        }

        addq(reg, value);
        /*
         * if (value == Integer.MIN_VALUE) { addl(reg, value); return; } if (value < 0) { decrementl(reg, -value);
         * return; } if (value == 0) { return; } if (value == 1 && AsmOptions.UseIncDec) { incl(reg); } else { addl(reg,
         * value); }
         */
    }

    public void incrementl(CiAddress dst, int value) {
        if (value == 0) {
            return;
        }
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r8, value);
        ldr(ConditionFlag.Always, ARMV7.r9, r12, 0);
        addRegisters(ConditionFlag.Always, false, ARMV7.r9, ARMV7.r9, ARMV7.r8, 0, 0);
        str(ConditionFlag.Always, ARMV7.r9, r12, 0);

        /*
         * if (value == Integer.MIN_VALUE) { addl(dst, value); return; } if (value < 0) { decrementl(dst, -value);
         * return; } if (value == 0) { return; } if (value == 1 && AsmOptions.UseIncDec) { incl(dst); } else { addl(dst,
         * value); }
         */
    }

    public void signExtendByte(CiRegister dest, CiRegister reg) {
        ishl(dest, reg, 24);
        iushr(dest, dest, 24);
    }

    public void signExtendByte(CiRegister reg) {
        ishl(reg, reg, 24);
        iushr(reg, reg, 24);
    }

    public void signExtendShort(CiRegister dest, CiRegister reg) {
        ishl(dest, reg, 16);
        iushr(dest, dest, 16);
    }

    public void movflt(CiRegister dst, CiRegister src, CiKind dstKind, CiKind srcKind) {
        vmov(ConditionFlag.Always, dst, src, dstKind, srcKind);
    }

    public void movflt(CiRegister dst, CiAddress src) {
        assert dst.isFpu();
        setUpScratch(src);
        vldr(ConditionFlag.Always, dst, r12, 0, CiKind.Float, CiKind.Int);
    }

    public void movflt(CiAddress dst, CiRegister src) {
        assert src.isFpu();
        setUpScratch(dst);
        vstr(ConditionFlag.Always, src, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
    }

    public void movdbl(CiRegister dst, CiRegister src, CiKind dstKind, CiKind srcKind) {
        assert dst.isFpu() && src.isFpu();
        vmov(ConditionFlag.Always, dst, src, dstKind, srcKind);
    }

    public void movdbl(CiRegister dst, CiAddress src) {
        assert dst.isFpu();
        setUpScratch(src);
        vldr(ConditionFlag.Always, dst, r12, 0, CiKind.Double, CiKind.Int);
    }

    public void movdbl(CiAddress dst, CiRegister src) {
        setUpScratch(dst);
        if (src.number > 15) {
            vstr(ConditionFlag.Always, src, r12, 0, CiKind.Double, CiKind.Int);
        } else {
            // TODO: Fix that
        }
    }

    public void movlong(CiRegister dst, long src, CiKind dstKind) {
        if (dstKind.isGeneral()) {
            mov64BitConstant(dst, ARMV7.cpuRegisters[dst.encoding + 1], src);
        } else {
            assert dstKind.isDouble() : "Dst reg must be double";
            mov64BitConstant(ARMV7.r8, ARMV7.r9, src);
            vmov(ConditionFlag.Always, dst, ARMV7.r8, dstKind, CiKind.Long);
        }
    }

    public void movlong(CiAddress dst, long src) {
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r8, (int) (0xffffffffL & src));
        mov32BitConstant(ARMV7.r9, (int) ((src >> 32) & 0xffffffffL));
        str(ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, ARMV7.r0, 0, 0);
        strImmediate(ConditionFlag.Always, 0, 1, 0, ARMV7.r9, ARMV7.r12, 4);
    }

    public void xchgptr(CiRegister src1, CiRegister src2) {
        xchgq(src1, src2);
    }

    public void flog(CiRegister dest, CiRegister value, boolean base10) {
        assert false : "flog not implemented";
    }

    public void fsin(CiRegister dest, CiRegister value) {
        assert false : "fsin not implemented";
    }

    public void fcos(CiRegister dest, CiRegister value) {
        assert false : "fcos not implemented";
    }

    public void ftan(CiRegister dest, CiRegister value) {
        assert false : "ftan not implemented";
    }

    private void ftrig(CiRegister dest, CiRegister value, char op) {
        assert false : "ftrig not implemented";
    }

    /**
     * Emit code to save a given set of callee save registers in the {@linkplain CiCalleeSaveLayout CSA} within the
     * frame.
     *
     * @param csl the description of the CSA
     * @param frameToCSA offset from the frame pointer to the CSA
     */
    public void save(CiCalleeSaveLayout csl, int frameToCSA) {
        CiRegisterValue frame = frameRegister.asValue();
        for (CiRegister r : csl.registers) {
            int offset = csl.offsetOf(r);
            setUpScratch(new CiAddress(target.wordKind, frame, frameToCSA + offset));
            if (r.number < 16) {
                strImmediate(ConditionFlag.Always, 1, 0, 0, r, r12, 0);
            } else {
                vstr(ConditionFlag.Always, r, r12, 0, CiKind.Float, CiKind.Int);
            }
        }
    }

    public void restore(CiCalleeSaveLayout csl, int frameToCSA) {
        CiRegisterValue frame = frameRegister.asValue();
        for (CiRegister r : csl.registers) {
            int offset = csl.offsetOf(r);
            setUpScratch(new CiAddress(target.wordKind, frame, frameToCSA + offset));
            if (r.number < 16) {
                ldrImmediate(ConditionFlag.Always, 1, 0, 0, r, r12, 0);
            } else {
                vldr(ConditionFlag.Always, r, r12, 0, CiKind.Float, CiKind.Int);
            }
        }
    }

    public void imul(CiRegister dest, CiRegister left, CiRegister right) {
        mul(ConditionFlag.Always, true, dest, left, right);
    }

    public void isub(CiRegister dest, CiRegister left, CiRegister right) {
        sub(ConditionFlag.Always, true, dest, left, right, 0, 0);
    }

    public void iadd(CiRegister dest, CiRegister left, CiRegister right) {
        addRegisters(ConditionFlag.Always, true, dest, left, right, 0, 0);
    }

    public void iadd(CiRegister dest, CiRegister left, CiAddress right) {
        setUpScratch(right);
        ldrImmediate(ConditionFlag.Always, 1, 0, 0, r12, r12, 0);
        addRegisters(ConditionFlag.Always, true, dest, left, r12, 0, 0);
    }

    public void isub(CiRegister dest, CiRegister left, CiAddress right) {
        setUpScratch(right);
        ldrImmediate(ConditionFlag.Always, 1, 0, 0, r12, r12, 0);
        sub(ConditionFlag.Always, true, dest, left, r12, 0, 0);
    }

    public void ineg(CiRegister dest, CiRegister left) {
        neg(ConditionFlag.Always, true, dest, left, 0);
    }

    public void lneg(CiRegister dest, CiRegister left) {
        rsb(ConditionFlag.Always, true, dest, left, 0, 0);
        rsc(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[left.number + 1], 0);
    }

    public void ior(CiRegister dest, CiRegister left, CiRegister right) {
        orr(ConditionFlag.Always, true, dest, left, right, 0, 0);
    }

    public void lor(CiRegister dest, CiRegister left, CiRegister right) {
        orr(ConditionFlag.Always, true, dest, left, right, 0, 0);
        orr(ConditionFlag.Always, true, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[left.number + 1], ARMV7.cpuRegisters[right.number + 1], 0, 0);
    }

    public void ixor(CiRegister dest, CiRegister left, CiRegister right) {
        eor(ConditionFlag.Always, true, dest, left, right, 0, 0);
    }

    public void lxor(CiRegister dest, CiRegister left, CiRegister right) {
        eor(ConditionFlag.Always, true, dest, left, right, 0, 0);
        eor(ConditionFlag.Always, true, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[left.number + 1], ARMV7.cpuRegisters[right.number + 1], 0, 0);
    }

    public void iand(CiRegister dest, CiRegister left, CiRegister right) {
        and(ConditionFlag.Always, true, dest, left, right, 0, 0);
    }

    public void land(CiRegister dest, CiRegister left, CiRegister right) {
        and(ConditionFlag.Always, true, dest, left, right, 0, 0);
        and(ConditionFlag.Always, true, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[left.number + 1], ARMV7.cpuRegisters[right.number + 1], 0, 0);
    }

    public void ishl(CiRegister dest, CiRegister left, int amount) {
        lsl(ConditionFlag.Always, true, dest, left, amount);
    }

    public void ishl(CiRegister dest, CiRegister left, CiRegister right) {
        lsl(ConditionFlag.Always, true, dest, right, left);
    }

    public void lshl(CiRegister dest, CiRegister left, CiRegister right) {
        assert dest.encoding % 2 == 0;
        assert left == dest;
        assert right == ARMV7.r8;
        assert left != ARMV7.r8;
        /*
         * FINAL register usages R0,R12 tmp we use r9 and r12 R1 right R2 LEFT R3 LEFT+1 R4 DEST R5 DEST+1
         *
         * 1c: e241c020 sub R12, RIGHT, #32 20: e2610020 rsb R9, RIGHT, #32 24: e1a05113 lsl DEST+1, LEFT+1, RIGHT //
         * LEFT+1 destroyed by this OK 28: e1855c12 orr DEST+1, DEST+1, LEFT, lsl R12 2c: e1855032 orr DEST+1, DEST+1,
         * LEFT, lsr R9 30: e1a04112 lsl DEST, LEFT, RIGHT
         */
        mov32BitConstant(ARMV7.r12, 0x3f); // We really need another register!!!!!!
        and(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, ARMV7.r12, 0, 0);
        sub(ConditionFlag.Always, false, ARMV7.r12, right, 32, 0);
        rsb(ConditionFlag.Always, false, ARMV7.r9, right, 32, 0);
        lsl(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], right, ARMV7.cpuRegisters[left.number + 1]);
        orsr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[dest.number + 1], left, ARMV7.r12, 0);
        orsr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[dest.number + 1], left, ARMV7.r9, 1);
        lsl(ConditionFlag.Always, false, dest, right, left);

    }

    public void ishr(CiRegister dest, CiRegister left, int amount) {
        // lsr(ConditionFlag.Always, true, dest, left, amount);
        asr(ConditionFlag.Always, true, dest, left, amount);
    }

    public void ishr(CiRegister dest, CiRegister left, CiRegister right) {
        // lsr(ConditionFlag.Always, true, dest, right, left);
        asrr(ConditionFlag.Always, true, dest, right, left);
    }

    public void lshr(CiRegister dest, CiRegister left, CiRegister right) {
        assert (left == dest);
        assert right == ARMV7.r8;
        assert left != ARMV7.r8;
        assert dest.encoding % 2 == 0;

        mov32BitConstant(ARMV7.r12, 0x3f);
        and(ConditionFlag.Always, false, right, right, ARMV7.r12, 0, 0);
        rsb(ConditionFlag.Always, false, ARMV7.r12, right, 32, 0);
        sub(ConditionFlag.Always, true, ARMV7.r9, right, 32, 0);

        lsr(ConditionFlag.Always, false, dest, right, left);
        orsr(ConditionFlag.Always, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r12, 0);

        orsr(ConditionFlag.Positive, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r9, 2);
        asrr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], right, ARMV7.cpuRegisters[left.number + 1]);
    }

    public void iushr(CiRegister dest, CiRegister left, int amount) {
        // lusr(ConditionFlag.Always, true, dest, left, amount);
        lsr(ConditionFlag.Always, true, dest, left, amount); // logical shift right is sufficient
        // the int amount is an ARM

    }

    public void iushr(CiRegister dest, CiRegister left, CiRegister right) {
        // lusr(ConditionFlag.Always, true, dest, right, left);
        lsr(ConditionFlag.Always, true, dest, right, left);

    }

    public void lushr(CiRegister dest, CiRegister left, CiRegister right) {
        Label l = new Label();
        assert left == dest;
        assert dest.encoding % 2 == 0;
        assert right == ARMV7.r8;
        assert left != ARMV7.r8;

        /*
         * 5c: e14b01f4 strd r0, [fp, #-20] ; 0xffffffec 60: e50b2018 str r2, [fp, #-24] 64: e14b21d4 ldrd r2, [fp,
         * #-20] ; 0xffffffec 68: e51b1018 ldr r1, [fp, #-24]
         *
         * R12 is tmp R9 is tmp R2 LEFT R3 LEFT+1 R1 RIGHT 6c: e261c020 rsb R12, RIGHT, #32 70: e2410020 sub R9, RIGHT,
         * #32 74: e1a04132 lsr DEST, LEFT, RIGHT 78: e1844c13 orr DEST, DEST, LEFT+1, lsl R12 7c: e1844033 orr DEST,
         * DEST, LEFT+1, lsr R9 80: e1a05133 lsr DEST+1, LEFT+1, RIGHT 84: e1a02004 mov r2, r4 88: e1a03005 mov r3, r5
         */
        mov32BitConstant(ARMV7.r12, 0x3f);
        and(ConditionFlag.Always, false, right, right, ARMV7.r12, 0, 0);
        rsb(ConditionFlag.Always, false, ARMV7.r12, right, 32, 0);
        sub(ConditionFlag.Always, false, ARMV7.r9, right, 32, 0);

        lsr(ConditionFlag.Always, false, dest, right, left);
        orsr(ConditionFlag.Always, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r12, 0);
        orsr(ConditionFlag.Positive, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r9, 1);
        lsr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], right, ARMV7.cpuRegisters[left.number + 1]);
    }
}
