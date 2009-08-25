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

import com.sun.c1x.target.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public final class CiLocation {

    public static final CiLocation InvalidLocation = new CiLocation();

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

    private CiLocation() {
        this.first = null;
        this.second = null;
        this.stackOffset = 0;
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

    public boolean isRegister() {
        return isSingleRegister() || isDoubleRegister();
    }

    public boolean isStackOffset() {
        return stackOffset > 0;
    }

    public boolean isValid() {
        return isStackOffset() || isRegister();

    }

    @Override
    public String toString() {
        if (isSingleRegister()) {
            return first.name;
        } else if (isDoubleRegister()) {
            return first.name + "+" + second.name;
        } else if (isStackOffset()) {
            return "STACKED REG";
        } else {
            assert !this.isValid();
            return "BAD";
        }
    }
}
