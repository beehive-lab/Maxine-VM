/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.util;

import com.sun.max.program.*;


/**
 * A wrapper for the standard trace package that keeps elapsed time between begin and end.
 *
 * @see Trace
 * @author Michael Van De Vanter
 */
public final class TimedTrace {

    private final int defaultTraceValue;
    private final String description;
    private long startTimeMillis;

    /**
     * Creates a tracer that keeps track of elapsed time between begin and end calls,
     * assuming that they are called in strictly alternating order, and prints a trace
     * message at both begin and end.
     * <br>
     * Each line of output appears as follows:
     * <br>
     * &lt;Trace level&gt; BEGIN/END &lt;description&gt;  &lt;messageSuffix (optional)&gt; (&lt;time&gt; ms)
     * <br>
     * The optional {@code messageSuffix} provides an opportunity to display additional summary statistics.
     *
     * @param defaultTraceValue the trace level to be used with calls to the {@link Trace} class,
     * unless overridden by a specific value in the calls.
     * @param description a string to identify what's being traced, appears on each line.
     * @see Trace
     * @see Trace#level()
     */
    public TimedTrace(int defaultTraceValue, String description) {
        this.defaultTraceValue = defaultTraceValue;
        this.description = description;
        this.startTimeMillis = System.currentTimeMillis();
    }

    /**
     * Produces a default trace message announcing the start of something and remembers
     * the start time.   The message is a combination of the default
     * message, followed by the string value of the argument, and it is presented
     * at the specified trace level.
     *
     * @param traceValue the trace level for the message
     * @param messageSuffix an object whose string value is appended to the message prefix
     */
    public void begin(int traceValue, Object messageSuffix) {
        startTimeMillis = System.currentTimeMillis();
        final String message = (messageSuffix == null) ? description :
            description + ", " + messageSuffix.toString();
        Trace.begin(traceValue, message);
    }

    /**
     * Produces a default trace message announcing the start of something and remembers
     * the start time.   The message is a combination of the default
     * message, followed by the string value of the argument, and it is presented
     * at the default trace level.
     *
     * @param messageSuffix an object whose string value is appended to the message prefix
     */
    public void begin(Object messageSuffix) {
        startTimeMillis = System.currentTimeMillis();
        final String message = (messageSuffix == null) ? description :
            description + ", " + messageSuffix.toString();
        Trace.begin(defaultTraceValue, message);
    }

    /**
     * Produces a default trace message announcing the start of something and remembers
     * the start time.   The message it is presented
     * at the specified trace level.
     *
     * @param traceValue the trace level for the message
     */
    public void begin(int traceValue) {
        startTimeMillis = System.currentTimeMillis();
        Trace.begin(traceValue, description);
    }

    /**
     * Produces a default trace message announcing the start of something and remembers
     * the start time.
     */
    public void begin() {
        startTimeMillis = System.currentTimeMillis();
        Trace.begin(defaultTraceValue, description);
    }

    /**
     * Produces a trace message announcing the completion of something along with
     * elapsed time.  The message is a combination of the default
     * message, followed by the string value of the argument, and it is presented
     * at the specified trace level.
     * <br>
     * Must always be preceded by a call to {@link #begin()}.
     *
     * @param the level for the trace.
     * @param messageSuffix an object whose string value is appended to the message prefix
     */
    public void end(int traceValue, Object messageSuffix) {
        assert startTimeMillis > -1;
        final String message = (messageSuffix == null) ? description :
            description + ", " + messageSuffix.toString();
        Trace.end(traceValue, message, startTimeMillis);
        startTimeMillis = -1;
    }

    /**
     * Produces a trace message announcing the completion of something along with
     * elapsed time.  The message is a combination of the default
     * message, followed by the string value of the argument.
     * <br>
     * Must always be preceded by a call to {@link #begin()}.
     *
     * @param messageSuffix an object whose string value is appended to the message prefix
     */
    public void end(Object messageSuffix) {
        end(defaultTraceValue, messageSuffix);
    }

    /**
     * Produces a default trace message announcing the completion of something along with
     * elapsed time.
     * <br>
     * Must always be preceded by a call to {@link #begin()}.
     *
     * @param traceValue the trace level for this call
     */
    public void end(int traceValue) {
        end(traceValue, null);
    }

    /**
     * Produces a default trace message announcing the completion of something along with
     * elapsed time, using the trace level specified at creation.
     * <br>
     * Must always be preceded by a call to {@link #begin()}.
     */
    public void end() {
        end(defaultTraceValue, null);
    }
}
