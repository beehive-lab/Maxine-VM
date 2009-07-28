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

    public RegisterOrConstant(int i) {
        // TODO Auto-generated constructor stub
    }

    public RegisterOrConstant(long l) {
        // TODO Auto-generated constructor stub
    }

    public RegisterOrConstant(Register tmp) {
        // TODO Auto-generated constructor stub
    }

    public boolean isConstant() {
        // TODO Auto-generated method stub
        return false;
    }

    public Register asRegister() {
        // TODO Auto-generated method stub
        return null;
    }

    public int constantOrZero() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isRegister() {
        // TODO Auto-generated method stub
        return false;
    }

    public int asConstant() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Register registerOrNoReg() {
        // TODO Auto-generated method stub
        return null;
    }

}
