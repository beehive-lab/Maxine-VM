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
/*VCSID=a6d0015d-6392-4a3e-a8c1-50a36539c143*/
package com.sun.max.tele.debug.darwin;

import com.sun.max.lang.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * @author mathiske
 */
public class DarwinDataAccess extends DataAccessAdapter {

    private final long _taskHandle;

    public DarwinDataAccess(long taskHandle, DataModel dataModel) {
        super(dataModel);
        _taskHandle = taskHandle;
    }

    private DataIOError error(Address address) {
        return new DataIOError(address);
    }

    static native int nativeReadBytes(long taskHandle, long address, byte[] buffer, int offset, int length);

    public int read(Address address, byte[] buffer, int offset, int length) {
        DataIO.Static.checkRead(buffer, offset, length);
        final int bytesRead = nativeReadBytes(_taskHandle, address.toLong(), buffer, offset, length);
        if (bytesRead < 0) {
            throw new DataIOError(address);
        }
        return bytesRead;
    }

    static native int nativeReadByte(long taskHandle, long address);

    public byte readByte(Address address) {
        final int result = nativeReadByte(_taskHandle, address.toLong());
        if (result < 0) {
            throw error(address);
        }
        return (byte) result;
    }

    static native int nativeReadShort(long taskHandle, long address);

    public short readShort(Address address) {
        final int result = nativeReadShort(_taskHandle, address.toLong());
        if (result < 0) {
            throw error(address);
        }
        return (short) result;
    }

    static native long nativeReadInt(long taskHandle, long address);

    public int readInt(Address address) {
        final long result = nativeReadInt(_taskHandle, address.toLong());
        if (result < 0L) {
            throw error(address);
        }
        return (int) result;
    }

    static native int nativeWriteBytes(long taskHandle, long address, byte[] buffer, int offset, int length);

    public int write(byte[] buffer, int offset, int length, Address address) {
        DataIO.Static.checkWrite(buffer, offset, length);
        return nativeWriteBytes(_taskHandle, address.toLong(), buffer, offset, length);
    }

    static native boolean nativeWriteByte(long taskHandle, long address, byte value);

    public void writeByte(Address address, byte value) {
        if (!nativeWriteByte(_taskHandle, address.toLong(), value)) {
            throw error(address);
        }
    }

    static native boolean nativeWriteShort(long taskHandle, long address, short value);

    public void writeShort(Address address, short value) {
        if (!nativeWriteShort(_taskHandle, address.toLong(), value)) {
            throw error(address);
        }
    }

    private static native boolean nativeWriteInt(long taskHandle, long address, int value);

    public void writeInt(Address address, int value) {
        if (!nativeWriteInt(_taskHandle, address.toLong(), value)) {
            throw error(address);
        }
    }

    static native boolean nativeWriteLong(long taskHandle, long address, long value);

    public void writeLong(Address address, long value) {
        if (!nativeWriteLong(_taskHandle, address.toLong(), value)) {
            throw error(address);
        }
    }

}
