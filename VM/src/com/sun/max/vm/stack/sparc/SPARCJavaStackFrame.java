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
/*VCSID=5ec4e122-c977-4979-a15c-2cbfb699a0e6*/
package com.sun.max.vm.stack.sparc;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * A Java Stack Frame for SPARC 64.
 * The stack frame is constructed with raw stack and frame pointer, as read off the stack and frame pointer registers (%i6 and %o6).
 * All bias computation that may be imposed by the underlying OS (e.g., Solaris 64-bits) is hidden and encapsulated here.
 *
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public class SPARCJavaStackFrame extends JavaStackFrame<OptoStackFrameLayout> {

    public SPARCJavaStackFrame(StackFrame callee, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, new OptoStackFrameLayout(targetMethod.frameSize(), false), targetMethod, instructionPointer, framePointer, stackPointer);
    }

    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame instanceof SPARCJavaStackFrame) {
            return targetMethod().equals(stackFrame.targetMethod()) && stackPointer().equals(stackFrame.stackPointer());
        }
        return false;
    }

    @Override
    public Pointer slotBase() {
        final Pointer topOfFrame = bias().unbias(stackPointer());
        assert topOfFrame.equals(topOfFrame.aligned(SPARCStackFrameLayout.STACK_FRAME_ALIGNMENT));
        return topOfFrame;
    }

    @Override
    public int biasedOffset(int offset) {
        // Stack slot offset are positive relative to the stack pointer. To convert them as offset to the frame pointer, we need the frame size.
        return SPARCStackFrameLayout.slotOffsetFromFrame(targetMethod().frameSize(), offset);
    }

    @Override
    public STACK_BIAS bias() {
        return STACK_BIAS.SPARC_V9;
    }

}

