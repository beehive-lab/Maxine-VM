package com.oracle.max.asm.target.aarch64;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class AARCH64MacroAssembler extends AARCH64Assembler {
    public AARCH64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public final void nop() {
        emitByte(0x90);
    }
}
