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
package com.sun.max.asm.gen.risc.field;

import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * A field describes a bit range and how it relates to an operand.
 * 
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class RiscField implements Cloneable, StaticFieldName, StaticFieldLiteral {

    private final BitRange _bitRange;

    protected RiscField(BitRange bitRange) {
        _bitRange = bitRange;
    }

    @Override
    public RiscField clone() {
        try {
            return (RiscField) super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected("Field.clone() not supported");
        }
    }

    private String _name;

    public String name() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    private String _literal;

    public String literal() {
        return _literal;
    }

    public void setLiteral(String literal) {
        _literal = literal;
    }

    private Class _literalClass;

    public Class literalClass() {
        return _literalClass;
    }

    public void setLiteralClass(Class literalClass) {
        _literalClass = literalClass;
    }

    public BitRange bitRange() {
        return _bitRange;
    }

    /**
     * Two RISC fields are considered equal if they define the same set of bits in an instruction
     * (i.e. their bit ranges are equal).
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof RiscField) {
            final RiscField riscField = (RiscField) other;
            return bitRange().equals(riscField.bitRange());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = _bitRange.hashCode();
        if (_name != null) {
            result ^= _name.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return name();
    }

}
