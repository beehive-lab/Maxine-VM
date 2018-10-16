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

import java.lang.invoke.*;

/**
 * Unit test for method handle getters.
 *
 */
public class MethodHandles09 {

    static String hello = "Hello ";

    String world = "World!";


    static MethodHandles.Lookup lookup() {
        return MethodHandles.lookup();
    }

    public static void main(String [] args) {
        try {
            MethodHandle getHello = lookup().findStaticGetter(MethodHandles09.class, "hello", String.class);
            MethodHandle getWorld = lookup().findGetter(MethodHandles09.class, "world", String.class);

            MethodHandles09 mh09 = new MethodHandles09();
            String h = (String) getHello.invokeExact();
            String w = (String) getWorld.invokeExact(mh09);
            System.out.print(h);
            System.out.println(w);

        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
