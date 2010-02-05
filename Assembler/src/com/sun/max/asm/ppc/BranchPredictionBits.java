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
package com.sun.max.asm.ppc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The branch prediction values for the conditional branches whose encoding includes
 * a hint about whether the branch is likely to be taken or is likely not to be taken.
 *
 * @author Doug Simon
 */
public final class BranchPredictionBits extends OptionSuffixSymbolicArgument {

    private BranchPredictionBits(int value, String externalMnemonicSuffix) {
        super(value, externalMnemonicSuffix);
    }

    /**
     * No hint is given.
     */
    public static final BranchPredictionBits NONE = new BranchPredictionBits(0, "");

    /**
     * The branch is very likely to be taken.
     */
    public static final BranchPredictionBits PT = new BranchPredictionBits(3, "++");

    /**
     * The branch is very likely <b>not</b> to be taken.
     */
    public static final BranchPredictionBits PN = new BranchPredictionBits(2, "--");

    public static final Symbolizer<BranchPredictionBits> SYMBOLIZER = Symbolizer.Static.initialize(BranchPredictionBits.class);
}
