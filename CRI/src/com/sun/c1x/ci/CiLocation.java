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

/**
 * This class represents either a register or a stack slot and is used when communicating the
 * locations of parameters for calling conventions across the compiler/runtime interface.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class CiLocation extends CiValue {

    /**
     * Singleton object representing an invalid location.
     */
    public static final CiLocation InvalidLocation = new CiStackLocation(CiKind.Illegal, 0, 0, false);

    protected CiLocation(CiKind kind) {
        super(kind);
    }

    public boolean isRegister() {
        return this instanceof CiRegisterLocation;
    }

    public boolean isStack() {
        return this instanceof CiStackLocation;
    }

    public boolean isValid() {
        return kind != CiKind.Illegal;
    }

    public CiRegister register() {
        return ((CiRegisterLocation) this).register;
    }

    public int stackOffset() {
        return ((CiStackLocation) this).stackOffset;
    }

    public int stackSize() {
        return ((CiStackLocation) this).stackSize;
    }

    public boolean isCallerFrame() {
        return ((CiStackLocation) this).callerFrame;
    }
}
