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
package com.sun.max.asm.amd64;

import com.sun.max.asm.x86.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * Aliases for 64-bit AMD64 general registers to be used as base registers.
 * 
 * @author Bernd Mathiske
 */
public enum AMD64BaseRegister64 implements GeneralRegister<AMD64BaseRegister64> {

    RAX_BASE,
    RCX_BASE,
    RDX_BASE,
    RBX_BASE,
    RSP_BASE,
    RBP_BASE,
    RSI_BASE,
    RDI_BASE,
    R8_BASE,
    R9_BASE,
    R10_BASE,
    R11_BASE,
    R12_BASE,
    R13_BASE,
    R14_BASE,
    R15_BASE;

    public static final Enumerator<AMD64BaseRegister64> ENUMERATOR = new Enumerator<AMD64BaseRegister64>(AMD64BaseRegister64.class);

    public static AMD64BaseRegister64 from(GeneralRegister generalRegister) {
        return ENUMERATOR.get(generalRegister.id());
    }

    public WordWidth width() {
        return WordWidth.BITS_64;
    }

    public int id() {
        return ordinal();
    }

    public int value() {
        return id();
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return AMD64GeneralRegister64.from(this).externalValue();
    }

    public String disassembledValue() {
        return AMD64GeneralRegister64.from(this).disassembledValue();
    }

    public Enumerator<AMD64BaseRegister64> enumerator() {
        return ENUMERATOR;
    }

    public AMD64BaseRegister64 exampleValue() {
        return RBX_BASE;
    }
}
