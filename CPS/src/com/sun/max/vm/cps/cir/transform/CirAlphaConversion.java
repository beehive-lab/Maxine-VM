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
package com.sun.max.vm.cps.cir.transform;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;

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
