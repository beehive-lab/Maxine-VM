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
package com.sun.max.vm.compiler.cir;

import com.sun.max.lang.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.transform.*;
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

    private final Kind _comparisonKind;
    private final ValueComparator _valueComparator;
    private final int _numberOfMatches;

    public CirSwitch(Kind comparisonKind, ValueComparator valueComparator, int numberOfMatches) {
        _comparisonKind = comparisonKind;
        _valueComparator = valueComparator;
        _numberOfMatches = numberOfMatches;
    }

    public ValueComparator valueComparator() {
        return _valueComparator;
    }

    public Kind comparisonKind() {
        return _comparisonKind;
    }

    public int numberOfMatches() {
        return _numberOfMatches;
    }

    public Kind resultKind() {
        return Kind.VOID;
    }

    @Override
    public Kind[] parameterKinds() {
        final Kind[] parameterKinds = new Kind[numberOfMatches() * 2];
        int i = 0;
        for (; i <= numberOfMatches(); i++) {
            parameterKinds[i] = _comparisonKind;
        }
        for (; i < parameterKinds.length; i++) {
            parameterKinds[i] = Kind.WORD;
        }
        return parameterKinds;
    }

    public String name() {
        return "<" + _comparisonKind.name() + "_" + _valueComparator.name() + "_" + numberOfMatches() + ">";
    }

    @Override
    public String toString() {
        return name();
    }

    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        assert arguments.length >= 2;
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

    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        final CirConstant tag = (CirConstant) arguments[0];
        final Value tagValue = _comparisonKind.convert(tag.toStackValue());
        final int n = numberOfMatches();
        assert n == (arguments.length - 2) >> 1;
        for (int i = 1; i <= n; i++) {
            final CirConstant match = (CirConstant) arguments[i];
            final Value matchValue = _comparisonKind.convert(match.toStackValue());
            if (_valueComparator.evaluate(tagValue, matchValue)) {
                final CirValue branchContinuation = arguments[n + i];
                return new CirCall(branchContinuation, CirCall.NO_ARGUMENTS);
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
