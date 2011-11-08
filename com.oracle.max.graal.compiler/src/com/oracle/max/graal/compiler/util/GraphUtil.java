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
package com.oracle.max.graal.compiler.util;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;

public class GraphUtil {

    public static void killCFG(FixedNode node) {
        for (Node successor : node.successors()) {
            if (successor != null) {
                node.replaceFirstSuccessor(successor, null);
                assert !node.isDeleted();
                killCFG((FixedNode) successor);
            }
        }
        if (node instanceof EndNode) {
            EndNode end = (EndNode) node;
            MergeNode merge = end.merge();
            if (merge instanceof LoopBeginNode) {
                for (PhiNode phi : merge.phis()) {
                    ValueNode value = phi.valueAt(0);
                    phi.replaceAndDelete(value);
                }
                killCFG(merge);
            } else {
                merge.removeEnd(end);
                if (merge.phiPredecessorCount() == 1) {
                    for (PhiNode phi : merge.phis()) {
                        ValueNode value = phi.valueAt(0);
                        phi.replaceAndDelete(value);
                    }
                    Node replacedSux = merge.phiPredecessorAt(0);
                    Node pred = replacedSux.predecessor();
                    assert replacedSux instanceof EndNode;
                    FixedNode next = merge.next();
                    merge.setNext(null);
                    pred.replaceFirstSuccessor(replacedSux, next);
                    merge.delete();
                    replacedSux.delete();
                }
            }
        }
        propagateKill(node, null);
    }

    private static void propagateKill(Node node, Node input) {
        if (node instanceof PhiNode) {
            node.replaceFirstInput(input, null);
        } else {
            for (Node usage : node.usages().snapshot()) {
                if (usage instanceof FloatingNode && !usage.isDeleted()) {
                    propagateKill(usage, node);
                }
            }
            // null out remaining usages
            node.replaceAtUsages(null);
            node.delete();
        }
    }

    public static void killFloating(FloatingNode node) {
        if (node.usages().size() == 0) {
            node.clearInputs();
            node.delete();
        }
    }
}
