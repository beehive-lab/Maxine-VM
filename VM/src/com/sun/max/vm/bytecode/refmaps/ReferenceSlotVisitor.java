/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.bytecode.refmaps;

import com.sun.cri.bytecode.*;

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
     *            invoke... or {@link Bytecodes#JNICALL} instruction and has already
     *            popped the parameters for the invocation from the stack but not yet pushed the return value
     */
    void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped);

    /**
     * Notifies this client that the local variable at a given index contains an object reference at the current
     * interpretation position.
     */
    void visitReferenceInLocalVariable(int localVariableIndex);
}
