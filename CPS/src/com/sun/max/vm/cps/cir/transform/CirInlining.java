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

import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Applies CIR block and method inlining within a CIR graph as often and as long as the given optimizer's inlining policy requires.
 *
 * @author Bernd Mathiske
 */
public final class CirInlining {

    private final CirOptimizer optimizer;
    private final CirNode node;

    private CirInlining(CirOptimizer optimizer, CirNode node) {
        this.optimizer = optimizer;
        this.node = node;
    }

    private boolean inlinedAny;

    public void updateCall(CirCall call) {
        while (true) {
            final CirValue procedure = call.procedure();
            final CirValue[] arguments = call.arguments();
            if (procedure instanceof CirBlock) {
                final CirBlock block = (CirBlock) procedure;
                if (optimizer.inliningPolicy().isInlineable(optimizer, block, arguments)) {
                    optimizer.notifyBeforeTransformation(block, TransformationType.BLOCK_INLINING);
                    call.assign(block.inline(optimizer, arguments, call.javaFrameDescriptor()));
                    CirBlockUpdating.apply(node);
                    optimizer.notifyAfterTransformation(block, TransformationType.BLOCK_INLINING);
                    inlinedAny = true;
                    continue;
                }
            }
            if (procedure instanceof CirMethod) {
                final CirMethod method = (CirMethod) procedure;
                if (optimizer.inliningPolicy().isInlineable(optimizer, method, arguments)) {
                    final Transformation transform = new Transformation(TransformationType.METHOD_INLINING, method.name());
                    optimizer.notifyBeforeTransformation(method, transform);
                    try {
                        call.assign(method.inline(optimizer, arguments, call.javaFrameDescriptor()));
                    } catch (Error error) {
                        Trace.stream().flush();
                        System.err.println("while inlining " + method.classMethodActor());
                        throw error;
                    }
                    CirBlockUpdating.apply(node);
                    optimizer.notifyAfterTransformation(method, transform);
                    inlinedAny = true;
                    continue;
                }
            }
            return;
        }
    }

    private void inlineCalls() {
        final LinkedList<CirNode> inspectionList = new LinkedList<CirNode>();
        final IdentityHashSet<CirBlock> visitedBlocks = new IdentityHashSet<CirBlock>();
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                updateCall(call);
                for (CirValue value : call.arguments()) {
                    inspectionList.add(value);
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

    public static boolean apply(CirOptimizer optimizer, CirNode node) {
        optimizer.notifyBeforeTransformation(node, TransformationType.INLINING);
        final CirInlining inlining = new CirInlining(optimizer, node);
        inlining.inlineCalls();
        optimizer.notifyAfterTransformation(node, TransformationType.INLINING);
        return inlining.inlinedAny;
    }

}
