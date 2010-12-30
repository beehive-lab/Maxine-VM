/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;

/**
 * We override here to retrieve a CIR method instead of a code address.
 *
 * @author Doug Simon
 */
public final class CirMakeEntrypoint extends CirSnippet {

    @HOSTED_ONLY
    public CirMakeEntrypoint() {
        super(MakeEntrypoint.SNIPPET);
    }

    private enum Parameter {
        classMethodActor, normalContinuation, exceptionContinuation;
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        final CirValue classMethodActorArgument = arguments[Parameter.classMethodActor.ordinal()];
        final ClassMethodActor classMethodActor;
        if (classMethodActorArgument instanceof CirMethod) {
            classMethodActor = ((CirMethod) classMethodActorArgument).classMethodActor();
        } else {
            classMethodActor = (ClassMethodActor) getConstantArgumentValue(arguments, Parameter.classMethodActor).asObject();
        }
        return new CirCall(getNormalContinuation(arguments), builtinOrMethod(classMethodActor, cirOptimizer.cirGenerator()));
    }
}
