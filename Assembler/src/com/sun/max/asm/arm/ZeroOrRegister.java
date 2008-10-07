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

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The super type of all the {@link GPR General Purpose Registers} and the constant {@link Zero#ZERO}.
 *
 * @author Sumeet Panchal
 */
public abstract class ZeroOrRegister extends AbstractSymbolicArgument {

    ZeroOrRegister(String name, int value) {
        super(name, value);
    }

    /**
     * Determines if this register specifier is outside the range of registers
     * {@code [target .. target+n]} where the range
     * wraps at 32.
     */
    public boolean isOutsideRegisterRange(GPR target, int n) {
        final int rt = target.value();
        final int ra = value();
        final int numRegs = (n + 3) / 4;
        final int lastReg = (rt + numRegs - 1) % 32;
        final boolean wrapsAround = lastReg < rt;
        if (wrapsAround) {
            return lastReg < ra && ra < rt;
        }
        return ra < rt || lastReg < ra;
    }

    public static Symbolizer<ZeroOrRegister> symbolizer() {
        if (_symbolizer == null) {
            _symbolizer = Symbolizer.Static.fromSequence(ZeroOrRegister.class, GPR.GPR_SYMBOLIZER, Zero.ZERO);
        }
        return _symbolizer;
    }

    // This must be lazily constructed to avoid dependency on the GPR class initializer
    private static Symbolizer<ZeroOrRegister> _symbolizer;

}
