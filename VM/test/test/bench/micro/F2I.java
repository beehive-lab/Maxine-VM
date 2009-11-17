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
package test.bench.micro;

/**
 * A microbenchmark for floating point to integer conversions.
 * @author Ben L. Titzer
 */
public class F2I {

    private static int CHUNK_SIZE = 5000;
    private static final int ITERATIONS = 500000000;

    public static void main(String[] args) {
        int count = ITERATIONS;
        if (args.length > 0) {
            count = Integer.parseInt(args[0]);
        }
        benchmark(count);
    }

    public static void benchmark(int count) {
        int chunks = (count + CHUNK_SIZE - 1) / CHUNK_SIZE;
        float fsum = 0;
        int isum = 0;
        for (int i = 0; i <= chunks; i++) {
            fsum += 0.4;
            isum = chunk(fsum, isum, CHUNK_SIZE);
        }
        System.out.println(fsum);
        System.out.println(isum);
    }

    private static int chunk(float fsum, int isum, int count) {
        for (int i = 0; i < count; i++) {
            fsum += 0.1;
            isum = (int) fsum;
        }
        return isum;
    }
}
