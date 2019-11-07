/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
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
package com.oracle.max.asm.target.aarch64;

import static com.oracle.max.asm.target.aarch64.Aarch64Assembler.InstructionType.*;
import static com.oracle.max.asm.target.aarch64.FunctionalUtils.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.aarch64.Aarch64Address.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

public class Aarch64Assembler extends AbstractAssembler {
    private static final int RdOffset = 0;
    private static final int Rs1Offset = 5;
    private static final int Rs2Offset = 16;
    private static final int Rs3Offset = 10;
    private static final int RtOffset = 0;
    private static final int Rt2Offset = 10;
    private static final int RnOffset = 5;
    /**
     * The register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    public final CiRegister frameRegister;
    public final CiRegister scratchRegister;

    /** Native instruction size in number of bytes. */
    public static final int INSTRUCTION_SIZE = 4;

    @Override
    public void patchJumpTarget(int branch, int target) {
        int branchOffset = target - branch;
        PatchLabelKind type = PatchLabelKind.fromEncoding(codeBuffer.getByte(branch));
        switch (type) {
            case BRANCH_CONDITIONALLY:
                assert codeBuffer.getShort(branch + 2) == 0;
                ConditionFlag cf = ConditionFlag.fromEncoding(codeBuffer.getByte(branch + 1));
                b(cf, branchOffset, branch);
                break;
            case TABLE_SWITCH:
            case BRANCH_UNCONDITIONALLY:
                assert codeBuffer.getByte(branch + 1) == 0;
                assert codeBuffer.getShort(branch + 2) == 0;
                b(branchOffset, branch);
                break;
            case BRANCH_NONZERO:
            case BRANCH_ZERO:
                int size = codeBuffer.getByte(branch + 1);
                int regEncoding = codeBuffer.getShort(branch + 2);
                CiRegister reg = Aarch64.cpuRegisters[regEncoding];
                switch (type) {
                    case BRANCH_NONZERO:
                        cbnz(size, reg, branchOffset, branch);
                        break;
                    case BRANCH_ZERO:
                        cbz(size, reg, branchOffset, branch);
                        break;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }

    }

    /**
     * Enumeration of all different instruction kinds: General32/64 are the general instructions
     * (integer, branch, etc.), for 32-, respectively 64-bit operands.
     * FP32/64 is the encoding for the 32/64bit float operations
     */
    public enum InstructionType {
        General32(0x00000000, 32, true),
        General64(0x80000000, 64, true),
        FP32(0x00000000, 32, false),
        FP64(0x00400000, 64, false);

        public final int encoding;
        public final boolean isGeneral;
        public final int width;

        InstructionType(int encoding, int width, boolean isGeneral) {
            this.encoding = encoding;
            this.width = width;
            this.isGeneral = isGeneral;
        }

        public static InstructionType generalFromSize(int size) {
            if (size == General32.width) {
                return General32;
            }
            assert size == General64.width;
            return General64;
        }

        public static InstructionType floatFromSize(int size) {
            if (size == FP32.width) {
                return FP32;
            }
            assert size == FP64.width;
            return FP64;
        }
    }

    private static final int ImmediateOffset = 10;
    private static final int ImmediateRotateOffset = 16;
    private static final int ImmediateSizeOffset = 22;
    private static final int ExtendTypeOffset = 13;

    private static final int AddSubImmOp = 0x11000000;
    // If 1 the immediate is interpreted as being left-shifted by 12 bits.
    private static final int AddSubShiftOffset = 22;
    private static final int AddSubSetFlag = 0x20000000;

    private static final int LogicalImmOp = 0x12000000;

    private static final int MoveWideImmOp = 0x12800000;
    private static final int MoveWideImmOffset = 5;
    private static final int MoveWideShiftOffset = 21;

    private static final int BitfieldImmOp = 0x13000000;

    private static final int AddSubShiftedOp = 0x0B000000;
    private static final int ShiftTypeOffset = 22;

    private static final int AddSubExtendedOp = 0x0B200000;

    private static final int MulOp = 0x1B000000;
    private static final int DataProcessing1SourceOp = 0x5AC00000;
    private static final int DataProcessing2SourceOp = 0x1AC00000;

    private static final int Fp1SourceOp = 0x1E204000;
    private static final int Fp2SourceOp = 0x1E200800;
    private static final int Fp3SourceOp = 0x1F000000;

    private static final int FpConvertOp = 0x1E200000;
    private static final int FpImmOp = 0x1E201000;
    private static final int FpImmOffset = 13;

    private static final int FpCmpOp = 0x1E202000;

    private static final int PcRelImmHiOffset = 5;
    private static final int PcRelImmLoOffset = 29;

    private static final int PcRelImmOp = 0x10000000;

    private static final int UnconditionalBranchImmOp = 0x14000000;
    private static final int UnconditionalBranchRegOp = 0xD6000000;
    private static final int CompareBranchOp = 0x34000000;

    private static final int ConditionalBranchImmOffset = 5;

    private static final int ConditionalSelectOp = 0x1A800000;
    private static final int ConditionalConditionOffset = 12;

    private static final int LoadStoreScaledOp = 0x39000000;
    private static final int LoadStoreUnscaledOp = 0x38000000;
    private static final int LoadStoreRegisterOp = 0x38200800;
    private static final int LoadLiteralOp = 0x18000000;
    private static final int LoadStorePostIndexedOp = 0x38000400;
    private static final int LoadStorePreIndexedOp = 0x38000C00;

    private static final int LoadStoreUnscaledImmOffset = 12;
    private static final int LoadStoreScaledImmOffset = 10;
    private static final int LoadStoreScaledRegOffset = 12;
    private static final int LoadStoreIndexedImmOffset = 12;
    private static final int LoadStoreTransferSizeOffset = 30;
    private static final int LoadStoreFpFlagOffset = 26;
    private static final int LoadLiteralImmeOffset = 5;

    private static final int LogicalShiftOp = 0x0A000000;

    private static final int ExceptionOp = 0xD4000000;
    private static final int SystemImmediateOffset = 5;

    private static final int SimdImmediateOffset = 16;

    private static final int BarrierOp = 0xD503301F;
    private static final int BarrierKindOffset = 8;


    private static final int LoadStorePairOp = 0b101_0 << 26;
    private static final int LoadStorePairPostIndexOp = 0b101_0_001 << 23;
    private static final int LoadStorePairPreIndexOp = 0b101_0_011 << 23;
    private static final int LoadStorePairImm7Offset = 15;

    /**
     * Encoding for all instructions.
     */
    protected enum Instruction {
        BCOND(0x54000000),
        CBNZ(0x01000000),
        CBZ(0x00000000),

        B(0x00000000),
        BL(0x80000000),
        BR(0x001F0000),
        BLR(0x003F0000),
        RET(0x005F0000),

        LDR(0x00000000),
        LDRS(0x00800000),
        LDXR(0x081f7c00),
        LDAR(0x8dffc00),
        LDAXR(0x85ffc00),


        LDP(0b1 << 22),
        STP(0b0 << 22),

        STR(0x00000000),
        STXR(0x08007c00),
        STLR(0x089ffc00),
        STLXR(0x0800fc00),

        ADR(0x00000000),
        ADRP(0x80000000),

        ADD(0x00000000),
        ADDS(ADD.encoding | AddSubSetFlag),
        SUB(0x40000000),
        SUBS(SUB.encoding | AddSubSetFlag),

        NOT(0x00200000),
        AND(0x00000000),
        BIC(AND.encoding | NOT.encoding),
        ORR(0x20000000),
        ORN(ORR.encoding | NOT.encoding),
        EOR(0x40000000),
        EON(EOR.encoding | NOT.encoding),
        ANDS(0x60000000),
        BICS(ANDS.encoding | NOT.encoding),

        ASRV(0x00002800),
        RORV(0x00002C00),
        LSRV(0x00002400),
        LSLV(0x00002000),

        CLS(0x00001400),
        CLZ(0x00001000),
        RBIT(0x00000000),
        REVX(0x00000C00),
        REVW(0x00000800),

        MOVN(0x00000000),
        MOVZ(0x40000000),
        MOVK(0x60000000),

        CSEL(0x00000000),
        CSNEG(0x40000400),
        CSINC(0x00000400),

        BFM(0x20000000),
        SBFM(0x00000000),
        UBFM(0x40000000),
        EXTR(0x13800000),

        MADD(0x00000000),
        MSUB(0x00008000),
        SDIV(0x00000C00),
        UDIV(0x00000800),

        FMOV(0x00000000),
        FMOVCPU2FPU(0x00070000),
        FMOVFPU2CPU(0x00060000),

        FCVTDS(0x00028000),
        FCVTSD(0x00020000),

        FCVTZS(0x00180000),
        SCVTF(0x00020000),

        FABS(0x00008000),
        FSQRT(0x00018000),
        FNEG(0x00010000),

        FRINTZ(0x00058000),

        FADD(0x00002000),
        FSUB(0x00003000),
        FMUL(0x00000000),
        FDIV(0x00001000),
        FMAX(0x00004000),
        FMIN(0x00005000),

        FMADD(0x00000000),
        FMSUB(0x00008000),

        FCMP(0x00000000),
        FCMPZERO(0x00000008),
        FCCMP(0x1E200400),
        FCSEL(0x1E200C00),

        INS(0x4e081c00),
        UMOV(0x4e083c00),

        CNT(0xe205800),
        USRA(0x6f001400),

        HLT(0x00400000),
        BRK(0x00200000),

        CLREX(0xd5033f5f),
        HINT(0xD503201F),
        DMB(0x000000A0),
        DSB(0x00000080),
        ISB(0x000000C0),

        BLR_NATIVE(0xc0000000),

//      MRS:     0b11010101001 << 21
//      MSR_REG: 0b11010101000 << 21
//      MSR_IMM: 0b11010101000000000100000000011111
        MRS(0xd5200000),
        MSR_REG(0xd5000000),
        MSR_IMM(0xd500401f);

        public final int encoding;

        Instruction(int encoding) {
            this.encoding = encoding;
        }

    }

    public enum ShiftType {
        LSL(0), LSR(1), ASR(2), ROR(3);

        public final int encoding;

        ShiftType(int encoding) {
            this.encoding = encoding;
        }
    }

    public enum ExtendType {
        UXTB(0), UXTH(1), UXTW(2), UXTX(3), SXTB(4), SXTH(5), SXTW(6), SXTX(7);

        public final int encoding;

        ExtendType(int encoding) {
            this.encoding = encoding;
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
         * Unsigned Higher or Same | Greater than, equal or unordered.
         */
        HS(0x2),
        /**
         * unsigned lower | less than.
         */
        LO(0x3),
        /**
         * minus (negative) | less than.
         */
        MI(0x4),
        /**
         * plus (positive or zero) | greater than, equal or unordered.
         */
        PL(0x5),
        /**
         * overflow set | unordered.
         */
        VS(0x6),
        /**
         * overflow clear | ordered.
         */
        VC(0x7),
        /**
         * unsigned higher | greater than or unordered.
         */
        HI(0x8),
        /**
         * unsigned lower or same | less than or equal.
         */
        LS(0x9),
        /**
         * signed greater than or equal | greater than or equal.
         */
        GE(0xA),
        /**
         * signed less than | less than or unordered.
         */
        LT(0xB),
        /**
         * signed greater than | greater than.
         */
        GT(0xC),
        /**
         * signed less than or equal | less than, equal or unordered.
         */
        LE(0xD),
        /**
         * always | always.
         */
        AL(0xE),
        /**
         * always | always (identical to AL, just to have valid 0b1111 encoding).
         */
        NV(0xF);

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
                case HS:
                    return LO;
                case LO:
                    return HS;
                case MI:
                    return PL;
                case PL:
                    return MI;
                case VS:
                    return VC;
                case VC:
                    return VS;
                case HI:
                    return LS;
                case LS:
                    return HI;
                case GE:
                    return LT;
                case LT:
                    return GE;
                case GT:
                    return LE;
                case LE:
                    return GT;
                case AL:
                case NV:
                default:
                    throw new Error("should not reach here");
            }
        }
    }

