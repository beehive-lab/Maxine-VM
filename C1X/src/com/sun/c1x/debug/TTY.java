/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.debug;

import java.util.regex.*;


/**
 * A collection of static methods for printing debug and informational output to a global {@link LogStream}.
 * The output can be (temporarily) suppressed per thread through use of a {@linkplain Filter filter}.
 *
 * @author Doug Simon
 */
public class TTY {

    /**
     * Support for thread-local suppression of {@link TTY}.
     *
     * @author Doug Simon
     */
    public static class Filter {
        private LogStream previous;
        private final Thread thread = Thread.currentThread();

        /**
         * Determines if a given filter matches the {@linkplain Object#toString() string} value of a given object.
         *
         * @param filter the pattern for matching. If {@code null}, then the match is successful. If it starts with "~",
         *            then a regular expression {@linkplain Pattern#matches(String, CharSequence) match} is performed
         *            where the regular expression is specified by {@code filter} without the "~" prefix. Otherwise, a
         *            simple {@linkplain String#contains(CharSequence) substring} match is performed where {@code
         *            filter} is the substring used.
         * @param object an object whose {@linkplain Object#toString() string} value is matched against {@code filter}
         * @return the result of the match
         */
        public static boolean matches(String filter, Object object) {
            if (filter == null) {
                return true;
            }
            String input = object.toString();
            if (filter.startsWith("~")) {
                return Pattern.matches(filter.substring(1), input);
            } else {
                return input.contains(filter);
            }
        }

        /**
         * Creates an object that will suppress {@link TTY} for the current thread if the given filter does not
         * {@linkplain #matches(String, Object) match} the given object. To revert the suppression state to how it was
         * before this call, the {@link #remove()} method must be called on the suppression object.
         *
         * @param filter the pattern used in the {@linkplain #matches(String, Object) match}
         * @param object the object used in the {@linkplain #matches(String, Object) match}
         */
        public Filter(String filter, Object object) {
            boolean suppressed = false;
            if (filter != null) {
                String input = object.toString();
                if (filter.startsWith("~")) {
                    suppressed = !Pattern.matches(filter.substring(1), input);
                } else {
                    suppressed = !input.contains(filter);
                }
                if (suppressed) {
                    previous = out();
                    out.set(LogStream.SINK);
                }
            }
        }

        /**
         * Reverts the suppression state of {@link TTY} to how it was before this object was constructed.
         */
        public void remove() {
            assert thread == Thread.currentThread();
            if (previous != null) {
                out.set(previous);
            }
        }
    }

    private static final LogStream log = new LogStream(System.out);
    private static final ThreadLocal<LogStream> out = new ThreadLocal<LogStream>() {
        @Override
        protected LogStream initialValue() {
            return log;
        };
    };

    /**
     * Gets the thread-local log stream to which the static methods of this class send their output.
     * This will either be a global log stream or the global {@linkplain LogStream#SINK sink} depending
     * on whether any suppression {@linkplain Filter filters} are in effect for the current thread.
     */
    public static LogStream out() {
        return out.get();
    }

    /**
     * @see LogStream#print(String)
     */
    public static void print(String s) {
        out().print(s);
    }

    /**
     * @see LogStream#print(int))
     */
    public static void print(int i) {
        out().print(i);
    }

    /**
     * @see LogStream#print(long)
     */
    public static void print(long i) {
        out().print(i);
    }

    /**
     * @see LogStream#print(char)
     */
    public static void print(char c) {
        out().print(c);
    }

    /**
     * @see LogStream#print(boolean)
     */
    public static void print(boolean b) {
        out().print(b);
    }

    /**
     * @see LogStream#print(double)
     */
    public static void print(double d) {
        out().print(d);
    }

    /**
     * @see LogStream#print(float)
     */
    public static void print(float f) {
        out().print(f);
    }

    /**
     * @see LogStream#println(String)
     */
    public static void println(String s) {
        out().println(s);
    }

    /**
     * @see LogStream#println()
     */
    public static void println() {
        out().println();
    }

    /**
     * @see LogStream#println(int)
     */
    public static void println(int i) {
        out().println(i);
    }

    /**
     * @see LogStream#println(long)
     */
    public static void println(long l) {
        out().println(l);
    }

    /**
     * @see LogStream#println(char)
     */
    public static void println(char c) {
        out().println(c);
    }

    /**
     * @see LogStream#println(boolean)
     */
    public static void println(boolean b) {
        out().println(b);
    }

    /**
     * @see LogStream#println(double)
     */
    public static void println(double d) {
        out().println(d);
    }

    /**
     * @see LogStream#println(float)
     */
    public static void println(float f) {
        out().println(f);
    }

    public static void print(String string, Object... args) {
        out().print(String.format(string, args));
    }

    public static void println(String string, Object... args) {
        out().println(String.format(string, args));
    }

    public static void fillTo(int i) {
        out().fillTo(i, ' ');
    }
}
