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
package com.sun.c1x.value;

/**
 * The <code>BasicType</code> enum represents the basic kinds of types in C1X,
 * including the primitive types, objects, {@code void}, and bytecode addresses used in JSR.
 *
 * @author Ben L. Titzer
 */
public enum BasicType {
    Boolean('z', "boolean", "jboolean", 1),
    Byte('b', "byte", "jbyte", 1),
    Short('s', "short", "jshort", 1),
    Char('c', "char", "jchar", 1),
    Int('i', "int", "jint", 1),
    Float('f', "float", "jfloat", 1),
    Double('d', "double", "jdouble", 2),
    Long('l', "long", "jlong", 2),
    Object('a', "object", "jobject", 1),
    Void('v', "void", "void", 0),
    Jsr('r', "jsr", null, 1),
    Illegal(' ', "illegal", null, -1);

    BasicType(char ch, String name, String jniName, int size) {
        this.basicChar = ch;
        this.javaName = name;
        this.jniName = jniName;
        this.size = size;
    }

    /**
     * The name of the basic type as a single character.
     */
    public final char basicChar;

    /**
     * The name of this basic type which will also be it Java programming language name if
     * it is {@linkplain #isPrimitiveType() primitive} or {@code void}.
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
     * @return <code>true</code> if this basic type is valid as the type of a Java field
     */
    public boolean isValidFieldType() {
        return ordinal() <= Object.ordinal();
    }

    /**
     * Checks whether this basic type is valid as the return type of a method.
     * @return <code>true</code> if this basic type is valid as the return type of a Java method
     */
    public boolean isValidReturnType() {
        return ordinal() <= Void.ordinal();
    }

    /**
     * Checks whether this type is valid as an <code>int</code> on the Java operand stack.
     * @return <code>true</code> if this type is represented by an <code>int</code> on the operand stack
     */
    public boolean isIntType() {
        return ordinal() <= Int.ordinal();
    }

    /**
     * Checks whether this type is a Java primitive type.
     * @return {@code true} if this is {@link #Boolean}, {@link #Byte}, {@link #Char}, {@link #Short},
     *                                 {@link #Int}, {@link #Long}, {@link #Float} or {@link #Double}.
     */
    public boolean isPrimitiveType() {
        return ordinal() <= Long.ordinal();
    }

    /**
     * Gets the basic type that represents this basic type when on the Java operand stack.
     * @return the basic type used on the operand stack
     */
    public BasicType stackType() {
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
     * @param oopSize the size of an object reference
     * @return the size of this basic type in bytes
     */
    public int sizeInBytes(int oopSize) {
        switch (this) {
            case Boolean: return 1;
            case Byte: return 1;
            case Char: return 2;
            case Short: return 2;
            case Int: return 4;
            case Long: return 8;
            case Float: return 4;
            case Double: return 8;
            case Object: return oopSize;
        }
        throw new IllegalArgumentException("invalid BasicType " + this + " for .sizeInBytes()");
    }

    /**
     * Gets the basic type of array elements for the array type code that appears
     * in a {@link com.sun.c1x.bytecode.Bytecodes#NEWARRAY newarray} bytecode.
     * @param code the array type code
     * @return the basic type from the array type code
     */
    public static BasicType fromArrayTypeCode(int code) {
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
    public static BasicType fromPrimitiveOrVoidTypeChar(char ch) {
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
}
