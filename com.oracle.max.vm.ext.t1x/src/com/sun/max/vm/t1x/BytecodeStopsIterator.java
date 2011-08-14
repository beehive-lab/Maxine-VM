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
package com.sun.max.vm.t1x;

import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.compiler.target.*;

/**
 * An iterator over a map from BCIs to {@linkplain Stops stops}. The context of the BCIs
 * and stops is a {@link T1XTargetMethod}.
 *
 * Each key in the map is the BCI of a bytecode instruction's opcode. Each value in the map is the set of
 * stops in the machine code generated for the bytecode instruction denoted by the associated key. A stop is represented
 * by its index in {@link T1XTargetMethod#stops()}.
 */
public class BytecodeStopsIterator implements BCIIterator {

    private final int[] map;
    private int bciCursor;
    private int stopIndex0;
    private int stopIndex;
    private final int firstStopIndex;

    /**
     * Creates an iterator over a mapping from BCIs to stops.
     *
     * @param map an encoded map of BCI to stops. The entries in the map are sorted in ascending order of
     *            the keys. Each entry is encoded as the BCI followed the number of stops in the template
     *            for the instruction at BCI.
     * @param firstStopIndex
     */
    public BytecodeStopsIterator(int[] map, int firstStopIndex) {
        assert assertIsValidMap(map);
        this.map = map;
        this.firstStopIndex = firstStopIndex;
    }

    /**
     * Resets this iterator to the first entry in the map.
     *
     * @see #next()
     */
    public void reset() {
        bciCursor = 0;
        stopIndex0 = firstStopIndex;
        stopIndex = firstStopIndex;
    }

    /**
     * Gets the BCI of the map entry at which this iterator is positioned.
     *
     * @return {@code -1} if this iterator is already at the end of the map
     * @see #next()
     */
    public int bci() {
        if (bciCursor < map.length) {
            return map[bciCursor];
        }
        assert bciCursor == map.length;
        return -1;
    }

    /**
     * Advances this iterator to the next entry in the map.
     *
     * To iterate over the entries in the map with a {@code BytecodeStopsIterator} instance {@code iter}, use the
     * following loop:
     *
     * <pre>
     * iter.reset();
     * for (int bci = iter.bci(); bci != -1; bci = iter.next()) {
     *     // operate on 'bci'
     * }
     * </pre>
     *
     * Note that the call to {@link #reset()} above is unnecessary if neither {@link #next()} nor
     * {@link #nextStopIndex(boolean)} has been invoked on {@code iter} since it was constructed or since the last
     * call to {@link #reset()}.
     *
     * @return the BCI of the entry to which this iterator was advanced or {@code -1} if this iterator is
     *         already at the end of the map
     */
    public int next() {
        if (bciCursor < map.length) {
            int nStops = map[bciCursor + 1];
            bciCursor += 2;
            if (bciCursor < map.length) {
                stopIndex0 += nStops;
                stopIndex = stopIndex0;
                return map[bciCursor];
            }
        }
        return -1;
    }

    /**
     * Gets the next stop for the entry at which this iterator is currently positioned.
     *
     * To iterate over the stops for the entry at which an iterator is currently positioned in the map with a {@code
     * BytecodeStopsIterator} instance {@code iter}, use the following loop:
     *
     * <pre>
     * for (int stopIndex = iter.nextStopIndex(true); stopIndex != -1; stopIndex = iter.nextStopIndex(false)) {
     *     // operate on 'stopIndex' including calling 'iter.isDirectRuntimeCall()' if necessary
     * }
     * </pre>
     *
     * @param reset if {@code true}, then this iterator is reset to the first stop of the current entry
     * @return the next stop for the current entry or -1 if this iterator is either already at the end of the map or
     *         past all the stops of the current entry. A non-negative return value is the index of the stop in
     *         {@link T1XTargetMethod#stops()}.
     */
    public int nextStopIndex(boolean reset) {
        if (reset) {
            stopIndex = stopIndex0;
        }
        if (bciCursor < map.length) {
            int end = map[bciCursor + 1] + stopIndex0;
            if (stopIndex < end) {
                int result = stopIndex;
                ++stopIndex;
                return result;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        final BytecodeStopsIterator copy = new BytecodeStopsIterator(map, firstStopIndex);
        final StringBuilder sb = new StringBuilder();
        for (int bci = copy.bci(); bci != -1; bci = copy.next()) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(bci).append(" -> {");
            boolean first = true;
            for (int stopIndex = copy.nextStopIndex(true); stopIndex != -1; stopIndex = copy.nextStopIndex(false)) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(stopIndex);
            }
            sb.append("}");
        }
        return sb.toString();
    }

    public static boolean assertIsValidMap(int[] map) {
        assert map.length % 2 == 0;
        int prev = -1;
        int nStops = 0;
        for (int i = 0; i < map.length; i += 2) {
            int bci = map[i];
            nStops += map[i + 1];
            assert bci > prev;
            prev = bci;
        }
        return true;
    }
}
