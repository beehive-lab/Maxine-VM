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

/**
 * An iterator over a map from BCIs to {@linkplain StopType stops}. The context of the BCIs
 * and stops is a {@linkplain T1XTargetMethod JIT compiled method}.
 *
 * Each key in the map is the BCI of a bytecode instruction's opcode. Each value in the map is the set of
 * stops in the machine code generated for the bytecode instruction denoted by the associated key. A stop is represented
 * by its index in the {@linkplain T1XTargetMethod#stopPositions() stops table} of the JIT compiled method.
 *
 */
public class BytecodeStopsIterator implements BCIIterator {

    /**
     * Tag for elements in {@link #map} denoting opcode positions.
     */
    public static final int BCP_BIT = 0x80000000;

    /**
     * Tag for elements in {@link #map} denoting a stop associated with a direct runtime call.
     */
    public static final int DIRECT_RUNTIME_CALL_BIT = 0x40000000;

    private final int[] map;
    private int cursor = 1;
    private int bciCursor;

    /**
     * Creates an iterator over a mapping from BCIs to {@linkplain StopType stops}.
     *
     * @param map an encoded map of BCI to stops. The entries in the map are sorted in ascending order of
     *            the keys. Each entry is encoded as the BCI followed by one or more stops. The
     *            BCI values in {@code map} are those that have their {@link #BCP_BIT} set. All other
     *            values are stops which are represented as their index in the
     *            {@linkplain T1XTargetMethod#stopPositions() stops table} of the associated JIT compiled method.
     */
    public BytecodeStopsIterator(int[] map) {
        assert assertIsValidMap(map);
        this.map = map;
    }

    /**
     * Resets this iterator to the first entry in the map.
     *
     * @see #next()
     */
    public void reset() {
        cursor = 1;
        bciCursor = 0;
    }

    /**
     * Gets the BCI of the map entry at which this iterator is positioned.
     *
     * @return {@code -1} if this iterator is already at the end of the map
     * @see #next()
     */
    public int bci() {
        if (bciCursor < map.length) {
            assert (map[bciCursor] & BCP_BIT) != 0;
            return map[bciCursor] & ~BCP_BIT;
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
        while (cursor < map.length) {
            final int value = map[cursor++];
            if ((value & BCP_BIT) != 0) {
                bciCursor = cursor - 1;
                return value & ~BCP_BIT;
            }
        }
        bciCursor = cursor;
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
     *         past all the stops of the current entry. A non-negative return value is the index of the stop in the
     *         {@linkplain T1XTargetMethod#stopPositions() stops table} of the associated JIT compiled method.
     */
    public int nextStopIndex(boolean reset) {
        if (reset) {
            cursor = bciCursor + 1;
        }
        if (cursor < map.length) {
            assert bciCursor < cursor;
            final int value = map[cursor];
            if ((value & BCP_BIT) == 0) {
                cursor++;
                return value & ~DIRECT_RUNTIME_CALL_BIT;
            }
        }
        return -1;
    }

    /**
     * Determines if the stop returned by the last call to {@link #nextStopIndex(boolean)} denotes a direct
     * call to the runtime.
     */
    public boolean isDirectRuntimeCall() {
        if (cursor > 0 && cursor < map.length) {
            final int value = map[cursor - 1];
            if ((value & BCP_BIT) == 0) {
                return (value & DIRECT_RUNTIME_CALL_BIT) != 0;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final BytecodeStopsIterator copy = new BytecodeStopsIterator(map);
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
                if (copy.isDirectRuntimeCall()) {
                    sb.append("*");
                }
            }
            sb.append("}");
        }
        return sb.toString();
    }

    public static boolean assertIsValidMap(int[] map) {
        int entry = -1;
        int stopCount = 0;
        int lastBcp = -1;
        for (int v : map) {
            if ((v & BCP_BIT) != 0) {
                if (entry != -1) {
                    assert stopCount != 0 : "Entry " + entry + " has 0 stop indexes";
                }
                final int bcp = v & ~BCP_BIT;
                assert bcp > lastBcp : "Entry " + entry + " has bcp lower or equal to preceding bcp: " + bcp + " <= " + lastBcp;
                stopCount = 0;
                entry++;
                lastBcp = bcp;
            } else {
                assert lastBcp != -1 : "Entry " + entry + " does not start with a BCI";
                stopCount++;
            }
        }
        assert stopCount != 0 : "Entry " + entry + " in stop index table has 0 stop indexes";
        return true;
    }
}
