/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import java.util.*;

/**
 * Categorization of stops. Stops are target code positions for which the locations of object references are precisely known.
 *
 * @author Doug Simon
 */
public enum StopType {
    /**
     * A position corresponding to a direct call (e.g. target method is known at compile time). The locations of object
     * references on the stack are precisely known at an indirect call.
     */
    DIRECT_CALL {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            if (n < 0 || n >= targetMethod.numberOfDirectCalls()) {
                throw new IllegalArgumentException();
            }
            return n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfDirectCalls();
        }
    },

    /**
     * A position corresponding to a register indirect call (e.g. late binding call, virtual/interface call). The
     * locations of object references on the stack are precisely known at an indirect call.
     */
    INDIRECT_CALL {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            return targetMethod.numberOfDirectCalls() + n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfIndirectCalls();
        }
    },

    /**
     * A safepoint is a position at which a thread. The locations of object references on both the stack
     * and in the registers are precisely known at a safepoint.
     */
    SAFEPOINT {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            return targetMethod.numberOfDirectCalls() + targetMethod.numberOfIndirectCalls() + n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfSafepoints();
        }
    };

    public static final List<StopType> VALUES = Arrays.asList(values());

    /**
     * Gets the index of the {@code n}th stop of this type in the {@linkplain TargetMethod#stopPositions() stop positions array} of a target method.
     * @param targetMethod the target method
     * @param n the index into the stop positions
     * @return the stop position index
     * @throws IllegalArgumentException if the index is invalid
     */
    public abstract int stopPositionIndex(TargetMethod targetMethod, int n) throws IllegalArgumentException;

    /**
     * Gets the number of stops of this type in a given target method.
     * @param targetMethod the target method
     * @return the number of stops of this type in the method
     */
    public abstract int numberOfStopPositions(TargetMethod targetMethod);

    /**
     * Gets the position of the {@code n}th stop of this type in a given target method.
     * @param targetMethod the target method
     * @param n the index into the stop positions
     * @return the position
     */
    public int stopPosition(TargetMethod targetMethod, int n) {
        return targetMethod.stopPosition(stopPositionIndex(targetMethod, n));
    }
}
