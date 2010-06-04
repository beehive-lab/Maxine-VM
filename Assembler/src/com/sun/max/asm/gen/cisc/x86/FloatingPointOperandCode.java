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

/**
 * @author Bernd Mathiske
 */
public enum FloatingPointOperandCode implements WrappableSpecification {
    bytes_2(""),
    bytes_14_28(""),
    bytes_98_108(""),
    word_integer("s"),
    short_integer("l"),
    long_integer("q"),
    single_real("s"),
    double_real("l"),
    extended_real("t"),
    packed_bcd(""),
    ST_i("");

    private final String operandTypeSuffix;

    private FloatingPointOperandCode(String operandTypeSuffix) {
        this.operandTypeSuffix = operandTypeSuffix;
    }

    public String operandTypeSuffix() {
        return operandTypeSuffix;
    }

    public TestArgumentExclusion excludeExternalTestArguments(Argument... arguments) {
        return new TestArgumentExclusion(AssemblyTestComponent.EXTERNAL_ASSEMBLER, this, new HashSet<Argument>(Arrays.asList(arguments)));
    }
}
