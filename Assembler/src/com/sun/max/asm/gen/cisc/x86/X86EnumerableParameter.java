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
package com.sun.max.asm.gen.cisc.x86;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public class X86EnumerableParameter<EnumerableArgument_Type extends Enum<EnumerableArgument_Type> & EnumerableArgument<EnumerableArgument_Type>> extends X86Parameter implements EnumerableParameter {

    private final Enumerator<EnumerableArgument_Type> enumerator;
    private final Argument exampleArgument;

    public X86EnumerableParameter(X86Operand.Designation designation, ParameterPlace place, Enumerator<EnumerableArgument_Type> enumerator) {
        super(designation, place);
        this.enumerator = enumerator;
        final Iterator<EnumerableArgument_Type> it = enumerator.iterator();
        exampleArgument = it.hasNext() ? it.next().exampleValue() : null;
        switch (place) {
            case MOD_REG:
            case MOD_REG_REXR:
            case MOD_RM:
            case MOD_RM_REXB:
                setVariableName(designation.name().toLowerCase());
                break;
            case SIB_SCALE:
                setVariableName("scale");
                break;
            case SIB_INDEX:
            case SIB_INDEX_REXX:
                setVariableName("index");
                break;
            case SIB_BASE:
            case SIB_BASE_REXB:
                setVariableName("base");
                break;
            case APPEND:
                setVariableName(enumerator.type().getSimpleName().toLowerCase());
                break;
            case OPCODE1:
            case OPCODE1_REXB:
            case OPCODE2_REXB:
                setVariableName("register");
                break;
            case OPCODE2:
                setVariableName("st_i");
                break;
            default:
                ProgramError.unexpected();
        }
    }

    public Enumerator<EnumerableArgument_Type> enumerator() {
        return enumerator;
    }

    public Class type() {
        return enumerator.type();
    }

    public String valueString() {
        return variableName() + ".value()";
    }

    public Iterable<EnumerableArgument_Type> getLegalTestArguments() {
        return enumerator;
    }

    public Iterable<? extends Argument> getIllegalTestArguments() {
        return Iterables.empty();
    }

    public Argument getExampleArgument() {
        return exampleArgument;
    }

    @Override
    public String toString() {
        return type().getSimpleName();
    }

}
