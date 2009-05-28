/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.util.timer;

import com.sun.max.profile.*;

/**
 * This class offers various utilities for composing and using timers.
 *
 * @author Ben L. Titzer
 */
public class TimerUtil {
    public static void time(Runnable runnable, Timer timer) {
        timer.start();
        runnable.run();
        timer.stop();
    }

    public static long timeElapsed(Runnable runnable, Timer timer) {
        timer.start();
        runnable.run();
        timer.stop();
        return timer.getLastElapsedTime();
    }

    public static long timeElapsed(Runnable runnable, Clock clock) {
        return timeElapsed(runnable, new SingleUseTimer(clock));
    }

    public static long getLastElapsedSeconds(Timer timer) {
        return (1 * timer.getLastElapsedTime()) / timer.getClock().getHZ();
    }

    public static long getLastElapsedMilliSeconds(Timer timer) {
        return (1000 * timer.getLastElapsedTime()) / timer.getClock().getHZ();
    }

    public static long getLastElapsedNanoSeconds(Timer timer) {
        return (1000000 * timer.getLastElapsedTime()) / timer.getClock().getHZ();
    }
}
