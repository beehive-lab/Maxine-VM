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
/*VCSID=572f74c5-3d0b-481b-be38-73ca254c3dd6*/
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;

/**
 * Abstracts over one or more stack frames for non-Java code. The reason that a
 * single object may represent multiple native frames is that it is impossible
 * to know the stack layout employed by the compiler that generated the code.
 * The only thing we can know for these frames is where the stack pointer and
 * return instruction is Java code that transitioned the call stack from Java
 * code into native code. This is known because all such transitions occur
 * through a {@linkplain NativeStubGenerator native stub} which records the current
 * {@linkplain VmThreadLocal#LAST_JAVA_CALLER_FRAME_POINTER frame pointer} and
 * {@linkplain VmThreadLocal#LAST_JAVA_CALLER_INSTRUCTION_POINTER instruction pointer}.
 *
 * @author Doug Simon
 */
public final class NativeStackFrame extends StackFrame {

    public NativeStackFrame(StackFrame callee, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, instructionPointer, framePointer, stackPointer);
    }

    @Override
    public TargetMethod targetMethod() {
        return null;
    }

    @Override
    public boolean isJavaStackFrame() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Two native frames are the same iff their stack and frame pointers are identical.
     */
    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame instanceof NativeStackFrame) {
            return stackFrame.stackPointer().equals(stackPointer()) && stackFrame.framePointer().equals(framePointer());
        }
        return false;
    }
}
