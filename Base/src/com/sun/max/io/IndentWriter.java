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

    private final PrintWriter writer;
    private int lineCount;

    /**
     * Gets an IndentWriter that wraps the {@linkplain Trace#stream() trace stream}.
     * @return
     */
    public static IndentWriter traceStreamWriter() {
        return new IndentWriter(new OutputStreamWriter(Trace.stream()));
    }

    public IndentWriter(Writer writer) {
        this.writer = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(writer);
    }

    public void close() {
        writer.close();
    }

    public void flush() {
        writer.flush();
    }

    private int indentation = 4;

    public int indentation() {
        return indentation;
    }

    public void setIndentation(int indentation) {
        this.indentation = indentation;
    }

    private int prefix;

    public void indent() {
        prefix += indentation;
    }

    public void outdent() {
        prefix -= indentation;
        assert prefix >= 0;
    }

    private boolean isCurrentLineIndented;

    private void writeIndentation() {
        if (!isCurrentLineIndented) {
            for (int i = 0; i < prefix; i++) {
                writer.print(" ");
            }
            isCurrentLineIndented = true;
        }
    }

    public void printSpaces(int width) {
        for (int i = 0; i < width; i++) {
            writer.print(" ");
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
        writer.print(text);
        printSpaces(width - text.length());
    }

    public void print(String s) {
        writeIndentation();
        writer.print(s);
    }

    public void println() {
        writer.println();
        isCurrentLineIndented = false;
        ++lineCount;
    }

    public void println(String s) {
        writeIndentation();
        writer.println(s);
        isCurrentLineIndented = false;
        ++lineCount;
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
        return lineCount;
    }
}
