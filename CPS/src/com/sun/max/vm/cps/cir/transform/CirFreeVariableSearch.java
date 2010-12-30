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

import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

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

    public static void findFreeVariables(CirNode node, LinkedIdentityHashSet<CirVariable> freeVariables) {
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
    public static LinkedIdentityHashSet<CirVariable> run(CirNode node) {
        if (node instanceof CirContinuationVariable) {
            return new LinkedIdentityHashSet<CirVariable>();
        }
        final LinkedIdentityHashSet<CirVariable> freeVariableSet = new LinkedIdentityHashSet<CirVariable>();
        findFreeVariables(node, freeVariableSet);
        return freeVariableSet;
    }

    public static void applyClosureConversion(CirClosure closure) {
        final LinkedIdentityHashSet<CirVariable> freeVariableSet = new LinkedIdentityHashSet<CirVariable>();
        findFreeVariables(closure, freeVariableSet);
        final CirVariable[] parameters = freeVariableSet.toArray(new CirVariable[freeVariableSet.size()]);
        closure.setParameters(parameters);

    }

}
