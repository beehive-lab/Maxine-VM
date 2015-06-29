package com.oracle.max.asm.target.aarch64;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

import static com.oracle.max.asm.target.aarch64.Aarch64Assembler.InstructionType.floatFromSize;
import static com.oracle.max.asm.target.aarch64.Aarch64Assembler.InstructionType.generalFromSize;
import static com.oracle.max.asm.target.aarch64.FunctionalUtils.Predicate;
import static com.oracle.max.asm.target.aarch64.FunctionalUtils.all;

public class Aarch64Assembler extends AbstractAssembler {
    private static final int RdOffset = 0;
    private static final int Rs1Offset = 5;
    private static final int Rs2Offset = 16;
    private static final int Rs3Offset = 10;
    private static final int RtOffset = 0;
    /**
     * The register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    public final CiRegister frameRegister;

    @Override
    public void patchJumpTarget(int branch, int target) {
        // TODO Auto-generated method stub

    }

    /**
     * Enumeration of all different instruction kinds: General32/64 are the general instructions
     * (integer, branch, etc.), for 32-, respectively 64-bit operands.
     * FP32/64 is the encoding for the 32/64bit float operations
     */
    public static enum InstructionType {
        General32(0x00000000, 32, true),
        General64(0x80000000, 64, true),
        FP32(0x00000000, 32, false),
        FP64(0x00400000, 64, false);

        public final int encoding;
        public final boolean isGeneral;
        public final int width;

        private InstructionType(int encoding, int width, boolean isGeneral) {
            this.encoding = encoding;
            this.width = width;
            this.isGeneral = isGeneral;
        }

        public static InstructionType generalFromSize(int size) {
            for (InstructionType type : values()) {
                if (type.isGeneral && type.width == size) {
                    return type;
                }
            }
            throw new Error("should not reach here");
        }

