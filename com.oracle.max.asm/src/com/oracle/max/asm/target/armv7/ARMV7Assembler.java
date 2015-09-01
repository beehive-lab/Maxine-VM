package com.oracle.max.asm.target.armv7;

import com.oracle.max.asm.AbstractAssembler;
import com.oracle.max.asm.Label;
import com.sun.cri.ci.CiAddress;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;
import com.sun.cri.ri.RiRegisterConfig;


	

/*
	ANDY NISBET comments/ramblings
	instrumentation support for the Xilinx Zynq platform is currently being added.

	We obtain a buffer using a C call. 	
	We had to add an interface to do this in order to avoid a circular dependence.
	
	ARMV7T1XCompilation static instance variable simBuf stores the address of the page length (4096 byte buffer)
	simOffset is actually stored at offset int [1023], which means it is
	@byte offset 4092
	The instance variable simOffset was there only for initial debugging
	CORRECTION WE STORE THE CURRENT ADDRESS TO BE WRITTEN TO AT INT[1023]
	IT MAKES THE ASSEMBLER EASIER

	When we want to update the buffer with a new address tha thas been loaded or stored then we must
	use ldrex strex to update the buffer and the int[1023] offset.

	If we take this approach then it will be thread safe.
	THE INITIAL VERSION WILL NOT DO THE LDREX STREX.

	iF THE ADDRESS WE WANT TO WRITE TO IS THE LAST INT[1022] STORING DATA ADDRESSES READ/WRITTEN
	THEN we need to flush the buffer.

	WE WILL IMPLEMENT THIS BY incrementing after the write and then checking to see if it matches the address of the simOffset in the int[1023]

	The above refers to the method OLDinstrument() ...
	

	The new way, calls out to C to enable the address loaded/stored with modifications to the LSBS to 
	be written to an appropriate buffer based on the currently executing thread. In this way we can allow the simulator to control over the buffer that the address
is written to, where a buffer is tied to a thread. 
	
*/
import static com.oracle.max.cri.intrinsics.MemoryBarriers.STORE_LOAD;

public class ARMV7Assembler extends AbstractAssembler {

