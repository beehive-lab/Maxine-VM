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

import static com.sun.max.vm.cps.collect.ListBag.MapType.*;

import java.util.*;

import com.sun.max.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Beta reduction: substitution of formal parameters by actual parameters (arguments).
 *
 * Example: {lambda[x y] . f(x y 3)}(1 2)  -->  f(1 2 3)
 *
 * We assume that alpha conversion has already happened before using this class.
 *
 * This one transformation gets a lot of otherwise separately implemented optimizations done in one big swoop.
 *
 * @author Bernd Mathiske
 */
public abstract class CirBetaReduction {

    protected abstract CirNode transformVariable(CirVariable variable);

    static final class Single extends CirBetaReduction {
        private final CirVariable parameter;
        private final CirValue argument;

        Single(CirVariable parameter, CirValue argument) {
            this.parameter = parameter;
            this.argument = argument;
        }

        @Override
        protected CirNode transformVariable(CirVariable variable) {
            if (variable == parameter) {
                return argument;
            }
            return variable;
        }
    }

    static final class Multiple extends CirBetaReduction {
        private final CirVariable[] parameters;
        private final CirValue[] arguments;

        Multiple(CirVariable[] parameters, CirValue[] arguments) {
            assert parameters.length == arguments.length || parameters.length == arguments.length + 2 :  parameters.length + "," + arguments.length;
            this.parameters = java.util.Arrays.copyOfRange(parameters, 0, arguments.length);
            this.arguments = arguments;
        }

        private final ListBag<CirContinuation, CirContinuation> continuationsBag = new ListBag<CirContinuation, CirContinuation>(HASHED);

        private void updateContinuations() {
            for (CirContinuation oldContinuation : continuationsBag.keys()) {
                final List<CirContinuation> newContinuations = continuationsBag.get(oldContinuation);
                if (newContinuations.size() > 1) {
                    final CirBlock block = new CirBlock(oldContinuation.body());
                    CirFreeVariableSearch.applyClosureConversion(block.closure());
                    for (CirContinuation newContinuation : newContinuations) {
                        final CirVariable[] parameters = block.closure().parameters();
                        final CirValue[] arguments;
                        if (parameters.length > 0) {
                            arguments = new CirValue[parameters.length];
                            System.arraycopy(parameters, 0, arguments, 0, arguments.length);
                        } else {
                            arguments = CirCall.NO_ARGUMENTS;
                        }
                        final CirCall newBlockCall = new CirCall(block, arguments);
                        newContinuation.setBody(newBlockCall);
                    }
                }
            }
        }

        private CirContinuation gatherContinuation(CirContinuation oldContinuation) {
            final CirContinuation newContinuation = (CirContinuation) oldContinuation.clone();
            final CirVariable[] parameters = oldContinuation.parameters();
            newContinuation.setParameters(parameters.length > 0 ? parameters.clone() : CirClosure.NO_PARAMETERS);
            continuationsBag.add(oldContinuation, newContinuation);
            return newContinuation;
        }

        @Override
        protected CirNode transformVariable(CirVariable variable) {
            final int index = Utils.indexOfIdentical(parameters, variable);
            if (index >= 0) {
                if (arguments[index] instanceof CirContinuation) {
                    return gatherContinuation((CirContinuation) arguments[index]);
                }
                return arguments[index];
            }
            return variable;
        }
    }

    private void transformValues(CirValue[] values, LinkedList<CirNode> inspectionList) {
        for (int i = 0; i < values.length; i++) {
            final CirValue value = values[i];
            if (value != null) {
                if (value instanceof CirVariable) {
                    final CirNode newVariable = transformVariable((CirVariable) value);
                    if (newVariable != value) {
                        assert newVariable != null;
                        values[i] = (CirValue) newVariable;
                    }
                } else if (!(value instanceof CirConstant)) {
                    // Only CirProcedure needs further inspection.
                    inspectionList.add(value);
                }
            }
        }
    }

    protected void transform(CirNode node) {
        final LinkedList<CirNode> inspectionList = new LinkedList<CirNode>();
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;

                final CirNode procedure = call.procedure();
                if (procedure instanceof CirVariable) {
                    final CirNode newProcedure = transformVariable((CirVariable) procedure);
                    if (newProcedure != procedure) {
                        call.setProcedure((CirValue) newProcedure);
                        call.clearJavaFrameDescriptorIfNotNeeded();
                    }
                } else {
                    inspectionList.add(procedure);
                }

                transformValues(call.arguments(), inspectionList);

                CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
                while (javaFrameDescriptor != null) {
                    transformValues(javaFrameDescriptor.locals, inspectionList);
                    transformValues(javaFrameDescriptor.stackSlots, inspectionList);
                    javaFrameDescriptor = javaFrameDescriptor.parent();
                }
            } else if (currentNode instanceof CirClosure) {
                final CirClosure closure = (CirClosure) currentNode;
                currentNode = closure.body();
                continue;
            }
            if (inspectionList.isEmpty()) {
                return;
            }
            currentNode = inspectionList.removeFirst();
        }
    }

    /**
     * Reduces all arguments of the given closure.
     * If a continuation argument is placed multiple times into the block body,
     * it is wrapped into a block.
     */
    public static CirCall applyMultiple(CirClosure closure, CirValue... arguments) {
        final Multiple betaReduction = new Multiple(closure.parameters(), arguments);
        final CirCall call = closure.body();
        betaReduction.transform(call);
        betaReduction.updateContinuations();
        return call;
    }

    /**
     * Reduces a single argument, which must not be a continuation (or any sort of closure).
     */
    public static void applySingle(CirClosure closure, CirVariable parameter, CirValue argument) {
        final CirBetaReduction betaReduction = new Single(parameter, argument);
        betaReduction.transform(closure.body());
    }
}
