/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ci;

import static com.sun.cri.ci.CiKind.Flags.*;
import static com.sun.cri.ci.CiKind.Slots.*;

import com.sun.cri.bytecode.Bytecodes;

/**
 * Denotes the basic kinds of types in CRI, including the all the Java primitive types,
 * for example, {@link CiKind#Int} for {@code int} and {@link CiKind#Object}
 * for all object types. {@link CiKind#Jsr} and {@link CiKind#Word} are special cases.
 * A kind has a single character short name, a Java name, a JNI name,
 * the number of (abstract) stack slots the value occupies, and a set of flags
 * further describing its behavior.
 */
public enum CiKind {
    Boolean('z', "boolean", "jboolean", SLOTS_1,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE | STACK_INT),
    Byte   ('b', "byte",    "jbyte",    SLOTS_1,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE | STACK_INT),
    Short  ('s', "short",   "jshort",   SLOTS_1,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE | STACK_INT),
    Char   ('c', "char",    "jchar",    SLOTS_1,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE | STACK_INT),
    Int    ('i', "int",     "jint",     SLOTS_1,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE | STACK_INT),
    Float  ('f', "float",   "jfloat",   SLOTS_1,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE),
    Long   ('l', "long",    "jlong",    SLOTS_2,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE),
    Double ('d', "double",  "jdouble",  SLOTS_2,  FIELD_TYPE | RETURN_TYPE | PRIMITIVE),
    Object ('a', "Object",  "jobject",  SLOTS_1,  FIELD_TYPE | RETURN_TYPE),
    /** Denotes a machine word type used in the extended bytecodes. */
    Word   ('w', "Word",    "jword",    SLOTS_1,  FIELD_TYPE | RETURN_TYPE),
    Void   ('v', "void",    "void",     SLOTS_0,  RETURN_TYPE),
    /** Denote a bytecode address in a {@code JSR} bytecode. */
    Jsr    ('r', "jsr",     null,       SLOTS_1,  0),
    /** The non-type. */
    Illegal('-', "illegal", null,       -1,       0);

    public static final CiKind[] VALUES = values();
    public static final CiKind[] JAVA_VALUES = new CiKind[] {CiKind.Boolean, CiKind.Byte, CiKind.Short, CiKind.Char, CiKind.Int, CiKind.Float, CiKind.Long, CiKind.Double, CiKind.Object};

    CiKind(char ch, String name, String jniName, int jvmSlots, int flags) {
        this.typeChar = ch;
        this.javaName = name;
        this.jniName = jniName;
        this.jvmSlots = jvmSlots;
        this.flags = flags;
    }

    static final class Slots {
        public static final int SLOTS_0 = 0;
        public static final int SLOTS_1 = 1;
        public static final int SLOTS_2 = 2;
    }

    static class Flags {
        /**
         * Can be an object field type.
         */
        public static final int FIELD_TYPE  = 0x0001;
        /**
         * Can be result type of a method.
         */
        public static final int RETURN_TYPE = 0x0002;
        /**
         * Behaves as an integer when on Java evaluation stack.
         */
        public static final int STACK_INT   = 0x0004;
        /**
         * Represents a Java primitive type.
         */
        public static final int PRIMITIVE   = 0x0008;
    }

    /**
     * The flags for this kind.
     */
    private final int flags;

    /**
     * The name of the kind as a single character.
     */
    public final char typeChar;

    /**
     * The name of this kind which will also be it Java programming language name if
     * it is {@linkplain #isPrimitive() primitive} or {@code void}.
     */
    public final String javaName;

    /**
     * The JNI name of this kind; {@code null} if this kind is not a valid JNI type.
     */
    public final String jniName;

    /**
     * The size of this kind in terms of abstract JVM words. Note that this may
     * differ from the actual size of this type in its machine representation.
     */
    public final int jvmSlots;

    /**
     * Checks whether this kind is valid as the type of a field.
     * @return {@code true} if this kind is valid as the type of a Java field
     */
    public boolean isValidFieldType() {
        return (flags & FIELD_TYPE) != 0;
    }

    /**
     * Checks whether this kind is valid as the return type of a method.
     * @return {@code true} if this kind is valid as the return type of a Java method
     */
    public boolean isValidReturnType() {
        return (flags & RETURN_TYPE) != 0;
    }

    /**
     * Checks whether this type is valid as an {@code int} on the Java operand stack.
     * @return {@code true} if this type is represented by an {@code int} on the operand stack
     */
    public boolean isInt() {
        return (flags & STACK_INT) != 0;
    }

