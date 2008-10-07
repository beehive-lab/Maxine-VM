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
/*VCSID=0f2dfb82-2f67-49ce-bd01-90b66f5c5e7b*/
package com.sun.max.unsafe;


public interface DataIO {

    /**
     * Reads bytes from an address into a given byte array.
     *
     * @param address the address from which reading should start
     * @param buffer the array into which the bytes are read
     * @param offset the offset in {@code buffer} at which the bytes are read
     * @param length the maximum number of bytes to be read
     * @return the number of bytes read into {@code buffer}
     * @throws DataIOError if some IO error occurs
     * @throws IndexOutOfBoundsException if {@code offset} is negative, {@code length} is negative, or
     *             {@code length > buffer.length - offset}
     */
    int read(Address address, byte[] buffer, int offset, int length) throws DataIOError;

    /**
     * Writes bytes from a given byte array to a given address.
     *
     * @param address the address at which writing should start
     * @param buffer the array from which the bytes are written
     * @param offset the offset in {@code buffer} from which the bytes are written
     * @param length the maximum number of bytes to be written
     * @return the number of bytes written to {@code address}
     * @throws DataIOError if some IO error occurs
     * @throws IndexOutOfBoundsException if {@code offset} is negative, {@code length} is negative, or
     *             {@code length > buffer.length - offset}
     */
    int write(byte[] buffer, int offset, int length, Address toAddress) throws DataIOError;

    public static class Static {
        public static byte[] readFully(DataIO dataIO, Address address, int length) {
            final byte[] buffer = new byte[length];
            readFully(dataIO, address, buffer);
            return buffer;
        }

        public static void readFully(DataIO dataIO, Address address, byte[] buffer) {
            final int length = buffer.length;
            int n = 0;
            while (n < length) {
                final int count = dataIO.read(address.plus(n), buffer, n, length - n);
                if (count <= 0) {
                    throw new DataIOError(address, (length - n) + " of " + length + " bytes unread");
                }
                n += count;
            }
        }

        /**
         * Checks the preconditions related to the destination buffer for {@link DataIO#read(Address, byte[], int, int)}.
         */
        public static void checkRead(byte[] buffer, int offset, int length) {
            if (buffer == null) {
                throw new NullPointerException();
            } else if (offset < 0 || length < 0 || length > buffer.length - offset) {
                throw new IndexOutOfBoundsException();
            }
        }

        /**
         * Checks the preconditions related to the destination buffer for {@link DataIO#write(byte[], int, int, Address)}.
         */
        public static void checkWrite(byte[] buffer, int offset, int length) {
            if ((offset < 0) || (offset > buffer.length) || (length < 0) ||
                            ((offset + length) > buffer.length) || ((offset + length) < 0)) {
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
