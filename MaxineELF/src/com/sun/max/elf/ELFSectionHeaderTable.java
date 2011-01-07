/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
/**
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created Sep 5, 2005
 */
package com.sun.max.elf;

import com.sun.max.program.ProgramError;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The <code>ELFSectionHeaderTable</code> class represents a cleaned-up view
 * of the ELF format's section header table that contains a summary of each
 * section in the ELF file. Each section might contain code, data, symbol
 * table information, etc. This class reads this header table from a
 * random access file.
 *
 * @author Ben L. Titzer
 */
public class ELFSectionHeaderTable {

    public static final int SHT_NULL = 0;
    public static final int SHT_PROGBITS = 1;
    public static final int SHT_SYMTAB = 2;
    public static final int SHT_STRTAB = 3;
    public static final int SHT_RELA = 4;
    public static final int SHT_HASH = 5;
    public static final int SHT_DYNAMIC = 6;
    public static final int SHT_NOTE = 7;
    public static final int SHT_NOBITS = 8;
    public static final int SHT_REL = 9;
    public static final int SHT_SHLIB = 10;
    public static final int SHT_DYNSYM = 11;
    public static final int SHT_LOPROC = 0x70000000;
    public static final int SHT_HIPROC = 0x7fffffff;
    public static final int SHT_LOUSER = 0x80000000;
    public static final int SHT_HIUSER = 0x8fffffff;

    public static final int SHF_WRITE = 0x1;
    public static final int SHF_ALLOC = 0x2;
    public static final int SHF_EXECINSTR = 0x4;
    public static final int SHF_MASKPROC = 0xf0000000;

    private static final int ELF32_SHTENT_SIZE = 40;
    private static final int ELF64_SHTENT_SIZE = 64;

    /************************************************************************/
    private static final int SHN_UNDEF = 0;
    private static final short NO_OF_SECTIONS = 5;

    /************************************************************************/


    public abstract class Entry {
        public abstract int getType();

        public String getTypeString() {
            switch (getType()) {
                case SHT_NULL:
                    return "null";
                case SHT_PROGBITS:
                    return "program";
                case SHT_SYMTAB:
                    return "symtab";
                case SHT_STRTAB:
                    return "strtab";
                case SHT_RELA:
                    return "rela";
                case SHT_HASH:
                    return "hash";
                case SHT_DYNAMIC:
                    return "dynamic";
                case SHT_NOTE:
                    return "note";
                case SHT_NOBITS:
                    return "nobits";
                case SHT_REL:
                    return "rel";
                case SHT_SHLIB:
                    return "shlib";
                case SHT_DYNSYM:
                    return "dynsym";
                default:
                    return "unknown";
            }
        }

        public String getFlagString() {
            final StringBuffer buffer = new StringBuffer();
            final int flags = getFlags();
            if ((flags & SHF_WRITE) != 0) {
                buffer.append("WRITE ");
            }
            if ((flags & SHF_ALLOC) != 0) {
                buffer.append("ALLOC ");
            }
            if ((flags & SHF_EXECINSTR) != 0) {
                buffer.append("EXEC ");
            }
            return buffer.toString();
        }

        public boolean isStringTable() {
            return getType() == SHT_STRTAB;
        }

        public boolean isSymbolTable() {
            return getType() == SHT_SYMTAB;
        }

        public boolean is32Bit() {
            return this instanceof Entry32;
        }

        public boolean is64Bit() {
            return this instanceof Entry64;
        }

        protected abstract int getFlags();
        public abstract String getName();
        public abstract long getOffset();
        public abstract long getSize();
        public abstract int getLink();
        public abstract int getEntrySize();

        public abstract void setEntrySize(long size);
    }

    public class Entry32 extends Entry {
        public int sh_name;
        public int sh_type;
        public int sh_flags;
        public int sh_addr;
        public int sh_offset;
        public int sh_size;
        public int sh_link;
        public int sh_info;
        public int sh_addralign;
        public int sh_entsize;

        @Override
        public int getType() {
            return sh_type;
        }

        @Override
        protected int getFlags() {
            return sh_flags;
        }

        @Override
        public String getName() {
            if (strtab != null) {
                return strtab.getString(sh_name);
            }
            return "";
        }

