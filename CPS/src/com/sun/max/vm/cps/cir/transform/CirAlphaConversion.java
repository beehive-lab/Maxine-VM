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
 * Rename every variable declaration in a CIR graph and rename every variable use according to lexical scoping.
 *
 * @author Bernd Mathiske
 */
public final class CirAlphaConversion {

    private final CirVariableFactory variableFactory = new CirVariableFactory();

    private CirVariableRenaming renameParameters(CirClosure closure, CirVariableRenaming renaming) {
        CirVariableRenaming r = renaming;
        final CirVariable[] parameters = closure.parameters();
        for (int i = 0; i < parameters.length; i++) {
            final CirVariable oldParameter = parameters[i];
            final CirVariable newParameter = variableFactory.createFresh(oldParameter);
            parameters[i] = newParameter;
            r = new CirVariableRenaming(r, oldParameter, newParameter);
        }
        return r;
    }

    private final class Inspection {
        private final CirNode node;
        private final CirVariableRenaming renaming;

        private Inspection(CirNode node, CirVariableRenaming renaming) {
            this.node = node;
            this.renaming = renaming;
        }
    }

    private void renameValues(CirValue[] values, CirVariableRenaming renaming, LinkedList<Inspection> toDo) {
        for (int i = 0; i < values.length; i++) {
            final CirValue value = values[i];
            assert value != null;
            if (value instanceof CirVariable) {
                final CirVariable oldVariable = (CirVariable) value;
                final CirVariable newVariable = renaming.find(oldVariable);
                assert newVariable != null : "missing renamed copy of " + oldVariable;
                values[i] = newVariable;
            } else {
                toDo.add(new Inspection(value, renaming));
            }
        }
    }

    private void run(CirNode node) {
        final IdentityHashSet<CirClosure> visitedClosures = new IdentityHashSet<CirClosure>();
        final LinkedList<Inspection> toDo = new LinkedList<Inspection>();
        CirVariableRenaming renaming = null;
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                renameValues(call.arguments(), renaming, toDo);
                CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
                while (javaFrameDescriptor != null) {
                    renameValues(javaFrameDescriptor.locals, renaming, toDo);
                    renameValues(javaFrameDescriptor.stackSlots, renaming, toDo);
                    javaFrameDescriptor = javaFrameDescriptor.parent();
                }
                if (call.procedure() instanceof CirVariable) {
                    final CirVariable variable = (CirVariable) call.procedure();
                    call.setProcedure(renaming.find(variable));
                } else {
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
                        renaming = renameParameters(closure, renaming);
                        currentNode = closure.body();
                        continue;
                    }
                }
            }
            if (toDo.isEmpty()) {
                return;
            }
            final Inspection inspection = toDo.removeFirst();
            currentNode = inspection.node;
            renaming = inspection.renaming;
        }
    }

    public static void apply(CirNode node) {
        final CirAlphaConversion conversion = new CirAlphaConversion();
        conversion.run(node);
    }
}
