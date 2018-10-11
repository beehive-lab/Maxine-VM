/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Simple program to test/demo the capability of the inspector with respect to GC.
 * This program makes it easier to set a view on a dead object before first GC.
 * Simply set a breakpoint on method entry to the method {@link #setTransientRoot()} and
 * open a view on the result of allocated object.
 * Then set a breakpoint to the GCOperation's phase of interest and continue.
 */
public class InspectorGCDemo {
    static Random rand = new Random(849375698743563L);
    static Object root = null;

    static void setTransientRoot() {
        root = new InspectorGCDemo();
        root = null;

    }
    static private void createGarbage(long nBytes) {
        final long arrayHeader = 24;
        long n = 0;
        while (n < nBytes) {
            int s = rand.nextInt(100);
            @SuppressWarnings("unused")
            byte [] b = new byte[s];
            n += arrayHeader + s;
        }

    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        setTransientRoot();
        long garbageAmount = 100L << 20;
        createGarbage(garbageAmount);
    }

}
