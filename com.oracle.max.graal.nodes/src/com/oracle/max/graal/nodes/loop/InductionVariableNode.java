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
package com.oracle.max.graal.nodes.loop;

import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;


public abstract class InductionVariableNode extends FloatingNode {

    public static enum StrideDirection {
        Up,
        Down
    }

    public InductionVariableNode(CiKind kind) {
        super(kind);
        assert kind.isInt() || kind.isLong();
    }

    public abstract LoopBeginNode loopBegin();

    public abstract void peelOneIteration();

    public abstract ValueNode lowerInductionVariable();

    public abstract boolean isNextIteration(InductionVariableNode other);

    public abstract ValueNode minValue(FixedNode point);

    public abstract ValueNode maxValue(FixedNode point);

    public abstract StrideDirection strideDirection();

    public ValueNode searchExtremum(FixedNode point, StrideDirection direction) {
        //LoopBeginNode upTo = loopBegin();
        //TODO (gd) collect conditions up the dominating CFG nodes path, stop as soon as we find a matching condition, it will usually be the 'narrowest'
        return null;
    }
}
