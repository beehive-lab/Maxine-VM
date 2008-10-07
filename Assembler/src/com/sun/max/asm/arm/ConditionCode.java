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
/*VCSID=45536b9a-ced9-4b1c-b3fc-23f87c803591*/
package com.sun.max.asm.arm;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The condition codes that can be used for the 4-bit condition field in every ARM instruction.
 *
 * @author Doug Simon
 */
public final class ConditionCode extends OptionSuffixSymbolicArgument {

    private ConditionCode(int value, String externalMnemonicSuffix) {
        super(value, externalMnemonicSuffix);
    }

    public static final ConditionCode EQ = new ConditionCode(0, "eq");
    public static final ConditionCode NE = new ConditionCode(1, "ne");
    public static final ConditionCode CS = new ConditionCode(2, "cs");
    public static final ConditionCode HS = new ConditionCode(2, "hs");
    public static final ConditionCode CC = new ConditionCode(3, "cc");
    public static final ConditionCode LO = new ConditionCode(3, "lo");
    public static final ConditionCode MI = new ConditionCode(4, "mi");
    public static final ConditionCode PL = new ConditionCode(5, "pl");
    public static final ConditionCode VS = new ConditionCode(6, "vs");
    public static final ConditionCode VC = new ConditionCode(7, "vc");
    public static final ConditionCode HI = new ConditionCode(8, "hi");
    public static final ConditionCode LS = new ConditionCode(9, "ls");
    public static final ConditionCode GE = new ConditionCode(10, "ge");
    public static final ConditionCode LT = new ConditionCode(11, "lt");
    public static final ConditionCode GT = new ConditionCode(12, "gt");
    public static final ConditionCode LE = new ConditionCode(13, "le");
    public static final ConditionCode AL = new ConditionCode(14, "al");
    public static final ConditionCode NO_COND = new ConditionCode(14, "");
    public static final ConditionCode NV = new ConditionCode(15, "nv");

    public static final Symbolizer<ConditionCode> SYMBOLIZER = Symbolizer.Static.initialize(ConditionCode.class);
}
