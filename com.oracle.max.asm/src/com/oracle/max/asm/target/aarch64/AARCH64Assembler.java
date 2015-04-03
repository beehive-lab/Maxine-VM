package com.oracle.max.asm.target.aarch64;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class AARCH64Assembler extends AbstractAssembler{


    /**
     * The register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    public final CiRegister frameRegister;

    /**
     * Constructs an assembler for the AARCH64 architecture.
     *
     * @param registerConfig the register configuration used to bind {@link CiRegister#Frame} and
     *            {@link CiRegister#CallerFrame} to physical registers. This value can be null if this assembler
     *            instance will not be used to assemble instructions using these logical registers.
     */
    public AARCH64Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.frameRegister = registerConfig == null ? null : registerConfig.getFrameRegister();
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        // TODO Auto-generated method stub

    }


}
