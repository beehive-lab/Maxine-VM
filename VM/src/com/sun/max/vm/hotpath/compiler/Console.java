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
package com.sun.max.vm.hotpath.compiler;

import java.io.*;

import com.sun.max.program.option.*;
import com.sun.max.vm.debug.*;

public class Console {
    public static Option<Boolean> _prettifyOutput = new Option<Boolean>("PrettifyOutput", Boolean.FALSE, OptionTypes.BOOLEAN_TYPE, "Enables colored console output.");

    private static boolean prettifyOutput() {
        return _prettifyOutput.getValue() == Boolean.TRUE;
    }

    public static String [] _colorCodes = new String[Color.values().length];
    public static String [] _backgroundColorCodes = new String[Color.values().length];

    public static enum Color {
        BLACK,
        GRAY,
        WHITE,
        RED,
        LIGHTRED,
        GREEN,
        LIGHTGREEN,
        YELLOW,
        LIGHTYELLOW,
        BLUE,
        LIGHTBLUE,
        MAGENTA,
        LIGHTMAGENTA,
        TEAL,
        LIGHTTEAL;

        public Color get(boolean opposite) {
            if (opposite) {
                switch (this) {
                    case BLACK:
                    case GRAY:
                        return WHITE;
                    case WHITE:
                        return BLACK;
                    case RED:
                        return LIGHTRED;
                    case LIGHTRED:
                        return RED;
                    case YELLOW:
                        return LIGHTYELLOW;
                    case LIGHTYELLOW:
                        return YELLOW;
                    case BLUE:
                        return LIGHTBLUE;
                    case LIGHTBLUE:
                        return BLUE;
                    case GREEN:
                        return LIGHTGREEN;
                    case LIGHTGREEN:
                        return GREEN;
                    case MAGENTA:
                        return LIGHTMAGENTA;
                    case LIGHTMAGENTA:
                        return MAGENTA;
                    case TEAL:
                        return LIGHTTEAL;
                    case LIGHTTEAL:
                        return TEAL;
                }
            }
            return this;
        }

        public static String color(Color color, String s) {
            if (prettifyOutput()) {
                return (char) 27 + _colorCodes[color.ordinal()] + s + (char) 27 + "[0m";
            }
            return s;
        }
    }

    static {
        _colorCodes[Color.BLACK.ordinal()] = "[30m";
        _colorCodes[Color.GRAY.ordinal()] = "[1;30m";
        _colorCodes[Color.WHITE.ordinal()] = "[37m";
        _colorCodes[Color.RED.ordinal()] = "[31m";
        _colorCodes[Color.LIGHTRED.ordinal()] = "[1;31m";
        _colorCodes[Color.GREEN.ordinal()] = "[32m";
        _colorCodes[Color.LIGHTGREEN.ordinal()] = "[1;32m";
        _colorCodes[Color.YELLOW.ordinal()] = "[33m";
        _colorCodes[Color.LIGHTYELLOW.ordinal()] = "[1;33m";
        _colorCodes[Color.BLUE.ordinal()] = "[34m";
        _colorCodes[Color.LIGHTBLUE.ordinal()] = "[1;34m";
        _colorCodes[Color.MAGENTA.ordinal()] = "[35m";
        _colorCodes[Color.LIGHTMAGENTA.ordinal()] = "[1;35m";
        _colorCodes[Color.TEAL.ordinal()] = "[36m";
        _colorCodes[Color.LIGHTTEAL.ordinal()] = "[1;36m";

        _backgroundColorCodes[Color.BLACK.ordinal()] = "[40m";
        _backgroundColorCodes[Color.RED.ordinal()] = "[41m";
        _backgroundColorCodes[Color.GREEN.ordinal()] = "[42m";
        _backgroundColorCodes[Color.YELLOW.ordinal()] = "[43m";
        _backgroundColorCodes[Color.BLUE.ordinal()] = "[44m";
        _backgroundColorCodes[Color.MAGENTA.ordinal()] = "[45m";
        _backgroundColorCodes[Color.TEAL.ordinal()] = "[46m";
    }

