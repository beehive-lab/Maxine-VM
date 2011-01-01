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
import com.sun.max.vm.value.*;

/**
 * A manifest Java value.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class CirConstant extends CirValue {

    private final Value value;

    public CirConstant(Value value) {
        super(value.kind());
        this.value = value;
    }

    public static final CirConstant NULL = new CirConstant(ReferenceValue.NULL);

    /**
     * @return the constant converted to a kind that the Java expression stack uses
     */
    public Value toStackValue() {
        return value().kind().stackKind.convert(value());
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Value value() {
        return value;
    }

    public static CirConstant fromInt(int n) {
        return new CirConstant(IntValue.from(n));
    }

    public static CirConstant fromFloat(float f) {
        return new CirConstant(FloatValue.from(f));
    }

    public static CirConstant fromDouble(double d) {
        return new CirConstant(DoubleValue.from(d));
    }

    public static CirConstant fromObject(Object object) {
        if (object == null) {
            return NULL;
        }
        return new CirConstant(ReferenceValue.from(object));
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CirConstant) {
            final CirConstant constant = (CirConstant) other;
            return value.equals(constant.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static final CirConstant TRUE = new CirConstant(BooleanValue.TRUE);

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitConstant(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitConstant(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformConstant(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateConstant(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateConstant(this);
    }
}