    /**
     * This enum is used to indicate how system registers are encoded.
     * See ARM ARM section C5.3
     */
    public enum SystemRegister {
        // Checkstyle: stop
        NZCV    (0b1101101000010000),
        DAIF    (0b1101101000010001),
        SPSel   (0b1100001000010000),
        SPSR_EL1(0b1100001000000000);
        // Checkstyle: resume

        public final int encoding;

        SystemRegister(int encoding) {
            this.encoding = encoding;
        }
    }

    /**
     * This enum is used to indicate how PState fields are encoded.
     * Each PStateField takes 14 bits.
     * The first 3 and last 3 bits are op1 and op3 respectively.
     * In section C6.6.130:
     *
     * SPSel   when op1 = 000, op2 = 101
     * DAIFSet when op1 = 011, op2 = 110
     * DAIFClr when op1 = 011, op2 = 111
     *
     * Between op1 and op2 there are 8 bits used to represent other parts of the instruction encoding.
     * So these 8 bits are set to zeros here.
     */
    public enum PStateField {
        // Checkstyle: stop
        PSTATEField_SP     (0b00000000000101),
        PSTATEField_DAIFSet(0b01100000000110),
        PSTATEField_DAIFClr(0b01100000000111);
        // Checkstyle: resume

        public final int encoding;

        PStateField(int encoding) {
            this.encoding = encoding;
        }
    }

