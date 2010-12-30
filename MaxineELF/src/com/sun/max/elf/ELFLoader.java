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
 * Copyright (c) 2006, Regents of the University of California
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
 * Created Sep 30, 2006
 */
package com.sun.max.elf;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

/**
 * The <code>ELFLoader</code> class presents a facade to users of the ELF
 * classes that permits a simpler usage model. This class contains a number of
 * utility methods to load sections and symbol tables from ELF files.
 *
 * @author Ben L. Titzer
 */
public final class ELFLoader {

    private ELFLoader() {
    }

    /**
     * The <code>readELFHeader()</code> method loads an ELF header data structure
     * from the specified random access file. This method checks for consistency; i.e.
     * that the header contains the special ELF magic, and throws a format error if
     * the file does not match the ELF specification.
     * @param fis the random access file from which to read the header
     * @return a reference to a new <code>ELFHeader</code> instance representing the header
     * @throws IOException if an IO exception occurred
     * @throws ELFHeader.FormatError if the header does not comply with the ELF specification
     */
    public static ELFHeader readELFHeader(RandomAccessFile fis) throws IOException, ELFHeader.FormatError {
        final ELFHeader header = new ELFHeader();
        header.read(fis);
        return header;
    }

    /**
     * The <code>readPHT()</code> method reads the program header table from the specified file.
     * @param fis the file from which to read the program header table
     * @param header the ELFHeader instance already loaded from this file
     * @return a reference to a new object representing the program header table for this ELF file
     * @throws IOException if an IO exception occurs
     */
    public static ELFProgramHeaderTable readPHT(RandomAccessFile fis, ELFHeader header) throws IOException {
        final ELFProgramHeaderTable pht = new ELFProgramHeaderTable(header);
        pht.read(fis);
        return pht;
    }

    /**
     * The <code>readSHT()</code> method loads the section header table from the specified file.
     * @param fis the file from which to load the section header table
     * @param header the ELF header corresponding to this file
     * @return a reference to a new object that represents the section header table
     * @throws IOException if an IO exception occurs
     */
    public static ELFSectionHeaderTable readSHT(RandomAccessFile fis, ELFHeader header) throws IOException {
        final ELFSectionHeaderTable sht = new ELFSectionHeaderTable(header);
        sht.read(fis);

        // read the ELF string table that contains the section names
        if (header.e_shstrndx < sht.entries.length) {
            final ELFSectionHeaderTable.Entry e = sht.entries[header.e_shstrndx];
            final ELFStringTable srttab = new ELFStringTable(header, e);
            srttab.read(fis);
            sht.setStringTable(srttab);
        }
        return sht;
    }

    /**
     * The <code>readSymbolTables()</code> method reads a list of symbol tables from the ELF
     * file, if any exist.
     * @param fis the file from which to load the symbol tables
     * @param header the ELF header for this file
     * @param sht the section header table for this file, which is used to locate string tables
     * @return a list of symbol tables that are contained in this ELF file
     * @throws IOException if an IO exception occurs
     */
    public static List<ELFSymbolTable> readSymbolTables(RandomAccessFile fis, ELFHeader header, ELFSectionHeaderTable sht) throws IOException {
        final List<ELFSymbolTable> symbolTables = new LinkedList<ELFSymbolTable>();
        for (int cntr = 0; cntr < sht.entries.length; cntr++) {
            final ELFSectionHeaderTable.Entry e1 = sht.entries[cntr];
            if (e1.isSymbolTable()) {
                final ELFSymbolTable stab = new ELFSymbolTable(header, e1);
                stab.read(fis);
                symbolTables.add(stab);
                final ELFSectionHeaderTable.Entry strent = sht.entries[e1.getLink()];
                if (strent.isStringTable()) {
                    final ELFStringTable str = new ELFStringTable(header, strent);
                    str.read(fis);
                    stab.setStringTable(str);
                }
            }
        }
        return symbolTables;
    }
}
