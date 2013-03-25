/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.memory;

import static com.sun.max.vm.MaxineVM.*;

import java.nio.*;
import java.util.*;

import com.oracle.graal.replacements.Snippet.Fold;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * This class provides methods to access raw memory through pointers.
 * It also provides allocation methods that are expected to be for small quantities
 * of memory (or quantities that are not multiple of a page) that will be satisfied
 * by the native allocation library, i.e. malloc/free.
 * Large amounts of memory should be allocated using the {@link VirtualMemory} class.
 */
public final class Memory {

    private Memory() {
    }

    @HOSTED_ONLY
    private static byte[] buf = new byte[1024];

    @HOSTED_ONLY
    public static ByteBuffer memory = ByteBuffer.wrap(buf);

    public static final OutOfMemoryError OUT_OF_MEMORY_ERROR = new OutOfMemoryError();
    /**
     * Marker used for filling dead heap area when in debug mode.
     * Note: the low-order bit must be 0 so the marker isn't confused with a forwarder.
     */
    public static final long ZAPPED_MARKER = 0xDEADBEEFCAFEBABEL;

    @C_FUNCTION
    private static native Pointer memory_allocate(Size size);

    /**
     * Allocates an aligned chunk of memory using a malloc(3)-like facility.
     *
     * @param size the size of the chunk of memory to be allocated
     * @return a pointer to the allocated chunk of memory or {@code Pointer.zero()} if allocation failed
     */
    public static Pointer allocate(Size size) {
        if (size.toLong() < 0) {
            throw new IllegalArgumentException();
        }
        if (isHosted()) {
            return boxedAllocate(size);
        }
        return memory_allocate(size);
    }

    @HOSTED_ONLY
    private static synchronized Pointer boxedAllocate(Size size) {
        int offset = buf.length;
        int newLength = offset + size.toInt();
        buf = Arrays.copyOf(buf, newLength);
        memory = ByteBuffer.wrap(buf);
        memory.order(ByteOrder.nativeOrder());
        return Pointer.fromInt(offset);
    }

    /**
     * @param size the size of the chunk of memory to be allocated
     * @return a pointer to the allocated chunk of memory
     * @throws OutOfMemoryError if allocation failed or log message and VM termination if early in bootstrap
     */
    public static Pointer mustAllocate(Size size) throws OutOfMemoryError, IllegalArgumentException {
        final Pointer result = isHosted() ? boxedAllocate(size) : memory_allocate(size);
        if (result.isZero()) {
            if (MaxineVM.isPrimordialOrPristine()) {
                MaxineVM.reportPristineMemoryFailure("unknown", "mustAllocate", size);
            } else {
                throw OUT_OF_MEMORY_ERROR;
            }
        }
        return result;
    }

    /**
     * @param size the size of the chunk of memory to be allocated
     * @return a pointer to the allocated chunk of memory
     * @throws IllegalArgumentException if size is negative
     * @throws OutOfMemoryError if allocation failed
     */
    public static Pointer mustAllocate(int size) throws OutOfMemoryError, IllegalArgumentException {
        return mustAllocate(Size.fromInt(size));
    }

    @C_FUNCTION
    private static native Pointer memory_reallocate(Pointer block, Size size);

    public static Pointer reallocate(Pointer block, Size size) throws OutOfMemoryError, IllegalArgumentException {
        if (isHosted()) {
            Pointer newBlock = allocate(size);
            Memory.copyBytes(block, newBlock, size);
            return newBlock;
        }
        return memory_reallocate(block, size);
    }

    @C_FUNCTION
    private static native int memory_deallocate(Address pointer);

