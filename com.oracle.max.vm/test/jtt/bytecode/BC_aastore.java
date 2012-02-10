/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: (true, 0) = 0; (true, 1) = 1; (true, 2) = 2; (false, 1) = 1; (false, 2) = 2
 */
public class BC_aastore {

    static Object[] param = {new Object(), null, "h"};
    static Object[] array1 = {null, null, null};
    static String[] array2 = {null, null, null};

    public static int test(boolean a, int indx) {
        Object[] array = a ? array1 : array2;
        Object val;
        val = param[indx];
        array[indx] = val;
        return indx;
    }

}
