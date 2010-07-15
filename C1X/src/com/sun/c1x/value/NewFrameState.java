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
package com.sun.c1x.value;

import java.util.*;

import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.cri.ci.*;

/**
 * The {@code NewFrameState} class encapsulates the frame state (i.e. local variables and
 * operand stack) at a particular point in the abstract interpretation.
 *
 * @author Ben L. Titzer
 */
public abstract class NewFrameState {

    protected final Value[] values; // manages both stack and locals
    protected int stackIndex;
    protected final int maxLocals;

    protected final IRScope scope;
    protected ArrayList<Value> locks;

    /**
     * Specifies if operand stack accesses should be checked for type safety.
     */
    public boolean unsafe;

    /**
     * The number of minimum stack slots required for doing IR wrangling during
     * {@linkplain GraphBuilder bytecode parsing}. While this may hide stack
     * overflow issues in the original bytecode, the assumption is that such
     * issues must be caught by the verifier.
     */
    private static final int MINIMUM_STACK_SLOTS = 1;

    public NewFrameState(IRScope irScope, int maxLocals, int maxStack) {
        this.scope = irScope;
        this.values = new Value[maxLocals + Math.max(maxStack, MINIMUM_STACK_SLOTS)];
        this.maxLocals = maxLocals;
    }

    /**
     * Copies the contents of this frame state so that further updates to either stack aren't reflected in the other.
     *
     * @param withLocals indicates whether to copy the local state
     * @param withStack indicates whether to copy the stack state
     * @param withLocks indicates whether to copy the lock state
     * @return a new frame state with the specified components
     */
    public NewMutableFrameState copy(boolean withLocals, boolean withStack, boolean withLocks) {
        final NewMutableFrameState other = new NewMutableFrameState(scope, localsSize(), maxStackSize());
        if (withLocals && withStack) {
            // fast path: use array copy
            System.arraycopy(values, 0, other.values, 0, valuesSize());
            other.stackIndex = stackIndex;
        } else {
            if (withLocals) {
                other.replaceLocals(this);
            }
            if (withStack) {
                other.replaceStack(this);
            }
        }
        if (withLocks) {
            other.replaceLocks(this);
        }
        other.unsafe = unsafe;
        return other;
    }

    /**
     * Gets a mutable copy of this frame state.
     */
    public NewMutableFrameState copy() {
        return copy(true, true, true);
    }

    /**
     * Gets an immutable copy of this state.
     */
    public NewFrameState immutableCopy() {
        return copy(true, true, true);
    }

