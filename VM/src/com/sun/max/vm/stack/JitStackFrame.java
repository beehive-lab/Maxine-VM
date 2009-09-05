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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;

/**
 *
 * @author Doug Simon
 */
public abstract class JitStackFrame extends JavaStackFrame {

    public JitStackFrame(StackFrame callee, JitStackFrameLayout layout, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, layout, targetMethod, instructionPointer, framePointer, stackPointer);
    }

    @Override
    public JitTargetMethod targetMethod() {
        return (JitTargetMethod) super.targetMethod();
    }

    /**
     * Gets a pointer to the operand stack slot indexed by a given offset.
     * @param index the operand stack index
     * @return a pointer to the memory location in the stack containing the operand
     */
    public abstract Pointer operandStackPointer(int index);

    /**
     * Gets a pointer to the local variable indexed by a given offset.
     * @param index the local variable index
     * @return a pointer to the memory location in the stack containing the local variable
     */
    public abstract Pointer localsPointer(int index);

    /**
     * Gets the number of elements currently on the operand stack.
     * @return the number of elements on the operand stack
     */
    public abstract int operandStackDepth();
}
