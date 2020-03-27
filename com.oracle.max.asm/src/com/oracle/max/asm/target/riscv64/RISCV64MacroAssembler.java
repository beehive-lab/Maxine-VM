/*
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
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
    /**
     * Reserved space for worst case scenario.
     *
     * <code>
     *     lui   x29, %hi(offset)
     *     addi  x29, x29, %lo(offset)
     *     add   x28, x28, x29
     * </code>
     */
    public static final int PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS = 3;
    public static final int INSTRUCTION_SIZE = 4;

    public static final int CALL_TRAMPOLINE_INSTRUCTIONS = 6;
    public static final int RIP_CALL_INSTRUCTION_SIZE = ((2 * CALL_TRAMPOLINE_INSTRUCTIONS) + 1) * INSTRUCTION_SIZE;
    public static final int CALL_TRAMPOLINE1_OFFSET = INSTRUCTION_SIZE;
    public static final int CALL_TRAMPOLINE2_OFFSET = INSTRUCTION_SIZE * (CALL_TRAMPOLINE_INSTRUCTIONS + 1);
    public static final int CALL_BRANCH_OFFSET = RIP_CALL_INSTRUCTION_SIZE - INSTRUCTION_SIZE;
    public static final int MOV_OFFSET_IN_TRAMPOLINE = 2 * INSTRUCTION_SIZE;

    private  static final int MOV_32_BIT_CONSTANT_INSTRUCTION_NUMBER = 2;

    /**
     * Same variables are declared in RISCV64T1XCompilation. However the values here are decremented by one because
     * MacroAssembler patching is done using the codebuffer as opposed to a separate data buffer as in T1X.
     * Therefore, we use less patch nops to include the extra instructions needed.
     */
    private static final int PATCH_BRANCH_CONDITIONALLY_NOPS = 2;
    private static final int PATCH_BRANCH_UNCONDITIONALLY_NOPS = 1;

    private static final int nopInstructionEncoding = RISCV64MacroAssembler.addImmediateHelper(RISCV64.x0, RISCV64.x0, 0);

    public RISCV64MacroAssembler(CiTarget target) {
        super(target);
    }

    public RISCV64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    /**
     * Return the displacement of the target of a branch immediate instruction.
     * @param instruction
     * @return
     */
    public static int jumpAndLinkExtractDisplacement(int instruction) {
        assert (instruction & NumUtil.getNbitNumberInt(7)) == JAL.getValue() :
                "Not a branch immediat instruction: 0x" + Integer.toHexString(instruction);

        instruction = instruction >>> 12;
        int displacement = ((instruction >>> 9) & 0x3FF) | (((instruction >>> 8) & 0x1) << 10) |
                            ((instruction & 0xFF) << 11) | (((instruction >>> 19) & 0x1) << 19);
        displacement = displacement << 1;
        // check the sign bit
        if (((1 << 20) & displacement) == 0) {
            return displacement;
        }
        // negative number -- sign extend.
        return displacement | (0xFFF << 20);
    }

    /**
     * Because RISC-V conditional branching only supports 12 bit signed offsets and unconditional
     * branching supports 20 bit signed offsets, the simple solution was to move the 32 bit offset into a register,
     * and then add a conditional jump with the negated condition to go past the direct 32 bit jump.
     * The following is an example of two equivalent cases, the first when the offset is 12 bit signed, and the second when the offset is 32 bit signed:
     * 12 bit signed:
     * +0: beq rs1, rs2, offset
     *
     * 32 bit signed:
     * +0: bneq rs1, rs2, +12
     * +4: auipc x30, offset (takes the upper 20 bits)
     * +8: jalr x30, offset (takes the lower 20 bits)
     * +12: past jump instruction (we jump here if the initial condition doesn't hold)
    */
    @Override
    protected void patchJumpTarget(int branch, int target) {
        int branchOffset = target - branch;
        PatchLabelKind type = PatchLabelKind.fromEncoding(codeBuffer.getByte(branch));
        switch (type) {
            case BRANCH_CONDITIONALLY: {
                assertIfNops(branch - PATCH_BRANCH_CONDITIONALLY_NOPS * INSTRUCTION_SIZE, PATCH_BRANCH_CONDITIONALLY_NOPS);
                ConditionFlag cf = ConditionFlag.fromEncoding(codeBuffer.getByte(branch + 1));
                CiRegister rs1 = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 2) & 0b11111];
                CiRegister rs2 = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 3) & 0b11111];
                if (isArithmeticImmediate(branchOffset)) {
                    emitConditionalBranch(cf, rs1, rs2, branchOffset, branch);
                } else {
                    // We can't negate ConditionFlag.AL
                    if (cf != ConditionFlag.AL) {
                        emitConditionalBranch(cf.negate(), rs1, rs2, 3 * INSTRUCTION_SIZE, branch - 2 * INSTRUCTION_SIZE);
                    }
                    insert32BitJumpForPatch(branchOffset, branch);
                }
                break;
            }
            case TABLE_SWITCH: {
                assert codeBuffer.getByte(branch + 1) == 0;
                assert codeBuffer.getShort(branch + 2) == 0;
                jal(RISCV64.zero, branchOffset, branch);
                break;
            }
            case BRANCH_UNCONDITIONALLY: {
                assertIfNops(branch - PATCH_BRANCH_UNCONDITIONALLY_NOPS * INSTRUCTION_SIZE, PATCH_BRANCH_UNCONDITIONALLY_NOPS);
                assert codeBuffer.getByte(branch + 1) == 0;
                assert codeBuffer.getShort(branch + 2) == 0;
                if (NumUtil.isSignedNbit(JTYPE_IMM_BITS, (long) branchOffset)) {
                    jal(RISCV64.zero, branchOffset, branch);
                } else {
                    insert32BitJumpForPatch(branchOffset, branch);
                }
                break;
            }
            case BRANCH_NONZERO:
                throw new UnsupportedOperationException("Unimplemented");
            case BRANCH_ZERO: {
                assertIfNops(branch - PATCH_BRANCH_CONDITIONALLY_NOPS * INSTRUCTION_SIZE, PATCH_BRANCH_CONDITIONALLY_NOPS);
                assert codeBuffer.getShort(branch + 2) == 0;
                CiRegister cmp = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 1) & 0b11111];
                if (isArithmeticImmediate(branchOffset)) {
                    emitConditionalBranch(ConditionFlag.EQ, cmp, RISCV64.zero, branchOffset, branch);
                } else {
                    emitConditionalBranch(ConditionFlag.EQ.negate(), cmp, RISCV64.zero, 3 * INSTRUCTION_SIZE, branch - 2 * INSTRUCTION_SIZE);
                    insert32BitJumpForPatch(branchOffset, branch);
                }
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    public void assertIfNops(int startPos, int numberOfNops) {
        for (int i = 0; i < numberOfNops; i++) {
            assert codeBuffer.getInt(startPos  + i * INSTRUCTION_SIZE) == nopInstructionEncoding;
        }
    }

    public void insert32BitJump(int address) {
        if ((address & 0xFFF) >>> 11 == 0b0) {
            auipc(RISCV64.x30, address);
        } else {
            auipc(RISCV64.x30, address - (address | 0xFFFFF000));
        }
        jalr(RISCV64.zero, RISCV64.x30, address);
    }

    private void insert32BitJumpForPatch(int address, int position) {
        // Adjust address to compensate with the fact that auipc will use PC(jalr) - INSTRUCTION_SIZE
        address += INSTRUCTION_SIZE;
        insert32BitJumpAtPosition(address, position);
    }

    private void insert32BitJumpAtPosition(int address, int position) {
        if ((address & 0xFFF) >>> 11 == 0b0) {
            auipc(RISCV64.x30, address, position - INSTRUCTION_SIZE);
        } else {
            auipc(RISCV64.x30, address - (address | 0xFFFFF000), position - INSTRUCTION_SIZE);
        }
        jalr(RISCV64.zero, RISCV64.x30, address, position);
    }

    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            assert modulus % 4 == 0;
            nop((modulus - (codeBuffer.position() % modulus)) / 4);
        }
    }

    public int getAlignNopTimes(int modulus, int codeBufferPosition) {
        return (modulus - (codeBufferPosition % modulus)) / 4;
    }

    /**
     * When patching up Labels we have to know what kind of code to generate.
     */
    public enum PatchLabelKind {
        BRANCH_CONDITIONALLY(0x0),
        BRANCH_UNCONDITIONALLY(0x1),
        BRANCH_NONZERO(0x2),
        BRANCH_ZERO(0x3),
        TABLE_SWITCH(0x4);

        public final int encoding;

        PatchLabelKind(int encoding) {
            this.encoding = encoding;
        }

        /**
         * @return PatchLabelKind with given encoding.
         */
        private static PatchLabelKind fromEncoding(int encoding) {
            return values()[encoding];
        }
    }


    public final void alignForPatchableDirectCall(int callPos) {
        assert callPos % INSTRUCTION_SIZE == 0 : "Should be 4 bytes aligned";
    }

    public void mov(CiRegister dst, CiRegister src) {
        and(dst, src, src);
    }

    public void fmov(int size, CiRegister dst, CiRegister src) {
        if (dst.equals(src)) {
            return;
        }
        if (RISCV64.isIntReg(dst)) {
            // Note: We need this check because the ABI can allow for FPU values to be hold in CPU registers
            if (RISCV64.isIntReg(src)) {
                mov(dst, src);
            } else {
                if (size == 32) {
                    fmvxw(dst, src);
                } else {
                    fmvxd(dst, src);
                }
            }
        } else if (RISCV64.isIntReg(src)) {
            if (size == 32) {
                fmvwx(dst, src);
            } else {
                fmvdx(dst, src);
            }
        } else {
            if (size == 32) {
                fsgnjs(dst, src, src);
            } else {
                fsgnjd(dst, src, src);
            }
        }
    }

    public void mov64BitConstant(CiRegister dst, long imm64) {
        //TODO improve this to get rid of scratchRegister1

        assert dst != scratchRegister1;
        mov32BitConstant(dst, (int) (imm64 >>> 32));
        slli(dst, dst, 32);
        mov32BitConstant(scratchRegister1, (int) imm64);
        slli(scratchRegister1, scratchRegister1, 32);
        srli(scratchRegister1, scratchRegister1, 32);

        add(dst, dst, scratchRegister1);
    }

    public void mov32BitConstant(CiRegister dst, int imm32) {
        // Any change made to this function must also be applied to mov32BitConstantHelper
        // Any change made to this function must also be applied to RISCV64TargetMethodUtil::getDisplacementFromTrampoline
        // Any change made to this function must also be applied to RISCV64DeoptStubPatch::apply
        if (imm32 == 0) {
            and(dst, RISCV64.x0, RISCV64.x0);
            return;
        }

        if ((imm32 & 0xFFF) >>> 11 == 0b0) {
            lui(dst, imm32);
        } else {
            lui(dst, imm32 - (imm32 | 0xFFFFF000));
        }
        if (imm32 > 0) {
            addiw(dst, dst, imm32);
        } else {
            addi(dst, dst, imm32);
        }
    }

    public static int[] mov32BitConstantHelper(CiRegister dst, int imm32) {
        int[] instructions = new int[MOV_32_BIT_CONSTANT_INSTRUCTION_NUMBER];

        if (imm32 == 0) {
            // and(dst, RISCV64.x0, RISCV64.x0);
            instructions[0] = AND.getValue() | dst.number << 7 | 7 << 12 |
                    RISCV64.x0.number << 15 | RISCV64.x0.number << 20;
            return instructions;
        }

        if ((imm32 & 0xFFF) >>> 11 == 0b0) {
            // lui(dst, imm32);
            instructions[0] = LUI.getValue() | dst.number << 7 | (imm32 & 0xFFFFF000);
        } else {
            // lui(dst, (imm32 + (0b1 << 12)) & 0xFFFFF000);
            instructions[0] = LUI.getValue() | dst.number << 7 | ((imm32 - (imm32 | 0xFFFFF000)) & 0xFFFFF000);
        }
        if (imm32 > 0) {
            // addiw(dst, dst, imm32);
            instructions[1] = COMP64.getValue() | dst.number << 7 | 0 << 12 | dst.number << 15 | imm32 << 20;
        } else {
            // addi(dst, dst, imm32);
            instructions[1] = COMP.getValue() | dst.number << 7 | 0 << 12 | dst.number << 15 | imm32 << 20;
        }

        return instructions;
    }

    public static int shiftLeftLogicImmediateHelper(CiRegister dst, CiRegister rs, int imm32) {
        return COMP.getValue() | dst.number << 7 | 1 << 12 | rs.number << 15 | imm32 << 20;
    }

    public static int loadUpperImmediateHelper(CiRegister dst, int imm32) {
        return LUI.getValue() | dst.number << 7 | (imm32 & 0xFFFFF000);
    }

    public static int addImmediateHelper(CiRegister rd, CiRegister rs,  int imm12) {
        return COMP.getValue() | rd.getEncoding() << 7 | 0 << 12 | rs.getEncoding() << 15 | imm12 << 20;
    }

    public static int addImmediateWordHelper(CiRegister rd, CiRegister rs, int imm12) {
        return COMP64.getValue() | rd.getEncoding() << 7 | 0 << 12 | rs.getEncoding() << 15 | imm12 << 20;
    }

    public static int addSubInstructionHelper(CiRegister rd, CiRegister rs1, CiRegister rs2, boolean isNegative) {
        if (isNegative) {
            return SUB.getValue() | rd.getEncoding() << 7 |  0 << 12 | rs1.getEncoding() << 15 |
                    rs2.getEncoding() << 20 | 32 << 25;
        }
        return ADD.getValue() | rd.getEncoding() << 7 |  0 << 12 | rs1.getEncoding() << 15 |
                rs2.getEncoding() << 20 | 0 << 25;
    }

    public static int jumpAndLinkHelper(CiRegister rd, CiRegister rs, int imm32) {
        return JALR.getValue() | rd.getEncoding() << 7 | 0 << 12 | rs.getEncoding() << 15 | imm32 << 20;
    }

    public static int jumpAndLinkImmediateHelper(CiRegister rd, int imm32) {
        int instruction = JAL.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= ((imm32 >> 20) & 1) << 31; // This places bit 20 of imm32 in bit 31 of instruction
        instruction |= ((imm32 >> 1) & 0x3FF) << 21; // This places bits 10:1 of imm32 in bits 30:21 of instruction
        instruction |= ((imm32 >> 11) & 1) << 20; // This places bit 11 of imm32 in bit20 of instruction
        instruction |= ((imm32 >> 12) & 0xFF) << 12; // This places bits 19:12 of imm32 in bits 19:12 of instruction
        return instruction;
    }

    public static int branchNotEqualHelper(CiRegister rs1, CiRegister rs2, int imm32) {
        int instruction = BRNC.getValue();
        instruction |= ((imm32 >> 11) & 1) << 7;
        instruction |= ((imm32 >> 1) & 0xF) << 8;
        instruction |= 1 << 12;
        instruction |= rs1.getEncoding() << 15;
        instruction |= rs2.getEncoding() << 20;
        instruction |= ((imm32 >> 5) & 0x3F) << 25;
        instruction |= ((imm32 >> 12) & 1) << 31;
        return instruction;
    }

    public static int ldHelper(CiRegister rd, CiRegister rs, int imm32) {
        int instruction = LD.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= 3 << 12;
        instruction |= rs.getEncoding() << 15;
        instruction |= imm32 << 20;
        return instruction;
    }

    /**
     * Checks if the given jump instruction is a linked branch or not.
     * If it is linked, then rd != RISCV64.x0
     *
     * @param instruction the machine code of the original instruction
     * @return {@code true} if the instruction is a linked branch
     */
    public static boolean isJumpLinked(int instruction) {
        assert isJumpInstruction(instruction) : Integer.toHexString(instruction);
        int rdReg = (instruction >>> 7) & 0b11111;
        return rdReg != RISCV64.x0.getEncoding();
    }

    public static boolean isJumpInstruction(int instruction) {
        int opcode = instruction & 0b1111111;
        return opcode == JALR.getValue() || opcode == JAL.getValue();
    }

    public static boolean isAddInstruction(int instruction) {
        assert isAddSubInstruction(instruction) : instruction;
        int opcode = instruction & 0b1111111;
        int funct7 = instruction >>> 25;
        return opcode == ADD.getValue() && funct7 == 0b0;
    }

    public static boolean isAddSubInstruction(int instruction) {
        int opcode = instruction & 0b1111111;
        int funct7 = instruction >>> 25;
        return opcode == ADD.getValue() &&
                (funct7 == 0b0 || funct7 == 0b0100000);
    }

    public static boolean isAndInstruction(int instruction) {
        return (instruction & 0b1111111) == RISCV64opCodes.AND.getValue();
    }

    public static int extractAddiImmediate(int instruction) {
        assert (instruction & 0b1111111) == RISCV64opCodes.COMP.getValue() : Integer.toBinaryString(instruction);
        return instruction >>> 20;
    }

    public static int extractLuiImmediate(int instruction) {
        assert (instruction & 0b1111111) == RISCV64opCodes.LUI.getValue() : Integer.toBinaryString(instruction);
        return instruction >>> 12;
    }

    public static boolean isSlliInstruction(int instruction) {
        return (instruction & 0b1111111) == RISCV64opCodes.COMP.getValue() &&
                ((instruction >>> 12) & 0b111) == 0b001;
    }

    public static boolean isSrliInstruction(int instruction) {
        return (instruction & 0b1111111) == RISCV64opCodes.COMP.getValue() &&
                ((instruction >>> 12) & 0b111) == 0b101;
    }

    public void mov(CiRegister rd, long imm) {
        if (imm <= Integer.MAX_VALUE && imm >= Integer.MIN_VALUE) {
            mov32BitConstant(rd, (int) imm);
        } else {
            mov64BitConstant(rd, imm);
        }
    }

    public void movByte(CiRegister rd, int imm) {
        int val = imm & 0xFF;
        if (val >>> 7 == 1) {
            val = ~0xFF | val;
        }

        mov64BitConstant(rd, imm);
    }

    public void movShort(CiRegister rd, int imm) {
        int val = imm & 0xFFFF;
        if (val >>> 15 == 1) {
            val = ~0xFFFF | val;
        }

        mov64BitConstant(rd, imm);
    }

    public void addi(int size, CiRegister dst, CiRegister rs1, int immediate) {
        if (size == 32) {
            super.addiw(dst, rs1, immediate);
        } else {
            super.addi(dst, rs1, immediate);
        }
    }

    public void add(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.addw(rd, rs1, rs2);
        } else {
            super.add(rd, rs1, rs2);
        }
    }

    public void sub(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.subw(rd, rs1, rs2);
        } else {
            super.sub(rd, rs1, rs2);
        }
    }

    public void mul(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.mulw(rd, rs1, rs2);
        } else {
            super.mul(rd, rs1, rs2);
        }
    }

    public void div(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.divw(rd, rs1, rs2);
        } else {
            super.div(rd, rs1, rs2);
        }
    }

    public void divu(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.divuw(rd, rs1, rs2);
        } else {
            super.divu(rd, rs1, rs2);
        }
    }

    public void rem(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.remw(rd, rs1, rs2);
        } else {
            super.rem(rd, rs1, rs2);
        }
    }


    public void fadd(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fadds(rd, rs1, rs2);
        } else {
            super.faddd(rd, rs1, rs2);
        }
    }

    public void fsub(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fsubs(rd, rs1, rs2);
        } else {
            super.fsubd(rd, rs1, rs2);
        }
    }

    public void fmul(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fmuls(rd, rs1, rs2);
        } else {
            super.fmuld(rd, rs1, rs2);
        }
    }

    public void fmulRTZ(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fmulsRTZ(rd, rs1, rs2);
        } else {
            super.fmuldRTZ(rd, rs1, rs2);
        }
    }

    public void fdiv(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fdivs(rd, rs1, rs2);
        } else {
            super.fdivd(rd, rs1, rs2);
        }
    }

    public void fdivRTZ(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fdivsRTZ(rd, rs1, rs2);
        } else {
            super.fdivdRTZ(rd, rs1, rs2);
        }
    }

    public void frem(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        // There is no frem instruction, instead we compute the remainder using the relation:
        // rem = n - Truncating(n / d) * d
        this.fdiv(size, RISCV64.f31, rs1, rs2);
        if (size == 64) {
            fcvtldRTZ(scratchRegister, RISCV64.f31);
            fcvtdl(RISCV64.f31, scratchRegister);
        } else {
            fcvtwsRTZ(scratchRegister, RISCV64.f31);
            fcvtsw(RISCV64.f31, scratchRegister);
        }
        this.fmul(size, RISCV64.f31, RISCV64.f31, rs2);
        this.fsub(size, rd, rs1, RISCV64.f31);
    }

    public void fabs(int size, CiRegister rd, CiRegister rs1) {
        if (size == 32) {
            super.fsgnjxs(rd, rs1, rs1);
        } else {
            super.fsgnjxd(rd, rs1, rs1);
        }
    }

    public void fsqrt(int size, CiRegister rd, CiRegister rs) {
        if (size == 32) {
            super.fsqrts(rd, rs);
        } else {
            super.fsqrtd(rd, rs);
        }
    }

    /**
     * dst = -src1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void neg(int size, CiRegister dst, CiRegister src) {
        sub(size, dst, RISCV64.zr, src);
    }

    /**
     * dst = -src1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void fneg(int size, CiRegister dst, CiRegister src) {
        if (size == 32) {
            fsgnjns(dst, src, src);
        } else {
            fsgnjnd(dst, src, src);
        }
    }

    public int insertDivByZeroCheck(CiRegister denominator) {
        Label jumpLabel = new Label();
        mov32BitConstant(scratchRegister1, 0);
        mov(RISCV64.x30, denominator);
        branchConditionally(ConditionFlag.NE, RISCV64.x30, scratchRegister1, jumpLabel);
        int offset = codeBuffer.position();
        ldru(64, RISCV64.zero, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister1));
        bind(jumpLabel);
        return offset;
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

    public void push(int size, CiRegister reg) {
        assert size == 8 || size == 16 || size == 32 || size == 64 : "Unimplemented push for size: " + size;
        subi(RISCV64.sp, RISCV64.sp, 16);
        str(size, RISCV64.sp, reg, 0);
    }

    public void pop(int size, CiRegister reg) {
        assert size == 8 || size == 16 || size == 32 || size == 64 : "Unimplemented pop for size: " + size;
        ldru(size, reg, RISCV64.sp, 0);
        addi(RISCV64.sp, RISCV64.sp, 16);
    }

    public void pop(int size, CiRegister reg, boolean unsigned) {
        assert size == 8 || size == 16 || size == 32 || size == 64 : "Unimplemented pop for size: " + size;
        if (unsigned) {
            ldru(size, reg, RISCV64.sp, 0);
        } else {
            ldr(size, reg, RISCV64.sp, 0);
        }
        addi(RISCV64.sp, RISCV64.sp, 16);
    }

    public void fpush(int size, CiRegister reg) {
        assert size == 32 || size == 64 : "Unimplemented push for size: " + size;
        subi(RISCV64.sp, RISCV64.sp, 16);
        fstr(size, RISCV64.sp, reg, 0);
    }

    public void fpop(int size, CiRegister reg) {
        assert size == 32 || size == 64 : "Unimplemented pop for size: " + size;
        fldr(size, reg, RISCV64.sp, 0);
        addi(RISCV64.sp, RISCV64.sp, 16);
    }

    public void fpush(int size, CiRegister... registers) {
        for (CiRegister register : registers) {
            fpush(size, register);
        }
    }

    public void fpop(int size, CiRegister... registers) {
        for (CiRegister register : registers) {
            fpop(size, register);
        }
    }

    public void fpush(int size, int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                fpush(size, RISCV64.fpuRegisters[regNumber]);
            }

            registerList = registerList >> 1;
        }
    }

    public void fpop(int size, int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                fpop(size, RISCV64.fpuRegisters[regNumber]);
            }
        }
    }

    public void push(int size, int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                push(size, RISCV64.cpuRegisters[regNumber]);
            }

            registerList = registerList >> 1;
        }
    }

    public void pop(int size, boolean unsigned, int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                pop(size, RISCV64.cpuRegisters[regNumber], unsigned);
            }
        }
    }

    public void push(int size, CiRegister... registers) {
        for (CiRegister register : registers) {
            push(size, register);
        }
    }

    public void pop(int size, boolean unsigned, CiRegister... registers) {
        for (CiRegister register : registers) {
            pop(size, register, unsigned);
        }
    }


    public void membar() {
        fence(0b1111, 0b1111);
        fencei();
    }

    /**
     * Compare and swap implementation.
     *
     * @param size
     * @param newValue
     * @param compareValue
     * @param address
     */
    public void cas(int size, CiRegister newValue, CiRegister compareValue, RISCV64Address address) {
        assert scratchRegister != compareValue;
        assert newValue != compareValue;
        assert size <= 64;

        // No support added for loadReserved/storeConditional with different addressing mode
        assert address.getAddressingMode() == RISCV64Address.AddressingMode.BASE_REGISTER_ONLY;

        Label atomicFail = new Label();
        Label notEqualTocmpValue = new Label();

        bind(atomicFail);
        loadReserved(size, scratchRegister, address.getBase(), 0b1, 0b1); // scratch has the current Value

        branchConditionally(ConditionFlag.NE, compareValue, scratchRegister, notEqualTocmpValue); // compare scratch with cmpValue; value was not equal to the cmpValue
        storeConditional(size, scratchRegister, newValue, address.getBase(), 0b1, 0b1); // store newValue to address and result to scratch register

        // If the Condition is Equal then the storeConditional took place but it MIGHT have failed so we need to test for this.
        // If the scratch register is not 0 then there was an issue with atomicity so do the operation again
        branchConditionally(ConditionFlag.NE, scratchRegister, RISCV64.zero, atomicFail);
        mov(scratchRegister, compareValue); // set scratch register to the cmp value to indicate success
        bind(notEqualTocmpValue);
        membar();
    }

    public void loadReserved(int size, CiRegister dest, CiRegister addr, int aq, int rl) {
        assert size == 32 || size == 64;
        assert (aq & 0b1) == aq;
        assert (rl & 0b1) == rl;

        if (size == 32) {
            lrw(dest, addr, aq, rl);
        } else {
            lrd(dest, addr, aq, rl);
        }
    }

    public void storeConditional(int size, CiRegister dest, CiRegister src, CiRegister addr, int aq, int rl) {
        assert size == 32 || size == 64;
        assert (aq & 0b1) == aq;
        assert (rl & 0b1) == rl;

        if (size == 32) {
            scw(dest, addr, src, aq, rl);
        } else {
            scd(dest, addr, src, aq, rl);
        }
    }

    /**
     * Count Trailing Zeros implementation.
     * RISC-V lacks this instruction
     *
     * @param size
     * @param dst
     * @param src
     */
    public void ctz(int size, CiRegister dst, CiRegister src) {
        boolean scratchAlreadyUsed = src.number == scratchRegister.number;
        Label end = new Label();
        Label continuel = new Label();
        mov32BitConstant(scratchRegister1, 0b1);
        mov32BitConstant(dst, 0);
        this.bind(continuel);
        if (scratchAlreadyUsed) {
            push(64, scratchRegister1);
            and(scratchRegister1, src, scratchRegister1);
            branchConditionally(ConditionFlag.NE, scratchRegister1, RISCV64.zero, end);
        } else {
            and(scratchRegister, src, scratchRegister1);
            branchConditionally(ConditionFlag.NE, scratchRegister, RISCV64.zero, end);
        }
        addi(dst, dst, 1);
        if (scratchAlreadyUsed) {
            pop(64, scratchRegister1, true);
        }
        slli(scratchRegister1, scratchRegister1, 1);
        b(continuel);

        this.bind(end);
        if (scratchAlreadyUsed) {
            pop(64, scratchRegister1, true);
        }
    }

    /**
     * Count Leading Zeros implementation.
     * RISC-V lacks this instruction
     *
     * @param size
     * @param dst
     * @param src
     */
    public void clz(int size, CiRegister dst, CiRegister src) {
        boolean scratchAlreadyUsed = src.number == scratchRegister.number;
        Label end = new Label();
        Label continuel = new Label();
        mov32BitConstant(scratchRegister1, 0b1);
        mov32BitConstant(dst, 0);
        if (size == 32) {
            slli(scratchRegister1, scratchRegister1, 31);
        } else {
            slli(scratchRegister1, scratchRegister1, 63);
        }
        this.bind(continuel);
        if (scratchAlreadyUsed) {
            push(64, scratchRegister1);
            and(scratchRegister1, src, scratchRegister1);
            branchConditionally(ConditionFlag.NE, scratchRegister1, RISCV64.zero, end);
        } else {
            and(scratchRegister, src, scratchRegister1);
            branchConditionally(ConditionFlag.NE, scratchRegister, RISCV64.zero, end);
        }
        addi(dst, dst, 1);
        if (scratchAlreadyUsed) {
            pop(64, scratchRegister1, true);
        }
        srli(scratchRegister1, scratchRegister1, 1);
        b(continuel);

        this.bind(end);
        if (scratchAlreadyUsed) {
            pop(64, scratchRegister1, true);
        }
    }

    /**
     * Compare register and branch if zero.
     *
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param label Can only handle 32-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void cbz(CiRegister cmp, Label label) {
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            emitConditionalBranch(RISCV64MacroAssembler.ConditionFlag.EQ, cmp, RISCV64.zero, offset);
        } else {
            // When the label is unbound, we are not sure if we are going to need 1 or 3 instructions when patching (depends on the offset size).
            nop(PATCH_BRANCH_CONDITIONALLY_NOPS);
            label.addPatchAt(codeBuffer.position());
            int regEncoding = cmp.getEncoding();
            emitByte(PatchLabelKind.BRANCH_ZERO.encoding);
            emitByte(regEncoding);
            emitShort(0);
        }
    }

    /**
     * Checks whether immediate can be encoded as an arithmetic immediate.
     *
     * @param imm Immediate has to be either an unsigned 11bit value or a signed 12bit value.
     * @return true if valid arithmetic immediate, false otherwise.
     */
    public static boolean isAimm(int imm) {
        return imm >= 0 ? NumUtil.isUnsignedNbit(11, imm) : NumUtil.isSignedNbit(12, imm);
    }

    /**
     * @return True if immediate can be used directly for arithmetic instructions (add/sub), false otherwise.
     */
    public static boolean isArithmeticImmediate(long imm) {
        return NumUtil.isInt(Math.abs(imm)) && isAimm((int) imm);
    }

    public void add(int size, CiRegister dest, CiRegister source, long delta) {
        if (delta == 0) {
            mov(dest, source);
        } else if (isArithmeticImmediate(delta)) {
            assert delta == (int) delta;
            addi(size, dest, source, (int) delta);
        } else {
            CiRegister reg;
            if (dest.number != source.number && dest.number != scratchRegister1.number) {
                reg = dest;
            } else {
                reg = RISCV64.x30;
            }
            mov(reg, delta);
            add(size, dest, source, reg);
        }
    }

    public void sub(int size, CiRegister dest, CiRegister source, long delta) {
        add(size, dest, source, -delta);
    }

    /**
     * Applies a delta value to the contents of reg as a 32bit quantity.
     *
     * @param reg
     * @param delta
     */
    public void increment32(CiRegister reg, int delta) {
        add(32, reg, reg, delta);
    }

    public void b(int offset) {
        if (NumUtil.isSignedNbit(JTYPE_IMM_BITS, offset)) {
            jal(RISCV64.zero, offset);
        } else {
            insert32BitJump(offset);
        }
    }

    public void b(int offset, int pos) {
        if (NumUtil.isSignedNbit(JTYPE_IMM_BITS, offset)) {
            jal(RISCV64.zero, offset, pos);
        } else {
            // insert32BitJumpAtPosition will insert auipc at pos - INSTRUCTION_SIZE
            insert32BitJumpAtPosition(offset, pos + INSTRUCTION_SIZE);
        }
    }

    /**
     * Branch unconditionally to a label.
     *
     * @param label
     */
    public void b(Label label) {
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            b(offset);
        } else {
            // When the label is unbound, we are not sure if we are going to need 1 or 2 instructions when patching (depends on the offset size).
            nop(PATCH_BRANCH_UNCONDITIONALLY_NOPS);
            label.addPatchAt(codeBuffer.position());
            emitByte(PatchLabelKind.BRANCH_UNCONDITIONALLY.encoding);
            emitByte(0);
            emitShort(0);
        }
    }

    /**
     * Condition Flags for branches. See 4.3
     */
    public enum ConditionFlag {
        // Integer | Floating-point meanings
        /**
         * Equal | Equal.
         */
        EQ(0x0),
        /**
         * Not Equal | Not equal or unordered.
         */
        NE(0x1),
        /**
         * signed greater than or equal | greater than or equal.
         */
        GE(0x2),
        /**
         * signed less than | less than or unordered.
         */
        LT(0x3),
        /**
         * signed greater than | greater than.
         */
        GT(0x4),
        /**
         * signed less than or equal | less than, equal or unordered.
         */
        LE(0x5),
        /** unsigned greater than or equal.
         */
        GEU(0x6),
        /** unsigned less than.
         */
        LTU(0x7),
        /** unsigned greater than.
         */
        GTU(0x8),
        /** unsigned less than or equal.
         */
        LEU(0x9),
        /**
         * always | always.
         */
        AL(0xA);

        public final int encoding;

        ConditionFlag(int encoding) {
            this.encoding = encoding;
        }

        /**
         * @return ConditionFlag specified by decoding.
         */
        public static ConditionFlag fromEncoding(int encoding) {
            return values()[encoding];
        }

        public ConditionFlag negate() {
            switch (this) {
                case EQ:
                    return NE;
                case NE:
                    return EQ;
                case GE:
                    return LT;
                case LT:
                    return GE;
                case GT:
                    return LE;
                case LE:
                    return GT;
                case GEU:
                    return LTU;
                case LTU:
                    return GEU;
                case GTU:
                    return LEU;
                case LEU:
                    return GTU;
                case AL:
                default:
                    throw new Error("should not reach here");
            }
        }

        public boolean isUnsigned() {
            return this == GEU || this == LTU || this == GTU || this == LEU;
        }
    }

    public void setLessThan(CiRegister rd, CiRegister rs1, CiRegister rs2, boolean unsignedComp) {
        if (unsignedComp) {
            sltu(rd, rs1, rs2);
        } else {
            slt(rd, rs1, rs2);
        }
    }

    public void setLessThanFloatingPoint(CiRegister rd, CiRegister rs1, CiRegister rs2, boolean isDouble) {
        if (isDouble) {
            fltd(rd, rs1, rs2);
        } else {
            flts(rd, rs1, rs2);
        }
    }

    public void bgt(CiRegister rs1, CiRegister rs2, int imm32) {
        blt(rs2, rs1, imm32);
    }

    public void bgt(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        blt(rs2, rs1, imm32, pos);
    }

    public void ble(CiRegister rs1, CiRegister rs2, int imm32) {
        bge(rs2, rs1, imm32);
    }

    public void ble(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        bge(rs2, rs1, imm32, pos);
    }

    /**
     * Branches to label if condition is true.
     *
     * @param condition any condition value allowed. Non null.
     * @param label     Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void branchConditionally(ConditionFlag condition, CiRegister rs1, CiRegister rs2, Label label) {
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            if (isArithmeticImmediate(offset)) {
                emitConditionalBranch(condition, rs1, rs2, offset);
            } else {
                if (condition != ConditionFlag.AL) {
                    emitConditionalBranch(condition.negate(), rs1, rs2, 3 * INSTRUCTION_SIZE);
                    offset -= INSTRUCTION_SIZE;
                }
                insert32BitJump(offset);
            }
        } else {
            // When the label is unbound, we are not sure if we are going to need 1 or 3 instructions when patching (depends on the offset size).
            nop(PATCH_BRANCH_CONDITIONALLY_NOPS);
            label.addPatchAt(codeBuffer.position());
            emitByte(PatchLabelKind.BRANCH_CONDITIONALLY.encoding);
            emitByte(condition.encoding);
            emitByte(rs1.number);
            emitByte(rs2.number);
        }
    }

    public void emitConditionalBranch(ConditionFlag condition, CiRegister rs1, CiRegister rs2, int offset) {
        emitConditionalBranch(condition, rs1, rs2, offset, -1);
    }

    public void emitConditionalBranch(ConditionFlag condition, CiRegister rs1, CiRegister rs2, int offset, int position) {
        switch (condition) {
            case EQ:
                beq(rs1, rs2, offset, position);
                break;
            case NE:
                bne(rs1, rs2, offset, position);
                break;
            case GE:
                bge(rs1, rs2, offset, position);
                break;
            case LT:
                blt(rs1, rs2, offset, position);
                break;
            case GT:
                bgt(rs1, rs2, offset, position);
                break;
            case LE:
                ble(rs1, rs2, offset, position);
                break;
            case GEU:
                bgeu(rs1, rs2, offset, position);
                break;
            case LTU:
                bltu(rs1, rs2, offset, position);
                break;
            case GTU:
                bltu(rs2, rs1, offset, position);
                break;
            case LEU:
                bgeu(rs2, rs1, offset, position);
                break;
            case AL:
                b(offset, position);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    /**
     * Conditional move. dst = src1 if condition else src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param result general purpose register. May not be null or the stackpointer.
     * @param trueValue general purpose register. May not be null or the stackpointer.
     * @param falseValue general purpose register. May not be null or the stackpointer.
     * @param cond any condition flag. May not be null.
     */
    public void cmov(int size, CiRegister result, CiRegister trueValue, CiRegister falseValue, ConditionFlag cond) {
        //TODO implement conditional move. RISC-V does not have conditional moves...
        throw new UnsupportedOperationException("Unimplemented");
    }

    public void save(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = frameToCSA + csl.offsetOf(r);

            if (r.isCpu()) {
                if (NumUtil.isSignedNbit(12, displacement)) {
                    sd(frameRegister, r, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    sd(scratchRegister, r, 0);
                }
            } else {
                if (NumUtil.isSignedNbit(12, displacement)) {
                    fsd(frameRegister, r, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    fsd(scratchRegister, r, 0);
                }
            }
        }
    }

    public void restore(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = csl.offsetOf(r) + frameToCSA;
            if (r.isCpu()) {
                if (NumUtil.isSignedNbit(12, displacement)) {
                    ldru(64, r, frameRegister, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    ldru(64, r, scratchRegister, 0);
                }
            } else if (r.isFpu()) {
                if (NumUtil.isSignedNbit(12, displacement)) {
                    fld(r, frameRegister, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    fld(r, scratchRegister, 0);
                }
            }
        }
    }

    public final void call() {
        int before = codeBuffer.position();
        jal(RISCV64.zero, CALL_TRAMPOLINE1_OFFSET); // Jump to Trampoline 1
        // Trampoline 1
        auipc(scratchRegister1, 0);
        addi(scratchRegister1, scratchRegister1, CALL_BRANCH_OFFSET - CALL_TRAMPOLINE1_OFFSET);
        nop(MOV_32_BIT_CONSTANT_INSTRUCTION_NUMBER); // mov32BitConstant(scratchRegister, 0);
        add(64, scratchRegister, scratchRegister1, scratchRegister);

        jal(RISCV64.zero, CALL_TRAMPOLINE_INSTRUCTIONS * INSTRUCTION_SIZE); // Jump to last branch
        // Trampoline 2
        auipc(scratchRegister1, 0);
        addi(scratchRegister1, scratchRegister1, CALL_BRANCH_OFFSET - CALL_TRAMPOLINE2_OFFSET);
        nop(MOV_32_BIT_CONSTANT_INSTRUCTION_NUMBER); // mov32BitConstant(scratchRegister, 0);
        add(64, scratchRegister, scratchRegister1, scratchRegister);
        int after = codeBuffer.position();
        assert CALL_BRANCH_OFFSET == after - before : after - before;
        jalr(RISCV64.ra, scratchRegister, 0);
        after = codeBuffer.position();
        assert RIP_CALL_INSTRUCTION_SIZE == after - before : after - before;
    }

    public final void ret() {
        pop(64, RISCV64.ra, true);
        ret(RISCV64.ra);
    }

    public final void ret(CiRegister r) {
        jalr(RISCV64.x0, r, 0);
    }

    public void leaq(CiRegister dest, CiAddress addr) {
        if (addr == CiAddress.Placeholder) {
            nop(4);
        } else {
            setUpScratch(addr);
            mov(dest, scratchRegister);
        }
    }

    public void pause() {
        // See https://github.com/riscv/riscv-gnu-toolchain/blob/master/linux-headers/include/asm-generic/unistd.h for the system call numbers
        // http://man7.org/linux/man-pages/man2/syscall.2.html for information about the system call convention
        // http://man7.org/linux/man-pages/man2/sched_yield.2.html for the sched_yield system call
        int sysSchedYield = 124;
        push(64, RISCV64.a0, RISCV64.a1, RISCV64.a7, RISCV64.LATCH_REGISTER, RISCV64.x31, RISCV64.ra);
        mov32BitConstant(RISCV64.a7, sysSchedYield);
        ecall();
        pop(64, true, RISCV64.ra, RISCV64.x31, RISCV64.LATCH_REGISTER, RISCV64.a7, RISCV64.a1, RISCV64.a0);
    }

    public void hlt() {
        //TODO Implement halt
    }

    public final void crashme() {
        mov(scratchRegister, 0);
        ldr(64, scratchRegister, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister));
        insertForeverLoop();
    }

    public void insertForeverLoop() {
        b(0);
    }

    private void ldr(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        switch (srcSize) {
            case 8:
                lb(rd, rs, offset);
                break;
            case 16:
                lh(rd, rs, offset);
                break;
            case 32:
                lw(rd, rs, offset);
                break;
            case 64:
                ld(rd, rs, offset);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    private void ldru(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        switch (srcSize) {
            case 8:
                lbu(rd, rs, offset);
                break;
            case 16:
                lhu(rd, rs, offset);
                break;
            case 32:
                lwu(rd, rs, offset);
                break;
            case 64:
                ld(rd, rs, offset);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void fldr(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        if (srcSize == 32) {
            flw(rd, rs, offset);
        } else if (srcSize == 64) {
            fld(rd, rs, offset);
        }
    }

    public void str(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        switch (srcSize) {
            case 8:
                sb(rd, rs, offset);
                break;
            case 16:
                sh(rd, rs, offset);
                break;
            case 32:
                sw(rd, rs, offset);
                break;
            case 64:
                sd(rd, rs, offset);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void fstr(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        if (srcSize == 32) {
            fsw(rd, rs, offset);
        } else if (srcSize == 64) {
            fsd(rd, rs, offset);
        } else {
            throw new UnsupportedOperationException("Unimplemented");
        }
    }

    private void ldr(int targetSize, int srcSize, CiRegister rt, RISCV64Address a) {
        assert targetSize == 32 || targetSize == 64;
        assert srcSize < targetSize;

        if (targetSize == srcSize) {
            ldru(srcSize, rt, a);
        } else {
            ldr(srcSize, rt, a);
        }
    }

    /**
     * Loads a srcSize value from address into rt sign-extending it.
     *
     * @param srcSize size of memory read in bits. Must be 8, 16 or 32, but may not be equivalent to targetSize.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    private void ldr(int srcSize, CiRegister rt, RISCV64Address address) {
        switch(address.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                ldr(srcSize, rt, address.getBase(), 0);
                break;
            case IMMEDIATE:
                ldr(srcSize, rt, address.getBase(), address.getImmediate());
                break;
            case IMMEDIATE_PRE_INDEXED: {
                addi(address.getBase(), address.getBase(), address.getImmediate());
                ldr(srcSize, address.getBase(), rt, 0);
                break;
            }
            case IMMEDIATE_POST_INDEXED: {
                ldr(srcSize, address.getBase(), rt, 0);
                addi(address.getBase(), address.getBase(), address.getImmediate());
                break;
            }
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    /**
     * Loads a srcSize value from address into rt zero-extending it.
     *
     * @param srcSize size of memory read in bits. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     * */
    public void ldru(int srcSize, CiRegister rt, RISCV64Address address) {
        switch(address.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                ldru(srcSize, rt, address.getBase(), 0);
                break;
            case IMMEDIATE:
                ldru(srcSize, rt, address.getBase(), address.getImmediate());
                break;
            case IMMEDIATE_PRE_INDEXED: {
                addi(address.getBase(), address.getBase(), address.getImmediate());
                ldru(srcSize, address.getBase(), rt, 0);
                break;
            }
            case IMMEDIATE_POST_INDEXED: {
                ldru(srcSize, address.getBase(), rt, 0);
                addi(address.getBase(), address.getBase(), address.getImmediate());
                break;
            }
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void fldr(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                fldr(srcSize, rt, a.getBase(), 0);
                break;
            case IMMEDIATE:
                fldr(srcSize, rt, a.getBase(), a.getImmediate());
                break;
            case IMMEDIATE_PRE_INDEXED: {
                addi(a.getBase(), a.getBase(), a.getImmediate());
                fldr(srcSize, a.getBase(), rt, 0);
                break;
            }
            case IMMEDIATE_POST_INDEXED: {
                fldr(srcSize, a.getBase(), rt, 0);
                addi(a.getBase(), a.getBase(), a.getImmediate());
                break;
            }
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void str(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                str(srcSize, a.getBase(), rt, 0);
                break;
            case IMMEDIATE:
                str(srcSize, a.getBase(), rt, a.getImmediate());
                break;
            case IMMEDIATE_PRE_INDEXED: {
                addi(a.getBase(), a.getBase(), a.getImmediate());
                str(srcSize, a.getBase(), rt, 0);
                break;
            }
            case IMMEDIATE_POST_INDEXED: {
                str(srcSize, a.getBase(), rt, 0);
                addi(a.getBase(), a.getBase(), a.getImmediate());
                break;
            }
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void fstr(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                fstr(srcSize, a.getBase(), rt, 0);
                break;
            case IMMEDIATE:
                fstr(srcSize, a.getBase(), rt, a.getImmediate());
                break;
            case IMMEDIATE_PRE_INDEXED: {
                addi(a.getBase(), a.getBase(), a.getImmediate());
                fstr(srcSize, a.getBase(), rt, 0);
                break;
            }
            case IMMEDIATE_POST_INDEXED: {
                fstr(srcSize, a.getBase(), rt, 0);
                addi(a.getBase(), a.getBase(), a.getImmediate());
                break;
            }
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void load(CiRegister dest, CiAddress addr, CiKind kind) {
        RISCV64Address address = calculateAddress(addr);
        switch (kind) {
            case Byte:
                ldr(64, 8, dest, address);
                break;
            case Boolean:
                ldru(8, dest, address);
                break;
            case Char:
                ldru(16, dest, address);
                break;
            case Short:
                ldr(64, 16, dest, address);
                break;
            case Int:
                ldr(64, 32, dest, address);
                break;
            case Object:
            case Long:
                ldru(64, dest, address);
                break;
            case Float:
                fldr(32, dest, address);
                break;
            case Double:
                fldr(64, dest, address);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void store(CiRegister src, CiAddress addr, CiKind kind) {
        RISCV64Address address = calculateAddress(addr);
        switch (kind) {
            case Boolean:
            case Byte:
                str(8, src, address);
                break;
            case Char:
            case Short:
                str(16, src, address);
                break;
            case Int:
                str(32, src, address);
                break;
            case Object:
            case Long:
                str(64, src, address);
                break;
            case Float:
                fstr(32, src, address);
                break;
            case Double:
                fstr(64, src, address);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public RISCV64Address getAddressInFrame(CiRegister frameRegister, int displacement) {
        if (NumUtil.isSignedNbit(12, displacement)) {
            return RISCV64Address.createImmediateAddress(frameRegister, displacement);
        } else {
            // Use scratch register to hold frame base + offset
            mov32BitConstant(scratchRegister1, displacement);
            add(scratchRegister1, frameRegister, scratchRegister1);
            return RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister1);
        }
    }

    private RISCV64Address calculateAddress(CiAddress addr) {
        if (addr instanceof RISCV64Address) {
            return (RISCV64Address) addr;
        }

        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        assert addr != CiAddress.Placeholder;
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }

        if (addr.index.isLegal()) {
            mov32BitConstant(scratchRegister, scale.log2);
            sll(scratchRegister, index, scratchRegister);
            add(scratchRegister, base, scratchRegister);
            base = scratchRegister;
        }

        if (disp != 0) {
            if (NumUtil.isSignedNbit(12, disp)) {
                return RISCV64Address.createImmediateAddress(base, disp);
            } else {
                assert base.number != scratchRegister1.number;
                mov32BitConstant(scratchRegister1, disp);
                add(scratchRegister1, base, scratchRegister1);
                return RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister1);
            }
        }

        return RISCV64Address.createBaseRegisterOnlyAddress(base);
    }

    public void setUpScratch(CiAddress addr) {
        setUpRegister(scratchRegister, addr);
    }

    public void setUpRegister(CiRegister dest, CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(4);
            return;
        }

        assert !(base.isValid() && disp == 0 && base.compareTo(RISCV64.LATCH_REGISTER) == 0);
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }

        assert base.isValid();

        if (disp != 0) {
            if (isArithmeticImmediate(disp)) {
                add(64, dest, base, disp);
            } else {
                mov32BitConstant(dest, disp);
                add(dest, dest, base);
            }
            base = dest;
        } else if (!index.isValid()) {
            mov(dest, base);
        }
        if (index.isValid()) {
            slli(dest, index, scale.log2);
            add(dest, base, dest);
        }
    }

    public void nullCheck(CiRegister r) {
        RISCV64Address address = RISCV64Address.createBaseRegisterOnlyAddress(r);
        ldr(64, RISCV64.zr, address);
    }

    /**
     * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
     *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack. For very large
     *            frames, this means that the offset may actually be negative (i.e. denoting a slot "up" the stack above
     *            RSP).
     */
    public void bangStackWithOffset(int offset) {
        sub(64, scratchRegister, RISCV64.sp, offset);
        str(64, RISCV64.a0, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister));
    }

    public final void call(CiRegister src) {
        jalr(RISCV64.ra, src, 0);
    }
}
