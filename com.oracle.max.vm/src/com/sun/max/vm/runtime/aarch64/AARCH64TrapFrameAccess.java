package com.sun.max.vm.runtime.aarch64;

import static com.oracle.max.asm.target.aarch64.AARCH64.*;
import static com.oracle.max.asm.target.amd64.AMD64.*;

import com.sun.cri.ci.*;


public class AARCH64TrapFrameAccess {

    public static final int TRAP_NUMBER_OFFSET;
    public static final int FLAGS_OFFSET;

    public static final CiCalleeSaveLayout CSL;

    static {
        CiRegister[] csaRegs = {
            x10, x11, x12, x13, x14, x15, x16, x17, x18, x19,
            x20, x21, x22, x23, x24, x25, x26, x27, x28, /*x29,*/
            x30
        };

        int size = (16 * 8) + (16 * 16);
        TRAP_NUMBER_OFFSET = size;
        size += 8;
        FLAGS_OFFSET = size;
        size += 8;

        CSL = new CiCalleeSaveLayout(0, size, 8, csaRegs);
    }


}
