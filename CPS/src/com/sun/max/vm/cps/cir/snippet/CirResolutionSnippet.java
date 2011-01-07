/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
        final ResolutionGuard.InPool guard = (ResolutionGuard.InPool) arguments[Parameter.guard.ordinal()].value().asObject();
        if (guard.value != null) {
            return true;
        }
        final ConstantPool constantPool = guard.pool;
        final ResolvableConstant resolvableConstant = constantPool.resolvableAt(guard.cpi);
        if (resolvableConstant.isResolvableWithoutClassLoading(constantPool)) {
            try {
                resolvableConstant.resolve(constantPool, guard.cpi);
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
