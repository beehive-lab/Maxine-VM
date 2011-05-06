/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.c1x.target.amd64;

import com.sun.c1x.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * This class implements commonly used X86 code patterns.
 */
public class AMD64MacroAssembler extends AMD64Assembler {

    protected final CiRegister rscratch1;

    public AMD64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
        this.rscratch1 = registerConfig.getScratchRegister();
    }

    void mov64(CiAddress dst, long src) {
        movq(rscratch1, src);
        movq(dst, rscratch1);
    }

    void pushptr(CiAddress src) {
        pushq(src);
    }

    void popptr(CiAddress src) {
        popq(src);
    }

    void xorptr(CiRegister dst, CiRegister src) {
        xorq(dst, src);
    }

    void xorptr(CiRegister dst, CiAddress src) {
        xorq(dst, src);
    }

    // 64 bit versions


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
        if (value == 1 && AsmOptions.UseIncDec) {
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
        if (value == 1 && AsmOptions.UseIncDec) {
            incq(reg);
        } else {
            addq(reg, value);
        }
    }

    // These are mostly for initializing null
    void movptr(CiAddress dst, int src) {
        movslq(dst, src);
    }

    public final void cmp32(CiRegister src1, int imm) {
        cmpl(src1, imm);
    }

    public final void cmp32(CiRegister src1, CiAddress src2) {
        cmpl(src1, src2);
    }

    void cmpsd2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
        assert opr1.isFpu() && opr2.isFpu();
        ucomisd(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(AMD64Assembler.ConditionFlag.parity, l);
            jcc(AMD64Assembler.ConditionFlag.below, l);
            movl(dst, 0);
            jcc(AMD64Assembler.ConditionFlag.equal, l);
            incrementl(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(AMD64Assembler.ConditionFlag.parity, l);
            jcc(AMD64Assembler.ConditionFlag.above, l);
            movl(dst, 0);
            jcc(AMD64Assembler.ConditionFlag.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void cmpss2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
        assert opr1.isFpu();
        assert opr2.isFpu();
        ucomiss(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(AMD64Assembler.ConditionFlag.parity, l);
            jcc(AMD64Assembler.ConditionFlag.below, l);
            movl(dst, 0);
            jcc(AMD64Assembler.ConditionFlag.equal, l);
            incrementl(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(AMD64Assembler.ConditionFlag.parity, l);
            jcc(AMD64Assembler.ConditionFlag.above, l);
            movl(dst, 0);
            jcc(AMD64Assembler.ConditionFlag.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
    }

    void cmpptr(CiRegister src1, CiRegister src2) {
        cmpq(src1, src2);
    }

    void cmpptr(CiRegister src1, CiAddress src2) {
        cmpq(src1, src2);
    }

    void cmpptr(CiRegister src1, int src2) {
        cmpq(src1, src2);
    }

    void cmpptr(CiAddress src1, int src2) {
        cmpq(src1, src2);
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
        if (value == 1 && AsmOptions.UseIncDec) {
            decl(reg);
        } else {
            subl(reg, value);
        }
    }

    void decrementl(CiAddress dst, int value) {
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
        if (value == 1 && AsmOptions.UseIncDec) {
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
        if (value == 1 && AsmOptions.UseIncDec) {
            incl(reg);
        } else {
            addl(reg, value);
        }
    }

    void incrementl(CiAddress dst, int value) {
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
        if (value == 1 && AsmOptions.UseIncDec) {
            incl(dst);
        } else {
            addl(dst, value);
        }
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

    // Support optimal SSE move instructions.
    void movflt(CiRegister dst, CiRegister src) {
        assert dst.isFpu() && src.isFpu();
        if (AsmOptions.UseXmmRegToRegMoveAll) {
            movaps(dst, src);
        } else {
            movss(dst, src);
        }
    }

    void movflt(CiRegister dst, CiAddress src) {
        assert dst.isFpu();
        movss(dst, src);
    }

    void movflt(CiAddress dst, CiRegister src) {
        assert src.isFpu();
        movss(dst, src);
    }

    void movdbl(CiRegister dst, CiRegister src) {
        assert dst.isFpu() && src.isFpu();
        if (AsmOptions.UseXmmRegToRegMoveAll) {
            movapd(dst, src);
        } else {
            movsd(dst, src);
        }
    }

    void movdbl(CiRegister dst, CiAddress src) {
        assert dst.isFpu();
        if (AsmOptions.UseXmmLoadAndClearUpper) {
            movsd(dst, src);
        } else {
            movlpd(dst, src);
        }
    }

    void xchgptr(CiRegister src1, CiRegister src2) {
        xchgq(src1, src2);
    }

    /**
     * Emit code to save a given set of callee save registers to the
     * {@linkplain CiCalleeSaveArea CSA} within the frame.
     * @param csa the description of the CSA
     * @param frameToCSA offset from the frame pointer to the CSA
     */
    public void save(CiCalleeSaveArea csa, int frameToCSA) {
        CiRegisterValue frame = frameRegister.asValue();
        for (CiRegister r : csa.registers) {
            int offset = csa.offsetOf(r);
            movq(new CiAddress(CiKind.Word, frame, frameToCSA + offset), r);
        }
    }

    public void restore(CiCalleeSaveArea csa, int frameToCSA) {
        CiRegisterValue frame = frameRegister.asValue();
        for (CiRegister r : csa.registers) {
            int offset = csa.offsetOf(r);
            movq(r, new CiAddress(CiKind.Word, frame, frameToCSA + offset));
        }
    }
}
