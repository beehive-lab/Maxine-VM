/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.elf;

import static com.oracle.max.elf.StringUtil.*;

import java.io.RandomAccessFile;

/**
 * The {@code ELFDumper} is a class that can load and display information
 * about ELF files.
 *
 * @author Ben L. Titzer
 */
public final class ELFDumper {

    private ELFDumper() {

    }

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("usage: elf-dumper <filename>");
            return;
        }

        final String fname = args[0];

        final RandomAccessFile fis = new RandomAccessFile(fname, "r");

        try {
            // read the ELF header
            final ELFHeader header = ELFLoader.readELFHeader(fis);
            printHeader(header);

            // read the program header table (if it exists)
            final ELFProgramHeaderTable pht = ELFLoader.readPHT(fis, header);
            printPHT(pht);

            // read the section header table
            final ELFSectionHeaderTable sht = ELFLoader.readSHT(fis, header);
            printSHT(sht);

            // read the symbol tables
            for (ELFSymbolTable stab : ELFLoader.readSymbolTables(fis, header, sht)) {
                printSymbolTable(stab, sht);
            }

        } catch (ELFHeader.FormatError e) {
            throw new Error("Invalid ELF file", e);
        }
    }

    public static void printHeader(ELFHeader header) {
        nextln();
        printSeparator();
        println("Ver Machine     Arch     Size  Endian");
        printThinSeparator();
        print(rightJustify(header.e_version, 3));
        print(rightJustify(header.e_machine, 8));
        print(rightJustify(header.getArchitecture(), 9));
        print(rightJustify(header.is64Bit() ? "64 bits" : "32 bits", 9));
        print(header.isLittleEndian() ? "  little" : "  big");
        nextln();
    }

    public static void printSHT(ELFSectionHeaderTable sht) {
        println("Section Header Table");
        printSeparator();
        print("Ent  Name                        Type   Address  Offset    Size  Flags");
        nextln();
        printThinSeparator();
        for (int cntr = 0; cntr < sht.entries.length; cntr++) {
            final ELFSectionHeaderTable.Entry e = sht.entries[cntr];
            print(rightJustify(cntr, 3));
            print("  " + leftJustify(e.getName(), 24));
            print(rightJustify(e.getType(), 11));
            if (e.is32Bit()) {
                final ELFSectionHeaderTable.Entry32 e32 = (ELFSectionHeaderTable.Entry32) e;
                print("  " + toHex(e32.sh_addr));
                print(rightJustify(e32.sh_offset, 8));
                print(rightJustify(e32.sh_size, 8));
            } else {
                final ELFSectionHeaderTable.Entry64 e64 = (ELFSectionHeaderTable.Entry64) e;
                print("  " + toHex(e64.sh_addr));
                print(rightJustify(e64.sh_offset, 8));
                print(rightJustify(e64.sh_size, 8));
            }
            print("  " + e.getFlagString());
            nextln();
        }
        nextln();
    }

    public static String getName(ELFStringTable st, int ind) {
        if (st == null) {
            return "";
        }
        return st.getString(ind);
    }

    public static void printPHT(ELFProgramHeaderTable pht) {
        println("Program Header Table");
        printSeparator();
        print("Ent     Type  Virtual   Physical  Offset  Filesize  Memsize  Flags");
        nextln();
        printThinSeparator();
        for (int cntr = 0; cntr < pht.entries.length; cntr++) {
            final ELFProgramHeaderTable.Entry e = pht.entries[cntr];
            print(rightJustify(cntr, 3));
            print(rightJustify(ELFProgramHeaderTable.getType(e), 9));
            if (e.is32Bit()) {
                final ELFProgramHeaderTable.Entry32 e32 = (ELFProgramHeaderTable.Entry32) e;
                print("  " + toHex(e32.p_vaddr));
                print("  " + toHex(e32.p_paddr));
                print(rightJustify(e32.p_offset, 8));
                print(rightJustify(e32.p_filesz, 10));
                print(rightJustify(e32.p_memsz, 9));
            } else {
                final ELFProgramHeaderTable.Entry64 e64 = (ELFProgramHeaderTable.Entry64) e;
                print("  " + toHex(e64.p_vaddr));
                print("  " + toHex(e64.p_paddr));
                print(rightJustify(e64.p_offset, 8));
                print(rightJustify(e64.p_filesz, 10));
                print(rightJustify(e64.p_memsz, 9));
            }
            print("  " + e.getFlagString());
            nextln();
        }
    }

    public static void printSymbolTable(ELFSymbolTable stab, ELFSectionHeaderTable sht) {
        println("Symbol Table");
        printSeparator();
        print("Ent  Type     Section     Bind    Name                     Address      Size");
        nextln();
        printThinSeparator();
        final ELFStringTable str = stab.getStringTable();
        for (int cntr = 0; cntr < stab.entries.length; cntr++) {
            final ELFSymbolTable.Entry e = stab.entries[cntr];
            print(rightJustify(cntr, 3));
            print("  " + leftJustify(e.getType(), 7));
            print("  " + leftJustify(sht.getSectionName(e.getSectionHeaderIndex()), 14));
            print(leftJustify(e.getBinding(), 8));
            print(leftJustify(getName(str, e.getNameIndex()), 32));
            if (e.is32Bit()) {
                final ELFSymbolTable.Entry32 e32 = (ELFSymbolTable.Entry32) e;
                print("  " + toHex(e32.st_value));
                print("  " + rightJustify(e32.st_size, 8));
            } else {
                final ELFSymbolTable.Entry64 e64 = (ELFSymbolTable.Entry64) e;
                print("  " + toHex(e64.st_value));
                print("  " + rightJustify(e64.st_size, 12));
            }
            nextln();
        }
    }

    static void print(String str) {
        System.out.print(str);
    }
    static void print(char c) {
        System.out.print(c);
    }

    static void println(String str) {
        System.out.println(str);
    }

    static void nextln() {
        System.out.println("");
    }

    static void printSeparator() {
        System.out.println("==============================================================================");
    }

    static void printThinSeparator() {
        System.out.println("------------------------------------------------------------------------------");
    }

    static String toHex(int v) {
        return StringUtil.toHex(v, 8);
    }

    static String toHex(long v) {
        return StringUtil.toHex(v, 16);
    }
}
