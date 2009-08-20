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
package com.sun.c1x.target.x86;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.target.*;

/**
 * The <code>X86Backend</code> class represents the backend for the x86 architectures,
 * i.e. {@link com.sun.c1x.target.Architecture#AMD64} and {@link com.sun.c1x.target.Architecture#IA32}.
 *
 * @author Ben L. Titzer
 */
public class X86Backend extends Backend {

    public X86Backend(Target target) {
        super(target);
    }
    /**
     * Creates a new LIRGenerator for x86.
     * @param compilation the compilation for which to create the LIR generator
     * @return an appropriate LIR generator instance
     */
    @Override
    public LIRGenerator newLIRGenerator(C1XCompilation compilation) {
        return new X86LIRGenerator(compilation);
    }

    /**
     * Creates a new LIRAssembler for x86.
     * @param compilation the compilation for which to create the LIR assembler
     * @return an appropriate LIR assembler instance
     */
    @Override
    public LIRAssembler newLIRAssembler(C1XCompilation compilation) {
        return new X86LIRAssembler(compilation);
    }

    @Override
    public FrameMap newFrameMap(C1XCompilation compilation, RiMethod method, int numberOfLocks, int maxStack) {
        return new X86FrameMap(compilation, method, numberOfLocks, maxStack);
    }
    @Override
    public AbstractAssembler newAssembler(C1XCompiler compiler) {
        return new X86MacroAssembler(compiler);
    }

    @Override
    public GlobalStubEmitter newGlobalStubEmitter(C1XCompiler compiler) {
        return new X86GlobalStubEmitter(compiler);
    }
}
