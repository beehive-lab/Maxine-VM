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
/*VCSID=4305307f-3a52-4de3-8a42-70e0d94ac104*/
package com.sun.max.vm.compiler.b.c;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Transforms an HCir tree (containing high-level Cir operators) to the corresponding
 * LCir tree (containing builtins and snippets).  It first verifies that the HCir input
 * is well formed by applying the Verifier (below) then does the actual translation with
 * the help of {@link HCirOperatorLoweringVisitor} for Cir nodes of the form of a call
 * to an {@link JavaOperator}.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public class HCirToLCirTranslation {
    public static final class Visitor extends CirVisitor {
        private BirToCirMethodTranslation _methodTranslation;
        private Visitor(BirToCirMethodTranslation methodTranslation) {
            _methodTranslation = methodTranslation;
        }

        @Override
        public void visitCall(CirCall call) {
            final CirValue proc = call.procedure();
            if (proc instanceof JavaOperator) {
                final JavaOperator op = (JavaOperator) proc;
                final HCirOperatorLoweringVisitor visitor = new HCirOperatorLoweringVisitor(call, _methodTranslation.variableFactory(), _methodTranslation.cirGenerator().compilerScheme());
                op.acceptVisitor(visitor);
            }
        }

        public CirVariableFactory variableFactory() {
            return _methodTranslation.variableFactory();
        }
    }

    public static final class Verifier extends CirVisitor {
        @Override
        public void visitCall(CirCall call) {
            final CirValue op = call.procedure();
            assert op instanceof CirClosure ||
                   op instanceof JavaOperator ||
                   op instanceof CirBlock ||
                   op instanceof CirVariable ||
                   op instanceof CirSwitch
               : "invalid operator in HCIR " + (op instanceof CirBuiltin ? ((CirBuiltin) op).builtin() : op);
        }
    }

    public static void apply(BirToCirMethodTranslation methodTranslation) {
        CirVisitingTraversal.apply(methodTranslation.cirClosure(), new Verifier());
        final Visitor translator = new Visitor(methodTranslation);
        CirVisitingTraversal.apply(methodTranslation.cirClosure(), translator);
    }
}
