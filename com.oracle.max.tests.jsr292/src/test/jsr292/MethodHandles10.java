/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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

package test.jsr292;

import static java.lang.invoke.MethodType.*;

import java.lang.invoke.*;

/**
 * Test for method handle constructor.
 */
public class MethodHandles10 {

    public static void main(String[] args) {
        try {
            MethodHandle mh =
                    MethodHandles.lookup().findConstructor(MethodHandles10.class, methodType(void.class));
            for (int i = 0; i < 1000000; i++) {
                MethodHandles10 mh10 = (MethodHandles10) mh.invokeExact();
                System.out.println(mh10.toString());
            }
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "MethodHandles10";
    }
}
