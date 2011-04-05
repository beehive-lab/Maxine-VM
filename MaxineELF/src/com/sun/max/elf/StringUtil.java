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
 * Copyright (c) 2004-2005, Regents of the University of California
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
 */

package com.sun.max.elf;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import com.sun.max.program.ProgramError;

/**
 * The <code>StringUtil</code> class implements several useful functions for dealing with strings such as
 * parsing pieces of syntax, formatting, etc.
 *
 * @author Ben L. Titzer
 */
public final class StringUtil {
    public static final String QUOTE = "\"";
    public static final String SQUOTE = "'";
    public static final String LPAREN = "(";
    public static final String RPAREN = ")";
    public static final String COMMA = ",";
    public static final String COMMA_SPACE = ", ";
    public static final String[] EMPTY_STRING_ARRAY = {};
    public static final char SQUOTE_CHAR = '\'';
    public static final char BACKSLASH = '\\';
    public static final char QUOTE_CHAR = '"';

    private StringUtil() {
    }

    /**
     * The <code>addToString()</code> method converts a numerical address (represented as a signed 32-bit
     * integer) and converts it to a string in the format 0xXXXX where 'X' represents a hexadecimal character.
     * The address is assumed to fit in 4 hexadecimal characters. If it does not, the string will have as many
     * characters as necessary (max 8) to represent the address.
     *
     * @param address the address value as an integer
     * @return a standard string representation of the address
     */
    public static String addrToString(int address) {
        return to0xHex(address, 4);
    }

    public static int readHexValue(CharacterIterator i, int maxchars) {
        int accumul = 0;

        for (int cntr = 0; cntr < maxchars; cntr++) {
            final char c = i.current();

            if (c == CharacterIterator.DONE || !isHexDigit(c)) {
                break;
            }

            accumul = (accumul << 4) | hexValueOf(c);
            i.next();
        }

        return accumul;
    }

    public static int readOctalValue(CharacterIterator i, int maxchars) {
        int accumul = 0;

        for (int cntr = 0; cntr < maxchars; cntr++) {
            final char c = i.current();

            if (!isOctalDigit(c)) {
                break;
            }

            accumul = (accumul << 3) | octalValueOf(c);
            i.next();
        }

        return accumul;
    }

    public static int readBinaryValue(CharacterIterator i, int maxchars) {
        int accumul = 0;

        if (maxchars >= 1) {
            final char ch = i.current();
            i.next();
            if (ch == '0') {
                accumul <<= 1;
            } else if (ch == '1') {
                accumul = (accumul << 1) | 1;
            }
        }

        return accumul;
    }

    public static int readDecimalValue(CharacterIterator i, int maxchars) {
        final StringBuffer buf = new StringBuffer();

        if (peekAndEat(i, '-')) {
            buf.append('-');
        }

        for (int cntr = 0; cntr < maxchars; cntr++) {
            final char c = i.current();

            if (!Character.isDigit(c)) {
                break;
            }

            buf.append(c);
            i.next();
        }

        return Integer.parseInt(buf.toString());
    }

    public static String readDecimalString(CharacterIterator i, int maxchars) {
        final StringBuffer buf = new StringBuffer();

        if (peekAndEat(i, '-')) {
            buf.append('-');
        }

        for (int cntr = 0; cntr < maxchars; cntr++) {
            final char c = i.current();

            if (!Character.isDigit(c)) {
                break;
            }

            buf.append(c);
            i.next();
        }

        return buf.toString();
    }

    public static int readIntegerValue(CharacterIterator i) {
        char ch = i.current();
        if (ch == '-') {
            return readDecimalValue(i, 10);
        }
        if (ch == '0') {
            ch = i.next();
            if (ch == 'x' || ch == 'X') {
                i.next();
                return readHexValue(i, 8);
            } else if (ch == 'b' || ch == 'B') {
                i.next();
                return readBinaryValue(i, 32);
            } else {
                return readOctalValue(i, 11);
            }
        }
        return readDecimalValue(i, 10);
    }

