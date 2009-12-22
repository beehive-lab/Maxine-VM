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
package com.sun.max.vm.compiler.cps.cir.transform;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cps.cir.optimize.*;
import com.sun.max.vm.compiler.cps.cir.variable.*;

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
        final CirValue[] arguments = block.calls().first().arguments();
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
