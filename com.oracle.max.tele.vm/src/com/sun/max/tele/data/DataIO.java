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
package com.sun.max.tele.data;

import java.nio.*;

import com.sun.max.unsafe.*;

/**
 * Buffered reading/writing of bytes from/to a source/destination that can be identified by an {@link Address}.
 */
public interface DataIO {

    /**
     * Reads bytes from an address into a given byte buffer.
     *
     * Precondition:
     * {@code buffer != null && offset >= 0 && offset < buffer.capacity() && length >= 0 && offset + length <= buffer.capacity()}
     *
     * @param src the address from which reading should start
     * @param dst the buffer into which the bytes are read
     * @param dstOffset the offset in {@code dst} at which the bytes are read
     * @param length the maximum number of bytes to be read
     * @return the number of bytes read into {@code dst}
     *
     * @throws DataIOError if some IO error occurs
     * @throws IndexOutOfBoundsException if {@code offset} is negative, {@code length} is negative, or
     *             {@code length > buffer.limit() - offset}
     */
    int read(Address src, ByteBuffer dst, int dstOffset, int length) throws DataIOError, IndexOutOfBoundsException;

    /**
     * Writes bytes from a given byte buffer to a given address.
     *
     * Precondition:
     * {@code buffer != null && offset >= 0 && offset < buffer.capacity() && length >= 0 && offset + length <= buffer.capacity()}
     *
     * @param src the buffer from which the bytes are written
     * @param srcOffset the offset in {@code src} from which the bytes are written
     * @param length the maximum number of bytes to be written
     * @param dst the address at which writing should start
     * @return the number of bytes written to {@code dst}
     *
     * @throws DataIOError if some IO error occurs
     * @throws IndexOutOfBoundsException if {@code srcOffset} is negative, {@code length} is negative, or
     *             {@code length > src.limit() - srcOffset}
     */
    int write(ByteBuffer src, int srcOffset, int length, Address dst) throws DataIOError, IndexOutOfBoundsException;

    public static class Static {

        /**
         * Fills a buffer by reading bytes from a source.
         *
         * @param dataIO the source of data to be read
         * @param src the location in the source where reading should start
         * @param dst the buffer to be filled with the data
         * @throws DataIOError if the read fails
         */
        public static void readFully(DataIO dataIO, Address src, ByteBuffer dst) throws DataIOError {
            final int length = dst.limit();
            int n = 0;
            assert dst.position() == 0;
            while (n < length) {
                final int count = dataIO.read(src.plus(n), dst, n, length - n);
                if (count <= 0) {
                    throw new DataIOError(src, (length - n) + " of " + length + " bytes unread");
                }
                n += count;
                dst.position(0);
            }
        }

        /**
         * Reads bytes from a source.
         *
         * @param dataIO the source of data to be read
         * @param src the location in the source where reading should start
         * @param length the total number of bytes to be read
         * @return the bytes read from the source.
         * @throws DataIOError if the read fails
         */
        public static byte[] readFully(DataIO dataIO, Address src, int length) throws DataIOError {
            final ByteBuffer buffer = ByteBuffer.wrap(new byte[length]);
            readFully(dataIO, src, buffer);
            return buffer.array();
        }

        /**
         * Checks the preconditions related to the destination buffer for {@link DataIO#read(Address, ByteBuffer, int, int)}.
         */
        public static void checkRead(ByteBuffer dst, int dstOffset, int length) {
            if (dst == null) {
                throw new NullPointerException();
            } else if (dstOffset < 0 || length < 0 || length > dst.limit() - dstOffset) {
                throw new IndexOutOfBoundsException();
            }
        }

        /**
         * Checks the preconditions related to the source buffer for {@link DataIO#write(ByteBuffer, int, int, Address)}.
         */
        public static void checkWrite(ByteBuffer src, int srcOffset, int length) {
            if ((srcOffset < 0) || (srcOffset > src.limit()) || (length < 0) ||
                            ((srcOffset + length) > src.limit()) || ((srcOffset + length) < 0)) {
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
