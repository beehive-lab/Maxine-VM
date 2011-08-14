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

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.runtime.*;

/**
 * Stop information for a target method.
 *
 * The format of a single stop (encoded as an int) is shown below.
 * <pre>
 *
 *   0                                               26 27 28 29 30 31
 *  +-----------------------------------------------+-----+--+-----+--+
 *  |                         position              |     |  |     |  |
 *  +-----------------------------------------------+-----+--+-----+--+
 *                                                      ^  ^     ^   ^
 *                                                      |  |     |   |
 *                                                      |  |     |   +--- NATIVE_FUNCTION_CALL
 *                                                      |  |     +------- DIRECT_CALL/INDIRECT_CALL/SAFEPOINT
 *                                                      |  +------------- TEMPLATE_CALL
 *                                                      +---------------- CAUSE_AT_POS_MINUS_*
 *
 * </pre>
 */
public class Stops {

    /**
     * Attributes of a stop.
     */
    public static class Attr {
        public final String name;

        // Bit field info.
        private final int valueMask;
        private final int width;
        private final int shift;
        private final int mask;

        @HOSTED_ONLY
        Attr(String name, int value, int width, int shift) {
            this.name = name;
            this.width = width;
            this.shift = shift;
            this.mask = ((1 << width) - 1) << shift;
            assert value >= 0;
            assert value >> width == 0;
            assert shift + width <= 32;
            this.valueMask = value << shift;
            assert isSet(this.valueMask);
        }

        /**
         * Determines if this attribute value is set for a given stop.
         *
         * @param stop the stop to query
         */
        public boolean isSet(int stop) {
            return (stop & mask) == valueMask;
        }
    }

    /**
     * Flags value denoting a direct call.
     */
    public static final Attr DIRECT_CALL = new Attr("DIRECT_CALL", 1, 2, 29);

    /**
     * Flags value denoting an indirect call.
     */
    public static final Attr INDIRECT_CALL = new Attr("INDIRECT_CALL", 2, 2, 29);

    /**
     * Flags value denoting a safepoint poll or implicit exception. That is, an instruction that can trap.
     */
    public static final Attr SAFEPOINT = new Attr("SAFEPOINT", 3, 2, 29);

    /**
     * Flags bit denoting a native function call.
     */
    public static final Attr NATIVE_CALL = new Attr("NATIVE_CALL", 1, 1, 31);

    /**
     * Flags bit denoting a template call.
     */
    public static final Attr TEMPLATE_CALL = new Attr("TEMPLATE_CALL", 1, 1, 28);

    public static final Attr CAUSE_AT_POS_MINUS_0 = new Attr("CAUSE_AT_POS_MINUS_0", 0, 2, 26);
    public static final Attr CAUSE_AT_POS_MINUS_2 = new Attr("CAUSE_AT_POS_MINUS_2", 1, 2, 26);
    public static final Attr CAUSE_AT_POS_MINUS_3 = new Attr("CAUSE_AT_POS_MINUS_3", 2, 2, 26);
    public static final Attr CAUSE_AT_POS_MINUS_5 = new Attr("CAUSE_AT_POS_MINUS_5", 3, 2, 26);