    /**
     * Constructs an assembler for the AMD64 architecture.
     *
     * @param registerConfig the register configuration used to bind {@link CiRegister#Frame} and
     *            {@link CiRegister#CallerFrame} to physical registers. This value can be null if this assembler
     *            instance will not be used to assemble instructions using these logical registers.
     */
    public Aarch64Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
        this.scratchRegister = registerConfig == null ? Aarch64.r16 : registerConfig.getScratchRegister();
    }

    /* Conditional Branch (5.2.1) */

    /**
     * Branch conditionally.
     *
     * @param condition may not be null.
     * @param imm21 Signed 21-bit offset, has to be word aligned.
     */
    public void b(ConditionFlag condition, int imm21) {
        b(condition, imm21, -1);
    }

    /**
     * Branch conditionally. Inserts instruction into code buffer at pos.
     *
     * @param condition may not be null.
     * @param imm21 Signed 21-bit offset, has to be word aligned.
     * @param pos Position at which instruction is inserted into buffer. -1 means insert at end.
     */
    public void b(ConditionFlag condition, int imm21, int pos) {
        if (pos == -1) {
            codeBuffer.emitInt(Instruction.BCOND.encoding | getConditionalBranchImm(imm21) | condition.encoding);
        } else {
            codeBuffer.emitInt(Instruction.BCOND.encoding | getConditionalBranchImm(imm21) | condition.encoding, pos);
        }
    }

    /**
     * Compare register and branch if non-zero.
     *
     * @param reg general purpose register. May not be null, zero-register or stackpointer.
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param imm21 Signed 21-bit offset, has to be word aligned.
     */
    public void cbnz(int size, CiRegister reg, int imm21) {
        conditionalBranchInstruction(reg, imm21, generalFromSize(size), Instruction.CBNZ, -1);
    }

    /**
     * Compare register and branch if non-zero.
     *
     * @param reg general purpose register. May not be null, zero-register or stackpointer.
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param imm21 Signed 21-bit offset, has to be word aligned.
     * @param pos Position at which instruction is inserted into buffer. -1 means insert at end.
     */
    public void cbnz(int size, CiRegister reg, int imm21, int pos) {
        conditionalBranchInstruction(reg, imm21, generalFromSize(size), Instruction.CBNZ, pos);
    }

    /**
     * Compare and branch if zero.
     *
     * @param reg general purpose register. May not be null, zero-register or stackpointer.
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param imm21 Signed 21-bit offset, has to be word aligned.
     */
    public void cbz(int size, CiRegister reg, int imm21) {
        conditionalBranchInstruction(reg, imm21, generalFromSize(size), Instruction.CBZ, -1);
    }

    /**
     * Compare register and branch if zero.
     *
     * @param reg general purpose register. May not be null, zero-register or stackpointer.
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param imm21 Signed 21-bit offset, has to be word aligned.
     * @param pos Position at which instruction is inserted into buffer. -1 means insert at end.
     */
    public void cbz(int size, CiRegister reg, int imm21, int pos) {
        conditionalBranchInstruction(reg, imm21, generalFromSize(size), Instruction.CBZ, pos);
    }

    public final void alignForPatchableDirectCall() {
        // Aarch64 instructions are 4-byte aligned, there is no need for special alignment
    }

    public void nop(int number) {
        for (int i = 0; i < number; i++) {
            nop();
        }
    }

    private void conditionalBranchInstruction(CiRegister reg, int imm21, InstructionType type, Instruction instr, int pos) {
        assert Aarch64.isGeneralPurposeReg(reg);
        int instrEncoding = instr.encoding | CompareBranchOp;
        if (pos == -1) {
            codeBuffer.emitInt(type.encoding |
                    instrEncoding |
                    getConditionalBranchImm(imm21) |
                    rd(reg));
        } else {
            codeBuffer.emitInt(type.encoding |
                    instrEncoding |
                    getConditionalBranchImm(imm21) |
                    rd(reg), pos);
        }
    }

    private static int getConditionalBranchImm(int imm21) {
        assert NumUtil.isSignedNbit(21, imm21) && (imm21 & 0x3) == 0
                : "Immediate has to be 21bit signed number and word aligned";
        int imm = (imm21 & NumUtil.getNbitNumberInt(21)) >> 2;
        return imm << ConditionalBranchImmOffset;
    }

    /* Unconditional Branch (immediate) (5.2.2) */

    /**
     * @param imm28 Signed 28-bit offset, has to be word aligned.
     */
    public void b(int imm28) {
        unconditionalBranchImmInstruction(imm28, Instruction.B);
    }

    /**
     *
     * @param imm28 Signed 28-bit offset, has to be word aligned.
     * @param pos Position where instruction is inserted into code buffer.
     */
    public void b(int imm28, int pos) {
        unconditionalBranchImmInstruction(imm28, Instruction.B, pos);
    }

    /**
     * Branch and link return address to register X30.
     *
     * @param imm28 Signed 28-bit offset, has to be word aligned.
     */
    public void bl(int imm28) {
        unconditionalBranchImmInstruction(imm28, Instruction.BL);
    }


    /**
     * Return an integer containing the encoding of an unconditional branch to
     * label (address) instruction.
     * @param imm28 -- the branch target displacement
     * @return
     */
    public static int blImmHelper(int imm28) {
        return Instruction.BL.encoding | UnconditionalBranchImmOp | imm28;
    }


    /**
     * Mask for the displacement bits of a branch immediate.
     */
    private static final int B_IMM_ADDRESS_MASK = NumUtil.getNbitNumberInt(26);

    /**
     * Return the displacement of the target of a branch immediate instruction.
     * @param instruction
     * @return
     */
    public static int bImmExtractDisplacement(int instruction) {
        assert (instruction & NumUtil.getNbitNumberInt(5) << 26) == UnconditionalBranchImmOp :
                "Not a branch immediate instruction: 0x" + Integer.toHexString(instruction);
        int displacement = B_IMM_ADDRESS_MASK & instruction;

        // check the sign bit
        if (((1 << 25) & displacement) == 0) {
            return displacement << 2;
        }
        // negative number -- sign extend.
        return (displacement << 2) | 0xF0000000;
    }

    /**
     * Checks if the given branch instruction is a linked branch or not.
     *
     * @param instruction the machine code of the original instruction
     * @return {@code true} if the instruction is a linked branch
     */
    public static boolean isBranchInstructionLinked(int instruction) {
        assert isBranchInstruction(instruction) : Integer.toHexString(instruction);
        if (isBimmInstruction(instruction)) {
            return instruction >> 31 != 0;
        } else {
            return (instruction >> 21 & NumUtil.getNbitNumberInt(2)) == 1;
        }
    }

    /**
     * Checks if the given branch instruction is a branch immediate or branch register.
     *
     * @param instruction the machine code of the original instruction
     * @return
     */
    public static boolean isBranchInstruction(int instruction) {
        final int immOp = instruction & (NumUtil.getNbitNumberInt(5) << 26);
        final int regOp = instruction & (NumUtil.getNbitNumberInt(7) << 25);
        return immOp == UnconditionalBranchImmOp || regOp == UnconditionalBranchRegOp;
    }

    /**
     * Checks if the given branch instruction is a branch register.
     *
     * @param instruction the machine code of the original instruction
     * @return
     */
    public static boolean isBranchRegInstruction(int instruction) {
        return (instruction & (NumUtil.getNbitNumberInt(7) << 25)) == UnconditionalBranchRegOp;
    }

    /**
     * Checks if the given branch instruction is a branch immediate.
     *
     * @param instruction the machine code of the original instruction
     * @return
     */
    public static boolean isBimmInstruction(int instruction) {
        return (instruction & (NumUtil.getNbitNumberInt(5) << 26)) == UnconditionalBranchImmOp;
    }

    /**
     * Load Pair of Registers calculates an address from a base register value and an immediate
     * offset, and stores two 32-bit words or two 64-bit doublewords to the calculated address, from
     * two registers.
     */
    public void ldp(int size, CiRegister rt, CiRegister rt2, Aarch64Address address) {
        assert size == 32 || size == 64;
        loadStorePairInstruction(Instruction.LDP, rt, rt2, address, generalFromSize(size));
    }

    /**
     * Store Pair of Registers calculates an address from a base register value and an immediate
     * offset, and stores two 32-bit words or two 64-bit doublewords to the calculated address, from
     * two registers.
     */
    public void stp(int size, CiRegister rt, CiRegister rt2, Aarch64Address address) {
        assert size == 32 || size == 64;
        loadStorePairInstruction(Instruction.STP, rt, rt2, address, generalFromSize(size));
    }

    private void loadStorePairInstruction(Instruction instr, CiRegister rt, CiRegister rt2, Aarch64Address address, InstructionType type) {
        int scaledOffset = NumUtil.getNbitNumberInt(7) & address.getImmediateRaw();  // LDP/STP use a 7-bit scaled
                                                                     // offset
        int memop = type.encoding | instr.encoding | scaledOffset << LoadStorePairImm7Offset | rt2(rt2) | rn(address.getBase()) | rt(rt);
        switch (address.getAddressingMode()) {
            case IMMEDIATE_SCALED:
                emitInt(memop | LoadStorePairOp | (0b010 << 23));
                break;
            case IMMEDIATE_POST_INDEXED:
                emitInt(memop | LoadStorePairOp | (0b001 << 23));
                break;
            case IMMEDIATE_PRE_INDEXED:
                emitInt(memop | LoadStorePairOp | (0b011 << 23));
                break;
            default:
                throw new Error("Unhandled addressing mode: " + address.getAddressingMode());
        }
    }

    /**
     * Write an integer in little endian order into the byte array at the specified offset.
     * @param instruction
     * @param buffer
     * @param offset
     */
    static void writeInt(int instruction, byte[] buffer, int offset) {
        assert buffer.length >= offset + 3 : "Buffer too small";
        buffer[offset + 0] = (byte) (instruction       & 0xFF);
        buffer[offset + 1] = (byte) (instruction >> 8  & 0xFF);
        buffer[offset + 2] = (byte) (instruction >> 16 & 0xFF);
        buffer[offset + 3] = (byte) (instruction >> 24 & 0xFF);
    }

    /**
     * Patch the address part of a branch immediate instruction. Returns the
     * patched instruction.
     * @param instruction -- the instruction to be patched
     * @param displacement -- the targets displacement
     * @return
     */
    public static int bImmPatch(int instruction, int displacement) {
        assert (instruction & NumUtil.getNbitNumberInt(5) << 26) == UnconditionalBranchImmOp :
                "Not a branch immediate instruction: 0x" + Integer.toHexString(instruction);
        assert NumUtil.isSignedNbit(28, displacement) && (displacement & 0x3) == 0
                        : "Immediate has to be 28bit signed number and word aligned: " + Integer.toHexString(displacement);
        return (instruction & ~B_IMM_ADDRESS_MASK) | ((displacement >> 2) & B_IMM_ADDRESS_MASK);
    }

    private void unconditionalBranchImmInstruction(int imm28, Instruction instr) {
        unconditionalBranchImmInstruction(imm28, instr, -1);
    }

    public static int unconditionalBranchImmInstructionHelper(int imm28, boolean linked) {
        return unconditionalBranchImmInstructionHelper(imm28, linked ? Instruction.BL : Instruction.B);
    }

    private static int unconditionalBranchImmInstructionHelper(int imm28, Instruction instr) {
        assert NumUtil.isSignedNbit(28, imm28) && (imm28 & 0x3) == 0
                : "Immediate has to be 28bit signed number and word aligned";
        int imm = (imm28 & NumUtil.getNbitNumberInt(28)) >> 2;
        return instr.encoding | UnconditionalBranchImmOp | imm;
    }

    private void unconditionalBranchImmInstruction(int imm28, Instruction instr, int pos) {
        int instruction = unconditionalBranchImmInstructionHelper(imm28, instr);
        if (pos == -1) {
            codeBuffer.emitInt(instruction);
        } else {
            codeBuffer.emitInt(instruction, pos);
        }
    }

    /* Unconditional Branch (register) (5.2.3) */

    /**
     * Branches to address in register and writes return address into register X30.
     *
     * @param target general purpose register. May not be null, zero-register or stackpointer.
     */
    public void blr(CiRegister target) {
        unconditionalBranchRegInstruction(target, Instruction.BLR);
    }

    /**
     * Branches to address in register.
     *
     * @param target general purpose register. May not be null, zero-register or stackpointer.
     */
    public void br(CiRegister target) {
        unconditionalBranchRegInstruction(target, Instruction.BR);
    }

    /**
     * Return to address in register.
     *
     * @param target general purpose register. May not be null, zero-register or stackpointer.
     */
    public void ret(CiRegister target) {
        unconditionalBranchRegInstruction(target, Instruction.RET);
    }

    public static int unconditionalBranchRegInstructionHelper(CiRegister target, boolean linked) {
        return unconditionalBranchRegInstructionHelper(target, linked ? Instruction.BLR : Instruction.BR);
    }

    private static int unconditionalBranchRegInstructionHelper(CiRegister target, Instruction instr) {
        assert Aarch64.isGeneralPurposeReg(target);
        return instr.encoding | UnconditionalBranchRegOp | rs1(target);
    }

    private void unconditionalBranchRegInstruction(CiRegister target, Instruction instr) {
        emitInt(unconditionalBranchRegInstructionHelper(target, instr));
    }

    /* Load-Store Single Register (5.3.1) */

    /**
     * Loads a srcSize value from address into rt zero-extending it.
     *
     * @param srcSize size of memory read in bits. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     * */
    public void ldr(int srcSize, CiRegister rt, Aarch64Address address) {
        assert Aarch64.isGeneralPurposeOrZeroReg(rt);
        assert srcSize == 8 || srcSize == 16 || srcSize == 32 || srcSize == 64;
        int transferSize = NumUtil.log2Ceil(srcSize / 8);
        loadStoreInstruction(rt, address, InstructionType.General32, Instruction.LDR, transferSize);
    }

    /**
     * Loads a srcSize value from address into rt sign-extending it.
     *
     * @param targetSize size of target register in bits. Must be 32 or 64.
     * @param srcSize size of memory read in bits. Must be 8, 16 or 32, but may not be equivalent to targetSize.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    public void ldrs(int targetSize, int srcSize, CiRegister rt, Aarch64Address address) {
        assert Aarch64.isGeneralPurposeOrZeroReg(rt);
        assert (srcSize == 8 || srcSize == 16 || srcSize == 32) && srcSize != targetSize;
        int transferSize = NumUtil.log2Ceil(srcSize / 8);
        loadStoreInstruction(rt, address, generalFromSize(targetSize), Instruction.LDRS, transferSize);
    }

// Might be same with ldrs from above
// public void ldrshw(CiRegister dest, Aarch64Address address) {
// if (address.getAddressingMode() == address.getAddressingMode().IMMEDIATE_UNSCALED) {
// int instruction = 0x2e6 << 22;
// instruction |= dest.getEncoding();
// instruction |= address.base().getEncoding() << 5;
// instruction |= address.getImmediate() << 10;
// System.out.println("instruction is " + instruction);
// emitInt(instruction);
// } else {
// throw new Error("unimplemented");
// }
// }

    /**
     * Stores register rt into memory pointed by address.
     *
     * @param destSize number of bits written to memory. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    public void str(int destSize, CiRegister rt, Aarch64Address address) {
        //assert Aarch64.isGeneralPurposeOrSpReg(rt);
        assert destSize == 8 || destSize == 16 || destSize == 32 || destSize == 64;
        int transferSize = NumUtil.log2Ceil(destSize / 8);
        loadStoreInstruction(rt, address, InstructionType.General64, Instruction.STR, transferSize);
    }

    public static final int loadStoreInstructionHelper(CiRegister reg, Aarch64Address address, InstructionType type,
                                      Instruction instr, int log2TransferSize) {
        assert log2TransferSize >= 0 && log2TransferSize < 4;
        assert !Aarch64.isSp(reg);
        int transferSizeEncoding = log2TransferSize << LoadStoreTransferSizeOffset;
        int is32Bit = type.width == 32 ? 1 << ImmediateSizeOffset : 0;
        int isFloat = !type.isGeneral ? 1 << LoadStoreFpFlagOffset : 0;
        int memop = instr.encoding |
                transferSizeEncoding |
                is32Bit |
                isFloat |
                rt(reg);
        switch (address.getAddressingMode()) {
            case IMMEDIATE_SCALED:
                return memop |
                        LoadStoreScaledOp |
                        address.getImmediate() << LoadStoreScaledImmOffset |
                        rs1(address.getBase());
            case IMMEDIATE_UNSCALED:
                return memop |
                        LoadStoreUnscaledOp |
                        address.getImmediate() << LoadStoreUnscaledImmOffset |
                        rs1(address.getBase());
            case BASE_REGISTER_ONLY:
                return memop | LoadStoreScaledOp | rs1(address.getBase());
            case EXTENDED_REGISTER_OFFSET:
            case REGISTER_OFFSET:
                ExtendType extendType = address.getAddressingMode() == AddressingMode.EXTENDED_REGISTER_OFFSET ?
                        address.getExtendType() : ExtendType.UXTX;
                boolean shouldScale = address.isScaled() && log2TransferSize != 0;
                return memop |
                        LoadStoreRegisterOp |
                        rs2(address.getOffset()) |
                        extendType.encoding << ExtendTypeOffset |
                        (shouldScale ? 1 : 0) << LoadStoreScaledRegOffset |
                        rs1(address.getBase());
            case PC_LITERAL:
                assert log2TransferSize >= 2 : "PC literal loads only works for load/stores of 32-bit and larger";
                transferSizeEncoding = (log2TransferSize - 2) << LoadStoreTransferSizeOffset;
                return transferSizeEncoding |
                        isFloat |
                        LoadLiteralOp |
                        rd(reg) |
                        address.getImmediate() << LoadLiteralImmeOffset;
            case IMMEDIATE_POST_INDEXED:
                return memop |
                        LoadStorePostIndexedOp |
                        rs1(address.getBase()) |
                        address.getImmediate() << LoadStoreIndexedImmOffset;
            case IMMEDIATE_PRE_INDEXED:
                return memop |
                        LoadStorePreIndexedOp |
                        rs1(address.getBase()) |
                        address.getImmediate() << LoadStoreIndexedImmOffset;
            default:
                throw new Error("should not reach here");
        }
    }

    private void loadStoreInstruction(CiRegister reg, Aarch64Address address, InstructionType type,
                                      Instruction instr, int log2TransferSize) {
        emitInt(loadStoreInstructionHelper(reg, address, type, instr, log2TransferSize));
    }

    /* Load-Store Exclusive (5.3.6) */

    /**
     * Load address exclusive. Natural alignment of address is required.
     *
     * @param size size of memory read in bits. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address Has to be {@link Aarch64Address.AddressingMode#BASE_REGISTER_ONLY BASE_REGISTER_ONLY}.
     *                May not be null.
     */
    public void ldxr(int size, CiRegister rt, Aarch64Address address) {
        assert size == 8 || size == 16 || size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        exclusiveLoadInstruction(rt, address, transferSize, Instruction.LDXR);
    }

    /**
     * Store address exclusive. Natural alignment of address is required. rs and rt may not point to the same register.
     *
     * @param size size of bits written to memory. Must be 8, 16, 32 or 64.
     * @param rs general purpose register. Set to exclusive access status. 0 means success, everything else failure.
     *           May not be null, or stackpointer.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address Has to be {@link Aarch64Address.AddressingMode#BASE_REGISTER_ONLY BASE_REGISTER_ONLY}.
     *                May not be null.
     */
    public void stxr(int size, CiRegister rs, CiRegister rt, Aarch64Address address) {
        assert size == 8 || size == 16 || size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        exclusiveStoreInstruction(rs, rt, address, transferSize, Instruction.STXR);
    }

    /* Load-Acquire/Store-Release (5.3.7) */

    /* non exclusive access */
    /**
     * Load acquire. Natural alignment of address is required.
     *
     * @param size size of memory read in bits. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address Has to be {@link Aarch64Address.AddressingMode#BASE_REGISTER_ONLY BASE_REGISTER_ONLY}.
     *                May not be null.
     */
    public void ldar(int size, CiRegister rt, Aarch64Address address) {
        assert size == 8 || size == 16 || size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        exclusiveLoadInstruction(rt, address, transferSize, Instruction.LDAR);
    }

    /**
     * Store-release. Natural alignment of address is required.
     *
     * @param size size of bits written to memory. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address Has to be {@link Aarch64Address.AddressingMode#BASE_REGISTER_ONLY BASE_REGISTER_ONLY}.
     *                May not be null.
     */
    public void stlr(int size, CiRegister rt, Aarch64Address address) {
        assert size == 8 || size == 16 || size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        // Hack: Passing the zero-register means it is ignored when building the encoding.
        exclusiveStoreInstruction(Aarch64.r0, rt, address, transferSize, Instruction.STLR);
    }

    /* exclusive access */
    /**
     * Load acquire exclusive. Natural alignment of address is required.
     *
     * @param size size of memory read in bits. Must be 8, 16, 32 or 64.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address Has to be {@link Aarch64Address.AddressingMode#BASE_REGISTER_ONLY BASE_REGISTER_ONLY}.
     *                May not be null.
     */
    public void ldaxr(int size, CiRegister rt, Aarch64Address address) {
        assert size == 8 || size == 16 || size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        exclusiveLoadInstruction(rt, address, transferSize, Instruction.LDAXR);
    }

    /**
     * Store-release exclusive. Natural alignment of address is required. rs and rt may not point to the same register.
     *
     * @param size size of bits written to memory. Must be 8, 16, 32 or 64.
     * @param rs general purpose register. Set to exclusive access status. 0 means success, everything else failure. May not be null, or stackpointer.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address Has to be {@link Aarch64Address.AddressingMode#BASE_REGISTER_ONLY BASE_REGISTER_ONLY}.
     *                May not be null.
     */
    public void stlxr(int size, CiRegister rs, CiRegister rt, Aarch64Address address) {
        assert size == 8 || size == 16 || size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        exclusiveStoreInstruction(rs, rt, address, transferSize, Instruction.STLXR);
    }

    private void exclusiveLoadInstruction(CiRegister reg, Aarch64Address address,
                                          int log2TransferSize, Instruction instr) {
        assert address.getAddressingMode() == AddressingMode.BASE_REGISTER_ONLY;
        assert log2TransferSize >= 0 && log2TransferSize < 4;
        assert Aarch64.isGeneralPurposeOrZeroReg(reg);
        int transferSizeEncoding = log2TransferSize << LoadStoreTransferSizeOffset;
        int instrEncoding = instr.encoding;
        emitInt(transferSizeEncoding |
                instrEncoding |
                1 << ImmediateSizeOffset |
                rt(reg) |
                rs1(address.getBase()));
    }

    /**
     * Stores data from rt into address and sets rs to the returned exclusive access status.
     *
     * @param rs               general purpose register into which the exclusive access status is written. May not be null.
     * @param rt               general purpose register containing data to be written to memory at address. May not be null
     * @param address          Address in base register without offset form specifying where rt is written
     *                         to.
     * @param log2TransferSize log2Ceil of memory transfer size.
     */
    private void exclusiveStoreInstruction(CiRegister rs, CiRegister rt,
                                           Aarch64Address address, int log2TransferSize,
                                           Instruction instr) {
        assert address.getAddressingMode() == AddressingMode.BASE_REGISTER_ONLY;
        assert log2TransferSize >= 0 && log2TransferSize < 4;
        assert Aarch64.isGeneralPurposeOrZeroReg(rt) && Aarch64.isGeneralPurposeOrZeroReg(rs) && !rs.equals(rt);
        int transferSizeEncoding = log2TransferSize << LoadStoreTransferSizeOffset;
        int instrEncoding = instr.encoding;
        emitInt(transferSizeEncoding | instrEncoding | rs2(rs) | rt(rt) |
                rs1(address.getBase()));
    }

    /* PC-relative Address Calculation (5.4.4) */

    /**
     * Address of page: sign extends 21-bit offset, shifts if left by 12 and adds it to the value of the PC with
     * its bottom 12-bits cleared, writing the result to dst.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm21 the address whose relative page we want.
     */
    public void adrp(CiRegister dst, int imm21) {
        addressCalculationInstruction(dst, imm21, Instruction.ADRP);
    }

    /**
     * Helper function for encoding an adr instruction. 
     * @param dst
     * @param imm21
     * @return
     */
    public static int adrHelper(CiRegister dst, int imm21) {
        return Instruction.ADR.encoding | PcRelImmOp | rd(dst) | getPcRelativeImmEncoding(imm21);
    }

    /**
     * Adds a 21-bit signed offset to the program counter and writes the result to dst.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm21 Signed 21-bit offset.
     */
    public void adr(CiRegister dst, int imm21) {
        emitInt(adrHelper(dst, imm21));
    }

    /**
     * Adds a 21-bit signed offset to the program counter and writes the result to dst inserting the
     * instruction into the code buffer at pos.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm21 Signed 21-bit offset.
     * @param pos the position at which to insert the instruction.
     */
    public void adr(CiRegister dst, int imm21, int pos) {
        codeBuffer.emitInt(adrHelper(dst, imm21), pos);
    }

    private void addressCalculationInstruction(CiRegister dst, int imm21, Instruction instr) {
        assert Aarch64.isGeneralPurposeReg(dst);
        int instrEncoding = instr.encoding | PcRelImmOp;
        emitInt(instrEncoding |
                rd(dst) |
                getPcRelativeImmEncoding(imm21));
    }

    private static int getPcRelativeImmEncoding(int imm21) {
        assert NumUtil.isSignedNbit(21, imm21);
        int imm = imm21 & NumUtil.getNbitNumberInt(21);
        // higher 19 bit
        int immHi = (imm >> 2) << PcRelImmHiOffset;
        // lower 2 bit
        int immLo = (imm & 0x3) << PcRelImmLoOffset;
        return immHi | immLo;
    }

    /* Arithmetic (Immediate) (5.4.1) */

    /**
     * dst = src + aimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or zero-register.
     * @param aimm arithmetic immediate. Either unsigned 12-bit value or unsigned 24-bit value with the
     *             lower 12-bit cleared.
     */
    public void add(int size, CiRegister dst, CiRegister src, int aimm) {
        assert all(IS_GENERAL_PURPOSE_OR_SP_REG, dst, src);
        addSubImmInstruction(dst, src, aimm, generalFromSize(size), Instruction.ADD);
    }

    /**
     * dst = src + aimm and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or zero-register.
     * @param aimm arithmetic immediate. Either unsigned 12-bit value or unsigned 24-bit value with the
     *             lower 12-bit cleared.
     */
    public void adds(int size, CiRegister dst, CiRegister src, int aimm) {
        assert Aarch64.isGeneralPurposeOrZeroReg(dst) && Aarch64.isGeneralPurposeOrSpReg(src);
        addSubImmInstruction(dst, src, aimm, generalFromSize(size), Instruction.ADDS);
    }

    /**
     * dst = src - aimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or zero-register.
     * @param aimm arithmetic immediate. Either unsigned 12-bit value or unsigned 24-bit value with the
     *             lower 12-bit cleared.
     */
    public void sub(int size, CiRegister dst, CiRegister src, int aimm) {
        assert all(IS_GENERAL_PURPOSE_OR_SP_REG, dst, src);
        addSubImmInstruction(dst, src, aimm, generalFromSize(size), Instruction.SUB);
    }

    /**
     * dst = src - aimm and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or zero-register.
     * @param aimm arithmetic immediate. Either unsigned 12-bit value or unsigned 24-bit value with the
     *             lower 12-bit cleared.
     */
    public void subs(int size, CiRegister dst, CiRegister src, int aimm) {
        assert Aarch64.isGeneralPurposeOrZeroReg(dst) && Aarch64.isGeneralPurposeOrSpReg(src);
        addSubImmInstruction(dst, src, aimm, generalFromSize(size), Instruction.SUBS);
    }

    private void addSubImmInstruction(CiRegister dst, CiRegister src, int aimm,
                                      InstructionType type, Instruction instr) {
        int instrEncoding = instr.encoding | AddSubImmOp;
        emitInt(type.encoding |
                instrEncoding |
                encodeAimm(aimm) |
                rd(dst) |
                rs1(src));
    }

    /**
     * Encodes arithmetic immediate.
     *
     * @param imm Immediate has to be either an unsigned 12bit value or un unsigned 24bit value with
     *            the lower 12 bits 0.
     * @return Representation of immediate for use with arithmetic instructions.
     */
    private static int encodeAimm(int imm) {
        assert isAimm(imm) : "Immediate has to be legal arithmetic immediate value " + imm;
        if (NumUtil.isUnsignedNbit(12, imm)) {
            return imm << ImmediateOffset;
        } else {
            // First 12 bit are 0, so shift immediate 12 bit and set flag to indicate
            // shifted immediate value.
            return (imm >>> 12 << ImmediateOffset) | (1 << AddSubShiftOffset);
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


    /* Logical (immediate) (5.4.2) */

   /*
    * The logical immediate instructions accept a bitmask immediate bimm32 or bimm64.
    * Such an immediate consists EITHER of a single consecutive sequence with at
    * least one non-zero bit, and at least one zero bit, within an element of 2, 4, 8, 16, 32 or 64 bits;
    * the element then being replicated across the register width, or the bitwise inverse of such a value.
    * The immediate values of all-zero and all-ones may not be encoded as a bitmask immediate,
    * so an assembler must either generate an error for a logical instruction with such an immediate, or a
    * programmer-friendly assembler may transform it into some other instruction which achieves the intended result.
    */

    /**
     * dst = src & bimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate.
     *
     */
    public void and(int size, CiRegister dst, CiRegister src, long bimm) {
        assert Aarch64.isGeneralPurposeOrSpReg(dst) && Aarch64.isGeneralPurposeOrZeroReg(src);
        logicalImmInstruction(dst, src, bimm, generalFromSize(size), Instruction.AND);
    }

    /**
     * dst = src & bimm and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stack-pointer.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate.
     */
    public void ands(int size, CiRegister dst, CiRegister src, long bimm) {
        assert Aarch64.isGeneralPurposeOrZeroReg(dst) && Aarch64.isGeneralPurposeOrZeroReg(src);
        logicalImmInstruction(dst, src, bimm, generalFromSize(size), Instruction.ANDS);
    }

    /**
     * dst = src ^ bimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate.
     */
    public void eor(int size, CiRegister dst, CiRegister src, long bimm) {
        assert Aarch64.isGeneralPurposeOrSpReg(dst) && Aarch64.isGeneralPurposeOrZeroReg(src);
        logicalImmInstruction(dst, src, bimm, generalFromSize(size), Instruction.EOR);
    }

    /**
     * dst = src | bimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate.
     */
    public void orr(int size, CiRegister dst, CiRegister src, long bimm) {
        assert Aarch64.isGeneralPurposeOrSpReg(dst) && Aarch64.isGeneralPurposeOrZeroReg(src);
        logicalImmInstruction(dst, src, bimm, generalFromSize(size), Instruction.ORR);
    }

    public void logicalImmInstruction(CiRegister dst, CiRegister src, long bimm,
                                       InstructionType type, Instruction instr) {
        // Mask higher bits off, since we always pass longs around even for the 32-bit instruction.
        if (type == InstructionType.General32) {
            assert (bimm >> 32) == 0 || (bimm >> 32) == -1L :
                    "Higher order bits for 32-bit instruction must either all be 0 or 1.";
            bimm &= NumUtil.getNbitNumberLong(32);
        }
        int immEncoding = Aarch64LogicalImmediateTable.getLogicalImmEncoding(type == InstructionType.General64, bimm);
        int instrEncoding = instr.encoding | LogicalImmOp;
        emitInt(type.encoding |
                instrEncoding |
                immEncoding |
                rd(dst) |
                rs1(src));
    }

    /**
     * Unoptimised (4 instruction) move of a 64bit constant into register.
     * @param reg
     * @param imm64
     */
    public void mov64BitConstant(CiRegister reg, long imm64) {
        movz(64, reg, (int) imm64 & 0xFFFF, 0);
        movk(64, reg, (int) (imm64 >> 16) & 0xFFFF, 16);
        movk(64, reg, (int) (imm64 >> 32) & 0xFFFF, 32);
        movk(64, reg, (int) (imm64 >> 48) & 0xFFFF, 48);
    }

    /**
     * Unoptimised (2 instruction) move of a 32bit constant into register.
     * @param reg
     * @param imm32
     */
    public void mov32BitConstant(CiRegister reg, long imm32) {
        movz(64, reg, (int) imm32 & 0xFFFF, 0);
        movk(64, reg, (int) (imm32 >> 16) & 0xFFFF, 16);
    }

    /* Move (wide immediate) (5.4.3) */

    /**
     * Extracts the 16bit immediate from a movz/movk instruction.
     * @param instruction
     * @return
     */
    public static short movExtractImmediate(int instruction) {
        final int op = instruction & (NumUtil.getNbitNumberInt(6) << 23);
        assert op == MoveWideImmOp : instruction;
        final int opc = instruction & (NumUtil.getNbitNumberInt(2) << 29);
        assert (opc == Instruction.MOVZ.encoding) || (opc == Instruction.MOVK.encoding) : instruction;
        return (short) ((instruction >> MoveWideImmOffset) & NumUtil.getNbitNumberInt(16));
    }

    public static boolean isMovz(int instruction) {
        final int op = instruction & (NumUtil.getNbitNumberInt(6) << 23);
        final int opc = instruction & (NumUtil.getNbitNumberInt(2) << 29);
        return op == MoveWideImmOp && opc == Instruction.MOVZ.encoding;
    }

    public static boolean isMovk(int instruction) {
        final int op = instruction & (NumUtil.getNbitNumberInt(6) << 23);
        final int opc = instruction & (NumUtil.getNbitNumberInt(2) << 29);
        return op == MoveWideImmOp && opc == Instruction.MOVK.encoding;
    }

    /**
     * dst = uimm16 << shiftAmt.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param uimm16 16-bit unsigned immediate
     * @param shiftAmt amount by which uimm16 is left shifted. Can be any multiple of 16 smaller than size.
     */
    public void movz(int size, CiRegister dst, int uimm16, int shiftAmt) {
        moveWideImmInstruction(dst, uimm16, shiftAmt, generalFromSize(size), Instruction.MOVZ);
    }

    public static int movzHelper(int size, CiRegister dst, int uimm16, int shiftAmt) {
        return moveWideImmInstructionHelper(dst, uimm16, shiftAmt, generalFromSize(size), Instruction.MOVZ);
    }

    /**
     * dst = ~(uimm16 << shiftAmt).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param uimm16 16-bit unsigned immediate
     * @param shiftAmt amount by which uimm16 is left shifted. Can be any multiple of 16 smaller than size.
     */
    public void movn(int size, CiRegister dst, int uimm16, int shiftAmt) {
        moveWideImmInstruction(dst, uimm16, shiftAmt, generalFromSize(size), Instruction.MOVN);
    }

    /**
     * dst<pos+15:pos> = uimm16.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param uimm16 16-bit unsigned immediate
     * @param pos position into which uimm16 is inserted. Can be any multiple of 16 smaller than size.
     */
    public void movk(int size, CiRegister dst, int uimm16, int pos) {
        // TODO unit test
        moveWideImmInstruction(dst, uimm16, pos, generalFromSize(size), Instruction.MOVK);
    }

    public static int movkHelper(int size, CiRegister dst, int uimm16, int pos) {
        return moveWideImmInstructionHelper(dst, uimm16, pos, generalFromSize(size), Instruction.MOVK);
    }

    private void moveWideImmInstruction(CiRegister dst, int uimm16, int shiftAmt,
                                        InstructionType type, Instruction instr) {
        assert Aarch64.isGeneralPurposeReg(dst);
        assert NumUtil.isUnsignedNbit(16, uimm16) : "Immediate has to be unsigned 16bit";
        assert shiftAmt == 0 || shiftAmt == 16 ||
                (type == InstructionType.General64 && (shiftAmt == 32 || shiftAmt == 48)) :
                "Invalid shift amount: " + shiftAmt;
        shiftAmt >>= 4;
        int instrEncoding = instr.encoding | MoveWideImmOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                uimm16 << MoveWideImmOffset |
                shiftAmt << MoveWideShiftOffset);
    }

    private static int moveWideImmInstructionHelper(CiRegister dst, int uimm16, int shiftAmt,
                                        InstructionType type, Instruction instr) {
        assert Aarch64.isGeneralPurposeReg(dst);
        assert NumUtil.isUnsignedNbit(16, uimm16) : "Immediate has to be unsigned 16bit";
        assert shiftAmt == 0 || shiftAmt == 16 ||
                (type == InstructionType.General64 && (shiftAmt == 32 || shiftAmt == 48)) :
                "Invalid shift amount: " + shiftAmt;
        shiftAmt >>= 4;
        int instrEncoding = instr.encoding | MoveWideImmOp;
        return type.encoding |
                instrEncoding |
                rd(dst) |
                uimm16 << MoveWideImmOffset |
                shiftAmt << MoveWideShiftOffset;
    }

    /**
     * Implementation of movror from the ARMv7 assembler.
     *
     * @param rd general purpose register.
     * @param rm general purpose register.
     * @param shiftImm 0-63 constant
     */
    public void movror(final CiRegister rd, final CiRegister rm, final int shiftImm) {
        orr(64, rd, Aarch64.zr, rm, ShiftType.ROR, shiftImm);
    }

    /* Bitfield Operations (5.4.5) */

    /**
     * Bitfield move.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param r must be in the range 0 to size - 1
     * @param s must be in the range 0 to size - 1
     */
    public void bfm(int size, CiRegister dst, CiRegister src, int r, int s) {
        // TODO unit test
        bitfieldInstruction(dst, src, r, s, generalFromSize(size), Instruction.BFM);
    }

    /**
     * Unsigned bitfield move.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param r must be in the range 0 to size - 1
     * @param s must be in the range 0 to size - 1
     */
    public void ubfm(int size, CiRegister dst, CiRegister src, int r, int s) {
        // TODO unit test
        bitfieldInstruction(dst, src, r, s, generalFromSize(size), Instruction.UBFM);
    }

    /**
     * Signed bitfield move.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param r must be in the range 0 to size - 1
     * @param s must be in the range 0 to size - 1
     */
    public void sbfm(int size, CiRegister dst, CiRegister src, int r, int s) {
        // TODO unit test
        bitfieldInstruction(dst, src, r, s, generalFromSize(size), Instruction.SBFM);
    }

    private void bitfieldInstruction(CiRegister dst, CiRegister src, int r, int s, InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_REG, dst, src);
        assert s >= 0 && s < type.width && r >= 0 && r < type.width;
        int instrEncoding = instr.encoding | BitfieldImmOp;
        int sf = type == InstructionType.General64 ? 1 << ImmediateSizeOffset : 0;
        emitInt(type.encoding |
                instrEncoding |
                sf |
                r << ImmediateRotateOffset |
                s << ImmediateOffset |
                rd(dst) |
                rs1(src));
    }

    /* Extract (Immediate) (5.4.6) */

    /**
     * Extract. dst = src1:src2<lsb+31:lsb>
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param lsb must be in range 0 to size - 1.
     */
    public void extr(int size, CiRegister dst, CiRegister src1, CiRegister src2, int lsb) {
        extractInstruction(dst, src1, src2, lsb, generalFromSize(size));
    }

    private void extractInstruction(CiRegister dst, CiRegister src1, CiRegister src2,
                                    int lsb, InstructionType type) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2);
        assert lsb >= 0 && lsb < type.width;
        int sf = type == InstructionType.General64 ? 1 << ImmediateSizeOffset : 0;
        emitInt(type.encoding |
                Instruction.EXTR.encoding |
                sf |
                lsb << ImmediateOffset |
                rd(dst) |
                rs1(src1) |
                rs2(src2));
    }

    /** The number of instructions in a trampoline. */
    public static final int TRAMPOLINE_INSTRUCTIONS = 2;

    /** The size of a trampoline in bytes. */
    public static final int TRAMPOLINE_SIZE = (TRAMPOLINE_INSTRUCTIONS * INSTRUCTION_SIZE) + Long.BYTES;

    /** The offset of the address operand in a trampoline. */
    public static final int TRAMPOLINE_ADDRESS_OFFSET = TRAMPOLINE_INSTRUCTIONS * INSTRUCTION_SIZE;

    /**
     * An address describing the PC relative offset of the trampoline address in the trampoline
     * itself. That is +8 from the load. The trampoline has the format:
     * <code>
     * ldr x16, #8              ; load target address
     * br x16                   ; branch to target
     * 0x0000_0000_0000_0000    ; target address
     * </code>
     */
    private static final Aarch64Address trampolineOffset = Aarch64Address.createAddress(CiKind.Long,
            AddressingMode.PC_LITERAL, Aarch64.zr, Aarch64.zr, TRAMPOLINE_ADDRESS_OFFSET, false, null);

    /**
     * Encode a load instruction for a trampoline.
     * @return
     */
    private int trampolineLdr() {
        return loadStoreInstructionHelper(scratchRegister, trampolineOffset, General64, Instruction.LDR, 3);
    }

    /**
     * Encode a branch instruction for a trampoline.
     * @return
     */
    private int trampolineBr() {
        return unconditionalBranchRegInstructionHelper(scratchRegister, false);
    }

    @Override
    public byte[] trampolines(int count) {
        byte[] trampolines = new byte[count * TRAMPOLINE_SIZE];
        for (int i = 0; i < trampolines.length; i += TRAMPOLINE_SIZE) {
            writeInt(trampolineLdr(), trampolines, i);
            writeInt(trampolineBr(), trampolines, i + INSTRUCTION_SIZE);
        }
        return trampolines;
    }

    /* Arithmetic (shifted register) (5.5.1) */

    /**
     * dst = src1 + shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param imm must be in range 0 to size - 1.
     */
    public void add(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int imm) {
        addSubShiftedInstruction(dst, src1, src2, shiftType, imm, generalFromSize(size), Instruction.ADD);
    }

    /**
     * dst = src1 + shiftType(src2, imm) and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param imm must be in range 0 to size - 1.
     */
    public void adds(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int imm) {
        // TODO how to access cpsr in unit test
        addSubShiftedInstruction(dst, src1, src2, shiftType, imm, generalFromSize(size), Instruction.ADDS);
    }

    /**
     * dst = src1 - shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param imm must be in range 0 to size - 1.
     */
    public void sub(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int imm) {
        addSubShiftedInstruction(dst, src1, src2, shiftType, imm, generalFromSize(size), Instruction.SUB);
    }

    /**
     * dst = src1 - shiftType(src2, imm) and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param imm must be in range 0 to size - 1.
     */
    public void subs(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int imm) {
        // TODO how to access cpsr in unit test
        addSubShiftedInstruction(dst, src1, src2, shiftType, imm, generalFromSize(size), Instruction.SUBS);
    }

    public static boolean isAddInstruction(int instruction) {
        assert isAddSubInstruction(instruction) : instruction;
        return (instruction & (1 << 30)) == Instruction.ADD.encoding;
    }

    public static boolean isAddSubInstruction(int instruction) {
        return (instruction & (NumUtil.getNbitNumberInt(5) << 24)) == AddSubShiftedOp;
    }

    public static int addSubInstructionHelper(CiRegister dst, CiRegister src1, CiRegister src2, boolean isSub) {
        return addSubShiftedInstructionHelper(dst, src1, src2, ShiftType.LSL, 0,
                                              InstructionType.General64, isSub ? Instruction.SUB : Instruction.ADD);
    }

    private static int addSubShiftedInstructionHelper(CiRegister dst, CiRegister src1,
                                                      CiRegister src2, ShiftType shiftType, int imm,
                                                      InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2);
        assert shiftType != ShiftType.ROR;
        assert imm >= 0 && imm < type.width;
        int instrEncoding = instr.encoding | AddSubShiftedOp;
        return type.encoding | instrEncoding | imm << ImmediateOffset | shiftType.encoding << ShiftTypeOffset |
               rd(dst) | rs1(src1) | rs2(src2);
    }

    private void addSubShiftedInstruction(CiRegister dst, CiRegister src1,
                                          CiRegister src2, ShiftType shiftType, int imm,
                                          InstructionType type, Instruction instr) {
        emitInt(addSubShiftedInstructionHelper(dst, src1, src2, shiftType, imm, type, instr));
    }

    /* Arithmetic (extended register) (5.5.2) */
    /**
     * dst = src1 + extendType(src2) << imm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register..
     * @param src1 general purpose register. May not be null or zero-register.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param extendType defines how src2 is extended to the same size as src1.
     * @param shiftAmt must be in range 0 to 4.
     */
    public void add(int size, CiRegister dst, CiRegister src1, CiRegister src2, ExtendType extendType, int shiftAmt) {
        assert all(IS_GENERAL_PURPOSE_OR_SP_REG, dst, src1) && Aarch64.isGeneralPurposeOrZeroReg(src2);
        addSubExtendedInstruction(dst, src1, src2, extendType, shiftAmt, generalFromSize(size), Instruction.ADD);
    }

    /**
     * dst = src1 + extendType(src2) << imm and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer..
     * @param src1 general purpose register. May not be null or zero-register.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param extendType defines how src2 is extended to the same size as src1.
     * @param shiftAmt must be in range 0 to 4.
     */
    public void adds(int size, CiRegister dst, CiRegister src1, CiRegister src2, ExtendType extendType, int shiftAmt) {
        //TODO unit test CPSR
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src2) && Aarch64.isGeneralPurposeOrSpReg(src1);
        addSubExtendedInstruction(dst, src1, src2, extendType, shiftAmt, generalFromSize(size), Instruction.ADDS);
    }

    protected void checkConstraint(boolean passed, String expression) {
        if (!passed) {
            throw new IllegalArgumentException(expression);
        }
    }

    public void addlsl(final CiRegister rd, final CiRegister rn, final CiRegister rm, final int shiftImm) {
        int instruction = 0x8b000000;
        checkConstraint(0 <= shiftImm && shiftImm <= 31, "0 <= shitImm && shitImm <= 31");
        instruction |= rd.getEncoding();
        instruction |= rn.getEncoding() << 5;
        instruction |= shiftImm << 10;
        instruction |= rm.getEncoding() << 16;
        emitInt(instruction);
    }

    /**
     * dst = src1 - extendType(src2) << imm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register..
     * @param src1 general purpose register. May not be null or zero-register.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param extendType defines how src2 is extended to the same size as src1.
     * @param shiftAmt must be in range 0 to 4.
     */
    public void sub(int size, CiRegister dst, CiRegister src1, CiRegister src2, ExtendType extendType, int shiftAmt) {
        assert all(IS_GENERAL_PURPOSE_OR_SP_REG, dst, src1) && Aarch64.isGeneralPurposeOrZeroReg(src2);
        addSubExtendedInstruction(dst, src1, src2, extendType, shiftAmt, generalFromSize(size), Instruction.SUB);
    }

    /**
     * dst = src1 - extendType(src2) << imm and sets flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer..
     * @param src1 general purpose register. May not be null or zero-register.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param extendType defines how src2 is extended to the same size as src1.
     * @param shiftAmt must be in range 0 to 4.
     */
    public void subs(int size, CiRegister dst, CiRegister src1, CiRegister src2, ExtendType extendType, int shiftAmt) {
        //TODO unit test CPSR
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src2) && Aarch64.isGeneralPurposeOrSpReg(src1);
        addSubExtendedInstruction(dst, src1, src2, extendType, shiftAmt, generalFromSize(size), Instruction.SUBS);
    }

    private void addSubExtendedInstruction(CiRegister dst, CiRegister src1, CiRegister src2, ExtendType extendType,
                                           int shiftAmt, InstructionType type, Instruction instr) {
        assert shiftAmt >= 0 && shiftAmt <= 4;
        int instrEncoding = instr.encoding | AddSubExtendedOp;
        emitInt(type.encoding |
                instrEncoding |
                shiftAmt << ImmediateOffset |
                extendType.encoding << ExtendTypeOffset |
                rd(dst) |
                rs1(src1) |
                rs2(src2));
    }

    /* Logical (shifted register) (5.5.3) */
    /**
     * dst = src1 & shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void and(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.AND);
    }

    /**
     * dst = src1 & shiftType(src2, imm) and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void ands(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.ANDS);
    }

    /**
     * dst = src1 & ~(shiftType(src2, imm)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void bic(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.BIC);
    }

    /**
     * dst = src1 & ~(shiftType(src2, imm)) and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void bics(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.BICS);
    }

    /**
     * dst = src1 ^ ~(shiftType(src2, imm)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void eon(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.EON);
    }

    /**
     * dst = src1 ^ shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void eor(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.EOR);
    }

    /**
     * dst = src1 | shiftType(src2, imm).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void orr(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.ORR);
    }

    /**
     * dst = src1 | ~(shiftType(src2, imm)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType all types allowed, may not be null.
     * @param shiftAmt must be in range 0 to size - 1.
     */
    public void orn(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        logicalRegInstruction(dst, src1, src2, shiftType, shiftAmt, generalFromSize(size), Instruction.ORN);
    }

    private void logicalRegInstruction(CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt,
                                       InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2);
        assert shiftAmt >= 0 && shiftAmt < type.width;
        int instrEncoding = instr.encoding | LogicalShiftOp;
        emitInt(type.encoding |
                instrEncoding |
                shiftAmt << ImmediateOffset |
                shiftType.encoding << ShiftTypeOffset |
                rd(dst) |
                rs1(src1) |
                rs2(src2));
    }

    /* Variable Shift (5.5.4) */
    /**
     * dst = src1 >> src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void asr(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        dataProcessing2SourceOp(dst, src1, src2, generalFromSize(size), Instruction.ASRV);
    }

    /**
     * dst = src1 << src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void lsl(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        dataProcessing2SourceOp(dst, src1, src2, generalFromSize(size), Instruction.LSLV);
    }

    /**
     * dst = src1 >>> src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void lsr(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        dataProcessing2SourceOp(dst, src1, src2, generalFromSize(size), Instruction.LSRV);
    }

    /**
     * dst = rotateRight(src1, (src2 & log2(size))).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void ror(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        dataProcessing2SourceOp(dst, src1, src2, generalFromSize(size), Instruction.RORV);
    }

    /* Bit Operations (5.5.5) */

    /**
     * Counts leading sign bits.
     * Sets Wd to the number of consecutive bits following the topmost bit in dst, that are the same as the topmost bit.
     * The count does not include the topmost bit itself , so the result  will be in the range 0 to size-1 inclusive.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, zero-register or the stackpointer.
     * @param src source register. May not be null, zero-register or the stackpointer.
     */
    public void cls(int size, CiRegister dst, CiRegister src) {
        dataProcessing1SourceOp(dst, src, generalFromSize(size), Instruction.CLS);
    }

    /**
     * Counts leading zeros.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, zero-register or the stackpointer.
     * @param src source register. May not be null, zero-register or the stackpointer.
     */
    public void clz(int size, CiRegister dst, CiRegister src) {
        dataProcessing1SourceOp(dst, src, generalFromSize(size), Instruction.CLZ);
    }

    /**
     * Reverses bits.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, zero-register or the stackpointer.
     * @param src source register. May not be null, zero-register or the stackpointer.
     */
    public void rbit(int size, CiRegister dst, CiRegister src) {
        dataProcessing1SourceOp(dst, src, generalFromSize(size), Instruction.RBIT);
    }

    /**
     * Reverses bytes.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src source register. May not be null or the stackpointer.
     */
    public void rev(int size, CiRegister dst, CiRegister src) {
        if (size == 64) {
            dataProcessing1SourceOp(dst, src, generalFromSize(size), Instruction.REVX);
        } else {
            assert size == 32;
            dataProcessing1SourceOp(dst, src, generalFromSize(size), Instruction.REVW);
        }
    }
    /* Conditional Data Processing (5.5.6) */

    /**
     * Conditional select. dst = src1 if condition else src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param condition any condition flag. May not be null.
     */
    public void csel(int size, CiRegister dst, CiRegister src1, CiRegister src2, ConditionFlag condition) {
        conditionalSelectInstruction(dst, src1, src2, condition, generalFromSize(size), Instruction.CSEL);
    }

    /**
     * Conditional select negate. dst = src1 if condition else -src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param condition any condition flag. May not be null.
     */
    public void csneg(int size, CiRegister dst, CiRegister src1, CiRegister src2, ConditionFlag condition) {
        conditionalSelectInstruction(dst, src1, src2, condition, generalFromSize(size), Instruction.CSNEG);
    }

    /**
     * Conditional increase. dst = src1 if condition else src2 + 1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param condition any condition flag. May not be null.
     */
    public void csinc(int size, CiRegister dst, CiRegister src1, CiRegister src2, ConditionFlag condition) {
        conditionalSelectInstruction(dst, src1, src2, condition, generalFromSize(size), Instruction.CSINC);
    }

    private void conditionalSelectInstruction(CiRegister dst, CiRegister src1, CiRegister src2, ConditionFlag condition,
                                              InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2);
        int instrEncoding = instr.encoding | ConditionalSelectOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src1) |
                rs2(src2) |
                condition.encoding << ConditionalConditionOffset);
    }

    /* Integer Multiply/Divide (5.6) */

    /**
     * dst = src1 * src2 + src3.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param src3 general purpose register. May not be null or the stackpointer.
     */
    public void madd(int size, CiRegister dst, CiRegister src1, CiRegister src2, CiRegister src3) {
        mulInstruction(dst, src1, src2, src3, generalFromSize(size), Instruction.MADD);
    }

    /**
     * dst = src3 - src1 * src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     * @param src3 general purpose register. May not be null or the stackpointer.
     */
    public void msub(int size, CiRegister dst, CiRegister src1, CiRegister src2, CiRegister src3) {
        mulInstruction(dst, src1, src2, src3, generalFromSize(size), Instruction.MSUB);
    }

    private void mulInstruction(CiRegister dst, CiRegister src1, CiRegister src2, CiRegister src3,
                                InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2, src3);
        int instrEncoding = instr.encoding | MulOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src1) |
                rs2(src2) |
                rs3(src3));
    }

    /**
     * Signed divide. dst = src1 / src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void sdiv(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        dataProcessing2SourceOp(dst, src1, src2, generalFromSize(size), Instruction.SDIV);
    }

    /**
     * Unsigned divide. dst = src1 / src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void udiv(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        dataProcessing2SourceOp(dst, src1, src2, generalFromSize(size), Instruction.UDIV);
    }

    private void dataProcessing2SourceOp(CiRegister dst, CiRegister src1, CiRegister src2,
                                         InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2);
        int instrEncoding = instr.encoding | DataProcessing2SourceOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src1) |
                rs2(src2));
    }

    private void dataProcessing1SourceOp(CiRegister dst, CiRegister src, InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_REG, dst, src);
        int instrEncoding = instr.encoding | DataProcessing1SourceOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src));
    }

    /* Floating point operations */

    /* Load-Store Single FP register (5.7.1.1) */
    /**
     * Floating point load.
     *
     * @param size number of bits read from memory into rt. Must be 32 or 64.
     * @param rt floating point register. May not be null.
     * @param address all addressing modes allowed. May not be null.
     */
    public void fldr(int size, CiRegister rt, Aarch64Address address) {
        assert Aarch64.isFpuReg(rt);
        assert size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        loadStoreInstruction(rt, address, InstructionType.FP32, Instruction.LDR, transferSize);
    }

    /**
     * Floating point store.
     *
     * @param size number of bits read from memory into rt. Must be 32 or 64.
     * @param rt floating point register. May not be null.
     * @param address all addressing modes allowed. May not be null.
     */
    public void fstr(int size, CiRegister rt, Aarch64Address address) {
        assert Aarch64.isFpuReg(rt);
        assert size == 32 || size == 64;
        int transferSize = NumUtil.log2Ceil(size / 8);
        loadStoreInstruction(rt, address, InstructionType.FP64, Instruction.STR, transferSize);
    }

    /* Floating-point Move (register) (5.7.2) */

    /**
     * Floating point move.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating point register. May not be null.
     * @param src floating point register. May not be null.
     */
    public void fmov(int size, CiRegister dst, CiRegister src) {
        fpDataProcessing1Source(dst, src, floatFromSize(size), Instruction.FMOV);
    }

    /**
     * Move size bits from floating point register unchanged to general purpose register.
     *
     * @param size number of bits read from memory into rt. Must be 32 or 64.
     * @param dst general purpose register. May not be null, stack-pointer or zero-register
     * @param src floating point register. May not be null.
     */
    public void fmovFpu2Cpu(int size, CiRegister dst, CiRegister src) {
        assert Aarch64.isGeneralPurposeReg(dst) && Aarch64.isFpuReg(src);
        fmovCpuFpuInstruction(dst, src, size == 64, Instruction.FMOVFPU2CPU);
    }

    /**
     * Move size bits from general purpose register unchanged to floating point register.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating point register. May not be null.
     * @param src general purpose register. May not be null or stack-pointer.
     */
    public void fmovCpu2Fpu(int size, CiRegister dst, CiRegister src) {
        assert Aarch64.isGeneralPurposeOrZeroReg(src) && Aarch64.isFpuReg(dst);
        fmovCpuFpuInstruction(dst, src, size == 64, Instruction.FMOVCPU2FPU);
    }

    private void fmovCpuFpuInstruction(CiRegister dst, CiRegister src, boolean is64bit, Instruction instr) {
        int instrEncoding = instr.encoding | FpConvertOp;
        int sf = is64bit ? InstructionType.FP64.encoding | InstructionType.General64.encoding
                : InstructionType.FP32.encoding | InstructionType.General32.encoding;
        emitInt(sf |
                instrEncoding |
                rd(dst) |
                rs1(src));
    }

    /* Floating-point Move (immediate) (5.7.3) */

    /**
     * Move immediate into register.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating point register. May not be null.
     * @param imm immediate that is loaded into dst. If size is 32 only float immediates can be loaded, i.e.
     *            (float) imm == imm must be true.
     *            In all cases {@code isFloatImmediate}, respectively {@code #isDoubleImmediate}
     *            must be true depending on size.
     */
    public void fmov(int size, CiRegister dst, double imm) {
        fmovImmInstruction(dst, imm, floatFromSize(size));
    }

    private void fmovImmInstruction(CiRegister dst, double imm, InstructionType type) {
        assert Aarch64.isFpuReg(dst);
        int immEncoding;
        if (type == InstructionType.FP64) {
            immEncoding = getDoubleImmediate(imm);
        } else {
            assert imm == (float) imm : "float mov must use an immediate that can be represented using a float.";
            immEncoding = getFloatImmediate((float) imm);
        }
        int instrEncoding = Instruction.FMOV.encoding | FpImmOp;
        emitInt(type.encoding |
                instrEncoding |
                immEncoding |
                rd(dst));
    }

    private static int getDoubleImmediate(double imm) {
        assert isDoubleImmediate(imm);
        // bits: aBbb.bbbb.bbcd.efgh.0000.0000.0000.0000
        // 0000.0000.0000.0000.0000.0000.0000.0000
        long repr = Double.doubleToRawLongBits(imm);
        int a = (int) (repr >>> 63) << 7;
        int b = (int) ((repr >>> 61) & 0x1) << 6;
        int cToH = (int) (repr >>> 48) & 0x3f;
        return (a | b | cToH) << FpImmOffset;
    }

    public static boolean isDoubleImmediate(double imm) {
        // Valid values will have the form:
        // aBbb.bbbb.bbcd.efgh.0000.0000.0000.0000
        // 0000.0000.0000.0000.0000.0000.0000.0000
        long bits = Double.doubleToRawLongBits(imm);
        // lower 48 bits are cleared
        if ((bits & NumUtil.getNbitNumberLong(48)) != 0) {
            return false;
        }
        // bits[61..54] are all set or all cleared.
        long pattern = (bits >> 54) & NumUtil.getNbitNumberLong(7);
        if (pattern != 0 && pattern != NumUtil.getNbitNumberLong(7)) {
            return false;
        }
        // bits[62] and bits[61] are opposites.
        return ((bits ^ (bits << 1)) & (1L << 62)) != 0;
    }

    private static int getFloatImmediate(float imm) {
        assert isFloatImmediate(imm);
        // bits: aBbb.bbbc.defg.h000.0000.0000.0000.0000
        int repr = Float.floatToRawIntBits(imm);
        int a = (repr >>> 31) << 7;
        int b = ((repr >>> 29) & 0x1) << 6;
        int cToH = (repr >>> 19) & NumUtil.getNbitNumberInt(6);
        return (a | b | cToH) << FpImmOffset;
    }

    public static boolean isFloatImmediate(float imm) {
        // Valid values will have the form:
        // aBbb.bbbc.defg.h000.0000.0000.0000.0000
        int bits = Float.floatToRawIntBits(imm);
        // lower 20 bits are cleared.
        if ((bits & NumUtil.getNbitNumberInt(19)) != 0) {
            return false;
        }
        // bits[29..25] are all set or all cleared
        int pattern = (bits >> 25) & NumUtil.getNbitNumberInt(5);
        if (pattern != 0 && pattern != NumUtil.getNbitNumberInt(5)) {
            return false;
        }
        // bits[29] and bits[30] have to be opposite
        return ((bits ^ (bits << 1)) & (1 << 30)) != 0;
    }

    /* Convert Floating-point Precision (5.7.4.1) */
    /* Converts float to double and vice-versa */

    /**
     * Convert float to double and vice-versa.
     *
     * @param srcSize size of source register in bits.
     * @param dst floating point register. May not be null.
     * @param src floating point register. May not be null.
     */
    public void fcvt(int srcSize, CiRegister dst, CiRegister src) {
        if (srcSize == 32) {
            fpDataProcessing1Source(dst, src, floatFromSize(srcSize), Instruction.FCVTDS);
        } else {
            fpDataProcessing1Source(dst, src, floatFromSize(srcSize), Instruction.FCVTSD);
        }
    }

    /* Convert to Integer (5.7.4.2) */

    /**
     * Convert floating point to integer. Rounds towards zero.
     *
     * @param targetSize size of integer register. 32 or 64.
     * @param srcSize size of floating point register. 32 or 64.
     * @param dst general purpose register. May not be null, the zero-register or the stackpointer.
     * @param src floating point register. May not be null.
     */
    public void fcvtzs(int targetSize, int srcSize, CiRegister dst, CiRegister src) {
        assert Aarch64.isGeneralPurposeReg(dst) && Aarch64.isFpuReg(src);
        fcvtCpuFpuInstruction(dst, src, generalFromSize(targetSize), floatFromSize(srcSize), Instruction.FCVTZS);
    }

    /* Convert from Integer (5.7.4.2) */
    /**
     * Converts integer to floating point. Uses rounding mode defined by FCPR.
     *
     * @param targetSize size of floating point register. 32 or 64.
     * @param srcSize size of integer register. 32 or 64.
     * @param dst floating point register. May not be null.
     * @param src general purpose register. May not be null or the stackpointer.
     */
    public void scvtf(int targetSize, int srcSize, CiRegister dst, CiRegister src) {
        assert Aarch64.isFpuReg(dst) && Aarch64.isGeneralPurposeOrZeroReg(src);
        fcvtCpuFpuInstruction(dst, src, floatFromSize(targetSize), generalFromSize(srcSize), Instruction.SCVTF);
    }

    private void fcvtCpuFpuInstruction(CiRegister dst, CiRegister src,
                                       InstructionType type1, InstructionType type2, Instruction instr) {
        int instrEncoding = instr.encoding | FpConvertOp;
        emitInt(type1.encoding |
                type2.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src));
    }

    /* Floating-point Round to Integral (5.7.5) */

    /**
     * Rounds floating-point to integral. Rounds towards zero.
     *
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src floating point register. May not be null.
     */
    public void frintz(int size, CiRegister dst, CiRegister src) {
        fpDataProcessing1Source(dst, src, floatFromSize(size), Instruction.FRINTZ);
    }

    /* Floating-point Arithmetic (1 source) (5.7.6) */

    /**
     * dst = |src|.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src floating point register. May not be null.
     */
    public void fabs(int size, CiRegister dst, CiRegister src) {
        fpDataProcessing1Source(dst, src, floatFromSize(size), Instruction.FABS);
    }

    /**
     * dst = -neg.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src floating point register. May not be null.
     */
    public void fneg(int size, CiRegister dst, CiRegister src) {
        fpDataProcessing1Source(dst, src, floatFromSize(size), Instruction.FNEG);
    }

    /**
     * dst = Sqrt(src).
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src floating point register. May not be null.
     */
    public void fsqrt(int size, CiRegister dst, CiRegister src) {
        fpDataProcessing1Source(dst, src, floatFromSize(size), Instruction.FSQRT);
    }

    private void fpDataProcessing1Source(CiRegister dst, CiRegister src,
                                         InstructionType type, Instruction instr) {
        assert all(IS_FPU_REG, dst, src);
        int instrEncoding = instr.encoding | Fp1SourceOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src));
    }

    /* Floating-point Arithmetic (2 source) (5.7.7) */

    /**
     * dst = src1 + src2.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     */
    public void fadd(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        fpDataProcessing2Source(dst, src1, src2, floatFromSize(size), Instruction.FADD);
    }

    /**
     * dst = src1 - src2.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     */
    public void fsub(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        fpDataProcessing2Source(dst, src1, src2, floatFromSize(size), Instruction.FSUB);
    }

    /**
     * dst = src1 * src2.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     */
    public void fmul(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        fpDataProcessing2Source(dst, src1, src2, floatFromSize(size), Instruction.FMUL);
    }

    /**
     * dst = src1 / src2.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     */
    public void fdiv(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        fpDataProcessing2Source(dst, src1, src2, floatFromSize(size), Instruction.FDIV);
    }

    private void fpDataProcessing2Source(CiRegister dst, CiRegister src1,
                                         CiRegister src2, InstructionType type, Instruction instr) {
        assert all(IS_FPU_REG, dst, src1, src2);
        int instrEncoding = instr.encoding | Fp2SourceOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src1) |
                rs2(src2));
    }

    /* Floating-point Multiply-Add (5.7.9) */

    /**
     * dst = src1 * src2 + src3.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     * @param src3 floating point register. May not be null.
     */
    public void fmadd(int size, CiRegister dst, CiRegister src1, CiRegister src2, CiRegister src3) {
        fpDataProcessing3Source(dst, src1, src2, src3, floatFromSize(size), Instruction.FMADD);
    }

    /**
     * dst = src3 - src1 * src2.
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     * @param src3 floating point register. May not be null.
     */
    public void fmsub(int size, CiRegister dst, CiRegister src1, CiRegister src2, CiRegister src3) {
        fpDataProcessing3Source(dst, src1, src2, src3, floatFromSize(size), Instruction.FMSUB);
    }

    private void fpDataProcessing3Source(CiRegister dst, CiRegister src1, CiRegister src2, CiRegister src3,
                                         InstructionType type, Instruction instr) {
        assert all(IS_FPU_REG, dst, src1, src2, src3);
        int instrEncoding = instr.encoding | Fp3SourceOp;
        emitInt(type.encoding |
                instrEncoding |
                rd(dst) |
                rs1(src1) |
                rs2(src2) |
                rs3(src3));
    }

    /* Floating-point Comparison (5.7.10) */

    /**
     * Compares src1 to src2.
     *
     * @param size register size.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     */
    public void fcmp(int size, CiRegister src1, CiRegister src2) {
        fcmpInstruction(src1, src2, floatFromSize(size));
    }

    private void fcmpInstruction(CiRegister src1, CiRegister src2, InstructionType type) {
        assert all(IS_FPU_REG, src1, src2);
        int instrEncoding = Instruction.FCMP.encoding | FpCmpOp;
        emitInt(type.encoding |
                instrEncoding |
                rs1(src1) |
                rs2(src2));
    }

    /**
     * Conditional compare. NZCV = fcmp(src1, src2) if condition else uimm4.
     *
     * @param size register size.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     * @param uimm4 condition flags that are used if condition is false.
     * @param condition every condition allowed. May not be null.
     */
    public void fccmp(int size, CiRegister src1, CiRegister src2, int uimm4, ConditionFlag condition) {
        fConditionalCompareInstruction(src1, src2, uimm4, condition, floatFromSize(size));
    }

    private void fConditionalCompareInstruction(CiRegister src1, CiRegister src2,
                                                int uimm4, ConditionFlag condition, InstructionType type) {
        assert NumUtil.isUnsignedNbit(4, uimm4);
        assert all(IS_FPU_REG, src1, src2);
        emitInt(type.encoding |
                Instruction.FCCMP.encoding |
                uimm4 |
                condition.encoding << ConditionalConditionOffset |
                rs1(src1) |
                rs2(src2));
    }

    /**
     * Compare register to 0.0 .
     *
     * @param size register size.
     * @param src floating point register. May not be null.
     */
    public void fcmpZero(int size, CiRegister src) {
        fcmpZeroInstruction(src, floatFromSize(size));
    }

    private void fcmpZeroInstruction(CiRegister src, InstructionType type) {
        assert Aarch64.isFpuReg(src);
        int instrEncoding = Instruction.FCMPZERO.encoding | FpCmpOp;
        emitInt(type.encoding |
                instrEncoding |
                rs1(src));
    }

    /* Floating-point Conditional Select (5.7.11) */

    /**
     * Conditional select. dst = src1 if condition else src2.
     *
     * @param size register size.
     * @param dst floating point register. May not be null.
     * @param src1 floating point register. May not be null.
     * @param src2 floating point register. May not be null.
     * @param condition every condition allowed. May not be null.
     */
    public void fcsel(int size, CiRegister dst, CiRegister src1, CiRegister src2, ConditionFlag condition) {
        fConditionalSelect(dst, src1, src2, condition, floatFromSize(size));
    }

    private void fConditionalSelect(CiRegister dst, CiRegister src1, CiRegister src2,
                                    ConditionFlag condition, InstructionType type) {
        assert all(IS_FPU_REG, dst, src1, src2);
        emitInt(type.encoding |
                Instruction.FCSEL.encoding |
                rd(dst) |
                rs1(src1) |
                rs2(src2) |
                condition.encoding << ConditionalConditionOffset);
    }

    /**
     * dst = sysReg
     * Read system register to general purpose register.
     *
     * @param dst general purpose register. May not be SP or ZP.
     * @param srcSysReg system register.
     */
    public void mrs(CiRegister dst, SystemRegister srcSysReg) {
        assert all(IS_GENERAL_PURPOSE_REG, dst);

        emitInt(Instruction.MRS.encoding |
                (srcSysReg.encoding << 5) |
                rt(dst));
    }

    /**
     * dstSysReg = src
     * Set system register with the value in a general purpose register.
     *
     * @param dstSysReg system register.
     * @param src general purpose register. May not be SP or ZP.
     */
    public void msr(SystemRegister dstSysReg, CiRegister src) {
        assert all(IS_GENERAL_PURPOSE_REG, src);

        emitInt(Instruction.MSR_REG.encoding |
                (dstSysReg.encoding << 5) |
                rt(src));
    }

    /**
     * PStateField = uimm4
     * Set PSTATEField with an unsigned 4-bit immediate value.
     *
     * @param pStateField PStateField
     * @param uimm4 unsigned 4-bit immediate value
     */
    public void msr(PStateField pStateField, int uimm4) {
        assert NumUtil.isUnsignedNbit(4, uimm4);

        emitInt(Instruction.MSR_IMM.encoding |
                (pStateField.encoding << 5) |
                (uimm4 << 8) |
                0b11111);
    }

