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
import com.sun.c1x.debug.*;
import com.sun.c1x.ri.*;

/**
 * The <code>LIRJavaCall</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class LIRJavaCall extends LIRCall {

    private RiMethod method;
    public final char cpi;
    public final RiConstantPool constantPool;

    public LIRJavaCall(LIROpcode opcode, RiMethod method, LIROperand receiver, LIROperand result, CiRuntimeCall address, List<LIROperand> arguments, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        super(opcode, address, result, receiver, arguments, info, false);
        this.method = method;
        this.cpi = cpi;
        this.constantPool = constantPool;
    }

    /**
     * Gets the method of this java call.
     *
     * @return the method
     */
    public RiMethod method() {
        return method;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitCall(this);
    }

    /**
     * Prints this instruction.
     *
     * @param out the output log stream
     */
    @Override
    public void printInstruction(LogStream out) {
        out.print("call: ");
        out.printf("[addr: %s]", (runtimeCall == null) ? "null" : runtimeCall.name());
        if (!receiver().isIllegal()) {
            out.printf(" [recv: %s]", receiver());
        }
        if (!result().isIllegal()) {
            out.printf(" [result: %s]", result());
        }
    }
}
