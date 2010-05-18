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
package com.sun.max.tele.debug.guestvm.xen.dbchannel;

/**
 * The actual protocol expected by the Inspector, that is an extension of the simple {@link SimpleProtocol}.
 * An in-process implementation typically will implement this entire interface directly. A communication-based
 * implementation will use an adaptor to convert to the methods support by {@link SimpleProtocol}.
 *
 * @author Mick Jordan
 *
 */
public interface Protocol extends SimpleProtocol {
    /**
     * Reads bytes from the target VM into either an array or a direct {@link java.nio.ByteBuffer}.
     * @param src virtual address to read from
     * @param dst either byte array or a  {@link java.nio.ByteBuffer byte buffer} to write to
     * @param isDirectByteBuffer {@code true} if {@code dst} is a {@link java.nio.ByteBuffer byte buffer}, {@code false} if it is a byte array.
     * @param dstOffset offset in the byte buffer where writing should begin
     * @param length number of bytes to read
     * @return the number of bytes actually read
     */
    int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);
    /**
     * Writes bytes from either an array or a direct {@link java.nio.ByteBuffer} to the target VM.
     * @param dst virtual address to write to
     * @param src either byte array or a {@link java.nio.ByteBuffer byte buffer} to read from
     * @param isDirectByteBuffer {@code true} if {@code src} is a {@link java.nio.ByteBuffer byte buffer}, {@code false} if it is a byte array.
     * @param srcOffset offset in the byte buffer where reading should begin
     * @param length number of bytes to write
     * @return number of bytes actually written
     */
    int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);
    /**
     * Gathers the set of active threads in the target VM.
     * This avoids explicit types so that different versions of the Inspector types can be used on the two sides
     * of the communication channel.
     * @param teleDomain a {@link GuestVMTeleDomain} object
     * @param threads an {@link AppendableSequence<TeleNativeThread>}
     * @param threadLocalsList address of the thread locals list in the target VM
     * @param primordialThreadLocals address of the primordial thread locals in the target VM
     * @return {@code true} if the gather was successful, {@code false} otherwise.
     */
    boolean gatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList, long primordialThreadLocals);


}