/*********/
    /* Debug exceptions (5.9.1.2) */

    /**
     * Halting mode software breakpoint: Enters halting mode debug state if enabled, else treated as UNALLOCATED instruction.
     *
     * @param uimm16 Arbitrary 16-bit unsigned payload.
     */
    public void hlt(int uimm16) {
        exceptionInstruction(uimm16, Instruction.HLT);
    }

    /**
     * Monitor mode software breakpoint: exception routed to a debug monitor executing in a higher exception level.
     *
     * @param uimm16 Arbitrary 16-bit unsigned payload.
     */
    public void brk(int uimm16) {
        exceptionInstruction(uimm16, Instruction.BRK);
    }

    public void brk() {
        brk(0);
    }

    /* Architectural hints (5.9.4) */
    public enum SystemHint {
        NOP(0x0), YIELD(0x1), WFE(0x2), WFI(0x3), SEV(0x4), SEVL(0x5);

        private final int encoding;

        SystemHint(int encoding) {
            this.encoding = encoding;
        }
    }

    public final void hlt() {
        hlt(0);
    }


    /**
     * Executes no-op instruction. No registers or flags are updated, except for PC.
     */
    public void nop() {
        hint(SystemHint.NOP);
    }

    public static int nopHelper() {
        return hintHelper(SystemHint.NOP);
    }

    private static int hintHelper(SystemHint hint) {
        return Instruction.HINT.encoding | hint.encoding << SystemImmediateOffset;
    }

    /**
     * Architectural hints.
     *
     * @param hint Can be any of the defined hints. May not be null.
     */
    public void hint(SystemHint hint) {
        emitInt(hintHelper(hint));
    }

    private void exceptionInstruction(int uimm16, Instruction instr) {
        assert NumUtil.isUnsignedNbit(16, uimm16);
        int instrEncoding = instr.encoding | ExceptionOp;
        emitInt(instrEncoding |
                uimm16 << SystemImmediateOffset);
    }

    /**
     * Clear Exclusive: clears the local record of the executing processor that an address has had a request for
     * an exclusive access.
     */
    public void clearex() {
        emitInt(Instruction.CLREX.encoding);
    }

    /**
     * Possible barrier definitions for Aarch64. LOAD_LOAD and LOAD_STORE map to the same underlying barrier.
     *
     * We only need synchronization across the inner shareable domain (see B2-90 in the Reference documentation).
     */
    public enum BarrierKind {
        LOAD_LOAD(0x9, "ISHLD"),
        LOAD_STORE(0x9, "ISHLD"),
        STORE_STORE(0xA, "ISHST"),
        STORE_LOAD(0xA, "ISHST"), /* not too sure about this */
        ANY_ANY(0xB, "ISH"),
        SY(0xF, "SY");

        public final int encoding;
        public final String optionName;

        BarrierKind(int encoding, String optionName) {
            this.encoding = encoding;
            this.optionName = optionName;
        }
    }

    /**
     * Data Memory Barrier.
     *
     * @param barrierKind barrier that is issued. May not be null.
     */
    public void dmb(BarrierKind barrierKind) {
        barrierInstruction(barrierKind, Instruction.DMB);
    }

    public void dsb(BarrierKind barrierKind) {
        barrierInstruction(barrierKind, Instruction.DSB);
    }

    public void isb() {
        barrierInstruction(BarrierKind.SY, Instruction.ISB);
    }

    private void barrierInstruction(BarrierKind barrierKind, Instruction instr) {
        int instrEncoding = instr.encoding | BarrierOp;
        emitInt(instrEncoding |
                barrierKind.encoding << BarrierKindOffset);
    }

