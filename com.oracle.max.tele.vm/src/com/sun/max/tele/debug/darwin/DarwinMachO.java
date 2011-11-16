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
import java.nio.*;


/**
 * (Limited) Access to Mach_O 64-bit format files. See /usr/include/mach-o/*.h for source.
 * Note that a file may (unusually) contain multiple binaries for different architectures,
 * see /usr/include/mach-o/fat.h. Such a file is called a universal binary file, (cf an archive file).
 *
 */
public class DarwinMachO {

    /**
     * Encapsulates whether the MachO file is embedded in a universal binary file.
     */
    public static class MachORandomAccessFile extends RandomAccessFile {

        /**
         * If non-zero, offset to start of architecture-specific MachO file in FAT file.
         */
        private int fatOffset;

        private MachORandomAccessFile(String path) throws FileNotFoundException {
            super(path, "r");
        }

        @Override
        public void seek(long filePos) throws IOException {
            super.seek(filePos + fatOffset);
        }

        @Override
        public long getFilePointer() throws IOException {
            long fp = super.getFilePointer();
            return fp - fatOffset;
        }
    }

    public final MachORandomAccessFile raf;
    private Header header;
    private LoadCommand[] loadCommands;

    public DarwinMachO(String path) throws FileNotFoundException, IOException {
        raf = new MachORandomAccessFile(path);
        header = new Header();
    }

    private class FatArch {
        final static int X86_64 = 0x01000007;
        int cputype;
        int cpusubtype;
        int offset;
        int size;
        int align;

        private FatArch() throws IOException {
            cputype = raf.readInt();
            cpusubtype = raf.readInt();
            offset = raf.readInt();
            size = raf.readInt();
            align = raf.readInt();
        }
    }

    public class Header {
        private static final int FAT_MAGIC = 0xcafebabe;
        public final int magic;
        public final int cputype;
        public final int cpusubtype;
        public final int filetype;
        public final int ncmds;
        public final int sizeofcmds;
        public final int flags;
        public final int reserved;

        private Header() throws IOException {
            int magic = raf.readInt();
            boolean found = false;
            if (magic == FAT_MAGIC) {
                // find our architecture subfile
                int nFatArch = raf.readInt();
                for (int i = 0; i < nFatArch; i++) {
                    FatArch fatArch = new FatArch();
                    if (fatArch.cputype == FatArch.X86_64) {
                        found = true;
                        raf.seek(fatArch.offset);
                        raf.fatOffset = fatArch.offset;
                        magic = readInt();
                        break;
                    }
                }
                assert found : "failed to find X86_64 architecture in MachO FAT file";
            }
            this.magic = magic;
            cputype = readInt();
            cpusubtype = readInt();
            filetype = readInt();
            ncmds = readInt();
            sizeofcmds = readInt();
            flags = readInt();
            reserved = readInt();

        }

    }

    public Header getHeader() throws IOException {
        if (header == null) {
            header = new Header();
        }
        return header;
    }

    /**
     * Common base class for all Mach-O load command types.
     */
    public class LoadCommand {
        public static final int LC_SEGMENT_64 = 0x19;
        public static final int LC_THREAD = 0x4;
        public static final int LC_SYMTAB = 0x2;

        public final int cmd;
        public final int cmdsize;

        protected LoadCommand() throws IOException {
            this.cmd = readInt();
            this.cmdsize = readInt();
        }

        public String typeName() {
            switch (cmd) {
                case LC_SEGMENT_64:
                    return "LC_SEGMENT_64";
                case LC_THREAD:
                    return "LC_THREAD";
                case LC_SYMTAB:
                    return "LC_SYMTAB";
                default:
                    return "LC #" + cmd;
            }
        }

    }

    /**
     * Reads a load command structure starting at the current file position, invoking
     * the appropriate subclass {@code read} command, based on the {@code cmd} field.
     * Leaves the file pointer at the next load command (if any).
     *
     * @return instance of the appropriate subclass for discovered command type
     * @throws IOException
     */
    private LoadCommand readNextLoadCommand() throws IOException {
        LoadCommand result = null;
        final long ptr = raf.getFilePointer();
        final int cmd = readInt();
        final int cmdsize = readInt();
        raf.seek(ptr);
        switch (cmd) {
            case LoadCommand.LC_SEGMENT_64:
                result = new Segment64LoadCommand();
                break;
            case LoadCommand.LC_THREAD:
                result = new ThreadLoadCommand();
                break;
            case LoadCommand.LC_SYMTAB:
                result = new SymTabLoadCommand();
                break;
            default:
                result = new LoadCommand();
        }
        // skip over entire command
        raf.seek(ptr + cmdsize);
        return result;
    }

