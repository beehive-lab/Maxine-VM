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
    Boolean('Z', "boolean"),
    Char('C', "char"),
    Float('F', "float"),
    Double('D', "double"),
    Byte('B', "byte"),
    Short('S', "short"),
    Int('I', "int"),
    Long('J', "long"),
    Object('L', "object"),
    Array('[', "array"),
    Void('V', "void"),
    Address,
    NarrowOop,
    Conflict,
    Illegal;

    BasicType() {
        _char = (char) 0;
        _name = "???";
    }

    BasicType(char ch, String name) {
        _char = ch;
        _name = name;
    }

    public final char _char;
    public final String _name;

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
