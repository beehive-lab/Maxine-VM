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
package com.sun.max.vm.cps.cir.transform;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A visitor for inspecting each node in a CIR graph. This is a delegating visitor in that the
 * default implementation for each visit method is a call the visit method for the next
 * non-abstract superclass of the current node type.
 *
 * @author Bernd Mathiske
 */
public abstract class CirVisitor {

    protected CirVisitor() {
    }

    public void visitValues(CirValue[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i].acceptVisitor(this);
        }
    }

    public void visitVariables(CirVariable[] variables) {
        for (int i = 0; i < variables.length; i++) {
            variables[i].acceptVisitor(this);
        }
    }

    public void visitNode(CirNode node) {
    }

    public void visitCall(CirCall call) {
        visitNode(call);
    }

    public void visitValue(CirValue value) {
        visitNode(value);
    }

    public void visitUndefined(CirValue.Undefined undefined) {
        visitValue(undefined);
    }

    public void visitConstant(CirConstant constant) {
        visitValue(constant);
    }

    public void visitProcedure(CirProcedure procedure) {
        visitValue(procedure);
    }

    public void visitPrimitive(CirOperator primitive) {
        visitProcedure(primitive);
    }

    public void visitBuiltin(CirBuiltin builtin) {
        visitPrimitive(builtin);
    }

    public void visitSwitch(CirSwitch cirSwitch) {
        visitProcedure(cirSwitch);
    }

    public void visitMethod(CirMethod method) {
        visitProcedure(method);
    }

    public void visitSnippet(CirSnippet cirSnippet) {
        visitMethod(cirSnippet);
    }

    public void visitBlock(CirBlock block) {
        visitProcedure(block);
    }

    public void visitClosure(CirClosure closure) {
        visitProcedure(closure);
    }

    public void visitContinuation(CirContinuation continuation) {
        visitClosure(continuation);
    }

    public void visitVariable(CirVariable variable) {
        visitValue(variable);
    }

    public void visitExceptionContinuationParameter(CirExceptionContinuationParameter parameter) {
        visitVariable(parameter);
    }

    public void visitNormalContinuationParameter(CirNormalContinuationParameter parameter) {
        visitVariable(parameter);
    }

    public void visitSlotVariable(CirSlotVariable variable) {
        visitVariable(variable);
    }

    public void visitLocalVariable(CirLocalVariable variable) {
        visitSlotVariable(variable);
    }

    public void visitStackVariable(CirStackVariable variable) {
        visitSlotVariable(variable);
    }

    public void visitMethodParameter(CirMethodParameter parameter) {
        visitSlotVariable(parameter);
    }

    public void visitTemporaryVariable(CirTemporaryVariable variable) {
        visitVariable(variable);
    }

    public void visitHCirOperator(JavaOperator op) {
        visitProcedure(op);
    }

    public void visitGetfield(GetField op) {
        visitHCirOperator(op);
    }

    public void visitGetstatic(GetStatic op) {
        visitHCirOperator(op);
    }

    public void visitInvokevirtual(InvokeVirtual op) {
        visitHCirOperator(op);
    }
}
