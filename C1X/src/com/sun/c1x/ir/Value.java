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

import com.sun.c1x.*;
import com.sun.c1x.opt.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

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
        NoDivSpecialCase,   // divide or modulus cannot be special case of MIN_INT / -1
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
    public final CiKind kind;

    private int id;
    private int flags;
    protected CiValue operand = CiValue.IllegalValue;

    /**
     * A cache for analysis information. Every optimization must reset this field to {@code null} once it has completed.
     */
    public Object optInfo;

    public Value subst;    // managed by InstructionSubstituter

    /**
     * Creates a new value with the specified kind.
     * @param kind the type of this value
     */
    public Value(CiKind kind) {
        assert kind == kind.stackKind() : kind + " != " + kind.stackKind();
        this.kind = kind;
    }

    /**
     * Checks whether this instruction is live (i.e. code should be generated for it).
     * This is computed in a dedicated pass by {@link LivenessMarker}.
     * An instruction is live because its value is needed by another live instruction,
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
     * for this instruction, {@code this} will be returned.
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
     * @return {@code true} if this instruction has a substitution.
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
     * @return {@code true} if this instruction has the flag
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
     * @param val if {@code true}, set the flag, otherwise clear it
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
     * @param val if {@code true}, set the flag, otherwise do nothing
     */
    public final void initFlag(Flag flag, boolean val) {
        if (val) {
            setFlag(flag);
        }
    }

    /**
     * Checks whether this instruction produces a value which is guaranteed to be non-null.
     * @return {@code true} if this instruction's value is not null
     */
    public final boolean isNonNull() {
        return checkFlag(Flag.NonNull);
    }

    /**
     * Checks whether this instruction needs a null check.
     * @return {@code true} if this instruction needs a null check
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
     * Checks whether this value represents the null constant.
     * @return {@code true} if this value represents the null constant
     */
    public final boolean isNullConstant() {
        return this instanceof Constant && ((Constant) this).value.isNull();
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
    public final CiValue operand() {
        return operand;
    }

    /**
     * Sets the LIR operand associated with this instruction.
     * @param operand the operand to associate with this instruction
     */
    public final void setOperand(CiValue operand) {
        assert this.operand.isIllegal() : "operand cannot be set twice";
        assert operand != null && operand.isLegal() : "operand must be legal";
        assert operand.kind.stackKind() == this.kind;
        this.operand = operand;
    }

    /**
     * Clears the LIR operand associated with this instruction.
     */
    public final void clearOperand() {
        this.operand = CiValue.IllegalValue;
    }

    /**
     * Computes the exact type of the result of this instruction, if possible.
     * @return the exact type of the result of this instruction, if it is known; {@code null} otherwise
     */
    public RiType exactType() {
        return null; // default: unknown exact type
    }

    /**
     * Computes the declared type of the result of this instruction, if possible.
     * @return the declared type of the result of this instruction, if it is known; {@code null} otherwise
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
        builder.append(id());
        if (this instanceof Instruction) {
            builder.append(" @ ");
            builder.append(((Instruction) this).bci());
        }
        builder.append(" [").append(flagsToString()).append("]");
        return builder.toString();
    }

    public String flagsToString() {
        StringBuilder sb = new StringBuilder();
        for (Flag f : Flag.values()) {
            if (checkFlag(f)) {
                if (sb.length() != 0) {
                    sb.append(' ');
                }
                sb.append(f.name());
            }
        }
        return sb.toString();
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

    /**
     * Utility method for computing the exact type of the specified value. Handles object constants by
     * querying the supplied {@link RiRuntime} instance.
     * @param value the value to get the exact type of
     * @param runtime the runtime interface to query in the case of an object constant
     * @return the exact type of the value if it can be computed; {@code null} otherwise
     */
    public static RiType exactType(Value value, RiRuntime runtime) {
        if (value.isConstant() && value.kind == CiKind.Object) {
            Object obj = value.asConstant().asObject();
            if (obj != null) {
                return runtime.getRiType(obj.getClass());
            }
        }
        return value.exactType();
    }

    /**
     * This method returns a unique identification number for this value. The number returned is unique
     * only to the compilation that produced this node and is computed lazily by using the current compilation
     * for the current thread. Thus the first access is a hash lookup using {@link java.lang.ThreadLocal} and
     * should not be considered fast. Because of the potentially slow first access, use of this ID should be
     * restricted to debugging output.
     * @return a unique ID for this value
     */
    public int id() {
        if (id == 0) {
            C1XMetrics.UniqueValueIdsAssigned++;
            id = C1XCompilation.current().nextID();
        }
        return id;
    }
}
