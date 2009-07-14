/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.lir;

import com.sun.c1x.target.sparc.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.value.*;


public class FrameMap {

    public CallingConvention runtimeCallingConvention(BasicType[] signature) {
        // TODO Auto-generated method stub
        return null;
    }

    public CallingConvention javaCallingConvention(BasicType[] signature, boolean b) {
        // TODO Auto-generated method stub
        return null;
    }

    public CallingConvention incomingArguments() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param cpuRegnr
     * @return
     */
    public static Register cpuRnr2Reg(int cpuRegnr) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param xmmRegnr
     * @return
     */
    public static XMMRegister nr2XmmReg(int xmmRegnr) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param fpuRegnr
     * @return
     */
    public static FloatRegister nr2FloatReg(int fpuRegnr) {
        // TODO Auto-generated method stub
        return null;
    }

    public Address addressForSlot(int singleStackIx) {
        // TODO Auto-generated method stub
        return null;
    }

    public Address addressForSlot(int doubleStackIx, int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public int reservedArgumentAreaSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Object addressForMonitorLock(int monitorNo) {
        // TODO Auto-generated method stub
        return null;
    }
}
