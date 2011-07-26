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
package com.sun.max.vm.classfile.constant;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;
import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Holds the constants found in a constant pool. A constant pool can be {@linkplain ConstantPoolEditor edited} to add new entries.
 *
 * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#20080">4.4 The Constant Pool</a>
 */
public final class ConstantPool implements RiConstantPool {

    /**
     * The initial capacity of a {@linkplain #ConstantPool(ClassLoader) generated} pool.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * Table 4.3 in #4.4.
     */
    public enum Tag {
        CLASS(7) {
            @Override
            public Kind valueKind() {
                return Kind.REFERENCE;
            }
        },
        FIELD_REF(9),
        METHOD_REF(10),
        INTERFACE_METHOD_REF(11),
        STRING(8) {
            @Override
            public Kind valueKind() {
                return Kind.REFERENCE;
            }
        },
        INTEGER(3) {
            @Override
            public Kind valueKind() {
                return Kind.INT;
            }
        },
        FLOAT(4) {
            @Override
            public Kind valueKind() {
                return Kind.FLOAT;
            }
        },
        LONG(5) {
            @Override
            public Kind valueKind() {
                return Kind.LONG;
            }
        },
        DOUBLE(6) {
            @Override
            public Kind valueKind() {
                return Kind.DOUBLE;
            }
        },
        NAME_AND_TYPE(12),
        UTF8(1),
        INVALID(0);

        private final byte classfileTag;

        /**
         * @param classfileTag
         */
        private Tag(int classfileTag) {
            assert (byte) classfileTag == classfileTag;
            this.classfileTag = (byte) classfileTag;
        }

        /**
         * Gets the kind of the Java program accessible constant value denoted by this tag.
         *
         * @throws ClassFormatError if the pool constant type denoted by this tag is not accessible as a Java program value
         */
        public Kind valueKind() {
            throw classFormatError(this + " pool constants are not accessible as Java program values");
        }

        public byte classfileTag() {
            return classfileTag;
        }

        /**
         * Decodes a tag from a classfile to the canonical Tag value representing an entry of the appropriate type.
         *
         * Constant Type               | Value
         * ----------------------------+--------
         * CONSTANT_Class              | 7
         * CONSTANT_Fieldref           | 9
         * CONSTANT_Methodref          | 10
         * CONSTANT_InterfaceMethodref | 11
         * CONSTANT_String             | 8
         * CONSTANT_Integer            | 3
         * CONSTANT_Float              | 4
         * CONSTANT_Long               | 5
         * CONSTANT_Double             | 6
         * CONSTANT_NameAndType        | 12
         * CONSTANT_Utf8               | 1
         */
        static Tag fromClassfile(int tag) {
            switch (tag) {
                case 7:
                    return CLASS;
                case 9:
                    return FIELD_REF;
                case 10:
                    return METHOD_REF;
                case 11:
                    return INTERFACE_METHOD_REF;
                case 8:
                    return STRING;
                case 3:
                    return INTEGER;
                case 4:
                    return FLOAT;
                case 5:
                    return LONG;
                case 6:
                    return DOUBLE;
                case 12:
                    return NAME_AND_TYPE;
                case 1:
                    return UTF8;
                default:
                    throw classFormatError("Invalid constant pool entry tag " + tag);
            }
        }

        public static final List<Tag> VALUES = Arrays.asList(values());
    }

    /**
     * Must only be updated by {@link ConstantPoolEditor#append(PoolConstant)}.
     */
    @INSPECTED
    private PoolConstant[] constants;

    public PoolConstant[] constants() {
        return constants;
    }

    public void setConstants(PoolConstant[] constants) {
        this.constants = constants;
    }

    public void setConstant(int index, PoolConstant constant) {
        constants[index] = constant;
    }

    /**
     * Must only be updated by {@link ConstantPoolEditor#append(PoolConstant)}.
     */
    int length;

    private final ClassLoader classLoader;

