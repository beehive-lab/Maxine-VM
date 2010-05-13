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

import java.nio.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.guestvm.xen.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.dataio.*;

/**
 * An adaptor that provides implementations of the methods in {@link Protocol} that cannot be
 * implemented directly using {@link SimpleProtocol}.
 *
 * @author Mick Jordan
 *
 */
public abstract class CompleteProtocolAdaptor extends DataIOProtocol implements Protocol {

    @Override
    public int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length) {
        byte[] bytes;
        if (isDirectByteBuffer) {
            ByteBuffer bb = (ByteBuffer) dst;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            final int n = readBytes(src, bytes, 0, length);
            bb.put(bytes, dstOffset, n);
            return n;
        } else {
            bytes = (byte[]) dst;
            return readBytes(src, bytes, dstOffset, length);
        }
    }

    @Override
    public int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length) {
        byte[] bytes;
        if (isDirectByteBuffer) {
            ByteBuffer bb = (ByteBuffer) src;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            bb.get(bytes);
            srcOffset = 0;
        } else {
            bytes = (byte[]) src;
        }
        return writeBytes(dst, bytes, srcOffset, length);
    }

    @Override
    public boolean gatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals) {
        ProgramError.unexpected(getClass().getName() + "." + "gatherThreads not implemented");
        return false;
    }


}
