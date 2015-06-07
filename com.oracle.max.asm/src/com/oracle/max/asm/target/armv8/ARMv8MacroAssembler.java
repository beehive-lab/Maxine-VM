package com.oracle.max.asm.target.armv8;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class ARMv8MacroAssembler extends ARMv8Assembler {
    public ARMv8MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public final void nop() {
        emitByte(0x90);
    }
}
