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
package com.sun.max.util;

/**
 * A {@code Range} denotes all the integer values between a {@linkplain #start() start} (inclusive) and
 * {@linkplain #end() end} (exclusive) value.
 *
 * @author Doug Simon
 */
public class Range {
    private final int start;
    private final int end;

    public Range(int start) {
        this(start, start + 1);
    }

    /**
     * Creates an object representing values in the range {@code [start .. end)}.
     *
     * @param start
     *                the lowest value in the range
     * @param end
     *                the value one greater than the highest value in the range
     */
    public Range(int start, int end) {
        assert end >= start;
        this.start = start;
        this.end = end;
        assert length() >= 0;
    }

    /**
     * Gets the lowest value in this range.
     * @return
     */
    public int start() {
        return start;
    }

    /**
     * Gets the number of values covered by this range.
     */
    public long length() {
        // This cast and the long return type prevents integer overflow
        return ((long) end) - start;
    }

    /**
     * Gets the value one greater than the highest value in this range.
     */
    public int end() {
        return end;
    }

    public boolean contains(long value) {
        return value >= start && value < end;
    }

    @Override
    public String toString() {
        return "[" + start() + '-' + (end() - 1) + ']';
    }
}
