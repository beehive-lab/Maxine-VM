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
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
/*
 * @Harness: java
 * @Runs: 0 = "XYZ"; 1 = "string"; 2 = "class java.lang.String"; 3 = !java.lang.NullPointerException
 */
package jtt.lang;

public class Object_toString02 {

    static final Object obj = new Object_toString02();

    public static String test(int i) {
        Object object = null;
        if (i == 0) {
            object = obj;
        } else if (i == 1) {
            object = "string";
        } else if (i == 2) {
            object = "string".getClass();
        }
        return object.toString();
    }

    @Override
    public String toString() {
        return "XYZ";
    }
}
