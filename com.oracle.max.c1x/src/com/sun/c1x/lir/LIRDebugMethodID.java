package com.sun.c1x.lir;

import com.sun.cri.ci.*;


public class LIRDebugMethodID extends LIRInstruction {

    final int bci;
    final String parentMethod;
    final String inlinedMethod;

    public LIRDebugMethodID(int bci, String parentMethod, String inlinedMethod) {
        super(LIROpcode.DebugMethodID, CiValue.IllegalValue, null, false);
        this.bci = bci;
        this.parentMethod = parentMethod;
        this.inlinedMethod = inlinedMethod;
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitDebugID(parentMethod, inlinedMethod);
    }
}
