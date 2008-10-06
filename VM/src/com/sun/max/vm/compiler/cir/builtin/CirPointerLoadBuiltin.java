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
/*VCSID=d729fc5e-0373-495e-b54d-00ef6b096882*/
package com.sun.max.vm.compiler.cir.builtin;

import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.transform.*;

/**
 * This class is not 'final', so not a leaf class, so needs its own initialization in a static block.
 * @see BcCompiler.createBuiltins()
 * 
 * @author Bernd Mathiske
 */
public class CirPointerLoadBuiltin extends CirSpecialBuiltin {

    protected CirPointerLoadBuiltin(Builtin builtin) {
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

    static {
        for (Builtin builtin : Builtin.builtins()) {
            if (builtin instanceof PointerLoadBuiltin) {
                new CirPointerLoadBuiltin(builtin);
            }
        }
    }
}
