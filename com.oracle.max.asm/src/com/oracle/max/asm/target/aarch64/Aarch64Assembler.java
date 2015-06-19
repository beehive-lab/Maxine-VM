package com.oracle.max.asm.target.aarch64;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class Aarch64Assembler extends AbstractAssembler {
    /**
     * The register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    public final CiRegister frameRegister;

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

    public final void movImmediate(CiRegister dst, int imm16) {
        int instruction = 0x52800000;
        instruction |= 1 << 31;
        instruction |= (imm16 & 0xffff) << 5;
        instruction |= (dst.encoding & 0x1f);
        emitInt(instruction);
    }

    public void add(final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0xB000000;
        instruction |= 1 << 31;
        instruction |= (Rm.encoding & 0x1f) << 16;
        instruction |= (Rn.encoding & 0x1f) << 5;
        instruction |= (Rd.encoding & 0x1f);
        emitInt(instruction);
    }

    public void sub(final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0x4B000000;
        instruction |= 1 << 31;
        instruction |= (Rm.encoding & 0x1f) << 16;
        instruction |= (Rn.encoding & 0x1f) << 5;
        instruction |= (Rd.encoding & 0x1f);
        emitInt(instruction);
    }

}
