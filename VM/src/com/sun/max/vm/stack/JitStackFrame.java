/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 *
 * @author Doug Simon
 */
public abstract class JitStackFrame extends CompiledStackFrame {

    public JitStackFrame(StackFrame callee, JitStackFrameLayout layout, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, layout, targetMethod, instructionPointer, framePointer, stackPointer);
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
