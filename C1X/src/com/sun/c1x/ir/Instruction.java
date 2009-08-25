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

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.value.*;

/**
 * The <code>Instruction</code> class represents a node in the IR. Each instruction
 * has a <code>next</code> field, which connects it to the next instruction in its
 * basic block. Subclasses of instruction represent arithmetic and object operations,
 * control flow operators, phi statements, method calls, the start of basic blocks, and
 * the end of basic blocks.
 *
 * @author Ben L. Titzer
 */
public abstract class Instruction {
    public boolean isDeadPhi() {
        return checkFlag(Flag.PhiDead);
    }

    /**
     * An enumeration of flags on instructions.
     */
    public enum Flag {
        NonNull,            // produces non-null value
        NoNullCheck,        // does not require null check
        NoStoreCheck,       // does not require store check
        NoRangeCheck,       // does not require range (bounds) check
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

    private static final int BCI_NOT_APPENDED = -99;
    private static final int LIVE_FLAGS = Flag.LiveValue.mask |
                                          Flag.LiveDeopt.mask |
                                          Flag.LiveControl.mask |
                                          Flag.LiveSideEffect.mask;
    public static final int INVOCATION_ENTRY_BCI = -1;
    public static final int SYNCHRONIZATION_ENTRY_BCI = -1;

    public static int nextID;

    private final int id;
    protected final BasicType valueType;
    private int bci;
    private int flags;
    private Instruction next;
    private Instruction subst;

    private List<ExceptionHandler> exceptionHandlers = ExceptionHandler.ZERO_HANDLERS;

    private LIROperand lirOperand;

    /**
     * Constructs a new instruction with the specified value type.
     * @param type the value type for this instruction
     */
    public Instruction(BasicType type) {
        id = nextID++;
        bci = BCI_NOT_APPENDED;
        valueType = type;
        lirOperand = LIROperand.ILLEGAL;
    }

    /**
     * Gets the unique ID of this instruction.
     * @return the id of this instruction
     */
    public final int id() {
        return id;
    }

    /**
     * Gets the bytecode index of this instruction.
     * @return the bytecode index of this instruction
     */
    public final int bci() {
        return bci;
    }

    /**
     * Sets the bytecode index of this instruction.
     * @param bci the new bytecode index for this instruction
     */
    public final void setBCI(int bci) {
        // XXX: BCI field may not be needed at all
        assert bci >= 0 || bci == SYNCHRONIZATION_ENTRY_BCI;
        this.bci = bci;
    }

