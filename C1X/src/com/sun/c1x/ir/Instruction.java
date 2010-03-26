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

import com.sun.c1x.ci.*;
import com.sun.c1x.value.*;
import com.sun.c1x.lir.LIROperand;

/**
 * This class represents an instruction node in the IR, which is a {@link Value} that
 * can be added to a basic block (whereas other {@link Value} nodes such as {@link Phi} and
 * {@link Local} cannot be added to basic blocks).
 *
 * Subclasses of instruction represent arithmetic and object operations,
 * control flow operators, phi statements, method calls, the start of basic blocks, and
 * the end of basic blocks.
 *
 * @author Ben L. Titzer
 */
public abstract class Instruction extends Value {

    public static final int BCI_NOT_APPENDED = -99;
    public static final int INVOCATION_ENTRY_BCI = -1;
    public static final int SYNCHRONIZATION_ENTRY_BCI = -1;

    private int bci;
    private Instruction next;

    private List<ExceptionHandler> exceptionHandlers = ExceptionHandler.ZERO_HANDLERS;

    /**
     * Constructs a new instruction with the specified value type.
     * @param kind the value type for this instruction
     */
    public Instruction(CiKind kind) {
        super(kind);
        bci = BCI_NOT_APPENDED;
        lirOperand = LIROperand.IllegalLocation;
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
     * @return {@code true} if this instruction has been added to the basic block containing it
     */
    public final boolean isAppended() {
        return bci != BCI_NOT_APPENDED;
    }

    /**
     * Gets the next instruction after this one in the basic block, or {@code null}
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
            assert !(this instanceof BlockEnd);
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
            assert !(this instanceof BlockEnd);
            this.next = next;
        }
        return next;
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
     * Gets the list of exception handlers associated with this instruction.
     * @return the list of exception handlers for this instruction
     */
    public final List<ExceptionHandler> exceptionHandlers() {
        return exceptionHandlers;
    }

    /**
     * Sets the list of exception handlers for this instruction.
     * @param exceptionHandlers the exception handlers
     */
    public final void setExceptionHandlers(List<ExceptionHandler> exceptionHandlers) {
        this.exceptionHandlers = exceptionHandlers;
    }

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
     * @return {@code true} if this instruction is equivalent to the specified
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
     * Tests whether this instruction can trap.
     * @return {@code true} if this instruction can cause a trap.
     */
    public boolean canTrap() {
        return false;
    }

    /**
     * Apply the specified closure to all the values of this instruction, including
     * input values, state values, and other values.
     * @param closure the closure to apply
     */
    public final void allValuesDo(ValueClosure closure) {
        inputValuesDo(closure);
        FrameState stateBefore = stateBefore();
        if (stateBefore != null) {
            stateBefore.valuesDo(closure);
        }
        FrameState stateAfter = stateAfter();
        if (stateAfter != null) {
            stateAfter.valuesDo(closure);
        }
    }

    /**
     * Gets the state before the instruction, if it is recorded.
     * @return the state before the instruction
     */
    public FrameState stateBefore() {
        return null;
    }

    /**
     * Gets the state after the instruction, if it is recorded. Typically only
     * instances of {@link BlockEnd} have a state after.
     * @return the state after the instruction
     */
    public FrameState stateAfter() {
        return null;
    }
}
