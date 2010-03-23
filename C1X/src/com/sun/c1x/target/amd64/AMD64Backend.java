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
package com.sun.c1x.target.amd64;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.target.*;
import com.sun.c1x.xir.*;

/**
 * The {@code X86Backend} class represents the backend for the AMD64 architecture.
 *
 * @author Ben L. Titzer
 */
public class AMD64Backend extends Backend {

    public AMD64Backend(C1XCompiler compiler) {
        super(compiler);
    }
    /**
     * Creates a new LIRGenerator for x86.
     * @param compilation the compilation for which to create the LIR generator
     * @return an appropriate LIR generator instance
     */
    @Override
    public LIRGenerator newLIRGenerator(C1XCompilation compilation) {
        return new AMD64LIRGenerator(compilation);
    }

    /**
     * Creates a new LIRAssembler for x86.
     * @param compilation the compilation for which to create the LIR assembler
     * @return an appropriate LIR assembler instance
     */
    @Override
    public LIRAssembler newLIRAssembler(C1XCompilation compilation) {
        return new AMD64LIRAssembler(compilation);
    }

    @Override
    public FrameMap newFrameMap(RiMethod method, int numberOfLocks) {
        return new FrameMap(compiler, method, numberOfLocks);
    }
    @Override
    public AbstractAssembler newAssembler() {
        return new AMD64MacroAssembler(compiler, compiler.target);
    }

    @Override
    public CiXirAssembler newXirAssembler() {
        return new AMD64XirAssembler();
    }

    @Override
    public GlobalStubEmitter newGlobalStubEmitter() {
        return new AMD64GlobalStubEmitter(compiler);
    }
}
