/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A test case for formatting using the new "printf" style.
 *
 * @author Ben L. Titzer
 */
public class Printf {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.printf("%s\n", args[i]);
        }
        System.out.printf("%d\n", 11);
        System.out.printf("%b\n", true);
        System.out.printf("%c\n", 'c');
        System.out.printf("%f\n", -12.3f);
        System.out.printf("%s\n", Blah.foo(Long.MAX_VALUE, Integer.MIN_VALUE, "blah"));
    }

    static class Blah {
        static Object foo(long p1, int p2, Object p3) {
            return p3;
        }
    }
}
