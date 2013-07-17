/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.vma.phases;

import java.util.*;

import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.util.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.vma.nodes.*;
import com.oracle.max.vm.ext.vma.*;

/**
 * Phase adds requested {@link AdviceNode} nodes to the graph.
 */
public class AdvicePhase extends Phase {
    private static final Map<NodeClass, EnumSet<AdviceMode>> nodeMap = new HashMap<>();
    @Override
    protected void run(StructuredGraph graph) {
        for (Node node: GraphOrder.forwardGraph(graph)) {
            EnumSet<AdviceMode> set = nodeMap.get(node.getNodeClass());
            if (set != null) {
                if (set.contains(AdviceMode.BEFORE)) {
                    insertBefore((FixedNode) node, graph.add(new AdviceNode(AdviceMode.BEFORE)));
                }
                if (set.contains(AdviceMode.AFTER)) {
                    insertAfter((FixedWithNextNode) node, graph.add(new AdviceNode(AdviceMode.AFTER)));
                }
            }
        }
        Debug.dump(graph, "After phase Advice insertion");

        // Now we lower the AdviceNodes because we can't currently control the lowering order
        // and the AdviceNode must be lowered before the node it advises.
        for (Node node: graph.getNodes().filter(AdviceNode.class).snapshot()) {
            MaxGraal.runtime().lower(node, null);
        }

    }

    /**
     * Inserts a node into the control flow of the graph.
     * @param node the node before which {@code insert} is to be inserted
     * @param insert the node being inserted
     */
    protected <T extends FixedWithNextNode> T insertBefore(FixedNode node, T insert) {
        node.replaceAtPredecessor(insert);
        insert.setNext(node);
        return insert;
    }

    protected <T extends FixedWithNextNode> T insertAfter(FixedWithNextNode node, T insert) {
        FixedNode next = node.next();
        next.replaceAtPredecessor(insert);
        insert.setNext(next);
        return insert;
    }

// START GENERATED CODE
// EDIT AND RUN VMAPhaseGenerator.main() TO MODIFY

    static {
        nodeMap.put(NodeClass.get(LoadIndexedNode.class), AdviceMode.BEFORE_AFTER_SET);
        nodeMap.put(NodeClass.get(StartNode.class), AdviceMode.AFTER_SET);
        nodeMap.put(NodeClass.get(ReturnNode.class), AdviceMode.BEFORE_SET);
        nodeMap.put(NodeClass.get(ArrayLengthNode.class), AdviceMode.AFTER_SET);
        nodeMap.put(NodeClass.get(InvokeWithExceptionNode.class), AdviceMode.BEFORE_SET);
        nodeMap.put(NodeClass.get(NewArrayNode.class), AdviceMode.AFTER_SET);
        nodeMap.put(NodeClass.get(IfNode.class), AdviceMode.BEFORE_SET);
        nodeMap.put(NodeClass.get(StoreIndexedNode.class), AdviceMode.BEFORE_SET);
        nodeMap.put(NodeClass.get(LoadFieldNode.class), AdviceMode.BEFORE_SET);
        nodeMap.put(NodeClass.get(StoreFieldNode.class), AdviceMode.BEFORE_SET);
        nodeMap.put(NodeClass.get(NewMultiArrayNode.class), AdviceMode.AFTER_SET);
        nodeMap.put(NodeClass.get(NewInstanceNode.class), AdviceMode.AFTER_SET);
    }
// END GENERATED CODE

}
