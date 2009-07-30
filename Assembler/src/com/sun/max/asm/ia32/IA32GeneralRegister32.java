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
public enum IA32GeneralRegister32 implements GeneralRegister<IA32GeneralRegister32> {

    // Note: keep the order such that 'value()' can rely on ordinals:

    EAX, ECX, EDX, EBX, ESP, EBP, ESI, EDI;

    public static final Enumerator<IA32GeneralRegister32> ENUMERATOR = new Enumerator<IA32GeneralRegister32>(IA32GeneralRegister32.class);

    public static IA32GeneralRegister32 from(GeneralRegister generalRegister) {
        return ENUMERATOR.get(generalRegister.id());
    }

    public IA32IndirectRegister32 indirect() {
        return IA32IndirectRegister32.from(this);
    }

    public IA32BaseRegister32 base() {
        return IA32BaseRegister32.from(this);
    }

    public IA32IndexRegister32 index() {
        return IA32IndexRegister32.from(this);
    }

    public WordWidth width() {
        return WordWidth.BITS_32;
    }

    public int value() {
        return ordinal();
    }

    public int id() {
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

    public Enumerator<IA32GeneralRegister32> enumerator() {
        return ENUMERATOR;
    }

    public IA32GeneralRegister32 exampleValue() {
        return EAX;
    }
}
