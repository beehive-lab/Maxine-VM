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
/*VCSID=089c1d28-9bd1-4cac-9a14-237d42a4442a*/
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * An optional (suffix) argument to a branch with prediction assembler instruction
 * specifying if the prediction bit is to be set. For example:
 * <pre>bgu,pt <i>i_or_x_cc</i>, <i>label</i></pre>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class BranchPredictionBit extends OptionSuffixSymbolicArgument {

    private BranchPredictionBit(int value, String externalMnemonicSuffix) {
        super(value, externalMnemonicSuffix);
    }

    /**
     * The prediction bit is not set, indicating that the branch is not likely to be taken.
     */
    public static final BranchPredictionBit PN = new BranchPredictionBit(0, ",pn");

    /**
     * The prediction bit is set, indicating that the branch is likely to be taken.
     */
    public static final BranchPredictionBit PT = new BranchPredictionBit(1, ",pt");

    public static final Symbolizer<BranchPredictionBit> SYMBOLIZER = Symbolizer.Static.initialize(BranchPredictionBit.class);
}
