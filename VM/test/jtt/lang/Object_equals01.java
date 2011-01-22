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
 * @Runs: 0=false; 1=false; 2=false; 3=true; 4=true; 5=false; 6=true; 7=false; 8=false
 */
package jtt.lang;

public final class Object_equals01 {
    private Object_equals01() {
    }

    public static Object_equals01 field = new Object_equals01();

    public static boolean test(int i) {
        final Object obj1 = new Object();
        final Object obj2 = new Object();
        switch (i) {
            case 0:
                return obj1.equals(field);
            case 1:
                return obj1.equals(obj2);
            case 2:
                return obj1.equals(null);
            case 3:
                return obj1.equals(obj1);
            case 4:
                return field.equals(field);
            case 5:
                return obj2.equals(field);
            case 6:
                return obj2.equals(obj2);
            case 7:
                return obj2.equals(null);
            case 8:
                return obj2.equals(obj1);
        }
        return false;
    }
}
