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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import com.sun.c1x.C1XOptions;
import com.sun.c1x.util.BitMap;
import com.sun.c1x.util.BlockClosure;
import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>BlockBegin</code> instruction represents the beginning of a basic block,
 * and holds a lot of information about the basic block, including the successor and
 * predecessor blocks, exception handlers, liveness information, etc.
 *
 * @author Ben L. Titzer
 */
public class BlockBegin extends StateSplit {
    /**
     * An enumeration of flags for block entries indicating various things.
     */
    public enum BlockFlag {
        StandardEntry,
        OsrEntry,
        ExceptionEntry,
        SubroutineEntry,
        BackwardBranchTarget,
        IsOnWorkList,
        WasVisited,
        DefaultExceptionHandler,
        ParserLoopHeader,
        CriticalEdgeSplit,
        LinearScanLoopHeader,
        LinearScanLoopEnd;

        public final int mask() {
            return 1 << ordinal();
        }
    }

    private static final int _entryFlags = BlockFlag.StandardEntry.mask() | BlockFlag.OsrEntry.mask() | BlockFlag.ExceptionEntry.mask();


    private int _blockFlags;
    private final List<BlockBegin> _predecessors;
    private BlockEnd _end;

    private int _blockID;
    private int _depthFirstNumber;
    private int _linearScanNumber;
    private int _loopDepth;
    private int _loopIndex;

    private BlockBegin _dominator;
    private List<BlockBegin> _exceptionHandlerBlocks;
    private List<ValueStack> _exceptionHandlerStates;

    /**
     * Constructs a new BlockBegin at the specified bytecode index.
     * @param bci the bytecode index of the start
     */
    public BlockBegin(int bci) {
        super(ValueType.ILLEGAL_TYPE);
        _depthFirstNumber = -1;
        _linearScanNumber = -1;
        _predecessors = new ArrayList<BlockBegin>(2);
        _loopIndex = -1;
        _exceptionHandlerBlocks = new ArrayList<BlockBegin>(0);
        _exceptionHandlerStates = new ArrayList<ValueStack>(0);
        setBCI(bci);
    }

    /**
     * Gets the ID of this block.
     * @return the id number
     */
    public int blockID() {
        return _blockID;
    }

    public void setBlockID(int i) {
        _blockID = i;
    }


    /**
     * Gets the list of predecessors of this block.
     * @return the predecessor list
     */
    public List<BlockBegin> predecessors() {
        return _predecessors;
    }

    /**
     * Gets the dominator of this block.
     * @return the dominator block
     */
    public BlockBegin dominator() {
        return _dominator;
    }

    /**
     * Sets the dominator block for this block.
     * @param dominator the dominator for this block
     */
    public void setDominator(BlockBegin dominator) {
        _dominator = dominator;
    }

    /**
     * Gets the depth first traversal number of this block.
     * @return the depth first number
     */
    public int depthFirstNumber() {
        return _depthFirstNumber;
    }

    /**
     * Gets the linear scan number of this block.
     * @return the linear scan number
     */
    public int linearScanNumber() {
        return _linearScanNumber;
    }

    /**
     * Gets the loop depth of this block.
     * @return the loop depth
     */
    public int loopDepth() {
        return _loopDepth;
    }

    /**
     * Gets the loop index of this block
     * @return the loop index
     */
    public int loopIndex() {
        return _loopIndex;
    }

    /**
     * Gets the block end associated with this basic block.
     * @return the block end
     */
    public BlockEnd end() {
        return _end;
    }

    /**
     * Gets the exception handlers that cover this basic block.
     * @return the exception handlers
     */
    public List<BlockBegin> exceptionHandlerBlocks() {
        return _exceptionHandlerBlocks;
    }

    public List<ValueStack> exceptionHandlerStates() {
        return _exceptionHandlerStates;
    }

    public void setDepthFirstNumber(int depthFirstNumber) {
        this._depthFirstNumber = depthFirstNumber;
    }

    public void setLinearScanNumber(int linearScanNumber) {
        this._linearScanNumber = linearScanNumber;
    }

    public void setLoopDepth(int loopDepth) {
        this._loopDepth = loopDepth;
    }

    public void setLoopIndex(int loopIndex) {
        this._loopIndex = loopIndex;
    }

    /**
     * Set the block end for this block begin. This method will
     * reset this block's successor list and rebuild it to be equivalent
     * to the successor list of the specified block end.
     * @param end the new block end for this block begin
     */
    public void setEnd(BlockEnd end) {
        assert end != null;
        BlockEnd old = _end;
        if (old != end) {
            if (old != null) {
                // disconnect this block from the old end
                old.setBegin(null);
                // disconnect this block from its current successors
                for (BlockBegin s : old.successors()) {
                    s.predecessors().remove(this);
                }
            }
            this._end = end;
            for (BlockBegin s : end.successors()) {
                s.addPredecessor(this);
            }
        }
    }

