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
/*VCSID=51500d17-3cbf-4b1a-8409-0328f2302a1b*/

package com.sun.max.bench.benchmarks;
import java.lang.reflect.*;

import com.sun.max.bench.*;


public class Shootout implements Runnable {

    Method _m;
    String _programName;
    String[] _args;

    public Shootout(String programName, String... args) {
        _args = args;
        _programName = programName;
        try {
            _m = Class.forName("shootout." + programName).getMethod("main", String[].class);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            _m.invoke(null, (Object) _args);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Shootout:" + _programName;
    }

    // Checkstyle: stop field name check
    public static Shootout ackermann = new Shootout("ackermann", "10");
    public static Shootout ary = new Shootout("ary", "100000");
    public static Shootout binarytrees = new Shootout("binarytrees", "14");
    public static Shootout chameneos = new Shootout("chameneos", "100000");
    public static Shootout fibo = new Shootout("fibo", "40");
    public static Shootout hash = new Shootout("hash", "100000");
    public static Shootout hash2 = new Shootout("hash2", "1000");
    public static Shootout lists = new Shootout("lists", "100");
    public static Shootout magicsquares = new Shootout("magicsquares", "4");
    public static Shootout matrix = new Shootout("matrix", "10000");
    public static Shootout message = new Shootout("message", "1000"); /* calls exit */
    public static Shootout meteor = new Shootout("meteor");
    public static Shootout methcall = new Shootout("methcall", "100000000");
    public static Shootout nestedloop = new Shootout("nestedloop", "30");
    public static Shootout nsieve = new Shootout("nsieve", "1000");
    public static Shootout nsievebits = new Shootout("nsievebits", "1000");
    public static Shootout objinst = new Shootout("objinst", "10000000");
    public static Shootout pidigits = new Shootout("pidigits", "1000");
    public static Shootout process = new Shootout("process", "1000"); /* calls exit */
    public static Shootout prodcons = new Shootout("prodcons", "100000");
    public static Shootout sieve = new Shootout("sieve", "10000");
    public static Shootout strcat = new Shootout("strcat", "2000000");
    public static Shootout takfp = new Shootout("takfp", "10");
    // Checkstyle: resume field name check

    public static void main(String[] args) {
        final java.lang.reflect.Field[] fields = Shootout.class.getFields();
        final Runnable[] runnables = new Runnable[fields.length];
        for (int i = 0; i < fields.length; i++) {
            try {
                runnables[i] = (Runnable) fields[i].get(Shootout.class);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Benchmark.runBenchmarks(runnables, 2);
    }
}
