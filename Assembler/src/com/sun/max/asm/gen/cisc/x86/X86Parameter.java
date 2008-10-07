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
import com.sun.max.program.*;

/**
 * An explicit operand, specifying an assembler method parameter.
 * 
 * @author Bernd Mathiske
 */
public abstract class X86Parameter extends X86Operand implements Parameter {

    private final ParameterPlace _place;

    protected X86Parameter(X86Operand.Designation designation, ParameterPlace place) {
        super(designation);
        _place = place;
    }

    public ParameterPlace place() {
        return _place;
    }

    private String _variableName = "p";

    public void setVariableName(String variableName) {
        _variableName = variableName;
    }

    public String variableName() {
        return _variableName;
    }

    private ArgumentRange _argumentRange;

    public ArgumentRange argumentRange() {
        return _argumentRange;
    }

    public void setArgumentRange(ArgumentRange argumentRange) {
        _argumentRange = argumentRange;
    }

    private Set<Argument> _excludedDisassemblerTestArguments = new HashSet<Argument>();
    private Set<Argument> _excludedExternalTestArguments = new HashSet<Argument>();

    public void excludeTestArguments(TestArgumentExclusion testArgumentExclusion) {
        switch (testArgumentExclusion.component()) {
            case DISASSEMBLER:
                _excludedDisassemblerTestArguments = testArgumentExclusion.arguments();
                break;
            case EXTERNAL_ASSEMBLER:
                _excludedExternalTestArguments = testArgumentExclusion.arguments();
                break;
            default:
                ProgramError.unexpected();
        }
    }

    public Set<Argument> excludedDisassemblerTestArguments() {
        return _excludedDisassemblerTestArguments;
    }

    public Set<Argument> excludedExternalTestArguments() {
        return _excludedExternalTestArguments;
    }

    public int compareTo(Parameter other) {
        return type().getName().compareTo(other.type().getName());
    }
}
