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
/*VCSID=9146dc07-95b9-4314-924e-588fb0a88dcd*/
package com.sun.max.asm.ia32;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * @author Andreas Gal
 */
public enum IA32XMMRegister implements EnumerableArgument<IA32XMMRegister> {

    XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7;

    public static final Enumerator<IA32XMMRegister> ENUMERATOR = new Enumerator<IA32XMMRegister>(IA32XMMRegister.class);
    public int value() {
        return ordinal();
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return "%" + name().toLowerCase();
    }

    public String disassembledValue() {
        return name().toLowerCase();
    }

    public Enumerator<IA32XMMRegister> enumerator() {
        return ENUMERATOR;
    }

    public IA32XMMRegister exampleValue() {
        return XMM0;
    }
}