    /**
     * Checks whether this type is a Java primitive type.
     * @return {@code true} if this is {@link #Boolean}, {@link #Byte}, {@link #Char}, {@link #Short},
     *                                 {@link #Int}, {@link #Long}, {@link #Float} or {@link #Double}.
     */
    public boolean isPrimitive() {
        return (flags & PRIMITIVE) != 0;
    }

    /**
     * Gets the kind that represents this kind when on the Java operand stack.
     * @return the kind used on the operand stack
     */
    public CiKind stackKind() {
        if (isInt()) {
            return Int;
        }
        return this;
    }

    /**
     * Gets the size of this kind in terms of the number of Java slots.
     * @return the size of the kind in slots
     */
    public int sizeInSlots() {
        return jvmSlots;
    }

    /**
     * Gets the size of this kind in bytes.
     *
     * @param wordSize the size of a word in bytes
     * @return the size of this kind in bytes
     */
    public int sizeInBytes(int wordSize) {
        // Checkstyle: stop
        switch (this) {
            case Boolean: return 1;
            case Byte: return 1;
            case Char: return 2;
            case Short: return 2;
            case Int: return 4;
            case Long: return 8;
            case Float: return 4;
            case Double: return 8;
            case Object: return wordSize;
            case Jsr: return 4;
            case Word: return wordSize;
            default: return 0;
        }
        // Checkstyle: resume
    }

    /**
     * Gets the kind of array elements for the array type code that appears
     * in a {@link Bytecodes#NEWARRAY} bytecode.
     * @param code the array type code
     * @return the kind from the array type code
     */
    public static CiKind fromArrayTypeCode(int code) {
        // Checkstyle: stop
        switch (code) {
            case 4: return Boolean;
            case 5: return Char;
            case 6: return Float;
            case 7: return Double;
            case 8: return Byte;
            case 9: return Short;
            case 10: return Int;
            case 11: return Long;
        }
        // Checkstyle: resume
        throw new IllegalArgumentException("unknown array type code: " + code);
    }

    public static CiKind fromTypeString(String typeString) {
        assert typeString.length() > 0;
        final char first = typeString.charAt(0);
        if (first == '[' || first == 'L') {
            return CiKind.Object;
        }
        return CiKind.fromPrimitiveOrVoidTypeChar(first);
    }

    /**
     * Gets the kind from the character describing a primitive or void.
     * @param ch the character
     * @return the kind
     */
    public static CiKind fromPrimitiveOrVoidTypeChar(char ch) {
        // Checkstyle: stop
        switch (ch) {
            case 'Z': return Boolean;
            case 'C': return Char;
            case 'F': return Float;
            case 'D': return Double;
            case 'B': return Byte;
            case 'S': return Short;
            case 'I': return Int;
            case 'J': return Long;
            case 'V': return Void;
        }
        // Checkstyle: resume
        throw new IllegalArgumentException("unknown primitive or void type character: " + ch);
    }

    /**
     * Gets the kind from a character.
     * @param ch the character corresponding to the {@link #typeChar} of a {@link CiKind} constant.
     * @return the kind
     */
    public static CiKind fromTypeChar(char ch) {
        // Checkstyle: stop
        switch (ch) {
            case 'z': return Boolean;
            case 'c': return Char;
            case 'f': return Float;
            case 'd': return Double;
            case 'b': return Byte;
            case 's': return Short;
            case 'i': return Int;
            case 'l': return Long;
            case 'a': return Object;
            case 'w': return Word;
            case 'v': return Void;
            case 'r': return Jsr;
        }
        // Checkstyle: resume
        throw new IllegalArgumentException("unknown type character: " + ch);
    }

    /**
     * Gets the array class which has elements of this kind. This method
     * is only defined for primtive types.
     * @return the Java class which represents arrays of this kind
     */
    public Class<?> primitiveArrayClass() {
        // Checkstyle: stop
        switch (this) {
            case Boolean: return boolean[].class;
            case Char:    return char[].class;
            case Float:   return float[].class;
            case Double:  return double[].class;
            case Byte:    return byte[].class;
            case Short:   return short[].class;
            case Int:     return int[].class;
            case Long:    return long[].class;
        }
        // Checkstyle: resume
        throw new IllegalArgumentException("not a primitive kind");
    }

