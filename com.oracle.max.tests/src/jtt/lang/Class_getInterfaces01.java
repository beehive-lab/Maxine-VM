/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.lang;

/*
 * @Harness: java
 * @Runs: 0 = "";
 * @Runs: 1 = "jtt.lang.Class_getInterfaces01$I1";
 * @Runs: 2 = "jtt.lang.Class_getInterfaces01$I1";
 * @Runs: 3 = "jtt.lang.Class_getInterfaces01$I2";
 * @Runs: 4 = "jtt.lang.Class_getInterfaces01$I1 jtt.lang.Class_getInterfaces01$I2";
*/

public final class Class_getInterfaces01 {
    private Class_getInterfaces01() {
    }

    public static String test(int i)  {
        switch (i) {
            case 0:
                return toString(I1.class);
            case 1:
                return toString(I2.class);
            case 2:
                return toString(C1.class);
            case 3:
                return toString(C2.class);
            case 4:
                return toString(C12.class);
            default:
                return null;
        }
    }

    private static String toString(Class<?> klass) {
        final Class<?>[] classes = klass.getInterfaces();
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Class<?> c : classes) {
            if (!first) {
                sb.append(' ');
            } else {
                first = false;
            }
            sb.append(c.getName());
        }
        return sb.toString();
    }

    static interface I1 {

    }

    static interface I2 extends I1 {

    }

    static class C1 implements I1 {

    }

    static class C2 implements I2 {

    }

    static class C12 implements I1, I2 {

    }

}