    /**
     * Checks whether this instruction has already been added to its basic block.
     * @return <code>true</code> if this instruction has been added to the basic block containing it
     */
    public final boolean isAppended() {
        return bci != BCI_NOT_APPENDED;
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
    public final BasicType type() {
        return valueType;
    }

    /**
     * Gets the next instruction after this one in the basic block, or <code>null</code>
     * if this instruction is the end of a basic block.
     * @return the next instruction after this one in the basic block
     */
    public final Instruction next() {
        return next;
    }

    /**
     * Sets the next instruction for this instruction. Note that it is illegal to
     * set the next field of a phi, block end, or local instruction.
     * @param next the next instruction
     * @param bci the bytecode index of the next instruction
     * @return the new next instruction
     */
    public final Instruction setNext(Instruction next, int bci) {
        if (next != null) {
            assert !(this instanceof Phi || this instanceof BlockEnd || this instanceof Local);
            this.next = next;
            next.setBCI(bci);
        }
        return next;
    }

    /**
     * Re-sets the next instruction for this instruction. Note that it is illegal to
     * set the next field of a phi, block end, or local instruction.
     * @param next the next instruction
     * @return the new next instruction
     */
    public final Instruction resetNext(Instruction next) {
        if (next != null) {
            assert !(this instanceof Phi || this instanceof BlockEnd || this instanceof Local);
            this.next = next;
        }
        return next;
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
     * Sets the instruction that will be substituted for this instruction.
     * @param subst the instruction to substitute for this instruction
     */
    public final void setSubst(Instruction subst) {
        this.subst = subst;
    }

    /**
     * Gets the instruction preceding this instruction in the specified basic block.
     * Note that instructions do not directly refer to their previous instructions,
     * and therefore this operation much search from the beginning of the basic
     * block, thereby requiring time linear in the size of the basic block in the worst
     * case. Use with caution!
     * @param block the basic block that contains this instruction
     * @return the instruction before this instruction in the basic block
     */
    public final Instruction prev(BlockBegin block) {
        Instruction p = null;
        Instruction q = block;
        while (q != this) {
            assert q != null : "this instruction is not in the specified basic block";
            p = q;
            q = q.next();
        }
        return p;
    }

    public void clearNullCheck() {
        clearFlag(Flag.NoNullCheck);
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
     * Gets the list of exception handlers associated with this instruction.
     * @return the list of exception handlers for this instruction
     */
    public List<ExceptionHandler> exceptionHandlers() {
        return exceptionHandlers;
    }

    /**
     * Sets the list of exception handlers for this instruction.
     * @param exceptionHandlers the exception handlers
     */
    public final void setExceptionHandlers(List<ExceptionHandler> exceptionHandlers) {
        this.exceptionHandlers = exceptionHandlers;
    }

    //========================== Value numbering support =================================

    /**
     * Compute the value number of this Instruction. Local and global value numbering
     * optimizations use a hash map, and the value number provides a hash code.
     * If the instruction cannot be value numbered, then this method should return
     * {@code 0}.
     * @return the hashcode of this instruction
     */
    public int valueNumber() {
        return 0;
    }

    /**
     * Checks that this instruction is equal to another instruction for the purposes
     * of value numbering.
     * @param i the other instruction
     * @return <code>true</code> if this instruction is equivalent to the specified
     * instruction w.r.t. value numbering
     */
    public boolean valueEqual(Instruction i) {
        return false;
    }

    /**
     * Gets the name of this instruction as a string.
     * @return the name of this instruction
     */
    public final String name() {
        return getClass().getSimpleName();
    }

    /**
     * This method supports the visitor pattern by accepting a visitor and calling the
     * appropriate <code>visit()</code> method.
     * @param v the visitor to accept
     */
    public abstract void accept(InstructionVisitor v);

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
     * Tests whether this instruction can trap.
     * @return <code>true</code> if this instruction can cause a trap.
     */
    public boolean canTrap() {
        // XXX: what is the relationship to the CanTrap?
        return false;
    }

    /**
     * Apply the specified closure to all the input values of this instruction.
     * @param closure the closure to apply
     */
    public void inputValuesDo(InstructionClosure closure) {
        // default: do nothing.
    }

    /**
     * Apply the specified closure to all the values of this instruction, including
     * input values, state values, and other values.
     * @param closure the closure to apply
     */
    public void allValuesDo(InstructionClosure closure) {
        inputValuesDo(closure);
        ValueStack stateBefore = stateBefore();
        if (stateBefore != null) {
            stateBefore.valuesDo(closure);
        }
        ValueStack stateAfter = stateAfter();
        if (stateAfter != null) {
            stateAfter.valuesDo(closure);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" #");
        builder.append(id);
        builder.append(" @ ");
        builder.append(bci);
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

    /**
     * Utility method to check that two instructions have the same basic type.
     * @param i the first instruction
     * @param other the second instruction
     * @return {@code true} if the instructions have the same basic type
     */
    public static boolean sameBasicType(Instruction i, Instruction other) {
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
     * Converts a given instruction to a value string. The representation of an instruction as
     * a value is formed by concatenating the {@linkplain com.sun.c1x.value.BasicType#tchar() character} denoting its
     * {@linkplain com.sun.c1x.ir.Instruction#type() type} and its {@linkplain com.sun.c1x.ir.Instruction#id()}. For example,
     * "i13".
     *
     * @param value the instruction to convert to a value string. If {@code value == null}, then "null" is returned.
     * @return the instruction representation as a string
     */
    public static String valueString(Instruction value) {
        return value == null ? "null" : "" + value.type().tchar() + value.id();
    }

    /**
     * Gets the lock stack of the instruction if one exists.
     * @return the lock stack
     */
    public ValueStack stateBefore() {
        return null;
    }

    /**
     * Gets the lock stack of the instruction if one exists.
     * @return the lock stack
     */
    public ValueStack stateAfter() {
        return null;
    }

    public void printLine() {
        InstructionPrinter ip = new InstructionPrinter(TTY.out, true);
        ip.printInstructionListing(this);
    }
}
