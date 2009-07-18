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
package com.sun.c1x.ci;

import com.sun.c1x.lir.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public final class CiLocation {

    public final Register first;
    public final Register second;
    public final int stackOffset;

    public CiLocation(Register register) {
        first = register;
        second = null;
        stackOffset = 0;
    }

    public CiLocation(Register first, Register second) {
        this.first = first;
        this.second = second;
        stackOffset = 0;
    }

    public CiLocation(int stackOffset) {
        assert stackOffset > 0;
        this.first = null;
        this.second = null;
        this.stackOffset = stackOffset;
    }

    public boolean isSingleRegister() {
        return second == null && first != null;
    }

    public boolean isDoubleRegister() {
        return second != null;
    }

    public boolean isStackOffset() {
        return stackOffset > 0;
    }

    // From VMRegImpl

    public static int stackSlotSize = 4;

    public static int slotsPerWord(int wordSize) {
        return wordSize / stackSlotSize;
    }

    /**
     * @return
     */
    public static CiLocation bad() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param i
     * @return
     */
        // TODO Auto-generated method stub
    public static CiLocation asVMReg(int i) {
        return null;
    }

    /**
     * @return
     */
    public int value() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @return
     */
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isValid() {
        // TODO Auto-generated method stub
        return true;
    }

    public CiLocation next() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return
     */
    public boolean isReg() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @return
     */
    public boolean isStack() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @return
     */
    public int reg2stack() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @param tty
     */
    public void printOn(LogStream tty) {
        // TODO Auto-generated method stub

    }

    /**
     * @param contentReg
     * @param b
     * @return
     */
    public static CiLocation asVMReg(short contentReg, boolean b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param i
     * @return
     */
    public static OopMapValue stack2reg(int i) {
        // TODO Auto-generated method stub
        return null;
    }

}
