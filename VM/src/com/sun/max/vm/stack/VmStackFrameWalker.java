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

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.thread.*;

/**
 * @author Doug Simon
 */
public final class VmStackFrameWalker extends StackFrameWalker {

    private Pointer vmThreadLocals;

    private boolean dumpingFatalStackTrace;

    public VmStackFrameWalker(Pointer vmThreadLocals) {
        super(VMConfiguration.hostOrTarget().compilerScheme());
        this.vmThreadLocals = vmThreadLocals;
    }

    public void setVmThreadLocals(Pointer vmThreadLocals) {
        assert this.vmThreadLocals.isZero();
        this.vmThreadLocals = vmThreadLocals;
    }

    @Override
    public TargetMethod targetMethodFor(Pointer instructionPointer) {
        return Code.codePointerToTargetMethod(instructionPointer);
    }

    @Override
    public byte readByte(Address address, int offset) {
        return address.asPointer().readByte(offset);
    }

    @Override
    public Word readWord(Address address, int offset) {
        return address.asPointer().readWord(offset);
    }

    @Override
    public int readInt(Address address, int offset) {
        return address.asPointer().readInt(offset);
    }

    @Override
    public Word readWord(VmThreadLocal local) {
        return local.getVariableWord(vmThreadLocals);
    }

    public boolean isDumpingFatalStackTrace() {
        return dumpingFatalStackTrace;
    }

    public void setIsDumpingFatalStackTrace(boolean flag) {
        dumpingFatalStackTrace = flag;
    }

    @Override
    public Word readRegister(Role role, TargetABI targetABI) {
        if (role == Role.FRAMELESS_CALL_INSTRUCTION_ADDRESS) {
            return VMRegister.getFramelessCallAddressRegister();
        }
        FatalError.unimplemented();
        return Word.zero();
    }

    @Override
    public void useABI(TargetABI targetABI) {
    }

    @INLINE
    public static ClassMethodActor getCallerClassMethodActor() {
        // TODO: a full stack walk is not necessary here.
        LinkSequence<StackFrame> frames = new LinkSequence<StackFrame>();
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).frames(frames, VMRegister.getInstructionPointer(),
                                                       VMRegister.getCpuStackPointer(),
                                                       VMRegister.getCpuFramePointer());
        return getCallerClassMethodActor(frames, false);
    }
}
