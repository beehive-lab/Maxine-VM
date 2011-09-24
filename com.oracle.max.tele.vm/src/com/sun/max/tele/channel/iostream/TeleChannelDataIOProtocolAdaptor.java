/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.channel.iostream;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.util.*;

/**
 * An adaptor that provides implementations of the methods in {@link TeleChannelProtocol} that cannot be
 * implemented directly using {@link TeleChannelDataIOProtocol}.
 */
public abstract class TeleChannelDataIOProtocolAdaptor extends TeleChannelDataIOProtocolImpl implements TeleChannelProtocol {

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        byte[] bytes;
        int result;
        if (dst.isDirect()) {
            ByteBuffer bb = dst;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            final int n = readBytes(src, bytes, 0, length);
            bb.put(bytes, dstOffset, n);
            result = n;
        } else {
            bytes = dst.array();
            result = readBytes(src, bytes, dst.arrayOffset() + dstOffset, length);
        }
        return result;
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        byte[] bytes;
        if (src.isDirect()) {
            ByteBuffer bb = src;
            // have to copy the byte buffer back into an array
            bytes = new byte[length];
            bb.get(bytes);
            srcOffset = 0;
        } else {
            bytes = src.array();
        }
        return writeBytes(dst, bytes, src.arrayOffset() + srcOffset, length);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean gatherThreads(Object teleDomainObject, Object threadList, long tlaList) {
        final int dataSize = gatherThreads(tlaList);
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
                teleProcess.jniGatherThread((List<TeleNativeThread>) threadList, t.id, t.localHandle, t.handle, t.state, t.instructionPointer, t.stackBase, t.stackSize, t.tlb, t.tlbSize, t.tlaSize);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            TeleError.unexpected(getClass().getName() + ".gatherThreads unexpected error: ", ex);
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