        @Override
        public long getOffset() {
            return sh_offset;
        }

        @Override
        public long getSize() {
            return sh_size;
        }

        @Override
        public int getLink() {
            return sh_link;
        }

        @Override
        public int getEntrySize() {
            return sh_entsize;
        }
        @Override
        public void setEntrySize(long size) {
            sh_size = (int) size;
        }
    }

    public class Entry64 extends Entry {
        public int sh_name;
        public int sh_type;
        public long sh_flags;
        public long sh_addr;
        public long sh_offset;
        public long sh_size;
        public int sh_link;
        public int sh_info;
        public long sh_addralign;
        public long sh_entsize;

        @Override
        public int getType() {
            return sh_type;
        }

        @Override
        protected int getFlags() {
            return (int) sh_flags;
        }

        @Override
        public String getName() {
            if (strtab != null) {
                return strtab.getString(sh_name);
            }
            return "";
        }
        @Override
        public long getOffset() {
            return sh_offset;
        }

        @Override
        public long getSize() {
            assert sh_size < Long.MAX_VALUE;
            return sh_size;
        }

        @Override
        public int getLink() {
            return sh_link;
        }
        @Override
        public int getEntrySize() {
            assert sh_entsize < Integer.MAX_VALUE;
            return (int) sh_entsize;
        }

        @Override
        public void setEntrySize(long size) {
            sh_size = size;
        }
    }

    public final ELFHeader header;
    public final Entry[] entries;
    protected ELFStringTable strtab;
    private long offsetCount;
    /**
     * The constructor for the <code>ELFSectionHeaderTable</code> class creates a new instance
     * corresponding to the specified ELF header. The ELF header contains an entry that
     * stores the offset of the beginning of the section header table relative to the start
     * of the file.
     * @param elfHeader the ELF header containing information about this ELF file
     */
    public ELFSectionHeaderTable(ELFHeader elfHeader) {
        this.header = elfHeader;
        entries = new Entry[elfHeader.e_shnum];
        offsetCount = 0;
    }

    /**
     * The <code>read()</code> method reads the section header table from the specified
     * file. The file must support random access, since the beginning offset of the table
     * is specified in the header table informationi.
     * @param fis the random access file that contains the section header table
     * @throws IOException if there is a problem reading the data from the file
     */
    public void read(RandomAccessFile fis) throws IOException {
        if (entries.length == 0) {
            return;
        }
        // seek to the beginning of the section header table
        fis.seek(header.e_shoff);
        final ELFDataInputStream is = new ELFDataInputStream(header, fis);
        // load each of the section header entries
        for (int cntr = 0; cntr < entries.length; cntr++) {
            entries[cntr] = readEntry(fis, is);
        }
    }

    private Entry readEntry(RandomAccessFile fis, ELFDataInputStream is) throws IOException {
        if (header.is32Bit()) {
            return readEntry32(fis, is);
        } else if (header.is64Bit()) {
            return readEntry64(fis, is);
        }
        throw ProgramError.unexpected("unknown bit size");
    }

    private Entry32 readEntry32(RandomAccessFile fis, ELFDataInputStream is) throws IOException {
        final Entry32 e = new Entry32();
        e.sh_name      = is.read_Elf32_Word();
        e.sh_type      = is.read_Elf32_Word();
        e.sh_flags     = is.read_Elf32_Word();
        e.sh_addr      = is.read_Elf32_Addr();
        e.sh_offset    = is.read_Elf32_Off();
        e.sh_size      = is.read_Elf32_Word();
        e.sh_link      = is.read_Elf32_Word();
        e.sh_info      = is.read_Elf32_Word();
        e.sh_addralign = is.read_Elf32_Word();
        e.sh_entsize   = is.read_Elf32_Word();

        for (int pad = ELF32_SHTENT_SIZE; pad < header.e_shentsize; pad++) {
            fis.read();
        }
        return e;
    }