    public final CiRegister frameRegister;
    public final CiRegister scratchRegister;
    public final RiRegisterConfig registerConfig;
    public static boolean INSTRUMENT = false;
    public static int	simBuf = 0;
    public static int	simBuffOffset = 0; // testing only not used in real simulation
    public static int	maxineFlushAddress = 0;
    public static com.oracle.max.criutils.NativeCMethodinVM maxineflush = null;
    public ARMV7Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.registerConfig = registerConfig;
        this.scratchRegister = registerConfig == null ? ARMV7.r12 : registerConfig.getScratchRegister();
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
    }
    static {
        initDebugMethods();
    }
    public static void initDebugMethods() {
        /*String value = System.getenv("FLOAT_IDIV");
        if (value == null || value.isEmpty()) {
            FLOAT_IDIV = false;
        } else {
            FLOAT_IDIV = Integer.parseInt(value) == 1 ? true : false;
        }
	//FLOAT_IDIV= true;
	*/
    }

    public enum ConditionFlag {
        Equal(0x0, "="), NotEqual(0x1, "!="), CarrySetUnsignedHigherEqual(0x2, "|carry|"), CarryClearUnsignedLower(0x3, "|ncarry|"), Minus(0x4, "|neg|"), Positive(0x5, "|pos|"), SignedOverflow(0x6,
                ".of."), NoSignedOverflow(0x7, "|nof|"), UnsignedHigher(0x8, "|>|"), UnsignedLowerOrEqual(0x9, "|<=|"), SignedGreaterOrEqual(0xA, ".>=."), SignedLesser(0xB, ".<."), SignedGreater(
                0xC, ".>."), SignedLowerOrEqual(0xD, ".<=."), Always(0xE, "al"), NeverUse(0xF, "NEVER");

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
    public void insertForeverLoop() {
	
        Label forever = new Label();
        bind(forever);
        jcc(ConditionFlag.Always,forever);
    }
    public void branch(Label l) {
        if (l.isBound()) {
            /*
        REMEMBER -- the current stored value of the PC is 8 bytes larger than that of the
        currently executing instruction, BUT WHEN we jump we must set PC to the actual
        address of the instruction we want to execute NEXT.!!!!
         */
            // Compute a relative address if it is less than 24bits;
            // then branch
            // or  TODO compute an absolute address and do a MOV PC,absolute.
            // branch(l.position(), false);
            checkConstraint(-0x800000 <= (l.position() - codeBuffer.position()) && (l.position() - codeBuffer.position()) <= 0x7fffff, "branch must be within  a 24bit offset");
            // emitInt(0x06000000 | (l.position() - codeBuffer.position()) | ConditionFlag.Always.value() & 0xf);
            emitInt(0x0a000000 | (0xffffff & ((l.position() - codeBuffer.position() - 8) / 4)) | ((ConditionFlag.Always.value() & 0xf) << 28));

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
        // if(branch == 76) return; // hack for dcmp01 to see what happens
        checkConstraint(-0x800000 <= (target - branch) && (target - branch) <= 0x7fffff, "branch must be within  a 24bit offset");
        // emitInt(0x06000000 | (target - branch) | ConditionFlag.Always.value() & 0xf);
        int disp = target - branch - 16;
        int instruction = 0;
        int operation = codeBuffer.getInt(branch);
        if (operation == (ConditionFlag.NeverUse.value() << 28 | 0xdead)) { // JCC
            //
            // System.out.println("MATCHED JCC " + branch + " target " + target);
            disp -= 4;
            instruction = movwHelper(ConditionFlag.Always, ARMV7.r12, disp & 0xffff);
            codeBuffer.emitInt(instruction, branch);
            instruction = movtHelper(ConditionFlag.Always, ARMV7.r12, (disp >> 16) & 0xffff);
            codeBuffer.emitInt(instruction, branch + 4);
        } else if (operation == (ConditionFlag.NeverUse.value() << 28 | 0xbeef)) { // JMP
            // disp += 8;
            disp += 8;

            disp = disp / 4;
            // System.out.println("MATCHED OTHER DISP " + (target-branch) + " bran "+ branch + " target " + target);
            if (disp < 0) {
                // System.out.println("NEGATIVE DISP " + disp);
            }
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28), branch);
            // System.out.println("MATCHED JMP branch " + branch + " DISP " + disp + " target "+ target );

        } else if ((operation & 0xf0000fff) == (ConditionFlag.NeverUse.value() << 28 | 0x0d0)) {
            disp = disp + 8;
            disp = disp / 4;
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28), branch);
            // System.out.println("DISP was "+ (target - branch)+ " " + target+ " "+ branch);
            /*
             * code[callOffset + 7] = (byte) (instruction & 0xff); code[callOffset + 6] = (byte) ((instruction >> 8) &
             * 0xff); code[callOffset + 5] = (byte) ((instruction >> 16) & 0xff); code[callOffset + 4] = (byte)
             * ((instruction >> 24) & 0xff);
             */

        } else {
            // System.out.println("JUMP was "+ (target - branch)+ " " + target+ " "+ branch);

// System.out.println("ASSUMING JMP: check this came from an emitPrologue as a result of hosted mode ARM ..patchjumpTarget NOT MATCHED "
// + branch + " " + target);
            disp += 8;
            disp = disp / 4;
            codeBuffer.emitInt(0x0a000000 | (disp & 0xffffff) | ((ConditionFlag.Always.value() & 0xf) << 28), branch);
            // assert 0 == 1;
        }

        // codeBuffer.emitInt(0x0a000000 | (target - branch) | ((ConditionFlag.Always.value() & 0xf) << 28),branch);

    }

    public void sxtb(final ConditionFlag cond, final CiRegister dest, final CiRegister source) {
        int instruction = 0x06af0070;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (dest.encoding & 0xf) << 12;
        instruction |= (source.encoding & 0xf);
        emitInt(instruction);
    }

    public void sxth(final ConditionFlag cond, final CiRegister dest, final CiRegister source) {
        int instruction = 0x06bf0070;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (dest.encoding & 0xf) << 12;
        instruction |= (source.encoding & 0xf);

        emitInt(instruction);
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

    public void orsr(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final CiRegister Rs, final int type) {
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
        instruction |= Rm.encoding & 0xf;
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
        assert (Rd.encoding < 16 && Rm.encoding < 16); // CORE Register move only!
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= Rm.encoding & 0xf;
        emitInt(instruction);
    }

    public void mov(final ConditionFlag cond, final CiRegister Rd, final int immed12) {
        int instruction = 0x3A00000;
        assert (Rd.encoding < 16);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= immed12 & 0xfff;
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
        if (imm16 == 0xfe4c) {
            // System.out.println("DEBUG ME");
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

    public void rsbRegister(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int immed5, final int rotate_amount) {
        int instruction = 0x600000;
        checkConstraint(0 <= immed5 && immed5 < 32, "0 <= immed5 && immed5 < 32");
        checkConstraint(0 <= rotate_amount && rotate_amount < 4, "0 <= rotate_amount && rotate_amount  < 4");
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rm.encoding & 0xf);
        instruction |= (immed5 & 0x1f) << 7;
        instruction |= (rotate_amount & 0x3) << 5;
        emitInt(instruction);
    }

    public void rsc(final ConditionFlag cond, final boolean s, final CiRegister Rd, final CiRegister Rn, final CiRegister Rm, final int immed_5, final int type) {
        int instruction = 0xE00000;
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (s ? 1 : 0) << 20;
        instruction |= (Rd.encoding & 0xf) << 12;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (immed_5 & 0x1f) << 7;
        instruction |= (type & 0x3) << 5;
        instruction |= (Rm.encoding & 0xf);
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
	 if(maxineflush != null) {
                System.out.println("NEVER CALLED strd?");
                instrument(false,true,true, Rn,0);
        }

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
	if(maxineflush != null) {
		System.out.println("NEVER CALLED str?");
                instrument(false,true,true, Rn,0);
        }

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

    public static int strHelper(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, final CiRegister Rm, int imm5, int imm2Type) {
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
	if(maxineflush != null) {
                instrument(false,true,true,Rmemory,imm12);
        }

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
	if(maxineflush != null) {
                instrument(false,true,true,Rn,imm8);
        }

        int instruction = 0x004000f0;
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
        instruction |= ((Rval.encoding & 0xf));
        emitInt(instruction);
    }

    public void rbit(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Rval) {
        int instruction = 0x06ff0f30;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Rval.encoding & 0xf));
        emitInt(instruction);
    }

    public void ldrex(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Raddr) {
	if(maxineflush != null) {
                instrument(true,true,true,Raddr,0);
        }

        int instruction = 0x01900f9f;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Raddr.encoding & 0xf) << 16);
        emitInt(instruction);
    }

    public void ldrexd(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Raddr) {
	if(maxineflush != null) {
                instrument(true,true,true,Raddr,0);
        }

        int instruction = 0x1B00F9F;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Raddr.encoding & 0xf) << 16);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        emitInt(instruction);
    }

    public void strex(final ConditionFlag cond, final CiRegister Rdest, final CiRegister Rnewval, final CiRegister Raddr) {
        int instruction = 0x01800f90;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rdest.encoding & 0xf) << 12);
        instruction |= ((Raddr.encoding & 0xf) << 16);
        instruction |= Rnewval.encoding & 0xf;
        emitInt(instruction);
	if(maxineflush != null) {
                instrument(false,true,true,Raddr,0);
        }

    }

    public void strexd(final ConditionFlag cond, final CiRegister Rd, final CiRegister Rt, final CiRegister Rn) {
        int instruction = 0x1A00F90;
        instruction |= ((cond.value() & 0xf) << 28);
        instruction |= ((Rn.encoding & 0xf) << 16);
        instruction |= ((Rd.encoding & 0xf) << 12);
        instruction |= Rt.encoding & 0xf;
        emitInt(instruction);
	if(maxineflush != null) {
                instrument(false,true,true,Rn,0);
        }

    }

    public void ldruhw(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm8) {
	if(maxineflush != null) {
                instrument(true,true,true,Rn,imm8);
        }

        int instruction = 0x005000b0;
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
        instruction |=  (imm8 & 0xf) | ((0xf0 & imm8) << 4);
        emitInt(instruction);
    }

    public void ldrshw(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm8) {
	if(maxineflush != null) {
                instrument(true,true,true,Rn,imm8);
        }

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
	if(maxineflush != null) {
                instrument(true,true,true,Rn,imm8);
        }

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
	if(maxineflush != null) {
                instrument(true,true,true,Rn,imm12);
        }

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

    private int modifyImmediate(int imm12) {
	// TODO implement the modified immediate arithmetic to include shifts etc. 
	return imm12;
    }

    public void strbImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm12) {
	if(maxineflush != null) {
		// imm12 needs to be modified according to ARM rules ...
                instrument(false,true,true,Rn,imm12);
        }

        int instruction = 0x04400000;
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
	if(maxineflush != null) {
                instrument(false,true,true,Rn,imm8);
        }

        int instruction = 0x004000b0;
        P = P & 1;
        U = U & 1;
        W = W & 1;
        instruction |= (P << 24) | (U << 23) | (W << 21);
        instruction |= (cond.value() & 0xf) << 28;
        instruction |= (Rn.encoding & 0xf) << 16;
        instruction |= (Rt.encoding & 0xf) << 12;
        instruction |= 0xf & imm8;
        instruction |= ((imm8 & 0xff) >> 4) << 8;
        emitInt(instruction);
    }
    private void instrument(boolean read,boolean data, boolean add, final CiRegister base, int immediate) {
	if(simBuf == 0) {
		simBuf = maxineflush.maxine_instrumentationBuffer();
	}	
	if(maxineFlushAddress == 0) {
		maxineFlushAddress  = maxineflush.maxine_flush_instrumentationBuffer();
	}
	// save some registers to the stack using a 

	/* Format bottom 2 bits used/
	2 instruction read
	1 data write
	0 data read
	i.e.
	bit 0 = Write
	bit 1 = Instruction 
		Instruction Operation
		Bit 1       Bit 0
-----------------------------
DATAREAD     0           0
DATAWRITE    0		 1
CODEREAD     1		 0
CODEWRITE    1		 1
    public static boolean INSTRUMENT = false;
    public static int	simBuf = 0;
    public static int	simBuffOffset = 0;
	*/
	CiRegister immReg = null;
	CiRegister spareAddress = null;
	CiRegister spareImm = null;
	CiRegister destAddress = null;
	CiRegister valAddress = null;
	switch(base.encoding) {
		/*
		r0 r1 r2 r8 r9 r12
		are pushed
		*/
		case 0:
			spareAddress = ARMV7.r8;
			spareImm = ARMV7.r9;	
			destAddress = ARMV7.r1;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r2;
		break;
		case 8:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r1;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r9;
		break;
			
		case 9:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r2;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r8;
		break;
		case 1:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r2;	
			destAddress = ARMV7.r8;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r9;
		break;
		case 2:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r8;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r9;
		break;
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 10:
		case 11:
		case 14:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r2;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r8;
		break;
		case 12:
		case 13:
			destAddress = ARMV7.r0;
			valAddress = ARMV7.r1;
			immReg = ARMV7.r2;
			spareAddress = ARMV7.r8;
			spareImm = ARMV7.r9;	
		break;
		default:
			assert 0 == 1 : "ERROR insturmentation uses illegal base register";	
		break;
	}	
	int orint = 0;
	if(!read)  {

		orint |= 1;
	}

	if(!data) {

		orint |= 2;
	}

	instrumentPush(ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512|1024|2048|4096|/*8192|*/16384);
	mov32BitConstant(valAddress,immediate);
	addRegisters(ConditionFlag.Always, false, valAddress, valAddress, base, 0, 0); // forms the address to be read/written
	or(ConditionFlag.Always, false, valAddress, valAddress, orint); // ors the read/write code/data bits
        mov(ConditionFlag.Always, false, ARMV7.r0, valAddress);
	mov32BitConstant(ARMV7.r12,maxineFlushAddress);
        blx(ARMV7.r12);
        instrumentPop(ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512|1024|2048|4096|/*8192|*/16384);
        return;

}
    private void OLDinstrument(boolean read,boolean data, boolean add, final CiRegister base, int immediate) {
	if(simBuf == 0) {
		simBuf = maxineflush.maxine_instrumentationBuffer();
	}	
	if(maxineFlushAddress == 0) {
		maxineFlushAddress  = maxineflush.maxine_flush_instrumentationBuffer();
	}
	assert(maxineFlushAddress != 0);
	simBuffOffset = 1023*4;
	// save some registers to the stack using a 

	/* Format bottom 2 bits used/
	2 instruction read
	1 data write
	0 data read
	i.e.
	bit 0 = Write
	bit 1 = Instruction 
		Instruction Operation
		Bit 1       Bit 0
-----------------------------
DATAREAD     0           0
DATAWRITE    0		 1
CODEREAD     1		 0
CODEWRITE    1		 1
    public static boolean INSTRUMENT = false;
    public static int	simBuf = 0;
    public static int	simBuffOffset = 0;
	*/
	CiRegister immReg = null;
	CiRegister spareAddress = null;
	CiRegister spareImm = null;
	CiRegister destAddress = null;
	CiRegister valAddress = null;
	switch(base.encoding) {
		/*
		r0 r1 r2 r8 r9 r12
		are pushed
		*/
		case 0:
			spareAddress = ARMV7.r8;
			spareImm = ARMV7.r9;	
			destAddress = ARMV7.r1;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r2;
		break;
		case 8:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r1;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r9;
		break;
			
		case 9:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r2;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r8;
		break;
		case 1:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r2;	
			destAddress = ARMV7.r8;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r9;
		break;
		case 2:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r8;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r9;
		break;
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 10:
		case 11:
		case 14:
			spareAddress = ARMV7.r0;
			spareImm = ARMV7.r1;	
			destAddress = ARMV7.r2;
			valAddress = ARMV7.r12;
			immReg = ARMV7.r8;
		break;
		case 12:
		case 13:
			destAddress = ARMV7.r0;
			valAddress = ARMV7.r1;
			immReg = ARMV7.r2;
			spareAddress = ARMV7.r8;
			spareImm = ARMV7.r9;	
		break;
		default:
			assert 0 == 1 : "ERROR insturmentation uses illegal base register";	
		break;
	}	
	instrumentPush(ConditionFlag.Always,1 | 2| 4| 1<<12|1<<8|1<<9);
	int address = simBuf;
	int orint = 0;

	if(!read)  {

		orint |= 1;
	}

	if(!data) {

		orint |= 2;
	}

	mov32BitConstant(destAddress,address); // currently this is simBuf
	mov32BitConstant(valAddress,simBuffOffset); // constructing the address of where actual offset is STORED
	addRegisters(ConditionFlag.Always, false, spareAddress, valAddress, destAddress, 0, 0); // spareAddress contains the address of the offset
	// spareAddress now holds the address of simBuf[1023] in C-style format, this holds the ADDRESS to write into the buffer
	instrumentLdr(ConditionFlag.Always, spareImm,spareAddress,0); // spareImm after this instr, contains the address to write into
	// spareImm holds the Address to write into

	mov32BitConstant(valAddress,immediate);
	addRegisters(ConditionFlag.Always, false, valAddress, valAddress, base, 0, 0); // forms the address to be read/written
	or(ConditionFlag.Always, false, valAddress, valAddress, orint); // ors the read/write code/data bits
	instrumentStr(ConditionFlag.Always,  valAddress, spareImm, 0);	// updates the buffer
	
	// so now we need to update the offset.
	add(ConditionFlag.Always, false, spareImm, spareImm, 4, 0); // we added 4 to the address to write to in the buffer!
	cmp(ConditionFlag.Always, spareImm, spareAddress, 0, 0);
	Label bufferNotFull = new Label();
	Label complete = new Label();
	jcc(ConditionFlag.CarryClearUnsignedLower,bufferNotFull);
	//maxineflush.maxine_flush_InstrumentationBuffer();
	//Label forever= new Label();
	//bind(forever);
	//jcc(ConditionFlag.Always,forever);
	instrumentPush(ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512|1024|2048|4096|/*8192|*/16384);
	membar(0);
	mov32BitConstant(ARMV7.r1,maxineFlushAddress);
	mov32BitConstant(ARMV7.r0,simBuf);
	blx(ARMV7.r1);
	instrumentPop(ConditionFlag.Always,1|2|4|8|16|32|64|128|256|512|1024|2048|4096|/*8192|*/16384);
	jcc(ConditionFlag.Always, complete);
	nop();
	bind(bufferNotFull);	// UPDATE the offset
	instrumentStr(ConditionFlag.Always,spareImm,spareAddress, 0);
	bind(complete);
	instrumentPop(ConditionFlag.Always, 1|2|4|1<<12|1<<8|1<<9);
	return;

    }

    public void ldrImmediate(final ConditionFlag cond, int P, int U, int W, final CiRegister Rt, final CiRegister Rn, int imm12) {
	if(maxineflush != null) {
                instrument(true,true,true,Rn,imm12);
        }
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
	if(maxineflush != null) {
                //instrument(true,true,true,ARMV7.rn,imm12);
		System.out.println("ldr cwSHOULD not be CALLED\n");
		/*
		Currently this instruction is not used apart from movss and DEOPT stub which we have not yet implemented fully
		*/
        }
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

    public void movss(final ConditionFlag cond, int P, int U, int W, final CiRegister Rn, final CiRegister Rt, final CiRegister Rm, int imm2Type, int imm5, CiKind destKind, CiKind srcKind) {
        if (destKind.isGeneral()) {
            ldr(cond, P, U, W, Rn, Rt, Rm, imm2Type, imm5);
        } else {
            assert destKind.isFloatOrDouble();
            vldr(cond, Rn, Rt, 0, destKind, srcKind);
        }
    }

    public void movsd(final ConditionFlag cond, int P, int U, int W, final CiRegister Rn, final CiRegister Rt, final CiRegister Rm, CiKind destKind, CiKind srcKind) {
        if (destKind.isGeneral()) {
            ldrd(cond, P, U, W, Rn, Rt, Rm);
        } else {
            assert destKind.isFloatOrDouble();
            vldr(cond, Rn, Rt, 0, destKind, srcKind);
        }
    }

    public void ldrd(final ConditionFlag cond, int P, int U, int W, final CiRegister Rn, final CiRegister Rt, final CiRegister Rm) {
        if(maxineflush != null) {
		/*
		NOT used
		*/
		System.out.println("ldrd should not be called\n");
                //instrument(true,true,true,Rn,imm12);
        }

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

    public void instrumentPush(final ConditionFlag flag, final int registerList) {
        int instruction;
        assert(registerList > 0);
        assert(registerList < 0x10000);
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x9 << 24;
        instruction |= 0x2 << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    private void instrumentTest() {
	if(simBuf == 0) {
		simBuf = maxineflush.maxine_instrumentationBuffer();
		
	} 
	boolean read = false;
	boolean data = false;
	push(ConditionFlag.Always, 1<<8);
	for(int i = 0; i <1028 ;i++)	{
		if(i% 4 == 0) {
			read = !read;
		}
		if(i % 3 == 0) {
			data =  !data;
		}
		mov32BitConstant(ARMV7.r8,i);
		instrument(read,data,true,ARMV7.r8,0);
	}
	pop(ConditionFlag.Always, 1<<8);
    }
    public void push(final ConditionFlag flag, final int registerList) {
	if(maxineflush != null) {
		int count = 0;
		if((registerList & (1<<14))  == 0) {
		for(int i =0; i < 16;i++) {
			if((registerList & (1<<i)) != 0) {
                		instrument(false,true,true,ARMV7.r13,-count++*4);
			}
		}
		}
        }

        int instruction;
	assert(registerList > 0);
	assert(registerList < 0x10000);
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x9 << 24;
        instruction |= 0x2 << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    // Wee need that method due to limited number of registers when we process long values in 32 bit space.
    public void saveRegister(int reg) {
        push(ConditionFlag.Always, 1 << reg);
    }

    public void restoreRegister(int reg) {
        pop(ConditionFlag.Always, 1 << reg);
    }
    public void instrumentPop(final ConditionFlag flag, final int registerList) {
  	int instruction;
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x8 << 24;
        instruction |= 0xb << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void pop(final ConditionFlag flag, final int registerList) {
	if(maxineflush != null) {
                int count = 0;
                for(int i =0; i < 16;i++) {
                        if((registerList & (1<<i)) != 0) {
                                instrument(true,true,true,ARMV7.r13,count++*4);
                        }
                }
        }

        int instruction;
        instruction = (flag.value() & 0xf) << 28;
        instruction |= 0x8 << 24;
        instruction |= 0xb << 20;
        instruction |= 0xd << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void ldmea(final ConditionFlag flag, final CiRegister theStack, final int registerList) {
        int instruction = 0x09100000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (theStack.encoding & 0xf) << 16;
        instruction |= 0xffff & registerList;
        emitInt(instruction);
    }

    public void ldrd(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseReg, int offset8) {
	if(maxineflush != null) {
                instrument(true,true,true,baseReg,offset8);
        }

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
	if(maxineflush != null) {
                instrument(false,true,true,baseReg,offset8);
        }

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

    public void instrumentStr(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseRegister, final int offset12) {

        int instruction;
        instruction = 0x05800000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (valueReg.encoding & 0xf) << 12;
        instruction |= (baseRegister.encoding & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);

    }

    public void instrumentLdr(final ConditionFlag flag, final CiRegister destReg, final CiRegister baseRegister, final int offset12) {

        int instruction;
        instruction = 0x05900000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (destReg.encoding & 0xf) << 12;
        instruction |= (baseRegister.encoding & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);

    }

    public void str(final ConditionFlag flag, final CiRegister valueReg, final CiRegister baseRegister, final int offset12) {
	if(maxineflush != null) {
                instrument(false,true,true, baseRegister,offset12);
        }

        int instruction;
        instruction = 0x05800000;
        instruction |= (flag.value() & 0xf) << 28;
        instruction |= (valueReg.encoding & 0xf) << 12;
        instruction |= (baseRegister.encoding & 0xf) << 16;
        instruction |= offset12 & 0xfff;
        emitInt(instruction);
    }

    public void ldr(final ConditionFlag flag, final CiRegister destReg, final CiRegister baseRegister, final int offset12) {
	if(maxineflush != null) {
                instrument(true,true,true,baseRegister,offset12);
        }

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

        assert (!(Rn.encoding == 12 && Rn.encoding == Rm.encoding)); // defensive assert against scratch problem in ARMV7LIemitCompare ...

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

    public void setUpScratch(CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(numInstructions(addr)); // 4 instructions, 2 for mov32, 1 for add and 1 for addclsl
            return;
        }

        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base.isValid() || base.compareTo(CiRegister.Frame) == 0) {
            if (base == CiRegister.Frame) {
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

        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base.isValid() || base.compareTo(CiRegister.Frame) == 0) {
            if (base == CiRegister.Frame) {
                base = frameRegister;
            }
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
        if (dst.number < 16) {
            movw(ConditionFlag.Always, dst, imm32 & 0xffff);
            imm32 = imm32 >> 16;
            imm32 = imm32 & 0xffff;
            movt(ConditionFlag.Always, dst, imm32 & 0xffff);
        } else {
            mov32BitConstant(ARMV7.r12, imm32);
            vmov(ConditionFlag.Always, dst, ARMV7.r12, null, CiKind.Float, CiKind.Int);
        }
    }

    public final void vsqrt(ConditionFlag cond, CiRegister dst, CiRegister src) {
        assert ((src.number > 15 && src.number < 32 && dst.number > 15 && dst.number < 32) || (src.number > 32 && dst.number > 32));
        int instruction = 0x0eb10ac0;
        instruction |= (cond.value() << 28);
        int dp = (src.number < 32) ? 1 : 0;
        int dest = dst.encoding;
        int srcr = src.encoding;
        instruction |= dp << 8; // sets the sz bit
        if (dp == 1) {
	    // in this case we can use the numbers without problem.
            instruction |= srcr;
            instruction |= dest << 12;
        } else {
            instruction |= srcr >> 1;
            instruction |= (srcr & 1) << 5;
            instruction |= (dest >> 1) << 12;
            instruction |= (dest & 1) << 22;
        }
        assert (instruction!=0xeeb12bd2);
        emitInt(instruction);
    }

    public final void mov16BitConstant(ConditionFlag cond, CiRegister dst, int imm16) {
        movw(cond, dst, imm16);
    }

    public final void mov64BitConstant(CiRegister dstLow, CiRegister dstUpper, long imm64) {
        int low32 = (int) (imm64 & 0xffffffffL);
        int high32 = (int) ((imm64 >> 32) & 0xffffffffL);
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

    public final void blx(CiRegister target) {
        int instruction = blxHelper(ConditionFlag.Always, target);
        emitInt(instruction);
    }

    public final void newIndirectT1XCall(CiRegister target) {
	int instruction = blxHelper(ConditionFlag.Always, ARMV7.r8);
        emitInt(instruction);

    }
    public final void call(CiRegister target) {
	if(ARMV7.r8 != target ) {
        	addRegisters(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, target, 0, 0);
	}
        int instruction = blxHelper(ConditionFlag.Always, ARMV7.r8);
        emitInt(instruction);
    }

    public final void leaq(CiRegister dest, CiAddress addr) {
        if (addr == CiAddress.Placeholder) {
            nop(4);
        } else {
            setUpScratch(addr);
            // ldrImmediate(ConditionFlag.Always, 1, 0, 0, dest, ARMV7.r12,0 );
            mov(ConditionFlag.Always, false, dest, ARMV7.r12);
        }
    }

    public final void leave() {
        ret();
    }

    public final void movslq(CiAddress dst, int imm32) {
        setUpScratch(dst);
        mov32BitConstant(ARMV7.r8, imm32);
        str(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
    }

    public final void cmpl(CiRegister src, int imm32) {
        assert src.isValid();
        mov32BitConstant(scratchRegister, imm32);
        cmp(ConditionFlag.Always, src, scratchRegister, 0, 0);

    }

    public final void cmpl(CiRegister src1, CiAddress src2) {
        assert src1.isValid();
        setUpScratch(src2); // APN not sure if this requires a load!
        ldr(ConditionFlag.Always, ARMV7.r12, ARMV7.r12, 0);
        cmp(ConditionFlag.Always, src1, scratchRegister, 0, 0);
    }

    public final void cmpl(CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
    }

    public final void lcmpl(ConditionFlag condition, CiRegister src1, CiRegister src2) {
        cmp(ConditionFlag.Always, src1, src2, 0, 0);
        if (condition == ConditionFlag.Equal || condition == ConditionFlag.NotEqual) {
            cmp(ConditionFlag.Equal, ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
        } else {
            sbc(ConditionFlag.Always, true, scratchRegister, ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
        }
    }

    public final void xchgq(CiRegister src1, CiRegister src2) {
        CiRegister tmp = ARMV7.r8;
        assert src1 != tmp && src2 != tmp;
        mov(ConditionFlag.Always, false, tmp, src1);
        mov(ConditionFlag.Always, false, src1, src2);
        mov(ConditionFlag.Always, false, src2, tmp);
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
        addCRegisters(ConditionFlag.Always, false, ARMV7.cpuRegisters[dst.number + 1], ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
    }

    public final void subLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        sub(ConditionFlag.Always, true, dst, src1, src2, 0, 0);
        sbc(ConditionFlag.Always, false, ARMV7.cpuRegisters[dst.number + 1], ARMV7.cpuRegisters[src1.number + 1], ARMV7.cpuRegisters[src2.number + 1], 0, 0);
    }

    public final void mulLong(CiRegister dst, CiRegister src1, CiRegister src2) {
        assert (src1 == dst);
        push(ConditionFlag.Always, 1 << src2.number | 1 << (src2.number + 1));
        mov(ConditionFlag.Always, false, ARMV7.r12, src2); // save src2
        mul(ConditionFlag.Always, false, src2, src2, ARMV7.cpuRegisters[src1.number + 1]);
        mul(ConditionFlag.Always, false, ARMV7.cpuRegisters[src2.number + 1], src1, ARMV7.cpuRegisters[src2.number + 1]);
        addRegisters(ConditionFlag.Always, false, ARMV7.cpuRegisters[src2.number + 1], src2, ARMV7.cpuRegisters[src2.number + 1], 0, 0);
        umull(ConditionFlag.Always, false, ARMV7.cpuRegisters[src2.number], dst, ARMV7.cpuRegisters[scratchRegister.number], src1);
        addRegisters(ConditionFlag.Always, false, scratchRegister, ARMV7.cpuRegisters[src2.number + 1], src2, 0, 0);
        mov(ConditionFlag.Always, false, ARMV7.cpuRegisters[dst.number + 1], scratchRegister);
        pop(ConditionFlag.Always, 1 << src2.number | 1 << (src2.number + 1));
    }

    public void xorq(CiRegister dest, CiAddress src) {
        assert dest.isValid();
        setUpScratch(src);
        eor(ConditionFlag.Always, false, dest, dest, scratchRegister, 0, 0);
    }

    public void xorq(CiRegister dest, CiRegister src) {
        assert dest.isValid();
        assert src.isValid();
        eor(ConditionFlag.Always, false, dest, dest, src, 0, 0);
    }

    public final void vcmp(boolean nanExceptions, ConditionFlag cond, CiRegister src1, CiRegister src2, CiKind src1Kind, CiKind src2Kind) {
        assert src1.isFpu() && src2.isFpu();
        int instruction = 0x0eb40a40;
        int dpOperation = 0;
        int quietNAN = nanExceptions ? 0 : 1;

        if (src1Kind.isDouble()) {
            assert src2Kind.isDouble();
            dpOperation = 1;
            instruction |= (src1.encoding << 12);
            instruction |= (src2.encoding);
        } else {
            assert src2Kind.isFloat();
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

    public final void ucomisd(CiRegister dst, CiRegister src, CiKind destKind, CiKind srcKind) {
        assert destKind.isFloatOrDouble();
        assert srcKind.isFloatOrDouble();
        vcmp(true, ConditionFlag.Always, dst, src, destKind, srcKind);
        vmrs(ConditionFlag.Always, ARMV7.r15);
    }

    public void align(int modulus) {
        // Is alignment relevant at all for ARM
        // we do not have the same restrictions as X86.
        if (codeBuffer.position() % modulus != 0) {
	 	assert(modulus %4 == 0); // ARM;
		nop((modulus - codeBuffer.position() )/4);
            //nop(modulus - (codeBuffer.position() % modulus));
        }
    }

    public final void nop() {
        nop(1);
    }

    public final void pause() {
        // YIELD instruction hint, intended to improve spinlock performance
        /*int instruction = 0x0320f001;
        instruction |= (ConditionFlag.Always.value() << 28);
        emitInt(instruction);
	*/
        push(ConditionFlag.Always, 1|2|4|128 );
        mov32BitConstant(ARMV7.r7,158); // sched_yield
        emitInt(0xef000000); // replaced with svc 0
        pop(ConditionFlag.Always, 1|2|4|128 );

    }

    public final void int3() {
        //emitInt(0xe1200070); // this is BKPT
/*
	svc implement system calls, r7 holds the system call number and a return value comes backin r0...
	swi is svc 0, so we load r7 with 0, NOTE we need to save/restore the registers used.
*/
        push(ConditionFlag.Always, 1|128 ); // push r0 and r7
        eor(ConditionFlag.Always, false, ARMV7.r0, ARMV7.r0, ARMV7.r0, 0, 0);
	eor(ConditionFlag.Always, false, ARMV7.r7, ARMV7.r7, ARMV7.r7, 0, 0);
        emitInt(0xef000000); // replaced with svc 0
	pop(ConditionFlag.Always, 1 |128 );


    }
    public final void flushicache(CiRegister startAddress, int bytes) {

/*
http://community.arm.com/groups/processors/blog/2010/02/17/caches-and-self-modifying-code

push {r0-r2, r7}  
  adr r0, start_label  
  ldr r1, =end_label  
  mov r2, #0  
  ldr r7, =0x000f0002  
  svc 0  
  pop {r0-r2, r7}  
  
start_label:  
  
  ...  @ Patched code.  
  
end_label:  
*/
	assert(startAddress.encoding == ARMV7.r12.encoding);
        push(ConditionFlag.Always, 1|2|4|8|128 );
        mov(ConditionFlag.Always, false, ARMV7.r0, scratchRegister);
	mov32BitConstant(ARMV7.r1,bytes);
        eor(ConditionFlag.Always, false, ARMV7.r2, ARMV7.r2, ARMV7.r2, 0, 0);
	mov32BitConstant(ARMV7.r7,0x000f0002);
	addlsl(ConditionFlag.Always,false,ARMV7.r1,ARMV7.r1,ARMV7.r0,0);
	emitInt(0xef000000); // replaced with svc 0
        pop(ConditionFlag.Always, 1|2|4|8|128 );
	//emitInt(0xf57ff06f);
	//pause();// try a sched_yield here as well.
	
    }

    public final void hlt() {
        nop(4);
        int3();
    }

    public final void nop(int times) {
        assert times > 0 : "Cannot emit a -v number of noops";
        for (int i = 0; i < times; i++) {
            nop(ConditionFlag.Always);
        }
    }

    public final void ret() {
        // TODO ret() implements an X86 return from subroutine this needs to pop the return value of the stack TODO we
        // might need to push the value of r14 onto the stack in order to make this work for a call from the C harness
        // TODO for testing of the methods


        // ldmea(ConditionFlag.Always,ARMV7.r11,1<<11|1<<13|1<<15);
        pop(ConditionFlag.Always, 1 << 15);
        // pop(ConditionFlag.Always,1<<15|1<<11);

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
        assert false : "Enter not implemented";
        push(ConditionFlag.Always, 1 << 14);
        mov32BitConstant(ARMV7.r12, imm16);
        sub(ConditionFlag.Always, false, ARMV7.r13, ARMV7.r13, ARMV7.r12, 0, 0);

    }

    public final void lock() {
        nop();
    }

    public void nullCheck(CiRegister r) {
        //emitInt((0xe << 28) | (0x3 << 24) | (0x5 << 20) | (r.encoding << 16) | 0); // sets condition flags
	if(r == ARMV7.r8) {
		emitInt((0xe << 28) | (0x3 << 24) | (0x5 << 20) | (r.encoding << 16) | 0); // sets condition flags
		ldr(ConditionFlag.Equal,ARMV7.r12,r,0);

	}else {
	 	emitInt((0xe << 28) | (0x3 << 24) | (0x5 << 20) | (r.encoding << 16) | 0); // sets condition flags
	       ldr(ConditionFlag.Equal,ARMV7.r8, r,0);

	}
    }

    public void membar(int barriers) {
        int instruction = 0xF57FF050;
        //if (target.isMP) {
            // We only have to handle StoreLoad
            //if (barriers == -1 || ((barriers & STORE_LOAD) != 0)) {
                instruction |= 0xf;
                emitInt(instruction);
                emitInt(0xf57ff04f); //DSB
		emitInt(0xf57ff06f); // ISB
                emitInt(instruction);
            //}
        //}
    }

    public void enter(short imm16) {
    }

    public final void jcc(ConditionFlag cc, int target, boolean forceDisp32) {
        /*
        REMEMBER -- the current stored value of the PC is 8 bytes larger than that of the
        currently executing instruction, BUT WHEN we jump we must set PC to the actual
        address of the instruction we want to execute NEXT.!!!!
         */
        int disp = (target - codeBuffer.position());
        if (Math.abs(disp) <= 16777214 && !forceDisp32) { // TODO check ok to make this false
            disp = (disp - 8) / 4;
            emitInt((cc.value & 0xf) << 28 | (0xa << 24) | (disp & 0xffffff));
        } else {
            if (disp > 0) {
                disp -= 16;
            } else {
                disp = disp - 16;
            }
            mov32BitConstant(scratchRegister, disp);
            addRegisters(ConditionFlag.Always, false, scratchRegister, ARMV7.r15, scratchRegister, 0, 0);
            mov(cc, false, ARMV7.r15, scratchRegister);
        }
    }

    public final void jcc(ConditionFlag cc, Label l) {
        assert (0 <= cc.value) && (cc.value < 16) : "illegal cc";
        if (l.isBound()) {
            // System.out.println("LABEL bound jcc no need to patch");
            // jcc(cc, l.position(), false);
            jcc(cc, l.position(), false);

        } else {
            // Note: could eliminate cond. jumps to this jump if condition
            // is the same however, seems to be rather unlikely case.
            // Note: use jccb() if label to be bound is very close to get
            // an 8-bit displacement
            l.addPatchAt(codeBuffer.position());
            // System.out.println("ADDED JCC PATCH AT" + codeBuffer.position());
            emitInt(ConditionFlag.NeverUse.value() << 28 | 0xdead); // JCC CODE for the PATCH
            nop(2);
            // TODO issues exist here ... what happens if R12 is loaded twice?
            // TODO or used as scratch inbetween the setup of its value and
            // this point?
            // TODO decide how to distinguish this from other patches
            // TODO update wiki on this
            // ldr(ConditionFlag.Always,0,0,0,ARMV7.r12,ARMV7.r12,ARMV7.r12,0,0);
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
            // System.out.println("JMP PATCHAT "+ codeBuffer.position());
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
            else if (disp < 0)
                disp = disp - 16;
            // System.out.println("JMP "+ target + " CODE " +codeBuffer.position());
            mov32BitConstant(scratchRegister, disp);
            addRegisters(ConditionFlag.Always, false, scratchRegister, ARMV7.r15, scratchRegister, 0, 0);
            mov(ConditionFlag.Always, false, ARMV7.r15, scratchRegister); // UPDATE the PC to the target
        }
    }

    public final void mrsReadAPSR(ConditionFlag cond, CiRegister reg) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x10f0000;
        instruction |= reg.encoding << 12;
        emitInt(instruction);
    }

    public void msrWriteAPSR(ConditionFlag cond, CiRegister reg) {
        int bits = 3;
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x120f000;
        instruction |= reg.encoding;
        instruction |= bits << 18;
        emitInt(instruction);

    }

    public final void vmrs(ConditionFlag cond, CiRegister dest) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0ef10a10;
        instruction |= dest.encoding << 12;
        emitInt(instruction);
    }

    public final void vmsr(ConditionFlag cond, CiRegister dest) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0ee10a10;
        instruction |= dest.encoding << 12;
        emitInt(instruction);
    }

    public final void vmul(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e200a00;
        int sz = 0;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vmul ALL  FP/DP regs");
        if (destKind.isDouble()) {
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

    public final void vcvt(ConditionFlag cond, CiRegister dest, boolean toInt, boolean signed, CiRegister src, CiKind destKind, CiKind srcKind) {
        int instruction = (cond.value() & 0xf) << 28;
        int sz = 0;
        int op = 0;
        int opc2 = 0;
        boolean double2Float = false;
        boolean int2Float = false;
        boolean int2Double = false;
        boolean floatConversion = false;

        if (toInt == false) {
            checkConstraint(!dest.isGeneral() && !src.isGeneral(), "vcvt must be FP/DP regs");
            if (destKind.isFloat() && srcKind.isDouble()) {
                double2Float = true;
            } else if (destKind.isFloat() && srcKind.isGeneral()) {
                int2Float = true;
            } else if (destKind.isDouble() && srcKind.isGeneral()) {
                int2Double = true;
            }
            if (destKind.isFloat()) {
                floatConversion = true;
            }
        }

        if (destKind.isDouble() || srcKind.isDouble()) {
            sz = 1;
        }

        if (signed) {
            if (toInt) {
                opc2 = 5;
		op = 1; // use round to zero mdoe NOT!!! NOT!!! FPSCR rounding mode!!!!
            } else {
                opc2 = 0;
                op = 1;
            }
        } else {
            if (toInt) {
                opc2 = 4;
		op = 1; // use round to zero mdoe NOT!!! NOT!!! FPSCR rounding mode!!!!
            } else {
                opc2 = 0;
            }
        }
        if (!floatConversion) {
            if (toInt) { // FD2I
                instruction |= 0xeb80a40;
                instruction |= (dest.encoding >> 1) << 12; // LSB in bit 22
                instruction |= (dest.encoding & 0x1) << 22;
                if (sz == 1) {
                    instruction |= (src.encoding & 0xf);
                    instruction |= (src.encoding >> 4) << 5;
                } else {
                    instruction |= (src.encoding >> 1);
                    instruction |= (src.encoding & 0x1) << 5;
                }
                instruction |= opc2 << 16;
                instruction |= op << 7;
                instruction |= sz << 8;
            } else if (int2Double) { // I2D
                instruction |= 0xEB80A40;
                instruction |= sz << 8;
                instruction |= (dest.encoding >> 4) << 22;
                instruction |= (dest.encoding & 0xf) << 12;
                instruction |= (src.encoding & 1) << 5;
                instruction |= (src.encoding >> 1);
                instruction |= opc2 << 16;
                instruction |= op << 7;
            } else { // F2D
                instruction |= 0xEB70AC0;
                instruction |= (src.encoding & 0x1) << 5;
                instruction |= src.encoding >> 1;
                instruction |= (dest.encoding >> 4) << 22;
                instruction |= (dest.encoding & 0xf) << 12;
            }
        } else {
            instruction |= sz << 8;
            if (double2Float) { // D2FI
                instruction |= 0xEB70AC0;
                instruction |= (dest.encoding >> 1) << 12;
                instruction |= (dest.encoding & 1) << 22;
                instruction |= (src.encoding & 0xf);
                instruction |= (src.encoding >> 4) << 5;
            } else if (int2Float) { // I2F
                instruction |= 0xEB80A40;
                instruction |= (dest.encoding >> 1) << 12;
                instruction |= (dest.encoding & 1) << 22;
                instruction |= (src.encoding & 1) << 5;
                instruction |= (src.encoding >> 1);
                instruction |= opc2 << 16;
                instruction |= op << 7;
            }
        }
        emitInt(instruction);
    }

    public final void vstr(ConditionFlag cond, CiRegister dest, CiRegister src, int imm8, CiKind destKind, CiKind srcKind) {
	if(maxineflush != null) {
                instrument(false,true,true,src,imm8);
        }

        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(dest.isFpu(), "vstr dest must be a FP/DP reg");
        checkConstraint(-255 <= imm8 && imm8 <= 255, "vmov offset greater than +/- 255 ");
        checkConstraint(src.isCpu(), "vstr base src address register must be core");
        if (imm8 >= 0) {
            instruction |= 1 << 23;
        } else {
            imm8 = -1 * imm8;
        }
        instruction |= (imm8 & 0xff);
        instruction |= (src.encoding & 0xf) << 16;
        if (destKind.isDouble()) {
            instruction |= 0x0d000b00;
            instruction |= (dest.encoding & 0xf) << 12;
        } else {
            instruction |= 0xd000a00;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (dest.encoding & 0x1) << 22;
        }
        emitInt(instruction);
    }

    public final void vneg(ConditionFlag cond, CiRegister dest, CiRegister src, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        assert destKind.isFloat() || destKind.isDouble() : " Dest register must be FP/DP reg";
        int sz = 0;
        if (destKind.isDouble()) {
            sz = 1;
        }
        instruction |= 0x0eb10a40;
        instruction |= (src.encoding & 0xf);
        instruction |= (src.encoding >> 4) << 5;
        instruction |= (dest.encoding & 0xf) << 12;
        instruction |= (dest.encoding >> 4) << 22;
        instruction |= sz << 8;
        emitInt(instruction);
    }

    public final void vldr(ConditionFlag cond, CiRegister dest, CiRegister src, int imm8, CiKind destKind, CiKind srcKind) {
	if(maxineflush != null) {
                instrument(true,true,true,src,imm8);
        }

        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(dest.isFpu(), "vldr dest must be a FP/DP reg");
        checkConstraint(-255 <= imm8 && imm8 <= 255, "vmov offset greater than +/- 255 ");
        if (imm8 >= 0) {
            instruction |= 1 << 23;
        } else {
            imm8 = -1 * imm8;
        }
        instruction |= (imm8 & 0xff);
        instruction |= src.encoding << 16;
        if (destKind.isDouble()) {
            instruction |= 0x0d100b00;
            instruction |= dest.encoding << 12;
        } else {
            instruction |= 0xd100a00;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (dest.encoding & 0x1) << 22;
        }
        emitInt(instruction);
    }

    public final void vadd(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind kind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e300a00;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vadd no core registers allowed");
        checkConstraint((dest.number <= 47 && rn.number <= 47 && rm.number <= 47) || (dest.number <= 47 && rn.number <= 47 && rm.number <= 47), "vadd register overflow");
        int sz = 0;
        if (kind.isDouble()) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= rm.encoding & 0xf;
        } else {
            instruction |= (rn.encoding & 1) << 7;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= (rm.encoding & 1) << 5;
            instruction |= rm.encoding >> 1;
            instruction |= (dest.encoding & 1) << 22;
            instruction |= (dest.encoding >> 1) << 12;
        }
        emitInt(instruction);
    }

    public final void vpop(ConditionFlag cond, CiRegister first, CiRegister last, CiKind firstKind, CiKind lastKind) {
	if(maxineflush != null) {
		for(int i = first.encoding; i <= last.encoding;i++)	{
                	//instrument(true,true,true,ARMV7.r13,-4*(i-first.encoding) );
		}
        }

        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(!first.isCpu() && !last.isCpu(), "vpop No core regs allowed");
        checkConstraint(!(firstKind.isDouble() && !lastKind.isDouble()) && !(!firstKind.isDouble() && lastKind.isDouble()), "vpush no mix of FP/DP allowed");
        checkConstraint(last.number >= first.number, "vpop at least ONE register!!");
        int sz = 0;
        if (firstKind.isDouble()) {
            sz = 1;
        }
        if (sz == 1) {
            instruction |= 0x0cbd0b00;
            instruction |= (first.encoding & 0xf) << 12;
            instruction |= (last.encoding - first.encoding + 1) << 1;
        } else {
            instruction |= 0x0cbd0a00;
            instruction |= (first.encoding & 0x1) << 22;
            instruction |= (first.encoding >> 1) << 12;
            instruction |= (last.encoding - first.encoding + 1);
        }
        emitInt(instruction);
    }

    public final void vpush(ConditionFlag cond, CiRegister first, CiRegister last, CiKind firstKind, CiKind lastKind) {
	if(maxineflush != null) {
                for(int i = first.encoding; i <= last.encoding;i++)     {
                        //instrument(false,true,true,ARMV7.r13,4*(i-first.encoding) );
                }
        }

        int instruction = (cond.value() & 0xf) << 28;
        checkConstraint(!first.isCpu() && !last.isCpu(), "vpush No core regs allowed");
        checkConstraint(!(firstKind.isDouble() && !lastKind.isDouble()) && !(!firstKind.isDouble() && lastKind.isDouble()), "vpush no mix of FP/DP allowed");
        checkConstraint(last.number >= first.number, "vpush at least one register!!");
        int sz = 0;
        if (firstKind.isDouble()) {
            sz = 1;
        }
        if (sz == 1) {
            instruction |= 0x0d2d0b00;
            instruction |= (first.encoding & 0xf) << 12;
            instruction |= (last.encoding - first.encoding + 1) << 1;
        } else {
            instruction |= 0x0d2d0a00;
            instruction |= (first.encoding & 0x1) << 22;
            instruction |= (first.encoding >> 1) << 12;
            instruction |= (last.encoding - first.encoding + 1);
        }
        emitInt(instruction);
    }

    public final void mul(ConditionFlag cond, boolean setFlags, CiRegister dest, CiRegister rn, CiRegister rm) {
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
        if(FLOAT_IDIV) {
            floatDIV(true,cond,dest,rn,rm);
            return;
        }
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0710f010;
        instruction |= (rm.encoding & 0xf) << 8;
        instruction |= (dest.encoding & 0xf) << 16;
        instruction |= rn.encoding & 0xf;
        emitInt(instruction);
    }
    public final void floatDIV(boolean signed, ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {

        push(ConditionFlag.Always, 1 << 8 | 1 << 9);
        vmrs(ConditionFlag.Always, ARMV7.r8);
        mov32BitConstant(ARMV7.r9, 0xc00000);
        orr(ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, ARMV7.r9, 0, 0);
        vmsr(ConditionFlag.Always, ARMV7.r8);
        pop(ConditionFlag.Always, 1 << 8 | 1 << 9);

        vpush(ConditionFlag.Always, ARMV7.s14, ARMV7.s15, CiKind.Double, CiKind.Double);
        vmov(ConditionFlag.Always, ARMV7.s28, rn, null, CiKind.Float, CiKind.Int);
        vmov(ConditionFlag.Always, ARMV7.s30, rm, null, CiKind.Float, CiKind.Int);
        vcvt(ConditionFlag.Always, ARMV7.s14, false, signed, ARMV7.s28, CiKind.Double, CiKind.Int);
        vcvt(ConditionFlag.Always, ARMV7.s15, false, signed, ARMV7.s30, CiKind.Double, CiKind.Int);
        vdiv(ConditionFlag.Always, ARMV7.s14, ARMV7.s14, ARMV7.s15, CiKind.Double);
        vcvt(ConditionFlag.Always, ARMV7.s28, true, signed, ARMV7.s14, CiKind.Int, CiKind.Double);// rounding?
        vmov(cond, dest, ARMV7.s28, null, CiKind.Int, CiKind.Float);
        vpop(ConditionFlag.Always, ARMV7.s14, ARMV7.s15, CiKind.Double, CiKind.Double);


    }

    public final void udiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm) {
        // A8.8.248
        // TODO we need a subroutine for this as most of the ARM hardware we have will not
        // have a hardware integer unit, so the instruction will be undefined/not implemented.

        if (FLOAT_IDIV) {
            floatDIV(false,cond,dest,rn,rm);
            return;

        }
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0730f010;
        instruction |= (rm.encoding & 0xf) << 8;
        instruction |= (dest.encoding & 0xf) << 16;
        instruction |= rn.encoding & 0xf;
        emitInt(instruction);
    }

    public final void vdiv(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e800a00;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vdiv no core registers allowed");
        checkConstraint((dest.number <= 31 && rn.number <= 31 && rm.number <= 31) || (dest.number <= 63 && rn.number <= 63 && rm.number <= 63), "vdiv all registers must be SP OR DP no mix allowed");
        int sz = 0;
        if (destKind.isDouble()) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= rm.encoding & 0xf;
        } else {
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (dest.encoding & 1) << 22;
            instruction |= (rn.encoding & 1) << 7;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= (rm.encoding & 1) << 5;
            instruction |= (rm.encoding >> 1);
        }
        emitInt(instruction);
    }

    public final void vsub(ConditionFlag cond, CiRegister dest, CiRegister rn, CiRegister rm, CiKind destKind) {
        int instruction = (cond.value() & 0xf) << 28;
        instruction |= 0x0e300a40;
        checkConstraint(dest.number >= 16 && rn.number >= 16 && rm.number >= 16, "vsub NO CORE REGISTERS ALLOWED");
        checkConstraint((dest.number <= 31 && rn.number <= 31 && rm.number <= 31) || (dest.number <= 63 && rn.number <= 63 && rm.number <= 63), "vsub ALL REGISTERS must be SP OR DP no mix allowed");
        int sz = 0;
        if (destKind.isDouble()) {
            sz = 1;
        }
        instruction |= sz << 8;
        if (sz == 1) {
            instruction |= (rn.encoding & 0xf) << 16;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= rm.encoding & 0xf;
        } else {
            instruction |= (rm.encoding & 0x1) << 5;
            instruction |= rm.encoding >> 1;
            instruction |= (rn.encoding >> 1) << 16;
            instruction |= (rn.encoding & 0x1) << 7;
            instruction |= (dest.encoding >> 1) << 12;
            instruction |= (dest.encoding & 0x1) << 22;
        }
        emitInt(instruction);
    }

    public final void vmov(ConditionFlag cond, CiRegister dest, CiRegister src, CiRegister src2, CiKind destKind, CiKind srcKind) {
        int instruction = (cond.value() & 0xf) << 28;
        int vmovSameType = 0x0eb00a40; // A8.8.340
        int vmovSingleCore = 0x0e000a10; // A8.8.343 full word only // ARM core to scalar
        int vmovDoubleCore = 0x0c400b10; // A8.8.345 // TWO ARM core to doubleword extension
        if (srcKind.isDouble() && destKind.isDouble()) {
            instruction |= (1 << 8) | vmovSameType;
            instruction |= (dest.encoding >> 4) << 22;
            instruction |= (dest.encoding & 0xf) << 12;
            instruction |= (src.encoding >> 4) << 5;
            instruction |= (src.encoding & 0xf);
        } else if (srcKind.isFloat() && destKind.isFloat()) {
            // dest LSB bit 22, and 12-15
            // src LSB 5 and 0-3
            instruction |= vmovSameType;
            instruction |= ((dest.encoding >> 1) << 12) | ((dest.encoding & 0x1) << 22);
            instruction |= (src.encoding >> 1) | ((src.encoding & 0x1) << 5);
        } else if ((destKind.isGeneral() || srcKind.isGeneral()) && (srcKind.isFloat() || destKind.isFloat())) {
            instruction |= vmovSingleCore;
            if (dest.number <= 15) {
                instruction |= (1 << 20) | ((src.encoding & 1) << 7) | (dest.encoding << 12) | ((src.encoding >> 1) << 16);
            } else {
                instruction |= (src.encoding << 12) | ((dest.encoding >> 1) << 16) | ((dest.encoding & 0x1) << 7);
            }
        } else if ((srcKind.isDouble() && destKind.isGeneral()) || (destKind.isDouble() && srcKind.isGeneral())) {
            instruction |= vmovDoubleCore;
            if (dest.isGeneral()) { // to ARM
                checkConstraint((dest.encoding) <= 14, "vmov doubleword to core destination register > 14");
                instruction |= 1 << 20;
                instruction |= dest.encoding << 12;
                instruction |= (dest.encoding + 1) << 16;
                instruction |= src.encoding;
            } else {
                assert src2 != null;
                checkConstraint((src.encoding) <= 14, "vmov core to doubleword core register > 14");
                instruction |= src2.encoding << 16;
                instruction |= src.encoding << 12;
                instruction |= (dest.encoding >> 4) << 5;
                instruction |= dest.encoding & 0xf;
            }
        }
        emitInt(instruction);
    }

    public final void vmovImm(ConditionFlag cond, CiRegister dst, int imm, CiKind dstKind) {
        int instruction = 0xEB00A00;
        assert imm == (long) imm : "Immediate must be size int";
        instruction |= (cond.value() & 0xf) << 28;
        int size = 0;
        if (dstKind.isDouble()) {
            size = 1;
        }
        instruction |= size << 8;
        instruction |= (imm >> 4) << 16;
        instruction |= (imm & 0xf);
        if (size == 1) {
            instruction |= (dst.encoding >> 4) << 22;
            instruction |= (dst.encoding & 0xf) << 12;
        } else {
            instruction |= (dst.encoding >> 1) << 12;
            instruction |= (dst.encoding << 4) << 22;
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
