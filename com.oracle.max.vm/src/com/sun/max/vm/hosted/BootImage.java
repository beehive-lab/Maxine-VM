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
package com.sun.max.vm.hosted;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.BootImage.StringInfo.Key;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * The <i>boot image</i> contains the heap objects that represent a Maxine VM, including compiled code.
 * This class is a utility for both writing and reading a boot image.
 *
 * The layout of the boot image as a pseudo-C struct is:
 * <pre>
 *
 *     struct BootImage {
 *          struct Header header;            // see declaration of image_Header in Native/substrate/image.h
 *          struct StringInfo string_info;   // see declaration of image_StringInfo in Native/substrate/image.h
 *          byte[header.relocationDataSize] relocation_data;
 *          byte[] pad;                      // padding such that next field will be aligned on an OS page-size address
 *          byte[header.heapSize];           // header.heapSize is a multiple of page-size
 *          byte[header.codeSize];           // header.codeSize is a multiple of page-size
 *          struct Trailer trailer;          // see declaration of image_Trailer in Native/substrate/image.h
 *     }
 *
 * </pre>
 *
 * The 'pad' member in the image exists so that in-memory images can be supported. That is, if the
 * VM obtains the image from a memory location as opposed to loading it from a file, then the
 * page-alignment requirement for the heap and code sections in the image will be satisfied if the
 * image itself starts at a page-aligned address.
 *
 */
public class BootImage {

    /**
     * A special identifier for Maxine boot image files. {@code 0xCAFE4DAD}
     */
    public static final int IDENTIFICATION = 0xcafe4dad;

    /**
     * A version number of the boot image file layout, checked against IMAGE_FORMAT_VERSION in Native/substrate/image.c .
     */
    public static final int BOOT_IMAGE_FORMAT_VERSION = 2;

    /**
     * A field section in a boot image is described by the {@code public final} and {@code final}
     * fields in a {@code FieldSection} subclass. That is, every {@code public final} field of the
     * specified {@linkplain #fieldType() type} declared in a subclass of {@code FieldSection}
     * describes a section entry.
     */
    public abstract static class FieldSection {

        private final int offset;

        protected FieldSection(int offset) {
            this.offset = offset;
        }

        static Field[] fields(Class holder, final Class fieldType) {
            Field[] declaredFields = holder.getDeclaredFields();
            ArrayList<Field> result = new ArrayList<Field>(declaredFields.length);
            for (Field declaredField : declaredFields) {
                final int flags = Actor.ACC_FINAL | Actor.ACC_PUBLIC;
                if ((declaredField.getModifiers() & flags) == flags && declaredField.getType().equals(fieldType)) {
                    result.add(declaredField);
                }
            }
            return result.toArray(new Field[result.size()]);
        }

        /**
         * Gets the fields representing the entries of this section.
         */
        public Field[] fields() {
            return fields(getClass(), fieldType());
        }

        /**
         * Gets the type of the fields in this class that describe section entries.
         */
        public abstract Class<?> fieldType();

        /**
         * Gets the size (in bytes) of this section within the boot image.
         */
        public abstract int size();

        /**
         * Gets the offset (in bytes) of this section within the boot image.
         */
        public final int offset() {
            return offset;
        }

        /**
         * Writes the data in this section to a given stream.
         */
        public abstract void write(OutputStream outputStream, Endianness endian) throws IOException;
    }

    /**
     * A section composed of {@code int} entries.
     */
    public abstract static class IntSection extends FieldSection {
        private final Endianness endianness;

        public Endianness endianness() {
            return endianness;
        }

        protected IntSection(Endianness endianness, int offset) {
            super(offset);
            this.endianness = endianness;
        }

        @Override
        public int size() {
            return fields().length * Ints.SIZE;
        }

        @Override
        public void write(OutputStream outputStream, Endianness endian) throws IOException {
            for (Field field : fields()) {
                try {
                    endianness.writeInt(outputStream, field.getInt(this));
                } catch (IllegalAccessException illegalAccessException) {
                    throw ProgramError.unexpected();
                }
            }
        }

        @Override
        public Class<?> fieldType() {
            return int.class;
        }
    }

    static int getCriticalEntryPoint(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
        return classMethodActor.currentTargetMethod().getEntryPoint(callEntryPoint).toInt();
    }

