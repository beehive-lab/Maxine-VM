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
package com.sun.c1x.opt;

import com.sun.c1x.graph.IR;
import com.sun.cri.ri.*;

/**
 * This class implements an iterative, flow-sensitve type analysis that can be
 * used to remove redundant checkcasts and instanceof tests as well as devirtualize
 * and deinterface method calls.
 *
 * @author Ben L. Titzer
 */
public class TypeAnalyzer {

    final IR ir;

    public TypeAnalyzer(IR ir) {
        this.ir = ir;
    }

    // type information sources:
    // new, anewarray, newarray
    // parameter types
    // instanceof
    // checkcast
    // array stores
    // array loads
    // field loads
    // exception handler type
    // equality comparison
    // call to Object.clone()
    // call to Class.newInstance()
    // call to Array.newInstance()
    // call to Class.isInstance()

    // possible optimizations:
    // remove redundant checkcasts
    // fold instanceof tests
    // remove dead branches in folded instanceof
    // detect redundant store checks
    // convert invokeinterface to invokevirtual when possible
    // convert invokevirtual to invokespecial when possible
    // remove finalizer checks
    // specialize array copy calls
    // transform reflective Class.newInstance() into allocation
    // transform reflective Array.newInstance() into allocation
    // transform reflective Class.isInstance() to checkcast
    // transform reflective method invocation to direct method invocation

    private static class TypeApprox {
        final RiType type = null;
        final boolean exact = false;
    }
}
