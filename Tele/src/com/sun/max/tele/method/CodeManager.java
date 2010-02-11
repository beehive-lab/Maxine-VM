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
package com.sun.max.tele.method;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Factory and manager for descriptions of locations in code in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class CodeManager extends AbstractTeleVMHolder implements MaxCodeManager {

    // Location of the caller return address relative to the saved location in a stack frame, usually 0 but see SPARC.
    private final int  offsetToReturnPC;

    public CodeManager(TeleVM teleVM) {
        super(teleVM);
        this.offsetToReturnPC = teleVM.vmConfiguration().platform.processorKind.instructionSet.offsetToReturnPC;
    }

    public MethodCodeLocation createMethodLocation(MethodKey methodKey, String description) {
        return CodeLocation.createMethodLocation(teleVM(), methodKey, description);
    }

    public  MethodCodeLocation createMethodLocation(TeleClassMethodActor teleClassMethodActor, int position, String description) {
        return CodeLocation.createMethodLocation(teleVM(), teleClassMethodActor, position, description);
    }

    public CompiledCodeLocation createCompiledLocation(Address address, String description) throws ProgramError {
        return CodeLocation.createCompiledLocation(teleVM(), address, description);
    }

    public CompiledCodeLocation createCompiledLocation(Address address, TeleClassMethodActor teleClassMethodActor, int position, String description) throws ProgramError {
        return CodeLocation.createCompiledLocation(teleVM(), address, teleClassMethodActor, position, description);
    }

    public CompiledCodeLocation createCompiledLocation(StackFrame stackFrame) {
        Pointer instructionPointer = stackFrame.ip;
        if (instructionPointer.isZero()) {
            return null;
        }
        final StackFrame callee = stackFrame.calleeFrame();
        if (callee == null) {
            // Top frame, not a call return so no adjustment.
            return createCompiledLocation(instructionPointer, "top stack frame IP");
        }
        // Add a platform-specific offset from the stored code address to the actual call return site.
        final TargetMethod calleeTargetMethod = callee.targetMethod();
        if (calleeTargetMethod != null) {
            final ClassMethodActor calleeClassMethodActor = calleeTargetMethod.classMethodActor();
            if (calleeClassMethodActor != null) {
                if (calleeClassMethodActor.isTrapStub()) {
                    // Special case, where the IP caused a trap; no adjustment.
                    return createCompiledLocation(instructionPointer, "stack frame return");
                }
            }
        }
        // An ordinary call; apply a platform-specific adjustment to get the real return address.
        return createCompiledLocation(instructionPointer.plus(offsetToReturnPC), "stack frame return");
    }

}

