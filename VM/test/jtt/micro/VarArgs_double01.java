/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package jtt.micro;

/*
 * @Harness: java
 * @Runs: 0=0.0d; 1=1.0d; 2=2.0d; 3=!java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: 4=!java.lang.ArrayIndexOutOfBoundsException
 */
public class VarArgs_double01 {
    public static double test(int arg) {
        if (arg == 4) {
            return get(0);
        }
        return get(arg, 0.0d, 1.0d, 2.0d);
    }

    private static double get(int index, double... args) {
        return args[index];
    }
}