    /**
     * Creates a constant pool from a class file.
     */
    public ConstantPool(ClassLoader classLoader, ClassfileStream classfileStream) {
        final int poolLength = classfileStream.readUnsigned2();
        if (poolLength < 1) {
            throw classFormatError("Invalid constant pool size (" + poolLength + ")");
        }

        final Tag[] tags = new Tag[poolLength];
        final int[] rawEntries = new int[poolLength];
        final PoolConstant[] poolConstants = new PoolConstant[poolLength];
        poolConstants[0] = InvalidConstant.VALUE;

        this.classLoader = classLoader;
        this.length = poolLength;

        // Pass 1: read in the primitive values
        int i = 1;
        while (i < poolLength) {
            final int tagByte = classfileStream.readUnsigned1();
            final Tag tag = Tag.fromClassfile(tagByte);
            tags[i] = tag;
            switch (tag) {
                case CLASS: {
                    rawEntries[i] = classfileStream.readUnsigned2();
                    break;
                }
                case STRING: {
                    rawEntries[i] = classfileStream.readUnsigned2();
                    break;
                }
                case FIELD_REF:
                case METHOD_REF:
                case INTERFACE_METHOD_REF: {
                    final int classIndex = classfileStream.readUnsigned2();
                    final int nameAndTypeIndex = classfileStream.readUnsigned2();
                    rawEntries[i] = (classIndex << 16) | (nameAndTypeIndex & 0xFFFF);
                    break;
                }
                case NAME_AND_TYPE: {
                    final int nameIndex = classfileStream.readUnsigned2();
                    final int descriptorIndex = classfileStream.readUnsigned2();
                    rawEntries[i] = (nameIndex << 16) | (descriptorIndex & 0xFFFF);
                    break;
                }
                case INTEGER: {
                    poolConstants[i] = createIntegerConstant(classfileStream.readInt());
                    break;
                }
                case FLOAT: {
                    poolConstants[i] = createFloatConstant(classfileStream.readFloat());
                    break;
                }
                case LONG: {
                    poolConstants[i] = createLongConstant(classfileStream.readLong());
                    ++i;
                    try {
                        tags[i] = INVALID;
                        poolConstants[i] = InvalidConstant.VALUE;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw classFormatError("Invalid long constant index " + (i - 1));
                    }
                    break;
                }
                case DOUBLE: {
                    poolConstants[i] = createDoubleConstant(classfileStream.readDouble());
                    ++i;
                    try {
                        tags[i] = INVALID;
                        poolConstants[i] = InvalidConstant.VALUE;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw classFormatError("Invalid double constant index " + (i - 1));
                    }
                    break;
                }
                case UTF8: {
                    poolConstants[i] = makeUtf8Constant(classfileStream.readUtf8String());
                    break;
                }
                default: {
                    ProgramError.unexpected();
                }
            }
            i++;
        }

        // Pass 2: first verification pass - validate cross references and fixup class and string constants
        i = 1;
        while (i < poolLength) {
            try {
                final Tag tag = tags[i];
                switch (tag) {
                    case CLASS: {
                        final int nameIndex = rawEntries[i];
                        final Utf8Constant utf8Constant = (Utf8Constant) poolConstants[nameIndex];
                        final String name = utf8Constant.toString();
                        if (name.charAt(0) == '[') {
                            poolConstants[i] = createClassConstant(JavaTypeDescriptor.parseTypeDescriptor(name));
                        } else {
                            poolConstants[i] = createClassConstant(JavaTypeDescriptor.parseTypeDescriptor('L' + name + ';'));
                        }
                        break;
                    }
                    case STRING: {
                        final int stringIndex = rawEntries[i];
                        final Utf8Constant utf8Constant = (Utf8Constant) poolConstants[stringIndex];
                        final String string = utf8Constant.toString();
                        poolConstants[i] = createStringConstant(string);
                        break;
                    }
                    case NAME_AND_TYPE: {
                        final int nameAndType = rawEntries[i];
                        final int nameIndex = nameAndType >>> 16;
                        final int descriptorIndex = nameAndType & 0xffff;
                        final Utf8Constant name = (Utf8Constant) poolConstants[nameIndex];
                        final Utf8Constant descriptor = (Utf8Constant) poolConstants[descriptorIndex];
                        poolConstants[i] = new NameAndTypeConstant(name, descriptor);
                        break;
                    }
                    case FIELD_REF: {
                        final int classNameAndType = rawEntries[i];
                        final int classIndex = classNameAndType >> 16;
                        final int nameAndTypeIndex = classNameAndType & 0xFFFF;
                        final FieldRefConstant.UnresolvedIndices fieldRef = new FieldRefConstant.UnresolvedIndices(classIndex, nameAndTypeIndex, tags);
                        poolConstants[i] = fieldRef;
                        break;
                    }
                    case METHOD_REF: {
                        final int classNameAndType = rawEntries[i];
                        final int classIndex = classNameAndType >> 16;
                        final int nameAndTypeIndex = classNameAndType & 0xFFFF;
                        final ClassMethodRefConstant.UnresolvedIndices methodRef = new ClassMethodRefConstant.UnresolvedIndices(classIndex, nameAndTypeIndex, tags);
                        poolConstants[i] = methodRef;
                        break;
                    }
                    case INTERFACE_METHOD_REF: {
                        final int classNameAndType = rawEntries[i];
                        final int classIndex = classNameAndType >> 16;
                        final int nameAndTypeIndex = classNameAndType & 0xFFFF;
                        final InterfaceMethodRefConstant.UnresolvedIndices methodRef = new InterfaceMethodRefConstant.UnresolvedIndices(classIndex, nameAndTypeIndex, tags);
                        poolConstants[i] = methodRef;
                        break;
                    }
                    default:
                        break;
                }
            } catch (NullPointerException e) {
                throw classFormatError("Invalid constant pool entry type at index " + i);
            } catch (ClassCastException e) {
                throw classFormatError("Invalid constant pool entry type at index " + i);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw classFormatError("Invalid constant pool index in entry at index " + i);
            }
            ++i;
        }

        this.constants = poolConstants;

        // Pass 3: second verification pass - checks the strings are of the right format
        i = 1;
        while (i < poolLength) {
            try {
                final Tag tag = tags[i];
                switch (tag) {
                    case FIELD_REF: {
                        final FieldRefConstant fieldRef = fieldAt(i);
                        fieldRef.type(this);
                        ClassfileReader.verifyFieldName(fieldRef.name(this));
                        break;
                    }
                    case INTERFACE_METHOD_REF:
                    case METHOD_REF: {
                        final MethodRefConstant methodRef = methodAt(i);
                        methodRef.signature(this);
                        ClassfileReader.verifyMethodName(methodRef.name(this), false);
                        break;
                    }
                    default:
                        break;
                }
            } catch (NullPointerException e) {
                throw classFormatError("Invalid constant pool entry type at index " + i);
            } catch (ClassCastException e) {
                throw classFormatError("Invalid constant pool entry type at index " + i);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw classFormatError("Invalid constant pool index in entry at index " + i);
            }
            ++i;
        }
    }

