/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * 
 * Parts of the product may be derived from Berkeley BSD systems,
 * licensed from the University of California. UNIX is a registered
 * trademark in the U.S.  and in other countries, exclusively licensed
 * through X/Open Company, Ltd.
 * 
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 * 
 * This product is covered and controlled by U.S. Export Control laws and
 * may be subject to the export or import laws in other
 * countries. Nuclear, missile, chemical biological weapons or nuclear
 * maritime end uses or end users, whether direct or indirect, are
 * strictly prohibited. Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion lists,
 * including, but not limited to, the denied persons and specially
 * designated nationals lists is strictly prohibited.
 * 
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
 * The <code>ELFProgramHeaderTable</code> class represents a program header table
 * contained in an ELF file. This table contains information about each section
 * in the program. This class represents a cleaned up view of the table. Since
 * the size and number of entries in this table are determined from the information
 * contained in the ELF header, this class requires an instance of the
 * <code>ELFHeader</code> class to be passed to the constructor.
 *
 * </p>
 * The ELF format states that a program header table is required for executables; this
 * table contains information for the operating system (or bootloader or programmer
 * in the case of embedded systems) to create a process image from the binary. This table
 * is optional for relocatable object files.
 *
 * @author Ben L. Titzer
 */
public class ELFProgramHeaderTable {

    public static final int PT_NULL = 0;
    public static final int PT_LOAD = 1;
    public static final int PT_DYNAMIC = 2;
    public static final int PT_INTERP = 3;
    public static final int PT_NOTE = 4;
    public static final int PT_SHLIB = 5;
    public static final int PT_PHDR = 6;
    public static final int PT_LOPROC = 0x70000000;
    public static final int PT_HIPROC = 0x7fffffff;

    public static final int PF_EXEC = 0x1;
    public static final int PF_WRITE = 0x2;
    public static final int PF_READ = 0x4;
    public static final int ELF32_PHTENT_SIZE = 32;
    public static final int ELF64_PHTENT_SIZE = 56;

    public abstract class Entry {
        public int p_type;
        public int p_flags;

        public int getFlags() {
            return p_flags;
        }

        public int getType() {
            return p_type;
        }

        public boolean isLoadable() {
            return getType() == PT_LOAD;
        }

        public boolean isExecutable() {
            return (getFlags() & PF_EXEC) != 0;
        }

        public String getFlagString() {
            final StringBuffer buffer = new StringBuffer();
            final int flags = getFlags();
            if ((flags & PF_EXEC) != 0) {
                buffer.append("EXEC ");
            }
            if ((flags & PF_WRITE) != 0) {
                buffer.append("WRITE ");
            }
            if ((flags & PF_READ) != 0) {
                buffer.append("READ ");
            }
            return buffer.toString();
        }

        public boolean is32Bit() {
            return this instanceof Entry32;
        }

        public boolean is64Bit() {
            return this instanceof Entry64;
        }
    }

    public class Entry32 extends Entry {
    	// Checkstyle: stop field name check
        public int p_offset;
        public int p_vaddr;
        public int p_paddr;
        public int p_filesz;
        public int p_memsz;
        public int p_align;
    	// Checkstyle: start field name check
    }

    public class Entry64 extends Entry {
        public long p_offset;
        public long p_vaddr;
        public long p_paddr;
        public long p_filesz;
        public long p_memsz;
        public long p_align;
    }

    public final ELFHeader header;
    public final Entry[] entries;

    /**
     * The constructor for the <code>ELFProgramHeaderTable</code> class creates a new instance
     * for the file containing the specified ELF header. The <code>ELFHeader</code> instance
     * contains information about the ELF file including the machine endianness that is
     * important for the program header table.
     * @param elfHeader the initialized ELF header from the file specified.
     */
    public ELFProgramHeaderTable(ELFHeader elfHeader) {
        this.header = elfHeader;
        entries = new Entry[elfHeader.e_phnum];
    }

    /**
     * The <code>read()</code> method reqds the program header table from the specified
     * input stream. This method assumes that the input stream has been positioned at
     * the beginning of the program header table.
     * @param fis the input stream from which to read the program header table
     * @throws IOException if there is a problem reading the header table from the input
     */
    public void read(RandomAccessFile fis) throws IOException {
        if (entries.length == 0) {
            return;
        }
        // seek to the beginning of the table
        fis.seek(header.e_phoff);
        final ELFDataInputStream is = new ELFDataInputStream(header, fis);
        // read each entry
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
        throw ProgramError.unexpected("unknown bit size for ELF header");
    }

    public Entry32 readEntry32(RandomAccessFile fis, ELFDataInputStream is) throws IOException {
        final Entry32 e = new Entry32();
        e.p_type   = is.read_Elf32_Word();
        e.p_offset = is.read_Elf32_Off();
        e.p_vaddr  = is.read_Elf32_Addr();
        e.p_paddr  = is.read_Elf32_Addr();
        e.p_filesz = is.read_Elf32_Word();
        e.p_memsz  = is.read_Elf32_Word();
        e.p_flags  = is.read_Elf32_Word();
        e.p_align  = is.read_Elf32_Word();
        readPadding(fis, ELF32_PHTENT_SIZE, header.e_phentsize);
        return e;
    }

    private void readPadding(RandomAccessFile fis, int read, short goal) throws IOException {
        for (int pad = read; pad < goal; pad++) {
            fis.read();
        }
    }

    public Entry64 readEntry64(RandomAccessFile fis, ELFDataInputStream is) throws IOException {
        final Entry64 e = new Entry64();
        // note the order of these fields is different between 32 and 64 bit versions.
        e.p_type   = is.read_Elf64_Word();
        e.p_flags  = is.read_Elf64_Word();
        e.p_offset = is.read_Elf64_Off();
        e.p_vaddr  = is.read_Elf64_Addr();
        e.p_paddr  = is.read_Elf64_Addr();
        e.p_filesz = is.read_Elf64_XWord();
        e.p_memsz  = is.read_Elf64_XWord();
        e.p_align  = is.read_Elf64_XWord();
        // read the rest of the entry (padding)
        readPadding(fis, ELF64_PHTENT_SIZE, header.e_phentsize);
        return e;
    }

    public Entry getEntry(int ind) {
        return entries[ind];
    }

    public static String getType(Entry e) {
        switch (e.getType()) {
            case PT_NULL: 
                return "null";
            case PT_LOAD: 
                return "load";
            case PT_DYNAMIC: 
                return "dynamic";
            case PT_INTERP: 
                return "interp";
            case PT_NOTE: 
                return "note";
            case PT_SHLIB: 
                return "shlib";
            case PT_PHDR: 
                return "phdr";
            default: 
                return Integer.toString(e.getType(), 16);
        }
    }

}
