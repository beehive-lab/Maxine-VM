/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.oracle.max.asm.target.riscv64;

import static com.oracle.max.asm.target.riscv64.RISCV64opCodes.*;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.RiRegisterConfig;

public class RISCV64MacroAssembler extends RISCV64Assembler {
    public RISCV64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public void mov(CiRegister dst, CiRegister src) {
        and(dst, src, src);
    }

    public void mov64BitConstant(CiRegister dst, long imm64) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    public void mov32BitConstant(CiRegister dst, int imm32) {
        lui(dst, imm32 >> 12);
        addi(dst, dst, imm32 % (2 << 11));
    }

    public void mov(CiRegister rd, long imm) {
        if (imm <= Integer.MAX_VALUE) {
            mov32BitConstant(rd, (int) imm);
        } else {
            mov64BitConstant(rd, imm);
        }
    }

    public void nop() {
        addi(RISCV64.x0, RISCV64.x0, 0);
    }

    public void nop(int times) {
        for (int i = 0; i < times; i++) {
            nop();
        }
    }

    public void subi(CiRegister rd, CiRegister rs, int imm32) {
        addi(rd, rs, -imm32);
    }

    public void push(CiRegister reg) {
        subi(RISCV64.sp, RISCV64.sp, 32);
        sw(reg, RISCV64.sp, 0);
    }

    public void pop(CiRegister reg) {
        lw(reg, RISCV64.sp, 0);
        addi(RISCV64.sp, RISCV64.sp, 32);
    }

    public void push(CiRegister... registers) {
        for (CiRegister register : registers) {
            push(register);
        }
    }

    public void pop(CiRegister... registers) {
        for (CiRegister register : registers) {
            pop(register);
        }
    }

    public void push(int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                push(RISCV64.cpuRegisters[regNumber]);
            }

            registerList = registerList >> 1;
        }
    }

    public void pop(int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                pop(RISCV64.cpuRegisters[regNumber]);
            }
        }
    }

    /**
     * Checks whether immediate can be encoded as an arithmetic immediate.
     *
     * @param imm Immediate has to be either an unsigned 12bit value or an unsigned 24bit value with
     *            the lower 12 bits 0.
     * @return true if valid arithmetic immediate, false otherwise.
     */
    public static boolean isAimm(int imm) {
        return NumUtil.isUnsignedNbit(12, imm) ||
                NumUtil.isUnsignedNbit(12, imm >>> 12) && (imm & 0xfff) == 0;
    }

    /**
     * @return True if immediate can be used directly for arithmetic instructions (add/sub), false otherwise.
     */
    public static boolean isArithmeticImmediate(long imm) {
        // If we have a negative immediate we just use the opposite operator. I.e.: x - (-5) == x + 5.
        return NumUtil.isInt(Math.abs(imm)) && isAimm((int) Math.abs(imm));
    }

    public void add(CiRegister dest, CiRegister source, long delta) {
        if (delta == 0) {
            mov(dest, source);
        } else if (isArithmeticImmediate(delta)) {
            assert delta == (int) delta;
            super.addi(dest, source, (int) delta);
        } else {
            assert dest != scratchRegister;
            assert source != scratchRegister;
            mov(scratchRegister, delta);
            add(dest, source, scratchRegister);
        }
    }

    /**
     * Applies a delta value to the contents of reg as a 32bit quantity.
     * @param reg
     * @param delta
     */
    public void increment32(CiRegister reg, int delta) {
        add(reg, reg, delta);
    }
}
