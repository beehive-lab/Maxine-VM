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
/*
 * @Harness: java
 * @Runs: 0 = null;
 * @Runs: 1 = "int";
 * @Runs: 2 = null;
 * @Runs: 3 = "java.lang.Object";
 * @Runs: 4 = null;
 * @Runs: 5 = null;
 * @Runs: 6 = "[Ljava.lang.Object;";
 * @Runs: 7 = null;
 * @Runs: 8 = null;
 */
package jtt.lang;

public final class Class_getComponentType01 {
    private Class_getComponentType01() {
    }

    public static String test(int i) {
        Class cl = Object.class;
        if (i == 0) {
            cl = int.class;
        } else if (i == 1) {
            cl = int[].class;
        } else if (i == 2) {
            cl = Object.class;
        } else if (i == 3) {
            cl = Object[].class;
        } else if (i == 4) {
            cl = Class_getComponentType01.class;
        } else if (i == 5) {
            cl = Cloneable.class;
        } else if (i == 6) {
            cl = Object[][].class;
        } else if (i == 7) {
            cl = void.class;
        }
        cl = cl.getComponentType();
        if (cl == null) {
            return null;
        }
        return cl.getName();
    }
}
