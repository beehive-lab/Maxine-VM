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
package com.sun.max.vm.classfile.create;

import com.sun.max.annotate.*;

/**
 * The common super class of all constant pool entries.
 *
 * @author Bernd Mathiske
 * @version 1.0
 */
abstract class MillConstant {

    static final byte CONSTANT_Utf8 = 1;
    static final byte CONSTANT_Integer = 3;
    static final byte CONSTANT_Float = 4;
    static final byte CONSTANT_Long = 5;
    static final byte CONSTANT_Double = 6;
    static final byte CONSTANT_Class = 7;
    static final byte CONSTANT_String = 8;
    static final byte CONSTANT_FieldRef = 9;
    static final byte CONSTANT_MethodRef = 10;
    static final byte CONSTANT_InterfaceMethodRef = 11;
    static final byte CONSTANT_NameAndType = 12;

    @CONSTANT
    byte _tag;

    @CONSTANT
    int _numberOfBytes;

    @CONSTANT
    int _hashValue;

    MillConstant _next;
    int _index = -1;

    protected MillConstant(byte tag, int nBytes, int hashValue) {
        this._tag = tag;
        this._numberOfBytes = nBytes;
        this._hashValue = hashValue;
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @return A hash code value for this object..
     */
    @Override
    public int hashCode() {
        return _hashValue;
    }

}
