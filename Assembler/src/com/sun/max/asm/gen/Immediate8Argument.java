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
public class Immediate8Argument extends ImmediateArgument {

    private final byte value;

    public Immediate8Argument(byte value) {
        this.value = value;
    }

    @Override
    public WordWidth width() {
        return WordWidth.BITS_8;
    }

    public byte value() {
        return value;
    }

    public long asLong() {
        return value();
    }

    public String externalValue() {
        return "0x" + Integer.toHexString(value & 0xff);
    }

    public String disassembledValue() {
        return "0x" + String.format("%X", value);
    }

    @Override
    public String signedExternalValue() {
        return Integer.toString(value);
    }

    @Override
    public Object boxedJavaValue() {
        return new Byte(value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Immediate8Argument) {
            final Immediate8Argument argument = (Immediate8Argument) other;
            return value == argument.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

}
