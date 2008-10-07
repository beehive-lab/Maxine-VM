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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.template.source.*;

/**
 * Testing the JIT-compiler with methods performing multi-dimensional array allocations.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileMultianewarray extends JitCompilerTestCase {
    /**
     * Testing with unresolved, and resolved class constant.
     */
    @Override
    protected Class[] templateSources() {
        return new Class[]{UnoptimizedBytecodeTemplateSource.class, ResolvedBytecodeTemplateSource.class, InstrumentedBytecodeSource.class};
    }

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
