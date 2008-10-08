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
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Like a visitor, but returning a boolean that indicates
 * whether a node has been updated during the visit.
 *
 * @author Bernd Mathiske
 */
public abstract class CirUpdate {

    private final CirOptimizer _cirOptimizer;

    public CirOptimizer cirOptimizer() {
        return _cirOptimizer;
    }

    protected CirUpdate(CirOptimizer cirOptimizer) {
        _cirOptimizer = cirOptimizer;
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
            result |= updateValues(javaFrameDescriptor.locals());
            result |= updateValues(javaFrameDescriptor.stackSlots());
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

    protected final IdentityHashSet<CirBlock> _visitedBlocks = new IdentityHashSet<CirBlock>();

    public boolean updateBlock(CirBlock block) {
        if (_visitedBlocks.contains(block)) {
            return false;
        }
        _visitedBlocks.add(block);
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
