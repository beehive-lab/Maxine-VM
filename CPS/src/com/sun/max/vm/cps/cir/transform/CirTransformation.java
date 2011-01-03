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
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Like a visitor, but with a return value,
 * the node that is about to replace the principal parameter in the CIR graph.
 *
 * @author Bernd Mathiske
 */
public abstract class CirTransformation {

    protected CirTransformation() {
    }

    public void transformValues(CirValue[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                values[i] = (CirValue) values[i].acceptTransformation(this);
            }
        }
    }

    public CirValue[] transformedValues(CirValue[] values) {
        final CirValue[] result = values.clone();
        transformValues(result);
        return result;
    }

    public void transformVariables(CirVariable[] variables) {
        for (int i = 0; i < variables.length; i++) {
            variables[i] = (CirVariable) variables[i].acceptTransformation(this);
        }
    }

    public CirVariable[] transformedVariables(CirVariable[] variables) {
        final CirVariable[] result = variables.clone();
        transformVariables(result);
        return result;
    }

    public CirNode transformNode(CirNode node) {
        return node;
    }

    public CirNode transformCall(CirCall call) {
        return transformNode(call);
    }

    public CirNode transformValue(CirValue value) {
        return transformNode(value);
    }

    public CirNode transformConstant(CirConstant constant) {
        return transformValue(constant);
    }

    public CirNode transformProcedure(CirProcedure procedure) {
        return transformValue(procedure);
    }

    public CirNode transformPrimitive(CirOperator primitive) {
        return transformProcedure(primitive);
    }

    public CirNode transformSwitch(CirSwitch cirSwitch) {
        return transformProcedure(cirSwitch);
    }

    public CirNode transformBuiltin(CirBuiltin builtin) {
        return transformPrimitive(builtin);
    }

    public CirNode transformMethod(CirMethod method) {
        return method;
    }

    public CirNode transformBlock(CirBlock block) {
        return block;
    }

    public CirNode transformClosure(CirClosure closure) {
        return transformProcedure(closure);
    }

    public CirNode transformContinuation(CirContinuation continuation) {
        return transformClosure(continuation);
    }

    public CirNode transformVariable(CirVariable variable) {
        return transformValue(variable);
    }

}
