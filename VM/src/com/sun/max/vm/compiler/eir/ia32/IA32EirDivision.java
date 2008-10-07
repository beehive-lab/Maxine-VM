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
/*VCSID=b76b2aa2-4cec-466b-9ef4-d6a81bb6cad6*/
package com.sun.max.vm.compiler.eir.ia32;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class IA32EirDivision extends IA32EirUnaryOperation {

    private final EirOperand _rd;
    private final EirOperand _ra;

    public EirOperand divisor() {
        return operand();
    }

    public EirLocation divisorLocation() {
        return divisor().location();
    }

    protected IA32EirDivision(EirBlock block, EirValue rd, EirValue ra, EirValue divisor) {
        super(block, divisor, EirOperand.Effect.USE, G_L_S);
        _rd = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        _rd.setRequiredLocation(IA32EirRegister.General.EDX);
        _rd.setEirValue(rd);
        _ra = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        _ra.setRequiredLocation(IA32EirRegister.General.EAX);
        _ra.setEirValue(ra);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        visitor.run(_rd);
        visitor.run(_ra);
    }

    @Override
    public String toString() {
        return super.toString() + ", rd: " + _rd + ", ra: " + _ra;
    }
}
