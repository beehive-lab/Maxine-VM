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
package com.sun.max.vm.stack.ia32;

import static com.sun.max.vm.stack.JavaStackFrameLayout.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.ia32.*;
import com.sun.max.vm.stack.*;

/**
 * Mechanism for accessing values on a stack frame for a method produced by the
 * {@linkplain IA32JitCompiler IA32 JIT compiler}.
 *
 * @see IA32JitStackFrameLayout
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public class IA32JitStackFrame extends JitStackFrame<IA32JitStackFrameLayout> {

    private final Pointer _localVariablesBase;

    public IA32JitStackFrame(StackFrame callee, TargetMethod targetMethod,
                    Pointer instructionPointer,
                    Pointer stackPointer,
                    Pointer framePointer,
                    Pointer localVariablesBase) {
        super(callee, new IA32JitStackFrameLayout(targetMethod), targetMethod, instructionPointer, framePointer, stackPointer);
        _localVariablesBase = localVariablesBase;
    }

    @Override
    public Pointer localsPointer(int index) {
        return _localVariablesBase.plus(_layout.localVariableOffset(index));
    }

    @Override
    public Pointer operandStackPointer(int index) {
        return _localVariablesBase.plus(_layout.operandStackOffset(index));
    }

    @Override
    public int operandStackDepth() {
        final Pointer operandStackBase = operandStackPointer(0);
        return stackPointer().minus(operandStackBase).toInt() / STACK_SLOT_SIZE;
    }

    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame instanceof IA32JitStackFrame) {
            final IA32JitStackFrame jitStackFrame = (IA32JitStackFrame) stackFrame;
            return targetMethod().equals(stackFrame.targetMethod()) && _localVariablesBase.equals(jitStackFrame._localVariablesBase);
        }
        return false;
    }
}
