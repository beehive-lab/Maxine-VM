/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.alloc;


/**
 * Represents a range of integers from a start (inclusive) to an end (exclusive.
 *
 * @author Thomas Wuerthinger
 */
public final class Range {

    public static final Range EndMarker = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE, null);

    /**
     * The start of the range, inclusive.
     */
    public int from;

    /**
     * The end of the range, exclusive.
     */
    public int to;

    /**
     * A link to allow the range to be put into a singly linked list.
     */
    public Range next;

    boolean intersects(Range r) {
        return intersectsAt(r) != -1;
    }


    /**
     * Creates a new range.
     *
     * @param from the start of the range, inclusive
     * @param to the end of the range, exclusive
     * @param next link to the next range in a linked list
     */
    Range(int from, int to, Range next) {
        this.from = from;
        this.to = to;
        this.next = next;
    }

    int intersectsAt(Range r2) {
        Range r1 = this;

        assert r2 != null : "null ranges not allowed";
        assert r1 != EndMarker && r2 != EndMarker : "empty ranges not allowed";

        do {
            if (r1.from < r2.from) {
                if (r1.to <= r2.from) {
                    r1 = r1.next;
                    if (r1 == EndMarker) {
                        return -1;
                    }
                } else {
                    return r2.from;
                }
            } else {
                if (r2.from < r1.from) {
                    if (r2.to <= r1.from) {
                        r2 = r2.next;
                        if (r2 == EndMarker) {
                            return -1;
                        }
                    } else {
                        return r1.from;
                    }
                } else { // r1.from() == r2.from()
                    if (r1.from == r1.to) {
                        r1 = r1.next;
                        if (r1 == EndMarker) {
                            return -1;
                        }
                    } else {
                        if (r2.from == r2.to) {
                            r2 = r2.next;
                            if (r2 == EndMarker) {
                                return -1;
                            }
                        } else {
                            return r1.from;
                        }
                    }
                }
            }
        } while (true);
    }

    @Override
    public String toString() {
        return "[" + from + ", " + to + "]";
    }
}
