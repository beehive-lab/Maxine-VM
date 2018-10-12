/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

import java.util.*;

/**
 * A simple class to allocate a lot of memory and then catch an OutOfMemory exception.
 */
public class CatchOutOfMemory {
    public static void main(String[] args) {
        System.out.println("starting...");
        if (test(0) == 0) {
            System.out.println("ok.");
            System.out.flush();
/*            if (test(1) == 0) {
                System.out.println("ok.");
                System.out.flush();
                System.exit(30);
            }
*/
            System.exit(10);
        } else {
            System.out.println("failed.");
            System.out.flush();
            System.exit(20);
        }
    }
    public static int test(int a) {
        List<Object[]> leak = new ArrayList<Object[]>();
        try {
            while (true) {
                leak.add(new Object[200000]);
            }
        } catch (OutOfMemoryError ex) {
            return 0;
        } catch (Throwable ex) {
            System.out.println(ex);
            return -1;
        } finally {
            leak = null;
        }
    }
}
