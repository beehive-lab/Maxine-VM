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
package com.sun.max.vm.prototype;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.CompilationScheme.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * The <i>boot image</i> contains the heap objects that represent a Maxine VM,
 * including compiled code.
 * This class organizes both writing and reading a boot image.
 * Whereas writing may be directed to any output stream,
 * reading assumes a binary file.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BootImage {

    public enum RelocationScheme {
        DEFAULT
    }

    /**
     * A special identifier for Maxine boot image files. {@code 0xCAFE4DAD}
     */
    public static final int IDENTIFICATION = 0xcafe4dad;

    /**
     * A version number of Maxine.
     */
    public static final int VERSION = 1;

    public abstract static class Section {
        protected Section() {
        }

        static Field[] fields(Class holder, final Class fieldType) {
            return Arrays.filter(holder.getDeclaredFields(), new Predicate<Field>() {
                public boolean evaluate(Field field) {
                    final int flags = Actor.ACC_FINAL | Actor.ACC_PUBLIC;
                    if ((field.getModifiers() & flags) == flags && field.getType().equals(fieldType)) {
                        return true;
                    }
                    return false;
                }
            }, new Field[0]);
        }

        protected Field[] fields() {
            return fields(getClass(), fieldType());
        }

        public abstract Class<?> fieldType();
        public abstract int size();
        public abstract void write(OutputStream outputStream) throws IOException;
    }

    public abstract static class IntSection extends Section {
        private final Endianness endianness;

        public Endianness endianness() {
            return endianness;
        }

        protected IntSection(Endianness endianness) {
            this.endianness = endianness;
        }

        @Override
        public int size() {
            int size = 0;
            for (Field field : fields()) {
                assert field.getType() == int.class;
                size += Ints.SIZE;
            }
            return size;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            for (Field field : fields()) {
                try {
                    endianness.writeInt(outputStream, field.getInt(this));
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected();
                }
            }
        }

        @Override
        public Class<?> fieldType() {
            return int.class;
        }
    }

    private static final Utf8Constant run = SymbolTable.makeSymbol("run");

    /**
     * Gets the class method actor for the first method with the name "run" found
     * while traversing all the class method actors declared by a given class and
     * its super classes.
     *
     * @param javaClass the class in which to start the search for a method named "run"
     * @return the found method or null
     */
    public static ClassMethodActor getRunMethodActor(Class<?> javaClass) {
        final ClassMethodActor runMethodActor = ClassActor.fromJava(javaClass).findLocalClassMethodActor(run, null);
        if (runMethodActor != null) {
            return runMethodActor;
        }
        return getRunMethodActor(javaClass.getSuperclass());
    }

    /**
     * ATTENTION: this must match 'image_HeaderStruct' in "Native/substrate/image.h".
     *
     * @author Bernd Mathiske
     */
    public static final class Header extends IntSection {
        public final int isBigEndian;

        public final int identification;
        public final int version;
        public final int randomID;

        public final int wordSize;
        public final int cacheAlignment;
        public final int relocationScheme;

        public final int pageSize;

        public final int vmThreadLocalsSize;
        public final int vmThreadLocalsTrapNumberOffset;

        public final int vmRunMethodOffset;
        public final int vmThreadRunMethodOffset;
        public final int runSchemeRunMethodOffset;

        public final int classRegistryOffset;

        public final int stringInfoSize;
        public final int relocationDataSize;

        public final int bootHeapSize;
        public final int bootCodeSize;
        public final int codeCacheSize;

        public final int heapRegionsPointerOffset;
        public final int codeRegionsPointerOffset;

        public final int auxiliarySpaceSize;

        /**
         * @see MaxineMessenger#_info
         */
        public final int messengerInfoOffset;

        /**
         * @see VmThread#threadSpecificsList
         */
        public final int threadSpecificsListOffset;

        public WordWidth wordWidth() {
            return WordWidth.fromInt(wordSize * 8);
        }

        private Header(DataInputStream dataInputStream) throws IOException {
            super(dataInputStream.readInt() == 0 ? Endianness.LITTLE : Endianness.BIG);
            final Endianness endian = endianness();
            isBigEndian = endian.ordinal();

            identification = endian.readInt(dataInputStream);
            version = endian.readInt(dataInputStream);
            randomID = endian.readInt(dataInputStream);

            wordSize = endian.readInt(dataInputStream);
            cacheAlignment = endian.readInt(dataInputStream);
            relocationScheme = endian.readInt(dataInputStream);

            pageSize = endian.readInt(dataInputStream);

            vmThreadLocalsSize = endian.readInt(dataInputStream);
            vmThreadLocalsTrapNumberOffset = endian.readInt(dataInputStream);

            vmRunMethodOffset = endian.readInt(dataInputStream);
            vmThreadRunMethodOffset = endian.readInt(dataInputStream);
            runSchemeRunMethodOffset = endian.readInt(dataInputStream);
            classRegistryOffset = endian.readInt(dataInputStream);

            stringInfoSize = endian.readInt(dataInputStream);
            relocationDataSize = endian.readInt(dataInputStream);

            bootHeapSize = endian.readInt(dataInputStream);
            bootCodeSize = endian.readInt(dataInputStream);
            codeCacheSize = endian.readInt(dataInputStream);

            heapRegionsPointerOffset = endian.readInt(dataInputStream);
            codeRegionsPointerOffset = endian.readInt(dataInputStream);

            auxiliarySpaceSize = endian.readInt(dataInputStream);

            messengerInfoOffset = endian.readInt(dataInputStream);
            threadSpecificsListOffset = endian.readInt(dataInputStream);
        }

        private int staticFieldPointerOffset(DataPrototype dataPrototype, Class javaClass, String staticFieldName) {
            final Pointer staticTupleOrigin = dataPrototype.objectToOrigin(ClassActor.fromJava(javaClass).staticTuple());
            final FieldActor fieldActor = ClassActor.fromJava(javaClass).findLocalStaticFieldActor(SymbolTable.makeSymbol(staticFieldName));
            return staticTupleOrigin.toInt() + fieldActor.offset();
        }

        private Header(DataPrototype dataPrototype, StringInfo stringInfo) {
            super(dataPrototype.vmConfiguration().platform().processorKind.dataModel.endianness);
            final VMConfiguration vmConfiguration = dataPrototype.vmConfiguration();
            isBigEndian = endianness() == Endianness.LITTLE ? 0 : 0xffffffff;
            identification = IDENTIFICATION;
            version = VERSION;
            randomID = UUID.randomUUID().hashCode();
            wordSize = vmConfiguration.platform().processorKind.dataModel.wordWidth.numberOfBytes;
            cacheAlignment = vmConfiguration.platform().processorKind.dataModel.cacheAlignment;
            relocationScheme = RelocationScheme.DEFAULT.ordinal();
            pageSize = vmConfiguration.platform().pageSize;
            vmThreadLocalsSize = VmThreadLocal.threadLocalStorageSize().toInt();
            vmThreadLocalsTrapNumberOffset = VmThreadLocal.TRAP_NUMBER.offset;
            vmRunMethodOffset = Static.getCriticalEntryPoint(getRunMethodActor(MaxineVM.class), CallEntryPoint.C_ENTRY_POINT).toInt();
            vmThreadRunMethodOffset = Static.getCriticalEntryPoint(getRunMethodActor(VmThread.class), CallEntryPoint.C_ENTRY_POINT).toInt();
            runSchemeRunMethodOffset = Static.getCriticalEntryPoint(getRunMethodActor(vmConfiguration.runScheme().getClass()), CallEntryPoint.OPTIMIZED_ENTRY_POINT).toInt();
            classRegistryOffset = dataPrototype.objectToOrigin(ClassRegistry.vmClassRegistry()).toInt();
            stringInfoSize = stringInfo.size();
            relocationDataSize = dataPrototype.relocationData().length;
            bootHeapSize = dataPrototype.heapData().length;
            bootCodeSize = dataPrototype.codeData().length;
            codeCacheSize = CodeManager.CODE_CACHE_SIZE;

            heapRegionsPointerOffset = staticFieldPointerOffset(dataPrototype, InspectableHeapInfo.class, "memoryRegions");
            codeRegionsPointerOffset = staticFieldPointerOffset(dataPrototype, Code.class, "memoryRegions");

            auxiliarySpaceSize = vmConfiguration.heapScheme().auxiliarySpaceSize(bootHeapSize + bootCodeSize);

            messengerInfoOffset = staticFieldPointerOffset(dataPrototype, MaxineMessenger.class, "info");
            threadSpecificsListOffset = staticFieldPointerOffset(dataPrototype, VmThread.class, "threadSpecificsList");
        }

        public void check() throws BootImageException {
            BootImageException.check(identification == IDENTIFICATION, "not a MaxineVM VM boot image file, wrong identification: " + identification);
            BootImageException.check(version == VERSION, "wrong version: " + version);
            BootImageException.check(wordSize == 4 || wordSize == 8, "illegal word size: " + wordSize);
            BootImageException.check(cacheAlignment > 4 && Ints.isPowerOfTwoOrZero(cacheAlignment), "implausible alignment size: " + cacheAlignment);
            BootImageException.check(pageSize >= Longs.K && pageSize % Longs.K == 0, "implausible page size: " + pageSize);
        }

        @Override
        public int size() {
            int size = 0;
            for (Field field : fields()) {
                assert field.getType() == int.class;
                size += Ints.SIZE;
            }
            return size;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            for (Field field : fields()) {
                try {
                    endianness().writeInt(outputStream, field.getInt(this));
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected();
                }
            }
        }
    }

    private final Header header;

    public Header header() {
        return header;
    }

    /**
     * See "image.h".
     */
    public static final class StringInfo extends Section {
        public final String buildLevelName;
        public final String processorModelName;
        public final String instructionSetName;
        public final String operatingSystemName;

        public final String gripPackageName;
        public final String referencePackageName;
        public final String layoutPackageName;
        public final String heapPackageName;
        public final String monitorPackageName;
        public final String compilerPackageName;
        public final String jitPackageName;
        public final String interpreterPackageName;
        public final String trampolinePackageName;
        public final String targetABIsPackageName;
        public final String runPackageName;

        public BuildLevel buildLevel() {
            return Enums.fromString(BuildLevel.class, buildLevelName);
        }

        public ProcessorModel processorModel() {
            return Enums.fromString(ProcessorModel.class, processorModelName);
        }

        public InstructionSet instructionSet() {
            return Enums.fromString(InstructionSet.class, instructionSetName);
        }

        public OperatingSystem operatingSystem() {
            return Enums.fromString(OperatingSystem.class, operatingSystemName);
        }

        public VMPackage gripPackage() {
            return (VMPackage) MaxPackage.fromName(gripPackageName);
        }

        public VMPackage referencePackage() {
            return (VMPackage) MaxPackage.fromName(referencePackageName);
        }

        public VMPackage layoutPackage() {
            return (VMPackage) MaxPackage.fromName(layoutPackageName);
        }

        public VMPackage heapPackage() {
            return (VMPackage) MaxPackage.fromName(heapPackageName);
        }

        public VMPackage monitorPackage() {
            return (VMPackage) MaxPackage.fromName(monitorPackageName);
        }

        public VMPackage compilerPackage() {
            return (VMPackage) MaxPackage.fromName(compilerPackageName);
        }

        public VMPackage jitPackage() {
            if (jitPackageName == null) {
                return null;
            }
            return (VMPackage) MaxPackage.fromName(jitPackageName);
        }

        public VMPackage interpreterPackage() {
            if (interpreterPackageName == null) {
                return null;
            }
            return (VMPackage) MaxPackage.fromName(interpreterPackageName);
        }

        public VMPackage trampolinePackage() {
            return (VMPackage) MaxPackage.fromName(trampolinePackageName);
        }

        public VMPackage targetABIsPackage() {
            return (VMPackage) MaxPackage.fromName(targetABIsPackageName);
        }

        public VMPackage runPackage() {
            return (VMPackage) MaxPackage.fromName(runPackageName);
        }

        private StringInfo(InputStream inputStream) throws IOException, Utf8Exception {
            super();
            buildLevelName = Utf8.readString(inputStream);
            processorModelName = Utf8.readString(inputStream);
            instructionSetName = Utf8.readString(inputStream);
            operatingSystemName = Utf8.readString(inputStream);

            gripPackageName = Utf8.readString(inputStream);
            referencePackageName = Utf8.readString(inputStream);
            layoutPackageName = Utf8.readString(inputStream);
            heapPackageName = Utf8.readString(inputStream);
            monitorPackageName = Utf8.readString(inputStream);
            compilerPackageName = Utf8.readString(inputStream);
            jitPackageName =  Utf8.readString(inputStream);
            interpreterPackageName = Utf8.readString(inputStream);
            trampolinePackageName = Utf8.readString(inputStream);
            targetABIsPackageName = Utf8.readString(inputStream);
            runPackageName = Utf8.readString(inputStream);
        }

        private StringInfo(VMConfiguration vmConfiguration) {
            super();
            buildLevelName = vmConfiguration.buildLevel().name();
            processorModelName = vmConfiguration.platform().processorKind.processorModel.name();
            instructionSetName = vmConfiguration.platform().processorKind.instructionSet.name();
            operatingSystemName = vmConfiguration.platform().operatingSystem.name();

            gripPackageName = vmConfiguration.gripPackage.name();
            referencePackageName = vmConfiguration.referencePackage.name();
            layoutPackageName = vmConfiguration.layoutPackage.name();
            heapPackageName = vmConfiguration.heapPackage.name();
            monitorPackageName = vmConfiguration.monitorPackage.name();
            compilerPackageName = vmConfiguration.compilerPackage.name();
            // Jit Package is optional and may be null. In which case, fall back to the default compiler.
            if (vmConfiguration.jitPackage == null) {
                jitPackageName = compilerPackageName;
            } else {
                jitPackageName = vmConfiguration.jitPackage.name();
            }
            interpreterPackageName = vmConfiguration.interpreterPackage.name();

            trampolinePackageName = vmConfiguration.trampolinePackage.name();
            targetABIsPackageName = vmConfiguration.targetABIsPackage.name();
            runPackageName = vmConfiguration.runPackage.name();
        }

        private void checkPackage(String packageName) throws BootImageException {
            BootImageException.check(MaxPackage.fromName(packageName) instanceof VMPackage, "not a VM package: " + packageName);
        }

        public void check() throws BootImageException {
            BootImageException.check(buildLevel() != null, "unknown build level: " + buildLevelName);
            BootImageException.check(processorModel() != null, "unknown processor model: " + processorModelName);
            BootImageException.check(instructionSet() != null, "unknown instruction set: " + instructionSetName);
            BootImageException.check(operatingSystem() != null, "unknown operating system: " + operatingSystemName);

            checkPackage(gripPackageName);
            checkPackage(referencePackageName);
            checkPackage(layoutPackageName);
            checkPackage(heapPackageName);
            checkPackage(monitorPackageName);
            checkPackage(compilerPackageName);
            checkPackage(jitPackageName);
            checkPackage(interpreterPackageName);
            checkPackage(trampolinePackageName);
            checkPackage(targetABIsPackageName);
            checkPackage(runPackageName);
        }

        @Override
        public int size() {
            int size = 0;
            for (Field field : fields()) {
                try {
                    final String string = (String) field.get(this);
                    size += Utf8.utf8Length(string) + 1;
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected();
                }
            }
            return size;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            for (Field field : fields()) {
                try {
                    final String string = (String) field.get(this);
                    Utf8.writeString(outputStream, string);
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected();
                }
            }
        }
        @Override
        public Class<?> fieldType() {
            return String.class;
        }
    }

    private final StringInfo stringInfo;

    /**
     * See "image.h".
     */
    public static final class Trailer extends IntSection {
        public final int randomID;
        public final int version;
        public final int identification;


        private Trailer(Header header, InputStream inputStream) throws IOException {
            super(header.endianness());
            randomID = endianness().readInt(inputStream);
            version = endianness().readInt(inputStream);
            identification = endianness().readInt(inputStream);
        }

        private Trailer(Header header) {
            super(header.endianness());
            randomID = header.randomID;
            version = header.version;
            identification = header.identification;
        }

        public void check(Header header) throws BootImageException {
            BootImageException.check(identification == header.identification, "inconsistent trailer identififcation");
            BootImageException.check(version == header.version, "inconsistent trailer version");
            BootImageException.check(randomID == header.randomID, "inconsistent trailer random ID");
        }
    }

    private final Trailer trailer;

    public Trailer trailer() {
        return trailer;
    }

    private final VMConfiguration vmConfiguration;

    public VMConfiguration vmConfiguration() {
        return vmConfiguration;
    }

    private static native void nativeRelocate(long heapPointer, int relocationScheme, byte[] relocationDataPointer, int relocationDataSize, int cacheAlignment, int isBigEndian, int wordSize);

    private void relocate(Pointer heap, byte[] relocationData) {
        nativeRelocate(heap.toLong(), header.relocationScheme, relocationData, relocationData.length, header.cacheAlignment, header.isBigEndian, header.wordSize);
    }

    private final DataPrototype dataPrototype;

    public int pagePaddingSize(int numberOfBytesSoFar) throws IOException {
        final int pageSize = vmConfiguration().platform().pageSize;
        final int rest = numberOfBytesSoFar % pageSize;
        if (rest == 0) {
            return 0;
        }
        return pageSize - rest;
    }

    /**
     * Creates a BootImage object representing the information in a given boot image file.
     */
    public BootImage(File file) throws BootImageException {
        this.dataPrototype = null;
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            try {
                header = new Header(new DataInputStream(fileInputStream));
                header.check();
                stringInfo = new StringInfo(fileInputStream);
                stringInfo.check();
                BootImageException.check(header.stringInfoSize == stringInfo.size(), "inconsistent string area size");

                final DataModel dataModel = new DataModel(header.wordWidth(), header.endianness(), header.cacheAlignment);
                final ProcessorKind processorKind = new ProcessorKind(stringInfo.processorModel(), stringInfo.instructionSet(), dataModel);
                final Platform platform = new Platform(processorKind, stringInfo.operatingSystem(), header.pageSize);
                vmConfiguration = new VMConfiguration(stringInfo.buildLevel(), platform,
                                stringInfo.gripPackage(),
                                stringInfo.referencePackage(),
                                stringInfo.layoutPackage(),
                                stringInfo.heapPackage(),
                                stringInfo.monitorPackage(),
                                stringInfo.compilerPackage(),
                                stringInfo.jitPackage(),
                                stringInfo.interpreterPackage(),
                                stringInfo.trampolinePackage(),
                                stringInfo.targetABIsPackage(),
                                stringInfo.runPackage());
                vmConfiguration.loadAndInstantiateSchemes();

                fileInputStream.skip(header.relocationDataSize);
                final int padding = pagePaddingSize(header.size() + header.stringInfoSize + header.relocationDataSize);
                fileInputStream.skip(padding + header.bootHeapSize + header.bootCodeSize);
                trailer = new Trailer(header, fileInputStream);
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
        this.dataPrototype = dataPrototype;
        this.vmConfiguration = dataPrototype.vmConfiguration();
        this.stringInfo = new StringInfo(vmConfiguration);
        this.stringInfo.check();
        this.header = new Header(dataPrototype, stringInfo);
        this.header.check();
        this.trailer = new Trailer(header);
    }

    public void write(OutputStream outputStream) throws IOException {
        header.write(outputStream);
        stringInfo.write(outputStream);
        outputStream.write(dataPrototype.relocationData());
        final byte[] padding = new byte[pagePaddingSize(header.size() + stringInfo.size() /*+ dataPrototype.cardOffsetTable().length*/ +  dataPrototype.relocationData().length)];
        outputStream.write(padding);
        outputStream.write(dataPrototype.heapData());
        outputStream.write(dataPrototype.codeData());
        trailer.write(outputStream);
    }

    @C_FUNCTION
    static native Address nativeGetEndOfCodeRegion();

    public static Address getEndOfCodeRegion() {
        return nativeGetEndOfCodeRegion();
    }


}
