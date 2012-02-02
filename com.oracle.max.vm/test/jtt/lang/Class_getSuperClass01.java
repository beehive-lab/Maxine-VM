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
/*
 * @Harness: java
 * @Runs: 0 = null;
 * @Runs: 1 = null;
 * @Runs: 2 = "java.lang.Object";
 * @Runs: 3 = null;
 * @Runs: 4 = "java.lang.Number";
 * @Runs: 5 = "java.lang.Object";
 * @Runs: 6 = "java.lang.Object";
 * @Runs: 7 = null;
 */
package jtt.lang;

public final class Class_getSuperClass01 {
    private Class_getSuperClass01() {
    }

    public static String test(int i) {
        Class cl = Object.class;
        if (i == 0) {
            cl = int.class;
        } else if (i == 1) {
            cl = Object.class;
        } else if (i == 2) {
            cl = int[].class;
        } else if (i == 3) {
            cl = Cloneable.class;
        } else if (i == 4) {
            cl = Integer.class;
        } else if (i == 5) {
            cl = Class.class;
        } else if (i == 6) {
            cl = Class_getSuperClass01.class;
        }
        cl = cl.getSuperclass();
        if (cl == null) {
            return null;
        }
        return cl.getName();
    }
}
