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
/*VCSID=e07099c7-82fa-45f5-8659-02754b8e6d4c*/
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class X86NumericalParameter extends X86Parameter implements AppendedParameter, ImmediateParameter {

    private final WordWidth _width;

    public X86NumericalParameter(X86Operand.Designation designation, WordWidth width) {
        super(designation, ParameterPlace.APPEND);
        _width = width;
    }

    public WordWidth width() {
        return _width;
    }

    public String valueString() {
        return variableName();
    }

    public Class type() {
        return width().canonicalPrimitiveType();
    }

    public Iterable< ? extends ImmediateArgument> getLegalTestArguments() {
        try {
            switch (_width) {
                case BITS_8:
                    return Static.createSequence(Immediate8Argument.class, byte.class, Byte.MIN_VALUE, (byte) -1, (byte) 2, Byte.MAX_VALUE);
                case BITS_16:
                    return Static.createSequence(Immediate16Argument.class, short.class, Short.MIN_VALUE, (short) (Byte.MIN_VALUE - 1), (short) (Byte.MAX_VALUE + 1), Short.MAX_VALUE);
                case BITS_32:
                    return Static.createSequence(Immediate32Argument.class, int.class, Integer.MIN_VALUE, Short.MIN_VALUE - 1, Short.MAX_VALUE + 1, Integer.MAX_VALUE);
                case BITS_64:
                    return Static.createSequence(Immediate64Argument.class, long.class, Long.MIN_VALUE, Integer.MIN_VALUE - 1L, Integer.MAX_VALUE + 1L, Long.MAX_VALUE);
                default:
                    throw ProgramError.unexpected();
            }
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not generate test argument for: " + this, throwable);
        }
    }

    public Iterable<? extends Argument> getIllegalTestArguments() {
        return Iterables.empty();
    }

    public Argument getExampleArgument() {
        switch (_width) {
            case BITS_8:
                return new Immediate8Argument((byte) 0x12);
            case BITS_16:
                return new Immediate16Argument((short) 0x1234);
            case BITS_32:
                return new Immediate32Argument(0x12345678);
            case BITS_64:
                return new Immediate64Argument(0x123456789ABCDEL);
            default:
                throw ProgramError.unexpected();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
