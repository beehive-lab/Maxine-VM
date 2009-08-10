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
package com.sun.c1x.debug;


import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.target.*;

/**
 * The <code>HIRGenerator</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class HIRGenerator {
    private C1XCompilation compilation;
    private CiRuntime runtime;
    private Target target;
    private C1XCompiler compiler;

    /**
     * @param runtime
     * @param target
     */
    public HIRGenerator(CiRuntime runtime, Target target, C1XCompiler compiler) {
        this.runtime = runtime;
        this.target = target;
        this.compiler = compiler;
    }

    public CiRuntime runtime() {
        return runtime;
    }
    /**
     * @param classMethodActor
     * @return
     */
    public IR makeHirMethod(CiMethod classMethodActor) {
        compilation = new C1XCompilation(compiler, target, runtime, classMethodActor);
        compilation.compile();
        return compilation.hir();
    }
}
