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

import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Something that can be called with arguments.
 *
 * Note that in CIR no call ever returns, because we use CPS.
 * This is one reason why we do not call this class "Function".
 * The other is that everything can potentially have side-effects.
 *
 * @author Bernd Mathiske
 */
public abstract class CirProcedure extends CirValue {

    protected CirProcedure() {
        super(Kind.WORD);
    }

    /**
     * Gets the kinds of the parameters declared by this procedure. This does not include the normal and exception
     * continuation parameters.
     *
     * @return the kinds of the parameters declared by this procedure. A {@code null} value in the returned array
     *         indicates that either a {@link Kind#WORD} or {@link Kind#REFERENCE} argument is accepted for the
     *         corresponding parameter position.
     */
    public abstract Kind[] parameterKinds();

    public static boolean isConstantArgument(CirValue[] arguments, Enum index) {
        return arguments[index.ordinal()].isConstant();
    }

    private static Value getConstantArgumentValue(CirValue[] arguments, int index) {
        final CirConstant cirConstant = (CirConstant) arguments[index];
        return cirConstant.value();
    }

    public static Value getConstantArgumentValue(CirValue[] arguments, Enum index) {
        return getConstantArgumentValue(arguments, index.ordinal());
    }

    public static CirValue getNormalContinuation(CirValue[] arguments) {
        return arguments[arguments.length - 2];
    }

    public static CirValue getExceptionContinuation(CirValue[] arguments) {
        return arguments[arguments.length - 1];
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitProcedure(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitProcedure(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformProcedure(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateProcedure(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateProcedure(this);
    }
}
