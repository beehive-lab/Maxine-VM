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
/*VCSID=ef6627f6-4da4-441c-8257-69cfc00b67cb*/
package com.sun.max.asm.amd64;

import com.sun.max.asm.x86.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public enum AMD64GeneralRegister8 implements GeneralRegister<AMD64GeneralRegister8> {

    AL(0, false),
    CL(1, false),
    DL(2, false),
    BL(3, false),
    SPL(4, false),
    BPL(5, false),
    SIL(6, false),
    DIL(7, false),
    R8B(8, false),
    R9B(9, false),
    R10B(10, false),
    R11B(11, false),
    R12B(12, false),
    R13B(13, false),
    R14B(14, false),
    R15B(15, false),
    AH(4, true),
    CH(5, true),
    DH(6, true),
    BH(7, true);

    public static final Enumerator<AMD64GeneralRegister8> ENUMERATOR = new Enumerator<AMD64GeneralRegister8>(AMD64GeneralRegister8.class);

    private final int _value;
    private final boolean _isHighByte;


    private AMD64GeneralRegister8(int value, boolean isHighByte) {
        _value = value;
        _isHighByte = isHighByte;
    }

    public static AMD64GeneralRegister8 lowFrom(GeneralRegister generalRegister) {
        return ENUMERATOR.get(generalRegister.id());
    }

    public static AMD64GeneralRegister8 highFrom(GeneralRegister generalRegister) {
        return ENUMERATOR.get(generalRegister.id() + 16);
    }

    public static AMD64GeneralRegister8 fromValue(int value, boolean isRexBytePresent) {
        if (!isRexBytePresent && value >= AH._value) {
            return ENUMERATOR.get((value - AH._value) + AH.ordinal());
        }
        return ENUMERATOR.fromValue(value);
    }

    public boolean isHighByte() {
        return _isHighByte;
    }

    public boolean requiresRexPrefix() {
        return _value >= 4 && !_isHighByte;
    }

    public WordWidth width() {
        return WordWidth.BITS_8;
    }

    public int id() {
        return ordinal() % 16;
    }

    public int value() {
        return _value;
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

    public Enumerator<AMD64GeneralRegister8> enumerator() {
        return ENUMERATOR;
    }

    public AMD64GeneralRegister8 exampleValue() {
        return AL;
    }
}
