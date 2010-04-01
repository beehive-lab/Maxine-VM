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
 * This class represents a register location storing a value of a fixed kind.
 *
 * @author Ben L. Titzer
 */
public final class CiRegisterLocation extends CiLocation {

    /**
     * The register.
     */
    public final CiRegister register;

    public static final CiRegisterLocation None = new CiRegisterLocation(CiKind.Illegal, CiRegister.None);
    public static final CiRegisterLocation Frame = new CiRegisterLocation(CiKind.Illegal, CiRegister.Frame);
    public static final CiRegisterLocation CallerFrame = new CiRegisterLocation(CiKind.Illegal, CiRegister.CallerFrame);

    /**
     * Gets a {@link CiRegisterLocation} instance representing a given register
     * storing a value of a given kind.
     *  
     * @param kind the kind of the value read/written to {@code register}
     * @param register a register
     * 
     * @see CiRegister#asLocation(CiKind)
     */
    public static CiRegisterLocation get(CiKind kind, CiRegister register) {
        assert register != CiRegister.None;
        assert register != CiRegister.Frame;
        assert register != CiRegister.CallerFrame;
        CiRegisterLocation reg = cache[kind.ordinal()][register.number];
        if (reg == null) {
            reg = new CiRegisterLocation(kind, register);
            
            // Note: this is not atomic so it's possible for more than one CiRegisterLocation
            // for the same (CiKind, CiRegister) pair to exist. That is fine as this pooling
            // is only for reducing memory usage, not for providing canonicalization.
            cache[kind.ordinal()][register.number] = reg;
        }
        return reg;
    }

    private static CiRegisterLocation[][] cache = new CiRegisterLocation[CiKind.values().length][CiRegister.LowestVirtualRegisterNumber];
    
    /**
     * Private constructor to enforce use of {@link #get(CiKind, int)} so that the
     * shared instance {@linkplain #cache cache} is used.
     */
    private CiRegisterLocation(CiKind kind, CiRegister register) {
        super(kind);
        this.register = register;
    }

    public int hashCode() {
        return kind.ordinal() ^ register.number;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CiRegisterLocation) {
            CiRegisterLocation l = (CiRegisterLocation) o;
            return l.kind == kind && l.register == register;
        }
        return false;
    }

    public String toString() {
        return "%" + register.name + ":" + kind;
    }
    
    @Override
    public CiRegister asRegister() {
        return register;
    }
}
