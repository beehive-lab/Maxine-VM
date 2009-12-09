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
package com.sun.c1x;

import com.sun.c1x.debug.*;

/**
 * This class contains timers that record the amount of time spent in various
 * parts of the compiler.
 *
 * @author Christian Wimmer
 */
public enum C1XTimers {
    HIR_CREATE("Create HIR"),
    HIR_OPTIMIZE("Optimize HIR"),
    NCE("Nullcheck elimination"),
    LIR_CREATE("Create LIR"),
    LIFETIME_ANALYSIS("Lifetime Analysis"),
    LINEAR_SCAN("Linear Scan"),
    RESOLUTION("Resolution"),
    DEBUG_INFO("Create Debug Info"),
    CODE_CREATE("Create Code");

    private final String name;
    private long start;
    private long total;

    private C1XTimers(String name) {
        this.name = name;
    }

    public void start() {
        start = System.nanoTime();
    }

    public void stop() {
        total += System.nanoTime() - start;
    }

    public static void reset() {
        for (C1XTimers t : values()) {
            t.total = 0;
        }
    }

    public static void print() {
        long total = 0;
        for (C1XTimers timer : C1XTimers.values()) {
            total += timer.total;
        }
        if (total == 0) {
            return;
        }

        TTY.println();
        for (C1XTimers timer : C1XTimers.values()) {
            TTY.println("%-20s: %7.4f s (%5.2f%%)", timer.name, timer.total / 1000000000.0, timer.total * 100.0 / total);
            timer.total = 0;
        }
        TTY.println();
    }
}
