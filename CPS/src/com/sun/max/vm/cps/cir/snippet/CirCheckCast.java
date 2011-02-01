/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;

/**
 * Eliminate all checkcasts referring to subtypes of Word.
 * These are gratuitously inserted by javac.
 * They would cause the target VM to crash.
 *
 * @author Bernd Mathiske
 */
public final class CirCheckCast extends CirSnippet {

    @HOSTED_ONLY
    public CirCheckCast() {
        super(Snippet.CheckCast.SNIPPET);
    }

    private enum Parameter {
        classActor, object, normalContinuation, exceptionContinuation
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (isConstantArgument(arguments, Parameter.classActor)) {
            final ClassActor classActor = (ClassActor) getConstantArgumentValue(arguments, Parameter.classActor).asObject();
            if (Word.class.isAssignableFrom(classActor.toJava())) {
                return true;
            }
        }
        return super.isFoldable(cirOptimizer, arguments);
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        if (isConstantArgument(arguments, Parameter.classActor)) {
            final ClassActor classActor = (ClassActor) getConstantArgumentValue(arguments, Parameter.classActor).asObject();
            if (Word.class.isAssignableFrom(classActor.toJava())) {
                return new CirCall(getNormalContinuation(arguments), CirCall.NO_ARGUMENTS);
            }
        }
        return super.fold(cirOptimizer, arguments);
    }

}
