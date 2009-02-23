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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.snippet.*;
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
public final class CirSelectVirtualMethod extends CirSpecialSnippet {

    public CirSelectVirtualMethod() {
        super(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
    }

    private enum Parameter {
        receiver, declaredVirtualMethodActor, normalContinuation, exceptionContinuation;

        public static final IndexedSequence<Parameter> VALUES = new ArraySequence<Parameter>(values());
    }

    @Override
    public boolean mustNotInline(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (MaxineVM.isPrototyping()) {
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
        assert arguments.length == Parameter.VALUES.length();
        return true;
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        final CirGenerator cirGenerator = cirOptimizer.cirGenerator();
        assert arguments.length == Parameter.VALUES.length();
        if (isConstantArgument(arguments, Parameter.declaredVirtualMethodActor)) {
            final VirtualMethodActor declaredMethod = (VirtualMethodActor) getConstantArgumentValue(arguments, Parameter.declaredVirtualMethodActor).asObject();
            if (declaredMethod.isFinal() || declaredMethod.holder().isFinal() || declaredMethod.holder().kind() == Kind.WORD) {
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
