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
package com.sun.c1x.lir;

import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiRegister;

/**
 * This class represents a LIR variable, i.e. a virtual register that can be used in LIRInstructions
 * as an operand. Each definition and use of a variable must be given a physical location by the register
 * allocator.
 *
 * @author Ben L. Titzer
 */
public class LIRVariable extends LIROperand {

    public final int index;

    public LIRVariable(CiKind kind, int index) {
        super(kind);
        assert index >= CiRegister.MaxPhysicalRegisterNumber;
        this.index = index;
    }

    @Override
    public boolean isVariable() {
        return true;
    }

    @Override
    public boolean isVariableOrRegister() {
        return true;
    }

    @Override
    public int variableNumber() {
        return index;
    }

    @Override
    public int hashCode() {
        return index + kind.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof LIRVariable) {
            LIRVariable v = (LIRVariable) o;
            return v.index == index && v.kind == kind;
        }
        return false;
    }
}
