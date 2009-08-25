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

/**
 * An adapter enables calls across code compiled by different compilers with different calling convention. The adapter
 * takes care of setting up a caller activation frame as expected by the callee. A {@code AdapterStackFrame} object
 * abstracts the activation frame of such adapters.
 *
 * @author Laurent Daynes
 */
public class AdapterStackFrame extends JavaStackFrame {

    public AdapterStackFrame(StackFrame callee, AdapterStackFrameLayout layout, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, layout, targetMethod, instructionPointer, framePointer, stackPointer);
    }

    /**
     * Two adapter frames are the same iff they are of the same concrete type and their stack and frame pointers are identical.
     */
    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame.getClass().equals(getClass())) {
            return stackFrame.stackPointer.equals(stackPointer) && stackFrame.framePointer.equals(framePointer);
        }
        return false;
    }
}
