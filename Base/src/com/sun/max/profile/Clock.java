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
/*VCSID=ce49775b-3205-49c7-9788-1aed7fdc666e*/
package com.sun.max.profile;

/**
 * The {@code Clock} class represents a clock source that has a continuously increasing tick
 * count. A clock can be used to produce a relative timing between events by comparing the
 * number of ticks between events. This can be used to implement time-based profiling
 * by using a clock based on the system time.
 *
 * @author Ben L. Titzer
 */
public abstract class Clock {

    public abstract long getTicks();
    public abstract long getHZ();

    public static final Clock SYSTEM_NANOSECONDS = new SystemNS();
    public static final Clock SYSTEM_MILLISECONDS = new SystemMS();

    private static class SystemMS extends Clock {
        @Override
        public long getTicks() {
            return System.currentTimeMillis();
        }
        @Override
        public long getHZ() {
            return 1000;
        }
    }

    private static class SystemNS extends Clock {
        @Override
        public long getTicks() {
            return System.nanoTime();
        }
        @Override
        public long getHZ() {
            return 1000000000;
        }
    }

    public static int[] sampleDeltas(Clock clock, int numberOfSamples) {
        final int[] result = new int[numberOfSamples];
        long sample = clock.getTicks();
        for (int i = 0; i < numberOfSamples; i++) {
            final long newSample = clock.getTicks();
            result[i] = (int) (newSample - sample);
            sample = newSample;
        }
        return result;
    }

    public static void sample5(Clock clock, long[] samples) {
        final long sample0 = clock.getTicks();
        final long sample1 = clock.getTicks();
        final long sample2 = clock.getTicks();
        final long sample3 = clock.getTicks();
        final long sample4 = clock.getTicks();
        samples[0] = sample0;
        samples[1] = sample1;
        samples[2] = sample2;
        samples[3] = sample3;
        samples[4] = sample4;
    }
}
