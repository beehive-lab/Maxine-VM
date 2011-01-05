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
package com.sun.max.vm.cps.cir.snippet;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.type.*;

/**
 * We override here to:
 * - enable folding when the method is final, even if the receiver is not constant,
 * - retrieve a CIR method instead of a target method,
 * - resolve builtins,
 * - statically select Word methods.
 *
 * @author Bernd Mathiske
 */
public final class CirSelectVirtualMethod extends CirSnippet {

    @HOSTED_ONLY
    public CirSelectVirtualMethod() {
        super(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
    }

    private enum Parameter {
        receiver, declaredVirtualMethodActor, normalContinuation, exceptionContinuation;

        public static final List<Parameter> VALUES = Arrays.asList(values());
    }

    @Override
    public boolean mustNotInline(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (MaxineVM.isHosted()) {
            if (isConstantArgument(arguments, Parameter.declaredVirtualMethodActor)) {
                final MethodActor declaredMethod = (MethodActor) getConstantArgumentValue(arguments, Parameter.declaredVirtualMethodActor).asObject();
                // Inlining of method selection for methods with accessor arguments must never be attempted:
                for (Class parameterClass : declaredMethod.toJava().getParameterTypes()) {
                    if (parameterClass == Accessor.class) {
                        ProgramError.unexpected("attempted method selection inlining for method with Accessor argument");
                    }
                }
            }
        }
        return super.mustNotInline(cirOptimizer, arguments);
    }

    @Override
    public Kind[] parameterKinds() {
        return new Kind[] {null, Kind.REFERENCE};
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        assert arguments.length == Parameter.VALUES.size();
        return true;
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        final CirGenerator cirGenerator = cirOptimizer.cirGenerator();
        assert arguments.length == Parameter.VALUES.size();
        if (isConstantArgument(arguments, Parameter.declaredVirtualMethodActor)) {
            final VirtualMethodActor declaredMethod = (VirtualMethodActor) getConstantArgumentValue(arguments, Parameter.declaredVirtualMethodActor).asObject();
            if (declaredMethod.isFinal() || declaredMethod.holder().isFinal() || declaredMethod.holder().kind.isWord) {
                return new CirCall(getNormalContinuation(arguments), builtinOrMethod(declaredMethod, cirGenerator));
            }
            if (isConstantArgument(arguments, Parameter.receiver)) {
                assert ((CirConstant) arguments[Parameter.receiver.ordinal()]).kind() != Kind.WORD;
                final Object receiver = getConstantArgumentValue(arguments, Parameter.receiver).asObject();
                if (receiver == null) {
                    throw new CirFoldingException(new NullPointerException());
                }
                final VirtualMethodActor selectedMethod = MethodSelectionSnippet.SelectVirtualMethod.quasiFold(receiver, declaredMethod);
                return new CirCall(getNormalContinuation(arguments), builtinOrMethod(selectedMethod, cirGenerator));
            }
        }
        final CirCall call = inline(cirOptimizer, arguments, NO_JAVA_FRAME_DESCRIPTOR);
        CirBuiltinVariantOptimization.apply(cirGenerator, call.procedure(), CirBuiltinVariantOptimization.Variant.FOLDABLE, cirOptimizer.cirMethod());
        return call;
    }

}
