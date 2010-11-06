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
package com.sun.max.tele.channel.iostream;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.channel.TeleChannelDataIOProtocol;
import com.sun.max.tele.channel.TeleChannelProtocol;
import com.sun.max.tele.debug.*;

/**
 * An adaptor that provides implementations of the methods in {@link TeleChannelProtocol} that cannot be
 * implemented directly using {@link TeleChannelDataIOProtocol}.
 *
 * @author Mick Jordan
 *
 */
public abstract class TeleChannelDataIOProtocolAdaptor extends TeleChannelDataIOProtocolImpl implements TeleChannelProtocol {

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        byte[] bytes;
        int result;
        if (dst.isDirect()) {
            ByteBuffer bb = (ByteBuffer) dst;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            final int n = readBytes(src, bytes, 0, length);
            bb.put(bytes, dstOffset, n);
            result = n;
        } else {
            bytes = (byte[]) dst.array();
            result = readBytes(src, bytes, dst.arrayOffset() + dstOffset, length);
        }
        return result;
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        byte[] bytes;
        if (src.isDirect()) {
            ByteBuffer bb = (ByteBuffer) src;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            bb.get(bytes);
            srcOffset = 0;
        } else {
            bytes = (byte[]) src.array();
        }
        return writeBytes(dst, bytes, src.arrayOffset() + srcOffset, length);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean gatherThreads(Object teleDomainObject, Object threadSequence, long tlaList, long primordialTLA) {
        final int dataSize = gatherThreads(tlaList, primordialTLA);
        final byte[] data = new byte[dataSize];
        readThreads(dataSize, data);
        // deserialize the thread data
        final ByteArrayInputStream bs = new ByteArrayInputStream(data);
        ObjectInputStream iis = null;
        try {
            iis = new ObjectInputStream(bs);
            TeleChannelDataIOProtocol.GatherThreadData[] threadDataArray = (TeleChannelDataIOProtocol.GatherThreadData[]) iis.readObject();
            // now we call the real jniGatherThread on this side
            TeleProcess teleProcess = (TeleProcess) teleDomainObject;
            for (TeleChannelDataIOProtocol.GatherThreadData t : threadDataArray) {
                Trace.line(1, "calling jniGatherThread id=" + t.id + ", lh=" + t.localHandle + ", h=" + Long.toHexString(t.handle) + ", st=" + t.state +
                        ", ip=" + Long.toHexString(t.instructionPointer) + ", sb=" + Long.toHexString(t.stackBase) + ", ss=" + Long.toHexString(t.stackSize) +
                        ", tlb=" + Long.toHexString(t.tlb) + ", tlbs=" + t.tlbSize + ", tlas=" + t.tlaSize);
                teleProcess.jniGatherThread((List<TeleNativeThread>) threadSequence, t.id, t.localHandle, t.handle, t.state, t.instructionPointer, t.stackBase, t.stackSize, t.tlb, t.tlbSize, t.tlaSize);
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

    @Override
    public ProcessState waitUntilStopped() {
        final int state = waitUntilStoppedAsInt();
        return ProcessState.values()[state];
    }
}