    public static void deallocate(Address block) throws IllegalArgumentException {
        if (block.isZero()) {
            throw new IllegalArgumentException();
        }
        final int errorCode = isHosted() ? 0 /* TODO (ds): implement a free list */ : memory_deallocate(block);
        if (errorCode != 0) {
            throw ProgramError.unexpected("Memory.deallocate() failed with OS error code: " + errorCode);
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void setBytes(Pointer pointer, Size numberOfBytes, byte value) {
        for (Offset i = Offset.zero(); i.lessThan(numberOfBytes.asOffset()); i = i.plus(1)) {
            pointer.writeByte(i, value);
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void setWords(Pointer pointer, int numberOfWords, Word value) {
        for (int i = 0; i < (numberOfWords * Word.size()); i += Word.size()) {
            pointer.writeWord(i, value);
        }
    }

    @NO_SAFEPOINT_POLLS("speed, and used in code that shouldn't be interrupted by GC")
    public static void clearWords(Pointer start, int length) {
        FatalError.check(start.isWordAligned(), "Can only zero word-aligned region");
        for (int i = 0; i < length; i++) {
            start.setWord(i, Address.zero());
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void setBytes(Pointer pointer, int numberOfBytes, byte value) {
        for (int i = 0; i < numberOfBytes; i++) {
            pointer.writeByte(i, value);
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void clearBytes(Pointer pointer, int numberOfBytes) {
        setBytes(pointer, numberOfBytes, (byte) 0);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static boolean equals(Pointer pointer1, Pointer pointer2, Size numberOfBytes) {
        for (Offset i = Offset.zero(); i.lessThan(numberOfBytes.asOffset()); i = i.plus(1)) {
            if (pointer1.readByte(i) != pointer2.readByte(i)) {
                return false;
            }
        }
        return true;
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static boolean equals(Pointer pointer1, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (pointer1.readByte(i) != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void copyBytes(Pointer fromPointer, Pointer toPointer, Size numberOfBytes) {
        Offset i = Offset.zero();
        Size wordBounds = numberOfBytes.alignDown(Word.size());
        while (i.lessThan(wordBounds.asOffset())) {
            toPointer.writeWord(i, fromPointer.readWord(i));
            i = i.plus(Word.size());
        }
        while (i.lessThan(numberOfBytes.asOffset())) {
            toPointer.writeByte(i, fromPointer.readByte(i));
            i = i.plus(1);
        }
        assert i.equals(numberOfBytes);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void readBytes(Pointer fromPointer, int numberOfBytes, byte[] toArray, int startIndex) {
        for (int i = 0; i < numberOfBytes; i++) {
            toArray[startIndex + i] = fromPointer.readByte(i);
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void readBytes(Pointer fromPointer, int numberOfBytes, byte[] toArray) {
        readBytes(fromPointer, numberOfBytes, toArray, 0);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void readBytes(Pointer fromPointer, byte[] toArray) {
        readBytes(fromPointer, toArray.length, toArray, 0);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void readWords(Pointer fromPointer, int numberOfWords, Word[] toArray, int startIndex) {
        for (int i = 0; i < numberOfWords; i++) {
            WordArray.set(toArray, startIndex + i, fromPointer.getWord(i));
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void readWords(Pointer fromPointer, int numberOfWords, Word[] toArray) {
        readWords(fromPointer, numberOfWords, toArray, 0);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void readWords(Pointer fromPointer, Word[] toArray) {
        readWords(fromPointer, toArray.length, toArray, 0);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void writeBytes(byte[] fromArray, int startIndex, int numberOfBytes, Pointer toPointer) {
        for (int i = 0; i < numberOfBytes; i++) {
            toPointer.writeByte(i, fromArray[startIndex + i]);
        }
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void writeBytes(byte[] fromArray, int numberOfBytes, Pointer toPointer) {
        writeBytes(fromArray, 0, numberOfBytes, toPointer);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void writeBytes(byte[] fromArray, Pointer toPointer) {
        writeBytes(fromArray, fromArray.length, toPointer);
    }

    @NO_SAFEPOINT_POLLS("speed")
    public static void zapRegion(MemoryRegion region) {
        FatalError.check(region.start().isWordAligned(), "Can only zap word-aligned region");
        FatalError.check(region.size().remainder(Word.size()) == 0, "Can only zap region of words");
        setWords(region.start().asPointer(), region.size().dividedBy(Word.size()).toInt(), zappedMarker());
    }

    @Fold
    public static Address zappedMarker() {
        return Address.fromLong(ZAPPED_MARKER);
    }
}
