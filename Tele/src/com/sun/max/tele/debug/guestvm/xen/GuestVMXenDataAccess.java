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
package com.sun.max.tele.debug.guestvm.xen;

import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;

public class GuestVMXenDataAccess extends DataAccessAdapter {

    private final int domainId;

    protected GuestVMXenDataAccess(DataModel dataModel, int domainId) {
        super(dataModel.wordWidth);
        this.domainId = domainId;
    }

    public byte readByte(Address address) {
        final int result = GuestVMXenDBChannel.readByte(domainId, address.toLong());
        if (result < 0) {
            throw new DataIOError(address);
        }
        return (byte) result;
    }

    public int read(Address address, ByteBuffer buffer, int offset, int length) {
        DataIO.Static.checkRead(buffer, offset, length);
        return GuestVMXenDBChannel.readBytes(address, buffer, offset, length);
    }

    public int readInt(Address address) {
        final long result = GuestVMXenDBChannel.readInt(domainId, address.toLong());
        if (result < 0L) {
            throw new DataIOError(address);
        }
        return (int) result;
    }

    public short readShort(Address address) {
        final int result = GuestVMXenDBChannel.readShort(domainId, address.toLong());
        if (result < 0L) {
            throw new DataIOError(address);
        }
        return (short) result;
    }

    public long readLong(Address address) {
        throw FatalError.unimplemented();
    }

    public void writeByte(Address address, byte value) {
        if (!GuestVMXenDBChannel.writeByte(domainId, address.toLong(), value)) {
            throw new DataIOError(address);
        }
    }

    public int write(ByteBuffer buffer, int offset, int length, Address address) {
        DataIO.Static.checkWrite(buffer, offset, length);
        return GuestVMXenDBChannel.writeBytes(buffer, offset, length, address);
    }

    public void writeInt(Address address, int value) {
        throw FatalError.unimplemented();
    }

    public void writeLong(Address address, long value) {
        throw FatalError.unimplemented();
    }

    public void writeShort(Address address, short value) {
        throw FatalError.unimplemented();
    }
}
