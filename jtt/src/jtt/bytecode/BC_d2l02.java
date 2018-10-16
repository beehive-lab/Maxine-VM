/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */

package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 0 = -9223372036854775808L; 1 = -9223372036854775808L; 2 = 0L; 3 = 9223372036854775807L; 4 = 9223372036854775807L
 */
public class BC_d2l02 {

    private static double[] inputs = {-1.3e44d, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY, 1.3e44d};

    public static long test(int i) {
        return (long) inputs[i];
    }

    public static double get(int i) {
        return inputs[i];
    }
}
