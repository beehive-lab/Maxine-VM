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
package com.sun.c1x.target;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;

/**
 * The <code>Register</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public final class Register {

    // Invalid register
    public static final Register noreg = new Register(-1, -1, "noreg");

    public static final int vregBase = 40;

    public final int number;
    public final String name;
    public final int encoding;
    private final int flags;

    public enum RegisterFlag {
        CPU, Byte, XMM, MMX;

        public int mask() {
            return 1 << (ordinal() + 1);
        }
    }

    public Register(int number, int encoding, String name, RegisterFlag... flags) {
        this.number = number;
        this.name = name;
        this.flags = createMask(flags);
        this.encoding = encoding;
    }

    private int createMask(RegisterFlag... flags) {
        int result = 0;
        for (RegisterFlag f : flags) {
            result |= f.mask();
        }
        return result;
    }

    private boolean checkFlag(RegisterFlag f) {
        return (flags & f.mask()) != 0;
    }

    public boolean isValid() {
        return number >= 0;
    }

    public boolean isXMM() {
        return checkFlag(RegisterFlag.XMM);
    }

    public CiLocation asVMReg() {
        return Util.nonFatalUnimplemented(null);
    }

    public boolean isCpu() {
        return checkFlag(RegisterFlag.CPU);
    }

    public boolean isByte() {
        return checkFlag(RegisterFlag.Byte);
    }

    public boolean isMMX() {
        return checkFlag(RegisterFlag.MMX);
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
}
