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
 * Aliases for 64-bit AMD64 general registers to be used as index registers.
 * 
 * @author Bernd Mathiske
 */
public enum AMD64IndexRegister64 implements GeneralRegister<AMD64IndexRegister64> {

    RAX_INDEX,
    RCX_INDEX,
    RDX_INDEX,
    RBX_INDEX,
    // no RSP_INDEX!
    RBP_INDEX,
    RSI_INDEX,
    RDI_INDEX,
    R8_INDEX,
    R9_INDEX,
    R10_INDEX,
    R11_INDEX,
    R12_INDEX,
    R13_INDEX,
    R14_INDEX,
    R15_INDEX;

    public static AMD64IndexRegister64 from(GeneralRegister generalRegister) {
        int ordinal = generalRegister.id();
        if (ordinal >= AMD64GeneralRegister64.RSP.id()) {
            ordinal--;
        }
        return ENUMERATOR.get(ordinal);
    }

    public int id() {
        int ordinal = ordinal();
        if (ordinal >= AMD64GeneralRegister64.RSP.id()) {
            ordinal++;
        }
        return ordinal;
    }

    public WordWidth width() {
        return WordWidth.BITS_64;
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

    public Enumerator<AMD64IndexRegister64> enumerator() {
        return ENUMERATOR;
    }

    public AMD64IndexRegister64 exampleValue() {
        return RSI_INDEX;
    }

    public static final Enumerator<AMD64IndexRegister64> ENUMERATOR = new Enumerator<AMD64IndexRegister64>(AMD64IndexRegister64.class);
}
