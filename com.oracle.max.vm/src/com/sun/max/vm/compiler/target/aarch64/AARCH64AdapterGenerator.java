package com.sun.max.vm.compiler.target.aarch64;

import com.sun.cri.ci.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;


public class AARCH64AdapterGenerator extends AdapterGenerator {

    final CiRegister scratch;

    public AARCH64AdapterGenerator(Adapter.Type adapterType) {
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
