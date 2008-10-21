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

import java.io.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.memory.VirtualMemory;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;


public class GuestVMXenTeleDomain extends TeleProcess {

    private int _domainId;
    public int domainId() {
        return _domainId;
    }

    private final DataAccess _dataAccess;

    private boolean _terminated = false;

    protected GuestVMXenTeleDomain(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, int id) {
        super(teleVM, platform, programFile, commandLineArguments);
        if (id < 0) {
            _domainId = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter the domain id"));
        } else {
            _domainId = id;
        }
        GuestVMXenDBChannel.attach(this, _domainId);
        if (System.getProperty("max.ins.access.page") != null) {
            _dataAccess = new PageDataAccess(platform.processorKind().dataModel(), this);
        } else {
            _dataAccess = new GuestVMXenDataAccess(platform.processorKind().dataModel(), _domainId);
        }
    }

    @Override
    public DataAccess dataAccess() {
        return _dataAccess;
    }

    public void invalidateCache() {
        if (_dataAccess instanceof PageDataAccess) {
            ((PageDataAccess) _dataAccess).invalidateCache();
        }
    }

    public Pointer getBootHeap() {
        return GuestVMXenDBChannel.getBootHeapStart();
    }

    private static final String primordialThreadName = "maxine";

    @Override
    protected void initPrimordialThread(TeleNativeThread thread) {
        if (((GuestVMXenNativeThread) thread).name().equals(primordialThreadName)) {
            super.initPrimordialThread(thread);
        }
    }

    /**
     * This is called from nativeGatherThreads.
     *
     * @param threadId
     * @param name
     */
    void jniGatherThread(AppendableSequence<TeleNativeThread> threads, int threadId, String name, int state, long stackBase, long stackSize) {
        GuestVMXenNativeThread thread = (GuestVMXenNativeThread) idToThread(threadId);
        if (thread == null) {
        	/* Need to align and skip over the guard page at the base of the stack.
        	 * N.B. "base" is low address (i.e., actually the end of the stack!).
        	 */
        	final long stackBottom = VirtualMemory.pageAlign(Address.fromLong(stackBase)).plus(VMConfiguration.hostOrTarget().platform().pageSize()).toLong();
        	final long adjStackSize = stackSize - (stackBottom - stackBase);
            thread = new GuestVMXenNativeThread(this, threadId, name, stackBottom, adjStackSize);
        } else {
            thread.setMarked(false);
        }

        assert state >= 0 && state < ThreadState.VALUES.length();
        thread.setState(ThreadState.VALUES.get(state));
        threads.append(thread);
    }

    @Override
    protected void kill() throws ExecutionRequestException {
        Problem.unimplementedWarning("cannot kill target domain from Inspector");
    }

    // In the current synchronous connection with the target domain, we only ever stop at a breakpoint
    // and control does not return to the inspector until that happens (see GuestVMDBChannel.nativeResume)

    @Override
    protected boolean waitUntilStopped() {
        if (!_terminated) {
            invalidateCache();
        }
        return !_terminated;
    }

    @Override
    protected void resume() throws ExecutionRequestException {
        final int rrc = GuestVMXenDBChannel.resume(_domainId);
        _terminated = rrc != 0;
    }

    @Override
    public void suspend() throws ExecutionRequestException {
        for (TeleNativeThread thread : threads()) {
            if (!thread.threadSuspend()) {
                throw new ExecutionRequestException("Could not suspend the VM [problem suspending + " + thread + "]");
            }
        }
    }

    @Override
    public void setTransportDebugLevel(int level) {
        GuestVMXenDBChannel.setTransportDebugLevel(level);
        super.setTransportDebugLevel(level);
    }

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        return GuestVMXenDBChannel.readBytes(address, buffer, offset, length);
    }

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        return GuestVMXenDBChannel.writeBytes(buffer, offset, length, address);
    }

    @Override
    protected boolean gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        return GuestVMXenDBChannel.gatherThreads(threads, _domainId);
    }
}
