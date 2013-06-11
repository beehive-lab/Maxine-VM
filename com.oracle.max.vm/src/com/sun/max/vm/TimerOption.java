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

package com.sun.max.vm;

import com.sun.max.annotate.*;
import com.sun.max.util.timer.*;

/**
 * A {@linkplain VMBooleanOption boolean VM} option that enables a timer.
 */
public class TimerOption extends VMBooleanOption {

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
