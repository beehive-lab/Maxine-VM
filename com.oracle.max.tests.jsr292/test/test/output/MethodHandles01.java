/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;

public class MethodHandles01 {

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class);
        MethodHandle mh = lookup.findVirtual(MethodHandles01.class, "hello", mt);
        MethodHandles01 self = new MethodHandles01();
        mh.invokeExact(self);
        MethodHandle mh2 = lookup.findStatic(MethodHandles01.class, "world", mt);
        mh2.invokeExact();
        MethodType mtx = MethodType.methodType(void.class, int.class);
        MethodHandle mhx = lookup.findVirtual(MethodHandles01.class, "ga", mtx);
        mhx.invokeExact(self, 23);
    }

    public void hello() {
        System.out.print("Hello, ");
    }

    public static void world() {
        System.out.println("world!");
    }

    public void ga(int x) {
        System.out.println(x);
    }

}
