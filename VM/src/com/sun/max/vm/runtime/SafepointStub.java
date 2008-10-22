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
package com.sun.max.vm.runtime;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;

/**
 * A memory region containing the machine code that is executed when handling a safepoint generated trap.
 *
 * @author Doug Simon
 */
public class SafepointStub extends RuntimeStub {

    private final ClassMethodActor _callee;

    /**
     * Creates a stub for holding some code that will be executed by a trap handler for a safepoint.
     *
     * @param size the safepoint stub code
     * @param callee the method called from the stub that performs the action for which the safepoint was triggered
     */
    public SafepointStub(byte[] code, ClassMethodActor callee) {
        super(code);
        _callee = callee;
    }

    /**
     * Gets the method called from this stub that performs the action for which the safepoint was triggered.
     */
    public ClassMethodActor callee() {
        return _callee;
    }
    @Override
    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, Purpose purpose, Object context) {
        FatalError.check(purpose == Purpose.INSPECTING, "Cannot walk stack unwind stub unless inspecting");
        final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
        final StackFrame stackFrame = new RuntimeStubStackFrame(stackFrameWalker.calleeStackFrame(), this,
                        stackFrameWalker.instructionPointer(), stackFrameWalker.stackPointer(), stackFrameWalker.framePointer());

        if (!stackFrameVisitor.visitFrame(stackFrame)) {
            return false;
        }

        stackFrameWalker.advanceToTrapFrame();
        return true;
    }

    @Override
    public String name() {
        return "safepoint stub[" + callee().name() + "]";
    }
}
