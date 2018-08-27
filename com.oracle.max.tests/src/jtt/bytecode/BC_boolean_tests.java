/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: (true,true,0) = true; (false,true,0) = false;
 * @Runs: (true,true,1) = false; (false,true,1) = true;
 * @Runs: (true,true,2) = true; (false,false,2) = true; (true,false,2) = false; (false,true,2) = false;
 * @Runs: (true,true,3) = false; (false,false,3) = false; (true,false,3) = true; (false,true,3) = true;
 */
public class BC_boolean_tests {

    public static boolean test(boolean a, boolean b, int i) {
        switch (i) {
            case 0:
                return a;
            case 1:
                return !a;
            case 2:
                return a == b;
            case 3:
                return a != b;
            default:
                return false;
        }
    }
}
