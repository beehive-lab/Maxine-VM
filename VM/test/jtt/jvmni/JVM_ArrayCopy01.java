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

package jtt.jvmni;

/*
 * @Harness: java
 * @Runs: 0 = true
 */
public class JVM_ArrayCopy01 {
    public static boolean test(int arg) {
        final String[] src = {"1", "2", "3", "4", "5", "6"};
        final String[] dest = {"1", "2", "3", "4", " 5", "6"};

        call(src, 3, dest, 0, 3);
        if (dest[0].equals("4") && dest[1].equals("5") && dest[2].equals("6")) {
            return true;
        }
        return false;
    }

    private static native void call(Object src, int srcPos, Object dest, int destPos, int len);
}
