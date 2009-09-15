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
 * Aliases for 32-bit AMD64 general registers to be used as index registers.
 * 
 * @author Bernd Mathiske
 */
public enum AMD64IndexRegister32 implements GeneralRegister<AMD64IndexRegister32> {

    EAX_INDEX,
    ECX_INDEX,
    EDX_INDEX,
    EBX_INDEX,
    // no ESP_INDEX!
    EBP_INDEX,
    ESI_INDEX,
    EDI_INDEX,
    R8D_INDEX,
    R9D_INDEX,
    R10D_INDEX,
    R11D_INDEX,
    R12D_INDEX,
    R13D_INDEX,
    R14D_INDEX,
    R15D_INDEX;

    public static final Enumerator<AMD64IndexRegister32> ENUMERATOR = new Enumerator<AMD64IndexRegister32>(AMD64IndexRegister32.class);

    public static AMD64IndexRegister32 from(GeneralRegister generalRegister) {
        int ordinal = generalRegister.id();
        if (ordinal >= AMD64GeneralRegister32.ESP.id()) {
            ordinal--;
        }
        return ENUMERATOR.get(ordinal);
    }

    public WordWidth width() {
        return WordWidth.BITS_32;
    }

    public int id() {
        int ordinal = ordinal();
        if (ordinal >= AMD64GeneralRegister32.ESP.id()) {
            ordinal++;
        }
        return ordinal;
    }

    public int value() {
        return id();
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return AMD64GeneralRegister32.from(this).externalValue();
    }

    public String disassembledValue() {
        return AMD64GeneralRegister32.from(this).disassembledValue();
    }

    public Enumerator<AMD64IndexRegister32> enumerator() {
        return ENUMERATOR;
    }

    public AMD64IndexRegister32 exampleValue() {
        return ESI_INDEX;
    }
}
