/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.tele.method;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.elf.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.MaxPlatform.OS;
import com.sun.max.tele.debug.darwin.*;
import com.sun.max.tele.debug.darwin.DarwinMachO.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


public abstract class TeleNativeLibrary extends AbstractVmHolder implements MaxNativeLibrary, MaxCodeHoldingRegion<MaxNativeLibrary> {

    private static final class NativeLibraryMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxNativeLibrary> {
        private final List<MaxEntityMemoryRegion< ? extends MaxEntity>> children = new ArrayList<MaxEntityMemoryRegion< ? extends MaxEntity>>();

        private MaxNativeLibrary library;

        private NativeLibraryMemoryRegion(MaxVM vm, MaxNativeLibrary library) {
            super(vm, "Native library " + library.path(), library.base(), library.length());
            this.library = library;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Native libraries are roots in the forest
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return children;
        }

        public void addChild(TeleNativeFunction teleNativeFunction) {
            children.add(teleNativeFunction.memoryRegion());
        }

        public MaxNativeLibrary owner() {
            return library;
        }

        public boolean isBootRegion() {
            return false;
        }

    }

    protected String path;
    protected Address base;
    protected long length;
    protected String sentinel;
    private Address sentinelAddress;
    private TeleNativeFunction[] functions;
    private NativeLibraryMemoryRegion nativeLibraryMemoryRegion;
    private final RemoteCodePointerManager codePointerManager;
    private boolean sortByName;

    public static TeleNativeLibrary create(MaxVM vm, OS os, String path) {
        try {
            String className = TeleNativeLibrary.class.getPackage().getName() + "." + "TeleNativeLibrary$" + os.toString();
            Class<?> klass = Class.forName(className);
            Constructor<?> cons = klass.getDeclaredConstructor(TeleVM.class, String.class, Address.class);
            TeleNativeLibrary result = (TeleNativeLibrary) cons.newInstance(vm, path, Address.zero());
            return result;
        } catch (Exception ex) {
            ProgramError.unexpected("cannot create OS-specific subclass of TeleNativeLibrary", ex);
        }
        return null;
    }

    private TeleNativeLibrary(TeleVM vm, String path, Address base) {
        super(vm);
        this.path = path;
        this.base = base;
        this.codePointerManager = new NativeRemoteCodePointerManager(vm, this);
    }

    public RemoteCodePointerManager codePointerManager() {
        return codePointerManager;
    }

    public void setSentinel(String sentinel, Address sentinelAddress) {
        this.sentinel = sentinel;
        this.sentinelAddress = sentinelAddress;
    }

    /**
     * If the sentinel symbol has been resolved, gather the functions.
     */
    public void gatherFunctions() throws Exception {
        if (sentinel == null) {
            return;
        }
        ArrayList<TeleNativeFunction> functionList = new ArrayList<TeleNativeFunction>();
        long sentinelOffset = readSymbols(functionList);
        assert sentinelOffset != 0;

        base = base.plus(sentinelAddress.minus(sentinelOffset));
        nativeLibraryMemoryRegion = new NativeLibraryMemoryRegion(vm(), this);
        functions = new TeleNativeFunction[functionList.size()];
        functionList.toArray(functions);
        Arrays.sort(functions);

        // relocate the native function address by base
        for (int i = 0; i < functions.length; i++) {
            functions[i].updateAddress();
            if (i > 0) {
                functions[i - 1].updateLength(functions[i].getCodeStart().minus(functions[i - 1].getCodeStart()).toInt());
            }
        }
        functions[functions.length - 1].updateLength(base.plus(length).minus(functions[functions.length - 2].getCodeStart()).toInt());

        for (int i = 0; i < functions.length; i++) {
            nativeLibraryMemoryRegion.addChild(functions[i]);
        }
        //ok, now sort by name
        sortByName = true;
        Arrays.sort(functions);
    }

    boolean sortByName() {
        return sortByName;
    }

    public String entityName() {
        String name = new File(path).getName();
        int index = name.lastIndexOf('.');
        if (index > 0) {
            name = name.substring(0, index);
        }
        return name;
    }

    @Override
    public String toString() {
        return path;
    }

    public String entityDescription() {
        return "Native library " + path();
    }