    /**
     * The header section in a boot image.
     *
     * <b>ATTENTION: this must match 'image_Header' in "com.oracle.max.vm.native/substrate/image.h".</b>
     */
    public static final class Header extends IntSection {
        public final int isBigEndian;

        public final int identification;
        public final int bootImageFormatVersion;
        public final int randomID;

        public final int wordSize;
        public final int cacheAlignment;

        public final int pageSize;
        public final int yellowZonePages;
        public final int redZonePages;

        public final int vmRunMethodOffset;
        public final int vmThreadAddMethodOffset;
        public final int vmThreadRunMethodOffset;
        public final int vmThreadAttachMethodOffset;
        public final int vmThreadDetachMethodOffset;

        public final int classRegistryOffset;

        public final int stringInfoSize;
        public final int relocationDataSize;

        public final int heapSize;
        public final int codeSize;

        public final int dynamicHeapRegionsArrayFieldOffset;

        /**
         * Instruct the boot image loader to reserve a range of contiguous virtual space of specified size.
         */
        public final int reservedVirtualSpaceSize;

        /**
         * Offset to the variable that will hold the address of the virtual space reserved by the boot image loader at boot-load time.
         *
         * @see Heap#reservedVirtualSpace
         */
        public final int reservedVirtualSpaceFieldOffset;

        /**
         * Instruct the boot image loader to memory map the boot heap region at a specific  location:
         * if 0, just map it anywhere; if 1 (respectively, 2), map it at the beginning (respectively, end),of the reserved virtual space.
         */
        public final int bootRegionMappingConstraint;

        /**
         * @see VmThreadMap#ACTIVE
         */
        public final int tlaListHeadOffset;

        /**
         * @see MaxineVM#exitCode
         */
        public final int exitCodeOffset;

        /**
         * The size of a TLA.
         */
        public final int tlaSize;

        /**
         * The indexes of the VM thread locals accessed directly by C code.
         */
        public final int SAFEPOINT_LATCH;
        public final int ETLA;
        public final int DTLA;
        public final int TTLA;
        public final int NATIVE_THREAD_LOCALS;
        public final int FORWARD_LINK;
        public final int BACKWARD_LINK;
        public final int ID;
        public final int JNI_ENV;
        public final int LAST_JAVA_FRAME_ANCHOR;
        public final int TRAP_NUMBER;
        public final int TRAP_INSTRUCTION_POINTER;
        public final int TRAP_FAULT_ADDRESS;
        public final int TRAP_LATCH_REGISTER;
        public final int STACK_REFERENCE_MAP;
        public final int STACK_REFERENCE_MAP_SIZE;

        public WordWidth wordWidth() {
            return WordWidth.fromInt(wordSize * 8);
        }

        private Header(DataInputStream dataInputStream) throws IOException, BootImageException {
            super(dataInputStream.readInt() == 0 ? Endianness.LITTLE : Endianness.BIG, 0);
            final Endianness endian = endianness();
            isBigEndian = endian.ordinal();

            identification = endian.readInt(dataInputStream);
            bootImageFormatVersion = endian.readInt(dataInputStream);
            randomID = endian.readInt(dataInputStream);

            wordSize = endian.readInt(dataInputStream);
            cacheAlignment = endian.readInt(dataInputStream);

            pageSize = endian.readInt(dataInputStream);
            yellowZonePages = endian.readInt(dataInputStream);
            redZonePages = endian.readInt(dataInputStream);

            vmRunMethodOffset = endian.readInt(dataInputStream);
            vmThreadAddMethodOffset = endian.readInt(dataInputStream);
            vmThreadRunMethodOffset = endian.readInt(dataInputStream);
            vmThreadAttachMethodOffset = endian.readInt(dataInputStream);
            vmThreadDetachMethodOffset = endian.readInt(dataInputStream);
            classRegistryOffset = endian.readInt(dataInputStream);

            stringInfoSize = endian.readInt(dataInputStream);
            relocationDataSize = endian.readInt(dataInputStream);

            heapSize = endian.readInt(dataInputStream);
            codeSize = endian.readInt(dataInputStream);

            dynamicHeapRegionsArrayFieldOffset = endian.readInt(dataInputStream);
            reservedVirtualSpaceSize = endian.readInt(dataInputStream);
            reservedVirtualSpaceFieldOffset = endian.readInt(dataInputStream);
            bootRegionMappingConstraint = endian.readInt(dataInputStream);
            tlaListHeadOffset = endian.readInt(dataInputStream);
            exitCodeOffset = endian.readInt(dataInputStream);

            tlaSize = endian.readInt(dataInputStream);

            SAFEPOINT_LATCH = endian.readInt(dataInputStream);
            ETLA = endian.readInt(dataInputStream);
            DTLA = endian.readInt(dataInputStream);
            TTLA = endian.readInt(dataInputStream);
            NATIVE_THREAD_LOCALS = endian.readInt(dataInputStream);
            FORWARD_LINK = endian.readInt(dataInputStream);
            BACKWARD_LINK = endian.readInt(dataInputStream);
            ID = endian.readInt(dataInputStream);
            JNI_ENV = endian.readInt(dataInputStream);
            LAST_JAVA_FRAME_ANCHOR = endian.readInt(dataInputStream);
            TRAP_NUMBER = endian.readInt(dataInputStream);
            TRAP_INSTRUCTION_POINTER = endian.readInt(dataInputStream);
            TRAP_FAULT_ADDRESS = endian.readInt(dataInputStream);
            TRAP_LATCH_REGISTER = endian.readInt(dataInputStream);
            STACK_REFERENCE_MAP = endian.readInt(dataInputStream);
            STACK_REFERENCE_MAP_SIZE = endian.readInt(dataInputStream);
        }

