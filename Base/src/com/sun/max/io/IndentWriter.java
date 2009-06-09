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
package com.sun.max.io;

import java.io.*;

import com.sun.max.program.*;

/**
 * A line oriented character writer that indents line output on the left.
 *
 * @author Bernd Mathiske
 */
public class IndentWriter {

    private final PrintWriter _writer;
    private int _lineCount;

    /**
     * Gets an IndentWriter that wraps the {@linkplain Trace#stream() trace stream}.
     * @return
     */
    public static IndentWriter traceStreamWriter() {
        return new IndentWriter(new OutputStreamWriter(Trace.stream()));
    }

    public IndentWriter(Writer writer) {
        _writer = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(writer);
    }

    public void close() {
        _writer.close();
    }

    public void flush() {
        _writer.flush();
    }

    private int _indentation = 4;

    public int indentation() {
        return _indentation;
    }

    public void setIndentation(int indentation) {
        _indentation = indentation;
    }

    private int _prefix;

    public void indent() {
        _prefix += _indentation;
    }

    public void outdent() {
        _prefix -= _indentation;
        assert _prefix >= 0;
    }

    private boolean _isCurrentLineIndented;

    private void writeIndentation() {
        if (!_isCurrentLineIndented) {
            for (int i = 0; i < _prefix; i++) {
                _writer.print(" ");
            }
            _isCurrentLineIndented = true;
        }
    }

    public void printWhite(int width) {
        for (int i = 0; i < width; i++) {
            _writer.print(" ");
        }
    }

    public void printFixedWidth(String s, int width) {
        assert width > 0 : "width must be positive";
        String text = s;
        if (text.length() + 1 > width) {
            if (width - 4 > 0) {
                text = s.substring(0, width - 4) + "...";
            } else {
                text = s.substring(0, width);
            }
        }
        _writer.print(text);
        printWhite(width - text.length());
    }

    public void print(String s) {
        writeIndentation();
        _writer.print(s);
    }

    public void println() {
        _writer.println();
        _isCurrentLineIndented = false;
        ++_lineCount;
    }

    public void println(String s) {
        writeIndentation();
        _writer.println(s);
        _isCurrentLineIndented = false;
        ++_lineCount;
    }

    public void printLines(InputStream inputStream) {
        printLines(new InputStreamReader(inputStream));
    }

    public void printLines(Reader reader) {
        final BufferedReader bufferedReader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                println(line);
            }
        } catch (IOException e) {
            ProgramWarning.message(e.toString());
        }
    }

    public int lineCount() {
        return _lineCount;
    }
}
