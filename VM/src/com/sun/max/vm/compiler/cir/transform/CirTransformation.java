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

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.variable.*;

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
