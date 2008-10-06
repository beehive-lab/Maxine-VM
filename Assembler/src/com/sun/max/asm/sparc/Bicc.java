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
/*VCSID=2366e26d-3152-4a4b-a568-15e1e9c35d01*/
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The argument to a Branch on Integer Condition Code instruction specifying
 * the conditional test to be performed.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class Bicc extends NameSuffixSymbolicArgument implements Predicate<ICCOperand, Bicc> {
    private Bicc _negation;

    private Bicc(int value) {
        super(value);
    }

    private Bicc(int value, Bicc negation) {
        this(value);
        _negation = negation;
        negation._negation = this;
    }

    public static final Bicc A = new Bicc(8);
    public static final Bicc N = new Bicc(0, A);
    public static final Bicc NE = new Bicc(9);
    public static final Bicc E = new Bicc(1, NE);
    public static final Bicc G = new Bicc(10);
    public static final Bicc LE = new Bicc(2, G);
    public static final Bicc GE = new Bicc(11);
    public static final Bicc L = new Bicc(3, GE);
    public static final Bicc GU = new Bicc(12);
    public static final Bicc LEU = new Bicc(4, GU);
    public static final Bicc CC = new Bicc(13);
    public static final Bicc CS = new Bicc(5, CC);
    public static final Bicc POS = new Bicc(14);
    public static final Bicc NEG = new Bicc(6, POS);
    public static final Bicc VC = new Bicc(15);
    public static final Bicc VS = new Bicc(7, VC);

    public static final Symbolizer<Bicc> SYMBOLIZER = Symbolizer.Static.initialize(Bicc.class);

    public Bicc negate() {
        return _negation;
    }

}
