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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirUnaryOperation extends AMD64EirOperation {

    private final EirOperand operand;

    protected AMD64EirUnaryOperation(EirBlock block, EirValue operand, EirOperand.Effect effect, PoolSet<EirLocationCategory> locationCategories) {
        super(block);
        this.operand = new EirOperand(this, effect, locationCategories);
        this.operand.setEirValue(operand);
    }

    public EirOperand operand() {
        return operand;
    }

    public EirValue operandValue() {
        return operand.eirValue();
    }

    public EirLocation operandLocation() {
        return operand.location();
    }

    public AMD64EirRegister.General operandGeneralRegister() {
        return (AMD64EirRegister.General) operandLocation();
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        if (operand != null) {
            visitor.run(operand);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "  " + operand;
    }
}
