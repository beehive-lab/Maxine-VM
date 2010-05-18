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

import java.io.*;
import java.nio.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.dataio.*;
import com.sun.max.tele.debug.guestvm.xen.*;
import com.sun.max.tele.debug.*;

/**
 * An adaptor that provides implementations of the methods in {@link Protocol} that cannot be
 * implemented directly using {@link SimpleProtocol}.
 *
 * @author Mick Jordan
 *
 */
public abstract class CompleteProtocolAdaptor extends DataIOProtocol implements Protocol {
    private Timings timings = new Timings("CompleteProtocolAdaptor.readBytes");

    @Override
    public int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length) {
        byte[] bytes;
        int result;
        timings.start();
        if (isDirectByteBuffer) {
            ByteBuffer bb = (ByteBuffer) dst;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            final int n = readBytes(src, bytes, 0, length);
            bb.put(bytes, dstOffset, n);
            result = n;
        } else {
            bytes = (byte[]) dst;
            result = readBytes(src, bytes, dstOffset, length);
        }
        timings.add();
        return result;
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

    @SuppressWarnings("unchecked")
    @Override
    public boolean gatherThreads(Object teleDomainObject, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        final int dataSize = gatherThreads(threadLocalsList, primordialThreadLocals);
        final byte[] data = new byte[dataSize];
        readThreads(dataSize, data);
        // deserialize the thread data
        final ByteArrayInputStream bs = new ByteArrayInputStream(data);
        ObjectInputStream iis = null;
        try {
            iis = new ObjectInputStream(bs);
            SimpleProtocol.GatherThreadData[] threadDataArray = (SimpleProtocol.GatherThreadData[]) iis.readObject();
            // now we call the real jniGatherThread on this side
            GuestVMXenTeleDomain teleDomain = (GuestVMXenTeleDomain) teleDomainObject;
            for (SimpleProtocol.GatherThreadData t : threadDataArray) {
                //Trace.line(1, "calling jniGatherThread " + t.id + ", " + t.localHandle + ", " + t.handle + ", " + t.state + ", " + t.instructionPointer + ", " +
                //                Long.toHexString(t.stackBase) + ", " + t.stackSize+ ", " + t.tlb + ", " + t.tlbSize + ", " + t.tlaSize);
                teleDomain.jniGatherThread((AppendableSequence<TeleNativeThread>) threadSequence, t.id, t.localHandle, t.handle, t.state, t.instructionPointer, t.stackBase, t.stackSize, t.tlb, t.tlbSize, t.tlaSize);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ProgramError.unexpected(getClass().getName() + ".gatherThreads unexpected error: ", ex);
        } finally {
            if (iis != null) {
                try {
                    iis.close();
                    bs.close();
                } catch (IOException ex) {
                }
            }
        }
        return false;
    }


}
