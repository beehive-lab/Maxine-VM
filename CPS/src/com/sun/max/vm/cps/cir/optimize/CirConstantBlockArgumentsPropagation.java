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
package com.sun.max.vm.cps.cir.optimize;

import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Propagate constant block call arguments into block bodies.
 *
 * Iff a block parameter has the same constant argument in all of its calls,
 * then we remove the parameter definition
 * and all corresponding argument passings
 * and replace all of its uses in block bodies by the argument.
 *
 * @author Bernd Mathiske
 */
public final class CirConstantBlockArgumentsPropagation {

    private CirConstantBlockArgumentsPropagation() {
    }

    private static int getArgumentIndex(CirVariable variable, List<CirCall> calls) {
        int index = -1;
        for (CirCall call : calls) {
            final int i = com.sun.max.Utils.indexOfIdentical(call.arguments(), variable);
            if (i < 0) {
                return -1;
            } else if (index < 0) {
                index = i;
            } else if (i != index) {
                return -1;
            }
        }
        return index;
    }

    /**
     * We have not (yet?) implemented translating closures with more than one exception continuation parameter (to DIR).
     * So for now we maintain the invariant that there is at most one cc and ce per closure parameters list.
     * Here, we filter out some cases where this would occur, losing the respective (continuation) constant propagation.
     *
     * (Causing more than one normal continuation parameter does not occur, because we always remove one as we add at most one.)
     *
     * @return whether these free variables would add up to having more than maximally one ce among the closure's parameters
     */
    private static boolean continuationParameterOverflow(CirClosure closure, Iterable<CirVariable> additionalParameters, Class parameterClass) {
        int n = 0;
        for (CirVariable variable : closure.parameters()) {
            if (parameterClass.isInstance(variable)) {
                n++;
            }
        }
        for (CirVariable variable : additionalParameters) {
            if (parameterClass.isInstance(variable)) {
                n++;
            }
        }
        return n > 1;
    }

    /**
     * Determines free variables in the continuation that is to be inlined by 'propagateConstantArgument'.
     * If such a variable is passed as an argument to the target block
     * at each of its calls at the exact same parameter position,
     * then the parameter and the variable are synonymous
     * and we substitute one for the other.
     * Otherwise we extend the parameter list of the block by the free variable
     * and pass it as an argument at every call.
     */
    private static boolean propagateFreeVariablesFromContinuation(CirClosure closure, List<CirCall> calls, int index, CirContinuation continuation) {
        final LinkedIdentityHashSet<CirVariable> freeVariables = CirFreeVariableSearch.run(continuation);
        if (closure.parameters()[index] instanceof CirNormalContinuationParameter) {
            if (continuationParameterOverflow(closure, freeVariables, CirExceptionContinuationParameter.class)) {
                return false;
            }
        } else {
            if (continuationParameterOverflow(closure, freeVariables, CirNormalContinuationParameter.class)) {
                return false;
            }
        }
        final List<CirVariable> remainingFreeVariables = new LinkedList<CirVariable>();
        for (CirVariable variable : freeVariables) {
            final int i = getArgumentIndex(variable, calls);
            if (i >= 0) {
                CirBetaReduction.applySingle(continuation, variable, closure.parameters()[i]);
            } else {
                remainingFreeVariables.add(variable);
            }
        }
        if (!remainingFreeVariables.isEmpty()) {
            final CirVariable[] variables = remainingFreeVariables.toArray(new CirVariable[remainingFreeVariables.size()]);
            closure.setParameters(com.sun.max.Utils.concat(closure.parameters(), variables));
            for (CirCall call : calls) {
                call.setArguments(com.sun.max.Utils.concat(call.arguments(), variables));
            }
        }
        return true;
    }

    private static boolean propagateConstantArgument(CirBlock block, List<CirCall> calls, int index, CirValue argument) {
        final CirClosure closure = block.closure();
        final CirVariable parameter = closure.parameters()[index];
        if (argument instanceof CirContinuation) {
            switch (CirCount.apply(closure.body(), parameter)) {
                case 0: {
                    closure.removeParameter(index);
                    return true;
                }
                case 1: {
                    if (!propagateFreeVariablesFromContinuation(closure, calls, index, (CirContinuation) argument)) {
                        return false;
                    }
                    break;
                }
                default: {
                    return false;
                }
            }
        } else {
            assert !(argument instanceof CirClosure) : argument + " index " + index + " closure = " + closure; // TODO: can this happen?
        }
        CirBetaReduction.applySingle(closure, parameter, argument);
        closure.removeParameter(index);
        assert closure.verifyParameters();
        return true;
    }

    private static boolean haveEqualArgument(Iterable<CirCall> calls, int index, CirValue argument) {
        for (CirCall call : calls) {
            if (!call.arguments()[index].equals(argument)) {
                return false;
            }
        }
        return true;
    }

    private static boolean apply(CirBlock block) {
        boolean propagatedAny = false;
        final LinkedList<CirCall> calls = block.calls();
        if (calls != null) {
            final CirCall call = calls.getFirst();
            final Iterable<CirCall> otherCalls = new Iterable<CirCall>() {
                @Override
                public Iterator<CirCall> iterator() {
                    Iterator<CirCall> iterator = calls.iterator();
                    if (iterator.hasNext()) {
                        // skip first element
                        iterator.next();
                    }
                    return iterator;
                }
            };
            int i = 0;
            while (i < call.arguments().length) {
                final CirValue argument = call.arguments()[i];
                if (argument.isConstant() && haveEqualArgument(otherCalls, i, argument) && propagateConstantArgument(block, calls, i, argument)) {
                    for (CirCall c : calls) {
                        c.removeArgument(i);
                    }
                    propagatedAny = true;
                } else {
                    i++;
                }
            }
        }
        return propagatedAny;
    }

    public static boolean apply(CirOptimizer optimizer, CirNode node, Iterable<CirBlock> blocks) {
        optimizer.notifyBeforeTransformation(node, TransformationType.CONSTANT_BLOCK_ARGUMENT_PROPAGATION);
        boolean propagatedAny = false;
        for (CirBlock block : blocks) {
            if (apply(block)) {
                propagatedAny = true;
            }
        }
        optimizer.notifyAfterTransformation(node, TransformationType.CONSTANT_BLOCK_ARGUMENT_PROPAGATION);
        return propagatedAny;
    }
}
