/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.loop;

/*
 * @Harness: java
 * @Runs: 0 = "ok0"; 10 = "ok0"; 25 = "ok0";
 */
public class LoopSwitch01 {
    static int count = 0;
    public static String test(int arg) {
        String line;
        while ((line = string()) != null) {
            switch (line.charAt(0)) {
                case 'a':
                    new Object();
                    break;
                case 'b':
                    new Object();
                    break;
                default:
                    new Object();
                    break;
            }
        }
        return "ok" + count;
    }
    private static String string() {
        if (count == 0) {
            return null;
        }
        count--;
        return "" + ('a' + count);
    }
}
