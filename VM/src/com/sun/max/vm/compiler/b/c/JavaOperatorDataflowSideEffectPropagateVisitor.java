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
/*VCSID=e8afabdf-ba0f-42eb-bc8d-ce5ebc686026*/
package com.sun.max.vm.compiler.b.c;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * This Java Operator Visitor is responsible to propagate the side effects of the Java Operator through the driver provided.
 *
 * @author Yi Guo
 */

public abstract class JavaOperatorDataflowSideEffectPropagateVisitor<T extends AbstractValue<T>> extends HCirOperatorDefaultVisitor {
    protected CirCall _call;
    protected CirVariable _vark;
    protected CirVariable _varek;

    public void visitCall(CirCall call) {
        assert call.procedure() instanceof JavaOperator;
        final CirValue[] arguments = call.arguments();
        assert arguments.length >= 2;

        _call = call;
        _vark = DataflowDriver.getContinuationTarget(arguments[arguments.length - 2]);
        _varek = DataflowDriver.getContinuationTarget(arguments[arguments.length - 1]);

        final JavaOperator op = (JavaOperator) call.procedure();
        op.acceptVisitor(this);
    }

    protected void onSplit(SplitTransformation.Split split, CirVariable vold, CirVariable vnew) {
        driver().flowInto(vold, vnew);
    }

    @Override
    public void visit(SplitTransformation.Split op) {
        final CirVariable vold = (CirVariable) _call.arguments()[0];
        final CirVariable vnew = ((CirContinuation) _call.arguments()[1]).parameters()[0];
        onSplit(op, vold, vnew);
    }

    @Override
    public abstract void visitDefault(JavaOperator op);

    protected abstract DataflowDriver<T> driver();
}
