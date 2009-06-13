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

import com.sun.c1x.ir.*;

/**
 * A utility for printing compiler debug and informational output to an output stream.
 *
 * A {@link LogStream} instance maintains an internal buffer that is flushed to the underlying
 * output stream every time one of the {@code println} methods is invoked, or a newline character
 * ({@code '\n'}) is written.
 *
 * All of the {@code print} and {@code println} methods return the {code LogStream} instance
 * on which they were invoked. This allows chaining of these calls to mitigate use of String
 * concatenation by the caller.
 *
 * A {@code LogStream} maintains a current {@linkplain #indentation() indentation} level.
 * Each line of output written to this stream has {@code n} spaces prefixed to it where
 * {@code n} is the value that would be returned by {@link #indentation()} when the first
 * character of a new line is written.
 *
 * A {@code LogStream} maintains a current {@linkplain #position() position} for the current
 * line being written. This position can be advanced to a specified position by
 * {@linkplain #fillTo(int, char) filling} this stream with a given character.

 * @author Doug Simon
 */
public class LogStream {

    private final StringBuilder _lineBuffer = new StringBuilder(100);
    private final PrintStream _ps;
    private int _indentation;
    private boolean _indentationDisabled;

    /**
     * The system dependent line separator.
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Creates a new log stream.
     *
     * @param os the underlying output stream to which prints are sent
     */
    public LogStream(OutputStream os) {
        _ps = os instanceof PrintStream ? (PrintStream) os : new PrintStream(os);
    }

    /**
     * Prepends spaces to the current output line until its write position is equal to the
     * current {@linkplain #indentation()} level.
     */
    private void indent() {
        if (!_indentationDisabled && _indentation != 0) {
            while (_lineBuffer.length() < _indentation) {
                _lineBuffer.append(' ');
            }
        }
    }

    private LogStream flushLine(boolean withNewline) {
        if (withNewline) {
            _lineBuffer.append(LINE_SEPARATOR);
        } else {
            assert _lineBuffer.indexOf(LINE_SEPARATOR, _lineBuffer.length() - LINE_SEPARATOR.length()) != -1;
        }
        _ps.print(_lineBuffer.toString());
        _ps.flush();
        _lineBuffer.setLength(0);
        return this;
    }

    /**
     * Flushes the stream. This is done by terminating the current line if it is not at position 0
     * and then flushing the underlying output stream.
     */
    public void flush() {
        if (_lineBuffer.length() != 0) {
            flushLine(true);
        }
        _ps.flush();
    }

    /**
     * Gets the current column position of this log stream.
     *
     * @return the current column position of this log stream
     */
    public int position() {
        return _lineBuffer.length();
    }

    /**
     * Gets the current indentation level for this log stream.
     * Each line of output written to this stream has {@code n} spaces prefixed to it where
     * {@code n} is the value returned by this method when the first character of a new line
     * is written.
     *
     * @return the current indentation level for this log stream.
     */
    public int indentation() {
        return _indentation;
    }

    /**
     * Adjusts the current indentation level of this log stream.
     * @param delta
     */
    public void adjustIndentation(int delta) {
        if (delta < 0) {
            _indentation = Math.max(0, _indentation + delta);
        } else {
            _indentation += delta;
        }
    }

    public void disableIndentation() {
        _indentationDisabled = true;
    }

    public void enableIndentation() {
        _indentationDisabled = false;
    }

    /**
     * Advances this stream's {@linkplain #position() position} to a given position by
     * repeatedly appending a given character as necessary.
     *
     * @param position the position to which this stream's position will be advanced
     * @param filler the character used to pad the stream
     */
    public LogStream fillTo(int position, char filler) {
        indent();
        while (_lineBuffer.length() < position) {
            _lineBuffer.append(filler);
        }
        return this;
    }