    public static class ColoredPrintStream extends PrintStream implements ColoredConsole {

        public ColoredPrintStream(OutputStream outputStream) {
            super(outputStream);
        }

        public void color(Color background, Color foreground) {
            if (prettifyOutput()) {
                if (background != null) {
                    print((char) 27 + _backgroundColorCodes[background.ordinal()]);
                }
                if (foreground != null) {
                    print((char) 27 + _colorCodes[foreground.ordinal()]);
                }
            }
        }

        public void color(Color color) {
            if (prettifyOutput()) {
                print((char) 27 + _colorCodes[color.ordinal()]);
            }
        }

        public void clearColor() {
            if (prettifyOutput()) {
                print((char) 27 + "[0m");
            }
        }

        public void printf(Color color, String format, Object ... args) {
            color(color);
            printf(format, args);
            clearColor();
        }

        public void println(Color color, String s) {
            color(color);
            println(s);
            clearColor();
        }

        public void print(Color color, String s) {
            color(color);
            print(s);
            clearColor();
        }
    }

    public static class ColoredDebugOutput implements ColoredConsole {

        public void color(Color background, Color foreground) {
            if (prettifyOutput()) {
                if (background != null) {
                    Debug.print((char) 27 + _backgroundColorCodes[background.ordinal()]);
                }
                if (foreground != null) {
                    Debug.print((char) 27 + _colorCodes[foreground.ordinal()]);
                }
            }
        }

        public void color(Color color) {
            if (prettifyOutput()) {
                Debug.print((char) 27 + _colorCodes[color.ordinal()]);
            }
        }

        public void clearColor() {
            if (prettifyOutput()) {
                Debug.print((char) 27 + "[0m");
            }
        }

        public void println(Color color, String s) {
            color(color);
            Debug.println(s);
            clearColor();
        }

        public void println(String s) {
            Debug.println(s);
        }

        public void print(Color color, String s) {
            color(color);
            Debug.print(s);
            clearColor();
        }

        public void print(String s) {
            Debug.print(s);
        }
    }

    public static ColoredConsole _err;
    public static ColoredConsole _out;
    public static ColoredConsole _debug;

    static {
        _err = new ColoredPrintStream(System.err);
        _out = new ColoredPrintStream(System.out);
        _debug = _out;
    }

    public static ColoredConsole err() {
        return _err;
    }

    public static ColoredConsole out() {
        return _out;
    }

    public static ColoredConsole debug() {
        return _debug;
    }

    public static void println(Color color, String s) {
        _debug.println(color, s);
    }

    public static void println(String s) {
        _debug.println(s);
    }

    public static void print(Color color, String s) {
        _debug.print(color, s);
    }

    public static void print(String s) {
        _debug.print(s);
    }

    public static void printf(Color color, String s, Object ... args) {
        _debug.print(color, String.format(s, args));
    }

    public static void printf(String s, Object ... args) {
        _debug.print(String.format(s, args));
    }

    public static void println() {
        println("");
    }

    public static void printDivider(char divider, int length) {
        println(createDivider(null, divider, 0, length));
    }

    public static void printDivider(char divider) {
        println(createDivider(null, divider, 3, 128));
    }

    public static void printThinDivider() {
        println(createDivider(null, '-', 3, 128));
    }

    public static void printThinDivider(String title) {
        println(createDivider(title, '-', 3, 128));
    }

    public static void printDivider(String title) {
        println(createDivider(title, '=', 3, 128));
    }

    public static void printDivider(String title, char divider, int prefixLength) {
        println(createDivider(title, divider, prefixLength, 128));
    }

    public static String createDivider(String title, char divider, int prefixLength, int dividerLength) {
        // Create a string formatted as: "-- name --------"
        final String name = title != null ? " " + title + " " : "";
        final char [] buffer = new char[dividerLength];
        for (int i = 0; i < dividerLength; i++) {
            if (i >= prefixLength && i < prefixLength + name.length()) {
                buffer[i] = name.charAt(i - prefixLength);
            } else {
                buffer[i] = divider;
            }
        }
        return new String(buffer);
    }

    public static void indent(int depth) {

    }
}
