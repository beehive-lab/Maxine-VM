/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 0 = 0; 1 = 1; 2 = 2; 3 = 3; -4 = -4
 */
public class BC_invokestatic {
    public static int test(int a) {
        int tmp = 11;
        a = tmp * a;
        return id(a);
    }
    /*public static int id(int i) {
        return id1(i+1);
    }
    public static int id1(int i) { return id2(i+2); }
    public static int id2(int i) {
        return id3(i+3);
    }
    public static int id3(int i) { int a;
        a = i; return a;}//id4(i); }
    /*public static int id4(int i) {
        return id5(i);
    }
    public static int id5(int i) { return id6(i); }
    public static int id6(int i) {
        return id7(i);
    }
    public static int id7(int i) { return id8(i); }
    public static int id8(int i) {
        return id9(i);
    }
    public static int id9(int i) { return id10(i); }
    public static int id10(int i) { return (i); } */
    public static int id(int i) {
        return id1(i);
    }
    public static int id1(int i) { return id2(i+1); }
    public static int id2(int i) {
        return id3(i+2);
    }
    public static int id3(int i) { return id4(i+3); }
    public static int id4(int i) {
        return id5(i+4);
    }
    public static int id5(int i) { return id6(i+5); }
    public static int id6(int i) {
        return id7(i+6);
    }
    public static int id7(int i) { return id8(i+7); }
    public static int id8(int i) {
        return id9(i+8);
    }
    public static int id9(int i) { return id10(i+9); }
    public static int id10(int i) { return (i+10); }


}
