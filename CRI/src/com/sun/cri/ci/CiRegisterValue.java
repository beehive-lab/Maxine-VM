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
 * This class represents a register storing a value of a fixed kind.
 * Use {@link CiRegister#asLocation(CiKind))} to retrieve the canonical
 * {@link CiRegisterValue} instance for a given (register,kind) pair.  
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class CiRegisterValue extends CiValue {

    /**
     * The register.
     */
    public final CiRegister register;

    /**
     * Should only be called from {@link CiRegister#CiRegister} to ensure canonicalization.
     */
    CiRegisterValue(CiKind kind, CiRegister register) {
        super(kind);
        this.register = register;
    }

    @Override
    public int hashCode() {
        return kind.ordinal() ^ register.number;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public String name() {
        return register.name;
    }
    
    @Override
    public CiRegister asRegister() {
        return register;
    }
}
