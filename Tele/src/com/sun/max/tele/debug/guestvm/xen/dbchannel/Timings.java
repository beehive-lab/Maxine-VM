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
package com.sun.max.tele.debug.guestvm.xen.dbchannel;

import java.util.*;
import com.sun.max.program.*;


public class Timings {
    public static int DEFAULT_CYCLE = 1000;
    private long[] times;
    private int index = 0;
    private long avg;
    private String name;
    private long start;
    private static boolean doTimings;
    private static final String TIMINGS_PROPERTY = "max.ins.channel.timings";

    static {
        final String p = System.getProperty(TIMINGS_PROPERTY);
        doTimings = p != null;
    }

    private static Map<String, Timings> map = new HashMap<String, Timings>();

    public Timings(String name) {
        this(name, DEFAULT_CYCLE);
    }

    public Timings(String name, int reportFrequency) {
        if (!doTimings) {
            return;
        }
        this.times = new long[reportFrequency];
        this.name = name;
        map.put(name, this);
    }

    public void add() {
        if (!doTimings) {
            return;
        }
        final long now = System.nanoTime();
        if (index >= times.length) {
            long total = 0;
            for (long t : times) {
                total += t;
            }
            avg = total / times.length;
            Trace.line(1, name + ": average: " + avg);
            index = 0;
        }
        times[index++] = now - start;
    }

    public long start() {
        if (!doTimings) {
            return 0;
        }
        start = System.nanoTime();
        return start;
    }
}
