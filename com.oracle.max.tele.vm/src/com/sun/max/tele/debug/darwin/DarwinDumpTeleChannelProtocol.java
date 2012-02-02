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
package com.sun.max.tele.debug.darwin;

import java.io.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.channel.iostream.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.darwin.DarwinMachO.LoadCommand;
import com.sun.max.tele.debug.darwin.DarwinMachO.Segment64LoadCommand;
import com.sun.max.tele.debug.darwin.DarwinMachO.ThreadLoadCommand;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.*;

/**
 * Core file handling for Darwin. Of course, it doesn't use ELF. Mach_O instead.
 *
 */
public class DarwinDumpTeleChannelProtocol extends TeleChannelDataIOProtocolAdaptor implements DarwinTeleChannelProtocol {
    protected int tlaSize;
    public boolean bigEndian;
    protected DarwinMachO machO;
    protected MaxVM teleVM;
    protected static final String HEAP_SYMBOL_NAME = "theHeap";  // defined in image.c, holds the base address of the boot heap
    private final List<Segment64LoadCommand> segmentList = new ArrayList<Segment64LoadCommand>();
    private final List<ThreadLoadCommand> threadDataList = new ArrayList<ThreadLoadCommand>();
    private DarwinDumpThreadAccess darwinDumpThreadAccess;

    public DarwinDumpTeleChannelProtocol(MaxVM teleVM, File vm, File dump) {
        this.teleVM = teleVM;
        try {
            // We need the tele library because we use it to access the OS-specific structs
            // that are embedded in the LC_THREAD segments of the dump file.
            Prototype.loadLibrary(TeleVM.TELE_LIBRARY_NAME);
            machO = new DarwinMachO(dump.getAbsolutePath());
            processLoadCommands(machO.getLoadCommands());
        } catch (Exception ex) {
            TeleError.unexpected("failed to open dump file: " + dump, ex);
        }
    }

    private void processLoadCommands(LoadCommand[] loadCommands) throws IOException {
        for (int i = 0; i < loadCommands.length; i++) {
            LoadCommand lc = loadCommands[i];
            switch (lc.cmd) {
                case LoadCommand.LC_SEGMENT_64:
                    segmentList.add((Segment64LoadCommand) lc);
                    break;

                case LoadCommand.LC_THREAD:
                    threadDataList.add((ThreadLoadCommand) lc);
                    break;

                default:
            }
        }
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        Trace.line(2, "WARNING: Inspector trying to write to " + Long.toHexString(dst));
        return length;
    }

    @Override
    public long create(String pathName, String[] commandLineArguments) {
        inappropriate("create");
        return -1;
    }

    @Override
    public boolean attach(int id) {
        return true;
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public int maxByteBufferSize() {
        return Integer.MAX_VALUE;
    }

    private Segment64LoadCommand findAddress(long addr) {
        final Address address = Address.fromLong(addr);
        for (Segment64LoadCommand slc : segmentList) {
            if (slc.filesize != 0) {
                final Address end = Address.fromLong(slc.vmaddr).plus(slc.vmsize);
                if (address.greaterEqual(Address.fromLong(slc.vmaddr)) && address.lessThan(end)) {
                    return slc;
                }
            }
        }
        return null;
    }

    @Override
    public long getBootHeapStart() {
        final BootImage.Header header = teleVM.bootImage().header;
        final int bootHeapSize = header.codeSize + header.heapSize;
        for (Segment64LoadCommand slc : segmentList) {
            if (slc.filesize != 0 && slc.filesize == bootHeapSize) {
                return slc.vmaddr;
            }
        }
        TeleError.unexpected("failed to find the start of the boot heap");
        return 0;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        final Segment64LoadCommand slc = findAddress(src);
        if (slc == null) {
            return 0;
        }
        try {
            machO.raf.seek(slc.fileoff + (src - slc.vmaddr));
            return machO.raf.read(dst, dstOffset, length);
        } catch (IOException ex) {
            return 0;
        }

    }

    @Override
    public boolean initialize(int tlaSize, boolean bigEndian) {
        this.tlaSize = tlaSize;
        darwinDumpThreadAccess = new DarwinDumpThreadAccess(this, tlaSize, threadDataList);
        return true;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        DarwinDumpThreadAccess.DarwinThreadInfo threadInfo = (DarwinDumpThreadAccess.DarwinThreadInfo) darwinDumpThreadAccess.getThreadInfo((int) threadId);
        System.arraycopy(threadInfo.integerRegisters, 0, integerRegisters, 0, integerRegisters.length);
        System.arraycopy(threadInfo.floatingPointRegisters, 0, floatingPointRegisters, 0, floatingPointRegisters.length);
        System.arraycopy(threadInfo.stateRegisters, 0, stateRegisters, 0, stateRegisters.length);
        return true;
    }

    @Override
    public boolean gatherThreads(Object teleProcessObject, Object threadList, long tlaList) {
        return darwinDumpThreadAccess.gatherThreads(teleProcessObject, threadList, tlaList);
    }

    @Override
    public int gatherThreads(long tlaList) {
        inappropriate("gatherThreads");
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        inappropriate("readThreads");
        return 0;
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        inappropriate("setInstructionPointer");
        return false;
    }

    @Override
    public boolean singleStep(long threadId) {
        inappropriate("setInstructionPointer");
        return false;
    }

    @Override
    public boolean resumeAll() {
        inappropriate("resumeAll");
        return false;
    }

    @Override
    public boolean suspendAll() {
        inappropriate("suspendAll");
        return false;
    }

    @Override
    public boolean resume(long threadId) {
        inappropriate("resume");
        return false;
    }

    @Override
    public boolean suspend(long threadId) {
        inappropriate("suspend");
        return false;
    }

    @Override
    public int waitUntilStoppedAsInt() {
        inappropriate("waitUntilStoppedAsInt");
        return 0;
    }

    @Override
    public boolean kill() {
        return true;
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        inappropriate("activateWatchpoint");
        return false;
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        inappropriate("deactivateWatchpoint");
        return false;
    }

    @Override
    public long readWatchpointAddress() {
        inappropriate("readWatchpointAddress");
        return 0;
    }

    @Override
    public int readWatchpointAccessCode() {
        inappropriate("readWatchpointAccessCode");
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return 0;
    }

    @Override
    public ProcessState waitUntilStopped() {
        inappropriate("waitUntilStoppedAsInt");
        return null;
    }

    protected static void inappropriate(String methodName) {
        TeleError.unexpected("method: " + methodName + " should not be called in dump mode");
    }

    protected static void unimplemented(String methodName) {
        TeleError.unimplemented("method: " + methodName);
    }

}

