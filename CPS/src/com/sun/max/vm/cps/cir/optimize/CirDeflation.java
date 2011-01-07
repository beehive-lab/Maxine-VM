/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.optimize;

import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.transform.*;

/**
 * Deflating (as opposed to inflating, such as inlining) CIR optimizations.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class CirDeflation {

    private final CirOptimizer optimizer;
    private final CirNode node;

    protected CirDeflation(CirOptimizer optimizer, CirNode node) {
        this.optimizer = optimizer;
        this.node = node;
    }

    private boolean reducedAny;

    /**
     * A utility class for encapsulating a transformation that deflates a CIR graph rooted by a {@link CirCall}.
     * This class is used to run a transformation that the caller of {@link #apply()} believes will deflate the graph.
     * The purpose of this class is that it provides support for tracing the CIR graph before and after the
     * transformation.
     *
     * @author Doug Simon
     */
    abstract class CallTransformation {
        final CirCall call;
        final CirValue[] arguments;

        CallTransformation(CirCall call, CirValue[] arguments) {
            this.call = call;
            this.arguments = arguments;
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
            final Transformation transform = new Transformation(description(), call.procedure().toString());
            optimizer.notifyBeforeTransformation(call.procedure(), transform);
            final boolean result = run();
            optimizer.notifyAfterTransformation(call.procedure(), transform);
            return result;
        }
    }

    class Folding extends CallTransformation {

        final CirFoldable foldable;

        Folding(CirCall call, CirFoldable foldable, CirValue[] arguments) {
            super(call, arguments);
            this.foldable = foldable;
        }

        @Override
        public TransformationType description() {
            return TransformationType.FOLDING;
        }

        @Override
        public boolean run() {
            try {
                final CirCall newCall = foldable.fold(optimizer, arguments);
                call.setProcedure(newCall.procedure());
                call.setArguments(newCall.arguments());
                return true;
            } catch (CirFoldingException cirFoldingException) {
                return false;
            }
        }

        @Override
        public String target() {
            return foldable.toString();
        }
    }

    class Reducing extends CallTransformation {

        final CirBuiltin builtin;

        Reducing(CirCall call, CirBuiltin builtin, CirValue[] arguments) {
            super(call, arguments);
            this.builtin = builtin;
        }

        @Override
        public TransformationType description() {
            return TransformationType.REDUCING;
        }

        @Override
        public boolean run() {
            call.assign(builtin.reduce(optimizer, arguments));
            return true;
        }

        @Override
        public String target() {
            return builtin.toString();
        }
    }

    private boolean reduceCallByFoldable(CirCall call, CirFoldable foldable, CirValue[] arguments) {
        if (foldable.isFoldable(optimizer, arguments)) {
            return new Folding(call, foldable, arguments).apply();
        }

        if (foldable instanceof CirBuiltin) {
            final CirBuiltin builtin = (CirBuiltin) foldable;
            if (builtin.isReducible(optimizer, arguments)) {
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
            optimizer.notifyBeforeTransformation(optimizer.node(), TransformationType.BETA_REDUCTION);
            final CirClosure closure = (CirClosure) procedure;
            call.assign(CirBetaReduction.applyMultiple(closure, arguments));
            optimizer.notifyAfterTransformation(optimizer.node(), TransformationType.BETA_REDUCTION);
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
        return deflation.reducedAny;
    }

    protected void reduceCalls() {
        final LinkedList<CirNode> inspectionList = new LinkedList<CirNode>();
        final Set<CirBlock> visitedBlocks = new HashSet<CirBlock>();
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                reducedAny |= updateCall(call);

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
                            reducedAny = true;

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
