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
package com.sun.max.vm.compiler.cir.snippet;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * We override here to:
 * - enable folding when the method is final, even if the receiver is not constant,
 * - retrieve a CIR method instead of a target method,
 * - resolve builtins,
 * - statically dispatch Accessor methods.
 *
 * @author Bernd Mathiske
 */
public final class CirSelectInterfaceMethod extends CirSpecialSnippet {

    public CirSelectInterfaceMethod() {
        super(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
    }

    private enum Parameter {
        receiver, declaredMethod, normalContinuation, exceptionContinuation;

        public static final IndexedSequence<Parameter> VALUES = new ArraySequence<Parameter>(values());
    }

    @Override
    public boolean mustNotInline(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (MaxineVM.isPrototyping()) {
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
        assert arguments.length == Parameter.VALUES.length();
        if (isConstantArgument(arguments, Parameter.declaredMethod)) {
            if (isConstantArgument(arguments, Parameter.receiver)) {
                return true;
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

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        assert arguments.length == Parameter.VALUES.length();
        final InterfaceMethodActor declaredMethod = (InterfaceMethodActor) getConstantArgumentValue(arguments, Parameter.declaredMethod).asObject();
        VirtualMethodActor selectedMethod = null;
        if (isConstantArgument(arguments, Parameter.receiver)) {
            final Object receiver = getConstantArgumentValue(arguments, Parameter.receiver).asObject();
            try {
                selectedMethod = MethodSelectionSnippet.SelectInterfaceMethod.quasiFold(receiver, declaredMethod);
            } catch (LinkageError error) {
                throw new CirFoldingException(error);
            }
        } else if (declaredMethod.holder().toJava() == Accessor.class) {
            selectedMethod = findAccessorMethodActor(declaredMethod, cirOptimizer.inliningPolicy().accessorClass());
        }
        return new CirCall(getNormalContinuation(arguments), builtinOrMethod(selectedMethod, cirOptimizer.cirGenerator()));
    }

}
