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
