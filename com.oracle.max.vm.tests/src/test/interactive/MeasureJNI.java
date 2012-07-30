/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.interactive;

import jtt.jni.*;

/**
 * Simple program to measure the performance of JNI.
 */
public class MeasureJNI {

    private static final int N = 10000000;

    static {
        System.loadLibrary("javatest");
    }

    public static void main(String[] args) {
        warmup();
        doit();
    }

    // Ensures the JNI_Identity*.test methods are resolved before doit() is compiled.
    static void warmup() {
        JNI_IdentityInt.test(10);
        JNI_IdentityFloat.test(10f);
        JNI_IdentityLong.test(10L);
        JNI_IdentityObject.test("10");
        final Object[] arguments = new Object[56 + 1];
        for (int i = 0; i < N / 10; i++) {
            JNI_ManyObjectParameters.manyObjectParameters(arguments,
                            arguments[0],
                            arguments[1],
                            arguments[2],
                            arguments[3],
                            arguments[4],
                            arguments[5],
                            arguments[6],
                            arguments[7],
                            arguments[8],
                            arguments[9],
                            arguments[10],
                            arguments[11],
                            arguments[12],
                            arguments[13],
                            arguments[14],
                            arguments[15],
                            arguments[16],
                            arguments[17],
                            arguments[18],
                            arguments[19],
                            arguments[20],
                            arguments[21],
                            arguments[22],
                            arguments[23],
                            arguments[24],
                            arguments[25],
                            arguments[26],
                            arguments[27],
                            arguments[28],
                            arguments[29],
                            arguments[30],
                            arguments[31],
                            arguments[32],
                            arguments[33],
                            arguments[34],
                            arguments[35],
                            arguments[36],
                            arguments[37],
                            arguments[38],
                            arguments[39],
                            arguments[40],
                            arguments[41],
                            arguments[42],
                            arguments[43],
                            arguments[44],
                            arguments[45],
                            arguments[46],
                            arguments[47],
                            arguments[48],
                            arguments[49],
                            arguments[50],
                            arguments[51],
                            arguments[52],
                            arguments[53],
                            arguments[54],
                            arguments[55]);
        }
    }

    public static void doit() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            JNI_IdentityInt.test(10);
        }
        System.out.println("int " + (System.currentTimeMillis() -  start) + "ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            JNI_IdentityFloat.test(10f);
        }
        System.out.println("float " + (System.currentTimeMillis() -  start) + "ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            JNI_IdentityLong.test(10L);
        }
        System.out.println("long " + (System.currentTimeMillis() -  start) + "ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            JNI_IdentityObject.test("10");
        }
        System.out.println("Object " + (System.currentTimeMillis() -  start) + "ms");
        start = System.currentTimeMillis();
        final Object[] arguments = new Object[56 + 1];
        for (int i = 0; i < N / 10; i++) {
            JNI_ManyObjectParameters.manyObjectParameters(arguments,
                            arguments[0],
                            arguments[1],
                            arguments[2],
                            arguments[3],
                            arguments[4],
                            arguments[5],
                            arguments[6],
                            arguments[7],
                            arguments[8],
                            arguments[9],
                            arguments[10],
                            arguments[11],
                            arguments[12],
                            arguments[13],
                            arguments[14],
                            arguments[15],
                            arguments[16],
                            arguments[17],
                            arguments[18],
                            arguments[19],
                            arguments[20],
                            arguments[21],
                            arguments[22],
                            arguments[23],
                            arguments[24],
                            arguments[25],
                            arguments[26],
                            arguments[27],
                            arguments[28],
                            arguments[29],
                            arguments[30],
                            arguments[31],
                            arguments[32],
                            arguments[33],
                            arguments[34],
                            arguments[35],
                            arguments[36],
                            arguments[37],
                            arguments[38],
                            arguments[39],
                            arguments[40],
                            arguments[41],
                            arguments[42],
                            arguments[43],
                            arguments[44],
                            arguments[45],
                            arguments[46],
                            arguments[47],
                            arguments[48],
                            arguments[49],
                            arguments[50],
                            arguments[51],
                            arguments[52],
                            arguments[53],
                            arguments[54],
                            arguments[55]);
        }
        System.out.println("ManyObjects " + (System.currentTimeMillis() -  start) + "ms");
    }
}
