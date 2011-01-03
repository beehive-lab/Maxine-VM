/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.test;

/**
 * A class on which to test the interpreter remotely.
 *
 * @author Athul Acharya
 */
public class TeleInterpreterRemoteTestClass {

    protected TeleInterpreterRemoteTestClass() { }

    protected TeleInterpreterRemoteTestClass(int y) {
        this.y = y;
    }

    protected TeleInterpreterRemoteTestClass(String z) {
        this.z = z;
    }

    private String z;

    private static int x = 0xdeadbeef;

    private static TeleInterpreterRemoteTestClass object = new TeleInterpreterRemoteTestClass();

    private static TeleInterpreterRemoteTestChildClass subclassObject = new TeleInterpreterRemoteTestChildClass();

    private static Object[] array = new Object[2];

    private static int[][] iarray = {{0xba5eba11, 2}, {3, 4}};

    public int y = 0xcafebabe;

    @Override
    public String toString() {
        return z;
    }

    //just here for sanity check
    public static int iadd(int a, int b) {
        return a + b;
    }

    public static int getstatic() {
        final int y = x;
        return y;
    }

    public static int getfield() {
        return object.y;
    }

    public static int arraylength() {
        return array.length;
    }

    public static int iaload_aaload() {
        return iarray[0][0];
    }

    public static int getfield2(TeleInterpreterRemoteTestClass i) { //for use from inspector
        return i.y;
    }

    public int return_y() {
        return y;
    }

    public int virtual_overriden(int a, int b, int c) {
        return 3;
    }

    public static int invokevirtual1(TeleInterpreterRemoteTestClass i) { //for use from inspector
        return i.virtual_overriden(1, 2, 3);
    }

    public static int invokevirtual2() {
        final TeleInterpreterRemoteTestClass i = object;
        return i.virtual_overriden(1, 2, 3); //should return 3
    }

    public static int invokevirtual3() {
        final TeleInterpreterRemoteTestClass i = subclassObject;
        return i.virtual_overriden(1, 2, 3); //should return 6
    }

    public static int new1() {
        final TeleInterpreterRemoteTestClass i = new TeleInterpreterRemoteTestClass();
        return i.y;
    }

    public static int new2() {
        final TeleInterpreterRemoteTestClass i = new TeleInterpreterRemoteTestClass(0x80081355);
        return i.y;
    }

    public static int putfield() {
        final TeleInterpreterRemoteTestClass i = new TeleInterpreterRemoteTestClass(0x80081355);

        i.y = 666;
        return i.y;
    }

    private static final String testString = new String("test");

    public static TeleInterpreterRemoteTestClass stringnew() {
        return new TeleInterpreterRemoteTestClass(testString);
    }
}
