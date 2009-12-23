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

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.variable.*;

/**
 * A utility class for finding free variables within a CIR node. A free variable is one that is
 * used within a closure but defined outside the closure.
 * <p>
 * For example, the variable {@code x} in the graph below is free with respect to the outermost
 * closure (the zero-parameter continuation) where as the variable {@code y} is not (it is
 * assigned the value 10 by an inner closure).
 * <p>
 * <hr>
 * <blockquote>
 * <pre>
 *       {cont[] .
 *           f1(x {cont[] .
 *               {proc[y] .
 *                   ...
 *                   f2(y {cont[t2] .
 *                       ...
 *                   } ce)
 *                   ...
 *               }(10)
 *           } ce)
 *       }
 * </pre>
 * </blockquote>
 * <hr>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CirFreeVariableSearch {

    private CirFreeVariableSearch() {
    }

    /**
     * A Binding represents the assignment of a value to a variable. A binding also
     * references all its enclosing bindings.
     */
    static final class Binding {

        private final Binding parent;
        private final CirVariable boundVariable;

        private Binding(Binding parent, CirVariable boundVariable) {
            this.parent = parent;
            this.boundVariable = boundVariable;
        }

        @Override
        public int hashCode() {
            return boundVariable.hashCode();
        }

        /**
         * Creates a binding for zero or more assignments within a given scope.
         *
         * @param variables
         *            the variables being defined
         * @param scope
         *            the current binding(s) for a scope. This may be null if no assignments have yet been found in the scope.
         * @return a new binding
         */
        public static Binding bind(CirVariable[] variables, Binding scope) {
            Binding binding = scope;
            for (CirVariable variable : variables) {
                binding = new Binding(binding, variable);
            }
            return binding;
        }

        /**
         * Determines if a given variable is bound within a given scope.
         *
         * @param variable the variable being queried
         * @param scope
         *            the current binding(s) for a scope. This may be null if no assignments have yet been found in the scope.
         * @return true if {@code variable} is assigned to within the scope
         */
        private static boolean isUnbound(CirVariable variable, Binding scope) {
            Binding binding = scope;
            while (binding != null) {
                if (variable == binding.boundVariable) {
                    return false;
                }
                binding = binding.parent;
            }
            return true;
        }
    }

    private static final class Inspection {
        private final CirNode node;
        private final Binding scope;

        private Inspection(CirNode node, Binding scope) {
            this.node = node;
            this.scope = scope;
        }
    }

    private static void addValues(CirValue[] values, Queue<Inspection> inspectionQueue, Binding scope) {
        for (CirValue value : values) {
            if (value != null) {
                inspectionQueue.add(new Inspection(value, scope));
            }
        }
    }

    public static void findFreeVariables(CirNode node, GrowableDeterministicSet<CirVariable> freeVariables) {
        final Queue<Inspection> inspectionQueue = new LinkedList<Inspection>();
        Binding scope = null;
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                addValues(call.arguments(), inspectionQueue, scope);
                CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
                while (javaFrameDescriptor != null) {
                    addValues(javaFrameDescriptor.locals, inspectionQueue, scope);
                    addValues(javaFrameDescriptor.stackSlots, inspectionQueue, scope);
                    javaFrameDescriptor = javaFrameDescriptor.parent();
                }
                currentNode = call.procedure();
                continue;
            } else if (currentNode instanceof CirClosure) {
                final CirClosure closure = (CirClosure) currentNode;
                scope = Binding.bind(closure.parameters(), scope);
                currentNode = closure.body();
                continue;
            } else if (currentNode instanceof CirVariable) {
                final CirVariable variable = (CirVariable) currentNode;
                if (Binding.isUnbound(variable, scope) && !freeVariables.contains(variable)) {
                    freeVariables.add(variable);
                }
            }
            if (inspectionQueue.isEmpty()) {
                return;
            }
            final Inspection inspection = inspectionQueue.remove();
            currentNode = inspection.node;
            scope = inspection.scope;
        }
    }

    /**
     * Finds all the free variables within the scope (i.e. sub-graph) of a given CIR node.
     *
     * @param node
     * @return the set of free variables within {@code node}.
     */
    public static DeterministicSet<CirVariable> run(CirNode node) {
        if (node instanceof CirContinuationVariable) {
            return DeterministicSet.Static.empty(CirVariable.class);
        }
        final GrowableDeterministicSet<CirVariable> freeVariableSet = new LinkedIdentityHashSet<CirVariable>();
        findFreeVariables(node, freeVariableSet);
        return freeVariableSet;
    }

    public static void applyClosureConversion(CirClosure closure) {
        final GrowableDeterministicSet<CirVariable> freeVariableSet = new LinkedIdentityHashSet<CirVariable>();
        findFreeVariables(closure, freeVariableSet);
        final CirVariable[] parameters = Sequence.Static.toArray(freeVariableSet, new CirVariable[freeVariableSet.length()]);
        closure.setParameters(parameters);

    }

}
