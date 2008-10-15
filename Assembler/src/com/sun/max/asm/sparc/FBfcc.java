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
 * The argument to a Branch on Floating-Point Condition Code instruction specifying
 * the conditional test to be performed.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class FBfcc extends NameSuffixSymbolicArgument implements Predicate<FCCOperand, FBfcc> {
    private FBfcc _negation;

    private FBfcc(int value) {
        super(value);
    }
    private FBfcc(int value, FBfcc negation) {
        this(value);
        _negation = negation;
        negation._negation = this;
    }

    public static final FBfcc A = new FBfcc(8);
    public static final FBfcc N = new FBfcc(0, A);
    public static final FBfcc U = new FBfcc(7);
    public static final FBfcc G = new FBfcc(6);
    public static final FBfcc UG = new FBfcc(5);
    public static final FBfcc L = new FBfcc(4);
    public static final FBfcc UL = new FBfcc(3);
    public static final FBfcc LG = new FBfcc(2);
    public static final FBfcc NE = new FBfcc(1);
    public static final FBfcc E = new FBfcc(9, NE);
    public static final FBfcc UE = new FBfcc(10, LG);
    public static final FBfcc GE = new FBfcc(11, UL);
    public static final FBfcc UGE = new FBfcc(12, L);
    public static final FBfcc LE = new FBfcc(13, UG);
    public static final FBfcc ULE = new FBfcc(14, G);
    public static final FBfcc O = new FBfcc(15, U);

    public static final Symbolizer<FBfcc> SYMBOLIZER = Symbolizer.Static.initialize(FBfcc.class);
    public FBfcc negate() { return _negation; }
}