    /**
     * Creates a constant pool for a generated class.
     */
    public ConstantPool(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.constants = new PoolConstant[INITIAL_CAPACITY];

        // Index 0 is always invalid
        this.constants[length++] = InvalidConstant.VALUE;
    }

    /**
     * Creates a constant pool for a generated class.
     */
    public ConstantPool(ClassLoader classLoader, PoolConstant[] constants, int length) {
        this.classLoader = classLoader;

        if (length < 1 || length > constants.length) {
            throw new IllegalArgumentException("length < 1 || length > constants.length");
        }

        if (constants[0] != InvalidConstant.VALUE) {
            throw new IllegalArgumentException("constants make have " + Tag.INVALID + " entry at index 0");
        }
        this.constants = constants;
        this.length = length;
    }

    /**
     * Creates an object that wraps the result of resolving a resolvable constant for a particular reason.
     *
     * @param index the index of a resolvable entry in this constant pool
     */
    public ResolutionGuard.InPool makeResolutionGuard(int index) {
        return new ResolutionGuard.InPool(this, index);
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public int numberOfConstants() {
        return length;
    }

    @INSPECTED
    @CONSTANT
    private ClassActor holder;

    public void setHolder(ClassActor holder) {
        assert this.holder == null;
        this.holder = holder;
    }

    public ClassActor holder() {
        return holder;
    }

    static ClassFormatError unexpectedEntry(int index, Tag tag, String description, Tag... expected) {
        throw verifyError("Constant pool entry" + (description == null ? "" : " for " + description) + " at " + index + " is a " + tag + ", expected " + com.sun.max.Utils.toString(expected, " or "));
    }

    private ClassFormatError unexpectedEntry(int index, String description, Tag... expected) {
        throw unexpectedEntry(index, tagAt(index), description, expected);
    }

    public PoolConstant at(int index) {
        return at(index, null);
    }

    public PoolConstant at(int index, String description) {
        try {
            return constants[index];
        } catch (IndexOutOfBoundsException exception) {
            throw verifyError("Constant pool index (" + index + ")" + (description == null ? "" : " for " + description) + " is out of range");
        }
    }

    /**
     * Updates the constant entry at a given index.
     *
     * This may overwrite another resolved constant in the situation of two threads racing to resolve a constant.
     * However, the two resolved constants are equivalent and so which one actually gets written to the pool
     * does not matter. This means that two pool constants for the same index cannot be compared for equality with '=='
     * but that's true anyway given that there's no guarantee of the two constants both being the resolved version.
     *
     * @param constant
     *                the {@linkplain PoolConstant#isResolved() resolved} constant to write at {@code index}
     */
    void updateAt(int index, ResolvableConstant constant) {
        assert constant.isResolved();
        assert constants[index].tag() == constant.tag();

        constants[index] = constant;
    }

    /**
     * Gets the tag at a given index. If {@code index == 0} or there is no valid entry at {@code index} (e.g. it denotes
     * the slot following a double or long entry), then {@link Tag#INVALID} is returned.
     */
    public Tag tagAt(int index) {
        try {
            return constants[index].tag();
        } catch (IndexOutOfBoundsException exception) {
            throw verifyError("Constant pool index " + index + " is out of range");
        }
    }

    public ResolvableConstant resolvableAt(int index) {
        try {
            return (ResolvableConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, CLASS, FIELD_REF, METHOD_REF, INTERFACE_METHOD_REF);
        }
    }

    public ValueConstant valueConstantAt(int index) {
        try {
            return (ValueConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, INTEGER, FLOAT, LONG, DOUBLE, STRING, CLASS);
        }
    }

    public Value valueAt(int index) {
        return valueConstantAt(index).value(this, index);
    }

    public int intAt(int index) {
        return intAt(index, null);
    }

    public int intAt(int index, String description) {
        try {
            final IntegerConstant constant = (IntegerConstant) at(index);
            return constant.value();
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, INTEGER);
        }
    }

