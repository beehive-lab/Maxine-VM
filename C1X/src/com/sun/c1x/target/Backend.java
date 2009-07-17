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
package com.sun.c1x.target;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.lir.*;

/**
 * The <code>Backend</code> class represents a compiler backend for C1X.
 *
 * @author Ben L. Titzer
 */
public abstract class Backend {
    public final Target target;

    protected Backend(Target target) {
        this.target = target;
    }

    public abstract FrameMap newFrameMap(C1XCompilation compilation, CiMethod method, int numberOfLocks, int maxStack);
    public abstract LIRGenerator newLIRGenerator(C1XCompilation compilation);
    public abstract LIRAssembler newLIRAssembler(C1XCompilation compilation);
    public abstract AbstractAssembler newAssembler(C1XCompilation c1xCompilation, CodeBuffer code);
}
