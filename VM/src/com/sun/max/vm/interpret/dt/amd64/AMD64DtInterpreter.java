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
package com.sun.max.vm.interpret.dt.amd64;

import com.sun.max.unsafe.*;
import com.sun.max.vm.interpret.dt.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;

/**
 * AMD64 specific DtInterpreter.
 *
 * @author Simon Wilkinson
 */
public class AMD64DtInterpreter extends DtInterpreter {

    /**
     * Offset from start() to first prologue instruction.
     */
    private final int prologueOffset;

    /**
     * Offset from start() to first template instruction.
     */
    private final int templatesOffset;

    /**
     * Framesize needed by the interpreter before non-parameter locals specific to the
     * method are allocated on the stack.
     */
    private final int baseFrameSize;

    AMD64DtInterpreter(byte[] code, int prologueOffset, int templatesOffset, int baseFrameSize) {
        super(code);
        this.prologueOffset = prologueOffset;
        this.templatesOffset = templatesOffset;
        this.baseFrameSize = baseFrameSize;
    }

    @Override
    public Address entryPoint() {
        return start().plus(prologueOffset);
    }

    Address templatesStart() {
        return start().plus(templatesOffset);
    }

    /**
     * Framesize needed by the interpreter before non-parameter locals specific to the
     * method are allocated on the stack.
     */
    int baseFrameSize() {
        return baseFrameSize;
    }

    @Override
    public String name() {
        return "AMD64DtInterpreter";
    }

    @Override
    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, Purpose purpose, Object context) {

        final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
        final StackFrame stackFrame = new RuntimeStubStackFrame(stackFrameWalker.calleeStackFrame(), this,
                        stackFrameWalker.instructionPointer(), stackFrameWalker.stackPointer(), stackFrameWalker.framePointer());
        if (!stackFrameVisitor.visitFrame(stackFrame)) {
            return false;
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(stackFrameWalker.framePointer(), baseFrameSize).asPointer();
        final Pointer callerStackPointer = stackFrameWalker.framePointer().plus(baseFrameSize);
        final Pointer callerFramePointer = stackFrameWalker.readWord(stackFrameWalker.framePointer(), baseFrameSize - Word.size()).asPointer();

        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);
        return true;
    }

}
