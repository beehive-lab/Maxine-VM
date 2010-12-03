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
package jtt.jni;

/*
 * @Harness: java
 * @Runs: 0 = true, 1 = true
 */
public class JNI_OverflowArguments {

    public static boolean test(int arg) {
        final byte [] buf = new byte[8338];
        final long jzfile = 0xdeadbeef;
        final long jzentry = 0xcafebabe;
        final long pos = 8192;
        final int off = 77;
        final int len = 177;

        if (arg == 0) {
            final int res = read1(jzfile, jzentry, pos, buf, off, len);
            if (res == len) {
                return true;
            }
        } else if (arg == 1) {
            final int res = read2(jzfile, jzentry, pos, buf, off, len);
            if (res == off) {
                return true;
            }
        }
        return false;
    }

    private static native int read1(long jzfile, long jzentry,
                   long pos, byte[] b, int off, int len);
    private static native int read2(long jzfile, long jzentry,
                    long pos, byte[] b, int off, int len);
}
