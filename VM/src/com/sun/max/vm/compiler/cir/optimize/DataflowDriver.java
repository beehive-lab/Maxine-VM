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

import java.util.*;

import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * This class is the driver for iterative data flow analysis.
 * There are two type parameters: Domain<D> and AbstractValue<T>.
 * To implement a real data flow analysis, The user needs to sub-class the driver with appropriate domain, and
 * implement makeJavaOperatorVisitor method to provide a JavaOperatorVisitor for each call visited. This visitor
 * is responsible to propagate the side effects of all java operators.
 *
 * We also assume free variables have been captured and are in the parameters list of the outermost closure.
 * All free variables are set to bottom.
 *
 * @see HCirDataFlowVisitor, DefsetAnalysis, ClassTypeAnalysis
 * @author Yi Guo
 */
public abstract class DataflowDriver<T extends AbstractValue<T>> extends CirVisitor implements DataflowAnalysis<T> {
    protected final Map<CirVariable, T> _representation = new HashMap<CirVariable, T>();
    protected boolean _complete;

    /**
     * Flow Abstract Value in to the Variable to.
     * If variable to is not initilialized, then set to AbstractValue in
     *
     * aka. AV(to) = AV(to) meet in
     *
     * @param in
     * @param to
     */
    public void flowInto(T in, CirVariable to) {
        final T out;

        if (!_representation.containsKey(to)) {
            out = domain().getTop();
            _representation.put(to, out);
        } else {
            out = _representation.get(to);
        }

        final T newV = domain().meet(out, in);
        if (domain().equal(newV, out) == false) {
            _representation.put(to, newV);
            _complete = false;
        }
    }

    /**
     * Flow the abstract value of variable in to variable to.
     * aka. AV(to) = AV(to) meet AV(in)
     *
     * @param from
     * @param to
     */
    public void flowInto(CirValue from, CirVariable to) {
        assert from instanceof CirVariable || from instanceof CirConstant;

        final T in;
        if (from instanceof CirVariable) {
            in = _representation.get(from);
            if (in == null) {
                return;
            }
        } else {
            assert from instanceof CirConstant : "Cannot flow " + from + " into " + to;
            in = domain().fromConstant((CirConstant) from);
        }

        flowInto(in, to);
    }

    /**
     * Flow the abstract value.
     *
     * Current we assume only CirContinuation or CirContinuationVariable can be bound to CirContinuationVariable which
     * is true for the Cir generated from java bytecode.
     *
     */

    @Override
    public void visitCall(CirCall call) {
        final CirValue proc = call.procedure();
        final CirValue[] arguments = call.arguments();
        if (proc instanceof JavaOperator) {
            javaOperatorDataflowSideEffectPropagateVisitor().visitCall(call);
        } else if (proc instanceof CirClosure || proc instanceof CirBlock) {
            /* CirClosure can only be called once statically. see CirBlock */
            final CirClosure closure;
            if (proc instanceof CirClosure) {
                closure = (CirClosure) proc;
            } else {
                closure = ((CirBlock) proc).closure();
            }

            for (int i = 0; i < arguments.length; i++) {
                final CirVariable var = closure.parameters()[i];
                final CirValue val = arguments[i];

                if (var instanceof CirContinuationVariable) {
                    assert val instanceof CirContinuationVariable || val instanceof CirContinuation;
                    if (val instanceof CirContinuationVariable) {
                        flowInto(var, (CirVariable) val);
                    } else {
                        final CirContinuation cont = (CirContinuation) val;
                        if (cont.parameters().length > 0) {
                            flowInto(var, cont.parameters()[0]);
                        }
                    }
                } else {
                    assert val instanceof CirConstant || val instanceof CirVariable;
                    flowInto(val, var);
                }
            }
        }
    }

    /**
     * get the abstract value.
     *
     */
    public T get(CirNode v) {
        return _representation.get(v);
    }

    protected void initialize(CirClosure closure) {
        for (CirVariable v : closure.parameters())  {
            _representation.put(v, domain().getBottom());
        }
    }


    public void apply(CirClosure closure) {
        initialize(closure);
        _complete = false;

        while (!_complete) {
            _complete = true;
            //TODO: Should be a Reverse-Postorder visitor for fast convergence
            CirVisitingTraversal.apply(closure, this);
        }
    }

    public static final CirVariable getContinuationTarget(CirValue argument) {
        if (argument == CirValue.UNDEFINED) {
            return null;
        }
        if (argument instanceof CirContinuationVariable) {
            return (CirContinuationVariable) argument;
        }
        assert argument instanceof CirContinuation : argument;
        final CirContinuation cont = (CirContinuation) argument;
        if (cont.parameters().length == 1) {
            return cont.parameters()[0];
        }
        return null;
    }

    // Checkstyle: stop
    protected abstract AbstractValueDomain<T> domain();
    // Checkstyle: resume
    protected abstract JavaOperatorDataflowSideEffectPropagateVisitor<T> javaOperatorDataflowSideEffectPropagateVisitor();
}
