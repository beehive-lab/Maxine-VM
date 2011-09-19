/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.exbytecode;

// memory -> register

/*
 * @Harness: java
 * @Runs: 0x7fc00000 = ()java.lang.Float.isNaN; 0x3f800000 = 1.0f; -1082130432 = -1.0f; 0x48e6f500 = 473000.0f
*/
public class EBC_movi2f_02 {
    static class I {
        int i;
        I(int i) {
            this.i = i;
        }
    }
    public static float test(int arg) {
        I i = new I(arg);
        return Float.intBitsToFloat(i.i);
    }

}