        private int staticFieldPointerOffset(DataPrototype dataPrototype, Class javaClass, String staticFieldName) {
            final Pointer staticTupleOrigin = dataPrototype.objectToOrigin(ClassActor.fromJava(javaClass).staticTuple());
            final FieldActor fieldActor = ClassActor.fromJava(javaClass).findLocalStaticFieldActor(SymbolTable.makeSymbol(staticFieldName));
            return staticTupleOrigin.toInt() + fieldActor.offset();
        }

        private Header(DataPrototype dataPrototype, int stringInfoSize) {
            super(platform().endianness(), 0);
            final VMConfiguration vmConfiguration = vmConfig();
            isBigEndian = endianness() == Endianness.LITTLE ? 0 : 0xffffffff;
            identification = IDENTIFICATION;
            bootImageFormatVersion = BOOT_IMAGE_FORMAT_VERSION;
            randomID = UUID.randomUUID().hashCode();
            wordSize = platform().wordWidth().numberOfBytes;
            cacheAlignment = platform().dataModel.cacheAlignment;
            pageSize = platform().pageSize;
            yellowZonePages = VmThread.YELLOW_ZONE_PAGES;
            redZonePages = VmThread.RED_ZONE_PAGES;
            vmRunMethodOffset = getCriticalEntryPoint(ClassRegistry.MaxineVM_run, CallEntryPoint.C_ENTRY_POINT);
            vmThreadAddMethodOffset = getCriticalEntryPoint(ClassRegistry.VmThread_add, CallEntryPoint.C_ENTRY_POINT);
            vmThreadRunMethodOffset = getCriticalEntryPoint(ClassRegistry.VmThread_run, CallEntryPoint.C_ENTRY_POINT);
            vmThreadAttachMethodOffset = getCriticalEntryPoint(ClassRegistry.VmThread_attach, CallEntryPoint.C_ENTRY_POINT);
            vmThreadDetachMethodOffset = getCriticalEntryPoint(ClassRegistry.VmThread_detach, CallEntryPoint.C_ENTRY_POINT);
            classRegistryOffset = dataPrototype.objectToOrigin(ClassRegistry.BOOT_CLASS_REGISTRY).toInt();
            this.stringInfoSize = stringInfoSize;
            relocationDataSize = dataPrototype.relocationData().length;
            heapSize = dataPrototype.heapData().length;
            codeSize = dataPrototype.codeData().length;

            dynamicHeapRegionsArrayFieldOffset = staticFieldPointerOffset(dataPrototype, InspectableHeapInfo.class, "dynamicHeapMemoryRegions");

            reservedVirtualSpaceSize = vmConfiguration.heapScheme().reservedVirtualSpaceKB();
            reservedVirtualSpaceFieldOffset = staticFieldPointerOffset(dataPrototype, Heap.class, "reservedVirtualSpace");
            bootRegionMappingConstraint = vmConfiguration.heapScheme().bootRegionMappingConstraint().ordinal();
            tlaListHeadOffset = dataPrototype.objectToOrigin(VmThreadMap.ACTIVE).toInt() + ClassActor.fromJava(VmThreadMap.class).findLocalInstanceFieldActor("tlaListHead").offset();
            exitCodeOffset = staticFieldPointerOffset(dataPrototype, MaxineVM.class, "exitCode");

            tlaSize = VmThreadLocal.tlaSize().toInt();

            SAFEPOINT_LATCH = VmThreadLocal.SAFEPOINT_LATCH.index;
            ETLA = VmThreadLocal.ETLA.index;
            DTLA = VmThreadLocal.DTLA.index;
            TTLA = VmThreadLocal.TTLA.index;
            NATIVE_THREAD_LOCALS = VmThreadLocal.NATIVE_THREAD_LOCALS.index;
            FORWARD_LINK = VmThreadLocal.FORWARD_LINK.index;
            BACKWARD_LINK = VmThreadLocal.BACKWARD_LINK.index;
            ID = VmThreadLocal.ID.index;
            JNI_ENV = VmThreadLocal.JNI_ENV.index;
            LAST_JAVA_FRAME_ANCHOR = VmThreadLocal.LAST_JAVA_FRAME_ANCHOR.index;
            TRAP_NUMBER = VmThreadLocal.TRAP_NUMBER.index;
            TRAP_INSTRUCTION_POINTER = VmThreadLocal.TRAP_INSTRUCTION_POINTER.index;
            TRAP_FAULT_ADDRESS = VmThreadLocal.TRAP_FAULT_ADDRESS.index;
            TRAP_LATCH_REGISTER = VmThreadLocal.TRAP_LATCH_REGISTER.index;
            STACK_REFERENCE_MAP = VmThreadLocal.STACK_REFERENCE_MAP.index;
            STACK_REFERENCE_MAP_SIZE = VmThreadLocal.STACK_REFERENCE_MAP_SIZE.index;
        }

