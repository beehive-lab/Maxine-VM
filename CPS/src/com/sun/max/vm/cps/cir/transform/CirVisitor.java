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
