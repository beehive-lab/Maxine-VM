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

package com.sun.max.asm.arm;

import com.sun.max.util.*;

/**
 * General purpose registers.
 * 
 * @author Sumeet Panchal
 */

public final class GPR extends ZeroOrRegister{
    private GPR(String name, int value) {
        super(name, value);
    }

    @Override
    public String externalValue() {
        return name().toLowerCase();
    }

    public static final GPR R0 = new GPR("R0", 0);
    public static final GPR R1 = new GPR("R1", 1);
    public static final GPR R2 = new GPR("R2", 2);
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
    public static final GPR PC = new GPR("PC", 15); //Program counter is also one of the GPRs, namely R15

    public static final Symbolizer<GPR> GPR_SYMBOLIZER =
        Symbolizer.Static.from(GPR.class, R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, PC);

}
