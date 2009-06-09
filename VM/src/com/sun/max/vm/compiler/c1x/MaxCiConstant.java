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
package com.sun.max.vm.compiler.c1x;

import com.sun.c1x.ci.CiConstant;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.BasicType;
import com.sun.max.vm.value.Value;

/**
 * The <code>MaxCiConstant</code> represents a constant, such as a primitive or object
 * reference, across the compiler interface.
 *
 * @author Ben L. Titzer
 */
public class MaxCiConstant implements CiConstant {

    private final Value _value;
    private final MaxCiType _type;

    /**
     * Constructs a new constant from a <code>Value</code>.
     * @param value the value
     */
    MaxCiConstant(Value value) {
        _value = value;
        _type = null;
    }

    /**
     * Constructs a new constant from a type (i.e. a possibly unresolved class constant).
     * @param type the type
     */
    MaxCiConstant(MaxCiType type) {
        _value = null;
        _type = type;
    }

    /**
     * Checks whether this constant represents a compiler interface type.
     * @return <code>true</code> if this constant is a compiler interface type, <code>false</code> otherwise
     */
    public boolean isCiType() {
        return _type != null;
    }

    /**
     * Converts this constant to a compiler interface type.
     * @return this constant as a compiler interface type; <code>null</code> if it is not a compiler interface
     * type
     */
    public CiType asCiType() {
        return _type;
    }

    /**
     * Converts this constant to an object reference.
     * @return the object reference this constant represents
     */
    public Object asObject() {
        if (_type != null) {
            if (!_type.isLoaded()) {
                throw new MaxCiUnresolved("asObject() not defined for unresolved MaxCiConstant of class " + _type);
            }
            return _type.javaClass();
        }
        return _value.asObject();
    }

    /**
     * Converts this constant to a string reference.
     * @return the string reference this constant represents
     */
    public String asString() {
        return (String) _value.asObject();
    }

    /**
     * Retrieves the boolean value of this constant.
     * @return the boolean value
     */
    public boolean asBoolean() {
        return _value.asBoolean();
    }

    /**
     * Retrieves the byte value of this constant.
     * @return the byte value
     */
    public byte asByte() {
        return _value.asByte();
    }

    /**
     * Retrieves the short value of this constant.
     * @return the short value
     */
    public short asShort() {
        return _value.asShort();
    }

    /**
     * Retrieves the char value of this constant.
     * @return the char value
     */
    public char asChar() {
        return _value.asChar();
    }

    /**
     * Retrieves the int value of this constant.
     * @return the int value
     */
    public int asInt() {
        return _value.asInt();
    }

    /**
     * Retrieves the long value of this constant.
     * @return the long value
     */
    public long asLong() {
        return _value.asLong();
    }

    /**
     * Retrieves the float value of this constant.
     * @return the float value
     */
    public float asFloat() {
        return _value.asFloat();
    }

    /**
     * Retrieves the double value of this constant.
     * @return the double value
     */
    public double asDouble() {
        return _value.asDouble();
    }

    /**
     * Gets the basic type of this constant.
     * @return the basic type of this constant.
     */
    public BasicType basicType() {
        if (_type != null) {
            return BasicType.Object;
        }
        return MaxCiType.kindToBasicType(_value.kind());
    }
}
