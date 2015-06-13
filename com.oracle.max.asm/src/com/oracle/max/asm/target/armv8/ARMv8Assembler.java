package com.oracle.max.asm.target.armv8;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class ARMv8Assembler extends AbstractAssembler {
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
    public ARMv8Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        // TODO Auto-generated method stub

    }

    public final void movImmediate(CiRegister dst, int imm16) {
        int instruction = 0x52800000;
        instruction |= 1 << 31;
        instruction |= (imm16 & 0xffff) << 5;
        instruction |= (dst.encoding & 0x1f);
        System.out.println(Integer.toHexString(instruction));
        emitInt(instruction);
    }

    public void add(final CiRegister Rd, final CiRegister Rm, final CiRegister Rn) {
        int instruction = 0xB000000;
        instruction |= 1 << 31;
        instruction |= (Rm.encoding & 0x1f) << 16;
        instruction |= (Rn.encoding & 0x1f) << 5;
        instruction |= (Rd.encoding & 0x1f);
        System.out.println(Integer.toHexString(instruction));
        emitInt(instruction);
    }

}
