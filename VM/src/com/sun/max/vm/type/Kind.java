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
package com.sun.max.vm.type;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.classfile.ErrorContext.*;

import com.sun.cri.ci.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.JavaTypeDescriptor.*;
import com.sun.max.vm.value.*;

/**
 * Groups/categories/subsets/whatever of types with common relevant implementation traits wrt.:
 * size, GC, stack representation, etc.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Kind<Value_Type extends Value<Value_Type>> {

    public final KindEnum asEnum;
    public final Utf8Constant name;
    public final Class javaClass;
    public final Class javaArrayClass;
    public final Class<Value_Type> valueClass;
    public final Kind stackKind;
    @INSPECTED
    public final char character;
    public final Class boxedClass;
    public final TypeDescriptor typeDescriptor;
    public final WordWidth width;
    public final boolean isCategory1;
    public final int stackSlots;
    public final boolean isWord;
    public final boolean isReference;
    public final CiKind ciKind;

    @HOSTED_ONLY
    protected Kind(KindEnum kindEnum, String name, Class javaClass, Class javaArrayClass, Class<Value_Type> valueClass, char character,
                   final Class boxedClass, TypeDescriptor typeDescriptor, WordWidth width) {
        this.asEnum = kindEnum;
        kindEnum.setKind(this);
        this.name = SymbolTable.makeSymbol(name);
        this.javaClass = javaClass;
        this.javaArrayClass = javaArrayClass;
        this.valueClass = valueClass;
        this.character = character;
        this.boxedClass = boxedClass;
        this.typeDescriptor = typeDescriptor;
        this.width = width;
        this.stackKind = "ZBCS".indexOf(character) != -1 ? INT : this;
        this.isCategory1 = "JD".indexOf(character) == -1;
        this.stackSlots = !isCategory1 ? 2 : (character == 'V' ? 0 : 1);
        this.isWord = kindEnum == KindEnum.WORD;
        this.isReference = kindEnum == KindEnum.REFERENCE;
        if (typeDescriptor instanceof AtomicTypeDescriptor) {
            ((AtomicTypeDescriptor) typeDescriptor).setKind(this);
        }
        String ciKindName = isReference ? "Object" : Strings.capitalizeFirst(name, true);
        this.ciKind = CiKind.valueOf(ciKindName);
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    @Override
    public final String toString() {
        return name.toString();
    }

    public static Kind<?> fromJava(Class type) {
        if (MaxineVM.isHosted()) {
            if (type.isPrimitive()) {
                for (Kind kind : Kind.PRIMITIVE_JAVA_CLASSES) {
                    if (kind.javaClass == type) {
                        return kind;
                    }
                }
            }
            if (Word.class.isAssignableFrom(type)) {
                return Kind.WORD;
            }
            return Kind.REFERENCE;
        }
        return ClassActor.fromJava(type).kind;
    }

    public static Kind<?> fromBoxedClass(Class type) {
        assert !type.isPrimitive();
        if (MaxineVM.isHosted()) {
            for (Kind kind : Kind.PRIMITIVE_JAVA_CLASSES) {
                if (kind.boxedClass == type) {
                    return kind;
                }
            }

            if (Word.class.isAssignableFrom(type)) {
                return Kind.WORD;
            }
            return Kind.REFERENCE;
        }
        return ClassActor.fromJava(type).kind;
    }

    /**
     * Determines if this is a primitive kind other than {@code void}.
     */
    public final boolean isPrimitiveValue() {
        return JavaTypeDescriptor.isPrimitive(typeDescriptor);
    }

    public final boolean isExtendedPrimitiveValue() {
        return asEnum != KindEnum.REFERENCE && asEnum != KindEnum.VOID;
    }

    public final boolean isPrimitiveOfSameSizeAs(Kind kind) {
        return kind == this || isPrimitiveValue() && kind.isPrimitiveValue() && width.numberOfBytes == kind.width.numberOfBytes;
    }

    public abstract ArrayLayout arrayLayout(LayoutScheme layoutScheme);

    public ArrayClassActor arrayClassActor() {
        throw new ClassCastException("there is no canonical array class for a non-primitive Java type");
    }

    public Value_Type readValue(Reference reference, int offset) {
        throw new ClassCastException();
    }

    public void writeValue(Object object, int offset, Value_Type value) {
        throw new ClassCastException();
    }

    public void writeErasedValue(Object object, int offset, Value value) {
        final Value_Type v = valueClass.cast(value);
        writeValue(object, offset, v);
    }

    public Value_Type getValue(Object array, int index) {
        throw new ClassCastException();
    }

    public void setValue(Object array, int index, Value_Type value) {
        throw new ClassCastException();
    }

    public void setErasedValue(Object array, int index, Value value) {
        final Value_Type v = valueClass.cast(value);
        setValue(array, index, v);
    }

    public Value_Type convert(Value value) {
        throw new IllegalArgumentException();
    }

    /**
     * Converts a given Java boxed value to the equivalent boxed {@link Value}.
     *
     * @param boxedJavaValue
     * @throws IllegalArgumentException if the type of {@code boxedJavaValue} is not equivalent with this kind
     */
    public abstract Value asValue(Object boxedJavaValue) throws IllegalArgumentException;

    public Value toValue(Object boxedJavaValue) {
        try {
            return asValue(boxedJavaValue);
        } catch (IllegalArgumentException illegalArgumentException) {
            return convert(Value.fromBoxedJavaValue(boxedJavaValue));
        }
    }

    public abstract Value_Type zeroValue();

    public static final Kind<VoidValue> VOID = new Kind<VoidValue>(KindEnum.VOID, "void", void.class, null, VoidValue.class, 'V', Void.class, JavaTypeDescriptor.VOID, null) {
        @Override
        public VoidValue convert(Value value) {
            final Kind kind = this;
            if (kind != value.kind()) {
                return super.convert(value);
            }
            return VoidValue.VOID;
        }

        @Override
        public Value asValue(Object boxedJavaValue) {
            if (boxedJavaValue == null || void.class.isInstance(boxedJavaValue)) {
                return VoidValue.VOID;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            throw new ClassCastException("there is no array layout for void");
        }

        @Override
        public VoidValue zeroValue() {
            return VoidValue.VOID;
        }
    };

    public static final Kind<IntValue> INT = new Kind<IntValue>(KindEnum.INT, "int", int.class, int[].class,
                                                                IntValue.class, 'I', Integer.class, JavaTypeDescriptor.INT,
                                                                WordWidth.BITS_32) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.INT_ARRAY;
        }

        @Override
        public IntValue readValue(Reference reference, int offset) {
            return IntValue.from(reference.readInt(offset));
        }

        @Override
        public void writeValue(Object object, int offset, IntValue value) {
            TupleAccess.writeInt(object, offset, value.asInt());
        }

        @Override
        public IntValue getValue(Object array, int index) {
            return IntValue.from(ArrayAccess.getInt(array, index));
        }

        @Override
        public void setValue(Object array, int index, IntValue value) {
            ArrayAccess.setInt(array, index, value.asInt());
        }

        @Override
        public IntValue convert(Value value) {
            return IntValue.from(value.toInt());
        }

        @Override
        public IntValue asValue(Object boxedJavaValue) {
            try {
                final Integer specificBox = (Integer) boxedJavaValue;
                return IntValue.from(specificBox.intValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public IntValue zeroValue() {
            return IntValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.intArrayLayout;
        }
    };
    public static final Kind<ByteValue> BYTE = new Kind<ByteValue>(KindEnum.BYTE, "byte", byte.class, byte[].class,
                                                                   ByteValue.class, 'B', Byte.class, JavaTypeDescriptor.BYTE,
                                                                   WordWidth.BITS_8) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.BYTE_ARRAY;
        }

        @Override
        public ByteValue readValue(Reference reference, int offset) {
            return ByteValue.from(reference.readByte(offset));
        }

        @Override
        public void writeValue(Object object, int offset, ByteValue value) {
            TupleAccess.writeByte(object, offset, value.asByte());
        }

        @Override
        public ByteValue getValue(Object array, int index) {
            return ByteValue.from(ArrayAccess.getByte(array, index));
        }

        @Override
        public void setValue(Object array, int index, ByteValue value) {
            ArrayAccess.setByte(array, index, value.asByte());
        }

        @Override
        public ByteValue convert(Value value) {
            return ByteValue.from(value.toByte());
        }

        @Override
        public ByteValue asValue(Object boxedJavaValue) {
            try {
                final Byte specificBox = (Byte) boxedJavaValue;
                return ByteValue.from(specificBox.byteValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public ByteValue zeroValue() {
            return ByteValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.byteArrayLayout;
        }
    };

    public static final Kind<BooleanValue> BOOLEAN = new Kind<BooleanValue>(KindEnum.BOOLEAN, "boolean", boolean.class, boolean[].class,
                                                                            BooleanValue.class, 'Z', Boolean.class, JavaTypeDescriptor.BOOLEAN,
                                                                            WordWidth.BITS_8) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.BOOLEAN_ARRAY;
        }

        @Override
        public BooleanValue readValue(Reference reference, int offset) {
            return BooleanValue.from(reference.readBoolean(offset));
        }

        @Override
        public void writeValue(Object object, int offset, BooleanValue value) {
            TupleAccess.writeBoolean(object, offset, value.asBoolean());
        }

        @Override
        public BooleanValue getValue(Object array, int index) {
            return BooleanValue.from(ArrayAccess.getBoolean(array, index));
        }

        @Override
        public void setValue(Object array, int index, BooleanValue value) {
            ArrayAccess.setBoolean(array, index, value.asBoolean());
        }

        @Override
        public BooleanValue convert(Value value) {
            return BooleanValue.from(value.toBoolean());
        }

        @Override
        public BooleanValue asValue(Object boxedJavaValue) {
            try {
                final Boolean specificBox = (Boolean) boxedJavaValue;
                return BooleanValue.from(specificBox.booleanValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public BooleanValue zeroValue() {
            return BooleanValue.FALSE;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.booleanArrayLayout;
        }
    };

    public static final Kind<ShortValue> SHORT = new Kind<ShortValue>(KindEnum.SHORT, "short", short.class, short[].class,
                                                                      ShortValue.class, 'S', Short.class, JavaTypeDescriptor.SHORT,
                                                                      WordWidth.BITS_16) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.SHORT_ARRAY;
        }

        @Override
        public ShortValue readValue(Reference reference, int offset) {
            return ShortValue.from(reference.readShort(offset));
        }

        @Override
        public void writeValue(Object object, int offset, ShortValue value) {
            TupleAccess.writeShort(object, offset, value.asShort());
        }

        @Override
        public ShortValue getValue(Object array, int index) {
            return ShortValue.from(ArrayAccess.getShort(array, index));
        }

        @Override
        public void setValue(Object array, int index, ShortValue value) {
            ArrayAccess.setShort(array, index, value.asShort());
        }

        @Override
        public ShortValue convert(Value value) {
            return ShortValue.from(value.toShort());
        }

        @Override
        public ShortValue asValue(Object boxedJavaValue) {
            try {
                final Short specificBox = (Short) boxedJavaValue;
                return ShortValue.from(specificBox.shortValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public ShortValue zeroValue() {
            return ShortValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.shortArrayLayout;
        }
    };

    public static final Kind<CharValue> CHAR = new Kind<CharValue>(KindEnum.CHAR, "char", char.class, char[].class,
                                                                   CharValue.class, 'C', Character.class, JavaTypeDescriptor.CHAR,
                                                                   WordWidth.BITS_16) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.CHAR_ARRAY;
        }

        @Override
        public CharValue readValue(Reference reference, int offset) {
            return CharValue.from(reference.readChar(offset));
        }

        @Override
        public void writeValue(Object object, int offset, CharValue value) {
            TupleAccess.writeChar(object, offset, value.asChar());
        }

        @Override
        public CharValue getValue(Object array, int index) {
            return CharValue.from(ArrayAccess.getChar(array, index));
        }

        @Override
        public void setValue(Object array, int index, CharValue value) {
            ArrayAccess.setChar(array, index, value.asChar());
        }

        @Override
        public CharValue convert(Value value) {
            return CharValue.from(value.toChar());
        }

        @Override
        public CharValue asValue(Object boxedJavaValue) {
            try {
                final Character specificBox = (Character) boxedJavaValue;
                return CharValue.from(specificBox.charValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public CharValue zeroValue() {
            return CharValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.charArrayLayout;
        }
    };

    public static final Kind<FloatValue> FLOAT = new Kind<FloatValue>(KindEnum.FLOAT, "float", float.class, float[].class,
                                                                      FloatValue.class, 'F', Float.class, JavaTypeDescriptor.FLOAT,
                                                                      WordWidth.BITS_32) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.FLOAT_ARRAY;
        }

        @Override
        public FloatValue readValue(Reference reference, int offset) {
            return FloatValue.from(reference.readFloat(offset));
        }

        @Override
        public void writeValue(Object object, int offset, FloatValue value) {
            TupleAccess.writeFloat(object, offset, value.asFloat());
        }

        @Override
        public FloatValue getValue(Object array, int index) {
            return FloatValue.from(ArrayAccess.getFloat(array, index));
        }

        @Override
        public void setValue(Object array, int index, FloatValue value) {
            ArrayAccess.setFloat(array, index, value.asFloat());
        }

        @Override
        public FloatValue convert(Value value) {
            return FloatValue.from(value.toFloat());
        }

        @Override
        public FloatValue asValue(Object boxedJavaValue) {
            try {
                final Float specificBox = (Float) boxedJavaValue;
                return FloatValue.from(specificBox.floatValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public FloatValue zeroValue() {
            return FloatValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.floatArrayLayout;
        }
    };

    public static final Kind<LongValue> LONG = new Kind<LongValue>(KindEnum.LONG, "long", long.class, long[].class,
                                                                   LongValue.class, 'J', Long.class, JavaTypeDescriptor.LONG,
                                                                   WordWidth.BITS_64) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.LONG_ARRAY;
        }

        @Override
        public LongValue readValue(Reference reference, int offset) {
            return LongValue.from(reference.readLong(offset));
        }

        @Override
        public void writeValue(Object object, int offset, LongValue value) {
            TupleAccess.writeLong(object, offset, value.asLong());
        }

        @Override
        public LongValue getValue(Object array, int index) {
            return LongValue.from(ArrayAccess.getLong(array, index));
        }

        @Override
        public void setValue(Object array, int index, LongValue value) {
            ArrayAccess.setLong(array, index, value.asLong());
        }

        @Override
        public LongValue convert(Value value) {
            return LongValue.from(value.toLong());
        }

        @Override
        public LongValue asValue(Object boxedJavaValue) {
            try {
                final Long specificBox = (Long) boxedJavaValue;
                return LongValue.from(specificBox.longValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public LongValue zeroValue() {
            return LongValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.longArrayLayout;
        }
    };

    public static final Kind<DoubleValue> DOUBLE = new Kind<DoubleValue>(KindEnum.DOUBLE, "double", double.class, double[].class,
                                                                         DoubleValue.class, 'D', Double.class, JavaTypeDescriptor.DOUBLE,
                                                                         WordWidth.BITS_64) {
        @Override
        public ArrayClassActor arrayClassActor() {
            return ClassRegistry.DOUBLE_ARRAY;
        }

        @Override
        public DoubleValue readValue(Reference reference, int offset) {
            return DoubleValue.from(reference.readDouble(offset));
        }

        @Override
        public void writeValue(Object object, int offset, DoubleValue value) {
            TupleAccess.writeDouble(object, offset, value.asDouble());
        }

        @Override
        public DoubleValue getValue(Object array, int index) {
            return DoubleValue.from(ArrayAccess.getDouble(array, index));
        }

        @Override
        public void setValue(Object array, int index, DoubleValue value) {
            ArrayAccess.setDouble(array, index, value.asDouble());
        }

        @Override
        public DoubleValue convert(Value value) {
            return DoubleValue.from(value.toDouble());
        }

        @Override
        public DoubleValue asValue(Object boxedJavaValue) {
            try {
                final Double specificBox = (Double) boxedJavaValue;
                return DoubleValue.from(specificBox.doubleValue());
            } catch (ClassCastException e) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public DoubleValue zeroValue() {
            return DoubleValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.doubleArrayLayout;
        }
    };

    public static final Kind<WordValue> WORD = new Kind<WordValue>(KindEnum.WORD, "Word", Word.class, Word[].class,
                                                                   WordValue.class, 'W', Word.class, JavaTypeDescriptor.WORD,
                                                                   Word.widthValue()) {
        @Override
        public WordValue readValue(Reference reference, int offset) {
            return new WordValue(reference.readWord(offset));
        }

        @Override
        public void writeValue(Object object, int offset, WordValue value) {
            TupleAccess.writeWord(object, offset, value.asWord());
        }

        @Override
        public WordValue getValue(Object array, int index) {
            return new WordValue(ArrayAccess.getWord(array, index));
        }

        @Override
        public void setValue(Object array, int index, WordValue value) {
            ArrayAccess.setWord(array, index, value.asWord());
        }

        @Override
        public WordValue convert(Value value) {
            return new WordValue(value.toWord());
        }

        @Override
        public WordValue asValue(Object boxedJavaValue) {
            if (boxedJavaValue instanceof WordValue) {
                return (WordValue) boxedJavaValue;
            }
            if (isHosted()) {
                if (boxedJavaValue instanceof Boxed) {
                    final Boxed box = (Boxed) boxedJavaValue;
                    return new WordValue(Address.fromLong(box.value()));
                }
            }
            if (boxedJavaValue == null) {
                // we are lenient here to allow default static initializers to function
                return WordValue.ZERO;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public WordValue zeroValue() {
            return WordValue.ZERO;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.wordArrayLayout;
        }
    };

    public static final Kind<ReferenceValue> REFERENCE = new Kind<ReferenceValue>(KindEnum.REFERENCE, "Reference", Object.class, Object[].class,
                                                                                  ReferenceValue.class, 'R', Object.class, JavaTypeDescriptor.REFERENCE,
                                                                                  Word.widthValue()) {
        @Override
        public ReferenceValue readValue(Reference reference, int offset) {
            return ReferenceValue.from(reference.readReference(offset).toJava());
        }

        @Override
        public void writeValue(Object object, int offset, ReferenceValue value) {
            TupleAccess.writeObject(object, offset, value.asObject());
        }

        @Override
        public ReferenceValue getValue(Object array, int index) {
            return ReferenceValue.from(ArrayAccess.getObject(array, index));
        }

        @Override
        public void setValue(Object array, int index, ReferenceValue value) {
            ArrayAccess.setObject(array, index, value.asObject());
        }

        @Override
        public ReferenceValue convert(Value value) {
            final Kind kind = this;
            if (kind != value.kind()) {
                return super.convert(value);
            }
            return (ReferenceValue) value;
        }

        @Override
        public ReferenceValue asValue(Object boxedJavaValue) {
            return ReferenceValue.from(boxedJavaValue);
        }

        @Override
        public ReferenceValue zeroValue() {
            return ReferenceValue.NULL;
        }

        @Override
        public final ArrayLayout arrayLayout(LayoutScheme layoutScheme) {
            return layoutScheme.referenceArrayLayout;
        }
    };

    public static final Kind[] PRIMITIVE_VALUES = {BYTE, BOOLEAN, SHORT, CHAR, INT, FLOAT, LONG, DOUBLE};

    public static final Kind[] PRIMITIVE_JAVA_CLASSES = Utils.concat(PRIMITIVE_VALUES, VOID);

    public static final Kind[] EXTENDED_PRIMITIVE_VALUES = Utils.concat(PRIMITIVE_VALUES, WORD);

    public static final Kind[] VALUES = Utils.concat(EXTENDED_PRIMITIVE_VALUES, REFERENCE);

    public static final Kind[] ALL = Utils.concat(VALUES, VOID);

    public static final Kind[] NONE = {};

    public static Kind fromNewArrayTag(int tag) {
        switch (tag) {
            case 4:
                return Kind.BOOLEAN;
            case 5:
                return Kind.CHAR;
            case 6:
                return Kind.FLOAT;
            case 7:
                return Kind.DOUBLE;
            case 8:
                return Kind.BYTE;
            case 9:
                return Kind.SHORT;
            case 10:
                return Kind.INT;
            case 11:
                return Kind.LONG;
            default:
                throw classFormatError("Invalid newarray type tag (" + tag + ")");
        }
    }

    public static Kind fromCharacter(char character) {
        switch (character) {
            case 'Z':
                return Kind.BOOLEAN;
            case 'C':
                return Kind.CHAR;
            case 'F':
                return Kind.FLOAT;
            case 'D':
                return Kind.DOUBLE;
            case 'B':
                return Kind.BYTE;
            case 'S':
                return Kind.SHORT;
            case 'I':
                return Kind.INT;
            case 'J':
                return Kind.LONG;
            case 'R':
                return Kind.REFERENCE;
            case 'W':
                return Kind.WORD;
            case 'V':
                return Kind.VOID;
            default:
                return null;
        }
    }

    /**
     * Unboxes a given object to a boolean.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} is not an instance of {@link Boolean}
     */
    public static boolean unboxBoolean(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Boolean) {
            final Boolean box = (Boolean) boxedJavaValue;
            return box.booleanValue();
        }
        throw new IllegalArgumentException("expected a boxed boolean, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to a byte.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} is not an instanceof {@link Byte}
     */
    public static byte unboxByte(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return box.byteValue();
        }
        throw new IllegalArgumentException("expected a boxed byte, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to a char, doing a primitive widening conversion if necessary.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} cannot be unboxed to a char
     *
     * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#23435">2.6.2 Widening Primitive Conversions</a>
     */
    public static char unboxChar(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return box.charValue();
        }
        throw new IllegalArgumentException("expected a boxed char, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to a short, doing a primitive widening conversion if necessary.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} cannot be unboxed to a short
     *
     * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#23435">2.6.2 Widening Primitive Conversions</a>
     */
    public static short unboxShort(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return box.shortValue();
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return box.shortValue();
        }
        throw new IllegalArgumentException("expected a boxed short, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to an int, doing a primitive widening conversion if necessary.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} cannot be unboxed to an int
     *
     * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#23435">2.6.2 Widening Primitive Conversions</a>
     */
    public static int unboxInt(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return box.intValue();
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return box.intValue();
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return box.charValue();
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return box.intValue();
        }
        throw new IllegalArgumentException("expected a boxed int, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to a float, doing a primitive widening conversion if necessary.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} cannot be unboxed to a float
     *
     * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#23435">2.6.2 Widening Primitive Conversions</a>
     */
    public static float unboxFloat(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Float) {
            final Float box = (Float) boxedJavaValue;
            return box.floatValue();
        }
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return box.floatValue();
        }
        if (boxedJavaValue instanceof Long) {
            final Long box = (Long) boxedJavaValue;
            return box.floatValue();
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return box.floatValue();
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return box.charValue();
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return box.floatValue();
        }
        throw new IllegalArgumentException("expected a boxed float, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to a long, doing a primitive widening conversion if necessary.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} cannot be unboxed to a long
     *
     * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#23435">2.6.2 Widening Primitive Conversions</a>
     */
    public static long unboxLong(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Long) {
            final Long box = (Long) boxedJavaValue;
            return box.longValue();
        }
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return box.longValue();
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return box.longValue();
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return box.charValue();
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return box.longValue();
        }
        throw new IllegalArgumentException("expected a boxed long, got " + boxedJavaValue.getClass().getName());
    }

    /**
     * Unboxes a given object to a double, doing a primitive widening conversion if necessary.
     *
     * @throws IllegalArgumentException if {@code boxedJavaValue} cannot be unboxed to a double
     *
     * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#23435">2.6.2 Widening Primitive Conversions</a>
     */
    public static double unboxDouble(Object boxedJavaValue) {
        if (boxedJavaValue instanceof Double) {
            final Double box = (Double) boxedJavaValue;
            return box.doubleValue();
        }
        if (boxedJavaValue instanceof Float) {
            final Float box = (Float) boxedJavaValue;
            return box.doubleValue();
        }
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return box.doubleValue();
        }
        if (boxedJavaValue instanceof Long) {
            final Long box = (Long) boxedJavaValue;
            return box.doubleValue();
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return box.doubleValue();
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return box.charValue();
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return box.doubleValue();
        }
        throw new IllegalArgumentException("expected a boxed double, got " + boxedJavaValue.getClass().getName());
    }
}
