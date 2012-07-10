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
 * Simple program to test/demo the capability of the inspector with respect to generational GC.
 * Run with 16m heap. The demo arrange for full GC to occur, with lot of garbage old generation
 * and a distinguishable live old object so as to ease verifying the inspector's behavior at GC time wrt to live and dead objects.
 */
public class InspectorGCDemo2 {
    static Random rand = new Random(849375698743563L);

    static Link  longLivedObjects = new Link();

    static class Link {
        Link next;
        HashMap<Integer, byte[]> payload;

        Link(HashMap<Integer, byte[]> payload) {
            this.payload = payload;
            InspectorGCDemo2.longLivedObjects.next = this;
        }
        Link() {
        }
    }

    static void setLongLivedObjects(HashMap<Integer, byte[]> tree) {
        longLivedObjects = new Link(tree);
    }

    static private void createLongLiveObjects(long nBytes) {
        HashMap<Integer, byte[]>tree = new HashMap<Integer, byte[]>();
        int count = 0;
        setLongLivedObjects(tree);
        final long arrayHeader = 24;
        long n = 0;
        while (n < nBytes) {
            int s = rand.nextInt(100);
            tree.put(new Integer(count++), new byte[s]);
            n += arrayHeader + s;
        }
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

    public static void main(String[] args) {
        long garbageAmount = 10L << 20;
        long longLiveAmount = 512L << 10;

        // Create long lived objects that will accumulated in the old generation and be dead after promotion so as to force
        // a full GC.
        for (int i = 0; i < 20; i++) {
            // Create a new set of long lived data that will be promoted in the old generation in this iteration
            createLongLiveObjects(longLiveAmount);
            // Create enough garbage for the long lived objects created in this iteration to be promoted by GC
            createGarbage(garbageAmount);
        }
    }
}
