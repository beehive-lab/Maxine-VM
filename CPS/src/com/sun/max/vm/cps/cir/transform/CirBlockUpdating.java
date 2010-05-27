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

/**
 * Reset all reachable blocks and update their respective call count,
 * the number of times they are called from within the given CIR graph.
 *
 * Use this after deflationary optimizations,
 * which potentially drop blocks and calls thereof.
 *
 * @author Bernd Mathiske
 */
public final class CirBlockUpdating {

    private CirBlockUpdating() {
    }

    /**
     * Recalculates the call count for each reachable block in a given CIR graph.
     *
     * @param node the root of the CIR graph to process
     * @return the list of reachable blocks in the CIR graph rooted at {@code node}. Only these blocks have had there
     *         call count recalculated.
     */
    public static Iterable<CirBlock> apply(CirNode node) {
        final IdentityHashSet<CirBlock> visitedBlocks = new IdentityHashSet<CirBlock>();
        final LinkedList<CirCall> blockCalls = new LinkedList<CirCall>();
        final LinkedList<CirNode> toDo = new LinkedList<CirNode>();
        CirNode currentNode = node;
        while (true) {
            if (currentNode instanceof CirCall) {
                final CirCall call = (CirCall) currentNode;
                for (CirValue argument : call.arguments()) {
                    assert argument != null;
                    toDo.add(argument);
                }
                currentNode = call.procedure();
                if (currentNode instanceof CirBlock) {
                    blockCalls.add(call);
                }
            } else {
                assert currentNode instanceof CirValue;
                if (currentNode instanceof CirClosure) {
                    final CirClosure closure = (CirClosure) currentNode;
                    currentNode = closure.body();
                    continue;
                }
                if (currentNode instanceof CirBlock) {
                    final CirBlock block = (CirBlock) currentNode;
                    if (!visitedBlocks.contains(block)) {
                        visitedBlocks.add(block);
                        block.reset();
                        currentNode = block.closure().body();
                        continue;
                    }
                }
                if (toDo.isEmpty()) {
                    for (CirCall call : blockCalls) {
                        final CirBlock block = (CirBlock) call.procedure();
                        block.addCall(call);
                    }
                    return visitedBlocks;
                }
                currentNode = toDo.removeFirst();
            }
        }
    }
}
