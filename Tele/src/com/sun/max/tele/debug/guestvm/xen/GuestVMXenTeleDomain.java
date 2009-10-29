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

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;


public class GuestVMXenTeleDomain extends TeleProcess {

    private int domainId;
    public int domainId() {
        return domainId;
    }

    private final DataAccess dataAccess;

    private boolean terminated = false;

    protected GuestVMXenTeleDomain(TeleVM teleVM, Platform platform, int id) {
        super(teleVM, platform, ProcessState.STOPPED);
        if (id < 0) {
            domainId = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter the domain id"));
        } else {
            domainId = id;
        }
        GuestVMXenDBChannel.attach(this, domainId);
        dataAccess = new PageDataAccess(this, platform.processorKind.dataModel);
    }

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    public Pointer getBootHeap() {
        return GuestVMXenDBChannel.getBootHeapStart();
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(int id, long threadId, long stackBase, long stackSize) {
        /* Need to align and skip over the guard page at the base of the stack.
         * N.B. "base" is low address (i.e., actually the end of the stack!).
         */
        final int pageSize = VMConfiguration.hostOrTarget().platform().pageSize;
        final long stackBottom = pageAlign(stackBase, pageSize) + pageSize;
        final long adjStackSize = stackSize - (stackBottom - stackBase);
        return new GuestVMXenNativeThread(this, id, threadId, stackBottom, adjStackSize);
    }

    private static long pageAlign(long address, int pageSize) {
        final long alignment = pageSize - 1;
        return (long) (address + alignment) & ~alignment;

    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        ProgramWarning.message("unimplemented: " + "cannot kill target domain from Inspector");
    }

    // In the current synchronous connection with the target domain, we only ever stop at a breakpoint
    // and control does not return to the inspector until that happens (see GuestVMDBChannel.nativeResume)

    @Override
    protected boolean waitUntilStopped() {
        return !terminated;
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        final int rrc = GuestVMXenDBChannel.resume(domainId);
        terminated = rrc != 0;
    }

    @Override
    public void suspend() throws OSExecutionRequestException {
        if (!GuestVMXenDBChannel.suspendAll()) {
            throw new OSExecutionRequestException("Could not suspend the VM");
        }
    }

    @Override
    public void setTransportDebugLevel(int level) {
        GuestVMXenDBChannel.setTransportDebugLevel(level);
        super.setTransportDebugLevel(level);
    }

    @Override
    protected int read0(Address address, ByteBuffer buffer, int offset, int length) {
        return GuestVMXenDBChannel.readBytes(address, buffer, offset, length);
    }

    @Override
    protected int write0(ByteBuffer buffer, int offset, int length, Address address) {
        return GuestVMXenDBChannel.writeBytes(buffer, offset, length, address);
    }

    @Override
    protected void gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        final Word primordialThreadLocals = dataAccess().readWord(teleVM().bootImageStart().plus(teleVM().bootImage().header.primordialThreadLocalsOffset));
        final Word threadLocalsList = dataAccess().readWord(teleVM().bootImageStart().plus(teleVM().bootImage().header.threadLocalsListHeadOffset));
        GuestVMXenDBChannel.gatherThreads(threads, domainId, threadLocalsList.asAddress().toLong(), primordialThreadLocals.asAddress().toLong());
    }

    @Override
    public int maximumWatchpointCount() {
        // not sure how many are supported; we'll try this
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean activateWatchpoint(TeleWatchpoint teleWatchpoint) {
        return GuestVMXenDBChannel.activateWatchpoint(domainId, teleWatchpoint);
    }

    @Override
    protected boolean deactivateWatchpoint(TeleWatchpoint teleWatchpoint) {
        return GuestVMXenDBChannel.deactivateWatchpoint(domainId, teleWatchpoint);
    }

    @Override
    protected long readWatchpointAddress() {
        return GuestVMXenDBChannel.readWatchpointAddress(domainId);
    }

    @Override
    protected int readWatchpointAccessCode() {
        int code = GuestVMXenDBChannel.readWatchpointAccessCode(domainId);
        if (code == 1) {
            return 1;
        } else if (code == 2) {
            return 2;
        } else if (code == 4) {
            return 3;
        }
        return 0;
    }
}
