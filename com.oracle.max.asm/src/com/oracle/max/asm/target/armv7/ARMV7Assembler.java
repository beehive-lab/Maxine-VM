package com.oracle.max.asm.target.armv7;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;

import com.oracle.max.asm.AbstractAssembler;
import com.oracle.max.asm.Label;
import com.sun.cri.ci.*;
import com.sun.cri.ri.RiRegisterConfig;

public class ARMV7Assembler extends AbstractAssembler {

    public final CiRegister frameRegister;
    public final CiRegister scratchRegister;
    public final RiRegisterConfig registerConfig;


    public ARMV7Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.registerConfig = registerConfig;
        this.scratchRegister = registerConfig == null ? ARMV7.r12 : registerConfig.getScratchRegister();
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
    }

    public enum ConditionFlag {
        Equal(0x0, "="), NotEqual(0x1, "!="), CarrySetUnsignedHigherEqual(0x2, "|carry|"), CarryClearUnsignedLower(0x3, "|ncarry|"), Minus(0x4, "|neg|"), Positive(0x5, "|pos|"), SignedOverflow(0x6, ".of."), NoSignedOverflow(0x7,
                        "|nof|"), UnsignedHigher(0x8, "|>|"), UnsignedLowerOrEqual(0x9, "|<=|"), SignedGreaterOrEqual(0xA, ".>=."), SignedLesser(0xB, ".<."), SignedGreater(0xC, ".>."), SignedLowerOrEqual(
                        0xD, ".<=."), Always(0xE, "al"), NeverUse (0xF,"NEVER");

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
            // branch(l.position(), false);
            checkConstraint(-0x800000 <= (l.position() - codeBuffer.position()) && (l.position() - codeBuffer.position()) <= 0x7fffff, "branch must be within  a 24bit offset");
            //emitInt(0x06000000 | (l.position() - codeBuffer.position()) | ConditionFlag.Always.value() & 0xf);
            emitInt(0x0a000000 | (l.position() - codeBuffer.position()) | ((ConditionFlag.Always.value() & 0xf) << 28));

        } else {
            // By default, forward jumps are always 24-bit displacements, since
            // we can't yet know where the label will be bound. If you're sure that
            // the forward jump will not run beyond 24-bits bytes, then its ok
            l.addPatchAt(codeBuffer.position());
            // emitByte(0xE9);
            // emitInt(0);
            nop();
        }
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        // b, bl & bx goes here .. could do an ADD PC,reg if too big
        //if(branch == 76) return; // hack for dcmp01 to see what happens
        checkConstraint(-0x800000 <= (target - branch) && (target - branch) <= 0x7fffff, "branch must be within  a 24bit offset");
        //emitInt(0x06000000 | (target - branch) | ConditionFlag.Always.value() & 0xf);
        int disp = target - branch - 16;
        int instruction = 0;
        int operation = codeBuffer.getInt(branch);
        if(operation == (ConditionFlag.NeverUse.value() << 28 | 0xdead)) { // JCC
           //
            //System.out.println("MATCHED JCC " + branch);
            disp -= 4;
            instruction = movwHelper(ConditionFlag.Always,ARMV7.r12,disp & 0xffff);
            codeBuffer.emitInt(instruction,branch);
            instruction = movtHelper(ConditionFlag.Always,ARMV7.r12,(disp >> 16) & 0xffff);
            codeBuffer.emitInt(instruction,branch+4);
        } else if (operation == (ConditionFlag.NeverUse.value() << 28 | 0xbeef)) { // JMP
            //disp += 8;
            disp +=8;

            disp = disp/4;
            if (disp < 0) {
                //System.out.println("NEGATIVE DISP " + disp);
            }
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28),branch);
            //System.out.println("MATCHED JMP branch " + branch + " DISP " + disp + " target "+ target );

        } else if ((operation & 0xf0000fff) == (ConditionFlag.NeverUse.value() << 28 | 0x0d0)) {
            disp = disp + 8;
            disp = disp/4;
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28),branch);

            /*code[callOffset + 7] = (byte) (instruction & 0xff);
            code[callOffset + 6] = (byte) ((instruction >> 8) & 0xff);
            code[callOffset + 5] = (byte) ((instruction >> 16) & 0xff);
            code[callOffset + 4] = (byte) ((instruction >> 24) & 0xff);
            */

        } else {

//System.out.println("ASSUMING JMP: check this came from an emitPrologue as a result of hosted mode ARM ..patchjumpTarget NOT MATCHED " + branch + " " + target);
                disp+=8;
                disp = disp/4;
                codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28),branch);
                //assert 0 == 1;
        }

        //codeBuffer.emitInt(0x0a000000 | (target - branch) | ((ConditionFlag.Always.value() & 0xf) << 28),branch);


    }

    public void addlsl(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x800000;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= Rm.encoding & 0xf;
        instruction |= (shift_imm & 0x1f) << 7;
        emitInt(instruction);
    }

    public void add(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x02800000;
        //System.out.println("MEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE\n");
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint((rotate_amount % 2) == 0, "(rotate_amount % 2) == 0");
        checkConstraint(0 <= rotate_amount / 2 && rotate_amount / 2 <= 15, "0 <= rotate_amount / 2 && rotate_amount / 2 <= 15");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= immed_8 & 0xff;
        instruction |= (rotate_amount / 2 & 0xf) << 8;
        emitInt(instruction);
    }

    public static int movRegistersHelper(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm) {
        int instruction = 0x01a00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        return instruction;
    }

    public static int addRegistersHelper(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm2Type, final int imm5) {
        int instruction = 0x00800000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (imm5 << 7) | (imm2Type << 5);
        instruction |= Rm.encoding & 0xf;
        return instruction;
    }

    public static int blxHelper(final ConditionFlag cond, final CiRegister Rm) {
        int instruction = 0x12FFF30;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= Rm.encoding & 0xf;
        return instruction;
    }

    public void addRegisters(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm2Type, final int imm5) {
        int instruction = 0x00800000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0 <= imm2Type && imm2Type <= 3");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (imm5 << 7) | (imm2Type << 5);
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);

    }

    public void addCRegisters(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm2Type, final int imm5) {
        int instruction = 0xA00000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0 <= imm2Type && imm2Type <= 3");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (imm5 << 7) | (imm2Type << 5);
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);

    }

    public void lsl(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final int imm5) {
        int instruction = 0x1A00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void lsl(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0x1A00010;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rm.encoding & 0xf) << 8;
        instruction |= Rn.encoding & 0xf;
        emitInt(instruction);
    }

    public void lsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final int imm5) {
        int instruction = 0x1A00020;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void lsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0x1A00030;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rm.encoding & 0xf) << 8;
        instruction |= Rn.encoding & 0xf;
        emitInt(instruction);
    }

    public void lusr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final int imm5) {
        int instruction = 0x1A00040;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void lusr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0x1A00050;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rm.encoding & 0xf) << 8;
        instruction |= Rn.encoding & 0xf;
        emitInt(instruction);
    }

    public void and(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm5, final int imm2) {
        int instruction = 0x0000000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2 & 0x3) << 5;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void and(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int imm12) {
        int instruction = 0x2000000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void eor(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm5, final int imm2) {
        int instruction = 0x00200000;
        // type ie imm2 refers to 00 LSL
        // 01 LSR
        // 10 ASR
        // 11 if imm5 == 00000 RRX, shift_n = 1
        // else ROR, shift_n = imm5; as a uint.
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2 & 0x3) << 5;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void orr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm5, final int imm2) {
        int instruction = 0x1800000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2 & 0x3) << 5;
        instruction |= Rm.encoding & 0xfff;
        emitInt(instruction);
    }

    public void orsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs,final int type) {
        int instruction = 0x1800010;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rs.encoding & 0xf) << 8;
        instruction |= (type & 0x3) << 5;
        instruction |= Rm.encoding & 0xfff;
        emitInt(instruction);
    }

    public void or(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int imm12) {
        int instruction = 0x3800000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm12 & 0xf;
        emitInt(instruction);
    }

    public void asr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final int imm5) {
        int instruction = 0x1A00040;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= Rm.encoding & 0xfff;
        emitInt(instruction);
    }

    public void asrr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0x1A00050;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rm.encoding & 0xf) << 8;
        instruction |= Rn.encoding & 0xf;
        emitInt(instruction);
    }

    public void movror(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm, final int shift_imm) {
        int instruction = 0x01A00060;
        checkConstraint(0 <= shift_imm && shift_imm <= 31, "0 <= shift_imm && shift_imm <= 31");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        instruction |= (shift_imm & 0x1f) << 7;
        emitInt(instruction);
    }

    public void mov(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rm) {
        int instruction = 0x01a00000;
        assert(Rd.encoding < 16 && Rm.encoding < 16); // CORE Register move only!
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void movt(final ConditionFlag cond, final CiRegister Rd, final int imm16) {
        int instruction = 0x03400000;
        checkConstraint(0 <= imm16 && imm16 <= 65535, "0<= imm16 && imm16 <= 65535 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        emitInt(instruction);
    }

    public static int movtHelper(final ConditionFlag cond, final CiRegister Rd, final int imm16) {
        int instruction = 0x03400000;
        // checkConstraint(0 <= imm16 && imm16 <= 65535, "0<= imm16 && imm16 <= 65535 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        return instruction;
    }

    public static int movwHelper(final ConditionFlag cond, final CiRegister Rd, final int imm16) {
        int instruction = 0x03000000;
        if(imm16 == 0xfe4c) {
            //System.out.println("DEBUG ME");
        }
        // checkConstraint(0 <= imm16 && imm16 <= 65535, "0<= imm16 && imm16 <= 65535 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        return instruction;
    }

    public void movw(final ConditionFlag cond, final CiRegister Rd, final int imm16) {
        int instruction = 0x03000000;
        if( imm16 == 0xfe4c) {
            //System.out.println("DEBUG ME movw");
        }
        checkConstraint(0 <= imm16 && imm16 <= 65535, "0<= imm16 && imm16 <= 65535 ");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        emitInt(instruction);
    }

    public void neg(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int imm12) {
        int instruction = 0x2600000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void nop(final ConditionFlag cond) {
        int instruction = 0x320F000;
        instruction |= (cond.value() & 0xf) << 28;
        emitInt(instruction);
    }

    public void sub(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x02400000; // subract of an immediate
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint(0 <= rotate_amount && rotate_amount <= 15, "0 <= rotate_amount && rotate_amount  <= 15");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= immed_8 & 0xff;
        instruction |= (rotate_amount / 2 & 0xf) << 8;
        emitInt(instruction);
    }

    public void rsb(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_8, final int rotate_amount) {
        int instruction = 0x2600000;
        checkConstraint(0 <= immed_8 && immed_8 <= 255, "0 <= immed_8 && immed_8 <= 255");
        checkConstraint(0 <= rotate_amount && rotate_amount <= 15, "0 <= rotate_amount && rotate_amount  <= 15");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= immed_8 & 0xff;
        instruction |= (rotate_amount / 2 & 0xf) << 8;
        emitInt(instruction);
    }

    public void rsc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm,final int immed_5, final int type) {
        int instruction = 0xE00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (immed_5 & 0x1f) << 7;
        instruction |= (type& 0x3) << 5;
        instruction |= (Rm .encoding& 0xf);
        emitInt(instruction);
    }

    public void rsc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final int immed_12) {
        int instruction = 0x2E00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (immed_12 & 0xfff);
        emitInt(instruction);
    }

    public void sbc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int immed_5, final int type) {
        int instruction = 0xC00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (immed_5 & 0x1f) << 7;
        instruction |= (type & 0x3) << 5;
        instruction |= (Rm.encoding & 0xf);
        emitInt(instruction);
    }

    public void sub(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int imm5, final int imm2Type) {
        int instruction = 0x00400000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0<= imm2Type && imm2Type <= 3");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= Rm.encoding & 0xf;
        instruction |= (imm2Type & 0x3) << 5;
        instruction |= (imm5 & 0x31) << 5;
        emitInt(instruction);
    }

    public void strd(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, final CiRegister Rm) {
        int instruction = 0x000000f0;
        instruction |= (P & 0x1) << 24;
        instruction |= (U & 0x1) << 23;
        instruction |= (W & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void str(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, final CiRegister Rm, int imm5, int imm2Type) {
        int instruction = 0x06000000;
        instruction |= (P & 0x1) << 24;
        instruction |= (U & 0x1) << 23;
        instruction |= (W & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2Type & 0x3) << 5;
        emitInt(instruction);
    }
    public static int  strHelper(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, final CiRegister Rm, int imm5, int imm2Type) {
        int instruction = 0x06000000;
        instruction |= (P & 0x1) << 24;
        instruction |= (U & 0x1) << 23;
        instruction |= (W & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (imm2Type & 0x3) << 5;
        return instruction;
    }

    public void strImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rvalue, final CiRegister Rmemory, int imm12) {
        int instruction = 0x04000000;
        assert imm12 == 0; // TODO fix the encoding its an ARM 12 bit
	assert Rvalue.encoding != Rmemory.encoding || !(P == 0 && U == 0 && W == 0);
        instruction |= (P & 0x1) << 24;
        instruction |= (U & 0x1) << 23;
        instruction |= (W & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rmemory.encoding & 0xf) << 16;
        instruction |= (Rvalue.encoding & 0xf) << 12;
        instruction |= imm12 & 0xfff;
        emitInt(instruction);
    }

    public void strDualImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm8) {
        int instruction = 0x040000f0;
        assert imm8 == 0; // TODO fix the encoding its an ARM 8 bit
        instruction |= (P & 0x1) << 24;
        instruction |= (U & 0x1) << 23;
        instruction |= (W & 0x1) << 21;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= imm8 & 0xf;
        instruction |= (imm8 & 0xf0) << 4;
        emitInt(instruction);
    }
  public void clz(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Rval) {
        int instruction = 0x016f0f10;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Rval.encoding & 0xf) );
        emitInt(instruction);
    }
    public void rbit(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Rval) {
        int instruction = 0x06ff0f30;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Rval.encoding & 0xf) );
        emitInt(instruction);
    }

    public void ldrex(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Raddr) {
        int instruction = 0x01900f9f;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Raddr.encoding & 0xf) << 16);
        emitInt(instruction);
    }

    public void ldrexd(final ConditionFlag cond, final CiRegister Rn, final CiRegister Rt) {
        int instruction = 0x1B00F9F;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rt.encoding & 0xf) << 12);
        emitInt(instruction);
    }

    public void strex(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Rnewval, final CiRegister Raddr) {
        int instruction = 0x01800f90;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Raddr.encoding & 0xf) << 16);
        instruction |= Rnewval.encoding & 0xf;
        emitInt(instruction);
    }

    public void strexd(final ConditionFlag cond, final CiRegister Rn, final CiRegister Rd, final CiRegister Rt) {
        int instruction = 0x1A00F90;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= Rt.encoding & 0xf;
        emitInt(instruction);
    }

    public void ldrshw(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm8) {
        int instruction = 0x005000f0;
        P = P & 1;
        U = U & 1;
        if (imm8 < 0) {
            U = 0;
            imm8 = imm8 * -1;
        }
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= 0xf0 | (imm8 & 0xf) | ((0xf0 & imm8) << 4);
        emitInt(instruction);
    }

    public void ldrsb(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm8) {
        int instruction = 0x005000d0;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= 0xf & imm8;
        instruction |= (imm8 >> 4) << 8;
        emitInt(instruction);
    }

    public void ldrb(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm12) {
        int instruction = 0x04500000;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= 0xfff & imm12;
        emitInt(instruction);
    }
    public void strbImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm12) {
        int instruction  = 0x04400000;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= 0xfff & imm12;
        emitInt(instruction);
    }
    public void strHImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm8) {
        int instruction  = 0x004000b0;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= 0xf & imm8;
        instruction |= ((imm8 &0xff) >> 4) << 8;
        emitInt(instruction);
    }
    public void ldrImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm12) {
        int instruction = 0x04100000;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= imm12;
        emitInt(instruction);
    }

    public void ldr(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, final CiRegister Rm, int imm2Type, int imm5) {
        int instruction = 0x06100000;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        instruction |= (imm2Type & 0x3) << 5;
        instruction |= (imm5 & 0x1f) << 7;
        emitInt(instruction);
    }

    // TODO: Finalize this
    public void movss(final ConditionFlag cond, int P, int U, int W, final CiRegister Rn, final CiRegister Rt, final CiRegister Rm, int imm2Type, int imm5) { // move
        assert (Rt.number < 16);
        if (Rn.number < 16) {
            ldr(cond, P, U, W, Rn, Rt, Rm, imm2Type, imm5);
        } else {
            //assert (Rn.number > 31);
	    if(Rn.number <= 31) {
			//System.out.println("movss assert removed ...attempting  vldr to a double not a single ... probably the hack to manipulate float/double registers has not been fixed in all places\n");
	    }
            vldr(cond, Rn, Rt, 0);
        }
    }

    // TODO: Finalize this
    public void movsd(final ConditionFlag cond, int P, int U, int W, final CiRegister Rn, final CiRegister Rt, final CiRegister Rm) {
        if (Rn.number < 16) {
            ldrd(cond, P, U, W, Rn, Rt, Rm);
        } else {
            assert (Rn.number < 32);
            vldr(cond, Rn, Rt, 0);
        }
    }

    public void ldrd(final ConditionFlag cond, int P, int U, int W, final CiRegister Rn, final CiRegister Rt, final CiRegister Rm) {
        int instruction = 0x000000d0;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void swi(final ConditionFlag cond, final int immed_24) {
        int instruction = 0x0F000000;
        checkConstraint(0 <= immed_24 && immed_24 <= 16777215, "0 <= immed_24 && immed_24 <= 16777215");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= immed_24 & 0xffffff;
        emitInt(instruction);
    }

    public void push(final ConditionFlag flag, final int registerList) {
        int instruction;
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x9 << 24;
        instruction |= 0x2 << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void pop(final ConditionFlag flag, final int registerList) {
        int instruction;
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x8 << 24;
        instruction |= 0xb << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void ldrd(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseReg, int offset8) {
        int instruction;
        int P;
        int U;
        int W;
        instruction = 0x004000d0;
        checkConstraint(-255 <= offset8 && offset8 <= 255, "-255 <= offset8 && offset8 <= 255");
        if (offset8 < 0) {
            U = 0;
            offset8 *= -1;
        } else {
            U = 1;
        }
        P = 1;
        W = 0;
        checkConstraint(valueReg.encoding % 2 == 0, "ldrd register must be even");
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= P << 24;
        instruction |= U << 23;
        instruction |= W << 21;
        instruction |= (valueReg.encoding & 0xf) << 12;
        instruction |= (baseReg.encoding & 0xf) << 16;
        instruction |= (offset8 & 0xf0) << 4;
        instruction |= offset8 & 0xf;
        emitInt(instruction);
    }

    public void strd(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseReg, int offset8) {
        int instruction;
        instruction = 0x004000f0;
        int P;
        int U;
        int W;
        checkConstraint(valueReg.encoding % 2 == 0, "strd register must be even");
        checkConstraint(-255 <= offset8 && offset8 <= 255, "-255 <= offset8 && offset8 <= 255");
        if (offset8 < 0) {
            U = 0;
            offset8 *= -1;
        } else {
            U = 1;
        }
        P = 1;
        W = 0;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= P << 24;
        instruction |= U << 23;
        instruction |= W << 21;
        instruction |= (valueReg.encoding & 0xf) << 12;
        instruction |= (baseReg.encoding & 0xf) << 16;
        instruction |= (offset8 & 0xf0) << 4;
        instruction |= offset8 & 0xf;
        emitInt(instruction);
    }

    public void str(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseRegister, final int offset12) {
        int instruction;
        instruction = 0x05800000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (valueReg.encoding & 0xf) << 12;
        instruction |= (baseRegister.encoding & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);
    }

    public void ldr(final ConditionFlag flag, final CiRegister destReg, final CiRegister baseRegister, final int offset12) {
        /*
        P,U,W are set to always add at the moment,
        P = 1, U = 1, W = 0

         */
        int instruction;
        instruction = 0x05900000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (destReg.encoding & 0xf) << 12;
        instruction |= (baseRegister.encoding & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);
    }

    public void cmp(final ConditionFlag flag, final CiRegister Rn, final CiRegister Rm, int imm5, int imm2Type) {
        int instruction = 0x01500000;
        checkConstraint(0 <= imm5 && imm5 <= 31, "0 <= imm5 && imm5 <= 31");
        checkConstraint(0 <= imm2Type && imm2Type <= 3, "0 <= imm2Type && imm2Type <= 3");
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= imm5 << 7;
        instruction |= imm2Type << 5;
        instruction |= (Rm.encoding & 0xf);
        emitInt(instruction);
    }

    protected void checkConstraint(boolean passed, String expression) {
        if (!passed) {
            throw new IllegalArgumentException(expression);
        }
    }

    private static int encode(CiRegister r) {
        assert r.encoding < 16 && r.encoding >= 0 : "encoding out of range: " + r.encoding;
        return r.encoding;
    }

    /**
     * APN The methods below here are largely to interface the ARMV7Assembler to the ARMV7MAcroAssembler which is based
     * on the X86 version in the longer term we probably want a more natural encoding/fit to ARM elsewhere in Maxine and
     * then to refactor but right now the priority is to get the port working. movl in the AMD assembler has complex
     * semantics, it might be a constant it might be memory location ..... movl is being replaced into mov32BitConstant
     * and other yet to be implemented instruction aspects in order to disambiguate the desired operation from the
     * purpose
     */
    public void setUpScratch(CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(numInstructions(addr)); // 4 instructions, 2 for mov32, 1 for add and 1 for addclsl
            return;
        }

        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0 || base.compareTo(CiRegister.CallerFrame) == 0;

        //}
        // APN can we have a memory address --- not handled yet?
        // APN simple case where we just have a register destination
        // TODO fix this so it will issue loads when appropriate!

        if (base.isValid() || base.compareTo(CiRegister.Frame) == 0) {
            if(base == CiRegister.Frame) {
                base = frameRegister;
            }
            if (disp != 0) {
                mov32BitConstant(scratchRegister, disp);
                addRegisters(ConditionFlag.Always, false, scratchRegister, scratchRegister, base, 0, 0);
                if (index.isValid()) {
                    addlsl(ConditionFlag.Always, false, scratchRegister, scratchRegister, index, scale.log2);
                }
            } else {
                if (index.isValid()) {
                    addlsl(ConditionFlag.Always, false, scratchRegister, base, index, scale.log2);
                } else {
                    mov(ConditionFlag.Always, false, scratchRegister, base);
                }
            }
        } else {
            base = frameRegister;
            mov32BitConstant(scratchRegister, addr.kind.isLong() ? disp + 4 : disp);
            sub(ConditionFlag.Always, false, scratchRegister, ARMV7.r11, scratchRegister, 0, 0);
            if (index.isValid()) {
                addlsl(ConditionFlag.Always, false, scratchRegister, scratchRegister, index, scale.log2);
            }
        }
    }

    public void setUpRegister(CiRegister dest, CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(numInstructions(addr)); // 4 instructions, 2 for mov32, 1 for add and 1 for addclsl
            return;
        }
        assert base.isValid();
        // APN can we have a memory address --- not handled yet?
        // APN simple case where we just have a register destination
        // TODO fix this so it will issue loads when appropriate!
        if (base.isValid()) {
            if (disp != 0) {
                mov32BitConstant(dest, disp);
                addRegisters(ConditionFlag.Always, false, dest, dest, base, 0, 0);
                if (index.isValid()) {
                    addlsl(ConditionFlag.Always, false, dest, dest, index, scale.log2);
                }
            } else {
                if (index.isValid()) {
                    addlsl(ConditionFlag.Always, false, dest, base, index, scale.log2);
                } else {
                    mov(ConditionFlag.Always, false, dest, base);
                }
            }
        }
    }

    private int numInstructions(CiAddress addr) {
        CiRegister index = addr.index();
        int disp = addr.displacement;
        if (disp != 0) {
            if (index.isValid()) {
                return 4;
            } else {
                return 3;
            }
        } else {
            // APN modification for CompilerStubEmitter ...
            // movw movt is two instructions ....
            // TODO sometimes this might be required for a long?
            // see ARMV7CompilerStubEmitter:: emit FNEG ...
            return 2;
        }
    }

    public final void decq(CiRegister dst) {
        assert dst.isValid();
        sub(ConditionFlag.Always, false, dst, dst, 1, 0);
    }

    public final void subq(CiRegister dst, int imm32) {
        assert dst.isValid();
        mov32BitConstant(scratchRegister, imm32);
        sub(ConditionFlag.Always, false, dst, dst, scratchRegister, 0, 0);
    }

    public final void mov32BitConstant(CiRegister dst, int imm32) {
        if(dst.number < 16) {
            movw(ConditionFlag.Always, dst, imm32 & 0xffff);
            imm32 = imm32 >> 16;
            imm32 = imm32 & 0xffff;
            movt(ConditionFlag.Always, dst, imm32 & 0xffff);
        }else { // initialise a float with a constant
            mov32BitConstant(ARMV7.r12,imm32);
            vmov(ConditionFlag.Always,dst,ARMV7.r12);
        }
    }
    public final void vsqrt(ConditionFlag cond,CiRegister dst, CiRegister src) {
        if((src.number > 15 && src.number < 32 && dst.number >15 && dst.number < 32) ||
          (src.number > 32 && dst.number >32)) {
             int instruction = 0x0eb10ad0;
             instruction |= (cond.value() << 28);
             int dp = (src.number < 32) ? 1 : 0;
             instruction |= dp << 8;
             int dest = dst.encoding;
	     int srcr = src.encoding;
             instruction |= dp << 8;

	     if(dp == 1) {
                 instruction |= srcr;
                 instruction |= dest << 12;
             } else {
                 instruction |= srcr >> 1;
                 instruction |= (srcr & 1) << 5;
		 instruction |= (dest >> 1) <<  12;
                 instruction |= (dest & 1) << 22;
             }


            emitInt(instruction);

        } else {
		assert 0 == 1 : "ERROR vsqrt illegal register combination";
        }



    }
    public final void mov16BitConstant(ConditionFlag cond, CiRegister dst, int imm16) {
        movw(cond, dst, imm16);
    }

    public final void mov64BitConstant(CiRegister dstUpper, CiRegister dstLow, long imm64) {
        int low32 = (int) (imm64 & 0xffffffff);
        int high32 = (int) ((imm64 >> 32) & 0xffffffff);
        mov32BitConstant(dstLow, low32);
        mov32BitConstant(dstUpper, high32);
    }

    public final void alignForPatchableDirectCall() {
        /*
         * APN as far as I am aware there are no alignment restrictions. seems to be an interaction with Safepoints ---
         * not tested but nops are based on movs which are tested, included for compatibility with X86.
         */
        int dispStart = codeBuffer.position() + 1;
        int mask = target.wordSize - 1;
        if ((dispStart & ~mask) != ((dispStart + 3) & ~mask)) {
            nop(target.wordSize - (dispStart & mask));
        }
    }

    public final void call() {
        // ok we do not have the same semantics as intel
        // this is used for a call where we don't know the actual target when we insert it
        // ie for a trampoline.
        // APN proposes we use the scratch register to calculate an address then we do the mov pc
        // looking at Stubs.java we can see that all registers have been saved
        // so we can use whatever registers we want!
        // emitInt(0); // space for setupscratch
        // emitInt(0);
        nop(4);
        // Target needs to be patched later ...
    }

    public final void call(CiRegister target) {
        // TODO APN believes that Adapters that do the necessary prologue/epilogue
        // to save / restore state ....
        ldrImmediate(ConditionFlag.Always, 0, 0, 0, ARMV7.r12, ARMV7.r12, 0); //
        push(ConditionFlag.Always, 1 << 15); // PUSH the PC onto the stack
        // THIs is an indirect call, assuming the contents of the registers are a memory location we need to load
        add(ConditionFlag.Always, false, ARMV7.r15, ARMV7.r12, 0, 0);
    }

    public final void leaq(CiRegister dest, CiAddress addr) {
        if (addr == CiAddress.Placeholder) {
            nop(4);
        } else {
            setUpScratch(addr);
            mov(ConditionFlag.Always, false, dest, ARMV7.r12);
        }
    }

    public final void leave() {
        mov(ConditionFlag.Always, false, ARMV7.r13, ARMV7.r11); // restore SP that is in the FP
        pop(ConditionFlag.Always, 1 << 11); // POP the old FP r11 off the stack.

    }

    public final void movslq(CiAddress dst, int imm32) {
        // TODO APN ok Im assuming this is just as simple mov rather than an actual sign extend?
        // it might be used in 64 bit mode, but ARMV7 is only 32 bit anyway.
        // if it transpires that this is necessary for 64bit values in a 32bit processor
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r8, imm32);
        str(ConditionFlag.Always,ARMV7.r8,ARMV7.r12,0);
    }

    public final void cmpl(CiRegister src, int imm32) {
        assert src.isValid();
        mov32BitConstant(scratchRegister, imm32);
        cmp(ConditionFlag.Always, src, scratchRegister, 0, 0);

    }

    public final void cmpl(CiRegister src1, CiAddress src2) {
        assert src1.isValid();
        setUpScratch(src2); // APN not sure if this requires a load!
        ldr(ConditionFlag.Always,ARMV7.r12,ARMV7.r12,0);
        cmp(ConditionFlag.Always, src1, scratchRegister, 0, 0);
    }

    public final void cmpl(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public final void lcmpl(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
        sbc(ConditionFlag.Always, true, scratchRegister,  registerConfig.getAllocatableRegisters()[src1.number + 1],  registerConfig.getAllocatableRegisters()[src2.number + 1], 0, 0);
        mov16BitConstant(ConditionFlag.SignedGreaterOrEqual, registerConfig.getReturnRegister(CiKind.Int), 0);
        mov16BitConstant(ConditionFlag.SignedLesser, registerConfig.getReturnRegister(CiKind.Int), 1);
    }

    public final void incq(CiRegister dst) {
        assert dst.isValid();
        add(ConditionFlag.Always, false, dst, dst, 1, 0);
    }

    public final void addq(CiRegister dst, int imm32) {
        assert dst.isValid();
        mov32BitConstant(scratchRegister, imm32);
        addRegisters(ConditionFlag.Always, false, dst, dst, scratchRegister, 0, 0);
    }

    public final void addLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        addRegisters(ConditionFlag.Always, true, dst, src1, src2, 0, 0);
        addCRegisters(ConditionFlag.Always, false, registerConfig.getAllocatableRegisters()[dst.number + 1], registerConfig.getAllocatableRegisters()[src1.number + 1],
                        registerConfig.getAllocatableRegisters()[src2.number + 1], 0, 0);
    }

    public final void subLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        sub(ConditionFlag.Always, true, dst, src1, src2, 0, 0);
        sbc(ConditionFlag.Always, false, registerConfig.getAllocatableRegisters()[dst.number + 1], registerConfig.getAllocatableRegisters()[src1.number + 1],
                        registerConfig.getAllocatableRegisters()[src2.number + 1], 0, 0);
    }

    public final void mulLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        mov(ConditionFlag.Always, false, registerConfig.getAllocatableRegisters()[scratchRegister.number+1], registerConfig.getAllocatableRegisters()[src2.number]);
        mul(ConditionFlag.Always, false, src2, src2, registerConfig.getAllocatableRegisters()[src1.number + 1]);
        mul(ConditionFlag.Always, false, registerConfig.getAllocatableRegisters()[src2.number + 1], src1, registerConfig.getAllocatableRegisters()[src2.number + 1]);
        addRegisters(ConditionFlag.Always, false, scratchRegister, src2, registerConfig.getAllocatableRegisters()[src2.number+1], 0, 0);
        umull(ConditionFlag.Always, false, registerConfig.getAllocatableRegisters()[src2.number], dst,
                        registerConfig.getAllocatableRegisters()[scratchRegister.number+1], src1);

        addRegisters(ConditionFlag.Always, false, scratchRegister, scratchRegister,
                        src2, 0, 0);
        mov(ConditionFlag.Always, false, registerConfig.getAllocatableRegisters()[dst.number+1], scratchRegister);

    }

    public void xorq(CiRegister dest, CiAddress src) {
        assert dest.isValid();
        setUpScratch(src);
        // scratchRegister now contains the value of the address
        // APN I'm not sure if I need to load the memory[valueofAddress] into scratch
        eor(ConditionFlag.Always, false, dest, dest, scratchRegister, 0, 0);
    }

    public void xorq(CiRegister dest, CiRegister src) {
        assert dest.isValid();
        assert src.isValid();
        eor(ConditionFlag.Always, false, dest, dest, src, 0, 0);
    }

    public void popq(CiAddress addr) {
        // APN presume we are popping off the stack?
        // addr could be a register, base index scale displacement --- handled
        // or a memory address constant not handled right now -- not sure how it would be represented
        // as a CiAddress.
        // Placeholders not handled at the moment. .
        // REFACTOR to use the code setUpScratch
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        assert addr != CiAddress.Placeholder;
        if (base == CiRegister.Frame || base == CiRegister.CallerFrame) {
            assert frameRegister != null : "cannot use register " + CiRegister.Frame + " in assembler with null register configuration";
            base = frameRegister;
        }
        assert base.isValid();

	//System.out.println("popq assert base.isValid() removed to allow boot image compile");
        // APN can we have a memory address --- not handled yet?
        // APN simple case where we just have a register destination
        if (base.isValid()) {
            if (disp != 0) {
                mov32BitConstant(scratchRegister, disp);
                add(ConditionFlag.Always, false, scratchRegister, base, 0, 0);
            }
            if (index.isValid()) {
                addlsl(ConditionFlag.Always, false, scratchRegister, scratchRegister, index, scale.log2);
            }
            pop(ConditionFlag.Always, 1 << encode(scratchRegister));
        }
    }

    public void pushq(CiAddress addr) {
        /*
         * APN push a value specified by an CiAddress onto the stack r13. // Im assuming base cannot be destructively
         * updated perhaps this is stupid and maybe DO NOT NEED to use the scratch register as defined in
         * RegisterConfigs.java for the target as AMD64Assembler does not seem to use it ... TODO are we really moving
         * the value stored at an address onto the stack or just the address!!
         */
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        if (base == CiRegister.Frame || base == CiRegister.CallerFrame) {
            assert frameRegister != null : "cannot use register " + CiRegister.Frame + " in assembler with null register configuration";
            base = frameRegister;
        }

        assert base.isValid();
	//System.out.println("assert base.isValid() removed pushq for attempt at image compilation");
        // APN thinks it has to be valid or its an ERROR?
        // might not be the case if the addr is a PlaceHolder!
        assert addr != CiAddress.Placeholder;
        /*
         * TODO APN we will need to add code for this ... Placeholders are sentinel values that will be patched at a
         * later point, once we see how/where they are patched then we will be able to make sensible decisions
         */

        // APN case that its just a valid register no index scale or displacement
        if (base.isValid() && (!index.isValid()) && scale.value == 1 && disp == 0) {
            // Base register is valid and stores an address
            push(ConditionFlag.Always, 1 << encode(base)); // r13 is the stack pointer
        } else if (base.isValid()) { // APN superfluous check, but base might be invalid once we sort out Placeholders
            if (disp != 0) {
                // TODO EMIT AN INSTRUCTION TO DO BASE + DISPLACEMENT
                // can we destructively update base to store the result?
                // do we know the range of immediate values that might be produced?
                // TODO try to do some tracing of Maxine and see what values come out of here.
                // in the meantime we do it inefficiently but correctly for 32 bit displacements
                mov32BitConstant(scratchRegister, disp);
                add(ConditionFlag.Always, false, scratchRegister, base, 0, 0); // APN A8.8.5 ADD(immediate,ARM)
            }
            if (index.isValid()) {
                addlsl(ConditionFlag.Always, false, scratchRegister, scratchRegister, index, scale.log2);
                // APN even if scale is zero this is ok.
                // as a shift of zero will not affect the value.
                /*
                 * if(scale.value != 1) { // TODO emit an instruction to do // instruction= base + indexRegister* scale
                 * // NOTE scale can be 1,2,4 or 8 so we should be able to do // this with a simple shift of 1,2 or 3
                 * bits }else { // TODO emit an instruction to do // instruction = base + indexRegister
                 *
                 * }
                 */
            }
            push(ConditionFlag.Always, 1 << encode(scratchRegister)); // r13 is the stack pointer
        }
    }

    public final void vcmpZERO(boolean nanExceptions, ConditionFlag cond, CiRegister src1) {
        int instruction = 0x0eb50a40;
        int dpOperation = 0;
        int quietNAN = 1;

        if (nanExceptions) {
            quietNAN = 0;
        }
        assert (src1.number > 15);
        if (src1.number < 32) {
            dpOperation = 1;
            instruction |= (src1.encoding << 12);
        } else {
            instruction |= (src1.encoding & 0x1) << 22;
            instruction |= (src1.encoding >> 1) << 12;
        }
        instruction |= (cond.value() << 28);
        instruction |= (dpOperation << 8);
        instruction |= (quietNAN << 7);

        emitInt(instruction);
    }

    public final void vcmp(boolean nanExceptions, ConditionFlag cond, CiRegister src1, CiRegister src2) {
        int instruction = 0x0eb40a40;
        int dpOperation = 0;
        int quietNAN = 1;

        if (nanExceptions) {
            quietNAN = 0;
        }

        assert src1.number > 15 && src2.number > 15;
        if (src1.number < 32) {
            assert (src2.number < 32);
            dpOperation = 1;
            instruction |= (src1.encoding << 12);
            instruction |= (src2.encoding);
        } else {
            assert (src2.number >= 32);
            instruction |= (src1.encoding >> 1) << 12;
            instruction |= (src1.encoding & 0x1) << 22;
            instruction |= (src2.encoding >> 1);
            instruction |= (src2.encoding & 0x1) << 5;
        }

        instruction |= (cond.value() << 28);
        instruction |= (dpOperation << 8);
        instruction |= (quietNAN << 7);
        emitInt(instruction);
    }

    public final void ucomisd(CiRegister dst, CiRegister src) {
        assert dst.isFpu(); // will this work
        assert src.isFpu();
        assert ((src.number > 15 && dst.number > 15) || ( src.number > 31 && dst.number > 31)); // Either FP FP or DP DP compare
        // Assuming this is a single precision load
        vcmp(true,ConditionFlag.Always,dst,src);
        // set FPSCR flags these need to be accessed using a VMRS to transfer them to arm flags
        // assert !dst.isFpu(); // force a crash one way or another as this is notimplemented yet
        vmrs(ConditionFlag.Always,ARMV7.r15);

    }

    public void align(int modulus) {
        // Is alignment relevant at all for ARM
        // we do not have the same restrictions as X86.
        if (codeBuffer.position() % modulus != 0) {
            nop(modulus - (codeBuffer.position() % modulus));
        }
    }

    public final void nop() {
        nop(1);
    }
    public final void int3() {
        //System.out.println("MISSING int3");
	nop(4);
    }
    public final void hlt() {
        //System.out.println("MISSING hlt");
	nop(4);
    }
    public final void nop(int times) {
        assert times > 0;
        for (int i = 0; i < times; i++) {
            nop(ConditionFlag.Always);
        }
    }

    public final void ret() {
        // TODO ret() implements an X86 return from subroutine this needs to pop the return value of the stack TODO we
        // might need to push the value of r14 onto the stack in order to make this work for a call from the C harness
        // TODO for testing of the methods
        mov(ConditionFlag.Always, false, ARMV7.r15, ARMV7.r14);

    }

    public final void ret(int imm16) {
        if (imm16 == 0) {
            ret();
        } else {
            // System.out.println("Need to ascertain purpose of ARMV7Assembler::ret(imm16)");
            addq(ARMV7.r13, imm16); // believe it is used to retract the stack
            ret();

        }
    }

    public void enter(short imm16, byte imm8) {
        assert false;
        /*
         * APN this does a push of the FP, moves the SP onto -> r11 subs the SP by imm16 .... DONT KNOW WHAT THE IMM8 IS
         * FOR
         */
        push(ConditionFlag.Always, 1 << 11); // push the FP.
        mov(ConditionFlag.Always, false, ARMV7.r11, ARMV7.r13); // move the SP onto the FP
        mov32BitConstant(ARMV7.r12, imm16);
        sub(ConditionFlag.Always, false, ARMV7.r13, ARMV7.r13, ARMV7.r12, 0, 0);
    }
    public final void lock() {
	    nop();
    }

    public void nullCheck(CiRegister r) {
        emitInt((0xe << 28) | (0x3 << 24) | (0x5 << 20) | (r.encoding << 16) | 0); // sets condition flags
    }

    public void membar(int barriers) {
        if (target.isMP) {
            // We only have to handle StoreLoad
            if (barriers == -1 || ((barriers & STORE_LOAD) != 0)) {
                emitInt((0xf << 28) | (0x5 << 24) | (0x7 << 20) | (0xff05 << 4) | 0xf);
            }
        }
    }

    public void enter(short imm16) {
    }

    public final void jcc(ConditionFlag cc, int target, boolean forceDisp32) {
        int disp = (target - codeBuffer.position());
        if (disp <= 16777215 && !forceDisp32) { // TODO check ok to make this false
            disp = (disp / 4) - 2;
            emitInt((cc.value & 0xf) << 28 | (0xa << 24) | (disp & 0xffffff));
        } else {
            if (disp > 0) {
                disp -= 16;
            }
            mov32BitConstant(scratchRegister, disp);
            addRegisters(ConditionFlag.Always, false, scratchRegister, ARMV7.r15, scratchRegister, 0, 0);
            mov(cc, false, ARMV7.r15, scratchRegister);
        }
    }

    public final void jcc(ConditionFlag cc, Label l) {
        assert (0 <= cc.value) && (cc.value < 16) : "illegal cc";
        if (l.isBound()) {
            //System.out.println("LABEL bound jcc no need to patch");
            jcc(cc, l.position(), false);
        } else {
            // Note: could eliminate cond. jumps to this jump if condition
            // is the same however, seems to be rather unlikely case.
            // Note: use jccb() if label to be bound is very close to get
            // an 8-bit displacement
            l.addPatchAt(codeBuffer.position());
            //System.out.println("ADDED JCC PATCH AT" + codeBuffer.position());
            emitInt(ConditionFlag.NeverUse.value() << 28 | 0xdead ); // JCC CODE for the PATCH
            nop(2);
            // TODO issues exist here ... what happens if R12 is loaded twice?
            // TODO or used as scratch inbetween the setup of its value and
            // this point?
            // TODO decide how to distinguish this from other patches
            // TODO update wiki on this
            //ldr(ConditionFlag.Always,0,0,0,ARMV7.r12,ARMV7.r12,ARMV7.r12,0,0);
            addRegisters(cc, false, ARMV7.r15, ARMV7.r12, ARMV7.r15, 0, 0);
        }
    }

    public final void jmp(CiRegister absoluteAddress) {
        bx(ConditionFlag.Always, absoluteAddress);
    }

    public final void bx(ConditionFlag cond, CiRegister target) {
        int instruction = 0x012fff10;
        instruction |= (cond.value() << 28);
        instruction |= target.encoding;
        emitInt(instruction);
    }

    public final void jmp(Label l) {
        if (l.isBound()) {
            jmp(l.position(), false);
        } else {
            // By default, forward jumps are always 32-bit displacements, since
            // we can't yet know where the label will be bound. If you're sure that
            // the forward jump will not run beyond 256 bytes, use jmpb to
            // force an 8-bit displacement.

            l.addPatchAt(codeBuffer.position());
            emitInt(ConditionFlag.NeverUse.value() << 28 | 0xbeef); // JMP CODE for the PATCH
            // TODO fix this it will not work ....
            nop(1);
            // emitByte(0xE9);
            // emitInt(0);
        }
    }

    public final void jmp(int target, boolean forceDisp32) {
        int disp = target - codeBuffer.position();
        if (disp <= 16777215 && forceDisp32) {
            disp = (disp / 4) - 2;
            emitInt((0xe << 28) | (0xa << 24) | (disp & 0xffffff));
        } else {
            if (disp > 0)
                disp -= 16;
            mov32BitConstant(scratchRegister, disp);
            addRegisters(ConditionFlag.Always, false, scratchRegister, ARMV7.r15, scratchRegister, 0, 0);
            mov(ConditionFlag.Always, false, ARMV7.r15, scratchRegister); // UPDATE the PC to the target
        }
    }

    public final void vmrs(ConditionFlag cond, CiRegister dest) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0ef10a10;
        instruction |= dest.encoding << 12;
        emitInt(instruction);
        }

    public final void vmul(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e200a00;
        int sz = 0;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vmul ALL  FP/DP regs");
        checkConstraint((dest.number <= 31 && rn.number <= 31 && rm.number <= 31) || (dest.number >= 32 && rn.number >= 32 && rm.number >= 32), "vmul ALL  FP OR ALL DP regs");
        if (dest.number <= 31) {
            sz = 1;
        }
        if (sz == 1) { // bit rest of bits
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= rm.encoding & 0xf;
            instruction |= (dest.encoding >> 4) << 22;
            instruction |= (rn.encoding >> 4) << 7;
            instruction |= (rm.encoding >> 4) << 5;

        } else {
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= rm.encoding >> 1;
            instruction |= (dest.encoding & 0x1) << 22;
            instruction |= (rn.encoding & 0x1) << 7;
            instruction |= (rm.encoding & 0x1) << 5;

        }
        instruction |= sz << 8;
        emitInt(instruction);
    }

    public final void vcvt(ConditionFlag cond, CiRegister dest, boolean toInt, boolean signed, CiRegister src) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0eb80a40;
        int sz = 0;
        int op = 0;
        int opc2;
        boolean double2Float = false;
        boolean floatConversion = false;
        //System.out.println("VCVT " + dest.number + " " + src.number);
        if (toInt == false && signed == false) {
            checkConstraint(dest.number >= 16 && src.number >= 16, "vcvt must be FP/DP regs");
            if(dest.number > src.number ) {
                double2Float = true;
            }
            floatConversion = true;
            checkConstraint(!(dest.number <= 31 && src.number <= 31), "vcvt one reg mus be FP another DP");
            checkConstraint(!(dest.number >= 32 && src.number >= 32), "vcvt one reg mus be FP another DP");
        }
        if (dest.number <= 31 || src.number <= 31) {
            sz = 1;
        }
        if (signed) {
            if (toInt) {
                opc2 = 5;
            } else {
                opc2 = 0;
                op = 1;
            }
        } else {
            if (toInt) {
                opc2 = 4;
            } else {
                opc2 = 0;
                op = 0;
            }
        }
        if(!floatConversion) {
            if (toInt) {
                instruction |= (dest.encoding >> 1) << 12; // LSB in bit 22
                instruction |= (dest.encoding & 0x1) << 22;
                if (sz == 1) {
                    instruction |= src.encoding & 0xf; //
                    instruction |= (src.encoding >> 4) << 5;

                } else {
                    instruction |= src.encoding >> 1;
                    instruction |= (src.encoding & 0x1) << 5;
                }
            } else {
                instruction |= src.encoding >> 1;
                instruction |= (src.encoding & 0x1) << 5;
                if (sz == 0) {
                    instruction |= (dest.encoding >> 1) << 12;
                    instruction |= (dest.encoding & 0x1) << 22;
                } else {
                    instruction |= (dest.encoding & 0xf) << 12;
                    instruction |= (dest.encoding >> 4) << 22;
                }
            }
            instruction |= opc2 << 16;
            instruction |= op << 7;
            instruction |= sz << 8;
        } else {
            instruction = cond.value << 28;
            instruction |= 0xeb70ac0;
            if(double2Float) {
                instruction |= 1<<8;
                // d is dest and dest is FLOAT SINGLE

                instruction |= (dest.encoding >>1) << 12;
                instruction |= (dest.encoding & 1) << 22;

                instruction |= (src.encoding & 0xf);
                instruction |= (src.encoding >> 4) << 5;

            } else {
                instruction |= (dest.encoding & 0xf) << 12;
                instruction |= (dest.encoding >> 4) << 22;

                instruction |= (src.encoding & 1) << 5;
                instruction |= (src.encoding >>1);

            }
        }
        emitInt(instruction);
        /*
         * VCVT{R}{<c>}{<q>}.S32.F64 <Sd>, <Dm> Encoded as opc2 = 0b101, sz = 1
         *
         * VCVT{R}{<c>}{<q>}.S32.F32 <Sd>, <Sm> Encoded as opc2 = 0b101, sz = 0
         *
         * VCVT{R}{<c>}{<q>}.U32.F64 <Sd>, <Dm> Encoded as opc2 = 0b100, sz = 1
         *
         * VCVT{R}{<c>}{<q>}.U32.F32 <Sd>, <Sm> Encoded as opc2 = 0b100, sz = 0
         *
         * VCVT{<c>}{<q>}.F64.<Tm> <Dd>, <Sm> Encoded as opc2 = 0b000, sz = 1
         *
         * VCVT{<c>}{<q>}.F32.<Tm> <Sd>, <Sm> Encoded as opc2 = 0b000, sz = 0
         *
         *
         * Tm S32 encoded as op =1 U32 encoded as op = 0;
         */
    }

    public final void vstr(ConditionFlag cond, CiRegister dest, CiRegister src, int imm8) {
        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(dest.number <= 63 && dest.number >= 16, "vstr dest must be a FP/DP reg");
        checkConstraint(-255 <= imm8 && imm8 <= 255, "vmov offset greater than +/- 255 ");
        checkConstraint(src.number <= 15, "vstr base src address register must be core");
        if (imm8 >= 0) {
            instruction |= 1 << 23;
        } else {
            imm8 = -1 * imm8;
        }
        instruction |= (imm8 & 0xff);
        instruction |= (src.encoding & 0xf) << 16;
        if (dest.number <= 31) {
            instruction |= 0x0d000b00;
            instruction |= (dest.encoding & 0xf) << 12;
        } else {
            instruction |= 0xd000a00;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (dest.encoding & 0x1) << 22; // / Hmmm check some assembler encodings for this please.
        }
        emitInt(instruction);
    }

    public final void vneg(ConditionFlag cond, CiRegister dest, CiRegister src) {
        int instruction = (cond.value() & 0xf) << 28;
        int sz = 0;
        if (src.number >= 32) {
            checkConstraint(src.number >= 32 && dest.number >= 32, "vldr dest must both be a FP reg");
        } else {
            sz = 1;
            checkConstraint(src.number >= 16 && src.number <= 31 && dest.number >= 16 && dest.number <= 31, "vldr dest must be a DP reg");
        }

        instruction |= 0x0eb10a40;

        instruction |= (src.encoding & 0xf);
        instruction |= (src.encoding >> 4) << 5;
        instruction |= (dest.encoding & 0xf) << 12;
        instruction |= (dest.encoding >> 4) << 22;
        instruction |= sz << 8;
        emitInt(instruction);
    }

    public final void vldr(ConditionFlag cond, CiRegister dest, CiRegister src, int imm8) {
        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(dest.number <= 63 && dest.number >= 16, "vldr dest must be a FP/DP reg");
        checkConstraint(-255 <= imm8 && imm8 <= 255, "vmov offset greater than +/- 255 ");
        if (imm8 >= 0) {
            instruction |= 1 << 23;
        } else {
            imm8 = -1 * imm8;
        }
        instruction |= (imm8 & 0xff);
        instruction |= src.encoding << 16;
        if (dest.number <= 31) {
            instruction |= 0x0d100b00;
            instruction |= dest.encoding << 12;
        } else {
            instruction |= 0xd100a00;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (dest.encoding & 0x1) << 22; // / Hmmm check some assembler encodings for this please.
        }
        emitInt(instruction);
    }

    public final void vadd(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        // A8.8.283
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e300a00;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vadd NO CORE REGISTERS ALLOWED");
        checkConstraint((dest.number <= 31 && rn.number <= 31 && rm.number <= 31) || (dest.number <= 63 && rn.number <= 63 && rm.number <= 63), "vadd ALL REGISTERS must be SP OR DP no mix allowed");
        int sz = 0;
        if (dest.number <= 31) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            // VFPV3 only has 16 regs and these fit so no need to do the MSB
            // for double precision registers
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= rm.encoding & 0xf;
        } else {
            // VFPV3 has 32 regs so we NEED to do the MSB manipulation --
            // different to what it would be for doubles!!! (if there were 32)
            instruction |= (rn.encoding >> 4) << 7;
            instruction |= (rm.encoding >> 4) << 5;
            instruction |= (dest.encoding >> 4) << 22;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= rm.encoding >> 1;
        }
        emitInt(instruction);
    }

    public final void vpop(ConditionFlag cond, CiRegister first, CiRegister last) {
        // A8.8.367
        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(first.number >= 16 && last.number >= 16, "vpop NO CORE REGISTERS ALLOWED");
        checkConstraint((first.number <= 31 && last.number <= 31) || (first.number <= 63 && last.number <= 63), "vpop ALL REGISTERS must be SP OR DP no mix allowed");
        checkConstraint(last.number >= first.number, "vpop at least ONE register!!");
        int sz = 0;
        if (first.number <= 31) {
            sz = 1;
        }
        if (sz == 1) {
            instruction |= 0x0cbd0c00;
            // VFPV3 only has 16 regs and these fit so no need to do the MSB
            // for double precision registers
            instruction |= (first.encoding & 0xf) << 12;
            instruction |= (last.encoding - first.encoding + 1) << 1;
        } else {
            instruction |= 0x0cbd0b00;
            // VFPV3 has 32 regs so we NEED to do the MSB manipulation --
            // different to what it would be for doubles!!! (if there were 32)
            instruction |= (first.encoding & 0x1) << 22;
            instruction |= (first.encoding >> 1) << 12;
            instruction |= (last.encoding - first.encoding + 1) << 1;
        }
        emitInt(instruction);
    }

    public final void vpush(ConditionFlag cond, CiRegister first, CiRegister last) {
        // A8.8.368
        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(first.number >= 16 && last.number >= 16, "vpush NO CORE REGISTERS ALLOWED");
        checkConstraint((first.number <= 31 && last.number <= 31) || (first.number <= 63 && last.number <= 63), "vpush ALL REGISTERS must be SP OR DP no mix allowed");
        checkConstraint(last.number >= first.number, "vpush at least ONE register!!");
        int sz = 0;
        if (first.number <= 31) {
            sz = 1;
        }
        if (sz == 1) {
            instruction |= 0x0d2d0b00;
            // VFPV3 only has 16 regs and these fit so no need to do the MSB
            // for double precision registers
            instruction |= (first.encoding & 0xf) << 12;
            instruction |= (last.encoding - first.encoding + 1) << 1;
        } else {
            instruction |= 0x0d2d0a00;
            // VFPV3 has 32 regs so we NEED to do the MSB manipulation --
            // different to what it would be for doubles!!! (if there were 32)
            instruction |= (first.encoding & 0x1) << 22;
            instruction |= (first.encoding >> 1) << 12;
            instruction |= (last.encoding - first.encoding + 1) << 1;
        }
        emitInt(instruction);
    }

    public final void mul(ConditionFlag cond, boolean setFlags, CiRegister dest, CiRegister rn, CiRegister rm) {
        //add(ConditionFlag.Always, false, dest, rm,0,  0);

        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x00000090;
        if (setFlags) {
            instruction |= 1 << 20;
        }
        instruction |= (rm.encoding & 0xf) << 8;
        instruction |= (dest.encoding & 0xf) << 16;
        instruction |= rn.encoding & 0xf;
        emitInt(instruction);

    }

    public final void umull(ConditionFlag cond, boolean s, CiRegister rdHigh, CiRegister rdLow, CiRegister rm, CiRegister rn) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x800090;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (rdHigh.encoding & 0xf) << 16;
        instruction |= (rdLow.encoding & 0xf) << 12;
        instruction |= (rm.encoding & 0xf) << 8;
        instruction |= rn.encoding & 0xf;
        emitInt(instruction);
    }

    public final void sdiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        // A8.8.165
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0710f010;
        instruction |= (rm.encoding & 0xf) << 8;
        instruction |= (dest.encoding & 0xf) << 16;
        instruction |= rn.encoding & 0xf;
        emitInt(instruction);
    }

    public final void udiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        // A8.8.248
        // TODO we need a subroutine for this as most of the ARM hardware we have will not
        // have a hardware integer unit, so the instruction will be undefined/not implemented.
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0730f010;
        instruction |= (rm.encoding & 0xf) << 8;
        instruction |= (dest.encoding & 0xf) << 16;
        instruction |= rn.encoding & 0xf;
        emitInt(instruction);
    }

    public final void vdiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        // A8.8.415
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e800a00;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vdiv NO CORE REGISTERS ALLOWED");
        checkConstraint((dest.number <= 31 && rn.number <= 31 && rm.number <= 31) || (dest.number <= 63 && rn.number <= 63 && rm.number <= 63), "vdiv ALL REGISTERS must be SP OR DP no mix allowed");
        int sz = 0;
        if (dest.number <= 31) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            // VFPV3 only has 16 regs and these fit so no need to do the MSB
            // for double precision registers
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= rm.encoding & 0xf;
        } else {
            // VFPV3 has 32 regs so we NEED to do the MSB manipulation --
            // different to what it would be for doubles!!! (if there were 32)
            instruction |= (rn.encoding >> 4) << 7;
            instruction |= (rm.encoding >> 4) << 5;
            instruction |= (dest.encoding >> 4) << 22;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= rm.encoding >> 1;
        }
        emitInt(instruction);
    }

    public final void vsub(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        // A8.8.415
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e300a40;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vsub NO CORE REGISTERS ALLOWED");
        checkConstraint((dest.number <= 31 && rn.number <= 31 && rm.number <= 31) || (dest.number <= 63 && rn.number <= 63 && rm.number <= 63), "vsub ALL REGISTERS must be SP OR DP no mix allowed");
        int sz = 0;
        if (dest.number <= 31) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            // VFPV3 only has 16 regs and these fit so no need to do the MSB
            // for double precision registers
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= rm.encoding & 0xf;
        } else {
            // VFPV3 has 32 regs so we NEED to do the MSB manipulation --
            // different to what it would be for doubles!!! (if there were 32)
            instruction |= (rn.encoding >> 4) << 7;
            instruction |= (rm.encoding >> 4) << 5;
            instruction |= (dest.encoding >> 4) << 22;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= rm.encoding >> 1;
        }
        emitInt(instruction);
    }

    public final void vmov(ConditionFlag cond, CiRegister dest, CiRegister src) {
        /*
         * APN potentially we need to do lots of checks on instrucitonencodings for this case regarding the particular
         * registers used vmov.f32 s,s vmov.f64 d,d
         */
        int instruction = (cond.value() & 0xf) << 28;
        int vmovSameType = 0x0eb00a40; // A8.8.340
        int vmovSingleCore = 0x0e000a10; // A8.8.343 full word only // ARM core to scalar
        int vmovDoubleCore = 0x0c400b10; // A8.8.345 // TWO ARM core to doubleword extension
        if ((src.number >= 16 && src.number <= 31) && (dest.number >= 16 && dest.number <= 31)) {
            instruction |= (1 << 8) | vmovSameType;
            instruction |= dest.encoding << 12;
            instruction |= src.encoding;
        } else if (src.number >= 32 && dest.number >= 32) {
            // dest LSB bit 22, and 12-15
            // src LSB 5 and 0-3
            instruction |= vmovSameType;
            instruction |= ((dest.encoding >>1) << 12) | ((dest.encoding & 0x1) << 22);
            instruction |= (src.encoding >> 1) | ((src.encoding & 0x1) << 5);
        } else if ((dest.number <= 15 || src.number <= 15) && (src.number >= 32 || dest.number >= 32)) {
            instruction |= vmovSingleCore;
            if (dest.number <= 15) {
                instruction |= (1 << 20) | ((src.encoding & 1) << 7) | (dest.encoding << 12) | ((src.encoding >> 1) << 16);
            } else {
                instruction |= (src.encoding << 12) | ((dest.encoding >> 1) << 16) | ((dest.encoding & 0x1) << 7);
            }
        } else if ((src.number >= 16 && src.number <= 31 && dest.number <= 15) || (dest.number >= 16 && dest.number <= 31 && src.number <= 15)) {
            // deviating slightly from ARM book, we are assuming this to transfer double to pair of core registers
            // aligned on an even 0, 2, ... boundary
            instruction |= vmovDoubleCore;
            if (dest.number <= 15) { // to ARM
                checkConstraint((dest.encoding) <= 14, "vmov doubleword to core destination register > 14");
                instruction |= 1 << 20;
                instruction |= dest.encoding << 12;
                instruction |= (dest.encoding + 1) << 16;
                instruction |= src.encoding;
            } else {
                checkConstraint((src.encoding) <= 14, "vmov core to doubleword core register > 14");
                instruction |= (src.encoding + 1) << 16;
                instruction |= src.encoding << 12;
                instruction |= dest.encoding;
            }
        }
        emitInt(instruction);
    }

    public final void teq(ConditionFlag cond, CiRegister Rn, CiRegister Rm, int imm5) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x13 << 20;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (imm5 & 0x1f) << 7;
        instruction |= (Rm.encoding & 0xf) << 16;
        emitInt(instruction);
    }
}
