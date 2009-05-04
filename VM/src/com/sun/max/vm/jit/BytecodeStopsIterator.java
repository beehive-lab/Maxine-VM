/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.jit;

import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.compiler.target.*;

/**
 * An iterator over a map from bytecode positions to {@linkplain StopType stops}. The context of the bytecode positions
 * and stops is a {@linkplain JitTargetMethod JIT compiled method}.
 *
 * Each key in the map is the bytecode position of a bytecode instruction's opcode. Each value in the map is the set of
 * stops in the machine code generated for the bytecode instruction denoted by the associated key. A stop is represented
 * by its index in the {@linkplain JitTargetMethod#stopPositions() stops table} of the JIT compiled method.
 *
 * @author Doug Simon
 */
public class BytecodeStopsIterator implements BytecodePositionIterator {

    /**
     * Tag for elements in {@link #_map} denoting opcode positions.
     */
    public static final int BCP_BIT = 0x80000000;

    /**
     * Tag for elements in {@link #_map} denoting a stop associated with a direct runtime call.
     */
    public static final int DIRECT_RUNTIME_CALL_BIT = 0x40000000;

    private final int[] _map;
    private int _cursor = 1;
    private int _bytecodePositionCursor;

    /**
     * Creates an iterator over a mapping from bytecode positions to {@linkplain StopType stops}.
     *
     * @param map an encoded map of bytecode position to stops. The entries in the map are sorted in ascending order of
     *            the keys. Each entry is encoded as the bytecode position followed by one or more stops. The
     *            bytecode position values in {@code map} are those that have their {@link #BCP_BIT} set. All other
     *            values are stops which are represented as their index in the
     *            {@linkplain JitTargetMethod#stopPositions() stops table} of the associated JIT compiled method.
     */
    public BytecodeStopsIterator(int[] map) {
        assert assertIsValidMap(map);
        _map = map;
    }

    /**
     * Resets this iterator to the first entry in the map.
     *
     * @see #next()
     */
    public void reset() {
        _cursor = 1;
        _bytecodePositionCursor = 0;
    }

    /**
     * Gets the bytecode position of the map entry at which this iterator is positioned.
     *
     * @return {@code -1} if this iterator is already at the end of the map
     * @see #next()
     */
    public int bytecodePosition() {
        if (_bytecodePositionCursor < _map.length) {
            assert (_map[_bytecodePositionCursor] & BCP_BIT) != 0;
            return _map[_bytecodePositionCursor] & ~BCP_BIT;
        }
        assert _bytecodePositionCursor == _map.length;
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
     * for (int bcp = iter.bytecodePosition(); bcp != -1; bcp = iter.next()) {
     *     // operate on 'bcp'
     * }
     * </pre>
     *
     * Note that the call to {@link #reset()} above is unnecessary if neither {@link #next()} nor
     * {@link #nextStopIndex(boolean)} has been invoked on {@code iter} since it was constructed or since the last
     * call to {@link #reset()}.
     *
     * @return the bytecode position of the entry to which this iterator was advanced or {@code -1} if this iterator is
     *         already at the end of the map
     */
    public int next() {
        while (_cursor < _map.length) {
            final int value = _map[_cursor++];
            if ((value & BCP_BIT) != 0) {
                _bytecodePositionCursor = _cursor - 1;
                return value & ~BCP_BIT;
            }
        }
        _bytecodePositionCursor = _cursor;
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
     *         {@linkplain JitTargetMethod#stopPositions() stops table} of the associated JIT compiled method.
     */
    public int nextStopIndex(boolean reset) {
        if (_cursor < _map.length) {
            assert _bytecodePositionCursor < _cursor;
            if (reset) {
                _cursor = _bytecodePositionCursor + 1;
            }
            final int value = _map[_cursor];
            if ((value & BCP_BIT) == 0) {
                _cursor++;
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
        if (_cursor > 0 && _cursor < _map.length) {
            final int value = _map[_cursor - 1];
            if ((value & BCP_BIT) == 0) {
                return (value & DIRECT_RUNTIME_CALL_BIT) != 0;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final BytecodeStopsIterator copy = new BytecodeStopsIterator(_map);
        final StringBuilder sb = new StringBuilder();
        for (int bytecodePosition = copy.bytecodePosition(); bytecodePosition != -1; bytecodePosition = copy.next()) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(bytecodePosition).append(" -> {");
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
        for (int v : map) {
            if ((v & BCP_BIT) != 0) {
                if (entry != -1) {
                    assert stopCount != 0 : "Entry " + entry + " in stop index table has 0 stop indexes";
                }
                stopCount = 0;
                entry++;
            } else {
                stopCount++;
            }
        }
        assert stopCount != 0 : "Entry " + entry + " in stop index table has 0 stop indexes";
        return true;
    }
}
