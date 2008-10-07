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
/*VCSID=4347eb2c-d5da-40b3-b287-38a227fb5a9d*/
package com.sun.max.asm.gen;

import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public class Immediate64Argument extends ImmediateArgument {

    private long _value;

    public Immediate64Argument(long value) {
        _value = value;
    }

    @Override
    public WordWidth width() {
        return WordWidth.BITS_64;
    }

    public long value() {
        return _value;
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return "0x" + Long.toHexString(_value);
    }

    public String disassembledValue() {
        return "0x" + String.format("%X", _value);
    }

    @Override
    public String signedExternalValue() {
        return Long.toString(_value);
    }

    @Override
    public Object boxedJavaValue() {
        return new Long(_value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Immediate64Argument) {
            final Immediate64Argument argument = (Immediate64Argument) other;
            return _value == argument._value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) (_value ^ _value >> 32);
    }

}