    private Entry64 readEntry64(RandomAccessFile fis, ELFDataInputStream is) throws IOException {
        final Entry64 e = new Entry64();
        e.sh_name      = is.read_Elf64_Word();   // 4
        e.sh_type      = is.read_Elf64_Word();   // 4
        e.sh_flags     = is.read_Elf64_XWord();  // 8
        e.sh_addr      = is.read_Elf64_Addr();   // 8
        e.sh_offset    = is.read_Elf64_Off();    // 8
        e.sh_size      = is.read_Elf64_XWord();  // 8
        e.sh_link      = is.read_Elf64_Word();   // 4
        e.sh_info      = is.read_Elf64_Word();   // 4
        e.sh_addralign = is.read_Elf64_XWord();  // 8
        e.sh_entsize   = is.read_Elf64_XWord();  // 8


        for (int pad = ELF64_SHTENT_SIZE; pad < header.e_shentsize; pad++) {
            fis.read();
        }
        return e;
    }

    public void setStringTable(ELFStringTable str) {
        strtab = str;
    }

    public ELFStringTable getStringTable() {
        return strtab;
    }

    public String getSectionName(int ind) {
        if (ind < 0 || ind >= entries.length) {
            return "";
        }
        return entries[ind].getName();
    }

    public String setSectionName(int cntr) throws Exception {
        switch (cntr) {
            case 0:
                return "";
            case 1:
                return "maxvm_image";
            case 2:
                return ".shstrtab";
            case 3:
                return ".symtab";
            case 4:
                return ".strtab";
            default:
                break;
        }
        throw ProgramError.unexpected("unknown section number");
    }
    Entry64  setSectionHeaderForNull(Entry64 e) throws IOException {
        e.sh_name = strtab.getIndex("\0");
        if (e.sh_name == -1) {
            throw ProgramError.unexpected("Unknown Section Name");
        }
        e.sh_type = SHT_NULL;
        e.sh_addr = 0;   // If the section will appear in the memory image of a process then it should contain the address else it should be 0.
        e.sh_addralign = 0;  // 0 or 1 means that this sections has no alignment constraints.
        e.sh_entsize = 0;  // If the section does not hold fixed sized entries then this should be 0.
        e.sh_flags = 0;
        e.sh_info = 0;
        e.sh_link = 0;
        e.sh_size = 0;
        e.sh_offset = 0;
        return e;
    }

    Entry64 setSectionHeaderForMaxvm(Entry64 e, long size) throws IOException {
        e.sh_name = strtab.getIndex("maxvm_image");
        if (e.sh_name == -1) {
            throw ProgramError.unexpected("Unknown Section Name");
        }
        e.sh_type = SHT_PROGBITS;
        e.sh_addr = 0;   // If the section will appear in the memory image of a process then it should contain the address else it should be 0.
        e.sh_addralign = 0x1;  // 0 or 1 means that this sections has no alignment constraints.
        e.sh_entsize = 0;  // If the section does not hold fixed sized entries then this should be 0.
        e.sh_flags = SHF_ALLOC;
        e.sh_info = 0;
        e.sh_link = SHN_UNDEF;
        e.sh_size = size;


        e.sh_offset = offsetCount + header.e_ehsize;  // This is the first section after the header section.
        offsetCount = e.sh_offset + size;

        return e;
    }

    Entry64 setSectionHeaderForShStrTab(Entry64 e, long size) throws IOException {
        e.sh_name = strtab.getIndex(".shstrtab");
        if (e.sh_name == -1) {
            throw ProgramError.unexpected("Unknown Section Name");
        }
        e.sh_type = SHT_STRTAB;
        e.sh_addr = 0;   // If the section will appear in the memory image of a process then it should contain the address else it should be 0.
        e.sh_addralign = 0x1;  // 0 or 1 means that this sections has no alignment constraints.
        e.sh_entsize = 0;  // If the section does not hold fixed sized entries then this should be 0.
        e.sh_flags = SHF_ALLOC;
        e.sh_info = 0;
        e.sh_link = SHN_UNDEF;
        e.sh_size = strtab.getStringLength();


        e.sh_offset = offsetCount;
        offsetCount += e.sh_size;
        return e;
    }

