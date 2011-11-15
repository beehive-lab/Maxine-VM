/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.optimize;

/*
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=40L; 1=85899345920L
 */
public class Reduce_Long04 {
    public static long test(long arg) {
        if (arg == 0) {
            return mul0(arg + 10);
        }
        if (arg == 1) {
            return mul1(arg + 9);
        }
        return 0;
    }
    public static long mul0(long x) {
        return x * 4;
    }
    public static long mul1(long x) {
        return x * 8589934592L;
    }
}