    public void setExceptionHandlerBlocks(List<BlockBegin> exceptionHandlers) {
        this._exceptionHandlerBlocks = exceptionHandlers;
    }

    public void setExceptionHandlerStates(List<ValueStack> exceptionHandlerStates) {
        this._exceptionHandlerStates = exceptionHandlerStates;
    }

    /**
     * Checks whether this block is an entrypoint, either as a standard entrypoint,
     * subroutine entrypoint, or an exception handler.
     * @return <code>true</code> if this block is an entrypoint
     */
    public boolean isEntryBlock() {
        return (_blockFlags & _entryFlags) != 0;
    }

    /**
     * Set a flag on this block.
     * @param flag the flag to set
     */
    public void setBlockFlag(BlockFlag flag) {
        _blockFlags |= flag.mask();
    }

    /**
     * Clear a flag on this block.
     * @param flag the flag to clear
     */
    public void clearBlockFlag(BlockFlag flag) {
        _blockFlags &= ~flag.mask();
    }

    public void copyBlockFlag(BlockBegin other, BlockFlag flag) {
        setBlockFlag(flag, other.checkBlockFlag(flag));
    }

    /**
     * Check whether this block has the specified flag set.
     * @param flag the flag to test
     * @return <code>true</code> if this block has the flag
     */
    public final boolean checkBlockFlag(BlockFlag flag) {
        return (_blockFlags & flag.mask()) != 0;
    }

    /**
     * Iterate over this block, its exception handlers, and its successors, in that order.
     * @param closure the closure to apply to each block
     */
    public void iteratePreOrder(BlockClosure closure) {
        // XXX: identity hash map might be too slow, consider a boolean array or a mark field
        iterate(new IdentityHashMap<BlockBegin, BlockBegin>(), closure, true);
    }

    /**
     * Iterate over this block's exception handlers, its successors, and itself, in that order.
     * @param closure the closure to apply to each block
     */
    public void iteratePostOrder(BlockClosure closure) {
        // XXX: identity hash map might be too slow, consider a boolean array or a mark field
        iterate(new IdentityHashMap<BlockBegin, BlockBegin>(), closure, false);
    }

    private void iterate(IdentityHashMap<BlockBegin, BlockBegin> mark, BlockClosure closure, boolean pre) {
        if (!mark.containsKey(this)) {
            mark.put(this, this);
            if (pre) {
                closure.apply(this);
            }
            BlockEnd e = end();
            iterateReverse(mark, closure, _exceptionHandlerBlocks, pre);
            iterateReverse(mark, closure, e.successors(), pre);
            if (!pre) {
                closure.apply(this);
            }
        }
    }

    private void iterateReverse(IdentityHashMap<BlockBegin, BlockBegin> mark, BlockClosure closure, List<BlockBegin> list, boolean pre) {
        for (int i = list.size() - 1; i >= 0; i--) {
            list.get(i).iterate(mark, closure, pre);
        }
    }

