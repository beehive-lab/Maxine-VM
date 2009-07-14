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

import com.sun.c1x.value.BasicType;

/**
 * The <code>CiConstant</code> interface represents a constant that can be referred to
 * through a LDC (load constant) bytecode, which includes integers, floats, longs, doubles,
 * class constants, and strings.
 *
 * @author Ben L. Titzer
 */
public interface CiConstant {
    /**
     * Checks whether this constant represents a reference to a type.
     * @return {@code true} if this constant refers to a type
     */
    boolean isCiType();

    /**
     * Converts this constant to a type, if it is a reference to a type.
     * @return a reference to the compiler interface type if this is a reference to a type; {@code null} otherwise
     */
    CiType asCiType();

    /**
     * Gets the object referred to by this constant, if it is an object reference.
     * @return a reference to the object if this is a reference to an object; {@code null} otherwise
     */
    Object asObject();

    /**
     * Gets the string referred to by this constant, if it is a string.
     * @return the string if this is a reference to a string; {@code null} otherwise
     */
    String asString();

    /**
     * Converts this constant to a boolean.
     * @return the boolean value of the constant, if it is a boolean
     */
    boolean asBoolean();

    /**
     * Converts this constant to a byte.
     * @return the byte value of the constant, if it is a byte
     */
    byte asByte();

    /**
     * Converts this constant to a short.
     * @return the short value of the constant, if it is a short
     */
    short asShort();

    /**
     * Converts this constant to a char.
     * @return the char value of the constant, if it is a char
     */
    char asChar();

    /**
     * Converts this constant to an int.
     * @return the int value of the constant, if it is an int
     */
    int asInt();

    /**
     * Converts this constant to a long.
     * @return the long value of the constant, if it is a long
     */
    long asLong();

    /**
     * Converts this constant to a float.
     * @return the float value of the constant, if it is a float
     */
    float asFloat();

    /**
     * Converts this constant to a double.
     * @return the double value of the constant, if it is a double
     */
    double asDouble();

    /**
     * Gets the basic type of this constant, which can be used to convert it
     * to the appropriate primitive value or object.
     * @return the basic type of this constant
     */
    BasicType basicType();
}
