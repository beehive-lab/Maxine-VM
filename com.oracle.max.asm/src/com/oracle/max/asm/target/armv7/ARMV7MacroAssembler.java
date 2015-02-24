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
    }

    public void popptr(CiAddress src) {
    }

    public final void casInt(CiRegister newValue, CiRegister cmpValue, CiAddress address) {
        assert (ARMV7.r8 != cmpValue);
        assert (ARMV7.r8 != newValue);
        assert (ARMV7.r0 == cmpValue);
        Label atomicFail = new Label();
        bind(atomicFail);
        membar(1);
        setUpScratch(address);
        ldrex(ConditionFlag.Always, ARMV7.r8, scratchRegister);
        cmp(ConditionFlag.Always, cmpValue, ARMV7.r8, 0, 0);
        strex(ConditionFlag.Equal, ARMV7.r8, newValue, scratchRegister);
        mov32BitConstant(ARMV7.r12, 1);
        cmp(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0, 0);
        jcc(ConditionFlag.Equal, atomicFail);
        mov32BitConstant(ARMV7.r12, 2);
        cmp(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0, 0);
        mov(ConditionFlag.Equal, false, cmpValue, newValue); // return newValue as we were successful
    }

    public final void casLong(CiRegister newValue, CiRegister cmpValue, CiAddress address) {
        assert (newValue != ARMV7.r8);
        assert (cmpValue != ARMV7.r8);
        Label atomicFail = new Label();
        bind(atomicFail);
        membar(1);
        setUpScratch(address);
        ldrexd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12);
        lcmpl(ConditionFlag.Equal, cmpValue, ARMV7.r8);
        strexd(ConditionFlag.Equal, ARMV7.r8, newValue, ARMV7.r12);
        mov(ConditionFlag.NotEqual, false, cmpValue, ARMV7.r8);
        mov(ConditionFlag.NotEqual, false, ARMV7.cpuRegisters[cmpValue.number + 1], ARMV7.cpuRegisters[ARMV7.r8.number + 1]);
        mov32BitConstant(ARMV7.r12, 1);
        cmp(ConditionFlag.Equal, ARMV7.r8, ARMV7.r12, 0, 0); // equal to 1 then we failed MP so loop
        jcc(ConditionFlag.Equal, atomicFail);
    }

    public void xorptr(CiRegister dst, CiRegister src) {
        xorq(dst, src);
    }

    public void xorptr(CiRegister dst, CiAddress src) {
        xorq(dst, src);
    }

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
        Label l = new Label();
        if (unorderedIsLess) {
            mov32BitConstant(dst, -1);
            jcc(ConditionFlag.SignedOverflow, l);
            jcc(ConditionFlag.CarryClearUnsignedLower, l);
            mov32BitConstant(dst, 0);
            jcc(ConditionFlag.Equal, l); // NEW
            incrementl(dst, 1);
        } else { // unordered is greater
            mov32BitConstant(dst, 1);
            jcc(ConditionFlag.SignedOverflow, l);
            jcc(ConditionFlag.SignedGreater, l);
            mov32BitConstant(dst, 0);
            jcc(ConditionFlag.Equal, l);
            decrementl(dst, 1);
        }
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
            jcc(ConditionFlag.CarryClearUnsignedLower, l);
            mov32BitConstant(dst, 0);
            jcc(ConditionFlag.Equal, l);
            incrementl(dst, 1);
        } else { // unordered is greater
            mov32BitConstant(dst, 1);
            jcc(ConditionFlag.SignedOverflow, l);
            jcc(ConditionFlag.SignedGreater, l);
            mov32BitConstant(dst, 0);
            jcc(ConditionFlag.Equal, l); // NEW
            decrementl(dst, 1);
        }
        bind(l);
    }

    public void cmpq(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public void cmpptr(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public void cmpptr(CiRegister src1, CiAddress src2) {
        setUpScratch(src2);
        assert (ARMV7.r12.number != src1.number);
        ldr(ConditionFlag.Always, ARMV7.r12, ARMV7.r12, 0); // TODO is this necessary or is the address the pointer?
        cmp(ConditionFlag.Always, src1, ARMV7.r12, 0, 0);
    }

    public void cmpptr(CiRegister src1, int src2) {
        assert (ARMV7.r12.number != src1.number);
        mov32BitConstant(ARMV7.r12, src2);
        cmp(ConditionFlag.Always, src1, ARMV7.r12, 0, 0);
    }

    public void cmpptr(CiAddress src1, int src2) {
        setUpScratch(src1);
        mov32BitConstant(ARMV7.r8, src2);
        cmp(ConditionFlag.Always, ARMV7.r12, ARMV7.r8, 0, 0);
    }

    public void decrementl(CiRegister reg, int value) {
        mov32BitConstant(ARMV7.r8, value);
        sub(ConditionFlag.Always, false, reg, reg, ARMV7.r8, 0, 0);
    }

    public void decrementl(CiAddress dst, int value) {
        if (value == 0) {
            return;
        }
        setUpScratch(dst);
        ldr(ConditionFlag.Always, ARMV7.r12, ARMV7.r12, 0);
        mov32BitConstant(ARMV7.r8, value);
        sub(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r8, 0, 0);
        mov(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r12);
        setUpScratch(dst); //Recalculate address to save scratch
        strImmediate(ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, 0);
    }

    public void incrementl(CiRegister reg, int value) {
        if (value == 0) {
            return;
        }
        addq(reg, value);
    }

    public void incrementl(CiAddress dst, int value) {
        if (value == 0) {
            return;
        }
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r8, value);
        ldr(ConditionFlag.Always, r12, r12, 0);
        addRegisters(ConditionFlag.Always, false, r12, r12, ARMV7.r8, 0, 0);
        mov(ConditionFlag.Always, false, ARMV7.r8, r12);
        setUpScratch(dst);
        str(ConditionFlag.Always, ARMV7.r8, r12, 0);
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
        assert (src.number > 15);
        vstr(ConditionFlag.Always, src, r12, 0, CiKind.Double, CiKind.Int);
    }

    public void movlong(CiRegister dst, long src, CiKind dstKind) {
        if (dstKind.isGeneral()) {
            mov64BitConstant(dst, ARMV7.cpuRegisters[dst.encoding + 1], src);
        } else {
            assert dstKind.isDouble() : "Dst reg must be double";
            mov64BitConstant(ARMV7.r8, ARMV7.cpuRegisters[ARMV7.r8.encoding + 1], src);
            vmov(ConditionFlag.Always, dst, ARMV7.r8, dstKind, CiKind.Long);
        }
    }

    public void movlong(CiAddress dst, long src) {
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r8, (int) (0xffffffffL & src));
        mov32BitConstant(ARMV7.cpuRegisters[ARMV7.r8.encoding + 1], (int) ((src >> 32) & 0xffffffffL));
        str(ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, ARMV7.r0, 0, 0);
        strImmediate(ConditionFlag.Always, 0, 1, 0, ARMV7.cpuRegisters[ARMV7.r8.encoding + 1], ARMV7.r12, 4);
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
        assert right == ARMV7.r1;
        assert left != ARMV7.r8;
        mov32BitConstant(ARMV7.r12, 0x3f); // We really need another register!!!!!!
        and(ConditionFlag.Always, false, right, right, ARMV7.r12, 0, 0);
        sub(ConditionFlag.Always, false, ARMV7.r12, right, 32, 0);
        rsb(ConditionFlag.Always, false, ARMV7.r8, right, 32, 0);
        lsl(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], right, ARMV7.cpuRegisters[left.number + 1]);
        orsr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[dest.number + 1], left, ARMV7.r12, 0);
        orsr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], ARMV7.cpuRegisters[dest.number + 1], left, ARMV7.r8, 1);
        lsl(ConditionFlag.Always, false, dest, right, left);
    }

    public void ishr(CiRegister dest, CiRegister left, int amount) {
        asr(ConditionFlag.Always, true, dest, left, amount);
    }

    public void ishr(CiRegister dest, CiRegister left, CiRegister right) {
        asrr(ConditionFlag.Always, true, dest, right, left);
    }

    public void lshr(CiRegister dest, CiRegister left, CiRegister right) {
        assert (left == dest);
        assert right == ARMV7.r1;
        assert left != ARMV7.r1;
        assert dest.encoding % 2 == 0;
        mov32BitConstant(ARMV7.r12, 0x3f);
        and(ConditionFlag.Always, false, right, right, ARMV7.r12, 0, 0);
        rsb(ConditionFlag.Always, false, ARMV7.r12, right, 32, 0);
        sub(ConditionFlag.Always, true, ARMV7.r8, right, 32, 0);
        lsr(ConditionFlag.Always, false, dest, right, left);
        orsr(ConditionFlag.Always, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r12, 0);
        orsr(ConditionFlag.Positive, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r8, 2);
        asrr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], right, ARMV7.cpuRegisters[left.number + 1]);
    }

    public void iushr(CiRegister dest, CiRegister left, int amount) {
        lsr(ConditionFlag.Always, true, dest, left, amount); // logical shift right is sufficient
    }

    public void iushr(CiRegister dest, CiRegister left, CiRegister right) {
        lsr(ConditionFlag.Always, true, dest, right, left);
    }

    public void lushr(CiRegister dest, CiRegister left, CiRegister right) {
        assert left == dest;
        assert dest.encoding % 2 == 0;
        assert right == ARMV7.r1;
        assert left != ARMV7.r1;
        mov32BitConstant(ARMV7.r12, 0x3f);
        and(ConditionFlag.Always, false, right, right, ARMV7.r12, 0, 0);
        rsb(ConditionFlag.Always, false, ARMV7.r12, right, 32, 0);
        sub(ConditionFlag.Always, false, ARMV7.r8, right, 32, 0);
        lsr(ConditionFlag.Always, false, dest, right, left);
        orsr(ConditionFlag.Always, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r12, 0);
        orsr(ConditionFlag.Positive, false, dest, dest, ARMV7.cpuRegisters[left.number + 1], ARMV7.r8, 1);
        lsr(ConditionFlag.Always, false, ARMV7.cpuRegisters[dest.number + 1], right, ARMV7.cpuRegisters[left.number + 1]);
    }
}
