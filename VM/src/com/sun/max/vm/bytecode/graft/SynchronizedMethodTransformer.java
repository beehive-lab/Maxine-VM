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
package com.sun.max.vm.bytecode.graft;

import com.sun.max.vm.bytecode.graft.BytecodeAssembler.*;
import com.sun.max.vm.type.*;

/**
 * 
 *
 * @author Doug Simon
 */
abstract class SynchronizedMethodTransformer extends BytecodeTransformer {

    /**
     * This field is only constructed if needed. That is, if the bytecode being transformed does not contain any return
     * instructions (e.g. method that sits in an infinite loop), it will never be constructed.
     */
    protected Label returnBlockLabel;

    SynchronizedMethodTransformer(BytecodeAssembler assembler) {
        super(assembler);
    }

    abstract void acquireMonitor();
    abstract void releaseMonitorAndReturn(Kind resultKind);
    abstract void releaseMonitorAndRethrow();

    private void convertReturnToGoto() {
        if (returnBlockLabel == null) {
            returnBlockLabel = asm().newLabel();
        }
        asm().goto_(returnBlockLabel);
        ignoreCurrentInstruction();
    }

    @Override
    protected void areturn() {
        convertReturnToGoto();
    }

    @Override
    protected void dreturn() {
        convertReturnToGoto();
    }

    @Override
    protected void freturn() {
        convertReturnToGoto();
    }

    @Override
    protected void ireturn() {
        convertReturnToGoto();
    }

    @Override
    protected void lreturn() {
        convertReturnToGoto();
    }

    @Override
    protected void vreturn() {
        convertReturnToGoto();
    }
}
