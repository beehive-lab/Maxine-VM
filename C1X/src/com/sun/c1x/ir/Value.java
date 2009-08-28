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
import com.sun.c1x.lir.LIROperand;
import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiConstant;

/**
 * The <code>Value</code> class definition.
 *
 * @author Ben L. Titzer
 */
public abstract class Value {
    /**
     * Utility method to check that two instructions have the same basic type.
     * @param i the first instruction
     * @param other the second instruction
     * @return {@code true} if the instructions have the same basic type
     */
    public static boolean sameBasicType(Value i, Value other) {
        return i.type().basicType == other.type().basicType;
    }

    /**
     * Checks that two instructions are equivalent, optionally comparing constants.
     * @param x the first instruction
     * @param y the second instruction
     * @param compareConstants {@code true} if equivalent constants should be considered equivalent
     * @return {@code true} if the instructions are equivalent; {@code false} otherwise
     */
    public static boolean equivalent(Instruction x, Instruction y, boolean compareConstants) {
        if (x == y) {
            return true;
        }
        if (compareConstants && x != null && y != null) {
            if (x.isConstant() && x.asConstant().equivalent(y.asConstant())) {
                return true;
            }
        }
        return false;
    }

    /**
     * An enumeration of flags on instructions.
     */
    public enum Flag {
        NonNull,            // produces non-null value
        NoNullCheck,        // does not require null check
        NoStoreCheck,       // does not require store check
        NoBoundsCheck,       // does not require range (bounds) check
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
        ThrowIncompatibleClassChangeError,
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
    protected final int id;
    protected final CiKind valueType;
    private int flags;
    private Value subst;
    protected LIROperand lirOperand;

    public Value(CiKind type) {
        valueType = type;
        id = nextID++;
    }

    /**
     * Gets the unique ID of this instruction.
     * @return the id of this instruction
     */
    public final int id() {
        return id;
    }

    /**
     * Checks whether this instruction is live (i.e. code should be generated for it).
     * This is computed in a dedicated pass by {@link com.sun.c1x.opt.LivenessMarker}.
     * An instruction be live because its value is needed by another live instruction,
     * because its value is needed for deoptimization, or the program is control dependent
     * upon it.
     * @return {@code true} if this instruction should be considered live
     */
    public boolean isLive() {
        return C1XOptions.PinAllInstructions || (flags & LIVE_FLAGS) != 0;
    }

    /**
     * Clears all liveness flags.
     */
    public void clearLive() {
        flags = flags & ~LIVE_FLAGS;
    }

    /**
     * Gets the type of the value pushed to the stack by this instruction.
     * @return the value type of this instruction
     */
    public final CiKind type() {
        return valueType;
    }

    /**
     * Gets the instruction that should be substituted for this one. Note that this
     * method is recursive; if the substituted instruction has a substitution, then
     * the final substituted instruction will be returned. If there is no substitution
     * for this instruction, <code>this</code> will be returned.
     * @return the substitution for this instruction
     */
    public final Instruction subst() {
        if (subst == null) {
            return (Instruction) this;
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
     * Sets the instruction that will be substituted for this instruction.
     * @param subst the instruction to substitute for this instruction
     */
    public final void setSubst(Value subst) {
        this.subst = subst;
    }

    public final void redundantNullCheck() {
        if (clearNullCheck()) {
            C1XMetrics.NullChecksRedundant++;
        }
    }

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
     * Set a flag on this instruction.
     * @param flag the flag to set
     */
    public final void clearFlag(Flag flag) {
        flags &= ~flag.mask;
    }

    /**
     * Set a flag on this instruction.
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
     * Initialize a flag on this instruction.
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
    public LIROperand operand() {
        return lirOperand;
    }

    /**
     * Sets the LIR operand associated with this instruction.
     * @param operand the operand to associate with this instruction
     */
    public void setOperand(LIROperand operand) {
        assert operand != LIROperand.ILLEGAL : "operand must exist";
        lirOperand = operand;
    }

    /**
     * Clears the LIR operand associated with this instruction.
     */
    public void clearOperand() {
        lirOperand = LIROperand.ILLEGAL;
    }

    /**
     * Computes the exact type of the result of this instruction, if possible.
     * @return the exact type of the result of this instruction, if it is known; <code>null</code> otherwise
     */
    public RiType exactType() {
        return null;
    }

    /**
     * Computes the declared type of the result of this instruction, if possible.
     * @return the declared type of the result of this instruction, if it is known; <code>null</code> otherwise
     */
    public RiType declaredType() {
        return null;
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

    public boolean isDeadPhi() {
        return checkFlag(Flag.PhiDead);
    }

    /**
     * This method supports the visitor pattern by accepting a visitor and calling the
     * appropriate <code>visit()</code> method.
     * @param v the visitor to accept
     */
    public abstract void accept(ValueVisitor v);

}
