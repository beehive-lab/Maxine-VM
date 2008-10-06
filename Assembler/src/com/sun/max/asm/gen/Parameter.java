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
/*VCSID=b590a09e-7c6a-4b49-b2bb-cf66d89e23b8*/
package com.sun.max.asm.gen;

import java.util.*;

import com.sun.max.asm.*;

/**
 * An operand that represents a formal parameter of an assembler method.
 *
 * @author Bernd Mathiske
 */
public interface Parameter extends Operand, Comparable<Parameter> {

    /**
     * @return source code variable name
     */
    String variableName();

    /**
     * @return source code for raw int value access
     */
    String valueString();

    Iterable<? extends Argument> getLegalTestArguments();

    Iterable<? extends Argument> getIllegalTestArguments();

    Argument getExampleArgument();

    ArgumentRange argumentRange();

    Set<Argument> excludedDisassemblerTestArguments();

    Set<Argument> excludedExternalTestArguments();
}
