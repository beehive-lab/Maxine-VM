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

import static com.sun.max.tele.debug.guestvm.xen.dbchannel.dataio.DataIOProtocol.*;

/**
 * A {@link SimpleProtocol} implementation that simply delegates to another.
 *
 * @author Mick Jordan
 *
 */

public class DelegatingProtocol extends RIProtocolAdaptor implements SimpleProtocol {
    private SimpleProtocol impl;

    public DelegatingProtocol(SimpleProtocol impl) {
        this.impl = impl;
        setArrayMode("readBytes", 1, ArrayMode.OUT);
        setArrayMode("writeBytes", 1, ArrayMode.IN);
        setArrayMode("readRegisters", 1, ArrayMode.OUT);
        setArrayMode("readRegisters", 3, ArrayMode.OUT);
        setArrayMode("readRegisters", 5, ArrayMode.OUT);
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return impl.activateWatchpoint(start, size, after, read, write, exec);
    }

    @Override
    public boolean attach(int domId) {
        return impl.attach(domId);
    }

    @Override
    public boolean detach() {
        return impl.detach();
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        return impl.deactivateWatchpoint(start, size);
    }

    @Override
    public long getBootHeapStart() {
        return impl.getBootHeapStart();
    }

    @Override
    public int maxByteBufferSize() {
        return impl.maxByteBufferSize();
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        return impl.readBytes(src, dst, dstOffset, length);
    }

    @Override
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        return impl.readRegisters(threadId, integerRegisters, integerRegistersSize, floatingPointRegisters, floatingPointRegistersSize, stateRegisters, stateRegistersSize);
    }

    @Override
    public int readWatchpointAccessCode() {
        return impl.readWatchpointAccessCode();
    }

    @Override
    public long readWatchpointAddress() {
        return impl.readWatchpointAddress();
    }

    @Override
    public int resume() {
        return impl.resume();
    }

    @Override
    public int setInstructionPointer(int threadId, long ip) {
        return impl.setInstructionPointer(threadId, ip);
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return impl.setTransportDebugLevel(level);
    }

    @Override
    public boolean singleStep(int threadId) {
        return impl.singleStep(threadId);
    }

    @Override
    public boolean suspend(int threadId) {
        return impl.suspend(threadId);
    }

    @Override
    public boolean suspendAll() {
        return impl.suspendAll();
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        return impl.writeBytes(dst, src, srcOffset, length);
    }

}
