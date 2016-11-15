package com.sun.c1x.ir;

import com.oracle.max.criutils.*;
import com.sun.cri.ci.*;

public final class DebugMethodID extends Instruction {

    private final int bci;
    private final String parentMethod;
    private final String inlinedMethod;

    public DebugMethodID(int bci, String parentMethod, String inlinedMethod) {
        super(CiKind.Illegal);
        this.bci = bci;
        this.parentMethod = parentMethod;
        this.inlinedMethod = inlinedMethod;
        setFlag(Flag.LiveSideEffect);
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitDebugMethodID(this);
    }

    @Override
    public void print(LogStream out) {
        // TODO Auto-generated method stub
    }

    public int getBci() {
        return bci;
    }

    public String getParentMethod() {
        return parentMethod;
    }

    public String getInlinedMethod() {
        return inlinedMethod;
    }
}