//    // Artificial instructions for simulator. These instructions are illegal in the normal aarch64 ISA,
//    // but have special meaning for the simulator
//
//    /**
//     * Branch and link register instruction with the target code being native, i.e. not aarch64.
//     *
//     * The simulator has to do extra work so needs to know the number of arguments (both gp and fp) as well as the type
//     * of the return value.
//     * See assembler_aarch64.hpp.
//     *
//     * @param target general purpose register. May not be null, zero-register or stackpointer. Contains address of
//     *               target method.
//     * @param gpArgs number of general purpose arguments passed to the function. 4-bit unsigned.
//     * @param fpArgs number of floating point arguments passed to the function. 4-bit unsigned.
//     * @param returnType returnType of function. May not be null, or Kind.ILLEGAL.
//     */
//    public void blrNative(CiRegister target, int gpArgs, int fpArgs, Kind returnType) {
//        assert Aarch64.isGeneralPurposeReg(target) && NumUtil.isUnsignedNbit(4, gpArgs) &&
//                NumUtil.isUnsignedNbit(4, fpArgs) && returnType != null;
//        emitInt(Instruction.BLR_NATIVE.encoding |
//                target.encoding |
//                getReturnTypeEncoding(returnType) << 5 |
//                fpArgs << 7 |
//                gpArgs << 11);
//    }
//
//    private static int getReturnTypeEncoding(Kind returnType) {
//        // See assembler_aarch64.hpp for encoding details
//        switch(returnType) {
//            case Boolean:
//            case Byte:
//            case Short:
//            case Char:
//            case Int:
//            case Long:
//            case Object:
//                return 1;
//            case Float:
//                return 2;
//            case Double:
//                return 3;
//            case Void:
//            case Illegal:
//                // Void functions use a result of Kind.Illegal apparently
//                return 0;
//            default:
//                throw new Error("should not reach here!!! Illegal kind");
//        }
//    }

    /* Helper functions */
    private static int rd(CiRegister reg) {
        return reg.getEncoding() << RdOffset;
    }

    private static int rs1(CiRegister reg) {
        return reg.getEncoding() << Rs1Offset;
    }

    private static int rs2(CiRegister reg) {
        return reg.getEncoding() << Rs2Offset;
    }

    private static int rs3(CiRegister reg) {
        return reg.getEncoding() << Rs3Offset;
    }

    private static int rt(CiRegister reg) {
        return reg.getEncoding() << RtOffset;
    }

    private static int rt2(CiRegister reg) {
        return reg.getEncoding() << Rt2Offset;
    }

    private static int rn(CiRegister reg) {
        return reg.getEncoding() << RnOffset;
    }


    private static final Predicate<CiRegister> IS_GENERAL_PURPOSE_REG = new Predicate<CiRegister>() {
        @Override
        public boolean test(CiRegister register) {
            return Aarch64.isGeneralPurposeReg(register);
        }
    };

    private static final Predicate<CiRegister> IS_GENERAL_PURPOSE_OR_ZERO_REG = new Predicate<CiRegister>() {
        @Override
        public boolean test(CiRegister register) {
            return Aarch64.isGeneralPurposeOrZeroReg(register);
        }
    };

    private static final Predicate<CiRegister> IS_GENERAL_PURPOSE_OR_SP_REG = new Predicate<CiRegister>() {
        @Override
        public boolean test(CiRegister register) {
            return Aarch64.isGeneralPurposeOrSpReg(register);
        }
    };

    private static final Predicate<CiRegister> IS_FPU_REG = new Predicate<CiRegister>() {
        @Override
        public boolean test(CiRegister register) {
            return Aarch64.isFpuReg(register);
        }
    };

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
}
