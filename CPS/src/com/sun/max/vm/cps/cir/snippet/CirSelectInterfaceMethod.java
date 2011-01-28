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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;

/**
 * We override here to:
 * - enable folding when the method is final, even if the receiver is not constant,
 * - retrieve a CIR method instead of a target method,
 * - resolve builtins,
 * - statically dispatch Accessor methods.
 *
 * @author Bernd Mathiske
 */
public final class CirSelectInterfaceMethod extends CirSnippet {

    @HOSTED_ONLY
    public CirSelectInterfaceMethod() {
        super(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
    }

    private enum Parameter {
        receiver, declaredMethod, normalContinuation, exceptionContinuation;

        public static final List<Parameter> VALUES = Arrays.asList(values());
    }

    @Override
    public boolean mustNotInline(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (MaxineVM.isHosted()) {
            if (isConstantArgument(arguments, Parameter.declaredMethod)) {
                final MethodActor declaredMethod = (MethodActor) getConstantArgumentValue(arguments, Parameter.declaredMethod).asObject();
                // Accessor method selection must never be inlined, instead it must always be folded:
                if (declaredMethod.holder().toJava() == Accessor.class) {
                    return true;
                }
                // Inlining of method selection for methods with accessor arguments must never be attempted:
                for (Class parameterClass : declaredMethod.toJava().getParameterTypes()) {
                    if (parameterClass == Accessor.class) {
                        ProgramError.unexpected("attempted method selection inlining for method with Accessor argument: " + declaredMethod);
                    }
                }
            }
        }
        return super.mustNotInline(cirOptimizer, arguments);
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        assert arguments.length == Parameter.VALUES.size();
        if (isConstantArgument(arguments, Parameter.declaredMethod)) {
            if (isConstantArgument(arguments, Parameter.receiver)) {
                return getConstantArgumentValue(arguments, Parameter.receiver).asObject() != null;
            }
            final MethodActor declaredMethod = (MethodActor) getConstantArgumentValue(arguments, Parameter.declaredMethod).asObject();
            return declaredMethod.holder().toJava() == Accessor.class && cirOptimizer.inliningPolicy().accessorClass() != Accessor.class;
        }
        return false;
    }

    private VirtualMethodActor findAccessorMethodActor(MethodActor declaredMethod, Class<? extends Accessor> javaHolder) {
        assert javaHolder != Accessor.class;
        final TupleClassActor holder = (TupleClassActor) ClassActor.fromJava(javaHolder);
        return holder.findVirtualMethodActor(declaredMethod);
    }

    /**
     * Meta-evaluates only up to the virtual method actor, not all the way to the target method's entry point.
     */
    public static VirtualMethodActor quasiFold(Object receiver, InterfaceMethodActor interfaceMethod) {
        final Class receiverClass = receiver.getClass();
        final ClassActor classActor = ClassActor.fromJava(receiverClass);
        if (MaxineVM.isHosted() && !CPSCompiler.Static.compiler().areSnippetsCompiled()) {
            return classActor.findVirtualMethodActor(interfaceMethod);
        }
        final InterfaceActor interfaceActor = UnsafeCast.asInterfaceActor(interfaceMethod.holder());
        final int interfaceIIndex = classActor.dynamicHub().getITableIndex(interfaceActor.id) - classActor.dynamicHub().iTableStartIndex;
        return classActor.getVirtualMethodActorByIIndex(interfaceIIndex + interfaceMethod.iIndexInInterface());
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        assert arguments.length == Parameter.VALUES.size();
        final InterfaceMethodActor declaredMethod = (InterfaceMethodActor) getConstantArgumentValue(arguments, Parameter.declaredMethod).asObject();
        VirtualMethodActor selectedMethod = null;
        if (isConstantArgument(arguments, Parameter.receiver)) {
            final Object receiver = getConstantArgumentValue(arguments, Parameter.receiver).asObject();
            try {
                selectedMethod = quasiFold(receiver, declaredMethod);
            } catch (LinkageError error) {
                throw new CirFoldingException(error);
            }
        } else if (declaredMethod.holder().toJava() == Accessor.class) {
            selectedMethod = findAccessorMethodActor(declaredMethod, cirOptimizer.inliningPolicy().accessorClass());
        }
        return new CirCall(getNormalContinuation(arguments), builtinOrMethod(selectedMethod, cirOptimizer.cirGenerator()));
    }

}
