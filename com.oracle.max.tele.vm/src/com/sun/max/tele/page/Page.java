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
package com.sun.max.tele.page;

import java.nio.*;

import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.TargetLocation.Kind;
import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * A cached page of remote memory contents.
 *
 * To avoid double buffering of {@code byte} arrays in the native code that copies bytes from the VM address space, NIO
 * {@linkplain ByteBuffer#isDirect() direct} {@link ByteBuffer}s are used, unless the target VM is remote. The buffer for each page is
 * allocated from a global buffer until the global buffer is exhausted. If the target VM is remote or the
 * global buffer has been exhausted, then the buffer for each page is a heap allocated byte array.
 */
public class Page {

    /**
     * Access to memory in the {@link TeleVM}.
     */
    private TeleIO teleIO;

    /**
     * Generation count of remote memory modification as of the last time this page was refreshed.
     */
    private long epoch = -1;

    private final long index;

    /**
     * The buffer for this page.
     */
    private final ByteBuffer buffer;

    private static final long DEFAULT_GLOBAL_DIRECTBUFFER_POOL_SIZE = 100 * 1024 * 1024;

    public static final long globalDirectBufferPoolSize;
    static {
        long size = DEFAULT_GLOBAL_DIRECTBUFFER_POOL_SIZE;
        final String value = System.getProperty("max.tele.page.directBufferPoolSize");
        if (value != null) {
            try {
                size = Long.parseLong(value);
            } catch (NumberFormatException numberFormatException) {
                TeleWarning.message("Malformed value for the \"max.tele.page.directBuffersPoolSize\" property", numberFormatException);
            }
        }
        globalDirectBufferPoolSize = size;
    }

    private static ByteBuffer globalBuffer;

    /**
     * Allocates the buffer for a page according to whether or not {@linkplain Page#noDirectBuffers direct buffers}
     * are being used.
     */
    private static synchronized ByteBuffer allocate(TeleIO teleIO, ByteOrder byteOrder, long index) {
        final int pageSize = teleIO.pageSize();
        if (useDirectBuffers()) {
            if (globalBuffer == null) {
                globalBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 100).order(byteOrder);
            }
            if (globalBuffer.remaining() >= pageSize) {
                final ByteBuffer buffer = globalBuffer.slice().order(byteOrder);
                globalBuffer.position(globalBuffer.position() + pageSize);
                buffer.limit(pageSize);
                return buffer;
            }
        }
        // Not using direct buffers or global buffer is exhausted
        return ByteBuffer.allocate(pageSize).order(byteOrder);
    }

    /**
     * Decide whether to use direct buffers.
     * It is counter-productive to use them if the target VM is remote.
     * @return
     */
    private static boolean useDirectBuffers() {
        return TeleVM.targetLocation().kind != Kind.REMOTE;
    }

    public Page(TeleIO teleIO, long index, ByteOrder byteOrder) {
        this.teleIO = teleIO;
        this.index = index;
        this.buffer = allocate(teleIO, byteOrder, index);
    }

    /**
     * @return size of the page in bytes.
     */
    public int size() {
        return teleIO.pageSize();
    }

    /**
     * @return starting location of the page in remote memory.
     */
    public Address address() {
        return Address.fromLong(index * size());
    }

    /**
     * Mark page contents as needful of refreshing, independent of any prior reads.
     */
    public void invalidate() {
        epoch = -1;
    }

    /**
     * Reads into the cache the contents of the remote memory page.
     *
     * @throws DataIOError
     */
    private void refreshRead() throws DataIOError {
        if (epoch < teleIO.epoch()) {
            try {
                DataIO.Static.readFully(teleIO, address(), buffer);
                epoch = teleIO.epoch();
            } catch (DataIOError e) {
                if (!(e instanceof ConcurrentDataIOError)) {
                    TeleWarning.message(e);
                }
                throw e;
            } catch (TerminatedProcessIOException e) {
            }
        }
    }

    public byte readByte(int offset) throws DataIOError {
        refreshRead();
        return buffer.get(offset);
    }

    public short readShort(int offset) {
        refreshRead();
        return buffer.getShort(offset);
    }

    public int readInt(int offset) {
        refreshRead();
        final int result = buffer.getInt(offset);
        return result;
    }

    public long readLong(int offset) {
        refreshRead();
        final long result = buffer.getLong(offset);
        return result;
    }

    /**
     * Transfers {@code n} bytes from this page to a given buffer where
     * {@code n == min(dst.limit() - dstOffset, size() - offset)}.
     *
     * @param offset the offset in this page from which to start reading
     * @param dst the buffer into which the bytes will be copied
     * @param dstOffset the offset in {@code dst} at which to start writing
     * @return the number of bytes copied
     */
    public int readBytes(int offset, ByteBuffer dst, int dstOffset) throws DataIOError {
        refreshRead();

        final int n = Math.min(dst.limit() - dstOffset, size() - offset);

        final ByteBuffer srcSlice = buffer.duplicate();
        final ByteBuffer dstSlice = dst.duplicate();

        srcSlice.position(offset).limit(offset + n);
        dstSlice.position(dstOffset).limit(dstOffset + n);

        dstSlice.put(srcSlice);
        return n;
    }
}
