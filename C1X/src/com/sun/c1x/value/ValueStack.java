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
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * The <code>ValueStack</code> class encapsulates the state of local variables and the stack at a particular point in
 * the abstract interpretation.
 *
 * @author Ben L. Titzer
 */
public class ValueStack {

    private final Value[] values; // manages both stack and locals
    private int stackIndex;
    private final int maxLocals;

    private final IRScope scope;
    private ArrayList<Value> locks;
    private boolean lockStack;

    public ValueStack(IRScope irScope, int maxLocals, int maxStack) {
        this.scope = irScope;
        this.values = new Value[maxLocals + maxStack];
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
    public ValueStack copy(boolean withLocals, boolean withStack, boolean withLocks) {
        final ValueStack other = new ValueStack(scope, localsSize(), maxStackSize());
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
        return other;
    }

    public ValueStack copyLocks() {
        int size = scope().lockStackSize();
        if (stackSize() == 0) {
            size = 0;
        }
        ValueStack s = new ValueStack(scope(), localsSize(), maxStackSize());
        s.lockStack = true;
        s.replaceLocks(this);
        s.replaceLocals(this);
        s.replaceStack(this);
        s.stackIndex = size; // trim stack back to lockstack size
        return s;
    }

    public ValueStack copy() {
        return copy(true, true, true);
    }

    public ValueStack immutableCopy() {
        return copy(true, true, true);
    }

    public boolean isSameAcrossScopes(ValueStack other) {
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
    public final IRScope scope() {
        return scope;
    }

    /**
     * Returns the size of the local variables.
     *
     * @return the size of the local variables
     */
    public final int localsSize() {
        return maxLocals;
    }

    /**
     * Returns the size of the locks.
     *
     * @return the size of the locks
     */
    public final int locksSize() {
        return locks == null ? 0 : locks.size();
    }

    /**
     * Returns the current size (height) of the stack.
     *
     * @return the size of the stack
     */
    public final int stackSize() {
        return stackIndex;
    }

    /**
     * Returns the maximum size of the stack.
     *
     * @return the maximum size of the stack
     */
    public final int maxStackSize() {
        return values.length - maxLocals;
    }

    /**
     * Checks whether the stack is empty.
     *
     * @return <code>true</code> the stack is currently empty
     */
    public final boolean stackEmpty() {
        return stackIndex == 0;
    }

    /**
     * Checks whether there are any active locks.
     *
     * @return <code>true</code> if there are <i>no</i> active locks
     */
    public final boolean noActiveLocks() {
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
            assert x.type().isSingleWord() || values[i + 1] == null || values[i + 1] instanceof Phi;
        }
        return x;
    }

    /**
     * Store the local variable at the specified index. If the value is a doubleword, then also overwrite the next local
     * variable position.
     *
     * @param i the index at which to store
     * @param x the instruction which produces the value for the local
     */
    public void storeLocal(int i, Value x) {
        invalidateLocal(i);
        values[i] = x;
        if (isDoubleWord(x)) {
            // if this was a double word and i + 1 was a double word, then kill i + 2
            Value h = values[i + 1];
            if (isDoubleWord(h)) {
                values[i + 2] = null;
            }
        }
        if (i > 0) {
            // if there was a double word at i - 1, then kill it
            Value p = values[i - 1];
            if (isDoubleWord(p)) {
                values[i - 1] = null;
            }
        }
    }

    /**
     * Replace the local variables in this value stack with the local variables from the specified value stack. This is
     * used in inlining.
     *
     * @param with the value stack containing the new local variables
     */
    public void replaceLocals(ValueStack with) {
        assert with.maxLocals == maxLocals;
        System.arraycopy(with.values, 0, values, 0, maxLocals);
    }

    /**
     * Replace the stack in this value stack with the stack from the specified value stack. This is used in inlining.
     *
     * @param with the value stack containing the new local variables
     */
    public void replaceStack(ValueStack with) {
        System.arraycopy(with.values, with.maxLocals, values, maxLocals, with.stackIndex);
        stackIndex = with.stackIndex;
    }

    /**
     * Replace the locks in this value stack with the locks from the specified value stack. This is used in inlining.
     *
     * @param with the value stack containing the new local variables
     */
    public void replaceLocks(ValueStack with) {
        if (with.locks == null) {
            locks = null;
        } else {
            locks = Util.uncheckedCast(with.locks.clone());
        }
    }

    /**
     * Get the value on the stack at the specified stack index.
     *
     * @param i the index into the stack, with <code>0</code> being the bottom of the stack
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
     *
     * @param size the size to truncate to
     */
    public void truncateStack(int size) {
        stackIndex = size;
    }

    /**
     * Pushes an instruction onto the stack with the expected type.
     *
     * @param type the type expected for this instruction
     * @param x the instruction to push onto the stack
     */
    public void push(CiKind type, Value x) {
        xpush(assertType(type, x));
        if (type.sizeInSlots() == 2) {
            xpush(null);
        }
    }

    /**
     * Pushes a value onto the stack without checking the type.
     *
     * @param x the instruction to push onto the stack
     */
    public void xpush(Value x) {
        assert stackIndex >= 0;
        values[maxLocals + stackIndex++] = x;
    }

    /**
     * Pushes a value onto the stack and checks that it is an int.
     *
     * @param x the instruction to push onto the stack
     */
    public void ipush(Value x) {
        xpush(assertInt(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is a float.
     *
     * @param x the instruction to push onto the stack
     */
    public void fpush(Value x) {
        xpush(assertFloat(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is an object.
     *
     * @param x the instruction to push onto the stack
     */
    public void apush(Value x) {
        xpush(assertObject(x));
    }

    /**
     * Pushes a value onto the stack and checks that it is a JSR return address.
     *
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
     *
     * @param x the instruction to push onto the stack
     */
    public void dpush(Value x) {
        xpush(assertDouble(x));
        xpush(null);
    }

    /**
     * Pops an instruction off the stack with the expected type.
     *
     * @param basicType the tag of the expected type
     * @return the instruction on the top of the stack
     */
    public Value pop(CiKind basicType) {
        if (basicType.sizeInSlots() == 2) {
            xpop();
        }
        return assertType(basicType, xpop());
    }

    /**
     * Pops a value off of the stack without checking the type.
     *
     * @return x the instruction popped off the stack
     */
    public Value xpop() {
        assert stackIndex >= 1;
        return values[maxLocals + --stackIndex];
    }

    /**
     * Pops a value off of the stack and checks that it is an int.
     *
     * @return x the instruction popped off the stack
     */
    public Value ipop() {
        return assertInt(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a float.
     *
     * @return x the instruction popped off the stack
     */
    public Value fpop() {
        return assertFloat(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is an object.
     *
     * @return x the instruction popped off the stack
     */
    public Value apop() {
        return assertObject(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a JSR return address.
     *
     * @return x the instruction popped off the stack
     */
    public Value jpop() {
        return assertJsr(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a long.
     *
     * @return x the instruction popped off the stack
     */
    public Value lpop() {
        assertHigh(xpop());
        return assertLong(xpop());
    }

    /**
     * Pops a value off of the stack and checks that it is a double.
     *
     * @return x the instruction popped off the stack
     */
    public Value dpop() {
        assertHigh(xpop());
        return assertDouble(xpop());
    }

    /**
     * Pop the specified number of slots off of this stack and return them as an array of instructions.
     *
     * @param size the number of arguments off of the stack
     * @return an array containing the arguments off of the stack
     */
    public Value[] popArguments(int size) {
        int base = stackIndex - size;
        Value[] r = new Value[size];
        System.arraycopy(values, maxLocals + base, r, 0, size);
        stackIndex = base;
        return r;
    }

    /**
     * Locks a new object within the specified IRScope.
     *
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
     *
     * @return the index of the lock just unlocked.
     */
    public int unlock() {
        locks.remove(locks.size() - 1);
        return locks.size();
    }

    /**
     * Retrieves the lock at the specified index in the lock stack.
     *
     * @param i the index into the lock stack
     * @return the instruction which produced the object at the specified location in the lock stack
     */
    public Value lockAt(int i) {
        return locks.get(i);
    }

    /**
     * Creates a new ValueStack corresponding to inlining the specified method into this point in this value stack.
     *
     * @param scope the IRScope representing the inlined method
     * @return a new value stack representing the state at the beginning of inlining the specified method into this one
     */
    public ValueStack pushScope(IRScope scope) {
        assert scope.caller == this.scope;
        RiMethod method = scope.method;
        ValueStack res = new ValueStack(scope, method.maxLocals(), maxStackSize() + method.maxStackSize());
        res.replaceStack(this);
        res.replaceLocks(this);
        return res;
    }

    /**
     * Creates a new ValueStack corresponding to the state upon returning from this inlined method into the outer
     * IRScope.
     *
     * @return a new value stack representing the state at exit from this value stack
     */
    public ValueStack popScope() {
        IRScope callingScope = scope.caller;
        assert callingScope != null;
        assert maxStackSize() >= scope.method.maxStackSize();
        ValueStack res = new ValueStack(callingScope, callingScope.method.maxLocals(), maxStackSize());
        res.replaceStack(this);
        res.replaceLocks(this);
        res.replaceLocals(scope.callerState());
        return res;
    }

    /**
     * Inserts a phi statement into the stack at the specified stack index.
     *
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
        values[maxLocals + i] = new Phi(p.type(), block, -i - 1);
    }

    /**
     * Inserts a phi statement for the local at the specified index.
     *
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
        storeLocal(i, new Phi(p.type(), block, i));
    }

    /**
     * Iterates over all the values in this value stack, including the stack, locals, and locks.
     *
     * @param closure the closure to apply to each value
     */
    public void valuesDo(ValueClosure closure) {
        final int max = valuesSize();
        for (int i = 0; i < max; i++) {
            if (values[i] != null) {
                values[i] = closure.apply(values[i]);
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
        ValueStack state = this.scope().callerState();
        if (state != null) {
            state.valuesDo(closure);
        }
    }

    public int valuesSize() {
        return maxLocals + stackIndex;
    }

    public void checkPhis(BlockBegin block, ValueStack other) {
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

    private void checkSize(ValueStack other) {
        if (other.stackIndex != stackIndex) {
            throw new CiBailout("stack sizes do not match");
        } else if (other.maxLocals != maxLocals) {
            throw new CiBailout("local sizes do not match");
        }
    }

    public void mergeAndInvalidate(BlockBegin block, ValueStack other) {
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
        return y == null || x.type() != y.type();
    }

    private static Value assertType(CiKind basicType, Value x) {
        assert x != null && x.type() == basicType;
        return x;
    }

    private static Value assertLong(Value x) {
        assert x != null && x.type() == CiKind.Long;
        return x;
    }

    private static Value assertJsr(Value x) {
        assert x != null && x.type() == CiKind.Jsr;
        return x;
    }

    private static Value assertInt(Value x) {
        assert x != null && x.type() == CiKind.Int;
        return x;
    }

    private static Value assertFloat(Value x) {
        assert x != null && x.type() == CiKind.Float;
        return x;
    }

    private static Value assertObject(Value x) {
        assert x != null && x.type() == CiKind.Object;
        return x;
    }

    private static Value assertDouble(Value x) {
        assert x != null && x.type() == CiKind.Double;
        return x;
    }

    private static void assertHigh(Value x) {
        assert x == null;
    }

    private static boolean isDoubleWord(Value x) {
        return x != null && x.type().isDoubleWord();
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
     * @return an interator over all state values
     */
    public Iterable<Value> allLiveStateValues() {
        // TODO: implement a more efficient iterator for use in linear scan
        List<Value> result = new ArrayList<Value>(valuesSize());
        for (ValueStack stack = this; stack != null; stack = stack.scope.callerState()) {
            int max = stack.valuesSize();

            for (int i = 0; i < max; i++) {
                Value instr = stack.values[i];
                if (instr != null && instr.isLive()) {
                    result.add(instr);
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "state [locals = " + maxLocals + ", stack = " + stackSize() + "] " + scope;
    }
}