    public MaxEntityMemoryRegion<MaxNativeLibrary> memoryRegion() {
        return nativeLibraryMemoryRegion;
    }

    public boolean contains(Address address) {
        return nativeLibraryMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        return null;
    }

    public String path() {
        return path;
    }

    public MaxNativeFunction[] functions() {
        return functions;
    }

    public TeleNativeFunction findNativeFunction(Address address) {
        for (TeleNativeFunction teleNativeFunction : functions) {
            if (teleNativeFunction.contains(address)) {
                return teleNativeFunction;
            }
        }
        return null;
    }

    public Address base() {
        return base;
    }

    public int length() {
        return (int) length;
    }

    /**
     * Process the library symbol table.
     * if {@code functionList != null} populate with symbols else just check for sentinel.
     * @param functionList
     * @return the offset of the sentinel symbol or zero if not found.
     * @throws Exception
     */
    protected abstract long readSymbols(ArrayList<TeleNativeFunction> functionList) throws Exception;

    static class Darwin extends TeleNativeLibrary {
        private static final String IGNORE = "_mh_dylib_header";

        Darwin(TeleVM vm, String path, Address base) {
            super(vm, path, base);
        }

        @Override
        protected long readSymbols(ArrayList<TeleNativeFunction> functionList) throws Exception {
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
                                    // value is the offset from the start of the __TEXT segment, not the __text section
                                    functionList.add(new TeleNativeFunction(vm(), name, Address.fromLong(nlist64.value), this));
                                    if (sentinelOffset == 0 && sentinel != null && name.equals(sentinel)) {
                                        sentinelOffset = nlist64.value;
                                        // set the base/length from the Section
                                        // N.B. base is also the offset from the start of the __TEXT segment
                                        base = Address.fromLong(s64.addr);
                                        length = s64.size;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // relocate the offsets to be from the start of the __text section
            for (TeleNativeFunction f : functionList) {
                f.base = f.base.minus(base);
            }
            return sentinelOffset;
        }
    }

    static class Unix extends TeleNativeLibrary {

        Unix(TeleVM vm, String path, Address base) {
            super(vm, path, base);
        }

        @Override
        protected long readSymbols(ArrayList<TeleNativeFunction> functionList) throws Exception {
            long sentinelOffset = 0;
            RandomAccessFile raf = new RandomAccessFile(path, "r");
            ELFHeader header = ELFLoader.readELFHeader(raf);
            ELFSectionHeaderTable elfSHT = ELFLoader.readSHT(raf, header);
            ELFSymbolLookup elfSym = new ELFSymbolLookup(raf, header, elfSHT);
            for (Map.Entry<String, List<ELFSymbolTable.Entry>> mapEntry : elfSym.symbolMap.entrySet()) {
                List<ELFSymbolTable.Entry> entryList = mapEntry.getValue();
                for (ELFSymbolTable.Entry entry : entryList) {
                    ELFSymbolTable.Entry64 entry64 = (ELFSymbolTable.Entry64) entry;
                    if (entry64.isFunction() && entry64.st_value != 0) {
                        ELFSectionHeaderTable.Entry64 section = getSection(elfSHT, entry64.st_shndx);
                        if (section.getName().equals(".text")) {
                            functionList.add(new TeleNativeFunction(vm(), entry64.getName(), Address.fromLong(entry64.st_value), this));
                            if (sentinel != null && entry64.getName().equals(sentinel)) {
                                sentinelOffset = entry64.st_value;
                                base = Address.fromLong(section.sh_addr);
                                length = section.sh_size;
                            }
                        }
                    }
                }
            }
            raf.close();
            // relocate the offsets to be from the start of the .text section
            for (TeleNativeFunction f : functionList) {
                f.base = f.base.minus(base);
            }
            return sentinelOffset;
        }

        private ELFSectionHeaderTable.Entry64 getSection(ELFSectionHeaderTable elfSHT, short shIndex) {
            return (ELFSectionHeaderTable.Entry64) elfSHT.entries[shIndex];
        }
    }

    static class Linux extends Unix {
        Linux(TeleVM vm, String path, Address base) {
            super(vm, path, base);
        }


    }

    static class Solaris extends Unix {
        Solaris(TeleVM vm, String path, Address base) {
            super(vm, path, base);
        }


    }

}
