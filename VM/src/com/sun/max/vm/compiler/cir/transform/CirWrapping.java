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
package com.sun.max.vm.compiler.cir.transform;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.Arrays;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Applies the transformation specified by the {@link WRAPPED} and {@link WRAPPER} annotations.
 *
 * @author Doug Simon
 */
public final class CirWrapping {

    private final CirMethod wrapperMethod;
    private final CirClosure resultClosure;
    private final CirClosure innerClosure;

    private final CirValue[] extraArguments;

    private CirWrapping(CirMethod wrapperMethod, CirClosure innerClosure) {
        this.wrapperMethod = wrapperMethod;
        this.innerClosure = innerClosure;
        this.resultClosure = wrapperMethod.copyClosure();

        final CirVariableFactory variableFactory = new CirVariableFactory();

        final CirVariable[] wrapperParameters = resultClosure.parameters();
        final CirVariable[] innerParameters = innerClosure.parameters();
        if (innerParameters.length != wrapperParameters.length) {

            final CirVariable[] resultParameters = CirClosure.newParameters(innerParameters.length);
            final int wrapperNormalContinuationIndex = wrapperParameters.length - 2;
            final int resultNormalContinuationIndex = resultParameters.length - 2;

            Arrays.copy(wrapperParameters, 0, resultParameters, 0, wrapperNormalContinuationIndex);
            Arrays.copy(wrapperParameters, wrapperNormalContinuationIndex, resultParameters, resultNormalContinuationIndex, 2);

            extraArguments = CirCall.newArguments(innerParameters.length - wrapperParameters.length);
            for (int i = 0; i != extraArguments.length; i++) {
                final int resultParameterIndex = wrapperNormalContinuationIndex + i;
                final CirVariable extraParameter = variableFactory.createFresh(innerParameters[resultParameterIndex]);
                resultParameters[resultParameterIndex] = extraParameter;
                extraArguments[i] = extraParameter;
            }

            resultClosure.setParameters(resultParameters);
        } else {
            extraArguments = null;
        }
    }

    public static CirClosure apply(CirMethod wrapperMethod, CirClosure wrapped) {
        final CirWrapping wrapping = new CirWrapping(wrapperMethod, wrapped);
        wrapping.run(wrapping.resultClosure);
        return wrapping.resultClosure;
    }

    private void run(CirNode node) {
        final IdentityHashSet<CirClosure> visitedClosures = new IdentityHashSet<CirClosure>();
        final LinkedList<CirNode> toDo = new LinkedList<CirNode>();
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                final boolean procedureIsWrapperMethod = call.procedure() == wrapperMethod;
                if (procedureIsWrapperMethod) {
                    replaceWithCallToWrappedMethod(call);
                }
                final CirValue[] arguments = call.arguments();
                for (int i = 0; i < arguments.length; i++) {
                    final CirValue argument = arguments[i];
                    if (!(argument instanceof CirVariable)) {
                        toDo.add(argument);
                    }
                }
                if (!procedureIsWrapperMethod && !(call.procedure() instanceof CirVariable)) {
                    currentNode = call.procedure();
                    continue;
                }
            } else {
                assert currentNode instanceof CirValue;
                assert !(currentNode instanceof CirVariable);
                if (currentNode instanceof CirBlock) {
                    final CirBlock block = (CirBlock) currentNode;
                    currentNode = block.closure();
                }
                if (currentNode instanceof CirClosure) {
                    final CirClosure closure = (CirClosure) currentNode;
                    if (!visitedClosures.contains(closure)) {
                        visitedClosures.add(closure);
                        currentNode = closure.body();
                        continue;
                    }
                }
            }
            if (toDo.isEmpty()) {
                return;
            }
            currentNode = toDo.removeFirst();
        }
    }

    private void replaceWithCallToWrappedMethod(final CirCall call) {
        if (extraArguments != null) {
            final CirValue[] oldArguments = call.arguments();
            final CirValue[] newArguments = CirCall.newArguments(innerClosure.parameters().length);

            final int oldNormalContinuationIndex = oldArguments.length - 2;
            final int newNormalContinuationIndex = newArguments.length - 2;

            Arrays.copy(oldArguments, 0, newArguments, 0, oldNormalContinuationIndex);
            Arrays.copy(extraArguments, 0, newArguments, oldNormalContinuationIndex, extraArguments.length);
            Arrays.copy(oldArguments, oldNormalContinuationIndex, newArguments, newNormalContinuationIndex, 2);

            call.setArguments(newArguments);
        }
        call.setProcedure(innerClosure);
    }

}
