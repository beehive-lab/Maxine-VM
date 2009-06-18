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
package com.sun.max.asm.ia32;

import com.sun.max.asm.x86.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public enum IA32GeneralRegister8 implements GeneralRegister<IA32GeneralRegister8> {

    // Note: keep the order such that 'value()' can rely on ordinals:

    AL, CL, DL, BL, AH, CH, DH, BH;

    public static final Enumerator<IA32GeneralRegister8> ENUMERATOR = new Enumerator<IA32GeneralRegister8>(IA32GeneralRegister8.class);

    private static final IA32GeneralRegister8[] lowRegisters = {AL, CL, DL, BL};

    public static IA32GeneralRegister8 lowFrom(GeneralRegister generalRegister) {
        return lowRegisters[generalRegister.id()];
    }

    private static final IA32GeneralRegister8[] highRegisters = {AH, CH, DH, BH};

    public static IA32GeneralRegister8 highFrom(GeneralRegister generalRegister) {
        return highRegisters[generalRegister.id()];
    }

    public WordWidth width() {
        return WordWidth.BITS_8;
    }

    public int value() {
        return ordinal();
    }

    public int id() {
        return ordinal() % 4;
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

    public Enumerator<IA32GeneralRegister8> enumerator() {
        return ENUMERATOR;
    }

    public IA32GeneralRegister8 exampleValue() {
        return AL;
    }
}
