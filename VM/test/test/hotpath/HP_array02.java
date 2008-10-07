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
/*VCSID=6fd4e6b0-18d2-4a35-af6e-f32642275d47*/
// Checkstyle: stop
package test.hotpath;
/*
 * @Harness: java
 * @Runs: 40 = 5460;
 */
public class HP_array02 {
    public static byte[] _b = new byte[40];
    public static char[] _c = new char[40];
    public static short[] _s = new short[40];
    public static int[] _i = new int[40];
    public static long[] _l = new long[40];
    public static float[] _f = new float[40];
    public static double[] _d = new double[40];

    public static int test(int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            _b[i] = (byte) i;
            _c[i] = (char) i;
            _s[i] = (short) i;
            _i[i] = i;
            _l[i] = i;
            _f[i] = i;
            _d[i] = i;
            sum += _b[i] + _c[i] + _s[i] + _i[i] + _l[i] + _f[i] + _d[i];
        }
        return sum;
    }
}
