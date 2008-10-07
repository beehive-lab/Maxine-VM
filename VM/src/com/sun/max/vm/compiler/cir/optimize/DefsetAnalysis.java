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
package com.sun.max.vm.compiler.cir.optimize;

import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.optimize.DefsetDomain.*;
import com.sun.max.vm.compiler.cir.variable.*;


public final class DefsetAnalysis extends DataflowDriver<Defset> {
    private final DefsetDomain _domain;
    private final JavaOperatorDataflowSideEffectPropagateVisitor<Defset> _visitor;

    public DefsetAnalysis(DefsetDomain domain) {
        _domain = domain;
        _visitor = new DefsetVisitor();
    }

    private class DefsetVisitor extends JavaOperatorDataflowSideEffectPropagateVisitor<Defset> {
        @Override
        public void visitDefault(JavaOperator op) {
            if (_vark != null) {
                DefsetAnalysis.this.flowInto(domain().fromNode(_call), _vark);
            }
            DefsetAnalysis.this.flowInto(domain().getBottom(), _varek);
        }

        @Override
        protected DefsetAnalysis driver() {
            return DefsetAnalysis.this;
        }
    }

    @Override
    protected void initialize(CirClosure closure) {
        for (CirVariable v : closure.parameters()) {
            _representation.put(v, domain().getBottom());
        }
    }

    @Override
    public void visitCall(CirCall call) {
        final CirValue proc = call.procedure();
        final CirValue[] arguments = call.arguments();

        if (arguments.length >= 2 && (proc instanceof CirMethod || proc instanceof CirBuiltin)) {
            CirVariable v;
            v = DataflowDriver.getContinuationTarget(arguments[arguments.length - 2]);
            if (v != null) {
                super.flowInto(domain().fromNode(call), v);
            }
            v = DataflowDriver.getContinuationTarget(arguments[arguments.length - 1]);
            if (v != null) {
                super.flowInto(domain().getBottom(), v);
            }
        }
        super.visitCall(call);
    }

    @Override
    protected JavaOperatorDataflowSideEffectPropagateVisitor<Defset> javaOperatorDataflowSideEffectPropagateVisitor() {
        return _visitor;
    }

    @Override
    protected DefsetDomain domain() {
        return _domain;
    }
}
