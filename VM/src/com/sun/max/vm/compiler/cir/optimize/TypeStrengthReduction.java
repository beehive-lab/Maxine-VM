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

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.optimize.TypesetDomain.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * TypeStrengthReduction will remove redundant instanceof/checkcast based on the result of ClassTypeAnalysis.
 *
 * @author Yi Guo
 */
public class TypeStrengthReduction {

    private final ClassTypeAnalysis _analysis;

    public TypeStrengthReduction(ClassTypeAnalysis analysis) {
        _analysis = analysis;
    }

    private class Visitor extends CirVisitor {

        private BytecodeLocation _bytecodeLocation;

        private CirCall call(CirValue proc, CirValue... args) {
            final CirCall call = new CirCall(proc, args);
            call.setBytecodeLocation(_bytecodeLocation);
            return call;
        }

        @Override
        public void visitCall(CirCall call) {
            _bytecodeLocation = call.bytecodeLocation();
            final CirValue op = call.procedure();

            final ClassActor classActor;
            final CirVariable var;

            if (call.procedure() instanceof InstanceOf) {
                if (call.arguments()[0] instanceof CirVariable) {
                    var = (CirVariable) call.arguments()[0];
                } else {
                    var = null;
                }
                classActor = ((InstanceOf) op).actor();
            } else if (call.procedure() instanceof CheckCast) {
                if (call.arguments()[0] instanceof CirVariable) {
                    var = (CirVariable) call.arguments()[0];
                } else {
                    var = null;
                }
                classActor = ((CheckCast) op).actor();
            } else {
                var = null;
                classActor = null;
            }
            if (var == null || classActor == null) {
                super.visitCall(call);
                return;
            }
            boolean defTrue = false;
            boolean defFalse = false;
            final Typeset typeset = _analysis.get(var);

            final StringBuilder st = new StringBuilder();
            st.append("Reducing " + call.procedure() + " var: " + var + ":  ");
            if (typeset == null || typeset.isTop()) {
                st.append("typeset=" + typeset);
            } else {
                if (!typeset.isBottom()) {
                    final Typeset elem = TypesetDomain.singleton.fromInstanceOf(classActor);

                    if (elem.containsAll(typeset)) {
                        defTrue = true;
                    }
                    if (typeset.intersect(elem).isEmpty()) {
                        defFalse = true;
                    }

                }
                st.append(typeset.toString());
                if (!defTrue  && !defFalse) {
                    st.append("  #irreducible#");
                } else if (defTrue) {
                    final CirValue cont = call.arguments()[call.arguments().length - 2];
                    if (op instanceof InstanceOf) {
                        call.assign(call(cont, CirConstant.fromInt(1)));
                    } else {
                        call.assign(call(cont));
                    }
                    st.append("  #reduced-true#");
                } else {
                    final CirValue cont = call.arguments()[call.arguments().length - 2];
                    if (op instanceof InstanceOf) {
                        call.assign(call(cont, CirConstant.fromInt(0)));
                    } else {
                        //TODO: checkcast should throw exeception explicitly
                    }
                    st.append("  #reduced-false#");
                }
            }
            super.visitCall(call);
        }
    }

    public void apply(CirClosure closure) {
        final Visitor visitor = new Visitor();
        CirVisitingTraversal.apply(closure, visitor);
    }
}
