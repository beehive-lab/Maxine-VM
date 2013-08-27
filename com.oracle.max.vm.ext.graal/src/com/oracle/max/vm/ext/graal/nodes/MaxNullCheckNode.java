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
package com.oracle.max.vm.ext.graal.nodes;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.max.vm.ext.graal.snippets.*;

/**
 * A vehicle for creating a null check within a snippet that lowers, say, a {@link ArrayLengthNode}, so it
 * "looks like" a method call, using the {@link #nullCheck} {@link NodeIntrinsic}. This node is then lowered using
 * {@link LoweringTool#createNullCheckGuard}. The lowering is actually done immediately after the instantiation
 * of the snippet to match the behavior had the guard been created explicitly.
 *
 */
public class MaxNullCheckNode extends MaxCheckNode {

    private @Input ValueNode object;

    public MaxNullCheckNode(ValueNode object) {
        this.object = object;
    }

    public ValueNode object() {
        return object;
    }

    public void lower(LoweringTool tool, LoweringType loweringType) {
        // The big assumption here is that the snippet that contains this null check created
        // a graph where the next node is the one that should be guarded.
        GuardedNode guardedNode = (GuardedNode) this.next();
        if (guardedNode.getGuard() != null) {
            // This can happen, say, with an UnresolvedFieldNode, as the ReadNode
            // is already guarded with a MergeNode from the (runtime) isVolatile check.
            if (object.objectStamp().nonNull()) {
                // Check not necessary anyway
                graph().removeFixed(this);
            } else {
                // Create a new GuardingNode with the same anchor as guardedNode
                GuardingNode guard = graph().unique(new GuardNode(graph().unique(new IsNullNode(object)), guardedNode.getGuard(),
                                DeoptimizationReason.NullCheckException, DeoptimizationAction.InvalidateReprofile, true));
                // Now create a ValueAnchorNode that is anchored to the original guard
                MaxMiscLowerings.FixedGuardLowering.lower(this, guard);
                // Discard the guard on guardNode as the ValueAnchorNode now does the job.
                guardedNode.setGuard(null);
            }
        } else {
            tool.createNullCheckGuard(guardedNode, object);
            graph().removeFixed(this);
        }
    }

    @NodeIntrinsic
    public static native void nullCheck(Object object);

}
