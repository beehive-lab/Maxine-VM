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
import java.nio.*;
import java.nio.channels.FileChannel.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.prototype.BootImage.*;

/**
 * A null process that "contains" the boot image for inspection, as if it were a {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class ReadOnlyTeleProcess extends TeleProcess {

    private final DataAccess _dataAccess;
    private final Pointer _heap;

    @Override
    public DataAccess dataAccess() {
        return _dataAccess;
    }

    public ReadOnlyTeleProcess(TeleVM teleVM, Platform platform, File programFile) throws BootImageException {
        super(teleVM, platform, ProcessState.NO_PROCESS);
        _heap = Pointer.zero();
        try {
            _dataAccess = map(teleVM.bootImageFile(), teleVM.bootImage(), false);
        } catch (IOException ioException) {
            throw new BootImageException("Error mapping in boot image", ioException);
        }
    }

    public Pointer heap() {
        return _heap;
    }

    /**
     * Maps the heap and code sections of the boot image in a given file into memory.
     *
     * @param bootImageFile the file containing the heap and code sections to map into memory
     * @param relocate specifies if the mapped sections should be relocated according to the relocation data in {@code
     *            file} after the mapping has occurred
     * @return the address at which the heap and code sections in {@code file} were mapped
     * @throws IOException if an IO error occurs while performing the memory mapping
     */
    public DataAccess map(File bootImageFile, BootImage bootImage, boolean relocate) throws IOException {
        final RandomAccessFile randomAccessFile = new RandomAccessFile(bootImageFile, "rwd");

        final Header header = bootImage.header();

        int heapOffsetInImage = header.size() + header._stringInfoSize;
        randomAccessFile.seek(heapOffsetInImage);

        final byte[] relocationData = new byte[header._relocationDataSize];
        randomAccessFile.read(relocationData);

        heapOffsetInImage += header._relocationDataSize;
        heapOffsetInImage += bootImage.pagePaddingSize(heapOffsetInImage);

        final MappedByteBuffer bootImageBuffer = randomAccessFile.getChannel().map(MapMode.PRIVATE, heapOffsetInImage, header.bootHeapSize + header.bootCodeSize);
        bootImageBuffer.order(bootImage.vmConfiguration().platform().processorKind().dataModel().endianness().asByteOrder());

        return new MappedByteBufferDataAccess(bootImageBuffer, _heap, WordWidth.BITS_64);
    }

    static final class MappedByteBufferDataAccess extends DataAccessAdapter {

        private final MappedByteBuffer _buffer;
        private final Address _base;

        MappedByteBufferDataAccess(MappedByteBuffer buffer, Address base, WordWidth wordWidth) {
            super(wordWidth);
            _buffer = buffer;
            _base = base;
        }

        public int read(Address src, ByteBuffer dst, int dstOffset, int length) throws DataIOError {
            final int toRead = Math.min(length, dst.limit() - dstOffset);
            final ByteBuffer srcView = (ByteBuffer) _buffer.duplicate().position(src.toInt()).limit(toRead);
            dst.put(srcView);
            return toRead;
        }

        public int write(ByteBuffer src, int srcOffset, int length, Address dst) throws DataIOError {
            _buffer.position(dst.toInt());
            final ByteBuffer srcView = (ByteBuffer) src.duplicate().position(srcOffset).limit(length);
            _buffer.put(srcView);
            return length;
        }

        private int asOffset(Address address) {
            if (address.lessThan(_base)) {
                throw new DataIOError(address);
            }

            if (address.toLong() < 0 || address.toLong() > Integer.MAX_VALUE) {
                throw new DataIOError(address);
            }
            return _base.toInt() + address.toInt();
        }

        public byte readByte(Address address) {
            return _buffer.get(asOffset(address));
        }

        public int readInt(Address address) {
            return _buffer.getInt(asOffset(address));
        }

        public long readLong(Address address) {
            return _buffer.getLong(asOffset(address));
        }

        public short readShort(Address address) {
            return _buffer.getShort(asOffset(address));
        }

        public void writeByte(Address address, byte value) {
            _buffer.put(asOffset(address), value);
        }

        public void writeInt(Address address, int value) {
            _buffer.putInt(asOffset(address), value);
        }

        public void writeLong(Address address, long value) {
            _buffer.putLong(asOffset(address), value);
        }

        public void writeShort(Address address, short value) {
            _buffer.putShort(asOffset(address), value);
        }
    }

    private static final String FAIL_MESSAGE = "Attempt to run/write/modify a read-only bootimage VM with no live process";

    @Override
    protected void gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        ProgramError.unexpected(FAIL_MESSAGE);
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(int id, long handle, long stackBase, long stackSize) {
        ProgramError.unexpected(FAIL_MESSAGE);
        return null;
    }

    @Override
    protected int read0(Address address, ByteBuffer buffer, int offset, int length) {
        return _dataAccess.read(address, buffer, offset, length);
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
    protected boolean waitUntilStopped() {
        ProgramError.unexpected(FAIL_MESSAGE);
        return false;
    }
}
