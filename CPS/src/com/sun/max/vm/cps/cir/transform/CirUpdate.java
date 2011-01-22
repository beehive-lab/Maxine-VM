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
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Like a visitor, but returning a boolean that indicates
 * whether a node has been updated during the visit.
 *
 * @author Bernd Mathiske
 */
public abstract class CirUpdate {

    private final CirOptimizer cirOptimizer;

    public CirOptimizer cirOptimizer() {
        return cirOptimizer;
    }

    protected CirUpdate(CirOptimizer cirOptimizer) {
        this.cirOptimizer = cirOptimizer;
    }

    public boolean updateValues(CirValue[] values) {
        boolean result = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i].acceptUpdate(this)) {
                result = true;
            }
        }
        return result;
    }

    public boolean updateVariables(CirVariable[] variables) {
        boolean result = false;
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].acceptUpdate(this)) {
                result = true;
            }
        }
        return result;
    }

    public boolean updateNode(CirNode node) {
        return false;
    }

    public boolean updateCall(CirCall call) {
        boolean result = call.procedure().acceptUpdate(this);
        result |= updateValues(call.arguments());
        CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
        while (javaFrameDescriptor != null) {
            result |= updateValues(javaFrameDescriptor.locals);
            result |= updateValues(javaFrameDescriptor.stackSlots);
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }
        result |= updateNode(call);
        return result;
    }

    public boolean updateValue(CirValue value) {
        return updateNode(value);
    }

    public boolean updateConstant(CirConstant constant) {
        return updateValue(constant);
    }

    public boolean updateProcedure(CirProcedure procedure) {
        return updateValue(procedure);
    }

    public boolean updatePrimitive(CirOperator primitive) {
        return updateProcedure(primitive);
    }

    public boolean updateSwitch(CirSwitch cirSwitch) {
        return updateProcedure(cirSwitch);
    }

    public boolean updateBuiltin(CirBuiltin builtin) {
        return updatePrimitive(builtin);
    }

    public boolean updateMethod(CirMethod method) {
        return updateProcedure(method);
    }

    protected final IdentityHashSet<CirBlock> visitedBlocks = new IdentityHashSet<CirBlock>();

    public boolean updateBlock(CirBlock block) {
        if (visitedBlocks.contains(block)) {
            return false;
        }
        visitedBlocks.add(block);
        boolean result = block.closure().acceptUpdate(this);
        result |= updateProcedure(block);
        return result;
    }

    public boolean updateClosure(CirClosure closure) {
        boolean result = updateVariables(closure.parameters());
        result |= closure.body().acceptUpdate(this);
        result |= updateProcedure(closure);
        return result;
    }

    public boolean updateContinuation(CirContinuation continuation) {
        return updateClosure(continuation);
    }

    public boolean updateVariable(CirVariable variable) {
        return updateValue(variable);
    }

}
