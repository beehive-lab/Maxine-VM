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
 */

package com.sun.max.elf;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The <code>ELFHeader</code> class represents the header of an ELF file.
 * It can load the header from a file and check the identification, identify
 * the version, endianness, and detect which architecture the file
 * has been created for.
 *
 * @author Ben L. Titzer
 */
public class ELFHeader {

    protected static final int ELFCLASSNONE = 0;
    protected static final int ELFCLASS32   = 1;
    protected static final int ELFCLASS64   = 2;
    protected static final int ELFCLASSNUM  = 3;

    protected static final int EI_NIDENT = 16;

    // constants for information within e_ident section
    protected static final int EI_CLASS = 4;
    protected static final int EI_DATA = 5;
    protected static final int EI_VERSION = 6;
    protected static final int EI_PAD = 7;

    // constants for data format
    protected static final int ELFDATA2LSB = 1;
    protected static final int ELFDATA2MSB = 2;

    /*******************************************/
    // constants for the e_type field
    protected static final int ET_NONE = 0;
    protected static final int ET_REL   = 1;
    protected static final int ET_EXEC  = 2;
    protected static final int ET_DYN  = 3;
    protected static final int ET_CORE = 4;

    // constants for the version field.
    protected static final int EV_NONE = 0;
    protected static final int EV_CURRENT   = 1;

    // constants for the e_ident[] member
    protected static final int EI_ELFMAG0 = 0;
    protected static final int EI_ELFMAG1 = 1;
    protected static final int EI_ELFMAG2 = 2;
    protected static final int EI_ELFMAG3 = 3;
    protected static final int EI_OSABI = 7;
    protected static final int EI_ABIVERSION = 8;
    protected static final int EI_PADJG = 9;

    /*******************************************/


    // Checkstyle: stop field name check
    public final byte[] e_ident;
    public short e_type;
    public short e_machine;
    public int e_version;
    public long e_entry;
    public long e_phoff;
    public long e_shoff;
    public int e_flags;
    public short e_ehsize;
    public short e_phentsize;
    public short e_phnum;
    public short e_shentsize;
    public short e_shnum;
    public short e_shstrndx;

    boolean bigEndian;
    // Checkstyle: resume field name check

    public class FormatError extends Exception {
        static final long serialVersionUID = 89236748768763L;

    }

    /**
     * The default constructor for the <code>ELFHeader</code> class simply creates a new, unitialized
     * instance of this class that is ready to load.
     */
    public ELFHeader() {
        e_ident = new byte[EI_NIDENT];
    }

    /**
     * The <code>read()</code> method reads the header from the specified input stream.
     * It loads the identification section and checks that the header is present by testing against
     * the magic ELF values, and reads the rests of the data section, initializes the ELF section.
     * @param fs the input stream from which to read the ELF header
     * @throws IOException if there is a problem reading from the input stream
     */
    public void read(RandomAccessFile fs) throws IOException, FormatError {
        // read the indentification string
        if (fs.length() < EI_NIDENT) {
            throw new FormatError();
        }
        int index = 0;
        //String abc = fs.readLine();
        //System.out.println(abc);
        while (index < EI_NIDENT) {
            index += fs.read(e_ident, index, EI_NIDENT - index);
        }

        checkIdent();
        final ELFDataInputStream is = new ELFDataInputStream(this, fs);
        if (is32Bit()) {
            // read a 32-bit header.
            readHeader32(is);
        } else if (is64Bit()) {
            // read a 64-bit header.
            readHeader64(is);
        }
    }

    private void readHeader32(ELFDataInputStream is) throws IOException {
        e_type      = is.read_Elf32_Half();
        e_machine   = is.read_Elf32_Half();
        e_version   = is.read_Elf32_Word();
        e_entry     = is.read_Elf32_Addr();
        e_phoff     = is.read_Elf32_Off();
        e_shoff     = is.read_Elf32_Off();
        e_flags     = is.read_Elf32_Word();
        e_ehsize    = is.read_Elf32_Half();
        e_phentsize = is.read_Elf32_Half();
        e_phnum     = is.read_Elf32_Half();
        e_shentsize = is.read_Elf32_Half();
        e_shnum     = is.read_Elf32_Half();
        e_shstrndx  = is.read_Elf32_Half();
    }

    private void readHeader64(ELFDataInputStream is) throws IOException {
        e_type      = is.read_Elf64_Half();
        e_machine   = is.read_Elf64_Half();
        e_version   = is.read_Elf64_Word();
        e_entry     = is.read_Elf64_Addr();
        e_phoff     = is.read_Elf64_Off();
        e_shoff     = is.read_Elf64_Off();
        e_flags     = is.read_Elf64_Word();
        e_ehsize    = is.read_Elf64_Half();
        e_phentsize = is.read_Elf64_Half();
        e_phnum     = is.read_Elf64_Half();
        e_shentsize = is.read_Elf64_Half();
        e_shnum     = is.read_Elf64_Half();
        e_shstrndx  = is.read_Elf64_Half();

    }