    public LoadCommand[] getLoadCommands() throws IOException {
        if (loadCommands == null) {
            getHeader();
            loadCommands = new LoadCommand[header.ncmds];
            for (int i = 0; i < header.ncmds; i++) {
                loadCommands[i] = readNextLoadCommand();
            }
        }
        return loadCommands;
    }

    public final class Segment64LoadCommand extends LoadCommand {
        public final String segName;
        public final long vmaddr;
        public final long vmsize;
        public final long fileoff;
        public final long filesize;
        public final int maxprot;
        public final int initprot;
        public final int nsects;
        public final int flags;
        public final Section64[] sections;

        private Segment64LoadCommand() throws IOException {
            final byte[] segname = new byte[16];
            for (int i = 0; i < 16; i++) {
                segname[i] = raf.readByte();
            }
            segName = new String(segname);
            vmaddr = readLong();
            vmsize = readLong();
            fileoff = readLong();
            filesize = readLong();
            maxprot = readInt();
            initprot = readInt();
            nsects = readInt();
            flags = readInt();
            sections = new Section64[nsects];
            for (int i = 0; i < nsects; i++) {
                sections[i] = new Section64(this);
            }
        }

    }

    public class Section64 {
        public final String sectname;
        public final String segname;
        public final long addr;
        public final long size;
        public final int offset;
        public final int align;
        public final int reloff;
        public final int nreloc;
        public final int flags;
        public final int reserved1;
        public final int reserved2;
        public final int reserved3;
        public final Segment64LoadCommand owningSegment;

        private Section64(Segment64LoadCommand segment64) throws IOException {
            owningSegment = segment64;
            sectname = readName();
            segname = readName();
            addr = readLong();
            size = readLong();
            offset = readInt();
            align = readInt();
            reloff = readInt();
            nreloc = readInt();
            flags = readInt();
            reserved1 = readInt();
            reserved2 = readInt();
            reserved3 = readInt();
        }

        private String readName() throws IOException {
            byte[] nameBytes = new byte[16];
            int length = 0;
            for (int i = 0; i < nameBytes.length; i++) {
                nameBytes[i] = raf.readByte();
                if (nameBytes[i] != 0) {
                    length++;
                }
            }
            return new String(nameBytes, 0, length);
        }

        public boolean isText() {
            return segname.equals("__TEXT");
        }
    }

    public class ThreadLoadCommand extends LoadCommand {
        public final ThreadRegState regstate;
        public final ThreadFPRegState fpregstate;
        public final ThreadExceptionState exstate;

        ThreadLoadCommand() throws IOException {
            regstate = new ThreadRegState();
            fpregstate = new ThreadFPRegState();
            exstate = new ThreadExceptionState();
        }

    }

    public class ThreadState {
        public final int flavor;
        public final int count;

        protected ThreadState() throws IOException {
            flavor = readInt();
            count = readInt();
        }
    }

    public class ThreadRegState extends ThreadState {
        public final int tsh_flavor;
        public final int tsh_count;
        public final ByteBuffer regbytes; // we also store registers as a directly allocated ByteBuffer for passing to native code
        public final long      rax;
        public final long      rbx;
        public final long      rcx;
        public final long      rdx;
        public final long      rdi;
        public final long      rsi;
        public final long      rbp;
        public final long      rsp;
        public final long      r8;
        public final long      r9;
        public final long      r10;
        public final long      r11;
        public final long      r12;
        public final long      r13;
        public final long      r14;
        public final long      r15;
        public final long      rip;
        public final long      rflags;
        public final long      cs;
        public final long      fs;
        public final long      gs;

        ThreadRegState() throws IOException {
            tsh_flavor = readInt();
            tsh_count = readInt();
            final long ptr = raf.getFilePointer();
            rax = readLong();
            rbx = readLong();
            rcx = readLong();
            rdx = readLong();
            rdi = readLong();
            rsi = readLong();
            rbp = readLong();
            rsp = readLong();
            r8 = readLong();
            r9 = readLong();
            r10 = readLong();
            r11 = readLong();
            r12 = readLong();
            r13 = readLong();
            r14 = readLong();
            r15 = readLong();
            rip = readLong();
            rflags = readLong();
            cs = readLong();
            fs = readLong();
            gs = readLong();
            raf.seek(ptr);
            // read and store again as byte array
            regbytes = ByteBuffer.allocateDirect(tsh_count * 4);
            for (int i = 0; i < tsh_count * 4; i++) {
                regbytes.put(raf.readByte());
            }
        }

    }

