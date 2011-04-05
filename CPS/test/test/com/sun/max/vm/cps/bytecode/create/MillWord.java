/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.bytecode.create;

/**
 * @author Bernd Mathiske
 */
public final class MillWord {
    private MillWord() {
    }

    /**
     * Get least significant byte number 3, counting from 0.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte3(int i) {
        return (byte) ((i >> 24) & 0xff);
    }

    /**
     * Get least significant byte number 2, counting from 0.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte2(int i) {
        return (byte) ((i >> 16) & 0xff);
    }

    /**
     * Get least significant byte number 1, counting from 0.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte1(int i) {
        return (byte) ((i >> 8) & 0xff);
    }

    /**
     * Get the least significant byte.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte0(int i) {
        return (byte) (i & 0xff);
    }

}
