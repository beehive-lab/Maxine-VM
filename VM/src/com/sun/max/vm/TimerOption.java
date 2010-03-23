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

package com.sun.max.vm;

import com.sun.max.annotate.*;
import com.sun.max.util.timer.*;

/**
 * A {@linkplain VMBooleanXXOption boolean VM} option that enables a timer.
 *
 * @author Doug Simon
 */
public class TimerOption extends VMBooleanXXOption {

    protected final TimerMetric timerMetric;
    protected final String label;

    /**
     * Creates an option for timing some action.
     *
     * @param prefix the prefix by which the option is activated (e.g. "-XX:-TimeCompilation")
     * @param label a description of action timed by this option (e.g. "Compilation")
     * @param help describes the option's semantics
     * @param timer the timer to use. This should be a multi-thread safe timer (e.g. {@link MultiThreadTimer}) if it
     *            may be used by more than one thread.
     */
    @HOSTED_ONLY
    public TimerOption(String prefix, String label, String help, Timer timer) {
        this(prefix, label, help, new TimerMetric(timer));
    }

    /**
     * Creates an option for timing some action.
     *
     * @param prefix the prefix by which the option is activated (e.g. "-XX:-TimeCompilation")
     * @param label a description of action timed by this option (e.g. "Compilation")
     * @param help describes the option's semantics
     */
    @HOSTED_ONLY
    public TimerOption(String prefix, String label, String help, TimerMetric timerMetric) {
        super(prefix, help);
        this.timerMetric = timerMetric;
        this.label = label;
    }

    /**
     * If this option is {@linkplain #isPresent() enabled}, then the timer is started.
     */
    public void start() {
        timerMetric.start();
    }

    /**
     * If this option is {@linkplain #isPresent() enabled}, then the timer is stopped.
     */
    public long stop() {
        timerMetric.stop();
        return timerMetric.getLastElapsedTime();
    }

    @Override
    protected void beforeExit() {
        if (isPresent()) {
            Log.print("    ");
            Log.print(label);
            Log.print(':');
            int column = 5 + label.length();
            for (; column < 22; column++) {
                Log.print(' ');
            }
            Log.print("Elapsed=");
            Log.print(timerMetric.getElapsedTime());
            Log.print(' ');
            Log.print(TimerUtil.getHzSuffix(timerMetric.getClock()));
            if (timerMetric.getNestedTime() != 0) {
                Log.print(" [Nested=");
                Log.print(timerMetric.getNestedTime());
                Log.print(' ');
                Log.print(TimerUtil.getHzSuffix(timerMetric.getClock()));
                Log.print(']');
            }
            Log.println();
        }
    }
}
