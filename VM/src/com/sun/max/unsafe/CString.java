/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */

package com.sun.max.unsafe;

import java.io.*;

import com.sun.max.memory.*;
import com.sun.max.util.*;

/**
 * Utilities for converting between Java strings and C strings (encoded as UTF8 bytes).
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CString {

    private CString() {
    }

    /**
     * Determines the length of a NULL terminated C string located in natively {@link Memory#allocate(int) allocated} memory.
     */
    public static Size length(Pointer cString) {
        Pointer p = cString;
        while (p.readByte(0) != (byte) 0) {
            p = p.plus(1);
        }
        return p.minus(cString).asSize();
    }

    /**
     * Gets the byte at given index in C string with bounds check.
     *
     * @param cString the C string
     * @param length length of C string (@see length)
     * @param _i index of byte to get
     * @return -1 if the index is out of range or the byte at the index
     */
    public static int getByte(Pointer cString, Size length, Offset index) {
        if (index.lessThan(length.asOffset())) {
            return cString.readByte(index);
        }
        return -1;
    }

    /**
     * Converts a NULL terminated C string located in natively allocated memory to a Java string.
     */
    public static String utf8ToJava(Pointer cString) throws Utf8Exception {
        final int n = length(cString).toInt();
        final byte[] bytes = new byte[n];
        Memory.readBytes(cString, n, bytes);
        return Utf8.utf8ToString(false, bytes);
    }

    /**
     * Creates a NULL terminated C string (in natively allocated} memory) from a Java string.
     * The returned C string must be deallocated by {@link Memory#deallocate()} when finished with.
     */
    public static Pointer utf8FromJava(String string) {
        final byte[] utf8 = Utf8.stringToUtf8(string);
        final Pointer cString = Memory.mustAllocate(utf8.length + 1);
        Pointer p = cString;
        for (byte utf8Char : utf8) {
            p.writeByte(0, utf8Char);
            p = p.plus(1);
        }
        p.writeByte(0, (byte) 0);
        return cString;
    }

    public static byte[] read(InputStream stream) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            final int ch = stream.read();
            if (ch < 0) {
                throw new IOException();
            }
            buffer.write(ch);
            if (ch == 0) {
                return buffer.toByteArray();
            }
        }
    }

    /**
     * Fills a given buffer with a zero-terminated sequence of bytes from a source buffer.
     *
     * @param source the byte array containing the source bytes
     * @param start the start offset in {@code source} of the bytes to be written (inclusive)
     * @param end the end offset of {@code source} of the bytes to be written (exclusive)
     * @param buffer a pointer to the beginning of the buffer
     * @param bufferSize the size of the buffer
     * @return an index into the next byte to be written which is start <= result <= end
     */
    public static int writeBytes(byte[] source, int start, int end, Pointer buffer, int bufferSize) {
        final int n = Math.min(bufferSize - 1, end - start);
        for (int i = 0; i < n; i++) {
            buffer.writeByte(i, source[start + i]);
        }
        buffer.writeByte(n, (byte) 0);
        return start + n;
    }

    /**
     * Fills a given buffer with the bytes in the UTF8 representation of a string following by a terminating zero. The
     * maximum number of bytes written to the buffer is limited to the number of leading characters of {@code string}
     * that can be completely encoded in {@code bufferSize - 2} bytes.
     *
     * @param string the String to write to the buffer
     * @param buffer a pointer to the beginning of the buffer
     * @param bufferSize the size of the buffer
     * @return a pointer to the position in the buffer following the terminating zero character
     */
    public static Pointer writeUtf8(final String string, final Pointer buffer, final int bufferSize) {
        // TODO: factor out encoding of UTF8 characters from this and the following methods
        int position = 0;
        final int endPosition = bufferSize - 1;
        for (int i = 0; i < string.length(); i++) {
            final char ch = string.charAt(i);
            if ((ch >= 0x0001) && (ch <= 0x007F)) {
                if (position >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) ch);
            } else if (ch > 0x07FF) {
                if (position + 2 >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) (0xe0 | (byte) (ch >> 12)));
                buffer.writeByte(position++, (byte) (0x80 | ((ch & 0xfc0) >> 6)));
                buffer.writeByte(position++, (byte) (0x80 | (ch & 0x3f)));
            } else {
                if (position + 1 >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) (0xc0 | (byte) (ch >> 6)));
                buffer.writeByte(position++, (byte) (0x80 | (ch & 0x3f)));
            }
        }
        buffer.writeByte(position, (byte) 0);
        return buffer.plus(position + 1);
    }

    /**
     * Fills a given buffer with the bytes in the UTF8 representation of a string following by a terminating zero. The
     * maximum number of bytes written to the buffer is limited to the number of leading characters of {@code string}
     * that can be completely encoded in {@code bufferSize - 2} bytes.
     *
     * @param string the String to write to the buffer
     * @param start the index of the character in {@code string} from which to start copying
     * @param buffer a pointer to the beginning of the buffer
     * @param bufferSize the size of the buffer
     * @return the number of characters from {@code string} written to the buffer
     */
    public static int writePartialUtf8(final char[] chars, final int start, final Pointer buffer, final int bufferSize) {
        int position = 0;
        final int endPosition = bufferSize - 1;
        int i = start;
        while (i < chars.length) {
            final char ch = chars[i];
            if ((ch >= 0x0001) && (ch <= 0x007F)) {
                if (position >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) ch);
            } else if (ch > 0x07FF) {
                if (position + 2 >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) (0xe0 | (byte) (ch >> 12)));
                buffer.writeByte(position++, (byte) (0x80 | ((ch & 0xfc0) >> 6)));
                buffer.writeByte(position++, (byte) (0x80 | (ch & 0x3f)));
            } else {
                if (position + 1 >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) (0xc0 | (byte) (ch >> 6)));
                buffer.writeByte(position++, (byte) (0x80 | (ch & 0x3f)));
            }
            i++;
        }
        buffer.writeByte(position, (byte) 0);
        return i;
    }

    /**
     * Fills a given buffer with the bytes in the UTF8 representation of a string following by a terminating zero. The
     * maximum number of bytes written to the buffer is limited to the number of leading characters of {@code string}
     * that can be completely encoded in {@code bufferSize - 2} bytes.
     *
     * @param string the String to write to the buffer
     * @param start the index of the character in {@code string} from which to start copying
     * @param buffer a pointer to the beginning of the buffer
     * @param bufferSize the size of the buffer
     * @return the number of characters from {@code string} written to the buffer
     */
    public static int writePartialUtf8(final String string, final int start, final Pointer buffer, final int bufferSize) {
        int position = 0;
        final int endPosition = bufferSize - 1;
        int i = start;
        while (i < string.length()) {
            final char ch = string.charAt(i);
            if ((ch >= 0x0001) && (ch <= 0x007F)) {
                if (position >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) ch);
            } else if (ch > 0x07FF) {
                if (position + 2 >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) (0xe0 | (byte) (ch >> 12)));
                buffer.writeByte(position++, (byte) (0x80 | ((ch & 0xfc0) >> 6)));
                buffer.writeByte(position++, (byte) (0x80 | (ch & 0x3f)));
            } else {
                if (position + 1 >= endPosition) {
                    break;
                }
                buffer.writeByte(position++, (byte) (0xc0 | (byte) (ch >> 6)));
                buffer.writeByte(position++, (byte) (0x80 | (ch & 0x3f)));
            }
            i++;
        }
        buffer.writeByte(position, (byte) 0);
        return i;
    }

    public static byte[] toByteArray(Pointer start, int length) {
        final byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = start.getByte(i);
        }
        return buffer;
    }

    public static int parseUnsignedInt(Pointer pointer) {
        int result = 0;
        Pointer ptr = pointer;
        while (true) {
            final char ch = (char) ptr.getByte();
            if (ch == 0) {
                break;
            }
            if (ch >= '0' && ch <= '9') {
                result *= 10;
                result += ch - '0';
            } else {
                return -1;
            }
            ptr = ptr.plus(1);
        }
        return result;
    }

    public static boolean equals(Pointer cstring, String string) {
        if (cstring.isZero()) {
            return false;
        }
        for (int i = 0; i < string.length(); i++) {
            final byte ch = cstring.getByte(i);
            if (ch == 0 || ch != string.charAt(i)) {
                return false;
            }
        }
        return cstring.getByte(string.length()) == 0;
    }

    public static boolean startsWith(Pointer cstring, String prefix) {
        if (cstring.isZero()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            final byte ch = cstring.getByte(i);
            if (ch == 0 || ch != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
