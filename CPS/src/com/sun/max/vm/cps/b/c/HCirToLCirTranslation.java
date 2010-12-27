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
package com.sun.max.vm.cps.b.c;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Transforms a HCIR tree (containing high-level CIR operators) to the corresponding
 * LCIR tree (containing builtins and snippets).  It first {@linkplain Verifier verifies}
 * that the HCIR input is well formed then does the actual translation with
 * the help of {@link HCirOperatorLowering} for CIR nodes of the form of a call
 * to an {@link JavaOperator}.
 *
 * @author Yi Guo
 * @author Aziz Ghuloum
 */
public class HCirToLCirTranslation {
    public static final class Visitor extends CirVisitor {
        private BirToCirMethodTranslation methodTranslation;
        Visitor(BirToCirMethodTranslation methodTranslation) {
            this.methodTranslation = methodTranslation;
        }

        @Override
        public void visitCall(CirCall call) {
            final CirValue procedure = call.procedure();
            if (procedure instanceof JavaOperator) {
                final JavaOperator operator = (JavaOperator) procedure;
                final Kind[] parameterKinds = operator.parameterKinds();
                assert call.arguments().length == parameterKinds.length + 2;
                final HCirOperatorLowering visitor = new HCirOperatorLowering(operator, call, methodTranslation);
                operator.acceptVisitor(visitor);
            }
        }

        public CirVariableFactory variableFactory() {
            return methodTranslation.variableFactory();
        }
    }

    public static final class Verifier extends CirVisitor {
        @Override
        public void visitCall(CirCall call) {
            final CirValue procedure = call.procedure();
            if (procedure instanceof CirClosure ||
                procedure instanceof JavaOperator ||
                procedure instanceof CirBlock ||
                procedure instanceof CirVariable ||
                procedure instanceof CirSwitch) {
                // ok
            } else {
                FatalError.unexpected("invalid operator in HCIR " + (procedure instanceof CirBuiltin ? ((CirBuiltin) procedure).builtin : procedure));
            }
        }
    }

    public static void apply(BirToCirMethodTranslation methodTranslation) {
        CirVisitingTraversal.apply(methodTranslation.cirClosure(), new Verifier());
        final Visitor translator = new Visitor(methodTranslation);
        CirVisitingTraversal.apply(methodTranslation.cirClosure(), translator);
    }
}
