/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
/**
 * Copyright (c) 2006, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created May 27, 2006
 */
package com.sun.max.elf;

/**
 * The <code>CharUtil</code> class includes a number of useful utilities in dealing
 * with characters. For example, testing whether a character is within a certain set,
 * converting between characters and digits in various base systems, etc.
 *
 * @author Ben L. Titzer
 */
public final class CharUtil {
    public static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static final char[] LOW_HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private CharUtil() {
    }

    /**
     * The <code>isHexDigit()</code> method checks whether the specified character
     * represents a valid hexadecimal character. This method is case-insensitive.
     *
     * @param c the character to check
     * @return true if the specified character is a valid hexadecimal value; false
     *         otherwise
     */
    public static boolean isHexDigit(char c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return true;
        }
        return false;
    }

    public static char toUpperHexChar(int digitValue) {
        if (digitValue < 10) {
            return (char) (digitValue | '0');
        }
        return (char) ('A' + digitValue - 10);
    }

    public static char toLowerHexChar(int digitValue) {
        if (digitValue < 10) {
            return (char) (digitValue | '0');
        }
        return (char) ('a' + digitValue - 10);
    }

    /**
     * The <code>hexValueOf()</code> method converts a character into an integer
     * value according to the hexadecimal base system.
     *
     * @param c the character to convert
     * @return the value of the character in the hexadecimal base system
     */
    public static int hexValueOf(char c) {
        return Character.digit(c, 16);
    }

    /**
     * The <code>isDecDigit()</code> method checks whether the specified character
     * represents a valid decimal character. .
     *
     * @param c the character to check
     * @return true if the specified character is a valid decimal digit; false
     *         otherwise
     */
    public static boolean isDecDigit(char c) {
        if (c < '0') {
            return false;
        }
        return c <= '9';
    }

    /**
     * The <code>decValueOf()</code> method converts a character into an integer
     * value according to the decimal base system.
     *
     * @param c the character to convert
     * @return the value of the character in the decimal base system
     */
    public static int decValueOf(char c) {
        return Character.digit(c, 10);
    }

    /**
     * The <code>isOctDigit()</code> method checks whether the specified character
     * represents a valid octal character.
     *
     * @param c the character to check
     * @return true if the specified character is a valid octal digit; false
     *         otherwise
     */
    public static boolean isOctDigit(char c) {
        if (c < '0') {
            return false;
        }
        return c <= '7';
    }

    /**
     * The <code>octValueOf()</code> method converts a character into an integer
     * value according to the octal base system.
     *
     * @param c the character to convert
     * @return the value of the character in the octal base system
     */
    public static int octValueOf(char c) {
        return Character.digit(c, 8);
    }

    /**
     * The <code>isBinDigit()</code> method checks whether the specified character
     * represents a valid binary character.
     *
     * @param c the character to check
     * @return true if the specified character is a valid binary digit; false
     *         otherwise
     */
    public static boolean isBinDigit(char c) {
        return c == '0' || c == '1';
    }

    /**
     * The <code>binValueOf()</code> method converts a character into an integer
     * value according to the binary base system.
     *
     * @param c the character to convert
     * @return the value of the character in the binary base system
     */
    public static int binValueOf(char c) {
        return c == '0' ? 0 : 1;
    }

    public static char alpha(int num) {
        return (char) ('a' + num - 1);
    }

}
