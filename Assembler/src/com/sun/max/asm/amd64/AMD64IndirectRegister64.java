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
 * Aliases for 64-bit AMD64 general registers to be used for indirect addressing.
 * 
 * @author Bernd Mathiske
 */
public enum AMD64IndirectRegister64 implements GeneralRegister<AMD64IndirectRegister64>, IndirectRegister {

    RAX_INDIRECT,
    RCX_INDIRECT,
    RDX_INDIRECT,
    RBX_INDIRECT,
    RSP_INDIRECT,
    RBP_INDIRECT,
    RSI_INDIRECT,
    RDI_INDIRECT,
    R8_INDIRECT,
    R9_INDIRECT,
    R10_INDIRECT,
    R11_INDIRECT,
    R12_INDIRECT,
    R13_INDIRECT,
    R14_INDIRECT,
    R15_INDIRECT;

    public static final Enumerator<AMD64IndirectRegister64> ENUMERATOR = new Enumerator<AMD64IndirectRegister64>(AMD64IndirectRegister64.class);

    public static AMD64IndirectRegister64 from(GeneralRegister generalRegister) {
        return ENUMERATOR.get(generalRegister.id());
    }

    public int id() {
        return ordinal();
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

    public Enumerator<AMD64IndirectRegister64> enumerator() {
        return ENUMERATOR;
    }

    public AMD64IndirectRegister64 exampleValue() {
        return RBX_INDIRECT;
    }
}
