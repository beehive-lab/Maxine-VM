/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir;

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
