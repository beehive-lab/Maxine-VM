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
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
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

    public abstract class Section {
        protected Section() {
        }

        protected Field[] fields() {
            return Arrays.filter(getClass().getDeclaredFields(), new Predicate<Field>() {
                public boolean evaluate(Field field) {
                    return field.getName().startsWith("_");
                }
            });
        }

        public abstract void check() throws BootImageException;
        public abstract int size();
        public abstract void write(OutputStream outputStream) throws IOException;
    }

    public abstract class IntSection extends Section {
        private final Endianness _endianness;

        public Endianness endianness() {
            return _endianness;
        }

        protected IntSection(Endianness endianness) {
            super();
            _endianness = endianness;
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
                    _endianness.writeInt(outputStream, field.getInt(this));
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected();
                }
            }
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
        final ClassMethodActor runMethodActor = ClassActor.fromJava(javaClass).findLocalClassMethodActor(run);
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
    public final class Header extends IntSection {
        public final int _isBigEndian;

        public final int _identification;
        public final int _version;
        public final int _randomID;

        public final int _wordSize;
        public final int _alignmentSize;
        public final int _relocationScheme;

        public final int _pageSize;

        public final int _vmThreadLocalsSize;
        public final int _vmThreadLocalsTrapNumberOffset;

        public final int _vmRunMethodOffset;
        public final int _vmThreadRunMethodOffset;
        public final int _runSchemeRunMethodOffset;

        public final int _classRegistryOffset;

        public final int _stringInfoSize;
        public final int _relocationDataSize;

        public final int _bootHeapSize;
        public final int _bootCodeSize;
        public final int _codeCacheSize;

        public final int _heapRegionsPointerOffset;
        public final int _codeRegionsPointerOffset;

        public final int _auxiliarySpaceSize;

        public final int _messengerInfoOffset;

        public WordWidth wordWidth() {
            return WordWidth.fromInt(_wordSize * 8);
        }

        public Alignment alignment() {
            return Alignment.fromInt(_alignmentSize);
        }

        public int relocationScheme() {
            return _relocationScheme;
        }

        private Header(DataInputStream dataInputStream) throws IOException {
            super(dataInputStream.readInt() == 0 ? Endianness.LITTLE : Endianness.BIG);
            final Endianness endian = endianness();
            _isBigEndian = endian.ordinal();

            _identification = endian.readInt(dataInputStream);
            _version = endian.readInt(dataInputStream);
            _randomID = endian.readInt(dataInputStream);

            _wordSize = endian.readInt(dataInputStream);
            _alignmentSize = endian.readInt(dataInputStream);
            _relocationScheme = endian.readInt(dataInputStream);

            _pageSize = endian.readInt(dataInputStream);

            _vmThreadLocalsSize = endian.readInt(dataInputStream);
            _vmThreadLocalsTrapNumberOffset = endian.readInt(dataInputStream);

            _vmRunMethodOffset = endian.readInt(dataInputStream);
            _vmThreadRunMethodOffset = endian.readInt(dataInputStream);
            _runSchemeRunMethodOffset = endian.readInt(dataInputStream);
            _classRegistryOffset = endian.readInt(dataInputStream);

            _stringInfoSize = endian.readInt(dataInputStream);
            _relocationDataSize = endian.readInt(dataInputStream);

            _bootHeapSize = endian.readInt(dataInputStream);
            _bootCodeSize = endian.readInt(dataInputStream);
            _codeCacheSize = endian.readInt(dataInputStream);

            _heapRegionsPointerOffset = endian.readInt(dataInputStream);
            _codeRegionsPointerOffset = endian.readInt(dataInputStream);

            _auxiliarySpaceSize = endian.readInt(dataInputStream);

            _messengerInfoOffset = endian.readInt(dataInputStream);
        }

        private int staticFieldPointerOffset(DataPrototype dataPrototype, Class javaClass, String staticFieldName) {
            final Pointer staticTupleOrigin = dataPrototype.objectToOrigin(ClassActor.fromJava(javaClass).staticTuple());
            final FieldActor fieldActor = ClassActor.fromJava(javaClass).findLocalStaticFieldActor(SymbolTable.makeSymbol(staticFieldName));
            return staticTupleOrigin.toInt() + fieldActor.offset();
        }

        private Header(DataPrototype dataPrototype, StringInfo stringInfo) {
            super(dataPrototype.vmConfiguration().platform().processorKind().dataModel().endianness());
            final VMConfiguration vmConfiguration = dataPrototype.vmConfiguration();
            _isBigEndian = endianness() == Endianness.LITTLE ? 0 : 0xffffffff;
            _identification = IDENTIFICATION;
            _version = VERSION;
            _randomID = UUID.randomUUID().hashCode();
            _wordSize = vmConfiguration.platform().processorKind().dataModel().wordWidth().numberOfBytes();
            _alignmentSize = vmConfiguration.platform().processorKind().dataModel().alignment().numberOfBytes();
            _relocationScheme = RelocationScheme.DEFAULT.ordinal();
            _pageSize = vmConfiguration.platform().pageSize();
            _vmThreadLocalsSize = VmThreadLocal.THREAD_LOCAL_STORAGE_SIZE.toInt();
            _vmThreadLocalsTrapNumberOffset = VmThreadLocal.TRAP_NUMBER.offset();
            _vmRunMethodOffset = Static.getCriticalEntryPoint(getRunMethodActor(MaxineVM.class), CallEntryPoint.C_ENTRY_POINT).toInt();
            _vmThreadRunMethodOffset = Static.getCriticalEntryPoint(getRunMethodActor(VmThread.class), CallEntryPoint.C_ENTRY_POINT).toInt();
            _runSchemeRunMethodOffset = Static.getCriticalEntryPoint(getRunMethodActor(vmConfiguration.runScheme().getClass()), CallEntryPoint.OPTIMIZED_ENTRY_POINT).toInt();
            _classRegistryOffset = dataPrototype.objectToOrigin(ClassRegistry.vmClassRegistry()).toInt();
            _stringInfoSize = stringInfo.size();
            _relocationDataSize = dataPrototype.relocationData().length;
            _bootHeapSize = dataPrototype.heapData().length;
            _bootCodeSize = dataPrototype.codeData().length;
            _codeCacheSize = CodeManager.CODE_CACHE_SIZE;

            _heapRegionsPointerOffset = staticFieldPointerOffset(dataPrototype, TeleHeapInfo.class, "_memoryRegions");
            _codeRegionsPointerOffset = staticFieldPointerOffset(dataPrototype, Code.class, "_memoryRegions");

            _auxiliarySpaceSize = vmConfiguration.heapScheme().auxiliarySpaceSize(_bootHeapSize + _bootCodeSize);

            _messengerInfoOffset = staticFieldPointerOffset(dataPrototype, MaxineMessenger.class, "_info");
        }

        @Override
        public void check() throws BootImageException {
            BootImageException.check(_identification == IDENTIFICATION, "not a MaxineVM VM boot image file, wrong identification: " + _identification);
            BootImageException.check(_version == VERSION, "wrong version: " + _version);
            BootImageException.check(_wordSize == 4 || _wordSize == 8, "illegal word size: " + _wordSize);
            BootImageException.check(_alignmentSize == 4 || _alignmentSize == 8, "illegal alignment"); // only 4 and 8 are allowed for now
            BootImageException.check(_pageSize >= Longs.K && _pageSize % Longs.K == 0, "implausible page size: " + _pageSize);
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

    private final Header _header;

    public Header header() {
        return _header;
    }

    /**
     * See "image.h".
     */
    public final class StringInfo extends Section {
        public final String _buildLevelName;
        public final String _processorModelName;
        public final String _instructionSetName;
        public final String _operatingSystemName;

        public final String _gripPackageName;
        public final String _referencePackageName;
        public final String _layoutPackageName;
        public final String _heapPackageName;
        public final String _monitorPackageName;
        public final String _compilerPackageName;
        public final String _jitPackageName;
        public final String _interpreterPackageName;
        public final String _trampolinePackageName;
        public final String _targetABIsPackageName;
        public final String _runPackageName;

        public BuildLevel buildLevel() {
            return Enums.fromString(BuildLevel.class, _buildLevelName);
        }

        public ProcessorModel processorModel() {
            return Enums.fromString(ProcessorModel.class, _processorModelName);
        }

        public InstructionSet instructionSet() {
            return Enums.fromString(InstructionSet.class, _instructionSetName);
        }

        public OperatingSystem operatingSystem() {
            return Enums.fromString(OperatingSystem.class, _operatingSystemName);
        }

        public VMPackage gripPackage() {
            return (VMPackage) MaxPackage.fromName(_gripPackageName);
        }

        public VMPackage referencePackage() {
            return (VMPackage) MaxPackage.fromName(_referencePackageName);
        }

        public VMPackage layoutPackage() {
            return (VMPackage) MaxPackage.fromName(_layoutPackageName);
        }

        public VMPackage heapPackage() {
            return (VMPackage) MaxPackage.fromName(_heapPackageName);
        }

        public VMPackage monitorPackage() {
            return (VMPackage) MaxPackage.fromName(_monitorPackageName);
        }

        public VMPackage compilerPackage() {
            return (VMPackage) MaxPackage.fromName(_compilerPackageName);
        }

        public VMPackage jitPackage() {
            if (_jitPackageName == null) {
                return null;
            }
            return (VMPackage) MaxPackage.fromName(_jitPackageName);
        }

        public VMPackage interpreterPackage() {
            if (_interpreterPackageName == null) {
                return null;
            }
            return (VMPackage) MaxPackage.fromName(_interpreterPackageName);
        }

        public VMPackage trampolinePackage() {
            return (VMPackage) MaxPackage.fromName(_trampolinePackageName);
        }

        public VMPackage targetABIsPackage() {
            return (VMPackage) MaxPackage.fromName(_targetABIsPackageName);
        }

        public VMPackage runPackage() {
            return (VMPackage) MaxPackage.fromName(_runPackageName);
        }

        private StringInfo(InputStream inputStream) throws IOException, Utf8Exception {
            super();
            _buildLevelName = Utf8.readString(inputStream);
            _processorModelName = Utf8.readString(inputStream);
            _instructionSetName = Utf8.readString(inputStream);
            _operatingSystemName = Utf8.readString(inputStream);

            _gripPackageName = Utf8.readString(inputStream);
            _referencePackageName = Utf8.readString(inputStream);
            _layoutPackageName = Utf8.readString(inputStream);
            _heapPackageName = Utf8.readString(inputStream);
            _monitorPackageName = Utf8.readString(inputStream);
            _compilerPackageName = Utf8.readString(inputStream);
            _jitPackageName =  Utf8.readString(inputStream);
            _interpreterPackageName = Utf8.readString(inputStream);
            _trampolinePackageName = Utf8.readString(inputStream);
            _targetABIsPackageName = Utf8.readString(inputStream);
            _runPackageName = Utf8.readString(inputStream);
        }

        private StringInfo(VMConfiguration vmConfiguration) {
            super();
            _buildLevelName = vmConfiguration.buildLevel().name();
            _processorModelName = vmConfiguration.platform().processorKind().processorModel().name();
            _instructionSetName = vmConfiguration.platform().processorKind().instructionSet().name();
            _operatingSystemName = vmConfiguration.platform().operatingSystem().name();

            _gripPackageName = vmConfiguration.gripPackage().name();
            _referencePackageName = vmConfiguration.referencePackage().name();
            _layoutPackageName = vmConfiguration.layoutPackage().name();
            _heapPackageName = vmConfiguration.heapPackage().name();
            _monitorPackageName = vmConfiguration.monitorPackage().name();
            _compilerPackageName = vmConfiguration.compilerPackage().name();
            // Jit Package is optional and may be null. In which case, fall back to the default compiler.
            if (vmConfiguration.jitPackage() == null) {
                _jitPackageName = _compilerPackageName;
            } else {
                _jitPackageName = vmConfiguration.jitPackage().name();
            }
            _interpreterPackageName = vmConfiguration.interpreterPackage().name();

            _trampolinePackageName = vmConfiguration.trampolinePackage().name();
            _targetABIsPackageName = vmConfiguration.targetABIsPackage().name();
            _runPackageName = vmConfiguration.runPackage().name();
        }

        private void checkPackage(String packageName) throws BootImageException {
            BootImageException.check(MaxPackage.fromName(packageName) instanceof VMPackage, "not a VM package: " + packageName);
        }

        @Override
        public void check() throws BootImageException {
            BootImageException.check(buildLevel() != null, "unknown build level: " + _buildLevelName);
            BootImageException.check(processorModel() != null, "unknown processor model: " + _processorModelName);
            BootImageException.check(instructionSet() != null, "unknown instruction set: " + _instructionSetName);
            BootImageException.check(operatingSystem() != null, "unknown operating system: " + _operatingSystemName);

            checkPackage(_gripPackageName);
            checkPackage(_referencePackageName);
            checkPackage(_layoutPackageName);
            checkPackage(_heapPackageName);
            checkPackage(_monitorPackageName);
            checkPackage(_compilerPackageName);
            checkPackage(_jitPackageName);
            checkPackage(_interpreterPackageName);
            checkPackage(_trampolinePackageName);
            checkPackage(_targetABIsPackageName);
            checkPackage(_runPackageName);
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
    }

    private final StringInfo _stringInfo;

    /**
     * See "image.h".
     */
    public final class Trailer extends IntSection {
        public final int _randomID;
        public final int _version;
        public final int _identification;

        private Trailer(Header header, InputStream inputStream) throws IOException {
            super(header.endianness());
            _randomID = endianness().readInt(inputStream);
            _version = endianness().readInt(inputStream);
            _identification = endianness().readInt(inputStream);
        }

        private Trailer(Header header) {
            super(header.endianness());
            _randomID = header._randomID;
            _version = header._version;
            _identification = header._identification;
        }

        @Override
        public void check() throws BootImageException {
            BootImageException.check(_identification == _header._identification, "inconsistent trailer identififcation");
            BootImageException.check(_version == _header._version, "inconsistent trailer version");
            BootImageException.check(_randomID == _header._randomID, "inconsistent trailer random ID");
        }
    }

    private final Trailer _trailer;

    public Trailer trailer() {
        return _trailer;
    }

    private final VMConfiguration _vmConfiguration;

    public VMConfiguration vmConfiguration() {
        return _vmConfiguration;
    }

    private static native void nativeRelocate(long heapPointer, int relocationScheme, byte[] relocationDataPointer, int relocationDataSize, int alignmentSize, int isBigEndian, int wordSize);

    private void relocate(Pointer heap, byte[] relocationData) {
        nativeRelocate(heap.toLong(), _header._relocationScheme, relocationData, relocationData.length, _header._alignmentSize, _header._isBigEndian, _header._wordSize);
    }

    private final DataPrototype _dataPrototype;

    private int pagePaddingSize(int numberOfBytesSoFar) throws IOException {
        final int pageSize = vmConfiguration().platform().pageSize();
        final int rest = numberOfBytesSoFar % pageSize;
        if (rest == 0) {
            return 0;
        }
        return pageSize - rest;
    }

    /**
     * Used when reading a boot image from a file.
     */
    public BootImage(File file) throws IOException, BootImageException {
        _dataPrototype = null;
        final FileInputStream fileInputStream = new FileInputStream(file);
        try {
            _header = new Header(new DataInputStream(fileInputStream));
            _header.check();
            _stringInfo = new StringInfo(fileInputStream);
            _stringInfo.check();
            BootImageException.check(_header._stringInfoSize == _stringInfo.size(), "inconsistent string area size");

            final DataModel dataModel = new DataModel(_header.wordWidth(), _header.endianness(), _header.alignment());
            final ProcessorKind processorKind = new ProcessorKind(_stringInfo.processorModel(), _stringInfo.instructionSet(), dataModel);
            final Platform platform = new Platform(processorKind, _stringInfo.operatingSystem(), _header._pageSize);
            _vmConfiguration = new VMConfiguration(_stringInfo.buildLevel(), platform,
                            _stringInfo.gripPackage(),
                            _stringInfo.referencePackage(),
                            _stringInfo.layoutPackage(),
                            _stringInfo.heapPackage(),
                            _stringInfo.monitorPackage(),
                            _stringInfo.compilerPackage(),
                            _stringInfo.jitPackage(),
                            _stringInfo.interpreterPackage(),
                            _stringInfo.trampolinePackage(),
                            _stringInfo.targetABIsPackage(),
                            _stringInfo.runPackage());
            _vmConfiguration.loadAndInstantiateSchemes();

            fileInputStream.skip(_header._relocationDataSize);
            final int padding = pagePaddingSize(_header.size() + _header._stringInfoSize + _header._relocationDataSize);
            fileInputStream.skip(padding + _header._bootHeapSize + _header._bootCodeSize);
            _trailer = new Trailer(_header, fileInputStream);
            _trailer.check();
        } catch (Utf8Exception utf8Exception) {
            throw new BootImageException(utf8Exception);
        } finally {
            fileInputStream.close();
        }
    }

    public Pointer map(File file, boolean relocating) throws IOException {
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

        int fileOffset = _header.size() + _header._stringInfoSize;
        randomAccessFile.seek(fileOffset);

        final byte[] relocationData = new byte[_header._relocationDataSize];
        randomAccessFile.read(relocationData);

        fileOffset += _header._relocationDataSize;
        fileOffset += pagePaddingSize(fileOffset);
        final Pointer heap = VirtualMemory.mapFile(Size.fromInt(_header._bootHeapSize + _header._bootCodeSize), randomAccessFile.getFD(), Address.fromInt(fileOffset));

        if (heap.isZero() || heap.toLong() == -1L) {
            throw new IOException("could not mmap boot heap and code");
        }
        if (relocating) {
            Trace.line(1, "BEGIN: relocating heap");
            relocate(heap, relocationData);
            Trace.line(1, "END: relocating heap");
        }
        return heap;
    }

    /**
     * Used when constructing a boot image to be written to a file.
     */
    public BootImage(DataPrototype dataPrototype) throws BootImageException {
        _dataPrototype = dataPrototype;
        _vmConfiguration = dataPrototype.vmConfiguration();
        _stringInfo = new StringInfo(_vmConfiguration);
        _stringInfo.check();
        _header = new Header(dataPrototype, _stringInfo);
        _header.check();
        _trailer = new Trailer(_header);
    }

    public void write(OutputStream outputStream) throws IOException {
        _header.write(outputStream);
        _stringInfo.write(outputStream);
        outputStream.write(_dataPrototype.relocationData());
        final byte[] padding = new byte[pagePaddingSize(_header.size() + _stringInfo.size() /*+ _dataPrototype.cardOffsetTable().length*/ +  _dataPrototype.relocationData().length)];
        outputStream.write(padding);
        outputStream.write(_dataPrototype.heapData());
        outputStream.write(_dataPrototype.codeData());
        _trailer.write(outputStream);
    }
}