        public void check() throws BootImageException {
            BootImageException.check(identification == IDENTIFICATION, "not a MaxineVM VM boot image file, wrong identification: " + identification);
            BootImageException.check(bootImageFormatVersion == BOOT_IMAGE_FORMAT_VERSION, "wrong version: " + bootImageFormatVersion);
            BootImageException.check(wordSize == 4 || wordSize == 8, "illegal word size: " + wordSize);
            BootImageException.check(cacheAlignment > 4 && Ints.isPowerOfTwoOrZero(cacheAlignment), "implausible alignment size: " + cacheAlignment);
            BootImageException.check(pageSize >= Longs.K && pageSize % Longs.K == 0, "implausible page size: " + pageSize);
            BootImageException.check(!(bootRegionMappingConstraint > 0 && reservedVirtualSpaceSize == 0), "invalid boot region mapping constraint");
        }

        @Override
        public int size() {
            return fields().length * Ints.SIZE;
        }

        @Override
        public void write(OutputStream outputStream, Endianness endian) throws IOException {
            for (Field field : fields()) {
                try {
                    endianness().writeInt(outputStream, field.getInt(this));
                } catch (IllegalAccessException illegalAccessException) {
                    throw ProgramError.unexpected();
                }
            }
        }
    }

    /**
     * The string info section in a boot image.
     *
     * <b>ATTENTION: this must match 'image_StringInfo' in "com.oracle.max.vm.native/substrate/image.h".</b>
     */
    public static final class StringInfo extends FieldSection {
        public enum Key {
            BUILD(BuildLevel.class),
            CPU(CPU.class),
            ISA(ISA.class),
            OS(OS.class),
            NSIG(String.class),
            REFERENCE(BootImagePackage.class),
            LAYOUT(BootImagePackage.class),
            HEAP(BootImagePackage.class),
            MONITOR(BootImagePackage.class),
            RUN(BootImagePackage.class),
            OPT(String.class),
            BASELINE(String.class);

            Key(Class valueType) {
                this.valueType = valueType;
            }
            public final Class valueType;

