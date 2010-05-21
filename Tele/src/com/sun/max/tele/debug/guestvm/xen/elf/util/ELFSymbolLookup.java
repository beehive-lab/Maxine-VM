/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems, licensed from the University of California. UNIX is a
 * registered trademark in the U.S. and in other countries, exclusively licensed through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered trademarks of Sun Microsystems, Inc. in the
 * U.S. and other countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and may be subject to the export or import laws in
 * other countries. Nuclear, missile, chemical biological weapons or nuclear maritime end uses or end users, whether
 * direct or indirect, are strictly prohibited. Export or reexport to countries subject to U.S. embargo or to entities
 * identified on U.S. export exclusion lists, including, but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 */
package com.sun.max.tele.debug.guestvm.xen.elf.util;

import java.io.*;
import java.util.*;

import com.sun.max.elf.*;
import com.sun.max.elf.ELFHeader.*;
import com.sun.max.elf.ELFSymbolTable.*;

/**
 * Builds a lookup table given the symbol table by arrangin the symbols as name,entry pairs.
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
