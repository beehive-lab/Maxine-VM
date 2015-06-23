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
    protected void patchJumpTarget(int branch, int target) {
        // TODO Auto-generated method stub

    }

    /**
     * Enumeration of all different instruction kinds: General32/64 are the general instructions
     * (integer, branch, etc.), for 32-, respectively 64-bit operands.
     * FP32/64 is the encoding for the 32/64bit float operations
     */
    protected static enum InstructionType {
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
    protected static boolean isAimm(int imm) {
        return NumUtil.isUnsignedNbit(12, imm) ||
                NumUtil.isUnsignedNbit(12, imm >>> 12) && (imm & 0xfff) == 0;
    }


    /* Logical (immediate) (5.4.2) */

    /**
     * dst = src & bimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate.
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
    protected void orr(int size, CiRegister dst, CiRegister src, long bimm) {
        assert Aarch64.isGeneralPurposeOrSpReg(dst) && Aarch64.isGeneralPurposeOrZeroReg(src);
        logicalImmInstruction(dst, src, bimm, generalFromSize(size), Instruction.ORR);
    }

    protected void logicalImmInstruction(CiRegister dst, CiRegister src, long bimm,
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
