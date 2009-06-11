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
 * The <code>BasicType</code> enum represents an enumeration of types used in C1X.
 *
 * @author Ben L. Titzer
 */
public enum BasicType {
    Boolean('Z', "boolean", 1),
    Byte('B', "byte", 1),
    Short('S', "short", 1),
    Char('C', "char", 1),
    Int('I', "int", 1),
    Float('F', "float", 1),
    Double('D', "double", 2),
    Long('J', "long", 2),
    Object('L', "object", 1),
    Void('V', "void", 0),
    Jsr('A', "jsr", 1),
    Illegal('?', "???", 1);

    BasicType(char ch, String name, int size) {
        _char = ch;
        _name = name;
        _size = size;
    }

    public final char _char;
    public final String _name;
    public final int _size;

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
        return _size;
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
        throw new IllegalArgumentException("unknown array type code");
    }

}
