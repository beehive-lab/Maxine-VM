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
package com.sun.max.vm.compiler.cps.eir;

import java.lang.reflect.*;

/**
 * Pseudo instruction enforcing allocation constraints on an exception catch parameter.
 * 
 * @author Bernd Mathiske
 */
public class EirCatch extends EirInstruction {

    private EirOperand catchParameterOperand;

    public EirOperand catchParameterOperand() {
        return catchParameterOperand;
    }

    public EirCatch(EirBlock block, EirValue catchParameter, EirLocation location) {
        super(block);
        catchParameterOperand = new EirOperand(this, EirOperand.Effect.DEFINITION, location.category().asSet());
        catchParameterOperand.setRequiredLocation(location);
        catchParameterOperand.setEirValue(catchParameter);
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

    @Override
    public void emit(EirTargetEmitter emitter) {
        // Do nothing since this is merely a pseudo instruction representing allocation constraints.
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(catchParameterOperand);
    }

    @Override
    public String toString() {
        return super.toString() + " (" + catchParameterOperand.eirValue() + ")";
    }

}
