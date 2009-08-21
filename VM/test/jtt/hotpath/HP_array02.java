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
package jtt.hotpath;
/*
 * @Harness: java
 * @Runs: 40 = 5460;
 */
public class HP_array02 {
    public static byte[] b = new byte[40];
    public static char[] c = new char[40];
    public static short[] s = new short[40];
    public static int[] iArray = new int[40];
    public static long[] l = new long[40];
    public static float[] f = new float[40];
    public static double[] d = new double[40];

    public static int test(int count) {
        int sum = 0;
        for (int x = 0; x < count; x++) {
            b[x] = (byte) x;
            c[x] = (char) x;
            s[x] = (short) x;
            iArray[x] = x;
            l[x] = x;
            f[x] = x;
            d[x] = x;
            sum += b[x] + c[x] + s[x] + iArray[x] + l[x] + f[x] + d[x];
        }
        return sum;
    }
}
