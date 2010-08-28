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
package com.sun.max.elf;

import java.io.*;
import java.util.*;

import com.sun.max.elf.ELFHeader.*;
import com.sun.max.elf.ELFSymbolTable.*;

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
