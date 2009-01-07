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
package com.sun.max.vm.classfile;

import static com.sun.max.annotate.SURROGATE.Static.*;
import static com.sun.max.vm.actor.Actor.*;
import static com.sun.max.vm.actor.holder.ClassActorFactory.*;
import static com.sun.max.vm.actor.member.MethodActor.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.type.ClassRegistry.Property.*;
import static com.sun.max.vm.type.JavaTypeDescriptor.*;

import java.security.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Reads a class file to create a corresponding {@link ClassActor}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author David Liu
 */
public class ClassfileReader {

    public static final char JAVA_MIN_SUPPORTED_VERSION = 45;
    public static final char JAVA_1_5_VERSION = 49;
    public static final char JAVA_6_VERSION = 50;
    public static final char JAVA_MAX_SUPPORTED_VERSION = 50;
    public static final char JAVA_MAX_SUPPORTED_MINOR_VERSION = 0;

    protected final ClassfileStream _classfileStream;
    protected final ClassLoader _classLoader;
    protected final ClassRegistry _classRegistry;
    protected ConstantPool _constantPool;
    protected int _majorVersion;
    protected TypeDescriptor _outerClass;
    protected TypeDescriptor[] _innerClasses;
    protected TypeDescriptor _classDescriptor;
    protected int _flags;

    public ClassfileReader(ClassfileStream classfileStream, ClassLoader classLoader) {
        _classfileStream = classfileStream;
        _classLoader = classLoader;
        _classRegistry = ClassRegistry.makeRegistry(classLoader);
    }

    /**
     * A utility class for efficiently determining that a sequence of fields or methods
     * are unique with respect to their names and signatures.
     */
    static class MemberSet extends ChainedHashMapping<MemberActor, MemberActor> {
        public MemberSet(int initialCapacity) {
            super(initialCapacity);
        }
        @Override
        public boolean equivalent(MemberActor memberActor1, MemberActor memberActor2) {
            return memberActor1.matchesNameAndType(memberActor2.name(), memberActor2.descriptor());
        }
        @Override
        public int hashCode(MemberActor memberActor) {
            return memberActor.name().hashCode() ^ memberActor.descriptor().hashCode();
        }

        /**
         * Adds a given member to this set.
         *
         * @param memberActor the member to add
         * @return the member actor currently in this set whose name and signature match {@code memberActor}. This will
         *         be null if there is no matching member currently in this set.
         */
        public MemberActor add(MemberActor memberActor) {
            return super.put(memberActor, memberActor);
        }
    }

    /**
     * Parses a Java identifier at a given offset of a string.
     *
     * @param string the string to test
     * @param offset the offset at which a legal field or type identifier should occur
     * @return the first character after the legal identifier or -1 if there is no legal identifier at {@code offset}
     */
    public static int parseIdentifier(String string, int offset) {
        boolean isFirstChar = true;
        char ch;
        int i;
        for (i = offset; i != string.length(); i++, isFirstChar = false) {
            ch = string.charAt(i);
            if (ch < 128) {
                // Quick check for ASCII
                if ((ch >= 'a' && ch <= 'z') ||
                    (ch >= 'A' && ch <= 'Z') ||
                    (ch == '_' || ch == '$') ||
                    (!isFirstChar && ch >= '0' && ch <= '9')) {
                    continue;
                }

                break;
            }
            if (Character.isJavaIdentifierStart(ch)) {
                continue;
            }
            if (!isFirstChar && Character.isJavaIdentifierPart(ch)) {
                continue;
            }
            break;
        }
        return isFirstChar ? -1 : i;
    }

    public static void verifyFieldName(Utf8Constant name) {
        if (!isValidFieldName(name)) {
            throw classFormatError("Invalid field name: " + name);
        }
    }

    public static void verifyMethodName(Utf8Constant name, boolean allowClinit) {
        if (!isValidMethodName(name, allowClinit)) {
            throw classFormatError("Invalid method name: " + name);
        }
    }

    public static boolean isValidFieldName(Utf8Constant name) {
        return parseIdentifier(name.string(), 0) == name.string().length();
    }

    public static boolean isValidMethodName(Utf8Constant name, boolean allowClinit) {
        if (name.equals(SymbolTable.INIT)) {
            return true;
        }
        if (allowClinit && name.equals(SymbolTable.CLINIT)) {
            return true;
        }
        return parseIdentifier(name.string(), 0) == name.string().length();
    }

    /**
     * Verifies that the class file version is supported.
     *
     * @param major
     * @param minor
     */
    public static void verifyVersion(int major, int minor) {
        if ((major >= JAVA_MIN_SUPPORTED_VERSION) &&
            (major <= JAVA_MAX_SUPPORTED_VERSION) &&
            ((major != JAVA_MAX_SUPPORTED_VERSION) ||
             (minor <= JAVA_MAX_SUPPORTED_MINOR_VERSION))) {
            return;
        }
        throw classFormatError("Unsupported major.minor version " + major + "." + minor);
    }

    /**
     * Verifies that the flags for a class are valid.
     *
     * @param flags the flags to test
     * @return the flags the VM should use
     * @throws ClassFormatError if the flags are invalid
     */
    public static int verifyClassFlags(int flags, int major) {
        boolean valid;
        final int finalAndAbstract = ACC_FINAL | ACC_ABSTRACT;
        if ((flags & ACC_INTERFACE) != 0) {
            final int newFlags;
            if (major < JAVA_6_VERSION) {
                // Set abstract bit for old class files for backward compatibility
                newFlags = flags | ACC_ABSTRACT;
            } else {
                newFlags = flags;
            }

            // If the ACC_INTERFACE flag of this class file is set, its ACC_ABSTRACT flag must also be set (2.13.1) and
            // its ACC_PUBLIC flag may be set. Such a class file may not have any of the other flags in Table 4.1 set.
            valid = (newFlags & (finalAndAbstract | ACC_SUPER)) == ACC_ABSTRACT;
        } else {
            // If the ACC_INTERFACE flag of this class file is not set, it may have any of the other flags in Table 4.1
            // set. However, such a class file cannot have both its ACC_FINAL and ACC_ABSTRACT flags set (2.8.2).
            valid = (flags & finalAndAbstract) != finalAndAbstract;
        }
        if (valid) {
            return flags;
        }
        throw classFormatError("Invalid class flags 0x" + Integer.toHexString(flags));
    }