            public static Key toKey(String name) {
                try {
                    return valueOf(name);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        public final Map<String, String> values = new TreeMap<String, String>();

        public BuildLevel buildLevel() {
            return BuildLevel.valueOf(values.get(Key.BUILD.name()));
        }

        public CPU cpu() {
            return CPU.valueOf(values.get(Key.CPU.name()));
        }

        public ISA isa() {
            return ISA.valueOf(values.get(Key.ISA.name()));
        }

        public OS os() {
            return OS.valueOf(values.get(Key.OS.name()));
        }

        public int nsig() {
            return Integer.valueOf(values.get(Key.OS.name()));
        }

        public BootImagePackage bootImagePackage(Key key) {
            return BootImagePackage.fromName(values.get(key.name()));
        }

        private StringInfo(InputStream inputStream, int offset, Endianness endian) throws IOException, Utf8Exception {
            super(offset);
            int count = endian.readInt(inputStream);
            while (count-- != 0) {
                String name = Utf8.readString(inputStream);
                String value = Utf8.readString(inputStream);
                put(name, value);
                Key key = Key.toKey(name);
                if (key == null) {
                    // This is a system property used by one of the schemes.
                    // Set it now so that it is available to the scheme when
                    // it is instantiated.
                    System.setProperty(name, value);
                }
            }
        }

        private void put(Key key, Object value) {
            put(key.name(), value);
        }

        private void put(String key, Object value) {
            String oldValue = values.put(key, value.toString());
            assert oldValue == null : "Multiple values for " + key + ": " + oldValue + ", " + value;
        }

        private StringInfo(VMConfiguration vmConfig, int offset) {
            super(offset);
            put(Key.BUILD, vmConfig.buildLevel);
            put(Key.CPU, platform().cpu);
            put(Key.ISA, platform().isa);
            put(Key.OS, platform().os);
            put(Key.REFERENCE, vmConfig.referencePackage);
            put(Key.LAYOUT, vmConfig.layoutPackage);
            put(Key.HEAP, vmConfig.heapPackage);
            put(Key.MONITOR, vmConfig.monitorPackage);
            put(Key.RUN, vmConfig.runPackage);

            for (VMScheme scheme : vmConfig.vmSchemes()) {
                Properties props = scheme.properties();
                if (props != null) {
                    for (Object k : props.keySet()) {
                        String key = (String) k;
                        String value = props.getProperty(key);
                        put(key, value);

                    }
                }
            }

            Properties props = MaxineVM.vm().compilationBroker.properties();
            if (props != null) {
                for (Object k : props.keySet()) {
                    String key = (String) k;
                    String value = props.getProperty(key);
                    put(key, value);

                }
            }
        }

        public void check() throws BootImageException {
            for (Map.Entry<String, String> e : values.entrySet()) {
                Key key = Key.toKey(e.getKey());
                if (key != null) {
                    String value = e.getValue();
                    if (Enum.class.isAssignableFrom(key.valueType)) {
                        Enum[] enumConstants = (Enum[]) key.valueType.getEnumConstants();
                        boolean match = false;
                        if (enumConstants != null) {
                            for (Enum c : enumConstants) {
                                if (c.name().equalsIgnoreCase(value)) {
                                    match = true;
                                    break;
                                }
                            }
                        }
                        BootImageException.check(match, "No " + key.valueType.getName() + " constant matches " + value);
                    } else {
                        assert key.valueType == BootImagePackage.class;
                    }
                } else {
                    // must be a system property for one of the schemes
                }
            }
        }

        @Override
        public int size() {
            int size = 4;
            for (Map.Entry<String, String> e : values.entrySet()) {
                size += Utf8.utf8Length(e.getKey()) + 1;
                size += Utf8.utf8Length(e.getValue()) + 1;
            }
            return size;
        }

        @Override
        public void write(OutputStream outputStream, Endianness endian) throws IOException {
            endian.writeInt(outputStream, values.size());
            for (Map.Entry<String, String> e : values.entrySet()) {
                Utf8.writeString(outputStream, e.getKey());
                Utf8.writeString(outputStream, e.getValue());
            }
        }

        @Override
        public Class<?> fieldType() {
            return String.class;
        }
    }

    /**
     * The trailer section in a boot image.
     *
     * <b>ATTENTION: this must match 'image_Trailer' in "com.oracle.max.vm.native/substrate/image.h".</b>
     */
    public static final class Trailer extends IntSection {
        public final int randomID;
        public final int bootImageFormatVersion;
        public final int identification;

        private Trailer(Header header, InputStream inputStream, int offset) throws IOException {
            super(header.endianness(), offset);
            randomID = endianness().readInt(inputStream);
            bootImageFormatVersion = endianness().readInt(inputStream);
            identification = endianness().readInt(inputStream);
        }

        private Trailer(Header header, int offset) {
            super(header.endianness(), offset);
            randomID = header.randomID;
            bootImageFormatVersion = header.bootImageFormatVersion;
            identification = header.identification;
        }

        public void check(Header header) throws BootImageException {
            BootImageException.check(identification == header.identification, "inconsistent trailer identififcation");
            BootImageException.check(bootImageFormatVersion == header.bootImageFormatVersion, "inconsistent trailer version");
            BootImageException.check(randomID == header.randomID, "inconsistent trailer random ID");
        }
    }

    /**
     * Gets the class method actor for the first method with the specified name found
     * while traversing all the class method actors declared by a given class and
     * its super classes.
     * @param n the method name
     * @param javaClass the class in which to start the search for a method named "run"
     *
     * @return the found method or null
     */
    public static ClassMethodActor getMethodActorFor(String n, Class<?> javaClass) {
        Utf8Constant name = SymbolTable.makeSymbol(n);
        final ClassMethodActor runMethodActor = ClassActor.fromJava(javaClass).findLocalClassMethodActor(name, null);
        if (runMethodActor != null) {
            return runMethodActor;
        }
        return getMethodActorFor(n, javaClass.getSuperclass());
    }

    public final Header header;
    public final StringInfo stringInfo;
    public final byte[] relocationData;
    public final byte[] padding;
    public final Trailer trailer;
    public final VMConfiguration vmConfiguration;

    private ByteBuffer heap;
    private ByteBuffer code;
    private ByteBuffer heapAndCode;
    public final File imageFile;

    /**
     * Creates a BootImage object representing the information in a given boot image file.
     */
    public BootImage(File file) throws BootImageException {
        this.imageFile = file;
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            try {
                DataInputStream dataInputStream = new DataInputStream(fileInputStream);
                header = new Header(dataInputStream);
                header.check();
                stringInfo = new StringInfo(dataInputStream, header.size(), header.endianness());
                stringInfo.check();
                BootImageException.check(header.stringInfoSize == stringInfo.size(), "inconsistent string area size");
                relocationData = new byte[header.relocationDataSize];
                fileInputStream.read(relocationData);
                this.padding = new byte[deltaToPageAlign(header.size() + stringInfo.size() + relocationData.length)];
                fileInputStream.read(padding);

                for (int i = 0; i < padding.length; ++i) {
                    BootImageException.check(padding[i] == 0, "all padding bytes should be 0");
                }
                BootImageException.check((heapOffset() % header.pageSize) == 0, "heap offset is not page-size aligned");
                BootImageException.check((codeOffset() % header.pageSize) == 0, "code offset is not page-size aligned");

                final DataModel dataModel = new DataModel(header.wordWidth(), header.endianness(), header.cacheAlignment);
                final Platform platform = new Platform(stringInfo.cpu(), stringInfo.isa(), dataModel, stringInfo.os(), header.pageSize, stringInfo.nsig());
                Platform.set(platform);
                vmConfiguration = new VMConfiguration(stringInfo.buildLevel(),
                                                      platform,
                                                      stringInfo.bootImagePackage(Key.REFERENCE),
                                                      stringInfo.bootImagePackage(Key.LAYOUT),
                                                      stringInfo.bootImagePackage(Key.HEAP),
                                                      stringInfo.bootImagePackage(Key.MONITOR),
                                                      stringInfo.bootImagePackage(Key.RUN));

                fileInputStream.skip(header.heapSize + header.codeSize);
                int trailerOffset = codeOffset() + header.codeSize;
                trailer = new Trailer(header, fileInputStream, trailerOffset);
                trailer.check(header);
            } catch (Utf8Exception utf8Exception) {
                throw new BootImageException(utf8Exception);
            } finally {
                fileInputStream.close();
            }
        } catch (IOException ioException) {
            throw new BootImageException(ioException);
        }
    }

