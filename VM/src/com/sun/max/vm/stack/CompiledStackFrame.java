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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A {@code CompiledStackFrame} object abstracts an activation frame on a call stack for a method compiled by the VM.
 *
 * @author Doug Simon
 * @author Laurent Daynes
 */
public abstract class CompiledStackFrame extends StackFrame {

    public final CompiledStackFrameLayout layout;

    private final TargetMethod targetMethod;

    public CompiledStackFrame(StackFrame callee, CompiledStackFrameLayout layout, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, instructionPointer, stackPointer, framePointer);
        this.layout = layout;
        this.targetMethod = targetMethod;
    }

    @Override
    public TargetMethod targetMethod() {
        return targetMethod;
    }

    /**
     * {@inheritDoc}
     *
     * The given stack frame is the same as the one represented by this object if all the following conditions hold:
     * <ul>
     * <li>Both frames have a known canonical frame pointer and its value is the same for both frames.</li>
     * <li>Both frames denote the same target method.</li>
     * </ul>
     * Other frame attributes such as the {@linkplain #instructionPointer()} and the value in each frame slot may differ
     * for the two frames.
     */
    @Override
    public abstract boolean isSameFrame(StackFrame stackFrame);

    @Override
    public String toString() {
        ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        if (classMethodActor != null) {
            return classMethodActor.format("%H.%n(%p)@") + ip.toHexString();
        }
        return targetMethod.description() + "@" + ip.toHexString();
    }

}
