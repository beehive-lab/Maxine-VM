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

package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A simple data flow analyzer that determines what values can/cannot be
 * null at different points in the program.  It currently recognizes the
 * following operations on objects: New, NewArray, GetField, PutField,
 * ArrayLength, ArrayLoad, ArrayStore, InvokeVirtual, and InvokeInterface.
 *
 * @author Aziz Ghuloum
 */
public class EnvBasedInitializedAnalysis extends EnvBasedDFA<InitializedDomain.Set>{

    public EnvBasedInitializedAnalysis(InitializedDomain domain) {
        super(domain);
    }

    @Override
    protected void analyzeJavaOperatorCall(CirCall call, Environment<CirVariable, InitializedDomain.Set> env) {
        final JavaOperator op = (JavaOperator) call.procedure();
        final CirValue[] args = call.arguments();
        final CirValue sk = args[args.length - 2];
        final CirValue fk = args[args.length - 1];
        if (op instanceof New || op instanceof NewArray) {
            /* New and NewArray are special in that if they succeed, they
             * produce a value that's known to be not null.
             */
            visitContinuation(sk, InitializedDomain.DOMAIN.getInitialized(), env);
            visitContinuation(fk, env);
        } else {
            if (op instanceof GetField      ||
                op instanceof PutField      ||
                op instanceof ArrayLoad     ||
                op instanceof ArrayLength   ||
                op instanceof ArrayStore    ||
                op instanceof InvokeVirtual ||
                op instanceof InvokeInterface) {
                final CirValue receiver = args[0];
                final InitializedDomain.Set t = lookupArg(receiver, env);
                /* record the actual value of the receiver at this
                 * point in the program.
                 */
                rememberMapping(call, t);
                /* in the normal continuation, the value of the receiver is known to be
                 * not null, so, we extend the environment to map the receiver to "not null"
                 */
                visitContinuation(sk,
                    (receiver instanceof CirVariable)
                    ? env.extend((CirVariable) receiver, InitializedDomain.DOMAIN.getInitialized())
                    : env);
                /* we don't care about the exception continuation except that we
                 * have to process to visit it.
                 */
                visitContinuation(fk, env);
            } else {
                super.analyzeJavaOperatorCall(call, env);
            }
        }
    }

    /**
     * Here, we use the results of the analysis that we performed above
     * in order to remove {@link Stoppable#NULL_POINTER_CHECK} as a reason
     * the operations which are called with a receiver may stop
     * where the receiver is known to be initialized (i.e., not null).
     *
     * @author Aziz Ghuloum
     */
    public static class InitializedResult {

        private static class TreeVisitor extends CirVisitor {
            final IdentityHashMapping<CirCall, InitializedDomain.Set[]> results;
            TreeVisitor(IdentityHashMapping<CirCall, InitializedDomain.Set[]> results) {
                this.results = results;
            }
            @Override
            public void visitCall(CirCall call) {
                final CirValue op = call.procedure();
                if (op instanceof JavaOperator) {
                    final InitializedDomain.Set[] set = results.get(call);
                    if (set != null) {
                        if (set.length != 0 && set[0].isInitialized()) {
                            final JavaOperator javaOperator = (JavaOperator) op;
                            javaOperator.removeReasonMayStop(Stoppable.NULL_POINTER_CHECK);
                        }
                    }
                }
            }
        }

        public static void applyResult(CirClosure closure, IdentityHashMapping<CirCall, InitializedDomain.Set[]> results) {
            final TreeVisitor visitor = new TreeVisitor(results);
            CirVisitingTraversal.apply(closure, visitor);
        }
    }
}