        public static InstructionType floatFromSize(int size) {
            for (InstructionType type : values()) {
                if (!type.isGeneral && type.width == size) {
                    return type;
                }
            }
            throw new Error("should not reach here");
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

    /**
     * Encoding for all instructions.
     */
    private static enum Instruction {
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

        BLR_NATIVE(0xc0000000);

        public final int encoding;

        private Instruction(int encoding) {
            this.encoding = encoding;
        }

    }

    public static enum ShiftType {
        LSL(0), LSR(1), ASR(2), ROR(3);

        public final int encoding;

        private ShiftType(int encoding) {
            this.encoding = encoding;
        }
    }

    public static enum ExtendType {
        UXTB(0), UXTH(1), UXTW(2), UXTX(3), SXTB(4), SXTH(5), SXTW(6), SXTX(7);

        public final int encoding;

        private ExtendType(int encoding) {
            this.encoding = encoding;
        }
    }

    /**
     * Condition Flags for branches. See 4.3
     */
    public static enum ConditionFlag {
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

        private ConditionFlag(int encoding) {
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
     * Constructs an assembler for the AMD64 architecture.
     *
     * @param registerConfig the register configuration used to bind {@link CiRegister#Frame} and
     *            {@link CiRegister#CallerFrame} to physical registers. This value can be null if this assembler
     *            instance will not be used to assemble instructions using these logical registers.
     */
    public Aarch64Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
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
            codeBuffer.emitInt(
                    Instruction.BCOND.encoding |
                    getConditionalBranchImm(imm21) |
                    condition.encoding);
        } else {
            codeBuffer.emitInt(
                    Instruction.BCOND.encoding |
                    getConditionalBranchImm(imm21) |
                    condition.encoding, pos);
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

    public final void movImmediate(CiRegister dst, int imm16) {
        int instruction = 0x52800000;
        instruction |= 1 << 31;
        instruction |= (imm16 & 0xffff) << 5;
        instruction |= (dst.encoding & 0x1f);
        emitInt(instruction);
    }

//    public void add(final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
//        int instruction = 0xB000000;
//        instruction |= 1 << 31;
//        instruction |= (Rm.encoding & 0x1f) << 16;
//        instruction |= (Rn.encoding & 0x1f) << 5;
//        instruction |= (Rd.encoding & 0x1f);
//        emitInt(instruction);
//    }
//
//    public void sub(final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
//        int instruction = 0x4B000000;
//        instruction |= 1 << 31;
//        instruction |= (Rm.encoding & 0x1f) << 16;
//        instruction |= (Rn.encoding & 0x1f) << 5;
//        instruction |= (Rd.encoding & 0x1f);
//        emitInt(instruction);
//    }



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

    /* Move (wide immediate) (5.4.3) */

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
        addSubShiftedInstruction(dst, src1, src2, shiftType, imm, generalFromSize(size), Instruction.SUBS);
    }

    private void addSubShiftedInstruction(CiRegister dst, CiRegister src1,
                                          CiRegister src2, ShiftType shiftType, int imm,
                                          InstructionType type, Instruction instr) {
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src1, src2);
        assert shiftType != ShiftType.ROR;
        assert imm >= 0 && imm < type.width;
        int instrEncoding = instr.encoding | AddSubShiftedOp;
        emitInt(type.encoding |
                instrEncoding |
                imm << ImmediateOffset |
                shiftType.encoding << ShiftTypeOffset |
                rd(dst) |
                rs1(src1) |
                rs2(src2));
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
        assert all(IS_GENERAL_PURPOSE_OR_ZERO_REG, dst, src2) && Aarch64.isGeneralPurposeOrSpReg(src1);
        addSubExtendedInstruction(dst, src1, src2, extendType, shiftAmt, generalFromSize(size), Instruction.ADDS);
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
     * dst = src1 >> (src2 & log2(size)).
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
     * dst = src1 << (src2 & log2(size)).
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
     * dst = src1 >>> (src2 & log2(size)).
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

//    /* Load-Store Single FP register (5.7.1.1) */
//    /**
//     * Floating point load.
//     *
//     * @param size number of bits read from memory into rt. Must be 32 or 64.
//     * @param rt floating point register. May not be null.
//     * @param address all addressing modes allowed. May not be null.
//     */
//    public void fldr(int size, CiRegister rt, ARMv8Address address) {
//        assert Aarch64.isFpuReg(rt);
//        assert size == 32 || size == 64;
//        int transferSize = NumUtil.log2Ceil(size / 8);
//        loadStoreInstruction(rt, address, InstructionType.FP32, Instruction.LDR, transferSize);
//    }
//
//    /**
//     * Floating point store.
//     *
//     * @param size number of bits read from memory into rt. Must be 32 or 64.
//     * @param rt floating point register. May not be null.
//     * @param address all addressing modes allowed. May not be null.
//     */
//    public void fstr(int size, CiRegister rt, ARMv8Address address) {
//        assert Aarch64.isFpuReg(rt);
//        assert size == 32 || size == 64;
//        int transferSize = NumUtil.log2Ceil(size / 8);
//        loadStoreInstruction(rt, address, InstructionType.FP64, Instruction.STR, transferSize);
//    }

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

    /* Architectural hints (5.9.4) */
    public static enum SystemHint {
        NOP(0x0), YIELD(0x1), WFE(0x2), WFI(0x3), SEV(0x4), SEVL(0x5);

        private final int encoding;

        private SystemHint(int encoding) {
            this.encoding = encoding;
        }
    }

    /**
     * Architectural hints.
     *
     * @param hint Can be any of the defined hints. May not be null.
     */
    public void hint(SystemHint hint) {
        emitInt(Instruction.HINT.encoding |
                hint.encoding << SystemImmediateOffset);
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
    public void clrex() {
        emitInt(Instruction.CLREX.encoding);
    }

    /**
     * Possible barrier definitions for Aarch64. LOAD_LOAD and LOAD_STORE map to the same underlying barrier.
     *
     * We only need synchronization across the inner shareable domain (see B2-90 in the Reference documentation).
     */
    public static enum BarrierKind {
        LOAD_LOAD(0x9, "ISHLD"),
        LOAD_STORE(0x9, "ISHLD"),
        STORE_STORE(0xA, "ISHST"),
        ANY_ANY(0xB, "ISH");

        public final int encoding;
        public final String optionName;

        private BarrierKind(int encoding, String optionName) {
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
//                throw GraalInternalError.shouldNotReachHere("Illegal kind");
//        }
//    }
















    /* Helper functions */
    private static int rd(CiRegister reg) {
        return reg.encoding << RdOffset;
    }

    private static int rs1(CiRegister reg) {
        return reg.encoding << Rs1Offset;
    }

    private static int rs2(CiRegister reg) {
        return reg.encoding << Rs2Offset;
    }

    private static int rs3(CiRegister reg) {
        return reg.encoding << Rs3Offset;
    }

    private static int rt(CiRegister reg) {
        return reg.encoding << RtOffset;
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

}
