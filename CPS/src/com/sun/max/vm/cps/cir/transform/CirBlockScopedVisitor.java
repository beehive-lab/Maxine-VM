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
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A visitor for CirNodes that indicates which block the node is in.
 *
 * This is a delegating visitor in that the default implementation
 * for each visit method is a call the visit method for the next
 * non-abstract superclass of the current node type.
 *
 * @author Bernd Mathiske
 */
public class CirBlockScopedVisitor {

    protected CirBlockScopedVisitor() {
    }

    public void visitValues(CirValue[] values, CirBlock holder) {
        for (int i = 0; i < values.length; i++) {
            values[i].acceptBlockScopedVisitor(this, holder);
        }
    }

    public void visitVariables(CirVariable[] variables, CirBlock holder) {
        for (int i = 0; i < variables.length; i++) {
            variables[i].acceptBlockScopedVisitor(this, holder);
        }
    }

    public void visitNode(CirNode node, CirBlock holder) {
    }

    public void visitCall(CirCall call, CirBlock holder) {
        visitNode(call, holder);
    }

    public void visitValue(CirValue value, CirBlock holder) {
        visitNode(value, holder);
    }

    public void visitUndefined(CirValue.Undefined undefined, CirBlock holder) {
        visitValue(undefined, holder);
    }

    public void visitConstant(CirConstant constant, CirBlock holder) {
        visitValue(constant, holder);
    }

    public void visitProcedure(CirProcedure procedure, CirBlock holder) {
        visitValue(procedure, holder);
    }

    public void visitPrimitive(CirOperator primitive, CirBlock holder) {
        visitProcedure(primitive, holder);
    }

    public void visitBuiltin(CirBuiltin builtin, CirBlock holder) {
        visitPrimitive(builtin, holder);
    }

    public void visitSwitch(CirSwitch cirSwitch, CirBlock holder) {
        visitProcedure(cirSwitch, holder);
    }

    public void visitMethod(CirMethod method, CirBlock holder) {
        visitProcedure(method, holder);
    }

    public void visitSnippet(CirSnippet cirSnippet, CirBlock holder) {
        visitMethod(cirSnippet, holder);
    }

    public void visitBlock(CirBlock block, CirBlock holder) {
        visitProcedure(block, holder);
    }

    public void visitClosure(CirClosure closure, CirBlock holder) {
        visitProcedure(closure, holder);
    }

    public void visitContinuation(CirContinuation continuation, CirBlock holder) {
        visitClosure(continuation, holder);
    }

    public void visitVariable(CirVariable variable, CirBlock holder) {
        visitValue(variable, holder);
    }

    public void visitExceptionContinuationParameter(CirExceptionContinuationParameter parameter, CirBlock holder) {
        visitVariable(parameter, holder);
    }

    public void visitNormalContinuationParameter(CirNormalContinuationParameter parameter, CirBlock holder) {
        visitVariable(parameter, holder);
    }

    public void visitSlotVariable(CirSlotVariable variable, CirBlock holder) {
        visitVariable(variable, holder);
    }

    public void visitLocalVariable(CirLocalVariable variable, CirBlock holder) {
        visitSlotVariable(variable, holder);
    }

    public void visitStackVariable(CirStackVariable variable, CirBlock holder) {
        visitSlotVariable(variable, holder);
    }

    public void visitMethodParameter(CirMethodParameter parameter, CirBlock holder) {
        visitSlotVariable(parameter, holder);
    }

    public void visitTemporaryVariable(CirTemporaryVariable variable, CirBlock holder) {
        visitVariable(variable, holder);
    }
}
