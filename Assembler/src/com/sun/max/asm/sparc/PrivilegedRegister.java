/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The class defining the symbolic identifiers for the privileged registers
 * accessed by the Read Privileged Register and Write Privileged Register
 * instructions.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class PrivilegedRegister extends AbstractSymbolicArgument {

    PrivilegedRegister(int value) {
        super(value);
    }

    public static class Writable extends PrivilegedRegister {
        Writable(int value) {
            super(value);
        }
    }

    public static final Writable TPC = new Writable(0);
    public static final Writable TNPC = new Writable(1);
    public static final Writable TSTATE = new Writable(2);
    public static final Writable TT = new Writable(3);
    public static final Writable TTICK = new Writable(4) {
        @Override
        public String externalValue() {
            return "%tick";
        }
    };
    public static final Writable TBA = new Writable(5);
    public static final Writable PSTATE = new Writable(6);
    public static final Writable TL = new Writable(7);
    public static final Writable PIL = new Writable(8);
    public static final Writable CWP = new Writable(9);
    public static final Writable CANSAVE = new Writable(10);
    public static final Writable CANRESTORE = new Writable(11);
    public static final Writable CLEANWIN = new Writable(12);
    public static final Writable OTHERWIN = new Writable(13);
    public static final Writable WSTATE = new Writable(14);
    public static final PrivilegedRegister FQ = new PrivilegedRegister(15);
    public static final PrivilegedRegister VER = new PrivilegedRegister(31);

    public static final Symbolizer<PrivilegedRegister> SYMBOLIZER = Symbolizer.Static.initialize(PrivilegedRegister.class);
    public static final Symbolizer<Writable> WRITE_ONLY_SYMBOLIZER = Symbolizer.Static.initialize(PrivilegedRegister.class, Writable.class);
}
