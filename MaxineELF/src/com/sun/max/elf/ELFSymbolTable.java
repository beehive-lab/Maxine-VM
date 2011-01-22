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
 * The <code>ELFSymbolTable</code> class represents a symbol table within
 * an ELF file. The symbol table is used to locate functions and variables
 * for relocation and debugging purposes.
 *
 * @author Ben L. Titzer
 */
public class ELFSymbolTable {

    public static final int STT_NOTYPE  = 0;
    public static final int STT_OBJECT  = 1;
    public static final int STT_FUNC    = 2;
    public static final int STT_SECTION = 3;
    public static final int STT_FILE    = 4;
    public static final int STT_LOPROC  = 13;
    public static final int STT_HIPROC  = 15;

    public static final int STB_LOCAL = 0;
    public static final int STB_GLOBAL = 1;
    public static final int STB_WEAK = 2;
    public static final int STB_LOPROC = 13;
    public static final int STB_HIPROC = 15;

    private static final int ELF32_STENT_SIZE = 16;
    private static final int ELF64_STENT_SIZE = 24;


    public abstract class Entry {


        public abstract int getInfo();

        public String getBinding() {
            switch ((getInfo() >> 4) & 0xf) {
                case STB_LOCAL:
                    return "LOCAL";
                case STB_GLOBAL:
                    return "GLOBAL";
                case STB_WEAK:
                    return "WEAK";
                default:
                    return "unknown";
            }
        }

        public String getType() {
            switch (getInfo() & 0xf) {
                case STT_NOTYPE:
                    return "n";
                case STT_OBJECT:
                    return "object";
                case STT_FUNC:
                    return "func";
                case STT_SECTION:
                    return "section";
                case STT_FILE:
                    return "file";
                default:
                    return "unknown";
            }
        }

        public boolean isFunction() {
            return (getInfo() & 0xf) == STT_FUNC;
        }

        public boolean isObject() {
            return (getInfo() & 0xf) == STT_OBJECT;
        }

        public abstract int getNameIndex();

        public boolean is32Bit() {
            return this instanceof Entry32;
        }

        public boolean is64Bit() {
            return this instanceof Entry64;
        }

        public String getName() {
            if (strtab != null) {
                return strtab.getString(getNameIndex());
            }
            return "";
        }

        public abstract short getSectionHeaderIndex();
    }

    public class Entry32 extends Entry {
        public int st_name;
        public int st_value;
        public int st_size;
        public int st_info;
        public int st_other;
        public short st_shndx;

        @Override
        public int getInfo() {
            return st_info;
        }

        @Override
        public int getNameIndex() {
            return st_name;
        }

        @Override
        public short getSectionHeaderIndex() {
            return st_shndx;
        }
    }

    public class Entry64 extends Entry {
        public int st_name;
        public int st_info; // Should be a byte
        public int st_other; // Should be a byte
        public short st_shndx;
        public long st_value;
        public long st_size;

        @Override
        public int getInfo() {
            return st_info;
        }

        @Override
        public int getNameIndex() {
            return st_name;
        }

        @Override
        public short getSectionHeaderIndex() {
            return st_shndx;
        }
    }

    public final ELFHeader header;
    public final ELFSectionHeaderTable.Entry entry;
    public final Entry[] entries;
    protected ELFStringTable strtab;

    /**
     * The constructor for the <code>ELFSymbolTable</code> class creates a new
     * symbol table with the specified ELF header from the specified ELF section
     * header table entry.
     * @param elfHeader the header of the ELF file
     * @param sectionEntry the entry in the section header table corresponding to this
     * symbol table
     */
    public ELFSymbolTable(ELFHeader elfHeader, ELFSectionHeaderTable.Entry sectionEntry) {
        this.header = elfHeader;
        this.entry = sectionEntry;
        entries = new Entry[(int) (sectionEntry.getSize() / (long) sectionEntry.getEntrySize())];
    }

    /**
     * The <code>read()</code> method reads this symbol table from the specified random
     * access file. The file is first advanced to the appropriate position with the
     * <code>seek()</code> method and then the entries are loaded.
     * @param f the random access file from which to read the symbol table
     * @throws IOException if there is a problem reading from the file
     */
    public void read(RandomAccessFile f) throws IOException {
        // seek to the beginning of the section
        f.seek(entry.getOffset());
        // create the elf data input stream
        final ELFDataInputStream is = new ELFDataInputStream(header, f);
        // read each of the entries
        for (int cntr = 0; cntr < entries.length; cntr++) {
            entries[cntr] = readEntry(f, is);
        }
    }

