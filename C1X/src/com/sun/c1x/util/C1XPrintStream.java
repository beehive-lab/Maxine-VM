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
package com.sun.c1x.util;

import java.io.*;

/**
 * A print stream that maintains a {@linkplain #position() column} position and an
 * {@linkplain #indentation() indentation} position.
 *
 * @author Doug Simon
 */
public class C1XPrintStream {

    /**
     * The global print stream for writing to the VM's standard log stream.
     */
    private static C1XPrintStream _out = new C1XPrintStream(System.out);

    /**
     * Gets the print stream that writes to the VM's standard log stream.
     */
    public static C1XPrintStream out() {
        return _out;
    }

    /**
     * Sets the print stream returned by {@link #out()} to a given print stream.
     *
     * @param out the new value that will be returned by subsequent calls to {@link #out()}
     */
    public static void setOut(C1XPrintStream out) {
        _out = out;
    }

    private final PrintStream _ps;
    private int _indentation;
    private int _position;

    /**
     * Creates a new print stream.
     *
     * @param os the underlying output stream to which prints are sent
     */
    public C1XPrintStream(OutputStream os) {
        _ps = os instanceof PrintStream ? (PrintStream) os : new PrintStream(os);
    }

    /**
     * Prints a given string to this stream, advancing the tracked position by the
     * length of the string.
     *
     * @param s the string to print
     * @param mayContainLineSeparator specifies if {@code s} may contain a {@link #LINE_SEPARATOR}
     */
    private void print(String s, boolean mayContainLineSeparator) {
        if (s == null) {
            s = "null";
        }
        _ps.print(s);
        if (mayContainLineSeparator) {
            int index = s.lastIndexOf(LINE_SEPARATOR);
            if (index != -1) {
                _position = s.length() - index - LINE_SEPARATOR.length();
            } else {
                _position += s.length();
            }
        } else {
            assert s.indexOf(LINE_SEPARATOR) == -1;
            _position += s.length();
        }
    }

    /**
     * Advances this stream's {@linkplain #position() position} to a given position by
     * repeatedly appending a given character as necessary.
     *
     * @param position the position to which this stream's position will be advanced
     * @param filler the character used to pad the stream
     */
    public void fillTo(int position, char filler) {
        while (_position < position) {
            print(filler);
        }
    }

    public void print(boolean b) {
        print(b ? "true" : "false", false);
    }

    public void print(char c) {
        if (c == LINE_SEPARATOR.charAt(LINE_SEPARATOR.length() - 1)) {
            print(String.valueOf(c), true);
        } else {
            ++_position;
            _ps.print(c);
        }
    }

    public void print(double d) {
        print(String.valueOf(d), false);
    }

    public void print(float f) {
        print(String.valueOf(f), false);
    }

    public void print(int i) {
        print(String.valueOf(i), false);
    }

    /**
     * The system dependent line separator.
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public void printf(String format, Object... args) {
        print(String.format(format, args), true);
    }

    public void print(long l) {
        print(String.valueOf(l), false);
    }

    public void print(String s) {
        print(s, true);
    }

    public void println(String s) {
        _ps.println(s);
        _position = 0;
    }

    public void println(char c) {
        _ps.println(c);
        _position = 0;
    }

    public void println() {
        _ps.println();
        _position = 0;
    }

    public void close() {
        _ps.close();
    }

    public void flush() {
        _ps.flush();
    }

    /**
     * Gets the current column position of this print stream.
     *
     * @return the current column position of this print stream
     */
    public int position() {
        return _position;
    }

    /**
     * Gets the current indentation level for this print stream.
     *
     * @return the current indentation level for this print stream.
     */
    public int indentation() {
        return _indentation;
    }

    public void incIndent() {
        _indentation++;
    }

    public void decIndent() {
        _indentation--;
    }
}
