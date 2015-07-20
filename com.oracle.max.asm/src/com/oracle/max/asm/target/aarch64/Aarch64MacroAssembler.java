package com.oracle.max.asm.target.aarch64;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class Aarch64MacroAssembler extends Aarch64Assembler {
    public Aarch64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public final void nop() {
        emitByte(0x90);
    }
}
