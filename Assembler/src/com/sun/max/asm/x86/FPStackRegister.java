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
package com.sun.max.asm.x86;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public enum FPStackRegister implements EnumerableArgument<FPStackRegister> {

    ST_0(0),
    ST_1(1),
    ST_2(2),
    ST_3(3),
    ST_4(4),
    ST_5(5),
    ST_6(6),
    ST_7(7),
    ST(0) {
        @Override
        public String externalValue() {
            return "%st";
        }
        @Override
        public String disassembledValue() {
            return "st";
        }
    };

    private final int _value;

    private FPStackRegister(int value) {
        _value = value;
    }

    public int value() {
        return _value;
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return "%st(" + value() + ")";
    }

    public String disassembledValue() {
        return "st(" + value() + ")";
    }

    public Enumerator<FPStackRegister> enumerator() {
        return ENUMERATOR;
    }

    public FPStackRegister exampleValue() {
        return ST_0;
    }

    public static final Enumerator<FPStackRegister> ENUMERATOR = new Enumerator<FPStackRegister>(FPStackRegister.class);

}