    public long longAt(int index) {
        return longAt(index, null);
    }

    public long longAt(int index, String description) {
        try {
            final LongConstant constant = (LongConstant) at(index);
            return constant.value();
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, LONG);
        }
    }

    public float floatAt(int index) {
        return floatAt(index, null);
    }

    public float floatAt(int index, String description) {
        try {
            final FloatConstant constant = (FloatConstant) at(index);
            return constant.value();
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, FLOAT);
        }
    }

    public double doubleAt(int index) {
        return doubleAt(index, null);
    }

    public double doubleAt(int index, String description) {
        try {
            final DoubleConstant constant = (DoubleConstant) at(index);
            return constant.value();
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, DOUBLE);
        }
    }

    public NameAndTypeConstant nameAndTypeAt(int index) {
        return nameAndTypeAt(index, null);
    }

    public NameAndTypeConstant nameAndTypeAt(int index, String description) {
        try {
            return (NameAndTypeConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, NAME_AND_TYPE);
        }
    }

    public Utf8Constant utf8At(int index, String description) {
        return utf8ConstantAt(index, description);
    }

    public Utf8Constant utf8At(int index) {
        return utf8ConstantAt(index, null);
    }

    public ClassConstant classAt(int index) {
        return classAt(index, null);
    }

    public ClassConstant classAt(int index, String description) {
        try {
            return (ClassConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, CLASS);
        }
    }

    public MemberRefConstant memberAt(int index) {
        return memberAt(index, null);
    }

    public MemberRefConstant memberAt(int index, String description) {
        try {
            return (MemberRefConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, METHOD_REF, INTERFACE_METHOD_REF, FIELD_REF);
        }
    }

    public MethodRefConstant methodAt(int index) {
        try {
            return (MethodRefConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, METHOD_REF, INTERFACE_METHOD_REF);
        }
    }

    public ClassMethodRefConstant classMethodAt(int index) {
        try {
            return (ClassMethodRefConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, METHOD_REF);
        }
    }

    public InterfaceMethodRefConstant interfaceMethodAt(int index) {
        try {
            return (InterfaceMethodRefConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, INTERFACE_METHOD_REF);
        }
    }

    public FieldRefConstant fieldAt(int index) {
        try {
            return (FieldRefConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, FIELD_REF);
        }
    }

    public Utf8Constant utf8ConstantAt(int index, String description) {
        try {
            return (Utf8Constant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, description, UTF8);
        }
    }

    public StringConstant stringConstantAt(int index) {
        try {
            return (StringConstant) at(index);
        } catch (ClassCastException e) {
            throw unexpectedEntry(index, null, STRING);
        }
    }

    public String stringAt(int index) {
        return stringConstantAt(index).value;
    }

    public void trace(int requiredLevel) {
        if (Trace.hasLevel(requiredLevel)) {
            Trace.begin(requiredLevel, "ConstantPool: " + numberOfConstants());
            for (int i = 0; i < length; i++) {
                final Tag tag = tagAt(i);
                if (tag == INVALID) {
                    // The entry after a long or double constant is empty
                    Trace.line(requiredLevel, Integer.toString(i) + ": null");
                } else {
                    Trace.line(requiredLevel, Integer.toString(i) + ": [" + tag + "] " + at(i));
                }
            }
            Trace.end(requiredLevel, "ConstantPool");
        }
    }

    ConstantPoolEditor editor;

    /**
     * Invoking this method is equivalent to {@link #edit(boolean) edit(true)}.
     */
    public ConstantPoolEditor edit() {
        return edit(true);
    }

    /**
     * Gets an object that can be used to look up and/or append new entries to this pool.
     *
     * At most one thread can be editing a constant pool instance. As such, the current thread
     * is blocked if another thread is currently editing this constant pool and it is only
     * woken up once the other thread  {@linkplain ConstantPoolEditor#release() releases} the editor.
     *
     * @param allowAppending
     *            specifies if the client is allowed to append entries to the pool
     */
    public synchronized ConstantPoolEditor edit(boolean allowAppending) {
        if (editor == null || editor.owner() != Thread.currentThread()) {
            while (editor != null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            editor = new ConstantPoolEditor(this, allowAppending);
        } else {
            editor.acquire();
        }
        return editor;
    }

    /**
     * Invoking this method is equivalent to {@link #edit(ConstantPoolEditorClient, boolean)}.
     */
    public void edit(ConstantPoolEditorClient client) {
        edit(client, true);
    }

    /**
     * Runs a supplied piece of code that looks up and/or appends entries to this pool. The call back mechanism used for
     * doing the editing ensures that no other thread can be editing the pool concurrently.
     *
     * @param allowAppending
     *            specifies if the client is allowed to append entries to the pool
     */
    public synchronized void edit(ConstantPoolEditorClient client, boolean allowAppending) {
        final ConstantPoolEditor constantPoolEditor = edit();
        try {
            client.edit(constantPoolEditor);
        } finally {
            constantPoolEditor.release();
        }
    }

    @Override
    public String toString() {
        return "ConstantPool[" + holder() + "]";
    }

    // --- Implementation of RiConstantPool ---

    public MethodActor resolveInvokeVirtual(int cpi) {
        final VirtualMethodActor virtualMethodActor = classMethodAt(cpi).resolveVirtual(this, cpi);
        if (virtualMethodActor.isInitializer()) {
            throw new VerifyError("<init> must be invoked with invokespecial");
        }
        return virtualMethodActor;
    }

    public static boolean isSpecial(MethodActor declaredMethod, ClassActor currentClassActor) {
        final ClassActor holder = declaredMethod.holder();
        return (currentClassActor.flags() & Actor.ACC_SUPER) != 0 && currentClassActor != holder && currentClassActor.hasSuperClass(holder) && !declaredMethod.isInstanceInitializer();
    }

    public MethodActor resolveInvokeSpecial(int cpi) {
        MethodActor methodActor = classMethodAt(cpi).resolveVirtual(this, cpi);
        if (isSpecial(methodActor, holder())) {
            methodActor = holder().superClassActor.findVirtualMethodActor(methodActor);
            if (methodActor == null) {
                throw new AbstractMethodError();
            }
        }
        if (methodActor.isAbstract()) {
            throw new AbstractMethodError();
        }
        return methodActor;
    }

    public MethodActor resolveInvokeInterface(int cpi) {
        final InterfaceMethodRefConstant methodConstant = interfaceMethodAt(cpi);
        final MethodActor declaredMethod = methodConstant.resolve(this, cpi);
        if (!(declaredMethod instanceof InterfaceMethodActor)) {
            throw new InternalError("Use of INVOKEINTERFACE for a method in java.lang.Object should have been rewritten");
        }
        return declaredMethod;
    }

    public MethodActor resolveInvokeStatic(int cpi) {
        final StaticMethodActor staticMethodActor = classMethodAt(cpi).resolveStatic(this, cpi);
        if (staticMethodActor.isConstructor()) {
            throw new VerifyError("<init> must be invoked with invokespecial");
        }
        return staticMethodActor;
    }

    private FieldActor checkResolvedFieldAccess(FieldActor field, int opcode) {
        if (opcode >= 0) {
            if (ALIAS.Static.isAliased(field)) {
                // The use of an aliased field is always ok.
                return field;
            }

            switch (opcode) {
                case GETSTATIC: {
                    if (!field.isStatic()) {
                        // IncompatibleClassChangeError
                        return null;
                    }
                    break;
                }
                case PUTSTATIC: {
                    if (!field.isStatic()) {
                        // IncompatibleClassChangeError
                        return null;
                    }
                    if (field.isFinal() && field.holder() != holder()) {
                        // IllegalAccessError
                        return null;
                    }
                    break;
                }
                case GETFIELD: {
                    if (field.isStatic()) {
                        // IncompatibleClassChangeError
                        return null;
                    }
                    break;
                }
                case PUTFIELD: {
                    if (field.isStatic()) {
                        // IncompatibleClassChangeError
                        return null;
                    }
                    if (field.isFinal() && field.holder() != holder()) {
                        // IllegalAccessError
                        return null;
                    }
                    break;
                }
            }
        }
        return field;
    }

    public RiField lookupField(int cpi, int opcode) {
        FieldRefConstant constant = fieldAt(cpi);
        if (constant.isResolvableWithoutClassLoading(this)) {
            // the resolution can occur without side effects
            try {
                FieldActor field = checkResolvedFieldAccess(constant.resolve(this, cpi), opcode);
                if (field != null) {
                    return field;
                }
            } catch (LinkageError error) {
                // Treat as unresolved so that error is thrown later at run time
            }
        }
        RiType holder = UnresolvedType.toRiType(constant.holder(this), holder());
        RiType type = UnresolvedType.toRiType(constant.type(this), holder());
        String name = constant.name(this).string;
        return new UnresolvedField(this, cpi, holder, name, type);
    }

    private MethodActor checkResolvedMethodAccess(MethodActor method, int opcode) {
        if (opcode >= 0) {
            if (ALIAS.Static.isAliased(method)) {
                // The use of an aliased method is always ok.
                return method;
            }

            switch (opcode) {
                case INVOKESTATIC: {
                    if (!method.isStatic()) {
                        // IncompatibleClassChangeError
                        return null;
                    }
                    if (method.isInitializer()) {
                        // VerifyError: <init> must be invoked with INVOKESPECIAL
                        return null;
                    }
                    break;
                }
                case INVOKEINTERFACE:
                case INVOKESPECIAL:
                case INVOKEVIRTUAL: {
                    if (method.isStatic()) {
                        // IncompatibleClassChangeError
                        return null;
                    }
                    if (method.isInitializer() && opcode != INVOKESPECIAL) {
                        // VerifyError: <init> must be invoked with INVOKESPECIAL
                        return null;
                    }
                    break;
                }
            }
        }
        return method;
    }

    public RiMethod lookupMethod(int cpi, int opcode) {
        MethodRefConstant constant = methodAt(cpi);
        if (constant.isResolvableWithoutClassLoading(this)) {
            // the resolution can occur without side effects
            try {
                MethodActor method = checkResolvedMethodAccess(constant.resolve(this, cpi), opcode);
                if (method != null) {
                    return method;
                }
            } catch (LinkageError error) {
                // Treat as unresolved
            }
        }
        RiType holder = UnresolvedType.toRiType(constant.holder(this), holder());
        RiSignature signature = constant.signature(this);
        String name = constant.name(this).string;

        return new UnresolvedMethod(this, cpi, holder, name, signature);
    }

    public RiType lookupType(int cpi, int opcode) {
        return typeFrom(classAt(cpi), cpi, opcode);
    }

    public RiSignature lookupSignature(int cpi) {
        return SignatureDescriptor.create(utf8At(cpi).string);
    }

    public Object lookupConstant(int cpi) {
        switch (tagAt(cpi)) {
            case CLASS: {
                RiType type = typeFrom(classAt(cpi), cpi, LDC);
                if (type.isResolved()) {
                    ClassActor classActor = (ClassActor) type;
                    return CiConstant.forObject(classActor.javaClass());
                }
                return type;
            }
            case INTEGER: {
                return CiConstant.forInt(intAt(cpi));
            }
            case FLOAT: {
                return CiConstant.forFloat(floatAt(cpi));
            }
            case STRING: {
                return CiConstant.forObject(stringAt(cpi));
            }
            case LONG: {
                return CiConstant.forLong(longAt(cpi));
            }
            case DOUBLE: {
                return CiConstant.forDouble(doubleAt(cpi));
            }
            default:
                throw ProgramError.unexpected("unknown constant type");
        }
    }

    private RiType typeFrom(ClassConstant constant, int cpi, int opcode) {
        if (constant.isResolvableWithoutClassLoading(this)) {
            try {
                // the resolution can occur without side effects
                ClassActor type = checkResolvedTypeAccess(constant.resolve(this, cpi), opcode);
                if (type != null) {
                    return type;
                }
            } catch (LinkageError error) {
                // Treat as unresolved so that error is thrown later at run time
            }
        }
        return new UnresolvedType.InPool(constant.typeDescriptor(), this, cpi);
    }

    private ClassActor checkResolvedTypeAccess(ClassActor classActor, int opcode) {
        if (opcode >= 0) {
            switch (opcode) {
                case NEW: {
                    if (classActor.isAbstract() || classActor.isArrayClass()) {
                        // InstantiationError
                        return null;
                    }
                    break;
                }
                case MULTIANEWARRAY:
                case ANEWARRAY:
                case CHECKCAST:
                case INSTANCEOF:
                case LDC: {
                    // No extra checks required
                    break;
                }
            }
        }
        return classActor;
    }

    public CiConstant encoding() {
        return CiConstant.forObject(this);
    }
}