    public boolean isSameAcrossScopes(NewFrameState other) {
        assert stackSize() == other.stackSize();
        assert localsSize() == other.localsSize();
        assert locksSize() == other.locksSize();
        for (int i = 0; i < stackIndex; i++) {
            Value x = stackAt(i);
            Value y = other.stackAt(i);
            if (x != y && typeMismatch(x, y)) {
                return false;
            }
        }
        if (locks != null) {
            for (int i = 0; i < locks.size(); i++) {
                if (lockAt(i) != other.lockAt(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the inlining context associated with this frame state.
     *
     * @return the inlining context
     */
    public IRScope scope() {
        return scope;
    }

    /**
     * Returns the size of the local variables.
     *
     * @return the size of the local variables
     */
    public int localsSize() {
        return maxLocals;
    }

    /**
     * Returns the size of the locks.
     *
     * @return the size of the locks
     */
    public int locksSize() {
        return locks == null ? 0 : locks.size();
    }

    /**
     * Returns the current size (height) of the stack.
     *
     * @return the size of the stack
     */
    public int stackSize() {
        return stackIndex;
    }

    /**
     * Returns the maximum size of the stack.
     *
     * @return the maximum size of the stack
     */
    public int maxStackSize() {
        return values.length - maxLocals;
    }

    /**
     * Checks whether the stack is empty.
     *
     * @return {@code true} the stack is currently empty
     */
    public boolean stackEmpty() {
        return stackIndex == 0;
    }

    /**
     * Checks whether there are any active locks.
     *
     * @return {@code true} if there are <i>no</i> active locks
     */
    public boolean noActiveLocks() {
        return locksSize() == 0;
    }

    /**
     * Invalidate the local variable at the specified index. If the specified index refers to a doubleword local, then
     * invalid the high word as well.
     *
     * @param i the index of the local to invalidate
     */
    public void invalidateLocal(int i) {
        // note that for double word locals, the high slot should already be null
        // unless the local is actually dead and the high slot is being reused;
        // in either case, it is not necessary to null the high slot
        values[i] = null;
    }

    /**
     * Load the local variable at the specified index.
     *
     * @param i the index of the local variable to load
     * @return the instruction that produced the specified local
     */
    public Value loadLocal(int i) {
        Value x = values[i];
        if (x != null) {
            if (x.isIllegal()) {
                return null;
            }
            assert x.kind.isSingleWord() || values[i + 1] == null || values[i + 1] instanceof Phi;
        }
        return x;
    }

    /**
     * Stores a given local variable at the specified index. If the value is a {@linkplain CiKind#isDoubleWord() double word},
     * then the next local variable index is also overwritten.
     *
     * @param i the index at which to store
     * @param x the instruction which produces the value for the local
     */
    public void storeLocal(int i, Value x) {
        invalidateLocal(i);
        values[i] = x;
        if (isDoubleWord(x)) {
            // (tw) if this was a double word then kill i+1
            values[i + 1] = null;
        }
        if (i > 0) {
            // if there was a double word at i - 1, then kill it
            Value p = values[i - 1];
            if (isDoubleWord(p)) {
                values[i - 1] = null;
            }
        }
        if (x.kind.isWord()) {
            unsafe = true;
        }
    }

    /**
     * Get the value on the stack at the specified stack index.
     *
     * @param i the index into the stack, with {@code 0} being the bottom of the stack
     * @return the instruction at the specified position in the stack
     */
    public Value stackAt(int i) {
        final Value x = values[i + maxLocals];
        assert i < stackIndex;
        return x;
    }

    /**
     * Get the value in the local variable at the specified offset.
     *
     * @param i the index into the locals
     * @return the instruction that produced the value for the specified local
     */
    public Value localAt(int i) {
        return values[i];
    }

    /**
     * Retrieves the lock at the specified index in the lock stack.
     * @param i the index into the lock stack
     * @return the instruction which produced the object at the specified location in the lock stack
     */
    public Value lockAt(int i) {
        return locks.get(i);
    }

    /**
     * Inserts a phi statement into the stack at the specified stack index.
     * @param block the block begin for which we are creating the phi
     * @param i the index into the stack for which to create a phi
     */
    public void setupPhiForStack(BlockBegin block, int i) {
        Value p = stackAt(i);
        if (p != null) {
            if (p instanceof Phi) {
                Phi phi = (Phi) p;
                if (phi.block() == block && phi.isOnStack() && phi.stackIndex() == i) {
                    return;
                }
            }
            values[maxLocals + i] = new Phi(p.kind, block, -i - 1);
        }
    }

    /**
     * Inserts a phi statement for the local at the specified index.
     * @param block the block begin for which we are creating the phi
     * @param i the index of the local variable for which to create the phi
     */
    public void setupPhiForLocal(BlockBegin block, int i) {
        Value p = values[i];
        if (p instanceof Phi) {
            Phi phi = (Phi) p;
            if (phi.block() == block && phi.isLocal() && phi.localIndex() == i) {
                return;
            }
        }
        storeLocal(i, new Phi(p.kind, block, i));
    }

    /**
     * Gets the value at a specified index in the set of operand stack and local values represented by this frame.
     * This method should only be used to iterate over all the values in this frame, irrespective of whether
     * they are on the stack or in local variables.
     * To iterate the stack slots, the {@link #stackAt(int)} and {@link #stackSize()} methods should be used.
     * To iterate the local variables, the {@link #localAt(int)} and {@link #localsSize()} methods should be used.
     *
     * @param i a value in the range {@code [0 .. valuesSize()]}
     * @return the value at index {@code i} which may be {@code null}
     */
    public Value valueAt(int i) {
        return values[i];
    }

    /**
     * The number of operand stack slots and local variables in this frame.
     * This method should typically only be used in conjunction with {@link #valueAt(int)}.
     * To iterate the stack slots, the {@link #stackAt(int)} and {@link #stackSize()} methods should be used.
     * To iterate the local variables, the {@link #localAt(int)} and {@link #localsSize()} methods should be used.
     *
     * @return the number of local variables in this frame
     */
    public int valuesSize() {
        return maxLocals + stackIndex;
    }

    public int callerStackSize() {
        NewFrameState callerState = scope().callerState();
        return callerState == null ? 0 : callerState.stackSize();
    }

    public void checkPhis(BlockBegin block, NewFrameState other) {
        checkSize(other);
        final int max = valuesSize();
        for (int i = 0; i < max; i++) {
            Value x = values[i];
            Value y = other.values[i];
            if (x != null && x != y) {
                if (x instanceof Phi) {
                    Phi phi = (Phi) x;
                    if (phi.block() == block) {
                        for (int j = 0; j < phi.inputCount(); j++) {
                            if (phi.inputIn(other) == null) {
                                throw new CiBailout("phi " + phi + " has null operand at new predecessor");
                            }
                        }
                        continue;
                    }
                }
                throw new CiBailout("instruction is not a phi or null at " + i);
            }
        }
    }

    private void checkSize(NewFrameState other) {
        if (other.stackIndex != stackIndex) {
            throw new CiBailout("stack sizes do not match");
        } else if (other.maxLocals != maxLocals) {
            throw new CiBailout("local sizes do not match");
        }
    }

    public void mergeAndInvalidate(BlockBegin block, NewFrameState other) {
        checkSize(other);
        for (int i = 0; i < valuesSize(); i++) {
            Value x = values[i];
            if (x != null) {
                Value y = other.values[i];
                if (x != y) {
                    if (typeMismatch(x, y)) {
                        if (x instanceof Phi) {
                            Phi phi = (Phi) x;
                            if (phi.block() == block) {
                                phi.makeDead();
                            }
                        }
                        values[i] = null;
                        continue;
                    }
                    if (i < maxLocals) {
                        // this a local
                        setupPhiForLocal(block, i);
                    } else {
                        // this is a stack slot
                        setupPhiForStack(block, i - maxLocals);
                    }
                }
            }
        }
    }

    private static boolean typeMismatch(Value x, Value y) {
        return y == null || x.kind != y.kind;
    }

    private static boolean isDoubleWord(Value x) {
        return x != null && x.kind.isDoubleWord();
    }

    /**
     * The interface implemented by a client of {@link NewFrameState#forEachPhi(BlockBegin, PhiProcedure)} and
     * {@link NewFrameState#forEachLivePhi(BlockBegin, PhiProcedure)}.
     */
    public static interface PhiProcedure {
        boolean doPhi(Phi phi);
    }

    /**
     * Traverses all {@linkplain Phi phis} of a given block in this frame state.
     *
     * @param block only phis {@linkplain Phi#block() associated} with this block are traversed
     * @param proc the call back invoked for each live phi traversed
     */
    public boolean forEachPhi(BlockBegin block, PhiProcedure proc) {
        int max = this.valuesSize();
        for (int i = 0; i < max; i++) {
            Value instr = values[i];
            if (instr instanceof Phi && !instr.isDeadPhi()) {
                Phi phi = (Phi) instr;
                if (block == null || phi.block() == block) {
                    if (!proc.doPhi(phi)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Traverses all live {@linkplain Phi phis} of a given block in this frame state.
     *
     * @param block only phis {@linkplain Phi#block() associated} with this block are traversed
     * @param proc the call back invoked for each live phi traversed
     */
    public boolean forEachLivePhi(BlockBegin block, PhiProcedure proc) {
        int max = this.valuesSize();
        for (int i = 0; i < max; i++) {
            Value instr = values[i];
            if (instr instanceof Phi && instr.isLive() && !instr.isDeadPhi()) {
                Phi phi = (Phi) instr;
                if (block == null || phi.block() == block) {
                    if (!proc.doPhi(phi)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks whether this frame state has any {@linkplain Phi phi} statements.
     */
    public boolean hasPhis() {
        int max = valuesSize();
        for (int i = 0; i < max; i++) {
            Value value = values[i];
            if (value instanceof Phi) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterates over all the values in this frame state and its callers, including the stack, locals, and locks.
     * @param closure the closure to apply to each value
     */
    public void valuesDo(ValueClosure closure) {
        valuesDo(this, closure);
    }

    /**
     * Iterates over all the values of a given frame state and its callers, including the stack, locals, and locks.
     * @param closure the closure to apply to each value
     */
    public static void valuesDo(NewFrameState state, ValueClosure closure) {
        do {
            final int max = state.valuesSize();
            for (int i = 0; i < max; i++) {
                if (state.values[i] != null) {
                    Value newValue = closure.apply(state.values[i]);
                    if (!state.unsafe && newValue.kind.isWord()) {
                        state.unsafe = true;
                    }
                    state.values[i] = newValue;
                }
            }
            if (state.locks != null) {
                for (int i = 0; i < state.locks.size(); i++) {
                    Value instr = state.locks.get(i);
                    if (instr != null) {
                        state.locks.set(i, closure.apply(instr));
                    }
                }
            }
            state = state.scope().callerState();
        } while (state != null);
    }

    /**
     * The interface implemented by a client of {@link NewFrameState#forEachLiveStateValue(ValueProcedure)}.
     */
    public static interface ValueProcedure {
        void doValue(Value value);
    }

    /**
     * Traverses all {@linkplain Value#isLive() live values} of this frame state and it's callers.
     * The set of values traversed includes all the live stack values in this frame as well as
     * all live locals in this frame and its callers.
     *
     * @param proc the call back called to process each live value traversed
     */
    public void forEachLiveStateValue(ValueProcedure proc) {
        NewFrameState state = this;
        for (int i = 0; i != state.stackSize(); ++i) {
            Value value = state.stackAt(i);
            if (value != null && value.isLive()) {
                proc.doValue(value);
            }
        }
        while (state != null) {
            for (int i = 0; i != state.localsSize(); ++i) {
                Value value = state.localAt(i);
                if (value != null && value.isLive()) {
                    proc.doValue(value);
                }
            }
            state = state.scope().callerState();
        }
    }

    @Override
    public String toString() {
        return "state [locals=" + maxLocals + ", stack=" + stackSize() + ", locks=" + locksSize() + "] " + scope;
    }
}
