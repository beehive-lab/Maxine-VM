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
package jtt.jdk;

import java.io.*;

import com.sun.max.annotate.*;

/*
 * @Harness: java
 * @Runs: 10000 = 100000
 */
public class System_setOut {

    public static int test(int n) throws Exception {
        PrintStream oldOut = System.out;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            ByteArrayOutputStream ba = new ByteArrayOutputStream(n * 10);
            PrintStream newOut = new PrintStream(ba);
            System.setOut(newOut);
            doPrint(n);
            sum += ba.size();
        }

        System.setOut(oldOut);
        return sum;
    }

    @NEVER_INLINE
    private static void doPrint(int n) {
        for (int i = 0; i < n; i++) {
            System.out.print('x');
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(test(10000));
    }
}