    private String getSymbolTableName(int cntr) {
        switch(cntr) {
            case 0:
                return "";
            case 1:
                return "maxvm_image";
            case 2:
                return "maxvm_image_start";
            case 3:
                return "maxvm_image_end";
            default:
                return "Error";
        }
    }

    public void setSymbolTableEntries(int index, long size) {
        for (int cntr = 0; cntr < entries.length; cntr++) {
            entries[cntr] = setEntries(getSymbolTableName(cntr), index, size);
        }
    }

    /*
     * This Function sets the entries for the symbol table.
     */
    public Entry64 setEntries(String sectionName, int index, long size) {
        Entry64 e = new Entry64();
        e.st_other = 0;
        e.st_size = 0;
        e.st_value = 0;
        e.st_name = 0;
        if (sectionName.equalsIgnoreCase("")) {

            e.st_shndx = 0;
            e.st_info = 0;
            return e;
        } else if (sectionName.equalsIgnoreCase("maxvm_image")) {
            // The below value of 3 will equate to STB_LOCAL for the bind value.
            // and section for the type section as per the bind and type value extraction
            // from the value field.
            e.st_info = 3;

        } else {
            // This is for the globals in the maxvm image that is stored in the maxvm_image section.
            e.st_name = strtab.getIndex(sectionName);
            // the value of 16 will equate to STB_GLOBAL for the bind value.
            e.st_info = 16;
            if (sectionName.equalsIgnoreCase("maxvm_image_end")) {
                // This entry will contain the size of the entire file.
                e.st_value = size;
            }
        }

        e.st_shndx = (short) index;
        return e;
    }

    private ELFSymbolTable.Entry readEntry(RandomAccessFile f, ELFDataInputStream is) throws IOException {
        if (header.is32Bit()) {
            return readEntry32(f, is);
        } else if (header.is64Bit()) {
            return readEntry64(f, is);
        }
        throw ProgramError.unexpected("unknown bit size");
    }

    private Entry32 readEntry32(RandomAccessFile f, ELFDataInputStream is) throws IOException {
        final Entry32 e = new Entry32();
        e.st_name = is.read_Elf32_Word();
        e.st_value = is.read_Elf32_Addr();
        e.st_size = is.read_Elf32_Word();
        e.st_info = is.read_Elf32_uchar();
        e.st_other = is.read_Elf32_uchar();
        e.st_shndx = is.read_Elf32_Half();
        for (int pad = ELF32_STENT_SIZE; pad < entry.getEntrySize(); pad++) {
            f.read();
        }
        return e;
    }

    private Entry64 readEntry64(RandomAccessFile f, ELFDataInputStream is) throws IOException {
        final Entry64 e = new Entry64();
        // note the order of fields is different in the 64 bit version.
        e.st_name  = is.read_Elf64_Word();
        e.st_info  = is.read_Elf64_uchar();
        e.st_other = is.read_Elf64_uchar();
        e.st_shndx = is.read_Elf64_Half();
        e.st_value = is.read_Elf64_Addr();
        e.st_size  = is.read_Elf64_XWord();
        for (int pad = ELF64_STENT_SIZE; pad < entry.getEntrySize(); pad++) {
            f.read();
        }
        return e;
    }

    public void setStringTable(ELFStringTable str) {
        strtab = str;
    }

    public ELFStringTable getStringTable() {
        return strtab;
    }

    public void write64ToFile(ELFDataOutputStream os, RandomAccessFile fis) throws IOException {
        for (int cntr = 0; cntr < entries.length; cntr++) {
            final Entry e = entries[cntr];
            final Entry64 e64 = (Entry64) e;
            os.write_Elf64_Word(e64.getNameIndex());   // 4 bytes;
            byte info = (byte) e64.getInfo();
            os.write_1(info);              // Only one byte
            byte other = (byte) e64.st_other;
            os.write_1(other);   // Only one byte
            os.write_Elf64_Half(e64.st_shndx);              // 2 bytes;
            os.write_Elf64_Addr(e64.st_value);            // 8 bytes;
            os.write_Elf64_XWord(e64.st_size);              // 8 bytes;
        }
    }
}
