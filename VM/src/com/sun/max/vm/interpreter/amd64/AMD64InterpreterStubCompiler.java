/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.interpreter.amd64;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.amd64.*;

/**
 * @author Paul Caprioli
 */
public class AMD64InterpreterStubCompiler extends InterpreterStubCompiler {

    @HOSTED_ONLY
    public AMD64InterpreterStubCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public TargetMethod compile(ClassMethodActor classMethodActor) {
        return new AMD64InterpreterStub(classMethodActor, this, vmConfiguration().targetABIsScheme().optimizedJavaABI());
    }

    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, TargetMethod lastJavaCallee, Purpose purpose, Object context) {
        final Pointer stackPointer = stackFrameWalker.stackPointer();
        switch (purpose) {
            case RAW_INSPECTING: {
                final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                final int flags = RawStackFrameVisitor.Util.makeFlags(isTopFrame, false);
                if (!stackFrameVisitor.visitFrame(targetMethod, stackFrameWalker.instructionPointer(), stackPointer, stackPointer, flags)) {
                    return false;
                }
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                if (!stackFrameVisitor.visitFrame(new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), targetMethod, stackFrameWalker.instructionPointer(), stackPointer, stackPointer))) {
                    return false;
                }
                break;
            }
            default: {
                FatalError.unexpected("can't happen");
            }
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(stackPointer, 0).asPointer();
        stackFrameWalker.advance(callerInstructionPointer, stackPointer, stackFrameWalker.framePointer());
        return true;
    }
}
