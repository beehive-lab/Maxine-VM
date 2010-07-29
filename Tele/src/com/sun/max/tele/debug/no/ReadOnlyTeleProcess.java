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
package com.sun.max.tele.debug.no;

import java.io.*;
import java.math.*;
import java.nio.*;
import java.nio.channels.FileChannel.*;
import java.util.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.prototype.BootImage.*;

/**
 * A null process that "contains" the boot image for inspection, as if it were a VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class ReadOnlyTeleProcess extends TeleProcess {

    /**
     * Name of system property that specifies the address to which the read-only heapPointer should be relocated.
     */
    public static final String HEAP_PROPERTY = "max.heap";

    private final DataAccess dataAccess;
    private final Pointer heapPointer;

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    public ReadOnlyTeleProcess(TeleVM teleVM, Platform platform, File programFile) throws BootImageException {
        super(teleVM, platform, ProcessState.UNKNOWN);
        long heap = 0L;
        String heapValue = System.getProperty(HEAP_PROPERTY);
        if (heapValue != null) {
            try {
                int radix = 10;
                if (heapValue.startsWith("0x")) {
                    radix = 16;
                    heapValue = heapValue.substring(2);
                }
                // Use BigInteger to handle unsigned 64-bit values that are greater than Long.MAX_VALUE
                BigInteger bi = new BigInteger(heapValue, radix);
                heap = bi.longValue();
            } catch (NumberFormatException e) {
                throw new BootImageException("Error parsing value of " + HEAP_PROPERTY + " system property: " + heapValue, e);
            }
        }
        this.heapPointer = Pointer.fromLong(heap);
        try {
            dataAccess = map(teleVM.bootImageFile(), teleVM.bootImage());
        } catch (IOException ioException) {
            throw new BootImageException("Error mapping in boot image", ioException);
        }
    }

    @Override
    public void updateCache() {
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
        bootImageBuffer.order(bootImage.vmConfiguration.platform().processorKind.dataModel.endianness.asByteOrder());
        randomAccessFile.close();

        if (!heapPointer.isZero()) {
            long address = (Long) WithoutAccessCheck.getInstanceField(bootImageBuffer, "address");
            bootImage.relocate(address, heapPointer);
        }
        return new MappedByteBufferDataAccess(bootImageBuffer, heapPointer, header.wordWidth());
    }

    private static final String FAIL_MESSAGE = "Attempt to run/write/modify a read-only bootimage VM with no live process";

    @Override
    protected void gatherThreads(List<TeleNativeThread> threads) {
        ProgramError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(Params params) {
        ProgramError.unexpected(FAIL_MESSAGE);
        return null;
    }

    @Override
    protected int read0(Address address, ByteBuffer buffer, int offset, int length) {
        return dataAccess.read(address, buffer, offset, length);
    }

    @Override
    protected int write0(ByteBuffer buffer, int offset, int length, Address address) {
        ProgramError.unexpected(FAIL_MESSAGE);
        return 0;
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        ProgramError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        ProgramError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        ProgramError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected ProcessState waitUntilStopped() {
        ProgramError.unexpected(FAIL_MESSAGE);
        return ProcessState.UNKNOWN;
    }
}
