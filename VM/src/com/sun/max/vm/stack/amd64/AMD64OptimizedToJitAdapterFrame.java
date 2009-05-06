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
package com.sun.max.vm.stack.amd64;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.amd64.*;
import com.sun.max.vm.stack.*;

/**
 * A {@code AMD64OptimizedToJitAdapterFrame} object abstracts the activation frame of an adapter for calls from
 * method compiled by the optimizing compiler to a method compiled by the JIT compiler on AMD64.
 *
 * @author Laurent Daynes
 */
public class AMD64OptimizedToJitAdapterFrame extends AdapterStackFrame<AdapterStackFrameLayout> {

    /**
     * Cache the value of the RIP address on the stack. This is used frequently for walking stacks and comparing stack frame.
     */
    private final Pointer _ripPointer;

    public Pointer ripPointer() {
        return _ripPointer;
    }

    public Pointer callerFramePointer() {
        return framePointer();
    }

    public AMD64OptimizedToJitAdapterFrame(StackFrame callee, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, new AdapterStackFrameLayout(targetMethod.classMethodActor().numberOfParameterSlots() * JitStackFrameLayout.JIT_SLOT_SIZE, true), targetMethod, instructionPointer, framePointer, stackPointer);
        // Initialize the cache of caller stack pointer
        if (isFrameless() && instructionPointer().equals(entryPoint())) {
            _ripPointer = stackPointer;
        } else {
            _ripPointer = stackPointer.plus(AMD64JitCompiler.adapterFrameSize(targetMethod().classMethodActor()));
        }
    }

    /**
     * Return the entry point to the adapter in the target code.
     */
    public Pointer entryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(targetMethod());
    }

    /**
     * Return true if adapter is for a parameterless method (can only be a static with no arguments).
     */
    private boolean isFrameless() {
        final MethodActor classMethodActor = targetMethod().classMethodActor();
        return classMethodActor.isStatic() && (classMethodActor.descriptor().numberOfParameters() == 0);
    }

    /**
     * Return true if stack frame is partial.
     */
    public boolean isPartial() {
        return instructionPointer().equals(entryPoint()) && !isFrameless();
    }

    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame instanceof AMD64OptimizedToJitAdapterFrame && stackFrame.targetMethod() == targetMethod()) {
            final AMD64OptimizedToJitAdapterFrame other = (AMD64OptimizedToJitAdapterFrame) stackFrame;
            return other.ripPointer().equals(ripPointer());
        }
        return false;
    }
}
