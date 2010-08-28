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
package com.sun.max.tele.debug.solaris;

import static com.sun.max.elf.ELFProgramHeaderTable.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.elf.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.unix.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Solaris implementation of the channel protocol for accessing core dump files.
 *
 * @author Mick Jordan
 *
 */
public class SolarisDumpTeleChannelProtocol extends UnixDumpTeleChannelProtocolAdaptor implements SolarisTeleChannelProtocol {

    private RandomAccessFile dumpRaf;
    private ELFHeader header;
    private ELFProgramHeaderTable programHeaderTable;
    private ELFProgramHeaderTable.Entry64 noteSectionEntry;
    private ELFSymbolLookup symbolLookup;
    private static final String HEAP_SYMBOL_NAME = "theHeap";  // image.c
    private List<LwpData> lwpDataList;
    private SolarisDumpThreadAccess solarisDumpThreadAccess;

    static class LwpData {
        // direct buffers
        ByteBuffer lwpStatus;
        ByteBuffer lwpInfo;

        LwpData(ByteBuffer lwpInfo) {
            this.lwpInfo = lwpInfo;
        }
    }

    public SolarisDumpTeleChannelProtocol(File vm, File dumpFile) {
        super(vm, dumpFile);
        try {
            dumpRaf = new RandomAccessFile(dumpFile, "r");
            this.header = ELFLoader.readELFHeader(dumpRaf);
            this.programHeaderTable = ELFLoader.readPHT(dumpRaf, header);
            symbolLookup = new ELFSymbolLookup(new File(vm.getParent(), "libjvm.so"));
        } catch (Exception ex) {
            FatalError.unexpected("failed to open dump file: " + dumpFile, ex);
        }
        processNoteSection();
        assert noteSectionEntry != null;
    }

    /**
     * A workaround until this can be done properly.
     * The boot heap is a large segment, in fact it is the second largest segment, the largest
     * being the terabyte allocated for the dynamic heap.
     * @return the base of the largest loaded segment which we trust is the boot heap!
     */
    static final long TERA_BYTE = 0x10000000L;
    private long getBootHeapStartHack() {
        long result = 0;
        long maxSize = 0;
        for (ELFProgramHeaderTable.Entry entry : programHeaderTable.entries) {
            if (entry.p_type == PT_LOAD) {
                ELFProgramHeaderTable.Entry64 entry64 = (ELFProgramHeaderTable.Entry64) entry;
                if (entry64.p_filesz > 0) {
                    if (entry64.p_filesz > maxSize && entry64.p_filesz != TERA_BYTE) {
                        maxSize = entry64.p_filesz;
                        result = entry64.p_vaddr;
                    }
                }
            }
        }
        return result;
    }

    private void processNoteSection() {
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
            LwpData[] lwpDataArray = null;
            int lwpDataArrayIndex = 0;
            while (readLength < size) {
                int namesz = dis.read_Elf64_Word();
                final int descsz = dis.read_Elf64_Word();
                final int type = dis.read_Elf64_Word();
                readNoteString(dis, (int) namesz);
                readLength += 12 + namesz;
                while (namesz % 8 != 0) {
                    dis.read_Elf32_byte();
                    readLength++;
                    namesz++;
                }
                final byte[] desc = readNoteDesc(dis, (int) descsz);
                readLength += descsz;

                final NoteType noteType = NoteType.get(type);
                switch (noteType) {
                    case NT_PSTATUS:
                        final int numLwps = readInt(desc, 4);
                        lwpDataArray = new LwpData[numLwps];
                        break;

                    case NT_LWPSINFO: {
                        // this comes before NT_LWPSTATUS
                        ByteBuffer lwpInfo = ByteBuffer.allocateDirect(descsz);
                        lwpInfo.put(desc);
                        lwpDataArray[lwpDataArrayIndex] = new LwpData(lwpInfo);
                        break;
                    }

                    case NT_LWPSTATUS: {
                        ByteBuffer lwpStatus = ByteBuffer.allocateDirect(descsz);
                        lwpStatus.put(desc);
                        lwpDataArray[lwpDataArrayIndex].lwpStatus = lwpStatus;
                        lwpDataArrayIndex++;
                        break;
                    }
                }
            }
            lwpDataList = Arrays.asList(lwpDataArray);
        } catch (IOException ex) {
            FatalError.unexpected("error reading dump file note section", ex);
        }
    }

    private int readInt(byte[] data, int index) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt(index);
    }

    private static enum NoteType {
        NT_PRSTATUS(1),
        NT_PRFPREG(2),
        NT_PRPSINFO(3),
        NT_PRXREG(4),
        NT_PLATFORM(5),
        NT_AUXV(6),
        NT_GWINDOWS(7),
        NT_ASRS(8),
        NT_LDT(9),
        NT_PSTATUS(10),
        NT_PSINFO(13),
        NT_PRCRED(14),
        NT_UTSNAME(15),
        NT_LWPSTATUS(16),
        NT_LWPSINFO(17),
        NT_PRPRIV(18),
        NT_PRPRIVINFO(19),
        NT_CONTENT(20),
        NT_ZONENAME(21);

        int value;

        static NoteType get(int type) {
            for (NoteType noteType : values()) {
                if (noteType.value == type) {
                    return noteType;
                }
            }
            return null;
        }

        NoteType(int value) {
            this.value = value;
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

    private ELFProgramHeaderTable.Entry64 findAddress(long addr) {
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

    @Override
    public boolean initialize(int threadLocalsAreaSize, boolean bigEndian) {
        super.initialize(threadLocalsAreaSize, bigEndian);
        solarisDumpThreadAccess = new SolarisDumpThreadAccess(this, threadLocalsAreaSize, lwpDataList);
        return true;
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
    public long getBootHeapStart() {
        if (true) {
            return getBootHeapStartHack();
        } else {
            // Either theHeap needs to be declared as global or we have to do a lookup that avoids
            // the .X... text, as this varies with every link.
            final long theHeapAddress = symbolLookup.lookupSymbolValue(".XAYaEDyz0vbMGnt." + HEAP_SYMBOL_NAME).longValue();
            ELFProgramHeaderTable.Entry64 entry64 = findAddress(theHeapAddress);
            try {
                dumpRaf.seek(entry64.p_offset + (theHeapAddress - entry64.p_vaddr));
                ELFDataInputStream ds = new ELFDataInputStream(header, dumpRaf);
                return ds.read_Elf64_Addr();
            } catch (Throwable ex) {
                FatalError.unexpected("failed to get boot heap address", ex);
                return 0;
            }
        }
        // return 0xfffffc7ffee00000L;
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        Trace.line(2, "WARNING: Inspector trying to write to " + Long.toHexString(dst));
        return length;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        SolarisDumpThreadAccess.SolarisThreadInfo threadInfo = (SolarisDumpThreadAccess.SolarisThreadInfo) solarisDumpThreadAccess.getThreadInfo((int) threadId);
        System.arraycopy(threadInfo.integerRegisters, 0, integerRegisters, 0, integerRegisters.length);
        System.arraycopy(threadInfo.floatingPointRegisters, 0, floatingPointRegisters, 0, floatingPointRegisters.length);
        System.arraycopy(threadInfo.stateRegisters, 0, stateRegisters, 0, stateRegisters.length);
        return true;
    }

    @Override
    public boolean gatherThreads(Object teleProcessObject, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        return solarisDumpThreadAccess.gatherThreads(teleProcessObject, threadSequence, threadLocalsList, primordialThreadLocals);
    }

}

