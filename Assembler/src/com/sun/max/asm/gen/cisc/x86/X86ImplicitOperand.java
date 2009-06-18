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

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;

/**
 * An operand that is already implicit in the machine instruction,
 * without requiring an assembler method parameter.
 *
 * @author Bernd Mathiske
 */
public class X86ImplicitOperand extends X86Operand implements ImplicitOperand {

    private final ImplicitOperand.ExternalPresence externalPresence;
    private final Argument argument;

    public X86ImplicitOperand(X86Operand.Designation designation, ImplicitOperand.ExternalPresence externalPresence, Argument argument) {
        super(designation);
        this.externalPresence = externalPresence;
        this.argument = argument;
    }

    public ImplicitOperand.ExternalPresence externalPresence() {
        return externalPresence;
    }

    public Class type() {
        return argument.getClass();
    }

    public Argument argument() {
        return argument;
    }

    public String name() {
        if (argument instanceof Enum) {
            final Enum enumerable = (Enum) argument;
            return enumerable.name();
        }
        final Immediate8Argument immediate8Argument = (Immediate8Argument) argument;
        assert immediate8Argument.value() > 0;
        return immediate8Argument.signedExternalValue();
    }

    @Override
    public String toString() {
        return "<ImplicitOperand: " + argument.externalValue() + ">";
    }
}
