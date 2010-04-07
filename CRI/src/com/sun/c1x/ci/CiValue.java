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
 * Base class for compiler interface values.
 *
 * @author Thomas Wuerthinger
 * @author Doug Simon
 */
public abstract class CiValue {

    public static CiLocation IllegalLocation = new CiLocation(CiKind.Illegal) {
        @Override
        public String name() {
            return "<illegal>";
        }
        @Override
        public CiRegister asRegister() {
            return CiRegister.None;
        }
    };
    
    /**
     * The kind of this value.
     */
    public final CiKind kind;

    /**
     * Initializes a new value of the specified kind.
     * @param kind the kind
     */
    protected CiValue(CiKind kind) {
        this.kind = kind;
    }

    public final boolean isVariableOrRegister() {
        return this instanceof CiVariable || this instanceof CiRegisterLocation;
    }

    protected Error illegalOperation(String operation) {
        throw new InternalError("Cannot call " + operation + " on " + this);
    }

    public CiRegister asRegister() {
        throw illegalOperation("asRegister");
    }

    public final CiLocation asLocation() {
        return (CiLocation) this;
    }

    public final boolean isIllegal() {
        return this == IllegalLocation;
    }
    
    public final boolean isLegal() {
        return this != IllegalLocation;
    }

    public final boolean isStackSlot() {
        return this instanceof CiStackSlot;
    }

    public final boolean isRegister() {
        return this instanceof CiRegisterLocation;
    }

    public final boolean isVariable() {
        return this instanceof CiVariable;
    }

    public final boolean isAddress() {
        return this instanceof CiAddress;
    }

    public final boolean isConstant() {
        return this instanceof CiConstant;
    }
    
    /**
     * Gets a string name for this value without indicating its {@linkplain #kind kind}.
     */
    public abstract String name();
    
    @Override
    public final String toString() {
        return name() + kindSuffix();
    }
    
    private final String kindSuffix() {
        if (kind == CiKind.Illegal) {
            return "";
        }
        return ":" + kind.typeChar;
    }
}
