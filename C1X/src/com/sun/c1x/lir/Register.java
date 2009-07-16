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

/**
 * The <code>Register</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class Register {

    public static final int vregBase = 50;
    public final int number;
    public final String name;
    public static final Register anyReg = new Register(-1, "any");

    protected Register(int number, String name) {
        this.number = number;
        this.name = name;
    }

    public boolean isValid() {
        // TODO Check if this implementation is correct?
        return number >= 0;
    }

    public boolean isNoReg() {
        return number == -1;
    }

    public boolean hasByteRegister() {
        // TODO Auto-generated method stub
        return false;
    }

    public static boolean assertDifferentRegisters(Register... reg) {

        for (int i = 0; i < reg.length; i++) {
            for (int j = 0; j < reg.length; j++) {
                if (i != j) {
                    if (reg[i] == reg[j]) {
                        assert false : "Registers " + i + " and " + j + " are both " + reg[i];
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public String name() {
        return name;
    }

    public boolean isFpu() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isXmm() {
        // TODO Auto-generated method stub
        return false;
    }

    public VMReg asVMReg() {
        // TODO Auto-generated method stub
        return null;
    }
}
