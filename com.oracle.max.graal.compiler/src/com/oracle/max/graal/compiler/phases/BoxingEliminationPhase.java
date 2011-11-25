/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.phases;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.virtual.*;

public class BoxingEliminationPhase extends Phase {

    @Override
    protected void run(StructuredGraph graph) {
        for (BoxNode boxNode : graph.getNodes(BoxNode.class)) {
            tryEliminate(boxNode, graph);
        }
    }

    private void tryEliminate(BoxNode boxNode, StructuredGraph graph) {

        System.out.println("try elminate on " + boxNode);
        for (Node n : boxNode.usages()) {
            if (!(n instanceof FrameState) && !(n instanceof UnboxNode)) {
                // Elimination failed, because boxing object escapes.
                return;
            }
        }

        ValueNode virtualValueNode = null;
        VirtualObjectNode virtualObjectNode = null;
        FrameState stateAfter = boxNode.stateAfter();
        for (Node n : boxNode.usages().snapshot()) {
            if (n == stateAfter) {
                n.replaceFirstInput(boxNode, null);
            } else if (n instanceof FrameState) {
                if (virtualValueNode == null) {
                    virtualObjectNode = graph.add(new VirtualObjectNode(boxNode.exactType(), 1));
                    virtualValueNode = graph.add(new VirtualObjectFieldNode(virtualObjectNode, null, boxNode.source(), 0));
                }
                ((FrameState) n).addVirtualObjectMapping(virtualValueNode);
                n.replaceFirstInput(boxNode, virtualObjectNode);
            } else if (n instanceof UnboxNode) {
                ((UnboxNode) n).replaceAndUnlink(boxNode.source());
            } else {
                assert false;
            }
        }

        System.out.println("ELIMINATED: " + boxNode);
        boxNode.setStateAfter(null);
        stateAfter.safeDelete();
        FixedNode next = boxNode.next();
        boxNode.setNext(null);
        boxNode.replaceAtPredecessors(next);
        boxNode.safeDelete();
    }
}
