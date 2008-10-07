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
/*VCSID=da1b2f91-32b6-46e4-958e-dfd95b2f88ff*/
package com.sun.max.test;

import java.io.*;

/**
 * The <code>ProgressPrinter</code> class is useful for printing status information
 * to the console while running tests. This class supports three output modes;
 * <i>silent</i>, where only test failures are printed at the end, <i>quiet</i>,
 * where each test produces either an '.' or an 'X' for success or failure, respectively,
 * or <i>verbose</i>, where each test produces a line of output.
 *
 * @author Ben L. Titzer
 */
public class ProgressPrinter {

    private static final String CTRL_RED = "\u001b[0;31m";
    private static final String CTRL_GREEN = "\u001b[0;32m";
    private static final String CTRL_NORM = "\u001b[0;00m";

    public final int _total;
    private String _current;
    private boolean _color;
    private int _passed;
    private int _finished;
    private int _verbose;

    private final PrintStream _output;
    private final String[] _failedTests;
    private final String[] _failedMessages;

    public ProgressPrinter(PrintStream out, int total, int verbose, boolean color) {
        this._output = out;
        this._total = total;
        this._verbose = verbose;
        this._color = color;
        this._failedTests = new String[total];
        this._failedMessages = new String[total];
    }

    /**
     * Begin running the next item.
     * @param test the name of the item to begin running, which is remembered in case the test fails
     */
    public void begin(String test) {
        _current = test;
        if (_verbose == 2) {
            printTest(test, _finished);
            _output.print("...");
        }
    }

    /**
     * Finish the current test, indicating success.
     */
    public void pass() {
        _passed++;
        if (_verbose > 0) {
            output(CTRL_GREEN, '.', "ok");
        }
    }

    /**
     * Finish the current test, indicating failure with the specified error message.
     * @param message the message to associate with the specified test failure
     */
    public void fail(String message) {
        _failedTests[_finished] = _current;
        _failedMessages[_finished] = message;
        if (_verbose > 0) {
            output(CTRL_RED, 'X', "failed");
        }
        if (_verbose == 2) {
            this._output.print("\t-> ");
            this._output.println(message);
        }
    }

    /**
     * Sets the verbosity level of this progress printer.
     * @param verbose the new verbosity level of this printer
     */
    public void setVerbose(int verbose) {
        this._verbose = verbose;
    }

    /**
     * Sets the color output behavior of this progress printer.
     * @param color the color output of this printer
     */
    public void setColors(boolean color) {
        this._color = color;
    }

    private void printTest(String test, int i) {
        _output.print(i);
        _output.print(':');
        if (i < 100) {
            _output.print(' ');
        }
        if (i < 100) {
            _output.print(' ');
        }
        _output.print(' ');
        _output.print(test);
    }

    private void output(String ctrl, char ch, String str) {
        _finished++;
        if (_verbose == 1) {
            control(ctrl);
            _output.print(ch);
            control(CTRL_NORM);
            if (_finished == _total) {
                // just go to next line
                _output.println();
            } else if (_finished % 50 == 0) {
                _output.print(" ");
                _output.print(_finished);
                _output.print(" of ");
                _output.print(_total);
                _output.println();
            } else if (_finished % 10 == 0) {
                _output.print(' ');
            }
        } else if (_verbose == 2) {
            control(ctrl);
            _output.print(str);
            control(CTRL_NORM);
            _output.println();
        }
    }

    private void control(String ctrl) {
        if (_color) {
            _output.print(ctrl);
        }
    }

    /**
     * Print a report of the number of tests that passed, and print the messages from test failures
     * in the <i>quiet</i> mode.
     */
    public void report() {
        _output.print(_passed);
        _output.print(" of ");
        _output.print(_total);
        _output.println(" passed");
        if (_verbose < 2) {
            for (int i = 0; i < _total; i++) {
                if (_failedTests[i] != null) {
                    control(CTRL_RED);
                    printTest(_failedTests[i], i);
                    control(CTRL_NORM);
                    _output.print(": ");
                    _output.println(_failedMessages[i]);
                }
            }
        }
    }
}
