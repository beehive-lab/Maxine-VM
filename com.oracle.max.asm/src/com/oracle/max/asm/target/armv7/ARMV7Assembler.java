package com.oracle.max.asm.target.armv7;

import com.oracle.max.asm.*;

import com.oracle.max.asm.AbstractAssembler;
import com.sun.cri.ci.CiAddress;
import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;
import com.sun.cri.ri.RiRegisterConfig;

import static com.oracle.max.asm.NumUtil.isByte;

/**
 * Created with IntelliJ IDEA.
 * User: yaman
 * Date: 10/12/13
 * Time: 09:19
 * To change this template use File | Settings | File Templates.
 */


public class ARMV7Assembler extends AbstractAssembler {
    /**
     * The register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    public final CiRegister frameRegister;

    /* APN not sure if needed
    * */
    public final CiRegister scratchRegister;


    /**
     * Constructs an assembler for the ARMV7 architecture.
     *
     * @param registerConfig the register configuration used to bind {@link CiRegister#Frame} and
     *            {@link CiRegister#CallerFrame} to physical registers. This value can be null if this assembler
     *            instance will not be used to assemble instructions using these logical registers.
     */

    public ARMV7Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        // APN bit of a hack to set the scratch as sometimes initialised with null
        // reigsterconfigs from ARMAdapterGenerator calls to create assembler ...
        this.scratchRegister = registerConfig == null ? ARMV7.r12 : registerConfig.getScratchRegister();
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
    }

    public enum ConditionFlag {
        Equal(0x0, "="),
        NotEqual(0x1, "!="),
        CarrySet(0x2, "|carry|"),
        CarryClear(0x3, "|ncarry|"),
        Minus(0x4, "|neg|"),
        Positive(0x5, "|pos|"),
        SignedOverflow(0x6,".of."),
        NoSignedOverflow(0x7, "|nof|"),
        UnsignedHigher(0x8, "|>|"),
        UnsignedLowerOrEqual(0x9, "|<=|"),
        SignedGreaterOrEqual(0xA, ".>=."),
        SignedLesser(0xB, ".<."),
        SignedGreater(0xC, ".>."),
        SignedLowerOrEqual(0xD, ".<=."),
        Always(0xE, "al");
        public static final ConditionFlag[] values = values();

        private final int value;
        private final String operator;
        private ConditionFlag(int value, String operator) {
            this.value = value;
            this.operator = operator;
        }

        public int value() {
            return value;
        }


    }
    public void branch(Label l) {
    if (l.isBound()) {
        // APn I need to compute a relative address if it is less than 24bits;
        // then branch
        // or I need to compute an absolute address and do a MOV PC,absolute.
        //branch(l.position(), false);
        checkConstraint(-0x800000<=(l.position()-codeBuffer.position()) && (l.position()-codeBuffer.position())<=0x7fffff, "branch must be within  a 24bit offset"  );
        emitInt(0x06000000|(l.position()-codeBuffer.position())|ConditionFlag.Always.value()&0xf ) ;
    } else {
        // By default, forward jumps are always 24-bit displacements, since
        // we can't yet know where the label will be bound. If you're sure that
        // the forward jump will not run beyond 24-bits bytes, then its ok

        l.addPatchAt(codeBuffer.position());
        emitByte(0xE9);
        emitInt(0);
    }
    }
    @Override
    protected void patchJumpTarget(int branch, int target) {
        // b, bl & bx goes here
        checkConstraint(-0x800000<=(target-branch) && (target-branch)<=0x7fffff, "branch must be within  a 24bit offset"  );
        emitInt(0x06000000|(target-branch)|ConditionFlag.Always.value()&0xf ) ;

    }