    /**
     * Writes a boolean value to this stream as {@code "true"} or {@code "false"}.
     *
     * @param b the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(boolean b) {
        indent();
        _lineBuffer.append(b);
        return this;
    }

    /**
     * Writes a boolean value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param b the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(boolean b) {
        indent();
        _lineBuffer.append(b);
        return flushLine(true);
    }

    /**
     * Writes a character value to this stream.
     *
     * @param c the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(char c) {
        indent();
        _lineBuffer.append(c);
        if (c == '\n') {
            if (_lineBuffer.indexOf(LINE_SEPARATOR, _lineBuffer.length() - LINE_SEPARATOR.length()) != -1) {
                flushLine(false);
            }
        }
        return this;
    }

    /**
     * Writes a character value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param c the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(char c) {
        indent();
        _lineBuffer.append(c);
        flushLine(true);
        return this;
    }

    /**
     * Prints an int value.
     *
     * @param i the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(int i) {
        indent();
        _lineBuffer.append(i);
        return this;
    }

    /**
     * Writes an int value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param i the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(int i) {
        indent();
        _lineBuffer.append(i);
        return flushLine(true);
    }

    /**
     * Writes a float value to this stream.
     *
     * @param f the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(float f) {
        indent();
        _lineBuffer.append(f);
        return this;
    }

    /**
     * Writes a float value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param f the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(float f) {
        indent();
        _lineBuffer.append(f);
        return flushLine(true);
    }

    /**
     * Writes a long value to this stream.
     *
     * @param l the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(long l) {
        indent();
        _lineBuffer.append(l);
        return this;
    }

    /**
     * Writes a long value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param l the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(long l) {
        indent();
        _lineBuffer.append(l);
        return flushLine(true);
    }

    /**
     * Writes a double value to this stream.
     *
     * @param d the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(double d) {
        indent();
        _lineBuffer.append(d);
        return this;
    }

    /**
     * Writes a double value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param d the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(double d) {
        indent();
        _lineBuffer.append(d);
        return flushLine(true);
    }

    /**
     * Writes a {@code String} value to this stream. This method ensures that the {@linkplain #position() position}
     * of this stream is updated correctly with respect to any {@linkplain #LINE_SEPARATOR line separators}
     * present in {@code s}.
     *
     * @param s the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream print(String s) {
        if (s == null) {
            indent();
            _lineBuffer.append(s);
            return this;
        }

        int index = 0;
        int next = s.indexOf(LINE_SEPARATOR, index);
        while (index < s.length()) {
            indent();
            if (next > index) {
                _lineBuffer.append(s.substring(index, next));
                flushLine(true);
                index = next + LINE_SEPARATOR.length();
                next = s.indexOf(LINE_SEPARATOR, index);
            } else {
                _lineBuffer.append(s.substring(index));
                break;
            }
        }
        return this;
    }

    /**
     * Writes a {@code String} value to this stream followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param s the value to be printed
     * @return this {@link LogStream} instance
     */
    public LogStream println(String s) {
        print(s);
        flushLine(true);
        return this;
    }

    /**
     * Writes a formatted string to this stream.
     *
     * @param format a format string as described in {@link String#format(String, Object...)}
     * @param args the arguments to be formatted
     * @return this {@link LogStream} instance
     */
    public LogStream printf(String format, Object... args) {
        print(String.format(format, args));
        return this;
    }

    /**
     * Writes an instruction formatted as a {@linkplain Instruction#valueString(Instruction) value} to this stream.
     *
     * @param value the instruction to print
     * @return this {@code LogStream} instance
     */
    public LogStream print(Instruction value) {
        indent();
        if (value == null) {
            _lineBuffer.append("null");
        } else {
            _lineBuffer.append(value.type().tchar()).append(value.id());
        }
        return this;
    }

    /**
     * Writes an instruction formatted as a {@linkplain Instruction#valueString(Instruction) value} to this stream
     * followed by a {@linkplain #LINE_SEPARATOR line separator}.
     *
     * @param value the instruction to print
     * @return this {@code LogStream} instance
     */
    public LogStream println(Instruction value) {
        print(value);
        flushLine(true);
        return this;
    }

    /**
     * Writes a {@linkplain #LINE_SEPARATOR line separator} to this stream.
     *
     * @return this {@code LogStream} instance
     */
    public LogStream println() {
        indent();
        flushLine(true);
        return this;
    }
}
