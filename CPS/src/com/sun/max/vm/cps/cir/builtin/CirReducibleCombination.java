/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.cir.builtin;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;

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
