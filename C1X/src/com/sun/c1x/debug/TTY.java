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

import com.sun.c1x.util.Util;

/**
 * A collection of static methods for printing debug and informational output to a global {@link LogStream}.
 *
 * @author Doug Simon
 */
public class TTY {

    /**
     * The global C1X log stream.
     */
    public static final LogStream out = new LogStream(System.out);

    /**
     * @see LogStream#print(String)
     */
    public static void print(String s) {
        out.print(s);
    }

    /**
     * @see LogStream#print(int))
     */
    public static void print(int i) {
        out.print(i);
    }

    /**
     * @see LogStream#print(long)
     */
    public static void print(long i) {
        out.print(i);
    }

    /**
     * @see LogStream#print(char)
     */
    public static void print(char c) {
        out.print(c);
    }

    /**
     * @see LogStream#print(boolean)
     */
    public static void print(boolean b) {
        out.print(b);
    }

    /**
     * @see LogStream#print(double)
     */
    public static void print(double d) {
        out.print(d);
    }

    /**
     * @see LogStream#print(float)
     */
    public static void print(float f) {
        out.print(f);
    }

    /**
     * @see LogStream#println(String)
     */
    public static void println(String s) {
        out.println(s);
    }

    /**
     * @see LogStream#println()
     */
    public static void println() {
        out.println();
    }

    /**
     * @see LogStream#println(int)
     */
    public static void println(int i) {
        out.println(i);
    }

    /**
     * @see LogStream#println(long)
     */
    public static void println(long l) {
        out.println(l);
    }

    /**
     * @see LogStream#println(char)
     */
    public static void println(char c) {
        out.println(c);
    }

    /**
     * @see LogStream#println(boolean)
     */
    public static void println(boolean b) {
        out.println(b);
    }

    /**
     * @see LogStream#println(double)
     */
    public static void println(double d) {
        out.println(d);
    }

    /**
     * @see LogStream#println(float)
     */
    public static void println(float f) {
        out.println(f);
    }

    public static void print(String string, Object... args) {
        out.print(String.format(string, args));
    }

    public static void println(String string, Object... args) {
        out.println(String.format(string, args));
    }

    public static void cr() {
        println();
    }

    public static void fillTo(int i) {
        Util.nonFatalUnimplemented();
    }
}
