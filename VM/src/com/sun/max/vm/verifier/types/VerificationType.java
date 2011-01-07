/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.verifier.types;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * The base class used to describe a hierarchy of types upon which a verifier enforces a type system. This hierarchy is
 * depicted in the following diagram (extracted from the <a href="http://jcp.org/en/jsr/detail?id=202">JSR 202 Class
 * File Specification Update</a>):
 *
 * <img src="doc-files/VerificationType-1.png" alt="The Java verification type hierarchy.">
 *
 * @author Doug Simon
 */
public abstract class VerificationType {

    public static final int ITEM_Top = 0;
    public static final int ITEM_Integer = 1;
    public static final int ITEM_Float = 2;
    public static final int ITEM_Double = 3;
    public static final int ITEM_Long = 4;
    public static final int ITEM_Null = 5;
    public static final int ITEM_UninitializedThis = 6;
    public static final int ITEM_Object = 7;
    public static final int ITEM_Uninitialized = 8;

    public static final VerificationType[] NO_TYPES = {};

    public static final TopType TOP = new TopType();
    public static final Category1Type CATEGORY1 = new Category1Type();
    public static final Category2Type CATEGORY2 = new Category2Type();
    public static final BooleanType BOOLEAN = new BooleanType();
    public static final ByteType BYTE = new ByteType();
    public static final CharType CHAR = new CharType();
    public static final ShortType SHORT = new ShortType();
    public static final IntegerType INTEGER = new IntegerType();
    public static final FloatType FLOAT = new FloatType();
    public static final LongType LONG = new LongType();
    public static final Long2Type LONG2 = new Long2Type();
    public static final DoubleType DOUBLE = new DoubleType();
    public static final Double2Type DOUBLE2 = new Double2Type();
    public static final ReferenceOrWordType REFERENCE_OR_WORD = new ReferenceOrWordType();
    public static final ReferenceType REFERENCE = new ReferenceType();
    public static final WordType WORD = new WordType();
    public static final NullType NULL = new NullType();
    public static final UninitializedType UNINITIALIZED = new UninitializedType();
    public static final UninitializedThisType UNINITIALIZED_THIS = new UninitializedThisType();
    public static final Subroutine SUBROUTINE = new Subroutine(-1, 0) {
        @Override
        public void accessesVariable(int index) {
        }

        @Override
        public boolean isVariableAccessed(int index) {
            return false;
        }

        @Override
        public String toString() {
            return "merged-subroutine";
        }
    };

    public static final ObjectType OBJECT = new ResolvedObjectType(ClassActor.fromJava(Object.class));
    public static final ObjectType STRING = new ResolvedObjectType(ClassActor.fromJava(String.class));
    public static final ObjectType CLASS = new ResolvedObjectType(ClassActor.fromJava(Class.class));
    public static final ObjectType THROWABLE = new ResolvedObjectType(ClassActor.fromJava(Throwable.class));
    public static final ObjectType CLONEABLE = new ResolvedObjectType(ClassActor.fromJava(Cloneable.class));
    public static final ObjectType SERIALIZABLE = new ResolvedObjectType(ClassActor.fromJava(Serializable.class));
    public static final ObjectType VM_REFERENCE = new ResolvedObjectType(ClassActor.fromJava(Reference.class));

