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
package com.oracle.max.asm.target.armv7;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * This class implements commonly used ARM!!!!! code patterns.
 */
public class ARMV7MacroAssembler extends ARMV7Assembler {

    public ARMV7MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public void pushptr(CiAddress src) {
        //stm(0xE,0,0,1,1,r13,src)
        // APN obvously we need to figure out how to resolve the CiAddress issue which is explained below.
        // in the next method.
        pushq(src);


        /** APN
         * A CiAddress Represents an address in target machine memory, specified via some combination of a base register, an index register,
         * a displacement and a scale. Note that the base and index registers may be {@link CiVariable variable}, that is as yet
         * unassigned to target machine registers.
         *
         * APN ok so this is problematic, Im not sure when the CiAddress might be lowered to an
         * address that lies in memory, or in a target machine register that we can
         * push onto the stack.
         *
         * prefixq(src)
         * emitByte(0xFF)
         * emitOperandHelper(rsi,src);
         *
         * A CiAddress can represent addresses of the form base + index*scale + displacement.
         * Base & index would be registers.
         *
         */
        pushq(src);


    }

    public void popptr(CiAddress src) {
        popq(src);
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

    public void cmpsd2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
        assert opr1.isFpu() && opr2.isFpu();
        ucomisd(opr1, opr2);
        assert(!opr1.isFpu()); //force crash as not implemented yet.
        // get condition codes. don't set
        // FPSCR register  flags
        // [31] N	Set to 1 if a comparison operation produces a less than result.
        // 30]	Z	Set to 1 if a comparison operation produces an equal result.
        //[29]	C	Set to 1 if a comparison operation produces an equal, greater than, or unordered result.
        //[28]	V	Set to 1 if a comparison operation produces an unordered result. SAME as parity flag?
        // insert the statement to do this
        // do appropriate conditional moves in ARM ...


        // dont need it in ARM?
        // Label l = new Label();
        /*
        APN please bear in mind I am a novice x86er
        My reading is that this code jumps to the label if the conditions are true
        dst is given the value -1 iff opr1 <- opr2
        0 -ff opr1 == opr2
        1 iff opr1 > opr2

        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(ARMV7Assembler.ConditionFlag.parity, l);
            jcc(ARMV7Assembler.ConditionFlag.below, l);
            movl(dst, 0);
            jcc(ARMV7Assembler.ConditionFlag.equal, l);
            incrementl(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(ARMV7Assembler.ConditionFlag.parity, l);
            jcc(ARMV7Assembler.ConditionFlag.above, l);
            movl(dst, 0);
            jcc(ARMV7Assembler.ConditionFlag.equal, l);
            decrementl(dst, 1);
        }
        */
        // don't need it in ARM
        // bind(l);
    }

    public void cmpss2int(CiRegister opr1, CiRegister opr2, CiRegister dst, boolean unorderedIsLess) {
        assert opr1.isFpu();
        assert opr2.isFpu();
        assert(!opr1.isFpu());// APN force crash as not yet implemented
        /*ucomiss(opr1, opr2);

        Label l = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(ARMV7Assembler.ConditionFlag.parity, l);
            jcc(ARMV7Assembler.ConditionFlag.below, l);
            movl(dst, 0);
            jcc(ARMV7Assembler.ConditionFlag.equal, l);
            incrementl(dst, 1);
        } else { // unordered is greater
            movl(dst, 1);
            jcc(ARMV7Assembler.ConditionFlag.parity, l);
            jcc(ARMV7Assembler.ConditionFlag.above, l);
            movl(dst, 0);
            jcc(ARMV7Assembler.ConditionFlag.equal, l);
            decrementl(dst, 1);
        }
        bind(l);
        */
    }

    public void cmpptr(CiRegister src1, CiRegister src2) {
        //cmpq(src1, src2);
    }

    public void cmpptr(CiRegister src1, CiAddress src2) {
        //cmpq(src1, src2);
    }

