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
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;

/**
 * Core file handling for Darwin. Of course, it doesn't use ELF. Mach_O instead.
 *
 * @author Mick Jordan
 *
 */
public class DarwinDumpTeleChannelProtocol extends TeleChannelDataIOProtocolAdaptor implements DarwinTeleChannelProtocol {
    protected int threadLocalsAreaSize;
    public boolean bigEndian;
    protected RandomAccessFile dumpRaf;
    protected DarwinMachO.Header header;
    protected TeleVM teleVM;
    protected static final String HEAP_SYMBOL_NAME = "theHeap";  // defined in image.c, holds the base address of the boot heap
    private final List<Segment64LoadCommand> segmentList = new ArrayList<Segment64LoadCommand>();
    private final List<ThreadLoadCommand> threadDataList = new ArrayList<ThreadLoadCommand>();
    private DarwinDumpThreadAccess darwinDumpThreadAccess;

    public DarwinDumpTeleChannelProtocol(TeleVM teleVM, File vm, File dump) {
        this.teleVM = teleVM;
        try {
            // We need the tele library because we use it to access the OS-specific structs
            // that are embedded in the LC_THREAD segments of the dump file.
            Prototype.loadLibrary(TeleVM.TELE_LIBRARY_NAME);
            dumpRaf = new RandomAccessFile(dump, "r");
            this.header = DarwinMachO.Header.read(dumpRaf);
            processLoadCommands();
        } catch (Exception ex) {
            FatalError.unexpected("failed to open dump file: " + dump, ex);
        }
    }

    private void processLoadCommands() throws IOException {
        for (int i = 0; i < header.ncmds; i++) {
            LoadCommand lc = LoadCommand.read(dumpRaf);
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
        FatalError.unexpected("failed to find the start of the boot heap");
        return 0;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        final Segment64LoadCommand slc = findAddress(src);
        if (slc == null) {
            return 0;
        }
        try {
            dumpRaf.seek(slc.fileoff + (src - slc.vmaddr));
            return dumpRaf.read(dst, dstOffset, length);
        } catch (IOException ex) {
            return 0;
        }

    }

    @Override
    public boolean initialize(int threadLocalsAreaSize, boolean bigEndian) {
        this.threadLocalsAreaSize = threadLocalsAreaSize;
        darwinDumpThreadAccess = new DarwinDumpThreadAccess(this, threadLocalsAreaSize, threadDataList);
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
    public boolean gatherThreads(Object teleProcessObject, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        return darwinDumpThreadAccess.gatherThreads(teleProcessObject, threadSequence, threadLocalsList, primordialThreadLocals);
    }

    @Override
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
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
        FatalError.unexpected("method: " + methodName + " should not be called in dump mode");
    }

    protected static void unimplemented(String methodName) {
        FatalError.unexpected("method: " + methodName + " is unimplemented");
    }

}

