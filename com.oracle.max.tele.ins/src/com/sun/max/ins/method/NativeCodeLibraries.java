/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import java.io.*;
import java.util.*;

import com.oracle.max.elf.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxPlatform.OS;
import com.sun.max.tele.debug.darwin.*;
import com.sun.max.tele.debug.darwin.DarwinMachO.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Provides access to native functions defined in shared libraries loaded into the VM.
 * Sadly, dlfcn.h does not provide any iterative capability for symbols so we have to read the
 * ELF file and match things up.
 */
public class NativeCodeLibraries {

    private static class TeleLibInfo {
        private Reference reference;
        private TeleVM vm;

        protected TeleLibInfo(TeleVM vm, Reference libInfoReference) {
            this.vm = vm;
            this.reference = libInfoReference;
        }

        private LibInfo libInfo;

        LibInfo libInfo() {
            if (libInfo == null) {
                final Word handle = vm.teleFields().DynamicLinker$LibInfo_handle.readWord(reference);
                final Pointer sentinelAsCString = vm.teleFields().DynamicLinker$LibInfo_sentinelAsCString.readWord(reference).asPointer();
                String sentinel = sentinelAsCString.isZero() ? null : stringFromCString(vm, sentinelAsCString);
                final Address sentinelAddress = vm.teleFields().DynamicLinker$LibInfo_sentinelAddress.readWord(reference).asAddress();
                String libPath;
                final Pointer pathAsCString = vm.teleFields().DynamicLinker$LibInfo_pathAsCString.readWord(reference).asPointer();
                if (pathAsCString.isNotZero()) {
                    libPath = stringFromCString(vm, pathAsCString);
                } else {
                    // this is mainHandle
                    OS os = vm.platform().getOS();
                    libPath = new File(vm.bootImageFile().getParent(), os.libjvmName() + "." + os.libSuffix()).getAbsolutePath();
                }
                libInfo = new LibInfo(libPath, handle, sentinel, sentinelAddress);
            }
            return libInfo;
        }

        private static String stringFromCString(TeleVM vm, Pointer cString) {
            byte[] bytes = new byte[1024];
            int index = 0;
            while (true) {
                byte b = vm.readValue(Kind.BYTE, cString, index).asByte();
                if (b == 0) {
                    break;
                }
                bytes[index++] = b;
            }
            return new String(bytes, 0, index);

        }
    }

    public static class LibInfo {
        String path;
        Word handle;
        String sentinel;
        Address sentinelAddress;
        SymbolInfo[] symbols;

        LibInfo(String path, Word handle, String sentinel, Address sentinelAddress) {
            this.path = path;
            this.handle = handle;
            this.sentinel = sentinel;
            this.sentinelAddress = sentinelAddress;
        }

        @Override
        public String toString() {
            String result = sentinel == null ? "(?) " : "";
            return result += path;
        }

        public String shortName() {
            String name = new File(path).getName();
            int index = name.lastIndexOf('.');
            if (index > 0) {
                name = name.substring(0, index);
            }
            return name;
        }
    }

    public static class SymbolInfo implements Comparable<SymbolInfo>{
        public final String name;
        public Address base;
        public int length;
        public final LibInfo lib;

        SymbolInfo(String name, LibInfo lib, long base) {
            this.name = name;
            this.lib = lib;
            this.base = Address.fromLong(base);
        }

        @Override
        public String toString() {
            return name;
        }

        public String qualName() {
            return lib.shortName() + "." + name;
        }