    public static final ObjectType OBJECT_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(Object[].class), OBJECT);
    public static final ObjectType BYTE_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(byte[].class), BYTE);
    public static final ObjectType BOOLEAN_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(boolean[].class), BOOLEAN);
    public static final ObjectType CHAR_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(char[].class), CHAR);
    public static final ObjectType SHORT_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(short[].class), SHORT);
    public static final ObjectType INTEGER_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(int[].class), INTEGER);
    public static final ObjectType FLOAT_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(float[].class), FLOAT);
    public static final ObjectType DOUBLE_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(double[].class), DOUBLE);
    public static final ObjectType LONG_ARRAY = new ResolvedArrayType((ArrayClassActor) ClassActor.fromJava(long[].class), LONG);

    public static final List<VerificationType> PRIMITIVE_TYPES;
    public static final List<ArrayType> PRIMITIVE_ARRAY_TYPES;
    public static final List<ObjectType> PREDEFINED_OBJECT_TYPES;
    public static final List<VerificationType> ALL_PREDEFINED_TYPES;

    static {
        final List<VerificationType> primitiveTypes = new ArrayList<VerificationType>();
        final List<ArrayType> primitiveArrayTypes = new ArrayList<ArrayType>();
        final List<VerificationType> allPredefinedTypes = new ArrayList<VerificationType>();
        final List<ObjectType> predefinedObjectTypes = new ArrayList<ObjectType>();
        for (Field field : VerificationType.class.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) {
                if (VerificationType.class.isAssignableFrom(field.getType())) {
                    try {
                        final VerificationType type = (VerificationType) field.get(null);
                        allPredefinedTypes.add(type);
                        if (type instanceof ObjectType) {
                            final ObjectType objectType = (ObjectType) type;
                            predefinedObjectTypes.add(objectType);
                            if (type.isArray() && JavaTypeDescriptor.isPrimitive(type.componentType().typeDescriptor())) {
                                primitiveArrayTypes.add((ArrayType) type);
                            }
                        }
                        if (type.typeDescriptor() != null && JavaTypeDescriptor.isPrimitive(type.typeDescriptor())) {
                            primitiveTypes.add(type);
                        }
                    } catch (IllegalAccessException illegalAccessException) {
                        ProgramError.unexpected("could not get value of field: " + field);
                    }
                }
            }
        }
        PRIMITIVE_TYPES = primitiveTypes;
        PRIMITIVE_ARRAY_TYPES = primitiveArrayTypes;
        PREDEFINED_OBJECT_TYPES = predefinedObjectTypes;
        ALL_PREDEFINED_TYPES = allPredefinedTypes;
    }

    public final boolean isAssignableFrom(VerificationType from) {
        return from == this || isAssignableFromDifferentType(from);
    }

    /**
     * A temporary hack to accommodate the fact that {@link Pointer} implements {@link Accessor}.
     * This will be removed once {@link Accessor} is removed.
     */
    public static boolean isTypeIncompatibilityBetweenPointerAndAccessor(VerificationType from, VerificationType to) {
        if (from == VerificationType.WORD) {
            if (to.toString().equals(Accessor.class.getName())) {
                return true;
            }
        }
        return false;
    }

    public abstract boolean isAssignableFromDifferentType(VerificationType from);

    /**
     * Gets the type descriptor for this type.
     * <p>
     * The default implementation of this returns null, indicating that the type has no valid type descriptor.
     */
    public TypeDescriptor typeDescriptor() {
        return null;
    }

    /**
     * Determines if this type represents an array.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Gets the {@linkplain ClassActor#componentClassActor() component type} of this type. Returns null if this type
     * does not represent an array.
     */
    public VerificationType componentType() {
        return null;
    }

    /**
     * Returns the size of this type in terms of 32-bit words. For example, an INT would return 1 and a DOUBLE would return 2.
     */
    public abstract int size();

    /**
     * Determines if this type is a category2 type.
     */
    public boolean isCategory2() {
        return false;
    }

    /**
     * Gets the type representing the second word of a double word type.
     * <p>
     * The default implementation of this method throws an error indicating
     * this verification type is not a double word type.
     */
    public VerificationType secondWordType() {
        throw verifyError("verification type does not have a second word type: " + this);
    }

    /**
     * Determines if this type represents the second word of a long or double value.
     */
    public boolean isSecondWordType() {
        return false;
    }

    /**
     * Find the lowest common denominator for this type and {@code from}.
     * <p>
     * All interfaces are treated as if they were of type java/lang/Object, since
     * the runtime will do the full checking.
     * <p>
     * The default implementation of this method returns {@link #TOP}, indicating that the two types are not mergeable.
     *
     * @return
     */
    public final VerificationType mergeWith(VerificationType from) {
        if (this == from) {
            return this;
        }
        return mergeWithDifferentType(from);
    }

    protected VerificationType mergeWithDifferentType(VerificationType from) {
        return TOP;
    }

    public static VerificationType getVerificationType(TypeDescriptor typeDescriptor, VerificationRegistry registry) {
        switch (typeDescriptor.toKind().asEnum) {
            case VOID:
                return VerificationType.TOP;
            case BYTE:
                return VerificationType.BYTE;
            case BOOLEAN:
                return VerificationType.BOOLEAN;
            case SHORT:
                return VerificationType.SHORT;
            case CHAR:
                return VerificationType.CHAR;
            case INT:
                return VerificationType.INTEGER;
            case FLOAT:
                return VerificationType.FLOAT;
            case LONG:
                return VerificationType.LONG;
            case DOUBLE:
                return VerificationType.DOUBLE;
            case REFERENCE:
                return registry.getObjectType(typeDescriptor);
            case WORD:
                return VerificationType.WORD;
        }
        throw ProgramError.unexpected("unexpected");
    }

    public static VerificationType[] readVerificationTypes(ClassfileStream classfileStream, VerificationRegistry registry, int length) {
        final VerificationType[] array = new VerificationType[length];
        for (int n = 0; n < length; n++) {
            array[n] = readVerificationType(classfileStream, registry);
        }
        return array;
    }

    /**
     * @param registry
     *                used to create specific {@linkplain ReferenceType reference types}. If {@code null}, then
     *                {@link #OBJECT} is return for any object type and {@link #UNINITIALIZED} is returned for any
     *                uninitialized type
     */
    public static VerificationType readVerificationType(ClassfileStream classfileStream, VerificationRegistry registry) {
        final int tag = classfileStream.readUnsigned1();
        switch (tag) {
            case ITEM_Top:
                return TOP;
            case ITEM_Integer:
                return INTEGER;
            case ITEM_Float:
                return FLOAT;
            case ITEM_Double:
                return DOUBLE;
            case ITEM_Long:
                return LONG;
            case ITEM_Null:
                return NULL;
            case ITEM_UninitializedThis:
                return UNINITIALIZED_THIS;
            case ITEM_Object: {
                final int constantPoolIndex = classfileStream.readUnsigned2();
                if (registry == null) {
                    return OBJECT;
                }
                return registry.getObjectType(registry.constantPool().classAt(constantPoolIndex, "object type descriptor").typeDescriptor());
            }
            case ITEM_Uninitialized: {
                final int constantPoolIndex = classfileStream.readUnsigned2();
                if (registry == null) {
                    return UNINITIALIZED;
                }
                return registry.getUninitializedNewType(constantPoolIndex);
            }
            default:
                throw classFormatError("Invalid verification type tag: " + tag);
        }
    }

    /**
     * All verification types are equal only iff they are identical. This is why all non-singleton types must be created
     * from a {@linkplain VerificationRegistry registry}.
     */
    @Override
    public final boolean equals(Object other) {
        return other == this;
    }

    @Override
    public abstract String toString();

    /**
     * Gets the tag denoting this type in a class file (in a {@link StackMapTable}.
     *
     * @return {@link #ITEM_Double}, {@link #ITEM_Float}, {@link #ITEM_Integer}, {@link #ITEM_Long},
     *         {@link #ITEM_Null}, {@link #ITEM_Object}, {@link #ITEM_Top}, {@link #ITEM_UninitializedThis},
     *         {@link #ITEM_Uninitialized} or -1 if this type is not valid in a classfile
     */
    public abstract int classfileTag();

    /**
     * Writes this verification type to a stream in class file format.
     *
     * @param stream
     * @param constantPoolEditor
     * @throws IllegalArgumentException if {@code classfileTag() == -1}
     */
    public final void write(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        final int classfileTag = classfileTag();
        if (classfileTag == -1) {
            throw new IllegalArgumentException("verification type " + this + " cannot be represented in a class file");
        }
        stream.writeByte(classfileTag);
        writeInfo(stream, constantPoolEditor);
    }

    /**
     * Writes the info after the {@linkplain #classfileTag() tag} for this verification type to a stream in class file format.
     * This method writes no output to {@code stream} if {@code classfileTag() == -1} or there is no info apart from the tag.
     *
     * @param stream
     * @param constantPoolEditor
     */
    protected void writeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
    }
}