// START GENERATED RAW ASSEMBLER METHODS
    /**
     * Pseudo-external assembler syntax: {@code adc[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>immed_8</i>, <i>rotate_amount</i>
     * Example disassembly syntax: {@code adceq         r0, r0, #0x0, 0x0}
     * <p>
     * Constraint: {@code 0 <= immed_8 && immed_8 <= 255}<br />
     * Constraint: {@code (rotate_amount % 2) == 0}<br />
     * Constraint: {@code 0 <= rotate_amount / 2 && rotate_amount / 2 <= 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.2"
     */
    // Template#: 1, Serial#: 2
    public void adc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x02A00000;
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint((rotate_amount % 2) == 0, "(rotate_amount % 2) == 0");
        checkConstraint(0 <= rotate_amount / 2 && rotate_amount / 2 <= 15, "0 <= rotate_amount / 2 && rotate_amount / 2 <= 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (immed_8 & 0xff);
        instruction |= ((rotate_amount / 2 & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code adceq         r0, r0, r0, lsl #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.2"
     */
    // Template#: 2, Serial#: 4
    public void adclsl(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x00A00000;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * REMEMBER in ARM an 12bit immediate is represented as an 8 bit number with 4 bit rotation
     * Pseudo-external assembler syntax: {@code add[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>immed_8</i>, <i>rotate_amount</i>
     * Example disassembly syntax: {@code addeq         r0, r0, #0x0, 0x0}
     * <p>
     * Constraint: {@code 0 <= immed_8 && immed_8 <= 255}<br />
     * Constraint: {@code (rotate_amount % 2) == 0}<br />
     * Constraint: {@code 0 <= rotate_amount / 2 && rotate_amount / 2 <= 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.3"
     */
    // Template#: 3, Serial#: 14
    public void add(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x02800000;
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint((rotate_amount % 2) == 0, "(rotate_amount % 2) == 0");
        checkConstraint(0 <= rotate_amount / 2 && rotate_amount / 2 <= 15, "0 <= rotate_amount / 2 && rotate_amount / 2 <= 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (immed_8 & 0xff);
        instruction |= ((rotate_amount / 2 & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code add[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code addeq         r0, r0, r0, ror #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.3"
     */
    // Template#: 4, Serial#: 19
    public void addror(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x00800060;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code bic[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code biceq         r0, r0, r0, lsr r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.6"
     */
    // Template#: 5, Serial#: 45
    public void biclsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x01C00030;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmn[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rn</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code cmneq         r0, r0, asr r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.13"
     */
    // Template#: 6, Serial#: 58
    public void cmnasr(final ConditionFlag cond, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x01700050;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmn[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rn</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code cmneq         r0, r0, ror r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.13"
     */
    // Template#: 7, Serial#: 59
    public void cmnror(final ConditionFlag cond, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x01700070;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code cmpeq         r0, r0, asr #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm % 32 && shift_imm % 32 <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.14"
     */
    // Template#: 8, Serial#: 66
    public void cmpasr(final ConditionFlag cond, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x01500040;
        checkConstraint(0 <= shift_imm % 32 && shift_imm % 32 <= 31, "0 <= shift_imm % 32 && shift_imm % 32 <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm % 32 & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code eor[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code eoreq         r0, r0, r0, lsr r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.15"
     */
    // Template#: 9, Serial#: 81
    public void eorlsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x00200030;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }
    public void eor(final ConditionFlag cond,final boolean s, final CiRegister Rd,final CiRegister Rn,final CiRegister Rm,final int imm5,final int  imm2) {
        int instruction = 0x00200000;
        // type ie imm2 refers to 00 LSL
        //                 01 LSR
        //                 10 ASR
        //                 11   if imm5 == 00000 RRX, shift_n = 1
        //                      else ROR, shift_n = imm5; as a uint.
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((imm5&0x1f) << 7);
        instruction |= ((imm2&0x3) << 5);
        instruction |= ((Rm.encoding&0xf));
        emitInt(instruction);

    }

    /**
     * Pseudo-external assembler syntax: {@code mov[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code moveq         r0, r0, ror #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.29"
     */
    // Template#: 10, Serial#: 91
    /* please check usage this encoding is movror IMMEDIATE it provides the contents of a register rotated by a constant value
    watch out for a rotate of ZERO bits that actually does RRX!!!!, this means the carry flag is shifted into bit 31!
    APN might have used movror to provide nops in this case!!!! and that would be wrong unless scratch r12 was used!!!
     */
    public void movror(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x01A00060;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    public void movt(final ConditionFlag cond,final CiRegister Rd, final int imm16) {
        int instruction = 0x03400000;
        checkConstraint(0<= imm16 && imm16 <= 65535,"0<= imm16 && imm16 <= 65535 " );
        instruction |= ((cond.value()&0xf) << 28);
        instruction |= ((imm16>> 12)<<16);
        instruction |= ((Rd.encoding &0xf) << 12);
        instruction |= (imm16&0xfff);
        emitInt(instruction);

    }

    public void movw(final ConditionFlag cond,final CiRegister Rd, final int imm16) {
        int instruction = 0x03000000;
        checkConstraint(0<= imm16 && imm16 <= 65535,"0<= imm16 && imm16 <= 65535 " );
        instruction |= ((cond.value()&0xf) << 28);
        instruction |= ((imm16>> 12)<<16);
        instruction |= ((Rd.encoding &0xf) << 12);
        instruction |= (imm16&0xfff);
        emitInt(instruction);

    }

    /**
     * Pseudo-external assembler syntax: {@code mvn[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code mvneq         r0, r0, ror r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.34"
     */
    // Template#: 11, Serial#: 107
    public void mvnror(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x01E00070;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code orr[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code orreq         r0, r0, r0, lsl #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.35"
     */
    // Template#: 12, Serial#: 112
    public void orrlsl(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x01800000;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code rsb[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>immed_8</i>, <i>rotate_amount</i>
     * Example disassembly syntax: {@code rsbeq         r0, r0, #0x0, 0x0}
     * <p>
     * Constraint: {@code 0 <= immed_8 && immed_8 <= 255}<br />
     * Constraint: {@code (rotate_amount % 2) == 0}<br />
     * Constraint: {@code 0 <= rotate_amount / 2 && rotate_amount / 2 <= 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.36"
     */
    // Template#: 13, Serial#: 122
    public void rsb(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x02600000;
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint((rotate_amount % 2) == 0, "(rotate_amount % 2) == 0");
        checkConstraint(0 <= rotate_amount / 2 && rotate_amount / 2 <= 15, "0 <= rotate_amount / 2 && rotate_amount / 2 <= 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (immed_8 & 0xff);
        instruction |= ((rotate_amount / 2 & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code rsb[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code rsbeq         r0, r0, r0, lsl #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.36"
     */
    // Template#: 14, Serial#: 124
    public void rsblsl(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x00600000;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code rsc[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>
     * Example disassembly syntax: {@code rsceq         r0, r0, r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.37"
     */
    // Template#: 15, Serial#: 135
    public void rsc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm) {
        int instruction = 0x00E00000;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code rsc[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code rsceq         r0, r0, r0, lsr r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.37"
     */
    // Template#: 16, Serial#: 141
    public void rsclsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x00E00030;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbc[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code sbceq         r0, r0, r0, ror r0}
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.38"
     */
    // Template#: 17, Serial#: 155
    public void sbcror(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x00C00070;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>Rd</i>, <i>Rn</i>, <i>immed_8</i>, <i>rotate_amount</i>
     * Example disassembly syntax: {@code subeq         r0, r0, #0x0, 0x0}
     * <p>
     * Constraint: {@code 0 <= immed_8 && immed_8 <= 255}<br />
     * Constraint: {@code (rotate_amount % 2) == 0}<br />
     * Constraint: {@code 0 <= rotate_amount / 2 && rotate_amount / 2 <= 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.49"
     */
    // Template#: 18, Serial#: 158
    public void sub(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x02400000;
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint((rotate_amount % 2) == 0, "(rotate_amount % 2) == 0");
        checkConstraint(0 <= rotate_amount / 2 && rotate_amount / 2 <= 15, "0 <= rotate_amount / 2 && rotate_amount / 2 <= 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (immed_8 & 0xff);
        instruction |= ((rotate_amount / 2 & 0xf) << 8);
        emitInt(instruction);
    }
    public void sub(final ConditionFlag cond, final boolean s, final CiRegister Rd,
                    final CiRegister Rn, final CiRegister Rm,final int imm5, final int imm2Type)     {
        int instruction = 0x00400000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0<= imm2Type && imm2Type <= 3, "0<= imm2Type && imm2Type <= 3");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rm.encoding & 0xf));
        instruction |= ((imm2Type & 0x3) <<5);
        instruction |= ((imm5 & 0x31) <<5);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code tst[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rn</i>, <i>immediate</i>
     * Example disassembly syntax: {@code tsteq         r0, #0x0}
     * <p>
     * Constraint: {@code ARMImmediates.isValidImmediate(immediate)}<br />
     * Constraint: {@code 0 <= ARMImmediates.calculateShifter(immediate) && ARMImmediates.calculateShifter(immediate) <= 4095}<br />
     *
     * @see com.oracle.max.asm.target.armv7.ARMImmediates#isValidImmediate
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.54"
     */
    // Template#: 19, Serial#: 181
    public void tst(final ConditionFlag cond, final CiRegister Rn, final int immediate) {
        int instruction = 0x03100000;
        checkConstraint(0 <= ARMImmediates.calculateShifter(immediate) && ARMImmediates.calculateShifter(immediate) <= 4095, "0 <= ARMImmediates.calculateShifter(immediate) && ARMImmediates.calculateShifter(immediate) <= 4095");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (ARMImmediates.calculateShifter(immediate) & 0xfff);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code tst[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code tsteq         r0, r0, lsr #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm % 32 && shift_imm % 32 <= 31}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.54"
     */
    // Template#: 20, Serial#: 185
    public void tstlsr(final ConditionFlag cond, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x01100020;
        checkConstraint(0 <= shift_imm % 32 && shift_imm % 32 <= 31, "0 <= shift_imm % 32 && shift_imm % 32 <= 31");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm % 32 & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code smlal[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>RdLo</i>, <i>RdHi</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code smlaleq       r0, r0, r0, r0}
     * <p>
     * Constraint: {@code RdLo.encoding != RdHi.encoding}<br />
     * Constraint: {@code RdLo.encoding != Rm.encoding}<br />
     * Constraint: {@code RdHi.encoding != Rm.encoding}<br />
     * Constraint: {@code RdHi.encoding != 15}<br />
     * Constraint: {@code RdLo.encoding != 15}<br />
     * Constraint: {@code Rm.encoding != 15}<br />
     * Constraint: {@code Rs.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.39"
     */
    // Template#: 21, Serial#: 195
    public void smlal(final ConditionFlag cond, final boolean s, final CiRegister RdLo, final CiRegister RdHi, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x00E00090;
        checkConstraint(RdLo.encoding != RdHi.encoding, "RdLo.encoding != RdHi.encoding");
        checkConstraint(RdLo.encoding != Rm.encoding, "RdLo.encoding != Rm.encoding");
        checkConstraint(RdHi.encoding != Rm.encoding, "RdHi.encoding != Rm.encoding");
        checkConstraint(RdHi.encoding != 15, "RdHi.encoding != 15");
        checkConstraint(RdLo.encoding != 15, "RdLo.encoding != 15");
        checkConstraint(Rm.encoding != 15, "Rm.encoding != 15");
        checkConstraint(Rs.encoding != 15, "Rs.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((RdLo.encoding & 0xf) << 12);
        instruction |= ((RdHi.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code umull[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv][s]  }<i>RdLo</i>, <i>RdHi</i>, <i>Rm</i>, <i>Rs</i>
     * Example disassembly syntax: {@code umulleq       r0, r0, r0, r0}
     * <p>
     * Constraint: {@code RdLo.encoding != RdHi.encoding}<br />
     * Constraint: {@code RdLo.encoding != Rm.encoding}<br />
     * Constraint: {@code RdHi.encoding != Rm.encoding}<br />
     * Constraint: {@code RdHi.encoding != 15}<br />
     * Constraint: {@code RdLo.encoding != 15}<br />
     * Constraint: {@code Rm.encoding != 15}<br />
     * Constraint: {@code Rs.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.56"
     */
    // Template#: 22, Serial#: 198
    public void umull(final ConditionFlag cond, final boolean s, final CiRegister RdLo, final CiRegister RdHi, final CiRegister Rm, final CiRegister Rs) {
        int instruction = 0x00800090;
        checkConstraint(RdLo.encoding != RdHi.encoding, "RdLo.encoding != RdHi.encoding");
        checkConstraint(RdLo.encoding != Rm.encoding, "RdLo.encoding != Rm.encoding");
        checkConstraint(RdHi.encoding != Rm.encoding, "RdHi.encoding != Rm.encoding");
        checkConstraint(RdHi.encoding != 15, "RdHi.encoding != 15");
        checkConstraint(RdLo.encoding != 15, "RdLo.encoding != 15");
        checkConstraint(Rm.encoding != 15, "Rm.encoding != 15");
        checkConstraint(Rs.encoding != 15, "Rs.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((s?1:0) << 20);
        instruction |= ((RdLo.encoding & 0xf) << 12);
        instruction |= ((RdHi.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((Rs.encoding & 0xf) << 8);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code ldr[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>
     * Example disassembly syntax: {@code ldreq         r0, [r0, +r0]}
     * <p>
     * Constraint: {@code Rm.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.20"
     */
    // Template#: 23, Serial#: 204
    public void ldradd(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm) {
        int instruction = 0x07900000;
        checkConstraint(Rm.encoding != 15, "Rm.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        emitInt(instruction);
    }
    public void strd(final ConditionFlag cond,int P, int U, int W,
                    final CiRegister Rt, final CiRegister Rn, final CiRegister Rm) {
        int instruction = 0x000000f0;
        instruction |= ((P&0x1) << 24);
        instruction |= ((U&0x1) << 23);
        instruction |= ((W&0x1) << 21);
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rt.encoding & 0xf) << 12);
        instruction |= (Rm.encoding & 0xf);

        emitInt(instruction);
    }
    public void str(final ConditionFlag cond,int P, int U, int W,
                    final CiRegister Rt, final CiRegister Rn, final CiRegister Rm,int imm5, int imm2Type) {
        int instruction = 0x06000000;
        instruction |= ((P&0x1) << 24);
        instruction |= ((U&0x1) << 23);
        instruction |= ((W&0x1) << 21);
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rt.encoding & 0xf) << 12);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((imm5& 0x1f) <<7);
        instruction |= ((imm2Type & 0x3) << 5);
        emitInt(instruction);
    }
    /**
     * Pseudo-external assembler syntax: {@code ldr[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rd</i>, <i>Rn</i>, <i>offset_12</i>
     * Example disassembly syntax: {@code ldreq         r0, [r0, #-0x0]!}
     * <p>
     * Constraint: {@code 0 <= offset_12 && offset_12 <= 4095}<br />
     * Constraint: {@code Rd.encoding != Rn.encoding}<br />
     * Constraint: {@code Rn.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.20"
     */
    // Template#: 24, Serial#: 217
    public void ldrsubw(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rn, final int offset_12) {
        int instruction = 0x05300000;
        checkConstraint(0 <= offset_12 && offset_12 <= 4095, "0 <= offset_12 && offset_12 <= 4095");
        checkConstraint(Rd.encoding != Rn.encoding, "Rd.encoding != Rn.encoding");
        checkConstraint(Rn.encoding != 15, "Rn.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (offset_12 & 0xfff);
        emitInt(instruction);
    }
    public void ldrshw(final ConditionFlag cond, int P, int U, int W,final CiRegister Rn, final CiRegister Rt, final CiRegister Rm)
    {

        int instruction =       0x001000f0;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P<<24) |   (U<<23) | (W << 21);
        instruction |=    ((cond.value() & 0xf) << 28);
        instruction |=     ( (Rn.encoding &0xf) << 16);
        instruction |=     ( (Rt.encoding &0xf) << 12);
        instruction |=      (Rm.encoding &0xf);
        emitInt(instruction);
    }
    public void ldrb(final ConditionFlag cond, int P, int U, int W,final CiRegister Rn, final CiRegister Rt, final CiRegister Rm,
    int imm2Type, int imm5)
    {

        int instruction =       0x06100000;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P<<24) |   (U<<23) | (W << 21);
        instruction |=    ((cond.value() & 0xf) << 28);
        instruction |=     ( (Rn.encoding &0xf) << 16);
        instruction |=     ( (Rt.encoding &0xf) << 12);
        instruction |=      (Rm.encoding &0xf);
        instruction |=  ((imm2Type & 0x3) <<5);
        instruction |=  ((imm5&0x1f) << 7);
        emitInt(instruction);
    }
    public void ldr(final ConditionFlag cond, int P, int U, int W,final CiRegister Rn, final CiRegister Rt, final CiRegister Rm,
                     int imm2Type, int imm5)
    {

        int instruction =       0x06100000;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P<<24) |   (U<<23) | (W << 21);
        instruction |=    ((cond.value() & 0xf) << 28);
        instruction |=     ( (Rn.encoding &0xf) << 16);
        instruction |=     ( (Rt.encoding &0xf) << 12);
        instruction |=      (Rm.encoding &0xf);
        instruction |=  ((imm2Type & 0x3) <<5);
        instruction |=  ((imm5&0x1f) << 7);
        emitInt(instruction);
    }
    public void movss(final ConditionFlag cond, int P, int U, int W,final CiRegister Rn, final CiRegister Rt, final CiRegister Rm,
        int imm2Type, int imm5)
    {                // move a float ...
                    // APN might want/need to make it use special registers?
                    // so some logic might need to be placed here to choose the correct instruction
        ldr(cond,P,U,W,Rn,Rt,Rm,imm2Type,imm5);

    }
    public void movsd(final ConditionFlag cond, int P, int U, int W,final CiRegister Rn, final CiRegister Rt, final CiRegister Rm)
    {
        // APN same issue as above ...
        ldrd(cond,P,U,W,Rn,Rt,Rm);

    }
    public void ldrd(final ConditionFlag cond, int P, int U, int W,final CiRegister Rn, final CiRegister Rt, final CiRegister Rm)
    {

        int instruction =       0x000000d0;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P<<24) |   (U<<23) | (W << 21);
        instruction |=    ((cond.value() & 0xf) << 28);
        instruction |=     ( (Rn.encoding &0xf) << 16);
        instruction |=     ( (Rt.encoding &0xf) << 12);
        instruction |=      (Rm.encoding &0xf);
        emitInt(instruction);
    }
    /**
     * Pseudo-external assembler syntax: {@code ldr[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code ldreq         r0, [r0], +r0, ror #0x0}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     * Constraint: {@code Rm.encoding != 15}<br />
     * Constraint: {@code Rn.encoding != 15}<br />
     * Constraint: {@code Rm.encoding != Rn.encoding}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.20"
     */
    // Template#: 25, Serial#: 240
    public void ldraddrorpost(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x06900060;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        checkConstraint(Rm.encoding != 15, "Rm.encoding != 15");
        checkConstraint(Rn.encoding != 15, "Rn.encoding != 15");
        checkConstraint(Rm.encoding != Rn.encoding, "Rm.encoding != Rn.encoding");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code str[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code streq         r0, [r0, -r0, asr #0x0]}
     * <p>
     * Constraint: {@code 0 <= shift_imm % 32 && shift_imm % 32 <= 31}<br />
     * Constraint: {@code Rm.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.44"
     */
    // Template#: 26, Serial#: 253
    public void strsubasr(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x07000040;
        checkConstraint(0 <= shift_imm % 32 && shift_imm % 32 <= 31, "0 <= shift_imm % 32 && shift_imm % 32 <= 31");
        checkConstraint(Rm.encoding != 15, "Rm.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm % 32 & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code str[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rd</i>, <i>Rn</i>, <i>Rm</i>, <i>shift_imm</i>
     * Example disassembly syntax: {@code streq         r0, [r0, -r0, ror #0x0]!}
     * <p>
     * Constraint: {@code 0 <= shift_imm && shift_imm <= 31}<br />
     * Constraint: {@code Rd.encoding != Rn.encoding}<br />
     * Constraint: {@code Rm.encoding != Rn.encoding}<br />
     * Constraint: {@code Rn.encoding != 15}<br />
     * Constraint: {@code Rm.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.44"
     */
    // Template#: 27, Serial#: 269
    public void strsubrorw(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x07200060;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        checkConstraint(Rd.encoding != Rn.encoding, "Rd.encoding != Rn.encoding");
        checkConstraint(Rm.encoding != Rn.encoding, "Rm.encoding != Rn.encoding");
        checkConstraint(Rn.encoding != 15, "Rn.encoding != 15");
        checkConstraint(Rm.encoding != 15, "Rm.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (Rm.encoding & 0xf);
        instruction |= ((shift_imm & 0x1f) << 7);
        emitInt(instruction);
    }

    /**
     * Pseudo-external assembler syntax: {@code str[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>Rd</i>, <i>Rn</i>, <i>offset_12</i>
     * Example disassembly syntax: {@code streq         r0, [r0], #+0x0}
     * <p>
     * Constraint: {@code 0 <= offset_12 && offset_12 <= 4095}<br />
     * Constraint: {@code Rn.encoding != 15}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.44"
     */
    // Template#: 28, Serial#: 272
    public void straddpost(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rn, final int offset_12) {
        int instruction = 0x04800000;
        checkConstraint(0 <= offset_12 && offset_12 <= 4095, "0 <= offset_12 && offset_12 <= 4095");
        checkConstraint(Rn.encoding != 15, "Rn.encoding != 15");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= (offset_12 & 0xfff);
        emitInt(instruction);
    }




    /**
     * Pseudo-external assembler syntax: {@code swi[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>immed_24</i>
     * Example disassembly syntax: {@code swieq         0x0}
     * <p>
     * Constraint: {@code 0 <= immed_24 && immed_24 <= 16777215}<br />
     *
     * @see "ARM Architecture Reference Manual, Second Edition - Section 4.1.50"
     */
    // Template#: 29, Serial#: 289
    public void swi(final ConditionFlag cond, final int immed_24) {
        int instruction = 0x0F000000;
        checkConstraint(0 <= immed_24 && immed_24 <= 16777215, "0 <= immed_24 && immed_24 <= 16777215");
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= (immed_24 & 0xffffff);
        emitInt(instruction);
    }
    // APN not including Thumb mode for now
    private int ldmstmHelper(final int isStore,final ConditionFlag flag,final int upWard,
            final int preIndexing, final int wBit, final int sBit,final CiRegister baseRegister,final int registerList) {
        int instruction;
        instruction = ((flag.value() &0xf)<< 28);
        instruction |= (((0x8|preIndexing)&0x9) <<24);
        instruction |= (((upWard&0x1)<<3)|((sBit&0x1) << 2)|((wBit&0x1)<<1)|(0x1&isStore)) <<20;
        instruction |= ((baseRegister.encoding &0xf) << 16);
        instruction |= (registerList & 0xFFFF);
        return instruction;

    }

    public void push(final ConditionFlag flag, final int registerList) {
        int instruction;
        instruction =  ((flag.value() &0xf)<< 28);

        instruction |= (0x9 << 24);
        instruction |= (0x2 << 20);
        instruction |= (0xd << 16);
        instruction |= (0xffff&registerList);


        emitInt(instruction);
    }
    public void pop(final ConditionFlag flag,final int  registerList) {
        int instruction;
        instruction =  ((flag.value() &0xf)<< 28);

        instruction |= (0x8 << 24);
        instruction |= (0xb << 20);
        instruction |= (0xd << 16);
        instruction |= (0xffff&registerList);
        emitInt(instruction);

    }
    public void strd(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseReg,
                    final int offset8) {
        int instruction;
        instruction= 0x004000f0;
        int P,U,W;
        P = 1;
        U = 1;
        W = 0;
        instruction |= P<<24;
        instruction |= U<<23;
        instruction |= W<<21;
        instruction |=  (valueReg.encoding&0xf) << 16;
        instruction |= (baseReg.encoding&0xf)<< 12;
        instruction |= (offset8 &0xf0) << 4;
        instruction |= (offset8&0xf);
        emitInt(instruction);
    }


    public void str(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseRegister, final int offset12)
    {
        int instruction;
        instruction = 0x05800000;
        instruction =  ((flag.value() &0xf)<< 28);
        instruction |= (valueReg.encoding&0xf) << 16;
        instruction |= (baseRegister.encoding&0xf)<<12;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);
    }
    public void ldr(final ConditionFlag flag, final CiRegister destReg, final CiRegister baseRegister, final int offset12)
    {
        int instruction;
        instruction = 0x05900000;
        instruction =  ((flag.value() &0xf)<< 28);
        instruction |= (destReg.encoding&0xf) << 16;
        instruction |= (baseRegister.encoding&0xf)<<12;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);

    }
    /*public void ldm(final ConditionFlag flag,final int upWard,final int preIndexing,
                    final int wBit, final int sBit,final CiRegister baseRegister,final int registerList ) {
        int instruction = ldmstmHelper(1, flag, upWard, preIndexing, wBit, sBit, baseRegister, registerList);
        emitInt(instruction);
    }
    // APN not including Thumb mode for now

    public void stm(final ConditionFlag flag,final int upWard,final int preIndexing,
                    final int wBit, final int sBit,final CiRegister baseRegister,final int registerList)  {
        int instruction = ldmstmHelper(0,flag,upWard,preIndexing,wBit,sBit,baseRegister,registerList);
        emitInt(instruction);
    }
    */
    public void cmp(final ConditionFlag flag, final CiRegister Rn, final CiRegister Rm, int imm5, int imm2Type) {
        int instruction = 0x01500000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0 <= imm2Type && imm2Type <= 3");
        instruction |=  ((flag.value() & 0xf) << 28);
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= ((imm5) << 7);
        instruction |= ((imm2Type) << 5);
        emitInt(instruction);

    }
// END GENERATED RAW ASSEMBLER METHODS

// START GENERATED LABEL ASSEMBLER METHODS
// END GENERATED LABEL ASSEMBLER METHODS

    protected void checkConstraint(boolean passed, String expression) {
        if (!passed) {
            throw new IllegalArgumentException(expression);
        }
    }

    private static int encode(CiRegister r) {
        assert r.encoding < 16 && r.encoding >= 0 : "encoding out of range: " + r.encoding;
        return r.encoding;
    }

    /* APN
        The methods below here are largely to interface the ARMV7Assembler to the ARMV7MAcroAssembler which is based on the X86
        version in the longer term we probably want a more natural encoding/fit to ARM elsewhere in Maxine and then to refactor
        but right now the priority is to get the port working.

        movl in the AMD assembler has complex semantics, it might be a constant it might be memory location .....
        movl is being replaced into mov32BitConstant and other yet to be implemented instruction aspects in order to disambiguate the desired operation from the
        purpose
     */
    public void setUpScratch(CiAddress addr) {
        // might be in memory
        // might be in a register

        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        assert (addr != CiAddress.Placeholder); // APN Placeholders not handled yet
        assert(base.isValid()); // APN can we have a memory address --- not handled yet?
        // APN simple case where we just have a register destination
        // TODO fix this so it will issue loads when appropriate!
        if (base.isValid()) {
            if(disp != 0) {
                mov32BitConstant(scratchRegister, disp);
                add(ConditionFlag.Always,false,scratchRegister,base,0,0);
            }
            if(index.isValid()){
                adclsl(ConditionFlag.Always,false,scratchRegister,scratchRegister,index,scale.log2); // APN even if scale is zero this is ok.
            }
        }
    }

    public final void decq(CiRegister dst) {
        assert(dst.isValid());
        sub(ConditionFlag.Always,false,dst,dst,1,0);
    }
    public final void subq(CiRegister dst, int imm32) {
        assert(dst.isValid());
        mov32BitConstant(scratchRegister, imm32);
        // dst = dst - imm32;
        sub(ConditionFlag.Always,false,dst,dst,scratchRegister,0,0);

    }
    public final void mov32BitConstant(CiRegister dst, int imm32)   {    // crude way to load a 32 bit immediate
        //assert(dst.isFpu());
        movw   (ConditionFlag.Always,dst,imm32&0xffff);
        movt    (ConditionFlag.Always,dst,((imm32&0xffff0000) >> 16)) ;

    }
    public final void alignForPatchableDirectCall() { // APN copy of X86
        /*
            APN as far as I am aware there are not alignment restrictions.
            seems to be an interaction with Safepoints

        */
        /*int dispStart = codeBuffer.position() + 1;

        int mask = target.wordSize - 1;
        System.err.println((codeBuffer.position()+1));
        if ((dispStart & ~mask) != ((dispStart + 3) & ~mask)) {
            for(int i = 0; i < (target.wordSize - (dispStart & mask));i++)
            //nop(target.wordSize - (dispStart & mask));
                    nop();
            System.err.println("total nops " + (target.wordSize - (dispStart & mask))+ " disp " + dispStart + " codebuff " + codeBuffer.position() + " mask "+ mask);
            // APN not relevant? assert ((codeBuffer.position() + 1) & mask) == 0;
        }*/
    }
    public final void call()
    {
        // ok we do not have the same semantics as intel
        // this is used for a call where we don't know the actual target when we insert it
        // ie for a trampoline.
        // APN proposes we use the scratch register to calculate an address then we do the mov pc
        // looking at Stubs.java we can see that all registers have been saved
        // so we can use whatever registers we want!
        emitInt(0); // movw(scratch,const)                                        fixup later
        emitInt(0); //movt(scratch,const)                                         fixup later
        movror(ConditionFlag.Always,false,ARMV7.r15,ARMV7.r12,0); // mov PC,scratch
        // APN need to update LR14 and do an absolute MOV to a new PC held in scratch
        // or need to do a BL
        // WHO/what/where is responsible for stack save/restore and procedure call standard

    }
    public final void call(CiRegister target)
    {
        emitInt(0); // movw(scratch,const)
        emitInt(0); //movt(scratch,const)
        emitInt(0); // mov PC,scratch
        // APN need to update LR14 and do an absolute MOV to a new PC held in scratch
        // or need to do a BL
        // WHO/what/where is responsible for stack save/restore and procedure call standard

    }
    public final void leave()
    {
        movror(ConditionFlag.Always,false,ARMV7.r15,ARMV7.r12,0); // might be wrong!

    }
    public final void movslq(CiAddress dst,int imm32)  {
        // TODO APN ok Im assuming this is just as simple mov rather than an actual sign extend?
        // it might be used in 64 bit mode, but ARMV7 is only 32 bit anyway.
        // if it transpires that this is necessary for 64bit values in a 32bit processor
        // -- we should be able to see this in the lowering phases AND/OR testing of bytecodes involving longs
        // then we will need to modify the function
        // to account for this ....
        // NOt sure how to work this, if dst is an address do we need to load the value of the
        // immediate into a 32bit address memory location??

        // not going to dwell on this for now but Im not confident that the code
        // below will be sensible
        // probable errors ... neds to use 2x32bit registers and do a sign extend
        // possibly needs to store result of sign extension in memory.
        mov32BitConstant(dst.base(), imm32);
    }
    public final void cmpl(CiRegister src,int imm32) {
        /*
        APN ... condition flags need to be set presumably and used at a later point
         */
        assert(src.isValid());
        mov32BitConstant(scratchRegister, imm32);
        cmp(ConditionFlag.Always,src,scratchRegister,0,0);

    }
    public final void cmpl(CiRegister src1, CiAddress src2) {
        /*
        APN condition flags need to be set and used at a later point
         */
        setUpScratch(src2); // APN not sure if this requires a load!
        assert(src1.isValid());
        cmp(ConditionFlag.Always,src1,scratchRegister,0,0);
    }

    public final void cmpl(CiRegister src1, CiRegister src2) {

        cmp(ConditionFlag.Always,src1,src2,0,0);
    }

    public final void incq(CiRegister dst) {
        assert(dst.isValid());
        add(ConditionFlag.Always,false,dst,dst,1,0);
    }
    public final void addq(CiRegister dst,int imm32)    {
        assert(dst.isValid());
        mov32BitConstant(scratchRegister, imm32); ;
        // dst = dst + imm32;
        add(ConditionFlag.Always,false,dst,scratchRegister,0,0);

    }
    public void xorq(CiRegister dest,CiAddress src) {

        assert(dest.isValid() );
        setUpScratch(src); // scratchRegister now contains the value of the address
        // APN I'm not sure if I need to load the memory[valueofAddress] into scratch
        eor(ConditionFlag.Always,false,dest,dest,scratchRegister,0,0);

    }
    public void xorq(CiRegister dest, CiRegister src) {
        assert(dest.isValid());
        assert(src.isValid());
        eor(ConditionFlag.Always,false,dest,dest,src,0,0);
    }
    public void popq(CiAddress addr) {
        // APN presume we are popping off the stack?
        // addr could be a register, base index scale displacement --- handled
        // or a memory address constant  not handled right now -- not sure how it would be represented
        // as a CiAddress.
        // Placeholders not handled at the moment. .
        // REFACTOR to use the code  setUpScratch
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        assert (addr != CiAddress.Placeholder); // APN Placeholders not handled yet
        assert(base.isValid()); // APN can we have a memory address --- not handled yet?
        // APN simple case where we just have a register destination
        if (base.isValid()) {
            if(disp != 0) {
                mov32BitConstant(scratchRegister, disp);
                add(ConditionFlag.Always,false,scratchRegister,base,0,0);
            }
            if(index.isValid()){
                adclsl(ConditionFlag.Always,false,scratchRegister,scratchRegister,index,scale.log2); // APN even if scale is zero this is ok.
            }
            pop(ConditionFlag.Always, 1 << encode(scratchRegister));// r13 is the stack pointer

        }
    }
    public void pushq(CiAddress addr) {
    /*
        APN push a value specified by an CiAddress onto the stack r13.
        // Im assuming base cannot be destructively updated perhaps this is stupid and maybe DO NOT NEED
        to use the scratch register as defined in RegisterConfigs.java for the target as AMD64Assembler does not
        seem to use it ...
         */

        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;

        int disp = addr.displacement;

        /* APN not checking addressing modes right now
        TODO ... check them
        The effective address to be pushed is some combination of
        base+index*scale*displacement
         switch (format()) {
            case BASE            : return "[" + s(base) + "]";
            case BASE_DISP       : return "[" + s(base) + signed(displacement) + "]";
            case BASE_INDEX      : return "[" + s(base) + "+" + s(index) + "]";
            case BASE_INDEX_DISP : return "[" + s(base) + "+(" + s(index) + "*" + scale.value + ")" + signed(displacement) + "]";
            case PLACEHOLDER     : return "[<placeholder>]";
            default              : throw new IllegalArgumentException("unknown format: " + format());
        }

        Scale has a value of 1,2,4,8 ... Shift of Zero,1,2,3
        */
        assert(base.isValid()); // APN thinks it has to be valid or its an ERROR?
        // might not be the case if the addr is a PlaceHolder!

        assert (addr != CiAddress.Placeholder);
        /* TODO APN we will need to add code for this ... Placeholders are sentinel values that will
        be patched at a later point, once we see how/where they are patched then we will be able to
        make sensible decisions
         */

        // APN case that its just a valid register no index scale or displacement
        if(base.isValid() && (!index.isValid()) && scale.value == 1 && disp == 0) {
            // Base register is valid and stores an address

            push(ConditionFlag.Always, 1 << encode(base));// r13 is the stack pointer

        } else if (base.isValid()) {  // APN superfluous check, but base might be invalid once we sort out Placeholders
                if(disp != 0) {
                    // TODO EMIT AN INSTRUCTION TO DO BASE + DISPLACEMENT
                    // can we destructively update base to store the result?
                    // do we know the range of immediate values that might be produced?
                    // TODO try to do some tracing of Maxine and see what values come out of here.
                    // in the meantime we do it inefficiently but correctly for 32 bit displacements
                    mov32BitConstant(scratchRegister, disp);
                    add(ConditionFlag.Always,false,scratchRegister,base,0,0); //APN A8.8.5 ADD(immediate,ARM)

                }
                if(index.isValid()) {

                    adclsl(ConditionFlag.Always,false,scratchRegister,scratchRegister,index,scale.log2); // APN even if scale is zero this is ok.
                    // as a shift of zero will not affect the value.
                /*if(scale.value != 1)  {
                        // TODO emit an instruction to do
                        // instruction= base + indexRegister* scale
                        // NOTE scale can be 1,2,4 or 8 so we should be able to do
                        // this with a simple shift of 1,2 or 3 bits
                    }else {
                        // TODO emit an instruction to do
                        // instruction = base + indexRegister

                } */

                }

                    push(ConditionFlag.Always,1<<encode(scratchRegister)); // r13 is the stack pointer

        }

    }
    public final void ucomisd(CiRegister dst, CiRegister src) {
        assert dst.isFpu(); // will this work
        assert src.isFpu();
        // Assuming this is a single precision load
        //vcmp(ConditionFlag.Always,dst,fpScratch);
        // set FPSCR flags these need to be accessed using a VMRS to transfer them to arm flags
        assert(!dst.isFpu());// force a crash one way or another as this is notimplemented yet



    }
    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            nop(modulus - (codeBuffer.position() % modulus));
        }
    }

    public final void nop(int times) {
        assert(times > 0);
        for(int i = 0; i < times; i++)
            nop();
    }
    public final void nop() {
        movror(ConditionFlag.Always,false,ARMV7.r12,ARMV7.r12,0);
    }

    public final void ret()

    {

        movror(ConditionFlag.Always,false,ARMV7.r15,ARMV7.r14,0);
    }
    public final void ret(int imm16) {
        movw(ConditionFlag.Always,ARMV7.r0,imm16);
        ret();
    }
    public void enter(short imm16, byte imm8) {
        emitByte(0xC8);
        // appended:
        emitByte(imm16 & 0xff);
        imm16 >>= 8;
        emitByte(imm16 & 0xff);
        emitByte(imm8);
    }
    public void nullCheck(CiRegister r) {
        emitInt((0xe<<28)|(0x3<< 24)|(0x5<< 20)|(r.encoding << 16)|0); // sets condition flags
        //to see if equal to zero

    }
    public void membar()
    {
        emitInt((0xf<<28)|(0x5<< 24)|(0x7<<20)|(0xff05<<4)|(0xf));
    }
    public void enter(short imm16) {

        // stacksize = imm16
        // push frame pointer
        //framepointer = stackpointer
        // stackptr = framepointer -stacksize


        /*
        case 0xC8: //C8 ENTER (80186+)
    stacksize = getmem16(segregs[regcs], ip); StepIP(2);
    nestlev = getmem8(segregs[regcs], ip); StepIP(1);
    push(getreg16(regbp));
    frametemp = getreg16(regsp);
    //if (nestlev) {
      //  for (temp16=1; temp16<nestlev; temp16++) {
        //    putreg16(regbp, getreg16(regbp) - 2);
          //  push(getreg16(regbp));
       // }
       // push(getreg16(regsp));
   // }
    putreg16(regbp, frametemp);
    putreg16(regsp, getreg16(regbp) - stacksize);
    break;
        */
    }
    public final void jcc(ConditionFlag cc, int target, boolean forceDisp32) {
        // forceDisp32 seems to be true if its a forward branch
        // and false if its negative ... a backwards branch
        //int shortSize = 2;
        //int longSize = 6;
        /*
        APN ok we now need to decide if we can do a PC relative branch,
        with a signed immediate of 24 bits.
        0..   16777215
        down to -16777216
        Some worries about alignment ... but not going to worry right now
         */
        int disp = target - codeBuffer.position();
        if(disp <=16777215 && disp >= 16777216 && forceDisp32) {
            // we can do this in a single conditional branch
            emitInt((cc.value&0xf) << 28| (0xa << 24)| (disp&0xffffff));
        }else {
            // we need or have been instructed to do this as a 32 bit branch
            mov32BitConstant(scratchRegister, target);
            movror(ConditionFlag.Always,false,ARMV7.r15,scratchRegister,0); // UPDATE the PC to the target
        }

    }
    public final void jmp(int target, boolean forceDisp32) {

        int disp = target - codeBuffer.position();
        if(disp <=16777215 && disp >= 16777216 && forceDisp32) {
            // we can do this in a single conditional branch
            emitInt(((0xe) << 28)| (0xa << 24)| (disp&0xffffff));
        }else {
            mov32BitConstant(scratchRegister, target);
            movror(ConditionFlag.Always,false,ARMV7.r15,scratchRegister,0); // UPDATE the PC to the target
        }


    }
    public final void vmov () {
        emitInt(0xdeadbeef);
        System.err.println("vmov in ARMV7Assembler completely unimplemented");
    }

}


