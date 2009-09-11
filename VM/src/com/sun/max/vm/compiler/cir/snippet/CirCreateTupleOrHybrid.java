/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.cir.snippet;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * Inlines the {@code NonFoldableSnippet.CreateTupleOrHybrid} snippet one step,
 * if the resulting object's hub is known,
 * so that the conditional branch that distinguishes between tuples and hybrids
 * can be eliminated and fast path allocation can be inlined.
 *
 * Not used at the moment, because our allocation fast path is still too complex.
 *
 * @author Bernd Mathiske
 */
@Hypothetical
public /* final (uncomment this to activate the optimization) */ class CirCreateTupleOrHybrid extends CirSpecialSnippet {

    public CirCreateTupleOrHybrid() {
        super(NonFoldableSnippet.CreateTupleOrHybrid.SNIPPET);
    }

    private enum Parameter {
        classActor, normalContinuation, exceptionContinuation
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return false; // isConstantArgument(arguments, Parameter.classActor);
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        return inline(cirOptimizer, arguments, NO_JAVA_FRAME_DESCRIPTOR);
    }

}
