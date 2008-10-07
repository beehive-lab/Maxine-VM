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
/*VCSID=a32437a4-e0a8-4fb1-9756-19425710f804*/
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;

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
