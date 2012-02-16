/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.optimize;

/*
 * Tests calls to the array copy method.
 * @Harness: java
 * @Runs: (0,0,0)=0;
 * @Runs: (0,0,-1)=!java.lang.IndexOutOfBoundsException;
 * @Runs: (-1,0,0)=!java.lang.IndexOutOfBoundsException;
 * @Runs: (0,-1,0)=!java.lang.IndexOutOfBoundsException;
 * @Runs: (0,0,2)=0;
 * @Runs: (0,1,2)=!java.lang.IndexOutOfBoundsException;
 * @Runs: (1,0,2)=!java.lang.IndexOutOfBoundsException;
 * @Runs: (1,1,-1)=!java.lang.IndexOutOfBoundsException;
 */
public class ArrayCopy01 {
    public static Object[] src = new Object[]{null, null};
    public static Object[] dest = new Object[]{null, null};
    public static int test(int srcPos, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
        return 0;
    }
}
