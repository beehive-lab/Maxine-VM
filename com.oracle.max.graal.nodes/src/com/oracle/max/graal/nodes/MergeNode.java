/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.nodes;

import java.util.*;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.spi.*;

/**
 * Denotes the merging of multiple control-flow paths.
 */
public class MergeNode extends BeginNode implements Node.IterableNodeType {

    @Input private final NodeInputList<EndNode> ends = new NodeInputList<EndNode>(this);

    @Override
    public boolean needsStateAfter() {
        return false;
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitMerge(this);
    }

    public int endIndex(EndNode end) {
        return ends.indexOf(end);
    }

    public void addEnd(EndNode end) {
        ends.add(end);
    }

    public int endCount() {
        return ends.size();
    }

    public EndNode endAt(int index) {
        return ends.get(index);
    }

    public Iterable<? extends Node> phiPredecessors() {
        return ends;
    }

    @Override
    public Iterable<EndNode> cfgPredecessors() {
        return ends;
    }

    @Override
    public Iterable< ? extends Node> dataInputs() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("merge #");
        builder.append(id());
        builder.append(" [");
        boolean hasSucc = false;
        for (Node s : this.successors()) {
            if (hasSucc) {
                builder.append(", ");
            }
            builder.append("#");
            if (s != null) {
                builder.append(s.id());
            } else {
                builder.append("null");
            }
            hasSucc = true;
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Determines if a given node is a phi whose {@linkplain PhiNode#merge() merge} is this node.
     *
     * @param value the instruction to test
     * @return {@code true} if {@code value} is a phi and its merge is {@code this}
     */
    public boolean isPhiAtMerge(Node value) {
        return value instanceof PhiNode && ((PhiNode) value).merge() == this;
    }


    /**
     * Formats a given instruction as a value in a {@linkplain FrameState frame state}. If the instruction is a phi defined at a given
     * block, its {@linkplain PhiNode#valueCount() inputs} are appended to the returned string.
     *
     * @param index the index of the value in the frame state
     * @param value the frame state value
     * @return the instruction representation as a string
     */
    public String stateString(int index, ValueNode value) {
        StringBuilder sb = new StringBuilder(30);
        sb.append(String.format("%2d  %s", index, ValueUtil.valueString(value)));
        if (value instanceof PhiNode) {
            PhiNode phi = (PhiNode) value;
            // print phi operands
            if (phi.merge() == this) {
                sb.append(" [");
                for (int j = 0; j < phi.valueCount(); j++) {
                    sb.append(' ');
                    ValueNode operand = phi.valueAt(j);
                    if (operand != null) {
                        sb.append(ValueUtil.valueString(operand));
                    } else {
                        sb.append("NULL");
                    }
                }
                sb.append("] ");
            }
        }
        return sb.toString();
    }

    public void removeEnd(EndNode pred) {
        int predIndex = ends.indexOf(pred);
        assert predIndex != -1;
        ends.remove(predIndex);

        for (Node usage : usages()) {
            if (usage instanceof PhiNode) {
                ((PhiNode) usage).removeInput(predIndex);
            }
        }
    }

    public int phiPredecessorCount() {
        return endCount();
    }

    public int phiPredecessorIndex(Node pred) {
        EndNode end = (EndNode) pred;
        return endIndex(end);
    }

    public Node phiPredecessorAt(int index) {
        return endAt(index);
    }

    public Collection<PhiNode> phis() {
        return ValueUtil.filter(this.usages(), PhiNode.class);
    }
}
