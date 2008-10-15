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
public enum IA32IndirectRegister16 implements GeneralRegister<IA32IndirectRegister16>, IndirectRegister {

    BX_PLUS_SI_INDIRECT(INVALID_ID, "%bx,%si", "bx + si"),
    BX_PLUS_DI_INDIRECT(INVALID_ID, "%bx,%di", "bx + si"),
    BP_PLUS_SI_INDIRECT(INVALID_ID, "%bp,%si", "bp + si"),
    BP_PLUS_DI_INDIRECT(INVALID_ID, "%bp,%di", "bp + di"),
            SI_INDIRECT(IA32GeneralRegister16.SI.id(), "%si", "si"),
            DI_INDIRECT(IA32GeneralRegister16.DI.id(), "%di", "di"),
            BP_INDIRECT(IA32GeneralRegister16.BP.id(), "%bp", "bp"),
            BX_INDIRECT(IA32GeneralRegister16.BX.id(), "%bx", "bx");

    public static final Enumerator<IA32IndirectRegister16> ENUMERATOR = new Enumerator<IA32IndirectRegister16>(IA32IndirectRegister16.class);

    private final int _id;
    private final String _externalValue;
    private final String _disassembledValue;

    private IA32IndirectRegister16(int id, String externalValue, String disassembledValue) {
        _id = id;
        _externalValue = externalValue;
        _disassembledValue = disassembledValue;
    }

    public static IA32IndirectRegister16 from(GeneralRegister generalRegister) {
        for (IA32IndirectRegister16 r : ENUMERATOR) {
            if (r._id == generalRegister.id()) {
                return r;
            }
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public WordWidth width() {
        return WordWidth.BITS_16;
    }

    public int value() {
        return ordinal();
    }

    public int id() {
        return _id;
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return _externalValue;
    }

    public String disassembledValue() {
        return _disassembledValue;
    }

    public Enumerator<IA32IndirectRegister16> enumerator() {
        return ENUMERATOR;
    }

    public IA32IndirectRegister16 exampleValue() {
        return BX_INDIRECT;
    }
}
