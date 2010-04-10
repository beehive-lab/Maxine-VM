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
package com.sun.cri.ci;

import com.sun.cri.ri.RiMethod;
import com.sun.cri.xir.RiXirGenerator;

/**
 * This class represents a compiler instance which has been configured for a particular runtime system and
 * target machine and is capable of compiling methods.
 *
 * @author Ben L. Titzer
 */
public abstract class CiCompiler {

    /**
     * Compile the specified method.
     * @param method the method to compile
     * @return a {@link CiResult result} representing the compilation result
     */
    public abstract CiResult compileMethod(RiMethod method, RiXirGenerator xirGenerator);

    /**
     * Compile the specified method.
     * @param method the method to compile
     * @param osrBCI the bytecode index of the entrypoint for an on-stack-replacement
     * @return a {@link CiResult result} representing the compilation result
     */
    public abstract CiResult compileMethod(RiMethod method, int osrBCI, RiXirGenerator xirGenerator);
}
