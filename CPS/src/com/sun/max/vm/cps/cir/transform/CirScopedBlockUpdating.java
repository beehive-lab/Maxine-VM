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
package com.sun.max.vm.cps.cir.transform;

import java.util.*;

import com.sun.max.vm.cps.cir.*;

/**
 * Finds out in which block each CIR call is nested and remembers this in a dictionary for later queries.
 * 
 * @author Bernd Mathiske
 */
public class CirScopedBlockUpdating extends CirBlockScopedTraversal {

    public CirScopedBlockUpdating(CirNode graph) {
        super(graph);
    }

    @Override
    public void visitBlock(CirBlock block, CirBlock scope) {
        block.reset();
        super.visitBlock(block, scope);
    }

    final LinkedList<CirCall> blockCalls = new LinkedList<CirCall>();

    public LinkedList<CirCall> blockCalls() {
        return blockCalls;
    }

    final Map<CirCall, CirBlock> callToScope = new IdentityHashMap<CirCall, CirBlock>();

    /**
     * @return the innermost block that contains the given call
     */
    public CirBlock scope(CirCall call) {
        assert call.javaFrameDescriptor() != null || call.procedure() instanceof CirBlock;
        return callToScope.get(call);
    }

    @Override
    public void visitCall(CirCall call, CirBlock scope) {
        if (call.javaFrameDescriptor() != null) {
            assert !CirBlock.class.isInstance(call.procedure());
            callToScope.put(call, scope);
        } else if (call.procedure() instanceof CirBlock) {
            callToScope.put(call, scope);
            blockCalls.add(call);
        }
        super.visitCall(call, scope);
    }

    @Override
    public void run() {
        super.run();
        for (CirCall call : blockCalls) {
            final CirBlock block = (CirBlock) call.procedure();
            block.addCall(call);
        }
    }

}
