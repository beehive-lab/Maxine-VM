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
/*
 * @Harness: java
 * @Runs: 0 = true
 */
package test.bench.java.lang;

import test.bench.util.*;

public class ArrayCopy_Loop extends RunBench {

    protected ArrayCopy_Loop() {
        super(new Bench());
    }

    public static boolean test(int i) {
        return new ArrayCopy_Loop().runBench(true);
    }

    public static class Bench extends MicroBenchmark {
        private static final byte[] TEST_BYTES = "HTTP/1.0".getBytes();
        public static int nonConstByteLength = TEST_BYTES.length;
        private byte[] buf;

        @Override
        public void prerun() {
            buf = new byte[TEST_BYTES.length];
        }

        @Override
        public long run() {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = TEST_BYTES[i];
            }
            return defaultResult;
        }
    }

    public static void main(String[] args) {
        test(0);
    }
}
