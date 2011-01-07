/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Utilities for manipulating and decoding the entries of a
 * target method's {@linkplain TargetMethod#stopPositions() stop positions} array.
 *
 * The format of an entry a stop positions array is shown below.
 * <pre>
 *   0                                                        29  30  31
 *  +------------------------------------------------------------+---+---+
 *  |                         position                           | N |   |
 *  +------------------------------------------------------------+---+---+
 *
 *  N: If set, then 'position' denotes a native function call
 * </pre>
 *
 * @author Doug Simon
 */
public class StopPositions {
    public static final int NATIVE_FUNCTION_CALL = 0x40000000;
    public static final int POSITION_MASK = ~NATIVE_FUNCTION_CALL;

    private final int[] stopPositions;

    /**
     * Creates an object that can be used to access the information in a target method's
     * {@linkplain TargetMethod#stopPositions() stop positions} array.
     */
    public StopPositions(int[] stopPositions) {
        this.stopPositions = stopPositions;
    }

    /**
     * Gets the position denoted by a given index in a given stop positions array.
     *
     * @param stopPositions a target method's {@linkplain TargetMethod#stopPositions() stop positions} array
     * @param index an index within {@code stopPositions}
     */
    public static int get(int[] stopPositions, int index) {
        return stopPositions[index] & POSITION_MASK;
    }

    /**
     * Gets the index of a given position in a given stop positions array.
     *
     * @param stopPositions a target method's {@linkplain TargetMethod#stopPositions() stop positions} array
     * @param position a position to search for
     * @return the index of {@code position} in {@code stopPositions} or -1 if not found
     */
    public static int indexOf(int[] stopPositions, int position) {
        if (stopPositions != null) {
            for (int i = 0; i < stopPositions.length; ++i) {
                if (get(stopPositions, i) == position) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Determines if a given position denotes a native function call.
     *
     * @param stopPositions a target method's {@linkplain TargetMethod#stopPositions() stop positions} array
     * @param position a position within a target method
     */
    public static boolean isNativeFunctionCallPosition(int[] stopPositions, int position) {
        if (stopPositions != null) {
            for (int i = 0; i < stopPositions.length; ++i) {
                if (get(stopPositions, i) == position && isNativeFunctionCall(stopPositions, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if a given index in a given stop positions array denotes the position
     * of a native function call.
     *
     * @param stopPositions a target method's {@linkplain TargetMethod#stopPositions() stop positions} array
     * @param index an index within {@code stopPositions}
     */
    public static boolean isNativeFunctionCall(int[] stopPositions, int index) {
        return (stopPositions[index] & NATIVE_FUNCTION_CALL) != 0;
    }

    /**
     * Gets the position denoted by a given index.
     *
     * @param index an index within the stop positions array wrapped by this object
     */
    public int get(int index) {
        return get(stopPositions, index);
    }

    /**
     * Gets the length of the stop positions array wrapped by this object.
     */
    public int length() {
        return stopPositions.length;
    }

    /**
     * Determines if a given index in a given stop positions array denotes the position
     * of a native function call.
     *
     * @param index an index within {@code stopPositions}
     */
    public boolean isNativeFunctionCall(int index) {
        return (stopPositions[index] & NATIVE_FUNCTION_CALL) != 0;
    }

    /**
     * Determines if a given position denotes a native function call.
     *
     * @param position a position within a target method
     */
    public boolean isNativeFunctionCallPosition(int position) {
        return isNativeFunctionCallPosition(stopPositions, position);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(stopPositions.length * 15);
        sb.append("{");
        for (int i = 0; i < stopPositions.length; ++i) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(i).append(" -> ").append(get(i));
            if (isNativeFunctionCall(stopPositions, i)) {
                sb.append(" | NFC");
            }
        }
        return sb.append("}").toString();

    }
}
