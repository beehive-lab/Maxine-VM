/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