    private void checkIdent() throws FormatError {
        checkIndentByte(0, 0x7f);
        checkIndentByte(1, 'E');
        checkIndentByte(2, 'L');
        checkIndentByte(3, 'F');
        bigEndian = isBigEndian();
    }

    private void checkIndentByte(int ind, int val) throws FormatError {
        if (e_ident[ind] != val) {
            throw new FormatError();
        }
    }

    /**
     * The <code>getVersion()</code> method returns the version of this ELF file. The version
     * number is stored in the identification section of the ELF file at the beginning.
     * @return the version of this ELF file as an integer
     */
    public int getVersion() {
        return e_ident[EI_VERSION];
    }

    /**
     * The <code>getArchitecture()</code> method resolves the name of the architecture
     * from the <code>e_machine</code> file of the structure, using an internal map for
     * well-known architectures.
     * @return a String representation of the architecture name
     */
    public String getArchitecture() {
        return ELFIdentifier.getArchitecture(e_machine);
    }

    /**
     * The <code>isLittleEndian()</code> method checks whether this ELF file is encoded
     * in the little endian format (i.e. least signficant byte first). This information is
     * present in the identification section of the ELF file
     * @return true if this file has the little endian data format; false otherwise
     */
    public boolean isLittleEndian() {
        return e_ident[EI_DATA] == ELFDATA2LSB;
    }

    /**
     * The <code>isBigEndian()</code> method checks whether this ELF file is encoded
     * in big endian format (i.e. most signficant byte first). This information is present
     * in the identification section of the ELF file.
     * @return true if this file has the big endian data format; false otherwise
     */
    public boolean isBigEndian() {
        return e_ident[EI_DATA] == ELFDATA2MSB;
    }

    /**
     * The <code>is32Bit()</code> method checks whether this ELF file is encoded
     * as 32 or 64 bits. This information is contained in the header in the EI_CLASS
     * byte.
     * @return true if this ELF file is 32 bit
     */
    public boolean is32Bit()  {
        return e_ident[EI_CLASS] == ELFCLASS32;
    }

    /**
     * The <code>is64Bit()</code> method checks whether this ELF file is encoded
     * as 32 or 64 bits. This information is contained in the header in the EI_CLASS
     * byte.
     * @return true if this ELF file is 64 bit
     */
    public boolean is64Bit()  {
        return e_ident[EI_CLASS] == ELFCLASS64;
    }
   // The following code is to write a 64 bit ELF header for creating a ELF file

    public void writeHeader64(long size) throws IOException {
        // To fill the e_ident[] data member.
        // The following are the magic numbers that tell that the file is a ELF file.
        e_ident[EI_ELFMAG0] = 0x7f;
        e_ident[EI_ELFMAG1] = 'E';
        e_ident[EI_ELFMAG2] = 'L';

        e_ident[EI_ELFMAG3] = 'F';
        e_ident[EI_CLASS] = ELFCLASS64;
        e_ident[EI_DATA] = ELFDATA2LSB;
        e_ident[EI_VERSION] = EV_CURRENT;

        // Shd find out what the following fields have to be populated as.
        e_ident[EI_OSABI] = 0;
        e_ident[EI_ABIVERSION] = 0;

        // The below field is for EI_PAD, and is not used.
        e_ident[9] = 0;

        e_type = ET_REL;
        e_machine = 62;   //EM_AMD64 or EM_X86_64
        e_version = EV_CURRENT;
        e_entry = 0;
        e_phoff = 0;        // No program table, hence the offset to the program header table is 0.
        e_shoff = 0; // section header offset, this will be filled later.
        e_flags = 0;    // No Processor specific flag.
        e_ehsize = 64; // The size of the header is 64 bytes for 64 bit architecture.
        e_phentsize = 0;  // The size of the file's program header table, here it is 0 since there is no program header table.


        e_phnum = 0;
        e_shentsize = 64;  // size of the entry64 members in ELFSecti0n HearderTable - These are the total bytes in each header.
        e_shnum = 5; // The total number of sections in the file. (intial null section, Maxvm_image, section string table, symbol table, string table )
        e_shstrndx = 2; // This is the index in the section header table of the section that contains the section string table, 0 if there is no section string table.


    }

    public void writeELFHeader64ToFile(ELFDataOutputStream os, RandomAccessFile fis) throws IOException {

        // 64 byte header is written.
        // Write the e_ident[16] byte array into the object file.
        fis.write(e_ident);
        os.write_Elf64_Half(e_type);
        os.write_Elf64_Half(e_machine);
        os.write_Elf64_Word(e_version);
        os.write_Elf64_Addr(e_entry);
        os.write_Elf64_Off(e_phoff);
        os.write_Elf64_Off(e_shoff);
        os.write_Elf64_Word(e_flags);
        os.write_Elf64_Half(e_ehsize);
        os.write_Elf64_Half(e_phentsize);
        os.write_Elf64_Half(e_phnum);
        os.write_Elf64_Half(e_shentsize);
        os.write_Elf64_Half(e_shnum);
        os.write_Elf64_Half(e_shstrndx);

    }

    public void setShOff(long offset) {
        this.e_shoff = offset;
    }
}
