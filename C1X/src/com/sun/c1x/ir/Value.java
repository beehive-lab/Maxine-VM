/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.C1XOptions;
import com.sun.c1x.C1XMetrics;
import com.sun.c1x.ri.RiType;
import com.sun.c1x.lir.*;
import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiConstant;

/**
 * This class represents a value within the HIR graph, including local variables, phis, and
 * all other instructions.
 *
 * @author Ben L. Titzer
 */
public abstract class Value {
    /**
     * An enumeration of flags on values.
     */
    public enum Flag {
        NonNull,            // this value is non-null
        NoNullCheck,        // does not require null check
        NoStoreCheck,       // does not require store check
        NoBoundsCheck,      // does not require bounds check
        NoReadBarrier,      // does not require read barrier
        NoWriteBarrier,     // does not require write barrier
        NoZeroCheck,        // divide or modulus cannot cause exception
        DirectCompare,
        IsLoaded,           // field or method is resolved and class is loaded and initialized
        IsStatic,           // field or method access is static
        IsSafepoint,        // branch is backward (safepoint)
        IsStrictFP,
        PreservesState,     // intrinsic preserves state
        UnorderedIsTrue,
        NeedsPatching,
        LiveValue,          // live because value is used
        LiveDeopt,          // live for deoptimization
        LiveControl,        // live for control dependencies
        LiveSideEffect,     // live for possible side-effects only
        LiveStore,          // instruction is a store
        PhiDead,            // phi is illegal because local is dead
        PhiCannotSimplify,  // phi cannot be simplified
        PhiVisited;         // phi has been visited during simplification

        public final int mask = 1 << ordinal();
    }

    private static final int LIVE_FLAGS = Flag.LiveValue.mask |
                                          Flag.LiveDeopt.mask |
                                          Flag.LiveControl.mask |
                                          Flag.LiveSideEffect.mask;
    public static int nextID;

    public final int id;
    public final CiKind kind;

    private int flags;
    private LIROperand lirOperand;

    public Value subst; // managed by InstructionSubstituter

    /**
     * Creates a new value with the specified kind.
     * @param type the type of this value
     */
    public Value(CiKind type) {
        kind = type;
        id = nextID++;
    }

    /**
     * Checks whether this instruction is live (i.e. code should be generated for it).
     * This is computed in a dedicated pass by {@link com.sun.c1x.opt.LivenessMarker}.
     * An instruction be live because its value is needed by another live instruction,
     * because its value is needed for deoptimization, or the program is control dependent
     * upon it.
     * @return {@code true} if this instruction should be considered live
     */
    public final boolean isLive() {
        return C1XOptions.PinAllInstructions || (flags & LIVE_FLAGS) != 0;
    }

    /**
     * Clears all liveness flags.
     */
    public final void clearLive() {
        flags = flags & ~LIVE_FLAGS;
    }

    /**
     * Gets the instruction that should be substituted for this one. Note that this
     * method is recursive; if the substituted instruction has a substitution, then
     * the final substituted instruction will be returned. If there is no substitution
     * for this instruction, <code>this</code> will be returned.
     * @return the substitution for this instruction
     */
    public final Value subst() {
        if (subst == null) {
            return this;
        }
        return subst.subst();
    }

    /**
     * Checks whether this instruction has a substitute.
     * @return <code>true</code> if this instruction has a substitution.
     */
    public final boolean hasSubst() {
        return subst != null;
    }

    /**
     * Clear any internal state related to null checks, because a null check
     * for this instruction is redundant. The state cleared may depend
     * on the type of this instruction
     */
    public final void redundantNullCheck() {
        if (clearNullCheck()) {
            C1XMetrics.NullChecksRedundant++;
        }
    }

    /**
     * Clear any internal state related to null checks, because a null check
     * for this instruction is redundant. The state cleared may depend
     * on the type of this instruction
     */
    public final void eliminateNullCheck() {
        if (clearNullCheck()) {
            C1XMetrics.NullCheckEliminations++;
        }
    }

    private boolean clearNullCheck() {
        if (!checkFlag(Flag.NoNullCheck)) {
            setFlag(Flag.NoNullCheck);
            return internalClearNullCheck();
        }
        return false;
    }

    /**
     * Clears any internal state associated with null checks.
     * @return {@code true} if this instruction had any state that was changed
     */
    protected boolean internalClearNullCheck() {
        // most instructions don't care about clearing of their null checks
        return false;
    }

