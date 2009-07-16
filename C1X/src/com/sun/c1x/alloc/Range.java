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

import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class Range {

    private static Range end = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE, null); // sentinel (from == to ==
                                                                                      // maxJint)

    static Range end() {
        return end;
    }

    private int from; // from (inclusive)
    private int to; // to (exclusive)
    private Range next; // linear list of Ranges

    // used only by class Interval, so hide them
    boolean intersects(Range r) {
        return intersectsAt(r) != -1;
    }

    int from() {
        return from;
    }

    int to() {
        return to;
    }

    Range next() {
        return next;
    }

    void setFrom(int from) {
        this.from = from;
    }

    void setTo(int to) {
        this.to = to;
    }

    void setNext(Range next) {
        this.next = next;
    }

    // * Implementation of Range *

    Range(int from, int to, Range next) {

        this.from = from;
        this.to = to;
        this.next = next;
    }

    int intersectsAt(Range r2) {
        Range r1 = this;

        assert r2 != null : "null ranges not allowed";
        assert r1 != end && r2 != end : "empty ranges not allowed";

        do {
            if (r1.from() < r2.from()) {
                if (r1.to() <= r2.from()) {
                    r1 = r1.next();
                    if (r1 == end) {
                        return -1;
                    }
                } else {
                    return r2.from();
                }
            } else if (r2.from() < r1.from()) {
                if (r2.to() <= r1.from()) {
                    r2 = r2.next();
                    if (r2 == end) {
                        return -1;
                    }
                } else {
                    return r1.from();
                }
            } else { // r1.from() == r2.from()
                if (r1.from() == r1.to()) {
                    r1 = r1.next();
                    if (r1 == end) {
                        return -1;
                    }
                } else if (r2.from() == r2.to()) {
                    r2 = r2.next();
                    if (r2 == end) {
                        return -1;
                    }
                } else {
                    return r1.from();
                }
            }
        } while (true);
    }

    void print(LogStream out) {
        out.printf("[%d, %d[ ", from, to);
    }
}
