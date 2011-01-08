/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import com.sun.max.vm.cps.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirDivision extends AMD64EirUnaryOperation {

    private final EirOperand rdx;
    private final EirOperand rax;

    public EirOperand divisor() {
        return operand();
    }

    public EirLocation divisorLocation() {
        return divisor().location();
    }

    protected AMD64EirDivision(EirBlock block, EirValue rdx, EirValue rax, EirValue divisor) {
        super(block, divisor, EirOperand.Effect.USE, G_L_S);
        this.rdx = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        this.rdx.setRequiredLocation(AMD64EirRegister.General.RDX);
        this.rdx.setEirValue(rdx);
        this.rax = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        this.rax.setRequiredLocation(AMD64EirRegister.General.RAX);
        this.rax.setEirValue(rax);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        visitor.run(rdx);
        visitor.run(rax);
    }

    @Override
    public String toString() {
        return super.toString() + ", rd: " + rdx + ", ra: " + rax;
    }
}
