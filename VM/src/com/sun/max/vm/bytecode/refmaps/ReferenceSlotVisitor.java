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
package com.sun.max.vm.bytecode.refmaps;

import com.sun.max.vm.bytecode.*;

/**
 * Implemented by clients interested in knowing which local variables and operand stack slots contain a reference at the
 * bytecode positions denoted by {@link BytecodePositionIterator} during {@linkplain ReferenceMapInterpreter
 * interpretation} of a method.
 *
 * @author Doug Simon
 */
public interface ReferenceSlotVisitor {
    /**
     * Notifies this client that the operand stack slot at a given index contains an object reference at the current
     * interpretation position.
     *
     * @param parametersPopped if {@code true}, then the interpreter is interpreting an
     *            {@linkplain Bytecode.Flags#INVOKE_ invoke} or {@link Bytecode#CALLNATIVE} instruction and has already
     *            popped the parameters for the invocation from the stack but not yet pushed the return value
     */
    void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped);

    /**
     * Notifies this client that the local variable at a given index contains an object reference at the current
     * interpretation position.
     */
    void visitReferenceInLocalVariable(int localVariableIndex);
}
