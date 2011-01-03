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
import com.sun.max.vm.cps.collect.*;

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
