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
/*VCSID=4afcfb1e-4352-4e01-bfac-dc16618a50cd*/
package com.sun.max.asm.ia32;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * @author Andreas Gal
 */
public enum IA32XMMComparison implements EnumerableArgument<IA32XMMComparison> {

    EQUAL,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL,
    UNORDERED,
    NOT_EQUAL,
    NOT_LESS_THAN,
    NOT_LESS_THAN_OR_EQUAL,
    ORDERED;

    public int value() {
        return ordinal();
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return "$" + Integer.toString(value());
    }

    public String disassembledValue() {
        return name().toLowerCase();
    }

    public Enumerator<IA32XMMComparison> enumerator() {
        return ENUMERATOR;
    }

    public IA32XMMComparison exampleValue() {
        return LESS_THAN_OR_EQUAL;
    }

    public static final Enumerator<IA32XMMComparison> ENUMERATOR = new Enumerator<IA32XMMComparison>(IA32XMMComparison.class);

}
