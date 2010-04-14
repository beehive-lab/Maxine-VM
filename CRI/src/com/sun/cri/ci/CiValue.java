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
package com.sun.cri.ci;



/**
 * Abstract base class for values manipulated by the compiler. All values have a {@linkplain CiKind kind} and are immutable.
 *
 * @author Thomas Wuerthinger
 * @author Doug Simon
 */
public abstract class CiValue {

    public static CiValue IllegalValue = new CiValue(CiKind.Illegal) {
        @Override
        public String name() {
            return "<illegal>";
        }
        @Override
        public CiRegister asRegister() {
            return CiRegister.None;
        }
        @Override
        public int hashCode() {
            return -1;
        }
        @Override
        public boolean equals(Object obj) {
            return obj == this;
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
        return this instanceof CiVariable || this instanceof CiRegisterValue;
    }

    protected Error illegalOperation(String operation) {
        throw new InternalError("Cannot call " + operation + " on " + this);
    }

    public CiRegister asRegister() {
        throw illegalOperation("asRegister");
    }

    public final boolean isIllegal() {
        return this == IllegalValue;
    }

    public final boolean isLegal() {
        return this != IllegalValue;
    }

    public final boolean isStackSlot() {
        return this instanceof CiStackSlot;
    }

    public final boolean isRegister() {
        return this instanceof CiRegisterValue;
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
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

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
