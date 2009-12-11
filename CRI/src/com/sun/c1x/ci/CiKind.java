/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ci;

/**
 * This enum represents the basic kinds of types in C1X, including the primitive types, objects, words,
 * {@code void}, and bytecode addresses used in JSR bytecodes.
 *
 * @author Ben L. Titzer
 */
public enum CiKind {
    Boolean('z', "boolean", "jboolean", 1),
    Byte('b', "byte", "jbyte", 1),
    Short('s', "short", "jshort", 1),
    Char('c', "char", "jchar", 1),
    Int('i', "int", "jint", 1),
    Float('f', "float", "jfloat", 1),
    Double('d', "double", "jdouble", 2),
    Long('l', "long", "jlong", 2),
    Object('a', "object", "jobject", 1),
    Word('w', "word", null, 1),
    Void('v', "void", "void", 0),
    Jsr('r', "jsr", null, 1),
    Illegal(' ', "illegal", null, -1);

    CiKind(char ch, String name, String jniName, int size) {
        this.typeChar = ch;
        this.javaName = name;
        this.jniName = jniName;
        this.size = size;
    }

    /**
     * The name of the basic type as a single character.
     */
    public final char typeChar;

    /**
     * The name of this basic type which will also be it Java programming language name if
     * it is {@linkplain #isPrimitive() primitive} or {@code void}.
     */
    public final String javaName;

    /**
     * The JNI name of this basic type; {@code null} if this basic type is not a valid JNI type.
     */
    public final String jniName;

    /**
     * The size of this basic type in terms of abstract JVM words. Note that this may
     * differ with actual size of this type in it machine representation.
     */
    public final int size;

    /**
     * Checks whether this basic type is valid as the type of a field.
     * @return {@code true} if this basic type is valid as the type of a Java field
     */
    public boolean isValidFieldType() {
        return ordinal() <= Object.ordinal();
    }

    /**
     * Checks whether this basic type is valid as the return type of a method.
     * @return {@code true} if this basic type is valid as the return type of a Java method
     */
    public boolean isValidReturnType() {
        return ordinal() <= Void.ordinal();
    }

    /**
     * Checks whether this type is valid as an <code>int</code> on the Java operand stack.
     * @return {@code true} if this type is represented by an <code>int</code> on the operand stack
     */
    public boolean isInt() {
        return ordinal() <= Int.ordinal();
    }

    /**
     * Checks whether this type is a Java primitive type.
     * @return {@code true} if this is {@link #Boolean}, {@link #Byte}, {@link #Char}, {@link #Short},
     *                                 {@link #Int}, {@link #Long}, {@link #Float} or {@link #Double}.
     */
    public boolean isPrimitive() {
        return ordinal() <= Long.ordinal();
    }

    /**
     * Gets the basic type that represents this basic type when on the Java operand stack.
     * @return the basic type used on the operand stack
     */
    public CiKind stackType() {
        if (ordinal() <= Int.ordinal()) {
            return Int;
        }
        return this;
    }

    /**
     * Gets the size of this basic type in terms of the number of Java slots.
     * @return the size of the basic type in slots
     */
    public int sizeInSlots() {
        return size;
    }

    /**
     * Gets the size of this basic type in bytes.
     * @param referenceSize the size of an object reference
     * @param wordSize the size of a word in bytes
     * @return the size of this basic type in bytes
     */
    public int sizeInBytes(int referenceSize, int wordSize) {
        switch (this) {
            case Boolean: return 1;
            case Byte: return 1;
            case Char: return 2;
            case Short: return 2;
            case Int: return 4;
            case Long: return 8;
            case Float: return 4;
            case Double: return 8;
            case Object: return referenceSize;
            case Jsr: return 4;
            case Word: return wordSize;
            default: return 0;
        }
    }

    /**
     * Gets the element size of this basic type in bytes.
     * @param oopSize the size of an object reference
     * @param wordSize the size of a word
     * @return the size of this basic type in bytes
     */
    public int elementSizeInBytes(int oopSize, int wordSize) {
        return sizeInBytes(oopSize, wordSize);
    }

    /**
     * Gets the basic type of array elements for the array type code that appears
     * in a newarray bytecode.
     * @param code the array type code
     * @return the basic type from the array type code
     */
    public static CiKind fromArrayTypeCode(int code) {
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
        throw new IllegalArgumentException("unknown array type code: " + code);
    }

    /**
     * Gets the basic type from the character describing a primitive or void.
     * @param ch the character
     * @return the basic type
     */
    public static CiKind fromPrimitiveOrVoidTypeChar(char ch) {
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
        throw new IllegalArgumentException("unknown primitive or void type character: " + ch);
    }

    /**
     * Gets the array class which has elements of this basic type. This method
     * is only defined for primtive types.
     * @return the Java class which represents arrays of this basic type
     */
    public Class<?> primitiveArrayClass() {
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
        throw new IllegalArgumentException("not a primitive basic type");
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
     * Checks whether this value type is an object type.
     * @return {@code true} if this type is an object
     */
    public final boolean isObject() {
        return this == CiKind.Object;
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
        if (other.stackType() == this.stackType()) {
            return this.stackType();
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
}
