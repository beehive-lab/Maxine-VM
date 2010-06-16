/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.lir;

import com.sun.c1x.lir.FrameMap.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;

/**
 * LIR instruction used in translating {@link Bytecodes#ALLOCA}.
 *
 * @author Doug Simon
 */
public class LIRStackAllocate extends LIRInstruction {

    public final StackBlock stackBlock;

    /**
     * Creates an LIR instruction modelling a stack block allocation.
     * @param result
     */
    public LIRStackAllocate(CiValue result, StackBlock stackBlock) {
        super(LIROpcode.Alloca, result, null, false, null);
        this.stackBlock = stackBlock;
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitStackAllocate(stackBlock, this.result());
    }
}
