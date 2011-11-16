/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.platform.Platform.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.runtime.*;

/**
 * A set of safepoints sorted by their {@linkplain #posAt(int) positions}. The information for each safepoint
 * is encoded in an {@code int} as shown below.
 * <pre>
 *
 *   0                                            25 26 27 28 29 30 31
 *  +--------------------------------------------+--------+--+--+--+--+
 *  |                         position           |        |  |  |  |  |
 *  +--------------------------------------------+--------+--+--+--+--+
 *                                                    ^    ^  ^  ^  ^
 *                                                    |    |  |  |  |
 *                                                    |    |  |  |  +---- NATIVE_CALL
 *                                                    |    |  |  +------- INDIRECT_CALL
 *                                                    |    |  +---------- DIRECT_CALL
 *                                                    |    +------------- TEMPLATE_CALL
 *                                                    +------------------ cause position offset
 * </pre>
 *
 * The width of the 'position' field supports a code array of up to 32Mb.
 */
public final class Safepoints {

    /**
     * Attributes of a safepoint.
     */
    public static class Attr {
        public final String name;

        // Bit field info.
        private final int mask;

        @HOSTED_ONLY
        Attr(String name, int bit) {
            this.name = name;
            this.mask = 1 << bit;
        }

        /**
         * Determines if this attribute value is set for a given safepoint.
         *
         * @param safepoint the safepoint to query
         */
        public boolean isSet(int safepoint) {
            return (safepoint & mask) != 0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Attribute value denoting a direct call. A direct call is a call instruction with
     * an operand encoding the offset from the call instruction to the target of the call.
     */
    public static final Attr DIRECT_CALL = new Attr("DIRECT_CALL", 29);

    /**
     * Attribute value denoting an indirect call. An indirect call is a call instruction
     * with a register or memory operand specifying the target address of the call.
     */
    public static final Attr INDIRECT_CALL = new Attr("INDIRECT_CALL", 30);

    /**
     * Attribute value denoting a native function call. A safepoint with this attribute
     * set will also have the {@link #INDIRECT_CALL} attribute set.
     */
    public static final Attr NATIVE_CALL = new Attr("NATIVE_CALL", 31);

    /**
     * Attribute value denoting a template call. A safepoint with this attribute
     * set will also have either the {@link #DIRECT_CALL} or {@link #INDIRECT_CALL} attribute set.
     */
    public static final Attr TEMPLATE_CALL = new Attr("TEMPLATE_CALL", 28);

    /**
     * Complete set of declared safepoint attributes.
     */
    public static final Attr[] ALL_ATTRS;
    static {
        ArrayList<Attr> attrs = new ArrayList<Attr>();
        int mask = 0;
        for (final Field field : Safepoints.class.getDeclaredFields()) {
            if (field.getType() == Attr.class && Modifier.isStatic(field.getModifiers())) {
                try {
                    Attr attr = (Attr) field.get(null);
                    assert !attr.isSet(mask) : attr + " has the same mask as another attribute";
                    mask |= attr.mask;
                    attrs.add(attr);
                } catch (Exception e) {
                    throw FatalError.unexpected("Could not read value of " + field, e);
                }
            }
        }
        ALL_ATTRS = attrs.toArray(new Attr[attrs.size()]);
    }

    /**
     * Mask for extracting position.
     */
    public static final int POS_MASK = (1 << 25) - 1;

    private static final int CAUSE_OFFSET_MASK = ((1 << 28) - 1) & ~POS_MASK;
    private static final int CAUSE_OFFSET_SHIFT = 25;
    private static final int MAX_CAUSE_OFFSET = 7;

    /**
     * Mask for extracting attributes.
     */
    public static final int ATTRS_MASK = -1 & ~(POS_MASK | CAUSE_OFFSET_MASK);

    private final int[] safepoints;

    public static final Safepoints NO_SAFEPOINTS = new Safepoints();

    /**
     * Creates an object encapsulating a set of safepoints.
     */
    public Safepoints(int... safepoints) {
        this.safepoints = safepoints;
        assert validateSafepoints(safepoints);
    }

    private boolean validateSafepoints(int[] safepoints) {
        for (int i = 0; i < safepoints.length; i++) {
            int safepoint = safepoints[i];
            boolean isCall = isCall(safepoint);
            assert !NATIVE_CALL.isSet(safepoint) || INDIRECT_CALL.isSet(safepoint) : "a native call must be an indirect call";
            assert !TEMPLATE_CALL.isSet(safepoint) || isCall : "a template call must be a direct or indirect call";
            assert causePos(safepoint) == pos(safepoint) || isCall : "cause position can only be different from safepoint position for a call";
            if (i != 0) {
                int pos = pos(safepoint);
                int prev = safepoints[i - 1];
                int prevPos = pos(prev);
                assert prevPos < pos : "safepoint positions are not sorted: " + prevPos + " >= " + pos;
            }
        }
        return true;
    }

    /**
     * Gets the position denoted by a given index.
     *
     * @param index an index within the safepoint positions array wrapped by this object
     */
    public int posAt(int index) {
        return pos(safepoints[index]);
    }

    /**
     * Gets the position of a safepoint.
     *
     * @param safepoint an encoded safepoint
     */
    public static int pos(int safepoint) {
        return safepoint & POS_MASK;
    }

    /**
     * Gets the position of the instruction that is the reason for safepoint at a given index.
     * On some platforms such as x86, the safepoint for a call is recorded at the position of
     * the instruction following the call (i.e. the return address). However, the cause for
     * the safepoint is the call instruction itself. Apart from safepoints for calls,
     * {@code posAt(i) == causePosAt(i)}.
     *
     * @param index the index of a safepoint
     */
    public int causePosAt(int index) {
        return causePos(safepoints[index]);
    }

    /**
     * Reads the cause position from an encoded safepoint.
     *
     * @see #causePosAt(int)
     * @param safepoint an encoded safepoint
     */
    public static int causePos(int safepoint) {
        int pos = pos(safepoint);
        int causeOffset = (safepoint & CAUSE_OFFSET_MASK) >> CAUSE_OFFSET_SHIFT;
        return pos - causeOffset;
    }

    /**
     * Determines if a given safepoint is a call.
     *
     * @param safepoint the safepoint to test
     */
    public static boolean isCall(int safepoint) {
        return INDIRECT_CALL.isSet(safepoint) || DIRECT_CALL.isSet(safepoint);
    }

    /**
     * Gets the safepoint at a given index.
     *
     * @param index the index of the safepoint to retrieve
     */
    public int safepointAt(int index) {
        return safepoints[index];
    }

    /**
     * Gets the number of safepoints.
     */
    public int size() {
        return safepoints.length;
    }

    /**
     * Determines if a given attribute is set for a given safepoint.
     *
     * @param a the attribute to test
     * @param index the index denoting the safepoint to query
     */
    public boolean isSetAt(Attr a, int index) {
        return a.isSet(safepoints[index]);
    }

    /**
     * Finds the safepoint corresponding to a given code position.
     *
     * @param pos the position to search for
     * @return the index of the safepoint whose {@linkplain #posAt(int) position} is equal to {@code pos} or -1 if no
     *         such safepoint exists
     */
    public int indexOf(int pos) {
        // Use binary search since safepoints are sorted by position
        int left = 0;
        int right = safepoints.length;
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            int p = posAt(middle);
            if (p > pos) {
                right = middle;
            } else if (p == pos) {
                return middle;
            } else {
                left = middle + 1;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first direct call safepoint at or after a specified start index.
     * If no such safepoint exists then -1 is returned.
     * <p>
     * To iterate over the direct calls, use the following loop:
     *
     * <pre>
     * for (int i = safepoints.nextDirectCall(0); i &gt;= 0; i = safepoints.nextDirectCall(i + 1)) {
     *     // operate on safepoint i here
     * }
     * </pre>
     *
     * @param i the index to start searching from (inclusive)
     * @return the index of the first direct call safepoint between {@code [i .. length())} or -1 if there is no direct call safepoint in this range
     * @throws IndexOutOfBoundsException if the specified index is negative.
     */
    public int nextDirectCall(int i) {
        while (i < safepoints.length) {
            if (isSetAt(DIRECT_CALL, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int numberOfDirectCalls() {
        int n = 0;
        for (int i = 0; i < safepoints.length; i++) {
            if (isSetAt(DIRECT_CALL, i)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(safepoints.length * 15);
        sb.append("{");
        for (int i = 0; i < safepoints.length; ++i) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append(i).append(" -> ").append(posAt(i));
            int attrs = safepoints[i] & ATTRS_MASK;
            if (attrs != 0) {
                for (Attr a : Safepoints.ALL_ATTRS) {
                    if (a.isSet(attrs)) {
                        sb.append(" | ").append(a.name);
                    }
                }

            }
        }
        return sb.append("}").toString();

    }

    /**
     * Gets the safepoint position associated with a call.
     *
     * @param callPos position of the call instruction
     * @param callSize size of the call instruction
     */
    public static int safepointPosForCall(int callPos, int callSize) {
        if (platform().isa == ISA.AMD64) {
            return callPos + callSize;
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Encodes a safepoint with no attributes.
     *
     * @param safepointPos the position of the safepoint (also its {@linkplain #causePosAt(int) cause} position)
     * @param an attribute of the safepoint
     */
    public static int make(int safepointPos) {
        return make(safepointPos, safepointPos, 0);
    }

    /**
     * Encodes a safepoint with a single attribute.
     *
     * @param safepointPos the position of the safepoint
     * @param causePos the {@linkplain #causePosAt(int) cause} position of the safepoint
     * @param a an attribute of the safepoint
     */
    public static int make(int safepointPos, int causePos, Attr a) {
        return make(safepointPos, causePos, a.mask);
    }

    /**
     * Encodes a safepoint with two attributes.
     *
     * @param safepointPos the position of the safepoint
     * @param causePos the {@linkplain #causePosAt(int) cause} position of the safepoint
     * @param a1 an attribute of the safepoint
     * @param a2 an attribute of the safepoint
     */
    public static int make(int safepointPos, int causePos, Attr a1, Attr a2) {
        assert (a1.mask & a2.mask) == 0;
        return make(safepointPos, causePos, a1.mask | a2.mask);
    }

    /**
     * Encodes a safepoint.
     *
     * @param safepointPos the position of the safepoint
     * @param causePos the {@linkplain #causePosAt(int) cause} position of the safepoint
     * @param attrs mask of the safepoint's attributes
     */
    public static int make(int safepointPos, int causePos, int attrs) {
        assert pos(safepointPos) == safepointPos : "safepoint position out of range";
        assert (attrs & ATTRS_MASK) == attrs;
        int causeOffset = safepointPos - causePos;
        assert causeOffset >= 0 && causeOffset <= MAX_CAUSE_OFFSET : "cause position out of range";
        return safepointPos | (causeOffset << CAUSE_OFFSET_SHIFT) | attrs;
    }
}
