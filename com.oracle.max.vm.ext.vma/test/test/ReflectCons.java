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
package test;

import java.lang.reflect.*;

public class ReflectCons {

    static class A {
        private int a;

        A(int a) {
            this.a = a;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("main entered");
        Constructor<?> c = ReflectCons.A.class.getDeclaredConstructor(new Class<?>[] {int.class});
        for (int i = 0; i < 100; i++) {
            @SuppressWarnings("unused")
            ReflectCons.A rc1 = (ReflectCons.A) c.newInstance(i);
            System.out.println("A constructed");
        }
        System.out.println("main exited");
    }

}
