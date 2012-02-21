/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
public class RelocatableWatchpointTest1 {

    /**
     * @param args
     */

    private static final int allocations = 10000000;
    private static final int allocationSize = 20;

    public static String getMessage(String msg) {
        return new String(msg);
    }

    public static String getGarbageMessage() {
        return new String("allocationTestGarbage");
    }

    public static void printMessage(String message) {
        System.out.println(message);
    }

    public static void relocationTest() {
        String test = getMessage("test1");
        String test1 = getGarbageMessage();
        for (int i = 0; i < allocations; i++) {
            final byte[] tmp = new byte[allocationSize];
            tmp[0] = 1;
        }
        printMessage(test);
        printMessage(test1);
        for (int i = 0; i < allocations; i++) {
            final byte[] tmp = new byte[allocationSize];
            tmp[0] = 1;
        }
        printMessage(test);
        printMessage(test1);
    }

    public static void main(String[] args) {
        relocationTest();
    }
}
