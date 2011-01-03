/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.hotpath.compiler;

import java.io.*;

import com.sun.max.program.option.*;
import com.sun.max.vm.*;

public class Console {
    public static Option<Boolean> prettifyOutput = new Option<Boolean>("PrettifyOutput", Boolean.FALSE, OptionTypes.BOOLEAN_TYPE, "Enables colored console output.");

    private static boolean prettifyOutput() {
        return prettifyOutput.getValue() == Boolean.TRUE;
    }

    public static String [] colorCodes = new String[Color.values().length];
    public static String [] backgroundColorCodes = new String[Color.values().length];

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
                return (char) 27 + colorCodes[color.ordinal()] + s + (char) 27 + "[0m";
            }
            return s;
        }
    }

    static {
        colorCodes[Color.BLACK.ordinal()] = "[30m";
        colorCodes[Color.GRAY.ordinal()] = "[1;30m";
        colorCodes[Color.WHITE.ordinal()] = "[37m";
        colorCodes[Color.RED.ordinal()] = "[31m";
        colorCodes[Color.LIGHTRED.ordinal()] = "[1;31m";
        colorCodes[Color.GREEN.ordinal()] = "[32m";
        colorCodes[Color.LIGHTGREEN.ordinal()] = "[1;32m";
        colorCodes[Color.YELLOW.ordinal()] = "[33m";
        colorCodes[Color.LIGHTYELLOW.ordinal()] = "[1;33m";
        colorCodes[Color.BLUE.ordinal()] = "[34m";
        colorCodes[Color.LIGHTBLUE.ordinal()] = "[1;34m";
        colorCodes[Color.MAGENTA.ordinal()] = "[35m";
        colorCodes[Color.LIGHTMAGENTA.ordinal()] = "[1;35m";
        colorCodes[Color.TEAL.ordinal()] = "[36m";
        colorCodes[Color.LIGHTTEAL.ordinal()] = "[1;36m";

        backgroundColorCodes[Color.BLACK.ordinal()] = "[40m";
        backgroundColorCodes[Color.RED.ordinal()] = "[41m";
        backgroundColorCodes[Color.GREEN.ordinal()] = "[42m";
        backgroundColorCodes[Color.YELLOW.ordinal()] = "[43m";
        backgroundColorCodes[Color.BLUE.ordinal()] = "[44m";
        backgroundColorCodes[Color.MAGENTA.ordinal()] = "[45m";
        backgroundColorCodes[Color.TEAL.ordinal()] = "[46m";
    }

    public static class ColoredPrintStream extends PrintStream implements ColoredConsole {

        public ColoredPrintStream(OutputStream outputStream) {
            super(outputStream);
        }

        public void color(Color background, Color foreground) {
            if (prettifyOutput()) {
                if (background != null) {
                    print((char) 27 + backgroundColorCodes[background.ordinal()]);
                }
                if (foreground != null) {
                    print((char) 27 + colorCodes[foreground.ordinal()]);
                }
            }
        }

        public void color(Color color) {
            if (prettifyOutput()) {
                print((char) 27 + colorCodes[color.ordinal()]);
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
                    Log.print((char) 27 + backgroundColorCodes[background.ordinal()]);
                }
                if (foreground != null) {
                    Log.print((char) 27 + colorCodes[foreground.ordinal()]);
                }
            }
        }

        public void color(Color color) {
            if (prettifyOutput()) {
                Log.print((char) 27 + colorCodes[color.ordinal()]);
            }
        }

        public void clearColor() {
            if (prettifyOutput()) {
                Log.print((char) 27 + "[0m");
            }
        }

        public void println(Color color, String s) {
            color(color);
            Log.println(s);
            clearColor();
        }

        public void println(String s) {
            Log.println(s);
        }

        public void print(Color color, String s) {
            color(color);
            Log.print(s);
            clearColor();
        }

        public void print(String s) {
            Log.print(s);
        }
    }

    public static ColoredConsole err;
    public static ColoredConsole out;
    public static ColoredConsole debug;

    static {
        err = new ColoredPrintStream(System.err);
        out = new ColoredPrintStream(System.out);
        debug = out;
    }

    public static ColoredConsole err() {
        return err;
    }

    public static ColoredConsole out() {
        return out;
    }

    public static ColoredConsole debug() {
        return debug;
    }

    public static void println(Color color, String s) {
        debug.println(color, s);
    }

    public static void println(String s) {
        debug.println(s);
    }

    public static void print(Color color, String s) {
        debug.print(color, s);
    }

    public static void print(String s) {
        debug.print(s);
    }

    public static void printf(Color color, String s, Object ... args) {
        debug.print(color, String.format(s, args));
    }

    public static void printf(String s, Object ... args) {
        debug.print(String.format(s, args));
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
