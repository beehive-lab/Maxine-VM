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
package com.oracle.max.elf;

import java.io.*;
import java.util.*;

import com.oracle.max.elf.ELFHeader.*;
import com.oracle.max.elf.ELFSymbolTable.*;

/**
 * Builds a lookup table given the symbol table by arranging the symbols as name,entry pairs.
 * @author Puneeet Lakhina
 *
 */
public class ELFSymbolLookup {

    private Map<String, Entry> symbolMap = new HashMap<String, Entry>();

    public ELFSymbolLookup(File elfFile) throws IOException, FormatError {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(elfFile, "r");
            ELFHeader header = ELFLoader.readELFHeader(raf);
            buildSymbolMap(raf, header, ELFLoader.readSHT(raf, header));
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ELFSymbolLookup(List<ELFSymbolTable> symbolTables) {
        buildSymbolMap(symbolTables);
    }

    public ELFSymbolLookup(RandomAccessFile raf, ELFHeader header) throws IOException {
        buildSymbolMap(raf, header, ELFLoader.readSHT(raf, header));
    }

    private void buildSymbolMap(RandomAccessFile raf, ELFHeader header, ELFSectionHeaderTable sht) throws IOException {
        buildSymbolMap(ELFLoader.readSymbolTables(raf, header, sht));
    }

    private void buildSymbolMap(List<ELFSymbolTable> symbolTables) {
        if (symbolTables == null || symbolTables.isEmpty()) {
            return;
        }
        for (ELFSymbolTable symbolTable : symbolTables) {
            Entry[] symbolTableEntries = symbolTable.entries;
            for (int i = 0; i < symbolTableEntries.length; i++) {
                Entry symbolTableEntry = symbolTableEntries[i];
                if (!"".equals(symbolTableEntry.getName() != null ? symbolTableEntry.getName() : "")) {
                    symbolMap.put(symbolTableEntry.getName(), symbolTableEntry);
                }
            }
        }
    }

    /** Get the value associated with a symbol.
     *
     * @param name - The name of the symbol
     * @return The value associated with the symbol if the symbol exists. Null otherwise. The value returned is an
     *         Integer if the ELF file is 32 bit, its a Long if the file is 64 bit
     */
    public Number lookupSymbolValue(String name) {
        Entry symbolTableEntry = symbolMap.get(name);
        return symbolTableEntry != null ? symbolTableEntry.is32Bit() ? ((Entry32) symbolTableEntry).st_value : ((Entry64) symbolTableEntry).st_value : null;
    }

}
