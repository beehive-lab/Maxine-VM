/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.transform;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

/**
 * If a CIR block has several parameters
 * that get the exact same argument passed to them in all calls of the block,
 * we merge them into one parameter.
 *
 * This transformation assumes that CirBlockUpdating has just occurred and its results are still valid.
 *
 * @author Bernd Mathiske
 */
public final class CirBlockParameterMerging {

    private CirBlockParameterMerging() {
    }

    private static boolean merge(CirBlock block, int parameterIndex) {
        final CirClosure closure = block.closure();
        final CirVariable[] parameters = closure.parameters();

        // Find out how many and which parameters are going to be merged and which will survive:
        final boolean[] isSurvivor = new boolean[parameters.length];
        isSurvivor[parameterIndex] = true;
        for (CirCall call : block.calls()) {
            final CirValue[] arguments = call.arguments();
            assert arguments.length == parameters.length;
            final CirValue representativeArgument = arguments[parameterIndex];
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] != representativeArgument) {
                    isSurvivor[i] = true;
                }
            }
        }
        int numberOfRemovedParameters = 0;
        for (int i = 0; i < isSurvivor.length; i++) {
            if (!isSurvivor[i]) {
                numberOfRemovedParameters++;
            }
        }
        if (numberOfRemovedParameters < 1) {
            return false;
        }

        // In the body, rename all merged parameters to one surviving representative:
        final CirVariable representativeParameter = parameters[parameterIndex];
        for (int i = 0; i < isSurvivor.length; i++) {
            if (!isSurvivor[i]) {
                CirBetaReduction.applySingle(closure, parameters[i], representativeParameter);
            }
        }

        // Remove redundant parameters from the parameter list:
        final int newParameterLength = parameters.length - numberOfRemovedParameters;
        final CirVariable[] newParameters = CirClosure.newParameters(newParameterLength);
        int n = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (isSurvivor[i]) {
                newParameters[n++] = parameters[i];
            }
        }
        closure.setParameters(newParameters);

        // Remove all call arguments that correspond to redundant parameters:
        for (CirCall call : block.calls()) {
            final CirValue[] arguments = call.arguments();
            assert arguments.length == parameters.length;
            final CirValue[] newArguments = CirCall.newArguments(newParameterLength);
            n = 0;
            for (int i = 0; i < arguments.length; i++) {
                if (isSurvivor[i]) {
                    newArguments[n++] = arguments[i];
                }
            }
            assert n == newParameterLength;
            call.setArguments(newArguments);
        }
        return true;
    }

    private static boolean applyOnce(CirBlock block) {
        final IdentityHashSet<CirValue> values = new IdentityHashSet<CirValue>();
        final CirValue[] arguments = block.calls().getFirst().arguments();
        for (int i = 0; i < arguments.length; i++) {
            final CirValue argument = arguments[i];
            if (values.contains(argument)) {
                if (merge(block, i)) {
                    return true;
                }
            } else {
                values.add(argument);
            }
        }
        return false;
    }

    public static boolean apply(CirOptimizer optimizer, CirNode node, Iterable<CirBlock> blocks) {
        optimizer.notifyBeforeTransformation(node, TransformationType.BLOCK_PARAMETER_MERGING);
        boolean mergedAny = false;
        for (CirBlock block : blocks) {
            while (applyOnce(block)) {
                mergedAny = true;
            }
        }
        optimizer.notifyAfterTransformation(node, TransformationType.BLOCK_PARAMETER_MERGING);
        return mergedAny;
    }

}