        public int compareTo(SymbolInfo other) {
            if (base.lessThan(other.base)) {
                return -1;
            } else if (base.greaterThan(other.base)) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private final static ArrayList<LibInfo> libs = new ArrayList<LibInfo>();

    public static ArrayList<LibInfo> getLibs(TeleVM vm) {
        try {
            updateLibInfo(vm);
        } catch (Exception ex) {
            System.out.println(ex);
            debug(ex);
        }
        return libs;
    }

    private static void debug(Exception ex) {  }

    /**
     * Add any new libraries since last refresh. It is possible that a sentinel value has appeared
     * since the last refresh, so we check that.
     * @param vm
     */
    private static void updateLibInfo(TeleVM vm) throws Exception {
        int length = vm.teleFields().DynamicLinker_libInfoIndex.readInt(vm);
        Reference libInfoArrayReference = vm.teleFields().DynamicLinker_libInfoArray.readReference(vm);
        for (int index = 0; index < length; index++) {
            if (index >= libs.size() || libs.get(index).sentinel == null) {
                Reference libInfoReference = vm.getElementValue(Kind.REFERENCE, libInfoArrayReference, index).asReference();
                TeleLibInfo teleLibInfo = new TeleLibInfo(vm, libInfoReference);
                LibInfo libInfo = teleLibInfo.libInfo();
                processLibrary(teleLibInfo);
                if (index >= libs.size()) {
                    libs.add(libInfo);
                }
            }
        }

    }

    public static SymbolInfo find(Address address) {
        Trace.line(1, "NativeCodeLibraries.find: " + address.to0xHexString());
        for (LibInfo libInfo : libs) {
            for (SymbolInfo info : libInfo.symbols) {
                if (address.greaterEqual(info.base) && address.lessThan(info.base.plus(info.length))) {
                    Trace.line(1, "NativeCodeLibraries.find: found " + info.name + " base=" + info.base.to0xHexString());
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Read the symbols from the library using ELF.
     * If we know the sentinel symbol's address, then fixup the symbol addresses
     * to their real values in the VM.
     * @param libInfo
     * @throws Exception
     */
    private static void processLibrary(TeleLibInfo teleLibInfo) throws Exception {
        LibInfo libInfo = teleLibInfo.libInfo;
        Trace.line(1, "NativeCodeLibraries.processLibrary: " + libInfo.path + ", sentinel: " + libInfo.sentinel);
        ArrayList<SymbolInfo> symbolList = new ArrayList<SymbolInfo>();
        long sentinelOffset = 0;
        switch (teleLibInfo.vm.platform().getOS()) {
            case DARWIN:
                sentinelOffset = readMachOSymbols(libInfo, symbolList);
                break;
            case LINUX:
            case SOLARIS:
                sentinelOffset = readElfSymbols(libInfo, symbolList);
                break;
            case WINDOWS:
            case MAXVE:
        }
        Address libBase = sentinelOffset == 0 ? Address.zero() : libInfo.sentinelAddress.minus(sentinelOffset);
        SymbolInfo[] symbolArray = new SymbolInfo[symbolList.size()];
        symbolList.toArray(symbolArray);
        Arrays.sort(symbolArray);
        for (int i = 0; i < symbolArray.length; i++) {
            symbolArray[i].base = symbolArray[i].base.plus(libBase);
            if (i > 0) {
                symbolArray[i - 1].length = symbolArray[i].base.minus(symbolArray[i - 1].base).toInt();
            }
        }
        symbolArray[symbolArray.length - 1].length = 100; // TODO do better
        libInfo.symbols = symbolArray;
    }

    private static long readElfSymbols(LibInfo libInfo, ArrayList<SymbolInfo> symbolList) throws Exception {
        long sentinelOffset = 0;
        ELFSymbolLookup elfSym = new ELFSymbolLookup(new File(libInfo.path));
        for (Map.Entry<String, List<ELFSymbolTable.Entry>> mapEntry : elfSym.symbolMap.entrySet()) {
            List<ELFSymbolTable.Entry> entryList = mapEntry.getValue();
            for (ELFSymbolTable.Entry entry : entryList) {
                ELFSymbolTable.Entry64 entry64 = (ELFSymbolTable.Entry64) entry;
                if (entry64.isFunction() && entry64.st_value != 0) {
                    symbolList.add(new SymbolInfo(entry64.getName(), libInfo, entry64.st_value));
                    if (libInfo.sentinel != null && entry64.getName().equals(libInfo.sentinel)) {
                        sentinelOffset = entry64.st_value;
                    }
                }
            }
        }
        return sentinelOffset;
    }

    private static long readMachOSymbols(LibInfo libInfo, ArrayList<SymbolInfo> symbolList) throws Exception {
        long sentinelOffset = 0;
        DarwinMachO machO = new DarwinMachO(libInfo.path);
        LoadCommand[] loadCommands = machO.getLoadCommands();
        for (int i = 0; i < loadCommands.length; i++) {
            if (loadCommands[i].cmd == LoadCommand.LC_SYMTAB) {
                SymTabLoadCommand slc = (SymTabLoadCommand) loadCommands[i];
                NList64[] symbolTable = slc.getSymbolTable();
                for (NList64 nlist64 : symbolTable) {
                    if ((nlist64.type & NList64.STAB) != 0) {
                        continue;
                    } else if ((nlist64.type & NList64.TYPE) == NList64.SECT) {
                        Section64 s64 = DarwinMachO.getSection(loadCommands, nlist64.sect);
                        if (s64.isText()) {
                            String name = slc.getSymbolName(nlist64);
                            symbolList.add(new SymbolInfo(name, libInfo, nlist64.value));
                            if (libInfo.sentinel != null && name.equals(libInfo.sentinel)) {
                                sentinelOffset = nlist64.value;
                            }
                        }
                    }
                }
            }
        }
        return sentinelOffset;
    }

}