    /**
     * Check whether this instruction has the specified flag set.
     * @param flag the flag to test
     * @return <code>true</code> if this instruction has the flag
     */
    public final boolean checkFlag(Flag flag) {
        return (flags & flag.mask) != 0;
    }

    /**
     * Set a flag on this instruction.
     * @param flag the flag to set
     */
    public final void setFlag(Flag flag) {
        flags |= flag.mask;
    }

    /**
     * Clear a flag on this instruction.
     * @param flag the flag to set
     */
    public final void clearFlag(Flag flag) {
        flags &= ~flag.mask;
    }

    /**
     * Set or clear a flag on this instruction.
     * @param flag the flag to set
     * @param val if <code>true</code>, set the flag, otherwise clear it
     */
    public final void setFlag(Flag flag, boolean val) {
        if (val) {
            setFlag(flag);
        } else {
            clearFlag(flag);
        }
    }

    /**
     * Initialize a flag on this instruction. Assumes the flag is not initially set,
     * e.g. in the constructor of an instruction.
     * @param flag the flag to set
     * @param val if <code>true</code>, set the flag, otherwise do nothing
     */
    public final void initFlag(Flag flag, boolean val) {
        if (val) {
            setFlag(flag);
        }
    }

    /**
     * Checks whether this instruction produces a value which is guaranteed to be non-null.
     * @return <code>true</code> if this instruction's value is not null
     */
    public final boolean isNonNull() {
        return checkFlag(Flag.NonNull);
    }

    /**
     * Checks whether this instruction needs a null check.
     * @return <code>true</code> if this instruction needs a null check
     */
    public final boolean needsNullCheck() {
        return !checkFlag(Flag.NoNullCheck);
    }

    /**
     * Checks whether this value is a constant (i.e. it is of type {@link Constant}.
     * @return {@code true} if this value is a constant
     */
    public final boolean isConstant() {
        return this instanceof Constant;
    }

    /**
     * Checks whether this instruction "is illegal"--i.e. it represents a dead
     * phi or an instruction which does not produce a value.
     * @return {@code true} if this instruction is illegal as an input value to another instruction
     */
    public final boolean isIllegal() {
        return checkFlag(Flag.PhiDead);
    }

    /**
     * Convert this value to a constant if it is a constant, otherwise return null.
     * @return the {@link CiConstant} represented by this value if it is a constant; {@code null}
     * otherwise
     */
    public final CiConstant asConstant() {
        if (this instanceof Constant) {
            return ((Constant) this).value;
        }
        return null;
    }

    /**
     * Gets the LIR operand associated with this instruction.
     * @return the LIR operand for this instruction
     */
    public final LIROperand operand() {
        return lirOperand;
    }

    /**
     * Sets the LIR operand associated with this instruction.
     * @param operand the operand to associate with this instruction
     */
    public final void setOperand(LIROperand operand) {
        assert operand != null && !operand.isIllegal() : "operand must exist";
        assert operand.kind == this.kind;
        lirOperand = operand;
    }

    /**
     * Clears the LIR operand associated with this instruction.
     */
    public final void clearOperand() {
        lirOperand = LIROperandFactory.IllegalLocation;
    }

    /**
     * Computes the exact type of the result of this instruction, if possible.
     * @return the exact type of the result of this instruction, if it is known; <code>null</code> otherwise
     */
    public RiType exactType() {
        return null; // default: unknown exact type
    }

    /**
     * Computes the declared type of the result of this instruction, if possible.
     * @return the declared type of the result of this instruction, if it is known; <code>null</code> otherwise
     */
    public RiType declaredType() {
        return null; // default: unknown declared type
    }

    /**
     * Apply the specified closure to all the input values of this instruction.
     * @param closure the closure to apply
     */
    public void inputValuesDo(ValueClosure closure) {
        // default: do nothing.
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" #");
        builder.append(id);
        if (this instanceof Instruction) {
            builder.append(" @ ");
            builder.append(((Instruction) this).bci());
        }
        builder.append(" [");
        boolean hasFlag = false;
        for (Flag f : Flag.values()) {
            if (checkFlag(f)) {
                if (hasFlag) {
                    builder.append(' ');
                }
                builder.append(f.name());
                hasFlag = true;
            }
        }
        builder.append("]");
        return builder.toString();
    }

    public final boolean isDeadPhi() {
        return checkFlag(Flag.PhiDead);
    }

    /**
     * This method supports the visitor pattern by accepting a visitor and calling the
     * appropriate {@code visit()} method.
     *
     * @param v the visitor to accept
     */
    public abstract void accept(ValueVisitor v);

}
