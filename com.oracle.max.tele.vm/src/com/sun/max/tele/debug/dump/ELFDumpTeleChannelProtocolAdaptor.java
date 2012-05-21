/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug.dump;

import static com.oracle.max.elf.ELFProgramHeaderTable.*;

import java.io.*;

import com.oracle.max.elf.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.iostream.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.*;

public class ELFDumpTeleChannelProtocolAdaptor extends TeleChannelDataIOProtocolAdaptor implements TeleChannelProtocol {

    protected int tlaSize;
    public boolean bigEndian;
    protected RandomAccessFile dumpRaf;
    protected ELFHeader header;
    protected ELFProgramHeaderTable programHeaderTable;
    protected ELFSymbolLookup symbolLookup;
    protected MaxVM teleVM;
    protected static final String HEAP_SYMBOL_NAME = "theHeap";  // defined in image.c, holds the base address of the boot heap


    protected ELFDumpTeleChannelProtocolAdaptor(MaxVM teleVM, File vm, File dump) {
        this.teleVM = teleVM;
        try {
            // We actually do need the tele library because we use it to access the OS-specific structs
            // that are embedded in the NOTE sections of the dump file.
            Prototype.loadLibrary(TeleVM.TELE_LIBRARY_NAME);
            dumpRaf = new RandomAccessFile(dump, "r");
            this.header = ELFLoader.readELFHeader(dumpRaf);
            this.programHeaderTable = ELFLoader.readPHT(dumpRaf, header);
            // This is not needed currently as we cannot look up symbols from shared libraries.
            //symbolLookup = new ELFSymbolLookup(new File(vm.getParent(), "libjvm.so"));
        } catch (Exception ex) {
            TeleError.unexpected("failed to open dump file: " + dump, ex);
        }
    }

    @Override
    public boolean initialize(int tlaSize, boolean bigEndian) {
        this.tlaSize = tlaSize;
        this.bigEndian = bigEndian;
        return true;
    }

    protected static class NoteEntryHandler {
        /**
         * OS-specific processing a NOTE entry.
         * @param type type of NOTE entry
         * @param name name of NOTE entry
         * @param desc byte array of NOTE contents
         */
        protected void processNoteEntry(int type, String name, byte[] desc) {

        }
    }

    protected void processNoteSection(NoteEntryHandler entryHandler) {
        ELFProgramHeaderTable.Entry64 noteSectionEntry = null;
        // if there are 2 NOTE entries we want the second
        for (ELFProgramHeaderTable.Entry entry : programHeaderTable.entries) {
            if (entry.p_type == PT_NOTE) {
                if (noteSectionEntry != null) {
                    noteSectionEntry = (ELFProgramHeaderTable.Entry64) entry;
                    break;
                }
                noteSectionEntry = (ELFProgramHeaderTable.Entry64) entry;
            }
        }
        try {
            dumpRaf.seek(noteSectionEntry.p_offset);
            final ELFDataInputStream dis = new ELFDataInputStream(header, dumpRaf);
            final long size = noteSectionEntry.p_filesz;
            long readLength = 0;
            while (readLength < size) {
                int namesz = dis.read_Elf64_Word();
                final int descsz = dis.read_Elf64_Word();
                final int type = dis.read_Elf64_Word();
                final String name = readNoteString(dis, namesz);
                readLength += 12 + namesz;
                while (namesz % 8 != 0) {
                    dis.read_Elf32_byte();
                    readLength++;
                    namesz++;
                }
                final byte[] desc = readNoteDesc(dis, descsz);
                readLength += descsz;

                entryHandler.processNoteEntry(type, name, desc);
            }
        } catch (IOException ex) {
            TeleError.unexpected("error reading dump file note section", ex);
        }

    }

    /**
     * Read a string from a NOTE entry with length length. The returned string is of size length - 1 as java strings are
     * not null terminated
     *
     * @param length
     * @return
     */
    private String readNoteString(ELFDataInputStream dis, int length) throws IOException {
        byte[] arr = new byte[length - 1];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = dis.read_Elf64_byte();
        }
        dis.read_Elf64_byte();
        return new String(arr);
    }

    private byte[] readNoteDesc(ELFDataInputStream dis, int length) throws IOException {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) {
            arr[i] = dis.read_Elf64_byte();
        }
        return arr;
    }

    protected ELFProgramHeaderTable.Entry64 findAddress(long addr) {
        final Address address = Address.fromLong(addr);
        for (ELFProgramHeaderTable.Entry entry : programHeaderTable.entries) {
            ELFProgramHeaderTable.Entry64 entry64 = (ELFProgramHeaderTable.Entry64) entry;
            if (entry64.p_type == PT_LOAD && entry64.p_filesz != 0) {
                final Address end = Address.fromLong(entry64.p_vaddr).plus(entry64.p_memsz);
                if (address.greaterEqual(Address.fromLong(entry64.p_vaddr)) && address.lessThan(end)) {
                    return entry64;
                }
            }
        }
        return null;
    }

    /**
     * A workaround until this can be done properly by reading it from memory.
     * We basically look for a segment that is the same size as the sum of the code and heap size
     * from the {@link BootImage#header}.
     * @return the base of the boot heap
     */
    private long getBootHeapStartHack() {
        final BootImage.Header header = teleVM.bootImage().header;
        final int bootHeapSize = header.codeSize + header.heapSize;
        for (ELFProgramHeaderTable.Entry entry : programHeaderTable.entries) {
            if (entry.p_type == PT_LOAD) {
                ELFProgramHeaderTable.Entry64 entry64 = (ELFProgramHeaderTable.Entry64) entry;
                if (entry64.p_filesz > 0 && entry64.p_filesz == bootHeapSize) {
                    return entry64.p_vaddr;
                }
            }
        }
        TeleError.unexpected("failed to find the start of the boot heap");
        return 0;
    }

    @Override
    public long getBootHeapStart() {
        // Check if an option has specified the heap address.
        final long heapAddress = VmHeapAccess.heapAddressOption();
        if (heapAddress != 0) {
            return heapAddress;
        }
        if (true) {
            return getBootHeapStartHack();
        } else {
            // This is the clean way to do it if you know how to get the absolute address of symbols loaded from shared libraries,
            // which is not trivial or documented.
            final long theHeapAddress = getBootHeapStartSymbolAddress();
            ELFProgramHeaderTable.Entry64 entry64 = findAddress(theHeapAddress);
            try {
                dumpRaf.seek(entry64.p_offset + (theHeapAddress - entry64.p_vaddr));
                ELFDataInputStream ds = new ELFDataInputStream(header, dumpRaf);
                return ds.read_Elf64_Addr();
            } catch (Throwable ex) {
                TeleError.unexpected("failed to get boot heap address", ex);
                return 0;
            }
        }
    }

    /**
     * Return the absolute address of the symbol in image.c whose vcalue is the base address of the boot heap.
     * Since this is a static, the name may be mangled (e.g. Solaris).
     * @return
     */
    protected long getBootHeapStartSymbolAddress() {
        return symbolLookup.lookupSymbolValue(HEAP_SYMBOL_NAME).longValue();
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

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        final ELFProgramHeaderTable.Entry64 entry64 = findAddress(src);
        if (entry64 == null) {
            return 0;
        }
        try {
            dumpRaf.seek(entry64.p_offset + (src - entry64.p_vaddr));
            return dumpRaf.read(dst, dstOffset, length);
        } catch (IOException ex) {
            return 0;
        }

    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        unimplemented("readRegisters");
        return false;
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
