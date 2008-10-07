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
package com.sun.max.vm.compiler.target.ia32;

import com.sun.max.asm.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 */
public class IA32OptimizedTargetMethod extends OptimizedTargetMethod implements IA32TargetMethod {

    public IA32OptimizedTargetMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    @Override
    public InstructionSet instructionSet() {
        return InstructionSet.IA32;
    }

    @Override
    public final void patchCallSite(int callOffset, Word callEntryPoint) {
        final Pointer callSite = codeStart().plus(callOffset).asPointer();
        final IA32Assembler assembler = new IA32Assembler(callSite.toInt());
        final Label label = new Label();
        assembler.fixLabel(label, callEntryPoint.asAddress().toInt());
        assembler.call(label);
        try {
            final byte[] code = assembler.toByteArray();
            Bytes.copy(code, 0, code(), callOffset, code.length);
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected("patching call site failed", assemblyException);
        }
    }

    @Override
    public final int registerReferenceMapSize() {
        return IA32TargetMethod.Static.registerReferenceMapSize();
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        throw Problem.unimplemented();
    }
}
