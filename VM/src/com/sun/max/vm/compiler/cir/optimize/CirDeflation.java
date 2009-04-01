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
package com.sun.max.vm.compiler.cir.optimize;

import java.util.*;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.transform.*;

/**
 * Deflating (as opposed to inflating, such as inlining) CIR optimizations.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class CirDeflation {

    private final CirOptimizer _optimizer;
    private final CirNode _node;

    protected CirDeflation(CirOptimizer optimizer, CirNode node) {
        _optimizer = optimizer;
        _node = node;
    }

    private boolean _reducedAny;

    /**
     * A utility class for encapsulating a transformation that deflates a CIR graph rooted by a {@link CirCall}.
     * This class is used to run a transformation that the caller of {@link #apply()} believes will deflate the graph.
     * The purpose of this class is that it provides support for tracing the CIR graph before and after the
     * transformation.
     *
     * @author Doug Simon
     */
    abstract class CallTransformation {
        final CirCall _call;
        final CirValue[] _arguments;

        CallTransformation(CirCall call, CirValue[] arguments) {
            _call = call;
            _arguments = arguments;
        }

        abstract TransformationType description();
        abstract String target();
        abstract boolean run();

        /**
         * Applies the deflating transformation implemented by this object.
         *
         * @return {@code true} if the deflation succeeded, {@code false} otherwise
         */
        final boolean apply() {
            final Transformation transform = new Transformation(description(), _call.procedure().toString());
            _optimizer.notifyBeforeTransformation(_call.procedure(), transform);
            final boolean result = run();
            _optimizer.notifyAfterTransformation(_call.procedure(), transform);
            return result;
        }
    }

    class Folding extends CallTransformation {

        final CirFoldable _foldable;

        Folding(CirCall call, CirFoldable foldable, CirValue[] arguments) {
            super(call, arguments);
            _foldable = foldable;
        }

        @Override
        public TransformationType description() {
            return TransformationType.FOLDING;
        }

        @Override
        public boolean run() {
            try {
                final CirCall call = _foldable.fold(_optimizer, _arguments);
                _call.setProcedure(call.procedure());
                _call.setArguments(call.arguments());
                return true;
            } catch (CirFoldingException cirFoldingException) {
                return false;
            }
        }

        @Override
        public String target() {
            return _foldable.toString();
        }
    }

    class Reducing extends CallTransformation {

        final CirBuiltin _builtin;

        Reducing(CirCall call, CirBuiltin builtin, CirValue[] arguments) {
            super(call, arguments);
            _builtin = builtin;
        }

        @Override
        public TransformationType description() {
            return TransformationType.REDUCING;
        }

        @Override
        public boolean run() {
            _call.assign(_builtin.reduce(_optimizer, _arguments));
            return true;
        }

        @Override
        public String target() {
            return _builtin.toString();
        }
    }

    private boolean reduceCallByFoldable(CirCall call, CirFoldable foldable, CirValue[] arguments) {
        if (foldable.isFoldable(_optimizer, arguments)) {
            return new Folding(call, foldable, arguments).apply();
        }

        if (foldable instanceof CirBuiltin) {
            final CirBuiltin builtin = (CirBuiltin) foldable;
            if (builtin.isReducible(_optimizer, arguments)) {
                return new Reducing(call, builtin, arguments).apply();
            }
        }
        return false;
    }

    private boolean reduceCallByProcedure(CirCall call) {
        final CirValue procedure = call.procedure();
        final CirValue[] arguments = call.arguments();

        if (procedure instanceof CirFoldable) {
            final CirFoldable foldable = (CirFoldable) procedure;
            return reduceCallByFoldable(call, foldable, arguments);
        }
        if (procedure instanceof CirClosure) {
            _optimizer.notifyBeforeTransformation(_optimizer.node(), TransformationType.BETA_REDUCTION);
            final CirClosure closure = (CirClosure) procedure;
            call.assign(CirBetaReduction.applyMultiple(closure, arguments));
            _optimizer.notifyAfterTransformation(_optimizer.node(), TransformationType.BETA_REDUCTION);
            return true;
        }
        return false;
    }

    protected boolean updateCall(CirCall call) {
        boolean result = false;
        while (true) {
            if (reduceCallByProcedure(call)) {
                result = true;
                continue;
            }
            return result;
        }
    }

    public static boolean apply(CirOptimizer cirOptimizer, CirNode node) {
        cirOptimizer.notifyBeforeTransformation(node, TransformationType.DEFLATION);
        final CirDeflation deflation = new CirDeflation(cirOptimizer, node);
        deflation.reduceCalls();
        cirOptimizer.notifyAfterTransformation(node, TransformationType.DEFLATION);
        return deflation._reducedAny;
    }

    protected void reduceCalls() {
        final LinkedList<CirNode> inspectionList = new LinkedList<CirNode>();
        final Set<CirBlock> visitedBlocks = new HashSet<CirBlock>();
        CirNode currentNode = _node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                _reducedAny |= updateCall(call);

                final CirValue[] arguments = call.arguments();
                for (int i = 0; i < arguments.length; i++) {
                    final CirValue argument = arguments[i];
                    if (argument instanceof CirClosure) {
                        final CirClosure closure = (CirClosure) argument;
                        final CirCall body = closure.body();
                        final CirValue procedure = body.procedure();
                        if (procedure instanceof CirClosure) {
                            // Careful: we would need to apply some beta reduction here.
                            // It's easier to just fall through and have the 'acceptUpdate()' below handle this case.
                        } else if (closure.hasTheseParameters(body.arguments())) {
                            // We found a trivial wrapper closure that just tail-calls a non-closure, e.g. a continuation variable.
                            arguments[i] = procedure;
                            _reducedAny = true;

                            // Don't add argument to work list
                            continue;
                        }
                    }
                    inspectionList.add(argument);
                }
                currentNode = call.procedure();
                continue;
            } else if (currentNode instanceof CirClosure) {
                final CirClosure closure = (CirClosure) currentNode;
                currentNode = closure.body();
                continue;
            } else if (currentNode instanceof CirBlock) {
                final CirBlock block = (CirBlock) currentNode;
                if (!visitedBlocks.contains(block)) {
                    visitedBlocks.add(block);
                    currentNode = block.closure().body();
                    continue;
                }
            }
            if (inspectionList.isEmpty()) {
                return;
            }
            currentNode = inspectionList.removeFirst();
        }
    }
}
