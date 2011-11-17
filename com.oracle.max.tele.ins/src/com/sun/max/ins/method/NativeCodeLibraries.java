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

        LibInfo getLibInfo(LibInfo oldLibInfo) {
            LibInfo result;
            if (oldLibInfo == null) {
                final Word handle = vm.fields().DynamicLinker$LibInfo_handle.readWord(reference);
                String libPath;
                final Pointer pathAsCString = vm.fields().DynamicLinker$LibInfo_pathAsCString.readWord(reference).asPointer();
                OS os = vm.platform().getOS();
                if (pathAsCString.isNotZero()) {
                    libPath = stringFromCString(vm, pathAsCString);
                } else {
                    // this is mainHandle
                    libPath = new File(vm.bootImageFile().getParent(), os.libjvmName() + "." + os.libSuffix()).getAbsolutePath();
                }
                result = LibInfo.create(os, libPath, handle);
            } else {
                result = oldLibInfo;
            }
            // These values may change between calls ti this method, although once set they are constant.
            final Pointer sentinelAsCString = vm.fields().DynamicLinker$LibInfo_sentinelAsCString.readWord(reference).asPointer();
            String sentinel = sentinelAsCString.isZero() ? null : stringFromCString(vm, sentinelAsCString);
            final Address sentinelAddress = vm.fields().DynamicLinker$LibInfo_sentinelAddress.readWord(reference).asAddress();
            result.sentinel = sentinel;
            result.sentinelAddress = sentinelAddress;
            return result;
        }

        private static String stringFromCString(TeleVM vm, Pointer cString) {
            byte[] bytes = new byte[1024];
            int index = 0;
            while (true) {
                byte b = vm.memory().readByte(cString, index);
                if (b == 0) {
                    break;
                }
                bytes[index++] = b;
            }
            return new String(bytes, 0, index);

        }
    }

    /**i
     * Information on a native code library that has been opened wth {@code dlopen}.
     * Until a symbol is actually looked up in the target VM, only the {@code path} and
     * {@code handle} fields are guaranteed to be set.
     */
    public static abstract class LibInfo {
        String path;
        Word handle;
        Address baseAddress;
        long size;
        String sentinel;
        Address sentinelAddress;
        SymbolInfo[] symbols;

        static LibInfo create(OS os, String path, Word handle) {
            try {
                String className = LibInfo.class.getPackage().getName() + "." + "NativeCodeLibraries$" + os.toString() + "LibInfo";
                LibInfo result = (LibInfo) Class.forName(className).newInstance();
                result.path = path;
                result.handle = handle;
                result.baseAddress = Address.zero();
                return result;
            } catch (Exception ex) {
                ProgramError.unexpected("cannot create OS-specific subclass of NativeCodeLibraries.LibInfo");
            }
            return null;
        }

        @Override
        public String toString() {
            String result = sentinel == null ? "(?) " : "";
            return result += path;
        }

        /**
         * Returns the library name without path prefix or extension (should still be unique).
         * @return
         */
        public String shortName() {
            String name = new File(path).getName();
            int index = name.lastIndexOf('.');
            if (index > 0) {
                name = name.substring(0, index);
            }
            return name;
        }

        /**
         * Process the library symbol table.
         * if {@code symbolList != null} populate with symbols else just check for sentinel.
         * @param symbolList
         * @return the offset of the sentinel symbol or zero if not found.
         * @throws Exception
         */
        protected abstract long readSymbols(ArrayList<SymbolInfo> symbolList) throws Exception;
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
            ProgramError.unexpected(ex);
        }
        return libs;
    }

    /**
     * Add any new libraries since last refresh. It is possible that a sentinel value has appeared
     * since the last refresh, so we check that.
     * @param vm
     */
    private static void updateLibInfo(TeleVM vm) throws Exception {
        int length = vm.fields().DynamicLinker_libInfoIndex.readInt(vm);
        Reference libInfoArrayReference = vm.fields().DynamicLinker_libInfoArray.readReference(vm);
        for (int index = 0; index < length; index++) {
            boolean newLib = index >= libs.size();
            if (newLib || libs.get(index).sentinel == null) {
                Reference libInfoReference = vm.memory().readArrayElementValue(Kind.REFERENCE, libInfoArrayReference, index).asReference();
                TeleLibInfo teleLibInfo = new TeleLibInfo(vm, libInfoReference);
                LibInfo libInfo = processLibrary(teleLibInfo, newLib ? null : libs.get(index));
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
     * Read the symbols from the library using OS-specific object format.
     * If we know the sentinel symbol's address, then fixup the symbol addresses
     * to their real values in the VM.
     * @param teleLibInfo
     * @param oldLibInfo null if new library else existing LibInfo
     * @return associated LibInfo
     * @throws Exception
     */
    private static LibInfo processLibrary(TeleLibInfo teleLibInfo, LibInfo oldLibInfo) throws Exception {
        LibInfo libInfo = teleLibInfo.getLibInfo(oldLibInfo);
        Trace.line(1, "NativeCodeLibraries.processLibrary: " + libInfo.path + ", sentinel: " + libInfo.sentinel);
        ArrayList<SymbolInfo> symbolList = oldLibInfo == null ? new ArrayList<SymbolInfo>() : null;
        long sentinelOffset = libInfo.readSymbols(symbolList);

        Address libBase = sentinelOffset == 0 ? Address.zero() : libInfo.sentinelAddress.minus(sentinelOffset);
        SymbolInfo[] symbolArray;
        if (oldLibInfo == null) {
            symbolArray = new SymbolInfo[symbolList.size()];
            symbolList.toArray(symbolArray);
            libInfo.symbols = symbolArray;
            Arrays.sort(symbolArray);
        } else {
            symbolArray = oldLibInfo.symbols;
        }
        // If we found the sentinel address, set the absolute address of the symbols and an estimate of the function length
        if (libBase.isNotZero()) {
            libInfo.baseAddress = libInfo.baseAddress.plus(libBase);
            for (int i = 0; i < symbolArray.length; i++) {
                symbolArray[i].base = symbolArray[i].base.plus(libBase);
                if (i > 0) {
                    symbolArray[i - 1].length = symbolArray[i].base.minus(symbolArray[i - 1].base).toInt();
                }
            }
            symbolArray[symbolArray.length - 1].length = libInfo.baseAddress.plus(libInfo.size).minus(symbolArray[symbolArray.length - 2].base).toInt();
        }
        return libInfo;
    }

    static class DarwinLibInfo extends LibInfo {
        private static final String IGNORE = "_mh_dylib_header";
        @Override
        protected long readSymbols(ArrayList<SymbolInfo> symbolList) throws Exception {
            long sentinelOffset = 0;
            DarwinMachO machO = new DarwinMachO(path);
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
                            // There are many TEXT sections but we are only interested in the __text one.
                            if (s64.sectname.equals("__text")) {
                                String name = slc.getSymbolName(nlist64);
                                if (!name.equals(IGNORE)) {
                                    if (symbolList != null) {
                                        symbolList.add(new SymbolInfo(name, this, nlist64.value));
                                    }
                                    if (sentinelOffset == 0 && sentinel != null && name.equals(sentinel)) {
                                        sentinelOffset = nlist64.value;
                                        // set the base/length from the Section
                                        baseAddress = Address.fromLong(s64.addr);
                                        size = s64.size;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return sentinelOffset;
        }
    }

    static class UnixLibInfo extends LibInfo {

        @Override
        protected long readSymbols(ArrayList<SymbolInfo> symbolList) throws Exception {
            long sentinelOffset = 0;
            ELFSymbolLookup elfSym = new ELFSymbolLookup(new File(path));
            for (Map.Entry<String, List<ELFSymbolTable.Entry>> mapEntry : elfSym.symbolMap.entrySet()) {
                List<ELFSymbolTable.Entry> entryList = mapEntry.getValue();
                for (ELFSymbolTable.Entry entry : entryList) {
                    ELFSymbolTable.Entry64 entry64 = (ELFSymbolTable.Entry64) entry;
                    if (entry64.isFunction() && entry64.st_value != 0) {
                        if (symbolList != null) {
                            symbolList.add(new SymbolInfo(entry64.getName(), this, entry64.st_value));
                        }
                        if (sentinel != null && entry64.getName().equals(sentinel)) {
                            sentinelOffset = entry64.st_value;
                        }
                    }
                }
            }
            return sentinelOffset;
        }
    }

    static class LinuxLibInfo extends UnixLibInfo {

    }

    static class SolarisLibInfo extends UnixLibInfo {

    }
}
