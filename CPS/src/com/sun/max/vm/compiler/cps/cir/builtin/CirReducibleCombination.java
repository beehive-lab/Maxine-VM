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

/**
 * Merging a builtin with subsequent builtins.
 *
 * @author Bernd Mathiske
 */
public class CirReducibleCombination extends CirSpecialBuiltin {

    @HOSTED_ONLY
    protected CirReducibleCombination(Builtin builtin) {
        super(builtin);
    }

    public static final class IntNot extends CirReducibleCombination {
        @HOSTED_ONLY
        public IntNot() {
            super(JavaBuiltin.IntNot.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            final CirValue normalContinuation = arguments[1];
            if (normalContinuation instanceof CirClosure) {
                final CirClosure closure = (CirClosure) normalContinuation;
                final CirCall body = closure.body();
                if (body.procedure() == this && closure.parameters().length == 1 &&
                                body.arguments()[0] == closure.parameters()[0]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue continuation1 = arguments[1];
            final CirClosure closure = (CirClosure) continuation1;
            final CirValue continuation2 = closure.body().arguments()[1];
            return new CirCall(continuation2, arguments[0]);
        }
    }

}
