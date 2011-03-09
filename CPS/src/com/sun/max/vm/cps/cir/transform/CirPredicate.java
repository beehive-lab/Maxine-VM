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
package com.sun.max.vm.cps.cir.transform;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A visitor for inspecting each node in a CIR graph,
 * just like CirVisitor, but with boolean return values.
 * In case several nodes are visited the result is the logical "or"
 * of the results of the individual visits, with evaluation shortcuts.
 *
 * @see CirVisitor
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
