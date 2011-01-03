/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.inspector;

/**
 * Simple Inspector test to proof the implemented watchpoints code.
 *
 * @author Hannes Payer
 */
public class RelocatableWatchpointTest2 {

    /**
     * @param args
     */

    private static final int allocations = 10000000;
    private static final int allocationSize = 20;

    public static SimpleObject getSimpleObject() {
        return new SimpleObject(100, 200, "live");
    }

    public static SimpleObject getGarbageSimpleObject() {
        return new SimpleObject(10, 20, "garbage");
    }

    public static void printMessage(SimpleObject simpleObject) {
        System.out.println(simpleObject.value1 + " " + simpleObject.value2);
    }

    public static void relocationTest() {
        SimpleObject test = getGarbageSimpleObject();
        test = getSimpleObject();
        System.gc();
        printMessage(test);
        System.gc();
        printMessage(test);
        System.gc();
        printMessage(test);
        System.out.println("program end");
    }

    private static void longTest() {
        for (int i = 0; i < 1000; i++) {
            System.gc();
            if (i % 10 == 0) {
                System.out.print(".");
            }
        }
    }

    public static void main(String[] args) {
        relocationTest();
        longTest();
    }

    private static class SimpleObject {

        public SimpleObject(int value1, int value2, String text) {
            this.value1 = value1;
            this.value2 = value2;
            this.string = text;
        }

        public int value1;
        public int value2;
        public String string;
    }
}
