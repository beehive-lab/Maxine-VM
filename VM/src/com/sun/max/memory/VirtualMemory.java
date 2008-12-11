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

package com.sun.max.memory;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public final class VirtualMemory {
    private VirtualMemory() {
    }

    private static native long nativeMapFile(long size, int fd, long fileOffset);

    public static Pointer mapFile(Size size, FileDescriptor fileDescriptor, Address fileOffset) throws IOException {
        final Integer fd = (Integer) WithoutAccessCheck.getInstanceField(fileDescriptor, "fd");
        return Pointer.fromLong(nativeMapFile(size.toLong(), fd, fileOffset.toLong()));
    }

    private static native long nativeMapFileIn31BitSpace(int size, int fd, long fileOffset);

    /**
     * Only supported on Linux.
     */
    public static Pointer mapFileIn31BitSpace(int size, FileDescriptor fileDescriptor, Address fileOffset) throws IOException {
        if (Platform.hostOrTarget().operatingSystem() != OperatingSystem.LINUX) {
            throw new UnsupportedOperationException();
        }
        final Integer fd = (Integer) WithoutAccessCheck.getInstanceField(fileDescriptor, "fd");
        return Pointer.fromLong(nativeMapFileIn31BitSpace(size, fd, fileOffset.toLong()));
    }

    @C_FUNCTION
    private static native Pointer virtualMemory_nativeAllocate(Size size);

    public static Pointer allocate(Size size) {
        return virtualMemory_nativeAllocate(size);
    }

    public static void deallocate(Address pointer, Size size) {
        nativeRelease(pointer, size);
    }

    @C_FUNCTION
    private static native Pointer nativeAllocateIn31BitSpace(Size size);

    /**
     * Only supported on Linux/GuestVM.
     */
    public static Pointer allocateIn31BitSpace(Size size) {
        return nativeAllocateIn31BitSpace(size);
    }

    public static boolean allocateMemoryAtFixedAddress(Address pointer, Size size) {
        if (!pointer.isAligned(Platform.target().pageSize())) {
            FatalError.unexpected("Error (Virtual Memory): Start address of the mmaped space must be page alligned ");
        }
        return nativeAllocateMemoryAtFixedAddress(pointer, size);
    }

    @C_FUNCTION
    static native boolean nativeAllocateMemoryAtFixedAddress(Address pointer, Size size);

    @C_FUNCTION
    private static native boolean nativeAllocateAtFixedAddress(Address address, Size size);

    /**
     * Only supported on Solaris.
     */
    public static boolean allocate(Address address, Size size) {
        return nativeAllocateAtFixedAddress(address, size);
    }

    @C_FUNCTION
    private static native Pointer nativeReserve(Size size);

    public static Pointer reserve(Size size) {
        return nativeReserve(size);
    }


    @C_FUNCTION
    static native Address nativeGetEndOfCodeRegion();

    public static Address getEndOfCodeRegion() {
        return nativeGetEndOfCodeRegion();
    }

    @C_FUNCTION
    private static native boolean nativeRelease(Address start, Size size);

    public static boolean release(Address start, Size size) {
        return nativeRelease(start, size);
    }

    /**
     * Sets access protection for a given memory page such that any access (read or write) to it causes a trap.
     *
     * @param pageAddress an address denoting the start of a mapped memory page. This value must be aligned to the
     *            underlying platform's {@linkplain com.sun.max.platform.Platform#pageSize() page size}.
     */
    @C_FUNCTION
    public static native void protectPage(Address pageAddress);

    /**
     * Sets access protection for a given memory page such that any read or write access to it is legal.
     *
     * @param pageAddress an address denoting the start of a mapped memory page. This value must be aligned to the
     *            underlying platform's {@linkplain com.sun.max.platform.Platform#pageSize() page size}.
     */
    @C_FUNCTION
    public static native void unprotectPage(Address pageAddress);

    @C_FUNCTION
    public static native Address pageAlign(Address address);
}
