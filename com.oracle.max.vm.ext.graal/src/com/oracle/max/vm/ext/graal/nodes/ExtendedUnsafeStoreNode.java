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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;

/**
 * As {@link UnsafeStoreNode} but allows a non-constant displacement.
 */
public class ExtendedUnsafeStoreNode extends ExtendedUnsafeAccessNode implements Lowerable {

    @Input ValueNode value;
    @Input(notDataflow = true) private FrameState stateAfter;


    private ExtendedUnsafeStoreNode(ValueNode object, ValueNode displacement, ValueNode offset, ValueNode value, Kind accessKind) {
        super(StampFactory.forVoid(), object, displacement, offset, accessKind);
        this.value = value;
    }

    public static FixedWithNextNode create(ValueNode object, ValueNode displacement, ValueNode offset, ValueNode value, Kind accessKind) {
        if (displacement.isConstant()) {
            return new UnsafeStoreNode(StampFactory.forVoid(), object, displacement.asConstant().asInt(), offset, value, accessKind);
        } else {
            return new ExtendedUnsafeStoreNode(object, displacement, offset, value, accessKind);
        }
    }

    public FrameState stateAfter() {
        return stateAfter;
    }

    public void setStateAfter(FrameState x) {
        assert x == null || x.isAlive() : "frame state must be in a graph";
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    public boolean hasSideEffect() {
        return true;
    }

    public ValueNode value() {
        return value;
    }

    @Override
    public void lower(LoweringTool tool, LoweringType loweringType) {
        tool.getRuntime().lower(this, tool);
    }

}
