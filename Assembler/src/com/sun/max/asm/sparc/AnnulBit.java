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
 * An optional (suffix) argument to a branch assembler instruction specifying
 * if the instruction in the delay slot of the branch will be executed. For example:
 * <pre>bne,a <i>label</i></pre>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class AnnulBit extends OptionSuffixSymbolicArgument {

    private AnnulBit(int value, String externalMnemonicSuffix) {
        super(value, externalMnemonicSuffix);
    }

    /**
     * The annul bit is not set.
     */
    public static final AnnulBit NO_A = new AnnulBit(0, "");

    /**
     * The annul bit is set.
     */
    public static final AnnulBit A = new AnnulBit(1, ",a");

    public static final Symbolizer<AnnulBit> SYMBOLIZER = Symbolizer.Static.initialize(AnnulBit.class);

}
