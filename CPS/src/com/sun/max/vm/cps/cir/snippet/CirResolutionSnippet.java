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
package com.sun.max.vm.cps.cir.snippet;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public final class CirResolutionSnippet extends CirSnippet {

    @HOSTED_ONLY
    public CirResolutionSnippet(ResolutionSnippet snippet) {
        super(snippet);
    }

    private enum Parameter {
        guard, normalContinuation, exceptionContinuation;
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (!super.isFoldable(cirOptimizer, arguments)) {
            // This occurs when compiling the stub for folding a snippet
            return false;
        }
        final ResolutionGuard guard = (ResolutionGuard) arguments[Parameter.guard.ordinal()].value().asObject();
        if (guard.value != null) {
            return true;
        }
        final ConstantPool constantPool = guard.constantPool;
        final ResolvableConstant resolvableConstant = constantPool.resolvableAt(guard.constantPoolIndex);
        if (resolvableConstant.isResolvableWithoutClassLoading(constantPool)) {
            try {
                resolvableConstant.resolve(constantPool, guard.constantPoolIndex);
                return true;
            } catch (LinkageError linkageError) {
                // Whatever went wrong here is supposed to show up at runtime, too.
                // So we just don't fold here and move on.
            }
        }
        return false;
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        CirCall call = super.fold(cirOptimizer, arguments);
        if (snippet == ResolveSpecialMethod.SNIPPET || snippet == ResolveStaticMethod.SNIPPET) {
            CirValue[] callArguments = call.arguments();
            ClassMethodActor classMethodActor = (ClassMethodActor) callArguments[0].value().asObject();
            callArguments[0] = builtinOrMethod(classMethodActor, cirOptimizer.cirGenerator());
        }
        return call;
    }
}