    public static void skipWhiteSpace(CharacterIterator i) {
        while (true) {
            final char c = i.current();
            if (c != ' ' && c != '\n' && c != '\t') {
                break;
            }
            i.next();
        }
    }

    public static char peek(CharacterIterator i) {
        return i.current();
    }

    public static boolean peekAndEat(CharacterIterator i, char c) {
        final char r = i.current();
        if (r == c) {
            i.next();
            return true;
        }
        return false;
    }

    public static boolean peekAndEat(CharacterIterator i, String s) {
        final int ind = i.getIndex();
        for (int cntr = 0; cntr < s.length(); cntr++) {
            if (i.current() == s.charAt(cntr)) {
                i.next();
            } else {
                i.setIndex(ind);
                return false;
            }
        }
        return true;
    }

    public static void expectChar(CharacterIterator i, char c) throws Exception {
        final char r = i.current();
        i.next();
        if (r != c) {
            ProgramError.unexpected("parse error at " + i.getIndex() + ", expected character '" + c + "'");
        }
    }

    public static void expectChars(CharacterIterator i, String s) throws Exception {
        for (int cntr = 0; cntr < s.length(); cntr++) {
            expectChar(i, s.charAt(cntr));
        }
    }

    /**
     * The <code>isHex()</code> method checks whether the specifed string represents a hexadecimal
     * integer. This method only checks the first two characters. If they match "0x" or "0X", then
     * this method returns true, otherwise, it returns false.
     *
     * @param s the string to check whether it begins with a hexadecimal sequence
     * @return true if the string begins with "0x" or "0X"; false otherwise
     */
    public static boolean isHex(String s) {
        if (s.length() < 2) {
            return false;
        }
        final char c = s.charAt(1);
        return s.charAt(0) == '0' && (c == 'x' || c == 'X');
    }

    /**
     * The <code>isBin()</code> method checks whether the specifed string represents a binary
     * integer. This method only checks the first two characters. If they match "0b" or "0B", then
     * this method returns true, otherwise, it returns false.
     *
     * @param s the string to check whether it begins with a hexadecimal sequence
     * @return true if the string begins with "0b" or "0B"; false otherwise
     */
    public static boolean isBin(String s) {
        if (s.length() < 2) {
            return false;
        }
        final char c = s.charAt(1);
        return s.charAt(0) == '0' && (c == 'b' || c == 'B');
    }

    /**
     * The <code>isHexDigit()</code> method tests whether the given character corresponds to one of the
     * characters used in the hexadecimal representation (i.e. is '0'-'9' or 'a'-'b', case insensitive. This
     * method is generally used in parsing and lexing of input.
     *
     * @param c the character to test
     * @return true if this character is a hexadecimal digit; false otherwise
     */
    public static boolean isHexDigit(char c) {
        return CharUtil.isHexDigit(c);
    }

    public static int hexValueOf(char c) {
        return CharUtil.hexValueOf(c);
    }

    public static int octalValueOf(char c) {
        return CharUtil.octValueOf(c);
    }

    public static boolean isOctalDigit(char c) {
        return CharUtil.isOctDigit(c);
    }

    /**
     * The <code>justify()</code> method justifies a string to either the right or left margin
     * by inserting spaces to pad the string to a specific width. This is useful in printing out
     * values in a columnar (aligned) format. This version of the method accepts a string buffer
     * into which to put the string.
     * @param right a parameter determining whether to justify to the right margin. If this parameter
     * is true, the padding spaces will be inserted on the left, before the string.
     * @param buf the string buffer into which to write the padded string
     * @param s the string to justify
     * @param width the width (in characters) to which to justify the string
     */
    public static void justify(boolean right, StringBuffer buf, String s, int width) {
        final int pad = width - s.length();
        if (right) {
            space(buf, pad);
            buf.append(s);
        } else {
            buf.append(s);
            space(buf, pad);
        }
    }

