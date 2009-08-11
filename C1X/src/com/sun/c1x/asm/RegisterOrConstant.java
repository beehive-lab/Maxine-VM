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
package com.sun.c1x.asm;

import com.sun.c1x.target.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class RegisterOrConstant {

    public final Register register;
    public final int constant;


    public RegisterOrConstant(int i) {
        this.constant = i;
        this.register = Register.noreg;
    }

    public RegisterOrConstant(Register r) {
        assert r != Register.noreg;
        this.constant = 0;
        this.register = r;
    }

    public boolean isConstant() {
        return register == Register.noreg;
    }

    public Register asRegister() {
        return register;
    }

    public int constantOrZero() {
        return constant;
    }

    public boolean isRegister() {
        return register != Register.noreg;
    }

    public int asConstant() {
        assert isConstant();
        return constant;
    }

    public Register registerOrNoReg() {
        return register;
    }

}
