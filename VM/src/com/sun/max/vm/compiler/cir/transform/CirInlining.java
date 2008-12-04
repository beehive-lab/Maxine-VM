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
package com.sun.max.vm.compiler.cir.transform;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cir.optimize.*;

/**
 * Applies CIR block and method inlining within a CIR graph as often and as long as the given optimizer's inlining policy requires.
 *
 * @author Bernd Mathiske
 */
public final class CirInlining {

    private final CirOptimizer _optimizer;
    private final CirNode _node;

    private CirInlining(CirOptimizer optimizer, CirNode node) {
        _optimizer = optimizer;
        _node = node;
    }

    private boolean _inlinedAny;

    public void updateCall(CirCall call) {
        while (true) {
            final CirValue procedure = call.procedure();
            final CirValue[] arguments = call.arguments();
            if (procedure instanceof CirBlock) {
                final CirBlock block = (CirBlock) procedure;
                if (_optimizer.inliningPolicy().isInlineable(_optimizer, block, arguments)) {
                    _optimizer.notifyBeforeTransformation(block, Transformation.BLOCK_UPDATING);
                    call.assign(block.inline(_optimizer, arguments, call.javaFrameDescriptor()));
                    CirBlockUpdating.apply(_node);
                    _optimizer.notifyAfterTransformation(block, Transformation.BLOCK_UPDATING);
                    _inlinedAny = true;
                    continue;
                }
            }
            if (procedure instanceof CirMethod) {
                final CirMethod method = (CirMethod) procedure;
                if (_optimizer.inliningPolicy().isInlineable(_optimizer, method, arguments)) {
                    _optimizer.notifyBeforeTransformation(method, Transformation.METHOD_UPDATING);
                    try {
                        call.assign(method.inline(_optimizer, arguments, call.javaFrameDescriptor()));
                    } catch (Error error) {
                        System.err.println("while inlining " + method.classMethodActor());
                        throw error;
                    }
                    CirBlockUpdating.apply(_node);
                    _optimizer.notifyAfterTransformation(method, Transformation.METHOD_UPDATING);
                    _inlinedAny = true;
                    continue;
                }
            }
            return;
        }
    }

    private void inlineCalls() {
        final LinkedList<CirNode> inspectionList = new LinkedList<CirNode>();
        final IdentityHashSet<CirBlock> visitedBlocks = new IdentityHashSet<CirBlock>();
        CirNode currentNode = _node;
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
        optimizer.notifyBeforeTransformation(node, Transformation.INLINING);
        final CirInlining inlining = new CirInlining(optimizer, node);
        inlining.inlineCalls();
        optimizer.notifyAfterTransformation(node, Transformation.INLINING);
        return inlining._inlinedAny;
    }

}
