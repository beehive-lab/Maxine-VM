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
package com.sun.c0x;

import com.sun.c1x.ci.CiCompiler;
import com.sun.c1x.ci.CiResult;
import com.sun.c1x.ci.CiTarget;
import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.ri.RiRuntime;
import com.sun.c1x.xir.RiXirGenerator;

/**
 * The {@code C0XCompiler} class definition.
 *
 * @author Ben L. Titzer
 */
public class C0XCompiler extends CiCompiler {

    /**
     * The target that this compiler has been configured for.
     */
    public final CiTarget target;
    /**
     * The runtime that this compiler has been configured for.
     */
    public final RiRuntime runtime;

    public C0XCompiler(RiRuntime runtime, CiTarget target) {
        this.runtime = runtime;
        this.target = target;
    }
    /**
     * Compile the specified method.
     *
     * @param method the method to compile
     * @return a {@link com.sun.c1x.ci.CiTargetMethod target method} representing the compiled method
     */
    @Override
    public CiResult compileMethod(RiMethod method, RiXirGenerator xirGenerator) {
        C0XCompilation comp = new C0XCompilation(runtime, method, target, null);
        comp.compile();
        return null;
    }

    /**
     * Compile the specified method.
     *
     * @param method the method to compile
     * @param osrBCI the bytecode index of the entrypoint for an on-stack-replacement
     * @return a {@link com.sun.c1x.ci.CiTargetMethod target method} representing the compiled method
     */
    @Override
    public CiResult compileMethod(RiMethod method, int osrBCI, RiXirGenerator xirGenerator) {
        C0XCompilation comp = new C0XCompilation(runtime, method, target, null);
        comp.compile();
        return null;
    }
}
