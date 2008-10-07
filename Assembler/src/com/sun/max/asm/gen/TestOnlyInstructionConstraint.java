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
/*VCSID=65913042-c85b-4736-a6a7-9471c48197a0*/
package com.sun.max.asm.gen;

import java.lang.reflect.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;

/**
 * These instruction constraints are only used for generating test cases.
 * They do not appear in the generated assembler methods.
 * 
 * @author Sumeet Panchal
 */
public class TestOnlyInstructionConstraint implements InstructionConstraint {

    private final InstructionConstraint _delegate;

    public TestOnlyInstructionConstraint(InstructionConstraint delegate) {
        _delegate = delegate;
    }

    public String asJavaExpression() {
        return _delegate.asJavaExpression();
    }

    public boolean check(Template template, IndexedSequence<Argument> arguments) {
        return _delegate.check(template, arguments);
    }

    public Method predicateMethod() {
        return _delegate.predicateMethod();
    }

    public boolean referencesParameter(Parameter parameter) {
        return _delegate.referencesParameter(parameter);
    }

    @Override
    public String toString() {
        return _delegate.toString();
    }
}