    public void addExceptionHandler(BlockBegin b) {
        assert b != null && b.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry);
        if (!_exceptionHandlerBlocks.contains(b)) {
            _exceptionHandlerBlocks.add(b);
        }
    }

    public int addExceptionState(ValueStack state) {
        assert checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry);
        if (_exceptionHandlerStates == null) {
            _exceptionHandlerStates = new ArrayList<ValueStack>(1);
        }
        _exceptionHandlerStates.add(state);
        return _exceptionHandlerStates.size() - 1;
    }

    /**
     * Add a predecessor to this block
     * @param pred the predecessor to add
     */
    public void addPredecessor(BlockBegin pred) {
        _predecessors.add(pred);
    }

    /**
     * Removes all occurrences of the specified block from the predecessor list of this block.
     * @param pred the predecessor to remove
     */
    public void removePredecessor(BlockBegin pred) {
        while (_predecessors.remove(pred)) {
            // the block may appear multiple times in the list
        }
    }

    /**
     * Implements half of the visitor pattern for this instruction.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitBlockBegin(this);
    }

    public boolean tryMerge(ValueStack newState) {
        ValueStack existingState = state();

        if (existingState == null) {
            // this is the first state for the block
            if (wasVisited()) {
                // this can happen for complex jsr/ret patterns; just bail out
                return false;
            }

            // copy state because it is modified
            newState = newState.copy();

            // if a liveness map is available, use it to invalidate dead locals
            BitMap liveness = newState.scope().method().liveness(bci());
            if (liveness != null) {
                invalidateDeadLocals(newState, liveness);
            }

            // if the block is a loop header, insert all necessary phis
            if (isLoopHeader()) {
                insertLoopPhis(newState);
            }

            setState(newState);
        } else if (existingState.isSameAcrossScopes(newState)) {

            while (existingState.scope() != newState.scope()) {
                // XXX: original code is not sure if this is necessary
                newState = newState.scope().callerState();
                assert newState != null : "could not match scopes";
            }

            assert existingState.scope() == newState.scope();
            assert existingState.localsSize() == newState.localsSize();
            assert existingState.stackSize() == newState.stackSize();

            if (wasVisited()) {
                // this block better be a loop header
                if (!isLoopHeader()) {
                    // jsr/ret structure too complicated
                    return false;
                }

                // check that all local and stack tags match
                if (!existingState.checkLocalAndStackTags(newState)) {
                    return false;
                }

                // verify all phis in locals and the stack
                if (C1XOptions.ExtraPhiChecking && !existingState.checkPhis(this, newState)) {
                    return false;
                }
            } else {
                // there is an existing state, but the block was not visited yet
                // do a merge of the stacks and locals
                existingState.merge(this, newState);
            }

        } else {
            // stacks or locks do not match--bytecodes would not verify
            return false;
        }

        return true;
    }

    private void invalidateDeadLocals(ValueStack newState, BitMap liveness) {
        int max = newState.localsSize();
        assert liveness.size() == max;
        for (int i = 0; i < max; i++) {
            Instruction x = newState.localAt(i);
            if (x != null && (x.type().isIllegal() || !liveness.get(i))) {
                // invalidate the local if it is not live
                newState.invalidateLocal(i);
            }
        }
    }

    private void insertLoopPhis(ValueStack newState) {
        int stackSize = newState.stackSize();
        for (int i = 0; i < stackSize; i++) {
            // always insert phis for the stack
            newState.setupPhiForStack(this, i);
        }
        int localsSize = newState.localsSize();
        BitMap requiresPhi = newState.scope().getStoresInLoops();
        for (int i = 0; i < localsSize; i++) {
            Instruction x = newState.localAt(i);
            if (x != null) {
                if (requiresPhi != null) {
                    if (requiresPhi.get(i) || (x.type().isDoubleWord() && requiresPhi.get(i + 1))) {
                        // selectively do a phi
                        newState.setupPhiForLocal(this, i);
                    }
                } else {
                    // always setup a phi
                    newState.setupPhiForLocal(this, i);
                }
            }
        }
    }

    public void merge(ValueStack newState) {
        boolean b = tryMerge(newState);
        assert b : "merge failed";
    }

    public final boolean isStandardEntry() {
        return checkBlockFlag(BlockFlag.StandardEntry);
    }

    public final void setStandardEntry() {
        setBlockFlag(BlockFlag.StandardEntry);
    }

    public final boolean isOsrEntry() {
        return checkBlockFlag(BlockFlag.OsrEntry);
    }

    public final void setOsrEntry(boolean value) {
        setBlockFlag(BlockFlag.OsrEntry, value);
    }

    public final boolean isExceptionEntry() {
        return checkBlockFlag(BlockFlag.ExceptionEntry);
    }

    public final void setExceptionEntry() {
        setBlockFlag(BlockFlag.ExceptionEntry);
    }

    public final boolean isSubroutineEntry() {
        return checkBlockFlag(BlockFlag.SubroutineEntry);
    }

    public final void setSubroutineEntry() {
        setBlockFlag(BlockFlag.SubroutineEntry);
    }

    public final boolean isOnWorkList() {
        return checkBlockFlag(BlockFlag.IsOnWorkList);
    }

    public final void setOnWorkList(boolean value) {
        setBlockFlag(BlockFlag.IsOnWorkList, value);
    }

    public final boolean wasVisited() {
        return checkBlockFlag(BlockFlag.WasVisited);
    }

    public final void setWasVisited(boolean value) {
        setBlockFlag(BlockFlag.WasVisited, value);
    }

    public final boolean isLoopHeader() {
        return checkBlockFlag(BlockFlag.ParserLoopHeader);
    }

    public final void setLoopHeader(boolean value) {
        setBlockFlag(BlockFlag.ParserLoopHeader, value);
    }


    private void setBlockFlag(BlockFlag flag, boolean value) {
        if (value) {
            setBlockFlag(flag);
        } else {
            clearBlockFlag(flag);
        }
    }

    public void copyBlockFlags(BlockBegin other) {
        copyBlockFlag(other, BlockBegin.BlockFlag.ParserLoopHeader);
        copyBlockFlag(other, BlockBegin.BlockFlag.SubroutineEntry);
        copyBlockFlag(other, BlockBegin.BlockFlag.ExceptionEntry);
        copyBlockFlag(other, BlockBegin.BlockFlag.WasVisited);
    }

}
