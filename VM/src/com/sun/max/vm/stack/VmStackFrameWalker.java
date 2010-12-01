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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Doug Simon
 */
public final class VmStackFrameWalker extends StackFrameWalker {

    private Pointer tla;

    private boolean dumpingFatalStackTrace;

    public VmStackFrameWalker(Pointer tla) {
        super();
        this.tla = tla;
    }

    public void setTLA(Pointer tla) {
        assert this.tla.isZero();
        this.tla = tla;
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
    public Pointer readPointer(VmThreadLocal tl) {
        Pointer etla = ETLA.load(tla);
        return tl.load(etla);
    }

    public boolean isDumpingFatalStackTrace() {
        return dumpingFatalStackTrace;
    }

    public void setIsDumpingFatalStackTrace(boolean flag) {
        dumpingFatalStackTrace = flag;
    }

    @INLINE
    public static ClassMethodActor getCallerClassMethodActor() {
        // TODO: a full stack walk is not necessary here.
        LinkedList<StackFrame> frames = new LinkedList<StackFrame>();
        new VmStackFrameWalker(VmThread.current().tla()).frames(frames, VMRegister.getInstructionPointer(),
                                                       VMRegister.getCpuStackPointer(),
                                                       VMRegister.getCpuFramePointer());
        return getCallerClassMethodActor(frames, false);
    }
}
