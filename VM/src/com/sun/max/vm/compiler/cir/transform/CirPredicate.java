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
/*VCSID=98ce46a9-53ef-43d4-b910-88f09b765cf7*/
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * A visitor for inspecting each node in a CIR graph,
 * just like CirVisitor, but with boolean return values.
 * In case several nodes are visited the result is the logical "or"
 * of the results of the individual visits, with evaluation shortcuts.
 *
 *  @see CirVisitor
 *
 * @author Bernd Mathiske
 */
public abstract class CirPredicate {

    protected CirPredicate() {
    }

    public boolean evaluateValues(CirValue[] values) {
        for (int i = 0; i < values.length; i++) {
            final boolean result = values[i].acceptPredicate(this);
            if (result) {
                return result;
            }
        }
        return false;
    }

    public boolean evaluateVariables(CirVariable[] variables) {
        for (int i = 0; i < variables.length; i++) {
            final boolean result = variables[i].acceptPredicate(this);
            if (result) {
                return result;
            }
        }
        return false;
    }

    public boolean evaluateNode(CirNode node) {
        return false;
    }

    public boolean evaluateCall(CirCall call) {
        return evaluateNode(call);
    }

    public boolean evaluateValue(CirValue value) {
        return evaluateNode(value);
    }

    public boolean evaluateConstant(CirConstant constant) {
        return evaluateValue(constant);
    }

    public boolean evaluateProcedure(CirProcedure procedure) {
        return evaluateValue(procedure);
    }

    public boolean evaluatePrimitive(CirOperator primitive) {
        return evaluateProcedure(primitive);
    }

    public boolean evaluateSwitch(CirSwitch cirSwitch) {
        return evaluateProcedure(cirSwitch);
    }

    public boolean evaluateBuiltin(CirBuiltin builtin) {
        return evaluatePrimitive(builtin);
    }

    public boolean evaluateMethod(CirMethod method) {
        return evaluateProcedure(method);
    }

    public boolean evaluateSnippet(CirSnippet cirSnippet) {
        return evaluateMethod(cirSnippet);
    }

    public boolean evaluateBlock(CirBlock block) {
        return evaluateProcedure(block);
    }

    public boolean evaluateClosure(CirClosure closure) {
        return evaluateProcedure(closure);
    }

    public boolean evaluateContinuation(CirContinuation continuation) {
        return evaluateClosure(continuation);
    }

    public boolean evaluateVariable(CirVariable variable) {
        return evaluateValue(variable);
    }

    public boolean evaluateExceptionContinuationParameter(CirExceptionContinuationParameter parameter) {
        return evaluateVariable(parameter);
    }

    public boolean evaluateNormalContinuationParameter(CirNormalContinuationParameter parameter) {
        return evaluateVariable(parameter);
    }

    public boolean evaluateSlotVariable(CirSlotVariable variable) {
        return evaluateVariable(variable);
    }

    public boolean evaluateLocalVariable(CirLocalVariable variable) {
        return evaluateSlotVariable(variable);
    }

    public boolean evaluateStackVariable(CirStackVariable variable) {
        return evaluateSlotVariable(variable);
    }

    public boolean evaluateMethodParameter(CirMethodParameter parameter) {
        return evaluateSlotVariable(parameter);
    }

    public boolean evaluateTemporaryVariable(CirTemporaryVariable variable) {
        return evaluateVariable(variable);
    }

}
