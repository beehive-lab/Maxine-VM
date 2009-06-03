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

import com.sun.c1x.value.ValueType;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.lir.LIROperand;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.InstructionClosure;

import java.util.List;

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
    private static final int BCI_NOT_APPENDED = -99;
    public static final int INVOCATION_ENTRY_BCI = -1;
    public static final int SYNCHRONIZATION_ENTRY_BCI = -1;

    /**
     * An enumeration of flags on instructions.
     */
    public enum Flag {
        NonNull,
        CanTrap,
        DirectCompare,
        IsEliminated,
        IsInitialized,
        IsLoaded,
        IsSafepoint,
        IsStatic,
        IsStrictFP,
        NeedsStoreCheck,
        NeedsWriteBarrier,
        PreservesState,
        TargetIsFinal,
        TargetIsLoaded,
        TargetIsStrictfp,
        UnorderedIsTrue,
        NeedsPatching,
        ThrowIncompatibleClassChangeError,
        ProfileMDO;

        public final int mask() {
            return 1 << ordinal();
        }
    }

    /**
     * An enumeration of the reasons that an instruction can be pinned.
     */
    public enum PinReason {
        PinUnknown,
        PinExplicitNullCheck,
        PinStackForStateSplit,
        PinStateSplitConstructor,
        PinGlobalValueNumbering;

        public final int mask() {
            return 1 << ordinal();
        }
    }

    private static int _nextID;

    private final int _id;
    private int _bci;
    private int _pinState;
    private int _flags;
    private ValueType _valueType;
    private Instruction _next;
    private Instruction _subst;

    private List<ExceptionHandler> _exceptionHandlers;

    private LIROperand _lirOperand;

    /**
     * Constructs a new instruction with the specified value type.
     * @param type the value type for this instruction
     */
    public Instruction(ValueType type) {
        _id = _nextID++;
        _bci = BCI_NOT_APPENDED;
        _valueType = type;
    }

    /**
     * Gets the unique ID of this instruction.
     * @return the id of this instruction
     */
    public final int id() {
        return _id;
    }

    /**
     * Gets the bytecode index of this instruction.
     * @return the bytecode index of this instruction
     */
    public final int bci() {
        return _bci;
    }

    /**
     * Sets the bytecode index of this instruction.
     * @param bci the new bytecode index for this instruction
     */
    public final void setBCI(int bci) {
        // XXX: BCI field may not be needed at all
        assert _bci >= 0 || _bci == SYNCHRONIZATION_ENTRY_BCI;
        _bci = bci;
    }

    /**
     * Checks whether this instruction has already been added to its basic block.
     * @return <code>true</code> if this instruction has been added to the basic block containing it
     */
    public boolean isAppended() {
        return _bci != BCI_NOT_APPENDED;
    }

    /**
     * Gets the pin state of this instruction
     * @return the pin state of this instruction
     */
    public final int pinState() {
        return _pinState;
    }

    /**
     * Checks whether this instruction is pinned. Note that this method
     * will return <code>true</code> if the appropriate global option in
     * {@link com.sun.c1x.C1XOptions#PinAllInstructions} is set.
     * @return <code>true</code> if this instruction has been pinned
     */
    public final boolean isPinned() {
        return C1XOptions.PinAllInstructions || (_pinState != 0);
    }

    /**
     * Gets the value type of this instruction.
     * @return the value type of this instruction
     */
    public final ValueType type() {
        return _valueType;
    }

    /**
     * Sets the value type of this instruction.
     * @param type the new value type for this instruction
     */
    public final void setType(ValueType type) {
        // XXX: refactor to facade and make field public?
        assert type != null;
        _valueType = type;
    }

    /**
     * Gets the next instruction after this one in the basic block, or <code>null</code>
     * if this instruction is the end of a basic block.
     * @return the next instruction after this one in the basic block
     */
    public final Instruction next() {
        return _next;
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
            // XXX: refactor to facade and make field public?
            assert !(this instanceof Phi || this instanceof BlockEnd || this instanceof Local);
            _next = next;
            next.setBCI(bci);
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
        if (_subst == null) {
            return this;
        }
        return _subst.subst();
    }

    /**
     * Checks whether this instruction has a substitute.
     * @return <code>true</code> if this instruction has a substitution.
     */
    public final boolean hasSubst() {
        return _subst != null;
    }

    /**
     * Sets the instruction that will be substituted for this instruction.
     * @param subst the instruction to substitute for this instruction
     */
    public final void setSubst(Instruction subst) {
        _subst = subst;
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

    /**
     * Pin this instruction.
     * @param reason the reason this instruction should be pinned.
     */
    public final void pin(PinReason reason) {
        _pinState |= reason.mask();
    }

    /**
     * Pin this instruction (with an unknown reason).
     */
    public final void pin() {
        pin(PinReason.PinUnknown);
    }

    /**
     * Unpin an instruction that might have been pinned for the specified reason.
     * Note that an instruction that has been pinned for an unknown reason cannot
     * be unpinned.
     * @param reason the reason this instruction might have been pinned
     */
    public final void unpin(PinReason reason) {
        // XXX: is it better to just ignore requests to unpin in the unknown case?
        assert reason != PinReason.PinUnknown : "cannot unpin unknown pin reason";
        _pinState &= ~reason.mask();
    }

    /**
     * Check whether this instruction has the specified flag set.
     * @param flag the flag to test
     * @return <code>true</code> if this instruction has the flag
     */
    public final boolean checkFlag(Flag flag) {
        return (_flags & flag.mask()) != 0;
    }

    /**
     * Set a flag on this instruction.
     * @param flag the flag to set
     */
    public final void setFlag(Flag flag) {
        _flags |= flag.mask();
    }

    /**
     * Set a flag on this instruction.
     * @param flag the flag to set
     */
    public final void clearFlag(Flag flag) {
        _flags &= ~flag.mask();
    }

    /**
     * Set a flag on this instruction.
     * @param flag the flag to set
     * @param val if <code>true</code>, set the flag, otherwise clear it
     */
    public final void setFlag(Flag flag, boolean val) {
        // PERF: this is often called to initialize a flag, so clearing is often unnecessary
        if (val) {
            setFlag(flag);
        } else {
            clearFlag(flag);
        }
    }

    /**
     * Checks whether this instruction needs a null check.
     * @return <code>true</code> if this instruction needs a null check
     */
    public boolean isNonNull() {
        return checkFlag(Flag.NonNull);
    }

    /**
     * Gets the LIR operand associated with this instruction.
     * @return the LIR operand for this instruction
     */
    public Object operand() {
        return _lirOperand;
    }

    /**
     * Sets the LIR operand associated with this instruction.
     * @param operand the operand to associate with this instruction
     */
    public void setOperand(LIROperand operand) {
        _lirOperand = operand;
    }

    /**
     * Clears the LIR operand associated with this instruction by setting it
     * to an illegal operand.
     */
    public void clearOperand() {
        // TODO: set the _lirOperand to an illegal operand
        _lirOperand = null;
    }

    /**
     * Gets the list of exception handlers associated with this instruction
     * @return the list of exception handlers for this instruction
     */
    public List<ExceptionHandler> exceptionHandlers() {
        return _exceptionHandlers;
    }

    /**
     * Sets the list of exception handlers for this instruction
     * @param exceptionHandlers the exception handlers
     */
    public void setExceptionHandlers(List<ExceptionHandler> exceptionHandlers) {
        _exceptionHandlers = exceptionHandlers;
    }

    //========================== Value numbering support =================================

    /**
     * Compute the hashcode of this Instruction. Local and global value numbering
     * optimizations use a hash map to implement equivalency checking, and all
     * instruction subclasses therefore must define .equals() and .hashCode().
     * The default implementation is to return the id of this instruction,
     * and object identity is used.
     * @return the hashcode of this instruction
     */
    @Override
    public int hashCode() {
        return _id;
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
    public CiType exactType() {
        return null;
    }

    /**
     * Computes the declared type of the result of this instruction, if possible.
     * @return the declared type of the result of this instruction, if it is known; <code>null</code> otherwise
     */
    public CiType declaredType() {
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
     * Apply the specified closure to all the input values of this instruction
     * @param closure the closure to apply
     */
    public void inputValuesDo(InstructionClosure closure) {
        // default: do nothing.
    }

    /**
     * Apply the specified closure to all the state values of this instruction
     * @param closure the closure to apply
     */
    public void stateValuesDo(InstructionClosure closure) {
        // default: do nothing.
    }

    /**
     * Apply the specified closure to all the other values of this instruction
     * @param closure the closure to apply
     */
    public void otherValuesDo(InstructionClosure closure) {
        // default: do nothing.
    }

    /**
     * Apply the specified closure to all the values of this instruction, including
     * input values, state values, and other values
     * @param closure the closure to apply
     */
    public void allValuesDo(InstructionClosure closure) {
        inputValuesDo(closure);
        stateValuesDo(closure);
        otherValuesDo(closure);
    }

}