    public void cmpptr(CiRegister src1, int src2) {
        //cmpq(src1, src2);
    }

    public void cmpptr(CiAddress src1, int src2) {
        //cmpq(src1, src2);
    }

    public void decrementl(CiRegister reg, int value) {
        /*if (value == Integer.MIN_VALUE) {
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
        } */
    }

    public void decrementl(CiAddress dst, int value) {
        /*if (value == Integer.MIN_VALUE) {
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
        } */
    }

    public void incrementl(CiRegister reg, int value) {
        movw(ConditionFlag.Always,ARMV7.r12,value&0xffff);
        movt(ConditionFlag.Always,ARMV7.r12,value&0xffff0000);
        add(ConditionFlag.Always,false,reg,ARMV7.r12,0,0);
        /*if (value == Integer.MIN_VALUE) {
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
        } */
    }

    public void incrementl(CiAddress dst, int value) {
        /*if (value == Integer.MIN_VALUE) {
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
        } */
    }

    public void signExtendByte(CiRegister reg) {
        /*if (reg.isByte()) {
            movsxb(reg, reg); // movsxb
        } else {
            shll(reg, 24);
            sarl(reg, 24);
        } */
    }

    public void signExtendShort(CiRegister reg) {
       // movsxw(reg, reg); // movsxw
    }

    // Support optimal SSE move instructions.
    public void movflt(CiRegister dst, CiRegister src) {
       /* assert dst.isFpu() && src.isFpu();
        if (AsmOptions.UseXmmRegToRegMoveAll) {
            movaps(dst, src);
        } else {
            movss(dst, src);
        }
        */
    }

    public void movflt(CiRegister dst, CiAddress src) {
        /*assert dst.isFpu();
        movss(dst, src);
        */
    }

    public void movflt(CiAddress dst, CiRegister src) {
        /*assert src.isFpu();
        movss(dst, src);
        */
    }

    public void movdbl(CiRegister dst, CiRegister src) {
        /*assert dst.isFpu() && src.isFpu();
        if (AsmOptions.UseXmmRegToRegMoveAll) {
            movapd(dst, src);
        } else {
            movsd(dst, src);
        }
        */
    }

    public void movdbl(CiRegister dst, CiAddress src) {
        /*assert dst.isFpu();
        if (AsmOptions.UseXmmLoadAndClearUpper) {
            movsd(dst, src);
        } else {
            movlpd(dst, src);
        } */
    }

    public void movdbl(CiAddress dst, CiRegister src) {
        /*assert src.isFpu();
        movsd(dst, src);*/
    }

    /**
     * Non-atomic write of a 64-bit constant to memory. Do not use
     * if the address might be a volatile field!
     */
    public void movlong(CiAddress dst, long src) {
        // APN ARM layout and endianness?
        // might be the wrong layout of values in addresses
        // this all seems a  bit tricky with ARM
        // AS a hack suggestion is to push a couple of registers onto the stack
        // load them with the constanv values

        push(ConditionFlag.Always,(1<<12)|1|2);// r13 is the stack pointer
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r0,(int)(0xffffffffL&src));
        mov32BitConstant(ARMV7.r1,(int) ((src>>32)&0xffffffffL));
        str(ConditionFlag.Always,0,0,0,ARMV7.r0,ARMV7.r12,ARMV7.r0,0,0);
        add(ConditionFlag.Always,false,ARMV7.r12,ARMV7.r12,4,0); // add 4 to the address
        str(ConditionFlag.Always,0,0,0,ARMV7.r1,ARMV7.r12,ARMV7.r1,0,0);
        pop(ConditionFlag.Always,(1<<12)|1|2); //restore all
        //CiAddress high = new CiAddress(dst.kind, dst.base, dst.index, dst.scale, dst.displacement + 4);