    Entry64  setSectionHeaderForSymTab(Entry64 e) throws IOException {
        e.sh_name = strtab.getIndex(".symtab");
        if (e.sh_name == -1) {
            throw ProgramError.unexpected("Unknown Section Name");
        }

        e.sh_type = SHT_SYMTAB;
        e.sh_link = 4; // this is the value of the position of the symbol table header in the section header table.
        e.sh_addr = 0;   // If the section will appear in the memory image of a process then it should contain the address else it should be 0.
        // the addr align is 8 since this section contains long variables which are 8 bytes long and hence the address alignment should be 8.
        e.sh_addralign = 0x8;
        e.sh_entsize = 24;  // If the section does not hold fixed sized entries then this should be 0.
        e.sh_flags = 0;
        e.sh_info = 2; //One greater than the symbol table index of the last local symbol (binding STB_LOCAL)

        e.sh_size = e.sh_entsize * 4; // Totally 4 symbols.
        if (offsetCount % 8 != 0) {
            offsetCount += 8 - (offsetCount % 8);
        }
        e.sh_offset = offsetCount;
        offsetCount += e.sh_size;
        return e;
    }

    Entry64  setSectionHeaderForStrTab(Entry64 e) {
        e.sh_type = SHT_STRTAB;
        e.sh_addr = 0;   // If the section will appear in the memory image of a process then it should contain the address else it should be 0.
        e.sh_addralign = 0x1;  // 0 or 1 means that this sections has no alignment constraints.
        e.sh_entsize = 0;  // If the section does not hold fixed sized entries then this should be 0.
        e.sh_flags = 0;
        e.sh_info = 0;
        e.sh_link = SHN_UNDEF;

        e.sh_name = strtab.getIndex(".strtab");
        if (e.sh_name == -1) {
            System.out.println("ERROR");
        }

        e.sh_size = 0x00; // The size of the this section is to be filled up later.
        e.sh_offset = offsetCount; // The offset count to the end of the section will be filled up later
        return e;
    }

    public long getOffsetCount() {
        return offsetCount;
    }

    public void setOffsetCount(long offset) {
        offsetCount = offset;
    }


    public void write(long size) throws IOException {
        if (entries.length == 0) {
            return;
        }

        String sectionName = "";

        // The Symbol table and the string table associated with the symbol table are
        // created later. We first create the section headers and use the values in those
        // to create the symbol table and its string table.

        // load each of the section header entries
        for (int cntr = 0; cntr < NO_OF_SECTIONS; cntr++) {
            try {
                sectionName = setSectionName(cntr);
                entries[cntr] = writeEntry(sectionName, size);
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    private Entry writeEntry(String sectionName, long size) throws IOException {
        if (header.is32Bit()) {
            return writeEntry32();
        } else if (header.is64Bit()) {
            return writeEntry64(sectionName, size);
        }
        throw ProgramError.unexpected("unknown bit size");
    }

    private Entry writeEntry32() {
        Entry32 e = new Entry32();
        return e;
    }

    private Entry writeEntry64(String sectionName, long size) throws IOException {
        Entry64 e = new Entry64();
        if (sectionName.equalsIgnoreCase("")) {
            e = setSectionHeaderForNull(e);
        }
        if (sectionName.equalsIgnoreCase("maxvm_image")) {
            e = setSectionHeaderForMaxvm(e, size);
        } else if (sectionName.equalsIgnoreCase(".shstrtab")) {
            e = setSectionHeaderForShStrTab(e, size);
        } else if (sectionName.equalsIgnoreCase(".symtab")) {
            e = setSectionHeaderForSymTab(e);
        } else if (sectionName.equalsIgnoreCase(".strtab")) {
            e = setSectionHeaderForStrTab(e);
        }
        return e;
    }

    public void writeSectionHeadersToFile64(ELFDataOutputStream os, RandomAccessFile fis)  throws IOException {
        for (int cntr = 0; cntr < NO_OF_SECTIONS; cntr++) {
            Entry ent = entries[cntr];
            Entry64 e64 = (Entry64) ent;
            os.write_Elf64_Word(e64.sh_name);
            os.write_Elf64_Word(e64.sh_type);
            os.write_Elf64_XWord(e64.sh_flags);
            os.write_Elf64_Addr(e64.sh_addr);
            os.write_Elf64_Off(e64.sh_offset);
            os.write_Elf64_XWord(e64.sh_size);
            os.write_Elf64_Word(e64.sh_link);
            os.write_Elf64_Word(e64.sh_info);
            os.write_Elf64_XWord(e64.sh_addralign);
            os.write_Elf64_XWord(e64.sh_entsize);
        }
    }

}
