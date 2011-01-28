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
package com.sun.max.vm.cps.cir;

import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A closure that is regarded as a continuation. Note that
 * {@link CirConstantBlockArgumentsPropagation} depends on CirContinuations
 * being distinguishable from CirClosures.
 * <p>
 * Continuations always have zero or one parameter. The single parameter
 * will hold the "return value" from the call which is being continued.
 * <p>
 * In a {@link CirPrinter trace}, a continuation is printed as
 * cont[] . { body }
 *
 * @author Bernd Mathiske
 */
public final class CirContinuation extends CirClosure {

    public CirContinuation(CirVariable parameter) {
        setParameters(parameter);
    }

    public CirContinuation() {
        setParameters(NO_PARAMETERS);
    }

    @Override
    public String toString() {
        return "<CirContinuation>";
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitContinuation(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitContinuation(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformContinuation(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateContinuation(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateContinuation(this);
    }

}
