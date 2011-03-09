/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;

/**
 * Verifies the well-formedness of a CIR graph.
 *
 * @author Doug Simon
 */
public final class CirVerifyingObserver extends IrObserverAdapter {

    public static class CirVerifyError extends InternalError {
        public CirVerifyError(String message) {
            super(message);
        }
    }

    static class Verifier {

        final ClassMethodActor classMethodActor;
        final CirNode ir;
        final Object transform;

        void check(boolean condition, String errorMessage) {
            if (!condition) {
                error(errorMessage);
            }
        }

        void error(String errorMessage) {
            final String prefix = transform == null ? "" : transform + ": ";
            Trace.stream().flush();
            throw new CirVerifyError(prefix + "Unwellformed CIR while compiling " + classMethodActor.format("%H.%n(%p)") + ": " + errorMessage);
        }

        void warn(boolean condition, String warningMessage) {
            if (!condition) {
                Trace.stream().println("Potentially unwellformed CIR while compiling " + classMethodActor.format("%H.%n(%p)") + ": " + warningMessage);
            }
        }

        public Verifier(ClassMethodActor classMethodActor, CirNode node, Object transform) {
            this.classMethodActor = classMethodActor;
            this.ir = node;
            this.transform = transform;
        }

        CirVariableRenaming declareParameters(CirClosure closure, CirVariableRenaming renaming) {
            CirVariableRenaming r = renaming;
            final CirVariable[] parameters = closure.parameters();
            for (int i = 0; i < parameters.length; i++) {
                final CirVariable parameter = parameters[i];
                r = new CirVariableRenaming(r, parameter, parameter);
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

        private void verifyValues(CirValue[] values, CirVariableRenaming renaming, LinkedList<Inspection> toDo) {
            for (int i = 0; i < values.length; i++) {
                final CirValue value = values[i];
                check(value != null, "null argument");
                if (value instanceof CirVariable) {
                    final CirVariable variable = (CirVariable) value;
                    check(renaming.find(variable) != null, "found use of undeclared variable " + variable);
                } else {
                    toDo.add(new Inspection(value, renaming));
                }
            }
        }

        void run() {
            if (ir == null || ir instanceof CirCall) {
                return;
            }
            final IdentityHashSet<CirClosure> visitedClosures = new IdentityHashSet<CirClosure>();
            final IdentityHashSet<CirCall> visitedCalls = new IdentityHashSet<CirCall>();
            final LinkedList<Inspection> toDo = new LinkedList<Inspection>();
            CirVariableRenaming renaming = null;
            CirNode currentNode = ir;
            while (true) {
                if (currentNode instanceof CirCall) {
                    final CirCall call = (CirCall) currentNode;
                    check(!visitedCalls.contains(call), "calls must be unique: " + call.id());
                    visitedCalls.add(call);
                    final CirValue[] arguments = call.arguments();
                    verifyValues(arguments, renaming, toDo);
//                    CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
//                    while (javaFrameDescriptor != null) {
//                        verifyValues(javaFrameDescriptor.locals(), renaming, toDo);
//                        verifyValues(javaFrameDescriptor.stackSlots(), renaming, toDo);
//                        javaFrameDescriptor = javaFrameDescriptor.parent();
//                    }
                    if (call.procedure() instanceof CirVariable) {
                        final CirVariable variable = (CirVariable) call.procedure();
                        renaming.find(variable);
                    } else {
                        if (call.procedure() instanceof Stoppable) {
                            final Stoppable stoppable = (Stoppable) call.procedure();
                            if (Stoppable.Static.canStop(stoppable)) {
                                check(call.javaFrameDescriptor() != null, "call to " + call.procedure() + " is missing Java frame descriptor");
                            }
                        }
                        currentNode = call.procedure();
                        continue;
                    }
                } else {
                    check(currentNode instanceof CirValue, "expected a CirValue, got " + (currentNode == null ? "null" : currentNode.getClass().getSimpleName()));
                    check(!(currentNode instanceof CirVariable), "unexpected CirVariable");
                    if (currentNode instanceof CirBlock) {
                        final CirBlock block = (CirBlock) currentNode;
                        currentNode = block.closure();
                    }
                    if (currentNode instanceof CirClosure) {
                        final CirClosure closure = (CirClosure) currentNode;
                        if (!visitedClosures.contains(closure)) {
                            visitedClosures.add(closure);
                            renaming = declareParameters(closure, renaming);
                            currentNode = closure.body();
                            continue;
                        }
                        check(!(closure instanceof CirContinuation), "continuation " + closure.id() + " appears more than once");
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
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (irMethod instanceof CirMethod) {
            final CirMethod cirMethod = (CirMethod) irMethod;
            new Verifier(cirMethod.classMethodActor(), cirMethod.closure(), null).run();
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof CirMethod) {
            final CirMethod cirMethod = (CirMethod) irMethod;
            new Verifier(cirMethod.classMethodActor(), (CirNode) context, "before " + transform).run();
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof CirMethod) {
            final CirMethod cirMethod = (CirMethod) irMethod;
            new Verifier(cirMethod.classMethodActor(), (CirNode) context, "after " + transform).run();
        }
    }
}
