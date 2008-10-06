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
/*VCSID=114c924e-d8ca-45bc-b05b-96750389a213*/
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * SPARC Eir Unary Operation.
 * The location of the operand can only be a register.
 * Sub-classes of this class may specify a null operand.
 * 
 * @author Laurent Daynes
 */
public abstract class SPARCEirUnaryOperation extends SPARCEirOperation {
    private final EirOperand _operand;

    /**
     * TODO: Unary operations can only have a register operand. We may want to change the signature of the constructor to reflect this constraint.
     * @param block
     * @param operand
     * @param effect
     * @param locationCategories
     */
    protected SPARCEirUnaryOperation(EirBlock block, EirValue operand, EirOperand.Effect effect, PoolSet<EirLocationCategory> locationCategories) {
        super(block);
        _operand = new EirOperand(this, effect, locationCategories);
        _operand.setEirValue(operand);
    }

    protected SPARCEirUnaryOperation(EirBlock block, EirOperand operand) {
        super(block);
        _operand = operand;
    }

    public EirOperand operand() {
        return _operand;
    }

    public EirValue operandValue() {
        return _operand.eirValue();
    }

    public EirLocation operandLocation() {
        return _operand.location();
    }

    public SPARCEirRegister.GeneralPurpose operandGeneralRegister() {
        return (SPARCEirRegister.GeneralPurpose) operandLocation();
    }

    public SPARCEirRegister.FloatingPoint operandFloatingPointRegister() {
        return (SPARCEirRegister.FloatingPoint) operandLocation();
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        if (_operand != null) {
            visitor.run(_operand);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "  " + _operand;
    }
}