    public static void justify(boolean right, StringBuffer buf, long l, int width) {
        justify(right, buf, Long.toString(l), width);
    }

    public static void justify(boolean right, StringBuffer buf, float f, int width) {
        justify(right, buf, Float.toString(f), width);
    }

    /**
     * The <code>justify()</code> method justifies a string to either the right or left margin
     * by inserting spaces to pad the string to a specific width. This is useful in printing out
     * values in a columnar (aligned) format.
     * @param right a parameter determining whether to justify to the right margin. If this parameter
     * is true, the padding spaces will be inserted on the left, before the string.
     * @param s the string to justify
     * @param width the width (in characters) to which to justify the string
     * @return a new string with padding inserted
     */
    public static String justify(boolean right, String s, int width) {
        // if the string is too wide, return the original
        if (width - s.length() <= 0) {
            return s;
        }
        // otherwise, adjust with padding
        final StringBuffer buf = new StringBuffer(width);
        justify(right, buf, s, width);
        return buf.toString();
    }

    public static String justify(boolean right, long l, int width) {
        return justify(right, Long.toString(l), width);
    }

    public static String justify(boolean right, float f, int width) {
        return justify(right, Float.toString(f), width);
    }

    /**
     * The <code>leftJustify()</code> method pads a string to a specified length by adding spaces on the
     * right, thus justifying the string to the left margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a long value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the right with spaces to achieve the desired
     *         length.
     */
    public static String leftJustify(long v, int width) {
        return justify(false, v, width);
    }

    /**
     * The <code>leftJustify()</code> method pads a string to a specified length by adding spaces on the
     * right, thus justifying the string to the left margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a floating point value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the right with spaces to achieve the desired
     *         length.
     */
    public static String leftJustify(float v, int width) {
        return justify(false, v, width);
    }

    /**
     * The <code>leftJustify()</code> method pads a string to a specified length by adding spaces on the
     * right, thus justifying the string to the left margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param s     a string to justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the right with spaces to achieve the desired
     *         length.
     */
    public static String leftJustify(String s, int width) {
        return justify(false, s, width);
    }

    /**
     * The <code>rightJustify()</code> method pads a string to a specified length by adding spaces on the
     * left, thus justifying the string to the right margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a long value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the left with spaces to achieve the desired
     *         length.
     */
    public static String rightJustify(long v, int width) {
        return justify(true, v, width);
    }

    /**
     * The <code>rightJustify()</code> method pads a string to a specified length by adding spaces on the
     * left, thus justifying the string to the right margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a floating point value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the left with spaces to achieve the desired
     *         length.
     */
    public static String rightJustify(float v, int width) {
        return justify(true, v, width);
    }

    /**
     * The <code>rightJustify()</code> method pads a string to a specified length by adding spaces on the
     * left, thus justifying the string to the right margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param s     a string to justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the left with spaces to achieve the desired
     *         length.
     */
    public static String rightJustify(String s, int width) {
        return justify(true, s, width);
    }

    /**
     * The <code>toHex()</code> converts the specified long value into a hexadecimal string of the given with.
     * The value will be padded on the left with zero values to achieve the desired with.
     *
     * @param value the long value to convert to a string
     * @param width the desired length of the string
     * @return a hexadecimal string representation of the given value, padded on the left with zeroes to the
     *         length specified
     */
    public static String toHex(long value, int width) {
        return convertToHex(value, width, 0, new char[width], CharUtil.HEX_CHARS);
    }

    public static String toLowHex(long value, int width) {
        return convertToHex(value, width, 0, new char[width], CharUtil.LOW_HEX_CHARS);
    }

    private static String convertToHex(long value, int width, int start, char[] result, char[] hexChars) {
        if (width < 16 && value > (long) 1 << width * 4) {
            final StringBuffer buf = new StringBuffer();
            for (int cntr = 0; cntr < start; cntr++) {
                buf.append(result[cntr]);
            }
            buf.append(Long.toHexString(value).toUpperCase());
            return buf.toString();
        }

        final int i = start + width - 1;
        for (int cntr = 0; cntr < width; cntr++) {
            result[i - cntr] = hexChars[(int) (value >> (cntr * 4)) & 0xf];
        }

        return new String(result);
    }

