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
package com.sun.max.vm.compiler.eir;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */

public final class EirVariable extends EirValue implements Comparable<EirVariable>, PoolObject {
    private int _serial;

    public int serial() {
        return _serial;
    }

    /**
     * (tw) Be careful with using this method! All pool sets with this variable become immediately invalid!
     * @param serial
     */
    public void setSerial(int serial) {
        _serial = serial;
    }

    private final Kind _kind;


    @Override
    public EirVariable asVariable() {
        return this;
    }

    @Override
    public Kind kind() {
        return _kind;
    }

    public boolean isReferenceCompatibleWith(EirValue other) {
        return (_kind == Kind.REFERENCE) == (other.kind() == Kind.REFERENCE);
    }

    private int _weight;

    public int weight() {
        return _weight;
    }

    public void setWeight(int weight) {
        _weight = weight;
    }

    public boolean isSpillingPrevented() {
        return _weight == Integer.MAX_VALUE;
    }

    public EirVariable(Kind kind, int serial) {
        _serial = serial;
        _kind = kind;
    }

    public int compareTo(EirVariable other) {
        return Ints.compare(other._weight, _weight); // backwards so that variables with higher weight come first
    }

    private EirLiveRange _liveRange;

    public void resetLiveRange() {
        _liveRange = new EirBitSetLiveRange(this);
    }

    public EirLiveRange liveRange() {
        return _liveRange;
    }

    @Override
    public void recordDefinition(EirOperand operand) {
        _liveRange.recordDefinition(operand);
    }

    @Override
    public void recordUse(EirOperand operand) {
        _liveRange.recordUse(operand);
        if (_aliasedVariables != null) {
            for (EirVariable variable : _aliasedVariables) {
                variable.recordUse(operand);
            }
        }
    }

    private PoolSet<EirVariable> _interferingVariables;

    public void resetInterferingVariables(PoolSet<EirVariable> emptyVariableSet) {
        _interferingVariables = emptyVariableSet.clone();
    }

    public PoolSet<EirVariable> interferingVariables() {
        return _interferingVariables;
    }

    public void beInterferingWith(EirVariable other) {
        if (other != this) {
            _interferingVariables.add(other);
            other._interferingVariables.add(this);
        }
    }

    public void beNotInterferingWith(EirVariable other) {
        _interferingVariables.remove(other);
        other._interferingVariables.remove(this);
    }

    public void determineInterferences() {
        for (EirOperand operand : operands()) {
            switch (operand.effect()) {
                case USE: {
                    break;
                }
                case UPDATE:
                case DEFINITION: {
                    final EirInstruction<?, ?> instruction = operand.instruction();
                    for (EirVariable variable : instruction.liveVariables()) {
                        if (variable != this) {
                            beInterferingWith(variable);
                        }
                    }
                    break;
                }
            }
        }
    }

    private boolean hasInterferingDefinitions(EirVariable variable) {
        if (variable == this) {
            return false;
        }
        for (EirOperand operand : variable.operands()) {
            switch (operand.effect()) {
                case USE: {
                    break;
                }
                case UPDATE:
                case DEFINITION: {
                    final EirInstruction<?, ?> instruction = operand.instruction();
                    if (_liveRange.contains(instruction)) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    public boolean isInterferingWith(EirVariable variable) {
        return hasInterferingDefinitions(variable) || variable.hasInterferingDefinitions(this);
    }

    public void updateInterferences() {
        for (EirVariable variable : interferingVariables().clone()) {
            if (isInterferingWith(variable)) {
                beInterferingWith(variable);
            } else {
                beNotInterferingWith(variable);
            }
        }
    }

    public void recomputeLiveRange() {
        _liveRange = new EirBitSetLiveRange(this);
        _liveRange.compute();
    }

    public boolean isLiveRangeIntact() {
        final EirLiveRange oldLiveRange = _liveRange;
        _liveRange = new EirBitSetLiveRange(this);
        _liveRange.compute();
        return oldLiveRange.equals(_liveRange);
    }

    public boolean areInterferencesIntact(EirMethodGeneration methodGeneration) {
        final PoolSet<EirVariable> nonInterferingVariables = PoolSet.allOf(methodGeneration.variablePool());
        for (EirVariable variable : _interferingVariables) {
            if (!isInterferingWith(variable)) {
                traceLiveRange(variable);
                return false;
            }
            nonInterferingVariables.remove(variable);
        }
        for (EirVariable variable : nonInterferingVariables) {
            if (isInterferingWith(variable)) {
                traceLiveRange(variable);
                return false;
            }
        }
        return true;
    }

    private void traceLiveRange(EirVariable variable) {
        if (Trace.hasLevel(1)) {
            Trace.line(1, "liveRange of " + this + ": " + liveRange());
            Trace.line(1, "liveRange of " + variable + ": " + variable.liveRange());
        }
    }

    private VariableDeterministicSet<EirVariable> _aliasedVariables;

    /**
     * Establishes the relationship between this variable and another whereby the
     * former is an alias for the latter. When this variable is determined during
     * register allocation to be {@linkplain #recordUse(EirOperand) used},
     * the aliased variable is also marked as used thus ensuring it won't be
     * re-allocated.
     */
    public void setAliasedVariable(EirVariable aliasedVariable) {
        if (_aliasedVariables == null) {
            _aliasedVariables = new LinkedIdentityHashSet<EirVariable>();
        }
        assert aliasedVariable != this;
        _aliasedVariables.add(aliasedVariable);
    }

    /**
     * @see #setAliasedVariable(EirVariable)
     */
    public DeterministicSet<EirVariable> aliasedVariables() {
        return _aliasedVariables;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        _liveRange = null;
        _interferingVariables = null;
        _aliasedVariables = null;
    }

    @Override
    public String toString() {
        String s = _kind.character() + "$" + _serial;
        if (location() != null) {
            s += "@" + location();
        }
        return s;
    }

    private Interval _interval;

    public void setInterval(Interval interval) {
        _interval = interval;
    }

    public Interval interval() {
        return _interval;
    }
}
