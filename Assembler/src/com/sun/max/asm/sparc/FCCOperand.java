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
 * The argument to a Branch on Floating-Point Condition Code with Prediction instruction specifying
 * the conditional code to be tested.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class FCCOperand extends AbstractSymbolicArgument implements ConditionCodeRegister {

    private FCCOperand(int value) {
        super(value);
    }

    public static final FCCOperand FCC0 = new FCCOperand(0);
    public static final FCCOperand FCC1 = new FCCOperand(1);
    public static final FCCOperand FCC2 = new FCCOperand(2);
    public static final FCCOperand FCC3 = new FCCOperand(3);

    private static final FCCOperand [] ALL = new FCCOperand[] {FCC0, FCC1, FCC2, FCC3 };
    public static  FCCOperand [] all() { return ALL; }

    public static final Symbolizer<FCCOperand> SYMBOLIZER = Symbolizer.Static.initialize(FCCOperand.class);
}
