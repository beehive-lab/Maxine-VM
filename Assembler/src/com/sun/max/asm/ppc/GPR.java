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
/*VCSID=110cdbb7-648e-4032-88d3-b908891b5b06*/
package com.sun.max.asm.ppc;

import com.sun.max.util.*;

/**
 * The general purpose registers.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class GPR extends ZeroOrRegister {

    private GPR(String name, int value) {
        super(name, value);
    }

    @Override
    public String externalValue() {
        return name().toLowerCase();
    }

    public static final GPR R0 = new GPR("R0", 0);
    public static final GPR SP = new GPR("R1", 1);     // Stack pointer register
    public static final GPR RTOC = new GPR("R2", 2);   // Table Of Contents register
    public static final GPR R3 = new GPR("R3", 3);
    public static final GPR R4 = new GPR("R4", 4);
    public static final GPR R5 = new GPR("R5", 5);
    public static final GPR R6 = new GPR("R6", 6);
    public static final GPR R7 = new GPR("R7", 7);
    public static final GPR R8 = new GPR("R8", 8);
    public static final GPR R9 = new GPR("R9", 9);
    public static final GPR R10 = new GPR("R10", 10);
    public static final GPR R11 = new GPR("R11", 11);
    public static final GPR R12 = new GPR("R12", 12);
    public static final GPR R13 = new GPR("R13", 13);
    public static final GPR R14 = new GPR("R14", 14);
    public static final GPR R15 = new GPR("R15", 15);
    public static final GPR R16 = new GPR("R16", 16);
    public static final GPR R17 = new GPR("R17", 17);
    public static final GPR R18 = new GPR("R18", 18);
    public static final GPR R19 = new GPR("R19", 19);
    public static final GPR R20 = new GPR("R20", 20);
    public static final GPR R21 = new GPR("R21", 21);
    public static final GPR R22 = new GPR("R22", 22);
    public static final GPR R23 = new GPR("R23", 23);
    public static final GPR R24 = new GPR("R24", 24);
    public static final GPR R25 = new GPR("R25", 25);
    public static final GPR R26 = new GPR("R26", 26);
    public static final GPR R27 = new GPR("R27", 27);
    public static final GPR R28 = new GPR("R28", 28);
    public static final GPR R29 = new GPR("R29", 29);
    public static final GPR R30 = new GPR("R30", 30);
    public static final GPR R31 = new GPR("R31", 31);

    public static final Symbolizer<GPR> GPR_SYMBOLIZER = Symbolizer.Static.from(GPR.class, R0, SP, RTOC, R3, R4, R5, R6, R7, R8, R9, R10,
                    R11, R12, R13, R14, R15, R16, R17, R18, R20, R21, R22, R23, R24, R25, R26, R27, R28, R29, R30, R31);
}
