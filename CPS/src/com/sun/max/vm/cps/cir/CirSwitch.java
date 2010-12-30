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

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The single conditional control flow construct in CIR.
 *
 * Parameter list format for instances of this builtin:
 *
 *     tag, match1, match2, match3, ... match_n, cont1, cont2, cont3, ... cont_n, default cont
 *
 * Example:
 *
 *     switch (x) {
 *         case 1:
 *             f1();
 *             break;
 *         case 2:
 *             f2();
 *             break;
 *         default:
 *             fd();
 *             break;
 *     }
 *
 * ...translates to something like this:
 *
 *     <SwitchBuiltin>(x 1 2 {cont[] f1(cc ce)} {cont[] f2(cc ce)} {cont[] fd(cc ce)})
 *
 * ATTENTION: CirSwitch of Kind WORD interprets all arguments as values of type Address
 *            when applying the ValueComparator.
 *            To generate a CirSwitch for SIGNED Word values (type Offset),
 *            use Kind.LONG or Kind.INT, depending on Word.width() instead!
 *
 * @see WordValue.compareSameKind()
 *
 * @author Bernd Mathiske
 */
public class CirSwitch extends CirProcedure implements CirFoldable, CirReducible {

    private final Kind comparisonKind;
    private final ValueComparator valueComparator;
    private final int numberOfMatches;

    public CirSwitch(Kind comparisonKind, ValueComparator valueComparator, int numberOfMatches) {
        this.comparisonKind = comparisonKind;
        this.valueComparator = valueComparator;
        this.numberOfMatches = numberOfMatches;
    }

    public ValueComparator valueComparator() {
        return valueComparator;
    }

    public Kind comparisonKind() {
        return comparisonKind;
    }

    public int numberOfMatches() {
        return numberOfMatches;
    }

    public Kind resultKind() {
        return Kind.VOID;
    }

    @Override
    public Kind[] parameterKinds() {
        final Kind[] parameterKinds = new Kind[numberOfMatches() * 2];
        int i = 0;
        for (; i <= numberOfMatches(); i++) {
            parameterKinds[i] = comparisonKind;
        }
        for (; i < parameterKinds.length; i++) {
            parameterKinds[i] = Kind.WORD;
        }
        return parameterKinds;
    }

    public String name() {
        return "<" + comparisonKind.name + "_" + valueComparator.name() + "_" + numberOfMatches() + ">";
    }

    @Override
    public String toString() {
        return name();
    }

    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (arguments.length == 2) {
            // Only has the default continuation
            return true;
        }
        final CirValue tag = arguments[0];
        if (!(tag instanceof CirConstant)) {
            return false;
        }
        assert numberOfMatches() == (arguments.length - 2) >> 1;
        for (int i = 1; i <= numberOfMatches(); i++) {
            if (!(arguments[i] instanceof CirConstant)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitSwitch(this);
    }

    public MethodActor foldingMethodActor() {
        throw new UnsupportedOperationException();
    }

    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        if (arguments.length > 2) {
            final CirConstant tag = (CirConstant) arguments[0];
            final Value tagValue = comparisonKind.convert(tag.toStackValue());
            final int n = numberOfMatches();
            assert n == (arguments.length - 2) >> 1;
            for (int i = 1; i <= n; i++) {
                final CirConstant match = (CirConstant) arguments[i];
                final Value matchValue = comparisonKind.convert(match.toStackValue());
                if (valueComparator.evaluate(tagValue, matchValue)) {
                    final CirValue branchContinuation = arguments[n + i];
                    return new CirCall(branchContinuation, CirCall.NO_ARGUMENTS);
                }
            }
        }
        final CirValue defaultContinuation = arguments[arguments.length - 1];
        return new CirCall(defaultContinuation, CirCall.NO_ARGUMENTS);
    }

    public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
        final int n = numberOfMatches();
        final CirValue defaultContinuation = arguments[arguments.length - 1];
        for (int i = 1; i <= n; i++) {
            final CirValue branchContinuation = arguments[n + i];
            if (!branchContinuation.equals(defaultContinuation)) {
                return false;
            }
        }
        return true;
    }

    public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
        final CirValue defaultContinuation = arguments[arguments.length - 1];
        return new CirCall(defaultContinuation, CirCall.NO_ARGUMENTS);
    }

    public static final CirSwitch INT_EQUAL = new CirSwitch(Kind.INT, ValueComparator.EQUAL, 1);
    public static final CirSwitch INT_NOT_EQUAL = new CirSwitch(Kind.INT, ValueComparator.NOT_EQUAL, 1);
    public static final CirSwitch SIGNED_INT_LESS_THAN = new CirSwitch(Kind.INT, ValueComparator.LESS_THAN, 1);
    public static final CirSwitch SIGNED_INT_LESS_EQUAL = new CirSwitch(Kind.INT, ValueComparator.LESS_EQUAL, 1);
    public static final CirSwitch SIGNED_INT_GREATER_EQUAL = new CirSwitch(Kind.INT, ValueComparator.GREATER_EQUAL, 1);
    public static final CirSwitch SIGNED_INT_GREATER_THAN = new CirSwitch(Kind.INT, ValueComparator.GREATER_THAN, 1);
    public static final CirSwitch REFERENCE_EQUAL = new CirSwitch(Kind.REFERENCE, ValueComparator.EQUAL, 1);
    public static final CirSwitch REFERENCE_NOT_EQUAL = new CirSwitch(Kind.REFERENCE, ValueComparator.NOT_EQUAL, 1);

    public static final CirSwitch UNSIGNED_INT_LESS_THAN = new CirSwitch(Kind.INT, ValueComparator.UNSIGNED_LESS_THAN, 1);
    public static final CirSwitch UNSIGNED_INT_LESS_EQUAL = new CirSwitch(Kind.INT, ValueComparator.UNSIGNED_LESS_EQUAL, 1);
    public static final CirSwitch UNSIGNED_INT_GREATER_EQUAL = new CirSwitch(Kind.INT, ValueComparator.UNSIGNED_GREATER_EQUAL, 1);
    public static final CirSwitch UNSIGNED_INT_GREATER_THAN = new CirSwitch(Kind.INT, ValueComparator.UNSIGNED_GREATER_THAN, 1);

    static {
        StaticFieldName.Static.initialize(CirSwitch.class);
    }
}
