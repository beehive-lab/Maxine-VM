package com.sun.c1x;

import com.sun.c1x.debug.*;

/**
 * @author Christian Wimmer
 *
 */
public enum C1XTimers {
    HIR_CREATE("Create HIR"),
    HIR_OPTIMIZE("Optimize HIR"),
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
