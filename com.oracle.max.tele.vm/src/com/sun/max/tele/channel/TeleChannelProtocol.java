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
package com.sun.max.tele.channel;

import java.nio.*;
import java.util.*;

import com.sun.max.tele.channel.agent.TeleNativeThread;
import com.sun.max.tele.debug.*;

/**
 * The actual protocol expected by the Inspector, that is an extension of the simple {@link TeleChannelDataIOProtocol},
 * and uses some object types.
 *
 * An in-process implementation typically will implement this entire interface directly. A communication-based
 * implementation will use an adaptor to convert to the methods support by {@link TeleChannelDataIOProtocol}.
 */
public interface TeleChannelProtocol extends TeleChannelDataIOProtocol {
    /**
     * Reads bytes from the target VM into a (likely direct) {@link java.nio.ByteBuffer}.
     * @param src virtual address to read from
     * @param dst either byte array or a  {@link java.nio.ByteBuffer byte buffer} to write to
     * @param dstOffset offset in the byte buffer where writing should begin
     * @param length number of bytes to read
     * @return the number of bytes actually read
     */
    int readBytes(long src, ByteBuffer dst, int dstOffset, int length);
    /**
     * Writes bytes from a (likely direct) {@link java.nio.ByteBuffer} to the target VM.
     * @param dst virtual address to write to
     * @param src either byte array or a {@link java.nio.ByteBuffer byte buffer} to read from
     * @param srcOffset offset in the byte buffer where reading should begin
     * @param length number of bytes to write
     * @return number of bytes actually written
     */
    int writeBytes(long dst, ByteBuffer src, int srcOffset, int length);
    /**
     * Gathers the set of active threads in the target VM.
     * This avoids explicit types so that different versions of the Inspector types can be used on the two sides
     * of the communication channel.
     * @param teleDomain a {@link MaxVETeleDomain} object
     * @param threadList a {@link List} of {@link TeleNativeThread} objects
     * @param tlaList address of the thread locals list in the target VM
     * @return {@code true} if the gather was successful, {@code false} otherwise.
     */
    boolean gatherThreads(Object teleDomain, Object threadList, long tlaList);

    /**
     * Wait until the target VM is stopped.
     * @return The {@link ProcessState} when stopped.
     */
    ProcessState waitUntilStopped();


}
