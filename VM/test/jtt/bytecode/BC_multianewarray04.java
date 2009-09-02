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
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 1 = 41; 2 = 81
 */
public class BC_multianewarray04 {
    public static int test(int a) {
        int i = 1;

        i += test_byte(a);
        i += test_boolean(a);
        i += test_char(a);
        i += test_short(a);
        i += test_int(a);
        i += test_float(a);
        i += test_long(a);
        i += test_double(a);

        return i;
    }

    private static int test_double(int a) {
        double[][] b2 = new double[a][a];
        double[][][] b3 = new double[a][a][a];
        double[][][][] b4 = new double[a][a][a][a];
        double[][][][][] b5 = new double[a][a][a][a][a];
        double[][][][][][] b6 = new double[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_long(int a) {
        long[][] b2 = new long[a][a];
        long[][][] b3 = new long[a][a][a];
        long[][][][] b4 = new long[a][a][a][a];
        long[][][][][] b5 = new long[a][a][a][a][a];
        long[][][][][][] b6 = new long[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_float(int a) {
        float[][] b2 = new float[a][a];
        float[][][] b3 = new float[a][a][a];
        float[][][][] b4 = new float[a][a][a][a];
        float[][][][][] b5 = new float[a][a][a][a][a];
        float[][][][][][] b6 = new float[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_int(int a) {
        int[][] b2 = new int[a][a];
        int[][][] b3 = new int[a][a][a];
        int[][][][] b4 = new int[a][a][a][a];
        int[][][][][] b5 = new int[a][a][a][a][a];
        int[][][][][][] b6 = new int[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_short(int a) {
        short[][] b2 = new short[a][a];
        short[][][] b3 = new short[a][a][a];
        short[][][][] b4 = new short[a][a][a][a];
        short[][][][][] b5 = new short[a][a][a][a][a];
        short[][][][][][] b6 = new short[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_char(int a) {
        char[][] b2 = new char[a][a];
        char[][][] b3 = new char[a][a][a];
        char[][][][] b4 = new char[a][a][a][a];
        char[][][][][] b5 = new char[a][a][a][a][a];
        char[][][][][][] b6 = new char[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_boolean(int a) {
        boolean[][] b2 = new boolean[a][a];
        boolean[][][] b3 = new boolean[a][a][a];
        boolean[][][][] b4 = new boolean[a][a][a][a];
        boolean[][][][][] b5 = new boolean[a][a][a][a][a];
        boolean[][][][][][] b6 = new boolean[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }

    private static int test_byte(int a) {
        byte[][] b2 = new byte[a][a];
        byte[][][] b3 = new byte[a][a][a];
        byte[][][][] b4 = new byte[a][a][a][a];
        byte[][][][][] b5 = new byte[a][a][a][a][a];
        byte[][][][][][] b6 = new byte[a][a][a][a][a][a];
        return b2.length + b3.length + b4.length + b5.length + b6.length;
    }
}