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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;

/**
 * Testing the JIT-compiler with methods performing multi-dimensional array allocations.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileMultianewarray extends JitCompilerTestCase {

    void perform_unresolved_2_dimension() {
        @SuppressWarnings("unused")
        final UnresolvedAtTestTime[][] a = new UnresolvedAtTestTime  [4][100];
    }

    void perform_unresolved_3_dimension() {
        @SuppressWarnings("unused")
        final UnresolvedAtTestTime[][][] a = new  UnresolvedAtTestTime  [4][100][100];
    }

    void perform_unresolved_4_dimension() {
        @SuppressWarnings("unused")
        final UnresolvedAtTestTime[][][][] a = new  UnresolvedAtTestTime  [100] [100][100][100];
    }

    void perform_resolved_2_dimension() {
        @SuppressWarnings("unused")
        final JITTest_compileMultianewarray[][] a = new JITTest_compileMultianewarray  [4][100];
    }

    void perform_resolved_3_dimension() {
        @SuppressWarnings("unused")
        final JITTest_compileMultianewarray[][][] a = new  JITTest_compileMultianewarray  [4][100][100];
    }

    void perform_resolved_4_dimension() {
        @SuppressWarnings("unused")
        final JITTest_compileMultianewarray[][][][] a = new  JITTest_compileMultianewarray  [100] [100][100][100];
    }

    void perform_int_2_dimension() {
        @SuppressWarnings("unused")
        final int[][]  a = new  int [100][100];
    }

    void perform_int_4_dimension() {
        @SuppressWarnings("unused")
        final int[][][][] a = new  int  [100][100][100][100];
    }

    void perform_byte_2_dimension() {
        @SuppressWarnings("unused")
        final byte[][]  a = new  byte [100][100];
    }

    void perform_byte_4_dimension() {
        @SuppressWarnings("unused")
        final byte[][][][] a = new  byte  [100][100][100][100];
    }

    void perform_double_2_dimension() {
        @SuppressWarnings("unused")
        final double[][]  a = new  double [100][100];
    }

    void perform_double_4_dimension() {
        @SuppressWarnings("unused")
        final double[][][][] a = new  double  [100][100][100][100];
    }

    public void test_unresolved_2_dimension() {
        compileMethod("perform_unresolved_2_dimension");
    }

    public void test_unresolved_3_dimension() {
        compileMethod("perform_unresolved_3_dimension");
    }

    public void test_unresolved_4_dimension() {
        compileMethod("perform_unresolved_4_dimension");
    }

    public void test_resolved_2_dimension() {
        initializeClassInTarget(JITTest_compileMultianewarray[][].class);
        compileMethod("perform_resolved_2_dimension");
    }

    public void test_resolved_3_dimension() {
        initializeClassInTarget(JITTest_compileMultianewarray[][][].class);
        compileMethod("perform_resolved_3_dimension");
    }

    public void test_resolved_4_dimension() {
        initializeClassInTarget(JITTest_compileMultianewarray[][][][].class);
        compileMethod("perform_resolved_4_dimension");
    }

    public void test_int_2_dimension() {
        compileMethod("perform_int_2_dimension");
    }

    public void test_int_4_dimension() {
        compileMethod("perform_int_4_dimension");
    }

    public void test_byte_2_dimension() {
        compileMethod("perform_byte_2_dimension");
    }

    public void test_byte_4_dimension() {
        compileMethod("perform_byte_4_dimension");
    }

    public void test_double_2_dimension() {
        compileMethod("perform_double_2_dimension");
    }

    public void test_double_4_dimension() {
        compileMethod("perform_double_4_dimension");
    }

}
