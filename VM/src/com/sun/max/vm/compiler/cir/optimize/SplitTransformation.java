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

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * Split transformation changes the code shape for path-sensitive analysis by inserting split operators.
 * Each split operator is attached to a branch of a call to a CirSwitch.
 *
 * The input to the split operator is called vold and the output of the split operator is vnew. The split operator creates a
 * new variable (vnew) in which the path-sensitive analysis can save the conditional abstract value of vold if the
 * branch specified in the split operator is taken.
 *
 * Split operators are removed again during HCir->LCir lowering.
 *
 * @author Yi Guo
 */
public final class SplitTransformation {

    public static class Split extends JavaOperator {

        @Override
        public boolean needsJavaFrameDescriptor() {
            return false;
        }

        @Override
        public Kind[] parameterKinds() {
            assert false : "unimplemented";
            return null;
        }

        /**
         * @param op: the call to a CirSwitch the operator is attached to
         * @param branch: the branch of the CirSwitch the operator is attached to.
         *
         * Note that branch = 0 means the default branch. switch (v) { p1 : cont1 // branch order = 1 p2 : cont2 //
         * branch order = 2 default : defaultCont //branch order = 0 }
         */
        public Split(CirCall switchCall, int branch) {
            assert switchCall.procedure() instanceof CirSwitch;
            _switchCall = switchCall;
            _branchOrder = branch;
        }

        private final CirCall _switchCall;
        private final int _branchOrder;

        @Override
        public String toString() {
            return "<split_br" + branchOrder() + ">";
        }

        @Override
        public Kind resultKind() {
            return switchCall().procedure().kind();
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitHCirOperator(this);
        }

        @Override
        public void acceptVisitor(HCirOperatorVisitor visitor) {
            visitor.visit(this);
        }

        public CirCall switchCall() {
            return _switchCall;
        }

        public int branchOrder() {
            return _branchOrder;
        }
    }

    private final BirToCirMethodTranslation _methodTranslation;

    private SplitTransformation(BirToCirMethodTranslation methodTranslation) {
        _methodTranslation = methodTranslation;
    }

    private void run() {
        assert _methodTranslation.cirClosure() != null;
        CirVisitingTraversal.apply(_methodTranslation.cirClosure(), new Transform());
    }

    public static void apply(BirToCirMethodTranslation methodTranslation) {
        final SplitTransformation splitTransformation = new SplitTransformation(methodTranslation);
        splitTransformation.run();
    }

    private BytecodeLocation _bytecodeLocation;

    private static CirClosure lambda(CirVariable arg, CirCall body) {
        if (arg == null) {
            return new CirClosure(body, CirClosure.NO_PARAMETERS);
        }
        return new CirClosure(body, arg);
    }

    private static CirContinuation lambda_k(CirVariable arg, CirCall body) {
        CirContinuation k;
        if (arg == null) {
            k = new CirContinuation();
        } else {
            k = new CirContinuation(arg);
        }
        k.setBody(body);
        return k;
    }

    private CirCall call(CirValue proc, CirValue... args) {
        final CirCall call = new CirCall(proc, args);
        call.setBytecodeLocation(_bytecodeLocation);
        return call;
    }

    private class Transform extends CirVisitor {

        private final Set<CirNode> _visited = new HashSet<CirNode>();

        @Override
        public void visitCall(CirCall oldCall) {
            if (oldCall.procedure() instanceof CirSwitch) {
                final CirSwitch op = (CirSwitch) (oldCall.procedure());
                if (_visited.contains(oldCall)) {
                    super.visitCall(oldCall);
                    return;
                }
                _bytecodeLocation = oldCall.bytecodeLocation();

                final CirExceptionContinuationParameter ce = (CirExceptionContinuationParameter) _methodTranslation.variableFactory().exceptionContinuationParameter();

                CirCall callBuilder = new CirCall();
                callBuilder.assign(oldCall);
                _visited.add(callBuilder);
                final CirCall switchCall = callBuilder;
                for (int i = 1; i <= op.numberOfMatches() + 1; i++) {
                    final CirContinuation branContinuation = (CirContinuation) oldCall.arguments()[i + op.numberOfMatches()];
                    final CirCall blockCall = branContinuation.body();
                    assert blockCall.procedure() instanceof CirBlock;

                    final Split s;
                    if (i == op.numberOfMatches() + 1) {
                        // default branch
                        s = new Split(switchCall, 0);
                    } else {
                        s = new Split(switchCall, i);
                    }

                    for (int j = 0; j < blockCall.arguments().length; j++) {
                        final CirValue[] arguments = blockCall.arguments();
                        final CirValue val = arguments[j];
                        if (val instanceof CirVariable && !(val instanceof CirContinuationVariable)) {
                            final CirVariable vold = (CirVariable) blockCall.arguments()[j];
                            if (true) {
                                // TODO: we only need to split the variables that may be interested to
                                // path-sensitive analysis
                                final CirVariable vnew = _methodTranslation.variableFactory().createTemporary(vold.kind());
                                arguments[j] = vnew;
                                callBuilder = call(s, vold, lambda_k(vnew, callBuilder), ce);
                            }
                        }
                    }
                }
                oldCall.assign(callBuilder);
            }
            super.visitCall(oldCall);
        }

    }

}
