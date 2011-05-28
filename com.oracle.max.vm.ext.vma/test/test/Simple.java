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

/**
 * Very simple test program to test the basic functionality of the immutability analysis.
 *
 * @author Mick Jordan
 *
 */
public class Simple {

    public int stationary;
    public int p;
    public int q;

    Simple(int p, int q) {
        this.p = p;
        this.q = q;
    }

    public void setStationary(int x) {
        stationary = x;
    }

    public static void main(String[] args) {
        Simple s = new Simple(100, 200);
        s.setStationary(1);
        System.out.println("value of p,q is " + s.p + ", " + s.q +
                ", stationary is " + s.stationary);
    }

}
