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
 * A VM option that wraps a timer.
 *
 * @author Doug Simon
 */
public class TimerOption extends VMOption {

    protected final TimerMetric _timerMetric;
    protected final String _label;

    @PROTOTYPE_ONLY
    public TimerOption(String label, String help, Timer timer) {
        this("-XX:Time:" + label, label, help, new TimerMetric(timer));
    }

    /**
     * Creates an option for timing some action.
     *
     * @param prefix the prefix by which the option is activated
     * @param help describes the option's semantics
     * @param timerMetric the timer to use. This should be a multi-thread safe timer (e.g. {@link MultiThreadTimer}) if it
     *            may be used by more than one thread.
     */
    @PROTOTYPE_ONLY
    public TimerOption(String prefix, String label, String help, TimerMetric timerMetric) {
        super(prefix, help);
        _timerMetric = timerMetric;
        _label = label;
    }

    /**
     * If this option is {@linkplain #isPresent() enabled}, then the timer is started.
     */
    public void start() {
        _timerMetric.start();
    }

    /**
     * If this option is {@linkplain #isPresent() enabled}, then the timer is stopped.
     */
    public long stop() {
        _timerMetric.stop();
        return _timerMetric.getLastElapsedTime();
    }

    @Override
    protected void beforeExit() {
        if (isPresent()) {
            Log.print("    ");
            Log.print(_label);
            Log.print(':');
            int column = 5 + _label.length();
            for (; column < 22; column++) {
                Log.print(' ');
            }
            Log.print("Elapsed=");
            Log.print(_timerMetric.getElapsedTime());
            Log.print(' ');
            Log.print(TimerUtil.getHzSuffix(_timerMetric.getClock()));
            if (_timerMetric.getNestedTime() != 0) {
                Log.print(" [Nested=");
                Log.print(_timerMetric.getNestedTime());
                Log.print(' ');
                Log.print(TimerUtil.getHzSuffix(_timerMetric.getClock()));
                Log.print(']');
            }
            Log.println();
        }
    }
}
