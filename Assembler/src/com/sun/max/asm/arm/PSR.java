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
 * Program status registers.
 * 
 * @author Sumeet Panchal
 */

public enum PSR implements EnumerableArgument<PSR>{

    CPSR, SPSR;

    public static final Enumerator<PSR> ENUMERATOR = new Enumerator<PSR>(PSR.class);

    public int value() {
        return ordinal();
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return Integer.toString(ordinal());
    }

    public String disassembledValue() {
        return externalValue();
    }

    public Enumerator<PSR> enumerator() {
        return ENUMERATOR;
    }

    public PSR exampleValue() {
        return CPSR;
    }
}
