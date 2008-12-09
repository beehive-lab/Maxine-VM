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
package com.sun.max.program;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.profile.*;
import com.sun.max.program.option.*;

/**
 * Tracing output for debugging purposes. No performance impact when disabled. Some performance impact when active, even
 * without output. Possibly significant performance impact when producing a lot of output.
 *
 * Should not by itself cause allocation/GC during output.
 *
 * @author Bernd Mathiske
 */
public final class Trace {

    private static Metrics.Counter _traceCalls = GlobalMetrics.newCounter("Trace.calls");
    private static Metrics.Counter _tracePrints = GlobalMetrics.newCounter("Trace.prints");

    private Trace() {
        // do nothing.
    }

    private static PrintStream _stream;

    public static PrintStream stream() {
        return _stream;
    }

    public static void setStream(PrintStream stream) {
        _stream = stream;
    }

    private static final boolean _showThread;

    static {
        _showThread = System.getProperty("max.trace.showThread") != null;
        final String traceFileName = System.getProperty("max.trace.file");
        _stream = System.out;
        if (traceFileName != null) {
            final File traceFile = new File(traceFileName);
            try {
                final OutputStream fileStream = new BufferedOutputStream(new FileOutputStream(traceFile));
                if (System.getProperty("max.trace.noconsole") != null) {
                    _stream = new PrintStream(fileStream);
                } else {
                    _stream = new PrintStream(new MultiOutputStream(fileStream, System.out));
                }
            } catch (IOException ioException) {
                System.err.println("Could not open file for trace output: " + traceFile.getAbsolutePath());
            }
        }
    }

    /**
     * Static master switch. Set by source code editing only (for now).
     *
     * Set '_enabled' to 'true' to enable tracing output, according to your other trace-related settings.
     *
     * Set '_enabled' to 'false' to prevent any tracing. All tracing routines will thus become dead code. The optimizing
     * compiler should then be able to eliminate the runtime overhead.
     */
    private static final boolean _ENABLED = true;

    public static void addTo(OptionSet options) {
        options.addOption(new Option<Integer>("trace", 0, OptionTypes.INT_TYPE, "Sets tracing level.") {
            @Override
            public void setValue(Integer value) {
                super.setValue(value);
                _level = value;
            }
        });
    }

    /**
     * Dynamically sets tracing level, causing trace commands at this or lower levels to produce output.
     */
    public static void on(int level) {
        assert level >= 0;
        _level = level;
    }

    /**
     * Dynamically turns tracing on for all levels.
     */
    public static void on() {
        on(Integer.MAX_VALUE);
    }

    private static transient long _count;

    @INSPECTED
    private static transient long _threshold;

    @INSPECTED
    private static transient int _level;

    /**
     * Dynamically sets the current tracing level to the greater of the current level or the specified new level.
     */
    public static void atLeast(int level) {
        if (level > _level) {
            _level = level;
        }
    }

    /**
     * Dynamically turns tracing off by setting current level to zero.
     */
    public static void off() {
        _level = 0;
    }

    /**
     * @return current tracing level, which must equal or exceed the level specified by trace commands for output to be produced.
     */
    public static int level() {
        return _level;
    }

    /**
     * Does the current tracing level equal or exceed the specified level.
     */
    public static boolean hasLevel(int requiredLevel) {
        _count++;
        return _level >= requiredLevel && _count >= _threshold;
    }

    private static final int MAX_INDENTATION = 10;
    private static int _indentation;

    private static void printInt(int n) {
        if (n < 10) {
            _stream.write(((char) n) + '0');
        } else {
            final int m = n / 10;
            printInt(m);
            printInt(n - (m * 10));
        }
    }

    /**
     * This should not cause allocation/GC.
     */
    private static void printPrefix(int requiredLevel) {
        if (_showThread) {
            _stream.print(Thread.currentThread().getName() + " <Trace");
        } else {
            _stream.print("<Trace ");
        }
        printInt(requiredLevel);
        _stream.print("> ");
        for (int i = 0; i < _indentation; i++) {
            _stream.print(" ");
        }
    }

    /**
     * Prints a newline on trace output if tracing is globally enabled and current tracing level is at least the level required.
     */
    public static void line(int requiredLevel) {
        if (_ENABLED) {
            _traceCalls.increment();
            if (hasLevel(requiredLevel)) {
                _tracePrints.increment();
                _stream.println();
                _stream.flush();
            }
        }
    }

    /**
     * Prints a line of trace output if tracing is globally enabled and if current tracing level is at least the level required.
     */
    public static void line(int requiredLevel, Object message) {
        if (_ENABLED) {
            _traceCalls.increment();
            if (hasLevel(requiredLevel)) {
                _tracePrints.increment();
                printPrefix(requiredLevel);
                _stream.println(message);
                _stream.flush();
            }
        }
    }

    /**
     * Prints a "BEGIN" line of trace output if tracing is globally enabled and if current tracing level is at least the level required; increases indentation.
     */
    public static void begin(int requiredLevel, Object message) {
        if (_ENABLED) {
            _traceCalls.increment();
            if (hasLevel(requiredLevel)) {
                _tracePrints.increment();
                printPrefix(requiredLevel);
                _stream.print("BEGIN: ");
                _stream.println(message);
                _stream.flush();
                _indentation++;
            }
        }
    }

    /**
     * Prints an "END" line of trace output if tracing is globally enabled and if current tracing level is at least the level required; decreases indentation.
     */
    public static void end(int requiredLevel, Object message) {
        end(requiredLevel, message, 0);
    }

    /**
     * Prints an "END" line of trace output if tracing is globally enabled and if current tracing level is at least the level required; decreases indentation;
     * appends a timing message, expressed in milliseconds, if a non-zero starting time is supplied.
     * @param requiredLevel
     * @param message
     * @param startTimeMillis a starting time, output from {@link System#currentTimeMillis()}; no timing message appears if zero.
     */
    public static void end(int requiredLevel, Object message, long startTimeMillis) {
        if (_ENABLED) {
            _traceCalls.increment();
            if (hasLevel(requiredLevel)) {
                final long endTimeMillis = System.currentTimeMillis();
                _tracePrints.increment();
                _indentation--;
                // It's quite possible for indentation to go negative in a multithreaded environment
                //assert _indentation >= 0;
                printPrefix(requiredLevel);
                _stream.print("END:   ");
                if (startTimeMillis > 0) {
                    _stream.print(message);
                    _stream.print("  (");
                    _stream.print(endTimeMillis - startTimeMillis);
                    _stream.println("ms)");
                } else {
                    _stream.println(message);
                    _stream.flush();
                }
            }
        }
    }
}
