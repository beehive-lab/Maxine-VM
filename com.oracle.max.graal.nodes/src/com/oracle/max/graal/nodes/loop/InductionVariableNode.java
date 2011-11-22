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
import com.oracle.max.graal.util.*;
import com.sun.cri.ci.*;

/**
 * This is a base class for all induction variables nodes.
 */
public abstract class InductionVariableNode extends FloatingNode {

    public static enum StrideDirection {
        Up,
        Down;

        public static StrideDirection opposite(StrideDirection d) {
            if (d == Up) {
                return Down;
            }
            if (d == Down) {
                return Up;
            }
            return null;
        }
    }

    public InductionVariableNode(CiKind kind) {
        super(kind);
        assert kind.isInt() || kind.isLong();
    }

    /**
     * Retruns the loopBeginNode corresponding to the loop this induction variables is attached to.
     * @return the loopBeginNode corresponding to the loop this induction variables is attached to.
     */
    public abstract LoopBeginNode loopBegin();

    /**
     * This will make the induction be initialized with the value it should have had on the second iteration of the loop.
     */
    public abstract void peelOneIteration();

    /**
     * Transforms this induction variable to generic nodes (Phis, arithmetics...).
     * @return the generic node that computes the value of this induction variables.
     */
    public abstract ValueNode lowerInductionVariable();

    /**
     * Checks if the provided induction variable is the value that this induction variable will have on the next iteration.
     * @param other the induction variable this check should run against
     * @return true if the provided induction variable is the value that this induction variable will have on the next iteration.
     */
    public abstract boolean isNextIteration(InductionVariableNode other);

    /**
     * Tries to statically find the minimum value that this induction variable can have over all possible iterations at a specific {@code point} in the CFG.
     * @param point the point in the CFG from which static information will be collected
     * @return the minimum value if it could be found, null otherwise
     */
    public abstract ValueNode minValue(FixedNode point);

    /**
     * Tries to statically find the maximum value that this induction variable can have over all possible iterations at a specific {@code point} in the CFG.
     * @param point the point in the CFG from which static information will be collected
     * @return the maximum value if it could be found, null otherwise
     */
    public abstract ValueNode maxValue(FixedNode point);

    /**
     * Tries to statically find the direction of this induction variable.
     * @return returns {@link StrideDirection#Up Up} if this variable is known to be increasing, {@link StrideDirection#Down Down}  if it is know to decrease, null otherwise.
     */
    public abstract StrideDirection strideDirection();

    public ValueNode searchExtremum(FixedNode point, StrideDirection direction) {
        //LoopBeginNode upTo = loopBegin();
        //TODO (gd) collect conditions up the dominating CFG nodes path, stop as soon as we find a matching condition, it will usually be the 'narrowest'
        return null;
    }
}
