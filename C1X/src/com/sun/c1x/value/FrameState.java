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

import com.sun.c1x.ci.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * The {@code FrameState} class encapsulates the frame state (i.e. local variables and
 * operand stack) at a particular point in the abstract interpretation.
 *
 * @author Ben L. Titzer
 */
public class FrameState {

    private final Value[] values; // manages both stack and locals
    private int stackIndex;
    private final int maxLocals;

    private final IRScope scope;
    private ArrayList<Value> locks;

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

    public FrameState(IRScope irScope, int maxLocals, int maxStack) {
        this.scope = irScope;
        this.values = new Value[maxLocals + Math.max(maxStack, MINIMUM_STACK_SLOTS)];
        this.maxLocals = maxLocals;
    }

    /**
     * Copies the contents of this value stack so that further updates to either stack aren't reflected in the other.
     *
     * @param withLocals indicates whether to copy the local state
     * @param withStack indicates whether to copy the stack state
     * @param withLocks indicates whether to copy the lock state
     * @return a new value stack with the specified components
     */
    public FrameState copy(boolean withLocals, boolean withStack, boolean withLocks) {
        final FrameState other = new FrameState(scope, localsSize(), maxStackSize());
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

    public FrameState copyLocks() {
        int size = scope().lockStackSize();
        if (stackSize() == 0) {
            size = 0;
        }
        FrameState s = new FrameState(scope(), localsSize(), maxStackSize());
        s.replaceLocks(this);
        s.replaceLocals(this);
        s.replaceStack(this);
        s.stackIndex = size; // trim stack back to lockstack size
        s.unsafe = unsafe;
        return s;
    }

    public FrameState copy() {
        return copy(true, true, true);
    }

    public FrameState immutableCopy() {
        return copy(true, true, true);
    }

    public boolean isSameAcrossScopes(FrameState other) {
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
     * Returns the inlining context associated with this value stack.
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
     * Replace the local variables in this value stack with the local variables from the specified value stack. This is
     * used in inlining.
     *
     * @param with the value stack containing the new local variables
     */
    public void replaceLocals(FrameState with) {
        assert with.maxLocals == maxLocals;
        System.arraycopy(with.values, 0, values, 0, maxLocals);
    }

    /**
     * Replace the stack in this value stack with the stack from the specified value stack. This is used in inlining.
     *
     * @param with the value stack containing the new local variables
     */
    public void replaceStack(FrameState with) {
        System.arraycopy(with.values, with.maxLocals, values, maxLocals, with.stackIndex);
        stackIndex = with.stackIndex;
    }

    /**
     * Replace the locks in this value stack with the locks from the specified value stack. This is used in inlining.
     *
     * @param with the value stack containing the new local variables
     */
    public void replaceLocks(FrameState with) {
        if (with.locks == null) {
            locks = null;
        } else {
            locks = Util.uncheckedCast(with.locks.clone());
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
     * Clears all values on this stack.
     */
    public void clearStack() {
        stackIndex = 0;
    }

    public void clearLocals() {
        for (int i = 0; i < values.length; i++) {
            values[i] = null;
        }
    }

    /**
     * Truncates this stack to the specified size.
     * @param size the size to truncate to
     */
    public void truncateStack(int size) {
        stackIndex = size;
    }

    /**
     * Pushes an instruction onto the stack with the expected type.
     * @param kind the type expected for this instruction
     * @param x the instruction to push onto the stack
     */
    public void push(CiKind kind, Value x) {
        xpush(assertKind(kind, x));
        if (kind.sizeInSlots() == 2) {
            xpush(null);
        }
        if (kind.isWord()) {
            unsafe = true;
        }
    }

    /**
     * Pushes a value onto the stack without checking the type.
     * @param x the instruction to push onto the stack
     */
    public void xpush(Value x) {
        assert stackIndex >= 0;
        assert maxLocals + stackIndex < values.length;
        values[maxLocals + stackIndex++] = x;
    }

    /**
     * Pushes a value onto the stack and checks that it is an int.
     * @param x the instruction to push onto the stack
     */
    public void ipush(Value x) {
        xpush(assertInt(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is a float.
     * @param x the instruction to push onto the stack
     */
    public void fpush(Value x) {
        xpush(assertFloat(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is an object.
     * @param x the instruction to push onto the stack
     */
    public void apush(Value x) {
        xpush(assertObject(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is a word.
     * @param x the instruction to push onto the stack
     */
    public void wpush(Value x) {
        unsafe = true;
        xpush(assertWord(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is a JSR return address.
     * @param x the instruction to push onto the stack
     */
    public void jpush(Value x) {
        xpush(assertJsr(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is a long.
     *
     * @param x the instruction to push onto the stack
     */
    public void lpush(Value x) {
        xpush(assertLong(x));
        xpush(null);
    }

    /**
     * Pushes a value onto the stack and checks that it is a double.
     * @param x the instruction to push onto the stack
     */
    public void dpush(Value x) {
        xpush(assertDouble(x));
        xpush(null);
    }

    /**
     * Pops an instruction off the stack with the expected type.
     * @param kind the expected type
     * @return the instruction on the top of the stack
     */
    public Value pop(CiKind kind) {
        if (kind.sizeInSlots() == 2) {
            xpop();
        }
        return assertKind(kind, xpop());
    }

    /**
     * Pops a value off of the stack without checking the type.
     * @return x the instruction popped off the stack
     */
    public Value xpop() {
        assert stackIndex >= 1;
        return values[maxLocals + --stackIndex];
    }

    /**
     * Pops a value off of the stack and checks that it is an int.
     * @return x the instruction popped off the stack
     */
    public Value ipop() {
        return assertInt(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a float.
     * @return x the instruction popped off the stack
     */
    public Value fpop() {
        return assertFloat(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is an object.
     * @return x the instruction popped off the stack
     */
    public Value apop() {
        return assertObject(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a word.
     * @return x the instruction popped off the stack
     */
    public Value wpop() {
        return assertWord(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a JSR return address.
     * @return x the instruction popped off the stack
     */
    public Value jpop() {
        return assertJsr(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a long.
     * @return x the instruction popped off the stack
     */
    public Value lpop() {
        assertHigh(xpop());
        return assertLong(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a double.
     * @return x the instruction popped off the stack
     */
    public Value dpop() {
        assertHigh(xpop());
        return assertDouble(xpop());
    }

    /**
     * Pop the specified number of slots off of this stack and return them as an array of instructions.
     * @param size the number of arguments off of the stack
     * @return an array containing the arguments off of the stack
     */
    public Value[] popArguments(int size) {
        int base = stackIndex - size;
        Value[] r = new Value[size];
        int y = maxLocals + base;
        for (int i = 0; i < size; ++i) {
            assert values[y] != null || values[y - 1].kind.jvmSlots == 2;
            r[i] = values[y++];
        }
        stackIndex = base;
        return r;
    }

    /**
     * Locks a new object within the specified IRScope.
     * @param scope the IRScope in which this locking operation occurs
     * @param obj the object being locked
     * @return the index of the lock within the lock stack
     */
    public int lock(IRScope scope, Value obj) {
        if (locks == null) {
            locks = new ArrayList<Value>();
        }
        locks.add(obj);
        int size = locks.size();
        scope.setMinimumNumberOfLocks(size);
        return size - 1;
    }

    /**
     * Unlock the lock on the top of the stack.
     * @return the index of the lock just unlocked.
     */
    public int unlock() {
        locks.remove(locks.size() - 1);
        return locks.size();
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
     * Creates a new ValueStack corresponding to inlining the specified method into this point in this value stack.
     * @param scope the IRScope representing the inlined method
     * @return a new value stack representing the state at the beginning of inlining the specified method into this one
     */
    public FrameState pushScope(IRScope scope) {
        assert scope.caller == this.scope;
        RiMethod method = scope.method;
        FrameState res = new FrameState(scope, method.maxLocals(), maxStackSize() + method.maxStackSize());
        res.replaceStack(this);
        res.replaceLocks(this);
        res.unsafe = unsafe;
        return res;
    }

    /**
     * Creates a new ValueStack corresponding to the state upon returning from this inlined method into the outer
     * IRScope.
     * @return a new value stack representing the state at exit from this value stack
     */
    public FrameState popScope() {
        IRScope callingScope = scope.caller;
        assert callingScope != null;
        assert maxStackSize() >= scope.method.maxStackSize();
        FrameState res = new FrameState(callingScope, callingScope.method.maxLocals(), maxStackSize());
        res.replaceStack(this);
        res.replaceLocks(this);
        res.replaceLocals(scope.callerState());
        res.unsafe = unsafe;
        return res;
    }

    /**
     * Inserts a phi statement into the stack at the specified stack index.
     * @param block the block begin for which we are creating the phi
     * @param i the index into the stack for which to create a phi
     */
    public void setupPhiForStack(BlockBegin block, int i) {
        Value p = stackAt(i);
        if (p instanceof Phi) {
            Phi phi = (Phi) p;
            if (phi.block() == block && phi.isOnStack() && phi.stackIndex() == i) {
                return;
            }
        }
        values[maxLocals + i] = new Phi(p.kind, block, -i - 1);
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
     * Iterates over all the values in this value stack, including the stack, locals, and locks.
     * @param closure the closure to apply to each value
     */
    public void valuesDo(ValueClosure closure) {
        final int max = valuesSize();
        for (int i = 0; i < max; i++) {
            if (values[i] != null) {
                Value newValue = closure.apply(values[i]);
                if (!unsafe && newValue.kind.isWord()) {
                    unsafe = true;
                }
                values[i] = newValue;
            }
        }
        if (locks != null) {
            for (int i = 0; i < locks.size(); i++) {
                Value instr = locks.get(i);
                if (instr != null) {
                    locks.set(i, closure.apply(instr));
                }
            }
        }
        FrameState state = this.scope().callerState();
        if (state != null) {
            state.valuesDo(closure);
        }
    }

    public int valuesSize() {
        return maxLocals + stackIndex;
    }

    public int callerStackSize() {
        FrameState callerState = scope().callerState();
        return callerState == null ? 0 : callerState.stackSize();
    }

    public void checkPhis(BlockBegin block, FrameState other) {
        checkSize(other);
        final int max = valuesSize();
        for (int i = 0; i < max; i++) {
            Value x = values[i];
            Value y = other.values[i];
            if (x != null && x != y) {
                if (x instanceof Phi) {
                    Phi phi = (Phi) x;
                    if (phi.block() == block) {
                        for (int j = 0; j < phi.operandCount(); j++) {
                            if (phi.operandIn(other) == null) {
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

    private void checkSize(FrameState other) {
        if (other.stackIndex != stackIndex) {
            throw new CiBailout("stack sizes do not match");
        } else if (other.maxLocals != maxLocals) {
            throw new CiBailout("local sizes do not match");
        }
    }

    public void mergeAndInvalidate(BlockBegin block, FrameState other) {
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

    private Value assertKind(CiKind kind, Value x) {
        assert x != null && (unsafe || x.kind == kind);
        return x;
    }

    private Value assertLong(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Long);
        return x;
    }

    private Value assertJsr(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Jsr);
        return x;
    }

    private Value assertInt(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Int);
        return x;
    }

    private Value assertFloat(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Float);
        return x;
    }

    private Value assertObject(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Object);
        return x;
    }

    private Value assertWord(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Word);
        return x;
    }

    private Value assertDouble(Value x) {
        assert x != null && (unsafe || x.kind == CiKind.Double);
        return x;
    }

    private static void assertHigh(Value x) {
        assert x == null;
    }

    private static boolean isDoubleWord(Value x) {
        return x != null && x.kind.isDoubleWord();
    }

    /**
     * This is a helper method for iterating over all phis in this value stack.
     * @return an iterator over all phis
     */
    public Iterable<Phi> allPhis(BlockBegin block) {
        final List<Phi> phis = new ArrayList<Phi>();

        int max = this.valuesSize();
        for (int i = 0; i < max; i++) {
            Value instr = values[i];
            if (instr instanceof Phi) {
                if (block == null || ((Phi) instr).block() == block && !instr.isDeadPhi()) {
                    phis.add((Phi) instr);
                }
            }
        }

        return phis;
    }

    /**
     * This is a helper method for iterating over all phis in this value stack.
     * @return an iterator over all phis
     */
    public Iterable<Phi> allLivePhis(BlockBegin block) {
        final List<Phi> phis = new ArrayList<Phi>();

        int max = this.valuesSize();
        for (int i = 0; i < max; i++) {
            Value instr = values[i];
            if (instr instanceof Phi && instr.isLive() && !instr.isDeadPhi()) {
                if (block == null || ((Phi) instr).block() == block) {
                    phis.add((Phi) instr);
                }
            }
        }

        return phis;
    }
    /**
     * Checks whether this value stack has any phi statements that refer to the specified block.
     * @param block the block to check
     * @return {@code true} if this value stack has phis for the specified block
     */
    public boolean hasPhisFor(BlockBegin block) {
        int max = valuesSize();
        for (int i = 0; i < max; i++) {
            Value instr = values[i];
            if (instr instanceof Phi && !instr.isDeadPhi()) {
                if (block == null || ((Phi) instr).block() == block) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This is a helper method for iterating over all stack values and local variables in this value stack.
     * @return an iterator over all state values
     */
    public Iterable<Value> allLiveStateValues() {
        // TODO: implement a more efficient iterator for use in linear scan
        List<Value> result = new ArrayList<Value>(valuesSize());
        for (FrameState state = this; state != null; state = state.scope.callerState()) {
            int max = state.valuesSize();

            for (int i = 0; i < max; i++) {
                Value instr = state.values[i];
                if (instr != null && instr.isLive()) {
                    result.add(instr);
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "state [nr locals = " + maxLocals + ", stack depth = " + stackSize() + "] " + scope;
    }
}
