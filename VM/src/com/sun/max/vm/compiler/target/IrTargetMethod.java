/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.target;

import com.sun.max.asm.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.stack.*;

/**
 * This class exists solely as a work around when running the IR tests where compilation stops at some IR level
 * above TargetMethod. In this context, the only thing needed from the target method is the entry point and so this
 * hidden class is just a bridge from {@link TargetMethod#getEntryPoint(CallEntryPoint)} to
 * {@link IrMethod#getEntryPoint(CallEntryPoint)}.
 */
public class IrTargetMethod extends TargetMethod {

    public static TargetMethod asTargetMethod(IrMethod irMethod) {
        if (irMethod == null) {
            return null;
        }
        if (irMethod instanceof TargetMethod) {
            return (TargetMethod) irMethod;
        }
        return new IrTargetMethod(irMethod);
    }

    final IrMethod irMethod;

    IrTargetMethod(IrMethod irMethod) {
        super(irMethod.classMethodActor(), null);
        this.irMethod = irMethod;
    }

    @Override
    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return irMethod.getEntryPoint(callEntryPoint);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        throw ProgramError.unexpected();
    }

    @Override
    public InstructionSet instructionSet() {
        throw ProgramError.unexpected();
    }

    @Override
    public void patchCallSite(int callOffset, Word callTarget) {
        throw ProgramError.unexpected();
    }

    @Override
    public int registerReferenceMapSize() {
        throw ProgramError.unexpected();
    }

    @Override
    public JavaStackFrameLayout stackFrameLayout() {
        throw ProgramError.unexpected();
    }

    @Override
    public Address throwAddressToCatchAddress(Address throwAddress, Class<? extends Throwable> throwableClass) {
        throw ProgramError.unexpected();
    }

    @Override
    public void traceExceptionHandlers(IndentWriter writer) {
        throw ProgramError.unexpected();
    }
}