    /**
     * Complete set of declared safepoint attributes.
     */
    public static final Attr[] ALL_ATTRS;
    static {
        ArrayList<Attr> attrs = new ArrayList<Attr>();
        for (final Field field : Stops.class.getDeclaredFields()) {
            if (field.getType() == Attr.class && Modifier.isStatic(field.getModifiers())) {
                try {
                    attrs.add((Attr) field.get(null));
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
    public static final int POS_MASK = (1 << 26) - 1;

    /**
     * Mask for extracting attributes.
     */
    public static final int ATTRS_MASK = ~POS_MASK;

    private final int[] stops;

    public static final Stops NO_STOPS = new Stops();

    /**
     * Creates an object encapsulating a set of stops.
     */
    public Stops(int... stops) {
        this.stops = stops;
        assert validateStops(stops.clone());
    }

    private boolean validateStops(int[] stops) {
        Arrays.sort(stops);
        for (int i = 0; i < stops.length; i++) {
            int stop = stops[i];
            boolean isCall = DIRECT_CALL.isSet(stop) || INDIRECT_CALL.isSet(stop);
            assert i == 0 || (stops[i - 1] & Stops.POS_MASK) != (stop & Stops.POS_MASK) : "stop positions are not unique: " + stop;
            assert (stop & ATTRS_MASK) != 0 : "no flags set for stop " + (stop & Stops.POS_MASK);
            assert isCall || SAFEPOINT.isSet(stop) : "a stop must be a call or a safepoint";
            assert !NATIVE_CALL.isSet(stop) || INDIRECT_CALL.isSet(stop) : "a native call must be an indirect call";
            assert !TEMPLATE_CALL.isSet(stop) || isCall : "a template call must be a direct or indirect call";
            assert CAUSE_AT_POS_MINUS_0.isSet(stop) || isCall : "non-zero cause offset can onlybe for a call";
        }
        return true;
    }

    /**
     * Gets the position denoted by a given index.
     *
     * @param index an index within the stop positions array wrapped by this object
     */
    public int posAt(int index) {
        return stops[index] & POS_MASK;
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
        return causePos(stops[index]);
    }

    /**
     * Reads the cause position from an enocoded safepoint.
     *
     * @see #causePosAt(int)
     * @param stop an encoded safepoints
     */
    public static int causePos(int stop) {
        int pos = stop & POS_MASK;
        if (CAUSE_AT_POS_MINUS_0.isSet(stop)) {
            return pos;
        }
        if (CAUSE_AT_POS_MINUS_2.isSet(stop)) {
            return pos - 2;
        }
        if (CAUSE_AT_POS_MINUS_3.isSet(stop)) {
            return pos - 3;
        }
        assert CAUSE_AT_POS_MINUS_5.isSet(stop);
        return pos - 5;
    }

    public int stopAt(int index) {
        return stops[index];
    }

    /**
     * Gets the length of the stop positions array wrapped by this object.
     */
    public int length() {
        return stops.length;
    }

    /**
     * Determines if a given attribute value is set for a given stop.
     *
     * @param a the attribute to test
     * @param index the index denoting the stop to query
     */
    public boolean isSetAt(Attr a, int index) {
        return a.isSet(stops[index]);
    }

    public int indexOf(int pos) {
        for (int i = 0; i != stops.length; i++) {
            if ((stops[i] & POS_MASK) == pos) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first direct call stop at or after a specified start index.
     * If no such stop exists then -1 is returned.
     * <p>
     * To iterate over the direct calls, use the following loop:
     *
     * <pre>
     * for (int i = stops.nextDirectCall(0); i &gt;= 0; i = stops.nextDirectCall(i + 1)) {
     *     // operate on stop i here
     * }
     * </pre>
     *
     * @param i the index to start searching from (inclusive)
     * @return the index of the first direct call stop between {@code [i .. length())} or -1 if there is no direct call stop in this range
     * @throws IndexOutOfBoundsException if the specified index is negative.
     */
    public int nextDirectCall(int i) {
        while (i < stops.length) {
            if (isSetAt(DIRECT_CALL, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int numberOfDirectCalls() {
        int n = 0;
        for (int i = 0; i < stops.length; i++) {
            if (isSetAt(DIRECT_CALL, i)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(stops.length * 15);
        sb.append("{");
        for (int i = 0; i < stops.length; ++i) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append(i).append(" -> ").append(posAt(i));
            int attrs = stops[i] & ATTRS_MASK;
            if (attrs != 0) {
                for (Attr a : Stops.ALL_ATTRS) {
                    if (a.isSet(attrs)) {
                        sb.append(" | ").append(a.name);
                    }
                }

            }
        }
        return sb.append("}").toString();

    }

    /**
     * Gets the stop position associated with a call.
     *
     * @param call a call site
     */
    public static int stopPosForCall(CiTargetMethod.Call call) {
        if (platform().isa == ISA.AMD64) {
            return call.pcOffset + call.size;
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Gets the stop position associated with a call.
     *
     * @param callPos position of the call instruction
     * @param callSize size of the call instruction
     */
    public static int stopPosForCall(int callPos, int callSize) {
        if (platform().isa == ISA.AMD64) {
            return callPos + callSize;
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Encodes a single stop.
     *
     * @param stopPos
     * @param causePos
     * @param a
     * @return
     */
    public static int make(int stopPos, int causePos, Attr a) {
        return make(stopPos, causePos, a.valueMask);
    }

    public static int make(int stopPos, int causePos, Attr a1, Attr a2) {
        assert (a1.mask & a2.mask) == 0;
        return make(stopPos, causePos, a1.valueMask | a2.valueMask);
    }

    public static int make(int stopPos, int causePos, int attrs) {
        assert causePos <= stopPos : "cause position must be before stop position";
        assert (stopPos & POS_MASK) == stopPos;
        assert (attrs & ATTRS_MASK) == attrs;
        int causeOffset = stopPos - causePos;
        if (causeOffset == 0) {
            attrs |= CAUSE_AT_POS_MINUS_0.valueMask;
        } else if (causeOffset == 2) {
            attrs |= CAUSE_AT_POS_MINUS_2.valueMask;
        } else if (causeOffset == 3) {
            attrs |= CAUSE_AT_POS_MINUS_3.valueMask;
        } else {
            assert causeOffset == 5;
            attrs |= CAUSE_AT_POS_MINUS_5.valueMask;
        }
        return stopPos | attrs;
    }
}
