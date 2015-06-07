package com.sun.max.vm.compiler.target.armv8;

import com.sun.cri.ci.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;


public class ARMv8AdapterGenerator extends AdapterGenerator {

    final CiRegister scratch;

    public ARMv8AdapterGenerator(Adapter.Type adapterType) {
        super(adapterType);
        scratch = opt.getScratchRegister();
    }

    @Override
    public int prologueSizeForCallee(ClassMethodActor callee) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean advanceIfInPrologue(StackFrameCursor current) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected int emitPrologue(Object out, Adapter adapter) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected Adapter create(Sig sig) {
        // TODO Auto-generated method stub
        return null;
    }

}
