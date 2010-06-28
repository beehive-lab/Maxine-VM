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

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class ImmediateArgument implements Argument {

    public static ImmediateArgument create(long value, WordWidth width) {
        switch (width) {
            case BITS_8:
                return new Immediate8Argument((byte) value);
            case BITS_16:
                return new Immediate16Argument((short) value);
            case BITS_32:
                return new Immediate32Argument((int) value);
            case BITS_64:
                return new Immediate64Argument(value);
            default:
                throw ProgramError.unknownCase();
        }
    }

    public ImmediateArgument plus(ImmediateArgument addend) {
        return create(asLong() + addend.asLong(), width());
    }

    public ImmediateArgument plus(long addend) {
        return create(asLong() + addend, width());
    }

    public ImmediateArgument minus(ImmediateArgument addend) {
        return create(asLong() - addend.asLong(), width());
    }

    public ImmediateArgument minus(long addend) {
        return create(asLong() - addend, width());
    }

    public abstract WordWidth width();

    public abstract String signedExternalValue();

    public abstract Object boxedJavaValue();

    @Override
    public final String toString() {
        return "<" + getClass().getSimpleName() + ": " + externalValue() + ">";
    }

}