    /**
     * Used when constructing a boot image to be written to a file.
     */
    public BootImage(DataPrototype dataPrototype) throws BootImageException {
        this.vmConfiguration = vmConfig();
        this.stringInfo = new StringInfo(vmConfiguration, new Header(dataPrototype, 0).size());
        this.stringInfo.check();
        this.header = new Header(dataPrototype, stringInfo.size());
        this.header.check();
        this.relocationData = dataPrototype.relocationData();
        this.padding = new byte[deltaToPageAlign(header.size() + stringInfo.size() + relocationData.length)];
        this.heap = ByteBuffer.wrap(dataPrototype.heapData());
        this.code = ByteBuffer.wrap(dataPrototype.codeData());
        int trailerOffset = codeOffset() + header.codeSize;
        this.trailer = new Trailer(header, trailerOffset);
        this.imageFile = null;
    }

    public int relocationDataOffset() {
        return header.size() + stringInfo.size();
    }

    public int heapOffset() {
        return paddingOffset() + paddingSize();
    }

    public int codeOffset() {
        return heapOffset() + header.heapSize;
    }

    public int paddingOffset() {
        return relocationDataOffset() + header.relocationDataSize;
    }

    public int paddingSize() {
        return padding.length;
    }

    public synchronized ByteBuffer heap() {
        if (heap == null) {
            heap = mapSection(heapOffset(), header.heapSize);
        }
        ByteBuffer duplicate = heap.duplicate();
        duplicate.order(heap.order());
        return duplicate;
    }