    public static String to0xHex(long value, int width) {
        final char[] result = new char[width + 2];
        result[0] = '0';
        result[1] = 'x';
        return convertToHex(value, width, 2, result, CharUtil.HEX_CHARS);
    }

    public static String toBin(long value, int width) {
        final char[] result = new char[width];

        for (int cntr = 0; cntr < width; cntr++) {
            result[width - cntr - 1] = (value & (0x1 << cntr)) == 0 ? '0' : '1';
        }
        return new String(result);
    }

    public static void toHex(StringBuffer buf, long value, int width) {
        if (value > (long) 1 << width * 4) {
            buf.append(Long.toHexString(value).toUpperCase());
            return;
        }

        for (int cntr = width - 1; cntr >= 0; cntr--) {
            buf.append(CharUtil.HEX_CHARS[(int) (value >> (cntr * 4)) & 0xf]);
        }
    }

    public static int evaluateIntegerLiteral(String val) {
        return readIntegerValue(new StringCharacterIterator(val));
    }

    public static String formatParagraphs(String s, int leftJust, int pindent, int width) {
        final int len = s.length();
        int indent = pindent;
        indent += leftJust;
        int consumed = indent + leftJust;
        final String indstr = space(indent);
        final String ljstr = space(leftJust);
        final StringBuffer buf = new StringBuffer(s.length() + 50);
        buf.append(indstr);
        int lastSp = -1;
        for (int cntr = 0; cntr < len; cntr++) {
            final char c = s.charAt(cntr);
            if (c == '\n') {
                buf.append('\n');
                consumed = indent;
                buf.append(indstr);
                continue;
            } else if (Character.isWhitespace(c)) {
                lastSp = buf.length();
            }
            buf.append(c);
            consumed++;

            if (consumed > width) {
                if (lastSp >= 0) {
                    buf.setCharAt(lastSp, '\n');
                    buf.insert(lastSp + 1, ljstr);
                    consumed = buf.length() - lastSp + leftJust - 1;
                }
            }
        }
        return buf.toString();
    }

    /**
     * The <code>dup()</code> method takes a character and a count and returns a string where that character
     * has been duplicated the specified number of times.
     *
     * @param c   the character to duplicate
     * @param len the number of times to duplicate the character
     * @return a string representation of the particular character duplicated the specified number of times
     */
    public static String dup(char c, int len) {
        final StringBuffer buf = new StringBuffer(len);
        for (int cntr = 0; cntr < len; cntr++) {
            buf.append(c);
        }
        return buf.toString();
    }

    protected static final String[] spacers = {
        "",            // 0
        " ",           // 1
        "  ",          // 2
        "   ",         // 3
        "    ",        // 4
        "     ",       // 5
        "      ",      // 6
        "       ",     // 7
        "        ",    // 8
        "         ",    // 9
        "          ",  // 10
    };

    public static String space(int len) {
        if (len <= 0) {
            return "";
        }
        if (len < spacers.length) {
            return spacers[len];
        }
        return dup(' ', len);
    }

    public static void space(StringBuffer buf, int len) {
        int i = 0;
        while (i++ < len) {
            buf.append(' ');
        }
    }

    public static String toDecimal(long value, int places) {
        int fract = places;
        long val = value;
        final StringBuffer buf = new StringBuffer(10 + fract);
        while (fract > 0) {
            buf.append(val % 10);
            fract--;
            val = val / 10;
            if (fract == 0) {
                buf.append('.');
            }
        }
        buf.reverse();
        return val + buf.toString();

    }

    public static char toBit(boolean f) {
        return f ? '1' : '0';
    }

    public static char[] getStringChars(String str) {
        final char[] val = new char[str.length()];
        str.getChars(0, val.length, val, 0);
        return val;
    }
}
