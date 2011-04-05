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
package com.sun.max.vm.cps.cir;

import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Representation of runtime values in CIR.
 *
 * @author Bernd Mathiske
 */
public abstract class CirValue extends CirNode implements IrValue {

    private final Kind kind;

    public Kind kind() {
        return kind;
    }

    protected CirValue(Kind kind) {
        this.kind = kind;
    }

    public boolean isCategory1() {
        return kind().isCategory1;
    }

    public boolean isCategory2() {
        return !kind().isCategory1;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitValue(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitValue(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformValue(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateValue(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateValue(this);
    }

    /**
     * Determines if all but the last two arguments (i.e. the continuation values)
     * in a CIR argument array are constant.
     */
    public static boolean areConstant(CirValue[] arguments) {
        for (int i = 0; i < arguments.length - 2; i++) {
            if (!arguments[i].isConstant()) {
                return false;
            }
        }
        return true;
    }

    public boolean isConstant() {
        return false;
    }

    public final boolean isScalarConstant() {
        if (isConstant()) {
            if (kind() != Kind.REFERENCE) {
                return true;
            }
            final CirConstant constant = (CirConstant) this;
            return constant.value().isZero();
        }
        return false;
    }

    public boolean isResolvedOffset() {
        return false;
    }

    public boolean isResolvedFieldConstant() {
        return false;
    }

    public Value value() {
        throw new RuntimeException(this + " is not a value, but a " + getClass());
    }

    public static final class Undefined extends CirValue {
        private Undefined() {
            super(Kind.VOID);
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitUndefined(this);
        }

        @Override
        public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
            visitor.visitUndefined(this, scope);
        }

        @Override
        public String toString() {
            return "UNDEFINED";
        }
    }

    public static final Undefined UNDEFINED = new Undefined();
}
