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
package com.sun.max.tele.debug.unix;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.tele.MaxWatchpoint.WatchpointSettings;
import com.sun.max.tele.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;


/**
 * Abstracts the common code for {@link TeleProcess} for the various flavors of Unix supported by Maxine.
 *
 */
public abstract class UnixTeleProcessAdaptor extends TeleProcess {

    protected final TeleChannelProtocol protocol;
    protected final DataAccess dataAccess;

    private UnixTeleProcessAdaptor(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, int id) throws BootImageException {
        super(teleVM, platform, ProcessState.STOPPED);
        protocol = TeleVM.teleChannelProtocol();
        dataAccess = new PageDataAccess(this, platform.dataModel);
        protocol.initialize(teleVM.bootImage().header.tlaSize, platform().endianness() == Endianness.BIG ? true : false);
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
        final Word tlaList = dataAccess().readWord(vm().bootImageStart().plus(vm().bootImage().header.tlaListHeadOffset));
        protocol.gatherThreads(this, threads, tlaList.asAddress().toLong());
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
    protected boolean activateWatchpoint(VmWatchpoint watchpoint) {
        final WatchpointSettings settings = watchpoint.getSettings();
        return protocol.activateWatchpoint(watchpoint.memoryRegion().start().toLong(), watchpoint.memoryRegion().nBytes(), true, settings.trapOnRead, settings.trapOnWrite, settings.trapOnExec);
    }

    @Override
    protected boolean deactivateWatchpoint(VmWatchpoint watchpoint) {
        return protocol.deactivateWatchpoint(watchpoint.memoryRegion().start().toLong(), watchpoint.memoryRegion().nBytes());
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