    public class ThreadFPRegState extends ThreadState {
        public final int fsh_flavor;
        public final int fsh_count;
        ByteBuffer regbytes; // way too complex for individual declarations; do it all via native code

        ThreadFPRegState() throws IOException {
            fsh_flavor = readInt();
            fsh_count = readInt();
            regbytes = ByteBuffer.allocateDirect(fsh_count * 4);
            for (int i = 0; i < fsh_count * 4; i++) {
                regbytes.put(raf.readByte());
            }
        }

    }

    public class ThreadExceptionState extends ThreadState {
        public final int esh_flavor;
        public final int esh_count;
        public final int trapno;
        public final int err;
        public final long faultvaddr;

        ThreadExceptionState() throws IOException {
            super();
            esh_flavor = readInt();
            esh_count = readInt();
            trapno = readInt();
            err = readInt();
            faultvaddr = readLong();
        }

    }

    public class SymTabLoadCommand extends LoadCommand {
        public final int symoff;
        public final int nsyms;
        public final int stroff;
        public final int strsize;
        /**
         * Lazily created string table.
         */
        private byte[] stringTable;
        /**
         * Lazily created symbol table.
         */
        private NList64[] symbolTable;

        SymTabLoadCommand() throws IOException {
            super();
            symoff = readInt();
            nsyms = readInt();
            stroff = readInt();
            strsize = readInt();
        }

        public NList64[] getSymbolTable() throws IOException {
            if (symbolTable != null) {
                return symbolTable;
            }
            stringTable = new byte[strsize];
            raf.seek(stroff);
            for (int i = 0; i < strsize; i++) {
                stringTable[i] = raf.readByte();
            }
            symbolTable = new NList64[nsyms];
            raf.seek(symoff);
            for (int i = 0; i < nsyms; i++) {
                symbolTable[i] = new NList64();
            }
            return symbolTable;
        }

        public String getSymbolName(NList64 nlist64) {
            String symbol = "";
            if (nlist64.strx != 0) {
                byte sb = stringTable[nlist64.strx];
                int sl = 0;
                while (sb != 0) {
                    sb = stringTable[nlist64.strx + sl];
                    sl++;
                }
                // remove leading/trailing underscores which bracket all symbols
                symbol = new String(stringTable, nlist64.strx + 1, sl - 2);
            }
            return symbol;
        }

    }

    public class NList64 {
        public static final int STAB = 0xe0;
        public static final int PEXT = 0x10;
        public static final int TYPE = 0xe;
        public static final int EXT = 0x1;

        public static final int UNDF = 0x0;
        public static final int ABS  = 0x2;
        public static final int SECT = 0xe;
        public static final int PBUD = 0xc;
        public static final int INDR = 0xa;

        public final int strx;
        public final byte type;
        public final byte sect;
        public final short desc;
        public final long value;

        NList64() throws IOException {
            strx = readInt();
            type = raf.readByte();
            sect = raf.readByte();
            desc = readShort();
            value = readLong();
        }
    }

    /**
     * Locates a given section within a given array of load commands.
     * Sections are numbered from 1 as they occur within SEGMENT_64 commands.
     * @param loadCommands
     * @param sectToFind
     * @return
     */
    public static Section64 getSection(LoadCommand[] loadCommands, int sectToFind) {
        int sect = 1;
        for (int i = 0; i < loadCommands.length; i++) {
            if (loadCommands[i].cmd == LoadCommand.LC_SEGMENT_64) {
                Segment64LoadCommand slc = (Segment64LoadCommand) loadCommands[i];
                if (sectToFind < sect + slc.nsects) {
                    return slc.sections[sectToFind - sect];
                }
                sect += slc.nsects;
            }
        }
        return null;
    }

    public short readShort() throws IOException {
        final int b1 = raf.read();
        final int b2 = raf.read();
        return (short) (((b2 << 8) | b1) & 0xFFFF);
    }

    public int readInt() throws IOException {
        final int b1 = raf.read();
        final int b2 = raf.read();
        final int b3 = raf.read();
        final int b4 = raf.read();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    public long readLong() throws IOException {
        final long lw = readInt();
        final long hw = readInt();
        return hw << 32 | (lw & 0xFFFFFFFFL);
    }

}
