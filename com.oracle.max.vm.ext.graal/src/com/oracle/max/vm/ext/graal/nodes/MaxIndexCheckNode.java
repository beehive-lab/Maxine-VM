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

import static com.oracle.graal.api.code.DeoptimizationAction.*;
import static com.oracle.graal.api.meta.DeoptimizationReason.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.Node.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.max.vm.ext.graal.*;


/**
 * A vehicle for creating an index bounds check within a snippet that lowers, say, a {@link LoadIndexedNode}, so it
 * "looks like" a method call, using the {@link #checkIndex} {@link NodeIntrinsic}. This node is then lowered using
 * {@link LoweringTool#createGuard}. The lowering is actually done immediately after the instantiation
 * of the snippet to match the behavior had the guard been created explicitly.
 *
 */
public class MaxIndexCheckNode extends MaxCheckNode {

    @Input private ValueNode array;
    @Input private ValueNode index;
    @Input private ValueNode object;

    private MaxIndexCheckNode(ValueNode array, ValueNode index) {
        this.array = array;
        this.index = index;
    }

    private MaxIndexCheckNode(ValueNode array, ValueNode index, ValueNode object) {
        this.array = array;
        this.index = index;
        this.object = object;
    }

    public void lower(LoweringTool tool, LoweringType loweringType) {
        GuardedNode guardedNode = (GuardedNode) this.next();
        assert guardedNode.getGuard() == null;
        // The lowering of ArrayLength will effect the null check, so after that we know the array is not null
        ArrayLengthNode arrayLength = graph().add(new ArrayLengthNode(array));
        graph().addBeforeFixed(this, arrayLength);
        GuardingNode guard = tool.createGuard(graph().unique(new IntegerBelowThanNode(index, arrayLength)), BoundsCheckException, InvalidateReprofile);
        guardedNode.setGuard(guard);
        // Now deal with the special case of storing to an object array
        if (object != null) {
            if (!object.objectStamp().alwaysNull()) {
                ResolvedJavaType arrayType = array.objectStamp().type();
                if (arrayType != null && array.objectStamp().isExactType()) {
                    ResolvedJavaType elementType = arrayType.getComponentType();
                    if (elementType != MaxResolvedJavaType.getJavaLangObject()) {
                        CheckCastNode checkcast = graph().add(new CheckCastNode(elementType, object, null, true));
                        graph().addBeforeFixed(this, checkcast);
                    }
                } else {
                    graph().addBeforeFixed(this, graph().add(new ArrayStoreCheckNode(array, object)));
                }
            }
        }
        graph().removeFixed(this);
    }

    @NodeIntrinsic
    public static native void checkIndex(Object array, int index);

    @NodeIntrinsic
    public static native void checkIndex(Object array, int index, Object object);

}
