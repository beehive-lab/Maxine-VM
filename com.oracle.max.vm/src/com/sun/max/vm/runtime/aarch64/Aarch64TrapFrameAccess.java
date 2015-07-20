package com.sun.max.vm.runtime.aarch64;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;


public class Aarch64TrapFrameAccess extends TrapFrameAccess {

    @Override
    public Pointer getPCPointer(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer getSP(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer getFP(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer getSafepointLatch(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSafepointLatch(Pointer trapFrame, Pointer value) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getTrapNumber(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Pointer getCalleeSaveArea(Pointer trapFrame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTrapNumber(Pointer trapFrame, int trapNumber) {
        // TODO Auto-generated method stub

    }

    @Override
    public void logTrapFrame(Pointer trapFrame) {
        // TODO Auto-generated method stub

    }

}
