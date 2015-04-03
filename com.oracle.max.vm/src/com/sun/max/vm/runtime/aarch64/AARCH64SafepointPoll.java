package com.sun.max.vm.runtime.aarch64;

import static com.sun.max.platform.Platform.*;

import com.oracle.max.asm.target.aarch64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;


public class AARCH64SafepointPoll extends SafepointPoll {
    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final CiRegister LATCH_REGISTER = AARCH64.x30;

    @HOSTED_ONLY
    public AARCH64SafepointPoll() {
    }

    @HOSTED_ONLY
    @Override
    protected byte[] createCode() {
        final AARCH64Assembler asm = new AARCH64Assembler(target(), null);
        //asm.movq(LATCH_REGISTER, new CiAddress(WordUtil.archKind(), LATCH_REGISTER.asValue()));
        return asm.codeBuffer.close(true);
    }

}
