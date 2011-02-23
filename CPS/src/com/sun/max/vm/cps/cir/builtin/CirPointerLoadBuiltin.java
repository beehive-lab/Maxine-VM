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
package com.sun.max.vm.cps.cir.builtin;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;

/**
 * @author Bernd Mathiske
 */
public class CirPointerLoadBuiltin extends CirSpecialBuiltin {

    @HOSTED_ONLY
    protected CirPointerLoadBuiltin(PointerLoadBuiltin builtin) {
        super(builtin);
    }

    @Override
    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        final CirValue normalContinuation = arguments[arguments.length - 2];
        if (normalContinuation instanceof CirClosure) {
            final CirClosure closure = (CirClosure) normalContinuation;
            final CirCall body = closure.body();
            return !CirSearch.OutsideBlocks.contains(body, closure.parameters()[0]);
        }
        return false;
    }

    @Override
    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        final CirValue normalContinuation = arguments[arguments.length - 2];
        final CirClosure closure = (CirClosure) normalContinuation;
        return closure.body();
    }

    /**
     * After optimizing CIR, given a direct reference scheme, we may find objects/references in the position of pointers.
     * If this occurs at a foldable variant of a pointer load instruction,
     * then we can deduce a field access that we can meta-evaluate on the host VM.
     * Typically, this is the case when we access the 'offset' field of a FieldActor.
     */
    @HOSTED_ONLY
    @Override
    public boolean isHostFoldable(CirValue[] arguments) {
        return arguments[0].kind().isReference && CirValue.areConstant(arguments);
    }
}
