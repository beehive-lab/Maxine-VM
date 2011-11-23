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
package com.sun.max.tele.debug.no;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

import com.sun.max.platform.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.Params;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.hosted.BootImage.Header;

/**
 * A null process that "contains" the boot image for inspection, as if it were a VM.
 */
public final class ReadOnlyTeleProcess extends TeleProcess {
    private final DataAccess dataAccess;
    private final Pointer heapPointer;

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    public ReadOnlyTeleProcess(TeleVM vm, Platform platform, File programFile) throws BootImageException {
        super(vm, platform, ProcessState.UNKNOWN);
        this.heapPointer = Pointer.fromLong(VmHeapAccess.heapAddressOption());
        try {
            dataAccess = map(vm.bootImageFile(), vm.bootImage());
        } catch (IOException ioException) {
            throw new BootImageException("Error mapping in boot image", ioException);
        }
    }

    @Override
    public void updateCache(long epoch) {
    }

    @Override
    public int platformWatchpointCount() {
        return 0;
    }

    public Pointer heapPointer() {
        return heapPointer;
    }

    /**
     * Maps the heapPointer and code sections of the boot image in a given file into memory.
     *
     * @param bootImageFile the file containing the heapPointer and code sections to map into memory
     * @return a {@link DataAccess} object that can be used to access the mapped sections
     * @throws IOException if an IO error occurs while performing the memory mapping
     */
    private DataAccess map(File bootImageFile, BootImage bootImage) throws IOException {
        final RandomAccessFile randomAccessFile = new RandomAccessFile(bootImageFile, "rwd");
        final Header header = bootImage.header;
        int heapOffset = bootImage.heapOffset();
        int heapAndCodeSize = header.heapSize + header.codeSize;
        final MappedByteBuffer bootImageBuffer = randomAccessFile.getChannel().map(MapMode.PRIVATE, heapOffset, heapAndCodeSize);
        bootImageBuffer.order(platform().endianness().asByteOrder());
        randomAccessFile.close();

        if (heapPointer.isNotZero()) {
            long address = (Long) WithoutAccessCheck.getInstanceField(bootImageBuffer, "address");
            bootImage.relocate(address, heapPointer);
        }
        return new MappedByteBufferDataAccess(bootImageBuffer, heapPointer, header.wordWidth());
    }

    private static final String FAIL_MESSAGE = "Attempt to run/write/modify a read-only bootimage VM with no live process";

    @Override
    protected void gatherThreads(List<TeleNativeThread> threads) {
        TeleError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(Params params) {
        TeleError.unexpected(FAIL_MESSAGE);
        return null;
    }

    @Override
    protected int read0(Address address, ByteBuffer buffer, int offset, int length) {
        return dataAccess.read(address, buffer, offset, length);
    }

    @Override
    protected int write0(ByteBuffer buffer, int offset, int length, Address address) {
        TeleError.unexpected(FAIL_MESSAGE);
        return 0;
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        TeleError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        TeleError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        TeleError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected ProcessState waitUntilStopped() {
        TeleError.unexpected(FAIL_MESSAGE);
        return ProcessState.UNKNOWN;
    }
}