        /*
        movl(dst, (int) (src & 0xFFFFFFFF));
        movl(high, (int) (src >> 32));*/
    }

    public void xchgptr(CiRegister src1, CiRegister src2) {
        //xchgq(src1, src2);
    }

    public void flog(CiRegister dest, CiRegister value, boolean base10) {
        /*assert value.spillSlotSize == dest.spillSlotSize;

        CiAddress tmp = new CiAddress(CiKind.Double, ARMV7.RSP);
        if (base10) {
            fldlg2();
        } else {
            fldln2();
        }
        subq(ARMV7.rsp, value.spillSlotSize);
        movsd(tmp, value);
        fld(tmp);
        fyl2x();
        fstp(tmp);
        movsd(dest, tmp);
        addq(ARMV7.rsp, dest.spillSlotSize);
        */
    }

    public void fsin(CiRegister dest, CiRegister value) {
        //ftrig(dest, value, 's');
    }

    public void fcos(CiRegister dest, CiRegister value) {
       // ftrig(dest, value, 'c');
    }

    public void ftan(CiRegister dest, CiRegister value) {
        //ftrig(dest, value, 't');
    }

    private void ftrig(CiRegister dest, CiRegister value, char op) {
        /*assert value.spillSlotSize == dest.spillSlotSize;

        CiAddress tmp = new CiAddress(CiKind.Double, ARMV7.RSP);
        subq(ARMV7.rsp, value.spillSlotSize);
        movsd(tmp, value);
        fld(tmp);
        if (op == 's') {
            fsin();
        } else if (op == 'c') {
            fcos();
        } else if (op == 't') {
            fptan();
            fstp(0); // ftan pushes 1.0 in addition to the actual result, pop
        } else {
            throw new InternalError("should not reach here");
        }
        fstp(tmp);
        movsd(dest, tmp);
        addq(ARMV7.rsp, dest.spillSlotSize);
        */
    }

    /**
     * Emit code to save a given set of callee save registers in the
     * {@linkplain CiCalleeSaveLayout CSA} within the frame.
     * @param csl the description of the CSA
     * @param frameToCSA offset from the frame pointer to the CSA
     */
    public void save(CiCalleeSaveLayout csl, int frameToCSA) {
        //CiRegisterValue frame = frameRegister.asValue();
        // APN if we assume that the first register is offset ZERO then we can save/restore them using a
        // single stm/ldm instruction for BASE REGISTERS but what about floating point!!!!!
        // this will need to be expanded to use VSTM/VLDM as necessary
        boolean      first = true;
        int oldOffset = -1;
        int registerList = 0;
        for (CiRegister r : csl.registers) {
            int offset = csl.offsetOf(r);
            registerList = (1<<(r.encoding & 0xf)); // only storing one register at a time.
            push(ConditionFlag.Always,registerList);// r13 is the stack po
            /*if (first) {
                //if(offset != 0) System.err.println("off set is " + offset);
               // assert(offset == 0);
                // if it fails then we need to add a value to the frame pointer
                first = false;
            }else {
                //System.err.println("New offset is " + offset);
                //assert(oldOffset == (offset-4));
                // if it fails then we cannot to stm
            } */

            oldOffset = offset;
            //movq(new CiAddress(target.wordKind, frame, frameToCSA + offset), r);
        }
        //stm(ConditionFlag.Always,0,0,1,0,ARMV7.r13,registerList);// r13 is the stack pointer

    }

    public void restore(CiCalleeSaveLayout csl, int frameToCSA) {
        CiRegisterValue frame = frameRegister.asValue();
        int registerList = 0;
        for (CiRegister r : csl.registers) {
            int offset = csl.offsetOf(r);
            registerList = (1<<(r.encoding & 0xf));
            //movq(r, new CiAddress(target.wordKind, frame, frameToCSA + offset));
            // APN TODO check that it is ok to use the stack pointer here  for ARM
            // TODO this means seeing if the frameRegister might somehow be used by Stubs
            // or Adapters in an unusual or unexpected way.
            pop(ConditionFlag.Always,registerList);

        }
    }
}