    /**
     * Verifies that the flags for a field are valid.
     *
     * @param flags the flags to test
     * @param classModifiers the flags of the enclosing class
     * @throws ClassFormatError if the flags are invalid
     */
    public static void verifyFieldFlags(String name, int flags, boolean isInterface) {
        boolean valid;
        if (!isInterface) {
            // Class or instance fields
            final int maskedFlags = flags & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);

            // Make sure that flags has at most one of its ACC_PRIVATE,
            // ACC_PROTECTED bits set. That is, do a population count of these
            // bit positions corresponding to these flags and ensure that it is
            // at most 1.
            valid = maskedFlags == 0 || (maskedFlags & ~(maskedFlags - 1)) == maskedFlags;

            // A field can't be both final and volatile
            final int finalAndVolatile = ACC_FINAL | ACC_VOLATILE;
            valid = valid && ((flags & finalAndVolatile) != finalAndVolatile);
        } else {
            // interface fields must be public static final (i.e. constants), but may have ACC_SYNTHETIC set
            valid = (flags & ~ACC_SYNTHETIC) == (ACC_STATIC | ACC_FINAL | ACC_PUBLIC);
        }
        if (!valid) {
            throw classFormatError(name + "Invalid field flags 0x" + Integer.toHexString(flags));
        }
    }

    /**
     * Verifies that the flags for a method are valid.
     *
     * @param flags the flags to test
     * @param isInit true if the method is "<init>"
     * @param isClinit true if the method is "<clinit>"
     * @throws ClassFormatError if the flags are invalid
     */
    public static void verifyMethodFlags(String name, int flags, boolean isInterface, boolean isInit, boolean isClinit, int majorVersion) {

        // Class and interface initialization methods (3.9) are called
        // implicitly by the Java virtual machine; the value of their
        // access_flags item is ignored except for the settings of the
        // ACC_STRICT flag.
        if (isClinit) {
            return;
        }

        // These are all small bits.  The value is between 0 and 7.
        final int maskedFlags = flags & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);

        // Make sure that flags has at most one of its ACC_PRIVATE,
        // ACC_PROTECTED bits set. That is, do a population count of these
        // bit positions corresponding to these flags and ensure that it is
        // at most 1.
        boolean valid = maskedFlags == 0 || (maskedFlags & ~(maskedFlags - 1)) == maskedFlags;

        if (valid) {
            if (!isInterface) {
                // class or instance methods
                if ((flags & ACC_ABSTRACT) != 0) {
                    if ((flags & (ACC_FINAL | ACC_NATIVE | ACC_PRIVATE | ACC_STATIC)) != 0) {
                        valid = false;
                    }
                    if (majorVersion >= JAVA_1_5_VERSION) {
                        if ((flags & (ACC_SYNCHRONIZED | ACC_STRICT)) != 0) {
                            valid = false;
                        }
                    }
                }
            } else {
                final int publicAndAbstract = ACC_ABSTRACT | ACC_PUBLIC;
                if ((flags & publicAndAbstract) != publicAndAbstract) {
                    valid = false;
                } else {
                    if ((flags & (ACC_STATIC | ACC_FINAL | ACC_NATIVE)) != 0) {
                        valid = false;
                    } else if (majorVersion >= JAVA_1_5_VERSION) {
                        if ((flags & (ACC_SYNCHRONIZED | ACC_STRICT)) != 0) {
                            valid = false;
                        }
                    }
                }
            }

            if (valid) {
                if (isInit) {
                    /*
                     * A specific instance initialization method (3.9) may have at most one of its ACC_PRIVATE,
                     * ACC_PROTECTED, and ACC_PUBLIC flags set and may also have its ACC_STRICT ACC_VARARGS, and
                     * ACC_SYNTHETIC flags set, but may not have any of the other flags in Table 4.5 set.
                     */
                    valid = (flags & ~(ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_STRICT | ACC_SYNTHETIC | ACC_VARARGS)) == 0;
                }
            }
        }
        if (!valid) {
            throw classFormatError("Invalid method flags 0x" + Integer.toHexString(flags));
        }
    }

    protected void readMagic() {
        final int magic = _classfileStream.readInt();
        if (magic != 0xcafebabe) {
            throw classFormatError("Invalid magic number 0x" + Integer.toHexString(magic));
        }
    }

    protected InterfaceActor resolveInterface(int index) {
        final ClassConstant classConstant = _constantPool.classAt(index, "interface name");
        try {
            return (InterfaceActor) classConstant.resolve(_constantPool, index);
        } catch (ClassCastException classCastException) {
            throw incompatibleClassChangeError(classConstant.typeDescriptor().toJavaString() + " is not an interface");
        }
    }

    protected InterfaceActor[] readInterfaces() {
        final int nInterfaces = _classfileStream.readUnsigned2();
        if (nInterfaces == 0) {
            return ClassActor.NO_INTERFACES;
        }
        final InterfaceActor[] interfaceActors = new InterfaceActor[nInterfaces];
        for (int i = 0; i < nInterfaces; i++) {
            interfaceActors[i] = resolveInterface(_classfileStream.readUnsigned2());
        }
        return interfaceActors;
    }

    protected FieldActor[] readFields(boolean isInterface)  {
        final int nFields = _classfileStream.readUnsigned2();
        if (nFields == 0) {
            // save time and space for classes that have no fields
            return ClassActor.NO_FIELDS;
        }
        final FieldActor[] fieldActors = new FieldActor[nFields];
        int nextFieldIndex = 0;

        final MemberSet fieldActorSet = new MemberSet(nFields);

    nextField:
        for (int i = 0; i < nFields; i++) {
            int flags = _classfileStream.readUnsigned2();
            final int nameIndex = _classfileStream.readUnsigned2();
            final Utf8Constant name = _constantPool.utf8ConstantAt(nameIndex, "field name");
            verifyFieldName(name);

            final boolean isStatic = isStatic(flags);

            try {
                enterContext(new Object() {
                    @Override
                    public String toString() {
                        return "parsing field \"" + name + "\"";
                    }
                });
                final int descriptorIndex = _classfileStream.readUnsigned2();
                final TypeDescriptor descriptor = parseTypeDescriptor(_constantPool.utf8At(descriptorIndex, "field descriptor").toString());
                verifyFieldFlags(name.toString(), flags, isInterface);

                char constantValueIndex = 0;
                byte[] runtimeVisibleAnnotationsBytes = NO_RUNTIME_VISIBLE_ANNOTATION_BYTES;
                Utf8Constant genericSignature = NO_GENERIC_SIGNATURE;

                int nAttributes = _classfileStream.readUnsigned2();
                while (nAttributes-- != 0) {
                    final int attributeNameIndex = _classfileStream.readUnsigned2();
                    final String attributeName = _constantPool.utf8At(attributeNameIndex, "attribute name").toString();
                    final Size attributeSize = _classfileStream.readSize4();
                    final Address startPosition = _classfileStream.getPosition();
                    if (isStatic && attributeName.equals("ConstantValue")) {
                        if (constantValueIndex != 0) {
                            throw classFormatError("Duplicate ConstantValue attribute");
                        }
                        constantValueIndex = (char) _classfileStream.readUnsigned2();
                        if (constantValueIndex == 0) {
                            throw classFormatError("Invalid ConstantValue index");
                        }
                    } else if (attributeName.equals("Deprecated")) {
                        flags += Actor.DEPRECATED;
                    } else if (attributeName.equals("Synthetic")) {
                        flags += Actor.ACC_SYNTHETIC;
                    } else if (_majorVersion >= JAVA_1_5_VERSION) {
                        if (attributeName.equals("Signature")) {
                            genericSignature = _constantPool.utf8At(_classfileStream.readUnsigned2(), "signature index");
                        } else if (attributeName.equals("RuntimeVisibleAnnotations")) {
                            runtimeVisibleAnnotationsBytes = _classfileStream.readByteArray(attributeSize);
                        } else {
                            _classfileStream.skip(attributeSize);
                        }
                    } else {
                        _classfileStream.skip(attributeSize);
                    }

                    if (!attributeSize.equals(_classfileStream.getPosition().minus(startPosition))) {
                        throw classFormatError("Invalid attribute_length for " + attributeName + " attribute");
                    }
                }

                if (MaxineVM.isPrototyping()) {
                    if (runtimeVisibleAnnotationsBytes != null) {
                        final ClassfileStream annotations = new ClassfileStream(runtimeVisibleAnnotationsBytes);
                        for (AnnotationInfo info : AnnotationInfo.parse(annotations, _constantPool)) {
                            final TypeDescriptor annotationTypeDescriptor = info.annotationTypeDescriptor();
                            if (annotationTypeDescriptor.equals(forJavaClass(PROTOTYPE_ONLY.class))) {
                                continue nextField;
                            } else if (info.annotationTypeDescriptor().equals(forJavaClass(CONSTANT.class))) {
                                flags |= CONSTANT;
                            } else if (info.annotationTypeDescriptor().equals(forJavaClass(CONSTANT_WHEN_NOT_ZERO.class))) {
                                flags |= CONSTANT_WHEN_NOT_ZERO;
                            }
                        }
                    }
                }

                final Kind kind = descriptor.toKind();
                final FieldActor<?> fieldActor = kind.createFieldActor(name, descriptor, flags);

                if (constantValueIndex != 0) {
                    switch (fieldActor.kind().asEnum()) {
                        case BYTE: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, ByteValue.from((byte) _constantPool.intAt(constantValueIndex)));
                            break;
                        }
                        case BOOLEAN: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, BooleanValue.from(_constantPool.intAt(constantValueIndex) != 0));
                            break;
                        }
                        case CHAR: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, CharValue.from((char) _constantPool.intAt(constantValueIndex)));
                            break;
                        }
                        case SHORT: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, ShortValue.from((short) _constantPool.intAt(constantValueIndex)));
                            break;
                        }
                        case INT: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, IntValue.from(_constantPool.intAt(constantValueIndex)));
                            break;
                        }
                        case FLOAT: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, FloatValue.from(_constantPool.floatAt(constantValueIndex)));
                            break;
                        }
                        case LONG: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, LongValue.from(_constantPool.longAt(constantValueIndex)));
                            break;
                        }
                        case DOUBLE: {
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, DoubleValue.from(_constantPool.doubleAt(constantValueIndex)));
                            break;
                        }
                        case REFERENCE: {
                            if (!descriptor.equals(STRING)) {
                                throw classFormatError("Invalid ConstantValue attribute");
                            }
                            _classRegistry.set(CONSTANT_VALUE, fieldActor, ReferenceValue.from(_constantPool.stringAt(constantValueIndex)));
                            break;
                        }
                        default: {
                            throw classFormatError("Cannot have ConstantValue for fields of type " + kind);
                        }
                    }
                }

                _classRegistry.set(GENERIC_SIGNATURE, fieldActor, genericSignature);
                _classRegistry.set(RUNTIME_VISIBLE_ANNOTATION_BYTES, fieldActor, runtimeVisibleAnnotationsBytes);

                // Check for duplicates
                if (fieldActorSet.add(fieldActor) != null) {
                    throw classFormatError("Duplicate field name and signature: " + name + " " + descriptor);
                }

                fieldActors[nextFieldIndex++] = fieldActor;
            } finally {
                exitContext();
            }
        }

        if (nextFieldIndex < nFields) {
            return Arrays.copyOf(fieldActors, nextFieldIndex);
        }
        return fieldActors;
    }

    protected TypeDescriptor[] readCheckedExceptionsAttribute() {
        final int nExceptionClasses = _classfileStream.readUnsigned2();
        final TypeDescriptor[] checkedExceptions = new TypeDescriptor[nExceptionClasses];
        for (int i = 0; i < nExceptionClasses; i++) {
            final int checkedExceptionIndex = _classfileStream.readUnsigned2();
            checkedExceptions[i] = _constantPool.classAt(checkedExceptionIndex).typeDescriptor();
        }
        return checkedExceptions;
    }

    protected ExceptionHandlerEntry readExceptionHandlerEntry(int codeLength) {
        final int startAddress = _classfileStream.readUnsigned2();
        final int endAddress = _classfileStream.readUnsigned2();
        final int handlerAddress = _classfileStream.readUnsigned2();
        final int catchClassIndex = _classfileStream.readUnsigned2();

        if (startAddress >= codeLength || endAddress > codeLength || startAddress >= endAddress || handlerAddress >= codeLength) {
            throw classFormatError("Invalid exception handler code range");
        }

        return new ExceptionHandlerEntry(startAddress, endAddress, handlerAddress, catchClassIndex);
    }

    protected Sequence<ExceptionHandlerEntry> readExceptionHandlerTable(int codeLength) {
        final int nEntries = _classfileStream.readUnsigned2();
        if (nEntries != 0) {
            final ExceptionHandlerEntry[] entries = new ExceptionHandlerEntry[nEntries];
            for (int i = 0; i < nEntries; i++) {
                entries[i] = readExceptionHandlerEntry(codeLength);
            }
            return new ArraySequence<ExceptionHandlerEntry>(entries);
        }
        return Sequence.Static.empty(ExceptionHandlerEntry.class);
    }

    // CheckStyle: stop parameter assignment check
    protected Map<LocalVariableTable.Entry, LocalVariableTable.Entry> readLocalVariableTable(int maxLocals, Size codeLength, Map<LocalVariableTable.Entry, LocalVariableTable.Entry> localVariableTableEntries, boolean forLVTT) {
        final int count = _classfileStream.readUnsigned2();
        if (count == 0) {
            return localVariableTableEntries;
        }
        if (localVariableTableEntries == null) {
            localVariableTableEntries = new HashMap<LocalVariableTable.Entry, LocalVariableTable.Entry>(count);
        }
        for (int i = 0; i != count; ++i) {
            final LocalVariableTable.Entry entry = new LocalVariableTable.Entry(_classfileStream, forLVTT);
            entry.verify(_constantPool, codeLength.toInt(), maxLocals, forLVTT);
            if (localVariableTableEntries.put(entry, entry) != null) {
                throw classFormatError("Duplicated " + (forLVTT ? "LocalVariableTypeTable" : "LocalVariableTable") + " entry at index " + i);
            }
        }
        return localVariableTableEntries;
    }
    // CheckStyle: resume parameter assignment check

    protected CodeAttribute readCodeAttribute(int methodAccessFlags) {
        final char maxStack = (char) _classfileStream.readUnsigned2();
        final char maxLocals = (char) _classfileStream.readUnsigned2();
        final Size codeLength = _classfileStream.readSize4();
        if (codeLength.lessEqual(0)) {
            throw classFormatError("The value of code_length must be greater than 0");
        } else if (codeLength.greaterEqual(0xFFFF)) {
            throw classFormatError("Method code longer than 64 KB");
        }

        final byte[] code = _classfileStream.readByteArray(codeLength);
        final Sequence<ExceptionHandlerEntry> exceptionHandlerTable = readExceptionHandlerTable(code.length);

        LineNumberTable lineNumberTable = LineNumberTable.EMPTY;
        LocalVariableTable localVariableTable = LocalVariableTable.EMPTY;
        Map<LocalVariableTable.Entry, LocalVariableTable.Entry> localVariableTableEntries = null;
        Map<LocalVariableTable.Entry, LocalVariableTable.Entry> localVariableTypeTableEntries = null;
        StackMapTable stackMapTable = null;

        int nAttributes = _classfileStream.readUnsigned2();
        while (nAttributes-- != 0) {
            final int attributeNameIndex = _classfileStream.readUnsigned2();
            final String attributeName = _constantPool.utf8At(attributeNameIndex, "attribute name").toString();
            final Size attributeSize = _classfileStream.readSize4();
            final Address startPosition = _classfileStream.getPosition();
            if (attributeName.equals("LineNumberTable")) {
                lineNumberTable = new LineNumberTable(lineNumberTable, _classfileStream, codeLength.toInt());
            } else if (attributeName.equals("StackMapTable")) {
                if (stackMapTable != null) {
                    throw classFormatError("Duplicate stack map attribute");
                }
                stackMapTable = new StackMapTable(_classfileStream, _constantPool, attributeSize);
            } else if (attributeName.equals("LocalVariableTable")) {
                localVariableTableEntries = readLocalVariableTable(maxLocals, codeLength, localVariableTableEntries, false);
            } else if (_majorVersion >= JAVA_1_5_VERSION) {
                if (attributeName.equals("LocalVariableTypeTable")) {
                    localVariableTypeTableEntries = readLocalVariableTable(maxLocals, codeLength, localVariableTypeTableEntries, true);
                } else {
                    _classfileStream.skip(attributeSize);
                }
            } else {
                _classfileStream.skip(attributeSize);
            }

            if (!attributeSize.equals(_classfileStream.getPosition().minus(startPosition))) {
                throw classFormatError("Invalid attribute length for " + attributeName + " attribute");
            }
        }

        if (localVariableTypeTableEntries != null) {
            if (localVariableTableEntries == null) {
                throw classFormatError("LocalVariableTypeTable attribute present without LocalVariableTable");
            }
            for (LocalVariableTable.Entry lvttEntry : localVariableTypeTableEntries.values()) {
                final LocalVariableTable.Entry lvtEntry = localVariableTableEntries.get(lvttEntry);
                if (lvtEntry == null) {
                    throw classFormatError("LocalVariableTypeTable entry does not match any LocalVariableTable entry");
                }
                lvtEntry.copySignatureIndex(lvttEntry);
            }
        }

        if (localVariableTableEntries != null) {
            localVariableTable = new LocalVariableTable(localVariableTableEntries.values());
        }

        return new CodeAttribute(
                        _constantPool,
                        code,
                        maxStack,
                        maxLocals,
                        exceptionHandlerTable,
                        lineNumberTable,
                        localVariableTable,
                        stackMapTable);
    }

    /**
     * Checks that a given method signature does not contain a {@linkplain Kind#REFERENCE reference} type.
     *
     * @param annotationClass the annotation applied to the method that enforces this constraint
     */
    protected void ensureSignatureIsPrimitive(SignatureDescriptor descriptor, Class annotationClass) {
        // Verify that there are no REFERENCE parameters
        for (Kind parameterKind : descriptor.getParameterKinds()) {
            ProgramError.check(parameterKind != Kind.REFERENCE, annotationClass.getSimpleName() + " annotated methods cannot have reference parameters: " + this);
        }
        ProgramError.check(descriptor.getResultKind() != Kind.REFERENCE, annotationClass.getSimpleName() + " annotated methods cannot have reference return type: " + this);
    }

    protected MethodActor[] readMethods(boolean isInterface) {
        final int numberOfMethods = _classfileStream.readUnsigned2();
        if (numberOfMethods == 0) {
            return ClassActor.NO_METHODS;
        }
        final MethodActor[] methodActors = new MethodActor[numberOfMethods];
        int nextMethodIndex = 0;

        // A map for efficiently checking the uniqueness of methods
        final MemberSet methodActorSet = new MemberSet(numberOfMethods);

    nextMethod:
        for (int i = 0; i < numberOfMethods; ++i) {
            int flags = _classfileStream.readUnsigned2();
            final int nameIndex = _classfileStream.readUnsigned2();
            Utf8Constant name = _constantPool.utf8ConstantAt(nameIndex, "method name");
            verifyMethodName(name, true);

            int extraFlags = flags;
            if (name.equals(SymbolTable.CLINIT)) {
                // Class and interface initialization methods (3.9) are called
                // implicitly by the Java virtual machine; the value of their
                // access_flags item is ignored except for the settings of the
                // ACC_STRICT flag.
                flags &= ACC_STRICT;
                flags |= ACC_STATIC;
                extraFlags = CLASS_INITIALIZER | flags;
            } else if (name.equals(SymbolTable.INIT)) {
                extraFlags |=  INSTANCE_INITIALIZER;
            }

            final boolean isClinit = isClassInitializer(extraFlags);
            final boolean isInit = isInstanceInitializer(extraFlags);
            final boolean isStatic = isStatic(extraFlags);

            try {
                enterContext(new Object() {
                    @Override
                    public String toString() {
                        return "parsing method \"" + _constantPool.utf8ConstantAt(nameIndex, "method name") + "\"";
                    }
                });

                verifyMethodFlags(name.toString(), flags, isInterface, isInit, isClinit, _majorVersion);
                flags = extraFlags;

                final int descriptorIndex = _classfileStream.readUnsigned2();
                final SignatureDescriptor descriptor = SignatureDescriptor.create(_constantPool.utf8At(descriptorIndex, "method descriptor"));

                if (descriptor.getNumberOfLocals() + (isStatic ? 0 : 1) > 255) {
                    throw classFormatError("Too many arguments in method signature: " + descriptor);
                }

                if (name.equals(SymbolTable.FINALIZE) && descriptor.equals(SignatureDescriptor.VOID) && (flags & ACC_STATIC) == 0) {
                    // this class has a finalizer method implementation
                    // (this bit will be cleared for java.lang.Object later)
                    _flags |= FINALIZER;
                }

                CodeAttribute codeAttribute = null;
                TypeDescriptor[] checkedExceptions = NO_CHECKED_EXCEPTIONS;
                byte[] runtimeVisibleAnnotationsBytes = NO_RUNTIME_VISIBLE_ANNOTATION_BYTES;
                byte[] runtimeVisibleParameterAnnotationsBytes = NO_RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES;
                byte[] annotationDefaultBytes = NO_ANNOTATION_DEFAULT_BYTES;
                Utf8Constant genericSignature = NO_GENERIC_SIGNATURE;

                int nAttributes = _classfileStream.readUnsigned2();
                while (nAttributes-- != 0) {
                    final int attributeNameIndex = _classfileStream.readUnsigned2();
                    final String attributeName = _constantPool.utf8At(attributeNameIndex, "attribute name").toString();
                    final Size attributeSize = _classfileStream.readSize4();
                    final Address startPosition = _classfileStream.getPosition();
                    if (attributeName.equals("Code")) {
                        if (codeAttribute != null) {
                            throw classFormatError("Duplicate Code attribute");
                        }
                        codeAttribute = readCodeAttribute(flags);
                    } else if (attributeName.equals("Exceptions")) {
                        if (checkedExceptions != NO_CHECKED_EXCEPTIONS) {
                            throw classFormatError("Duplicate Exceptions attribute");
                        }
                        checkedExceptions = readCheckedExceptionsAttribute();
                    } else if (attributeName.equals("Deprecated")) {
                        flags |= Actor.DEPRECATED;
                    } else if (attributeName.equals("Synthetic")) {
                        flags |= Actor.ACC_SYNTHETIC;
                    } else if (_majorVersion >= JAVA_1_5_VERSION) {
                        if (attributeName.equals("Signature")) {
                            genericSignature = _constantPool.utf8At(_classfileStream.readUnsigned2(), "signature index");
                        } else if (attributeName.equals("RuntimeVisibleAnnotations")) {
                            runtimeVisibleAnnotationsBytes = _classfileStream.readByteArray(attributeSize);
                        } else if (attributeName.equals("RuntimeVisibleParameterAnnotations")) {
                            runtimeVisibleParameterAnnotationsBytes = _classfileStream.readByteArray(attributeSize);
                        } else if (attributeName.equals("AnnotationDefault")) {
                            annotationDefaultBytes = _classfileStream.readByteArray(attributeSize);
                        } else {
                            _classfileStream.skip(attributeSize);
                        }
                    } else {
                        _classfileStream.skip(attributeSize);
                    }

                    final Address distance = _classfileStream.getPosition().minus(startPosition);
                    if (!attributeSize.equals(distance)) {
                        final int size = attributeSize.toInt();
                        final int dist = distance.toInt();
                        final String message = "Invalid attribute_length for " + attributeName + " attribute (reported " + size + " != parsed " + dist + ")";
                        throw classFormatError(message);
                    }
                }

                if (isAbstract(flags) || isNative(flags)) {
                    if (codeAttribute != null) {
                        throw classFormatError("Code attribute supplied for native or abstract method");
                    }
                } else {
                    if (codeAttribute == null) {
                        throw classFormatError("Missing Code attribute");
                    }
                }

                TRAMPOLINE.Invocation trampolineInvocation = null;
                int substituteeIndex = -1;
                if (MaxineVM.isPrototyping()) {
                    if (isClinit) {
                        // Class initializer's for all Maxine class are run while prototyping and do no need to be in the boot image
                        if (com.sun.max.Package.contains(_classDescriptor.toJavaString())) {
                            continue nextMethod;
                        }
                    }

                    if (runtimeVisibleAnnotationsBytes != null) {
                        final ClassfileStream annotations = new ClassfileStream(runtimeVisibleAnnotationsBytes);
                        for (AnnotationInfo info : AnnotationInfo.parse(annotations, _constantPool)) {
                            final TypeDescriptor annotationTypeDescriptor = info.annotationTypeDescriptor();
                            if (annotationTypeDescriptor.equals(forJavaClass(PROTOTYPE_ONLY.class))) {
                                continue nextMethod;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(C_FUNCTION.class))) {
                                ensureSignatureIsPrimitive(descriptor, C_FUNCTION.class);
                                flags |= C_FUNCTION;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(NO_SAFEPOINTS.class))) {
                                flags |= NO_SAFEPOINTS;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(BUILTIN.class))) {
                                flags |= BUILTIN | UNSAFE;
                                codeAttribute = null;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(WRAPPER.class))) {
                                flags |= WRAPPER;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(BYTECODE_TEMPLATE.class))) {
                                flags |= TEMPLATE;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(INLINE.class))) {
                                flags |= INLINE;
                                for (AnnotationInfo.NameElementPair nameElementPair : info.nameElementPairs()) {
                                    if (nameElementPair.name().equals("afterSnippetsAreCompiled")) {
                                        final AnnotationInfo.ValueElement valueElement = (AnnotationInfo.ValueElement) nameElementPair.element();
                                        if (valueElement.value().toBoolean()) {
                                            flags |= INLINE_AFTER_SNIPPETS_ARE_COMPILED;
                                        }
                                    }
                                }
                            } else if (annotationTypeDescriptor.equals(forJavaClass(NEVER_INLINE.class))) {
                                flags |= NEVER_INLINE;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(TRAMPOLINE.class))) {
                                for (AnnotationInfo.NameElementPair nameElementPair : info.nameElementPairs()) {
                                    if (nameElementPair.name().equals("invocation")) {
                                        final AnnotationInfo.EnumElement enumElement = (AnnotationInfo.EnumElement) nameElementPair.element();
                                        trampolineInvocation = Enum.valueOf(TRAMPOLINE.Invocation.class, enumElement.enumConstantName());
                                    } else {
                                        ProgramError.unexpected();
                                    }
                                }
                            } else if (annotationTypeDescriptor.equals(forJavaClass(JNI_FUNCTION.class))) {
                                ensureSignatureIsPrimitive(descriptor, JNI_FUNCTION.class);
                                ProgramError.check(!isSynchronized(flags), "Cannot apply " + JNI_FUNCTION.class.getName() + " to a synchronized method");
                                ProgramError.check(!isNative(flags), "Cannot apply " + JNI_FUNCTION.class.getName() + " to native method");
                                ProgramError.check(isStatic, "Cannot apply " + JNI_FUNCTION.class.getName() + " to non-static method");
                                flags |= JNI_FUNCTION;
                                flags |= C_FUNCTION;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(FOLD.class))) {
                                flags |= FOLD;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(UNSAFE.class))) {
                                flags |= UNSAFE;
                            } else if (annotationTypeDescriptor.equals(forJavaClass(SURROGATE.class))) {
                                flags |= SURROGATE;
                                final Utf8Constant substituteeName = SymbolTable.lookupSymbol(toSubstituteeName(name.toString()));
                                if (substituteeName != null) {
                                    for (int j = nextMethodIndex - 1; j >= 0; --j) {
                                        final MethodActor substituteeActor = methodActors[j];
                                        if (substituteeActor.name().equals(substituteeName) && substituteeActor.descriptor().equals(descriptor)) {
                                            ProgramError.check(isStatic == substituteeActor.isStatic());
                                            Trace.line(1, "Substituted " + _classDescriptor.toJavaString() + "." + substituteeName + descriptor);
                                            Trace.line(1, "       with " + _classDescriptor.toJavaString() + "." + name + descriptor);
                                            substituteeIndex = j;
                                            name = substituteeName;

                                            // Copy the access level of the substitutee to the surrogate
                                            final int accessFlagsMask = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;
                                            flags &= ~accessFlagsMask;
                                            flags |= substituteeActor.flags() & accessFlagsMask;
                                            substituteeActor.beUnsafe();
                                            break;
                                        }
                                    }
                                }
                                ProgramError.check(substituteeIndex != -1, "Could not find substitutee for surrogate method: " + name + " " + descriptor);
                            }
                        }
                    }

                    if (isStatic) {
                        // The following helps folding down enum switch code for known enums, which should not incur binary compatibility issues:
                        final String javacPrefix = "$TODO"; // TODO
                        final String jdtPrefix = "$SWITCH_TABLE";
                        if (name.toString().startsWith(javacPrefix) || name.toString().startsWith(jdtPrefix)) {
                            flags |= FOLD;
                        }
                    }

                } // Maxine.isPrototyping()

                if (isNative(flags)) {
                    flags |= UNSAFE;
                }
                if (isNative(flags)) {
                    flags |= UNSAFE;
                }

                final MethodActor methodActor;
                if (isInterface) {
                    if (isClinit) {
                        methodActor = new StaticMethodActor(name, descriptor, flags, codeAttribute);
                    } else if (isInit) {
                        throw classFormatError("Interface cannot have a constructor");
                    } else {
                        methodActor = new InterfaceMethodActor(name, descriptor, flags);
                    }
                } else if (isStatic) {
                    if (trampolineInvocation != null) {
                        methodActor = new TrampolineMethodActor(name, descriptor, flags, codeAttribute, trampolineInvocation);
                    } else {
                        methodActor = new StaticMethodActor(name, descriptor, flags, codeAttribute);
                    }
                } else {
                    methodActor = new VirtualMethodActor(name, descriptor, flags, codeAttribute);
                }

                _classRegistry.set(GENERIC_SIGNATURE, methodActor, genericSignature);
                _classRegistry.set(CHECKED_EXCEPTIONS, methodActor, checkedExceptions);
                _classRegistry.set(RUNTIME_VISIBLE_ANNOTATION_BYTES, methodActor, runtimeVisibleAnnotationsBytes);
                _classRegistry.set(RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES, methodActor, runtimeVisibleParameterAnnotationsBytes);
                _classRegistry.set(ANNOTATION_DEFAULT_BYTES, methodActor, annotationDefaultBytes);

                if (MaxineVM.isPrototyping() && substituteeIndex != -1) {
                    methodActors[substituteeIndex] = methodActor;
                } else {
                    if (methodActorSet.add(methodActor) != null) {
                        throw classFormatError("Duplicate method name and signature: " + name + " " + descriptor);
                    }
                    methodActors[nextMethodIndex++] = methodActor;
                }
            } finally {
                exitContext();
            }
        }

        if (nextMethodIndex < numberOfMethods) {
            return Arrays.copyOf(methodActors, nextMethodIndex);
        }
        return methodActors;
    }

    protected void readInnerClassesAttribute() {
        if (_innerClasses != null) {
            throw classFormatError("Duplicate InnerClass attribute");
        }

        final int nInnerClasses = _classfileStream.readUnsigned2();
        final InnerClassInfo[] innerClassInfos = new InnerClassInfo[nInnerClasses];
        final TypeDescriptor[] innerClasses = new TypeDescriptor[innerClassInfos.length];
        int nextInnerClass = 0;

        for (int i = 0; i < nInnerClasses; ++i) {
            final InnerClassInfo innerClassInfo = new InnerClassInfo(_classfileStream, _constantPool);
            final int innerClassIndex = innerClassInfo.innerClassIndex();
            final int outerClassIndex = innerClassInfo.outerClassIndex();

            // The JVM specification allows a null inner class but don't ask me what it means!
            if (innerClassIndex == 0) {
                continue;
            }

            // If no outer class is specified, then this entry denotes a local or anonymous class
            // that will have an EnclosingMethod attribute instead. That is, these classes are
            // not *immediately* enclosed by another class
            if (outerClassIndex == 0) {
                continue;
            }

            // If this entry refers to the current class, then the current class must be an inner class:
            // it's enclosing class is recorded and it's flags are updated.
            final TypeDescriptor innerClassDescriptor = _constantPool.classAt(innerClassIndex, "inner class descriptor").typeDescriptor();
            if (innerClassDescriptor.equals(_classDescriptor)) {
                if (_outerClass != null) {
                    throw classFormatError("duplicate outer class");
                }
                _flags |= INNER_CLASS;
                _outerClass = _constantPool.classAt(outerClassIndex).typeDescriptor();
                _flags |= innerClassInfo.flags();
            }

            final TypeDescriptor outerClassDescriptor = _constantPool.classAt(outerClassIndex, "outer class descriptor").typeDescriptor();
            if (outerClassDescriptor.equals(_classDescriptor)) {
                // The inner class is enclosed by the current class
                innerClasses[nextInnerClass++] = _constantPool.classAt(innerClassIndex).typeDescriptor();
            } else {
                // The inner class is enclosed by some class other than current class: ignore it
            }

            for (int j = 0; j < i; ++j) {
                final InnerClassInfo otherInnerClassInfo = innerClassInfos[j];
                if (otherInnerClassInfo != null) {
                    if (innerClassIndex == otherInnerClassInfo.innerClassIndex() && outerClassIndex == otherInnerClassInfo.outerClassIndex()) {
                        throw classFormatError("Duplicate entry in InnerClasses attribute");
                    }
                }
            }
            innerClassInfos[i] = innerClassInfo;
        }

        if (nextInnerClass != 0) {
            if (nextInnerClass == innerClasses.length) {
                _innerClasses = innerClasses;
            } else {
                _innerClasses = new TypeDescriptor[nextInnerClass];
                System.arraycopy(innerClasses, 0, _innerClasses, 0, nextInnerClass);
            }
        }
    }

    protected EnclosingMethodInfo readEnclosingMethodAttribute() {
        final int classIndex = _classfileStream.readUnsigned2();
        final int nameAndTypeIndex = _classfileStream.readUnsigned2();

        final ClassConstant holder = _constantPool.classAt(classIndex);
        final String name;
        final String descriptor;
        if (nameAndTypeIndex != 0) {
            final NameAndTypeConstant nameAndType = _constantPool.nameAndTypeAt(nameAndTypeIndex);
            name = nameAndType.name().toString();
            descriptor = nameAndType.descriptorString();
        } else {
            name = null;
            descriptor = null;
        }

        return new EnclosingMethodInfo(holder.typeDescriptor(), name, descriptor);
    }

    protected ClassActor resolveSuperClass(int superClassIndex, boolean isInterface) {
        final ClassActor superClassActor;
        if (superClassIndex != 0) {
            final TypeDescriptor superClassDescriptor = _constantPool.classAt(superClassIndex, "super class descriptor").typeDescriptor();

            if (superClassDescriptor.equals(_classDescriptor)) {
                throw classFormatError("Class cannot be its own super class");
            }

            /*
             * If this is an interface class, its superclass must be
             * java.lang.Object.
             */
            if (isInterface && !superClassDescriptor.equals(OBJECT)) {
                throw classFormatError("Interface class must inherit from java.lang.Object");
            }

            /*
             * Now ensure the super class is resolved
             */
            superClassActor = _constantPool.classAt(superClassIndex).resolve(_constantPool, superClassIndex);

            /*
             * Cannot inherit from an array class.
             */
            if (superClassActor.isArrayClassActor()) {
                throw classFormatError("Cannot inherit from array class");
            }

            /*
             * The superclass cannot be an interface. From the
             * JVM Spec section 5.3.5:
             *
             *   If the class of interface named as the direct
             *   superclass of C is in fact an interface, loading
             *   throws an IncompatibleClassChangeError.
             */
            if (superClassActor.isInterfaceActor()) {
                throw classFormatError("Cannot extend an interface class");
            }

            /*
             * The superclass cannot be final.
             */
            if (superClassActor.isFinal()) {
                throw verifyError("Cannot extend a final class " + superClassActor.name());
            }
        } else {
            if (!_classDescriptor.equals(OBJECT)) {
                throw classFormatError("missing required super class");
            }
            superClassActor = null;
        }
        return superClassActor;
    }

    protected ClassActor loadClass0(Utf8Constant name) {
        readMagic();
        final char minorVersion = (char) _classfileStream.readUnsigned2();
        final char majorVersion = (char) _classfileStream.readUnsigned2();

        verifyVersion(majorVersion, minorVersion);
        _constantPool = new ConstantPool(_classLoader, _classfileStream);
        _majorVersion = majorVersion;

        _flags = _classfileStream.readUnsigned2();

        verifyClassFlags(_flags, majorVersion);
        final boolean isInterface = isInterface(_flags);

        final int thisClassIndex = _classfileStream.readUnsigned2();
        _classDescriptor = _constantPool.classAt(thisClassIndex, "this class descriptor").typeDescriptor();
        if (!_classDescriptor.equals(getDescriptorForJavaString(name.toString()))) {
            /*
             * VMSpec 5.3.5:
             *
             *   Otherwise, if the purported representation does not actually
             *   represent a class named N, loading throws an instance of
             *   NoClassDefFoundError or an instance of one of its
             *   subclasses.
             */
            throw noClassDefFoundError("'this_class' indicates wrong type");
        }

        final int superClassIndex = _classfileStream.readUnsigned2();
        final ClassActor superClassActor = resolveSuperClass(superClassIndex, isInterface);

        final InterfaceActor[] interfaceActors = readInterfaces();
        final FieldActor[] fieldActors = readFields(isInterface);
        final MethodActor[] methodActors = readMethods(isInterface);

        String sourceFileName = null;
        byte[] runtimeVisibleAnnotationsBytes = null;
        Utf8Constant genericSignature = null;
        EnclosingMethodInfo enclosingMethodInfo = null;

        int nAttributes = _classfileStream.readUnsigned2();
        while (nAttributes-- != 0) {
            final int attributeNameIndex = _classfileStream.readUnsigned2();
            final String attributeName = _constantPool.utf8At(attributeNameIndex, "attribute name").toString();
            final Size attributeSize = _classfileStream.readSize4();
            final Address startPosition = _classfileStream.getPosition();
            if (attributeName.equals("SourceFile")) {
                if (sourceFileName != null) {
                    throw classFormatError("Duplicate SourceFile attribute");
                }
                final int sourceFileNameIndex = _classfileStream.readUnsigned2();
                sourceFileName = _constantPool.utf8At(sourceFileNameIndex, "source file name").toString();
            } else if (attributeName.equals("Deprecated")) {
                _flags += Actor.DEPRECATED;
            } else if (attributeName.equals("Synthetic")) {
                _flags += Actor.ACC_SYNTHETIC;
            } else if (attributeName.equals("InnerClasses")) {
                readInnerClassesAttribute();
            } else if (_majorVersion >= JAVA_1_5_VERSION) {
                if (attributeName.equals("Signature")) {
                    genericSignature = _constantPool.utf8At(_classfileStream.readUnsigned2(), "signature index");
                } else if (attributeName.equals("RuntimeVisibleAnnotations")) {
                    runtimeVisibleAnnotationsBytes = _classfileStream.readByteArray(attributeSize);
                } else if (attributeName.equals("EnclosingMethod")) {
                    if (enclosingMethodInfo != null) {
                        throw classFormatError("Duplicate EnclosingMethod attribute");
                    }
                    enclosingMethodInfo = readEnclosingMethodAttribute();
                } else {
                    _classfileStream.skip(attributeSize);
                }
            } else {
                _classfileStream.skip(attributeSize);
            }

            if (!attributeSize.equals(_classfileStream.getPosition().minus(startPosition))) {
                throw classFormatError("Invalid attribute length for " + name + " attribute");
            }
        }

        // inherit the REFERENCE and FINALIZER bits from the superClassActor
        if (superClassActor != null) {
            if (superClassActor.isReferenceObject()) {
                _flags |= Actor.REFERENCE;
            }
            if (superClassActor.hasFinalizer()) {
                _flags |= Actor.FINALIZER;
            }
        } else {
            // clear the finalizer bit for the java.lang.Object class; otherwise all classes would have it!
            _flags &= ~Actor.FINALIZER;
        }

        // is this a Java Reference object class?
        if (name.equals("java.lang.ref.Reference")) {
            _flags |= Actor.REFERENCE;
        }

        // Ensure there are no trailing bytes
        _classfileStream.checkEndOfFile();

        if (MaxineVM.isPrototyping() && runtimeVisibleAnnotationsBytes != null) {
            final ClassfileStream annotations = new ClassfileStream(runtimeVisibleAnnotationsBytes);
            for (AnnotationInfo annotationInfo : AnnotationInfo.parse(annotations, _constantPool)) {
                if (annotationInfo.annotationTypeDescriptor().equals(forJavaClass(TEMPLATE.class))) {
                    _flags |= TEMPLATE;
                } else if (annotationInfo.annotationTypeDescriptor().equals(forJavaClass(PROTOTYPE_ONLY.class))) {
                    ProgramError.unexpected("Trying to load a prototype only class");
                }
            }
        }

        final ClassActor classActor;
        if (isInterface) {
            classActor = createInterfaceActor(
                            _constantPool,
                            _classLoader,
                            name,
                            majorVersion,
                            minorVersion,
                            _flags,
                            interfaceActors,
                            fieldActors,
                            methodActors,
                            genericSignature,
                            runtimeVisibleAnnotationsBytes,
                            sourceFileName,
                            _innerClasses,
                            _outerClass,
                            enclosingMethodInfo);
        } else {
            classActor = createTupleOrHybridClassActor(
                            _constantPool,
                            _classLoader,
                            name,
                            majorVersion,
                            minorVersion,
                            _flags,
                            superClassActor,
                            interfaceActors,
                            fieldActors,
                            methodActors,
                            genericSignature,
                            runtimeVisibleAnnotationsBytes,
                            sourceFileName,
                            _innerClasses,
                            _outerClass,
                            enclosingMethodInfo);
        }
        if (superClassActor != null) {
            superClassActor.checkAccessBy(classActor);
        }

        if (MaxineVM.isPrototyping() && runtimeVisibleAnnotationsBytes != null) {
            final ClassfileStream annotations = new ClassfileStream(runtimeVisibleAnnotationsBytes);
            for (AnnotationInfo annotationInfo : AnnotationInfo.parse(annotations, _constantPool)) {
                if (annotationInfo.annotationTypeDescriptor().equals(forJavaClass(METHOD_SUBSTITUTIONS.class))) {
                    METHOD_SUBSTITUTIONS.Static.processAnnotationInfo(annotationInfo, classActor);
                }
            }
        }

        return classActor;
    }

    private ClassActor loadClass(final Utf8Constant name, Object source) {
        try {
            String optSource = null;
            if (VerboseVMOption.verboseClassLoading()) {
                if (source != null) {
                    Log.println("[Loading " + name + " from " + source + "]");
                } else {
                    optSource = _classLoader == null ? "generated data" : _classLoader.getClass().getName();
                    Log.println("[Loading " + name + " from " + optSource + "]");
                }
            }
            enterContext(new Object() {
                @Override
                public String toString() {
                    return "loading " + name;
                }
            });
            final ClassActor classActor = loadClass0(name);

            if (VerboseVMOption.verboseClassLoading()) {
                if (source != null) {
                    Log.println("[Loaded " + name + " from " + source + "]");
                } else {
                    Log.println("[Loaded " + name + " from " + optSource + "]");
                }
            }

            TeleClassInfo.registerClassLoaded(classActor);

            return classActor;
        } finally {
            exitContext();
        }
    }

    private static void traceBeforeDefineClass(String name) {
        if (Trace.hasLevel(2)) {
            Trace.begin(2, "defineClass: " + name);
        }
    }

    private static void traceAfterDefineClass(String name) {
        if (Trace.hasLevel(2)) {
            Trace.end(2, "defineClass: " + name);
        }
    }

    /**
     * Converts an array of bytes into a {@code ClassActor}.
     *
     * @param name the name of the class being defined
     * @param classLoader the defining class loader
     * @param bytes The bytes that make up the class data. The bytes should have the format of a valid class file as
     *            defined by the <a href="http://java.sun.com/docs/books/vmspec/">Java Virtual Machine Specification</a>.
     * @param protectionDomain the ProtectionDomain of the class. This value can be null.
     * @param source a object whose {@code toString()} method describes source location of the bytes. This is purely
     *            informative detail. For example, it may be used to provide the output for verbose class loading. This
     *            value can be null.
     * @return the {@code ClassActor} object created from the data, and optional {@code ProtectionDomain}
     * @throws ClassFormatError if the data did not contain a valid class
     * @throws NoClassDefFoundError if {@code name} is not equal to the {@linkplain Class#getName() binary name} of the
     *             class specified by {@code bytes}
     */
    public static ClassActor defineClassActor(String name, ClassLoader classLoader, byte[] bytes, ProtectionDomain protectionDomain, Object source) {
        return defineClassActor(name, classLoader, bytes, 0, bytes.length, protectionDomain, source);
    }

    /**
     * Converts an array of bytes into a {@code ClassActor}.
     *
     * @param name the name of the class being defined
     * @param classLoader the defining class loader
     * @param bytes the bytes that make up the class data. The bytes in positions {@code offset} through
     *            {@code offset + length - 1} should have the format of a valid class file as defined by the <a
     *            href="http://java.sun.com/docs/books/vmspec/">Java Virtual Machine Specification</a>.
     * @param offset the start offset in {@code bytes} of the class data
     * @param length the length of the class data
     * @param protectionDomain the ProtectionDomain of the class. This value can be null.
     * @param source a object whose {@code toString()} method describes source location of the bytes. This is purely
     *            informative detail. For example, it may be used to provide the output for verbose class loading. This
     *            value can be null.
     * @return the {@code ClassActor} object created from the data, and optional {@code ProtectionDomain}
     * @throws ClassFormatError if the data did not contain a valid class
     * @throws NoClassDefFoundError if {@code name} is not equal to the {@linkplain Class#getName() binary name} of the
     *             class specified by {@code bytes}
     */
    public static ClassActor defineClassActor(String name, ClassLoader classLoader, byte[] bytes, int offset, int length, ProtectionDomain protectionDomain, Object source) {
        traceBeforeDefineClass(name);
        try {
            final ClassfileStream classfileStream = new ClassfileStream(bytes, offset, length);
            final ClassfileReader classfileReader = new ClassfileReader(classfileStream, classLoader);
            final ClassActor classActor = classfileReader.loadClass(SymbolTable.makeSymbol(name), source);
            classActor.setProtectionDomain(protectionDomain);
            return classActor;
        } finally {
            traceAfterDefineClass(name);
        }
    }
}
