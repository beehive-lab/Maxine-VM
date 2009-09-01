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
package com.sun.c1x.lir;

import java.util.*;

import com.sun.c1x.ci.*;

/**
 * The <code>LIRCall</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public abstract class LIRCall extends LIRInstruction {

    protected CiRuntimeCall addr;   // TODO not sure if we should create a new class for address
    protected List<LIROperand> arguments;

    /**
     * Creates a new LIRCall instruction.
     *
     * @param entry
     * @param arguments
     */
    public LIRCall(LIROpcode opcode, CiRuntimeCall entry, LIROperand result, List<LIROperand> arguments, CodeEmitInfo info) {
        super(opcode, result, info);
        this.addr = entry;
        this.arguments = arguments;
    }

    /**
     * Gets the address of this call.
     *
     * @return the address
     */
    public CiRuntimeCall address() {
        return addr;
    }

    /**
     * Gets the arguments list of this call.
     *
     * @return the arguments
     */
    public List<LIROperand> arguments() {
        return arguments;
    }
}
