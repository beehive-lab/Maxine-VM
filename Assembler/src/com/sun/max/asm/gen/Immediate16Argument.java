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
package com.sun.max.asm.gen;

import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public class Immediate16Argument extends ImmediateArgument {

    private short _value;

    public Immediate16Argument(short value) {
        _value = value;
    }

    @Override
    public WordWidth width() {
        return WordWidth.BITS_16;
    }

    public short value() {
        return _value;
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return "0x" + Integer.toHexString(_value & 0xffff);
    }

    public String disassembledValue() {
        return "0x" + String.format("%X", _value);
    }

    @Override
    public String signedExternalValue() {
        return Integer.toString(_value);
    }

    @Override
    public Object boxedJavaValue() {
        return new Short(_value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Immediate16Argument) {
            final Immediate16Argument argument = (Immediate16Argument) other;
            return _value == argument._value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _value;
    }
}
