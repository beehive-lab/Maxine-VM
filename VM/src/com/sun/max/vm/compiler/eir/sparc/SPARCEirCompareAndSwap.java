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
package com.sun.max.vm.compiler.eir.sparc;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import java.lang.reflect.*;

import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * @author Laurent Daynes
 */
public class SPARCEirCompareAndSwap extends SPARCEirUnaryOperation {
    private Kind _kind;

    public Kind kind() {
        return _kind;
    }
    private final EirOperand _comparedOperand;

    public EirOperand comparedOperand() {
        return _addressOperand;
    }
    public EirLocation comparedLocation() {
        return _comparedOperand.location();
    }
    private final EirOperand _addressOperand;

    public EirOperand addressOperand() {
        return _addressOperand;
    }

    public EirLocation addressLocation() {
        return _addressOperand.location();
    }

    public SPARCEirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue comparedValue) {
        super(block, newValue, EirOperand.Effect.UPDATE, G);
        _kind = kind;
        _addressOperand = new EirOperand(this, EirOperand.Effect.USE, G);
        _addressOperand.setEirValue(pointer);
        _comparedOperand = new EirOperand(this, EirOperand.Effect.USE, G);
        _comparedOperand.setEirValue(comparedValue);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        visitor.run(_comparedOperand);
        visitor.run(_addressOperand);
    }

    public SPARCEirRegister.GeneralPurpose newValueRegister() {
        return operandGeneralRegister();
    }

    public SPARCEirRegister.GeneralPurpose oldValueRegister() {
        return (SPARCEirRegister.GeneralPurpose) comparedLocation();
    }

    public SPARCEirRegister.GeneralPurpose addressRegister() {
        return (SPARCEirRegister.GeneralPurpose) addressLocation();
    }

    @Override
    public void emit(SPARCEirTargetEmitter emitter) {
        switch (kind().asEnum()) {
            case LONG:
            case WORD:
            case REFERENCE:
                emitter.assembler().casx(addressRegister().as(), oldValueRegister().as(), newValueRegister().as());
                break;
            case INT:
                emitter.assembler().cas(addressRegister().as(), oldValueRegister().as(), newValueRegister().as());
                break;
            default: {
                FatalError.unimplemented();
            }
        }
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

}