    public synchronized ByteBuffer code() {
        if (code == null) {
            code = mapSection(codeOffset(), header.codeSize);
        }
        ByteBuffer duplicate = code.duplicate();
        duplicate.order(code.order());
        return duplicate;
    }

    public synchronized ByteBuffer heapAndCode() {
        if (heapAndCode == null) {
            heapAndCode = mapSection(heapOffset(), header.heapSize + header.codeSize);
        }
        ByteBuffer duplicate = heapAndCode.duplicate();
        duplicate.order(heapAndCode.order());
        return duplicate;
    }

    /**
     * Writes this image to a given stream.
     *
     * @param outputStream
     * @throws IOException
     */
    public void write(OutputStream outputStream) throws IOException {
        header.write(outputStream, header.endianness());
        stringInfo.write(outputStream, header.endianness());
        outputStream.write(relocationData);
        outputStream.write(padding);
        write(heap(), outputStream);
        write(code(), outputStream);
        trailer.write(outputStream, header.endianness());
    }

    private void write(ByteBuffer buffer, OutputStream outputStream) throws IOException {
        if (buffer.hasArray()) {
            outputStream.write(buffer.array(), buffer.arrayOffset(), buffer.limit());
        } else {
            byte[] array = new byte[buffer.limit()];
            buffer.get(array);
            outputStream.write(array);
        }
    }

    /**
     * Computes the amount to add to a given size to bump it up to the next page-aligned size.
     */
    private int deltaToPageAlign(int size) {
        final int pageSize = header.pageSize;
        final int rest = size % pageSize;
        if (rest == 0) {
            return 0;
        }
        return pageSize - rest;
    }

    /**
     * Maps a section of an image file to memory.
     *
     * @param offset the offset within the image file to map
     * @param size the size of the image file section to map
     * @return a buffer representing the mapped section
     */
    private ByteBuffer mapSection(int offset, int size) {
        assert imageFile != null : "Cannot map section of image for which underlying file is not available";
        try {
            final RandomAccessFile raf = new RandomAccessFile(imageFile, "r");
            final MappedByteBuffer buffer = raf.getChannel().map(MapMode.READ_ONLY, offset, size);
            raf.close();
            ByteOrder byteOrder = platform().endianness().asByteOrder();
            buffer.order(byteOrder);
            raf.close();
            return buffer;
        } catch (IOException e) {
            throw new InternalError("Error trying to map section of image file: " + e);
        }
    }

    private static native void nativeRelocate(long heap, long relocatedHeap, byte[] relocationDataPointer, int relocationDataSize, int isBigEndian, int wordSize);

    /**
     * Relocates the pointers in the heap and code. All the pointers are assumed to be
     * canonicalized; their current values assume that the heap and code start address 0.
     *
     * @param heap the physical address at which the (contiguous) heap and code reside
     * @param relocatedHeap the logical address to which the heap and code is being relocated
     */
    public void relocate(long heap, Address relocatedHeap) {
        nativeRelocate(heap, relocatedHeap.toLong(), relocationData, relocationData.length, header.isBigEndian, header.wordSize);
    }
}