    public static CiKind fromJavaClass(Class<?> c) {
        // Checkstyle: stop
        if (c == java.lang.Void.TYPE) return Void;
        if (c == java.lang.Integer.TYPE) return Int;
        if (c == java.lang.Byte.TYPE) return Byte;
        if (c == java.lang.Character.TYPE) return Char;
        if (c == java.lang.Double.TYPE) return Double;
        if (c == java.lang.Float.TYPE) return Float;
        if (c == java.lang.Long.TYPE) return Long;
        if (c == java.lang.Short.TYPE) return Short;
        if (c == java.lang.Boolean.TYPE) return Boolean;
        return CiKind.Object;
        // Checkstyle: resume
    }

    public Class< ? > toJavaClass() {
        // Checkstyle: stop
        switch(this) {
            case Void:      return java.lang.Void.TYPE;
            case Long:      return java.lang.Long.TYPE;
            case Int:       return java.lang.Integer.TYPE;
            case Byte:      return java.lang.Byte.TYPE;
            case Char:      return java.lang.Character.TYPE;
            case Double:    return java.lang.Double.TYPE;
            case Float:     return java.lang.Float.TYPE;
            case Short:     return java.lang.Short.TYPE;
            case Boolean:   return java.lang.Boolean.TYPE;
            default:        return null;
        }
        // Checkstyle: resume
    }

    /**
     * Checks whether this value type is void.
     * @return {@code true} if this type is void
     */
    public final boolean isVoid() {
        return this == CiKind.Void;
    }

    /**
     * Checks whether this value type is long.
     * @return {@code true} if this type is long
     */
    public final boolean isLong() {
        return this == CiKind.Long;
    }

    /**
     * Checks whether this value type is float.
     * @return {@code true} if this type is float
     */
    public final boolean isFloat() {
        return this == CiKind.Float;
    }

    /**
     * Checks whether this value type is double.
     * @return {@code true} if this type is double
     */
    public final boolean isDouble() {
        return this == CiKind.Double;
    }

    /**
     * Checks whether this value type is float or double.
     * @return {@code true} if this type is float or double
     */
    public final boolean isFloatOrDouble() {
        return this == CiKind.Double || this == CiKind.Float;
    }

   /**
     * Checks whether this value type is an object type.
     * @return {@code true} if this type is an object
     */
    public final boolean isObject() {
        return this == CiKind.Object;
    }

    /**
     * Checks whether this value type is a word type.
     * @return {@code true} if this type is a word
     */
    public final boolean isWord() {
        return this == CiKind.Word;
    }

    /**
     * Checks whether this value type is an address type.
     * @return {@code true} if this type is an address
     */
    public boolean isJsr() {
        return this == CiKind.Jsr;
    }

    /**
     * Checks whether this type is represented by a single word.
     * @return true if this type is represented by a single word
     */
    public boolean isSingleWord() {
        return sizeInSlots() == 1;
    }

    /**
     * Checks whether this type is represented by a double word (two words).
     * @return {@code true} if this type is represented by two words
     */
    public boolean isDoubleWord() {
        return sizeInSlots() == 2;
    }

    /**
     * Performs the meet operation on this type and another type.
     * @param other the other value type
     * @return the result of the meet operation for these two types
     */
    public final CiKind meet(CiKind other) {
        if (other.stackKind() == this.stackKind()) {
            return this.stackKind();
        }
        return CiKind.Illegal;
    }

    /**
     * Converts this value type to a string.
     */
    @Override
    public String toString() {
        return javaName;
    }

    /**
     * Gets a formatted string for a given value of this kind.
     *
     * @param value a value of this kind
     * @return a formatted string for {@code value} based on this kind
     */
    public String format(Object value) {
        StringBuilder sb = new StringBuilder();
        if (isWord()) {
            sb.append("0x" + java.lang.Long.toHexString(((Number) value).longValue()));
        } else if (isObject()) {
            if (value == null) {
                sb.append("null");
            } else {
                String s = "";
                try {
                    if (value instanceof String) {
                        s = (String) value;
                        if (s.length() > 50) {
                            s = s.substring(0, 30) + "...";
                        }
                        s = " \"" + s + '"';
                    }
                } catch (Exception e) {
                }
                if (s.isEmpty()) {
                    s = "@" + System.identityHashCode(value);
                }
                sb.append(CiUtil.getSimpleName(value.getClass(), true)).append(s);
            }
        } else {
            sb.append(value);
        }
        return sb.toString();
    }
}
