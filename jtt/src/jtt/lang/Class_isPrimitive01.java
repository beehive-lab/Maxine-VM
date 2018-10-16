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
 * @Runs: 0 = true; 1 = false; 2 = false; 3 = false; 4 = false; 5 = false; 6 = false; 7 = true; 8 = false
 */
package jtt.lang;

public final class Class_isPrimitive01 {
    private Class_isPrimitive01() {
    }

    public static boolean test(int i) {
        if (i == 0) {
            return int.class.isPrimitive();
        }
        if (i == 1) {
            return int[].class.isPrimitive();
        }
        if (i == 2) {
            return Object.class.isPrimitive();
        }
        if (i == 3) {
            return Object[].class.isPrimitive();
        }
        if (i == 4) {
            return Class_isPrimitive01.class.isPrimitive();
        }
        if (i == 5) {
            return Cloneable.class.isPrimitive();
        }
        if (i == 6) {
            return Runnable.class.isPrimitive();
        }
        if (i == 7) {
            return void.class.isPrimitive();
        }
        return false;
    }
}
