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

    private final BitRange bitRange;

    protected RiscField(BitRange bitRange) {
        this.bitRange = bitRange;
    }

    @Override
    public RiscField clone() {
        try {
            return (RiscField) super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected("Field.clone() not supported");
        }
    }

    private String name;

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String literal;

    public String literal() {
        return literal;
    }

    public void setLiteral(String literal) {
        this.literal = literal;
    }

    private Class literalClass;

    public Class literalClass() {
        return literalClass;
    }

    public void setLiteralClass(Class literalClass) {
        this.literalClass = literalClass;
    }

    public BitRange bitRange() {
        return bitRange;
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
        int result = bitRange.hashCode();
        if (name != null) {
            result ^= name.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return name();
    }

}
