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
package com.sun.max.vm.compiler.cir;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;

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

        final ClassMethodActor _classMethodActor;
        final CirNode _ir;
        final Object _transform;

        void check(boolean condition, String errorMessage) {
            if (!condition) {
                error(errorMessage);
            }
        }

        void error(String errorMessage) {
            final String prefix = _transform == null ? "" : _transform + ": ";
            Trace.stream().flush();
            throw new CirVerifyError(prefix + "Unwellformed CIR while compiling " + _classMethodActor.format("%H.%n(%p)") + ": " + errorMessage);
        }

        void warn(boolean condition, String warningMessage) {
            if (!condition) {
                Trace.stream().println("Potentially unwellformed CIR while compiling " + _classMethodActor.format("%H.%n(%p)") + ": " + warningMessage);
            }
        }

        public Verifier(ClassMethodActor classMethodActor, CirNode node, Object transform) {
            _classMethodActor = classMethodActor;
            _ir = node;
            _transform = transform;
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
            private final CirNode _node;
            private final CirVariableRenaming _renaming;

            private Inspection(CirNode node, CirVariableRenaming renaming) {
                _node = node;
                _renaming = renaming;
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
            if (_ir == null || _ir instanceof CirCall) {
                return;
            }
            final IdentityHashSet<CirClosure> visitedClosures = new IdentityHashSet<CirClosure>();
            final IdentityHashSet<CirCall> visitedCalls = new IdentityHashSet<CirCall>();
            final LinkedList<Inspection> toDo = new LinkedList<Inspection>();
            CirVariableRenaming renaming = null;
            CirNode currentNode = _ir;
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
                currentNode = inspection._node;
                renaming = inspection._renaming;
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
