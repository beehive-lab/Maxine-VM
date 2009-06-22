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
    private int serial;

    public int serial() {
        return serial;
    }

    /**
     * (tw) Be careful with using this method! All pool sets with this variable become immediately invalid!
     * @param serial
     */
    public void setSerial(int serial) {
        this.serial = serial;
    }

    private final Kind kind;


    @Override
    public EirVariable asVariable() {
        return this;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    public boolean isReferenceCompatibleWith(EirValue other) {
        return (kind == Kind.REFERENCE) == (other.kind() == Kind.REFERENCE);
    }

    private int weight;

    public int weight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isSpillingPrevented() {
        return weight == Integer.MAX_VALUE;
    }

    public EirVariable(Kind kind, int serial) {
        this.serial = serial;
        this.kind = kind;
    }

    public int compareTo(EirVariable other) {
        return Ints.compare(other.weight, weight); // backwards so that variables with higher weight come first
    }

    private EirLiveRange liveRange;

    public void resetLiveRange() {
        liveRange = new EirBitSetLiveRange(this);
    }

    public EirLiveRange liveRange() {
        return liveRange;
    }

    @Override
    public void recordDefinition(EirOperand operand) {
        liveRange.recordDefinition(operand);
    }

    @Override
    public void recordUse(EirOperand operand) {
        liveRange.recordUse(operand);
        if (aliasedVariables != null) {
            for (EirVariable variable : aliasedVariables) {
                variable.recordUse(operand);
            }
        }
    }

    private PoolSet<EirVariable> interferingVariables;

    public void resetInterferingVariables(PoolSet<EirVariable> emptyVariableSet) {
        interferingVariables = emptyVariableSet.clone();
    }

    public PoolSet<EirVariable> interferingVariables() {
        return interferingVariables;
    }

    public void beInterferingWith(EirVariable other) {
        if (other != this) {
            interferingVariables.add(other);
            other.interferingVariables.add(this);
        }
    }

    public void beNotInterferingWith(EirVariable other) {
        interferingVariables.remove(other);
        other.interferingVariables.remove(this);
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
                    if (liveRange.contains(instruction)) {
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
        liveRange = new EirBitSetLiveRange(this);
        liveRange.compute();
    }

    public boolean isLiveRangeIntact() {
        final EirLiveRange oldLiveRange = liveRange;
        liveRange = new EirBitSetLiveRange(this);
        liveRange.compute();
        return oldLiveRange.equals(liveRange);
    }

    public boolean areInterferencesIntact(EirMethodGeneration methodGeneration) {
        final PoolSet<EirVariable> nonInterferingVariables = PoolSet.allOf(methodGeneration.variablePool());
        for (EirVariable variable : interferingVariables) {
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

    private VariableDeterministicSet<EirVariable> aliasedVariables;

    /**
     * Establishes the relationship between this variable and another whereby the
     * former is an alias for the latter. When this variable is determined during
     * register allocation to be {@linkplain #recordUse(EirOperand) used},
     * the aliased variable is also marked as used thus ensuring it won't be
     * re-allocated.
     */
    public void setAliasedVariable(EirVariable aliasedVariable) {
        if (aliasedVariables == null) {
            aliasedVariables = new LinkedIdentityHashSet<EirVariable>();
        }
        assert aliasedVariable != this;
        aliasedVariables.add(aliasedVariable);
    }

    /**
     * @see #setAliasedVariable(EirVariable)
     */
    public DeterministicSet<EirVariable> aliasedVariables() {
        return aliasedVariables;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        liveRange = null;
        interferingVariables = null;
        aliasedVariables = null;
    }

    @Override
    public String toString() {
        String s = kind.character + "$" + serial;
        if (location() != null) {
            s += "@" + location();
        }
        return s;
    }

    public Interval interval;
}
