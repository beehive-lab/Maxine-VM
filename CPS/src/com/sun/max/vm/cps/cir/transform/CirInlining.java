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
