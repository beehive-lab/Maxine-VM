/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @Harness: java
 * @Runs: (-2, 0s) = !java.lang.NullPointerException; (-1, 3s) = !java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: (0, 0s) = 0s;
 * @Runs: (4, 0s) = !java.lang.ArrayIndexOutOfBoundsException
 */
package jtt.except;

public class BC_sastore {

    static short[] arr = {0, 0, 0, 0};

    public static short test(int arg, short val) {
        final short[] array = arg == -2 ? null : arr;
        array[arg] = val;
        return array[arg];
    }
}
