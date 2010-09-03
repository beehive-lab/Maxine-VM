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
package com.sun.max.tele.debug.unix;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.tele.MaxWatchpoint.WatchpointSettings;
import com.sun.max.tele.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;


/**
 * Abstracts the common code for {@link TeleProcess} for the various flavors of Unix supported by Maxine.
 *
 * @author Mick Jordan
 *
 */
public abstract class UnixTeleProcessAdaptor extends TeleProcess {

    protected final TeleChannelProtocol protocol;
    protected final DataAccess dataAccess;

    private UnixTeleProcessAdaptor(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, int id) throws BootImageException {
        super(teleVM, platform, ProcessState.STOPPED);
        protocol = TeleVM.teleChannelProtocol();
        dataAccess = new PageDataAccess(this, platform.dataModel());
        protocol.initialize(teleVM.bootImage().header.threadLocalsAreaSize, teleVM.vmConfiguration().platform.endianness() == Endianness.BIG ? true : false);
        if (commandLineArguments != null) {
            final long processHandle = protocol.create(programFile.getAbsolutePath(), commandLineArguments);
            if (processHandle < 0) {
                String exe = programFile.getName();
                Log.println("This may be due to resources being consumed by zombie maxvm processes. Try running:");
                Log.println();
                Log.println("    pgrep " + exe + "; pkill -9 " + exe);
                Log.println();
                throw new BootImageException("Could not start VM process");
            }
            try {
                resume();
            } catch (OSExecutionRequestException e) {
                throw new BootImageException("Error resuming VM after starting it", e);
            }
        } else {
            protocol.attach(id);
        }
    }

    protected UnixTeleProcessAdaptor(TeleVM teleVM, Platform platform, File programFile, int id) throws BootImageException {
        this(teleVM, platform, programFile, null, id);
    }

    protected UnixTeleProcessAdaptor(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments) throws BootImageException {
        this(teleVM, platform, programFile, commandLineArguments, -1);
    }

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    @Override
    public void kill() throws OSExecutionRequestException {
        if (!protocol.kill()) {
            throw new OSExecutionRequestException("Could not kill target VM");
        }
    }

    @Override
    public void suspend() throws OSExecutionRequestException {
        if (!protocol.suspendAll()) {
            throw new OSExecutionRequestException("Could not suspend target VM");
        }
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        if (!protocol.resumeAll()) {
            throw new OSExecutionRequestException("Could not resume the target VM ");
        }
    }

    /**
     * Waits until this process is stopped.
     */
    @Override
    protected ProcessState waitUntilStopped() {
        return protocol.waitUntilStopped();
    }

    @Override
    protected void gatherThreads(List<TeleNativeThread> threads) {
        final Word primordialVmThreadLocals = dataAccess().readWord(vm().bootImageStart().plus(vm().bootImage().header.primordialThreadLocalsOffset));
        final Word threadLocalsList = dataAccess().readWord(vm().bootImageStart().plus(vm().bootImage().header.threadLocalsListHeadOffset));
        protocol.gatherThreads(this, threads, threadLocalsList.asAddress().toLong(), primordialVmThreadLocals.asAddress().toLong());
    }

    @Override
    protected int read0(Address src, ByteBuffer dst, int offset, int length) {
        return TeleChannelTransferBytes.readBytes(protocol, dst, offset, length, src);
    }

    @Override
    protected int write0(ByteBuffer src, int offset, int length, Address dst) {
        return TeleChannelTransferBytes.writeBytes(protocol, src, offset, length, dst);
    }

    @Override
    public int platformWatchpointCount() {
        return 0;
    }

    @Override
    protected boolean activateWatchpoint(TeleWatchpoint teleWatchpoint) {
        final WatchpointSettings settings = teleWatchpoint.getSettings();
        return protocol.activateWatchpoint(teleWatchpoint.memoryRegion().start().toLong(), teleWatchpoint.memoryRegion().size().toLong(), true, settings.trapOnRead, settings.trapOnWrite, settings.trapOnExec);
    }

    @Override
    protected boolean deactivateWatchpoint(TeleWatchpoint teleWatchpoint) {
        return protocol.deactivateWatchpoint(teleWatchpoint.memoryRegion().start().toLong(), teleWatchpoint.memoryRegion().size().toLong());
    }

    /**
     * Reads the address which triggered a watchpoint signal.
     *
     * @param processHandle
     * @return address
     */
    @Override
    protected long readWatchpointAddress() {
        return protocol.readWatchpointAddress();
    }

    /**
     * Reads the access code of the watchpoint which triggered a signal.
     *
     * @param processHandle
     * @return access code
     */
    @Override
    protected int readWatchpointAccessCode() {
        int code = protocol.readWatchpointAccessCode();
        if (code == 3) {
            return 1;
        } else if (code == 4) {
            return 2;
        } else if (code == 5) {
            return 3;
        }
        return 0;
    }

}
