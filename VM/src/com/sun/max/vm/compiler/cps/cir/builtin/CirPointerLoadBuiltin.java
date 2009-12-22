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
package com.sun.max.vm.compiler.cps.cir.builtin;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.optimize.*;
import com.sun.max.vm.compiler.cps.cir.transform.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class CirPointerLoadBuiltin extends CirSpecialBuiltin {

    @HOSTED_ONLY
    protected CirPointerLoadBuiltin(PointerLoadBuiltin builtin) {
        super(builtin);
    }

    @Override
    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        final CirValue normalContinuation = arguments[arguments.length - 2];
        if (normalContinuation instanceof CirClosure) {
            final CirClosure closure = (CirClosure) normalContinuation;
            final CirCall body = closure.body();
            return !CirSearch.OutsideBlocks.contains(body, closure.parameters()[0]);
        }
        return false;
    }

    @Override
    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        final CirValue normalContinuation = arguments[arguments.length - 2];
        final CirClosure closure = (CirClosure) normalContinuation;
        return closure.body();
    }

    /**
     * After optimizing CIR, given a direct reference scheme, we may find objects/references in the position of pointers.
     * If this occurs at a foldable variant of a pointer load instruction,
     * then we can deduce a field access that we can meta-evaluate on the host VM.
     * Typically, this is the case when we access the 'offset' field of a FieldActor.
     */
    @HOSTED_ONLY
    @Override
    public boolean isHostFoldable(CirValue[] arguments) {
        return arguments[0].kind() == Kind.REFERENCE && CirValue.areConstant(arguments);
    }
}
